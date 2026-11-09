package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.Shop
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentReportPeriod(
    val key: String,
    val label: String,
    val shortLabel: String,
    val maxDaysAgo: Int
) {
    ONE_DAY("1D", "1 Day (Today)", "1 Day", 1),
    SEVEN_DAYS("7D", "7 Days", "7 Days", 7),
    ONE_MONTH("1M", "1 Month (30 Days)", "1 Month", 30),
    SIX_MONTHS("6M", "6 Months", "6 Months", 180),
    ONE_YEAR("1Y", "1 Year (365 Days)", "1 Year", 365);

    companion object {
        fun fromKey(key: String): PaymentReportPeriod =
            entries.find { it.key.equals(key, ignoreCase = true) || it.shortLabel.equals(key, ignoreCase = true) }
                ?: SEVEN_DAYS
    }
}

enum class ReportExportType(val title: String) {
    TOTAL_PAYMENTS("Total Payments & Shop Breakdown Report"),
    SHOP_SPECIFIC("Shop Payment & Settlement Statement"),
    PROFIT_AND_LOSS("Profit & Loss (P&L) & Multi-Period Financial Report")
}

data class PeriodFinancialSummary(
    val period: PaymentReportPeriod,
    val shopFilterLabel: String,
    val totalOrdersCount: Int,
    val totalOrdersAmount: Double,
    val onlineOrdersCount: Int,
    val onlineAmount: Double,
    val codOrdersCount: Int,
    val codAmount: Double,
    val pendingOrdersCount: Int,
    val pendingAmount: Double,
    val failedOrRefundedCount: Int,
    val failedOrRefundedAmount: Double,
    val onlineProblemOccurrences: Int,
    val platformCommissionEarned: Double,
    val deliveryFeeCollected: Double,
    val grossPlatformRevenue: Double,
    val discountAndSubsidyCost: Double,
    val refundAndFailureLoss: Double,
    val totalExpensesAndLosses: Double,
    val netProfitOrLoss: Double,
    val shopkeeperNetPayoutTotal: Double,
    val shopkeeperPendingPayout: Double
) {
    val isProfitable: Boolean get() = netProfitOrLoss >= 0.0
    val profitMarginPercent: Double
        get() = if (totalOrdersAmount > 0.0) (netProfitOrLoss / totalOrdersAmount) * 100.0 else 0.0
}

data class ShopPaymentBreakdown(
    val shopId: String,
    val shopName: String,
    val category: String,
    val totalOrdersCount: Int,
    val totalOrdersAmount: Double,
    val orderSharePercent: Float,
    val onlineOrdersCount: Int,
    val onlineAmount: Double,
    val codOrdersCount: Int,
    val codAmount: Double,
    val pendingOrdersCount: Int,
    val pendingAmount: Double,
    val shopkeeperPayoutDue: Double,
    val shopkeeperSettledAmount: Double,
    val onlineProblemOccurrences: Int,
    val platformProfitFromShop: Double
)

data class OnlinePaymentProblemStat(
    val entityId: String,
    val entityName: String,
    val secondaryLabel: String, // Shop Name or Customer Phone
    val problemOccurrences: Int,
    val failedTransactionsCount: Int,
    val affectedAmount: Double,
    val topProblemReasons: List<String>,
    val latestTimestamp: String
)

object AdminPaymentReportExporter {

    fun filterTransactionsByPeriodAndShop(
        transactions: List<Transaction>,
        period: PaymentReportPeriod,
        selectedShopId: String = "ALL"
    ): List<Transaction> {
        return transactions.filter { txn ->
            val withinPeriod = txn.daysAgo <= period.maxDaysAgo
            val matchesShop = selectedShopId == "ALL" ||
                    txn.shopId.equals(selectedShopId, ignoreCase = true) ||
                    txn.shopName.equals(selectedShopId, ignoreCase = true)
            withinPeriod && matchesShop
        }
    }

