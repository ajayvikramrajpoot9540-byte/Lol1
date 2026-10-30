package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.repository.AdminShopCreationResult
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddShopScreen(
    repository: FlashDropRepository,
    onShopCreated: ((Shop, ShopkeeperAccount) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentProfile by repository.currentProfile.collectAsState()

    // 9 Form Fields
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

    // Validation error states
    var shopNameError by remember { mutableStateOf<String?>(null) }
    var ownerNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var gstError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var generalErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var creationSuccessResult by remember { mutableStateOf<AdminShopCreationResult?>(null) }

    // Supported categories matching app specification
    val supportedCategories = listOf(
        "Grocery",
        "Printing Press",
        "Fashion",
        "Electronics",
        "Food",
        "Home & Kitchen",
        "Books & Stationery",
        "Pharmacy",
        "Beauty & Care",
        "Other"
    )

    val adminOwnerName = if (currentProfile.loginId == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
    val adminLoginId = if (currentProfile.loginId == "CD12") "CD12" else "PR12"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(OrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Panel → Add Shop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Direct Shopkeeper & Unique Shop ID Provisioning",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_add_shop_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Admin Dashboard",
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Admin Authorization Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GreenLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Authorized Main Admin: $adminOwnerName ($adminLoginId)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Account status will be set to APPROVED automatically with unique Shop ID.",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                    }
                }
            }

            // SUCCESS CONFIRMATION VIEW
            if (creationSuccessResult != null) {
                val res = creationSuccessResult!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_shop_creation_success_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = GreenPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Shopkeeper Account Created!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenPrimary)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Account Status: APPROVED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Credentials Breakdown
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OrangeLight.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeSecondary.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Generated Unique Shop ID",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = res.generatedShopId,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OrangeSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Shop ID", res.generatedShopId)
                                            clipboard.setPrimaryClip(clip)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Copied Shop ID: ${res.generatedShopId}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Shop ID",
                                            tint = OrangeSecondary
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OrangeSecondary.copy(alpha = 0.2f))

                                DetailRow("Shop Name", res.shop.name)
                                DetailRow("Owner Full Name", res.shop.ownerName)
                                DetailRow("Mobile Number", res.shop.phone)
                                DetailRow("GSTIN", res.shop.gstNumber)
                                DetailRow("Category", res.shop.category)
                                DetailRow("Login ID", res.loginId)
                                DetailRow("Shop Address", res.shop.address)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Shopkeeper Sign-In Instructions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangeSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "The merchant can now sign in immediately on the Shopkeeper Sign In page using their GSTIN, Shop ID (${res.generatedShopId}), Mobile Number, and password.",
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Actions
                        Button(
                            onClick = {
                                onShopCreated?.invoke(res.shop, res.account) ?: onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_in_shops_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Done • View in All Shops", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                // Reset form to add another
                                shopName = ""
                                ownerName = ""
                                contactNumber = ""
                                category = "Grocery"
                                address = ""
                                gstNumber = ""
                                password = ""
                                confirmPassword = ""
                                description = ""
                                creationSuccessResult = null
                                generalErrorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_another_shop_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Another Shop", color = OrangeSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // REGISTRATION FORM
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_shop_form_card"),
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
                            text = "Shop Registration Form",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Fill in the complete details to generate a unique Shop ID and provision the shopkeeper's login.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // Error Banner
                        if (generalErrorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = generalErrorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("admin_add_shop_error_banner")
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 1. Shop / Store Name (required)
                        AdminInputField(
                            value = shopName,
                            onValueChange = {
                                shopName = it
                                shopNameError = null
                                generalErrorMessage = null
                            },
                            label = "Shop / Store Name *",
                            placeholder = "e.g. Bundelkhand Digital Printers & Xerox",
                            leadingIcon = Icons.Default.Storefront,
                            isError = shopNameError != null,
                            errorMessage = shopNameError,
                            testTag = "admin_shop_name_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Owner Full Name (required)
                        AdminInputField(
                            value = ownerName,
                            onValueChange = {
                                ownerName = it
                                ownerNameError = null
                                generalErrorMessage = null
                            },
                            label = "Owner Full Name *",
                            placeholder = "e.g. Dharmendra Kumar Sharma",
                            leadingIcon = Icons.Default.Person,
                            isError = ownerNameError != null,
                            errorMessage = ownerNameError,
                            testTag = "admin_owner_name_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Mobile Contact Number (required)
                        AdminInputField(
                            value = contactNumber,
                            onValueChange = {
                                contactNumber = it
                                phoneError = null
                                generalErrorMessage = null
                            },
                            label = "Mobile Contact Number *",
                            placeholder = "10-digit number (e.g. 9876543210)",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            isError = phoneError != null,
                            errorMessage = phoneError,
                            testTag = "admin_contact_phone_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Shop Category (required dropdown)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Shop Category *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = OrangeSecondary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Category",
                                            tint = OrangeSecondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .testTag("admin_category_dropdown"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeSecondary,
                                    focusedLabelColor = OrangeSecondary,
                                    unfocusedBorderColor = BorderLight
                                )
                            )

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                supportedCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            category = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Physical Shop Address (required)
                        AdminInputField(
                            value = address,
                            onValueChange = {
                                address = it
                                addressError = null
                                generalErrorMessage = null
                            },
                            label = "Physical Shop Address *",
                            placeholder = "e.g. Shop No. 12, Alha Chowk, Chhatarpur Road, Mahoba",
                            leadingIcon = Icons.Default.LocationOn,
                            singleLine = false,
                            maxLines = 2,
                            isError = addressError != null,
                            errorMessage = addressError,
                            testTag = "admin_shop_address_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 6. GST Number (required)
                        AdminInputField(
                            value = gstNumber,
                            onValueChange = { input ->
                                if (input.length <= 15) {
                                    gstNumber = input.uppercase().filter { it.isLetterOrDigit() }
                                    gstError = null
                                    generalErrorMessage = null
                                }
                            },
                            label = "GST Number *",
                            placeholder = "15-character GSTIN (e.g. 09AABCU9603R1ZV)",
                            leadingIcon = Icons.Default.ReceiptLong,
                            isError = gstError != null,
                            errorMessage = gstError ?: "Mandatory for shopkeeper verification & billing compliance",
                            isSupporting = gstError == null,
                            testTag = "admin_gstin_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7. Create Password (required)
                        AdminInputField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                generalErrorMessage = null
                            },
                            label = "Create Password *",
                            placeholder = "Min. 6 characters",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                            isError = passwordError != null,
                            errorMessage = passwordError ?: "Used by the merchant to log into Shopkeeper Hub",
                            isSupporting = passwordError == null,
                            testTag = "admin_password_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 8. Confirm Password (required)
                        AdminInputField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                                generalErrorMessage = null
                            },
                            label = "Confirm Password *",
                            placeholder = "Re-enter merchant password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            passwordVisible = confirmPasswordVisible,
                            onPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                            isError = confirmPasswordError != null,
                            errorMessage = confirmPasswordError,
                            testTag = "admin_confirm_password_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 9. Shop Description / Key Items (optional)
                        AdminInputField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Shop Description / Key Items (Optional)",
                            placeholder = "e.g. Color Xerox, Flex Banners, Pamphlets, Visiting Cards, and Binding",
                            leadingIcon = Icons.Default.Description,
                            singleLine = false,
                            maxLines = 3,
                            testTag = "admin_description_input"
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // ACTION BUTTONS
                        // Primary: "Create Shopkeeper Account"
                        Button(
                            onClick = {
                                // Form Validation
                                var hasError = false

                                if (shopName.trim().isEmpty()) {
                                    shopNameError = "Shop / Store Name is required."
                                    hasError = true
                                }
                                if (ownerName.trim().isEmpty()) {
                                    ownerNameError = "Owner Full Name is required."
                                    hasError = true
                                }

                                val cleanPhone = contactNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
                                val phoneRegex = Regex("^[6-9]\\d{9}$")
                                if (cleanPhone.isEmpty()) {
                                    phoneError = "Mobile Contact Number is required."
                                    hasError = true
                                } else if (!phoneRegex.matches(cleanPhone)) {
                                    phoneError = "Please enter a valid 10-digit Indian mobile number."
                                    hasError = true
                                }

                                if (address.trim().isEmpty()) {
                                    addressError = "Physical Shop Address is required."
                                    hasError = true
                                }

                                val cleanGst = gstNumber.trim().uppercase()
                                val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
                                if (cleanGst.isEmpty()) {
                                    gstError = "GST Number is required."
                                    hasError = true
                                } else if (!gstRegex.matches(cleanGst)) {
                                    gstError = "Invalid GSTIN format (must be 15-character e.g. 09AABCU9603R1ZV)."
                                    hasError = true
                                }

                                if (password.trim().length < 6) {
                                    passwordError = "Password must be at least 6 characters."
                                    hasError = true
                                }

                                if (password.trim() != confirmPassword.trim()) {
                                    confirmPasswordError = "Passwords do not match."
                                    hasError = true
                                }

                                if (hasError) return@Button

                                isSubmitting = true
                                generalErrorMessage = null

                                coroutineScope.launch {
                                    val result = repository.adminCreateShopWithShopkeeper(
                                        shopName = shopName,
                                        ownerName = ownerName,
                                        contactNumber = cleanPhone,
                                        category = category,
                                        address = address,
                                        gstNumber = cleanGst,
                                        password = password.trim(),
                                        confirmPassword = confirmPassword.trim(),
                                        description = description
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        creationSuccessResult = result.getOrNull()
                                    } else {
                                        generalErrorMessage = result.exceptionOrNull()?.message
                                            ?: "Failed to create shopkeeper account. Please try again."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_shopkeeper_account_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeSecondary,
                                disabledContainerColor = OrangeSecondary.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating Account in Database...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Create Shopkeeper Account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary: "Cancel / Back"
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_add_shop_cancel_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Cancel / Back",
                                color = TextDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isError: Boolean = false,
    errorMessage: String? = null,
    isSupporting: Boolean = false,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = OrangeSecondary
                )
            },
            trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = OrangeSecondary
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeSecondary,
                focusedLabelColor = OrangeSecondary,
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else BorderLight,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = if (isError) MaterialTheme.colorScheme.error else TextMuted,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
