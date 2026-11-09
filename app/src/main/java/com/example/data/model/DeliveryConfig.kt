package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Distance-based delivery rate tier.
 * Defines distance bounds in kilometers and the associated base delivery charge in INR (₹).
 */
@IgnoreExtraProperties
data class DistanceTier(
    val minDistanceKm: Double = 0.0,
    val maxDistanceKm: Double = 2.0,
    val charge: Double = 9.0,
    val label: String = "0 – 2 km"
)

/**
 * The three configurable free-delivery modes supported in FlashDrop:
 * 1. MINIMUM_ORDER: Free delivery when merchandise subtotal reaches minimum threshold (default ₹199)
 * 2. NO_FREE_DELIVERY: Always applies distance-based delivery charges on all orders
 * 3. MANUAL: Master administrator ON/OFF toggle switch
 */
enum class FreeDeliveryPolicy(val code: String, val displayName: String, val description: String) {
    MINIMUM_ORDER(
        code = "MINIMUM_ORDER",
        displayName = "Minimum Order Threshold",
        description = "Free delivery when merchandise subtotal reaches minimum threshold (default ₹599)"
    ),
    NO_FREE_DELIVERY(
        code = "NO_FREE_DELIVERY",
        displayName = "No Free Delivery",
        description = "Standard distance-based charges always apply to all orders"
    ),
    MANUAL(
        code = "MANUAL",
        displayName = "Manual Admin Switch",
        description = "Controlled manually by Main Admins via ON/OFF toggle switch"
    )
}

/**
 * Global Delivery Configuration controlled exclusively by Main Admins (Pranjal Rajpoot & Chanchal Didi).
 */
@IgnoreExtraProperties
data class DeliveryConfig(
    @DocumentId val id: String = "default",

    // Distance-based rate tiers (Default: 0-2 km: ₹10, >2-5 km: ₹20, >5-10 km: ₹40, >10 km: ₹70)
    val distanceTiers: List<DistanceTier> = listOf(
        DistanceTier(minDistanceKm = 0.0, maxDistanceKm = 2.0, charge = 9.0, label = "0 – 2 km"),
        DistanceTier(minDistanceKm = 2.0, maxDistanceKm = 5.0, charge = 19.0, label = "2 – 5 km"),
        DistanceTier(minDistanceKm = 5.0, maxDistanceKm = 10.0, charge = 39.0, label = "5 – 10 km"),
        DistanceTier(minDistanceKm = 10.0, maxDistanceKm = 15.0, charge = 69.0, label = "> 10 km")
    ),

    // Maximum permitted delivery distance (Default: 15.0 km)
    val maxDeliveryDistanceKm: Double = 15.0,

    // Active Free-Delivery Policy ("MINIMUM_ORDER", "NO_FREE_DELIVERY", "MANUAL")
    val activePolicy: String = "MINIMUM_ORDER",

    // Threshold for MINIMUM_ORDER policy (Default: ₹199.0)
    val minOrderThreshold: Double = 199.0,

    // State of manual free delivery switch when MANUAL policy is active
    val isManualFreeDeliveryOn: Boolean = false,

    // Admin audit trail and versioning
    val version: Long = 1L,
    val lastUpdatedByAdminId: String = "PR12",
    val lastUpdatedByAdminName: String = "Pranjal Rajpoot",
    val lastUpdatedAtFormatted: String = "System Initial Setup",

    // Backward-compatibility fields
    val defaultDeliveryFee: Double = 20.0,
    val freeDeliveryThreshold: Double = 199.0,
    val minOrderAmount: Double = 50.0,
    val supportedAreas: List<String> = listOf(
        "Subhash Chowk",
        "Alha Chowk",
        "Gandhi Nagar",
        "Bhatipura",
        "Civil Lines",
        "Kabrai Road",
        "Rajnagar Gate",
        "Station Road",
        "Baradari Marg"
    ),

    @ServerTimestamp val updatedAt: Timestamp? = null
)
