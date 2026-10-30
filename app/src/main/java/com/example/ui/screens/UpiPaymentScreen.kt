package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiPaymentScreen(
    amount: Double = 315.0,
    orderId: String = "FD20261021A001",
    onProceedToProcessing: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pay with UPI",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Amount to Pay Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upi_amount_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount to Pay",
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${amount.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Order ID: $orderId",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scan & Pay Section
            Text(
                text = "Scan & Pay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Scan the QR code using any UPI app",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp)
            )

            // Dynamic QR Code Card
            Card(
                modifier = Modifier
                    .size(210.dp)
                    .clickable { onProceedToProcessing() }
                    .testTag("upi_qr_code_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UpiQrCodeCanvas(modifier = Modifier.size(175.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // UPI Apps Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UpiAppBadge(name = "GPay", color = Color(0xFF4285F4), label = "GPay", onClick = onProceedToProcessing)
                UpiAppBadge(name = "PhonePe", color = Color(0xFF5F259F), label = "पे", onClick = onProceedToProcessing)
                UpiAppBadge(name = "Paytm", color = Color(0xFF002970), label = "paytm", onClick = onProceedToProcessing)
                UpiAppBadge(name = "BHIM", color = Color(0xFF00796B), label = "BHIM", onClick = onProceedToProcessing)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intent "Click to Pay" Button
            Button(
                onClick = onProceedToProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("click_to_pay_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Click to Pay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Open any UPI app and pay ₹${amount.toInt()}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your payment will be verified securely by our payment gateway.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun UpiAppBadge(name: String, color: Color, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = name, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun UpiQrCodeCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dark = Color(0xFF1E293B)
        val step = w / 15f

        // Draw outer position markers (top-left, top-right, bottom-left)
        fun drawCornerMarker(x: Float, y: Float) {
            drawRoundRect(
                color = dark,
                topLeft = Offset(x, y),
                size = Size(step * 4, step * 4),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x + step * 0.7f, y + step * 0.7f),
                size = Size(step * 2.6f, step * 2.6f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = dark,
                topLeft = Offset(x + step * 1.3f, y + step * 1.3f),
                size = Size(step * 1.4f, step * 1.4f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        drawCornerMarker(0f, 0f)
        drawCornerMarker(w - step * 4, 0f)
        drawCornerMarker(0f, h - step * 4)

        // Draw stylized internal QR matrix dots
        val pattern = listOf(
            5 to 1, 6 to 1, 8 to 1, 9 to 1,
            5 to 2, 7 to 2, 9 to 2,
            5 to 3, 6 to 3, 8 to 3,
            1 to 5, 3 to 5, 6 to 5, 7 to 5, 9 to 5, 11 to 5, 13 to 5,
            2 to 6, 4 to 6, 7 to 6, 8 to 6, 12 to 6, 14 to 6,
            1 to 7, 3 to 7, 5 to 7, 9 to 7, 11 to 7, 13 to 7,
            2 to 8, 4 to 8, 6 to 8, 10 to 8, 12 to 8,
            1 to 9, 7 to 9, 8 to 9, 11 to 9, 13 to 9,
            5 to 11, 7 to 11, 8 to 11, 10 to 11, 12 to 11, 14 to 11,
            6 to 12, 9 to 12, 11 to 12, 13 to 12,
            5 to 13, 8 to 13, 10 to 13, 12 to 13, 14 to 13
        )

        for ((c, r) in pattern) {
            drawRoundRect(
                color = dark,
                topLeft = Offset(c * step, r * step),
                size = Size(step * 0.9f, step * 0.9f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Center FlashDrop shield logo badge
        val centerBox = step * 3.5f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset((w - centerBox) / 2f, (h - centerBox) / 2f),
            size = Size(centerBox, centerBox),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = GreenPrimary,
            topLeft = Offset((w - centerBox) / 2f + 3f, (h - centerBox) / 2f + 3f),
            size = Size(centerBox - 6f, centerBox - 6f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawCircle(
            color = Color.White,
            radius = step * 0.6f,
            center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = OrangeSecondary,
            radius = step * 0.35f,
            center = Offset(w / 2f, h / 2f)
        )
    }
}
