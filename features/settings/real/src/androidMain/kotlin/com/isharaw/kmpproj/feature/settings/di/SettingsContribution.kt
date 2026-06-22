package com.isharaw.kmpproj.feature.settings.di

import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureAction
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.Tab
import com.isharaw.kmpproj.core.TabMeta
import com.isharaw.kmpproj.feature.settings.SettingsRepository
import com.isharaw.kmpproj.feature.settings.ui.SettingsScreen
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

data object SettingsKey : NavKey

/** Settings screen + tab. Logout clears the session; the app shell reacts via the observable session. */
@ContributesTo(AppScope::class)
@BindingContainer
object SettingsContribution {
    @Provides
    @IntoSet
    fun settingsEntry(
        repository: SettingsRepository,
        sessionManager: SessionManager,
        featureActions: Set<FeatureAction>,
    ): EntryProviderInstaller = {
        entry<SettingsKey> {
            SettingsScreen(
                repository = repository,
                userEmail = sessionManager.session?.email.orEmpty(),
                actions = featureActions
                    .filter { FeatureSlot.SETTINGS in it.slots }
                    .sortedBy { it.order },
                onLogout = { sessionManager.session = null },
            )
        }
    }

    @Provides
    @IntoSet
    fun settingsTab(): Tab = Tab(SettingsKey, TabMeta("Settings", "⚙️", order = 100))
}
