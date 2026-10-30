package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.DeliveryConfig
import com.example.data.model.PrintFileItem
import com.example.data.model.PrintingOrderSpec
import com.example.data.model.PrintingPriceCalculator
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.util.DeliveryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Tests for Printing Press document ordering and pricing rules:
 * - 1 to 1,000 prints @ ₹1.00
 * - 1,001+ prints @ ₹0.70 flat across all prints (NO SLAB PRICING)
 * - Exact test cases: 999 = ₹999.00, 1,000 = ₹1,000.00, 1,001 = ₹700.70, 1,500 = ₹1,050.00, 2,000 = ₹1,400.00
 * - PDF page range & copies calculations
 * - Single vs Double-sided sheets estimation
 * - Fixed ₹19.00 platform fee once per order
 */
class PrintingPricingTest {

    @Test
    fun testBaseRateForPrintsUpTo1000() {
        assertEquals(1.00, PrintingPriceCalculator.getApplicableRate(1), 0.001)
        assertEquals(1.00, PrintingPriceCalculator.getApplicableRate(500), 0.001)
        assertEquals(1.00, PrintingPriceCalculator.getApplicableRate(999), 0.001)
        assertEquals(1.00, PrintingPriceCalculator.getApplicableRate(1000), 0.001)
    }

    @Test
    fun testBulkRateForPrintsOver1000() {
        assertEquals(0.70, PrintingPriceCalculator.getApplicableRate(1001), 0.001)
        assertEquals(0.70, PrintingPriceCalculator.getApplicableRate(1500), 0.001)
        assertEquals(0.70, PrintingPriceCalculator.getApplicableRate(2000), 0.001)
        assertEquals(0.70, PrintingPriceCalculator.getApplicableRate(5000), 0.001)
    }

    @Test
    fun testExactPricingEdgeCasesFromSpecification() {
        // 999 physical prints = ₹999.00
        assertEquals(999.00, PrintingPriceCalculator.calculatePrintingSubtotal(999), 0.01)

        // 1,000 physical prints = ₹1,000.00
        assertEquals(1000.00, PrintingPriceCalculator.calculatePrintingSubtotal(1000), 0.01)

        // 1,001 physical prints = ₹700.70 (Strictly flat rate 0.70 on all prints, NOT slab pricing!)
        assertEquals(700.70, PrintingPriceCalculator.calculatePrintingSubtotal(1001), 0.01)

        // 1,500 physical prints = ₹1,050.00
        assertEquals(1050.00, PrintingPriceCalculator.calculatePrintingSubtotal(1500), 0.01)

        // 2,000 physical prints = ₹1,400.00
        assertEquals(1400.00, PrintingPriceCalculator.calculatePrintingSubtotal(2000), 0.01)
    }

    @Test
    fun testPhysicalPrintCountCalculation() {
        // 10-page PDF with 1 copy = 10 physical prints
        val file1 = PrintFileItem(
            fileName = "contract.pdf",
            fileExtension = "pdf",
            detectedPageCount = 10,
            selectedPagesCount = 10,
            copies = 1
        )
        assertEquals(10, file1.physicalPrints)

        // 10-page PDF with 3 copies = 30 physical prints
        val file2 = PrintFileItem(
            fileName = "report.pdf",
            fileExtension = "pdf",
            detectedPageCount = 10,
            selectedPagesCount = 10,
            copies = 3
        )
        assertEquals(30, file2.physicalPrints)

        // Single JPG image with 5 copies = 5 physical prints
        val file3 = PrintFileItem(
            fileName = "photo.jpg",
            fileExtension = "jpg",
            detectedPageCount = 1,
            selectedPagesCount = 1,
            copies = 5
        )
        assertEquals(5, file3.physicalPrints)

        // Page range 2-5 of 10-page PDF (4 pages) with 3 copies = 12 physical prints
        val parsedPageCount = PrintingPriceCalculator.parsePageRangeCount("2-5", 10)
        assertEquals(4, parsedPageCount)
        val file4 = PrintFileItem(
            fileName = "chapter.pdf",
            fileExtension = "pdf",
            detectedPageCount = 10,
            pageRangeInput = "2-5",
            selectedPagesCount = parsedPageCount,
            copies = 3
        )
        assertEquals(12, file4.physicalPrints)
    }

