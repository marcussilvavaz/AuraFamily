package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.BankAccount
import com.example.data.BankAccountCatalog
import com.example.data.CurrencyRatesState
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.PrivacyScope
import com.example.data.SubscriptionBill
import com.example.data.formatCurrencyBrl
import com.example.data.formatCurrencyOriginal
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.NeedsBlue
import com.example.ui.theme.SavingsGold
import com.example.ui.theme.WantsAmber
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinancesScreen(
    selectedYearMonth: String,
    bankAccounts: List<BankAccount> = BankAccountCatalog.defaultAccounts(),
    transactions: List<FinanceTransaction>,
    subscriptions: List<SubscriptionBill>,
    currencyRates: CurrencyRatesState,
    onRefreshRates: () -> Unit,
    onUpdateManualRate: (String, Double) -> Unit,
    onOpenNewTransactionDialog: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit,
    onSetCreditTransactionPaid: (FinanceTransaction, Boolean, String, String) -> Unit = { _, _, _, _ -> },
    onPayAllCreditForMonth: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveBankAccount: (BankAccount) -> Unit = {},
    onDeleteBankAccount: (String, String) -> Unit = { _, _ -> },
    onAddSubscription: (
        title: String,
        amountOriginal: Double,
        currency: String,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String,
        syncDueDayToCalendar: Boolean
    ) -> Unit,
    onUpdateSubscription: (
        existing: SubscriptionBill,
        title: String,
        amountOriginal: Double,
        currency: String,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String,
        syncDueDayToCalendar: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onPostSubscriptionNow: (SubscriptionBill) -> Unit,
    onAutoPostAllSubscriptions: () -> Unit,
    onDeleteSubscription: (String) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val triggerFeedback = rememberOmniFeedback()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Extrato & Lançamentos, 1 = Custos Mensais & Assinaturas
    var scopeFilter by remember { mutableStateOf("ALL") } // ALL, FAMILY, PERSONAL
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE, TRANSFER, CREDIT
    var selectedAccountFilter by remember { mutableStateOf<String?>(null) }

    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<SubscriptionBill?>(null) }
    var deletingSubscription by remember { mutableStateOf<SubscriptionBill?>(null) }
    var deletingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }

    var editingBankAccount by remember { mutableStateOf<BankAccount?>(null) }
    var showBankAccountDialog by remember { mutableStateOf(false) }
    var deletingBankAccount by remember { mutableStateOf<BankAccount?>(null) }
    var showFullStatementModal by remember { mutableStateOf(false) }

    var showEditRatesDialog by remember { mutableStateOf(false) }
    var quickConverterAmountText by remember { mutableStateOf("100") }
    var quickConverterCurrency by remember { mutableStateOf("USD") }

    // Live recalculated balances across all registered bank accounts
    val allCalculatedAccounts = remember(bankAccounts, transactions) {
        BankAccountCatalog.calculateCurrentBalances(bankAccounts, transactions)
    }
    val visibleAccounts = remember(allCalculatedAccounts, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> allCalculatedAccounts.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> allCalculatedAccounts.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> allCalculatedAccounts
        }
    }
    val totalAccountsBalanceBrl = remember(visibleAccounts) {
        visibleAccounts.sumOf { it.currentBalanceBrl }
    }

    val allMonthTransactions = remember(transactions, selectedYearMonth) {
        transactions.filter { it.yearMonth == selectedYearMonth }
    }

    val scopeFilteredTransactions = remember(allMonthTransactions, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> allMonthTransactions.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> allMonthTransactions.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> allMonthTransactions
        }
    }

    val monthTransactions = remember(scopeFilteredTransactions, typeFilter, selectedAccountFilter) {
        scopeFilteredTransactions.filter { tx ->
            val matchesAccount = selectedAccountFilter == null ||
                tx.accountId == selectedAccountFilter ||
                tx.destinationAccountId == selectedAccountFilter
            val matchesType = when (typeFilter) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE" && !tx.isCreditCard
                "CREDIT" -> tx.type == "EXPENSE" && tx.isCreditCard
                "TRANSFER" -> tx.type == "TRANSFER"
                else -> true
            }
            matchesAccount && matchesType
        }
    }

    val monthIncomesBrl = remember(scopeFilteredTransactions) {
        scopeFilteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val monthImmediateOutflowsBrl = remember(scopeFilteredTransactions) {
        scopeFilteredTransactions.filter { it.type == "EXPENSE" && (!it.isCreditCard || it.isPaid) }.sumOf { it.amountBrl }
    }
    val monthCreditOpenBrl = remember(scopeFilteredTransactions) {
        scopeFilteredTransactions.filter { it.type == "EXPENSE" && it.isCreditCard && !it.isPaid }.sumOf { it.amountBrl }
    }

    val filteredSubscriptions = remember(subscriptions, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> subscriptions.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> subscriptions.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> subscriptions
        }
    }

    val familyItemsCount = remember(allMonthTransactions, subscriptions, activeSubTab) {
        if (activeSubTab == 0) {
            allMonthTransactions.count { it.privacyScope != PrivacyScope.PERSONAL }
        } else {
            subscriptions.count { it.privacyScope != PrivacyScope.PERSONAL }
        }
    }

    val personalItemsCount = remember(allMonthTransactions, subscriptions, activeSubTab) {
        if (activeSubTab == 0) {
            allMonthTransactions.count { it.privacyScope == PrivacyScope.PERSONAL }
        } else {
            subscriptions.count { it.privacyScope == PrivacyScope.PERSONAL }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("finances_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Sub-Tab selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contas, Extrato & Lançamentos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mês: ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)} • Individual & Familiar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (activeSubTab == 0) onOpenNewTransactionDialog()
                            else {
                                editingSubscription = null
                                showAddSubscriptionDialog = true
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("finances_primary_add_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (activeSubTab == 0) "+ Novo Lançamento" else "Novo Custo Mensal",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scope Filter Bar: Tudo vs Compartilhadas (Família) vs Individuais (Só Meu)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Alternar Financeiro: Tudo vs. Familiar vs. Individual",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = scopeFilter == "ALL",
                                onClick = { scopeFilter = "ALL" },
                                label = { Text("Tudo (${familyItemsCount + personalItemsCount})", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("finance_scope_all")
                            )
                            FilterChip(
                                selected = scopeFilter == PrivacyScope.FAMILY,
                                onClick = { scopeFilter = PrivacyScope.FAMILY },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FamilyRestroom,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Familiar ($familyItemsCount)") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("finance_scope_family")
                            )
                            FilterChip(
                                selected = scopeFilter == PrivacyScope.PERSONAL,
                                onClick = { scopeFilter = PrivacyScope.PERSONAL },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Individual ($personalItemsCount)") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("finance_scope_personal")
                            )
                        }
                    }
                }

                // Saldo Total, Contas Bancárias & Resumo do Extrato
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finances_accounts_summary_card"),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SALDO TOTAL EM CONTAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrencyBrl(totalAccountsBalanceBrl, hideValues),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalAccountsBalanceBrl >= 0.0) IncomeEmerald else ExpenseCoral
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showFullStatementModal = true },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("finances_open_extrato_modal_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Extrato", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        editingBankAccount = null
                                        showBankAccountDialog = true
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("finances_add_bank_account_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Conta", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        // Monthly Inflow / Outflow / Credit Invoice Summary Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = IncomeEmerald.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Entradas Mês", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "+ ${formatCurrencyBrl(monthIncomesBrl, hideValues)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IncomeEmerald
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ExpenseCoral.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Saídas (Débito/Pix)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "- ${formatCurrencyBrl(monthImmediateOutflowsBrl, hideValues)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ExpenseCoral
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = WantsAmber.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Fatura Crédito", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatCurrencyBrl(monthCreditOpenBrl, hideValues),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WantsAmber
                                    )
                                }
                            }
                        }

                        // Cartões & Faturas de cada Conta (Limite Usado x Disponível + Fechamento/Vencimento + Pagar Fatura)
                        Text(
                            text = "Cartões & Limites de Crédito por Conta:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        visibleAccounts.forEach { acc ->
                            val accColor = if (acc.colorHex != 0L) Color(acc.colorHex.toInt()) else MaterialTheme.colorScheme.primary
                            val openInvMonth = remember(acc.id, selectedYearMonth, transactions) {
                                BankAccountCatalog.openInvoiceForMonthBrl(acc.id, selectedYearMonth, transactions)
                            }
                            val usageRatio = if (acc.creditLimitBrl > 0.0) {
                                (acc.usedCreditLimitBrl / acc.creditLimitBrl).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = accColor.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, accColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${acc.emoji} ${acc.name} • Saldo: ${formatCurrencyBrl(acc.currentBalanceBrl, hideValues)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (!acc.isCashOrWallet() && acc.creditLimitBrl > 0.0 && acc.closingDay > 0 && acc.dueDay > 0) {
                                                    "Fecha dia ${acc.closingDay} • Vence dia ${acc.dueDay} • Fatura do mês: ${formatCurrencyBrl(openInvMonth, hideValues)}"
                                                } else {
                                                    "${acc.institution} • Conta à vista / Carteira (sem fatura de cartão)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!acc.isCashOrWallet() && openInvMonth > 0.009) {
                                            FilledTonalButton(
                                                onClick = {
                                                    onPayAllCreditForMonth(selectedYearMonth, acc.id, acc.name)
                                                    triggerFeedback("Fatura do mês de ${acc.name} paga e limite liberado!")
                                                },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("finances_pay_invoice_btn_${acc.id}")
                                            ) {
                                                Text(
                                                    text = "Pagar Fatura do Mês",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                    if (!acc.isCashOrWallet() && acc.creditLimitBrl > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Usado: ${formatCurrencyBrl(acc.usedCreditLimitBrl, hideValues)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (usageRatio > 0.85f) ExpenseCoral else WantsAmber
                                            )
                                            Text(
                                                text = "Disponível: ${formatCurrencyBrl(acc.availableCreditLimitBrl, hideValues)} / ${formatCurrencyBrl(acc.creditLimitBrl, hideValues)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = IncomeEmerald
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { usageRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = if (usageRatio > 0.85f) ExpenseCoral else WantsAmber,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Filter by Bank Account chips + Edit Account action
                        Text(
                            text = "Contas Cadastradas (toque para filtrar o extrato):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedAccountFilter == null,
                                onClick = { selectedAccountFilter = null },
                                label = { Text("Todas as Contas") },
                                shape = RoundedCornerShape(50)
                            )
                            visibleAccounts.forEach { acc ->
                                val isSelected = selectedAccountFilter == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAccountFilter = if (isSelected) null else acc.id
                                    },
                                    label = {
                                        Text(
                                            text = "${acc.emoji} ${acc.name}: ${formatCurrencyBrl(acc.currentBalanceBrl, hideValues)}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                editingBankAccount = acc
                                                showBankAccountDialog = true
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar conta ${acc.name}",
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = activeSubTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text("Extrato & Lançamentos (${monthTransactions.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_daily_expenses")
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("Custos Mensais (${filteredSubscriptions.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_subscriptions")
                    )
                }

                // Transaction type filter pills (Todos, Entradas, Saídas, Crédito, Transferências)
                if (activeSubTab == 0) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "Tudo",
                            "INCOME" to "Entradas (Receitas)",
                            "EXPENSE" to "Saídas (À Vista)",
                            "CREDIT" to "Fatura / Crédito",
                            "TRANSFER" to "Transferências"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = typeFilter == code,
                                onClick = { typeFilter = code },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("extrato_type_filter_$code")
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Multi-Currency Converter Card (Menu Expansível)
        item {
            val amountVal = quickConverterAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
            val rate = currencyRates.rateFor(quickConverterCurrency)
            val convertedBrl = amountVal * rate

            AuraExpandableSectionCard(
                title = "💱 Simulador & Câmbio Automático (BRL)",
                subtitle = "${currencyRates.lastUpdatedLabel} • 1 $quickConverterCurrency = ${String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", rate)}",
                icon = Icons.Default.CurrencyExchange,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeText = formatCurrencyBrl(convertedBrl),
                initiallyExpanded = false,
                testTag = "expandable_currency_converter_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencyRates.lastUpdatedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(
                            onClick = { showEditRatesDialog = true },
                            modifier = Modifier.testTag("edit_rates_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ajustar taxas manualmente"
                            )
                        }
                        IconButton(onClick = onRefreshRates) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar cotações"
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickConverterAmountText,
                        onValueChange = { quickConverterAmountText = it },
                        label = { Text("Valor em $quickConverterCurrency") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("converter_amount_input")
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Equivalente em Real",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrencyBrl(convertedBrl),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.forLanguageTag("pt-BR"), "1 %s = R$ %.2f", quickConverterCurrency, rate),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USD", "EUR", "GBP").forEach { code ->
                        FilterChip(
                            selected = quickConverterCurrency == code,
                            onClick = { quickConverterCurrency = code },
                            label = { Text("$code (${currencyRates.symbolFor(code)})") }
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
            // DAILY TRANSACTIONS & STATEMENT LIST
            if (monthTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cardRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhum lançamento nesta visualização",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque em '+ Novo Lançamento' para registrar Despesas (à vista ou no crédito parcelado), Receitas ou Transferências entre contas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(monthTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        onEdit = { onEditTransaction(tx) },
                        onToggleCreditPaid = {
                            val newPaid = !tx.isPaid
                            onSetCreditTransactionPaid(tx, newPaid, tx.accountId, tx.accountName)
                            triggerFeedback(
                                if (newPaid) "Parcela marcada como Paga e descontada de ${tx.accountName}!"
                                else "Parcela voltou para Em Aberto na fatura"
                            )
                        },
                        onDelete = { deletingTransaction = tx }
                    )
                }
            }
        } else {
            // RECURRING SUBSCRIPTIONS & MONTHLY COSTS LIST
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custos Mensais Sincronizados com a Agenda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ao criar ou editar um custo mensal com dia de vencimento, ele aparece automaticamente na Agenda de Tarefas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                onAutoPostAllSubscriptions()
                                triggerFeedback("Custos mensais lançados!")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("run_auto_post_subscriptions_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lançar Todas")
                        }
                    }
                }
            }

            if (filteredSubscriptions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cardRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhum custo mensal nesta visualização",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cadastre contas fixas (Aluguel, Luz, Internet, Academia, Cartão) compartilhadas ou individuais para criar lembretes automáticos na Agenda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredSubscriptions, key = { it.id }) { sub ->
                    SubscriptionBillCard(
                        subscription = sub,
                        selectedYearMonth = selectedYearMonth,
                        onPostForMonth = {
                            onPostSubscriptionNow(sub)
                            triggerFeedback("${sub.title} lançada no mês!")
                        },
                        onEdit = {
                            editingSubscription = sub
                            showAddSubscriptionDialog = true
                        },
                        onDelete = { deletingSubscription = sub }
                    )
                }
            }
        }
    }

    if (showAddSubscriptionDialog) {
        val existingSub = editingSubscription
        AddSubscriptionDialog(
            initialSubscription = existingSub,
            currencyRates = currencyRates,
            onDismiss = {
                showAddSubscriptionDialog = false
                editingSubscription = null
            },
            onSave = { title, amountOrig, currency, category, bucket, dueDay, reminderDays, autoPost, privacyScope, syncToAgenda ->
                if (existingSub == null) {
                    onAddSubscription(
                        title,
                        amountOrig,
                        currency,
                        category,
                        bucket,
                        dueDay,
                        reminderDays,
                        autoPost,
                        privacyScope,
                        syncToAgenda
                    )
                    triggerFeedback("Custo mensal '$title' criado com sucesso!")
                } else {
                    onUpdateSubscription(
                        existingSub,
                        title,
                        amountOrig,
                        currency,
                        category,
                        bucket,
                        dueDay,
                        reminderDays,
                        autoPost,
                        privacyScope,
                        syncToAgenda
                    )
                    triggerFeedback("Custo mensal '$title' atualizado!")
                }
                editingSubscription = null
            }
        )
    }

    if (showEditRatesDialog) {
        EditCurrencyRatesDialog(
            currencyRates = currencyRates,
            onDismiss = { showEditRatesDialog = false },
            onSaveRate = { code, rate ->
                onUpdateManualRate(code, rate)
                triggerFeedback("Cotações atualizadas!")
            }
        )
    }

    if (showBankAccountDialog) {
        BankAccountEditorDialog(
            initialAccount = editingBankAccount,
            onDismiss = {
                showBankAccountDialog = false
                editingBankAccount = null
            },
            onSave = { savedAcc ->
                onSaveBankAccount(savedAcc)
                triggerFeedback("Conta '${savedAcc.name}' salva com sucesso!")
                showBankAccountDialog = false
                editingBankAccount = null
            }
        )
    }

    val accToDelete = deletingBankAccount
    if (accToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Conta Bancária",
            message = "Deseja excluir a conta '${accToDelete.name}'?",
            onConfirm = {
                onDeleteBankAccount(accToDelete.id, accToDelete.name)
                triggerFeedback("Conta '${accToDelete.name}' excluída")
                deletingBankAccount = null
            },
            onDismiss = { deletingBankAccount = null }
        )
    }

    val txToDelete = deletingTransaction
    if (txToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Lançamento",
            message = "Deseja excluir '${txToDelete.title}' (${formatCurrencyBrl(txToDelete.amountBrl)})? O saldo da conta vinculada será recalculado automaticamente.",
            onConfirm = {
                onDeleteTransaction(txToDelete.id)
                triggerFeedback("Lançamento excluído e saldo recalculado!")
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null }
        )
    }

    val subToDelete = deletingSubscription
    if (subToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Custo Mensal",
            message = "Deseja remover o custo mensal '${subToDelete.title}'?",
            onConfirm = {
                onDeleteSubscription(subToDelete.id)
                triggerFeedback("Custo mensal removido!")
                deletingSubscription = null
            },
            onDismiss = { deletingSubscription = null }
        )
    }

    if (showFullStatementModal) {
        FullStatementDialog(
            selectedYearMonth = selectedYearMonth,
            accounts = visibleAccounts,
            transactions = scopeFilteredTransactions,
            onEditTransaction = { tx ->
                showFullStatementModal = false
                onEditTransaction(tx)
            },
            onDeleteTransaction = { tx ->
                deletingTransaction = tx
            },
            onDismiss = { showFullStatementModal = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionItemCard(
    tx: FinanceTransaction,
    onEdit: () -> Unit,
    onToggleCreditPaid: () -> Unit = {},
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val isIncome = tx.type == "INCOME"
    val isTransfer = tx.type == "TRANSFER"
    val isPersonal = tx.privacyScope == PrivacyScope.PERSONAL
    val isLinkedToAgenda = tx.generateCalendarTask || tx.linkedTaskId.isNotBlank()
    val accentColor = when {
        isIncome -> IncomeEmerald
        isTransfer -> NeedsBlue
        tx.isCreditCard && !tx.isPaid -> WantsAmber
        else -> ExpenseCoral
    }

    val bucketLabel = when {
        isTransfer -> "Transferência entre Contas"
        isIncome -> if (tx.isRecurring) "Receita Fixa/Recorrente" else "Receita"
        tx.budgetBucket == "NEEDS" -> "50% Necessidades"
        tx.budgetBucket == "WANTS" -> "30% Desejos"
        tx.budgetBucket == "SAVINGS" -> "20% Economia"
        else -> "Despesa"
    }
    val bucketColor = when {
        isTransfer -> NeedsBlue
        isIncome -> IncomeEmerald
        tx.budgetBucket == "NEEDS" -> NeedsBlue
        tx.budgetBucket == "WANTS" -> WantsAmber
        tx.budgetBucket == "SAVINGS" -> SavingsGold
        else -> ExpenseCoral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${tx.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isIncome -> Icons.Default.TrendingUp
                        isTransfer -> Icons.Default.SwapHoriz
                        tx.isCreditCard -> Icons.Default.CreditCard
                        else -> Icons.Default.TrendingDown
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val accountInfo = if (isTransfer) {
                    "${tx.accountName} → ${tx.destinationAccountName}"
                } else {
                    "Conta: ${tx.accountName.ifBlank { "Conta Principal" }}"
                }
                Text(
                    text = "${tx.category} • ${DateCalendarUtils.formatEpochDayPt(tx.dateEpochDay)} • $accountInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Scope Badge: Individual vs Família
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isPersonal) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPersonal) Icons.Default.Lock else Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isPersonal) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPersonal) "Individual" else "Família",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPersonal) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }

                    // Budget Bucket Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = bucketColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = bucketLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = bucketColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    if (tx.isCreditCard) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (tx.isPaid) IncomeEmerald.copy(alpha = 0.16f) else WantsAmber.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (tx.isPaid) {
                                    "💳 Crédito Pago (${tx.installmentIndex}/${tx.installmentCount})"
                                } else {
                                    "💳 Fatura em Aberto (${tx.installmentIndex}/${tx.installmentCount})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.isPaid) IncomeEmerald else WantsAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Linked Agenda Task Badge
                    if (isLinkedToAgenda) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SavingsGold.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Na Agenda",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (tx.currency != "BRL") {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${formatCurrencyOriginal(tx.amountOriginal, tx.currency)} (R$ ${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", tx.exchangeRateToBrl)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val signPrefix = when {
                    isIncome -> "+ "
                    isTransfer -> "⇄ "
                    else -> "- "
                }
                Text(
                    text = signPrefix + formatCurrencyBrl(tx.amountBrl, hideValues),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (tx.isCreditCard) {
                    TextButton(
                        onClick = onToggleCreditPaid,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("tx_card_toggle_credit_paid_${tx.id}")
                    ) {
                        Text(
                            text = if (tx.isPaid) "Estornar" else "Marcar Paga",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_tx_btn_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar lançamento",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_tx_btn_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir lançamento",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionBillCard(
    subscription: SubscriptionBill,
    selectedYearMonth: String,
    onPostForMonth: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val isPostedForMonth = subscription.lastPostedYearMonth == selectedYearMonth
    val isPersonal = subscription.privacyScope == PrivacyScope.PERSONAL
    val isSyncedToAgenda = subscription.syncDueDayToCalendar || subscription.linkedTaskId.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_card_${subscription.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${subscription.category} • Pagar até dia ${subscription.dueDayOfMonth} de cada mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrencyBrl(subscription.amountBrl, hideValues),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (subscription.currency != "BRL") {
                        Text(
                            text = formatCurrencyOriginal(subscription.amountOriginal, subscription.currency),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scope & Agenda sync badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPersonal) Icons.Default.Lock else Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isPersonal) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersonal) "Individual (Só Meu)" else "Compartilhada (Família)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPersonal) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }

                if (isSyncedToAgenda) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SavingsGold.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Na Agenda (Dia ${subscription.dueDayOfMonth})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lembrete ${subscription.reminderDaysBefore} dia(s) antes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPostedForMonth) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = IncomeEmerald.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paga no mês ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPostForMonth,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("post_sub_btn_${subscription.id}")
                        ) {
                            Text("Pagar / Lançar", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_sub_btn_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar custo mensal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_sub_btn_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover assinatura",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Tab Unified Transaction Dialog (+ Novo Lançamento / Editar Lançamento):
 * - Tab 0: Despesa (À Vista/Débito/Pix ou Crédito Parcelado em 1x..24x, vinculada à Conta Bancária)
 * - Tab 1: Receita (Salário, Extra, Vendas, Reembolso, Outros • Fixa/Recorrente ou Avulsa • Conta de destino)
 * - Tab 2: Transferência (Conta de Origem -> Conta de Destino sem contabilizar como gasto ou receita nos relatórios)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialEpochDay: Long,
    initialTransaction: FinanceTransaction? = null,
    bankAccounts: List<BankAccount> = BankAccountCatalog.defaultAccounts(),
    currencyRates: CurrencyRatesState,
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveTransaction: (
        title: String,
        amountOriginal: Double,
        currency: String,
        type: String,
        budgetBucket: String,
        category: String,
        dateEpochDay: Long,
        privacyScope: String,
        generateCalendarTask: Boolean,
        accountId: String,
        accountName: String,
        destinationAccountId: String,
        destinationAccountName: String,
        isRecurring: Boolean,
        isCreditCard: Boolean,
        installmentCount: Int,
        isPaid: Boolean
    ) -> Unit
) {
    val triggerFeedback = rememberOmniFeedback()
    val effectiveAccounts = remember(bankAccounts) {
        bankAccounts.ifEmpty { BankAccountCatalog.defaultAccounts() }
    }
    val firstAcc = effectiveAccounts.first()
    val secondAcc = effectiveAccounts.getOrNull(1) ?: firstAcc

    var type by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.type ?: "EXPENSE") // "EXPENSE", "INCOME", "TRANSFER"
    }
    var title by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.title ?: "")
    }
    var amountText by remember(initialTransaction) {
        mutableStateOf(
            if (initialTransaction != null && initialTransaction.amountOriginal > 0.0) {
                String.format(Locale.US, "%.2f", initialTransaction.amountOriginal)
            } else ""
        )
    }
    var currency by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.currency ?: "BRL")
    }
    var budgetBucket by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.budgetBucket ?: "NEEDS")
    }
    var category by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.category ?: "Alimentação")
    }
    var privacyScope by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.privacyScope ?: PrivacyScope.FAMILY)
    }
    var dateDigits by remember(initialTransaction, initialEpochDay) {
        mutableStateOf(
            OmniDateMaskUtils.epochDayToDigits(initialTransaction?.dateEpochDay ?: initialEpochDay)
        )
    }
    var generateCalendarTask by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.generateCalendarTask ?: false)
    }

    // Bank account selection
    var selectedAccountId by remember(initialTransaction, effectiveAccounts) {
        mutableStateOf(
            initialTransaction?.accountId?.takeIf { id -> effectiveAccounts.any { it.id == id } }
                ?: firstAcc.id
        )
    }
    var selectedDestinationAccountId by remember(initialTransaction, effectiveAccounts) {
        mutableStateOf(
            initialTransaction?.destinationAccountId?.takeIf { id -> effectiveAccounts.any { it.id == id } }
                ?: secondAcc.id
        )
    }

    // Income recurrence (Fixa/Recorrente vs Avulsa)
    var isRecurringIncome by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.isRecurring ?: false)
    }

    // Credit Card & Installments (Crédito Parcelado separado do Saldo Imediato)
    var isCreditCard by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.isCreditCard ?: false)
    }
    var installmentCountText by remember(initialTransaction) {
        mutableStateOf((initialTransaction?.installmentCount ?: 1).toString())
    }
    var isCreditPaid by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.isPaid ?: false)
    }

    val selectedAccount = remember(selectedAccountId, effectiveAccounts) {
        effectiveAccounts.firstOrNull { it.id == selectedAccountId } ?: firstAcc
    }
    val selectedDestinationAccount = remember(selectedDestinationAccountId, effectiveAccounts) {
        effectiveAccounts.firstOrNull { it.id == selectedDestinationAccountId } ?: secondAcc
    }

    val currencies = listOf("BRL", "USD", "EUR", "GBP")
    val defaultExpenseCategories = listOf(
        "Alimentação",
        "Moradia",
        "Farmácia",
        "Exames",
        "Consultas",
        "Pré-Natal",
        "Saúde",
        "Transporte",
        "Lazer",
        "Educação",
        "Tecnologia",
        "Outros"
    )
    var localAddedCategories by remember { mutableStateOf(listOf<String>()) }
    val expenseCategories = remember(customCategories, localAddedCategories) {
        (defaultExpenseCategories + customCategories + localAddedCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val incomeCategories = remember(customCategories, localAddedCategories) {
        (listOf("Salário", "Extra", "Vendas", "Reembolso", "Outros") + customCategories + localAddedCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCustomCategoryText by remember { mutableStateOf("") }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val rate = currencyRates.rateFor(currency)
    val convertedBrl = parsedAmount * rate
    val resolvedEpochDay = remember(dateDigits, initialEpochDay) {
        OmniDateMaskUtils.digitsToEpochDayOrNull(dateDigits)
            ?: (initialTransaction?.dateEpochDay ?: initialEpochDay)
    }
    val parsedInstallments = (installmentCountText.toIntOrNull() ?: 1).coerceIn(1, 24)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) "+ Novo Lançamento" else "Editar Lançamento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3 Top Tabs/Chips: Despesa | Receita | Transferência
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            if (category !in expenseCategories) category = "Alimentação"
                            if (budgetBucket !in listOf("NEEDS", "WANTS", "SAVINGS")) budgetBucket = "NEEDS"
                        },
                        label = { Text("Despesa", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_tab_expense")
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            if (category !in incomeCategories) category = "Salário"
                            budgetBucket = "INCOME"
                            isCreditCard = false
                            generateCalendarTask = false
                        },
                        label = { Text("Receita", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_tab_income")
                    )
                    FilterChip(
                        selected = type == "TRANSFER",
                        onClick = {
                            type = "TRANSFER"
                            category = "Transferência"
                            budgetBucket = "TRANSFER"
                            isCreditCard = false
                            generateCalendarTask = false
                        },
                        label = { Text("Transferência", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_tab_transfer")
                    )
                }

                // Escopo Individual vs Familiar (for Expense and Income)
                if (type != "TRANSFER") {
                    Text(
                        text = "Escopo do Lançamento",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = privacyScope == PrivacyScope.FAMILY,
                            onClick = { privacyScope = PrivacyScope.FAMILY },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Familiar") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tx_scope_family_chip")
                        )
                        FilterChip(
                            selected = privacyScope == PrivacyScope.PERSONAL,
                            onClick = { privacyScope = PrivacyScope.PERSONAL },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Individual") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tx_scope_personal_chip")
                        )
                    }
                }

                // Descrição (optional for Transfer)
                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        if (type == "EXPENSE") {
                            val lower = newTitle.lowercase(Locale.ROOT)
                            when {
                                lower.contains("farm") || lower.contains("reméd") || lower.contains("remed") || lower.contains("vitamina") || lower.contains("insulina") -> {
                                    category = "Farmácia"
                                    budgetBucket = "NEEDS"
                                }
                                lower.contains("exame") || lower.contains("ultrassom") || lower.contains("sangue") || lower.contains("laborat") || lower.contains("curva") -> {
                                    category = "Exames"
                                    budgetBucket = "NEEDS"
                                }
                                lower.contains("consulta") || lower.contains("médic") || lower.contains("medic") || lower.contains("obstetra") || lower.contains("cardio") -> {
                                    category = "Consultas"
                                    budgetBucket = "NEEDS"
                                }
                                lower.contains("gesta") || lower.contains("bebê") || lower.contains("bebe") || lower.contains("enxoval") || lower.contains("pré-natal") || lower.contains("prenatal") -> {
                                    category = "Pré-Natal"
                                    budgetBucket = "NEEDS"
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            when (type) {
                                "INCOME" -> "Descrição da Receita (Ex: Salário, Venda)"
                                "TRANSFER" -> "Descrição (Opcional)"
                                else -> "Descrição da Despesa (Ex: Supermercado, Farmácia)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                // Valor & Data com Máscara Automática (DD/MM/AAAA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = {
                            Text(
                                if (type == "EXPENSE" && isCreditCard && parsedInstallments > 1 && initialTransaction == null) {
                                    "Valor Total ($currency)"
                                } else {
                                    "Valor ($currency)"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_amount_input")
                    )

                    MaskedDateTextField(
                        digitsValue = dateDigits,
                        onDigitsChange = { dateDigits = it },
                        label = "Data (DD/MM/AAAA)",
                        modifier = Modifier.weight(1.1f),
                        testTag = "tx_date_masked_input"
                    )
                }

                // Bank Account Selector
                if (type == "TRANSFER") {
                    Text(
                        text = "Conta de Origem (De onde sai o valor)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveAccounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.testTag("tx_origin_acc_${acc.id}")
                            )
                        }
                    }

                    Text(
                        text = "Conta de Destino (Para onde vai o valor)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveAccounts.forEach { acc ->
                            FilterChip(
                                selected = selectedDestinationAccountId == acc.id,
                                onClick = { selectedDestinationAccountId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.testTag("tx_dest_acc_${acc.id}")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NeedsBlue.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⇄ A transferência subtrai de '${selectedAccount.name}' e soma em '${selectedDestinationAccount.name}' sem contabilizar como gasto nem receita nos relatórios mensais.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeedsBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (type == "INCOME") {
                            "Conta Bancária de Destino (Onde entra)"
                        } else {
                            "Conta Bancária / Carteira Vinculada"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveAccounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.testTag("tx_account_chip_${acc.id}")
                            )
                        }
                    }
                }

                // INCOME Specific Options: Fixa/Recorrente vs Avulsa & Categorias de Receita
                if (type == "INCOME") {
                    Text(
                        text = "Tipo de Receita",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isRecurringIncome,
                            onClick = { isRecurringIncome = false },
                            label = { Text("Receita Avulsa") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isRecurringIncome,
                            onClick = { isRecurringIncome = true },
                            label = { Text("Fixa / Recorrente") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Categoria da Receita",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        incomeCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                // EXPENSE Specific Options: À Vista/Débito/Pix vs Crédito Parcelado
                if (type == "EXPENSE") {
                    Text(
                        text = "Forma de Pagamento",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isCreditCard,
                            onClick = { isCreditCard = false },
                            label = { Text("À Vista / Débito / Pix") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tx_payment_debit_chip")
                        )
                        FilterChip(
                            selected = isCreditCard,
                            onClick = { isCreditCard = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Crédito Parcelado") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tx_payment_credit_chip")
                        )
                    }

                    if (isCreditCard) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WantsAmber.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💳 Crédito Parcelado (Não desconta do saldo imediato)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WantsAmber
                                )
                                if (initialTransaction == null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = installmentCountText,
                                            onValueChange = { installmentCountText = it.filter { ch -> ch.isDigit() }.take(2) },
                                            label = { Text("Nº de Parcelas (1 a 24x)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("tx_installments_input")
                                        )
                                        if (parsedAmount > 0.0) {
                                            val perMonth = convertedBrl / parsedInstallments
                                            Text(
                                                text = "${parsedInstallments}x de\n${formatCurrencyBrl(perMonth)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = WantsAmber
                                            )
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(1, 2, 3, 4, 6, 10, 12).forEach { count ->
                                            FilterChip(
                                                selected = parsedInstallments == count,
                                                onClick = { installmentCountText = count.toString() },
                                                label = { Text("${count}x") }
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fatura / Parcela já foi paga?",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Switch(
                                            checked = isCreditPaid,
                                            onCheckedChange = { isCreditPaid = it }
                                        )
                                    }
                                }
                                val (_, _, purchaseDayOfMonth) = DateCalendarUtils.fromEpochDay(resolvedEpochDay)
                                val isAfterClosing = purchaseDayOfMonth > selectedAccount.closingDay
                                Text(
                                    text = "Limite disponível em ${selectedAccount.name}: ${formatCurrencyBrl(selectedAccount.availableCreditLimitBrl)} (Fecha dia ${selectedAccount.closingDay} • Vence dia ${selectedAccount.dueDay})" +
                                        if (isAfterClosing) " • Compra após o fechamento: entrará na fatura do próximo mês!" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAfterClosing) NeedsBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Faixa do Plano de Economia (60/20/20)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "NEEDS" to "60% Necessidades",
                            "WANTS" to "20% Desejos",
                            "SAVINGS" to "20% Economia"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = budgetBucket == code,
                                onClick = { budgetBucket = code },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categoria da Despesa (Sincronizada)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { showNewCategoryInput = !showNewCategoryInput },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showNewCategoryInput) "Fechar" else "+ Nova Categoria",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (showNewCategoryInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCustomCategoryText,
                                onValueChange = { newCustomCategoryText = it },
                                label = { Text("Nome da categoria da família") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clean = newCustomCategoryText.trim()
                                    if (clean.isNotBlank()) {
                                        localAddedCategories = localAddedCategories + clean
                                        category = clean
                                        onAddCustomCategory(clean)
                                        newCustomCategoryText = ""
                                        showNewCategoryInput = false
                                    }
                                }
                            ) {
                                Text("Criar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        expenseCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    // Agenda Integration Switch ("Criar na Agenda de Tarefas")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📅 Criar também na Agenda de Tarefas",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Agenda para ${DateCalendarUtils.formatEpochDayPt(resolvedEpochDay)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = generateCalendarTask,
                                onCheckedChange = { generateCalendarTask = it },
                                modifier = Modifier.testTag("tx_create_agenda_task_switch")
                            )
                        }
                    }
                }

                if (type != "TRANSFER") {
                    Text(
                        text = "Moeda (Conversão Automática para BRL)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        currencies.forEach { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code },
                                label = { Text(code) },
                                modifier = Modifier.testTag("currency_chip_$code")
                            )
                        }
                    }

                    if (currency != "BRL" && parsedAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Conversão Automática: ${formatCurrencyOriginal(parsedAmount, currency)} = ${formatCurrencyBrl(convertedBrl)} (Taxa: R$ ${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", rate)})",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canSave = parsedAmount > 0.0 && (type == "TRANSFER" || title.isNotBlank())
            Button(
                onClick = {
                    if (canSave) {
                        val finalTitle = if (type == "TRANSFER" && title.isBlank()) {
                            "Transferência: ${selectedAccount.name} → ${selectedDestinationAccount.name}"
                        } else {
                            title.trim()
                        }
                        onSaveTransaction(
                            finalTitle,
                            parsedAmount,
                            if (type == "TRANSFER") "BRL" else currency,
                            type,
                            when (type) {
                                "INCOME" -> "INCOME"
                                "TRANSFER" -> "TRANSFER"
                                else -> budgetBucket
                            },
                            when (type) {
                                "TRANSFER" -> "Transferência"
                                else -> category
                            },
                            resolvedEpochDay,
                            privacyScope,
                            type == "EXPENSE" && generateCalendarTask,
                            selectedAccount.id,
                            selectedAccount.name,
                            if (type == "TRANSFER") selectedDestinationAccount.id else "",
                            if (type == "TRANSFER") selectedDestinationAccount.name else "",
                            type == "INCOME" && isRecurringIncome,
                            type == "EXPENSE" && isCreditCard,
                            if (type == "EXPENSE" && isCreditCard) parsedInstallments else 1,
                            if (type == "EXPENSE" && isCreditCard) isCreditPaid else true
                        )
                        triggerFeedback(
                            if (initialTransaction == null) "Lançamento salvo com sucesso!"
                            else "Lançamento atualizado e saldos recalculados!"
                        )
                        onDismiss()
                    }
                },
                enabled = canSave,
                modifier = Modifier.testTag("save_tx_confirm_btn")
            ) {
                Text(if (initialTransaction == null) "Salvar Lançamento" else "Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BankAccountEditorDialog(
    initialAccount: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    var name by remember(initialAccount) { mutableStateOf(initialAccount?.name ?: "") }
    var institution by remember(initialAccount) { mutableStateOf(initialAccount?.institution ?: "Nubank") }
    var initialBalanceText by remember(initialAccount) {
        mutableStateOf(
            if (initialAccount != null) {
                String.format(Locale.US, "%.2f", initialAccount.initialBalanceBrl)
            } else "0.00"
        )
    }
    var creditLimitText by remember(initialAccount) {
        mutableStateOf(
            if (initialAccount != null) {
                String.format(Locale.US, "%.2f", initialAccount.creditLimitBrl)
            } else "5000.00"
        )
    }
    var closingDayText by remember(initialAccount) {
        mutableStateOf((initialAccount?.closingDay ?: 25).toString())
    }
    var dueDayText by remember(initialAccount) {
        mutableStateOf((initialAccount?.dueDay ?: 5).toString())
    }
    var emoji by remember(initialAccount) { mutableStateOf(initialAccount?.emoji ?: "🏦") }
    var colorHex by remember(initialAccount) { mutableStateOf(initialAccount?.colorHex ?: 0xFF820AD1L) }
    var privacyScope by remember(initialAccount) { mutableStateOf(initialAccount?.privacyScope ?: PrivacyScope.FAMILY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAccount == null) "Nova Conta & Cartão de Crédito" else "Editar Conta & Cartão (${initialAccount.name})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Bancos / Carteiras Rápidas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BankAccountCatalog.presetInstitutions.forEach { (instName, instEmoji, instColor) ->
                        FilterChip(
                            selected = institution == instName,
                            onClick = {
                                institution = instName
                                if (name.isBlank() || BankAccountCatalog.presetInstitutions.any { it.first == name }) {
                                    name = instName
                                }
                                emoji = instEmoji
                                colorHex = instColor
                            },
                            label = { Text("$instEmoji $instName", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Conta (Ex: Nubank Casal, Itaú, PicPay)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_account_name_input")
                )

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Saldo Inicial da Conta Corrente (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_account_initial_balance_input")
                )

                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("💳 Limite de Crédito do Cartão (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_account_credit_limit_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = closingDayText,
                        onValueChange = { closingDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Dia Fechamento (1-28)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bank_account_closing_day_input")
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Dia Vencimento (1-28)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bank_account_due_day_input")
                    )
                }

                Text(
                    text = "Escopo da Conta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = privacyScope == PrivacyScope.FAMILY,
                        onClick = { privacyScope = PrivacyScope.FAMILY },
                        label = { Text("Familiar") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = privacyScope == PrivacyScope.PERSONAL,
                        onClick = { privacyScope = PrivacyScope.PERSONAL },
                        label = { Text("Individual") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim().ifBlank { institution }
                    val initBal = initialBalanceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val credLim = (creditLimitText.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val closeDay = if (credLim > 0.0) (closingDayText.toIntOrNull() ?: 25).coerceIn(1, 28) else 0
                    val dueDay = if (credLim > 0.0) (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 28) else 0
                    onSave(
                        BankAccount(
                            id = initialAccount?.id ?: "acc_${UUID.randomUUID().toString().take(8)}",
                            name = cleanName,
                            institution = institution,
                            initialBalanceBrl = initBal,
                            currentBalanceBrl = initBal,
                            colorHex = colorHex,
                            emoji = emoji,
                            privacyScope = privacyScope,
                            creditLimitBrl = credLim,
                            closingDay = closeDay,
                            dueDay = dueDay
                        ).sanitized()
                    )
                },
                modifier = Modifier.testTag("save_bank_account_btn")
            ) {
                Text("Salvar Conta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullStatementDialog(
    selectedYearMonth: String,
    accounts: List<BankAccount>,
    transactions: List<FinanceTransaction>,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (FinanceTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    var accountFilter by remember { mutableStateOf<String?>(null) }
    val filteredTxs = remember(transactions, accountFilter) {
        if (accountFilter == null) {
            transactions
        } else {
            transactions.filter { it.accountId == accountFilter || it.destinationAccountId == accountFilter }
        }
    }

    val totalInBrl = remember(filteredTxs) {
        filteredTxs.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val totalOutBrl = remember(filteredTxs) {
        filteredTxs.filter { it.type == "EXPENSE" && (!it.isCreditCard || it.isPaid) }.sumOf { it.amountBrl }
    }
    val totalTransfersBrl = remember(filteredTxs) {
        filteredTxs.filter { it.type == "TRANSFER" }.sumOf { it.amountBrl }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "📄 Extrato Completo (${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entradas: +${formatCurrencyBrl(totalInBrl)} • Saídas: -${formatCurrencyBrl(totalOutBrl)} • Transf.: ${formatCurrencyBrl(totalTransfersBrl)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = accountFilter == null,
                        onClick = { accountFilter = null },
                        label = { Text("Todas (${accounts.size})") }
                    )
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = accountFilter == acc.id,
                            onClick = { accountFilter = if (accountFilter == acc.id) null else acc.id },
                            label = { Text("${acc.emoji} ${acc.name}") }
                        )
                    }
                }

                if (filteredTxs.isEmpty()) {
                    Text(
                        text = "Nenhuma movimentação encontrada para este filtro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    filteredTxs.forEach { tx ->
                        val isIncome = tx.type == "INCOME"
                        val isTransfer = tx.type == "TRANSFER"
                        val color = when {
                            isIncome -> IncomeEmerald
                            isTransfer -> NeedsBlue
                            tx.isCreditCard && !tx.isPaid -> WantsAmber
                            else -> ExpenseCoral
                        }
                        val prefix = when {
                            isIncome -> "+ "
                            isTransfer -> "⇄ "
                            else -> "- "
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = color.copy(alpha = 0.10f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val accDesc = if (isTransfer) {
                                        "${tx.accountName} → ${tx.destinationAccountName}"
                                    } else {
                                        tx.accountName
                                    }
                                    Text(
                                        text = "${DateCalendarUtils.formatEpochDayPt(tx.dateEpochDay)} • $accDesc • ${tx.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = prefix + formatCurrencyBrl(tx.amountBrl),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color
                                    )
                                    IconButton(
                                        onClick = { onEditTransaction(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteTransaction(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Excluir",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar Extrato")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSubscriptionDialog(
    initialSubscription: SubscriptionBill? = null,
    currencyRates: CurrencyRatesState,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amountOriginal: Double,
        currency: String,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String,
        syncDueDayToCalendar: Boolean
    ) -> Unit
) {
    var title by remember(initialSubscription) { mutableStateOf(initialSubscription?.title ?: "") }
    var amountText by remember(initialSubscription) {
        mutableStateOf(
            if (initialSubscription != null) String.format(Locale.US, "%.2f", initialSubscription.amountOriginal)
            else ""
        )
    }
    var currency by remember(initialSubscription) { mutableStateOf(initialSubscription?.currency ?: "BRL") }
    var category by remember(initialSubscription) { mutableStateOf(initialSubscription?.category ?: "Moradia") }
    var budgetBucket by remember(initialSubscription) { mutableStateOf(initialSubscription?.budgetBucket ?: "NEEDS") }
    var dueDayText by remember(initialSubscription) { mutableStateOf((initialSubscription?.dueDayOfMonth ?: 10).toString()) }
    var reminderDays by remember(initialSubscription) { mutableIntStateOf(initialSubscription?.reminderDaysBefore?.toInt() ?: 2) }
    var autoPostEnabled by remember(initialSubscription) { mutableStateOf(initialSubscription?.autoPostEnabled ?: true) }
    var privacyScope by remember(initialSubscription) { mutableStateOf(initialSubscription?.privacyScope ?: PrivacyScope.FAMILY) }
    var syncDueDayToCalendar by remember(initialSubscription) { mutableStateOf(initialSubscription?.syncDueDayToCalendar ?: true) }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val dueDay = (dueDayText.toIntOrNull() ?: 10).coerceIn(1, 31)
    val convertedBrl = parsedAmount * currencyRates.rateFor(currency)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSubscription == null) "Novo Custo Mensal ou Conta Fixa" else "Editar Custo Mensal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quem pode ver este custo mensal?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = privacyScope == PrivacyScope.FAMILY,
                        onClick = { privacyScope = PrivacyScope.FAMILY },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Família (Todos)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_scope_family_chip")
                    )
                    FilterChip(
                        selected = privacyScope == PrivacyScope.PERSONAL,
                        onClick = { privacyScope = PrivacyScope.PERSONAL },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Individual (Só Meu)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_scope_personal_chip")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Custo Mensal / Conta") },
                    placeholder = { Text("Ex: Aluguel / Energia / Academia / Cartão") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor ($currency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_amount_input")
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Pagar até dia (1-31)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_due_day_input")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📅 Criar na Agenda de Tarefas (Dia $dueDay)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gera automaticamente a tarefa no calendário",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = syncDueDayToCalendar,
                            onCheckedChange = { syncDueDayToCalendar = it },
                            modifier = Modifier.testTag("sub_sync_agenda_switch")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BRL", "USD", "EUR", "GBP").forEach { code ->
                        FilterChip(
                            selected = currency == code,
                            onClick = { currency = code },
                            label = { Text(code) }
                        )
                    }
                }

                if (currency != "BRL" && parsedAmount > 0) {
                    Text(
                        text = "Valor convertido: ${formatCurrencyBrl(convertedBrl)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Categoria e Faixa 60/20/20",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Moradia", "Energia", "Farmácia", "Exames", "Consultas", "Pré-Natal", "Saúde", "Internet", "Streaming", "Educação").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                budgetBucket = if (cat == "Streaming") "WANTS" else "NEEDS"
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                Text(
                    text = "Lembrete Recorrente Antes do Vencimento",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "No dia", 1 to "1 dia antes", 3 to "3 dias antes", 5 to "5 dias antes").forEach { (days, label) ->
                        FilterChip(
                            selected = reminderDays == days,
                            onClick = { reminderDays = days },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lançamento Automático Mensal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = autoPostEnabled,
                        onCheckedChange = { autoPostEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && parsedAmount > 0.0) {
                        onSave(
                            title,
                            parsedAmount,
                            currency,
                            category,
                            budgetBucket,
                            dueDay,
                            reminderDays,
                            autoPostEnabled,
                            privacyScope,
                            syncDueDayToCalendar
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && parsedAmount > 0.0,
                modifier = Modifier.testTag("save_sub_confirm_btn")
            ) {
                Text(if (initialSubscription == null) "Salvar Custo Mensal" else "Atualizar")
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
private fun EditCurrencyRatesDialog(
    currencyRates: CurrencyRatesState,
    onDismiss: () -> Unit,
    onSaveRate: (String, Double) -> Unit
) {
    var usdText by remember { mutableStateOf(currencyRates.usdToBrl.toString()) }
    var eurText by remember { mutableStateOf(currencyRates.eurToBrl.toString()) }
    var gbpText by remember { mutableStateOf(currencyRates.gbpToBrl.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajustar Cotações para Real (BRL)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = usdText,
                    onValueChange = { usdText = it },
                    label = { Text("1 Dólar (USD) em R$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = eurText,
                    onValueChange = { eurText = it },
                    label = { Text("1 Euro (EUR) em R$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gbpText,
                    onValueChange = { gbpText = it },
                    label = { Text("1 Libra (GBP) em R$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    usdText.replace(",", ".").toDoubleOrNull()?.let { onSaveRate("USD", it) }
                    eurText.replace(",", ".").toDoubleOrNull()?.let { onSaveRate("EUR", it) }
                    gbpText.replace(",", ".").toDoubleOrNull()?.let { onSaveRate("GBP", it) }
                    onDismiss()
                }
            ) {
                Text("Aplicar Taxas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
