package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.invoices.InvoiceRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

/** iOS graph for stores that ship invoices (storeA). */
@DependencyGraph(AppScope::class)
interface IosAppGraph : CommonGraph {
    val invoiceRepository: InvoiceRepository
}

/** Swift entry point — `createGraph` is a compile-time intrinsic, so it can't be called directly from Swift. */
fun createIosAppGraph(): IosAppGraph = createGraph<IosAppGraph>()
