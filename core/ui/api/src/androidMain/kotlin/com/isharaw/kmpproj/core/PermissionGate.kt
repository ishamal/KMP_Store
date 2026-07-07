package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable

/**
 * **Android (Compose) permission wrapper.** The single gate for permission-based UI — it replaces the
 * former `FeatureGate` (feature check) and `CapabilityGate` (capability check) with one API that does
 * both from a single [capability] string.
 *
 * [capability] is a dotted key (e.g. `"order.view"`, `"rebate.view.total"`). From it the gate derives
 * two checks against the active [snapshot]:
 *  1. **feature** — [FeatureId.fromCapability] maps the prefix to a [FeatureId] (`"order.view"` →
 *     [FeatureId.ORDERS]); [content] shows only if the snapshot resolves that feature.
 *  2. **capability** — the snapshot must also grant the exact [capability].
 *
 * Both must hold. A more-specific capability (e.g. `"rebate.view.total"`) still inherits the parent
 * feature check because the prefix is unchanged. Passing `capability = null` disables gating entirely
 * ([content] always shows) — use that only for unconditional render.
 *
 * [snapshot] defaults to [LocalExperienceSnapshot.current] — call sites inside an authenticated
 * session scope (where the app shell provides the local) can omit it. Pass an explicit value to
 * override (e.g. Compose previews or tests). If [snapshot] is null (local not provided or no active
 * session) nothing is granted and [fallback] (nothing by default) is shown.
 *
 * ```
 * // Inside session scope — snapshot from CompositionLocal:
 * PermissionGate(capability = "order.view") {
 *     OrdersSummaryCard(...)
 * }
 *
 * // Explicit override (preview / test):
 * PermissionGate(snapshot = previewSnapshot, capability = "order.create") {
 *     Button(onClick = { … }) { Text("New order") }
 * }
 * ```
 */
@Composable
fun PermissionGate(
    capability: String? = null,
    snapshot: ExperienceSnapshot? = LocalExperienceSnapshot.current,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val featureId = capability?.let { FeatureId.fromCapability(it) }
    val granted =
        (featureId == null || snapshot?.hasFeature(featureId) == true) &&
        (capability == null || snapshot?.hasCapability(capability) == true)
    if (granted) content() else fallback()
}
