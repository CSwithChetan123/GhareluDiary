package com.ghareludiary.app.repository

import android.icu.util.Calendar
import com.ghareludiary.app.local.EntryDao
import com.ghareludiary.app.local.UserProfileDao
import com.ghareludiary.app.model.CategoryStats
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.Entry
import com.ghareludiary.app.model.MonthlySummary
import com.ghareludiary.app.model.UserProfile
import com.ghareludiary.app.remote.FirebaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


class GhareluRepository(
    private val entryDao: EntryDao,
    private val userProfileDao: UserProfileDao,
    private val firebaseManager: FirebaseManager
) {
    private fun getUserId(): String{
        return firebaseManager.getCurrentUserId()?: ""
    }
    fun getUserProfile(): Flow<UserProfile?>{
        return userProfileDao.getUserProfile(getUserId())
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>{
        return try{
            val userId = getUserId()
            val email = firebaseManager.getCurrentUserEmail()
            val profile = UserProfile(
                userId = userId,
                name = profile.name,
                email = email?: "",
                createdAt = System.currentTimeMillis()
            )
            userProfileDao.insert(profile)
            if(firebaseManager.isUserLoggedIn()){
                firebaseManager.saveUserProfile(profile)
            }
            Result.success(Unit)
        }
        catch (e: Exception){
            Result.failure(e)
        }
    }

    fun getEntriesForMonth(monthYear: String): Flow<List<Entry>> {
        return entryDao.getEntriesForMonth(getUserId(), monthYear)
    }

    fun getEntriesByCategory(category: CategoryType, monthYear: String?): Flow<List<Entry>> {
        return entryDao.getEntriesByCategory(getUserId(), category.name, monthYear)
    }

    suspend fun getMonthlySummary(monthYear: String): MonthlySummary {
        val userId = getUserId()
        val categoryState = mutableMapOf<CategoryType, CategoryStats>()

        CategoryType.values().forEach { category ->
            val totalAmount = entryDao.getTotalAmountByCategory(userId, category.name, monthYear)
            val totalQuantity = entryDao.getTotalQuantityByCategory(userId, category.name, monthYear)
            val entryCount = entryDao.getEntryCountByCategory(userId, category.name, monthYear)
            val lastEntryDate = entryDao.getLastEntryDate(userId, category.name, monthYear)

            categoryState[category] = CategoryStats(
                categoryType = category,
                totalAmount = totalAmount,
                totalQuantity = totalQuantity,
                entryCount = entryCount,
                lastEntryDate = lastEntryDate
            )
        }

        return MonthlySummary(
            monthYear = monthYear,
            categoryStates = categoryState
        )

    }

    suspend fun saveEntry(entry: Entry): Result<Long>{
        return try {
            val userId = getUserId()
            if(userId.isEmpty()){
                return Result.failure(Exception("User not logged in"))
            }
            val entryWithUserId = entry.copy(userId = userId)
            val existingEntries = entryDao.getEntriesByCategory(userId, entry.category, entry.monthYear).first()

            val dublicate = existingEntries.find {existingEntry ->
                normalizeDate(existingEntry.date) == normalizeDate(entry.date)
            }

            if(dublicate!= null){
                return Result.failure(Exception("Entry already exists"))
            }

            val localId = entryDao.insert(entryWithUserId)
            if(firebaseManager.isUserLoggedIn()){
                val result = firebaseManager.saveEntry(entryWithUserId)
                if(result.isSuccess){
                    val firebaseId = result.getOrNull()
                    entryDao.update(entryWithUserId.copy(
                        id = localId,
                        firebaseId = firebaseId,
                        isSynced = true
                    ))
                }
            }
            Result.success(localId)

        }catch (e: Exception){
            Result.failure(e)
        }

    }

    private fun normalizeDate(timeStamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeStamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis

    }

    suspend fun updateEntry(entry: Entry): Result<Unit>{
        return try {
            entryDao.update(entry)
            if(firebaseManager.isUserLoggedIn()){
                firebaseManager.updateEntry(entry)
            }
            Result.success(Unit)
        }catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun syncFromFirebase(monthYear: String): Result<Unit>{
        return try {
            if(!firebaseManager.isUserLoggedIn()){
                return Result.failure(Exception("User not logged in"))
            }
            val entries = firebaseManager.getEntriesForMonth(monthYear).getOrNull()
            val existingEntries = entryDao.getEntriesForMonth(getUserId(), monthYear).first()

            val newEntries = entries?.filter { firebaseEntry ->
                val isDublicate = existingEntries.any{ localEntry ->

                    if(firebaseEntry.firebaseId != null && localEntry.firebaseId != null){
                        firebaseEntry.firebaseId == localEntry.firebaseId
                    }
                    else{
                        normalizeDate(firebaseEntry.date) == normalizeDate(localEntry.date) &&
                                firebaseEntry.category == localEntry.category

                    }

                }
                !isDublicate

            }
            if(newEntries != null){
                entryDao.insertAll(newEntries)
            }
            Result.success(Unit)
        }catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun syncToFirebase(): Result<Unit>{
        return try {
            if(!firebaseManager.isUserLoggedIn()) {
                return Result.failure(Exception("User not logged in"))
            }
            val unsyncedEntries = entryDao.getUnsyncedEntries(getUserId())
            firebaseManager.syncEntries(unsyncedEntries)
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entry: Entry){
        entryDao.delete(entry)
    }

    suspend fun signOut(){
        try {
            firebaseManager.signOut()
            entryDao.deleteAllForUser(getUserId())
            userProfileDao.delete(getUserId())
        } catch (e: Exception){

        }
    }


}