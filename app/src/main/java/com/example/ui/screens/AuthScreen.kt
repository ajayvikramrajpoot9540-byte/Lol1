package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.repository.FlashDropRepository
import com.example.ui.auth.AuthManager
import com.example.ui.components.FlashDropLogo
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
fun AuthScreen(
    repository: FlashDropRepository,
    onAuthSuccess: (role: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Sign Up

    // Sign In inputs
    var signInNameOrId by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up inputs
    var signUpFullName by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Feedback & state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Password reset dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetInput by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    // Security info dialog
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FlashDrop Authentication",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Sign In or Create Account",
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
                actions = {
                    IconButton(
                        onClick = { showSecurityInfoDialog = true },
                        modifier = Modifier.testTag("auth_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Security Info",
                            tint = GreenPrimary
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
                iconSize = 90.dp,
                showTagline = true,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_form_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Two Options: Sign In vs Sign Up TabRow
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFF3F4F6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = if (selectedTab == 0) GreenPrimary else OrangeSecondary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == 0) GreenPrimary else TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sign In",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (selectedTab == 0) GreenPrimary else TextMuted
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_sign_in")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == 1) OrangeSecondary else TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sign Up",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (selectedTab == 1) OrangeSecondary else TextMuted
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_sign_up")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message Display
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
                                modifier = Modifier.testTag("auth_error_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Success Message Display
                    if (successMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenLight)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = GreenPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("auth_success_text")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }


                    // Customer Google OAuth Sign-In Button
                    Button(
                        onClick = {
                            val activity = context as? Activity
                            if (activity == null) {
                                errorMessage = "Unable to launch Google Sign-In. Please try again."
                                return@Button
                            }
                            isGoogleSigningIn = true
                            errorMessage = null
                            AuthManager.onGoogleSignInClicked(
                                activity = activity,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isGoogleSigningIn = false
                                    val profile = repository.onGoogleSignInSuccess()
                                    onAuthSuccess(profile.role)
                                },
                                onAuthError = { err ->
                                    isGoogleSigningIn = false
                                    errorMessage = err.ifBlank { "Google Sign-In failed. Please try again." }
                                },
                                scope = coroutineScope,
                                onAuthCancelled = {
                                    isGoogleSigningIn = false
                                }
                            )
                        },
                        enabled = !isLoading && !isGoogleSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_google_signin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        if (isGoogleSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connecting to Google...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = OrangeSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                        Text(
                            text = "  OR PORTAL / LOCAL LOGIN  ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == 0) {
                        // ----------------- SIGN IN FORM -----------------
                        Text(
                            text = "Welcome Back!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Sign in using your Full Name or assigned Login ID",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                            textAlign = TextAlign.Center
                        )

                        // Full Name or Login ID
                        OutlinedTextField(
                            value = signInNameOrId,
                            onValueChange = {
                                signInNameOrId = it
                                errorMessage = null
                            },
                            label = { Text("Full Name or Login ID") },
                            placeholder = { Text("e.g. Pranjal Rajpoot, Ramesh Kumar, PR12") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = GreenPrimary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signin_name_or_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                focusedLabelColor = GreenPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password (masked with show/hide toggle)
                        OutlinedTextField(
                            value = signInPassword,
                            onValueChange = {
                                signInPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GreenPrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { signInPasswordVisible = !signInPasswordVisible },
                                    modifier = Modifier.testTag("toggle_signin_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (signInPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (signInPasswordVisible) "Hide password" else "Show password",
                                        tint = GreenPrimary
                                    )
                                }
                            },
                            visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signin_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                focusedLabelColor = GreenPrimary
                            )
                        )

                        // Forgot Password button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetInput = signInNameOrId
                                    resetMessage = null
                                    showResetDialog = true
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 12.sp,
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sign In Action Button
                        Button(
                            onClick = {
                                if (signInNameOrId.isBlank() || signInPassword.isBlank()) {
                                    errorMessage = "Please enter both Full Name / Login ID and password."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    delay(200) // subtle smooth transition
                                    val result = repository.authenticateUser(signInNameOrId, signInPassword)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        val profile = result.getOrThrow()
                                        onAuthSuccess(profile.role)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                            ?: "Invalid credentials. Please verify your details."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_signin_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign In",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    } else {
                        // ----------------- SIGN UP FORM -----------------
                        Text(
                            text = "Create Your Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Register to place orders and track deliveries in Mahoba",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                            textAlign = TextAlign.Center
                        )

                        // Full Name
                        OutlinedTextField(
                            value = signUpFullName,
                            onValueChange = {
                                signUpFullName = it
                                errorMessage = null
                            },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Ramesh Kumar or Priya Sharma") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = OrangeSecondary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_fullname_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeSecondary,
                                focusedLabelColor = OrangeSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = {
                                signUpPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Minimum 4 characters") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OrangeSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { signUpPasswordVisible = !signUpPasswordVisible },
                                    modifier = Modifier.testTag("toggle_signup_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (signUpPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (signUpPasswordVisible) "Hide password" else "Show password",
                                        tint = OrangeSecondary
                                    )
                                }
                            },
                            visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeSecondary,
                                focusedLabelColor = OrangeSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = signUpConfirmPassword,
                            onValueChange = {
                                signUpConfirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OrangeSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible },
                                    modifier = Modifier.testTag("toggle_signup_confirm_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (signUpConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (signUpConfirmPasswordVisible) "Hide password" else "Show password",
                                        tint = OrangeSecondary
                                    )
                                }
                            },
                            visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_confirm_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeSecondary,
                                focusedLabelColor = OrangeSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Sign Up Action Button
                        Button(
                            onClick = {
                                // Validate required fields
                                val trimmedName = signUpFullName.trim()
                                if (trimmedName.isEmpty()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }

                                // Critical validation: Check reserved admin names in UI layer
                                if (repository.isReservedAdminName(trimmedName)) {
                                    errorMessage = "This name is reserved for a FlashDrop Admin. Please choose another name."
                                    return@Button
                                }

                                if (signUpPassword.isEmpty()) {
                                    errorMessage = "Please enter a password."
                                    return@Button
                                }

                                if (signUpPassword.length < 4) {
                                    errorMessage = "Password must be at least 4 characters long."
                                    return@Button
                                }

                                if (signUpPassword != signUpConfirmPassword) {
                                    errorMessage = "Passwords do not match. Please re-enter."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    delay(200)
                                    val result = repository.registerNewUser(trimmedName, signUpPassword)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        val profile = result.getOrThrow()
                                        onAuthSuccess(profile.role)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                            ?: "Registration failed. Please try again."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_signup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Account / Sign Up",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("auth_cancel_button")
                    ) {
                        Text(
                            text = "Cancel & Return",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin & Security Information Card
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
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FlashDrop Admin Reservation Policy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Administrator accounts are strictly reserved for verified FlashDrop owners: Pranjal Rajpoot (Admin 1, Code PR) and Chanchal Didi (Admin 2, Code CD).\n" +
                               "• Self-registration using reserved Admin names is blocked across the app and database rules.\n" +
                               "• Regular users and shopkeepers cannot obtain admin rights by entering admin names.\n" +
                               "• Temporary demo credentials PR12 & CD12 are disabled in production builds, where Firebase Authentication custom claims govern portal access.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered Full Name or Email to request a password reset.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetInput,
                        onValueChange = {
                            resetInput = it
                            resetMessage = null
                        },
                        label = { Text("Full Name or Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_password_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetMessage ?: "",
                            fontSize = 12.sp,
                            color = if (resetMessage?.contains("sent") == true || resetMessage?.contains("logged") == true) GreenPrimary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = repository.requestPasswordReset(resetInput)
                        if (res.isSuccess) {
                            resetMessage = res.getOrThrow()
                        } else {
                            resetMessage = res.exceptionOrNull()?.message ?: "Reset request failed."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_reset_button")
                ) {
                    Text("Request Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }

    // Security Info Dialog
    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "FlashDrop Security & Access Architecture",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Role-Based Access Control (RBAC):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = "• MAIN_ADMIN: Pranjal Rajpoot and Chanchal Didi hold equal co-ownership.\n" +
                               "• SHOPKEEPER: Merchants access only their respective shop dashboard.\n" +
                               "• CUSTOMER: Shoppers browse Mahoba products and order locally.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "2. Production Credential Safety:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = "• Passwords are encrypted/hashed and never stored in plain text.\n" +
                               "• Temporary demo credentials (PR12, CD12) are restricted to development mode.\n" +
                               "• Production deployments enforce Firebase Authentication and Security Rules.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSecurityInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got It")
                }
            }
        )
    }
}
