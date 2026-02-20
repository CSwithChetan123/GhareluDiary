package com.ghareludiary.app.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ghareludiary.app.worker.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAllNotifications(context: Context) {
        scheduleMorningReminder(context)
        scheduleEveningReminder(context)
        scheduleMissingEntryCheck(context)
        scheduleWeeklySummary(context)
    }

    private fun scheduleMorningReminder(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If time has passed today, schedule for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val delay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("REMINDER_TYPE" to "MORNING"))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "morning_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleEveningReminder(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)  // 8 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val delay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("REMINDER_TYPE" to "EVENING"))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "evening_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleMissingEntryCheck(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)  // 9 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val delay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("REMINDER_TYPE" to "MISSING_CHECK"))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "missing_entry_check",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleWeeklySummary(context: Context) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            // Schedule for Sunday 7 PM
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If Sunday has passed or time has passed, schedule for next Sunday
            if (before(currentTime)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val delay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("REMINDER_TYPE" to "WEEKLY_SUMMARY"))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_summary",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("morning_reminder")
        WorkManager.getInstance(context).cancelUniqueWork("evening_reminder")
        WorkManager.getInstance(context).cancelUniqueWork("missing_entry_check")
        WorkManager.getInstance(context).cancelUniqueWork("weekly_summary")
    }
}