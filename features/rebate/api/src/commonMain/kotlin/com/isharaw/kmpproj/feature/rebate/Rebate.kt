package com.isharaw.kmpproj.feature.rebate

data class Rebate(val label: String, val amount: Double)

/** Rebate loyalty tiers, lowest to highest. */
enum class RebateTier(val displayName: String) {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    PLATINUM("Platinum"),
}

/**
 * The rebate dashboard data shown on the rebate screen. Normally computed by the backend; stubbed in
 * `RealRebateClient` for now.
 *
 * @param monthlyTotal total rebates earned this month
 * @param currentTier the tier the customer is in right now
 * @param currentPoints the customer's current points
 * @param nextTier the tier above [currentTier], or `null` if already at the top
 * @param pointsToNextTier points still needed to reach [nextTier] (0 at the top tier)
 * @param tierProgress how far through the current tier band the customer is, 0f..1f (1f at the top tier)
 */
data class RebateSummary(
    val monthlyTotal: Double,
    val currentTier: RebateTier,
    val currentPoints: Int,
    val nextTier: RebateTier?,
    val pointsToNextTier: Int,
    val tierProgress: Float,
)

/** Public rebate contract. Implemented by `RealRebateRepository` in the `:real` module. */
interface RebateRepository {
    fun available(): List<Rebate>
    fun total(): Double
}

/**
 * Customer-facing rebate actions used by the rebate ViewModel. Implemented by `RealRebateClient` in
 * the `:real` module. [logout] ends the session (returns the app to login).
 */
interface RebateClient {
    /**
     * Loads the rebate dashboard data (monthly total, tier, points to next tier), or `null` if the
     * current user's business unit isn't allowed to view rebates (capability check happens here, in
     * the function layer).
     */
    suspend fun summary(): RebateSummary?

    suspend fun logout()
    fun rebateFunctionOne()
    fun rebateFunctionTwo()
    fun rebateFunctionThree()
}
