package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinancesScreen(
    selectedYearMonth: String,
    transactions: List<FinanceTransaction>,
    subscriptions: List<SubscriptionBill>,
    currencyRates: CurrencyRatesState,
    onRefreshRates: () -> Unit,
    onUpdateManualRate: (String, Double) -> Unit,
    onOpenNewTransactionDialog: () -> Unit,
    onDeleteTransaction: (String) -> Unit,
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
    onPostSubscriptionNow: (SubscriptionBill) -> Unit,
    onAutoPostAllSubscriptions: () -> Unit,
    onDeleteSubscription: (String) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Gastos Diários, 1 = Custos Mensais & Assinaturas
    var scopeFilter by remember { mutableStateOf("ALL") } // ALL, FAMILY, PERSONAL
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showEditRatesDialog by remember { mutableStateOf(false) }
    var quickConverterAmountText by remember { mutableStateOf("100") }
    var quickConverterCurrency by remember { mutableStateOf("USD") }

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
                            text = "Gastos, Custos Mensais & Câmbio",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mês: ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)} • Integrado à Agenda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (activeSubTab == 0) onOpenNewTransactionDialog()
                            else showAddSubscriptionDialog = true
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("finances_primary_add_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (activeSubTab == 0) "Novo Gasto" else "Novo Custo Mensal")
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
                        text = { Text("Gastos & Contas (${monthTransactions.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_daily_expenses")
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("Custos Mensais (${filteredSubscriptions.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_subscriptions")
                    )
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
                            text = "Filtrar Contas: Compartilhadas com a Família vs. Individuais",
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
                                label = { Text("Família ($familyItemsCount)") },
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
                                label = { Text("Só Meu ($personalItemsCount)") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("finance_scope_personal")
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Multi-Currency Converter Card
        item {
            val amountVal = quickConverterAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
            val rate = currencyRates.rateFor(quickConverterCurrency)
            val convertedBrl = amountVal * rate

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Simulador & Câmbio Automático (BRL)",
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

                    Spacer(modifier = Modifier.height(10.dp))

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
                                text = String.format(Locale("pt", "BR"), "1 %s = R$ %.2f", quickConverterCurrency, rate),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
        }

        if (activeSubTab == 0) {
            // DAILY TRANSACTIONS & BILLS LIST
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
                                text = "Registre gastos compartilhados ou individuais e marque a opção para criar automaticamente na Agenda de Tarefas.",
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
                        onDelete = { onDeleteTransaction(tx.id) }
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
                                text = "Ao criar um custo mensal com dia de vencimento, ele aparece automaticamente na Agenda de Tarefas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onAutoPostAllSubscriptions,
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
                        onPostForMonth = { onPostSubscriptionNow(sub) },
                        onDelete = { onDeleteSubscription(sub.id) }
                    )
                }
            }
        }
    }

    if (showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            currencyRates = currencyRates,
            onDismiss = { showAddSubscriptionDialog = false },
            onSave = { title, amountOrig, currency, category, bucket, dueDay, reminderDays, autoPost, privacyScope, syncToAgenda ->
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
            }
        )
    }

    if (showEditRatesDialog) {
        EditCurrencyRatesDialog(
            currencyRates = currencyRates,
            onDismiss = { showEditRatesDialog = false },
            onSaveRate = onUpdateManualRate
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionItemCard(
    tx: FinanceTransaction,
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val isIncome = tx.type == "INCOME"
    val isPersonal = tx.privacyScope == PrivacyScope.PERSONAL
    val isLinkedToAgenda = tx.generateCalendarTask || tx.linkedTaskId.isNotBlank()
    val accentColor = if (isIncome) IncomeEmerald else ExpenseCoral

    val bucketLabel = when (tx.budgetBucket) {
        "NEEDS" -> "50% Necessidades"
        "WANTS" -> "30% Desejos"
        "SAVINGS" -> "20% Economia"
        else -> "Receita"
    }
    val bucketColor = when (tx.budgetBucket) {
        "NEEDS" -> NeedsBlue
        "WANTS" -> WantsAmber
        "SAVINGS" -> SavingsGold
        else -> IncomeEmerald
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
                    imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
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
                Text(
                    text = "${tx.category} • ${DateCalendarUtils.formatEpochDayPt(tx.dateEpochDay)} • Por ${tx.createdByName}",
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
                                text = if (isPersonal) "Individual (Só Meu)" else "Família",
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
                                text = "${formatCurrencyOriginal(tx.amountOriginal, tx.currency)} (R$ ${String.format(Locale("pt", "BR"), "%.2f", tx.exchangeRateToBrl)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + formatCurrencyBrl(tx.amountBrl),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_tx_btn_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir lançamento",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    onDelete: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
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
                        text = formatCurrencyBrl(subscription.amountBrl),
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
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_sub_btn_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover assinatura",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialEpochDay: Long,
    currencyRates: CurrencyRatesState,
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
        generateCalendarTask: Boolean
    ) -> Unit
) {
    val (initYear, initMonth, initDay) = remember(initialEpochDay) {
        DateCalendarUtils.fromEpochDay(initialEpochDay)
    }
    val maxDaysInMonth = remember(initYear, initMonth) {
        DateCalendarUtils.daysInMonth(initYear, initMonth)
    }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BRL") }
    var type by remember { mutableStateOf("EXPENSE") }
    var budgetBucket by remember { mutableStateOf("NEEDS") }
    var category by remember { mutableStateOf("Alimentação") }
    var privacyScope by remember { mutableStateOf(PrivacyScope.FAMILY) }
    var dueDayText by remember { mutableStateOf(initDay.toString()) }
    var generateCalendarTask by remember { mutableStateOf(true) }

    val currencies = listOf("BRL", "USD", "EUR", "GBP")
    val expenseCategories = listOf("Alimentação", "Moradia", "Transporte", "Lazer", "Saúde", "Educação", "Tecnologia")
    val incomeCategories = listOf("Salário", "Freelance", "Rendimentos", "Bônus")

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val rate = currencyRates.rateFor(currency)
    val convertedBrl = parsedAmount * rate
    val chosenDay = (dueDayText.toIntOrNull() ?: initDay).coerceIn(1, maxDaysInMonth)
    val resolvedEpochDay = DateCalendarUtils.toEpochDay(initYear, initMonth, chosenDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Novo Gasto ou Conta a Pagar",
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
                // 1. Intuitive Scope Selector: Compartilhada vs Individual
                Text(
                    text = "Visibilidade da Conta",
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
                        label = { Text("Individual (Só Meu)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_scope_personal_chip")
                    )
                }

                Text(
                    text = if (privacyScope == PrivacyScope.PERSONAL) {
                        "🔒 Conta individual: não aparece para os outros membros da família."
                    } else {
                        "👨‍👩‍👧‍👦 Conta compartilhada: entra no orçamento visível da família."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            category = "Alimentação"
                        },
                        label = { Text("Gasto / Conta") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            category = "Salário"
                            generateCalendarTask = false
                        },
                        label = { Text("Receita / Renda") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex: Supermercado / Conta de Luz / Cartão") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor ($currency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_amount_input")
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Dia (1-$maxDaysInMonth)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("tx_due_day_input")
                    )
                }

                // 2. Agenda Integration Switch ("Criar na Agenda de Tarefas")
                if (type == "EXPENSE") {
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
                                    text = "Agenda para ${DateCalendarUtils.formatEpochDayPt(resolvedEpochDay)} com a mesma visibilidade (${if (privacyScope == PrivacyScope.PERSONAL) "Individual" else "Família"})",
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
                            text = "Conversão Automática: ${formatCurrencyOriginal(parsedAmount, currency)} = ${formatCurrencyBrl(convertedBrl)} (Taxa: R$ ${String.format(Locale("pt", "BR"), "%.2f", rate)})",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (type == "EXPENSE") {
                    Text(
                        text = "Faixa do Plano de Economia (50/30/20)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "NEEDS" to "50% Necessidades",
                            "WANTS" to "30% Desejos",
                            "SAVINGS" to "20% Economia"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = budgetBucket == code,
                                onClick = { budgetBucket = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cats = if (type == "EXPENSE") expenseCategories else incomeCategories
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && parsedAmount > 0.0) {
                        onSaveTransaction(
                            title,
                            parsedAmount,
                            currency,
                            type,
                            budgetBucket,
                            category,
                            resolvedEpochDay,
                            privacyScope,
                            type == "EXPENSE" && generateCalendarTask
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && parsedAmount > 0.0,
                modifier = Modifier.testTag("save_tx_confirm_btn")
            ) {
                Text("Registrar")
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
private fun AddSubscriptionDialog(
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
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BRL") }
    var category by remember { mutableStateOf("Moradia") }
    var budgetBucket by remember { mutableStateOf("NEEDS") }
    var dueDayText by remember { mutableStateOf("10") }
    var reminderDays by remember { mutableIntStateOf(2) }
    var autoPostEnabled by remember { mutableStateOf(true) }
    var privacyScope by remember { mutableStateOf(PrivacyScope.FAMILY) }
    var syncDueDayToCalendar by remember { mutableStateOf(true) }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val dueDay = (dueDayText.toIntOrNull() ?: 10).coerceIn(1, 31)
    val convertedBrl = parsedAmount * currencyRates.rateFor(currency)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Novo Custo Mensal ou Conta Fixa",
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
                // 1. Intuitive Scope Selector: Compartilhado vs Individual
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

                Text(
                    text = if (privacyScope == PrivacyScope.PERSONAL) {
                        "🔒 Conta individual: fica oculta da família nas Finanças e na Agenda."
                    } else {
                        "👨‍👩‍👧‍👦 Custo familiar: compartilhado com todos da família."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

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

                // 2. Automatic Agenda Creation Toggle (ON by default)
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
                                text = "Gera automaticamente a tarefa no calendário para você dar Check ✓ quando pagar",
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
                    text = "Categoria e Faixa 50/30/20",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Moradia", "Energia", "Internet", "Streaming", "Educação", "Saúde").forEach { cat ->
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
                Text("Salvar Custo Mensal")
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
