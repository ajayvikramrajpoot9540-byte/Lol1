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
 * Screen categorization for responsive design across Android devices:
 * - Small mobile: width < 380 dp (e.g., 360 x 640)
 * - Normal mobile: width 380..419 dp (e.g., 393 x 873)
 * - Large mobile: width 420..599 dp (e.g., 432 x 960)
 * - Tablet: width 600..839 dp (e.g., 800 x 1280 portrait)
 * - Large tablet / Landscape: width >= 840 dp (e.g., 1280 x 800)
 */
enum class ScreenType {
    SMALL_MOBILE,
    NORMAL_MOBILE,
    LARGE_MOBILE,
    TABLET,
    LARGE_TABLET
}

/**
 * Detects the current device screen classification using LocalConfiguration.
 */
@Composable
fun rememberScreenType(): ScreenType {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 380 -> ScreenType.SMALL_MOBILE
        screenWidth < 420 -> ScreenType.NORMAL_MOBILE
        screenWidth < 600 -> ScreenType.LARGE_MOBILE
        screenWidth < 840 -> ScreenType.TABLET
        else -> ScreenType.LARGE_TABLET
    }
}

/**
 * Returns recommended FlashDrop logo height according to device screen specification:
 * - Small mobile: 32–36 dp (header: 32 dp, card/body: 34 dp)
 * - Normal mobile: 38–42 dp (header: 38 dp, card/body: 40 dp)
 * - Large mobile: 42–46 dp (header: 42 dp, card/body: 44 dp)
 * - Tablet & Large tablet: 44–52 dp (header: 44 dp, card/body: 48 dp)
 */
@Composable
fun getResponsiveLogoHeight(isHeader: Boolean = false): Dp {
    val screenType = rememberScreenType()
    return if (isHeader) {
        when (screenType) {
            ScreenType.SMALL_MOBILE -> 32.dp
            ScreenType.NORMAL_MOBILE -> 38.dp
            ScreenType.LARGE_MOBILE -> 42.dp
            ScreenType.TABLET -> 44.dp
            ScreenType.LARGE_TABLET -> 46.dp
        }
    } else {
        when (screenType) {
            ScreenType.SMALL_MOBILE -> 34.dp
            ScreenType.NORMAL_MOBILE -> 40.dp
            ScreenType.LARGE_MOBILE -> 44.dp
            ScreenType.TABLET -> 48.dp
            ScreenType.LARGE_TABLET -> 50.dp
        }
    }
}

/**
 * Checks if the current configuration is a tablet or desktop-class display.
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
