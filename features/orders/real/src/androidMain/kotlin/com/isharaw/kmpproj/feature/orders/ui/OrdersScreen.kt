package com.isharaw.kmpproj.feature.orders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.formatPrice
import com.isharaw.kmpproj.feature.orders.Order
import com.isharaw.kmpproj.feature.orders.OrderRepository
import com.isharaw.kmpproj.feature.orders.OrderStatus

@Composable
fun OrdersScreen(repository: OrderRepository) {
    val orders = remember { repository.all() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Orders", style = MaterialTheme.typography.headlineSmall)

        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(orders, key = { it.number }) { order ->
                OrderRow(order)
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(order.number, fontWeight = FontWeight.Medium)
                Text(
                    "${order.date} · ${order.itemCount} item(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(order.total), fontWeight = FontWeight.Bold)
                Text(
                    order.status.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = order.status.color(),
                )
            }
        }
    }
}

private val OrderStatus.label: String
    get() = when (this) {
        OrderStatus.PROCESSING -> "Processing"
        OrderStatus.SHIPPED -> "Shipped"
        OrderStatus.DELIVERED -> "Delivered"
    }

@Composable
private fun OrderStatus.color(): Color = when (this) {
    OrderStatus.PROCESSING -> MaterialTheme.colorScheme.tertiary
    OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primary
    OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onSurfaceVariant
}
