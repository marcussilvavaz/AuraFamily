package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val PtBrLocale = Locale("pt", "BR")

/**
 * Reusable Expandable Section Card to keep screens clean and organized.
 */
@Composable
fun AuraExpandableSectionCard(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    initiallyExpanded: Boolean = false,
    testTag: String = "expandable_section",
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val cardRadius = com.example.ui.theme.LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (expanded) 0.32f else 0.14f),
                shape = organicShape
            )
            .testTag(testTag),
        shape = organicShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.28f),
                                        accentColor.copy(alpha = 0.12f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.10f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.16f))
                    content()
                }
            }
        }
    }
}

/**
 * Resolve Task Icon ImageVector from task iconKey or category/title.
 */
fun resolveStructuredTaskIcon(task: TaskItem): ImageVector {
    val titleLower = task.title.lowercase()
    val emoji = task.iconEmoji
    return when {
        emoji == "⏰" || titleLower.contains("acordar") || titleLower.contains("levantar") -> Icons.Default.AccessAlarm
        emoji == "🍽️" || emoji == "☕" || titleLower.contains("almoço") || titleLower.contains("jantar") || titleLower.contains("café") || titleLower.contains("refeição") -> Icons.Default.Restaurant
        emoji == "💻" || task.category.equals("Trabalho", ignoreCase = true) || titleLower.contains("trabalhar") || titleLower.contains("reunião") -> Icons.Default.LaptopMac
        emoji == "💊" || task.category.equals("Saúde", ignoreCase = true) || titleLower.contains("remédio") || titleLower.contains("vitamina") || titleLower.contains("glicemia") -> Icons.Default.Medication
        emoji == "🏠" || task.category.equals("Casa", ignoreCase = true) || titleLower.contains("casa") || titleLower.contains("limpar") -> Icons.Default.Home
        emoji == "👨‍👩‍👧‍👦" || task.category.equals("Família", ignoreCase = true) -> Icons.Default.FamilyRestroom
        emoji == "💳" || task.category.equals("Finanças", ignoreCase = true) || task.estimatedCostBrl > 0.0 -> Icons.Default.AccountBalanceWallet
        emoji == "🌙" || titleLower.contains("dormir") || titleLower.contains("sono") -> Icons.Default.Bedtime
        emoji == "📞" || titleLower.contains("ligar") -> Icons.Default.Call
        else -> Icons.Default.CheckCircleOutline
    }
}

/**
 * Structured Top Month Header + 7-Day Horizontal Week Strip.
 * Matches screenshots 3, 4, 6, 8:
 * - "novembro " (white/onBackground bold) + "2026" (accentColor bold)
 * - Right side: Calendar modal icon button + Filter/Tools toggle icon + Appearance layout pill
 * - 7-day horizontal strip (seg..dom) with circular selected pill and conditional colored dots in Completo mode.
 */
