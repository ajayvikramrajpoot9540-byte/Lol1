package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val name: String = "Guest",
    val email: String = "",
    val phone: String = "",
    val role: String = "customer", // "customer", "shopkeeper", "MAIN_ADMIN"
    val loginId: String = "",
    val ownerTitle: String = "", // "Owner 1", "Owner 2"
    val shopId: String = "",
    val shopName: String = "",
    val isVerifiedAdmin: Boolean = false,
    val isVerifiedShopkeeper: Boolean = false,
    val isAuthenticated: Boolean = false,
    val photoUrl: String = "",
    val address: String = "Subhash Chowk, Chhatarpur Road",
    val locality: String = "Alha Chowk",
    val pinCode: String = "210427",
    val city: String = "Mahoba",
    val state: String = "Uttar Pradesh",
    val savedAddresses: List<DeliveryAddress> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
