package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppCustomizationConfig
import com.example.data.CurrencyRatesState
import com.example.data.CustomizationCatalog
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.Household
import com.example.data.HubWidgetIds
import com.example.data.SavingsGoal
import com.example.data.SubscriptionBill
import com.example.data.TaskItem
import com.example.data.UserProfile
import com.example.data.formatCurrencyBrl
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold

private data class MemberColorFilterOption(
    val label: String,
    val colorHex: Long
)

private fun hubOrganicEnterTransition(): EnterTransition =
    expandVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        expandFrom = Alignment.Top
    ) + fadeIn(
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    )

private fun hubOrganicExitTransition(): ExitTransition =
    shrinkVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        shrinkTowards = Alignment.Top
    ) + fadeOut(
        animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
    )

@Composable
private fun OrganicExpandChevronBadge(
    expanded: Boolean,
    accentColor: Color,
    testTag: String? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hubChevronRotation"
    )
    Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = if (expanded) 0.22f else 0.12f),
        modifier = Modifier
            .size(32.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Recolher menu" else "Expandir menu",
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyHubScreen(
    selectedYearMonth: String,
    userProfile: UserProfile?,
    household: Household,
    tasks: List<TaskItem>,
    transactions: List<FinanceTransaction>,
    subscriptions: List<SubscriptionBill>,
    savingsGoals: List<SavingsGoal>,
    currencyRates: CurrencyRatesState,
    customizationConfig: AppCustomizationConfig,
    healthState: com.example.data.HealthModuleState = com.example.data.HealthModuleState(),
    diaryState: com.example.data.PrivateDiaryState? = null,
    userEmail: String? = null,
    onRequestGoogleLogin: () -> Unit = {},
    onShiftMonth: (Int) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onPostSubscriptionNow: (SubscriptionBill) -> Unit,
    onIncrementHealthTracker: (dateIso: String, trackerId: String, step: Int) -> Unit = { _, _, _ -> },
    onNextShiftQuote: () -> Unit = {},
    onOpenNewTaskDialog: () -> Unit,
    onOpenNewTransactionDialog: () -> Unit,
    onOpenAgendaScreen: () -> Unit,
    onOpenHealthScreen: () -> Unit = {},
    onOpenDiaryScreen: () -> Unit = {},
    onOpenFinanceScreen: (subSection: Int) -> Unit,
    onOpenProfileSettings: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current

    // All expandable smartwatch widgets start collapsed (false) by default
    val expandedWidgets = remember {
        mutableStateMapOf(
            HubWidgetIds.TASKS_TODAY to false,
            HubWidgetIds.HEALTH_SUMMARY to false,
            HubWidgetIds.BALANCE_RING to false,
            HubWidgetIds.BILLS_CURRENCY to false,
            HubWidgetIds.FAMILY_COLORS to false
        )
    }

    // Optional member color filter in the Hub
    var selectedMemberColorFilter by remember { mutableStateOf<Long?>(null) }

    val profileName = remember(customizationConfig.profileName, userProfile?.displayName) {
        customizationConfig.profileName.ifBlank {
            userProfile?.displayName?.ifBlank { "Você" } ?: "Você"
        }
    }
    val profileColorHex = customizationConfig.profileColorHex
    val hubAccentColor = remember(customizationConfig.hubColorHex) {
        Color(customizationConfig.hubColorHex.toInt())
    }

    // Enabled widgets in the user's chosen order
    val activeWidgetIds = remember(customizationConfig.hubWidgetOrder, customizationConfig.enabledHubWidgets) {
        customizationConfig.hubWidgetOrder.filter { it in customizationConfig.enabledHubWidgets }
    }

    // Finance metrics for the selected month
    val monthTransactions = remember(transactions, selectedYearMonth) {
        transactions.filter { it.yearMonth == selectedYearMonth }
    }
    val loggedIncomeBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
    val totalExpensesBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountBrl }
    }
    val netBalanceBrl = effectiveIncomeBrl - totalExpensesBrl

    val needsSpentBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
    }
    val wantsSpentBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
    }
    val savingsSpentBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "SAVINGS" }.sumOf { it.amountBrl }
    }

    // Agenda metrics
    val filteredTasksByColor = remember(tasks, selectedMemberColorFilter) {
        if (selectedMemberColorFilter == null) {
            tasks
        } else {
            tasks.filter { it.assignedMemberColorHex == selectedMemberColorFilter }
        }
    }
    val monthTasks = remember(filteredTasksByColor, selectedYearMonth) {
        filteredTasksByColor.filter { it.yearMonth == selectedYearMonth }
    }
    val pendingTasks = remember(filteredTasksByColor) {
        filteredTasksByColor.filter { !it.completed }.sortedBy { it.dateEpochDay }
    }
    val completedTasksCount = remember(monthTasks) {
        monthTasks.count { it.completed }
    }
    val totalMonthTasksCount = monthTasks.size.coerceAtLeast(1)
    val tasksCompletionRatio = (completedTasksCount.toFloat() / totalMonthTasksCount.toFloat()).coerceIn(0f, 1f)

    // Bills & Subscriptions metrics
    val activeSubscriptions = remember(subscriptions) {
        subscriptions.filter { it.active }.sortedBy { it.dueDayOfMonth }
    }
    val pendingSubscriptions = remember(activeSubscriptions, selectedYearMonth) {
        activeSubscriptions.filter { it.lastPostedYearMonth != selectedYearMonth }
    }
    val paidSubscriptionsCount = (activeSubscriptions.size - pendingSubscriptions.size).coerceAtLeast(0)
    val billsPaidRatio = if (activeSubscriptions.isEmpty()) {
        1f
    } else {
        (paidSubscriptionsCount.toFloat() / activeSubscriptions.size.toFloat()).coerceIn(0f, 1f)
    }

    // Distinct member profile colors present in the family agenda
    val memberColorOptions = remember(tasks, profileName, profileColorHex) {
        val map = LinkedHashMap<Long, String>()
        map[profileColorHex] = "$profileName (Meu Perfil)"
        tasks.forEach { task ->
            val hex = task.assignedMemberColorHex
            if (hex != 0L && !map.containsKey(hex)) {
                val cleanName = task.assignedMemberName.ifBlank { "Membro" }
                map[hex] = cleanName
            }
        }
        map.entries.map { MemberColorFilterOption(label = it.value, colorHex = it.key) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("family_hub_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Clean Smartwatch-Inspired Hub Header with Clickable Profile Avatar & Google Account Option
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = hubAccentColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(cardRadius)
                    ),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    hubAccentColor.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Olá, $profileName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${household.name} • Toque nos menus para expandir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Google Login / Cloud status pill (supports "Fazer login depois")
                            if (userEmail.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = hubAccentColor.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onRequestGoogleLogin() }
                                        .testTag("hub_google_login_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = hubAccentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Conectar conta Google",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = hubAccentColor
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = IncomeEmerald.copy(alpha = 0.14f),
                                    modifier = Modifier.testTag("hub_google_connected_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = IncomeEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IncomeEmerald,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Clickable Profile Avatar -> opens Unified Profile & Settings Menu
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenProfileSettings() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("hub_profile_avatar_btn")
                        ) {
                            ProfileAvatarBadge(
                                profileName = profileName,
                                profileColorHex = profileColorHex,
                                profilePhotoPath = customizationConfig.profilePhotoPath,
                                profilePhotoVersion = customizationConfig.profilePhotoVersion,
                                size = 50.dp,
                                onClick = { onOpenProfileSettings() },
                                testTag = "hub_top_avatar_badge"
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Perfil",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(profileColorHex.toInt())
                            )
                        }
                    }

                    // Month Selector + Quick Direct Buttons for Agenda, Saúde & Finanças
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { onShiftMonth(-1) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("prev_month_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Mês anterior"
                                    )
                                }
                                Text(
                                    text = DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { onShiftMonth(1) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("next_month_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Próximo mês"
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onOpenAgendaScreen,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("enter_agenda_module_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agenda", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onOpenHealthScreen,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("enter_health_module_btn")
                            ) {
                                Text("🩺 Saúde", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { onOpenFinanceScreen(0) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("enter_finance_module_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finanças", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // PASSO 4: Frases Motivacionais por Turno (Manhã 05:00-11:59, Tarde 12:00-17:59, Noite 18:00-04:59) + Atalho Diário
        // Collapsible with smooth AnimatedVisibility and collapsed by default in the Hub
        if (customizationConfig.threeShiftQuotesEnabled && diaryState != null) {
            item {
                ShiftMotivationalQuoteCard(
                    diaryState = diaryState,
                    onNextQuote = onNextShiftQuote,
                    onOpenDiary = onOpenDiaryScreen,
                    collapsible = true,
                    initiallyExpanded = false,
                    accentOverrideHex = customizationConfig.diaryColorHex
                )
            }
        }

        // Active member color filter banner if user filtered by a member color
        if (selectedMemberColorFilter != null) {
            item {
                val activeColor = Color(selectedMemberColorFilter!!.toInt())
                val activeLabel = memberColorOptions.firstOrNull { it.colorHex == selectedMemberColorFilter }?.label
                    ?: "Cor Selecionada"
                Surface(
                    shape = RoundedCornerShape(50),
                    color = activeColor.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeColor, RoundedCornerShape(50))
                        .clickable { selectedMemberColorFilter = null }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtrando tarefas por: $activeLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                        Text(
                            text = "Limpar filtro",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. User-Configured Smartwatch Widgets (Compact tiles that start collapsed and expand smoothly on tap)
        if (activeWidgetIds.isEmpty()) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Seu Hub está vazio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toque no seu Avatar do Perfil para ativar os widgets estilo smartwatch que você quer ver aqui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenProfileSettings,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Montar Meu Hub no Perfil")
                        }
                    }
                }
            }
        } else {
            items(activeWidgetIds, key = { it }) { widgetId ->
                val isExpanded = expandedWidgets[widgetId] == true
                when (widgetId) {
                    HubWidgetIds.TASKS_TODAY -> SmartwatchTasksWidget(
                        pendingTasks = pendingTasks,
                        completedCount = completedTasksCount,
                        totalCount = monthTasks.size,
                        progressRatio = tasksCompletionRatio,
                        moduleColorHex = customizationConfig.agendaColorHex,
                        expanded = isExpanded,
                        onToggleExpand = { expandedWidgets[widgetId] = !isExpanded },
                        onToggleTask = onToggleTask,
                        onOpenNewTaskDialog = onOpenNewTaskDialog,
                        onOpenAgendaScreen = onOpenAgendaScreen
                    )

                    HubWidgetIds.HEALTH_SUMMARY -> SmartwatchHealthWidget(
                        healthState = healthState,
                        customizationConfig = customizationConfig,
                        moduleColorHex = customizationConfig.healthColorHex,
                        expanded = isExpanded,
                        onToggleExpand = { expandedWidgets[widgetId] = !isExpanded },
                        onIncrementTracker = onIncrementHealthTracker,
                        onOpenHealthScreen = onOpenHealthScreen
                    )

                    HubWidgetIds.BALANCE_RING -> SmartwatchBalanceRingWidget(
                        netBalanceBrl = netBalanceBrl,
                        effectiveIncomeBrl = effectiveIncomeBrl,
                        totalExpensesBrl = totalExpensesBrl,
                        needsSpentBrl = needsSpentBrl,
                        wantsSpentBrl = wantsSpentBrl,
                        savingsSpentBrl = savingsSpentBrl,
                        household = household,
                        moduleColorHex = customizationConfig.financeColorHex,
                        expanded = isExpanded,
                        onToggleExpand = { expandedWidgets[widgetId] = !isExpanded },
                        onOpenNewTransactionDialog = onOpenNewTransactionDialog,
                        onOpenFinanceScreen = onOpenFinanceScreen
                    )

                    HubWidgetIds.BILLS_CURRENCY -> SmartwatchBillsCurrencyWidget(
                        activeSubscriptions = activeSubscriptions,
                        pendingSubscriptions = pendingSubscriptions,
                        paidRatio = billsPaidRatio,
                        currencyRates = currencyRates,
                        moduleColorHex = customizationConfig.financeColorHex,
                        expanded = isExpanded,
                        onToggleExpand = { expandedWidgets[widgetId] = !isExpanded },
                        onPostSubscriptionNow = onPostSubscriptionNow,
                        onOpenFinanceBills = { onOpenFinanceScreen(1) }
                    )

                    HubWidgetIds.FAMILY_COLORS -> SmartwatchFamilyColorsWidget(
                        profileName = profileName,
                        profileColorHex = profileColorHex,
                        moduleColorHex = customizationConfig.hubColorHex,
                        memberOptions = memberColorOptions,
                        selectedColorFilter = selectedMemberColorFilter,
                        tasks = tasks,
                        expanded = isExpanded,
                        onToggleExpand = { expandedWidgets[widgetId] = !isExpanded },
                        onSelectColorFilter = { colorHex ->
                            selectedMemberColorFilter = if (selectedMemberColorFilter == colorHex) null else colorHex
                        },
                        onOpenProfileSettings = onOpenProfileSettings
                    )
                }
            }
        }

        // 3. Subtle Bottom Shortcut to Customize Hub Widgets & Profile
        item {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable { onOpenProfileSettings() }
                    .testTag("hub_customize_widgets_footer_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personalizar Foto, Cores dos Módulos & Montar Meu Hub",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================================
// WIDGET 1: TAREFAS DO DIA & PRÓXIMOS ALARMES (SMARTWATCH COMPACT TILE)
// ============================================================================
@Composable
private fun SmartwatchTasksWidget(
    pendingTasks: List<TaskItem>,
    completedCount: Int,
    totalCount: Int,
    progressRatio: Float,
    moduleColorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onOpenNewTaskDialog: () -> Unit,
    onOpenAgendaScreen: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val ringColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }
    val nextAlarmTask = remember(pendingTasks) {
        pendingTasks.firstOrNull { it.alarmEnabled }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = ringColor.copy(alpha = if (expanded) 0.34f else 0.14f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_tasks"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ringColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Compact Smartwatch Header Row (Always visible, taps to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Smartwatch Circular Progress Complication
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            drawArc(
                                color = ringColor.copy(alpha = 0.18f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progressRatio,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${pendingTasks.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ringColor
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tarefas & Alarmes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pendingTasks.isEmpty()) {
                                "Tudo concluído ($completedCount/$totalCount)"
                            } else {
                                "${pendingTasks.size} pendente(s) • $completedCount concluída(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (nextAlarmTask != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AlarmOn,
                                    contentDescription = null,
                                    tint = ringColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Próx: ${DateCalendarUtils.formatTime(nextAlarmTask.alarmHour.toInt(), nextAlarmTask.alarmMinute.toInt())} • ${nextAlarmTask.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ringColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OrganicExpandChevronBadge(
                    expanded = expanded,
                    accentColor = ringColor
                )
            }

            // Expandable Drawer with Task Checklist colored by Member Profile Color
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pendingTasks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhuma tarefa pendente! Toque em Nova Tarefa para agendar com sua cor do perfil.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        pendingTasks.take(4).forEach { task ->
                            val taskMemberColor = if (task.assignedMemberColorHex != 0L) {
                                Color(task.assignedMemberColorHex.toInt())
                            } else {
                                ringColor
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = taskMemberColor.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, taskMemberColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onToggleTask(task) }
                                    .testTag("hub_quick_task_${task.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(taskMemberColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Concluir tarefa",
                                        tint = taskMemberColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${task.assignedMemberName.ifBlank { "Família" }} • ${DateCalendarUtils.formatEpochDayPt(task.dateEpochDay)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = taskMemberColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (task.estimatedCostBrl > 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = formatCurrencyBrl(task.estimatedCostBrl),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAgendaScreen,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Agenda")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenNewTaskDialog,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_task_btn")
                        ) {
                            Icon(imageVector = Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nova Tarefa")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// WIDGET 2: SALDO DISPONÍVEL & ANEL DE ECONOMIA 50/30/20 (SMARTWATCH TILE)
// ============================================================================
@Composable
private fun SmartwatchBalanceRingWidget(
    netBalanceBrl: Double,
    effectiveIncomeBrl: Double,
    totalExpensesBrl: Double,
    needsSpentBrl: Double,
    wantsSpentBrl: Double,
    savingsSpentBrl: Double,
    household: Household,
    moduleColorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenNewTransactionDialog: () -> Unit,
    onOpenFinanceScreen: (Int) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val financeColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }
    val safeIncome = effectiveIncomeBrl.coerceAtLeast(1.0)
    val spentRatio = (totalExpensesBrl / safeIncome).toFloat().coerceIn(0f, 1f)

    val needsTargetBrl = safeIncome * (household.needsPercentage / 100.0)
    val wantsTargetBrl = safeIncome * (household.wantsPercentage / 100.0)
    val savingsTargetBrl = safeIncome * (household.savingsPercentage / 100.0)

    val needsRatio = (needsSpentBrl / needsTargetBrl.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val wantsRatio = (wantsSpentBrl / wantsTargetBrl.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val savingsRatio = (savingsSpentBrl / savingsTargetBrl.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = financeColor.copy(alpha = if (expanded) 0.34f else 0.14f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_balance"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            financeColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Compact Smartwatch Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Smartwatch Multi-Ring 50/30/20 Complication
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val outerStroke = 5.dp.toPx()
                            // Outer ring: Needs (50%)
                            drawArc(
                                color = financeColor.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = financeColor,
                                startAngle = -90f,
                                sweepAngle = 360f * needsRatio,
                                useCenter = false,
                                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                            )
                        }
                        Box(modifier = Modifier.size(38.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val midStroke = 4.5.dp.toPx()
                                // Inner ring: Savings (20%)
                                drawArc(
                                    color = SavingsGold.copy(alpha = 0.22f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = midStroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = SavingsGold,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (1f - spentRatio).coerceIn(0.1f, 1f),
                                    useCenter = false,
                                    style = Stroke(width = midStroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Text(
                            text = "${((1f - spentRatio) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saldo & Anel ${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatCurrencyBrl(netBalanceBrl),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalanceBrl >= 0) IncomeEmerald else ExpenseCoral
                        )
                        Text(
                            text = "Gastos no mês: ${formatCurrencyBrl(totalExpensesBrl)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OrganicExpandChevronBadge(
                    expanded = expanded,
                    accentColor = financeColor
                )
            }

            // Expandable 50/30/20 Breakdown & Quick Expense Action
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmartwatchBucketBar(
                        label = "Essenciais (${household.needsPercentage}%)",
                        spentBrl = needsSpentBrl,
                        targetBrl = needsTargetBrl,
                        ratio = needsRatio,
                        color = financeColor
                    )
                    SmartwatchBucketBar(
                        label = "Estilo de Vida (${household.wantsPercentage}%)",
                        spentBrl = wantsSpentBrl,
                        targetBrl = wantsTargetBrl,
                        ratio = wantsRatio,
                        color = ExpenseCoral
                    )
                    SmartwatchBucketBar(
                        label = "Economia & Metas (${household.savingsPercentage}%)",
                        spentBrl = savingsSpentBrl,
                        targetBrl = savingsTargetBrl,
                        ratio = savingsRatio,
                        color = SavingsGold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenFinanceScreen(0) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Finanças")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenNewTransactionDialog,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_expense_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Novo Gasto")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartwatchBucketBar(
    label: String,
    spentBrl: Double,
    targetBrl: Double,
    ratio: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${formatCurrencyBrl(spentBrl)} / ${formatCurrencyBrl(targetBrl)}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

// ============================================================================
// WIDGET 3: CONTAS A VENCER NO MÊS & CÂMBIO RÁPIDO (SMARTWATCH TILE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartwatchBillsCurrencyWidget(
    activeSubscriptions: List<SubscriptionBill>,
    pendingSubscriptions: List<SubscriptionBill>,
    paidRatio: Float,
    currencyRates: CurrencyRatesState,
    moduleColorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onPostSubscriptionNow: (SubscriptionBill) -> Unit,
    onOpenFinanceBills: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val financeColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }
    val nextBill = pendingSubscriptions.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = financeColor.copy(alpha = if (expanded) 0.34f else 0.14f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_bills_currency"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            financeColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Compact Smartwatch Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                            val stroke = 6.dp.toPx()
                            drawArc(
                                color = financeColor.copy(alpha = 0.22f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = financeColor,
                                startAngle = -90f,
                                sweepAngle = 360f * paidRatio,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = financeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contas Fixas & Câmbio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (nextBill != null) {
                                "Próx: ${nextBill.title} (Dia ${nextBill.dueDayOfMonth}) • ${formatCurrencyBrl(nextBill.amountBrl)}"
                            } else {
                                "Contas fixas em dia • USD R$ ${"%.2f".format(currencyRates.usdToBrl)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OrganicExpandChevronBadge(
                    expanded = expanded,
                    accentColor = financeColor
                )
            }

            // Expandable Drawer with Monthly Bills & Exchange Rates
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Currency Pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "USD" to currencyRates.usdToBrl,
                            "EUR" to currencyRates.eurToBrl,
                            "GBP" to currencyRates.gbpToBrl
                        ).forEach { (code, rate) ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = financeColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "1 $code = R$ ${"%.2f".format(rate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = financeColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (pendingSubscriptions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Todas as ${activeSubscriptions.size} conta(s) recorrente(s) deste mês já foram lançadas!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        pendingSubscriptions.take(3).forEach { sub ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${sub.title} • Dia ${sub.dueDayOfMonth}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatCurrencyBrl(sub.amountBrl),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = financeColor
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onPostSubscriptionNow(sub) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Lançar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenFinanceBills,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gerenciar Custos Mensais & Câmbio")
                    }
                }
            }
        }
    }
}

// ============================================================================
// WIDGET 4: MEMBROS DA FAMÍLIA & FILTRO POR COR DO PERFIL (SMARTWATCH TILE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartwatchFamilyColorsWidget(
    profileName: String,
    profileColorHex: Long,
    moduleColorHex: Long,
    memberOptions: List<MemberColorFilterOption>,
    selectedColorFilter: Long?,
    tasks: List<TaskItem>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectColorFilter: (Long) -> Unit,
    onOpenProfileSettings: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val myColor = remember(profileColorHex) { Color(profileColorHex.toInt()) }
    val hubColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }
    val myColorName = remember(profileColorHex) {
        CustomizationCatalog.memberAgendaColors.firstOrNull { it.second == profileColorHex }?.first
            ?: "Cor do Perfil"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = hubColor.copy(alpha = if (expanded) 0.34f else 0.14f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_family_colors"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            hubColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Compact Smartwatch Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Multi-color Smartwatch Ring showing member agenda colors
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            val count = memberOptions.size.coerceAtLeast(1)
                            val sweep = 360f / count
                            memberOptions.forEachIndexed { idx, opt ->
                                drawArc(
                                    color = Color(opt.colorHex.toInt()),
                                    startAngle = -90f + idx * sweep + 4f,
                                    sweepAngle = (sweep - 8f).coerceAtLeast(12f),
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(myColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cores da Família na Agenda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sua cor: $myColorName ($profileName)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = myColor
                        )
                    }
                }

                OrganicExpandChevronBadge(
                    expanded = expanded,
                    accentColor = hubColor
                )
            }

            // Expandable Drawer to filter tasks by Member Profile Color or edit Profile Color
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Toque em uma cor para filtrar as tarefas do Hub por membro da família:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        memberOptions.forEach { option ->
                            val isSelected = selectedColorFilter == option.colorHex
                            val chipColor = Color(option.colorHex.toInt())
                            val taskCount = tasks.count { it.assignedMemberColorHex == option.colorHex }

                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectColorFilter(option.colorHex) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${option.label} ($taskCount)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenProfileSettings,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mudar Minha Foto & Cor na Agenda")
                    }
                }
            }
        }
    }
}

// ============================================================================
// WIDGET 5: SAÚDE, INEGOCIÁVEIS & MEDIÇÕES (SMARTWATCH COMPACT TILE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartwatchHealthWidget(
    healthState: com.example.data.HealthModuleState,
    customizationConfig: AppCustomizationConfig,
    moduleColorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onIncrementTracker: (dateIso: String, trackerId: String, step: Int) -> Unit,
    onOpenHealthScreen: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val todayIso = remember { DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay()) }
    val totalTrackers = healthState.trackers.size.coerceAtLeast(1)
    val completedTrackers = healthState.completedTrackersCount(todayIso)
    val progressRatio = (completedTrackers.toFloat() / totalTrackers.toFloat()).coerceIn(0f, 1f)

    val latestGlucose = healthState.glucoseRecords.firstOrNull()
    val latestBp = healthState.bloodPressureRecords.firstOrNull()
    val timer = healthState.postMealTimer
    val ringColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = ringColor.copy(alpha = if (expanded) 0.34f else 0.14f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_health"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ringColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Compact Smartwatch Header Row (Always visible, taps to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Smartwatch Circular Progress Ring for Daily Trackers (Inegociáveis)
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            drawArc(
                                color = ringColor.copy(alpha = 0.18f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (completedTrackers == totalTrackers) IncomeEmerald else ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progressRatio,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$completedTrackers/$totalTrackers",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (completedTrackers == totalTrackers) IncomeEmerald else ringColor
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saúde & Inegociáveis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val glucoseLabel = latestGlucose?.let { "🩸 ${it.valueMgDl} mg/dL" } ?: "🩸 Sem glicemia"
                        val bpLabel = latestBp?.let { "❤️ ${it.systolic}/${it.diastolic} mmHg" } ?: "❤️ Sem pressão"
                        Text(
                            text = "$glucoseLabel • $bpLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (timer.isRunning || timer.completedAlertPending) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (timer.completedAlertPending) {
                                    "🔔 1h pós-${timer.mealLabel} concluída! Hora de medir"
                                } else {
                                    "⏱️ Timer 1h pós-${timer.mealLabel} em andamento"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (timer.completedAlertPending) ExpenseCoral else ringColor
                            )
                        } else if (customizationConfig.pregnancyEnabled) {
                            val preg = com.example.data.PregnancyCalculator.calculateFromDpp(customizationConfig.pregnancyDppDate)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🤰 Gestação: Semana ${preg.currentWeek} (+${preg.currentDaysRemainder}d)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84B72)
                            )
                        }
                    }
                }

                OrganicExpandChevronBadge(
                    expanded = expanded,
                    accentColor = ringColor
                )
            }

            // Expandable Drawer with Quick Daily Trackers (+1) and Latest Measurements
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Marcação Rápida dos Inegociáveis de Hoje:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    healthState.trackers.take(4).forEach { habit ->
                        val currentVal = healthState.progressForTracker(todayIso, habit.id)
                        val isDone = currentVal >= habit.targetValue
                        val habitColor = Color(habit.colorHex.toInt())

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = habitColor.copy(alpha = 0.10f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, habitColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .testTag("hub_health_tracker_${habit.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = habit.emoji, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$currentVal / ${habit.targetValue} ${habit.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) IncomeEmerald else habitColor
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { onIncrementTracker(todayIso, habit.id, habit.stepValue) },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("hub_tracker_plus_${habit.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("+${habit.stepValue}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Latest clinical measurements summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Última Glicemia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestGlucose?.let { "${it.valueMgDl} mg/dL (${it.slot})" } ?: "Sem registro",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Última Pressão",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestBp?.let { "${it.systolic}/${it.diastolic} mmHg" } ?: "Sem registro",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onOpenHealthScreen,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_open_health_module_btn")
                    ) {
                        Text("Abrir Módulo de Saúde, Timer 1h & Relatório PDF")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
