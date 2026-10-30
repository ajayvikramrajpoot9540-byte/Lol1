package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents an individual document or image uploaded for printing.
 */
@IgnoreExtraProperties
data class PrintFileItem(
    val id: String = "",
    val uriString: String = "",
    val fileName: String = "",
    val fileExtension: String = "", // "pdf", "jpg", "jpeg", "png"
    val fileSizeBytes: Long = 0L,
    val fileSizeFormatted: String = "",
    val mimeType: String = "",
    val storageUrl: String = "",
    val storagePath: String = "",
    val uploadStatus: String = "ready", // "idle", "uploading", "uploaded", "error"
    val uploadProgress: Int = 100,
    val errorMessage: String = "",
    val detectedPageCount: Int = 1,
    val pageRangeInput: String = "All", // "All", "1-5", "2,4,6", etc.
    val selectedPagesCount: Int = 1,
    val copies: Int = 1,
    val isDoubleSided: Boolean = false,
    val specialNote: String = ""
) {
    /**
     * Total physical prints (impressions) for this file.
     * e.g., 4 pages with 3 copies = 12 physical prints.
     */
    val physicalPrints: Int
        get() = (selectedPagesCount.coerceAtLeast(1)) * (copies.coerceAtLeast(1))

    /**
     * Total physical sheets of A4 paper needed.
     * Single-sided: 1 sheet per print.
     * Double-sided: 2 prints per sheet (rounded up).
     */
    val estimatedSheets: Int
        get() = if (isDoubleSided) {
            (physicalPrints + 1) / 2
        } else {
            physicalPrints
        }
}

/**
 * Complete specification for a Printing Press order.
 * Strictly enforces A4 paper size and Black & White printing.
 */
@IgnoreExtraProperties
data class PrintingOrderSpec(
    val shopId: String = "hemu_printing",
    val shopName: String = "Hemu Printing Shop",
    val files: List<PrintFileItem> = emptyList(),
    val paperSize: String = "A4", // Compulsory, non-changeable
    val colorMode: String = "Black & White (B&W)", // Strictly B&W only, color removed
    val customerInstructions: String = "",
    val totalPhysicalPrints: Int = 0,
    val totalSheets: Int = 0,
    val appliedRatePerPrint: Double = 1.0,
    val printingSubtotal: Double = 0.0,
    val platformFee: Double = 19.0
)

/**
 * Tiers for display in the Printing Price Table.
 */
data class PrintingPriceTier(
    val rangeLabel: String,
    val minPrints: Int,
    val maxPrints: Int,
    val ratePerPrint: Double,
    val isDiscounted: Boolean = false,
    val description: String = ""
)

/**
 * Strict pricing rules and calculation engine for Printing Press.
 * Enforces NO SLAB PRICING:
 * - 1 to 1,000 physical prints: ₹1.00 per print
 * - 1,001+ physical prints: ₹0.70 per print applied to EVERY physical print in the order.
 * Fixed ₹19.00 Platform Fee applied once per Printing Press order.
 */
object PrintingPriceCalculator {

    const val FIXED_PLATFORM_FEE: Double = 19.0
    const val TIER_THRESHOLD_BULK: Int = 1000
    const val BASE_RATE_PER_PRINT: Double = 1.00
    const val BULK_RATE_PER_PRINT: Double = 0.70

