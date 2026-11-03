package com.example.ui.screens.flow

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.components.FlashDropLogo
import com.example.ui.screens.UpiQrCodeCanvas
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

// =========================================================================
// SCREEN 1: Cart / Order Summary
// =========================================================================
@Composable
fun Screen1CartView(onProceed: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            FlashDropCompactLogo(size = 24.dp, showText = true)
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(OrangeSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text("Your Cart (3 items)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))

            // Item 1: Aashirvaad Atta
            CartItemRow("🍞", "Aashirvaad Atta (5kg)", "₹245", 1)
            Spacer(modifier = Modifier.height(6.dp))
            // Item 2: Parle-G Biscuits
            CartItemRow("🍪", "Parle-G Biscuits", "₹20", 2)
            Spacer(modifier = Modifier.height(6.dp))
            // Item 3: Maggi Noodles
            CartItemRow("🍜", "Maggi Noodles", "₹15", 1)

            Spacer(modifier = Modifier.height(12.dp))

            // Price Details Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Price Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    PriceRowSmall("Subtotal", "₹300")
                    PriceRowSmall("Delivery Charge", "₹20")
                    PriceRowSmall("Platform Fee", "₹5")
                    PriceRowSmall("Discount", "- ₹10", isDiscount = true)
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                        Text("₹315", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Proceed to Checkout", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Mini bottom navigation bar
        MiniBottomBar(selectedIndex = 3)
    }
}

// =========================================================================
// SCREEN 2: Checkout
// =========================================================================
@Composable
fun Screen2CheckoutView(onPlaceOrder: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Checkout", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Delivery Address Box
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        Text("Edit", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Raju Kumar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextDark)
                    Text("Mahoba, Uttar Pradesh - 210427", fontSize = 11.sp, color = TextMuted)
                }
            }

            // Order Items Summary
            Text("Order Items", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OrderItemCompact("Aashirvaad Atta (5kg)", "₹245", "×1")
                OrderItemCompact("Parle-G Biscuits", "₹20", "×2")
                OrderItemCompact("Maggi Noodles", "₹15", "×1")
            }

            // Price Summary
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Price Summary", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    PriceRowSmall("Subtotal", "₹300")
                    PriceRowSmall("Delivery Charge", "₹20")
                    PriceRowSmall("Platform Fee", "₹5")
                    PriceRowSmall("Discount", "- ₹10", isDiscount = true)
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payable", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        Text("₹315", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenPrimary)
                    }
                }
            }

            // Payment Method
            Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = false, onClick = {}, colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Cash on Delivery (COD)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextDark)
                            Text("Pay in cash when your order arrives", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = true, onClick = {}, colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("UPI (Pay Online)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GreenPrimary)
                            Text("Pay securely using UPI apps", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }

            Button(
                onClick = onPlaceOrder,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Place Order", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// SCREEN 3: UPI Payment
// =========================================================================
@Composable
fun Screen3UpiPaymentView(onPay: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Pay with UPI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Amount Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Amount to Pay", fontSize = 11.sp, color = TextDark)
                Text("₹315", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                Text("Order ID: FD20261021A001", fontSize = 10.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Scan & Pay", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark, modifier = Modifier.fillMaxWidth())
        Text("Scan the QR code using any UPI app", fontSize = 10.sp, color = TextMuted, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        // QR Code Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .size(140.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable { onPay() }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                UpiQrCodeCanvas(modifier = Modifier.size(120.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // UPI app pills
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            UpiMiniBadge("GPay", Color(0xFF4285F4), "GPay", onPay)
            UpiMiniBadge("PhonePe", Color(0xFF5F259F), "पे", onPay)
            UpiMiniBadge("Paytm", Color(0xFF002970), "paytm", onPay)
            UpiMiniBadge("BHIM", Color(0xFF00796B), "BHIM", onPay)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("OR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPay,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Click to Pay ₹315", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Verified securely by UPI payment gateway", fontSize = 9.sp, color = TextMuted)
        }
    }
}

// =========================================================================
// SCREEN 4: Payment Processing
// =========================================================================
@Composable
fun Screen4PaymentProcessingView(onComplete: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(18.dp)
            .clickable { onComplete() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FlashDropLogo(iconSize = 56.dp, showTagline = true)
        Spacer(modifier = Modifier.height(28.dp))

        // Dual Color Progress Ring
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val stroke = 5.dp.toPx()
                drawCircle(color = Color(0xFFF1F5F9), style = Stroke(stroke))
                drawArc(color = GreenPrimary, startAngle = 30f, sweepAngle = 180f, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color = OrangeSecondary, startAngle = 210f, sweepAngle = 90f, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Processing Payment...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Please do not close the app.\nThis may take a few seconds.", fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(26.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCheckpoint("Payment Sent", state = 2) // Completed
                MiniCheckpoint("Verifying with Bank", state = 1) // Active orange
                MiniCheckpoint("Order Confirmed", state = 0) // Pending
            }
        }
    }
}

// =========================================================================
// SCREEN 5: Payment Success
// =========================================================================
@Composable
fun Screen5PaymentSuccessView(onViewOrder: () -> Unit = {}, onBackHome: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Checkmark
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Payment Successful!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("₹315", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
        Text("Order ID: FD20261021A001", fontSize = 11.sp, color = TextMuted)
        Text("Payment Method: UPI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)

        Spacer(modifier = Modifier.height(14.dp))

        // Receipt Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PriceRowSmall("Paid Amount", "₹315")
                PriceRowSmall("Payment Reference", "7HGF54J3K2")
                PriceRowSmall("Transaction ID", "UPI1234567890")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onViewOrder,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Text("View Order", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onBackHome,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Text("Back to Home", fontSize = 13.sp, color = TextDark)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Celebration, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Thank you for shopping with FlashDrop!", fontSize = 10.sp, color = TextDark)
        }
    }
}

// =========================================================================
// SCREEN 6: Order Confirmation
// =========================================================================
@Composable
fun Screen6OrderConfirmationView(onTrackOrder: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlashDropCompactLogo(size = 22.dp, showText = true)
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Order Placed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        Text("Your order has been successfully placed.", fontSize = 11.sp, color = TextMuted)

        Spacer(modifier = Modifier.height(12.dp))

        // Order Meta Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PriceRowSmall("Order ID", "FD20261021A001")
                PriceRowSmall("Total Amount", "₹315")
                PriceRowSmall("Payment Method", "UPI (Paid)")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Expected delivery slot
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = OrangeLight.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Expected Delivery", fontSize = 10.sp, color = TextMuted)
                    Text("Today, 4:30 PM - 6:30 PM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onTrackOrder,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Text("Track Order", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SCREEN 7: Order Tracking (with payment status)
// =========================================================================
@Composable
fun Screen7OrderTrackingView(onViewReceipt: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Track Order", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vertical Timeline
        TimelineNodeItem("Order Placed", "21 Oct, 2026 • 10:12 AM", isDone = true)
        TimelineNodeItem("Payment Completed", "₹315 • UPI", isDone = true)
        TimelineNodeItem("Preparing Your Order", "Shopkeeper is preparing your items", isDone = false, isActive = true)
        TimelineNodeItem("Out for Delivery", "Will be delivered by 4:30 PM", isDone = false)
        TimelineNodeItem("Delivered", "You'll get a notification", isDone = false, isLast = true)

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Details Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Payment Details", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Method", fontSize = 11.sp, color = TextMuted)
                    Text("UPI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status", fontSize = 11.sp, color = TextMuted)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenLight)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("● Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transaction ID", fontSize = 11.sp, color = TextMuted)
                    Text("UPI1234567890", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onViewReceipt,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("View Payment Receipt", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SCREEN 8: Admin Payment Dashboard
// =========================================================================
@Composable
fun Screen8PaymentDashboardView(onViewAll: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlashDropCompactLogo(size = 22.dp, showText = true)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("PR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Period selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Last 7 Days ▾", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }

            // 4 KPI Cards in 2x2 grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminKpiCard("Total Orders", "248", "↑ 12%", GreenPrimary, modifier = Modifier.weight(1f))
                AdminKpiCard("Online Payments", "182", "↑ 16%", GreenPrimary, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminKpiCard("COD Amount", "₹34,560", "↑ 5%", GreenPrimary, modifier = Modifier.weight(1f))
                AdminKpiCard("Pending Payments", "14", "↓ 3%", OrangeSecondary, modifier = Modifier.weight(1f))
            }

            // Donut Chart Payment Overview
            Text("Payment Overview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Donut Chart
                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(54.dp)) {
                            val stroke = 8.dp.toPx()
                            // UPI: 60% (216 deg)
                            drawArc(color = GreenPrimary, startAngle = -90f, sweepAngle = 216f, useCenter = false, style = Stroke(stroke))
                            // COD: 38% (136.8 deg)
                            drawArc(color = OrangeSecondary, startAngle = 126f, sweepAngle = 136.8f, useCenter = false, style = Stroke(stroke))
                            // Refunds: 2% (7.2 deg)
                            drawArc(color = Color(0xFFEF4444), startAngle = 262.8f, sweepAngle = 7.2f, useCenter = false, style = Stroke(stroke))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LegendRow(GreenPrimary, "UPI", "60%")
                        LegendRow(OrangeSecondary, "COD", "38%")
                        LegendRow(Color(0xFFEF4444), "Refunds", "2%")
                    }
                }
            }

            // Shop-wise summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shop-wise Order Summary", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark)
                Text("View All", color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onViewAll() })
            }
            ShopSummaryRow("Sharma General Store", "78 orders", "₹12,450")
            ShopSummaryRow("Gupta Medical Store", "54 orders", "₹8,320")
            ShopSummaryRow("Rajat Mobile Shop", "42 orders", "₹6,870")
        }

        // Admin Bottom Bar
        AdminMiniBottomBar(selectedIndex = 2)
    }
}

// =========================================================================
// SCREEN 9: Admin Transaction History
// =========================================================================
@Composable
fun Screen9TransactionHistoryView(onItemClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipSmall("All", isSelected = true)
            FilterChipSmall("Online (UPI)")
            FilterChipSmall("COD")
            FilterChipSmall("Refunds")
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TransactionItemRow("#FD20261021A001", "UPI • Paid • 10:32 AM", "₹315", statusColor = GreenPrimary, onClick = onItemClick)
            TransactionItemRow("#FD20261021A002", "COD • Collected • 11:15 AM", "₹280", statusColor = GreenPrimary, onClick = onItemClick)
            TransactionItemRow("#FD20261021A003", "UPI • Pending • 12:03 PM", "₹520", statusColor = OrangeSecondary, onClick = onItemClick)
            TransactionItemRow("#FD20261020A098", "UPI • Failed • 05:21 PM", "₹199", statusColor = Color(0xFFEF4444), onClick = onItemClick)
            TransactionItemRow("#FD20261020A097", "COD • Collected • 04:32 PM", "₹760", statusColor = GreenPrimary, onClick = onItemClick)
            TransactionItemRow("#FD20261020A096", "UPI • Refunded • 02:17 PM", "₹345", statusColor = Color(0xFFEF4444), onClick = onItemClick)
        }

        AdminMiniBottomBar(selectedIndex = 2)
    }
}

