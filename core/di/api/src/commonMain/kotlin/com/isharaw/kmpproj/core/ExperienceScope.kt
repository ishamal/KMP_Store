package com.isharaw.kmpproj.core

/**
 * DI scope marker for a resolved [ExperienceSnapshot]. The scope lives for the duration of one
 * resolved snapshot; it is **recreated on every BU/experience switch** and **dropped at logout**.
 *
 * Objects bound inside this scope (e.g. ViewModels) receive a non-null [ExperienceSnapshot] at
 * construction time, eliminating the nullable app-scoped holder pattern.
 *
 * Lives in `:core:di:api` next to [AppScope] so feature modules can reference it without creating
 * dependency cycles.
 */
abstract class ExperienceScope private constructor()
