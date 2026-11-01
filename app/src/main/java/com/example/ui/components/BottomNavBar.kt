package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.navigation.Screen
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    activeOrdersCount: Int = 0,
    hasOrders: Boolean = (activeOrdersCount > 0)
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = GreenPrimary
    ) {
        // Home
        NavigationBarItem(
            modifier = Modifier.testTag("nav_home"),
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary
            )
        )

        // Categories
        NavigationBarItem(
            modifier = Modifier.testTag("nav_categories"),
            selected = currentRoute == Screen.Categories.route,
            onClick = { onNavigate(Screen.Categories.route) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Categories.route) Icons.Filled.Category else Icons.Outlined.Category,
                    contentDescription = "Categories"
                )
            },
            label = { Text("Categories") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary
            )
        )

        // Orders: Only display if the customer has placed an order (or is currently viewing the Orders screen)
        if (hasOrders || currentRoute == Screen.Orders.route) {
            NavigationBarItem(
                modifier = Modifier.testTag("nav_orders"),
                selected = currentRoute == Screen.Orders.route,
                onClick = { onNavigate(Screen.Orders.route) },
                icon = {
                    if (activeOrdersCount > 0) {
                        BadgedBox(badge = { Badge { Text("$activeOrdersCount") } }) {
                            Icon(
                                imageVector = if (currentRoute == Screen.Orders.route) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = "Orders"
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (currentRoute == Screen.Orders.route) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Orders"
                        )
                    }
                },
                label = { Text("Orders") },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GreenLight,
                    selectedIconColor = GreenPrimary,
                    selectedTextColor = GreenPrimary
                )
            )
        }

        // Profile
        NavigationBarItem(
            modifier = Modifier.testTag("nav_profile"),
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screen.Profile.route) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary
            )
        )
    }
}
