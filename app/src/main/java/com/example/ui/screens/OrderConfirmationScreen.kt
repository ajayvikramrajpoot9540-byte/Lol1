package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlashDropCompactLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    orderId: String = "FD20261021A001",
    totalAmount: Double = 315.0,
    paymentMethod: String = "UPI",
    paymentStatus: String = "paid",
    onTrackOrder: () -> Unit,
    onBackToHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    FlashDropCompactLogo(size = 28.dp, showText = true)
                },
                actions = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .testTag("order_confirmation_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large Checkmark
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Order Placed!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your order has been successfully placed.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Order Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Order ID", fontSize = 13.sp, color = TextMuted)
                        Text(text = orderId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Amount", fontSize = 13.sp, color = TextMuted)
                        Text(text = "₹${totalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Payment Method", fontSize = 13.sp, color = TextMuted)
                        val isPaid = paymentStatus.equals("paid", ignoreCase = true)
                        Text(
                            text = paymentMethod,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) GreenPrimary else TextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expected Delivery Slot Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OrangeLight.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(OrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = OrangeSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Expected Delivery",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Today, 4:30 PM - 6:30 PM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Order Items (3) Preview
            Text(
                text = "Order Items (3)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfirmationItemRow("🍞", "Aashirvaad Atta (5kg)", "₹245", "×1")
                ConfirmationItemRow("🍪", "Parle-G Biscuits", "₹20", "×2")
                ConfirmationItemRow("🍜", "Maggi Noodles", "₹15", "×1")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Track Order CTA
            Button(
                onClick = onTrackOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("track_order_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(
                    text = "Track Order",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "Back to Home",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConfirmationItemRow(emoji: String, title: String, price: String, qty: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundWarm)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(price, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.width(6.dp))
            Text(qty, fontSize = 11.sp, color = TextMuted)
        }
    }
}
