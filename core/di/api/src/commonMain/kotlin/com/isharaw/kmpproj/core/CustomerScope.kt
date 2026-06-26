package com.isharaw.kmpproj.core

/**
 * DI scope marker for a **logged-in customer session**. Unlike [AppScope] (which lives for the whole
 * app), `CustomerScope` **starts after login and ends at logout**: the app builds a customer child
 * graph when a session appears and drops it when the session is cleared, so everything
 * `@SingleIn(CustomerScope::class)` is rebuilt fresh on each login. See `CustomerGraph` in `androidApp`.
 */
abstract class CustomerScope private constructor()
