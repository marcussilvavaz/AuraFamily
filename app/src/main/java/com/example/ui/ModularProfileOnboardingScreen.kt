package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationRepository
import com.example.data.DiabetesSlotCatalog
import com.example.data.EnergyLevel
import com.example.data.RoutineRhythm
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.SavingsGold

/**
 * Full-screen First-Access Onboarding for "Meu Perfil e Módulos" (Passo 1).
 * Can also be opened at any time from Settings/Profile.
 */
@Composable
fun ModularProfileOnboardingScreen(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onFinishOnboarding: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("modular_profile_onboarding_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Card
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Meu Perfil e Módulos",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Personalize seu ritmo de rotina, energia, saúde e acolhimento diário",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Configure agora para moldar o aplicativo ao seu ritmo real. Você pode alterar qualquer opção depois no menu do seu Perfil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Reusable Modular Profile Sections
        item {
            ModularProfileEditorContent(
                config = config,
                customizationRepo = customizationRepo
            )
        }

        // Complete Onboarding CTA
        item {
            Button(
                onClick = {
                    customizationRepo.completeModularOnboarding()
                    onFinishOnboarding()
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("finish_modular_onboarding_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salvar Meu Perfil e Entrar no App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Reusable Modular Profile & Health Modules Editor (Passo 1)
 * Used inside both First-Access Onboarding and the Profile & Settings Screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModularProfileEditorContent(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository
) {
    var wakeTimeInput by remember(config.fixedWakeTime) { mutableStateOf(config.fixedWakeTime) }
    var sleepTimeInput by remember(config.fixedSleepTime) { mutableStateOf(config.fixedSleepTime) }
    var dppInput by remember(config.pregnancyDppDate) { mutableStateOf(config.pregnancyDppDate) }
    var bpReminderInput by remember(config.bloodPressureReminderTime) {
        mutableStateOf(config.bloodPressureReminderTime)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =====================================================================
        // 1. RITMO DA ROTINA ("Fixa" vs "Elástica")
        // =====================================================================
        ModularBlockCard(
            title = "Ritmo da Rotina",
            subtitle = "Como o seu dia começa na Agenda",
            icon = Icons.Default.Schedule,
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isFixa = config.routineRhythm == RoutineRhythm.FIXA
                    val isElastica = config.routineRhythm == RoutineRhythm.ELASTICA

                    RoutineChoiceCard(
                        modifier = Modifier.weight(1f),
                        title = "Rotina Fixa",
                        description = "Horário padrão para acordar e dormir",
                        selected = isFixa,
                        onClick = {
                            customizationRepo.updateRoutineRhythm(
                                rhythm = RoutineRhythm.FIXA,
                                fixedWakeTime = wakeTimeInput,
                                fixedSleepTime = sleepTimeInput
                            )
                        },
                        testTag = "routine_rhythm_fixa_btn"
                    )

                    RoutineChoiceCard(
                        modifier = Modifier.weight(1f),
                        title = "Rotina Elástica",
                        description = "O dia começa quando clico em \"Acordei\"",
                        selected = isElastica,
                        onClick = {
                            customizationRepo.updateRoutineRhythm(
                                rhythm = RoutineRhythm.ELASTICA,
                                fixedWakeTime = wakeTimeInput,
                                fixedSleepTime = sleepTimeInput
                            )
                        },
                        testTag = "routine_rhythm_elastica_btn"
                    )
                }

                AnimatedVisibility(visible = config.routineRhythm == RoutineRhythm.FIXA) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = wakeTimeInput,
                                onValueChange = {
                                    wakeTimeInput = it
                                    customizationRepo.updateRoutineRhythm(
                                        rhythm = RoutineRhythm.FIXA,
                                        fixedWakeTime = it,
                                        fixedSleepTime = sleepTimeInput
                                    )
                                },
                                label = { Text("Acordar (HH:mm)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fixed_wake_time_input")
                            )
                            OutlinedTextField(
                                value = sleepTimeInput,
                                onValueChange = {
                                    sleepTimeInput = it
                                    customizationRepo.updateRoutineRhythm(
                                        rhythm = RoutineRhythm.FIXA,
                                        fixedWakeTime = wakeTimeInput,
                                        fixedSleepTime = it
                                    )
                                },
                                label = { Text("Dormir (HH:mm)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fixed_sleep_time_input")
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = config.routineRhythm == RoutineRhythm.ELASTICA) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rotina Elástica ativa: um botão discreto \"Acordei\" ficará no topo da sua Agenda para iniciar o dia no seu próprio ritmo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. MAPA DE ENERGIA (Manhã, Tarde e Noite: Baixa, Média ou Alta)
        // =====================================================================
        ModularBlockCard(
            title = "Mapa de Energia",
            subtitle = "Como costuma ser sua disposição em cada turno do dia",
            icon = Icons.Default.BatteryChargingFull,
            accentColor = SavingsGold
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EnergyShiftSelectorRow(
                    shiftLabel = "🌅 Manhã",
                    selectedLevel = config.energyMorning,
                    onSelectLevel = { level ->
                        customizationRepo.updateEnergyMap(morning = level)
                    },
                    testTagPrefix = "energy_morning"
                )
                EnergyShiftSelectorRow(
                    shiftLabel = "☀️ Tarde",
                    selectedLevel = config.energyAfternoon,
                    onSelectLevel = { level ->
                        customizationRepo.updateEnergyMap(afternoon = level)
                    },
                    testTagPrefix = "energy_afternoon"
                )
                EnergyShiftSelectorRow(
                    shiftLabel = "🌙 Noite",
                    selectedLevel = config.energyNight,
                    onSelectLevel = { level ->
                        customizationRepo.updateEnergyMap(night = level)
                    },
                    testTagPrefix = "energy_night"
                )
            }
        }

        // =====================================================================
        // 3. MÓDULOS DE SAÚDE (Gestação, Diabetes/Glicemia, Pressão, Dores/Fadiga)
        // =====================================================================
        ModularBlockCard(
            title = "Módulos de Saúde",
            subtitle = "Ative apenas os acompanhamentos que fazem sentido para você",
            icon = Icons.Default.MonitorHeart,
            accentColor = ExpenseCoral
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 3.1 Gestação (pede Data Prevista do Parto - DPP)
                HealthModuleSwitchCard(
                    title = "Gestação",
                    subtitle = "Acompanhamento gestacional com Data Prevista do Parto (DPP)",
                    icon = Icons.Default.ChildCare,
                    checked = config.pregnancyEnabled,
                    onCheckedChange = { enabled ->
                        customizationRepo.updatePregnancyModule(enabled = enabled, dppDate = dppInput)
                    },
                    switchTestTag = "switch_pregnancy_module"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dppInput,
                            onValueChange = {
                                dppInput = it
                                customizationRepo.updatePregnancyModule(enabled = true, dppDate = it)
                            },
                            label = { Text("Data Prevista do Parto - DPP (DD/MM/AAAA)") },
                            placeholder = { Text("Ex: 15/05/2027") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pregnancy_dpp_input")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("10/03/2027", "20/05/2027", "15/08/2027").forEach { presetDpp ->
                                FilterChip(
                                    selected = dppInput == presetDpp,
                                    onClick = {
                                        dppInput = presetDpp
                                        customizationRepo.updatePregnancyModule(enabled = true, dppDate = presetDpp)
                                    },
                                    label = { Text("DPP: $presetDpp") },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                // 3.2 Diabetes / Glicemia (escolher horários/slots de medição)
                HealthModuleSwitchCard(
                    title = "Diabetes / Glicemia",
                    subtitle = "Escolha os horários e momentos de medição glicêmica",
                    icon = Icons.Default.Favorite,
                    checked = config.diabetesEnabled,
                    onCheckedChange = { enabled ->
                        customizationRepo.updateDiabetesModule(enabled = enabled)
                    },
                    switchTestTag = "switch_diabetes_module"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Toque nos horários/slots de medição que você utiliza:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiabetesSlotCatalog.allSlots.forEach { slot ->
                                val selected = slot in config.diabetesSlots
                                FilterChip(
                                    selected = selected,
                                    onClick = { customizationRepo.toggleDiabetesSlot(slot) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    } else null,
                                    label = { Text(slot) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("diabetes_slot_$slot")
                                )
                            }
                        }
                    }
                }

                // 3.3 Pressão Arterial (pedir horário do lembrete, ex: antes de dormir)
                HealthModuleSwitchCard(
                    title = "Pressão Arterial",
                    subtitle = "Lembrete diário para aferição da pressão arterial",
                    icon = Icons.Default.MonitorHeart,
                    checked = config.bloodPressureEnabled,
                    onCheckedChange = { enabled ->
                        customizationRepo.updateBloodPressureModule(
                            enabled = enabled,
                            reminderTime = bpReminderInput
                        )
                    },
                    switchTestTag = "switch_blood_pressure_module"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bpReminderInput,
                            onValueChange = {
                                bpReminderInput = it
                                customizationRepo.updateBloodPressureModule(
                                    enabled = true,
                                    reminderTime = it
                                )
                            },
                            label = { Text("Horário do Lembrete (ex: 21:30 - Antes de dormir)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bp_reminder_time_input")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "08:00 (Ao acordar)",
                                "14:00 (Tarde)",
                                "21:30 (Antes de dormir)"
                            ).forEach { presetBp ->
                                FilterChip(
                                    selected = bpReminderInput == presetBp,
                                    onClick = {
                                        bpReminderInput = presetBp
                                        customizationRepo.updateBloodPressureModule(
                                            enabled = true,
                                            reminderTime = presetBp
                                        )
                                    },
                                    label = { Text(presetBp) },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                // 3.4 Dores Físicas / Fadiga (ativa o botão "Modo Dia Difícil")
                HealthModuleSwitchCard(
                    title = "Dores Físicas / Fadiga",
                    subtitle = "Habilita o botão \"Modo Dia Difícil\" no topo da Agenda",
                    icon = Icons.Default.Healing,
                    checked = config.chronicPainFatigueEnabled,
                    onCheckedChange = { enabled ->
                        customizationRepo.updateChronicPainFatigueModule(enabled)
                    },
                    switchTestTag = "switch_pain_fatigue_module"
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Com este módulo ativo, você terá o botão \"Modo Dia Difícil\" no topo da Agenda para ocultar tarefas não essenciais, receber acolhimento e congelar sua sequência (streak) sem penalidade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. DIÁRIO E ACOLHIMENTO (Humor por hora, Frases em 3 turnos, Carta IA)
        // =====================================================================
        ModularBlockCard(
            title = "Diário e Acolhimento",
            subtitle = "Recursos de bem-estar emocional e mensagens de apoio",
            icon = Icons.Default.AutoAwesome,
            accentColor = IncomeEmerald
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleToggleRow(
                    title = "Humor por hora",
                    subtitle = "Registre como está se sentindo ao longo dos turnos do dia",
                    checked = config.hourlyMoodEnabled,
                    onCheckedChange = {
                        customizationRepo.updateDiaryAndSupportModules(hourlyMoodEnabled = it)
                    },
                    testTag = "switch_hourly_mood"
                )
                SimpleToggleRow(
                    title = "Frases em 3 turnos",
                    subtitle = "Mensagens acolhedoras para Manhã, Tarde e Noite",
                    checked = config.threeShiftQuotesEnabled,
                    onCheckedChange = {
                        customizationRepo.updateDiaryAndSupportModules(threeShiftQuotesEnabled = it)
                    },
                    testTag = "switch_three_shift_quotes"
                )
                SimpleToggleRow(
                    title = "Carta de Apoio por IA",
                    subtitle = "Receba uma carta personalizada de encorajamento em dias intensos",
                    checked = config.aiSupportLetterEnabled,
                    onCheckedChange = {
                        customizationRepo.updateDiaryAndSupportModules(aiSupportLetterEnabled = it)
                    },
                    testTag = "switch_ai_support_letter"
                )
            }
        }
    }
}

@Composable
private fun ModularBlockCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun RoutineChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnergyShiftSelectorRow(
    shiftLabel: String,
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    testTagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = shiftLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(88.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    EnergyLevel.BAIXA to "Baixa",
                    EnergyLevel.MEDIA to "Média",
                    EnergyLevel.ALTA to "Alta"
                ).forEach { (levelKey, label) ->
                    val isSelected = selectedLevel == levelKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectLevel(levelKey) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("${testTagPrefix}_$levelKey")
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthModuleSwitchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String,
    expandedContent: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag(switchTestTag)
                )
            }

            AnimatedVisibility(visible = checked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }
}

@Composable
private fun SimpleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
