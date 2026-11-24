package com.example.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

// ============================================================================
// 0. OMNI SPLASH SCREEN (TELA DE ABERTURA ESCURA COM SÍMBOLO E DETALHE AZUL NO "O")
// ============================================================================

@Composable
fun OmniSplashScreen(
    onFinished: () -> Unit = {},
    onSplashFinished: () -> Unit = onFinished
) {
    var startAnim by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "omniSplashAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.92f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "omniSplashScale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1500L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF25272C),
                        Color(0xFF191A1E)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSplashFinished() }
            .testTag("omni_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
                .padding(horizontal = 32.dp)
        ) {
            // Símbolo minimalista acima
            Image(
                painter = painterResource(id = R.drawable.ic_omni_symbol),
                contentDescription = "Símbolo Omni",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(138.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Texto "Omni" em branco com o detalhe azul (#2962FF) no topo do "O"
            Box(contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.TopCenter) {
                        Text(
                            text = "O",
                            color = Color.White,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp
                        )
                        // Detalhe azul no arco superior do "O"
                        Canvas(
                            modifier = Modifier
                                .padding(top = 13.dp)
                                .size(13.dp)
                        ) {
                            drawCircle(
                                color = Color(0xFF1B1C20),
                                radius = size.minDimension / 2f + 2.5f
                            )
                            drawCircle(
                                color = Color(0xFF2962FF),
                                radius = size.minDimension / 2f
                            )
                        }
                    }
                    Text(
                        text = "mni",
                        color = Color.White,
                        fontSize = 62.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtítulo TAREFAS • ROTINA • FINANÇAS • SAÚDE
            Text(
                text = "TAREFAS • ROTINA • FINANÇAS • SAÚDE",
                color = Color(0xFFB5BAC4),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// 1. MÁSCARA AUTOMÁTICA DE DATA (DD/MM/AAAA) COM TECLADO NUMÉRICO
// ============================================================================

class DdMmYyyyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.filter { it.isDigit() }.take(8)
        val out = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if ((i == 1 || i == 3) && i != trimmed.lastIndex) {
                    append('/')
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, trimmed.length)
                return when {
                    clamped <= 2 -> clamped
                    clamped <= 4 -> clamped + 1
                    else -> (clamped + 2).coerceAtMost(out.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, out.length)
                return when {
                    clamped <= 2 -> clamped
                    clamped <= 5 -> clamped - 1
                    else -> (clamped - 2).coerceAtMost(trimmed.length)
                }
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

object OmniDateMaskUtils {
    /**
     * Mantém apenas até 8 dígitos numéricos (DDMMAAAA).
     */
    fun filterDigits(input: String): String {
        return input.filter { it.isDigit() }.take(8)
    }

    /**
     * Converte uma string "DD/MM/AAAA" ou "AAAA-MM-DD" ou dígitos puros para 8 dígitos "DDMMAAAA".
     */
    fun normalizeToDigits(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val parts = trimmed.split("-")
            return "${parts[2]}${parts[1]}${parts[0]}"
        }
        return filterDigits(trimmed)
    }

    /**
     * Formata dígitos "22112026" em "22/11/2026".
     */
    fun formatDigitsToDdMmYyyy(rawDigitsOrDate: String): String {
        val digits = normalizeToDigits(rawDigitsOrDate)
        val sb = StringBuilder()
        for (i in digits.indices) {
            sb.append(digits[i])
            if ((i == 1 || i == 3) && i != digits.lastIndex) {
                sb.append('/')
            }
        }
        return sb.toString()
    }

    fun epochDayToDigits(epochDay: Long): String {
        return try {
            val d = LocalDate.ofEpochDay(epochDay)
            String.format(Locale.US, "%02d%02d%04d", d.dayOfMonth, d.monthValue, d.year)
        } catch (_: Exception) {
            val now = LocalDate.now()
            String.format(Locale.US, "%02d%02d%04d", now.dayOfMonth, now.monthValue, now.year)
        }
    }

    fun epochDayToDdMmYyyy(epochDay: Long): String {
        return formatDigitsToDdMmYyyy(epochDayToDigits(epochDay))
    }

    fun epochDayToBrDate(epochDay: Long): String {
        return epochDayToDdMmYyyy(epochDay)
    }

    /**
     * Converte "22112026" ou "22/11/2026" ou "2026-11-22" para epochDay, ou null se incompleto/inválido.
     */
    fun parseToEpochDay(rawDigitsOrFormatted: String): Long? {
        val digits = normalizeToDigits(rawDigitsOrFormatted)
        if (digits.length != 8) return null
        val day = digits.substring(0, 2).toIntOrNull() ?: return null
        val month = digits.substring(2, 4).toIntOrNull() ?: return null
        val year = digits.substring(4, 8).toIntOrNull() ?: return null
        if (year !in 1900..2150 || month !in 1..12 || day !in 1..31) return null
        return try {
            LocalDate.of(year, month, day).toEpochDay()
        } catch (_: Exception) {
            null
        }
    }

    fun digitsToEpochDayOrNull(rawDigitsOrFormatted: String): Long? {
        return parseToEpochDay(rawDigitsOrFormatted)
    }

    fun parseBrDateToEpochDay(rawDigitsOrFormatted: String): Long? {
        return parseToEpochDay(rawDigitsOrFormatted)
    }

    /**
     * Extrai "AAAA-MM" de uma data mascarada ou dígitos "DDMMAAAA", com fallback para o ano/mês atual.
     */
    fun parseToYearMonth(rawDigitsOrFormatted: String, fallbackYearMonth: String): String {
        val ep = parseToEpochDay(rawDigitsOrFormatted) ?: return fallbackYearMonth
        val d = LocalDate.ofEpochDay(ep)
        return String.format(Locale.US, "%04d-%02d", d.year, d.monthValue)
    }
}

/**
 * Campo de data padrão Omni com máscara automática DD/MM/AAAA e teclado exclusivamente numérico.
 */
@Composable
fun MaskedDateTextField(
    value: String,
    onValueChange: (formattedDdMmYyyy: String, cleanDigits: String) -> Unit,
    label: String = "Data (DD/MM/AAAA)",
    placeholder: String = "DD/MM/AAAA",
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    testTag: String = "masked_date_input",
    modifier: Modifier = Modifier
) {
    val cleanDigits = remember(value) { OmniDateMaskUtils.normalizeToDigits(value) }
    val visualTransformation = remember { DdMmYyyyVisualTransformation() }

    OutlinedTextField(
        value = cleanDigits,
        onValueChange = { newRaw ->
            val digits = OmniDateMaskUtils.filterDigits(newRaw)
            val formatted = OmniDateMaskUtils.formatDigitsToDdMmYyyy(digits)
            onValueChange(formatted, digits)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        isError = isError,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = singleLine,
        modifier = modifier.testTag(testTag)
    )
}

@Composable
fun MaskedDateTextField(
    digitsValue: String,
    onDigitsChange: (String) -> Unit,
    label: String = "Data (DD/MM/AAAA)",
    placeholder: String = "DD/MM/AAAA",
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    testTag: String = "masked_date_input",
    modifier: Modifier = Modifier
) {
    MaskedDateTextField(
        value = digitsValue,
        onValueChange = { _, cleanDigits -> onDigitsChange(cleanDigits) },
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        testTag = testTag,
        modifier = modifier
    )
}

@Composable
fun MaskedDateTextField(
    dateValue: String,
    onDateValueChange: (formattedDdMmYyyy: String, parsedEpochDay: Long?) -> Unit,
    label: String = "Data (DD/MM/AAAA)",
    placeholder: String = "DD/MM/AAAA",
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    testTag: String = "masked_date_input",
    modifier: Modifier = Modifier,
    parseEpochDay: Boolean = true
) {
    MaskedDateTextField(
        value = dateValue,
        onValueChange = { formatted, cleanDigits ->
            val epoch = if (parseEpochDay) OmniDateMaskUtils.parseToEpochDay(cleanDigits) else null
            onDateValueChange(formatted, epoch)
        },
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        testTag = testTag,
        modifier = modifier
    )
}

// ============================================================================
// 2. FEEDBACK VISUAL + HÁPTICO EM BOTÕES DE SALVAR / OK / CONFIRMAR
// ============================================================================

@Composable
fun rememberOmniFeedback(
    onShowBannerMessage: ((String) -> Unit)? = null
): (String) -> Unit {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    return remember(context, haptic, onShowBannerMessage) {
        { message: String ->
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {
            }
            if (onShowBannerMessage != null) {
                onShowBannerMessage(message)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ============================================================================
// 3. DIÁLOGO PADRÃO DE CONFIRMAÇÃO DE EXCLUSÃO
// ============================================================================

@Composable
fun OmniConfirmDeleteDialog(
    title: String = "Confirmar Exclusão",
    itemName: String,
    extraMessage: String = "Esta ação não poderá ser desfeita.",
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val triggerFeedback = rememberOmniFeedback()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = ExpenseCoral
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja excluir \"$itemName\"? $extraMessage",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    triggerFeedback("Item excluído com sucesso!")
                    onConfirmDelete()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseCoral,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_dialog_btn")
            ) {
                Text("Excluir", fontWeight = FontWeight.Bold)
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
fun OmniConfirmDeleteDialog(
    title: String = "Confirmar Exclusão",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val triggerFeedback = rememberOmniFeedback()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = ExpenseCoral
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    triggerFeedback("Item excluído com sucesso!")
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseCoral,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_dialog_btn")
            ) {
                Text("Excluir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ============================================================================
// 4. VERIFICAÇÃO E ATALHOS DE PERMISSÕES ESSENCIAIS (NOTIFICAÇÕES E ALARMES EXATOS)
// ============================================================================

object OmniPermissionHelper {
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun openExactAlarmSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                openNotificationSettings(context)
            }
        } catch (_: Exception) {
            openNotificationSettings(context)
        }
    }
}

/**
 * Card discreto exibido nas Configurações e na criação de alarmes caso Notificações
 * ou Alarmes Exatos estejam desativados no Android, com botão "Ativar agora".
 */
@Composable
fun OmniPermissionsNoticeCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showEvenIfAllGranted: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationsGranted = remember(refreshTick) {
        OmniPermissionHelper.hasNotificationPermission(context)
    }
    val exactAlarmsGranted = remember(refreshTick) {
        OmniPermissionHelper.canScheduleExactAlarms(context)
    }

    if (notificationsGranted && exactAlarmsGranted && !showEvenIfAllGranted) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("omni_permissions_notice_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notificationsGranted || !exactAlarmsGranted) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (!notificationsGranted || !exactAlarmsGranted) {
                        ExpenseCoral
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!notificationsGranted || !exactAlarmsGranted) {
                            "Permissões de Alarme e Notificação Pendentes"
                        } else {
                            "Permissões do Sistema Ativas"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Para que lembretes de tarefas e o cronômetro pós-refeição de glicemia toquem mesmo com a tela bloqueada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!notificationsGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ExpenseCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Notificações Desativadas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alertas de tarefas, contas e glicemia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { OmniPermissionHelper.openNotificationSettings(context) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("btn_enable_notifications_now")
                        ) {
                            Text("Ativar agora", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!exactAlarmsGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = ExpenseCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Alarmes Exatos Desativados",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tocar no horário exato com tela bloqueada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { OmniPermissionHelper.openExactAlarmSettings(context) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("btn_enable_exact_alarms_now")
                        ) {
                            Text("Ativar agora", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
