package com.isharaw.kmpproj.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Global accessor for the active brand colors — mirrors how `MaterialTheme.colorScheme` is read. Any
 * Android feature screen can use `AppTheme.colors.cardBackground` without touching the CompositionLocal
 * itself. The values are whatever the app shell provides at the root for the active experience (see
 * App.kt), so they switch with the experience.
 *
 * Material's own slots still come from `MaterialTheme.colorScheme`; this is for the app's custom
 * [BrandColorScheme] roles.
 */
object AppTheme {
    val colors: BrandColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandColorScheme.current
}
