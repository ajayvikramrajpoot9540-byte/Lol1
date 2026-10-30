package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenPrimaryDark,
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OrangeDark,
    background = BackgroundWarm,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = BorderLight,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF48C78E),
    onPrimary = Color(0xFF00381E),
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = Color(0xFFA5F3CD),
    secondary = Color(0xFFFFB066),
    onSecondary = Color(0xFF4E2600),
    secondaryContainer = OrangeDark,
    onSecondaryContainer = OrangeLight,
    background = Color(0xFF141816),
    onBackground = Color(0xFFE8EDE9),
    surface = Color(0xFF1B211E),
    onSurface = Color(0xFFE8EDE9),
    surfaceVariant = Color(0xFF26302B),
    onSurfaceVariant = Color(0xFFB0BDB5),
    outline = Color(0xFF38463F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun FlashDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ApnaBazTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = FlashDropTheme(darkTheme, content)

