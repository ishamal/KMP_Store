package com.isharaw.kmpproj.feature.invoices.ui

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.Capability
import com.isharaw.kmpproj.core.CapabilityGated
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.allowedFor
import com.isharaw.kmpproj.core.formatPrice
import com.isharaw.kmpproj.feature.invoices.Invoice
import com.isharaw.kmpproj.feature.invoices.InvoiceRepository
import com.isharaw.kmpproj.feature.invoices.InvoiceStatus
import com.isharaw.kmpproj.feature.invoices.invoicesFor

/** A toolbar action gated by a capability. Add a row to gate a new action — no extra `if`. */
private data class InvoiceAction(
    val label: String,
    override val capability: Capability,
    val onClick: () -> Unit,
) : CapabilityGated

@Composable
fun InvoicesScreen(repository: InvoiceRepository, experience: Experience) {
    val invoices = remember(experience) { repository.invoicesFor(experience) }

    // All toolbar actions declared once with the capability each needs; filtered generically.
    val actions = remember(experience) {
        listOf(
            InvoiceAction("Export", Capability.EXPORT_INVOICES) { /* export */ },
            // future actions go here — no new `if`
        ).allowedFor(experience)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Invoices", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) { Text(action.label) }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(invoices, key = { it.number }) { invoice ->
                InvoiceRow(invoice)
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(invoice.number, fontWeight = FontWeight.Medium)
                Text(
                    invoice.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(invoice.amount), fontWeight = FontWeight.Bold)
                Text(
                    invoice.status.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = invoice.status.color(),
                )
            }
        }
    }
}

private val InvoiceStatus.label: String
    get() = when (this) {
        InvoiceStatus.PAID -> "Paid"
        InvoiceStatus.PENDING -> "Pending"
        InvoiceStatus.OVERDUE -> "Overdue"
    }

@Composable
private fun InvoiceStatus.color(): Color = when (this) {
    InvoiceStatus.PAID -> MaterialTheme.colorScheme.primary
    InvoiceStatus.PENDING -> MaterialTheme.colorScheme.tertiary
    InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.error
}
