package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppReleaseMetadata
import com.example.data.AppUpdateManager
import com.example.data.AppUpdateUiState
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalOrganicCardRadius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppUpdateTopPillBanner(
    updateState: AppUpdateUiState,
    onOpenUpdateModal: () -> Unit,
    onClearUpgradedBanner: () -> Unit
) {
    val remote = updateState.remoteRelease
    val upgradedMsg = updateState.justUpgradedBannerMessage

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = upgradedMsg != null) {
            upgradedMsg?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("just_upgraded_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IncomeEmerald.copy(alpha = 0.16f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IncomeEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearUpgradedBanner,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = updateState.isUpdateAvailable && remote != null) {
            if (remote != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenUpdateModal() }
                        .testTag("app_update_top_banner"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Atualização disponível",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🚀 Nova atualização disponível (Rev #${remote.buildRevision})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Alterações foram feitas no projeto. Toque para baixar o APK ou atualizar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Baixar APK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdateAvailableDialog(
    updateState: AppUpdateUiState,
    updateManager: AppUpdateManager,
    onShowMessage: (String) -> Unit
) {
    val remote = updateState.remoteRelease ?: return
    if (!updateState.isUpdateAvailable) return
    if (updateState.isModalDismissedForSession && !remote.mandatory) return

    val context = LocalContext.current
    val dateFormatted = remember(remote.updatedAtEpochMs) {
        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
            .format(Date(remote.updatedAtEpochMs))
    }

    val hasExternalApkLink = remember(remote.downloadUrl, updateState.customDownloadUrl) {
        updateManager.hasValidExternalDownloadUrl(remote.downloadUrl)
    }
    val effectiveExternalUrl = remember(remote.downloadUrl, updateState.customDownloadUrl) {
        updateManager.getEffectiveExternalDownloadUrl(remote.downloadUrl)
    }

    // Native Android Storage Access Framework launcher to save the .APK file directly to Downloads / Drive
    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { destUri ->
        if (destUri != null) {
            updateManager.exportCurrentInstalledApkToUri(context, destUri)
                .onSuccess { msg -> onShowMessage(msg) }
                .onFailure { e ->
                    onShowMessage("Não foi possível salvar o APK: ${e.localizedMessage ?: "erro de gravação"}")
                }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!remote.mandatory) {
                updateManager.dismissModalForSession()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !remote.mandatory,
            dismissOnClickOutside = !remote.mandatory
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (remote.mandatory) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = if (remote.mandatory) "ATUALIZAÇÃO OBRIGATÓRIA" else "NOVA ALTERAÇÃO DETECTADA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remote.mandatory) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Atualizar Aura Finanças",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Foram feitas novas alterações no projeto Aura Finanças. Escolha abaixo como deseja baixar o instalador .APK ou sincronizar seu aparelho:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Version comparison box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Instalada neste aparelho:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v${updateState.localVersionName} (#${updateState.localBuildSignature})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nova versão disponível:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rev #${remote.buildRevision} • v${remote.versionName} (#${remote.latestSignature.ifBlank { "NOVA" }})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Publicada em $dateFormatted • Pacote ${updateState.localApkSizeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Release notes / Changelog
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📋 O que há de novo:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = remote.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // PRIMARY ACTION 1: If a custom external APK link (Google Drive / GitHub) is configured, open it directly;
                // otherwise, directly download/save the compiled .APK file onto the device!
                if (hasExternalApkLink) {
                    Button(
                        onClick = {
                            val opened = updateManager.openUpdateDownloadLink(context, effectiveExternalUrl)
                            if (!opened) {
                                copyUrlToClipboard(context, effectiveExternalUrl)
                                onShowMessage("Link direto do APK copiado: $effectiveExternalUrl")
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_confirm_update_now")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Baixar Novo APK (Link Direto)",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            saveApkLauncher.launch(updateManager.suggestedApkFileName())
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_apk_direct_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Salvar Instalador .APK no Celular (${updateState.localApkSizeLabel})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            saveApkLauncher.launch(updateManager.suggestedApkFileName())
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_confirm_update_now")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Baixar Arquivo .APK no Celular (${updateState.localApkSizeLabel})",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // ACTION 2: Share .APK directly to Google Drive / WhatsApp / Quick Share
                FilledTonalButton(
                    onClick = {
                        updateManager.shareCurrentInstalledApk(context)
                            .onSuccess { msg -> onShowMessage(msg) }
                            .onFailure {
                                saveApkLauncher.launch(updateManager.suggestedApkFileName())
                            }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_apk_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar / Salvar .APK (Google Drive ou WhatsApp)",
                        fontWeight = FontWeight.Bold
                    )
                }

                // ACTION 3: Open AI Studio Apps Portal or Copy External Link + Mark as Updated
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasExternalApkLink) {
                                copyUrlToClipboard(context, effectiveExternalUrl)
                                onShowMessage("Link direto do APK copiado: $effectiveExternalUrl")
                            } else {
                                val opened = updateManager.openAiStudioPortal(context)
                                if (opened) {
                                    onShowMessage("No AI Studio: toque no menu ⋮ / Configurações → Generate APK para baixar da nuvem.")
                                } else {
                                    copyUrlToClipboard(context, AppReleaseMetadata.AI_STUDIO_PORTAL_URL)
                                    onShowMessage("Link do AI Studio copiado!")
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (hasExternalApkLink) Icons.Default.ContentCopy else Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasExternalApkLink) "Copiar Link" else "Abrir AI Studio",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (!remote.mandatory) {
                        OutlinedButton(
                            onClick = {
                                updateManager.acknowledgeCurrentRevisionOnThisDevice()
                                onShowMessage("✅ Revisão #${remote.buildRevision} sincronizada e confirmada neste aparelho!")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Já Atualizei", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!remote.mandatory) {
                TextButton(
                    onClick = { updateManager.dismissModalForSession() },
                    modifier = Modifier.testTag("btn_snooze_update_modal")
                ) {
                    Text("Lembrar Depois")
                }
            }
        }
    )
}

@Composable
fun AppUpdateControlCenterCard(
    updateState: AppUpdateUiState,
    updateManager: AppUpdateManager,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    var downloadUrlInput by remember(updateState.customDownloadUrl) {
        mutableStateOf(updateState.customDownloadUrl)
    }
    var releaseNotesInput by remember(updateState.remoteRelease?.releaseNotes) {
        mutableStateOf(
            updateState.remoteRelease?.releaseNotes ?: AppReleaseMetadata.DEFAULT_RELEASE_NOTES
        )
    }
    var mandatoryToggle by remember(updateState.remoteRelease?.mandatory) {
        mutableStateOf(updateState.remoteRelease?.mandatory ?: false)
    }
    var isExpanded by remember { mutableStateOf(false) }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { destUri ->
        if (destUri != null) {
            updateManager.exportCurrentInstalledApkToUri(context, destUri)
                .onSuccess { msg -> onShowMessage(msg) }
                .onFailure { e ->
                    onShowMessage("Não foi possível salvar o APK: ${e.localizedMessage ?: "erro de gravação"}")
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_app_update_control_center"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
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
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Atualizações & Instalador .APK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = IncomeEmerald.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "LIVE SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "v${updateState.localVersionName} • Build #${updateState.localBuildSignature} • ${updateState.localApkSizeLabel} • Rev #${updateState.remoteRelease?.buildRevision ?: updateState.localKnownRevision}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { updateManager.checkNow() },
                    modifier = Modifier.testTag("btn_check_updates_now")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Verificar atualizações agora",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status summary pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (updateState.isUpdateAvailable) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = updateState.lastCheckStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (updateState.isUpdateAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Você pode baixar o arquivo instalador .APK diretamente para a pasta Downloads do celular, enviar para o Google Drive/WhatsApp ou avisar outros aparelhos em tempo real.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct APK Download & Share Row (works 100% offline/locally without any broken web link)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        saveApkLauncher.launch(updateManager.suggestedApkFileName())
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_local_apk_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Baixar .APK (${updateState.localApkSizeLabel})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        updateManager.shareCurrentInstalledApk(context)
                            .onSuccess { msg -> onShowMessage(msg) }
                            .onFailure {
                                saveApkLauncher.launch(updateManager.suggestedApkFileName())
                            }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_share_local_apk_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Enviar (Drive/Zap)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Sync Broadcast & Test Modal Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        updateManager.publishCurrentBuildImmediately { _, msg ->
                            onShowMessage(msg)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_publish_update_now")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Publicar .APK p/ Todos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        updateManager.simulateIncomingUpdatePrompt(
                            customNotes = releaseNotesInput,
                            customUrl = downloadUrlInput,
                            mandatory = mandatoryToggle
                        )
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("btn_quick_preview_update_modal")
                ) {
                    Text(
                        text = "Testar Modal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Configurar Link na Nuvem (Google Drive / GitHub) & Notas",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Dica: Toque em 'Enviar (Drive/Zap)' acima para salvar o APK no seu Google Drive. Se colar o link de compartilhamento do Google Drive abaixo, o app converte automaticamente em download direto!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = downloadUrlInput,
                        onValueChange = { downloadUrlInput = it },
                        label = { Text("Link Externo do APK (Opcional: Google Drive / Dropbox / GitHub)") },
                        placeholder = { Text("Ex: https://drive.google.com/file/d/.../view") },
                        supportingText = {
                            Text("Se deixado em branco, o botão 'Baixar Arquivo .APK' salvará o instalador diretamente na memória do celular.")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_download_url")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = releaseNotesInput,
                        onValueChange = { releaseNotesInput = it },
                        label = { Text("Notas da Atualização (O que mudou)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_release_notes")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Atualização Obrigatória",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Se ativo, exige que o aparelho antigo atualize antes de continuar usando.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mandatoryToggle,
                            onCheckedChange = { mandatoryToggle = it },
                            modifier = Modifier.testTag("switch_mandatory_update")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val normalized = AppUpdateManager.sanitizeAndNormalizeUrl(downloadUrlInput)
                            downloadUrlInput = normalized
                            updateManager.publishManualUpdateToInstalledDevices(
                                releaseNotes = releaseNotesInput,
                                downloadUrl = normalized,
                                mandatory = mandatoryToggle,
                                alsoShowPromptOnThisDevice = false
                            ) { _, msg ->
                                onShowMessage(msg)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_publish_update_to_devices")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Salvar e Enviar Aviso aos Aparelhos Instalados",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            updateManager.simulateIncomingUpdatePrompt(
                                customNotes = releaseNotesInput,
                                customUrl = downloadUrlInput,
                                mandatory = mandatoryToggle
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_update_prompt")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visualizar / Testar Modal de Atualização na Tela",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun copyUrlToClipboard(context: Context, url: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Link de Atualização Aura Finanças", url))
    }
}
