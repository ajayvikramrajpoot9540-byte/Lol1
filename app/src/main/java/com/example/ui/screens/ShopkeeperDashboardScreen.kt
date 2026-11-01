package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.util.ImageUtils
import com.example.util.rememberCameraManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperDashboardScreen(
    repository: FlashDropRepository,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val shops by repository.shops.collectAsState()
    val products by repository.products.collectAsState()
    val orders by repository.orders.collectAsState()
    val profile by repository.currentProfile.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Orders", "My Products", "Shop Profile", "Earnings")

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Strictly enforce shop isolation: Only load the assigned shop
    val assignedShopId = profile.shopId.ifBlank { "sharma_general" }
    val currentShop = remember(shops, assignedShopId) {
        shops.find { it.id == assignedShopId } ?: shops.firstOrNull()
    }

    // GATING: Ensure user is authenticated shopkeeper with approved shop
    if (!profile.isVerifiedShopkeeper || !profile.isAuthenticated || profile.role != "shopkeeper") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Restricted",
                        tint = OrangeSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Shopkeeper Access Restricted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You must be signed in as an authorized shopkeeper with an approved store to access this portal.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Return to Customer Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    if (currentShop != null && currentShop.status.equals("pending", ignoreCase = true)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pending Approval",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Shop Awaiting Admin Approval",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your shop registration is awaiting Admin approval from FlashDrop Main Admins (Pranjal Rajpoot & Chanchal Didi).",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Return to Customer Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // STRICT ISOLATION: Filter orders and products ONLY for this shop
    val myShopOrders = remember(orders, currentShop) {
        if (currentShop != null) orders.filter { it.shopId == currentShop.id } else emptyList()
    }

    val myShopProducts = remember(products, currentShop) {
        if (currentShop != null) products.filter { it.shopId == currentShop.id } else emptyList()
    }

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

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
                                Text(
                                    text = "Shopkeeper Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(OrangeLight, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(profile.loginId.ifBlank { "SK" }, fontSize = 11.sp, color = OrangeSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "${currentShop?.name ?: "Local Shop"} • Restricted Access",
                                fontSize = 12.sp,
                                color = OrangeSecondary,
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
                    NotificationBellButton(
                        repository = repository,
                        role = "shopkeeper",
                        shopId = currentShop?.id ?: "",
                        tint = OrangeSecondary
                    )
                    RoleSwitchBadge(
                        currentRole = "shopkeeper",
                        loginId = profile.loginId,
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
            // Metrics Banner (Revenue & Order Counts for this shop only)
            val deliveredSales = myShopOrders.filter { it.status == "delivered" }.sumOf { it.totalAmount }
            val pendingOrders = myShopOrders.count { it.status in listOf("pending", "accepted", "preparing", "out_for_delivery") }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OrangeLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active Orders", fontSize = 12.sp, color = TextDark)
                        Text("$pendingOrders", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OrangeSecondary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderLight))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Delivered Sales", fontSize = 12.sp, color = TextDark)
                        Text("₹${deliveredSales.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GreenPrimary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderLight))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("My Products", fontSize = 12.sp, color = TextDark)
                        Text("${myShopProducts.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = OrangeSecondary
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
                                color = if (selectedTabIndex == index) OrangeSecondary else TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: MY ORDERS ONLY
                    if (myShopOrders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No orders placed yet for ${currentShop?.name ?: "your shop"}.", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(myShopOrders) { order ->
                                ShopkeeperOrderCard(
                                    order = order,
                                    onUpdateStatus = { nextStatus ->
                                        repository.updateOrderStatus(order.id, nextStatus)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Order ${order.orderNumber} updated to $nextStatus")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: MY PRODUCTS ONLY (Add, Edit, Stock, Delete)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("My Shop Inventory (${myShopProducts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Button(
                                onClick = {
                                    editingProduct = null
                                    showProductDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                modifier = Modifier.testTag("shopkeeper_add_product")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Product", fontSize = 12.sp)
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(myShopProducts) { prod ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("product_card_${prod.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Image Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(BackgroundWarm)
                                                .border(1.dp, BorderLight, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (prod.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = prod.imageUrl,
                                                    contentDescription = prod.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.ShoppingBag,
                                                    contentDescription = null,
                                                    tint = GreenPrimary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                            if (prod.description.isNotBlank()) {
                                                Text(
                                                    prod.description,
                                                    fontSize = 11.sp,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text("${prod.unit} • ₹${prod.price.toInt()} (MRP: ₹${prod.originalPrice.toInt()})", fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(if (prod.inStock) "In Stock (Available)" else "Out of Stock (Khali)", fontSize = 11.sp, color = if (prod.inStock) GreenPrimary else Color.Red)
                                        }

                                        IconButton(
                                            onClick = {
                                                editingProduct = prod
                                                showProductDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GreenPrimary)
                                        }

                                        IconButton(
                                            onClick = {
                                                repository.deleteProduct(prod.id)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Product removed from your shop")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: SHOP PROFILE (Timings, Phone, Open/Closed)
                    if (currentShop != null) {
                        var phoneInput by remember { mutableStateOf(currentShop.phone) }
                        var addressInput by remember { mutableStateOf(currentShop.address) }
                        var descInput by remember { mutableStateOf(currentShop.description) }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(currentShop.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                                        Text("Shop ID: ${currentShop.id} • Category: ${currentShop.category}", fontSize = 12.sp, color = OrangeSecondary)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Shop Open for Orders in Mahoba", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Switch(
                                                checked = currentShop.isOpen,
                                                onCheckedChange = { repository.toggleShopStatus(currentShop.id) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = { onNavigate(Screen.EditShopProfile.route) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("open_edit_shop_profile_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Edit Shop Profile & Branding (Logo, Banner, GST)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Edit Shop Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = phoneInput,
                                            onValueChange = { phoneInput = it },
                                            label = { Text("Contact Phone") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = addressInput,
                                            onValueChange = { addressInput = it },
                                            label = { Text("Shop Address in Mahoba") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = descInput,
                                            onValueChange = { descInput = it },
                                            label = { Text("Shop Description / Specials") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                repository.updateShop(
                                                    currentShop.copy(
                                                        phone = phoneInput,
                                                        address = addressInput,
                                                        description = descInput
                                                    )
                                                )
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Shop profile updated")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Save Profile Changes")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: EARNINGS & PAYOUTS
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Shop Payout Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Total Gross Sales: ₹${deliveredSales.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                                    Text("Marketplace Commission (0% Promo): ₹0", fontSize = 13.sp, color = TextMuted)
                                    Text("Net Payout Due: ₹${deliveredSales.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Payouts are settled directly to registered merchant bank account or Cash on Delivery.", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add / Edit Product for this shop with Photo Upload support
    if (showProductDialog && currentShop != null) {
        val context = LocalContext.current
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var description by remember { mutableStateOf(editingProduct?.description ?: "") }
        var selectedCategory by remember { mutableStateOf(editingProduct?.category ?: currentShop.category) }
        var priceText by remember { mutableStateOf(editingProduct?.price?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: "100") }
        var originalPriceText by remember { mutableStateOf(editingProduct?.originalPrice?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: "120") }
        var unit by remember { mutableStateOf(editingProduct?.unit ?: "1 pc") }
        var inStock by remember { mutableStateOf(editingProduct?.inStock ?: true) }

        // Photo Upload State
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var currentImageUrl by remember { mutableStateOf(editingProduct?.imageUrl ?: "") }
        var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
        var isUploading by remember { mutableStateOf(false) }
        var uploadProgressText by remember { mutableStateOf("") }
        var uploadError by remember { mutableStateOf<String?>(null) }
        var showValidationErrors by remember { mutableStateOf(false) }

        val availableCategories = remember {
            listOf("Grocery", "Dairy & Bakery", "Fruits & Vegetables", "Snacks", "Beverages", "Personal Care", "Household", "Sweets & Mithai")
        }

        // Photo Picker launcher (zero runtime storage permissions on Android 13+)
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                uploadError = null
            }
        }

        // Camera manager handling runtime permissions, FileProvider URI grant, preview fallback, and errors
        val cameraManager = rememberCameraManager(
            onImageCaptured = { uri: Uri ->
                selectedImageUri = uri
                uploadError = null
            },
            onError = { err: String ->
                uploadError = err
            }
        )

        val isNameValid = name.isNotBlank()
        val parsedPrice = priceText.toDoubleOrNull()
        val isPriceValid = parsedPrice != null && parsedPrice > 0.0
        val parsedOriginal = originalPriceText.toDoubleOrNull() ?: parsedPrice ?: 0.0
        val isOriginalPriceValid = parsedOriginal >= 0.0
        val isFormValid = isNameValid && isPriceValid && isOriginalPriceValid

        fun executeSave(skipPhotoUpload: Boolean = false) {
            val targetProductId = editingProduct?.id ?: ("prod_" + UUID.randomUUID().toString().take(6))
            if (!skipPhotoUpload && selectedImageUri != null) {
                isUploading = true
                uploadProgressText = "Compressing & uploading photo to Cloud Storage..."
                uploadError = null
                coroutineScope.launch {
                    val uploadResult = repository.uploadProductPhoto(
                        shopId = currentShop.id,
                        productId = targetProductId,
                        imageUri = selectedImageUri!!
                    )
                    uploadResult.onSuccess { uploadedUrl ->
                        val newProduct = Product(
                            id = targetProductId,
                            shopId = currentShop.id,
                            shopName = currentShop.name,
                            name = name.trim(),
                            category = selectedCategory,
                            price = parsedPrice ?: 100.0,
                            originalPrice = parsedOriginal,
                            unit = unit.trim().ifBlank { "1 pc" },
                            description = description.trim(),
                            inStock = inStock,
                            imageUrl = uploadedUrl
                        )
                        repository.saveProduct(newProduct)
                        isUploading = false
                        showProductDialog = false
                        editingProduct = null
                        snackbarHostState.showSnackbar("Product saved with uploaded photo to ${currentShop.name}")
                    }.onFailure { err ->
                        isUploading = false
                        uploadError = err.message ?: "Failed to upload photo. Please check connectivity or retry."
                    }
                }
            } else {
                val finalImageUrl = if (currentImageUrl.isNotBlank()) {
                    currentImageUrl
                } else {
                    "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&auto=format&fit=crop&q=80"
                }
                val newProduct = Product(
                    id = targetProductId,
                    shopId = currentShop.id,
                    shopName = currentShop.name,
                    name = name.trim(),
                    category = selectedCategory,
                    price = parsedPrice ?: 100.0,
                    originalPrice = parsedOriginal,
                    unit = unit.trim().ifBlank { "1 pc" },
                    description = description.trim(),
                    inStock = inStock,
                    imageUrl = finalImageUrl
                )
                repository.saveProduct(newProduct)
                showProductDialog = false
                editingProduct = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Product saved to ${currentShop.name}")
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isUploading) {
                    showProductDialog = false
                    editingProduct = null
                }
            },
            title = {
                Column {
                    Text(
                        if (editingProduct == null) "Add New Product" else "Edit Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Text(
                        "Shop: ${currentShop.name}",
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: PRODUCT PHOTO (Preview, Gallery, Camera, Remove)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Product Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            if (selectedImageUri != null || currentImageUrl.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        selectedImageUri = null
                                        currentImageUrl = ""
                                    },
                                    enabled = !isUploading,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Photo", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Preview Card (170.dp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val activePhoto = selectedImageUri ?: if (currentImageUrl.isNotBlank()) currentImageUrl else null
                                if (activePhoto != null) {
                                    AsyncImage(
                                        model = activePhoto,
                                        contentDescription = "Product preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Status Badge on photo preview
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (selectedImageUri != null) GreenPrimary else Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (selectedImageUri != null) "New Photo Selected" else "Current Image",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "No Photo Selected",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            "Upload clear photo from gallery or snap with camera",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Photo Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("choose_from_gallery_button"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isUploading
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { cameraManager.launchCamera() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("take_photo_button"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isUploading && !cameraManager.isLaunching
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 12.sp, color = OrangeSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Upload Progress Indicator
                    if (isUploading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = GreenPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(uploadProgressText, fontSize = 12.sp, color = TextDark)
                            }
                        }
                    }

                    // Upload Error Banner & Retry
                    if (uploadError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Error", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                }
                                Text(uploadError ?: "", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            uploadError = null
                                            executeSave()
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry Upload", fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            selectedImageUri = null
                                            uploadError = null
                                            executeSave(skipPhotoUpload = true)
                                        }
                                    ) {
                                        Text("Save Without New Photo", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    // SECTION 2: PRODUCT DETAILS
                    // Product Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        placeholder = { Text("e.g. Fortune Mustard Oil 1L") },
                        isError = showValidationErrors && !isNameValid,
                        supportingText = {
                            if (showValidationErrors && !isNameValid) {
                                Text("Product name cannot be empty", color = Color.Red, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Category", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableCategories) { cat ->
                                FilterChip(
                                    selected = (selectedCategory == cat),
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = GreenPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Product Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Product Description (Optional)") },
                        placeholder = { Text("Key ingredients, fresh origin, package details...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_description_input"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3,
                        minLines = 2
                    )

                    // Price & MRP
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Selling Price (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = showValidationErrors && !isPriceValid,
                            supportingText = {
                                if (showValidationErrors && !isPriceValid) {
                                    Text("Enter price > 0", color = Color.Red, fontSize = 10.sp)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_price_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = originalPriceText,
                            onValueChange = { originalPriceText = it },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_mrp_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Unit
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. 1kg, 500g, Pack of 2)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_unit_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Stock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundWarm, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available in Stock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                            Text(
                                if (inStock) "Customers can order this product" else "Marked out of stock for customers",
                                fontSize = 11.sp,
                                color = if (inStock) GreenPrimary else Color.Red
                            )
                        }
                        Switch(
                            checked = inStock,
                            onCheckedChange = { inStock = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showValidationErrors = true
                        if (isFormValid && !isUploading) {
                            executeSave()
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_product_button")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saving...", fontSize = 13.sp)
                    } else {
                        Text(if (editingProduct == null) "Save Product" else "Update Product", fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUploading) {
                            showProductDialog = false
                            editingProduct = null
                        }
                    },
                    enabled = !isUploading
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun ShopkeeperOrderCard(
    order: Order,
    onUpdateStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp, color = TextMuted)
                    Text("Delivery: ${order.deliveryAddress}", fontSize = 12.sp, color = TextDark)
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val printingItem = order.items.find { it.isPrintingOrder }
            if (printingItem != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenLight)
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Document Printing Job (A4 B&W Only)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenPrimary)
                        }
                        if (printingItem.printingDetails.isNotBlank()) {
                            Text(printingItem.printingDetails, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                        }
                        if (printingItem.fileNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Customer Uploaded Files:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            printingItem.fileNames.forEach { fn ->
                                Text("• $fn", fontSize = 11.sp, color = TextDark)
                            }
                        }
                        if (printingItem.fileUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("✓ Secure Cloud Storage Attached (${printingItem.fileUrls.size} file(s))", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                        if (printingItem.printInstructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Customer Instructions: \"${printingItem.printInstructions}\"", fontSize = 11.sp, color = OrangeSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Text(
                    text = order.items.joinToString(", ") { "${it.productName} x${it.quantity}" },
                    fontSize = 12.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text("Total: ₹${"%.2f".format(order.totalAmount)} (${order.paymentMethod})", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons strictly for this shopkeeper's order fulfillment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    "pending" -> {
                        Button(
                            onClick = { onUpdateStatus("accepted") },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Accept Order", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus("rejected") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reject", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                    "accepted" -> {
                        Button(
                            onClick = { onUpdateStatus("preparing") },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start Packing (Preparing)", fontSize = 12.sp)
                        }
                    }
                    "preparing" -> {
                        Button(
                            onClick = { onUpdateStatus("out_for_delivery") },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dispatch / Out for Delivery", fontSize = 12.sp)
                        }
                    }
                    "out_for_delivery" -> {
                        Button(
                            onClick = { onUpdateStatus("delivered") },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mark as Delivered ✓", fontSize = 12.sp)
                        }
                    }
                    else -> {
                        Text("Order Completed (${order.status})", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}
