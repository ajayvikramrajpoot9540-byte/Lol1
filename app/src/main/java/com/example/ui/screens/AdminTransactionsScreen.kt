package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Shop
import com.example.data.model.Transaction
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.util.ScreenType
import com.example.ui.util.rememberScreenType
import com.example.util.AdminPaymentReportExporter
import com.example.util.OnlinePaymentProblemStat
import com.example.util.PaymentReportPeriod
import com.example.util.PeriodFinancialSummary
import com.example.util.ReportExportType
import com.example.util.ShopPaymentBreakdown
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private val AdminPurple = Color(0xFF6D28D9)
private val AdminPurpleLight = Color(0xFFEDE9FE)
private val RoyalBlue = Color(0xFF2563EB)
private val RoyalBlueLight = Color(0xFFDBEAFE)
private val CrimsonRed = Color(0xFFDC2626)
private val CrimsonRedLight = Color(0xFFFEE2E2)
private val AmberGold = Color(0xFFD97706)
private val AmberGoldLight = Color(0xFFFEF3C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionsScreen(
    repository: FlashDropRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Payments, P&L & Audit Ledger",
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark,
                                fontSize = if (isSmallMobile) 14.sp else 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Shop Settlements • Circle Overview • PDF/CSV Export",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            AdminPaymentsAnalyticsSection(
                repository = repository,
                snackbarHostState = snackbarHostState,
                isEmbeddedInDashboard = false
            )
        }
    }
}

/**
 * Unified, responsive Admin Payments & Financial Analytics Section used both in
 * AdminDashboardScreen (Tab 0: Payments & Ledger) and full-screen AdminTransactionsScreen.
 */
