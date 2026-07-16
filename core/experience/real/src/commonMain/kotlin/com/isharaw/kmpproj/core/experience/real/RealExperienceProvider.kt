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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped, **set-later** experience provider backed by a [MutableStateFlow]. Created at app start
 * with no snapshot; the snapshot is pushed in via [load] when the home screen loads and removed via
 * [clear] on logout. Because it's a single app-wide instance, [clear] on logout is what prevents one
 * user's snapshot leaking to the next. Queries delegate to the snapshot's cached O(1) lookups
 * (null-safe before a snapshot is loaded); [snapshotFlow] lets observers react to every swap.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceProvider : ExperienceProvider {

    private val _snapshotFlow = MutableStateFlow<ExperienceSnapshot?>(null)
    override val snapshotFlow: StateFlow<ExperienceSnapshot?> = _snapshotFlow.asStateFlow()

    override val snapshot: ExperienceSnapshot?
        get() = _snapshotFlow.value

    override val features: List<Feature>
        get() = snapshot?.resolvedFeatures?.toList().orEmpty()

    override fun capabilitiesOf(featureId: FeatureId): Set<String> =
        snapshot?.capabilitiesOf(featureId).orEmpty()

    override fun hasFeature(featureId: FeatureId): Boolean = snapshot?.hasFeature(featureId) ?: false

    override fun hasCapability(featureId: FeatureId, capability: String): Boolean =
        snapshot?.hasCapability(featureId, capability) ?: false

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

    override fun load(experienceSnapshot: ExperienceSnapshot) {
        _snapshotFlow.value = experienceSnapshot
    }

    override fun clear() {
        _snapshotFlow.value = null
    }
}
