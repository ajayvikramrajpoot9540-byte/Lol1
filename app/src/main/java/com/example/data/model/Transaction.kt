package com.example.data.model

data class Transaction(
    val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "UPI", // "UPI", "COD"
    val status: String = "Paid", // "Paid", "Collected", "Pending", "Failed", "Refunded"
    val timestamp: String = "",
    val timeOnly: String = "",
    val providerTxnId: String = "",
    val referenceId: String = "",
    val verificationProof: String = "Verified by FlashDrop Gateway",
    val failureReason: String = "",
    val daysAgo: Int = 0, // 0..1 = 1 Day, <=7 = 7 Days, <=30 = 1 Month, <=180 = 6 Months, <=365 = 1 Year
    val itemsSummary: String = "",
    val deliveryFee: Double = 19.0,
    val platformCommission: Double = 0.0,
    val discountSubsidy: Double = 0.0,
    val shopkeeperPayoutAmount: Double = 0.0,
    val shopkeeperSettlementStatus: String = "Settled", // "Settled", "Pending Settlement"
    val onlineProblemCount: Int = 0,
    val onlineProblemType: String = ""
) {
    val isOnlinePayment: Boolean
        get() = paymentMethod.equals("UPI", ignoreCase = true) ||
                paymentMethod.equals("ONLINE", ignoreCase = true) ||
                paymentMethod.equals("CARD", ignoreCase = true)

    val isCodPayment: Boolean
        get() = paymentMethod.equals("COD", ignoreCase = true) ||
                paymentMethod.equals("CASH_ON_DELIVERY", ignoreCase = true)

    val isPendingPayment: Boolean
        get() = status.equals("Pending", ignoreCase = true) ||
                shopkeeperSettlementStatus.equals("Pending Settlement", ignoreCase = true)

    val hasOnlinePaymentProblem: Boolean
        get() = isOnlinePayment && (
                status.equals("Failed", ignoreCase = true) ||
                status.equals("Refunded", ignoreCase = true) ||
                status.equals("Pending", ignoreCase = true) ||
                onlineProblemCount > 0 ||
                failureReason.isNotBlank()
            )

    val effectivePlatformCommission: Double
        get() = if (platformCommission > 0.0) platformCommission else (amount * 0.08)

    val effectiveShopkeeperPayout: Double
        get() = if (shopkeeperPayoutAmount > 0.0) {
            shopkeeperPayoutAmount
        } else {
            (amount - effectivePlatformCommission - deliveryFee).coerceAtLeast(amount * 0.82)
        }

    /**
     * Net platform profit (+) or loss (-) on this transaction:
     * Revenue = Platform commission + Delivery fee collected (on successful orders)
     * Cost/Loss = Promotional/free-delivery subsidy + Gateway/Refund loss on failed/refunded orders
     */
    val netPlatformProfitOrLoss: Double
        get() {
            return when (status.lowercase()) {
                "failed" -> -(discountSubsidy.coerceAtLeast(12.0)) // Gateway retry & operational handling loss
                "refunded" -> -(discountSubsidy + 18.0) // Refund processing & reverse logistics loss
                else -> effectivePlatformCommission + deliveryFee - discountSubsidy
            }
        }
}
