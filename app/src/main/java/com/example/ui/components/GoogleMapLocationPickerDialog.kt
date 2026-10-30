package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Mahoba center coordinates and landmark localities
private val MAHOBA_CENTER = LatLng(25.2937, 79.8722)

data class MahobaAreaLocation(
    val name: String,
    val latLng: LatLng
)

private val MAHOBA_LOCALITIES = listOf(
    MahobaAreaLocation("Alha Chowk", LatLng(25.2915, 79.8730)),
    MahobaAreaLocation("Subhash Chowk", LatLng(25.2940, 79.8715)),
    MahobaAreaLocation("Gandhi Nagar", LatLng(25.2980, 79.8690)),
    MahobaAreaLocation("Civil Lines", LatLng(25.2890, 79.8780)),
    MahobaAreaLocation("Station Road", LatLng(25.2850, 79.8820)),
    MahobaAreaLocation("Kirat Sagar", LatLng(25.2820, 79.8650)),
    MahobaAreaLocation("Bhatipura", LatLng(25.3010, 79.8760)),
    MahobaAreaLocation("Kasba", LatLng(25.2960, 79.8740))
)

@Composable
fun GoogleMapLocationPickerDialog(
    initialAddress: String = "",
    initialLocality: String = "Alha Chowk",
    initialPinCode: String = "210427",
    onDismiss: () -> Unit,
    onLocationConfirmed: (address: String, locality: String, pinCode: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // GPS hardware state
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    var isGpsHardwareEnabled by remember {
        mutableStateOf(
            try {
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            } catch (e: Exception) {
                true
            }
        )
    }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showGpsDisabledAlert by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Selected location coordinates
    val initialLatLng = remember(initialLocality) {
        MAHOBA_LOCALITIES.find { it.name.equals(initialLocality, ignoreCase = true) }?.latLng ?: MAHOBA_CENTER
    }

    var selectedLatLng by remember { mutableStateOf(initialLatLng) }
    var selectedLocality by remember { mutableStateOf(initialLocality.ifBlank { "Alha Chowk" }) }
    var streetAddressInput by remember { mutableStateOf(initialAddress.ifBlank { "Near Main Market" }) }
    var pinCodeInput by remember { mutableStateOf(initialPinCode.ifBlank { "210427" }) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15.5f)
    }

    // Permission launcher for Location/GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted

        if (hasLocationPermission) {
            statusMessage = "GPS Permission granted. Pinpointing your live location..."
            // Check GPS hardware
            val gpsOn = try {
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            } catch (e: Exception) { true }
            isGpsHardwareEnabled = gpsOn

            if (!gpsOn) {
                showGpsDisabledAlert = true
            } else {
                fetchCurrentGpsLocation(
                    context = context,
                    onLocationFound = { loc ->
                        val target = LatLng(loc.latitude, loc.longitude)
                        selectedLatLng = target
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16.5f))
                        }
                        reverseGeocodeCoordinates(context, target) { detectedLocality, detectedStreet ->
                            selectedLocality = detectedLocality
                            if (streetAddressInput.isBlank() || streetAddressInput == "Near Main Market") {
                                streetAddressInput = detectedStreet
                            }
                            statusMessage = "Live GPS Location locked: $detectedLocality"
                        }
                    },
                    onError = { err ->
                        statusMessage = err
                    }
                )
            }
        } else {
            statusMessage = "GPS permission was denied. You can select your location on the map manually."
        }
    }

    // Helper to request GPS permission or fetch location
    val requestGpsAndCenter = {
        if (!hasLocationPermission) {
            showPermissionRationaleDialog = true
        } else {
            val gpsOn = try {
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            } catch (e: Exception) { true }
            isGpsHardwareEnabled = gpsOn

            if (!gpsOn) {
                showGpsDisabledAlert = true
            } else {
                isDetectingLocation = true
                statusMessage = "Acquiring live GPS coordinates..."
                fetchCurrentGpsLocation(
                    context = context,
                    onLocationFound = { loc ->
                        isDetectingLocation = false
                        val target = LatLng(loc.latitude, loc.longitude)
                        selectedLatLng = target
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16.5f))
                        }
                        reverseGeocodeCoordinates(context, target) { detectedLocality, detectedStreet ->
                            selectedLocality = detectedLocality
                            if (streetAddressInput.isBlank() || streetAddressInput == "Near Main Market") {
                                streetAddressInput = detectedStreet
                            }
                            statusMessage = "GPS position locked: $detectedLocality"
                        }
                    },
                    onError = { err ->
                        isDetectingLocation = false
                        statusMessage = err
                    }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_map_location_picker_dialog")
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Choose Delivery Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Mahoba, Uttar Pradesh • Google Maps",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_location_picker_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                    }
                }

                HorizontalDivider(color = BorderLight)

                // GPS Permission & Detection Action Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("detect_gps_action_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasLocationPermission) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (hasLocationPermission) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasLocationPermission) Icons.Default.MyLocation else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasLocationPermission) GreenPrimary else OrangeSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasLocationPermission) "GPS Ready" else "GPS Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = if (hasLocationPermission) {
                                        "Pinpoint your exact doorstep on Google Maps"
                                    } else {
                                        "Allow GPS to auto-detect your delivery address"
                                    },
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Button(
                            onClick = { requestGpsAndCenter() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasLocationPermission) GreenPrimary else OrangeSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("use_gps_location_button")
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hasLocationPermission) "Detect GPS" else "Allow GPS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Status notice if any
                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("location_picker_status_text")
                    )
                }

                // Interactive Google Map Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFE5E7EB))
                ) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("google_map_picker_view"),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { clickedLatLng ->
                            selectedLatLng = clickedLatLng
                            reverseGeocodeCoordinates(context, clickedLatLng) { detectedLocality, detectedStreet ->
                                selectedLocality = detectedLocality
                                streetAddressInput = detectedStreet
                            }
                        },
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            compassEnabled = true,
                            mapToolbarEnabled = true
                        ),
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission
                        )
                    ) {
                        // User destination marker
                        Marker(
                            state = MarkerState(position = selectedLatLng),
                            title = "Deliver Here",
                            snippet = "$streetAddressInput, $selectedLocality",
                            draggable = true
                        )
                    }

                    // Floating Recenter on GPS button on bottom-right of map
                    FloatingActionButton(
                        onClick = { requestGpsAndCenter() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .size(44.dp)
                            .testTag("recenter_gps_fab"),
                        containerColor = Color.White,
                        contentColor = GreenPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My GPS Location",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Mahoba Locality Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Quick Select Mahoba Locality:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(MAHOBA_LOCALITIES) { area ->
                            val isSelected = selectedLocality.equals(area.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLocality = area.name
                                    selectedLatLng = area.latLng
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(area.latLng, 16f)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = area.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenLight,
                                    selectedLabelColor = GreenPrimary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Selected Address Details Card & Confirmation Inputs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = streetAddressInput,
                        onValueChange = { streetAddressInput = it },
                        label = { Text("House / Flat / Street / Landmark") },
                        placeholder = { Text("e.g. Near Ramleela Maidan, House 12") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_picker_street_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedLocality,
                            onValueChange = { selectedLocality = it },
                            label = { Text("Locality") },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("location_picker_locality_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = pinCodeInput,
                            onValueChange = { pinCodeInput = it },
                            label = { Text("PIN") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("location_picker_pin_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Action Buttons: Confirm vs Cancel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = TextMuted)
                        }

                        Button(
                            onClick = {
                                val finalAddress = streetAddressInput.ifBlank { "Subhash Chowk, Chhatarpur Road" }
                                val finalLocality = selectedLocality.ifBlank { "Mahoba" }
                                val finalPin = pinCodeInput.ifBlank { "210427" }
                                onLocationConfirmed(finalAddress, finalLocality, finalPin)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("confirm_map_location_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Location", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // DIALOG: PERMISSION RATIONALE
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Allow GPS Location Access", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FlashDrop uses your device GPS to pinpoint your exact delivery address on Google Maps so delivery partners can reach your doorstep in 20-30 minutes.",
                        fontSize = 13.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Your location is only used while choosing your delivery address.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Allow Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // DIALOG: GPS HARDWARE DISABLED ALERT
    if (showGpsDisabledAlert) {
        AlertDialog(
            onDismissRequest = { showGpsDisabledAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn On Device GPS", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    text = "Device Location / GPS is currently turned off. To pinpoint your delivery address using Google Maps, please enable GPS in your device settings.",
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDisabledAlert = false
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            // ignore fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Location Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDisabledAlert = false }) {
                    Text("Select Manually", color = TextMuted)
                }
            }
        )
    }
}

/**
 * Fetches current device GPS location using FusedLocationProviderClient with fallback to LocationManager.
 */
private fun fetchCurrentGpsLocation(
    context: Context,
    onLocationFound: (Location) -> Unit,
    onError: (String) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onError("Location permission not granted")
        return
    }

    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationFound(location)
            } else {
                // Fallback to LocationManager
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val gpsLoc = try { locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
                val netLoc = try { locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
                val bestLoc = gpsLoc ?: netLoc
                if (bestLoc != null) {
                    onLocationFound(bestLoc)
                } else {
                    onError("Could not acquire GPS fix. Please ensure GPS is enabled or select location on map.")
                }
            }
        }.addOnFailureListener {
            onError("GPS detection error: ${it.message}")
        }
    } catch (e: SecurityException) {
        onError("Location permission missing: ${e.message}")
    } catch (e: Exception) {
        onError("Error accessing GPS: ${e.message}")
    }
}

/**
 * Reverse-geocodes coordinates into a readable address line and Mahoba locality.
 */
private fun reverseGeocodeCoordinates(
    context: Context,
    latLng: LatLng,
    onResult: (locality: String, street: String) -> Unit
) {
    // 1. Find closest known Mahoba locality based on distance
    var closestLocality = "Alha Chowk"
    var minDistance = Double.MAX_VALUE

    for (area in MAHOBA_LOCALITIES) {
        val dLat = area.latLng.latitude - latLng.latitude
        val dLng = area.latLng.longitude - latLng.longitude
        val dist = dLat * dLat + dLng * dLng
        if (dist < minDistance) {
            minDistance = dist
            closestLocality = area.name
        }
    }

    // 2. Attempt Geocoder lookup on IO dispatcher
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = addresses?.firstOrNull()
                withContext(Dispatchers.Main) {
                    if (address != null) {
                        val subLocality = address.subLocality ?: address.locality ?: closestLocality
                        val thoroughfare = address.thoroughfare ?: address.featureName ?: "Near $closestLocality"
                        onResult(subLocality, thoroughfare)
                    } else {
                        onResult(closestLocality, "Near $closestLocality, Main Road")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(closestLocality, "Near $closestLocality, Main Road")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(closestLocality, "Near $closestLocality, Main Road")
            }
        }
    }
}
