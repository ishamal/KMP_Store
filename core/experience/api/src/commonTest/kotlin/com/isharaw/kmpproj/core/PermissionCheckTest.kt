package com.isharaw.kmpproj.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the shared [permits] gate rule — the single access decision both the Android
 * `PermissionGate` and the iOS SwiftUI gates rely on — over both its [String] and [Capability] forms.
 */
class PermissionCheckTest {

    private fun snapshot(vararg features: Feature) = ExperienceSnapshot(
        experience = Experience.KEELS,
        businessUnit = BusinessUnit.USBL,
        userRoles = UserRole.USER,
        resolvedFeatures = features.toSet(),
    )

    /** REBATE resolved with the full set of rebate capabilities. */
    private val rebate = Feature(
        featureId = FeatureId.REBATE,
        featureName = "Rebate",
        capabilities = setOf("rebate.view", "rebate.view.daily", "rebate.view.total"),
    )

    /** REBATE resolved, but only the baseline `.view` capability. */
    private val rebateViewOnly = Feature(FeatureId.REBATE, "Rebate", setOf("rebate.view"))

    // --- String form: the primary implementation + backend/dynamic path ---

    @Test
    fun granted_whenFeatureResolvedAndCapabilityPresent() {
        assertTrue(snapshot(rebate).permits("rebate.view"))
    }

    @Test
    fun subCapability_inheritsParentFeatureCheck() {
        assertTrue(snapshot(rebate).permits("rebate.view.total"))
    }

    @Test
    fun denied_whenFeatureResolvedButCapabilityMissing() {
        assertFalse(snapshot(rebateViewOnly).permits("rebate.view.total"))
    }

    @Test
    fun denied_whenFeatureNotResolved() {
        // No ORDERS feature in the snapshot → the derived ORDERS feature check fails.
        assertFalse(snapshot(rebate).permits("order.view"))
    }

    @Test
    fun nullCapability_isUngated_alwaysGranted() {
        assertTrue(snapshot(rebate).permits(null as String?))
    }

    @Test
    fun nullCapability_grantedEvenForNullSnapshot() {
        val none: ExperienceSnapshot? = null
        assertTrue(none.permits(null as String?))
    }

    @Test
    fun nullSnapshot_deniesAnyRealCapability() {
        val none: ExperienceSnapshot? = null
        assertFalse(none.permits("rebate.view"))
    }

    @Test
    fun unmappedPrefix_checksCapabilityOnly_granted() {
        // "delivery.*" maps to no FeatureId → the feature check short-circuits and only the
        // capability check applies. Granted here because some resolved feature exposes it.
        val settingsWithDelivery = Feature(FeatureId.SETTINGS, "Settings", setOf("delivery.widget.view"))
        assertTrue(snapshot(settingsWithDelivery).permits("delivery.widget.view"))
    }

    @Test
    fun unmappedPrefix_deniedWhenCapabilityAbsent() {
        assertFalse(snapshot(rebate).permits("delivery.widget.view"))
    }

    // --- Capability (enum) form: same rule, compile-checked call sites ---

    @Test
    fun enumOverload_matchesStringPath() {
        val snap = snapshot(rebate)
        assertTrue(snap.permits(Capability.REBATE_VIEW_TOTAL))
        assertEquals(
            snap.permits(Capability.REBATE_VIEW_TOTAL.value),
            snap.permits(Capability.REBATE_VIEW_TOTAL),
        )
    }

    @Test
    fun enumOverload_deniedWhenCapabilityMissing() {
        assertFalse(snapshot(rebateViewOnly).permits(Capability.REBATE_VIEW_TOTAL))
    }

    @Test
    fun enumOverload_nullIsUngated() {
        val none: ExperienceSnapshot? = null
        assertTrue(none.permits(null as Capability?))
    }

    @Test
    fun capabilityFrom_roundTripsWireValue() {
        assertEquals(Capability.REBATE_VIEW_TOTAL, Capability.from("rebate.view.total"))
        assertNull(Capability.from("not.a.capability"))
    }
}
