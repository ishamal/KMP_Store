package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceProvider
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.UserRole
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Stateless snapshot builder. Computes an [ExperienceSnapshot] from the inputs resolved at login
 * or on a runtime BU/experience switch. The live snapshot is no longer held here — it is bound
 * non-null inside [ExperienceGraph], recreated per BU/experience switch and dropped at logout.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceProvider : ExperienceProvider {

    override fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole,
        capabilities: Set<String>,
        permission: Set<String>,
    ): ExperienceSnapshot {
        // Effective capabilities = granted AND permitted (only keys in both sets survive).
        val interceptedCapabilities = capabilities intersect permission

        val resolvedFeatures = interceptedCapabilities
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
