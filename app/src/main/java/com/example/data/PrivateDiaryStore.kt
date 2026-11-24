package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class DayShift(
    val id: String,
    val label: String,
    val timeRangeLabel: String,
    val emoji: String,
    val accentColorHex: Long
) {
    MORNING("MORNING", "Turno da Manhã", "05:00 às 11:59", "🌅", 0xFFF39C12),
    AFTERNOON("AFTERNOON", "Turno da Tarde", "12:00 às 17:59", "☀️", 0xFF0984E3),
    NIGHT("NIGHT", "Turno da Noite", "18:00 às 04:59", "🌙", 0xFF6C5CE7);

    companion object {
        fun fromHour(hourOfDay: Int): DayShift {
            return when (hourOfDay) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                else -> NIGHT
            }
        }

        fun currentShift(): DayShift {
            val hour = runCatching { LocalTime.now().hour }
                .getOrElse { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            return fromHour(hour)
        }
    }
}

data class MoodOption(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long,
    val empathyDescriptor: String
)

data class PrivateDiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = LocalOfflineStore.LOCAL_USER_ID,
    val dateIso: String, // YYYY-MM-DD
    val epochDay: Long,
    val hour: Int,
    val minute: Int,
    val moodId: String,
    val symptomTags: List<String>,
    val freeText: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val timeFormatted: String
        get() = String.format(Locale.US, "%02d:%02d", hour, minute)

    val shift: DayShift
        get() = DayShift.fromHour(hour)
}

