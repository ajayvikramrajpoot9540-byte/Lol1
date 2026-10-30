package com.example.data.model

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val printingSpec: PrintingOrderSpec? = null
) {
    val totalPrice: Double
        get() = if (printingSpec != null) {
            printingSpec.printingSubtotal
        } else {
            product.price * quantity
        }
}
