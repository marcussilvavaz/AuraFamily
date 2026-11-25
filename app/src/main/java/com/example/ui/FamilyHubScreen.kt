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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
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
    val colorHex: Long,
    val avatarBase64: String = ""
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
    healthRepo: com.example.data.HealthRepository? = null,
    diaryState: com.example.data.PrivateDiaryState? = null,
    achievementsSummary: com.example.data.AchievementsEvaluationSummary = com.example.data.AchievementsEvaluationSummary(),
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
    onOpenFamilyMuralScreen: () -> Unit = onOpenDiaryScreen,
    onOpenAchievementsScreen: () -> Unit = {},
    onOpenFinanceScreen: (subSection: Int) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onToggleFinancePrivacy: () -> Unit = {},
    onToggleMuralItem: (String) -> Unit = {},
    onAddQuickMuralItem: (String) -> Unit = {}
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideFinanceValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val organicButtonShape = com.example.ui.theme.omniOrganicButtonShape()

    // All expandable smartwatch widgets start collapsed (false) by default
    val expandedWidgets = remember {
        mutableStateMapOf(
            HubWidgetIds.TASKS_TODAY to false,
            HubWidgetIds.HEALTH_SUMMARY to false,
            HubWidgetIds.BALANCE_RING to false,
            HubWidgetIds.ACHIEVEMENTS_MEDALS to false,
            HubWidgetIds.BILLS_CURRENCY to false,
            HubWidgetIds.FAMILY_COLORS to false
        )
    }

    // Optional member color filter in the Hub + Global Search Query
    var selectedMemberColorFilter by remember { mutableStateOf<Long?>(null) }
    var globalSearchQuery by remember { mutableStateOf("") }
    var quickMuralInput by remember { mutableStateOf("") }

    val profileName = remember(customizationConfig.profileName, userProfile?.displayName, userEmail) {
        val rawCustom = customizationConfig.profileName.trim()
        val rawProfile = userProfile?.displayName?.trim().orEmpty()
        val emailPrefix = userEmail?.substringBefore("@")?.trim().orEmpty()
        when {
            rawCustom.isNotBlank() && !rawCustom.equals("Você", ignoreCase = true) && !rawCustom.equals("VO", ignoreCase = true) -> rawCustom
            rawProfile.isNotBlank() && !rawProfile.equals("Você", ignoreCase = true) && !rawProfile.equals("VO", ignoreCase = true) -> rawProfile
            emailPrefix.isNotBlank() -> emailPrefix.replaceFirstChar { it.uppercase() }
            else -> "Membro"
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

    // Agenda metrics (including recurring tasks and multi-day tasks occurring today with per-date completion)
    val todayEpoch = remember { DateCalendarUtils.todayEpochDay() }
    val filteredTasksByColor = remember(tasks, selectedMemberColorFilter) {
        if (selectedMemberColorFilter == null) {
            tasks
        } else {
            tasks.filter { it.assignedMemberColorHex == selectedMemberColorFilter }
        }
    }
    val todayAndMonthTasks = remember(filteredTasksByColor, selectedYearMonth, todayEpoch) {
        filteredTasksByColor
            .filter { task ->
                (!task.isInbox && task.occursOnEpochDay(todayEpoch)) ||
                    task.yearMonth == selectedYearMonth
            }
            .map { task ->
                if (!task.isInbox && task.occursOnEpochDay(todayEpoch)) {
                    task.withCompletedForDate(todayEpoch)
                } else {
                    task
                }
            }
    }
    val monthTasks = todayAndMonthTasks
    val pendingTasks = remember(todayAndMonthTasks, todayEpoch) {
        todayAndMonthTasks
            .filter { !it.completed }
            .sortedWith(
                compareByDescending<TaskItem> { !it.isInbox && it.occursOnEpochDay(todayEpoch) }
                    .thenBy { it.dateEpochDay }
                    .thenBy { it.alarmHour }
                    .thenBy { it.alarmMinute }
            )
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

    // Official family members from Firestore household (e.g. Marcus & Nathalia) with auto-assigned distinct colors
    val officialMembers = remember(household.members, profileName, profileColorHex) {
        household.membersWithAssignedColors(
            fallbackProfileName = profileName,
            fallbackProfileColorHex = profileColorHex
        )
    }

    val memberColorOptions = remember(officialMembers, profileName, profileColorHex, customizationConfig.profileAvatarBase64, userProfile?.avatarBase64) {
        val seenNames = mutableSetOf<String>()
        val seenColors = mutableSetOf<Long>()
        val result = mutableListOf<MemberColorFilterOption>()
        val myAvatar = customizationConfig.profileAvatarBase64.ifBlank { userProfile?.avatarBase64.orEmpty() }

        officialMembers.forEach { member ->
            val cleanName = member.displayName.trim()
            val normName = cleanName.lowercase()
            if (cleanName.isNotBlank() &&
                normName != "você" &&
                normName != "voce" &&
                normName != "vo" &&
                normName != "família" &&
                normName != "familia" &&
                member.colorHex != 0L &&
                seenNames.add(normName) &&
                seenColors.add(member.colorHex)
            ) {
                val resolvedAvatar = member.avatarBase64.ifBlank {
                    if (normName == profileName.trim().lowercase()) myAvatar else ""
                }
                result.add(
                    MemberColorFilterOption(
                        label = cleanName,
                        colorHex = member.colorHex,
                        avatarBase64 = resolvedAvatar
                    )
                )
            }
        }
        if (result.isEmpty() && profileName.isNotBlank() && !profileName.equals("Família", ignoreCase = true)) {
            result.add(
                MemberColorFilterOption(
                    label = profileName,
                    colorHex = profileColorHex,
                    avatarBase64 = myAvatar
                )
            )
        }
        result
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val muralItems = remember(household.muralItems) {
        household.muralItems.ifEmpty {
            com.example.data.OmniBackupAndMigrationManager.loadFamilyMuralItems(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("family_hub_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Compact Header with Couple/Family Avatars, Scrollable Quick Shortcuts & Profile in Top-Right
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
                elevation = CardDefaults.cardElevation(
                    defaultElevation = com.example.ui.theme.omniOrganicCardElevationDp(defaultDp = 3.dp)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    hubAccentColor.copy(alpha = 0.14f),
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
                                text = "Olá, $profileName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = household.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (userEmail.isNullOrBlank()) {
                                Surface(
                                    shape = organicButtonShape,
                                    color = hubAccentColor.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(organicButtonShape)
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
                                            text = "Conectar Google",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = hubAccentColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = organicButtonShape,
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

                        // Clickable Profile Avatar in Top-Right -> Opens Unified Profile & Settings Menu
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
                                profileAvatarBase64 = customizationConfig.profileAvatarBase64.ifBlank { userProfile?.avatarBase64.orEmpty() },
                                size = 48.dp,
                                onClick = { onOpenProfileSettings() },
                                testTag = "hub_top_avatar_badge"
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Perfil",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(profileColorHex.toInt()),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Slim Couple / Family Member Avatars Bar (1 single "Família" filter + deduplicated real members)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = organicButtonShape,
                            color = if (selectedMemberColorFilter == null) hubAccentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedMemberColorFilter == null) 1.5.dp else 1.dp,
                                color = if (selectedMemberColorFilter == null) hubAccentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(organicButtonShape)
                                .clickable { selectedMemberColorFilter = null }
                                .testTag("hub_member_avatar_filter_all")
                        ) {
                            Text(
                                text = "Família",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMemberColorFilter == null) hubAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        memberColorOptions.forEach { option ->
                            val isSelected = selectedMemberColorFilter == option.colorHex
                            val memberCol = Color(option.colorHex.toInt())
                            Surface(
                                shape = organicButtonShape,
                                color = if (isSelected) memberCol.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = memberCol
                                ),
                                modifier = Modifier
                                    .clip(organicButtonShape)
                                    .clickable {
                                        selectedMemberColorFilter = if (isSelected) null else option.colorHex
                                    }
                                    .testTag("hub_member_avatar_filter_${option.label}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ProfileAvatarBadge(
                                        profileName = option.label,
                                        profileColorHex = option.colorHex,
                                        profilePhotoPath = "",
                                        profilePhotoVersion = 0L,
                                        profileAvatarBase64 = option.avatarBase64,
                                        size = 20.dp
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = organicButtonShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .clip(organicButtonShape)
                                .clickable { onOpenProfileSettings() }
                                .testTag("hub_edit_photo_color_shortcut")
                        ) {
                            Text(
                                text = "🎨 Cor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Month Selector + Horizontally Scrollable Quick Direct Buttons (never clipped!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = organicButtonShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { onShiftMonth(-1) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("prev_month_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Mês anterior"
                                    )
                                }
                                Text(
                                    text = DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = { onShiftMonth(1) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("next_month_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Próximo mês"
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = onOpenAgendaScreen,
                            shape = organicButtonShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                            Text("Agenda", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = onOpenHealthScreen,
                            shape = organicButtonShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("enter_health_module_btn")
                        ) {
                            Text("🩺 Saúde", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = onOpenFamilyMuralScreen,
                            shape = organicButtonShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("enter_diary_mural_module_btn")
                        ) {
                            Text("📌 Mural", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = { onOpenFinanceScreen(0) },
                            shape = organicButtonShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                            Text("Finanças", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = onOpenAchievementsScreen,
                            shape = organicButtonShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("enter_achievements_module_btn")
                        ) {
                            Text(
                                text = "🏆 ${achievementsSummary.unlockedCount}/${achievementsSummary.totalCount}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Global Search Bar in the Hub (Tasks, Transactions, Family Mural & Glucose)
                    OutlinedTextField(
                        value = globalSearchQuery,
                        onValueChange = { globalSearchQuery = it },
                        placeholder = {
                            Text(
                                "Buscar tarefas, gastos, mural da família ou glicemia...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Busca Global",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (globalSearchQuery.isNotBlank()) {
                                IconButton(onClick = { globalSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar busca",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = organicButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_global_search_input")
                    )

                    if (globalSearchQuery.isNotBlank()) {
                        val q = globalSearchQuery.trim().lowercase()
                        val matchedTasks = tasks.filter {
                            it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.location.lowercase().contains(q)
                        }.take(3)
                        val matchedTx = transactions.filter {
                            it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.accountName.lowercase().contains(q)
                        }.take(3)
                        val matchedMural = muralItems.filter {
                            it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.notes.lowercase().contains(q)
                        }.take(3)
                        val matchedGlucose = healthState.glucoseRecords.filter {
                            it.valueMgDl.toString().contains(q) || it.slot.lowercase().contains(q) || it.notes.lowercase().contains(q)
                        }.take(2)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_global_search_results")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val totalResults = matchedTasks.size + matchedTx.size + matchedMural.size + matchedGlucose.size
                                Text(
                                    text = "🔍 Resultados da Busca Global ($totalResults)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = hubAccentColor
                                )
                                if (totalResults == 0) {
                                    Text(
                                        text = "Nenhum item encontrado para \"$globalSearchQuery\".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                matchedTasks.forEach { t ->
                                    Text(
                                        text = "📅 Tarefa: ${t.title} (${DateCalendarUtils.formatEpochDayPt(t.dateEpochDay)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { onOpenAgendaScreen() }
                                    )
                                }
                                matchedTx.forEach { tx ->
                                    Text(
                                        text = "💳 Finanças: ${tx.title} • ${formatCurrencyBrl(tx.amountBrl, hideFinanceValues)} (${tx.category})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { onOpenFinanceScreen(0) }
                                    )
                                }
                                matchedMural.forEach { m ->
                                    Text(
                                        text = "📌 Mural (${m.category}): ${m.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { onOpenFamilyMuralScreen() }
                                    )
                                }
                                matchedGlucose.forEach { g ->
                                    Text(
                                        text = "🩸 Glicemia: ${g.valueMgDl} mg/dL (${g.slot} • ${g.dateIso})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { onOpenHealthScreen() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PASSO 4: Frases Motivacionais por Turno (Manhã 05:00-11:59, Tarde 12:00-17:59, Noite 18:00-04:59) + Atalho Diário
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

        // 2. BENTO GRID & MAIN BLOCKS IN THE HUB
        // Full-Width Block A: Monitoramento de Glicemia & Saúde
        if (HubWidgetIds.HEALTH_SUMMARY in activeWidgetIds) {
            item(key = "hub_bento_health") {
                val isExpanded = expandedWidgets[HubWidgetIds.HEALTH_SUMMARY] == true
                SmartwatchHealthWidget(
                    healthState = healthState,
                    healthRepo = healthRepo,
                    customizationConfig = customizationConfig,
                    moduleColorHex = customizationConfig.healthColorHex,
                    expanded = isExpanded,
                    onToggleExpand = { expandedWidgets[HubWidgetIds.HEALTH_SUMMARY] = !isExpanded },
                    onIncrementTracker = onIncrementHealthTracker,
                    onOpenHealthScreen = onOpenHealthScreen
                )
            }
        }

        // Full-Width Block B: Tarefas de Hoje & Mural da Família
        if (HubWidgetIds.TASKS_TODAY in activeWidgetIds) {
            item(key = "hub_bento_tasks") {
                val isExpanded = expandedWidgets[HubWidgetIds.TASKS_TODAY] == true
                SmartwatchTasksWidget(
                    pendingTasks = pendingTasks,
                    completedCount = completedTasksCount,
                    totalCount = monthTasks.size,
                    progressRatio = tasksCompletionRatio,
                    moduleColorHex = customizationConfig.agendaColorHex,
                    expanded = isExpanded,
                    onToggleExpand = { expandedWidgets[HubWidgetIds.TASKS_TODAY] = !isExpanded },
                    onToggleTask = onToggleTask,
                    onOpenNewTaskDialog = onOpenNewTaskDialog,
                    onOpenAgendaScreen = onOpenAgendaScreen
                )
            }
        }

        // Quick Family Mural & Shopping List Card in the Hub
        item(key = "hub_bento_family_mural") {
            val muralAccent = Color(customizationConfig.diaryColorHex.toInt())
            val pendingMural = muralItems.filter { !it.done }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, muralAccent.copy(alpha = 0.25f), RoundedCornerShape(cardRadius))
                    .testTag("hub_family_mural_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = com.example.ui.theme.omniOrganicCardElevationDp(defaultDp = 2.dp)
                )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(muralAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = muralAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mural da Família & Compras",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${pendingMural.size} item(ns) pendente(s) na lista compartilhada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = organicButtonShape,
                            color = muralAccent.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(organicButtonShape)
                                .clickable { onOpenFamilyMuralScreen() }
                                .testTag("hub_open_family_mural_btn")
                        ) {
                            Text(
                                text = "Abrir Mural",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = muralAccent,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickMuralInput,
                            onValueChange = { quickMuralInput = it },
                            placeholder = { Text("Adicionar item ou nota no mural...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = organicButtonShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_quick_mural_input")
                        )
                        FilledTonalButton(
                            onClick = {
                                if (quickMuralInput.isNotBlank()) {
                                    onAddQuickMuralItem(quickMuralInput.trim())
                                    quickMuralInput = ""
                                }
                            },
                            shape = organicButtonShape,
                            modifier = Modifier.testTag("hub_quick_mural_add_btn")
                        ) {
                            Text("+ Incluir", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                        }
                    }

                    pendingMural.take(3).forEach { item ->
                        val badgeCol = Color(item.addedByColorHex.toInt())
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleMuralItem(item.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (item.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = muralAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (item.pinned) "📌 ${item.title}" else item.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(badgeCol)
                                    )
                                    Text(
                                        text = item.addedBy,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2-Column Bento Grid for Smaller Financial Cards: "Saldo & Metas" + "Contas Fixas & Câmbio"
        item(key = "hub_bento_finance_2col") {
            val balanceExpanded = expandedWidgets[HubWidgetIds.BALANCE_RING] == true
            val billsExpanded = expandedWidgets[HubWidgetIds.BILLS_CURRENCY] == true

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!balanceExpanded && !billsExpanded) {
                    // 2-Column Side-by-Side Bento Grid when collapsed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val financeColor = Color(customizationConfig.financeColorHex.toInt())
                        // Bento Left Tile: Saldo & Metas
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, financeColor.copy(alpha = 0.25f), RoundedCornerShape(cardRadius))
                                .clickable { expandedWidgets[HubWidgetIds.BALANCE_RING] = true }
                                .testTag("smartwatch_widget_balance"),
                            shape = RoundedCornerShape(cardRadius),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = com.example.ui.theme.omniOrganicCardElevationDp(defaultDp = 2.dp)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saldo & Metas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onToggleFinancePrivacy,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("hub_toggle_finance_privacy_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (hideFinanceValues) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Ocultar ou mostrar saldo",
                                            tint = financeColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = formatCurrencyBrl(netBalanceBrl, hideFinanceValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netBalanceBrl >= 0) IncomeEmerald else ExpenseCoral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Gastos: ${formatCurrencyBrl(totalExpensesBrl, hideFinanceValues)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Bento Right Tile: Contas Fixas & Câmbio
                        val nextBill = pendingSubscriptions.firstOrNull()
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, financeColor.copy(alpha = 0.25f), RoundedCornerShape(cardRadius))
                                .clickable { expandedWidgets[HubWidgetIds.BILLS_CURRENCY] = true }
                                .testTag("smartwatch_widget_bills_currency"),
                            shape = RoundedCornerShape(cardRadius),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = com.example.ui.theme.omniOrganicCardElevationDp(defaultDp = 2.dp)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Contas Fixas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CurrencyExchange,
                                        contentDescription = null,
                                        tint = financeColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = if (nextBill != null) {
                                        "Dia ${nextBill.dueDayOfMonth} • ${nextBill.title}"
                                    } else {
                                        "Em dia (${activeSubscriptions.size})"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = financeColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "USD R$ ${"%.2f".format(currencyRates.usdToBrl)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    if (HubWidgetIds.BALANCE_RING in activeWidgetIds) {
                        SmartwatchBalanceRingWidget(
                            netBalanceBrl = netBalanceBrl,
                            effectiveIncomeBrl = effectiveIncomeBrl,
                            totalExpensesBrl = totalExpensesBrl,
                            needsSpentBrl = needsSpentBrl,
                            wantsSpentBrl = wantsSpentBrl,
                            savingsSpentBrl = savingsSpentBrl,
                            household = household,
                            moduleColorHex = customizationConfig.financeColorHex,
                            expanded = balanceExpanded,
                            onToggleExpand = { expandedWidgets[HubWidgetIds.BALANCE_RING] = !balanceExpanded },
                            onOpenNewTransactionDialog = onOpenNewTransactionDialog,
                            onOpenFinanceScreen = onOpenFinanceScreen,
                            onToggleFinancePrivacy = onToggleFinancePrivacy
                        )
                    }
                    if (HubWidgetIds.BILLS_CURRENCY in activeWidgetIds) {
                        SmartwatchBillsCurrencyWidget(
                            activeSubscriptions = activeSubscriptions,
                            pendingSubscriptions = pendingSubscriptions,
                            paidRatio = billsPaidRatio,
                            currencyRates = currencyRates,
                            moduleColorHex = customizationConfig.financeColorHex,
                            expanded = billsExpanded,
                            onToggleExpand = { expandedWidgets[HubWidgetIds.BILLS_CURRENCY] = !billsExpanded },
                            onPostSubscriptionNow = onPostSubscriptionNow,
                            onOpenFinanceBills = { onOpenFinanceScreen(1) }
                        )
                    }
                }
            }
        }

        // Compact Banner: Conquistas & Medalhas
        if (HubWidgetIds.ACHIEVEMENTS_MEDALS in activeWidgetIds) {
            item(key = "hub_bento_achievements") {
                val isExpanded = expandedWidgets[HubWidgetIds.ACHIEVEMENTS_MEDALS] == true
                SmartwatchAchievementsWidget(
                    summary = achievementsSummary,
                    customizationConfig = customizationConfig,
                    expanded = isExpanded,
                    onToggleExpand = { expandedWidgets[HubWidgetIds.ACHIEVEMENTS_MEDALS] = !isExpanded },
                    onOpenAchievementsScreen = onOpenAchievementsScreen
                )
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
    onOpenFinanceScreen: (Int) -> Unit,
    onToggleFinancePrivacy: () -> Unit = {}
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val hideFinanceValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val financeColor = remember(moduleColorHex) { Color(moduleColorHex.toInt()) }
    val cleanIncome = effectiveIncomeBrl.coerceAtLeast(0.0)
    val safeIncomeDenominator = if (cleanIncome > 0.0) cleanIncome else 1.0
    val spentRatio = if (cleanIncome > 0.0) (totalExpensesBrl / safeIncomeDenominator).toFloat().coerceIn(0f, 1f) else 0f

    val needsTargetBrl = cleanIncome * (household.needsPercentage / 100.0)
    val wantsTargetBrl = cleanIncome * (household.wantsPercentage / 100.0)
    val savingsTargetBrl = cleanIncome * (household.savingsPercentage / 100.0)

    val needsRatio = if (needsTargetBrl > 0.0) (needsSpentBrl / needsTargetBrl).toFloat().coerceIn(0f, 1f) else 0f
    val wantsRatio = if (wantsTargetBrl > 0.0) (wantsSpentBrl / wantsTargetBrl).toFloat().coerceIn(0f, 1f) else 0f
    val savingsRatio = if (savingsTargetBrl > 0.0) (savingsSpentBrl / savingsTargetBrl).toFloat().coerceIn(0f, 1f) else 0f

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
                    // Smartwatch Multi-Ring Complication
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val outerStroke = 5.dp.toPx()
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
                            text = "Saldo & Metas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatCurrencyBrl(netBalanceBrl, hideFinanceValues),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalanceBrl >= 0) IncomeEmerald else ExpenseCoral
                        )
                        Text(
                            text = "Gastos no mês: ${formatCurrencyBrl(totalExpensesBrl, hideFinanceValues)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFinancePrivacy,
                    modifier = Modifier.testTag("hub_toggle_finance_privacy_expanded_btn")
                ) {
                    Icon(
                        imageVector = if (hideFinanceValues) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Ocultar ou mostrar saldo",
                        tint = financeColor
                    )
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
                            Text("Novo Lançamento")
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
    val hideFinanceValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
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
                text = "${formatCurrencyBrl(spentBrl, hideFinanceValues)} / ${formatCurrencyBrl(targetBrl, hideFinanceValues)}",
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
    healthRepo: com.example.data.HealthRepository?,
    customizationConfig: AppCustomizationConfig,
    moduleColorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onIncrementTracker: (dateIso: String, trackerId: String, step: Int) -> Unit,
    onOpenHealthScreen: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val triggerFeedback = rememberOmniFeedback()
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

    // Live countdown ticker when diabetesEnabled is true
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedMealLabel by remember(timer.mealLabel) {
        mutableStateOf(timer.mealLabel.ifBlank { "Almoço" })
    }
    var selectedDurationMillis by remember(timer.durationMillis) {
        mutableStateOf(if (timer.durationMillis > 0L) timer.durationMillis else 60L * 60L * 1000L)
    }
    var quickGlucoseValueInput by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(
        customizationConfig.diabetesEnabled,
        timer.isRunning,
        timer.targetEpochMillis
    ) {
        if (!customizationConfig.diabetesEnabled) return@LaunchedEffect
        while (timer.isRunning) {
            val current = System.currentTimeMillis()
            nowMillis = current
            if (current >= timer.targetEpochMillis && timer.targetEpochMillis > 0L) {
                healthRepo?.markPostMealTimerCompleted()
                break
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    val remainingMillis = remember(timer, nowMillis, selectedDurationMillis) {
        when {
            timer.isRunning -> (timer.targetEpochMillis - nowMillis).coerceAtLeast(0L)
            timer.pausedRemainingMillis > 0L -> timer.pausedRemainingMillis
            timer.completedAlertPending -> 0L
            else -> selectedDurationMillis
        }
    }
    val totalMinutes = (remainingMillis / 1000L) / 60L
    val secondsPart = (remainingMillis / 1000L) % 60L
    val formattedCountdown = remember(totalMinutes, secondsPart) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", totalMinutes, secondsPart)
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
                            text = if (customizationConfig.diabetesEnabled) {
                                "Saúde, Glicemia & Timer"
                            } else {
                                "Saúde & Inegociáveis"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val enabledVitalsSummary = buildList {
                            if (customizationConfig.diabetesEnabled) {
                                add(latestGlucose?.let { "🩸 ${it.valueMgDl} mg/dL (${it.slot})" } ?: "🩸 Sem glicemia hoje")
                            }
                            if (customizationConfig.bloodPressureEnabled) {
                                add(latestBp?.let { "❤️ ${it.systolic}/${it.diastolic} mmHg" } ?: "❤️ Sem pressão")
                            }
                        }
                        Text(
                            text = if (enabledVitalsSummary.isNotEmpty()) {
                                enabledVitalsSummary.joinToString(" • ")
                            } else {
                                "$completedTrackers de $totalTrackers inegociáveis concluídos hoje"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (customizationConfig.diabetesEnabled) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    timer.completedAlertPending -> "🔔 Alarme Pós-${timer.mealLabel}! Hora de medir a glicemia"
                                    timer.isRunning -> "⏱️ Cronômetro Pós-${timer.mealLabel}: $formattedCountdown restantes"
                                    timer.pausedRemainingMillis > 0L -> "⏸️ Cronômetro pausado ($formattedCountdown)"
                                    else -> "⏱️ Cronômetro de Glicemia pronto (${selectedDurationMillis / 60000L} min)"
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

            // Always-visible Quick Glucose Timer Strip in the Hub when Diabetes/Glicemia is enabled
            if (customizationConfig.diabetesEnabled && !expanded) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ringColor.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
                        .border(1.dp, ringColor.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                        .testTag("hub_glucose_timer_compact_strip")
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
                                text = "🩸 Glicemia Pós-$selectedMealLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cronômetro: $formattedCountdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (timer.completedAlertPending) ExpenseCoral else ringColor
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!timer.isRunning) {
                                FilledTonalButton(
                                    onClick = {
                                        val duration = if (timer.pausedRemainingMillis > 0L) {
                                            timer.pausedRemainingMillis
                                        } else {
                                            selectedDurationMillis
                                        }
                                        if (timer.pausedRemainingMillis > 0L) {
                                            healthRepo?.resumePostMealTimer()
                                        } else {
                                            healthRepo?.startPostMealTimer(selectedMealLabel, selectedDurationMillis)
                                        }
                                        nowMillis = System.currentTimeMillis()
                                        com.example.alarm.AlarmScheduler.schedulePostMealGlucoseAlarm(
                                            context = context,
                                            triggerAtMillis = nowMillis + duration,
                                            mealLabel = selectedMealLabel
                                        )
                                        triggerFeedback("Cronômetro de glicemia iniciado!")
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("hub_compact_start_glucose_timer_btn")
                                ) {
                                    Text(
                                        text = if (timer.pausedRemainingMillis > 0L) "Retomar" else "Iniciar Timer",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        healthRepo?.pausePostMealTimer()
                                        com.example.alarm.AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                                        triggerFeedback("Cronômetro pausado")
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("hub_compact_pause_glucose_timer_btn")
                                ) {
                                    Text("Pausar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (timer.isRunning || timer.pausedRemainingMillis > 0L || timer.completedAlertPending) {
                                OutlinedButton(
                                    onClick = {
                                        healthRepo?.resetPostMealTimer()
                                        com.example.alarm.AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                                        triggerFeedback("Cronômetro zerado")
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("hub_compact_reset_glucose_timer_btn")
                                ) {
                                    Text("Zerar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Drawer with Full Glucose Post-Meal Countdown Timer, Quick Daily Trackers (+1), and Latest Measurements
            AnimatedVisibility(
                visible = expanded,
                enter = hubOrganicEnterTransition(),
                exit = hubOrganicExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dedicated Glicemia & Cronômetro Pós-Refeição Card inside Hub when diabetesEnabled == true
                    if (customizationConfig.diabetesEnabled) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ringColor.copy(alpha = 0.11f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ringColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .testTag("hub_glucose_timer_expanded_card")
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
                                            text = "🩸 Glicemia & Cronômetro Pós-Refeição",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = latestGlucose?.let {
                                                "Última: ${it.valueMgDl} mg/dL (${it.slot} • ${it.timeFormatted})"
                                            } ?: "Nenhuma medição registrada hoje",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formattedCountdown,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (timer.completedAlertPending) ExpenseCoral else ringColor
                                    )
                                }

                                // Duration preset chips (1h, 1h30, 2h)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tempo:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    listOf(
                                        "1h" to (60L * 60L * 1000L),
                                        "1h30" to (90L * 60L * 1000L),
                                        "2h" to (120L * 60L * 1000L)
                                    ).forEach { (label, millis) ->
                                        FilterChip(
                                            selected = selectedDurationMillis == millis,
                                            onClick = {
                                                selectedDurationMillis = millis
                                                if (!timer.isRunning) {
                                                    healthRepo?.resetPostMealTimer()
                                                }
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(50)
                                        )
                                    }
                                }

                                // Meal selector chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Café", "Almoço", "Lanche", "Jantar").forEach { meal ->
                                        FilterChip(
                                            selected = selectedMealLabel == meal,
                                            onClick = { selectedMealLabel = meal },
                                            label = { Text(meal, style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(50)
                                        )
                                    }
                                }

                                // Timer action buttons (Iniciar / Pausar / Zerar)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!timer.isRunning) {
                                        Button(
                                            onClick = {
                                                val duration = if (timer.pausedRemainingMillis > 0L) {
                                                    timer.pausedRemainingMillis
                                                } else {
                                                    selectedDurationMillis
                                                }
                                                if (timer.pausedRemainingMillis > 0L) {
                                                    healthRepo?.resumePostMealTimer()
                                                } else {
                                                    healthRepo?.startPostMealTimer(selectedMealLabel, selectedDurationMillis)
                                                }
                                                nowMillis = System.currentTimeMillis()
                                                com.example.alarm.AlarmScheduler.schedulePostMealGlucoseAlarm(
                                                    context = context,
                                                    triggerAtMillis = nowMillis + duration,
                                                    mealLabel = selectedMealLabel
                                                )
                                                triggerFeedback("Alarme de glicemia agendado!")
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_start_glucose_timer_btn")
                                        ) {
                                            Text(
                                                text = if (timer.pausedRemainingMillis > 0L) "Retomar Timer" else "Iniciar Cronômetro",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = {
                                                healthRepo?.pausePostMealTimer()
                                                com.example.alarm.AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                                                triggerFeedback("Cronômetro pausado")
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_pause_glucose_timer_btn")
                                        ) {
                                            Text("Pausar", maxLines = 1, softWrap = false)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            healthRepo?.resetPostMealTimer()
                                            com.example.alarm.AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                                            triggerFeedback("Cronômetro zerado")
                                        },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("hub_reset_glucose_timer_btn")
                                    ) {
                                        Text("Zerar", maxLines = 1, softWrap = false)
                                    }
                                }

                                // Quick Glucose Log inside Hub
                                if (healthRepo != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = quickGlucoseValueInput,
                                            onValueChange = { quickGlucoseValueInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                            label = { Text("Registrar Glicemia (mg/dL)") },
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("hub_quick_glucose_input")
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                val mgDl = quickGlucoseValueInput.toIntOrNull()
                                                if (mgDl != null && mgDl in 20..600) {
                                                    val slot = when (selectedMealLabel) {
                                                        "Café" -> "1h pós-café"
                                                        "Almoço" -> "1h pós-almoço"
                                                        "Jantar" -> "1h pós-jantar"
                                                        else -> "Pós-$selectedMealLabel"
                                                    }
                                                    val cal = java.util.Calendar.getInstance()
                                                    val timeStr = String.format(
                                                        java.util.Locale.getDefault(),
                                                        "%02d:%02d",
                                                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                                                        cal.get(java.util.Calendar.MINUTE)
                                                    )
                                                    healthRepo.addGlucoseRecord(
                                                        dateIso = todayIso,
                                                        timeFormatted = timeStr,
                                                        slot = slot,
                                                        valueMgDl = mgDl,
                                                        notes = "Registrado pelo Hub"
                                                    )
                                                    healthRepo.resetPostMealTimer()
                                                    com.example.alarm.AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                                                    quickGlucoseValueInput = ""
                                                    triggerFeedback("Glicemia ($mgDl mg/dL) salva com sucesso!")
                                                }
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.testTag("hub_quick_glucose_save_btn")
                                        ) {
                                            Text("Salvar", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                                    onClick = {
                                        onIncrementTracker(todayIso, habit.id, habit.stepValue)
                                        triggerFeedback("${habit.title} +${habit.stepValue} registrado!")
                                    },
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

                    // Latest clinical measurements summary chips (only for marked questionnaire modules)
                    if (customizationConfig.diabetesEnabled || customizationConfig.bloodPressureEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (customizationConfig.diabetesEnabled) {
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
                            }

                            if (customizationConfig.bloodPressureEnabled) {
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
                        }
                    }

                    Button(
                        onClick = onOpenHealthScreen,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_open_health_module_btn")
                    ) {
                        Text(
                            if (customizationConfig.diabetesEnabled) {
                                "Abrir Módulo de Saúde, Glicemia & Relatório PDF"
                            } else {
                                "Abrir Módulo de Saúde & Relatório PDF"
                            }
                        )
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
