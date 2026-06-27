package com.isharaw.kmpproj.feature.rebate.ui

import com.isharaw.kmpproj.feature.rebate.Rebate

/**
 * MVI state for the rebate screen. [canViewTotal] / [canViewDaily] are the per-function restrictions
 * (computed in the ViewModel from the user's rebate capabilities), so the screen reads plain booleans.
 */
data class RebateState(
    val rebates: List<Rebate>,
    val total: Double,
    val canViewTotal: Boolean,
    val canViewDaily: Boolean,
    val onEvent: (RebateEvent) -> Unit,
)

sealed interface RebateEvent {
    data object Refresh : RebateEvent
}
