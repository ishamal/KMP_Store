package com.isharaw.kmpproj.feature.cart

data class CartItem(
    val id: Int,
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
) {
    val lineTotal: Double get() = unitPrice * quantity
}

/** Public cart contract. Implemented by `RealCartRepository` in the `:real` module. */
interface CartRepository {
    val items: List<CartItem>
    val total: Double
    fun increment(id: Int)
    fun decrement(id: Int)
    fun remove(id: Int)
}
