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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.PrivacyScope
import com.example.data.PrivateDiaryCatalog
import com.example.data.VoiceCommandInterpreter
import com.example.data.VoiceIntentType
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun GlobalVoiceMicFab(
    onOpenVoiceCommandSheet: (startListeningImmediately: Boolean) -> Unit,
    onPermissionDeniedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onOpenVoiceCommandSheet(true)
        } else {
            onPermissionDeniedMessage(
                "Permissão de microfone não concedida. Você ainda pode usar ou testar o comando no painel."
            )
            onOpenVoiceCommandSheet(false)
        }
    }

    FloatingActionButton(
        onClick = {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onOpenVoiceCommandSheet(true)
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        modifier = modifier.testTag("global_voice_mic_fab")
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Comando de Voz Global (Finanças, Agenda e Diário)",
            modifier = Modifier.size(26.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalVoiceCommandBottomSheet(
    referenceEpochDay: Long,
    startListeningImmediately: Boolean,
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amountBrl: Double,
        category: String,
        privacyScope: String
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
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isListening by remember { mutableStateOf(false) }
    var recognizerStatusText by remember {
        mutableStateOf("Toque no microfone para falar em português (pt-BR) ou escolha um exemplo rápido.")
    }
    var transcriptText by remember { mutableStateOf("") }

    // Parsed / Editable Confirmation State
    var selectedIntent by remember { mutableStateOf(VoiceIntentType.EXPENSE) }
    var privacyScope by remember { mutableStateOf(PrivacyScope.PERSONAL) }

    // Expense pre-filled states
    var expenseTitle by remember { mutableStateOf("Farmácia") }
    var expenseAmountInput by remember { mutableStateOf("45.00") }
    var expenseCategory by remember { mutableStateOf("Farmácia") }

    // Task pre-filled states
    var taskTitle by remember { mutableStateOf("Pagar a conta de luz") }
    var taskEpochDay by remember { mutableLongStateOf(referenceEpochDay + 1L) }
    var taskHour by remember { mutableIntStateOf(10) }
    var taskMinute by remember { mutableIntStateOf(0) }
    var taskCategory by remember { mutableStateOf("Contas") }
    var taskIsEssential by remember { mutableStateOf(true) }

    // Diary pre-filled states
    var diaryMoodId by remember { mutableStateOf("FOCUSED") }
    val diarySymptoms = remember { mutableStateListOf("Sem dores", "Boa disposição") }
    var diaryFreeText by remember { mutableStateOf("Hoje acordei bem disposto, sem dor de cabeça") }

    fun applyParsedTranscript(raw: String) {
        transcriptText = raw
        if (raw.isBlank()) return
        val parsed = VoiceCommandInterpreter.parseTranscript(raw, referenceEpochDay)
        selectedIntent = parsed.detectedIntent
        privacyScope = parsed.privacyScope

        expenseTitle = parsed.expenseTitle
        expenseAmountInput = if (parsed.expenseAmountBrl > 0.0) {
            String.format(Locale.US, "%.2f", parsed.expenseAmountBrl)
        } else {
            ""
        }
        expenseCategory = parsed.expenseCategory

        taskTitle = parsed.taskTitle
        taskEpochDay = parsed.taskEpochDay
        taskHour = parsed.taskHour
        taskMinute = parsed.taskMinute
        taskCategory = parsed.taskCategory
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
                recognizerStatusText = "🎙️ Microfone pronto! Pode falar (Despesa, Tarefa ou Diário)..."
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

            // Quick voice preset chips (great for testing all 3 classifications in 1 tap)
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
                placeholder = { Text("Ex.: Gastei 45 reais na farmácia...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_voice_transcript")
            )

            HorizontalDivider()

            // Intent Type Selector (Auto-detected, user can adjust with 1 tap)
            Text(
                text = "Classificação Automática (toque se quiser alterar o destino):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceIntentType.entries.forEach { type ->
                    val isSelected = selectedIntent == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedIntent = type },
                        label = {
                            Text(
                                text = "${type.emoji} ${type.label}",
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_intent_chip_${type.name}")
                    )
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
                                label = { Text("Descrição") },
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
                                listOf("Farmácia", "Alimentação", "Exames", "Consultas", "Moradia", "Transporte", "Lazer").forEach { cat ->
                                    FilterChip(
                                        selected = expenseCategory.equals(cat, ignoreCase = true),
                                        onClick = { expenseCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                        }

                        VoiceIntentType.TASK -> {
                            val todayEpoch = remember { LocalDate.now().toEpochDay() }
                            Text(
                                text = "📅 Revisar Tarefa / Lembrete na Agenda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                label = { Text("Título da Tarefa") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = taskEpochDay == todayEpoch,
                                    onClick = { taskEpochDay = todayEpoch },
                                    label = { Text("Hoje") }
                                )
                                FilterChip(
                                    selected = taskEpochDay == todayEpoch + 1L,
                                    onClick = { taskEpochDay = todayEpoch + 1L },
                                    label = { Text("Amanhã") }
                                )
                                FilterChip(
                                    selected = taskHour == 10 && taskMinute == 0,
                                    onClick = {
                                        taskHour = 10
                                        taskMinute = 0
                                    },
                                    label = { Text("10:00") }
                                )
                                FilterChip(
                                    selected = taskHour == 14 && taskMinute == 0,
                                    onClick = {
                                        taskHour = 14
                                        taskMinute = 0
                                    },
                                    label = { Text("14:00") }
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

                    // Scope selector for Expense & Task
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
                        when (selectedIntent) {
                            VoiceIntentType.EXPENSE -> {
                                val amount = expenseAmountInput.replace(",", ".").toDoubleOrNull() ?: 45.0
                                onSaveExpense(
                                    expenseTitle.ifBlank { "Despesa por Voz" },
                                    amount.coerceAtLeast(0.01),
                                    expenseCategory.ifBlank { "Outros" },
                                    privacyScope
                                )
                            }

                            VoiceIntentType.TASK -> {
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
