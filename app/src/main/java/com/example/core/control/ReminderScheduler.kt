package com.example.core.control

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.notification.AlarmReceiver

/**
 * Handles real Android system alarm scheduling with AlarmManager.
 * Strictly verifies whether exact alarm permission is available and falls back safely.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_RECURRENCE = "extra_recurrence"
    }

    /**
     * Schedules a real Android alarm for the given timestamp.
     * Returns true if scheduling was accepted by AlarmManager, false otherwise.
     */
    fun scheduleAlarm(
        reminderId: String,
        title: String,
        description: String,
        scheduledAtMs: Long,
        notificationId: Int,
        recurrence: String = "ONCE"
    ): Boolean {
        if (alarmManager == null) return false
        if (scheduledAtMs <= System.currentTimeMillis()) return false

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_RECURRENCE, recurrence)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAtMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAtMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduledAtMs,
                    pendingIntent
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cancels an existing scheduled alarm.
     */
    fun cancelAlarm(notificationId: Int) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
