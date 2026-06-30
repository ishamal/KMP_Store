package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's **own** color scheme — the source of truth for brand colors, not limited to Material's
 * fixed slots. The shared *type* lives here (core/ui) so feature screens read it via
 * [LocalBrandColorScheme]; per-brand *values* live in androidApp (KeelsColors.kt / …) and are provided
 * at the app root for the active experience.
 *
 * Fields are **semantic roles** (priceTag, success, …), not raw token names (cargillsRed), because one
 * shared type is read by brand-agnostic screens — each brand maps its own tokens into these roles.
 *
 * The "core" roles (primary/surface/…) double as the inputs for the minimal Material `ColorScheme` that
 * androidApp derives so M3 widgets (Button, Card, …) stay branded; the rest are app-specific.
 */
data class BrandColorScheme(
    // Core roles — also feed the derived Material ColorScheme.
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val onError: Color,
    // Custom roles — beyond what Material covers. Add as many as you need (set in every brand file).
    val priceTag: Color,
    val discountBadge: Color,
    val loyaltyAccent: Color,
    val success: Color,
    val warning: Color,
    val divider: Color,
    val buttonBackground: Color,
    val cardBackground: Color,
)

/** Provided once at the app root; read anywhere with `LocalBrandColorScheme.current`. */
val LocalBrandColorScheme = staticCompositionLocalOf<BrandColorScheme> {
    error("LocalBrandColorScheme not provided — wrap content in CompositionLocalProvider(LocalBrandColorScheme provides …)")
}