    fun computePeriodSummary(
        transactions: List<Transaction>,
        period: PaymentReportPeriod,
        selectedShopId: String = "ALL",
        shopFilterLabel: String = "All Shops"
    ): PeriodFinancialSummary {
        val filtered = filterTransactionsByPeriodAndShop(transactions, period, selectedShopId)
        val totalOrdersCount = filtered.size
        val totalOrdersAmount = filtered.sumOf { it.amount }

        val onlineTxns = filtered.filter { it.isOnlinePayment }
        val onlineOrdersCount = onlineTxns.size
        val onlineAmount = onlineTxns.sumOf { it.amount }

        val codTxns = filtered.filter { it.isCodPayment }
        val codOrdersCount = codTxns.size
        val codAmount = codTxns.sumOf { it.amount }

        val pendingTxns = filtered.filter { it.isPendingPayment }
        val pendingOrdersCount = pendingTxns.size
        val pendingAmount = pendingTxns.sumOf { it.amount }

        val failedOrRefunded = filtered.filter {
            it.status.equals("Failed", ignoreCase = true) || it.status.equals("Refunded", ignoreCase = true)
        }
        val failedOrRefundedCount = failedOrRefunded.size
        val failedOrRefundedAmount = failedOrRefunded.sumOf { it.amount }

        val onlineProblemOccurrences = filtered.sumOf {
            if (it.onlineProblemCount > 0) it.onlineProblemCount
            else if (it.hasOnlinePaymentProblem) 1
            else 0
        }

        val successfulTxns = filtered.filterNot {
            it.status.equals("Failed", ignoreCase = true) || it.status.equals("Refunded", ignoreCase = true)
        }
        val platformCommissionEarned = successfulTxns.sumOf { it.effectivePlatformCommission }
        val deliveryFeeCollected = successfulTxns.sumOf { it.deliveryFee }
        val grossPlatformRevenue = platformCommissionEarned + deliveryFeeCollected

        val discountAndSubsidyCost = successfulTxns.sumOf { it.discountSubsidy }
        val refundAndFailureLoss = failedOrRefunded.sumOf {
            when (it.status.lowercase()) {
                "failed" -> it.discountSubsidy.coerceAtLeast(12.0)
                "refunded" -> it.discountSubsidy + 18.0
                else -> 0.0
            }
        }
        val totalExpensesAndLosses = discountAndSubsidyCost + refundAndFailureLoss
        val netProfitOrLoss = grossPlatformRevenue - totalExpensesAndLosses

        val shopkeeperNetPayoutTotal = successfulTxns.sumOf { it.effectiveShopkeeperPayout }
        val shopkeeperPendingPayout = successfulTxns
            .filter { it.isPendingPayment }
            .sumOf { it.effectiveShopkeeperPayout }

        return PeriodFinancialSummary(
            period = period,
            shopFilterLabel = shopFilterLabel,
            totalOrdersCount = totalOrdersCount,
            totalOrdersAmount = totalOrdersAmount,
            onlineOrdersCount = onlineOrdersCount,
            onlineAmount = onlineAmount,
            codOrdersCount = codOrdersCount,
            codAmount = codAmount,
            pendingOrdersCount = pendingOrdersCount,
            pendingAmount = pendingAmount,
            failedOrRefundedCount = failedOrRefundedCount,
            failedOrRefundedAmount = failedOrRefundedAmount,
            onlineProblemOccurrences = onlineProblemOccurrences,
            platformCommissionEarned = platformCommissionEarned,
            deliveryFeeCollected = deliveryFeeCollected,
            grossPlatformRevenue = grossPlatformRevenue,
            discountAndSubsidyCost = discountAndSubsidyCost,
            refundAndFailureLoss = refundAndFailureLoss,
            totalExpensesAndLosses = totalExpensesAndLosses,
            netProfitOrLoss = netProfitOrLoss,
            shopkeeperNetPayoutTotal = shopkeeperNetPayoutTotal,
            shopkeeperPendingPayout = shopkeeperPendingPayout
        )
    }

    fun computeAllPeriodSummaries(
        transactions: List<Transaction>,
        selectedShopId: String = "ALL",
        shopFilterLabel: String = "All Shops"
    ): List<PeriodFinancialSummary> {
        return PaymentReportPeriod.entries.map { period ->
            computePeriodSummary(transactions, period, selectedShopId, shopFilterLabel)
        }
    }

