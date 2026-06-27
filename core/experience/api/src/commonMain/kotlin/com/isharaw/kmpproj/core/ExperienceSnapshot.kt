package com.isharaw.kmpproj.core

/**
 * The access **snapshot** the backend resolves for a logged-in user (sent down at login). It bundles
 * who the user is and what they may use, so the app doesn't compute access itself — it just queries
 * the snapshot.
 *
 * Access queries ([hasFeature], [capabilitiesOf], [hasCapability]) are **O(1)**: the lookup structures
 * ([grantedCapabilities] and the by-id index) are computed **once per snapshot**, lazily, and reused —
 * so gating many UI items (each recomposition) doesn't re-scan the feature list every time.
 *
 * @param experience the store brand (Keels / Cargills / Glomark).
 * @param businessUnit the user's business unit (CABL / USBL / SENM).
 * @param userRoles the role the user holds.
 * @param resolvedFeatures the features the backend resolved as available to this user.
 */
data class ExperienceSnapshot(
    val experience: Experience,
    val businessUnit: BusinessUnit,
    val userRoles: UserRole,
    val resolvedFeatures: Set<Feature>,
) {
    // Built once per snapshot (not part of equals/hashCode/copy — they're derived from the ctor data).
    private val featuresById: Map<FeatureId, Feature> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedFeatures.associateBy { it.featureId }
    }

    /** Every granted capability flattened across features, e.g. `{"cart.view", "order.create", …}`. */
    val grantedCapabilities: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedFeatures.flatMapTo(mutableSetOf()) { it.capabilities }
    }

    /** True if the feature [featureId] is resolved for this user. */
    fun hasFeature(featureId: FeatureId): Boolean = featureId in featuresById

    /** The capabilities granted within [featureId] (empty if the feature isn't resolved). */
    fun capabilitiesOf(featureId: FeatureId): Set<String> = featuresById[featureId]?.capabilities.orEmpty()

    /** True if [capability] (a dotted key, e.g. `"order.create"`) is granted anywhere in the snapshot. */
    fun hasCapability(capability: String): Boolean = capability in grantedCapabilities

    /** True if [capability] is granted within the feature [featureId]. */
    fun hasCapability(featureId: FeatureId, capability: String): Boolean =
        capability in capabilitiesOf(featureId)
}
