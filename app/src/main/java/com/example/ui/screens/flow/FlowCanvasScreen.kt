package com.example.ui.screens.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.components.FlashDropLogo
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

enum class CanvasViewMode {
    PANORAMIC_CANVAS,
    STEP_WALKTHROUGH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowCanvasScreen(
    onNavigateToScreen: (String) -> Unit,
    onBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(CanvasViewMode.PANORAMIC_CANVAS) }
    var currentStep by remember { mutableIntStateOf(1) } // 1 to 10
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val screenTitles = listOf(
        "Cart / Order Summary",
        "Checkout",
        "Pay with UPI",
        "Payment Processing",
        "Payment Success",
        "Order Confirmation",
        "Order Tracking (with payment status)",
        "Payment Dashboard (Admin)",
        "Transaction History (Admin)",
        "Shopkeeper View (limited access)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(size = 28.dp, showText = true)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "10-Screen UI/UX Flow",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    // Toggle between Canvas Grid and Walkthrough Mode
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(BackgroundWarm)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (viewMode == CanvasViewMode.PANORAMIC_CANVAS) GreenPrimary else Color.Transparent)
                                .clickable { viewMode = CanvasViewMode.PANORAMIC_CANVAS }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = if (viewMode == CanvasViewMode.PANORAMIC_CANVAS) Color.White else TextDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Canvas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == CanvasViewMode.PANORAMIC_CANVAS) Color.White else TextDark
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (viewMode == CanvasViewMode.STEP_WALKTHROUGH) GreenPrimary else Color.Transparent)
                                .clickable { viewMode = CanvasViewMode.STEP_WALKTHROUGH }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (viewMode == CanvasViewMode.STEP_WALKTHROUGH) Color.White else TextDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Walkthrough",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == CanvasViewMode.STEP_WALKTHROUGH) Color.White else TextDark
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(paddingValues)
                .testTag("flow_canvas_screen")
        ) {
            when (viewMode) {
                CanvasViewMode.PANORAMIC_CANVAS -> {
                    PanoramicCanvasView(
                        onSelectScreen = { step ->
                            currentStep = step
                            viewMode = CanvasViewMode.STEP_WALKTHROUGH
                        },
                        onLaunchLiveScreen = { step ->
                            when (step) {
                                1 -> onNavigateToScreen(Screen.Cart.route)
                                2 -> onNavigateToScreen(Screen.Checkout.route)
                                3 -> onNavigateToScreen(Screen.UpiPayment.route)
                                4 -> onNavigateToScreen(Screen.PaymentProcessing.route)
                                5 -> onNavigateToScreen(Screen.PaymentSuccess.route)
                                6 -> onNavigateToScreen(Screen.OrderConfirmation.route)
                                7 -> onNavigateToScreen(Screen.OrderDetail.route)
                                8 -> onNavigateToScreen(Screen.AdminDashboard.route)
                                9 -> onNavigateToScreen(Screen.AdminTransactions.route)
                                10 -> onNavigateToScreen(Screen.ShopkeeperDashboard.route)
                            }
                        }
                    )
                }

                CanvasViewMode.STEP_WALKTHROUGH -> {
                    StepWalkthroughView(
                        currentStep = currentStep,
                        onStepChange = { currentStep = it },
                        screenTitles = screenTitles,
                        onLaunchLiveScreen = {
                            when (currentStep) {
                                1 -> onNavigateToScreen(Screen.Cart.route)
                                2 -> onNavigateToScreen(Screen.Checkout.route)
                                3 -> onNavigateToScreen(Screen.UpiPayment.route)
                                4 -> onNavigateToScreen(Screen.PaymentProcessing.route)
                                5 -> onNavigateToScreen(Screen.PaymentSuccess.route)
                                6 -> onNavigateToScreen(Screen.OrderConfirmation.route)
                                7 -> onNavigateToScreen(Screen.OrderDetail.route)
                                8 -> onNavigateToScreen(Screen.AdminDashboard.route)
                                9 -> onNavigateToScreen(Screen.AdminDashboard.route)
                                10 -> onNavigateToScreen(Screen.ShopkeeperDashboard.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 2-Row Panoramic Presentation Canvas displaying all 10 device frames
 * arranged sequentially with the footer logo and tagline matching payment-page.png.
 */
@Composable
private fun PanoramicCanvasView(
    onSelectScreen: (Int) -> Unit,
    onLaunchLiveScreen: (Int) -> Unit
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .horizontalScroll(hScroll)
            .padding(24.dp)
    ) {
        // Presentation Canvas Header Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FlashDropCompactLogo(size = 36.dp, showText = true)
                Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                Column {
                    Text(
                        text = "FlashDrop Hyperlocal UI/UX Flow Design Board",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Text(
                        text = "10 Sequential mobile screens • Tap any phone frame to test walkthrough • Clean white canvas & M3 standards",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // ROW 1: Screens 1 to 5
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Cart / Order Summary
            DeviceFrame(1, "Cart / Order Summary", onClick = { onSelectScreen(1) }) {
                Screen1CartView(onProceed = { onSelectScreen(2) })
            }

            // 2. Checkout
            DeviceFrame(2, "Checkout", onClick = { onSelectScreen(2) }) {
                Screen2CheckoutView(onPlaceOrder = { onSelectScreen(3) })
            }

            // 3. UPI Payment
            DeviceFrame(3, "UPI Payment", onClick = { onSelectScreen(3) }) {
                Screen3UpiPaymentView(onPay = { onSelectScreen(4) })
            }

            // 4. Payment Processing
            DeviceFrame(4, "Payment Processing", onClick = { onSelectScreen(4) }) {
                Screen4PaymentProcessingView(onComplete = { onSelectScreen(5) })
            }

            // 5. Payment Success
            DeviceFrame(5, "Payment Success", onClick = { onSelectScreen(5) }) {
                Screen5PaymentSuccessView(
                    onViewOrder = { onSelectScreen(6) },
                    onBackHome = { onSelectScreen(1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // ROW 2: Screens 6 to 10
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 6. Order Confirmation
            DeviceFrame(6, "Order Confirmation", onClick = { onSelectScreen(6) }) {
                Screen6OrderConfirmationView(onTrackOrder = { onSelectScreen(7) })
            }

            // 7. Order Tracking (with payment status)
            DeviceFrame(7, "Order Tracking (with payment status)", onClick = { onSelectScreen(7) }) {
                Screen7OrderTrackingView(onViewReceipt = { onSelectScreen(5) })
            }

            // 8. Payment Dashboard (Admin)
            DeviceFrame(8, "Payment Dashboard (Admin)", onClick = { onSelectScreen(8) }) {
                Screen8PaymentDashboardView(onViewAll = { onSelectScreen(9) })
            }

            // 9. Transaction History (Admin)
            DeviceFrame(9, "Transaction History (Admin)", onClick = { onSelectScreen(9) }) {
                Screen9TransactionHistoryView(onItemClick = { onSelectScreen(8) })
            }

            // 10. Shopkeeper View (limited access)
            DeviceFrame(10, "Shopkeeper View (limited access)", onClick = { onSelectScreen(10) }) {
                Screen10ShopkeeperView(onViewAll = { onSelectScreen(10) })
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Presentation Canvas Footer (matching payment-page.png)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dual green and orange line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(2.5.dp).background(GreenPrimary))
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(OrangeSecondary))
                Box(modifier = Modifier.weight(1f).height(2.5.dp).background(GreenPrimary))
            }

            Spacer(modifier = Modifier.height(18.dp))

            FlashDropLogo(iconSize = 64.dp, showTagline = true)
        }
    }
}

/**
 * Step Walkthrough View with large focused device frame and step navigation bar.
 */
@Composable
private fun StepWalkthroughView(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    screenTitles: List<String>,
    onLaunchLiveScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step selector chip row
        ScrollableTabRow(
            selectedTabIndex = currentStep - 1,
            edgePadding = 8.dp,
            containerColor = Color.White,
            contentColor = GreenPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            screenTitles.forEachIndexed { index, title ->
                Tab(
                    selected = currentStep == index + 1,
                    onClick = { onStepChange(index + 1) },
                    text = {
                        Text(
                            text = "${index + 1}. $title",
                            fontSize = 11.sp,
                            fontWeight = if (currentStep == index + 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Stepper Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { if (currentStep > 1) onStepChange(currentStep - 1) },
                enabled = currentStep > 1,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous", fontSize = 12.sp)
            }

            Text(
                text = "Screen $currentStep of 10",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )

            Button(
                onClick = { if (currentStep < 10) onStepChange(currentStep + 1) },
                enabled = currentStep < 10,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Next", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Device Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DeviceFrame(
                frameNumber = currentStep,
                title = screenTitles[currentStep - 1],
                frameWidth = 320.dp,
                frameHeight = 580.dp
            ) {
                when (currentStep) {
                    1 -> Screen1CartView(onProceed = { onStepChange(2) })
                    2 -> Screen2CheckoutView(onPlaceOrder = { onStepChange(3) })
                    3 -> Screen3UpiPaymentView(onPay = { onStepChange(4) })
                    4 -> Screen4PaymentProcessingView(onComplete = { onStepChange(5) })
                    5 -> Screen5PaymentSuccessView(onViewOrder = { onStepChange(6) }, onBackHome = { onStepChange(1) })
                    6 -> Screen6OrderConfirmationView(onTrackOrder = { onStepChange(7) })
                    7 -> Screen7OrderTrackingView(onViewReceipt = { onStepChange(5) })
                    8 -> Screen8PaymentDashboardView(onViewAll = { onStepChange(9) })
                    9 -> Screen9TransactionHistoryView(onItemClick = { onStepChange(8) })
                    10 -> Screen10ShopkeeperView(onViewAll = { onStepChange(1) })
                }
            }
        }
    }
}
