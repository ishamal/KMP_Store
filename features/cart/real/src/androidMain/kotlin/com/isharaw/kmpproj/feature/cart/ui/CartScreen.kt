package com.isharaw.kmpproj.feature.cart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.LocalBranding
import com.isharaw.kmpproj.core.formatPrice
import com.isharaw.kmpproj.feature.cart.CartItem
import com.isharaw.kmpproj.feature.cart.CartRepository

@Composable
fun CartScreen(repository: CartRepository) {
    var items by remember { mutableStateOf(repository.items) }
    fun refresh() { items = repository.items }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Per-store wording, read ambiently — value comes from the active flavor's strings.xml.
        Text(LocalBranding.current.welcome, style = MaterialTheme.typography.titleMedium)
        Text("Cart", style = MaterialTheme.typography.headlineSmall)

        if (items.isEmpty()) {
            Text(
                "Your cart is empty.",
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                CartRow(
                    item = item,
                    onIncrement = { repository.increment(item.id); refresh() },
                    onDecrement = { repository.decrement(item.id); refresh() },
                    onRemove = { repository.remove(item.id); refresh() },
                )
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", fontWeight = FontWeight.Bold)
            Text(formatPrice(repository.total), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.name, fontWeight = FontWeight.Medium)
                Text(formatPrice(item.lineTotal))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onDecrement) { Text("-") }
                    Text(item.quantity.toString(), modifier = Modifier.padding(horizontal = 16.dp))
                    OutlinedButton(onClick = onIncrement) { Text("+") }
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
    }
}
