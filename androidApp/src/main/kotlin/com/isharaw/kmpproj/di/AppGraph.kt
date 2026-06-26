package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.EntryProviderInstaller
import com.isharaw.kmpproj.core.FeatureAction
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.Tab
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.feature.login.Authenticator
import com.isharaw.kmpproj.feature.login.LoginValidator
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * The one app graph — store-agnostic. Features self-register Navigation 3 [NavDestination]s, so the
 * contributed set reflects exactly the feature modules the active flavor links.
 *
 * Extends [ViewModelGraph] so Metro builds the `ViewModel` multibinding map + a
 * [dev.zacsweers.metrox.viewmodel.MetroViewModelFactory]; screens resolve VMs via `metroViewModel()`.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    // Every linked feature registers its screen(s) here; the shell runs them all into NavDisplay.
    @Multibinds(allowEmpty = true) val entryInstallers: Set<EntryProviderInstaller>

    // Only features that want a bottom-bar item contribute a Tab (screens reached from a button —
    // e.g. password reset — register an installer but no Tab).
    @Multibinds(allowEmpty = true) val tabs: Set<Tab>

    // Optional, store-gated entry points features expose into shared host surfaces (Settings, …),
    // each tagged with a FeatureSlot. Empty in stores that ship no feature contributing one.
    @Multibinds(allowEmpty = true) val featureActions: Set<FeatureAction>

    val loginValidator: LoginValidator
    val authenticator: Authenticator
    val sessionManager: SessionManager
    val accessControl: AccessControl

    // Builds the customer (logged-in) child graph; the app shell creates one after login.
    val customerGraphFactory: CustomerGraph.Factory
}

fun createAppGraph(): AppGraph = createGraph<AppGraph>()
