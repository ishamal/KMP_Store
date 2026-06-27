package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.formatPrice

/** Pure, state-driven rebate screen (state comes from RebateViewModel). */
@Composable
fun RebateScreen(state: RebateState) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rebates", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        state.rebates.forEach { rebate ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(rebate.label)
                Text(formatPrice(rebate.amount))
            }
        }

        // Restricted functions — each shown only when the user has the matching rebate capability.
        if (state.canViewDaily || state.canViewTotal) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }

        // rebate.view.daily
        if (state.canViewDaily) {
            val daily = if (state.rebates.isNotEmpty()) state.total / state.rebates.size else 0.0
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Daily average")
                Text(formatPrice(daily))
            }
        }

        // rebate.view.total
        if (state.canViewTotal) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total rebate", fontWeight = FontWeight.Bold)
                Text(formatPrice(state.total), fontWeight = FontWeight.Bold)
            }
        }
    }
}