    fun computeShopBreakdowns(
        transactions: List<Transaction>,
        shops: List<Shop>,
        period: PaymentReportPeriod
    ): List<ShopPaymentBreakdown> {
        val periodTxns = filterTransactionsByPeriodAndShop(transactions, period, "ALL")
        val totalPeriodOrders = periodTxns.size.coerceAtLeast(1)

        val shopMap = shops.associateBy { it.id }
        val groupedByShop = periodTxns.groupBy { it.shopId.ifBlank { it.shopName } }

        val allShopKeys = (shops.map { it.id } + groupedByShop.keys).distinct()

        return allShopKeys.mapNotNull { key ->
            val shopObj = shopMap[key] ?: shops.find { it.name.equals(key, ignoreCase = true) }
            val txns = groupedByShop[key] ?: emptyList()
            if (txns.isEmpty() && shopObj == null) return@mapNotNull null

            val resolvedName = shopObj?.name ?: txns.firstOrNull()?.shopName ?: key
            val resolvedCategory = shopObj?.category ?: "Local Store"

            val totalOrders = txns.size
            val totalAmount = txns.sumOf { it.amount }
            val sharePct = if (periodTxns.isNotEmpty()) (totalOrders.toFloat() / totalPeriodOrders.toFloat()) * 100f else 0f

            val onlineTxns = txns.filter { it.isOnlinePayment }
            val codTxns = txns.filter { it.isCodPayment }
            val pendingTxns = txns.filter { it.isPendingPayment }

            val validTxns = txns.filterNot {
                it.status.equals("Failed", ignoreCase = true) || it.status.equals("Refunded", ignoreCase = true)
            }
            val duePayout = validTxns.filter { it.isPendingPayment }.sumOf { it.effectiveShopkeeperPayout }
            val settledPayout = validTxns.filterNot { it.isPendingPayment }.sumOf { it.effectiveShopkeeperPayout }

            val problemCount = txns.sumOf {
                if (it.onlineProblemCount > 0) it.onlineProblemCount
                else if (it.hasOnlinePaymentProblem) 1
                else 0
            }
            val profit = txns.sumOf { it.netPlatformProfitOrLoss }

            ShopPaymentBreakdown(
                shopId = shopObj?.id ?: key,
                shopName = resolvedName,
                category = resolvedCategory,
                totalOrdersCount = totalOrders,
                totalOrdersAmount = totalAmount,
                orderSharePercent = sharePct,
                onlineOrdersCount = onlineTxns.size,
                onlineAmount = onlineTxns.sumOf { it.amount },
                codOrdersCount = codTxns.size,
                codAmount = codTxns.sumOf { it.amount },
                pendingOrdersCount = pendingTxns.size,
                pendingAmount = pendingTxns.sumOf { it.amount },
                shopkeeperPayoutDue = duePayout,
                shopkeeperSettledAmount = settledPayout,
                onlineProblemOccurrences = problemCount,
                platformProfitFromShop = profit
            )
        }.sortedWith(
            compareByDescending<ShopPaymentBreakdown> { it.totalOrdersCount }
                .thenByDescending { it.totalOrdersAmount }
        )
    }

    fun computeOnlineProblemsByShop(
        transactions: List<Transaction>,
        period: PaymentReportPeriod
    ): List<OnlinePaymentProblemStat> {
        val problemTxns = filterTransactionsByPeriodAndShop(transactions, period, "ALL")
            .filter { it.hasOnlinePaymentProblem }

        return problemTxns.groupBy { it.shopId.ifBlank { it.shopName } }
            .map { (shopKey, list) ->
                val occurrences = list.sumOf { if (it.onlineProblemCount > 0) it.onlineProblemCount else 1 }
                val reasons = list.mapNotNull {
                    it.onlineProblemType.ifBlank { it.failureReason }.takeIf { r -> r.isNotBlank() }
                }.distinct()
                OnlinePaymentProblemStat(
                    entityId = shopKey,
                    entityName = list.first().shopName,
                    secondaryLabel = "${list.size} affected orders",
                    problemOccurrences = occurrences,
                    failedTransactionsCount = list.size,
                    affectedAmount = list.sumOf { it.amount },
                    topProblemReasons = if (reasons.isEmpty()) listOf("Gateway / Bank Timeout") else reasons,
                    latestTimestamp = list.first().timestamp
                )
            }
            .sortedByDescending { it.problemOccurrences }
    }

