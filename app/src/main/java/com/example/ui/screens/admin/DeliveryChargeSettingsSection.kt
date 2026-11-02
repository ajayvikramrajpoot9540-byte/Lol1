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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryConfig
import com.example.data.model.DistanceTier
import com.example.data.repository.ApnaBazRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.components.StockToggleSwitch
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * Dedicated Delivery Charge Settings section in Admin Dashboard.
 * Controlled exclusively by the two authorized Main Admins: Pranjal Rajpoot (PR12) and Chanchal Didi (CD12).
 */
@Composable
fun DeliveryChargeSettingsSection(
    repository: ApnaBazRepository,
    deliveryConfig: DeliveryConfig,
    ownerName: String,
    ownerId: String,
    ownerTitle: String,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val isMainAdmin = repository.isAuthorizedMainAdmin()

    // Policy Selection State
    var selectedPolicy by remember(deliveryConfig) { mutableStateOf(deliveryConfig.activePolicy) }
    var minOrderThresholdInput by remember(deliveryConfig) {
        mutableStateOf(deliveryConfig.minOrderThreshold.toInt().toString())
    }
    var isManualFreeDeliverySwitchOn by remember(deliveryConfig) {
        mutableStateOf(deliveryConfig.isManualFreeDeliveryOn)
    }

    // Distance Tier Rate States (Default: 0-2 km: ₹10, >2-5 km: ₹20, >5-10 km: ₹40, >10 km: ₹70)
    val tiers = deliveryConfig.distanceTiers
    var tier1ChargeInput by remember(deliveryConfig) {
        mutableStateOf(tiers.getOrNull(0)?.charge?.toInt()?.toString() ?: "10")
    }
    var tier2ChargeInput by remember(deliveryConfig) {
        mutableStateOf(tiers.getOrNull(1)?.charge?.toInt()?.toString() ?: "20")
    }
    var tier3ChargeInput by remember(deliveryConfig) {
        mutableStateOf(tiers.getOrNull(2)?.charge?.toInt()?.toString() ?: "40")
    }
    var tier4ChargeInput by remember(deliveryConfig) {
        mutableStateOf(tiers.getOrNull(3)?.charge?.toInt()?.toString() ?: "70")
    }

    // Maximum Permitted Delivery Distance State
    var maxDistanceInput by remember(deliveryConfig) {
        mutableStateOf(deliveryConfig.maxDeliveryDistanceKm.toInt().toString())
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Equal Main Admin Governance Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_delivery_governance_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE)),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EQUAL MAIN ADMIN GOVERNANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pranjal Rajpoot (PR12) & Chanchal Didi (CD12)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Both Main Admins possess equal permissions to configure distance rates, delivery boundaries, and free-delivery policies. Non-admin accounts (Customers & Shopkeepers) are strictly denied write access.",
                            fontSize = 11.sp,
                            color = TextDark.copy(alpha = 0.8f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Current Session: $ownerName ($ownerId) • Config Version #${deliveryConfig.version}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6D28D9)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6D28D9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Access Control Verification Check
        if (!isMainAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFDC2626))
                        Column {
                            Text(
                                text = "Permission Denied: Read-Only Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "Only authorized Main Admins (Pranjal Rajpoot & Chanchal Didi) can modify delivery charges or save policies.",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        // 2. Active Free-Delivery Policy Card (3 Configurable Policies)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("free_delivery_policy_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Free Delivery Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Select active policy applied across single & multi-shop orders",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Surface(
                            color = GreenLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Active: $selectedPolicy",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Policy Option 1: Minimum-Order Threshold
                    PolicySelectionOption(
                        title = "1. Free Delivery on Minimum Order",
                        description = "Applies free delivery when server-validated merchandise subtotal (after product discounts) reaches the editable threshold.",
                        isSelected = selectedPolicy == "MINIMUM_ORDER",
                        onSelect = { selectedPolicy = "MINIMUM_ORDER" }
                    ) {
                        if (selectedPolicy == "MINIMUM_ORDER") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = minOrderThresholdInput,
                                onValueChange = { minOrderThresholdInput = it },
                                label = { Text("Minimum Order Threshold (₹)") },
                                placeholder = { Text("199") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("min_order_threshold_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenPrimary,
                                    focusedLabelColor = GreenPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Default: ₹199. Free delivery applies when subtotal >= ₹${minOrderThresholdInput.ifBlank { "199" }}.",
                                fontSize = 11.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Policy Option 2: No Free Delivery
                    PolicySelectionOption(
                        title = "2. No Free Delivery (Distance Rates Always Apply)",
                        description = "All orders are charged their exact distance-based delivery fee regardless of order size or merchandise subtotal.",
                        isSelected = selectedPolicy == "NO_FREE_DELIVERY",
                        onSelect = { selectedPolicy = "NO_FREE_DELIVERY" }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Policy Option 3: Manual Free Delivery Mode (Admin Switch)
                    PolicySelectionOption(
                        title = "3. Manual Free Delivery Mode",
                        description = "Main Admin controlled master switch to immediately turn free delivery ON or OFF across the entire marketplace.",
                        isSelected = selectedPolicy == "MANUAL",
                        onSelect = { selectedPolicy = "MANUAL" }
                    ) {
                        if (selectedPolicy == "MANUAL") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isManualFreeDeliverySwitchOn) GreenLight else Color(0xFFF3F4F6))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isManualFreeDeliverySwitchOn) "Manual Free Delivery: ON" else "Manual Free Delivery: OFF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isManualFreeDeliverySwitchOn) GreenPrimary else TextDark
                                    )
                                    Text(
                                        text = if (isManualFreeDeliverySwitchOn) "All customers currently receive 100% Free Delivery" else "Standard distance-based charges are currently applied",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                StockToggleSwitch(
                                    checked = isManualFreeDeliverySwitchOn,
                                    onCheckedChange = { isManualFreeDeliverySwitchOn = it },
                                    enabled = isMainAdmin,
                                    modifier = Modifier.testTag("manual_free_delivery_switch")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Distance-Based Delivery Rates Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("distance_delivery_charges_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Distance-Based Delivery Rates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Rates per distinct shop in Mahoba (Default: ₹10, ₹20, ₹40, ₹70)",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Icon(Icons.Default.Route, contentDescription = null, tint = OrangeSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tier 1: 0 - 2 km (Default ₹10)
                    DistanceRateTierRow(
                        tierLabel = "0 to 2 km",
                        description = "Short-distance local neighborhood delivery",
                        value = tier1ChargeInput,
                        onValueChange = { tier1ChargeInput = it },
                        defaultHint = "Default: ₹10",
                        tag = "tier_1_charge_input"
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 10.dp))

                    // Tier 2: > 2 - 5 km (Default ₹20)
                    DistanceRateTierRow(
                        tierLabel = "> 2 km up to 5 km",
                        description = "Mid-distance city cross-town delivery",
                        value = tier2ChargeInput,
                        onValueChange = { tier2ChargeInput = it },
                        defaultHint = "Default: ₹20",
                        tag = "tier_2_charge_input"
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 10.dp))

                    // Tier 3: > 5 - 10 km (Default ₹40)
                    DistanceRateTierRow(
                        tierLabel = "> 5 km up to 10 km",
                        description = "Outer city and peri-urban delivery",
                        value = tier3ChargeInput,
                        onValueChange = { tier3ChargeInput = it },
                        defaultHint = "Default: ₹40",
                        tag = "tier_3_charge_input"
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 10.dp))

                    // Tier 4: > 10 km (Default ₹70)
                    DistanceRateTierRow(
                        tierLabel = "> 10 km (up to max limit)",
                        description = "Extended peripheral and rural fringe delivery",
                        value = tier4ChargeInput,
                        onValueChange = { tier4ChargeInput = it },
                        defaultHint = "Default: ₹70",
                        tag = "tier_4_charge_input"
                    )
                }
            }
        }

        // 4. Maximum Permitted Delivery Distance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("max_delivery_distance_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Maximum Permitted Delivery Distance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Shops exceeding this distance limit cannot be delivered to customers",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = maxDistanceInput,
                        onValueChange = { maxDistanceInput = it },
                        label = { Text("Maximum Distance Limit (km)") },
                        placeholder = { Text("15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("max_delivery_distance_input"),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = { Text("km", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Default: 15 km. If any shop in the customer's cart exceeds this limit, checkout is blocked with a clear explanation.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 5. Multi-Shop Order Delivery Logic Transparency Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Multi-Shop Calculation & Duplicate Prevention",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangeSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Distinct Shops: Delivery fee is calculated separately for each distinct shop using verified route distances.\n" +
                                "• Duplicate Prevention: Multiple products from the same shop incur only ONE delivery charge for that shop.\n" +
                                "• Consistent Policy: Free delivery discounts apply across all participating shops proportionally without duplicate fees.",
                        fontSize = 11.sp,
                        color = TextDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Error message banner if validation fails
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 6. Save & Apply Delivery Settings Button
        item {
            Button(
                onClick = {
                    errorMessage = null
                    val t1 = tier1ChargeInput.toDoubleOrNull()
                    val t2 = tier2ChargeInput.toDoubleOrNull()
                    val t3 = tier3ChargeInput.toDoubleOrNull()
                    val t4 = tier4ChargeInput.toDoubleOrNull()
                    val maxDist = maxDistanceInput.toDoubleOrNull()
                    val minThreshold = minOrderThresholdInput.toDoubleOrNull()

                    if (t1 == null || t1 < 0.0 || t2 == null || t2 < 0.0 || t3 == null || t3 < 0.0 || t4 == null || t4 < 0.0) {
                        errorMessage = "All distance rate charges must be valid, non-negative numbers."
                        return@Button
                    }
                    if (maxDist == null || maxDist <= 0.0 || maxDist > 50.0) {
                        errorMessage = "Maximum delivery distance must be between 1.0 km and 50.0 km."
                        return@Button
                    }
                    if (minThreshold == null || minThreshold < 0.0) {
                        errorMessage = "Minimum order threshold must be a non-negative amount."
                        return@Button
                    }

                    val updatedTiers = listOf(
                        DistanceTier(minDistanceKm = 0.0, maxDistanceKm = 2.0, charge = t1, label = "0 – 2 km"),
                        DistanceTier(minDistanceKm = 2.0, maxDistanceKm = 5.0, charge = t2, label = "2 – 5 km"),
                        DistanceTier(minDistanceKm = 5.0, maxDistanceKm = 10.0, charge = t3, label = "5 – 10 km"),
                        DistanceTier(minDistanceKm = 10.0, maxDistanceKm = maxDist, charge = t4, label = "> 10 km")
                    )

                    val updatedConfig = deliveryConfig.copy(
                        distanceTiers = updatedTiers,
                        maxDeliveryDistanceKm = maxDist,
                        activePolicy = selectedPolicy,
                        minOrderThreshold = minThreshold,
                        isManualFreeDeliveryOn = isManualFreeDeliverySwitchOn,
                        defaultDeliveryFee = t1,
                        freeDeliveryThreshold = minThreshold
                    )

                    isSaving = true
                    val result = repository.updateGlobalDeliveryConfig(updatedConfig)
                    isSaving = false

                    if (result.isSuccess) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Delivery charges & policy saved successfully by $ownerName")
                        }
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update delivery settings"
                    }
                },
                enabled = isMainAdmin && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_delivery_settings_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "Saving Settings..." else "Save Delivery Configuration ($ownerId Auth)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PolicySelectionOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenLight.copy(alpha = 0.5f) else Color(0xFFF9FAFB)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) GreenPrimary else Color(0xFFE5E7EB)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = GreenPrimary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSelected) GreenPrimary else TextDark
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            content?.invoke()
        }
    }
}

@Composable
private fun DistanceRateTierRow(
    tierLabel: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    defaultHint: String,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tierLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDark
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = defaultHint,
                fontSize = 10.sp,
                color = OrangeSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            prefix = { Text("₹", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(100.dp)
                .testTag(tag),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary
            )
        )
    }
}