// =========================================================================
// SCREEN 10: Shopkeeper View (limited access)
// =========================================================================
@Composable
fun Screen10ShopkeeperView(onViewAll: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Shop", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Store Profile Header
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Sharma General Store", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                        Text("Shop ID: SHP001", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            // Tabs: Orders (active), Earnings, Profile
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Orders", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenPrimary)
                    Box(modifier = Modifier.width(36.dp).height(2.dp).background(GreenPrimary))
                }
                Text("Earnings", fontSize = 12.sp, color = TextMuted)
                Text("Profile", fontSize = 12.sp, color = TextMuted)
            }

            // 2 KPIs: Today's Orders & Total Sales
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminKpiCard("Today's Orders", "12", "", GreenPrimary, modifier = Modifier.weight(1f))
                AdminKpiCard("Total Sales", "₹4,560", "", OrangeSecondary, modifier = Modifier.weight(1f))
            }

            // Recent Orders list
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent Orders", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Text("View All", color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onViewAll() })
            }

            TransactionItemRow("#FD20261021A001", "Paid • 10:32 AM", "₹315", statusColor = GreenPrimary)
            TransactionItemRow("#FD20261021A002", "COD • 11:15 AM", "₹280", statusColor = OrangeSecondary)
            TransactionItemRow("#FD20261020A098", "Paid • 05:21 PM", "₹199", statusColor = GreenPrimary)
            TransactionItemRow("#FD20261020A097", "COD • 04:32 PM", "₹760", statusColor = OrangeSecondary)
        }

        // Shopkeeper bottom bar
        ShopkeeperMiniBottomBar(selectedIndex = 1)
    }
}

