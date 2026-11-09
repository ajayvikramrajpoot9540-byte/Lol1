package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.util.getResponsiveLogoHeight

/**
 * Official FlashDrop Logo using the user-provided official brand logo asset.
 * Preserves the exact deep green and saffron orange colors, shopping bag with lightning bolt symbol,
 * and official typography without stretching, cropping, or altering the original design.
 *
 * Automatically sizes according to device screen specification:
 * - Small mobile: 32–36 dp
 * - Normal mobile: 38–42 dp
 * - Large mobile: 42–46 dp
 * - Tablet & Large tablet: 44–52 dp
 *
 * Width adjusts automatically according to the original aspect ratio using ContentScale.Fit.
 */
@Composable
fun FlashDropLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified,
    showTagline: Boolean = true,
    useTransparentBackground: Boolean = true
) {
    val responsiveHeight = getResponsiveLogoHeight(isHeader = false)
    val targetHeight = if (iconSize != Dp.Unspecified && iconSize in 28.dp..52.dp) {
        iconSize
    } else {
        responsiveHeight
    }

    val drawableRes = if (useTransparentBackground) {
        R.drawable.flashdrop_official_logo_transparent
    } else {
        R.drawable.flashdrop_official_logo
    }

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "FlashDrop - Click Karo, Ghar Mangao!",
            modifier = Modifier
                .height(targetHeight)
                .wrapContentWidth(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Compact FlashDrop Logo for headers, TopAppBars, and navigation rows.
 * Features the official FlashDrop logo proportional and scaled to header height specs.
 */
@Composable
fun FlashDropCompactLogo(
    modifier: Modifier = Modifier,
    size: Dp = Dp.Unspecified,
    showText: Boolean = true
) {
    val responsiveHeight = getResponsiveLogoHeight(isHeader = true)
    val targetHeight = if (size != Dp.Unspecified && size in 24.dp..46.dp) {
        size
    } else {
        responsiveHeight
    }

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showText) {
            Image(
                painter = painterResource(id = R.drawable.flashdrop_official_logo_transparent),
                contentDescription = "FlashDrop Logo",
                modifier = Modifier
                    .height(targetHeight)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.flashdrop_symbol_transparent),
                contentDescription = "FlashDrop Symbol",
                modifier = Modifier.size(targetHeight),
                contentScale = ContentScale.Fit
            )
        }
    }
}
