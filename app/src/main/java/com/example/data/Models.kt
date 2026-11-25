package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Escopos de privacidade para tarefas da agenda e transações financeiras.
 */
object PrivacyScope {
    const val FAMILY = "FAMILY"               // Compartilhado com todos os membros da família
    const val PERSONAL = "PERSONAL"           // Privado: visível apenas para o criador
    const val COUPLE = "COUPLE"               // Visível apenas para administradores/casal
    const val ASSIGNED_ONLY = "ASSIGNED_ONLY" // Visível apenas para o criador e responsável designado

    fun labelPt(scope: String): String = when (scope) {
        PERSONAL -> "Pessoal (Privado)"
        COUPLE -> "Casal / Administradores"
        ASSIGNED_ONLY -> "Apenas Responsáveis"
        else -> "Toda a Família"
    }
}

/**
 * Modos de divisão de valores entre membros da família nas despesas e contas fixas.
 */
object SplitMode {
    const val NONE = "NONE"                   // Sem divisão (100% no responsável)
    const val EQUAL = "EQUAL"                 // Divisão igualitária (50/50, etc.)
    const val PERCENTAGE = "PERCENTAGE"       // Divisão proporcional por porcentagem (%)
    const val CUSTOM_AMOUNT = "CUSTOM_AMOUNT" // Divisão por valor fixo (R$) para cada membro

    fun labelPt(mode: String): String = when (mode) {
        EQUAL -> "Divisão Igualitária"
        PERCENTAGE -> "Por Porcentagem (%)"
        CUSTOM_AMOUNT -> "Valor Fixo por Membro"
        else -> "Sem Divisão (Individual/Família)"
    }
}

/**
 * Cota individual de um membro na divisão de uma despesa ou conta fixa.
 */
@Keep
@IgnoreExtraProperties
data class ExpenseSplitShare(
    val memberId: String = "",
    val memberName: String = "",
    val memberColorHex: Long = 0xFF0E7C5B,
    val memberEmoji: String = "🌿",
    val percentage: Double = 100.0,
    val shareAmountBrl: Double = 0.0,
    val settled: Boolean = false
)

/**
 * Identificação visual resumida de um membro da família para exibição em avatares, tarefas e gastos.
 */
@Keep
@IgnoreExtraProperties
data class FamilyMemberBadge(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarBase64: String = "",
    val colorHex: Long = 0xFF0E7C5B,
    val avatarEmoji: String = "🌿",
    val role: String = "ADMIN",
    val isCurrentUser: Boolean = false
)

/**
 * Ritmo da Rotina do Usuário (Passo 1 - Perfil Modular)
 */
object RoutineRhythm {
    const val FIXA = "FIXA"         // Horário padrão de acordar e dormir
    const val ELASTICA = "ELASTICA" // O dia só começa quando o usuário clica em "Acordei"

    fun labelPt(rhythm: String): String = when (rhythm) {
        ELASTICA -> "Rotina Elástica (Inicia ao clicar em 'Acordei')"
        else -> "Rotina Fixa (Horário padrão de acordar/dormir)"
    }
}

/**
 * Níveis do Mapa de Energia por Turno (Manhã, Tarde, Noite)
 */
object EnergyLevel {
    const val BAIXA = "BAIXA"
    const val MEDIA = "MEDIA"
    const val ALTA = "ALTA"

    fun labelPt(level: String): String = when (level) {
        BAIXA -> "Baixa"
        ALTA -> "Alta"
        else -> "Média"
    }
}

/**
 * Slots padrão de medição de Monitoramento de Glicemia (10 períodos diários completos)
 */
object DiabetesSlotCatalog {
    const val JEJUM = "Jejum"
    const val PRE_REFEICAO = "Pré-Refeição"
    const val POS_CAFE_1H = "1h Pós-Café"
    const val POS_CAFE_2H = "2h Pós-Café"
    const val POS_ALMOCO_1H = "1h Pós-Almoço"
    const val POS_ALMOCO_2H = "2h Pós-Almoço"
    const val POS_LANCHE_1H = "1h Pós-Lanche"
    const val POS_JANTAR_1H = "1h Pós-Jantar"
    const val POS_JANTAR_2H = "2h Pós-Jantar"
    const val AO_DEITAR = "Ao Deitar"