    fun computeOnlineProblemsByCustomer(
        transactions: List<Transaction>,
        period: PaymentReportPeriod
    ): List<OnlinePaymentProblemStat> {
        val problemTxns = filterTransactionsByPeriodAndShop(transactions, period, "ALL")
            .filter { it.hasOnlinePaymentProblem }

        return problemTxns.groupBy { it.customerName }
            .map { (customerName, list) ->
                val occurrences = list.sumOf { if (it.onlineProblemCount > 0) it.onlineProblemCount else 1 }
                val shopsInvolved = list.map { it.shopName }.distinct().joinToString(", ")
                val reasons = list.mapNotNull {
                    it.onlineProblemType.ifBlank { it.failureReason }.takeIf { r -> r.isNotBlank() }
                }.distinct()
                OnlinePaymentProblemStat(
                    entityId = customerName,
                    entityName = customerName,
                    secondaryLabel = "Shops: $shopsInvolved",
                    problemOccurrences = occurrences,
                    failedTransactionsCount = list.size,
                    affectedAmount = list.sumOf { it.amount },
                    topProblemReasons = if (reasons.isEmpty()) listOf("UPI Verification Issue") else reasons,
                    latestTimestamp = list.first().timestamp
                )
            }
            .sortedByDescending { it.problemOccurrences }
    }