data class SupportLetterRecord(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = LocalOfflineStore.LOCAL_USER_ID,
    val dateIso: String,
    val timeFormatted: String,
    val title: String,
    val content: String,
    val moodSummaryUsed: String,
    val generatedViaCloudAi: Boolean,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class PrivateDiaryState(
    val userId: String = LocalOfflineStore.LOCAL_USER_ID,
    val entries: List<PrivateDiaryEntry> = emptyList(),
    val customSymptomTags: List<String> = emptyList(),
    val savedSupportLetters: List<SupportLetterRecord> = emptyList(),
    val activeShift: DayShift = DayShift.currentShift(),
    val currentShiftQuote: String = "",
    val currentShiftQuoteAuthor: String = "",
    val hasLocalApiKeyConfigured: Boolean = false
)

object PrivateDiaryCatalog {
    val moodOptions: List<MoodOption> = listOf(
        MoodOption("RADIANT", "Radiante", "✨", 0xFF00B894, "com energia luminosa e entusiasmo"),
        MoodOption("FOCUSED", "Disposto(a)", "☀️", 0xFF0984E3, "com boa disposição e presença"),
        MoodOption("CALM", "Tranquilo(a)", "🌿", 0xFF0B6E4F, "em um estado de calma e serenidade"),
        MoodOption("NEUTRAL", "Estável", "🌤️", 0xFF636E72, "em ritmo estável, vivendo um passo de cada vez"),
        MoodOption("TIRED", "Cansado(a)", "🌙", 0xFFE17055, "sentindo cansaço e precisando desacelerar"),
        MoodOption("ANXIOUS", "Ansioso(a)", "🌊", 0xFFD63031, "atravessando ondas de ansiedade e inquietação"),
        MoodOption("OVERWHELMED", "Sensível / Difícil", "🌧️", 0xFF8E44AD, "em um momento delicado que pede acolhimento extra")
    )

    val defaultSymptomTags: List<String> = listOf(
        "Sem dores",
        "Boa disposição",
        "Paz interior",
        "Dor de cabeça",
        "Fadiga muscular",
        "Sono leve",
        "Ansiedade",
        "Tensão nos ombros",
        "Enjoo / Náusea",
        "Inchaço",
        "Falta de foco",
        "Coração acelerado"
    )

    fun moodById(id: String): MoodOption {
        return moodOptions.firstOrNull { it.id == id } ?: moodOptions[2]
    }

    val morningQuotes: List<Pair<String, String>> = listOf(
        "Você não precisa resolver a semana inteira hoje; basta cuidar com carinho das próximas horas." to "Acolhimento da Manhã",
        "Que a sua manhã comece no ritmo que o seu corpo permite. Todo pequeno passo já é movimento." to "Despertar Consciente",
        "Respire fundo antes da primeira tarefa: sua saúde e sua paz vêm antes de qualquer lista." to "Bússola Matinal",
        "Hoje é uma página em branco. Seja gentil consigo mesmo ao escrever as primeiras linhas do dia." to "Ritmo gentil",
        "A luz da manhã lembra que sempre podemos recomeçar com mais leveza e menos cobrança." to "Recomeço Diário",
        "Celebre o simples fato de estar aqui hoje. Sua presença já é o ponto de partida mais importante." to "Presença & Calma",
        "Organize o que for possível e acolha o que precisar esperar. Você está fazendo o seu melhor." to "Equilíbrio Matinal",
        "Um copo d'água, uma respiração profunda e um pensamento gentil mudam a direção da manhã." to "Cuidado Essencial"
    )

    val afternoonQuotes: List<Pair<String, String>> = listOf(
        "Metade do caminho já passou. Faça uma pausa breve, solte os ombros e hidrate-se." to "Pausa da Tarde",
        "Se a energia diminuir à tarde, não se culpe: ajustar a rota também é uma forma de inteligência e autocuidado." to "Ritmo da Tarde",
        "Olhe com carinho para o que você já realizou hoje, mesmo nas pequenas coisas invisíveis." to "Reconhecimento Diário",
        "Nem toda tarde precisa ser acelerada. Permita-se trabalhar e viver em uma cadência humana." to "Cadência Humana",
        "Respire entre um compromisso e outro. O intervalo livre também faz parte da sua saúde." to "Respiro Consciente",
        "Você já venceu muitos desafios até chegar aqui. Continue no seu passo, sem comparar sua jornada." to "Força Tranquila",
        "Uma pausa de cinco minutos para ouvir seu corpo vale mais do que horas no piloto automático." to "Escuta Interior",
        "A tarde é o momento perfeito para agradecer pelo que deu certo e soltar o que não está no seu controle." to "Leveza da Tarde"
    )

    val nightQuotes: List<Pair<String, String>> = listOf(
        "O dia está desacelerando. Tudo o que ficou para amanhã pode esperar; agora é hora de recolher sua energia." to "Refúgio da Noite",
        "Você fez o melhor que pôde com a energia que tinha hoje. Permita-se descansar em paz." to "Abraço Noturno",
        "Solte o peso das cobranças antes de deitar. Seu corpo e sua mente merecem um pouso suave." to "Pouso Suave",
        "A noite não existe para revisar culpas, mas para restaurar suas forças com silêncio e afeto." to "Serenidade Noturna",
        "Agradeça por três pequenos momentos de hoje e deixe a quietude da noite cuidar do resto." to "Gratidão da Noite",
        "Mesmo nos dias difíceis, você atravessou cada hora com coragem. Descanse com orgulho da sua caminhada." to "Coragem Silenciosa",
        "Feche as abas da mente aos poucos. Amanhã haverá nova luz e novas possibilidades." to "Desconexão Gentil",
        "Que a sua noite seja um porto seguro de calma, reparo físico e tranquilidade emocional." to "Porto Seguro"
    )

    fun quotesForShift(shift: DayShift): List<Pair<String, String>> {
        return when (shift) {
            DayShift.MORNING -> morningQuotes
            DayShift.AFTERNOON -> afternoonQuotes
            DayShift.NIGHT -> nightQuotes
        }
    }
}

class PrivateDiaryRepository(
    context: Context,
    private val userId: String = LocalOfflineStore.LOCAL_USER_ID
) {
    private val appContext = context.applicationContext
    private val safeUserKey = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "local_user" }
    private val prefs = appContext.getSharedPreferences("aura_private_diary_$safeUserKey", Context.MODE_PRIVATE)
    private val db: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance(
            appContext.getString(R.string.firestore_database_id)
        )
    }.getOrNull()
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val _stateFlow = MutableStateFlow(loadInitialState())
    val stateFlow: StateFlow<PrivateDiaryState> = _stateFlow.asStateFlow()

    init {
        syncFromFirestoreIfAuthenticated()
    }

    private fun loadInitialState(): PrivateDiaryState {
        val shift = DayShift.currentShift()
        val (quote, author) = pickNextQuoteForShift(shift, forceAdvance = false)
        val rawJson = prefs.getString(KEY_DIARY_JSON, null)
        val localKey = prefs.getString(KEY_LOCAL_GEMINI_API_KEY, "").orEmpty()

        if (rawJson.isNullOrBlank()) {
            val seeded = createInitialSeedState(shift, quote, author, localKey.isNotBlank())
            persistState(seeded)
            return seeded
        }

        return runCatching {
            val root = JSONObject(rawJson)
            val entriesArr = root.optJSONArray("entries") ?: JSONArray()
            val entries = mutableListOf<PrivateDiaryEntry>()
            for (i in 0 until entriesArr.length()) {
                val obj = entriesArr.getJSONObject(i)
                val tagsArr = obj.optJSONArray("symptomTags") ?: JSONArray()
                val tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                entries.add(
                    PrivateDiaryEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", userId),
                        dateIso = obj.optString("dateIso", LocalDate.now().toString()),
                        epochDay = obj.optLong("epochDay", LocalDate.now().toEpochDay()),
                        hour = obj.optInt("hour", 9),
                        minute = obj.optInt("minute", 0),
                        moodId = obj.optString("moodId", "CALM"),
                        symptomTags = tags,
                        freeText = obj.optString("freeText", ""),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }

            val customTagsArr = root.optJSONArray("customSymptomTags") ?: JSONArray()
            val customTags = (0 until customTagsArr.length()).map { customTagsArr.getString(it) }

            val lettersArr = root.optJSONArray("savedSupportLetters") ?: JSONArray()
            val letters = mutableListOf<SupportLetterRecord>()
            for (i in 0 until lettersArr.length()) {
                val obj = lettersArr.getJSONObject(i)
                letters.add(
                    SupportLetterRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", userId),
                        dateIso = obj.optString("dateIso", LocalDate.now().toString()),
                        timeFormatted = obj.optString("timeFormatted", "10:00"),
                        title = obj.optString("title", "Carta de Apoio"),
                        content = obj.optString("content", ""),
                        moodSummaryUsed = obj.optString("moodSummaryUsed", ""),
                        generatedViaCloudAi = obj.optBoolean("generatedViaCloudAi", false),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }

            PrivateDiaryState(
                userId = userId,
                entries = entries.sortedWith(compareByDescending<PrivateDiaryEntry> { it.epochDay }.thenBy { it.hour * 60 + it.minute }),
                customSymptomTags = customTags,
                savedSupportLetters = letters.sortedByDescending { it.createdAtEpochMs },
                activeShift = shift,
                currentShiftQuote = quote,
                currentShiftQuoteAuthor = author,
                hasLocalApiKeyConfigured = isGeminiApiKeyConfigured(localKey)
            )
        }.getOrElse {
            val seeded = createInitialSeedState(shift, quote, author, localKey.isNotBlank())
            persistState(seeded)
            seeded
        }
    }

    private fun createInitialSeedState(
        shift: DayShift,
        quote: String,
        author: String,
        hasLocalKey: Boolean
    ): PrivateDiaryState {
        val today = LocalDate.now()
        val todayIso = today.toString()
        val todayEpoch = today.toEpochDay()

        val initialEntries = listOf(
            PrivateDiaryEntry(
                id = "diary_seed_1",
                userId = userId,
                dateIso = todayIso,
                epochDay = todayEpoch,
                hour = 8,
                minute = 15,
                moodId = "FOCUSED",
                symptomTags = listOf("Boa disposição", "Sem dores"),
                freeText = "Acordei com boa disposição hoje, tomei água e organizei as prioridades essenciais da manhã."
            ),
            PrivateDiaryEntry(
                id = "diary_seed_2",
                userId = userId,
                dateIso = todayIso,
                epochDay = todayEpoch,
                hour = 13,
                minute = 40,
                moodId = "CALM",
                symptomTags = listOf("Paz interior", "Tensão nos ombros"),
                freeText = "Pausa após o almoço. Um pouco de tensão nos ombros, mas mantendo o ritmo tranquilo."
            )
        )

        return PrivateDiaryState(
            userId = userId,
            entries = initialEntries,
            customSymptomTags = emptyList(),
            savedSupportLetters = emptyList(),
            activeShift = shift,
            currentShiftQuote = quote,
            currentShiftQuoteAuthor = author,
            hasLocalApiKeyConfigured = isGeminiApiKeyConfigured(
                prefs.getString(KEY_LOCAL_GEMINI_API_KEY, "").orEmpty()
            ) || hasLocalKey
        )
    }

    private fun persistState(state: PrivateDiaryState) {
        val root = JSONObject()
        val entriesArr = JSONArray()
        state.entries.forEach { entry ->
            entriesArr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("userId", entry.userId)
                    put("dateIso", entry.dateIso)
                    put("epochDay", entry.epochDay)
                    put("hour", entry.hour)
                    put("minute", entry.minute)
                    put("moodId", entry.moodId)
                    val tagsArr = JSONArray()
                    entry.symptomTags.forEach { tagsArr.put(it) }
                    put("symptomTags", tagsArr)
                    put("freeText", entry.freeText)
                    put("createdAtEpochMs", entry.createdAtEpochMs)
                }
            )
        }
        root.put("entries", entriesArr)

        val customTagsArr = JSONArray()
        state.customSymptomTags.forEach { customTagsArr.put(it) }
        root.put("customSymptomTags", customTagsArr)

        val lettersArr = JSONArray()
        state.savedSupportLetters.forEach { letter ->
            lettersArr.put(
                JSONObject().apply {
                    put("id", letter.id)
                    put("userId", letter.userId)
                    put("dateIso", letter.dateIso)
                    put("timeFormatted", letter.timeFormatted)
                    put("title", letter.title)
                    put("content", letter.content)
                    put("moodSummaryUsed", letter.moodSummaryUsed)
                    put("generatedViaCloudAi", letter.generatedViaCloudAi)
                    put("createdAtEpochMs", letter.createdAtEpochMs)
                }
            )
        }
        root.put("savedSupportLetters", lettersArr)

        prefs.edit().putString(KEY_DIARY_JSON, root.toString()).apply()
    }

    private fun updateState(transform: (PrivateDiaryState) -> PrivateDiaryState) {
        val updated = transform(_stateFlow.value)
        _stateFlow.value = updated
        persistState(updated)
    }

    /**
     * Picks a motivational quote for the given shift (Morning 05:00-11:59, Afternoon 12:00-17:59, Night 18:00-04:59),
     * rotating automatically without repeating in sequence.
     */
    private fun pickNextQuoteForShift(
        shift: DayShift,
        forceAdvance: Boolean
    ): Pair<String, String> {
        val list = PrivateDiaryCatalog.quotesForShift(shift)
        val lastIdxKey = "last_quote_idx_${shift.id}"
        val lastShownDateShiftKey = "last_quote_dateshift"
        val todayShiftToken = "${LocalDate.now()}_${shift.id}"

        val previousIdx = prefs.getInt(lastIdxKey, -1)
        val lastToken = prefs.getString(lastShownDateShiftKey, "")

        val nextIdx = if (!forceAdvance && lastToken == todayShiftToken && previousIdx in list.indices) {
            previousIdx
        } else {
            val candidates = list.indices.filter { it != previousIdx }
            val chosen = if (candidates.isNotEmpty()) {
                candidates[Random.nextInt(candidates.size)]
            } else {
                0
            }
            prefs.edit()
                .putInt(lastIdxKey, chosen)
                .putString(lastShownDateShiftKey, todayShiftToken)
                .apply()
            chosen
        }

        return list[nextIdx]
    }

    fun refreshShiftQuote(forceNewQuote: Boolean = true) {
        val shift = DayShift.currentShift()
        val (quote, author) = pickNextQuoteForShift(shift, forceAdvance = forceNewQuote)
        updateState {
            it.copy(
                activeShift = shift,
                currentShiftQuote = quote,
                currentShiftQuoteAuthor = author
            )
        }
    }

    fun addDiaryEntry(
        epochDay: Long,
        hour: Int,
        minute: Int,
        moodId: String,
        symptomTags: List<String>,
        freeText: String
    ): PrivateDiaryEntry {
        val dateIso = LocalDate.ofEpochDay(epochDay).toString()
        val effectiveUid = auth?.currentUser?.uid ?: userId
        val entry = PrivateDiaryEntry(
            id = "diary_${System.currentTimeMillis()}",
            userId = effectiveUid,
            dateIso = dateIso,
            epochDay = epochDay,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            moodId = moodId,
            symptomTags = symptomTags.distinct(),
            freeText = freeText.trim()
        )

        updateState { current ->
            current.copy(
                entries = (current.entries + entry).sortedWith(
                    compareByDescending<PrivateDiaryEntry> { it.epochDay }
                        .thenBy { it.hour * 60 + it.minute }
                )
            )
        }

        syncEntryToFirestore(entry)
        return entry
    }

    fun updateDiaryEntry(
        entryId: String,
        epochDay: Long,
        hour: Int,
        minute: Int,
        moodId: String,
        symptomTags: List<String>,
        freeText: String
    ) {
        val dateIso = LocalDate.ofEpochDay(epochDay).toString()
        var updatedEntry: PrivateDiaryEntry? = null
        updateState { current ->
            val mapped = current.entries.map { existing ->
                if (existing.id == entryId) {
                    val mod = existing.copy(
                        dateIso = dateIso,
                        epochDay = epochDay,
                        hour = hour.coerceIn(0, 23),
                        minute = minute.coerceIn(0, 59),
                        moodId = moodId,
                        symptomTags = symptomTags.distinct(),
                        freeText = freeText.trim()
                    )
                    updatedEntry = mod
                    mod
                } else {
                    existing
                }
            }.sortedWith(
                compareByDescending<PrivateDiaryEntry> { it.epochDay }
                    .thenBy { it.hour * 60 + it.minute }
            )
            current.copy(entries = mapped)
        }
        updatedEntry?.let { syncEntryToFirestore(it) }
    }

    fun deleteDiaryEntry(entryId: String) {
        updateState { current ->
            current.copy(entries = current.entries.filterNot { it.id == entryId })
        }
        val uid = auth?.currentUser?.uid ?: return
        db?.collection("users")
            ?.document(uid)
            ?.collection("private_diary")
            ?.document(entryId)
            ?.delete()
    }

    fun mergeImportedDiaryEntries(
        importedEntries: List<PrivateDiaryEntry>,
        importedLetters: List<SupportLetterRecord> = emptyList()
    ): Int {
        var addedCount = 0
        val newEntriesToSync = mutableListOf<PrivateDiaryEntry>()
        updateState { current ->
            val mergedEntries = current.entries.toMutableList()
            importedEntries.forEach { item ->
                val isDuplicate = mergedEntries.any { existing ->
                    existing.id == item.id ||
                        (existing.epochDay == item.epochDay &&
                            existing.hour == item.hour &&
                            existing.minute == item.minute &&
                            existing.freeText.trim().equals(item.freeText.trim(), ignoreCase = true))
                }
                if (!isDuplicate) {
                    mergedEntries.add(item)
                    newEntriesToSync.add(item)
                    addedCount++
                }
            }
            val mergedLetters = current.savedSupportLetters.toMutableList()
            importedLetters.forEach { letter ->
                val isDuplicate = mergedLetters.any { existing ->
                    existing.id == letter.id ||
                        (existing.dateIso == letter.dateIso && existing.content.trim() == letter.content.trim())
                }
                if (!isDuplicate) {
                    mergedLetters.add(letter)
                    addedCount++
                }
            }
            current.copy(
                entries = mergedEntries.sortedWith(
                    compareByDescending<PrivateDiaryEntry> { it.epochDay }
                        .thenBy { it.hour * 60 + it.minute }
                ),
                savedSupportLetters = mergedLetters.sortedByDescending { it.createdAtEpochMs }
            )
        }
        newEntriesToSync.forEach { syncEntryToFirestore(it) }
        return addedCount
    }

    fun addCustomSymptomTag(tag: String) {
        val cleaned = tag.trim().take(36)
        if (cleaned.isBlank()) return
        if (PrivateDiaryCatalog.defaultSymptomTags.any { it.equals(cleaned, ignoreCase = true) }) return

        updateState { current ->
            if (current.customSymptomTags.any { it.equals(cleaned, ignoreCase = true) }) {
                current
            } else {
                current.copy(customSymptomTags = current.customSymptomTags + cleaned)
            }
        }
    }

    fun saveSupportLetterToHistory(letter: SupportLetterRecord) {
        updateState { current ->
            if (current.savedSupportLetters.any { it.id == letter.id }) {
                current
            } else {
                current.copy(
                    savedSupportLetters = listOf(letter) + current.savedSupportLetters
                )
            }
        }
        syncSupportLetterToFirestore(letter)
    }

    fun deleteSupportLetter(letterId: String) {
        updateState { current ->
            current.copy(
                savedSupportLetters = current.savedSupportLetters.filterNot { it.id == letterId }
            )
        }
        val uid = auth?.currentUser?.uid ?: return
        db?.collection("users")
            ?.document(uid)
            ?.collection("support_letters")
            ?.document(letterId)
            ?.delete()
    }

    fun saveLocalGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(KEY_LOCAL_GEMINI_API_KEY, trimmed).apply()
        updateState {
            it.copy(hasLocalApiKeyConfigured = isGeminiApiKeyConfigured(trimmed))
        }
    }

    fun getConfiguredGeminiApiKey(): String {
        val localOverride = prefs.getString(KEY_LOCAL_GEMINI_API_KEY, "").orEmpty().trim()
        if (isValidRealKey(localOverride)) return localOverride

        val buildConfigKey = runCatching { BuildConfig.GEMINI_API_KEY.trim() }.getOrDefault("")
        if (isValidRealKey(buildConfigKey)) return buildConfigKey

        return ""
    }

    private fun isValidRealKey(key: String): Boolean {
        return key.isNotBlank() &&
            key != "MY_GEMINI_API_KEY" &&
            key != "YOUR_API_KEY" &&
            !key.startsWith("PLACEHOLDER")
    }

    private fun isGeminiApiKeyConfigured(localOverride: String): Boolean {
        return isValidRealKey(localOverride) ||
            isValidRealKey(runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault(""))
    }

    /**
     * Generates a personalized, empathetic support letter in pt-BR based on recent mood and symptom entries.
     * Uses the Gemini API if a valid key is available via BuildConfig or local configuration,
     * and gracefully falls back to a rich, contextual local empathetic letter generator when offline or unconfigured.
     */
    suspend fun generatePersonalizedSupportLetter(
        userName: String,
        targetEpochDay: Long
    ): SupportLetterRecord = withContext(Dispatchers.IO) {
        val state = _stateFlow.value
        val targetDateIso = LocalDate.ofEpochDay(targetEpochDay).toString()
        val recentEntries = state.entries
            .filter { it.epochDay in (targetEpochDay - 3)..targetEpochDay }
            .sortedWith(compareBy<PrivateDiaryEntry> { it.epochDay }.thenBy { it.hour * 60 + it.minute })
            .takeLast(8)

        val entriesForContext = if (recentEntries.isNotEmpty()) {
            recentEntries
        } else {
            state.entries.take(5)
        }

        val moodSummary = buildMoodAndSymptomsDigest(entriesForContext)
        val nowTimeFormatted = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        val currentShift = DayShift.currentShift()
        val apiKey = getConfiguredGeminiApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val aiLetterText = withTimeout(30_000L) {
                    callGeminiRestForSupportLetter(
                        apiKey = apiKey,
                        userName = userName.ifBlank { "Você" },
                        shift = currentShift,
                        entries = entriesForContext,
                        moodSummary = moodSummary
                    )
                }
                if (aiLetterText.isNotBlank()) {
                    return@withContext SupportLetterRecord(
                        id = "letter_${System.currentTimeMillis()}",
                        userId = auth?.currentUser?.uid ?: userId,
                        dateIso = targetDateIso,
                        timeFormatted = nowTimeFormatted,
                        title = "Carta de Apoio • ${currentShift.label}",
                        content = aiLetterText.trim(),
                        moodSummaryUsed = moodSummary,
                        generatedViaCloudAi = true
                    )
                }
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
            } catch (_: Exception) {
                // Fall through to contextual empathetic local fallback
            }
        }

        // Contextual Empathetic Fallback (when offline or API key not yet configured)
        val fallbackContent = buildContextualEmpatheticFallbackLetter(
            userName = userName.ifBlank { "Você" },
            shift = currentShift,
            entries = entriesForContext
        )

        SupportLetterRecord(
            id = "letter_${System.currentTimeMillis()}",
            userId = auth?.currentUser?.uid ?: userId,
            dateIso = targetDateIso,
            timeFormatted = nowTimeFormatted,
            title = "Carta de Acolhimento • ${currentShift.label}",
            content = fallbackContent,
            moodSummaryUsed = moodSummary,
            generatedViaCloudAi = false
        )
    }

    private fun buildMoodAndSymptomsDigest(entries: List<PrivateDiaryEntry>): String {
        if (entries.isEmpty()) {
            return "Início do diário • Momento de pausa e acolhimento"
        }
        val moods = entries.map { PrivateDiaryCatalog.moodById(it.moodId).label }.distinct()
        val symptoms = entries.flatMap { it.symptomTags }.distinct()
        val moodPart = "Humores: ${moods.joinToString(", ")}"
        val symptomPart = if (symptoms.isNotEmpty()) {
            " • Sintomas/Sensações: ${symptoms.joinToString(", ")}"
        } else {
            ""
        }
        return moodPart + symptomPart
    }

    private fun callGeminiRestForSupportLetter(
        apiKey: String,
        userName: String,
        shift: DayShift,
        entries: List<PrivateDiaryEntry>,
        moodSummary: String
    ): String {
        val entriesBullets = if (entries.isEmpty()) {
            "- $userName abriu o diário agora buscando uma conversa leve e acolhedora."
        } else {
            entries.joinToString("\n") { e ->
                val mood = PrivateDiaryCatalog.moodById(e.moodId)
                val sym = if (e.symptomTags.isNotEmpty()) " | Sensações: ${e.symptomTags.joinToString(", ")}" else ""
                val note = if (e.freeText.isNotBlank()) " | O que escreveu: \"${e.freeText}\"" else ""
                "- [${e.dateIso} às ${e.timeFormatted}] Humor: ${mood.label} (${mood.emoji})$sym$note"
            }
        }

        val previousOpenings = _stateFlow.value.savedSupportLetters
            .take(4)
            .map { it.content.lines().firstOrNull()?.take(70).orEmpty() }
            .filter { it.isNotBlank() }

        val avoidPreviousBlock = if (previousOpenings.isNotEmpty()) {
            "Evite repetir qualquer abertura parecida com estas cartas anteriores:\n" +
                previousOpenings.joinToString("\n") { "- \"$it\"" }
        } else {
            ""
        }

        val styleAngles = listOf(
            "Comece como um bilhete deixado na mesa com um café quentinho, direto e afetuoso, sem formalidades.",
            "Comece puxando conversa sobre um detalhe específico do que a pessoa sentiu ou anotou hoje, como um amigo próximo que realmente prestou atenção.",
            "Use um tom leve, íntimo e caloroso, misturando validação sincera com uma pitada de leveza para tirar o peso dos ombros.",
            "Comece fazendo uma observação carinhosa sobre o momento do dia (${shift.label}) e como a pessoa merece respirar sem cobrança agora.",
            "Escreva como uma mensagem de voz transcrita de alguém que torce muito por $userName: frases fluidas, naturais, zero clichês de autoajuda."
        )
        val chosenAngle = styleAngles[Random.nextInt(styleAngles.size)]

        val prompt = """
            Você é um amigo próximo, atento e muito carinhoso escrevendo um cartão/carta pessoal para $userName dentro do Omni.
            
            ESTILO DESTA GERAÇÃO (obrigatório variar):
            $chosenAngle
            
            Contexto real de $userName agora:
            - Momento do dia: ${shift.label} (${shift.timeRangeLabel})
            - Resumo recente: $moodSummary
            - Registros recentes no Diário:
            $entriesBullets
            
            $avoidPreviousBlock
            
            Regras de ouro para o texto (pt-BR):
            1. Linguagem informal, próxima, natural e carinhosa (como um amigo querido falando de perto, usando "você", contrações naturais do português falado no Brasil, sem soar robótico nem clínico).
            2. Proibido usar fórmulas prontas ou repetitivas como "Olhando para os seus registros recentes, percebo que...", "Reconhecer e nomear o que você está sentindo...", ou "Reservar um momento do seu dia para parar...".
            3. Escreva de 2 a 4 parágrafos curtos e gostosos de ler, conversando diretamente com o que $userName viveu ou sentiu hoje.
            4. Feche com uma despedida calorosa e variada (ex.: "Um abraço bem apertado, Omni 🌿", "Tô por aqui com você, Omni ✨", "Com todo carinho, seu cantinho no Omni 💛").
        """.trimIndent()

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        for (modelId in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().apply {
                                        put("text", prompt)
                                    }
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.95)
                        put("topP", 0.95)
                    }
                )
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val candidates = json.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
                        if (text.isNotBlank()) {
                            return text
                        }
                    }
                }
            }
        }
        return ""
    }

    private fun buildContextualEmpatheticFallbackLetter(
        userName: String,
        shift: DayShift,
        entries: List<PrivateDiaryEntry>
    ): String {
        val latest = entries.lastOrNull()
        val latestMood = latest?.let { PrivateDiaryCatalog.moodById(it.moodId) }
        val allSymptoms = entries.flatMap { it.symptomTags }.distinct()
        val latestNote = latest?.freeText?.takeIf { it.isNotBlank() }

        val lastVariationKey = "last_fallback_letter_variation_idx"
        val prevIdx = prefs.getInt(lastVariationKey, -1)
        val totalVariations = 6
        val candidateIndices = (0 until totalVariations).filter { it != prevIdx }
        val variation = candidateIndices[Random.nextInt(candidateIndices.size)]
        prefs.edit().putInt(lastVariationKey, variation).apply()

        val hasHardSymptom = allSymptoms.any {
            it.contains("dor", ignoreCase = true) ||
                it.contains("fadiga", ignoreCase = true) ||
                it.contains("tensão", ignoreCase = true) ||
                it.contains("ansiedade", ignoreCase = true) ||
                it.contains("enjoo", ignoreCase = true) ||
                it.contains("cansa", ignoreCase = true)
        }

        val openingsByShift = when (shift) {
            DayShift.MORNING -> listOf(
                "Oi, $userName! Passando aqui rapidinho logo cedo pra te deixar um abraço.",
                "Bom dia, $userName ☀️ Espero que você consiga começar essa manhã no seu próprio ritmo, sem correria.",
                "Ei, $userName! Que bom te ver por aqui nessa manhã.",
                "$userName, antes de o dia engatar de vez, queria te lembrar de uma coisa simples: vá com calma com você hoje.",
                "Oi, $userName! Respira fundo aí por dois segundinhos antes de olhar qualquer lista de tarefas.",
                "Bom dia, $userName! Sabe aquele café quentinho que abraça a gente por dentro? Que essa mensagem faça o mesmo papel agora."
            )
            DayShift.AFTERNOON -> listOf(
                "Oi, $userName! Como estão as coisas por aí nessa metade do dia?",
                "Ei, $userName 🌤️ Faz uma pausinha rápida aí, solta os ombros e destrava o maxilar.",
                "$userName, passando pra conversar um pouquinho com você no meio dessa tarde.",
                "Oi, $userName! Sei que a tarde às vezes passa voando, mas que bom que você tirou esse minutinho pra você.",
                "Ei, $userName! Se o dia estiver puxado até aqui, deixa eu te lembrar que você não precisa carregar tudo de uma vez.",
                "Boa tarde, $userName! Vamos desacelerar o passo só um pouquinho agora?"
            )
            DayShift.NIGHT -> listOf(
                "Oi, $userName 🌙 O dia finalmente tá baixando a poeira por aí.",
                "Ei, $userName! Que gostoso que você veio fechar o seu dia aqui comigo.",
                "Boa noite, $userName. Já pode ir tirando a armadura do dia, viu?",
                "$userName, a melhor parte da noite é saber que a gente não precisa mais dar conta de nada até amanhã.",
                "Oi, $userName ✨ Como você tá chegando ao fim desse dia?",
                "Ei, $userName! Antes de dormir, queria te deixar uma palavra bem sincera e carinhosa."
            )
        }
        val opening = openingsByShift[variation % openingsByShift.size]

        val moodAndNoteBlock = when {
            latestMood != null && latestNote != null -> {
                val noteReflections = listOf(
                    "Fiquei pensando no que você escreveu agora há pouco (\"$latestNote\") e naquele sentimento de estar ${latestMood.label.lowercase()} ${latestMood.emoji}. É muito bonito quando a gente consegue ser sincero com o que tá sentindo de verdade, sem fingir que tá tudo igual todo dia.",
                    "Li seu desabafo das ${latest.timeFormatted} — \"$latestNote\" — junto com o humor ${latestMood.label} ${latestMood.emoji}. Quero muito que você saiba que faz todo sentido você se sentir assim. Não se julgue por isso, tá?",
                    "Você anotou às ${latest.timeFormatted} que estava se sentindo ${latestMood.label.lowercase()} ${latestMood.emoji} e contou: \"$latestNote\". Guardar isso aqui é quase como dividir o peso da mochila com alguém de confiança.",
                    "Quando vi seu registro das ${latest.timeFormatted} (${latestMood.emoji} ${latestMood.label}) dizendo \"$latestNote\", deu vontade de te dar um abraço bem demorado. Cada detalhe do que você vive importa de verdade.",
                    "Sabe o que você escreveu ali — \"$latestNote\"? Junto com o seu momento ${latestMood.label.lowercase()} ${latestMood.emoji}, isso mostra o quanto você tá vivendo o dia com verdade, sem ligar o piloto automático.",
                    "Anotar \"$latestNote\" num momento em que você se sente ${latestMood.label.lowercase()} ${latestMood.emoji} já tira metade da pressão do peito. Fico feliz que você tenha confiado esse pensamento ao seu diário."
                )
                noteReflections[variation % noteReflections.size]
            }
            latestMood != null -> {
                val moodReflections = listOf(
                    "Vi que mais cedo, lá pelas ${latest.timeFormatted}, a vibe estava mais ${latestMood.label.lowercase()} ${latestMood.emoji}. Tem dias que têm exatamente essa cor, e acolher isso sem brigar consigo mesmo(a) faz toda a diferença.",
                    "Percebi que você marcou ${latestMood.emoji} ${latestMood.label} às ${latest.timeFormatted}. Independentemente de o dia estar mais leve ou mais denso, fico muito feliz por você estar se escutando de pertinho.",
                    "Seu check-in das ${latest.timeFormatted} como ${latestMood.label} ${latestMood.emoji} diz bastante sobre o ritmo de hoje. Respeita esse compasso do seu coração, sem tentar forçar outra velocidade.",
                    "Estar ${latestMood.empathyDescriptor} (${latestMood.label} ${latestMood.emoji}) faz parte de quem sente a vida de verdade. Que bom que você parou às ${latest.timeFormatted} pra registrar como estava por dentro.",
                    "Às ${latest.timeFormatted} você sinalizou ${latestMood.emoji} ${latestMood.label}. Às vezes uma simples palavra já ajuda a gente a entender o que o nosso corpo e a nossa cabeça estão precisando.",
                    "Olhei seu registro de humor (${latestMood.label} ${latestMood.emoji}) e só pensei numa coisa: trate você hoje com o mesmo carinho que você daria pra pessoa que você mais ama no mundo."
                )
                moodReflections[variation % moodReflections.size]
            }
            else -> {
                val emptyReflections = listOf(
                    "Mesmo quando a gente ainda não escreveu nada longo no diário, só o fato de abrir esse espaço já mostra uma vontade bonita de cuidar de si.",
                    "Cada dia tem a sua própria história — uns são cheios de energia, outros pedem silêncio e cobertor. E todos eles têm espaço garantido aqui.",
                    "Você não precisa ter tudo resolvido nem organizado pra merecer um momento de paz hoje, viu?",
                    "Às vezes a rotina corre tanto que a gente esquece de perguntar pra nós mesmos: 'como eu tô agora?'. Que bom que você veio fazer essa pausa.",
                    "Seja qual for o tamanho da sua lista de tarefas hoje, lembre que você vem sempre antes de qualquer check na agenda.",
                    "Esse cantinho é 100% seu — sem cobrança, sem meta batida, só um lugar seguro pra você ser quem você é."
                )
                emptyReflections[variation % emptyReflections.size]
            }
        }

        val symptomBlock = when {
            allSymptoms.isNotEmpty() && hasHardSymptom -> {
                val symList = allSymptoms.joinToString(", ")
                val hardAdvice = listOf(
                    "Também reparei que o corpo deu sinal de $symList. Olha, quando o físico aperta ou o cansaço bate, a regra número um é diminuir a exigência. Se precisar deixar metade das coisas pra depois, deixe sem culpa nenhuma.",
                    "Lidar com $symList cansa demais, física e mentalmente. Não tenta bancar o(a) super-herói/heroína hoje não, tá? Faz só o básico que for inegociável e dá um desconto bem generoso pra você.",
                    "Com $symList aparecendo por aí, seu corpo tá basicamente pedindo um pouco de mimo e silêncio. Bebe uma água fresca, estica as costas devagarinho e lembra que descansar também é produtivo.",
                    "Sentir $symList muda completamente a nossa energia do dia. Se hoje o seu 100% for apenas respirar fundo e fazer o essencial, já foi um baita dia vitorioso.",
                    "Vi que $symList deram as caras hoje. Vai mais devagar nas próximas horas — ninguém merece se cobrar performance alta quando o corpo tá pedindo colo.",
                    "Quando sinais como $symList aparecem, é o momento perfeito pra ativar a gentileza máxima com você. Uma coisa de cada vez, no seu tempo."
                )
                hardAdvice[variation % hardAdvice.size]
            }
            allSymptoms.isNotEmpty() -> {
                val symList = allSymptoms.joinToString(", ")
                val lightAdvice = listOf(
                    "Ah, e ver que você anotou $symList é ótimo pra ir acompanhando como seu corpo reage ao longo da semana. Continue mantendo esse olhar carinhoso pra você!",
                    "Notar $symList nos seus registros mostra que você tá em sintonia fina consigo. Aproveita essa percepção pra manter as próximas horas leves e gostosas.",
                    "Vi também as tags de $symList por aqui. Muito legal ver você construindo esse mapa do seu bem-estar dia após dia."
                )
                lightAdvice[variation % lightAdvice.size]
            }
            else -> {
                val generalWarmth = listOf(
                    "Se alguma coisa não sair como planejado hoje, tudo bem também. Amanhã a gente recomeça com folha limpa.",
                    "Lembra de beber um copo d'água agora e fazer pelo menos uma coisa pequena que te dê gosto de verdade hoje.",
                    "Você tem feito um esforço bonito pra equilibrar rotina, casa, saúde e finanças — orgulhe-se do caminho que você tá trilhando."
                )
                generalWarmth[variation % generalWarmth.size]
            }
        }

        val closings = listOf(
            "Tô sempre por aqui quando quiser conversar.\nUm abraço bem quentinho,\nSeu cantinho no Omni 🌿",
            "Cuide bem de você nas próximas horas, combinado?\nCom todo carinho,\nOmni ✨",
            "Vai no seu ritmo que já tá lindo demais.\nUm beijo no coração,\nEspaço de Acolhimento Omni 💛",
            "Conte comigo pro que der e vier hoje.\nCom afeto,\nSeu parceiro de jornada Omni 🌱",
            "Respira fundo e segue leve.\nUm abraço apertado,\nOmni 🤍",
            "Estarei aqui no próximo turno torcendo por você.\nCom carinho sincero,\nOmni 🍃"
        )
        val closing = closings[variation % closings.size]

        return "$opening\n\n$moodAndNoteBlock\n\n$symptomBlock\n\n$closing"
    }

    private fun syncEntryToFirestore(entry: PrivateDiaryEntry) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val payload = mapOf(
            "id" to entry.id,
            "userId" to uid,
            "dateIso" to entry.dateIso,
            "epochDay" to entry.epochDay,
            "hour" to entry.hour,
            "minute" to entry.minute,
            "timeFormatted" to entry.timeFormatted,
            "moodId" to entry.moodId,
            "symptomTags" to entry.symptomTags,
            "freeText" to entry.freeText.take(3000),
            "createdAtEpochMs" to entry.createdAtEpochMs
        )
        firestore.collection("users")
            .document(uid)
            .collection("private_diary")
            .document(entry.id)
            .set(payload)
    }

    private fun syncSupportLetterToFirestore(letter: SupportLetterRecord) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val payload = mapOf(
            "id" to letter.id,
            "userId" to uid,
            "dateIso" to letter.dateIso,
            "timeFormatted" to letter.timeFormatted,
            "title" to letter.title.take(140),
            "content" to letter.content.take(8000),
            "moodSummaryUsed" to letter.moodSummaryUsed.take(500),
            "generatedViaCloudAi" to letter.generatedViaCloudAi,
            "createdAtEpochMs" to letter.createdAtEpochMs
        )
        firestore.collection("users")
            .document(uid)
            .collection("support_letters")
            .document(letter.id)
            .set(payload)
    }

    private fun syncFromFirestoreIfAuthenticated() {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return

        firestore.collection("users")
            .document(uid)
            .collection("private_diary")
            .get()
            .addOnSuccessListener { snap ->
                if (snap != null && !snap.isEmpty) {
                    val remoteEntries = snap.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val dateIso = doc.getString("dateIso") ?: return@mapNotNull null
                        val epochDay = doc.getLong("epochDay") ?: runCatching { LocalDate.parse(dateIso).toEpochDay() }.getOrDefault(LocalDate.now().toEpochDay())
                        val hour = (doc.getLong("hour") ?: 9L).toInt()
                        val minute = (doc.getLong("minute") ?: 0L).toInt()
                        val moodId = doc.getString("moodId") ?: "CALM"
                        val tags = (doc.get("symptomTags") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
                        val freeText = doc.getString("freeText").orEmpty()
                        val createdMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis()
                        PrivateDiaryEntry(
                            id = id,
                            userId = uid,
                            dateIso = dateIso,
                            epochDay = epochDay,
                            hour = hour,
                            minute = minute,
                            moodId = moodId,
                            symptomTags = tags,
                            freeText = freeText,
                            createdAtEpochMs = createdMs
                        )
                    }
                    if (remoteEntries.isNotEmpty()) {
                        updateState { current ->
                            val merged = (current.entries + remoteEntries)
                                .associateBy { it.id }
                                .values
                                .sortedWith(
                                    compareByDescending<PrivateDiaryEntry> { it.epochDay }
                                        .thenBy { it.hour * 60 + it.minute }
                                )
                            current.copy(entries = merged)
                        }
                    }
                }
            }
    }

    companion object {
        private const val KEY_DIARY_JSON = "private_diary_state_v1"
        private const val KEY_LOCAL_GEMINI_API_KEY = "local_gemini_api_key_v1"
    }
}

