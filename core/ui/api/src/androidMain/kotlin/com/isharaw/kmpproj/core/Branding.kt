package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Per-store wordings — the brand-specific strings shown in the UI. The shared *type* lives here (in
 * core/ui) so feature screens can read it via [LocalBranding]; the per-brand *values* live in androidApp
 * (KeelsStrings.kt / CargillsStrings.kt / GlomarkStrings.kt) and are provided at the app root for the
 * active experience. Add a string → add a field here and set it in every brand's file (the compiler
 * enforces it). The brand **colors** live separately — Material slots in the per-brand ColorScheme and
 * custom extras in [BrandColors].
 */
data class Branding(
    val appName: String,
    val welcome: String,
    val tagline: String,
    val loginCta: String,
)

/** Provided once at the app root; read anywhere with `LocalBranding.current`. */
val LocalBranding = staticCompositionLocalOf<Branding> {
    error("LocalBranding not provided — wrap content in CompositionLocalProvider(LocalBranding provides …)")
}
