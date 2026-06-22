package com.isharaw.kmpproj.feature.orders

enum class OrderStatus { PROCESSING, SHIPPED, DELIVERED }

data class Order(
    val number: String,
    val date: String,
    val itemCount: Int,
    val total: Double,
    val status: OrderStatus,
)

/** Public orders contract. Implemented by `RealOrderRepository` in the `:real` module. */
interface OrderRepository {
    fun all(): List<Order>
}
