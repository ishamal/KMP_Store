package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.CustomerScope
import com.isharaw.kmpproj.core.ExperienceSnapshot
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

/**
 * The **customer (logged-in) child graph** — a [GraphExtension] of [AppGraph] for the [CustomerScope]
 * lifetime. The app shell builds one after login (via [AppGraph.customerGraphFactory]) and drops it on
 * logout, so its scope starts after login and ends at logout. Anything `@SingleIn(CustomerScope::class)`
 * lives only for that one logged-in session.
 *
 * The resolved [ExperienceSnapshot] is passed in at creation (`@Provides`), so it's a customer-scoped
 * binding any session-scoped component can inject.
 */
@GraphExtension(CustomerScope::class)
interface CustomerGraph {
    /** The snapshot for this login (provided into the graph by the factory). */
    val snapshot: ExperienceSnapshot

    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides snapshot: ExperienceSnapshot): CustomerGraph
    }
}
