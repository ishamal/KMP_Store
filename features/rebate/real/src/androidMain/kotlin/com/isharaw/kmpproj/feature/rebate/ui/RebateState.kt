package com.isharaw.kmpproj.feature.rebate.ui

import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.UserRole
import com.isharaw.kmpproj.feature.rebate.RebateSummary

/**
 * MVI state for the rebate screen.
 *
 * @param businessUnit / @param userRole the logged-in user's identity, passed from the ViewModel so
 *   the screen's `AccessGate` can do the capability ∩ permission check (no access guard in the UI).
 *   Null only if there's no session (shouldn't happen on this screen).
 * @param loading true while the dashboard data is being fetched.
 * @param summary the monthly total + tier progress once loaded; `null` means the business unit isn't
 *   allowed to view rebates (the REBATE_VIEW *capability* check happens in the function layer).
 * @param onEvent the event sink (the presenter handles events).
 */
data class RebateState(
    val businessUnit: BusinessUnit?,
    val userRole: UserRole?,
    val loading: Boolean,
    val summary: RebateSummary?,
    val onEvent: (RebateEvent) -> Unit,
)

sealed interface RebateEvent {
    data object LogoutClick : RebateEvent
    data object RebateFunctionOneClick : RebateEvent
    data object RebateFunctionTwoClick : RebateEvent
    data object RebateFunctionThreeClick : RebateEvent
}
