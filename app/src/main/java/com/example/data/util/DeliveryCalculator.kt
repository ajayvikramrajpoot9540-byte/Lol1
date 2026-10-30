package com.example.data.util

import com.example.data.model.CartItem
import com.example.data.model.DeliveryConfig
import com.example.data.model.DistanceTier
import com.example.data.model.OrderBillingSnapshot
import com.example.data.model.Shop
import com.example.data.model.ShopDeliveryBreakdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Result of comprehensive customer bill & delivery charge calculation.
 */
data class DeliveryBillResult(
    val merchandiseSubtotal: Double,
    val productDiscount: Double,
    val platformFee: Double,
    val shopDeliveries: List<ShopDeliveryBreakdown>,
    val totalDeliveryBeforeDiscount: Double,
    val isFreeDeliveryEligible: Boolean,
    val freeDeliveryEligibilityReason: String,
    val totalDeliveryDiscount: Double,
    val netDeliveryFee: Double,
    val finalPayableAmount: Double,
    val isCheckoutAllowed: Boolean,
    val checkoutBlockedReason: String?,
    val snapshot: OrderBillingSnapshot
)

object DeliveryCalculator {

    /**
     * Parse or resolve verified distance in kilometers for a given shop.
     * Uses verified shop distance metadata from Mahoba store records.
     */
    fun resolveShopDistanceKm(shop: Shop?): Double {
        if (shop == null) return 1.2
        val cleaned = shop.distance.replace("km", "").replace("KM", "").trim()
        val parsed = cleaned.toDoubleOrNull()
        return if (parsed != null && parsed > 0.0) parsed else 1.2
    }

    /**
     * Find matching distance tier for a specific distance.
     * Boundaries:
     * - Tier 1: 0 <= d <= 2.0 km
     * - Upper Tiers: tier.minDistanceKm < d <= tier.maxDistanceKm
     */
    fun findMatchingTier(distanceKm: Double, tiers: List<DistanceTier>): DistanceTier {
        if (tiers.isEmpty()) {
            return DistanceTier(0.0, 2.0, 10.0, "0 – 2 km")
        }
        for (tier in tiers) {
            val matches = if (tier.minDistanceKm <= 0.0) {
                distanceKm >= 0.0 && distanceKm <= tier.maxDistanceKm
            } else {
                distanceKm > tier.minDistanceKm && distanceKm <= tier.maxDistanceKm
            }
            if (matches) return tier
        }
        // If distance exceeds the highest configured tier, use the highest tier rate
        return tiers.last()
    }

