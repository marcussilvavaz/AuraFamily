package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BankAccount
import com.example.data.BankAccountCatalog
import com.example.data.CurrencyRatesState
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.Household
import com.example.data.PrivacyScope
import com.example.data.SavingsGoal
import com.example.data.SubscriptionBill
import com.example.data.TaskItem
import com.example.data.formatCurrencyBrl
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOrganicCardRadius
import com.example.ui.theme.NeedsBlue
import com.example.ui.theme.SavingsGold
import com.example.ui.theme.WantsAmber
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    selectedYearMonth: String,
    household: Household,
    transactions: List<FinanceTransaction>,
    subscriptions: List<SubscriptionBill>,
    savingsGoals: List<SavingsGoal>,
    currencyRates: CurrencyRatesState,
    onShiftMonth: (Int) -> Unit,
    onRefreshRates: () -> Unit,
    onOpenNewTransactionDialog: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit = {},
    onSetCreditTransactionPaid: (FinanceTransaction, Boolean, String, String) -> Unit = { _, _, _, _ -> },
    onPayAllCreditForMonth: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveBankAccount: (BankAccount) -> Unit = {},
    onDeleteBankAccount: (String, String) -> Unit = { _, _ -> },
    onAutoPostRecurringBills: () -> Unit,
    onOpenFinanceSubSection: (Int) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val triggerFeedback = rememberOmniFeedback()

    // Escopo Individual vs Familiar vs Tudo
    var scopeFilter by remember { mutableStateOf("ALL") } // "ALL", PrivacyScope.FAMILY, PrivacyScope.PERSONAL

    // Dialog states for Bank Account Editor, Delete Confirmations, and Quick Statement Modal
    var editingBankAccount by remember { mutableStateOf<BankAccount?>(null) }
    var showBankAccountDialog by remember { mutableStateOf(false) }
    var deletingBankAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deletingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    var showFullStatementModal by remember { mutableStateOf(false) }

    // Live recalculated balances across all registered bank accounts
    val allCalculatedAccounts = remember(household.bankAccounts, transactions) {
        BankAccountCatalog.calculateCurrentBalances(household.bankAccounts, transactions)
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
    val monthTransactions = remember(allMonthTransactions, scopeFilter) {
        when (scopeFilter) {
            PrivacyScope.FAMILY -> allMonthTransactions.filter { it.privacyScope != PrivacyScope.PERSONAL }
            PrivacyScope.PERSONAL -> allMonthTransactions.filter { it.privacyScope == PrivacyScope.PERSONAL }
            else -> allMonthTransactions
        }
    }

    // Logged incomes in the month + base monthly income (if viewing ALL or FAMILY)
    val loggedIncomeBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val baseIncomeBrl = if (scopeFilter == PrivacyScope.PERSONAL) 0.0 else household.monthlyIncomeBrl
    val effectiveIncomeBrl = remember(baseIncomeBrl, loggedIncomeBrl) {
        if (baseIncomeBrl > 0.0 && loggedIncomeBrl > 0.0) {
            baseIncomeBrl + loggedIncomeBrl
        } else {
            maxOf(baseIncomeBrl, loggedIncomeBrl)
        }
    }

    // Cash/Debit/Pix expenses + paid credit installments
    val immediateAndPaidExpensesBrl = remember(monthTransactions) {
        monthTransactions.filter {
            it.type == "EXPENSE" && (!it.isCreditCard || it.isPaid)
        }.sumOf { it.amountBrl }
    }

    // Credit card installments for the month (Fatura / Crédito do Mês)
    val monthCreditInstallments = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.isCreditCard }
    }
    val openCreditInvoiceBrl = remember(monthCreditInstallments) {
        monthCreditInstallments.filter { !it.isPaid }.sumOf { it.amountBrl }
    }
    val paidCreditInvoiceBrl = remember(monthCreditInstallments) {
        monthCreditInstallments.filter { it.isPaid }.sumOf { it.amountBrl }
    }
    val totalMonthExpensesBrl = immediateAndPaidExpensesBrl + openCreditInvoiceBrl
    val netMonthResultBrl = effectiveIncomeBrl - immediateAndPaidExpensesBrl

    val needsSpent = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
    }
    val wantsSpent = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
    }
    val savingsSpent = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "SAVINGS" }.sumOf { it.amountBrl }
    }

    val safeBudgetBase = effectiveIncomeBrl.coerceAtLeast(1.0)
    val needsLimit = safeBudgetBase * (household.needsPercentage / 100.0)
    val wantsLimit = safeBudgetBase * (household.wantsPercentage / 100.0)
    val savingsTarget = safeBudgetBase * (household.savingsPercentage / 100.0)

    val pendingSubscriptions = remember(subscriptions, selectedYearMonth, scopeFilter) {
        subscriptions.filter {
            it.active && it.lastPostedYearMonth != selectedYearMonth &&
                (scopeFilter == "ALL" || it.privacyScope == scopeFilter)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month Selector & Financial Scope Toggle (Tudo / Familiar / Individual)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Finanças • ${household.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Contas bancárias, extrato fácil, crédito parcelado e metas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { onShiftMonth(-1) },
                                modifier = Modifier.testTag("finance_prev_month_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Mês anterior"
                                )
                            }
                            Text(
                                text = DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { onShiftMonth(1) },
                                modifier = Modifier.testTag("finance_next_month_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Próximo mês"
                                )
                            }
                        }
                    }
                }

                // Escopo Individual vs Familiar filter bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scopeFilter == "ALL",
                        onClick = { scopeFilter = "ALL" },
                        label = { Text("Tudo (Família + Meu)", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("dashboard_scope_all")
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
                        label = { Text("Familiar") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("dashboard_scope_family")
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
                        label = { Text("Individual") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("dashboard_scope_personal")
                    )
                }
            }
        }

        // 2. Hero Wealth & Total Bank Accounts Balance Banner + Quick Extrato Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_wealth_1795185246551),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(242.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(242.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (scopeFilter) {
                                            PrivacyScope.PERSONAL -> "SALDO TOTAL INDIVIDUAL (CONTAS)"
                                            PrivacyScope.FAMILY -> "SALDO TOTAL FAMILIAR (CONTAS)"
                                            else -> "SALDO TOTAL EM CONTAS (BRL)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatCurrencyBrl(totalAccountsBalanceBrl, hideValues),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalAccountsBalanceBrl >= 0) MaterialTheme.colorScheme.onSurface else ExpenseCoral
                                    )
                                    Text(
                                        text = "Balanço do mês (Receitas - Débito/Pix): ${formatCurrencyBrl(netMonthResultBrl, hideValues)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { showFullStatementModal = true },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dashboard_open_extrato_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ver Extrato", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryMetricPill(
                                    modifier = Modifier.weight(1f),
                                    label = "Renda Base / Receitas",
                                    amount = formatCurrencyBrl(effectiveIncomeBrl, hideValues),
                                    icon = Icons.Default.TrendingUp,
                                    tint = IncomeEmerald
                                )
                                SummaryMetricPill(
                                    modifier = Modifier.weight(1f),
                                    label = "Saídas à Vista / Pagas",
                                    amount = formatCurrencyBrl(immediateAndPaidExpensesBrl, hideValues),
                                    icon = Icons.Default.TrendingDown,
                                    tint = ExpenseCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Primary Actions: "+ Novo Lançamento" (Despesa, Receita, Transferência) & "Ver Extrato Completo"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenNewTransactionDialog,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(50.dp)
                        .testTag("finance_overview_add_expense_btn"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Novo Lançamento", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = { onOpenFinanceSubSection(1) },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                        .testTag("finance_overview_statement_btn"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extrato & Contas", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Contas Bancárias / Carteiras (Nubank, Itaú, PicPay, Dinheiro, etc.)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_bank_accounts_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Contas Bancárias & Carteiras",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Saldo atualizado automaticamente por conta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                editingBankAccount = null
                                showBankAccountDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("dashboard_add_bank_account_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Conta", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    visibleAccounts.forEach { acc ->
                        val accColor = if (acc.colorHex != 0L) Color(acc.colorHex.toInt()) else MaterialTheme.colorScheme.primary
                        val accOpenInvoiceMonthBrl = remember(acc.id, selectedYearMonth, transactions) {
                            BankAccountCatalog.openInvoiceForMonthBrl(acc.id, selectedYearMonth, transactions)
                        }
                        val creditUsageRatio = if (acc.creditLimitBrl > 0.0) {
                            (acc.usedCreditLimitBrl / acc.creditLimitBrl).toFloat().coerceIn(0f, 1f)
                        } else 0f

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = accColor.copy(alpha = 0.10f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, accColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .testTag("bank_account_row_${acc.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(accColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = acc.emoji.ifBlank { "🏦" }, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${acc.institution} • ${if (acc.privacyScope == PrivacyScope.PERSONAL) "Individual" else "Familiar"} • Fecha dia ${acc.closingDay} • Vence dia ${acc.dueDay}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatCurrencyBrl(acc.currentBalanceBrl, hideValues),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (acc.currentBalanceBrl >= 0.0) IncomeEmerald else ExpenseCoral
                                        )
                                        IconButton(
                                            onClick = {
                                                editingBankAccount = acc
                                                showBankAccountDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("edit_bank_account_${acc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar conta",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (allCalculatedAccounts.size > 1) {
                                            IconButton(
                                                onClick = { deletingBankAccount = acc },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("delete_bank_account_${acc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Excluir conta",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Barra de progresso de Limite de Crédito (Usado x Disponível) + Botão 1-toque "Pagar Fatura do Mês"
                                if (acc.creditLimitBrl > 0.0) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "💳 Limite Usado: ${formatCurrencyBrl(acc.usedCreditLimitBrl, hideValues)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = if (creditUsageRatio > 0.85f) ExpenseCoral else WantsAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Disponível: ${formatCurrencyBrl(acc.availableCreditLimitBrl, hideValues)} / ${formatCurrencyBrl(acc.creditLimitBrl, hideValues)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = IncomeEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { creditUsageRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = if (creditUsageRatio > 0.85f) ExpenseCoral else WantsAmber,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        if (accOpenInvoiceMonthBrl > 0.009) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Fatura do mês: ${formatCurrencyBrl(accOpenInvoiceMonthBrl, hideValues)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WantsAmber
                                                )
                                                FilledTonalButton(
                                                    onClick = {
                                                        onPayAllCreditForMonth(selectedYearMonth, acc.id, acc.name)
                                                        triggerFeedback("Fatura de ${acc.name} paga e limite liberado!")
                                                    },
                                                    shape = RoundedCornerShape(50),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("pay_account_invoice_btn_${acc.id}")
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Fatura / Crédito do Mês Card (Crédito Parcelado separado do Saldo Imediato)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_credit_invoice_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = WantsAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "💳 Fatura / Crédito do Mês (${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Parcelas no crédito não descontam do saldo até serem marcadas como Pagas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WantsAmber.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Fatura em Aberto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrencyBrl(openCreditInvoiceBrl, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WantsAmber
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = IncomeEmerald.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Fatura Paga no Mês",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrencyBrl(paidCreditInvoiceBrl, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IncomeEmerald
                                )
                            }
                        }
                    }

                    if (monthCreditInstallments.isEmpty()) {
                        Text(
                            text = "Nenhuma parcela de cartão de crédito projetada para este mês.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val defaultPayAccount = visibleAccounts.firstOrNull() ?: allCalculatedAccounts.first()
                        if (openCreditInvoiceBrl > 0.0) {
                            Button(
                                onClick = {
                                    onPayAllCreditForMonth(selectedYearMonth, defaultPayAccount.id, defaultPayAccount.name)
                                    triggerFeedback("Fatura do mês paga pela conta ${defaultPayAccount.name}!")
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pay_all_credit_month_btn")
                            ) {
                                Text(
                                    text = "Pagar Fatura do Mês (${formatCurrencyBrl(openCreditInvoiceBrl, hideValues)}) via ${defaultPayAccount.name}",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        monthCreditInstallments.forEach { creditTx ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
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
                                            text = creditTx.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Conta: ${creditTx.accountName} • ${formatCurrencyBrl(creditTx.amountBrl, hideValues)} • ${if (creditTx.isPaid) "Paga ✓" else "Em aberto na fatura"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = if (creditTx.isPaid) IncomeEmerald else WantsAmber
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = {
                                                val newPaid = !creditTx.isPaid
                                                onSetCreditTransactionPaid(
                                                    creditTx,
                                                    newPaid,
                                                    creditTx.accountId,
                                                    creditTx.accountName
                                                )
                                                triggerFeedback(
                                                    if (newPaid) "Parcela marcada como Paga e descontada de ${creditTx.accountName}!"
                                                    else "Parcela voltou para Em Aberto na fatura"
                                                )
                                            },
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("toggle_credit_paid_${creditTx.id}")
                                        ) {
                                            Text(
                                                text = if (creditTx.isPaid) "Estornar" else "Marcar Paga",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(
                                            onClick = { onEditTransaction(creditTx) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Statement Preview (Últimos Lançamentos do Extrato)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_quick_statement_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📄 Extrato Rápido (${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Entradas em verde, saídas em coral e transferências em azul",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showFullStatementModal = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_full_extrato_modal_btn")
                        ) {
                            Text("Extrato Completo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (monthTransactions.isEmpty()) {
                        Text(
                            text = "Nenhum lançamento neste mês. Toque em '+ Novo Lançamento' acima.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        monthTransactions.take(5).forEach { tx ->
                            val isIncome = tx.type == "INCOME"
                            val isTransfer = tx.type == "TRANSFER"
                            val tint = when {
                                isIncome -> IncomeEmerald
                                isTransfer -> NeedsBlue
                                else -> ExpenseCoral
                            }
                            val prefix = when {
                                isIncome -> "+ "
                                isTransfer -> "⇄ "
                                else -> "- "
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = tint.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
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
                                        val accountSubtitle = if (isTransfer) {
                                            "${tx.accountName} → ${tx.destinationAccountName}"
                                        } else {
                                            tx.accountName
                                        }
                                        val creditTag = if (tx.isCreditCard) {
                                            if (tx.isPaid) " • Crédito Pago" else " • Crédito (Fatura)"
                                        } else ""
                                        Text(
                                            text = "${DateCalendarUtils.formatEpochDayPt(tx.dateEpochDay)} • $accountSubtitle$creditTag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = prefix + formatCurrencyBrl(tx.amountBrl, hideValues),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = tint
                                        )
                                        IconButton(
                                            onClick = { onEditTransaction(tx) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { deletingTransaction = tx },
                                            modifier = Modifier.size(30.dp)
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
            }
        }

        // 7. 50/30/20 Family Savings Plan Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plano de Economia (${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total de gastos considerados no mês: ${formatCurrencyBrl(totalMonthExpensesBrl)} • ${savingsGoals.size} meta(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onOpenFinanceSubSection(2) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ver relatórios detalhados"
                            )
                        }
                    }

                    BucketProgressBarRow(
                        label = "Necessidades (${household.needsPercentage}%)",
                        spentBrl = needsSpent,
                        limitBrl = needsLimit,
                        barColor = NeedsBlue
                    )
                    BucketProgressBarRow(
                        label = "Desejos & Lazer (${household.wantsPercentage}%)",
                        spentBrl = wantsSpent,
                        limitBrl = wantsLimit,
                        barColor = WantsAmber
                    )
                    BucketProgressBarRow(
                        label = "Economia & Metas (${household.savingsPercentage}%)",
                        spentBrl = savingsSpent,
                        limitBrl = savingsTarget,
                        barColor = SavingsGold
                    )
                }
            }
        }

        // 8. Multi-Currency Live Strip (BRL, USD, EUR, GBP)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Conversão Automática Multi-Moeda",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currencyRates.lastUpdatedLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onRefreshRates,
                            modifier = Modifier.testTag("refresh_rates_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar cotações"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyRateBadge(
                            modifier = Modifier.weight(1f),
                            code = "USD ($)",
                            rateBrl = currencyRates.usdToBrl
                        )
                        CurrencyRateBadge(
                            modifier = Modifier.weight(1f),
                            code = "EUR (€)",
                            rateBrl = currencyRates.eurToBrl
                        )
                        CurrencyRateBadge(
                            modifier = Modifier.weight(1f),
                            code = "GBP (£)",
                            rateBrl = currencyRates.gbpToBrl
                        )
                    }
                }
            }
        }

        // 9. Pending Recurring Subscriptions & Fixed Bills Banner
        if (pendingSubscriptions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${pendingSubscriptions.size} Conta(s) Fixa(s) Pendente(s)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total estimado: ${formatCurrencyBrl(pendingSubscriptions.sumOf { it.amountBrl })}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onAutoPostRecurringBills()
                                triggerFeedback("Contas fixas lançadas no mês!")
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("auto_post_bills_btn")
                        ) {
                            Text("Lançar Mês")
                        }
                    }
                }
            }
        }
    }

    // Bank Account Create / Edit Dialog
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

    // Bank Account Delete Confirmation
    val accToDelete = deletingBankAccount
    if (accToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Conta Bancária",
            message = "Deseja excluir a conta '${accToDelete.name}'? Os lançamentos serão mantidos no extrato.",
            onConfirm = {
                onDeleteBankAccount(accToDelete.id, accToDelete.name)
                triggerFeedback("Conta '${accToDelete.name}' removida")
                deletingBankAccount = null
            },
            onDismiss = { deletingBankAccount = null }
        )
    }

    // Transaction Delete Confirmation
    val txToDelete = deletingTransaction
    if (txToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Lançamento",
            message = "Deseja excluir '${txToDelete.title}' (${formatCurrencyBrl(txToDelete.amountBrl)})? O saldo da conta vinculada será recalculado automaticamente.",
            onConfirm = {
                onDeleteTransaction(txToDelete.id)
                triggerFeedback("Lançamento excluído e saldo atualizado!")
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null }
        )
    }

    // Full Extrato Modal
    if (showFullStatementModal) {
        FullStatementDialog(
            selectedYearMonth = selectedYearMonth,
            accounts = visibleAccounts,
            transactions = monthTransactions,
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

@Composable
private fun SummaryMetricPill(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrencyRateBadge(
    modifier: Modifier = Modifier,
    code: String,
    rateBrl: Double
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", rateBrl),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BucketProgressBarRow(
    label: String,
    spentBrl: Double,
    limitBrl: Double,
    barColor: androidx.compose.ui.graphics.Color
) {
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val ratio = if (limitBrl > 0) (spentBrl / limitBrl).toFloat() else 0f
    val clampedProgress = ratio.coerceIn(0f, 1f)
    val isOverBudget = spentBrl > limitBrl && limitBrl > 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatCurrencyBrl(spentBrl, hideValues)} / ${formatCurrencyBrl(limitBrl, hideValues)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (isOverBudget) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { clampedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = if (isOverBudget) ExpenseCoral else barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun QuickTaskCheckCard(
    task: TaskItem,
    onToggleCheck: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val containerColor by animateColorAsState(
        targetValue = if (task.completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "taskCardBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCheck() }
            .testTag("dashboard_task_item_${task.id}"),
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleCheck,
                modifier = Modifier.testTag("task_check_btn_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.completed) "Tarefa concluída" else "Marcar tarefa como concluída",
                    tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${DateCalendarUtils.formatEpochDayPt(task.dateEpochDay)} • ${task.category}" +
                        if (task.alarmEnabled) " • ⏰ ${DateCalendarUtils.formatTime(task.alarmHour.toInt(), task.alarmMinute.toInt())}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