    // Aliases legados para compatibilidade
    const val PRE_ALMOCO = PRE_REFEICAO
    const val PRE_JANTAR_1H = PRE_REFEICAO
    const val ANTES_DORMIR = AO_DEITAR

    val allSlots: List<String> = listOf(
        JEJUM,
        PRE_REFEICAO,
        POS_CAFE_1H,
        POS_CAFE_2H,
        POS_ALMOCO_1H,
        POS_ALMOCO_2H,
        POS_LANCHE_1H,
        POS_JANTAR_1H,
        POS_JANTAR_2H,
        AO_DEITAR
    )

    val defaultSelectedSlots: Set<String> = allSlots.toSet()

    fun normalizeSlot(rawSlot: String): String {
        val clean = rawSlot.trim().lowercase(Locale.ROOT)
        return when {
            clean == "jejum" -> JEJUM
            clean.contains("pré") || clean.contains("pre") -> PRE_REFEICAO
            clean.contains("2h") && (clean.contains("café") || clean.contains("cafe")) -> POS_CAFE_2H
            clean.contains("café") || clean.contains("cafe") -> POS_CAFE_1H
            clean.contains("2h") && (clean.contains("almoço") || clean.contains("almoco")) -> POS_ALMOCO_2H
            clean.contains("almoço") || clean.contains("almoco") -> POS_ALMOCO_1H
            clean.contains("lanche") -> POS_LANCHE_1H
            clean.contains("2h") && clean.contains("jantar") -> POS_JANTAR_2H
            clean.contains("jantar") -> POS_JANTAR_1H
            clean.contains("dormir") || clean.contains("deitar") || clean.contains("madrugada") -> AO_DEITAR
            else -> rawSlot.ifBlank { JEJUM }
        }
    }

    fun slotForPostMealTimer(mealLabel: String, durationSeconds: Int): String {
        val isTwoHours = durationSeconds >= 5400 // >= 90 min maps to 2h slot if 2h exists
        val cleanMeal = mealLabel.lowercase(Locale.ROOT)
        return when {
            cleanMeal.contains("café") || cleanMeal.contains("cafe") ->
                if (isTwoHours) POS_CAFE_2H else POS_CAFE_1H
            cleanMeal.contains("almoço") || cleanMeal.contains("almoco") ->
                if (isTwoHours) POS_ALMOCO_2H else POS_ALMOCO_1H
            cleanMeal.contains("lanche") ->
                POS_LANCHE_1H
            cleanMeal.contains("jantar") ->
                if (isTwoHours) POS_JANTAR_2H else POS_JANTAR_1H
            else -> POS_ALMOCO_1H
        }
    }
}

/**
 * Estado Diário (daily_state por data YYYY-MM-DD)
 */
@Keep
@IgnoreExtraProperties
data class DailyState(
    val dateIso: String = "", // YYYY-MM-DD
    val wokeUpAt: String = "", // HH:mm quando clicou em "Acordei" (vazio se ainda não iniciou na rotina Elástica)
    val wokeUpTimestampMillis: Long = 0L,
    val hardDayModeActive: Boolean = false, // "Modo Dia Difícil" ativo na data
    val streakFrozen: Boolean = false,      // Congela o contador de sequência sem penalidade
    val updatedAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarBase64: String = "",
    val householdId: String = "",
    val themeMode: String = "SYSTEM",
    val baseCurrency: String = "BRL",
    // Campos de identificação visual do membro
    val memberColorHex: Long = 0xFF0E7C5B,
    val avatarEmoji: String = "🌿",
    val avatarInitials: String = "FA",
    val memberRole: String = "ADMIN", // ADMIN, ADULT, TEEN, CHILD
    val defaultSplitPercentage: Double = 50.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val uid: String
        get() = userId.ifBlank { id }
}

/**
 * Conta Bancária / Carteira cadastrada pelo usuário (Individual ou Familiar).
 */
