package com.example.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class ThemePresetInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val isDark: Boolean,
    val backgroundHex: Long,
    val cardHex: Long,
    val textHex: Long,
    val primaryHex: Long,
    val accentHex: Long,
    val hubColorHex: Long = primaryHex,
    val agendaColorHex: Long = accentHex,
    val healthColorHex: Long = 0xFF10B981,
    val diaryColorHex: Long = 0xFF8B5CF6,
    val financeColorHex: Long = 0xFFE09F3E
)

data class FontChoiceInfo(
    val id: String,
    val displayName: String,
    val styleHint: String
)

data class HubWidgetCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String
)

object HubWidgetIds {
    const val TASKS_TODAY = "TASKS_TODAY"
    const val HEALTH_SUMMARY = "HEALTH_SUMMARY"
    const val BALANCE_RING = "BALANCE_RING"
    const val BILLS_CURRENCY = "BILLS_CURRENCY"
    const val ACHIEVEMENTS_MEDALS = "ACHIEVEMENTS_MEDALS"
    const val FAMILY_COLORS = "FAMILY_COLORS"

    val defaultOrder: List<String> = listOf(
        TASKS_TODAY,
        HEALTH_SUMMARY,
        BALANCE_RING,
        ACHIEVEMENTS_MEDALS,
        BILLS_CURRENCY,
        FAMILY_COLORS
    )
}

object AgendaLayoutMode {
    const val COMPLETO = "COMPLETO"
    const val SIMPLIFICADO = "SIMPLIFICADO"
    const val MINIMO = "MINIMO"
}

object OrganicVisualStyle {
    const val ORGANICO_FLUIDO = "ORGANICO_FLUIDO"
    const val TRANSLUCIDO_GLASS = "TRANSLUCIDO_GLASS"
    const val MINIMALISTA_ACOLHEDOR = "MINIMALISTA_ACOLHEDOR"
}

object AppModuleId {
    const val HUB = "HUB"
    const val AGENDA = "AGENDA"
    const val HEALTH = "HEALTH"
    const val DIARY = "DIARY"
    const val FINANCE = "FINANCE"
}

data class ModuleColorSpec(
    val moduleId: String,
    val label: String,
    val description: String
)

data class AppCustomizationConfig(
    val presetId: String = "EMERALD_GOLD",
    val useCustomColors: Boolean = false,
    val backgroundHex: Long = 0xFFF2F7F4,
    val cardHex: Long = 0xFFFFFFFF,
    val textHex: Long = 0xFF10221A,
    val primaryHex: Long = 0xFF0B6E4F,
    val accentHex: Long = 0xFFD4AF37,
    val fontFamilyId: String = "NUNITO",
    val cardCornerRadiusDp: Int = 34,
    val organicVisualStyle: String = OrganicVisualStyle.ORGANICO_FLUIDO,
    // Cores Independentes para os 5 Módulos
    val hubColorHex: Long = 0xFF118D5F,
    val agendaColorHex: Long = 0xFFE88378,
    val healthColorHex: Long = 0xFF00A896,
    val diaryColorHex: Long = 0xFF8B5CF6,
    val financeColorHex: Long = 0xFFE09F3E,
    val taskAlarmSoundUri: String = "",
    val taskAlarmSoundTitle: String = "Alarme Padrão do Sistema",
    val taskVibrateEnabled: Boolean = true,
    val billReminderSoundUri: String = "",
    val billReminderSoundTitle: String = "Notificação Padrão do Sistema",
    val billVibrateEnabled: Boolean = true,
    // Profile Identity (Photo + Name + Personal Color for Family Agenda)
    val profileName: String = "Você",
    val profileColorHex: Long = 0xFFE88378,
    val profilePhotoPath: String = "",
    val profilePhotoVersion: Long = 0L,
    // Structured Agenda Appearance & Layout Mode ("COMPLETO", "SIMPLIFICADO", "MINIMO")
    val agendaLayoutMode: String = AgendaLayoutMode.COMPLETO,
    // Smartwatch Hub Builder Config
    val hubWidgetOrder: List<String> = HubWidgetIds.defaultOrder,
    val enabledHubWidgets: Set<String> = HubWidgetIds.defaultOrder.toSet(),
    // =========================================================================
    // PASSO 1: PERFIL MODULAR, SAÚDE, ACOLHIMENTO & ESTADO DO DIA
    // =========================================================================
    val hasCompletedModularOnboarding: Boolean = false,
    // 1. Ritmo da Rotina ("FIXA" ou "ELASTICA")
    val routineRhythm: String = RoutineRhythm.ELASTICA,
    val fixedWakeTime: String = "07:00",
    val fixedSleepTime: String = "22:30",
    // 2. Mapa de Energia (Manhã, Tarde, Noite -> "BAIXA", "MEDIA", "ALTA")
    val energyMorning: String = EnergyLevel.ALTA,
    val energyAfternoon: String = EnergyLevel.MEDIA,
    val energyNight: String = EnergyLevel.BAIXA,
    // 3. Módulos de Saúde (switches opcionais)
    val pregnancyEnabled: Boolean = false,
    val pregnancyDppDate: String = "", // Data Prevista do Parto (DD/MM/AAAA)
    val diabetesEnabled: Boolean = false,
    val diabetesSlots: Set<String> = DiabetesSlotCatalog.defaultSelectedSlots,
    val bloodPressureEnabled: Boolean = false,
    val bloodPressureReminderTime: String = "21:30 (Antes de dormir)",
    val chronicPainFatigueEnabled: Boolean = false, // Ativa o botão "Modo Dia Difícil" somente se marcado no questionário
    // 4. Diário e Acolhimento (switches)
    val hourlyMoodEnabled: Boolean = true,
    val threeShiftQuotesEnabled: Boolean = true,
    val aiSupportLetterEnabled: Boolean = true,
    // 5. Sequência (Streak) e Estado Diário (daily_state por data YYYY-MM-DD)
    val currentStreakDays: Int = 5,
    val dailyStatesByDate: Map<String, DailyState> = emptyMap()
) {
    fun dailyStateFor(dateIso: String): DailyState {
        return dailyStatesByDate[dateIso] ?: DailyState(dateIso = dateIso)
    }

    fun colorForModule(moduleId: String): Long {
        return when (moduleId) {
            AppModuleId.HUB -> hubColorHex
            AppModuleId.AGENDA -> agendaColorHex
            AppModuleId.HEALTH -> healthColorHex
            AppModuleId.DIARY -> diaryColorHex
            AppModuleId.FINANCE -> financeColorHex
            else -> primaryHex
        }
    }
}

