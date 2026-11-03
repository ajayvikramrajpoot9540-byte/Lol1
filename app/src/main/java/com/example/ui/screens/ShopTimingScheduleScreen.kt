package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySchedule
import com.example.data.model.Shop
import com.example.data.model.WeeklySchedule
import com.example.data.model.ShopScheduleUtils
import com.example.data.repository.FlashDropRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
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

    val targetShopId = shopIdOverride
        ?: profile.shopId.takeIf { it.isNotBlank() }
        ?: "sharma_general"

    val currentShop = remember(shops, targetShopId) {
        shops.find { it.id == targetShopId } ?: shops.firstOrNull()
    }

    // Editable weekly days schedule
    val initialSchedule = remember(targetShopId, schedules) {
        repository.getShopSchedule(targetShopId)
    }

    var editableDays by remember(initialSchedule) {
        mutableStateOf(initialSchedule.days)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Live status in Indian Standard Time (IST)
    var liveStatus by remember(editableDays) {
        mutableStateOf(ShopScheduleUtils.evaluateLiveStatus(initialSchedule.copy(days = editableDays)))
    }

    // Time picker dialog state
    var editingDayIndex by remember { mutableIntStateOf(-1) }
    var isEditingOpeningTime by remember { mutableStateOf(true) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shop Timing & Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${currentShop?.name ?: "Local Shop"} • Mahoba (IST)",
                            fontSize = 12.sp,
                            color = OrangeSecondary,
                            fontWeight = FontWeight.Medium
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("shop_timing_schedule_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Shop Status Banner (IST)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (liveStatus.isOpen) GreenLight.copy(alpha = 0.5f) else Color(0xFFFEE2E2)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (liveStatus.isOpen) GreenLight else Color(0xFFFECACA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (liveStatus.isOpen) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Current Shop Status (IST)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = liveStatus.statusLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (liveStatus.isOpen) GreenPrimary else Color(0xFFDC2626)
                            )
                            Text(
                                text = "Today (${liveStatus.currentDay}): ${liveStatus.openingTimeToday} to ${liveStatus.closingTimeToday}",
                                fontSize = 11.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            // 2. Preset Quick Actions Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Quick Presets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editableDays = editableDays.map {
                                        it.copy(openingTime = "10:00 AM", closingTime = "10:00 PM", isOpen = true)
                                    }
                                    liveStatus = ShopScheduleUtils.evaluateLiveStatus(initialSchedule.copy(days = editableDays))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("10 AM - 10 PM (All)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    editableDays = editableDays.map {
                                        it.copy(openingTime = "08:00 AM", closingTime = "11:00 PM", isOpen = true)
                                    }
                                    liveStatus = ShopScheduleUtils.evaluateLiveStatus(initialSchedule.copy(days = editableDays))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("8 AM - 11 PM (All)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 3. Opening and Closing Time Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Weekly Opening & Closing Timetable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Set daily operational hours. Toggling OFF marks shop closed for that day.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Table Header
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
                                text = "Opening Time",
                                modifier = Modifier.weight(1.3f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Closing Time",
                                modifier = Modifier.weight(1.3f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Shop Status",
                                modifier = Modifier.weight(1.0f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Table Rows for each day
                        editableDays.forEachIndexed { index, daySchedule ->
                            WeeklyScheduleRow(
                                daySchedule = daySchedule,
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
                                    liveStatus = ShopScheduleUtils.evaluateLiveStatus(initialSchedule.copy(days = editableDays))
                                }
                            )
                            if (index < editableDays.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            // 4. Instructions & Automatic Status Information Card
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
                                text = "Automatic Status Rules",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• At the scheduled opening time (e.g. 10:00 AM), your shop automatically opens.\n" +
                                    "• At the scheduled closing time (e.g. 10:00 PM), your shop automatically closes.\n" +
                                    "• Customers see LIVE status in real time and cannot place orders when closed.\n" +
                                    "• If an order arrives near closing time (within 60 mins), you receive an instant confirmation alert to deliver today or tomorrow.\n" +
                                    "• You can also use the temporary ON/OFF switch in the dashboard for emergency closures without losing your weekly timetable.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5. Save Schedule CTA Button
            item {
                Button(
                    onClick = {
                        val updatedSchedule = initialSchedule.copy(
                            shopId = targetShopId,
                            days = editableDays,
                            autoScheduleEnabled = true
                        )
                        repository.saveShopSchedule(updatedSchedule)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Shop weekly schedule saved successfully!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_schedule_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Schedule",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Time Picker Modal Dialog
    if (showTimePickerDialog && editingDayIndex in editableDays.indices) {
        val targetDay = editableDays[editingDayIndex]
        val currentStr = if (isEditingOpeningTime) targetDay.openingTime else targetDay.closingTime

        ScheduleTimePickerDialog(
            title = if (isEditingOpeningTime) "Select Opening Time for ${targetDay.dayOfWeek}" else "Select Closing Time for ${targetDay.dayOfWeek}",
            initialTime = currentStr,
            onDismiss = { showTimePickerDialog = false },
            onConfirmTime = { newTime ->
                editableDays = editableDays.toMutableList().also { list ->
                    list[editingDayIndex] = if (isEditingOpeningTime) {
                        targetDay.copy(openingTime = newTime)
                    } else {
                        targetDay.copy(closingTime = newTime)
                    }
                }
                liveStatus = ShopScheduleUtils.evaluateLiveStatus(initialSchedule.copy(days = editableDays))
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun WeeklyScheduleRow(
    daySchedule: DaySchedule,
    onEditOpeningTime: () -> Unit,
    onEditClosingTime: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Day
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = daySchedule.dayOfWeek,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (daySchedule.isOpen) TextDark else TextMuted
            )
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
                    modifier = Modifier.size(width = 42.dp, height = 24.dp)
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
