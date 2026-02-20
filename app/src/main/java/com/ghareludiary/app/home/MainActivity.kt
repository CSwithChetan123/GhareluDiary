package com.ghareludiary.app.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ghareludiary.app.category.CategoryActivity
import com.ghareludiary.app.databinding.ActivityMainBinding
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.MonthlySummary
import com.ghareludiary.app.report.ReportActivity
import com.ghareludiary.app.settings.SettingsActivity
import com.ghareludiary.app.utils.NotificationHelper
import com.ghareludiary.app.utils.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
        viewModel.initializeUserProfile()
        setUpObservers()
        setUpClickListener()
        checkAndCleanFirebase()

        NotificationHelper.createNotificationChannels(this)
        NotificationScheduler.scheduleAllNotifications(this)
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

    }

    private fun checkAndCleanFirebase(){
        val pref = getSharedPreferences("my_prefs", MODE_PRIVATE)
        val hasCleanedFirebase = pref.getBoolean("firebase_cleaned", false)
        if (!hasCleanedFirebase) {
            cleanFirebaseDublicate()
        }
    }

    private fun getCurrentMonthYear(): String{
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        return dateFormat.format(calendar.time)
    }

    private fun cleanFirebaseDublicate(){
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val currentMonthYear = viewModel.currentMonthYear.value ?: return@launch

                val categories = listOf("MILK", "WATER", "MAID", "COOK", "DRIVER", "GARDENER")
                for (category in categories) {
                    val snapshot = firestore
                        .collection("users")
                        .document("userId")
                        .collection("entries")
                        .whereEqualTo("category", category)
                        .whereEqualTo("monthYear", currentMonthYear)
                        .get()
                        .await()

                    if(snapshot.documents.isEmpty()) continue
                    val groupByDate = snapshot.documents.groupBy { doc ->
                        val timeStamp = doc.getLong("date")
                        normalizeDate(timeStamp)
                    }

                    groupByDate.forEach { (normalizeDate, doc) ->
                        if(doc.size > 1){
                            val sorted = doc.sortedBy { it.getLong("date") }
                            sorted.drop(1).forEach { doc ->
                                doc.reference.delete()
                            }
                        }

                    }
                }

                getSharedPreferences("my_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firebase_cleaned", true)
                    .apply()

                viewModel.loadMonthlySummary()
            }
            catch (e: Exception){

            }
        }
    }

    private fun normalizeDate(timeStamp: Long?): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeStamp ?: 0
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun formattedDate(timeStamp: Long?): String? {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timeStamp)
    }

    private fun setUpObservers(){
        viewModel.userProfile.observe(this){ profile ->
            if(profile != null && profile.name.isNotEmpty()){
                binding.welcomeName.text = profile.name.uppercase()
            }
        }

        viewModel.currentMonthYear.observe(this){ monthYear ->
            binding.tvMonthYear.text = monthYear
        }

        viewModel.monthlySummary.observe(this) { summary ->
            updateMonthlySummaryUi(summary)
        }

    }

private fun updateMonthlySummaryUi(summary: MonthlySummary?) {
    summary?.categoryStates[CategoryType.MILK].let { stats ->
        binding.tvMilkEntries.text = "${stats?.entryCount}" + if(stats?.entryCount == 1) " entry" else " entries"
        binding.tvMilkAmount.text = "${stats?.totalQuantity}" + (if(stats?.totalAmount == 1.0) " liter" else " liters")
    }

    summary?.categoryStates[CategoryType.WATER].let { stats ->
        binding.tvWaterEntries.text = "${stats?.entryCount}" + if(stats?.entryCount == 1) " entry" else " entries"
        binding.tvWaterCount.text = "${stats?.totalQuantity}" + (if(stats?.totalAmount == 1.0) " can" else " cans")
    }

    summary?.categoryStates[CategoryType.MAID].let { stats ->
        binding.tvMaidDays.text = "${stats?.entryCount} days"
    }

    summary?.categoryStates[CategoryType.COOK].let { stats ->
        binding.tvCookDays.text = "${stats?.entryCount} days"
    }

    summary?.categoryStates[CategoryType.DRIVER].let { stats ->
        binding.tvDriverDays.text = "${stats?.entryCount} days"
    }

    summary?.categoryStates[CategoryType.GARDENER].let { stats ->
        binding.tvGardenerVisits.text = "${stats?.entryCount} visits"
    }
}

private fun setUpClickListener() {
    binding.btnReport.setOnClickListener {
        val intent = Intent(this, ReportActivity::class.java)
        startActivity(intent)
    }

    binding.btnSettings.setOnClickListener {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    val currentMonthYear = viewModel.currentMonthYear.value ?: return

    binding.cardMilk.setOnClickListener {
        openCategory(CategoryType.MILK, currentMonthYear)
    }

    binding.cardWater.setOnClickListener {
        openCategory(CategoryType.WATER, currentMonthYear)
    }

    binding.cardMaid.setOnClickListener {
        openCategory(CategoryType.MAID, currentMonthYear)
    }

    binding.cardCook.setOnClickListener {
        openCategory(CategoryType.COOK, currentMonthYear)
    }

    binding.cardDriver.setOnClickListener {
        openCategory(CategoryType.DRIVER, currentMonthYear)
    }

    binding.cardGardener.setOnClickListener {
        openCategory(CategoryType.GARDENER, currentMonthYear)
    }
}
    private fun openCategory(categoryType: CategoryType, monthYear: String){
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("categoryType", categoryType.name)
        intent.putExtra("monthYear", monthYear)
        startActivity(intent)
    }


    override fun onResume() {
        super.onResume()
        viewModel.loadMonthlySummary()
    }
}
