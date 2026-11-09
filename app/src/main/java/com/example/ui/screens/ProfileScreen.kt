package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import android.app.Activity
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.repository.FlashDropRepository
import com.example.ui.auth.AuthManager
import com.example.ui.components.BottomNavBar
import com.example.ui.components.CircularProfileAvatar
import com.example.ui.components.GoogleMapLocationPickerDialog
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: FlashDropRepository,
    onNavigate: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val isProfileLoading by repository.isProfileLoading.collectAsState()
    val orders by repository.orders.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val isGoogleSignedIn = auth?.currentUser != null

    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }

    val userOrders = remember(orders, profile) {
        repository.getOrdersForCurrentUser(orders)
    }
    val activeOrdersCount = remember(userOrders) {
        userOrders.count { it.status in listOf("pending", "accepted", "preparing", "out_for_delivery", "on_the_way", "order_confirmed") }
    }

    if (showLoginDialog) {
        com.example.ui.components.LoginDialog(
            repository = repository,
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { role ->
                if (role == "MAIN_ADMIN") {
                    onNavigate(Screen.AdminDashboard.route)
                } else if (role == "shopkeeper") {
                    onNavigate(Screen.ShopkeeperDashboard.route)
                }
            },
            onOpenDedicatedAdminLogin = {
                onNavigate(Screen.AdminLogin.route)
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support (Sahayata)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("FlashDrop Mahoba Support", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("✉️ support@flashdrop.in", fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("For delivery assistance, shop onboarding, or order inquiries, please contact our support team via email.", fontSize = 12.sp, color = TextDark)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddressDialog) {
        AddressEditDialog(
            initialAddress = profile.address,
            initialLocality = profile.locality,
            initialPin = profile.pinCode,
            supportedAreas = repository.deliveryConfig.collectAsState().value.supportedAreas,
            onDismiss = { showAddressDialog = false },
            onSave = { addr, loc, pin ->
                repository.updateAddress(addr, loc, pin)
                showAddressDialog = false
            },
            onOpenMap = {
                showMapLocationPicker = true
            }
        )
    }

    if (showMapLocationPicker) {
        GoogleMapLocationPickerDialog(
            initialAddress = profile.address,
            initialLocality = profile.locality.ifBlank { "Alha Chowk" },
            initialPinCode = profile.pinCode.ifBlank { "210427" },
            onDismiss = { showMapLocationPicker = false },
            onLocationConfirmed = { newAddress, newLocality, newPin ->
                repository.updateAddress(newAddress, newLocality, newPin)
                showMapLocationPicker = false
            }
        )
    }

    val isUserAuthenticated = isGoogleSignedIn || (profile.isAuthenticated && profile.role.lowercase() != "guest" && profile.name.isNotBlank() && !profile.name.equals("Guest", ignoreCase = true))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile & Settings", fontWeight = FontWeight.Bold, color = TextDark)
                    }
                },
                actions = {
                    FilterChip(
                        selected = isUserAuthenticated,
                        onClick = {
                            if (isUserAuthenticated) {
                                repository.logout()
                                AuthManager.signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = { },
                                    scope = coroutineScope
                                )
                            } else {
                                onNavigate(Screen.Auth.route)
                            }
                        },
                        label = {
                            Text(
                                text = if (isUserAuthenticated) "Auth: Signed In" else "Auth: Guest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUserAuthenticated) GreenPrimary else TextDark
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isUserAuthenticated) Icons.Default.CheckCircle else Icons.Default.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isUserAuthenticated) GreenPrimary else TextMuted
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenLight,
                            containerColor = Color(0xFFF3F4F6)
                        ),
                        border = BorderStroke(1.dp, if (isUserAuthenticated) GreenPrimary.copy(alpha = 0.4f) else Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("auth_state_demo_toggle")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Profile.route,
                onNavigate = onNavigate,
                activeOrdersCount = activeOrdersCount,
                hasOrders = userOrders.isNotEmpty()
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
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // User Header Profile Card matching design
            val headerBg = if (!isUserAuthenticated) {
                GreenPrimary
            } else {
                when (profile.role.uppercase()) {
                    "MAIN_ADMIN", "ADMIN" -> Color(0xFF6D28D9)
                    "SHOPKEEPER" -> OrangeSecondary
                    else -> GreenPrimary
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isUserAuthenticated) "authenticated_header_card" else "guest_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = headerBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val resolvedPhotoUrl = profile.photoUrl.ifBlank {
                        auth?.currentUser?.photoUrl?.toString() ?: ""
                    }
                    CircularProfileAvatar(
                        photoUrl = resolvedPhotoUrl,
                        displayName = profile.name.takeIf { it.isNotBlank() && !it.equals("Guest", ignoreCase = true) } ?: "User",
                        role = profile.role,
                        size = 62.dp,
                        tintColor = headerBg,
                        backgroundColor = Color.White,
                        modifier = Modifier.testTag("profile_avatar_box")
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (isProfileLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Loading Profile...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Please wait a moment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        } else {
                            val hasCustomName = profile.name.isNotBlank() && !profile.name.equals("Guest", ignoreCase = true)
                            val displayName = when {
                                hasCustomName -> profile.name
                                auth?.currentUser?.displayName?.isNotBlank() == true -> auth.currentUser?.displayName!!
                                else -> "Customer (Mahoba)"
                            }
                            val displayEmail = profile.email.ifBlank { auth?.currentUser?.email ?: "" }
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.testTag("profile_user_name")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (profile.role.uppercase()) {
                                    "MAIN_ADMIN", "ADMIN" -> "Main Admin (${profile.loginId.ifBlank { "PR12" }}) • ${profile.ownerTitle.ifBlank { "Owner" }}"
                                    "SHOPKEEPER" -> "Shopkeeper (${profile.shopName.ifBlank { "Mahoba Merchant" }})"
                                    else -> if (hasCustomName || isUserAuthenticated) "Customer (Mahoba Shopper)" else "Customer (Local Shopper)"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.95f),
                                maxLines = 1,
                                modifier = Modifier.testTag("profile_user_role")
                            )
                            if (displayEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = displayEmail,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    modifier = Modifier.testTag("profile_user_email")
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${profile.locality.ifBlank { "Mahoba" }}, ${profile.state.ifBlank { "Uttar Pradesh" }}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.testTag("profile_user_location")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            when (profile.role.uppercase()) {
                                "MAIN_ADMIN", "ADMIN" -> onNavigate(Screen.EditAdminProfile.route)
                                "SHOPKEEPER" -> onNavigate(Screen.EditShopProfile.route)
                                else -> onNavigate(Screen.EditCustomerProfile.route)
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .size(38.dp)
                            .testTag("header_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val isMainAdmin = isUserAuthenticated && profile.role.uppercase() in listOf("MAIN_ADMIN", "ADMIN")
            val isShopkeeper = isUserAuthenticated && profile.role.uppercase() == "SHOPKEEPER"

            // Role-based UI access control for "Account & Role Switcher" section card (Admins / Shopkeepers only)
            if (isMainAdmin || isShopkeeper) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account & Role Switcher",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            RoleSwitchBadge(
                                currentRole = profile.role,
                                loginId = profile.loginId,
                                onRoleSelected = { role, shopId, loginId ->
                                    repository.setRole(role, shopId, loginId)
                                    if (role == "MAIN_ADMIN" || role == "admin") {
                                        onNavigate(Screen.AdminDashboard.route)
                                    } else if (role == "shopkeeper") {
                                        onNavigate(Screen.ShopkeeperDashboard.route)
                                    }
                                },
                                onOpenLoginModal = { showLoginDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (repository.isShopkeeper()) {
                                        onNavigate(Screen.ShopkeeperDashboard.route)
                                    } else {
                                        showLoginDialog = true
                                    }
                                },
                                modifier = if (isMainAdmin) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shopkeeper Hub", fontSize = 12.sp)
                            }

                            if (isMainAdmin) {
                                Button(
                                    onClick = {
                                        if (repository.isMainAdmin()) {
                                            onNavigate(Screen.AdminDashboard.route)
                                        } else {
                                            onNavigate(Screen.AdminLogin.route)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Main Admin Hub", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Menu Items List Card (Clean list: My Orders, Saved Addresses, Help & Support)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile & Settings",
                        subtitle = "Name, photo, contact & delivery addresses",
                        onClick = {
                            when (profile.role.uppercase()) {
                                "MAIN_ADMIN", "ADMIN" -> onNavigate(Screen.EditAdminProfile.route)
                                "SHOPKEEPER" -> onNavigate(Screen.EditShopProfile.route)
                                else -> onNavigate(Screen.EditCustomerProfile.route)
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "My Orders (Meri Kharidari)",
                        subtitle = "Track ongoing and past orders",
                        onClick = { onNavigate(Screen.Orders.route) }
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    ProfileMenuItem(
                        icon = Icons.Default.LocationOn,
                        title = "Saved Addresses",
                        subtitle = if (isUserAuthenticated && profile.address.isNotBlank()) {
                            "${profile.address}, ${profile.locality}"
                        } else {
                            "Subhash Chowk, Chhatarpur Road, Alha Chowk"
                        },
                        onClick = { showAddressDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help & Support",
                        subtitle = "Email support and merchant assistance",
                        onClick = { showHelpDialog = true }
                    )
                }
            }

            // Authentication Action Card (Logout or Sign In / Sign Up)
            if (isUserAuthenticated) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            repository.logout()
                            AuthManager.signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = { },
                                scope = coroutineScope
                            )
                        }
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Sign Out / Logout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Sign out of your account and switch to Guest mode",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sign In / Sign Up", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenPrimary)
                                Text("Sign in with Google to place orders & track deliveries", fontSize = 12.sp, color = TextDark)
                            }
                        }
                        if (googleSignInError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = googleSignInError ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val activity = context as? Activity
                                    if (activity == null) {
                                        googleSignInError = "Unable to launch Google Sign-In."
                                        return@Button
                                    }
                                    googleSignInError = null
                                    AuthManager.onGoogleSignInClicked(
                                        activity = activity,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            repository.onGoogleSignInSuccess()
                                        },
                                        onAuthError = { err ->
                                            googleSignInError = err.ifBlank { "Google Sign-In failed." }
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_google_signin_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Text("Continue with Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onNavigate(Screen.Auth.route) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                                modifier = Modifier.testTag("profile_signin_button")
                            ) {
                                Text("Portal Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextDark
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
