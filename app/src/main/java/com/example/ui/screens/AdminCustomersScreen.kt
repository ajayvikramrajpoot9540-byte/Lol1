package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerOrderSummary
import com.example.data.model.CustomerRecord
import com.example.data.model.DeliveryAddress
import com.example.data.model.Order
import com.example.data.repository.ApnaBazRepository
import com.example.ui.components.CircularProfileAvatar
import com.example.ui.theme.*
import com.example.ui.util.ScreenType
import com.example.ui.util.rememberScreenType
import kotlinx.coroutines.launch

private val FlashDropOrange = OrangeSecondary
private val TrustGreen = GreenPrimary
private val AlertRed = ErrorRed
private val DividerGray = BorderLight
private val TextPrimary = TextDark
private val TextSecondary = TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersScreen(
    repository: ApnaBazRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val customers by repository.customers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val totalGmv = remember(customers) { customers.sumOf { it.totalSpentAmount }.toInt() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(if (isSmallMobile) 32.dp else 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FlashDropOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isSmallMobile) 18.dp else 20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Customers & User Data",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isSmallMobile) 15.sp else 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${customers.size} Customers • ₹$totalGmv Lifetime Spend",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = if (isSmallMobile) 10.sp else 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_customers_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Admin Portal",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddCustomerDialog = true },
                        modifier = Modifier.testTag("admin_add_customer_top_button")
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add Customer",
                            tint = Color(0xFF86EFAC)
                        )
                    }
                    IconButton(
                        onClick = {
                            repository.syncWithFirestore()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Customer directory & data synchronized")
                            }
                        },
                        modifier = Modifier.testTag("admin_refresh_customers_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync Customers",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1B4B))
            )
        },
        containerColor = BackgroundWarm
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AdminCustomersSection(
                repository = repository,
                snackbarHostState = snackbarHostState,
                isEmbeddedInTab = false,
                externalShowAddDialog = showAddCustomerDialog,
                onDismissExternalAddDialog = { showAddCustomerDialog = false }
            )
        }
    }
}

