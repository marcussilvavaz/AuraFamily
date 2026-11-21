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
        val nowTimeFormatted = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date())
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
            "- O usuário abriu o diário agora buscando uma palavra de acolhimento."
        } else {
            entries.joinToString("\n") { e ->
                val mood = PrivateDiaryCatalog.moodById(e.moodId)
                val sym = if (e.symptomTags.isNotEmpty()) " | Sintomas: ${e.symptomTags.joinToString(", ")}" else ""
                val note = if (e.freeText.isNotBlank()) " | Relato: \"${e.freeText}\"" else ""
                "- [${e.dateIso} às ${e.timeFormatted}] Humor: ${mood.label} (${mood.emoji})$sym$note"
            }
        }

        val prompt = """
            Você é um companheiro de jornada empático, caloroso e acolhedor dentro do aplicativo Aura Finanças & Saúde.
            Escreva uma "Carta de Apoio" curta (3 a 4 parágrafos breves), profundamente humana, gentil e reconfortante em português do Brasil (pt-BR) para $userName.
            
            Contexto atual:
            - Turno atual: ${shift.label} (${shift.timeRangeLabel})
            - Resumo recente: $moodSummary
            - Registros recentes do Diário Privado por hora:
            $entriesBullets
            
            Diretrizes importantes:
            1. Valide os sentimentos e eventuais sintomas físicos/emocionais relatados sem dar diagnósticos médicos.
            2. Ofereça um acolhimento sincero, lembrando que dias difíceis ou cansativos não anulam o valor da jornada.
            3. Sugira um pequeno gesto gentil de autocuidado para as próximas horas (como hidratar-se, respirar fundo ou respeitar o próprio limite).
            4. Assine no final de forma afetuosa como "Com carinho, seu Espaço de Acolhimento Aura".
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

        val greeting = when (shift) {
            DayShift.MORNING -> "Bom dia, $userName."
            DayShift.AFTERNOON -> "Olá, $userName. Que esta tarde encontre um espaço de calma em você."
            DayShift.NIGHT -> "Boa noite, $userName. Que bom que você reservou este instante para si."
        }

        val moodParagraph = if (latestMood != null) {
            "Olhando para os seus registros recentes, percebo que você esteve ${latestMood.empathyDescriptor}, marcando o humor \"${latestMood.label}\" ${latestMood.emoji} às ${latest.timeFormatted}. " +
                if (latestNote != null) {
                    "Ao ler suas palavras — \"$latestNote\" — quero lembrar que cada sensação que você registra aqui importa e merece ser ouvida sem julgamentos."
                } else {
                    "Reconhecer e nomear o que você está sentindo ao longo das horas já é um gesto poderoso de presença e cuidado consigo."
                }
        } else {
            "Reservar um momento do seu dia para parar, respirar e observar como o seu corpo e o seu coração estão é um dos maiores gestos de respeito pela sua própria história."
        }

        val symptomParagraph = if (allSymptoms.isNotEmpty()) {
            val hasPainOrFatigue = allSymptoms.any {
                it.contains("dor", ignoreCase = true) ||
                    it.contains("fadiga", ignoreCase = true) ||
                    it.contains("tensão", ignoreCase = true) ||
                    it.contains("ansiedade", ignoreCase = true) ||
                    it.contains("enjoo", ignoreCase = true) ||
                    it.contains("cansa", ignoreCase = true)
            }
            if (hasPainOrFatigue) {
                "Vi também que você sinalizou sensações como ${allSymptoms.joinToString(", ")}. Quando o corpo dá sinais de desconforto ou cansaço, ele não está falhando com você — ele está apenas pedindo gentileza, pausa e um ritmo mais macio. Não se cobre produtividade máxima hoje; focar apenas no que é essencial já é uma grande vitória."
            } else {
                "Notar registros como ${allSymptoms.joinToString(", ")} mostra o quanto você está atento(a) ao seu bem-estar. Aproveite essa sintonia com o seu corpo para preservar a sua energia com carinho, sem sobrecarregar sua rotina."
            }
        } else {
            "Independentemente de como a lista de tarefas esteja hoje, lembre-se de que o seu valor nunca é medido pela quantidade de coisas que você risca da agenda."
        }

        val closingShiftAdvice = when (shift) {
            DayShift.MORNING -> "Para o restante desta manhã, escolha apenas um passo de cada vez, beba um copo de água fresca e permita-se caminhar sem pressa."
            DayShift.AFTERNOON -> "Para as próximas horas da tarde, solte o peso dos ombros, faça três respirações lentas e trate a si mesmo(a) com a mesma paciência que teria com alguém que você ama."
            DayShift.NIGHT -> "Agora que a noite chegou, desacelere aos poucos. Tudo o que pôde ser feito hoje já foi entregue; permita que o seu descanso seja leve e reparador."
        }

        return "$greeting\n\n$moodParagraph\n\n$symptomParagraph\n\n$closingShiftAdvice\n\nCom carinho,\nSeu Espaço de Acolhimento Aura 🌿"
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
    EXPENSE("Despesa / Financeiro", "💸"),
    TASK("Tarefa / Agenda", "📅"),
    DIARY("Entrada no Diário", "📔")
}

data class ParsedVoiceCommand(
    val rawTranscript: String,
    val detectedIntent: VoiceIntentType,
    // Expense fields
    val expenseTitle: String,
    val expenseAmountBrl: Double,
    val expenseCategory: String,
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
        val lower = clean.lowercase(Locale("pt", "BR"))

        // 1. Classify primary intent
        val hasExpenseKeyword = listOf(
            "gastei", "paguei", "comprei", "custou", "reais", "r$", "despesa",
            "pix", "boleto", "mercado", "farmácia", "farmacia", "conta de", "almoço custou"
        ).any { lower.contains(it) }

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

        // 2. Extract Expense Details
        val amountBrl = extractedAmount ?: 0.0
        val expenseCategory = inferExpenseCategory(lower)
        val expenseTitle = cleanExpenseDescription(clean, expenseCategory)

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
        // Matches e.g. "45", "45,90", "45.90", "R$ 120,50", "45 reais e 50 centavos"
        val regex = Regex("""(?:r\$\s*)?(\d{1,6})(?:[.,](\d{1,2}))?\s*(?:reais|real)?""")
        val matches = regex.findAll(lower).toList()
        for (match in matches) {
            // Avoid matching hours like "10h" or "14 horas" as currency
            val endIndex = match.range.last + 1
            val trailing = lower.substring(endIndex).trimStart()
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
            .replace(Regex("""(?i)\b\d{1,6}([.,]\d{1,2})?\s*(reais|real)\b"""), "")
            .replace(Regex("""(?i)^(na|no|em|com|de|por)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

        return if (cleaned.length >= 2) cleaned else category
    }

    private fun extractTimeFromSpeech(lower: String): Pair<Int, Int> {
        // E.g. "às 10h30", "as 10:30", "às 14 horas", "às 9h"
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
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
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
