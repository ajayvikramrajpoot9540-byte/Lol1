package com.example.data.model

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "",
    val type: String = "general", // "order", "shop", "admin", "promo", "alert", "delivery"
    val targetRole: String = "all", // "all", "customer", "shopkeeper", "admin"
    val targetId: String = "", // e.g. userId or shopId or "admin"
    val isRead: Boolean = false,
    val actionRoute: String? = null
)
