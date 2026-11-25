package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alarm.AlarmScheduler
import com.example.data.AppUpdateManager
import com.example.data.AuraFirestoreRepository
import com.example.data.CustomizationRepository
import com.example.data.LocalOfflineStore
import com.example.data.UiState
import com.example.ui.AppUpdateAvailableDialog
import com.example.ui.AppUpdateTopPillBanner
import com.example.ui.AuthScreen
import com.example.ui.MainFeatureScreen
import com.example.ui.OmniPermissionHelper
import com.example.ui.OmniSplashScreen
import com.example.ui.signOut
import com.example.ui.theme.AuraFinancasTheme
import com.example.viewmodel.AuraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import android.content.Intent
import com.example.alarm.TaskAndBillAlarmReceiver
import com.example.widget.OmniHomeWidgetProvider

class MainActivity : ComponentActivity() {
    private val pendingWidgetActionState = mutableStateOf<String?>(null)
    private val pendingGlucoseSlotState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.ensureNotificationChannel(this)
        extractWidgetAction(intent)
        setContent {
            AuraAppRoot(
                pendingWidgetAction = pendingWidgetActionState.value,
                onConsumeWidgetAction = { pendingWidgetActionState.value = null },
                pendingGlucoseSlot = pendingGlucoseSlotState.value,
                onConsumeGlucoseSlot = { pendingGlucoseSlotState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetAction(intent)
    }

    private fun extractWidgetAction(intent: Intent?) {
        val action = intent?.getStringExtra(OmniHomeWidgetProvider.EXTRA_WIDGET_ACTION)
        if (!action.isNullOrBlank()) {
            pendingWidgetActionState.value = action
            intent.removeExtra(OmniHomeWidgetProvider.EXTRA_WIDGET_ACTION)
        }
        val glucoseSlot = intent?.getStringExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT)
        if (!glucoseSlot.isNullOrBlank()) {
            pendingGlucoseSlotState.value = glucoseSlot
            if (pendingWidgetActionState.value.isNullOrBlank()) {
                pendingWidgetActionState.value = OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_GLUCOSE
            }
            intent.removeExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT)
        }
    }
}

@Composable
fun AuraAppRoot(
    pendingWidgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {},
    pendingGlucoseSlot: String? = null,
    onConsumeGlucoseSlot: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val customizationRepo = remember(context) { CustomizationRepository(context) }
    val customizationConfig by customizationRepo.configFlow.collectAsStateWithLifecycle()

    val updateManager = remember(context) { AppUpdateManager(context.applicationContext) }
    val updateState by updateManager.uiState.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable {
        mutableStateOf(pendingWidgetAction.isNullOrBlank() && pendingGlucoseSlot.isNullOrBlank())
    }
    var hasRequestedInitialPermissions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingWidgetAction, pendingGlucoseSlot) {
        if (!pendingWidgetAction.isNullOrBlank() || !pendingGlucoseSlot.isNullOrBlank()) {
            showSplash = false
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    if (showSplash && pendingWidgetAction.isNullOrBlank()) {
        OmniSplashScreen(
            onSplashFinished = {
                showSplash = false
            }
        )
        return
    }

    // Request POST_NOTIFICATIONS automatically on first launch after splash on Android 13+
    LaunchedEffect(hasRequestedInitialPermissions) {
        if (!hasRequestedInitialPermissions) {
            hasRequestedInitialPermissions = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !OmniPermissionHelper.hasNotificationPermission(context)
            ) {
                runCatching {
                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var hasSkippedGoogleLogin by rememberSaveable { mutableStateOf(false) }
    var showOptionalLoginScreen by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(updateManager) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            updateManager.onAuthSessionChanged()
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, updateManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateManager.checkNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val user = currentUser
    val shouldShowAuthScreen = user == null &&
        (!hasSkippedGoogleLogin || showOptionalLoginScreen) &&
        pendingWidgetAction.isNullOrBlank() &&
        pendingGlucoseSlot.isNullOrBlank()

    if (shouldShowAuthScreen) {
        BackHandler {
            hasSkippedGoogleLogin = true
            showOptionalLoginScreen = false
        }
        AuraFinancasTheme(
            themeMode = customizationConfig.themeMode.ifBlank { "SYSTEM" },
            customization = customizationConfig
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                AppUpdateTopPillBanner(
                    updateState = updateState,
                    onOpenUpdateModal = { updateManager.reopenUpdateModal() },
                    onClearUpgradedBanner = { updateManager.clearJustUpgradedBanner() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                            showOptionalLoginScreen = false
                            updateManager.onAuthSessionChanged()
                        },
                        onSkipLogin = {
                            hasSkippedGoogleLogin = true
                            showOptionalLoginScreen = false
                        }
                    )
                }
            }

            AppUpdateAvailableDialog(
                updateState = updateState,
                updateManager = updateManager,
                onShowMessage = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            )
        }
    } else {
        val activeUserId = user?.uid ?: LocalOfflineStore.LOCAL_USER_ID
        val displayName = user?.displayName ?: "Família Omni"
        val userEmail = user?.email
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(activeUserId) {
            val auraViewModel: AuraViewModel = viewModel(
                key = activeUserId,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY ausente em CreationExtras"
                        }
                        AuraViewModel(
                            repository = AuraFirestoreRepository(app),
                            currentUserId = activeUserId,
                            initialDisplayName = displayName
                        )
                    }
                }
            )

            val profileState by auraViewModel.userProfileState.collectAsStateWithLifecycle()
            val remoteThemeMode = (profileState as? UiState.Success)?.data?.themeMode
            LaunchedEffect(remoteThemeMode) {
                if (!remoteThemeMode.isNullOrBlank() && remoteThemeMode != "SYSTEM" && remoteThemeMode != customizationConfig.themeMode) {
                    customizationRepo.updateThemeMode(remoteThemeMode)
                }
            }
            val effectiveThemeMode = customizationConfig.themeMode.takeIf { it.isNotBlank() && it != "SYSTEM" }
                ?: remoteThemeMode?.takeIf { it.isNotBlank() }
                ?: "SYSTEM"

            AuraFinancasTheme(
                themeMode = effectiveThemeMode,
                customization = customizationConfig
            ) {
                MainFeatureScreen(
                    viewModel = auraViewModel,
                    customizationConfig = customizationConfig,
                    customizationRepo = customizationRepo,
                    userEmail = userEmail,
                    onSignOut = {
                        if (user == null) {
                            showOptionalLoginScreen = true
                        } else {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = {
                                    currentUser = null
                                    hasSkippedGoogleLogin = false
                                    showOptionalLoginScreen = true
                                }
                            )
                        }
                    },
                    sharedUpdateManager = updateManager,
                    pendingWidgetAction = pendingWidgetAction,
                    onConsumeWidgetAction = onConsumeWidgetAction,
                    pendingGlucoseSlot = pendingGlucoseSlot,
                    onConsumeGlucoseSlot = onConsumeGlucoseSlot
                )
            }
        }
    }
}
