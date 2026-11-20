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
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CustomizationRepository
import com.example.data.DateCalendarUtils

class TaskAndBillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Lembrete Aura Finanças"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Você tem uma tarefa ou conta agendada para agora."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        val isBillReminder = intent.getBooleanExtra(EXTRA_IS_BILL, false)

        val customRepo = CustomizationRepository(context)
        val config = customRepo.configFlow.value
        val soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI)
            ?: if (isBillReminder) config.billReminderSoundUri else config.taskAlarmSoundUri
        val vibrateEnabled = intent.getBooleanExtra(
            EXTRA_VIBRATE,
            if (isBillReminder) config.billVibrateEnabled else config.taskVibrateEnabled
        )

        AlarmScheduler.ensureNotificationChannel(context)
        AlarmScheduler.playCustomRingtoneAndVibrate(
            context = context,
            soundUriString = soundUriStr,
            vibrate = vibrateEnabled
        )
        AlarmScheduler.showImmediateNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            soundUriString = soundUriStr,
            vibrate = vibrateEnabled
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_MESSAGE = "extra_alarm_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_IS_BILL = "extra_is_bill"
        const val CHANNEL_ID = "aura_financas_alarms_channel"
    }
}

object AlarmScheduler {

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                TaskAndBillAlarmReceiver.CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun playCustomRingtoneAndVibrate(
        context: Context,
        soundUriString: String,
        vibrate: Boolean
    ) {
        try {
            val uri = if (soundUriString.isNotBlank()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.play()
            }
        } catch (_: Exception) {
        }

        if (vibrate) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(450L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(450L)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun scheduleTaskAlarm(
        context: Context,
        taskId: String,
        title: String,
        category: String,
        dateEpochDay: Long,
        hour: Int,
        minute: Int
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = DateCalendarUtils.epochMillisForAlarm(dateEpochDay, hour, minute)
        val config = CustomizationRepository(context).configFlow.value

        val notificationId = ("task_$taskId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "Alarme da Agenda: $title")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Categoria: $category • Horário: ${DateCalendarUtils.formatTime(hour, minute)}"
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

        val effectiveTime = if (triggerAtMillis > System.currentTimeMillis()) {
            triggerAtMillis
        } else {
            System.currentTimeMillis() + 5_000L
        }

        return try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                effectiveTime,
                pendingIntent
            )
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

    fun scheduleSubscriptionReminder(
        context: Context,
        subscriptionId: String,
        title: String,
        amountFormatted: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = DateCalendarUtils.nextDueAlarmMillis(dueDayOfMonth, reminderDaysBefore)
        val config = CustomizationRepository(context).configFlow.value

        val notificationId = ("sub_$subscriptionId").hashCode()
        val intent = Intent(context, TaskAndBillAlarmReceiver::class.java).apply {
            putExtra(TaskAndBillAlarmReceiver.EXTRA_TITLE, "Lembrete de Conta Fixa: $title")
            putExtra(
                TaskAndBillAlarmReceiver.EXTRA_MESSAGE,
                "Valor: $amountFormatted • Vencimento todo dia $dueDayOfMonth"
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
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showImmediateNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        soundUriString: String = "",
        vibrate: Boolean = true
    ) {
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, TaskAndBillAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (soundUriString.isNotBlank()) {
            try {
                builder.setSound(Uri.parse(soundUriString))
            } catch (_: Exception) {
            }
        }
        if (vibrate) {
            builder.setVibrate(longArrayOf(0L, 350L, 150L, 350L))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
