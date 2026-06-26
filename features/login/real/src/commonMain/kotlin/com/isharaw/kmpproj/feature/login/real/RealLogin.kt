package com.isharaw.kmpproj.feature.login.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.Session
import com.isharaw.kmpproj.core.UserRole
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
    // STUB: the backend will resolve the ExperienceSnapshot. Until then we derive it from the email's
    // local part, e.g. "cargills.cabl.admin@x.com".
    override fun authenticate(email: String, password: String): Session {
        val key = email.trim().substringBefore('@').lowercase()

        val experience = when {
            "cargills" in key -> Experience.CARGILLS
            "glomark" in key -> Experience.GLOMARK
            else -> Experience.KEELS
        }

        val businessUnit = when {
            "cabl" in key -> BusinessUnit.CABL
            "senm" in key -> BusinessUnit.SENM
            else -> BusinessUnit.USBL
        }

        val userRole = when {
            "custadmin" in key || "customer" in key -> UserRole.CUSTOMER_ADMIN
            "admin" in key -> UserRole.ADMIN
            "manager" in key -> UserRole.MANAGER
            else -> UserRole.USER
        }

        // The full feature catalog (id + name + capabilities) the "backend" knows about.
        val catalog = listOf(
            Feature(FeatureId.CART, "Cart", setOf("cart.view", "cart.add", "cart.remove", "cart.checkout")),
            Feature(FeatureId.CATALOG, "Catalog", setOf("catalog.view")),
            Feature(FeatureId.INVOICES, "Invoices", setOf("invoice.view", "invoice.export")),
            Feature(FeatureId.ORDERS, "Orders", setOf("order.view", "order.create", "order.update", "order.cancel")),
            Feature(FeatureId.REBATE, "Rebate", setOf("rebate.view")),
            Feature(FeatureId.SETTINGS, "Settings", setOf("settings.view", "settings.edit")),
        )

        // What the "backend" resolved as available. SETTINGS is always granted so there's always a tab.
        val allowedIds = when (businessUnit) {
            BusinessUnit.USBL -> FeatureId.entries.toSet() // full
            BusinessUnit.CABL -> setOf(FeatureId.CART, FeatureId.INVOICES, FeatureId.SETTINGS)
            BusinessUnit.SENM -> setOf(FeatureId.CART, FeatureId.ORDERS, FeatureId.REBATE, FeatureId.CATALOG, FeatureId.SETTINGS)
        }
        val resolvedFeatures = catalog.filter { it.featureId in allowedIds }.toSet()

        return Session(
            email = email.trim(),
            snapshot = ExperienceSnapshot(
                experience = experience,
                businessUnit = businessUnit,
                userRoles = userRole,
                resolvedFeatures = resolvedFeatures,
            ),
        )
    }
}
