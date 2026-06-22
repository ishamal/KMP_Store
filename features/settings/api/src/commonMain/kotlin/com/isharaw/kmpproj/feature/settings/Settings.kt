package com.isharaw.kmpproj.feature.settings

/** Public settings contract. Implemented by `RealSettingsRepository` in the `:real` module. */
interface SettingsRepository {
    var darkMode: Boolean
    var notifications: Boolean
}
