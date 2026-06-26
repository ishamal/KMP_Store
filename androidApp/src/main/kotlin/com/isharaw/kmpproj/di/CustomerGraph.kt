package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.CustomerScope
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * The **customer (logged-in) child graph**, a [GraphExtension] of [AppGraph] for the [CustomerScope]
 * lifetime. The app builds one after login (via [AppGraph.customerGraphFactory]) and drops it on
 * logout, so everything `@SingleIn(CustomerScope::class)` — including the customer-scoped ViewModels
 * (`@ContributesIntoMap(CustomerScope::class, …)`, e.g. RebateViewModel) — is rebuilt per session.
 *
 * Extends [ViewModelGraph] so Metro builds a **customer-scoped** ViewModel map + factory; the rebate
 * screen resolves its VM from this graph's `metroViewModelFactory` (published into LocalMetroViewModelFactory).
 */
@GraphExtension(CustomerScope::class)
interface CustomerGraph : ViewModelGraph {
    @GraphExtension.Factory
    interface Factory {
        fun create(): CustomerGraph
    }
}
