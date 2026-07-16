package com.isharaw.kmpproj.core

import kotlinx.coroutines.flow.StateFlow

/**
 * The single access point for the current user's [ExperienceSnapshot]: it **builds** the snapshot
 * ([getExperienceSnapshot]), **holds** it for the session ([load]/[clear]) and **answers** the
 * feature/capability queries — replacing the former ExperienceReader/ExperienceResolver split.
 *
 * **App-scoped, set-later holder.** It's created at app start (before login), so the snapshot doesn't
 * exist yet — it's pushed in with [load] when the home screen loads, and removed with [clear] on
 * logout. Queries are null-safe (false/empty before a snapshot is loaded).
 *
 * [snapshotFlow] exposes the live snapshot reactively, so non-Compose observers (coroutines,
 * ViewModels) can react to login/logout and runtime BU/experience switches. Implemented by
 * `RealExperienceProvider`.
 */
interface ExperienceProvider {

    /** The snapshot loaded for the current session, or `null` before [load] (e.g. before login). */
    val snapshot: ExperienceSnapshot?

    /** Reactive view of [snapshot]: emits on [load]/[clear] (login, logout, BU/experience switch). */
    val snapshotFlow: StateFlow<ExperienceSnapshot?>

    /** The features available in the current experience (empty before a snapshot is loaded). */
    val features: List<Feature>

    fun capabilitiesOf(featureId: FeatureId): Set<String>

    fun hasFeature(featureId: FeatureId): Boolean

    fun hasCapability(featureId: FeatureId, capability: String): Boolean

    /**
     * Builds the snapshot for a login or a runtime BU/experience switch. The **effective**
     * capabilities are the intersection of [capabilities] (what the user is granted) and
     * [permission] (what is permitted) — only keys present in **both** are kept. Features are then
     * derived from that effective set (a capability's prefix names its feature, e.g. `"cart.view"`
     * ⇒ the CART feature). Pure — does **not** [load] the result.
     */
    fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole,
        capabilities: Set<String>,
        permission: Set<String>,
    ): ExperienceSnapshot

    /** Load the snapshot for this session (call when the home screen loads). */
    fun load(experienceSnapshot: ExperienceSnapshot)

    /** Drop the snapshot (call on logout). */
    fun clear()
}
