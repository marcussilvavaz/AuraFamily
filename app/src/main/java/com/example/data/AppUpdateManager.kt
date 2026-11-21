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
import com.example.BuildConfig
import com.example.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val downloadUrl: String = BuildConfig.DEFAULT_UPDATE_URL,
    val mandatory: Boolean = false,
    val publishedBy: String = "Aura Studio Auto-Sync",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val signatureList: List<String>
        get() = dexSignatureChain.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val latestSignature: String
        get() = signatureList.lastOrNull().orEmpty()

    companion object {
        const val DEFAULT_RELEASE_NOTES =
            "• Novas alterações e melhorias aplicadas no projeto Aura Finanças.\n" +
            "• Módulo de Saúde, Inegociáveis, Cronômetro 1h Pós-Refeição e Relatório PDF.\n" +
            "• Sincronização em tempo real com o Firebase (AuraManager)."
    }
}

data class AppUpdateUiState(
    val localVersionCode: Int = BuildConfig.VERSION_CODE,
    val localVersionName: String = BuildConfig.VERSION_NAME,
    val localBuildSignature: String = "",
    val localKnownRevision: Long = 1L,
    val remoteRelease: AppReleaseMetadata? = null,
    val isUpdateAvailable: Boolean = false,
    val isModalDismissedForSession: Boolean = false,
    val isCheckingOrPublishing: Boolean = false,
    val lastCheckStatusMessage: String = "Sincronizado com o servidor de atualizações",
    val customDownloadUrl: String = BuildConfig.DEFAULT_UPDATE_URL,
    val justUpgradedBannerMessage: String? = null
)

