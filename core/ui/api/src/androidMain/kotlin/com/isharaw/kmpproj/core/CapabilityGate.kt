package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable

/**
 * **Android (Compose) capability wrapper.** Renders [content] only if [snapshot] grants [capability]
 * (a dotted key, e.g. `"order.create"`); otherwise renders [fallback] (nothing by default). The
 * matching iOS wrapper is `CapabilityGate` in `iosApp` (SwiftUI) — both share
 * `ExperienceSnapshot.hasCapability`.
 *
 * ```
 * CapabilityGate(snapshot, "order.create") {
 *     Button(onClick = { … }) { Text("New order") }
 * }
 * ```
 */
@Composable
fun CapabilityGate(
    snapshot: ExperienceSnapshot,
    capability: String,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (snapshot.hasCapability(capability)) content() else fallback()
}
