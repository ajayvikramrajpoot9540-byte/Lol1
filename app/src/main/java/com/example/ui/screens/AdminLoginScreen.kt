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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

private val AdminPrimaryColor = Color(0xFF6D28D9)
private val AdminLightBg = Color(0xFFEDE9FE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    repository: FlashDropRepository,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }

    val isDevMode = repository.isDevMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AdminLightBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AdminPrimaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Main Admin Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "FlashDrop Authorized Administration",
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
                        onClick = { showSetupGuideDialog = true },
                        modifier = Modifier.testTag("admin_setup_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Firebase Setup Info",
                            tint = AdminPrimaryColor
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Official FlashDrop Brand Logo
            FlashDropLogo(
                iconSize = 100.dp,
                showTagline = true,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Portal Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AdminLightBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AdminPrimaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FlashDrop Co-Owner Administration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminPrimaryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sign in with your Owner Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Equal administrative authority for Pranjal Rajpoot and Chanchal Didi to govern the FlashDrop Mahoba marketplace.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        lineHeight = 16.sp
                    )

                    // Development Mode Notice (Only shown in Development builds)
                    if (isDevMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Development Demo Authentication Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Main Admin 1: Pranjal Rajpoot (ID: PR12 / Pass: PR12)\n• Main Admin 2: Chanchal Didi (ID: CD12 / Pass: CD12)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Error Message Display
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("admin_login_error_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Login ID input
                    OutlinedTextField(
                        value = loginId,
                        onValueChange = {
                            loginId = it
                            errorMessage = null
                        },
                        label = { Text("Login ID or Full Name") },
                        placeholder = { Text("e.g. PR12 or Chanchal Didi") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AdminPrimaryColor
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_login_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminPrimaryColor,
                            focusedLabelColor = AdminPrimaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password input with masked characters and Show/Hide toggle
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Enter secure password") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = AdminPrimaryColor
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = AdminPrimaryColor
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminPrimaryColor,
                            focusedLabelColor = AdminPrimaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign In Button
                    Button(
                        onClick = {
                            if (loginId.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both Login ID and password."
                                return@Button
                            }

                            val result = repository.loginAsMainAdmin(loginId, password)
                            if (result.isSuccess) {
                                errorMessage = null
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                                    ?: "Invalid Login ID or password. Access restricted to authorized Main Admins."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_admin_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimaryColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In as Main Admin",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel & Return to Shopping",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equal Permissions Overview Card
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
                    Text(
                        text = "Equal Main Admin Authority",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Both owners operate with identical permissions on FlashDrop. Neither owner holds superior authority over the other.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Spacer(modifier = Modifier.height(10.dp))

                    val permissionsList = listOf(
                        "Add & remove shopkeeper accounts",
                        "Approve, suspend, and reactivate local shops",
                        "View & manage all shops and products",
                        "View & manage all marketplace customer orders",
                        "Configure delivery fees and supported Mahoba localities",
                        "View sales & revenue summaries"
                    )

                    permissionsList.forEach { perm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = perm,
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase Production Security Note
            OutlinedButton(
                onClick = { showSetupGuideDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AdminPrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Firebase Production Security Guide",
                    color = AdminPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showSetupGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSetupGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AdminPrimaryColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Firebase Production Setup Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "How Main Admin accounts are secured in production:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Owner Provisioning:\nCreate Firebase Auth accounts in Firebase Console for the two authorized owners (pranjal.owner@flashdrop.local and chanchal.owner@flashdrop.local).\n\n" +
                                "2. Server-Side Role Assignment:\nSet Custom Claims (`{ role: 'MAIN_ADMIN' }`) via Firebase Admin SDK. Users cannot assign this role to themselves.\n\n" +
                                "3. Zero Password Exposure:\nPasswords are never saved in Firestore or visible in client code. Demo credentials are automatically disabled in release builds (`BuildConfig.DEBUG == false`).\n\n" +
                                "4. Firebase Security Rules:\nAll admin read/writes in firestore.rules require `request.auth.token.role == 'MAIN_ADMIN'` or verified owner credentials.",
                        fontSize = 12.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSetupGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimaryColor)
                ) {
                    Text("Understood")
                }
            }
        )
    }
}