@Keep
@IgnoreExtraProperties
data class BankAccount(
    val id: String = "acc_nubank",
    val name: String = "Nubank",
    val institution: String = name,
    val institutionColorHex: Long = 0xFF820AD1,
    val colorHex: Long = institutionColorHex,
    val iconEmoji: String = "🟣",
    val emoji: String = iconEmoji,
    val initialBalanceBrl: Double = 0.0,
    val currentBalanceBrl: Double = initialBalanceBrl,
    val creditLimitBrl: Double = 0.0,
    val closingDay: Int = 0,
    val dueDay: Int = 0,
    val usedCreditLimitBrl: Double = 0.0,
    val availableCreditLimitBrl: Double = creditLimitBrl,
    val privacyScope: String = PrivacyScope.FAMILY
) {
    fun isCashOrWallet(): Boolean {
        val lowerName = name.trim().lowercase(Locale.ROOT)
        val lowerInst = institution.trim().lowercase(Locale.ROOT)
        return id == "acc_cash" ||
            lowerName.contains("dinheiro") ||
            lowerName.contains("carteira") ||
            lowerInst.contains("dinheiro") ||
            lowerInst.contains("carteira")
    }

    fun sanitized(): BankAccount {
        return if (isCashOrWallet()) {
            copy(
                creditLimitBrl = 0.0,
                closingDay = 0,
                dueDay = 0,
                usedCreditLimitBrl = 0.0,
                availableCreditLimitBrl = 0.0
            )
        } else {
            this
        }
    }
}

object BankAccountCatalog {
    fun defaultAccounts(): List<BankAccount> = listOf(
        BankAccount(
            id = "acc_nubank",
            name = "Nubank",
            institutionColorHex = 0xFF820AD1,
            iconEmoji = "🟣",
            initialBalanceBrl = 0.0,
            creditLimitBrl = 0.0,
            closingDay = 25,
            dueDay = 5,
            privacyScope = PrivacyScope.FAMILY
        ),
        BankAccount(
            id = "acc_itau",
            name = "Itaú",
            institutionColorHex = 0xFFEC7000,
            iconEmoji = "🟠",
            initialBalanceBrl = 0.0,
            creditLimitBrl = 0.0,
            closingDay = 20,
            dueDay = 1,
            privacyScope = PrivacyScope.FAMILY
        ),
        BankAccount(
            id = "acc_picpay",
            name = "PicPay",
            institutionColorHex = 0xFF11C76F,
            iconEmoji = "🟢",
            initialBalanceBrl = 0.0,
            creditLimitBrl = 0.0,
            closingDay = 28,
            dueDay = 10,
            privacyScope = PrivacyScope.PERSONAL
        ),
        BankAccount(
            id = "acc_cash",
            name = "Dinheiro / Carteira",
            institutionColorHex = 0xFF0B6E4F,
            iconEmoji = "💵",
            initialBalanceBrl = 0.0,
            creditLimitBrl = 0.0,
            closingDay = 0,
            dueDay = 0,
            privacyScope = PrivacyScope.PERSONAL
        )
    )

    val presetInstitutions: List<Triple<String, String, Long>> = listOf(
        Triple("Nubank", "🟣", 0xFF820AD1),
        Triple("Itaú", "🟠", 0xFFEC7000),
        Triple("Bradesco", "🔴", 0xFFCC092F),
        Triple("Santander", "🔴", 0xFFEC0000),
        Triple("Banco do Brasil", "🟡", 0xFFF8D117),
        Triple("Caixa", "🔵", 0xFF005CA9),
        Triple("Inter", "🟠", 0xFFFF7A00),
        Triple("PicPay", "🟢", 0xFF11C76F),
        Triple("Mercado Pago", "🔵", 0xFF009EE3),
        Triple("C6 Bank", "⚫", 0xFF242424),
        Triple("Dinheiro / Carteira", "💵", 0xFF0B6E4F)
    )

