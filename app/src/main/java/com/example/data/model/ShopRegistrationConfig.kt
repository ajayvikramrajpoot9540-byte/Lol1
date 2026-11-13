package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Global Registration Configuration controlled by Main Admins.
 * Controls:
 * 1. Whether new shopkeeper applicants go through the 3-Step Verification system or single-page registration.
 * 2. Local Customer Sign-Up Daily Quota (default: max 100 customer accounts per day) and enable/disable switches.
 *
 * Stored in Firestore at: appSettings/shopRegistration
 */
@IgnoreExtraProperties
data class ShopRegistrationConfig(
    @DocumentId val id: String = "shopRegistration",
    val threeStepEnabled: Boolean = true, // 3-step verification process enabled by default
    val customerLocalSignupEnabled: Boolean = true, // Whether local customer sign-up is enabled
    val customerDailySignupLimitEnabled: Boolean = true, // Enforce per-day customer sign-up cap
    val maxLocalSignupsPerDay: Int = 100, // Default: 100 customers per day can create a local account
    val todayDateKey: String = "", // Format: yyyy-MM-dd
    val todayLocalSignupCount: Int = 0, // Number of local customer accounts created today
    val lastUpdatedByAdminId: String = "PR12",
    val lastUpdatedByAdminName: String = "Main Admin",
    val lastUpdatedAtFormatted: String = "29 Oct 2026, 10:00 AM",
    val version: Long = 1,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