object CustomizationCatalog {

    val moduleSpecs: List<ModuleColorSpec> = listOf(
        ModuleColorSpec(AppModuleId.HUB, "Hub", "Anéis smartwatch, saudação e resumo do lar"),
        ModuleColorSpec(AppModuleId.AGENDA, "Agenda", "Linha do tempo, tarefas, pílulas e calendário"),
        ModuleColorSpec(AppModuleId.HEALTH, "Saúde", "Inegociáveis, glicemia, pressão e gestação"),
        ModuleColorSpec(AppModuleId.DIARY, "Diário", "Humor por hora, frases de turno e carta IA"),
        ModuleColorSpec(AppModuleId.FINANCE, "Finanças", "Contas, orçamento 50/30/20 e metas")
    )

    // Paleta completa de cores orgânicas e vibrantes para personalizar cada módulo
    val structuredAppColors: List<Pair<String, Long>> = listOf(
        "Coral Structured" to 0xFFE88378,
        "Esmeralda Aura" to 0xFF118D5F,
        "Turquesa Saúde" to 0xFF00A896,
        "Lavanda Diário" to 0xFF8B5CF6,
        "Dourado Solar" to 0xFFE09F3E,
        "Tangerina" to 0xFFE67E45,
        "Amarelo Solar" to 0xFFF5C132,
        "Verde Folha" to 0xFF6BB848,
        "Azul Céu" to 0xFF5E9AD6,
        "Azul Royal" to 0xFF0971E6,
        "Rosé Afeto" to 0xFFD84B72,
        "Rubi" to 0xFFD84949,
        "Índigo Sereno" to 0xFF6366F1,
        "Cinza Neutro" to 0xFF8E99A4
    )

    val hubWidgets: List<HubWidgetCatalogItem> = listOf(
        HubWidgetCatalogItem(
            id = HubWidgetIds.TASKS_TODAY,
            title = "Tarefas do Dia & Próximos Alarmes",
            subtitle = "Anel de progresso diário, cor do membro e check rápido"
        ),
        HubWidgetCatalogItem(
            id = HubWidgetIds.HEALTH_SUMMARY,
            title = "Saúde, Inegociáveis & Medições",
            subtitle = "Trackers diários, últimas medições (Glicemia/Pressão) e timer 1h"
        ),
        HubWidgetCatalogItem(
            id = HubWidgetIds.BALANCE_RING,
            title = "Saldo Disponível & Anel 50/30/20",
            subtitle = "Mostrador circular de economia familiar e saldo livre"
        ),
        HubWidgetCatalogItem(
            id = HubWidgetIds.BILLS_CURRENCY,
            title = "Contas a Vencer & Câmbio Rápido",
            subtitle = "Próximos vencimentos do mês e cotações USD, EUR e GBP"
        ),
        HubWidgetCatalogItem(
            id = HubWidgetIds.ACHIEVEMENTS_MEDALS,
            title = "Conquistas & Medalhas (Saúde, Finanças e Agenda)",
            subtitle = "Objetivos integrados, medalhas de Bronze a Diamante e nível de evolução"
        ),
        HubWidgetCatalogItem(
            id = HubWidgetIds.FAMILY_COLORS,
            title = "Membros da Família & Filtro por Cor",
            subtitle = "Avatares da família e filtro de tarefas pela cor do perfil"
        )
    )

    val memberAgendaColors: List<Pair<String, Long>> = structuredAppColors

