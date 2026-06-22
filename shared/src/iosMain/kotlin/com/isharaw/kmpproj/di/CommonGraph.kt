package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.feature.cart.CartRepository
import com.isharaw.kmpproj.feature.login.LoginValidator
import com.isharaw.kmpproj.feature.settings.SettingsRepository

/**
 * Accessors for the features every store ships. The concrete `IosAppGraph` (defined per store)
 * extends this and adds optional accessors. Exposed to Swift via the `Shared` framework.
 */
interface CommonGraph {
    val loginValidator: LoginValidator
    val cartRepository: CartRepository
    val settingsRepository: SettingsRepository
}
