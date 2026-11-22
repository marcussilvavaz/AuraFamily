package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * 1. Inegociáveis Personalizáveis (Trackers Diários de Hábitos e Saúde)
 */
data class DailyTrackerHabit(
    val id: String,
    val title: String,
    val emoji: String,
    val unit: String,          // ex: "copos", "doses", "min", "pausas"
    val targetValue: Int,      // ex: 8
    val stepValue: Int = 1,    // ex: 1
    val colorHex: Long = 0xFF0B6E4F,
    val privacyScope: String = PrivacyScope.PERSONAL // PERSONAL ou FAMILY
)

/**
 * 2. Registro de Glicemia (mg/dL)
 */
data class GlucoseRecord(
    val id: String,
    val dateIso: String,       // YYYY-MM-DD
    val dateEpochDay: Long,
    val timeHHmm: String,      // HH:mm
    val slot: String,          // Jejum, 1h pós-café, Pré-almoço, 1h pós-almoço, etc.
    val valueMgDl: Int,
    val notes: String,
    val privacyScope: String = PrivacyScope.PERSONAL,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun statusLabel(): String = when {
        valueMgDl < 70 -> "Hipoglicemia (<70)"
        slot.contains("Jejum", ignoreCase = true) && valueMgDl <= 99 -> "Normal em Jejum"
        slot.contains("Jejum", ignoreCase = true) && valueMgDl in 100..125 -> "Atenção (Jejum)"
        !slot.contains("Jejum", ignoreCase = true) && valueMgDl <= 140 -> "Normal Pós-Refeição"
        !slot.contains("Jejum", ignoreCase = true) && valueMgDl in 141..180 -> "Atenção Pós-Refeição"
        else -> "Alerta Elevado"
    }

    fun isAlert(): Boolean = valueMgDl < 70 ||
        (slot.contains("Jejum", ignoreCase = true) && valueMgDl > 125) ||
        (!slot.contains("Jejum", ignoreCase = true) && valueMgDl > 180)
}

/**
 * 3. Registro de Pressão Arterial (mmHg) e Pulso (bpm)
 */
data class BloodPressureRecord(
    val id: String,
    val dateIso: String,       // YYYY-MM-DD
    val dateEpochDay: Long,
    val timeHHmm: String,      // HH:mm
    val systolic: Int,         // ex: 120
    val diastolic: Int,        // ex: 80
    val pulseBpm: Int = 72,    // ex: 72
    val notes: String,
    val privacyScope: String = PrivacyScope.PERSONAL,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun formattedValue(): String = "${systolic}/${diastolic} mmHg"

    fun statusLabel(): String = when {
        systolic < 90 || diastolic < 60 -> "Hipotensão (<90/60)"
        systolic < 120 && diastolic < 80 -> "Ótima / Normal"
        systolic in 120..129 && diastolic < 85 -> "Normal Limítrofe"
        systolic in 130..139 || diastolic in 85..89 -> "Pré-Hipertensão"
        else -> "Alerta Pressão Elevada"
    }

    fun isAlert(): Boolean = systolic >= 140 || diastolic >= 90 || systolic < 90 || diastolic < 60
}

/**
 * 4. Item de Acompanhamento Gestacional (Escopo Familiar)
 */
data class PregnancyChecklistItem(
    val id: String,
    val title: String,
    val trimester: Int,        // 1, 2 ou 3
    val recommendedWeek: Int,
    val completed: Boolean = false,
    val notes: String = "",
    val suggestedFinanceCategory: String = "Exames",
    val estimatedCostBrl: Double = 0.0
)

/**
 * 5. Sugestão Financeira Inteligente ligada à Saúde (integrada ao módulo Financeiro)
 */
data class HealthFinanceSuggestion(
    val id: String,
    val title: String,
    val category: String,      // "Farmácia", "Exames", "Consultas", "Pré-Natal", "Suplementos"
    val emoji: String,
    val defaultAmountBrl: Double,
    val description: String,
    val privacyScope: String = PrivacyScope.FAMILY
)

/**
 * 6. Estado do Cronômetro / Timer de 1 Hora Pós-Refeição
 */
data class PostMealTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mealLabel: String = "Almoço",
    val totalDurationSeconds: Int = 3600, // 1 hora = 3600s
    val targetEndEpochMillis: Long = 0L,
    val pausedRemainingSeconds: Int = 3600,
    val startedAtHHmm: String = "",
    val completedAlertPending: Boolean = false
)

/**
 * Estado consolidado do Módulo de Saúde (Passo 3)
 */
data class HealthModuleState(
    val trackers: List<DailyTrackerHabit> = emptyList(),
    // Mapa: dateIso (YYYY-MM-DD) -> (trackerId -> valor atual no dia)
    val trackerProgressByDate: Map<String, Map<String, Int>> = emptyMap(),
    val glucoseRecords: List<GlucoseRecord> = emptyList(),
    val bloodPressureRecords: List<BloodPressureRecord> = emptyList(),
    val pregnancyChecklist: List<PregnancyChecklistItem> = emptyList(),
    val pregnancyNotesLog: List<String> = emptyList(),
    val postMealTimer: PostMealTimerState = PostMealTimerState(),
    val lastGeneratedPdfPath: String = "",
    val lastGeneratedPdfSummary: String = ""
) {
    fun progressForTracker(dateIso: String, trackerId: String): Int {
        return trackerProgressByDate[dateIso]?.get(trackerId) ?: 0
    }

    fun completedTrackersCount(dateIso: String, scopeFilter: String = "ALL"): Int {
        val relevant = trackers.filter {
            scopeFilter == "ALL" || it.privacyScope == scopeFilter
        }
        return relevant.count { habit ->
            progressForTracker(dateIso, habit.id) >= habit.targetValue
        }
    }
}