    /**
     * Calcula o saldo atual e o limite de crédito utilizado/disponível de cada conta bancária:
     * - Saldo = initialBalanceBrl
     *         + Soma(INCOME vinculados à conta)
     *         - Soma(EXPENSE vinculados à conta onde !isCreditCard OU isPaid == true)
     *         - Soma(TRANSFER onde accountId == conta)
     *         + Soma(TRANSFER onde destinationAccountId == conta)
     * - Limite Usado = Soma de todas as compras no crédito em aberto (!isPaid) vinculadas à conta (todas as parcelas futuras)
     * - Limite Disponível = (creditLimitBrl - usedCreditLimitBrl).coerceAtLeast(0.0)
     */
    fun calculateCurrentBalances(
        accounts: List<BankAccount>,
        transactions: List<FinanceTransaction>
    ): List<BankAccount> {
        val effectiveAccounts = accounts.ifEmpty { defaultAccounts() }.map { it.sanitized() }
        return effectiveAccounts.map { acc ->
            var balance = acc.initialBalanceBrl
            var usedCredit = 0.0
            for (tx in transactions) {
                val txAccId = tx.accountId.ifBlank { effectiveAccounts.first().id }
                when (tx.type) {
                    "INCOME" -> {
                        if (txAccId == acc.id && tx.isPaid) {
                            balance += tx.amountBrl
                        }
                    }
                    "EXPENSE" -> {
                        val deductFromAccount = !tx.isCreditCard || tx.isPaid
                        if (txAccId == acc.id && deductFromAccount) {
                            balance -= tx.amountBrl
                        }
                        if (!acc.isCashOrWallet() && txAccId == acc.id && tx.isCreditCard && !tx.isPaid) {
                            usedCredit += tx.amountBrl
                        }
                    }
                    "TRANSFER" -> {
                        if (txAccId == acc.id) {
                            balance -= tx.amountBrl
                        }
                        if (tx.destinationAccountId == acc.id) {
                            balance += tx.amountBrl
                        }
                    }
                }
            }
            val resolvedColor = if (acc.colorHex != 0L) acc.colorHex else acc.institutionColorHex
            val resolvedEmoji = acc.emoji.ifBlank { acc.iconEmoji.ifBlank { "🏦" } }
            val resolvedInst = acc.institution.ifBlank { acc.name }
            val isCash = acc.isCashOrWallet()
            val safeLimit = if (isCash) 0.0 else acc.creditLimitBrl.coerceAtLeast(0.0)
            val availableLimit = if (isCash) 0.0 else (safeLimit - usedCredit).coerceAtLeast(0.0)
            val safeClosingDay = if (isCash || safeLimit <= 0.0) 0 else acc.closingDay.coerceIn(1, 31)
            val safeDueDay = if (isCash || safeLimit <= 0.0) 0 else acc.dueDay.coerceIn(1, 31)
            acc.copy(
                institution = resolvedInst,
                institutionColorHex = resolvedColor,
                colorHex = resolvedColor,
                iconEmoji = resolvedEmoji,
                emoji = resolvedEmoji,
                currentBalanceBrl = balance,
                creditLimitBrl = safeLimit,
                closingDay = safeClosingDay,
                dueDay = safeDueDay,
                usedCreditLimitBrl = if (isCash) 0.0 else usedCredit,
                availableCreditLimitBrl = availableLimit
            )
        }
    }

    fun openInvoiceForMonthBrl(
        accountId: String,
        yearMonth: String,
        transactions: List<FinanceTransaction>
    ): Double {
        return transactions
            .filter {
                it.type == "EXPENSE" &&
                    it.isCreditCard &&
                    !it.isPaid &&
                    it.yearMonth == yearMonth &&
                    (accountId.isBlank() || it.accountId == accountId)
            }
            .sumOf { it.amountBrl }
    }
}

