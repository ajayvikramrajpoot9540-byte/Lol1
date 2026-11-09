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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.CircularProfileAvatar
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAdminProfileScreen(
    repository: FlashDropRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentProfile by repository.currentProfile.collectAsState()

    // Enforce Main Admin authentication and identity
    val currentLoginId = if (currentProfile.loginId.uppercase() == "CD12" || currentProfile.name.contains("Chanchal", ignoreCase = true)) "CD12" else "PR12"
    val ownerTitle = if (currentLoginId == "CD12") "Owner 2" else "Owner 1"
    val defaultName = if (currentLoginId == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"

    var displayName by remember(currentProfile.name) {
        mutableStateOf(currentProfile.name.ifBlank { defaultName })
    }
    var phone by remember(currentProfile.phone) {
        mutableStateOf(currentProfile.phone.ifBlank { if (currentLoginId == "CD12") "+91 98765 00013" else "+91 98765 00012" })
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var currentPhotoUrl by remember(currentProfile.photoUrl) { mutableStateOf(currentProfile.photoUrl) }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val adminColor = Color(0xFF6D28D9)
    val adminBgLight = Color(0xFFEDE9FE)

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persistent = ImageUtils.persistImageToAppStorage(context, uri, "admin_photos", "admin_${currentLoginId}_picker")
            selectedImageUri = persistent ?: uri
        }
    }

    val cameraManager = rememberCameraManager(
        onImageCaptured = { uri: Uri ->
            val persistent = ImageUtils.persistImageToAppStorage(context, uri, "admin_photos", "admin_${currentLoginId}_camera")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Edit Main Admin Profile",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "$displayName ($currentLoginId) • $ownerTitle",
                                fontSize = 11.sp,
                                color = adminColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("edit_admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("cancel_admin_top_button")) {
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
            // Two-Owner Equal Co-Owner Safeguard Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = adminBgLight)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = adminColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Equal Co-Owner Safeguard Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = adminColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pranjal Rajpoot (PR12) and Chanchal Didi (CD12) hold equal administrative rights. Security rules prevent cross-admin profile tampering.",
                                fontSize = 11.sp,
                                color = TextDark,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Admin Photo Section
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
                            text = "Admin Profile Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        CircularProfileAvatar(
                            photoUrl = selectedImageUri?.toString() ?: currentPhotoUrl,
                            displayName = displayName,
                            role = "MAIN_ADMIN",
                            size = 100.dp,
                            tintColor = adminColor,
                            backgroundColor = adminBgLight,
                            testTag = "admin_photo_preview"
                        )

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("New photo selected", fontSize = 12.sp, color = adminColor, fontWeight = FontWeight.SemiBold)
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
                                border = BorderStroke(1.dp, adminColor),
                                modifier = Modifier.testTag("pick_admin_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = adminColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", color = adminColor, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { cameraManager.launchCamera() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = adminColor),
                                enabled = !cameraManager.isLaunching,
                                modifier = Modifier.testTag("take_admin_photo_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Admin Profile Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Admin Credentials & Info",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                nameError = if (it.trim().isBlank()) "Display name cannot be blank" else null
                            },
                            label = { Text("Display Name *") },
                            placeholder = { Text("Pranjal Rajpoot") },
                            isError = nameError != null,
                            supportingText = { if (nameError != null) Text(nameError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                phoneError = if (it.trim().filter { c -> c.isDigit() }.length < 10) {
                                    "Enter a valid mobile number"
                                } else null
                            },
                            label = { Text("Contact Mobile Number *") },
                            placeholder = { Text("+91 98765 00012") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null,
                            supportingText = { if (phoneError != null) Text(phoneError ?: "", color = Color.Red, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Security Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Role: MAIN_ADMIN • Login ID: $currentLoginId", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Administrative passwords and encryption keys are strictly protected and cannot be viewed or altered in plaintext.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val trimmedName = displayName.trim()
                            if (trimmedName.isBlank()) {
                                nameError = "Display name cannot be blank"
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                var finalPhoto = currentPhotoUrl
                                if (selectedImageUri != null) {
                                    isUploadingPhoto = true
                                    val persistent = ImageUtils.persistImageToAppStorage(
                                        context = context,
                                        sourceUri = selectedImageUri!!,
                                        folderName = "admin_photos",
                                        fileNamePrefix = "admin_$currentLoginId"
                                    )
                                    finalPhoto = persistent?.toString() ?: selectedImageUri.toString()
                                    val uploadResult = repository.uploadAdminProfilePhoto(currentLoginId, persistent ?: selectedImageUri!!)
                                    uploadResult.onSuccess { finalPhoto = it }
                                    isUploadingPhoto = false
                                }

                                val result = repository.updateAdminProfile(
                                    adminId = currentLoginId,
                                    displayName = trimmedName,
                                    phone = phone,
                                    photoUrl = finalPhoto
                                )

                                isSaving = false
                                result.onSuccess {
                                    snackbarHostState.showSnackbar("Admin profile updated successfully!")
                                    onBack()
                                }.onFailure { err ->
                                    snackbarHostState.showSnackbar(err.message ?: "Failed to update admin profile.")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_admin_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adminColor),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isUploadingPhoto) "Uploading Photo..." else "Saving Profile...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Admin Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_admin_profile_button"),
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
}
