package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * Manages official Android Notification Channels and high-priority alerts
 * for reminders, timers, study goals, and system safety confirmations.
 */
class NilaNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_REMINDERS = "nila_reminders_channel"
        const val CHANNEL_STUDY = "nila_study_channel"
        const val CHANNEL_TIMERS = "nila_timers_channel"

        const val ACTION_DISMISS = "com.example.action.DISMISS_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Reminders Channel (High Importance, Sound, Vibration)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Nila Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for scheduled tasks, alarms, and personal reminders."
                enableVibration(true)
                enableLights(true)
            }

            // 2. Study & Focus Channel
            val studyChannel = NotificationChannel(
                CHANNEL_STUDY,
                "Study & Adaptive Learning",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for study sessions, flashcard reviews, and quiz goals."
            }

            // 3. Timers & Pomodoro Channel
            val timersChannel = NotificationChannel(
                CHANNEL_TIMERS,
                "Timers & Pomodoro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for completed pomodoro focus cycles and countdown timers."
                enableVibration(true)
            }

            manager.createNotificationChannel(remindersChannel)
            manager.createNotificationChannel(studyChannel)
            manager.createNotificationChannel(timersChannel)
        }
    }

    fun showReminderNotification(
        notificationId: Int,
        title: String,
        content: String,
        isRecurring: Boolean = false
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "control")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isRecurring) {
            builder.setSubText("Recurring Reminder")
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not granted
        }
    }

    fun showTimerCompleteNotification(
        timerTitle: String,
        durationMinutes: Int
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "control")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Timer Finished")
            .setContentText("$timerTitle ($durationMinutes min) is complete.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(9999, builder.build())
        } catch (_: SecurityException) {}
    }
}
