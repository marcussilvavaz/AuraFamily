package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alarm.AlarmScheduler
import com.example.data.AuraFirestoreRepository
import com.example.data.CustomizationRepository
import com.example.data.LocalOfflineStore
import com.example.data.UiState
import com.example.ui.AuthScreen
import com.example.ui.MainFeatureScreen
import com.example.ui.signOut
import com.example.ui.theme.AuraFinancasTheme
import com.example.viewmodel.AuraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.ensureNotificationChannel(this)
        setContent {
            AuraAppRoot()
        }
    }
}

@Composable
fun AuraAppRoot() {
    val context = LocalContext.current
    val customizationRepo = remember(context) { CustomizationRepository(context) }
    val customizationConfig by customizationRepo.configFlow.collectAsStateWithLifecycle()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    // Show Google Sign-In screen on launch with the option "Fazer login depois"
    var hasSkippedGoogleLogin by rememberSaveable { mutableStateOf(false) }
    var showOptionalLoginScreen by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    val shouldShowAuthScreen = user == null && (!hasSkippedGoogleLogin || showOptionalLoginScreen)

    if (shouldShowAuthScreen) {
        BackHandler {
            hasSkippedGoogleLogin = true
            showOptionalLoginScreen = false
        }
        AuraFinancasTheme(
            themeMode = "SYSTEM",
            customization = customizationConfig
        ) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    showOptionalLoginScreen = false
                },
                onSkipLogin = {
                    hasSkippedGoogleLogin = true
                    showOptionalLoginScreen = false
                }
            )
        }
    } else {
        val activeUserId = user?.uid ?: LocalOfflineStore.LOCAL_USER_ID
        val displayName = user?.displayName ?: "Família Aura"
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
            val themeMode = (profileState as? UiState.Success)?.data?.themeMode ?: "SYSTEM"

            AuraFinancasTheme(
                themeMode = themeMode,
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
                    }
                )
            }
        }
    }
}
