package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AlarmScheduler
import com.example.alarm.TaskTonePreviewPlayer
import com.example.data.AppCustomizationConfig
import com.example.data.BloodPressureRecord
import com.example.data.CustomizationCatalog
import com.example.data.CustomizationRepository
import com.example.data.DailyTrackerHabit
import com.example.data.DateCalendarUtils
import com.example.data.DiabetesSlotCatalog
import com.example.data.FamilyMemberBadge
import com.example.data.GlucoseContextCatalog
import com.example.data.GlucoseRecord
import com.example.data.HealthRepository
import com.example.data.Household
import com.example.data.InsulinRecord
import com.example.data.InsulinScheduleConfig
import com.example.data.InsulinSiteCatalog
import com.example.data.InsulinTypeCatalog
import com.example.data.MedicalReportPdfGenerator
import com.example.data.PregnancyCalculator
import com.example.data.PrivacyScope
import com.example.data.SupplyReplenishCatalog
import com.example.data.UserProfile
import com.example.data.formatCurrencyBrl
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthModuleScreen(
    selectedEpochDay: Long,
    customizationConfig: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    healthRepo: HealthRepository,
    onCreateHealthFinanceExpense: (title: String, amountBrl: Double, category: String, privacyScope: String) -> Unit,
    onShowBannerMessage: (String) -> Unit,
    household: Household = Household(),
    userProfile: UserProfile? = null,
    pendingGlucoseSlotFromNotification: String? = null,
    onConsumePendingGlucoseSlot: () -> Unit = {},
    onAddSupplyToMural: (String) -> Unit = {},
    onSyncPregnancyConfigToCloud: (dppDate: String, comparisonMode: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val healthState by healthRepo.stateFlow.collectAsStateWithLifecycle()
    val triggerFeedback = rememberOmniFeedback(onShowBannerMessage)

    // Navegação de data dedicada ao Monitoramento de Glicemia & Saúde (< Hoje >)
    var activeDateEpochDay by rememberSaveable(selectedEpochDay) {
        mutableLongStateOf(selectedEpochDay)
    }
    val todayEpochDay = remember { DateCalendarUtils.todayEpochDay() }
    val selectedDateIso = remember(activeDateEpochDay) {
        DateCalendarUtils.formatIsoDate(activeDateEpochDay)
    }

    // Lista oficial de membros da família (ex: Marcus e Nathalia) sincronizada com o Firestore
    val familyMembers = remember(
        household.members,
        customizationConfig.profileName,
        customizationConfig.profileColorHex,
        userProfile?.userId,
        userProfile?.displayName
    ) {
        val resolvedFallbackName = userProfile?.displayName?.takeIf {
            it.isNotBlank() && !it.equals("Você", true) && !it.equals("VO", true)
        } ?: customizationConfig.profileName.takeIf {
            it.isNotBlank() && !it.equals("Você", true) && !it.equals("VO", true)
        } ?: "Marcus"
        household.membersWithAssignedColors(
            fallbackProfileName = resolvedFallbackName,
            fallbackProfileColorHex = customizationConfig.profileColorHex,
            fallbackUserId = userProfile?.userId.orEmpty()
        )
    }

    val defaultMember = remember(familyMembers, userProfile?.userId, customizationConfig.profileName) {
        familyMembers.firstOrNull {
            (userProfile?.userId?.isNotBlank() == true && it.userId == userProfile.userId) ||
                it.isCurrentUser ||
                it.displayName.equals(customizationConfig.profileName, ignoreCase = true)
        } ?: familyMembers.firstOrNull()
            ?: FamilyMemberBadge(
                userId = userProfile?.userId.orEmpty().ifBlank { "member_primary" },
                displayName = customizationConfig.profileName.takeIf {
                    it.isNotBlank() && !it.equals("Você", true) && !it.equals("VO", true)
                } ?: "Marcus",
                colorHex = customizationConfig.profileColorHex
            )
    }

    var selectedMemberId by rememberSaveable(defaultMember.userId) { mutableStateOf(defaultMember.userId) }
    val activeMember = remember(familyMembers, selectedMemberId, defaultMember) {
        familyMembers.firstOrNull {
            it.userId == selectedMemberId || it.displayName.equals(selectedMemberId, ignoreCase = true)
        } ?: defaultMember
    }
    val isPrimaryDefaultMember = remember(activeMember, defaultMember) {
        activeMember.userId == defaultMember.userId ||
            activeMember.displayName.equals(defaultMember.displayName, ignoreCase = true)
    }

    // Migração automática em segundo plano de registros antigos sem memberId válido ou com "Você" / "VO"
    LaunchedEffect(defaultMember.userId, defaultMember.displayName, healthState.glucoseRecords.size, healthState.insulinRecords.size) {
        healthRepo.migratePlaceholderMembersToLoggedMember(
            loggedMemberId = defaultMember.userId,
            loggedMemberName = defaultMember.displayName
        )
    }

    // Filtro de Escopo nos Trackers ("ALL", PrivacyScope.PERSONAL, PrivacyScope.FAMILY)
    var scopeFilter by rememberSaveable { mutableStateOf("ALL") }
    // Período selecionado para o Relatório Médico PDF (7, 30 ou 365 dias)
    var pdfPeriodDays by rememberSaveable { mutableIntStateOf(30) }

    // Modais de criação/edição e exclusão com confirmação
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var trackerToEdit by remember { mutableStateOf<DailyTrackerHabit?>(null) }
    var trackerToDelete by remember { mutableStateOf<DailyTrackerHabit?>(null) }

    var showAddGlucoseDialog by remember { mutableStateOf(false) }
    var glucoseToEdit by remember { mutableStateOf<GlucoseRecord?>(null) }
    var glucoseToDelete by remember { mutableStateOf<GlucoseRecord?>(null) }
    var presetGlucoseSlot by remember { mutableStateOf(DiabetesSlotCatalog.JEJUM) }

    var showAddInsulinDialog by remember { mutableStateOf(false) }
    var insulinToEdit by remember { mutableStateOf<InsulinRecord?>(null) }
    var insulinToDelete by remember { mutableStateOf<InsulinRecord?>(null) }
    var showInsulinSchedulesDialog by remember { mutableStateOf(false) }

    var showUnifiedHistoryModal by remember { mutableStateOf(false) }

    var showAddBpDialog by remember { mutableStateOf(false) }
    var bpToEdit by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var bpToDelete by remember { mutableStateOf<BloodPressureRecord?>(null) }

    var showEditDppDialog by remember { mutableStateOf(false) }

    // Quando a notificação do cronômetro de 1h pós-refeição dispara e o usuário toca nela,
    // abre diretamente o modal de registro de glicemia naquele slot ("1h Pós-Café", "1h Pós-Almoço" ou "1h Pós-Janta").
    LaunchedEffect(pendingGlucoseSlotFromNotification) {
        val slotFromNotif = pendingGlucoseSlotFromNotification
        if (!slotFromNotif.isNullOrBlank()) {
            presetGlucoseSlot = DiabetesSlotCatalog.normalizeSlot(slotFromNotif)
            glucoseToEdit = null
            showAddGlucoseDialog = true
            onConsumePendingGlucoseSlot()
        }
    }

    // Cronômetro Pós-Refeição fixo em 1h (60:00) integrado ao card de Glicemia
    val timerState = healthState.postMealTimer
    var selectedMealForTimer by remember(timerState.mealLabel) {
        mutableStateOf(
            DiabetesSlotCatalog.normalizeSlot(timerState.mealLabel)
                .takeIf { it in DiabetesSlotCatalog.postMealTimerSlots }
                ?: DiabetesSlotCatalog.POS_ALMOCO_1H
        )
    }
    var liveRemainingSeconds by remember(timerState) {
        mutableIntStateOf(
            if (timerState.isRunning && !timerState.isPaused) {
                ((timerState.targetEndEpochMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            } else {
                timerState.pausedRemainingSeconds.coerceIn(0, 3600)
            }
        )
    }

    LaunchedEffect(timerState.isRunning, timerState.isPaused, timerState.targetEndEpochMillis) {
        if (timerState.isRunning && !timerState.isPaused) {
            while (true) {
                val rem = ((timerState.targetEndEpochMillis - System.currentTimeMillis()) / 1000L).toInt()
                if (rem <= 0) {
                    liveRemainingSeconds = 0
                    healthRepo.markPostMealTimerFinished()
                    TaskTonePreviewPlayer.playPreview("SUAVE")
                    val targetSlot = DiabetesSlotCatalog.slotForPostMealTimer(timerState.mealLabel, 3600)
                    AlarmScheduler.showImmediateNotification(
                        context = context,
                        notificationId = 7701,
                        title = "⏰ 1h Concluída ($targetSlot)!",
                        message = "Toque para registrar sua medição de glicemia ($targetSlot) agora.",
                        openGlucoseSlot = targetSlot
                    )
                    break
                } else {
                    liveRemainingSeconds = rem
                }
                delay(1000L)
            }
        } else {
            liveRemainingSeconds = timerState.pausedRemainingSeconds.coerceIn(0, 3600)
        }
    }

    val filteredTrackers = remember(healthState.trackers, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> healthState.trackers.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> healthState.trackers.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> healthState.trackers
        }
    }

    fun matchesActiveMember(recordMemberId: String, recordMemberName: String): Boolean {
        val cleanId = recordMemberId.trim()
        val cleanName = recordMemberName.trim()
        if ((cleanId.isBlank() || cleanId.equals("Você", true) || cleanId.equals("VO", true)) &&
            (cleanName.isBlank() || cleanName.equals("Você", true) || cleanName.equals("VO", true))
        ) {
            return isPrimaryDefaultMember
        }
        return cleanId.equals(activeMember.userId, ignoreCase = true) ||
            cleanId.equals(activeMember.displayName, ignoreCase = true) ||
            cleanName.equals(activeMember.displayName, ignoreCase = true) ||
            cleanName.equals(activeMember.userId, ignoreCase = true)
    }

    // Filtra medições clínicas individualmente pelo membro selecionado (ex: Marcus | Nathalia)
    val filteredGlucose = remember(healthState.glucoseRecords, activeMember, isPrimaryDefaultMember) {
        healthState.glucoseRecords.filter { rec ->
            matchesActiveMember(rec.memberId, rec.memberName)
        }
    }

    val filteredInsulin = remember(healthState.insulinRecords, activeMember, isPrimaryDefaultMember) {
        healthState.insulinRecords.filter { rec ->
            matchesActiveMember(rec.memberId, rec.memberId)
        }
    }

    val filteredBp = remember(healthState.bloodPressureRecords, activeMember, isPrimaryDefaultMember) {
        healthState.bloodPressureRecords.filter { rec ->
            matchesActiveMember(rec.memberId, rec.memberName)
        }
    }

    val memberFilteredHealthState = remember(healthState, filteredGlucose, filteredBp, filteredInsulin) {
        healthState.copy(
            glucoseRecords = filteredGlucose,
            bloodPressureRecords = filteredBp,
            insulinRecords = filteredInsulin
        )
    }

    // Launcher para salvar PDF nativo em pasta escolhida pelo usuário (CreateDocument)
    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null) {
            val result = MedicalReportPdfGenerator.exportToUserUri(
                context = context,
                targetUri = targetUri,
                profileName = activeMember.displayName,
                periodDays = pdfPeriodDays,
                customizationConfig = customizationConfig,
                healthState = memberFilteredHealthState
            )
            if (result.success) {
                val localCopy = MedicalReportPdfGenerator.exportToLocalAppFile(
                    context = context,
                    profileName = activeMember.displayName,
                    periodDays = pdfPeriodDays,
                    customizationConfig = customizationConfig,
                    healthState = memberFilteredHealthState
                )
                val safePath = if (localCopy.success) localCopy.filePath else ""
                healthRepo.recordGeneratedPdfInfo(safePath, result.summaryText)
                onShowBannerMessage("📄 Relatório Médico de ${activeMember.displayName} salvo com sucesso!")
            } else {
                onShowBannerMessage(result.summaryText)
            }
        }
    }

    val effectiveDppDate = household.pregnancyDppDate.ifBlank { customizationConfig.pregnancyDppDate }
    val showPregnancyCard = customizationConfig.pregnancyEnabled || effectiveDppDate.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("health_module_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CARD DE GESTAÇÃO (DPP) COM COMPARADOR (DOCE / FRUTA / ANIMAL) E PRÓXIMO MARCO (no topo da aba Saúde)
        if (showPregnancyCard) {
            item {
                var newPregNote by remember { mutableStateOf("") }
                var showPrenatalChecklist by rememberSaveable { mutableStateOf(false) }

                OmniPregnancyHeroCard(
                    dppDateRaw = effectiveDppDate,
                    comparisonMode = customizationConfig.pregnancyComparisonMode.ifBlank {
                        household.pregnancyComparisonMode.ifBlank { "TODOS" }
                    },
                    accentColor = Color(customizationConfig.healthColorHex),
                    onSelectComparisonMode = { mode ->
                        customizationRepo.updatePregnancyComparisonMode(mode)
                        onSyncPregnancyConfigToCloud(effectiveDppDate, mode)
                    },
                    onOpenEditDpp = { showEditDppDialog = true },
                    extraFooterContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showPrenatalChecklist = !showPrenatalChecklist },
                                    modifier = Modifier.testTag("toggle_prenatal_checklist_btn")
                                ) {
                                    Text(
                                        text = if (showPrenatalChecklist) "Ocultar Checklist Pré-Natal ▲" else "Checklist Pré-Natal & Evolução ▼",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            AnimatedVisibility(visible = showPrenatalChecklist) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    healthState.pregnancyChecklist.forEach { item ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (item.completed) {
                                                IncomeEmerald.copy(alpha = 0.10f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { healthRepo.togglePregnancyChecklistItem(item.id) }
                                                .testTag("pregnancy_check_item_${item.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = item.completed,
                                                    onCheckedChange = { healthRepo.togglePregnancyChecklistItem(item.id) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${item.title} (Sem. ${item.recommendedWeek})",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                    Text(
                                                        text = item.notes,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (item.estimatedCostBrl > 0.0) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            onCreateHealthFinanceExpense(
                                                                item.title,
                                                                item.estimatedCostBrl,
                                                                item.suggestedFinanceCategory,
                                                                PrivacyScope.FAMILY
                                                            )
                                                            onShowBannerMessage("💰 Lançado no Financeiro (${item.suggestedFinanceCategory}): ${item.title} - ${formatCurrencyBrl(item.estimatedCostBrl)}")
                                                        },
                                                        shape = RoundedCornerShape(50),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Lançar ${formatCurrencyBrl(item.estimatedCostBrl)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newPregNote,
                                            onValueChange = { newPregNote = it },
                                            label = { Text("Registrar sintoma, ultrassom ou marco") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                if (newPregNote.isNotBlank()) {
                                                    healthRepo.addPregnancyNote(newPregNote)
                                                    newPregNote = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text("Salvar")
                                        }
                                    }

                                    healthState.pregnancyNotesLog.take(3).forEach { note ->
                                        Text(
                                            text = "• $note",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        // 2. CARD ÚNICO CONSOLIDADO: MONITORAMENTO DE GLICEMIA (mg/dL), INSULINA, RODÍZIO, INSUMOS E TIMER 1H
        item {
            val recordsOnSelectedDate = remember(filteredGlucose, activeDateEpochDay, selectedDateIso) {
                filteredGlucose.filter { rec ->
                    rec.dateEpochDay == activeDateEpochDay || rec.dateIso == selectedDateIso
                }
            }
            val latestBySlotOnDate = remember(recordsOnSelectedDate) {
                val map = mutableMapOf<String, GlucoseRecord>()
                for (slot in DiabetesSlotCatalog.allSlots) {
                    val match = recordsOnSelectedDate
                        .filter { DiabetesSlotCatalog.normalizeSlot(it.slot) == slot }
                        .maxByOrNull { it.timestampEpochMillis }
                    if (match != null) {
                        map[slot] = match
                    }
                }
                map
            }
            val measuredCountOnDate = latestBySlotOnDate.size
            val dayValues = remember(recordsOnSelectedDate) {
                recordsOnSelectedDate.map { it.valueMgDl }.filter { it > 0 }
            }
            val dayMin = remember(dayValues) { dayValues.minOrNull() ?: 0 }
            val dayMax = remember(dayValues) { dayValues.maxOrNull() ?: 0 }
            val dayAvg = remember(dayValues) {
                if (dayValues.isNotEmpty()) dayValues.average().roundToInt() else 0
            }

            val latestInsulinBanner = remember(healthState.insulinRecords, activeMember, selectedDateIso) {
                healthState.formatLatestInsulinBanner(
                    memberId = activeMember.userId,
                    memberName = activeMember.displayName,
                    todayIso = DateCalendarUtils.todayIsoDate()
                )
            }

            val isTodaySelected = activeDateEpochDay == todayEpochDay
            val dateDisplayLabel = remember(activeDateEpochDay, isTodaySelected) {
                val formatted = DateCalendarUtils.formatEpochDayPt(activeDateEpochDay)
                if (isTodaySelected) "Hoje • $formatted" else formatted
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clinical_measurements_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Título + Botão rápido de PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🩸 Monitoramento de Glicemia (mg/dL)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "6 períodos oficiais • Insulina & Rodízio • Cronômetro 1h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showUnifiedHistoryModal = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("top_open_glucose_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Histórico", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Seletor rápido de membro ("Marcus" | "Nathalia")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Membro da Família:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            familyMembers.forEach { member ->
                                val isSelected = activeMember.userId == member.userId ||
                                    activeMember.displayName.equals(member.displayName, ignoreCase = true)
                                val memberColor = Color(member.colorHex.toInt())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMemberId = member.userId },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(memberColor)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${member.avatarEmoji} ${member.displayName}",
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("health_member_chip_${member.userId}")
                                )
                            }
                        }
                    }

                    // Navegação de data (< Hoje >) + Contador "X de 6 períodos medidos na data selecionada"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { activeDateEpochDay -= 1L },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .testTag("glucose_prev_date_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Dia anterior"
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeDateEpochDay = todayEpochDay }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = dateDisplayLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$measuredCountOnDate de 6 períodos medidos na data selecionada",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { activeDateEpochDay += 1L },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .testTag("glucose_next_date_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Próximo dia"
                                    )
                                }
                            }

                            // 6 Chips dos Períodos Oficiais com o valor medido diretamente dentro do chip
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiabetesSlotCatalog.allSlots.forEach { slotName ->
                                    val recForSlot = latestBySlotOnDate[slotName]
                                    val isMeasured = recForSlot != null
                                    val isAlert = recForSlot?.isAlert() == true
                                    val chipAccent = when {
                                        !isMeasured -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        isAlert -> ExpenseCoral
                                        else -> IncomeEmerald
                                    }
                                    val chipBg = when {
                                        !isMeasured -> MaterialTheme.colorScheme.surface
                                        isAlert -> ExpenseCoral.copy(alpha = 0.16f)
                                        else -> IncomeEmerald.copy(alpha = 0.18f)
                                    }
                                    val chipLabel = if (recForSlot != null) {
                                        "$slotName • ${recForSlot.valueMgDl} mg/dL"
                                    } else {
                                        slotName
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = chipBg,
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = chipAccent,
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                presetGlucoseSlot = slotName
                                                glucoseToEdit = recForSlot
                                                showAddGlucoseDialog = true
                                            }
                                            .testTag("glucose_slot_chip_$slotName")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (isMeasured) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Medido",
                                                    tint = chipAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = chipLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isMeasured) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isMeasured) chipAccent else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Resumo rápido do dia (menor, maior e média do dia)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (dayValues.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Menor do dia",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$dayMin mg/dL",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Média do dia",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$dayAvg mg/dL",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Maior do dia",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$dayMax mg/dL",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (dayMax > 140) ExpenseCoral else IncomeEmerald
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Nenhuma medição registrada para ${activeMember.displayName} nesta data. Toque em um dos 6 períodos acima ou em '+ Medir'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Botões de ação rápida: "+ Medir", "+ Insulina" e "Histórico"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val nextMissingSlot = DiabetesSlotCatalog.allSlots.firstOrNull { it !in latestBySlotOnDate }
                                    ?: DiabetesSlotCatalog.JEJUM
                                presetGlucoseSlot = nextMissingSlot
                                glucoseToEdit = null
                                showAddGlucoseDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_add_glucose_btn")
                        ) {
                            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Medir", fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = {
                                insulinToEdit = null
                                showAddInsulinDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_add_insulin_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Insulina", fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = { showUnifiedHistoryModal = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_unified_history_btn")
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Histórico", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    // Resumo de Insulina + Configuração de Horários Fixos + Repor Insumos em 1 toque
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insulin_summary_banner")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💉 $latestInsulinBanner",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { showInsulinSchedulesDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("open_insulin_schedules_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Doses & Alarmes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Atalho "Repor insumos" -> envia em 1 toque para o checklist de Compras do Mural da Família
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Repor insumos (envia em 1 toque para Compras no Mural):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SupplyReplenishCatalog.items.forEach { supplyItem ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(50)
                                                )
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    onAddSupplyToMural(supplyItem)
                                                    triggerFeedback("🛒 \"$supplyItem\" enviado para a lista de Compras do Mural da Família!")
                                                }
                                                .testTag("replenish_supply_chip_$supplyItem")
                                        ) {
                                            Text(
                                                text = "+ $supplyItem",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Registros do dia selecionado (Glicemia + Insulina) com edição/exclusão rápidas
                    if (recordsOnSelectedDate.isNotEmpty() || filteredInsulin.any { it.dateIso == selectedDateIso }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Registros de ${activeMember.displayName} em ${DateCalendarUtils.formatEpochDayPt(activeDateEpochDay)}:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            recordsOnSelectedDate.take(6).forEach { record ->
                                GlucoseRecordRowCard(
                                    record = record,
                                    fallbackMemberName = activeMember.displayName,
                                    onEdit = { glucoseToEdit = record },
                                    onDelete = { glucoseToDelete = record }
                                )
                            }
                            filteredInsulin.filter { it.dateIso == selectedDateIso }.take(4).forEach { insRec ->
                                InsulinRecordRowCard(
                                    record = insRec,
                                    onEdit = { insulinToEdit = insRec },
                                    onDelete = { insulinToDelete = insRec }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // Na base do mesmo card: Cronômetro Pós-Refeição fixo em 1h (60:00) integrado ao AlarmManager
                    IntegratedOneHourPostMealTimerSection(
                        timerState = timerState,
                        liveRemainingSeconds = liveRemainingSeconds,
                        selectedMealSlot = selectedMealForTimer,
                        onSelectMealSlot = { slot ->
                            selectedMealForTimer = slot
                            if (!timerState.isRunning) {
                                healthRepo.resetPostMealTimer(slot)
                            }
                        },
                        onStartTimer = {
                            healthRepo.startPostMealTimer(selectedMealForTimer, 3600)
                            val triggerAt = System.currentTimeMillis() + 3600_000L
                            AlarmScheduler.schedulePostMealGlucoseAlarm(
                                context = context,
                                triggerAtMillis = triggerAt,
                                mealLabel = selectedMealForTimer
                            )
                            triggerFeedback("⏱️ Cronômetro de 1h ($selectedMealForTimer) iniciado com alarme exato!")
                        },
                        onPauseResumeTimer = {
                            healthRepo.pauseOrResumePostMealTimer()
                            val updated = healthRepo.stateFlow.value.postMealTimer
                            if (updated.isPaused) {
                                AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                            } else if (updated.isRunning && updated.targetEndEpochMillis > System.currentTimeMillis()) {
                                AlarmScheduler.schedulePostMealGlucoseAlarm(
                                    context = context,
                                    triggerAtMillis = updated.targetEndEpochMillis,
                                    mealLabel = updated.mealLabel
                                )
                            }
                        },
                        onResetTimer = {
                            healthRepo.resetPostMealTimer(selectedMealForTimer)
                            AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                        },
                        onLogPostMealGlucoseNow = {
                            healthRepo.dismissPostMealTimerAlert()
                            AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                            presetGlucoseSlot = DiabetesSlotCatalog.normalizeSlot(selectedMealForTimer)
                            glucoseToEdit = null
                            showAddGlucoseDialog = true
                        }
                    )
                }
            }
        }

        // 3. MONITORAMENTO DE PRESSÃO ARTERIAL (mmHg) — exibido quando ativo ou se houver registros
        if (customizationConfig.bloodPressureEnabled || filteredBp.isNotEmpty()) {
            item {
                val avgSys = remember(filteredBp) {
                    if (filteredBp.isNotEmpty()) filteredBp.map { it.systolic }.average().roundToInt() else 0
                }
                val avgDia = remember(filteredBp) {
                    if (filteredBp.isNotEmpty()) filteredBp.map { it.diastolic }.average().roundToInt() else 0
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bp_measurements_card"),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "❤️ Pressão Arterial (mmHg) • ${activeMember.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (avgSys > 0) "Média: $avgSys/$avgDia mmHg • Lembrete: ${customizationConfig.bloodPressureReminderTime}"
                                    else "Lembrete diário: ${customizationConfig.bloodPressureReminderTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    bpToEdit = null
                                    showAddBpDialog = true
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("open_add_bp_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pressão", fontWeight = FontWeight.Bold)
                            }
                        }

                        filteredBp.take(5).forEach { record ->
                            BloodPressureRecordRowCard(
                                record = record,
                                fallbackMemberName = activeMember.displayName,
                                onEdit = { bpToEdit = record },
                                onDelete = { bpToDelete = record }
                            )
                        }
                    }
                }
            }
        }

        // 4. INEGOCIÁVEIS PERSONALIZÁVEIS (TRACKERS DIÁRIOS: ÁGUA, VITAMINAS, ETC.)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_trackers_section_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💧 Inegociáveis do Dia (Trackers Diários)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val doneCount = healthState.completedTrackersCount(selectedDateIso, scopeFilter)
                            Text(
                                text = "Data: ${DateCalendarUtils.formatEpochDayPt(activeDateEpochDay)} • $doneCount/${filteredTrackers.size} metas batidas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { showAddTrackerDialog = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_custom_tracker_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo Tracker", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scopeFilter == "ALL",
                            onClick = { scopeFilter = "ALL" },
                            label = { Text("Tudo", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("health_scope_all")
                        )
                        FilterChip(
                            selected = scopeFilter == PrivacyScope.PERSONAL,
                            onClick = { scopeFilter = PrivacyScope.PERSONAL },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("Individual") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("health_scope_personal")
                        )
                        FilterChip(
                            selected = scopeFilter == PrivacyScope.FAMILY,
                            onClick = { scopeFilter = PrivacyScope.FAMILY },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("Familiar") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("health_scope_family")
                        )
                    }

                    filteredTrackers.forEach { habit ->
                        val currentVal = healthState.progressForTracker(selectedDateIso, habit.id)
                        DailyTrackerItemCard(
                            habit = habit,
                            currentValue = currentVal,
                            onDecrement = {
                                healthRepo.incrementTrackerProgress(selectedDateIso, habit.id, -habit.stepValue)
                            },
                            onIncrement = {
                                healthRepo.incrementTrackerProgress(selectedDateIso, habit.id, habit.stepValue)
                            },
                            onCompleteAll = {
                                healthRepo.completeTrackerForDay(selectedDateIso, habit.id)
                                triggerFeedback("Meta de ${habit.title} concluída hoje!")
                            },
                            onEdit = { trackerToEdit = habit },
                            onDelete = { trackerToDelete = habit }
                        )
                    }
                }
            }
        }

        // 5. RELATÓRIO MÉDICO CONSOLIDADO EM PDF (CONFIRMAÇÃO LIMPA + BOTÃO "ABRIR / COMPARTILHAR PDF")
        item {
            AuraExpandableSectionCard(
                title = "📄 Relatório Médico Consolidado em PDF",
                subtitle = "Glicemia (6 períodos + contexto), Insulina (doses e locais), Pressão e Gestação",
                icon = Icons.Default.PictureAsPdf,
                accentColor = ExpenseCoral,
                badgeText = "PDF Limpo",
                initiallyExpanded = true,
                testTag = "medical_pdf_report_card"
            ) {
                Text(
                    text = "Paciente selecionado: ${activeMember.displayName} • Escolha o período:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        7 to "Últimos 7 dias",
                        30 to "Últimos 30 dias",
                        365 to "Todo o Histórico"
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = pdfPeriodDays == days,
                            onClick = { pdfPeriodDays = days },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("pdf_period_chip_$days")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val localResult = MedicalReportPdfGenerator.exportToLocalAppFile(
                                context = context,
                                profileName = activeMember.displayName,
                                periodDays = pdfPeriodDays,
                                customizationConfig = customizationConfig,
                                healthState = memberFilteredHealthState
                            )
                            if (localResult.success) {
                                healthRepo.recordGeneratedPdfInfo(localResult.filePath, localResult.summaryText)
                                MedicalReportPdfGenerator.openOrSharePdf(
                                    context = context,
                                    pdfFilePath = localResult.filePath,
                                    profileName = activeMember.displayName
                                )
                            } else {
                                onShowBannerMessage(localResult.summaryText)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("generate_medical_pdf_now_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar & Compartilhar PDF", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val fileName = "Relatorio_Medico_${activeMember.displayName.replace(" ", "_")}_${selectedDateIso}.pdf"
                            createPdfDocumentLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("export_medical_pdf_file_btn")
                    ) {
                        Text("Salvar no Aparelho", fontWeight = FontWeight.Bold)
                    }
                }

                if (healthState.lastGeneratedPdfSummary.isNotBlank()) {
                    val hasValidLocalFile = remember(healthState.lastGeneratedPdfPath) {
                        val path = healthState.lastGeneratedPdfPath
                        path.isNotBlank() && !path.startsWith("content://") && java.io.File(path).exists()
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = IncomeEmerald.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, IncomeEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓ Relatório Médico Consolidado Pronto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeEmerald
                            )
                            Text(
                                text = healthState.lastGeneratedPdfSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    val pathToShare = if (hasValidLocalFile) {
                                        healthState.lastGeneratedPdfPath
                                    } else {
                                        val regenerated = MedicalReportPdfGenerator.exportToLocalAppFile(
                                            context = context,
                                            profileName = activeMember.displayName,
                                            periodDays = pdfPeriodDays,
                                            customizationConfig = customizationConfig,
                                            healthState = memberFilteredHealthState
                                        )
                                        if (regenerated.success) {
                                            healthRepo.recordGeneratedPdfInfo(regenerated.filePath, regenerated.summaryText)
                                        }
                                        regenerated.filePath
                                    }
                                    if (pathToShare.isNotBlank()) {
                                        MedicalReportPdfGenerator.openOrSharePdf(
                                            context = context,
                                            pdfFilePath = pathToShare,
                                            profileName = activeMember.displayName
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_or_share_clean_pdf_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir / Compartilhar PDF", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAIS DE CRIAÇÃO, EDIÇÃO, INSULINA, HISTÓRICO E DPP
    // =========================================================================

    if (showAddTrackerDialog || trackerToEdit != null) {
        val editing = trackerToEdit
        AddCustomTrackerDialog(
            initialTracker = editing,
            defaultScope = if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL,
            onDismiss = {
                showAddTrackerDialog = false
                trackerToEdit = null
            },
            onSave = { title, emoji, unit, target, step, colorHex, scope ->
                if (editing != null) {
                    healthRepo.updateCustomTracker(editing.id, title, emoji, unit, target, step, colorHex, scope)
                    triggerFeedback("Tracker \"$title\" atualizado com sucesso!")
                } else {
                    healthRepo.addCustomTracker(title, emoji, unit, target, step, colorHex, scope)
                    triggerFeedback("Tracker \"$title\" criado com sucesso!")
                }
                showAddTrackerDialog = false
                trackerToEdit = null
            }
        )
    }

    trackerToDelete?.let { habit ->
        OmniConfirmDeleteDialog(
            title = "Excluir Tracker Diário?",
            message = "Deseja excluir o tracker \"${habit.title}\"? Esta ação não pode ser desfeita.",
            onConfirm = {
                healthRepo.deleteTracker(habit.id)
                triggerFeedback("Tracker \"${habit.title}\" excluído.")
                trackerToDelete = null
            },
            onDismiss = { trackerToDelete = null }
        )
    }

    if (showAddGlucoseDialog || glucoseToEdit != null) {
        val editing = glucoseToEdit
        AddGlucoseRecordDialog(
            initialRecord = editing,
            initialEpochDay = editing?.dateEpochDay ?: activeDateEpochDay,
            initialSlot = editing?.slot ?: presetGlucoseSlot,
            availableSlots = DiabetesSlotCatalog.allSlots,
            defaultScope = editing?.privacyScope ?: PrivacyScope.FAMILY,
            memberName = activeMember.displayName,
            onDismiss = {
                showAddGlucoseDialog = false
                glucoseToEdit = null
            },
            onSave = { epochDay, timeHHmm, slot, valMgDl, notes, scope, startOneHourAlarm ->
                if (editing != null) {
                    healthRepo.updateGlucoseRecord(
                        recordId = editing.id,
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        slot = slot,
                        valueMgDl = valMgDl,
                        notes = notes,
                        privacyScope = scope,
                        memberId = activeMember.userId,
                        memberName = activeMember.displayName
                    )
                } else {
                    healthRepo.addGlucoseRecord(
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        slot = slot,
                        valueMgDl = valMgDl,
                        notes = notes,
                        privacyScope = scope,
                        memberId = activeMember.userId,
                        memberName = activeMember.displayName
                    )
                }

                val postMealTargetSlot = DiabetesSlotCatalog.postMealSlotAfterPreMeal(slot)
                if (startOneHourAlarm && postMealTargetSlot != null) {
                    selectedMealForTimer = postMealTargetSlot
                    healthRepo.startPostMealTimer(postMealTargetSlot, 3600)
                    val triggerAt = System.currentTimeMillis() + 3600_000L
                    AlarmScheduler.schedulePostMealGlucoseAlarm(
                        context = context,
                        triggerAtMillis = triggerAt,
                        mealLabel = postMealTargetSlot
                    )
                    triggerFeedback("🩸 Glicemia ($valMgDl mg/dL • $slot) salva e alarme de 1h ($postMealTargetSlot) iniciado!")
                } else {
                    triggerFeedback("🩸 Glicemia de ${activeMember.displayName} salva: $valMgDl mg/dL ($slot)")
                }

                showAddGlucoseDialog = false
                glucoseToEdit = null
            }
        )
    }

    glucoseToDelete?.let { rec ->
        OmniConfirmDeleteDialog(
            title = "Excluir Registro de Glicemia?",
            message = "Deseja remover a medição de ${rec.valueMgDl} mg/dL (${rec.slot} em ${rec.dateIso})?",
            onConfirm = {
                healthRepo.deleteGlucoseRecord(rec.id)
                triggerFeedback("Medição de glicemia removida.")
                glucoseToDelete = null
            },
            onDismiss = { glucoseToDelete = null }
        )
    }

    if (showAddInsulinDialog || insulinToEdit != null) {
        val editing = insulinToEdit
        AddInsulinRecordDialog(
            initialRecord = editing,
            initialEpochDay = activeDateEpochDay,
            activeMember = activeMember,
            familyMembers = familyMembers,
            healthState = healthState,
            onOpenSchedulesConfig = {
                showAddInsulinDialog = false
                showInsulinSchedulesDialog = true
            },
            onDismiss = {
                showAddInsulinDialog = false
                insulinToEdit = null
            },
            onSave = { epochDay, timeHHmm, insulinType, units, injectionSite, memberBadge, notes ->
                if (editing != null) {
                    healthRepo.updateInsulinRecord(
                        recordId = editing.id,
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        insulinType = insulinType,
                        units = units,
                        injectionSite = injectionSite,
                        memberId = memberBadge.displayName,
                        notes = notes
                    )
                    triggerFeedback("💉 Insulina de ${memberBadge.displayName} atualizada: $insulinType ${units} UI ($injectionSite)")
                } else {
                    healthRepo.addInsulinRecord(
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        insulinType = insulinType,
                        units = units,
                        injectionSite = injectionSite,
                        memberId = memberBadge.displayName,
                        notes = notes
                    )
                    triggerFeedback("💉 Insulina registrada: $insulinType ${units} UI em $injectionSite (${memberBadge.displayName})")
                }
                showAddInsulinDialog = false
                insulinToEdit = null
            }
        )
    }

    insulinToDelete?.let { rec ->
        OmniConfirmDeleteDialog(
            title = "Excluir Aplicação de Insulina?",
            message = "Deseja remover o registro de ${rec.insulinType} (${rec.units} UI • ${rec.injectionSite})?",
            onConfirm = {
                healthRepo.deleteInsulinRecord(rec.id)
                triggerFeedback("Registro de insulina removido.")
                insulinToDelete = null
            },
            onDismiss = { insulinToDelete = null }
        )
    }

    if (showInsulinSchedulesDialog) {
        InsulinSchedulesConfigDialog(
            configs = healthState.insulinConfigs,
            activeMemberName = activeMember.displayName,
            onSaveConfig = { cfg ->
                healthRepo.saveInsulinScheduleConfig(
                    configId = cfg.id,
                    memberId = cfg.memberId.ifBlank { activeMember.displayName },
                    insulinType = cfg.insulinType,
                    defaultUnits = cfg.defaultUnits,
                    alarmHour = cfg.alarmHour,
                    alarmMinute = cfg.alarmMinute,
                    alarmEnabled = cfg.alarmEnabled
                )
                triggerFeedback("⏰ Horário de insulina (${cfg.insulinType} • ${cfg.defaultUnits} UI) salvo e alarme sincronizado!")
            },
            onDeleteConfig = { cfgId ->
                healthRepo.deleteInsulinScheduleConfig(cfgId)
                triggerFeedback("Horário de insulina removido.")
            },
            onDismiss = { showInsulinSchedulesDialog = false }
        )
    }

    if (showUnifiedHistoryModal) {
        UnifiedGlucoseAndInsulinHistoryDialog(
            familyMembers = familyMembers,
            initialMemberFilter = "ALL",
            glucoseRecords = healthState.glucoseRecords,
            insulinRecords = healthState.insulinRecords,
            defaultMember = defaultMember,
            onEditGlucose = { rec ->
                showUnifiedHistoryModal = false
                glucoseToEdit = rec
            },
            onDeleteGlucose = { rec ->
                glucoseToDelete = rec
            },
            onEditInsulin = { rec ->
                showUnifiedHistoryModal = false
                insulinToEdit = rec
            },
            onDeleteInsulin = { rec ->
                insulinToDelete = rec
            },
            onDismiss = { showUnifiedHistoryModal = false }
        )
    }

    if (showAddBpDialog || bpToEdit != null) {
        val editing = bpToEdit
        AddBloodPressureRecordDialog(
            initialRecord = editing,
            initialEpochDay = editing?.dateEpochDay ?: activeDateEpochDay,
            defaultScope = editing?.privacyScope ?: PrivacyScope.PERSONAL,
            onDismiss = {
                showAddBpDialog = false
                bpToEdit = null
            },
            onSave = { epochDay, timeHHmm, sys, dia, pulse, notes, scope ->
                if (editing != null) {
                    healthRepo.updateBloodPressureRecord(
                        recordId = editing.id,
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        systolic = sys,
                        diastolic = dia,
                        pulseBpm = pulse,
                        notes = notes,
                        privacyScope = scope,
                        memberId = activeMember.userId,
                        memberName = activeMember.displayName
                    )
                    triggerFeedback("❤️ Pressão de ${activeMember.displayName} atualizada: $sys/$dia mmHg ($pulse bpm)")
                } else {
                    healthRepo.addBloodPressureRecord(
                        dateEpochDay = epochDay,
                        timeHHmm = timeHHmm,
                        systolic = sys,
                        diastolic = dia,
                        pulseBpm = pulse,
                        notes = notes,
                        privacyScope = scope,
                        memberId = activeMember.userId,
                        memberName = activeMember.displayName
                    )
                    triggerFeedback("❤️ Pressão de ${activeMember.displayName} registrada: $sys/$dia mmHg ($pulse bpm)")
                }
                showAddBpDialog = false
                bpToEdit = null
            }
        )
    }

    bpToDelete?.let { rec ->
        OmniConfirmDeleteDialog(
            title = "Excluir Registro de Pressão?",
            message = "Deseja remover a aferição de ${rec.systolic}/${rec.diastolic} mmHg (${rec.dateIso})?",
            onConfirm = {
                healthRepo.deleteBloodPressureRecord(rec.id)
                triggerFeedback("Registro de pressão arterial removido.")
                bpToDelete = null
            },
            onDismiss = { bpToDelete = null }
        )
    }

    if (showEditDppDialog) {
        EditDppAndUltrasoundDialog(
            initialDppDate = effectiveDppDate,
            initialPregnancyEnabled = customizationConfig.pregnancyEnabled || effectiveDppDate.isNotBlank(),
            onDismiss = { showEditDppDialog = false },
            onSave = { enabled, finalDpp ->
                val mode = customizationConfig.pregnancyComparisonMode.ifBlank { "TODOS" }
                customizationRepo.updatePregnancyModule(
                    enabled = enabled,
                    dppDate = finalDpp,
                    notes = customizationConfig.pregnancyNotes,
                    comparisonMode = mode
                )
                onSyncPregnancyConfigToCloud(finalDpp, mode)
                triggerFeedback("🤰 DPP ($finalDpp) sincronizada no Hub, Saúde e Nuvem!")
                showEditDppDialog = false
            }
        )
    }
}

// =============================================================================
// CARD DE GESTAÇÃO (DPP) COM COMPARADOR (DOCE / FRUTA / ANIMAL) E PRÓXIMO MARCO
// Reutilizado tanto no topo da aba "Saúde & Glicemia" quanto no "Hub da Família"
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmniPregnancyHeroCard(
    dppDateRaw: String,
    comparisonMode: String,
    accentColor: Color,
    onSelectComparisonMode: (String) -> Unit,
    onOpenEditDpp: () -> Unit,
    modifier: Modifier = Modifier,
    extraFooterContent: (@Composable () -> Unit)? = null
) {
    val cardRadius = LocalOrganicCardRadius.current
    val pregProgress = remember(dppDateRaw) {
        PregnancyCalculator.calculateFromDpp(dppDateRaw)
    }
    val activeMode = remember(comparisonMode) {
        when (comparisonMode.trim().uppercase(Locale.ROOT)) {
            "DOCE", "FRUTA", "ANIMAL", "TODOS" -> comparisonMode.trim().uppercase(Locale.ROOT)
            else -> "TODOS"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_pregnancy_module_card"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🤰 Gestação da Família • ${pregProgress.gestationalAgeLabelPt}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "DPP: ${pregProgress.dppFormatted} • ${pregProgress.trimester}º Trimestre • Faltam ${pregProgress.daysRemainingToDpp} dias",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onOpenEditDpp,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_dpp_date_btn")
                ) {
                    Text("DPP / Ultrassom", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Barra de progresso das 40 semanas + contagem regressiva
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progresso das 40 semanas (${(pregProgress.progressFraction40Weeks * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "Tamanho aprox.: ${String.format(Locale.forLanguageTag("pt-BR"), "%.1f cm", pregProgress.approxLengthCm)} • Peso: ${pregProgress.approxWeightGrams} g",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { pregProgress.progressFraction40Weeks },
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // Chips de modo de comparação ("Doce" | "Fruta" | "Animal" | "Todos")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "DOCE" to "🧁 Doce",
                    "FRUTA" to "🍓 Fruta",
                    "ANIMAL" to "🐾 Animal",
                    "TODOS" to "✨ Todos"
                ).forEach { (modeKey, modeLabel) ->
                    val selected = activeMode == modeKey
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectComparisonMode(modeKey) },
                        label = {
                            Text(
                                text = modeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("pregnancy_mode_chip_$modeKey")
                    )
                }
            }

            // Card visual do Comparador da Semana Atual
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Na semana ${pregProgress.currentWeek}, o bebê tem o tamanho aproximado de:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    when (activeMode) {
                        "DOCE" -> Text(
                            text = "🧁 Doce: ${pregProgress.sweetComparison}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        "FRUTA" -> Text(
                            text = "🍓 Fruta: ${pregProgress.fruitComparison}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        "ANIMAL" -> Text(
                            text = "🐾 Animal: ${pregProgress.animalComparison}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🧁 Doce: ${pregProgress.sweetComparison}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "🍓 Fruta: ${pregProgress.fruitComparison}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "🐾 Animal: ${pregProgress.animalComparison}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    HorizontalDivider(color = accentColor.copy(alpha = 0.2f))

                    // Próximo marco (dias para a próxima semana e próximo trimestre)
                    val nextWeekText = if (pregProgress.currentWeek < 42) {
                        "⏳ Faltam ${pregProgress.daysToNextWeek} dia(s) para completar a ${pregProgress.currentWeek + 1}ª semana"
                    } else {
                        "🎉 Reta final da gestação!"
                    }
                    val nextTrimesterText = when {
                        pregProgress.trimester == 1 && pregProgress.daysToNextTrimester > 0 ->
                            " • ${pregProgress.daysToNextTrimester} dia(s) para entrar no 2º trimestre (14ª sem.)"
                        pregProgress.trimester == 2 && pregProgress.daysToNextTrimester > 0 ->
                            " • ${pregProgress.daysToNextTrimester} dia(s) para entrar no 3º trimestre (28ª sem.)"
                        else ->
                            " • 3º Trimestre em andamento (${pregProgress.daysRemainingToDpp} dias até a DPP)"
                    }
                    Text(
                        text = nextWeekText + nextTrimesterText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            extraFooterContent?.invoke()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDppAndUltrasoundDialog(
    initialDppDate: String,
    initialPregnancyEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, dppDateBr: String) -> Unit
) {
    var pregEnabled by remember { mutableStateOf(initialPregnancyEnabled) }
    var dppInput by remember { mutableStateOf(initialDppDate.ifBlank { "15/03/2027" }) }
    var useUltrasoundCalculator by remember { mutableStateOf(false) }
    var examDateInput by remember {
        mutableStateOf(OmniDateMaskUtils.epochDayToBrDate(DateCalendarUtils.todayEpochDay()))
    }
    var weeksAtExamText by remember { mutableStateOf("12") }
    var daysAtExamText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🤰 Configurar DPP ou Calcular pelo Ultrassom",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exibir Card de Gestação no Hub e Saúde", fontWeight = FontWeight.SemiBold)
                    Switch(checked = pregEnabled, onCheckedChange = { pregEnabled = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useUltrasoundCalculator,
                        onClick = { useUltrasoundCalculator = false },
                        label = { Text("Informar DPP Direta", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = useUltrasoundCalculator,
                        onClick = { useUltrasoundCalculator = true },
                        label = { Text("Calcular por Ultrassom", fontWeight = FontWeight.Bold) }
                    )
                }

                if (!useUltrasoundCalculator) {
                    MaskedDateTextField(
                        dateValue = dppInput,
                        onDateValueChange = { formatted, _ -> dppInput = formatted },
                        label = "Data Prevista do Parto - DPP (DD/MM/AAAA)",
                        placeholder = "Ex: 15/03/2027",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "edit_dpp_masked_input"
                    )
                } else {
                    Text(
                        text = "Informe a data em que o ultrassom foi feito e com quantas semanas/dias o bebê estava:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MaskedDateTextField(
                        dateValue = examDateInput,
                        onDateValueChange = { formatted, _ -> examDateInput = formatted },
                        label = "Data do Ultrassom (DD/MM/AAAA)",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "ultrasound_date_input"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weeksAtExamText,
                            onValueChange = { weeksAtExamText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Semanas no exame") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = daysAtExamText,
                            onValueChange = { daysAtExamText = it.filter { c -> c.isDigit() }.take(1) },
                            label = { Text("Dias (0 a 6)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val calculatedDpp = remember(examDateInput, weeksAtExamText, daysAtExamText) {
                        PregnancyCalculator.calculateDppFromUltrasound(
                            examDateBr = examDateInput,
                            weeksAtExam = weeksAtExamText.toIntOrNull() ?: 0,
                            daysAtExam = daysAtExamText.toIntOrNull() ?: 0
                        )
                    }
                    if (calculatedDpp != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IncomeEmerald.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ DPP calculada pelo ultrassom: $calculatedDpp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeEmerald,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedDpp = if (useUltrasoundCalculator) {
                        PregnancyCalculator.calculateDppFromUltrasound(
                            examDateBr = examDateInput,
                            weeksAtExam = weeksAtExamText.toIntOrNull() ?: 0,
                            daysAtExam = daysAtExamText.toIntOrNull() ?: 0
                        ) ?: dppInput.trim()
                    } else {
                        dppInput.trim()
                    }
                    onSave(pregEnabled, resolvedDpp)
                }
            ) {
                Text("Salvar DPP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntegratedOneHourPostMealTimerSection(
    timerState: com.example.data.PostMealTimerState,
    liveRemainingSeconds: Int,
    selectedMealSlot: String,
    onSelectMealSlot: (String) -> Unit,
    onStartTimer: () -> Unit,
    onPauseResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onLogPostMealGlucoseNow: () -> Unit
) {
    val minutes = liveRemainingSeconds / 60
    val seconds = liveRemainingSeconds % 60
    val formattedCountdown = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    val progressRatio = (liveRemainingSeconds.toFloat() / 3600f).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (timerState.completedAlertPending) {
            SavingsGold.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_meal_1h_timer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 4.dp.toPx()
                            drawArc(
                                color = Color(0xFF0077B6).copy(alpha = 0.18f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (timerState.completedAlertPending) IncomeEmerald else Color(0xFF0077B6),
                                startAngle = -90f,
                                sweepAngle = 360f * (if (timerState.isRunning) progressRatio else 1f),
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF0077B6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "⏱️ Cronômetro Pós-Refeição (1h • 60:00)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when {
                                timerState.isRunning ->
                                    "Contando 1h para $selectedMealSlot (iniciado às ${timerState.startedAtHHmm})"
                                timerState.completedAlertPending ->
                                    "🔔 1h concluída! Hora de medir $selectedMealSlot"
                                else ->
                                    "Alarme exato de 1h pós-refeição integrado ao celular"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = formattedCountdown,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (timerState.completedAlertPending) IncomeEmerald else Color(0xFF0077B6),
                    modifier = Modifier.testTag("post_meal_timer_display")
                )
            }

            // Seletor dos 3 períodos pós-refeição de 1h ("1h Pós-Café", "1h Pós-Almoço", "1h Pós-Janta")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DiabetesSlotCatalog.postMealTimerSlots.forEach { slot ->
                    val selected = selectedMealSlot == slot
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMealSlot(slot) },
                        label = {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!timerState.isRunning) {
                    Button(
                        onClick = onStartTimer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_1h_post_meal_timer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Iniciar 1h",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onPauseResumeTimer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_resume_1h_timer_btn")
                    ) {
                        Icon(
                            imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (timerState.isPaused) "Retomar" else "Pausar",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onResetTimer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("reset_1h_timer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar", maxLines = 1, softWrap = false)
                    }
                }

                OutlinedButton(
                    onClick = onLogPostMealGlucoseNow,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("test_1h_timer_alert_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DailyTrackerItemCard(
    habit: DailyTrackerHabit,
    currentValue: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onCompleteAll: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = remember(habit.colorHex) { Color(habit.colorHex.toInt()) }
    val progressRatio = (currentValue.toFloat() / habit.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isDone = currentValue >= habit.targetValue

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDone) IncomeEmerald else accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("tracker_card_${habit.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = habit.emoji, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = if (habit.privacyScope == PrivacyScope.PERSONAL) "🔒 Individual" else "👨‍👩‍👧‍👦 Família",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$currentValue / ${habit.targetValue} ${habit.unit}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) IncomeEmerald else accentColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("tracker_dec_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Diminuir",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onIncrement,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("tracker_inc_${habit.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${habit.stepValue}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }

                    if (!isDone) {
                        IconButton(
                            onClick = onCompleteAll,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Bater meta do dia",
                                tint = IncomeEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tracker_edit_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar tracker",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tracker_delete_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover tracker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progressRatio },
                color = if (isDone) IncomeEmerald else accentColor,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun GlucoseRecordRowCard(
    record: GlucoseRecord,
    fallbackMemberName: String = "",
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAlert = record.isAlert()
    val statusColor = if (isAlert) ExpenseCoral else IncomeEmerald
    val displayMember = record.memberName.takeIf {
        it.isNotBlank() && !it.equals("Você", true) && !it.equals("VO", true)
    } ?: fallbackMemberName

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = statusColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .testTag("glucose_record_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${record.valueMgDl} mg/dL",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = DiabetesSlotCatalog.normalizeSlot(record.slot),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (displayMember.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "👤 $displayMember",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.statusLabel()} • ${DateCalendarUtils.formatEpochDayPt(record.dateEpochDay)} às ${record.timeHHmm}" +
                        if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_glucose_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar medição",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_glucose_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir medição",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsulinRecordRowCard(
    record: InsulinRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .testTag("insulin_record_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💉 ${record.insulinType} • ${record.units} UI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = record.injectionSite,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.memberId} • ${record.dateIso} às ${record.timeFormatted}" +
                        if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar insulina",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir insulina",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BloodPressureRecordRowCard(
    record: BloodPressureRecord,
    fallbackMemberName: String = "",
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAlert = record.isAlert()
    val statusColor = if (isAlert) ExpenseCoral else IncomeEmerald
    val displayMember = record.memberName.ifBlank { fallbackMemberName }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = statusColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("bp_record_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${record.systolic}/${record.diastolic} mmHg",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${record.pulseBpm} bpm",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (displayMember.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "👤 $displayMember",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.statusLabel()} • ${DateCalendarUtils.formatEpochDayPt(record.dateEpochDay)} às ${record.timeHHmm}" +
                        if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_bp_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar aferição",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_bp_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir aferição",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomTrackerDialog(
    initialTracker: DailyTrackerHabit? = null,
    defaultScope: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        emoji: String,
        unit: String,
        targetValue: Int,
        stepValue: Int,
        colorHex: Long,
        privacyScope: String
    ) -> Unit
) {
    var title by remember(initialTracker) { mutableStateOf(initialTracker?.title ?: "") }
    var emoji by remember(initialTracker) { mutableStateOf(initialTracker?.emoji ?: "💧") }
    var unit by remember(initialTracker) { mutableStateOf(initialTracker?.unit ?: "copos") }
    var targetText by remember(initialTracker) { mutableStateOf(initialTracker?.targetValue?.toString() ?: "8") }
    var stepText by remember(initialTracker) { mutableStateOf(initialTracker?.stepValue?.toString() ?: "1") }
    var selectedColorHex by remember(initialTracker) { mutableLongStateOf(initialTracker?.colorHex ?: 0xFF0077B6L) }
    var privacyScope by remember(initialTracker) { mutableStateOf(initialTracker?.privacyScope ?: defaultScope) }

    val emojiChoices = listOf("💧", "💊", "🚶", "🧘", "🍎", "🏋️", "😴", "🍵", "🦷", "🩸")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTracker != null) "Editar Tracker Diário" else "Novo Inegociável (Tracker Diário)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Hábito / Tracker") },
                    placeholder = { Text("Ex: Água, Creatina, Alongamento") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_tracker_title_input")
                )

                Text("Ícone:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojiChoices.forEach { em ->
                        FilterChip(
                            selected = emoji == em,
                            onClick = { emoji = em },
                            label = { Text(em) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Meta Diária") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidade (copos, doses, ml)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Incremento por toque (+1, +250, etc.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = privacyScope == PrivacyScope.PERSONAL,
                        onClick = { privacyScope = PrivacyScope.PERSONAL },
                        label = { Text("🔒 Individual") }
                    )
                    FilterChip(
                        selected = privacyScope == PrivacyScope.FAMILY,
                        onClick = { privacyScope = PrivacyScope.FAMILY },
                        label = { Text("👨‍👩‍👧‍👦 Familiar") }
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomizationCatalog.memberAgendaColors.take(6).forEach { (_, hex) ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex.toInt()))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            emoji,
                            unit,
                            (targetText.toIntOrNull() ?: 1).coerceAtLeast(1),
                            (stepText.toIntOrNull() ?: 1).coerceAtLeast(1),
                            selectedColorHex,
                            privacyScope
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_tracker_btn")
            ) {
                Text(if (initialTracker != null) "Salvar Alterações" else "Criar Tracker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGlucoseRecordDialog(
    initialRecord: GlucoseRecord? = null,
    initialEpochDay: Long,
    initialSlot: String,
    availableSlots: List<String> = DiabetesSlotCatalog.allSlots,
    defaultScope: String = PrivacyScope.FAMILY,
    memberName: String = "",
    onDismiss: () -> Unit,
    onSave: (
        dateEpochDay: Long,
        timeHHmm: String,
        slot: String,
        valueMgDl: Int,
        notes: String,
        privacyScope: String,
        startOneHourAlarm: Boolean
    ) -> Unit
) {
    var dateDigitsOrFormatted by remember(initialRecord, initialEpochDay) {
        mutableStateOf(OmniDateMaskUtils.epochDayToBrDate(initialRecord?.dateEpochDay ?: initialEpochDay))
    }
    var selectedEpochDay by remember(initialRecord, initialEpochDay) {
        mutableLongStateOf(initialRecord?.dateEpochDay ?: initialEpochDay)
    }
    var valueText by remember(initialRecord) { mutableStateOf(initialRecord?.valueMgDl?.toString() ?: "") }
    var timeText by remember(initialRecord) { mutableStateOf(initialRecord?.timeHHmm ?: DateCalendarUtils.currentTimeHHmm()) }
    var selectedSlot by remember(initialRecord, initialSlot) {
        mutableStateOf(DiabetesSlotCatalog.normalizeSlot(initialRecord?.slot ?: initialSlot))
    }
    var notes by remember(initialRecord) { mutableStateOf(initialRecord?.notes ?: "") }
    var privacyScope by remember(initialRecord, defaultScope) { mutableStateOf(initialRecord?.privacyScope ?: defaultScope) }

    val postMealTargetSlot = remember(selectedSlot) {
        DiabetesSlotCatalog.postMealSlotAfterPreMeal(selectedSlot)
    }
    var startOneHourAlarm by remember(selectedSlot) {
        mutableStateOf(postMealTargetSlot != null && initialRecord == null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialRecord != null) "🩸 Editar Medição de Glicemia" else "🩸 Registrar Medição de Glicemia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                if (memberName.isNotBlank()) {
                    Text(
                        text = "Paciente: $memberName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MaskedDateTextField(
                    dateValue = dateDigitsOrFormatted,
                    onDateValueChange = { formatted, parsedEpoch ->
                        dateDigitsOrFormatted = formatted
                        if (parsedEpoch != null) selectedEpochDay = parsedEpoch
                    },
                    label = "Data da Medição (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "glucose_date_masked_input"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Glicemia (mg/dL)") },
                        placeholder = { Text("Ex: 96") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("glucose_value_input")
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Hora (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Período Oficial (6 Períodos):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DiabetesSlotCatalog.allSlots.forEach { slot ->
                        FilterChip(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            label = { Text(slot, fontWeight = if (selectedSlot == slot) FontWeight.ExtraBold else FontWeight.Medium) }
                        )
                    }
                }

                // Switch opcional "Iniciar alarme de 1h ao salvar" ao selecionar "Almoço" ou "Janta"
                if (postMealTargetSlot != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⏱️ Iniciar alarme de 1h ao salvar",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Dispara o cronômetro de 60 min para medir '$postMealTargetSlot'",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = startOneHourAlarm,
                                onCheckedChange = { startOneHourAlarm = it },
                                modifier = Modifier.testTag("start_1h_alarm_on_save_switch")
                            )
                        }
                    }
                }

                // Chips rápidos de contexto clínico
                Text(
                    text = "Contexto Rápido (toque para adicionar):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlucoseContextCatalog.quickTags.forEach { tag ->
                        val isTagPresent = notes.contains(tag, ignoreCase = true)
                        FilterChip(
                            selected = isTagPresent,
                            onClick = {
                                notes = if (isTagPresent) {
                                    notes.replace(tag, "", ignoreCase = true)
                                        .replace(" •  • ", " • ")
                                        .trim(' ', '•', ',')
                                } else if (notes.isBlank()) {
                                    tag
                                } else {
                                    "${notes.trim()} • $tag"
                                }
                            },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isTagPresent) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações / Contexto (refeição, sintomas)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glucose_notes_input")
                )
            }
        },
        confirmButton = {
            val parsedVal = valueText.toIntOrNull() ?: 0
            Button(
                onClick = {
                    if (parsedVal > 0) {
                        val finalEpoch = OmniDateMaskUtils.parseBrDateToEpochDay(dateDigitsOrFormatted) ?: selectedEpochDay
                        onSave(
                            finalEpoch,
                            timeText,
                            selectedSlot,
                            parsedVal,
                            notes,
                            privacyScope,
                            startOneHourAlarm && postMealTargetSlot != null
                        )
                    }
                },
                enabled = parsedVal > 0,
                modifier = Modifier.testTag("confirm_save_glucose_btn")
            ) {
                Text(if (initialRecord != null) "Atualizar Glicemia" else "Salvar Glicemia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// =============================================================================
// MODAL 1-TOQUE DE REGISTRO DE INSULINA E RODÍZIO INTELIGENTE DE LOCAL
// Reutilizado tanto na aba "Saúde & Glicemia" quanto no card expansível do "Hub"
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddInsulinRecordDialog(
    initialRecord: InsulinRecord? = null,
    initialEpochDay: Long = DateCalendarUtils.todayEpochDay(),
    activeMember: FamilyMemberBadge,
    familyMembers: List<FamilyMemberBadge>,
    healthState: com.example.data.HealthModuleState,
    onOpenSchedulesConfig: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        dateEpochDay: Long,
        timeHHmm: String,
        insulinType: String,
        units: Int,
        injectionSite: String,
        memberBadge: FamilyMemberBadge,
        notes: String
    ) -> Unit
) {
    var selectedMember by remember(activeMember) { mutableStateOf(activeMember) }
    val latestForMember = remember(healthState.insulinRecords, selectedMember) {
        healthState.latestInsulinForMember(selectedMember.userId, selectedMember.displayName)
    }
    val lastSiteUsed = latestForMember?.injectionSite
    val suggestedNextSite = remember(lastSiteUsed) {
        InsulinSiteCatalog.suggestNextSite(lastSiteUsed)
    }
    val siteHistoryMap = remember(healthState.insulinRecords, selectedMember) {
        healthState.lastApplicationBySiteForMember(selectedMember.userId, selectedMember.displayName)
    }

    val configuredTypes = remember(healthState.insulinConfigs) {
        (healthState.insulinConfigs.map { it.insulinType } + InsulinTypeCatalog.presets)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    val initialType = initialRecord?.insulinType
        ?: healthState.insulinConfigs.firstOrNull()?.insulinType
        ?: latestForMember?.insulinType
        ?: InsulinTypeCatalog.presets.first()

    val defaultDoseForType = remember(initialType, healthState.insulinConfigs) {
        healthState.insulinConfigs.firstOrNull { it.insulinType.equals(initialType, ignoreCase = true) }?.defaultUnits
            ?: latestForMember?.units
            ?: 10
    }

    var selectedType by remember(initialRecord, initialType) { mutableStateOf(initialType) }
    var unitsText by remember(initialRecord, defaultDoseForType) {
        mutableStateOf((initialRecord?.units ?: defaultDoseForType).toString())
    }
    var selectedSite by remember(initialRecord, suggestedNextSite) {
        mutableStateOf(initialRecord?.injectionSite?.ifBlank { suggestedNextSite } ?: suggestedNextSite)
    }
    var dateDigitsOrFormatted by remember(initialRecord, initialEpochDay) {
        val epoch = initialRecord?.dateIso?.let {
            runCatching { java.time.LocalDate.parse(it).toEpochDay() }.getOrNull()
        } ?: initialEpochDay
        mutableStateOf(OmniDateMaskUtils.epochDayToBrDate(epoch))
    }
    var selectedEpochDay by remember(initialRecord, initialEpochDay) {
        val epoch = initialRecord?.dateIso?.let {
            runCatching { java.time.LocalDate.parse(it).toEpochDay() }.getOrNull()
        } ?: initialEpochDay
        mutableLongStateOf(epoch)
    }
    var timeText by remember(initialRecord) {
        mutableStateOf(initialRecord?.timeFormatted?.ifBlank { DateCalendarUtils.currentTimeHHmm() } ?: DateCalendarUtils.currentTimeHHmm())
    }
    var notes by remember(initialRecord) { mutableStateOf(initialRecord?.notes ?: "") }

    val todayIso = remember { DateCalendarUtils.todayIsoDate() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initialRecord != null) "💉 Editar Aplicação de Insulina" else "💉 Registrar Insulina & Rodízio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Paciente: ${selectedMember.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (onOpenSchedulesConfig != null) {
                    TextButton(onClick = onOpenSchedulesConfig) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alarmes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (familyMembers.size > 1) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        familyMembers.forEach { member ->
                            val isSelected = selectedMember.userId == member.userId ||
                                selectedMember.displayName.equals(member.displayName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMember = member },
                                label = {
                                    Text(
                                        text = "${member.avatarEmoji} ${member.displayName}",
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                Text("Tipo de Insulina:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    configuredTypes.forEach { type ->
                        val isSel = selectedType == type
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedType = type
                                val cfgUnits = healthState.insulinConfigs
                                    .firstOrNull { it.insulinType.equals(type, ignoreCase = true) }
                                    ?.defaultUnits
                                if (cfgUnits != null && initialRecord == null) {
                                    unitsText = cfgUnits.toString()
                                }
                            },
                            label = {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { unitsText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Dose (UI)") },
                        placeholder = { Text("Ex: 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("insulin_units_input")
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Hora (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                MaskedDateTextField(
                    dateValue = dateDigitsOrFormatted,
                    onDateValueChange = { formatted, parsedEpoch ->
                        dateDigitsOrFormatted = formatted
                        if (parsedEpoch != null) selectedEpochDay = parsedEpoch
                    },
                    label = "Data da Aplicação (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth()
                )

                // Destaque do último local utilizado e sugestão automática do próximo local no rodízio
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = IncomeEmerald.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IncomeEmerald.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "🔄 Rodízio de Local Sugerido: $suggestedNextSite",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomeEmerald
                        )
                        Text(
                            text = if (!lastSiteUsed.isNullOrBlank()) {
                                "Último local utilizado: $lastSiteUsed (${latestForMember?.timeFormatted.orEmpty()})"
                            } else {
                                "Primeira aplicação registrada • Recomendado iniciar por $suggestedNextSite"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Local de Aplicação (com histórico visual por local):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InsulinSiteCatalog.allSites.forEach { site ->
                        val isSelected = selectedSite == site
                        val isLastUsed = site == lastSiteUsed
                        val isSuggested = site == suggestedNextSite
                        val lastRecAtSite = siteHistoryMap[site]
                        val lastWhenText = when {
                            lastRecAtSite == null -> "Livre"
                            lastRecAtSite.dateIso == todayIso -> "Hoje ${lastRecAtSite.timeFormatted}"
                            else -> lastRecAtSite.dateIso.takeLast(5).replace("-", "/")
                        }

                        val borderCol = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isSuggested -> IncomeEmerald
                            isLastUsed -> SavingsGold
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }
                        val bgCol = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isSuggested -> IncomeEmerald.copy(alpha = 0.14f)
                            isLastUsed -> SavingsGold.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bgCol,
                            modifier = Modifier
                                .border(
                                    width = if (isSelected || isSuggested) 1.5.dp else 1.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedSite = site }
                                .testTag("insulin_site_chip_$site")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = site,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected || isSuggested) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                    if (isSuggested) {
                                        Text(
                                            text = "• Sugerido",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = IncomeEmerald
                                        )
                                    } else if (isLastUsed) {
                                        Text(
                                            text = "• Último",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                                Text(
                                    text = "Última vez: $lastWhenText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações opcionais") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsedUnits = unitsText.toIntOrNull() ?: 0
            Button(
                onClick = {
                    if (parsedUnits > 0) {
                        val finalEpoch = OmniDateMaskUtils.parseBrDateToEpochDay(dateDigitsOrFormatted) ?: selectedEpochDay
                        onSave(
                            finalEpoch,
                            timeText,
                            selectedType,
                            parsedUnits,
                            selectedSite,
                            selectedMember,
                            notes
                        )
                    }
                },
                enabled = parsedUnits > 0,
                modifier = Modifier.testTag("confirm_save_insulin_btn")
            ) {
                Text(if (initialRecord != null) "Atualizar Insulina" else "Registrar Insulina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsulinSchedulesConfigDialog(
    configs: List<InsulinScheduleConfig>,
    activeMemberName: String,
    onSaveConfig: (InsulinScheduleConfig) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(InsulinTypeCatalog.presets.first()) }
    var unitsText by remember { mutableStateOf("10") }
    var hourText by remember { mutableStateOf("08") }
    var minuteText by remember { mutableStateOf("00") }
    var alarmEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⏰ Tipos, Doses Habituais & Alarmes de Insulina",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure os tipos de insulina, doses habituais em UI e horários fixos vinculados ao AlarmManager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (configs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        configs.forEach { cfg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "💉 ${cfg.insulinType} • ${cfg.defaultUnits} UI",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Horário: ${DateCalendarUtils.formatTime(cfg.alarmHour, cfg.alarmMinute)} • ${if (cfg.alarmEnabled) "Alarme Ativo 🔔" else "Sem alarme"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = cfg.alarmEnabled,
                                        onCheckedChange = { enabled ->
                                            onSaveConfig(cfg.copy(alarmEnabled = enabled))
                                        }
                                    )
                                    IconButton(onClick = { onDeleteConfig(cfg.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remover horário"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }

                Text("Adicionar Novo Horário / Dose Habitual:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InsulinTypeCatalog.presets.forEach { preset ->
                        FilterChip(
                            selected = selectedType == preset,
                            onClick = { selectedType = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { unitsText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Dose (UI)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hora (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Despertar alarme diário neste horário", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                Button(
                    onClick = {
                        val units = (unitsText.toIntOrNull() ?: 10).coerceIn(1, 300)
                        val hour = (hourText.toIntOrNull() ?: 8).coerceIn(0, 23)
                        val minute = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                        onSaveConfig(
                            InsulinScheduleConfig(
                                memberId = activeMemberName,
                                insulinType = selectedType,
                                defaultUnits = units,
                                alarmHour = hour,
                                alarmMinute = minute,
                                alarmEnabled = alarmEnabled
                            )
                        )
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adicionar Horário de Insulina", fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Concluir", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedGlucoseAndInsulinHistoryDialog(
    familyMembers: List<FamilyMemberBadge>,
    initialMemberFilter: String = "ALL",
    glucoseRecords: List<GlucoseRecord>,
    insulinRecords: List<InsulinRecord>,
    defaultMember: FamilyMemberBadge,
    onEditGlucose: (GlucoseRecord) -> Unit,
    onDeleteGlucose: (GlucoseRecord) -> Unit,
    onEditInsulin: (InsulinRecord) -> Unit,
    onDeleteInsulin: (InsulinRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var memberFilter by remember { mutableStateOf(initialMemberFilter) }

    fun recordMatchesFilter(memberId: String, memberName: String): Boolean {
        if (memberFilter == "ALL") return true
        val targetMember = familyMembers.firstOrNull {
            it.userId == memberFilter || it.displayName.equals(memberFilter, ignoreCase = true)
        } ?: return true
        val isPrimary = targetMember.userId == defaultMember.userId ||
            targetMember.displayName.equals(defaultMember.displayName, ignoreCase = true)

        val cleanId = memberId.trim()
        val cleanName = memberName.trim()
        if ((cleanId.isBlank() || cleanId.equals("Você", true) || cleanId.equals("VO", true)) &&
            (cleanName.isBlank() || cleanName.equals("Você", true) || cleanName.equals("VO", true))
        ) {
            return isPrimary
        }
        return cleanId.equals(targetMember.userId, ignoreCase = true) ||
            cleanId.equals(targetMember.displayName, ignoreCase = true) ||
            cleanName.equals(targetMember.displayName, ignoreCase = true) ||
            cleanName.equals(targetMember.userId, ignoreCase = true)
    }

    val filteredGlucose = remember(glucoseRecords, memberFilter, familyMembers) {
        glucoseRecords.filter { recordMatchesFilter(it.memberId, it.memberName) }
    }
    val filteredInsulin = remember(insulinRecords, memberFilter, familyMembers) {
        insulinRecords.filter { recordMatchesFilter(it.memberId, it.memberId) }
    }

    val allDatesDesc = remember(filteredGlucose, filteredInsulin) {
        (filteredGlucose.map { it.dateIso } + filteredInsulin.map { it.dateIso })
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "📋 Histórico de Glicemia & Insulina",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Agrupado por dia • Edição e exclusão sincronizadas em tempo real",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filtro por membro ("Todos", "Marcus", "Nathalia")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = memberFilter == "ALL",
                        onClick = { memberFilter = "ALL" },
                        label = { Text("Todos", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("history_filter_all")
                    )
                    familyMembers.forEach { member ->
                        val isSel = memberFilter == member.userId || memberFilter.equals(member.displayName, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { memberFilter = member.userId },
                            label = {
                                Text(
                                    text = "${member.avatarEmoji} ${member.displayName}",
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("history_filter_${member.displayName}")
                        )
                    }
                }

                if (allDatesDesc.isEmpty()) {
                    Text(
                        text = "Nenhum registro encontrado para o filtro selecionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allDatesDesc.take(30).forEach { dateIso ->
                        val dayEpoch = runCatching { java.time.LocalDate.parse(dateIso).toEpochDay() }.getOrNull()
                        val dateTitle = if (dayEpoch != null) DateCalendarUtils.formatEpochDayPt(dayEpoch) else dateIso
                        val dayGlu = filteredGlucose.filter { it.dateIso == dateIso }
                        val dayIns = filteredInsulin.filter { it.dateIso == dateIso }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📅 $dateTitle (${dayGlu.size} glicemia • ${dayIns.size} insulina)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            dayGlu.forEach { rec ->
                                GlucoseRecordRowCard(
                                    record = rec,
                                    fallbackMemberName = defaultMember.displayName,
                                    onEdit = { onEditGlucose(rec) },
                                    onDelete = { onDeleteGlucose(rec) }
                                )
                            }
                            dayIns.forEach { ins ->
                                InsulinRecordRowCard(
                                    record = ins,
                                    onEdit = { onEditInsulin(ins) },
                                    onDelete = { onDeleteInsulin(ins) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AddBloodPressureRecordDialog(
    initialRecord: BloodPressureRecord? = null,
    initialEpochDay: Long,
    defaultScope: String,
    onDismiss: () -> Unit,
    onSave: (
        dateEpochDay: Long,
        timeHHmm: String,
        systolic: Int,
        diastolic: Int,
        pulseBpm: Int,
        notes: String,
        privacyScope: String
    ) -> Unit
) {
    var dateDigitsOrFormatted by remember(initialRecord, initialEpochDay) {
        mutableStateOf(OmniDateMaskUtils.epochDayToBrDate(initialRecord?.dateEpochDay ?: initialEpochDay))
    }
    var selectedEpochDay by remember(initialRecord, initialEpochDay) {
        mutableLongStateOf(initialRecord?.dateEpochDay ?: initialEpochDay)
    }
    var sysText by remember(initialRecord) { mutableStateOf(initialRecord?.systolic?.toString() ?: "120") }
    var diaText by remember(initialRecord) { mutableStateOf(initialRecord?.diastolic?.toString() ?: "80") }
    var pulseText by remember(initialRecord) { mutableStateOf(initialRecord?.pulseBpm?.toString() ?: "72") }
    var timeText by remember(initialRecord) { mutableStateOf(initialRecord?.timeHHmm ?: DateCalendarUtils.currentTimeHHmm()) }
    var notes by remember(initialRecord) { mutableStateOf(initialRecord?.notes ?: "") }
    var privacyScope by remember(initialRecord, defaultScope) { mutableStateOf(initialRecord?.privacyScope ?: defaultScope) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialRecord != null) "❤️ Editar Pressão Arterial" else "❤️ Registrar Pressão Arterial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MaskedDateTextField(
                    dateValue = dateDigitsOrFormatted,
                    onDateValueChange = { formatted, parsedEpoch ->
                        dateDigitsOrFormatted = formatted
                        if (parsedEpoch != null) selectedEpochDay = parsedEpoch
                    },
                    label = "Data da Aferição (DD/MM/AAAA)",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "bp_date_masked_input"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sysText,
                        onValueChange = { sysText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Sistólica (PAS)") },
                        placeholder = { Text("120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bp_systolic_input")
                    )
                    OutlinedTextField(
                        value = diaText,
                        onValueChange = { diaText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Diastólica (PAD)") },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bp_diastolic_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pulseText,
                        onValueChange = { pulseText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Pulso (bpm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Hora (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = privacyScope == PrivacyScope.PERSONAL,
                        onClick = { privacyScope = PrivacyScope.PERSONAL },
                        label = { Text("🔒 Individual") }
                    )
                    FilterChip(
                        selected = privacyScope == PrivacyScope.FAMILY,
                        onClick = { privacyScope = PrivacyScope.FAMILY },
                        label = { Text("👨‍👩‍👧‍👦 Familiar") }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (em repouso, medicação, sintomas)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bp_notes_input")
                )
            }
        },
        confirmButton = {
            val sys = sysText.toIntOrNull() ?: 0
            val dia = diaText.toIntOrNull() ?: 0
            val pulse = pulseText.toIntOrNull() ?: 72
            Button(
                onClick = {
                    if (sys > 0 && dia > 0) {
                        val finalEpoch = OmniDateMaskUtils.parseBrDateToEpochDay(dateDigitsOrFormatted) ?: selectedEpochDay
                        onSave(finalEpoch, timeText, sys, dia, pulse, notes, privacyScope)
                    }
                },
                enabled = sys > 0 && dia > 0,
                modifier = Modifier.testTag("confirm_save_bp_btn")
            ) {
                Text(if (initialRecord != null) "Atualizar Pressão" else "Salvar Pressão")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
