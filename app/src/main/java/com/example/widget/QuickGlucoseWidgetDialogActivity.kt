package com.example.widget

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CustomizationRepository
import com.example.data.DiabetesSlotCatalog
import com.example.data.HealthRepository
import com.example.data.LocalOfflineStore
import com.example.data.OmniBackupAndMigrationManager
import kotlinx.coroutines.delay

/**
 * Compact floating translucent Dialog Activity launched directly from the Home Screen Widgets:
 * - Mode "GLUCOSE" (default): Pre-selects the glucose slot, focuses the numeric mg/dL field with keyboard open,
 *   lets user select family member (Marcus/Nathalia), saves directly to HealthRepository, updates widgets, and closes.
 * - Mode "FAMILY_MURAL": Quick floating input to add an item to "Omni • Lista & Mural da Família" directly from the widget.
 */
class QuickGlucoseWidgetDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val dialogMode = intent?.getStringExtra(EXTRA_DIALOG_MODE) ?: MODE_GLUCOSE
        val initialSlot = DiabetesSlotCatalog.normalizeSlot(
            intent?.getStringExtra(EXTRA_PRESELECTED_SLOT) ?: DiabetesSlotCatalog.JEJUM
        )

        val appContext = applicationContext
        val healthRepo = HealthRepository(appContext)
        val localStore = LocalOfflineStore(appContext)
        val customizationConfig = CustomizationRepository(appContext).configFlow.value
        val household = localStore.householdFlow.value
        val familyMembers = household.membersWithAssignedColors(
            fallbackProfileName = customizationConfig.profileName,
            fallbackProfileColorHex = customizationConfig.profileColorHex
        )

        setContent {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val scheme = if (isDark) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { finish() }
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (dialogMode == MODE_FAMILY_MURAL) {
                        QuickFamilyMuralFloatingCard(
                            defaultAuthor = familyMembers.firstOrNull()?.displayName ?: "Família",
                            onDismiss = { finish() },
                            onSaveItem = { title, category, author ->
                                OmniBackupAndMigrationManager.addFamilyMuralItem(
                                    context = appContext,
                                    title = title,
                                    category = category,
                                    addedBy = author
                                )
                                OmniHomeWidgetProvider.requestUpdate(appContext)
                                Toast.makeText(
                                    appContext,
                                    "✓ '$title' adicionado à Lista da Família!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        )
                    } else {
                        QuickGlucoseFloatingCard(
                            initialSlot = initialSlot,
                            familyMembers = familyMembers,
                            onDismiss = { finish() },
                            onSaveGlucose = { mgDl, slot, contextTag, notes, memberId, memberName, start1hTimer ->
                                healthRepo.addGlucoseRecord(
                                    dateEpochDay = com.example.data.DateCalendarUtils.todayEpochDay(),
                                    timeHHmm = com.example.data.DateCalendarUtils.currentTimeHHmm(),
                                    slot = slot,
                                    valueMgDl = mgDl,
                                    contextTag = contextTag,
                                    notes = notes,
                                    privacyScope = com.example.data.PrivacyScope.FAMILY,
                                    memberId = memberId,
                                    memberName = memberName
                                )
                                healthRepo.dismissPostMealTimerAlert()
                                if (start1hTimer) {
                                    val postSlot = DiabetesSlotCatalog.postMealSlotForPreMeal(slot)
                                    healthRepo.startPostMealTimer(postSlot, 3600)
                                    com.example.alarm.AlarmScheduler.schedulePostMealGlucoseAlarm(
                                        context = appContext,
                                        triggerAtMillis = System.currentTimeMillis() + 3600_000L,
                                        mealLabel = postSlot
                                    )
                                }
                                OmniHomeWidgetProvider.requestUpdate(appContext)
                                Toast.makeText(
                                    appContext,
                                    "🩸 $mgDl mg/dL ($slot • $memberName) salvo!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DIALOG_MODE = "extra_omni_widget_dialog_mode"
        const val EXTRA_PRESELECTED_SLOT = "extra_omni_widget_preselected_slot"
        const val MODE_GLUCOSE = "GLUCOSE"
        const val MODE_FAMILY_MURAL = "FAMILY_MURAL"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickGlucoseFloatingCard(
    initialSlot: String,
    familyMembers: List<com.example.data.FamilyMemberBadge>,
    onDismiss: () -> Unit,
    onSaveGlucose: (mgDl: Int, slot: String, contextTag: String, notes: String, memberId: String, memberName: String, start1hTimer: Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var mgDlText by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableStateOf(initialSlot) }
    var selectedContextTag by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var start1hTimerSwitch by remember(selectedSlot) {
        mutableStateOf(DiabetesSlotCatalog.canTrigger1hTimerFromSlot(selectedSlot))
    }
    var selectedMember by remember {
        mutableStateOf(
            familyMembers.firstOrNull() ?: com.example.data.FamilyMemberBadge(
                userId = "marcus",
                displayName = "Marcus",
                colorHex = 0xFF0E7C5B
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(120L)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🩸 Registro Rápido de Glicemia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            // Seletor de Membro da Família
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                familyMembers.forEach { member ->
                    val isSel = selectedMember.displayName.equals(member.displayName, ignoreCase = true)
                    val memColor = Color(member.colorHex.toInt())
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSel) memColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedMember = member }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(memColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) memColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Campo Numérico focado automaticamente com teclado numérico aberto
            OutlinedTextField(
                value = mgDlText,
                onValueChange = { mgDlText = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text("Valor da Glicemia (mg/dL)") },
                placeholder = { Text("Ex: 98") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = mgDlText.toIntOrNull() ?: 0
                        if (parsed in 20..600) {
                            onSaveGlucose(
                                parsed,
                                selectedSlot,
                                selectedContextTag,
                                notesText.trim(),
                                selectedMember.userId,
                                selectedMember.displayName,
                                DiabetesSlotCatalog.canTrigger1hTimerFromSlot(selectedSlot) && start1hTimerSwitch
                            )
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Seletor compacto dos 6 períodos oficiais do dia
            Text(
                text = "Período Oficial (1 de 6):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DiabetesSlotCatalog.allSlots.forEach { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = { selectedSlot = slot },
                        label = {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            if (DiabetesSlotCatalog.canTrigger1hTimerFromSlot(selectedSlot)) {
                val targetPostSlot = DiabetesSlotCatalog.postMealSlotForPreMeal(selectedSlot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Iniciar alarme de 1h ($targetPostSlot) ao salvar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Switch(
                        checked = start1hTimerSwitch,
                        onCheckedChange = { start1hTimerSwitch = it }
                    )
                }
            }

            // Chips rápidos de contexto
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                com.example.data.GlucoseContextCatalog.quickContexts.forEach { ctx ->
                    val isSel = selectedContextTag == ctx
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedContextTag = if (isSel) "" else ctx },
                        label = { Text(ctx, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Observação rápida (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                val parsedVal = mgDlText.toIntOrNull() ?: 0
                Button(
                    onClick = {
                        if (parsedVal in 20..600) {
                            onSaveGlucose(
                                parsedVal,
                                selectedSlot,
                                selectedContextTag,
                                notesText.trim(),
                                selectedMember.userId,
                                selectedMember.displayName,
                                DiabetesSlotCatalog.canTrigger1hTimerFromSlot(selectedSlot) && start1hTimerSwitch
                            )
                        }
                    },
                    enabled = parsedVal in 20..600,
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Salvar Medição", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickFamilyMuralFloatingCard(
    defaultAuthor: String,
    onDismiss: () -> Unit,
    onSaveItem: (title: String, category: String, author: String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var titleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Compras") }

    LaunchedEffect(Unit) {
        delay(120L)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🛒 Adicionar à Lista da Família",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Compras", "Mural", "Casa").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Item ou recado para a família") },
                placeholder = { Text("Ex: Leite, frutas, papel toalha...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (titleText.trim().isNotEmpty()) {
                            onSaveItem(titleText.trim(), selectedCategory, defaultAuthor)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (titleText.trim().isNotEmpty()) {
                            onSaveItem(titleText.trim(), selectedCategory, defaultAuthor)
                        }
                    },
                    enabled = titleText.trim().isNotEmpty(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Adicionar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
