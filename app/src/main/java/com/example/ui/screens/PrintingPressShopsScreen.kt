package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Shop
import com.example.data.repository.ApnaBazRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingPressShopsScreen(
    repository: ApnaBazRepository,
    onShopSelected: (String) -> Unit,
    onDocumentPrintSelected: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allShops by repository.shops.collectAsState()
    val orders by repository.orders.collectAsState()
    val profile by repository.currentProfile.collectAsState()

    val activeOrdersCount = remember(orders, profile) {
        val userOrders = repository.getOrdersForCurrentUser(orders)
        userOrders.count { it.status in listOf("pending", "accepted", "preparing", "out_for_delivery", "on_the_way", "order_confirmed") }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Open Now", "Top Rated (4.5+)", "Nearest (< 2 km)"

    // Dynamically filter all approved and active Printing Press shops from Firebase
    val printingShops = remember(allShops) {
        repository.getPrintingPressShops(allShops)
    }

    val filteredShops = remember(printingShops, searchQuery, selectedFilter) {
        printingShops.filter { shop ->
            val matchesQuery = searchQuery.isBlank() ||
                    shop.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    shop.address.contains(searchQuery.trim(), ignoreCase = true) ||
                    shop.description.contains(searchQuery.trim(), ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Open Now" -> shop.isOpen
                "Top Rated (4.5+)" -> shop.rating >= 4.5
                "Nearest (< 2 km)" -> {
                    val distNum = shop.distance.replace("km", "").trim().toDoubleOrNull() ?: 1.0
                    distNum <= 2.0
                }
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Printing Press & Xerox",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Mahoba Local Printing Hub",
                            fontSize = 11.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("printing_press_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Categories.route,
                onNavigate = onNavigate,
                activeOrdersCount = activeOrdersCount
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner for Printing Category
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FLASHDROP PRINTING PRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Local Printing & Xerox",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Photocopy • Visiting Cards • Banners • Binding • Doorstep Delivery in Mahoba",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search printing shops in Mahoba...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_printing_shops_input")
                )
            }

            // Filters Row
            item {
                val filters = listOf("All", "Open Now", "Top Rated (4.5+)", "Nearest (< 2 km)")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenLight,
                                selectedLabelColor = GreenPrimary,
                                containerColor = Color.White,
                                labelColor = TextDark
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GreenPrimary else Color(0xFFE5E7EB)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("filter_chip_${filter.replace(" ", "_")}")
                        )
                    }
                }
            }

            // Section Header & Dynamic Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Approved Printing Press Shops",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Text(
                        text = "${filteredShops.size} available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenPrimary
                    )
                }
            }

            // Empty States
            if (printingShops.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Printing Press shops available yet.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please check back soon. New local merchants are being registered and verified across Mahoba.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredShops.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No printing press shops match your search.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try adjusting your search terms or clearing the active filters.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = "All"
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Filters", fontSize = 12.sp, color = GreenPrimary)
                            }
                        }
                    }
                }
            } else {
                // Dynamically Render Printing Press Shop Cards
                items(filteredShops, key = { it.id }) { shop ->
                    PrintingPressShopCard(
                        shop = shop,
                        onOpenShop = { onShopSelected(shop.id) },
                        onQuickDocumentPrint = { onDocumentPrintSelected(shop.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PrintingPressShopCard(
    shop: Shop,
    onOpenShop: () -> Unit,
    onQuickDocumentPrint: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenShop)
            .testTag("printing_shop_card_${shop.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Shop Logo / Photo
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (shop.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = shop.imageUrl,
                            contentDescription = shop.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = OrangeSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shop.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Open / Closed Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (shop.isOpen) GreenLight else Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (shop.isOpen) "Open Now" else "Closed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shop.isOpen) GreenPrimary else Color(0xFFDC2626)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating & Distance Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "%.1f".format(shop.rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        if (shop.ratingCount > 0) {
                            Text(
                                text = " (${shop.ratingCount})",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = shop.distance.ifBlank { "1.2 km" },
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = shop.deliveryTime.ifBlank { "Same Day" },
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Address
                    Text(
                        text = shop.address,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Printing Services Tag Chips
            val servicesList = listOf(
                "Photocopy", "B&W Print", "Colour Print", "Banners",
                "Visiting Cards", "Lamination", "Binding"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(servicesList) { service ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = service,
                            fontSize = 10.sp,
                            color = TextDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (shop.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shop.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenShop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("browse_services_button_${shop.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Browse Services",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }

                Button(
                    onClick = onQuickDocumentPrint,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("quick_print_button_${shop.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Document Printing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
