package com.ghareludiary.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "entries")
@IgnoreExtraProperties
data class Entry(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("category")
    val category: String = "",

    @PropertyName("date")
    val date: Long = 0,

    @PropertyName("monthYear")
    val monthYear: String? = "",

    @PropertyName("quantity")
    val quantity: Double = 0.0,

    @PropertyName("amount")
    val amount: Double = 0.0,

    @PropertyName("remark")
    val remark: String? = "",

    @PropertyName("firebaseId")
    val firebaseId: String? = null,

    @PropertyName("isSynced")
    val isSynced: Boolean = false,

    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
){
    constructor() : this(
        id = 0,
        userId = "",
        category = "",
        date = 0L,
        monthYear = "",
        quantity = 0.0,
        amount = 0.0,
        remark = "",
        firebaseId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(Date(date))
    }
}
