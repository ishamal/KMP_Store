package com.isharaw.kmpproj.core

/**
 * The pure access decision shared by every UI gate on both platforms — the Android Compose
 * `PermissionGate` and the iOS SwiftUI gates both call this so the "is this allowed?" rule lives in
 * one tested place. Does the receiver snapshot permit [capability]?
 *
 * From the single [capability] string two checks are derived against the receiver snapshot:
 *  1. **feature** — [FeatureId.fromCapability] maps the prefix to a [FeatureId] (`"order.view"` →
 *     [FeatureId.ORDERS]); the snapshot must resolve that feature.
 *  2. **capability** — the snapshot must grant the exact [capability].
 *
 * Both must hold. A more-specific capability (`"rebate.view.total"`) still inherits the parent
 * feature check because the prefix is unchanged. Rules:
 *  - [capability] `null` → ungated: always `true`, even for a null snapshot.
 *  - a capability whose prefix maps to no [FeatureId] (e.g. `"delivery.widget.view"`) → the feature
 *    check short-circuits and only the capability check applies.
 *  - receiver snapshot `null` (no active session) → `false` for any non-null [capability].
 */
fun ExperienceSnapshot?.permits(capability: String?): Boolean {
    val featureId = capability?.let { FeatureId.fromCapability(it) }
    return (featureId == null || this?.hasFeature(featureId) == true) &&
        (capability == null || this?.hasCapability(capability) == true)
}

/**
 * Type-safe overload of [permits] — prefer this at call sites so the capability is compile-checked
 * (no typo-prone dotted string). Delegates to the [String] form via [Capability.value].
 */
fun ExperienceSnapshot?.permits(capability: Capability?): Boolean = permits(capability?.value)