@Composable
fun AdminPaymentsAnalyticsSection(
    repository: FlashDropRepository,
    snackbarHostState: SnackbarHostState,
    isEmbeddedInDashboard: Boolean = false,
    headerBannerContent: (@Composable () -> Unit)? = null,
    onOpenFullLedgerScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE

    val transactions by repository.transactions.collectAsState()
    val shops by repository.shops.collectAsState()

    // 1. Time Horizon Filter: 1 Day, 7 Days, 1 Month, 6 Months, 1 Year
    var selectedPeriod by remember { mutableStateOf(PaymentReportPeriod.SEVEN_DAYS) }

    // 2. Selected Shop Filter ("ALL" or specific shopId) for Shop-Specific Download & Filtering
    var selectedShopId by remember { mutableStateOf("ALL") }
    val selectedShopName = remember(selectedShopId, shops) {
        if (selectedShopId == "ALL") "All Shops"
        else shops.find { it.id == selectedShopId }?.name ?: selectedShopId
    }

    // 3. Selected KPI focus tab for Shop-wise Part Breakdown
    // 0 = All (Orders & Payout), 1 = Total Orders Share, 2 = Online Payments, 3 = COD Payments, 4 = Pending Payments
    var selectedShopPartFocus by remember { mutableIntStateOf(0) }

    // 4. Online Payment Problem sub-tab: 0 = By Shop, 1 = By Customer, 2 = All Problem Incidents
    var selectedProblemViewTab by remember { mutableIntStateOf(0) }

    // 5. Transaction History Search & Filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedTxnFilterIndex by remember { mutableIntStateOf(0) }
    val txnFilterTabs = listOf(
        "All",
        "Online (UPI)",
        "COD",
        "Pending",
        "Online Problems",
        "Refunds"
    )

    // 6. Modals: Transaction Inspector & Report Preview Window
    var inspectingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showReportPreviewDialog by remember { mutableStateOf(false) }
    var activeReportExportType by remember { mutableStateOf(ReportExportType.TOTAL_PAYMENTS) }
    var pendingSafExportFormat by remember { mutableStateOf("PDF") } // "PDF" or "CSV"

    // 7. Drop-down Expand/Collapse state for sections after Payments Overview
    var isShopBreakdownExpanded by remember { mutableStateOf(true) }
    var isProfitLossExpanded by remember { mutableStateOf(false) }
    var isTotalSummaryExpanded by remember { mutableStateOf(false) }
    var isOnlineProblemsExpanded by remember { mutableStateOf(false) }
    var isTxnHistoryExpanded by remember { mutableStateOf(false) }
    var selectedDropdownSectionLabel by remember { mutableStateOf("Shop-wise Orders & Settlements") }

    // Precomputed analytics for the active period & shop filter
    val currentSummary = remember(transactions, selectedPeriod, selectedShopId, selectedShopName) {
        AdminPaymentReportExporter.computePeriodSummary(
            transactions = transactions,
            period = selectedPeriod,
            selectedShopId = selectedShopId,
            shopFilterLabel = selectedShopName
        )
    }

    val allPeriodSummaries = remember(transactions, selectedShopId, selectedShopName) {
        AdminPaymentReportExporter.computeAllPeriodSummaries(
            transactions = transactions,
            selectedShopId = selectedShopId,
            shopFilterLabel = selectedShopName
        )
    }

    val shopBreakdowns = remember(transactions, shops, selectedPeriod) {
        AdminPaymentReportExporter.computeShopBreakdowns(
            transactions = transactions,
            shops = shops,
            period = selectedPeriod
        )
    }

    val problemsByShop = remember(transactions, selectedPeriod) {
        AdminPaymentReportExporter.computeOnlineProblemsByShop(transactions, selectedPeriod)
    }

    val problemsByCustomer = remember(transactions, selectedPeriod) {
        AdminPaymentReportExporter.computeOnlineProblemsByCustomer(transactions, selectedPeriod)
    }

    val periodFilteredTxns = remember(transactions, selectedPeriod, selectedShopId) {
        AdminPaymentReportExporter.filterTransactionsByPeriodAndShop(
            transactions = transactions,
            period = selectedPeriod,
            selectedShopId = selectedShopId
        )
    }

    val problemTransactions = remember(periodFilteredTxns) {
        periodFilteredTxns.filter { it.hasOnlinePaymentProblem }
    }

    val displayedTransactions = remember(periodFilteredTxns, searchQuery, selectedTxnFilterIndex) {
        periodFilteredTxns.filter { txn ->
            val matchesFilter = when (selectedTxnFilterIndex) {
                1 -> txn.isOnlinePayment
                2 -> txn.isCodPayment
                3 -> txn.isPendingPayment
                4 -> txn.hasOnlinePaymentProblem
                5 -> txn.status.equals("Refunded", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    txn.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    txn.customerName.contains(searchQuery, ignoreCase = true) ||
                    txn.shopName.contains(searchQuery, ignoreCase = true) ||
                    txn.providerTxnId.contains(searchQuery, ignoreCase = true) ||
                    txn.referenceId.contains(searchQuery, ignoreCase = true) ||
                    txn.itemsSummary.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    // SAF Document Creation Launchers for zero-permission PDF & CSV Downloads
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val file = AdminPaymentReportExporter.generatePdfFile(
                context = context,
                exportType = activeReportExportType,
                period = selectedPeriod,
                selectedShopId = selectedShopId,
                selectedShopName = selectedShopName,
                transactions = transactions,
                shops = shops,
                destinationUri = uri
            )
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (file != null) "PDF downloaded successfully (${file.name})"
                    else "Saved PDF report to selected location"
                )
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csvText = AdminPaymentReportExporter.generateCsvContent(
                exportType = activeReportExportType,
                period = selectedPeriod,
                selectedShopId = selectedShopId,
                selectedShopName = selectedShopName,
                transactions = transactions,
                shops = shops
            )
            val file = AdminPaymentReportExporter.saveCsvFile(
                context = context,
                exportType = activeReportExportType,
                period = selectedPeriod,
                selectedShopName = selectedShopName,
                csvContent = csvText,
                destinationUri = uri
            )
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (file != null) "CSV downloaded successfully (${file.name})"
                    else "Saved CSV report to selected location"
                )
            }
        }
    }

    fun triggerQuickDownload(exportType: ReportExportType, format: String) {
        activeReportExportType = exportType
        pendingSafExportFormat = format
        val safeShop = selectedShopName.replace(Regex("[^A-Za-z0-9]"), "_").take(18)
        if (format == "PDF") {
            val file = AdminPaymentReportExporter.generatePdfFile(
                context = context,
                exportType = exportType,
                period = selectedPeriod,
                selectedShopId = selectedShopId,
                selectedShopName = selectedShopName,
                transactions = transactions,
                shops = shops
            )
            val suggestedName = file?.name ?: "FlashDrop_${exportType.name.lowercase()}_${selectedPeriod.key}_$safeShop.pdf"
            try {
                createPdfLauncher.launch(suggestedName)
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("PDF saved to app reports: $suggestedName")
                }
            }
        } else {
            val csvText = AdminPaymentReportExporter.generateCsvContent(
                exportType = exportType,
                period = selectedPeriod,
                selectedShopId = selectedShopId,
                selectedShopName = selectedShopName,
                transactions = transactions,
                shops = shops
            )
            val file = AdminPaymentReportExporter.saveCsvFile(
                context = context,
                exportType = exportType,
                period = selectedPeriod,
                selectedShopName = selectedShopName,
                csvContent = csvText
            )
            val suggestedName = file?.name ?: "FlashDrop_${exportType.name.lowercase()}_${selectedPeriod.key}_$safeShop.csv"
            try {
                createCsvLauncher.launch(suggestedName)
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("CSV saved to app reports: $suggestedName")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1140.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Optional Dashboard Header Banner (Co-Owners + Customers quick link when embedded in AdminDashboard)
        if (headerBannerContent != null) {
            item {
                headerBannerContent()
            }
        }

        // =========================================================================
        // SECTION A: TIME PERIOD SELECTOR (1 Day, 7 Days, 1 Month, 6 Months, 1 Year)
        // + PREVIEW & DOWNLOAD TOTAL PAYMENTS (PDF / CSV)
        // =========================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_payments_period_export_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Payments, Settlements & P&L Analytics",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isSmallMobile) 15.sp else 17.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Filter by 1 Day, 7 Days, 1 Month, 6 Months or 1 Year • Scope: $selectedShopName",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Surface(
                            color = AdminPurpleLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedPeriod.shortLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminPurple,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5 Period Filter Pills: 1 Day, 7 Days, 1 Month, 6 Months, 1 Year
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentReportPeriod.entries.forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) GreenPrimary else Color(0xFFF8FAFC))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GreenPrimary else BorderLight,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("period_filter_${period.key}")
                            ) {
                                Text(
                                    text = period.shortLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preview Window + Download PDF + Download CSV Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                activeReportExportType = ReportExportType.TOTAL_PAYMENTS
                                showReportPreviewDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_report_preview_window_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Report (${selectedPeriod.shortLabel})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { triggerQuickDownload(ReportExportType.TOTAL_PAYMENTS, "PDF") },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("download_total_payments_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { triggerQuickDownload(ReportExportType.TOTAL_PAYMENTS, "CSV") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GreenPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("download_total_payments_csv_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION B: 4 PRIMARY KPI CARDS (Total Orders, Online Payments, COD Payments, Pending Payments)
        // =========================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Total Orders
                    PaymentKpiCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_orders_card"),
                        title = "Total Orders",
                        primaryValue = "${currentSummary.totalOrdersCount} Orders",
                        secondaryValue = "₹${formatInr(currentSummary.totalOrdersAmount)}",
                        subtitle = "Top: ${shopBreakdowns.firstOrNull()?.shopName ?: "All Shops"}",
                        accentColor = Color(0xFF1E1B4B),
                        badgeBg = AdminPurpleLight,
                        icon = Icons.Default.LocalMall,
                        isSelected = selectedShopPartFocus == 1,
                        onClick = {
                            selectedShopPartFocus = if (selectedShopPartFocus == 1) 0 else 1
                        }
                    )

                    // 2. Online Payments (UPI)
                    val onlineShare = if (currentSummary.totalOrdersCount > 0) {
                        (currentSummary.onlineOrdersCount * 100) / currentSummary.totalOrdersCount
                    } else 0
                    PaymentKpiCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_online_payments_card"),
                        title = "Online Payments",
                        primaryValue = "₹${formatInr(currentSummary.onlineAmount)}",
                        secondaryValue = "${currentSummary.onlineOrdersCount} UPI Orders ($onlineShare%)",
                        subtitle = "Tap to see shop share",
                        accentColor = GreenPrimary,
                        badgeBg = GreenLight,
                        icon = Icons.Default.QrCode,
                        isSelected = selectedShopPartFocus == 2,
                        onClick = {
                            selectedShopPartFocus = if (selectedShopPartFocus == 2) 0 else 2
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3. COD Payments
                    val codShare = if (currentSummary.totalOrdersCount > 0) {
                        (currentSummary.codOrdersCount * 100) / currentSummary.totalOrdersCount
                    } else 0
                    PaymentKpiCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_cod_payments_card"),
                        title = "COD Payments",
                        primaryValue = "₹${formatInr(currentSummary.codAmount)}",
                        secondaryValue = "${currentSummary.codOrdersCount} COD Orders ($codShare%)",
                        subtitle = "Cash collected by riders",
                        accentColor = OrangeSecondary,
                        badgeBg = OrangeLight,
                        icon = Icons.Default.Payments,
                        isSelected = selectedShopPartFocus == 3,
                        onClick = {
                            selectedShopPartFocus = if (selectedShopPartFocus == 3) 0 else 3
                        }
                    )

                    // 4. Pending Payments (Shopkeeper Payout & Gateway Pending)
                    PaymentKpiCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_pending_payments_card"),
                        title = "Pending Payments",
                        primaryValue = "₹${formatInr(currentSummary.pendingAmount)}",
                        secondaryValue = "${currentSummary.pendingOrdersCount} Orders Pending",
                        subtitle = "Shop Payout Due: ₹${formatInr(currentSummary.shopkeeperPendingPayout)}",
                        accentColor = CrimsonRed,
                        badgeBg = CrimsonRedLight,
                        icon = Icons.Default.HourglassTop,
                        isSelected = selectedShopPartFocus == 4,
                        onClick = {
                            selectedShopPartFocus = if (selectedShopPartFocus == 4) 0 else 4
                            isShopBreakdownExpanded = true
                        }
                    )
                }
            }
        }

        // =========================================================================
        // SECTION C: PAYMENTS OVERVIEW IN A CIRCLE (INTERACTIVE DONUT CHART)
        // =========================================================================
        item {
            CircularPaymentOverviewCard(
                summary = currentSummary,
                selectedPeriod = selectedPeriod,
                selectedShopName = selectedShopName
            )
        }

        // =========================================================================
        // SECTION C.2: DROP-DOWN SELECTOR BAR RIGHT AFTER PAYMENTS OVERVIEW
        // (Select Shop Drop-Down + Section View Drop-Down)
        // =========================================================================
        item {
            PostOverviewDropdownControlBar(
                shopBreakdowns = shopBreakdowns,
                selectedShopId = selectedShopId,
                selectedShopName = selectedShopName,
                selectedPeriod = selectedPeriod,
                selectedSectionLabel = selectedDropdownSectionLabel,
                onSelectShop = { shopId ->
                    selectedShopId = shopId
                    isShopBreakdownExpanded = true
                },
                onSelectSectionOption = { option ->
                    selectedDropdownSectionLabel = option
                    when (option) {
                        "Expand All Sections" -> {
                            isShopBreakdownExpanded = true
                            isProfitLossExpanded = true
                            isTotalSummaryExpanded = true
                            isOnlineProblemsExpanded = true
                            isTxnHistoryExpanded = true
                        }
                        "Collapse All Sections" -> {
                            isShopBreakdownExpanded = false
                            isProfitLossExpanded = false
                            isTotalSummaryExpanded = false
                            isOnlineProblemsExpanded = false
                            isTxnHistoryExpanded = false
                        }
                        "Shop-wise Orders & Settlements" -> {
                            isShopBreakdownExpanded = true
                            isProfitLossExpanded = false
                            isTotalSummaryExpanded = false
                            isOnlineProblemsExpanded = false
                            isTxnHistoryExpanded = false
                        }
                        "Profit & Loss (P&L) Statement" -> {
                            isShopBreakdownExpanded = false
                            isProfitLossExpanded = true
                            isTotalSummaryExpanded = false
                            isOnlineProblemsExpanded = false
                            isTxnHistoryExpanded = false
                        }
                        "Total Summary (1D • 7D • 1M • 6M • 1Y)" -> {
                            isShopBreakdownExpanded = false
                            isProfitLossExpanded = false
                            isTotalSummaryExpanded = true
                            isOnlineProblemsExpanded = false
                            isTxnHistoryExpanded = false
                        }
                        "Online Payment Problems Tracker" -> {
                            isShopBreakdownExpanded = false
                            isProfitLossExpanded = false
                            isTotalSummaryExpanded = false
                            isOnlineProblemsExpanded = true
                            isTxnHistoryExpanded = false
                        }
                        "Shop ↔ Customer Transaction History" -> {
                            isShopBreakdownExpanded = false
                            isProfitLossExpanded = false
                            isTotalSummaryExpanded = false
                            isOnlineProblemsExpanded = false
                            isTxnHistoryExpanded = true
                        }
                    }
                },
                onDownloadSelectedShopPdf = {
                    triggerQuickDownload(
                        if (selectedShopId == "ALL") ReportExportType.TOTAL_PAYMENTS else ReportExportType.SHOP_SPECIFIC,
                        "PDF"
                    )
                },
                onDownloadSelectedShopCsv = {
                    triggerQuickDownload(
                        if (selectedShopId == "ALL") ReportExportType.TOTAL_PAYMENTS else ReportExportType.SHOP_SPECIFIC,
                        "CSV"
                    )
                }
            )
        }

        // =========================================================================
        // SECTION D (DROP-DOWN 1): SHOP SELECTOR & SHOP-SPECIFIC DOWNLOAD + SHOP-WISE
        // PART IN ORDERS, ONLINE PAYMENTS, COD, AND PENDING PAYMENTS
        // =========================================================================
        item {
            PaymentSectionDropdownContainer(
                title = "1. Shop-wise Orders, Online, COD & Pending Settlements",
                subtitle = "See which shop gets most orders, settle shopkeeper payouts & download shop statements",
                badgeText = "${shopBreakdowns.size} Shops • Due ₹${formatInr(currentSummary.shopkeeperPendingPayout)}",
                badgeColor = AdminPurple,
                badgeBg = AdminPurpleLight,
                icon = Icons.Default.Storefront,
                isExpanded = isShopBreakdownExpanded,
                onToggleExpand = { isShopBreakdownExpanded = !isShopBreakdownExpanded },
                testTag = "dropdown_section_shop_wise"
            ) {
                ShopWiseContributionAndSettlementCard(
                    shops = shops,
                    shopBreakdowns = shopBreakdowns,
                    selectedPeriod = selectedPeriod,
                    selectedShopId = selectedShopId,
                    selectedShopName = selectedShopName,
                    selectedShopPartFocus = selectedShopPartFocus,
                    onSelectShopFilter = { newShopId -> selectedShopId = newShopId },
                    onSelectFocus = { focusIdx -> selectedShopPartFocus = focusIdx },
                    onPreviewShopReport = { shopId ->
                        selectedShopId = shopId
                        activeReportExportType = ReportExportType.SHOP_SPECIFIC
                        showReportPreviewDialog = true
                    },
                    onDownloadShopPdf = { shopId ->
                        selectedShopId = shopId
                        triggerQuickDownload(ReportExportType.SHOP_SPECIFIC, "PDF")
                    },
                    onDownloadShopCsv = { shopId ->
                        selectedShopId = shopId
                        triggerQuickDownload(ReportExportType.SHOP_SPECIFIC, "CSV")
                    },
                    onSettleShopPayout = { shopId, shopTitle ->
                        val settledCount = repository.settleShopkeeperPendingPayments(shopId)
                        coroutineScope.launch {
                            if (settledCount > 0) {
                                snackbarHostState.showSnackbar("Settled $settledCount pending payment(s) for $shopTitle")
                            } else {
                                snackbarHostState.showSnackbar("All payments for $shopTitle are already settled")
                            }
                        }
                    }
                )
            }
        }

        // =========================================================================
        // SECTION E (DROP-DOWN 2): PROFIT & LOSS (P&L) ANALYTICS IN 1D, 7D, 1M, 6M, 1Y
        // =========================================================================
        item {
            val isProfit = currentSummary.netProfitOrLoss >= 0
            PaymentSectionDropdownContainer(
                title = "2. Profit & Loss (P&L) Earnings Statement",
                subtitle = "Commissions, delivery fees, subsidies & net profit/loss in 1D, 7D, 1M, 6M & 1Y",
                badgeText = if (isProfit) "+₹${currentSummary.netProfitOrLoss.toInt()} Profit" else "-₹${abs(currentSummary.netProfitOrLoss.toInt())} Loss",
                badgeColor = if (isProfit) GreenPrimary else CrimsonRed,
                badgeBg = if (isProfit) GreenLight else CrimsonRedLight,
                icon = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                isExpanded = isProfitLossExpanded,
                onToggleExpand = { isProfitLossExpanded = !isProfitLossExpanded },
                testTag = "dropdown_section_profit_loss"
            ) {
                ProfitAndLossAnalyticsCard(
                    currentSummary = currentSummary,
                    allPeriodSummaries = allPeriodSummaries,
                    shopBreakdowns = shopBreakdowns,
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = { selectedPeriod = it },
                    onPreviewPnlReport = {
                        activeReportExportType = ReportExportType.PROFIT_AND_LOSS
                        showReportPreviewDialog = true
                    },
                    onDownloadPnlPdf = {
                        triggerQuickDownload(ReportExportType.PROFIT_AND_LOSS, "PDF")
                    },
                    onDownloadPnlCsv = {
                        triggerQuickDownload(ReportExportType.PROFIT_AND_LOSS, "CSV")
                    }
                )
            }
        }

        // =========================================================================
        // SECTION F (DROP-DOWN 3): TOTAL SUMMARY ACROSS 1 DAY, 7 DAYS, 1 MONTH, 6 MONTHS & 1 YEAR
        // =========================================================================
        item {
            PaymentSectionDropdownContainer(
                title = "3. Total Summary (1 Day • 7 Days • 1 Month • 6 Months • 1 Year)",
                subtitle = "Multi-period side-by-side comparison for $selectedShopName with PDF & CSV download",
                badgeText = "5 Horizons",
                badgeColor = RoyalBlue,
                badgeBg = RoyalBlueLight,
                icon = Icons.Default.Analytics,
                isExpanded = isTotalSummaryExpanded,
                onToggleExpand = { isTotalSummaryExpanded = !isTotalSummaryExpanded },
                testTag = "dropdown_section_total_summary"
            ) {
                MultiPeriodTotalSummaryCard(
                    allPeriodSummaries = allPeriodSummaries,
                    selectedPeriod = selectedPeriod,
                    selectedShopName = selectedShopName,
                    onSelectPeriod = { selectedPeriod = it },
                    onPreviewPeriodReport = { period ->
                        selectedPeriod = period
                        activeReportExportType = ReportExportType.TOTAL_PAYMENTS
                        showReportPreviewDialog = true
                    },
                    onDownloadPeriodPdf = { period ->
                        selectedPeriod = period
                        triggerQuickDownload(ReportExportType.TOTAL_PAYMENTS, "PDF")
                    },
                    onDownloadPeriodCsv = { period ->
                        selectedPeriod = period
                        triggerQuickDownload(ReportExportType.TOTAL_PAYMENTS, "CSV")
                    }
                )
            }
        }

        // =========================================================================
        // SECTION G (DROP-DOWN 4): ONLINE PAYMENT PROBLEMS TRACKER (WHICH SHOP & CUSTOMER)
        // =========================================================================
        item {
            PaymentSectionDropdownContainer(
                title = "4. Online Payment Problems & Failure Tracker",
                subtitle = "See how many times online payment problems occurred in which Shop & Customer",
                badgeText = "${currentSummary.onlineProblemOccurrences} Issues",
                badgeColor = CrimsonRed,
                badgeBg = CrimsonRedLight,
                icon = Icons.Default.Warning,
                isExpanded = isOnlineProblemsExpanded,
                onToggleExpand = { isOnlineProblemsExpanded = !isOnlineProblemsExpanded },
                testTag = "dropdown_section_online_problems"
            ) {
                OnlinePaymentProblemsTrackerCard(
                    selectedPeriod = selectedPeriod,
                    totalProblemOccurrences = currentSummary.onlineProblemOccurrences,
                    problemsByShop = problemsByShop,
                    problemsByCustomer = problemsByCustomer,
                    problemTransactions = problemTransactions,
                    selectedTab = selectedProblemViewTab,
                    onSelectTab = { selectedProblemViewTab = it },
                    onResolveIssue = { txn ->
                        val resolved = repository.resolveOnlinePaymentIssue(txn.id, "Paid")
                        if (resolved) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Resolved online payment for ${txn.orderNumber} (${txn.customerName})")
                            }
                        }
                    }
                )
            }
        }

        // =========================================================================
        // SECTION H (DROP-DOWN 5): SHOP <-> CUSTOMER PAYMENT & TRANSACTION HISTORY LEDGER
        // =========================================================================
        item {
            PaymentSectionDropdownContainer(
                title = "5. Shop ↔ Customer Payment & Transaction History",
                subtitle = "Complete payment trail between customers and shops (${displayedTransactions.size} records in ${selectedPeriod.shortLabel})",
                badgeText = "${displayedTransactions.size} Txns",
                badgeColor = GreenPrimary,
                badgeBg = GreenLight,
                icon = Icons.Default.ReceiptLong,
                isExpanded = isTxnHistoryExpanded,
                onToggleExpand = { isTxnHistoryExpanded = !isTxnHistoryExpanded },
                testTag = "dropdown_section_txn_history"
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_customer_transaction_history_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isEmbeddedInDashboard && onOpenFullLedgerScreen != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onOpenFullLedgerScreen) {
                                    Text("Open Full Ledger Page", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_txn_search_input"),
                            placeholder = {
                                Text(
                                    "Search by Customer, Shop, Order ID, or Txn ID...",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = GreenPrimary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Tabs
                        ScrollableTabRow(
                            selectedTabIndex = selectedTxnFilterIndex,
                            containerColor = Color.White,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                if (selectedTxnFilterIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTxnFilterIndex]),
                                        color = GreenPrimary
                                    )
                                }
                            }
                        ) {
                            txnFilterTabs.forEachIndexed { index, label ->
                                val count = when (index) {
                                    0 -> periodFilteredTxns.size
                                    1 -> periodFilteredTxns.count { it.isOnlinePayment }
                                    2 -> periodFilteredTxns.count { it.isCodPayment }
                                    3 -> periodFilteredTxns.count { it.isPendingPayment }
                                    4 -> periodFilteredTxns.count { it.hasOnlinePaymentProblem }
                                    5 -> periodFilteredTxns.count { it.status.equals("Refunded", ignoreCase = true) }
                                    else -> 0
                                }
                                Tab(
                                    selected = selectedTxnFilterIndex == index,
                                    onClick = { selectedTxnFilterIndex = index },
                                    text = {
                                        Text(
                                            text = "$label ($count)",
                                            fontWeight = if (selectedTxnFilterIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedTxnFilterIndex == index) GreenPrimary else TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (displayedTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No matching transactions in ${selectedPeriod.shortLabel}",
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Try selecting '1 Year' or clearing the search filter.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                displayedTransactions.forEach { txn ->
                                    ShopCustomerTransactionCard(
                                        transaction = txn,
                                        onClick = { inspectingTransaction = txn }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAL 1: REPORT PREVIEW & DOWNLOAD WINDOW (PDF & CSV PREVIEW)
    // =========================================================================
    if (showReportPreviewDialog) {
        ReportPreviewWindowDialog(
            exportType = activeReportExportType,
            selectedPeriod = selectedPeriod,
            selectedShopId = selectedShopId,
            selectedShopName = selectedShopName,
            transactions = transactions,
            shops = shops,
            onChangeExportType = { activeReportExportType = it },
            onChangePeriod = { selectedPeriod = it },
            onChangeShopId = { selectedShopId = it },
            onDownloadPdf = {
                triggerQuickDownload(activeReportExportType, "PDF")
            },
            onDownloadCsv = {
                triggerQuickDownload(activeReportExportType, "CSV")
            },
            onSharePdf = {
                val file = AdminPaymentReportExporter.generatePdfFile(
                    context = context,
                    exportType = activeReportExportType,
                    period = selectedPeriod,
                    selectedShopId = selectedShopId,
                    selectedShopName = selectedShopName,
                    transactions = transactions,
                    shops = shops
                )
                if (file != null) {
                    AdminPaymentReportExporter.shareGeneratedFile(context, file, "application/pdf")
                }
            },
            onShareCsv = {
                val csv = AdminPaymentReportExporter.generateCsvContent(
                    exportType = activeReportExportType,
                    period = selectedPeriod,
                    selectedShopId = selectedShopId,
                    selectedShopName = selectedShopName,
                    transactions = transactions,
                    shops = shops
                )
                val file = AdminPaymentReportExporter.saveCsvFile(
                    context = context,
                    exportType = activeReportExportType,
                    period = selectedPeriod,
                    selectedShopName = selectedShopName,
                    csvContent = csv
                )
                if (file != null) {
                    AdminPaymentReportExporter.shareGeneratedFile(context, file, "text/csv")
                }
            },
            onDismiss = { showReportPreviewDialog = false }
        )
    }

    // =========================================================================
    // MODAL 2: TRANSACTION RECEIPT & AUDIT INSPECTOR DIALOG
    // =========================================================================
    inspectingTransaction?.let { txn ->
        AlertDialog(
            onDismissRequest = { inspectingTransaction = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = txn.orderNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${txn.customerName} → ${txn.shopName}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    TransactionStatusPill(status = txn.status)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    DetailLine("Order Amount (GMV)", "₹${txn.amount.toInt()}", isHighlight = true)
                    DetailLine("Payment Method", txn.paymentMethod)
                    DetailLine("Customer", "${txn.customerName} ${if (txn.customerPhone.isNotBlank()) "(${txn.customerPhone})" else ""}")
                    DetailLine("Shop", txn.shopName)
                    if (txn.itemsSummary.isNotBlank()) {
                        DetailLine("Ordered Items", txn.itemsSummary)
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    DetailLine("Net Shopkeeper Payout", "₹${txn.effectiveShopkeeperPayout.toInt()}")
                    DetailLine("Shopkeeper Settlement", txn.shopkeeperSettlementStatus)
                    DetailLine("Platform Commission", "₹${txn.effectivePlatformCommission.toInt()}")
                    DetailLine("Delivery Fee", "₹${txn.deliveryFee.toInt()}")
                    DetailLine("Promo / Free-Delivery Subsidy", "₹${txn.discountSubsidy.toInt()}")
                    val pnl = txn.netPlatformProfitOrLoss.toInt()
                    DetailLine(
                        "Platform Net Profit / Loss",
                        if (pnl >= 0) "+₹$pnl (Profit)" else "-₹${abs(pnl)} (Loss)",
                        isHighlight = pnl >= 0
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    DetailLine("Transaction ID", txn.providerTxnId.ifBlank { "N/A" })
                    DetailLine("Reference ID", txn.referenceId.ifBlank { "N/A" })
                    DetailLine("Timestamp", txn.timestamp)
                    if (txn.hasOnlinePaymentProblem) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CrimsonRedLight)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Online Payment Problem (${txn.onlineProblemCount.coerceAtLeast(1)}x Occurrence)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonRed
                                )
                                Text(
                                    text = txn.failureReason.ifBlank { txn.onlineProblemType.ifBlank { "Gateway / Bank Verification Issue" } },
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Audit Proof: ${txn.verificationProof}",
                                fontSize = 11.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (txn.isPendingPayment) {
                        OutlinedButton(
                            onClick = {
                                repository.settleShopkeeperPendingPayments(txn.shopId)
                                inspectingTransaction = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Settled payment for ${txn.shopName}")
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Settle Shop Payout")
                        }
                    }
                    Button(
                        onClick = { inspectingTransaction = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
private fun PaymentKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    primaryValue: String,
    secondaryValue: String,
    subtitle: String,
    accentColor: Color,
    badgeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) badgeBg.copy(alpha = 0.45f) else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else BorderLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = primaryValue,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryValue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Requirement #2: Payments Overview in a Circle (Interactive Donut Chart)
 */
@Composable
private fun CircularPaymentOverviewCard(
    summary: PeriodFinancialSummary,
    selectedPeriod: PaymentReportPeriod,
    selectedShopName: String
) {
    val totalAmt = summary.totalOrdersAmount.coerceAtLeast(1.0)
    val settledOnlineAmt = (summary.onlineAmount - summary.failedOrRefundedAmount).coerceAtLeast(0.0)
    val codAmt = summary.codAmount
    val pendingAmt = summary.pendingAmount
    val issueAmt = summary.failedOrRefundedAmount

    val segmentsSum = (settledOnlineAmt + codAmt + pendingAmt + issueAmt).coerceAtLeast(1.0)
    val onlineSweep = ((settledOnlineAmt / segmentsSum) * 360.0).toFloat()
    val codSweep = ((codAmt / segmentsSum) * 360.0).toFloat()
    val pendingSweep = ((pendingAmt / segmentsSum) * 360.0).toFloat()
    val issueSweep = (360f - onlineSweep - codSweep - pendingSweep).coerceAtLeast(0f)

    val onlinePct = ((summary.onlineAmount / totalAmt) * 100).toInt()
    val codPct = ((summary.codAmount / totalAmt) * 100).toInt()
    val pendingPct = ((summary.pendingAmount / totalAmt) * 100).toInt()
    val issuePct = ((summary.failedOrRefundedAmount / totalAmt) * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_circle_overview_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Payments Overview (Circle Breakdown)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "${selectedPeriod.label} • $selectedShopName • ${summary.totalOrdersCount} Orders",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Surface(
                    color = GreenLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GMV: ₹${formatInr(summary.totalOrdersAmount)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circle Donut Chart
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        if (summary.totalOrdersCount == 0) {
                            drawArc(
                                color = Color(0xFFE2E8F0),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            var startAngle = -90f
                            // 1. Online Payments (Green)
                            if (onlineSweep > 0f) {
                                drawArc(
                                    color = Color(0xFF087443),
                                    startAngle = startAngle,
                                    sweepAngle = onlineSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += onlineSweep
                            }
                            // 2. COD Payments (Orange)
                            if (codSweep > 0f) {
                                drawArc(
                                    color = Color(0xFFFF8A24),
                                    startAngle = startAngle,
                                    sweepAngle = codSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += codSweep
                            }
                            // 3. Pending Payments (Blue)
                            if (pendingSweep > 0f) {
                                drawArc(
                                    color = Color(0xFF2563EB),
                                    startAngle = startAngle,
                                    sweepAngle = pendingSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += pendingSweep
                            }
                            // 4. Failed / Refunded Issues (Red)
                            if (issueSweep > 0f) {
                                drawArc(
                                    color = Color(0xFFDC2626),
                                    startAngle = startAngle,
                                    sweepAngle = issueSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Volume",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "₹${formatInr(summary.totalOrdersAmount)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )
                        Text(
                            text = "${summary.totalOrdersCount} Orders",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Legend & Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircleLegendRow(
                        color = Color(0xFF087443),
                        label = "Online (UPI)",
                        detail = "$onlinePct% • ₹${formatInr(summary.onlineAmount)} (${summary.onlineOrdersCount})"
                    )
                    CircleLegendRow(
                        color = Color(0xFFFF8A24),
                        label = "COD Payments",
                        detail = "$codPct% • ₹${formatInr(summary.codAmount)} (${summary.codOrdersCount})"
                    )
                    CircleLegendRow(
                        color = Color(0xFF2563EB),
                        label = "Pending",
                        detail = "$pendingPct% • ₹${formatInr(summary.pendingAmount)} (${summary.pendingOrdersCount})"
                    )
                    CircleLegendRow(
                        color = Color(0xFFDC2626),
                        label = "Failed / Refund",
                        detail = "$issuePct% • ₹${formatInr(summary.failedOrRefundedAmount)} (${summary.failedOrRefundedCount})"
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleLegendRow(
    color: Color,
    label: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = detail,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            maxLines = 1
        )
    }
}

/**
 * Interactive Drop-Down Control Bar placed directly after Payments Overview (Circle Breakdown).
 * Lets the admin select any Shop from a Drop-Down menu and select/expand/collapse sections after Payments Overview.
 */
@Composable
private fun PostOverviewDropdownControlBar(
    shopBreakdowns: List<ShopPaymentBreakdown>,
    selectedShopId: String,
    selectedShopName: String,
    selectedPeriod: PaymentReportPeriod,
    selectedSectionLabel: String,
    onSelectShop: (String) -> Unit,
    onSelectSectionOption: (String) -> Unit,
    onDownloadSelectedShopPdf: () -> Unit,
    onDownloadSelectedShopCsv: () -> Unit
) {
    var shopMenuExpanded by remember { mutableStateOf(false) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }

    val sectionOptions = listOf(
        "Shop-wise Orders & Settlements",
        "Profit & Loss (P&L) Statement",
        "Total Summary (1D • 7D • 1M • 6M • 1Y)",
        "Online Payment Problems Tracker",
        "Shop ↔ Customer Transaction History",
        "Expand All Sections",
        "Collapse All Sections"
    )

    val selectedShopStat = remember(shopBreakdowns, selectedShopId) {
        shopBreakdowns.find { it.shopId == selectedShopId }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_overview_dropdown_control_bar"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, AdminPurple.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Quick Filter & Section Drop-Downs",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Choose a Shop or Analytics Section below Payments Overview (${selectedPeriod.shortLabel})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = AdminPurple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Select Shop Drop-Down
            Text(
                text = "Select Shop (Orders, Online, COD & Pending):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { shopMenuExpanded = true }
                        .testTag("post_overview_shop_dropdown_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, AdminPurple)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = AdminPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedShopId == "ALL") {
                                    "All Shops (${shopBreakdowns.sumOf { it.totalOrdersCount }} Orders)"
                                } else {
                                    "$selectedShopName (${selectedShopStat?.totalOrdersCount ?: 0} Orders • ₹${formatInr(selectedShopStat?.totalOrdersAmount ?: 0.0)})"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (shopMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Open Shop Drop-Down",
                            tint = AdminPurple
                        )
                    }
                }

                DropdownMenu(
                    expanded = shopMenuExpanded,
                    onDismissRequest = { shopMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "All Shops (${shopBreakdowns.sumOf { it.totalOrdersCount }} Orders)",
                                    fontWeight = if (selectedShopId == "ALL") FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selectedShopId == "ALL") AdminPurple else TextDark
                                )
                                Text(
                                    text = "View combined orders, online UPI, COD & pending payouts",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        },
                        onClick = {
                            onSelectShop("ALL")
                            shopMenuExpanded = false
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    shopBreakdowns.forEach { sb ->
                        val isChosen = selectedShopId == sb.shopId
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${sb.shopName} (${sb.totalOrdersCount} Orders)",
                                        fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isChosen) AdminPurple else TextDark
                                    )
                                    Text(
                                        text = "GMV: ₹${sb.totalOrdersAmount.toInt()} • Online: ₹${sb.onlineAmount.toInt()} • COD: ₹${sb.codAmount.toInt()} • Due: ₹${sb.shopkeeperPayoutDue.toInt()}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            },
                            onClick = {
                                onSelectShop(sb.shopId)
                                shopMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Select Analytics Section Drop-Down
            Text(
                text = "Jump to / Open Section Drop-Down:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { sectionMenuExpanded = true }
                        .testTag("post_overview_section_dropdown_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedSectionLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (sectionMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Open Section Drop-Down",
                            tint = GreenPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = sectionMenuExpanded,
                    onDismissRequest = { sectionMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .background(Color.White)
                ) {
                    sectionOptions.forEach { option ->
                        val isChosen = selectedSectionLabel == option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isChosen) GreenPrimary else TextDark
                                )
                            },
                            onClick = {
                                onSelectSectionOption(option)
                                sectionMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Quick Download Row for the Selected Shop / Scope
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDownloadSelectedShopPdf,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPurple),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedShopId == "ALL") "Download All PDF" else "Download Shop PDF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onDownloadSelectedShopCsv,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedShopId == "ALL") "Download All CSV" else "Download Shop CSV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Reusable Collapsible Drop-Down Section Container for all cards below Payments Overview.
 */
@Composable
private fun PaymentSectionDropdownContainer(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isExpanded) Color(0xFF1E1B4B) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                1.dp,
                if (isExpanded) Color(0xFF1E1B4B) else BorderLight
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpanded) Color.White.copy(alpha = 0.16f) else badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isExpanded) Color.White else badgeColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isExpanded) Color.White else TextDark
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = if (isExpanded) Color.White.copy(alpha = 0.78f) else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isExpanded) Color.White.copy(alpha = 0.18f) else badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isExpanded) Color.White else badgeColor,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                        tint = if (isExpanded) Color.White else TextDark
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

/**
 * Requirements #1 & #6:
 * - Shows which shop participates in Total Orders, Online Payments, COD Payments, and Pending Payments
 * - Highlights which shop gets the most orders
 * - Allows easy payment settlement to the shopkeeper
 * - Allows selecting any specific shop to preview & download its COD, Total Orders, Online Payments & Pending Payments in PDF/CSV
 */
@Composable
private fun ShopWiseContributionAndSettlementCard(
    shops: List<Shop>,
    shopBreakdowns: List<ShopPaymentBreakdown>,
    selectedPeriod: PaymentReportPeriod,
    selectedShopId: String,
    selectedShopName: String,
    selectedShopPartFocus: Int,
    onSelectShopFilter: (String) -> Unit,
    onSelectFocus: (Int) -> Unit,
    onPreviewShopReport: (String) -> Unit,
    onDownloadShopPdf: (String) -> Unit,
    onDownloadShopCsv: (String) -> Unit,
    onSettleShopPayout: (String, String) -> Unit
) {
    var innerShopDropdownExpanded by remember { mutableStateOf(false) }
    val focusLabels = listOf(
        "All Metrics",
        "Most Orders",
        "Online (UPI) Share",
        "COD Share",
        "Pending Payouts"
    )

    val sortedShops = remember(shopBreakdowns, selectedShopPartFocus) {
        when (selectedShopPartFocus) {
            1 -> shopBreakdowns.sortedByDescending { it.totalOrdersCount }
            2 -> shopBreakdowns.sortedByDescending { it.onlineAmount }
            3 -> shopBreakdowns.sortedByDescending { it.codAmount }
            4 -> shopBreakdowns.sortedByDescending { it.pendingAmount }
            else -> shopBreakdowns
        }
    }

    val topOrderingShop = remember(shopBreakdowns) {
        shopBreakdowns.maxByOrNull { it.totalOrdersCount }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_wise_payment_breakdown_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Shop-wise Orders, Online, COD & Pending Settlements",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Select a shop from the drop-down to view & download its COD, Online, Total Orders & Pending Payouts",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Icon(Icons.Default.Storefront, contentDescription = null, tint = GreenPrimary)
            }

            // Top Shop Highlight Banner
            if (topOrderingShop != null && topOrderingShop.totalOrdersCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Highest Order Volume: ${topOrderingShop.shopName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "${topOrderingShop.totalOrdersCount} Orders (${String.format(Locale.US, "%.0f%%", topOrderingShop.orderSharePercent)} share) • GMV ₹${formatInr(topOrderingShop.totalOrdersAmount)} • Payout Due: ₹${formatInr(topOrderingShop.shopkeeperPayoutDue)}",
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Requirement #6: Select Shop Drop-Down + Chip Bar for Shop-Specific Statement Download
            Text(
                text = "Select Shop Drop-Down to Filter & Download Statement:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { innerShopDropdownExpanded = true }
                        .testTag("shop_wise_dropdown_selector"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, AdminPurple)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedShopId == "ALL") {
                                "All Shops (${shopBreakdowns.sumOf { it.totalOrdersCount }} Orders)"
                            } else {
                                selectedShopName
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Icon(
                            imageVector = if (innerShopDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Shop",
                            tint = AdminPurple
                        )
                    }
                }
                DropdownMenu(
                    expanded = innerShopDropdownExpanded,
                    onDismissRequest = { innerShopDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "All Shops (${shopBreakdowns.sumOf { it.totalOrdersCount }} Orders)",
                                fontWeight = if (selectedShopId == "ALL") FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selectedShopId == "ALL") AdminPurple else TextDark
                            )
                        },
                        onClick = {
                            onSelectShopFilter("ALL")
                            innerShopDropdownExpanded = false
                        }
                    )
                    shopBreakdowns.forEach { sb ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${sb.shopName} (${sb.totalOrdersCount} orders • ₹${sb.totalOrdersAmount.toInt()})",
                                    fontWeight = if (selectedShopId == sb.shopId) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedShopId == sb.shopId) AdminPurple else TextDark
                                )
                            },
                            onClick = {
                                onSelectShopFilter(sb.shopId)
                                innerShopDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = selectedShopId == "ALL"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAll) AdminPurple else Color(0xFFF1F5F9))
                            .clickable { onSelectShopFilter("ALL") }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("shop_selector_all")
                    ) {
                        Text(
                            text = "All Shops (${shopBreakdowns.sumOf { it.totalOrdersCount }})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAll) Color.White else TextDark
                        )
                    }
                }
                items(shopBreakdowns) { sb ->
                    val isSelected = selectedShopId == sb.shopId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) AdminPurple else Color(0xFFF1F5F9))
                            .clickable { onSelectShopFilter(sb.shopId) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("shop_selector_${sb.shopId}")
                    ) {
                        Text(
                            text = "${sb.shopName} (${sb.totalOrdersCount})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextDark
                        )
                    }
                }
            }

            // Selected Shop Quick Statement & Download Banner (Requirement #6)
            if (selectedShopId != "ALL") {
                val selectedShopStat = shopBreakdowns.find { it.shopId == selectedShopId }
                if (selectedShopStat != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_shop_download_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AdminPurpleLight.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, AdminPurple)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "${selectedShopStat.shopName} — Payment Statement (${selectedPeriod.shortLabel})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E1B4B)
                                    )
                                    Text(
                                        text = "Total Orders: ${selectedShopStat.totalOrdersCount} (₹${formatInr(selectedShopStat.totalOrdersAmount)})  •  Online: ₹${formatInr(selectedShopStat.onlineAmount)}  •  COD: ₹${formatInr(selectedShopStat.codAmount)}  •  Pending: ₹${formatInr(selectedShopStat.pendingAmount)}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onPreviewShopReport(selectedShopStat.shopId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview Shop Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onDownloadShopPdf(selectedShopStat.shopId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdminPurple),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("download_selected_shop_pdf_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download Shop PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onDownloadShopCsv(selectedShopStat.shopId) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AdminPurple),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("download_selected_shop_csv_button")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = AdminPurple, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download Shop CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminPurple)
                                }
                                if (selectedShopStat.shopkeeperPayoutDue > 0) {
                                    Button(
                                        onClick = { onSettleShopPayout(selectedShopStat.shopId, selectedShopStat.shopName) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pay Shopkeeper (₹${selectedShopStat.shopkeeperPayoutDue.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sort / Focus Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                focusLabels.forEachIndexed { idx, label ->
                    val active = selectedShopPartFocus == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) GreenLight else Color(0xFFF8FAFC))
                            .border(
                                1.dp,
                                if (active) GreenPrimary else BorderLight,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFocus(idx) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) GreenPrimary else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shop Breakdown Cards
            val displayShops = if (selectedShopId == "ALL") sortedShops else sortedShops.filter { it.shopId == selectedShopId }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayShops.forEachIndexed { index, shopStat ->
                    val isTopShop = topOrderingShop?.shopId == shopStat.shopId && shopStat.totalOrdersCount > 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_breakdown_row_${shopStat.shopId}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(
                            1.dp,
                            if (isTopShop) GreenPrimary.copy(alpha = 0.5f) else BorderLight
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${index + 1} ${shopStat.shopName}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                        if (isTopShop) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = GreenLight,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Most Orders",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GreenPrimary
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${shopStat.category} • ${String.format(Locale.US, "%.1f%%", shopStat.orderSharePercent)} of total orders",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Text(
                                    text = "₹${formatInr(shopStat.totalOrdersAmount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = GreenPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (shopStat.orderSharePercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = GreenPrimary,
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // 4-Column Shop Part Grid: Orders | Online | COD | Pending
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ShopMiniMetricBox(
                                    modifier = Modifier.weight(1f),
                                    label = "Total Orders",
                                    value = "${shopStat.totalOrdersCount}",
                                    sub = "₹${shopStat.totalOrdersAmount.toInt()}",
                                    color = Color(0xFF1E1B4B)
                                )
                                ShopMiniMetricBox(
                                    modifier = Modifier.weight(1f),
                                    label = "Online (UPI)",
                                    value = "₹${shopStat.onlineAmount.toInt()}",
                                    sub = "${shopStat.onlineOrdersCount} orders",
                                    color = GreenPrimary
                                )
                                ShopMiniMetricBox(
                                    modifier = Modifier.weight(1f),
                                    label = "COD",
                                    value = "₹${shopStat.codAmount.toInt()}",
                                    sub = "${shopStat.codOrdersCount} orders",
                                    color = OrangeSecondary
                                )
                                ShopMiniMetricBox(
                                    modifier = Modifier.weight(1f),
                                    label = "Pending",
                                    value = "₹${shopStat.pendingAmount.toInt()}",
                                    sub = "${shopStat.pendingOrdersCount} orders",
                                    color = if (shopStat.pendingAmount > 0) CrimsonRed else TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderLight)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Shopkeeper Payout & Quick Download Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (shopStat.shopkeeperPayoutDue > 0) {
                                            "Shopkeeper Payout Due: ₹${shopStat.shopkeeperPayoutDue.toInt()}"
                                        } else {
                                            "Shopkeeper Settled: ₹${shopStat.shopkeeperSettledAmount.toInt()}"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (shopStat.shopkeeperPayoutDue > 0) CrimsonRed else GreenPrimary
                                    )
                                    Text(
                                        text = "Platform P&L from Shop: ${if (shopStat.platformProfitFromShop >= 0) "+₹${shopStat.platformProfitFromShop.toInt()}" else "-₹${abs(shopStat.platformProfitFromShop.toInt())}"}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (shopStat.shopkeeperPayoutDue > 0) {
                                        Button(
                                            onClick = { onSettleShopPayout(shopStat.shopId, shopStat.shopName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Pay ₹${shopStat.shopkeeperPayoutDue.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onDownloadShopPdf(shopStat.shopId) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp), tint = AdminPurple)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminPurple)
                                    }
                                    OutlinedButton(
                                        onClick = { onDownloadShopCsv(shopStat.shopId) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(13.dp), tint = GreenPrimary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopMiniMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            fontSize = 10.sp,
            color = TextDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Requirement #7: Profit & Loss (P&L) Analytics in 1 Day, 7 Days, 1 Month, 6 Months, and 1 Year
 * with PDF & CSV Download and Preview.
 */
@Composable
private fun ProfitAndLossAnalyticsCard(
    currentSummary: PeriodFinancialSummary,
    allPeriodSummaries: List<PeriodFinancialSummary>,
    shopBreakdowns: List<ShopPaymentBreakdown>,
    selectedPeriod: PaymentReportPeriod,
    onSelectPeriod: (PaymentReportPeriod) -> Unit,
    onPreviewPnlReport: () -> Unit,
    onDownloadPnlPdf: () -> Unit,
    onDownloadPnlCsv: () -> Unit
) {
    val isProfit = currentSummary.netProfitOrLoss >= 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profit_and_loss_analytics_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Profit & Loss (P&L) Earnings Statement",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Track platform commissions, delivery fees, subsidies & net profit/loss (${selectedPeriod.shortLabel})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    color = if (isProfit) GreenLight else CrimsonRedLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isProfit) GreenPrimary else CrimsonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isProfit) "+₹${currentSummary.netProfitOrLoss.toInt()} Profit"
                            else "-₹${abs(currentSummary.netProfitOrLoss.toInt())} Loss",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProfit) GreenPrimary else CrimsonRed,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // P&L Breakdown Grid for Current Period
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfit) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(1.dp, if (isProfit) Color(0xFFBBF7D0) else Color(0xFFFECACA))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailLine("Gross Order Value (GMV)", "₹${formatInr(currentSummary.totalOrdersAmount)}")
                    DetailLine("Platform Commission Earned (+8%)", "+₹${formatInr(currentSummary.platformCommissionEarned)}", isHighlight = true)
                    DetailLine("Delivery Charges Collected (+)", "+₹${formatInr(currentSummary.deliveryFeeCollected)}", isHighlight = true)
                    HorizontalDivider(color = BorderLight)
                    DetailLine("Gross Platform Earnings", "₹${formatInr(currentSummary.grossPlatformRevenue)}", isHighlight = true)
                    DetailLine("Free-Delivery & Promo Subsidies (-)", "-₹${formatInr(currentSummary.discountAndSubsidyCost)}")
                    DetailLine("Failed / Refunded Payment Losses (-)", "-₹${formatInr(currentSummary.refundAndFailureLoss)}")
                    HorizontalDivider(color = BorderLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Platform Profit / Loss (${selectedPeriod.shortLabel})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Text(
                            text = if (isProfit) "+₹${formatInr(currentSummary.netProfitOrLoss)} (${String.format(Locale.US, "%.1f%%", currentSummary.profitMarginPercent)})"
                            else "-₹${formatInr(abs(currentSummary.netProfitOrLoss))}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isProfit) GreenPrimary else CrimsonRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-Horizon P&L Strip (1 Day, 7 Days, 1 Month, 6 Months, 1 Year)
            Text(
                text = "Profit & Loss Across Time Horizons (Tap to Inspect):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allPeriodSummaries.forEach { p ->
                    val selected = p.period == selectedPeriod
                    val positive = p.netProfitOrLoss >= 0
                    Card(
                        modifier = Modifier
                            .width(142.dp)
                            .clickable { onSelectPeriod(p.period) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Color(0xFF1E1B4B) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(1.dp, if (selected) Color(0xFF1E1B4B) else BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = p.period.shortLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White.copy(alpha = 0.85f) else TextMuted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (positive) "+₹${p.netProfitOrLoss.toInt()}" else "-₹${abs(p.netProfitOrLoss.toInt())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) {
                                    if (positive) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                                } else {
                                    if (positive) GreenPrimary else CrimsonRed
                                }
                            )
                            Text(
                                text = "Earn: ₹${p.grossPlatformRevenue.toInt()} | Loss: ₹${p.totalExpensesAndLosses.toInt()}",
                                fontSize = 10.sp,
                                color = if (selected) Color.White.copy(alpha = 0.75f) else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download P&L PDF & CSV Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPreviewPnlReport,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("preview_pnl_report_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview P&L Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDownloadPnlPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("download_pnl_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download P&L PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDownloadPnlCsv,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("download_pnl_csv_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download P&L CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }
        }
    }
}

/**
 * Requirement #8: Total Summary in 1 Day, 7 Days, 1 Month, 6 Months, and 1 Year
 */
@Composable
private fun MultiPeriodTotalSummaryCard(
    allPeriodSummaries: List<PeriodFinancialSummary>,
    selectedPeriod: PaymentReportPeriod,
    selectedShopName: String,
    onSelectPeriod: (PaymentReportPeriod) -> Unit,
    onPreviewPeriodReport: (PaymentReportPeriod) -> Unit,
    onDownloadPeriodPdf: (PaymentReportPeriod) -> Unit,
    onDownloadPeriodCsv: (PaymentReportPeriod) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_period_total_summary_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Total Summary (1 Day • 7 Days • 1 Month • 6 Months • 1 Year)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Complete multi-period comparison for $selectedShopName with instant PDF/CSV export",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Icon(Icons.Default.Analytics, contentDescription = null, tint = AdminPurple)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allPeriodSummaries.forEach { summary ->
                    val isCurrent = summary.period == selectedPeriod
                    val isProfit = summary.netProfitOrLoss >= 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPeriod(summary.period) }
                            .testTag("summary_row_${summary.period.key}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) AdminPurpleLight.copy(alpha = 0.45f) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) AdminPurple else BorderLight
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = summary.period.label,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isProfit) GreenLight else CrimsonRedLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isProfit) "Profit +₹${summary.netProfitOrLoss.toInt()}"
                                            else "Loss -₹${abs(summary.netProfitOrLoss.toInt())}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProfit) GreenPrimary else CrimsonRed
                                        )
                                    }
                                }

                                Text(
                                    text = "GMV: ₹${formatInr(summary.totalOrdersAmount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = AdminPurple
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Orders: ${summary.totalOrdersCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Online: ₹${summary.onlineAmount.toInt()} (${summary.onlineOrdersCount})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "COD: ₹${summary.codAmount.toInt()} (${summary.codOrdersCount})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeSecondary
                                )
                                Text(
                                    text = "Pending: ₹${summary.pendingAmount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrimsonRed
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shop Payout: ₹${summary.shopkeeperNetPayoutTotal.toInt()} • Problems: ${summary.onlineProblemOccurrences}",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = { onPreviewPeriodReport(summary.period) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                                    }
                                    TextButton(
                                        onClick = { onDownloadPeriodPdf(summary.period) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                    TextButton(
                                        onClick = { onDownloadPeriodCsv(summary.period) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Requirement #4: Online Payment Problems Tracker (How many times online payment problems occur in which shop and customer)
 */
@Composable
private fun OnlinePaymentProblemsTrackerCard(
    selectedPeriod: PaymentReportPeriod,
    totalProblemOccurrences: Int,
    problemsByShop: List<OnlinePaymentProblemStat>,
    problemsByCustomer: List<OnlinePaymentProblemStat>,
    problemTransactions: List<Transaction>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onResolveIssue: (Transaction) -> Unit
) {
    val tabs = listOf(
        "By Shop (${problemsByShop.size})",
        "By Customer (${problemsByCustomer.size})",
        "Incident Log (${problemTransactions.size})"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("online_payment_problems_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Online Payment Problems & Failure Tracker",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "See how many times online payment problems occurred in which Shop & Customer (${selectedPeriod.shortLabel})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Surface(
                    color = CrimsonRedLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$totalProblemOccurrences Occurrences",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonRed,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, title ->
                    val active = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) CrimsonRed else Color(0xFFF8FAFC))
                            .border(1.dp, if (active) CrimsonRed else BorderLight, RoundedCornerShape(10.dp))
                            .clickable { onSelectTab(idx) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (problemsByShop.isEmpty()) {
                        Text("Zero online payment problems recorded for shops in ${selectedPeriod.shortLabel}.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            problemsByShop.forEach { stat ->
                                OnlineProblemStatRow(
                                    stat = stat,
                                    badgeLabel = "${stat.problemOccurrences}x in Shop"
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (problemsByCustomer.isEmpty()) {
                        Text("Zero online payment problems recorded for customers in ${selectedPeriod.shortLabel}.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            problemsByCustomer.forEach { stat ->
                                OnlineProblemStatRow(
                                    stat = stat,
                                    badgeLabel = "${stat.problemOccurrences}x by Customer"
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (problemTransactions.isEmpty()) {
                        Text("No online payment problem incidents in ${selectedPeriod.shortLabel}.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            problemTransactions.forEach { txn ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = "${txn.customerName} → ${txn.shopName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = "${txn.orderNumber} • ₹${txn.amount.toInt()} • ${txn.timestamp}",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Surface(
                                                color = CrimsonRed,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${txn.onlineProblemCount.coerceAtLeast(1)}x Issue (${txn.status})",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Reason: ${txn.failureReason.ifBlank { txn.onlineProblemType.ifBlank { "UPI Gateway Timeout" } }}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF991B1B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (txn.status.equals("Failed", ignoreCase = true) || txn.status.equals("Pending", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { onResolveIssue(txn) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Verify & Mark Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineProblemStatRow(
    stat: OnlinePaymentProblemStat,
    badgeLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = stat.entityName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Text(
                    text = "${stat.secondaryLabel} • Affected: ₹${stat.affectedAmount.toInt()}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Issues: ${stat.topProblemReasons.joinToString(" | ")}",
                    fontSize = 11.sp,
                    color = CrimsonRed,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                color = CrimsonRedLight,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimsonRed
                )
            }
        }
    }
}

/**
 * Requirement #3: Shop <-> Customer Transaction Card
 */
@Composable
private fun ShopCustomerTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val pnl = transaction.netPlatformProfitOrLoss.toInt()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("txn_row_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (transaction.isOnlinePayment) GreenLight else OrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.isOnlinePayment) Icons.Default.QrCode else Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (transaction.isOnlinePayment) GreenPrimary else OrangeSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = transaction.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Text(
                                text = " → ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GreenPrimary
                            )
                            Text(
                                text = transaction.shopName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AdminPurple,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${transaction.orderNumber} • ${transaction.paymentMethod} • ${transaction.timestamp}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${transaction.amount.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GreenPrimary
                    )
                    TransactionStatusPill(status = transaction.status)
                }
            }

            if (transaction.itemsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Items: ${transaction.itemsSummary}",
                    fontSize = 11.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (transaction.hasOnlinePaymentProblem) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠ Online Payment Problem (${transaction.onlineProblemCount.coerceAtLeast(1)}x): ${transaction.failureReason.ifBlank { transaction.onlineProblemType }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shop Payout: ₹${transaction.effectiveShopkeeperPayout.toInt()} (${transaction.shopkeeperSettlementStatus})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.shopkeeperSettlementStatus.contains("Pending", true)) CrimsonRed else TextDark
                )
                Text(
                    text = if (pnl >= 0) "Platform P&L: +₹$pnl" else "Platform P&L: -₹${abs(pnl)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pnl >= 0) GreenPrimary else CrimsonRed
                )
            }
        }
    }
}

/**
 * Requirement #5, #6, #7: Interactive Report Preview Window (Modal) for PDF & CSV
 * across 1 Day, 7 Days, 1 Month, 6 Months, and 1 Year, and for any selected Shop.
 */
@Composable
private fun ReportPreviewWindowDialog(
    exportType: ReportExportType,
    selectedPeriod: PaymentReportPeriod,
    selectedShopId: String,
    selectedShopName: String,
    transactions: List<Transaction>,
    shops: List<Shop>,
    onChangeExportType: (ReportExportType) -> Unit,
    onChangePeriod: (PaymentReportPeriod) -> Unit,
    onChangeShopId: (String) -> Unit,
    onDownloadPdf: () -> Unit,
    onDownloadCsv: () -> Unit,
    onSharePdf: () -> Unit,
    onShareCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    var previewFormatTab by remember { mutableIntStateOf(0) } // 0 = Formatted PDF Preview, 1 = Raw CSV Preview

    val summary = remember(transactions, selectedPeriod, selectedShopId, selectedShopName) {
        AdminPaymentReportExporter.computePeriodSummary(transactions, selectedPeriod, selectedShopId, selectedShopName)
    }
    val allPeriodSummaries = remember(transactions, selectedShopId, selectedShopName) {
        AdminPaymentReportExporter.computeAllPeriodSummaries(transactions, selectedShopId, selectedShopName)
    }
    val shopBreakdowns = remember(transactions, shops, selectedPeriod) {
        AdminPaymentReportExporter.computeShopBreakdowns(transactions, shops, selectedPeriod)
    }
    val csvPreviewText = remember(exportType, selectedPeriod, selectedShopId, selectedShopName, transactions, shops) {
        AdminPaymentReportExporter.generateCsvContent(
            exportType = exportType,
            period = selectedPeriod,
            selectedShopId = selectedShopId,
            selectedShopName = selectedShopName,
            transactions = transactions,
            shops = shops
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("report_preview_modal_window"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Report Preview & Download Window",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${exportType.title} • ${selectedPeriod.label} • $selectedShopName",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Preview", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Report Type Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportExportType.entries.forEach { type ->
                            val selected = exportType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) AdminPurple else Color(0xFFF1F5F9))
                                    .clickable { onChangeExportType(type) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextDark
                                )
                            }
                        }
                    }

                    // Period Selector inside Preview Window (1D, 7D, 1M, 6M, 1Y)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentReportPeriod.entries.forEach { p ->
                            val selected = selectedPeriod == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) GreenPrimary else Color(0xFFF8FAFC))
                                    .border(1.dp, if (selected) GreenPrimary else BorderLight, RoundedCornerShape(14.dp))
                                    .clickable { onChangePeriod(p) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = p.shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextDark
                                )
                            }
                        }
                    }

                    // Format Preview Switcher: PDF Document View vs CSV Raw Data View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { previewFormatTab = 0 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (previewFormatTab == 0) GreenPrimary else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = if (previewFormatTab == 0) Color.White else TextDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "PDF Document Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (previewFormatTab == 0) Color.White else TextDark
                            )
                        }
                        Button(
                            onClick = { previewFormatTab = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (previewFormatTab == 1) AdminPurple else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                tint = if (previewFormatTab == 1) Color.White else TextDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "CSV Sheet Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (previewFormatTab == 1) Color.White else TextDark
                            )
                        }
                    }

                    if (previewFormatTab == 0) {
                        // Formatted PDF Document Sheet Preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "FLASHDROP MAHOBA — OFFICIAL STATEMENT PREVIEW",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "Period: ${selectedPeriod.label}  •  Shop Scope: $selectedShopName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                HorizontalDivider()
                                DetailLine("Total Orders", "${summary.totalOrdersCount} Orders (₹${formatInr(summary.totalOrdersAmount)})", true)
                                DetailLine("Online Payments (UPI)", "${summary.onlineOrdersCount} Orders (₹${formatInr(summary.onlineAmount)})")
                                DetailLine("COD Payments", "${summary.codOrdersCount} Orders (₹${formatInr(summary.codAmount)})")
                                DetailLine("Pending Payments", "${summary.pendingOrdersCount} Orders (₹${formatInr(summary.pendingAmount)})")
                                DetailLine("Shopkeeper Payout Due", "₹${formatInr(summary.shopkeeperPendingPayout)}")
                                DetailLine("Online Payment Problem Occurrences", "${summary.onlineProblemOccurrences} Incidents")
                                HorizontalDivider()
                                DetailLine("Platform Commission + Delivery Fees", "+₹${formatInr(summary.grossPlatformRevenue)}")
                                DetailLine("Subsidies & Refund/Failure Losses", "-₹${formatInr(summary.totalExpensesAndLosses)}")
                                val netPnl = summary.netProfitOrLoss.toInt()
                                DetailLine(
                                    "Net Platform Profit / Loss",
                                    if (netPnl >= 0) "+₹$netPnl (PROFIT)" else "-₹${abs(netPnl)} (LOSS)",
                                    isHighlight = netPnl >= 0
                                )
                                HorizontalDivider()
                                Text(
                                    text = "Multi-Period Summary (1D / 7D / 1M / 6M / 1Y):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                                allPeriodSummaries.forEach { p ->
                                    Text(
                                        text = "• ${p.period.shortLabel}: ${p.totalOrdersCount} Orders | GMV ₹${p.totalOrdersAmount.toInt()} | Online ₹${p.onlineAmount.toInt()} | COD ₹${p.codAmount.toInt()} | Net P&L: ${if (p.netProfitOrLoss >= 0) "+₹${p.netProfitOrLoss.toInt()}" else "-₹${abs(p.netProfitOrLoss.toInt())}"}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                                HorizontalDivider()
                                Text(
                                    text = "Shop-wise Breakdown (${selectedPeriod.shortLabel}):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                                val targetShops = if (selectedShopId == "ALL") shopBreakdowns else shopBreakdowns.filter { it.shopId == selectedShopId }
                                targetShops.forEach { s ->
                                    Text(
                                        text = "• ${s.shopName}: ${s.totalOrdersCount} Orders | Online ₹${s.onlineAmount.toInt()} | COD ₹${s.codAmount.toInt()} | Pending ₹${s.pendingAmount.toInt()}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }
                    } else {
                        // Raw CSV Text Preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text(
                                text = csvPreviewText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState()),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Bottom Download & Share Bar inside Preview Window
                HorizontalDivider(color = BorderLight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDownloadPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("modal_download_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDownloadCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("modal_download_csv_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSharePdf,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onShareCsv,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share CSV", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionStatusPill(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "paid" -> GreenLight to GreenPrimary
        "collected" -> RoyalBlueLight to RoyalBlue
        "pending" -> OrangeLight to OrangeSecondary
        "failed" -> CrimsonRedLight to CrimsonRed
        "refunded" -> AdminPurpleLight to AdminPurple
        else -> Color(0xFFF1F5F9) to TextMuted
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isHighlight) GreenPrimary else TextDark,
            textAlign = TextAlign.End
        )
    }
}

private fun formatInr(amount: Double): String {
    return String.format(Locale.US, "%,d", amount.toInt())
}
