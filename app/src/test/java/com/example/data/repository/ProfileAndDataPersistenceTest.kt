package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppPersistenceManager
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryConfig
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopRegistrationConfig
import com.example.data.model.UserProfile
import com.example.util.ImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileAndDataPersistenceTest {

    private lateinit var context: Context
    private lateinit var persistence: AppPersistenceManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        persistence = AppPersistenceManager(context)
        // Clear test files to ensure clean state
        val dir = File(context.filesDir, "flashdrop_data")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testUserProfilePersistence_SurvivesSimulatedRestart() {
        val repo1 = ApnaBazRepository(context)

        // Customer updates profile
        val addr = DeliveryAddress(
            id = "addr_test_01",
            houseNumber = "House 45",
            street = "Chhatarpur Road",
            area = "Civil Lines",
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            isDefault = true
        )
        repo1.saveDeliveryAddress(addr)

        val updateResult = repo1.updateCustomerProfile(
            name = "Ramesh Kumar",
            phone = "+91 98765 43210",
            email = "ramesh.kumar@flashdrop.local",
            photoUrl = "file:///data/user/0/com.example/files/user_photos/profile_ramesh.jpg"
        )
        assertTrue("Profile update should succeed", updateResult.isSuccess)

        // Verify saved on disk via AppPersistenceManager
        val savedProfile = persistence.loadUserProfile()
        assertNotNull(savedProfile)
        assertEquals("Ramesh Kumar", savedProfile?.name)
        assertEquals("ramesh.kumar@flashdrop.local", savedProfile?.email)
        assertEquals("file:///data/user/0/com.example/files/user_photos/profile_ramesh.jpg", savedProfile?.photoUrl)
        assertEquals(1, savedProfile?.savedAddresses?.size)
        assertEquals("Civil Lines", savedProfile?.savedAddresses?.first()?.area)

        // Simulate app restart by instantiating a brand-new repository instance
        val repo2 = ApnaBazRepository(context)
        val restoredProfile = repo2.currentProfile.value
        assertEquals("Ramesh Kumar", restoredProfile.name)
        assertEquals("ramesh.kumar@flashdrop.local", restoredProfile.email)
        assertEquals("file:///data/user/0/com.example/files/user_photos/profile_ramesh.jpg", restoredProfile.photoUrl)
        assertEquals(1, restoredProfile.savedAddresses.size)
    }

    private fun setAdminProfile(repo: ApnaBazRepository, loginId: String = "PR12", name: String = "Pranjal Rajpoot") {
        val field = ApnaBazRepository::class.java.getDeclaredField("_currentProfile")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(repo) as kotlinx.coroutines.flow.MutableStateFlow<UserProfile>
        stateFlow.value = UserProfile(
            id = "user_$loginId",
            name = name,
            phone = "+91 98765 00012",
            email = "pranjal.owner@flashdrop.local",
            loginId = loginId,
            role = "MAIN_ADMIN",
            isAuthenticated = true,
            isVerifiedAdmin = true
        )
    }

    @Test
    fun testProductPersistence_AddAndEdit_SurvivesRestart() {
        val repo1 = ApnaBazRepository(context)
        setAdminProfile(repo1, "PR12", "Pranjal Rajpoot")

        val newProduct = Product(
            id = "prod_custom_999",
            shopId = "sharma_general",
            shopName = "Sharma General Store",
            name = "Organic Bundelkhand Wheat",
            category = "Grocery",
            subcategory = "Atta & Flours",
            price = 450.0,
            originalPrice = 500.0,
            description = "100% Pure Sharbati Wheat from Mahoba farms",
            imageUrl = "https://images.unsplash.com/sample_wheat",
            unit = "10 kg",
            inStock = true
        )

        val saved = repo1.saveProduct(newProduct)
        assertTrue("Admin saveProduct should succeed", saved)

        // Simulate app restart
        val repo2 = ApnaBazRepository(context)
        val loadedProduct = repo2.products.value.find { it.id == "prod_custom_999" }
        assertNotNull("Product should exist after restart", loadedProduct)
        assertEquals("Organic Bundelkhand Wheat", loadedProduct?.name)
        assertEquals(450.0, loadedProduct?.price ?: 0.0, 0.01)

        // Delete product
        setAdminProfile(repo2, "PR12", "Pranjal Rajpoot")
        val deleted = repo2.deleteProduct("prod_custom_999")
        assertTrue("Delete product should succeed", deleted)

        // Simulate another restart
        val repo3 = ApnaBazRepository(context)
        val deletedProduct = repo3.products.value.find { it.id == "prod_custom_999" }
        assertTrue("Product should remain deleted after restart", deletedProduct == null)
    }

    @Test
    fun testCartPersistence_AcrossSimulatedRestart() {
        val repo1 = ApnaBazRepository(context)
        val sampleProduct = repo1.products.value.first()

        repo1.clearCart()
        assertEquals(0, repo1.cartItems.value.size)

        repo1.addToCart(sampleProduct)
        repo1.updateCartQuantity(sampleProduct.id, 2) // quantity = 3
        assertEquals(1, repo1.cartItems.value.size)
        assertEquals(3, repo1.cartItems.value.first().quantity)

        // Simulate app restart
        val repo2 = ApnaBazRepository(context)
        assertEquals(1, repo2.cartItems.value.size)
        assertEquals(sampleProduct.id, repo2.cartItems.value.first().product.id)
        assertEquals(3, repo2.cartItems.value.first().quantity)
    }

    @Test
    fun testAdminSettingsPersistence_3StepRegistrationAndDeliveryConfig() {
        val repo1 = ApnaBazRepository(context)
        setAdminProfile(repo1, "PR12", "Pranjal Rajpoot")

        // Toggle 3-step registration ON
        val regResult = repo1.updateShopRegistrationMode(true)
        assertTrue("Toggling 3-step registration mode should succeed", regResult.isSuccess)
        assertTrue(repo1.shopRegistrationConfig.value.threeStepEnabled)

        // Update delivery config
        repo1.updateDeliveryConfig(fee = 35.0, minOrder = 150.0, freeThreshold = 499.0)
        assertEquals(35.0, repo1.deliveryConfig.value.defaultDeliveryFee, 0.01)
        assertEquals(150.0, repo1.deliveryConfig.value.minOrderAmount, 0.01)
        assertEquals(499.0, repo1.deliveryConfig.value.freeDeliveryThreshold, 0.01)

        // Simulate app restart
        val repo2 = ApnaBazRepository(context)
        assertTrue("3-step registration toggle must remain ON after restart", repo2.shopRegistrationConfig.value.threeStepEnabled)
        assertEquals("PR12", repo2.shopRegistrationConfig.value.lastUpdatedByAdminId)
        assertEquals(35.0, repo2.deliveryConfig.value.defaultDeliveryFee, 0.01)
        assertEquals(150.0, repo2.deliveryConfig.value.minOrderAmount, 0.01)
        assertEquals(499.0, repo2.deliveryConfig.value.freeDeliveryThreshold, 0.01)
    }

    @Test
    fun testPrintingPressShopsAndCategory_Persistence() {
        val repo = ApnaBazRepository(context)
        val printingShops = repo.getPrintingPressShops()

        assertFalse("Printing Press shops must exist and not be empty", printingShops.isEmpty())
        assertTrue("Bundelkhand Printing Press should be available", printingShops.any { it.name.contains("Printing", ignoreCase = true) })
        assertTrue("Printing Press category must match exactly", printingShops.all { it.category.equals("Printing Press", ignoreCase = true) })
    }

    @Test
    fun testRegisterShop_WithPrintingPressCategory_Step1() {
        val repo = ApnaBazRepository(context)
        val result = repo.submitShopRegistrationStep1(
            name = "Mahoba Digital Printers",
            ownerName = "Sunil Sahu",
            phone = "9812399991",
            category = "Printing Press",
            address = "Station Road, Near Bus Stand, Mahoba",
            description = "All types of photo printing, banners and document xerox",
            gstNumber = "09AABCP1234D1Z9",
            password = "PrintPassword123",
            confirmPassword = "PrintPassword123"
        )

        assertTrue("Shop registration with Printing Press category should succeed", result.isSuccess)
        val createdRequest = result.getOrNull()
        assertNotNull(createdRequest)
        assertEquals("Printing Press", createdRequest?.category)
        assertEquals("Mahoba Digital Printers", createdRequest?.shopName)
    }

    @Test
    fun testImageUtils_PersistImageToAppStorage() {
        // Create a dummy image file in cache
        val cacheFile = File(context.cacheDir, "temp_test_image.jpg")
        cacheFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) // JPEG header dummy
        val cacheUri = Uri.fromFile(cacheFile)

        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = cacheUri,
            folderName = "test_photos",
            fileNamePrefix = "profile_test"
        )

        assertNotNull("Persisted URI should not be null", persistedUri)
        val persistedFile = File(context.filesDir, "test_photos/${persistedUri?.lastPathSegment}")
        assertTrue("Persisted file must physically exist in app internal storage", persistedFile.exists())
    }

    @Test
    fun testCartAndOrdersEmptyInitialStateForFreshCustomer() {
        val repo = ApnaBazRepository(context)
        // Verify cart is empty initially so floating cart pill is NOT displayed
        assertEquals("Cart should start empty when customer chose nothing", 0, repo.cartItems.value.size)

        // When customer chooses an item, it should display
        val sampleProduct = repo.products.value.first()
        repo.addToCart(sampleProduct)
        assertEquals("Cart should contain 1 item when customer chooses something", 1, repo.cartItems.value.size)

        // Verify orders is empty initially so Orders icon is hidden when nothing ordered
        assertEquals("Orders should start empty when customer has not ordered anything", 0, repo.orders.value.size)
    }

    @Test
    fun testAdminCustomersDirectoryAndManagement_Persistence() {
        val repo1 = ApnaBazRepository(context)
        assertTrue("Initial customers list should be populated", repo1.customers.value.size >= 7)

        // Authenticate as Main Admin (PR12) and add a new customer record
        setAdminProfile(repo1, "PR12", "Pranjal Rajpoot")
        val addRes = repo1.addCustomerByAdmin(
            fullName = "Vikash Chaurasia",
            phone = "9876509988",
            email = "vikash.c@flashdrop.local",
            locality = "Alha Chowk",
            primaryAddress = "House 19, Alha Chowk, Mahoba",
            accountStatus = "VIP",
            adminNotes = "Frequent bulk grocery buyer"
        )
        assertTrue("Admin adding customer should succeed", addRes.isSuccess)
        val created = addRes.getOrNull()
        assertNotNull(created)

        // Update status to Active and back to VIP
        val statusUpdated = repo1.updateCustomerStatus(created!!.id, "Active", "Updated note")
        assertTrue("Status update by Main Admin should succeed", statusUpdated)

        // Simulate app restart and verify customer record persists
        val repo2 = ApnaBazRepository(context)
        val restoredCustomer = repo2.customers.value.find { it.id == created.id }
        assertNotNull("Added customer must survive restart", restoredCustomer)
        assertEquals("Vikash Chaurasia", restoredCustomer?.fullName)
        assertEquals("Active", restoredCustomer?.accountStatus)
        assertEquals("Updated note", restoredCustomer?.adminNotes)
    }
}