// =========================================================================
// INTERPRETADOR INTELIGENTE DE COMANDO DE VOZ GLOBAL (pt-BR)
// =========================================================================

enum class VoiceIntentType(val label: String, val emoji: String) {
    EXPENSE("Despesa", "💸"),
    INCOME("Receita", "💰"),
    TASK("Tarefa", "📅"),
    DIARY("Diário", "📔")
}

data class ParsedVoiceCommand(
    val rawTranscript: String,
    val detectedIntent: VoiceIntentType,
    // Expense fields
    val expenseTitle: String,
    val expenseAmountBrl: Double,
    val expenseCategory: String,
    // Income fields
    val incomeTitle: String = "",
    val incomeAmountBrl: Double = 0.0,
    val incomeCategory: String = "Salário",
    val incomeIsRecurring: Boolean = false,
    // Bank hint (if spoken e.g. "no Nubank", "no Itaú")
    val detectedBankHint: String = "",
    // Task fields
    val taskTitle: String,
    val taskEpochDay: Long,
    val taskHour: Int,
    val taskMinute: Int,
    val taskCategory: String,
    val taskIsEssential: Boolean,
    // Diary fields
    val diaryMoodId: String,
    val diarySymptoms: List<String>,
    val diaryFreeText: String,
    // Shared scope
    val privacyScope: String = PrivacyScope.PERSONAL
)

