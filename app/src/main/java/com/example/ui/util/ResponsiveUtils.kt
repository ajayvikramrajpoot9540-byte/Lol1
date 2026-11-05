package com.example.ui.util

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen categorization for responsive design across Android devices per PDF specifications:
 * - A. Small Mobile Phones: Target widths 320–375 px/dp (e.g., 320x640, 360x640, 360x800)
 * - B. Large Mobile Phones: Target widths 376–600 px/dp (e.g., 393x873, 432x960)
 * - C. Tablets & Foldables: Target widths 601–1024 px/dp and above (e.g., 800x1280 portrait, 1280x800 landscape)
 */
enum class ScreenType {
    SMALL_MOBILE,
    LARGE_MOBILE,
    TABLET,
    LARGE_TABLET
}

/**
 * Detects current device screen classification using LocalConfiguration.
 */
@Composable
fun rememberScreenType(): ScreenType {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth <= 375 -> ScreenType.SMALL_MOBILE
        screenWidth <= 600 -> ScreenType.LARGE_MOBILE
        screenWidth <= 1024 -> ScreenType.TABLET
        else -> ScreenType.LARGE_TABLET
    }
}

/**
 * Returns recommended FlashDrop logo height according to device screen specification.
 * Original aspect ratio is ~1.47 (width / height):
 * - Small phones: 110–130 px wide -> height 30–34 dp (header: 30 dp, standalone/card: 34 dp)
 * - Large phones: 130–155 px wide -> height 36–40 dp (header: 36 dp, standalone/card: 40 dp)
 * - Tablets: 150–180 px wide -> height 42–48 dp (header: 42 dp, standalone/card: 48 dp)
 */
@Composable
fun getResponsiveLogoHeight(isHeader: Boolean = false): Dp {
    val screenType = rememberScreenType()
    return if (isHeader) {
        when (screenType) {
            ScreenType.SMALL_MOBILE -> 30.dp
            ScreenType.LARGE_MOBILE -> 36.dp
            ScreenType.TABLET -> 40.dp
            ScreenType.LARGE_TABLET -> 44.dp
        }
    } else {
        when (screenType) {
            ScreenType.SMALL_MOBILE -> 34.dp
            ScreenType.LARGE_MOBILE -> 40.dp
            ScreenType.TABLET -> 46.dp
            ScreenType.LARGE_TABLET -> 50.dp
        }
    }
}

/**
 * Recommended side page margins per PDF specification:
 * - Small phones: 12–16 px/dp (14 dp)
 * - Large phones: 16–24 px/dp (18 dp)
 * - Tablets: 24–32 px/dp (28 dp)
 */
@Composable
fun getResponsiveHorizontalPadding(): Dp {
    val screenType = rememberScreenType()
    return when (screenType) {
        ScreenType.SMALL_MOBILE -> 14.dp
        ScreenType.LARGE_MOBILE -> 18.dp
        ScreenType.TABLET, ScreenType.LARGE_TABLET -> 28.dp
    }
}

/**
 * Recommended header internal vertical padding per PDF specification:
 * - 8–12 px/dp
 */
@Composable
fun getResponsiveHeaderVerticalPadding(): Dp {
    val screenType = rememberScreenType()
    return when (screenType) {
        ScreenType.SMALL_MOBILE -> 8.dp
        ScreenType.LARGE_MOBILE -> 10.dp
        ScreenType.TABLET, ScreenType.LARGE_TABLET -> 12.dp
    }
}

/**
 * Recommended gap between header and page content per PDF specification:
 * - 12–20 px/dp
 */
@Composable
fun getResponsiveContentGap(): Dp {
    val screenType = rememberScreenType()
    return when (screenType) {
        ScreenType.SMALL_MOBILE -> 12.dp
        ScreenType.LARGE_MOBILE -> 16.dp
        ScreenType.TABLET, ScreenType.LARGE_TABLET -> 20.dp
    }
}

/**
 * Checks if the current configuration is a tablet or landscape display.
 */
@Composable
fun isTabletDevice(): Boolean {
    val type = rememberScreenType()
    return type == ScreenType.TABLET || type == ScreenType.LARGE_TABLET
}

/**
 * Checks if the display is currently in landscape orientation.
 */
@Composable
fun isLandscapeOrientation(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Responsive container that constrains content width on large/tablet screens
 * and centers it horizontally, preventing awkward full-width stretching of forms,
 * dialogs, and dashboard cards on tablets and landscape screens.
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 680.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            content = content
        )
    }
}
