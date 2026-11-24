package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AlarmScheduler
import com.example.alarm.TaskRingtoneCatalog
import com.example.alarm.TaskTonePreviewPlayer
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationRepository

/**
 * Helper to resolve a readable file name from a content:// URI and take persistable read permission.
 */
fun resolveAndPersistCustomAudioUri(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    val queriedName = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
    val ringtoneTitle = runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()
    return (queriedName ?: ringtoneTitle ?: "Áudio personalizado")
        .removeSuffix(".mp3")
        .removeSuffix(".ogg")
        .removeSuffix(".wav")
        .removeSuffix(".m4a")
}

/**
 * Componente Único Global de Sons e Notificações (exibido apenas em Perfil -> Configurações -> Sons e Notificações).
 * Contém exclusivamente as duas opções globais solicitadas:
 * 1. Som Padrão de Alarmes
 * 2. Som Padrão de Notificações
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmniGlobalSoundSettingsSection(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val activePreviewId by TaskTonePreviewPlayer.activePreviewIdFlow.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            TaskTonePreviewPlayer.stopPreview(context)
        }
    }

    // 1. Seletores do Sistema e Áudio do Celular para Som Padrão de Alarmes
    val defaultAlarmRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val pickedUri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (pickedUri != null) {
                val title = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull() ?: "Toque de Alarme"
                customizationRepo.updateTaskAlarmSound(
                    uriString = pickedUri.toString(),
                    title = title,
                    vibrate = config.taskVibrateEnabled
                )
                onShowMessage("Som Padrão de Alarmes atualizado: $title")
            }
        }
    }

    val customAlarmAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveAndPersistCustomAudioUri(context, uri)
            customizationRepo.updateTaskAlarmSound(
                uriString = uri.toString(),
                title = "🎵 $fileName",
                vibrate = config.taskVibrateEnabled
            )
            TaskTonePreviewPlayer.playUriPreview(
                context = context,
                soundUriString = uri.toString(),
                previewKey = "GLOBAL_ALARM_PREVIEW",
                vibrate = config.taskVibrateEnabled
            )
            onShowMessage("Som Padrão de Alarmes definido: $fileName")
        }
    }

    // 2. Seletores do Sistema e Áudio do Celular para Som Padrão de Notificações
    val defaultNotificationRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val pickedUri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (pickedUri != null) {
                val title = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull() ?: "Som de Notificação"
                customizationRepo.updateBillReminderSound(
                    uriString = pickedUri.toString(),
                    title = title,
                    vibrate = config.billVibrateEnabled
                )
                AlarmScheduler.syncCustomNotificationChannel(context, pickedUri.toString(), title)
                onShowMessage("Som Padrão de Notificações atualizado: $title")
            }
        }
    }

    val customNotificationAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveAndPersistCustomAudioUri(context, uri)
            customizationRepo.updateBillReminderSound(
                uriString = uri.toString(),
                title = "🎵 $fileName",
                vibrate = config.billVibrateEnabled
            )
            AlarmScheduler.syncCustomNotificationChannel(context, uri.toString(), fileName)
            TaskTonePreviewPlayer.playUriPreview(
                context = context,
                soundUriString = uri.toString(),
                previewKey = "GLOBAL_NOTIF_PREVIEW",
                vibrate = config.billVibrateEnabled
            )
            onShowMessage("Som Padrão de Notificações definido: $fileName")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Defina os sons padrão do Omni em um só lugar. Na criação de tarefas, você também pode escolher um som individual específico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // OPÇÃO 1: Som Padrão de Alarmes
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏰ Som Padrão de Alarmes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Usado em tarefas com alarme e no cronômetro de glicemia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Atual: ${config.taskAlarmSoundTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vibrar",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = config.taskVibrateEnabled,
                            onCheckedChange = { vibrate ->
                                customizationRepo.updateTaskAlarmSound(
                                    uriString = config.taskAlarmSoundUri,
                                    title = config.taskAlarmSoundTitle,
                                    vibrate = vibrate
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TaskTonePreviewPlayer.stopPreview(context)
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Som Padrão de Alarmes")
                                val existingUri = config.taskAlarmSoundUri.takeIf { it.isNotBlank() && !it.startsWith("TONE:") }?.let { Uri.parse(it) }
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            defaultAlarmRingtoneLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_global_alarm_ringtone_btn")
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tons do Sistema", fontSize = 12.sp, maxLines = 1)
                    }

                    val isPlayingAlarmPreview = activePreviewId == "GLOBAL_ALARM_PREVIEW" ||
                        (config.taskAlarmSoundUri.startsWith("TONE:") && activePreviewId == config.taskAlarmSoundUri.removePrefix("TONE:"))
                    Button(
                        onClick = {
                            if (isPlayingAlarmPreview) {
                                TaskTonePreviewPlayer.stopPreview(context)
                            } else if (config.taskAlarmSoundUri.startsWith("TONE:")) {
                                TaskTonePreviewPlayer.playPreview(config.taskAlarmSoundUri.removePrefix("TONE:"))
                            } else {
                                TaskTonePreviewPlayer.playUriPreview(
                                    context = context,
                                    soundUriString = config.taskAlarmSoundUri,
                                    previewKey = "GLOBAL_ALARM_PREVIEW",
                                    vibrate = config.taskVibrateEnabled
                                )
                            }
                        },
                        colors = if (isPlayingAlarmPreview) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.testTag("test_global_alarm_sound_btn")
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlayingAlarmPreview) "⏹ Parar" else "Ouvir", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        TaskTonePreviewPlayer.stopPreview(context)
                        customAlarmAudioLauncher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_custom_alarm_audio_file_btn")
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escolher áudio do celular (MP3 / OGG / WAV)", fontSize = 12.sp)
                }

                // Toques Sintetizados Rápidos do Omni
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskRingtoneCatalog.allOptions.forEach { preset ->
                        val toneUri = "TONE:${preset.id}"
                        val isSelected = config.taskAlarmSoundUri == toneUri
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                customizationRepo.updateTaskAlarmSound(
                                    uriString = toneUri,
                                    title = "${preset.categoryLabel} • ${preset.title}",
                                    vibrate = config.taskVibrateEnabled
                                )
                                TaskTonePreviewPlayer.playPreview(preset.id)
                            },
                            label = {
                                Text(preset.categoryLabel, fontSize = 11.sp)
                            }
                        )
                    }
                }
            }
        }

        // OPÇÃO 2: Som Padrão de Notificações
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Som Padrão de Notificações",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Usado em lembretes gerais, contas fixas e avisos do app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Atual: ${config.billReminderSoundTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vibrar",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = config.billVibrateEnabled,
                            onCheckedChange = { vibrate ->
                                customizationRepo.updateBillReminderSound(
                                    uriString = config.billReminderSoundUri,
                                    title = config.billReminderSoundTitle,
                                    vibrate = vibrate
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TaskTonePreviewPlayer.stopPreview(context)
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Som Padrão de Notificações")
                                val existingUri = config.billReminderSoundUri.takeIf { it.isNotBlank() && !it.startsWith("TONE:") }?.let { Uri.parse(it) }
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            defaultNotificationRingtoneLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_global_notification_ringtone_btn")
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tons do Sistema", fontSize = 12.sp, maxLines = 1)
                    }

                    val isPlayingNotifPreview = activePreviewId == "GLOBAL_NOTIF_PREVIEW"
                    Button(
                        onClick = {
                            if (isPlayingNotifPreview) {
                                TaskTonePreviewPlayer.stopPreview(context)
                            } else {
                                TaskTonePreviewPlayer.playUriPreview(
                                    context = context,
                                    soundUriString = config.billReminderSoundUri,
                                    previewKey = "GLOBAL_NOTIF_PREVIEW",
                                    vibrate = config.billVibrateEnabled
                                )
                            }
                        },
                        colors = if (isPlayingNotifPreview) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.testTag("test_global_notification_sound_btn")
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlayingNotifPreview) "⏹ Parar" else "Ouvir", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        TaskTonePreviewPlayer.stopPreview(context)
                        customNotificationAudioLauncher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_custom_notification_audio_file_btn")
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escolher áudio do celular (MP3 / OGG / WAV)", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Componente Único Reutilizável de Seleção de Som Individual por Tarefa (em Criar/Editar Tarefa).
 * Mantém "Usar padrão do app" ("APP_DEFAULT") selecionado por padrão e usa preview controlado pelo TaskTonePreviewPlayer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmniTaskIndividualSoundSelector(
    selectedRingtoneCategory: String,
    customAudioFileName: String,
    alarmEnabled: Boolean,
    accentColor: Color,
    onAlarmEnabledChange: (Boolean) -> Unit,
    onSelectRingtoneCategory: (String) -> Unit,
    onCustomAudioPicked: (uriString: String, displayName: String) -> Unit
) {
    val context = LocalContext.current
    val activePreviewId by TaskTonePreviewPlayer.activePreviewIdFlow.collectAsStateWithLifecycle()
    val config = CustomizationRepository(context).configFlow.value

    val customTaskAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolvedName = resolveAndPersistCustomAudioUri(context, uri)
            onCustomAudioPicked(uri.toString(), resolvedName)
            TaskTonePreviewPlayer.playUriPreview(
                context = context,
                soundUriString = uri.toString(),
                previewKey = "TASK_CUSTOM_URI",
                vibrate = false
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Column {
                        Text(
                            text = "Alarme & Som desta Tarefa",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Padrão global: ${config.taskAlarmSoundTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = alarmEnabled,
                    onCheckedChange = onAlarmEnabledChange,
                    modifier = Modifier.testTag("task_alarm_switch")
                )
            }

            if (alarmEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                val isAppDefault = selectedRingtoneCategory.isBlank() || selectedRingtoneCategory == "APP_DEFAULT"
                val isCustomUri = selectedRingtoneCategory.startsWith("content://") || selectedRingtoneCategory.startsWith("file://")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Opção Padrão: "Usar padrão do app"
                    FilterChip(
                        selected = isAppDefault,
                        onClick = {
                            onSelectRingtoneCategory("APP_DEFAULT")
                            if (config.taskAlarmSoundUri.startsWith("TONE:")) {
                                TaskTonePreviewPlayer.playPreview(config.taskAlarmSoundUri.removePrefix("TONE:"))
                            } else {
                                TaskTonePreviewPlayer.playUriPreview(
                                    context = context,
                                    soundUriString = config.taskAlarmSoundUri,
                                    previewKey = "TASK_APP_DEFAULT_PREVIEW",
                                    vibrate = false
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "🔔 Usar padrão do app",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAppDefault) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.18f),
                            selectedLabelColor = accentColor
                        )
                    )

                    // 2. Opções de Toque Individual da Tarefa
                    TaskRingtoneCatalog.allOptions.forEach { ringtone ->
                        val isSelected = selectedRingtoneCategory == ringtone.id
                        val isPlayingThis = activePreviewId == ringtone.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectRingtoneCategory(ringtone.id)
                                TaskTonePreviewPlayer.playPreview(ringtone.id)
                            },
                            label = {
                                Text(
                                    text = if (isPlayingThis) "⏹ ${ringtone.categoryLabel}" else "🔊 ${ringtone.categoryLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.18f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }

                    // 3. Áudio Personalizado do Celular para esta Tarefa
                    FilterChip(
                        selected = isCustomUri,
                        onClick = {
                            TaskTonePreviewPlayer.stopPreview(context)
                            customTaskAudioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        label = {
                            Text(
                                text = if (isCustomUri) "🎵 ${customAudioFileName.ifBlank { "Áudio do celular" }}" else "🎵 Áudio do celular...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCustomUri) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.18f),
                            selectedLabelColor = accentColor
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSoundSummary = when {
                        isAppDefault -> "Usando padrão do app (${config.taskAlarmSoundTitle})"
                        isCustomUri -> "Áudio exclusivo: ${customAudioFileName.ifBlank { "Arquivo do celular" }}"
                        else -> {
                            val opt = TaskRingtoneCatalog.findById(selectedRingtoneCategory)
                            "${opt.categoryLabel} • ${opt.title}"
                        }
                    }
                    Text(
                        text = currentSoundSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val isAnyPreviewPlaying = activePreviewId != null
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isAnyPreviewPlaying) MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                                else accentColor.copy(alpha = 0.15f)
                            )
                            .clickable {
                                if (isAnyPreviewPlaying) {
                                    TaskTonePreviewPlayer.stopPreview(context)
                                } else if (isAppDefault) {
                                    if (config.taskAlarmSoundUri.startsWith("TONE:")) {
                                        TaskTonePreviewPlayer.playPreview(config.taskAlarmSoundUri.removePrefix("TONE:"))
                                    } else {
                                        TaskTonePreviewPlayer.playUriPreview(
                                            context = context,
                                            soundUriString = config.taskAlarmSoundUri,
                                            previewKey = "TASK_APP_DEFAULT_PREVIEW",
                                            vibrate = false
                                        )
                                    }
                                } else if (isCustomUri) {
                                    TaskTonePreviewPlayer.playUriPreview(
                                        context = context,
                                        soundUriString = selectedRingtoneCategory,
                                        previewKey = "TASK_CUSTOM_URI",
                                        vibrate = false
                                    )
                                } else {
                                    TaskTonePreviewPlayer.playPreview(selectedRingtoneCategory)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("preview_task_tone_btn")
                    ) {
                        Text(
                            text = if (isAnyPreviewPlaying) "⏹ Parar" else "🔊 Ouvir",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnyPreviewPlaying) MaterialTheme.colorScheme.error else accentColor
                        )
                    }
                }
            }
        }
    }
}