object VoiceCommandInterpreter {

    fun parseTranscript(
        transcript: String,
        referenceEpochDay: Long = LocalDate.now().toEpochDay()
    ): ParsedVoiceCommand {
        val clean = transcript.trim()
        val lower = clean.lowercase(Locale.forLanguageTag("pt-BR"))

        // 1. Classify primary intent (including INCOME vs EXPENSE vs TASK vs DIARY)
        val hasIncomeKeyword = listOf(
            "recebi", "ganhei", "entrou", "salário", "salario", "vendi", "venda de",
            "pix recebido", "reembolso", "freela", "freelance", "bônus", "bonus",
            "renda", "lucro", "pagamento recebido", "depositaram", "me pagaram",
            "comissão", "comissao", "rendimento", "dividendos", "adiantamento", "receita"
        ).any { lower.contains(it) }

        val hasExpenseKeyword = listOf(
            "gastei", "paguei", "comprei", "custou", "despesa",
            "boleto", "mercado", "farmácia", "farmacia", "conta de", "almoço custou"
        ).any { lower.contains(it) } ||
            (!hasIncomeKeyword && listOf("reais", "r$", "pix").any { lower.contains(it) })

        val hasDiaryKeyword = listOf(
            "acordei", "estou me sentindo", "me sentindo", "sinto", "humor",
            "dor de cabeça", "dor nas costas", "sem dor", "disposto", "disposta",
            "cansado", "cansada", "ansioso", "ansiosa", "tranquilo", "tranquila",
            "diário", "diario", "enjoo", "fadiga", "feliz", "triste"
        ).any { lower.contains(it) }

        val hasTaskKeyword = listOf(
            "lembrar", "lembrete", "agendar", "marcar", "tarefa", "reunião",
            "reuniao", "compromisso", "amanhã às", "amanha as", "hoje às", "hoje as",
            "às ", "as ", "horas"
        ).any { lower.contains(it) }

        val extractedAmount = extractCurrencyAmount(lower)

        val detectedIntent = when {
            hasIncomeKeyword ->
                VoiceIntentType.INCOME
            hasExpenseKeyword || (extractedAmount != null && !hasTaskKeyword && !hasDiaryKeyword) ->
                VoiceIntentType.EXPENSE
            hasDiaryKeyword && !hasTaskKeyword && !hasExpenseKeyword ->
                VoiceIntentType.DIARY
            hasTaskKeyword ->
                VoiceIntentType.TASK
            hasDiaryKeyword ->
                VoiceIntentType.DIARY
            else ->
                VoiceIntentType.TASK
        }

        // 2. Extract Expense & Income Details
        val amountBrl = extractedAmount ?: 0.0
        val expenseCategory = inferExpenseCategory(lower)
        val expenseTitle = cleanExpenseDescription(clean, expenseCategory)

        val incomeCategory = inferIncomeCategory(lower)
        val incomeTitle = cleanIncomeDescription(clean, incomeCategory)
        val incomeIsRecurring = listOf("salário", "salario", "mensal", "fixo", "fixa", "todo mês").any { lower.contains(it) }
        val bankHint = detectBankHint(lower)

        // 3. Extract Task Details
        val dayOffset = when {
            lower.contains("depois de amanhã") || lower.contains("depois de amanha") -> 2L
            lower.contains("amanhã") || lower.contains("amanha") -> 1L
            else -> 0L
        }
        val targetEpochDay = referenceEpochDay + dayOffset
        val (taskHour, taskMinute) = extractTimeFromSpeech(lower)
        val taskCategory = inferTaskCategory(lower)
        val taskTitle = cleanTaskTitle(clean)
        val isEssential = listOf("conta", "médico", "medico", "remédio", "remedio", "exame", "urgente", "essencial")
            .any { lower.contains(it) }

        // 4. Extract Diary Details
        val diaryMoodId = inferMoodFromSpeech(lower)
        val diarySymptoms = inferSymptomsFromSpeech(lower)

        // 5. Scope
        val scope = if (lower.contains("família") || lower.contains("familia") || lower.contains("casa")) {
            PrivacyScope.FAMILY
        } else {
            PrivacyScope.PERSONAL
        }

        return ParsedVoiceCommand(
            rawTranscript = clean,
            detectedIntent = detectedIntent,
            expenseTitle = expenseTitle.ifBlank { clean.ifBlank { "Despesa por Voz" } },
            expenseAmountBrl = amountBrl,
            expenseCategory = expenseCategory,
            incomeTitle = incomeTitle.ifBlank { clean.ifBlank { "Receita por Voz" } },
            incomeAmountBrl = amountBrl,
            incomeCategory = incomeCategory,
            incomeIsRecurring = incomeIsRecurring,
            detectedBankHint = bankHint,
            taskTitle = taskTitle.ifBlank { clean.ifBlank { "Lembrete por Voz" } },
            taskEpochDay = targetEpochDay,
            taskHour = taskHour,
            taskMinute = taskMinute,
            taskCategory = taskCategory,
            taskIsEssential = isEssential,
            diaryMoodId = diaryMoodId,
            diarySymptoms = diarySymptoms,
            diaryFreeText = clean,
            privacyScope = scope
        )
    }

