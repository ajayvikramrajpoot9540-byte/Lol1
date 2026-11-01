package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.data.repository.ApnaBazRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark

data class CategoryTile(
    val name: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val hindiTitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigate: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit,
    repository: ApnaBazRepository? = null
) {
    val categories = listOf(
        CategoryTile("Grocery", Icons.Default.ShoppingCart, GreenPrimary, GreenLight, "Kirana & Ration"),
        CategoryTile("Printing Press", Icons.Default.Print, GreenPrimary, GreenLight, "Flex, Cards & Print"),
        CategoryTile("Fashion", Icons.Default.Checkroom, OrangeSecondary, OrangeLight, "Kapde & Saree"),
        CategoryTile("Electronics", Icons.Default.Devices, Color(0xFF2563EB), Color(0xFFDBEAFE), "Mobile & Bijli"),
        CategoryTile("Home & Living", Icons.Default.Weekend, Color(0xFF0D9488), Color(0xFFCCFBF1), "Ghar & Kitchen"),
        CategoryTile("Beauty & Personal Care", Icons.Default.Spa, Color(0xFFDB2777), Color(0xFFFCE7F3), "Sringaar & Skin"),
        CategoryTile("Books & Stationery", Icons.Default.Book, Color(0xFF7C3AED), Color(0xFFEDE9FE), "Kitab & Copy"),
        CategoryTile("Food & Sweets", Icons.Default.Fastfood, Color(0xFFD97706), Color(0xFFFEF3C7), "Mithai & Namkeen"),
        CategoryTile("Pharmacy", Icons.Default.LocalPharmacy, Color(0xFFDC2626), Color(0xFFFEE2E2), "Dawa & Swasthya"),
        CategoryTile("Toys & Games", Icons.Default.SportsEsports, Color(0xFF059669), Color(0xFFD1FAE5), "Khilone & Games"),
        CategoryTile("More", Icons.Default.MoreHoriz, Color(0xFF4B5563), Color(0xFFF3F4F6), "Anya Suvidhayein")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Categories / Varg",
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            val allOrders = repository?.orders?.collectAsState()?.value.orEmpty()
            val activeOrders = allOrders.count {
                it.status in listOf("pending", "accepted", "preparing", "out_for_delivery")
            }
            BottomNavBar(
                currentRoute = Screen.Categories.route,
                onNavigate = onNavigate,
                activeOrdersCount = activeOrders,
                hasOrders = allOrders.isNotEmpty()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
        ) {
            Text(
                text = "Explore Mahoba Local Markets",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreenPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable { onCategorySelected(cat.name) }
                            .testTag("category_card_${cat.name.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(cat.bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = cat.iconColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cat.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = cat.hindiTitle,
                                fontSize = 11.sp,
                                color = cat.iconColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
