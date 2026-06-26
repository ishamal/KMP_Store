package com.isharaw.kmpproj.feature.orders.di

import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.Tab
import com.isharaw.kmpproj.core.TabMeta
import com.isharaw.kmpproj.feature.orders.OrderRepository
import com.isharaw.kmpproj.feature.orders.ui.OrdersScreen
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

data object OrdersKey : NavKey

/** Orders screen + tab (the tab is visible only when ORDERS is in the user's resolved features). */
@ContributesTo(AppScope::class)
@BindingContainer
object OrdersContribution {
    @Provides
    @IntoSet
    fun ordersEntry(repository: OrderRepository): EntryProviderInstaller = {
        entry<OrdersKey> { OrdersScreen(repository = repository) }
    }

    @Provides
    @IntoSet
    fun ordersTab(): Tab =
        Tab(OrdersKey, TabMeta("Orders", "📦", order = 20, featureId = FeatureId.ORDERS))
}
