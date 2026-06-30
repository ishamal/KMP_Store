package com.isharaw.kmpproj.branding

import androidx.compose.ui.graphics.Color
import com.isharaw.kmpproj.core.BrandColorScheme

/** GLOMARK palette — Glomark orange (see KeelsColors.kt for the per-brand file pattern). */

// --- Raw palette: every named Glomark color, defined once. ---
val GlomarkOrange = Color(0xFFF47920)
val GlomarkLightOrange = Color(0xFFFFA726)
val GlomarkInk = Color(0xFF201B16)
val GlomarkPaper = Color(0xFFFFFBFF)
val GlomarkPink = Color(0xFFD81B60)
val GlomarkLeaf = Color(0xFF7CB342)
val GlomarkSuccess = Color(0xFF2E7D32)
val GlomarkWarning = Color(0xFFF9A825)
val GlomarkDivider = Color(0xFFE0E0E0)
val GlomarkErrorRed = Color(0xFFBA1A1A)
val GlomarkCardBg = Color(0xFFFFF3E0)
// …add the rest of your named colors here.

// --- Semantic mapping: assign the named palette to the app's brand color roles. ---
val GlomarkColors = BrandColorScheme(
    primary = GlomarkOrange,
    onPrimary = Color.White,
    secondary = GlomarkLightOrange,
    onSecondary = GlomarkInk,
    background = GlomarkPaper,
    onBackground = GlomarkInk,
    surface = GlomarkPaper,
    onSurface = GlomarkInk,
    error = GlomarkErrorRed,
    onError = Color.White,
    priceTag = GlomarkOrange,
    discountBadge = GlomarkPink,
    loyaltyAccent = GlomarkLeaf,
    success = GlomarkSuccess,
    warning = GlomarkWarning,
    divider = GlomarkDivider,
    buttonBackground = GlomarkWarning,
    cardBackground = GlomarkCardBg,
)
