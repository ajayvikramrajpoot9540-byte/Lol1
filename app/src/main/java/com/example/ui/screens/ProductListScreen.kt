package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.repository.FlashDropRepository
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.example.ui.theme.GoldRating
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.CameraVisualSearchEngine
import com.example.ui.components.LowestPriceFirstBadge
import com.example.ui.components.SearchBarCameraTrailingActions
import com.example.ui.components.VisualSearchStatusBanner
import com.example.ui.components.VisualSearchUiState
import com.example.util.rememberCameraManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    repository: FlashDropRepository,
    categoryName: String = "All",
    shopId: String? = null,
    onProductSelected: (String) -> Unit,
    onShopSelected: ((String) -> Unit)? = null,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val products by repository.products.collectAsState()
    val cartItems by repository.cartItems.collectAsState()
    val shops by repository.shops.collectAsState()
    val schedules by repository.shopSchedules.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var visualSearchState by remember { mutableStateOf<VisualSearchUiState>(VisualSearchUiState.Idle) }
    var lastCapturedSearchUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSubcategory by remember { mutableStateOf("All") }
    var showCustomPrintDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val baseCatalogProducts = remember(products, categoryName, shopId, selectedSubcategory) {
        products.filter { p ->
            val matchCategory = (categoryName == "All" || p.category.equals(categoryName, ignoreCase = true))
            val matchShop = (shopId == null || p.shopId == shopId)
            val matchSub = (selectedSubcategory == "All" || p.subcategory.equals(selectedSubcategory, ignoreCase = true))
            matchCategory && matchShop && matchSub
        }
    }

    fun triggerVisualProductSearch(uri: Uri) {
        lastCapturedSearchUri = uri
        visualSearchState = VisualSearchUiState.Loading(uri)
        coroutineScope.launch {
            val state = CameraVisualSearchEngine.analyzeProductPhoto(
                context = context,
                imageUri = uri,
                catalogProducts = baseCatalogProducts.ifEmpty { products }
            )
            visualSearchState = state
            if (state is VisualSearchUiState.Success) {
                searchQuery = if (state.matchedProductsSortedByLowestPrice.isNotEmpty()) {
                    state.searchQuery
                } else {
                    state.detectedItemName
                }
            }
        }
    }

    val searchCameraManager = rememberCameraManager(
        onImageCaptured = { uri -> triggerVisualProductSearch(uri) },
        onError = { err -> visualSearchState = VisualSearchUiState.Error(lastCapturedSearchUri, err) }
    )

    val searchGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) triggerVisualProductSearch(uri)
    }

    val activeShop = remember(shops, shopId) {
        if (shopId != null) shops.find { it.id == shopId } else null
    }

    val isPrintingCategory = remember(categoryName) {
        categoryName.equals("Printing Press", ignoreCase = true)
    }

    val hemuShop = remember(shops) {
        shops.find { it.id == "hemu_printing" } ?: shops.find { it.category.equals("Printing Press", ignoreCase = true) }
    }

    val availableSubcategories = remember(products, categoryName, shopId) {
        val base = products.filter {
            (categoryName == "All" || it.category.equals(categoryName, ignoreCase = true)) &&
                    (shopId == null || it.shopId == shopId)
        }
        listOf("All") + base.map { it.subcategory }.filter { it.isNotBlank() && it != "All" }.distinct()
    }

    val isSearchOrPhotoActive = searchQuery.isNotBlank() || visualSearchState is VisualSearchUiState.Success

    val filteredProducts = remember(baseCatalogProducts, searchQuery, visualSearchState) {
        CameraVisualSearchEngine.filterAndRankByLowestPrice(
            allProducts = baseCatalogProducts,
            query = searchQuery,
            visualState = visualSearchState
        )
    }

    val lowestPriceProductId = remember(filteredProducts, isSearchOrPhotoActive) {
        if (isSearchOrPhotoActive) {
            filteredProducts.firstOrNull { it.inStock }?.id ?: filteredProducts.firstOrNull()?.id
        } else {
            null
        }
    }

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
                                text = activeShop?.name ?: (if (categoryName == "All") "Mahoba Products" else categoryName),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (activeShop != null) {
                                Text(
                                    text = "${activeShop.category} • ${activeShop.distance} • ${activeShop.deliveryTime}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
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
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Cart.route) }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = GreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1000.dp)
            ) {
            // Search field with Camera Photo Search
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_list_search"),
                placeholder = { Text("Search by text, mic voice, or camera photo...", color = TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        onLaunchCamera = { searchCameraManager.launchCamera() },
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
                shape = RoundedCornerShape(14.dp),
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

            // Subcategory filter chips
            if (availableSubcategories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableSubcategories) { sub ->
                        FilterChip(
                            selected = selectedSubcategory == sub,
                            onClick = { selectedSubcategory = sub },
                            label = { Text(sub, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                // If Printing Press category is active, render the Curated Vendor Listing with Hemu Printing Shop
                if (isPrintingCategory) {
                    item {
                        HemuFeaturedShopCard(
                            shop = hemuShop,
                            onUploadDocuments = {
                                onNavigate(Screen.DocumentPrinting.route)
                            },
                            onViewCatalog = {
                                if (hemuShop != null && onShopSelected != null) {
                                    onShopSelected(hemuShop.id)
                                } else {
                                    showCustomPrintDialog = true
                                }
                            },
                            onQuickInquiry = {
                                showCustomPrintDialog = true
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Print Catalog & Services",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Instant ordering with same-day dispatch in Mahoba",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Surface(
                                color = GreenLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${filteredProducts.size} Services",
                                    color = GreenPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No products found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try searching with different keywords or select 'All'.",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts) { product ->
                        val inCart = cartItems.find { it.product.id == product.id }
                        val shopLive = remember(schedules, shops, product.shopId) {
                            repository.getShopLiveStatus(product.shopId)
                        }

                        val isLowestPriceHighlight = product.id == lowestPriceProductId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { onProductSelected(product.id) }
                                .testTag("product_row_${product.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowestPriceHighlight) Color(0xFFF0FDF4) else Color.White
                            ),
                            border = if (isLowestPriceHighlight) androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isLowestPriceHighlight) 2.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isLowestPriceHighlight) {
                                        LowestPriceFirstBadge(modifier = Modifier.padding(bottom = 3.dp))
                                    }
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${product.shopName} • ${product.unit}",
                                            fontSize = 12.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (shopLive.isOpen) GreenLight else Color(0xFFFEE2E2))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (shopLive.isOpen) "OPEN" else "CLOSED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (shopLive.isOpen) GreenPrimary else Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (shopLive.isOpen) {
                                            "Est. Delivery: ${shopLive.expectedDeliveryEstimate}"
                                        } else {
                                            shopLive.statusLabel
                                        },
                                        fontSize = 10.sp,
                                        color = if (shopLive.isOpen) GreenPrimary else Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${product.price.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = GreenPrimary
                                        )
                                        if (product.originalPrice > product.price) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "₹${product.originalPrice.toInt()}",
                                                fontSize = 12.sp,
                                                color = TextMuted,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFECFDF5))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⚖️ Compare Stores",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenPrimary
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

                                Spacer(modifier = Modifier.width(8.dp))

                                if (!shopLive.isOpen) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier
                                            .testTag("add_button_${product.id}")
                                            .height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFFEE2E2),
                                            disabledContentColor = Color(0xFFDC2626)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Closed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (!product.inStock) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier
                                            .testTag("add_button_${product.id}")
                                            .height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFF1F5F9),
                                            disabledContentColor = Color(0xFF94A3B8)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Out of Stock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else if (inCart != null) {
                                    // Stepper
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GreenLight)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { repository.updateCartQuantity(product.id, -1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = GreenPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "${inCart.quantity}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = GreenPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { repository.updateCartQuantity(product.id, 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = GreenPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            repository.addToCart(product)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added ${product.name}")
                                            }
                                        },
                                        modifier = Modifier
                                            .testTag("add_button_${product.id}")
                                            .height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                    ) {
                                        Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    if (showCustomPrintDialog) {
        var specItemType by remember { mutableStateOf("Flex Banner") }
        var specDimensions by remember { mutableStateOf("6 ft x 3 ft") }
        var specNotes by remember { mutableStateOf("Urgent same day print requirement") }

        AlertDialog(
            onDismissRequest = { showCustomPrintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom Print Order", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Hemu Printing Shop • Mahoba Local Printing Hub",
                        fontSize = 13.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Selected Print Type:", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Flex Banner", "Visiting Cards", "Docs Print").forEach { type ->
                            FilterChip(
                                selected = specItemType == type,
                                onClick = { specItemType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = specDimensions,
                        onValueChange = { specDimensions = it },
                        label = { Text("Dimensions / Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = specNotes,
                        onValueChange = { specNotes = it },
                        label = { Text("Design / Printing Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customProduct = Product(
                            id = "custom_print_${System.currentTimeMillis()}",
                            shopId = "hemu_printing",
                            shopName = "Hemu Printing Shop",
                            name = "$specItemType ($specDimensions)",
                            category = "Printing Press",
                            subcategory = "Custom Print",
                            price = if (specItemType == "Flex Banner") 350.0 else if (specItemType == "Visiting Cards") 299.0 else 50.0,
                            description = "Custom order: $specNotes",
                            imageUrl = "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=400&auto=format&fit=crop&q=80",
                            unit = "Custom Order",
                            inStock = true,
                            rating = 4.8
                        )
                        repository.addToCart(customProduct)
                        showCustomPrintDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Added custom print order to cart!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPrintDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

/**
 * Curated Primary Featured Shop Card for Hemu Printing Shop
 * Highlighting verified badge, 'Mahoba Local Printing Hub', ratings (4.8 ★),
 * turnaround time ('Same Day Dispatch'), and 'View Catalog / Order Print' button.
 */
@Composable
fun HemuFeaturedShopCard(
    shop: Shop?,
    onUploadDocuments: () -> Unit = {},
    onViewCatalog: () -> Unit,
    onQuickInquiry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("hemu_printing_featured_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Curated header chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Curated Vendor Listing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
                Surface(
                    color = OrangeLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Primary Featured",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Info Row: Avatar + Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GreenLight)
                ) {
                    AsyncImage(
                        model = shop?.imageUrl ?: "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=500&auto=format&fit=crop&q=80",
                        contentDescription = "Hemu Printing Shop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = shop?.name ?: "Hemu Printing Shop",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Verified Badge
                        Surface(
                            color = GreenLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Badge",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Verified",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Mahoba Local Printing Hub",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = shop?.address ?: "Chhatarpur Road, Near Alha Chowk, Mahoba",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rating + Turnaround Time Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ratings 4.8 ★
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Ratings",
                            tint = GoldRating,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.8 ★",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(142)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Turnaround time: Same Day Dispatch
                Surface(
                    color = OrangeLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Turnaround",
                            tint = OrangeSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Same Day Dispatch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OrangeSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Service tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Flex Banners", "Visiting Cards", "Docs Print", "Wedding Cards").forEach { tag ->
                    Surface(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            color = TextDark,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action: Upload Documents for Printing
            Button(
                onClick = onUploadDocuments,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("upload_documents_hero_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Documents for Printing (A4 B&W)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Action: View Catalog
            OutlinedButton(
                onClick = onViewCatalog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("view_catalog_order_print_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GreenPrimary)
            ) {
                Text(
                    text = "View Catalog & Services",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Custom Inquiry Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop?.phone ?: "+91 98765 11223",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(
                    onClick = onQuickInquiry,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Custom Specs Order ↗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecondary
                    )
                }
            }
        }
    }
}
