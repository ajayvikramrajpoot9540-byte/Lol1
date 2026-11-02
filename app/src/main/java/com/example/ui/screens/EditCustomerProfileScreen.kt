package com.example.ui.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.util.rememberCameraManager
import com.example.data.model.DeliveryAddress
import com.example.data.repository.ApnaBazRepository
import com.example.ui.components.AddNewAddressDialog
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import androidx.compose.material.icons.filled.MyLocation
import android.util.Log
import com.example.ui.components.CircularProfileAvatar
import com.example.ui.components.GoogleMapLocationPickerDialog
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerProfileScreen(
    repository: ApnaBazRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile by repository.currentProfile.collectAsState()

    var name by remember(profile.name) { mutableStateOf(if (profile.name == "Guest") "" else profile.name) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var originalEmail by remember(profile.email) { mutableStateOf(profile.email) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoUrl by remember(profile.photoUrl) { mutableStateOf(profile.photoUrl) }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var showEmailNoticeDialog by remember { mutableStateOf(false) }

    // Address Dialog State
    var showAddressDialog by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persistent = ImageUtils.persistImageToAppStorage(context, uri, "user_photos", "profile_picker")
            selectedImageUri = persistent ?: uri
        }
    }

    // Camera manager handling permissions, FileProvider grants, and captures
    val cameraManager = rememberCameraManager(
        onImageCaptured = { uri: Uri ->
            val persistent = ImageUtils.persistImageToAppStorage(context, uri, "user_photos", "profile_camera")
            selectedImageUri = persistent ?: uri
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
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("edit_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("cancel_customer_profile_top_button")
                    ) {
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Photo Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Profile Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        CircularProfileAvatar(
                            photoUrl = selectedImageUri?.toString() ?: currentPhotoUrl,
                            displayName = name.ifBlank { "Customer" },
                            role = "customer",
                            size = 100.dp,
                            tintColor = GreenPrimary,
                            backgroundColor = GreenLight,
                            testTag = "profile_photo_preview"
                        )

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "New photo ready to save",
                                fontSize = 12.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GreenPrimary),
                                modifier = Modifier.testTag("pick_gallery_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GreenPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", color = GreenPrimary, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { cameraManager.launchCamera() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                enabled = !cameraManager.isLaunching,
                                modifier = Modifier.testTag("take_camera_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Personal Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Personal Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = if (it.trim().isBlank()) "Name cannot be empty" else null
                            },
                            label = { Text("Full Name *") },
                            placeholder = { Text("Enter your full name") },
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) {
                                    Text(nameError ?: "", color = Color.Red, fontSize = 11.sp)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                phoneError = if (it.trim().filter { c -> c.isDigit() }.length < 10) {
                                    "Enter a valid 10-digit mobile number"
                                } else null
                            },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("+91 98765 12345") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null,
                            supportingText = {
                                if (phoneError != null) {
                                    Text(phoneError ?: "", color = Color.Red, fontSize = 11.sp)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (originalEmail.isNotBlank() && it.trim() != originalEmail.trim()) {
                                    showEmailNoticeDialog = true
                                }
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("customer@flashdrop.in") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_email_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Protected User ID Info Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Account UID: ${profile.id.ifBlank { "cust_user_default" }} (Protected & Immutable)",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Delivery Addresses Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Saved Delivery Addresses",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Mahoba, Uttar Pradesh",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = {
                                    editingAddress = null
                                    showAddressDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenLight),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_address_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add New", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (profile.savedAddresses.isEmpty()) {
                            // Default Fallback Address Display
                            AddressItemCard(
                                address = DeliveryAddress(
                                    houseNumber = "",
                                    street = "Subhash Chowk, Chhatarpur Road",
                                    area = profile.locality.ifBlank { "Alha Chowk" },
                                    city = "Mahoba",
                                    state = "Uttar Pradesh",
                                    pinCode = "210427",
                                    isDefault = true
                                ),
                                onSetDefault = {},
                                onEdit = {
                                    editingAddress = DeliveryAddress(
                                        street = "Subhash Chowk, Chhatarpur Road",
                                        area = profile.locality.ifBlank { "Alha Chowk" },
                                        city = "Mahoba",
                                        state = "Uttar Pradesh",
                                        pinCode = "210427",
                                        isDefault = true
                                    )
                                    showAddressDialog = true
                                },
                                onDelete = null
                            )
                        } else {
                            profile.savedAddresses.forEach { addr ->
                                AddressItemCard(
                                    address = addr,
                                    onSetDefault = {
                                        repository.setDefaultDeliveryAddress(addr.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Default delivery address set.")
                                        }
                                    },
                                    onEdit = {
                                        editingAddress = addr
                                        showAddressDialog = true
                                    },
                                    onDelete = if (profile.savedAddresses.size > 1) {
                                        {
                                            repository.deleteDeliveryAddress(addr.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Address deleted.")
                                            }
                                        }
                                    } else null
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank() || trimmedName.equals("Guest", ignoreCase = true)) {
                                nameError = "Please enter your valid name"
                                return@Button
                            }
                            val digits = phone.filter { it.isDigit() }
                            if (digits.length < 10) {
                                phoneError = "Please enter a valid 10-digit mobile number"
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                var finalPhotoUrl = currentPhotoUrl
                                if (selectedImageUri != null) {
                                    isUploadingPhoto = true
                                    // 1. Ensure persisted to app internal storage
                                    val persistent = ImageUtils.persistImageToAppStorage(
                                        context = context,
                                        sourceUri = selectedImageUri!!,
                                        folderName = "user_photos",
                                        fileNamePrefix = "profile_${profile.id.ifBlank { "active" }}"
                                    )
                                    finalPhotoUrl = persistent?.toString() ?: selectedImageUri.toString()

                                    // 2. Upload to Firebase Storage if available
                                    val uploadResult = repository.uploadUserProfilePhoto(
                                        userId = profile.id.ifBlank { "cust_user_${System.currentTimeMillis()}" },
                                        imageUri = persistent ?: selectedImageUri!!
                                    )
                                    uploadResult.onSuccess { url ->
                                        finalPhotoUrl = url
                                    }.onFailure { err ->
                                        Log.d("EditCustomerProfile", "Cloud photo sync deferred: ${err.message}")
                                    }
                                    isUploadingPhoto = false
                                }

                                val result = repository.updateCustomerProfile(
                                    name = trimmedName,
                                    phone = phone,
                                    email = email,
                                    photoUrl = finalPhotoUrl
                                )

                                isSaving = false
                                result.onSuccess {
                                    snackbarHostState.showSnackbar("Profile updated successfully!")
                                    onBack()
                                }.onFailure { err ->
                                    snackbarHostState.showSnackbar(err.message ?: "Failed to update profile.")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_customer_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isUploadingPhoto) "Uploading Photo..." else "Saving Changes...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_customer_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                    ) {
                        Text("Cancel", color = TextDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Email change verification warning dialog
    if (showEmailNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showEmailNoticeDialog = false },
            title = { Text("Email Verification Note", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "If you change your registered email address, Firebase Authentication may require re-authentication or verification before applying the change permanently to your Google / Firebase Auth account.",
                    fontSize = 13.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = { showEmailNoticeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("I Understand")
                }
            }
        )
    }

    // Dialog for Adding or Editing Delivery Address
    if (showAddressDialog) {
        AddNewAddressDialog(
            initialAddress = editingAddress,
            initialName = profile.name.takeIf { !it.equals("Guest", ignoreCase = true) } ?: "",
            initialPhone = profile.phone,
            onDismiss = { showAddressDialog = false },
            onSave = { newOrUpdatedAddress ->
                repository.saveDeliveryAddress(newOrUpdatedAddress)
                showAddressDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Delivery address saved!")
                }
            },
            onOpenMap = {
                showAddressDialog = false
                showMapLocationPicker = true
            }
        )
    }

    if (showMapLocationPicker) {
        GoogleMapLocationPickerDialog(
            initialAddress = editingAddress?.formatDisplay() ?: profile.address,
            initialLocality = editingAddress?.area?.ifBlank { profile.locality } ?: "Alha Chowk",
            initialPinCode = editingAddress?.pinCode?.ifBlank { profile.pinCode } ?: "210427",
            onDismiss = { showMapLocationPicker = false },
            onLocationConfirmed = { newAddress, newLocality, newPin ->
                val updatedAddr = (editingAddress ?: DeliveryAddress()).copy(
                    street = newAddress,
                    area = newLocality,
                    city = "Mahoba",
                    state = "Uttar Pradesh",
                    pinCode = newPin,
                    isDefault = editingAddress?.isDefault ?: (profile.savedAddresses.isEmpty())
                )
                repository.saveDeliveryAddress(updatedAddr)
                showMapLocationPicker = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Address updated from Map GPS!")
                }
            }
        )
    }
}

@Composable
fun AddressItemCard(
    address: DeliveryAddress,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault) GreenLight.copy(alpha = 0.5f) else Color(0xFFF9FAFB)
        ),
        border = BorderStroke(1.dp, if (address.isDefault) GreenPrimary.copy(alpha = 0.4f) else Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (address.isDefault) GreenPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = address.area.ifBlank { "Delivery Address" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }

                if (address.isDefault) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GreenPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Default", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Set as Default", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = address.formatDisplay(),
                fontSize = 12.sp,
                color = TextDark,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = TextDark, fontSize = 12.sp)
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = onDelete,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryAddressDialog(
    initialAddress: DeliveryAddress?,
    onDismiss: () -> Unit,
    onSave: (DeliveryAddress) -> Unit,
    onOpenMap: () -> Unit = {}
) {
    var houseNumber by remember { mutableStateOf(initialAddress?.houseNumber ?: "") }
    var street by remember { mutableStateOf(initialAddress?.street ?: "") }
    var area by remember { mutableStateOf(initialAddress?.area ?: "Subhash Chowk") }
    var landmark by remember { mutableStateOf(initialAddress?.landmark ?: "") }
    var city by remember { mutableStateOf(initialAddress?.city ?: "Mahoba") }
    var state by remember { mutableStateOf(initialAddress?.state ?: "Uttar Pradesh") }
    var pinCode by remember { mutableStateOf(initialAddress?.pinCode ?: "210427") }
    var isDefault by remember { mutableStateOf(initialAddress?.isDefault ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val popularAreas = listOf(
        "Subhash Chowk", "Alha Chowk", "Gandhi Nagar", "Bhatipura",
        "Civil Lines", "Chhatarpur Road", "Railway Station Road", "Haveli Darwaza"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAddress == null) "Add Delivery Address" else "Edit Delivery Address",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_open_google_map_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pinpoint on Google Map (Live GPS)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GreenPrimary
                    )
                }

                OutlinedTextField(
                    value = houseNumber,
                    onValueChange = { houseNumber = it },
                    label = { Text("House / Flat / Floor") },
                    placeholder = { Text("e.g. House No. 42 / Flat 3B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street / Road / Lane *") },
                    placeholder = { Text("e.g. Station Road, Main Bazaar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area / Locality *") },
                    placeholder = { Text("e.g. Subhash Chowk, Alha Chowk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark (Optional)") },
                    placeholder = { Text("Near Kali Mandir, SBI Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("PIN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDefault = !isDefault }
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set as default delivery address", fontSize = 12.sp, color = TextDark)
                }

                if (validationError != null) {
                    Text(validationError ?: "", color = Color.Red, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (street.isBlank() && area.isBlank() && houseNumber.isBlank()) {
                        validationError = "Please enter street, area, or house number"
                        return@Button
                    }
                    val newAddress = (initialAddress ?: DeliveryAddress()).copy(
                        houseNumber = houseNumber.trim(),
                        street = street.trim(),
                        area = area.trim(),
                        landmark = landmark.trim(),
                        city = city.trim().ifBlank { "Mahoba" },
                        state = state.trim().ifBlank { "Uttar Pradesh" },
                        pinCode = pinCode.trim().ifBlank { "210427" },
                        isDefault = isDefault
                    )
                    onSave(newAddress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
