package com.isharaw.kmpproj.branding

import androidx.compose.ui.graphics.Color
import com.isharaw.kmpproj.core.BrandColorScheme

/** CARGILLS palette — Cargills FoodCity red (see KeelsColors.kt for the per-brand file pattern). */

// --- Raw palette: every named Cargills color, defined once. ---
val CargillsRed = Color(0xFFE2231A)
val CargillsLightRed = Color(0xFFEF5350)
val CargillsInk = Color(0xFF201A19)
val CargillsPaper = Color(0xFFFFFBFF)
val CargillsBlue = Color(0xFF1E88E5)
val CargillsYellow = Color(0xFFFFD600)
val CargillsSuccess = Color(0xFF2E7D32)
val CargillsWarning = Color(0xFFF9A825)
val CargillsDivider = Color(0xFFE0E0E0)
val CargillsErrorRed = Color(0xFFBA1A1A)
// …add the rest of your named colors here.

// --- Semantic mapping: assign the named palette to the app's brand color roles. ---
val CargillsColors = BrandColorScheme(
    primary = CargillsRed,
    onPrimary = Color.White,
    secondary = CargillsLightRed,
    onSecondary = Color.White,
    background = CargillsPaper,
    onBackground = CargillsInk,
    surface = CargillsPaper,
    onSurface = CargillsInk,
    error = CargillsErrorRed,
    onError = Color.White,
    priceTag = CargillsRed,
    discountBadge = CargillsBlue,
    loyaltyAccent = CargillsYellow,
    success = CargillsSuccess,
    warning = CargillsWarning,
    divider = CargillsDivider,
)
