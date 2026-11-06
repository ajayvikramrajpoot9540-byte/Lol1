package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopLiveStatus
import com.example.ui.theme.GoldRating
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

/**
 * Data representation of an offer from a specific Mahoba shop for the price comparison card.
 */
data class ShopPriceOffer(
    val product: Product,
    val shop: Shop,
    val liveStatus: ShopLiveStatus?,
    val isCurrentShop: Boolean,
    val isLowestPrice: Boolean,
    val isLowestTotal: Boolean,
    val totalCost: Double,
    val priceDiffFromCurrent: Double // negative means cheaper than current
)

/**
 * Matches comparable products across local Mahoba shops.
 */
object MultiShopComparisonMatcher {

    private val KEYWORD_GROUPS = listOf(
        setOf("atta", "flour", "gehu", "sharbati", "wheat"),
        setOf("biscuit", "biscuits", "parle-g", "parle", "glucose"),
        setOf("maggi", "noodles", "instant"),
        setOf("butter", "makhan", "amul"),
        setOf("salt", "namak", "tata"),
        setOf("oil", "tel", "refined", "sunlite", "mustard"),
        setOf("rice", "chawal", "basmati"),
        setOf("kurti", "cotton", "suit", "ethnic"),
        setOf("shirt", "linen", "formal"),
        setOf("saree", "silk", "chanderi"),
        setOf("earphone", "earphones", "bassheads", "headphone"),
        setOf("charger", "cable", "adapter", "fast"),
        setOf("powerbank", "power bank", "battery"),
        setOf("pedha", "peda", "khoya", "sweet"),
        setOf("gulab jamun", "gulabjamun", "rasgulla"),
        setOf("samosa", "jalebi", "kachori"),
        setOf("ghee", "desi ghee", "bilona"),
        setOf("dolo", "paracetamol", "crocin"),
        setOf("chyawanprash", "immunity", "dabur"),
        setOf("notebook", "copy", "register", "classmate"),
        setOf("visiting card", "visiting cards", "card"),
        setOf("flex", "banner", "poster"),
        setOf("print", "printing", "document", "xerox", "photo printing"),
        setOf("binding", "lamination", "spiral")
    )

    fun findOffers(
        currentProduct: Product,
        allProducts: List<Product>,
        allShops: List<Shop>,
        getLiveStatus: (String) -> ShopLiveStatus?
    ): List<ShopPriceOffer> {
        val currentShopId = currentProduct.shopId
        val targetNameLower = currentProduct.name.lowercase()

        // Identify matched keyword group
        val matchedGroup = KEYWORD_GROUPS.firstOrNull { group ->
            group.any { targetNameLower.contains(it) }
        }

        // Group candidate products by shop
        val candidatesByShop = allProducts
            .filter { it.category.equals(currentProduct.category, ignoreCase = true) ||
                    (currentProduct.subcategory.isNotBlank() && it.subcategory.equals(currentProduct.subcategory, ignoreCase = true)) ||
                    (matchedGroup != null && matchedGroup.any { k -> it.name.lowercase().contains(k) })
            }
            .groupBy { it.shopId }

        val offers = mutableListOf<ShopPriceOffer>()

        // 1. Current Shop offer
        val currentShop = allShops.find { it.id == currentShopId }
            ?: Shop(id = currentShopId, name = currentProduct.shopName, deliveryFee = 20.0, rating = 4.8, distance = "1.2 km")
        val currentLive = getLiveStatus(currentShopId)
        val currentTotal = currentProduct.price + currentShop.deliveryFee

        // 2. Candidate other shops
        val otherShops = allShops.filter { it.id != currentShopId }

        val matchedPerShop = mutableMapOf<String, Product>()
        for (shop in otherShops) {
            val shopProds = candidatesByShop[shop.id] ?: emptyList()
            if (shopProds.isEmpty()) continue

            // Pick closest matching product
            val bestProd = shopProds.minByOrNull { cand ->
                var score = 0
                val candName = cand.name.lowercase()
                if (matchedGroup != null && matchedGroup.any { candName.contains(it) }) score -= 50
                if (cand.subcategory.equals(currentProduct.subcategory, ignoreCase = true)) score -= 20
                if (cand.inStock) score -= 10
                score += (cand.price / 10).toInt()
                score
            }
            if (bestProd != null) {
                matchedPerShop[shop.id] = bestProd
            }
        }

        // If fewer than 2 other shops matched, check same category shops
        if (matchedPerShop.size < 2) {
            val sameCategoryShops = allShops.filter {
                it.id != currentShopId &&
                        it.category.equals(currentProduct.category, ignoreCase = true) &&
                        !matchedPerShop.containsKey(it.id)
            }
            for (shop in sameCategoryShops) {
                val anyProd = allProducts.filter { it.shopId == shop.id }.minByOrNull { it.price }
                if (anyProd != null) {
                    matchedPerShop[shop.id] = anyProd
                }
            }
        }

        val allCandidatePairs = mutableListOf<Pair<Product, Shop>>()
        allCandidatePairs.add(currentProduct to currentShop)
        for ((sId, prod) in matchedPerShop) {
            val shop = allShops.find { it.id == sId } ?: continue
            allCandidatePairs.add(prod to shop)
        }

        val minPrice = allCandidatePairs.minOfOrNull { it.first.price } ?: currentProduct.price
        val minTotal = allCandidatePairs.minOfOrNull { it.first.price + it.second.deliveryFee } ?: currentTotal

        for ((prod, shop) in allCandidatePairs) {
            val isCurrent = prod.id == currentProduct.id && shop.id == currentShopId
            val total = prod.price + shop.deliveryFee
            val live = getLiveStatus(shop.id)
            offers.add(
                ShopPriceOffer(
                    product = prod,
                    shop = shop,
                    liveStatus = live,
                    isCurrentShop = isCurrent,
                    isLowestPrice = prod.price <= minPrice,
                    isLowestTotal = total <= minTotal,
                    totalCost = total,
                    priceDiffFromCurrent = prod.price - currentProduct.price
                )
            )
        }

        // Sort: lowest item price first, with current shop ranked fairly
        return offers.sortedWith(
            compareBy<ShopPriceOffer> { !it.product.inStock }
                .thenBy { it.product.price }
                .thenBy { it.totalCost }
        )
    }
}