@Keep
@IgnoreExtraProperties
data class Household(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "Família Omni",
    val ownerId: String = "",
    val migratedTo: String = "",
    val monthlyIncomeBrl: Double = 0.0,
    val needsPercentage: Long = 60L,
    val wantsPercentage: Long = 20L,
    val savingsPercentage: Long = 20L,
    val defaultSplitMode: String = SplitMode.EQUAL,
    val members: List<FamilyMemberBadge> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val categoryBudgetsBrl: Map<String, Double> = emptyMap(),
    val customCategories: List<String> = emptyList(),
    val muralItems: List<FamilyMuralItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * Retorna a lista oficial de membros da família deduplicada e normalizada,
     * sem placeholders genéricos ("Você", "VO", "Família" ou vazio) e com cores distintas.
     */
    fun membersWithAssignedColors(
        fallbackProfileName: String = "",
        fallbackProfileColorHex: Long = 0xFF0E7C5B,
        fallbackUserId: String = "",
        fallbackAvatarBase64: String = ""
    ): List<FamilyMemberBadge> {
        val defaultPalette = listOf(
            0xFF0E7C5B, // Esmeralda
            0xFF7B61FF, // Roxo
            0xFF2E7D9B, // Azul Oceano
            0xFFD84B72, // Rosa
            0xFFE07A2E, // Âmbar
            0xFF00897B, // Turquesa
            0xFF5C6BC0, // Índigo
            0xFFC62828  // Coral
        )

        fun isPlaceholderName(raw: String): Boolean {
            val clean = raw.trim().lowercase(Locale.ROOT)
            return clean.isBlank() ||
                clean == "você" ||
                clean == "voce" ||
                clean == "vo" ||
                clean == "família" ||
                clean == "familia" ||
                clean == "membro"
        }

        val cleanFallbackName = fallbackProfileName.trim().takeUnless { isPlaceholderName(it) } ?: ""

        // Deduplicar membros por userId (ou por nome normalizado quando userId estiver vazio ou coincidir)
        val mergedByKey = linkedMapOf<String, FamilyMemberBadge>()
        for (rawMember in members) {
            val isCurrent = (fallbackUserId.isNotBlank() && rawMember.userId == fallbackUserId) || rawMember.isCurrentUser
            val resolvedName = when {
                !isPlaceholderName(rawMember.displayName) -> rawMember.displayName.trim()
                isCurrent && cleanFallbackName.isNotBlank() -> cleanFallbackName
                rawMember.email.isNotBlank() -> rawMember.email.substringBefore('@').replaceFirstChar { it.titlecase(Locale.ROOT) }
                cleanFallbackName.isNotBlank() && members.size == 1 -> cleanFallbackName
                else -> ""
            }
            if (resolvedName.isBlank()) continue

            val normalizedNameKey = resolvedName.lowercase(Locale.ROOT)
            val existingKey = mergedByKey.entries.firstOrNull { (_, existing) ->
                (rawMember.userId.isNotBlank() && existing.userId == rawMember.userId) ||
                    existing.displayName.trim().lowercase(Locale.ROOT) == normalizedNameKey ||
                    (rawMember.email.isNotBlank() && existing.email.equals(rawMember.email, ignoreCase = true))
            }?.key ?: rawMember.userId.ifBlank { normalizedNameKey }

            val previous = mergedByKey[existingKey]
            val mergedAvatar = when {
                rawMember.avatarBase64.isNotBlank() -> rawMember.avatarBase64
                previous?.avatarBase64?.isNotBlank() == true -> previous.avatarBase64
                isCurrent && fallbackAvatarBase64.isNotBlank() -> fallbackAvatarBase64
                else -> ""
            }
            mergedByKey[existingKey] = FamilyMemberBadge(
                userId = rawMember.userId.ifBlank { previous?.userId.orEmpty() }.ifBlank {
                    if (isCurrent) fallbackUserId else ""
                },
                displayName = if (!isPlaceholderName(rawMember.displayName)) rawMember.displayName.trim()
                else previous?.displayName?.takeUnless { isPlaceholderName(it) } ?: resolvedName,
                email = rawMember.email.ifBlank { previous?.email.orEmpty() },
                avatarBase64 = mergedAvatar,
                colorHex = if (rawMember.colorHex != 0L) rawMember.colorHex else (previous?.colorHex ?: 0L),
                avatarEmoji = rawMember.avatarEmoji.ifBlank { previous?.avatarEmoji ?: "🌿" },
                role = rawMember.role.ifBlank { previous?.role ?: "ADMIN" },
                isCurrentUser = isCurrent || (previous?.isCurrentUser == true)
            )
        }

        val rawList = if (mergedByKey.isNotEmpty()) {
            mergedByKey.values.toList()
        } else if (cleanFallbackName.isNotBlank()) {
            listOf(
                FamilyMemberBadge(
                    userId = fallbackUserId.ifBlank { ownerId.ifBlank { "member_admin" } },
                    displayName = cleanFallbackName,
                    avatarBase64 = fallbackAvatarBase64,
                    colorHex = if (fallbackProfileColorHex != 0L) fallbackProfileColorHex else defaultPalette[0],
                    avatarEmoji = "🌿",
                    role = "ADMIN",
                    isCurrentUser = true
                )
            )
        } else {
            emptyList()
        }

        val usedColors = mutableSetOf<Long>()
        return rawList.mapIndexed { index, member ->
            val cleanColor = if (member.colorHex != 0L && !usedColors.contains(member.colorHex)) {
                member.colorHex
            } else {
                defaultPalette.firstOrNull { !usedColors.contains(it) }
                    ?: defaultPalette[index % defaultPalette.size]
            }
            usedColors.add(cleanColor)
            member.copy(
                displayName = member.displayName.trim(),
                colorHex = cleanColor
            )
        }
    }
}

