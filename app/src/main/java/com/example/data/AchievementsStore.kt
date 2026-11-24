package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Pilar / Módulo ao qual a conquista e a medalha estão vinculadas.
 */
enum class AchievementModule(
    val id: String,
    val label: String,
    val shortLabel: String,
    val emoji: String
) {
    HEALTH("HEALTH", "Saúde & Inegociáveis", "Saúde", "🩺"),
    FINANCE("FINANCE", "Financeiro & Economia", "Financeiro", "💰"),
    CALENDAR("CALENDAR", "Calendário & Agenda", "Calendário", "📅"),
    TRINITY("TRINITY", "Tríade Omni 360°", "Tríade 360°", "🏆");

    companion object {
        fun fromId(id: String): AchievementModule {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HEALTH
        }
    }
}

/**
 * Nível da Medalha ganha automaticamente ao atingir o objetivo real no aplicativo.
 */
enum class MedalTier(
    val id: String,
    val label: String,
    val badgeTitle: String,
    val emoji: String,
    val xpReward: Int,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val ribbonColorHex: Long
) {
    BRONZE(
        id = "BRONZE",
        label = "Bronze",
        badgeTitle = "Medalha de Bronze",
        emoji = "🥉",
        xpReward = 60,
        primaryColorHex = 0xFFCD7F32,
        secondaryColorHex = 0xFFE8A568,
        ribbonColorHex = 0xFF0B6E4F
    ),
    SILVER(
        id = "SILVER",
        label = "Prata",
        badgeTitle = "Medalha de Prata",
        emoji = "🥈",
        xpReward = 120,
        primaryColorHex = 0xFFB4C2CC,
        secondaryColorHex = 0xFFE8EEF2,
        ribbonColorHex = 0xFF0971E6
    ),
    GOLD(
        id = "GOLD",
        label = "Ouro",
        badgeTitle = "Medalha de Ouro",
        emoji = "🥇",
        xpReward = 220,
        primaryColorHex = 0xFFD4AF37,
        secondaryColorHex = 0xFFFFE57F,
        ribbonColorHex = 0xFFD84B72
    ),
    DIAMOND(
        id = "DIAMOND",
        label = "Diamante",
        badgeTitle = "Medalha de Diamante",
        emoji = "💎",
        xpReward = 360,
        primaryColorHex = 0xFF38BDF8,
        secondaryColorHex = 0xFFE0F2FE,
        ribbonColorHex = 0xFF8B5CF6
    );

    companion object {
        fun fromId(id: String): MedalTier {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BRONZE
        }
    }
}

/**
 * Objetivo personalizado criado pelo usuário vinculado a Saúde, Financeiro ou Calendário.
 * O progresso é calculado 100% automaticamente a partir das ações reais do usuário no módulo.
 */