@Composable
fun StructuredAgendaHeader(
    selectedEpochDay: Long,
    accentColor: Color,
    layoutMode: String,
    tasksByDay: Map<Long, List<TaskItem>>,
    activeFiltersCount: Int,
    inboxCount: Int,
    dueBillsCount: Int,
    onSelectEpochDay: (Long) -> Unit,
    onOpenDatePickerModal: () -> Unit,
    onToggleFiltersPanel: () -> Unit,
    onOpenAppearanceSheet: () -> Unit,
    onSwitchToInbox: () -> Unit
) {
    val selectedDate = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }
    val todayEpoch = remember { DateCalendarUtils.todayEpochDay() }
    val monthName = remember(selectedDate) {
        selectedDate.month.getDisplayName(TextStyle.FULL, PtBrLocale).lowercase(PtBrLocale)
    }
    val yearText = remember(selectedDate) { selectedDate.year.toString() }

    // Compute the 7 days of the week (Monday..Sunday) containing selectedDate
    val weekDates = remember(selectedDate) {
        val monday = selectedDate.with(DayOfWeek.MONDAY)
        (0L..6L).map { monday.plusDays(it) }
    }

    val dayLabels = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("structured_agenda_header"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: "novembro 2026" + action icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("$monthName ")
                    }
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append(yearText)
                    }
                },
                fontSize = 26.sp,
                modifier = Modifier
                    .clickable { onOpenDatePickerModal() }
                    .testTag("structured_month_year_title")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Inbox pill if there are inbox items
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSwitchToInbox() }
                        .testTag("structured_inbox_shortcut_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Inbox",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inbox ($inboxCount)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Layout mode shortcut button (Completo / Simplificado / Mínimo)
                IconButton(
                    onClick = onOpenAppearanceSheet,
                    modifier = Modifier.testTag("structured_appearance_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Aparência e Layout da Agenda",
                        tint = accentColor
                    )
                }

                // Expandable Filters & Tools toggle button
                BadgedBox(
                    badge = {
                        if (activeFiltersCount > 0 || dueBillsCount > 0) {
                            Badge(containerColor = accentColor) {
                                Text("${activeFiltersCount + dueBillsCount}")
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onToggleFiltersPanel,
                        modifier = Modifier.testTag("structured_toggle_filters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtros e Opções da Agenda",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Calendar Date Picker Modal button (matching Structured top-right icon)
                IconButton(
                    onClick = onOpenDatePickerModal,
                    modifier = Modifier.testTag("structured_open_calendar_modal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Selecionar data no calendário",
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 7-Day Week Strip (seg..dom) with swipe/prev-next arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDates.forEachIndexed { idx, date ->
                val epoch = date.toEpochDay()
                val isSelected = epoch == selectedEpochDay
                val isToday = epoch == todayEpoch
                val isWeekend = idx >= 5
                val dayTasks = tasksByDay[epoch].orEmpty()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectEpochDay(epoch) }
                        .padding(vertical = 4.dp)
                        .testTag("week_strip_day_${date.dayOfMonth}")
                ) {
                    Text(
                        text = dayLabels[idx],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (isToday && !isSelected) {
                                    Modifier.border(1.5.dp, accentColor, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.background
                                isToday || isWeekend -> accentColor
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }

                    // In Completo layout mode, show overlapping task indicator dots below each day
                    if (layoutMode == AgendaLayoutMode.COMPLETO) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (dayTasks.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-3).dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(10.dp)
                            ) {
                                dayTasks.take(4).forEach { t ->
                                    val dotColor = if (t.taskColorHex != 0L) Color(t.taskColorHex) else accentColor
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Structured Timeline Task Row (inside the rounded timeline sheet).
 * Matches screenshots 3, 4, 6, 8:
 * - Left time column ("06:30") shown in COMPLETO & SIMPLIFICADO, hidden in MINIMO.
 * - Vertical connecting line (solid above & below).
 * - Node shape:
 *     - Short tasks (<= 30m): 54.dp Circle
 *     - Long tasks (> 30m): Tall vertical rounded pill proportional to duration (e.g., 108.dp - 170.dp)
 *       with start time at top, end time at bottom, and icon centered.
 * - Center column:
 *     - Time range ("06:45 - 08:15 (1h 30m)" or "06:30") + repeat icon (hidden in MINIMO)
 *     - Task Title (17.sp bold, strikethrough when completed)
 *     - Metadata row: subtask counter ("☑️ 0/5") + notes icon ("☰") + family badge (shown in COMPLETO)
 * - Right side:
 *     - 26.dp circular checkbox with 2.dp border in taskColor/accentColor.
 */
@Composable
fun StructuredTimelineTaskRow(
    task: TaskItem,
    startMinutesOfDay: Int,
    endMinutesOfDay: Int,
    accentColor: Color,
    layoutMode: String,
    isFirstNode: Boolean,
    isLastNode: Boolean,
    nextNodeIsGap: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit
) {
    val taskColor = remember(task.taskColorHex, accentColor) {
        if (task.taskColorHex != 0L) Color(task.taskColorHex) else accentColor
    }
    val durationMinutes = (endMinutesOfDay - startMinutesOfDay).coerceAtLeast(15)
    val isLongTask = durationMinutes > 30 && layoutMode != AgendaLayoutMode.MINIMO
    val nodeHeight: Dp = remember(durationMinutes, isLongTask) {
        if (!isLongTask) {
            54.dp
        } else {
            (72 + (durationMinutes - 30).coerceIn(15, 180) * 0.55f).dp
        }
    }

    val startHHmm = remember(startMinutesOfDay) {
        DateCalendarUtils.formatTime(startMinutesOfDay / 60, startMinutesOfDay % 60)
    }
    val endHHmm = remember(endMinutesOfDay) {
        DateCalendarUtils.formatTime(endMinutesOfDay / 60, endMinutesOfDay % 60)
    }
    val durationLabel = remember(durationMinutes) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    val completedSubtasks = remember(task.subtasks) { task.subtasks.count { it.done } }
    val totalSubtasks = task.subtasks.size
    val iconVector = remember(task) { resolveStructuredTaskIcon(task) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) }
            .padding(horizontal = 14.dp)
            .testTag("structured_task_row_${task.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Optional left time label in SIMPLIFICADO/COMPLETO for short tasks or preview alignment
        // Note: In Structured screenshots 4, 6, 8, tall pills show start/end time right next to the vertical line,
        // while the appearance preview shows the time column on the left. We show a clean time column when not a tall pill or in Completo.
        if (layoutMode != AgendaLayoutMode.MINIMO) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = startHHmm,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
                if (isLongTask) {
                    Spacer(modifier = Modifier.height(nodeHeight - 34.dp))
                    Text(
                        text = endHHmm,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        // Timeline Node + Vertical Connecting Line
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(nodeHeight + 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vertical connecting lines above and below the node
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val strokeWidth = 3.dp.toPx()
                if (!isFirstNode) {
                    drawLine(
                        color = taskColor.copy(alpha = 0.75f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height / 2f),
                        strokeWidth = strokeWidth
                    )
                }
                if (!isLastNode) {
                    val pathEffect = if (nextNodeIsGap) {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    } else {
                        null
                    }
                    drawLine(
                        color = taskColor.copy(alpha = 0.75f),
                        start = Offset(centerX, size.height / 2f),
                        end = Offset(centerX, size.height),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )
                }
            }

            // Circle (<= 30m) or Tall Vertical Pill (> 30m)
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(nodeHeight)
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        if (task.completed) taskColor.copy(alpha = 0.45f) else taskColor.copy(alpha = 0.85f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = task.title,
                    tint = Color(0xFF1A1B1E),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Task Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (layoutMode != AgendaLayoutMode.MINIMO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val timeHeaderText = if (durationMinutes > 15) {
                        "$startHHmm - $endHHmm ($durationLabel)"
                    } else {
                        startHHmm
                    }
                    Text(
                        text = timeHeaderText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Recorrente",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                color = if (task.completed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Extra metadata shown in COMPLETO mode (or subtasks count in SIMPLIFICADO)
            if (layoutMode == AgendaLayoutMode.COMPLETO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (totalSubtasks > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = "Subtarefas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$completedSubtasks/$totalSubtasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (task.notes.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Possui notas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (task.privacyScope != PrivacyScope.PERSONAL) {
                        Text(
                            text = "Família",
                            style = MaterialTheme.typography.labelSmall,
                            color = taskColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (task.isEssential) {
                        Text(
                            text = "★ Essencial",
                            style = MaterialTheme.typography.labelSmall,
                            color = taskColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (layoutMode == AgendaLayoutMode.SIMPLIFICADO && totalSubtasks > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$completedSubtasks/$totalSubtasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Circular Checkbox (matching Structured's clean circular ring / checkmark)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (task.completed) taskColor else Color.Transparent)
                .border(2.dp, taskColor, CircleShape)
                .clickable { onToggleTask(task) }
                .testTag("structured_task_checkbox_${task.id}"),
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concluída",
                    tint = Color(0xFF1A1B1E),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Structured Free Gap Row along the vertical connecting line.
 * Adapts to layoutMode:
 * - COMPLETO: "⏱️ Você terá Xh Ym livre. Alguma coisa que você gostaria de fazer?" + "+ Nova Tarefa" chip
 * - SIMPLIFICADO: Minimalist "⏱️ +1h 30m de intervalo" along the dashed line
 * - MINIMO: Hidden completely (caller does not render)
 */
@Composable
fun StructuredFreeGapRow(
    startMinutesOfDay: Int,
    endMinutesOfDay: Int,
    gapMinutes: Int,
    accentColor: Color,
    layoutMode: String,
    onAddInGap: () -> Unit
) {
    if (layoutMode == AgendaLayoutMode.MINIMO) return

    val durationLabel = remember(gapMinutes) {
        val h = gapMinutes / 60
        val m = gapMinutes % 60
        when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
    val startHHmm = remember(startMinutesOfDay) {
        DateCalendarUtils.formatTime(startMinutesOfDay / 60, startMinutesOfDay % 60)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddInGap() }
            .padding(horizontal = 14.dp)
            .testTag("structured_free_gap_$startMinutesOfDay"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left time offset column to align with task nodes
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (layoutMode == AgendaLayoutMode.COMPLETO) {
                Text(
                    text = startHHmm,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))

        // Dashed vertical line segment
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(if (layoutMode == AgendaLayoutMode.COMPLETO) 64.dp else 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                drawLine(
                    color = accentColor.copy(alpha = 0.55f),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        if (layoutMode == AgendaLayoutMode.COMPLETO) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("⏱️ Você terá ")
                        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                            append(durationLabel)
                        }
                        append(" livre. Alguma coisa que você gostaria de fazer?")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agendar às $startHHmm",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            // SIMPLIFICADO mode: clean single-line text like "+1h 30m de intervalo"
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold)) {
                        append("+$durationLabel")
                    }
                    append(" de intervalo")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Structured Task Detail Bottom Sheet (matching Screenshot 3: Screenshot_20261121_111132_Structured.jpg).
 * Displays:
 * - Coral/Accent handle bar at top
 * - Circular icon badge + subtitle/time + bold task title
 * - Notes / description text
 * - Interactive Subtask checklist with rounded square colored checkboxes & horizontal dividers
 * - Toggle Essential & Complete actions
 * - Full-width "🗑️ Excluir" button at the bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructuredTaskDetailBottomSheet(
    task: TaskItem,
    accentColor: Color,
    onDismiss: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onToggleEssential: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val taskColor = if (task.taskColorHex != 0L) Color(task.taskColorHex) else accentColor
    val startHHmm = DateCalendarUtils.formatTime(task.alarmHour.toInt(), task.alarmMinute.toInt())
    val iconVector = remember(task) { resolveStructuredTaskIcon(task) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18191C),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(taskColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("structured_task_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with circular icon, subtitle/time, and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(taskColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color(0xFF18191C),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$startHHmm • ${task.durationMinutes} min • ${task.category.ifBlank { "Rotina" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = { onToggleEssential(task) }) {
                    Icon(
                        imageVector = if (task.isEssential) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Essencial",
                        tint = if (task.isEssential) taskColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            // Notes / Description
            if (task.notes.isNotBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            }

            // Subtasks Checklist (matching screenshot 3 square coral checkboxes + dividers)
            if (task.subtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    task.subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(task, sub.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (sub.done) taskColor else Color.Transparent)
                                    .border(2.dp, taskColor, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF18191C),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (sub.done) Color.White.copy(alpha = 0.5f) else Color.White
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    }
                }
            }

            // Bottom Actions: Concluir & Excluir (matching screenshot 3 "🗑️ Excluir" button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25262B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onToggleTask(task)
                            onDismiss()
                        }
                        .testTag("structured_sheet_toggle_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = taskColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (task.completed) "Reabrir" else "Concluir",
                            fontWeight = FontWeight.Bold,
                            color = taskColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25262B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDeleteTask(task)
                            onDismiss()
                        }
                        .testTag("structured_sheet_delete_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = taskColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Excluir",
                            fontWeight = FontWeight.Bold,
                            color = taskColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Structured "Selecionar data" Calendar Modal Dialog (matching Screenshot 5: Screenshot_20261121_111114_Structured.jpg).
 */
@Composable
fun StructuredDatePickerDialog(
    selectedEpochDay: Long,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirmEpochDay: (Long) -> Unit
) {
    var tempSelectedDate by remember(selectedEpochDay) {
        mutableStateOf(LocalDate.ofEpochDay(selectedEpochDay))
    }
    var currentMonth by remember(selectedEpochDay) {
        mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(selectedEpochDay)))
    }
    val today = remember { LocalDate.ofEpochDay(DateCalendarUtils.todayEpochDay()) }

    val formattedSelectedHeader = remember(tempSelectedDate) {
        val dayOfWeekShort = tempSelectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, PtBrLocale)
            .lowercase(PtBrLocale)
            .removeSuffix(".")
        val monthShort = tempSelectedDate.month.getDisplayName(TextStyle.SHORT, PtBrLocale)
            .lowercase(PtBrLocale)
            .removeSuffix(".")
        "$dayOfWeekShort., ${tempSelectedDate.dayOfMonth} de $monthShort."
    }

    val monthYearLabel = remember(currentMonth) {
        val m = currentMonth.month.getDisplayName(TextStyle.FULL, PtBrLocale).lowercase(PtBrLocale)
        "$m de ${currentMonth.year}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF242222),
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("structured_date_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecionar data",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSelectedHeader,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Month switcher row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthYearLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mês anterior",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Próximo mês",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Weekday headers: D S T Q Q S S
                val weekHeaders = listOf("D", "S", "T", "Q", "Q", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekHeaders.forEach { h ->
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = h,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Calendar Days Grid
                val firstDayOfMonth = currentMonth.atDay(1)
                // Sunday = 0 .. Saturday = 6
                val leadingEmpty = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentMonth.lengthOfMonth()
                val totalSlots = ((leadingEmpty + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until (totalSlots / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dow in 0..6) {
                                val index = week * 7 + dow
                                val dayNum = index - leadingEmpty + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentMonth.atDay(dayNum)
                                    val isSelected = cellDate == tempSelectedDate
                                    val isToday = cellDate == today
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else Color.Transparent)
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.5.dp, accentColor, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { tempSelectedDate = cellDate },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color(0xFF18191C)
                                                isToday -> accentColor
                                                else -> Color.White
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }

                // Bottom buttons: Cancelar | OK
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancelar",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirmEpochDay(tempSelectedDate.toEpochDay())
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "OK",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Structured "Aparência" Settings Card (matching Screenshots 1, 2, 7:
 * Screenshot_20261121_111328, 111330, 111335).
 * Includes:
 * 1. Live Interactive Preview Card that updates in real time as the user toggles
 *    between "Completo", "Simplificado", and "Mínimo" and picks a "COR DO APP".
 * 2. "COR DO APP" horizontal circular swatches + "Editar paleta" action.
 * 3. "LAYOUT" 3-option segmented selector (Completo | Simplificado | Mínimo) + description.
 */
@Composable
fun StructuredAppearanceCard(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onOpenFullPalette: () -> Unit = {}
) {
    var selectedModuleId by remember { mutableStateOf(AppModuleId.AGENDA) }
    val activeModuleColorHex = config.colorForModule(selectedModuleId)
    val accentColor = Color(activeModuleColorHex)
    val agendaAccentColor = Color(config.agendaColorHex)
    val layoutMode = config.agendaLayoutMode
    val organicStyle = config.organicVisualStyle

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("structured_appearance_settings_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141518)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aparência & Cores dos Módulos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Personalize cada módulo separadamente, escolha temas sugeridos e o estilo orgânico",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }

            // 1. LIVE INTERACTIVE PREVIEW CARD (Exact Structured Mockup + 5 Module Pills Preview)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF282A2E),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("structured_appearance_live_preview")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top 7-day preview header inside the mockup
                    Surface(
                        color = Color(0xFF3A3C42),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in 1..7) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.35f))
                                    )
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    // Overlapping colored dots ONLY in COMPLETO mode
                                    if (layoutMode == AgendaLayoutMode.COMPLETO) {
                                        Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                                            listOf(
                                                Color(config.agendaColorHex),
                                                Color(config.hubColorHex),
                                                Color(config.healthColorHex),
                                                Color(config.financeColorHex)
                                            ).forEach { dotCol ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotCol)
                                                        .border(1.dp, Color(0xFF3A3C42), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview Timeline Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        PreviewTimelineItem(
                            timeText = "13:00",
                            icon = Icons.Default.LaptopMac,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = true
                        )
                        PreviewTimelineItem(
                            timeText = "13:15",
                            icon = Icons.Default.Restaurant,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = true
                        )

                        if (layoutMode == AgendaLayoutMode.COMPLETO) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "13:30",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.width(38.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .fillMaxHeight()
                                            .background(agendaAccentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("⏱️ Você terá ")
                                            withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.Bold)) {
                                                append("30m")
                                            }
                                            append(" livre. Alguma coisa que você go...")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(agendaAccentColor)
                                        )
                                    }
                                }
                            }
                        } else if (layoutMode == AgendaLayoutMode.SIMPLIFICADO) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "13:30",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.width(38.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .fillMaxHeight()
                                            .background(agendaAccentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append("⏱️ ")
                                        withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.Bold)) {
                                            append("30m")
                                        }
                                        append(" livre")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }

                        PreviewTimelineItem(
                            timeText = "14:00",
                            icon = Icons.Default.Call,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = layoutMode == AgendaLayoutMode.MINIMO
                        )

                        if (layoutMode == AgendaLayoutMode.MINIMO) {
                            PreviewTimelineItem(
                                timeText = "15:00",
                                icon = Icons.Default.FormatListBulleted,
                                accentColor = agendaAccentColor,
                                showTime = false,
                                connectBelow = false
                            )
                        }
                    }

                    // Mini Bar Showing the 5 Module Colors Live
                    Surface(
                        color = Color(0xFF1F2126),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomizationCatalog.moduleSpecs.forEach { spec ->
                                val modColor = Color(config.colorForModule(spec.moduleId))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(modColor)
                                    )
                                    Text(
                                        text = spec.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SUGESTÕES DE TEMA PRONTOS (Aplicam paleta harmônica para o app e os 5 módulos)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUGESTÕES DE TEMA (PALETAS HARMÔNICAS)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomizationCatalog.presets.forEach { preset ->
                        val isPresetSelected = !config.useCustomColors && config.presetId == preset.id
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isPresetSelected) Color(preset.primaryHex).copy(alpha = 0.25f) else Color(0xFF282A2E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    width = if (isPresetSelected) 2.dp else 1.dp,
                                    color = if (isPresetSelected) Color(preset.agendaColorHex) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable {
                                    customizationRepo.applyThemePreset(preset)
                                }
                                .testTag("theme_suggestion_${preset.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(
                                        preset.hubColorHex,
                                        preset.agendaColorHex,
                                        preset.healthColorHex,
                                        preset.diaryColorHex,
                                        preset.financeColorHex
                                    ).forEach { dotHex ->
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(Color(dotHex))
                                                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CORES INDEPENDENTES DOS 5 MÓDULOS (Hub, Agenda, Saúde, Diário, Finanças)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COR POR MÓDULO (5 MÓDULOS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = "Fontes & Sons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier
                            .clickable { onOpenFullPalette() }
                            .testTag("open_fonts_and_sounds_btn")
                    )
                }

                // Seletor em pílulas dos 5 módulos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomizationCatalog.moduleSpecs.forEach { spec ->
                        val isSelectedMod = selectedModuleId == spec.moduleId
                        val modColor = Color(config.colorForModule(spec.moduleId))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelectedMod) modColor.copy(alpha = 0.24f) else Color(0xFF282A2E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = if (isSelectedMod) 2.dp else 1.dp,
                                    color = if (isSelectedMod) modColor else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedModuleId = spec.moduleId }
                                .testTag("module_color_tab_${spec.moduleId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(modColor)
                                )
                                Text(
                                    text = spec.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelectedMod) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelectedMod) Color.White else Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // Barra de Swatches Circulares para o Módulo Selecionado
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomizationCatalog.structuredAppColors.forEachIndexed { idx, (_, colorHex) ->
                            val swatchColor = Color(colorHex)
                            val isSelected = activeModuleColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.5.dp, Color(0xFF141518), CircleShape)
                                                .border(4.dp, Color.White, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        customizationRepo.updateModuleColor(selectedModuleId, colorHex)
                                    }
                                    .testTag("structured_color_swatch_$idx")
                            )
                        }
                    }
                }

                val selectedModuleSpec = CustomizationCatalog.moduleSpecs.firstOrNull { it.moduleId == selectedModuleId }
                Text(
                    text = "Personalizando: ${selectedModuleSpec?.label ?: "Agenda"} — ${selectedModuleSpec?.description ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }

            // 4. ESTILO VISUAL ORGÂNICO (Opção de escolher nas configurações para ficar menos quadradão)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ESTILO VISUAL ORGÂNICO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val styles = listOf(
                            OrganicVisualStyle.ORGANICO_FLUIDO to "Pílula & Fluido",
                            OrganicVisualStyle.TRANSLUCIDO_GLASS to "Translúcido",
                            OrganicVisualStyle.MINIMALISTA_ACOLHEDOR to "Suave Minimal"
                        )
                        styles.forEach { (styleKey, label) ->
                            val selected = organicStyle == styleKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) accentColor else Color.Transparent)
                                    .clickable {
                                        customizationRepo.updateOrganicVisualStyle(styleKey)
                                    }
                                    .padding(vertical = 11.dp)
                                    .testTag("organic_visual_style_$styleKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF141518) else Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 5. LAYOUT DA AGENDA Section (Completo | Simplificado | Mínimo)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LAYOUT DA AGENDA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val options = listOf(
                            AgendaLayoutMode.COMPLETO to "Completo",
                            AgendaLayoutMode.SIMPLIFICADO to "Simplificado",
                            AgendaLayoutMode.MINIMO to "Mínimo"
                        )
                        options.forEach { (modeKey, label) ->
                            val selected = layoutMode == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) agendaAccentColor else Color.Transparent)
                                    .clickable {
                                        customizationRepo.updateAgendaLayoutMode(modeKey)
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag("structured_layout_mode_$modeKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF141518) else agendaAccentColor
                                )
                            }
                        }
                    }
                }

                Text(
                    text = buildAnnotatedString {
                        append("Os layouts ")
                        withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.SemiBold)) {
                            append("Simplificado")
                        }
                        append(" e ")
                        withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.SemiBold)) {
                            append("Mínimo")
                        }
                        append(" esconderão certos elementos para a interface causar menos distrações.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun PreviewTimelineItem(
    timeText: String,
    icon: ImageVector,
    accentColor: Color,
    showTime: Boolean,
    connectBelow: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTime) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.width(38.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (connectBelow) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(14.dp)
                        .background(accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF282A2E),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
