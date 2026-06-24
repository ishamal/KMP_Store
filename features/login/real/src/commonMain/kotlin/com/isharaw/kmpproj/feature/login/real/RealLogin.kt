package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.Session
import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.UserRole
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
    // STUB: derive the session identity from the email until a real backend is wired. The local part
    // of the address (before "@") picks the business unit and role, e.g. "sesm.manager@x.com".
    override fun authenticate(email: String, password: String): Session {
        val normalized = email.trim()
        val key = normalized.substringBefore('@').lowercase()

        val experience = if (key.startsWith("cabl")) Experience.CABL else Experience.USBL

        val businessUnit = when {
            key.startsWith("cabl") -> BusinessUnit.CABL
            key.startsWith("sesm") -> BusinessUnit.SESM
            else -> BusinessUnit.USBL
        }

        val userRole = when {
            "custadmin" in key || "customer" in key -> UserRole.CUSTOMER_ADMIN
            "admin" in key -> UserRole.ADMIN
            "manager" in key -> UserRole.MANAGER
            else -> UserRole.USER
        }

        return Session(
            email = normalized,
            experience = experience,
            businessUnit = businessUnit,
            userRole = userRole,
        )
    }
}
