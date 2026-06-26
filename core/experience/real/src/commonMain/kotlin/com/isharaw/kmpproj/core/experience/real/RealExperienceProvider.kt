package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceProvider
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.Feature
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.UserRole
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Reads the current user's [ExperienceSnapshot] off the [SessionManager] (it was resolved at login)
 * and answers feature/capability queries against it. Injected as [ExperienceProvider] everywhere.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExperienceProvider(
    private val sessionManager: SessionManager,
) : ExperienceProvider {

    override val snapshot: ExperienceSnapshot?
        get() = sessionManager.session?.snapshot

    override val features: List<Feature>
        get() = snapshot?.resolvedFeatures?.toList().orEmpty()

    override fun capabilitiesOf(featureId: FeatureId): Set<String> =
        features.firstOrNull { it.featureId == featureId }?.capabilities.orEmpty()

    override fun hasFeature(featureId: FeatureId): Boolean =
        features.any { it.featureId == featureId }

    override fun hasCapability(featureId: FeatureId, capability: String): Boolean =
        capability in capabilitiesOf(featureId)

    /**
     * Resolves a snapshot from the given inputs using **sample data** (stands in for the backend):
     * the [businessUnit] decides which features are available, and the [userRoles] role trims the
     * capabilities (a plain USER gets view-only).
     */
    override fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole,
    ): ExperienceSnapshot {
        // Full feature catalog (id + display name + all capabilities).
        val catalog = listOf(
            Feature(FeatureId.CART, "Cart", setOf("cart.view", "cart.add", "cart.remove", "cart.checkout")),
            Feature(FeatureId.CATALOG, "Catalog", setOf("catalog.view")),
            Feature(FeatureId.INVOICES, "Invoices", setOf("invoice.view", "invoice.export")),
            Feature(FeatureId.ORDERS, "Orders", setOf("order.view", "order.create", "order.update", "order.cancel")),
            Feature(FeatureId.REBATE, "Rebate", setOf("rebate.view")),
            Feature(FeatureId.SETTINGS, "Settings", setOf("settings.view", "settings.edit")),
        )

        // Which features the business unit makes available (SETTINGS always, so there's a tab).
        val allowedIds = when (businessUnit) {
            BusinessUnit.USBL -> FeatureId.entries.toSet() // full
            BusinessUnit.CABL -> setOf(FeatureId.CART, FeatureId.INVOICES, FeatureId.SETTINGS)
            BusinessUnit.SENM -> setOf(FeatureId.CART, FeatureId.ORDERS, FeatureId.REBATE, FeatureId.CATALOG, FeatureId.SETTINGS)
        }

        // The role trims capabilities: a plain USER only gets the read (".view") actions.
        fun capabilitiesFor(feature: Feature): Set<String> = when (userRoles) {
            UserRole.USER -> feature.capabilities.filterTo(mutableSetOf()) { it.endsWith(".view") }
            else -> feature.capabilities
        }

        val resolvedFeatures = catalog
            .filter { it.featureId in allowedIds }
            .mapTo(mutableSetOf()) { it.copy(capabilities = capabilitiesFor(it)) }

        return ExperienceSnapshot(
            experience = experience,
            businessUnit = businessUnit,
            userRoles = userRoles,
            resolvedFeatures = resolvedFeatures,
        )
    }
}
