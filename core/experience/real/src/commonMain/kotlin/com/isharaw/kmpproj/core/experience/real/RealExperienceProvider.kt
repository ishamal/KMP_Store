package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceProvider
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.UserRole
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Reads the current user's [ExperienceSnapshot] off the [SessionManager] (it was resolved at login)
 * and answers feature/capability queries against it. Injected as [ExperienceProvider] everywhere.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceProvider(
    private val sessionManager: SessionManager,
) : ExperienceProvider {

    override val snapshot: ExperienceSnapshot?
        get() = sessionManager.session?.snapshot

    override val features: List<Feature>
        get() = snapshot?.resolvedFeatures?.toList().orEmpty()

    override fun capabilitiesOf(featureId: FeatureId): Set<String> =
        features.firstOrNull { it.featureId == featureId }?.capabilities.orEmpty()

    override fun hasFeature(featureId: FeatureId): Boolean =
        features.any { it.featureId == featureId }

    override fun hasCapability(featureId: FeatureId, capability: String): Boolean =
        capability in capabilitiesOf(featureId)

    /**
     * Builds the snapshot for a login by **deriving features from the [capabilities] list**: each
     * capability's prefix names its feature (`"cart.view"` ⇒ CART), so we group the capabilities by
     * [FeatureId] and build one [Feature] per group. Capabilities the app doesn't recognise are ignored.
     */
    override fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole,
        capabilities: Set<String>,
    ): ExperienceSnapshot {
        val resolvedFeatures = capabilities
            .groupBy { FeatureId.fromCapability(it) }
            .mapNotNullTo(mutableSetOf()) { (featureId, caps) ->
                featureId?.let { Feature(it, it.displayName, caps.toSet()) }
            }

        return ExperienceSnapshot(
            experience = experience,
            businessUnit = businessUnit,
            userRoles = userRoles,
            resolvedFeatures = resolvedFeatures,
        )
    }
}
