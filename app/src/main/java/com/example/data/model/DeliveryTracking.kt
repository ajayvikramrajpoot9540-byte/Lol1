package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Live delivery tracking record reflecting real-time GPS coordinates,
 * 6-stage progression, and assigned delivery personnel metadata.
 */
@IgnoreExtraProperties
data class DeliveryTracking(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerDeliveryAddress: String = "",
    val customerLatitude: Double = 25.2922, // Mahoba delivery point
    val customerLongitude: Double = 79.8718,

    val shopId: String = "",
    val shopName: String = "",
    val shopAddress: String = "",
    val shopLatitude: Double = 25.2934, // Mahoba shop point
    val shopLongitude: Double = 79.8724,

    val deliveryManId: String = "",
    val deliveryManName: String = "",
    val deliveryManPhone: String = "",
    val deliveryManVehicle: String = "Hero Splendor • UP 95 AB 4122",
    val deliveryManPhotoUrl: String = "",

    // Exactly the 6 stages in order:
    // "order_confirmed", "preparing_order", "ready_for_pickup", "picked_up", "on_the_way", "delivered"
    val orderStatus: String = "order_confirmed",

    // Actual live GPS coordinates of assigned delivery personnel (null when not yet acquired)
    val actualLatitude: Double? = null,
    val actualLongitude: Double? = null,
    val actualAccuracyMeters: Float? = null,
    val isGpsActive: Boolean = false,
    val locationErrorMessage: String? = null,

    // Dependable routing and ETA values (computed only from verified coordinates)
    val remainingDistanceKm: Double? = null,
    val estimatedArrivalMinutes: Int? = null,

    // Real timestamps for reached delivery stages
    val stageTimestamps: Map<String, String> = emptyMap(),

    @ServerTimestamp val lastLocationUpdate: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
