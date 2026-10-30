package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@Composable
fun LoginDialog(
    repository: FlashDropRepository,
    onDismiss: () -> Unit,
    onLoginSuccess: (role: String) -> Unit,
    onOpenDedicatedAdminLogin: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Main Admin, 1: Shopkeeper, 2: Customer
    var loginIdInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Customer fields
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FlashDropLogo(
                    iconSize = 84.dp,
                    showTagline = true,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = when (selectedTab) {
                        0 -> "Main Admin Secure Portal"
                        1 -> "Shopkeeper Merchant Access"
                        else -> "Customer Sign In & Register"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when (selectedTab) {
                        0 -> "Authorized Mahoba owners (PR12 / CD12)"
                        1 -> "Authorized Mahoba shopkeeper login"
                        else -> "Sign in or register to shop in Mahoba"
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                            loginIdInput = ""
                            passwordInput = ""
                        },
                        text = { Text("Main Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                            loginIdInput = ""
                            passwordInput = ""
                        },
                        text = { Text("Shopkeeper", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            errorMessage = null
                        },
                        text = { Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Main Admin Login Form (PR12 or CD12)
                        Text(
                            text = "Main Admin Owners: Enter PR12 (Pranjal) or CD12 (Chanchal)",
                            fontSize = 11.sp,
                            color = Color(0xFF6D28D9),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = loginIdInput,
                            onValueChange = {
                                loginIdInput = it
                                errorMessage = null
                            },
                            label = { Text("Admin Login ID (e.g. PR12 or CD12)") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF6D28D9)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_id_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            label = { Text("Secure Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (onOpenDedicatedAdminLogin != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenDedicatedAdminLogin()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_dedicated_admin_screen_button")
                            ) {
                                Text(
                                    "Open Dedicated Main Admin Screen →",
                                    color = Color(0xFF6D28D9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    1 -> {
                        // Shopkeeper Login Form
                        Text(
                            text = "Authorized Shopkeepers: Enter your assigned Shopkeeper ID",
                            fontSize = 11.sp,
                            color = OrangeSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = loginIdInput,
                            onValueChange = {
                                loginIdInput = it
                                errorMessage = null
                            },
                            label = { Text("Shopkeeper ID (e.g. SK_SHARMA)") },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shopkeeper_login_id_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            label = { Text("Shopkeeper Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shopkeeper_password_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    2 -> {
                        // Customer Login Form
                        Text(
                            text = "Mahoba Customer: Browse and place orders",
                            fontSize = 11.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Your Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Mobile Number (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            val res = repository.loginAsMainAdmin(loginIdInput, passwordInput)
                            if (res.isSuccess) {
                                onLoginSuccess("MAIN_ADMIN")
                                onDismiss()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Admin verification failed"
                            }
                        }
                        1 -> {
                            val res = repository.loginAsShopkeeper(loginIdInput, passwordInput)
                            if (res.isSuccess) {
                                onLoginSuccess("shopkeeper")
                                onDismiss()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Shopkeeper verification failed"
                            }
                        }
                        2 -> {
                            val trimmedName = customerName.trim()
                            if (trimmedName.isEmpty()) {
                                errorMessage = "Please enter your name to continue."
                                return@Button
                            }
                            if (repository.isReservedAdminName(trimmedName)) {
                                errorMessage = "This name is reserved for a FlashDrop Admin. Please choose another name."
                                return@Button
                            }
                            repository.loginAsCustomer(
                                name = trimmedName,
                                phone = customerPhone.ifBlank { "+91 98765 12345" }
                            )
                            onLoginSuccess("customer")
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedTab) {
                        0 -> Color(0xFF6D28D9)
                        1 -> OrangeSecondary
                        else -> GreenPrimary
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_login_button")
            ) {
                Text("Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
