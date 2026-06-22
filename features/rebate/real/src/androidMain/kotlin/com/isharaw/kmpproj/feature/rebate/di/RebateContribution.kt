package com.isharaw.kmpproj.feature.rebate.di

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureAction
import com.isharaw.kmpproj.core.FeatureKind
import com.isharaw.kmpproj.core.FeatureSlot
import com.isharaw.kmpproj.feature.rebate.ui.RebateScreen
import com.isharaw.kmpproj.feature.rebate.ui.RebateViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

data object RebateKey : NavKey

/**
 * Rebate self-registration. Reached from a **Settings button** (not a bottom tab): it contributes an
 * [EntryProviderInstaller] for the screen + a [FeatureAction] in the SETTINGS slot — no [com.isharaw.kmpproj.core.Tab].
 * The screen is driven by a Metro-injected Molecule ViewModel.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object RebateContribution {
    @Provides
    @IntoSet
    fun rebateEntry(): EntryProviderInstaller = {
        entry<RebateKey> {
            val viewModel = metroViewModel<RebateViewModel>()
            val state by viewModel.models.collectAsStateWithLifecycle()
            RebateScreen(state)
        }
    }

    @Provides
    @IntoSet
    fun rebateAction(): FeatureAction =
        FeatureAction(
            label = "Rebates",
            target = RebateKey,
            slot = FeatureSlot.SETTINGS,
            kind = FeatureKind.REBATE,
            order = 10,
        )
}
