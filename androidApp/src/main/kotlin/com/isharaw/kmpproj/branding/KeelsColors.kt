package com.isharaw.kmpproj.branding

import androidx.compose.ui.graphics.Color
import com.isharaw.kmpproj.core.BrandColorScheme

/**
 * KEELS palette. One file per brand; all brands ship in every build so the active experience can be
 * switched at runtime (see brandColorsFor in BrandPalette.kt).
 */

// --- Raw palette: every named Keells color, defined once. Brand-prefix the names so they don't
// collide with the other brand files (all share this package). Add as many as you have. ---
val KeelsGreen = Color(0xFF00843D)
val KeelsLightGreen = Color(0xFF4CAF50)
val KeelsInk = Color(0xFF191C19)
val KeelsPaper = Color(0xFFFBFDF8)
val KeelsPink = Color(0xFFD81B60)
val KeelsAmber = Color(0xFFFFC107)
val KeelsSuccess = Color(0xFF2E7D32)
val KeelsWarning = Color(0xFFF9A825)
val KeelsDivider = Color(0xFFE0E0E0)
val KeelsErrorRed = Color(0xFFBA1A1A)
// …add the rest of your named colors here.

// --- Semantic mapping: assign the named palette to the app's brand color roles. ---
val KeelsColors = BrandColorScheme(
    primary = KeelsGreen,
    onPrimary = Color.White,
    secondary = KeelsLightGreen,
    onSecondary = Color.White,
    background = KeelsPaper,
    onBackground = KeelsInk,
    surface = KeelsPaper,
    onSurface = KeelsInk,
    error = KeelsErrorRed,
    onError = Color.White,
    priceTag = KeelsGreen,
    discountBadge = KeelsPink,
    loyaltyAccent = KeelsAmber,
    success = KeelsSuccess,
    warning = KeelsWarning,
    divider = KeelsDivider,
)
