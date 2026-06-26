package com.isharaw.kmpproj.feature.cart.di

import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.Tab
import com.isharaw.kmpproj.core.TabMeta
import com.isharaw.kmpproj.feature.cart.CartRepository
import com.isharaw.kmpproj.feature.cart.ui.CartScreen
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

data object CartKey : NavKey

/** Self-registers the Cart screen + its bottom-bar tab. */
@ContributesTo(AppScope::class)
@BindingContainer
object CartContribution {
    @Provides
    @IntoSet
    fun cartEntry(repository: CartRepository): EntryProviderInstaller = {
        entry<CartKey> { CartScreen(repository = repository) }
    }

    @Provides
    @IntoSet
    fun cartTab(): Tab = Tab(CartKey, TabMeta("Cart", "🛒", order = 10, featureId = FeatureId.CART))
}
