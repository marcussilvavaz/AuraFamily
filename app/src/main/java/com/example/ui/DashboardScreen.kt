package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CurrencyRatesState
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.Household
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
    onAutoPostRecurringBills: () -> Unit,
    onOpenFinanceSubSection: (Int) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val monthTransactions = transactions.filter { it.yearMonth == selectedYearMonth }
    val loggedIncomeBrl = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
    val totalExpensesBrl = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountBrl }
    val netBalanceBrl = effectiveIncomeBrl - totalExpensesBrl

    val needsSpent = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
    val wantsSpent = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
    val savingsSpent = monthTransactions.filter { it.type == "EXPENSE" && it.budgetBucket == "SAVINGS" }.sumOf { it.amountBrl }

    val needsLimit = effectiveIncomeBrl * (household.needsPercentage / 100.0)
    val wantsLimit = effectiveIncomeBrl * (household.wantsPercentage / 100.0)
    val savingsTarget = effectiveIncomeBrl * (household.savingsPercentage / 100.0)

    val pendingSubscriptions = subscriptions.filter { it.active && it.lastPostedYearMonth != selectedYearMonth }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Month Selector & Family Financial Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Painel Financeiro • ${household.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Resumo exclusivo de receitas, gastos, câmbio e metas",
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
        }

        // 2. Hero Wealth Summary Banner (Organic Rounded Shape)
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
                            .height(220.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                                    )
                                )
                            )
                            .padding(22.dp)
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
                                Column {
                                    Text(
                                        text = "SALDO DISPONÍVEL DO MÊS (BRL)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatCurrencyBrl(netBalanceBrl),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netBalanceBrl >= 0) MaterialTheme.colorScheme.onSurface else ExpenseCoral
                                    )
                                }
                                AssistChip(
                                    onClick = { onOpenFinanceSubSection(2) },
                                    shape = RoundedCornerShape(50),
                                    label = {
                                        Text(
                                            text = "${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryMetricPill(
                                    modifier = Modifier.weight(1f),
                                    label = "Renda Base / Receitas",
                                    amount = formatCurrencyBrl(effectiveIncomeBrl),
                                    icon = Icons.Default.TrendingUp,
                                    tint = IncomeEmerald
                                )
                                SummaryMetricPill(
                                    modifier = Modifier.weight(1f),
                                    label = "Gastos no Mês",
                                    amount = formatCurrencyBrl(totalExpensesBrl),
                                    icon = Icons.Default.TrendingDown,
                                    tint = ExpenseCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Organic Financial Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenNewTransactionDialog,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("finance_overview_add_expense_btn"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar Gasto", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = { onOpenFinanceSubSection(2) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("finance_overview_reports_btn"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Relatórios & Metas", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // 4. Multi-Currency Live Strip (BRL, USD, EUR, GBP)
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

        // 5. 50/30/20 Family Savings Plan Overview Card
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
                                text = "Plano de Economia Familiar (${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Controle por porcentagem da renda mensal • ${savingsGoals.size} meta(s) ativa(s)",
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

        // 6. Pending Recurring Subscriptions & Fixed Bills Banner
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
                            onClick = onAutoPostRecurringBills,
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
                text = String.format(Locale("pt", "BR"), "R$ %.2f", rateBrl),
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
                text = "${formatCurrencyBrl(spentBrl)} / ${formatCurrencyBrl(limitBrl)}",
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