class AppUpdateManager(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val db: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance(
            appContext.getString(R.string.firestore_database_id)
        )
    }.getOrNull()

    private val localSignature: String = computeLocalApkSignature(appContext)
    private var snapshotListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        AppUpdateUiState(
            localVersionCode = BuildConfig.VERSION_CODE,
            localVersionName = BuildConfig.VERSION_NAME,
            localBuildSignature = localSignature,
            localKnownRevision = prefs.getLong(KEY_LOCAL_KNOWN_REVISION, 1L),
            customDownloadUrl = prefs.getString(KEY_CUSTOM_DOWNLOAD_URL, BuildConfig.DEFAULT_UPDATE_URL)
                ?: BuildConfig.DEFAULT_UPDATE_URL
        )
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        ensureUpdateNotificationChannel()
        detectLocalApkUpgrade()
        startRealtimeUpdateListener()
    }

    /**
     * Checks if the user just installed a newer APK over a previous one on this device.
     */
    private fun detectLocalApkUpgrade() {
        val previousRecordedSig = prefs.getString(KEY_LAST_INSTALLED_SIGNATURE, null)
        if (previousRecordedSig != null && previousRecordedSig != localSignature) {
            prefs.edit()
                .putString(KEY_LAST_INSTALLED_SIGNATURE, localSignature)
                .apply()
            _uiState.value = _uiState.value.copy(
                justUpgradedBannerMessage = "✅ Aplicativo atualizado com sucesso para a compilação #$localSignature (v${BuildConfig.VERSION_NAME})!"
            )
        } else if (previousRecordedSig == null) {
            prefs.edit()
                .putString(KEY_LAST_INSTALLED_SIGNATURE, localSignature)
                .apply()
        }
    }

    fun clearJustUpgradedBanner() {
        _uiState.value = _uiState.value.copy(justUpgradedBannerMessage = null)
    }

    /**
     * Starts a real-time Firestore snapshot listener on `/app_releases/latest`.
     * Whenever a new build is compiled in AI Studio, its `localSignature` changes automatically.
     * The new build registers itself in `/app_releases/latest`, and any older installed APK
     * immediately receives the update event and prompts the user to update.
     */
    fun startRealtimeUpdateListener() {
        val firestore = db ?: return
        snapshotListener?.remove()

        val docRef = firestore.collection(COLLECTION_APP_RELEASES).document(DOC_LATEST)
        snapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Verificação local ativa (Build #$localSignature)"
                )
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                // First build ever connecting to this Firestore collection: publish initial release metadata
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
            handleRemoteReleaseSnapshot(remote)
        }
    }

    private fun handleRemoteReleaseSnapshot(remote: AppReleaseMetadata) {
        val chain = remote.signatureList
        val latestRemoteSig = remote.latestSignature
        val localVer = BuildConfig.VERSION_CODE

        // Sync custom download URL if remote has a non-blank one
        if (remote.downloadUrl.isNotBlank() && remote.downloadUrl != _uiState.value.customDownloadUrl) {
            prefs.edit().putString(KEY_CUSTOM_DOWNLOAD_URL, remote.downloadUrl).apply()
        }

        // Case 1: Our running APK has a brand-new code signature that is NOT in the remote chain yet,
        // and our versionCode is >= remote.versionCode.
        // This happens automatically every time code is modified and compiled here in AI Studio!
        val isBrandNewUnregisteredBuild = !chain.contains(localSignature) && localVer >= remote.versionCode

        if (isBrandNewUnregisteredBuild) {
            val nextRev = (remote.buildRevision + 1L).coerceAtLeast(1L)
            publishCurrentBuildAsLatest(
                previousChain = chain,
                nextRevision = nextRev,
                releaseNotes = buildAutoChangelogNote(nextRev),
                downloadUrl = remote.downloadUrl.ifBlank { _uiState.value.customDownloadUrl },
                mandatory = remote.mandatory
            )
            return
        }

        // Case 2: Our running APK's signature matches the latest signature in Firestore
        // (and no manual forced revision higher than our acknowledged revision is pending).
        val ignoredManualRev = prefs.getLong(KEY_SELF_PUBLISHED_REVISION, -1L)
        if (latestRemoteSig == localSignature && localVer >= remote.versionCode && remote.buildRevision <= ignoredManualRev.coerceAtLeast(remote.buildRevision)) {
            // Check if a manual update broadcast bumped buildRevision above what this device had installed
            val storedLocalRev = prefs.getLong(KEY_LOCAL_KNOWN_REVISION, remote.buildRevision)
            val hasManualBumpForOtherDevices =
                remote.buildRevision > storedLocalRev && ignoredManualRev < remote.buildRevision

            if (!hasManualBumpForOtherDevices) {
                prefs.edit().putLong(KEY_LOCAL_KNOWN_REVISION, remote.buildRevision).apply()
                _uiState.value = _uiState.value.copy(
                    localKnownRevision = remote.buildRevision,
                    remoteRelease = remote,
                    isUpdateAvailable = false,
                    isCheckingOrPublishing = false,
                    customDownloadUrl = remote.downloadUrl.ifBlank { _uiState.value.customDownloadUrl },
                    lastCheckStatusMessage = "App atualizado na última compilação (Rev #${remote.buildRevision} • #$localSignature)"
                )
                return
            }
        }

        // Case 3: Remote build is newer than this installed APK!
        // Either:
        // - localVersionCode < remote.versionCode
        // - localSignature is an older entry in the signature chain (latestRemoteSig != localSignature)
        // - or a manual update broadcast incremented remote.buildRevision
        val storedLocalRev = prefs.getLong(KEY_LOCAL_KNOWN_REVISION, 1L)
        val isPreviouslyDismissedSameRev =
            _uiState.value.isModalDismissedForSession &&
                _uiState.value.remoteRelease?.buildRevision == remote.buildRevision

        _uiState.value = _uiState.value.copy(
            localKnownRevision = storedLocalRev,
            remoteRelease = remote,
            isUpdateAvailable = true,
            isModalDismissedForSession = isPreviouslyDismissedSameRev,
            isCheckingOrPublishing = false,
            customDownloadUrl = remote.downloadUrl.ifBlank { _uiState.value.customDownloadUrl },
            lastCheckStatusMessage = "Nova atualização disponível! (Revisão #${remote.buildRevision} • v${remote.versionName})"
        )

        maybeSendSystemUpdateNotification(remote)
    }

    /**
     * Automatically registers the newly compiled APK in Firestore `/app_releases/latest`
     * so all older installed APKs on phones immediately get prompted to update.
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

        val nowMs = System.currentTimeMillis()
        val payload = mapOf(
            "buildRevision" to nextRevision,
            "versionCode" to BuildConfig.VERSION_CODE,
            "versionName" to BuildConfig.VERSION_NAME,
            "dexSignature" to updatedChain.ifBlank { localSignature },
            "releaseNotes" to releaseNotes.take(1500),
            "downloadUrl" to downloadUrl.ifBlank { BuildConfig.DEFAULT_UPDATE_URL }.take(500),
            "mandatory" to mandatory,
            "publishedBy" to "Aura Studio Build #$localSignature",
            "updatedAtEpochMs" to nowMs
        )

        prefs.edit()
            .putLong(KEY_LOCAL_KNOWN_REVISION, nextRevision)
            .putLong(KEY_SELF_PUBLISHED_REVISION, nextRevision)
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
                    lastCheckStatusMessage = "Nova compilação #$localSignature registrada! Aparelhos anteriores serão avisados."
                )
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Compilação local #$localSignature ativa."
                )
            }
    }

    /**
     * Allows the user to manually broadcast an update notification to all installed devices,
     * customize the changelog, set the download URL, and optionally require mandatory update.
     */
    fun publishManualUpdateToInstalledDevices(
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean,
        alsoShowPromptOnThisDevice: Boolean = false,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanUrl = downloadUrl.trim().ifBlank { BuildConfig.DEFAULT_UPDATE_URL }
        val cleanNotes = releaseNotes.trim().ifBlank { AppReleaseMetadata.DEFAULT_RELEASE_NOTES }
        prefs.edit().putString(KEY_CUSTOM_DOWNLOAD_URL, cleanUrl).apply()

        val currentRemote = _uiState.value.remoteRelease
        val nextRev = ((currentRemote?.buildRevision ?: _uiState.value.localKnownRevision) + 1L).coerceAtLeast(2L)
        val existingChain = currentRemote?.signatureList.orEmpty()
        // If alsoShowPromptOnThisDevice is true, append a synthetic release marker so even devices with the same APK get prompted
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
                .apply()
        }

        val firestore = db
        if (firestore == null) {
            // Offline / local simulation fallback
            val simulated = AppReleaseMetadata(
                buildRevision = nextRev,
                versionCode = BuildConfig.VERSION_CODE + 1,
                versionName = "${BuildConfig.VERSION_NAME}.$nextRev",
                dexSignatureChain = updatedChain,
                releaseNotes = cleanNotes,
                downloadUrl = cleanUrl,
                mandatory = mandatory,
                publishedBy = "Painel AuraManager",
                updatedAtEpochMs = System.currentTimeMillis()
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
            "updatedAtEpochMs" to System.currentTimeMillis()
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
                    lastCheckStatusMessage = "Revisão #$nextRev publicada! Todos os apps instalados receberão o pedido de atualização."
                )
                onResult(
                    true,
                    "Pedido de atualização (Revisão #$nextRev) enviado em tempo real para todos os aparelhos instalados!"
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isCheckingOrPublishing = false)
                onResult(
                    false,
                    "Não foi possível publicar no Firestore (${e.localizedMessage ?: "erro de permissão"}). Verifique as regras do Firebase."
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
        val simulatedRelease = AppReleaseMetadata(
            buildRevision = nextRev,
            versionCode = BuildConfig.VERSION_CODE + 1,
            versionName = "${BuildConfig.VERSION_NAME} (Nova Build #$nextRev)",
            dexSignatureChain = "$localSignature,NEW$nextRev",
            releaseNotes = customNotes?.takeIf { it.isNotBlank() }
                ?: currentRemote?.releaseNotes?.takeIf { it.isNotBlank() }
                ?: AppReleaseMetadata.DEFAULT_RELEASE_NOTES,
            downloadUrl = customUrl?.takeIf { it.isNotBlank() }
                ?: _uiState.value.customDownloadUrl,
            mandatory = mandatory,
            publishedBy = "Aura Studio Live Sync",
            updatedAtEpochMs = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            remoteRelease = simulatedRelease,
            isUpdateAvailable = true,
            isModalDismissedForSession = false,
            lastCheckStatusMessage = "Nova atualização detectada (Revisão #$nextRev)!"
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
                    handleRemoteReleaseSnapshot(snapshot.toAppReleaseMetadata())
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCheckingOrPublishing = false,
                        lastCheckStatusMessage = "Seu aplicativo já está na versão mais recente (#$localSignature)."
                    )
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(
                    isCheckingOrPublishing = false,
                    lastCheckStatusMessage = "Verificação concluída • Build atual #$localSignature"
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
     * Marks the current remote revision as acknowledged on this device if the user chooses
     * "Já atualizei / Marcar como atualizado neste aparelho".
     */
    fun acknowledgeCurrentRevisionOnThisDevice() {
        val remoteRev = _uiState.value.remoteRelease?.buildRevision ?: return
        prefs.edit()
            .putLong(KEY_LOCAL_KNOWN_REVISION, remoteRev)
            .putLong(KEY_SELF_PUBLISHED_REVISION, remoteRev)
            .apply()
        _uiState.value = _uiState.value.copy(
            localKnownRevision = remoteRev,
            isUpdateAvailable = false,
            isModalDismissedForSession = true,
            lastCheckStatusMessage = "Versão Revisão #$remoteRev confirmada neste aparelho."
        )
    }

    fun openUpdateDownloadLink(context: Context, urlOverride: String? = null): Boolean {
        val targetUrl = (urlOverride ?: _uiState.value.remoteRelease?.downloadUrl ?: _uiState.value.customDownloadUrl)
            .trim()
            .ifBlank { BuildConfig.DEFAULT_UPDATE_URL }

        val normalizedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl
        } else {
            "https://$targetUrl"
        }

        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun buildAutoChangelogNote(revision: Long): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date())
        return "• Nova compilação detectada automaticamente no Aura Studio (Revisão #$revision em $dateStr).\n" +
            "• Assinatura do código-fonte: #$localSignature (v${BuildConfig.VERSION_NAME}).\n" +
            "• Instale o novo APK gerado para aplicar as alterações mais recentes no seu aparelho."
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
                .setContentText("O Aura Finanças recebeu novas alterações. Toque para atualizar seu aplicativo.")
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
        val url = getString("downloadUrl") ?: BuildConfig.DEFAULT_UPDATE_URL
        val mandatory = getBoolean("mandatory") ?: false
        val pubBy = getString("publishedBy") ?: "Aura Studio"
        val updatedMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis()
        return AppReleaseMetadata(
            buildRevision = rev,
            versionCode = verCode,
            versionName = verName,
            dexSignatureChain = sig,
            releaseNotes = notes,
            downloadUrl = url,
            mandatory = mandatory,
            publishedBy = pubBy,
            updatedAtEpochMs = updatedMs
        )
    }

    companion object {
        private const val PREFS_NAME = "aura_app_update_prefs"
        private const val KEY_LOCAL_KNOWN_REVISION = "local_known_revision"
        private const val KEY_SELF_PUBLISHED_REVISION = "self_published_revision"
        private const val KEY_LAST_INSTALLED_SIGNATURE = "last_installed_signature"
        private const val KEY_LAST_NOTIFIED_REVISION = "last_notified_revision"
        private const val KEY_CUSTOM_DOWNLOAD_URL = "custom_download_url"

        private const val COLLECTION_APP_RELEASES = "app_releases"
        private const val DOC_LATEST = "latest"
        private const val UPDATE_CHANNEL_ID = "aura_app_updates_channel"
        private const val MAX_SIGNATURE_HISTORY = 10

        /**
         * Computes an 8-character uppercase hex signature from the compiled APK's `classes.dex`
         * and `resources.arsc` CRC-32 checksums + `BuildConfig.VERSION_CODE`.
         * Whenever any Kotlin code or XML resource is modified and recompiled in AI Studio,
         * this signature changes automatically!
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
