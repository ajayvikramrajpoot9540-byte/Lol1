package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@Composable
fun PaymentSuccessScreen(
    amount: Double = 315.0,
    orderId: String = "FD20261021A001",
    paymentReference: String = "7HGF54J3K2",
    transactionId: String = "UPI1234567890",
    onViewOrder: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("payment_success_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Large green checkmark with celebratory particles
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val dots = listOf(
                    Offset(size.width * 0.15f, size.height * 0.2f) to OrangeSecondary,
                    Offset(size.width * 0.85f, size.height * 0.22f) to GreenPrimary,
                    Offset(size.width * 0.08f, size.height * 0.75f) to Color(0xFF3B82F6),
                    Offset(size.width * 0.92f, size.height * 0.7f) to OrangeSecondary,
                    Offset(size.width * 0.5f, size.height * 0.05f) to Color(0xFF10B981)
                )
                for ((pos, color) in dots) {
                    drawCircle(color = color, radius = 4.dp.toPx(), center = pos)
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Payment Successful!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "₹${amount.toInt()}",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Order ID: $orderId",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Payment Method Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundWarm)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Payment Method: ",
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "UPI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Payment Receipt Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Paid Amount", fontSize = 13.sp, color = TextMuted)
                    Text(text = "₹${amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Payment Reference", fontSize = 13.sp, color = TextMuted)
                    Text(text = paymentReference, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Transaction ID", fontSize = 13.sp, color = TextMuted)
                    Text(text = transactionId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Button(
            onClick = onViewOrder,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("success_view_order_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text(text = "View Order", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("success_back_to_home_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
        ) {
            Text(text = "Back to Home", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Festive Thank You Note
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = OrangeSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Thank you for shopping with FlashDrop!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
    }
}
