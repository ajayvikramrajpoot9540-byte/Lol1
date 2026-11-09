package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeliveryAddress
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
class EditProfileTest {

    private lateinit var context: Context
    private lateinit var repository: FlashDropRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FlashDropRepository(context)
    }

    @Test
    fun testCustomerProfileUpdate_Success() {
        // Given customer is signed in
        repository.setRole("customer", "", "CUST")

        // When customer updates valid profile info
        val result = repository.updateCustomerProfile(
            name = "Ravi Sharma",
            phone = "+91 98765 11223",
            email = "ravi.sharma@flashdrop.local",
            photoUrl = "https://example.com/photo.jpg"
        )

        // Then update should succeed
        assertTrue("Customer profile update should succeed", result.isSuccess)
        val profile = repository.currentProfile.value
        assertEquals("Ravi Sharma", profile.name)
        assertTrue(profile.phone.contains("9876511223") || profile.phone.contains("98765 11223"))
        assertEquals("ravi.sharma@flashdrop.local", profile.email)
        assertEquals("https://example.com/photo.jpg", profile.photoUrl)
        assertTrue(profile.isAuthenticated)
    }

    @Test
    fun testCustomerProfileUpdate_ValidationErrors() {
        repository.setRole("customer", "", "CUST")

        // Blank name should fail
        val blankNameResult = repository.updateCustomerProfile(
            name = "   ",
            phone = "9876512345",
            email = "test@example.com"
        )
        assertFalse("Blank name must be rejected", blankNameResult.isSuccess)

        // Guest name should fail
        val guestNameResult = repository.updateCustomerProfile(
            name = "Guest",
            phone = "9876512345",
            email = "test@example.com"
        )
        assertFalse("Guest name must be rejected", guestNameResult.isSuccess)

        // Invalid phone (< 10 digits) should fail
        val shortPhoneResult = repository.updateCustomerProfile(
            name = "Valid Name",
            phone = "12345",
            email = "test@example.com"
        )
        assertFalse("Phone with less than 10 digits must be rejected", shortPhoneResult.isSuccess)
    }

    @Test
    fun testDeliveryAddressesManagement() {
        repository.setRole("customer", "", "CUST")

        val addr1 = DeliveryAddress(
            id = "addr_1",
            houseNumber = "House 12B",
            street = "Station Road",
            area = "Subhash Chowk",
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            landmark = "Near Kali Mandir",
            isDefault = true
        )

        val save1 = repository.saveDeliveryAddress(addr1)
        assertTrue("Saving first address should succeed", save1.isSuccess)
        val profile1 = repository.currentProfile.value
        assertEquals(1, profile1.savedAddresses.size)
        assertTrue(profile1.savedAddresses[0].isDefault)
        assertEquals("Subhash Chowk", profile1.locality)

        // Add second address and make it default
        val addr2 = DeliveryAddress(
            id = "addr_2",
            houseNumber = "Flat 4A",
            street = "Civil Lines Road",
            area = "Alha Chowk",
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            isDefault = true
        )

        val save2 = repository.saveDeliveryAddress(addr2)
        assertTrue("Saving second address should succeed", save2.isSuccess)
        val profile2 = repository.currentProfile.value
        assertEquals(2, profile2.savedAddresses.size)

        // addr2 should be default, addr1 should NOT be default
        val updatedAddr1 = profile2.savedAddresses.find { it.id == "addr_1" }
        val updatedAddr2 = profile2.savedAddresses.find { it.id == "addr_2" }
        assertNotNull(updatedAddr1)
        assertNotNull(updatedAddr2)
        assertFalse(updatedAddr1!!.isDefault)
        assertTrue(updatedAddr2!!.isDefault)
        assertEquals("Alha Chowk", profile2.locality)

        // Change default back to addr1
        val setDefault = repository.setDefaultDeliveryAddress("addr_1")
        assertTrue(setDefault.isSuccess)
        val profile3 = repository.currentProfile.value
        assertTrue(profile3.savedAddresses.find { it.id == "addr_1" }!!.isDefault)
        assertFalse(profile3.savedAddresses.find { it.id == "addr_2" }!!.isDefault)
        assertEquals("Subhash Chowk", profile3.locality)

        // Delete addr2
        val delete2 = repository.deleteDeliveryAddress("addr_2")
        assertTrue(delete2.isSuccess)
        val profile4 = repository.currentProfile.value
        assertEquals(1, profile4.savedAddresses.size)
        assertEquals("addr_1", profile4.savedAddresses[0].id)
    }

    @Test
    fun testAddNewAddressFormFieldsAndPersistence() {
        repository.setRole("customer", "", "CUST")

        val workAddress = DeliveryAddress(
            id = "addr_work",
            name = "Ajay Rajput",
            phone = "9876543210",
            countryCode = "+91",
            label = "Work",
            houseNumber = "Office 302",
            buildingBlock = "Trade Center Block B",
            street = "Station Road",
            area = "Civil Lines",
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            landmark = "Near Head Post Office",
            isDefault = true
        )

        val saveResult = repository.saveDeliveryAddress(workAddress)
        assertTrue("Saving full address with new fields should succeed", saveResult.isSuccess)

        val profile = repository.currentProfile.value
        val saved = profile.savedAddresses.find { it.id == "addr_work" }
        assertNotNull(saved)
        assertEquals("Ajay Rajput", saved!!.name)
        assertEquals("9876543210", saved.phone)
        assertEquals("+91", saved.countryCode)
        assertEquals("Work", saved.label)
        assertEquals("Office 302", saved.houseNumber)
        assertEquals("Trade Center Block B", saved.buildingBlock)
        assertEquals("Near Head Post Office", saved.landmark)
        assertEquals("Civil Lines", saved.area)
        assertTrue(saved.isDefault)

        // Contact and display format helpers
        assertEquals("Ajay Rajput • +91 9876543210", saved.fullContact())
        assertTrue(saved.formatDisplay().contains("Office 302"))
        assertTrue(saved.formatDisplay().contains("Trade Center Block B"))
        assertTrue(saved.formatDisplay().contains("Near Head Post Office"))

        // Add second address with "Friend/Parent's Home" label
        val parentAddress = DeliveryAddress(
            id = "addr_parent",
            name = "Ramlal Rajput",
            phone = "9123456789",
            countryCode = "+91",
            label = "Friend/Parent's Home",
            houseNumber = "House 18",
            buildingBlock = "",
            street = "Bhatipura Lane 2",
            area = "Bhatipura",
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            landmark = "Opposite Primary School",
            isDefault = true
        )

        val saveParent = repository.saveDeliveryAddress(parentAddress)
        assertTrue(saveParent.isSuccess)

        val updatedProfile = repository.currentProfile.value
        assertEquals(2, updatedProfile.savedAddresses.size)
        val parentSaved = updatedProfile.savedAddresses.find { it.id == "addr_parent" }
        val workSaved = updatedProfile.savedAddresses.find { it.id == "addr_work" }
        assertNotNull(parentSaved)
        assertNotNull(workSaved)
        assertTrue("Newest default should be active", parentSaved!!.isDefault)
        assertFalse("Previous address should no longer be default", workSaved!!.isDefault)
        assertEquals("Bhatipura", updatedProfile.locality)
    }

    @Test
    fun testShopProfileUpdate_ByShopkeeper() {
        // Log in as verified shopkeeper of sharma_general
        repository.setRole("shopkeeper", "sharma_general", "SK_SHARMA")

        val updateResult = repository.updateShopProfile(
            shopId = "sharma_general",
            name = "Sharma Mega General Store",
            phone = "+91 98765 99999",
            address = "New Subhash Chowk, Mahoba",
            description = "Serving daily fresh groceries across Mahoba since 2010",
            category = "Grocery",
            openingHours = "07:00 AM - 11:00 PM",
            imageUrl = "https://example.com/logo.jpg",
            bannerUrl = "https://example.com/banner.jpg"
        )

        assertTrue("Shopkeeper updating own shop should succeed", updateResult.isSuccess)
        val updatedShop = repository.shops.value.find { it.id == "sharma_general" }
        assertNotNull(updatedShop)
        assertEquals("Sharma Mega General Store", updatedShop?.name)
        assertEquals("+91 98765 99999", updatedShop?.phone)
        assertEquals("07:00 AM - 11:00 PM", updatedShop?.openingHours)
        assertEquals("https://example.com/logo.jpg", updatedShop?.imageUrl)
        assertEquals("https://example.com/banner.jpg", updatedShop?.bannerUrl)
        // Restricted fields must remain intact
        assertEquals("approved", updatedShop?.status)
    }

    @Test
    fun testShopProfileUpdate_UnauthorizedAnotherShopRejected() {
        // Log in as shopkeeper of sharma_general
        repository.setRole("shopkeeper", "sharma_general", "SK_SHARMA")

        // Try to update hemu_printing
        val unauthorizedUpdate = repository.updateShopProfile(
            shopId = "hemu_printing",
            name = "Tampered Printing Press",
            phone = "9876543210",
            address = "Fake Address",
            description = "Hacked",
            category = "Printing Press",
            openingHours = "00:00 - 24:00"
        )

        assertFalse("Shopkeeper cannot edit another shop's profile", unauthorizedUpdate.isSuccess)
    }

    @Test
    fun testShopGstUpdate_TriggersPendingAdminApproval() {
        repository.setRole("shopkeeper", "sharma_general", "SK_SHARMA")
        val currentGst = repository.shops.value.find { it.id == "sharma_general" }?.gstNumber ?: "09AAAAA0000A1Z5"

        val gstUpdate = repository.updateShopProfile(
            shopId = "sharma_general",
            name = "Sharma General Store",
            phone = "+91 98765 43210",
            address = "Subhash Chowk, Mahoba",
            description = "General Store",
            category = "Grocery",
            openingHours = "08:00 AM - 10:00 PM",
            newGstNumber = "09XYZAB1234M1Z8"
        )

        assertTrue(gstUpdate.isSuccess)
        val shop = repository.shops.value.find { it.id == "sharma_general" }
        // Current verified GST remains intact
        assertEquals(currentGst, shop?.gstNumber)
        // Pending GST number is recorded and status is pending_approval
        assertEquals("09XYZAB1234M1Z8", shop?.pendingGstNumber)
        assertEquals("pending_approval", shop?.gstApprovalStatus)
    }

    @Test
    fun testMainAdminProfileUpdate_BothOwnersEqualPermissions() {
        // Test Owner 1: Pranjal Rajpoot (PR12)
        val loginPR = repository.loginAsMainAdmin("PR12", "PR12")
        assertTrue("Pranjal Rajpoot login should succeed", loginPR.isSuccess)
        assertTrue(repository.isAuthorizedMainAdmin())

        val prUpdate = repository.updateAdminProfile(
            adminId = "PR12",
            displayName = "Pranjal Rajpoot (Lead)",
            phone = "+91 98765 00012",
            photoUrl = "https://example.com/pranjal.jpg"
        )
        assertTrue("Pranjal Rajpoot editing PR12 profile should succeed", prUpdate.isSuccess)
        val prProfile = repository.currentProfile.value
        assertEquals("Pranjal Rajpoot (Lead)", prProfile.name)
        assertEquals("https://example.com/pranjal.jpg", prProfile.photoUrl)

        // Pranjal Rajpoot cannot edit Chanchal Didi's profile without authorization
        val tamperCD = repository.updateAdminProfile(
            adminId = "CD12",
            displayName = "Tampered Name",
            phone = "+91 98765 99999"
        )
        assertFalse("Admin cannot edit another admin's profile without authorization", tamperCD.isSuccess)

        // Test Owner 2: Chanchal Didi (CD12)
        val loginCD = repository.loginAsMainAdmin("CD12", "CD12")
        assertTrue("Chanchal Didi login should succeed", loginCD.isSuccess)
        assertTrue(repository.isAuthorizedMainAdmin())

        val cdUpdate = repository.updateAdminProfile(
            adminId = "CD12",
            displayName = "Chanchal Didi (Executive)",
            phone = "+91 98765 00013",
            photoUrl = "https://example.com/chanchal.jpg"
        )
        assertTrue("Chanchal Didi editing CD12 profile should succeed", cdUpdate.isSuccess)
        val cdProfile = repository.currentProfile.value
        assertEquals("Chanchal Didi (Executive)", cdProfile.name)
        assertEquals("https://example.com/chanchal.jpg", cdProfile.photoUrl)
    }
}
