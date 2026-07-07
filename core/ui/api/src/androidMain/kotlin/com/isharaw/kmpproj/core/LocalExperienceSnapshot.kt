package com.isharaw.kmpproj.core

import androidx.compose.runtime.compositionLocalOf

/**
 * The active session's [ExperienceSnapshot], available to any Composable within the authenticated
 * session scope. Provided by the app shell once a session is established; null before login or
 * after logout.
 *
 * [PermissionGate] defaults its `snapshot` param to this local, so call sites
 * inside the session scope can omit it entirely — the local is consumed implicitly. Call sites that
 * need to override the snapshot (e.g. Compose previews, unit tests) can still pass an explicit
 * non-null value.
 *
 * **Default is null, not an error.** This is a deliberate deviation from the sibling locals
 * ([LocalBrandColorScheme], [LocalExperienceController]) that use `error(...)` defaults: gates
 * render nothing (their existing "not permitted" fallback) when unprovided, rather than crashing.
 * This makes the local safe to consume anywhere in the tree without a provider guard.
 *
 * **`compositionLocalOf` (not `static`)**: the snapshot can change at runtime when the user
 * switches Business Unit or Experience without a full session swap. With `compositionLocalOf` each
 * [PermissionGate] that reads this local is an individual Compose subscriber; only
 * the gate composables recompose when the snapshot changes, not the entire [NavDisplay] subtree.
 * `staticCompositionLocalOf` would skip re-evaluation inside stable NavDisplay entries and leave
 * gates stale after a BU/experience switch.
 */
val LocalExperienceSnapshot = compositionLocalOf<ExperienceSnapshot?> { null }
