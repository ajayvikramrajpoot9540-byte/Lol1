package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class OrderItemRecord(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val unit: String = "",
    val isPrintingOrder: Boolean = false,
    val printingDetails: String = "",
    val fileNames: List<String> = emptyList(),
    val fileUrls: List<String> = emptyList(),
    val printInstructions: String = "",
    val uploadedFileCount: Int = 0
)

/**
 * Individual shop delivery charge breakdown in multi-shop or single-shop orders.
 */
@IgnoreExtraProperties
data class ShopDeliveryBreakdown(
    val shopId: String = "",
    val shopName: String = "",
    val distanceKm: Double = 0.0,
    val distanceRangeLabel: String = "",
    val baseDeliveryCharge: Double = 0.0,
    val allocatedDiscount: Double = 0.0,
    val finalDeliveryCharge: Double = 0.0,
    val isDistanceVerified: Boolean = true,
    val isWithinMaxLimit: Boolean = true
)

/**
 * Immutable billing snapshot captured with every confirmed order.
 * Ensures historical order details, receipts, and billing reports never mutate
 * when Main Admins update global delivery settings later.
 */
@IgnoreExtraProperties
data class OrderBillingSnapshot(
    val configVersion: Long = 1L,
    val appliedPolicy: String = "MINIMUM_ORDER",
    val policyDisplayName: String = "Minimum Order Threshold (₹199)",
    val thresholdAmount: Double = 199.0,
    val manualSwitchState: Boolean = false,
    val isFreeDeliveryEligible: Boolean = false,
    val freeDeliveryEligibilityReason: String = "",
    val merchandiseSubtotal: Double = 0.0,
    val productDiscount: Double = 0.0,
    val platformFee: Double = 0.0,
    val shopDeliveries: List<ShopDeliveryBreakdown> = emptyList(),
    val totalDeliveryBeforeDiscount: Double = 0.0,
    val totalDeliveryDiscount: Double = 0.0,
    val netDeliveryFee: Double = 0.0,
    val finalPayableAmount: Double = 0.0,
    val calculationTimestamp: String = "",
    val currency: String = "INR"
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val locality: String = "",
    val pinCode: String = "210427",
    val shopId: String = "",
    val shopName: String = "",
    val items: List<OrderItemRecord> = emptyList(),
    val itemsCount: Int = 0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val platformFee: Double = 0.0,
    val discount: Double = 10.0,
    val totalAmount: Double = 0.0,
    val deliveryMethod: String = "shop_delivery", // "shop_delivery" or "customer_pickup"
    val paymentMethod: String = "cash_on_delivery", // "cash_on_delivery" or "upi"
    val paymentStatus: String = "pending",
    val transactionId: String = "",
    val paymentReference: String = "",
    val expectedDeliverySlot: String = "Today, 4:30 PM - 6:30 PM",
    val isNearClosingTime: Boolean = false,
    val lateNightDecision: String = "", // "PENDING", "DELIVER_TODAY", "DELIVER_TOMORROW"
    val confirmedDeliveryTime: String = "",
    val scheduledForTomorrow: Boolean = false,
    val status: String = "pending", // "pending", "accepted", "preparing", "out_for_delivery", "delivered", "cancelled", "rejected"
    val statusNote: String = "Order received by shopkeeper",
    // Immutable snapshot of applied delivery charges & billing calculation
    val billingSnapshot: OrderBillingSnapshot? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
