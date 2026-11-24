package com.example.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.BankAccount
import com.example.data.BankAccountCatalog
import com.example.data.DateCalendarUtils
import com.example.data.FamilyMuralItem
import com.example.data.LocalOfflineStore
import com.example.data.OmniBackupAndMigrationManager
import com.example.data.PrivacyScope
import com.example.data.TaskItem
import com.example.data.formatCurrencyBrl
import kotlin.math.abs

class OmniWidgetsRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val listType = intent.getStringExtra(EXTRA_WIDGET_LIST_TYPE) ?: LIST_TYPE_AGENDA
        return when (listType) {
            LIST_TYPE_FINANCE -> FinanceAccountsRemoteViewsFactory(applicationContext)
            LIST_TYPE_FAMILY_MURAL -> FamilyMuralRemoteViewsFactory(applicationContext)
            else -> AgendaTasksRemoteViewsFactory(applicationContext)
        }
    }

    companion object {
        const val EXTRA_WIDGET_LIST_TYPE = "extra_omni_widget_list_type"
        const val LIST_TYPE_AGENDA = "AGENDA"
        const val LIST_TYPE_FINANCE = "FINANCE"
        const val LIST_TYPE_FAMILY_MURAL = "FAMILY_MURAL"

        const val EXTRA_CLICKED_TASK_ID = "extra_clicked_task_id"
        const val EXTRA_CLICKED_TASK_COMPLETED = "extra_clicked_task_completed"
        const val EXTRA_CLICKED_MURAL_ID = "extra_clicked_mural_id"
    }
}

private class AgendaTasksRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var todayTasks: List<TaskItem> = emptyList()

    override fun onCreate() {
        reloadData()
    }

    override fun onDataSetChanged() {
        reloadData()
    }

    private fun reloadData() {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val store = LocalOfflineStore(context)
        todayTasks = store.tasksFlow.value
            .filter { !it.isInbox && it.occursOnEpochDay(todayEpoch) }
            .map { it.withCompletedForDate(todayEpoch) }
            .sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenBy { it.alarmHour }
                    .thenBy { it.alarmMinute }
            )
    }

    override fun onDestroy() {
        todayTasks = emptyList()
    }

    override fun getCount(): Int = todayTasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        val task = todayTasks.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.omni_widget_list_item_row)

        val checkIcon = if (task.completed) "✅" else "⬜"
        views.setTextViewText(R.id.widget_item_check_badge, checkIcon)

        val emoji = task.iconEmoji.ifBlank { "📋" }
        views.setTextViewText(R.id.widget_item_title, "$emoji ${task.title}")

        val startMin = (task.alarmHour.toInt().coerceIn(0, 23) * 60) + task.alarmMinute.toInt().coerceIn(0, 59)
        val durMin = task.durationMinutes.coerceIn(5, 720)
        val endMin = (startMin + durMin).coerceAtMost(23 * 60 + 59)
        val startStr = DateCalendarUtils.formatTime(startMin / 60, startMin % 60)
        val endStr = DateCalendarUtils.formatTime(endMin / 60, endMin % 60)
        val scopeLabel = if (task.privacyScope == PrivacyScope.PERSONAL) {
            "Individual"
        } else {
            task.assignedMemberName.ifBlank { "Família" }
        }
        val locPart = if (task.location.isNotBlank()) " • 📍${task.location}" else ""
        views.setTextViewText(
            R.id.widget_item_subtitle,
            "$startStr-$endStr (${durMin}m) • $scopeLabel$locPart"
        )

        views.setViewVisibility(R.id.widget_item_right_value, View.GONE)
        views.setViewVisibility(R.id.widget_item_progress_bar, View.GONE)

        val fillInIntent = Intent().apply {
            putExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_TASK_ID, task.id)
            putExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_TASK_COMPLETED, !task.completed)
        }
        views.setOnClickFillInIntent(R.id.widget_item_row_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = todayTasks.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}

private class FinanceAccountsRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var accountsWithBalance: List<BankAccount> = emptyList()
    private var hideBalances: Boolean = false

    override fun onCreate() {
        reloadData()
    }

    override fun onDataSetChanged() {
        reloadData()
    }

    private fun reloadData() {
        hideBalances = OmniBackupAndMigrationManager.isWidgetFinanceBalanceHidden(context)
        val store = LocalOfflineStore(context)
        val household = store.householdFlow.value
        val transactions = store.transactionsFlow.value
        accountsWithBalance = BankAccountCatalog.calculateCurrentBalances(
            accounts = household.bankAccounts,
            transactions = transactions
        )
    }

    override fun onDestroy() {
        accountsWithBalance = emptyList()
    }

    override fun getCount(): Int = accountsWithBalance.size

    override fun getViewAt(position: Int): RemoteViews? {
        val acc = accountsWithBalance.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.omni_widget_list_item_row)

        val emoji = acc.iconEmoji.ifBlank { acc.emoji.ifBlank { "🏦" } }
        views.setTextViewText(R.id.widget_item_check_badge, emoji)
        views.setTextViewText(R.id.widget_item_title, acc.name)

        val scopeLabel = if (acc.privacyScope == PrivacyScope.PERSONAL) "Individual" else "Familiar"
        views.setTextViewText(R.id.widget_item_subtitle, "${acc.institution} • $scopeLabel")

        val valueText = if (hideBalances) {
            "R$ ••••"
        } else {
            formatCurrencyBrl(acc.currentBalanceBrl)
        }
        views.setTextViewText(R.id.widget_item_right_value, valueText)
        views.setViewVisibility(R.id.widget_item_right_value, View.VISIBLE)

        // Show visual balance/limit utilization bar
        val baseRef = maxOf(abs(acc.initialBalanceBrl), abs(acc.currentBalanceBrl), 1000.0)
        val pct = ((acc.currentBalanceBrl.coerceAtLeast(0.0) / baseRef) * 100.0).toInt().coerceIn(8, 100)
        views.setViewVisibility(R.id.widget_item_progress_bar, View.VISIBLE)
        views.setProgressBar(R.id.widget_item_progress_bar, 100, pct, false)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_row_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = accountsWithBalance.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}

private class FamilyMuralRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var muralItems: List<FamilyMuralItem> = emptyList()

    override fun onCreate() {
        reloadData()
    }

    override fun onDataSetChanged() {
        reloadData()
    }

    private fun reloadData() {
        muralItems = OmniBackupAndMigrationManager.getFamilyMuralItems(context)
    }

    override fun onDestroy() {
        muralItems = emptyList()
    }

    override fun getCount(): Int = muralItems.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = muralItems.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.omni_widget_list_item_row)

        val checkIcon = if (item.completed) "✅" else "⬜"
        views.setTextViewText(R.id.widget_item_check_badge, checkIcon)
        views.setTextViewText(R.id.widget_item_title, item.title)
        views.setTextViewText(R.id.widget_item_subtitle, "${item.category} • ${item.addedBy}")

        views.setViewVisibility(R.id.widget_item_right_value, View.GONE)
        views.setViewVisibility(R.id.widget_item_progress_bar, View.GONE)

        val fillInIntent = Intent().apply {
            putExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_MURAL_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_row_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = muralItems.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
