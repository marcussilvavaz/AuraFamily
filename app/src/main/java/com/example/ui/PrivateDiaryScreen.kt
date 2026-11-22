package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppCustomizationConfig
import com.example.data.DayShift
import com.example.data.MoodOption
import com.example.data.PrivateDiaryCatalog
import com.example.data.PrivateDiaryEntry
import com.example.data.PrivateDiaryRepository
import com.example.data.PrivateDiaryState
import com.example.data.SupportLetterRecord
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reusable discrete & welcoming card displaying the motivational quote for the current shift:
 * - Manhã: 05:00 às 11:59
 * - Tarde: 12:00 às 17:59
 * - Noite: 18:00 às 04:59
 */
@Composable
fun ShiftMotivationalQuoteCard(
    diaryState: PrivateDiaryState,
    onNextQuote: () -> Unit,
    onOpenDiary: (() -> Unit)? = null,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    accentOverrideHex: Long? = null,
    modifier: Modifier = Modifier
) {
    val cardRadius = LocalOrganicCardRadius.current
    val shift = diaryState.activeShift
    val effectiveHex = accentOverrideHex ?: shift.accentColorHex
    val accentColor = remember(effectiveHex) { Color(effectiveHex.toInt()) }
    var isExpanded by remember { mutableStateOf(if (collapsible) initiallyExpanded else true) }
    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "shiftQuoteChevron"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shift_motivational_quote_card"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isExpanded) 0.14f else 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = if (isExpanded) 0.34f else 0.18f),
                    shape = RoundedCornerShape(cardRadius)
                )
                .then(
                    if (collapsible) {
                        Modifier.clickable { isExpanded = !isExpanded }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = "${shift.emoji} ${shift.label} • ${shift.timeRangeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (collapsible && !isExpanded) {
                        Text(
                            text = "Mensagem do turno",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onNextQuote,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_next_shift_quote")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Outra frase para este turno",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (collapsible) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.14f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Recolher mensagem" else "Expandir mensagem",
                                    tint = accentColor,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Top
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(240)
                ),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    shrinkTowards = Alignment.Top
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(180)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "“${diaryState.currentShiftQuote}”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "— ${diaryState.currentShiftQuoteAuthor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (onOpenDiary != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accentColor.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenDiary() }
                                    .testTag("btn_open_diary_from_quote")
                            ) {
                                Text(
                                    text = "📔 Abrir Diário Privado",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivateDiaryScreen(
    initialEpochDay: Long,
    customizationConfig: AppCustomizationConfig,
    diaryRepo: PrivateDiaryRepository,
    onShowBannerMessage: (String) -> Unit
) {
    val diaryState by diaryRepo.stateFlow.collectAsStateWithLifecycle()
    val cardRadius = LocalOrganicCardRadius.current
    val scope = rememberCoroutineScope()

    var selectedDayEpoch by rememberSaveable(initialEpochDay) {
        mutableLongStateOf(initialEpochDay)
    }
    val selectedDate = remember(selectedDayEpoch) { LocalDate.ofEpochDay(selectedDayEpoch) }
    val todayEpoch = remember { LocalDate.now().toEpochDay() }

    val dayEntries = remember(diaryState.entries, selectedDayEpoch) {
        diaryState.entries
            .filter { it.epochDay == selectedDayEpoch }
            .sortedBy { it.hour * 60 + it.minute }
    }

    // Form state for quick hourly mood & symptom logging
    val nowTime = remember { LocalTime.now() }
    var entryHour by remember { mutableIntStateOf(nowTime.hour) }
    var entryMinute by remember { mutableIntStateOf(nowTime.minute) }
    var selectedMoodId by remember { mutableStateOf("CALM") }
    val selectedSymptoms = remember { mutableStateListOf("Boa disposição") }
    var freeTextInput by remember { mutableStateOf("") }

    var showAddSymptomDialog by remember { mutableStateOf(false) }
    var newSymptomInput by remember { mutableStateOf("") }

    // AI Support Letter generation states
    var isGeneratingLetter by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<SupportLetterRecord?>(null) }
    var showSavedLettersHistory by remember { mutableStateOf(false) }
    var showApiKeyConfigDialog by remember { mutableStateOf(false) }

    val allSymptomTags = remember(diaryState.customSymptomTags) {
        (PrivateDiaryCatalog.defaultSymptomTags + diaryState.customSymptomTags).distinct()
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale.forLanguageTag("pt-BR"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("private_diary_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER: DIÁRIO PRIVADO & ISOLADO POR USUÁRIO
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Privado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Diário Privado & Acolhimento",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = "Espaço 100% individual • Humor por hora, sintomas e Carta de Apoio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date selector bar (< Dia anterior | Data | Próximo dia >)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { selectedDayEpoch -= 1L },
                            modifier = Modifier.testTag("diary_prev_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Dia anterior"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedDate.format(dateFormatter)
                                    .replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = when (selectedDayEpoch) {
                                    todayEpoch -> "Hoje • ${dayEntries.size} registro(s)"
                                    todayEpoch - 1L -> "Ontem • ${dayEntries.size} registro(s)"
                                    else -> "${dayEntries.size} registro(s) nesta data"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedDayEpoch != todayEpoch) {
                                TextButton(onClick = { selectedDayEpoch = todayEpoch }) {
                                    Text("Hoje", fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(
                                onClick = { selectedDayEpoch += 1L },
                                modifier = Modifier.testTag("diary_next_day_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Próximo dia"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick 7-day history strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (offset in -6..0) {
                            val dayEp = todayEpoch + offset
                            val d = LocalDate.ofEpochDay(dayEp)
                            val countForDay = diaryState.entries.count { it.epochDay == dayEp }
                            val isSelected = dayEp == selectedDayEpoch
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .clickable { selectedDayEpoch = dayEp }
                                    .testTag("diary_day_chip_$offset")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (offset == 0) "Hoje" else String.format(Locale.US, "%02d/%02d", d.dayOfMonth, d.monthValue),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = if (countForDay > 0) "$countForDay reg." else "—",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. FRASES MOTIVACIONAIS POR TURNO (Manhã 05:00-11:59, Tarde 12:00-17:59, Noite 18:00-04:59)
        if (customizationConfig.threeShiftQuotesEnabled) {
            item {
                ShiftMotivationalQuoteCard(
                    diaryState = diaryState,
                    onNextQuote = { diaryRepo.refreshShiftQuote(forceNewQuote = true) }
                )
            }
        }

        // 3. CARTA DE APOIO PERSONALIZADA VIA IA (Menu Expansível)
        if (customizationConfig.aiSupportLetterEnabled) {
            item {
                AuraExpandableSectionCard(
                    title = "💌 Carta de Apoio Personalizada via IA",
                    subtitle = "Reflexão empática baseada nos seus registros recentes de humor e sintomas",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Color(customizationConfig.diaryColorHex),
                    badgeText = "${diaryState.savedSupportLetters.size} salvas",
                    initiallyExpanded = false,
                    testTag = "expandable_support_letter_section"
                ) {
                    SupportLetterGeneratorCard(
                        diaryState = diaryState,
                        activeLetter = activeLetter,
                        isGenerating = isGeneratingLetter,
                        showHistory = showSavedLettersHistory,
                        onToggleHistory = { showSavedLettersHistory = !showSavedLettersHistory },
                        onGenerateLetter = {
                            scope.launch {
                                isGeneratingLetter = true
                                val generated = diaryRepo.generatePersonalizedSupportLetter(
                                    userName = customizationConfig.profileName.ifBlank { "Você" },
                                    targetEpochDay = selectedDayEpoch
                                )
                                activeLetter = generated
                                isGeneratingLetter = false
                            }
                        },
                        onSaveLetter = { letter ->
                            diaryRepo.saveSupportLetterToHistory(letter)
                            onShowBannerMessage("Carta de Apoio salva no seu histórico privado!")
                        },
                        onDeleteLetter = { id ->
                            diaryRepo.deleteSupportLetter(id)
                        },
                        onOpenApiKeyConfig = { showApiKeyConfigDialog = true }
                    )
                }
            }
        }

        // 4. NOVO REGISTRO DE HUMOR E SINTOMAS POR HORA (Menu Expansível - Somente se marcado no questionário)
        if (customizationConfig.hourlyMoodEnabled) {
            item {
                AuraExpandableSectionCard(
                    title = "📝 Registrar Humor & Sintomas por Hora",
                subtitle = "Carimbo das ${String.format(Locale.US, "%02d:%02d", entryHour, entryMinute)} • Humor, tags e relato livre",
                icon = Icons.Default.EditNote,
                accentColor = Color(customizationConfig.diaryColorHex),
                badgeText = String.format(Locale.US, "%02d:%02d", entryHour, entryMinute),
                initiallyExpanded = false,
                testTag = "card_new_diary_entry"
            ) {
                // Quick Hour Adjuster Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Horário:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val now = LocalTime.now()
                            entryHour = now.hour
                            entryMinute = now.minute
                        },
                        label = { Text("Agora (${LocalTime.now().let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) }})") }
                    )
                    listOf(8 to 0, 10 to 30, 13 to 0, 16 to 0, 19 to 30, 22 to 0).forEach { (h, m) ->
                        val isSel = entryHour == h && entryMinute == m
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                entryHour = h
                                entryMinute = m
                            },
                            label = { Text(String.format(Locale.US, "%02d:%02d", h, m)) }
                        )
                    }
                }

                // Mood Selection Chips
                Text(
                    text = "1. Como está o seu humor agora?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrivateDiaryCatalog.moodOptions.forEach { mood ->
                        val isSelected = selectedMoodId == mood.id
                        val moodColor = Color(mood.colorHex.toInt())
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) {
                                moodColor.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) moodColor else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedMoodId = mood.id }
                                .testTag("mood_chip_${mood.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = mood.emoji)
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) moodColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Symptom Multi-Tags Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "2. Sintomas ou Sensações (múltipla escolha)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddSymptomDialog = true },
                        modifier = Modifier.testTag("btn_add_custom_symptom")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nova Tag", style = MaterialTheme.typography.labelMedium)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSymptomTags.forEach { tag ->
                        val isSelected = selectedSymptoms.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedSymptoms.remove(tag)
                                } else {
                                    selectedSymptoms.add(tag)
                                }
                            },
                            label = { Text(tag) }
                        )
                    }
                }

                // Free text input
                OutlinedTextField(
                    value = freeTextInput,
                    onValueChange = { freeTextInput = it },
                    label = { Text("3. Relato livre (pensamentos, gatilhos ou observações)") },
                    placeholder = { Text("Ex.: Hoje acordei bem disposto, sem dor de cabeça...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_diary_free_text")
                )

                Button(
                    onClick = {
                        diaryRepo.addDiaryEntry(
                            epochDay = selectedDayEpoch,
                            hour = entryHour,
                            minute = entryMinute,
                            moodId = selectedMoodId,
                            symptomTags = selectedSymptoms.toList(),
                            freeText = freeTextInput
                        )
                        freeTextInput = ""
                        val now = LocalTime.now()
                        entryHour = now.hour
                        entryMinute = now.minute
                        onShowBannerMessage("Registro salvo na linha do tempo do Diário Privado!")
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_diary_entry")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar Registro das ${String.format(Locale.US, "%02d:%02d", entryHour, entryMinute)}",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        }

        // 5. LINHA DO TEMPO DIÁRIA ORDENADA POR HORA
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Linha do Tempo do Dia (${dayEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ordenada por hora",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (dayEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📔 Nenhum registro neste dia ainda",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registre seu humor e sintomas acima ou use o microfone flutuante por voz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(dayEntries, key = { it.id }) { entry ->
                PrivateDiaryTimelineItemCard(
                    entry = entry,
                    onDelete = { diaryRepo.deleteDiaryEntry(entry.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showAddSymptomDialog) {
        AlertDialog(
            onDismissRequest = { showAddSymptomDialog = false },
            title = { Text("Nova Tag de Sintoma / Sensação", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSymptomInput,
                    onValueChange = { newSymptomInput = it },
                    label = { Text("Nome do sintoma ou sensação") },
                    placeholder = { Text("Ex.: Sensibilidade à luz, Azia, Calma...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newSymptomInput.trim()
                        if (clean.isNotBlank()) {
                            diaryRepo.addCustomSymptomTag(clean)
                            if (!selectedSymptoms.contains(clean)) {
                                selectedSymptoms.add(clean)
                            }
                            newSymptomInput = ""
                            showAddSymptomDialog = false
                        }
                    }
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSymptomDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showApiKeyConfigDialog) {
        LocalGeminiApiKeyDialog(
            diaryRepo = diaryRepo,
            onDismiss = { showApiKeyConfigDialog = false },
            onSaved = {
                showApiKeyConfigDialog = false
                onShowBannerMessage("Configuração da chave Gemini atualizada com segurança!")
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateDiaryTimelineItemCard(
    entry: PrivateDiaryEntry,
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val mood: MoodOption = remember(entry.moodId) { PrivateDiaryCatalog.moodById(entry.moodId) }
    val moodColor = remember(mood.colorHex) { Color(mood.colorHex.toInt()) }
    val shift = entry.shift

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diary_entry_card_${entry.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Hour Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(58.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = moodColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = entry.timeFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = moodColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shift.emoji,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = moodColor.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = mood.emoji)
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = moodColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir entrada",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (entry.symptomTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        entry.symptomTags.forEach { symptom ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = symptom,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (entry.freeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = entry.freeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportLetterGeneratorCard(
    diaryState: PrivateDiaryState,
    activeLetter: SupportLetterRecord?,
    isGenerating: Boolean,
    showHistory: Boolean,
    onToggleHistory: () -> Unit,
    onGenerateLetter: () -> Unit,
    onSaveLetter: (SupportLetterRecord) -> Unit,
    onDeleteLetter: (String) -> Unit,
    onOpenApiKeyConfig: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val isActiveSaved = remember(activeLetter, diaryState.savedSupportLetters) {
        activeLetter != null && diaryState.savedSupportLetters.any { it.id == activeLetter.id }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_letter_generator_card"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Carta de Apoio Personalizada via IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (diaryState.hasLocalApiKeyConfigured) {
                                "Gemini API ativa • Baseada nos seus humores e sintomas recentes"
                            } else {
                                "Analisa seu humor e sintomas recentes (com suporte a Gemini API e modo offline)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onOpenApiKeyConfig,
                    modifier = Modifier.testTag("btn_configure_gemini_key")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Configurar chave Gemini",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateLetter,
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_generate_support_letter")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escrevendo Carta...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar Carta de Apoio", fontWeight = FontWeight.ExtraBold)
                    }
                }

                if (diaryState.savedSupportLetters.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onToggleHistory,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("btn_toggle_saved_letters")
                    ) {
                        Text(
                            text = "Salvas (${diaryState.savedSupportLetters.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active generated letter view
            AnimatedVisibility(visible = activeLetter != null) {
                activeLetter?.let { letter ->
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💌 ${letter.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (letter.generatedViaCloudAi) {
                                            IncomeEmerald.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ) {
                                        Text(
                                            text = if (letter.generatedViaCloudAi) "Gemini IA" else "Acolhimento Local",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (letter.generatedViaCloudAi) {
                                                IncomeEmerald
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = letter.moodSummaryUsed,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = letter.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onSaveLetter(letter) },
                                        enabled = !isActiveSaved,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("btn_save_support_letter")
                                    ) {
                                        Icon(
                                            imageVector = if (isActiveSaved) {
                                                Icons.Default.BookmarkAdded
                                            } else {
                                                Icons.Default.BookmarkAdd
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isActiveSaved) "Salva no Histórico" else "Salvar Carta no Histórico",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved letters history list
            AnimatedVisibility(visible = showHistory && diaryState.savedSupportLetters.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()
                    Text(
                        text = "Histórico de Cartas de Apoio Salvas",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    diaryState.savedSupportLetters.forEach { saved ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${saved.dateIso} às ${saved.timeFormatted} • ${saved.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onDeleteLetter(saved.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remover carta",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = saved.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalGeminiApiKeyDialog(
    diaryRepo: PrivateDiaryRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var keyInput by remember {
        mutableStateOf(diaryRepo.getConfiguredGeminiApiKey())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chave da API Gemini (Opcional)",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "O aplicativo lê automaticamente a chave GEMINI_API_KEY via BuildConfig (painel Secrets do AI Studio). Se preferir, você também pode informar uma chave local abaixo. Caso fique em branco ou offline, o app usa o gerador empático local automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Chave Gemini API") },
                    placeholder = { Text("AIzaSy...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    diaryRepo.saveLocalGeminiApiKey(keyInput)
                    onSaved()
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
