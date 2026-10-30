package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartItem
import com.example.data.model.DeliveryConfig
import com.example.data.model.DistanceTier
import com.example.data.model.FreeDeliveryPolicy
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.UserProfile
import com.example.data.util.DeliveryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeliveryChargeManagementTest {

    private lateinit var context: Context
    private lateinit var repository: ApnaBazRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ApnaBazRepository(context)
    }

    private fun setProfile(loginId: String, name: String, role: String, email: String, isVerifiedAdmin: Boolean) {
        val field = ApnaBazRepository::class.java.getDeclaredField("_currentProfile")
        field.isAccessible = true
        val stateFlow = field.get(repository) as kotlinx.coroutines.flow.MutableStateFlow<UserProfile>
        stateFlow.value = UserProfile(
            id = "user_$loginId",
            name = name,
            phone = "+91 98765 00000",
            email = email,
            loginId = loginId,
            role = role,
            isAuthenticated = true,
            isVerifiedAdmin = isVerifiedAdmin
        )
    }

    @Test
    fun testEqualMainAdminPermissionsForPranjalAndChanchal() {
        // 1. Pranjal Rajpoot (PR12)
        setProfile(
            loginId = "PR12",
            name = "Pranjal Rajpoot",
            role = "MAIN_ADMIN",
            email = "pranjal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        assertTrue("Pranjal Rajpoot must be recognized as authorized Main Admin", repository.isAuthorizedMainAdmin())

        val pranjalConfig = repository.deliveryConfig.value.copy(
            minOrderThreshold = 250.0
        )
        val prResult = repository.updateGlobalDeliveryConfig(pranjalConfig)
        assertTrue("Pranjal Rajpoot can successfully update global delivery settings", prResult.isSuccess)
        assertEquals(250.0, repository.deliveryConfig.value.minOrderThreshold, 0.01)
        assertEquals("PR12", repository.deliveryConfig.value.lastUpdatedByAdminId)
        assertEquals("Pranjal Rajpoot", repository.deliveryConfig.value.lastUpdatedByAdminName)

        // 2. Chanchal Didi (CD12)
        setProfile(
            loginId = "CD12",
            name = "Chanchal Didi",
            role = "MAIN_ADMIN",
            email = "chanchal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        assertTrue("Chanchal Didi must have equal Main Admin authority", repository.isAuthorizedMainAdmin())

        val chanchalConfig = repository.deliveryConfig.value.copy(
            minOrderThreshold = 220.0
        )
        val cdResult = repository.updateGlobalDeliveryConfig(chanchalConfig)
        assertTrue("Chanchal Didi can successfully update global delivery settings with equal permissions", cdResult.isSuccess)
        assertEquals(220.0, repository.deliveryConfig.value.minOrderThreshold, 0.01)
        assertEquals("CD12", repository.deliveryConfig.value.lastUpdatedByAdminId)
        assertEquals("Chanchal Didi", repository.deliveryConfig.value.lastUpdatedByAdminName)
    }

    @Test
    fun testNonAdminAccessDenied() {
        // 1. Customer Attempt
        setProfile(
            loginId = "cust_rahul",
            name = "Rahul Sharma",
            role = "CUSTOMER",
            email = "rahul@example.com",
            isVerifiedAdmin = false
        )
        assertFalse("Customer must not be authorized as Main Admin", repository.isAuthorizedMainAdmin())
        val custResult = repository.updateGlobalDeliveryConfig(repository.deliveryConfig.value.copy(minOrderThreshold = 100.0))
        assertTrue("Customer request must be rejected", custResult.isFailure)
        assertTrue("Exception must be SecurityException", custResult.exceptionOrNull() is SecurityException)

        // 2. Shopkeeper Attempt
        setProfile(
            loginId = "SK_SHARMA",
            name = "Ramesh Sharma",
            role = "SHOPKEEPER",
            email = "sharma.store@flashdrop.local",
            isVerifiedAdmin = false
        )
        assertFalse("Shopkeeper must not be authorized as Main Admin", repository.isAuthorizedMainAdmin())
        val skResult = repository.updateGlobalDeliveryConfig(repository.deliveryConfig.value.copy(minOrderThreshold = 150.0))
        assertTrue("Shopkeeper request must be rejected", skResult.isFailure)
        assertTrue("Exception must be SecurityException", skResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testDefaultDistanceTiersAndRates() {
        val config = repository.deliveryConfig.value
        assertEquals("Default max delivery distance should be 15 km", 15.0, config.maxDeliveryDistanceKm, 0.01)
        assertEquals(4, config.distanceTiers.size)

        // Tier 1: 0 - 2 km -> ₹10
        val t1 = config.distanceTiers[0]
        assertEquals(0.0, t1.minDistanceKm, 0.01)
        assertEquals(2.0, t1.maxDistanceKm, 0.01)
        assertEquals(10.0, t1.charge, 0.01)

        // Tier 2: >2 - 5 km -> ₹20
        val t2 = config.distanceTiers[1]
        assertEquals(2.0, t2.minDistanceKm, 0.01)
        assertEquals(5.0, t2.maxDistanceKm, 0.01)
        assertEquals(20.0, t2.charge, 0.01)

        // Tier 3: >5 - 10 km -> ₹40
        val t3 = config.distanceTiers[2]
        assertEquals(5.0, t3.minDistanceKm, 0.01)
        assertEquals(10.0, t3.maxDistanceKm, 0.01)
        assertEquals(40.0, t3.charge, 0.01)

        // Tier 4: >10 km -> ₹70
        val t4 = config.distanceTiers[3]
        assertEquals(10.0, t4.minDistanceKm, 0.01)
        assertEquals(15.0, t4.maxDistanceKm, 0.01)
        assertEquals(70.0, t4.charge, 0.01)
    }

    @Test
    fun testConfigurationValidationRejectsInvalidSettings() {
        // Negative charge
        val negativeTiers = listOf(
            DistanceTier(0.0, 2.0, -10.0, "0-2 km"),
            DistanceTier(2.0, 5.0, 20.0, "2-5 km"),
            DistanceTier(5.0, 10.0, 40.0, "5-10 km"),
            DistanceTier(10.0, 15.0, 70.0, "10-15 km")
        )
        val negativeConfig = DeliveryConfig(distanceTiers = negativeTiers)
        val valRes1 = DeliveryCalculator.validateConfiguration(negativeConfig)
        assertTrue("Negative delivery charges must be rejected", valRes1.isFailure)

        // Non-contiguous / overlapping distance tiers
        val brokenTiers = listOf(
            DistanceTier(0.0, 3.0, 10.0, "0-3 km"),
            DistanceTier(2.0, 5.0, 20.0, "2-5 km"), // overlapping
            DistanceTier(5.0, 10.0, 40.0, "5-10 km"),
            DistanceTier(10.0, 15.0, 70.0, "10-15 km")
        )
        val brokenConfig = DeliveryConfig(distanceTiers = brokenTiers)
        val valRes2 = DeliveryCalculator.validateConfiguration(brokenConfig)
        assertTrue("Overlapping tiers must be rejected", valRes2.isFailure)

        // Invalid max distance
        val invalidMaxConfig = DeliveryConfig(maxDeliveryDistanceKm = 0.5)
        val valRes3 = DeliveryCalculator.validateConfiguration(invalidMaxConfig)
        assertTrue("Max distance smaller than first tier must be rejected", valRes3.isFailure)
    }

    @Test
    fun testThreeFreeDeliveryPolicies() {
        val p1 = Product(id = "p1", shopId = "sharma_general", name = "Rice", price = 150.0, inStock = true)
        val p2 = Product(id = "p2", shopId = "sharma_general", name = "Atta", price = 100.0, inStock = true)

        val shops = listOf(
            Shop(id = "sharma_general", name = "Sharma General Store", distance = "1.2 km")
        )

        // Policy 1: MINIMUM_ORDER (Threshold ₹199)
        val minOrderConfig = DeliveryConfig(
            activePolicy = FreeDeliveryPolicy.MINIMUM_ORDER.name,
            minOrderThreshold = 199.0
        )

        // Subtotal ₹150 (< ₹199) -> charges apply (1.2 km = ₹10)
        val billUnder = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(p1, 1)),
            config = minOrderConfig,
            shops = shops
        )
        assertFalse("Under threshold order should not get free delivery", billUnder.isFreeDeliveryEligible)
        assertEquals(10.0, billUnder.netDeliveryFee, 0.01)
        assertEquals(0.0, billUnder.totalDeliveryDiscount, 0.01)

        // Subtotal ₹250 (>= ₹199) -> Free delivery
        val billOver = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(p1, 1), CartItem(p2, 1)),
            config = minOrderConfig,
            shops = shops
        )
        assertTrue("Order over threshold should get free delivery", billOver.isFreeDeliveryEligible)
        assertEquals(0.0, billOver.netDeliveryFee, 0.01)
        assertEquals(10.0, billOver.totalDeliveryDiscount, 0.01)

        // Policy 2: NO_FREE_DELIVERY
        val noFreeConfig = DeliveryConfig(
            activePolicy = FreeDeliveryPolicy.NO_FREE_DELIVERY.name,
            minOrderThreshold = 199.0
        )
        val billNoFree = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(p1, 1), CartItem(p2, 1)), // ₹250 subtotal
            config = noFreeConfig,
            shops = shops
        )
        assertFalse("No-free-delivery mode must never grant free delivery", billNoFree.isFreeDeliveryEligible)
        assertEquals(10.0, billNoFree.netDeliveryFee, 0.01)
        assertEquals(0.0, billNoFree.totalDeliveryDiscount, 0.01)

        // Policy 3: MANUAL (Switch ON vs OFF)
        val manualOnConfig = DeliveryConfig(
            activePolicy = FreeDeliveryPolicy.MANUAL.name,
            isManualFreeDeliveryOn = true
        )
        val billManualOn = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(p1, 1)), // ₹150 subtotal
            config = manualOnConfig,
            shops = shops
        )
        assertTrue("Manual ON must grant free delivery", billManualOn.isFreeDeliveryEligible)
        assertEquals(0.0, billManualOn.netDeliveryFee, 0.01)

        val manualOffConfig = DeliveryConfig(
            activePolicy = FreeDeliveryPolicy.MANUAL.name,
            isManualFreeDeliveryOn = false
        )
        val billManualOff = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(p1, 1)),
            config = manualOffConfig,
            shops = shops
        )
        assertFalse("Manual OFF must charge standard distance fees", billManualOff.isFreeDeliveryEligible)
        assertEquals(10.0, billManualOff.netDeliveryFee, 0.01)
    }

    @Test
    fun testMultiShopDeliveryCalculationAndDuplicatePrevention() {
        val p1 = Product(id = "p1", shopId = "sharma_general", name = "Atta", price = 50.0, inStock = true)
        val p2 = Product(id = "p2", shopId = "sharma_general", name = "Rice", price = 60.0, inStock = true)
        val p3 = Product(id = "p3", shopId = "hemu_printing", name = "Banner", price = 80.0, inStock = true)

        val shops = listOf(
            Shop(id = "sharma_general", name = "Sharma General Store", distance = "1.2 km"), // Tier 1: ₹10
            Shop(id = "hemu_printing", name = "Hemu Printing Shop", distance = "3.5 km")     // Tier 2: ₹20
        )

        val config = DeliveryConfig(
            activePolicy = FreeDeliveryPolicy.NO_FREE_DELIVERY.name
        )

        // Cart contains 2 items from Sharma General and 1 item from Hemu Printing
        val cartItems = listOf(
            CartItem(p1, 2),
            CartItem(p2, 1),
            CartItem(p3, 1)
        )

        val bill = DeliveryCalculator.calculateBill(
            cartItems = cartItems,
            config = config,
            shops = shops
        )

        assertEquals("Should have exactly 2 distinct shops", 2, bill.shopDeliveries.size)

        val sharmaBreakdown = bill.shopDeliveries.find { it.shopId == "sharma_general" }
        assertNotNull(sharmaBreakdown)
        assertEquals("Sharma delivery fee should be ₹10 despite having 2 distinct products", 10.0, sharmaBreakdown?.finalDeliveryCharge ?: 0.0, 0.01)

        val hemuBreakdown = bill.shopDeliveries.find { it.shopId == "hemu_printing" }
        assertNotNull(hemuBreakdown)
        assertEquals("Hemu delivery fee for 3.5 km should be ₹20", 20.0, hemuBreakdown?.finalDeliveryCharge ?: 0.0, 0.01)

        // Total delivery before discount: 10 + 20 = 30
        assertEquals(30.0, bill.totalDeliveryBeforeDiscount, 0.01)
        assertEquals(30.0, bill.netDeliveryFee, 0.01)
    }

    @Test
    fun testMaxDistanceRestrictionBlocksCheckout() {
        val pFar = Product(id = "p_far", shopId = "far_store", name = "Far Product", price = 100.0, inStock = true)
        val shops = listOf(
            Shop(id = "far_store", name = "Far Outpost", distance = "18.5 km")
        )

        val config = DeliveryConfig(maxDeliveryDistanceKm = 15.0)

        val bill = DeliveryCalculator.calculateBill(
            cartItems = listOf(CartItem(pFar, 1)),
            config = config,
            shops = shops
        )

        assertFalse("Checkout should be blocked if any shop exceeds max permitted distance", bill.isCheckoutAllowed)
        assertNotNull("Reason must be specified", bill.checkoutBlockedReason)
        assertTrue("Reason should mention exceeded limit", bill.checkoutBlockedReason!!.contains("exceeds the maximum permitted"))
    }

    @Test
    fun testImmutableBillingSnapshotPreservation() {
        setProfile(
            loginId = "cust_priya",
            name = "Priya Rajput",
            role = "CUSTOMER",
            email = "priya@example.com",
            isVerifiedAdmin = false
        )

        // Initial settings: default Tier 1 is ₹10, min order ₹199
        val p1 = Product(id = "prod_1", shopId = "sharma_general", name = "Cooking Oil", price = 250.0, inStock = true)
        repository.addToCart(p1)

        val initialBill = repository.calculateDeliveryBill()
        assertTrue("₹250 order should qualify for free delivery under initial settings", initialBill.isFreeDeliveryEligible)
        assertEquals(0.0, initialBill.netDeliveryFee, 0.01)

        val placedOrder = repository.placeOrder(
            deliveryMethod = "shop_delivery",
            paymentMethod = "cash_on_delivery",
            address = "Kacheri Road",
            locality = "Mahoba",
            pinCode = "210427",
            customerName = "Priya Rajput",
            customerPhone = "+91 98765 43210"
        )

        val savedSnapshot = placedOrder.billingSnapshot
        assertNotNull("Placed order must have an immutable billing snapshot", savedSnapshot)
        assertEquals(0.0, savedSnapshot!!.netDeliveryFee, 0.01)
        assertEquals(10.0, savedSnapshot.totalDeliveryDiscount, 0.01)
        assertEquals("MINIMUM_ORDER", savedSnapshot.appliedPolicy)
        val originalVersion = savedSnapshot.configVersion

        // Now Admin updates delivery settings later: increases tier 1 to ₹30 and switches policy to NO_FREE_DELIVERY
        setProfile(
            loginId = "PR12",
            name = "Pranjal Rajpoot",
            role = "MAIN_ADMIN",
            email = "pranjal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        val updatedConfig = repository.deliveryConfig.value.copy(
            activePolicy = FreeDeliveryPolicy.NO_FREE_DELIVERY.name,
            distanceTiers = listOf(
                DistanceTier(0.0, 2.0, 30.0, "0 - 2 km"),
                DistanceTier(2.0, 5.0, 50.0, "2 - 5 km"),
                DistanceTier(5.0, 10.0, 70.0, "5 - 10 km"),
                DistanceTier(10.0, 15.0, 100.0, "10 - 15 km")
            )
        )
        repository.updateGlobalDeliveryConfig(updatedConfig)

        // Historical order must retain its original billing snapshot
        val historicalOrder = repository.orders.value.find { it.id == placedOrder.id }
        assertNotNull(historicalOrder)
        assertEquals("Historical order total amount must never change when settings are updated later", placedOrder.totalAmount, historicalOrder!!.totalAmount, 0.01)
        assertEquals("Historical order delivery fee must remain unchanged", 0.0, historicalOrder.billingSnapshot?.netDeliveryFee ?: -1.0, 0.01)
        assertEquals("Historical order config version must remain the original version", originalVersion, historicalOrder.billingSnapshot?.configVersion)
        assertEquals("Historical order policy must remain MINIMUM_ORDER", "MINIMUM_ORDER", historicalOrder.billingSnapshot?.appliedPolicy)
    }
}
