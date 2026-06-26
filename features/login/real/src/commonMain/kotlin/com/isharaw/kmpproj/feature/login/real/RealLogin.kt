package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.ExperienceProvider
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
class RealAuthenticator(
    private val experienceProvider: ExperienceProvider,
) : Authenticator {

    // A successful login yields the business unit, role and capability list ([StubLoginData] stands in
    // for the backend). We build the snapshot via getExperienceSnapshot, which derives the features
    // from those capabilities (e.g. "cart.view" ⇒ the Cart feature).
    override fun authenticate(email: String, password: String): Session {
        val key = email.trim().substringBefore('@').lowercase()
        val businessUnit = StubLoginData.businessUnitFor(key)
        val userRole = StubLoginData.userRoleFor(key)
        val capabilities = StubLoginData.capabilitiesFor(businessUnit)

        val snapshot = experienceProvider.getExperienceSnapshot(
            experience = StubLoginData.experience,
            businessUnit = businessUnit,
            userRoles = userRole,
            capabilities = capabilities,
        )
        return Session(email = email.trim(), snapshot = snapshot)
    }
}
