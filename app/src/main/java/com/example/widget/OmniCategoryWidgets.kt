package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AuraFirestoreRepository
import com.example.data.BankAccountCatalog
import com.example.data.DateCalendarUtils
import com.example.data.DiabetesSlotCatalog
import com.example.data.HealthRepository
import com.example.data.LocalOfflineStore
import com.example.data.OmniBackupAndMigrationManager
import com.example.data.formatCurrencyBrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Helper that refreshes all 4 independent category widgets + notifies their scrollable ListViews.
 */
object OmniCategoryWidgetsUpdater {
    const val ACTION_TOGGLE_TASK_FROM_WIDGET = "com.auramanager.app.ACTION_TOGGLE_TASK_FROM_WIDGET"
    const val ACTION_TOGGLE_FINANCE_EYE = "com.auramanager.app.ACTION_TOGGLE_FINANCE_EYE"
    const val ACTION_TOGGLE_MURAL_ITEM = "com.auramanager.app.ACTION_TOGGLE_MURAL_ITEM"
    const val ACTION_GLUCOSE_WIDGET_TIMER_TOGGLE = "com.auramanager.app.ACTION_GLUCOSE_WIDGET_TIMER_TOGGLE"
    const val ACTION_GLUCOSE_WIDGET_TIMER_RESET = "com.auramanager.app.ACTION_GLUCOSE_WIDGET_TIMER_RESET"