/**
 * Recorrência de tarefas da Agenda (Passo 2 & Parte 3)
 */
object TaskRecurrence {
    const val ONCE = "ONCE"               // Única
    const val DAILY = "DAILY"             // Diária
    const val WEEKDAYS = "WEEKDAYS"       // Seg a Sex
    const val CUSTOM_DAYS = "CUSTOM_DAYS" // Dias da Semana específicos
    const val WEEKLY = "WEEKLY"           // Semanal
    const val BIWEEKLY = "BIWEEKLY"       // Quinzenal
    const val MONTHLY = "MONTHLY"         // Mensal

    fun labelPt(recurrence: String): String = when (recurrence) {
        DAILY -> "Diária"
        WEEKDAYS -> "Seg a Sex"
        CUSTOM_DAYS -> "Dias da Semana"
        WEEKLY -> "Semanal"
        BIWEEKLY -> "Quinzenal"
        MONTHLY -> "Mensal"
        else -> "Única"
    }

    val allOptions: List<Pair<String, String>> = listOf(
        ONCE to "Única",
        DAILY to "Diária",
        WEEKDAYS to "Seg a Sex",
        CUSTOM_DAYS to "Dias da Semana",
        WEEKLY to "Semanal",
        BIWEEKLY to "Quinzenal",
        MONTHLY to "Mensal"
    )
}

/**
 * Subtarefa / item de checklist de uma tarefa da Agenda (Passo 2)
 */
