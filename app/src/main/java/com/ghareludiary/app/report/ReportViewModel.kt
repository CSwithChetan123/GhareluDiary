package com.ghareludiary.app.report

import android.app.Application
import android.icu.util.Calendar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghareludiary.app.local.AppDatabase
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.Entry
import com.ghareludiary.app.remote.FirebaseManager
import com.ghareludiary.app.repository.GhareluRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReportViewModel(application: Application): AndroidViewModel(application) {
    private val repository: GhareluRepository
    private val auth = FirebaseAuth.getInstance()

    init {
        val database = AppDatabase.getDatabase(application)
        val firebaseManager = FirebaseManager(auth)
        repository = GhareluRepository(
            database.entryDao,
            database.userProfileDao,
            firebaseManager
        )
    }

    private val _reportData = MutableStateFlow<ReportData>(ReportData(emptyList(), 0, 0, 0.0, 0.0))
    val reportData: StateFlow<ReportData> = _reportData

    fun loadReport(
        startDate: Long,
        endDate: Long,
        category: CategoryType?,
        filterStatus: FilterStatus
    ){
        viewModelScope.launch {
            try {
                val reportEntries = mutableListOf<ReportEntry>()
                val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val monthYears = getMonthYearsInRange(startDate, endDate)
                val allEntries = mutableListOf<Entry>()

                for(monthYear in monthYears){
                    val entries = if(category != null){
                        repository.getEntriesByCategory(category, monthYear).first()
                    }
                    else{
                        repository.getEntriesForMonth(monthYear).first()
                    }
                    allEntries.addAll(entries)
                }

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = startDate

                while (calendar.timeInMillis <= endDate){
                    val currentDate = calendar.timeInMillis
                    val currentCal = Calendar.getInstance()
                    currentCal.timeInMillis = currentDate

                    currentCal.set(Calendar.HOUR_OF_DAY, 0)
                    currentCal.set(Calendar.MINUTE, 0)
                    currentCal.set(Calendar.SECOND, 0)
                    currentCal.set(Calendar.MILLISECOND, 0)
                    val normalizedDate = currentCal.timeInMillis

                    val entry = allEntries.find { dbEntry ->
                        val isSameDay = isSameDay(dbEntry.date, normalizedDate)
                        val isSameCategory = category == null || dbEntry.category == category.name
                        isSameDay && isSameCategory
                    }

                    if(entry != null){
                        val isNoEntry = entry.amount < 0
                        reportEntries.add(
                            ReportEntry(
                                date = normalizedDate,
                                categoryName = category?.displayName ?: "",
                                quantity = if(isNoEntry) 0.0 else entry.quantity,
                                amount = if(isNoEntry) 0.0 else entry.amount,
                                remark = entry.remark?: "",
                                isNoEntry = isNoEntry,
                                hasEntry = true
                            )

                        )
                    }
                    else{
                        reportEntries.add(
                            ReportEntry(
                                date = normalizedDate,
                                categoryName = category?.displayName ?: "",
                                quantity = 0.0,
                                amount = 0.0,
                                remark = "",
                                isNoEntry = false,
                                hasEntry = false
                            )
                        )
                    }
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                val filterEntries = when(filterStatus){
                    FilterStatus.ALL -> reportEntries
                    FilterStatus.ENTRIES_ONLY -> reportEntries.filter { it.hasEntry && !it.isNoEntry }
                    FilterStatus.NO_ENTRIES_ONLY -> reportEntries.filter { it.isNoEntry || !it.hasEntry}

                }
                val entryCount = filterEntries.count { it.hasEntry && !it.isNoEntry }
                val noEntryCount = filterEntries.count { it.isNoEntry || !it.hasEntry }
                val totalAmount = filterEntries.filter { !it.isNoEntry && it.hasEntry }.sumOf { it.amount }
                val totalQuantity = filterEntries.filter { !it.isNoEntry && it.hasEntry }.sumOf { it.quantity }

                _reportData.value = ReportData(
                    filterEntries,
                    entryCount,
                    noEntryCount,
                    totalAmount,
                    totalQuantity
                )
            }
            catch (e: Exception){

            }

        }

    }

    private fun getMonthYearsInRange(startDate: Long, endDate: Long): List<String>{
        val monthYears = mutableSetOf<String>()
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        while (calendar.timeInMillis <= endDate){
            val monthYear = dateFormat.format(calendar.time)
            monthYears.add(monthYear)
            calendar.add(Calendar.MONTH, 1)
        }
        return monthYears.toList()
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean{
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }


}