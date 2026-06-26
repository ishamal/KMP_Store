package com.isharaw.kmpproj.core

/**
 * Read access to the current user's [ExperienceSnapshot] and conveniences derived from it (the
 * feature list for the experience, the capability list for a feature). The contract lives in
 * `:core:experience:api`; `RealExperienceProvider` (in `:real`) supplies the snapshot at runtime.
 */
interface ExperienceProvider {

    /** The snapshot resolved for the logged-in user, or `null` before login. */
    val snapshot: ExperienceSnapshot?

    /** The features available in the current experience (empty before login). */
    val features: List<Feature>

    /** The capability list for [featureId] in the current experience (empty if not resolved). */
    fun capabilitiesOf(featureId: FeatureId): Set<String>

    /** True if the current experience has the feature [featureId]. */
    fun hasFeature(featureId: FeatureId): Boolean

    /** True if the current experience grants [capability] within [featureId] (e.g. "order.create"). */
    fun hasCapability(featureId: FeatureId, capability: String): Boolean

    fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole
    ) : ExperienceSnapshot
 }
