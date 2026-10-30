package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ShopkeeperAccount(
    @DocumentId val id: String = "",
    val loginId: String = "", // e.g. "SK_SHARMA"
    val shopkeeperName: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val phone: String = "",
    val email: String = "",
    val gstNumber: String = "",
    val password: String = "password123",
    val passwordHash: String = "",
    val isActive: Boolean = true,
    val status: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED", "SUSPENDED"
    val rejectionReason: String = "",
    val suspensionReason: String = "",
    val assignedBy: String = "PR12", // Main Admin who provisioned
    val createdAt: String = "2026-10-19"
)
