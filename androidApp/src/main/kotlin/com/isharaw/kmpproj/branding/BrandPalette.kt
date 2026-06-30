package com.isharaw.kmpproj.branding

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import com.isharaw.kmpproj.core.BrandColorScheme
import com.isharaw.kmpproj.core.Experience

/**
 * Maps an [Experience] to its brand colors. [brandColorsFor] returns the app's own [BrandColorScheme]
 * (the source of truth, provided down the tree); [colorSchemeFor] derives the minimal Material
 * `ColorScheme` from it so M3 widgets (Button, Card, …) stay branded. `when` is exhaustive, so adding a
 * store forces a BrandColorScheme to be wired. (Wordings are mapped separately in BrandStrings.kt.)
 */

fun brandColorsFor(experience: Experience): BrandColorScheme = when (experience) {
    Experience.KEELS -> KeelsColors
    Experience.CARGILLS -> CargillsColors
    Experience.GLOMARK -> GlomarkColors
}

/** Minimal Material scheme built from the brand's core roles — keeps M3 components on-brand. */
fun colorSchemeFor(experience: Experience): ColorScheme = brandColorsFor(experience).toMaterialColorScheme()

private fun BrandColorScheme.toMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    error = error,
    onError = onError,
)
