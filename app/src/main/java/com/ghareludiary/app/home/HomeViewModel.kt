package com.ghareludiary.app.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ghareludiary.app.local.AppDatabase
import com.ghareludiary.app.model.MonthlySummary
import com.ghareludiary.app.model.UserProfile
import com.ghareludiary.app.remote.FirebaseManager
import com.ghareludiary.app.repository.GhareluRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale


class HomeViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _monthlySummary = MutableLiveData<MonthlySummary?>()
    val monthlySummary: LiveData<MonthlySummary?> = _monthlySummary

    private val _currentMonthYear = MutableLiveData<String>()
    val currentMonthYear: LiveData<String> = _currentMonthYear

    init {
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val currentMonthYear = dateFormat.format(Date(System.currentTimeMillis()))
        _currentMonthYear.value = currentMonthYear

        loadUserProfile()
        autoSyncOnStartup()
        loadMonthlySummary()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            repository.getUserProfile().collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    private fun autoSyncOnStartup() {
        viewModelScope.launch {
            try {
                val monthYear = _currentMonthYear.value ?: ""
                val result = repository.syncFromFirebase(monthYear)
                if (result.isSuccess) {
                    loadMonthlySummary()
                }
            } catch (e: Exception) {

            }

        }
    }

    fun loadMonthlySummary() {
        viewModelScope.launch {
            try {
                val monthYear = _currentMonthYear.value ?: ""
                val summary = repository.getMonthlySummary(monthYear)
                _monthlySummary.value = summary
            } catch (e: Exception) {

            }
        }
    }

    fun initializeUserProfile() {
        viewModelScope.launch {
            try {
                val existingProfile = repository.getUserProfile().first()
                if (existingProfile == null || existingProfile.name.isEmpty()) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val userName = currentUser.displayName ?: ""
                        val firstName = if(userName.isNotEmpty()){
                            userName.split(" ")[0]
                        } else {
                            currentUser.email?.substringBefore("@")?.uppercase()
                        }
                        if(firstName?.isNotEmpty() ?: false){
                            saveUserProfile(firstName)
                        }
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    fun saveUserProfile(name: String) {
        viewModelScope.launch{
            val result = repository.saveUserProfile(UserProfile(name = name))
            if(result.isSuccess){
                loadUserProfile()
            }
        }

    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }


}