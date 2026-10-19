package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.control.ReminderScheduler
import com.example.data.local.NilaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Android AlarmManager for exact reminders.
 * Runs on device wake and notifies the user with high priority.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Nila Reminder"
        val description = intent.getStringExtra(ReminderScheduler.EXTRA_DESCRIPTION) ?: ""
        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, reminderId.hashCode())
        val recurrence = intent.getStringExtra(ReminderScheduler.EXTRA_RECURRENCE) ?: "ONCE"

        val notificationHelper = NilaNotificationHelper(context)
        notificationHelper.showReminderNotification(
            notificationId = notificationId,
            title = title,
            content = if (description.isNotBlank()) description else "Time for your scheduled task.",
            isRecurring = recurrence != "ONCE"
        )

        // Reschedule next recurrence if DAILY or WEEKLY
        if (recurrence == "DAILY" || recurrence == "WEEKLY") {
            val scheduler = ReminderScheduler(context)
            val intervalMs = if (recurrence == "DAILY") 24 * 60 * 60 * 1000L else 7 * 24 * 60 * 60 * 1000L
            val nextTime = System.currentTimeMillis() + intervalMs
            scheduler.scheduleAlarm(reminderId, title, description, nextTime, notificationId, recurrence)
        }

        // Update reminder state in Room database asynchronously
        val db = NilaDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // If ONCE, mark as COMPLETED
                if (recurrence == "ONCE") {
                    // Update any matching reminder
                }
            } catch (_: Exception) {}
        }
    }
}
