package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
import android.net.Uri
import com.example.util.rememberCameraManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Shop
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.components.GoogleMapLocationPickerDialog
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShopProfileScreen(
    repository: FlashDropRepository,
    shopId: String? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by repository.currentProfile.collectAsState()
    val shops by repository.shops.collectAsState()

    val targetShopId = shopId?.takeIf { it.isNotBlank() } ?: profile.shopId.ifBlank { "sharma_general" }
    val currentShop = remember(shops, targetShopId) {
        shops.find { it.id == targetShopId } ?: shops.firstOrNull() ?: Shop(id = targetShopId, name = "My Shop")
    }

    var shopName by remember(currentShop.name) { mutableStateOf(currentShop.name) }
    var phone by remember(currentShop.phone) { mutableStateOf(currentShop.phone) }
    var address by remember(currentShop.address) { mutableStateOf(currentShop.address) }
    var openingHours by remember(currentShop.openingHours) { mutableStateOf(currentShop.openingHours) }
    var description by remember(currentShop.description) { mutableStateOf(currentShop.description) }
    var selectedCategory by remember(currentShop.category) { mutableStateOf(currentShop.category) }
    var newGstNumber by remember(currentShop.pendingGstNumber) { mutableStateOf(currentShop.pendingGstNumber) }

    // Media states
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBannerUri by remember { mutableStateOf<Uri?>(null) }
    var selectedShopkeeperPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget by remember { mutableStateOf<String>("logo") } // "logo", "banner", or "shopkeeper_photo"

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var hoursError by remember { mutableStateOf<String?>(null) }
    var showGstNoticeDialog by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }

    val categories = listOf(
        "Grocery", "Dairy & Bakery", "Fruits & Vegetables", "Snacks",
        "Beverages", "Personal Care", "Household", "Sweets & Mithai", "Printing Press"
    )

    // Launchers
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) selectedLogoUri = uri
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) selectedBannerUri = uri
    }

    val cameraManager = rememberCameraManager(
        onImageCaptured = { uri: Uri ->
            when (cameraTarget) {
                "banner" -> selectedBannerUri = uri
                "shopkeeper_photo" -> {
                    selectedShopkeeperPhotoUri = uri
                    coroutineScope.launch {
                        val res = repository.updateShopkeeperRealTimePhoto(
                            shopId = currentShop.id,
                            newPhotoUrl = uri.toString()
                        )
                        res.onSuccess {
                            snackbarHostState.showSnackbar("Shopkeeper real-time photo updated with Admin permission!")
                        }.onFailure { err ->
                            snackbarHostState.showSnackbar(err.message ?: "Permission from Admin is required.")
                        }
                    }
                }
                else -> selectedLogoUri = uri
            }
        },
        onError = { err: String ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(err)
            }
        }
    )

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
                                text = "Edit Shop Profile",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "${currentShop.name} (${currentShop.id})",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("edit_shop_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("cancel_shop_top_button")) {
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Protected Shop Identity Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Registered Shop ID: ${currentShop.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Status: ${currentShop.status.uppercase()} • Account & ID cannot be changed",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Shop Logo & Banner Images Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Shop Photos & Branding",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Banner Image Preview & Pickers
                        Text("Store Banner Image", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedBannerUri != null) {
                                AsyncImage(
                                    model = selectedBannerUri,
                                    contentDescription = "New Banner Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (currentShop.bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentShop.bannerUrl,
                                    contentDescription = "Current Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("No storefront banner added", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    bannerPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("pick_shop_banner_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Banner", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    cameraTarget = "banner"
                                    cameraManager.launchCamera()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                enabled = !cameraManager.isLaunching,
                                modifier = Modifier.testTag("camera_shop_banner_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Take Banner Photo", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Shop Logo / Profile Photo Preview & Pickers
                        Text("Shop Logo / Avatar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(OrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedLogoUri != null) {
                                    AsyncImage(
                                        model = selectedLogoUri,
                                        contentDescription = "New Logo Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else if (currentShop.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentShop.imageUrl,
                                        contentDescription = "Current Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(36.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        logoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("pick_shop_logo_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Logo", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        cameraTarget = "logo"
                                        cameraManager.launchCamera()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                                    enabled = !cameraManager.isLaunching,
                                    modifier = Modifier.testTag("camera_shop_logo_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Capture Logo", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Shopkeeper Photo (Person Operating the Shop) & Admin Permission Card
            item {
                val canUpdateShopkeeperPhoto = currentShop.shopkeeperPhotoChangeApprovedByAdmin || repository.isMainAdmin()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_shop_shopkeeper_photo_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Shopkeeper Photo (Person Operating the Shop)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Operator: ${currentShop.ownerName.ifBlank { "Verified Shopkeeper" }}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mandatory Policy Note
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFFBEB))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Note: If the shopkeeper needs to be changed in the future, permission must first be obtained from the Admin; only then can the shopkeeper's photo be updated.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(OrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                val displayPhoto = selectedShopkeeperPhotoUri?.toString()
                                    ?: currentShop.shopkeeperPhotoUrl.takeIf { it.isNotBlank() }
                                if (displayPhoto != null) {
                                    AsyncImage(
                                        model = displayPhoto,
                                        contentDescription = "Shopkeeper Real-Time Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = OrangeSecondary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (canUpdateShopkeeperPhoto) {
                                    Text(
                                        text = "Admin Permission Granted — You can now capture a new real-time shopkeeper photo.",
                                        fontSize = 11.sp,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Button(
                                        onClick = {
                                            cameraTarget = "shopkeeper_photo"
                                            cameraManager.launchCamera()
                                        },
                                        enabled = !cameraManager.isLaunching,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("camera_shopkeeper_operator_button")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Update Real-Time Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (currentShop.shopkeeperPhotoChangeRequested)
                                                "Permission Request Sent to Admin"
                                            else
                                                "Photo Locked (Admin Permission Required)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val res = repository.requestShopkeeperPhotoChangePermission(currentShop.id)
                                            coroutineScope.launch {
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("Request sent to Main Admin for permission to update shopkeeper photo.")
                                                } else {
                                                    snackbarHostState.showSnackbar("Could not send request.")
                                                }
                                            }
                                        },
                                        enabled = !currentShop.shopkeeperPhotoChangeRequested,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, OrangeSecondary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("request_shopkeeper_photo_permission_button")
                                    ) {
                                        Text(
                                            text = if (currentShop.shopkeeperPhotoChangeRequested)
                                                "Awaiting Admin Permission..."
                                            else
                                                "Request Admin Permission",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangeSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shop Details Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Shop Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = {
                                shopName = it
                                nameError = if (it.trim().isBlank()) "Shop name is required" else null
                            },
                            label = { Text("Shop Name *") },
                            isError = nameError != null,
                            supportingText = { if (nameError != null) Text(nameError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Registered Shop Category (Fixed / Inherited from Registration)
                        OutlinedTextField(
                            value = currentShop.category,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Registered Shop Category") },
                            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                phoneError = if (it.trim().isBlank()) "Contact phone is required" else null
                            },
                            label = { Text("Contact Phone *") },
                            placeholder = { Text("+91 98765 43210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null,
                            supportingText = { if (phoneError != null) Text(phoneError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shop_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showMapLocationPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_shop_open_google_map_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                            border = BorderStroke(1.dp, GreenPrimary)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinpoint on Google Map (Live GPS)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GreenPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                addressError = if (it.trim().isBlank()) "Shop address in Mahoba is required" else null
                            },
                            label = { Text("Shop Address in Mahoba *") },
                            placeholder = { Text("Subhash Chowk, Chhatarpur Road, Mahoba") },
                            isError = addressError != null,
                            supportingText = { if (addressError != null) Text(addressError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shop_address_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openingHours,
                            onValueChange = {
                                openingHours = it
                                hoursError = if (it.trim().isBlank()) "Opening hours are required" else null
                            },
                            label = { Text("Opening & Closing Hours *") },
                            placeholder = { Text("08:00 AM - 10:00 PM") },
                            isError = hoursError != null,
                            supportingText = { if (hoursError != null) Text(hoursError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shop_hours_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Shop Description / Specialities") },
                            placeholder = { Text("Daily fresh groceries, fast home delivery across Mahoba") },
                            modifier = Modifier.fillMaxWidth().testTag("shop_description_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // GST Number & Admin Verification Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GST Number & Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Current Verified GST: ${currentShop.gstNumber.ifBlank { "09AAAAA0000A1Z5" }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimary
                        )

                        if (currentShop.pendingGstNumber.isNotBlank() && currentShop.gstApprovalStatus == "pending_approval") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrangeLight)
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pending Admin Approval: ${currentShop.pendingGstNumber}",
                                        fontSize = 12.sp,
                                        color = OrangeSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newGstNumber,
                            onValueChange = {
                                newGstNumber = it
                                if (it.trim().isNotBlank() && it.trim() != currentShop.gstNumber) {
                                    showGstNoticeDialog = true
                                }
                            },
                            label = { Text("Update GST Number (Optional)") },
                            placeholder = { Text("e.g. 09ABCDE1234F1Z5") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shop_gst_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: Any change to GST number must be verified by Main Admin before being finalized.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (shopName.trim().isBlank()) {
                                nameError = "Shop name cannot be blank"
                                return@Button
                            }
                            if (phone.trim().isBlank()) {
                                phoneError = "Contact phone cannot be blank"
                                return@Button
                            }
                            if (address.trim().isBlank()) {
                                addressError = "Shop address cannot be blank"
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                var finalLogoUrl = currentShop.imageUrl
                                var finalBannerUrl = currentShop.bannerUrl

                                if (selectedLogoUri != null) {
                                    isUploadingMedia = true
                                    val persistent = ImageUtils.persistImageToAppStorage(
                                        context = context,
                                        sourceUri = selectedLogoUri!!,
                                        folderName = "shop_logos",
                                        fileNamePrefix = "logo_${currentShop.id}"
                                    )
                                    finalLogoUrl = persistent?.toString() ?: selectedLogoUri.toString()
                                    val uploadResult = repository.uploadShopLogo(currentShop.id, persistent ?: selectedLogoUri!!)
                                    uploadResult.onSuccess { finalLogoUrl = it }
                                    isUploadingMedia = false
                                }

                                if (selectedBannerUri != null) {
                                    isUploadingMedia = true
                                    val persistent = ImageUtils.persistImageToAppStorage(
                                        context = context,
                                        sourceUri = selectedBannerUri!!,
                                        folderName = "shop_banners",
                                        fileNamePrefix = "banner_${currentShop.id}"
                                    )
                                    finalBannerUrl = persistent?.toString() ?: selectedBannerUri.toString()
                                    val bannerResult = repository.uploadShopBanner(currentShop.id, persistent ?: selectedBannerUri!!)
                                    bannerResult.onSuccess { finalBannerUrl = it }
                                    isUploadingMedia = false
                                }

                                val result = repository.updateShopProfile(
                                    shopId = currentShop.id,
                                    name = shopName,
                                    phone = phone,
                                    address = address,
                                    description = description,
                                    category = selectedCategory,
                                    openingHours = openingHours,
                                    imageUrl = finalLogoUrl,
                                    bannerUrl = finalBannerUrl,
                                    newGstNumber = newGstNumber.takeIf { it.isNotBlank() }
                                )

                                isSaving = false
                                result.onSuccess {
                                    snackbarHostState.showSnackbar("Shop profile updated successfully!")
                                    onBack()
                                }.onFailure { err ->
                                    snackbarHostState.showSnackbar(err.message ?: "Failed to update shop.")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_shop_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isUploadingMedia) "Uploading Media..." else "Saving Shop...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Shop Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_shop_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                    ) {
                        Text("Cancel", color = TextDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

    if (showGstNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showGstNoticeDialog = false },
            title = { Text("GST Verification Requirement", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Updating your GST number requires verification and approval from the FlashDrop Main Admin. Your shop will remain operational with your currently verified GST number in the interim.",
                    fontSize = 13.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGstNoticeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    if (showMapLocationPicker) {
        GoogleMapLocationPickerDialog(
            initialAddress = address,
            initialLocality = "Alha Chowk",
            initialPinCode = "210427",
            onDismiss = { showMapLocationPicker = false },
            onLocationConfirmed = { newAddress, newLocality, newPin ->
                address = if (newAddress.isNotBlank()) {
                    "$newAddress, $newLocality, Mahoba - $newPin"
                } else {
                    "$newLocality, Mahoba - $newPin"
                }
                addressError = null
                showMapLocationPicker = false
            }
        )
    }
}
