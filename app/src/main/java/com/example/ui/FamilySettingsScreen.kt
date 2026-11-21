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
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "profile_avatar_badge"
) {
    val ringColor = remember(profileColorHex) {
        if (profileColorHex != 0L) Color(profileColorHex.toInt()) else Color(0xFF0B6E4F)
    }

    val imageBitmap = remember(profilePhotoPath, profilePhotoVersion) {
        if (profilePhotoPath.isNotBlank()) {
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
        val parts = profileName.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "VC"
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
    onChangeThemeMode: (String) -> Unit,
    onOpenCustomizationStudio: () -> Unit,
    onOpenModularOnboardingScreen: () -> Unit = {},
    updateState: AppUpdateUiState? = null,
    updateManager: AppUpdateManager? = null,
    onBackToHub: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val clipboardManager = LocalClipboardManager.current

    var nameInput by remember(customizationConfig.profileName, userProfile?.displayName) {
        mutableStateOf(
            customizationConfig.profileName.ifBlank {
                userProfile?.displayName?.ifBlank { "Você" } ?: "Você"
            }
        )
    }
    val selectedColorHex = customizationConfig.profileColorHex

    // Expandable section states (all start collapsed by default so the screen stays compact and clean)
    var expandStructuredAppearance by remember { mutableStateOf(false) }
    var expandProfileIdentity by remember { mutableStateOf(false) }
    var expandModularProfile by remember { mutableStateOf(false) }
    var expandHubBuilder by remember { mutableStateOf(false) }
    var expandFamilySync by remember { mutableStateOf(false) }

    var targetCodeInput by remember { mutableStateOf("") }
    var targetFamilyNameInput by remember { mutableStateOf(household.name) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val activeTheme = userProfile?.themeMode ?: "SYSTEM"
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
                onShowMessage("Foto do perfil atualizada com sucesso!")
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
                }
            }
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

                    // Modo Base Claro / Escuro / Automático + Acesso às Fontes e Sons
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
                                    text = "Modo Base (Claro / Escuro):",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "SYSTEM" to "Automático",
                                    "LIGHT" to "Claro",
                                    "DARK" to "Escuro"
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = activeTheme == mode,
                                        onClick = { onChangeThemeMode(mode) },
                                        label = { Text(label) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("theme_chip_$mode")
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
                                Text("Personalizar 10 Fontes & Toques de Alarme")
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

                            if (customizationConfig.profilePhotoPath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        customizationRepo.clearProfilePhoto()
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
                                val clean = nameInput.trim().ifBlank { "Você" }
                                customizationRepo.updateProfileIdentity(clean, selectedColorHex)
                                onUpdateProfileIdentity(clean, selectedColorHex)
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
        // EXPANDABLE SECTION 3: MONTAR MEU HUB (WIDGETS ESTILO SMARTWATCH)
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "3. Montar Meu Hub (Widgets Smartwatch)",
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
                            onShowMessage("Widgets do Hub restaurados para o padrão smartwatch!")
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
        // EXPANDABLE SECTION 4: CÓDIGO DA FAMÍLIA, SINCRONIZAÇÃO & CONTA
        // =========================================================================
        item {
            ExpandableProfileSectionCard(
                title = "4. Código da Família & Conta (${household.householdId})",
                subtitle = "Compartilhe o orçamento familiar ou conecte outra conta",
                icon = Icons.Default.FamilyRestroom,
                accentColor = MaterialTheme.colorScheme.tertiary,
                expanded = expandFamilySync,
                onToggleExpand = { expandFamilySync = !expandFamilySync },
                testTag = "section_family_sync"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CÓDIGO DE CONVITE DA FAMÍLIA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = household.householdId,
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
                            text = "Entrar ou Alternar Espaço Familiar",
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
    val organicShape = RoundedCornerShape(cardRadius)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (expanded) 0.32f else 0.14f),
                shape = organicShape
            )
            .testTag(testTag),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp)
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

            AnimatedVisibility(visible = expanded) {
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
