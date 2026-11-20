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
    val colorHex: Long = 0xFF0E7C5B,
    val avatarEmoji: String = "🌿",
    val role: String = "ADMIN"
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
 * Slots padrão de medição de Glicemia / Diabetes
 */
object DiabetesSlotCatalog {
    const val JEJUM = "Jejum"
    const val POS_CAFE_1H = "1h pós-café"
    const val PRE_ALMOCO = "Pré-almoço"
    const val POS_ALMOCO_1H = "1h pós-almoço"
    const val PRE_JANTAR_1H = "1h pré-jantar"
    const val POS_JANTAR_1H = "1h pós-jantar"
    const val ANTES_DORMIR = "Antes de dormir"

    val allSlots: List<String> = listOf(
        JEJUM,
        POS_CAFE_1H,
        PRE_ALMOCO,
        POS_ALMOCO_1H,
        PRE_JANTAR_1H,
        POS_JANTAR_1H,
        ANTES_DORMIR
    )

    val defaultSelectedSlots: Set<String> = setOf(
        JEJUM,
        POS_ALMOCO_1H,
        POS_JANTAR_1H
    )
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
)

@Keep
@IgnoreExtraProperties
data class Household(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "Orçamento da Família",
    val ownerId: String = "",
    val monthlyIncomeBrl: Double = 8500.0,
    val needsPercentage: Long = 50L,
    val wantsPercentage: Long = 30L,
    val savingsPercentage: Long = 20L,
    val defaultSplitMode: String = SplitMode.EQUAL,
    val members: List<FamilyMemberBadge> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Recorrência de tarefas da Agenda (Passo 2)
 */
object TaskRecurrence {
    const val ONCE = "ONCE"         // Única
    const val DAILY = "DAILY"       // Diária
    const val WEEKLY = "WEEKLY"     // Semanal
    const val BIWEEKLY = "BIWEEKLY" // Quinzenal
    const val MONTHLY = "MONTHLY"   // Mensal

    fun labelPt(recurrence: String): String = when (recurrence) {
        DAILY -> "Diária"
        WEEKLY -> "Semanal"
        BIWEEKLY -> "Quinzenal"
        MONTHLY -> "Mensal"
        else -> "Única"
    }

    val allOptions: List<Pair<String, String>> = listOf(
        ONCE to "Única",
        DAILY to "Diária",
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
    val category: String = "Finanças",
    val priority: String = "MEDIUM",
    val isEssential: Boolean = false, // Tarefa essencial / inegociável (mantida visível no Modo Dia Difícil)
    val dateEpochDay: Long = 0L,
    val yearMonth: String = "",
    val alarmHour: Long = 9L,
    val alarmMinute: Long = 0L,
    val alarmEnabled: Boolean = false,
    val completed: Boolean = false,
    // Passo 2: Time Blocking, Inbox, Subtarefas, Recorrência e Ringtone Individual
    val isInbox: Boolean = false,           // true quando criada sem horário definido -> vai para a aba Inbox
    val durationMinutes: Int = 30,          // Duração em minutos (15, 30, 45, 60, 90 ou custom)
    val iconEmoji: String = "📋",           // Ícone visual da tarefa na Linha do Tempo
    val taskColorHex: Long = 0xFF0E7C5B,    // Cor escolhida para o bloco da tarefa
    val recurrence: String = TaskRecurrence.ONCE, // ONCE, DAILY, WEEKLY, BIWEEKLY, MONTHLY
    val subtasks: List<SubtaskItem> = emptyList(), // Checklist de subtarefas (ex: 1/3)
    val ringtoneCategory: String = "SUAVE", // SUAVE, FOCO, URGENTE, CONTAS, DISCRETO
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
)

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
    val type: String = "EXPENSE", // EXPENSE or INCOME
    val budgetBucket: String = "NEEDS", // NEEDS, WANTS, SAVINGS, INCOME
    val category: String = "Alimentação",
    val dateEpochDay: Long = 0L,
    val yearMonth: String = "",
    val recurringSourceId: String = "",
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

fun formatCurrencyBrl(amount: Double): String {
    return String.format(Locale("pt", "BR"), "R$ %,.2f", amount)
}

fun formatCurrencyOriginal(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "US$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "R$"
    }
    return String.format(Locale("pt", "BR"), "%s %,.2f", symbol, amount)
}
