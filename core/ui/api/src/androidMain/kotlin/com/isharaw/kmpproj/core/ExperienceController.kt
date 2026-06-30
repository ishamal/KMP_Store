package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * In-app store switcher. The app shell holds the active [Experience] as state and exposes it here;
 * any screen (e.g. Settings) can read [current] and call [switch] to change stores at runtime, which
 * re-derives the theme + wordings (see [brandThemeFor]) and recomposes — no restart.
 *
 * Switching changes only the **branding/experience**; which features are visible is still gated by the
 * session's `ExperienceSnapshot`, and which feature modules exist is still decided by the build flavor.
 */
interface ExperienceController {
    val current: Experience
    fun switch(experience: Experience)
}

/** Provided once at the app root; read anywhere with `LocalExperienceController.current`. */
val LocalExperienceController = staticCompositionLocalOf<ExperienceController> {
    error("LocalExperienceController not provided — wrap content in CompositionLocalProvider(LocalExperienceController provides …)")
}
