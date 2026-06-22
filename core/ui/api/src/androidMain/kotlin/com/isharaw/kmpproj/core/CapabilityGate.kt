package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The current logged-in [Experience], published once by the app shell (see App.kt) via
 * `CompositionLocalProvider(LocalExperience provides session.experience)`. Capability-aware UI reads
 * it ambiently, so screens don't have to thread `experience` through every call.
 */
val LocalExperience = staticCompositionLocalOf<Experience> {
    error("LocalExperience not provided — wrap content in CompositionLocalProvider(LocalExperience provides …)")
}

/**
 * Renders [content] only if the current user (from [LocalExperience]) is granted [capability].
 * The capability decision lives here, once — call sites read as plain UI with no `if`:
 *
 * ```
 * CapabilityGate(Capability.EXPORT_INVOICES) {
 *     TextButton(onClick = { … }) { Text("Export") }
 * }
 * ```
 */
@Composable
fun CapabilityGate(capability: Capability, content: @Composable () -> Unit) {
    if (LocalExperience.current.has(capability)) content()
}

/**
 * Explicit-[experience] variant — for tests/previews, or where the experience isn't ambient.
 */
@Composable
fun CapabilityGate(
    capability: Capability,
    experience: Experience,
    content: @Composable () -> Unit,
) {
    if (experience.has(capability)) content()
}
