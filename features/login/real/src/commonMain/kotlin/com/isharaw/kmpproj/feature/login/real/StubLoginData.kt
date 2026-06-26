package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
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

    /** The flat capability list the "backend" returns for a [businessUnit]; features derive from these. */
    fun capabilitiesFor(businessUnit: BusinessUnit): Set<String> = when (businessUnit) {
        BusinessUnit.USBL -> setOf(
            "cart.view", "cart.add", "cart.remove", "cart.checkout",
            "catalog.view",
            "invoice.view", "invoice.export",
            "order.view", "order.create", "order.update", "order.cancel",
            "rebate.view","rebate.view.total","rebate.view.daily",
            "settings.view", "settings.edit",
        )
        BusinessUnit.CABL -> setOf("cart.view", "invoice.view", "settings.view")
        BusinessUnit.SENM -> setOf(
            "cart.view", "cart.add", "catalog.view",
            "order.view", "order.create", "rebate.view", "settings.view",
        )
    }
}
