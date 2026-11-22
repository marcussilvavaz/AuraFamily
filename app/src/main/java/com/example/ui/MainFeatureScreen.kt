package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppCustomizationConfig
import com.example.data.AppUpdateManager
import com.example.data.CustomizationRepository
import com.example.data.HealthRepository
import com.example.data.Household
import com.example.data.LocalOfflineStore
import com.example.data.PrivateDiaryRepository
import com.example.data.UiState
import com.example.viewmodel.AuraViewModel

private data class NavDestination(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainFeatureScreen(
    viewModel: AuraViewModel,
    customizationConfig: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    userEmail: String?,
    onSignOut: () -> Unit,
    sharedUpdateManager: AppUpdateManager? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val healthRepo = remember { HealthRepository(context.applicationContext) }
    val healthState by healthRepo.stateFlow.collectAsStateWithLifecycle()

    val updateManager = sharedUpdateManager ?: remember { AppUpdateManager(context.applicationContext) }
    val updateState by updateManager.uiState.collectAsStateWithLifecycle()

    val achievementsRepo = remember { com.example.data.AchievementsRepository(context.applicationContext) }
    val persistedAchievementsState by achievementsRepo.persistedStateFlow.collectAsStateWithLifecycle()

    // Tabs: 0 = Hub, 1 = Agenda, 2 = Finanças, 3 = Perfil & Configurações, 4 = Estúdio Visual Completo, 5 = Saúde, 6 = Diário Privado, 7 = Conquistas & Medalhas
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    // Inside Finance Screen (Tab 2): 0 = Visão Geral, 1 = Gastos & Fixas, 2 = Economia 50/30/20
    var financeSubSection by rememberSaveable { mutableIntStateOf(0) }
    var localStudioBanner by remember { mutableStateOf<String?>(null) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var presetTaskStartHour by remember { mutableIntStateOf(9) }
    var presetTaskStartMinute by remember { mutableIntStateOf(0) }
    var presetTaskDuration by remember { mutableIntStateOf(30) }
    var presetTaskIsInbox by remember { mutableStateOf(false) }
    var presetTaskScope by remember { mutableStateOf(com.example.data.PrivacyScope.FAMILY) }

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showModularOnboardingModal by rememberSaveable { mutableStateOf(false) }
    var showVoiceCommandSheet by remember { mutableStateOf(false) }
    var startVoiceListeningImmediately by remember { mutableStateOf(true) }

    // Mandatory BackHandler for secondary screens and manual onboarding modal
    if (showModularOnboardingModal) {
        BackHandler {
            showModularOnboardingModal = false
        }
    } else if (currentTab != 0) {
        BackHandler {
            currentTab = if (currentTab == 4) 3 else 0
        }
    }

    // PASSO 1: Exibir "Meu Perfil e Módulos" no primeiro acesso como onboarding
    if (!customizationConfig.hasCompletedModularOnboarding || showModularOnboardingModal) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppUpdateTopPillBanner(
                updateState = updateState,
                onOpenUpdateModal = { updateManager.reopenUpdateModal() },
                onClearUpgradedBanner = { updateManager.clearJustUpgradedBanner() }
            )
            Box(modifier = Modifier.weight(1f)) {
                ModularProfileOnboardingScreen(
                    config = customizationConfig,
                    customizationRepo = customizationRepo,
                    onFinishOnboarding = {
                        showModularOnboardingModal = false
                        localStudioBanner = "Perfil Modular configurado com sucesso! Suas preferências de rotina, saúde e acolhimento estão ativas."
                    }
                )
            }
        }
        AppUpdateAvailableDialog(
            updateState = updateState,
            updateManager = updateManager,
            onShowMessage = { localStudioBanner = it }
        )
        return
    }

    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val selectedEpochDay by viewModel.selectedEpochDay.collectAsStateWithLifecycle()
    val currencyRates by viewModel.currencyRates.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val householdState by viewModel.householdState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val subscriptionsState by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val savingsGoalsState by viewModel.savingsGoalsState.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val effectiveDiaryUserId = userProfile?.userId ?: LocalOfflineStore.LOCAL_USER_ID
    val diaryRepo = remember(effectiveDiaryUserId) {
        PrivateDiaryRepository(context.applicationContext, effectiveDiaryUserId)
    }
    val diaryState by diaryRepo.stateFlow.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, updateManager, diaryRepo) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateManager.checkNow()
                diaryRepo.refreshShiftQuote(forceNewQuote = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val household = (householdState as? UiState.Success)?.data
        ?: Household(
            householdId = userProfile?.householdId ?: "FAM_PADRAO",
            name = "Orçamento Familiar"
        )
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val subscriptions = (subscriptionsState as? UiState.Success)?.data.orEmpty()
    val savingsGoals = (savingsGoalsState as? UiState.Success)?.data.orEmpty()

    val achievementsSummary = remember(
        persistedAchievementsState,
        healthState,
        customizationConfig,
        tasks,
        transactions,
        subscriptions,
        savingsGoals,
        household,
        selectedYearMonth
    ) {
        achievementsRepo.evaluateAchievements(
            healthState = healthState,
            customizationConfig = customizationConfig,
            tasks = tasks,
            transactions = transactions,
            subscriptions = subscriptions,
            savingsGoals = savingsGoals,
            household = household,
            selectedYearMonth = selectedYearMonth
        )
    }

    // Keep repository member color synced with persisted profile color on startup
    LaunchedEffect(customizationConfig.profileName, customizationConfig.profileColorHex) {
        if (customizationConfig.profileName.isNotBlank()) {
            viewModel.updateUserProfileIdentity(
                displayName = customizationConfig.profileName,
                memberColorHex = customizationConfig.profileColorHex
            )
        }
    }

    val navItems = remember {
        listOf(
            NavDestination(0, "Hub", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_dashboard"),
            NavDestination(1, "Agenda", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_tasks"),
            NavDestination(5, "Saúde", Icons.Filled.MonitorHeart, Icons.Outlined.MonitorHeart, "nav_tab_health"),
            NavDestination(6, "Diário", Icons.Filled.Book, Icons.Outlined.Book, "nav_tab_diary"),
            NavDestination(2, "Finanças", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_tab_finances"),
            NavDestination(3, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_family")
        )
    }

    val activeBanner = localStudioBanner ?: statusBanner

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                GlobalVoiceMicFab(
                    onOpenVoiceCommandSheet = { listenImmediately ->
                        startVoiceListeningImmediately = listenImmediately
                        showVoiceCommandSheet = true
                    },
                    onPermissionDeniedMessage = { msg ->
                        localStudioBanner = msg
                    },
                    modifier = Modifier.padding(bottom = if (currentTab == 1) 72.dp else 0.dp)
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    FloatingOrganicPillNavBar(
                        items = navItems,
                        selectedIndex = if (currentTab == 4) 3 else currentTab,
                        customizationConfig = customizationConfig,
                        onSelect = { currentTab = it }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        navItems.forEach { dest ->
                            val selected = (if (currentTab == 4) 3 else currentTab) == dest.index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest.index },
                                icon = {
                                    if (dest.index == 3) {
                                        ProfileAvatarBadge(
                                            profileName = customizationConfig.profileName,
                                            profileColorHex = customizationConfig.profileColorHex,
                                            profilePhotoPath = customizationConfig.profilePhotoPath,
                                            profilePhotoVersion = customizationConfig.profilePhotoVersion,
                                            size = 26.dp,
                                            testTag = "rail_profile_avatar"
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                    ) {
                        AppUpdateTopPillBanner(
                            updateState = updateState,
                            onOpenUpdateModal = { updateManager.reopenUpdateModal() },
                            onClearUpgradedBanner = { updateManager.clearJustUpgradedBanner() }
                        )
                    }

                    AnimatedVisibility(visible = activeBanner != null) {
                        activeBanner?.let { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(50),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            localStudioBanner = null
                                            viewModel.clearStatusBanner()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Fechar aviso"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (currentTab) {
                            // 0: HUB DA FAMÍLIA (Smartwatch Expandable Widgets + Profile Avatar Header)
                            0 -> FamilyHubScreen(
                                selectedYearMonth = selectedYearMonth,
                                userProfile = userProfile,
                                household = household,
                                tasks = tasks,
                                transactions = transactions,
                                subscriptions = subscriptions,
                                savingsGoals = savingsGoals,
                                currencyRates = currencyRates,
                                customizationConfig = customizationConfig,
                                healthState = healthState,
                                diaryState = diaryState,
                                achievementsSummary = achievementsSummary,
                                userEmail = userEmail,
                                onRequestGoogleLogin = onSignOut,
                                onIncrementHealthTracker = { dateIso, trackerId, step ->
                                    healthRepo.incrementTrackerProgress(dateIso, trackerId, step)
                                },
                                onQuickAdvanceAchievement = { goal ->
                                    val unlocked = achievementsRepo.incrementGoalProgress(
                                        goalId = goal.id,
                                        step = 1,
                                        targetValue = goal.targetValue
                                    )
                                    localStudioBanner = if (unlocked) {
                                        "🏆 Parabéns! Você conquistou a ${goal.medalTier.badgeTitle} (${goal.medalTier.emoji}): ${goal.title} (+${goal.medalTier.xpReward} XP)!"
                                    } else {
                                        "🎯 Progresso registrado em '${goal.title}'!"
                                    }
                                },
                                onNextShiftQuote = { diaryRepo.refreshShiftQuote(forceNewQuote = true) },
                                onOpenHealthScreen = { currentTab = 5 },
                                onOpenDiaryScreen = { currentTab = 6 },
                                onOpenAchievementsScreen = { currentTab = 7 },
                                onShiftMonth = { viewModel.shiftMonth(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(context, it) },
                                onPostSubscriptionNow = { viewModel.postSingleSubscriptionNow(it) },
                                onOpenNewTaskDialog = {
                                    presetTaskStartHour = 9
                                    presetTaskStartMinute = 0
                                    presetTaskDuration = 30
                                    presetTaskIsInbox = false
                                    presetTaskScope = com.example.data.PrivacyScope.FAMILY
                                    showAddTaskDialog = true
                                },
                                onOpenNewTransactionDialog = { showAddTransactionDialog = true },
                                onOpenAgendaScreen = { currentTab = 1 },
                                onOpenFinanceScreen = { subSec ->
                                    financeSubSection = subSec
                                    currentTab = 2
                                },
                                onOpenProfileSettings = { currentTab = 3 }
                            )

                            // 1: AGENDA & TIME BLOCKING (Passo 1 + Passo 2: Timeline, Horas Ociosas, Inbox, Ringtones, Financeiro)
                            1 -> TasksCalendarScreen(
                                selectedYearMonth = selectedYearMonth,
                                selectedEpochDay = selectedEpochDay,
                                tasks = tasks,
                                subscriptions = subscriptions,
                                customizationConfig = customizationConfig,
                                customizationRepo = customizationRepo,
                                onShiftMonth = { viewModel.shiftMonth(it) },
                                onSelectEpochDay = { viewModel.selectEpochDay(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(context, it) },
                                onToggleTaskEssential = { viewModel.toggleTaskEssential(it) },
                                onToggleSubtask = { task, subtaskId -> viewModel.toggleSubtaskDone(task, subtaskId) },
                                onScheduleInboxTask = { task, epochDay, hour, minute, duration ->
                                    viewModel.scheduleInboxTask(context, task, epochDay, hour, minute, duration)
                                },
                                onPostSubscriptionNow = { viewModel.postSingleSubscriptionNow(it) },
                                onUpdateSubscriptionRingtone = { bill, ringtone ->
                                    viewModel.updateSubscriptionRingtone(bill, ringtone)
                                },
                                onDeleteTask = { viewModel.deleteTask(context, it) },
                                onOpenNewTaskDialog = {
                                    presetTaskStartHour = 9
                                    presetTaskStartMinute = 0
                                    presetTaskDuration = 30
                                    presetTaskIsInbox = false
                                    presetTaskScope = com.example.data.PrivacyScope.FAMILY
                                    showAddTaskDialog = true
                                },
                                onOpenNewTaskWithPreset = { startHour, startMinute, suggestedDur, isInbox, defaultScope ->
                                    presetTaskStartHour = startHour
                                    presetTaskStartMinute = startMinute
                                    presetTaskDuration = suggestedDur
                                    presetTaskIsInbox = isInbox
                                    presetTaskScope = defaultScope
                                    showAddTaskDialog = true
                                }
                            )

                            // 2: FINANÇAS DA FAMÍLIA
                            2 -> Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        0 to "Visão Geral",
                                        1 to "Gastos & Fixas",
                                        2 to "Economia 50/30/20"
                                    ).forEach { (idx, title) ->
                                        FilterChip(
                                            selected = financeSubSection == idx,
                                            onClick = { financeSubSection = idx },
                                            label = {
                                                Text(
                                                    text = title,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("finance_subtab_$idx")
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (financeSubSection) {
                                        0 -> DashboardScreen(
                                            selectedYearMonth = selectedYearMonth,
                                            household = household,
                                            transactions = transactions,
                                            subscriptions = subscriptions,
                                            savingsGoals = savingsGoals,
                                            currencyRates = currencyRates,
                                            onShiftMonth = { viewModel.shiftMonth(it) },
                                            onRefreshRates = { viewModel.refreshExchangeRates() },
                                            onOpenNewTransactionDialog = { showAddTransactionDialog = true },
                                            onAutoPostRecurringBills = { viewModel.autoPostDueSubscriptionsForSelectedMonth() },
                                            onOpenFinanceSubSection = { financeSubSection = it }
                                        )

                                        1 -> FinancesScreen(
                                            selectedYearMonth = selectedYearMonth,
                                            transactions = transactions,
                                            subscriptions = subscriptions,
                                            currencyRates = currencyRates,
                                            onRefreshRates = { viewModel.refreshExchangeRates() },
                                            onUpdateManualRate = { code, rate -> viewModel.updateManualRate(code, rate) },
                                            onOpenNewTransactionDialog = { showAddTransactionDialog = true },
                                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                            onAddSubscription = { title, amountOrig, currency, cat, bucket, dueDay, remDays, autoPost, privacyScope, syncToAgenda ->
                                                viewModel.createSubscriptionBill(
                                                    context = context,
                                                    title = title,
                                                    amountOriginal = amountOrig,
                                                    currency = currency,
                                                    category = cat,
                                                    budgetBucket = bucket,
                                                    dueDayOfMonth = dueDay,
                                                    reminderDaysBefore = remDays,
                                                    autoPostEnabled = autoPost,
                                                    privacyScope = privacyScope,
                                                    syncDueDayToCalendar = syncToAgenda,
                                                    overrideMemberColorHex = customizationConfig.profileColorHex
                                                )
                                            },
                                            onPostSubscriptionNow = { viewModel.postSingleSubscriptionNow(it) },
                                            onAutoPostAllSubscriptions = { viewModel.autoPostDueSubscriptionsForSelectedMonth() },
                                            onDeleteSubscription = { viewModel.deleteSubscription(it) }
                                        )

                                        else -> SavingsReportsScreen(
                                            selectedYearMonth = selectedYearMonth,
                                            household = household,
                                            transactions = transactions,
                                            savingsGoals = savingsGoals,
                                            onSaveBudgetPlan = { name, income, needs, wants, savings ->
                                                viewModel.saveHouseholdBudgetPlan(name, income, needs, wants, savings)
                                            },
                                            onAddSavingsGoal = { title, target, current, pct, cat, targetYm ->
                                                viewModel.createSavingsGoal(title, target, current, pct, cat, targetYm)
                                            },
                                            onContributeGoal = { goal, amount, recordInMonth ->
                                                viewModel.contributeToGoal(goal, amount, recordInMonth)
                                            },
                                            onDeleteGoal = { viewModel.deleteSavingsGoal(it) }
                                        )
                                    }
                                }
                            }

                            // 3: MENU DO PERFIL & CONFIGURAÇÕES (Opened by clicking the Profile Avatar)
                            3 -> FamilySettingsScreen(
                                userProfile = userProfile,
                                household = household,
                                customizationConfig = customizationConfig,
                                customizationRepo = customizationRepo,
                                userEmail = userEmail,
                                onUpdateProfileIdentity = { displayName, memberColorHex ->
                                    viewModel.updateUserProfileIdentity(displayName, memberColorHex)
                                },
                                onJoinHousehold = { code, famName ->
                                    viewModel.joinOrCreateHousehold(code, famName)
                                },
                                onChangeThemeMode = { viewModel.setThemeMode(it) },
                                onOpenCustomizationStudio = { currentTab = 4 },
                                onOpenModularOnboardingScreen = { showModularOnboardingModal = true },
                                achievementsSummary = achievementsSummary,
                                onOpenAchievementsScreen = { currentTab = 7 },
                                updateState = updateState,
                                updateManager = updateManager,
                                onBackToHub = { currentTab = 0 },
                                onShowMessage = { localStudioBanner = it },
                                onSignOut = onSignOut
                            )

                            // 4: ESTÚDIO DE PERSONALIZAÇÃO TOTAL (Colors, 8 Themes, 10 Fonts, Ringtones)
                            4 -> CustomizationStudioScreen(
                                config = customizationConfig,
                                customizationRepo = customizationRepo,
                                onShowMessage = { localStudioBanner = it }
                            )

                            // 5: PASSO 3 — SAÚDE, INEGOCIÁVEIS, GESTAÇÃO, SUGESTÕES FINANCEIRAS E RELATÓRIO PDF NATIVO
                            5 -> HealthModuleScreen(
                                selectedEpochDay = selectedEpochDay,
                                customizationConfig = customizationConfig,
                                customizationRepo = customizationRepo,
                                healthRepo = healthRepo,
                                onCreateHealthFinanceExpense = { title, amountBrl, category, scope ->
                                    viewModel.createTransaction(
                                        title = title,
                                        amountOriginal = amountBrl,
                                        currency = "BRL",
                                        type = "EXPENSE",
                                        budgetBucket = "NEEDS",
                                        category = category,
                                        dateEpochDay = selectedEpochDay,
                                        privacyScope = scope,
                                        generateCalendarTask = true,
                                        overrideMemberColorHex = customizationConfig.profileColorHex
                                    )
                                },
                                onShowBannerMessage = { localStudioBanner = it }
                            )

                            // 6: PASSO 4 — DIÁRIO PRIVADO, FRASES POR TURNO E CARTA DE APOIO VIA IA
                            6 -> PrivateDiaryScreen(
                                initialEpochDay = selectedEpochDay,
                                customizationConfig = customizationConfig,
                                diaryRepo = diaryRepo,
                                onShowBannerMessage = { localStudioBanner = it }
                            )

                            // 7: GALERIA DE CONQUISTAS & MEDALHAS (Vinculada a Saúde, Financeiro e Calendário)
                            7 -> AchievementsMedalsScreen(
                                summary = achievementsSummary,
                                customizationConfig = customizationConfig,
                                onAdvanceGoalProgress = { goal ->
                                    val unlocked = achievementsRepo.incrementGoalProgress(
                                        goalId = goal.id,
                                        step = 1,
                                        targetValue = goal.targetValue
                                    )
                                    localStudioBanner = if (unlocked) {
                                        "🏆 Parabéns! Você conquistou a ${goal.medalTier.badgeTitle} (${goal.medalTier.emoji}): ${goal.title} (+${goal.medalTier.xpReward} XP)!"
                                    } else {
                                        "🎯 +1 etapa concluída no objetivo '${goal.title}'!"
                                    }
                                },
                                onCreateCustomGoal = { title, desc, module, tier, target, unit ->
                                    val created = achievementsRepo.addCustomAchievementGoal(
                                        title = title,
                                        description = desc,
                                        module = module,
                                        medalTier = tier,
                                        targetValue = target,
                                        unitLabel = unit
                                    )
                                    localStudioBanner = "🏅 Novo objetivo '${created.title}' criado para ${created.module.shortLabel} valendo ${created.medalTier.badgeTitle}!"
                                },
                                onDeleteCustomGoal = { goalId ->
                                    achievementsRepo.deleteCustomGoal(goalId)
                                    localStudioBanner = "Objetivo personalizado removido."
                                },
                                onNavigateToModule = { module ->
                                    currentTab = when (module) {
                                        com.example.data.AchievementModule.HEALTH -> 5
                                        com.example.data.AchievementModule.FINANCE -> 2
                                        com.example.data.AchievementModule.CALENDAR -> 1
                                        com.example.data.AchievementModule.TRINITY -> 0
                                    }
                                },
                                onBack = { currentTab = 0 }
                            )
                        }
                    }
                }
            }
        }
    }

    // Real-time In-App Update Modal Dialog (triggers automatically when new changes are built)
    AppUpdateAvailableDialog(
        updateState = updateState,
        updateManager = updateManager,
        onShowMessage = { localStudioBanner = it }
    )

    // PASSO 4: Botão Global de Microfone -> Bottom Sheet Rápido de Confirmação
    if (showVoiceCommandSheet) {
        GlobalVoiceCommandBottomSheet(
            referenceEpochDay = selectedEpochDay,
            startListeningImmediately = startVoiceListeningImmediately,
            onDismiss = { showVoiceCommandSheet = false },
            onSaveExpense = { title, amountBrl, category, privacyScope ->
                viewModel.createTransaction(
                    title = title,
                    amountOriginal = amountBrl,
                    currency = "BRL",
                    type = "EXPENSE",
                    budgetBucket = "NEEDS",
                    category = category,
                    dateEpochDay = selectedEpochDay,
                    privacyScope = privacyScope,
                    generateCalendarTask = true,
                    overrideMemberColorHex = customizationConfig.profileColorHex
                )
                localStudioBanner = "💸 Despesa por voz salva: $title (R$ ${String.format(java.util.Locale.US, "%.2f", amountBrl)} em $category)!"
            },
            onSaveTask = { title, epochDay, hour, minute, category, isEssential, privacyScope ->
                viewModel.createTask(
                    context = context,
                    title = title,
                    notes = "Criada por Comando de Voz",
                    category = category,
                    priority = if (isEssential) "HIGH" else "MEDIUM",
                    dateEpochDay = epochDay,
                    alarmHour = hour,
                    alarmMinute = minute,
                    alarmEnabled = true,
                    privacyScope = privacyScope,
                    estimatedCostBrl = 0.0,
                    autoCreateExpenseOnComplete = false,
                    overrideMemberColorHex = customizationConfig.profileColorHex,
                    isEssential = isEssential,
                    isInbox = false,
                    durationMinutes = 30
                )
                localStudioBanner = "📅 Tarefa por voz agendada: $title às ${String.format(java.util.Locale.US, "%02d:%02d", hour, minute)}!"
            },
            onSaveDiaryEntry = { epochDay, hour, minute, moodId, symptomTags, freeText ->
                diaryRepo.addDiaryEntry(
                    epochDay = epochDay,
                    hour = hour,
                    minute = minute,
                    moodId = moodId,
                    symptomTags = symptomTags,
                    freeText = freeText
                )
                localStudioBanner = "📔 Entrada por voz salva no seu Diário Privado!"
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            initialEpochDay = selectedEpochDay,
            defaultProfileColorHex = customizationConfig.profileColorHex,
            initialStartHour = presetTaskStartHour,
            initialStartMinute = presetTaskStartMinute,
            initialDurationMinutes = presetTaskDuration,
            initialIsInbox = presetTaskIsInbox,
            initialPrivacyScope = presetTaskScope,
            onDismiss = { showAddTaskDialog = false },
            onSaveTask = { title, notes, category, priority, dateEpoch, hour, min, alarmEnabled, privacyScope, estimatedCostBrl, autoCreateExpense, isEssential, isInbox, durationMinutes, iconEmoji, taskColorHex, recurrence, subtasks, ringtoneCategory ->
                viewModel.createTask(
                    context = context,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    dateEpochDay = dateEpoch,
                    alarmHour = hour,
                    alarmMinute = min,
                    alarmEnabled = alarmEnabled,
                    privacyScope = privacyScope,
                    estimatedCostBrl = estimatedCostBrl,
                    autoCreateExpenseOnComplete = autoCreateExpense,
                    overrideMemberColorHex = customizationConfig.profileColorHex,
                    isEssential = isEssential,
                    isInbox = isInbox,
                    durationMinutes = durationMinutes,
                    iconEmoji = iconEmoji,
                    taskColorHex = taskColorHex,
                    recurrence = recurrence,
                    subtasks = subtasks,
                    ringtoneCategory = ringtoneCategory
                )
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            initialEpochDay = selectedEpochDay,
            currencyRates = currencyRates,
            onDismiss = { showAddTransactionDialog = false },
            onSaveTransaction = { title, amountOrig, currency, type, bucket, cat, dateEpoch, privacyScope, generateCalendarTask ->
                viewModel.createTransaction(
                    title = title,
                    amountOriginal = amountOrig,
                    currency = currency,
                    type = type,
                    budgetBucket = bucket,
                    category = cat,
                    dateEpochDay = dateEpoch,
                    privacyScope = privacyScope,
                    generateCalendarTask = generateCalendarTask,
                    overrideMemberColorHex = customizationConfig.profileColorHex
                )
            }
        )
    }
}

@Composable
private fun FloatingOrganicPillNavBar(
    items: List<NavDestination>,
    selectedIndex: Int,
    customizationConfig: AppCustomizationConfig,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { dest ->
                    val selected = selectedIndex == dest.index
                    val moduleColor = remember(dest.index, customizationConfig) {
                        when (dest.index) {
                            0 -> Color(customizationConfig.hubColorHex)
                            1 -> Color(customizationConfig.agendaColorHex)
                            5 -> Color(customizationConfig.healthColorHex)
                            6 -> Color(customizationConfig.diaryColorHex)
                            2 -> Color(customizationConfig.financeColorHex)
                            else -> Color(customizationConfig.profileColorHex)
                        }
                    }
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) {
                            moduleColor.copy(alpha = 0.22f)
                        } else {
                            Color.Transparent
                        },
                        label = "navPillBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            moduleColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "navPillContent"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .clickable { onSelect(dest.index) }
                            .testTag(dest.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (dest.index == 3) {
                                ProfileAvatarBadge(
                                    profileName = customizationConfig.profileName,
                                    profileColorHex = customizationConfig.profileColorHex,
                                    profilePhotoPath = customizationConfig.profilePhotoPath,
                                    profilePhotoVersion = customizationConfig.profilePhotoVersion,
                                    size = 24.dp,
                                    testTag = "bottom_nav_profile_avatar"
                                )
                            } else {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
