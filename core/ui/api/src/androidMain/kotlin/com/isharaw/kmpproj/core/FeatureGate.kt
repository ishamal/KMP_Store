package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable

/**
 * **Android (Compose) feature wrapper.** Renders [content] only if [snapshot] has the feature
 * [featureId]; otherwise renders [fallback] (nothing by default). The coarse counterpart of
 * [CapabilityGate] (which checks a single capability) — use this to show/hide a whole feature/section.
 * The tab bar gates the same way in `App.kt` via `snapshot.hasFeature(...)`.
 *
 * ```
 * FeatureGate(snapshot, FeatureId.ORDERS) {
 *     OrdersSummaryCard(...)
 * }
 * ```
 */
@Composable
fun FeatureGate(
    snapshot: ExperienceSnapshot,
    featureId: FeatureId,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (snapshot.hasFeature(featureId)) content() else fallback()
}
