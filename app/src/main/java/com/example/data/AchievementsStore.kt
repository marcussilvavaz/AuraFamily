package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
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
    TRINITY("TRINITY", "Tríade Aura 360°", "Tríade 360°", "🏆");

    companion object {
        fun fromId(id: String): AchievementModule {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HEALTH
        }
    }
}

/**
 * Nível da Medalha ganha ao atingir o objetivo.
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
 * Representa uma Conquista / Objetivo avaliado com seu progresso atual e Medalha correspondente.
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
 * Resumo consolidado do Sistema de Conquistas e Medalhas.
 */
data class AchievementsEvaluationSummary(
    val goals: List<AchievementGoal> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val levelTitle: String = "Iniciante Aura",
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
    val calendarTotal: Int = 0
)

/**
 * Estado persistido do repositório de conquistas (medalhas desbloqueadas + objetivos personalizados + progresso manual).
 */
data class PersistedAchievementsState(
    val unlockedTimestamps: Map<String, Long> = emptyMap(),
    val manualBoosts: Map<String, Int> = emptyMap(),
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

            val boostsMap = mutableMapOf<String, Int>()
            val boostsObj = root.optJSONObject("manualBoosts")
            if (boostsObj != null) {
                val keys = boostsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    boostsMap[k] = boostsObj.optInt(k, 0)
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
                        currentValue = item.optInt("currentValue", 0),
                        targetValue = item.optInt("targetValue", 1).coerceAtLeast(1),
                        unitLabel = item.optString("unitLabel", "etapas"),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            PersistedAchievementsState(
                unlockedTimestamps = unlockedMap,
                manualBoosts = boostsMap,
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

        val boostsObj = JSONObject()
        state.manualBoosts.forEach { (id, v) -> boostsObj.put(id, v) }
        root.put("manualBoosts", boostsObj)

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
     * Incrementa o progresso de uma conquista (personalizada ou padrão) e desbloqueia a medalha se atingir a meta.
     */
    fun incrementGoalProgress(goalId: String, step: Int = 1, targetValue: Int = 1): Boolean {
        val current = _persistedStateFlow.value
        val now = System.currentTimeMillis()
        var newlyUnlocked = false

        val isCustom = current.customGoals.any { it.id == goalId }
        val updatedCustomGoals = if (isCustom) {
            current.customGoals.map { g ->
                if (g.id == goalId) {
                    val nextVal = (g.currentValue + step).coerceAtLeast(0)
                    if (nextVal >= g.targetValue && !current.unlockedTimestamps.containsKey(g.id)) {
                        newlyUnlocked = true
                    }
                    g.copy(currentValue = nextVal)
                } else {
                    g
                }
            }
        } else {
            current.customGoals
        }

        val updatedBoosts = current.manualBoosts.toMutableMap()
        if (!isCustom) {
            val nextBoost = (updatedBoosts[goalId] ?: 0) + step
            updatedBoosts[goalId] = nextBoost.coerceAtLeast(0)
            if (nextBoost >= targetValue && !current.unlockedTimestamps.containsKey(goalId)) {
                newlyUnlocked = true
            }
        }

        val updatedUnlocked = current.unlockedTimestamps.toMutableMap()
        if (newlyUnlocked) {
            updatedUnlocked[goalId] = now
        }

        savePersistedState(
            current.copy(
                unlockedTimestamps = updatedUnlocked,
                manualBoosts = updatedBoosts,
                customGoals = updatedCustomGoals,
                lastCelebratedGoalId = if (newlyUnlocked) goalId else current.lastCelebratedGoalId
            )
        )
        return newlyUnlocked
    }

    /**
     * Cria um novo objetivo personalizado vinculado a Saúde, Financeiro ou Calendário com Medalha à escolha.
     */
    fun addCustomAchievementGoal(
        title: String,
        description: String,
        module: AchievementModule,
        medalTier: MedalTier,
        targetValue: Int,
        unitLabel: String
    ): CustomAchievementGoal {
        val cleanTitle = title.trim().ifBlank { "Novo Objetivo ${module.shortLabel}" }
        val cleanDesc = description.trim().ifBlank {
            "Meta personalizada vinculada ao módulo de ${module.shortLabel}"
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
            targetValue = targetValue.coerceIn(1, 10000),
            unitLabel = unitLabel.trim().ifBlank { "ações" }
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
     * Avalia todas as conquistas em tempo real a partir dos dados de Saúde, Financeiro e Calendário,
     * sincronizando automaticamente novas medalhas desbloqueadas.
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
        val todayIso = DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())

        // =====================================================================
        // 1. MÉTRICAS REAIS DE SAÚDE
        // =====================================================================
        val totalTrackers = healthState.trackers.size.coerceAtLeast(1)
        val completedTrackersToday = healthState.completedTrackersCount(todayIso)
        val totalTrackerCheckinsAllDays = healthState.trackerProgressByDate.values.sumOf { dayMap ->
            dayMap.values.count { it > 0 }
        }
        val totalClinicalOrTrackerActions = completedTrackersToday +
            (if (customizationConfig.diabetesEnabled) healthState.glucoseRecords.size else 0) +
            (if (customizationConfig.bloodPressureEnabled) healthState.bloodPressureRecords.size else 0) +
            (if (customizationConfig.pregnancyEnabled) healthState.pregnancyChecklist.count { it.completed } else 0)
        val hasGeneratedMedicalPdf = if (healthState.lastGeneratedPdfSummary.isNotBlank()) 1 else 0

        // =====================================================================
        // 2. MÉTRICAS REAIS DO FINANCEIRO
        // =====================================================================
        val monthTransactions = transactions.filter { it.yearMonth == selectedYearMonth }
        val loggedIncomeBrl = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
        val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
        val totalExpensesBrl = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountBrl }
        val netBalanceBrl = effectiveIncomeBrl - totalExpensesBrl

        val activeSubs = subscriptions.filter { it.active }
        val paidSubsThisMonth = activeSubs.count { it.lastPostedYearMonth == selectedYearMonth }
        val fundedSavingsGoals = savingsGoals.count { it.currentAmountBrl > 0.0 }
        val totalSavingsAccumulatedRatio = savingsGoals.sumOf {
            if (it.targetAmountBrl > 0) ((it.currentAmountBrl / it.targetAmountBrl) * 100.0).toInt().coerceAtMost(100) else 0
        }

        val needsSpentBrl = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
        val wantsSpentBrl = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
        val needsLimitBrl = effectiveIncomeBrl * (household.needsPercentage / 100.0)
        val wantsLimitBrl = effectiveIncomeBrl * (household.wantsPercentage / 100.0)
        val budget503020PillarsMet = listOf(
            netBalanceBrl > 0.0,
            needsSpentBrl <= needsLimitBrl || needsLimitBrl <= 0.0,
            wantsSpentBrl <= wantsLimitBrl || wantsLimitBrl <= 0.0
        ).count { it }

        // =====================================================================
        // 3. MÉTRICAS REAIS DO CALENDÁRIO & AGENDA
        // =====================================================================
        val completedTasksTotal = tasks.count { it.completed }
        val completedNonNegotiableTasks = tasks.count { it.completed && it.isEssential }
        val wokeUpRegisteredDays = customizationConfig.dailyStatesByDate.values.count { it.wokeUpAt.isNotBlank() }
        val effectiveStreakDays = maxOf(customizationConfig.currentStreakDays, wokeUpRegisteredDays)

        fun buildGoal(
            id: String,
            title: String,
            description: String,
            module: AchievementModule,
            medalTier: MedalTier,
            iconEmoji: String,
            liveValue: Int,
            targetValue: Int,
            unitLabel: String
        ): AchievementGoal {
            val manualBoost = persisted.manualBoosts[id] ?: 0
            val wasAlreadyUnlocked = persisted.unlockedTimestamps.containsKey(id)
            val effectiveCurrent = if (wasAlreadyUnlocked) {
                maxOf(liveValue + manualBoost, targetValue)
            } else {
                (liveValue + manualBoost).coerceAtMost(targetValue)
            }
            val isUnlockedNow = wasAlreadyUnlocked || effectiveCurrent >= targetValue
            return AchievementGoal(
                id = id,
                title = title,
                description = description,
                module = module,
                medalTier = medalTier,
                iconEmoji = iconEmoji,
                currentValue = effectiveCurrent,
                targetValue = targetValue,
                unitLabel = unitLabel,
                isUnlocked = isUnlockedNow,
                unlockedAtMillis = persisted.unlockedTimestamps[id] ?: if (isUnlockedNow) System.currentTimeMillis() else 0L,
                isCustomGoal = false
            )
        }

        val coreGoals = mutableListOf(
            // --- MÓDULO DE SAÚDE (4 Medalhas: Bronze, Prata, Ouro, Diamante) ---
            buildGoal(
                id = "health_first_tracker_bronze",
                title = "Despertar do Autocuidado",
                description = "Conclua pelo menos 1 meta diária dos seus Inegociáveis de Saúde (água, vitaminas ou pausa).",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "💧",
                liveValue = completedTrackersToday.coerceAtLeast(if (totalTrackerCheckinsAllDays > 0) 1 else 0),
                targetValue = 1,
                unitLabel = "inegociável"
            ),
            buildGoal(
                id = "health_all_daily_silver",
                title = "Inegociáveis 100% em Dia",
                description = "Complete todas as metas diárias de saúde configuradas nos seus trackers do dia.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.SILVER,
                iconEmoji = "🌿",
                liveValue = completedTrackersToday,
                targetValue = totalTrackers,
                unitLabel = "metas do dia"
            ),
            buildGoal(
                id = "health_consistency_gold",
                title = "Vitalidade & Monitoramento Ativo",
                description = "Alcance 5 registros acumulados de hábitos inegociáveis ou medições de saúde.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🩺",
                liveValue = maxOf(totalTrackerCheckinsAllDays, totalClinicalOrTrackerActions),
                targetValue = 5,
                unitLabel = "registros"
            ),
            buildGoal(
                id = "health_master_diamond",
                title = "Guardião da Saúde Integral",
                description = "Complete seus inegociáveis diários e gere seu Relatório Médico Consolidado em PDF.",
                module = AchievementModule.HEALTH,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "💎",
                liveValue = completedTrackersToday + hasGeneratedMedicalPdf,
                targetValue = totalTrackers + 1,
                unitLabel = "etapas"
            ),

            // --- MÓDULO FINANCEIRO (4 Medalhas: Bronze, Prata, Ouro, Diamante) ---
            buildGoal(
                id = "finance_organizer_bronze",
                title = "Radar Financeiro Ativo",
                description = "Registre e categorize pelo menos 3 lançamentos financeiros no orçamento do mês.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.BRONZE,
                iconEmoji = "🧾",
                liveValue = monthTransactions.size,
                targetValue = 3,
                unitLabel = "lançamentos"
            ),
            buildGoal(
                id = "finance_bills_silver",
                title = "Contas Fixas sob Controle",
                description = "Mantenha pelo menos 2 contas fixas ou assinaturas lançadas e em dia no mês.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.SILVER,
                iconEmoji = "⚡",
                liveValue = paidSubsThisMonth.coerceAtLeast(if (activeSubs.isNotEmpty() && paidSubsThisMonth == activeSubs.size) 2 else paidSubsThisMonth),
                targetValue = 2,
                unitLabel = "contas em dia"
            ),
            buildGoal(
                id = "finance_savings_gold",
                title = "Construtor de Patrimônio",
                description = "Mantenha pelo menos 2 Caixinhas / Metas de Economia ativas com saldo poupado.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🏦",
                liveValue = fundedSavingsGoals,
                targetValue = 2,
                unitLabel = "metas ativas"
            ),
            buildGoal(
                id = "finance_503020_diamond",
                title = "Mestre do Equilíbrio 50/30/20",
                description = "Mantenha os 3 pilares do orçamento familiar (Saldo Positivo, Essenciais e Estilo de Vida) dentro da meta.",
                module = AchievementModule.FINANCE,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "👑",
                liveValue = budget503020PillarsMet + (if (totalSavingsAccumulatedRatio >= 50) 1 else 0),
                targetValue = 4,
                unitLabel = "pilares 50/30/20"
            ),

            // --- MÓDULO DE CALENDÁRIO & AGENDA (4 Medalhas: Bronze, Prata, Ouro, Diamante) ---
            buildGoal(
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
            buildGoal(
                id = "calendar_essential_silver",
                title = "Foco no Essencial da Agenda",
                description = "Conclua 2 tarefas prioritárias (Inegociáveis da Agenda) sem deixar acumular.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.SILVER,
                iconEmoji = "⭐",
                liveValue = completedNonNegotiableTasks.coerceAtLeast(completedTasksTotal / 2),
                targetValue = 2,
                unitLabel = "prioridades"
            ),
            buildGoal(
                id = "calendar_streak_gold",
                title = " Guardião do Ritmo & Sequência",
                description = "Alcance uma sequência consistente de 5 dias ativos no calendário e rotina diária.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.GOLD,
                iconEmoji = "🔥",
                liveValue = effectiveStreakDays,
                targetValue = 5,
                unitLabel = "dias seguidos"
            ),
            buildGoal(
                id = "calendar_master_diamond",
                title = "Arquiteto do Tempo",
                description = "Conclua 5 tarefas planejadas na linha do tempo mantendo sua rotina organizada.",
                module = AchievementModule.CALENDAR,
                medalTier = MedalTier.DIAMOND,
                iconEmoji = "🗓️",
                liveValue = completedTasksTotal,
                targetValue = 5,
                unitLabel = "tarefas concluídas"
            )
        )

        // Custom user-created goals
        val customEvaluated = persisted.customGoals.map { custom ->
            val unlocked = persisted.unlockedTimestamps.containsKey(custom.id) || custom.currentValue >= custom.targetValue
            AchievementGoal(
                id = custom.id,
                title = custom.title.trim(),
                description = custom.description.trim(),
                module = custom.module,
                medalTier = custom.medalTier,
                iconEmoji = custom.iconEmoji,
                currentValue = custom.currentValue.coerceAtMost(custom.targetValue),
                targetValue = custom.targetValue,
                unitLabel = custom.unitLabel,
                isUnlocked = unlocked,
                unlockedAtMillis = persisted.unlockedTimestamps[custom.id] ?: if (unlocked) System.currentTimeMillis() else 0L,
                isCustomGoal = true
            )
        }

        val allBeforeTrinity = coreGoals + customEvaluated
        val hasHealthMedal = allBeforeTrinity.any { it.module == AchievementModule.HEALTH && it.isUnlocked }
        val hasFinanceMedal = allBeforeTrinity.any { it.module == AchievementModule.FINANCE && it.isUnlocked }
        val hasCalendarMedal = allBeforeTrinity.any { it.module == AchievementModule.CALENDAR && it.isUnlocked }
        val trinityPillarsUnlocked = listOf(hasHealthMedal, hasFinanceMedal, hasCalendarMedal).count { it }

        val trinityGoal = buildGoal(
            id = "trinity_aura_360_diamond",
            title = "Sinergia Tríade Aura 360°",
            description = "Conquiste pelo menos 1 medalha em Saúde, 1 no Financeiro e 1 no Calendário para dominar a rotina completa.",
            module = AchievementModule.TRINITY,
            medalTier = MedalTier.DIAMOND,
            iconEmoji = "🏆",
            liveValue = trinityPillarsUnlocked,
            targetValue = 3,
            unitLabel = "módulos com medalha"
        )

        val allGoals = customEvaluated + coreGoals + trinityGoal

        // Persist newly unlocked timestamps automatically so once earned, a medal is never lost
        val newlyUnlockedIds = allGoals.filter { it.isUnlocked && !persisted.unlockedTimestamps.containsKey(it.id) }
        if (newlyUnlockedIds.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val updatedUnlocked = persisted.unlockedTimestamps.toMutableMap()
            newlyUnlockedIds.forEach { g -> updatedUnlocked[g.id] = now }
            savePersistedState(persisted.copy(unlockedTimestamps = updatedUnlocked))
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
            else -> "Lenda Aura Nível $currentLevel"
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
            calendarTotal = calendarGoals.size
        )
    }

    companion object {
        private const val PREFS_NAME = "aura_achievements_medals_prefs"
        private const val KEY_STATE_JSON = "achievements_state_json"
    }
}