    val DISPLAY_TIERS: List<PrintingPriceTier> = listOf(
        PrintingPriceTier(
            rangeLabel = "1 – 100",
            minPrints = 1,
            maxPrints = 100,
            ratePerPrint = 1.00,
            description = "Standard rate"
        ),
        PrintingPriceTier(
            rangeLabel = "101 – 500",
            minPrints = 101,
            maxPrints = 500,
            ratePerPrint = 1.00,
            description = "Standard rate"
        ),
        PrintingPriceTier(
            rangeLabel = "501 – 1,000",
            minPrints = 501,
            maxPrints = 1000,
            ratePerPrint = 1.00,
            description = "Standard rate"
        ),
        PrintingPriceTier(
            rangeLabel = "1,001 – 2,000",
            minPrints = 1001,
            maxPrints = 2000,
            ratePerPrint = 0.70,
            isDiscounted = true,
            description = "Bulk Discount (Flat ₹0.70 for all)"
        ),
        PrintingPriceTier(
            rangeLabel = "2,001 – 5,000",
            minPrints = 2001,
            maxPrints = 5000,
            ratePerPrint = 0.70,
            isDiscounted = true,
            description = "Bulk Discount (Flat ₹0.70 for all)"
        ),
        PrintingPriceTier(
            rangeLabel = "Above 5,000",
            minPrints = 5001,
            maxPrints = Int.MAX_VALUE,
            ratePerPrint = 0.70,
            isDiscounted = true,
            description = "Wholesale rate (Flat ₹0.70 for all)"
        )
    )

    /**
     * Determines applicable rate per physical print.
     * NOT slab pricing: if totalPrints > 1000, ₹0.70 applies to ALL prints.
     */
    fun getApplicableRate(totalPrints: Int): Double {
        return if (totalPrints > TIER_THRESHOLD_BULK) BULK_RATE_PER_PRINT else BASE_RATE_PER_PRINT
    }

    /**
     * Calculates printing subtotal (excluding platform fee).
     * e.g., 999 prints = ₹999.00
     * 1,000 prints = ₹1,000.00
     * 1,001 prints = ₹700.70
     * 1,500 prints = ₹1,050.00
     * 2,000 prints = ₹1,400.00
     */
    fun calculatePrintingSubtotal(totalPrints: Int): Double {
        if (totalPrints <= 0) return 0.0
        val rate = getApplicableRate(totalPrints)
        return (totalPrints * rate * 100.0).toLong() / 100.0 // 2 decimal precision
    }

    /**
     * Total physical prints across all files in the order.
     */
    fun calculateTotalPrints(files: List<PrintFileItem>): Int {
        return files.sumOf { it.physicalPrints }
    }

    /**
     * Total physical sheets across all files in the order.
     */
    fun calculateTotalSheets(files: List<PrintFileItem>): Int {
        return files.sumOf { it.estimatedSheets }
    }

    /**
     * Finds active pricing tier for UI highlighting.
     */
    fun findActiveTier(totalPrints: Int): PrintingPriceTier {
        if (totalPrints <= 0) return DISPLAY_TIERS.first()
        return DISPLAY_TIERS.find {
            totalPrints >= it.minPrints && totalPrints <= it.maxPrints
        } ?: DISPLAY_TIERS.last()
    }

    /**
     * Parses page range string and counts matching valid pages in 1..maxPages.
     * Examples:
     * - "All", "" -> maxPages
     * - "2-5" -> 4 pages (2, 3, 4, 5)
     * - "1, 3, 5" -> 3 pages
     * - "1-3, 5" -> 4 pages (1, 2, 3, 5)
     */
    fun parsePageRangeCount(rangeStr: String, maxPages: Int): Int {
        val clean = rangeStr.trim()
        if (clean.isBlank() || clean.equals("All", ignoreCase = true)) {
            return maxPages.coerceAtLeast(1)
        }

        val pagesSet = mutableSetOf<Int>()
        val parts = clean.split(",", ";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val sub = trimmed.split("-")
                if (sub.size == 2) {
                    val start = sub[0].trim().toIntOrNull()
                    val end = sub[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (p in start..end) {
                            if (p in 1..maxPages) pagesSet.add(p)
                        }
                    }
                }
            } else {
                val single = trimmed.toIntOrNull()
                if (single != null && single in 1..maxPages) {
                    pagesSet.add(single)
                }
            }
        }

        return if (pagesSet.isNotEmpty()) pagesSet.size else maxPages.coerceAtLeast(1)
    }

    /**
     * Formats bytes to human-readable string (KB / MB).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