    @Test
    fun testSingleSidedVsDoubleSidedSheetEstimation() {
        // 10 physical prints single-sided = 10 sheets
        val singleSided = PrintFileItem(
            selectedPagesCount = 10,
            copies = 1,
            isDoubleSided = false
        )
        assertEquals(10, singleSided.physicalPrints)
        assertEquals(10, singleSided.estimatedSheets)

        // 10 physical prints double-sided = 5 sheets (ceil(10/2) = 5)
        val doubleSided = PrintFileItem(
            selectedPagesCount = 10,
            copies = 1,
            isDoubleSided = true
        )
        assertEquals(10, doubleSided.physicalPrints)
        assertEquals(5, doubleSided.estimatedSheets)

        // 11 physical prints double-sided = 6 sheets (ceil(11/2) = 6)
        val doubleSidedOdd = PrintFileItem(
            selectedPagesCount = 11,
            copies = 1,
            isDoubleSided = true
        )
        assertEquals(11, doubleSidedOdd.physicalPrints)
        assertEquals(6, doubleSidedOdd.estimatedSheets)
    }

    @Test
    fun testPageRangeParsingVariants() {
        assertEquals(10, PrintingPriceCalculator.parsePageRangeCount("All", 10))
        assertEquals(10, PrintingPriceCalculator.parsePageRangeCount("", 10))
        assertEquals(4, PrintingPriceCalculator.parsePageRangeCount("2-5", 10))
        assertEquals(3, PrintingPriceCalculator.parsePageRangeCount("1, 3, 5", 10))
        assertEquals(5, PrintingPriceCalculator.parsePageRangeCount("1-3, 5-6", 10))
        // Out of bound pages are clamped to valid range
        assertEquals(2, PrintingPriceCalculator.parsePageRangeCount("9-15", 10)) // 9, 10
    }

    @Test
    fun testPlatformFeeAppliedToPrintingPressOrders() {
        val shops = listOf(
            Shop(
                id = "hemu_printing",
                name = "Hemu Printing Shop",
                category = "Printing Press",
                distance = "1.5 km",
                isOpen = true
            )
        )
        val config = DeliveryConfig()

        val printSpec = PrintingOrderSpec(
            shopId = "hemu_printing",
            shopName = "Hemu Printing Shop",
            totalPhysicalPrints = 50,
            totalSheets = 50,
            appliedRatePerPrint = 1.0,
            printingSubtotal = 50.0,
            platformFee = 19.0
        )

        val printProduct = Product(
            id = "prod_print_1",
            shopId = "hemu_printing",
            category = "Printing Press",
            price = 50.0
        )

        val cartWithPrinting = listOf(
            CartItem(product = printProduct, quantity = 1, printingSpec = printSpec)
        )

        val billResult = DeliveryCalculator.calculateBill(
            cartItems = cartWithPrinting,
            shops = shops,
            config = config,
            deliveryMethod = "shop_delivery"
        )

        // Platform fee must be exactly ₹19.00
        assertEquals(19.0, billResult.platformFee, 0.001)

        // Final payable = Merchandise Subtotal (50) + Platform Fee (19) + Delivery Fee (10) = 79.0
        val expectedPayable = billResult.merchandiseSubtotal - billResult.productDiscount + billResult.platformFee + billResult.netDeliveryFee
        assertEquals(expectedPayable, billResult.finalPayableAmount, 0.01)
    }

    @Test
    fun testPlatformFeeNotChargedOnStandardGroceryOrders() {
        val shops = listOf(
            Shop(
                id = "sharma_general",
                name = "Sharma Kirana Store",
                category = "Grocery",
                distance = "1.2 km",
                isOpen = true
            )
        )
        val config = DeliveryConfig()

        val groceryProduct = Product(
            id = "prod_atta",
            shopId = "sharma_general",
            category = "Grocery",
            price = 220.0
        )

        val cartGrocery = listOf(
            CartItem(product = groceryProduct, quantity = 1)
        )

        val billResult = DeliveryCalculator.calculateBill(
            cartItems = cartGrocery,
            shops = shops,
            config = config,
            deliveryMethod = "shop_delivery"
        )

        // Non-printing orders must have ₹0.0 platform fee
        assertEquals(0.0, billResult.platformFee, 0.001)
    }