    /**
     * Generates a clean, comprehensive CSV string for preview and export.
     */
    fun generateCsvContent(
        exportType: ReportExportType,
        period: PaymentReportPeriod,
        selectedShopId: String,
        selectedShopName: String,
        transactions: List<Transaction>,
        shops: List<Shop>
    ): String {
        val sb = StringBuilder()
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val summary = computePeriodSummary(transactions, period, selectedShopId, selectedShopName)
        val allPeriodSummaries = computeAllPeriodSummaries(transactions, selectedShopId, selectedShopName)
        val shopBreakdowns = computeShopBreakdowns(transactions, shops, period)
        val filteredTxns = filterTransactionsByPeriodAndShop(transactions, period, selectedShopId)

        sb.appendLine("FLASHDROP MAHOBA - ADMIN FINANCIAL & PAYMENTS REPORT")
        sb.appendLine("Report Type,${escapeCsv(exportType.title)}")
        sb.appendLine("Selected Time Period,${escapeCsv(period.label)}")
        sb.appendLine("Shop Scope,${escapeCsv(selectedShopName)}")
        sb.appendLine("Generated At,${escapeCsv(nowStr)}")
        sb.appendLine()

        // Section 1: Selected Period Summary
        sb.appendLine("--- 1. SELECTED PERIOD SUMMARY (${period.label}) ---")
        sb.appendLine("Metric,Orders Count,Amount (INR)")
        sb.appendLine("Total Orders,${summary.totalOrdersCount},${summary.totalOrdersAmount.toInt()}")
        sb.appendLine("Online Payments (UPI),${summary.onlineOrdersCount},${summary.onlineAmount.toInt()}")
        sb.appendLine("COD Payments,${summary.codOrdersCount},${summary.codAmount.toInt()}")
        sb.appendLine("Pending Payments,${summary.pendingOrdersCount},${summary.pendingAmount.toInt()}")
        sb.appendLine("Online Payment Problem Occurrences,${summary.onlineProblemOccurrences},${summary.failedOrRefundedAmount.toInt()}")
        sb.appendLine("Shopkeeper Net Payout Total,${summary.totalOrdersCount - summary.failedOrRefundedCount},${summary.shopkeeperNetPayoutTotal.toInt()}")
        sb.appendLine("Shopkeeper Pending Payout Due,${summary.pendingOrdersCount},${summary.shopkeeperPendingPayout.toInt()}")
        sb.appendLine()

        // Section 2: Profit & Loss across 1 Day, 7 Days, 1 Month, 6 Months, 1 Year
        sb.appendLine("--- 2. MULTI-PERIOD TOTAL SUMMARY & PROFIT / LOSS (1D / 7D / 1M / 6M / 1Y) ---")
        sb.appendLine("Time Period,Total Orders,Online Payments (INR),COD Payments (INR),Pending Payments (INR),Gross GMV (INR),Platform Commission (INR),Delivery Fees (INR),Subsidies & Refund Losses (INR),Net Profit / Loss (INR),Status")
        allPeriodSummaries.forEach { p ->
            val statusLabel = if (p.netProfitOrLoss >= 0) "PROFIT" else "LOSS"
            sb.appendLine(
                "${escapeCsv(p.period.label)},${p.totalOrdersCount},${p.onlineAmount.toInt()},${p.codAmount.toInt()},${p.pendingAmount.toInt()},${p.totalOrdersAmount.toInt()},${p.platformCommissionEarned.toInt()},${p.deliveryFeeCollected.toInt()},${p.totalExpensesAndLosses.toInt()},${p.netProfitOrLoss.toInt()},$statusLabel"
            )
        }
        sb.appendLine()

        // Section 3: Shop-wise Breakdown
        sb.appendLine("--- 3. SHOP-WISE ORDERS, ONLINE, COD & PENDING PAYMENT BREAKDOWN (${period.shortLabel}) ---")
        sb.appendLine("Rank,Shop Name,Category,Total Orders,Order Share %,Total GMV (INR),Online Orders,Online (INR),COD Orders,COD (INR),Pending Orders,Pending (INR),Shopkeeper Payout Due (INR),Online Payment Problems,Platform Net P&L (INR)")
        val targetShops = if (selectedShopId == "ALL") shopBreakdowns else shopBreakdowns.filter {
            it.shopId.equals(selectedShopId, ignoreCase = true) || it.shopName.equals(selectedShopName, ignoreCase = true)
        }
        targetShops.forEachIndexed { idx, s ->
            sb.appendLine(
                "${idx + 1},${escapeCsv(s.shopName)},${escapeCsv(s.category)},${s.totalOrdersCount},${String.format(Locale.US, "%.1f%%", s.orderSharePercent)},${s.totalOrdersAmount.toInt()},${s.onlineOrdersCount},${s.onlineAmount.toInt()},${s.codOrdersCount},${s.codAmount.toInt()},${s.pendingOrdersCount},${s.pendingAmount.toInt()},${s.shopkeeperPayoutDue.toInt()},${s.onlineProblemOccurrences},${s.platformProfitFromShop.toInt()}"
            )
        }
        sb.appendLine()

        // Section 4: Online Payment Problems
        val problemTxns = filteredTxns.filter { it.hasOnlinePaymentProblem }
        sb.appendLine("--- 4. ONLINE PAYMENT PROBLEMS LOG (SHOP & CUSTOMER INCIDENTS) ---")
        sb.appendLine("Order Number,Shop Name,Customer Name,Amount (INR),Status,Problem Occurrences,Problem Type / Reason,Timestamp")
        problemTxns.forEach { t ->
            val count = if (t.onlineProblemCount > 0) t.onlineProblemCount else 1
            val reason = t.onlineProblemType.ifBlank { t.failureReason.ifBlank { "Gateway Issue" } }
            sb.appendLine(
                "${escapeCsv(t.orderNumber)},${escapeCsv(t.shopName)},${escapeCsv(t.customerName)},${t.amount.toInt()},${escapeCsv(t.status)},$count,${escapeCsv(reason)},${escapeCsv(t.timestamp)}"
            )
        }
        sb.appendLine()

        // Section 5: Shop <-> Customer Transaction History
        sb.appendLine("--- 5. SHOP & CUSTOMER TRANSACTION LEDGER (${period.shortLabel}) ---")
        sb.appendLine("Order Number,Customer Name,Shop Name,Payment Method,Amount (INR),Payment Status,Shopkeeper Settlement,Net Shopkeeper Payout (INR),Platform P&L (INR),Transaction ID,Timestamp")
        filteredTxns.forEach { t ->
            sb.appendLine(
                "${escapeCsv(t.orderNumber)},${escapeCsv(t.customerName)},${escapeCsv(t.shopName)},${escapeCsv(t.paymentMethod)},${t.amount.toInt()},${escapeCsv(t.status)},${escapeCsv(t.shopkeeperSettlementStatus)},${t.effectiveShopkeeperPayout.toInt()},${t.netPlatformProfitOrLoss.toInt()},${escapeCsv(t.providerTxnId)},${escapeCsv(t.timestamp)}"
            )
        }

        return sb.toString()
    }

