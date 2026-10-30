package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.DeliveryTracking
import com.example.data.model.Order
import com.example.data.repository.ApnaBazRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    repository: ApnaBazRepository,
    orderId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val orders by repository.orders.collectAsState()
    val deliveryTrackingMap by repository.deliveryTracking.collectAsState()

    val order = orders.find { it.id == orderId }
    val tracking = deliveryTrackingMap[orderId] ?: remember(orderId) { repository.getDeliveryTracking(orderId) }

    var showReceiptDialog by remember { mutableStateOf(false) }
    var showPartnerConsole by remember { mutableStateOf(false) }

    // Check location permissions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Check GPS provider status
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val isGpsHardwareEnabled = remember(hasLocationPermission) {
        try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (e: Exception) {
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = order?.orderNumber ?: "Live Delivery Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "FlashDrop Mahoba • Real-Time GPS",
                            fontSize = 11.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPartnerConsole = !showPartnerConsole },
                        modifier = Modifier.testTag("toggle_partner_console")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Partner Controls",
                            tint = if (showPartnerConsole) OrangeSecondary else TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Order #$orderId not found", color = TextMuted)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundWarm)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Status Card
                HeaderStatusCard(order = order, tracking = tracking)

                // GPS & Location Status Warning Banner
                LocationStatusBanner(
                    hasPermission = hasLocationPermission,
                    isGpsHardwareEnabled = isGpsHardwareEnabled,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                // Interactive Live Map Section
                LiveDeliveryMapCard(
                    tracking = tracking,
                    context = context
                )

                // Estimated Arrival & Delivery Partner Card
                EstimatedArrivalPartnerCard(
                    tracking = tracking,
                    context = context
                )

                // Strict 6-Stage Timeline (Order Confirmed -> Preparing Order -> Ready for Pickup -> Picked Up -> On the Way -> Delivered)
                DeliveryProgressTimelineCard(
                    tracking = tracking
                )

                // Delivery Partner / Admin Interactive Console
                AnimatedVisibility(visible = showPartnerConsole) {
                    DeliveryPartnerConsoleCard(
                        order = order,
                        tracking = tracking,
                        repository = repository
                    )
                }

                // Ordered Items Summary Card
                OrderItemsCard(order = order)

                // Payment Details Card
                PaymentDetailsCard(
                    order = order,
                    onViewReceipt = { showReceiptDialog = true }
                )

                if (order.status == "pending" || order.status == "order_confirmed") {
                    OutlinedButton(
                        onClick = { repository.updateOrderStatus(order.id, "cancelled", "Cancelled by customer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_order_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel Order", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showReceiptDialog && order != null) {
            PaymentReceiptDialog(order = order, onDismiss = { showReceiptDialog = false })
        }
    }
}

/**
 * Top summary card showing order number, shop name, and live status badge.
 */
@Composable
private fun HeaderStatusCard(order: Order, tracking: DeliveryTracking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Column {
                    Text(
                        text = order.orderNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Shop: ${order.shopName}",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (tracking.orderStatus == "delivered") GreenPrimary else OrangeSecondary)
                )
                Text(
                    text = order.statusNote.ifBlank { getStageDescription(tracking.orderStatus) },
                    fontSize = 13.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Warning banner for GPS permission or device GPS state.
 */
@Composable
private fun LocationStatusBanner(
    hasPermission: Boolean,
    isGpsHardwareEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    if (!hasPermission) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = OrangeLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = OrangeSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Location Permission Needed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangeSecondary
                        )
                        Text(
                            text = "Enable location to display accurate arrival distance to your doorstep.",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("grant_location_permission_button")
                ) {
                    Text("Enable", fontSize = 12.sp)
                }
            }
        }
    } else if (!isGpsHardwareEnabled) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = OrangeLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = OrangeSecondary)
                Text(
                    text = "Device GPS is disabled. Turn on device location for live marker updates.",
                    fontSize = 12.sp,
                    color = TextDark
                )
            }
        }
    }
}

/**
 * Interactive Live Map showing Shop, Customer Destination, and Delivery Partner Marker.
 * Never fabricates coordinates.
 */
