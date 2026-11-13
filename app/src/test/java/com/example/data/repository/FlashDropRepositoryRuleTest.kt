package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
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
class FlashDropRepositoryRuleTest {

    private lateinit var context: Context
    private lateinit var repository: FlashDropRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FlashDropRepository(context)
    }

    @Test
    fun testShopsAndProductsLoaded() {
        val shops = repository.shops.value
        assertTrue("Shops should have sample data", shops.isNotEmpty())
        val products = repository.products.value
        assertTrue("Products should have sample data", products.isNotEmpty())

        val sharmaShop = shops.find { it.id == "sharma_general" }
        assertNotNull("Sharma General Store should exist", sharmaShop)
        assertEquals("Sharma General Store", sharmaShop?.name)

        val hemuShop = shops.find { it.id == "hemu_printing" }
        assertNotNull("Hemu Printing Shop should exist", hemuShop)
        assertEquals("Hemu Printing Shop", hemuShop?.name)
        assertEquals("Printing Press", hemuShop?.category)
        assertEquals(4.8, hemuShop?.rating ?: 0.0, 0.01)
        assertEquals("Same Day Dispatch", hemuShop?.deliveryTime)

        val printingProducts = products.filter { it.shopId == "hemu_printing" }
        assertTrue("Printing products should be loaded for Hemu Printing Shop", printingProducts.isNotEmpty())
    }

    @Test
    fun testCartOperations() {
        val testProduct = Product(
            id = "test_item_1",
            shopId = "sharma_general",
            name = "Test Item",
            price = 100.0,
            inStock = true
        )

        repository.clearCart()
        assertEquals(0, repository.cartItems.value.size)

        repository.addToCart(testProduct)
        assertEquals(1, repository.cartItems.value.size)
        assertEquals(1, repository.cartItems.value.first().quantity)
        assertEquals(100.0, repository.cartItems.value.first().totalPrice, 0.01)

        repository.updateCartQuantity("test_item_1", 1)
        assertEquals(2, repository.cartItems.value.first().quantity)
        assertEquals(200.0, repository.cartItems.value.first().totalPrice, 0.01)

        repository.removeFromCart("test_item_1")
        assertEquals(0, repository.cartItems.value.size)
    }

    @Test
    fun testPlaceOrderAndStatusUpdate() {
        val product = repository.products.value.first()
        repository.clearCart()
        repository.addToCart(product)

        // Authenticate customer before placing order
        repository.loginAsCustomer("Ajay Rajput", "+91 98765 00000")

        val order = repository.placeOrder(
            deliveryMethod = "shop_delivery",
            paymentMethod = "cash_on_delivery",
            address = "Subhash Chowk",
            locality = "Subhash Chowk",
            pinCode = "210427",
            customerName = "Ajay Rajput",
            customerPhone = "+91 98765 00000"
        )

        assertNotNull("Order should be created", order)
        assertTrue("Order number should start with #FD", order.orderNumber.startsWith("#FD"))
        assertEquals("pending", order.status)
        assertEquals(0, repository.cartItems.value.size)

        // Update status to preparing and out_for_delivery
        repository.updateOrderStatus(order.id, "preparing")
        val updatedOrder = repository.orders.value.find { it.id == order.id }
        assertEquals("preparing", updatedOrder?.status)

        repository.updateOrderStatus(order.id, "delivered")
        val deliveredOrder = repository.orders.value.find { it.id == order.id }
        assertEquals("delivered", deliveredOrder?.status)
    }

    @Test
    fun testGuestBrowsingCartAndOrderBlockingAndGoogleSignInFlow() {
        // 1. A guest user can browse products and add items to the cart without signing in.
        repository.setGuestMode()
        assertFalse("Guest user must not be signed in", repository.isUserSignedIn())
        assertTrue("Guest user can browse shops", repository.shops.value.isNotEmpty())
        assertTrue("Guest user can browse products", repository.products.value.isNotEmpty())

        repository.clearCart()
        val prod = repository.products.value.first()
        repository.addToCart(prod)
        assertEquals("Guest user can add items to cart", 1, repository.cartItems.value.size)

        // 2 & 3. A guest user cannot place an order; attempting to place an order throws SecurityException("Sign in to place your order.")
        val initialOrderCount = repository.orders.value.size
        var caughtException: SecurityException? = null
        try {
            repository.placeOrder(
                deliveryMethod = "shop_delivery",
                paymentMethod = "cash_on_delivery",
                address = "Subhash Chowk",
                locality = "Alha Chowk",
                pinCode = "210427",
                customerName = "Guest",
                customerPhone = ""
            )
        } catch (e: SecurityException) {
            caughtException = e
        }
        assertNotNull("Guest order attempt must throw SecurityException", caughtException)
        assertEquals("Sign in to place your order.", caughtException?.message)
        assertEquals("No order should be created when unauthenticated", initialOrderCount, repository.orders.value.size)
        assertEquals("Cart items must remain intact when guest is prompted to sign in", 1, repository.cartItems.value.size)

        // 5. A user who cancels Google Sign-In or fails authentication stays unauthenticated and does not create an order.
        val invalidSignIn = repository.signInWithGoogleAccount(uid = "", displayName = "", email = "")
        assertTrue("Empty UID Google Sign-In must fail", invalidSignIn.isFailure)
        assertFalse("User remains unauthenticated after failed/cancelled sign-in", repository.isUserSignedIn())
        assertEquals("No order created on cancelled/failed sign-in", initialOrderCount, repository.orders.value.size)

        // 4. A user can sign in with Google and immediately place the order.
        val googleSignInResult = repository.signInWithGoogleAccount(
            uid = "google_uid_rahul_101",
            displayName = "Rahul Verma",
            email = "rahul.verma@gmail.com",
            photoUrl = "https://lh3.googleusercontent.com/a/rahul_avatar.jpg",
            phone = "+91 98765 11223"
        )
        assertTrue("Google Sign-In should succeed", googleSignInResult.isSuccess)
        assertTrue("User is now signed in", repository.isUserSignedIn())
        assertEquals("Rahul Verma", repository.currentProfile.value.name)
        assertEquals("https://lh3.googleusercontent.com/a/rahul_avatar.jpg", repository.currentProfile.value.photoUrl)

        val placedOrder = repository.placeOrder(
            deliveryMethod = "shop_delivery",
            paymentMethod = "cash_on_delivery",
            address = "Subhash Chowk",
            locality = "Alha Chowk",
            pinCode = "210427",
            customerName = repository.currentProfile.value.name,
            customerPhone = repository.currentProfile.value.phone
        )
        assertEquals("Order must be associated with authenticated user's UID", "google_uid_rahul_101", placedOrder.userId)
        assertEquals("Rahul Verma", placedOrder.customerName)
        assertEquals("Cart is cleared after successful order placement", 0, repository.cartItems.value.size)

        // 6. A signed-in user remains signed in after closing and reopening the app.
        val restartedRepository = FlashDropRepository(context)
        assertTrue("Signed-in session must persist across app restarts", restartedRepository.isUserSignedIn())
        assertEquals("google_uid_rahul_101", restartedRepository.currentProfile.value.id)
        assertEquals("Rahul Verma", restartedRepository.currentProfile.value.name)
        assertEquals("https://lh3.googleusercontent.com/a/rahul_avatar.jpg", restartedRepository.currentProfile.value.photoUrl)

        // 8 & 9. An authenticated user can view their own orders, and cannot access another user's private orders.
        val rahulVisibleOrders = repository.getOrdersForCurrentUser()
        assertTrue("Rahul must see his own order", rahulVisibleOrders.any { it.id == placedOrder.id })
        assertTrue("Rahul must only see orders matching his own UID", rahulVisibleOrders.all { it.userId == "google_uid_rahul_101" })

        // Sign in as a second user (Priya) and verify she cannot access Rahul's private order
        repository.signInWithGoogleAccount(
            uid = "google_uid_priya_202",
            displayName = "Priya Sharma",
            email = "priya.sharma@gmail.com"
        )
        val priyaVisibleOrders = repository.getOrdersForCurrentUser()
        assertFalse("Priya must not see Rahul's private order", priyaVisibleOrders.any { it.id == placedOrder.id })
        assertEquals("Priya cannot access Rahul's order by ID", null, repository.getOrderForCurrentUser(placedOrder.id))

        // 7. A signed-in user can sign out, and the next order attempt asks for sign-in again.
        repository.logout()
        assertFalse("User must be signed out after logout()", repository.isUserSignedIn())
        assertEquals("Guest", repository.currentProfile.value.name)
        assertTrue("Guest user cannot view private orders after sign out", repository.getOrdersForCurrentUser().isEmpty())

        repository.addToCart(prod)
        var postLogoutException: SecurityException? = null
        try {
            repository.placeOrder(
                deliveryMethod = "shop_delivery",
                paymentMethod = "cash_on_delivery",
                address = "Subhash Chowk",
                locality = "Alha Chowk",
                pinCode = "210427",
                customerName = "Guest",
                customerPhone = ""
            )
        } catch (e: SecurityException) {
            postLogoutException = e
        }
        assertNotNull("Next order attempt after sign-out must require sign-in again", postLogoutException)
        assertEquals("Sign in to place your order.", postLogoutException?.message)
    }

    @Test
    fun testSignedOutUserCannotBypassAuthenticationByCallingOrderCreation() {
        // Scenario 7: A signed-out user cannot bypass authentication by calling the order-creation function
        repository.logout()
        assertFalse("Repository must report unauthenticated", repository.isUserSignedIn())

        repository.clearCart()
        val prod = repository.products.value.first()
        repository.addToCart(prod)
        assertEquals(1, repository.cartItems.value.size)

        val beforeCount = repository.orders.value.size

        // Attempting to bypass by supplying client-side user metadata
        var thrown: SecurityException? = null
        try {
            repository.placeOrder(
                deliveryMethod = "shop_delivery",
                paymentMethod = "cash_on_delivery",
                address = "Subhash Chowk",
                locality = "Alha Chowk",
                pinCode = "210427",
                customerName = "Bypass Attempter",
                customerPhone = "+91 99999 00000"
            )
        } catch (e: SecurityException) {
            thrown = e
        }

        assertNotNull("Bypass attempt must be blocked by throwing SecurityException", thrown)
        assertEquals("Sign in to place your order.", thrown?.message)
        assertEquals("Order count must not increase", beforeCount, repository.orders.value.size)
        assertEquals("Cart must remain intact for user after blocked order attempt", 1, repository.cartItems.value.size)
    }

    @Test
    fun testTwoMainAdminOwnersAccessAndEquality() {
        // Main Admin 1: Pranjal Rajpoot (Login ID: PR12 / Password: PR12)
        val loginPR = repository.loginAsMainAdmin("PR12", "PR12")
        assertTrue("PR12 should login as MAIN_ADMIN", loginPR.isSuccess)
        assertTrue(repository.isMainAdmin())
        assertEquals("PR12", repository.currentProfile.value.loginId)
        assertEquals("Pranjal Rajpoot", repository.currentProfile.value.name)
        assertEquals("MAIN_ADMIN", repository.currentProfile.value.role)
        assertTrue(repository.currentProfile.value.isVerifiedAdmin)

        // Login using full name: "Pranjal Rajpoot"
        val loginPRByName = repository.loginAsMainAdmin("Pranjal Rajpoot", "PR12")
        assertTrue("Pranjal Rajpoot by name should login as MAIN_ADMIN", loginPRByName.isSuccess)
        assertEquals("Pranjal Rajpoot", repository.currentProfile.value.name)

        // Main Admin 2: Chanchal Didi (Login ID: CD12 / Password: CD12)
        val loginCD = repository.loginAsMainAdmin("CD12", "CD12")
        assertTrue("CD12 should login as MAIN_ADMIN", loginCD.isSuccess)
        assertTrue(repository.isMainAdmin())
        assertEquals("CD12", repository.currentProfile.value.loginId)
        assertEquals("Chanchal Didi", repository.currentProfile.value.name)
        assertEquals("MAIN_ADMIN", repository.currentProfile.value.role)
        assertTrue(repository.currentProfile.value.isVerifiedAdmin)

        // Login using full name: "Chanchal Didi"
        val loginCDByName = repository.loginAsMainAdmin("Chanchal Didi", "CD12")
        assertTrue("Chanchal Didi by name should login as MAIN_ADMIN", loginCDByName.isSuccess)
        assertEquals("Chanchal Didi", repository.currentProfile.value.name)

        // Both owners have equal administrative permissions:
        // Sensitive Admin Changes require mutual owner confirmation safeguard
        val pendingAction = repository.submitSensitiveAdminChange("Disable Owner PR12")
        assertTrue(repository.pendingAdminActions.value.contains(pendingAction))
    }

    @Test
    fun testMainAdminLoginRejectsIncorrectCredentials() {
        // Wrong password for PR12
        val wrongPass1 = repository.loginAsMainAdmin("PR12", "WRONG_PASS")
        assertTrue("Wrong password for PR12 should fail", wrongPass1.isFailure)
        assertEquals(
            "Invalid Login ID or password. Access restricted to authorized Main Admins.",
            wrongPass1.exceptionOrNull()?.message
        )

        // Wrong password for CD12
        val wrongPass2 = repository.loginAsMainAdmin("CD12", "WRONG_PASS")
        assertTrue("Wrong password for CD12 should fail", wrongPass2.isFailure)

        // Cross-using password (PR12 pass for CD12)
        val crossPass = repository.loginAsMainAdmin("PR12", "CD12")
        assertTrue("Cross password should fail", crossPass.isFailure)

        // Non-existent login ID
        val wrongId = repository.loginAsMainAdmin("RandomUser", "PR12")
        assertTrue("Unknown login ID should fail", wrongId.isFailure)

        // Empty credentials
        val emptyId = repository.loginAsMainAdmin("", "PR12")
        assertTrue("Empty ID should fail", emptyId.isFailure)

        val emptyPass = repository.loginAsMainAdmin("PR12", "")
        assertTrue("Empty pass should fail", emptyPass.isFailure)
    }

    @Test
    fun testProductionBuildDisablesDemoCredentials() {
        // In production mode (isDevelopmentMode = false), demo credentials must fail
        val prodLoginPR = repository.loginAsMainAdmin("PR12", "PR12", isDevelopmentMode = false)
        assertTrue("Production mode should reject demo credentials for PR12", prodLoginPR.isFailure)
        assertTrue(
            prodLoginPR.exceptionOrNull()?.message?.contains("disabled in production") == true
        )

        val prodLoginCD = repository.loginAsMainAdmin("CD12", "CD12", isDevelopmentMode = false)
        assertTrue("Production mode should reject demo credentials for CD12", prodLoginCD.isFailure)
        assertTrue(
            prodLoginCD.exceptionOrNull()?.message?.contains("disabled in production") == true
        )
    }

    @Test
    fun testShopkeeperStrictShopIsolation() {
        // Login as Sharma General Store shopkeeper (SK_SHARMA)
        val loginShopkeeper = repository.loginAsShopkeeper("SK_SHARMA", "password123")
        assertTrue("SK_SHARMA should login as shopkeeper", loginShopkeeper.isSuccess)
        assertTrue(repository.isShopkeeper())
        assertFalse("Shopkeeper is NOT Main Admin", repository.isMainAdmin())
        assertEquals("sharma_general", repository.currentProfile.value.shopId)

        // Shopkeeper CANNOT self-assign MAIN_ADMIN role
        repository.setRole("MAIN_ADMIN")
        assertEquals("Role should remain shopkeeper", "shopkeeper", repository.currentProfile.value.role)

        // Shopkeeper can save products for own shop
        val ownProduct = Product(
            id = "test_sk_prod_1",
            shopId = "sharma_general",
            name = "Fresh Ghee",
            price = 650.0,
            inStock = true
        )
        val saveOwnResult = repository.saveProduct(ownProduct)
        assertTrue("Shopkeeper can add product to their own shop", saveOwnResult)

        // Shopkeeper CANNOT save or edit products for another shop
        val otherShopProduct = Product(
            id = "test_sk_prod_2",
            shopId = "rani_fashion",
            name = "Silk Saree",
            price = 1200.0,
            inStock = true
        )
        val saveOtherResult = repository.saveProduct(otherShopProduct)
        assertFalse("Shopkeeper cannot add/edit another shop's product", saveOtherResult)

        // Shopkeeper CANNOT add other shopkeepers (only Main Admins can)
        val addSkResult = repository.addShopkeeper(
            name = "Unauthorized User",
            loginId = "SK_FAKE",
            shopId = "rani_fashion",
            shopName = "Rani Fashion Hub",
            phone = "+91 99999 88888",
            email = "fake@test.com"
        )
        assertFalse("Shopkeeper cannot add other shopkeepers", addSkResult)
    }

    @Test
    fun testReservedAdminNamesRejectionOnRegistration() {
        val expectedMessage = "This name is reserved for a FlashDrop Admin. Please choose another name."

        // Exact match
        val res1 = repository.registerNewUser("Pranjal Rajpoot", "userPass123", "9876543210")
        assertTrue("Pranjal Rajpoot registration should fail", res1.isFailure)
        assertEquals(expectedMessage, res1.exceptionOrNull()?.message)

        val res2 = repository.registerNewUser("Chanchal Didi", "userPass123", "9876543211")
        assertTrue("Chanchal Didi registration should fail", res2.isFailure)
        assertEquals(expectedMessage, res2.exceptionOrNull()?.message)

        // Case-insensitivity & extra whitespace variations
        val res3 = repository.registerNewUser("   pranjal   rajpoot  ", "userPass123", "9876543212")
        assertTrue("Normalized Pranjal Rajpoot should fail", res3.isFailure)
        assertEquals(expectedMessage, res3.exceptionOrNull()?.message)

        val res4 = repository.registerNewUser("chanchal didi", "userPass123", "9876543213")
        assertTrue("Lowercase chanchal didi should fail", res4.isFailure)
        assertEquals(expectedMessage, res4.exceptionOrNull()?.message)

        val res5 = repository.registerNewUser("Pranjal", "userPass123", "9876543214")
        assertTrue("Pranjal short name should fail", res5.isFailure)
        assertEquals(expectedMessage, res5.exceptionOrNull()?.message)

        val res6 = repository.registerNewUser("Chanchal", "userPass123", "9876543215")
        assertTrue("Chanchal short name should fail", res6.isFailure)
        assertEquals(expectedMessage, res6.exceptionOrNull()?.message)

        val res7 = repository.registerNewUser("PR", "userPass123", "9876543216")
        assertTrue("PR code should fail", res7.isFailure)
        assertEquals(expectedMessage, res7.exceptionOrNull()?.message)

        val res8 = repository.registerNewUser("CD", "userPass123", "9876543217")
        assertTrue("CD code should fail", res8.isFailure)
        assertEquals(expectedMessage, res8.exceptionOrNull()?.message)
    }

    @Test
    fun testSuccessfulUserRegistrationAndDuplicateValidation() {
        // Blank mobile number should fail (compulsory check)
        val blankMobileResult = repository.registerNewUser("Sunil Verma", "securePass99", "")
        assertTrue("Blank mobile number should fail", blankMobileResult.isFailure)

        // Invalid mobile number should fail
        val invalidMobileResult = repository.registerNewUser("Sunil Verma", "securePass99", "12345")
        assertTrue("Short mobile number should fail", invalidMobileResult.isFailure)

        // Register a regular new user with valid 10-digit mobile number
        val regResult = repository.registerNewUser("Sunil Verma", "securePass99", "9123456789")
        assertTrue("Sunil Verma registration should succeed", regResult.isSuccess)
        val profile = regResult.getOrThrow()
        assertEquals("Sunil Verma", profile.name)
        assertEquals("+91 91234 56789", profile.phone)
        assertEquals("customer", profile.role)
        assertFalse(profile.isVerifiedAdmin)
        assertFalse(profile.isVerifiedShopkeeper)

        // Duplicate registration with same name (ignoring case)
        val duplicateResult = repository.registerNewUser("  sunil verma  ", "otherPass123", "9988776655")
        assertTrue("Duplicate registration should fail", duplicateResult.isFailure)
        assertTrue(
            "Error should state duplicate account",
            duplicateResult.exceptionOrNull()?.message?.contains("already exists") == true
        )

        // Duplicate registration with same mobile number
        val duplicateMobileResult = repository.registerNewUser("Anil Verma", "otherPass123", "9123456789")
        assertTrue("Duplicate mobile number should fail", duplicateMobileResult.isFailure)
    }

    @Test
    fun testUnifiedAuthenticationFlowForCustomerShopkeeperAndAdmin() {
        // 1. Admin 1 authentication via Admin Code "PR" and password "PR12"
        val admin1Auth = repository.authenticateUser("PR", "PR12")
        assertTrue("Admin 1 via PR should succeed", admin1Auth.isSuccess)
        assertEquals("MAIN_ADMIN", admin1Auth.getOrThrow().role)
        assertTrue(admin1Auth.getOrThrow().isVerifiedAdmin)

        // 2. Admin 2 authentication via Admin Code "CD" and password "CD12"
        val admin2Auth = repository.authenticateUser("CD", "CD12")
        assertTrue("Admin 2 via CD should succeed", admin2Auth.isSuccess)
        assertEquals("MAIN_ADMIN", admin2Auth.getOrThrow().role)
        assertTrue(admin2Auth.getOrThrow().isVerifiedAdmin)

        // 3. Admin authentication with wrong password should fail
        val adminFail = repository.authenticateUser("PR", "WRONG_PASSWORD")
        assertTrue("Admin with wrong password should fail", adminFail.isFailure)

        // 4. Shopkeeper authentication
        val skAuth = repository.authenticateUser("SK_SHARMA", "password123")
        assertTrue("Shopkeeper authentication should succeed", skAuth.isSuccess)
        assertEquals("shopkeeper", skAuth.getOrThrow().role)
        assertEquals("sharma_general", skAuth.getOrThrow().shopId)

        // 5. Customer authentication
        repository.registerNewUser("Geeta Devi", "geetaPass1", "9812345670")
        val customerAuth = repository.authenticateUser("Geeta Devi", "geetaPass1")
        assertTrue("Customer authentication should succeed", customerAuth.isSuccess)
        assertEquals("customer", customerAuth.getOrThrow().role)
        assertFalse(customerAuth.getOrThrow().isVerifiedAdmin)

        // 6. Customer with wrong password
        val customerFail = repository.authenticateUser("Geeta Devi", "wrongPass")
        assertTrue("Customer with wrong password should fail", customerFail.isFailure)
    }

    @Test
    fun testPasswordResetFlow() {
        // Reset for existing customer
        val resetResult = repository.requestPasswordReset("Ajay Rajput")
        assertTrue("Password reset should succeed", resetResult.isSuccess)

        // Reset for Admin
        val adminReset = repository.requestPasswordReset("PR12")
        assertTrue("Admin reset should succeed", adminReset.isSuccess)

        // Reset for unknown user
        val unknownReset = repository.requestPasswordReset("nonexistent_user_9999")
        assertTrue("Unknown user reset should fail", unknownReset.isFailure)
    }

    @Test
    fun testConditionalAuthenticationStates() {
        // Initial state is unauthenticated Guest
        assertFalse("Initial profile should be unauthenticated Guest", repository.currentProfile.value.isAuthenticated)
        assertEquals("Guest", repository.currentProfile.value.name)
        assertEquals("customer", repository.currentProfile.value.role)

        // Switching to Guest Mode (Unauthenticated State)
        repository.setGuestMode()
        assertFalse("Guest mode should be unauthenticated", repository.currentProfile.value.isAuthenticated)
        assertEquals("Guest", repository.currentProfile.value.name)
        assertEquals("customer", repository.currentProfile.value.role)

        // Authenticating as Registered Customer (e.g. Pranjal Rajpoot)
        repository.loginAsCustomer("Pranjal Rajpoot")
        assertTrue("Should be authenticated after login", repository.currentProfile.value.isAuthenticated)
        assertEquals("Pranjal Rajpoot", repository.currentProfile.value.name)
        assertEquals("customer", repository.currentProfile.value.role)

        // Logout transitions back to Guest Mode with name Guest
        repository.logout()
        assertFalse("Logout should transition to unauthenticated guest mode", repository.currentProfile.value.isAuthenticated)
        assertEquals("Guest", repository.currentProfile.value.name)
        assertEquals("customer", repository.currentProfile.value.role)
    }

    @Test
    fun testShopkeeperPortalAuthenticationSuccess() {
        // Authenticate with valid Sharma General Store credentials
        val result = repository.authenticateShopkeeper(
            gstNumber = "09AABCU9603R1ZV",
            shopNameOrId = "Sharma General Store",
            mobileNumber = "9876543210",
            password = "password123"
        )

        assertTrue("Authentication should succeed for valid approved shopkeeper", result is ShopkeeperLoginResult.Success)
        val success = result as ShopkeeperLoginResult.Success
        assertEquals("shopkeeper", success.profile.role)
        assertTrue("Profile must be verified shopkeeper", success.profile.isVerifiedShopkeeper)
        assertTrue("Profile must be authenticated", success.profile.isAuthenticated)
        assertEquals("sharma_general", success.profile.shopId)
    }

    @Test
    fun testShopkeeperPortalAuthenticationGenericErrorAndRateLimiting() {
        // Attempt login with invalid credentials
        val result = repository.authenticateShopkeeper(
            gstNumber = "09AABCU9603R1ZV",
            shopNameOrId = "Sharma General Store",
            mobileNumber = "9876543210",
            password = "wrongPassword!"
        )

        assertTrue("Authentication must fail with invalid password", result is ShopkeeperLoginResult.Failure)
        val failure = result as ShopkeeperLoginResult.Failure
        assertEquals(
            "The shop account details could not be verified. Please check your details and try again.",
            failure.message
        )

        // Attempt 4 more times to trigger rate-limiting lockout (total 5 failures)
        for (i in 2..5) {
            repository.authenticateShopkeeper(
                gstNumber = "09AABCU9603R1ZV",
                shopNameOrId = "Sharma General Store",
                mobileNumber = "9876543210",
                password = "wrongPassword!"
            )
        }

        assertTrue("After 5 failures, rate limiting lockout must be active", repository.getShopkeeperLockoutSecondsRemaining() > 0)
    }

    @Test
    fun testShopkeeperRegistrationValidationAndPendingStatus() {
        // Test invalid GSTIN format rejection
        val badGstResult = repository.submitShopRegistration(
            name = "Gupta Kirana",
            ownerName = "Sunil Gupta",
            phone = "9876512345",
            category = "Grocery",
            address = "Near Railway Station, Mahoba",
            gstNumber = "INVALID_GST",
            password = "secretPassword123"
        )
        assertTrue("Invalid GST format must be rejected", badGstResult.isFailure)

        // Test valid registration
        val validResult = repository.submitShopRegistration(
            name = "Gupta Kirana",
            ownerName = "Sunil Gupta",
            phone = "9876512345",
            category = "Grocery",
            address = "Near Railway Station, Mahoba",
            gstNumber = "09AABCG1234F1Z8",
            password = "secretPassword123"
        )
        assertTrue("Valid registration must succeed", validResult.isSuccess)
        val createdShop = validResult.getOrNull()
        assertNotNull("Created shop must not be null", createdShop)
        assertEquals("pending", createdShop?.status)
        assertFalse("Pending shop must not be open", createdShop?.isOpen ?: true)
        assertTrue("Plaintext password must not be stored", createdShop?.password.isNullOrEmpty())
        assertTrue("Password hash must be generated", createdShop?.passwordHash?.isNotEmpty() == true)

        // Test duplicate GSTIN rejection
        val duplicateResult = repository.submitShopRegistration(
            name = "Another Shop",
            ownerName = "Another Owner",
            phone = "9876554321",
            category = "Grocery",
            address = "Alha Chowk, Mahoba",
            gstNumber = "09AABCG1234F1Z8", // Same GSTIN as above
            password = "password123"
        )
        assertTrue("Duplicate GSTIN registration must be rejected", duplicateResult.isFailure)
    }

    @Test
    fun testProductPhotoPersistenceAndValidation() {
        // Authenticate as Sharma General Store shopkeeper
        val loginShopkeeper = repository.loginAsShopkeeper("SK_SHARMA", "password123")
        assertTrue("SK_SHARMA should login as shopkeeper", loginShopkeeper.isSuccess)
        assertTrue(repository.isShopkeeper())
        assertEquals("sharma_general", repository.currentProfile.value.shopId)

        // 1. Rejected if name is blank
        val blankNameProd = Product(
            id = "prod_blank_test",
            shopId = "sharma_general",
            name = "",
            price = 150.0,
            imageUrl = "https://images.example.com/item.jpg"
        )
        assertFalse("Product with blank name must be rejected", repository.saveProduct(blankNameProd))

        // 2. Rejected if price is non-positive
        val zeroPriceProd = Product(
            id = "prod_zero_price",
            shopId = "sharma_general",
            name = "Zero Price Biscuit",
            price = 0.0,
            imageUrl = "https://images.example.com/item.jpg"
        )
        assertFalse("Product with 0 price must be rejected", repository.saveProduct(zeroPriceProd))

        // 3. Main Admin or authorized shopkeeper can save valid product with custom photo & description
        val validProduct = Product(
            id = "prod_valid_photo_1",
            shopId = "sharma_general",
            name = "Organic Turmeric Powder",
            category = "Grocery",
            price = 120.0,
            originalPrice = 140.0,
            unit = "250g",
            description = "Farm-fresh high curcumin turmeric from Mahoba district farms",
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/products%2Fsharma_general%2Fturmeric.jpg",
            inStock = true
        )
        val saved = repository.saveProduct(validProduct)
        assertTrue("Valid product with photo and description must be saved successfully", saved)

        val retrieved = repository.products.value.find { it.id == "prod_valid_photo_1" }
        assertNotNull("Saved product must exist in repository state", retrieved)
        assertEquals("Organic Turmeric Powder", retrieved?.name)
        assertEquals("Farm-fresh high curcumin turmeric from Mahoba district farms", retrieved?.description)
        assertEquals("https://firebasestorage.googleapis.com/v0/b/app/o/products%2Fsharma_general%2Fturmeric.jpg", retrieved?.imageUrl)
        assertEquals(120.0, retrieved?.price ?: 0.0, 0.01)
    }

    @Test
    fun testCameraTempUriCreation() {
        val pair = com.example.util.ImageUtils.createCameraTempUri(context)
        assertNotNull("Camera temp URI and File must be created successfully", pair)
        assertNotNull("Temp URI must not be null", pair?.first)
        assertTrue("Temp File must exist in cache directory", pair?.second?.exists() == true)
    }
}

