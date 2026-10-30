package com.example.data.model

data class Transaction(
    val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "UPI", // "UPI", "COD"
    val status: String = "Paid", // "Paid", "Collected", "Pending", "Failed", "Refunded"
    val timestamp: String = "",
    val timeOnly: String = "",
    val providerTxnId: String = "",
    val referenceId: String = "",
    val verificationProof: String = "Verified by FlashDrop Gateway",
    val failureReason: String = ""
)
