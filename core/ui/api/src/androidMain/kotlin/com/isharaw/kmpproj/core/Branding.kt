package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Per-store wordings/branding, published once by the app shell (App.kt) from the active flavor's
 * resources (`androidApp/src/<store>/res/values/strings.xml`). Feature screens read it via
 * [LocalBranding] — they can't see androidApp's resources directly, so this bridges the values down.
 */
data class Branding(
    val appName: String,
    val welcome: String,
)

/** Provided once at the app root; read anywhere with `LocalBranding.current`. */
val LocalBranding = staticCompositionLocalOf<Branding> {
    error("LocalBranding not provided — wrap content in CompositionLocalProvider(LocalBranding provides …)")
}
