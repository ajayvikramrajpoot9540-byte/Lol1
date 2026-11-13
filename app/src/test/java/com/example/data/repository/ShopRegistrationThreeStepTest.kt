package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ShopVerificationRequest
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
class ShopRegistrationThreeStepTest {

    private lateinit var context: Context
    private lateinit var repository: FlashDropRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FlashDropRepository(context)
    }

    private fun setProfile(loginId: String, name: String, role: String, email: String, isVerifiedAdmin: Boolean) {
        val field = FlashDropRepository::class.java.getDeclaredField("_currentProfile")
        field.isAccessible = true
        val stateFlow = field.get(repository) as MutableStateFlow<UserProfile>
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
    fun testEqualAdminPermissionsToToggleThreeStepRegistration() = runBlocking {
        // 1. Owner 1: Pranjal Rajpoot (PR12) enables 3-step registration
        setProfile(
            loginId = "PR12",
            name = "Pranjal Rajpoot",
            role = "MAIN_ADMIN",
            email = "pranjal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        val result1 = repository.updateShopRegistrationMode(true)
        assertTrue("PR12 should be able to enable 3-step registration", result1.isSuccess)
        val config1 = repository.shopRegistrationConfig.value
        assertTrue("3-step registration should be enabled", config1.threeStepEnabled)
        assertEquals("PR12", config1.lastUpdatedByAdminId)
        assertEquals("Pranjal Rajpoot", config1.lastUpdatedByAdminName)

        // 2. Owner 2: Chanchal Didi (CD12) has equal authority and can disable or update it
        setProfile(
            loginId = "CD12",
            name = "Chanchal Didi",
            role = "MAIN_ADMIN",
            email = "chanchal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        val result2 = repository.updateShopRegistrationMode(false)
        assertTrue("CD12 should be able to toggle 3-step registration", result2.isSuccess)
        val config2 = repository.shopRegistrationConfig.value
        assertFalse("3-step registration should be disabled by CD12", config2.threeStepEnabled)
        assertEquals("CD12", config2.lastUpdatedByAdminId)
        assertEquals("Chanchal Didi", config2.lastUpdatedByAdminName)
    }

    @Test
    fun testUnauthorizedUserCannotToggleSetting() = runBlocking {
        // Customer profile
        setProfile(
            loginId = "CUST_1001",
            name = "Ramesh Verma",
            role = "customer",
            email = "ramesh@example.com",
            isVerifiedAdmin = false
        )
        val result = repository.updateShopRegistrationMode(true)
        assertTrue("Unauthorized user should fail to update setting", result.isFailure)
    }

    @Test
    fun testStep1RegistrationSubmissionAndValidation() = runBlocking {
        // Valid Step 1 submission
        val result = repository.submitShopRegistrationStep1(
            name = "Bundelkhand Organic Spices",
            ownerName = "Ramdas Kushwaha",
            phone = "9812345671",
            category = "Grocery & Kirana",
            address = "Station Road, Mahoba, UP 210427",
            description = "Organic farm spices",
            gstNumber = "09ABCDE1234F1Z5",
            password = "SecretPassword123",
            confirmPassword = "SecretPassword123"
        )

        assertTrue("Valid Step 1 registration should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val request = result.getOrNull()
        assertNotNull(request)
        assertEquals("Bundelkhand Organic Spices", request?.shopName)
        assertEquals("Ramdas Kushwaha", request?.ownerName)
        assertEquals("+91 9812345671", request?.phone)
        assertEquals("09ABCDE1234F1Z5", request?.gstNumber)
        assertEquals(ShopVerificationRequest.STATUS_DRAFT, request?.status)

        // Invalid GST number should fail
        val invalidGstResult = repository.submitShopRegistrationStep1(
            name = "Shop Invalid",
            ownerName = "Test Owner",
            phone = "9812345672",
            category = "General",
            address = "Address",
            description = "Desc",
            gstNumber = "INVALID_GST",
            password = "SecretPassword123",
            confirmPassword = "SecretPassword123"
        )
        assertTrue("Invalid GSTIN format should fail Step 1", invalidGstResult.isFailure)
    }

    @Test
    fun testStep2DocumentSubmissionWithMaskedAadhaarAndPanValidation() = runBlocking {
        // Submit Step 1 first
        val step1Res = repository.submitShopRegistrationStep1(
            name = "Mahoba Electronics",
            ownerName = "Vikram Singh",
            phone = "9812345673",
            category = "Electronics",
            address = "Gandhi Nagar, Mahoba",
            description = "Appliances and parts",
            gstNumber = "09AAAAA0000A1Z5",
            password = "SecretPassword123",
            confirmPassword = "SecretPassword123"
        )
        val req = step1Res.getOrNull()
        assertNotNull(req)

        // Submit Step 2 documents
        val step2Res = repository.submitShopRegistrationStep2(
            requestId = req!!.id,
            aadhaarNumberRaw = "987654321098",
            aadhaarDocUri = "content://media/aadhaar.jpg",
            panNumber = "ABCDE1234F",
            panDocUri = "content://media/pan.jpg",
            gstCertificateDocUri = "content://media/gst.jpg"
        )

        assertTrue("Valid Step 2 document submission should succeed", step2Res.isSuccess)
        val updatedReq = step2Res.getOrNull()
        assertNotNull(updatedReq)
        assertEquals(ShopVerificationRequest.STATUS_PENDING_REVIEW, updatedReq?.status)
        assertEquals("XXXX-XXXX-1098", updatedReq?.aadhaarNumberMasked)
        assertEquals("ABCDE1234F", updatedReq?.panNumber)
        assertEquals("09AAAAA0000A1Z5", updatedReq?.gstCertificateGstin)
    }

    @Test
    fun testStep3ShopIdGenerationIsUniqueAndIdempotent() = runBlocking {
        val step1Res = repository.submitShopRegistrationStep1(
            name = "Shree Balaji Sweets",
            ownerName = "Govind Sahu",
            phone = "9812345674",
            category = "Sweets & Snacks",
            address = "Alha Chowk, Mahoba",
            description = "Traditional sweets",
            gstNumber = "09BBBBB1111B1Z5",
            password = "SecretPassword123",
            confirmPassword = "SecretPassword123"
        )
        val req = step1Res.getOrThrow()

        // Generate ID
        val genResult1 = repository.generateAndAssignShopIdForApplication(req.id)
        assertTrue("Shop ID generation should succeed", genResult1.isSuccess)
        val (shop1, account1) = genResult1.getOrThrow()
        assertTrue("Shop ID must follow FD-SHOP format", shop1.id.startsWith("FD-SHOP-"))
        assertEquals(shop1.id, account1.shopId)

        // Idempotency check: Generating again for the same application returns the same pair
        val genResult2 = repository.generateAndAssignShopIdForApplication(req.id)
        val (shop2, account2) = genResult2.getOrThrow()
        assertEquals("Subsequent calls must be idempotent and return identical shop ID", shop1.id, shop2.id)
        assertEquals(account1.id, account2.id)
    }

    @Test
    fun testAdminApprovalWorkflowAndShopkeeperLoginGating() = runBlocking {
        // 1. Submit application
        val step1Res = repository.submitShopRegistrationStep1(
            name = "Gupta Provision Store",
            ownerName = "Sunil Gupta",
            phone = "9812345675",
            category = "Kirana & Grocery",
            address = "Subhash Nagar, Mahoba",
            description = "Groceries and spices",
            gstNumber = "09CCCCC2222C1Z5",
            password = "StrongPassword456",
            confirmPassword = "StrongPassword456"
        )
        val req = step1Res.getOrThrow()

        // 2. Generate Shop ID
        val (shop, _) = repository.generateAndAssignShopIdForApplication(req.id).getOrThrow()

        // 3. Before Admin approval, shopkeeper cannot log in to dashboard
        val loginBeforeApproval = repository.authenticateShopkeeper(
            gstNumber = "09CCCCC2222C1Z5",
            shopNameOrId = shop.id,
            mobileNumber = "9812345675",
            password = "StrongPassword456"
        )
        assertTrue("Applicant should be blocked with PendingApproval before admin review", loginBeforeApproval is ShopkeeperLoginResult.PendingApproval)

        // 4. Admin PR12 approves application
        setProfile(
            loginId = "PR12",
            name = "Pranjal Rajpoot",
            role = "MAIN_ADMIN",
            email = "pranjal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )
        val approveRes = repository.approveShopVerification(req.id, "All documents verified in Mahoba directory")
        assertTrue("Admin approval should succeed", approveRes.isSuccess)

        // 5. After approval, shopkeeper can successfully log in and access dashboard
        val loginAfterApproval = repository.authenticateShopkeeper(
            gstNumber = "09CCCCC2222C1Z5",
            shopNameOrId = shop.id,
            mobileNumber = "9812345675",
            password = "StrongPassword456"
        )
        assertTrue("Approved shopkeeper should receive Success login result", loginAfterApproval is ShopkeeperLoginResult.Success)
        val success = loginAfterApproval as ShopkeeperLoginResult.Success
        assertEquals("Gupta Provision Store", success.profile.shopName)
        assertEquals("shopkeeper", success.profile.role)
    }

    @Test
    fun testAdminRequestCorrectedDocumentsAndRejectWorkflows() = runBlocking {
        // Admin profile
        setProfile(
            loginId = "CD12",
            name = "Chanchal Didi",
            role = "MAIN_ADMIN",
            email = "chanchal.owner@flashdrop.local",
            isVerifiedAdmin = true
        )

        // Submit application
        val step1Res = repository.submitShopRegistrationStep1(
            name = "Bundeli Textiles",
            ownerName = "Rakesh Jain",
            phone = "9812345676",
            category = "Clothing & Textiles",
            address = "Main Cloth Market, Mahoba",
            description = "Handloom textiles",
            gstNumber = "09DDDDD3333D1Z5",
            password = "TextilePassword789",
            confirmPassword = "TextilePassword789"
        )
        val req = step1Res.getOrThrow()

        // Request document correction
        val reqDocsRes = repository.requestCorrectedDocuments(req.id, "GST certificate image is blurry, please re-upload")
        assertTrue("Requesting corrected documents should succeed", reqDocsRes.isSuccess)

        val updatedReq = repository.shopVerificationRequests.value.firstOrNull { it.id == req.id }
        assertEquals(ShopVerificationRequest.STATUS_DOCUMENTS_REQUIRED, updatedReq?.status)
        assertEquals("GST certificate image is blurry, please re-upload", updatedReq?.documentsRequiredNotes)

        // Shopkeeper sign in returns DocumentsRequired
        val loginResult = repository.authenticateShopkeeper(
            gstNumber = "09DDDDD3333D1Z5",
            shopNameOrId = "Bundeli Textiles",
            mobileNumber = "9812345676",
            password = "TextilePassword789"
        )
        assertTrue("Login should return DocumentsRequired", loginResult is ShopkeeperLoginResult.DocumentsRequired)
        val docsReq = loginResult as ShopkeeperLoginResult.DocumentsRequired
        assertEquals("GST certificate image is blurry, please re-upload", docsReq.reason)

        // Admin rejects application
        val rejectRes = repository.rejectShopVerification(req.id, "Entity not operating at specified address")
        assertTrue("Rejecting application should succeed", rejectRes.isSuccess)
        val rejectedReq = repository.shopVerificationRequests.value.firstOrNull { it.id == req.id }
        assertEquals(ShopVerificationRequest.STATUS_REJECTED, rejectedReq?.status)
        assertEquals("Entity not operating at specified address", rejectedReq?.rejectionReason)
    }

    @Test
    fun testStep2RealTimeShopkeeperPhotoAndAdminPermissionForFutureChange() = runBlocking {
        // 1. Submit Step 1
        val step1Res = repository.submitShopRegistrationStep1(
            name = "Mahoba Fresh Dairy",
            ownerName = "Deepak Tiwari",
            phone = "9812345679",
            category = "Grocery",
            address = "Civil Lines, Mahoba",
            description = "Fresh milk & paneer",
            gstNumber = "09EEEEE4444E1Z5",
            password = "DairyPassword123",
            confirmPassword = "DairyPassword123"
        )
        val req = step1Res.getOrThrow()

        // 2. Step 2 with requireShopkeeperPhoto = true fails if shopkeeperPhotoUri is blank
        val missingPhotoRes = repository.submitShopRegistrationStep2(
            requestId = req.id,
            aadhaarNumberRaw = "987654321098",
            aadhaarDocUri = "content://media/aadhaar.jpg",
            panNumber = "ABCDE1234F",
            panDocUri = "content://media/pan.jpg",
            gstCertificateDocUri = "content://media/gst.jpg",
            shopkeeperPhotoUri = "",
            requireShopkeeperPhoto = true
        )
        assertTrue("Step 2 must fail when real-time shopkeeper photo is missing", missingPhotoRes.isFailure)

        // 3. Step 2 succeeds when real-time shopkeeper photo is captured
        val validStep2Res = repository.submitShopRegistrationStep2(
            requestId = req.id,
            aadhaarNumberRaw = "987654321098",
            aadhaarDocUri = "content://media/aadhaar.jpg",
            panNumber = "ABCDE1234F",
            panDocUri = "content://media/pan.jpg",
            gstCertificateDocUri = "content://media/gst.jpg",
            shopkeeperPhotoUri = "content://media/realtime_shopkeeper_deepak.jpg",
            requireShopkeeperPhoto = true
        )
        assertTrue("Step 2 with real-time shopkeeper photo should succeed", validStep2Res.isSuccess)
        val submittedReq = validStep2Res.getOrThrow()
        assertEquals("content://media/realtime_shopkeeper_deepak.jpg", submittedReq.shopkeeperPhotoUri)

        // 4. Non-admin shopkeeper cannot update shopkeeper photo without Admin permission
        setProfile(
            loginId = "SK_DEEPAK",
            name = "Deepak Tiwari",
            role = "shopkeeper",
            email = "deepak@example.com",
            isVerifiedAdmin = false
        )
        val unauthorizedUpdate = repository.updateShopkeeperRealTimePhoto(
            shopId = submittedReq.shopId,
            newPhotoUrl = "content://media/new_operator_photo.jpg"
        )
        assertTrue("Updating shopkeeper photo without Admin permission must fail", unauthorizedUpdate.isFailure)

        // 5. Shopkeeper requests permission, Admin grants permission
        val requestPermRes = repository.requestShopkeeperPhotoChangePermission(submittedReq.shopId)
        assertTrue(requestPermRes.isSuccess)
        assertTrue(requestPermRes.getOrThrow().shopkeeperPhotoChangeRequested)

        setProfile(
            loginId = "PR12",
            name = "Main Admin",
            role = "MAIN_ADMIN",
            email = "admin@flashdrop.local",
            isVerifiedAdmin = true
        )
        val grantRes = repository.grantShopkeeperPhotoChangePermission(submittedReq.shopId)
        assertTrue(grantRes.isSuccess)
        assertTrue(grantRes.getOrThrow().shopkeeperPhotoChangeApprovedByAdmin)

        // 6. Now shopkeeper can update real-time photo once, after which permission locks again
        setProfile(
            loginId = "SK_DEEPAK",
            name = "Deepak Tiwari",
            role = "shopkeeper",
            email = "deepak@example.com",
            isVerifiedAdmin = false
        )
        val authorizedUpdate = repository.updateShopkeeperRealTimePhoto(
            shopId = submittedReq.shopId,
            newPhotoUrl = "content://media/new_operator_photo.jpg"
        )
        assertTrue("Updating shopkeeper photo after Admin permission should succeed", authorizedUpdate.isSuccess)
        val updatedShop = authorizedUpdate.getOrThrow()
        assertEquals("content://media/new_operator_photo.jpg", updatedShop.shopkeeperPhotoUrl)
        assertFalse("Permission should reset to false after photo is updated", updatedShop.shopkeeperPhotoChangeApprovedByAdmin)
    }
}

