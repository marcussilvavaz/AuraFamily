package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.CustomizationRepository
import com.example.data.DateCalendarUtils
import com.example.data.HealthRepository
import com.example.data.LocalOfflineStore
import java.util.Locale

class OmniHomeWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                requestUpdate(appContext)
            }
            ACTION_WIDGET_GLUCOSE_TOGGLE -> {
                handleGlucoseTimerToggle(appContext)
                requestUpdate(appContext)
            }
            ACTION_WIDGET_GLUCOSE_RESET -> {
                val healthRepo = HealthRepository(appContext)
                healthRepo.resetPostMealTimer()
                AlarmScheduler.cancelPostMealGlucoseAlarm(appContext)
                requestUpdate(appContext)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context.applicationContext, appWidgetManager, appWidgetId)
        }
    }

    private fun handleGlucoseTimerToggle(context: Context) {
        val healthRepo = HealthRepository(context)
        val timer = healthRepo.stateFlow.value.postMealTimer
        val now = System.currentTimeMillis()

        when {
            !timer.isRunning -> {
                val durationSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                val mealLabel = timer.mealLabel.ifBlank { "Refeição" }
                healthRepo.startPostMealTimer(mealLabel, durationSec)
                val targetEnd = System.currentTimeMillis() + (durationSec * 1000L)
                AlarmScheduler.schedulePostMealGlucoseAlarm(
                    context = context,
                    triggerAtMillis = targetEnd,
                    mealLabel = mealLabel
                )
            }
            !timer.isPaused -> {
                val remainingMillis = timer.targetEndEpochMillis - now
                if (remainingMillis <= 0L) {
                    // Timer já terminou -> reiniciar novo ciclo de 1h
                    val durationSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                    val mealLabel = timer.mealLabel.ifBlank { "Refeição" }
                    healthRepo.startPostMealTimer(mealLabel, durationSec)
                    val targetEnd = System.currentTimeMillis() + (durationSec * 1000L)
                    AlarmScheduler.schedulePostMealGlucoseAlarm(
                        context = context,
                        triggerAtMillis = targetEnd,
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
        const val ACTION_WIDGET_REFRESH = "com.auramanager.app.ACTION_WIDGET_REFRESH"
        const val ACTION_WIDGET_GLUCOSE_TOGGLE = "com.auramanager.app.ACTION_WIDGET_GLUCOSE_TOGGLE"
        const val ACTION_WIDGET_GLUCOSE_RESET = "com.auramanager.app.ACTION_WIDGET_GLUCOSE_RESET"

        const val EXTRA_WIDGET_ACTION = "extra_omni_widget_action"
        const val WIDGET_ACTION_OPEN_VOICE = "OPEN_VOICE_COMMAND"
        const val WIDGET_ACTION_OPEN_NEW_TRANSACTION = "OPEN_NEW_TRANSACTION"
        const val WIDGET_ACTION_OPEN_AGENDA = "OPEN_AGENDA"
        const val WIDGET_ACTION_OPEN_GLUCOSE = "OPEN_GLUCOSE_LOG"
        const val WIDGET_ACTION_OPEN_HUB = "OPEN_HUB"

        fun requestUpdate(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                val manager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, OmniHomeWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(componentName)
                if (ids != null && ids.isNotEmpty()) {
                    for (id in ids) {
                        updateSingleWidget(appContext, manager, id)
                    }
                }
                OmniCategoryWidgetsUpdater.refreshAllCategoryWidgets(appContext)
            }
        }

        private fun formatDurationSeconds(seconds: Int): String {
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

        private fun buildActivityPendingIntent(
            context: Context,
            requestCode: Int,
            widgetAction: String
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_WIDGET_ACTION, widgetAction)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun buildBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, OmniHomeWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.omni_home_widget)

            val todayEpoch = DateCalendarUtils.todayEpochDay()
            val todayFormatted = DateCalendarUtils.formatEpochDayPt(todayEpoch)
            views.setTextViewText(R.id.widget_today_date, "Hoje • $todayFormatted")

            // Click actions on Header & Shortcuts
            views.setOnClickPendingIntent(
                R.id.widget_header_click_area,
                buildActivityPendingIntent(context, 9101, WIDGET_ACTION_OPEN_HUB)
            )
            views.setOnClickPendingIntent(
                R.id.widget_logo_icon,
                buildActivityPendingIntent(context, 9102, WIDGET_ACTION_OPEN_HUB)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_voice,
                buildActivityPendingIntent(context, 9103, WIDGET_ACTION_OPEN_VOICE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_new_tx,
                buildActivityPendingIntent(context, 9104, WIDGET_ACTION_OPEN_NEW_TRANSACTION)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_refresh,
                buildBroadcastPendingIntent(context, 9105, ACTION_WIDGET_REFRESH)
            )

            // 1. Condicional: Cronômetro de Glicemia Pós-Refeição
            val config = CustomizationRepository(context).configFlow.value
            if (config.diabetesEnabled) {
                views.setViewVisibility(R.id.widget_glucose_container, View.VISIBLE)
                val healthRepo = HealthRepository(context)
                val timer = healthRepo.stateFlow.value.postMealTimer
                val now = System.currentTimeMillis()
                val mealName = timer.mealLabel.ifBlank { "Refeição" }
                views.setTextViewText(R.id.widget_glucose_title, "🩸 Glicemia Pós-$mealName")

                val remainingMillis = if (timer.isRunning && !timer.isPaused) {
                    (timer.targetEndEpochMillis - now).coerceAtLeast(0L)
                } else if (timer.isRunning && timer.isPaused) {
                    timer.pausedRemainingSeconds * 1000L
                } else {
                    timer.totalDurationSeconds.coerceAtLeast(60) * 1000L
                }

                val isFinished = (timer.isRunning && !timer.isPaused && remainingMillis == 0L) || timer.completedAlertPending

                if (timer.isRunning && !timer.isPaused && remainingMillis > 0L) {
                    views.setViewVisibility(R.id.widget_glucose_chronometer, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_glucose_static_time, View.GONE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.widget_glucose_chronometer, true)
                    }
                    val baseElapsed = SystemClock.elapsedRealtime() + remainingMillis
                    views.setChronometer(R.id.widget_glucose_chronometer, baseElapsed, "%s", true)
                    views.setTextViewText(
                        R.id.widget_glucose_status,
                        "Contagem ativa em tempo real • Alarme programado"
                    )
                    views.setTextViewText(R.id.widget_btn_glucose_toggle, "⏸ Pausar")
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_glucose_toggle,
                        buildBroadcastPendingIntent(context, 9106, ACTION_WIDGET_GLUCOSE_TOGGLE)
                    )
                } else if (isFinished) {
                    views.setViewVisibility(R.id.widget_glucose_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_glucose_static_time, View.VISIBLE)
                    views.setTextViewText(R.id.widget_glucose_static_time, "00:00")
                    views.setTextViewText(
                        R.id.widget_glucose_status,
                        "⏰ Tempo concluído! Toque para registrar a glicemia agora."
                    )
                    views.setTextViewText(R.id.widget_btn_glucose_toggle, "🩸 Registrar Glicemia")
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_glucose_toggle,
                        buildActivityPendingIntent(context, 9107, WIDGET_ACTION_OPEN_GLUCOSE)
                    )
                } else if (timer.isRunning && timer.isPaused) {
                    views.setViewVisibility(R.id.widget_glucose_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_glucose_static_time, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_glucose_static_time,
                        formatDurationSeconds(timer.pausedRemainingSeconds)
                    )
                    views.setTextViewText(
                        R.id.widget_glucose_status,
                        "Cronômetro pausado • Toque em Retomar para continuar"
                    )
                    views.setTextViewText(R.id.widget_btn_glucose_toggle, "▶ Retomar")
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_glucose_toggle,
                        buildBroadcastPendingIntent(context, 9106, ACTION_WIDGET_GLUCOSE_TOGGLE)
                    )
                } else {
                    val totalSec = timer.totalDurationSeconds.takeIf { it > 0 } ?: 3600
                    views.setViewVisibility(R.id.widget_glucose_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_glucose_static_time, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_glucose_static_time,
                        formatDurationSeconds(totalSec)
                    )
                    views.setTextViewText(
                        R.id.widget_glucose_status,
                        "Pronto • Toque em Iniciar após sua refeição"
                    )
                    val labelMin = totalSec / 60
                    val btnLabel = if (labelMin == 60) "▶ Iniciar 1h" else "▶ Iniciar ${labelMin}m"
                    views.setTextViewText(R.id.widget_btn_glucose_toggle, btnLabel)
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_glucose_toggle,
                        buildBroadcastPendingIntent(context, 9106, ACTION_WIDGET_GLUCOSE_TOGGLE)
                    )
                }

                views.setOnClickPendingIntent(
                    R.id.widget_btn_glucose_reset,
                    buildBroadcastPendingIntent(context, 9108, ACTION_WIDGET_GLUCOSE_RESET)
                )
                val defaultSlot = com.example.data.DiabetesSlotCatalog.slotForPostMealTimer(
                    timer.mealLabel,
                    timer.totalDurationSeconds
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_glucose_log,
                    OmniCategoryWidgetsUpdater.buildQuickGlucoseDialogPendingIntent(context, 9109, defaultSlot)
                )
            } else {
                views.setViewVisibility(R.id.widget_glucose_container, View.GONE)
            }

            // 2. Tarefas de Hoje (respeitando recorrência, período e conclusão por data)
            val allTasks = LocalOfflineStore(context).tasksFlow.value
            val todayTasks = allTasks
                .filter { !it.isInbox && it.occursOnEpochDay(todayEpoch) }
                .map { it.withCompletedForDate(todayEpoch) }
                .sortedWith(
                    compareBy<com.example.data.TaskItem> { it.completed }
                        .thenBy { it.alarmHour }
                        .thenBy { it.alarmMinute }
                )

            val pendingCount = todayTasks.count { !it.completed }
            val completedCount = todayTasks.count { it.completed }
            val badgeText = when {
                todayTasks.isEmpty() -> "Livre hoje"
                pendingCount == 0 -> "✓ Tudo concluído ($completedCount)"
                else -> "$pendingCount pendente(s) • $completedCount feita(s)"
            }
            views.setTextViewText(R.id.widget_tasks_count_badge, badgeText)

            views.setOnClickPendingIntent(
                R.id.widget_tasks_container,
                buildActivityPendingIntent(context, 9110, WIDGET_ACTION_OPEN_AGENDA)
            )

            val rowIds = listOf(
                R.id.widget_task_row_1,
                R.id.widget_task_row_2,
                R.id.widget_task_row_3,
                R.id.widget_task_row_4
            )
            rowIds.forEach { views.setViewVisibility(it, View.GONE) }

            if (todayTasks.isEmpty()) {
                views.setViewVisibility(R.id.widget_task_empty, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_task_empty, View.GONE)
                val displayTasks = todayTasks.take(4)
                displayTasks.forEachIndexed { index, task ->
                    val rowId = rowIds[index]
                    val timeLabel = DateCalendarUtils.formatTime(task.alarmHour.toInt(), task.alarmMinute.toInt())
                    val statusCheck = if (task.completed) "✅" else "🕒"
                    val locSuffix = if (task.location.isNotBlank()) " • 📍${task.location}" else ""
                    val rowText = "$statusCheck $timeLabel  ${task.iconEmoji} ${task.title}$locSuffix"
                    views.setTextViewText(rowId, rowText)
                    views.setViewVisibility(rowId, View.VISIBLE)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
