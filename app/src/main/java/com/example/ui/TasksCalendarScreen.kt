package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.AgendaLayoutMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import com.example.alarm.TaskRingtoneCatalog
import com.example.alarm.TaskTonePreviewPlayer
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationCatalog
import com.example.data.CustomizationRepository
import com.example.data.DateCalendarUtils
import com.example.data.EnergyLevel
import com.example.data.PrivacyScope
import com.example.data.RoutineRhythm
import com.example.data.SubscriptionBill
import com.example.data.SubtaskItem
import com.example.data.TaskItem
import com.example.data.TaskRecurrence
import com.example.data.formatCurrencyBrl
import com.example.data.formatCurrencyOriginal
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold
import java.util.UUID

/**
 * Representa um item renderizado na Linha do Tempo diária (Time Blocking):
 * - Bloco de Tarefa Agendada (proporcional à duração)
 * - Intervalo Livre / Hora Ociosa (Gap >= 10 minutos, clicável para agendar)
 * - Indicador suave de Turno de Baixa Energia (conforme Mapa de Energia do Perfil)
 */
private sealed interface TimelineNode {
    val startMinutesOfDay: Int
    val key: String

    data class LowEnergyShiftBanner(
        val shiftName: String,
        val periodLabel: String,
        override val startMinutesOfDay: Int
    ) : TimelineNode {
        override val key: String = "low_energy_$shiftName"
    }

    data class ScheduledTaskBlock(
        val task: TaskItem,
        override val startMinutesOfDay: Int,
        val endMinutesOfDay: Int
    ) : TimelineNode {
        override val key: String = "task_block_${task.id}"
    }

    data class FreeGapBlock(
        override val startMinutesOfDay: Int,
        val endMinutesOfDay: Int,
        val gapMinutes: Int
    ) : TimelineNode {
        override val key: String = "gap_${startMinutesOfDay}_$endMinutesOfDay"
    }
}

private fun parseHHmmToMinutes(raw: String, fallbackMinutes: Int): Int {
    val parts = raw.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return fallbackMinutes
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (h.coerceIn(0, 23) * 60 + m.coerceIn(0, 59))
}

private fun formatMinutesToHHmm(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
    val h = clamped / 60
    val m = clamped % 60
    return DateCalendarUtils.formatTime(h, m)
}

