package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.navigation.Screen
import com.example.ui.theme.BorderLight
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.repository.FlashDropRepository
import com.example.ui.screens.admin.DeliveryChargeSettingsSection
import com.example.ui.screens.admin.ShopVerificationSettingsSection
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: FlashDropRepository,
    onNavigate: ((String) -> Unit)? = null,
    onBack: () -> Unit
) {
    val shops by repository.shops.collectAsState()
    val products by repository.products.collectAsState()
    val orders by repository.orders.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val deliveryConfig by repository.deliveryConfig.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()
    val shopkeepers by repository.shopkeepers.collectAsState()
    val pendingActions by repository.pendingAdminActions.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Payments", "Delivery Settings", "Shop Verification & Settings", "Overview", "All Shops", "All Products", "All Orders", "Shopkeepers & Security")

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showAddShopkeeperDialog by remember { mutableStateOf(false) }
    var showAddShopDialog by remember { mutableStateOf(false) }
    var showEditProductDialog by remember { mutableStateOf<Product?>(null) }
    var showTwoOwnerSafeguardDialog by remember { mutableStateOf(false) }

    // Delivery settings inputs
    var feeInput by remember { mutableStateOf(deliveryConfig.defaultDeliveryFee.toInt().toString()) }
    var minOrderInput by remember { mutableStateOf(deliveryConfig.minOrderAmount.toInt().toString()) }
    var freeThresholdInput by remember { mutableStateOf(deliveryConfig.freeDeliveryThreshold.toInt().toString()) }

    // Display info for current Main Admin owner
    val ownerName = if (currentProfile.loginId == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
    val ownerId = if (currentProfile.loginId == "CD12") "CD12" else "PR12"
    val ownerTitle = if (currentProfile.loginId == "CD12") "Owner 2" else "Owner 1"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(size = 32.dp, showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Main Admin Portal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEDE9FE))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(ownerTitle, fontSize = 11.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "$ownerName ($ownerId) • Equal Main Admin",
                                fontSize = 12.sp,
                                color = Color(0xFF6D28D9),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigate?.invoke(Screen.EditAdminProfile.route) },
                        modifier = Modifier.testTag("admin_portal_edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF6D28D9))
                    }
                    RoleSwitchBadge(
                        currentRole = "MAIN_ADMIN",
                        loginId = ownerId,
                        onRoleSelected = { role, shopId, loginId ->
                            repository.setRole(role, shopId, loginId)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF6D28D9)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) Color(0xFF6D28D9) else TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: SCREEN 8 - ADMIN PAYMENT DASHBOARD
                    var selectedDateFilter by remember { mutableStateOf("Last 7 Days") }
                    val dateFilters = listOf("Today", "Last 7 Days", "Last 30 Days", "Custom Range")

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Co-Owners Banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Main Admin • Financial Governance",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6D28D9)
                                        )
                                        Text(
                                            text = "$ownerName ($ownerId) • Equal Admin Authority",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "Verified Co-Owner with Chanchal Didi & Pranjal Rajpoot",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onNavigate?.invoke(Screen.EditAdminProfile.route) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF6D28D9)),
                                            modifier = Modifier.testTag("admin_banner_edit_profile_button")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6D28D9))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit Profile", fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6D28D9))
                                                .clickable { onNavigate?.invoke(Screen.EditAdminProfile.route) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (currentProfile.photoUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = currentProfile.photoUrl,
                                                    contentDescription = "Admin Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = if (ownerId == "CD12") "CD" else "PR",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Date Filter Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dateFilters.forEach { filter ->
                                    val isSelected = selectedDateFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) GreenPrimary else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) GreenPrimary else BorderLight,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { selectedDateFilter = filter }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextDark
                                        )
                                    }
                                }
                            }
                        }

                        // 4 Metric Cards in 2x2 Grid
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Total Orders
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Total Orders", fontSize = 12.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("248", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+12% vs last week", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    // Online Payments
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Online Payments", fontSize = 12.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("182", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("73.4% success rate", fontSize = 10.sp, color = TextMuted)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // COD Amount
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("COD Amount", fontSize = 12.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("₹34,560", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OrangeSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("66 orders collected", fontSize = 10.sp, color = TextMuted)
                                        }
                                    }

                                    // Pending Payments
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Pending Payments", fontSize = 12.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("14", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Awaiting confirmation", fontSize = 10.sp, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        // Payment Overview Card with Donut Chart
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Payment Overview",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        // Donut Chart Canvas
                                        Box(
                                            modifier = Modifier.size(130.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val strokeWidth = 18.dp.toPx()
                                                // UPI: 60% -> 216 deg
                                                drawArc(
                                                    color = Color(0xFF087443),
                                                    startAngle = -90f,
                                                    sweepAngle = 216f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                                // COD: 38% -> 136.8 deg
                                                drawArc(
                                                    color = Color(0xFFFF8A24),
                                                    startAngle = 126f,
                                                    sweepAngle = 136.8f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                                // Refunds: 2% -> 7.2 deg
                                                drawArc(
                                                    color = Color(0xFFDC2626),
                                                    startAngle = 262.8f,
                                                    sweepAngle = 7.2f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Total", fontSize = 10.sp, color = TextMuted)
                                                Text("₹84.2K", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            }
                                        }

                                        // Donut Legend
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF087443)))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("UPI: 60% (₹50,520)", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF8A24)))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("COD: 38% (₹32,000)", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Refunds: 2% (₹1,680)", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Shop-wise Order Summary
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Shop-wise Order Summary",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextDark
                                        )
                                        TextButton(
                                            onClick = {
                                                onNavigate?.invoke(com.example.ui.navigation.Screen.AdminTransactions.route)
                                            }
                                        ) {
                                            Text("View All", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val shopSummaries = listOf(
                                        Triple("Sharma General Store", "84 orders", "₹24,850"),
                                        Triple("Hemu Printing Shop", "42 orders", "₹12,600"),
                                        Triple("Bundelkhand Organics", "36 orders", "₹9,450"),
                                        Triple("Sahu Sweets & Namkeen", "28 orders", "₹7,200"),
                                        Triple("Rani Fashion Hub", "25 orders", "₹8,150")
                                    )

                                    shopSummaries.forEachIndexed { index, (shop, ordersCount, amount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(shop, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                                                Text(ordersCount, fontSize = 11.sp, color = TextMuted)
                                            }
                                            Text(amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenPrimary)
                                        }
                                        if (index < shopSummaries.size - 1) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }
                        }

                        // CTA to View Full Transaction History
                        item {
                            Button(
                                onClick = {
                                    onNavigate?.invoke(com.example.ui.navigation.Screen.AdminTransactions.route)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("view_all_transactions_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Transaction History & Audit Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: DELIVERY CHARGE SETTINGS (Admin-Controlled Delivery System)
                    DeliveryChargeSettingsSection(
                        repository = repository,
                        deliveryConfig = deliveryConfig,
                        ownerName = ownerName,
                        ownerId = ownerId,
                        ownerTitle = ownerTitle,
                        snackbarHostState = snackbarHostState
                    )
                }

                2 -> {
                    // TAB 2: SHOP VERIFICATION & SETTINGS (Admin-Controlled 3-Step System)
                    ShopVerificationSettingsSection(
                        repository = repository,
                        snackbarHostState = snackbarHostState
                    )
                }

                3 -> {
                    // TAB 3: OVERVIEW & ANALYTICS
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Two Equal Owners Banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("FlashDrop Co-Owners Governance", fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF6D28D9))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Owner 1: Pranjal Rajpoot (PR12) • Owner 2: Chanchal Didi (CD12)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Both owners hold equal administrative authority over shops, products, orders, and shopkeepers across Mahoba.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        item {
                            // Stats Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Market GMV", fontSize = 11.sp, color = TextMuted)
                                        Text("₹${orders.sumOf { it.totalAmount }.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GreenPrimary)
                                        Text("Total Gross Sales", fontSize = 10.sp, color = TextMuted)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Total Orders", fontSize = 11.sp, color = TextMuted)
                                        Text("${orders.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                                        Text("Mahoba Customers", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Active Shops", fontSize = 11.sp, color = TextMuted)
                                        Text("${shops.count { it.isOpen }} / ${shops.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OrangeSecondary)
                                        Text("Local Merchants", fontSize = 10.sp, color = TextMuted)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Shopkeepers", fontSize = 11.sp, color = TextMuted)
                                        Text("${shopkeepers.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF6D28D9))
                                        Text("Authorized logins", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }

                        item {
                            // Quick Action Buttons
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Administrative Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onNavigate?.invoke(Screen.AdminAddShop.route) ?: run { showAddShopDialog = true }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                        ) {
                                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Shop", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                onNavigate?.invoke(Screen.AdminAddShop.route) ?: run { showAddShopDialog = true }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Shopkeeper", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Marketplace Delivery Configuration
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Mahoba Delivery Fee & Thresholds", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = feeInput,
                                        onValueChange = { feeInput = it },
                                        label = { Text("Standard Delivery Fee (₹)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = freeThresholdInput,
                                        onValueChange = { freeThresholdInput = it },
                                        label = { Text("Free Delivery Threshold (₹)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = minOrderInput,
                                        onValueChange = { minOrderInput = it },
                                        label = { Text("Minimum Order Amount (₹)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val f = feeInput.toDoubleOrNull() ?: 20.0
                                            val m = minOrderInput.toDoubleOrNull() ?: 50.0
                                            val t = freeThresholdInput.toDoubleOrNull() ?: 200.0
                                            repository.updateDeliveryConfig(f, m, t)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Marketplace delivery settings saved successfully")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Save Delivery Rules")
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: ALL SHOPS (Approve, Suspend, Edit, Delete)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Registered Shops (${shops.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(
                                    onClick = {
                                        onNavigate?.invoke(Screen.AdminAddShop.route) ?: run { showAddShopDialog = true }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add New Shop", fontSize = 12.sp)
                                }
                            }
                        }

                        items(shops) { shop ->
                            val isPending = shop.status.equals("pending", ignoreCase = true)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPending) Color(0xFFFFFBEB) else Color.White
                                ),
                                border = if (isPending) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                                if (isPending) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(OrangeLight)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Pending Review", fontSize = 10.sp, color = OrangeSecondary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text("ID: ${shop.id} • ${shop.category}", fontSize = 12.sp, color = OrangeSecondary)
                                            if (shop.ownerName.isNotBlank() || shop.phone.isNotBlank()) {
                                                Text("Owner: ${shop.ownerName} • Phone: ${shop.phone}", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                            }
                                            if (shop.gstNumber.isNotBlank()) {
                                                Text("GSTIN: ${shop.gstNumber}", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                            Text(shop.address, fontSize = 12.sp, color = TextMuted)
                                        }

                                        if (!isPending) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (shop.isOpen) "Open" else "Suspended",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (shop.isOpen) GreenPrimary else Color.Red
                                                )
                                                Switch(
                                                    checked = shop.isOpen,
                                                    onCheckedChange = { repository.toggleShopStatus(shop.id) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF3F4F6))

                                    if (isPending) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    repository.rejectShop(shop.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Shop application rejected for ${shop.name}")
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Reject", color = Color.Red, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    repository.approveShop(shop.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Shop ${shop.name} approved by $ownerName! Now active for customers.")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Approve Shop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Min Order: ₹${shop.minOrder.toInt()} • Delivery: ₹${shop.deliveryFee.toInt()}", fontSize = 12.sp, color = TextMuted)

                                            IconButton(
                                                onClick = {
                                                    repository.deleteShop(shop.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Shop ${shop.name} removed by $ownerName")
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Shop", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: ALL PRODUCTS (Moderate across all shops)
                    var filterShopId by remember { mutableStateOf("ALL") }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Shop Products (${products.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        val filteredProducts = if (filterShopId == "ALL") products else products.filter { it.shopId == filterShopId }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProducts) { prod ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                            Text("Shop: ${prod.shopName} • ${prod.unit}", fontSize = 12.sp, color = TextMuted)
                                            Text("₹${prod.price.toInt()} (MRP: ₹${prod.originalPrice.toInt()})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                                            Text(if (prod.inStock) "In Stock" else "Out of Stock", fontSize = 11.sp, color = if (prod.inStock) GreenPrimary else Color.Red)
                                        }

                                        IconButton(
                                            onClick = { showEditProductDialog = prod }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Price/Stock", tint = GreenPrimary)
                                        }

                                        IconButton(
                                            onClick = {
                                                repository.deleteProduct(prod.id)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Product removed from catalog")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Product", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // TAB 6: ALL ORDERS (Across Mahoba)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Shop: ${order.shopName}", fontSize = 12.sp, color = OrangeSecondary, fontWeight = FontWeight.SemiBold)
                                        }
                                        StatusBadge(status = order.status)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp, color = TextDark)
                                    Text("Address: ${order.deliveryAddress}", fontSize = 12.sp, color = TextMuted)
                                    Text("Items: " + order.items.joinToString(", ") { "${it.productName} x${it.quantity}" }, fontSize = 12.sp, color = TextDark)

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Total Amount: ₹${order.totalAmount.toInt()} (${order.paymentMethod})", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 13.sp)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Admin Status Update Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (order.status != "delivered") {
                                            Button(
                                                onClick = {
                                                    repository.updateOrderStatus(order.id, "delivered", "Marked delivered by Main Admin ($ownerId)")
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                            ) {
                                                Text("Mark Delivered", fontSize = 11.sp)
                                            }
                                        }

                                        if (order.status != "cancelled" && order.status != "delivered") {
                                            OutlinedButton(
                                                onClick = {
                                                    repository.updateOrderStatus(order.id, "cancelled", "Cancelled by Main Admin ($ownerId)")
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Cancel Order", fontSize = 11.sp, color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                7 -> {
                    // TAB 7: SHOPKEEPERS & SECURITY SAFEGUARDS
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Two-Owner Security Safeguards Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Two-Owner Confirmation Safeguard", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF6D28D9))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "High-impact actions (such as removing a Main Admin, modifying master security rules, or altering global commission rates) require mutual confirmation between Pranjal Rajpoot (PR12) and Chanchal Didi (CD12).",
                                        fontSize = 12.sp,
                                        color = TextDark,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showTwoOwnerSafeguardDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                                    ) {
                                        Text("Test Security Safeguard Rule", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (pendingActions.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = OrangeLight)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Pending Two-Owner Approvals", fontWeight = FontWeight.Bold, color = OrangeSecondary, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        pendingActions.forEach { action ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(action, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(1f))
                                                Button(
                                                    onClick = {
                                                        repository.approveSensitiveAdminChange(action)
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("Action approved by $ownerName")
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Co-Approve", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Authorized Shopkeepers (${shopkeepers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(
                                    onClick = {
                                        onNavigate?.invoke(Screen.AdminAddShop.route) ?: run { showAddShopDialog = true }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Shopkeeper", fontSize = 12.sp)
                                }
                            }
                        }

                        items(shopkeepers) { sk ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sk.shopkeeperName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                            Text("Login ID: ${sk.loginId} • Shop: ${sk.shopName}", fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold)
                                            Text("Shop ID: ${sk.shopId} • Phone: ${sk.phone}", fontSize = 12.sp, color = TextMuted)
                                            Text("Provisioned by: ${sk.assignedBy}", fontSize = 11.sp, color = TextMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (sk.isActive) "Active" else "Suspended",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sk.isActive) GreenPrimary else Color.Red
                                            )
                                            Switch(
                                                checked = sk.isActive,
                                                onCheckedChange = { repository.toggleShopkeeperStatus(sk.id) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                                            )
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Access limited to ${sk.shopName}", fontSize = 11.sp, color = TextMuted)
                                        IconButton(
                                            onClick = {
                                                repository.removeShopkeeper(sk.id)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Shopkeeper ${sk.shopkeeperName} removed")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Shopkeeper", tint = Color.Red)
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

    // Dialog: Add Shopkeeper
    if (showAddShopkeeperDialog) {
        var skName by remember { mutableStateOf("") }
        var skLoginId by remember { mutableStateOf("") }
        var skPhone by remember { mutableStateOf("") }
        var skEmail by remember { mutableStateOf("") }
        var selectedShop by remember { mutableStateOf(shops.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showAddShopkeeperDialog = false },
            title = { Text("Provision New Shopkeeper", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Main Admin ($ownerId) creates restricted shopkeeper login", fontSize = 12.sp, color = Color(0xFF6D28D9))

                    OutlinedTextField(
                        value = skName,
                        onValueChange = { skName = it },
                        label = { Text("Shopkeeper Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = skLoginId,
                        onValueChange = { skLoginId = it },
                        label = { Text("Login ID (e.g. SK_SHARMA)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = skPhone,
                        onValueChange = { skPhone = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = skEmail,
                        onValueChange = { skEmail = it },
                        label = { Text("Email (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assigned Shop:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    shops.forEach { shop ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedShop = shop }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedShop?.id == shop.id) Icons.Default.CheckCircle else Icons.Default.Store,
                                contentDescription = null,
                                tint = if (selectedShop?.id == shop.id) GreenPrimary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(shop.name, fontSize = 13.sp, fontWeight = if (selectedShop?.id == shop.id) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (skName.isNotBlank() && skLoginId.isNotBlank() && selectedShop != null) {
                            repository.addShopkeeper(
                                name = skName,
                                loginId = skLoginId,
                                shopId = selectedShop!!.id,
                                shopName = selectedShop!!.name,
                                phone = skPhone,
                                email = skEmail.ifBlank { "${skLoginId.lowercase()}@flashdrop.local" }
                            )
                            showAddShopkeeperDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Shopkeeper account $skLoginId created")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShopkeeperDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add Shop (Full Complete Shop Registration & Unique ID creation)
    if (showAddShopDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddShopDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AdminAddShopScreen(
                repository = repository,
                onShopCreated = { _, _ -> showAddShopDialog = false },
                onBack = { showAddShopDialog = false }
            )
        }
    }

    // Dialog: Edit Product Price/Stock
    if (showEditProductDialog != null) {
        val prod = showEditProductDialog!!
        var priceText by remember { mutableStateOf(prod.price.toInt().toString()) }
        var inStock by remember { mutableStateOf(prod.inStock) }

        AlertDialog(
            onDismissRequest = { showEditProductDialog = null },
            title = { Text("Moderate Product: ${prod.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Shop: ${prod.shopName}", fontSize = 12.sp, color = TextMuted)
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("In Stock Availability")
                        Switch(checked = inStock, onCheckedChange = { inStock = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: prod.price
                        repository.saveProduct(prod.copy(price = p, inStock = inStock))
                        showEditProductDialog = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Product updated across catalog")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProductDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Two-Owner Safeguard Simulation
    if (showTwoOwnerSafeguardDialog) {
        var actionInput by remember { mutableStateOf("Modify Platform Commission to 2.5%") }
        AlertDialog(
            onDismissRequest = { showTwoOwnerSafeguardDialog = false },
            title = { Text("Two-Owner Safeguard Request", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Because you are logged in as $ownerName ($ownerId), sensitive structural changes require confirmation by the co-owner.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionInput,
                        onValueChange = { actionInput = it },
                        label = { Text("Sensitive Action Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.submitSensitiveAdminChange(actionInput)
                        showTwoOwnerSafeguardDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Safeguard approval request queued for second owner")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Submit for Dual Confirmation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTwoOwnerSafeguardDialog = false }) { Text("Cancel") }
            }
        )
    }
}
