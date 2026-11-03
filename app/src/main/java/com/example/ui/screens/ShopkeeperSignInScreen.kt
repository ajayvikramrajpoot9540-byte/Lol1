package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.ui.util.ScreenType
import com.example.ui.util.rememberScreenType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.model.UserProfile
import com.example.data.repository.FlashDropRepository
import com.example.data.repository.ShopkeeperLoginResult
import com.example.ui.components.FlashDropCompactLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Orange-themed palette matching the FlashDrop Shopkeeper Portal design
private val OrangePrimary = Color(0xFFFF8A24)
private val OrangeDark = Color(0xFFE87510)
private val PeachContainer = Color(0xFFFFE8D6)
private val WarmBackground = Color(0xFFFFF9F2)
private val ShieldGreen = Color(0xFF16A34A)
private val FieldBorderColor = Color(0xFFE5E7EB)
private val FieldTextColor = Color(0xFF1F2937)
private val MutedTextColor = Color(0xFF6B7280)

@Composable
fun ShopkeeperSignInScreen(
    repository: FlashDropRepository,
    onSignInSuccess: (UserProfile) -> Unit,
    onPendingApproval: (Shop, ShopkeeperAccount?) -> Unit,
    onAccountStatus: (Shop, String, String) -> Unit, // shop, status, reason
    onRegisterShop: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var gstNumber by remember { mutableStateOf("") }
    var shopNameOrId by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(repository.getShopkeeperLockoutSecondsRemaining()) }

    // Lockout countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = repository.getShopkeeperLockoutSecondsRemaining()
        }
    }

    fun handleSignIn() {
        errorMessage = null
        val trimmedGst = gstNumber.trim().uppercase()
        val trimmedShop = shopNameOrId.trim()
        val trimmedMobile = mobileNumber.trim()
        val trimmedPassword = password.trim()

        if (trimmedGst.isEmpty() || trimmedShop.isEmpty() || trimmedMobile.isEmpty() || trimmedPassword.isEmpty()) {
            errorMessage = "Please enter all 4 credentials to sign in."
            return
        }

        isLoading = true
        coroutineScope.launch {
            delay(400) // Brief smooth feedback
            val result = repository.authenticateShopkeeper(
                gstNumber = trimmedGst,
                shopNameOrId = trimmedShop,
                mobileNumber = trimmedMobile,
                password = trimmedPassword
            )
            isLoading = false
            when (result) {
                is ShopkeeperLoginResult.Success -> {
                    onSignInSuccess(result.profile)
                }
                is ShopkeeperLoginResult.PendingApproval -> {
                    onPendingApproval(result.shop, result.account)
                }
                is ShopkeeperLoginResult.Rejected -> {
                    onAccountStatus(result.shop, "REJECTED", result.reason)
                }
                is ShopkeeperLoginResult.Suspended -> {
                    onAccountStatus(result.shop, "SUSPENDED", result.reason)
                }
                is ShopkeeperLoginResult.DocumentsRequired -> {
                    onAccountStatus(result.shop, "DOCUMENTS_REQUIRED", result.reason)
                }
                is ShopkeeperLoginResult.Failure -> {
                    errorMessage = result.message
                    lockoutSeconds = repository.getShopkeeperLockoutSecondsRemaining()
                }
            }
        }
    }

    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallMobile) 16.dp else 24.dp, vertical = if (isSmallMobile) 10.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Back Button, FlashDrop Logo, SHOPKEEPER Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = if (isSmallMobile) 12.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("shopkeeper_sign_in_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTextColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    FlashDropCompactLogo(showText = true)
                }

                // SHOPKEEPER pill badge matching screenshot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PeachContainer)
                        .padding(horizontal = if (isSmallMobile) 10.dp else 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SHOPKEEPER",
                        fontSize = if (isSmallMobile) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeDark,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallMobile) 6.dp else 10.dp))

            // Center Circular Store Icon Badge
            Box(
                modifier = Modifier
                    .size(if (isSmallMobile) 64.dp else 84.dp)
                    .clip(CircleShape)
                    .background(PeachContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Shopkeeper Store",
                    tint = OrangePrimary,
                    modifier = Modifier.size(if (isSmallMobile) 36.dp else 46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Heading & Subtitle
            Text(
                text = "Shopkeeper Sign In",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = FieldTextColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Manage your shop, products and orders.",
                fontSize = 13.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error or Lockout Banner
            if (errorMessage != null || lockoutSeconds > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("shopkeeper_login_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lockoutSeconds > 0) Color(0xFFFEF2F2) else Color(0xFFFFF1F2)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (lockoutSeconds > 0) Color(0xFFFCA5A5) else Color(0xFFFECDD3)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (lockoutSeconds > 0) {
                                "Too many failed attempts. Locked for security. Try again in $lockoutSeconds seconds."
                            } else {
                                errorMessage ?: ""
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Input 1: GST Number
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "GST Number",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FieldTextColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = {
                        gstNumber = it.uppercase()
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "Enter your GST number",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = FieldBorderColor,
                        focusedTextColor = FieldTextColor,
                        unfocusedTextColor = FieldTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_gst_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input 2: Shop Name / Shop ID
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Shop Name / Shop ID",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FieldTextColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = shopNameOrId,
                    onValueChange = {
                        shopNameOrId = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "Enter your shop name or ID",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = FieldBorderColor,
                        focusedTextColor = FieldTextColor,
                        unfocusedTextColor = FieldTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_shop_name_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input 3: Mobile Number
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Mobile Number",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FieldTextColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = {
                        // Allow digits and spaces only
                        val filtered = it.filter { ch -> ch.isDigit() || ch == ' ' || ch == '+' }
                        if (filtered.length <= 14) {
                            mobileNumber = filtered
                            errorMessage = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Enter your mobile number",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = FieldBorderColor,
                        focusedTextColor = FieldTextColor,
                        unfocusedTextColor = FieldTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_mobile_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input 4: Password
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FieldTextColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "••••••••••••",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        handleSignIn()
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = FieldBorderColor,
                        focusedTextColor = FieldTextColor,
                        unfocusedTextColor = FieldTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_password_input")
                )
            }

            // Forgot Password Link (right-aligned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OrangeDark,
                    modifier = Modifier
                        .clickable { showForgotPasswordDialog = true }
                        .padding(vertical = 4.dp)
                        .testTag("shopkeeper_forgot_password_btn")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main "Sign In" Button (solid orange)
            Button(
                onClick = { handleSignIn() },
                enabled = !isLoading && lockoutSeconds == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("shopkeeper_sign_in_submit_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    disabledContainerColor = OrangePrimary.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subtitle: New to FlashDrop?
            Text(
                text = "New to FlashDrop?",
                fontSize = 13.sp,
                color = MutedTextColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary "Register Your Shop" Outlined Button
            OutlinedButton(
                onClick = onRegisterShop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("shopkeeper_register_shop_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = OrangePrimary
                )
            ) {
                Text(
                    text = "Register Your Shop",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Security Pill Banner matching screenshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PeachContainer.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified Security",
                        tint = ShieldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shopkeeper access requires Admin approval.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FieldTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetContact by remember { mutableStateOf("") }
        var resetError by remember { mutableStateOf<String?>(null) }
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                forgotPasswordMessage = null
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Shop Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FieldTextColor
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered GSTIN, Shop ID, or Contact Number. FlashDrop Admins will verify and send reset instructions.",
                        fontSize = 13.sp,
                        color = MutedTextColor,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (forgotPasswordMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = forgotPasswordMessage ?: "",
                                fontSize = 12.sp,
                                color = ShieldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = resetContact,
                            onValueChange = {
                                resetContact = it
                                resetError = null
                            },
                            placeholder = { Text("GSTIN or Mobile Number", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (resetError != null) {
                            Text(
                                text = resetError ?: "",
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (forgotPasswordMessage == null) {
                    Button(
                        onClick = {
                            if (resetContact.isBlank()) {
                                resetError = "Please enter your GSTIN or mobile number."
                                return@Button
                            }
                            isResetting = true
                            val res = repository.requestPasswordReset(resetContact)
                            isResetting = false
                            if (res.isSuccess) {
                                forgotPasswordMessage = res.getOrNull()
                            } else {
                                resetError = res.exceptionOrNull()?.message ?: "Reset request failed."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Send Instructions", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showForgotPasswordDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (forgotPasswordMessage == null) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel", color = MutedTextColor)
                    }
                }
            }
        )
    }
}
