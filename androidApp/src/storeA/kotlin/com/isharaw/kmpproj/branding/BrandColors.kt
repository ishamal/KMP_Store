package com.isharaw.kmpproj.branding

import androidx.compose.ui.graphics.Color

/**
 * Store A brand colors. One copy of this object lives in each flavor source set
 * (androidApp/src/<store>/kotlin/...); the active flavor's version is compiled in, and App.kt
 * builds the Compose color scheme from it. Add a store → add its BrandColors.
 */
object BrandColors {
    val primary = Color(0xFF1565C0)
    val secondary = Color(0xFF42A5F5)
}
