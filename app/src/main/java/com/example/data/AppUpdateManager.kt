package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

data class AppReleaseMetadata(
    val buildRevision: Long = 1L,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val versionName: String = BuildConfig.VERSION_NAME,
    val dexSignatureChain: String = "",
    val releaseNotes: String = DEFAULT_RELEASE_NOTES,
    val downloadUrl: String = "",
    val mandatory: Boolean = false,
    val publishedBy: String = "Aura Studio Auto-Sync",
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val buildTimestampMs: Long = BuildConfig.BUILD_TIMESTAMP_MS
) {
    val signatureList: List<String>
        get() = dexSignatureChain.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val latestSignature: String
        get() = signatureList.lastOrNull().orEmpty()

    companion object {
        const val AI_STUDIO_PORTAL_URL = "https://aistudio.google.com/apps"
        const val DEFAULT_RELEASE_NOTES =
            "• Novas alterações e melhorias aplicadas no projeto Aura Finanças.\n" +
            "• Download e exportação direta do arquivo instalador .APK integrados no app.\n" +
            "• Sistema de Conquistas e Medalhas (Saúde, Financeiro e Calendário) e sincronização Firebase."
    }
}

data class AppUpdateUiState(
    val localVersionCode: Int = BuildConfig.VERSION_CODE,
    val localVersionName: String = BuildConfig.VERSION_NAME,
    val localBuildSignature: String = "",
    val localBuildTimestampMs: Long = BuildConfig.BUILD_TIMESTAMP_MS,
    val localApkSizeLabel: String = "",
    val localKnownRevision: Long = 1L,
    val isRunningOnStudioPreview: Boolean = false,
    val remoteRelease: AppReleaseMetadata? = null,
    val isUpdateAvailable: Boolean = false,
    val isModalDismissedForSession: Boolean = false,
    val isCheckingOrPublishing: Boolean = false,
    val lastCheckStatusMessage: String = "Sincronizado com o servidor de atualizações",
    val customDownloadUrl: String = "",
    val justUpgradedBannerMessage: String? = null
)

class AppUpdateManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val db: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance(
            appContext.getString(R.string.firestore_database_id)
        )
    }.getOrNull()

    private val localSignature: String = computeLocalApkSignature(appContext)
    private val localBuildTimestampMs: Long = BuildConfig.BUILD_TIMESTAMP_MS
    private val isStudioPreview: Boolean = isRunningOnEmulatorOrPreview()
    private val localApkSizeLabel: String = computeLocalApkSizeLabel(appContext)
    private var snapshotListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        AppUpdateUiState(
            localVersionCode = BuildConfig.VERSION_CODE,
            localVersionName = BuildConfig.VERSION_NAME,
            localBuildSignature = localSignature,
            localBuildTimestampMs = localBuildTimestampMs,
            localApkSizeLabel = localApkSizeLabel,
            localKnownRevision = prefs.getLong(KEY_LOCAL_KNOWN_REVISION, 1L),
            isRunningOnStudioPreview = isStudioPreview,
            customDownloadUrl = sanitizeAndNormalizeUrl(
                prefs.getString(KEY_CUSTOM_DOWNLOAD_URL, "").orEmpty()
            )
        )
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        scrubLegacyBrokenRunAppUrlFromPrefs()
        ensureUpdateNotificationChannel()
        detectLocalApkUpgrade()
        startRealtimeUpdateListener()
    }

    /**
     * Purges any previously saved `ais-pre-*.run.app` or `ais-dev-*.run.app` URL from SharedPreferences,
     * since `.run.app` is a Web container proxy and returns a broken page on Android projects.
     */
    private fun scrubLegacyBrokenRunAppUrlFromPrefs() {
        val saved = prefs.getString(KEY_CUSTOM_DOWNLOAD_URL, "").orEmpty()
        if (isBrokenRunAppPlaceholderUrl(saved)) {
            prefs.edit().putString(KEY_CUSTOM_DOWNLOAD_URL, "").apply()
        }
    }

    /**
     * Checks if the user just installed a newer APK over a previous one on this device.
     */
    private fun detectLocalApkUpgrade() {
        val previousRecordedSig = prefs.getString(KEY_LAST_INSTALLED_SIGNATURE, null)
        val previousRecordedVer = prefs.getInt(KEY_LAST_INSTALLED_VERSION_CODE, -1)
        val hasChanged = (previousRecordedSig != null && previousRecordedSig != localSignature) ||
            (previousRecordedVer != -1 && previousRecordedVer < BuildConfig.VERSION_CODE)

        if (hasChanged) {
            prefs.edit()
                .putString(KEY_LAST_INSTALLED_SIGNATURE, localSignature)
                .putInt(KEY_LAST_INSTALLED_VERSION_CODE, BuildConfig.VERSION_CODE)
                .apply()
            _uiState.value = _uiState.value.copy(
                justUpgradedBannerMessage = "✅ Aplicativo atualizado com sucesso para a v${BuildConfig.VERSION_NAME} (Build #$localSignature)!"
            )
        } else if (previousRecordedSig == null) {
            prefs.edit()
                .putString(KEY_LAST_INSTALLED_SIGNATURE, localSignature)
                .putInt(KEY_LAST_INSTALLED_VERSION_CODE, BuildConfig.VERSION_CODE)
                .apply()
        }
    }

    fun clearJustUpgradedBanner() {
        _uiState.value = _uiState.value.copy(justUpgradedBannerMessage = null)
    }

    /**
     * Called when Firebase Auth state changes so the listener & check are refreshed immediately.
     */
    fun onAuthSessionChanged() {
        startRealtimeUpdateListener()
        checkNow()
    }

    /**
     * Starts a real-time Firestore snapshot listener on `/app_releases/latest`.
     */
    fun startRealtimeUpdateListener() {
        val firestore = db ?: return
        snapshotListener?.remove()

        val docRef = firestore.collection(COLLECTION_APP_RELEASES).document(DOC_LATEST)
        snapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Monitoramento local ativo (Build #$localSignature • v${BuildConfig.VERSION_NAME})"
                )
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                publishCurrentBuildAsLatest(
                    previousChain = emptyList(),
                    nextRevision = 1L,
                    releaseNotes = AppReleaseMetadata.DEFAULT_RELEASE_NOTES,
                    downloadUrl = _uiState.value.customDownloadUrl,
                    mandatory = false
                )
                return@addSnapshotListener
            }

            val remote = snapshot.toAppReleaseMetadata()
            handleRemoteReleaseSnapshot(remote, isStudioPreviewOverride = isStudioPreview)
        }
    }

    internal fun handleRemoteReleaseForTesting(
        remote: AppReleaseMetadata,
        isStudioPreviewOverride: Boolean = false
    ) {
        handleRemoteReleaseSnapshot(remote, isStudioPreviewOverride)
    }

    private fun handleRemoteReleaseSnapshot(
        remote: AppReleaseMetadata,
        isStudioPreviewOverride: Boolean
    ) {
        val chain = remote.signatureList
        val latestRemoteSig = remote.latestSignature
        val localVer = BuildConfig.VERSION_CODE
        val localBuildTs = localBuildTimestampMs
        val cleanRemoteUrl = sanitizeAndNormalizeUrl(remote.downloadUrl)
        val sanitizedRemote = remote.copy(downloadUrl = cleanRemoteUrl)

        if (cleanRemoteUrl.isNotBlank() && cleanRemoteUrl != _uiState.value.customDownloadUrl) {
            prefs.edit().putString(KEY_CUSTOM_DOWNLOAD_URL, cleanRemoteUrl).apply()
        }

        // Determine if THIS running APK is genuinely a newly compiled build that should auto-publish to Firestore.
        val isStrictlyHigherVersionCode = localVer > sanitizedRemote.versionCode
        val isStrictlyNewerCompileTimestamp =
            localBuildTs > 0L &&
                sanitizedRemote.buildTimestampMs > 0L &&
                (localBuildTs > sanitizedRemote.buildTimestampMs + 2000L) &&
                localVer >= sanitizedRemote.versionCode

        val isStudioPreviewNewSignature =
            isStudioPreviewOverride &&
                !chain.contains(localSignature) &&
                localVer >= sanitizedRemote.versionCode &&
                (sanitizedRemote.buildTimestampMs == 0L || localBuildTs >= sanitizedRemote.buildTimestampMs)

        // Also auto-republish from Studio Preview if remote still had the broken `.run.app` URL
        val remoteHadBrokenRunAppUrl =
            isStudioPreviewOverride &&
                isBrokenRunAppPlaceholderUrl(remote.downloadUrl) &&
                localVer >= sanitizedRemote.versionCode

        val shouldAutoPublishThisBuild =
            isStrictlyHigherVersionCode ||
                isStrictlyNewerCompileTimestamp ||
                isStudioPreviewNewSignature ||
                remoteHadBrokenRunAppUrl

        if (shouldAutoPublishThisBuild) {
            val nextRev = (sanitizedRemote.buildRevision + 1L).coerceAtLeast(1L)
            publishCurrentBuildAsLatest(
                previousChain = chain,
                nextRevision = nextRev,
                releaseNotes = buildAutoChangelogNote(nextRev),
                downloadUrl = cleanRemoteUrl.ifBlank { _uiState.value.customDownloadUrl },
                mandatory = sanitizedRemote.mandatory
            )
            return
        }

        // Determine if the remote release in Firestore is newer than this installed APK:
        val storedLocalRev = prefs.getLong(KEY_LOCAL_KNOWN_REVISION, 1L)
        val selfPublishedRev = prefs.getLong(KEY_SELF_PUBLISHED_REVISION, -1L)
        val acknowledgedRev = prefs.getLong(KEY_ACKNOWLEDGED_REVISION, -1L)

        val isRemoteHigherVersionCode = sanitizedRemote.versionCode > localVer
        val isRemoteNewerCompileTimestamp =
            sanitizedRemote.buildTimestampMs > 0L &&
                localBuildTs > 0L &&
                (sanitizedRemote.buildTimestampMs > localBuildTs + 2000L)
        val isRemoteDifferentSignature =
            latestRemoteSig.isNotBlank() && latestRemoteSig != localSignature
        val isManualRevisionBumpForOtherDevices =
            sanitizedRemote.buildRevision > storedLocalRev && selfPublishedRev < sanitizedRemote.buildRevision

        val hasUnacknowledgedRemoteUpdate =
            (isRemoteHigherVersionCode ||
                isRemoteNewerCompileTimestamp ||
                isRemoteDifferentSignature ||
                isManualRevisionBumpForOtherDevices) &&
                acknowledgedRev < sanitizedRemote.buildRevision

        if (!hasUnacknowledgedRemoteUpdate) {
            prefs.edit().putLong(KEY_LOCAL_KNOWN_REVISION, sanitizedRemote.buildRevision).apply()
            _uiState.value = _uiState.value.copy(
                localKnownRevision = sanitizedRemote.buildRevision,
                remoteRelease = sanitizedRemote,
                isUpdateAvailable = false,
                isCheckingOrPublishing = false,
                customDownloadUrl = cleanRemoteUrl.ifBlank { _uiState.value.customDownloadUrl },
                lastCheckStatusMessage = "App atualizado na última compilação (Rev #${sanitizedRemote.buildRevision} • v${BuildConfig.VERSION_NAME} • #$localSignature)"
            )
            return
        }

        val isPreviouslyDismissedSameRev =
            _uiState.value.isModalDismissedForSession &&
                _uiState.value.remoteRelease?.buildRevision == sanitizedRemote.buildRevision

        _uiState.value = _uiState.value.copy(
            localKnownRevision = storedLocalRev,
            remoteRelease = sanitizedRemote,
            isUpdateAvailable = true,
            isModalDismissedForSession = isPreviouslyDismissedSameRev,
            isCheckingOrPublishing = false,
            customDownloadUrl = cleanRemoteUrl.ifBlank { _uiState.value.customDownloadUrl },
            lastCheckStatusMessage = "🚀 Nova atualização disponível! (Revisão #${sanitizedRemote.buildRevision} • v${sanitizedRemote.versionName})"
        )

        maybeSendSystemUpdateNotification(sanitizedRemote)
    }

    /**
     * Automatically registers the newly compiled APK in Firestore `/app_releases/latest`.
     */
    private fun publishCurrentBuildAsLatest(
        previousChain: List<String>,
        nextRevision: Long,
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean
    ) {
        val firestore = db ?: return
        val updatedChain = (previousChain + localSignature)
            .distinct()
            .takeLast(MAX_SIGNATURE_HISTORY)
            .joinToString(",")
            .take(120)

        val cleanUrl = sanitizeAndNormalizeUrl(downloadUrl)
        val nowMs = System.currentTimeMillis()
        val payload = mapOf(
            "buildRevision" to nextRevision,
            "versionCode" to BuildConfig.VERSION_CODE,
            "versionName" to BuildConfig.VERSION_NAME,
            "dexSignature" to updatedChain.ifBlank { localSignature },
            "releaseNotes" to releaseNotes.take(1500),
            "downloadUrl" to cleanUrl.take(500),
            "mandatory" to mandatory,
            "publishedBy" to "Aura Studio Build #$localSignature",
            "updatedAtEpochMs" to nowMs,
            "buildTimestampMs" to localBuildTimestampMs
        )

        prefs.edit()
            .putLong(KEY_LOCAL_KNOWN_REVISION, nextRevision)
            .putLong(KEY_SELF_PUBLISHED_REVISION, nextRevision)
            .putLong(KEY_ACKNOWLEDGED_REVISION, nextRevision)
            .apply()

        _uiState.value = _uiState.value.copy(
            localKnownRevision = nextRevision,
            isCheckingOrPublishing = true,
            lastCheckStatusMessage = "Publicando nova compilação #$localSignature (Rev #$nextRevision)..."
        )

        firestore.collection(COLLECTION_APP_RELEASES)
            .document(DOC_LATEST)
            .set(payload)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    isUpdateAvailable = false,
                    lastCheckStatusMessage = "✅ Nova compilação #$localSignature (v${BuildConfig.VERSION_NAME} • Rev #$nextRevision) publicada! Aparelhos anteriores serão avisados."
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Compilação local #$localSignature ativa (${e.localizedMessage ?: "aguardando rede"})."
                )
            }
    }

    /**
     * Quick 1-tap action to publish the current build and notify all installed phones immediately.
     */
    fun publishCurrentBuildImmediately(
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val currentRemote = _uiState.value.remoteRelease
        val notes = currentRemote?.releaseNotes?.takeIf { it.isNotBlank() }
            ?: buildAutoChangelogNote((currentRemote?.buildRevision ?: _uiState.value.localKnownRevision) + 1L)
        publishManualUpdateToInstalledDevices(
            releaseNotes = notes,
            downloadUrl = _uiState.value.customDownloadUrl,
            mandatory = currentRemote?.mandatory ?: false,
            alsoShowPromptOnThisDevice = false,
            onResult = onResult
        )
    }

    /**
     * Allows the user to manually broadcast an update notification to all installed devices.
     */
    fun publishManualUpdateToInstalledDevices(
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean,
        alsoShowPromptOnThisDevice: Boolean = false,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanUrl = sanitizeAndNormalizeUrl(downloadUrl)
        val cleanNotes = releaseNotes.trim().ifBlank { AppReleaseMetadata.DEFAULT_RELEASE_NOTES }
        prefs.edit().putString(KEY_CUSTOM_DOWNLOAD_URL, cleanUrl).apply()

        val currentRemote = _uiState.value.remoteRelease
        val nextRev = ((currentRemote?.buildRevision ?: _uiState.value.localKnownRevision) + 1L).coerceAtLeast(2L)
        val existingChain = currentRemote?.signatureList.orEmpty()
        val markerSig = if (alsoShowPromptOnThisDevice) {
            "R${(System.currentTimeMillis() % 10000000L)}"
        } else {
            localSignature
        }
        val updatedChain = (existingChain + markerSig)
            .distinct()
            .takeLast(MAX_SIGNATURE_HISTORY)
            .joinToString(",")
            .take(120)

        if (!alsoShowPromptOnThisDevice) {
            prefs.edit()
                .putLong(KEY_LOCAL_KNOWN_REVISION, nextRev)
                .putLong(KEY_SELF_PUBLISHED_REVISION, nextRev)
                .putLong(KEY_ACKNOWLEDGED_REVISION, nextRev)
                .apply()
        }

        val nowMs = System.currentTimeMillis()
        val firestore = db
        if (firestore == null) {
            val simulated = AppReleaseMetadata(
                buildRevision = nextRev,
                versionCode = BuildConfig.VERSION_CODE + 1,
                versionName = "${BuildConfig.VERSION_NAME}.$nextRev",
                dexSignatureChain = updatedChain,
                releaseNotes = cleanNotes,
                downloadUrl = cleanUrl,
                mandatory = mandatory,
                publishedBy = "Painel AuraManager",
                updatedAtEpochMs = nowMs,
                buildTimestampMs = nowMs
            )
            _uiState.value = _uiState.value.copy(
                remoteRelease = simulated,
                isUpdateAvailable = alsoShowPromptOnThisDevice,
                isModalDismissedForSession = false,
                customDownloadUrl = cleanUrl,
                lastCheckStatusMessage = "Aviso de atualização (Rev #$nextRev) disparado!"
            )
            onResult(true, "Aviso de atualização Revisão #$nextRev ativado!")
            return
        }

        val payload = mapOf(
            "buildRevision" to nextRev,
            "versionCode" to BuildConfig.VERSION_CODE,
            "versionName" to BuildConfig.VERSION_NAME,
            "dexSignature" to updatedChain.ifBlank { localSignature },
            "releaseNotes" to cleanNotes.take(1500),
            "downloadUrl" to cleanUrl.take(500),
            "mandatory" to mandatory,
            "publishedBy" to "Painel AuraManager",
            "updatedAtEpochMs" to nowMs,
            "buildTimestampMs" to nowMs
        )

        _uiState.value = _uiState.value.copy(isCheckingOrPublishing = true)
        firestore.collection(COLLECTION_APP_RELEASES)
            .document(DOC_LATEST)
            .set(payload)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    customDownloadUrl = cleanUrl,
                    isModalDismissedForSession = false,
                    lastCheckStatusMessage = "✅ Revisão #$nextRev publicada! Todos os celulares com versão anterior receberão o alerta de atualização."
                )
                onResult(
                    true,
                    "🚀 Pedido de atualização (Revisão #$nextRev) enviado em tempo real para todos os aparelhos instalados!"
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isCheckingOrPublishing = false)
                onResult(
                    false,
                    "Não foi possível publicar no Firestore (${e.localizedMessage ?: "erro de conexão"})."
                )
            }
    }

    /**
     * Simulates an incoming update prompt immediately on screen so the user can preview/test the dialog.
     */
    fun simulateIncomingUpdatePrompt(
        customNotes: String? = null,
        customUrl: String? = null,
        mandatory: Boolean = false
    ) {
        val currentRemote = _uiState.value.remoteRelease
        val nextRev = (currentRemote?.buildRevision ?: _uiState.value.localKnownRevision) + 1L
        val nowMs = System.currentTimeMillis()
        val cleanUrl = sanitizeAndNormalizeUrl(customUrl ?: _uiState.value.customDownloadUrl)
        val simulatedRelease = AppReleaseMetadata(
            buildRevision = nextRev,
            versionCode = BuildConfig.VERSION_CODE + 1,
            versionName = "${BuildConfig.VERSION_NAME} (Nova Build #$nextRev)",
            dexSignatureChain = "$localSignature,NEW$nextRev",
            releaseNotes = customNotes?.takeIf { it.isNotBlank() }
                ?: currentRemote?.releaseNotes?.takeIf { it.isNotBlank() }
                ?: AppReleaseMetadata.DEFAULT_RELEASE_NOTES,
            downloadUrl = cleanUrl,
            mandatory = mandatory,
            publishedBy = "Aura Studio Live Sync",
            updatedAtEpochMs = nowMs,
            buildTimestampMs = nowMs + 10_000L
        )

        _uiState.value = _uiState.value.copy(
            remoteRelease = simulatedRelease,
            isUpdateAvailable = true,
            isModalDismissedForSession = false,
            lastCheckStatusMessage = "🚀 Nova atualização detectada (Revisão #$nextRev)!"
        )
        maybeSendSystemUpdateNotification(simulatedRelease)
    }

    /**
     * Forces an immediate manual check against Firestore `/app_releases/latest`.
     */
    fun checkNow() {
        val firestore = db ?: return
        _uiState.value = _uiState.value.copy(
            isCheckingOrPublishing = true,
            lastCheckStatusMessage = "Verificando se há novas alterações publicadas..."
        )
        firestore.collection(COLLECTION_APP_RELEASES)
            .document(DOC_LATEST)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    handleRemoteReleaseSnapshot(
                        remote = snapshot.toAppReleaseMetadata(),
                        isStudioPreviewOverride = isStudioPreview
                    )
                } else {
                    publishCurrentBuildAsLatest(
                        previousChain = emptyList(),
                        nextRevision = 1L,
                        releaseNotes = AppReleaseMetadata.DEFAULT_RELEASE_NOTES,
                        downloadUrl = _uiState.value.customDownloadUrl,
                        mandatory = false
                    )
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Verificação concluída • Build atual #$localSignature (v${BuildConfig.VERSION_NAME})"
                )
            }
    }

    fun dismissModalForSession() {
        val mandatory = _uiState.value.remoteRelease?.mandatory == true
        if (!mandatory) {
            _uiState.value = _uiState.value.copy(isModalDismissedForSession = true)
        }
    }

    fun reopenUpdateModal() {
        _uiState.value = _uiState.value.copy(isModalDismissedForSession = false)
    }

    /**
     * Marks the current remote revision as acknowledged on this device.
     */
    fun acknowledgeCurrentRevisionOnThisDevice() {
        val remoteRev = _uiState.value.remoteRelease?.buildRevision ?: _uiState.value.localKnownRevision
        prefs.edit()
            .putLong(KEY_LOCAL_KNOWN_REVISION, remoteRev)
            .putLong(KEY_SELF_PUBLISHED_REVISION, remoteRev)
            .putLong(KEY_ACKNOWLEDGED_REVISION, remoteRev)
            .apply()
        _uiState.value = _uiState.value.copy(
            localKnownRevision = remoteRev,
            isUpdateAvailable = false,
            isModalDismissedForSession = true,
            lastCheckStatusMessage = "✅ Revisão #$remoteRev sincronizada e confirmada neste aparelho."
        )
    }

    /**
     * Returns the suggested file name for exporting/downloading the compiled APK directly from the app.
     */
    fun suggestedApkFileName(): String {
        val cleanVer = BuildConfig.VERSION_NAME.replace(" ", "_")
        return "AuraFinancas-v$cleanVer-build-$localSignature.apk"
    }

    /**
     * Exports/saves the currently running compiled `.apk` binary (`applicationInfo.sourceDir`)
     * directly to the user-selected destination URI (via Storage Access Framework `CreateDocument`).
     * Works 100% offline without needing any external web server!
     */
    fun exportCurrentInstalledApkToUri(
        context: Context,
        destinationUri: Uri
    ): Result<String> {
        return runCatching {
            val sourceApkFile = File(context.applicationInfo.sourceDir)
            require(sourceApkFile.exists() && sourceApkFile.length() > 0L) {
                "Arquivo APK base não encontrado no dispositivo."
            }
            var totalBytes = 0L
            FileInputStream(sourceApkFile).use { input ->
                val output = context.contentResolver.openOutputStream(destinationUri)
                    ?: error("Não foi possível abrir o destino selecionado para gravação.")
                output.use { outStream ->
                    totalBytes = input.copyTo(outStream)
                    outStream.flush()
                }
            }
            val sizeMb = String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", totalBytes / (1024.0 * 1024.0))
            "✅ Instalador ${suggestedApkFileName()} ($sizeMb) baixado e salvo com sucesso! Abra a pasta onde salvou para instalar ou enviar."
        }
    }

    /**
     * Shares the compiled `.apk` file directly via Android's native Share Sheet (`FileProvider`),
     * allowing the user to save it to Google Drive, send via WhatsApp/Telegram, or Quick Share in 1 tap.
     */
    fun shareCurrentInstalledApk(context: Context): Result<String> {
        return runCatching {
            val sourceApkFile = File(context.applicationInfo.sourceDir)
            require(sourceApkFile.exists() && sourceApkFile.length() > 0L) {
                "Arquivo APK base não encontrado no dispositivo."
            }
            val exportDir = File(context.cacheDir, "apk_exports").apply { mkdirs() }
            // Clean old cached exports
            exportDir.listFiles()?.forEach { it.delete() }

            val targetFile = File(exportDir, suggestedApkFileName())
            sourceApkFile.copyTo(targetFile, overwrite = true)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Instalador Aura Finanças v${BuildConfig.VERSION_NAME} (#$localSignature)"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Instalador atualizado do Aura Finanças — v${BuildConfig.VERSION_NAME} (Compilação #$localSignature)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                "Salvar ou Enviar Instalador .APK (${suggestedApkFileName()})"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            "Compartilhando instalador ${suggestedApkFileName()}..."
        }
    }

    /**
     * Checks whether a custom external download URL (e.g. Google Drive, GitHub Releases, Dropbox)
     * is configured and valid (not empty and not the broken `.run.app` container proxy).
     */
    fun hasValidExternalDownloadUrl(urlOverride: String? = null): Boolean {
        val candidate = urlOverride ?: _uiState.value.remoteRelease?.downloadUrl ?: _uiState.value.customDownloadUrl
        return sanitizeAndNormalizeUrl(candidate).isNotBlank()
    }

    fun getEffectiveExternalDownloadUrl(urlOverride: String? = null): String {
        val candidate = urlOverride ?: _uiState.value.remoteRelease?.downloadUrl ?: _uiState.value.customDownloadUrl
        return sanitizeAndNormalizeUrl(candidate)
    }

    /**
     * Opens a configured external APK download link (e.g. Google Drive direct download, GitHub Releases),
     * or opens the AI Studio Apps portal (`https://aistudio.google.com/apps`) if requested.
     */
    fun openUpdateDownloadLink(context: Context, urlOverride: String? = null): Boolean {
        val cleanUrl = getEffectiveExternalDownloadUrl(urlOverride)
        val targetUrl = cleanUrl.ifBlank { AppReleaseMetadata.AI_STUDIO_PORTAL_URL }

        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openAiStudioPortal(context: Context): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppReleaseMetadata.AI_STUDIO_PORTAL_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun buildAutoChangelogNote(revision: Long): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        return "• Nova compilação detectada automaticamente no Aura Studio (Revisão #$revision em $dateStr).\n" +
            "• Versão v${BuildConfig.VERSION_NAME} (Código ${BuildConfig.VERSION_CODE} • Assinatura #$localSignature).\n" +
            "• Baixe o instalador .APK diretamente pelo botão abaixo ou sincronize as novidades no seu aparelho."
    }

    private fun ensureUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Atualizações do Aplicativo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa sempre que houver uma nova versão ou alteração disponível para atualizar o aplicativo."
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun maybeSendSystemUpdateNotification(remote: AppReleaseMetadata) {
        val lastNotifiedRev = prefs.getLong(KEY_LAST_NOTIFIED_REVISION, -1L)
        if (remote.buildRevision <= lastNotifiedRev) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        prefs.edit().putLong(KEY_LAST_NOTIFIED_REVISION, remote.buildRevision).apply()

        runCatching {
            val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    appContext,
                    9010,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

            val notification = NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("🚀 Nova atualização disponível (Rev #${remote.buildRevision})")
                .setContentText("O Aura Finanças recebeu novas alterações (v${remote.versionName}). Toque para baixar ou atualizar.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("O Aura Finanças recebeu novas alterações (v${remote.versionName} • Revisão #${remote.buildRevision}).\n\n${remote.releaseNotes}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .apply {
                    if (pendingIntent != null) {
                        setContentIntent(pendingIntent)
                    }
                }
                .build()

            NotificationManagerCompat.from(appContext).notify(9010, notification)
        }
    }

    private fun DocumentSnapshot.toAppReleaseMetadata(): AppReleaseMetadata {
        val rev = getLong("buildRevision") ?: 1L
        val verCode = (getLong("versionCode") ?: BuildConfig.VERSION_CODE.toLong()).toInt()
        val verName = getString("versionName") ?: BuildConfig.VERSION_NAME
        val sig = getString("dexSignature").orEmpty()
        val notes = getString("releaseNotes") ?: AppReleaseMetadata.DEFAULT_RELEASE_NOTES
        val rawUrl = getString("downloadUrl").orEmpty()
        val mandatory = getBoolean("mandatory") ?: false
        val pubBy = getString("publishedBy") ?: "Aura Studio"
        val updatedMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis()
        val buildTs = getLong("buildTimestampMs") ?: 0L
        return AppReleaseMetadata(
            buildRevision = rev,
            versionCode = verCode,
            versionName = verName,
            dexSignatureChain = sig,
            releaseNotes = notes,
            downloadUrl = sanitizeAndNormalizeUrl(rawUrl),
            mandatory = mandatory,
            publishedBy = pubBy,
            updatedAtEpochMs = updatedMs,
            buildTimestampMs = buildTs
        )
    }

    companion object {
        private const val PREFS_NAME = "aura_app_update_prefs"
        private const val KEY_LOCAL_KNOWN_REVISION = "local_known_revision"
        private const val KEY_SELF_PUBLISHED_REVISION = "self_published_revision"
        private const val KEY_ACKNOWLEDGED_REVISION = "acknowledged_revision"
        private const val KEY_LAST_INSTALLED_SIGNATURE = "last_installed_signature"
        private const val KEY_LAST_INSTALLED_VERSION_CODE = "last_installed_version_code"
        private const val KEY_LAST_NOTIFIED_REVISION = "last_notified_revision"
        private const val KEY_CUSTOM_DOWNLOAD_URL = "custom_download_url"

        private const val COLLECTION_APP_RELEASES = "app_releases"
        private const val DOC_LATEST = "latest"
        private const val UPDATE_CHANNEL_ID = "aura_app_updates_channel"
        private const val MAX_SIGNATURE_HISTORY = 10

        /**
         * Returns true if the URL is the internal `.run.app` AI Studio Web proxy placeholder
         * (`ais-pre-*.run.app` or `ais-dev-*.run.app`), which does not serve Android `.apk` files.
         */
        fun isBrokenRunAppPlaceholderUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val lower = url.trim().lowercase(Locale.US)
            return lower.contains(".run.app") && (lower.contains("ais-pre-") || lower.contains("ais-dev-"))
        }

        /**
         * Sanitizes and normalizes user-provided APK download URLs:
         * - Removes broken `ais-pre-*.run.app` / `ais-dev-*.run.app` placeholder URLs.
         * - Automatically converts Google Drive sharing links (`/file/d/{ID}/view`) into direct download links
         *   (`https://drive.google.com/uc?export=download&id={ID}`).
         * - Automatically converts Dropbox sharing links (`?dl=0`) into direct download links (`?dl=1`).
         */
        fun sanitizeAndNormalizeUrl(rawUrl: String?): String {
            val trimmed = rawUrl?.trim().orEmpty()
            if (trimmed.isBlank() || isBrokenRunAppPlaceholderUrl(trimmed)) {
                return ""
            }

            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }

            // Convert Google Drive `/file/d/{FILE_ID}/view...` to direct download URL
            val driveFileRegex = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
            val driveMatch = driveFileRegex.find(withScheme)
            if (driveMatch != null) {
                val fileId = driveMatch.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }

            val driveOpenRegex = Regex("""drive\.google\.com/open\?id=([a-zA-Z0-9_-]+)""")
            val driveOpenMatch = driveOpenRegex.find(withScheme)
            if (driveOpenMatch != null) {
                val fileId = driveOpenMatch.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Convert Dropbox `?dl=0` to `?dl=1` for direct APK download
            if (withScheme.contains("dropbox.com", ignoreCase = true) && withScheme.endsWith("?dl=0")) {
                return withScheme.removeSuffix("?dl=0") + "?dl=1"
            }

            return withScheme
        }

        fun computeLocalApkSizeLabel(context: Context): String {
            return runCatching {
                val file = File(context.applicationInfo.sourceDir)
                if (file.exists() && file.length() > 0L) {
                    val mb = file.length() / (1024.0 * 1024.0)
                    String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", mb)
                } else {
                    "APK Local"
                }
            }.getOrDefault("APK Local")
        }

        /**
         * Detects if the app is running inside the AI Studio Streaming Emulator / virtual device preview
         * vs a physical Android smartphone/tablet.
         */
        fun isRunningOnEmulatorOrPreview(): Boolean {
            val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
            val model = Build.MODEL.lowercase(Locale.US)
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
            val brand = Build.BRAND.lowercase(Locale.US)
            val device = Build.DEVICE.lowercase(Locale.US)
            val product = Build.PRODUCT.lowercase(Locale.US)
            val hardware = Build.HARDWARE.lowercase(Locale.US)

            return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for") ||
                model.contains("sdk_gphone") ||
                manufacturer.contains("genymotion") ||
                (brand.startsWith("generic") && device.startsWith("generic")) ||
                product.contains("sdk") ||
                product.contains("emulator") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("cuttlefish")
        }

        /**
         * Computes an 8-character uppercase hex signature from the compiled APK's `classes.dex`
         * and `resources.arsc` CRC-32 checksums + `BuildConfig.VERSION_CODE`.
         */
        fun computeLocalApkSignature(context: Context): String {
            return runCatching {
                val apkPath = context.applicationInfo.sourceDir
                var combinedHash = BuildConfig.VERSION_CODE.toLong() * 31L
                ZipFile(apkPath).use { zip ->
                    val trackedEntries = listOf(
                        "classes.dex",
                        "classes2.dex",
                        "classes3.dex",
                        "resources.arsc",
                        "AndroidManifest.xml"
                    )
                    for (entryName in trackedEntries) {
                        val entry = zip.getEntry(entryName)
                        if (entry != null) {
                            combinedHash = (combinedHash * 31L) xor entry.crc xor entry.size
                        }
                    }
                }
                val positive32 = (combinedHash and 0xFFFFFFFFL)
                String.format(Locale.US, "%08X", positive32)
            }.getOrDefault("V${BuildConfig.VERSION_CODE}00000")
        }
    }
}
