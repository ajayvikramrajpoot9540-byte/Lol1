package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropCompactLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OrangePrimary = Color(0xFFFF8A24)
private val OrangeDark = Color(0xFFE87510)
private val PeachContainer = Color(0xFFFFE8D6)
private val WarmBackground = Color(0xFFFFF9F2)
private val FieldTextColor = Color(0xFF1F2937)
private val MutedTextColor = Color(0xFF6B7280)
private val SuccessGreen = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperPendingApprovalScreen(
    repository: FlashDropRepository,
    shop: Shop?,
    account: ShopkeeperAccount? = null,
    onApproved: () -> Unit,
    onRejected: (String) -> Unit,
    onSuspended: (String) -> Unit,
    onBackToHome: () -> Unit,
    onSignInAnother: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shops by repository.shops.collectAsState()
    val shopkeepers by repository.shopkeepers.collectAsState()

    var isCheckingStatus by remember { mutableStateOf(false) }

    // Live updated shop instance from repository
    val currentShop = remember(shops, shop) {
        shops.find { it.id == shop?.id || (shop != null && it.gstNumber.equals(shop.gstNumber, ignoreCase = true)) } ?: shop
    }

    val currentAccount = remember(shopkeepers, account, currentShop) {
        shopkeepers.find { it.shopId == currentShop?.id || (currentShop != null && it.gstNumber.equals(currentShop.gstNumber, ignoreCase = true)) } ?: account
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(showText = true)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTextColor
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PENDING APPROVAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
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
                .background(WarmBackground)
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Animated Pending Badge Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PeachContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "Awaiting Approval",
                    tint = OrangeDark,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Your shop registration is awaiting Admin approval.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FieldTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We have received your registration details. FlashDrop Main Admins are reviewing your store profile.",
                fontSize = 13.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Store Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentShop?.name?.ifBlank { "Registered Store" } ?: "Your Shop",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FieldTextColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Under Review",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!currentShop?.gstNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GSTIN: ", fontSize = 12.sp, color = MutedTextColor)
                            Text(
                                text = currentShop?.gstNumber ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FieldTextColor
                            )
                        }
                    }

                    if (!currentShop?.ownerName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text("Owner: ", fontSize = 12.sp, color = MutedTextColor)
                            Text(
                                text = currentShop?.ownerName ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FieldTextColor
                            )
                        }
                    }

                    if (!currentShop?.phone.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text("Contact: ", fontSize = 12.sp, color = MutedTextColor)
                            Text(
                                text = currentShop?.phone ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FieldTextColor
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

                    // Step 1: Application Received
                    TimelineStep(
                        title = "Registration Submitted",
                        subtitle = "Application details saved securely",
                        isCompleted = true,
                        isCurrent = false
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2: Verification In Progress
                    TimelineStep(
                        title = "Admin Verification In Progress",
                        subtitle = "Verifying GST and store location in Mahoba",
                        isCompleted = false,
                        isCurrent = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 3: Portal Activation
                    TimelineStep(
                        title = "Shopkeeper Portal Access",
                        subtitle = "Access orders, inventory and product listings",
                        isCompleted = false,
                        isCurrent = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PeachContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = OrangeDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "To ensure consumer safety, FlashDrop Main Admins (Pranjal Rajpoot & Chanchal Didi) personally review all new shop registrations. Verification typically completes within 24 hours.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Check Status Button
            Button(
                onClick = {
                    isCheckingStatus = true
                    coroutineScope.launch {
                        delay(600)
                        val updatedShop = repository.shops.value.find {
                            it.id == currentShop?.id || (currentShop != null && it.gstNumber.equals(currentShop.gstNumber, ignoreCase = true))
                        }
                        isCheckingStatus = false
                        if (updatedShop != null) {
                            when {
                                updatedShop.status.equals("approved", ignoreCase = true) || updatedShop.isOpen -> {
                                    repository.activateShopkeeperSessionForShop(updatedShop)
                                    snackbarHostState.showSnackbar("Congratulations! Your shop has been approved!")
                                    delay(400)
                                    onApproved()
                                }
                                updatedShop.status.equals("rejected", ignoreCase = true) -> {
                                    onRejected(updatedShop.rejectionReason.ifBlank { "Application did not meet verification criteria." })
                                }
                                updatedShop.status.equals("suspended", ignoreCase = true) -> {
                                    onSuspended(updatedShop.suspensionReason.ifBlank { "Account temporarily suspended." })
                                }
                                else -> {
                                    snackbarHostState.showSnackbar("Your registration is still under review by Main Admin.")
                                }
                            }
                        } else {
                            snackbarHostState.showSnackbar("Your registration is still under review by Main Admin.")
                        }
                    }
                },
                enabled = !isCheckingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("check_approval_status_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Approval Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Return to Home Button
            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pending_back_to_home_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTextColor)
            ) {
                Text("Return to Customer Home", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sign In With Another Account
            OutlinedButton(
                onClick = onSignInAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pending_sign_in_another_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PeachContainer),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeDark)
            ) {
                Text("Sign In with Different Shop", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}

@Composable
private fun TimelineStep(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> SuccessGreen
                        isCurrent -> OrangePrimary
                        else -> Color(0xFFE5E7EB)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9CA3AF))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) OrangeDark else FieldTextColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MutedTextColor
            )
        }
    }
}
