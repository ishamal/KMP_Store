package com.isharaw.kmpproj.core

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * A function a feature contributes (into the graph's `Set<EntryProviderInstaller>`) to register its
 * Navigation 3 screen(s) via the `entry<Key> { … }` DSL.
 *
 * Registering a screen and showing a bottom-bar tab are **independent** concerns:
 * every screen contributes an [EntryProviderInstaller]; only screens that should appear in the
 * bottom bar also contribute a [Tab]. A screen reached only from a button (e.g. password reset)
 * contributes an installer but **no** [Tab].
 */
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

/**
 * A bottom-bar tab a feature contributes (into the graph's `Set<Tab>`). Pairs the destination
 * [key] with its presentation [meta] (label/icon/order + optional capability gating).
 */
class Tab(val key: NavKey, val meta: TabMeta)
