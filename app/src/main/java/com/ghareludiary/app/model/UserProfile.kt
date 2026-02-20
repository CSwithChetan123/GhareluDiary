package com.ghareludiary.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "user_profiles")
@IgnoreExtraProperties
data class UserProfile(
    @PrimaryKey
    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("name")
    val name: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
){
    constructor() : this(
        userId = "",
        name = "",
        email = "",
        createdAt = System.currentTimeMillis()
    )
}
