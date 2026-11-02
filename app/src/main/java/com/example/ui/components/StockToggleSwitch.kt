package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.TextDark

val FlashDropGreenColor = Color(0xFF087443)
val FlashDropLightGreyColor = Color(0xFFBDBDBD)

/**
 * Functional Android-style ON/OFF toggle switch for Product Stock Availability.
 * Follows exact specifications from the FlashDrop Product Form Requirements:
 * - When ON:
 *    - Track colour: FlashDrop Green (#087443)
 *    - White circular thumb positioned on the RIGHT
 *    - Label: "Available in Stock"
 *    - Description: "Customers can order this product."
 * - When OFF:
 *    - Track colour: Light Grey (#BDBDBD)
 *    - White circular thumb positioned on the LEFT
 *    - Label: "Out of Stock"
 *    - Description: "Customers cannot order this product."
 * - Responsive, animated, and fully clickable on mobile screens.
 */
@Composable
fun StockToggleRow(
    inStock: Boolean,
    onStockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundWarm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) {
                onStockChange(!inStock)
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("stock_toggle_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = if (inStock) "Available in Stock" else "Out of Stock",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextDark
            )
            Text(
                text = if (inStock) "Customers can order this product." else "Customers cannot order this product.",
                fontSize = 11.sp,
                color = if (inStock) FlashDropGreenColor else Color(0xFF757575),
                lineHeight = 15.sp
            )
        }

        StockToggleSwitch(
            checked = inStock,
            onCheckedChange = onStockChange
        )
    }
}

@Composable
fun StockToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val activeTrackColor = if (checked) FlashDropGreenColor else FlashDropLightGreyColor
    val trackColor by animateColorAsState(
        targetValue = if (enabled) activeTrackColor else Color(0xFFE2E8F0),
        animationSpec = tween(durationMillis = 200),
        label = "stockToggleTrackColor"
    )

    // Track width: 52dp, height: 30dp, thumb: 22dp circle
    // When checked (RIGHT): start padding = 52 - 22 - 4 = 26dp
    // When unchecked (LEFT): start padding = 4dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 26.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "stockToggleThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp)
            ) {
                onCheckedChange(!checked)
            }
            .testTag("stock_toggle_switch"),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(22.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
