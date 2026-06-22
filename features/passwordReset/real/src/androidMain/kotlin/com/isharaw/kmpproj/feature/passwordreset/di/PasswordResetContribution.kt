package com.isharaw.kmpproj.feature.passwordreset.di

import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureAction
import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.feature.passwordreset.ui.PasswordResetScreen
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

data object PasswordResetKey : NavKey

/**
 * Password-reset self-registration. Contributes TWO things into the app graph:
 *  1. an [EntryProviderInstaller] that registers the screen — but NO [com.isharaw.kmpproj.core.Tab],
 *     so it never appears in the bottom bar (reached only from the Settings button);
 *  2. a [FeatureAction] in the SETTINGS slot so the Settings screen renders a "Reset password" button.
 *
 * Because both come from this module, they appear ONLY in stores whose manifest links
 * `passwordReset` (storeA + storeC). Settings itself never references this module.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object PasswordResetContribution {
    @Provides
    @IntoSet
    fun passwordResetEntry(): EntryProviderInstaller = {
        entry<PasswordResetKey> { PasswordResetScreen() }
    }

    @Provides
    @IntoSet
    fun passwordResetAction(): FeatureAction =
        FeatureAction(
            label = "Reset password",
            target = PasswordResetKey,
            slot = FeatureSlot.SETTINGS,
            kind = FeatureKind.PASSWORD_RESET,
            order = 0,
        )
}
