package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySchedule
import com.example.data.model.ShopScheduleUtils
import com.example.data.model.WeeklySchedule
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.util.ScreenType
import com.example.ui.util.rememberScreenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTimingScheduleScreen(
    repository: FlashDropRepository,
    shopIdOverride: String? = null,
    onBack: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val shops by repository.shops.collectAsState()
    val schedules by repository.shopSchedules.collectAsState()

    val screenType = rememberScreenType()
    val isSmallMobile = screenType == ScreenType.SMALL_MOBILE

    val targetShopId = shopIdOverride
        ?: profile.shopId.takeIf { it.isNotBlank() }
        ?: "sharma_general"

    val currentShop = remember(shops, targetShopId) {
        shops.find { it.id == targetShopId } ?: shops.firstOrNull()
    }

    val initialSchedule = remember(targetShopId, schedules) {
        repository.getShopSchedule(targetShopId)
    }

    var masterOpeningTime by remember(initialSchedule) {
        mutableStateOf(initialSchedule.defaultOpeningTime)
    }
    var masterClosingTime by remember(initialSchedule) {
        mutableStateOf(initialSchedule.defaultClosingTime)
    }
    var autoOpeningEnabled by remember(initialSchedule) {
        mutableStateOf(initialSchedule.autoOpeningEnabled)
    }
    var autoClosingEnabled by remember(initialSchedule) {
        mutableStateOf(initialSchedule.autoClosingEnabled)
    }
    var manualOverride by remember(initialSchedule) {
        mutableStateOf(initialSchedule.manualOverride)
    }
    var isTemporarilyClosed by remember(initialSchedule) {
        mutableStateOf(initialSchedule.isTemporarilyClosed)
    }
    var editableDays by remember(initialSchedule) {
        mutableStateOf(initialSchedule.days)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun buildCurrentDraftSchedule(): WeeklySchedule {
        return initialSchedule.copy(
            shopId = targetShopId,
            defaultOpeningTime = masterOpeningTime,
            defaultClosingTime = masterClosingTime,
            days = editableDays,
            autoScheduleEnabled = autoOpeningEnabled || autoClosingEnabled,
            autoOpeningEnabled = autoOpeningEnabled,
            autoClosingEnabled = autoClosingEnabled,
            manualOverride = manualOverride,
            isTemporarilyClosed = isTemporarilyClosed
        )
    }

    var liveStatus by remember(
        masterOpeningTime,
        masterClosingTime,
        autoOpeningEnabled,
        autoClosingEnabled,
        manualOverride,
        isTemporarilyClosed,
        editableDays
    ) {
        mutableStateOf(ShopScheduleUtils.evaluateLiveStatus(buildCurrentDraftSchedule()))
    }

    var currentIstClock by remember {
        mutableStateOf(ShopScheduleUtils.calculateExact30MinForward(offsetMinutes = 0))
    }

    // Periodically refresh IST clock and live evaluation every 15 seconds
    LaunchedEffect(
        masterOpeningTime,
        masterClosingTime,
        autoOpeningEnabled,
        autoClosingEnabled,
        manualOverride,
        isTemporarilyClosed,
        editableDays
    ) {
        while (true) {
            currentIstClock = ShopScheduleUtils.calculateExact30MinForward(offsetMinutes = 0)
            liveStatus = ShopScheduleUtils.evaluateLiveStatus(buildCurrentDraftSchedule())
            delay(15_000L)
        }
    }

    fun persistScheduleNow(toastMessage: String? = null) {
        val updated = buildCurrentDraftSchedule()
        val saved = repository.saveShopSchedule(updated)
        liveStatus = ShopScheduleUtils.evaluateLiveStatus(updated)
        if (toastMessage != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (saved) toastMessage else "Permission denied: You can only modify your own shop's schedule."
                )
            }
        }
    }

    // Time picker dialog state:
    // editingDayIndex == -2 means editing Master Shop Opening/Closing Time
    // editingDayIndex in 0..6 means editing a specific day in the Weekly Schedule
    var editingDayIndex by remember { mutableIntStateOf(-1) }
    var isEditingOpeningTime by remember { mutableStateOf(true) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Shop Schedule & Auto Opening/Closing",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isSmallMobile) 14.sp else 16.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentShop?.name ?: "Local Shop"} • IST ($currentIstClock)",
                                fontSize = 11.sp,
                                color = OrangeSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
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
                .background(BackgroundWarm)
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = if (isSmallMobile) 12.dp else 16.dp, vertical = 12.dp)
                    .testTag("shop_timing_schedule_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Live Shop Availability & Accurate Delivery Estimate Banner (IST)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (liveStatus.isOpen) GreenLight.copy(alpha = 0.6f) else Color(0xFFFEE2E2)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (liveStatus.isOpen) GreenPrimary.copy(alpha = 0.4f) else Color(0xFFFCA5A5)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (liveStatus.isOpen) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (liveStatus.isOpen) "OPEN" else "CLOSED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "IST Live Status (${liveStatus.currentDay})",
                                            fontSize = 11.sp,
                                            color = TextDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = liveStatus.statusLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (liveStatus.isOpen) GreenPrimary else Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = "Expected Delivery Estimate (+30 mins): ${liveStatus.expectedDeliveryEstimate}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Schedule Management Table (Section 2 of PDF)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Schedule Management Table",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Default working hours & automatic opening/closing controls (Asia/Kolkata)",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Table Header: Setting | Current / Default | Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Setting",
                                    modifier = Modifier.weight(1.35f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Default / Value",
                                    modifier = Modifier.weight(1.15f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Action",
                                    modifier = Modifier.weight(1.1f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Row 1: Shop Opening Time | 10:00 AM | Edit
                            ScheduleSettingTableRow(
                                settingName = "Shop Opening Time",
                                valueText = masterOpeningTime,
                                valueColor = GreenPrimary,
                                actionContent = {
                                    OutlinedButton(
                                        onClick = {
                                            editingDayIndex = -2
                                            isEditingOpeningTime = true
                                            showTimePickerDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("edit_master_opening_time")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = GreenPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 2: Shop Closing Time | 10:00 PM | Edit
                            ScheduleSettingTableRow(
                                settingName = "Shop Closing Time",
                                valueText = masterClosingTime,
                                valueColor = OrangeSecondary,
                                actionContent = {
                                    OutlinedButton(
                                        onClick = {
                                            editingDayIndex = -2
                                            isEditingOpeningTime = false
                                            showTimePickerDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("edit_master_closing_time")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = OrangeSecondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangeSecondary)
                                    }
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 3: Automatic Opening | ON | ON/OFF
                            ScheduleSettingTableRow(
                                settingName = "Automatic Opening",
                                subtitle = if (autoOpeningEnabled) "Auto-opens at $masterOpeningTime" else "Requires manual opening",
                                valueText = if (autoOpeningEnabled) "ON" else "OFF",
                                valueColor = if (autoOpeningEnabled) GreenPrimary else Color(0xFFDC2626),
                                actionContent = {
                                    Switch(
                                        checked = autoOpeningEnabled,
                                        onCheckedChange = { enabled ->
                                            autoOpeningEnabled = enabled
                                            if (!enabled && manualOverride == null) {
                                                // When Auto Opening is switched OFF, do not auto-open until manually opened
                                                manualOverride = false
                                            } else if (enabled && manualOverride == false) {
                                                manualOverride = null
                                            }
                                            persistScheduleNow(
                                                if (enabled) "Automatic Opening turned ON" else "Automatic Opening turned OFF (Manual open required)"
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = GreenPrimary,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier.testTag("auto_opening_switch")
                                    )
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 4: Automatic Closing | ON | ON/OFF
                            ScheduleSettingTableRow(
                                settingName = "Automatic Closing",
                                subtitle = if (autoClosingEnabled) "Auto-closes at $masterClosingTime" else "Manual closing control",
                                valueText = if (autoClosingEnabled) "ON" else "OFF",
                                valueColor = if (autoClosingEnabled) GreenPrimary else Color(0xFFDC2626),
                                actionContent = {
                                    Switch(
                                        checked = autoClosingEnabled,
                                        onCheckedChange = { enabled ->
                                            autoClosingEnabled = enabled
                                            persistScheduleNow(
                                                if (enabled) "Automatic Closing turned ON" else "Automatic Closing turned OFF (Shop stays open until closed manually)"
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = GreenPrimary,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier.testTag("auto_closing_switch")
                                    )
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 5: Manual Shop Status | Open / Closed | Change
                            val manualStatusLabel = when {
                                isTemporarilyClosed -> "Temp Closed"
                                manualOverride == true -> "Open (Manual)"
                                manualOverride == false -> "Closed (Manual)"
                                liveStatus.isOpen -> "Open"
                                else -> "Closed"
                            }
                            ScheduleSettingTableRow(
                                settingName = "Manual Shop Status",
                                subtitle = if (manualOverride != null) "Manual override active" else "Following schedule",
                                valueText = manualStatusLabel,
                                valueColor = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626),
                                actionContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val newManualOpen = !liveStatus.isOpen
                                                manualOverride = newManualOpen
                                                isTemporarilyClosed = false
                                                persistScheduleNow(
                                                    if (newManualOpen) "Shop manually opened for orders" else "Shop manually closed for orders"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (liveStatus.isOpen) Color(0xFFDC2626) else GreenPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("change_manual_shop_status_button")
                                        ) {
                                            Text(
                                                text = if (liveStatus.isOpen) "Close" else "Open",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        if (manualOverride != null || isTemporarilyClosed) {
                                            IconButton(
                                                onClick = {
                                                    manualOverride = null
                                                    isTemporarilyClosed = false
                                                    persistScheduleNow("Restored to Automatic Schedule")
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("reset_to_automatic_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset to Automatic",
                                                    tint = OrangeSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 6: Current Shop Status | Automatic | View
                            ScheduleSettingTableRow(
                                settingName = "Current Shop Status",
                                subtitle = liveStatus.currentStatusMode,
                                valueText = if (liveStatus.isOpen) "OPEN" else "CLOSED",
                                valueColor = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626),
                                actionContent = {
                                    ViewBadgePill(
                                        text = "View: ${if (liveStatus.isOpen) "Open" else "Closed"}",
                                        isPositive = liveStatus.isOpen
                                    )
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 7: Next Opening Time | Automatic | View
                            ScheduleSettingTableRow(
                                settingName = "Next Opening Time",
                                subtitle = "Automatic",
                                valueText = liveStatus.nextOpeningTime,
                                valueColor = TextDark,
                                actionContent = {
                                    ViewBadgePill(text = "View", isPositive = true)
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Row 8: Next Closing Time | Automatic | View
                            ScheduleSettingTableRow(
                                settingName = "Next Closing Time",
                                subtitle = "Automatic",
                                valueText = liveStatus.nextClosingTime,
                                valueColor = TextDark,
                                actionContent = {
                                    ViewBadgePill(text = "View", isPositive = false)
                                }
                            )
                        }
                    }
                }

                // 3. Custom Weekly Shop Schedule (Section 3 of PDF)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Custom Weekly Shop Schedule",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Edit each day's opening and closing times independently or mark weekly holidays OFF.",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Presets Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editableDays = editableDays.map {
                                            it.copy(
                                                openingTime = masterOpeningTime,
                                                closingTime = masterClosingTime,
                                                isOpen = true
                                            )
                                        }
                                        persistScheduleNow("Applied $masterOpeningTime - $masterClosingTime to all 7 days")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Apply Default ($masterOpeningTime - $masterClosingTime)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        editableDays = editableDays.map {
                                            if (it.dayOfWeek.equals("Sunday", ignoreCase = true)) {
                                                it.copy(isOpen = false)
                                            } else {
                                                it.copy(isOpen = true)
                                            }
                                        }
                                        persistScheduleNow("Sunday set as weekly holiday (OFF)")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Mon–Sat ON, Sunday OFF",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Weekly Table Header: Day | Opening | Closing | Status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Day",
                                    modifier = Modifier.weight(1.1f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Opening",
                                    modifier = Modifier.weight(1.3f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Closing",
                                    modifier = Modifier.weight(1.3f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Status",
                                    modifier = Modifier.weight(1.0f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Table Rows for each day (Monday..Sunday)
                            editableDays.forEachIndexed { index, daySchedule ->
                                WeeklyScheduleRow(
                                    daySchedule = daySchedule,
                                    isToday = daySchedule.dayOfWeek.equals(liveStatus.currentDay, ignoreCase = true),
                                    onEditOpeningTime = {
                                        editingDayIndex = index
                                        isEditingOpeningTime = true
                                        showTimePickerDialog = true
                                    },
                                    onEditClosingTime = {
                                        editingDayIndex = index
                                        isEditingOpeningTime = false
                                        showTimePickerDialog = true
                                    },
                                    onToggleStatus = { newIsOpen ->
                                        editableDays = editableDays.toMutableList().also { list ->
                                            list[index] = daySchedule.copy(isOpen = newIsOpen)
                                        }
                                        persistScheduleNow("${daySchedule.dayOfWeek} set to ${if (newIsOpen) "ON" else "OFF"}")
                                    }
                                )
                                if (index < editableDays.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Save Weekly Schedule Button
                            Button(
                                onClick = {
                                    persistScheduleNow("Weekly schedule saved permanently to Firebase!")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_schedule_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save Weekly Schedule",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. Automatic Opening/Closing & Late-Night Order Rules Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangeSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Smart Schedule & Late-Night Order Rules",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• When Automatic Opening is ON: Your shop automatically opens at its saved opening time.\n" +
                                        "• When Automatic Opening is OFF: Your shop stays closed until you manually open it.\n" +
                                        "• When Automatic Closing is ON: Your shop automatically closes at its saved closing time and disables new customer orders.\n" +
                                        "• When Automatic Closing is OFF: Your shop stays open past closing time until you manually close it.\n" +
                                        "• Late-Night Orders (within 30 mins of closing): You will be prompted in the Dashboard to choose DELIVER TODAY or DELIVER TOMORROW (auto-calculated as next working day opening + 30 mins).",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Time Picker Modal Dialog (supports both Master table and individual Weekly rows)
    if (showTimePickerDialog) {
        val isMasterEdit = editingDayIndex == -2
        val targetDay = if (!isMasterEdit && editingDayIndex in editableDays.indices) editableDays[editingDayIndex] else null
        val currentStr = when {
            isMasterEdit && isEditingOpeningTime -> masterOpeningTime
            isMasterEdit && !isEditingOpeningTime -> masterClosingTime
            targetDay != null && isEditingOpeningTime -> targetDay.openingTime
            targetDay != null && !isEditingOpeningTime -> targetDay.closingTime
            else -> "10:00 AM"
        }
        val dialogTitle = when {
            isMasterEdit && isEditingOpeningTime -> "Edit Default Shop Opening Time"
            isMasterEdit && !isEditingOpeningTime -> "Edit Default Shop Closing Time"
            targetDay != null && isEditingOpeningTime -> "Opening Time for ${targetDay.dayOfWeek}"
            targetDay != null -> "Closing Time for ${targetDay.dayOfWeek}"
            else -> "Select Time"
        }

        ScheduleTimePickerDialog(
            title = dialogTitle,
            initialTime = currentStr,
            onDismiss = { showTimePickerDialog = false },
            onConfirmTime = { newTime ->
                if (isMasterEdit) {
                    if (isEditingOpeningTime) {
                        val oldMasterOpen = masterOpeningTime
                        masterOpeningTime = newTime
                        // Also update days that were using the previous default opening time
                        editableDays = editableDays.map { d ->
                            if (d.openingTime == oldMasterOpen) d.copy(openingTime = newTime) else d
                        }
                    } else {
                        val oldMasterClose = masterClosingTime
                        masterClosingTime = newTime
                        editableDays = editableDays.map { d ->
                            if (d.closingTime == oldMasterClose) d.copy(closingTime = newTime) else d
                        }
                    }
                    persistScheduleNow("Updated default shop ${if (isEditingOpeningTime) "opening" else "closing"} time to $newTime")
                } else if (targetDay != null && editingDayIndex in editableDays.indices) {
                    editableDays = editableDays.toMutableList().also { list ->
                        list[editingDayIndex] = if (isEditingOpeningTime) {
                            targetDay.copy(openingTime = newTime)
                        } else {
                            targetDay.copy(closingTime = newTime)
                        }
                    }
                    persistScheduleNow("Updated ${targetDay.dayOfWeek} ${if (isEditingOpeningTime) "opening" else "closing"} time to $newTime")
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleSettingTableRow(
    settingName: String,
    subtitle: String? = null,
    valueText: String,
    valueColor: Color,
    actionContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.35f)) {
            Text(
                text = settingName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1.15f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier.weight(1.1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            actionContent()
        }
    }
}

@Composable
private fun ViewBadgePill(
    text: String,
    isPositive: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPositive) GreenLight else OrangeLight)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) GreenPrimary else OrangeSecondary
        )
    }
}

@Composable
private fun WeeklyScheduleRow(
    daySchedule: DaySchedule,
    isToday: Boolean = false,
    onEditOpeningTime: () -> Unit,
    onEditClosingTime: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isToday) GreenLight.copy(alpha = 0.2f) else Color.Transparent)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Day
        Column(modifier = Modifier.weight(1.1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = daySchedule.dayOfWeek,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (daySchedule.isOpen) TextDark else TextMuted
                )
                if (isToday) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary)
                    )
                }
            }
            if (!daySchedule.isOpen) {
                Text("Weekly Off", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
            }
        }

        // Column 2: Opening Time
        Box(
            modifier = Modifier
                .weight(1.3f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (daySchedule.isOpen) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
                    .border(1.dp, if (daySchedule.isOpen) BorderLight else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .clickable(enabled = daySchedule.isOpen, onClick = onEditOpeningTime)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("open_time_${daySchedule.dayOfWeek.lowercase()}")
            ) {
                Text(
                    text = daySchedule.openingTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (daySchedule.isOpen) GreenPrimary else TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Column 3: Closing Time
        Box(
            modifier = Modifier
                .weight(1.3f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (daySchedule.isOpen) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
                    .border(1.dp, if (daySchedule.isOpen) BorderLight else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .clickable(enabled = daySchedule.isOpen, onClick = onEditClosingTime)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("close_time_${daySchedule.dayOfWeek.lowercase()}")
            ) {
                Text(
                    text = daySchedule.closingTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (daySchedule.isOpen) OrangeSecondary else TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Column 4: Shop Status Switch
        Box(
            modifier = Modifier.weight(1.0f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Switch(
                    checked = daySchedule.isOpen,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GreenPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .size(width = 42.dp, height = 24.dp)
                        .testTag("day_switch_${daySchedule.dayOfWeek.lowercase()}")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (daySchedule.isOpen) "ON" else "OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (daySchedule.isOpen) GreenPrimary else TextMuted
                )
            }
        }
    }
}

@Composable
private fun ScheduleTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirmTime: (String) -> Unit
) {
    val initialMinutes = remember(initialTime) {
        ShopScheduleUtils.parseTimeToMinutes(initialTime)
    }

    var selectedHour by remember {
        val totalHours = initialMinutes / 60
        val displayHour = when (val h = totalHours % 12) {
            0 -> 12
            else -> h
        }
        mutableIntStateOf(displayHour)
    }

    var selectedMinute by remember {
        mutableIntStateOf(initialMinutes % 60)
    }

    var isPM by remember {
        mutableStateOf(initialMinutes >= 720)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.ENGLISH, "%02d:%02d %s", selectedHour, selectedMinute, if (isPM) "PM" else "AM"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AM / PM Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isPM,
                        onClick = { isPM = false },
                        label = { Text("AM (Morning)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    FilterChip(
                        selected = isPM,
                        onClick = { isPM = true },
                        label = { Text("PM (Evening/Night)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeSecondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hour Selection Chips
                Text("Select Hour", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                val hours = listOf(6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        hours.take(6).forEach { h ->
                            TimeChip(
                                label = "$h",
                                isSelected = selectedHour == h,
                                onClick = { selectedHour = h }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        hours.drop(6).forEach { h ->
                            TimeChip(
                                label = "$h",
                                isSelected = selectedHour == h,
                                onClick = { selectedHour = h }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Minute Selection Chips
                Text("Select Minute", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        TimeChip(
                            label = String.format(Locale.ENGLISH, ":%02d", m),
                            isSelected = selectedMinute == m,
                            onClick = { selectedMinute = m }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(
                        Locale.ENGLISH,
                        "%02d:%02d %s",
                        selectedHour,
                        selectedMinute,
                        if (isPM) "PM" else "AM"
                    )
                    onConfirmTime(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Set Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun TimeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) GreenPrimary else Color(0xFFF1F5F9))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextDark
        )
    }
}
