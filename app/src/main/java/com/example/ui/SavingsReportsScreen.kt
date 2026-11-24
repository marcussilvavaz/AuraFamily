package com.example.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.Household
import com.example.data.SavingsGoal
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
fun SavingsReportsScreen(
    selectedYearMonth: String,
    household: Household,
    transactions: List<FinanceTransaction>,
    savingsGoals: List<SavingsGoal>,
    onSaveBudgetPlan: (
        name: String,
        monthlyIncomeBrl: Double,
        needsPct: Int,
        wantsPct: Int,
        savingsPct: Int
    ) -> Unit,
    onAddSavingsGoal: (
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ) -> Unit,
    onUpdateSavingsGoal: (
        existing: SavingsGoal,
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onContributeGoal: (SavingsGoal, Double, Boolean) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onUpdateCategoryBudget: (category: String, limitBrl: Double) -> Unit = { _, _ -> },
    onAddCustomCategory: (String) -> Unit = {}
) {
    val cardRadius = LocalOrganicCardRadius.current
    val hideValues = com.example.ui.theme.LocalFinancePrivacyHideValues.current
    val triggerFeedback = rememberOmniFeedback()
    var showEditPlanDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalForContribution by remember { mutableStateOf<SavingsGoal?>(null) }
    var editingCategoryBudget by remember { mutableStateOf<String?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val prevYearMonth = remember(selectedYearMonth) {
        DateCalendarUtils.shiftYearMonth(selectedYearMonth, -1)
    }

    val monthTransactions = remember(transactions, selectedYearMonth) {
        transactions.filter { it.yearMonth == selectedYearMonth }
    }
    val prevMonthExpenses = remember(transactions, prevYearMonth) {
        transactions.filter { it.yearMonth == prevYearMonth && it.type == "EXPENSE" }
    }
    val prevCategoryTotals = remember(prevMonthExpenses) {
        prevMonthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountBrl } }
    }
    val monthExpenses = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }
    }
    val loggedIncomeBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
    val totalExpensesBrl = monthExpenses.sumOf { it.amountBrl }
    val prevTotalExpensesBrl = prevMonthExpenses.sumOf { it.amountBrl }

    val needsSpent = monthExpenses.filter { it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
    val wantsSpent = monthExpenses.filter { it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
    val savingsSpent = monthExpenses.filter { it.budgetBucket == "SAVINGS" }.sumOf { it.amountBrl }

    val categoryBreakdown = remember(monthExpenses) {
        monthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountBrl } }
            .entries
            .sortedByDescending { it.value }
    }

    val allAvailableCategories = remember(household.customCategories, household.categoryBudgetsBrl, categoryBreakdown) {
        val defaultCats = listOf(
            "Moradia", "Alimentação", "Supermercado", "Transporte",
            "Saúde", "Educação", "Lazer", "Assinaturas", "Cartão", "Outros"
        )
        (defaultCats + household.customCategories + household.categoryBudgetsBrl.keys + categoryBreakdown.map { it.key })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("savings_reports_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Metas & Teto de Gastos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Análise de ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)} vs ${DateCalendarUtils.formatYearMonthDisplay(prevYearMonth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingGoal = null
                        showAddGoalDialog = true
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("new_savings_goal_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nova Meta", maxLines = 1, softWrap = false)
                }
            }
        }

        // 2. 50/30/20 Percentage Savings Plan Configuration Card
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
                                text = "Regra de Orçamento (${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Renda Familiar Base: ${formatCurrencyBrl(effectiveIncomeBrl, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FilledTonalButton(
                            onClick = { showEditPlanDialog = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("edit_50_30_20_plan_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Personalizar %", maxLines = 1, softWrap = false)
                        }
                    }

                    BucketProgressBarRow(
                        label = "Necessidades Essenciais (${household.needsPercentage}%)",
                        spentBrl = needsSpent,
                        limitBrl = effectiveIncomeBrl * (household.needsPercentage / 100.0),
                        barColor = NeedsBlue
                    )
                    BucketProgressBarRow(
                        label = "Estilo de Vida & Desejos (${household.wantsPercentage}%)",
                        spentBrl = wantsSpent,
                        limitBrl = effectiveIncomeBrl * (household.wantsPercentage / 100.0),
                        barColor = WantsAmber
                    )
                    BucketProgressBarRow(
                        label = "Poupança & Metas da Família (${household.savingsPercentage}%)",
                        spentBrl = savingsSpent,
                        limitBrl = effectiveIncomeBrl * (household.savingsPercentage / 100.0),
                        barColor = SavingsGold
                    )
                }
            }
        }

        // 2.5. Category Spending Caps (Teto de Gastos) & Monthly Comparison Ranking
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_budgets_comparison_card"),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎯 Teto de Gastos por Categoria & Comparativo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val monthDiffText = if (prevTotalExpensesBrl > 0.0) {
                                val diffPct = ((totalExpensesBrl - prevTotalExpensesBrl) / prevTotalExpensesBrl) * 100.0
                                val sign = if (diffPct >= 0) "+" else ""
                                "Total do mês: $sign${String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", diffPct)} vs ${DateCalendarUtils.formatYearMonthDisplay(prevYearMonth)}"
                            } else {
                                "Defina limites mensais por categoria com alerta visual"
                            }
                            Text(
                                text = monthDiffText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showNewCategoryDialog = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_custom_category_reports_btn")
                        ) {
                            Text("+ Categoria", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }

                    // Filter chips to quickly set or edit a Teto de Gastos on any category
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAvailableCategories.forEach { cat ->
                            val hasCap = (household.categoryBudgetsBrl[cat] ?: 0.0) > 0.0
                            FilterChip(
                                selected = hasCap,
                                onClick = { editingCategoryBudget = cat },
                                label = {
                                    val capVal = household.categoryBudgetsBrl[cat] ?: 0.0
                                    Text(
                                        text = if (capVal > 0.0) "$cat: ${formatCurrencyBrl(capVal, hideValues)}" else "+ Teto: $cat",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    val rankedCategories = remember(categoryBreakdown, household.categoryBudgetsBrl) {
                        val map = linkedMapOf<String, Double>()
                        categoryBreakdown.forEach { map[it.key] = it.value }
                        household.categoryBudgetsBrl.forEach { (cat, limit) ->
                            if (limit > 0.0 && !map.containsKey(cat)) {
                                map[cat] = 0.0
                            }
                        }
                        map.entries.toList()
                    }

                    if (rankedCategories.isEmpty()) {
                        Text(
                            text = "Toque em qualquer categoria acima para definir um Teto de Gastos mensal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        rankedCategories.forEachIndexed { index, (cat, spentBrl) ->
                            val prevSpent = prevCategoryTotals[cat] ?: 0.0
                            val capBrl = household.categoryBudgetsBrl[cat] ?: 0.0
                            val diffBadgeText = when {
                                prevSpent > 0.0 -> {
                                    val diffPct = ((spentBrl - prevSpent) / prevSpent) * 100.0
                                    val sign = if (diffPct >= 0) "+" else ""
                                    "$sign${String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", diffPct)} vs mês ant."
                                }
                                spentBrl > 0.0 -> "Novo neste mês"
                                else -> "Sem gastos"
                            }
                            val diffBadgeColor = when {
                                prevSpent > 0.0 && spentBrl > prevSpent * 1.05 -> ExpenseCoral
                                prevSpent > 0.0 && spentBrl < prevSpent * 0.95 -> IncomeEmerald
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "#${index + 1} $cat",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = diffBadgeColor.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = diffBadgeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = diffBadgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        TextButton(
                                            onClick = { editingCategoryBudget = cat },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = if (capBrl > 0.0) "Editar Teto" else "Definir Teto",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Gasto: ${formatCurrencyBrl(spentBrl, hideValues)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (capBrl > 0.0) {
                                                val pct = ((spentBrl / capBrl) * 100.0).toInt()
                                                "Teto: ${formatCurrencyBrl(capBrl, hideValues)} ($pct%)"
                                            } else {
                                                "Mês ant.: ${formatCurrencyBrl(prevSpent, hideValues)}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = if (capBrl > 0.0 && spentBrl > capBrl) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (capBrl > 0.0) {
                                        val progress = (spentBrl / capBrl).toFloat().coerceIn(0f, 1f)
                                        val barColor = when {
                                            spentBrl > capBrl -> ExpenseCoral
                                            progress >= 0.85f -> WantsAmber
                                            else -> IncomeEmerald
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = barColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Detailed Monthly Expense Report Donut Chart & Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Relatório Financeiro Detalhado de Gastos Mensais",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total gasto em ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)}: ${formatCurrencyBrl(totalExpensesBrl, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryBreakdown.isEmpty() || totalExpensesBrl <= 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Registre gastos neste mês para visualizar o gráfico detalhado por categoria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val palette = listOf(
                            Color(0xFF0B6E4F),
                            Color(0xFFD4AF37),
                            Color(0xFF2563EB),
                            Color(0xFFE15554),
                            Color(0xFF8B5CF6),
                            Color(0xFF059669),
                            Color(0xFFD97706)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 24.dp.toPx()
                                    val diameter = minOf(size.width, size.height) - strokeWidth
                                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                    var startAngle = -90f

                                    categoryBreakdown.forEachIndexed { index, entry ->
                                        val sweep = ((entry.value / totalExpensesBrl) * 360.0).toFloat()
                                        drawArc(
                                            color = palette[index % palette.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweep.coerceAtLeast(2f),
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = Size(diameter, diameter),
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${categoryBreakdown.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Categorias",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryBreakdown.forEachIndexed { index, entry ->
                                    val pct = (entry.value / totalExpensesBrl) * 100.0
                                    val color = palette[index % palette.size]
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
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.key,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", pct)} • ${formatCurrencyBrl(entry.value, hideValues)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Custom Family Savings Goals Section
        item {
            Text(
                text = "Metas Personalizadas de Economia Familiar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (savingsGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = SavingsGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma meta personalizada criada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Crie metas como Reserva de Emergência, Viagem ou Casa Própria definindo a porcentagem da renda mensal destinada a cada meta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savingsGoals, key = { it.id }) { goal ->
                SavingsGoalCard(
                    goal = goal,
                    effectiveIncomeBrl = effectiveIncomeBrl,
                    hideValues = hideValues,
                    onContributeClick = { goalForContribution = goal },
                    onEditClick = {
                        editingGoal = goal
                        showAddGoalDialog = true
                    },
                    onDeleteClick = { deletingGoal = goal }
                )
            }
        }
    }

    editingCategoryBudget?.let { cat ->
        val initialLimit = household.categoryBudgetsBrl[cat] ?: 0.0
        var limitText by remember(cat) {
            mutableStateOf(if (initialLimit > 0.0) String.format(Locale.US, "%.2f", initialLimit) else "")
        }
        AlertDialog(
            onDismissRequest = { editingCategoryBudget = null },
            title = {
                Text(
                    text = "Teto de Gastos: $cat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Defina o limite mensal para '$cat'. Deixe 0 para remover o limite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Teto Mensal (R$)") },
                        placeholder = { Text("Ex: 1200.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = limitText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onUpdateCategoryBudget(cat, parsed.coerceAtLeast(0.0))
                        triggerFeedback(
                            if (parsed > 0.0) "Teto de $cat salvo!" else "Teto de $cat removido."
                        )
                        editingCategoryBudget = null
                    }
                ) {
                    Text("Salvar Teto")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategoryBudget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showNewCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatLimit by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = {
                Text(
                    text = "Nova Categoria da Família",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Esta categoria será sincronizada para todos os aparelhos da família:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nome da Categoria") },
                        placeholder = { Text("Ex: Pets / Farmácia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCatLimit,
                        onValueChange = { newCatLimit = it },
                        label = { Text("Teto Mensal Opcional (R$)") },
                        placeholder = { Text("Ex: 400.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCatName.trim()
                        if (clean.isNotEmpty()) {
                            onAddCustomCategory(clean)
                            val limitVal = newCatLimit.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (limitVal > 0.0) {
                                onUpdateCategoryBudget(clean, limitVal)
                            }
                            triggerFeedback("Categoria '$clean' sincronizada na família!")
                            showNewCategoryDialog = false
                        }
                    },
                    enabled = newCatName.trim().isNotEmpty()
                ) {
                    Text("Criar Categoria")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditPlanDialog) {
        EditBudgetPlanDialog(
            household = household,
            onDismiss = { showEditPlanDialog = false },
            onSave = { name, inc, n, w, s ->
                onSaveBudgetPlan(name, inc, n, w, s)
                triggerFeedback("Plano de economia salvo com sucesso!")
            }
        )
    }

    if (showAddGoalDialog) {
        val currentEditing = editingGoal
        AddSavingsGoalDialog(
            initialGoal = currentEditing,
            effectiveIncomeBrl = effectiveIncomeBrl,
            onDismiss = {
                showAddGoalDialog = false
                editingGoal = null
            },
            onSave = { title, target, current, pct, cat, ym ->
                if (currentEditing == null) {
                    onAddSavingsGoal(title, target, current, pct, cat, ym)
                    triggerFeedback("Meta '$title' criada com sucesso!")
                } else {
                    onUpdateSavingsGoal(currentEditing, title, target, current, pct, cat, ym)
                    triggerFeedback("Meta '$title' atualizada!")
                }
                editingGoal = null
            }
        )
    }

    val goalToDelete = deletingGoal
    if (goalToDelete != null) {
        OmniConfirmDeleteDialog(
            title = "Excluir Meta Financeira",
            message = "Deseja excluir a meta '${goalToDelete.title}'?",
            onConfirm = {
                onDeleteGoal(goalToDelete.id)
                triggerFeedback("Meta '${goalToDelete.title}' removida")
                deletingGoal = null
            },
            onDismiss = { deletingGoal = null }
        )
    }

    goalForContribution?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            effectiveIncomeBrl = effectiveIncomeBrl,
            onDismiss = { goalForContribution = null },
            onConfirm = { amount, recordInMonth ->
                onContributeGoal(goal, amount, recordInMonth)
                triggerFeedback("Aporte registrado na meta '${goal.title}'!")
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    effectiveIncomeBrl: Double,
    hideValues: Boolean = false,
    onContributeClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    val progress = if (goal.targetAmountBrl > 0) {
        (goal.currentAmountBrl / goal.targetAmountBrl).toFloat().coerceIn(0f, 1f)
    } else 0f
    val monthlySuggestedBrl = effectiveIncomeBrl * (goal.incomePercentage / 100.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_goal_card_${goal.id}"),
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
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goal.category} • Alocação: ${goal.incomePercentage}% da renda (${formatCurrencyBrl(monthlySuggestedBrl, hideValues)}/mês)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("edit_goal_btn_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar meta",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_goal_btn_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir meta",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Acumulado: ${formatCurrencyBrl(goal.currentAmountBrl, hideValues)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Meta: ${formatCurrencyBrl(goal.targetAmountBrl, hideValues)} (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (progress >= 1f) IncomeEmerald else SavingsGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.targetYearMonth.isNotBlank()) "Prazo alvo: ${goal.targetYearMonth}" else "Meta contínua",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onContributeClick,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("contribute_goal_btn_${goal.id}")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo Aporte")
                }
            }
        }
    }
}

@Composable
private fun EditBudgetPlanDialog(
    household: Household,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, Int, Int) -> Unit
) {
    var nameText by remember { mutableStateOf(household.name) }
    var incomeText by remember { mutableStateOf(household.monthlyIncomeBrl.toString()) }
    var needsText by remember { mutableStateOf(household.needsPercentage.toString()) }
    var wantsText by remember { mutableStateOf(household.wantsPercentage.toString()) }
    var savingsText by remember { mutableStateOf(household.savingsPercentage.toString()) }

    val needs = needsText.toIntOrNull() ?: 0
    val wants = wantsText.toIntOrNull() ?: 0
    val savings = savingsText.toIntOrNull() ?: 0
    val sum = needs + wants + savings
    val income = incomeText.replace(",", ".").toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Personalizar Plano de Economia (%)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nome do Orçamento Familiar") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = incomeText,
                    onValueChange = { incomeText = it },
                    label = { Text("Renda Familiar Mensal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.testTag("budget_income_input")
                )

                Text(
                    text = "Atalhos de Regra por Porcentagem",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = needs == 50 && wants == 30 && savings == 20,
                        onClick = {
                            needsText = "50"
                            wantsText = "30"
                            savingsText = "20"
                        },
                        label = { Text("50/30/20") }
                    )
                    FilterChip(
                        selected = needs == 60 && wants == 20 && savings == 20,
                        onClick = {
                            needsText = "60"
                            wantsText = "20"
                            savingsText = "20"
                        },
                        label = { Text("60/20/20") }
                    )
                    FilterChip(
                        selected = needs == 50 && wants == 20 && savings == 30,
                        onClick = {
                            needsText = "50"
                            wantsText = "20"
                            savingsText = "30"
                        },
                        label = { Text("50/20/30 (Foco Meta)") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = needsText,
                        onValueChange = { needsText = it },
                        label = { Text("Nec. %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wantsText,
                        onValueChange = { wantsText = it },
                        label = { Text("Des. %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = savingsText,
                        onValueChange = { savingsText = it },
                        label = { Text("Econ. %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Soma atual: $sum% (Deve ser igual a 100%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sum == 100) IncomeEmerald else ExpenseCoral,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sum == 100) {
                        onSave(nameText, income, needs, wants, savings)
                        onDismiss()
                    }
                },
                enabled = sum == 100 && income >= 0.0,
                modifier = Modifier.testTag("save_budget_plan_btn")
            ) {
                Text("Salvar Plano")
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
private fun AddSavingsGoalDialog(
    initialGoal: SavingsGoal? = null,
    effectiveIncomeBrl: Double,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ) -> Unit
) {
    var title by remember(initialGoal) { mutableStateOf(initialGoal?.title ?: "") }
    var targetText by remember(initialGoal) {
        mutableStateOf(
            if (initialGoal != null) String.format(Locale.US, "%.2f", initialGoal.targetAmountBrl)
            else "10000"
        )
    }
    var currentText by remember(initialGoal) {
        mutableStateOf(
            if (initialGoal != null) String.format(Locale.US, "%.2f", initialGoal.currentAmountBrl)
            else "0"
        )
    }
    var percentageText by remember(initialGoal) { mutableStateOf((initialGoal?.incomePercentage ?: 10).toString()) }
    var category by remember(initialGoal) { mutableStateOf(initialGoal?.category ?: "Reserva") }
    var targetYearMonth by remember(initialGoal) { mutableStateOf(initialGoal?.targetYearMonth ?: "2027-12") }

    val targetVal = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val currentVal = currentText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pctVal = (percentageText.toIntOrNull() ?: 10).coerceIn(0, 100)
    val monthlyContribution = effectiveIncomeBrl * (pctVal / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGoal == null) "Nova Meta Familiar por Porcentagem" else "Editar Meta Familiar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da Meta") },
                    placeholder = { Text("Ex: Reserva de Emergência / Viagem") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Valor Alvo (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_target_input")
                    )
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        label = { Text("Já Guardado (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = percentageText,
                        onValueChange = { percentageText = it },
                        label = { Text("% da Renda Mensal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_pct_input")
                    )
                    OutlinedTextField(
                        value = targetYearMonth,
                        onValueChange = { targetYearMonth = it },
                        label = { Text("Prazo (AAAA-MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Com $pctVal% da renda familiar, o aporte mensal sugerido é de ${formatCurrencyBrl(monthlyContribution)}.",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Reserva", "Viagem", "Imóvel", "Educação", "Investimento").forEach { cat ->
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
                    if (title.isNotBlank() && targetVal > 0.0) {
                        onSave(title, targetVal, currentVal, pctVal, category, targetYearMonth)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && targetVal > 0.0,
                modifier = Modifier.testTag("save_goal_confirm_btn")
            ) {
                Text(if (initialGoal == null) "Criar Meta" else "Atualizar Meta")
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
private fun ContributeGoalDialog(
    goal: SavingsGoal,
    effectiveIncomeBrl: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    val suggested = (effectiveIncomeBrl * (goal.incomePercentage / 100.0)).coerceAtLeast(100.0)
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", suggested)) }
    var recordInMonth by remember { mutableStateOf(true) }
    val parsed = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Aporte na Meta: ${goal.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aporte sugerido (${goal.incomePercentage}% da renda): ${formatCurrencyBrl(suggested)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor do Aporte (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordInMonth,
                        onCheckedChange = { recordInMonth = it }
                    )
                    Text(
                        text = "Registrar também na faixa de 20% Economia deste mês",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsed > 0.0) {
                        onConfirm(parsed, recordInMonth)
                        onDismiss()
                    }
                },
                enabled = parsed > 0.0
            ) {
                Text("Confirmar Aporte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
