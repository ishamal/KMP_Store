package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceResolver
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.UserRole
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * App-scoped resolver — runs at login (before the customer scope exists) to build the snapshot.
 * Derives the features from the flat [capabilities] list by grouping them by [FeatureId].
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceResolver : ExperienceResolver {

    override fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRole: UserRole,
        capabilities: Set<String>,
        permitionList: Set<String>,
    ): ExperienceSnapshot {
        // Effective capabilities = granted AND permitted (only keys in both sets survive).
        val effectiveCapabilities = capabilities intersect permitionList

        val resolvedFeatures = effectiveCapabilities
            .groupBy { FeatureId.fromCapability(it) }
            .mapNotNullTo(mutableSetOf()) { (featureId, caps) ->
                featureId?.let { Feature(it, it.displayName, caps.toSet()) }
            }

        return ExperienceSnapshot(
            experience = experience,
            businessUnit = businessUnit,
            userRoles = userRole,
            resolvedFeatures = resolvedFeatures,
        )
    }
}
