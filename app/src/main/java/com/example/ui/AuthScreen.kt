package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.LocalOrganicCardRadius
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthException
import java.security.MessageDigest

private const val FALLBACK_WEB_CLIENT_ID =
    "725763199828-ianl05t6nnsprgotnvt896cmgvcq7kb7.apps.googleusercontent.com"

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private fun resolveServerClientId(context: Context): String {
    val fromRes = runCatching {
        context.getString(R.string.default_web_client_id).trim()
    }.getOrDefault("")
    return if (fromRes.isNotBlank() && fromRes.endsWith(".apps.googleusercontent.com")) {
        fromRes
    } else {
        FALLBACK_WEB_CLIENT_ID
    }
}

@Suppress("DEPRECATION")
fun getCurrentApkSha1Fingerprint(context: Context): String {
    return runCatching {
        val pm = context.packageManager
        val pkgName = context.packageName
        val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            val signatures = if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else null
            signatures?.firstOrNull()?.toByteArray()
        } else {
            val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
            info.signatures?.firstOrNull()?.toByteArray()
        }
        if (certBytes == null) return@runCatching "Indisponível"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(certBytes)
        digest.joinToString(":") { b -> String.format("%02X", b) }
    }.getOrDefault("Indisponível")
}

private fun buildDetailedAuthErrorMessage(
    context: Context,
    error: Throwable,
    clientIdUsed: String,
    wasCancellationException: Boolean = false
): String {
    val sha1 = getCurrentApkSha1Fingerprint(context)
    val className = error::class.java.simpleName
    val credType = (error as? GetCredentialException)?.type.orEmpty()
    val credErrMsg = (error as? GetCredentialException)?.errorMessage?.toString().orEmpty()

    var apiStatusCode: Int? = null
    var firebaseErrorCode: String? = null
    var causeCursor: Throwable? = error
    while (causeCursor != null) {
        if (causeCursor is ApiException && apiStatusCode == null) {
            apiStatusCode = causeCursor.statusCode
        }
        if (causeCursor is FirebaseAuthException && firebaseErrorCode == null) {
            firebaseErrorCode = causeCursor.errorCode
        }
        causeCursor = causeCursor.cause
    }

    val rawMessage = credErrMsg.ifBlank {
        error.localizedMessage ?: error.message ?: "Sem mensagem adicional"
    }

    val codeParts = mutableListOf<String>()
    codeParts.add(className)
    if (credType.isNotBlank()) codeParts.add("type=$credType")
    if (apiStatusCode != null) codeParts.add("ApiException statusCode=$apiStatusCode")
    if (!firebaseErrorCode.isNullOrBlank()) codeParts.add("FirebaseAuth=$firebaseErrorCode")

    val header = if (wasCancellationException) {
        "⚠️ O seletor do Google foi fechado ou recusou o token (${codeParts.joinToString(" • ")})."
    } else {
        "❌ Falha no Login com Google (${codeParts.joinToString(" • ")})."
    }

    val clientIdSuffix = clientIdUsed.takeLast(34)
    return buildString {
        appendLine(header)
        appendLine("Detalhe: $rawMessage")
        appendLine("SHA-1 deste APK: $sha1")
        appendLine("Web Client ID: ...$clientIdSuffix")
        append(
            "Dica: Se o seletor fecha após escolher o e-mail, confira se o SHA-1 acima está cadastrado no Firebase Console (Configurações do Projeto -> app Android com.auramanager.app)."
        )
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveServerClientId(context)
    if (clientId.isBlank()) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            Log.w("Auth", "Auto sign-in skipped: ${e::class.java.simpleName}: ${e.message}")
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = resolveServerClientId(context)
    if (clientId.isBlank()) {
        onAuthError("Web Client ID ausente no google-services.json (default_web_client_id).")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Contexto de Activity indisponível para abrir o seletor de contas Google.")
            return@launch
        }

        try {
            val cred = try {
                credentialManager.getCredential(activity, primaryRequest).credential
            } catch (primaryEx: Exception) {
                Log.w(
                    "Auth",
                    "GetSignInWithGoogleOption falhou (${primaryEx::class.java.simpleName}: ${primaryEx.message}); tentando fallback GetGoogleIdOption...",
                    primaryEx
                )
                // Se o primeiro método falhar (ex: incompatibilidade do provedor), tenta GetGoogleIdOption com filterByAuthorizedAccounts = false
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                try {
                    credentialManager.getCredential(activity, fallbackRequest).credential
                } catch (_: Exception) {
                    throw primaryEx
                }
            }

            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                val token = googleIdTokenCred.idToken
                if (token.isBlank()) {
                    onAuthError("O Google retornou um ID Token vazio. Verifique o Web Client ID e o SHA-1 no Firebase.")
                    return@launch
                }
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Tipo de credencial inesperado retornado pelo Credential Manager: ${cred.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.e("Auth", "Google Sign-In cancelled or rejected by Play Services: ${e.type} / ${e.message}", e)
            val detailed = buildDetailedAuthErrorMessage(
                context = context,
                error = e,
                clientIdUsed = clientId,
                wasCancellationException = true
            )
            onAuthCancelled()
            onAuthError(detailed)
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed: ${e::class.java.simpleName} / ${e.message}", e)
            val detailed = buildDetailedAuthErrorMessage(
                context = context,
                error = e,
                clientIdUsed = clientId,
                wasCancellationException = false
            )
            onAuthError(detailed)
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cardRadius = LocalOrganicCardRadius.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val apkSha1 = remember(context) { getCurrentApkSha1Fingerprint(context) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    shape = RoundedCornerShape(cardRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_wealth_1795185246551),
                            contentDescription = "Banner Omni",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text(
                                    text = "OMNI • TAREFAS, ROTINA, FINANÇAS & SAÚDE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Orçamento Familiar, Agenda Limpa, Saúde & Diário Privado",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(cardRadius)
                        ),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Agenda Limpa & Check de Tarefas",
                            subtitle = "Time blocking, modo dia difícil e alarmes no horário exato."
                        )
                        FeatureRow(
                            icon = Icons.Default.CurrencyExchange,
                            title = "Finanças, Saúde & Relatório PDF",
                            subtitle = "Múltiplas moedas, glicemia, pressão arterial e inegociáveis."
                        )
                        FeatureRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Diário Privado & Comando de Voz",
                            subtitle = "Frases por turno, carta de apoio via IA e atalho global de voz."
                        )
                        FeatureRow(
                            icon = Icons.Default.CloudSync,
                            title = "Sincronização Familiar na Nuvem",
                            subtitle = "Conecte sua conta Google agora ou escolha fazer login depois."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("google_sign_in_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Start
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(apkSha1))
                                        Toast.makeText(context, "SHA-1 copiado: $apkSha1", Toast.LENGTH_LONG).show()
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Copiar SHA-1", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(errorMessage.orEmpty()))
                                        Toast.makeText(context, "Diagnóstico completo copiado!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Copiar Erro", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Primary Action: Sign in with Google
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = msg.lines().firstOrNull() ?: "Falha no login Google",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.sign_in_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Action: Login later ("Fazer login depois")
                FilledTonalButton(
                    onClick = onSkipLogin,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("skip_google_login_btn")
                ) {
                    Text(
                        text = "Fazer login depois",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continuar sem login por enquanto",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Você pode usar todos os módulos imediatamente e conectar sua conta Google quando quiser pelo Hub ou Perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
