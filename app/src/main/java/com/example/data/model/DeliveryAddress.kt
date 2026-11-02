package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

@IgnoreExtraProperties
data class DeliveryAddress(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val countryCode: String = "+91",
    val label: String = "Home", // Home, Work, Friend/Parent's Home, Other
    val houseNumber: String = "",
    val buildingBlock: String = "",
    val street: String = "",
    val area: String = "Subhash Chowk", // Subhash Chowk, Alha Chowk, etc.
    val city: String = "Mahoba",
    val state: String = "Uttar Pradesh",
    val pinCode: String = "210427",
    val landmark: String = "",
    val isDefault: Boolean = false
) {
    fun formatDisplay(): String {
        val parts = mutableListOf<String>()
        if (houseNumber.isNotBlank()) parts.add(houseNumber.trim())
        if (buildingBlock.isNotBlank()) parts.add(buildingBlock.trim())
        if (street.isNotBlank()) parts.add(street.trim())
        if (landmark.isNotBlank()) parts.add("Near ${landmark.trim()}")
        if (area.isNotBlank()) parts.add(area.trim())
        if (city.isNotBlank()) parts.add(city.trim())
        if (state.isNotBlank()) parts.add(state.trim())
        if (pinCode.isNotBlank()) parts.add(pinCode.trim())
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Subhash Chowk, Mahoba, Uttar Pradesh - 210427"
    }

    fun fullContact(): String {
        val n = name.trim()
        val p = phone.trim()
        return when {
            n.isNotBlank() && p.isNotBlank() -> "$n • $countryCode $p"
            n.isNotBlank() -> n
            p.isNotBlank() -> "$countryCode $p"
            else -> ""
        }
    }
}