object HealthCatalog {
    val financeSuggestions: List<HealthFinanceSuggestion> = listOf(
        HealthFinanceSuggestion(
            id = "hf_farmacia",
            title = "Medicamentos & Uso Contínuo",
            category = "Farmácia",
            emoji = "💊",
            defaultAmountBrl = 145.0,
            description = "Fitas de glicemia, medicamentos contínuos ou receita médica",
            privacyScope = PrivacyScope.FAMILY
        ),
        HealthFinanceSuggestion(
            id = "hf_exames",
            title = "Exames Laboratoriais / Check-up",
            category = "Exames",
            emoji = "🧪",
            defaultAmountBrl = 220.0,
            description = "Hemograma, hemoglobina glicada, curva glicêmica ou imagem",
            privacyScope = PrivacyScope.FAMILY
        ),
        HealthFinanceSuggestion(
            id = "hf_consulta",
            title = "Consulta Especialista / Retorno",
            category = "Consultas",
            emoji = "🩺",
            defaultAmountBrl = 350.0,
            description = "Endocrinologista, cardiologista, obstetra ou clínica geral",
            privacyScope = PrivacyScope.FAMILY
        ),
        HealthFinanceSuggestion(
            id = "hf_prenatal",
            title = "Ultrassom & Enxoval Gestacional",
            category = "Pré-Natal",
            emoji = "🤰",
            defaultAmountBrl = 280.0,
            description = "Ultrassom morfológico, vitaminas gestacionais e itens do bebê",
            privacyScope = PrivacyScope.FAMILY
        ),
        HealthFinanceSuggestion(
            id = "hf_vitaminas",
            title = "Vitaminas & Suplementação",
            category = "Farmácia",
            emoji = "🌿",
            defaultAmountBrl = 95.0,
            description = "Vitamina D, Ômega 3, Ácido Fólico ou suplementos dos Inegociáveis",
            privacyScope = PrivacyScope.PERSONAL
        )
    )

    fun financeSuggestionsFor(config: AppCustomizationConfig): List<HealthFinanceSuggestion> {
        return financeSuggestions.filter { suggestion ->
            if (suggestion.id == "hf_prenatal") config.pregnancyEnabled else true
        }
    }

    val mealOptions: List<String> = listOf(
        "Café da Manhã",
        "Almoço",
        "Lanche da Tarde",
        "Jantar"
    )
}

class HealthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _stateFlow = MutableStateFlow(loadState())
    val stateFlow: StateFlow<HealthModuleState> = _stateFlow.asStateFlow()

    private fun loadState(): HealthModuleState {
        val raw = prefs.getString(KEY_HEALTH_JSON, null)
        if (raw.isNullOrBlank()) {
            return createDefaultSeedState()
        }
        return try {
            val root = JSONObject(raw)

            // 1. Trackers
            val trackersArr = root.optJSONArray("trackers") ?: JSONArray()
            val trackers = mutableListOf<DailyTrackerHabit>()
            for (i in 0 until trackersArr.length()) {
                val obj = trackersArr.getJSONObject(i)
                trackers.add(
                    DailyTrackerHabit(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        emoji = obj.optString("emoji", "💧"),
                        unit = obj.optString("unit", "un"),
                        targetValue = obj.optInt("targetValue", 1).coerceAtLeast(1),
                        stepValue = obj.optInt("stepValue", 1).coerceAtLeast(1),
                        colorHex = obj.optLong("colorHex", 0xFF0B6E4F),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.PERSONAL)
                    )
                )
            }

            // 2. Progresso por data
            val progressObj = root.optJSONObject("trackerProgressByDate") ?: JSONObject()
            val progressMap = mutableMapOf<String, Map<String, Int>>()
            val dateKeys = progressObj.keys()
            while (dateKeys.hasNext()) {
                val dateIso = dateKeys.next()
                val dayObj = progressObj.optJSONObject(dateIso) ?: continue
                val inner = mutableMapOf<String, Int>()
                val habitKeys = dayObj.keys()
                while (habitKeys.hasNext()) {
                    val habitId = habitKeys.next()
                    inner[habitId] = dayObj.optInt(habitId, 0)
                }
                progressMap[dateIso] = inner
            }

            // 3. Glicemia
            val glucoseArr = root.optJSONArray("glucoseRecords") ?: JSONArray()
            val glucoseList = mutableListOf<GlucoseRecord>()
            for (i in 0 until glucoseArr.length()) {
                val obj = glucoseArr.getJSONObject(i)
                glucoseList.add(
                    GlucoseRecord(
                        id = obj.optString("id"),
                        dateIso = obj.optString("dateIso"),
                        dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                        timeHHmm = obj.optString("timeHHmm", "08:00"),
                        slot = obj.optString("slot", DiabetesSlotCatalog.JEJUM),
                        valueMgDl = obj.optInt("valueMgDl", 95),
                        notes = obj.optString("notes", ""),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.PERSONAL),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            // 4. Pressão Arterial
            val bpArr = root.optJSONArray("bloodPressureRecords") ?: JSONArray()
            val bpList = mutableListOf<BloodPressureRecord>()
            for (i in 0 until bpArr.length()) {
                val obj = bpArr.getJSONObject(i)
                bpList.add(
                    BloodPressureRecord(
                        id = obj.optString("id"),
                        dateIso = obj.optString("dateIso"),
                        dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                        timeHHmm = obj.optString("timeHHmm", "21:00"),
                        systolic = obj.optInt("systolic", 120),
                        diastolic = obj.optInt("diastolic", 80),
                        pulseBpm = obj.optInt("pulseBpm", 72),
                        notes = obj.optString("notes", ""),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.PERSONAL),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            // 5. Checklist Gestacional
            val pregArr = root.optJSONArray("pregnancyChecklist") ?: JSONArray()
            val pregList = mutableListOf<PregnancyChecklistItem>()
            for (i in 0 until pregArr.length()) {
                val obj = pregArr.getJSONObject(i)
                pregList.add(
                    PregnancyChecklistItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        trimester = obj.optInt("trimester", 1),
                        recommendedWeek = obj.optInt("recommendedWeek", 12),
                        completed = obj.optBoolean("completed", false),
                        notes = obj.optString("notes", ""),
                        suggestedFinanceCategory = obj.optString("suggestedFinanceCategory", "Exames"),
                        estimatedCostBrl = obj.optDouble("estimatedCostBrl", 0.0)
                    )
                )
            }

            // 6. Notas gestacionais
            val notesArr = root.optJSONArray("pregnancyNotesLog") ?: JSONArray()
            val pregNotes = mutableListOf<String>()
            for (i in 0 until notesArr.length()) {
                pregNotes.add(notesArr.getString(i))
            }

            // 7. Timer 1h pós-refeição
            val timerObj = root.optJSONObject("postMealTimer")
            val timerState = if (timerObj != null) {
                PostMealTimerState(
                    isRunning = timerObj.optBoolean("isRunning", false),
                    isPaused = timerObj.optBoolean("isPaused", false),
                    mealLabel = timerObj.optString("mealLabel", "Almoço"),
                    totalDurationSeconds = timerObj.optInt("totalDurationSeconds", 3600),
                    targetEndEpochMillis = timerObj.optLong("targetEndEpochMillis", 0L),
                    pausedRemainingSeconds = timerObj.optInt("pausedRemainingSeconds", 3600),
                    startedAtHHmm = timerObj.optString("startedAtHHmm", ""),
                    completedAlertPending = timerObj.optBoolean("completedAlertPending", false)
                )
            } else {
                PostMealTimerState()
            }

            HealthModuleState(
                trackers = trackers.ifEmpty { defaultTrackers() },
                trackerProgressByDate = progressMap,
                glucoseRecords = glucoseList.sortedWith(
                    compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                ),
                bloodPressureRecords = bpList.sortedWith(
                    compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                ),
                pregnancyChecklist = pregList.ifEmpty { defaultPregnancyChecklist() },
                pregnancyNotesLog = pregNotes,
                postMealTimer = timerState,
                lastGeneratedPdfPath = root.optString("lastGeneratedPdfPath", ""),
                lastGeneratedPdfSummary = root.optString("lastGeneratedPdfSummary", "")
            )
        } catch (_: Exception) {
            createDefaultSeedState()
        }
    }

    private fun saveState(updated: HealthModuleState) {
        val root = JSONObject()

        val trackersArr = JSONArray()
        updated.trackers.forEach { t ->
            trackersArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("emoji", t.emoji)
                    put("unit", t.unit)
                    put("targetValue", t.targetValue)
                    put("stepValue", t.stepValue)
                    put("colorHex", t.colorHex)
                    put("privacyScope", t.privacyScope)
                }
            )
        }
        root.put("trackers", trackersArr)

        val progressObj = JSONObject()
        updated.trackerProgressByDate.forEach { (dateIso, map) ->
            val inner = JSONObject()
            map.forEach { (habitId, value) -> inner.put(habitId, value) }
            progressObj.put(dateIso, inner)
        }
        root.put("trackerProgressByDate", progressObj)

        val glucoseArr = JSONArray()
        updated.glucoseRecords.forEach { g ->
            glucoseArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("dateIso", g.dateIso)
                    put("dateEpochDay", g.dateEpochDay)
                    put("timeHHmm", g.timeHHmm)
                    put("slot", g.slot)
                    put("valueMgDl", g.valueMgDl)
                    put("notes", g.notes)
                    put("privacyScope", g.privacyScope)
                    put("createdAtMillis", g.createdAtMillis)
                }
            )
        }
        root.put("glucoseRecords", glucoseArr)

        val bpArr = JSONArray()
        updated.bloodPressureRecords.forEach { bp ->
            bpArr.put(
                JSONObject().apply {
                    put("id", bp.id)
                    put("dateIso", bp.dateIso)
                    put("dateEpochDay", bp.dateEpochDay)
                    put("timeHHmm", bp.timeHHmm)
                    put("systolic", bp.systolic)
                    put("diastolic", bp.diastolic)
                    put("pulseBpm", bp.pulseBpm)
                    put("notes", bp.notes)
                    put("privacyScope", bp.privacyScope)
                    put("createdAtMillis", bp.createdAtMillis)
                }
            )
        }
        root.put("bloodPressureRecords", bpArr)

        val pregArr = JSONArray()
        updated.pregnancyChecklist.forEach { item ->
            pregArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("trimester", item.trimester)
                    put("recommendedWeek", item.recommendedWeek)
                    put("completed", item.completed)
                    put("notes", item.notes)
                    put("suggestedFinanceCategory", item.suggestedFinanceCategory)
                    put("estimatedCostBrl", item.estimatedCostBrl)
                }
            )
        }
        root.put("pregnancyChecklist", pregArr)

        val notesArr = JSONArray()
        updated.pregnancyNotesLog.forEach { notesArr.put(it) }
        root.put("pregnancyNotesLog", notesArr)

        val t = updated.postMealTimer
        root.put(
            "postMealTimer",
            JSONObject().apply {
                put("isRunning", t.isRunning)
                put("isPaused", t.isPaused)
                put("mealLabel", t.mealLabel)
                put("totalDurationSeconds", t.totalDurationSeconds)
                put("targetEndEpochMillis", t.targetEndEpochMillis)
                put("pausedRemainingSeconds", t.pausedRemainingSeconds)
                put("startedAtHHmm", t.startedAtHHmm)
                put("completedAlertPending", t.completedAlertPending)
            }
        )

        root.put("lastGeneratedPdfPath", updated.lastGeneratedPdfPath)
        root.put("lastGeneratedPdfSummary", updated.lastGeneratedPdfSummary)

        prefs.edit().putString(KEY_HEALTH_JSON, root.toString()).apply()
        _stateFlow.value = updated
    }

    private fun defaultTrackers(): List<DailyTrackerHabit> = listOf(
        DailyTrackerHabit(
            id = "tr_water",
            title = "Hidratação (Água)",
            emoji = "💧",
            unit = "copos (250ml)",
            targetValue = 8,
            stepValue = 1,
            colorHex = 0xFF0077B6,
            privacyScope = PrivacyScope.PERSONAL
        ),
        DailyTrackerHabit(
            id = "tr_vitamins",
            title = "Vitaminas & Suplementos",
            emoji = "💊",
            unit = "doses",
            targetValue = 2,
            stepValue = 1,
            colorHex = 0xFF0B6E4F,
            privacyScope = PrivacyScope.PERSONAL
        ),
        DailyTrackerHabit(
            id = "tr_movement",
            title = "Caminhada / Movimento Leve",
            emoji = "🚶",
            unit = "min",
            targetValue = 30,
            stepValue = 10,
            colorHex = 0xFFD4AF37,
            privacyScope = PrivacyScope.FAMILY
        ),
        DailyTrackerHabit(
            id = "tr_pause",
            title = "Pausa Respiratória & Postura",
            emoji = "🧘",
            unit = "pausas",
            targetValue = 3,
            stepValue = 1,
            colorHex = 0xFF7C4DFF,
            privacyScope = PrivacyScope.PERSONAL
        )
    )

    private fun defaultPregnancyChecklist(): List<PregnancyChecklistItem> = listOf(
        PregnancyChecklistItem(
            id = "preg_1",
            title = "Ultrassom Obstétrico & Morfológico 1º Tri",
            trimester = 1,
            recommendedWeek = 12,
            completed = true,
            notes = "Transluscência nucal e batimentos cardíacos OK",
            suggestedFinanceCategory = "Exames",
            estimatedCostBrl = 290.0
        ),
        PregnancyChecklistItem(
            id = "preg_2",
            title = "Suplementação Pré-Natal (Ácido Fólico + Ferro)",
            trimester = 1,
            recommendedWeek = 8,
            completed = true,
            notes = "Uso diário contínuo",
            suggestedFinanceCategory = "Farmácia",
            estimatedCostBrl = 115.0
        ),
        PregnancyChecklistItem(
            id = "preg_3",
            title = "Curva Glicêmica (TOTG 75g) & Exames 2º Tri",
            trimester = 2,
            recommendedWeek = 24,
            completed = false,
            notes = "Rastreio gestacional entre 24 e 28 semanas",
            suggestedFinanceCategory = "Exames",
            estimatedCostBrl = 180.0
        ),
        PregnancyChecklistItem(
            id = "preg_4",
            title = "Consulta Obstétrica Mensal da Família",
            trimester = 2,
            recommendedWeek = 26,
            completed = false,
            notes = "Levar relatório PDF de Glicemia e Pressão Arterial",
            suggestedFinanceCategory = "Consultas",
            estimatedCostBrl = 350.0
        ),
        PregnancyChecklistItem(
            id = "preg_5",
            title = "Mala da Maternidade & Itens Essenciais do Bebê",
            trimester = 3,
            recommendedWeek = 34,
            completed = false,
            notes = "Fraldas, roupinhas RN e kit higiene",
            suggestedFinanceCategory = "Pré-Natal",
            estimatedCostBrl = 480.0
        )
    )

    private fun createDefaultSeedState(): HealthModuleState {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val todayIso = DateCalendarUtils.formatIsoDate(todayEpoch)
        val yesterdayEpoch = todayEpoch - 1L
        val yesterdayIso = DateCalendarUtils.formatIsoDate(yesterdayEpoch)

        return HealthModuleState(
            trackers = defaultTrackers(),
            trackerProgressByDate = mapOf(
                todayIso to mapOf(
                    "tr_water" to 5,
                    "tr_vitamins" to 2,
                    "tr_movement" to 20,
                    "tr_pause" to 2
                )
            ),
            glucoseRecords = listOf(
                GlucoseRecord(
                    id = "gl_seed_1",
                    dateIso = todayIso,
                    dateEpochDay = todayEpoch,
                    timeHHmm = "07:30",
                    slot = DiabetesSlotCatalog.JEJUM,
                    valueMgDl = 91,
                    notes = "Medição ao acordar em jejum de 8h",
                    privacyScope = PrivacyScope.PERSONAL
                ),
                GlucoseRecord(
                    id = "gl_seed_2",
                    dateIso = todayIso,
                    dateEpochDay = todayEpoch,
                    timeHHmm = "13:30",
                    slot = DiabetesSlotCatalog.POS_ALMOCO_1H,
                    valueMgDl = 126,
                    notes = "1h após almoço balanceado (arroz integral, frango e salada)",
                    privacyScope = PrivacyScope.PERSONAL
                ),
                GlucoseRecord(
                    id = "gl_seed_3",
                    dateIso = yesterdayIso,
                    dateEpochDay = yesterdayEpoch,
                    timeHHmm = "20:45",
                    slot = DiabetesSlotCatalog.POS_JANTAR_1H,
                    valueMgDl = 118,
                    notes = "1h pós-jantar leve",
                    privacyScope = PrivacyScope.PERSONAL
                )
            ),
            bloodPressureRecords = listOf(
                BloodPressureRecord(
                    id = "bp_seed_1",
                    dateIso = todayIso,
                    dateEpochDay = todayEpoch,
                    timeHHmm = "08:00",
                    systolic = 118,
                    diastolic = 76,
                    pulseBpm = 70,
                    notes = "Repouso de 5 minutos antes de medir",
                    privacyScope = PrivacyScope.PERSONAL
                ),
                BloodPressureRecord(
                    id = "bp_seed_2",
                    dateIso = yesterdayIso,
                    dateEpochDay = yesterdayEpoch,
                    timeHHmm = "21:30",
                    systolic = 122,
                    diastolic = 80,
                    pulseBpm = 74,
                    notes = "Medição noturna antes de dormir",
                    privacyScope = PrivacyScope.PERSONAL
                )
            ),
            pregnancyChecklist = defaultPregnancyChecklist(),
            pregnancyNotesLog = listOf(
                "$todayIso • Bebê ativo após o almoço; pressão e glicemia estáveis."
            )
        )
    }

    // =========================================================================
    // 1. OPERAÇÕES DOS INEGOCIÁVEIS (TRACKERS DIÁRIOS)
    // =========================================================================

    fun incrementTrackerProgress(dateIso: String, trackerId: String, delta: Int) {
        val current = _stateFlow.value
        val habit = current.trackers.firstOrNull { it.id == trackerId } ?: return
        val dayMap = current.trackerProgressByDate[dateIso]?.toMutableMap() ?: mutableMapOf()
        val currentVal = dayMap[trackerId] ?: 0
        val newVal = (currentVal + delta).coerceIn(0, habit.targetValue * 3)
        dayMap[trackerId] = newVal

        val updatedProgress = current.trackerProgressByDate.toMutableMap().apply {
            put(dateIso, dayMap)
        }
        saveState(current.copy(trackerProgressByDate = updatedProgress))
    }

    fun completeTrackerForDay(dateIso: String, trackerId: String) {
        val current = _stateFlow.value
        val habit = current.trackers.firstOrNull { it.id == trackerId } ?: return
        val dayMap = current.trackerProgressByDate[dateIso]?.toMutableMap() ?: mutableMapOf()
        dayMap[trackerId] = habit.targetValue

        val updatedProgress = current.trackerProgressByDate.toMutableMap().apply {
            put(dateIso, dayMap)
        }
        saveState(current.copy(trackerProgressByDate = updatedProgress))
    }

    fun addCustomTracker(
        title: String,
        emoji: String,
        unit: String,
        targetValue: Int,
        stepValue: Int,
        colorHex: Long,
        privacyScope: String
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val current = _stateFlow.value
        val newHabit = DailyTrackerHabit(
            id = "tr_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            emoji = emoji.ifBlank { "✨" },
            unit = unit.trim().ifBlank { "un" },
            targetValue = targetValue.coerceAtLeast(1),
            stepValue = stepValue.coerceAtLeast(1),
            colorHex = colorHex,
            privacyScope = privacyScope
        )
        saveState(current.copy(trackers = current.trackers + newHabit))
    }

    fun deleteTracker(trackerId: String) {
        val current = _stateFlow.value
        saveState(current.copy(trackers = current.trackers.filterNot { it.id == trackerId }))
    }

    // =========================================================================
    // 2. OPERAÇÕES DE GLICEMIA E PRESSÃO ARTERIAL
    // =========================================================================

    fun addGlucoseRecord(
        dateEpochDay: Long,
        timeHHmm: String,
        slot: String,
        valueMgDl: Int,
        notes: String,
        privacyScope: String
    ) {
        val current = _stateFlow.value
        val newRecord = GlucoseRecord(
            id = "gl_" + UUID.randomUUID().toString().take(8),
            dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
            dateEpochDay = dateEpochDay,
            timeHHmm = timeHHmm.ifBlank { DateCalendarUtils.currentTimeHHmm() },
            slot = slot,
            valueMgDl = valueMgDl.coerceIn(20, 600),
            notes = notes.trim(),
            privacyScope = privacyScope
        )
        val sorted = (listOf(newRecord) + current.glucoseRecords).sortedWith(
            compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(glucoseRecords = sorted))
    }

    fun deleteGlucoseRecord(recordId: String) {
        val current = _stateFlow.value
        saveState(current.copy(glucoseRecords = current.glucoseRecords.filterNot { it.id == recordId }))
    }

    fun addBloodPressureRecord(
        dateEpochDay: Long,
        timeHHmm: String,
        systolic: Int,
        diastolic: Int,
        pulseBpm: Int,
        notes: String,
        privacyScope: String
    ) {
        val current = _stateFlow.value
        val newRecord = BloodPressureRecord(
            id = "bp_" + UUID.randomUUID().toString().take(8),
            dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
            dateEpochDay = dateEpochDay,
            timeHHmm = timeHHmm.ifBlank { DateCalendarUtils.currentTimeHHmm() },
            systolic = systolic.coerceIn(50, 260),
            diastolic = diastolic.coerceIn(30, 180),
            pulseBpm = pulseBpm.coerceIn(35, 220),
            notes = notes.trim(),
            privacyScope = privacyScope
        )
        val sorted = (listOf(newRecord) + current.bloodPressureRecords).sortedWith(
            compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(bloodPressureRecords = sorted))
    }

    fun deleteBloodPressureRecord(recordId: String) {
        val current = _stateFlow.value
        saveState(current.copy(bloodPressureRecords = current.bloodPressureRecords.filterNot { it.id == recordId }))
    }

    // =========================================================================
    // 3. CRONÔMETRO / TIMER DE 1 HORA PÓS-REFEIÇÃO
    // =========================================================================

    fun startPostMealTimer(mealLabel: String, durationSeconds: Int = 3600) {
        val current = _stateFlow.value
        val now = System.currentTimeMillis()
        val updatedTimer = PostMealTimerState(
            isRunning = true,
            isPaused = false,
            mealLabel = mealLabel,
            totalDurationSeconds = durationSeconds,
            targetEndEpochMillis = now + durationSeconds * 1000L,
            pausedRemainingSeconds = durationSeconds,
            startedAtHHmm = DateCalendarUtils.currentTimeHHmm(),
            completedAlertPending = false
        )
        saveState(current.copy(postMealTimer = updatedTimer))
    }

    fun pauseOrResumePostMealTimer() {
        val current = _stateFlow.value
        val timer = current.postMealTimer
        if (!timer.isRunning) return
        val now = System.currentTimeMillis()
        val updatedTimer = if (!timer.isPaused) {
            val remainingSec = ((timer.targetEndEpochMillis - now) / 1000L).toInt().coerceAtLeast(0)
            timer.copy(
                isPaused = true,
                pausedRemainingSeconds = remainingSec
            )
        } else {
            timer.copy(
                isPaused = false,
                targetEndEpochMillis = now + timer.pausedRemainingSeconds * 1000L
            )
        }
        saveState(current.copy(postMealTimer = updatedTimer))
    }

    fun resetPostMealTimer(mealLabel: String = _stateFlow.value.postMealTimer.mealLabel) {
        val current = _stateFlow.value
        saveState(
            current.copy(
                postMealTimer = PostMealTimerState(
                    isRunning = false,
                    isPaused = false,
                    mealLabel = mealLabel,
                    totalDurationSeconds = 3600,
                    targetEndEpochMillis = 0L,
                    pausedRemainingSeconds = 3600,
                    startedAtHHmm = "",
                    completedAlertPending = false
                )
            )
        )
    }

    fun markPostMealTimerFinished() {
        val current = _stateFlow.value
        val timer = current.postMealTimer
        saveState(
            current.copy(
                postMealTimer = timer.copy(
                    isRunning = false,
                    isPaused = false,
                    pausedRemainingSeconds = 0,
                    completedAlertPending = true
                )
            )
        )
    }

    fun dismissPostMealTimerAlert() {
        val current = _stateFlow.value
        saveState(
            current.copy(
                postMealTimer = current.postMealTimer.copy(completedAlertPending = false)
            )
        )
    }

    // =========================================================================
    // 4. ACOMPANHAMENTO GESTACIONAL (ESCOPO FAMILIAR)
    // =========================================================================

    fun togglePregnancyChecklistItem(itemId: String) {
        val current = _stateFlow.value
        val updated = current.pregnancyChecklist.map {
            if (it.id == itemId) it.copy(completed = !it.completed) else it
        }
        saveState(current.copy(pregnancyChecklist = updated))
    }

    fun addPregnancyNote(noteText: String) {
        val clean = noteText.trim()
        if (clean.isEmpty()) return
        val current = _stateFlow.value
        val todayIso = DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())
        val entry = "$todayIso • $clean"
        saveState(current.copy(pregnancyNotesLog = listOf(entry) + current.pregnancyNotesLog))
    }

    fun recordGeneratedPdfInfo(path: String, summary: String) {
        val current = _stateFlow.value
        saveState(
            current.copy(
                lastGeneratedPdfPath = path,
                lastGeneratedPdfSummary = summary
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "aura_health_module_prefs"
        private const val KEY_HEALTH_JSON = "health_module_state_json"
    }
}

/**
 * Utilitário para calcular semanas gestacionais a partir da Data Prevista do Parto (DPP - DD/MM/AAAA)
 */
object PregnancyCalculator {
    data class PregnancyProgress(
        val dppFormatted: String,
        val currentWeek: Int,
        val currentDaysRemainder: Int,
        val daysRemainingToDpp: Int,
        val trimester: Int,
        val babySizeComparison: String
    )

    fun calculateFromDpp(dppRaw: String): PregnancyProgress {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val parsedDppEpoch = parseDdMmYyyyToEpochDay(dppRaw) ?: (todayEpoch + 112L) // default ~24 semanas se vazio
        val daysRemaining = (parsedDppEpoch - todayEpoch).toInt().coerceIn(0, 280)
        val elapsedDays = (280 - daysRemaining).coerceIn(1, 280)
        val currentWeek = (elapsedDays / 7).coerceIn(1, 40)
        val currentDaysRem = elapsedDays % 7
        val trimester = when {
            currentWeek <= 13 -> 1
            currentWeek <= 27 -> 2
            else -> 3
        }
        val sizeComparison = when {
            currentWeek < 8 -> "Mirtilo (~1,5 cm)"
            currentWeek < 12 -> "Limão Tahiti (~5 cm)"
            currentWeek < 16 -> "Maçã (~11 cm)"
            currentWeek < 20 -> "Manga (~16 cm)"
            currentWeek < 24 -> "Espiga de Milho (~28 cm)"
            currentWeek < 28 -> "Couve-flor (~36 cm)"
            currentWeek < 32 -> "Coco Verde (~41 cm)"
            currentWeek < 36 -> "Abacaxi (~46 cm)"
            else -> "Melancia Baby (~50 cm)"
        }
        return PregnancyProgress(
            dppFormatted = if (dppRaw.isNotBlank()) dppRaw else DateCalendarUtils.formatEpochDayPt(parsedDppEpoch),
            currentWeek = currentWeek,
            currentDaysRemainder = currentDaysRem,
            daysRemainingToDpp = daysRemaining,
            trimester = trimester,
            babySizeComparison = sizeComparison
        )
    }

    private fun parseDdMmYyyyToEpochDay(raw: String): Long? {
        val parts = raw.trim().split("/", "-", ".")
        if (parts.size < 3) return null
        val d = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val y = parts[2].toIntOrNull() ?: return null
        if (y < 2024 || m !in 1..12 || d !in 1..31) return null
        return DateCalendarUtils.toEpochDay(y, m, d)
    }
}

/**
 * 4. GERADOR DE RELATÓRIO MÉDICO EM PDF (NATIVO ANDROID - android.graphics.pdf.PdfDocument)
 * Consolida histórico de Glicemia, Pressão Arterial, Inegociáveis Diários e Acompanhamento Gestacional por período.
 */
object MedicalReportPdfGenerator {

    data class PdfExportResult(
        val success: Boolean,
        val filePath: String,
        val summaryText: String
    )

    fun generateAndWriteToStream(
        outputStream: OutputStream,
        profileName: String,
        periodDays: Int, // 7, 30 ou 365 (Todo o histórico)
        customizationConfig: AppCustomizationConfig,
        healthState: HealthModuleState
    ): String {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val todayIso = DateCalendarUtils.formatIsoDate(todayEpoch)
        val minEpochDay = todayEpoch - periodDays.toLong()

        val filteredGlucose = healthState.glucoseRecords.filter { it.dateEpochDay >= minEpochDay }
        val filteredBp = healthState.bloodPressureRecords.filter { it.dateEpochDay >= minEpochDay }

        val periodLabel = when (periodDays) {
            7 -> "Últimos 7 dias"
            30 -> "Últimos 30 dias"
            else -> "Histórico Completo"
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(11, 110, 79)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(80, 95, 88)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(16, 34, 26)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(25, 35, 30)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(45, 55, 50)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(242, 247, 244)
            style = Paint.Style.FILL
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(210, 222, 215)
            strokeWidth = 1f
        }

        var y = 42f
        val leftMargin = 36f
        val rightMargin = pageWidth - 36f

        // Cabeçalho Clínico
        canvas.drawRoundRect(
            RectF(leftMargin, y - 18f, rightMargin, y + 48f),
            12f,
            12f,
            boxFillPaint
        )
        canvas.drawText("AURAMANAGER • RELATÓRIO MÉDICO E DE SAÚDE", leftMargin + 14f, y + 4f, titlePaint)
        val emittedNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        canvas.drawText(
            "Paciente / Perfil: $profileName   •   Período: $periodLabel   •   Emissão: $emittedNow",
            leftMargin + 14f,
            y + 22f,
            subtitlePaint
        )
        val activeModulesText = buildList {
            if (customizationConfig.diabetesEnabled) add("Glicemia")
            if (customizationConfig.bloodPressureEnabled) add("Pressão Arterial")
            if (customizationConfig.pregnancyEnabled) add("Gestação (DPP: ${customizationConfig.pregnancyDppDate.ifBlank { "Ativa" }})")
            add("Inegociáveis Diários")
        }.joinToString(" | ")
        canvas.drawText("Módulos Monitorados: $activeModulesText", leftMargin + 14f, y + 38f, subtitlePaint)

        y += 72f

        // 1. Resumo Estatístico Clínico
        canvas.drawText("1. RESUMO ESTATÍSTICO DO PERÍODO ($periodLabel)", leftMargin, y, sectionHeaderPaint)
        y += 16f

        val avgGlucose = if (filteredGlucose.isNotEmpty()) {
            filteredGlucose.map { it.valueMgDl }.average().roundToInt()
        } else 0
        val minGlucose = filteredGlucose.minOfOrNull { it.valueMgDl } ?: 0
        val maxGlucose = filteredGlucose.maxOfOrNull { it.valueMgDl } ?: 0

        val avgSys = if (filteredBp.isNotEmpty()) {
            filteredBp.map { it.systolic }.average().roundToInt()
        } else 0
        val avgDia = if (filteredBp.isNotEmpty()) {
            filteredBp.map { it.diastolic }.average().roundToInt()
        } else 0
        val avgPulse = if (filteredBp.isNotEmpty()) {
            filteredBp.map { it.pulseBpm }.average().roundToInt()
        } else 0

        if (customizationConfig.diabetesEnabled) {
            canvas.drawText(
                "• Glicemia (${filteredGlucose.size} medições): Média = ${if (avgGlucose > 0) "$avgGlucose mg/dL (Mín: $minGlucose / Máx: $maxGlucose mg/dL)" else "Sem registros no período"}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 15f
        }
        if (customizationConfig.bloodPressureEnabled) {
            canvas.drawText(
                "• Pressão Arterial (${filteredBp.size} aferições): Média = ${if (avgSys > 0) "$avgSys/$avgDia mmHg (Pulso médio: $avgPulse bpm)" else "Sem registros no período"}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 15f
        }
        val completedTrackersToday = healthState.completedTrackersCount(todayIso)
        canvas.drawText(
            "• Hábitos Inegociáveis Hoje ($todayIso): $completedTrackersToday de ${healthState.trackers.size} metas concluídas",
            leftMargin + 8f,
            y,
            bodyBoldPaint
        )
        y += 14f
        canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
        y += 20f

        // 2. Histórico de Glicemia (somente se Diabetes / Glicemia estiver marcado no questionário)
        if (customizationConfig.diabetesEnabled) {
            canvas.drawText("2. HISTÓRICO DE GLICEMIA CAPILAR (mg/dL)", leftMargin, y, sectionHeaderPaint)
            y += 16f
            canvas.drawText("DATA        HORA   MOMENTO / SLOT         VALOR      CLASSIFICAÇÃO & OBSERVAÇÕES", leftMargin, y, bodyBoldPaint)
            y += 12f

            if (filteredGlucose.isEmpty()) {
                canvas.drawText("Nenhuma medição de glicemia registrada no período selecionado.", leftMargin, y, subtitlePaint)
                y += 18f
            } else {
                filteredGlucose.take(12).forEach { rec ->
                    val slotFixed = rec.slot.take(20).padEnd(20, ' ')
                    val valFixed = "${rec.valueMgDl} mg/dL".padEnd(10, ' ')
                    val noteClean = if (rec.notes.isNotBlank()) " - ${rec.notes.take(30)}" else ""
                    val line = "${rec.dateIso}  ${rec.timeHHmm}  $slotFixed $valFixed ${rec.statusLabel()}$noteClean"
                    canvas.drawText(line, leftMargin, y, bodyPaint)
                    y += 13f
                }
            }

            y += 10f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 20f
        }

        // 3. Histórico de Pressão Arterial (somente se Pressão Arterial estiver marcado no questionário)
        if (customizationConfig.bloodPressureEnabled) {
            canvas.drawText("3. HISTÓRICO DE PRESSÃO ARTERIAL (mmHg) & PULSO (bpm)", leftMargin, y, sectionHeaderPaint)
            y += 16f
            canvas.drawText("DATA        HORA   PAS/PAD (mmHg)   PULSO     CLASSIFICAÇÃO & OBSERVAÇÕES", leftMargin, y, bodyBoldPaint)
            y += 12f

            if (filteredBp.isEmpty()) {
                canvas.drawText("Nenhuma aferição de pressão arterial registrada no período selecionado.", leftMargin, y, subtitlePaint)
                y += 18f
            } else {
                filteredBp.take(12).forEach { bp ->
                    val bpFixed = "${bp.systolic}/${bp.diastolic} mmHg".padEnd(15, ' ')
                    val pulseFixed = "${bp.pulseBpm} bpm".padEnd(9, ' ')
                    val noteClean = if (bp.notes.isNotBlank()) " - ${bp.notes.take(30)}" else ""
                    val line = "${bp.dateIso}  ${bp.timeHHmm}  $bpFixed $pulseFixed ${bp.statusLabel()}$noteClean"
                    canvas.drawText(line, leftMargin, y, bodyPaint)
                    y += 13f
                }
            }

            y += 10f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 20f
        }

        // 4. Inegociáveis Diários & Acompanhamento Gestacional (somente se Gestação estiver marcado)
        val sectionTitle = if (customizationConfig.pregnancyEnabled) {
            "INEGOCIÁVEIS DIÁRIOS (TRACKERS) & GESTAÇÃO FAMILIAR"
        } else {
            "INEGOCIÁVEIS DIÁRIOS (TRACKERS)"
        }
        canvas.drawText(sectionTitle, leftMargin, y, sectionHeaderPaint)
        y += 16f

        healthState.trackers.take(6).forEach { habit ->
            val currentVal = healthState.progressForTracker(todayIso, habit.id)
            canvas.drawText(
                "• ${habit.title}: $currentVal / ${habit.targetValue} ${habit.unit} (${if (currentVal >= habit.targetValue) "Meta Atingida" else "Em andamento"})",
                leftMargin + 8f,
                y,
                bodyPaint
            )
            y += 13f
        }

        if (customizationConfig.pregnancyEnabled) {
            y += 8f
            val pregProgress = PregnancyCalculator.calculateFromDpp(customizationConfig.pregnancyDppDate)
            canvas.drawText(
                "Acompanhamento Gestacional: Semana ${pregProgress.currentWeek} (+${pregProgress.currentDaysRemainder}d) • ${pregProgress.trimester}º Trimestre • DPP: ${pregProgress.dppFormatted}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 14f
        }

        // Rodapé
        canvas.drawText(
            "Documento gerado nativamente em PDF pelo aplicativo AuraManager (com.auramanager.app) para acompanhamento médico.",
            leftMargin,
            pageHeight - 28f,
            subtitlePaint
        )

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        val summaryParts = buildList {
            add("$completedTrackersToday/${healthState.trackers.size} inegociáveis")
            if (customizationConfig.diabetesEnabled) {
                add("${filteredGlucose.size} glicemias (média ${avgGlucose}mg/dL)")
            }
            if (customizationConfig.bloodPressureEnabled) {
                add("${filteredBp.size} pressões (média ${avgSys}/${avgDia}mmHg)")
            }
            if (customizationConfig.pregnancyEnabled) {
                add("Gestação ativa")
            }
        }
        return "PDF gerado ($periodLabel): ${summaryParts.joinToString(", ")}."
    }

    fun exportToLocalAppFile(
        context: Context,
        profileName: String,
        periodDays: Int,
        customizationConfig: AppCustomizationConfig,
        healthState: HealthModuleState
    ): PdfExportResult {
        return try {
            val reportsDir = File(context.filesDir, "relatorios_medicos").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "Relatorio_Medico_AuraManager_${DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())}.pdf"
            val pdfFile = File(reportsDir, fileName)
            var summary = ""
            FileOutputStream(pdfFile).use { fos ->
                summary = generateAndWriteToStream(
                    outputStream = fos,
                    profileName = profileName,
                    periodDays = periodDays,
                    customizationConfig = customizationConfig,
                    healthState = healthState
                )
            }
            PdfExportResult(
                success = true,
                filePath = pdfFile.absolutePath,
                summaryText = summary
            )
        } catch (e: Exception) {
            PdfExportResult(
                success = false,
                filePath = "",
                summaryText = "Erro ao gerar PDF: ${e.localizedMessage ?: "Falha de escrita"}"
            )
        }
    }

    fun exportToUserUri(
        context: Context,
        targetUri: Uri,
        profileName: String,
        periodDays: Int,
        customizationConfig: AppCustomizationConfig,
        healthState: HealthModuleState
    ): PdfExportResult {
        return try {
            var summary = ""
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                summary = generateAndWriteToStream(
                    outputStream = out,
                    profileName = profileName,
                    periodDays = periodDays,
                    customizationConfig = customizationConfig,
                    healthState = healthState
                )
            }
            PdfExportResult(
                success = true,
                filePath = targetUri.toString(),
                summaryText = summary
            )
        } catch (e: Exception) {
            PdfExportResult(
                success = false,
                filePath = "",
                summaryText = "Erro ao salvar PDF: ${e.localizedMessage ?: "Falha"}"
            )
        }
    }
}