    fun refreshAllCategoryWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)

        // 1. Agenda Widget
        runCatching {
            val comp = ComponentName(appContext, OmniAgendaWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(comp)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    OmniAgendaWidgetProvider.updateAgendaWidget(appContext, manager, id)
                }
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_agenda_list_view)
            }
        }

        // 2. Glucose Monitoring Widget
        runCatching {
            val comp = ComponentName(appContext, OmniGlucoseWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(comp)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    OmniGlucoseWidgetProvider.updateGlucoseWidget(appContext, manager, id)
                }
            }
        }

        // 3. Finance & Cards Widget
        runCatching {
            val comp = ComponentName(appContext, OmniFinanceWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(comp)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    OmniFinanceWidgetProvider.updateFinanceWidget(appContext, manager, id)
                }
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_finance_list_view)
            }
        }

        // 4. Family List & Mural Widget
        runCatching {
            val comp = ComponentName(appContext, OmniFamilyListWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(comp)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    OmniFamilyListWidgetProvider.updateFamilyListWidget(appContext, manager, id)
                }
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_family_list_view)
            }
        }
    }

    fun buildMainActivityIntent(
        context: Context,
        requestCode: Int,
        widgetAction: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OmniHomeWidgetProvider.EXTRA_WIDGET_ACTION, widgetAction)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildQuickGlucoseDialogPendingIntent(
        context: Context,
        requestCode: Int,
        preselectedSlot: String
    ): PendingIntent {
        val intent = Intent(context, QuickGlucoseWidgetDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(QuickGlucoseWidgetDialogActivity.EXTRA_DIALOG_MODE, QuickGlucoseWidgetDialogActivity.MODE_GLUCOSE)
            putExtra(QuickGlucoseWidgetDialogActivity.EXTRA_PRESELECTED_SLOT, preselectedSlot)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildQuickFamilyMuralDialogPendingIntent(
        context: Context,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, QuickGlucoseWidgetDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(QuickGlucoseWidgetDialogActivity.EXTRA_DIALOG_MODE, QuickGlucoseWidgetDialogActivity.MODE_FAMILY_MURAL)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Widget 1/4: "Omni • Agenda & Tarefas"
 * Compact 2x2 resizable widget with scrollable ListView of all today's personal & family tasks and 1-tap completion.
 */
class OmniAgendaWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            OmniCategoryWidgetsUpdater.ACTION_TOGGLE_TASK_FROM_WIDGET -> {
                val taskId = intent.getStringExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_TASK_ID).orEmpty()
                val newCompleted = intent.getBooleanExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_TASK_COMPLETED, true)
                if (taskId.isNotBlank()) {
                    val todayIso = DateCalendarUtils.formatIsoDate(DateCalendarUtils.todayEpochDay())
                    val localStore = LocalOfflineStore(appContext)
                    localStore.toggleTaskCompleted(
                        taskId = taskId,
                        completed = newCompleted,
                        targetDateIso = todayIso
                    )
                    val houseId = localStore.userProfileFlow.value.householdId
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            AuraFirestoreRepository(appContext).toggleTaskCompleted(
                                householdId = houseId,
                                taskId = taskId,
                                completed = newCompleted,
                                targetDateIso = todayIso
                            )
                        }
                    }
                    OmniHomeWidgetProvider.requestUpdate(appContext)
                }
            }
            OmniHomeWidgetProvider.ACTION_WIDGET_REFRESH -> {
                OmniHomeWidgetProvider.requestUpdate(appContext)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        for (id in appWidgetIds) {
            updateAgendaWidget(appContext, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAgendaWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.omni_widget_agenda)
            val todayEpoch = DateCalendarUtils.todayEpochDay()
            val todayTasks = LocalOfflineStore(context).tasksFlow.value
                .filter { !it.isInbox && it.occursOnEpochDay(todayEpoch) }
                .map { it.withCompletedForDate(todayEpoch) }
            val pendingCount = todayTasks.count { !it.completed }
            val doneCount = todayTasks.count { it.completed }
            val subtitle = when {
                todayTasks.isEmpty() -> "Livre hoje • ${DateCalendarUtils.formatEpochDayPt(todayEpoch)}"
                pendingCount == 0 -> "✓ $doneCount concluída(s) hoje"
                else -> "$pendingCount pendente(s) • $doneCount feita(s)"
            }
            views.setTextViewText(R.id.widget_agenda_subtitle, subtitle)

            views.setOnClickPendingIntent(
                R.id.widget_agenda_header_click,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9201,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_AGENDA
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_agenda_btn_add,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9202,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_AGENDA
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_agenda_btn_voice,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9203,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_VOICE
                )
            )
            val refreshIntent = Intent(context, OmniAgendaWidgetProvider::class.java).apply {
                action = OmniHomeWidgetProvider.ACTION_WIDGET_REFRESH
            }
            views.setOnClickPendingIntent(
                R.id.widget_agenda_btn_refresh,
                PendingIntent.getBroadcast(
                    context,
                    9204,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Attach RemoteViewsService for scrollable ListView
            val serviceIntent = Intent(context, OmniWidgetsRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(
                    OmniWidgetsRemoteViewsService.EXTRA_WIDGET_LIST_TYPE,
                    OmniWidgetsRemoteViewsService.LIST_TYPE_AGENDA
                )
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_agenda_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_agenda_list_view, R.id.widget_agenda_empty_view)

            val toggleTemplateIntent = Intent(context, OmniAgendaWidgetProvider::class.java).apply {
                action = OmniCategoryWidgetsUpdater.ACTION_TOGGLE_TASK_FROM_WIDGET
            }
            val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val templatePendingIntent = PendingIntent.getBroadcast(
                context,
                9205,
                toggleTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
            )
            views.setPendingIntentTemplate(R.id.widget_agenda_list_view, templatePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Widget 2/4: "Omni • Monitoramento de Glicemia"
 * Compact resizable widget showing latest reading, period selector chips, post-meal timer,
 * and "🩸 Medir" button launching QuickGlucoseWidgetDialogActivity over the home screen.
 */
class OmniGlucoseWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            OmniCategoryWidgetsUpdater.ACTION_GLUCOSE_WIDGET_TIMER_TOGGLE -> {
                togglePostMealTimer(appContext)
                OmniHomeWidgetProvider.requestUpdate(appContext)
            }
            OmniCategoryWidgetsUpdater.ACTION_GLUCOSE_WIDGET_TIMER_RESET -> {
                HealthRepository(appContext).resetPostMealTimer()
                AlarmScheduler.cancelPostMealGlucoseAlarm(appContext)
                OmniHomeWidgetProvider.requestUpdate(appContext)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        for (id in appWidgetIds) {
            updateGlucoseWidget(appContext, appWidgetManager, id)
        }
    }

    private fun togglePostMealTimer(context: Context) {
        val healthRepo = HealthRepository(context)
        val timer = healthRepo.stateFlow.value.postMealTimer
        val now = System.currentTimeMillis()
        when {
            !timer.isRunning -> {
                val durationSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                val mealLabel = timer.mealLabel.ifBlank { "Refeição" }
                healthRepo.startPostMealTimer(mealLabel, durationSec)
                AlarmScheduler.schedulePostMealGlucoseAlarm(
                    context = context,
                    triggerAtMillis = System.currentTimeMillis() + (durationSec * 1000L),
                    mealLabel = mealLabel
                )
            }
            !timer.isPaused -> {
                val remaining = timer.targetEndEpochMillis - now
                if (remaining <= 0L) {
                    val durationSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                    val mealLabel = timer.mealLabel.ifBlank { "Refeição" }
                    healthRepo.startPostMealTimer(mealLabel, durationSec)
                    AlarmScheduler.schedulePostMealGlucoseAlarm(
                        context = context,
                        triggerAtMillis = System.currentTimeMillis() + (durationSec * 1000L),
                        mealLabel = mealLabel
                    )
                } else {
                    healthRepo.pausePostMealTimer()
                    AlarmScheduler.cancelPostMealGlucoseAlarm(context)
                }
            }
            else -> {
                healthRepo.resumePostMealTimer()
                val resumed = healthRepo.stateFlow.value.postMealTimer
                if (resumed.targetEndEpochMillis > System.currentTimeMillis()) {
                    AlarmScheduler.schedulePostMealGlucoseAlarm(
                        context = context,
                        triggerAtMillis = resumed.targetEndEpochMillis,
                        mealLabel = resumed.mealLabel.ifBlank { "Refeição" }
                    )
                }
            }
        }
    }

    companion object {
        private fun formatSec(seconds: Int): String {
            val safe = seconds.coerceAtLeast(0)
            val h = safe / 3600
            val m = (safe % 3600) / 60
            val s = safe % 60
            return if (h > 0) {
                String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.ROOT, "%02d:%02d", m, s)
            }
        }

        fun updateGlucoseWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.omni_widget_glucose)
            val healthRepo = HealthRepository(context)
            val state = healthRepo.stateFlow.value
            val latest = state.glucoseRecords.firstOrNull()

            if (latest != null) {
                val memTag = if (latest.memberName.isNotBlank()) " • ${latest.memberName}" else ""
                views.setTextViewText(
                    R.id.widget_glucose_last_reading,
                    "Última: ${latest.valueMgDl} mg/dL (${latest.slot}$memTag • ${latest.timeHHmm})"
                )
            } else {
                views.setTextViewText(
                    R.id.widget_glucose_last_reading,
                    "Nenhuma medição hoje • Toque em Medir"
                )
            }

            views.setOnClickPendingIntent(
                R.id.widget_glucose_header_click,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9301,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_GLUCOSE
                )
            )

            // Quick Floating Dialog buttons (pre-selecting period and opening numeric keyboard)
            val timer = state.postMealTimer
            val defaultDialogSlot = if (timer.isRunning || timer.completedAlertPending) {
                DiabetesSlotCatalog.slotForPostMealTimer(timer.mealLabel, timer.totalDurationSeconds)
            } else {
                DiabetesSlotCatalog.JEJUM
            }
            views.setOnClickPendingIntent(
                R.id.widget_glucose_btn_quick_measure,
                OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(
                    context,
                    9302,
                    defaultDialogSlot
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_slot_btn_jejum,
                OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(
                    context,
                    9303,
                    DiabetesSlotCatalog.JEJUM
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_slot_btn_1h_almoco,
                OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(
                    context,
                    9304,
                    DiabetesSlotCatalog.POS_ALMOCO_1H
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_slot_btn_2h_almoco,
                OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(
                    context,
                    9305,
                    DiabetesSlotCatalog.POS_ALMOCO_2H
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_slot_btn_jantar,
                OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(
                    context,
                    9306,
                    DiabetesSlotCatalog.POS_JANTAR_1H
                )
            )

            // Post-Meal Timer row
            val now = System.currentTimeMillis()
            val mealLabel = timer.mealLabel.ifBlank { "Refeição" }
            views.setTextViewText(R.id.widget_glucose_timer_label, "⏱️ Pós-$mealLabel")

            val remainingMillis = if (timer.isRunning && !timer.isPaused) {
                (timer.targetEndEpochMillis - now).coerceAtLeast(0L)
            } else if (timer.isRunning && timer.isPaused) {
                timer.pausedRemainingSeconds * 1000L
            } else {
                timer.totalDurationSeconds.coerceAtLeast(60) * 1000L
            }

            if (timer.isRunning && !timer.isPaused && remainingMillis > 0L) {
                views.setViewVisibility(R.id.widget_glucose_only_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_glucose_only_static_time, View.GONE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    views.setChronometerCountDown(R.id.widget_glucose_only_chronometer, true)
                }
                views.setChronometer(
                    R.id.widget_glucose_only_chronometer,
                    SystemClock.elapsedRealtime() + remainingMillis,
                    "%s",
                    true
                )
                views.setTextViewText(R.id.widget_glucose_only_btn_toggle, "⏸ Pausar")
            } else if (timer.isRunning && timer.isPaused) {
                views.setViewVisibility(R.id.widget_glucose_only_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_glucose_only_static_time, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_glucose_only_static_time,
                    formatSec(timer.pausedRemainingSeconds)
                )
                views.setTextViewText(R.id.widget_glucose_only_btn_toggle, "▶ Retomar")
            } else {
                val totalSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                views.setViewVisibility(R.id.widget_glucose_only_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_glucose_only_static_time, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_glucose_only_static_time,
                    formatSec(totalSec)
                )
                val labelH = if (totalSec >= 7200) "▶ 2h" else "▶ 1h"
                views.setTextViewText(R.id.widget_glucose_only_btn_toggle, labelH)
            }

            val toggleIntent = Intent(context, OmniGlucoseWidgetProvider::class.java).apply {
                action = OmniCategoryWidgetsUpdater.ACTION_GLUCOSE_WIDGET_TIMER_TOGGLE
            }
            views.setOnClickPendingIntent(
                R.id.widget_glucose_only_btn_toggle,
                PendingIntent.getBroadcast(
                    context,
                    9307,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            val resetIntent = Intent(context, OmniGlucoseWidgetProvider::class.java).apply {
                action = OmniCategoryWidgetsUpdater.ACTION_GLUCOSE_WIDGET_TIMER_RESET
            }
            views.setOnClickPendingIntent(
                R.id.widget_glucose_only_btn_reset,
                PendingIntent.getBroadcast(
                    context,
                    9308,
                    resetIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Widget 3/4: "Omni • Finanças & Cartões"
 * Compact 2x2 resizable widget with consolidated balance, privacy eye toggle, "+ Gasto" button,
 * and scrollable ListView of all bank accounts & cards with balance/limit progress bars.
 */
class OmniFinanceWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        if (intent.action == OmniCategoryWidgetsUpdater.ACTION_TOGGLE_FINANCE_EYE) {
            OmniBackupAndMigrationManager.toggleWidgetFinanceBalanceHidden(appContext)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        for (id in appWidgetIds) {
            updateFinanceWidget(appContext, appWidgetManager, id)
        }
    }

    companion object {
        fun updateFinanceWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.omni_widget_finance)
            val hideBalances = OmniBackupAndMigrationManager.isWidgetFinanceBalanceHidden(context)
            val store = LocalOfflineStore(context)
            val household = store.householdFlow.value
            val transactions = store.transactionsFlow.value
            val accounts = BankAccountCatalog.calculateCurrentBalances(
                accounts = household.bankAccounts,
                transactions = transactions
            )
            val totalBalance = accounts.sumOf { it.currentBalanceBrl }

            views.setTextViewText(
                R.id.widget_finance_total_balance,
                if (hideBalances) "R$ ••••••" else formatCurrencyBrl(totalBalance)
            )
            views.setTextViewText(
                R.id.widget_finance_btn_eye,
                if (hideBalances) "🙈" else "👁️"
            )

            views.setOnClickPendingIntent(
                R.id.widget_finance_header_click,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9401,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_NEW_TRANSACTION
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_finance_btn_add_tx,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9402,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_NEW_TRANSACTION
                )
            )

            val eyeIntent = Intent(context, OmniFinanceWidgetProvider::class.java).apply {
                action = OmniCategoryWidgetsUpdater.ACTION_TOGGLE_FINANCE_EYE
            }
            views.setOnClickPendingIntent(
                R.id.widget_finance_btn_eye,
                PendingIntent.getBroadcast(
                    context,
                    9403,
                    eyeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val serviceIntent = Intent(context, OmniWidgetsRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(
                    OmniWidgetsRemoteViewsService.EXTRA_WIDGET_LIST_TYPE,
                    OmniWidgetsRemoteViewsService.LIST_TYPE_FINANCE
                )
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_finance_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_finance_list_view, R.id.widget_finance_empty_view)

            views.setPendingIntentTemplate(
                R.id.widget_finance_list_view,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9404,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_NEW_TRANSACTION
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Widget 4/4: "Omni • Lista & Mural da Família"
 * Compact 2x2 resizable widget with scrollable shared family shopping list & mural checklist.
 */
class OmniFamilyListWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        if (intent.action == OmniCategoryWidgetsUpdater.ACTION_TOGGLE_MURAL_ITEM) {
            val itemId = intent.getStringExtra(OmniWidgetsRemoteViewsService.EXTRA_CLICKED_MURAL_ID).orEmpty()
            if (itemId.isNotBlank()) {
                OmniBackupAndMigrationManager.toggleFamilyMuralItem(appContext, itemId)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        for (id in appWidgetIds) {
            updateFamilyListWidget(appContext, appWidgetManager, id)
        }
    }

    companion object {
        fun updateFamilyListWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.omni_widget_family_list)
            val items = OmniBackupAndMigrationManager.getFamilyMuralItems(context)
            val pending = items.count { !it.completed }
            val done = items.count { it.completed }
            views.setTextViewText(
                R.id.widget_family_list_subtitle,
                "$pending pendente(s) • $done marcado(s)"
            )

            views.setOnClickPendingIntent(
                R.id.widget_family_list_header_click,
                OmniCategoryWidgetsUpdater.buildMainActivityIntent(
                    context,
                    9501,
                    OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_HUB
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_family_list_btn_add,
                OmniCategoryWidgetsUpdater.buildQuickFamilyMuralDialogPendingIntent(context, 9502)
            )

            val serviceIntent = Intent(context, OmniWidgetsRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(
                    OmniWidgetsRemoteViewsService.EXTRA_WIDGET_LIST_TYPE,
                    OmniWidgetsRemoteViewsService.LIST_TYPE_FAMILY_MURAL
                )
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_family_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_family_list_view, R.id.widget_family_list_empty_view)

            val toggleIntent = Intent(context, OmniFamilyListWidgetProvider::class.java).apply {
                action = OmniCategoryWidgetsUpdater.ACTION_TOGGLE_MURAL_ITEM
            }
            val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val templatePendingIntent = PendingIntent.getBroadcast(
                context,
                9503,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
            )
            views.setPendingIntentTemplate(R.id.widget_family_list_view, templatePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
