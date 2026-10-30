package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(Color(0xFFFFF7ED), Color(0xFFC2410C), "Pending")
        "accepted" -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "Accepted")
        "preparing" -> Triple(OrangeLight, OrangeSecondary, "Preparing")
        "out_for_delivery" -> Triple(OrangeLight, Color(0xFFD97706), "Out for Delivery")
        "delivered" -> Triple(GreenLight, GreenPrimary, "Delivered")
        "cancelled" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Cancelled")
        "rejected" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Rejected")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), status.replace('_', ' ').capitalize())
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
