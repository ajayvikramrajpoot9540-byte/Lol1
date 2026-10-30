package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Shop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAddShopTest {

    private lateinit var context: Context
    private lateinit var repository: ApnaBazRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ApnaBazRepository(context)
    }

    @Test
    fun testGenerateUniqueShopId_FormatAndUniqueness() {
        val id1 = repository.generateUniqueShopId()
        assertTrue("Generated ID should match FD-SHOP-\\d+ format", id1.matches(Regex("^FD-SHOP-\\d+$")))

        // Add a shop with this ID to the repository
        repository.loginAsMainAdmin("PR12", "PR12")
        val shop1 = Shop(id = id1, name = "Test Shop 1", status = "approved")
        repository.addShop(shop1)

        val id2 = repository.generateUniqueShopId()
        assertTrue("Next generated ID should match format", id2.matches(Regex("^FD-SHOP-\\d+$")))
        assertNotEquals("IDs must never collide or duplicate", id1, id2)
    }

    @Test
    fun testAdminCreateShopWithShopkeeper_SuccessWorkflow() = runBlocking {
        // 1. Authenticate as Main Admin (PR12)
        val adminLogin = repository.loginAsMainAdmin("PR12", "PR12")
        assertTrue("Main Admin PR12 login should succeed", adminLogin.isSuccess)
        assertTrue("Repository isMainAdmin should be true", repository.isMainAdmin())

        // 2. Provision new shop via adminCreateShopWithShopkeeper
        val result = repository.adminCreateShopWithShopkeeper(
            shopName = "Bundelkhand High-Speed Digital Printers",
            ownerName = "Devendra Pratap Singh",
            contactNumber = "9876500001",
            category = "Printing Press",
            address = "Shop 14, Alha Chowk, Mahoba",
            gstNumber = "09ABCDE1234F1Z5",
            password = "Secur3Password!",
            confirmPassword = "Secur3Password!",
            description = "High speed color xerox, flex banners and binding in Mahoba."
        )

        assertTrue("Creation should succeed", result.isSuccess)
        val creationResult = result.getOrNull()
        assertNotNull(creationResult)

        val shopId = creationResult!!.generatedShopId
        assertTrue("Shop ID must have FD-SHOP- prefix", shopId.startsWith("FD-SHOP-"))
        assertEquals("Bundelkhand High-Speed Digital Printers", creationResult.shop.name)
        assertEquals("Devendra Pratap Singh", creationResult.shop.ownerName)
        assertEquals("approved", creationResult.shop.status)
        assertTrue("Shop should be open immediately", creationResult.shop.isOpen)
        assertEquals("APPROVED", creationResult.account.status)
        assertTrue("Shopkeeper account should be active", creationResult.account.isActive)

        // Verify plaintext password is NOT stored
        assertEquals("", creationResult.shop.password)
        assertEquals("", creationResult.account.password)
        assertTrue("Password hash must be populated", creationResult.shop.passwordHash.isNotEmpty())

        // 3. Verify shop exists in repository state
        val inMemoryShop = repository.shops.value.find { it.id == shopId }
        assertNotNull("Shop must exist in repository", inMemoryShop)
        assertEquals("approved", inMemoryShop?.status)

        val inMemoryAccount = repository.shopkeepers.value.find { it.shopId == shopId }
        assertNotNull("Shopkeeper account must exist in repository", inMemoryAccount)
        assertEquals("APPROVED", inMemoryAccount?.status)

        // 4. Verify shopkeeper can sign in with credentials via authenticateShopkeeper
        val authResult = repository.authenticateShopkeeper(
            gstNumber = "09ABCDE1234F1Z5",
            shopNameOrId = shopId,
            mobileNumber = "9876500001",
            password = "Secur3Password!"
        )

        assertTrue("Shopkeeper login must be successful", authResult is ShopkeeperLoginResult.Success)
        val profile = (authResult as ShopkeeperLoginResult.Success).profile
        assertEquals("shopkeeper", profile.role)
        assertTrue(profile.isVerifiedShopkeeper)
        assertFalse(profile.isVerifiedAdmin)
        assertEquals(shopId, profile.shopId)
        assertEquals("Bundelkhand High-Speed Digital Printers", profile.shopName)
    }

    @Test
    fun testUnauthorizedUserCannotCreateShopkeeper() = runBlocking {
        // 1. In Guest / Customer mode
        repository.setGuestMode()
        assertFalse("Guest is not admin", repository.isMainAdmin())

        val result = repository.adminCreateShopWithShopkeeper(
            shopName = "Hacker Shop",
            ownerName = "Anonymous",
            contactNumber = "9876500002",
            category = "Grocery",
            address = "Unknown",
            gstNumber = "09ZZZZZ1234Z1Z9",
            password = "password123",
            confirmPassword = "password123"
        )

        assertFalse("Guest cannot create approved shopkeeper account", result.isSuccess)
        val ex = result.exceptionOrNull()
        assertTrue("Should throw SecurityException", ex is SecurityException)
    }

    @Test
    fun testValidationFailures_DuplicateGstAndPhone() = runBlocking {
        repository.loginAsMainAdmin("PR12", "PR12")

        // 1. Password mismatch
        val mismatchResult = repository.adminCreateShopWithShopkeeper(
            shopName = "Test Shop",
            ownerName = "Owner",
            contactNumber = "9876500003",
            category = "Grocery",
            address = "Address",
            gstNumber = "09TESTA1234A1Z1",
            password = "password123",
            confirmPassword = "password456"
        )
        assertFalse("Password mismatch must fail", mismatchResult.isSuccess)

        // 2. Invalid GSTIN format
        val invalidGstResult = repository.adminCreateShopWithShopkeeper(
            shopName = "Test Shop",
            ownerName = "Owner",
            contactNumber = "9876500003",
            category = "Grocery",
            address = "Address",
            gstNumber = "INVALID_GST",
            password = "password123",
            confirmPassword = "password123"
        )
        assertFalse("Invalid GSTIN format must fail", invalidGstResult.isSuccess)

        // 3. Create initial shop
        val createSuccess = repository.adminCreateShopWithShopkeeper(
            shopName = "Unique Store",
            ownerName = "Unique Owner",
            contactNumber = "9876500004",
            category = "Grocery",
            address = "Address",
            gstNumber = "09UNIKQ1234U1Z2",
            password = "password123",
            confirmPassword = "password123"
        )
        assertTrue(createSuccess.isSuccess)

        // 4. Duplicate GST
        val dupGstResult = repository.adminCreateShopWithShopkeeper(
            shopName = "Another Store",
            ownerName = "Another Owner",
            contactNumber = "9876500005",
            category = "Grocery",
            address = "Address",
            gstNumber = "09UNIKQ1234U1Z2", // Same GST
            password = "password123",
            confirmPassword = "password123"
        )
        assertFalse("Duplicate GSTIN must be rejected", dupGstResult.isSuccess)

        // 5. Duplicate Mobile
        val dupPhoneResult = repository.adminCreateShopWithShopkeeper(
            shopName = "Third Store",
            ownerName = "Third Owner",
            contactNumber = "9876500004", // Same Phone
            category = "Grocery",
            address = "Address",
            gstNumber = "09THRDQ1234T1Z3",
            password = "password123",
            confirmPassword = "password123"
        )
        assertFalse("Duplicate mobile number must be rejected", dupPhoneResult.isSuccess)
    }

    @Test
    fun testCustomerCannotAccessShopkeeperRole() {
        // Customer login
        repository.loginAsCustomer("Rajesh Kumar", "+91 98765 11223")
        val currentProfile = repository.currentProfile.value

        assertEquals("customer", currentProfile.role)
        assertFalse(currentProfile.isVerifiedShopkeeper)
        assertFalse(currentProfile.isVerifiedAdmin)
    }
}