@Keep
@IgnoreExtraProperties
data class SubtaskItem(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val householdId: String = "",
    val title: String = "",
    val notes: String = "",
    val location: String = "", // Local opcional da tarefa/evento
    val category: String = "Finanças",
    val priority: String = "MEDIUM",
    val isEssential: Boolean = false, // Tarefa essencial / inegociável (mantida visível no Modo Dia Difícil)
    val dateEpochDay: Long = 0L,
    val endDateEpochDay: Long = 0L,   // Data final opcional do período (se > dateEpochDay, tarefa abrange o intervalo)
    val yearMonth: String = "",
    val alarmHour: Long = 9L,
    val alarmMinute: Long = 0L,
    val alarmEnabled: Boolean = false,
    val completed: Boolean = false,
    val completedDates: List<String> = emptyList(), // Datas concluídas no formato yyyy-MM-dd
    val recurrenceWeekdays: List<Int> = emptyList(), // Dias da semana ISO (1=Seg .. 7=Dom)
    // Passo 2: Time Blocking, Inbox, Subtarefas, Recorrência e Ringtone Individual
    val isInbox: Boolean = false,           // true quando criada sem horário definido -> vai para a aba Inbox
    val durationMinutes: Int = 30,          // Duração em minutos (15, 30, 45, 60, 90 ou custom em horas+minutos)
    val iconEmoji: String = "📋",           // Ícone visual da tarefa na Linha do Tempo
    val taskColorHex: Long = 0xFF0E7C5B,    // Cor escolhida para o bloco da tarefa
    val recurrence: String = TaskRecurrence.ONCE, // ONCE, DAILY, WEEKDAYS, CUSTOM_DAYS, WEEKLY, BIWEEKLY, MONTHLY
    val subtasks: List<SubtaskItem> = emptyList(), // Checklist de subtarefas (ex: 1/3)
    val ringtoneCategory: String = "APP_DEFAULT", // APP_DEFAULT, SUAVE, FOCO, URGENTE, CONTAS, DISCRETO ou URI customizado
    // Escopo de privacidade da tarefa
    val privacyScope: String = PrivacyScope.FAMILY, // FAMILY, PERSONAL, COUPLE, ASSIGNED_ONLY
    val visibleToMemberIds: List<String> = emptyList(),
    // Responsabilidade e identificação visual por membro
    val assignedMemberId: String = "",
    val assignedMemberName: String = "Família",
    val assignedMemberColorHex: Long = 0xFF0E7C5B,
    val assignedMemberEmoji: String = "🌿",
    val createdByName: String = "Família",
    val createdByColorHex: Long = 0xFF0E7C5B,
    val completedByUserId: String = "",
    val completedByName: String = "",
    // Integração Agenda <-> Finanças & Divisão de valores
    val linkedTransactionId: String = "",
    val linkedSubscriptionId: String = "",
    val estimatedCostBrl: Double = 0.0,
    val autoCreateExpenseOnComplete: Boolean = false,
    val splitMode: String = SplitMode.NONE,
    val splitShares: List<ExpenseSplitShare> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val hasDateRange: Boolean
        get() = endDateEpochDay > dateEpochDay && dateEpochDay > 0L

    val isRecurringOrMultiDay: Boolean
        get() = recurrence != TaskRecurrence.ONCE || hasDateRange || recurrenceWeekdays.isNotEmpty()

    fun isCompletedOnDate(targetEpochDay: Long): Boolean {
        val dateIso = DateCalendarUtils.formatIsoDate(targetEpochDay)
        if (completedDates.contains(dateIso)) return true
        return if (!isRecurringOrMultiDay) completed else false
    }

    fun withCompletedForDate(targetEpochDay: Long): TaskItem {
        return copy(completed = isCompletedOnDate(targetEpochDay))
    }

    fun occursOnEpochDay(targetEpochDay: Long): Boolean {
        if (isInbox) return false
        if (targetEpochDay < dateEpochDay) return false
        if (hasDateRange && targetEpochDay > endDateEpochDay) return false
        if (targetEpochDay == dateEpochDay) return true

        val targetDayOfWeek = java.time.LocalDate.ofEpochDay(targetEpochDay).dayOfWeek.value // 1=Seg .. 7=Dom
        return when (recurrence) {
            TaskRecurrence.ONCE -> hasDateRange && targetEpochDay in dateEpochDay..endDateEpochDay
            TaskRecurrence.DAILY -> true
            TaskRecurrence.WEEKDAYS -> targetDayOfWeek in 1..5
            TaskRecurrence.CUSTOM_DAYS -> {
                if (recurrenceWeekdays.isNotEmpty()) {
                    targetDayOfWeek in recurrenceWeekdays
                } else {
                    val startDayOfWeek = java.time.LocalDate.ofEpochDay(dateEpochDay).dayOfWeek.value
                    targetDayOfWeek == startDayOfWeek
                }
            }
            TaskRecurrence.WEEKLY -> {
                if (recurrenceWeekdays.isNotEmpty()) {
                    targetDayOfWeek in recurrenceWeekdays
                } else {
                    ((targetEpochDay - dateEpochDay) % 7L) == 0L
                }
            }
            TaskRecurrence.BIWEEKLY -> ((targetEpochDay - dateEpochDay) % 14L) == 0L
            TaskRecurrence.MONTHLY -> {
                val (_, _, startDay) = DateCalendarUtils.fromEpochDay(dateEpochDay)
                val (_, _, targetDay) = DateCalendarUtils.fromEpochDay(targetEpochDay)
                startDay == targetDay
            }
            else -> hasDateRange && targetEpochDay in dateEpochDay..endDateEpochDay
        }
    }
}

