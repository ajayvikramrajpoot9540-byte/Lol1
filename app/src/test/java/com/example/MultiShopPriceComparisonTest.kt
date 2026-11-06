package com.example

import com.example.data.local.SeedData
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.ui.components.MultiShopComparisonMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiShopPriceComparisonTest {

    @Test
    fun testAttaMultiShopComparisonFindsAlternativeMahobaShops() {
        val currentAtta = SeedData.sampleProducts.first { it.id == "prod_atta" }
        assertEquals("Sharma General Store", currentAtta.shopName)

        val offers = MultiShopComparisonMatcher.findOffers(
            currentProduct = currentAtta,
            allProducts = SeedData.sampleProducts,
            allShops = SeedData.sampleShops,
            getLiveStatus = { null }
        )

        // Multiple local shops found
        assertTrue("Should find at least 2 shop offers for Atta in Mahoba", offers.size >= 2)

        // Current shop exists in offers
        val currentOffer = offers.find { it.isCurrentShop }
        assertNotNull("Current shop offer must be present", currentOffer)
        assertEquals(currentAtta.id, currentOffer?.product?.id)

        // Lowest price shop identified
        val lowestOffer = offers.find { it.isLowestPrice }
        assertNotNull("Lowest price offer must be identified", lowestOffer)
        assertTrue(
            "Lowest price offer must have price <= all other offers",
            offers.all { it.product.price >= lowestOffer!!.product.price }
        )

        // Net total cost calculation check (price + delivery fee)
        for (offer in offers) {
            val expectedTotal = offer.product.price + offer.shop.deliveryFee
            assertEquals(expectedTotal, offer.totalCost, 0.001)
        }
    }

    @Test
    fun testButterMultiShopComparisonRanksCheapestFirst() {
        val currentButter = SeedData.sampleProducts.first { it.id == "prod_butter" }

        val offers = MultiShopComparisonMatcher.findOffers(
            currentProduct = currentButter,
            allProducts = SeedData.sampleProducts,
            allShops = SeedData.sampleShops,
            getLiveStatus = { null }
        )

        assertTrue(offers.size >= 2)
        // First in-stock item should be lowest price
        assertEquals(offers.minOf { it.product.price }, offers.first().product.price, 0.001)
    }
}
