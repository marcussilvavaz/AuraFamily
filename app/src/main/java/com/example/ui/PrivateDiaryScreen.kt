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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.data.DiaryLetterRecord
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
    customizationRepo: com.example.data.CustomizationRepository? = null,
    diaryRepo: PrivateDiaryRepository,
    onShowBannerMessage: (String) -> Unit,
    initialSubTab: Int = 0,
    householdMuralItems: List<com.example.data.FamilyMuralItem> = emptyList(),
    onAddMuralItem: (title: String, category: String, notes: String, pinned: Boolean, isNote: Boolean) -> Unit = { _, _, _, _, _ -> },
    onToggleMuralItem: (itemId: String) -> Unit = {},
    onTogglePinMuralItem: (itemId: String) -> Unit = {},
    onClearCompletedMuralItems: (category: String?) -> Unit = {},
    onDeleteMuralItem: (itemId: String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val diaryState by diaryRepo.stateFlow.collectAsStateWithLifecycle()
    val cardRadius = LocalOrganicCardRadius.current
    val organicButtonShape = com.example.ui.theme.omniOrganicButtonShape()
    val scope = rememberCoroutineScope()
    val triggerFeedback = rememberOmniFeedback(onShowBannerMessage)

    // Sub-tabs: 0 = Meu Diário (Com Bloqueio PIN/Biometria), 1 = Mural da Família (Compartilhado sem bloqueio)
    var activeSubTab by rememberSaveable(initialSubTab) { mutableIntStateOf(initialSubTab) }

    // When diaryBiometricLockEnabled is true, opening the "Meu Diário" sub-tab starts locked
    var isUnlockedInThisVisit by remember {
        mutableStateOf(!customizationConfig.diaryBiometricLockEnabled)
    }
    var showSecurityConfigModal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Sub-tab Switcher: "Meu Diário" | "Mural da Família"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                label = {
                    Text(
                        text = "🔒 Meu Diário",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                shape = organicButtonShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("diary_subtab_private")
            )
            FilterChip(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                label = {
                    Text(
                        text = "📌 Mural da Família",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                shape = organicButtonShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("diary_subtab_family_mural")
            )
        }

        if (activeSubTab == 1) {
            FamilyMuralSharedSubScreen(
                customizationConfig = customizationConfig,
                remoteMuralItems = householdMuralItems,
                onAddMuralItem = onAddMuralItem,
                onToggleMuralItem = onToggleMuralItem,
                onTogglePinMuralItem = onTogglePinMuralItem,
                onClearCompletedMuralItems = onClearCompletedMuralItems,
                onDeleteMuralItem = onDeleteMuralItem
            )
            return@Column
        }

        if (customizationConfig.diaryBiometricLockEnabled && !isUnlockedInThisVisit) {
            PrivateDiaryBiometricLockGate(
                backupPin = customizationConfig.diaryBackupPin.ifBlank { "1234" },
                accentColor = Color(customizationConfig.diaryColorHex),
                onUnlocked = {
                    isUnlockedInThisVisit = true
                    triggerFeedback("🔓 Diário Privado desbloqueado!")
                }
            )
            return@Column
        }

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

    // Edit & Delete diary entry states
    var editingEntry by remember { mutableStateOf<PrivateDiaryEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<PrivateDiaryEntry?>(null) }

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

    if (showSecurityConfigModal && customizationRepo != null) {
        val savedPin = customizationConfig.diaryBackupPin.ifBlank { "1234" }
        var lockEnabledDraft by remember(customizationConfig.diaryBiometricLockEnabled) {
            mutableStateOf(customizationConfig.diaryBiometricLockEnabled)
        }
        var isCurrentPinVerified by remember(customizationConfig.diaryBiometricLockEnabled) {
            mutableStateOf(!customizationConfig.diaryBiometricLockEnabled)
        }
        var currentPinInput by remember { mutableStateOf("") }
        var currentPinError by remember { mutableStateOf(false) }
        var newPinDraft by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSecurityConfigModal = false },
            title = {
                Text(
                    text = "🔒 Bloqueio do Diário (Biometria / PIN)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quando ativado, o Omni exigirá Biometria (impressão digital / facial) ou PIN sempre que você abrir a aba Diário.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exigir Biometria / PIN ao abrir",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Switch(
                            checked = lockEnabledDraft,
                            onCheckedChange = { lockEnabledDraft = it },
                            modifier = Modifier.testTag("diary_modal_biometric_switch")
                        )
                    }
                    AnimatedVisibility(visible = lockEnabledDraft) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PIN atual protegido: ••••",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isCurrentPinVerified) {
                                OutlinedTextField(
                                    value = currentPinInput,
                                    onValueChange = { raw ->
                                        currentPinInput = raw.filter { it.isDigit() }.take(6)
                                        currentPinError = false
                                    },
                                    label = { Text("Confirmar PIN atual para alterar") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                    ),
                                    isError = currentPinError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("diary_modal_verify_current_pin_input")
                                )
                                if (currentPinError) {
                                    Text(
                                        text = "PIN atual incorreto.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (currentPinInput == savedPin) {
                                            isCurrentPinVerified = true
                                            currentPinError = false
                                        } else {
                                            currentPinError = true
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Validar PIN Atual")
                                }
                            } else {
                                OutlinedTextField(
                                    value = newPinDraft,
                                    onValueChange = { raw ->
                                        newPinDraft = raw.filter { it.isDigit() }.take(6)
                                    },
                                    label = { Text("Novo PIN numérico (4 a 6 dígitos)") },
                                    placeholder = { Text("••••") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("diary_modal_pin_input")
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validPin = if (isCurrentPinVerified && newPinDraft.length >= 4) {
                            newPinDraft
                        } else {
                            savedPin
                        }
                        customizationRepo.updateDiaryBiometricLock(
                            enabled = lockEnabledDraft,
                            backupPin = validPin
                        )
                        if (lockEnabledDraft) {
                            isUnlockedInThisVisit = true
                        }
                        showSecurityConfigModal = false
                        triggerFeedback(
                            if (lockEnabledDraft) "🔒 Bloqueio por Biometria / PIN salvo no Diário!"
                            else "🔓 Bloqueio do Diário desativado."
                        )
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityConfigModal = false }) {
                    Text("Cancelar")
                }
            }
        )
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
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

                        if (customizationRepo != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (customizationConfig.diaryBiometricLockEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { isUnlockedInThisVisit = false }
                                            .testTag("btn_lock_diary_now")
                                    ) {
                                        Text(
                                            text = "🔒 Trancar",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showSecurityConfigModal = true }
                                        .testTag("btn_diary_security_settings")
                                ) {
                                    Text(
                                        text = if (customizationConfig.diaryBiometricLockEnabled) "⚙️ PIN/Bio" else "🔓 Biometria",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
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
                                text = if (selectedDayEpoch == todayEpoch) {
                                    "Hoje • ${dayEntries.size} registro(s)"
                                } else {
                                    "${String.format(Locale.US, "%02d/%02d/%04d", selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)} • ${dayEntries.size} registro(s)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedDayEpoch != todayEpoch) {
                                TextButton(onClick = { selectedDayEpoch = todayEpoch }) {
                                    Text("Ir para Hoje", fontWeight = FontWeight.Bold)
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

                    // Quick 7-day history strip anchored so "Hoje" only appears on todayEpoch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val stripEpochs = remember(selectedDayEpoch, todayEpoch) {
                            val base = (-6..0).map { todayEpoch + it }
                            if (selectedDayEpoch in base) {
                                base
                            } else {
                                ((-3..3).map { selectedDayEpoch + it })
                            }
                        }
                        stripEpochs.forEachIndexed { idx, dayEp ->
                            val d = LocalDate.ofEpochDay(dayEp)
                            val countForDay = diaryState.entries.count { it.epochDay == dayEp }
                            val isSelected = dayEp == selectedDayEpoch
                            val isActuallyToday = dayEp == todayEpoch
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .clickable { selectedDayEpoch = dayEp }
                                    .testTag("diary_day_chip_$idx")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isActuallyToday) {
                                            "Hoje"
                                        } else {
                                            String.format(Locale.US, "%02d/%02d", d.dayOfMonth, d.monthValue)
                                        },
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
                        hasProfileApiKey = customizationConfig.geminiApiKey.isNotBlank(),
                        activeLetter = activeLetter,
                        isGenerating = isGeneratingLetter,
                        showHistory = showSavedLettersHistory,
                        onToggleHistory = { showSavedLettersHistory = !showSavedLettersHistory },
                        onGenerateLetter = {
                            scope.launch {
                                isGeneratingLetter = true
                                val generated = diaryRepo.generatePersonalizedSupportLetter(
                                    userName = customizationConfig.profileName.takeIf {
                                        it.isNotBlank() && !it.equals("Você", ignoreCase = true)
                                    } ?: "Membro",
                                    targetEpochDay = selectedDayEpoch,
                                    overrideApiKey = customizationConfig.geminiApiKey
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

        // 3B. CARTA DO DIÁRIO (Cartas autorais para o Bebê, Família ou Futuro — sincronizadas na nuvem)
        item {
            AuraExpandableSectionCard(
                title = "✉️ Carta do Diário (Para o Bebê, Família & Futuro)",
                subtitle = "Escreva e guarde cartas afetivas sincronizadas com segurança na sua família",
                icon = Icons.Default.EditNote,
                accentColor = Color(customizationConfig.diaryColorHex),
                badgeText = "${diaryState.diaryLetters.size} cartas",
                initiallyExpanded = false,
                testTag = "expandable_diary_letters_section"
            ) {
                PersonalDiaryLettersCard(
                    letters = diaryState.diaryLetters,
                    defaultAuthorName = customizationConfig.profileName.takeIf {
                        it.isNotBlank() && !it.equals("Você", ignoreCase = true)
                    } ?: "Família",
                    babyNameHint = customizationConfig.babyName,
                    selectedEpochDay = selectedDayEpoch,
                    onSaveLetter = { title, recipientLabel, bodyText ->
                        diaryRepo.saveDiaryLetter(
                            title = title,
                            content = bodyText,
                            recipientLabel = recipientLabel,
                            authorName = customizationConfig.profileName.takeIf {
                                it.isNotBlank() && !it.equals("Você", ignoreCase = true)
                            } ?: "Família"
                        )
                        triggerFeedback("Carta do Diário salva e sincronizada!")
                    },
                    onDeleteLetter = { id ->
                        diaryRepo.deleteDiaryLetter(id)
                        triggerFeedback("Carta do Diário removida.")
                    }
                )
            }
        }

        // 4. NOVO REGISTRO DE HUMOR E SINTOMAS POR HORA (Menu Expansível - Somente se marcado no questionário)
        if (customizationConfig.hourlyMoodEnabled) {
            item {
                val formattedSelectedTime = String.format(Locale.US, "%02d:%02d", entryHour, entryMinute)
                AuraExpandableSectionCard(
                    title = "📝 Registrar Humor & Sintomas por Hora",
                    subtitle = "Carimbo das $formattedSelectedTime • Humor, tags e relato livre",
                    icon = Icons.Default.EditNote,
                    accentColor = Color(customizationConfig.diaryColorHex),
                    badgeText = formattedSelectedTime,
                    initiallyExpanded = false,
                    testTag = "card_new_diary_entry"
                ) {
                    // Quick Hour Adjuster Chips
                    val presetHours = remember {
                        listOf(8 to 0, 10 to 30, 13 to 0, 16 to 0, 19 to 30, 22 to 0)
                    }
                    val isPresetSelected = presetHours.any { (h, m) -> entryHour == h && entryMinute == m }
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
                            selected = !isPresetSelected,
                            onClick = {
                                val now = LocalTime.now()
                                entryHour = now.hour
                                entryMinute = now.minute
                            },
                            label = {
                                val labelTime = if (!isPresetSelected) {
                                    formattedSelectedTime
                                } else {
                                    LocalTime.now().let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) }
                                }
                                Text("Agora ($labelTime)")
                            }
                        )
                        presetHours.forEach { (h, m) ->
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
                        triggerFeedback("Registro das $formattedSelectedTime salvo na linha do tempo do Diário Privado!")
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
                        text = "Salvar Registro das $formattedSelectedTime",
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
                    onEdit = { editingEntry = entry },
                    onDelete = { deletingEntry = entry }
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
                            triggerFeedback("Tag '$clean' adicionada!")
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

    // Edit Diary Entry Dialog
    val currentEditingEntry = editingEntry
    if (currentEditingEntry != null) {
        var editDateDigits by remember(currentEditingEntry) {
            mutableStateOf(OmniDateMaskUtils.epochDayToDigits(currentEditingEntry.epochDay))
        }
        var editHourText by remember(currentEditingEntry) {
            mutableStateOf(String.format(Locale.US, "%02d", currentEditingEntry.hour))
        }
        var editMinuteText by remember(currentEditingEntry) {
            mutableStateOf(String.format(Locale.US, "%02d", currentEditingEntry.minute))
        }
        var editMoodId by remember(currentEditingEntry) {
            mutableStateOf(currentEditingEntry.moodId)
        }
        val editSymptoms = remember(currentEditingEntry) {
            mutableStateListOf<String>().apply { addAll(currentEditingEntry.symptomTags) }
        }
        var editFreeText by remember(currentEditingEntry) {
            mutableStateOf(currentEditingEntry.freeText)
        }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Editar Registro do Diário Privado", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaskedDateTextField(
                            digitsValue = editDateDigits,
                            onDigitsChange = { editDateDigits = it },
                            label = "Data (DD/MM/AAAA)",
                            modifier = Modifier.weight(1.3f),
                            testTag = "edit_diary_date_masked_input"
                        )
                        OutlinedTextField(
                            value = editHourText,
                            onValueChange = { editHourText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Hora") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                        OutlinedTextField(
                            value = editMinuteText,
                            onValueChange = { editMinuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Min") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Text("Humor:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrivateDiaryCatalog.moodOptions.forEach { mood ->
                            FilterChip(
                                selected = editMoodId == mood.id,
                                onClick = { editMoodId = mood.id },
                                label = { Text("${mood.emoji} ${mood.label}", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Sintomas / Sensações:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allSymptomTags.forEach { tag ->
                            val isSel = editSymptoms.contains(tag)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (isSel) editSymptoms.remove(tag) else editSymptoms.add(tag)
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editFreeText,
                        onValueChange = { editFreeText = it },
                        label = { Text("Relato livre") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_diary_free_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newEpochDay = OmniDateMaskUtils.digitsToEpochDayOrNull(editDateDigits)
                            ?: currentEditingEntry.epochDay
                        val h = (editHourText.toIntOrNull() ?: currentEditingEntry.hour).coerceIn(0, 23)
                        val m = (editMinuteText.toIntOrNull() ?: currentEditingEntry.minute).coerceIn(0, 59)
                        diaryRepo.updateDiaryEntry(
                            entryId = currentEditingEntry.id,
                            epochDay = newEpochDay,
                            hour = h,
                            minute = m,
                            moodId = editMoodId,
                            symptomTags = editSymptoms.toList(),
                            freeText = editFreeText
                        )
                        editingEntry = null
                        triggerFeedback("Registro do diário atualizado com sucesso!")
                    },
                    modifier = Modifier.testTag("confirm_edit_diary_entry_btn")
                ) {
                    Text("Salvar Alterações")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Diary Entry Confirmation Dialog
    val entryToDelete = deletingEntry
    if (entryToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Registro do Diário",
            message = "Deseja excluir o registro das ${entryToDelete.timeFormatted}?",
            onConfirm = {
                diaryRepo.deleteDiaryEntry(entryToDelete.id)
                deletingEntry = null
                triggerFeedback("Registro do diário excluído!")
            },
            onDismiss = { deletingEntry = null }
        )
    }

    if (showApiKeyConfigDialog) {
        LocalGeminiApiKeyDialog(
            diaryRepo = diaryRepo,
            initialProfileKey = customizationConfig.geminiApiKey,
            onDismiss = { showApiKeyConfigDialog = false },
            onSaved = { updatedKey ->
                customizationRepo?.updateGeminiApiKey(updatedKey)
                showApiKeyConfigDialog = false
                triggerFeedback("Configuração da chave Gemini atualizada com segurança!")
            }
        )
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateDiaryTimelineItemCard(
    entry: PrivateDiaryEntry,
    onEdit: () -> Unit,
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

                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_diary_entry_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar entrada",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_diary_entry_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Excluir entrada",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalDiaryLettersCard(
    letters: List<DiaryLetterRecord>,
    defaultAuthorName: String,
    babyNameHint: String,
    selectedEpochDay: Long,
    onSaveLetter: (title: String, recipientLabel: String, bodyText: String) -> Unit,
    onDeleteLetter: (String) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val recipientPresets = remember(babyNameHint) {
        val babyChip = if (babyNameHint.isNotBlank()) "Para ${babyNameHint.trim()}" else "Para o Bebê"
        listOf(babyChip, "Para a Família", "Para o Futuro", "Memória Especial")
    }
    var selectedRecipient by remember(recipientPresets) { mutableStateOf(recipientPresets.first()) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Destinatário da carta:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            recipientPresets.forEach { preset ->
                FilterChip(
                    selected = selectedRecipient == preset,
                    onClick = { selectedRecipient = preset },
                    label = { Text(preset) }
                )
            }
        }
        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Título da Carta") },
            placeholder = { Text("Ex.: O dia em que ouvimos seu coração...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_diary_letter_title")
        )
        OutlinedTextField(
            value = bodyInput,
            onValueChange = { bodyInput = it },
            label = { Text("Mensagem da Carta") },
            placeholder = { Text("Escreva com carinho suas memórias, desejos e sentimentos...") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_diary_letter_body")
        )
        Button(
            onClick = {
                if (bodyInput.isNotBlank()) {
                    onSaveLetter(titleInput.trim(), selectedRecipient, bodyInput.trim())
                    titleInput = ""
                    bodyInput = ""
                }
            },
            enabled = bodyInput.isNotBlank(),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_save_personal_diary_letter")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Salvar Carta do Diário", fontWeight = FontWeight.ExtraBold)
        }

        if (letters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cartas Guardadas (${letters.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            letters.forEach { letter ->
                val datePt = remember(letter.dateIso) {
                    runCatching {
                        LocalDate.parse(letter.dateIso)
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")))
                    }.getOrDefault(letter.dateIso)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = letter.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${letter.recipientLabel} • $datePt • Por ${letter.authorName.ifBlank { defaultAuthorName }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteLetter(letter.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Excluir carta",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = letter.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportLetterGeneratorCard(
    diaryState: PrivateDiaryState,
    hasProfileApiKey: Boolean = false,
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
                            text = if (diaryState.hasLocalApiKeyConfigured || hasProfileApiKey) {
                                "Gemini 2.5 Flash ativa (sincronizada com o Perfil) • Baseada nos seus humores e sintomas"
                            } else {
                                "Analisa seu humor e sintomas recentes (configure a Chave Gemini no Perfil ou toque na chave ao lado)"
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
private fun FamilyMuralSharedSubScreen(
    customizationConfig: AppCustomizationConfig,
    remoteMuralItems: List<com.example.data.FamilyMuralItem>,
    onAddMuralItem: (title: String, category: String, notes: String, pinned: Boolean, isNote: Boolean) -> Unit,
    onToggleMuralItem: (itemId: String) -> Unit,
    onTogglePinMuralItem: (itemId: String) -> Unit,
    onClearCompletedMuralItems: (category: String?) -> Unit,
    onDeleteMuralItem: (itemId: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    val organicButtonShape = com.example.ui.theme.omniOrganicButtonShape()
    val accentColor = Color(customizationConfig.diaryColorHex)

    var localTick by remember { mutableIntStateOf(0) }
    val allItems = remember(remoteMuralItems, localTick) {
        remoteMuralItems.ifEmpty {
            com.example.data.OmniBackupAndMigrationManager.loadFamilyMuralItems(context)
        }
    }

    var selectedCategoryFilter by rememberSaveable { mutableStateOf("TODOS") }
    var newItemTitle by remember { mutableStateOf("") }
    var newItemNotes by remember { mutableStateOf("") }
    var newItemCategory by remember { mutableStateOf("Mercado") }
    var newItemIsPinnedNote by remember { mutableStateOf(false) }

    val categories = listOf("TODOS", "Mercado", "Casa", "Recados", "Farmácia")
    val filteredItems = remember(allItems, selectedCategoryFilter) {
        allItems
            .filter { selectedCategoryFilter == "TODOS" || it.category.equals(selectedCategoryFilter, ignoreCase = true) }
            .sortedWith(compareByDescending<com.example.data.FamilyMuralItem> { it.pinned }.thenBy { it.done })
    }
    val pinnedNotes = remember(filteredItems) { filteredItems.filter { it.pinned || it.isNote } }
    val checklistItems = remember(filteredItems) { filteredItems.filter { !it.isNote } }
    val completedChecklistCount = remember(checklistItems) { checklistItems.count { it.done } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("family_mural_shared_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Quick Creation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(cardRadius)),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📌 Mural de Notas & Listas da Família",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Compartilhado em tempo real no Firestore e no Widget da Tela Inicial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mode Switcher: Item de Lista / Compras vs Nota Fixada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !newItemIsPinnedNote,
                            onClick = {
                                newItemIsPinnedNote = false
                                if (newItemCategory == "Recados") newItemCategory = "Mercado"
                            },
                            label = { Text("🛒 Item de Checklist", maxLines = 1, softWrap = false) },
                            shape = organicButtonShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newItemIsPinnedNote,
                            onClick = {
                                newItemIsPinnedNote = true
                                newItemCategory = "Recados"
                            },
                            label = { Text("📌 Nota / Recado Fixado", maxLines = 1, softWrap = false) },
                            shape = organicButtonShape,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Mercado", "Casa", "Recados", "Farmácia").forEach { cat ->
                            FilterChip(
                                selected = newItemCategory.equals(cat, ignoreCase = true),
                                onClick = { newItemCategory = cat },
                                label = { Text(cat, maxLines = 1, softWrap = false) },
                                shape = organicButtonShape
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newItemTitle,
                        onValueChange = { newItemTitle = it },
                        label = {
                            Text(if (newItemIsPinnedNote) "Título do Recado / Lembrete" else "Adicionar item na lista (ex.: Leite, Pão, Detergente)")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mural_new_title")
                    )

                    if (newItemIsPinnedNote) {
                        OutlinedTextField(
                            value = newItemNotes,
                            onValueChange = { newItemNotes = it },
                            label = { Text("Detalhes da nota (opcional)") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_mural_new_notes")
                        )
                    }

                    Button(
                        onClick = {
                            if (newItemTitle.isNotBlank()) {
                                onAddMuralItem(
                                    newItemTitle.trim(),
                                    newItemCategory,
                                    newItemNotes.trim(),
                                    newItemIsPinnedNote,
                                    newItemIsPinnedNote
                                )
                                newItemTitle = ""
                                newItemNotes = ""
                                localTick++
                            }
                        },
                        shape = organicButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_add_mural_item_confirm")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (newItemIsPinnedNote) "Fixar Nota no Mural da Família" else "Adicionar à Lista Compartilhada",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Category Filter Row + "Limpar marcados" button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(if (cat == "TODOS") "Todos (${allItems.size})" else cat, maxLines = 1, softWrap = false) },
                        shape = organicButtonShape
                    )
                }
                if (completedChecklistCount > 0) {
                    OutlinedButton(
                        onClick = {
                            val catToClear = if (selectedCategoryFilter == "TODOS") null else selectedCategoryFilter
                            onClearCompletedMuralItems(catToClear)
                            localTick++
                        },
                        shape = organicButtonShape,
                        modifier = Modifier.testTag("btn_clear_completed_mural")
                    ) {
                        Text(
                            text = "🧹 Limpar marcados ($completedChecklistCount)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Pinned Notes Section
        if (pinnedNotes.isNotEmpty()) {
            item {
                Text(
                    text = "📌 NOTAS & LEMBRETES FIXADOS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
            items(pinnedNotes, key = { "note_${it.id}" }) { note ->
                val memberColor = Color(note.addedByColorHex.toInt())
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, memberColor.copy(alpha = 0.55f), RoundedCornerShape(cardRadius)),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = memberColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = organicButtonShape,
                                    color = memberColor.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(memberColor)
                                        )
                                        Text(
                                            text = note.addedBy,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = memberColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteMuralItem(note.id)
                                        localTick++
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Excluir nota",
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                        if (note.notes.isNotBlank()) {
                            Text(
                                text = note.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Interactive Checklist Section (Lista de Compras / Mercado / Casa)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛒 CHECKLISTS COMPARTILHADOS (${checklistItems.count { !it.done }} pendentes)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(checklistItems, key = { "chk_${it.id}" }) { item ->
            val creatorColor = Color(item.addedByColorHex.toInt())
            val checkerColor = if (item.checkedByColorHex != 0L) Color(item.checkedByColorHex.toInt()) else creatorColor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (item.done) MaterialTheme.colorScheme.outlineVariant else creatorColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(cardRadius)
                    )
                    .clickable {
                        onToggleMuralItem(item.id)
                        localTick++
                    }
                    .testTag("mural_checklist_item_${item.id}"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (item.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Marcar item",
                            tint = if (item.done) IncomeEmerald else creatorColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(creatorColor)
                                )
                                Text(
                                    text = "por ${item.addedBy}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.done && item.checkedBy.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(checkerColor)
                                    )
                                    Text(
                                        text = "✓ ${item.checkedBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                onTogglePinMuralItem(item.id)
                                localTick++
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Fixar item",
                                tint = if (item.pinned) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                onDeleteMuralItem(item.id)
                                localTick++
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remover item",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
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
    initialProfileKey: String = "",
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var keyInput by remember(initialProfileKey) {
        mutableStateOf(initialProfileKey.ifBlank { diaryRepo.getConfiguredGeminiApiKey() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chave da API Gemini (Perfil & Diário)",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Esta chave Gemini 2.5 Flash é compartilhada entre a Central de Documentos Médicos e a Carta de Apoio via IA. Caso fique em branco ou offline, o Diário usa o gerador empático local automaticamente.",
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
                    val trimmed = keyInput.trim()
                    diaryRepo.saveLocalGeminiApiKey(trimmed)
                    onSaved(trimmed)
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

@Composable
private fun PrivateDiaryBiometricLockGate(
    backupPin: String,
    accentColor: Color,
    onUnlocked: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardRadius = LocalOrganicCardRadius.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val deviceCredentialLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlocked()
        }
    }

    val launchNativeBiometricOrCredential: () -> Unit = remember(context) {
        {
            var launchedBiometric = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                try {
                    val promptBuilder = android.hardware.biometrics.BiometricPrompt.Builder(context)
                        .setTitle("Desbloquear Diário Privado")
                        .setSubtitle("Use sua biometria ou PIN do aparelho")
                        .setDescription("Seus registros do Diário Privado estão protegidos.")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        promptBuilder.setAllowedAuthenticators(
                            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        promptBuilder.setDeviceCredentialAllowed(true)
                    } else {
                        promptBuilder.setNegativeButton(
                            "Usar PIN",
                            androidx.core.content.ContextCompat.getMainExecutor(context)
                        ) { _, _ -> }
                    }
                    val prompt = promptBuilder.build()
                    val cancellationSignal = android.os.CancellationSignal()
                    prompt.authenticate(
                        cancellationSignal,
                        androidx.core.content.ContextCompat.getMainExecutor(context),
                        object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?
                            ) {
                                onUnlocked()
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence?
                            ) {
                                // Fallback to KeyguardManager or PIN field if hardware biometric is unavailable
                                try {
                                    val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                                    @Suppress("DEPRECATION")
                                    val intent = km?.createConfirmDeviceCredentialIntent(
                                        "Desbloquear Diário Privado",
                                        "Confirme o bloqueio de tela do aparelho"
                                    )
                                    if (intent != null) {
                                        deviceCredentialLauncher.launch(intent)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    )
                    launchedBiometric = true
                } catch (_: Exception) {
                    launchedBiometric = false
                }
            }
            if (!launchedBiometric) {
                try {
                    val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                    @Suppress("DEPRECATION")
                    val intent = km?.createConfirmDeviceCredentialIntent(
                        "Desbloquear Diário Privado",
                        "Confirme sua identidade para acessar o Diário"
                    )
                    if (intent != null) {
                        deviceCredentialLauncher.launch(intent)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        launchNativeBiometricOrCredential()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("diary_biometric_lock_gate"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Diário Protegido",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Diário Privado Protegido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Use a Biometria / PIN do aparelho ou digite o seu PIN do Diário para acessar seus registros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = { launchNativeBiometricOrCredential() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_unlock_diary_biometric")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Desbloquear com Biometria / PIN do Aparelho",
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(6)
                        enteredPin = digits
                        errorMessage = null
                        if (digits == backupPin) {
                            onUnlocked()
                        }
                    },
                    label = { Text("Ou digite o PIN do Diário") },
                    placeholder = { Text("PIN numérico") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_unlock_diary_pin")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (enteredPin == backupPin) {
                            onUnlocked()
                        } else {
                            errorMessage = "PIN incorreto. Tente novamente ou use a Biometria acima."
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_unlock_diary_pin_confirm")
                ) {
                    Text("Entrar com PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