private fun formatDurationPt(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksCalendarScreen(
    selectedYearMonth: String,
    selectedEpochDay: Long,
    tasks: List<TaskItem>,
    subscriptions: List<SubscriptionBill>,
    customizationConfig: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onShiftMonth: (Int) -> Unit,
    onSelectEpochDay: (Long) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onToggleTaskEssential: (TaskItem) -> Unit = {},
    onToggleSubtask: (TaskItem, String) -> Unit = { _, _ -> },
    onScheduleInboxTask: (TaskItem, Long, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onPostSubscriptionNow: (SubscriptionBill) -> Unit = {},
    onUpdateSubscriptionRingtone: (SubscriptionBill, String) -> Unit = { _, _ -> },
    onDeleteTask: (TaskItem) -> Unit,
    onOpenNewTaskDialog: () -> Unit,
    onOpenNewTaskWithPreset: (startHour: Int, startMinute: Int, suggestedDuration: Int, isInbox: Boolean, defaultScope: String) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current

    // Modos de visualização da Agenda (Passo 2 + Design Structured):
    // "TIMELINE" = Linha do Tempo Diária Estilo Structured (Completo / Simplificado / Mínimo)
    // "INBOX" = Tarefas sem horário definido (isInbox = true)
    // "CALENDAR" = Calendário Mensal + Lista Completa
    var agendaViewMode by remember { mutableStateOf("TIMELINE") }
    var showMiniCalendarInTimeline by remember { mutableStateOf(false) }
    var showStructuredDatePickerModal by remember { mutableStateOf(false) }
    var showExpandableFiltersAndState by remember { mutableStateOf(false) }
    var showExpandableAppearancePanel by remember { mutableStateOf(false) }
    var showExpandableBillsSection by remember { mutableStateOf(false) }
    var selectedTaskForBottomSheet by remember { mutableStateOf<TaskItem?>(null) }

    var scopeFilter by remember { mutableStateOf("ALL") } // "ALL", PrivacyScope.PERSONAL, PrivacyScope.FAMILY
    // Filtro para separar Contas de Tarefas: "ALL" (Tudo), "TASKS_ONLY" (Só Tarefas), "BILLS_ONLY" (Só Contas)
    var itemTypeFilter by remember { mutableStateOf("ALL") }
    // Filtro por Categoria: "ALL" (Todas) ou nome da categoria ("Finanças", "Casa", "Trabalho", "Família", "Saúde")
    var categoryFilter by remember { mutableStateOf("ALL") }
    var taskToScheduleFromInbox by remember { mutableStateOf<TaskItem?>(null) }
    var billForRingtoneDialog by remember { mutableStateOf<SubscriptionBill?>(null) }

    val accentColor = remember(customizationConfig.agendaColorHex, customizationConfig.profileColorHex) {
        Color(customizationConfig.agendaColorHex)
    }
    val layoutMode = customizationConfig.agendaLayoutMode

    val availableCategories = remember(tasks, subscriptions) {
        val defaults = listOf("Finanças", "Casa", "Trabalho", "Família", "Saúde")
        val fromTasks = tasks.map { it.category.trim() }.filter { it.isNotBlank() }
        val fromSubs = subscriptions.map { it.category.trim() }.filter { it.isNotBlank() }
        (defaults + fromTasks + fromSubs).distinct()
    }

    val calendarCells = remember(selectedYearMonth) {
        DateCalendarUtils.buildMonthCalendarGrid(selectedYearMonth)
    }

    // PASSO 1: daily_state por data YYYY-MM-DD
    val selectedDateIso = remember(selectedEpochDay) {
        DateCalendarUtils.formatIsoDate(selectedEpochDay)
    }
    val (_, _, selectedDayOfMonth) = remember(selectedEpochDay) {
        DateCalendarUtils.fromEpochDay(selectedEpochDay)
    }
    val dailyState = remember(customizationConfig.dailyStatesByDate, selectedDateIso) {
        customizationConfig.dailyStateFor(selectedDateIso)
    }
    val isHardDayActive = customizationConfig.chronicPainFatigueEnabled && dailyState.hardDayModeActive

    // Helper para identificar se uma TaskItem é uma Conta/Pagamento Financeiro ou uma Tarefa de Rotina
    fun isFinancialBillTask(task: TaskItem): Boolean {
        return task.estimatedCostBrl > 0.0 ||
            task.linkedSubscriptionId.isNotBlank() ||
            task.linkedTransactionId.isNotBlank()
    }

    // 1. Filtro de Escopo ("Tudo", "Individual", "Familiar") + Categoria + Tipo (Tarefas vs Contas)
    val scopeFilteredTasks = remember(tasks, scopeFilter, categoryFilter, itemTypeFilter) {
        tasks.filter { task ->
            val matchesScope = when (scopeFilter) {
                PrivacyScope.FAMILY -> task.privacyScope != PrivacyScope.PERSONAL
                PrivacyScope.PERSONAL -> task.privacyScope == PrivacyScope.PERSONAL
                else -> true
            }
            val matchesCategory = categoryFilter == "ALL" ||
                task.category.equals(categoryFilter, ignoreCase = true) ||
                (categoryFilter == "Finanças" && isFinancialBillTask(task))
            val isBill = isFinancialBillTask(task)
            val matchesType = when (itemTypeFilter) {
                "TASKS_ONLY" -> !isBill
                "BILLS_ONLY" -> isBill
                else -> true
            }
            matchesScope && matchesCategory && matchesType
        }
    }

    val activeFiltersCount = remember(scopeFilter, itemTypeFilter, categoryFilter) {
        (if (scopeFilter != "ALL") 1 else 0) +
            (if (itemTypeFilter != "ALL") 1 else 0) +
            (if (categoryFilter != "ALL") 1 else 0)
    }

    // Contadores para os chips de escopo e tipo
    val totalIndividualCount = remember(tasks) {
        tasks.count { it.privacyScope == PrivacyScope.PERSONAL }
    }
    val totalFamilyCount = remember(tasks) {
        tasks.count { it.privacyScope != PrivacyScope.PERSONAL }
    }
    val totalRoutineTasksCount = remember(tasks) {
        tasks.count { !isFinancialBillTask(it) }
    }
    val totalBillsCount = remember(tasks, subscriptions) {
        tasks.count { isFinancialBillTask(it) } + subscriptions.count { it.active }
    }

    // Tarefas da Inbox (sem horário definido: isInbox == true) respeitando os filtros e Modo Dia Difícil
    val inboxTasks = remember(scopeFilteredTasks, isHardDayActive) {
        val rawInbox = scopeFilteredTasks.filter { it.isInbox }
        if (isHardDayActive) {
            rawInbox.filter { it.isEssential || it.priority == "HIGH" || isFinancialBillTask(it) }
        } else {
            rawInbox
        }
    }

    // Tarefas agendadas para o dia selecionado (não estão na Inbox)
    val baseDayScheduledTasks = remember(scopeFilteredTasks, selectedEpochDay) {
        scopeFilteredTasks
            .filter { !it.isInbox && it.dateEpochDay == selectedEpochDay }
            .sortedWith(compareBy<TaskItem> { it.alarmHour }.thenBy { it.alarmMinute })
    }

    // Contas do Financeiro que vencem na data selecionada (respeitando Escopo, Categoria e Tipo)
    val dueDaySubscriptions = remember(subscriptions, selectedDayOfMonth, scopeFilter, categoryFilter, itemTypeFilter) {
        if (itemTypeFilter == "TASKS_ONLY") {
            emptyList()
        } else {
            subscriptions.filter { sub ->
                val matchesScope = when (scopeFilter) {
                    PrivacyScope.FAMILY -> sub.privacyScope != PrivacyScope.PERSONAL
                    PrivacyScope.PERSONAL -> sub.privacyScope == PrivacyScope.PERSONAL
                    else -> true
                }
                val matchesCategory = categoryFilter == "ALL" ||
                    categoryFilter == "Finanças" ||
                    sub.category.equals(categoryFilter, ignoreCase = true)
                sub.active &&
                    sub.dueDayOfMonth.toInt() == selectedDayOfMonth &&
                    matchesScope &&
                    matchesCategory
            }
        }
    }

    // No Modo Dia Difícil, mantém tarefas essenciais OU tarefas vinculadas a contas vencendo no dia
    val dayScheduledTasks = remember(baseDayScheduledTasks, isHardDayActive) {
        if (isHardDayActive) {
            baseDayScheduledTasks.filter { task ->
                val isFinancialDue = isFinancialBillTask(task)
                task.isEssential || task.priority == "HIGH" || isFinancialDue
            }
        } else {
            baseDayScheduledTasks
        }
    }

    // Separação explícita no dia entre Tarefas com Custo/Conta Vinculada vs Tarefas Puras de Rotina
    val dayFinancialTasks = remember(dayScheduledTasks) {
        dayScheduledTasks.filter { isFinancialBillTask(it) }
    }
    val dayRoutineOnlyTasks = remember(dayScheduledTasks) {
        dayScheduledTasks.filter { !isFinancialBillTask(it) }
    }

    val hiddenNonEssentialCount = remember(baseDayScheduledTasks, dayScheduledTasks, isHardDayActive) {
        if (isHardDayActive) (baseDayScheduledTasks.size - dayScheduledTasks.size).coerceAtLeast(0) else 0
    }

    // Início do dia na Linha do Tempo:
    val dayStartMinutes = remember(
        customizationConfig.routineRhythm,
        customizationConfig.fixedWakeTime,
        dailyState.wokeUpAt
    ) {
        if (customizationConfig.routineRhythm == RoutineRhythm.ELASTICA && dailyState.wokeUpAt.isNotBlank()) {
            parseHHmmToMinutes(dailyState.wokeUpAt, 7 * 60)
        } else {
            parseHHmmToMinutes(customizationConfig.fixedWakeTime, 7 * 60)
        }
    }

    val dayEndMinutes = remember(customizationConfig.fixedSleepTime, dayStartMinutes) {
        val parsedEnd = parseHHmmToMinutes(customizationConfig.fixedSleepTime, 22 * 60 + 30)
        if (parsedEnd <= dayStartMinutes + 60) (dayStartMinutes + 8 * 60).coerceAtMost(23 * 60 + 45) else parsedEnd
    }

    // Algoritmo de Linha do Tempo (exclusivo para Tarefas de Rotina, separado das Contas) + Horas Ociosas (Gaps >= 10 minutos)
    val timelineNodes = remember(
        dayRoutineOnlyTasks,
        dayStartMinutes,
        dayEndMinutes,
        customizationConfig.energyMorning,
        customizationConfig.energyAfternoon,
        customizationConfig.energyNight
    ) {
        val nodes = mutableListOf<TimelineNode>()

        if (customizationConfig.energyMorning == EnergyLevel.BAIXA) {
            nodes.add(
                TimelineNode.LowEnergyShiftBanner(
                    shiftName = "Manhã",
                    periodLabel = "06:00 - 12:00",
                    startMinutesOfDay = maxOf(dayStartMinutes, 6 * 60)
                )
            )
        }
        if (customizationConfig.energyAfternoon == EnergyLevel.BAIXA) {
            nodes.add(
                TimelineNode.LowEnergyShiftBanner(
                    shiftName = "Tarde",
                    periodLabel = "12:00 - 18:00",
                    startMinutesOfDay = 12 * 60
                )
            )
        }
        if (customizationConfig.energyNight == EnergyLevel.BAIXA) {
            nodes.add(
                TimelineNode.LowEnergyShiftBanner(
                    shiftName = "Noite",
                    periodLabel = "18:00 - 22:30",
                    startMinutesOfDay = 18 * 60
                )
            )
        }

        val sortedTasks = dayRoutineOnlyTasks.sortedBy { it.alarmHour * 60 + it.alarmMinute }
        var cursorMinutes = dayStartMinutes

        for (task in sortedTasks) {
            val taskStart = (task.alarmHour.toInt().coerceIn(0, 23) * 60) + task.alarmMinute.toInt().coerceIn(0, 59)
            val duration = task.durationMinutes.coerceIn(10, 480)
            val taskEnd = (taskStart + duration).coerceAtMost(23 * 60 + 59)

            if (taskStart - cursorMinutes >= 10) {
                nodes.add(
                    TimelineNode.FreeGapBlock(
                        startMinutesOfDay = cursorMinutes,
                        endMinutesOfDay = taskStart,
                        gapMinutes = taskStart - cursorMinutes
                    )
                )
            }

            nodes.add(
                TimelineNode.ScheduledTaskBlock(
                    task = task,
                    startMinutesOfDay = taskStart,
                    endMinutesOfDay = taskEnd
                )
            )

            if (taskEnd > cursorMinutes) {
                cursorMinutes = taskEnd
            }
        }

        if (dayEndMinutes - cursorMinutes >= 10) {
            nodes.add(
                TimelineNode.FreeGapBlock(
                    startMinutesOfDay = cursorMinutes,
                    endMinutesOfDay = dayEndMinutes,
                    gapMinutes = dayEndMinutes - cursorMinutes
                )
            )
        }

        nodes.sortedWith(
            compareBy<TimelineNode> { it.startMinutesOfDay }
                .thenBy {
                    when (it) {
                        is TimelineNode.LowEnergyShiftBanner -> 0
                        is TimelineNode.FreeGapBlock -> 1
                        is TimelineNode.ScheduledTaskBlock -> 2
                    }
                }
        )
    }

    val totalFreeMinutesToday = remember(timelineNodes) {
        timelineNodes.filterIsInstance<TimelineNode.FreeGapBlock>().sumOf { it.gapMinutes }
    }

    val tasksByDay = remember(scopeFilteredTasks) {
        scopeFilteredTasks.filter { !it.isInbox }.groupBy { it.dateEpochDay }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AlarmScheduler.showImmediateNotification(
                context = context,
                notificationId = 9999,
                title = "Alarme da Agenda Ativado!",
                message = "Seus alarmes personalizados de tarefas estão prontos."
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tasks_calendar_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 156.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. CABEÇALHO CLEAN ESTILO STRUCTURED ("novembro 2026" + Faixa de 7 dias + Botões rápidos)
            item {
                StructuredAgendaHeader(
                    selectedEpochDay = selectedEpochDay,
                    accentColor = accentColor,
                    layoutMode = layoutMode,
                    tasksByDay = tasksByDay,
                    activeFiltersCount = activeFiltersCount,
                    inboxCount = inboxTasks.size,
                    dueBillsCount = dueDaySubscriptions.size + dayFinancialTasks.size,
                    onSelectEpochDay = onSelectEpochDay,
                    onOpenDatePickerModal = { showStructuredDatePickerModal = true },
                    onToggleFiltersPanel = { showExpandableFiltersAndState = !showExpandableFiltersAndState },
                    onOpenAppearanceSheet = { showExpandableAppearancePanel = !showExpandableAppearancePanel },
                    onSwitchToInbox = {
                        agendaViewMode = if (agendaViewMode == "INBOX") "TIMELINE" else "INBOX"
                    }
                )
            }

            // 2. BARRA COMPACTA DE AÇÕES RÁPIDAS & ESTADO DO DIA (Discreta para não sujar a tela)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Acordei discreto quando rotina for Elástica
                    if (customizationConfig.routineRhythm == RoutineRhythm.ELASTICA) {
                        if (dailyState.wokeUpAt.isBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accentColor.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { customizationRepo.recordWokeUp(selectedDateIso) }
                                    .testTag("woke_up_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Acordei",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = IncomeEmerald.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { customizationRepo.resetWokeUpForDate(selectedDateIso) }
                                    .testTag("woke_up_recorded_badge")
                            ) {
                                Text(
                                    text = "☀️ Acordou ${dailyState.wokeUpAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Botão discreto de Modo Dia Difícil se Dores/Fadiga estiver ativo
                    if (customizationConfig.chronicPainFatigueEnabled) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isHardDayActive) ExpenseCoral.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { customizationRepo.toggleHardDayMode(selectedDateIso) }
                                .testTag("hard_day_mode_btn")
                        ) {
                            Text(
                                text = if (isHardDayActive) "💜 Dia Difícil Ativo" else "Dia Difícil",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isHardDayActive) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Chips rápidos de escopo: Tudo | Individual | Familiar
                    listOf(
                        Triple("ALL", "Tudo", "agenda_scope_all"),
                        Triple(PrivacyScope.PERSONAL, "Individual", "agenda_scope_personal"),
                        Triple(PrivacyScope.FAMILY, "Família", "agenda_scope_family")
                    ).forEach { (key, label, tag) ->
                        val selected = scopeFilter == key
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { scopeFilter = key }
                                .testTag(tag)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Chip para abrir/fechar Menu Expansível de Categorias, Tipo (Contas vs Tarefas) e Visão
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (showExpandableFiltersAndState || activeFiltersCount > 0) {
                            accentColor.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showExpandableFiltersAndState = !showExpandableFiltersAndState }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showExpandableFiltersAndState) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (categoryFilter == "ALL") "Filtros & Visão" else "Cat: $categoryFilter",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            // Banner acolhedor caso Modo Dia Difícil esteja ativo
            if (isHardDayActive) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hard_day_welcome_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = ExpenseCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Modo Dia Difícil ativo: ocultamos $hiddenNonEssentialCount tarefa(s) não essencial(is) e congelamos sua sequência (${customizationConfig.currentStreakDays}d ❄️).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. MENU EXPANSÍVEL: APARÊNCIA DA AGENDA (Completo / Simplificado / Mínimo + Cor do App)
            item {
                AnimatedVisibility(visible = showExpandableAppearancePanel) {
                    StructuredAppearanceCard(
                        config = customizationConfig,
                        customizationRepo = customizationRepo
                    )
                }
            }

            // 4. MENU EXPANSÍVEL: FILTROS AVANÇADOS (Separar Contas e Tarefas, Categorias, Estado do Dia e Abas)
            item {
                AnimatedVisibility(visible = showExpandableFiltersAndState) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_state_control_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Opções & Filtros da Agenda",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SavingsGold.copy(alpha = 0.2f),
                                    modifier = Modifier.testTag("daily_streak_badge")
                                ) {
                                    Text(
                                        text = if (isHardDayActive || dailyState.streakFrozen) {
                                            "Sequência: ${customizationConfig.currentStreakDays}d ❄️"
                                        } else {
                                            "Sequência: ${customizationConfig.currentStreakDays}d 🔥"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Separar Contas e Tarefas
                            Text(
                                text = "Separar Contas e Tarefas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = itemTypeFilter == "ALL",
                                    onClick = { itemTypeFilter = "ALL" },
                                    label = { Text("Tudo") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("agenda_type_all")
                                )
                                FilterChip(
                                    selected = itemTypeFilter == "TASKS_ONLY",
                                    onClick = { itemTypeFilter = "TASKS_ONLY" },
                                    label = { Text("📋 Só Tarefas ($totalRoutineTasksCount)") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("agenda_type_tasks_only")
                                )
                                FilterChip(
                                    selected = itemTypeFilter == "BILLS_ONLY",
                                    onClick = { itemTypeFilter = "BILLS_ONLY" },
                                    label = { Text("💳 Só Contas ($totalBillsCount)") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("agenda_type_bills_only")
                                )
                            }

                            // Filtrar por Categoria
                            Text(
                                text = "Categoria",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = categoryFilter == "ALL",
                                    onClick = { categoryFilter = "ALL" },
                                    label = { Text("Todas") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("agenda_category_all")
                                )
                                availableCategories.forEach { cat ->
                                    FilterChip(
                                        selected = categoryFilter.equals(cat, ignoreCase = true),
                                        onClick = { categoryFilter = cat },
                                        label = { Text(cat) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("agenda_category_$cat")
                                    )
                                }
                            }

                            // Alternar modo de visão: Linha do Tempo | Inbox | Grade Mensal
                            Text(
                                text = "Modo de Exibição",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = agendaViewMode == "TIMELINE",
                                    onClick = { agendaViewMode = "TIMELINE" },
                                    label = { Text("Linha do Tempo") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("view_mode_timeline_tab")
                                )
                                FilterChip(
                                    selected = agendaViewMode == "INBOX",
                                    onClick = { agendaViewMode = "INBOX" },
                                    label = { Text("Inbox (${inboxTasks.size})") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("view_mode_inbox_tab")
                                )
                                FilterChip(
                                    selected = agendaViewMode == "CALENDAR",
                                    onClick = { agendaViewMode = "CALENDAR" },
                                    label = { Text("Mês") },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("view_mode_calendar_tab")
                                )
                            }
                        }
                    }
                }
            }

            if (agendaViewMode == "TIMELINE") {
                // 5. MENU EXPANSÍVEL DE CONTAS & PAGAMENTOS DO DIA (Separado das Tarefas de Rotina)
                val totalBillsDueToday = dueDaySubscriptions.size + dayFinancialTasks.size
                if (itemTypeFilter != "TASKS_ONLY" && totalBillsDueToday > 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(cardRadius)
                                )
                                .testTag("expandable_daily_bills_card"),
                            shape = RoundedCornerShape(cardRadius),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExpandableBillsSection = !showExpandableBillsSection }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Contas & Vencimentos de Hoje ($totalBillsDueToday)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.12f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (showExpandableBillsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Expandir ou recolher contas",
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = showExpandableBillsSection) {
                                    Column(
                                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        dueDaySubscriptions.forEach { bill ->
                                            val isPaidThisMonth = bill.lastPostedYearMonth == selectedYearMonth
                                            AgendaFinancialBillCard(
                                                bill = bill,
                                                isPaidThisMonth = isPaidThisMonth,
                                                onMarkPaidNow = { onPostSubscriptionNow(bill) },
                                                onOpenRingtonePicker = { billForRingtoneDialog = bill }
                                            )
                                        }
                                        dayFinancialTasks.forEach { billTask ->
                                            val startMin = billTask.alarmHour.toInt() * 60 + billTask.alarmMinute.toInt()
                                            TimeBlockingTaskCard(
                                                task = billTask,
                                                startMinutes = startMin,
                                                endMinutes = startMin + billTask.durationMinutes,
                                                onToggleCheck = { onToggleTask(billTask) },
                                                onToggleEssential = { onToggleTaskEssential(billTask) },
                                                onToggleSubtask = { subtaskId -> onToggleSubtask(billTask, subtaskId) },
                                                onDelete = { onDeleteTask(billTask) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (itemTypeFilter == "BILLS_ONLY" && totalBillsDueToday == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Nenhuma conta a vencer neste dia para o filtro selecionado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                }

                // 6. LINHA DO TEMPO VISUAL ESTILO STRUCTURED (Container orgânico curvo com nós conectados)
                if (itemTypeFilter != "BILLS_ONLY") {
                    item {
                        Surface(
                            shape = RoundedCornerShape(cardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(cardRadius)
                                )
                                .testTag("structured_timeline_sheet")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp)
                            ) {
                                // Filtramos nós conforme o layoutMode (no MÍNIMO ocultamos FreeGapBlock e LowEnergyShiftBanner)
                                val visibleNodes = remember(timelineNodes, layoutMode) {
                                    when (layoutMode) {
                                        AgendaLayoutMode.MINIMO -> timelineNodes.filterIsInstance<TimelineNode.ScheduledTaskBlock>()
                                        AgendaLayoutMode.SIMPLIFICADO -> timelineNodes.filter { it !is TimelineNode.LowEnergyShiftBanner }
                                        else -> timelineNodes
                                    }
                                }

                                val scheduledTaskNodes = remember(visibleNodes) {
                                    visibleNodes.filterIsInstance<TimelineNode.ScheduledTaskBlock>()
                                }

                                if (scheduledTaskNodes.isEmpty()) {
                                    // Empty state clean inside the Structured sheet
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Text(
                                            text = "Comece a estruturar seu dia",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Você tem ${formatDurationPt(totalFreeMinutesToday)} livres hoje. Toque no botão + para adicionar sua primeira tarefa.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    visibleNodes.forEachIndexed { idx, node ->
                                        when (node) {
                                            is TimelineNode.LowEnergyShiftBanner -> {
                                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                                    LowEnergyShiftIndicatorCard(
                                                        shiftName = node.shiftName,
                                                        periodLabel = node.periodLabel
                                                    )
                                                }
                                            }

                                            is TimelineNode.FreeGapBlock -> {
                                                // Exibir intervalo livre apenas entre tarefas ou após a primeira tarefa para visual limpo estilo Structured
                                                StructuredFreeGapRow(
                                                    startMinutesOfDay = node.startMinutesOfDay,
                                                    endMinutesOfDay = node.endMinutesOfDay,
                                                    gapMinutes = node.gapMinutes,
                                                    accentColor = accentColor,
                                                    layoutMode = layoutMode,
                                                    onAddInGap = {
                                                        val startH = node.startMinutesOfDay / 60
                                                        val startM = node.startMinutesOfDay % 60
                                                        val suggestedDur = when {
                                                            node.gapMinutes >= 60 -> 60
                                                            node.gapMinutes >= 45 -> 45
                                                            node.gapMinutes >= 30 -> 30
                                                            else -> 15
                                                        }
                                                        val defaultScope = if (scopeFilter == PrivacyScope.PERSONAL) {
                                                            PrivacyScope.PERSONAL
                                                        } else {
                                                            PrivacyScope.FAMILY
                                                        }
                                                        onOpenNewTaskWithPreset(startH, startM, suggestedDur, false, defaultScope)
                                                    }
                                                )
                                            }

                                            is TimelineNode.ScheduledTaskBlock -> {
                                                val firstTaskIndex = visibleNodes.indexOfFirst { it is TimelineNode.ScheduledTaskBlock }
                                                val lastTaskIndex = visibleNodes.indexOfLast { it is TimelineNode.ScheduledTaskBlock }
                                                val nextNode = visibleNodes.getOrNull(idx + 1)

                                                StructuredTimelineTaskRow(
                                                    task = node.task,
                                                    startMinutesOfDay = node.startMinutesOfDay,
                                                    endMinutesOfDay = node.endMinutesOfDay,
                                                    accentColor = accentColor,
                                                    layoutMode = layoutMode,
                                                    isFirstNode = idx == firstTaskIndex,
                                                    isLastNode = idx == lastTaskIndex && nextNode !is TimelineNode.FreeGapBlock,
                                                    nextNodeIsGap = nextNode is TimelineNode.FreeGapBlock,
                                                    onTaskClick = { clickedTask ->
                                                        selectedTaskForBottomSheet = clickedTask
                                                    },
                                                    onToggleTask = { toggledTask ->
                                                        onToggleTask(toggledTask)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (agendaViewMode == "INBOX") {
                // 7. ABA / SEÇÃO INBOX (Tarefas sem horário definido: isInbox == true)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cardRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
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
                                    text = "📥 Inbox • Sem Horário Definido",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toque em '+ Agendar' em qualquer item para encaixá-lo na Linha do Tempo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val defaultScope = if (scopeFilter == PrivacyScope.PERSONAL) {
                                        PrivacyScope.PERSONAL
                                    } else {
                                        PrivacyScope.FAMILY
                                    }
                                    onOpenNewTaskWithPreset(9, 0, 30, true, defaultScope)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("add_inbox_task_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Inbox", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (inboxTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(cardRadius),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sua Inbox está limpa!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Capture tarefas sem horário definido aqui para agendá-las depois.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(inboxTasks, key = { "inbox_${it.id}" }) { inboxTask ->
                        InboxTaskCard(
                            task = inboxTask,
                            onScheduleClick = { taskToScheduleFromInbox = inboxTask },
                            onToggleCheck = { onToggleTask(inboxTask) },
                            onToggleSubtask = { subtaskId -> onToggleSubtask(inboxTask, subtaskId) },
                            onDelete = { onDeleteTask(inboxTask) }
                        )
                    }
                }
            } else {
                // 8. MODO CALENDÁRIO MENSAL COMPLETO + LISTA DO MÊS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cardRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            MonthlyCalendarGridContent(
                                selectedYearMonth = selectedYearMonth,
                                selectedEpochDay = selectedEpochDay,
                                calendarCells = calendarCells,
                                tasksByDay = tasksByDay,
                                subscriptions = subscriptions,
                                scopeFilter = scopeFilter,
                                onShiftMonth = onShiftMonth,
                                onSelectEpochDay = {
                                    onSelectEpochDay(it)
                                    agendaViewMode = "TIMELINE"
                                }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compromissos de ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    TaskTonePreviewPlayer.playPreview("SUAVE")
                                    AlarmScheduler.showImmediateNotification(
                                        context = context,
                                        notificationId = 8888,
                                        title = "Alarme da Agenda",
                                        message = "Teste de alarme personalizado concluído com sucesso!"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_alarm_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testar Toque", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                val monthTasks = scopeFilteredTasks.filter { it.yearMonth == selectedYearMonth && !it.isInbox }
                if (monthTasks.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhuma tarefa agendada neste mês para o filtro selecionado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(monthTasks, key = { "cal_${it.id}" }) { task ->
                        val startMin = task.alarmHour.toInt() * 60 + task.alarmMinute.toInt()
                        TimeBlockingTaskCard(
                            task = task,
                            startMinutes = startMin,
                            endMinutes = startMin + task.durationMinutes,
                            onToggleCheck = { onToggleTask(task) },
                            onToggleEssential = { onToggleTaskEssential(task) },
                            onToggleSubtask = { subtaskId -> onToggleSubtask(task, subtaskId) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }

        // Botão Circular "+" Flutuante Estilo Structured no canto inferior direito (empilhado abaixo do Microfone)
        FloatingActionButton(
            onClick = {
                val defaultScope = if (scopeFilter == PrivacyScope.PERSONAL) PrivacyScope.PERSONAL else PrivacyScope.FAMILY
                val isInboxDefault = (agendaViewMode == "INBOX")
                onOpenNewTaskWithPreset(9, 0, 30, isInboxDefault, defaultScope)
            },
            shape = CircleShape,
            containerColor = accentColor,
            contentColor = Color(0xFF18191C),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp)
                .testTag("add_task_header_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nova Tarefa",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Modal Structured "Selecionar data" (Screenshot 5)
    if (showStructuredDatePickerModal) {
        StructuredDatePickerDialog(
            selectedEpochDay = selectedEpochDay,
            accentColor = accentColor,
            onDismiss = { showStructuredDatePickerModal = false },
            onConfirmEpochDay = { pickedEpochDay ->
                onSelectEpochDay(pickedEpochDay)
            }
        )
    }

    // Bottom Sheet Structured de Detalhes da Tarefa (Screenshot 3)
    selectedTaskForBottomSheet?.let { activeTask ->
        // Sincroniza estado atualizado da tarefa na lista principal
        val liveTask = tasks.firstOrNull { it.id == activeTask.id } ?: activeTask
        StructuredTaskDetailBottomSheet(
            task = liveTask,
            accentColor = accentColor,
            onDismiss = { selectedTaskForBottomSheet = null },
            onToggleTask = { onToggleTask(it) },
            onToggleSubtask = { taskObj, subId -> onToggleSubtask(taskObj, subId) },
            onToggleEssential = { onToggleTaskEssential(it) },
            onDeleteTask = { onDeleteTask(it) }
        )
    }

    // Modal rápido para agendar item da Inbox na Linha do Tempo
    taskToScheduleFromInbox?.let { task ->
        ScheduleInboxTaskDialog(
            task = task,
            selectedEpochDay = selectedEpochDay,
            onDismiss = { taskToScheduleFromInbox = null },
            onConfirmSchedule = { epochDay, hour, minute, duration ->
                onScheduleInboxTask(task, epochDay, hour, minute, duration)
                taskToScheduleFromInbox = null
                agendaViewMode = "TIMELINE"
            }
        )
    }

    // Modal para configurar Ringtone próprio de lembrete de Conta do Financeiro
    billForRingtoneDialog?.let { bill ->
        BillRingtonePickerDialog(
            bill = bill,
            onDismiss = { billForRingtoneDialog = null },
            onSaveRingtone = { newRingtone ->
                onUpdateSubscriptionRingtone(bill, newRingtone)
                billForRingtoneDialog = null
            }
        )
    }
}

@Composable
private fun LowEnergyShiftIndicatorCard(
    shiftName: String,
    periodLabel: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF7C4DFF).copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF7C4DFF).copy(alpha = 0.28f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("low_energy_banner_$shiftName")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = Color(0xFF7C4DFF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Turno de Baixa Energia ($shiftName • $periodLabel) • Priorize tarefas leves ou descanso",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF5E35B1),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FreeTimeGapCard(
    startMinutes: Int,
    endMinutes: Int,
    gapMinutes: Int,
    onClickAddInGap: () -> Unit
) {
    val startLabel = formatMinutesToHHmm(startMinutes)
    val endLabel = formatMinutesToHHmm(endMinutes)
    val durationLabel = formatDurationPt(gapMinutes)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClickAddInGap() }
            .testTag("free_gap_block_$startMinutes")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$durationLabel livres • $startLabel - $endLabel",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Janela ociosa detectada • Toque para encaixar tarefa neste horário",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Adicionar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgendaFinancialBillCard(
    bill: SubscriptionBill,
    isPaidThisMonth: Boolean,
    onMarkPaidNow: () -> Unit,
    onOpenRingtonePicker: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val isPersonal = bill.privacyScope == PrivacyScope.PERSONAL
    val ringtoneOption = remember(bill.ringtoneCategory) {
        TaskRingtoneCatalog.findById(bill.ringtoneCategory)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (isPaidThisMonth) IncomeEmerald.copy(alpha = 0.5f) else SavingsGold,
                shape = RoundedCornerShape(cardRadius)
            )
            .testTag("agenda_bill_card_${bill.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaidThisMonth) {
                IncomeEmerald.copy(alpha = 0.10f)
            } else {
                SavingsGold.copy(alpha = 0.14f)
            }
        )
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
                    Text(
                        text = "💳",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isPaidThisMonth) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = "${formatCurrencyOriginal(bill.amountOriginal, bill.currency)} • Vence dia ${bill.dueDayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaidThisMonth) IncomeEmerald else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isPaidThisMonth) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = IncomeEmerald.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "Paga no Financeiro ✓",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeEmerald,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onMarkPaidNow,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("pay_bill_agenda_btn_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dar Baixa / Pagar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Badge Essencial Automático
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SavingsGold.copy(alpha = 0.28f)
                ) {
                    Text(
                        text = "⭐ Essencial (Conta do Dia)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Badge Escopo: Família vs Individual
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (isPersonal) "🔒 Individual" else "👨‍👩‍👧‍👦 Família",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Seletor e Preview de Ringtone da Conta
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            TaskTonePreviewPlayer.playPreview(ringtoneOption.id)
                            onOpenRingtonePicker()
                        }
                        .testTag("bill_ringtone_chip_${bill.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Som: ${ringtoneOption.categoryLabel} (Tocar / Trocar)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeBlockingTaskCard(
    task: TaskItem,
    startMinutes: Int,
    endMinutes: Int,
    onToggleCheck: () -> Unit,
    onToggleEssential: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val isPersonal = task.privacyScope == PrivacyScope.PERSONAL
    val hasFinanceCost = task.estimatedCostBrl > 0.0 || task.linkedSubscriptionId.isNotBlank() || task.linkedTransactionId.isNotBlank()
    var subtasksExpanded by remember { mutableStateOf(task.subtasks.isNotEmpty()) }

    val rawColorHex = if (task.taskColorHex != 0L) task.taskColorHex else task.assignedMemberColorHex
    val blockColor = remember(rawColorHex) {
        if (rawColorHex != 0L) Color(rawColorHex.toInt()) else Color(0xFF0E7C5B)
    }

    // Altura visual proporcional à duração (mínimo 88.dp, escalando até 180.dp para blocos longos)
    val proportionalMinHeight = remember(task.durationMinutes) {
        (76 + (task.durationMinutes.coerceIn(15, 180) * 0.45f)).dp
    }

    val ringtoneOption = remember(task.ringtoneCategory) {
        TaskRingtoneCatalog.findById(task.ringtoneCategory)
    }

    val completedSubtasksCount = remember(task.subtasks) {
        task.subtasks.count { it.done }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = proportionalMinHeight)
            .border(
                width = if (!isPersonal) 2.dp else 1.5.dp,
                color = blockColor.copy(alpha = if (task.completed) 0.28f else 0.70f),
                shape = RoundedCornerShape(cardRadius)
            )
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                blockColor.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coluna de Horário & Duração (Time Blocking)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(blockColor.copy(alpha = 0.16f))
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = formatMinutesToHHmm(startMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = blockColor
                    )
                    Text(
                        text = formatMinutesToHHmm(endMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = blockColor
                    ) {
                        Text(
                            text = formatDurationPt(task.durationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Ícone + Título + Notas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleCheck() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.iconEmoji.ifBlank { "📋" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.completed) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }

                IconButton(
                    onClick = onToggleCheck,
                    modifier = Modifier.testTag("toggle_task_check_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (task.completed) "Concluída" else "Concluir tarefa",
                        tint = if (task.completed) IncomeEmerald else blockColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_task_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir tarefa",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Badges de Escopo (Família vs Individual), Subtarefas (0/3), Essencial, Recorrência, Ringtone e Financeiro
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Diferenciação visual clara de cards Familiares vs Individuais na Linha do Tempo
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        blockColor.copy(alpha = 0.22f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPersonal) Icons.Default.Lock else Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isPersonal) MaterialTheme.colorScheme.onTertiaryContainer else blockColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersonal) "Individual" else "Família • ${task.assignedMemberName.ifBlank { "Compartilhada" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPersonal) MaterialTheme.colorScheme.onTertiaryContainer else blockColor
                        )
                    }
                }

                // Badge de Categoria da Tarefa / Conta
                if (task.category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "🏷️ ${task.category}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Contador de Subtarefas ex: 1/3 (clicável para expandir/recolher checklist)
                if (task.subtasks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (completedSubtasksCount == task.subtasks.size) {
                            IncomeEmerald.copy(alpha = 0.20f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { subtasksExpanded = !subtasksExpanded }
                            .testTag("subtask_counter_badge_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (completedSubtasksCount == task.subtasks.size) {
                                    IncomeEmerald
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Subtarefas: $completedSubtasksCount/${task.subtasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (completedSubtasksCount == task.subtasks.size) {
                                    IncomeEmerald
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }

                // Badge Essencial / Inegociável (visível no Modo Dia Difícil)
                val isTaskEssential = task.isEssential || task.priority == "HIGH"
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isTaskEssential) {
                        SavingsGold.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleEssential() }
                        .testTag("toggle_essential_badge_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTaskEssential) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isTaskEssential) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTaskEssential) "Essencial" else "Flexível",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isTaskEssential) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTaskEssential) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badge de Recorrência
                if (task.recurrence != TaskRecurrence.ONCE) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = TaskRecurrence.labelPt(task.recurrence),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Badge de Ringtone Individual (toque para ouvir o preview!)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { TaskTonePreviewPlayer.playPreview(ringtoneOption.id) }
                        .testTag("task_ringtone_preview_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Ouvir ringtone",
                            tint = blockColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ringtoneOption.categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = blockColor
                        )
                    }
                }

                // Badge Financeiro (quando integrado com Finanças)
                if (hasFinanceCost) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (task.completed) IncomeEmerald.copy(alpha = 0.16f) else SavingsGold.copy(alpha = 0.22f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (task.completed) IncomeEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val costText = if (task.estimatedCostBrl > 0.0) formatCurrencyBrl(task.estimatedCostBrl) else "Finanças"
                            Text(
                                text = if (task.completed) "$costText • Pago ✓" else "$costText • Dar Baixa ao Concluir",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (task.completed) IncomeEmerald else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Checklist interativo de Subtarefas
            AnimatedVisibility(visible = subtasksExpanded && task.subtasks.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        task.subtasks.forEach { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleSubtask(st.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = st.done,
                                    onCheckedChange = { onToggleSubtask(st.id) },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = st.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (st.done) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (st.done) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InboxTaskCard(
    task: TaskItem,
    onScheduleClick: () -> Unit,
    onToggleCheck: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val isPersonal = task.privacyScope == PrivacyScope.PERSONAL
    val rawColorHex = if (task.taskColorHex != 0L) task.taskColorHex else task.assignedMemberColorHex
    val blockColor = remember(rawColorHex) {
        if (rawColorHex != 0L) Color(rawColorHex.toInt()) else Color(0xFF0E7C5B)
    }
    val completedSubtasks = task.subtasks.count { it.done }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = blockColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(cardRadius)
            )
            .testTag("inbox_task_card_${task.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Text(
                        text = task.iconEmoji.ifBlank { "📥" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (task.notes.isNotBlank()) {
                            Text(
                                text = task.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botão rápido "+" para agendar na Linha do Tempo
                Button(
                    onClick = onScheduleClick,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("schedule_inbox_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agendar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        blockColor.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = if (isPersonal) "🔒 Individual" else "👨‍👩‍👧‍👦 Família",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPersonal) MaterialTheme.colorScheme.onTertiaryContainer else blockColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Duração estimada: ${formatDurationPt(task.durationMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (task.subtasks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Subtarefas: $completedSubtasks/${task.subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleCheck,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Concluir",
                        tint = if (task.completed) IncomeEmerald else blockColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyCalendarGridContent(
    selectedYearMonth: String,
    selectedEpochDay: Long,
    calendarCells: List<Long?>,
    tasksByDay: Map<Long, List<TaskItem>>,
    subscriptions: List<SubscriptionBill>,
    scopeFilter: String,
    onShiftMonth: (Int) -> Unit,
    onSelectEpochDay: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onShiftMonth(-1) },
                modifier = Modifier.testTag("calendar_prev_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mês anterior"
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ) {
                Text(
                    text = DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            IconButton(
                onClick = { onShiftMonth(1) },
                modifier = Modifier.testTag("calendar_next_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Próximo mês"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DateCalendarUtils.weekdayNamesShort().forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val rows = calendarCells.chunked(7)
        val todayEpoch = DateCalendarUtils.todayEpochDay()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until 7) {
                        val cellEpoch = week.getOrNull(i)
                        if (cellEpoch == null) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                        } else {
                            val (_, _, dayNumber) = DateCalendarUtils.fromEpochDay(cellEpoch)
                            val dayTasks = tasksByDay[cellEpoch].orEmpty()
                            val hasPendingTask = dayTasks.any { !it.completed }
                            val pendingMemberColorHex = dayTasks.firstOrNull { !it.completed }?.taskColorHex
                                ?: dayTasks.firstOrNull()?.assignedMemberColorHex
                            val hasCompletedTask = dayTasks.isNotEmpty() && dayTasks.all { it.completed }
                            val hasBillDue = dayTasks.any {
                                !it.completed && (it.estimatedCostBrl > 0.0 || it.linkedSubscriptionId.isNotBlank() || it.linkedTransactionId.isNotBlank())
                            } || subscriptions.any {
                                it.active && it.dueDayOfMonth.toInt() == dayNumber &&
                                    (scopeFilter == "ALL" || it.privacyScope == scopeFilter)
                            }
                            val isSelected = cellEpoch == selectedEpochDay
                            val isToday = cellEpoch == todayEpoch

                            CalendarDayCell(
                                modifier = Modifier.weight(1f),
                                dayNumber = dayNumber,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasPendingTask = hasPendingTask,
                                pendingMemberColorHex = pendingMemberColorHex,
                                hasCompletedTask = hasCompletedTask,
                                hasBillDue = hasBillDue,
                                onClick = { onSelectEpochDay(cellEpoch) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendDotItem(color = MaterialTheme.colorScheme.primary, label = "Cor da Tarefa")
            LegendDotItem(color = SavingsGold, label = "Conta a Vencer")
            LegendDotItem(color = IncomeEmerald, label = "Concluída / Paga ✓")
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasPendingTask: Boolean,
    pendingMemberColorHex: Long?,
    hasCompletedTask: Boolean,
    hasBillDue: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val taskDotColor = remember(pendingMemberColorHex, isSelected) {
        if (isSelected) {
            Color.White
        } else if (pendingMemberColorHex != null && pendingMemberColorHex != 0L) {
            Color(pendingMemberColorHex.toInt())
        } else {
            ExpenseCoral
        }
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .testTag("calendar_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPendingTask) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(taskDotColor)
                    )
                }
                if (hasBillDue) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SavingsGold)
                    )
                }
                if (hasCompletedTask) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else IncomeEmerald)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleInboxTaskDialog(
    task: TaskItem,
    selectedEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirmSchedule: (epochDay: Long, hour: Int, minute: Int, durationMinutes: Int) -> Unit
) {
    var chosenHour by remember { mutableIntStateOf(task.alarmHour.toInt().coerceIn(6, 22)) }
    var chosenMinute by remember { mutableIntStateOf(task.alarmMinute.toInt().coerceIn(0, 59)) }
    var chosenDuration by remember { mutableIntStateOf(task.durationMinutes.coerceIn(15, 240)) }

    val timePresets = listOf(8 to 0, 9 to 30, 11 to 0, 14 to 0, 16 to 0, 19 to 0)
    val durationPresets = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agendar na Linha do Tempo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${task.iconEmoji} ${task.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Data: ${DateCalendarUtils.formatEpochDayPt(selectedEpochDay)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Escolha o Horário Inicial:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    timePresets.forEach { (h, m) ->
                        FilterChip(
                            selected = chosenHour == h && chosenMinute == m,
                            onClick = {
                                chosenHour = h
                                chosenMinute = m
                            },
                            label = {
                                Text(
                                    text = DateCalendarUtils.formatTime(h, m),
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        )
                    }
                }

                Text(
                    text = "Duração do Bloco:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationPresets.forEach { dur ->
                        FilterChip(
                            selected = chosenDuration == dur,
                            onClick = { chosenDuration = dur },
                            label = { Text(formatDurationPt(dur)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSchedule(selectedEpochDay, chosenHour, chosenMinute, chosenDuration)
                }
            ) {
                Text("Encaixar na Linha do Tempo")
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
private fun BillRingtonePickerDialog(
    bill: SubscriptionBill,
    onDismiss: () -> Unit,
    onSaveRingtone: (String) -> Unit
) {
    var selectedRingtone by remember { mutableStateOf(bill.ringtoneCategory.ifBlank { "CONTAS" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Som do Lembrete da Conta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Conta: ${bill.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TaskRingtoneCatalog.allOptions.forEach { option ->
                    val isSelected = selectedRingtone == option.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedRingtone = option.id
                                TaskTonePreviewPlayer.playPreview(option.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${option.categoryLabel} • ${option.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedRingtone = option.id
                                    TaskTonePreviewPlayer.playPreview(option.id)
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveRingtone(selectedRingtone) }) {
                Text("Salvar Ringtone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * PASSO 2 - FORMULÁRIO DE TAREFA COMPLETO COM RINGTONES INDIVIDUAIS, SUBTAREFAS, DURAÇÃO,
 * ESCOPO INDIVIDUAL/FAMILIAR, OPÇÃO DE INBOX E PREVIEW DE ÁUDIO SINTETIZADO EM TEMPO REAL.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    initialEpochDay: Long,
    defaultProfileColorHex: Long = 0xFF0E7C5B,
    initialStartHour: Int = 9,
    initialStartMinute: Int = 0,
    initialDurationMinutes: Int = 30,
    initialIsInbox: Boolean = false,
    initialPrivacyScope: String = PrivacyScope.FAMILY,
    onDismiss: () -> Unit,
    onSaveTask: (
        title: String,
        notes: String,
        category: String,
        priority: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        alarmEnabled: Boolean,
        privacyScope: String,
        estimatedCostBrl: Double,
        autoCreateExpenseOnComplete: Boolean,
        isEssential: Boolean,
        isInbox: Boolean,
        durationMinutes: Int,
        iconEmoji: String,
        taskColorHex: Long,
        recurrence: String,
        subtasks: List<SubtaskItem>,
        ringtoneCategory: String
    ) -> Unit
) {
    val context = LocalContext.current
    val (initYear, initMonth, initDay) = remember(initialEpochDay) {
        DateCalendarUtils.fromEpochDay(initialEpochDay)
    }
    val maxDaysInMonth = remember(initYear, initMonth) {
        DateCalendarUtils.daysInMonth(initYear, initMonth)
    }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Finanças") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var privacyScope by remember { mutableStateOf(initialPrivacyScope) }
    var isEssential by remember { mutableStateOf(false) }

    // Ícone e Cor da Tarefa
    val iconOptions = remember { listOf("📋", "📊", "💳", "🛒", "🏋️", "🩺", "🏡", "📚", "🧘", "💼", "🍎", "🚗") }
    var selectedIconEmoji by remember { mutableStateOf("📋") }
    var selectedColorHex by remember { mutableLongStateOf(defaultProfileColorHex) }

    // Data / Hora (se sem horário definido -> vai para Inbox)
    var sendToInbox by remember { mutableStateOf(initialIsInbox) }
    var dueDayText by remember { mutableStateOf(initDay.toString()) }
    var alarmHourText by remember { mutableStateOf(initialStartHour.toString().padStart(2, '0')) }
    var alarmMinuteText by remember { mutableStateOf(initialStartMinute.toString().padStart(2, '0')) }
    var alarmEnabled by remember { mutableStateOf(true) }

    // Duração (chips rápidos: 15m, 30m, 45m, 1h, 1h30, custom)
    val durationPresets = remember {
        listOf(
            15 to "15m",
            30 to "30m",
            45 to "45m",
            60 to "1h",
            90 to "1h30"
        )
    }
    var selectedDurationMinutes by remember { mutableIntStateOf(initialDurationMinutes) }
    var isCustomDuration by remember { mutableStateOf(initialDurationMinutes !in listOf(15, 30, 45, 60, 90)) }
    var customDurationText by remember { mutableStateOf(initialDurationMinutes.toString()) }

    // Recorrência (única, diária, semanal, quinzenal, mensal)
    var selectedRecurrence by remember { mutableStateOf(TaskRecurrence.ONCE) }

    // Subtarefas (checklist)
    val subtasksList = remember { mutableStateListOf<SubtaskItem>() }
    var newSubtaskTitle by remember { mutableStateOf("") }

    // Seletor de Ringtone (Som do Alerta) por tarefa com Preview em tempo real
    var selectedRingtoneCategory by remember { mutableStateOf("SUAVE") }

    // Integração Financeira
    var hasFinancialCost by remember { mutableStateOf(false) }
    var costAmountText by remember { mutableStateOf("") }
    var autoCreateExpense by remember { mutableStateOf(true) }

    val categories = listOf("Finanças", "Casa", "Trabalho", "Família", "Saúde")
    val timePresets = listOf(8 to 0, 9 to 0, 11 to 30, 14 to 0, 16 to 30, 19 to 0)

    val chosenDay = (dueDayText.toIntOrNull() ?: initDay).coerceIn(1, maxDaysInMonth)
    val resolvedEpochDay = DateCalendarUtils.toEpochDay(initYear, initMonth, chosenDay)
    val resolvedHour = (alarmHourText.toIntOrNull() ?: initialStartHour).coerceIn(0, 23)
    val resolvedMinute = (alarmMinuteText.toIntOrNull() ?: initialStartMinute).coerceIn(0, 59)
    val resolvedDuration = if (isCustomDuration) {
        (customDurationText.toIntOrNull() ?: 30).coerceIn(5, 480)
    } else {
        selectedDurationMinutes.coerceIn(5, 480)
    }

    val parsedCostBrl = if (hasFinancialCost) {
        costAmountText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    } else {
        0.0
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (sendToInbox) "Nova Tarefa na Inbox 📥" else "Nova Tarefa na Linha do Tempo ⏱️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da Tarefa ou Conta") },
                    placeholder = { Text("Ex: Revisão financeira / Academia / Pagar luz") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // 2. Escopo ("Individual - só meu" ou "Familiar - compartilhado")
                Text(
                    text = "Escopo de Privacidade",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = privacyScope == PrivacyScope.PERSONAL,
                        onClick = { privacyScope = PrivacyScope.PERSONAL },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Individual - só meu") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_scope_personal_chip")
                    )
                    FilterChip(
                        selected = privacyScope == PrivacyScope.FAMILY,
                        onClick = { privacyScope = PrivacyScope.FAMILY },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Familiar - compartilhado") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_scope_family_chip")
                    )
                }

                // 3. Ícone e Cor da Tarefa
                Text(
                    text = "Ícone Visual",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconOptions.forEach { emoji ->
                        val selected = selectedIconEmoji == emoji
                        Surface(
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedIconEmoji = emoji }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                Text(
                    text = "Cor do Bloco na Linha do Tempo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomizationCatalog.memberAgendaColors.forEach { (colorName, hex) ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(hex.toInt()))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    }
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. Data/Hora (opcional; se vazio/Inbox, vai para Inbox)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📥 Enviar para Inbox (Sem horário definido)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (sendToInbox) {
                                        "Ficará na aba Inbox para agendar depois com 1 toque"
                                    } else {
                                        "Será exibida na Linha do Tempo no horário abaixo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = sendToInbox,
                                onCheckedChange = { sendToInbox = it },
                                modifier = Modifier.testTag("task_send_to_inbox_switch")
                            )
                        }

                        AnimatedVisibility(visible = !sendToInbox) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dueDayText,
                                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                                        label = { Text("Dia (1-$maxDaysInMonth)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("task_due_day_input")
                                    )
                                    OutlinedTextField(
                                        value = alarmHourText,
                                        onValueChange = { alarmHourText = it.filter { ch -> ch.isDigit() }.take(2) },
                                        label = { Text("Hora (0-23)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("task_hour_input")
                                    )
                                    OutlinedTextField(
                                        value = alarmMinuteText,
                                        onValueChange = { alarmMinuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                                        label = { Text("Min (0-59)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("task_minute_input")
                                    )
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    timePresets.forEach { (h, m) ->
                                        FilterChip(
                                            selected = resolvedHour == h && resolvedMinute == m,
                                            onClick = {
                                                alarmHourText = h.toString().padStart(2, '0')
                                                alarmMinuteText = m.toString().padStart(2, '0')
                                            },
                                            label = {
                                                Text(
                                                    text = DateCalendarUtils.formatTime(h, m),
                                                    fontFamily = JetBrainsMonoFontFamily
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Duração (chips rápidos: 15m, 30m, 45m, 1h, 1h30, custom)
                Text(
                    text = "Duração do Bloco (${formatDurationPt(resolvedDuration)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationPresets.forEach { (mins, label) ->
                        FilterChip(
                            selected = !isCustomDuration && selectedDurationMinutes == mins,
                            onClick = {
                                isCustomDuration = false
                                selectedDurationMinutes = mins
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("duration_chip_$mins")
                        )
                    }
                    FilterChip(
                        selected = isCustomDuration,
                        onClick = { isCustomDuration = true },
                        label = { Text("Custom") },
                        modifier = Modifier.testTag("duration_chip_custom")
                    )
                }
                AnimatedVisibility(visible = isCustomDuration) {
                    OutlinedTextField(
                        value = customDurationText,
                        onValueChange = { customDurationText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Duração personalizada (minutos)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 6. Recorrência (única, diária, semanal, quinzenal, mensal)
                Text(
                    text = "Recorrência",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskRecurrence.allOptions.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedRecurrence == code,
                            onClick = { selectedRecurrence = code },
                            label = { Text(label) }
                        )
                    }
                }

                // 7. Toggle "Tarefa Essencial (visível no Modo Dia Difícil)"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isEssential) {
                        SavingsGold.copy(alpha = 0.20f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⭐ Tarefa Essencial / Inegociável",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Continua visível mesmo quando o Modo Dia Difícil estiver ativado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEssential,
                            onCheckedChange = { isEssential = it },
                            modifier = Modifier.testTag("task_essential_switch")
                        )
                    }
                }

                // 8. Subtarefas (checklist)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "☑️ Subtarefas / Checklist (${subtasksList.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubtaskTitle,
                                onValueChange = { newSubtaskTitle = it },
                                label = { Text("Adicionar passo / subtarefa") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_subtask_input")
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clean = newSubtaskTitle.trim()
                                    if (clean.isNotEmpty()) {
                                        subtasksList.add(
                                            SubtaskItem(
                                                id = "st_" + UUID.randomUUID().toString().take(6),
                                                title = clean,
                                                done = false
                                            )
                                        )
                                        newSubtaskTitle = ""
                                    }
                                },
                                modifier = Modifier.testTag("add_subtask_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar subtarefa")
                            }
                        }

                        subtasksList.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${item.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { subtasksList.removeAt(idx) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover subtarefa",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Seletor de Ringtone (Som do Alerta) por tarefa com botão de "Preview" (Síntese de Áudio em tempo real)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Som do Alerta (Ringtone da Tarefa)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = alarmEnabled,
                                onCheckedChange = { enabled ->
                                    alarmEnabled = enabled
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("task_alarm_switch")
                            )
                        }

                        TaskRingtoneCatalog.allOptions.forEach { option ->
                            val isSelected = selectedRingtoneCategory == option.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedRingtoneCategory = option.id
                                        TaskTonePreviewPlayer.playPreview(option.id)
                                    }
                                    .testTag("ringtone_option_${option.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${option.categoryLabel} • ${option.title}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            selectedRingtoneCategory = option.id
                                            TaskTonePreviewPlayer.playPreview(option.id)
                                        },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("preview_ringtone_btn_${option.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Ouvir Preview",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // 10. Integração Financeira & Notas
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💰 Integrar com Contas / Finanças?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dá baixa e lança o pagamento automaticamente ao concluir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasFinancialCost,
                                onCheckedChange = {
                                    hasFinancialCost = it
                                    if (it) {
                                        isEssential = true
                                        selectedRingtoneCategory = "CONTAS"
                                    }
                                },
                                modifier = Modifier.testTag("task_has_cost_switch")
                            )
                        }

                        AnimatedVisibility(visible = hasFinancialCost) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = costAmountText,
                                    onValueChange = { costAmountText = it },
                                    label = { Text("Valor a pagar (R$)") },
                                    placeholder = { Text("Ex: 250.00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("task_cost_amount_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Lançar gasto nas Finanças ao dar Check ✓",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = autoCreateExpense,
                                        onCheckedChange = { autoCreateExpense = it }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Observações (opcional)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        // Se o usuário digitou uma subtarefa no campo e não clicou em "+", adiciona automaticamente
                        val finalSubtasks = subtasksList.toMutableList()
                        if (newSubtaskTitle.trim().isNotEmpty()) {
                            finalSubtasks.add(
                                SubtaskItem(
                                    id = "st_" + UUID.randomUUID().toString().take(6),
                                    title = newSubtaskTitle.trim(),
                                    done = false
                                )
                            )
                        }
                        onSaveTask(
                            title,
                            notes,
                            category,
                            if (isEssential) "HIGH" else priority,
                            resolvedEpochDay,
                            resolvedHour,
                            resolvedMinute,
                            if (sendToInbox) false else alarmEnabled,
                            privacyScope,
                            parsedCostBrl,
                            hasFinancialCost && autoCreateExpense && parsedCostBrl > 0.0,
                            isEssential || hasFinancialCost,
                            sendToInbox,
                            resolvedDuration,
                            selectedIconEmoji,
                            selectedColorHex,
                            selectedRecurrence,
                            finalSubtasks,
                            selectedRingtoneCategory
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_confirm_btn")
            ) {
                Text(if (sendToInbox) "Salvar na Inbox" else "Salvar na Linha do Tempo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
