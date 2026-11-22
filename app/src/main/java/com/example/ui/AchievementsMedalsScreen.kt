package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AchievementGoal
import com.example.data.AchievementModule
import com.example.data.AchievementsEvaluationSummary
import com.example.data.AppCustomizationConfig
import com.example.data.MedalTier
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Desenho vetorial em Canvas de uma Medalha Metálica (Bronze, Prata, Ouro ou Diamante)
 * com fita superior, aro metálico chanfrado, brilho radial e ícone central.
 */
@Composable
fun MedalBadgeCanvas(
    medalTier: MedalTier,
    iconEmoji: String,
    isUnlocked: Boolean,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isUnlocked) {
        Color(medalTier.primaryColorHex.toInt())
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    }
    val secondaryColor = if (isUnlocked) {
        Color(medalTier.secondaryColorHex.toInt())
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val ribbonColor = if (isUnlocked) {
        Color(medalTier.ribbonColorHex.toInt())
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f
            val medalCenterY = h * 0.56f
            val medalRadius = (minOf(w, h) * 0.37f)

            // 1. Fita superior da medalha (V-shape ribbon)
            val leftRibbon = Path().apply {
                moveTo(centerX - medalRadius * 0.65f, h * 0.08f)
                lineTo(centerX - medalRadius * 0.08f, h * 0.08f)
                lineTo(centerX - medalRadius * 0.20f, medalCenterY - medalRadius * 0.55f)
                lineTo(centerX - medalRadius * 0.72f, medalCenterY - medalRadius * 0.55f)
                close()
            }
            val rightRibbon = Path().apply {
                moveTo(centerX + medalRadius * 0.08f, h * 0.08f)
                lineTo(centerX + medalRadius * 0.65f, h * 0.08f)
                lineTo(centerX + medalRadius * 0.72f, medalCenterY - medalRadius * 0.55f)
                lineTo(centerX + medalRadius * 0.20f, medalCenterY - medalRadius * 0.55f)
                close()
            }
            drawPath(path = leftRibbon, color = ribbonColor)
            drawPath(path = rightRibbon, color = ribbonColor.copy(alpha = 0.82f))

            // 2. Halo externo luminoso quando conquistada
            if (isUnlocked) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, medalCenterY),
                        radius = medalRadius * 1.38f
                    ),
                    radius = medalRadius * 1.38f,
                    center = Offset(centerX, medalCenterY)
                )
            }

            // 3. Corpo metálico da medalha
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor,
                        primaryColor
                    ),
                    center = Offset(centerX - medalRadius * 0.28f, medalCenterY - medalRadius * 0.28f),
                    radius = medalRadius * 1.35f
                ),
                radius = medalRadius,
                center = Offset(centerX, medalCenterY)
            )

            // 4. Aro interno chanfrado da medalha
            drawCircle(
                color = if (isUnlocked) Color.White.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.4f),
                radius = medalRadius * 0.82f,
                center = Offset(centerX, medalCenterY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = primaryColor,
                radius = medalRadius,
                center = Offset(centerX, medalCenterY),
                style = Stroke(width = 2.2.dp.toPx())
            )
        }

        // Ícone central da medalha (ou cadeado discreto se bloqueada)
        Box(
            modifier = Modifier.padding(top = size * 0.12f),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = iconEmoji,
                    style = if (size >= 68.dp) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Medalha bloqueada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(size * 0.30f)
                )
            }
        }
    }
}

/**
 * Widget Estilo Smartwatch no Hub para o Sistema de Conquistas & Medalhas
 * (Inicia recolhido por padrão e expande suavemente com AnimatedVisibility).
 */
