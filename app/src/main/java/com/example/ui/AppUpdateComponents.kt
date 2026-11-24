package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppReleaseMetadata
import com.example.data.AppUpdateManager
import com.example.data.AppUpdateUiState
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold
import kotlinx.coroutines.delay

/**
 * Top Pill Banner that appears automatically across the app when a new update is detected in real time
 * OR when the current device just upgraded and auto-published its new version to Firestore.
 * Automatically dismisses transient upgrade notifications after 3 seconds.
 */
@Composable
fun AppUpdateTopPillBanner(
    updateState: AppUpdateUiState,
    onOpenUpdateModal: () -> Unit,
    onClearUpgradedBanner: () -> Unit
) {
    val remote = updateState.remoteRelease
    val upgradedMsg = updateState.justUpgradedBannerMessage

    // Auto-dismiss transient upgrade message after 3 seconds
    LaunchedEffect(upgradedMsg) {
        if (!upgradedMsg.isNullOrBlank()) {
            delay(3000L)
            onClearUpgradedBanner()
        }
    }

    AnimatedVisibility(visible = (updateState.isUpdateAvailable && remote != null) || !upgradedMsg.isNullOrBlank()) {
        if (updateState.isUpdateAvailable && remote != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = IncomeEmerald.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.5.dp, IncomeEmerald, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable { onOpenUpdateModal() }
                    .testTag("live_update_available_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(IncomeEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚀 Nova versão disponível: v${remote.versionName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IncomeEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Toque para ver as novidades e baixar a atualização",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = IncomeEmerald
                    ) {
                        Text(
                            text = "Atualizar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        } else if (!upgradedMsg.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("live_update_published_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = upgradedMsg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearUpgradedBanner,
                        modifier = Modifier.size(26.dp)
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
}

/**
 * Modal Dialog that pops up automatically when a newer version is published in Firestore.
 */
@Composable
fun AppUpdateAvailableDialog(
    updateState: AppUpdateUiState,
    updateManager: AppUpdateManager,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val remote = updateState.remoteRelease ?: return
    if (!updateState.showUpdateDialog) return

    val simpleNotes = remote.releaseNotes.ifBlank { AppReleaseMetadata.RELEASE_NOTES_SIMPLE }

    AlertDialog(
        onDismissRequest = {
            if (!remote.forceUpdate) {
                updateManager.dismissUpdateDialog()
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(IncomeEmerald.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = IncomeEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nova Atualização do Omni",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = IncomeEmerald.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Versão ${remote.versionName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Confira o que mudou nesta versão:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Novidades da versão:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = simpleNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (updateState.isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Baixando instalador da nova versão...",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    updateManager.downloadAndInstallUpdate(context) { msg ->
                        onShowMessage(msg)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("confirm_update_now_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Baixar atualização", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!remote.forceUpdate) {
                TextButton(
                    onClick = { updateManager.dismissUpdateDialog() },
                    modifier = Modifier.testTag("dismiss_update_later_btn")
                ) {
                    Text("Depois")
                }
            }
        }
    )
}

/**
 * Clean, Simplified Update Card in the Profile & Settings screen:
 * Displays ONLY:
 * - Installed version
 * - "Verificar atualização" button
 * - And ONLY when a newer version is available in Firestore: "Baixar atualização" button + simple release notes.
 */
@Composable
fun AppUpdateControlCenterCard(
    updateState: AppUpdateUiState,
    updateManager: AppUpdateManager,
    publisherName: String = "",
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val remote = updateState.remoteRelease
    val triggerFeedback = rememberOmniFeedback(onShowBannerMessage = onShowMessage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_update_control_center_card"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (updateState.isUpdateAvailable) {
                                    SavingsGold.copy(alpha = 0.2f)
                                } else {
                                    IncomeEmerald.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = if (updateState.isUpdateAvailable) SavingsGold else IncomeEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Atualizações do Aplicativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Versão instalada: v${updateState.localVersionName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (updateState.isUpdateAvailable) {
                        SavingsGold.copy(alpha = 0.2f)
                    } else {
                        IncomeEmerald.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = if (updateState.isUpdateAvailable) "Nova versão" else "Atualizado ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (updateState.isUpdateAvailable) SavingsGold else IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Show release notes and "Baixar atualização" ONLY when a newer version is available in Firestore
            if (updateState.isUpdateAvailable && remote != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "O que mudou na versão v${remote.versionName}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeEmerald
                        )
                        Text(
                            text = remote.releaseNotes.ifBlank { AppReleaseMetadata.RELEASE_NOTES_SIMPLE },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        updateManager.checkNow()
                        triggerFeedback(
                            if (updateState.isUpdateAvailable && remote != null) {
                                "Nova versão v${remote.versionName} disponível para baixar!"
                            } else {
                                "Você já está com a versão mais recente instalada (v${updateState.localVersionName})."
                            }
                        )
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("check_app_update_now_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verificar atualização", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                if (updateState.isUpdateAvailable && remote != null) {
                    Button(
                        onClick = {
                            updateManager.downloadAndInstallUpdate(context) { msg ->
                                onShowMessage(msg)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_remote_update_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar atualização", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
