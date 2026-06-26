package com.isharaw.kmpproj.core

/** The logged-in user for this app session: email + the [ExperienceSnapshot] resolved at login. */
data class Session(val email: String, val snapshot: ExperienceSnapshot)
