package com.isharaw.kmpproj

import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.di.createIosAppGraph
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that the iOS DI graph actually aggregates `IosFeatureAction` contributions from linked
 * `:real` modules. A silently-empty set (e.g. Metro not picking up iosMain contributions) would
 * pass every compile/link check but break the Settings UI — this test catches that case.
 *
 * Runs against the DEFAULT store (storeA = iosStoreWithInvoices), which ships both rebate and
 * passwordReset. Per-store variations are intentional; only storeA ships both kinds.
 */
class IosFeatureActionsTest {

    @Test
    fun featureActionsAreAggregated() {
        val actions = createIosAppGraph().featureActions

        assertTrue(actions.isNotEmpty(), "featureActions must not be empty for storeA — Metro did not aggregate iosMain contributions")

        val kinds = actions.map { it.kind }.toSet()
        assertTrue(FeatureKind.REBATE in kinds, "REBATE action must be contributed by features:rebate:real")
        assertTrue(FeatureKind.PASSWORD_RESET in kinds, "PASSWORD_RESET action must be contributed by features:passwordReset:real")
    }

    @Test
    fun featureActionsTargetSettingsSlot() {
        val actions = createIosAppGraph().featureActions
        assertTrue(actions.all { it.slot == FeatureSlot.SETTINGS }, "All contributed actions must target the SETTINGS slot")
    }

    @Test
    fun featureActionsAreSortedByOrder() {
        val actions = createIosAppGraph().featureActions
            .filter { it.slot == FeatureSlot.SETTINGS }
            .sortedBy { it.order }
        // password reset (order=0) must come before rebates (order=10)
        val passwordResetIndex = actions.indexOfFirst { it.kind == FeatureKind.PASSWORD_RESET }
        val rebateIndex = actions.indexOfFirst { it.kind == FeatureKind.REBATE }
        assertTrue(passwordResetIndex < rebateIndex, "PASSWORD_RESET (order=0) must sort before REBATE (order=10)")
    }
}
