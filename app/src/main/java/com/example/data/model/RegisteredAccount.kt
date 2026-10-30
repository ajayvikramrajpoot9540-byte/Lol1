package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RegisteredAccount(
    val id: String = "",
    val fullName: String = "",
    val normalizedName: String = "",
    val passwordHash: String = "",
    val role: String = "customer",
    val phone: String = "+91 98765 12345",
    val email: String = "customer@flashdrop.local",
    val createdAt: Long = System.currentTimeMillis()
)
