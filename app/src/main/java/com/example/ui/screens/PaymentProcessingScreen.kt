package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlashDropLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun PaymentProcessingScreen(
    onProcessingComplete: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        delay(900)
        stage = 2
        delay(1100)
        stage = 3
        delay(800)
        onProcessingComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spinner_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .testTag("payment_processing_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            FlashDropLogo(
                iconSize = 76.dp,
                showTagline = true,
                modifier = Modifier.padding(bottom = 36.dp)
            )

            // Dynamic Gradient Circular Loader
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(rotationAngle)
                ) {
                    val stroke = 6.dp.toPx()
                    // Track
                    drawCircle(
                        color = Color(0xFFF1F5F9),
                        style = Stroke(width = stroke)
                    )
                    // Gradient-style green & orange sweep
                    drawArc(
                        color = GreenPrimary,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = OrangeSecondary,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Processing Payment...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Please do not close the app.\nThis may take a few seconds.",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Status Checkpoints
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CheckpointRow(
                        title = "Payment Sent",
                        state = if (stage >= 1) CheckpointState.COMPLETED else CheckpointState.PENDING
                    )
                    CheckpointRow(
                        title = "Verifying with Bank",
                        state = when {
                            stage > 2 -> CheckpointState.COMPLETED
                            stage == 2 -> CheckpointState.ACTIVE
                            else -> CheckpointState.PENDING
                        }
                    )
                    CheckpointRow(
                        title = "Order Confirmed",
                        state = when {
                            stage >= 3 -> CheckpointState.COMPLETED
                            else -> CheckpointState.PENDING
                        }
                    )
                }
            }
        }
    }
}

enum class CheckpointState {
    COMPLETED, ACTIVE, PENDING
}

@Composable
fun CheckpointRow(title: String, state: CheckpointState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            CheckpointState.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            CheckpointState.ACTIVE -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, OrangeSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(OrangeSecondary)
                    )
                }
            }
            CheckpointState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (state == CheckpointState.ACTIVE) FontWeight.Bold else FontWeight.SemiBold,
            color = when (state) {
                CheckpointState.COMPLETED -> TextDark
                CheckpointState.ACTIVE -> OrangeSecondary
                CheckpointState.PENDING -> TextMuted
            }
        )
    }
}
