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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
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
import com.example.data.GlucoseRecord
import com.example.data.HealthCatalog
import com.example.data.HealthFinanceSuggestion
import com.example.data.HealthRepository
import com.example.data.MedicalReportPdfGenerator
import com.example.data.PregnancyCalculator
import com.example.data.PrivacyScope
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
    household: com.example.data.Household = com.example.data.Household(),
    userProfile: com.example.data.UserProfile? = null
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val healthState by healthRepo.stateFlow.collectAsStateWithLifecycle()
    val triggerFeedback = rememberOmniFeedback(onShowBannerMessage)

    val selectedDateIso = remember(selectedEpochDay) {
        DateCalendarUtils.formatIsoDate(selectedEpochDay)
    }

    // Lista oficial de membros da família (ex: Marcus e Nathalia) sincronizada com o Firestore
    val familyMembers = remember(household.members, customizationConfig.profileName, customizationConfig.profileColorHex, userProfile?.userId) {
        household.membersWithAssignedColors(
            fallbackProfileName = customizationConfig.profileName,
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
            ?: com.example.data.FamilyMemberBadge(
                userId = userProfile?.userId.orEmpty().ifBlank { "member_primary" },
                displayName = customizationConfig.profileName.ifBlank { "Meu Perfil" },
                colorHex = customizationConfig.profileColorHex
            )
    }

    var selectedMemberId by remember(defaultMember.userId) { mutableStateOf(defaultMember.userId) }
    val activeMember = remember(familyMembers, selectedMemberId, defaultMember) {
        familyMembers.firstOrNull { it.userId == selectedMemberId } ?: defaultMember
    }
    val isPrimaryDefaultMember = remember(activeMember, defaultMember) {
        activeMember.userId == defaultMember.userId ||
            activeMember.displayName.equals(defaultMember.displayName, ignoreCase = true)
    }

    // Filtro de Escopo na Saúde ("ALL", PrivacyScope.PERSONAL, PrivacyScope.FAMILY)
    var scopeFilter by remember { mutableStateOf("ALL") }
    // Sub-aba das medições clínicas: "GLUCOSE" (Glicemia) ou "BP" (Pressão Arterial)
    var clinicalTab by remember { mutableStateOf("GLUCOSE") }
    // Período selecionado para o Relatório Médico PDF (7, 30 ou 365 dias)
    var pdfPeriodDays by remember { mutableIntStateOf(30) }

    // Modais de criação/edição e exclusão com confirmação
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var trackerToEdit by remember { mutableStateOf<DailyTrackerHabit?>(null) }
    var trackerToDelete by remember { mutableStateOf<DailyTrackerHabit?>(null) }

    var showAddGlucoseDialog by remember { mutableStateOf(false) }
    var glucoseToEdit by remember { mutableStateOf<GlucoseRecord?>(null) }
    var glucoseToDelete by remember { mutableStateOf<GlucoseRecord?>(null) }
    var presetGlucoseSlot by remember { mutableStateOf(DiabetesSlotCatalog.JEJUM) }

    var showAddBpDialog by remember { mutableStateOf(false) }
    var bpToEdit by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var bpToDelete by remember { mutableStateOf<BloodPressureRecord?>(null) }

    var showEditDppDialog by remember { mutableStateOf(false) }
    var financeSuggestionToConfirm by remember { mutableStateOf<HealthFinanceSuggestion?>(null) }

    // Cronômetro Pós-Refeição em tempo real
    val timerState = healthState.postMealTimer
    var selectedMealForTimer by remember(timerState.mealLabel) { mutableStateOf(timerState.mealLabel) }
    var selectedDurationSeconds by remember(timerState.totalDurationSeconds) {
        mutableIntStateOf(timerState.totalDurationSeconds.coerceAtLeast(60))
    }
    var liveRemainingSeconds by remember(timerState) {
        mutableIntStateOf(
            if (timerState.isRunning && !timerState.isPaused) {
                ((timerState.targetEndEpochMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            } else {
                timerState.pausedRemainingSeconds
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
                    AlarmScheduler.showImmediateNotification(
                        context = context,
                        notificationId = 7744,
                        title = "⏰ Pós-${timerState.mealLabel} Concluído!",
                        message = "Momento ideal para registrar sua medição no Monitoramento de Glicemia."
                    )
                    break
                } else {
                    liveRemainingSeconds = rem
                }
                delay(1000L)
            }
        } else {
            liveRemainingSeconds = timerState.pausedRemainingSeconds
        }
    }

    val filteredTrackers = remember(healthState.trackers, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> healthState.trackers.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> healthState.trackers.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> healthState.trackers
        }
    }

    // Filtra medições clínicas individualmente pelo membro selecionado (Marcus / Nathalia) sem misturar dados do casal
    val filteredGlucose = remember(healthState.glucoseRecords, activeMember, isPrimaryDefaultMember) {
        healthState.glucoseRecords.filter { rec ->
            val matchesMember = if (rec.memberId.isBlank() && rec.memberName.isBlank()) {
                isPrimaryDefaultMember
            } else {
                rec.memberId == activeMember.userId ||
                    rec.memberName.equals(activeMember.displayName, ignoreCase = true)
            }
            matchesMember
        }
    }

    val filteredBp = remember(healthState.bloodPressureRecords, activeMember, isPrimaryDefaultMember) {
        healthState.bloodPressureRecords.filter { rec ->
            val matchesMember = if (rec.memberId.isBlank() && rec.memberName.isBlank()) {
                isPrimaryDefaultMember
            } else {
                rec.memberId == activeMember.userId ||
                    rec.memberName.equals(activeMember.displayName, ignoreCase = true)
            }
            matchesMember
        }
    }

    val memberFilteredHealthState = remember(healthState, filteredGlucose, filteredBp) {
        healthState.copy(
            glucoseRecords = filteredGlucose,
            bloodPressureRecords = filteredBp
        )
    }

    // Launcher para salvar PDF nativo em local escolhido pelo usuário (CreateDocument)
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
                healthRepo.recordGeneratedPdfInfo(result.filePath, result.summaryText)
                onShowBannerMessage("📄 Relatório Médico de ${activeMember.displayName} salvo! ${result.summaryText}")
            } else {
                onShowBannerMessage(result.summaryText)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("health_module_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabeçalho & Filtro de Escopo (Tudo / Individual / Familiar)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saúde, Inegociáveis & Relatório PDF",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val activeSpecificModules = buildList {
                                add("Trackers diários")
                                if (customizationConfig.diabetesEnabled) add("Monitoramento de Glicemia & Timer")
                                if (customizationConfig.bloodPressureEnabled) add("Pressão Arterial")
                                if (customizationConfig.pregnancyEnabled) add("Gestação Familiar")
                            }.joinToString(", ")
                            Text(
                                text = activeSpecificModules,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
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
                                    onShowBannerMessage("📄 PDF Médico de ${activeMember.displayName} gerado! ${localResult.summaryText}")
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quick_generate_pdf_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gerar PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Seletor Rápido de Membro da Família (ex.: Marcus / Nathalia) para separar histórico clínico
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Paciente / Membro Selecionado:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            familyMembers.forEach { member ->
                                val isSelected = activeMember.userId == member.userId
                                val memberColor = Color(member.colorHex.toInt())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMemberId = member.userId },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(memberColor)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${member.avatarEmoji} ${member.displayName}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("health_member_chip_${member.userId}")
                                )
                            }
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
                            label = {
                                Text(if (customizationConfig.pregnancyEnabled) "Familiar & Gestação" else "Familiar")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("health_scope_family")
                        )
                    }
                }
            }
        }

        // 2. INEGOCIÁVEIS PERSONALIZÁVEIS (TRACKERS DIÁRIOS: ÁGUA, VITAMINAS, ETC.)
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
                                text = "Data: $selectedDateIso • $doneCount/${filteredTrackers.size} metas batidas hoje",
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
                            onEdit = {
                                trackerToEdit = habit
                            },
                            onDelete = {
                                trackerToDelete = habit
                            }
                        )
                    }
                }
            }
        }

        // 3. CRONÔMETRO / TIMER PÓS-REFEIÇÃO (SOMENTE SE DIABETES / GLICEMIA ESTIVER MARCADO)
        if (customizationConfig.diabetesEnabled) {
            item {
                PostMealOneHourTimerCard(
                    timerState = timerState,
                    liveRemainingSeconds = liveRemainingSeconds,
                    selectedMeal = selectedMealForTimer,
                    selectedDurationSeconds = selectedDurationSeconds,
                    onSelectMeal = { selectedMealForTimer = it },
                    onSelectDuration = { secs -> selectedDurationSeconds = secs },
                    onStartTimer = {
                        val dur = selectedDurationSeconds.coerceAtLeast(60)
                        healthRepo.startPostMealTimer(selectedMealForTimer, dur)
                        val triggerAt = System.currentTimeMillis() + dur * 1000L
                        AlarmScheduler.schedulePostMealGlucoseAlarm(
                            context = context,
                            triggerAtMillis = triggerAt,
                            mealLabel = selectedMealForTimer
                        )
                        triggerFeedback("Cronômetro pós-$selectedMealForTimer iniciado com alarme!")
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
                    onTriggerAlertNow = {
                        // Abre o modal de registro de glicemia já com a refeição e o intervalo selecionados no timer
                        healthRepo.dismissPostMealTimerAlert()
                        AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                        presetGlucoseSlot = DiabetesSlotCatalog.slotForPostMealTimer(
                            mealLabel = selectedMealForTimer,
                            durationSeconds = selectedDurationSeconds
                        )
                        glucoseToEdit = null
                        showAddGlucoseDialog = true
                    },
                    onLogPostMealGlucoseNow = {
                        healthRepo.dismissPostMealTimerAlert()
                        presetGlucoseSlot = DiabetesSlotCatalog.slotForPostMealTimer(
                            mealLabel = selectedMealForTimer,
                            durationSeconds = selectedDurationSeconds
                        )
                        glucoseToEdit = null
                        showAddGlucoseDialog = true
                    }
                )
            }
        }

        // 4. MÓDULOS ESPECÍFICOS DE SAÚDE: MONITORAMENTO DE GLICEMIA (mg/dL) E/OU PRESSÃO ARTERIAL (mmHg)
        if (customizationConfig.diabetesEnabled || customizationConfig.bloodPressureEnabled) {
            item {
                val activeClinicalMode = when {
                    customizationConfig.diabetesEnabled && customizationConfig.bloodPressureEnabled -> clinicalTab
                    customizationConfig.diabetesEnabled -> "GLUCOSE"
                    else -> "BP"
                }
                val cardTitle = when {
                    customizationConfig.diabetesEnabled && customizationConfig.bloodPressureEnabled ->
                        "🩺 Monitoramento de Glicemia & Pressão (${activeMember.displayName})"
                    customizationConfig.diabetesEnabled ->
                        "🩸 Monitoramento de Glicemia (mg/dL) • ${activeMember.displayName}"
                    else ->
                        "❤️ Monitoramento de Pressão Arterial (mmHg) • ${activeMember.displayName}"
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cardTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Histórico individual de ${activeMember.displayName} • Toque em um período para registrar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (customizationConfig.diabetesEnabled && customizationConfig.bloodPressureEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = activeClinicalMode == "GLUCOSE",
                                    onClick = { clinicalTab = "GLUCOSE" },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "🩸 Glicemia (${filteredGlucose.size})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("clinical_tab_glucose")
                                )

                                FilterChip(
                                    selected = activeClinicalMode == "BP",
                                    onClick = { clinicalTab = "BP" },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.MonitorHeart,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "❤️ Pressão (${filteredBp.size})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("clinical_tab_bp")
                                )
                            }
                        }

                        if (activeClinicalMode == "GLUCOSE") {
                            // Resumo + Botão Registrar Glicemia + Grade Interativa dos 10 Slots Diários (sem texto cortado)
                            val avgGlucose = remember(filteredGlucose) {
                                if (filteredGlucose.isNotEmpty()) {
                                    filteredGlucose.map { it.valueMgDl }.average().roundToInt()
                                } else 0
                            }
                            val measuredSlotsToday = remember(filteredGlucose, selectedEpochDay) {
                                filteredGlucose
                                    .filter { it.dateEpochDay == selectedEpochDay }
                                    .map { DiabetesSlotCatalog.normalizeSlot(it.slot) }
                                    .toSet()
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (avgGlucose > 0) "Média (${activeMember.displayName}): $avgGlucose mg/dL" else "Sem medições de ${activeMember.displayName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${measuredSlotsToday.size} de ${DiabetesSlotCatalog.allSlots.size} períodos medidos na data selecionada",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                presetGlucoseSlot = DiabetesSlotCatalog.JEJUM
                                                glucoseToEdit = null
                                                showAddGlucoseDialog = true
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.testTag("open_add_glucose_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Glicemia", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "Períodos Diários (toque em um período para registrar):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DiabetesSlotCatalog.allSlots.forEach { slotName ->
                                            val isDoneToday = measuredSlotsToday.contains(slotName)
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (isDoneToday) {
                                                    IncomeEmerald.copy(alpha = 0.20f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                modifier = Modifier
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isDoneToday) IncomeEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        presetGlucoseSlot = slotName
                                                        glucoseToEdit = null
                                                        showAddGlucoseDialog = true
                                                    }
                                                    .testTag("glucose_slot_chip_$slotName")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isDoneToday) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Medido hoje",
                                                            tint = IncomeEmerald,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = slotName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isDoneToday) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isDoneToday) IncomeEmerald else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            filteredGlucose.take(15).forEach { record ->
                                GlucoseRecordRowCard(
                                    record = record,
                                    fallbackMemberName = activeMember.displayName,
                                    onEdit = { glucoseToEdit = record },
                                    onDelete = { glucoseToDelete = record }
                                )
                            }
                        } else {
                            // Resumo + Botão Registrar Pressão Arterial
                            val avgSys = remember(filteredBp) {
                                if (filteredBp.isNotEmpty()) filteredBp.map { it.systolic }.average().roundToInt() else 0
                            }
                            val avgDia = remember(filteredBp) {
                                if (filteredBp.isNotEmpty()) filteredBp.map { it.diastolic }.average().roundToInt() else 0
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (avgSys > 0) "Média (${activeMember.displayName}): $avgSys/$avgDia mmHg" else "Sem aferições de ${activeMember.displayName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Lembrete configurado: ${customizationConfig.bloodPressureReminderTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
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
                            }

                            filteredBp.take(15).forEach { record ->
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
        }

        // 5. ESCOPO FAMILIAR: MÓDULO DE ACOMPANHAMENTO GESTACIONAL (Somente quando Gestação estiver marcado no questionário)
        if (customizationConfig.pregnancyEnabled) {
            item {
                val pregProgress = remember(customizationConfig.pregnancyDppDate) {
                    PregnancyCalculator.calculateFromDpp(customizationConfig.pregnancyDppDate)
                }
                var newPregNote by remember { mutableStateOf("") }

                AuraExpandableSectionCard(
                    title = "🤰 Acompanhamento Gestacional (Familiar)",
                    subtitle = "DPP: ${pregProgress.dppFormatted} • Semana ${pregProgress.currentWeek}+${pregProgress.currentDaysRemainder}d (${pregProgress.trimester}º Trim.)",
                    icon = Icons.Default.FamilyRestroom,
                    accentColor = Color(customizationConfig.healthColorHex),
                    badgeText = "${pregProgress.currentWeek} sem",
                    initiallyExpanded = false,
                    testTag = "family_pregnancy_module_card"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tamanho aprox.: ${pregProgress.babySizeComparison} • Faltam ${pregProgress.daysRemainingToDpp} dias",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showEditDppDialog = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_dpp_date_btn")
                        ) {
                            Text("Ajustar DPP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Checklist Pré-Natal & Exames da Família",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

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
                                        text = "${item.notes} • Cat. sugerida: ${item.suggestedFinanceCategory}",
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
                            label = { Text("Registrar sintoma, consulta ou evolução do bebê") },
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

        // 6. INTEGRAÇÃO FINANCEIRA: SUGESTÕES AUTOMÁTICAS DE CATEGORIAS DE SAÚDE (Menu Expansível)
        item {
            val activeFinanceSuggestions = remember(customizationConfig.pregnancyEnabled) {
                HealthCatalog.financeSuggestionsFor(customizationConfig)
            }
            val suggestionsSubtitle = if (customizationConfig.pregnancyEnabled) {
                "Farmácia, Exames, Consultas & Pré-Natal integrados ao Financeiro e Agenda"
            } else {
                "Farmácia, Exames & Consultas integrados ao Financeiro e Agenda"
            }
            AuraExpandableSectionCard(
                title = "💊 Sugestões Financeiras de Saúde",
                subtitle = suggestionsSubtitle,
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeText = "${activeFinanceSuggestions.size} opções",
                initiallyExpanded = false,
                testTag = "health_finance_suggestions_card"
            ) {
                activeFinanceSuggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("health_finance_suggestion_${suggestion.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = suggestion.emoji, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = suggestion.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = suggestion.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = suggestion.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { financeSuggestionToConfirm = suggestion },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("launch_health_expense_btn_${suggestion.id}")
                            ) {
                                Text(
                                    text = "Lançar",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. RELATÓRIO MÉDICO EM PDF (NATIVO ANDROID - Menu Expansível)
        item {
            AuraExpandableSectionCard(
                title = "📄 Relatório Médico Consolidado em PDF",
                subtitle = "Exportar histórico de Glicemia, Pressão, Inegociáveis e Gestação",
                icon = Icons.Default.PictureAsPdf,
                accentColor = ExpenseCoral,
                badgeText = "PDF Nativo",
                initiallyExpanded = false,
                testTag = "medical_pdf_report_card"
            ) {
                Text(
                    text = "Selecione o Período do Relatório:",
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
                                profileName = customizationConfig.profileName,
                                periodDays = pdfPeriodDays,
                                customizationConfig = customizationConfig,
                                healthState = healthState
                            )
                            if (localResult.success) {
                                healthRepo.recordGeneratedPdfInfo(localResult.filePath, localResult.summaryText)
                                onShowBannerMessage("📄 Relatório Médico em PDF gerado com sucesso! ${localResult.summaryText}")
                            } else {
                                onShowBannerMessage(localResult.summaryText)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_medical_pdf_now_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar PDF Agora", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val fileName = "Relatorio_Medico_${customizationConfig.profileName.replace(" ", "_")}_${selectedDateIso}.pdf"
                            createPdfDocumentLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_medical_pdf_file_btn")
                    ) {
                        Text("Salvar PDF em Pasta...", fontWeight = FontWeight.Bold)
                    }
                }

                if (healthState.lastGeneratedPdfSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = IncomeEmerald.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✓ Último Relatório PDF Consolidado:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeEmerald
                            )
                            Text(
                                text = healthState.lastGeneratedPdfSummary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (healthState.lastGeneratedPdfPath.isNotBlank()) {
                                Text(
                                    text = "Arquivo: ${healthState.lastGeneratedPdfPath}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAIS DE CRIAÇÃO E REGISTRO
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
            initialEpochDay = editing?.dateEpochDay ?: selectedEpochDay,
            initialSlot = editing?.slot ?: presetGlucoseSlot,
            availableSlots = DiabetesSlotCatalog.allSlots,
            defaultScope = editing?.privacyScope ?: (if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL),
            memberName = activeMember.displayName,
            onDismiss = {
                showAddGlucoseDialog = false
                glucoseToEdit = null
            },
            onSave = { epochDay, timeHHmm, slot, valMgDl, notes, scope ->
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
                    triggerFeedback("🩸 Glicemia de ${activeMember.displayName} atualizada: $valMgDl mg/dL ($slot)")
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
                    triggerFeedback("🩸 Glicemia de ${activeMember.displayName} registrada: $valMgDl mg/dL ($slot)")
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

    if (showAddBpDialog || bpToEdit != null) {
        val editing = bpToEdit
        AddBloodPressureRecordDialog(
            initialRecord = editing,
            initialEpochDay = editing?.dateEpochDay ?: selectedEpochDay,
            defaultScope = editing?.privacyScope ?: (if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL),
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
        var dppInput by remember { mutableStateOf(customizationConfig.pregnancyDppDate.ifBlank { "15/03/2027" }) }
        var pregEnabled by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showEditDppDialog = false },
            title = { Text("Acompanhamento Gestacional (Família)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Módulo de Gestação Ativo", fontWeight = FontWeight.SemiBold)
                        Switch(checked = pregEnabled, onCheckedChange = { pregEnabled = it })
                    }
                    MaskedDateTextField(
                        dateValue = dppInput,
                        onDateValueChange = { formatted, _ -> dppInput = formatted },
                        label = "Data Prevista do Parto - DPP (DD/MM/AAAA)",
                        placeholder = "Ex: 15/03/2027",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "edit_dpp_masked_input"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customizationRepo.updatePregnancyModule(pregEnabled, dppInput.trim())
                        triggerFeedback("DPP salva com sucesso!")
                        showEditDppDialog = false
                    }
                ) {
                    Text("Salvar DPP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDppDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    financeSuggestionToConfirm?.let { suggestion ->
        var titleInput by remember(suggestion) { mutableStateOf(suggestion.title) }
        var amountInput by remember(suggestion) { mutableStateOf(suggestion.defaultAmountBrl.toString()) }
        var scopeInput by remember(suggestion) { mutableStateOf(suggestion.privacyScope) }

        AlertDialog(
            onDismissRequest = { financeSuggestionToConfirm = null },
            title = {
                Text(
                    text = "${suggestion.emoji} Lançar em ${suggestion.category}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Descrição da Despesa de Saúde") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Valor (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = scopeInput == PrivacyScope.FAMILY,
                            onClick = { scopeInput = PrivacyScope.FAMILY },
                            label = { Text("👨‍👩‍👧‍👦 Familiar") }
                        )
                        FilterChip(
                            selected = scopeInput == PrivacyScope.PERSONAL,
                            onClick = { scopeInput = PrivacyScope.PERSONAL },
                            label = { Text("🔒 Individual") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = amountInput.replace(",", ".").toDoubleOrNull() ?: suggestion.defaultAmountBrl
                        onCreateHealthFinanceExpense(
                            titleInput.ifBlank { suggestion.title },
                            parsed,
                            suggestion.category,
                            scopeInput
                        )
                        financeSuggestionToConfirm = null
                        onShowBannerMessage("💰 Despesa de saúde (${suggestion.category}) criada nas Finanças e na Agenda!")
                    }
                ) {
                    Text("Lançar nas Finanças + Agenda")
                }
            },
            dismissButton = {
                TextButton(onClick = { financeSuggestionToConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostMealOneHourTimerCard(
    timerState: com.example.data.PostMealTimerState,
    liveRemainingSeconds: Int,
    selectedMeal: String,
    selectedDurationSeconds: Int,
    onSelectMeal: (String) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onStartTimer: () -> Unit,
    onPauseResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onTriggerAlertNow: () -> Unit,
    onLogPostMealGlucoseNow: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val minutes = liveRemainingSeconds / 60
    val seconds = liveRemainingSeconds % 60
    val formattedCountdown = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    val progressRatio = (liveRemainingSeconds.toFloat() / timerState.totalDurationSeconds.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_meal_1h_timer_card"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (timerState.completedAlertPending) {
                SavingsGold.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 5.dp.toPx()
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
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "⏱️ Cronômetro Pós-Refeição com Alarme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (timerState.isRunning) {
                                "Contagem pós-$selectedMeal (Iniciado às ${timerState.startedAtHHmm})"
                            } else if (timerState.completedAlertPending) {
                                "🔔 Tempo concluído! Hora de medir sua glicemia pós-$selectedMeal"
                            } else {
                                "Escolha o tempo e a refeição para despertar o alarme exato de medição"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = formattedCountdown,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (timerState.completedAlertPending) IncomeEmerald else Color(0xFF0077B6),
                    modifier = Modifier.testTag("post_meal_timer_display")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    3600 to "1h (60 min)",
                    5400 to "1h30 (90 min)",
                    7200 to "2h (120 min)"
                ).forEach { (secs, label) ->
                    FilterChip(
                        selected = selectedDurationSeconds == secs,
                        onClick = { onSelectDuration(secs) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            // Grade uniforme 2x2 para as 4 refeições (Café da Manhã, Almoço, Lanche da Tarde, Jantar)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthCatalog.mealOptions.chunked(2).forEach { rowMeals ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMeals.forEach { meal ->
                            FilterChip(
                                selected = selectedMeal == meal,
                                onClick = { onSelectMeal(meal) },
                                label = {
                                    Text(
                                        text = meal,
                                        fontWeight = if (selectedMeal == meal) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
                            text = "Iniciar Cronômetro",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
                    onClick = onTriggerAlertNow,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("test_1h_timer_alert_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Medir Agora", fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = timerState.completedAlertPending || timerState.isRunning) {
                Button(
                    onClick = onLogPostMealGlucoseNow,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_post_meal_glucose_cta_btn")
                ) {
                    Text(
                        text = "🩸 Registrar Glicemia Pós-$selectedMeal Agora",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
    val displayMember = record.memberName.ifBlank { fallbackMemberName }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = statusColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("glucose_record_${record.id}")
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
                        text = "${record.valueMgDl} mg/dL",
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
                            text = record.slot,
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
private fun AddGlucoseRecordDialog(
    initialRecord: GlucoseRecord? = null,
    initialEpochDay: Long,
    initialSlot: String,
    availableSlots: List<String>,
    defaultScope: String,
    memberName: String = "",
    onDismiss: () -> Unit,
    onSave: (
        dateEpochDay: Long,
        timeHHmm: String,
        slot: String,
        valueMgDl: Int,
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
    var valueText by remember(initialRecord) { mutableStateOf(initialRecord?.valueMgDl?.toString() ?: "") }
    var timeText by remember(initialRecord) { mutableStateOf(initialRecord?.timeHHmm ?: DateCalendarUtils.currentTimeHHmm()) }
    var selectedSlot by remember(initialRecord, initialSlot) {
        mutableStateOf(DiabetesSlotCatalog.normalizeSlot(initialRecord?.slot ?: initialSlot))
    }
    var notes by remember(initialRecord) { mutableStateOf(initialRecord?.notes ?: "") }
    var privacyScope by remember(initialRecord, defaultScope) { mutableStateOf(initialRecord?.privacyScope ?: defaultScope) }

    val allSlots = remember(availableSlots) {
        (DiabetesSlotCatalog.allSlots + availableSlots).map { DiabetesSlotCatalog.normalizeSlot(it) }.distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialRecord != null) "🩸 Editar Medição de Glicemia" else "🩸 Registrar Medição de Glicemia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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

                Text("Momento / Slot da Medição:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allSlots.forEach { slot ->
                        FilterChip(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            label = { Text(slot) }
                        )
                    }
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
                    label = { Text("Observações (refeição, sintomas, insulina)") },
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
                        onSave(finalEpoch, timeText, selectedSlot, parsedVal, notes, privacyScope)
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
