package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.IosFeatureAction
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.createGraph

/** iOS graph for stores without invoices (storeB, storeC). */
@DependencyGraph(AppScope::class)
interface IosAppGraph : CommonGraph {
    /**
     * Optional, store-gated entry points features expose into shared host surfaces (Settings, …).
     * Empty in stores that ship no contributing feature; `allowEmpty` keeps the graph valid in that
     * case. Swift reads this, filters to [FeatureSlot.SETTINGS], sorts by [IosFeatureAction.order],
     * and routes on [IosFeatureAction.kind].
     */
    @Multibinds(allowEmpty = true) val featureActions: Set<IosFeatureAction>
}

/** Swift entry point — `createGraph` is a compile-time intrinsic, so it can't be called directly from Swift. */
fun createIosAppGraph(): IosAppGraph = createGraph<IosAppGraph>()
