package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The compiled-in set of [FeatureAction]s for this build — every optional feature the linked modules
 * contribute (via Metro `@IntoSet`). Provided by the app shell from the app graph's multibound
 * `Set<FeatureAction>`.
 *
 * [PermissionGate] reads this to resolve the action matching a capability's [FeatureId] and hand it to
 * its content (so a host surface can render, e.g., the feature's button). Defaults to an **empty set**
 * (not an error): a gate whose feature contributes no action simply receives `null`, which is a valid
 * "no compiled-in action" state rather than a crash.
 *
 * **`staticCompositionLocalOf` (unlike [LocalExperienceSnapshot])**: the action set is fixed at build
 * time — it never changes at runtime — so there's no need for per-read subscriptions. A BU/experience
 * switch changes the *snapshot* (what's granted), not this *catalog* (what's compiled in).
 */
val LocalFeatureActions = staticCompositionLocalOf<Set<FeatureAction>> { emptySet() }