    /**
     * Core delivery and billing calculation engine.
     * Evaluates multi-shop items, active free delivery policy, distance rates, and discount allocation.
     */
    fun calculateBill(
        cartItems: List<CartItem>,
        shops: List<Shop>,
        config: DeliveryConfig,
        deliveryMethod: String = "shop_delivery"
    ): DeliveryBillResult {
        val merchandiseSubtotal = cartItems.sumOf { it.totalPrice }
        val hasPrintingOrder = cartItems.any {
            it.printingSpec != null ||
            it.product.category.equals("Printing Press", ignoreCase = true) ||
            it.product.shopId == "hemu_printing"
        }
        val platformFee = if (hasPrintingOrder) 19.0 else 0.0
        val productDiscount = if (merchandiseSubtotal >= 300.0) 10.0 else 0.0
        val effectiveMerchandise = (merchandiseSubtotal - productDiscount).coerceAtLeast(0.0)

        // Customer pickup bypasses delivery charges
        if (deliveryMethod == "customer_pickup" || cartItems.isEmpty()) {
            val netTotal = (merchandiseSubtotal - productDiscount + platformFee).coerceAtLeast(0.0)
            val emptySnapshot = OrderBillingSnapshot(
                configVersion = config.version,
                appliedPolicy = "CUSTOMER_PICKUP",
                policyDisplayName = "Customer Store Pickup (₹0 Delivery)",
                thresholdAmount = config.minOrderThreshold,
                manualSwitchState = config.isManualFreeDeliveryOn,
                isFreeDeliveryEligible = true,
                freeDeliveryEligibilityReason = "Self-pickup selected by customer.",
                merchandiseSubtotal = merchandiseSubtotal,
                productDiscount = productDiscount,
                platformFee = platformFee,
                shopDeliveries = emptyList(),
                totalDeliveryBeforeDiscount = 0.0,
                totalDeliveryDiscount = 0.0,
                netDeliveryFee = 0.0,
                finalPayableAmount = netTotal,
                calculationTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            )
            return DeliveryBillResult(
                merchandiseSubtotal = merchandiseSubtotal,
                productDiscount = productDiscount,
                platformFee = platformFee,
                shopDeliveries = emptyList(),
                totalDeliveryBeforeDiscount = 0.0,
                isFreeDeliveryEligible = true,
                freeDeliveryEligibilityReason = "Self-pickup selected.",
                totalDeliveryDiscount = 0.0,
                netDeliveryFee = 0.0,
                finalPayableAmount = netTotal,
                isCheckoutAllowed = true,
                checkoutBlockedReason = null,
                snapshot = emptySnapshot
            )
        }

        // Group cart items by distinct shop to calculate shop-level charges and prevent duplicate charges
        val distinctShopIds = cartItems.map { it.product.shopId }.distinct()
        var checkoutBlockedReason: String? = null
        var isCheckoutAllowed = true

        val shopBreakdowns = distinctShopIds.map { shopId ->
            val shop = shops.find { it.id == shopId }
            val shopName = shop?.name ?: cartItems.firstOrNull { it.product.shopId == shopId }?.product?.shopName ?: "Mahoba Local Shop"
            val distanceKm = resolveShopDistanceKm(shop)
            val isWithinMax = distanceKm <= config.maxDeliveryDistanceKm

            if (!isWithinMax) {
                isCheckoutAllowed = false
                checkoutBlockedReason = "Shop '$shopName' is ${"%.1f".format(distanceKm)} km away, which exceeds the maximum permitted delivery limit of ${"%.1f".format(config.maxDeliveryDistanceKm)} km."
            }

            val tier = findMatchingTier(distanceKm, config.distanceTiers)

            ShopDeliveryBreakdown(
                shopId = shopId,
                shopName = shopName,
                distanceKm = distanceKm,
                distanceRangeLabel = tier.label,
                baseDeliveryCharge = tier.charge,
                allocatedDiscount = 0.0,
                finalDeliveryCharge = tier.charge,
                isDistanceVerified = true,
                isWithinMaxLimit = isWithinMax
            )
        }

        val totalDeliveryBeforeDiscount = shopBreakdowns.sumOf { it.baseDeliveryCharge }

        // Evaluate the active Admin-Controlled Free-Delivery Policy
        var isFreeDeliveryEligible = false
        var eligibilityReason = ""

        when (config.activePolicy.uppercase()) {
            "MINIMUM_ORDER" -> {
                if (effectiveMerchandise >= config.minOrderThreshold) {
                    isFreeDeliveryEligible = true
                    eligibilityReason = "Free delivery applied: Merchandise subtotal (₹${effectiveMerchandise.toInt()}) meets minimum order requirement of ₹${config.minOrderThreshold.toInt()}."
                } else {
                    isFreeDeliveryEligible = false
                    val needed = (config.minOrderThreshold - effectiveMerchandise).toInt()
                    eligibilityReason = "Add ₹$needed more of items to qualify for Free Delivery (Minimum Order: ₹${config.minOrderThreshold.toInt()})."
                }
            }
            "NO_FREE_DELIVERY" -> {
                isFreeDeliveryEligible = false
                eligibilityReason = "Standard distance-based delivery charges apply to all orders (No Free Delivery mode active)."
            }
            "MANUAL" -> {
                if (config.isManualFreeDeliveryOn) {
                    isFreeDeliveryEligible = true
                    eligibilityReason = "Special promotional Free Delivery active, enabled by FlashDrop Admins."
                } else {
                    isFreeDeliveryEligible = false
                    eligibilityReason = "Standard distance-based delivery charges apply (Manual Free Delivery switch is currently OFF)."
                }
            }
            else -> {
                isFreeDeliveryEligible = false
                eligibilityReason = "Standard distance-based delivery charges apply."
            }
        }

        // Allocate free delivery discount across shops
        val totalDeliveryDiscount = if (isFreeDeliveryEligible) totalDeliveryBeforeDiscount else 0.0
        val finalShopBreakdowns = shopBreakdowns.map { item ->
            if (isFreeDeliveryEligible) {
                item.copy(
                    allocatedDiscount = item.baseDeliveryCharge,
                    finalDeliveryCharge = 0.0
                )
            } else {
                item.copy(
                    allocatedDiscount = 0.0,
                    finalDeliveryCharge = item.baseDeliveryCharge
                )
            }
        }

        val netDeliveryFee = (totalDeliveryBeforeDiscount - totalDeliveryDiscount).coerceAtLeast(0.0)
        val finalPayableAmount = (merchandiseSubtotal - productDiscount + platformFee + netDeliveryFee).coerceAtLeast(0.0)

        val policyDisplayName = when (config.activePolicy.uppercase()) {
            "MINIMUM_ORDER" -> "Minimum Order Threshold (₹${config.minOrderThreshold.toInt()})"
            "NO_FREE_DELIVERY" -> "No Free Delivery (Distance Rates Apply)"
            "MANUAL" -> if (config.isManualFreeDeliveryOn) "Manual Free Delivery (ON)" else "Manual Free Delivery (OFF)"
            else -> "Standard Distance Rates"
        }

        val snapshot = OrderBillingSnapshot(
            configVersion = config.version,
            appliedPolicy = config.activePolicy,
            policyDisplayName = policyDisplayName,
            thresholdAmount = config.minOrderThreshold,
            manualSwitchState = config.isManualFreeDeliveryOn,
            isFreeDeliveryEligible = isFreeDeliveryEligible,
            freeDeliveryEligibilityReason = eligibilityReason,
            merchandiseSubtotal = merchandiseSubtotal,
            productDiscount = productDiscount,
            platformFee = platformFee,
            shopDeliveries = finalShopBreakdowns,
            totalDeliveryBeforeDiscount = totalDeliveryBeforeDiscount,
            totalDeliveryDiscount = totalDeliveryDiscount,
            netDeliveryFee = netDeliveryFee,
            finalPayableAmount = finalPayableAmount,
            calculationTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
            currency = "INR"
        )

        return DeliveryBillResult(
            merchandiseSubtotal = merchandiseSubtotal,
            productDiscount = productDiscount,
            platformFee = platformFee,
            shopDeliveries = finalShopBreakdowns,
            totalDeliveryBeforeDiscount = totalDeliveryBeforeDiscount,
            isFreeDeliveryEligible = isFreeDeliveryEligible,
            freeDeliveryEligibilityReason = eligibilityReason,
            totalDeliveryDiscount = totalDeliveryDiscount,
            netDeliveryFee = netDeliveryFee,
            finalPayableAmount = finalPayableAmount,
            isCheckoutAllowed = isCheckoutAllowed,
            checkoutBlockedReason = checkoutBlockedReason,
            snapshot = snapshot
        )
    }

