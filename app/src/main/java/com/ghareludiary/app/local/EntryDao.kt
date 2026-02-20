package com.ghareludiary.app.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ghareludiary.app.model.Entry
import kotlinx.coroutines.flow.Flow


@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE userId = :userId AND monthYear = :monthYear ORDER BY date DESC")
    fun getEntriesForMonth(userId: String, monthYear: String): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear ORDER BY date DESC")
    fun getEntriesByCategory(userId: String, category: String, monthYear: String?): Flow<List<Entry>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear")
    suspend fun getTotalAmountByCategory(userId: String, category: String, monthYear: String): Double

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear")
    suspend fun getTotalQuantityByCategory(userId: String, category: String, monthYear: String): Double

    @Query("SELECT COUNT(*) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear AND amount >= 0")
    suspend fun getEntryCountByCategory(userId: String, category: String, monthYear: String): Int

    @Query("SELECT COUNT(*) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear AND amount < 0")
    suspend fun getNonEntryCountByCategory(userId: String, category: String, monthYear: String): Int

    @Query("SELECT COUNT(*) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear")
    suspend fun getTotalEntryCountByCategory(userId: String, category: String, monthYear: String): Int

    @Query("SELECT MAX(date) FROM entries WHERE userId = :userId AND category = :category AND monthYear = :monthYear")
    suspend fun getLastEntryDate(userId: String, category: String, monthYear: String): Long?

    @Query("SELECT DISTINCT monthYear FROM entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllMonthYears(userId: String): Flow<List<String>>

    @Query("SELECT * FROM entries WHERE userId = :userId AND isSynced = 0")
    fun getUnsyncedEntries(userId: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<Entry>)

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("DELETE FROM entries WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM entries WHERE userId = :userId")
    suspend fun deleteAllEntriesForUser(userId: String)

}