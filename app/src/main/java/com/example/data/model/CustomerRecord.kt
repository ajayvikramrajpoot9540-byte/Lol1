package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CustomerOrderSummary(
    val orderId: String = "",
    val orderNumber: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val itemsSummary: String = "",
    val itemsCount: Int = 1,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "UPI", // "UPI" or "COD"
    val paymentStatus: String = "Paid", // "Paid", "Pending", "Collected", "Refunded", "Failed"
    val orderStatus: String = "delivered", // "pending", "accepted", "preparing", "out_for_delivery", "delivered", "cancelled", "refunded"
    val orderDate: String = "",
    val deliveryAddress: String = ""
) {
    val status: String
        get() = orderStatus
    val createdAt: String
        get() = orderDate
}

@IgnoreExtraProperties
data class CustomerRecord(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val primaryAddress: String = "Subhash Chowk, Chhatarpur Road",
    val locality: String = "Alha Chowk",
    val city: String = "Mahoba",
    val state: String = "Uttar Pradesh",
    val pinCode: String = "210427",
    val savedAddresses: List<DeliveryAddress> = emptyList(),
    val authMethod: String = "Registered Account", // "Google Sign-In", "Mobile & Password", "Registered Account"
    val accountStatus: String = "Active", // "Active", "VIP", "Suspended"
    val isVerified: Boolean = true,
    val joinedDate: String = "Oct 2026",
    val lastActive: String = "Today",
    val lastOrderDate: String = "Recently",
    val totalOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
    val cancelledOrdersCount: Int = 0,
    val totalSpentAmount: Double = 0.0,
    val upiSpentAmount: Double = 0.0,
    val codSpentAmount: Double = 0.0,
    val preferredPaymentMethod: String = "UPI",
    val favoriteShopName: String = "Sharma General Store",
    val recentOrders: List<CustomerOrderSummary> = emptyList(),
    val adminNotes: String = ""
) {
    val averageOrderValue: Double
        get() = if (totalOrdersCount > 0) totalSpentAmount / totalOrdersCount else 0.0

    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_VIP = "VIP"
        const val STATUS_SUSPENDED = "Suspended"
    }
}