@Keep
@IgnoreExtraProperties
data class FinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val householdId: String = "",
    val title: String = "",
    val amountOriginal: Double = 0.0,
    val currency: String = "BRL",
    val exchangeRateToBrl: Double = 1.0,
    val amountBrl: Double = 0.0,
    val type: String = "EXPENSE", // EXPENSE, INCOME, TRANSFER
    val budgetBucket: String = "NEEDS", // NEEDS, WANTS, SAVINGS, INCOME, TRANSFER
    val category: String = "Alimentação",
    val dateEpochDay: Long = 0L,
    val yearMonth: String = "",
    val recurringSourceId: String = "",
    // Conta Bancária vinculada (Origem ou Destino)
    val accountId: String = "acc_nubank",
    val accountName: String = "Nubank",
    val destinationAccountId: String = "",
    val destinationAccountName: String = "",
    // Receita/Despesa Fixa ou Recorrente
    val isRecurring: Boolean = false,
    // Crédito Parcelado separado do Saldo Imediato
    val isCreditCard: Boolean = false,
    val installmentCount: Int = 1,
    val installmentIndex: Int = 1,
    val parentTransactionId: String = "",
    val isPaid: Boolean = true, // Para crédito parcelado inicia false e só abate da conta quando marcada como Paga
    // Identificação visual de quem criou
    val createdByName: String = "",
    val createdByColorHex: Long = 0xFF0E7C5B,
    val createdByEmoji: String = "🌿",
    // Escopo de privacidade da transação
    val privacyScope: String = PrivacyScope.FAMILY, // FAMILY, PERSONAL, COUPLE, ASSIGNED_ONLY
    val visibleToMemberIds: List<String> = emptyList(),
    // Responsabilidade e divisão de valores entre membros
    val responsibleMemberId: String = "",
    val responsibleMemberName: String = "Família",
    val responsibleMemberColorHex: Long = 0xFF0E7C5B,
    val responsibleMemberEmoji: String = "🌿",
    val splitMode: String = SplitMode.NONE, // NONE, EQUAL, PERCENTAGE, CUSTOM_AMOUNT
    val splitWithMemberIds: List<String> = emptyList(),
    val splitSharesMap: Map<String, Double> = emptyMap(),
    val splitShares: List<ExpenseSplitShare> = emptyList(),
    // Integração Finanças <-> Agenda
    val linkedTaskId: String = "",
    val generateCalendarTask: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionBill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val householdId: String = "",
    val title: String = "",
    val amountOriginal: Double = 0.0,
    val currency: String = "BRL",
    val exchangeRateToBrl: Double = 1.0,
    val amountBrl: Double = 0.0,
    val category: String = "Streaming",
    val budgetBucket: String = "WANTS",
    val dueDayOfMonth: Long = 10L,
    val reminderDaysBefore: Long = 2L,
    val autoPostEnabled: Boolean = true,
    val lastPostedYearMonth: String = "",
    val active: Boolean = true,
    // Escopo de privacidade
    val privacyScope: String = PrivacyScope.FAMILY,
    val visibleToMemberIds: List<String> = emptyList(),
    // Responsabilidade e identificação visual por membro
    val responsibleMemberId: String = "",
    val responsibleMemberName: String = "Família",
    val responsibleMemberColorHex: Long = 0xFF0E7C5B,
    val responsibleMemberEmoji: String = "🌿",
    // Divisão de valores da assinatura/conta fixa
    val splitMode: String = SplitMode.NONE,
    val splitWithMemberIds: List<String> = emptyList(),
    val splitSharesMap: Map<String, Double> = emptyMap(),
    val splitShares: List<ExpenseSplitShare> = emptyList(),
    // Integração com a Agenda de Tarefas & Ringtone de Contas
    val linkedTaskId: String = "",
    val syncDueDayToCalendar: Boolean = true,
    val ringtoneCategory: String = "CONTAS",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val householdId: String = "",
    val title: String = "",
    val targetAmountBrl: Double = 1000.0,
    val currentAmountBrl: Double = 0.0,
    val incomePercentage: Long = 10L,
    val category: String = "Reserva",
    val targetYearMonth: String = "",
    // Escopo de privacidade e identificação visual do responsável pela meta
    val privacyScope: String = PrivacyScope.FAMILY,
    val responsibleMemberId: String = "",
    val responsibleMemberName: String = "Família",
    val responsibleMemberColorHex: Long = 0xFF0E7C5B,
    val responsibleMemberEmoji: String = "🌿",
    val splitMode: String = SplitMode.EQUAL,
    val memberContributionsMap: Map<String, Double> = emptyMap(),
    val linkedTaskId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CurrencyRatesState(
    val usdToBrl: Double = 5.78,
    val eurToBrl: Double = 6.12,
    val gbpToBrl: Double = 7.34,
    val lastUpdatedLabel: String = "Taxas de referência BCB/ECB",
    val isLive: Boolean = false
) {
    fun rateFor(currency: String): Double = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> usdToBrl
        "EUR" -> eurToBrl
        "GBP" -> gbpToBrl
        else -> 1.0
    }

    fun symbolFor(currency: String): String = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "US$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "R$"
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun formatCurrencyBrl(amount: Double, hideValues: Boolean = false): String {
    if (hideValues) return "R$ •••••"
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", amount)
}

fun formatCurrencyOriginal(amount: Double, currency: String, hideValues: Boolean = false): String {
    val symbol = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "US$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "R$"
    }
    if (hideValues) return "$symbol •••••"
    return String.format(Locale.forLanguageTag("pt-BR"), "%s %,.2f", symbol, amount)
}

