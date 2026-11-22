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
    onShowBannerMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val healthState by healthRepo.stateFlow.collectAsStateWithLifecycle()

    val selectedDateIso = remember(selectedEpochDay) {
        DateCalendarUtils.formatIsoDate(selectedEpochDay)
    }

    // Filtro de Escopo na Saúde ("ALL", PrivacyScope.PERSONAL, PrivacyScope.FAMILY)
    var scopeFilter by remember { mutableStateOf("ALL") }
    // Sub-aba das medições clínicas: "GLUCOSE" (Glicemia) ou "BP" (Pressão Arterial)
    var clinicalTab by remember { mutableStateOf("GLUCOSE") }
    // Período selecionado para o Relatório Médico PDF (7, 30 ou 365 dias)
    var pdfPeriodDays by remember { mutableIntStateOf(30) }

    // Modais
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var showAddGlucoseDialog by remember { mutableStateOf(false) }
    var presetGlucoseSlot by remember { mutableStateOf(DiabetesSlotCatalog.JEJUM) }
    var showAddBpDialog by remember { mutableStateOf(false) }
    var showEditDppDialog by remember { mutableStateOf(false) }
    var financeSuggestionToConfirm by remember { mutableStateOf<HealthFinanceSuggestion?>(null) }

    // Cronômetro de 1 Hora Pós-Refeição em tempo real
    val timerState = healthState.postMealTimer
    var selectedMealForTimer by remember(timerState.mealLabel) { mutableStateOf(timerState.mealLabel) }
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
                        title = "⏰ 1 Hora Pós-${timerState.mealLabel} Concluída!",
                        message = "Momento ideal para registrar sua medição de Glicemia pós-refeição."
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

    // Launcher para salvar PDF nativo em local escolhido pelo usuário (CreateDocument)
    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null) {
            val result = MedicalReportPdfGenerator.exportToUserUri(
                context = context,
                targetUri = targetUri,
                profileName = customizationConfig.profileName,
                periodDays = pdfPeriodDays,
                customizationConfig = customizationConfig,
                healthState = healthState
            )
            if (result.success) {
                healthRepo.recordGeneratedPdfInfo(result.filePath, result.summaryText)
                onShowBannerMessage("📄 Relatório Médico em PDF salvo com sucesso! ${result.summaryText}")
            } else {
                onShowBannerMessage(result.summaryText)
            }
        }
    }

    val filteredTrackers = remember(healthState.trackers, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> healthState.trackers.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> healthState.trackers.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> healthState.trackers
        }
    }

    val filteredGlucose = remember(healthState.glucoseRecords, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> healthState.glucoseRecords.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> healthState.glucoseRecords.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> healthState.glucoseRecords
        }
    }

    val filteredBp = remember(healthState.bloodPressureRecords, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> healthState.bloodPressureRecords.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> healthState.bloodPressureRecords.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> healthState.bloodPressureRecords
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
                                if (customizationConfig.diabetesEnabled) add("Glicemia & Timer 1h")
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
                                    profileName = customizationConfig.profileName,
                                    periodDays = pdfPeriodDays,
                                    customizationConfig = customizationConfig,
                                    healthState = healthState
                                )
                                if (localResult.success) {
                                    healthRepo.recordGeneratedPdfInfo(localResult.filePath, localResult.summaryText)
                                    onShowBannerMessage("📄 PDF Médico gerado! ${localResult.summaryText}")
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
                            },
                            onDelete = {
                                healthRepo.deleteTracker(habit.id)
                            }
                        )
                    }
                }
            }
        }

        // 3. CRONÔMETRO / TIMER DE 1 HORA PÓS-REFEIÇÃO (SOMENTE SE DIABETES / GLICEMIA ESTIVER MARCADO)
        if (customizationConfig.diabetesEnabled) {
            item {
                PostMealOneHourTimerCard(
                    timerState = timerState,
                    liveRemainingSeconds = liveRemainingSeconds,
                    selectedMeal = selectedMealForTimer,
                    onSelectMeal = { selectedMealForTimer = it },
                    onStartTimer = {
                        healthRepo.startPostMealTimer(selectedMealForTimer, 3600)
                    },
                    onPauseResumeTimer = {
                        healthRepo.pauseOrResumePostMealTimer()
                    },
                    onResetTimer = {
                        healthRepo.resetPostMealTimer(selectedMealForTimer)
                    },
                    onTriggerAlertNow = {
                        healthRepo.markPostMealTimerFinished()
                        TaskTonePreviewPlayer.playPreview("SUAVE")
                        AlarmScheduler.showImmediateNotification(
                            context = context,
                            notificationId = 7745,
                            title = "⏰ Hora de Medir a Glicemia (1h Pós-$selectedMealForTimer)!",
                            message = "Toque para registrar o valor da sua glicemia pós-refeição."
                        )
                    },
                    onLogPostMealGlucoseNow = {
                        healthRepo.dismissPostMealTimerAlert()
                        presetGlucoseSlot = when {
                            selectedMealForTimer.contains("Café", ignoreCase = true) -> DiabetesSlotCatalog.POS_CAFE_1H
                            selectedMealForTimer.contains("Jantar", ignoreCase = true) -> DiabetesSlotCatalog.POS_JANTAR_1H
                            else -> DiabetesSlotCatalog.POS_ALMOCO_1H
                        }
                        showAddGlucoseDialog = true
                    }
                )
            }
        }

        // 4. MÓDULOS ESPECÍFICOS DE SAÚDE: GLICEMIA (mg/dL) E/OU PRESSÃO ARTERIAL (mmHg) — SOMENTE SE MARCADOS NO QUESTIONÁRIO
        if (customizationConfig.diabetesEnabled || customizationConfig.bloodPressureEnabled) {
            item {
                val activeClinicalMode = when {
                    customizationConfig.diabetesEnabled && customizationConfig.bloodPressureEnabled -> clinicalTab
                    customizationConfig.diabetesEnabled -> "GLUCOSE"
                    else -> "BP"
                }
                val cardTitle = when {
                    customizationConfig.diabetesEnabled && customizationConfig.bloodPressureEnabled ->
                        "🩺 Medições de Glicemia & Pressão Arterial"
                    customizationConfig.diabetesEnabled ->
                        "🩸 Monitoramento de Glicemia Capilar (mg/dL)"
                    else ->
                        "❤️ Monitoramento de Pressão Arterial (mmHg)"
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
                                    text = "Registre com data, hora, valor e observações para o Relatório PDF",
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
                            // Resumo + Botão Registrar Glicemia
                            val avgGlucose = remember(filteredGlucose) {
                                if (filteredGlucose.isNotEmpty()) {
                                    filteredGlucose.map { it.valueMgDl }.average().roundToInt()
                                } else 0
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
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
                                            text = if (avgGlucose > 0) "Média Recente: $avgGlucose mg/dL" else "Sem medições registradas",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Slots ativos: ${customizationConfig.diabetesSlots.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            presetGlucoseSlot = DiabetesSlotCatalog.JEJUM
                                            showAddGlucoseDialog = true
                                        },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("open_add_glucose_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Glicemia")
                                    }
                                }
                            }

                            filteredGlucose.take(8).forEach { record ->
                                GlucoseRecordRowCard(
                                    record = record,
                                    onDelete = { healthRepo.deleteGlucoseRecord(record.id) }
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
                                            text = if (avgSys > 0) "Média Recente: $avgSys/$avgDia mmHg" else "Sem aferições registradas",
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
                                        onClick = { showAddBpDialog = true },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("open_add_bp_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Pressão")
                                    }
                                }
                            }

                            filteredBp.take(8).forEach { record ->
                                BloodPressureRecordRowCard(
                                    record = record,
                                    onDelete = { healthRepo.deleteBloodPressureRecord(record.id) }
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

    if (showAddTrackerDialog) {
        AddCustomTrackerDialog(
            defaultScope = if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL,
            onDismiss = { showAddTrackerDialog = false },
            onSave = { title, emoji, unit, target, step, colorHex, scope ->
                healthRepo.addCustomTracker(title, emoji, unit, target, step, colorHex, scope)
                showAddTrackerDialog = false
            }
        )
    }

    if (showAddGlucoseDialog) {
        AddGlucoseRecordDialog(
            initialEpochDay = selectedEpochDay,
            initialSlot = presetGlucoseSlot,
            availableSlots = customizationConfig.diabetesSlots.toList().ifEmpty { DiabetesSlotCatalog.allSlots },
            defaultScope = if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL,
            onDismiss = { showAddGlucoseDialog = false },
            onSave = { epochDay, timeHHmm, slot, valMgDl, notes, scope ->
                healthRepo.addGlucoseRecord(epochDay, timeHHmm, slot, valMgDl, notes, scope)
                showAddGlucoseDialog = false
                onShowBannerMessage("🩸 Glicemia registrada: $valMgDl mg/dL ($slot)")
            }
        )
    }

    if (showAddBpDialog) {
        AddBloodPressureRecordDialog(
            initialEpochDay = selectedEpochDay,
            defaultScope = if (scopeFilter == PrivacyScope.FAMILY) PrivacyScope.FAMILY else PrivacyScope.PERSONAL,
            onDismiss = { showAddBpDialog = false },
            onSave = { epochDay, timeHHmm, sys, dia, pulse, notes, scope ->
                healthRepo.addBloodPressureRecord(epochDay, timeHHmm, sys, dia, pulse, notes, scope)
                showAddBpDialog = false
                onShowBannerMessage("❤️ Pressão Arterial registrada: $sys/$dia mmHg ($pulse bpm)")
            }
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
                    OutlinedTextField(
                        value = dppInput,
                        onValueChange = { dppInput = it },
                        label = { Text("Data Prevista do Parto - DPP (DD/MM/AAAA)") },
                        placeholder = { Text("Ex: 15/03/2027") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customizationRepo.updatePregnancyModule(pregEnabled, dppInput.trim())
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(34.dp)
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("tracker_inc_${habit.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${habit.stepValue}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }

                    if (!isDone) {
                        IconButton(
                            onClick = onCompleteAll,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Bater meta do dia",
                                tint = IncomeEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
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
    onSelectMeal: (String) -> Unit,
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
                            text = "⏱️ Cronômetro 1h Pós-Refeição",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (timerState.isRunning) {
                                "Contando 1h pós-$selectedMeal (Iniciado às ${timerState.startedAtHHmm})"
                            } else if (timerState.completedAlertPending) {
                                "🔔 1 hora concluída! Hora de medir sua glicemia pós-$selectedMeal"
                            } else {
                                "Inicie ao terminar a refeição para lembrar o momento exato da medição"
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthCatalog.mealOptions.forEach { meal ->
                    FilterChip(
                        selected = selectedMeal == meal,
                        onClick = { onSelectMeal(meal) },
                        label = { Text(meal) },
                        shape = RoundedCornerShape(50)
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
                        Text("Iniciar 1h Pós-Refeição", fontWeight = FontWeight.Bold)
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
                        Text(if (timerState.isPaused) "Retomar" else "Pausar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onResetTimer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("reset_1h_timer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar")
                    }
                }

                OutlinedButton(
                    onClick = onTriggerAlertNow,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("test_1h_timer_alert_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Medir Agora")
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
                        text = "🩸 Registrar Glicemia 1h Pós-$selectedMeal Agora",
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
    onDelete: () -> Unit
) {
    val isAlert = record.isAlert()
    val statusColor = if (isAlert) ExpenseCoral else IncomeEmerald

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = record.statusLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.dateIso} às ${record.timeHHmm}" +
                        if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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

@Composable
private fun BloodPressureRecordRowCard(
    record: BloodPressureRecord,
    onDelete: () -> Unit
) {
    val isAlert = record.isAlert()
    val statusColor = if (isAlert) ExpenseCoral else IncomeEmerald

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = record.statusLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.dateIso} às ${record.timeHHmm}" +
                        if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomTrackerDialog(
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
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("💧") }
    var unit by remember { mutableStateOf("copos") }
    var targetText by remember { mutableStateOf("8") }
    var stepText by remember { mutableStateOf("1") }
    var selectedColorHex by remember { mutableLongStateOf(0xFF0077B6L) }
    var privacyScope by remember { mutableStateOf(defaultScope) }

    val emojiChoices = listOf("💧", "💊", "🚶", "🧘", "🍎", "🏋️", "😴", "🍵", "🦷", "🩸")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Novo Inegociável (Tracker Diário)",
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
                Text("Criar Tracker")
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
    initialEpochDay: Long,
    initialSlot: String,
    availableSlots: List<String>,
    defaultScope: String,
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
    var valueText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf(DateCalendarUtils.currentTimeHHmm()) }
    var selectedSlot by remember { mutableStateOf(initialSlot) }
    var notes by remember { mutableStateOf("") }
    var privacyScope by remember { mutableStateOf(defaultScope) }

    val allSlots = remember(availableSlots) {
        (availableSlots + DiabetesSlotCatalog.allSlots).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🩸 Registrar Medição de Glicemia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Data: ${DateCalendarUtils.formatEpochDayPt(initialEpochDay)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
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
                        onSave(initialEpochDay, timeText, selectedSlot, parsedVal, notes, privacyScope)
                    }
                },
                enabled = parsedVal > 0,
                modifier = Modifier.testTag("confirm_save_glucose_btn")
            ) {
                Text("Salvar Glicemia")
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
    var sysText by remember { mutableStateOf("120") }
    var diaText by remember { mutableStateOf("80") }
    var pulseText by remember { mutableStateOf("72") }
    var timeText by remember { mutableStateOf(DateCalendarUtils.currentTimeHHmm()) }
    var notes by remember { mutableStateOf("") }
    var privacyScope by remember { mutableStateOf(defaultScope) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "❤️ Registrar Pressão Arterial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Data: ${DateCalendarUtils.formatEpochDayPt(initialEpochDay)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
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
                        onSave(initialEpochDay, timeText, sys, dia, pulse, notes, privacyScope)
                    }
                },
                enabled = sys > 0 && dia > 0,
                modifier = Modifier.testTag("confirm_save_bp_btn")
            ) {
                Text("Salvar Pressão")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
