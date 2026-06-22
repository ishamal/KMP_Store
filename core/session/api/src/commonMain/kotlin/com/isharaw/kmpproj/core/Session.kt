package com.isharaw.kmpproj.core

/** The logged-in user for this app session (email + the [Experience] they were granted). */
data class Session(val email: String, val experience: Experience)