@Composable
fun SmartwatchAchievementsWidget(
    summary: AchievementsEvaluationSummary,
    customizationConfig: AppCustomizationConfig,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAchievementsScreen: () -> Unit,
    onQuickAdvanceGoal: (AchievementGoal) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val ringColor = SavingsGold
    val progressRatio = if (summary.totalCount > 0) {
        (summary.unlockedCount.toFloat() / summary.totalCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "achievementsChevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                color = ringColor.copy(alpha = if (expanded) 0.42f else 0.18f),
                shape = organicShape
            )
            .testTag("smartwatch_widget_achievements"),
        shape = organicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ringColor.copy(alpha = if (expanded) 0.14f else 0.07f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Cabeçalho Compacto (Sempre visível, toca para expandir)
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
                    // Anel Circular de Medalhas Conquistadas
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            drawArc(
                                color = ringColor.copy(alpha = 0.20f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (summary.unlockedCount == summary.totalCount && summary.totalCount > 0) {
                                    IncomeEmerald
                                } else {
                                    ringColor
                                },
                                startAngle = -90f,
                                sweepAngle = 360f * progressRatio,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${summary.unlockedCount}/${summary.totalCount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Conquistas & Medalhas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ringColor.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = "Nv. ${summary.currentLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "🩺 ${summary.healthUnlocked}/${summary.healthTotal} • 💰 ${summary.financeUnlocked}/${summary.financeTotal} • 📅 ${summary.calendarUnlocked}/${summary.calendarTotal}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "💎 ${summary.diamondUnlocked}  🥇 ${summary.goldUnlocked}  🥈 ${summary.silverUnlocked}  🥉 ${summary.bronzeUnlocked}  •  ${summary.totalXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = ringColor.copy(alpha = if (expanded) 0.24f else 0.14f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Recolher conquistas" else "Expandir conquistas",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }
                }
            }

            // Conteúdo Expansível com Transição Orgânica Suave
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Resumo dos 3 Pilares (Saúde, Financeiro e Calendário)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillarMiniProgressChip(
                            emoji = "🩺",
                            title = "Saúde",
                            unlocked = summary.healthUnlocked,
                            total = summary.healthTotal,
                            accentColor = Color(customizationConfig.healthColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                        PillarMiniProgressChip(
                            emoji = "💰",
                            title = "Finanças",
                            unlocked = summary.financeUnlocked,
                            total = summary.financeTotal,
                            accentColor = Color(customizationConfig.financeColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                        PillarMiniProgressChip(
                            emoji = "📅",
                            title = "Agenda",
                            unlocked = summary.calendarUnlocked,
                            total = summary.calendarTotal,
                            accentColor = Color(customizationConfig.agendaColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Vitrine rápida das últimas medalhas conquistadas ou próximos objetivos
                    val unlockedList = remember(summary.goals) {
                        summary.goals.filter { it.isUnlocked }
                    }
                    val nextInProgress = remember(summary.goals) {
                        summary.goals
                            .filter { !it.isUnlocked }
                            .sortedByDescending { it.progressRatio }
                            .take(2)
                    }

                    if (unlockedList.isNotEmpty()) {
                        Text(
                            text = "Medalhas Conquistadas (${unlockedList.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            unlockedList.forEach { goal ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(goal.medalTier.primaryColorHex.toInt()).copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            Color(goal.medalTier.primaryColorHex.toInt()).copy(alpha = 0.45f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onOpenAchievementsScreen() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MedalBadgeCanvas(
                                            medalTier = goal.medalTier,
                                            iconEmoji = goal.iconEmoji,
                                            isUnlocked = true,
                                            size = 36.dp
                                        )
                                        Column {
                                            Text(
                                                text = goal.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${goal.medalTier.emoji} ${goal.medalTier.label} • ${goal.module.shortLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (nextInProgress.isNotEmpty()) {
                        Text(
                            text = "Próximas Medalhas para Desbloquear:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        nextInProgress.forEach { goal ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MedalBadgeCanvas(
                                            medalTier = goal.medalTier,
                                            iconEmoji = goal.iconEmoji,
                                            isUnlocked = false,
                                            size = 40.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${goal.module.emoji} ${goal.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${goal.currentValue}/${goal.targetValue} ${goal.unitLabel} • Vale ${goal.medalTier.emoji} ${goal.medalTier.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { onQuickAdvanceGoal(goal) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("hub_advance_goal_${goal.id}")
                                    ) {
                                        Text(
                                            text = "+1",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onOpenAchievementsScreen,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_open_achievements_gallery_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Abrir Galeria de Medalhas & Criar Objetivo",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarMiniProgressChip(
    emoji: String,
    title: String,
    unlocked: Int,
    total: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (total > 0) (unlocked.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.11f),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$emoji $title",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "$unlocked/$total",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
            LinearProgressIndicator(
                progress = { ratio },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

/**
 * Tela Completa da Galeria de Conquistas & Medalhas vinculadas a Saúde, Financeiro e Calendário.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsMedalsScreen(
    summary: AchievementsEvaluationSummary,
    customizationConfig: AppCustomizationConfig,
    onAdvanceGoalProgress: (AchievementGoal) -> Unit,
    onCreateCustomGoal: (
        title: String,
        description: String,
        module: AchievementModule,
        medalTier: MedalTier,
        targetValue: Int,
        unitLabel: String
    ) -> Unit,
    onDeleteCustomGoal: (String) -> Unit,
    onNavigateToModule: (AchievementModule) -> Unit,
    onBack: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var inspectedGoal by remember { mutableStateOf<AchievementGoal?>(null) }
    var showCreateCustomGoalDialog by remember { mutableStateOf(false) }

    val filteredGoals = remember(summary.goals, selectedFilter) {
        when (selectedFilter) {
            "HEALTH" -> summary.goals.filter { it.module == AchievementModule.HEALTH }
            "FINANCE" -> summary.goals.filter { it.module == AchievementModule.FINANCE }
            "CALENDAR" -> summary.goals.filter { it.module == AchievementModule.CALENDAR }
            "UNLOCKED" -> summary.goals.filter { it.isUnlocked }
            "IN_PROGRESS" -> summary.goals.filter { !it.isUnlocked }
            else -> summary.goals
        }
    }

    fun accentForModule(module: AchievementModule): Color {
        return when (module) {
            AchievementModule.HEALTH -> Color(customizationConfig.healthColorHex.toInt())
            AchievementModule.FINANCE -> Color(customizationConfig.financeColorHex.toInt())
            AchievementModule.CALENDAR -> Color(customizationConfig.agendaColorHex.toInt())
            AchievementModule.TRINITY -> SavingsGold
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("achievements_medals_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabeçalho & Quadro de Honra (Trophy Showcase)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SavingsGold.copy(alpha = 0.20f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("achievements_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }
                            Column {
                                Text(
                                    text = "Conquistas & Medalhas",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Objetivos integrados à Saúde, Financeiro e Calendário",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showCreateCustomGoalDialog = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_create_achievement_goal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Novo Objetivo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Card de Nível & XP + Contadores de Medalhas (Diamante, Ouro, Prata, Bronze)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                                Column {
                                    Text(
                                        text = "NÍVEL ${summary.currentLevel} • ${summary.levelTitle.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${summary.unlockedCount} de ${summary.totalCount} medalhas conquistadas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SavingsGold.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "✨ ${summary.totalXp} XP",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { summary.currentLevelProgressRatio },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MedalTierCountBadge(
                                    tier = MedalTier.DIAMOND,
                                    count = summary.diamondUnlocked,
                                    modifier = Modifier.weight(1f)
                                )
                                MedalTierCountBadge(
                                    tier = MedalTier.GOLD,
                                    count = summary.goldUnlocked,
                                    modifier = Modifier.weight(1f)
                                )
                                MedalTierCountBadge(
                                    tier = MedalTier.SILVER,
                                    count = summary.silverUnlocked,
                                    modifier = Modifier.weight(1f)
                                )
                                MedalTierCountBadge(
                                    tier = MedalTier.BRONZE,
                                    count = summary.bronzeUnlocked,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Progresso por Módulo (Saúde, Financeiro, Calendário)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillarMiniProgressChip(
                            emoji = "🩺",
                            title = "Saúde",
                            unlocked = summary.healthUnlocked,
                            total = summary.healthTotal,
                            accentColor = Color(customizationConfig.healthColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                        PillarMiniProgressChip(
                            emoji = "💰",
                            title = "Financeiro",
                            unlocked = summary.financeUnlocked,
                            total = summary.financeTotal,
                            accentColor = Color(customizationConfig.financeColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                        PillarMiniProgressChip(
                            emoji = "📅",
                            title = "Calendário",
                            unlocked = summary.calendarUnlocked,
                            total = summary.calendarTotal,
                            accentColor = Color(customizationConfig.agendaColorHex.toInt()),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Filtros por Módulo e Status
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todas (${summary.totalCount})",
                    "HEALTH" to "🩺 Saúde (${summary.healthUnlocked}/${summary.healthTotal})",
                    "FINANCE" to "💰 Financeiro (${summary.financeUnlocked}/${summary.financeTotal})",
                    "CALENDAR" to "📅 Calendário (${summary.calendarUnlocked}/${summary.calendarTotal})",
                    "UNLOCKED" to "🏆 Conquistadas (${summary.unlockedCount})",
                    "IN_PROGRESS" to "🎯 Em Progresso (${summary.totalCount - summary.unlockedCount})"
                ).forEach { (filterId, label) ->
                    FilterChip(
                        selected = selectedFilter == filterId,
                        onClick = { selectedFilter = filterId },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (selectedFilter == filterId) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("achievement_filter_$filterId")
                    )
                }
            }
        }

        // 3. Lista de Objetivos & Medalhas
        items(filteredGoals, key = { it.id }) { goal ->
            val moduleAccent = accentForModule(goal.module)
            val tierColor = Color(goal.medalTier.primaryColorHex.toInt())

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (goal.isUnlocked) 1.5.dp else 1.dp,
                        color = if (goal.isUnlocked) tierColor.copy(alpha = 0.65f) else moduleAccent.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(cardRadius)
                    )
                    .clickable { inspectedGoal = goal }
                    .testTag("achievement_goal_card_${goal.id}"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (goal.isUnlocked) 4.dp else 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    (if (goal.isUnlocked) tierColor else moduleAccent).copy(alpha = if (goal.isUnlocked) 0.14f else 0.06f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp),
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
                            MedalBadgeCanvas(
                                medalTier = goal.medalTier,
                                iconEmoji = goal.iconEmoji,
                                isUnlocked = goal.isUnlocked,
                                size = 62.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = moduleAccent.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "${goal.module.emoji} ${goal.module.shortLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = moduleAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = tierColor.copy(alpha = 0.20f)
                                    ) {
                                        Text(
                                            text = "${goal.medalTier.emoji} ${goal.medalTier.label} (+${goal.medalTier.xpReward} XP)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Text(
                                    text = goal.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (goal.isCustomGoal) {
                            IconButton(
                                onClick = { onDeleteCustomGoal(goal.id) },
                                modifier = Modifier.testTag("delete_custom_goal_${goal.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Excluir objetivo personalizado",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Barra de Progresso do Objetivo + Ações
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (goal.isUnlocked) {
                                    "🏅 Medalha Conquistada! (${goal.currentValue}/${goal.targetValue} ${goal.unitLabel})"
                                } else {
                                    "Progresso: ${goal.currentValue} de ${goal.targetValue} ${goal.unitLabel}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (goal.isUnlocked) IncomeEmerald else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${goal.progressPercentage}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (goal.isUnlocked) IncomeEmerald else moduleAccent
                            )
                        }

                        LinearProgressIndicator(
                            progress = { goal.progressRatio },
                            color = if (goal.isUnlocked) IncomeEmerald else moduleAccent,
                            trackColor = moduleAccent.copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToModule(goal.module) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_goal_module_btn_${goal.id}")
                        ) {
                            Text(
                                text = "Abrir ${goal.module.shortLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (goal.isUnlocked) {
                            FilledTonalButton(
                                onClick = { inspectedGoal = goal },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("view_medal_certificate_btn_${goal.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ver Medalha",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { onAdvanceGoalProgress(goal) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("advance_goal_progress_btn_${goal.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Registrar +1 Etapa",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Certificado / Detalhes da Medalha
    if (inspectedGoal != null) {
        val goal = inspectedGoal!!
        MedalCertificateDialog(
            goal = goal,
            onAdvanceProgress = {
                onAdvanceGoalProgress(goal)
                inspectedGoal = null
            },
            onOpenModule = {
                val mod = goal.module
                inspectedGoal = null
                onNavigateToModule(mod)
            },
            onDismiss = { inspectedGoal = null }
        )
    }

    // Modal para Criar Novo Objetivo com Medalha (Saúde, Financeiro ou Calendário)
    if (showCreateCustomGoalDialog) {
        CreateCustomAchievementDialog(
            onDismiss = { showCreateCustomGoalDialog = false },
            onCreate = { title, desc, module, tier, target, unit ->
                onCreateCustomGoal(title, desc, module, tier, target, unit)
                showCreateCustomGoalDialog = false
            }
        )
    }
}

@Composable
private fun MedalTierCountBadge(
    tier: MedalTier,
    count: Int,
    modifier: Modifier = Modifier
) {
    val tierColor = Color(tier.primaryColorHex.toInt())
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tierColor.copy(alpha = 0.16f),
        modifier = modifier.border(1.dp, tierColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${tier.emoji} $count",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = tier.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MedalCertificateDialog(
    goal: AchievementGoal,
    onAdvanceProgress: () -> Unit,
    onOpenModule: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(goal.unlockedAtMillis) {
        if (goal.unlockedAtMillis > 0L) {
            SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(Date(goal.unlockedAtMillis))
        } else {
            "Hoje"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedalBadgeCanvas(
                    medalTier = goal.medalTier,
                    iconEmoji = goal.iconEmoji,
                    isUnlocked = goal.isUnlocked,
                    size = 92.dp
                )
                Text(
                    text = if (goal.isUnlocked) {
                        "${goal.medalTier.emoji} ${goal.medalTier.badgeTitle} Conquistada!"
                    } else {
                        "${goal.medalTier.emoji} Objetivo para ${goal.medalTier.badgeTitle}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Módulo Vinculado: ${goal.module.emoji} ${goal.module.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Progresso: ${goal.currentValue} / ${goal.targetValue} ${goal.unitLabel} • Recompensa: +${goal.medalTier.xpReward} XP",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        if (goal.isUnlocked) {
                            Text(
                                text = "Conquistada em: $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!goal.isUnlocked) {
                Button(
                    onClick = onAdvanceProgress,
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Avançar +1 Etapa Agora")
                }
            } else {
                Button(
                    onClick = onOpenModule,
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Ir para ${goal.module.shortLabel}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCustomAchievementDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        description: String,
        module: AchievementModule,
        medalTier: MedalTier,
        targetValue: Int,
        unitLabel: String
    ) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf(AchievementModule.HEALTH) }
    var selectedTier by remember { mutableStateOf(MedalTier.GOLD) }
    var targetValueInput by remember { mutableStateOf("5") }
    var unitLabelInput by remember { mutableStateOf("dias") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Novo Objetivo com Medalha",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Vincular a qual módulo?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AchievementModule.HEALTH,
                        AchievementModule.FINANCE,
                        AchievementModule.CALENDAR
                    ).forEach { mod ->
                        FilterChip(
                            selected = selectedModule == mod,
                            onClick = { selectedModule = mod },
                            label = { Text("${mod.emoji} ${mod.shortLabel}") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("custom_goal_module_${mod.id}")
                        )
                    }
                }

                Text(
                    text = "2. Escolha a Medalha da Recompensa:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MedalTier.entries.forEach { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            label = { Text("${tier.emoji} ${tier.label}") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("custom_goal_tier_${tier.id}")
                        )
                    }
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Título do Objetivo") },
                    placeholder = {
                        Text(
                            when (selectedModule) {
                                AchievementModule.HEALTH -> "Ex.: Beber 2L de água por 7 dias"
                                AchievementModule.FINANCE -> "Ex.: Guardar R$ 300 na reserva este mês"
                                else -> "Ex.: Concluir todas as tarefas da semana"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_goal_title_input")
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Descrição / Motivação (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetValueInput,
                        onValueChange = { targetValueInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Meta (Qtd)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_goal_target_input")
                    )
                    OutlinedTextField(
                        value = unitLabelInput,
                        onValueChange = { unitLabelInput = it },
                        label = { Text("Unidade") },
                        placeholder = { Text("dias, tarefas, R$") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetValueInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onCreate(
                        titleInput,
                        descriptionInput,
                        selectedModule,
                        selectedTier,
                        target,
                        unitLabelInput
                    )
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("confirm_create_custom_goal_btn")
            ) {
                Text("Criar Objetivo & Medalha", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
