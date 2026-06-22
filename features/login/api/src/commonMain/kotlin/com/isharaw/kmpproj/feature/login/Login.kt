package com.isharaw.kmpproj.feature.login

import com.isharaw.kmpproj.core.Session

/** Validates login form input; returns an error message or null. */
interface LoginValidator {
    fun validate(email: String, password: String): String?
}

/** Turns credentials into a [Session] (email + experience). */
interface Authenticator {
    fun authenticate(email: String, password: String): Session
}
