package com.isharaw.kmpproj.core

/**
 * Controls runtime Experience and Business-Unit switches that recompute the live
 * [ExperienceSnapshot] without a full logout/login cycle.
 *
 * Each switch recomputes the snapshot from scratch using [StubCapabilities] for the new BU, then
 * loads it into [ExperienceProvider] and updates the [LocalExperienceSnapshot] provided by the app
 * shell. Only [PermissionGate] readers recompose — the back stack and NavDisplay
 * host are stable across switches so the user remains on whichever screen triggered the switch.
 * The [LocalSnapshotController] CompositionLocal exposes the active instance.
 *
 * Null before login (Settings is only reachable after login, so callers never encounter null in
 * practice).
 */
interface SnapshotController {
    /** The business unit currently reflected in the live snapshot. */
    val currentBusinessUnit: BusinessUnit?

    /**
     * The selectable Business Units for [experience]. Empty for experiences that have no BU
     * concept (e.g. GLOMARK); in that case the BU selector is not shown.
     */
    fun availableBusinessUnitsFor(experience: Experience): List<BusinessUnit>

    /**
     * Recomputes the snapshot for [businessUnit] within the current experience and loads it as the
     * new live snapshot. Gates react automatically via [LocalExperienceSnapshot].
     */
    fun switchBusinessUnit(businessUnit: BusinessUnit)

    /**
     * Recomputes the snapshot for [experience], using its configured default BU (from
     * [ExperienceBusinessUnitDefaults]), then loads it as the new live snapshot.
     */
    fun switchExperience(experience: Experience)
}
