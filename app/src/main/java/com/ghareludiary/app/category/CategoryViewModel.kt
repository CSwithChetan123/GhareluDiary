package com.ghareludiary.app.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ghareludiary.app.local.AppDatabase
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.Entry
import com.ghareludiary.app.remote.FirebaseManager
import com.ghareludiary.app.repository.GhareluRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale



class CategoryViewModel(application: Application): AndroidViewModel(application){
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

    private val _category = MutableLiveData<CategoryType>()
    private val _monthYear = MutableLiveData<String?>()
    private val _refreshTrigger = MutableStateFlow(0L)
    private lateinit var _entriesFlow: Flow<List<Entry>>

    fun init(category: CategoryType, monthYear: String?){
        _category.value = category
        _monthYear.value = monthYear

        _entriesFlow = _refreshTrigger.flatMapLatest {
            repository.getEntriesByCategory(category, monthYear)
        }
        _refreshTrigger.value = System.currentTimeMillis()

    }

    fun getEntriesWithBlanks(): Flow<List<Entry>>{
        return _entriesFlow.map { entries ->
            val monthYear = _monthYear.value ?: return@map entries
            val category = _category.value ?: return@map entries
            val allDaysEntries = mutableListOf<Entry>()

            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(monthYear) ?: return@map entries

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val today = Calendar.getInstance()
            val currentDay = if(isSameMonth(calendar, today)){
                today.get(Calendar.DAY_OF_MONTH)
            }
            else{
                calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            for(day in 1..currentDay){
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val dayTimeStamp = calendar.timeInMillis

                val existingEntry = entries.find { entry ->
                    isSameDay(entry.date, dayTimeStamp)
                }

                if(existingEntry!= null){
                    allDaysEntries.add(existingEntry)
                }
                else{
                    allDaysEntries.add(Entry(
                        id = 0,
                        userId = auth.currentUser?.uid ?: "",
                        category = category.name,
                        date = dayTimeStamp,
                        monthYear = monthYear,
                        quantity = 0.0,
                        amount = 0.0,
                        remark = "",
                        firebaseId = null,
                        isSynced = false
                    ))
                }



            }

            allDaysEntries.sortedByDescending { it.date }


        }
    }

    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean{
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)

    }

    private fun isSameDay(timeStamp1: Long, timeStamp2: Long): Boolean{

        val cal1 = Calendar.getInstance().apply {
            timeInMillis = timeStamp1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = timeStamp2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun saveEntry(
        quantity: Double = 0.0,
        amount: Double = 0.0,
        remark: String? = "",
        date: Long? = System.currentTimeMillis()
    ){
        val category = _category.value ?: return
        val monthYear = _monthYear.value?: return

        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.timeInMillis = date
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val normalizedDate = calendar.timeInMillis

        viewModelScope.launch {
            val entry = Entry(
                userId = auth.currentUser?.uid ?: "",
                category = category.name,
                date = normalizedDate,
                monthYear = monthYear,
                quantity = quantity,
                amount = amount,
                remark = remark
            )

            val result = repository.saveEntry(entry)
            if(result.isSuccess){
                _refreshTrigger.value = System.currentTimeMillis()
            }

        }
    }

    fun updateEntry(entry: Entry){
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entry.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val normalizedDate = calendar.timeInMillis

            val updatedEntry = entry.copy(
                date = normalizedDate
            )
            val result = repository.updateEntry(updatedEntry)
            if(result.isSuccess){
                _refreshTrigger.value = System.currentTimeMillis()
            }

        }
    }

    fun deleteEntry(entry: Entry){
        viewModelScope.launch {
            val result = repository.deleteEntry(entry)
                _refreshTrigger.value = System.currentTimeMillis()
        }
    }

}