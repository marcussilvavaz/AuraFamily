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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.FinanceTransaction
import com.example.data.HealthRepository
import com.example.data.Household
import com.example.data.LocalOfflineStore
import com.example.data.PrivateDiaryRepository
import com.example.data.TaskItem
import com.example.data.UiState
import com.example.viewmodel.AuraViewModel
import kotlinx.coroutines.launch

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
    sharedUpdateManager: AppUpdateManager? = null,
    pendingWidgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val healthRepo = remember { HealthRepository(context.applicationContext) }
    val healthState by healthRepo.stateFlow.collectAsStateWithLifecycle()

    val updateManager = sharedUpdateManager ?: remember { AppUpdateManager(context.applicationContext) }
    val updateState by updateManager.uiState.collectAsStateWithLifecycle()

    val achievementsRepo = remember { com.example.data.AchievementsRepository(context.applicationContext) }
    val persistedAchievementsState by achievementsRepo.persistedStateFlow.collectAsStateWithLifecycle()

    // Tabs: 0 = Hub, 1 = Agenda, 2 = Finanças, 3 = Perfil & Configurações, 4 = Estúdio Visual Completo, 5 = Saúde, 6 = Diário Privado & Mural, 7 = Conquistas & Medalhas
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    // Inside Finance Screen (Tab 2): 0 = Visão Geral, 1 = Cartões & Faturas, 2 = Metas & Fixas
    var financeSubSection by rememberSaveable { mutableIntStateOf(0) }
    // Inside Diary Screen (Tab 6): 0 = Meu Diário (Bloqueado), 1 = Mural da Família (Compartilhado)
    var diaryInitialSubTab by rememberSaveable { mutableIntStateOf(0) }
    var localStudioBanner by remember { mutableStateOf<String?>(null) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var presetTaskStartHour by remember { mutableIntStateOf(9) }
    var presetTaskStartMinute by remember { mutableIntStateOf(0) }
    var presetTaskDuration by remember { mutableIntStateOf(30) }
    var presetTaskIsInbox by remember { mutableStateOf(false) }
    var presetTaskScope by remember { mutableStateOf(com.example.data.PrivacyScope.FAMILY) }

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    var showModularOnboardingModal by rememberSaveable { mutableStateOf(false) }
    var showVoiceCommandSheet by remember { mutableStateOf(false) }
    var startVoiceListeningImmediately by remember { mutableStateOf(true) }
    var showQuickCreateChooserModal by remember { mutableStateOf(false) }

    // Handle Home Screen Widget deep-link actions (Comando de Voz, + Novo Lançamento, Agenda, Glicemia, Hub)
    LaunchedEffect(pendingWidgetAction) {
        val action = pendingWidgetAction
        if (!action.isNullOrBlank()) {
            when (action) {
                com.example.widget.OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_VOICE -> {
                    startVoiceListeningImmediately = true
                    showVoiceCommandSheet = true
                }
                com.example.widget.OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_NEW_TRANSACTION -> {
                    currentTab = 2
                    financeSubSection = 0
                    editingTransaction = null
                    showAddTransactionDialog = true
                }
                com.example.widget.OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_AGENDA -> {
                    viewModel.selectEpochDay(com.example.data.DateCalendarUtils.todayEpochDay())
                    currentTab = 1
                }
                com.example.widget.OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_GLUCOSE -> {
                    currentTab = 5
                }
                com.example.widget.OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_HUB -> {
                    currentTab = 0
                }
            }
            onConsumeWidgetAction()
        }
    }

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

    val isRestoringFromCloud by viewModel.isRestoringFromCloud.collectAsStateWithLifecycle()
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
    val loadedHousehold = (householdState as? UiState.Success)?.data

    // Quando o perfil/família for restaurado da nuvem, sincroniza nome, cor, avatarBase64 e marca onboarding como concluído se já houver perfil válido na nuvem
    LaunchedEffect(userProfile?.displayName, userProfile?.memberColorHex, userProfile?.avatarBase64, loadedHousehold?.householdId) {
        val remoteName = userProfile?.displayName?.trim().orEmpty()
        if (remoteName.isNotBlank() &&
            !remoteName.equals("Você", ignoreCase = true) &&
            !remoteName.equals("VO", ignoreCase = true)
        ) {
            if (customizationConfig.profileName.isBlank() ||
                customizationConfig.profileName.equals("Você", ignoreCase = true) ||
                customizationConfig.profileName.equals("VO", ignoreCase = true)
            ) {
                customizationRepo.updateProfileIdentity(
                    name = remoteName,
                    colorHex = userProfile?.memberColorHex ?: customizationConfig.profileColorHex
                )
            }
            if (!customizationConfig.hasCompletedModularOnboarding && !loadedHousehold?.householdId.isNullOrBlank()) {
                customizationRepo.completeModularOnboarding()
            }
        }
        val remoteAvatar = userProfile?.avatarBase64.orEmpty()
        if (remoteAvatar.isNotBlank() && remoteAvatar != customizationConfig.profileAvatarBase64) {
            customizationRepo.updateProfileAvatarBase64(remoteAvatar)
        }
    }

    // FASE "NUVEM PRIMEIRO": Exibe tela de carregamento "Restaurando seus dados da nuvem..." antes de abrir onboarding ou Hub
    if (isRestoringFromCloud) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Restaurando seus dados da nuvem...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sincronizando sua família oficial, agenda, finanças, saúde e mural no Cloud Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        return
    }

    // PASSO 1: Exibir "Meu Perfil e Módulos" no primeiro acesso como onboarding
    if ((!customizationConfig.hasCompletedModularOnboarding && pendingWidgetAction.isNullOrBlank()) || showModularOnboardingModal) {
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

    val migrationRepository = remember(context) {
        com.example.data.AuraFirestoreRepository(context.applicationContext)
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Item 2: Rotina automática na inicialização que varre dados antigos (SharedPreferences aura_* e famílias anteriores no Firestore) e mescla na família atual
    LaunchedEffect(household.householdId, tasksState is UiState.Success, transactionsState is UiState.Success) {
        val activeHouseId = household.householdId.ifBlank { userProfile?.householdId.orEmpty() }
        if (activeHouseId.isNotBlank() && tasksState is UiState.Success && transactionsState is UiState.Success) {
            val result = com.example.data.OmniBackupAndMigrationManager.autoRunStartupMigrationIfNeeded(
                context = context,
                activeHouseholdId = activeHouseId,
                currentHousehold = household,
                currentTasks = tasks,
                currentTransactions = transactions,
                repository = migrationRepository,
                healthRepo = healthRepo,
                diaryRepo = diaryRepo
            )
            if (result != null && result.totalMerged > 0) {
                localStudioBanner = result.toSummaryMessagePt(isRestore = false)
            }
        }
    }

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

    LaunchedEffect(achievementsSummary.newlyUnlockedGoalTitle) {
        val unlockedTitle = achievementsSummary.newlyUnlockedGoalTitle
        if (!unlockedTitle.isNullOrBlank()) {
            localStudioBanner = "🏆 Nova Medalha Desbloqueada Automaticamente: $unlockedTitle!"
        }
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

    // Keep local Mural cache & Home Widget in sync with Firestore household.muralItems
    LaunchedEffect(household.muralItems) {
        if (household.muralItems.isNotEmpty()) {
            com.example.data.OmniBackupAndMigrationManager.syncMuralItemsFromRemote(
                context = context,
                remoteItems = household.muralItems
            )
        }
    }

    // Keep Daily Morning Summary alarm updated whenever settings or items change
    LaunchedEffect(
        customizationConfig.morningSummaryEnabled,
        customizationConfig.morningSummaryHour,
        customizationConfig.morningSummaryMinute,
        tasks.size,
        subscriptions.size,
        household.muralItems.size
    ) {
        viewModel.refreshMorningSummarySchedule(
            context = context,
            enabled = customizationConfig.morningSummaryEnabled,
            hour = customizationConfig.morningSummaryHour,
            minute = customizationConfig.morningSummaryMinute
        )
    }

    // Keep Agenda tasks and Subscription bills alarms synchronized in real time when received via SnapshotListener
    LaunchedEffect(tasks, subscriptions) {
        if (tasks.isNotEmpty() || subscriptions.isNotEmpty()) {
            viewModel.syncAlarmsWithCurrentTasksAndSubscriptions(context)
        }
    }

    // Keep Daily Blood Pressure reminder alarm scheduled when enabled in Health/Profile settings
    LaunchedEffect(customizationConfig.bloodPressureEnabled, customizationConfig.bloodPressureReminderTime) {
        com.example.alarm.AlarmScheduler.scheduleBloodPressureDailyReminder(
            context = context,
            enabled = customizationConfig.bloodPressureEnabled,
            reminderTimeRaw = customizationConfig.bloodPressureReminderTime
        )
    }

    // Keep Widget finance privacy mask synced
    LaunchedEffect(customizationConfig.financePrivacyHideValues) {
        com.example.data.OmniBackupAndMigrationManager.setWidgetFinanceBalanceHidden(
            context = context,
            hidden = customizationConfig.financePrivacyHideValues
        )
    }

    // Bottom Navigation Bar with 5 well-spaced tabs (Profile moved to top-right avatar)
    val navItems = remember {
        listOf(
            NavDestination(0, "Hub", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_dashboard"),
            NavDestination(1, "Agenda", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_tasks"),
            NavDestination(5, "Saúde", Icons.Filled.MonitorHeart, Icons.Outlined.MonitorHeart, "nav_tab_health"),
            NavDestination(6, "Diário", Icons.Filled.Book, Icons.Outlined.Book, "nav_tab_diary"),
            NavDestination(2, "Finanças", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_tab_finances")
        )
    }

    val activeBanner = localStudioBanner ?: statusBanner
    val activeRingingAlarm by com.example.alarm.TaskTonePreviewPlayer.activeRingingAlarmFlow.collectAsStateWithLifecycle()

    // Item 1: Auto-dismiss EVERY top banner after 3 seconds so it never gets stuck
    LaunchedEffect(activeBanner) {
        if (!activeBanner.isNullOrBlank()) {
            kotlinx.coroutines.delay(3000L)
            localStudioBanner = null
            viewModel.clearStatusBanner()
        }
    }

    // Item 1: Clear top banners immediately whenever the user switches tabs or opens any modal/sheet
    LaunchedEffect(
        currentTab,
        showAddTaskDialog,
        editingTask,
        showAddTransactionDialog,
        editingTransaction,
        showVoiceCommandSheet,
        showModularOnboardingModal
    ) {
        localStudioBanner = null
        viewModel.clearStatusBanner()
        updateManager.clearJustUpgradedBanner()
    }

    val isAnyModalOpen = showAddTaskDialog ||
        editingTask != null ||
        showAddTransactionDialog ||
        editingTransaction != null ||
        showVoiceCommandSheet ||
        showModularOnboardingModal ||
        showQuickCreateChooserModal

    val showSmartFab = !isAnyModalOpen && currentTab !in listOf(3, 4, 7)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (showSmartFab) {
                    SmartOmniFloatingActionButton(
                        accentColor = when (currentTab) {
                            1 -> Color(customizationConfig.agendaColorHex)
                            2 -> Color(customizationConfig.financeColorHex)
                            5 -> Color(customizationConfig.healthColorHex)
                            6 -> Color(customizationConfig.diaryColorHex)
                            else -> Color(customizationConfig.hubColorHex)
                        },
                        onQuickAddClick = {
                            when (currentTab) {
                                1 -> {
                                    editingTask = null
                                    presetTaskStartHour = 9
                                    presetTaskStartMinute = 0
                                    presetTaskDuration = 30
                                    presetTaskIsInbox = false
                                    presetTaskScope = com.example.data.PrivacyScope.FAMILY
                                    showAddTaskDialog = true
                                }
                                2 -> {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                }
                                else -> {
                                    showQuickCreateChooserModal = true
                                }
                            }
                        },
                        onVoiceLongPress = { listenImmediately ->
                            startVoiceListeningImmediately = listenImmediately
                            showVoiceCommandSheet = true
                        },
                        onPermissionDeniedMessage = { msg ->
                            localStudioBanner = msg
                        }
                    )
                }
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    FloatingOrganicPillNavBar(
                        items = navItems,
                        selectedIndex = currentTab,
                        customizationConfig = customizationConfig,
                        onSelect = { tabIdx ->
                            if (tabIdx == 6) {
                                diaryInitialSubTab = 0
                            }
                            currentTab = tabIdx
                        }
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
                            val selected = currentTab == dest.index
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (dest.index == 6) diaryInitialSubTab = 0
                                    currentTab = dest.index
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1, softWrap = false) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                        NavigationRailItem(
                            selected = currentTab == 3 || currentTab == 4,
                            onClick = { currentTab = 3 },
                            icon = {
                                ProfileAvatarBadge(
                                    profileName = customizationConfig.profileName.ifBlank { userProfile?.displayName.orEmpty() },
                                    profileColorHex = customizationConfig.profileColorHex,
                                    profilePhotoPath = customizationConfig.profilePhotoPath,
                                    profilePhotoVersion = customizationConfig.profilePhotoVersion,
                                    profileAvatarBase64 = customizationConfig.profileAvatarBase64.ifBlank { userProfile?.avatarBase64.orEmpty() },
                                    size = 26.dp,
                                    testTag = "rail_profile_avatar"
                                )
                            },
                            label = { Text("Perfil", maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("nav_tab_family")
                        )
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

                    // Compact Top Profile Access Bar on secondary tabs (Agenda, Saúde, Diário, Finanças) so Profile is always 1 tap away in top-right
                    if (currentTab in listOf(1, 2, 5, 6)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (currentTab) {
                                    1 -> "📅 Agenda & Rotina"
                                    2 -> "💳 Finanças & Cartões"
                                    5 -> "🩺 Saúde & Glicemia"
                                    6 -> if (diaryInitialSubTab == 1) "📌 Mural da Família" else "📔 Diário & Mural"
                                    else -> "Omni"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { currentTab = 3 }
                                    .testTag("top_bar_open_profile_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = customizationConfig.profileName.ifBlank { "Perfil" }.split(" ").firstOrNull() ?: "Perfil",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    ProfileAvatarBadge(
                                        profileName = customizationConfig.profileName.ifBlank { userProfile?.displayName.orEmpty() },
                                        profileColorHex = customizationConfig.profileColorHex,
                                        profilePhotoPath = customizationConfig.profilePhotoPath,
                                        profilePhotoVersion = customizationConfig.profilePhotoVersion,
                                        profileAvatarBase64 = customizationConfig.profileAvatarBase64.ifBlank { userProfile?.avatarBase64.orEmpty() },
                                        size = 24.dp,
                                        testTag = "top_bar_profile_avatar"
                                    )
                                }
                            }
                        }
                    }

                    // In-App Ringing Alarm Alert Card with "Parar Alarme" & "Soneca (5 min)"
                    AnimatedVisibility(visible = activeRingingAlarm != null) {
                        activeRingingAlarm?.let { ringing ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("in_app_ringing_alarm_card"),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "⏰ ${ringing.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = ringing.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                com.example.alarm.TaskTonePreviewPlayer.stopAllAudio(
                                                    context = context,
                                                    dismissRingingNotification = true
                                                )
                                                localStudioBanner = "⏹️ Alarme interrompido com sucesso."
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("btn_stop_ringing_alarm_now")
                                        ) {
                                            Text("⏹️ Parar Alarme", fontWeight = FontWeight.ExtraBold)
                                        }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                com.example.alarm.TaskTonePreviewPlayer.stopAllAudio(
                                                    context = context,
                                                    dismissRingingNotification = true
                                                )
                                                com.example.alarm.AlarmScheduler.scheduleSnoozeAlarm(
                                                    context = context,
                                                    notificationId = ringing.notificationId,
                                                    title = ringing.title,
                                                    message = ringing.message,
                                                    soundUriString = ringing.soundUriString,
                                                    vibrate = ringing.vibrate,
                                                    isBillReminder = ringing.isBillReminder,
                                                    snoozeMinutes = 5
                                                )
                                                localStudioBanner = "💤 Soneca ativada: o alarme tocará novamente em 5 minutos."
                                            },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_snooze_ringing_alarm_now")
                                        ) {
                                            Text("💤 Soneca (5 min)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
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
                                healthRepo = healthRepo,
                                diaryState = diaryState,
                                achievementsSummary = achievementsSummary,
                                userEmail = userEmail,
                                onRequestGoogleLogin = onSignOut,
                                onIncrementHealthTracker = { dateIso, trackerId, step ->
                                    healthRepo.incrementTrackerProgress(dateIso, trackerId, step)
                                },
                                onNextShiftQuote = { diaryRepo.refreshShiftQuote(forceNewQuote = true) },
                                onOpenHealthScreen = { currentTab = 5 },
                                onOpenDiaryScreen = {
                                    diaryInitialSubTab = 0
                                    currentTab = 6
                                },
                                onOpenFamilyMuralScreen = {
                                    diaryInitialSubTab = 1
                                    currentTab = 6
                                },
                                onOpenAchievementsScreen = { currentTab = 7 },
                                onShiftMonth = { viewModel.shiftMonth(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(context, it, com.example.data.DateCalendarUtils.todayEpochDay()) },
                                onPostSubscriptionNow = { viewModel.postSingleSubscriptionNow(it) },
                                onOpenNewTaskDialog = {
                                    editingTask = null
                                    presetTaskStartHour = 9
                                    presetTaskStartMinute = 0
                                    presetTaskDuration = 30
                                    presetTaskIsInbox = false
                                    presetTaskScope = com.example.data.PrivacyScope.FAMILY
                                    showAddTaskDialog = true
                                },
                                onOpenNewTransactionDialog = {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                },
                                onOpenAgendaScreen = { currentTab = 1 },
                                onOpenFinanceScreen = { subSec ->
                                    financeSubSection = subSec
                                    currentTab = 2
                                },
                                onOpenProfileSettings = { currentTab = 3 },
                                onToggleFinancePrivacy = {
                                    customizationRepo.toggleFinancePrivacyHideValues()
                                },
                                onToggleMuralItem = { itemId ->
                                    viewModel.toggleMuralItem(
                                        context = context,
                                        itemId = itemId,
                                        checkedBy = customizationConfig.profileName.ifBlank { "Família" },
                                        checkedByColorHex = customizationConfig.profileColorHex
                                    )
                                },
                                onAddQuickMuralItem = { title ->
                                    viewModel.addMuralItem(
                                        context = context,
                                        title = title,
                                        category = "Compras",
                                        addedBy = customizationConfig.profileName.ifBlank { "Família" },
                                        addedByColorHex = customizationConfig.profileColorHex
                                    )
                                }
                            )

                            // 1: AGENDA & TIME BLOCKING
                            1 -> TasksCalendarScreen(
                                selectedYearMonth = selectedYearMonth,
                                selectedEpochDay = selectedEpochDay,
                                tasks = tasks,
                                subscriptions = subscriptions,
                                transactions = transactions,
                                customizationConfig = customizationConfig,
                                customizationRepo = customizationRepo,
                                household = household,
                                onShiftMonth = { viewModel.shiftMonth(it) },
                                onSelectEpochDay = { viewModel.selectEpochDay(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(context, it, selectedEpochDay) },
                                onToggleTaskEssential = { viewModel.toggleTaskEssential(it) },
                                onToggleSubtask = { task, subtaskId -> viewModel.toggleSubtaskDone(task, subtaskId) },
                                onScheduleInboxTask = { task, epochDay, hour, minute, duration ->
                                    viewModel.scheduleInboxTask(context, task, epochDay, hour, minute, duration)
                                },
                                onPostSubscriptionNow = { viewModel.postSingleSubscriptionNow(it) },
                                onPayCreditInvoiceNow = { accId, accName ->
                                    viewModel.payAllCreditForMonth(selectedYearMonth, accId, accName)
                                },
                                onUpdateSubscriptionRingtone = { bill, ringtone ->
                                    viewModel.updateSubscriptionRingtone(bill, ringtone)
                                },
                                onEditTask = { taskToEdit ->
                                    editingTask = taskToEdit
                                },
                                onDeleteTask = { viewModel.deleteTask(context, it) },
                                onOpenNewTaskDialog = {
                                    editingTask = null
                                    presetTaskStartHour = 9
                                    presetTaskStartMinute = 0
                                    presetTaskDuration = 30
                                    presetTaskIsInbox = false
                                    presetTaskScope = com.example.data.PrivacyScope.FAMILY
                                    showAddTaskDialog = true
                                },
                                onOpenNewTaskWithPreset = { startHour, startMinute, suggestedDur, isInbox, defaultScope ->
                                    editingTask = null
                                    presetTaskStartHour = startHour
                                    presetTaskStartMinute = startMinute
                                    presetTaskDuration = suggestedDur
                                    presetTaskIsInbox = isInbox
                                    presetTaskScope = defaultScope
                                    showAddTaskDialog = true
                                }
                            )

                            // 2: FINANÇAS DA FAMÍLIA (Sub-abas roláveis sem cortar texto + Botão "Olhinho" de Privacidade)
                            2 -> Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            0 to "Visão Geral",
                                            1 to "Cartões & Faturas",
                                            2 to "Metas & Fixas"
                                        ).forEach { (idx, title) ->
                                            FilterChip(
                                                selected = financeSubSection == idx,
                                                onClick = { financeSubSection = idx },
                                                label = {
                                                    Text(
                                                        text = title,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                shape = com.example.ui.theme.omniOrganicButtonShape(),
                                                modifier = Modifier.testTag("finance_subtab_$idx")
                                            )
                                        }
                                    }

                                    // Botão "Olhinho" de Privacidade Global das Finanças
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { customizationRepo.toggleFinancePrivacyHideValues() }
                                            .testTag("finance_top_privacy_toggle_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (customizationConfig.financePrivacyHideValues) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = if (customizationConfig.financePrivacyHideValues) "Mostrar saldos" else "Ocultar saldos",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (customizationConfig.financePrivacyHideValues) "Oculto" else "Saldos",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
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
                                            onOpenNewTransactionDialog = {
                                                editingTransaction = null
                                                showAddTransactionDialog = true
                                            },
                                            onAutoPostRecurringBills = { viewModel.autoPostDueSubscriptionsForSelectedMonth() },
                                            onOpenFinanceSubSection = { financeSubSection = it },
                                            onEditTransaction = { tx -> editingTransaction = tx },
                                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                            onSetCreditTransactionPaid = { tx, paid, accId, accName ->
                                                viewModel.setCreditTransactionPaid(tx, paid, accId, accName)
                                            },
                                            onPayAllCreditForMonth = { ym, accId, accName ->
                                                viewModel.payAllCreditForMonth(ym, accId, accName)
                                            },
                                            onSaveBankAccount = { viewModel.saveBankAccount(it) },
                                            onDeleteBankAccount = { accId, accName ->
                                                viewModel.deleteBankAccount(accId, accName)
                                            }
                                        )

                                        1 -> FinancesScreen(
                                            selectedYearMonth = selectedYearMonth,
                                            bankAccounts = household.bankAccounts,
                                            transactions = transactions,
                                            subscriptions = subscriptions,
                                            currencyRates = currencyRates,
                                            onRefreshRates = { viewModel.refreshExchangeRates() },
                                            onUpdateManualRate = { code, rate -> viewModel.updateManualRate(code, rate) },
                                            onOpenNewTransactionDialog = {
                                                editingTransaction = null
                                                showAddTransactionDialog = true
                                            },
                                            onEditTransaction = { tx -> editingTransaction = tx },
                                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                            onSetCreditTransactionPaid = { tx, paid, accId, accName ->
                                                viewModel.setCreditTransactionPaid(tx, paid, accId, accName)
                                            },
                                            onPayAllCreditForMonth = { ym, accId, accName ->
                                                viewModel.payAllCreditForMonth(ym, accId, accName)
                                            },
                                            onSaveBankAccount = { viewModel.saveBankAccount(it) },
                                            onDeleteBankAccount = { accId, accName ->
                                                viewModel.deleteBankAccount(accId, accName)
                                            },
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
                                            onUpdateSubscription = { existing, title, amountOrig, currency, cat, bucket, dueDay, remDays, autoPost, privacyScope, _ ->
                                                viewModel.updateSubscriptionBill(
                                                    context = context,
                                                    subscriptionId = existing.id,
                                                    title = title,
                                                    amountOriginal = amountOrig,
                                                    currency = currency,
                                                    category = cat,
                                                    budgetBucket = bucket,
                                                    dueDayOfMonth = dueDay,
                                                    reminderDaysBefore = remDays,
                                                    autoPostEnabled = autoPost,
                                                    privacyScope = privacyScope
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
                                            onUpdateSavingsGoal = { existing, title, target, current, pct, cat, targetYm ->
                                                viewModel.updateSavingsGoal(
                                                    goalId = existing.id,
                                                    title = title,
                                                    targetAmountBrl = target,
                                                    currentAmountBrl = current,
                                                    incomePercentage = pct,
                                                    category = cat,
                                                    targetYearMonth = targetYm
                                                )
                                            },
                                            onContributeGoal = { goal, amount, recordInMonth ->
                                                viewModel.contributeToGoal(goal, amount, recordInMonth)
                                            },
                                            onDeleteGoal = { viewModel.deleteSavingsGoal(it) },
                                            onUpdateCategoryBudget = { category, limitBrl ->
                                                viewModel.updateCategoryBudget(category, limitBrl)
                                            },
                                            onAddCustomCategory = { newCat ->
                                                viewModel.addCustomCategory(newCat)
                                            }
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
                                syncedItemsTotalCount = tasks.size + transactions.size + subscriptions.size + savingsGoals.size + household.muralItems.size + diaryState.entries.size + healthState.glucoseRecords.size + healthState.bloodPressureRecords.size,
                                onForceCloudSyncNow = {
                                    viewModel.forceFullCloudSyncNow(
                                        context = context,
                                        displayNameHint = customizationConfig.profileName,
                                        avatarBase64Hint = customizationConfig.profileAvatarBase64
                                    )
                                },
                                onUpdateProfileIdentity = { displayName, memberColorHex ->
                                    viewModel.updateUserProfileIdentity(displayName, memberColorHex)
                                },
                                onUpdateAvatarBase64 = { base64 ->
                                    viewModel.updateUserAvatarBase64(base64)
                                },
                                onJoinHousehold = { code, famName ->
                                    viewModel.joinOrCreateHousehold(code, famName)
                                },
                                onToggleInviteOpen = { inviteOpen ->
                                    viewModel.setHouseholdInviteOpen(inviteOpen)
                                },
                                onCreateNewFamily = { famName, customCode ->
                                    viewModel.createNewFamily(famName, customCode)
                                },
                                onSaveFamilyMember = { member ->
                                    viewModel.saveFamilyMember(member)
                                },
                                onRemoveFamilyMember = { memberId, memberName ->
                                    viewModel.removeFamilyMember(memberId, memberName)
                                },
                                onChangeThemeMode = { mode ->
                                    customizationRepo.updateThemeMode(mode)
                                    viewModel.setThemeMode(mode)
                                },
                                onOpenCustomizationStudio = { currentTab = 4 },
                                onOpenModularOnboardingScreen = { showModularOnboardingModal = true },
                                achievementsSummary = achievementsSummary,
                                onOpenAchievementsScreen = { currentTab = 7 },
                                updateState = updateState,
                                updateManager = updateManager,
                                onRecoverLegacyData = {
                                    coroutineScope.launch {
                                        val activeHouseId = household.householdId.ifBlank { userProfile?.householdId ?: "FAM_PADRAO" }
                                        val res = com.example.data.OmniBackupAndMigrationManager.recoverAndMergeLegacyData(
                                            context = context,
                                            activeHouseholdId = activeHouseId,
                                            currentHousehold = household,
                                            currentTasks = tasks,
                                            currentTransactions = transactions,
                                            repository = migrationRepository,
                                            healthRepo = healthRepo,
                                            diaryRepo = diaryRepo
                                        )
                                        localStudioBanner = res.toSummaryMessagePt(isRestore = false)
                                    }
                                },
                                onBuildFullBackupJson = {
                                    com.example.data.OmniBackupAndMigrationManager.exportFullBackupJson(
                                        context = context,
                                        household = household,
                                        tasks = tasks,
                                        transactions = transactions,
                                        subscriptions = subscriptions,
                                        savingsGoals = savingsGoals,
                                        healthState = healthState,
                                        diaryState = diaryState
                                    )
                                },
                                onRestoreBackupFromUri = { uri ->
                                    coroutineScope.launch {
                                        val activeHouseId = household.householdId.ifBlank { userProfile?.householdId ?: "FAM_PADRAO" }
                                        val res = com.example.data.OmniBackupAndMigrationManager.restoreBackupFromUri(
                                            context = context,
                                            sourceUri = uri,
                                            activeHouseholdId = activeHouseId,
                                            currentHousehold = household,
                                            currentTasks = tasks,
                                            currentTransactions = transactions,
                                            repository = migrationRepository,
                                            healthRepo = healthRepo,
                                            diaryRepo = diaryRepo
                                        )
                                        res.onSuccess { summary ->
                                            localStudioBanner = summary.toSummaryMessagePt(isRestore = true)
                                        }.onFailure { err ->
                                            localStudioBanner = "Falha ao restaurar backup: ${err.localizedMessage ?: "arquivo inválido"}"
                                        }
                                    }
                                },
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
                                onShowBannerMessage = { localStudioBanner = it },
                                household = household,
                                userProfile = userProfile
                            )

                            // 6: PASSO 4 — DIÁRIO PRIVADO (BLOQUEADO) & MURAL DA FAMÍLIA (COMPARTILHADO)
                            6 -> PrivateDiaryScreen(
                                initialEpochDay = selectedEpochDay,
                                initialSubTab = diaryInitialSubTab,
                                customizationConfig = customizationConfig,
                                customizationRepo = customizationRepo,
                                diaryRepo = diaryRepo,
                                householdMuralItems = household.muralItems,
                                onAddMuralItem = { title, notes, category, isNote, pinned ->
                                    viewModel.addMuralItem(
                                        context = context,
                                        title = title,
                                        notes = notes,
                                        category = category,
                                        addedBy = customizationConfig.profileName.ifBlank { "Família" },
                                        addedByColorHex = customizationConfig.profileColorHex,
                                        isNote = isNote,
                                        pinned = pinned
                                    )
                                },
                                onToggleMuralItem = { itemId ->
                                    viewModel.toggleMuralItem(
                                        context = context,
                                        itemId = itemId,
                                        checkedBy = customizationConfig.profileName.ifBlank { "Família" },
                                        checkedByColorHex = customizationConfig.profileColorHex
                                    )
                                },
                                onTogglePinMuralItem = { itemId ->
                                    viewModel.togglePinMuralItem(context, itemId)
                                },
                                onClearCompletedMuralItems = { category ->
                                    viewModel.clearCompletedMuralItems(context, category)
                                },
                                onDeleteMuralItem = { itemId ->
                                    viewModel.deleteMuralItem(context, itemId)
                                },
                                onShowBannerMessage = { localStudioBanner = it }
                            )

                            // 7: GALERIA DE CONQUISTAS & MEDALHAS (100% Automática vinculada a Saúde, Financeiro e Calendário)
                            7 -> AchievementsMedalsScreen(
                                summary = achievementsSummary,
                                customizationConfig = customizationConfig,
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
            bankAccounts = household.bankAccounts,
            onDismiss = { showVoiceCommandSheet = false },
            onSaveExpense = { title, amountBrl, category, privacyScope, accountId, accountName ->
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
                    overrideMemberColorHex = customizationConfig.profileColorHex,
                    accountId = accountId,
                    accountName = accountName
                )
                localStudioBanner = "💸 Despesa por voz salva: $title (R$ ${String.format(java.util.Locale.US, "%.2f", amountBrl)} em $category)!"
            },
            onSaveIncome = { title, amountBrl, category, privacyScope, accountId, accountName, isRecurring ->
                viewModel.createTransaction(
                    title = title,
                    amountOriginal = amountBrl,
                    currency = "BRL",
                    type = "INCOME",
                    budgetBucket = "INCOME",
                    category = category,
                    dateEpochDay = selectedEpochDay,
                    privacyScope = privacyScope,
                    generateCalendarTask = false,
                    overrideMemberColorHex = customizationConfig.profileColorHex,
                    accountId = accountId,
                    accountName = accountName,
                    isRecurring = isRecurring
                )
                localStudioBanner = "💰 Receita por voz salva: $title (+ R$ ${String.format(java.util.Locale.US, "%.2f", amountBrl)} em $category)!"
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
            },
            onSaveMuralItems = { items, category ->
                items.forEach { itemTitle ->
                    viewModel.addMuralItem(
                        context = context,
                        title = itemTitle,
                        category = category,
                        addedBy = customizationConfig.profileName.ifBlank { "Família" },
                        addedByColorHex = customizationConfig.profileColorHex
                    )
                }
                localStudioBanner = "🛒 ${items.size} item(ns) adicionado(s) ao Mural da Família ($category)!"
            }
        )
    }

    // Modal Rápido do Botão Flutuante Único Inteligente (+)
    if (showQuickCreateChooserModal) {
        AlertDialog(
            onDismissRequest = { showQuickCreateChooserModal = false },
            title = {
                Text(
                    text = "✨ Criação Rápida Omni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Dica: Segure o botão + pressionado em qualquer tela para ativar o Comando de Voz imediatamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            showQuickCreateChooserModal = false
                            editingTask = null
                            presetTaskStartHour = 9
                            presetTaskStartMinute = 0
                            presetTaskDuration = 30
                            presetTaskIsInbox = false
                            presetTaskScope = com.example.data.PrivacyScope.FAMILY
                            showAddTaskDialog = true
                        },
                        shape = com.example.ui.theme.omniOrganicButtonShape(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_chooser_add_task_btn")
                    ) {
                        Text("📅 + Nova Tarefa ou Compromisso", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = {
                            showQuickCreateChooserModal = false
                            editingTransaction = null
                            showAddTransactionDialog = true
                        },
                        shape = com.example.ui.theme.omniOrganicButtonShape(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_chooser_add_tx_btn")
                    ) {
                        Text("💳 + Novo Gasto ou Receita", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = {
                            showQuickCreateChooserModal = false
                            diaryInitialSubTab = 1
                            currentTab = 6
                        },
                        shape = com.example.ui.theme.omniOrganicButtonShape(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_chooser_open_mural_btn")
                    ) {
                        Text("📌 + Item no Mural / Lista de Compras", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = {
                            showQuickCreateChooserModal = false
                            startVoiceListeningImmediately = true
                            showVoiceCommandSheet = true
                        },
                        shape = com.example.ui.theme.omniOrganicButtonShape(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_chooser_open_voice_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Comando de Voz Inteligente", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showQuickCreateChooserModal = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    if (showAddTaskDialog || editingTask != null) {
        val currentEditingTask = editingTask
        AddTaskDialog(
            initialEpochDay = currentEditingTask?.dateEpochDay ?: selectedEpochDay,
            defaultProfileColorHex = customizationConfig.profileColorHex,
            initialStartHour = presetTaskStartHour,
            initialStartMinute = presetTaskStartMinute,
            initialDurationMinutes = presetTaskDuration,
            initialIsInbox = presetTaskIsInbox,
            initialPrivacyScope = presetTaskScope,
            initialTask = currentEditingTask,
            customizationRepo = customizationRepo,
            customCategories = household.customCategories,
            onAddCustomCategory = { newCat -> viewModel.addCustomCategory(newCat) },
            onDismiss = {
                showAddTaskDialog = false
                editingTask = null
            },
            onSaveTask = { title, notes, category, priority, dateEpoch, hour, min, alarmEnabled, privacyScope, estimatedCostBrl, autoCreateExpense, isEssential, isInbox, durationMinutes, iconEmoji, taskColorHex, recurrence, subtasks, ringtoneCategory, location, endDateEpochDay, recurrenceWeekdays ->
                if (currentEditingTask != null) {
                    viewModel.updateTask(
                        context = context,
                        taskId = currentEditingTask.id,
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
                        isEssential = isEssential,
                        isInbox = isInbox,
                        durationMinutes = durationMinutes,
                        iconEmoji = iconEmoji,
                        taskColorHex = taskColorHex,
                        recurrence = recurrence,
                        subtasks = subtasks,
                        ringtoneCategory = ringtoneCategory,
                        location = location,
                        endDateEpochDay = endDateEpochDay,
                        recurrenceWeekdays = recurrenceWeekdays
                    )
                } else {
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
                        ringtoneCategory = ringtoneCategory,
                        location = location,
                        endDateEpochDay = endDateEpochDay,
                        recurrenceWeekdays = recurrenceWeekdays
                    )
                }
            }
        )
    }

    if (showAddTransactionDialog || editingTransaction != null) {
        val currentEditingTx = editingTransaction
        AddTransactionDialog(
            initialEpochDay = currentEditingTx?.dateEpochDay ?: selectedEpochDay,
            initialTransaction = currentEditingTx,
            bankAccounts = household.bankAccounts,
            currencyRates = currencyRates,
            customCategories = household.customCategories,
            onAddCustomCategory = { newCat -> viewModel.addCustomCategory(newCat) },
            onDismiss = {
                showAddTransactionDialog = false
                editingTransaction = null
            },
            onSaveTransaction = { title, amountOrig, currency, type, bucket, cat, dateEpoch, privacyScope, generateCalendarTask, accountId, accountName, destinationAccountId, destinationAccountName, isRecurring, isCreditCard, installmentCount, isPaid ->
                if (currentEditingTx != null) {
                    viewModel.updateTransaction(
                        transactionId = currentEditingTx.id,
                        title = title,
                        amountOriginal = amountOrig,
                        currency = currency,
                        type = type,
                        budgetBucket = bucket,
                        category = cat,
                        dateEpochDay = dateEpoch,
                        privacyScope = privacyScope,
                        accountId = accountId,
                        accountName = accountName,
                        destinationAccountId = destinationAccountId,
                        destinationAccountName = destinationAccountName,
                        isRecurring = isRecurring,
                        isCreditCard = isCreditCard,
                        isPaid = isPaid
                    )
                } else {
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
                        overrideMemberColorHex = customizationConfig.profileColorHex,
                        accountId = accountId,
                        accountName = accountName,
                        destinationAccountId = destinationAccountId,
                        destinationAccountName = destinationAccountName,
                        isRecurring = isRecurring,
                        isCreditCard = isCreditCard,
                        installmentCount = installmentCount,
                        isPaid = isPaid
                    )
                }
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
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
