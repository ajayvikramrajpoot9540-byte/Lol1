package com.example.ui.screens

import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropLogo
import com.example.ui.components.GoogleMapLocationPickerDialog
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopRegistrationScreen(
    repository: FlashDropRepository,
    onRegistrationSuccess: (com.example.data.model.Shop) -> Unit,
    onSignInAsShopkeeper: () -> Unit,
    onOpenInstructions: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Grocery") }
    var address by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    val registrationConfig by repository.shopRegistrationConfig.collectAsState()
    val isThreeStepMode = registrationConfig.threeStepEnabled

    var currentStep by remember { mutableIntStateOf(1) }
    var currentRequestId by remember { mutableStateOf<String?>(null) }
    var draftSavedNotice by remember { mutableStateOf<String?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var registeredShopResult by remember { mutableStateOf<com.example.data.model.Shop?>(null) }

    val isPrintingPressCategory = category.equals("Printing Press", ignoreCase = true)

    // If 3-step mode is active and user is on Step 2
    if (isThreeStepMode && currentStep == 2 && currentRequestId != null) {
        VerifyDocumentsScreen(
            repository = repository,
            requestId = currentRequestId!!,
            step1GstNumber = gstNumber,
            isGstOptional = isPrintingPressCategory,
            onDocumentsSubmitted = { _ ->
                currentStep = 3
            },
            onBackToStep1 = {
                currentStep = 1
            },
            onOpenInstructions = onOpenInstructions
        )
        return
    }

    // If 3-step mode is active and user is on Step 3
    if (isThreeStepMode && currentStep == 3 && currentRequestId != null) {
        ShopIdGenerationScreen(
            repository = repository,
            applicationId = currentRequestId!!,
            onSignInAsShopkeeper = onSignInAsShopkeeper,
            onBackToHome = onBack
        )
        return
    }

    val categories = listOf(
        "Grocery",
        "Printing Press",
        "Fashion",
        "Electronics",
        "Food & Restaurant",
        "Pharmacy",
        "Books & Stationery",
        "Home & Living",
        "Beauty & Care"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isThreeStepMode) "Register Your Shop (Step 1 of 3)" else "Register Your Shop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Join Mahoba's Hyperlocal Marketplace",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlashDropLogo(
                useTransparentBackground = true,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (isThreeStepMode) {
                RegistrationProgressHeader(currentStep = 1)
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (onOpenInstructions != null) {
                OutlinedButton(
                    onClick = onOpenInstructions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .testTag("step1_shopkeeper_instructions_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Read Shopkeeper Instructions & Verification Rules",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (draftSavedNotice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenLight.copy(alpha = 0.8f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = draftSavedNotice ?: "",
                        color = GreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("shop_registration_form_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Shop Registration Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Fill in your store details. Your application will be reviewed by FlashDrop Admins.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    // Error Message Banner
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("shop_registration_error_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Shop Name
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = {
                            shopName = it
                            errorMessage = null
                        },
                        label = { Text("Shop / Store Name *") },
                        placeholder = { Text("e.g. Bundelkhand Sweets & Bakery") },
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Owner Full Name
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = {
                            ownerName = it
                            errorMessage = null
                        },
                        label = { Text("Owner Full Name *") },
                        placeholder = { Text("e.g. Rakesh Kumar Gupta") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OrangeSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact Number
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = {
                            contactNumber = it
                            errorMessage = null
                        },
                        label = { Text("Mobile Contact Number *") },
                        placeholder = { Text("10-digit number e.g. 9876543210") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = OrangeSecondary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_phone_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Shop Category *") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = OrangeSecondary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select category")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryDropdownExpanded = true }
                                .testTag("category_selector"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeSecondary,
                                focusedLabelColor = OrangeSecondary
                            )
                        )

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live GPS / Google Map Pinpoint for Shop Address
                    OutlinedButton(
                        onClick = { showMapLocationPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_reg_open_google_map_button"),
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shop Address in Mahoba
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            errorMessage = null
                        },
                        label = { Text("Physical Shop Address *") },
                        placeholder = { Text("e.g. Near Alha Chowk, Chhatarpur Road, Mahoba") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeSecondary)
                        },
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // GST Number / GSTIN (Optional for Printing Press, Mandatory for other categories)
                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { input ->
                            if (input.length <= 15) {
                                gstNumber = input.uppercase().filter { it.isLetterOrDigit() }
                                errorMessage = null
                            }
                        },
                        label = {
                            Text(
                                if (isPrintingPressCategory) "GST Number (Optional for Printing Press)"
                                else "GST Number *"
                            )
                        },
                        placeholder = {
                            Text(
                                if (isPrintingPressCategory) "Optional 15-character GSTIN (leave blank if none)"
                                else "15-character GSTIN (e.g. 09AABCU9603R1ZV)"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = OrangeSecondary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_gstin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        ),
                        supportingText = {
                            Text(
                                text = if (isPrintingPressCategory)
                                    "Optional for Printing Press shops — leave blank if you don't have a GST number"
                                else
                                    "Mandatory for seller verification & tax compliance",
                                fontSize = 11.sp,
                                color = if (isPrintingPressCategory) GreenPrimary else TextMuted
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Create Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Create Password *") },
                        placeholder = { Text("Min. 6 characters") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = OrangeSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirm Password *") },
                        placeholder = { Text("Re-enter password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = OrangeSecondary
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_confirm_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        ),
                        supportingText = {
                            Text("Used to log in to your Shopkeeper Hub once approved", fontSize = 11.sp, color = TextMuted)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Business Description / Details
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Shop Description / Key Items (Optional)") },
                        placeholder = { Text("e.g. Fresh daily grocery items, atta, spices, and pulses") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null, tint = OrangeSecondary)
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_description_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeSecondary,
                            focusedLabelColor = OrangeSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isThreeStepMode) {
                        // 3-STEP MODE BUTTONS: "Continue to Step 2: Documents" and "Save Draft"
                        Button(
                            onClick = {
                                if (!isPrintingPressCategory && gstNumber.isBlank()) {
                                    errorMessage = "GST Number is required for shopkeeper registration."
                                    return@Button
                                }
                                val gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$".toRegex()
                                if (gstNumber.trim().isNotEmpty() && !gstNumber.trim().matches(gstinPattern)) {
                                    errorMessage = "Please enter a valid 15-character Indian GSTIN (e.g. 09AABCU9603R1ZV)."
                                    return@Button
                                }
                                if (shopName.isBlank()) {
                                    errorMessage = "Please enter the shop name."
                                    return@Button
                                }
                                if (ownerName.isBlank()) {
                                    errorMessage = "Please enter the owner's full name."
                                    return@Button
                                }
                                val cleanPhone = contactNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
                                val phoneRegex = Regex("^[6-9]\\d{9}$")
                                if (!phoneRegex.matches(cleanPhone)) {
                                    errorMessage = "Please enter a valid 10-digit Indian mobile number."
                                    return@Button
                                }
                                if (address.isBlank()) {
                                    errorMessage = "Please enter the shop's physical address in Mahoba."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Please enter a password with at least 6 characters for shop security."
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match. Please re-enter your password."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = repository.submitShopRegistrationStep1(
                                        name = shopName,
                                        ownerName = ownerName,
                                        phone = cleanPhone,
                                        category = category,
                                        address = address,
                                        description = description,
                                        gstNumber = gstNumber.trim(),
                                        password = password.trim(),
                                        confirmPassword = confirmPassword.trim(),
                                        existingRequestId = currentRequestId
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        currentRequestId = result.getOrThrow().id
                                        currentStep = 2
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                            ?: "Failed to process Step 1. Please try again."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("continue_to_step2_button"),
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
                                Text(
                                    text = "Continue to Step 2: Documents",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (shopName.isBlank()) {
                                    errorMessage = "Please enter the shop name to save a draft."
                                    return@OutlinedButton
                                }
                                val cleanPhone = contactNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
                                coroutineScope.launch {
                                    val result = repository.submitShopRegistrationStep1(
                                        name = shopName,
                                        ownerName = ownerName.ifBlank { "Applicant" },
                                        phone = cleanPhone.ifBlank { "9876543210" },
                                        category = category,
                                        address = address.ifBlank { "Mahoba" },
                                        description = description,
                                        gstNumber = if (isPrintingPressCategory) gstNumber.trim() else gstNumber.trim().ifBlank { "09AABCU9603R1ZV" },
                                        password = password.trim().ifBlank { "DraftPass123" },
                                        confirmPassword = confirmPassword.trim().ifBlank { "DraftPass123" },
                                        existingRequestId = currentRequestId
                                    )
                                    if (result.isSuccess) {
                                        currentRequestId = result.getOrThrow().id
                                        draftSavedNotice = "Draft saved successfully. You can return anytime to continue."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_draft_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save Draft Application",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                        }
                    } else {
                        // STANDARD NORMAL REGISTRATION BUTTON
                        Button(
                            onClick = {
                                if (!isPrintingPressCategory && gstNumber.isBlank()) {
                                    errorMessage = "GST Number is required for shopkeeper registration."
                                    return@Button
                                }
                                val gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$".toRegex()
                                if (gstNumber.trim().isNotEmpty() && !gstNumber.trim().matches(gstinPattern)) {
                                    errorMessage = "Please enter a valid 15-character Indian GSTIN (e.g. 09AABCU9603R1ZV)."
                                    return@Button
                                }
                                if (shopName.isBlank()) {
                                    errorMessage = "Please enter the shop name."
                                    return@Button
                                }
                                if (ownerName.isBlank()) {
                                    errorMessage = "Please enter the owner's full name."
                                    return@Button
                                }
                                val cleanPhone = contactNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
                                val phoneRegex = Regex("^[6-9]\\d{9}$")
                                if (!phoneRegex.matches(cleanPhone)) {
                                    errorMessage = "Please enter a valid 10-digit Indian mobile number."
                                    return@Button
                                }
                                if (address.isBlank()) {
                                    errorMessage = "Please enter the shop's physical address in Mahoba."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Please enter a password with at least 6 characters for shop security."
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match. Please re-enter your password."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
                                coroutineScope.launch {
                                    delay(300)
                                    val result = repository.submitShopRegistration(
                                        name = shopName,
                                        ownerName = ownerName,
                                        phone = cleanPhone,
                                        category = category,
                                        address = address,
                                        description = description,
                                        gstNumber = gstNumber.trim(),
                                        password = password.trim()
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        registeredShopResult = result.getOrNull()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                            ?: "Failed to submit application. Please try again."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_shop_registration_button"),
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
                                Icon(
                                    imageVector = Icons.Default.AppRegistration,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Register Shop Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Already registered? Sign In link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onSignInAsShopkeeper,
                            modifier = Modifier.testTag("shopkeeper_signin_link")
                        ) {
                            Text(
                                text = "Already registered? Sign In",
                                color = OrangeSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant Review & Security Safeguard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Review & Verification Process",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• All shop registrations are submitted with 'Pending' approval status.\n" +
                               "• FlashDrop Main Admins verify shop details before approving.\n" +
                               "• Only verified and approved stores are displayed to Mahoba customers.\n" +
                               "• Once approved, you will be issued your shopkeeper credentials to manage products and orders.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Success Confirmation Dialog
    val createdShop = registeredShopResult
    if (createdShop != null) {
        AlertDialog(
            onDismissRequest = {
                registeredShopResult = null
                onRegistrationSuccess(createdShop)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OrangeSecondary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Application Submitted!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Dhanyawaad! Your shop '$shopName' has been registered with pending status.\n\n" +
                           "Your shop registration is awaiting Admin approval. FlashDrop Main Admins will verify your details before activating your shopkeeper portal.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        registeredShopResult = null
                        onRegistrationSuccess(createdShop)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("registration_success_done_button")
                ) {
                    Text("View Approval Status", fontWeight = FontWeight.Bold)
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
                showMapLocationPicker = false
            }
        )
    }
}

