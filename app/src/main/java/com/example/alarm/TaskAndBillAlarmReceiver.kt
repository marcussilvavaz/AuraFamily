package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CustomizationRepository
import com.example.data.DateCalendarUtils
import com.example.data.DiabetesSlotCatalog
import com.example.data.HealthRepository
import com.example.data.InsulinScheduleConfig
import com.example.data.LocalOfflineStore
import com.example.widget.OmniHomeWidgetProvider
import java.util.Locale

class OmniBootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            runCatching {
                AlarmScheduler.rescheduleAllActiveAlarms(context.applicationContext)
            }
        }
    }
}

class TaskAndBillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

        if (action == ACTION_STOP_ALARM) {
            TaskTonePreviewPlayer.stopAllAudio(context, dismissRingingNotification = true)
            runCatching {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            return
        }

        if (action == ACTION_SNOOZE_ALARM) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Lembrete Omni"
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Soneca de 5 minutos concluída."
            val soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
            val vibrateEnabled = intent.getBooleanExtra(EXTRA_VIBRATE, true)
            val isBillReminder = intent.getBooleanExtra(EXTRA_IS_BILL, false)
            val openGlucoseSlot = intent.getStringExtra(EXTRA_OPEN_GLUCOSE_SLOT).orEmpty()

            TaskTonePreviewPlayer.stopAllAudio(context, dismissRingingNotification = true)
            runCatching {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            AlarmScheduler.scheduleSnoozeAlarm(
                context = context,
                notificationId = notificationId,
                title = title,
                message = message,
                soundUriString = soundUriStr,
                vibrate = vibrateEnabled,
                isBillReminder = isBillReminder,
                snoozeMinutes = 5,
                openGlucoseSlot = openGlucoseSlot
            )
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Lembrete Omni"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Você tem uma tarefa, conta ou medição agendada para agora."
        val isBillReminder = intent.getBooleanExtra(EXTRA_IS_BILL, false)
        val openGlucoseSlot = intent.getStringExtra(EXTRA_OPEN_GLUCOSE_SLOT).orEmpty()
        val isInsulinAlarm = intent.getBooleanExtra(EXTRA_IS_INSULIN_ALARM, false)
        val insulinConfigId = intent.getStringExtra(EXTRA_INSULIN_CONFIG_ID).orEmpty()

        val customRepo = CustomizationRepository(context)
        val config = customRepo.configFlow.value
        val soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI)
            ?: if (isBillReminder) config.billReminderSoundUri else config.taskAlarmSoundUri
        val vibrateEnabled = intent.getBooleanExtra(
            EXTRA_VIBRATE,
            if (isBillReminder) config.billVibrateEnabled else config.taskVibrateEnabled
        )

        if (notificationId == 7701) {
            runCatching {
                HealthRepository(context.applicationContext).markPostMealTimerFinished()
            }
        }

        AlarmScheduler.playCustomRingtoneAndVibrate(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            soundUriString = soundUriStr,
            vibrate = vibrateEnabled,
            isBillReminder = isBillReminder
        )
        AlarmScheduler.showImmediateNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            soundUriString = soundUriStr,
            vibrate = vibrateEnabled,
            isBillReminder = isBillReminder,
            openGlucoseSlot = openGlucoseSlot
        )

        // Automatically re-schedule recurring daily reminders for the next day
        if (notificationId == 7799 && config.morningSummaryEnabled) {
            runCatching {
                AlarmScheduler.scheduleMorningSummary(
                    context = context,
                    hour = config.morningSummaryHour,
                    minute = config.morningSummaryMinute
                )
            }
        } else if (notificationId == 7788 && config.bloodPressureEnabled) {
            runCatching {
                AlarmScheduler.scheduleBloodPressureDailyReminder(
                    context = context,
                    enabled = true,
                    reminderTimeRaw = config.bloodPressureReminderTime
                )
            }
        } else if (isInsulinAlarm && insulinConfigId.isNotBlank()) {
            runCatching {
                val healthRepo = HealthRepository(context.applicationContext)
                val cfg = healthRepo.stateFlow.value.insulinConfigs.firstOrNull { it.id == insulinConfigId }
                if (cfg != null && cfg.alarmEnabled) {
                    AlarmScheduler.scheduleSingleInsulinAlarm(context.applicationContext, cfg)
                }
            }
        }
    }

    companion object {
        const val ACTION_STOP_ALARM = "com.auramanager.app.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.auramanager.app.ACTION_SNOOZE_ALARM"
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_MESSAGE = "extra_alarm_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_IS_BILL = "extra_is_bill"
        const val EXTRA_OPEN_GLUCOSE_SLOT = "extra_open_glucose_slot"
        const val EXTRA_IS_INSULIN_ALARM = "extra_is_insulin_alarm"
        const val EXTRA_INSULIN_CONFIG_ID = "extra_insulin_config_id"
        const val CHANNEL_ID = "aura_financas_alarms_channel"
    }
}