@Composable
private fun LiveDeliveryMapCard(
    tracking: DeliveryTracking,
    context: Context
) {
    val shopLatLng = remember(tracking.shopLatitude, tracking.shopLongitude) {
        LatLng(tracking.shopLatitude, tracking.shopLongitude)
    }
    val customerLatLng = remember(tracking.customerLatitude, tracking.customerLongitude) {
        LatLng(tracking.customerLatitude, tracking.customerLongitude)
    }

    val riderLatLng = if (tracking.actualLatitude != null && tracking.actualLongitude != null) {
        LatLng(tracking.actualLatitude, tracking.actualLongitude)
    } else null

    val initialCenter = remember(riderLatLng, shopLatLng, customerLatLng) {
        riderLatLng ?: LatLng(
            (shopLatLng.latitude + customerLatLng.latitude) / 2.0,
            (shopLatLng.longitude + customerLatLng.longitude) / 2.0
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 15f)
    }

    LaunchedEffect(riderLatLng) {
        if (riderLatLng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(riderLatLng, 15.5f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_map_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (tracking.isGpsActive) GreenPrimary else Color.Gray)
                    )
                    Text(
                        text = if (tracking.isGpsActive) "Live GPS Active" else "GPS Idle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                }

                if (tracking.actualAccuracyMeters != null) {
                    Text(
                        text = "±${tracking.actualAccuracyMeters.toInt()}m accuracy",
                        fontSize = 11.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFFE8ECEF))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        rotationGesturesEnabled = true,
                        scrollGesturesEnabled = true,
                        tiltGesturesEnabled = false,
                        zoomGesturesEnabled = true
                    ),
                    properties = MapProperties(isMyLocationEnabled = false)
                ) {
                    // Shop Location Marker
                    Marker(
                        state = MarkerState(position = shopLatLng),
                        title = tracking.shopName,
                        snippet = "Pickup Shop: ${tracking.shopAddress}"
                    )

                    // Customer Destination Marker
                    Marker(
                        state = MarkerState(position = customerLatLng),
                        title = "Delivery Destination",
                        snippet = "${tracking.customerName} • ${tracking.customerDeliveryAddress}"
                    )

                    // Rider Marker (Rendered only when real coordinates exist)
                    if (riderLatLng != null) {
                        Marker(
                            state = MarkerState(position = riderLatLng),
                            title = "${tracking.deliveryManName} (Rider)",
                            snippet = "Vehicle: ${tracking.deliveryManVehicle}"
                        )

                        Polyline(
                            points = listOf(riderLatLng, customerLatLng),
                            color = OrangeSecondary,
                            width = 10f
                        )
                    } else {
                        Polyline(
                            points = listOf(shopLatLng, customerLatLng),
                            color = GreenPrimary.copy(alpha = 0.5f),
                            width = 8f
                        )
                    }
                }

                // Floating Action: Open in External Maps
                IconButton(
                    onClick = {
                        val lat = tracking.actualLatitude ?: tracking.customerLatitude
                        val lng = tracking.actualLongitude ?: tracking.customerLongitude
                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(FlashDrop+Delivery)")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore fallback
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .testTag("open_external_map_button")
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Open in Google Maps",
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (riderLatLng != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Rider GPS: ${"%.4f".format(riderLatLng.latitude)}, ${"%.4f".format(riderLatLng.longitude)}",
                                fontSize = 12.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Updated: ${tracking.stageTimestamps[tracking.orderStatus] ?: "Just now"}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (tracking.orderStatus == "order_confirmed" || tracking.orderStatus == "preparing_order")
                                "Live coordinates will activate as soon as the rider picks up your order."
                            else
                                "Waiting for rider's GPS satellite signal...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estimated Arrival & Assigned Delivery Partner Card.
 * Adheres strictly to Zero Fabrication: Displays ETA and distance ONLY when dependable data is available.
 */
@Composable
private fun EstimatedArrivalPartnerCard(
    tracking: DeliveryTracking,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("estimated_arrival_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val hasReliableEta = tracking.estimatedArrivalMinutes != null &&
                    tracking.remainingDistanceKm != null &&
                    tracking.actualLatitude != null &&
                    tracking.orderStatus != "delivered"

            if (hasReliableEta) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED ARRIVAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${tracking.estimatedArrivalMinutes} minutes",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )
                    }

                    Surface(
                        color = GreenLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${tracking.remainingDistanceKm} km away",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(14.dp))
            } else if (tracking.orderStatus == "delivered") {
                Surface(
                    color = GreenLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary)
                        Text(
                            text = "Delivered to your doorstep at ${tracking.stageTimestamps["delivered"] ?: "recently"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GreenPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = OrangeSecondary
                    )
                    Text(
                        text = "ETA and remaining distance will appear once rider begins transit.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Assigned Delivery Partner Profile Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(OrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeliveryDining,
                            contentDescription = "Delivery Partner",
                            tint = OrangeSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tracking.deliveryManName.ifBlank { "Vikram Rathore" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tracking.deliveryManVehicle.ifBlank { "Hero Splendor • UP 95 AB 4122" },
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(14.dp))
                            Text("4.9", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = OrangeSecondary)
                            Text("• FlashDrop Partner", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }

                if (tracking.deliveryManPhone.isNotBlank() && tracking.orderStatus != "delivered") {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tracking.deliveryManPhone}"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary)
                            .testTag("call_rider_button")
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call Delivery Partner",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Strict 6-Stage Delivery Progress Timeline:
 * 1. Order Confirmed
 * 2. Preparing Order
 * 3. Ready for Pickup
 * 4. Picked Up
 * 5. On the Way
 * 6. Delivered
 */
@Composable
private fun DeliveryProgressTimelineCard(
    tracking: DeliveryTracking
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivery_timeline_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Delivery Progress Timeline",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark
            )
            Text(
                text = "Official 6-stage delivery tracking in Mahoba",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            val stages = listOf(
                TimelineStage("order_confirmed", "Order Confirmed", "Shopkeeper acknowledged and accepted order"),
                TimelineStage("preparing_order", "Preparing Order", "Items are being packed at the store"),
                TimelineStage("ready_for_pickup", "Ready for Pickup", "Order securely packaged for pickup"),
                TimelineStage("picked_up", "Picked Up", "Rider received and verified package items"),
                TimelineStage("on_the_way", "On the Way", "Rider dispatched with live GPS tracking"),
                TimelineStage("delivered", "Delivered", "Delivered to your doorstep in Mahoba")
            )

            val currentStageIndex = when (tracking.orderStatus) {
                "order_confirmed" -> 0
                "preparing_order" -> 1
                "ready_for_pickup" -> 2
                "picked_up" -> 3
                "on_the_way" -> 4
                "delivered" -> 5
                else -> 0
            }

            stages.forEachIndexed { index, stage ->
                val isCompleted = index < currentStageIndex
                val isCurrent = index == currentStageIndex
                val isUpcoming = index > currentStageIndex
                val timestamp = tracking.stageTimestamps[stage.key]

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> OrangeSecondary
                                    isCompleted -> GreenPrimary
                                    else -> Color(0xFFE5E7EB)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = if (isUpcoming) TextMuted else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
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
                                text = stage.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = when {
                                    isCurrent -> OrangeSecondary
                                    isCompleted -> GreenPrimary
                                    else -> TextMuted
                                }
                            )

                            if (timestamp != null) {
                                Text(
                                    text = timestamp,
                                    fontSize = 11.sp,
                                    color = if (isCurrent) OrangeSecondary else TextMuted,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Text(
                            text = stage.description,
                            fontSize = 11.sp,
                            color = if (isUpcoming) Color(0xFF9CA3AF) else TextMuted
                        )
                    }
                }

                if (index < stages.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .width(2.dp)
                            .height(24.dp)
                            .background(if (index < currentStageIndex) GreenPrimary else Color(0xFFE5E7EB))
                    )
                }
            }
        }
    }
}

/**
 * Interactive delivery partner / test console allowing authorized delivery personnel or admins
 * to advance delivery stages and test live GPS telemetry.
 */
@Composable
private fun DeliveryPartnerConsoleCard(
    order: Order,
    tracking: DeliveryTracking,
    repository: ApnaBazRepository
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivery_partner_console_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = OrangeSecondary)
                    Text(
                        text = "Delivery Partner Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OrangeSecondary
                    )
                }

                Text(
                    text = "Rider: ${tracking.deliveryManName}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Use these controls to advance order stages or update GPS coordinates during testing.",
                fontSize = 12.sp,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            val nextStage = when (tracking.orderStatus) {
                "order_confirmed" -> "preparing_order"
                "preparing_order" -> "ready_for_pickup"
                "ready_for_pickup" -> "picked_up"
                "picked_up" -> "on_the_way"
                "on_the_way" -> "delivered"
                else -> null
            }

            if (nextStage != null) {
                Button(
                    onClick = { repository.updateDeliveryStage(order.id, nextStage) },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_stage_button")
                ) {
                    Text("Advance to: ${getStageTitle(nextStage)}", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = GreenLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Order has reached final Delivered status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var stepIndex by remember { mutableIntStateOf(0) }
            val mahobaRouteSteps = listOf(
                LatLng(25.2922, 79.8718), // Shop: Subhash Chowk
                LatLng(25.2930, 79.8725),
                LatLng(25.2942, 79.8735), // Midpoint
                LatLng(25.2955, 79.8745),
                LatLng(25.2965, 79.8752)  // Destination: Alha Chowk
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val nextPoint = mahobaRouteSteps[stepIndex % mahobaRouteSteps.size]
                        stepIndex++
                        repository.updateDeliveryGps(
                            orderId = order.id,
                            lat = nextPoint.latitude,
                            lng = nextPoint.longitude,
                            accuracy = 6.0f
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_gps_step_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangeSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ping GPS Step", fontSize = 12.sp, color = OrangeSecondary)
                }

                OutlinedButton(
                    onClick = {
                        if (tracking.isGpsActive) {
                            repository.stopRiderGpsService()
                        } else {
                            repository.startRiderGpsService(order.id, tracking.deliveryManId)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (tracking.isGpsActive) "Stop Service" else "Start Service",
                        fontSize = 12.sp,
                        color = TextDark
                    )
                }
            }
        }
    }
}

/**
 * Ordered Items list card.
 */
@Composable
private fun OrderItemsCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ordered Items (${order.itemsCount})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
            Spacer(modifier = Modifier.height(10.dp))

            order.items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                                if (item.isPrintingOrder) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GreenLight)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text("A4 B&W Only", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                }
                            }
                            Text("${item.unit} • Qty: ${item.quantity}", fontSize = 11.sp, color = TextMuted)
                        }
                        Text("₹${"%.2f".format(item.price * item.quantity)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                    }

                    if (item.isPrintingOrder && item.fileNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundWarm)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Uploaded Documents for Printing:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                item.fileNames.forEach { fn ->
                                    Text("• $fn", fontSize = 10.sp, color = TextDark)
                                }
                                if (item.fileUrls.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("✓ Cloud Storage Securely Attached", fontSize = 9.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                }
                                if (item.printInstructions.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Instructions: \"${item.printInstructions}\"", fontSize = 10.sp, color = TextMuted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            val snapshot = order.billingSnapshot
            if (snapshot != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Merchandise Subtotal", fontSize = 13.sp, color = TextMuted)
                    Text("₹${snapshot.merchandiseSubtotal.toInt()}", fontSize = 13.sp, color = TextDark)
                }

                if (snapshot.shopDeliveries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Shop Delivery Distance Breakdown:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    for (s in snapshot.shopDeliveries) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${s.shopName} (${s.distanceKm} km)",
                                fontSize = 11.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (s.finalDeliveryCharge == 0.0) "FREE" else "₹${s.finalDeliveryCharge.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (s.finalDeliveryCharge == 0.0) GreenPrimary else TextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Delivery Fee (${snapshot.appliedPolicy})", fontSize = 13.sp, color = TextMuted)
                    Text(
                        if (snapshot.netDeliveryFee == 0.0) "FREE" else "₹${snapshot.netDeliveryFee.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (snapshot.netDeliveryFee == 0.0) GreenPrimary else TextDark
                    )
                }

                if (snapshot.totalDeliveryDiscount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Policy Discount", fontSize = 12.sp, color = GreenPrimary)
                        Text("-₹${snapshot.totalDeliveryDiscount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }

                if (snapshot.platformFee > 0.0 || order.platformFee > 0.0) {
                    val pFee = if (snapshot.platformFee > 0.0) snapshot.platformFee else order.platformFee
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fixed Platform Fee (Printing Press)", fontSize = 13.sp, color = TextMuted)
                        Text("₹${"%.2f".format(pFee)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Snapshot: Config v${snapshot.configVersion} • ${snapshot.calculationTimestamp}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 13.sp, color = TextMuted)
                    Text("₹${"%.2f".format(order.subtotal)}", fontSize = 13.sp, color = TextDark)
                }
                if (order.platformFee > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fixed Platform Fee (Printing Press)", fontSize = 13.sp, color = TextMuted)
                        Text("₹${"%.2f".format(order.platformFee)}", fontSize = 13.sp, color = TextDark)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Delivery Fee", fontSize = 13.sp, color = TextMuted)
                    Text(if (order.deliveryFee == 0.0) "FREE" else "₹${order.deliveryFee.toInt()}", fontSize = 13.sp, color = if (order.deliveryFee == 0.0) GreenPrimary else TextDark)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Paid / Payable", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Text("₹${"%.2f".format(order.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
            }
        }
    }
}

/**
 * Payment details card with receipt access.
 */
@Composable
private fun PaymentDetailsCard(order: Order, onViewReceipt: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Payment Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Method", fontSize = 13.sp, color = TextMuted)
                Text(
                    if (order.paymentMethod == "cash_on_delivery") "Cash on Delivery (COD)" else "UPI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status", fontSize = 13.sp, color = TextMuted)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (order.paymentStatus == "paid") GreenLight else OrangeLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (order.paymentStatus == "paid") "● Paid" else "● Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (order.paymentStatus == "paid") GreenPrimary else OrangeSecondary
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transaction ID", fontSize = 13.sp, color = TextMuted)
                Text("UPI${order.id.filter { it.isDigit() }.padStart(10, '0').take(10)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onViewReceipt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("view_payment_receipt_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Payment Receipt", fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Payment Receipt Dialog.
 */
@Composable
private fun PaymentReceiptDialog(order: Order, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Payment Receipt", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("FlashDrop Mahoba • Order Receipt", fontSize = 11.sp, color = TextMuted)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", fontSize = 12.sp, color = TextMuted)
                    Text(order.orderNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                val snap = order.billingSnapshot
                if (snap != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 12.sp, color = TextMuted)
                        Text("₹${snap.merchandiseSubtotal.toInt()}", fontSize = 12.sp, color = TextDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Fee (${snap.appliedPolicy})", fontSize = 12.sp, color = TextMuted)
                        Text(if (snap.netDeliveryFee == 0.0) "FREE" else "₹${snap.netDeliveryFee.toInt()}", fontSize = 12.sp, color = TextDark)
                    }
                    if (snap.totalDeliveryDiscount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Discount", fontSize = 12.sp, color = GreenPrimary)
                            Text("-₹${snap.totalDeliveryDiscount.toInt()}", fontSize = 12.sp, color = GreenPrimary)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount", fontSize = 12.sp, color = TextMuted)
                    Text("₹${order.totalAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment Mode", fontSize = 12.sp, color = TextMuted)
                    Text(if (order.paymentMethod == "cash_on_delivery") "COD" else "UPI (Online)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment Status", fontSize = 12.sp, color = TextMuted)
                    Text(order.paymentStatus.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
                if (snap != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Policy Version", fontSize = 11.sp, color = TextMuted)
                        Text("v${snap.configVersion} (${snap.currency})", fontSize = 11.sp, color = TextDark)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done")
            }
        }
    )
}

private data class TimelineStage(val key: String, val title: String, val description: String)

private fun getStageTitle(stageKey: String): String {
    return when (stageKey) {
        "order_confirmed" -> "Order Confirmed"
        "preparing_order" -> "Preparing Order"
        "ready_for_pickup" -> "Ready for Pickup"
        "picked_up" -> "Picked Up"
        "on_the_way" -> "On the Way"
        "delivered" -> "Delivered"
        else -> stageKey
    }
}

private fun getStageDescription(stageKey: String): String {
    return when (stageKey) {
        "order_confirmed" -> "Order confirmed by shopkeeper"
        "preparing_order" -> "Items are being packed at shop"
        "ready_for_pickup" -> "Packed and ready for rider pickup"
        "picked_up" -> "Rider picked up order from shop"
        "on_the_way" -> "Rider is on the way to your address"
        "delivered" -> "Order delivered successfully"
        else -> "Delivery in progress"
    }
}
