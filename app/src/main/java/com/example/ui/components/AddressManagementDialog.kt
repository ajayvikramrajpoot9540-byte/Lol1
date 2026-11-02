package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DeliveryAddress
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * Address Management Dialog opened when clicking the location/address section
 * on Home Screen, during Checkout, or in Customer Profile.
 */
@Composable
fun AddressManagementDialog(
    repository: FlashDropRepository,
    onDismiss: () -> Unit,
    onOpenMap: () -> Unit = {},
    onAddressSelected: ((DeliveryAddress) -> Unit)? = null
) {
    val profile by repository.currentProfile.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
    var addressToDelete by remember { mutableStateOf<DeliveryAddress?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(GreenLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Delivery Addresses",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Choose or add address for deliveries in Mahoba",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_address_management_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Current Selected Location Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.55f)),
                            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CURRENT SELECTED LOCATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = GreenPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.address.ifBlank { "Subhash Chowk, Chhatarpur Road, Mahoba - 210427" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Locality: ${profile.locality.ifBlank { "Mahoba" }} • PIN: ${profile.pinCode.ifBlank { "210427" }}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Add New Address Button & Map Option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    editingAddress = null
                                    showAddEditDialog = true
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("btn_add_new_address"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Address", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenMap()
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .testTag("btn_pinpoint_map"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Map GPS", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }

                        // Saved Addresses Header
                        Text(
                            text = "Saved Addresses (${profile.savedAddresses.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (profile.savedAddresses.isEmpty()) {
                            // Empty state
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundWarm)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No saved addresses yet",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Add your home, work, or friend's address for faster checkout in Mahoba.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            profile.savedAddresses.forEach { addr ->
                                SavedAddressCard(
                                    address = addr,
                                    isSelected = addr.isDefault,
                                    onSelectAsDefault = {
                                        repository.setDefaultDeliveryAddress(addr.id)
                                        onAddressSelected?.invoke(addr)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Default address set to ${addr.label.ifBlank { "Address" }}")
                                        }
                                    },
                                    onEdit = {
                                        editingAddress = addr
                                        showAddEditDialog = true
                                    },
                                    onDelete = if (profile.savedAddresses.size > 1) {
                                        { addressToDelete = addr }
                                    } else null
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (addressToDelete != null) {
        val target = addressToDelete!!
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("Delete Address?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete this ${target.label} address (${target.formatDisplay()})? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = repository.deleteDeliveryAddress(target.id)
                        addressToDelete = null
                        if (result.isSuccess) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Address deleted.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Add / Edit Address Form Modal
    if (showAddEditDialog) {
        AddNewAddressDialog(
            initialAddress = editingAddress,
            initialName = profile.name.takeIf { !it.equals("Guest", ignoreCase = true) } ?: "",
            initialPhone = profile.phone,
            onDismiss = {
                showAddEditDialog = false
                editingAddress = null
            },
            onSave = { updatedOrNewAddress ->
                val result = repository.saveDeliveryAddress(updatedOrNewAddress)
                showAddEditDialog = false
                editingAddress = null
                if (result.isSuccess) {
                    onAddressSelected?.invoke(updatedOrNewAddress)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Address saved successfully!")
                    }
                }
            },
            onOpenMap = {
                showAddEditDialog = false
                onDismiss()
                onOpenMap()
            }
        )
    }
}

/**
 * Clean Card rendering an individual saved address in the management list.
 */
@Composable
fun SavedAddressCard(
    address: DeliveryAddress,
    isSelected: Boolean,
    onSelectAsDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectAsDefault() }
            .testTag("saved_address_card_${address.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenLight.copy(alpha = 0.35f) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) GreenPrimary else BorderLight
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Label Badge + Default Tag + Radio selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AddressLabelBadge(label = address.label.ifBlank { "Home" })
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(GreenPrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelectAsDefault,
                    colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Name & Mobile
            if (address.name.isNotBlank() || address.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (address.name.isNotBlank()) {
                        Text(
                            text = address.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                    }
                    if (address.name.isNotBlank() && address.phone.isNotBlank()) {
                        Text(text = " • ", fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                    if (address.phone.isNotBlank()) {
                        Text(
                            text = "${address.countryCode} ${address.phone}",
                            fontSize = 13.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Full Formatted Address
            Text(
                text = address.formatDisplay(),
                fontSize = 12.sp,
                color = TextDark,
                lineHeight = 17.sp
            )

            if (address.landmark.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Landmark: ${address.landmark}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))

            // Actions row: Set as default, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!address.isDefault) {
                    TextButton(
                        onClick = onSelectAsDefault,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use This Address", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        text = "Selected for delivery",
                        fontSize = 11.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("edit_address_${address.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("delete_address_${address.id}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small badge showing address label with matching icon.
 */
@Composable
fun AddressLabelBadge(label: String) {
    val (icon, bgColor, tintColor) = when (label.trim().lowercase()) {
        "work" -> Triple(Icons.Default.Business, Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        "friend/parent's home", "friend", "parent" -> Triple(Icons.Default.Group, Color(0xFFFAF5FF), Color(0xFF7E22CE))
        "other" -> Triple(Icons.Default.Place, Color(0xFFF3F4F6), Color(0xFF4B5563))
        else -> Triple(Icons.Default.Home, Color(0xFFECFDF5), GreenPrimary)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label.ifBlank { "Home" },
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = tintColor
        )
    }
}

/**
 * Redesigned Add New Address Form Modal.
 * Implements all specifications from the user's PDF:
 * A. Name* (Customer's name, required)
 * B. Address* (Flat / House Number*, Building / Block, Street / Area*)
 * C. Landmark (Optional, with example)
 * D. Address Label* (Home, Work, Friend/Parent's Home, Other - single selection)
 * E. Mobile Number* (Country code selector default +91, 10-digit Indian mobile validation)
 * F. Default Address (Checkbox/toggle: Set as default address)
 * G. Save Address Button (bottom visible, validates fields, shows errors, saves, success feedback)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNewAddressDialog(
    initialAddress: DeliveryAddress?,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (DeliveryAddress) -> Unit,
    onOpenMap: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialAddress?.name?.ifBlank { initialName } ?: initialName) }
    var houseNumber by remember { mutableStateOf(initialAddress?.houseNumber ?: "") }
    var buildingBlock by remember { mutableStateOf(initialAddress?.buildingBlock ?: "") }
    var street by remember { mutableStateOf(initialAddress?.street ?: "") }
    var area by remember { mutableStateOf(initialAddress?.area ?: "Subhash Chowk") }
    var landmark by remember { mutableStateOf(initialAddress?.landmark ?: "") }
    var selectedLabel by remember { mutableStateOf(initialAddress?.label?.ifBlank { "Home" } ?: "Home") }
    var countryCode by remember { mutableStateOf(initialAddress?.countryCode?.ifBlank { "+91" } ?: "+91") }
    var phone by remember { mutableStateOf(initialAddress?.phone?.ifBlank { initialPhone } ?: initialPhone) }
    var isDefault by remember { mutableStateOf(initialAddress?.isDefault ?: false) }

    var city by remember { mutableStateOf(initialAddress?.city?.ifBlank { "Mahoba" } ?: "Mahoba") }
    var state by remember { mutableStateOf(initialAddress?.state?.ifBlank { "Uttar Pradesh" } ?: "Uttar Pradesh") }
    var pinCode by remember { mutableStateOf(initialAddress?.pinCode?.ifBlank { "210427" } ?: "210427") }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var houseError by remember { mutableStateOf<String?>(null) }
    var streetError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var showAttemptedValidation by remember { mutableStateOf(false) }

    val popularLocalities = listOf(
        "Subhash Chowk", "Alha Chowk", "Gandhi Nagar",
        "Bhatipura", "Civil Lines", "Chhatarpur Road"
    )

    val labelOptions = listOf(
        AddressLabelOption("Home", Icons.Default.Home),
        AddressLabelOption("Work", Icons.Default.Business),
        AddressLabelOption("Friend/Parent's Home", Icons.Default.Group),
        AddressLabelOption("Other", Icons.Default.Place)
    )

    fun validateForm(): Boolean {
        var isValid = true

        if (name.trim().isBlank()) {
            nameError = "Customer's name is required"
            isValid = false
        } else {
            nameError = null
        }

        if (houseNumber.trim().isBlank()) {
            houseError = "Flat / House number is required"
            isValid = false
        } else {
            houseError = null
        }

        if (street.trim().isBlank() && area.trim().isBlank()) {
            streetError = "Street / Area is required"
            isValid = false
        } else {
            streetError = null
        }

        val digits = phone.filter { it.isDigit() }
        if (digits.length != 10) {
            phoneError = "Please enter a valid 10-digit Indian mobile number"
            isValid = false
        } else if (!digits.startsWith("6") && !digits.startsWith("7") && !digits.startsWith("8") && !digits.startsWith("9")) {
            phoneError = "Indian mobile number must start with 6, 7, 8, or 9"
            isValid = false
        } else {
            phoneError = null
        }

        return isValid
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialAddress == null) "Add New Address" else "Edit Address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Deliveries in Mahoba, Uttar Pradesh",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_address_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // FIELD A: Customer Name*
                    Column {
                        Text(
                            text = "Name *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (showAttemptedValidation) {
                                    nameError = if (it.trim().isBlank()) "Customer's name is required" else null
                                }
                            },
                            placeholder = { Text("Customer's full name", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) Text(nameError!!, color = ErrorRed, fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    // FIELD B: Address*
                    Column {
                        Text(
                            text = "Address *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Flat / House Number*
                        OutlinedTextField(
                            value = houseNumber,
                            onValueChange = {
                                houseNumber = it
                                if (showAttemptedValidation) {
                                    houseError = if (it.trim().isBlank()) "Flat / House number is required" else null
                                }
                            },
                            label = { Text("Flat / House Number *", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Flat 3B / House No. 42", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            isError = houseError != null,
                            supportingText = {
                                if (houseError != null) Text(houseError!!, color = ErrorRed, fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_house_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Building / Block
                        OutlinedTextField(
                            value = buildingBlock,
                            onValueChange = { buildingBlock = it },
                            label = { Text("Building / Block (Optional)", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Block C, Saraswati Niwas", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Apartment, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_building_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Street / Area*
                        OutlinedTextField(
                            value = street,
                            onValueChange = {
                                street = it
                                if (showAttemptedValidation) {
                                    streetError = if (it.trim().isBlank() && area.trim().isBlank()) "Street / Area is required" else null
                                }
                            },
                            label = { Text("Street / Area *", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Station Road, Subhash Chowk", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            isError = streetError != null,
                            supportingText = {
                                if (streetError != null) Text(streetError!!, color = ErrorRed, fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_street_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        // Quick Locality Chips
                        Text(
                            text = "Quick select Mahoba locality:",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            popularLocalities.forEach { loc ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (area == loc) GreenLight else BackgroundWarm)
                                        .border(
                                            1.dp,
                                            if (area == loc) GreenPrimary else BorderLight,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            area = loc
                                            if (street.isBlank()) street = loc
                                            if (showAttemptedValidation) streetError = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = loc,
                                        fontSize = 11.sp,
                                        color = if (area == loc) GreenPrimary else TextDark,
                                        fontWeight = if (area == loc) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // FIELD C: Landmark (Optional)
                    Column {
                        Text(
                            text = "Landmark",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = landmark,
                            onValueChange = { landmark = it },
                            placeholder = { Text("e.g. Near Post Office, School, Hospital", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.NearMe, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_landmark_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    // FIELD D: Address Label*
                    Column {
                        Text(
                            text = "Address Label *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Select one label for this address",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_label_group")
                        ) {
                            labelOptions.forEach { opt ->
                                val isSelected = selectedLabel.equals(opt.label, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) GreenPrimary else BackgroundWarm)
                                        .border(
                                            1.dp,
                                            if (isSelected) GreenPrimary else BorderLight,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedLabel = opt.label }
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                        .testTag("address_label_${opt.label.lowercase().replace("[^a-z]".toRegex(), "_")}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = opt.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else TextDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = opt.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // FIELD E: Mobile Number*
                    Column {
                        Text(
                            text = "Mobile Number *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Country code selector box (Default +91)
                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BackgroundWarm)
                                    .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🇮🇳 $countryCode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                }
                            }

                            // Mobile number input
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }
                                    if (digitsOnly.length <= 10) {
                                        phone = digitsOnly
                                    }
                                    if (showAttemptedValidation) {
                                        phoneError = if (phone.length == 10 && (phone.startsWith("6") || phone.startsWith("7") || phone.startsWith("8") || phone.startsWith("9"))) {
                                            null
                                        } else {
                                            "Enter valid 10-digit Indian mobile number"
                                        }
                                    }
                                },
                                placeholder = { Text("10-digit mobile number", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = phoneError != null,
                                supportingText = {
                                    if (phoneError != null) Text(phoneError!!, color = ErrorRed, fontSize = 11.sp)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("address_phone_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }
                    }

                    // City & Pin Code Info (Fixed for Mahoba)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { pinCode = it },
                            label = { Text("PIN Code", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // FIELD F: Default Address (Toggle / Switch)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundWarm)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) {
                                isDefault = !isDefault
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("default_address_toggle_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Set as default address",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Make this the customer's default delivery address for all orders.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 15.sp
                            )
                        }

                        StockToggleSwitch(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it }
                        )
                    }

                    // Optional Map Pinpoint Option
                    OutlinedButton(
                        onClick = onOpenMap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_pinpoint_on_map_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pinpoint Exact Location on GPS Map", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(color = BorderLight)

                // FIELD 3: Save Address Button at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            showAttemptedValidation = true
                            if (validateForm()) {
                                val finalizedAddress = (initialAddress ?: DeliveryAddress()).copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    countryCode = countryCode.trim().ifBlank { "+91" },
                                    label = selectedLabel.trim().ifBlank { "Home" },
                                    houseNumber = houseNumber.trim(),
                                    buildingBlock = buildingBlock.trim(),
                                    street = street.trim(),
                                    area = area.trim().ifBlank { "Subhash Chowk" },
                                    landmark = landmark.trim(),
                                    city = city.trim().ifBlank { "Mahoba" },
                                    state = state.trim().ifBlank { "Uttar Pradesh" },
                                    pinCode = pinCode.trim().ifBlank { "210427" },
                                    isDefault = isDefault
                                )
                                onSave(finalizedAddress)
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("save_address_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text(
                            text = if (initialAddress == null) "Save Address" else "Update Address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

data class AddressLabelOption(
    val label: String,
    val icon: ImageVector
)
