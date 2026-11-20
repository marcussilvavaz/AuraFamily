package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppCustomizationConfig

val LocalOrganicCardRadius = staticCompositionLocalOf<Dp> { 30.dp }

private fun contrastColorFor(color: Color): Color {
    return if (color.luminance() > 0.45f) Color(0xFF102018) else Color(0xFFFFFFFF)
}

private fun blendColors(base: Color, overlay: Color, ratio: Float): Color {
    val r = base.red * (1f - ratio) + overlay.red * ratio
    val g = base.green * (1f - ratio) + overlay.green * ratio
    val b = base.blue * (1f - ratio) + overlay.blue * ratio
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 1f)
}

@Composable
fun AuraFinancasTheme(
    themeMode: String = "SYSTEM",
    customization: AppCustomizationConfig = AppCustomizationConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val forceDark = themeMode == "DARK" || (themeMode == "SYSTEM" && systemDark)

    // When the user hasn't explicitly picked a custom color or dark preset, honor themeMode if DARK is forced
    val bgColor = if (forceDark && !customization.useCustomColors && customization.presetId in listOf("EMERALD_GOLD", "WARM_TERRACOTTA", "LAVENDER_CALM", "OCEAN_BREEZE", "ROSE_PEACH", "CAFE_CREAM")) {
        Color(0xFF091410)
    } else {
        Color(customization.backgroundHex)
    }

    val cardColor = if (forceDark && !customization.useCustomColors && customization.presetId in listOf("EMERALD_GOLD", "WARM_TERRACOTTA", "LAVENDER_CALM", "OCEAN_BREEZE", "ROSE_PEACH", "CAFE_CREAM")) {
        Color(0xFF132620)
    } else {
        Color(customization.cardHex)
    }

    val textColor = if (forceDark && !customization.useCustomColors && customization.presetId in listOf("EMERALD_GOLD", "WARM_TERRACOTTA", "LAVENDER_CALM", "OCEAN_BREEZE", "ROSE_PEACH", "CAFE_CREAM")) {
        Color(0xFFE9F5F0)
    } else {
        Color(customization.textHex)
    }

    val primaryColor = Color(customization.primaryHex)
    val accentColor = Color(customization.accentHex)

    val isDarkBg = bgColor.luminance() < 0.35f
    val onPrimary = contrastColorFor(primaryColor)
    val onAccent = contrastColorFor(accentColor)
    val primaryContainer = blendColors(cardColor, primaryColor, if (isDarkBg) 0.28f else 0.16f)
    val secondaryContainer = blendColors(cardColor, accentColor, if (isDarkBg) 0.28f else 0.18f)
    val surfaceVariant = blendColors(cardColor, textColor, if (isDarkBg) 0.12f else 0.07f)
    val onSurfaceVariant = blendColors(textColor, cardColor, 0.32f)
    val outline = blendColors(textColor, cardColor, 0.65f)

    val colorScheme = if (isDarkBg) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = textColor,
            secondary = accentColor,
            onSecondary = onAccent,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = textColor,
            tertiary = accentColor,
            onTertiary = onAccent,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = textColor,
            background = bgColor,
            onBackground = textColor,
            surface = cardColor,
            onSurface = textColor,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            error = ExpenseCoral,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = textColor,
            secondary = accentColor,
            onSecondary = onAccent,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = textColor,
            tertiary = accentColor,
            onTertiary = onAccent,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = textColor,
            background = bgColor,
            onBackground = textColor,
            surface = cardColor,
            onSurface = textColor,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            error = ExpenseCoral,
            onError = Color.White
        )
    }

    val radius = customization.cardCornerRadiusDp.dp
    val organicShapes = Shapes(
        extraSmall = RoundedCornerShape((radius.value * 0.45f).dp),
        small = RoundedCornerShape((radius.value * 0.6f).dp),
        medium = RoundedCornerShape((radius.value * 0.85f).dp),
        large = RoundedCornerShape(radius),
        extraLarge = RoundedCornerShape((radius.value * 1.15f).dp)
    )

    CompositionLocalProvider(LocalOrganicCardRadius provides radius) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(customization.fontFamilyId),
            shapes = organicShapes,
            content = content
        )
    }
}
