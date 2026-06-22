package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

/** iOS graph for stores without invoices (storeB). */
@DependencyGraph(AppScope::class)
interface IosAppGraph : CommonGraph

/** Swift entry point — `createGraph` is a compile-time intrinsic, so it can't be called directly from Swift. */
fun createIosAppGraph(): IosAppGraph = createGraph<IosAppGraph>()
