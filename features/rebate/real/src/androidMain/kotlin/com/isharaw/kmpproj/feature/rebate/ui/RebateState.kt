package com.isharaw.kmpproj.feature.rebate.ui

import com.isharaw.kmpproj.feature.rebate.Rebate

/** MVI state for the rebate screen. */
data class RebateState(
    val rebates: List<Rebate>,
    val total: Double,
    val onEvent: (RebateEvent) -> Unit,
)

sealed interface RebateEvent {
    data object Refresh : RebateEvent
}
