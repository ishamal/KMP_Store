package com.isharaw.kmpproj.core

/**
 * DI scope marker shared by every module. Repositories scoped with
 * `@SingleIn(AppScope::class)` live for the lifetime of the app graph.
 *
 * Lives in `:core` (which every feature depends on) so feature modules can reference the scope
 * without creating a dependency cycle.
 */
abstract class AppScope private constructor()