// =========================================================================
// Helper UI Components
// =========================================================================

@Composable
private fun CartItemRow(icon: String, title: String, price: String, qty: Int) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextDark)
                    Text(price, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GreenPrimary)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Add, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun OrderItemCompact(name: String, price: String, qty: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BackgroundWarm)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Medium)
        Row {
            Text(price, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.width(4.dp))
            Text(qty, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun PriceRowSmall(label: String, value: String, isDiscount: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 10.sp, color = TextMuted)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isDiscount) GreenPrimary else TextDark)
    }
}

@Composable
private fun UpiMiniBadge(name: String, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.8.dp, Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(name, fontSize = 9.sp, color = TextDark)
    }
}

@Composable
private fun MiniCheckpoint(title: String, state: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            2 -> { // Done
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(GreenPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            1 -> { // Active
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).border(2.dp, OrangeSecondary, CircleShape), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OrangeSecondary))
                }
            }
            else -> { // Pending
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).border(1.dp, Color(0xFFCBD5E1), CircleShape))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 11.sp, fontWeight = if (state == 1) FontWeight.Bold else FontWeight.Medium, color = if (state == 1) OrangeSecondary else TextDark)
    }
}

@Composable
private fun TimelineNodeItem(title: String, desc: String, isDone: Boolean, isActive: Boolean = false, isLast: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDone) GreenPrimary else if (isActive) OrangeSecondary else Color.White)
                    .then(if (!isDone && !isActive) Modifier.border(1.dp, Color(0xFFCBD5E1), CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(24.dp).background(if (isDone) GreenPrimary else Color(0xFFE2E8F0)))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isActive) OrangeSecondary else TextDark)
            Text(desc, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun AdminKpiCard(title: String, value: String, change: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 9.sp, color = TextMuted)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            if (change.isNotBlank()) {
                Text(change, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, pct: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: ", fontSize = 10.sp, color = TextMuted)
        Text(pct, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@Composable
private fun ShopSummaryRow(name: String, orders: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BackgroundWarm)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Store, contentDescription = null, tint = OrangeSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Text(orders, fontSize = 9.sp, color = TextMuted)
            }
        }
        Text(amount, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
    }
}

@Composable
private fun FilterChipSmall(text: String, isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GreenPrimary else BackgroundWarm)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else TextDark)
    }
}

