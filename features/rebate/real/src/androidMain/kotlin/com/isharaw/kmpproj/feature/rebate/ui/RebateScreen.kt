package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.access.Feature
import com.isharaw.kmpproj.core.access.ui.AccessGate
import com.isharaw.kmpproj.core.formatPrice
import com.isharaw.kmpproj.feature.rebate.RebateSummary

/** Pure, state-driven rebate screen. Buttons send events; the presenter handles them. */
@Composable
fun RebateScreen(state: RebateState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rebates", style = MaterialTheme.typography.headlineSmall)

        // Gated wrapper: identity (business unit + role) comes from the state (passed by the ViewModel);
        // AccessGate checks the capability ∩ permission intersection for REBATE_VIEW and draws the
        // content, the fallback, or nothing. No access guard is read here.
        val businessUnit = state.businessUnit
        val userRole = state.userRole
        if (businessUnit != null && userRole != null) {
            AccessGate(
                businessUnit = businessUnit,
                userRole = userRole,
                feature = Feature.REBATE_VIEW,
                fallback = {
                    Text(
                        "You don't have access to rebates.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val summary = state.summary
                when {
                    state.loading -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }

                    summary != null -> RebateSummaryCard(summary)

                    // loaded but no data → the business unit isn't allowed (REBATE_VIEW capability).
                    else -> Text(
                        "Rebates aren't available for your account.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedButton(
                    onClick = { state.onEvent(RebateEvent.RebateFunctionOneClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Rebate function one") }

                OutlinedButton(
                    onClick = { state.onEvent(RebateEvent.RebateFunctionTwoClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Rebate function two") }

                OutlinedButton(
                    onClick = { state.onEvent(RebateEvent.RebateFunctionThreeClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Rebate function three") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Always available — even without rebate permission, the user can log out.
        Button(
            onClick = { state.onEvent(RebateEvent.LogoutClick) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Log out") }
    }
}

@Composable
private fun RebateSummaryCard(summary: RebateSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total rebates this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPrice(summary.monthlyTotal), fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Current tier", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(summary.currentTier.displayName, fontWeight = FontWeight.Bold)
            }

            val nextTier = summary.nextTier
            if (nextTier != null) {
                Text(
                    "${summary.pointsToNextTier} points to ${nextTier.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { summary.tierProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${summary.currentPoints} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("You're at the top tier 🎉", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
