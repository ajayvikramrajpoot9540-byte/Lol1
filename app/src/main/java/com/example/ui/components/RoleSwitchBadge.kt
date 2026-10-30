package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextMuted

/**
 * Compact, icon-only Role Switcher for FlashDrop header.
 *
 * Displays only icons representing Customer (person), Shopkeeper (storefront),
 * and Admin (shield/admin). Tapping opens a clean popup with 3 icon-only options
 * with visible selected-state indicator and accessibility descriptions.
 *
 * Enforces strict security: Selecting Shopkeeper or Admin without verified
 * credentials directs the user to authenticate rather than escalating permissions.
 */
@Composable
fun RoleSwitchBadge(
    currentRole: String,
    isVerifiedAdmin: Boolean = false,
    isVerifiedShopkeeper: Boolean = false,
    onRoleSelected: ((String) -> Unit)? = null,
    onOpenShopRegistration: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onOpenAuth: (() -> Unit)? = null,
    onNavigateToDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val isCustomer = currentRole.equals("customer", ignoreCase = true) ||
            currentRole.isBlank() ||
            currentRole.equals("guest", ignoreCase = true)
    val isShopkeeper = currentRole.equals("shopkeeper", ignoreCase = true)
    val isAdmin = currentRole.equals("MAIN_ADMIN", ignoreCase = true) ||
            currentRole.equals("admin", ignoreCase = true)

    val (activeIcon, activeColor, activeBg, activeDescription) = when {
        isAdmin -> Tuple4(Icons.Default.AdminPanelSettings, Color(0xFF6D28D9), Color(0xFFEDE9FE), "Admin role")
        isShopkeeper -> Tuple4(Icons.Default.Storefront, OrangeSecondary, OrangeLight, "Shopkeeper role")
        else -> Tuple4(Icons.Default.Person, GreenPrimary, GreenLight, "Customer role")
    }

    Box(modifier = modifier) {
        // Compact Icon-Only Role Selector Button
        Card(
            modifier = Modifier
                .testTag("role_switcher_pill")
                .semantics {
                    contentDescription = activeDescription
                    role = Role.Button
                }
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Active Role Icon (No visible text)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(activeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeIcon,
                        contentDescription = activeDescription,
                        tint = activeColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Small arrow indicator showing popup affordance
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change role",
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Small, Clean Icon-Only Popup with 3 Options
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-40).dp, y = 4.dp),
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, BorderLight), RoundedCornerShape(16.dp))
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Customer Option (Person Icon)
                RoleIconOption(
                    icon = Icons.Default.Person,
                    accessibilityLabel = "Customer role",
                    isSelected = isCustomer,
                    activeColor = GreenPrimary,
                    activeBg = GreenLight,
                    onClick = {
                        expanded = false
                        onRoleSelected?.invoke("customer")
                    }
                )

                // 2. Shopkeeper Option (Store Icon)
                RoleIconOption(
                    icon = Icons.Default.Storefront,
                    accessibilityLabel = "Shopkeeper role",
                    isSelected = isShopkeeper,
                    activeColor = OrangeSecondary,
                    activeBg = OrangeLight,
                    onClick = {
                        expanded = false
                        if (isVerifiedShopkeeper || isShopkeeper) {
                            onRoleSelected?.invoke("shopkeeper")
                            onNavigateToDashboard?.invoke()
                        } else {
                            // Security: Customer cannot gain Shopkeeper access without credentials
                            onOpenAuth?.invoke() ?: onOpenShopRegistration?.invoke()
                        }
                    }
                )

                // 3. Admin Option (Shield / Admin Icon)
                RoleIconOption(
                    icon = Icons.Default.AdminPanelSettings,
                    accessibilityLabel = "Admin role",
                    isSelected = isAdmin,
                    activeColor = Color(0xFF6D28D9),
                    activeBg = Color(0xFFEDE9FE),
                    onClick = {
                        expanded = false
                        if (isVerifiedAdmin || isAdmin) {
                            onRoleSelected?.invoke("MAIN_ADMIN")
                            onNavigateToDashboard?.invoke()
                        } else {
                            // Security: Customer cannot gain Admin access without verifying credentials
                            onOpenAuth?.invoke()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual Icon-Only Role Option within the popup.
 * Provides selected-state visual indicator, accessibility label, and ripple feedback.
 */
@Composable
private fun RoleIconOption(
    icon: ImageVector,
    accessibilityLabel: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeBg else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) activeColor else BorderLight,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .semantics {
                contentDescription = accessibilityLabel
                role = Role.Button
            }
            .testTag("role_option_${accessibilityLabel.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = accessibilityLabel,
            tint = if (isSelected) activeColor else TextMuted,
            modifier = Modifier.size(22.dp)
        )

        // Visible Selected-State Indicator Dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}

// Backward-compatible overload for legacy signatures
@Composable
fun RoleSwitchBadge(
    currentRole: String,
    loginId: String = "",
    onRoleSelected: (role: String, shopId: String?, loginId: String?) -> Unit,
    onOpenLoginModal: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isVerified = currentRole.equals("MAIN_ADMIN", ignoreCase = true) ||
            currentRole.equals("admin", ignoreCase = true) ||
            currentRole.equals("shopkeeper", ignoreCase = true)

    RoleSwitchBadge(
        currentRole = currentRole,
        isVerifiedAdmin = currentRole.equals("MAIN_ADMIN", ignoreCase = true) || currentRole.equals("admin", ignoreCase = true),
        isVerifiedShopkeeper = currentRole.equals("shopkeeper", ignoreCase = true),
        onRoleSelected = { role -> onRoleSelected(role, null, null) },
        onOpenAuth = onOpenLoginModal,
        modifier = modifier
    )
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