object AlarmScheduler {

    private fun scheduleExactOrFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }
        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    /**
     * Ensures the notification channel exists. Since TaskTonePreviewPlayer handles centralized
     * audio playback (so the user can stop it immediately with "Parar Alarme"), we silence
     * the channel's duplicate OS sound to prevent double-playing or un-stoppable OS ringtones,
     * while optionally creating a custom sound channel for standard notifications.
     */
    fun ensureNotificationChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return TaskAndBillAlarmReceiver.CHANNEL_ID
            val channelId = "omni_controlled_alarms_v2"
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                // Audio is played centrally by TaskTonePreviewPlayer so "Parar Alarme" stops it immediately
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
            return channelId
        }
        return TaskAndBillAlarmReceiver.CHANNEL_ID
    }

    /**
     * Recreates/updates a dedicated notification channel when the user chooses a custom audio file
     * from the phone for app notifications.
     */
    fun syncCustomNotificationChannel(context: Context, soundUriString: String, soundTitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "omni_custom_notif_${soundUriString.hashCode()}"
            val channel = NotificationChannel(
                channelId,
                "Notificações Omni ($soundTitle)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal de notificações com áudio personalizado do Omni"
                if (soundUriString.isNotBlank()) {
                    runCatching {
                        val attrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        setSound(Uri.parse(soundUriString), attrs)
                    }
                }
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun playCustomRingtoneAndVibrate(
        context: Context,
        soundUriString: String,
        vibrate: Boolean,
        notificationId: Int = 1001,
        title: String = "Alarme Omni",
        message: String = "Toque em Parar Alarme para silenciar.",
        isBillReminder: Boolean = false
    ) {
        TaskTonePreviewPlayer.startRingingAlarm(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            soundUriString = soundUriString,
            vibrate = vibrate,
            isBillReminder = isBillReminder
        )
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        soundUriString: String,
        vibrate: Boolean,
        isBillReminder: Boolean,
        snoozeMinutes: Int = 5,
        openGlucoseSlot: String = ""
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60_000L)
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, title)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, soundUriString)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, vibrate)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, isBillReminder)
            if (openGlucoseSlot.isNotBlank()) {
                putExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT, openGlucoseSlot)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun scheduleTaskAlarm(
        context: Context,
        taskId: String,
        title: String,
        category: String,
        dateEpochDay: Long,
        hour: Int,
        minute: Int,
        ringtoneCategory: String = "APP_DEFAULT"
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(dateEpochDay, hour, minute)
        val config = CustomizationRepository(context).configFlow.value
        val resolvedSoundUri = when {
            ringtoneCategory.isBlank() || ringtoneCategory == "APP_DEFAULT" -> config.taskAlarmSoundUri
            ringtoneCategory.startsWith("content://") || ringtoneCategory.startsWith("file://") || ringtoneCategory.startsWith("android.resource://") -> ringtoneCategory
            else -> "TONE:$ringtoneCategory"
        }

        val notificationId = ("task_$taskId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "Omni • Alarme da Agenda: $title")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Categoria: $category • Horário: ${DateCalendarUtils.formatTime(hour, minute)}"
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, resolvedSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, config.taskVibrateEnabled)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val effectiveTime = if (triggerAtMillis > System.currentTimeMillis()) {
            triggerAtMillis
        } else {
            System.currentTimeMillis() + 5_000L
        }

        return try {
            scheduleExactOrFallback(alarmManager, effectiveTime, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = ("task_$taskId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun schedulePostMealGlucoseAlarm(
        context: Context,
        triggerAtMillis: Long,
        mealLabel: String
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val config = CustomizationRepository(context).configFlow.value
        val notificationId = 7701
        val normalizedSlot = DiabetesSlotCatalog.slotForPostMealTimer(mealLabel, 3600)
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "🩸 Omni • Hora de Medir sua Glicemia ($normalizedSlot)!")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Completou 1h pós-refeição ($normalizedSlot). Toque para registrar sua medição de glicemia agora."
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, config.taskAlarmSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, false)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT, normalizedSlot)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelPostMealGlucoseAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = 7701
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun syncInsulinScheduleAlarms(
        context: Context,
        configs: List<InsulinScheduleConfig>
    ) {
        for (cfg in configs) {
            if (cfg.alarmEnabled) {
                scheduleSingleInsulinAlarm(context, cfg)
            } else {
                cancelInsulinScheduleAlarm(context, cfg.id)
            }
        }
    }

    fun scheduleSingleInsulinAlarm(
        context: Context,
        config: InsulinScheduleConfig
    ): Boolean {
        if (!config.alarmEnabled) {
            cancelInsulinScheduleAlarm(context, config.id)
            return false
        }
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val appConfig = CustomizationRepository(context).configFlow.value
        val notificationId = ("insulin_${config.id}").hashCode()

        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val hour = config.alarmHour.coerceIn(0, 23)
        val minute = config.alarmMinute.coerceIn(0, 59)
        var triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch, hour, minute)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch + 1L, hour, minute)
        }

        val timeFormatted = DateCalendarUtils.formatTime(hour, minute)
        val memberPart = config.memberId.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_TITLE,
                "💉 Omni • Hora da Insulina: ${config.insulinType} (${config.defaultUnits} UI)"
            )
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Horário programado: $timeFormatted$memberPart. Lembre-se de alternar o local de aplicação."
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, appConfig.taskAlarmSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, false)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_INSULIN_ALARM, true)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_INSULIN_CONFIG_ID, config.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelInsulinScheduleAlarm(context: Context, configId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = ("insulin_$configId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSubscriptionReminder(
        context: Context,
        subscriptionId: String,
        title: String,
        amountFormatted: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        ringtoneCategory: String = "CONTAS"
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = DateCalendarUtils.nextDueAlarmMillis(dueDayOfMonth, reminderDaysBefore)
        val config = CustomizationRepository(context).configFlow.value
        val resolvedSoundUri = when {
            ringtoneCategory.isBlank() || ringtoneCategory == "APP_DEFAULT" || ringtoneCategory == "CONTAS" -> config.billReminderSoundUri
            ringtoneCategory.startsWith("content://") || ringtoneCategory.startsWith("file://") || ringtoneCategory.startsWith("android.resource://") -> ringtoneCategory
            else -> "TONE:$ringtoneCategory"
        }

        val notificationId = ("sub_$subscriptionId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "Omni • Lembrete de Conta Fixa: $title")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Valor: $amountFormatted • Vencimento todo dia $dueDayOfMonth"
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, resolvedSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, config.billVibrateEnabled)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelSubscriptionReminder(context: Context, subscriptionId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = ("sub_$subscriptionId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleBloodPressureDailyReminder(
        context: Context,
        enabled: Boolean,
        reminderTimeRaw: String
    ): Boolean {
        if (!enabled) {
            cancelBloodPressureDailyReminder(context)
            return false
        }
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val config = CustomizationRepository(context).configFlow.value
        val notificationId = 7788

        val timeRegex = Regex("""(\d{1,2}):(\d{2})""")
        val match = timeRegex.find(reminderTimeRaw)
        val hour = match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 21
        val minute = match?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 30

        val todayEpoch = DateCalendarUtils.todayEpochDay()
        var triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch, hour, minute)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch + 1L, hour, minute)
        }

        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "❤️ Omni • Hora de Aferir sua Pressão Arterial")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Lembrete diário ($reminderTimeRaw). Registre sua medição sistólica/diastólica na aba Saúde."
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, config.taskAlarmSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, config.taskVibrateEnabled)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelBloodPressureDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = 7788
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllActiveAlarms(context: Context) {
        val appContext = context.applicationContext
        val customRepo = CustomizationRepository(appContext)
        val config = customRepo.configFlow.value
        val store = LocalOfflineStore(appContext)
        val healthRepo = HealthRepository(appContext)

        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val nowMs = System.currentTimeMillis()
        val tasks = store.tasksFlow.value
        val subscriptions = store.subscriptionsFlow.value
        val household = store.householdFlow.value

        tasks.forEach { task ->
            val completedToday = task.isCompletedOnDate(todayEpoch)
            val targetEpoch = if (task.dateEpochDay >= todayEpoch) task.dateEpochDay else todayEpoch
            val alarmMillis = DateCalendarUtils.epochMillisForAlarm(
                targetEpoch,
                task.alarmHour.toInt(),
                task.alarmMinute.toInt()
            )
            if (task.alarmEnabled && !task.isInbox && !completedToday && alarmMillis > nowMs) {
                scheduleTaskAlarm(
                    context = appContext,
                    taskId = task.id,
                    title = task.title,
                    category = task.category,
                    dateEpochDay = targetEpoch,
                    hour = task.alarmHour.toInt(),
                    minute = task.alarmMinute.toInt(),
                    ringtoneCategory = task.ringtoneCategory
                )
            }
        }

        subscriptions.forEach { sub ->
            if (sub.active) {
                scheduleSubscriptionReminder(
                    context = appContext,
                    subscriptionId = sub.id,
                    title = sub.title,
                    amountFormatted = String.format(Locale.US, "R$ %.2f", sub.amountBrl),
                    dueDayOfMonth = sub.dueDayOfMonth.toInt(),
                    reminderDaysBefore = sub.reminderDaysBefore.toInt(),
                    ringtoneCategory = sub.ringtoneCategory
                )
            }
        }

        if (config.morningSummaryEnabled) {
            val tasksTodayCount = tasks.count { task ->
                !task.isInbox && task.occursOnEpochDay(todayEpoch) && !task.isCompletedOnDate(todayEpoch)
            }
            val currentDayOfMonth = java.time.LocalDate.ofEpochDay(todayEpoch).dayOfMonth.toLong()
            val billsTodayCount = subscriptions.count { it.active && it.dueDayOfMonth == currentDayOfMonth }
            val shoppingPendingCount = household.muralItems.count { !it.isNote && !it.done }
            scheduleMorningSummary(
                context = appContext,
                hour = config.morningSummaryHour,
                minute = config.morningSummaryMinute,
                tasksCountToday = tasksTodayCount,
                billsDueTodayCount = billsTodayCount,
                shoppingItemsPendingCount = shoppingPendingCount
            )
        } else {
            cancelMorningSummary(appContext)
        }

        scheduleBloodPressureDailyReminder(
            context = appContext,
            enabled = config.bloodPressureEnabled,
            reminderTimeRaw = config.bloodPressureReminderTime
        )

        val healthState = healthRepo.stateFlow.value
        val timer = healthState.postMealTimer
        if (timer.isRunning && !timer.isPaused && timer.targetEndEpochMillis > nowMs) {
            schedulePostMealGlucoseAlarm(
                context = appContext,
                triggerAtMillis = timer.targetEndEpochMillis,
                mealLabel = timer.mealLabel
            )
        }
        syncInsulinScheduleAlarms(appContext, healthState.insulinConfigs)
    }

    fun scheduleMorningSummary(
        context: Context,
        hour: Int,
        minute: Int,
        tasksCountToday: Int = 0,
        billsDueTodayCount: Int = 0,
        shoppingItemsPendingCount: Int = 0
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val config = CustomizationRepository(context).configFlow.value
        val notificationId = 7799

        val todayEpoch = DateCalendarUtils.todayEpochDay()
        var triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (triggerAtMillis <= System.currentTimeMillis()) {
            triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(todayEpoch + 1L, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        val summaryParts = buildList {
            add("📋 $tasksCountToday compromisso(s) na agenda")
            add("💳 $billsDueTodayCount conta(s)/fatura(s) vencendo hoje")
            add("🛒 $shoppingItemsPendingCount item(ns) pendente(s) no Mural da Família")
        }

        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "☀️ Bom dia! Resumo Matinal Omni")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                summaryParts.joinToString(" • ")
            )
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, config.billReminderSoundUri)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, config.billVibrateEnabled)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelMorningSummary(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val notificationId = 7799
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showImmediateNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        soundUriString: String = "",
        vibrate: Boolean = true,
        isBillReminder: Boolean = false,
        openGlucoseSlot: String = ""
    ) {
        val channelId = ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openGlucoseSlot.isNotBlank()) {
                putExtra(OmniHomeWidgetProvider.EXTRA_WIDGET_ACTION, OmniHomeWidgetProvider.WIDGET_ACTION_OPEN_GLUCOSE)
                putExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT, openGlucoseSlot)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            action = TaskAndBillAlarmReceiver.ACTION_STOP_ALARM
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            action = TaskAndBillAlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(TaskAndBillAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, title)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_SOUND_URI, soundUriString)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_VIBRATE, vibrate)
            putExtra(TaskAndBillAlarmReceiver.EXTRA_IS_BILL, isBillReminder)
            if (openGlucoseSlot.isNotBlank()) {
                putExtra(TaskAndBillAlarmReceiver.EXTRA_OPEN_GLUCOSE_SLOT, openGlucoseSlot)
            }
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)

        if (openGlucoseSlot.isNotBlank()) {
            builder.addAction(
                android.R.drawable.ic_input_add,
                "Registrar Glicemia",
                contentPendingIntent
            )
        }

        builder
            .addAction(
                android.R.drawable.ic_media_pause,
                "Parar Alarme",
                stopPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                "Soneca (5 min)",
                snoozePendingIntent
            )

        if (vibrate) {
            builder.setVibrate(longArrayOf(0L, 350L, 150L, 350L))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
