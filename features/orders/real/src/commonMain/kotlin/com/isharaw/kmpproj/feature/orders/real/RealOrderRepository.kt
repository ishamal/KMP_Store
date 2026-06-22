package com.isharaw.kmpproj.feature.orders.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.orders.Order
import com.isharaw.kmpproj.feature.orders.OrderRepository
import com.isharaw.kmpproj.feature.orders.OrderStatus
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class RealOrderRepository : OrderRepository {
    override fun all(): List<Order> = listOf(
        Order("ORD-2001", "2026-06-02", itemCount = 3, total = 109.74, status = OrderStatus.DELIVERED),
        Order("ORD-2002", "2026-06-11", itemCount = 1, total = 19.99, status = OrderStatus.SHIPPED),
        Order("ORD-2003", "2026-06-18", itemCount = 5, total = 240.50, status = OrderStatus.PROCESSING),
    )
}