@Composable
fun AdminCustomersSection(
    repository: ApnaBazRepository,
    snackbarHostState: SnackbarHostState,
    isEmbeddedInTab: Boolean = false,
    onOpenFullPage: (() -> Unit)? = null,
    externalShowAddDialog: Boolean = false,
    onDismissExternalAddDialog: () -> Unit = {}
) {
    val customers by repository.customers.collectAsState()
    val liveOrders by repository.orders.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val screenType = rememberScreenType()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedLocalityFilter by remember { mutableStateOf("All Areas") }
    var selectedSortOption by remember { mutableStateOf("Most Spent") }

    // Selected customer for detail inspection (used in both 2-pane tablet mode and modal on phone)
    var selectedCustomerId by remember { mutableStateOf<String?>(customers.firstOrNull()?.id) }
    var showDetailModalOnCompact by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerRecord?>(null) }
    var showInternalAddDialog by remember { mutableStateOf(false) }

    val isAddDialogVisible = externalShowAddDialog || showInternalAddDialog

    // Keep selectedCustomerId valid when list updates
    LaunchedEffect(customers) {
        if (selectedCustomerId == null || customers.none { it.id == selectedCustomerId }) {
            selectedCustomerId = customers.firstOrNull()?.id
        }
    }

    // Merge any live orders placed during the current session into customer records for accurate display
    val enrichedCustomers = remember(customers, liveOrders) {
        customers.map { customer ->
            val matchingLiveOrders = liveOrders.filter {
                it.userId == customer.id || it.customerName.equals(customer.fullName, ignoreCase = true)
            }
            if (matchingLiveOrders.isEmpty()) {
                customer
            } else {
                val existingOrderIds = customer.recentOrders.map { it.orderId }.toSet()
                val newSummaries = matchingLiveOrders
                    .filterNot { existingOrderIds.contains(it.id) }
                    .map { ord ->
                        CustomerOrderSummary(
                            orderId = ord.id,
                            orderNumber = ord.orderNumber,
                            shopId = ord.shopId,
                            shopName = ord.shopName,
                            totalAmount = ord.totalAmount,
                            itemsCount = ord.itemsCount,
                            paymentMethod = if (ord.paymentMethod == "cash_on_delivery") "COD" else "UPI",
                            orderStatus = ord.status,
                            orderDate = ord.billingSnapshot?.calculationTimestamp?.takeIf { it.isNotBlank() } ?: "Today",
                            deliveryAddress = ord.deliveryAddress
                        )
                    }
                val combinedOrders = newSummaries + customer.recentOrders
                val extraSpent = newSummaries.sumOf { it.totalAmount }
                customer.copy(
                    totalOrdersCount = customer.totalOrdersCount + newSummaries.size,
                    totalSpentAmount = customer.totalSpentAmount + extraSpent,
                    recentOrders = combinedOrders
                )
            }
        }
    }

    val localities = remember(enrichedCustomers) {
        val dynamic = enrichedCustomers.map { it.locality.trim() }.filter { it.isNotBlank() }.distinct()
        listOf("All Areas") + (listOf("Alha Chowk", "Subhash Chowk", "Civil Lines", "Gandhi Nagar", "Station Road", "Degree College Road") + dynamic).distinct()
    }

    val filteredCustomers = remember(enrichedCustomers, searchQuery, selectedStatusFilter, selectedLocalityFilter, selectedSortOption) {
        val query = searchQuery.trim().lowercase()
        val filtered = enrichedCustomers.filter { cust ->
            val matchesStatus = when (selectedStatusFilter) {
                "Active" -> cust.accountStatus.equals(CustomerRecord.STATUS_ACTIVE, ignoreCase = true)
                "VIP" -> cust.accountStatus.equals(CustomerRecord.STATUS_VIP, ignoreCase = true)
                "Suspended" -> cust.accountStatus.equals(CustomerRecord.STATUS_SUSPENDED, ignoreCase = true)
                else -> true
            }
            val matchesLocality = selectedLocalityFilter == "All Areas" ||
                cust.locality.equals(selectedLocalityFilter, ignoreCase = true) ||
                cust.primaryAddress.contains(selectedLocalityFilter, ignoreCase = true) ||
                cust.savedAddresses.any { it.area.equals(selectedLocalityFilter, ignoreCase = true) }

            val matchesSearch = query.isEmpty() ||
                cust.fullName.lowercase().contains(query) ||
                cust.id.lowercase().contains(query) ||
                cust.phone.lowercase().contains(query) ||
                cust.email.lowercase().contains(query) ||
                cust.locality.lowercase().contains(query) ||
                cust.primaryAddress.lowercase().contains(query) ||
                cust.recentOrders.any { it.orderNumber.lowercase().contains(query) || it.shopName.lowercase().contains(query) }

            matchesStatus && matchesLocality && matchesSearch
        }

        when (selectedSortOption) {
            "Most Spent" -> filtered.sortedByDescending { it.totalSpentAmount }
            "Most Orders" -> filtered.sortedByDescending { it.totalOrdersCount }
            "Name A-Z" -> filtered.sortedBy { it.fullName.lowercase() }
            else -> filtered
        }
    }

    // Aggregate KPI metrics
    val totalCustomersCount = enrichedCustomers.size
    val activeCustomersCount = enrichedCustomers.count {
        it.accountStatus.equals(CustomerRecord.STATUS_ACTIVE, ignoreCase = true) ||
            it.accountStatus.equals(CustomerRecord.STATUS_VIP, ignoreCase = true)
    }
    val vipCustomersCount = enrichedCustomers.count { it.accountStatus.equals(CustomerRecord.STATUS_VIP, ignoreCase = true) }
    val suspendedCustomersCount = enrichedCustomers.count { it.accountStatus.equals(CustomerRecord.STATUS_SUSPENDED, ignoreCase = true) }
    val totalCustomerSpend = enrichedCustomers.sumOf { it.totalSpentAmount }.toInt()
    val avgCustomerLtv = if (totalCustomersCount > 0) totalCustomerSpend / totalCustomersCount else 0
    val totalOrdersSum = enrichedCustomers.sumOf { it.totalOrdersCount }
    val totalSavedAddressesSum = enrichedCustomers.sumOf { it.savedAddresses.size.coerceAtLeast(1) }

    val selectedCustomer = remember(enrichedCustomers, selectedCustomerId) {
        enrichedCustomers.find { it.id == selectedCustomerId } ?: filteredCustomers.firstOrNull()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val isWideSplitScreen = availableWidth >= 760.dp
        val isMediumGridScreen = availableWidth in 540.dp..759.dp
        val isCompactPhone = availableWidth < 400.dp
        val horizontalPad = when {
            availableWidth >= 840.dp -> 24.dp
            availableWidth >= 600.dp -> 18.dp
            isCompactPhone -> 12.dp
            else -> 16.dp
        }

        if (isWideSplitScreen) {
            // =========================================================================
            // CANONICAL 2-PANE LIST-DETAIL SPLIT VIEW FOR TABLETS / FOLDABLES / LANDSCAPE
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = 12.dp)
            ) {
                AdminCustomerHeaderBanner(
                    totalCustomersCount = totalCustomersCount,
                    vipCustomersCount = vipCustomersCount,
                    isCompactPhone = false,
                    isEmbeddedInTab = isEmbeddedInTab,
                    onOpenFullPage = onOpenFullPage,
                    onAddCustomerClick = { showInternalAddDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ResponsiveCustomerKpiGrid(
                    availableWidth = availableWidth,
                    totalCustomers = totalCustomersCount,
                    activeCustomers = activeCustomersCount,
                    vipCustomers = vipCustomersCount,
                    totalSpend = totalCustomerSpend,
                    avgLtv = avgCustomerLtv,
                    totalOrders = totalOrdersSum,
                    totalAddresses = totalSavedAddressesSum
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Pane: Search, Filters & Scrollable Customer List
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                    ) {
                        CustomerSearchAndFilterPanel(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedStatusFilter = selectedStatusFilter,
                            onStatusFilterChange = { selectedStatusFilter = it },
                            selectedLocalityFilter = selectedLocalityFilter,
                            onLocalityFilterChange = { selectedLocalityFilter = it },
                            selectedSortOption = selectedSortOption,
                            onSortOptionChange = { selectedSortOption = it },
                            localities = localities,
                            totalCount = totalCustomersCount,
                            activeCount = activeCustomersCount,
                            vipCount = vipCustomersCount,
                            suspendedCount = suspendedCustomersCount,
                            filteredCount = filteredCustomers.size,
                            isCompactPhone = false
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredCustomers.isEmpty()) {
                            EmptyCustomerFilterState(
                                onResetFilters = {
                                    searchQuery = ""
                                    selectedStatusFilter = "All"
                                    selectedLocalityFilter = "All Areas"
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("admin_customers_list"),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredCustomers, key = { it.id }) { customer ->
                                    ResponsiveCustomerCard(
                                        customer = customer,
                                        isSelected = selectedCustomer?.id == customer.id,
                                        isCompactPhone = false,
                                        onClick = {
                                            selectedCustomerId = customer.id
                                        },
                                        onQuickStatusChange = { newStatus ->
                                            if (repository.updateCustomerStatus(customer.id, newStatus)) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("${customer.fullName} marked as $newStatus")
                                                }
                                            }
                                        },
                                        onEditClick = { customerToEdit = customer }
                                    )
                                }
                            }
                        }
                    }

                    // Right Pane: Dedicated Customer Data & Activity Inspector
                    Card(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .testTag("admin_customer_detail_pane"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, DividerGray),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (selectedCustomer != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(18.dp)
                            ) {
                                CustomerDetailInspectorContent(
                                    customer = selectedCustomer,
                                    isCompactPhone = false,
                                    onStatusChange = { newStatus ->
                                        if (repository.updateCustomerStatus(selectedCustomer.id, newStatus)) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("${selectedCustomer.fullName} status updated to $newStatus")
                                            }
                                        }
                                    },
                                    onEditCustomer = { customerToEdit = selectedCustomer }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a customer from the directory to inspect their data",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // SINGLE-PANE ADAPTIVE LAYOUT FOR SMALL_MOBILE, LARGE_MOBILE & MEDIUM GRIDS
            // =========================================================================
            val chunkedForMediumGrid = remember(filteredCustomers, isMediumGridScreen) {
                if (isMediumGridScreen) filteredCustomers.chunked(2) else filteredCustomers.map { listOf(it) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("admin_customers_list"),
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = 12.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AdminCustomerHeaderBanner(
                        totalCustomersCount = totalCustomersCount,
                        vipCustomersCount = vipCustomersCount,
                        isCompactPhone = isCompactPhone,
                        isEmbeddedInTab = isEmbeddedInTab,
                        onOpenFullPage = onOpenFullPage,
                        onAddCustomerClick = { showInternalAddDialog = true }
                    )
                }

                item {
                    ResponsiveCustomerKpiGrid(
                        availableWidth = availableWidth,
                        totalCustomers = totalCustomersCount,
                        activeCustomers = activeCustomersCount,
                        vipCustomers = vipCustomersCount,
                        totalSpend = totalCustomerSpend,
                        avgLtv = avgCustomerLtv,
                        totalOrders = totalOrdersSum,
                        totalAddresses = totalSavedAddressesSum
                    )
                }

                item {
                    CustomerSearchAndFilterPanel(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedStatusFilter = selectedStatusFilter,
                        onStatusFilterChange = { selectedStatusFilter = it },
                        selectedLocalityFilter = selectedLocalityFilter,
                        onLocalityFilterChange = { selectedLocalityFilter = it },
                        selectedSortOption = selectedSortOption,
                        onSortOptionChange = { selectedSortOption = it },
                        localities = localities,
                        totalCount = totalCustomersCount,
                        activeCount = activeCustomersCount,
                        vipCount = vipCustomersCount,
                        suspendedCount = suspendedCustomersCount,
                        filteredCount = filteredCustomers.size,
                        isCompactPhone = isCompactPhone
                    )
                }

                if (filteredCustomers.isEmpty()) {
                    item {
                        EmptyCustomerFilterState(
                            onResetFilters = {
                                searchQuery = ""
                                selectedStatusFilter = "All"
                                selectedLocalityFilter = "All Areas"
                            }
                        )
                    }
                } else {
                    items(chunkedForMediumGrid, key = { row -> row.first().id }) { rowItems ->
                        if (rowItems.size == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { customer ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ResponsiveCustomerCard(
                                            customer = customer,
                                            isSelected = false,
                                            isCompactPhone = isCompactPhone,
                                            onClick = {
                                                selectedCustomerId = customer.id
                                                showDetailModalOnCompact = true
                                            },
                                            onQuickStatusChange = { newStatus ->
                                                if (repository.updateCustomerStatus(customer.id, newStatus)) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("${customer.fullName} marked as $newStatus")
                                                    }
                                                }
                                            },
                                            onEditClick = { customerToEdit = customer }
                                        )
                                    }
                                }
                            }
                        } else {
                            val customer = rowItems.first()
                            ResponsiveCustomerCard(
                                customer = customer,
                                isSelected = false,
                                isCompactPhone = isCompactPhone,
                                onClick = {
                                    selectedCustomerId = customer.id
                                    showDetailModalOnCompact = true
                                },
                                onQuickStatusChange = { newStatus ->
                                    if (repository.updateCustomerStatus(customer.id, newStatus)) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("${customer.fullName} marked as $newStatus")
                                        }
                                    }
                                },
                                onEditClick = { customerToEdit = customer }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Responsive Customer Data Inspector Modal for Mobile & Medium Screens
    if (showDetailModalOnCompact && selectedCustomer != null) {
        CustomerDetailInspectorDialog(
            customer = selectedCustomer,
            screenType = screenType,
            onDismiss = { showDetailModalOnCompact = false },
            onStatusChange = { newStatus ->
                if (repository.updateCustomerStatus(selectedCustomer.id, newStatus)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("${selectedCustomer.fullName} status updated to $newStatus")
                    }
                }
            },
            onEditCustomer = {
                customerToEdit = selectedCustomer
            }
        )
    }

    // Edit Customer Details Modal
    customerToEdit?.let { targetCustomer ->
        AdminEditCustomerDialog(
            customer = targetCustomer,
            onDismiss = { customerToEdit = null },
            onSave = { fullName, phone, email, locality, address, status, notes ->
                val result = repository.updateCustomerDetailsByAdmin(
                    customerId = targetCustomer.id,
                    fullName = fullName,
                    phone = phone,
                    email = email,
                    locality = locality,
                    primaryAddress = address,
                    accountStatus = status,
                    adminNotes = notes
                )
                result.fold(
                    onSuccess = {
                        customerToEdit = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Updated customer record for ${it.fullName}")
                        }
                    },
                    onFailure = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err.message ?: "Failed to update customer")
                        }
                    }
                )
            }
        )
    }

    // Add New Customer Modal
    if (isAddDialogVisible) {
        AdminAddCustomerDialog(
            onDismiss = {
                showInternalAddDialog = false
                onDismissExternalAddDialog()
            },
            onAdd = { fullName, phone, email, locality, address, status, notes ->
                val result = repository.addCustomerByAdmin(
                    fullName = fullName,
                    phone = phone,
                    email = email,
                    locality = locality,
                    primaryAddress = address,
                    accountStatus = status,
                    adminNotes = notes
                )
                result.fold(
                    onSuccess = { created ->
                        showInternalAddDialog = false
                        onDismissExternalAddDialog()
                        selectedCustomerId = created.id
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Customer '${created.fullName}' added to directory")
                        }
                    },
                    onFailure = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err.message ?: "Could not add customer")
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun AdminCustomerHeaderBanner(
    totalCustomersCount: Int,
    vipCustomersCount: Int,
    isCompactPhone: Boolean,
    isEmbeddedInTab: Boolean,
    onOpenFullPage: (() -> Unit)?,
    onAddCustomerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                    )
                )
                .padding(if (isCompactPhone) 14.dp else 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompactPhone) 38.dp else 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FlashDropOrange.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PeopleAlt,
                                contentDescription = null,
                                tint = Color(0xFFFDBA74),
                                modifier = Modifier.size(if (isCompactPhone) 20.dp else 24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Customer Directory & Data",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isCompactPhone) 15.sp else 17.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF22C55E).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "LIVE CRM",
                                        color = Color(0xFF86EFAC),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Inspect registered Mahoba customers, saved delivery addresses, UPI/COD spending, and order history.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = if (isCompactPhone) 11.sp else 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddCustomerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FlashDropOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("admin_add_customer_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Customer",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactPhone) 11.sp else 12.sp
                        )
                    }

                    if (isEmbeddedInTab && onOpenFullPage != null) {
                        OutlinedButton(
                            onClick = onOpenFullPage,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("open_full_customers_page_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCompactPhone) "Full View" else "Open Full Customer Page",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isCompactPhone) 11.sp else 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsiveCustomerKpiGrid(
    availableWidth: androidx.compose.ui.unit.Dp,
    totalCustomers: Int,
    activeCustomers: Int,
    vipCustomers: Int,
    totalSpend: Int,
    avgLtv: Int,
    totalOrders: Int,
    totalAddresses: Int
) {
    val isFourColumn = availableWidth >= 600.dp
    val isCompact = availableWidth < 380.dp

    if (isFourColumn) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CustomerKpiMetricCard(
                title = "Total Customers",
                value = "$totalCustomers",
                subtitle = "Verified Mahoba profiles",
                icon = Icons.Default.Groups,
                accentColor = Color(0xFF2563EB),
                isCompact = false,
                modifier = Modifier.weight(1f)
            )
            CustomerKpiMetricCard(
                title = "Active & VIP",
                value = "$activeCustomers",
                subtitle = "$vipCustomers VIP Tier members",
                icon = Icons.Default.VerifiedUser,
                accentColor = Color(0xFF16A34A),
                isCompact = false,
                modifier = Modifier.weight(1f)
            )
            CustomerKpiMetricCard(
                title = "Customer Spend",
                value = "₹$totalSpend",
                subtitle = "Avg LTV: ₹$avgLtv / user",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = FlashDropOrange,
                isCompact = false,
                modifier = Modifier.weight(1f)
            )
            CustomerKpiMetricCard(
                title = "Orders & Addresses",
                value = "$totalOrders",
                subtitle = "$totalAddresses saved addresses",
                icon = Icons.Default.LocationOn,
                accentColor = Color(0xFF7C3AED),
                isCompact = false,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerKpiMetricCard(
                    title = "Total Customers",
                    value = "$totalCustomers",
                    subtitle = "Verified in Mahoba",
                    icon = Icons.Default.Groups,
                    accentColor = Color(0xFF2563EB),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
                CustomerKpiMetricCard(
                    title = "Active & VIP",
                    value = "$activeCustomers",
                    subtitle = "$vipCustomers VIP members",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = Color(0xFF16A34A),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerKpiMetricCard(
                    title = "Customer Spend",
                    value = "₹$totalSpend",
                    subtitle = "Avg LTV: ₹$avgLtv",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = FlashDropOrange,
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
                CustomerKpiMetricCard(
                    title = "Orders & Places",
                    value = "$totalOrders",
                    subtitle = "$totalAddresses addresses",
                    icon = Icons.Default.LocationOn,
                    accentColor = Color(0xFF7C3AED),
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomerKpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, DividerGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 10.dp else 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 26.dp else 30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(if (isCompact) 15.dp else 17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isCompact) 17.sp else 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = accentColor,
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerSearchAndFilterPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    selectedLocalityFilter: String,
    onLocalityFilterChange: (String) -> Unit,
    selectedSortOption: String,
    onSortOptionChange: (String) -> Unit,
    localities: List<String>,
    totalCount: Int,
    activeCount: Int,
    vipCount: Int,
    suspendedCount: Int,
    filteredCount: Int,
    isCompactPhone: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, DividerGray)
    ) {
        Column(modifier = Modifier.padding(if (isCompactPhone) 10.dp else 14.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search customer name, phone, ID, locality or order #...",
                        fontSize = if (isCompactPhone) 12.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = FlashDropOrange)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_customer_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlashDropOrange,
                    unfocusedBorderColor = DividerGray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status filter row
            val statusOptions = listOf(
                "All" to totalCount,
                "Active" to (activeCount - vipCount).coerceAtLeast(0),
                "VIP" to vipCount,
                "Suspended" to suspendedCount
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                statusOptions.forEach { (statusLabel, count) ->
                    val selected = selectedStatusFilter == statusLabel
                    FilterChip(
                        selected = selected,
                        onClick = { onStatusFilterChange(statusLabel) },
                        label = {
                            Text(
                                text = "$statusLabel ($count)",
                                fontSize = if (isCompactPhone) 11.sp else 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E1B4B),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("customer_filter_status_${statusLabel.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Locality & Sort horizontal chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                localities.forEach { area ->
                    val isAreaSelected = selectedLocalityFilter == area
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAreaSelected) FlashDropOrange.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                        border = BorderStroke(
                            1.dp,
                            if (isAreaSelected) FlashDropOrange else Color.Transparent
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLocalityFilterChange(area) }
                    ) {
                        Text(
                            text = area,
                            color = if (isAreaSelected) FlashDropOrange else TextSecondary,
                            fontWeight = if (isAreaSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort bar + Showing count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing $filteredCount of $totalCount customers",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sortChoices = listOf("Most Spent", "Most Orders", "Name A-Z")
                    sortChoices.forEach { sortName ->
                        val active = selectedSortOption == sortName
                        Text(
                            text = sortName,
                            color = if (active) Color(0xFF1E1B4B) else TextSecondary,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFFE0E7FF) else Color.Transparent)
                                .clickable { onSortOptionChange(sortName) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsiveCustomerCard(
    customer: CustomerRecord,
    isSelected: Boolean,
    isCompactPhone: Boolean,
    onClick: () -> Unit,
    onQuickStatusChange: (String) -> Unit,
    onEditClick: () -> Unit
) {
    val statusColor = when (customer.accountStatus.uppercase()) {
        "VIP" -> Color(0xFFD97706)
        "SUSPENDED" -> AlertRed
        else -> TrustGreen
    }
    val statusBg = when (customer.accountStatus.uppercase()) {
        "VIP" -> Color(0xFFFEF3C7)
        "SUSPENDED" -> Color(0xFFFEE2E2)
        else -> Color(0xFFDCFCE7)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF5F3FF) else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF4F46E5) else DividerGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(if (isCompactPhone) 12.dp else 15.dp)) {
            // Top Row: Avatar, Name, ID, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProfileAvatar(
                    displayName = customer.fullName,
                    photoUrl = customer.photoUrl,
                    size = if (isCompactPhone) 44.dp else 50.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customer.fullName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isCompactPhone) 14.sp else 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (customer.isVerified) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified Customer",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "${customer.phone} • ${customer.locality.ifBlank { "Mahoba" }}",
                        color = TextSecondary,
                        fontSize = if (isCompactPhone) 11.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${customer.id} • ${customer.authMethod}",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = customer.accountStatus.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = customer.lastActive,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DividerGray.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Responsive Data Metrics Pill Strip (Total Spent, Orders, Avg Order, Addresses)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CustomerStatMiniPill(
                    label = "Total Spent",
                    value = "₹${customer.totalSpentAmount.toInt()}",
                    valueColor = FlashDropOrange,
                    modifier = Modifier.weight(1f)
                )
                CustomerStatMiniPill(
                    label = "Orders",
                    value = "${customer.totalOrdersCount}",
                    valueColor = Color(0xFF1E1B4B),
                    modifier = Modifier.weight(1f)
                )
                CustomerStatMiniPill(
                    label = "Avg Order",
                    value = "₹${customer.averageOrderValue.toInt()}",
                    valueColor = TrustGreen,
                    modifier = Modifier.weight(1f)
                )
                CustomerStatMiniPill(
                    label = "Addresses",
                    value = "${customer.savedAddresses.size.coerceAtLeast(1)}",
                    valueColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Delivery Address & Favorite Shop summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = customer.primaryAddress.ifBlank { "${customer.locality}, Mahoba - 210427" },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row: View Full Data, Edit, Status Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextStatus = when (customer.accountStatus.uppercase()) {
                        "ACTIVE" -> CustomerRecord.STATUS_VIP
                        "VIP" -> CustomerRecord.STATUS_ACTIVE
                        else -> CustomerRecord.STATUS_ACTIVE
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickStatusChange(nextStatus) }
                            .testTag("toggle_customer_vip_${customer.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (customer.accountStatus.equals("VIP", ignoreCase = true)) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (customer.accountStatus.equals("VIP", ignoreCase = true)) "VIP Member" else "Make VIP",
                                color = Color(0xFF4F46E5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_customer_${customer.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Customer",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("inspect_customer_${customer.id}")
                ) {
                    Text(
                        text = "View Customer Data",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerStatMiniPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, DividerGray.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CustomerDetailInspectorContent(
    customer: CustomerRecord,
    isCompactPhone: Boolean,
    onStatusChange: (String) -> Unit,
    onEditCustomer: () -> Unit
) {
    val totalSpend = customer.totalSpentAmount.coerceAtLeast(0.0)
    val upiShare = if (totalSpend > 0) (customer.upiSpentAmount / totalSpend).toFloat().coerceIn(0f, 1f) else 0.6f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Customer Identity Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProfileAvatar(
                        displayName = customer.fullName,
                        photoUrl = customer.photoUrl,
                        size = if (isCompactPhone) 52.dp else 60.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.fullName,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isCompactPhone) 16.sp else 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (customer.accountStatus.uppercase()) {
                                    "VIP" -> Color(0xFFF59E0B)
                                    "SUSPENDED" -> AlertRed
                                    else -> Color(0xFF22C55E)
                                }
                            ) {
                                Text(
                                    text = customer.accountStatus.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Customer UID: ${customer.id} • Auth: ${customer.authMethod}",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Joined: ${customer.joinedDate} • Last Active: ${customer.lastActive}",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Admin Governance Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditCustomer,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("edit_customer_data_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Edit Profile & Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    listOf(
                        CustomerRecord.STATUS_ACTIVE to Color(0xFF22C55E),
                        CustomerRecord.STATUS_VIP to Color(0xFFF59E0B),
                        CustomerRecord.STATUS_SUSPENDED to AlertRed
                    ).forEach { (statusOption, chipColor) ->
                        val isCurrent = customer.accountStatus.equals(statusOption, ignoreCase = true)
                        Button(
                            onClick = { onStatusChange(statusOption) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) chipColor else Color.White.copy(alpha = 0.14f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = statusOption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Contact & Location Profile Data
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, DividerGray)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CONTACT & PERSONAL DATA",
                    color = Color(0xFF1E1B4B),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
                CustomerDataRow(icon = Icons.Default.Phone, label = "Phone Number", value = customer.phone)
                CustomerDataRow(icon = Icons.Default.Email, label = "Email Address", value = customer.email.ifBlank { "Not provided" })
                CustomerDataRow(
                    icon = Icons.Default.LocationCity,
                    label = "Locality & PIN",
                    value = "${customer.locality}, ${customer.city} (${customer.pinCode})"
                )
                CustomerDataRow(
                    icon = Icons.Default.Home,
                    label = "Primary Address",
                    value = customer.primaryAddress
                )
                if (customer.favoriteShopName.isNotBlank()) {
                    CustomerDataRow(
                        icon = Icons.Default.Storefront,
                        label = "Most Ordered Shop",
                        value = customer.favoriteShopName
                    )
                }
                if (customer.adminNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Note: ${customer.adminNotes}",
                                color = Color(0xFF92400E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 3. Spending & Payment Analytics (UPI vs COD Breakdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, DividerGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SPENDING & ORDER ANALYTICS",
                    color = Color(0xFF1E1B4B),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerStatMiniPill(
                        label = "Lifetime Spend",
                        value = "₹${customer.totalSpentAmount.toInt()}",
                        valueColor = FlashDropOrange,
                        modifier = Modifier.weight(1f)
                    )
                    CustomerStatMiniPill(
                        label = "Avg Order Value",
                        value = "₹${customer.averageOrderValue.toInt()}",
                        valueColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    CustomerStatMiniPill(
                        label = "Delivered",
                        value = "${customer.completedOrdersCount}/${customer.totalOrdersCount}",
                        valueColor = TrustGreen,
                        modifier = Modifier.weight(1f)
                    )
                    CustomerStatMiniPill(
                        label = "Cancelled",
                        value = "${customer.cancelledOrdersCount}",
                        valueColor = if (customer.cancelledOrdersCount > 0) AlertRed else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "UPI Paid: ₹${customer.upiSpentAmount.toInt()}",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "COD Paid: ₹${customer.codSpentAmount.toInt()}",
                        color = TrustGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE5E7EB))
                ) {
                    if (upiShare > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(upiShare.coerceAtLeast(0.05f))
                                .background(Color(0xFF2563EB))
                        )
                    }
                    if (1f - upiShare > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((1f - upiShare).coerceAtLeast(0.05f))
                                .background(TrustGreen)
                        )
                    }
                }
            }
        }

        // 4. Saved Delivery Addresses Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, DividerGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED DELIVERY ADDRESSES (${customer.savedAddresses.size})",
                        color = Color(0xFF1E1B4B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = FlashDropOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (customer.savedAddresses.isEmpty()) {
                    Text(
                        text = customer.primaryAddress.ifBlank { "No saved addresses recorded" },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    customer.savedAddresses.forEach { addr ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (addr.isDefault) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (addr.isDefault) FlashDropOrange.copy(alpha = 0.4f) else DividerGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            when (addr.label.lowercase()) {
                                                "work", "office" -> Icons.Default.Work
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = null,
                                            tint = FlashDropOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${addr.label} • ${addr.name.ifBlank { customer.fullName }}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    }
                                    if (addr.isDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = FlashDropOrange
                                        ) {
                                            Text(
                                                text = "DEFAULT",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 8.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = addr.formatDisplay(),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                if (addr.landmark.isNotBlank()) {
                                    Text(
                                        text = "Landmark: ${addr.landmark}",
                                        color = Color(0xFF4F46E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Order History Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, DividerGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ORDERS (${customer.recentOrders.size})",
                        color = Color(0xFF1E1B4B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Last Order: ${customer.lastOrderDate}",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (customer.recentOrders.isEmpty()) {
                    Text(
                        text = "No orders placed by this customer yet.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    customer.recentOrders.forEach { orderSummary ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, DividerGray.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = orderSummary.orderNumber,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1E1B4B)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (orderSummary.paymentMethod.equals("UPI", ignoreCase = true)) {
                                                Color(0xFFDBEAFE)
                                            } else {
                                                Color(0xFFDCFCE7)
                                            }
                                        ) {
                                            Text(
                                                text = orderSummary.paymentMethod.uppercase(),
                                                color = if (orderSummary.paymentMethod.equals("UPI", ignoreCase = true)) {
                                                    Color(0xFF1D4ED8)
                                                } else {
                                                    Color(0xFF15803D)
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${orderSummary.shopName} • ${orderSummary.itemsCount} items",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = orderSummary.createdAt,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${orderSummary.totalAmount.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = FlashDropOrange
                                    )
                                    Text(
                                        text = orderSummary.status.replace("_", " ").uppercase(),
                                        color = if (orderSummary.status.equals("delivered", ignoreCase = true)) TrustGreen else Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
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
private fun CustomerDataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = FlashDropOrange,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CustomerDetailInspectorDialog(
    customer: CustomerRecord,
    screenType: ScreenType,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEditCustomer: () -> Unit
) {
    val isCompactPhone = screenType == ScreenType.SMALL_MOBILE
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isCompactPhone) 0.96f else 0.92f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = FlashDropOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customer Data & History",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_customer_inspector_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    CustomerDetailInspectorContent(
                        customer = customer,
                        isCompactPhone = isCompactPhone,
                        onStatusChange = onStatusChange,
                        onEditCustomer = onEditCustomer
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminEditCustomerDialog(
    customer: CustomerRecord,
    onDismiss: () -> Unit,
    onSave: (fullName: String, phone: String, email: String, locality: String, address: String, status: String, notes: String) -> Unit
) {
    var fullName by remember(customer) { mutableStateOf(customer.fullName) }
    var phone by remember(customer) { mutableStateOf(customer.phone) }
    var email by remember(customer) { mutableStateOf(customer.email) }
    var locality by remember(customer) { mutableStateOf(customer.locality) }
    var primaryAddress by remember(customer) { mutableStateOf(customer.primaryAddress) }
    var accountStatus by remember(customer) { mutableStateOf(customer.accountStatus) }
    var adminNotes by remember(customer) { mutableStateOf(customer.adminNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Customer Data",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Customer Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Mahoba Locality (e.g. Alha Chowk)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = primaryAddress,
                    onValueChange = { primaryAddress = it },
                    label = { Text("Primary Delivery Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Account Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        CustomerRecord.STATUS_ACTIVE,
                        CustomerRecord.STATUS_VIP,
                        CustomerRecord.STATUS_SUSPENDED
                    ).forEach { statusOpt ->
                        FilterChip(
                            selected = accountStatus.equals(statusOpt, ignoreCase = true),
                            onClick = { accountStatus = statusOpt },
                            label = { Text(statusOpt, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = { Text("Admin Internal Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(fullName, phone, email, locality, primaryAddress, accountStatus, adminNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlashDropOrange),
                modifier = Modifier.testTag("save_customer_edit_button")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminAddCustomerDialog(
    onDismiss: () -> Unit,
    onAdd: (fullName: String, phone: String, email: String, locality: String, address: String, status: String, notes: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 ") }
    var email by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("Alha Chowk") }
    var primaryAddress by remember { mutableStateOf("") }
    var accountStatus by remember { mutableStateOf(CustomerRecord.STATUS_ACTIVE) }
    var adminNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Customer to Directory",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Customer Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("10-Digit Mobile Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_customer_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Mahoba Locality") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = primaryAddress,
                    onValueChange = { primaryAddress = it },
                    label = { Text("Complete Delivery Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Initial Tier / Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        CustomerRecord.STATUS_ACTIVE,
                        CustomerRecord.STATUS_VIP
                    ).forEach { statusOpt ->
                        FilterChip(
                            selected = accountStatus.equals(statusOpt, ignoreCase = true),
                            onClick = { accountStatus = statusOpt },
                            label = { Text(statusOpt, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = { Text("Admin Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(fullName, phone, email, locality, primaryAddress, accountStatus, adminNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlashDropOrange),
                modifier = Modifier.testTag("confirm_add_customer_button")
            ) {
                Text("Add Customer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyCustomerFilterState(
    onResetFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, DividerGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PersonSearch,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No matching customers found",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = "Try adjusting your search query, status filter, or Mahoba locality filter.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reset All Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