    val presets: List<ThemePresetInfo> = listOf(
        ThemePresetInfo(
            id = "EMERALD_GOLD",
            name = "Aura Orgânica",
            subtitle = "Esmeralda, coral suave e dourado acolhedor",
            isDark = false,
            backgroundHex = 0xFFF2F7F4,
            cardHex = 0xFFFFFFFF,
            textHex = 0xFF10221A,
            primaryHex = 0xFF0B6E4F,
            accentHex = 0xFFD4AF37,
            hubColorHex = 0xFF118D5F,
            agendaColorHex = 0xFFE88378,
            healthColorHex = 0xFF00A896,
            diaryColorHex = 0xFF8B5CF6,
            financeColorHex = 0xFFE09F3E
        ),
        ThemePresetInfo(
            id = "STRUCTURED_DARK",
            name = "Structured Coral (Escuro)",
            subtitle = "Fundo obsidiana suave com coral, menta e lavanda",
            isDark = true,
            backgroundHex = 0xFF111317,
            cardHex = 0xFF1B1E24,
            textHex = 0xFFF3F4F6,
            primaryHex = 0xFFE88378,
            accentHex = 0xFF34D399,
            hubColorHex = 0xFF34D399,
            agendaColorHex = 0xFFE88378,
            healthColorHex = 0xFF38BDF8,
            diaryColorHex = 0xFFA78BFA,
            financeColorHex = 0xFFFBBF24
        ),
        ThemePresetInfo(
            id = "WARM_TERRACOTTA",
            name = "Terracota & Sol",
            subtitle = "Tons quentes de argila, âmbar e sálvia",
            isDark = false,
            backgroundHex = 0xFFFDF6F0,
            cardHex = 0xFFFFFFFF,
            textHex = 0xFF2D1A12,
            primaryHex = 0xFFC85A32,
            accentHex = 0xFFE09F3E,
            hubColorHex = 0xFFC85A32,
            agendaColorHex = 0xFFE67E45,
            healthColorHex = 0xFF6BB848,
            diaryColorHex = 0xFFD84B72,
            financeColorHex = 0xFFE09F3E
        ),
        ThemePresetInfo(
            id = "LAVENDER_CALM",
            name = "Lavanda & Sereno",
            subtitle = "Suave, relaxante e poético",
            isDark = false,
            backgroundHex = 0xFFF6F4FB,
            cardHex = 0xFFFFFFFF,
            textHex = 0xFF1F1833,
            primaryHex = 0xFF6C4AB6,
            accentHex = 0xFFB983FF,
            hubColorHex = 0xFF6C4AB6,
            agendaColorHex = 0xFF8B5CF6,
            healthColorHex = 0xFF00A896,
            diaryColorHex = 0xFFD84B72,
            financeColorHex = 0xFF5E9AD6
        ),
        ThemePresetInfo(
            id = "OCEAN_BREEZE",
            name = "Brisa do Oceano",
            subtitle = "Azul profundo, turquesa e coral marinho",
            isDark = false,
            backgroundHex = 0xFFF0F7FA,
            cardHex = 0xFFFFFFFF,
            textHex = 0xFF0E2431,
            primaryHex = 0xFF0077B6,
            accentHex = 0xFF00B4D8,
            hubColorHex = 0xFF0077B6,
            agendaColorHex = 0xFF5E9AD6,
            healthColorHex = 0xFF00A896,
            diaryColorHex = 0xFF6366F1,
            financeColorHex = 0xFF118D5F
        ),
        ThemePresetInfo(
            id = "ROSE_PEACH",
            name = "Rosé & Pêssego",
            subtitle = "Delicado, afetivo e luminoso",
            isDark = false,
            backgroundHex = 0xFFFFF5F7,
            cardHex = 0xFFFFFFFF,
            textHex = 0xFF33151E,
            primaryHex = 0xFFD84B72,
            accentHex = 0xFFF4978E,
            hubColorHex = 0xFFD84B72,
            agendaColorHex = 0xFFE88378,
            healthColorHex = 0xFF118D5F,
            diaryColorHex = 0xFF8B5CF6,
            financeColorHex = 0xFFE67E45
        ),
        ThemePresetInfo(
            id = "FOREST_NIGHT",
            name = "Floresta Noturna (Escuro)",
            subtitle = "Verde obsidiana profundo com menta e coral",
            isDark = true,
            backgroundHex = 0xFF091410,
            cardHex = 0xFF132620,
            textHex = 0xFFE9F5F0,
            primaryHex = 0xFF34D399,
            accentHex = 0xFFF3C65F,
            hubColorHex = 0xFF34D399,
            agendaColorHex = 0xFFE88378,
            healthColorHex = 0xFF2DD4BF,
            diaryColorHex = 0xFFA78BFA,
            financeColorHex = 0xFFF3C65F
        ),
        ThemePresetInfo(
            id = "STARRY_DARK",
            name = "Noite Estrelada (Escuro)",
            subtitle = "Azul meia-noite com violeta, céu e ouro",
            isDark = true,
            backgroundHex = 0xFF0B0F1A,
            cardHex = 0xFF161D2F,
            textHex = 0xFFEDF2FB,
            primaryHex = 0xFF818CF8,
            accentHex = 0xFFFBBF24,
            hubColorHex = 0xFF818CF8,
            agendaColorHex = 0xFF5E9AD6,
            healthColorHex = 0xFF34D399,
            diaryColorHex = 0xFFC084FC,
            financeColorHex = 0xFFFBBF24
        )
    )

    val fontChoices: List<FontChoiceInfo> = listOf(
        FontChoiceInfo("NUNITO", "Nunito", "Orgânica, arredondada e super convidativa"),
        FontChoiceInfo("JAKARTA", "Plus Jakarta Sans", "Moderna, limpa e equilibrada"),
        FontChoiceInfo("QUICKSAND", "Quicksand", "Curvas suaves e leitura leve"),
        FontChoiceInfo("COMFORTAA", "Comfortaa", "Estilo circular amigável para família"),
        FontChoiceInfo("OUTFIT", "Outfit", "Geométrica contemporânea"),
        FontChoiceInfo("MONTSERRAT", "Montserrat", "Clássica, forte e elegante"),
        FontChoiceInfo("PLAYFAIR", "Playfair Display", "Editorial sofisticada com serifa"),
        FontChoiceInfo("MERRIWEATHER", "Merriweather", "Literária e confortável"),
        FontChoiceInfo("PACIFICO", "Pacifico", "Manuscrita afetiva nos destaques"),
        FontChoiceInfo("JETBRAINS_MONO", "JetBrains Mono", "Estilo técnico e numérico tabular")
    )

    val backgroundSwatches: List<Pair<String, Long>> = listOf(
        "Menta Suave" to 0xFFF2F7F4,
        "Areia Quente" to 0xFFFDF6F0,
        "Lavanda Claro" to 0xFFF6F4FB,
        "Céu Sereno" to 0xFFF0F7FA,
        "Rosé Pêssego" to 0xFFFFF5F7,
        "Creme Baunilha" to 0xFFFBF8F1,
        "Pérola Puro" to 0xFFF7F7F8,
        "Obsidiana Verde" to 0xFF091410,
        "Meia-Noite Azul" to 0xFF0B0F1A,
        "Carvão Suave" to 0xFF141416,
        "Ameixa Noturno" to 0xFF181020,
        "Café Expresso" to 0xFF1A120E
    )

    val cardSwatches: List<Pair<String, Long>> = listOf(
        "Branco Puro" to 0xFFFFFFFF,
        "Creme Marfim" to 0xFFFFFCF7,
        "Menta Cristal" to 0xFFE8F5EE,
        "Lavanda Névoa" to 0xFFEEEAF9,
        "Azul Gelo" to 0xFFE6F3F8,
        "Pêssego Suave" to 0xFFFDECEF,
        "Verde Noturno" to 0xFF132620,
        "Ardósia Azul" to 0xFF161D2F,
        "Grafite Escuro" to 0xFF202124,
        "Vinho Escuro" to 0xFF281A33,
        "Cacau Escuro" to 0xFF2A1E19
    )

