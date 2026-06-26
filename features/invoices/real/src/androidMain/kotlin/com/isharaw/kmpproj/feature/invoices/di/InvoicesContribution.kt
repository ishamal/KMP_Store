package com.isharaw.kmpproj.feature.invoices.di

import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.Tab
import com.isharaw.kmpproj.core.TabMeta
import com.isharaw.kmpproj.feature.invoices.InvoiceRepository
import com.isharaw.kmpproj.feature.invoices.ui.InvoicesScreen
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

data object InvoicesKey : NavKey

/** Invoices screen + tab (gated by the INVOICES resolved feature). */
@ContributesTo(AppScope::class)
@BindingContainer
object InvoicesContribution {
    @Provides
    @IntoSet
    fun invoicesEntry(repository: InvoiceRepository): EntryProviderInstaller = {
        entry<InvoicesKey> {
            InvoicesScreen(repository = repository)
        }
    }

    @Provides
    @IntoSet
    fun invoicesTab(): Tab = Tab(InvoicesKey, TabMeta("Invoices", "🧾", order = 30, featureId = FeatureId.INVOICES))
}