data class CustomAchievementGoal(
    val id: String,
    val title: String,
    val description: String,
    val module: AchievementModule,
    val medalTier: MedalTier,
    val iconEmoji: String,
    val currentValue: Int,
    val targetValue: Int,
    val unitLabel: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Representa uma Conquista / Objetivo avaliado automaticamente com seu progresso real e Medalha correspondente.
 */
data class AchievementGoal(
    val id: String,
    val title: String,
    val description: String,
    val module: AchievementModule,
    val medalTier: MedalTier,
    val iconEmoji: String,
    val currentValue: Int,
    val targetValue: Int,
    val unitLabel: String,
    val isUnlocked: Boolean,
    val unlockedAtMillis: Long = 0L,
    val isCustomGoal: Boolean = false
) {
    val progressRatio: Float
        get() = if (targetValue <= 0) 1f else (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)

    val progressPercentage: Int
        get() = (progressRatio * 100f).roundToInt().coerceIn(0, 100)
}

/**
 * Resumo consolidado do Sistema de Conquistas e Medalhas 100% Automático.
 */
data class AchievementsEvaluationSummary(
    val goals: List<AchievementGoal> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val levelTitle: String = "Iniciante Omni",
    val currentLevelProgressRatio: Float = 0f,
    val xpIntoCurrentLevel: Int = 0,
    val xpRequiredForNextLevel: Int = 250,
    val bronzeUnlocked: Int = 0,
    val silverUnlocked: Int = 0,
    val goldUnlocked: Int = 0,
    val diamondUnlocked: Int = 0,
    val healthUnlocked: Int = 0,
    val healthTotal: Int = 0,
    val financeUnlocked: Int = 0,
    val financeTotal: Int = 0,
    val calendarUnlocked: Int = 0,
    val calendarTotal: Int = 0,
    val newlyUnlockedGoalTitle: String? = null
)

/**
 * Estado persistido do repositório de conquistas (datas de desbloqueio + objetivos personalizados).
 */
data class PersistedAchievementsState(
    val unlockedTimestamps: Map<String, Long> = emptyMap(),
    val customGoals: List<CustomAchievementGoal> = emptyList(),
    val lastCelebratedGoalId: String = ""
)

class AchievementsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _persistedStateFlow = MutableStateFlow(loadPersistedState())
    val persistedStateFlow: StateFlow<PersistedAchievementsState> = _persistedStateFlow.asStateFlow()

    private fun loadPersistedState(): PersistedAchievementsState {
        val raw = prefs.getString(KEY_STATE_JSON, null) ?: return PersistedAchievementsState()
        return try {
            val root = JSONObject(raw)
            val unlockedMap = mutableMapOf<String, Long>()
            val unlockedObj = root.optJSONObject("unlockedTimestamps")
            if (unlockedObj != null) {
                val keys = unlockedObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    unlockedMap[k] = unlockedObj.optLong(k, System.currentTimeMillis())
                }
            }

            val customList = mutableListOf<CustomAchievementGoal>()
            val customArr = root.optJSONArray("customGoals") ?: JSONArray()
            for (i in 0 until customArr.length()) {
                val item = customArr.optJSONObject(i) ?: continue
                customList.add(
                    CustomAchievementGoal(
                        id = item.optString("id", "custom_$i"),
                        title = item.optString("title", "Objetivo Personalizado"),
                        description = item.optString("description", ""),
                        module = AchievementModule.fromId(item.optString("module", "HEALTH")),
                        medalTier = MedalTier.fromId(item.optString("medalTier", "GOLD")),
                        iconEmoji = item.optString("iconEmoji", "🏅"),
                        currentValue = 0,
                        targetValue = item.optInt("targetValue", 1).coerceAtLeast(1),
                        unitLabel = item.optString("unitLabel", "etapas"),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            PersistedAchievementsState(
                unlockedTimestamps = unlockedMap,
                customGoals = customList,
                lastCelebratedGoalId = root.optString("lastCelebratedGoalId", "")
            )
        } catch (_: Exception) {
            PersistedAchievementsState()
        }
    }

    private fun savePersistedState(state: PersistedAchievementsState) {
        val root = JSONObject()

        val unlockedObj = JSONObject()
        state.unlockedTimestamps.forEach { (id, ts) -> unlockedObj.put(id, ts) }
        root.put("unlockedTimestamps", unlockedObj)

        val customArr = JSONArray()
        state.customGoals.forEach { g ->
            customArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("description", g.description)
                    put("module", g.module.id)
                    put("medalTier", g.medalTier.id)
                    put("iconEmoji", g.iconEmoji)
                    put("currentValue", g.currentValue)
                    put("targetValue", g.targetValue)
                    put("unitLabel", g.unitLabel)
                    put("createdAtMillis", g.createdAtMillis)
                }
            )
        }
        root.put("customGoals", customArr)
        root.put("lastCelebratedGoalId", state.lastCelebratedGoalId)

        prefs.edit().putString(KEY_STATE_JSON, root.toString()).apply()
        _persistedStateFlow.value = state
    }

    /**
     * Cria um novo objetivo personalizado vinculado a Saúde, Financeiro ou Calendário com Medalha à escolha.
     * O progresso será contabilizado automaticamente pelas ações reais no módulo escolhido.
     */
    fun addCustomAchievementGoal(
        title: String,
        description: String,
        module: AchievementModule,
        medalTier: MedalTier,
        targetValue: Int,
        unitLabel: String
    ): CustomAchievementGoal {
        val cleanTitle = title.trim().ifBlank { "Meta Automática ${module.shortLabel}" }
        val cleanDesc = description.trim().ifBlank {
            "Conquista monitorada automaticamente pelas suas ações reais no módulo de ${module.shortLabel}."
        }
        val emoji = when (module) {
            AchievementModule.HEALTH -> "🩺"
            AchievementModule.FINANCE -> "💰"
            AchievementModule.CALENDAR -> "📅"
            AchievementModule.TRINITY -> "🏆"
        }
        val goal = CustomAchievementGoal(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle,
            description = cleanDesc,
            module = module,
            medalTier = medalTier,
            iconEmoji = emoji,
            currentValue = 0,
            targetValue = targetValue.coerceIn(1, 100000),
            unitLabel = unitLabel.trim().ifBlank { "registros" }
        )
        val current = _persistedStateFlow.value
        savePersistedState(
            current.copy(customGoals = listOf(goal) + current.customGoals)
        )
        return goal
    }

    fun deleteCustomGoal(goalId: String) {
        val current = _persistedStateFlow.value
        val updatedUnlocked = current.unlockedTimestamps.toMutableMap().apply { remove(goalId) }
        savePersistedState(
            current.copy(
                unlockedTimestamps = updatedUnlocked,
                customGoals = current.customGoals.filterNot { it.id == goalId }
            )
        )
    }

    /**
     * Avalia todas as 28+ conquistas em tempo real exclusivamente a partir dos dados reais de Saúde,
     * Financeiro e Calendário — sem botões manuais de atalho, garantindo mérito real para cada medalha.
     */
    fun evaluateAchievements(
        healthState: HealthModuleState,
        customizationConfig: AppCustomizationConfig,
        tasks: List<TaskItem>,
        transactions: List<FinanceTransaction>,
        subscriptions: List<SubscriptionBill>,
        savingsGoals: List<SavingsGoal>,
        household: Household,
        selectedYearMonth: String
    ): AchievementsEvaluationSummary {
        val persisted = _persistedStateFlow.value
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val todayIso = DateCalendarUtils.formatIsoDate(todayEpoch)

        // =====================================================================
        // 1. MÉTRICAS REAIS AUTOMÁTICAS DE SAÚDE (7 Medalhas)
        // =====================================================================
        val totalTrackers = healthState.trackers.size.coerceAtLeast(1)
        val completedTrackersToday = healthState.completedTrackersCount(todayIso)
        val totalTrackerCheckinsAllDays = healthState.trackerProgressByDate.values.sumOf { dayMap ->
            dayMap.values.count { it > 0 }
        }
        val activeHealthDaysCount = healthState.trackerProgressByDate.count { (_, dayMap) ->
            dayMap.values.any { it > 0 }
        }
        val clinicalRecordsCount =
            (if (customizationConfig.diabetesEnabled) healthState.glucoseRecords.size else 0) +
                (if (customizationConfig.bloodPressureEnabled) healthState.bloodPressureRecords.size else 0) +
                (if (customizationConfig.pregnancyEnabled) healthState.pregnancyChecklist.count { it.completed } else 0)
        val totalClinicalOrTrackerActions = totalTrackerCheckinsAllDays + clinicalRecordsCount
        val hasGeneratedMedicalPdf = if (healthState.lastGeneratedPdfSummary.isNotBlank()) 1 else 0

        // =====================================================================
        // 2. MÉTRICAS REAIS AUTOMÁTICAS DO FINANCEIRO (7 Medalhas)
        // =====================================================================
        val monthTransactions = transactions.filter { it.yearMonth == selectedYearMonth }
        val allTransactionsCount = maxOf(transactions.size, monthTransactions.size)
        val loggedIncomeBrl = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
        val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
        val totalExpensesBrl = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountBrl }
        val netBalanceBrl = effectiveIncomeBrl - totalExpensesBrl

        val activeSubs = subscriptions.filter { it.active }
        val paidSubsThisMonth = activeSubs.count { it.lastPostedYearMonth == selectedYearMonth }
        val fundedSavingsGoals = savingsGoals.count { it.currentAmountBrl > 0.0 }
        val totalSavedAmountBrl = savingsGoals.sumOf { it.currentAmountBrl.coerceAtLeast(0.0) }.roundToInt()
        val totalSavingsAccumulatedRatio = savingsGoals.sumOf {
            if (it.targetAmountBrl > 0) ((it.currentAmountBrl / it.targetAmountBrl) * 100.0).toInt().coerceAtMost(100) else 0
        }

        val needsSpentBrl = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
        val wantsSpentBrl = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
        val needsLimitBrl = effectiveIncomeBrl * (household.needsPercentage / 100.0)
        val wantsLimitBrl = effectiveIncomeBrl * (household.wantsPercentage / 100.0)
        val budget503020PillarsMet = listOf(
            netBalanceBrl > 0.0 && monthTransactions.isNotEmpty(),
            needsSpentBrl <= needsLimitBrl && needsLimitBrl > 0.0,
            wantsSpentBrl <= wantsLimitBrl && wantsLimitBrl > 0.0
        ).count { it }

        // =====================================================================
        // 3. MÉTRICAS REAIS AUTOMÁTICAS DO CALENDÁRIO & AGENDA (7 Medalhas)
        // =====================================================================
        val completedTasksTotal = tasks.count { it.completed }
        val completedTasksToday = tasks.count { it.completed && it.dateEpochDay == todayEpoch }
        val completedNonNegotiableTasks = tasks.count { it.completed && it.isEssential }
        val wokeUpRegisteredDays = customizationConfig.dailyStatesByDate.values.count { it.wokeUpAt.isNotBlank() }
        val effectiveStreakDays = maxOf(customizationConfig.currentStreakDays, wokeUpRegisteredDays)

        fun buildAutomaticGoal(
            id: String,
            title: String,
            description: String,
            module: AchievementModule,
            medalTier: MedalTier,
            iconEmoji: String,
            liveValue: Int,
            targetValue: Int,
            unitLabel: String,
            isCustom: Boolean = false
        ): AchievementGoal {
            val safeTarget = targetValue.coerceAtLeast(1)
            val effectiveCurrent = liveValue.coerceIn(0, safeTarget)
            val isUnlockedNow = effectiveCurrent >= safeTarget
            val existingTimestamp = persisted.unlockedTimestamps[id] ?: 0L
            return AchievementGoal(
                id = id,
                title = title,
                description = description,
                module = module,
                medalTier = medalTier,
                iconEmoji = iconEmoji,
                currentValue = effectiveCurrent,
                targetValue = safeTarget,
                unitLabel = unitLabel,
                isUnlocked = isUnlockedNow,
                unlockedAtMillis = if (isUnlockedNow) {
                    if (existingTimestamp > 0L) existingTimestamp else System.currentTimeMillis()
                } else {
                    0L
                },
                isCustomGoal = isCustom
            )
        }

        val coreGoals = mutableListOf(
            // =================================================================
            // 🩺 MÓDULO DE SAÚDE & BEM-ESTAR (7 Conquistas Automáticas)
            // =================================================================
            buildAutomaticGoal(
                id = "health_first_tracker_bronze",
                title = "Despertar do Autocuidado",
                description = "Conclua pelo menos 1 meta diária dos seus Inegociáveis de Saúde (água, vitaminas ou pausa).",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "💧",
                liveValue = completedTrackersToday.coerceAtLeast(if (totalTrackerCheckinsAllDays > 0) 1 else 0),
                targetValue = 1,
                unitLabel = "inegociável concluído"
            ),
            buildAutomaticGoal(
                id = "health_three_checkins_bronze",
                title = "Ritmo Saudável em Movimento",
                description = "Acumule 3 check-ins em hábitos inegociáveis de saúde na sua rotina.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🌱",
                liveValue = totalTrackerCheckinsAllDays,
                targetValue = 3,
                unitLabel = "check-ins de saúde"
            ),
            buildAutomaticGoal(
                id = "health_all_daily_silver",
                title = "Inegociáveis 100% em Dia",
                description = "Complete todas as metas diárias de saúde configuradas nos seus trackers no mesmo dia.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.SILVER,
                iconEmoji = "🌿",
                liveValue = completedTrackersToday,
                targetValue = totalTrackers,
                unitLabel = "metas do dia"
            ),
            buildAutomaticGoal(
                id = "health_vitals_monitor_silver",
                title = "Sentinela dos Sinais Vitais",
                description = "Alcance 6 registros combinados entre hábitos de saúde e medições clínicas.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.SILVER,
                iconEmoji = "🫀",
                liveValue = totalClinicalOrTrackerActions,
                targetValue = 6,
                unitLabel = "registros de saúde"
            ),
            buildAutomaticGoal(
                id = "health_consistency_gold",
                title = "Vitalidade & Consistência",
                description = "Alcance 12 registros acumulados de hábitos inegociáveis ou medições clínicas de saúde.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🩺",
                liveValue = totalClinicalOrTrackerActions,
                targetValue = 12,
                unitLabel = "registros acumulados"
            ),
            buildAutomaticGoal(
                id = "health_active_days_gold",
                title = "Semana de Saúde Blindada",
                description = "Mantenha seus hábitos de saúde ativos em pelo menos 5 dias registrados.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🛡️",
                liveValue = activeHealthDaysCount,
                targetValue = 5,
                unitLabel = "dias de saúde"
            ),
            buildAutomaticGoal(
                id = "health_master_diamond",
                title = "Guardião da Saúde Integral",
                description = "Complete todos os inegociáveis do dia e gere seu Relatório Médico Consolidado em PDF.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "💎",
                liveValue = completedTrackersToday + hasGeneratedMedicalPdf,
                targetValue = totalTrackers + 1,
                unitLabel = "etapas completas"
            ),

            // =================================================================
            // 💰 MÓDULO FINANCEIRO & PATRIMÔNIO (7 Conquistas Automáticas)
            // =================================================================
            buildAutomaticGoal(
                id = "finance_organizer_bronze",
                title = "Radar Financeiro Ativo",
                description = "Registre e categorize pelo menos 3 lançamentos financeiros no orçamento do mês.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🧾",
                liveValue = monthTransactions.size,
                targetValue = 3,
                unitLabel = "lançamentos no mês"
            ),
            buildAutomaticGoal(
                id = "finance_first_saving_bronze",
                title = "Primeiro Passo da Reserva",
                description = "Mantenha saldo positivo guardado em pelo menos 1 Caixinha / Meta de Economia.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🐷",
                liveValue = fundedSavingsGoals,
                targetValue = 1,
                unitLabel = "caixinha ativa"
            ),
            buildAutomaticGoal(
                id = "finance_bills_silver",
                title = "Contas Fixas sob Controle",
                description = "Mantenha pelo menos 2 contas fixas ou assinaturas lançadas e quitadas em dia no mês.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.SILVER,
                iconEmoji = "⚡",
                liveValue = paidSubsThisMonth.coerceAtLeast(
                    if (activeSubs.isNotEmpty() && paidSubsThisMonth == activeSubs.size) 2 else paidSubsThisMonth
                ),
                targetValue = 2,
                unitLabel = "contas em dia"
            ),
            buildAutomaticGoal(
                id = "finance_ten_tx_silver",
                title = "Mapeamento Completo de Gastos",
                description = "Registre 8 movimentações financeiras categorizadas para visão detalhada do orçamento.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.SILVER,
                iconEmoji = "📊",
                liveValue = allTransactionsCount,
                targetValue = 8,
                unitLabel = "lançamentos"
            ),
            buildAutomaticGoal(
                id = "finance_savings_gold",
                title = "Construtor de Patrimônio",
                description = "Mantenha pelo menos 2 Caixinhas / Metas de Economia ativas com saldo poupado.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🏦",
                liveValue = fundedSavingsGoals,
                targetValue = 2,
                unitLabel = "caixinhas com saldo"
            ),
            buildAutomaticGoal(
                id = "finance_thousand_saved_gold",
                title = "Guardião da Reserva R$ 1.000+",
                description = "Acumule R$ 1.000 ou mais somando o saldo guardado nas suas Caixinhas de Economia.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.GOLD,
                iconEmoji = "💰",
                liveValue = totalSavedAmountBrl,
                targetValue = 1000,
                unitLabel = "R$ poupados"
            ),
            buildAutomaticGoal(
                id = "finance_503020_diamond",
                title = "Mestre do Equilíbrio 50/30/20",
                description = "Mantenha os 3 pilares do orçamento familiar (Saldo Positivo, Essenciais e Estilo de Vida) dentro da meta com reserva ativa.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "👑",
                liveValue = budget503020PillarsMet + (if (totalSavingsAccumulatedRatio >= 50) 1 else 0),
                targetValue = 4,
                unitLabel = "pilares 50/30/20"
            ),

            // =================================================================
            // 📅 MÓDULO DE CALENDÁRIO & AGENDA (7 Conquistas Automáticas)
            // =================================================================
            buildAutomaticGoal(
                id = "calendar_first_task_bronze",
                title = "Agenda em Movimento",
                description = "Conclua pelo menos 1 compromisso ou tarefa agendada no seu Calendário.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "✅",
                liveValue = completedTasksTotal,
                targetValue = 1,
                unitLabel = "tarefa concluída"
            ),
            buildAutomaticGoal(
                id = "calendar_morning_check_bronze",
                title = "Despertar Planejado",
                description = "Registre seu horário de despertar ('Acordei') no topo da agenda para ajustar seu dia automaticamente.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🌅",
                liveValue = wokeUpRegisteredDays,
                targetValue = 1,
                unitLabel = "despertar registrado"
            ),
            buildAutomaticGoal(
                id = "calendar_essential_silver",
                title = "Foco no Essencial da Agenda",
                description = "Conclua 2 tarefas prioritárias (Inegociáveis da Agenda) sem deixar acumular.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.SILVER,
                iconEmoji = "⭐",
                liveValue = completedNonNegotiableTasks,
                targetValue = 2,
                unitLabel = "prioridades concluídas"
            ),
            buildAutomaticGoal(
                id = "calendar_five_tasks_silver",
                title = "Produtividade Consistente",
                description = "Conclua 4 tarefas planejadas na linha do tempo do seu Calendário.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.SILVER,
                iconEmoji = "📋",
                liveValue = completedTasksTotal,
                targetValue = 4,
                unitLabel = "tarefas concluídas"
            ),
            buildAutomaticGoal(
                id = "calendar_streak_gold",
                title = "Guardião do Ritmo & Sequência",
                description = "Alcance uma sequência consistente de 5 dias ativos no calendário e rotina diária.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🔥",
                liveValue = effectiveStreakDays,
                targetValue = 5,
                unitLabel = "dias seguidos"
            ),
            buildAutomaticGoal(
                id = "calendar_ten_tasks_gold",
                title = "Mestre da Execução Diária",
                description = "Conclua 8 tarefas ou compromissos agendados no Calendário.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.GOLD,
                iconEmoji = "⚡",
                liveValue = completedTasksTotal,
                targetValue = 8,
                unitLabel = "tarefas concluídas"
            ),
            buildAutomaticGoal(
                id = "calendar_master_diamond",
                title = "Arquiteto do Tempo Inabalável",
                description = "Conclua 12 tarefas planejadas na linha do tempo mantendo sua rotina organizada.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "🗓️",
                liveValue = completedTasksTotal,
                targetValue = 12,
                unitLabel = "tarefas concluídas"
            )
        )

        // Custom user-created goals — 100% automatically evaluated from the live metrics of their linked module!
        val customEvaluated = persisted.customGoals.map { custom ->
            val unitLower = custom.unitLabel.lowercase(Locale.forLanguageTag("pt-BR"))
            val autoMetricValue = when (custom.module) {
                AchievementModule.HEALTH -> {
                    when {
                        unitLower.contains("dia") -> activeHealthDaysCount
                        unitLower.contains("hoje") || unitLower.contains("meta") -> completedTrackersToday
                        else -> totalClinicalOrTrackerActions
                    }
                }
                AchievementModule.FINANCE -> {
                    when {
                        unitLower.contains("r$") || unitLower.contains("reais") || unitLower.contains("valor") -> totalSavedAmountBrl
                        unitLower.contains("conta") || unitLower.contains("boleto") || unitLower.contains("assinatura") -> paidSubsThisMonth
                        unitLower.contains("caixinha") || unitLower.contains("reserva") || unitLower.contains("aporte") || unitLower.contains("meta") -> fundedSavingsGoals
                        else -> allTransactionsCount
                    }
                }
                AchievementModule.CALENDAR -> {
                    when {
                        unitLower.contains("dia") || unitLower.contains("sequência") || unitLower.contains("ofensiva") -> effectiveStreakDays
                        unitLower.contains("essencial") || unitLower.contains("prioridade") || unitLower.contains("inegoci") -> completedNonNegotiableTasks
                        else -> completedTasksTotal
                    }
                }
                AchievementModule.TRINITY -> {
                    coreGoals.count { it.isUnlocked }
                }
            }

            buildAutomaticGoal(
                id = custom.id,
                title = custom.title.trim(),
                description = custom.description.trim(),
                module = custom.module,
                medalTier = custom.medalTier,
                iconEmoji = custom.iconEmoji,
                liveValue = autoMetricValue,
                targetValue = custom.targetValue,
                unitLabel = custom.unitLabel,
                isCustom = true
            )
        }

        val allModuleGoals = coreGoals + customEvaluated
        val healthUnlockedSoFar = allModuleGoals.count { it.module == AchievementModule.HEALTH && it.isUnlocked }
        val financeUnlockedSoFar = allModuleGoals.count { it.module == AchievementModule.FINANCE && it.isUnlocked }
        val calendarUnlockedSoFar = allModuleGoals.count { it.module == AchievementModule.CALENDAR && it.isUnlocked }
        val totalModuleMedalsSoFar = healthUnlockedSoFar + financeUnlockedSoFar + calendarUnlockedSoFar

        val activePillarsStarted = listOf(
            totalClinicalOrTrackerActions > 0,
            allTransactionsCount > 0,
            completedTasksTotal > 0
        ).count { it }

        val trinityPillarsWithMedal = listOf(
            healthUnlockedSoFar > 0,
            financeUnlockedSoFar > 0,
            calendarUnlockedSoFar > 0
        ).count { it }

        val perfectDayPillars = listOf(
            completedTrackersToday >= totalTrackers && totalTrackers > 0,
            completedTasksToday >= 1 || completedTasksTotal >= 1,
            netBalanceBrl > 0.0 && monthTransactions.isNotEmpty()
        ).count { it }

        // =====================================================================
        // 🏆 MÓDULO TRÍADE AURA 360° & SINERGIA (7 Conquistas Automáticas)
        // =====================================================================
        val trinityGoals = listOf(
            buildAutomaticGoal(
                id = "trinity_starter_bronze",
                title = "Primeiros Passos 360°",
                description = "Realize pelo menos 1 ação em Saúde, 1 lançamento no Financeiro e 1 tarefa concluída no Calendário.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🧭",
                liveValue = activePillarsStarted,
                targetValue = 3,
                unitLabel = "módulos ativos"
            ),
            buildAutomaticGoal(
                id = "trinity_aura_360_diamond",
                title = "Sinergia Tríade Omni 360°",
                description = "Conquiste pelo menos 1 medalha em Saúde, 1 no Financeiro e 1 no Calendário.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.SILVER,
                iconEmoji = "🔱",
                liveValue = trinityPillarsWithMedal,
                targetValue = 3,
                unitLabel = "módulos com medalha"
            ),
            buildAutomaticGoal(
                id = "trinity_five_medals_silver",
                title = "Colecionador de Conquistas",
                description = "Desbloqueie 5 medalhas automáticas somando Saúde, Financeiro e Calendário.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.SILVER,
                iconEmoji = "🎖️",
                liveValue = totalModuleMedalsSoFar,
                targetValue = 5,
                unitLabel = "medalhas ganhas"
            ),
            buildAutomaticGoal(
                id = "trinity_perfect_day_gold",
                title = "Dia Perfeito Omni 360°",
                description = "No mesmo dia, conclua 100% dos inegociáveis de Saúde, tenha tarefa concluída na Agenda e saldo financeiro positivo.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🌟",
                liveValue = perfectDayPillars,
                targetValue = 3,
                unitLabel = "metas do dia perfeito"
            ),
            buildAutomaticGoal(
                id = "trinity_ten_medals_gold",
                title = "Mestre da Tríade de Ouro",
                description = "Conquiste 10 medalhas automáticas pela sua consistência em Saúde, Finanças e Agenda.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🏅",
                liveValue = totalModuleMedalsSoFar,
                targetValue = 10,
                unitLabel = "medalhas ganhas"
            ),
            buildAutomaticGoal(
                id = "trinity_fifteen_medals_diamond",
                title = "Excelência Diamante 360°",
                description = "Desbloqueie 15 medalhas automáticas comprovando domínio completo da sua rotina e orçamento.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "💎",
                liveValue = totalModuleMedalsSoFar,
                targetValue = 15,
                unitLabel = "medalhas ganhas"
            ),
            buildAutomaticGoal(
                id = "trinity_legend_diamond",
                title = "Lenda Suprema Omni",
                description = "Alcance o patamar máximo conquistando 20 medalhas automáticas no ecossistema Omni.",
                module = AchievementModule.TRINITY,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "🏆",
                liveValue = totalModuleMedalsSoFar,
                targetValue = 20,
                unitLabel = "medalhas ganhas"
            )
        )

        val allGoals = customEvaluated + coreGoals + trinityGoals

        // Record unlock timestamps for newly earned automatic achievements
        val newlyUnlockedGoals = allGoals.filter { it.isUnlocked && !persisted.unlockedTimestamps.containsKey(it.id) }
        if (newlyUnlockedGoals.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val updatedUnlocked = persisted.unlockedTimestamps.toMutableMap()
            newlyUnlockedGoals.forEach { g -> updatedUnlocked[g.id] = now }
            savePersistedState(
                persisted.copy(
                    unlockedTimestamps = updatedUnlocked,
                    lastCelebratedGoalId = newlyUnlockedGoals.last().id
                )
            )
        }

        val unlockedGoals = allGoals.filter { it.isUnlocked }
        val totalXp = unlockedGoals.sumOf { it.medalTier.xpReward }

        val levelStepXp = 250
        val currentLevel = (totalXp / levelStepXp) + 1
        val xpIntoLevel = totalXp % levelStepXp
        val levelRatio = (xpIntoLevel.toFloat() / levelStepXp.toFloat()).coerceIn(0f, 1f)
        val levelTitle = when (currentLevel) {
            1 -> "Explorador da Rotina"
            2 -> "Organizador Consciente"
            3 -> "Guardião do Equilíbrio"
            4 -> "Estrategista 360°"
            5 -> "Mestre das Finanças & Saúde"
            6 -> "Embaixador da Disciplina"
            7 -> "Arquiteto da Prosperidade"
            else -> "Lenda Omni Nível $currentLevel"
        }

        val healthGoals = allGoals.filter { it.module == AchievementModule.HEALTH }
        val financeGoals = allGoals.filter { it.module == AchievementModule.FINANCE }
        val calendarGoals = allGoals.filter { it.module == AchievementModule.CALENDAR }

        return AchievementsEvaluationSummary(
            goals = allGoals,
            unlockedCount = unlockedGoals.size,
            totalCount = allGoals.size,
            totalXp = totalXp,
            currentLevel = currentLevel,
            levelTitle = levelTitle,
            currentLevelProgressRatio = levelRatio,
            xpIntoCurrentLevel = xpIntoLevel,
            xpRequiredForNextLevel = levelStepXp,
            bronzeUnlocked = unlockedGoals.count { it.medalTier == MedalTier.BRONZE },
            silverUnlocked = unlockedGoals.count { it.medalTier == MedalTier.SILVER },
            goldUnlocked = unlockedGoals.count { it.medalTier == MedalTier.GOLD },
            diamondUnlocked = unlockedGoals.count { it.medalTier == MedalTier.DIAMOND },
            healthUnlocked = healthGoals.count { it.isUnlocked },
            healthTotal = healthGoals.size,
            financeUnlocked = financeGoals.count { it.isUnlocked },
            financeTotal = financeGoals.size,
            calendarUnlocked = calendarGoals.count { it.isUnlocked },
            calendarTotal = calendarGoals.size,
            newlyUnlockedGoalTitle = newlyUnlockedGoals.lastOrNull()?.let { "${it.medalTier.emoji} ${it.title} (+${it.medalTier.xpReward} XP)" }
        )
    }

    companion object {
        private const val PREFS_NAME = "aura_achievements_medals_prefs"
        private const val KEY_STATE_JSON = "achievements_state_json"
    }
}
