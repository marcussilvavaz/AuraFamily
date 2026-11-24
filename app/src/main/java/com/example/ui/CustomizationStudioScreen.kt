package com.example.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AlarmScheduler
import com.example.alarm.TaskTonePreviewPlayer
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationCatalog
import com.example.data.CustomizationRepository
import com.example.data.ThemePresetInfo
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.resolveFontFamilyById

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationStudioScreen(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val activePreviewId by TaskTonePreviewPlayer.activePreviewIdFlow.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            TaskTonePreviewPlayer.stopPreview(context)
        }
    }

    // 0 = Fundo, 1 = Cartões, 2 = Letras, 3 = Botão/Primária, 4 = Destaque
    var selectedColorTarget by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customization_studio_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Organic Studio Header & Live Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Estúdio de Personalização Total",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Molde as cores de fundo, cartões, letras, fontes e sons da família",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live sample preview pill inside card
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Prévia ao vivo: Fonte ${
                                        CustomizationCatalog.fontChoices.firstOrNull { it.id == config.fontFamilyId }?.displayName ?: "Nunito"
                                    } • Curva ${config.cardCornerRadiusDp}dp",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Ready-Made Family Theme Presets (8 Rich Themes)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Temas Prontos & Ambientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque em um tema para transformar todas as cores de uma só vez",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(CustomizationCatalog.presets, key = { it.id }) { preset ->
                            PresetThemeCard(
                                preset = preset,
                                isSelected = !config.useCustomColors && config.presetId == preset.id,
                                onClick = {
                                    customizationRepo.applyThemePreset(preset)
                                    onShowMessage("Tema '${preset.name}' aplicado!")
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Individual Element Color Studio (Fundo, Cartões, Letras, Botões, Destaque)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Paleta Individual (Fundo, Cartões, Letras e Destaques)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Escolha qual parte do aplicativo deseja colorir e toque na cor desejada:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val targets = listOf(
                            0 to "1. Fundo da Tela",
                            1 to "2. Cor dos Cartões",
                            2 to "3. Cor das Letras",
                            3 to "4. Botões & Ícones",
                            4 to "5. Cor de Destaque"
                        )
                        targets.forEach { (idx, label) ->
                            FilterChip(
                                selected = selectedColorTarget == idx,
                                onClick = { selectedColorTarget = idx },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("color_target_chip_$idx")
                            )
                        }
                    }

                    val activeSwatches: List<Pair<String, Long>> = when (selectedColorTarget) {
                        0 -> CustomizationCatalog.backgroundSwatches
                        1 -> CustomizationCatalog.cardSwatches
                        2 -> CustomizationCatalog.textSwatches
                        else -> CustomizationCatalog.primaryAndAccentSwatches
                    }

                    val currentHexForTarget: Long = when (selectedColorTarget) {
                        0 -> config.backgroundHex
                        1 -> config.cardHex
                        2 -> config.textHex
                        3 -> config.primaryHex
                        else -> config.accentHex
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeSwatches.forEach { (colorName, hexVal) ->
                            val isCurrent = currentHexForTarget == hexVal
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        when (selectedColorTarget) {
                                            0 -> customizationRepo.updateCustomColor(backgroundHex = hexVal)
                                            1 -> customizationRepo.updateCustomColor(cardHex = hexVal)
                                            2 -> customizationRepo.updateCustomColor(textHex = hexVal)
                                            3 -> customizationRepo.updateCustomColor(primaryHex = hexVal)
                                            4 -> customizationRepo.updateCustomColor(accentHex = hexVal)
                                        }
                                    }
                                    .then(
                                        if (isCurrent) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                                        } else Modifier
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(hexVal))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (Color(hexVal).red + Color(hexVal).green > 1.1f) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = colorName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Organic Curvature Slider (Menos Quadrado / Mais Orgânico)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RoundedCorner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Formato Orgânico dos Cartões (${config.cardCornerRadiusDp} dp)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ajuste o nível de arredondamento para um visual mais acolhedor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Slider(
                        value = config.cardCornerRadiusDp.toFloat(),
                        onValueChange = { customizationRepo.updateCardCornerRadius(it.toInt()) },
                        valueRange = 18f..38f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20 to "Suave (20dp)", 30 to "Orgânico (30dp)", 36 to "Super Curvo (36dp)").forEach { (dpVal, label) ->
                            FilterChip(
                                selected = config.cardCornerRadiusDp == dpVal,
                                onClick = { customizationRepo.updateCardCornerRadius(dpVal) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            }
        }

        // 5. Rich Typography Gallery (10 Selectable Google Fonts)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Coleção de Fontes (10 Estilos)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Escolha a tipografia que mais combina com a sua família",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomizationCatalog.fontChoices.forEach { fontChoice ->
                            val isSelected = config.fontFamilyId == fontChoice.id
                            val previewFont = resolveFontFamilyById(fontChoice.id)

                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        customizationRepo.updateFontFamily(fontChoice.id)
                                        onShowMessage("Fonte alterada para ${fontChoice.displayName}!")
                                    }
                                    .testTag("font_choice_${fontChoice.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fontChoice.displayName,
                                            fontFamily = previewFont,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${fontChoice.styleHint} • R$ 1.250,00",
                                            fontFamily = previewFont,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selecionada",
                                                tint = MaterialTheme.colorScheme.onPrimary,
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
}

@Composable
private fun PresetThemeCard(
    preset: ThemePresetInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(preset.cardHex),
        modifier = Modifier
            .width(175.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(preset.primaryHex) else Color(preset.textHex).copy(alpha = 0.2f),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("preset_theme_${preset.id}")
    ) {
        Column(
            modifier = Modifier
                .background(Color(preset.backgroundHex))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(preset.primaryHex))
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(preset.accentHex))
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(preset.cardHex))
                        .border(1.dp, Color(preset.textHex).copy(alpha = 0.3f), CircleShape)
                )
            }

            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(preset.textHex)
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(preset.textHex).copy(alpha = 0.75f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun BuiltInSoundChip(
    label: String,
    ringtoneType: Int,
    onSelect: (String, String) -> Unit
) {
    FilterChip(
        selected = false,
        onClick = {
            val uri = try {
                RingtoneManager.getDefaultUri(ringtoneType)?.toString() ?: ""
            } catch (_: Exception) {
                ""
            }
            onSelect(uri, label)
        },
        label = { Text(label) },
        shape = RoundedCornerShape(50)
    )
}
