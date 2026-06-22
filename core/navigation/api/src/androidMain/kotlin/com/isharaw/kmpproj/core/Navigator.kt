package com.isharaw.kmpproj.core

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Lets any screen push/pop the app back stack without owning it. The app shell provides a
 * back-stack-backed implementation via [LocalNavigator], so a feature screen can navigate to
 * another destination (e.g. Settings → password reset) without holding a reference to the shell.
 */
interface Navigator {
    fun goTo(key: NavKey)
    fun back()
}

/** Provided by the app shell (App.kt). Reading it outside the shell is a programming error. */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided — wrap content in CompositionLocalProvider(LocalNavigator provides …)")
}
