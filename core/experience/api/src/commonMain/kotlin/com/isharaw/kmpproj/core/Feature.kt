package com.isharaw.kmpproj.core

/**
 * Stable identity of a top-level app feature — used for tab gating and equality. [key] is the prefix
 * of its capabilities (e.g. ORDERS → `"order"`, so `"order.create"` belongs to ORDERS).
 */
enum class FeatureId(val key: String, val displayName: String) {
    CART("cart", "Cart"),
    CATALOG("catalog", "Catalog"),
    INVOICES("invoice", "Invoices"),
    ORDERS("order", "Orders"),
    REBATE("rebate", "Rebate"),
    SETTINGS("settings", "Settings"),
    ;

    companion object {
        /** The feature a dotted capability belongs to, by its prefix; `"cart.view"` → [CART]. */
        fun fromCapability(capability: String): FeatureId? {
            val prefix = capability.substringBefore('.').trim()
            return entries.firstOrNull { it.key == prefix }
        }
    }
}

/**
 * A feature the backend **resolved** for a user: its [featureId], a display [featureName], and the
 * [capabilities] the user has within it (dotted action keys, e.g. for ORDERS: `"order.view"`,
 * `"order.create"`, `"order.update"`, `"order.cancel"`).
 *
 * The [ExperienceSnapshot] carries the set of these; the app shows a feature's tab when a Feature with
 * the matching [featureId] is present (and the feature module is linked in the build).
 */
data class Feature(
    val featureId: FeatureId,
    val featureName: String,
    val capabilities: Set<String>,
)
