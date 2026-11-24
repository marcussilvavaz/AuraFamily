package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BankAccount
import com.example.data.BankAccountCatalog
import com.example.data.PrivacyScope
import com.example.data.PrivateDiaryCatalog
import com.example.data.VoiceCommandInterpreter
import com.example.data.VoiceIntentType
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Único Botão Flutuante Inteligente (Toque curto = Criar; Segurar = Comando de Voz).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartOmniFloatingActionButton(
    onShortTapCreate: () -> Unit = {},
    onOpenVoiceCommandSheet: (startListeningImmediately: Boolean) -> Unit = {},
    onQuickAddClick: () -> Unit = onShortTapCreate,
    onVoiceLongPress: (startListeningImmediately: Boolean) -> Unit = onOpenVoiceCommandSheet,
    onPermissionDeniedMessage: (String) -> Unit = {},
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onVoiceLongPress(true)
        } else {
            onPermissionDeniedMessage(
                "Permissão de microfone não concedida. Você ainda pode usar o comando de voz digitando no painel."
            )
            onVoiceLongPress(false)
        }
    }

    val triggerVoice = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onVoiceLongPress(true)
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        shape = CircleShape,
        color = accentColor,
        contentColor = Color(0xFF0F172A),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp,
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onQuickAddClick,
                onLongClick = triggerVoice
            )
            .testTag("global_voice_mic_fab")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Toque para Criar ou Segure para Comando de Voz",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Segure para Voz",
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
fun GlobalVoiceMicFab(
    onOpenVoiceCommandSheet: (startListeningImmediately: Boolean) -> Unit,
    onPermissionDeniedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SmartOmniFloatingActionButton(
        onShortTapCreate = { onOpenVoiceCommandSheet(true) },
        onOpenVoiceCommandSheet = onOpenVoiceCommandSheet,
        onPermissionDeniedMessage = onPermissionDeniedMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalVoiceCommandBottomSheet(
    referenceEpochDay: Long,
    startListeningImmediately: Boolean,
    bankAccounts: List<BankAccount> = emptyList(),
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amountBrl: Double,
        category: String,
        privacyScope: String,
        accountId: String,
        accountName: String
    ) -> Unit,
    onSaveIncome: (
        title: String,
        amountBrl: Double,
        category: String,
        privacyScope: String,
        accountId: String,
        accountName: String,
        isRecurring: Boolean
    ) -> Unit,
    onSaveTask: (
        title: String,
        epochDay: Long,
        hour: Int,
        minute: Int,
        category: String,
        isEssential: Boolean,
        privacyScope: String
    ) -> Unit,
    onSaveDiaryEntry: (
        epochDay: Long,
        hour: Int,
        minute: Int,
        moodId: String,
        symptomTags: List<String>,
        freeText: String
    ) -> Unit,
    onSaveMuralItems: (items: List<String>, category: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableAccounts = remember(bankAccounts) {
        bankAccounts.ifEmpty { BankAccountCatalog.defaultAccounts() }
    }

    var isListening by remember { mutableStateOf(false) }
    var recognizerStatusText by remember {
        mutableStateOf("Toque no microfone para falar em português (pt-BR) ou escolha um exemplo rápido.")
    }
    var transcriptText by remember { mutableStateOf("") }

    // Parsed / Editable Confirmation State
    var selectedIntent by remember { mutableStateOf(VoiceIntentType.EXPENSE) }
    var privacyScope by remember { mutableStateOf(PrivacyScope.PERSONAL) }
    var selectedAccountId by remember(availableAccounts) {
        mutableStateOf(availableAccounts.firstOrNull()?.id ?: "")
    }

    // Expense pre-filled states
    var expenseTitle by remember { mutableStateOf("Farmácia") }
    var expenseAmountInput by remember { mutableStateOf("45.00") }
    var expenseCategory by remember { mutableStateOf("Farmácia") }

    // Income pre-filled states
    var incomeTitle by remember { mutableStateOf("Salário") }
    var incomeAmountInput by remember { mutableStateOf("1500.00") }
    var incomeCategory by remember { mutableStateOf("Salário") }
    var incomeIsRecurring by remember { mutableStateOf(false) }

    // Task pre-filled states
    var taskTitle by remember { mutableStateOf("Pagar a conta de luz") }
    var taskEpochDay by remember { mutableLongStateOf(referenceEpochDay + 1L) }
    var taskHour by remember { mutableIntStateOf(10) }
    var taskMinute by remember { mutableIntStateOf(0) }
    var taskCategory by remember { mutableStateOf("Contas") }
    var taskIsEssential by remember { mutableStateOf(true) }
    var sendTaskToFamilyMural by remember { mutableStateOf(false) }

    // Diary pre-filled states
    var diaryMoodId by remember { mutableStateOf("FOCUSED") }
    val diarySymptoms = remember { mutableStateListOf("Sem dores", "Boa disposição") }
    var diaryFreeText by remember { mutableStateOf("Hoje acordei bem disposto, sem dor de cabeça") }

    fun applyParsedTranscript(raw: String) {
        transcriptText = raw
        if (raw.isBlank()) return
        val lower = raw.lowercase()
        val isMuralShoppingCommand = lower.contains("lista de compras") ||
            lower.contains("mural da família") ||
            lower.contains("no mural") ||
            lower.contains("na lista do mercado") ||
            (lower.startsWith("adicionar ") && (lower.contains("compras") || lower.contains("mercado") || lower.contains("lista")))

        val parsed = VoiceCommandInterpreter.parseTranscript(raw, referenceEpochDay)
        selectedIntent = if (isMuralShoppingCommand) VoiceIntentType.TASK else parsed.detectedIntent
        sendTaskToFamilyMural = isMuralShoppingCommand
        privacyScope = if (isMuralShoppingCommand) PrivacyScope.FAMILY else parsed.privacyScope

        expenseTitle = parsed.expenseTitle
        expenseAmountInput = if (parsed.expenseAmountBrl > 0.0) {
            String.format(Locale.US, "%.2f", parsed.expenseAmountBrl)
        } else {
            ""
        }
        expenseCategory = parsed.expenseCategory

        incomeTitle = parsed.incomeTitle
        incomeAmountInput = if (parsed.incomeAmountBrl > 0.0) {
            String.format(Locale.US, "%.2f", parsed.incomeAmountBrl)
        } else {
            ""
        }
        incomeCategory = parsed.incomeCategory
        incomeIsRecurring = parsed.incomeIsRecurring

        if (parsed.detectedBankHint.isNotBlank()) {
            val matchedAcc = availableAccounts.firstOrNull { acc ->
                acc.name.contains(parsed.detectedBankHint, ignoreCase = true) ||
                    acc.institution.contains(parsed.detectedBankHint, ignoreCase = true)
            }
            if (matchedAcc != null) {
                selectedAccountId = matchedAcc.id
            }
        }

        taskTitle = if (isMuralShoppingCommand) {
            raw.replace(Regex("(?i)(adicionar|colocar|incluir|na lista de compras|na lista do mercado|no mural da família|no mural|na lista)"), "")
                .trim()
                .ifBlank { parsed.taskTitle }
        } else {
            parsed.taskTitle
        }
        taskEpochDay = parsed.taskEpochDay
        taskHour = parsed.taskHour
        taskMinute = parsed.taskMinute
        taskCategory = if (isMuralShoppingCommand) "Mercado" else parsed.taskCategory
        taskIsEssential = parsed.taskIsEssential

        diaryMoodId = parsed.diaryMoodId
        diarySymptoms.clear()
        diarySymptoms.addAll(parsed.diarySymptoms)
        diaryFreeText = parsed.diaryFreeText
    }

    val speechRecognizer = remember(context) {
        runCatching {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        }.getOrNull()
    }

    fun startSpeechListening() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) {
            recognizerStatusText = "Permissão de áudio pendente. Digite ou toque em um comando rápido abaixo."
            return
        }

        val recognizer = speechRecognizer
        if (recognizer == null) {
            recognizerStatusText = "Serviço de voz nativo indisponível neste dispositivo/emulador. Digite ou use os exemplos abaixo."
            return
        }

        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            isListening = true
            recognizerStatusText = "🎙️ Ouvindo em português (pt-BR)... Fale seu comando agora."
            recognizer.startListening(intent)
        }.onFailure {
            isListening = false
            recognizerStatusText = "Não foi possível iniciar a captura de áudio. Use o campo abaixo."
        }
    }

    fun stopSpeechListening() {
        runCatching {
            speechRecognizer?.stopListening()
        }
        isListening = false
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                recognizerStatusText = "🎙️ Microfone pronto! Pode falar (Despesa, Receita, Tarefa ou Diário)..."
            }

            override fun onBeginningOfSpeech() {
                isListening = true
                recognizerStatusText = "🎙️ Capturando sua voz..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                recognizerStatusText = "Processando comando de voz..."
            }

            override fun onError(error: Int) {
                isListening = false
                recognizerStatusText = "Microfone aguardando. Toque para falar novamente ou revise os campos abaixo."
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    applyParsedTranscript(best)
                    recognizerStatusText = "✅ Comando interpretado! Revise os campos pré-preenchidos e toque em Salvar."
                } else {
                    recognizerStatusText = "Nenhuma fala detectada. Tente novamente ou escolha um exemplo."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    applyParsedTranscript(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        if (startListeningImmediately) {
            startSpeechListening()
        }

        onDispose {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopSpeechListening()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Live Microphone Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎙️ Comando de Voz Inteligente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = recognizerStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isListening) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                val micBg by animateColorAsState(
                    targetValue = if (isListening) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    label = "micPulseColor"
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(micBg)
                        .clickable {
                            if (isListening) {
                                stopSpeechListening()
                            } else {
                                startSpeechListening()
                            }
                        }
                        .testTag("btn_toggle_voice_listening"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Parar escuta" else "Falar comando",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Quick voice preset chips (great for testing all 4 classifications in 1 tap)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Exemplos rápidos (toque para preencher e testar a classificação):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "💸 \"Gastei 45 reais na farmácia\"" to "Gastei 45 reais na farmácia",
                        "💰 \"Recebi 1500 reais de salário no Nubank\"" to "Recebi 1500 reais de salário no Nubank",
                        "🛒 \"Adicionar leite e pão na lista de compras\"" to "Adicionar leite e pão na lista de compras",
                        "📅 \"Lembrar de pagar a conta de luz amanhã às 10h\"" to "Lembrar de pagar a conta de luz amanhã às 10h",
                        "📔 \"Hoje acordei bem disposto, sem dor de cabeça\"" to "Hoje acordei bem disposto, sem dor de cabeça"
                    ).forEachIndexed { idx, (label, samplePhrase) ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable {
                                    applyParsedTranscript(samplePhrase)
                                    recognizerStatusText = "✅ Exemplo aplicado e classificado automaticamente!"
                                }
                                .testTag("voice_sample_chip_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Transcript field (editable in real time)
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { applyParsedTranscript(it) },
                label = { Text("Fala capturada / Comando (pt-BR)") },
                placeholder = { Text("Ex.: Recebi 1500 reais de salário ou Gastei 45 reais...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_voice_transcript")
            )

            HorizontalDivider()

            // Intent Type Selector (4 short, clean destination chips that never truncate)
            Text(
                text = "Classificação Automática (toque se quiser alterar o destino):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceIntentType.entries.forEach { type ->
                    val isSelected = selectedIntent == type
                    val chipBg = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                    val chipBorder = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = chipBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedIntent = type }
                            .testTag("voice_intent_chip_${type.name}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = type.emoji,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Dynamic Pre-filled Form based on detected/selected Intent
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedIntent) {
                        VoiceIntentType.EXPENSE -> {
                            Text(
                                text = "💸 Revisar Nova Despesa Financeira",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = expenseTitle,
                                onValueChange = { expenseTitle = it },
                                label = { Text("Descrição da Despesa") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = expenseAmountInput,
                                    onValueChange = { expenseAmountInput = it },
                                    label = { Text("Valor (R$)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = expenseCategory,
                                    onValueChange = { expenseCategory = it },
                                    label = { Text("Categoria") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Farmácia", "Alimentação", "Exames", "Consultas", "Moradia", "Transporte", "Lazer", "Outros").forEach { cat ->
                                    FilterChip(
                                        selected = expenseCategory.equals(cat, ignoreCase = true),
                                        onClick = { expenseCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }

                            Text(
                                text = "Conta Bancária de saída:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableAccounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountId == acc.id,
                                        onClick = { selectedAccountId = acc.id },
                                        label = { Text("${acc.iconEmoji} ${acc.name}") }
                                    )
                                }
                            }
                        }

                        VoiceIntentType.INCOME -> {
                            Text(
                                text = "💰 Revisar Nova Receita / Entrada Financeira",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0E7C5B)
                            )
                            OutlinedTextField(
                                value = incomeTitle,
                                onValueChange = { incomeTitle = it },
                                label = { Text("Descrição da Receita") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_income_title_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = incomeAmountInput,
                                    onValueChange = { incomeAmountInput = it },
                                    label = { Text("Valor Recebido (R$)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("voice_income_amount_input")
                                )
                                OutlinedTextField(
                                    value = incomeCategory,
                                    onValueChange = { incomeCategory = it },
                                    label = { Text("Categoria") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text(
                                text = "Categoria de Receita:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Salário", "Extra", "Vendas", "Reembolso", "Outros").forEach { cat ->
                                    FilterChip(
                                        selected = incomeCategory.equals(cat, ignoreCase = true),
                                        onClick = { incomeCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }

                            Text(
                                text = "Conta Bancária de destino (onde o dinheiro entrou):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableAccounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountId == acc.id,
                                        onClick = { selectedAccountId = acc.id },
                                        label = { Text("${acc.iconEmoji} ${acc.name}") }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Receita Fixa / Recorrente mensal",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = incomeIsRecurring,
                                    onCheckedChange = { incomeIsRecurring = it }
                                )
                            }
                        }

                        VoiceIntentType.TASK -> {
                            val todayEpoch = remember { LocalDate.now().toEpochDay() }
                            Text(
                                text = if (sendTaskToFamilyMural) "🛒 Adicionar ao Mural da Família / Lista de Compras" else "📅 Revisar Tarefa / Lembrete na Agenda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Enviar para o Mural / Lista de Compras",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = sendTaskToFamilyMural,
                                    onCheckedChange = { sendTaskToFamilyMural = it }
                                )
                            }
                            OutlinedTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                label = { Text(if (sendTaskToFamilyMural) "Itens (separe por 'e' ou vírgula)" else "Título da Tarefa") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!sendTaskToFamilyMural) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = taskEpochDay == todayEpoch,
                                        onClick = { taskEpochDay = todayEpoch },
                                        label = { Text("Hoje", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = taskEpochDay == todayEpoch + 1L,
                                        onClick = { taskEpochDay = todayEpoch + 1L },
                                        label = { Text("Amanhã", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = taskHour == 10 && taskMinute == 0,
                                        onClick = {
                                            taskHour = 10
                                            taskMinute = 0
                                        },
                                        label = { Text("10:00", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = taskHour == 14 && taskMinute == 0,
                                        onClick = {
                                            taskHour = 14
                                            taskMinute = 0
                                        },
                                        label = { Text("14:00", maxLines = 1, softWrap = false) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Horário: ${String.format(Locale.US, "%02d:%02d", taskHour, taskMinute)} • Categoria: $taskCategory",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Essencial", style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = taskIsEssential,
                                            onCheckedChange = { taskIsEssential = it }
                                        )
                                    }
                                }
                            }
                        }

                        VoiceIntentType.DIARY -> {
                            Text(
                                text = "📔 Revisar Entrada no Diário Privado",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Humor detectado:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrivateDiaryCatalog.moodOptions.forEach { mood ->
                                    val isSel = diaryMoodId == mood.id
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { diaryMoodId = mood.id },
                                        label = { Text("${mood.emoji} ${mood.label}") }
                                    )
                                }
                            }
                            Text(
                                text = "Sintomas / Sensações detectadas:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrivateDiaryCatalog.defaultSymptomTags.take(7).forEach { sym ->
                                    val isSel = diarySymptoms.contains(sym)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            if (isSel) diarySymptoms.remove(sym) else diarySymptoms.add(sym)
                                        },
                                        label = { Text(sym) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = diaryFreeText,
                                onValueChange = { diaryFreeText = it },
                                label = { Text("Relato no Diário") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Scope selector for Expense, Income & Task
                    if (selectedIntent != VoiceIntentType.DIARY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Escopo:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = privacyScope == PrivacyScope.PERSONAL,
                                onClick = { privacyScope = PrivacyScope.PERSONAL },
                                label = { Text("👤 Individual") }
                            )
                            FilterChip(
                                selected = privacyScope == PrivacyScope.FAMILY,
                                onClick = { privacyScope = PrivacyScope.FAMILY },
                                label = { Text("👨‍👩‍👧 Familiar") }
                            )
                        }
                    }
                }
            }

            // Confirm & Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        stopSpeechListening()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        stopSpeechListening()
                        val chosenAcc = availableAccounts.firstOrNull { it.id == selectedAccountId }
                            ?: availableAccounts.firstOrNull()
                        val accId = chosenAcc?.id.orEmpty()
                        val accName = chosenAcc?.name ?: "Conta Principal"

                        when (selectedIntent) {
                            VoiceIntentType.EXPENSE -> {
                                val amount = expenseAmountInput.replace(",", ".").toDoubleOrNull() ?: 45.0
                                onSaveExpense(
                                    expenseTitle.ifBlank { "Despesa por Voz" },
                                    amount.coerceAtLeast(0.01),
                                    expenseCategory.ifBlank { "Outros" },
                                    privacyScope,
                                    accId,
                                    accName
                                )
                            }

                            VoiceIntentType.INCOME -> {
                                val amount = incomeAmountInput.replace(",", ".").toDoubleOrNull() ?: 100.0
                                onSaveIncome(
                                    incomeTitle.ifBlank { "Receita por Voz" },
                                    amount.coerceAtLeast(0.01),
                                    incomeCategory.ifBlank { "Salário" },
                                    privacyScope,
                                    accId,
                                    accName,
                                    incomeIsRecurring
                                )
                            }

                            VoiceIntentType.TASK -> {
                                if (sendTaskToFamilyMural) {
                                    val splitItems = taskTitle
                                        .split(Regex("(?i)(,|\\s+e\\s+)"))
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .ifEmpty { listOf(taskTitle.ifBlank { "Item de Compra" }) }
                                    onSaveMuralItems(splitItems, taskCategory.ifBlank { "Mercado" })
                                } else {
                                    onSaveTask(
                                        taskTitle.ifBlank { "Lembrete por Voz" },
                                        taskEpochDay,
                                        taskHour,
                                        taskMinute,
                                        taskCategory.ifBlank { "Família" },
                                        taskIsEssential,
                                        privacyScope
                                    )
                                }
                            }

                            VoiceIntentType.DIARY -> {
                                val now = LocalTime.now()
                                onSaveDiaryEntry(
                                    LocalDate.now().toEpochDay(),
                                    now.hour,
                                    now.minute,
                                    diaryMoodId,
                                    diarySymptoms.toList(),
                                    diaryFreeText.ifBlank { transcriptText }
                                )
                            }
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("btn_confirm_save_voice_command")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirmar e Salvar",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
