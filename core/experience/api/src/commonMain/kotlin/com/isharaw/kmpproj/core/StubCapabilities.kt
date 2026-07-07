package com.isharaw.kmpproj.core

/**
 * Stub capability / permission data keyed by [BusinessUnit].
 *
 * Stands in for the backend until real API permission responses are wired. Both the login flow
 * ([features/login/real]) and the in-app BU-switch path ([App.kt]) call into this so the snapshot
 * is always computed from a consistent capability set for a given BU.
 *
 * Replace all three methods with real backend responses when available.
 */
object StubCapabilities {

    /** The capability strings a successful login would return for [businessUnit]. */
    fun capabilitiesFor(businessUnit: BusinessUnit): Set<String> = when (businessUnit) {
        BusinessUnit.USBL -> setOf(
            "cart.view", "cart.add", "cart.remove", "cart.checkout",
            "catalog.view",
            "invoice.view", "invoice.export",
            "order.view", "order.create", "order.update", "order.cancel",
            "rebate.view", "rebate.view.total", "rebate.view.daily",
            "settings.view", "settings.edit",
        )
        BusinessUnit.CABL -> setOf("cart.view", "invoice.view", "settings.view")
        BusinessUnit.SENM -> setOf(
            "cart.view", "cart.add", "catalog.view",
            "order.view", "order.create", "rebate.view", "settings.view",
        )
    }

    /**
     * The permission list the backend returns alongside capabilities. The resolver keeps only keys
     * present in **both** (capabilities ∩ permissionList). Kept as the full catalog here so the
     * intersection equals the capabilities — tighten a specific key to restrict access.
     */
    fun permissionListFor(businessUnit: BusinessUnit): Set<String> = setOf(
        "cart.view", "cart.add", "cart.remove", "cart.checkout",
        "catalog.view",
        "invoice.view", "invoice.export",
        "order.view", "order.create", "order.update", "order.cancel",
        "rebate.view", "rebate.view.total", "rebate.view.daily",
        "settings.view", "settings.edit",
    )
}
