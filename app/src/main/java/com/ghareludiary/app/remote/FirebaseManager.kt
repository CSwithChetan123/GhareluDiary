package com.ghareludiary.app.remote

import com.ghareludiary.app.model.Entry
import com.ghareludiary.app.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseManager(auth1: FirebaseAuth) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String?{
        return auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String?{
        return auth.currentUser?.email
    }

    fun isUserLoggedIn(): Boolean{
        return auth.currentUser != null
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>{
        return try {
            firestore
                .collection("user_profiles")
                .document(profile.userId)
                .set(profile)
                .await()
            Result.success(Unit)
        }
        catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun saveEntry(entry: Entry): Result<String?>{
        return try {
            val userId: String = getCurrentUserId()?: return Result.failure(Exception("User not logged in"))
            val docRef = firestore
                .collection("users")
                .document(userId)
                .collection("entries")
                .document()
            val firebseId = docRef.id

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())

            val formattedDate = dateFormat.format(Date(entry.date))
            val formattedCreatedAt = dateTimeFormat.format(Date(entry.createdAt))
            val formattedUpdatedAt = dateTimeFormat.format(Date(entry.updatedAt))

            val status = when{
                entry.amount < 0 -> "NO"
                entry.amount == 0.0 && entry.quantity == 0.0 -> "YES (No payment)"
                entry.amount > 0 -> "YES (Paid)"
                else -> "YES"
            }

            val description = when{
                entry.amount < 0 -> "${entry.category} did not come"
                entry.quantity > 0 -> "${entry.category}: ${entry.quantity} Units, ₹${entry.amount}"
                entry.amount > 0 -> "${entry.category} came, paid ₹${entry.amount}"
                else -> "${entry.category} came (No payment)"
            }

            val data = hashMapOf<String, Any?>(
                "userId" to userId,
                "category" to entry.category,
                "amount" to entry.amount,
                "quantity" to entry.quantity,
                "date" to formattedDate,
                "monthYear" to entry.monthYear,
                "createdAt" to entry.createdAt,
                "updatedAt" to entry.updatedAt,
                "isSynced" to true,
                "firebaseId" to firebseId,
                "formattedDate" to formattedDate,
                "formattedCreatedAt" to formattedCreatedAt,
                "formattedUpdatedAt" to formattedUpdatedAt,
                "status" to status,
                "description" to description
            )
            entry.remark.let { data["remark"] = it }
            docRef.set(data).await()
            Result.success(firebseId)
        }
        catch (e: Exception){
            Result.failure(e)
        }

    }

    suspend fun updateEntry(entry: Entry): Result<Unit>{
        return try {
            val userId: String = getCurrentUserId()?: return Result.failure(Exception("User not logged in"))
            val firebaseId = entry.firebaseId?: return Result.failure(Exception("Firebase ID not found"))

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())

            val formattedDate = dateFormat.format(Date(entry.date))
            val formattedCreatedAt = dateTimeFormat.format(Date(entry.createdAt))
            val formattedUpdatedAt = dateTimeFormat.format(Date(entry.updatedAt))

            val status = when{
                entry.amount < 0 -> "NO"
                entry.amount == 0.0 && entry.quantity == 0.0 -> "YES (No payment)"
                entry.amount > 0 -> "YES (Paid)"
                else -> "YES"
            }

            val description = when{
                entry.amount < 0 -> "${entry.category} did not come"
                entry.quantity > 0 -> "${entry.category}: ${entry.quantity} Units, ₹${entry.amount}"
                entry.amount > 0 -> "${entry.category} came, paid ₹${entry.amount}"
                else -> "${entry.category} came (No payment)"
            }

            val data = hashMapOf<String, Any?>(
                "userId" to userId,
                "category" to entry.category,
                "amount" to entry.amount,
                "quantity" to entry.quantity,
                "date" to formattedDate,
                "monthYear" to entry.monthYear,
                "createdAt" to entry.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "isSynced" to true,
                "firebaseId" to firebaseId,
                "formattedDate" to formattedDate,
                "formattedCreatedAt" to formattedCreatedAt,
                "formattedUpdatedAt" to formattedUpdatedAt,
                "status" to status,
                "description" to description
            )
            entry.remark.let { data["remark"] = it }

            firestore
                .collection("users")
                .document(userId)
                .collection("entries")
                .document(firebaseId)
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun getEntriesForMonth(monthYear: String): Result<List<Entry>>{
        return try {
            val userId: String = getCurrentUserId()?: return Result.failure(Exception("User not logged in"))
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("entries")
                .whereEqualTo("monthYear", monthYear)
                .get()
                .await()

            val entries = snapshot.documents.mapNotNull { doc ->
                try {
                    Entry(
                        id = doc.getLong("id") ?: 0,
                        userId = doc.getString("userId") ?: "",
                        category = doc.getString("category") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        quantity = doc.getDouble("quantity") ?: 0.0,
                        date = doc.getLong("date") ?: 0L,
                        monthYear = doc.getString("monthYear") ?: "",
                        remark = doc.getString("remark"),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        isSynced = true,
                        firebaseId = doc.id
                    )


                } catch (e: Exception){
                    null
                }
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncEntries(entries: List<Entry>): Result<Unit>{
        return try {
            entries.forEach { entry ->
                try {
                    saveEntry(entry)
                    Result.success(Unit)
                } catch (e: Exception){
                    Result.failure(e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    fun signOut(){
        auth.signOut()
    }

}