@Composable
private fun TransactionItemRow(id: String, sub: String, amount: String, statusColor: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundWarm)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(id, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(sub, fontSize = 9.sp, color = TextMuted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun MiniBottomBar(selectedIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White)
            .border(0.5.dp, Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniNavItem(Icons.Default.Home, "Home", selected = selectedIndex == 0)
        MiniNavItem(Icons.Default.Storefront, "Categories", selected = selectedIndex == 1)
        MiniNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Orders", selected = selectedIndex == 2)
        MiniNavItem(Icons.Default.ShoppingCart, "Cart", selected = selectedIndex == 3, badge = "3")
        MiniNavItem(Icons.Default.Person, "Profile", selected = selectedIndex == 4)
    }
}

@Composable
private fun AdminMiniBottomBar(selectedIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White)
            .border(0.5.dp, Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniNavItem(Icons.Default.Home, "Home", selected = selectedIndex == 0)
        MiniNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Orders", selected = selectedIndex == 1)
        MiniNavItem(Icons.Default.PointOfSale, "Payments", selected = selectedIndex == 2)
        MiniNavItem(Icons.Default.Store, "Shops", selected = selectedIndex == 3)
        MiniNavItem(Icons.Default.MoreHoriz, "More", selected = selectedIndex == 4)
    }
}

@Composable
private fun ShopkeeperMiniBottomBar(selectedIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White)
            .border(0.5.dp, Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniNavItem(Icons.Default.Home, "Home", selected = selectedIndex == 0)
        MiniNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Orders", selected = selectedIndex == 1)
        MiniNavItem(Icons.Default.Payments, "Earnings", selected = selectedIndex == 2)
        MiniNavItem(Icons.Default.Person, "Profile", selected = selectedIndex == 3)
    }
}

@Composable
private fun MiniNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, badge: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) GreenPrimary else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(OrangeSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) GreenPrimary else TextMuted
        )
    }
}
