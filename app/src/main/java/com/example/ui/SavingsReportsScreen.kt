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
    onContributeGoal: (SavingsGoal, Double, Boolean) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    val cardRadius = LocalOrganicCardRadius.current
    var showEditPlanDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalForContribution by remember { mutableStateOf<SavingsGoal?>(null) }

    val monthTransactions = remember(transactions, selectedYearMonth) {
        transactions.filter { it.yearMonth == selectedYearMonth }
    }
    val monthExpenses = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }
    }
    val loggedIncomeBrl = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amountBrl }
    }
    val effectiveIncomeBrl = maxOf(household.monthlyIncomeBrl, loggedIncomeBrl)
    val totalExpensesBrl = monthExpenses.sumOf { it.amountBrl }

    val needsSpent = monthExpenses.filter { it.budgetBucket == "NEEDS" }.sumOf { it.amountBrl }
    val wantsSpent = monthExpenses.filter { it.budgetBucket == "WANTS" }.sumOf { it.amountBrl }
    val savingsSpent = monthExpenses.filter { it.budgetBucket == "SAVINGS" }.sumOf { it.amountBrl }

    val categoryBreakdown = remember(monthExpenses) {
        monthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountBrl } }
            .entries
            .sortedByDescending { it.value }
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
                        text = "Economia & Relatórios",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Análise de ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)} e Metas Familiares",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddGoalDialog = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("new_savings_goal_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nova Meta")
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
                                text = "Regra de Orçamento por Porcentagem (${household.needsPercentage}/${household.wantsPercentage}/${household.savingsPercentage})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Renda Familiar Base: ${formatCurrencyBrl(effectiveIncomeBrl)}",
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
                            Text("Personalizar %")
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
                                text = "Total gasto em ${DateCalendarUtils.formatYearMonthDisplay(selectedYearMonth)}: ${formatCurrencyBrl(totalExpensesBrl)}",
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
                                            text = "${String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", pct)} • ${formatCurrencyBrl(entry.value)}",
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
                    onContributeClick = { goalForContribution = goal },
                    onDeleteClick = { onDeleteGoal(goal.id) }
                )
            }
        }
    }

    if (showEditPlanDialog) {
        EditBudgetPlanDialog(
            household = household,
            onDismiss = { showEditPlanDialog = false },
            onSave = onSaveBudgetPlan
        )
    }

    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            effectiveIncomeBrl = effectiveIncomeBrl,
            onDismiss = { showAddGoalDialog = false },
            onSave = onAddSavingsGoal
        )
    }

    goalForContribution?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            effectiveIncomeBrl = effectiveIncomeBrl,
            onDismiss = { goalForContribution = null },
            onConfirm = { amount, recordInMonth ->
                onContributeGoal(goal, amount, recordInMonth)
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    effectiveIncomeBrl: Double,
    onContributeClick: () -> Unit,
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
                        text = "${goal.category} • Alocação: ${goal.incomePercentage}% da renda (${formatCurrencyBrl(monthlySuggestedBrl)}/mês)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_goal_btn_${goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir meta",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Acumulado: ${formatCurrencyBrl(goal.currentAmountBrl)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Meta: ${formatCurrencyBrl(goal.targetAmountBrl)} (${(progress * 100).toInt()}%)",
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
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("10000") }
    var currentText by remember { mutableStateOf("0") }
    var percentageText by remember { mutableStateOf("10") }
    var category by remember { mutableStateOf("Reserva") }
    var targetYearMonth by remember { mutableStateOf("2027-12") }

    val targetVal = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val currentVal = currentText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pctVal = (percentageText.toIntOrNull() ?: 10).coerceIn(0, 100)
    val monthlyContribution = effectiveIncomeBrl * (pctVal / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Meta Familiar por Porcentagem",
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
                Text("Criar Meta")
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