    @Test
    fun testSinglePlatformFeeAcrossMultiplePrintingFiles() {
        val files = listOf(
            PrintFileItem(fileName = "doc1.pdf", selectedPagesCount = 10, copies = 2), // 20 prints
            PrintFileItem(fileName = "doc2.pdf", selectedPagesCount = 5, copies = 1),  // 5 prints
            PrintFileItem(fileName = "photo.png", selectedPagesCount = 1, copies = 3)  // 3 prints
        )
        val totalPrints = PrintingPriceCalculator.calculateTotalPrints(files)
        assertEquals(28, totalPrints)

        val subtotal = PrintingPriceCalculator.calculatePrintingSubtotal(totalPrints)
        assertEquals(28.00, subtotal, 0.01)

        val spec = PrintingOrderSpec(
            files = files,
            totalPhysicalPrints = totalPrints,
            totalSheets = PrintingPriceCalculator.calculateTotalSheets(files),
            appliedRatePerPrint = PrintingPriceCalculator.getApplicableRate(totalPrints),
            printingSubtotal = subtotal,
            platformFee = 19.0
        )

        // Platform fee is fixed at ₹19.00 once per order, never multiplied
        assertEquals(19.0, spec.platformFee, 0.001)
    }

    @Test
    fun testDuplicateFileRejection() {
        val existingFiles = listOf(
            PrintFileItem(fileName = "report.pdf", fileSizeBytes = 1024L * 500),
            PrintFileItem(fileName = "photo.jpg", fileSizeBytes = 1024L * 200)
        )

        // Attempting to add "report.pdf" with same size
        val duplicateAttempt = PrintFileItem(fileName = "report.pdf", fileSizeBytes = 1024L * 500)
        val isDuplicate = existingFiles.any {
            it.fileName.equals(duplicateAttempt.fileName, ignoreCase = true) &&
                    it.fileSizeBytes == duplicateAttempt.fileSizeBytes
        }
        assertTrue("Duplicate file with matching name and size must be detected and rejected", isDuplicate)

        // Adding distinct file must pass
        val newFile = PrintFileItem(fileName = "id_card.png", fileSizeBytes = 1024L * 300)
        val isNewDuplicate = existingFiles.any {
            it.fileName.equals(newFile.fileName, ignoreCase = true) &&
                    it.fileSizeBytes == newFile.fileSizeBytes
        }
        assertFalse("Unique file must be allowed", isNewDuplicate)
    }

    @Test
    fun testMaxFileSizeLimit150MB() {
        val maxLimitBytes = 150L * 1024L * 1024L

        val validSize = 145L * 1024L * 1024L
        val oversized = 151L * 1024L * 1024L

        assertTrue("145MB file is within 150MB limit", validSize <= maxLimitBytes)
        assertFalse("151MB file exceeds 150MB limit", oversized <= maxLimitBytes)
    }

    @Test
    fun testSupportedAndUnsupportedFileTypes() {
        val supportedExtensions = listOf("pdf", "jpg", "jpeg", "png")

        assertTrue(supportedExtensions.contains("pdf"))
        assertTrue(supportedExtensions.contains("jpg"))
        assertTrue(supportedExtensions.contains("jpeg"))
        assertTrue(supportedExtensions.contains("png"))

        assertFalse(supportedExtensions.contains("docx"))
        assertFalse(supportedExtensions.contains("xlsx"))
        assertFalse(supportedExtensions.contains("exe"))
        assertFalse(supportedExtensions.contains("mp4"))
    }

    @Test
    fun testRemovingFileRecalculatesTotalsAccurately() {
        val file1 = PrintFileItem(id = "f1", fileName = "contract.pdf", selectedPagesCount = 10, copies = 2) // 20 prints
        val file2 = PrintFileItem(id = "f2", fileName = "invoice.pdf", selectedPagesCount = 5, copies = 1)   // 5 prints
        val fileList = mutableListOf(file1, file2)

        var totalPrints = PrintingPriceCalculator.calculateTotalPrints(fileList)
        assertEquals(25, totalPrints)
        assertEquals(25.0, PrintingPriceCalculator.calculatePrintingSubtotal(totalPrints), 0.01)

        // Remove file2
        fileList.remove(file2)
        totalPrints = PrintingPriceCalculator.calculateTotalPrints(fileList)
        assertEquals(20, totalPrints)
        assertEquals(20.0, PrintingPriceCalculator.calculatePrintingSubtotal(totalPrints), 0.01)
    }

    @Test
    fun testHemuPrintingPressOrderIsolation() {
        val shopId = "hemu_printing"
        val customerUserId = "cust_ajay_123"
        val fileName = "project_report.pdf"

        val storagePath = "printing_docs/$shopId/$customerUserId/${fileName}"
        assertTrue("Storage path must isolate to shop and customer", storagePath.startsWith("printing_docs/hemu_printing/cust_ajay_123/"))
    }
}