/**
 * ⚖️ Multi-Shop Price Comparison Card on Product Details:
 * Displays comparison list of all local Mahoba shops selling this item or comparable goods side-by-side.
 */
@Composable
fun MultiShopPriceComparisonCard(
    currentProduct: Product,
    allProducts: List<Product>,
    allShops: List<Shop>,
    getLiveStatus: (String) -> ShopLiveStatus?,
    onSelectProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val offers = remember(currentProduct, allProducts, allShops) {
        MultiShopComparisonMatcher.findOffers(currentProduct, allProducts, allShops, getLiveStatus)
    }

    if (offers.size <= 1 && offers.firstOrNull()?.isCurrentShop == true) {
        // Only one shop exists for this niche, render reassurance card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("single_shop_exclusive_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚖️ Mahoba Exclusive Deal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Exclusively sold by ${currentProduct.shopName} at verified best local rate.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
        return
    }

    var isExpanded by remember { mutableStateOf(true) }
    val lowestOffer = offers.firstOrNull { it.isLowestPrice }
    val maxSavings = offers.maxOfOrNull { it.product.price }?.minus(lowestOffer?.product?.price ?: 0.0) ?: 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_shop_price_comparison_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Gradient Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFECFDF5), Color(0xFFFFFBEB))
                        )
                    )
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚖️ Mahoba Multi-Shop Price Compare",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${offers.size} Stores",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary
                                    )
                                }
                            }
                            Text(
                                text = if (maxSavings > 0) "Compare local shops side-by-side • Save up to ₹${maxSavings.toInt()}"
                                else "Compare local prices, ratings & delivery fees side-by-side",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextMuted
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Comparison table sub-header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL MAHOBA SHOPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "ITEM + DELIVERY = NET TOTAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Shop Comparison Rows
                    offers.forEach { offer ->
                        ShopComparisonRow(
                            offer = offer,
                            onSwitchStore = { onSelectProduct(offer.product) },
                            onAddToCart = { onAddToCart(offer.product) }
                        )
                    }

                    // Bottom Guarantee Note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "All prices verified from direct Mahoba shopkeeper billing with 30-min doorstep delivery.",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopComparisonRow(
    offer: ShopPriceOffer,
    onSwitchStore: () -> Unit,
    onAddToCart: () -> Unit
) {
    val isCheapest = offer.isLowestPrice
    val isCurrent = offer.isCurrentShop

    val borderColor = when {
        isCurrent -> GreenPrimary
        isCheapest -> Color(0xFF10B981)
        else -> Color(0xFFE2E8F0)
    }

    val bgColor = when {
        isCurrent -> Color(0xFFF0FDF4)
        isCheapest -> Color(0xFFF7FEE7)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                border = BorderStroke(if (isCurrent || isCheapest) 1.5.dp else 1.dp, borderColor),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Badges & Shop Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.shop.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isCheapest) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF15803D))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ Lowest Price",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Viewing",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Distance, Rating & Live Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rating
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldRating,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${offer.shop.rating} (${offer.shop.ratingCount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(text = " • ", fontSize = 11.sp, color = TextMuted)
                    // Distance
                    Text(text = offer.shop.distance, fontSize = 11.sp, color = TextMuted)
                }

                // Live status badge
                val isOpen = offer.liveStatus?.isOpen ?: offer.shop.isOpen
                val statusText = if (isOpen) {
                    "OPEN • ${offer.liveStatus?.expectedDeliveryEstimate ?: offer.shop.deliveryTime}"
                } else {
                    "CLOSED (${offer.liveStatus?.nextOpeningTime ?: "Opens 09:00 AM"})"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOpen) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) Color(0xFF166534) else Color(0xFFDC2626)
                    )
                }
            }

            // Variant product name if different
            if (offer.product.name != offer.shop.name && offer.product.name.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${offer.product.name} (${offer.product.unit})",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Price comparison & Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Price Details: Item Price + Delivery Fee = Total
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${offer.product.price.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCheapest) Color(0xFF15803D) else GreenPrimary
                        )
                        if (offer.product.originalPrice > offer.product.price) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "₹${offer.product.originalPrice.toInt()}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (offer.shop.deliveryFee == 0.0) "Free Delivery" else "+ ₹${offer.shop.deliveryFee.toInt()} Delivery",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(text = " = ", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = "₹${offer.totalCost.toInt()} Total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                // Actions: Switch or Add to Cart
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Selected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSwitchStore,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("switch_to_store_${offer.shop.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GreenPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Switch (₹${offer.product.price.toInt()})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }

                        Button(
                            onClick = onAddToCart,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_from_store_${offer.shop.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+ Add",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