    val textSwatches: List<Pair<String, Long>> = listOf(
        "Floresta Escura" to 0xFF10221A,
        "Carvão Puro" to 0xFF18181B,
        "Cacau Intenso" to 0xFF2D1A12,
        "Marinho Profundo" to 0xFF0E2431,
        "Índigo Escuro" to 0xFF1F1833,
        "Branco Neve" to 0xFFFFFFFF,
        "Menta Luminoso" to 0xFFE9F5F0,
        "Creme Claro" to 0xFFFAF3E8,
        "Dourado Claro" to 0xFFFFE7AD
    )

    val primaryAndAccentSwatches: List<Pair<String, Long>> = listOf(
        "Esmeralda" to 0xFF0B6E4F,
        "Menta Vivo" to 0xFF10B981,
        "Dourado Real" to 0xFFD4AF37,
        "Terracota" to 0xFFC85A32,
        "Âmbar Solar" to 0xFFE09F3E,
        "Lavanda Real" to 0xFF6C4AB6,
        "Violeta Neon" to 0xFF8B5CF6,
        "Oceano" to 0xFF0077B6,
        "Turquesa" to 0xFF06B6D4,
        "Rosé Afeto" to 0xFFD84B72,
        "Coral Vibrante" to 0xFFE15554,
        "Café Torrado" to 0xFF7F5539
    )
}

class CustomizationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadFromPrefs())
    val configFlow: StateFlow<AppCustomizationConfig> = _configFlow.asStateFlow()

    private fun loadFromPrefs(): AppCustomizationConfig {
        val defaultTaskUri = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
        val defaultBillUri = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }

        val presetId = prefs.getString(KEY_PRESET_ID, "EMERALD_GOLD") ?: "EMERALD_GOLD"
        val preset = CustomizationCatalog.presets.firstOrNull { it.id == presetId }
            ?: CustomizationCatalog.presets.first()

        val savedOrderRaw = prefs.getString(KEY_HUB_WIDGET_ORDER, null)
        val parsedOrder = savedOrderRaw
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it in HubWidgetIds.defaultOrder }
            ?.distinct()
            .orEmpty()
        val completeOrder = (parsedOrder + HubWidgetIds.defaultOrder).distinct()

        val savedEnabledRaw = prefs.getString(KEY_HUB_ENABLED_WIDGETS, null)
        val enabledSet = if (savedEnabledRaw == null) {
            HubWidgetIds.defaultOrder.toSet()
        } else {
            val baseSet = savedEnabledRaw.split(",")
                .map { it.trim() }
                .filter { it in HubWidgetIds.defaultOrder }
                .toMutableSet()
            if (savedOrderRaw != null && !savedOrderRaw.contains(HubWidgetIds.HEALTH_SUMMARY)) {
                baseSet.add(HubWidgetIds.HEALTH_SUMMARY)
            }
            if (savedOrderRaw != null && !savedOrderRaw.contains(HubWidgetIds.ACHIEVEMENTS_MEDALS)) {
                baseSet.add(HubWidgetIds.ACHIEVEMENTS_MEDALS)
            }
            baseSet
        }

        val savedPhotoPath = prefs.getString(KEY_PROFILE_PHOTO_PATH, "") ?: ""
        val validPhotoPath = if (savedPhotoPath.isNotBlank() && File(savedPhotoPath).exists()) {
            savedPhotoPath
        } else {
            ""
        }

        val savedDiabetesSlotsRaw = prefs.getString(KEY_DIABETES_SLOTS, null)
        val diabetesSlotsSet = if (savedDiabetesSlotsRaw == null) {
            DiabetesSlotCatalog.defaultSelectedSlots
        } else {
            savedDiabetesSlotsRaw.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
                .ifEmpty { DiabetesSlotCatalog.defaultSelectedSlots }
        }

        val dailyStatesMap = deserializeDailyStates(prefs.getString(KEY_DAILY_STATES_JSON, null))

        return AppCustomizationConfig(
            presetId = presetId,
            useCustomColors = prefs.getBoolean(KEY_USE_CUSTOM_COLORS, false),
            backgroundHex = prefs.getLong(KEY_BG_HEX, preset.backgroundHex),
            cardHex = prefs.getLong(KEY_CARD_HEX, preset.cardHex),
            textHex = prefs.getLong(KEY_TEXT_HEX, preset.textHex),
            primaryHex = prefs.getLong(KEY_PRIMARY_HEX, preset.primaryHex),
            accentHex = prefs.getLong(KEY_ACCENT_HEX, preset.accentHex),
            fontFamilyId = prefs.getString(KEY_FONT_ID, "NUNITO") ?: "NUNITO",
            cardCornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS, 34),
            organicVisualStyle = prefs.getString(KEY_ORGANIC_VISUAL_STYLE, OrganicVisualStyle.ORGANICO_FLUIDO)
                ?: OrganicVisualStyle.ORGANICO_FLUIDO,
            hubColorHex = prefs.getLong(KEY_HUB_COLOR_HEX, preset.hubColorHex),
            agendaColorHex = prefs.getLong(KEY_AGENDA_COLOR_HEX, prefs.getLong(KEY_PROFILE_COLOR_HEX, preset.agendaColorHex)),
            healthColorHex = prefs.getLong(KEY_HEALTH_COLOR_HEX, preset.healthColorHex),
            diaryColorHex = prefs.getLong(KEY_DIARY_COLOR_HEX, preset.diaryColorHex),
            financeColorHex = prefs.getLong(KEY_FINANCE_COLOR_HEX, preset.financeColorHex),
            taskAlarmSoundUri = prefs.getString(KEY_TASK_SOUND_URI, defaultTaskUri) ?: defaultTaskUri,
            taskAlarmSoundTitle = prefs.getString(KEY_TASK_SOUND_TITLE, "Alarme Padrão do Sistema")
                ?: "Alarme Padrão do Sistema",
            taskVibrateEnabled = prefs.getBoolean(KEY_TASK_VIBRATE, true),
            billReminderSoundUri = prefs.getString(KEY_BILL_SOUND_URI, defaultBillUri) ?: defaultBillUri,
            billReminderSoundTitle = prefs.getString(KEY_BILL_SOUND_TITLE, "Notificação Padrão do Sistema")
                ?: "Notificação Padrão do Sistema",
            billVibrateEnabled = prefs.getBoolean(KEY_BILL_VIBRATE, true),
            profileName = prefs.getString(KEY_PROFILE_NAME, "Você") ?: "Você",
            profileColorHex = prefs.getLong(KEY_PROFILE_COLOR_HEX, prefs.getLong(KEY_AGENDA_COLOR_HEX, 0xFFE88378)),
            profilePhotoPath = validPhotoPath,
            profilePhotoVersion = prefs.getLong(KEY_PROFILE_PHOTO_VERSION, 0L),
            agendaLayoutMode = prefs.getString(KEY_AGENDA_LAYOUT_MODE, AgendaLayoutMode.COMPLETO)
                ?: AgendaLayoutMode.COMPLETO,
            hubWidgetOrder = completeOrder,
            enabledHubWidgets = enabledSet,
            hasCompletedModularOnboarding = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false),
            routineRhythm = prefs.getString(KEY_ROUTINE_RHYTHM, RoutineRhythm.ELASTICA) ?: RoutineRhythm.ELASTICA,
            fixedWakeTime = prefs.getString(KEY_FIXED_WAKE_TIME, "07:00") ?: "07:00",
            fixedSleepTime = prefs.getString(KEY_FIXED_SLEEP_TIME, "22:30") ?: "22:30",
            energyMorning = prefs.getString(KEY_ENERGY_MORNING, EnergyLevel.ALTA) ?: EnergyLevel.ALTA,
            energyAfternoon = prefs.getString(KEY_ENERGY_AFTERNOON, EnergyLevel.MEDIA) ?: EnergyLevel.MEDIA,
            energyNight = prefs.getString(KEY_ENERGY_NIGHT, EnergyLevel.BAIXA) ?: EnergyLevel.BAIXA,
            pregnancyEnabled = prefs.getBoolean(KEY_PREGNANCY_ENABLED, false),
            pregnancyDppDate = prefs.getString(KEY_PREGNANCY_DPP, "") ?: "",
            diabetesEnabled = prefs.getBoolean(KEY_DIABETES_ENABLED, false),
            diabetesSlots = diabetesSlotsSet,
            bloodPressureEnabled = prefs.getBoolean(KEY_BP_ENABLED, false),
            bloodPressureReminderTime = prefs.getString(KEY_BP_REMINDER_TIME, "21:30 (Antes de dormir)")
                ?: "21:30 (Antes de dormir)",
            chronicPainFatigueEnabled = prefs.getBoolean(KEY_PAIN_FATIGUE_ENABLED, false),
            hourlyMoodEnabled = prefs.getBoolean(KEY_HOURLY_MOOD_ENABLED, true),
            threeShiftQuotesEnabled = prefs.getBoolean(KEY_THREE_SHIFT_QUOTES_ENABLED, true),
            aiSupportLetterEnabled = prefs.getBoolean(KEY_AI_SUPPORT_LETTER_ENABLED, true),
            currentStreakDays = prefs.getInt(KEY_CURRENT_STREAK_DAYS, 5),
            dailyStatesByDate = dailyStatesMap
        )
    }

    private fun serializeDailyStates(map: Map<String, DailyState>): String {
        val root = org.json.JSONObject()
        map.forEach { (dateIso, state) ->
            val obj = org.json.JSONObject().apply {
                put("dateIso", state.dateIso)
                put("wokeUpAt", state.wokeUpAt)
                put("wokeUpTimestampMillis", state.wokeUpTimestampMillis)
                put("hardDayModeActive", state.hardDayModeActive)
                put("streakFrozen", state.streakFrozen)
                put("updatedAtMillis", state.updatedAtMillis)
            }
            root.put(dateIso, obj)
        }
        return root.toString()
    }

    private fun deserializeDailyStates(raw: String?): Map<String, DailyState> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val root = org.json.JSONObject(raw)
            val result = mutableMapOf<String, DailyState>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val dateIso = keys.next()
                val obj = root.optJSONObject(dateIso) ?: continue
                result[dateIso] = DailyState(
                    dateIso = obj.optString("dateIso", dateIso),
                    wokeUpAt = obj.optString("wokeUpAt", ""),
                    wokeUpTimestampMillis = obj.optLong("wokeUpTimestampMillis", 0L),
                    hardDayModeActive = obj.optBoolean("hardDayModeActive", false),
                    streakFrozen = obj.optBoolean("streakFrozen", false),
                    updatedAtMillis = obj.optLong("updatedAtMillis", 0L)
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveAndEmit(updated: AppCustomizationConfig) {
        prefs.edit()
            .putString(KEY_PRESET_ID, updated.presetId)
            .putBoolean(KEY_USE_CUSTOM_COLORS, updated.useCustomColors)
            .putLong(KEY_BG_HEX, updated.backgroundHex)
            .putLong(KEY_CARD_HEX, updated.cardHex)
            .putLong(KEY_TEXT_HEX, updated.textHex)
            .putLong(KEY_PRIMARY_HEX, updated.primaryHex)
            .putLong(KEY_ACCENT_HEX, updated.accentHex)
            .putString(KEY_FONT_ID, updated.fontFamilyId)
            .putInt(KEY_CORNER_RADIUS, updated.cardCornerRadiusDp)
            .putString(KEY_ORGANIC_VISUAL_STYLE, updated.organicVisualStyle)
            .putLong(KEY_HUB_COLOR_HEX, updated.hubColorHex)
            .putLong(KEY_AGENDA_COLOR_HEX, updated.agendaColorHex)
            .putLong(KEY_HEALTH_COLOR_HEX, updated.healthColorHex)
            .putLong(KEY_DIARY_COLOR_HEX, updated.diaryColorHex)
            .putLong(KEY_FINANCE_COLOR_HEX, updated.financeColorHex)
            .putString(KEY_TASK_SOUND_URI, updated.taskAlarmSoundUri)
            .putString(KEY_TASK_SOUND_TITLE, updated.taskAlarmSoundTitle)
            .putBoolean(KEY_TASK_VIBRATE, updated.taskVibrateEnabled)
            .putString(KEY_BILL_SOUND_URI, updated.billReminderSoundUri)
            .putString(KEY_BILL_SOUND_TITLE, updated.billReminderSoundTitle)
            .putBoolean(KEY_BILL_VIBRATE, updated.billVibrateEnabled)
            .putString(KEY_PROFILE_NAME, updated.profileName)
            .putLong(KEY_PROFILE_COLOR_HEX, updated.profileColorHex)
            .putString(KEY_PROFILE_PHOTO_PATH, updated.profilePhotoPath)
            .putLong(KEY_PROFILE_PHOTO_VERSION, updated.profilePhotoVersion)
            .putString(KEY_AGENDA_LAYOUT_MODE, updated.agendaLayoutMode)
            .putString(KEY_HUB_WIDGET_ORDER, updated.hubWidgetOrder.joinToString(","))
            .putString(KEY_HUB_ENABLED_WIDGETS, updated.enabledHubWidgets.joinToString(","))
            .putBoolean(KEY_HAS_COMPLETED_ONBOARDING, updated.hasCompletedModularOnboarding)
            .putString(KEY_ROUTINE_RHYTHM, updated.routineRhythm)
            .putString(KEY_FIXED_WAKE_TIME, updated.fixedWakeTime)
            .putString(KEY_FIXED_SLEEP_TIME, updated.fixedSleepTime)
            .putString(KEY_ENERGY_MORNING, updated.energyMorning)
            .putString(KEY_ENERGY_AFTERNOON, updated.energyAfternoon)
            .putString(KEY_ENERGY_NIGHT, updated.energyNight)
            .putBoolean(KEY_PREGNANCY_ENABLED, updated.pregnancyEnabled)
            .putString(KEY_PREGNANCY_DPP, updated.pregnancyDppDate)
            .putBoolean(KEY_DIABETES_ENABLED, updated.diabetesEnabled)
            .putString(KEY_DIABETES_SLOTS, updated.diabetesSlots.joinToString("|"))
            .putBoolean(KEY_BP_ENABLED, updated.bloodPressureEnabled)
            .putString(KEY_BP_REMINDER_TIME, updated.bloodPressureReminderTime)
            .putBoolean(KEY_PAIN_FATIGUE_ENABLED, updated.chronicPainFatigueEnabled)
            .putBoolean(KEY_HOURLY_MOOD_ENABLED, updated.hourlyMoodEnabled)
            .putBoolean(KEY_THREE_SHIFT_QUOTES_ENABLED, updated.threeShiftQuotesEnabled)
            .putBoolean(KEY_AI_SUPPORT_LETTER_ENABLED, updated.aiSupportLetterEnabled)
            .putInt(KEY_CURRENT_STREAK_DAYS, updated.currentStreakDays)
            .putString(KEY_DAILY_STATES_JSON, serializeDailyStates(updated.dailyStatesByDate))
            .apply()
        _configFlow.value = updated
    }

    fun updateAgendaLayoutMode(layoutMode: String) {
        val valid = when (layoutMode) {
            AgendaLayoutMode.COMPLETO,
            AgendaLayoutMode.SIMPLIFICADO,
            AgendaLayoutMode.MINIMO -> layoutMode
            else -> AgendaLayoutMode.COMPLETO
        }
        val current = _configFlow.value
        saveAndEmit(current.copy(agendaLayoutMode = valid))
    }

    fun updateOrganicVisualStyle(style: String) {
        val valid = when (style) {
            OrganicVisualStyle.ORGANICO_FLUIDO,
            OrganicVisualStyle.TRANSLUCIDO_GLASS,
            OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> style
            else -> OrganicVisualStyle.ORGANICO_FLUIDO
        }
        val radius = when (valid) {
            OrganicVisualStyle.ORGANICO_FLUIDO -> 34
            OrganicVisualStyle.TRANSLUCIDO_GLASS -> 30
            OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 28
            else -> 34
        }
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                organicVisualStyle = valid,
                cardCornerRadiusDp = radius
            )
        )
    }

    fun updateModuleColor(moduleId: String, colorHex: Long) {
        val current = _configFlow.value
        val updated = when (moduleId) {
            AppModuleId.HUB -> current.copy(hubColorHex = colorHex, primaryHex = colorHex)
            AppModuleId.AGENDA -> current.copy(agendaColorHex = colorHex, profileColorHex = colorHex)
            AppModuleId.HEALTH -> current.copy(healthColorHex = colorHex)
            AppModuleId.DIARY -> current.copy(diaryColorHex = colorHex)
            AppModuleId.FINANCE -> current.copy(financeColorHex = colorHex)
            else -> current.copy(primaryHex = colorHex, profileColorHex = colorHex)
        }
        saveAndEmit(updated)
    }

    fun updateStructuredAppColor(colorHex: Long) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                useCustomColors = true,
                primaryHex = colorHex,
                agendaColorHex = colorHex,
                profileColorHex = colorHex
            )
        )
    }

    // =========================================================================
    // PASSO 1: MÉTODOS DE PERFIL MODULAR E ESTADO DIÁRIO (daily_state)
    // =========================================================================

    fun completeModularOnboarding() {
        val current = _configFlow.value
        saveAndEmit(current.copy(hasCompletedModularOnboarding = true))
    }

    fun updateRoutineRhythm(
        rhythm: String,
        fixedWakeTime: String = _configFlow.value.fixedWakeTime,
        fixedSleepTime: String = _configFlow.value.fixedSleepTime
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                routineRhythm = rhythm,
                fixedWakeTime = fixedWakeTime,
                fixedSleepTime = fixedSleepTime
            )
        )
    }

    fun updateEnergyMap(
        morning: String = _configFlow.value.energyMorning,
        afternoon: String = _configFlow.value.energyAfternoon,
        night: String = _configFlow.value.energyNight
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                energyMorning = morning,
                energyAfternoon = afternoon,
                energyNight = night
            )
        )
    }

    fun updatePregnancyModule(
        enabled: Boolean,
        dppDate: String = _configFlow.value.pregnancyDppDate
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                pregnancyEnabled = enabled,
                pregnancyDppDate = dppDate
            )
        )
    }

    fun updateDiabetesModule(
        enabled: Boolean,
        slots: Set<String> = _configFlow.value.diabetesSlots
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                diabetesEnabled = enabled,
                diabetesSlots = slots
            )
        )
    }

    fun toggleDiabetesSlot(slot: String) {
        val current = _configFlow.value
        val updatedSlots = current.diabetesSlots.toMutableSet()
        if (slot in updatedSlots) {
            if (updatedSlots.size > 1) updatedSlots.remove(slot)
        } else {
            updatedSlots.add(slot)
        }
        saveAndEmit(current.copy(diabetesSlots = updatedSlots))
    }

    fun updateBloodPressureModule(
        enabled: Boolean,
        reminderTime: String = _configFlow.value.bloodPressureReminderTime
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                bloodPressureEnabled = enabled,
                bloodPressureReminderTime = reminderTime
            )
        )
    }

    fun updateChronicPainFatigueModule(enabled: Boolean) {
        val current = _configFlow.value
        saveAndEmit(current.copy(chronicPainFatigueEnabled = enabled))
    }

    fun updateDiaryAndSupportModules(
        hourlyMoodEnabled: Boolean = _configFlow.value.hourlyMoodEnabled,
        threeShiftQuotesEnabled: Boolean = _configFlow.value.threeShiftQuotesEnabled,
        aiSupportLetterEnabled: Boolean = _configFlow.value.aiSupportLetterEnabled
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                hourlyMoodEnabled = hourlyMoodEnabled,
                threeShiftQuotesEnabled = threeShiftQuotesEnabled,
                aiSupportLetterEnabled = aiSupportLetterEnabled
            )
        )
    }

    /**
     * Registra a hora exata (woke_up_at) no daily_state da data YYYY-MM-DD quando o usuário clica em "Acordei".
     */
    fun recordWokeUp(dateIso: String, wokeUpTimeHHmm: String = DateCalendarUtils.currentTimeHHmm()) {
        val current = _configFlow.value
        val existing = current.dailyStateFor(dateIso)
        val now = System.currentTimeMillis()
        val updatedState = existing.copy(
            dateIso = dateIso,
            wokeUpAt = wokeUpTimeHHmm,
            wokeUpTimestampMillis = now,
            updatedAtMillis = now
        )
        val updatedMap = current.dailyStatesByDate.toMutableMap().apply {
            put(dateIso, updatedState)
        }
        saveAndEmit(current.copy(dailyStatesByDate = updatedMap))
    }

    fun resetWokeUpForDate(dateIso: String) {
        val current = _configFlow.value
        val existing = current.dailyStateFor(dateIso)
        val updatedState = existing.copy(
            wokeUpAt = "",
            wokeUpTimestampMillis = 0L,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedMap = current.dailyStatesByDate.toMutableMap().apply {
            put(dateIso, updatedState)
        }
        saveAndEmit(current.copy(dailyStatesByDate = updatedMap))
    }

    /**
     * Ativa ou desativa o "Modo Dia Difícil" no daily_state da data YYYY-MM-DD,
     * congelando o contador de sequência (streak) sem penalidade.
     */
    fun toggleHardDayMode(dateIso: String): Boolean {
        val current = _configFlow.value
        val existing = current.dailyStateFor(dateIso)
        val newActive = !existing.hardDayModeActive
        val now = System.currentTimeMillis()
        val updatedState = existing.copy(
            dateIso = dateIso,
            hardDayModeActive = newActive,
            streakFrozen = newActive,
            updatedAtMillis = now
        )
        val updatedMap = current.dailyStatesByDate.toMutableMap().apply {
            put(dateIso, updatedState)
        }
        saveAndEmit(current.copy(dailyStatesByDate = updatedMap))
        return newActive
    }

    fun updateProfileIdentity(
        name: String = _configFlow.value.profileName,
        colorHex: Long = _configFlow.value.profileColorHex
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                profileName = name.trim().ifBlank { "Você" },
                profileColorHex = colorHex,
                agendaColorHex = colorHex
            )
        )
    }

    fun saveProfilePhotoFromUri(sourceUri: Uri): Boolean {
        return try {
            val destFile = File(appContext.filesDir, "profile_avatar.jpg")
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val current = _configFlow.value
            saveAndEmit(
                current.copy(
                    profilePhotoPath = destFile.absolutePath,
                    profilePhotoVersion = System.currentTimeMillis()
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearProfilePhoto() {
        try {
            val destFile = File(appContext.filesDir, "profile_avatar.jpg")
            if (destFile.exists()) destFile.delete()
        } catch (_: Exception) {
        }
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                profilePhotoPath = "",
                profilePhotoVersion = System.currentTimeMillis()
            )
        )
    }

    fun toggleHubWidget(widgetId: String, enabled: Boolean) {
        val current = _configFlow.value
        val updatedSet = current.enabledHubWidgets.toMutableSet()
        if (enabled) {
            updatedSet.add(widgetId)
        } else if (updatedSet.size > 1) {
            // Keep at least 1 widget active on the Hub
            updatedSet.remove(widgetId)
        }
        saveAndEmit(current.copy(enabledHubWidgets = updatedSet))
    }

    fun moveHubWidget(widgetId: String, direction: Int) {
        val current = _configFlow.value
        val list = current.hubWidgetOrder.toMutableList()
        val index = list.indexOf(widgetId)
        if (index == -1) return
        val newIndex = (index + direction).coerceIn(0, list.lastIndex)
        if (newIndex == index) return
        val item = list.removeAt(index)
        list.add(newIndex, item)
        saveAndEmit(current.copy(hubWidgetOrder = list))
    }

    fun resetHubWidgetsToDefault() {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                hubWidgetOrder = HubWidgetIds.defaultOrder,
                enabledHubWidgets = HubWidgetIds.defaultOrder.toSet()
            )
        )
    }

    fun applyThemePreset(preset: ThemePresetInfo) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                presetId = preset.id,
                useCustomColors = false,
                backgroundHex = preset.backgroundHex,
                cardHex = preset.cardHex,
                textHex = preset.textHex,
                primaryHex = preset.primaryHex,
                accentHex = preset.accentHex,
                hubColorHex = preset.hubColorHex,
                agendaColorHex = preset.agendaColorHex,
                healthColorHex = preset.healthColorHex,
                diaryColorHex = preset.diaryColorHex,
                financeColorHex = preset.financeColorHex,
                profileColorHex = preset.agendaColorHex
            )
        )
    }

    fun updateCustomColor(
        backgroundHex: Long = _configFlow.value.backgroundHex,
        cardHex: Long = _configFlow.value.cardHex,
        textHex: Long = _configFlow.value.textHex,
        primaryHex: Long = _configFlow.value.primaryHex,
        accentHex: Long = _configFlow.value.accentHex
    ) {
        val current = _configFlow.value
        saveAndEmit(
            current.copy(
                presetId = "CUSTOM",
                useCustomColors = true,
                backgroundHex = backgroundHex,
                cardHex = cardHex,
                textHex = textHex,
                primaryHex = primaryHex,
                accentHex = accentHex
            )
        )
    }

    fun updateFontFamily(fontId: String) {
        val current = _configFlow.value
        saveAndEmit(current.copy(fontFamilyId = fontId))
    }

    fun updateCardCornerRadius(radiusDp: Int) {
        val current = _configFlow.value
        saveAndEmit(current.copy(cardCornerRadiusDp = radiusDp.coerceIn(18, 40)))
    }

    fun updateTaskSound(uriString: String, title: String) {
        val current = _configFlow.value
        saveAndEmit(current.copy(taskAlarmSoundUri = uriString, taskAlarmSoundTitle = title))
    }

    fun updateTaskVibrate(enabled: Boolean) {
        val current = _configFlow.value
        saveAndEmit(current.copy(taskVibrateEnabled = enabled))
    }

    fun updateBillSound(uriString: String, title: String) {
        val current = _configFlow.value
        saveAndEmit(current.copy(billReminderSoundUri = uriString, billReminderSoundTitle = title))
    }

    fun updateBillVibrate(enabled: Boolean) {
        val current = _configFlow.value
        saveAndEmit(current.copy(billVibrateEnabled = enabled))
    }

    fun resolveRingtoneTitle(uri: Uri?): String {
        if (uri == null) return "Silencioso"
        return try {
            val ringtone = RingtoneManager.getRingtone(appContext, uri)
            ringtone?.getTitle(appContext)?.takeIf { it.isNotBlank() } ?: "Toque Personalizado"
        } catch (_: Exception) {
            "Toque Selecionado"
        }
    }

    companion object {
        private const val PREFS_NAME = "aura_customization_prefs"
        private const val KEY_PRESET_ID = "preset_id"
        private const val KEY_USE_CUSTOM_COLORS = "use_custom_colors"
        private const val KEY_BG_HEX = "bg_hex"
        private const val KEY_CARD_HEX = "card_hex"
        private const val KEY_TEXT_HEX = "text_hex"
        private const val KEY_PRIMARY_HEX = "primary_hex"
        private const val KEY_ACCENT_HEX = "accent_hex"
        private const val KEY_FONT_ID = "font_id"
        private const val KEY_CORNER_RADIUS = "corner_radius_dp"
        private const val KEY_TASK_SOUND_URI = "task_sound_uri"
        private const val KEY_TASK_SOUND_TITLE = "task_sound_title"
        private const val KEY_TASK_VIBRATE = "task_vibrate"
        private const val KEY_BILL_SOUND_URI = "bill_sound_uri"
        private const val KEY_BILL_SOUND_TITLE = "bill_sound_title"
        private const val KEY_BILL_VIBRATE = "bill_vibrate"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_COLOR_HEX = "profile_color_hex"
        private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"
        private const val KEY_PROFILE_PHOTO_VERSION = "profile_photo_version"
        private const val KEY_HUB_WIDGET_ORDER = "hub_widget_order"
        private const val KEY_HUB_ENABLED_WIDGETS = "hub_enabled_widgets"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_modular_onboarding"
        private const val KEY_ROUTINE_RHYTHM = "routine_rhythm"
        private const val KEY_FIXED_WAKE_TIME = "fixed_wake_time"
        private const val KEY_FIXED_SLEEP_TIME = "fixed_sleep_time"
        private const val KEY_ENERGY_MORNING = "energy_morning"
        private const val KEY_ENERGY_AFTERNOON = "energy_afternoon"
        private const val KEY_ENERGY_NIGHT = "energy_night"
        private const val KEY_PREGNANCY_ENABLED = "pregnancy_enabled"
        private const val KEY_PREGNANCY_DPP = "pregnancy_dpp"
        private const val KEY_DIABETES_ENABLED = "diabetes_enabled"
        private const val KEY_DIABETES_SLOTS = "diabetes_slots"
        private const val KEY_BP_ENABLED = "bp_enabled"
        private const val KEY_BP_REMINDER_TIME = "bp_reminder_time"
        private const val KEY_PAIN_FATIGUE_ENABLED = "pain_fatigue_enabled"
        private const val KEY_HOURLY_MOOD_ENABLED = "hourly_mood_enabled"
        private const val KEY_THREE_SHIFT_QUOTES_ENABLED = "three_shift_quotes_enabled"
        private const val KEY_AI_SUPPORT_LETTER_ENABLED = "ai_support_letter_enabled"
        private const val KEY_CURRENT_STREAK_DAYS = "current_streak_days"
        private const val KEY_DAILY_STATES_JSON = "daily_states_json"
        private const val KEY_AGENDA_LAYOUT_MODE = "agenda_layout_mode"
        private const val KEY_ORGANIC_VISUAL_STYLE = "organic_visual_style"
        private const val KEY_HUB_COLOR_HEX = "hub_color_hex"
        private const val KEY_AGENDA_COLOR_HEX = "agenda_color_hex"
        private const val KEY_HEALTH_COLOR_HEX = "health_color_hex"
        private const val KEY_DIARY_COLOR_HEX = "diary_color_hex"
        private const val KEY_FINANCE_COLOR_HEX = "finance_color_hex"
    }
}