    /**
     * Validates new delivery configuration updates submitted by Main Admins.
     * Rejects negative charges, negative distances, non-contiguous tiers, and invalid limits.
     */
    fun validateConfiguration(config: DeliveryConfig): Result<Unit> {
        if (config.maxDeliveryDistanceKm < 1.0 || config.maxDeliveryDistanceKm > 50.0) {
            return Result.failure(IllegalArgumentException("Maximum delivery distance must be between 1 km and 50 km."))
        }
        if (config.minOrderThreshold < 0.0) {
            return Result.failure(IllegalArgumentException("Minimum order threshold cannot be negative."))
        }
        if (config.activePolicy !in listOf("MINIMUM_ORDER", "NO_FREE_DELIVERY", "MANUAL")) {
            return Result.failure(IllegalArgumentException("Unsupported free delivery policy: '${config.activePolicy}'."))
        }
        if (config.distanceTiers.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one distance tier is required."))
        }

        var expectedMin = 0.0
        for ((index, tier) in config.distanceTiers.withIndex()) {
            if (tier.charge < 0.0) {
                return Result.failure(IllegalArgumentException("Delivery charge cannot be negative in tier '${tier.label}'."))
            }
            if (tier.minDistanceKm < 0.0 || tier.maxDistanceKm <= tier.minDistanceKm) {
                return Result.failure(IllegalArgumentException("Invalid distance bounds in tier '${tier.label}'. Max distance must be greater than min distance."))
            }
            if (index > 0 && abs(tier.minDistanceKm - expectedMin) > 0.001) {
                return Result.failure(IllegalArgumentException("Distance tiers must be contiguous without gaps or overlaps. Tier '${tier.label}' starts at ${tier.minDistanceKm} km instead of expected ${expectedMin} km."))
            }
            expectedMin = tier.maxDistanceKm
        }

        return Result.success(Unit)
    }
}
