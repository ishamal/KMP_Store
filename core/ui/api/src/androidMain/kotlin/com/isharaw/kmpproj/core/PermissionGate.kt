package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable

/**
 * **Android (Compose) permission wrapper.** The single gate for permission-based UI — it replaces the
 * former `FeatureGate` (feature check) and `CapabilityGate` (capability check) with one API that does
 * both from a single [capability].
 *
 * [capability] is a type-safe [Capability] entry (e.g. [Capability.ORDER_VIEW],
 * [Capability.REBATE_VIEW_TOTAL]). The allow/deny decision is the shared [permits] rule (common to
 * Android and iOS): it derives a [FeatureId] from the capability's prefix and requires both that the
 * [snapshot] resolves that feature **and** grants the exact capability. A more-specific capability
 * (e.g. [Capability.REBATE_VIEW_TOTAL]) still inherits the parent feature check because the prefix is
 * unchanged. Passing `capability = null` disables gating entirely ([content] always shows) — use that
 * only for unconditional render.
 *
 * [content] receives the compiled-in [FeatureAction] whose [FeatureAction.featureId] matches the
 * capability's feature, resolved from [features] — or `null` when no linked module contributes one
 * (e.g. gating a sub-widget, or a feature that exposes no host action). This lets a host surface render
 * the feature's action (label/target) without depending on the feature module. **The action is not
 * part of the allow/deny decision** — visibility is `snapshot.permits(capability)` alone, so a feature
 * with no action still gates correctly.
 *
 * [snapshot] defaults to [LocalExperienceSnapshot.current] and [features] to [LocalFeatureActions.current]
 * — call sites inside an authenticated session scope (where the app shell provides the locals) can omit
 * both. Pass explicit values to override (e.g. Compose previews or tests). If [snapshot] is null (local
 * not provided or no active session) nothing is granted and [fallback] (nothing by default) is shown.
 *
 * ```
 * // Host surface — render the feature's action when granted:
 * PermissionGate(capability = Capability.REBATE_VIEW) { action ->
 *     action?.let { FeatureButton(it) }
 * }
 *
 * // Sub-widget gate — action unused:
 * PermissionGate(capability = Capability.REBATE_VIEW_TOTAL) {
 *     RebateTotalRow(...)
 * }
 * ```
 */
@Composable
fun PermissionGate(
    capability: Capability? = null,
    snapshot: ExperienceSnapshot? = LocalExperienceSnapshot.current,
    features: Set<FeatureAction> = LocalFeatureActions.current,
    fallback: @Composable () -> Unit = {},
    content: @Composable (FeatureAction?) -> Unit,
) {
    val featureId = capability?.let { FeatureId.fromCapability(it.value) }
    val action = featureId?.let { id -> features.firstOrNull { it.featureId == id } }
    if (snapshot.permits(capability)) content(action) else fallback()
}
