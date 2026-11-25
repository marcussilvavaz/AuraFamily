package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.alarm.AlarmScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
 * 2. Registro de Monitoramento de Glicemia (mg/dL) nos 6 Períodos Oficiais com separação por membro
 */
data class GlucoseRecord(
    val id: String,
    val dateIso: String,       // YYYY-MM-DD
    val dateEpochDay: Long,
    val timeHHmm: String,      // HH:mm
    val slot: String,          // Jejum, 1h Pós-Café, Almoço, 1h Pós-Almoço, Janta, 1h Pós-Janta
    val valueMgDl: Int,
    val notes: String,
    val privacyScope: String = PrivacyScope.PERSONAL,
    val memberId: String = "",
    val memberName: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    private fun isPreMealOrFasting(): Boolean {
        val norm = DiabetesSlotCatalog.normalizeSlot(slot)
        return norm == DiabetesSlotCatalog.JEJUM ||
            norm == DiabetesSlotCatalog.ALMOCO ||
            norm == DiabetesSlotCatalog.JANTA
    }

    fun statusLabel(): String = when {
        valueMgDl < 70 -> "Hipoglicemia (<70)"
        isPreMealOrFasting() && valueMgDl <= 99 -> "Normal (${DiabetesSlotCatalog.normalizeSlot(slot)})"
        isPreMealOrFasting() && valueMgDl in 100..125 -> "Atenção (${DiabetesSlotCatalog.normalizeSlot(slot)})"
        !isPreMealOrFasting() && valueMgDl <= 140 -> "Normal Pós-Refeição"
        !isPreMealOrFasting() && valueMgDl in 141..180 -> "Atenção Pós-Refeição"
        else -> "Alerta Elevado"
    }

    val timeFormatted: String
        get() = timeHHmm

    val timestampEpochMillis: Long
        get() = createdAtMillis

    constructor(
        id: String,
        dateIso: String,
        timeFormatted: String,
        slot: String,
        valueMgDl: Int,
        notes: String = "",
        privacyScope: String = PrivacyScope.PERSONAL,
        memberId: String = "",
        memberName: String = "",
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        dateIso = dateIso,
        dateEpochDay = runCatching { java.time.LocalDate.parse(dateIso).toEpochDay() }.getOrDefault(DateCalendarUtils.todayEpochDay()),
        timeHHmm = timeFormatted,
        slot = DiabetesSlotCatalog.normalizeSlot(slot),
        valueMgDl = valueMgDl,
        notes = notes,
        privacyScope = privacyScope,
        memberId = memberId,
        memberName = memberName,
        createdAtMillis = timestampEpochMillis
    )

    fun isAlert(): Boolean = valueMgDl < 70 ||
        (isPreMealOrFasting() && valueMgDl > 125) ||
        (!isPreMealOrFasting() && valueMgDl > 180)
}

/**
 * 3. Registro de Pressão Arterial (mmHg) e Pulso (bpm) com separação individual por membro
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
    val memberId: String = "",
    val memberName: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val timeFormatted: String
        get() = timeHHmm

    val timestampEpochMillis: Long
        get() = createdAtMillis

    constructor(
        id: String,
        dateIso: String,
        timeFormatted: String,
        systolic: Int,
        diastolic: Int,
        pulseBpm: Int = 72,
        notes: String = "",
        privacyScope: String = PrivacyScope.PERSONAL,
        memberId: String = "",
        memberName: String = "",
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        dateIso = dateIso,
        dateEpochDay = runCatching { java.time.LocalDate.parse(dateIso).toEpochDay() }.getOrDefault(DateCalendarUtils.todayEpochDay()),
        timeHHmm = timeFormatted,
        systolic = systolic,
        diastolic = diastolic,
        pulseBpm = pulseBpm,
        notes = notes,
        privacyScope = privacyScope,
        memberId = memberId,
        memberName = memberName,
        createdAtMillis = timestampEpochMillis
    )

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
 * 5. Sugestão Financeira Inteligente ligada à Saúde (mantida para compatibilidade de dados)
 */
data class HealthFinanceSuggestion(
    val id: String,
    val title: String,
    val category: String,
    val emoji: String,
    val defaultAmountBrl: Double,
    val description: String,
    val privacyScope: String = PrivacyScope.FAMILY
)

/**
 * 6. Estado do Cronômetro / Timer Fixo de 1 Hora Pós-Refeição
 */
data class PostMealTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mealLabel: String = DiabetesSlotCatalog.POS_ALMOCO_1H,
    val totalDurationSeconds: Int = 3600, // Fixo em 1 hora = 3600s
    val targetEndEpochMillis: Long = 0L,
    val pausedRemainingSeconds: Int = 3600,
    val startedAtHHmm: String = "",
    val completedAlertPending: Boolean = false
) {
    val durationMillis: Long
        get() = totalDurationSeconds * 1000L

    val targetEpochMillis: Long
        get() = targetEndEpochMillis

    val pausedRemainingMillis: Long
        get() = if (isPaused) pausedRemainingSeconds * 1000L else 0L
}

/**
 * Estado consolidado do Módulo de Saúde
 */
data class HealthModuleState(
    val trackers: List<DailyTrackerHabit> = emptyList(),
    // Mapa: dateIso (YYYY-MM-DD) -> (trackerId -> valor atual no dia)
    val trackerProgressByDate: Map<String, Map<String, Int>> = emptyMap(),
    val glucoseRecords: List<GlucoseRecord> = emptyList(),
    val bloodPressureRecords: List<BloodPressureRecord> = emptyList(),
    val insulinRecords: List<InsulinRecord> = emptyList(),
    val insulinConfigs: List<InsulinScheduleConfig> = emptyList(),
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

    fun matchesMember(
        recMemberId: String,
        recMemberName: String,
        activeMemberId: String,
        activeMemberName: String,
        isSingleMemberFamily: Boolean = false
    ): Boolean {
        if (activeMemberId.isBlank() && activeMemberName.isBlank()) return true
        val idMatch = recMemberId.isNotBlank() && recMemberId == activeMemberId
        val nameMatch = recMemberName.isNotBlank() && recMemberName.equals(activeMemberName, ignoreCase = true)
        val isLegacyUnassigned = isPlaceholderMember(recMemberId, recMemberName)
        return idMatch || nameMatch || (isLegacyUnassigned && isSingleMemberFamily)
    }

    fun latestInsulinForMember(
        activeMemberId: String = "",
        activeMemberName: String = "",
        allRecordsFallback: Boolean = true
    ): InsulinRecord? {
        val memberMatches = insulinRecords.filter { rec ->
            (activeMemberId.isNotBlank() && rec.memberId == activeMemberId) ||
                (activeMemberName.isNotBlank() && rec.memberName.equals(activeMemberName, ignoreCase = true)) ||
                isPlaceholderMember(rec.memberId, rec.memberName)
        }
        return memberMatches.firstOrNull() ?: if (allRecordsFallback) insulinRecords.firstOrNull() else null
    }

    fun glucoseRecordsForMemberAndDate(
        dateEpochDay: Long,
        memberId: String = "",
        memberName: String = ""
    ): List<GlucoseRecord> {
        val targetIso = DateCalendarUtils.formatIsoDate(dateEpochDay)
        return glucoseRecords.filter { rec ->
            (rec.dateEpochDay == dateEpochDay || rec.dateIso == targetIso) &&
                matchesMember(rec.memberId, rec.memberName, memberId, memberName, isSingleMemberFamily = true)
        }
    }

    fun formatLatestInsulinBanner(
        memberId: String = "",
        memberName: String = "",
        todayIso: String = ""
    ): String {
        val latest = latestInsulinForMember(memberId, memberName, allRecordsFallback = true)
            ?: return "Última insulina: Nenhuma aplicação registrada ainda"
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val whenText = when {
            latest.dateEpochDay == todayEpoch || (todayIso.isNotBlank() && latest.dateIso == todayIso) -> "hoje às ${latest.timeHHmm}"
            latest.dateEpochDay == todayEpoch - 1L -> "ontem às ${latest.timeHHmm}"
            else -> "${DateCalendarUtils.formatEpochDayPt(latest.dateEpochDay)} às ${latest.timeHHmm}"
        }
        val shortType = latest.insulinType.substringBefore(" (").trim().ifBlank { latest.insulinType }
        return "Última insulina: $shortType ${latest.doseUnits} UI • $whenText (${latest.site})"
    }

    fun lastApplicationBySiteForMember(
        activeMemberId: String = "",
        activeMemberName: String = ""
    ): Map<String, InsulinRecord> {
        val filtered = insulinRecords.filter { rec ->
            (activeMemberId.isBlank() && activeMemberName.isBlank()) ||
                rec.memberId == activeMemberId ||
                rec.memberName.equals(activeMemberName, ignoreCase = true) ||
                isPlaceholderMember(rec.memberId, rec.memberName)
        }
        val result = linkedMapOf<String, InsulinRecord>()
        for (site in InsulinSiteCatalog.allSites) {
            val match = filtered.firstOrNull { it.site.equals(site, ignoreCase = true) }
            if (match != null) {
                result[site] = match
            }
        }
        return result
    }

    companion object {
        fun isPlaceholderMember(memberId: String, memberName: String): Boolean {
            val cleanName = memberName.trim().lowercase(Locale.ROOT)
            val cleanId = memberId.trim()
            val placeholderName = cleanName.isEmpty() ||
                cleanName == "você" ||
                cleanName == "voce" ||
                cleanName == "vo" ||
                cleanName == "família" ||
                cleanName == "familia"
            val placeholderId = cleanId.isEmpty() ||
                cleanId == "LOCAL_TEST_USER" ||
                cleanId == "local_offline_user" ||
                cleanId == "member_admin"
            return placeholderName && placeholderId || cleanName == "você" || cleanName == "vo"
        }
    }
}

object HealthCatalog {
    val mealOptions: List<String> = DiabetesSlotCatalog.postMealTimerSlots
}

class HealthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val stateFlow: StateFlow<HealthModuleState> = getOrInitSharedFlow(appContext)

    private fun getFirestoreDb(): FirebaseFirestore? {
        return runCatching {
            FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
        }.getOrNull()
    }

    private fun getActiveHouseholdId(): String {
        val fromSync = activeSyncedHouseholdId
        if (fromSync.isNotBlank()) return fromSync
        return runCatching {
            LocalOfflineStore(appContext).userProfileFlow.value.householdId
                .ifBlank { LocalOfflineStore(appContext).householdFlow.value.householdId }
        }.getOrDefault("")
    }

    private fun isCloudAuthenticated(): Boolean {
        return runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
    }

    /**
     * Migra automaticamente registros antigos sem memberId válido ou com "Você"/"VO"/"Família"
     * para o membro logado (ex.: Marcus) e normaliza todos os slots para os 6 períodos oficiais.
     */
    fun migratePlaceholderMembersToLoggedMember(
        loggedMemberId: String,
        loggedMemberName: String
    ) {
        val cleanName = loggedMemberName.trim()
        if (cleanName.isBlank() ||
            cleanName.equals("Você", ignoreCase = true) ||
            cleanName.equals("VO", ignoreCase = true) ||
            cleanName.equals("Família", ignoreCase = true) ||
            cleanName.equals("Família Omni", ignoreCase = true)
        ) {
            return
        }
        val cleanId = loggedMemberId.trim().ifBlank { "mem_${cleanName.lowercase(Locale.ROOT)}" }
        val current = sharedStateFlow?.value ?: loadStateFromPrefs(appContext)
        var changed = false

        val migratedGlucose = current.glucoseRecords.map { rec ->
            val normalizedSlot = DiabetesSlotCatalog.normalizeSlot(rec.slot)
            val needsMemberFix = HealthModuleState.isPlaceholderMember(rec.memberId, rec.memberName)
            if (normalizedSlot != rec.slot || needsMemberFix) {
                changed = true
                val updatedRec = rec.copy(
                    slot = normalizedSlot,
                    memberId = if (needsMemberFix) cleanId else rec.memberId,
                    memberName = if (needsMemberFix) cleanName else rec.memberName
                )
                pushGlucoseRecordToFirestore(updatedRec)
                updatedRec
            } else {
                rec
            }
        }

        val migratedBp = current.bloodPressureRecords.map { rec ->
            val needsMemberFix = HealthModuleState.isPlaceholderMember(rec.memberId, rec.memberName)
            if (needsMemberFix) {
                changed = true
                val updatedRec = rec.copy(
                    memberId = cleanId,
                    memberName = cleanName
                )
                pushBpRecordToFirestore(updatedRec)
                updatedRec
            } else {
                rec
            }
        }

        val migratedInsulin = current.insulinRecords.map { rec ->
            val needsMemberFix = HealthModuleState.isPlaceholderMember(rec.memberId, rec.memberName)
            if (needsMemberFix) {
                changed = true
                val updatedRec = rec.copy(
                    memberId = cleanId,
                    memberName = cleanName
                )
                pushInsulinRecordToFirestore(updatedRec)
                updatedRec
            } else {
                rec
            }
        }

        if (changed) {
            saveState(
                current.copy(
                    glucoseRecords = migratedGlucose,
                    bloodPressureRecords = migratedBp,
                    insulinRecords = migratedInsulin
                )
            )
        }
    }

    /**
     * Conecta listeners em tempo real para households/{householdId}/health_logs e
     * households/{householdId}/insulin_records, garantindo zero duplicação de listeners.
     */
    fun attachHouseholdRealtimeSync(
        householdId: String,
        loggedMemberId: String = "",
        loggedMemberName: String = "",
        loggedUserId: String = "",
        loggedUserName: String = ""
    ) {
        val resolvedMemberId = loggedMemberId.ifBlank { loggedUserId }
        val resolvedMemberName = loggedMemberName.ifBlank { loggedUserName }
        val cleanHouseId = householdId.trim()
        if (cleanHouseId.isBlank() || !isCloudAuthenticated()) return

        if (resolvedMemberName.isNotBlank()) {
            migratePlaceholderMembersToLoggedMember(resolvedMemberId, resolvedMemberName)
        }

        synchronized(listenerLock) {
            if (activeSyncedHouseholdId == cleanHouseId &&
                healthLogsRegistration != null &&
                insulinRecordsRegistration != null
            ) {
                return
            }
            detachHouseholdRealtimeSync()
            activeSyncedHouseholdId = cleanHouseId
            val db = getFirestoreDb() ?: return
            val houseRef = db.collection("households").document(cleanHouseId)

            // Push any local records that aren't in Firestore yet
            pushAllLocalHealthAndInsulinToFirestore(cleanHouseId)

            healthLogsRegistration = houseRef.collection("health_logs")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val remoteGlucose = mutableListOf<GlucoseRecord>()
                    val remoteBp = mutableListOf<BloodPressureRecord>()
                    for (doc in snap.documents) {
                        val logType = doc.getString("logType") ?: if (doc.contains("valueMgDl")) "GLUCOSE" else "BP"
                        if (logType == "GLUCOSE") {
                            val valMgDl = (doc.getLong("valueMgDl") ?: 0L).toInt()
                            if (valMgDl > 0) {
                                val rawMemName = doc.getString("memberName").orEmpty()
                                val rawMemId = doc.getString("memberId").orEmpty()
                                val isPlaceholder = HealthModuleState.isPlaceholderMember(rawMemId, rawMemName)
                                val resolvedName = if (isPlaceholder && loggedMemberName.isNotBlank() && !loggedMemberName.equals("Você", true)) {
                                    loggedMemberName
                                } else rawMemName
                                val resolvedId = if (isPlaceholder && loggedMemberId.isNotBlank()) {
                                    loggedMemberId
                                } else rawMemId
                                val dateEpoch = doc.getLong("dateEpochDay") ?: DateCalendarUtils.todayEpochDay()
                                remoteGlucose.add(
                                    GlucoseRecord(
                                        id = doc.id,
                                        dateIso = doc.getString("dateIso") ?: DateCalendarUtils.formatIsoDate(dateEpoch),
                                        dateEpochDay = dateEpoch,
                                        timeHHmm = doc.getString("timeHHmm") ?: "08:00",
                                        slot = DiabetesSlotCatalog.normalizeSlot(doc.getString("slot") ?: DiabetesSlotCatalog.JEJUM),
                                        valueMgDl = valMgDl,
                                        notes = doc.getString("notes").orEmpty(),
                                        privacyScope = doc.getString("privacyScope") ?: PrivacyScope.FAMILY,
                                        memberId = resolvedId,
                                        memberName = resolvedName,
                                        createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        } else if (logType == "BP") {
                            val sys = (doc.getLong("systolic") ?: 0L).toInt()
                            val dia = (doc.getLong("diastolic") ?: 0L).toInt()
                            if (sys > 0 && dia > 0) {
                                val rawMemName = doc.getString("memberName").orEmpty()
                                val rawMemId = doc.getString("memberId").orEmpty()
                                val isPlaceholder = HealthModuleState.isPlaceholderMember(rawMemId, rawMemName)
                                val resolvedName = if (isPlaceholder && loggedMemberName.isNotBlank() && !loggedMemberName.equals("Você", true)) {
                                    loggedMemberName
                                } else rawMemName
                                val resolvedId = if (isPlaceholder && loggedMemberId.isNotBlank()) {
                                    loggedMemberId
                                } else rawMemId
                                val dateEpoch = doc.getLong("dateEpochDay") ?: DateCalendarUtils.todayEpochDay()
                                remoteBp.add(
                                    BloodPressureRecord(
                                        id = doc.id,
                                        dateIso = doc.getString("dateIso") ?: DateCalendarUtils.formatIsoDate(dateEpoch),
                                        dateEpochDay = dateEpoch,
                                        timeHHmm = doc.getString("timeHHmm") ?: "21:00",
                                        systolic = sys,
                                        diastolic = dia,
                                        pulseBpm = (doc.getLong("pulseBpm") ?: 72L).toInt(),
                                        notes = doc.getString("notes").orEmpty(),
                                        privacyScope = doc.getString("privacyScope") ?: PrivacyScope.FAMILY,
                                        memberId = resolvedId,
                                        memberName = resolvedName,
                                        createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                    val current = sharedStateFlow?.value ?: loadStateFromPrefs(appContext)
                    if (snap.documents.isNotEmpty() || (current.glucoseRecords.isEmpty() && current.bloodPressureRecords.isEmpty())) {
                        val sortedGl = remoteGlucose.sortedWith(
                            compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                        )
                        val sortedBp = remoteBp.sortedWith(
                            compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                        )
                        saveState(
                            current.copy(
                                glucoseRecords = sortedGl,
                                bloodPressureRecords = sortedBp
                            )
                        )
                    }
                }

            insulinRecordsRegistration = houseRef.collection("insulin_records")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val remoteInsulin = snap.documents.mapNotNull { doc ->
                        val dose = (doc.getLong("doseUnits") ?: 0L).toInt()
                        if (dose <= 0) return@mapNotNull null
                        val rawMemName = doc.getString("memberName").orEmpty()
                        val rawMemId = doc.getString("memberId").orEmpty()
                        val isPlaceholder = HealthModuleState.isPlaceholderMember(rawMemId, rawMemName)
                        val resolvedName = if (isPlaceholder && loggedMemberName.isNotBlank() && !loggedMemberName.equals("Você", true)) {
                            loggedMemberName
                        } else rawMemName
                        val resolvedId = if (isPlaceholder && loggedMemberId.isNotBlank()) {
                            loggedMemberId
                        } else rawMemId
                        val dateEpoch = doc.getLong("dateEpochDay") ?: DateCalendarUtils.todayEpochDay()
                        InsulinRecord(
                            id = doc.id,
                            householdId = cleanHouseId,
                            dateIso = doc.getString("dateIso") ?: DateCalendarUtils.formatIsoDate(dateEpoch),
                            dateEpochDay = dateEpoch,
                            timeHHmm = doc.getString("timeHHmm") ?: "08:00",
                            insulinType = doc.getString("insulinType") ?: InsulinTypeCatalog.BASAL,
                            doseUnits = dose,
                            site = InsulinSiteCatalog.normalizeSite(doc.getString("site") ?: InsulinSiteCatalog.ABDOMEN_ESQ),
                            notes = doc.getString("notes").orEmpty(),
                            memberId = resolvedId,
                            memberName = resolvedName,
                            createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                        )
                    }.sortedWith(
                        compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                    )
                    val current = sharedStateFlow?.value ?: loadStateFromPrefs(appContext)
                    if (snap.documents.isNotEmpty() || current.insulinRecords.isEmpty()) {
                        saveState(current.copy(insulinRecords = remoteInsulin))
                    }
                }
        }
    }

    fun syncInsulinConfigsFromRemoteHousehold(remoteConfigs: List<InsulinScheduleConfig>) {
        if (remoteConfigs.isEmpty()) return
        val current = sharedStateFlow?.value ?: loadStateFromPrefs(appContext)
        if (current.insulinConfigs != remoteConfigs) {
            saveState(current.copy(insulinConfigs = remoteConfigs))
            AlarmScheduler.syncInsulinScheduleAlarms(appContext, remoteConfigs)
        }
    }

    fun replaceWithCloudHealthPull(
        householdId: String = "",
        glucoseRecords: List<GlucoseRecord>,
        bloodPressureRecords: List<BloodPressureRecord>,
        insulinRecords: List<InsulinRecord>,
        insulinConfigs: List<InsulinScheduleConfig>,
        loggedUserId: String = "",
        loggedUserName: String = ""
    ) {
        if (loggedUserName.isNotBlank()) {
            migratePlaceholderMembersToLoggedMember(loggedUserId, loggedUserName)
        }
        val current = sharedStateFlow?.value ?: loadStateFromPrefs(appContext)
        // Merge with any local records not yet on server
        val mergedGl = (glucoseRecords + current.glucoseRecords)
            .distinctBy { it.id }
            .map { it.copy(slot = DiabetesSlotCatalog.normalizeSlot(it.slot)) }
            .sortedWith(compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm })
        val mergedBp = (bloodPressureRecords + current.bloodPressureRecords)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm })
        val mergedIns = (insulinRecords + current.insulinRecords)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm })
        val resolvedConfigs = insulinConfigs.ifEmpty { current.insulinConfigs }

        saveState(
            current.copy(
                glucoseRecords = mergedGl,
                bloodPressureRecords = mergedBp,
                insulinRecords = mergedIns,
                insulinConfigs = resolvedConfigs
            )
        )
        if (resolvedConfigs.isNotEmpty()) {
            AlarmScheduler.syncInsulinScheduleAlarms(appContext, resolvedConfigs)
        }
        val resolvedHouseId = householdId.ifBlank { getActiveHouseholdId() }
        if (resolvedHouseId.isNotBlank()) {
            pushAllLocalHealthAndInsulinToFirestore(resolvedHouseId)
        }
    }

    private fun pushAllLocalHealthAndInsulinToFirestore(householdId: String) {
        if (householdId.isBlank() || !isCloudAuthenticated()) return
        val current = sharedStateFlow?.value ?: return
        current.glucoseRecords.forEach { pushGlucoseRecordToFirestore(it, householdId) }
        current.bloodPressureRecords.forEach { pushBpRecordToFirestore(it, householdId) }
        current.insulinRecords.forEach { pushInsulinRecordToFirestore(it, householdId) }
    }

    private fun pushGlucoseRecordToFirestore(record: GlucoseRecord, explicitHouseholdId: String = "") {
        val houseId = explicitHouseholdId.ifBlank { getActiveHouseholdId() }
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        val payload = mapOf(
            "id" to record.id,
            "logType" to "GLUCOSE",
            "householdId" to houseId,
            "dateIso" to record.dateIso,
            "dateEpochDay" to record.dateEpochDay,
            "timeHHmm" to record.timeHHmm,
            "slot" to DiabetesSlotCatalog.normalizeSlot(record.slot),
            "valueMgDl" to record.valueMgDl,
            "notes" to record.notes,
            "privacyScope" to record.privacyScope,
            "memberId" to record.memberId,
            "memberName" to record.memberName,
            "createdAtMillis" to record.createdAtMillis
        )
        runCatching {
            db.collection("households")
                .document(houseId)
                .collection("health_logs")
                .document(record.id)
                .set(payload, SetOptions.merge())
        }
    }

    private fun pushBpRecordToFirestore(record: BloodPressureRecord, explicitHouseholdId: String = "") {
        val houseId = explicitHouseholdId.ifBlank { getActiveHouseholdId() }
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        val payload = mapOf(
            "id" to record.id,
            "logType" to "BP",
            "householdId" to houseId,
            "dateIso" to record.dateIso,
            "dateEpochDay" to record.dateEpochDay,
            "timeHHmm" to record.timeHHmm,
            "systolic" to record.systolic,
            "diastolic" to record.diastolic,
            "pulseBpm" to record.pulseBpm,
            "notes" to record.notes,
            "privacyScope" to record.privacyScope,
            "memberId" to record.memberId,
            "memberName" to record.memberName,
            "createdAtMillis" to record.createdAtMillis
        )
        runCatching {
            db.collection("households")
                .document(houseId)
                .collection("health_logs")
                .document(record.id)
                .set(payload, SetOptions.merge())
        }
    }

    private fun deleteHealthLogFromFirestore(recordId: String) {
        val houseId = getActiveHouseholdId()
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        runCatching {
            db.collection("households")
                .document(houseId)
                .collection("health_logs")
                .document(recordId)
                .delete()
        }
    }

    private fun pushInsulinRecordToFirestore(record: InsulinRecord, explicitHouseholdId: String = "") {
        val houseId = explicitHouseholdId.ifBlank { record.householdId.ifBlank { getActiveHouseholdId() } }
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        val payload = mapOf(
            "id" to record.id,
            "householdId" to houseId,
            "dateIso" to record.dateIso,
            "dateEpochDay" to record.dateEpochDay,
            "timeHHmm" to record.timeHHmm,
            "insulinType" to record.insulinType,
            "doseUnits" to record.doseUnits,
            "site" to InsulinSiteCatalog.normalizeSite(record.site),
            "notes" to record.notes,
            "memberId" to record.memberId,
            "memberName" to record.memberName,
            "createdAtMillis" to record.createdAtMillis
        )
        runCatching {
            db.collection("households")
                .document(houseId)
                .collection("insulin_records")
                .document(record.id)
                .set(payload, SetOptions.merge())
        }
    }

    private fun deleteInsulinRecordFromFirestore(recordId: String) {
        val houseId = getActiveHouseholdId()
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        runCatching {
            db.collection("households")
                .document(houseId)
                .collection("insulin_records")
                .document(recordId)
                .delete()
        }
    }

    private fun pushInsulinConfigsToHouseholdFirestore(configs: List<InsulinScheduleConfig>) {
        val houseId = getActiveHouseholdId()
        if (houseId.isBlank() || !isCloudAuthenticated()) return
        val db = getFirestoreDb() ?: return
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }.getOrDefault("")
        val listMaps = configs.map { cfg ->
            mapOf(
                "id" to cfg.id,
                "insulinType" to cfg.insulinType,
                "defaultDoseUnits" to cfg.defaultDoseUnits,
                "fixedTimeHHmm" to cfg.fixedTimeHHmm,
                "alarmEnabled" to cfg.alarmEnabled,
                "memberId" to cfg.memberId,
                "memberName" to cfg.memberName
            )
        }
        runCatching {
            val updates = mutableMapOf<String, Any>(
                "insulinConfigs" to listMaps,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            if (uid.isNotBlank()) {
                updates["memberUids"] = com.google.firebase.firestore.FieldValue.arrayUnion(uid)
            }
            db.collection("households")
                .document(houseId)
                .update(updates)
        }
    }

    private fun saveState(updated: HealthModuleState) {
        saveStateToPrefs(appContext, prefs, updated)
        sharedStateFlow?.value = updated
        com.example.widget.OmniHomeWidgetProvider.requestUpdate(appContext)
    }

    // =========================================================================
    // 1. OPERAÇÕES DOS INEGOCIÁVEIS (TRACKERS DIÁRIOS)
    // =========================================================================

    fun incrementTrackerProgress(dateIso: String, trackerId: String, delta: Int) {
        val current = stateFlow.value
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
        val current = stateFlow.value
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
        val current = stateFlow.value
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

    fun updateCustomTracker(
        trackerId: String,
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
        val current = stateFlow.value
        val updatedTrackers = current.trackers.map { existing ->
            if (existing.id == trackerId) {
                existing.copy(
                    title = cleanTitle,
                    emoji = emoji.ifBlank { existing.emoji },
                    unit = unit.trim().ifBlank { existing.unit },
                    targetValue = targetValue.coerceAtLeast(1),
                    stepValue = stepValue.coerceAtLeast(1),
                    colorHex = colorHex,
                    privacyScope = privacyScope
                )
            } else {
                existing
            }
        }
        saveState(current.copy(trackers = updatedTrackers))
    }

    fun deleteTracker(trackerId: String) {
        val current = stateFlow.value
        saveState(current.copy(trackers = current.trackers.filterNot { it.id == trackerId }))
    }

    // =========================================================================
    // 2. OPERAÇÕES DE GLICEMIA, INSULINA E PRESSÃO ARTERIAL
    // =========================================================================

    fun addGlucoseRecord(
        dateEpochDay: Long,
        timeHHmm: String,
        slot: String,
        valueMgDl: Int,
        contextTag: String = "",
        notes: String = "",
        privacyScope: String = PrivacyScope.FAMILY,
        memberId: String = "",
        memberName: String = ""
    ) {
        val current = stateFlow.value
        val combinedNotes = listOf(contextTag.trim(), notes.trim())
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" • ")
        val newRecord = GlucoseRecord(
            id = "gl_" + UUID.randomUUID().toString().take(8),
            dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
            dateEpochDay = dateEpochDay,
            timeHHmm = timeHHmm.ifBlank { DateCalendarUtils.currentTimeHHmm() },
            slot = DiabetesSlotCatalog.normalizeSlot(slot),
            valueMgDl = valueMgDl.coerceIn(20, 600),
            notes = combinedNotes,
            privacyScope = privacyScope,
            memberId = memberId,
            memberName = memberName
        )
        val sorted = (listOf(newRecord) + current.glucoseRecords).sortedWith(
            compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(glucoseRecords = sorted))
        pushGlucoseRecordToFirestore(newRecord)
    }

    fun addGlucoseRecord(
        dateIso: String,
        timeFormatted: String,
        slot: String,
        valueMgDl: Int,
        notes: String = "",
        privacyScope: String = PrivacyScope.FAMILY,
        memberId: String = "",
        memberName: String = ""
    ) {
        val epochDay = runCatching { java.time.LocalDate.parse(dateIso).toEpochDay() }
            .getOrDefault(DateCalendarUtils.todayEpochDay())
        addGlucoseRecord(
            dateEpochDay = epochDay,
            timeHHmm = timeFormatted,
            slot = slot,
            valueMgDl = valueMgDl,
            notes = notes,
            privacyScope = privacyScope,
            memberId = memberId,
            memberName = memberName
        )
    }

    fun updateGlucoseRecord(
        recordId: String,
        dateEpochDay: Long,
        timeHHmm: String,
        slot: String,
        valueMgDl: Int,
        notes: String,
        privacyScope: String,
        memberId: String = "",
        memberName: String = ""
    ) {
        val current = stateFlow.value
        var modifiedRecord: GlucoseRecord? = null
        val updated = current.glucoseRecords.map { existing ->
            if (existing.id == recordId) {
                val mod = existing.copy(
                    dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
                    dateEpochDay = dateEpochDay,
                    timeHHmm = timeHHmm.ifBlank { existing.timeHHmm },
                    slot = DiabetesSlotCatalog.normalizeSlot(slot),
                    valueMgDl = valueMgDl.coerceIn(20, 600),
                    notes = notes.trim(),
                    privacyScope = privacyScope,
                    memberId = memberId.ifBlank { existing.memberId },
                    memberName = memberName.ifBlank { existing.memberName }
                )
                modifiedRecord = mod
                mod
            } else {
                existing
            }
        }.sortedWith(
            compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(glucoseRecords = updated))
        modifiedRecord?.let { pushGlucoseRecordToFirestore(it) }
    }

    fun deleteGlucoseRecord(recordId: String) {
        val current = stateFlow.value
        saveState(current.copy(glucoseRecords = current.glucoseRecords.filterNot { it.id == recordId }))
        deleteHealthLogFromFirestore(recordId)
    }

    fun addInsulinRecord(
        dateEpochDay: Long,
        timeHHmm: String,
        insulinType: String,
        doseUnits: Int = 0,
        units: Int = 0,
        site: String = "",
        injectionSite: String = "",
        notes: String = "",
        memberId: String = "",
        memberName: String = ""
    ): InsulinRecord {
        val resolvedDose = if (doseUnits > 0) doseUnits else if (units > 0) units else 10
        val resolvedSite = site.ifBlank { injectionSite.ifBlank { InsulinSiteCatalog.ABDOMEN_ESQ } }
        val current = stateFlow.value
        val houseId = getActiveHouseholdId()
        val newRecord = InsulinRecord(
            id = "ins_" + UUID.randomUUID().toString().take(8),
            householdId = houseId,
            dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
            dateEpochDay = dateEpochDay,
            timeHHmm = timeHHmm.ifBlank { DateCalendarUtils.currentTimeHHmm() },
            insulinType = insulinType.trim().ifBlank { InsulinTypeCatalog.BASAL },
            doseUnits = resolvedDose.coerceIn(1, 300),
            site = InsulinSiteCatalog.normalizeSite(resolvedSite),
            notes = notes.trim(),
            memberId = memberId,
            memberName = memberName,
            createdAtMillis = System.currentTimeMillis()
        )
        val sorted = (listOf(newRecord) + current.insulinRecords).sortedWith(
            compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(insulinRecords = sorted))
        pushInsulinRecordToFirestore(newRecord, houseId)
        return newRecord
    }

    fun updateInsulinRecord(
        recordId: String,
        dateEpochDay: Long,
        timeHHmm: String,
        insulinType: String,
        doseUnits: Int = 0,
        units: Int = 0,
        site: String = "",
        injectionSite: String = "",
        notes: String = "",
        memberId: String = "",
        memberName: String = ""
    ) {
        val resolvedDose = if (doseUnits > 0) doseUnits else if (units > 0) units else 10
        val resolvedSite = site.ifBlank { injectionSite.ifBlank { InsulinSiteCatalog.ABDOMEN_ESQ } }
        val current = stateFlow.value
        var modifiedRecord: InsulinRecord? = null
        val updated = current.insulinRecords.map { existing ->
            if (existing.id == recordId) {
                val mod = existing.copy(
                    dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
                    dateEpochDay = dateEpochDay,
                    timeHHmm = timeHHmm.ifBlank { existing.timeHHmm },
                    insulinType = insulinType.trim().ifBlank { existing.insulinType },
                    doseUnits = resolvedDose.coerceIn(1, 300),
                    site = InsulinSiteCatalog.normalizeSite(resolvedSite),
                    notes = notes.trim(),
                    memberId = memberId.ifBlank { existing.memberId },
                    memberName = memberName.ifBlank { existing.memberName }
                )
                modifiedRecord = mod
                mod
            } else {
                existing
            }
        }.sortedWith(
            compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(insulinRecords = updated))
        modifiedRecord?.let { pushInsulinRecordToFirestore(it) }
    }

    fun deleteInsulinRecord(recordId: String) {
        val current = stateFlow.value
        saveState(current.copy(insulinRecords = current.insulinRecords.filterNot { it.id == recordId }))
        deleteInsulinRecordFromFirestore(recordId)
    }

    fun saveInsulinScheduleConfig(
        existingId: String? = null,
        configId: String = "",
        insulinType: String,
        defaultDoseUnits: Int = 0,
        defaultUnits: Int = 0,
        fixedTimeHHmm: String = "",
        alarmHour: Int = 8,
        alarmMinute: Int = 0,
        alarmEnabled: Boolean = true,
        memberId: String = "",
        memberName: String = ""
    ) {
        val current = stateFlow.value
        val resolvedId = existingId?.takeIf { it.isNotBlank() }
            ?: configId.takeIf { it.isNotBlank() }
            ?: ("inscfg_" + UUID.randomUUID().toString().take(8))
        val resolvedDose = if (defaultDoseUnits > 0) defaultDoseUnits else if (defaultUnits > 0) defaultUnits else 10
        val resolvedTime = fixedTimeHHmm.trim().ifBlank {
            String.format(Locale.ROOT, "%02d:%02d", alarmHour.coerceIn(0, 23), alarmMinute.coerceIn(0, 59))
        }
        val newConfig = InsulinScheduleConfig(
            id = resolvedId,
            insulinType = insulinType.trim().ifBlank { InsulinTypeCatalog.BASAL },
            defaultDoseUnits = resolvedDose.coerceIn(1, 300),
            fixedTimeHHmm = resolvedTime,
            alarmEnabled = alarmEnabled,
            memberId = memberId,
            memberName = memberName
        )
        val updatedConfigs = if (current.insulinConfigs.any { it.id == resolvedId }) {
            current.insulinConfigs.map { if (it.id == resolvedId) newConfig else it }
        } else {
            current.insulinConfigs + newConfig
        }
        saveState(current.copy(insulinConfigs = updatedConfigs))
        pushInsulinConfigsToHouseholdFirestore(updatedConfigs)
        AlarmScheduler.syncInsulinScheduleAlarms(appContext, updatedConfigs)
    }

    fun deleteInsulinScheduleConfig(configId: String) {
        val current = stateFlow.value
        val updatedConfigs = current.insulinConfigs.filterNot { it.id == configId }
        saveState(current.copy(insulinConfigs = updatedConfigs))
        pushInsulinConfigsToHouseholdFirestore(updatedConfigs)
        AlarmScheduler.cancelInsulinScheduleAlarm(appContext, configId)
    }

    fun addBloodPressureRecord(
        dateEpochDay: Long,
        timeHHmm: String,
        systolic: Int,
        diastolic: Int,
        pulseBpm: Int,
        notes: String,
        privacyScope: String,
        memberId: String = "",
        memberName: String = ""
    ) {
        val current = stateFlow.value
        val newRecord = BloodPressureRecord(
            id = "bp_" + UUID.randomUUID().toString().take(8),
            dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
            dateEpochDay = dateEpochDay,
            timeHHmm = timeHHmm.ifBlank { DateCalendarUtils.currentTimeHHmm() },
            systolic = systolic.coerceIn(50, 260),
            diastolic = diastolic.coerceIn(30, 180),
            pulseBpm = pulseBpm.coerceIn(35, 220),
            notes = notes.trim(),
            privacyScope = privacyScope,
            memberId = memberId,
            memberName = memberName
        )
        val sorted = (listOf(newRecord) + current.bloodPressureRecords).sortedWith(
            compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(bloodPressureRecords = sorted))
        pushBpRecordToFirestore(newRecord)
    }

    fun updateBloodPressureRecord(
        recordId: String,
        dateEpochDay: Long,
        timeHHmm: String,
        systolic: Int,
        diastolic: Int,
        pulseBpm: Int,
        notes: String,
        privacyScope: String,
        memberId: String = "",
        memberName: String = ""
    ) {
        val current = stateFlow.value
        var modifiedBp: BloodPressureRecord? = null
        val updated = current.bloodPressureRecords.map { existing ->
            if (existing.id == recordId) {
                val mod = existing.copy(
                    dateIso = DateCalendarUtils.formatIsoDate(dateEpochDay),
                    dateEpochDay = dateEpochDay,
                    timeHHmm = timeHHmm.ifBlank { existing.timeHHmm },
                    systolic = systolic.coerceIn(50, 260),
                    diastolic = diastolic.coerceIn(30, 180),
                    pulseBpm = pulseBpm.coerceIn(35, 220),
                    notes = notes.trim(),
                    privacyScope = privacyScope,
                    memberId = memberId.ifBlank { existing.memberId },
                    memberName = memberName.ifBlank { existing.memberName }
                )
                modifiedBp = mod
                mod
            } else {
                existing
            }
        }.sortedWith(
            compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
        )
        saveState(current.copy(bloodPressureRecords = updated))
        modifiedBp?.let { pushBpRecordToFirestore(it) }
    }

    fun deleteBloodPressureRecord(recordId: String) {
        val current = stateFlow.value
        saveState(current.copy(bloodPressureRecords = current.bloodPressureRecords.filterNot { it.id == recordId }))
        deleteHealthLogFromFirestore(recordId)
    }

    /**
     * Mescla registros de glicemia, pressão arterial e insulina recuperados de armazenamento legado ou backup JSON
     */
    fun mergeImportedHealthRecords(
        importedGlucose: List<GlucoseRecord>,
        importedBp: List<BloodPressureRecord>,
        importedInsulin: List<InsulinRecord> = emptyList()
    ): Int {
        val current = stateFlow.value
        val existingGlIds = current.glucoseRecords.map { it.id }.toMutableSet()
        val existingGlSigs = current.glucoseRecords.map { "${it.dateIso}_${it.timeHHmm}_${it.valueMgDl}" }.toMutableSet()

        val addedGlucose = mutableListOf<GlucoseRecord>()
        for (rec in importedGlucose) {
            val sig = "${rec.dateIso}_${rec.timeHHmm}_${rec.valueMgDl}"
            if (!existingGlIds.contains(rec.id) && !existingGlSigs.contains(sig)) {
                existingGlIds.add(rec.id)
                existingGlSigs.add(sig)
                val normalized = rec.copy(slot = DiabetesSlotCatalog.normalizeSlot(rec.slot))
                addedGlucose.add(normalized)
                pushGlucoseRecordToFirestore(normalized)
            }
        }

        val existingBpIds = current.bloodPressureRecords.map { it.id }.toMutableSet()
        val existingBpSigs = current.bloodPressureRecords.map { "${it.dateIso}_${it.timeHHmm}_${it.systolic}_${it.diastolic}" }.toMutableSet()
        val addedBp = mutableListOf<BloodPressureRecord>()
        for (bp in importedBp) {
            val sig = "${bp.dateIso}_${bp.timeHHmm}_${bp.systolic}_${bp.diastolic}"
            if (!existingBpIds.contains(bp.id) && !existingBpSigs.contains(sig)) {
                existingBpIds.add(bp.id)
                existingBpSigs.add(sig)
                addedBp.add(bp)
                pushBpRecordToFirestore(bp)
            }
        }

        val existingInsIds = current.insulinRecords.map { it.id }.toMutableSet()
        val existingInsSigs = current.insulinRecords.map { "${it.dateIso}_${it.timeHHmm}_${it.doseUnits}_${it.site}" }.toMutableSet()
        val addedInsulin = mutableListOf<InsulinRecord>()
        for (ins in importedInsulin) {
            val sig = "${ins.dateIso}_${ins.timeHHmm}_${ins.doseUnits}_${ins.site}"
            if (!existingInsIds.contains(ins.id) && !existingInsSigs.contains(sig)) {
                existingInsIds.add(ins.id)
                existingInsSigs.add(sig)
                val normalizedIns = ins.copy(site = InsulinSiteCatalog.normalizeSite(ins.site))
                addedInsulin.add(normalizedIns)
                pushInsulinRecordToFirestore(normalizedIns)
            }
        }

        val totalAdded = addedGlucose.size + addedBp.size + addedInsulin.size
        if (totalAdded > 0) {
            val mergedGl = (current.glucoseRecords + addedGlucose).sortedWith(
                compareByDescending<GlucoseRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
            )
            val mergedBp = (current.bloodPressureRecords + addedBp).sortedWith(
                compareByDescending<BloodPressureRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
            )
            val mergedIns = (current.insulinRecords + addedInsulin).sortedWith(
                compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
            )
            saveState(
                current.copy(
                    glucoseRecords = mergedGl,
                    bloodPressureRecords = mergedBp,
                    insulinRecords = mergedIns
                )
            )
        }
        return totalAdded
    }

    // =========================================================================
    // 3. CRONÔMETRO / TIMER FIXO DE 1 HORA (3600s) PÓS-REFEIÇÃO
    // =========================================================================

    fun startPostMealTimer(mealLabel: String, durationSeconds: Int = 3600) {
        val current = stateFlow.value
        val now = System.currentTimeMillis()
        val normalizedMealSlot = DiabetesSlotCatalog.slotForPostMealTimer(mealLabel, 3600)
        val fixedOneHourSeconds = 3600
        val updatedTimer = PostMealTimerState(
            isRunning = true,
            isPaused = false,
            mealLabel = normalizedMealSlot,
            totalDurationSeconds = fixedOneHourSeconds,
            targetEndEpochMillis = now + fixedOneHourSeconds * 1000L,
            pausedRemainingSeconds = fixedOneHourSeconds,
            startedAtHHmm = DateCalendarUtils.currentTimeHHmm(),
            completedAlertPending = false
        )
        saveState(current.copy(postMealTimer = updatedTimer))
    }

    fun pauseOrResumePostMealTimer() {
        val current = stateFlow.value
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

    fun resetPostMealTimer(mealLabel: String = stateFlow.value.postMealTimer.mealLabel) {
        val current = stateFlow.value
        val normalizedMealSlot = DiabetesSlotCatalog.slotForPostMealTimer(mealLabel, 3600)
        saveState(
            current.copy(
                postMealTimer = PostMealTimerState(
                    isRunning = false,
                    isPaused = false,
                    mealLabel = normalizedMealSlot,
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
        val current = stateFlow.value
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

    fun startPostMealTimer(mealLabel: String, durationMillis: Long) {
        startPostMealTimer(
            mealLabel = mealLabel,
            durationSeconds = 3600
        )
    }

    fun pausePostMealTimer() {
        val timer = stateFlow.value.postMealTimer
        if (timer.isRunning && !timer.isPaused) {
            pauseOrResumePostMealTimer()
        }
    }

    fun resumePostMealTimer() {
        val timer = stateFlow.value.postMealTimer
        if (timer.isRunning && timer.isPaused) {
            pauseOrResumePostMealTimer()
        }
    }

    fun markPostMealTimerCompleted() {
        markPostMealTimerFinished()
    }

    fun dismissPostMealTimerAlert() {
        val current = stateFlow.value
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
        val current = stateFlow.value
        val updated = current.pregnancyChecklist.map {
            if (it.id == itemId) it.copy(completed = !it.completed) else it
        }
        saveState(current.copy(pregnancyChecklist = updated))
    }

    fun addPregnancyNote(noteText: String) {
        val clean = noteText.trim()
        if (clean.isEmpty()) return
        val current = stateFlow.value
        val todayIso = DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())
        val entry = "$todayIso • $clean"
        saveState(current.copy(pregnancyNotesLog = listOf(entry) + current.pregnancyNotesLog))
    }

    fun recordGeneratedPdfInfo(path: String, summary: String) {
        val current = stateFlow.value
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

        private val listenerLock = Any()
        @Volatile
        private var sharedStateFlow: MutableStateFlow<HealthModuleState>? = null
        @Volatile
        private var activeSyncedHouseholdId: String = ""
        private var healthLogsRegistration: ListenerRegistration? = null
        private var insulinRecordsRegistration: ListenerRegistration? = null

        private fun getOrInitSharedFlow(appContext: Context): StateFlow<HealthModuleState> {
            val existing = sharedStateFlow
            if (existing != null) return existing.asStateFlow()
            return synchronized(listenerLock) {
                val again = sharedStateFlow
                if (again != null) {
                    again.asStateFlow()
                } else {
                    val created = MutableStateFlow(loadStateFromPrefs(appContext))
                    sharedStateFlow = created
                    created.asStateFlow()
                }
            }
        }

        fun detachHouseholdRealtimeSync() {
            synchronized(listenerLock) {
                runCatching { healthLogsRegistration?.remove() }
                runCatching { insulinRecordsRegistration?.remove() }
                healthLogsRegistration = null
                insulinRecordsRegistration = null
                activeSyncedHouseholdId = ""
            }
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
                notes = "Levar relatório PDF de Glicemia, Insulina e Pressão Arterial",
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
            return HealthModuleState(
                trackers = defaultTrackers(),
                trackerProgressByDate = emptyMap(),
                glucoseRecords = emptyList(),
                bloodPressureRecords = emptyList(),
                insulinRecords = emptyList(),
                insulinConfigs = emptyList(),
                pregnancyChecklist = defaultPregnancyChecklist(),
                pregnancyNotesLog = emptyList()
            )
        }

        private fun loadStateFromPrefs(appContext: Context): HealthModuleState {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

                // 3. Glicemia (normalizada para os 6 períodos oficiais)
                val glucoseArr = root.optJSONArray("glucoseRecords") ?: JSONArray()
                val glucoseList = mutableListOf<GlucoseRecord>()
                for (i in 0 until glucoseArr.length()) {
                    val obj = glucoseArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.startsWith("gl_seed_")) continue
                    glucoseList.add(
                        GlucoseRecord(
                            id = id,
                            dateIso = obj.optString("dateIso"),
                            dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                            timeHHmm = obj.optString("timeHHmm", "08:00"),
                            slot = DiabetesSlotCatalog.normalizeSlot(obj.optString("slot", DiabetesSlotCatalog.JEJUM)),
                            valueMgDl = obj.optInt("valueMgDl", 95),
                            notes = obj.optString("notes", ""),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            memberId = obj.optString("memberId", ""),
                            memberName = obj.optString("memberName", ""),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }

                // 4. Pressão Arterial
                val bpArr = root.optJSONArray("bloodPressureRecords") ?: JSONArray()
                val bpList = mutableListOf<BloodPressureRecord>()
                for (i in 0 until bpArr.length()) {
                    val obj = bpArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.startsWith("bp_seed_")) continue
                    bpList.add(
                        BloodPressureRecord(
                            id = id,
                            dateIso = obj.optString("dateIso"),
                            dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                            timeHHmm = obj.optString("timeHHmm", "21:00"),
                            systolic = obj.optInt("systolic", 120),
                            diastolic = obj.optInt("diastolic", 80),
                            pulseBpm = obj.optInt("pulseBpm", 72),
                            notes = obj.optString("notes", ""),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            memberId = obj.optString("memberId", ""),
                            memberName = obj.optString("memberName", ""),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }

                // 5. Insulina (Aplicações & Rodízio de Locais)
                val insulinArr = root.optJSONArray("insulinRecords") ?: JSONArray()
                val insulinList = mutableListOf<InsulinRecord>()
                for (i in 0 until insulinArr.length()) {
                    val obj = insulinArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    insulinList.add(
                        InsulinRecord(
                            id = id,
                            householdId = obj.optString("householdId", ""),
                            dateIso = obj.optString("dateIso"),
                            dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                            timeHHmm = obj.optString("timeHHmm", "08:00"),
                            insulinType = obj.optString("insulinType", InsulinTypeCatalog.BASAL),
                            doseUnits = obj.optInt("doseUnits", 10),
                            site = InsulinSiteCatalog.normalizeSite(obj.optString("site", InsulinSiteCatalog.ABDOMEN_ESQ)),
                            notes = obj.optString("notes", ""),
                            memberId = obj.optString("memberId", ""),
                            memberName = obj.optString("memberName", ""),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }

                // 6. Configurações de Horários Fixos de Insulina
                val insulinCfgArr = root.optJSONArray("insulinConfigs") ?: JSONArray()
                val insulinConfigsList = mutableListOf<InsulinScheduleConfig>()
                for (i in 0 until insulinCfgArr.length()) {
                    val obj = insulinCfgArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    insulinConfigsList.add(
                        InsulinScheduleConfig(
                            id = id,
                            insulinType = obj.optString("insulinType", InsulinTypeCatalog.BASAL),
                            defaultDoseUnits = obj.optInt("defaultDoseUnits", 10),
                            fixedTimeHHmm = obj.optString("fixedTimeHHmm", "08:00"),
                            alarmEnabled = obj.optBoolean("alarmEnabled", true),
                            memberId = obj.optString("memberId", ""),
                            memberName = obj.optString("memberName", "")
                        )
                    )
                }

                // 7. Checklist Gestacional
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

                // 8. Notas gestacionais
                val notesArr = root.optJSONArray("pregnancyNotesLog") ?: JSONArray()
                val pregNotes = mutableListOf<String>()
                for (i in 0 until notesArr.length()) {
                    pregNotes.add(notesArr.getString(i))
                }

                // 9. Timer 1h pós-refeição
                val timerObj = root.optJSONObject("postMealTimer")
                val timerState = if (timerObj != null) {
                    PostMealTimerState(
                        isRunning = timerObj.optBoolean("isRunning", false),
                        isPaused = timerObj.optBoolean("isPaused", false),
                        mealLabel = DiabetesSlotCatalog.slotForPostMealTimer(
                            timerObj.optString("mealLabel", DiabetesSlotCatalog.POS_ALMOCO_1H),
                            3600
                        ),
                        totalDurationSeconds = 3600,
                        targetEndEpochMillis = timerObj.optLong("targetEndEpochMillis", 0L),
                        pausedRemainingSeconds = timerObj.optInt("pausedRemainingSeconds", 3600).coerceIn(0, 3600),
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
                    insulinRecords = insulinList.sortedWith(
                        compareByDescending<InsulinRecord> { it.dateEpochDay }.thenByDescending { it.timeHHmm }
                    ),
                    insulinConfigs = insulinConfigsList,
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

        private fun saveStateToPrefs(
            appContext: Context,
            prefs: android.content.SharedPreferences,
            updated: HealthModuleState
        ) {
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
                        put("slot", DiabetesSlotCatalog.normalizeSlot(g.slot))
                        put("valueMgDl", g.valueMgDl)
                        put("notes", g.notes)
                        put("privacyScope", g.privacyScope)
                        put("memberId", g.memberId)
                        put("memberName", g.memberName)
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
                        put("memberId", bp.memberId)
                        put("memberName", bp.memberName)
                        put("createdAtMillis", bp.createdAtMillis)
                    }
                )
            }
            root.put("bloodPressureRecords", bpArr)

            val insulinArr = JSONArray()
            updated.insulinRecords.forEach { ins ->
                insulinArr.put(
                    JSONObject().apply {
                        put("id", ins.id)
                        put("householdId", ins.householdId)
                        put("dateIso", ins.dateIso)
                        put("dateEpochDay", ins.dateEpochDay)
                        put("timeHHmm", ins.timeHHmm)
                        put("insulinType", ins.insulinType)
                        put("doseUnits", ins.doseUnits)
                        put("site", InsulinSiteCatalog.normalizeSite(ins.site))
                        put("notes", ins.notes)
                        put("memberId", ins.memberId)
                        put("memberName", ins.memberName)
                        put("createdAtMillis", ins.createdAtMillis)
                    }
                )
            }
            root.put("insulinRecords", insulinArr)

            val insulinCfgArr = JSONArray()
            updated.insulinConfigs.forEach { cfg ->
                insulinCfgArr.put(
                    JSONObject().apply {
                        put("id", cfg.id)
                        put("insulinType", cfg.insulinType)
                        put("defaultDoseUnits", cfg.defaultDoseUnits)
                        put("fixedTimeHHmm", cfg.fixedTimeHHmm)
                        put("alarmEnabled", cfg.alarmEnabled)
                        put("memberId", cfg.memberId)
                        put("memberName", cfg.memberName)
                    }
                )
            }
            root.put("insulinConfigs", insulinCfgArr)

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
                    put("totalDurationSeconds", 3600)
                    put("targetEndEpochMillis", t.targetEndEpochMillis)
                    put("pausedRemainingSeconds", t.pausedRemainingSeconds)
                    put("startedAtHHmm", t.startedAtHHmm)
                    put("completedAlertPending", t.completedAlertPending)
                }
            )

            root.put("lastGeneratedPdfPath", updated.lastGeneratedPdfPath)
            root.put("lastGeneratedPdfSummary", updated.lastGeneratedPdfSummary)

            prefs.edit().putString(KEY_HEALTH_JSON, root.toString()).apply()
        }
    }
}

/**
 * 3. CALCULADORA GESTACIONAL (DPP) COM TABELA COMPLETA SEMANA 4 A 42 (DOCE / FRUTA / ANIMAL)
 */
object PregnancyCalculator {

    data class WeekComparisonInfo(
        val week: Int,
        val sweetComparison: String,
        val fruitComparison: String,
        val animalComparison: String,
        val lengthCm: Double,
        val weightGrams: Int
    )

    data class PregnancyProgress(
        val dppFormatted: String,
        val hasConfiguredDpp: Boolean,
        val currentWeek: Int,
        val currentDaysRemainder: Int,
        val daysRemainingToDpp: Int,
        val trimester: Int,
        val progressFraction40Weeks: Float,
        val daysToNextWeek: Int,
        val nextWeekNumber: Int,
        val daysToNextTrimester: Int,
        val sweetComparison: String,
        val fruitComparison: String,
        val animalComparison: String,
        val lengthCm: Double,
        val weightGrams: Int,
        val babySizeComparison: String
    ) {
        fun formattedLengthAndWeight(): String {
            val weightText = if (weightGrams >= 1000) {
                String.format(Locale.forLanguageTag("pt-BR"), "%.2f kg (%d g)", weightGrams / 1000.0, weightGrams)
            } else {
                "$weightGrams g"
            }
            val lengthText = String.format(Locale.forLanguageTag("pt-BR"), "%.1f cm", lengthCm)
            return "~$lengthText • ~$weightText"
        }

        fun gestationalAgeLabel(): String = "$currentWeek semanas e $currentDaysRemainder dia(s)"

        val gestationalAgeLabelPt: String
            get() = gestationalAgeLabel()

        val approxLengthCm: Double
            get() = lengthCm

        val approxWeightGrams: Int
            get() = weightGrams
    }

    val weeklyComparisonTable: Map<Int, WeekComparisonInfo> = listOf(
        WeekComparisonInfo(4, "Grão de açúcar / Granulado", "Semente de papoula", "Girino recém-nascido", 0.2, 1),
        WeekComparisonInfo(5, "Confeito colorido (Miçanga)", "Semente de gergelim", "Girino", 0.4, 1),
        WeekComparisonInfo(6, "Bala Tic Tac", "Semente de romã", "Joaninha", 0.6, 2),
        WeekComparisonInfo(7, "Jujuba / Bala de goma", "Mirtilo (Blueberry)", "Abelha", 1.3, 4),
        WeekComparisonInfo(8, "Ursinho de gelatina", "Framboesa", "Borboleta pequena", 1.6, 7),
        WeekComparisonInfo(9, "Trufa pequena / Cereja em calda", "Uva verde", "Peixinho neon", 2.3, 10),
        WeekComparisonInfo(10, "Bombom Ferrero Rocher", "Morango", "Beija-flor", 3.1, 14),
        WeekComparisonInfo(11, "Brigadeiro de festa", "Figo / Limão pequeno", "Camundongo baby", 4.1, 20),
        WeekComparisonInfo(12, "Macaron francês", "Ameixa / Limão Tahiti", "Pintinho recém-nascido", 5.4, 28),
        WeekComparisonInfo(13, "Pão de mel", "Pêssego", "Hamster sírio", 7.4, 43),
        WeekComparisonInfo(14, "Cupcake decorado", "Laranja Bahia", "Esquilo baby", 8.7, 57),
        WeekComparisonInfo(15, "Sonho de padaria", "Maçã Fuji", "Canário / Calopsita", 10.1, 70),
        WeekComparisonInfo(16, "Picolé artesanal", "Abacate médio", "Porquinho-da-índia", 11.6, 100),
        WeekComparisonInfo(17, "Croissant de chocolate", "Pera Williams", "Filhote de ouriço", 13.0, 140),
        WeekComparisonInfo(18, "Churros recheado", "Manga Palmer", "Furão jovem", 14.2, 190),
        WeekComparisonInfo(19, "Waffle belga com calda", "Manga Tommy grande", "Chinchila", 15.3, 240),
        WeekComparisonInfo(20, "Pote de açaí 300ml", "Banana Prata", "Coelhinho anão", 25.6, 300),
        WeekComparisonInfo(21, "Barra grande de chocolate", "Romã grande", "Filhote de lontra", 26.7, 360),
        WeekComparisonInfo(22, "Caixa de bombons gourmet", "Mamão Papaia", "Filhote de gato persa", 27.8, 430),
        WeekComparisonInfo(23, "Bolo inglês / Rocambole", "Toranja (Grapefruit)", "Filhote de pug", 28.9, 500),
        WeekComparisonInfo(24, "Pacote grande de algodão-doce", "Melão Cantaloupe", "Filhote de raposa", 30.0, 600),
        WeekComparisonInfo(25, "Torta holandesa média", "Cacho grande de uvas", "Filhote de panda-vermelho", 34.6, 660),
        WeekComparisonInfo(26, "Panetone trufado", "Melão Amarelo", "Filhote de lontra-marinha", 35.6, 760),
        WeekComparisonInfo(27, "Pudim de leite condensado", "Cacho de bananas", "Coelho adulto", 36.6, 875),
        WeekComparisonInfo(28, "Bolo de chocolate confeitado", "Coco seco", "Filhote de bicho-preguiça", 37.6, 1005),
        WeekComparisonInfo(29, "Torta de morango de vitrine", "Abacaxi Pérola", "Pato-real", 38.6, 1150),
        WeekComparisonInfo(30, "Ovo de Páscoa de 1kg", "Jaca pequena", "Gatinho Siamês jovem", 39.9, 1320),
        WeekComparisonInfo(31, "Bolo Red Velvet de 2 andares", "Coco Verde grande", "Filhote de lince", 41.1, 1500),
        WeekComparisonInfo(32, "Torta Banoffee família", "Abacaxi Havaí grande", "Filhote de pinguim-imperador", 42.4, 1700),
        WeekComparisonInfo(33, "Cheesecake grande com calda", "Melão Rei", "Filhote de foca", 43.7, 1920),
        WeekComparisonInfo(34, "Bolo Floresta Negra grande", "Melão Dino grande", "Cachorrinho Spitz / Lulu", 45.0, 2150),
        WeekComparisonInfo(35, "Cesta completa de doces gourmet", "Melancia Baby", "Filhote de coala", 46.2, 2380),
        WeekComparisonInfo(36, "Bolo de festa de 2,5kg", "Mamão Formosa grande", "Cachorrinho Beagle baby", 47.4, 2620),
        WeekComparisonInfo(37, "Bolo de aniversário de 3 andares", "Melancia média", "Filhote de coala crescido", 48.6, 2860),
        WeekComparisonInfo(38, "Torre de macarons e profiteroles", "Jaca média", "Gatinho Maine Coon jovem", 49.8, 3080),
        WeekComparisonInfo(39, "Bolo de casamento clássico", "Melancia grande", "Gatinho adulto", 50.7, 3290),
        WeekComparisonInfo(40, "Super bolo festivo", "Melancia", "Gatinho adulto / Filhote de coala", 51.2, 3460),
        WeekComparisonInfo(41, "Super bolo festivo imperial", "Melancia extra grande", "Gatinho adulto fofinho", 51.7, 3600),
        WeekComparisonInfo(42, "Super bolo festivo supremo", "Melancia gigante", "Filhote de coala abraçável", 52.0, 3700)
    ).associateBy { it.week }

    fun infoForWeek(week: Int): WeekComparisonInfo {
        val clamped = week.coerceIn(4, 42)
        return weeklyComparisonTable[clamped] ?: weeklyComparisonTable.getValue(40)
    }

    /**
     * Calcula a DPP (DD/MM/AAAA) a partir da data de um Ultrassom e da idade gestacional (semanas e dias) naquele exame.
     */
    fun calculateDppFromUltrasound(
        examDateBr: String,
        weeksAtExam: Int,
        daysAtExam: Int
    ): String? {
        val examEpoch = parseDdMmYyyyToEpochDay(examDateBr) ?: return null
        val elapsedAtExam = (weeksAtExam.coerceIn(1, 42) * 7) + daysAtExam.coerceIn(0, 6)
        val remainingFromExam = (280 - elapsedAtExam).coerceAtLeast(0)
        val dppEpoch = examEpoch + remainingFromExam.toLong()
        return DateCalendarUtils.formatEpochDayPt(dppEpoch)
    }

    fun calculateFromDpp(dppRaw: String): PregnancyProgress {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val parsedDppEpoch = parseDdMmYyyyToEpochDay(dppRaw)
        val hasValidDpp = parsedDppEpoch != null
        val effectiveDppEpoch = parsedDppEpoch ?: (todayEpoch + 112L) // fallback ~24 semanas se vazio

        val rawDaysRemaining = (effectiveDppEpoch - todayEpoch).toInt()
        val daysRemaining = rawDaysRemaining.coerceIn(0, 280)
        val elapsedDays = (280 - rawDaysRemaining).coerceIn(28, 294) // 4 semanas (28d) até 42 semanas (294d)
        val currentWeek = (elapsedDays / 7).coerceIn(4, 42)
        val currentDaysRem = (elapsedDays % 7).coerceIn(0, 6)

        val trimester = when {
            currentWeek <= 13 -> 1
            currentWeek <= 27 -> 2
            else -> 3
        }

        val progressFraction40Weeks = (elapsedDays.toFloat() / 280f).coerceIn(0f, 1f)
        val daysToNextWeek = if (currentWeek >= 42) 0 else (7 - currentDaysRem).coerceIn(1, 7)
        val nextWeekNumber = (currentWeek + 1).coerceAtMost(42)

        val daysToNextTrimester = when (trimester) {
            1 -> (14 * 7 - elapsedDays).coerceAtLeast(1) // 2º trimestre inicia na semana 14
            2 -> (28 * 7 - elapsedDays).coerceAtLeast(1) // 3º trimestre inicia na semana 28
            else -> daysRemaining // no 3º trimestre, próximo grande marco é a DPP
        }

        val weekInfo = infoForWeek(currentWeek)
        val summaryComparison = "${weekInfo.fruitComparison} (~${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", weekInfo.lengthCm)} cm)"

        return PregnancyProgress(
            dppFormatted = if (hasValidDpp) DateCalendarUtils.formatEpochDayPt(effectiveDppEpoch) else dppRaw.ifBlank { DateCalendarUtils.formatEpochDayPt(effectiveDppEpoch) },
            hasConfiguredDpp = hasValidDpp,
            currentWeek = currentWeek,
            currentDaysRemainder = currentDaysRem,
            daysRemainingToDpp = daysRemaining,
            trimester = trimester,
            progressFraction40Weeks = progressFraction40Weeks,
            daysToNextWeek = daysToNextWeek,
            nextWeekNumber = nextWeekNumber,
            daysToNextTrimester = daysToNextTrimester,
            sweetComparison = weekInfo.sweetComparison,
            fruitComparison = weekInfo.fruitComparison,
            animalComparison = weekInfo.animalComparison,
            lengthCm = weekInfo.lengthCm,
            weightGrams = weekInfo.weightGrams,
            babySizeComparison = summaryComparison
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
 * Consolida histórico de Glicemia (nos 6 períodos oficiais + contexto), Aplicações de Insulina (com local),
 * Pressão Arterial, Inegociáveis Diários e Acompanhamento Gestacional por período.
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
        val filteredInsulin = healthState.insulinRecords.filter { it.dateEpochDay >= minEpochDay }
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
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(80, 95, 88)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(16, 34, 26)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(25, 35, 30)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(45, 55, 50)
            textSize = 9f
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

        var y = 38f
        val leftMargin = 34f
        val rightMargin = pageWidth - 34f

        // Cabeçalho Clínico
        canvas.drawRoundRect(
            RectF(leftMargin, y - 16f, rightMargin, y + 46f),
            12f,
            12f,
            boxFillPaint
        )
        canvas.drawText("OMNI • RELATÓRIO MÉDICO CONSOLIDADO", leftMargin + 14f, y + 4f, titlePaint)
        val emittedNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        canvas.drawText(
            "Paciente / Família: $profileName   •   Período: $periodLabel   •   Emissão: $emittedNow",
            leftMargin + 14f,
            y + 21f,
            subtitlePaint
        )
        val activeModulesText = buildList {
            if (customizationConfig.diabetesEnabled || filteredGlucose.isNotEmpty() || filteredInsulin.isNotEmpty()) {
                add("Glicemia (6 Períodos) & Insulina")
            }
            if (customizationConfig.bloodPressureEnabled || filteredBp.isNotEmpty()) add("Pressão Arterial")
            if (customizationConfig.pregnancyEnabled || customizationConfig.pregnancyDppDate.isNotBlank()) {
                add("Gestação (DPP: ${customizationConfig.pregnancyDppDate.ifBlank { "Ativa" }})")
            }
            add("Inegociáveis Diários")
        }.joinToString(" | ")
        canvas.drawText("Módulos Monitorados: $activeModulesText", leftMargin + 14f, y + 36f, subtitlePaint)

        y += 66f

        // 1. Resumo Estatístico Clínico
        canvas.drawText("1. RESUMO ESTATÍSTICO DO PERÍODO ($periodLabel)", leftMargin, y, sectionHeaderPaint)
        y += 15f

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

        if (customizationConfig.diabetesEnabled || filteredGlucose.isNotEmpty()) {
            canvas.drawText(
                "• Glicemia (${filteredGlucose.size} medições): Média = ${if (avgGlucose > 0) "$avgGlucose mg/dL (Menor: $minGlucose / Maior: $maxGlucose mg/dL)" else "Sem registros no período"}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 13f

            // Média por cada um dos 6 períodos oficiais
            val slotSummary = DiabetesSlotCatalog.allSlots.mapNotNull { officialSlot ->
                val slotRecs = filteredGlucose.filter { DiabetesSlotCatalog.normalizeSlot(it.slot) == officialSlot }
                if (slotRecs.isNotEmpty()) {
                    val slotAvg = slotRecs.map { it.valueMgDl }.average().roundToInt()
                    "$officialSlot: $slotAvg mg/dL (${slotRecs.size}x)"
                } else null
            }.joinToString(" | ")
            if (slotSummary.isNotBlank()) {
                canvas.drawText(
                    "  Médias por período: ${slotSummary.take(95)}",
                    leftMargin + 8f,
                    y,
                    subtitlePaint
                )
                y += 13f
            }
        }

        if (customizationConfig.diabetesEnabled || filteredInsulin.isNotEmpty()) {
            val totalUnits = filteredInsulin.sumOf { it.doseUnits }
            canvas.drawText(
                "• Insulina (${filteredInsulin.size} aplicações): ${if (filteredInsulin.isNotEmpty()) "Total aplicado = $totalUnits UI • Último local: ${filteredInsulin.first().site}" else "Sem aplicações registradas no período"}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 13f
        }

        if (customizationConfig.bloodPressureEnabled || filteredBp.isNotEmpty()) {
            canvas.drawText(
                "• Pressão Arterial (${filteredBp.size} aferições): Média = ${if (avgSys > 0) "$avgSys/$avgDia mmHg (Pulso médio: $avgPulse bpm)" else "Sem registros no período"}",
                leftMargin + 8f,
                y,
                bodyBoldPaint
            )
            y += 13f
        }

        val completedTrackersToday = healthState.completedTrackersCount(todayIso)
        canvas.drawText(
            "• Hábitos Inegociáveis Hoje ($todayIso): $completedTrackersToday de ${healthState.trackers.size} metas concluídas",
            leftMargin + 8f,
            y,
            bodyBoldPaint
        )
        y += 12f
        canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
        y += 16f

        // 2. Histórico de Monitoramento de Glicemia nos 6 Períodos Oficiais + Contexto
        if (customizationConfig.diabetesEnabled || filteredGlucose.isNotEmpty()) {
            canvas.drawText("2. MONITORAMENTO DE GLICEMIA (6 PERÍODOS OFICIAIS & CONTEXTO CLÍNICO)", leftMargin, y, sectionHeaderPaint)
            y += 14f
            canvas.drawText("DATA        HORA   MEMBRO      PERÍODO          VALOR      STATUS & CONTEXTO / SINTOMAS", leftMargin, y, bodyBoldPaint)
            y += 11f

            if (filteredGlucose.isEmpty()) {
                canvas.drawText("Nenhuma medição de glicemia registrada no período selecionado.", leftMargin, y, subtitlePaint)
                y += 15f
            } else {
                filteredGlucose.take(14).forEach { rec ->
                    val memberTag = rec.memberName.ifBlank { profileName }.take(10).padEnd(10, ' ')
                    val normalizedSlot = DiabetesSlotCatalog.normalizeSlot(rec.slot)
                    val slotFixed = normalizedSlot.take(15).padEnd(15, ' ')
                    val valFixed = "${rec.valueMgDl} mg/dL".padEnd(10, ' ')
                    val noteClean = if (rec.notes.isNotBlank()) " • ${rec.notes.take(32)}" else ""
                    val line = "${rec.dateIso}  ${rec.timeHHmm}  $memberTag  $slotFixed  $valFixed ${rec.statusLabel()}$noteClean"
                    canvas.drawText(line, leftMargin, y, bodyPaint)
                    y += 12f
                }
            }

            y += 8f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 16f
        }

        // 3. Aplicações de Insulina & Rodízio de Local
        if (customizationConfig.diabetesEnabled || filteredInsulin.isNotEmpty()) {
            canvas.drawText("3. CONTROLE DE INSULINA (UI) & RODÍZIO DE LOCAL DE APLICAÇÃO", leftMargin, y, sectionHeaderPaint)
            y += 14f
            canvas.drawText("DATA        HORA   MEMBRO      TIPO DE INSULINA        DOSE     LOCAL DE APLICAÇÃO & OBS.", leftMargin, y, bodyBoldPaint)
            y += 11f

            if (filteredInsulin.isEmpty()) {
                canvas.drawText("Nenhuma aplicação de insulina registrada no período selecionado.", leftMargin, y, subtitlePaint)
                y += 15f
            } else {
                filteredInsulin.take(10).forEach { ins ->
                    val memberTag = ins.memberName.ifBlank { profileName }.take(10).padEnd(10, ' ')
                    val typeFixed = ins.insulinType.take(22).padEnd(22, ' ')
                    val doseFixed = "${ins.doseUnits} UI".padEnd(8, ' ')
                    val noteClean = if (ins.notes.isNotBlank()) " • ${ins.notes.take(24)}" else ""
                    val line = "${ins.dateIso}  ${ins.timeHHmm}  $memberTag  $typeFixed  $doseFixed ${ins.site}$noteClean"
                    canvas.drawText(line, leftMargin, y, bodyPaint)
                    y += 12f
                }
            }

            y += 8f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 16f
        }

        // 4. Histórico de Pressão Arterial
        if (customizationConfig.bloodPressureEnabled || filteredBp.isNotEmpty()) {
            canvas.drawText("4. HISTÓRICO DE PRESSÃO ARTERIAL (mmHg) & PULSO (bpm)", leftMargin, y, sectionHeaderPaint)
            y += 14f
            canvas.drawText("DATA        HORA   MEMBRO      PAS/PAD (mmHg)  PULSO    CLASSIFICAÇÃO & OBS.", leftMargin, y, bodyBoldPaint)
            y += 11f

            if (filteredBp.isEmpty()) {
                canvas.drawText("Nenhuma aferição de pressão arterial registrada no período selecionado.", leftMargin, y, subtitlePaint)
                y += 15f
            } else {
                filteredBp.take(8).forEach { bp ->
                    val memberTag = bp.memberName.ifBlank { profileName }.take(10).padEnd(10, ' ')
                    val bpFixed = "${bp.systolic}/${bp.diastolic} mmHg".padEnd(14, ' ')
                    val pulseFixed = "${bp.pulseBpm} bpm".padEnd(8, ' ')
                    val noteClean = if (bp.notes.isNotBlank()) " • ${bp.notes.take(24)}" else ""
                    val line = "${bp.dateIso}  ${bp.timeHHmm}  $memberTag  $bpFixed  $pulseFixed ${bp.statusLabel()}$noteClean"
                    canvas.drawText(line, leftMargin, y, bodyPaint)
                    y += 12f
                }
            }

            y += 8f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 16f
        }

        // 5. Inegociáveis Diários & Acompanhamento Gestacional
        if (y < pageHeight - 90f) {
            val sectionTitle = if (customizationConfig.pregnancyEnabled || customizationConfig.pregnancyDppDate.isNotBlank()) {
                "5. INEGOCIÁVEIS DIÁRIOS & ACOMPANHAMENTO GESTACIONAL (DPP)"
            } else {
                "5. INEGOCIÁVEIS DIÁRIOS (TRACKERS)"
            }
            canvas.drawText(sectionTitle, leftMargin, y, sectionHeaderPaint)
            y += 14f

            healthState.trackers.take(4).forEach { habit ->
                val currentVal = healthState.progressForTracker(todayIso, habit.id)
                canvas.drawText(
                    "• ${habit.title}: $currentVal / ${habit.targetValue} ${habit.unit} (${if (currentVal >= habit.targetValue) "Meta Atingida" else "Em andamento"})",
                    leftMargin + 8f,
                    y,
                    bodyPaint
                )
                y += 12f
            }

            if (customizationConfig.pregnancyEnabled || customizationConfig.pregnancyDppDate.isNotBlank()) {
                y += 4f
                val pregProgress = PregnancyCalculator.calculateFromDpp(customizationConfig.pregnancyDppDate)
                canvas.drawText(
                    "Gestação: ${pregProgress.gestationalAgeLabel()} • ${pregProgress.trimester}º Trimestre • DPP: ${pregProgress.dppFormatted} • Bebê: ${pregProgress.formattedLengthAndWeight()}",
                    leftMargin + 8f,
                    y,
                    bodyBoldPaint
                )
            }
        }

        // Rodapé
        canvas.drawText(
            "Documento gerado nativamente em PDF pelo aplicativo Omni (com.auramanager.app) para acompanhamento médico.",
            leftMargin,
            pageHeight - 24f,
            subtitlePaint
        )

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        val summaryParts = buildList {
            if (customizationConfig.diabetesEnabled || filteredGlucose.isNotEmpty()) {
                add("${filteredGlucose.size} glicemia(s)${if (avgGlucose > 0) " (média ${avgGlucose} mg/dL)" else ""}")
            }
            if (filteredInsulin.isNotEmpty()) {
                add("${filteredInsulin.size} insulina(s)")
            }
            if (customizationConfig.bloodPressureEnabled || filteredBp.isNotEmpty()) {
                add("${filteredBp.size} pressão(ões)${if (avgSys > 0) " (média ${avgSys}/${avgDia} mmHg)" else ""}")
            }
            add("$completedTrackersToday/${healthState.trackers.size} inegociáveis")
            if (customizationConfig.pregnancyEnabled || customizationConfig.pregnancyDppDate.isNotBlank()) {
                add("Gestação ativa")
            }
        }
        return "Relatório consolidado ($periodLabel): ${summaryParts.joinToString(", ")}."
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
            val fileName = "Relatorio_Medico_Omni_${DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())}.pdf"
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
            // Also keep a clean local copy in filesDir so "Abrir / Compartilhar PDF" works via FileProvider
            val localCopy = exportToLocalAppFile(
                context = context,
                profileName = profileName,
                periodDays = periodDays,
                customizationConfig = customizationConfig,
                healthState = healthState
            )
            PdfExportResult(
                success = true,
                filePath = localCopy.filePath,
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

    /**
     * Abre ou compartilha o relatório PDF gerado usando FileProvider + Intent.createChooser,
     * nunca expondo caminhos internos ou URIs content:// na interface.
     */
    fun openOrSharePdf(
        context: Context,
        pdfFilePath: String,
        profileName: String = "Família",
        onError: (String) -> Unit = {}
    ) {
        val customConfig = runCatching { CustomizationRepository(context).configFlow.value }.getOrDefault(AppCustomizationConfig())
        val healthState = runCatching { HealthRepository(context).stateFlow.value }.getOrDefault(HealthModuleState())
        openOrSharePdf(
            context = context,
            localFilePath = pdfFilePath,
            profileName = profileName,
            periodDays = 30,
            customizationConfig = customConfig,
            healthState = healthState,
            onError = onError
        )
    }

    fun openOrSharePdf(
        context: Context,
        localFilePath: String,
        profileName: String,
        periodDays: Int,
        customizationConfig: AppCustomizationConfig,
        healthState: HealthModuleState,
        onError: (String) -> Unit = {}
    ) {
        try {
            var targetFile = if (localFilePath.isNotBlank() && !localFilePath.startsWith("content://")) {
                File(localFilePath)
            } else null

            if (targetFile == null || !targetFile.exists()) {
                val generated = exportToLocalAppFile(
                    context = context,
                    profileName = profileName,
                    periodDays = periodDays,
                    customizationConfig = customizationConfig,
                    healthState = healthState
                )
                if (!generated.success || generated.filePath.isBlank()) {
                    onError(generated.summaryText)
                    return
                }
                targetFile = File(generated.filePath)
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Médico Omni - $profileName")
                putExtra(Intent.EXTRA_TEXT, "Segue em anexo o Relatório Médico Consolidado gerado pelo aplicativo Omni.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Abrir ou Compartilhar Relatório Médico PDF").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            onError("Não foi possível abrir o compartilhamento de PDF: ${e.localizedMessage ?: "verifique se há um leitor de PDF instalado"}")
        }
    }
}
