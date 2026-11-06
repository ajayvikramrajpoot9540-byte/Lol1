package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.ui.components.AddressManagementDialog
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.components.GoogleMapLocationPickerDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopLiveStatus
import com.example.data.repository.ApnaBazRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.components.CameraVisualSearchEngine
import com.example.ui.components.LowestPriceFirstBadge
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SearchBarCameraTrailingActions
import com.example.ui.components.VisualSearchStatusBanner
import com.example.ui.components.VisualSearchUiState
import com.example.ui.navigation.Screen
import com.example.ui.util.ScreenType
import com.example.ui.util.rememberScreenType
import com.example.util.rememberCameraManager
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GoldRating
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: ApnaBazRepository,
    onNavigate: (String) -> Unit,
    onShopSelected: (String) -> Unit,
    onProductSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val shops by repository.shops.collectAsState()
    val products by repository.products.collectAsState()
    val cartItems by repository.cartItems.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val deliveryConfig by repository.deliveryConfig.collectAsState()
    val schedules by repository.shopSchedules.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var visualSearchState by remember { mutableStateOf<VisualSearchUiState>(VisualSearchUiState.Idle) }
    var lastCapturedSearchUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLocality by remember { mutableStateOf(profile.locality) }
    var showAddressManagementDialog by remember { mutableStateOf(false) }
    var locationMenuExpanded by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var showWebsitePreview by remember { mutableStateOf(false) }

    LaunchedEffect(profile.locality) {
        if (profile.locality.isNotBlank()) {
            selectedLocality = profile.locality
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun triggerVisualProductSearch(uri: Uri) {
        lastCapturedSearchUri = uri
        visualSearchState = VisualSearchUiState.Loading(uri)
        coroutineScope.launch {
            val state = CameraVisualSearchEngine.analyzeProductPhoto(
                context = context,
                imageUri = uri,
                catalogProducts = products
            )
            visualSearchState = state
            if (state is VisualSearchUiState.Success) {
                if (state.matchedProductsSortedByLowestPrice.isNotEmpty()) {
                    searchQuery = state.searchQuery
                } else {
                    searchQuery = state.detectedItemName
                }
            }
        }
    }

    val searchCameraManager = rememberCameraManager(
        onImageCaptured = { uri ->
            triggerVisualProductSearch(uri)
        },
        onError = { err ->
            visualSearchState = VisualSearchUiState.Error(lastCapturedSearchUri, err)
        }
    )

    val searchGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            triggerVisualProductSearch(uri)
        }
    }

    var dynamicApprovedCount by remember { mutableStateOf<Int?>(null) }
    var isLoadingShopCount by remember { mutableStateOf(true) }
    var shopCountError by remember { mutableStateOf(false) }

    LaunchedEffect(shops) {
        isLoadingShopCount = true
        val result = repository.fetchDynamicApprovedShopsCount()
        result.onSuccess { count ->
            dynamicApprovedCount = count
            isLoadingShopCount = false
            shopCountError = false
        }.onFailure {
            shopCountError = true
            isLoadingShopCount = false
        }
    }

    val customerVisibleShops = remember(shops) {
        shops.filter { it.status.equals("approved", ignoreCase = true) && it.isOpen }
    }

    val filteredShops = remember(customerVisibleShops, searchQuery) {
        if (searchQuery.isBlank()) customerVisibleShops
        else customerVisibleShops.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val isSearchOrPhotoActive = searchQuery.isNotBlank() || visualSearchState is VisualSearchUiState.Success

    val filteredProducts = remember(products, searchQuery, visualSearchState) {
        if (!isSearchOrPhotoActive) {
            products.take(8)
        } else {
            CameraVisualSearchEngine.filterAndRankByLowestPrice(
                allProducts = products,
                query = searchQuery,
                visualState = visualSearchState
            )
        }
    }

    val lowestPriceProductId = remember(filteredProducts, isSearchOrPhotoActive) {
        if (isSearchOrPhotoActive) {
            filteredProducts.firstOrNull { it.inStock }?.id ?: filteredProducts.firstOrNull()?.id
        } else {
            null
        }
    }

    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE
    val isTablet = screenType == ScreenType.TABLET || screenType == ScreenType.LARGE_TABLET

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1000.dp)
                            .padding(
                                horizontal = if (isSmallMobile) 12.dp else 16.dp,
                                vertical = if (isSmallMobile) 8.dp else 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlashDropCompactLogo(
                            showText = true,
                            modifier = Modifier.testTag("home_top_bar_logo")
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .testTag("location_selector")
                                        .clickable { showAddressManagementDialog = true }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(if (isSmallMobile) 15.dp else 18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = selectedLocality.ifBlank { "Mahoba" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isSmallMobile) 11.sp else 13.sp,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Change Locality",
                                                tint = TextMuted,
                                                modifier = Modifier.size(if (isSmallMobile) 13.dp else 16.dp)
                                            )
                                        }
                                        if (!isSmallMobile) {
                                            Text(
                                                text = "Uttar Pradesh",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                            DropdownMenu(
                                expanded = locationMenuExpanded,
                                onDismissRequest = { locationMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = null,
                                                tint = GreenPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Choose on Google Map",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = GreenPrimary
                                                )
                                                Text(
                                                    text = "Pinpoint with live GPS",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        locationMenuExpanded = false
                                        showMapLocationPicker = true
                                    },
                                    modifier = Modifier.testTag("choose_on_google_map_menu_item")
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                deliveryConfig.supportedAreas.forEach { area ->
                                    DropdownMenuItem(
                                        text = { Text(area) },
                                        onClick = {
                                            selectedLocality = area
                                            repository.updateAddress(profile.address, area, profile.pinCode)
                                            locationMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Live Website Preview Button in Review Window
                        com.example.ui.components.WebsitePreviewTopButton(
                            onClick = { showWebsitePreview = true }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Customer Notification Bell Button
                        NotificationBellButton(
                            repository = repository,
                            role = "customer",
                            tint = GreenPrimary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Role switch pill
                        RoleSwitchBadge(
                            currentRole = profile.role,
                            isVerifiedAdmin = profile.isVerifiedAdmin,
                            isVerifiedShopkeeper = profile.isVerifiedShopkeeper,
                            onRoleSelected = { role ->
                                repository.setRole(role)
                                if (role == "shopkeeper") {
                                    onNavigate(Screen.ShopkeeperDashboard.route)
                                } else if (role == "admin" || role == "MAIN_ADMIN") {
                                    onNavigate(Screen.AdminDashboard.route)
                                }
                            },
                            onOpenShopRegistration = {
                                onNavigate(Screen.ShopRegistration.route)
                            },
                            onNavigateToProfile = {
                                onNavigate(Screen.Profile.route)
                            },
                            onOpenAuth = {
                                onNavigate(Screen.Auth.route)
                            },
                            onNavigateToDashboard = {
                                if (profile.role == "MAIN_ADMIN" || profile.role == "admin") {
                                    onNavigate(Screen.AdminDashboard.route)
                                } else if (profile.role == "shopkeeper") {
                                    onNavigate(Screen.ShopkeeperDashboard.route)
                                }
                            }
                        )
                    }
                }
            }
        }
    },
        bottomBar = {
            val allOrders = repository.orders.collectAsState().value
            val activeOrders = allOrders.count {
                it.status in listOf("pending", "accepted", "preparing", "out_for_delivery")
            }
            BottomNavBar(
                currentRoute = Screen.Home.route,
                onNavigate = onNavigate,
                activeOrdersCount = activeOrders,
                hasOrders = allOrders.isNotEmpty()
            )
        },
        floatingActionButton = {
            if (cartItems.isNotEmpty()) {
                val totalQty = cartItems.sumOf { it.quantity }
                val totalPrice = cartItems.sumOf { it.totalPrice }
                ExtendedCartPill(
                    count = totalQty,
                    total = totalPrice,
                    onClick = { onNavigate(Screen.Cart.route) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1000.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
            // Search Bar with Camera Photo Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isBlank() && visualSearchState is VisualSearchUiState.Success) {
                            visualSearchState = VisualSearchUiState.Idle
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("home_search_input"),
                    placeholder = {
                        Text(
                            "Search by text, mic voice, or camera photo...",
                            color = TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = GreenPrimary
                        )
                    },
                    trailingIcon = {
                        SearchBarCameraTrailingActions(
                            hasActiveQueryOrPhoto = searchQuery.isNotBlank() || visualSearchState !is VisualSearchUiState.Idle,
                            isAnalyzingPhoto = visualSearchState is VisualSearchUiState.Loading,
                            onClear = {
                                searchQuery = ""
                                visualSearchState = VisualSearchUiState.Idle
                            },
                            onLaunchCamera = {
                                searchCameraManager.launchCamera()
                            },
                            onPickFromGallery = {
                                searchGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onVoiceSearchResult = { spokenText ->
                                searchQuery = spokenText
                                visualSearchState = VisualSearchUiState.Idle
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    singleLine = true
                )

                VisualSearchStatusBanner(
                    state = visualSearchState,
                    onClear = {
                        visualSearchState = VisualSearchUiState.Idle
                        searchQuery = ""
                    },
                    onRetry = {
                        lastCapturedSearchUri?.let { triggerVisualProductSearch(it) }
                    }
                )
            }

            // Immediate Lowest-Price-First Search Results right below Search Bar when user searches or clicks a photo
            if (isSearchOrPhotoActive) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (visualSearchState is VisualSearchUiState.Success) {
                                    "Available Matches (Least Price First)"
                                } else {
                                    "Search Results (Least Price First)"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (filteredProducts.isNotEmpty()) {
                                    "Sorted by lowest rate first • Cheapest available at the top"
                                } else {
                                    "No matching product available in store right now"
                                },
                                fontSize = 11.sp,
                                color = if (filteredProducts.isNotEmpty()) GreenPrimary else Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(filteredProducts) { product ->
                    val shopLive = remember(schedules, product.shopId) {
                        repository.getShopLiveStatus(product.shopId)
                    }
                    HomeProductRowItem(
                        product = product,
                        isShopOpen = shopLive.isOpen,
                        shopStatusLabel = shopLive.statusLabel,
                        expectedDelivery = shopLive.expectedDeliveryEstimate,
                        isLowestPriceHighlight = product.id == lowestPriceProductId,
                        onItemClick = { onProductSelected(product.id) },
                        onAddToCart = {
                            if (shopLive.isOpen) {
                                repository.addToCart(product)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Added ${product.name} to cart")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Shop is currently CLOSED (${shopLive.nextOpeningTime})")
                                }
                            }
                        }
                    )
                }
            }

            // Promotional Banner matching layout.png
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Click Karo,\nGhar Mangao! 🛍️",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeSecondary,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mahoba ke dukanon se saman mangayein. Tez home delivery!",
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Categories Section (Horizontal carousel like old app)
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigate(Screen.Categories.route) }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val categoriesList = listOf(
                        CategoryItemData("Grocery", Icons.Default.ShoppingCart, GreenPrimary, GreenLight),
                        CategoryItemData("Printing Press", Icons.Default.Print, GreenPrimary, GreenLight),
                        CategoryItemData("Fashion", Icons.Default.Checkroom, OrangeSecondary, OrangeLight),
                        CategoryItemData("Electronics", Icons.Default.Devices, Color(0xFF2563EB), Color(0xFFDBEAFE)),
                        CategoryItemData("Food", Icons.Default.Fastfood, Color(0xFFD97706), Color(0xFFFEF3C7)),
                        CategoryItemData("Home & Living", Icons.Default.Weekend, Color(0xFF0D9488), Color(0xFFCCFBF1)),
                        CategoryItemData("Pharmacy", Icons.Default.LocalPharmacy, Color(0xFFDC2626), Color(0xFFFEE2E2)),
                        CategoryItemData("Stationery", Icons.Default.Book, Color(0xFF7C3AED), Color(0xFFEDE9FE))
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(categoriesList) { cat ->
                            CategoryCircleItem(
                                cat = cat,
                                onClick = { onCategorySelected(cat.name) }
                            )
                        }
                    }
                }
            }

            // Become a Shopkeeper Card
            item {
                BecomeShopkeeperCard(
                    connectedShopsCount = dynamicApprovedCount,
                    isLoadingCount = isLoadingShopCount,
                    isCountError = shopCountError,
                    onRegisterShopClick = { onNavigate(Screen.ShopRegistration.route) },
                    onSignInClick = { onNavigate(Screen.ShopkeeperSignIn.route) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Popular Shops Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Shops in Mahoba",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigate(Screen.Categories.route) }
                            .padding(4.dp)
                    )
                }
            }

            // Popular Shops Horizontal Carousel
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredShops) { shop ->
                        val liveStatus = repository.getShopLiveStatus(shop.id)
                        ShopCardItem(
                            shop = shop,
                            liveStatus = liveStatus,
                            onClick = { onShopSelected(shop.id) }
                        )
                    }
                }
            }

            // Recommended Products Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recommended Products",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "View More",
                        fontSize = 13.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigate(Screen.ProductList.route) }
                            .padding(4.dp)
                    )
                }
            }

            // Recommended Products List
            if (isTablet) {
                val productPairs = filteredProducts.chunked(2)
                items(productPairs) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { product ->
                            val shopLive = remember(schedules, product.shopId) {
                                repository.getShopLiveStatus(product.shopId)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                HomeProductRowItem(
                                    product = product,
                                    isShopOpen = shopLive.isOpen,
                                    shopStatusLabel = shopLive.statusLabel,
                                    expectedDelivery = shopLive.expectedDeliveryEstimate,
                                    isLowestPriceHighlight = product.id == lowestPriceProductId,
                                    onItemClick = { onProductSelected(product.id) },
                                    onAddToCart = {
                                        if (shopLive.isOpen) {
                                            repository.addToCart(product)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added ${product.name} to cart")
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Shop is currently CLOSED (${shopLive.nextOpeningTime})")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filteredProducts) { product ->
                    val shopLive = remember(schedules, product.shopId) {
                        repository.getShopLiveStatus(product.shopId)
                    }
                    HomeProductRowItem(
                        product = product,
                        isShopOpen = shopLive.isOpen,
                        shopStatusLabel = shopLive.statusLabel,
                        expectedDelivery = shopLive.expectedDeliveryEstimate,
                        isLowestPriceHighlight = product.id == lowestPriceProductId,
                        onItemClick = { onProductSelected(product.id) },
                        onAddToCart = {
                            if (shopLive.isOpen) {
                                repository.addToCart(product)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Added ${product.name} to cart")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Shop is currently CLOSED (${shopLive.nextOpeningTime})")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

    if (showMapLocationPicker) {
        GoogleMapLocationPickerDialog(
            initialAddress = profile.address,
            initialLocality = selectedLocality,
            initialPinCode = profile.pinCode,
            onDismiss = { showMapLocationPicker = false },
            onLocationConfirmed = { newAddress, newLocality, newPin ->
                selectedLocality = newLocality
                repository.updateAddress(newAddress, newLocality, newPin)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Delivery address updated: $newLocality")
                }
            }
        )
    }

    if (showAddressManagementDialog) {
        AddressManagementDialog(
            repository = repository,
            onDismiss = { showAddressManagementDialog = false },
            onOpenMap = { showMapLocationPicker = true },
            onAddressSelected = { selected ->
                selectedLocality = selected.area.ifBlank { "Mahoba" }
            }
        )
    }

    if (showWebsitePreview) {
        com.example.ui.components.WebsitePreviewFullScreenDialog(
            onDismiss = { showWebsitePreview = false }
        )
    }
}

@Composable
fun ShopCardItem(
    shop: Shop,
    liveStatus: ShopLiveStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick)
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                AsyncImage(
                    model = shop.imageUrl,
                    contentDescription = shop.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // GREEN OPEN / RED CLOSED badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (liveStatus.isOpen) "OPEN" else "CLOSED",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // Delivery time badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = liveStatus.expectedDeliveryEstimate.ifBlank { shop.deliveryTime },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = shop.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${shop.category} • ${shop.distance}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldRating,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${shop.rating} (${shop.ratingCount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (liveStatus.isOpen) "OPEN" else "CLOSED",
                        color = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (liveStatus.isOpen) {
                        "Closes: ${liveStatus.closingTimeToday}"
                    } else {
                        "Opens: ${liveStatus.nextOpeningTime}"
                    },
                    fontSize = 10.sp,
                    color = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeProductRowItem(
    product: Product,
    isShopOpen: Boolean = true,
    shopStatusLabel: String = "",
    expectedDelivery: String = "",
    isLowestPriceHighlight: Boolean = false,
    onItemClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onItemClick)
            .testTag("product_item_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowestPriceHighlight) Color(0xFFF0FDF4) else Color.White
        ),
        border = if (isLowestPriceHighlight) androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLowestPriceHighlight) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (isLowestPriceHighlight) {
                    LowestPriceFirstBadge(modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.shopName} • ${product.unit}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isShopOpen) GreenLight else Color(0xFFFEE2E2))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isShopOpen) "OPEN" else "CLOSED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isShopOpen) GreenPrimary else Color(0xFFDC2626)
                        )
                    }
                }
                if (expectedDelivery.isNotBlank()) {
                    Text(
                        text = if (isShopOpen) "Est. Delivery: $expectedDelivery" else "$shopStatusLabel",
                        fontSize = 10.sp,
                        color = if (isShopOpen) GreenPrimary else Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GreenPrimary
                    )
                    if (product.originalPrice > product.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹${product.originalPrice.toInt()}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
                if (!product.inStock) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Out of Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    }
                }
            }
            if (!isShopOpen) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .testTag("add_to_cart_${product.id}")
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFFFEE2E2),
                        disabledContentColor = Color(0xFFDC2626)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Closed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (product.inStock) {
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .testTag("add_to_cart_${product.id}")
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .testTag("add_to_cart_${product.id}")
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Out of Stock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ExtendedCartPill(
    count: Int,
    total: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("floating_cart_pill"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Cart",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count items | ₹${total.toInt()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "View Cart ➔",
                color = OrangeSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

private data class CategoryItemData(
    val name: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color
)

@Composable
fun BecomeShopkeeperCard(
    connectedShopsCount: Int?,
    isLoadingCount: Boolean,
    isCountError: Boolean,
    onRegisterShopClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("become_shopkeeper_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Shopkeeper Icon",
                        tint = OrangeSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Become a Shopkeeper",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Apni shop ko FlashDrop par register karein aur zyada customers tak pahunchein.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "List your products, manage orders, and grow your business with FlashDrop.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Connected Shops Statistic
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OrangeLight.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connected_shops_stat_badge")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (isLoadingCount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = OrangeSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking active shops in Mahoba...",
                            fontSize = 12.sp,
                            color = OrangeSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isCountError || connectedShopsCount == null) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = OrangeSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Join our active merchant network in Mahoba",
                            fontSize = 12.sp,
                            color = OrangeSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OrangeSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$connectedShopsCount shops connected to FlashDrop",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Register Shop Button
            Button(
                onClick = onRegisterShopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("register_your_shop_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeSecondary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = "Register Your Shop",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Already have shop account link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Already have a shop account? Sign In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeSecondary,
                    modifier = Modifier
                        .testTag("already_have_shop_link")
                        .clickable { onSignInClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryCircleItem(
    cat: CategoryItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
            .testTag("category_item_${cat.name.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(cat.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = cat.icon,
                contentDescription = cat.name,
                tint = cat.iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = cat.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

