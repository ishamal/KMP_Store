package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.StubCapabilities
import com.isharaw.kmpproj.core.UserRole

/**
 * Dummy login data — stands in for the backend until it's wired. Maps the email's local part (before
 * `@`) to a business unit + role, and returns the capability list a successful login would carry.
 * Replace this whole object with real backend responses.
 */
internal object StubLoginData {

    /** The experience (store brand) is known from app start, not from login. */
    val experience: Experience = Experience.KEELS

    fun businessUnitFor(emailKey: String): BusinessUnit = when {
        "cabl" in emailKey -> BusinessUnit.CABL
        "senm" in emailKey -> BusinessUnit.SENM
        else -> BusinessUnit.USBL
    }

    fun userRoleFor(emailKey: String): UserRole = when {
        "custadmin" in emailKey || "customer" in emailKey -> UserRole.CUSTOMER_ADMIN
        "admin" in emailKey -> UserRole.ADMIN
        "manager" in emailKey -> UserRole.MANAGER
        else -> UserRole.USER
    }

    /** Delegates to [StubCapabilities] so both login and runtime BU-switch use the same source. */
    fun capabilitiesFor(businessUnit: BusinessUnit): Set<String> =
        StubCapabilities.capabilitiesFor(businessUnit)

    /** Delegates to [StubCapabilities]. */
    fun permitionListFor(businessUnit: BusinessUnit): Set<String> =
        StubCapabilities.permissionListFor(businessUnit)
}
