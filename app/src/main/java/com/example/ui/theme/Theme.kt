package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationCatalog
import com.example.data.OrganicVisualStyle

val LocalOrganicCardRadius = staticCompositionLocalOf<Dp> { 24.dp }
val LocalOrganicVisualStyle = staticCompositionLocalOf<String> { OrganicVisualStyle.ORGANICO_FLUIDO }
val LocalFinancePrivacyHideValues = staticCompositionLocalOf<Boolean> { false }

private fun contrastColorFor(color: Color): Color {
    return if (color.luminance() > 0.45f) Color(0xFF0F172A) else Color(0xFFFFFFFF)
}

private fun blendColors(base: Color, overlay: Color, ratio: Float): Color {
    val r = base.red * (1f - ratio) + overlay.red * ratio
    val g = base.green * (1f - ratio) + overlay.green * ratio
    val b = base.blue * (1f - ratio) + overlay.blue * ratio
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 1f)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Retorna a forma de botões e chips respeitando o Estilo Visual Orgânico global:
 * - Pílula & Fluido: CircleShape (totalmente arredondado)
 * - Translúcido: RoundedCornerShape(16.dp)
 * - Suave Minimal: RoundedCornerShape(8.dp)
 */
@Composable
fun omniOrganicButtonShape(
    style: String = LocalOrganicVisualStyle.current
): Shape {
    return when (style) {
        OrganicVisualStyle.ORGANICO_FLUIDO -> CircleShape
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> RoundedCornerShape(16.dp)
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> RoundedCornerShape(8.dp)
        else -> CircleShape
    }
}

/**
 * Retorna a elevação padrão dos cards respeitando o Estilo Visual Orgânico global:
 * - Pílula & Fluido: defaultDp (ex: 3.dp)
 * - Translúcido: 0.dp (efeito vidro com borda luminosa)
 * - Suave Minimal: 0.dp (superfície plana sem elevação)
 */
@Composable
fun omniOrganicCardElevationDp(
    defaultDp: Dp = 3.dp,
    style: String = LocalOrganicVisualStyle.current
): Dp {
    return when (style) {
        OrganicVisualStyle.ORGANICO_FLUIDO -> defaultDp
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> 0.dp
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 0.dp
        else -> defaultDp
    }
}

/**
 * Retorna a borda global para cards conforme o Estilo Visual Orgânico e o tema Claro/Escuro.
 */
@Composable
fun omniOrganicCardBorder(
    accentColor: Color = MaterialTheme.colorScheme.primary,
    style: String = LocalOrganicVisualStyle.current
): BorderStroke {
    val outline = MaterialTheme.colorScheme.outline
    return when (style) {
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> BorderStroke(1.dp, outline.copy(alpha = 0.65f))
        else -> BorderStroke(1.dp, outline.copy(alpha = 0.28f))
    }
}

/**
 * Aplica a borda do Estilo Visual Orgânico em qualquer Card ou Container.
 */
fun Modifier.omniOrganicBorder(
    accentColor: Color? = null,
    shape: Shape? = null
): Modifier = composed {
    val style = LocalOrganicVisualStyle.current
    val radius = LocalOrganicCardRadius.current
    val resolvedShape = shape ?: RoundedCornerShape(radius)
    val resolvedAccent = accentColor ?: MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    when (style) {
        OrganicVisualStyle.TRANSLUCIDO_GLASS ->
            this.border(1.dp, resolvedAccent.copy(alpha = 0.48f), resolvedShape)
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR ->
            this.border(1.dp, outline.copy(alpha = 0.72f), resolvedShape)
        else ->
            this.border(1.dp, outline.copy(alpha = 0.28f), resolvedShape)
    }
}

