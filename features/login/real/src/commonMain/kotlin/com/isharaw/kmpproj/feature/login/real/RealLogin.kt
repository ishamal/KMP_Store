package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.Session
import com.isharaw.kmpproj.feature.login.Authenticator
import com.isharaw.kmpproj.feature.login.LoginValidator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class RealLoginValidator : LoginValidator {
    override fun validate(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Please enter your email and password."
        !email.contains("@") -> "Please enter a valid email address."
        password.length < 4 -> "Password must be at least 4 characters."
        else -> null
    }
}

@Inject
@ContributesBinding(AppScope::class)
class RealAuthenticator : Authenticator {
    // STUB: derive experience from the email until a real backend is wired.
    override fun authenticate(email: String, password: String): Session {
        val experience =
            if (email.trim().startsWith("cabl", ignoreCase = true)) Experience.CABL else Experience.USBL
        return Session(email = email.trim(), experience = experience)
    }
}
