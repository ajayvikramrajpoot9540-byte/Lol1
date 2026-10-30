package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeSecondary

/**
 * Official FlashDrop Logo using the user-provided official brand logo asset.
 * Preserves the exact deep green and saffron orange colors, shopping bag with lightning bolt symbol,
 * and tagline without stretching or altering the original design.
 */
@Composable
fun FlashDropLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 120.dp,
    showTagline: Boolean = true,
    useTransparentBackground: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val drawableRes = if (useTransparentBackground) {
            if (showTagline) R.drawable.flashdrop_logo_transparent else R.drawable.flashdrop_symbol_transparent
        } else {
            if (showTagline) R.drawable.flashdrop_logo else R.drawable.flashdrop_symbol
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "FlashDrop - Click Karo, Ghar Mangao!",
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Compact FlashDrop Logo for headers, TopAppBars, and navigation rows.
 * Features the official shopping bag & lightning bolt symbol next to the official bold brand name.
 */
@Composable
fun FlashDropCompactLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.flashdrop_symbol_transparent),
            contentDescription = "FlashDrop Logo",
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
        if (showText) {
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Flash",
                    fontSize = (size.value * 0.58f).coerceAtLeast(16f).sp,
                    fontWeight = FontWeight.Black,
                    color = GreenPrimary
                )
                Text(
                    text = "Drop",
                    fontSize = (size.value * 0.58f).coerceAtLeast(16f).sp,
                    fontWeight = FontWeight.Black,
                    color = OrangeSecondary
                )
            }
        }
    }
}

@Composable
fun ApnaBazLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 120.dp,
    showTagline: Boolean = true
) = FlashDropLogo(modifier, iconSize, showTagline)
