package com.isharaw.kmpproj.core

/**
 * Read access to the current user's [ExperienceSnapshot] and feature/capability queries.
 *
 * **App-scoped, set-later holder.** It's created at app start (before login), so the snapshot doesn't
 * exist yet — it's pushed in with [load] when the home screen loads, and removed with [clear] on
 * logout. Queries are null-safe (false/empty before a snapshot is loaded). Implemented by
 * `RealExperienceReader`.
 */
interface ExperienceReader {
    /** The snapshot loaded for the current session, or `null` before [load] (e.g. before login). */
    val snapshot: ExperienceSnapshot?

    /** The features available in the current experience (empty before a snapshot is loaded). */
    val features: List<Feature>

    fun hasFeature(featureId: FeatureId): Boolean
    fun capabilitiesOf(featureId: FeatureId): Set<String>
    fun hasCapability(featureId: FeatureId, capability: String): Boolean

    /** Load the snapshot for this session (call when the home screen loads). */
    fun load(snapshot: ExperienceSnapshot)

    /** Drop the snapshot (call on logout). */
    fun clear()
}