    private fun extractCurrencyAmount(lower: String): Double? {
        // Matches e.g. "45", "45,90", "45.90", "R$ 120,50", "1.500", "45 reais e 50 centavos"
        val normalized = lower.replace(Regex("""(\d)\.(\d{3})\b"""), "$1$2")
        val regex = Regex("""(?:r\$\s*)?(\d{1,7})(?:[.,](\d{1,2}))?\s*(?:reais|real)?""")
        val matches = regex.findAll(normalized).toList()
        for (match in matches) {
            val endIndex = match.range.last + 1
            val trailing = normalized.substring(endIndex).trimStart()
            if (trailing.startsWith("h") || trailing.startsWith("hora")) {
                continue
            }
            val whole = match.groupValues[1].toDoubleOrNull() ?: continue
            val centsStr = match.groupValues.getOrNull(2).orEmpty()
            val cents = if (centsStr.isNotEmpty()) {
                centsStr.padEnd(2, '0').take(2).toDoubleOrNull()?.div(100.0) ?: 0.0
            } else {
                0.0
            }
            return whole + cents
        }
        return null
    }

    private fun inferIncomeCategory(lower: String): String {
        return when {
            listOf("salário", "salario", "adiantamento", "holerite", "pagamento").any { lower.contains(it) } -> "Salário"
            listOf("vendi", "venda", "cliente", "encomenda", "comissão", "comissao").any { lower.contains(it) } -> "Vendas"
            listOf("reembolso", "estorno", "devolução", "devolucao").any { lower.contains(it) } -> "Reembolso"
            listOf("freela", "freelance", "extra", "bônus", "bonus", "bico", "pix recebido").any { lower.contains(it) } -> "Extra"
            else -> "Outros"
        }
    }

