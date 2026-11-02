package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShopVerificationRequest
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.StockToggleSwitch
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

private val PurplePrimary = Color(0xFF6D28D9)
private val PurpleLight = Color(0xFFEDE9FE)
private val WarningAmber = Color(0xFFD97706)
private val WarningBg = Color(0xFFFEF3C7)

@Composable
fun ShopVerificationSettingsSection(
    repository: FlashDropRepository,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val registrationConfig by repository.shopRegistrationConfig.collectAsState()
    val verificationRequests by repository.shopVerificationRequests.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedRequestForReview by remember { mutableStateOf<ShopVerificationRequest?>(null) }

    // Dialog action states
    var showApproveConfirmDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showRequestDocsDialog by remember { mutableStateOf(false) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var requestDocsNotesInput by remember { mutableStateOf("") }
    var adminApprovalNotesInput by remember { mutableStateOf("") }
    var isTogglingSetting by remember { mutableStateOf(false) }

    val filteredRequests = remember(verificationRequests, selectedStatusFilter) {
        if (selectedStatusFilter == "ALL") {
            verificationRequests
        } else {
            verificationRequests.filter { it.status.equals(selectedStatusFilter, ignoreCase = true) }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // CARD 1: 3-STEP SHOP REGISTRATION SYSTEM SETTING
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_shop_registration_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PurpleLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "3-Step Shop Registration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Admin-Controlled Onboarding Workflow",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Toggle Switch with equal Main Admin permissions
                        if (isTogglingSetting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = PurplePrimary
                            )
                        } else {
                            StockToggleSwitch(
                                checked = registrationConfig.threeStepEnabled,
                                onCheckedChange = { newState ->
                                    isTogglingSetting = true
                                    coroutineScope.launch {
                                        val res = repository.updateShopRegistrationMode(newState)
                                        isTogglingSetting = false
                                        res.onSuccess {
                                            val stateLabel = if (newState) "ENABLED (3-Step)" else "DISABLED (Single-Page)"
                                            snackbarHostState.showSnackbar("Shop Registration set to $stateLabel")
                                        }.onFailure { err ->
                                            snackbarHostState.showSnackbar("Failed to update: ${err.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("three_step_registration_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Status pill and configuration details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (registrationConfig.threeStepEnabled) GreenPrimary else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (registrationConfig.threeStepEnabled) "ACTIVE (3-Step Verification Required)" else "NORMAL (Single-Page Form)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (registrationConfig.threeStepEnabled) GreenPrimary else Color.Gray
                            )
                        }

                        Text(
                            text = "v${registrationConfig.version}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (registrationConfig.threeStepEnabled) {
                            "When ON: Applicants must complete Step 1 (Store Profile), Step 2 (Aadhaar / PAN / GST Document Verification), and Step 3 (Secure 20-Second FlashDrop Shop ID Generation). Application is held in PENDING_REVIEW until approved below."
                        } else {
                            "When OFF: Applicants register through the traditional single-page form with immediate submission. Document verification workflow is bypassed."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Equal Main Admin authorization footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Updated: ${registrationConfig.lastUpdatedAtFormatted} by ${registrationConfig.lastUpdatedByAdminName} (${registrationConfig.lastUpdatedByAdminId}) • Equal co-admin control",
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // CARD 2: DOCUMENT REVIEW REQUESTS HEADER & FILTERS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_verification_requests_container"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = OrangeSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Shop Verification Requests",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "${verificationRequests.size} total merchant applications",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { repository.syncWithFirestore() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = PurplePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status filter chips
                    val filterOptions = listOf(
                        "ALL",
                        ShopVerificationRequest.STATUS_PENDING_REVIEW,
                        ShopVerificationRequest.STATUS_DOCUMENTS_REQUIRED,
                        ShopVerificationRequest.STATUS_APPROVED,
                        ShopVerificationRequest.STATUS_REJECTED,
                        ShopVerificationRequest.STATUS_DRAFT
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterOptions) { filter ->
                            val isSelected = selectedStatusFilter.equals(filter, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatusFilter = filter },
                                label = {
                                    val count = if (filter == "ALL") {
                                        verificationRequests.size
                                    } else {
                                        verificationRequests.count { it.status.equals(filter, ignoreCase = true) }
                                    }
                                    Text(
                                        text = "${filter.replace('_', ' ')} ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleLight,
                                    selectedLabelColor = PurplePrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // REQUESTS LIST
        if (filteredRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No applications in '${selectedStatusFilter.replace('_', ' ')}'",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                        Text(
                            text = "New merchant registrations will appear here for verification.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredRequests, key = { it.id }) { req ->
                VerificationRequestItemCard(
                    request = req,
                    onInspect = { selectedRequestForReview = req }
                )
            }
        }
    }

    // INSPECT / REVIEW DETAIL DIALOG
    selectedRequestForReview?.let { req ->
        AlertDialog(
            onDismissRequest = { selectedRequestForReview = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = req.shopName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Application #${req.id}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    StatusPill(status = req.status)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Shop Code Card
                    if (req.shopId.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generated Shop Code:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = req.shopId,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = GreenPrimary
                                )
                            }
                        }
                    }

                    // Merchant Overview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Owner: ${req.ownerName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mobile: ${req.phone}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Category: ${req.category}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("Address: ${req.address}", fontSize = 12.sp, color = Color(0xFF4B5563))
                            Text("GST Number: ${req.gstNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Verification Documents Section
                    Text("Submitted Business Documents", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)

                    // 1. Aadhaar Card Card
                    DocumentDetailCard(
                        title = "1. Aadhaar Card (Masked)",
                        detail = "Aadhaar: ${req.aadhaarNumberMasked.ifBlank { "XXXX-XXXX-XXXX" }}",
                        imageUri = req.aadhaarDocUri.ifBlank { null },
                        complianceNote = "UIDAI compliant: Never stores unmasked Aadhaar."
                    )

                    // 2. PAN Card Card
                    DocumentDetailCard(
                        title = "2. PAN Card",
                        detail = "PAN: ${req.panNumber.ifBlank { "Not provided" }}",
                        imageUri = req.panDocUri.ifBlank { null },
                        complianceNote = "Statutory 10-char format verified."
                    )

                    // 3. GST Registration Certificate
                    DocumentDetailCard(
                        title = "3. GST Registration Certificate",
                        detail = "GSTIN: ${req.gstCertificateGstin.ifBlank { req.gstNumber }}",
                        imageUri = req.gstCertificateDocUri.ifBlank { null },
                        complianceNote = "Matched against Step 1 GSTIN."
                    )

                    // Notes from previous review
                    if (req.documentsRequiredNotes.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = WarningBg)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Admin Clarification Notes:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarningAmber)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(req.documentsRequiredNotes, fontSize = 12.sp, color = TextDark)
                            }
                        }
                    }

                    if (req.rejectionReason.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Rejection Reason:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(req.rejectionReason, fontSize = 12.sp, color = TextDark)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (req.status != ShopVerificationRequest.STATUS_APPROVED) {
                        Button(
                            onClick = { showApproveConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showRequestDocsDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            border = BorderStroke(1.dp, WarningAmber),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Request Docs", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = { selectedRequestForReview = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            },
            dismissButton = {
                if (req.status != ShopVerificationRequest.STATUS_APPROVED) {
                    TextButton(onClick = { selectedRequestForReview = null }) {
                        Text("Close", color = TextMuted)
                    }
                }
            }
        )
    }

    // DIALOG: APPROVE APPLICATION CONFIRMATION
    if (showApproveConfirmDialog && selectedRequestForReview != null) {
        val req = selectedRequestForReview!!
        AlertDialog(
            onDismissRequest = { showApproveConfirmDialog = false },
            title = { Text("Approve Shopkeeper Application", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to approve '${req.shopName}'? This will activate the shopkeeper account and grant immediate Dashboard access.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = adminApprovalNotesInput,
                        onValueChange = { adminApprovalNotesInput = it },
                        label = { Text("Approval Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notes = adminApprovalNotesInput.ifBlank { "Approved by Admin ${currentProfile.loginId}" }
                        coroutineScope.launch {
                            val res = repository.approveShopVerification(req.id, notes)
                            showApproveConfirmDialog = false
                            selectedRequestForReview = null
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Shop '${req.shopName}' approved & activated!")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("Approval failed: ${err.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Confirm Approval")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG: REQUEST CORRECTED DOCUMENTS
    if (showRequestDocsDialog && selectedRequestForReview != null) {
        val req = selectedRequestForReview!!
        AlertDialog(
            onDismissRequest = { showRequestDocsDialog = false },
            title = { Text("Request Corrected Documents", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify what needs correction. The applicant will see this message upon logging in and can re-upload their documents.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = requestDocsNotesInput,
                        onValueChange = { requestDocsNotesInput = it },
                        label = { Text("Correction Notes (e.g., GST Certificate blur)") },
                        placeholder = { Text("Please re-upload a clear copy of your GST registration certificate.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notes = requestDocsNotesInput.ifBlank { "Please re-upload clearer copies of your business documents." }
                        coroutineScope.launch {
                            val res = repository.requestCorrectedDocuments(req.id, notes)
                            showRequestDocsDialog = false
                            selectedRequestForReview = null
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Correction request sent to applicant.")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("Failed: ${err.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDocsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG: REJECT APPLICATION
    if (showRejectDialog && selectedRequestForReview != null) {
        val req = selectedRequestForReview!!
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Decline Application", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter reason for declining '${req.shopName}'. The applicant will be able to review this notice.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("GSTIN invalid or address does not match municipal directory.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectionReasonInput.ifBlank { "Application declined by FlashDrop Admins due to verification criteria." }
                        coroutineScope.launch {
                            val res = repository.rejectShopVerification(req.id, reason)
                            showRejectDialog = false
                            selectedRequestForReview = null
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Application declined.")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("Failed: ${err.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VerificationRequestItemCard(
    request: ShopVerificationRequest,
    onInspect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("verification_request_item_${request.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = request.shopName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${request.ownerName} • ${request.category}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                StatusPill(status = request.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${request.shopId.ifBlank { "Pending ID" }}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary
                )

                Text(
                    text = "Tap to review docs →",
                    fontSize = 11.sp,
                    color = PurplePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DocumentDetailCard(
    title: String,
    detail: String,
    imageUri: String?,
    complianceNote: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                if (imageUri != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("UPLOADED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(detail, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF374151))
            Spacer(modifier = Modifier.height(2.dp))
            Text(complianceNote, fontSize = 10.sp, color = TextMuted)

            if (imageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bgColor, textColor, label) = when (status) {
        ShopVerificationRequest.STATUS_APPROVED -> Triple(GreenLight, GreenPrimary, "APPROVED")
        ShopVerificationRequest.STATUS_PENDING_REVIEW -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "PENDING")
        ShopVerificationRequest.STATUS_DOCUMENTS_REQUIRED -> Triple(WarningBg, WarningAmber, "DOCS REQ")
        ShopVerificationRequest.STATUS_REJECTED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "DECLINED")
        ShopVerificationRequest.STATUS_SUSPENDED -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "SUSPENDED")
        ShopVerificationRequest.STATUS_SUBMITTED -> Triple(Color(0xFFEDE9FE), PurplePrimary, "SUBMITTED")
        ShopVerificationRequest.STATUS_DRAFT -> Triple(Color(0xFFF3F4F6), Color.Gray, "DRAFT")
        else -> Triple(Color(0xFFF3F4F6), Color.Gray, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
