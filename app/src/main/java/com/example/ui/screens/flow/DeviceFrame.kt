package com.example.ui.screens.flow

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark

/**
 * High-fidelity mobile phone frame container modeling the presentation
 * frames shown in the FlashDrop 10-screen UI/UX design board.
 */
@Composable
fun DeviceFrame(
    frameNumber: Int,
    title: String,
    modifier: Modifier = Modifier,
    frameWidth: Dp = 320.dp,
    frameHeight: Dp = 680.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.width(frameWidth)
    ) {
        // Frame title header (e.g. "1. Cart / Order Summary")
        Text(
            text = "$frameNumber. $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Phone Device Container
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .width(frameWidth)
                .height(frameHeight)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x33000000))
                .border(2.5.dp, Color(0xFF0F172A), RoundedCornerShape(32.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Phone Status Bar (9:41, Camera Island, Wifi & Battery)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(Color.White)
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "9:41",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    // Subtle dynamic island / camera pill
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Screen body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                ) {
                    content()
                }
            }
        }
    }
}
