package com.isharaw.kmpproj.feature.rebate.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.core.access.Capability
import com.isharaw.kmpproj.core.access.withCapability
import com.isharaw.kmpproj.feature.rebate.RebateClient
import com.isharaw.kmpproj.feature.rebate.RebateSummary
import com.isharaw.kmpproj.feature.rebate.RebateTier
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay

/**
 * Stub [RebateClient]. App-scoped so the (customer-scoped) RebateViewModel can inject it from the
 * parent graph. [logout] clears the session, which sends the app back to the login screen.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealRebateClient(
    private val sessionManager: SessionManager,
    private val accessControl: AccessControl,
) : RebateClient {

    override suspend fun summary(): RebateSummary? {
        // FUNCTION-LEVEL capability check: the user's business unit must grant REBATE_VIEW. If not,
        // return null (no data) — the action simply doesn't run. (`withCapability` is inline, so the
        // suspending body below is allowed inside it.)
        val unit = sessionManager.session?.businessUnit ?: return null
        return accessControl.withCapability(unit, Capability.REBATE_VIEW) {
            delay(300) // simulate a network round-trip; replace with a real backend call later

            // Hardcoded stand-in for backend data. The tier and points-to-next are CALCULATED from the
            // customer's points against the tier thresholds (what the backend would do).
            val currentPoints = 320
            val monthlyTotal = 142.50

            val thresholds = listOf(
                RebateTier.BRONZE to 0,
                RebateTier.SILVER to 200,
                RebateTier.GOLD to 500,
                RebateTier.PLATINUM to 1000,
            )
            val current = thresholds.last { currentPoints >= it.second }   // (tier, floor)
            val next = thresholds.firstOrNull { it.second > currentPoints } // (tier, threshold) or null

            val progress = if (next != null) {
                (currentPoints - current.second).toFloat() / (next.second - current.second).toFloat()
            } else {
                1f // already at the top tier
            }

            RebateSummary(
                monthlyTotal = monthlyTotal,
                currentTier = current.first,
                currentPoints = currentPoints,
                nextTier = next?.first,
                pointsToNextTier = next?.let { it.second - currentPoints } ?: 0,
                tierProgress = progress.coerceIn(0f, 1f),
            )
        }
    }

    override suspend fun logout() {
        sessionManager.session = null
    }

    // Placeholders — wire to real rebate operations when the backend exists.
    override fun rebateFunctionOne() {}
    override fun rebateFunctionTwo() {}
    override fun rebateFunctionThree() {}
}
