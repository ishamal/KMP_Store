package com.isharaw.kmpproj.core

/**
 * DI scope marker for a **logged-in customer session**. Things scoped with
 * `@SingleIn(CustomerScope::class)` (and ViewModels contributed with
 * `@ContributesIntoMap(CustomerScope::class, …)`) live in the **customer child graph**, which the app
 * builds after login and drops on logout — see `CustomerGraph` in `androidApp`.
 *
 * Sits *under* [AppScope]: the customer graph can see all app-scoped bindings, but app-scoped code
 * cannot see customer-scoped ones.
 */
abstract class CustomerScope private constructor()
