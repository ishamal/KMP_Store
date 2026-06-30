package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.ExperienceReader
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * App-scoped, **set-later** experience reader. Created at app start with no snapshot; the snapshot is
 * pushed in via [load] when the home screen loads and removed via [clear] on logout. Because it's a
 * single app-wide instance, [clear] on logout is what prevents one user's snapshot leaking to the next.
 * Queries delegate to the snapshot's cached O(1) lookups (null-safe before a snapshot is loaded).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceReader : ExperienceReader {

    private var current: ExperienceSnapshot? = null

    override val snapshot: ExperienceSnapshot?
        get() = current

    override val features: List<Feature>
        get() = current?.resolvedFeatures?.toList().orEmpty()

    override fun hasFeature(featureId: FeatureId): Boolean = current?.hasFeature(featureId) ?: false

    override fun capabilitiesOf(featureId: FeatureId): Set<String> =
        current?.capabilitiesOf(featureId).orEmpty()

    override fun hasCapability(featureId: FeatureId, capability: String): Boolean =
        current?.hasCapability(featureId, capability) ?: false

    override fun load(snapshot: ExperienceSnapshot) { current = snapshot }

    override fun clear() { current = null }
}
