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
 * [snapshot] defaults to [LocalExperienceSnapshot.current] — call sites inside an authenticated
 * session scope (where the app shell provides the local) can omit it. Pass an explicit value to
 * override (e.g. Compose previews or tests). If [snapshot] is null (local not provided or no active
 * session) nothing is granted and [fallback] (nothing by default) is shown.
 *
 * ```
 * // Inside session scope — snapshot from CompositionLocal:
 * PermissionGate(capability = Capability.ORDER_VIEW) {
 *     OrdersSummaryCard(...)
 * }
 *
 * // Explicit override (preview / test):
 * PermissionGate(snapshot = previewSnapshot, capability = Capability.ORDER_CREATE) {
 *     Button(onClick = { … }) { Text("New order") }
 * }
 * ```
 */
@Composable
fun PermissionGate(
    capability: Capability? = null,
    snapshot: ExperienceSnapshot? = LocalExperienceSnapshot.current,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (snapshot.permits(capability)) content() else fallback()
}
