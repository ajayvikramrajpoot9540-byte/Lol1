package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShopVerificationRequest
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.util.rememberCameraManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyDocumentsScreen(
    repository: FlashDropRepository,
    requestId: String,
    step1GstNumber: String,
    initialAadhaarMasked: String = "",
    initialPanNumber: String = "",
    onDocumentsSubmitted: (ShopVerificationRequest) -> Unit,
    onBackToStep1: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var aadhaarInput by remember { mutableStateOf(initialAadhaarMasked) }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }

    var panInput by remember { mutableStateOf(initialPanNumber) }
    var panUri by remember { mutableStateOf<Uri?>(null) }

    var gstCertUri by remember { mutableStateOf<Uri?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Target tracking for camera capture
    var activeCameraTarget by remember { mutableStateOf<String>("aadhaar") } // "aadhaar", "pan", "gst"

    val cameraManager = rememberCameraManager(
        onImageCaptured = { uri ->
            when (activeCameraTarget) {
                "aadhaar" -> aadhaarUri = uri
                "pan" -> panUri = uri
                "gst" -> gstCertUri = uri
            }
            errorMessage = null
        },
        onError = { err ->
            errorMessage = err
        }
    )

    // Gallery Launchers
    val aadhaarGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            aadhaarUri = uri
            errorMessage = null
        }
    }

    val panGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            panUri = uri
            errorMessage = null
        }
    }

    val gstGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            gstCertUri = uri
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Verify Your Business Documents",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Step 2 of 3: Merchant Compliance",
                                fontSize = 11.sp,
                                color = OrangeSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToStep1,
                        modifier = Modifier.testTag("verify_docs_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Step 1",
                            tint = TextDark
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // STEP PROGRESS HEADER
            RegistrationProgressHeader(currentStep = 2)

            Spacer(modifier = Modifier.height(16.dp))

            // Information banner explaining lawful merchant verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = OrangeLight.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangeSecondary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = OrangeSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lawful Merchant Verification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Text(
                            text = "To safeguard consumers across Mahoba, FlashDrop requires business document verification. Sensitive document data is restricted to authorized Admin review and never publicly exposed.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error Banner
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("verify_docs_error_banner")
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // DOCUMENT 1: AADHAAR CARD
            DocumentSectionCard(
                title = "1. Aadhaar Card (Masked Merchant Record)",
                subtitle = "Enter Aadhaar number and upload front photo. Only last 4 digits are stored.",
                icon = Icons.Default.Badge,
                selectedUri = aadhaarUri,
                onCameraClick = {
                    activeCameraTarget = "aadhaar"
                    cameraManager.launchCamera()
                },
                onGalleryClick = {
                    aadhaarGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { aadhaarUri = null },
                cameraEnabled = !isSubmitting && !cameraManager.isLaunching,
                tagPrefix = "aadhaar"
            ) {
                OutlinedTextField(
                    value = aadhaarInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 12) {
                            aadhaarInput = if (digits.length == 12) {
                                "XXXX-XXXX-" + digits.takeLast(4)
                            } else {
                                digits
                            }
                        }
                    },
                    label = { Text("Aadhaar Number *") },
                    placeholder = { Text("12 digits (stored as XXXX-XXXX-1234)") },
                    leadingIcon = {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = OrangeSecondary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aadhaar_number_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeSecondary,
                        focusedLabelColor = OrangeSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DOCUMENT 2: PAN CARD
            DocumentSectionCard(
                title = "2. Permanent Account Number (PAN Card)",
                subtitle = "Statutory 10-character Indian PAN of store proprietor or business entity.",
                icon = Icons.Default.CreditCard,
                selectedUri = panUri,
                onCameraClick = {
                    activeCameraTarget = "pan"
                    cameraManager.launchCamera()
                },
                onGalleryClick = {
                    panGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { panUri = null },
                cameraEnabled = !isSubmitting && !cameraManager.isLaunching,
                tagPrefix = "pan"
            ) {
                OutlinedTextField(
                    value = panInput,
                    onValueChange = { input ->
                        if (input.length <= 10) {
                            panInput = input.uppercase()
                        }
                    },
                    label = { Text("PAN Number *") },
                    placeholder = { Text("e.g. ABCDE1234F") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = OrangeSecondary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pan_number_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeSecondary,
                        focusedLabelColor = OrangeSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DOCUMENT 3: GST CERTIFICATE
            DocumentSectionCard(
                title = "3. GST Registration Certificate",
                subtitle = "Upload original GST Registration Certificate matching GSTIN entered in Step 1.",
                icon = Icons.Default.Description,
                selectedUri = gstCertUri,
                onCameraClick = {
                    activeCameraTarget = "gst"
                    cameraManager.launchCamera()
                },
                onGalleryClick = {
                    gstGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { gstCertUri = null },
                cameraEnabled = !isSubmitting && !cameraManager.isLaunching,
                tagPrefix = "gst"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenLight.copy(alpha = 0.4f))
                        .border(1.dp, GreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Step 1 GSTIN: $step1GstNumber",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextDark
                            )
                            Text(
                                text = "GST certificate image must match this GST registration number",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    // Validations
                    val cleanAadhaar = aadhaarInput.replace(" ", "").replace("-", "").trim()
                    if (cleanAadhaar.length < 4) {
                        errorMessage = "Please enter your Aadhaar number (stored as masked record)."
                        return@Button
                    }
                    if (aadhaarUri == null) {
                        errorMessage = "Please capture or select your Aadhaar card photo."
                        return@Button
                    }

                    val cleanPan = panInput.trim().uppercase()
                    val panRegex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
                    if (!panRegex.matches(cleanPan)) {
                        errorMessage = "Please enter a valid 10-character Indian PAN (e.g. ABCDE1234F)."
                        return@Button
                    }
                    if (panUri == null) {
                        errorMessage = "Please capture or select your PAN card photo."
                        return@Button
                    }

                    if (gstCertUri == null) {
                        errorMessage = "Please capture or select your GST Registration Certificate photo."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = repository.submitShopRegistrationStep2(
                            requestId = requestId,
                            aadhaarNumberRaw = aadhaarInput,
                            aadhaarDocUri = aadhaarUri.toString(),
                            panNumber = cleanPan,
                            panDocUri = panUri.toString(),
                            gstCertificateDocUri = gstCertUri.toString()
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            onDocumentsSubmitted(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                                ?: "Failed to submit documents. Please try again."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_step2_documents_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Documents & Proceed to Step 3",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBackToStep1,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step2_back_to_step1_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Back to Step 1: Store Information",
                    color = TextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
}

@Composable
fun RegistrationProgressHeader(currentStep: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicatorItem(
                    stepNumber = 1,
                    stepName = "Store Details",
                    isCompleted = currentStep > 1,
                    isActive = currentStep == 1
                )
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    color = if (currentStep > 1) GreenPrimary else BorderLight,
                    thickness = 2.dp
                )
                StepIndicatorItem(
                    stepNumber = 2,
                    stepName = "Documents",
                    isCompleted = currentStep > 2,
                    isActive = currentStep == 2
                )
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    color = if (currentStep > 2) GreenPrimary else BorderLight,
                    thickness = 2.dp
                )
                StepIndicatorItem(
                    stepNumber = 3,
                    stepName = "Shop ID Code",
                    isCompleted = false,
                    isActive = currentStep == 3
                )
            }
        }
    }
}

@Composable
private fun StepIndicatorItem(
    stepNumber: Int,
    stepName: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> GreenPrimary
                        isActive -> OrangeSecondary
                        else -> Color(0xFFE5E7EB)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = "$stepNumber",
                    color = if (isActive) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stepName,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) OrangeSecondary else if (isCompleted) GreenPrimary else TextMuted
        )
    }
}

@Composable
fun DocumentSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: () -> Unit,
    cameraEnabled: Boolean,
    tagPrefix: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_section_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PeachContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = OrangeDark, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extra inputs (e.g. number fields)
            content()

            Spacer(modifier = Modifier.height(12.dp))

            // Document Image Preview & Capture Controls
            if (selectedUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "$title Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uploaded", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Retake, Replace, Remove
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCameraClick,
                        enabled = cameraEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_retake_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = OrangeSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake", fontSize = 11.sp, color = OrangeSecondary)
                    }

                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_replace_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replace", fontSize = 11.sp, color = GreenPrimary)
                    }

                    OutlinedButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_remove_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 11.sp, color = Color.Red)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCameraClick,
                        enabled = cameraEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_camera_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 12.sp, color = OrangeSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_gallery_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private val PeachContainer = Color(0xFFFFE8D6)
private val OrangeDark = Color(0xFFE87510)
