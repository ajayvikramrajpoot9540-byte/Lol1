package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Global Shop Registration Configuration controlled by Main Admins.
 * Controls whether new shopkeeper applicants go through the 3-Step Verification
 * system or the existing normal single-page registration.
 *
 * Stored in Firestore at: appSettings/shopRegistration/threeStepEnabled
 */
@IgnoreExtraProperties
data class ShopRegistrationConfig(
    @DocumentId val id: String = "shopRegistration",
    val threeStepEnabled: Boolean = false, // Safe default fallback: false
    val lastUpdatedByAdminId: String = "PR12",
    val lastUpdatedByAdminName: String = "Main Admin",
    val lastUpdatedAtFormatted: String = "29 Oct 2026, 10:00 AM",
    val version: Long = 1,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
