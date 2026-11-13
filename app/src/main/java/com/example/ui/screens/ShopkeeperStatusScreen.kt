package com.example.ui.screens

import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.ui.components.FlashDropCompactLogo

private val OrangePrimary = Color(0xFFFF8A24)
private val WarmBackground = Color(0xFFFFF9F2)
private val FieldTextColor = Color(0xFF1F2937)
private val MutedTextColor = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopkeeperStatusScreen(
    shop: Shop?,
    status: String, // "REJECTED" or "SUSPENDED"
    reason: String,
    onReApply: () -> Unit,
    onSignInAnother: () -> Unit,
    onBackToHome: () -> Unit
) {
    val isSuspended = status.equals("SUSPENDED", ignoreCase = true)
    val isDocumentsRequired = status.equals("DOCUMENTS_REQUIRED", ignoreCase = true)
    val isPendingReview = status.equals("PENDING_REVIEW", ignoreCase = true)
    val statusTitle = when {
        isDocumentsRequired -> "Document Clarification Required"
        isPendingReview -> "Registration Under Review"
        isSuspended -> "Account Suspended"
        else -> "Application Declined"
    }
    val accentColor = when {
        isDocumentsRequired -> Color(0xFFD97706) // Amber
        isPendingReview -> Color(0xFF2563EB) // Blue
        isSuspended -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    val containerBg = when {
        isDocumentsRequired -> Color(0xFFFEF3C7)
        isPendingReview -> Color(0xFFDBEAFE)
        isSuspended -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }

    var showContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(showText = true)
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Status Icon Circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isDocumentsRequired -> Icons.Default.WarningAmber
                        isPendingReview -> Icons.Default.HelpOutline
                        isSuspended -> Icons.Default.PauseCircleOutline
                        else -> Icons.Default.Block
                    },
                    contentDescription = statusTitle,
                    tint = accentColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = statusTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FieldTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isDocumentsRequired -> "FlashDrop Admins requested corrected or clearer business documents before activating ${shop?.name ?: "this store"}."
                    isPendingReview -> "Your shop registration and verification documents for ${shop?.name ?: "this store"} are currently under review."
                    isSuspended -> "Operations for ${shop?.name ?: "this store"} have been suspended by FlashDrop Admins."
                    else -> "The shop registration for ${shop?.name ?: "this store"} could not be approved at this time."
                },
                fontSize = 13.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reason Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isDocumentsRequired -> "Admin Clarification Notes"
                                isPendingReview -> "Review Details"
                                isSuspended -> "Suspension Notice"
                                else -> "Reason for Decline"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FieldTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = reason.ifBlank {
                            when {
                                isDocumentsRequired -> "Please re-upload a clear copy of your business documents (Aadhaar, PAN, or GST certificate)."
                                isPendingReview -> "Application submitted and queued. Verification by authorized Main Admins typically completes within 24 hours."
                                isSuspended -> "Account flagged for compliance verification by FlashDrop Admins. Please contact support to resolve."
                                else -> "Submitted GST or store details could not be verified in the commercial directory."
                            }
                        },
                        fontSize = 13.sp,
                        color = FieldTextColor,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(containerBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = when {
                                isDocumentsRequired -> "You can re-upload or update your documents directly to continue registration."
                                isPendingReview -> "You will receive immediate dashboard access as soon as both Main Admins approve your credentials."
                                isSuspended -> "Orders and catalog are currently disabled for this store. Customer orders will resume once verified."
                                else -> "You may correct your store credentials or provide updated GST documentation to re-apply."
                            },
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action 1: Appeal / Contact Admin
            Button(
                onClick = { showContactDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("status_contact_admin_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSuspended) "Appeal Suspension" else "Contact Main Admins",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: Re-Apply or Re-Upload
            if (isDocumentsRequired) {
                OutlinedButton(
                    onClick = onReApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("status_reupload_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary)
                ) {
                    Text("Re-Upload / Fix Documents", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (!isSuspended && !isPendingReview) {
                OutlinedButton(
                    onClick = onReApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("status_reapply_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary)
                ) {
                    Text("Re-Apply with New Details", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action 3: Sign In with Different Shop
            OutlinedButton(
                onClick = onSignInAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("status_sign_in_another_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTextColor)
            ) {
                Text("Sign In with Different Shop", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back to Customer Home
            TextButton(onClick = onBackToHome) {
                Text("Return to Customer Home", color = MutedTextColor, fontSize = 13.sp)
            }
        }
    }
}

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "FlashDrop Admin Governance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FieldTextColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "To resolve approval or suspension queries, reach out directly to FlashDrop Co-Owners:",
                        fontSize = 13.sp,
                        color = MutedTextColor
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Main Admin Desk 1: +91 98765 00012", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FieldTextColor)
                            Text("Main Admin Desk 2: +91 98765 00013", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FieldTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("admin@flashdrop.local", fontSize = 13.sp, color = FieldTextColor)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subhash Chowk, Chhatarpur Road, Mahoba", fontSize = 13.sp, color = MutedTextColor)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showContactDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
