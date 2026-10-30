package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val name: String = "",
    val category: String = "Grocery",
    val subcategory: String = "All",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val unit: String = "1 pc",
    val inStock: Boolean = true,
    val rating: Double = 4.5,
    val ratingCount: Int = 50,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