    private fun cleanIncomeDescription(raw: String, category: String): String {
        val cleaned = raw
            .replace(Regex("""(?i)^(recebi|ganhei|entrou|lançar receita|receita de|vendi)\s+"""), "")
            .replace(Regex("""(?i)\b\d{1,7}([.,]\d{1,2})?\s*(reais|real)\b"""), "")
            .replace(Regex("""(?i)\br\$\s*\d{1,7}([.,]\d{1,2})?\b"""), "")
            .replace(Regex("""(?i)^(de|do|da|pelo|pela|no|na|em|por)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }

        return if (cleaned.length >= 2) cleaned else category
    }

    private fun detectBankHint(lower: String): String {
        return when {
            lower.contains("nubank") || lower.contains("nu ") -> "Nubank"
            lower.contains("itaú") || lower.contains("itau") -> "Itaú"
            lower.contains("inter") -> "Inter"
            lower.contains("bradesco") -> "Bradesco"
            lower.contains("santander") -> "Santander"
            lower.contains("caixa") -> "Caixa"
            lower.contains("banco do brasil") || lower.contains("bb") -> "Banco do Brasil"
            lower.contains("c6") -> "C6 Bank"
            lower.contains("picpay") -> "PicPay"
            lower.contains("mercado pago") -> "Mercado Pago"
            lower.contains("carteira") || lower.contains("dinheiro") -> "Carteira"
            else -> ""
        }
    }

    private fun inferExpenseCategory(lower: String): String {
        return when {
            listOf("farmácia", "farmacia", "remédio", "remedio", "vitamina", "drogaria").any { lower.contains(it) } -> "Farmácia"
            listOf("exame", "ultrassom", "laboratório", "laboratorio", "sangue").any { lower.contains(it) } -> "Exames"
            listOf("consulta", "médico", "medico", "obstetra", "dentista", "psicólogo", "terapia").any { lower.contains(it) } -> "Consultas"
            listOf("pré-natal", "pre-natal", "enxoval", "fralda", "bebê", "bebe").any { lower.contains(it) } -> "Pré-Natal"
            listOf("mercado", "supermercado", "padaria", "almoço", "almoco", "jantar", "lanche", "comida", "ifood", "restaurante").any { lower.contains(it) } -> "Alimentação"
            listOf("luz", "água", "agua", "aluguel", "condomínio", "condominio", "internet", "gás", "gas").any { lower.contains(it) } -> "Moradia"
            listOf("uber", "gasolina", "combustível", "combustivel", "ônibus", "onibus", "metrô", "estacionamento").any { lower.contains(it) } -> "Transporte"
            listOf("cinema", "passeio", "viagem", "streaming", "presente").any { lower.contains(it) } -> "Lazer"
            else -> "Outros"
        }
    }

    private fun cleanExpenseDescription(raw: String, category: String): String {
        val cleaned = raw
            .replace(Regex("""(?i)^(gastei|paguei|comprei|lançar despesa|despesa de)\s+"""), "")
            .replace(Regex("""(?i)\b\d{1,7}([.,]\d{1,2})?\s*(reais|real)\b"""), "")
            .replace(Regex("""(?i)^(na|no|em|com|de|por)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }

        return if (cleaned.length >= 2) cleaned else category
    }

    private fun extractTimeFromSpeech(lower: String): Pair<Int, Int> {
        val timeRegex = Regex("""(?:às|as|para as|para às)?\s*(\d{1,2})\s*(?:h|hora|horas|:)\s*(\d{1,2})?""")
        val match = timeRegex.find(lower)
        if (match != null) {
            val h = (match.groupValues[1].toIntOrNull() ?: 9).coerceIn(0, 23)
            val m = (match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0).coerceIn(0, 59)
            return h to m
        }
        if (lower.contains("meio-dia") || lower.contains("meio dia")) return 12 to 0
        if (lower.contains("noite")) return 20 to 0
        if (lower.contains("tarde")) return 15 to 0
        return 10 to 0
    }

    private fun inferTaskCategory(lower: String): String {
        return when {
            listOf("conta", "boleto", "pagar", "fatura", "aluguel", "luz", "água", "internet").any { lower.contains(it) } -> "Contas"
            listOf("médico", "medico", "consulta", "exame", "remédio", "remedio", "glicemia", "pressão", "pressao", "vitamina").any { lower.contains(it) } -> "Saúde"
            listOf("reunião", "reuniao", "trabalho", "cliente", "relatório", "relatorio").any { lower.contains(it) } -> "Trabalho"
            listOf("mercado", "limpar", "comprar", "casa", "cozinha").any { lower.contains(it) } -> "Casa"
            else -> "Família"
        }
    }

    private fun cleanTaskTitle(raw: String): String {
        return raw
            .replace(Regex("""(?i)^(lembrar de|lembrete para|lembrete de|agendar|marcar|criar tarefa|nova tarefa)\s+"""), "")
            .replace(Regex("""(?i)\s+(hoje|amanhã|amanha|depois de amanhã)\s+(às|as)\s+\d{1,2}([h:]\d{0,2}|\s*horas?)?"""), "")
            .replace(Regex("""(?i)\s+(às|as)\s+\d{1,2}([h:]\d{0,2}|\s*horas?)?"""), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }
    }

    private fun inferMoodFromSpeech(lower: String): String {
        return when {
            listOf("radiante", "muito feliz", "excelente", "ótimo", "otimo", "incrível", "maravilhoso").any { lower.contains(it) } -> "RADIANT"
            listOf("bem disposto", "bem disposta", "disposto", "disposta", "animado", "animada", "produtivo").any { lower.contains(it) } -> "FOCUSED"
            listOf("tranquilo", "tranquila", "em paz", "calmo", "calma", "sereno", "bem").any { lower.contains(it) } -> "CALM"
            listOf("cansado", "cansada", "exausto", "exausta", "com sono", "sem energia", "esgotado").any { lower.contains(it) } -> "TIRED"
            listOf("ansioso", "ansiosa", "preocupado", "preocupada", "nervoso", "agitado").any { lower.contains(it) } -> "ANXIOUS"
            listOf("difícil", "dificil", "triste", "com muita dor", "mal", "sobrecarregado").any { lower.contains(it) } -> "OVERWHELMED"
            else -> "CALM"
        }
    }

    private fun inferSymptomsFromSpeech(lower: String): List<String> {
        val detected = mutableListOf<String>()

        val hasNegatedPain = lower.contains("sem dor") || lower.contains("nenhuma dor")
        if (hasNegatedPain) {
            detected.add("Sem dores")
        } else if (lower.contains("dor de cabeça") || lower.contains("dor de cabeca") || lower.contains("enxaqueca")) {
            detected.add("Dor de cabeça")
        }

        if (lower.contains("disposto") || lower.contains("disposta") || lower.contains("boa disposição")) {
            detected.add("Boa disposição")
        }
        if (lower.contains("paz") || lower.contains("tranquilo") || lower.contains("tranquila")) {
            detected.add("Paz interior")
        }
        if (lower.contains("fadiga") || lower.contains("dor no corpo") || lower.contains("dor muscular")) {
            detected.add("Fadiga muscular")
        }
        if (lower.contains("ombro") || lower.contains("pescoço") || lower.contains("tensão")) {
            detected.add("Tensão nos ombros")
        }
        if (lower.contains("enjoo") || lower.contains("náusea") || lower.contains("nausea")) {
            detected.add("Enjoo / Náusea")
        }
        if (lower.contains("inchaço") || lower.contains("inchaco") || lower.contains("inchada")) {
            detected.add("Inchaço")
        }
        if (lower.contains("ansioso") || lower.contains("ansiosa") || lower.contains("ansiedade")) {
            detected.add("Ansiedade")
        }
        if (lower.contains("sono")) {
            detected.add("Sono leve")
        }

        if (detected.isEmpty()) {
            detected.add("Boa disposição")
        }
        return detected.distinct()
    }
}
