package com.ghareludiary.app.worker

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghareludiary.app.local.AppDatabase
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyReminderWorker"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("REMINDER_TYPE") ?: return Result.failure()

        return try {
            when (reminderType) {
                "MORNING" -> {
                    Log.d(TAG, "Triggering morning reminder")
                    NotificationHelper.showMorningReminder(applicationContext)
                }
                "EVENING" -> {
                    Log.d(TAG, "Triggering evening reminder")
                    NotificationHelper.showEveningReminder(applicationContext)
                }
                "MISSING_CHECK" -> {
                    Log.d(TAG, "Checking missing entries")
                    checkMissingEntries()
                }
                "WEEKLY_SUMMARY" -> {
                    Log.d(TAG, "Generating weekly summary")
                    generateWeeklySummary()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork: ${e.message}", e)
            Result.retry()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun checkMissingEntries() {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.entryDao

            // Get current user ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not logged in, skipping missing entries check")
                return
            }

            // Get today's date
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            // Format: "Oct 2025" (matches your database format)
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val monthYear = dateFormat.format(Date())

            Log.d(TAG, "Checking entries for month: $monthYear, userId: $userId")

            // Get all entries for this month - NOW WITH BOTH PARAMETERS
            val entries = try {
                dao.getEntriesForMonth(userId, monthYear).first()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting entries: ${e.message}")
                emptyList()
            }

            Log.d(TAG, "Found ${entries.size} entries for this month")

            // Check which categories are missing for today
            val todayEntries = entries.filter { entry ->
                val entryDate = Calendar.getInstance().apply { timeInMillis = entry.date }
                entryDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            }

            Log.d(TAG, "Found ${todayEntries.size} entries for today")

            val recordedCategories = todayEntries.map { it.category }.toSet()
            val allCategories = CategoryType.entries.map { it.name }
            val missingCategories = allCategories.filter { it !in recordedCategories }

            Log.d(TAG, "Missing categories: $missingCategories")

            if (missingCategories.isNotEmpty()) {
                val displayNames = missingCategories.mapNotNull { categoryName ->
                    try {
                        CategoryType.valueOf(categoryName).displayName
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting category: $categoryName", e)
                        null
                    }
                }

                if (displayNames.isNotEmpty()) {
                    NotificationHelper.showMissingEntryAlert(
                        applicationContext,
                        displayNames
                    )
                    Log.d(TAG, "Sent missing entry notification for: $displayNames")
                }
            } else {
                Log.d(TAG, "All categories recorded for today!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkMissingEntries: ${e.message}", e)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun generateWeeklySummary() {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.entryDao

            // Get current user ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not logged in, skipping weekly summary")
                return
            }

            // Get last 7 days entries
            val calendar = Calendar.getInstance()
            val endDate = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val startDate = calendar.timeInMillis

            // Format: "Oct 2025" (matches your database format)
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val monthYear = dateFormat.format(Date())

            Log.d(TAG, "Generating summary for month: $monthYear, userId: $userId")
            Log.d(TAG, "Date range: ${Date(startDate)} to ${Date(endDate)}")

            // NOW WITH BOTH PARAMETERS
            val entries = try {
                dao.getEntriesForMonth(userId, monthYear).first()
                    .filter { it.date in startDate..endDate && it.amount > 0 }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting entries for summary: ${e.message}")
                emptyList()
            }

            Log.d(TAG, "Found ${entries.size} entries for weekly summary")

            if (entries.isEmpty()) {
                Log.d(TAG, "No entries to summarize")
                return
            }

            // Calculate total and breakdown
            val totalSpent = entries.sumOf { it.amount }
            val summary = entries.groupBy { it.category }
                .mapValues { (_, categoryEntries) ->
                    val count = categoryEntries.size
                    val amount = categoryEntries.sumOf { it.amount }
                    "$count days (₹${"%.2f".format(amount)})"
                }

            Log.d(TAG, "Total spent: ₹$totalSpent")
            Log.d(TAG, "Summary: $summary")

            NotificationHelper.showWeeklySummary(
                applicationContext,
                totalSpent,
                summary
            )
            Log.d(TAG, "Sent weekly summary notification")
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateWeeklySummary: ${e.message}", e)
        }
    }
}