package com.ghareludiary.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ghareludiary.app.R
import com.ghareludiary.app.home.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID_DAILY = "daily_reminders"
    private const val CHANNEL_ID_ALERTS = "entry_alerts"
    private const val CHANNEL_ID_SUMMARY = "weekly_summary"

    private const val NOTIFICATION_ID_MORNING = 1001
    private const val NOTIFICATION_ID_EVENING = 1002
    private const val NOTIFICATION_ID_MISSING = 1003
    private const val NOTIFICATION_ID_WEEKLY = 1004

    // Create notification channels (Android 8.0+)
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_DAILY,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily entry reminders"
                },
                NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Entry Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Missing entry alerts"
                },
                NotificationChannel(
                    CHANNEL_ID_SUMMARY,
                    "Weekly Summary",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Weekly reports and summaries"
                }
            )

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { notificationManager?.createNotificationChannel(it) }
        }
    }

    // 1. Morning Reminder
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMorningReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(R.mipmap.ic_launcher)  // Using your app icon
            .setContentTitle("🌅 Good Morning!")
            .setContentText("Don't forget to record today's entries")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Did Milk arrive? Did Maid come? Tap to record your morning entries."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
    }

    // 2. Evening Reminder
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showEveningReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 Evening Check-in")
            .setContentText("Did you record today's attendance?")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Quick reminder: Record attendance for Maid, Cook, and other services before you forget!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
    }

    // 3. Missing Entry Alert
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMissingEntryAlert(context: Context, missingCategories: List<String>) {
        if (missingCategories.isEmpty()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val categoriesText = missingCategories.joinToString(", ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Missing Entries")
            .setContentText("You haven't recorded: $categoriesText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Missing entries for today:\n• ${missingCategories.joinToString("\n• ")}\n\nTap to add them now!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSING, notification)
    }

    // 4. Weekly Summary
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWeeklySummary(
        context: Context,
        totalSpent: Double,
        summary: Map<String, String>
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summaryText = summary.entries.joinToString("\n") {
            "• ${it.key}: ${it.value}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📊 Weekly Summary")
            .setContentText("Total spent this week: ₹${"%.2f".format(totalSpent)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Week in Review:\n$summaryText\n\nTotal: ₹${"%.2f".format(totalSpent)}"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY, notification)
    }

    // 5. Payment Reminder (Monthly)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMonthlyPaymentReminder(
        context: Context,
        monthlyTotal: Double,
        breakdown: Map<String, Double>
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val breakdownText = breakdown.entries.joinToString("\n") {
            "• ${it.key}: ₹${"%.2f".format(it.value)}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💰 Monthly Payment Summary")
            .setContentText("Total expenses: ₹${"%.2f".format(monthlyTotal)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Monthly Breakdown:\n$breakdownText\n\nTotal: ₹${"%.2f".format(monthlyTotal)}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1005, notification)
    }

    // 6. Streak Celebration
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showStreakNotification(context: Context, streakDays: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎉 $streakDays-Day Streak!")
            .setContentText("Great job! You've been tracking consistently")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Amazing! You've recorded entries for $streakDays days in a row. Keep up the excellent work! 🔥"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1006, notification)
    }

    // 7. Test Notification (for testing)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showTestNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ Test Notification")
            .setContentText("If you see this, notifications are working perfectly!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(9999, notification)
    }
}