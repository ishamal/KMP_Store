package com.isharaw.kmpproj.core

/**
 * Bottom-bar presentation for a top-level Navigation 3 destination. Stored in `NavEntry.metadata`
 * under [KEY]; the app shell reads it to build the navigation bar and apply feature gating.
 *
 * [featureId] is the [FeatureId] this tab belongs to: the shell shows the tab only when a resolved
 * Feature with that id is in the user's `ExperienceSnapshot.resolvedFeatures` (null = always shown).
 */
data class TabMeta(
    val label: String,
    val icon: String,
    val order: Int,
    val featureId: FeatureId? = null,
) {
    companion object {
        const val KEY = "tab"
    }
}
