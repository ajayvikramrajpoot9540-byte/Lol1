package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.GreenPrimary

/**
 * Robust, crash-proof Circular Profile Avatar for FlashDrop.
 *
 * Supports:
 * - Content URIs (from MediaStore / Camera / Photo Picker)
 * - File URIs (file:///data/user/0/... or direct filesystem paths)
 * - Remote URLs (https://firebasestorage.googleapis.com/...)
 *
 * Features:
 * - Circular clipping with ContentScale.Crop (no distortion or stretching)
 * - Smooth loading progress indicator
 * - Graceful fallback to role-specific default vector icons on error, missing files, or empty source
 * - Zero crashes on malformed or unreadable images
 */
@Composable
fun CircularProfileAvatar(
    photoUrl: String?,
    displayName: String = "User",
    role: String = "customer",
    size: Dp = 60.dp,
    tintColor: Color = GreenPrimary,
    backgroundColor: Color = Color.White,
    borderStroke: BorderStroke? = null,
    testTag: String = "profile_user_photo",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedRole = role.lowercase().trim()

    val fallbackIcon: ImageVector = when {
        normalizedRole in listOf("main_admin", "admin") -> Icons.Default.AdminPanelSettings
        normalizedRole == "shopkeeper" -> Icons.Default.Storefront
        else -> Icons.Default.Person
    }

    val trimmedPhoto = photoUrl?.trim().orEmpty()

    Box(
        modifier = modifier
            .size(size)
            .then(if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (trimmedPhoto.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(trimmedPhoto)
                    .crossfade(true)
                    .build(),
                contentDescription = "$displayName Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .testTag(testTag),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size * 0.35f),
                            strokeWidth = 2.dp,
                            color = tintColor
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = "$displayName Default Avatar",
                            tint = tintColor,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    }
                }
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = "$displayName Default Avatar",
                tint = tintColor,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
