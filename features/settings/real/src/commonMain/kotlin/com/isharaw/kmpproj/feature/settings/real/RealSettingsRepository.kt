package com.isharaw.kmpproj.feature.settings.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.settings.SettingsRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Holds user preferences. @SingleIn because it carries mutable state for the app's lifetime. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealSettingsRepository : SettingsRepository {
    override var darkMode: Boolean = false
    override var notifications: Boolean = true
}