    /**
     * Generates a real, multi-section PDF document using Android's PdfDocument API
     * and writes it to internal storage (and optionally to a user-chosen SAF Uri).
     */
    fun generatePdfFile(
        context: Context,
        exportType: ReportExportType,
        period: PaymentReportPeriod,
        selectedShopId: String,
        selectedShopName: String,
        transactions: List<Transaction>,
        shops: List<Shop>,
        destinationUri: Uri? = null
    ): File? {
        return try {
            val reportsDir = File(context.filesDir, "reports").apply {
                if (!exists()) mkdirs()
            }
            val safeShopSlug = selectedShopName.replace(Regex("[^A-Za-z0-9]"), "_").take(20)
            val fileName = "FlashDrop_${exportType.name.lowercase()}_${period.key}_${safeShopSlug}.pdf"
            val outFile = File(reportsDir, fileName)

            val summary = computePeriodSummary(transactions, period, selectedShopId, selectedShopName)
            val allPeriodSummaries = computeAllPeriodSummaries(transactions, selectedShopId, selectedShopName)
            val shopBreakdowns = computeShopBreakdowns(transactions, shops, period)
            val filteredTxns = filterTransactionsByPeriodAndShop(transactions, period, selectedShopId)
            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())

            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            var y = 40f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(8, 116, 67)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 27, 75)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(55, 65, 81)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(31, 41, 55)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(8, 116, 67)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(229, 231, 235)
                strokeWidth = 1f
            }

            fun checkNewPage(requiredSpace: Float = 36f) {
                if (y + requiredSpace > pageHeight - 40f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                }
            }

            // Header Banner
            canvas.drawText("FLASHDROP MAHOBA — ADMIN PAYMENTS & P&L REPORT", 36f, y, titlePaint)
            y += 18f
            canvas.drawText("Report: ${exportType.title}  |  Period: ${period.label}  |  Scope: $selectedShopName", 36f, y, headerPaint)
            y += 14f
            canvas.drawText("Generated: $nowStr  |  Authorized Main Admins: Pranjal Rajpoot (PR12) & Chanchal Didi (CD12)", 36f, y, bodyPaint)
            y += 10f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 18f

            // Section 1: Executive Summary
            canvas.drawText("1. EXECUTIVE PAYMENT SUMMARY (${period.label} • $selectedShopName)", 36f, y, sectionPaint)
            y += 15f
            canvas.drawText(
                "• Total Orders: ${summary.totalOrdersCount} (Rs.${summary.totalOrdersAmount.toInt()})   |   Online (UPI): ${summary.onlineOrdersCount} (Rs.${summary.onlineAmount.toInt()})   |   COD: ${summary.codOrdersCount} (Rs.${summary.codAmount.toInt()})",
                36f, y, bodyPaint
            )
            y += 14f
            canvas.drawText(
                "• Pending Payments: ${summary.pendingOrdersCount} (Rs.${summary.pendingAmount.toInt()})   |   Shopkeeper Payout Due: Rs.${summary.shopkeeperPendingPayout.toInt()}   |   Online Payment Issues: ${summary.onlineProblemOccurrences}",
                36f, y, bodyPaint
            )
            y += 14f
            val pnlStatus = if (summary.netProfitOrLoss >= 0) "NET PROFIT: +Rs.${summary.netProfitOrLoss.toInt()}" else "NET LOSS: -Rs.${kotlin.math.abs(summary.netProfitOrLoss.toInt())}"
            canvas.drawText(
                "• Platform Commission: Rs.${summary.platformCommissionEarned.toInt()}  |  Delivery Fees: Rs.${summary.deliveryFeeCollected.toInt()}  |  Subsidies/Losses: Rs.${summary.totalExpensesAndLosses.toInt()}  |  $pnlStatus",
                36f, y, accentPaint
            )
            y += 18f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 18f

            // Section 2: Multi-Period Summary (1 Day, 7 Days, 1 Month, 6 Months, 1 Year)
            canvas.drawText("2. TOTAL SUMMARY & PROFIT/LOSS ACROSS 1 DAY, 7 DAYS, 1 MONTH, 6 MONTHS & 1 YEAR", 36f, y, sectionPaint)
            y += 15f
            canvas.drawText("Period            Orders    Online (UPI)    COD Amount    Pending      Gross GMV    Expenses    Net Profit/Loss", 36f, y, headerPaint)
            y += 12f
            allPeriodSummaries.forEach { p ->
                checkNewPage(16f)
                val row = String.format(
                    Locale.US,
                    "%-16s  %-8d  Rs.%-11d  Rs.%-9d  Rs.%-8d  Rs.%-9d  Rs.%-7d  %sRs.%d",
                    p.period.shortLabel,
                    p.totalOrdersCount,
                    p.onlineAmount.toInt(),
                    p.codAmount.toInt(),
                    p.pendingAmount.toInt(),
                    p.totalOrdersAmount.toInt(),
                    p.totalExpensesAndLosses.toInt(),
                    if (p.netProfitOrLoss >= 0) "+" else "-",
                    kotlin.math.abs(p.netProfitOrLoss.toInt())
                )
                canvas.drawText(row, 36f, y, bodyPaint)
                y += 13f
            }
            y += 10f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 18f

            // Section 3: Shop-wise Breakdown
            checkNewPage(40f)
            canvas.drawText("3. SHOP-WISE ORDERS, ONLINE, COD & PENDING SETTLEMENT BREAKDOWN (${period.shortLabel})", 36f, y, sectionPaint)
            y += 15f
            canvas.drawText("Shop Name                     Orders(Share)   Online(Rs)   COD(Rs)     Pending(Rs)   Payout Due   Problems", 36f, y, headerPaint)
            y += 12f
            val shopsToRender = if (selectedShopId == "ALL") shopBreakdowns else shopBreakdowns.filter {
                it.shopId.equals(selectedShopId, ignoreCase = true) || it.shopName.equals(selectedShopName, ignoreCase = true)
            }
            shopsToRender.forEach { s ->
                checkNewPage(16f)
                val row = String.format(
                    Locale.US,
                    "%-28s  %2d (%.0f%%)       Rs.%-8d  Rs.%-7d  Rs.%-9d  Rs.%-8d  %d",
                    s.shopName.take(27),
                    s.totalOrdersCount,
                    s.orderSharePercent,
                    s.onlineAmount.toInt(),
                    s.codAmount.toInt(),
                    s.pendingAmount.toInt(),
                    s.shopkeeperPayoutDue.toInt(),
                    s.onlineProblemOccurrences
                )
                canvas.drawText(row, 36f, y, bodyPaint)
                y += 13f
            }
            y += 10f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 18f

            // Section 4: Shop <-> Customer Transaction History
            checkNewPage(40f)
            canvas.drawText("4. SHOP & CUSTOMER TRANSACTION LEDGER (${filteredTxns.size} Records)", 36f, y, sectionPaint)
            y += 15f
            canvas.drawText("Order #           Customer          Shop Name               Method   Amount    Status     P&L     Timestamp", 36f, y, headerPaint)
            y += 12f
            filteredTxns.forEach { t ->
                checkNewPage(16f)
                val row = String.format(
                    Locale.US,
                    "%-16s  %-16s  %-22s  %-6s   Rs.%-5d  %-9s  %sRs.%-3d %s",
                    t.orderNumber.take(16),
                    t.customerName.take(15),
                    t.shopName.take(21),
                    t.paymentMethod,
                    t.amount.toInt(),
                    t.status.take(9),
                    if (t.netPlatformProfitOrLoss >= 0) "+" else "-",
                    kotlin.math.abs(t.netPlatformProfitOrLoss.toInt()),
                    t.timestamp.take(18)
                )
                canvas.drawText(row, 36f, y, bodyPaint)
                y += 13f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(outFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            if (destinationUri != null) {
                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    outFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
            pdfDocument.close()
            outFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves CSV string to internal storage and optionally copies to user-selected SAF Uri.
     */
    fun saveCsvFile(
        context: Context,
        exportType: ReportExportType,
        period: PaymentReportPeriod,
        selectedShopName: String,
        csvContent: String,
        destinationUri: Uri? = null
    ): File? {
        return try {
            val reportsDir = File(context.filesDir, "reports").apply {
                if (!exists()) mkdirs()
            }
            val safeShopSlug = selectedShopName.replace(Regex("[^A-Za-z0-9]"), "_").take(20)
            val fileName = "FlashDrop_${exportType.name.lowercase()}_${period.key}_${safeShopSlug}.csv"
            val outFile = File(reportsDir, fileName)
            outFile.writeText(csvContent)

            if (destinationUri != null) {
                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    outStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shares or opens a generated PDF or CSV report file using FileProvider.
     */
    fun shareGeneratedFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = ImageUtils.getFileProviderUri(context, file) ?: return
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FlashDrop Admin Financial Report - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Download / Share Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n")) {
            "\"$cleaned\""
        } else {
            cleaned
        }
    }
}