@Composable
fun AuraFinancasTheme(
    themeMode: String = "SYSTEM",
    customization: AppCustomizationConfig = AppCustomizationConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val presetInfo = CustomizationCatalog.presets.firstOrNull { it.id == customization.presetId }
    val resolvedMode = themeMode.ifBlank { customization.themeMode }.ifBlank { "SYSTEM" }
    val isAmoled = resolvedMode == "AMOLED"

    val isDarkBg = when (resolvedMode) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> {
            if (!customization.useCustomColors && presetInfo != null) {
                presetInfo.isDark || systemDark
            } else {
                systemDark
            }
        }
    }

    val visualStyle = customization.organicVisualStyle
    val isTranslucent = visualStyle == OrganicVisualStyle.TRANSLUCIDO_GLASS

    val primaryColor = Color(customization.primaryHex)
    val accentColor = Color(customization.accentHex)

    // Cores de fundo, card e texto com alto contraste garantido
    // Quando AMOLED estiver ativo, sobrescreve com prioridade absoluta o fundo principal para preto puro (#000000)
    // e superfícies para cinza escuro (#0D0D0D / #141414)
    val bgColor = when {
        isAmoled -> Color(0xFF000000)
        isDarkBg -> {
            val rawBg = Color(customization.backgroundHex)
            if (rawBg.luminance() < 0.25f) rawBg else Color(0xFF0F141C)
        }
        else -> Color(0xFFF6F7FB) // Superfície clara limpa (#F6F7FB) sem fundos bege/oliva
    }

    val baseCardColor = when {
        isAmoled -> Color(0xFF0D0D0D)
        isDarkBg -> {
            val rawCard = Color(customization.cardHex)
            if (rawCard.luminance() < 0.35f) rawCard else Color(0xFF171E29)
        }
        else -> Color(0xFFFFFFFF) // Cards brancos (#FFFFFF) no Modo Claro
    }

    // No estilo "Translúcido", aplicamos alpha 0.65f globalmente na superfície dos cards
    val cardColor = if (isTranslucent) {
        baseCardColor.copy(alpha = 0.65f)
    } else {
        baseCardColor
    }

    val textColor = when {
        isAmoled -> Color(0xFFF9FAFB)
        isDarkBg -> Color(0xFFF8FAFC)
        else -> Color(0xFF0F172A) // Alto contraste (#0F172A) no Modo Claro
    }

    val onPrimary = contrastColorFor(primaryColor)
    val onAccent = contrastColorFor(accentColor)
    val primaryContainer = blendColors(baseCardColor, primaryColor, if (isDarkBg) 0.28f else 0.14f)
        .let { if (isTranslucent) it.copy(alpha = 0.65f) else it }
    val secondaryContainer = blendColors(baseCardColor, accentColor, if (isDarkBg) 0.26f else 0.16f)
        .let { if (isTranslucent) it.copy(alpha = 0.65f) else it }

    val surfaceVariant = when {
        isAmoled -> if (isTranslucent) Color(0xFF141414).copy(alpha = 0.65f) else Color(0xFF141414)
        isDarkBg -> if (isTranslucent) Color(0xFF222B38).copy(alpha = 0.65f) else Color(0xFF222B38)
        else -> if (isTranslucent) Color(0xFFEEF2F6).copy(alpha = 0.65f) else Color(0xFFEEF2F6)
    }

    val onSurfaceVariant = when {
        isAmoled -> Color(0xFF9CA3AF)
        isDarkBg -> Color(0xFF94A3B8)
        else -> Color(0xFF334155)
    }
    val outline = when {
        isTranslucent -> primaryColor.copy(alpha = 0.48f)
        isAmoled -> Color(0xFF1F2937)
        isDarkBg -> Color(0xFF334155)
        else -> Color(0xFFE2E8F0) // Borda sutil (#E2E8F0) nos cards claros
    }

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
            tertiary = Color(customization.healthColorHex),
            onTertiary = Color.White,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = textColor,
            background = bgColor,
            onBackground = textColor,
            surface = cardColor,
            onSurface = textColor,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outline.copy(alpha = 0.65f),
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
            tertiary = Color(customization.healthColorHex),
            onTertiary = Color.White,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = textColor,
            background = bgColor,
            onBackground = textColor,
            surface = cardColor,
            onSurface = textColor,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = Color(0xFFE2E8F0),
            error = ExpenseCoral,
            onError = Color.White
        )
    }

    // Configura dinamicamente os ícones da Barra de Status do Android (relógio, Wi-Fi, bateria):
    // ESCUROS quando o Modo Claro estiver ativo (!isDarkBg) e CLAROS/BRANCOS no Modo Escuro ou Preto Puro (AMOLED).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkBg
            insetsController.isAppearanceLightNavigationBars = !isDarkBg
        }
    }

    val resolvedRadiusDp = when (visualStyle) {
        OrganicVisualStyle.ORGANICO_FLUIDO -> 24.dp
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> 20.dp
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 10.dp
        else -> 24.dp
    }

    val smallShapeRadius = when (visualStyle) {
        OrganicVisualStyle.ORGANICO_FLUIDO -> 50.dp // CircleShape nos botões e chips
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> 16.dp
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 8.dp
        else -> 50.dp
    }

    val organicShapes = Shapes(
        extraSmall = RoundedCornerShape(smallShapeRadius),
        small = RoundedCornerShape(smallShapeRadius),
        medium = RoundedCornerShape(resolvedRadiusDp),
        large = RoundedCornerShape(resolvedRadiusDp),
        extraLarge = RoundedCornerShape(resolvedRadiusDp)
    )

    CompositionLocalProvider(
        LocalOrganicCardRadius provides resolvedRadiusDp,
        LocalOrganicVisualStyle provides visualStyle,
        LocalFinancePrivacyHideValues provides customization.financePrivacyHideValues
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(customization.fontFamilyId),
            shapes = organicShapes,
            content = content
        )
    }
}
