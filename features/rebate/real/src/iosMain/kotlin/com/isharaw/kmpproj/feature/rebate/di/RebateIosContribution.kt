package com.isharaw.kmpproj.feature.rebate.di

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.core.IosFeatureAction
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Rebate iOS self-registration. Contributes an [IosFeatureAction] into the SETTINGS slot so
 * Settings renders a "Rebates" navigation entry. Because this comes from the `:real` module, the
 * entry only appears in stores whose manifest links `rebate` (storeA, storeB).
 *
 * Mirror of the `@Provides @IntoSet fun rebateAction(): FeatureAction` in the androidMain
 * [RebateContribution] — same literals, no NavKey (iOS routing is handled in Swift).
 */
@ContributesTo(AppScope::class)
@BindingContainer
object RebateIosContribution {
    @Provides
    @IntoSet
    fun rebateIosAction(): IosFeatureAction =
        IosFeatureAction(
            label = "Rebates",
            slot = FeatureSlot.SETTINGS,
            kind = FeatureKind.REBATE,
            order = 10,
        )
}
