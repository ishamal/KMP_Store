package com.isharaw.kmpproj.feature.passwordreset.di

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.core.IosFeatureAction
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Password-reset iOS self-registration. Contributes an [IosFeatureAction] into the SETTINGS slot
 * so Settings renders a "Reset password" navigation entry. Because this comes from the `:real`
 * module, the entry only appears in stores whose manifest links `passwordReset` (storeA).
 *
 * Mirror of the `@Provides @IntoSet fun passwordResetAction(): FeatureAction` in the androidMain
 * [PasswordResetContribution] — same literals, no NavKey (iOS routing is handled in Swift).
 */
@ContributesTo(AppScope::class)
@BindingContainer
object PasswordResetIosContribution {
    @Provides
    @IntoSet
    fun passwordResetIosAction(): IosFeatureAction =
        IosFeatureAction(
            label = "Reset password",
            slot = FeatureSlot.SETTINGS,
            kind = FeatureKind.PASSWORD_RESET,
            order = 0,
        )
}
