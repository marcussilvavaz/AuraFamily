package com.example.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppCustomizationConfig
import com.example.data.AppUpdateManager
import com.example.data.AppUpdateUiState
import com.example.data.CustomizationCatalog
import com.example.data.CustomizationRepository
import com.example.data.Household
import com.example.data.UserProfile
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import kotlinx.coroutines.launch
import java.io.File

/**
 * Reusable Profile Avatar Badge that displays the user's selected gallery photo
 * (or initials) surrounded by a ring in their chosen Family Agenda profile color.
 */
@Composable
fun ProfileAvatarBadge(
    profileName: String,
    profileColorHex: Long,
    profilePhotoPath: String,
    profilePhotoVersion: Long,
    profileAvatarBase64: String = "",
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "profile_avatar_badge"
) {
    val ringColor = remember(profileColorHex) {
        if (profileColorHex != 0L) Color(profileColorHex.toInt()) else Color(0xFF0B6E4F)
    }

    val imageBitmap = remember(profileAvatarBase64, profilePhotoPath, profilePhotoVersion) {
        if (profileAvatarBase64.isNotBlank()) {
            runCatching {
                val bytes = android.util.Base64.decode(profileAvatarBase64, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else if (profilePhotoPath.isNotBlank()) {
            runCatching {
                val file = File(profilePhotoPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        } else {
            null
        }
    }

    val initials = remember(profileName) {
        val clean = profileName.trim()
        val parts = clean.split(" ").filter { it.isNotBlank() }
        when {
            clean.equals("Você", ignoreCase = true) || clean.equals("VO", ignoreCase = true) -> "OM"
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "OM"
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ringColor.copy(alpha = 0.18f))
            .border(2.5.dp, ringColor, CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto do perfil de $profileName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.labelLarge
                },
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }

        // Small color indicator dot on bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(if (size >= 64.dp) 18.dp else 13.dp)
                .clip(CircleShape)
                .background(ringColor)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilySettingsScreen(
    userProfile: UserProfile?,
    household: Household,
    customizationConfig: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    userEmail: String?,
    onUpdateProfileIdentity: (displayName: String, memberColorHex: Long) -> Unit,
    onJoinHousehold: (String, String) -> Unit,
    onToggleInviteOpen: (Boolean) -> Unit = {},
    onCreateNewFamily: (familyName: String, customInviteCode: String) -> Unit = { _, _ -> },
    onSaveFamilyMember: (com.example.data.FamilyMemberBadge) -> Unit = {},
    onRemoveFamilyMember: (memberUserId: String, memberName: String) -> Unit = { _, _ -> },
    onChangeThemeMode: (String) -> Unit,
    onOpenCustomizationStudio: () -> Unit,
    onOpenModularOnboardingScreen: () -> Unit = {},
    achievementsSummary: com.example.data.AchievementsEvaluationSummary? = null,
    onOpenAchievementsScreen: () -> Unit = {},
    updateState: AppUpdateUiState? = null,
    updateManager: AppUpdateManager? = null,
    onRecoverLegacyData: () -> Unit = {},
    onBuildFullBackupJson: () -> String = { "{}" },
    onRestoreBackupFromUri: (android.net.Uri) -> Unit = {},
    syncedItemsTotalCount: Int = 0,
    onForceCloudSyncNow: () -> Unit = {},
    onUpdateAvatarBase64: (String) -> Unit = {},
    onBackToHub: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val cardRadius = LocalOrganicCardRadius.current
    val clipboardManager = LocalClipboardManager.current
    val triggerFeedback = rememberOmniFeedback()

    var nameInput by remember(customizationConfig.profileName, userProfile?.displayName, userEmail) {
        mutableStateOf(
            customizationConfig.profileName.takeIf { it.isNotBlank() && !it.equals("Você", ignoreCase = true) }
                ?: userProfile?.displayName?.takeIf { it.isNotBlank() && !it.equals("Você", ignoreCase = true) }
                ?: userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Membro"
        )
    }
    val selectedColorHex = customizationConfig.profileColorHex
    val effectiveAvatarBase64 = customizationConfig.profileAvatarBase64.ifBlank {
        userProfile?.avatarBase64.orEmpty()
    }

    // Expandable section states
    var expandStructuredAppearance by remember { mutableStateOf(false) }
    var expandProfileIdentity by remember { mutableStateOf(false) }
    var expandModularProfile by remember { mutableStateOf(false) }
    var expandHubBuilder by remember { mutableStateOf(false) }
    var expandSoundsSettings by remember { mutableStateOf(false) }
    var expandFamilySync by remember { mutableStateOf(true) }
    var expandBackupAndMural by remember { mutableStateOf(true) }

    // Lista & Mural da Família state (sincronizado com o 4º Widget da tela inicial)
    var familyMuralItems by remember {
        mutableStateOf(com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context))
    }
    var newMuralItemText by remember { mutableStateOf("") }
    var newMuralCategory by remember { mutableStateOf("Compras") }

    // 1-Tap Backup Export (.json) & Restore (.json)
    val createBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { targetUri ->
        if (targetUri != null) {
            val jsonPayload = onBuildFullBackupJson()
            coroutineScope.launch {
                val res = com.example.data.OmniBackupAndMigrationManager.writeBackupToUri(
                    context = context,
                    targetUri = targetUri,
                    jsonContent = jsonPayload
                )
                if (res.isSuccess) {
                    triggerFeedback("Backup exportado com sucesso!")
                    onShowMessage("Arquivo de backup (.json) salvo com sucesso!")
                } else {
                    onShowMessage("Erro ao exportar backup: ${res.exceptionOrNull()?.localizedMessage ?: "falha"}")
                }
            }
        }
    }

    val openBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { sourceUri ->
        if (sourceUri != null) {
            triggerFeedback("Restaurando e mesclando backup...")
            onRestoreBackupFromUri(sourceUri)
        }
    }

    var targetCodeInput by remember { mutableStateOf("") }
    var targetFamilyNameInput by remember(household.name) { mutableStateOf(household.name) }
    var copiedFeedback by remember { mutableStateOf(false) }

    // Create Family & Member Edit/Delete dialogs
    var showCreateFamilyDialog by remember { mutableStateOf(false) }
    var editingFamilyMember by remember { mutableStateOf<com.example.data.FamilyMemberBadge?>(null) }
    var showMemberEditorDialog by remember { mutableStateOf(false) }
    var deletingFamilyMember by remember { mutableStateOf<com.example.data.FamilyMemberBadge?>(null) }

    val activeTheme = customizationConfig.themeMode.takeIf { it.isNotBlank() && it != "SYSTEM" }
        ?: userProfile?.themeMode
        ?: "SYSTEM"
    val selectedMemberColor = remember(selectedColorHex) {
        Color(selectedColorHex.toInt())
    }
    val selectedColorName = remember(selectedColorHex) {
        CustomizationCatalog.memberAgendaColors.firstOrNull { it.second == selectedColorHex }?.first
            ?: "Cor Personalizada"
    }

    // Zero-permission Android Photo Picker for Profile Picture
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = customizationRepo.saveProfilePhotoFromUri(uri)
            if (saved) {
                val base64 = customizationRepo.configFlow.value.profileAvatarBase64
                if (base64.isNotBlank()) {
                    onUpdateAvatarBase64(base64)
                }
                onShowMessage("Foto do perfil sincronizada com a família na nuvem!")
            } else {
                onShowMessage("Não foi possível carregar a foto selecionada.")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("family_settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Back to Hub & Profile Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBackToHub,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("profile_back_to_hub_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar ao Hub"
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Meu Perfil & Configurações",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Foto, cor na agenda familiar, widgets do Hub e temas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ProfileAvatarBadge(
                            profileName = nameInput,
                            profileColorHex = selectedColorHex,
                            profilePhotoPath = customizationConfig.profilePhotoPath,
                            profilePhotoVersion = customizationConfig.profilePhotoVersion,
                            profileAvatarBase64 = effectiveAvatarBase64,
                            size = 54.dp,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            testTag = "profile_header_avatar"
                        )
                    }

                    // Live preview of how user's tasks appear in the Family Agenda
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = selectedMemberColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, selectedMemberColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(selectedMemberColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Prévia na Agenda Familiar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Suas tarefas ficam na cor $selectedColorName • $nameInput",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedMemberColor
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = selectedMemberColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Compact Trophy & Medals Summary Bar in Profile Header
                    if (achievementsSummary != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenAchievementsScreen() }
                                .testTag("profile_open_achievements_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🏆 Conquistas & Medalhas • Nível ${achievementsSummary.currentLevel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${achievementsSummary.unlockedCount}/${achievementsSummary.totalCount} medalhas (🩺 Saúde • 💰 Finanças • 📅 Agenda) • ${achievementsSummary.totalXp} XP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onOpenAchievementsScreen,
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("profile_open_achievements_btn")
                                ) {
                                    Text(
                                        text = "Ver Medalhas",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Quick Google Account Sign-In / Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userEmail.isNullOrBlank()) {
                            Text(
                                text = "Modo Local (Login adiado)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("profile_header_google_login_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Entrar com Google",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Google: $userEmail",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("profile_header_google_logout_btn")
                            ) {
                                Text(
                                    text = "Trocar / Sair",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STATUS DA NUVEM (FIRESTORE) & GARANTIR NOTIFICAÇÕES EM 2º PLANO (ONE UI)
        // =========================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_status_firestore_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Status da Nuvem (Firestore)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!userEmail.isNullOrBlank()) {
                                        "Sincronização em Tempo Real Ativa • $userEmail"
                                    } else {
                                        "Modo Offline Local • Entre com Google para sincronizar"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Família: ${household.name} (${household.householdId})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$syncedItemsTotalCount itens sincronizados (Agenda, Finanças, Saúde, Diário e Mural)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerFeedback("Sincronizando todos os dados com o Cloud Firestore...")
                                onForceCloudSyncNow()
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("force_cloud_sync_now_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Forçar Sincronização Agora",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botão Garantir Notificações em 2º Plano (Samsung One UI / Android 12+)
                    FilledTonalButton(
                        onClick = {
                            var openedSettings = false
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
                                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                                    runCatching {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        openedSettings = true
                                    }
                                }
                            }
                            if (!openedSettings && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                                if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    runCatching {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        openedSettings = true
                                    }
                                }
                            }
                            com.example.alarm.AlarmScheduler.rescheduleAllActiveAlarms(context)
                            if (openedSettings) {
                                onShowMessage("Autorize alarmes exatos / bateria sem restrições para notificações em 2º plano.")
                            } else {
                                triggerFeedback("Notificações e alarmes exatos verificados e reagendados!")
                                onShowMessage("✅ Alarmes exatos e bateria sem restrição ativos! Todos os lembretes foram reagendados.")
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ensure_background_notifications_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Garantir Notificações em 2º Plano",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Discreet Permissions Notice Banner ("Ativar agora" opens Android Settings directly)
        item {
            OmniPermissionsNoticeCard(compact = false)
        }

        // =========================================================================
        // CENTRAL DE ATUALIZAÇÕES EM TEMPO REAL (AUTO-UPDATE CHECKER)
        // =========================================================================
        if (updateState != null && updateManager != null) {
            item {
                AppUpdateControlCenterCard(
                    updateState = updateState,
                    updateManager = updateManager,
                    onShowMessage = onShowMessage
                )
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION 1: APARÊNCIA UNIFICADA, CORES POR MÓDULO, TEMAS & LAYOUT
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "1. Aparência, Cores dos Módulos & Temas",
                subtitle = "Cores dos 5 módulos, sugestões de tema, estilo orgânico e layout (${customizationConfig.agendaLayoutMode.lowercase().replaceFirstChar { it.uppercase() }})",
                icon = Icons.Default.Palette,
                accentColor = selectedMemberColor,
                expanded = expandStructuredAppearance,
                onToggleExpand = { expandStructuredAppearance = !expandStructuredAppearance },
                testTag = "section_structured_appearance"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StructuredAppearanceCard(
                        config = customizationConfig,
                        customizationRepo = customizationRepo,
                        onOpenFullPalette = onOpenCustomizationStudio
                    )

                    // Modo Base Claro / Escuro / Preto Puro (AMOLED) + Cor de Destaque + Fontes e Sons
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = selectedMemberColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Modo Base (Claro / Escuro / AMOLED):",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "SYSTEM" to "Automático",
                                    "LIGHT" to "Claro",
                                    "DARK" to "Escuro",
                                    "AMOLED" to "Preto Puro (AMOLED)"
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = activeTheme == mode,
                                        onClick = {
                                            onChangeThemeMode(mode)
                                            triggerFeedback("Modo $label ativado!")
                                        },
                                        label = { Text(label, maxLines = 1, softWrap = false) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("theme_chip_$mode")
                                    )
                                }
                            }

                            Text(
                                text = "Cor de Destaque Principal (Botões e Ícones Ativos):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CustomizationCatalog.memberAgendaColors.take(6).forEach { (colorName, hex) ->
                                    val c = Color(hex.toInt())
                                    val isSelected = customizationConfig.primaryHex == hex
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            customizationRepo.updateStructuredAppColor(hex)
                                            triggerFeedback("Cor principal '$colorName' aplicada!")
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = colorName,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        shape = RoundedCornerShape(50)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onOpenCustomizationStudio,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("family_open_studio_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Personalizar Fontes & Toques de Alarme",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION 2: FOTO & NOME DO PERFIL
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "2. Foto & Nome do Perfil",
                subtitle = "Escolha sua foto de perfil e como seu nome aparece para a família",
                icon = Icons.Default.Person,
                accentColor = selectedMemberColor,
                expanded = expandProfileIdentity,
                onToggleExpand = { expandProfileIdentity = !expandProfileIdentity },
                testTag = "section_profile_identity"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Photo Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileAvatarBadge(
                            profileName = nameInput,
                            profileColorHex = selectedColorHex,
                            profilePhotoPath = customizationConfig.profilePhotoPath,
                            profilePhotoVersion = customizationConfig.profilePhotoVersion,
                            profileAvatarBase64 = effectiveAvatarBase64,
                            size = 72.dp,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            testTag = "profile_large_avatar_picker"
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_profile_photo_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escolher Foto do Perfil")
                            }

                            if (customizationConfig.profilePhotoPath.isNotBlank() || effectiveAvatarBase64.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        customizationRepo.clearProfilePhoto()
                                        onUpdateAvatarBase64("")
                                        onShowMessage("Foto removida. Usando iniciais do perfil.")
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("clear_profile_photo_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remover Foto")
                                }
                            }
                        }
                    }

                    // Profile Display Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Seu Nome na Agenda Familiar") },
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_name_input")
                        )
                        FilledTonalButton(
                            onClick = {
                                val clean = nameInput.trim().takeIf {
                                    it.isNotBlank() && !it.equals("Você", ignoreCase = true)
                                } ?: userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "Membro"
                                nameInput = clean
                                customizationRepo.updateProfileIdentity(clean, selectedColorHex)
                                onUpdateProfileIdentity(clean, selectedColorHex)
                                triggerFeedback("Nome do perfil salvo com sucesso!")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("save_profile_name_btn")
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION: MEU PERFIL E MÓDULOS (PASSO 1 - ROTINA, ENERGIA, SAÚDE E DIÁRIO)
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "2. Meu Perfil e Módulos (Rotina & Saúde)",
                subtitle = "Ritmo Fixo/Elástico, Mapa de Energia, Gestação, Glicemia, Pressão, Fadiga e Diário",
                icon = Icons.Default.Person,
                accentColor = MaterialTheme.colorScheme.primary,
                expanded = expandModularProfile,
                onToggleExpand = { expandModularProfile = !expandModularProfile },
                testTag = "section_modular_profile_passo1"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onOpenModularOnboardingScreen,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_full_modular_profile_screen_btn")
                    ) {
                        Text("Abrir Tela Completa de Onboarding (Meu Perfil e Módulos)")
                    }

                    ModularProfileEditorContent(
                        config = customizationConfig,
                        customizationRepo = customizationRepo
                    )
                }
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION 3: MONTAR MEU HUB
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "3. Montar Meu Hub",
                subtitle = "Escolha quais blocos compactos aparecem no seu Hub e a ordem deles",
                icon = Icons.Default.Watch,
                accentColor = MaterialTheme.colorScheme.primary,
                expanded = expandHubBuilder,
                onToggleExpand = { expandHubBuilder = !expandHubBuilder },
                testTag = "section_hub_builder"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ative apenas as informações que você quer ver no Hub da Família e use as setas para mudar a ordem:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val orderedIds = customizationConfig.hubWidgetOrder
                    orderedIds.forEachIndexed { index, widgetId ->
                        val spec = CustomizationCatalog.hubWidgets.firstOrNull { it.id == widgetId }
                        if (spec != null) {
                            val isEnabled = widgetId in customizationConfig.enabledHubWidgets
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hub_builder_row_$widgetId")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${spec.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = spec.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { customizationRepo.moveHubWidget(widgetId, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("move_up_$widgetId")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Subir widget",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { customizationRepo.moveHubWidget(widgetId, 1) },
                                        enabled = index < orderedIds.lastIndex,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("move_down_$widgetId")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Descer widget",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            customizationRepo.toggleHubWidget(widgetId, checked)
                                        },
                                        modifier = Modifier.testTag("toggle_widget_$widgetId")
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            customizationRepo.resetHubWidgetsToDefault()
                            onShowMessage("Blocos do Hub restaurados para o padrão!")
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_hub_widgets_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar Padrão do Hub")
                    }
                }
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION: CONFIGURAÇÕES -> SONS, NOTIFICAÇÕES & RESUMO MATINAL
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "4. Sons, Notificações & Resumo Matinal",
                subtitle = "Resumo Matinal (${String.format(java.util.Locale.getDefault(), "%02d:%02d", customizationConfig.morningSummaryHour, customizationConfig.morningSummaryMinute)}) • Alarmes (${customizationConfig.taskAlarmSoundTitle})",
                icon = Icons.Default.Notifications,
                accentColor = MaterialTheme.colorScheme.primary,
                expanded = expandSoundsSettings,
                onToggleExpand = { expandSoundsSettings = !expandSoundsSettings },
                testTag = "section_sounds_and_notifications"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Resumo Matinal Automático
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morning_summary_config_card")
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
                                        text = "☀️ Resumo Matinal Automático",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Notificação diária com compromissos do dia, contas/faturas vencendo hoje e itens pendentes da Lista de Compras.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = customizationConfig.morningSummaryEnabled,
                                    onCheckedChange = { enabled ->
                                        customizationRepo.updateMorningSummaryConfig(
                                            enabled = enabled,
                                            hour = customizationConfig.morningSummaryHour,
                                            minute = customizationConfig.morningSummaryMinute
                                        )
                                        triggerFeedback(
                                            if (enabled) "Resumo Matinal ativado!" else "Resumo Matinal desativado."
                                        )
                                    },
                                    modifier = Modifier.testTag("morning_summary_toggle")
                                )
                            }

                            if (customizationConfig.morningSummaryEnabled) {
                                Text(
                                    text = "Horário do Resumo Matinal:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        7 to 0,
                                        7 to 30,
                                        8 to 0,
                                        8 to 30,
                                        9 to 0
                                    ).forEach { (h, m) ->
                                        val timeLabel = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
                                        val isSelected = customizationConfig.morningSummaryHour == h &&
                                            customizationConfig.morningSummaryMinute == m
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                customizationRepo.updateMorningSummaryConfig(
                                                    enabled = true,
                                                    hour = h,
                                                    minute = m
                                                )
                                                triggerFeedback("Resumo Matinal agendado para $timeLabel!")
                                            },
                                            label = {
                                                Text(
                                                    text = timeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OmniGlobalSoundSettingsSection(
                        config = customizationConfig,
                        customizationRepo = customizationRepo,
                        onShowMessage = onShowMessage
                    )
                }
            }
        }

        // =========================================================================
        // EXPANDABLE SECTION 5: GESTÃO DE FAMÍLIA, MEMBROS & CÓDIGO DE CONVITE
        // =========================================================================
        item {
            val effectiveMembers = remember(household.members, nameInput, selectedColorHex, userProfile, effectiveAvatarBase64) {
                val realMembers = household.membersWithAssignedColors()
                if (realMembers.isNotEmpty()) {
                    realMembers
                } else {
                    listOf(
                        com.example.data.FamilyMemberBadge(
                            userId = userProfile?.uid?.ifBlank { "local_user" } ?: "local_user",
                            displayName = nameInput.ifBlank { "Membro" },
                            colorHex = selectedColorHex,
                            avatarEmoji = userProfile?.avatarEmoji ?: "🧑",
                            role = "Admin",
                            email = userEmail.orEmpty(),
                            avatarBase64 = effectiveAvatarBase64
                        )
                    )
                }
            }

            ExpandableProfileSectionCard(
                title = "4. Gestão de Família & Membros (${household.name})",
                subtitle = "${effectiveMembers.size} integrante(s) • Código: ${household.householdId}",
                icon = Icons.Default.FamilyRestroom,
                accentColor = MaterialTheme.colorScheme.tertiary,
                expanded = expandFamilySync,
                onToggleExpand = { expandFamilySync = !expandFamilySync },
                testTag = "section_family_sync"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Current Family Card + Invite Code + Create New Family button
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "FAMÍLIA ATUAL: ${household.name.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Código: ${household.householdId}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(household.householdId))
                                        copiedFeedback = true
                                        triggerFeedback("Código ${household.householdId} copiado!")
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("copy_household_code_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copiedFeedback) "Copiado!" else "Copiar")
                                }
                            }

                            // Trava de Convite (inviteOpen) para entrada por código
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Aceitar novos membros (Convite aberto)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (household.inviteOpen) {
                                                "Aberto: familiares com o código ${household.householdId} podem entrar"
                                            } else {
                                                "Fechado: ninguém de fora pode entrar usando o código"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (household.inviteOpen) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = household.inviteOpen,
                                        onCheckedChange = { checked ->
                                            onToggleInviteOpen(checked)
                                        },
                                        modifier = Modifier.testTag("toggle_household_invite_open_switch")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showCreateFamilyDialog = true },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_create_family_dialog_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FamilyRestroom,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Criar Nova Família")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        editingFamilyMember = null
                                        showMemberEditorDialog = true
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_family_member_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GroupAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Membro")
                                }
                            }
                        }
                    }

                    // QUEM FAZ PARTE DA FAMÍLIA (Members list)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("family_members_list_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "👨‍👩‍👧‍👦 Quem faz parte da família",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${effectiveMembers.size} integrante(s) vinculado(s) a ${household.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            effectiveMembers.forEach { member ->
                                val memberColor = if (member.colorHex != 0L) {
                                    Color(member.colorHex.toInt())
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = memberColor.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, memberColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .testTag("family_member_row_${member.userId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val memberBitmap = remember(member.avatarBase64) {
                                                if (member.avatarBase64.isNotBlank()) {
                                                    runCatching {
                                                        val bytes = android.util.Base64.decode(member.avatarBase64, android.util.Base64.DEFAULT)
                                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                                    }.getOrNull()
                                                } else null
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(memberColor.copy(alpha = 0.2f))
                                                    .border(2.dp, memberColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (memberBitmap != null) {
                                                    Image(
                                                        bitmap = memberBitmap,
                                                        contentDescription = member.displayName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(2.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Text(
                                                        text = member.avatarEmoji.ifBlank { "🧑" },
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = member.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${member.role.ifBlank { "Membro" }} • Cor na Agenda Familiar",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = memberColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingFamilyMember = member
                                                    showMemberEditorDialog = true
                                                },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("edit_family_member_${member.userId}")
                                            ) {
                                                Text("Editar", style = MaterialTheme.typography.labelSmall)
                                            }
                                            val isAdminMember = member.role.equals("ADMIN", ignoreCase = true) ||
                                                member.userId == userProfile?.userId ||
                                                member.userId == household.ownerId ||
                                                member.isCurrentUser
                                            if (effectiveMembers.size > 1 && !isAdminMember) {
                                                IconButton(
                                                    onClick = { deletingFamilyMember = member },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("delete_family_member_${member.userId}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remover membro",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entrar em Outra Família por Código de Convite",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = targetCodeInput,
                        onValueChange = { targetCodeInput = it.uppercase() },
                        label = { Text("Código Familiar (Ex: FAM_SILVA01)") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_household_code_input")
                    )

                    OutlinedTextField(
                        value = targetFamilyNameInput,
                        onValueChange = { targetFamilyNameInput = it },
                        label = { Text("Nome da Família") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (targetCodeInput.isNotBlank()) {
                                onJoinHousehold(targetCodeInput, targetFamilyNameInput)
                                targetCodeInput = ""
                            }
                        },
                        enabled = targetCodeInput.isNotBlank(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_household_submit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conectar ao Espaço Familiar")
                    }

                    val isLocalMode = userEmail.isNullOrBlank()
                    if (isLocalMode) {
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("optional_login_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectar Conta Google (Opcional)")
                        }
                    } else {
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voltar para Modo sem Login")
                        }
                    }
                }
            }
        }

        // 7. BACKUP EM 1 TOQUE, RECUPERAÇÃO DE DADOS ANTIGOS & LISTA/MURAL DA FAMÍLIA (Itens 2, 8d e 9)
        item {
            ExpandableProfileSectionCard(
                title = "Backup, Recuperação & Lista da Família",
                subtitle = "Recuperar dados antigos • Exportar/Restaurar (.json) • Lista de Compras",
                icon = Icons.Default.CloudDone,
                accentColor = Color(0xFF0E7C5B),
                expanded = expandBackupAndMural,
                onToggleExpand = { expandBackupAndMural = !expandBackupAndMural },
                testTag = "profile_section_backup_and_mural"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Proteja ou recupere todos os seus registros (Tarefas, Finanças, Contas, Monitoramento de Glicemia/Saúde e Diário Privado) sem duplicar itens:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Botão Recuperar Dados Antigos (Item 2)
                    Button(
                        onClick = {
                            triggerFeedback("Varrendo e mesclando dados antigos...")
                            onRecoverLegacyData()
                            familyMuralItems = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recover_legacy_data_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recuperar Dados Antigos", fontWeight = FontWeight.Bold)
                    }

                    // Botões Exportar Backup & Restaurar Backup (Item 9)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val todayStr = java.time.LocalDate.now().toString()
                                createBackupDocumentLauncher.launch("omni_backup_$todayStr.json")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_json_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar Backup", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                openBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_backup_json_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Backup", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Lista de Compras & Mural da Família (sincronizado com o widget "Omni • Lista & Mural da Família")
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛒 Lista de Compras & Mural da Família",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Itens compartilhados sincronizados diretamente com o widget 'Omni • Lista & Mural da Família' na tela inicial do celular.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Compras", "Mural", "Casa").forEach { cat ->
                                    FilterChip(
                                        selected = newMuralCategory == cat,
                                        onClick = { newMuralCategory = cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newMuralItemText,
                                    onValueChange = { newMuralItemText = it },
                                    label = { Text("Novo item da lista / recado") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("family_mural_new_item_input")
                                )
                                Button(
                                    onClick = {
                                        val clean = newMuralItemText.trim()
                                        if (clean.isNotEmpty()) {
                                            com.example.data.OmniBackupAndMigrationManager.addFamilyMuralItem(
                                                context = context,
                                                title = clean,
                                                category = newMuralCategory,
                                                addedBy = nameInput
                                            )
                                            newMuralItemText = ""
                                            familyMuralItems = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
                                            triggerFeedback("Item adicionado à Lista da Família!")
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("family_mural_add_btn")
                                ) {
                                    Text("+")
                                }
                            }

                            familyMuralItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                com.example.data.OmniBackupAndMigrationManager.toggleFamilyMuralItem(context, item.id)
                                                familyMuralItems = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (item.completed) Icons.Default.CheckCircle else Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (item.completed) Color(0xFF00B894) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (item.completed) FontWeight.Normal else FontWeight.SemiBold,
                                                    color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${item.category} • ${item.addedBy}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                com.example.data.OmniBackupAndMigrationManager.deleteFamilyMuralItem(context, item.id)
                                                familyMuralItems = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remover item",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Family Dialog
    if (showCreateFamilyDialog) {
        var newFamilyName by remember { mutableStateOf("Família ${nameInput.take(15)}") }
        var customInviteCode by remember {
            val suffix = (1000..9999).random()
            mutableStateOf("FAM_OMNI$suffix")
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateFamilyDialog = false },
            title = { Text("Criar Nova Família", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Defina o nome da sua família e o código de convite para compartilhar com os integrantes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newFamilyName,
                        onValueChange = { newFamilyName = it },
                        label = { Text("Nome da Família") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_family_name_input")
                    )
                    OutlinedTextField(
                        value = customInviteCode,
                        onValueChange = { customInviteCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                        label = { Text("Código de Convite (Identificador)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_family_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = newFamilyName.trim().ifBlank { "Família Omni" }
                        val cleanCode = customInviteCode.trim().ifBlank { "FAM_OMNI01" }
                        onCreateNewFamily(cleanName, cleanCode)
                        triggerFeedback("Família '$cleanName' criada com sucesso!")
                        showCreateFamilyDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_family_btn")
                ) {
                    Text("Criar Família")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateFamilyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add / Edit Family Member Dialog
    if (showMemberEditorDialog) {
        val initial = editingFamilyMember
        var memberName by remember(initial) { mutableStateOf(initial?.displayName ?: "") }
        var memberRole by remember(initial) { mutableStateOf(initial?.role ?: "Membro") }
        var memberEmoji by remember(initial) { mutableStateOf(initial?.avatarEmoji ?: "🧑") }
        var memberColorHex by remember(initial) {
            mutableStateOf(
                initial?.colorHex ?: CustomizationCatalog.memberAgendaColors.first().second
            )
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showMemberEditorDialog = false
                editingFamilyMember = null
            },
            title = {
                Text(
                    text = if (initial == null) "Adicionar Membro da Família" else "Editar Membro da Família",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Nome do Integrante") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("family_member_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Membro", "Admin", "Cônjuge", "Filho(a)").forEach { roleOpt ->
                            FilterChip(
                                selected = memberRole == roleOpt,
                                onClick = { memberRole = roleOpt },
                                label = { Text(roleOpt, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Text(
                        text = "Avatar / Emoji:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("🧑", "👩", "👨", "🧒", "👧", "👦", "👵", "👴", "🤰").forEach { emoji ->
                            FilterChip(
                                selected = memberEmoji == emoji,
                                onClick = { memberEmoji = emoji },
                                label = { Text(emoji) }
                            )
                        }
                    }
                    Text(
                        text = "Cor do Integrante na Agenda:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomizationCatalog.memberAgendaColors.take(8).forEach { (colorName, hex) ->
                            val c = Color(hex.toInt())
                            FilterChip(
                                selected = memberColorHex == hex,
                                onClick = { memberColorHex = hex },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                    )
                                },
                                label = { Text(colorName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = memberName.trim()
                        if (cleanName.isNotBlank()) {
                            val badge = com.example.data.FamilyMemberBadge(
                                userId = initial?.userId ?: "mem_${java.util.UUID.randomUUID().toString().take(8)}",
                                displayName = cleanName,
                                colorHex = memberColorHex,
                                avatarEmoji = memberEmoji,
                                role = memberRole
                            )
                            onSaveFamilyMember(badge)
                            triggerFeedback(
                                if (initial == null) "$cleanName adicionado(a) à família!" else "$cleanName atualizado(a)!"
                            )
                            showMemberEditorDialog = false
                            editingFamilyMember = null
                        }
                    },
                    modifier = Modifier.testTag("save_family_member_btn")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showMemberEditorDialog = false
                        editingFamilyMember = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Family Member Confirmation Dialog
    val memberToDelete = deletingFamilyMember
    if (memberToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Remover Membro da Família",
            message = "Deseja remover '${memberToDelete.displayName}' da lista de integrantes da família?",
            onConfirm = {
                onRemoveFamilyMember(memberToDelete.userId, memberToDelete.displayName)
                triggerFeedback("${memberToDelete.displayName} removido(a) da família")
                deletingFamilyMember = null
            },
            onDismiss = { deletingFamilyMember = null }
        )
    }
}

@Composable
private fun ExpandableProfileSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicStyle = com.example.ui.theme.LocalOrganicVisualStyle.current
    val organicShape = RoundedCornerShape(cardRadius)
    val borderWidth = when (organicStyle) {
        com.example.data.OrganicVisualStyle.TRANSLUCIDO_GLASS -> 1.dp
        com.example.data.OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 1.dp
        else -> 1.dp
    }
    val borderColor = when (organicStyle) {
        com.example.data.OrganicVisualStyle.TRANSLUCIDO_GLASS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        com.example.data.OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> MaterialTheme.colorScheme.outlineVariant
        else -> accentColor.copy(alpha = if (expanded) 0.32f else 0.14f)
    }
    val elevationDp = com.example.ui.theme.omniOrganicCardElevationDp(if (expanded) 4.dp else 1.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = borderColor,
                shape = organicShape
            )
            .testTag(testTag),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationDp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Recolher" else "Expandir",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Top
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(250)
                ),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    shrinkTowards = Alignment.Top
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(190)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                ) {
                    content()
                }
            }
        }
    }
}
