package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Shop(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Grocery",
    val rating: Double = 4.5,
    val ratingCount: Int = 85,
    val distance: String = "1.2 km",
    val deliveryTime: String = "20-30 mins",
    val deliveryAvailable: Boolean = true,
    val deliveryFee: Double = 20.0,
    val minOrder: Double = 100.0,
    val isOpen: Boolean = true,
    val status: String = "approved", // "approved", "pending", "suspended", "rejected"
    val ownerName: String = "",
    val gstNumber: String = "",
    val password: String = "",
    val passwordHash: String = "",
    val rejectionReason: String = "",
    val suspensionReason: String = "",
    val description: String = "",
    val address: String = "Mahoba, Uttar Pradesh",
    val phone: String = "+91 98765 43210",
    val imageUrl: String = "",
    val bannerUrl: String = "",
    val shopkeeperPhotoUrl: String = "",
    val shopkeeperPhotoChangeApprovedByAdmin: Boolean = false,
    val shopkeeperPhotoChangeRequested: Boolean = false,
    val openingHours: String = "08:00 AM - 10:00 PM",
    val pendingGstNumber: String = "",
    val gstApprovalStatus: String = "verified", // "verified", "pending_approval"
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
