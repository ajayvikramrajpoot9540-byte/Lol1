package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Shop
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
class PrintingPressShopsTest {

    private lateinit var context: Context
    private lateinit var repository: FlashDropRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FlashDropRepository(context)
    }

    @Test
    fun testGetPrintingPressShops_DynamicFiltering() {
        val printingShops = repository.getPrintingPressShops()

        // Hemu Printing Shop, Sharma Printing Press, R.K. Printing Press must be loaded
        assertTrue("Printing press shops should not be empty", printingShops.isNotEmpty())
        assertTrue("Should include Hemu Printing Shop", printingShops.any { it.id == "hemu_printing" })
        assertTrue("Should include Sharma Printing Press", printingShops.any { it.id == "sharma_printing" })
        assertTrue("Should include R.K. Printing Press", printingShops.any { it.id == "rk_printing" })

        // Grocery shops should be excluded
        assertFalse("Should exclude grocery shop", printingShops.any { it.id == "sharma_general" })
        assertFalse("Should exclude dairy shop", printingShops.any { it.id == "verma_dairy" })
    }

    @Test
    fun testCategoryNormalization_CaseAndSpacing() {
        val sampleList = listOf(
            Shop(id = "shop1", name = "Shop 1", category = "Printing Press", status = "approved"),
            Shop(id = "shop2", name = "Shop 2", category = "printing press", status = "approved"),
            Shop(id = "shop3", name = "Shop 3", category = "  Printing   Press  ", status = "approved"),
            Shop(id = "shop4", name = "Shop 4", category = "PRINTING PRESS", status = "approved"),
            Shop(id = "shop5", name = "Shop 5", category = "Grocery", status = "approved"),
            Shop(id = "shop6", name = "Shop 6", category = "Printing Press", status = "pending"),
            Shop(id = "shop7", name = "Shop 7", category = "Printing Press", status = "rejected")
        )

        val result = repository.getPrintingPressShops(sampleList)

        assertEquals("Only approved printing press shops with variations should match", 4, result.size)
        assertTrue(result.any { it.id == "shop1" })
        assertTrue(result.any { it.id == "shop2" })
        assertTrue(result.any { it.id == "shop3" })
        assertTrue(result.any { it.id == "shop4" })
        assertFalse(result.any { it.id == "shop5" })
        assertFalse(result.any { it.id == "shop6" })
        assertFalse(result.any { it.id == "shop7" })
    }

    @Test
    fun testDynamicAdminApprovalOfNewPrintingPressShop() {
        // Admin approves a newly registered shop
        val newShop = Shop(
            id = "bundelkhand_printers",
            name = "Bundelkhand High-Speed Digital Printers",
            category = "Printing Press",
            rating = 4.9,
            ratingCount = 20,
            distance = "1.5 km",
            deliveryTime = "1 Hour Delivery",
            deliveryAvailable = true,
            isOpen = true,
            status = "approved",
            address = "Alha Chowk, Mahoba",
            phone = "+91 98765 44556"
        )

        // Login as admin and update/add shop
        repository.loginAsMainAdmin("PR12", "PR12")
        val saved = repository.updateShop(newShop)
        assertTrue(saved)

        val updatedPrintingShops = repository.getPrintingPressShops()
        val found = updatedPrintingShops.find { it.id == "bundelkhand_printers" }
        assertNotNull("Newly approved printing press shop must appear dynamically", found)
        assertEquals("Bundelkhand High-Speed Digital Printers", found?.name)
        assertEquals("1.5 km", found?.distance)
    }
}
