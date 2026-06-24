package com.isharaw.kmpproj.core.access.real

import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.Capability

/**
 * **BusinessUnit → Capability policy.** This is the one place that says which capabilities each
 * business unit makes available.
 *
 * ➜ When you add a new feature: after adding its key to `AccessKeys` / `Feature` / `Capability`, grant
 *   the new `Capability.X` to every business unit that should have it **here**. (`Capability.ALL`
 *   automatically includes it for full-access units like USBL.)
 */
internal object BusinessUnitCapabilities {
    val map: Map<BusinessUnit, Set<Capability>> = mapOf(
        // USBL: full access — every capability in the catalog.
        BusinessUnit.USBL to Capability.ALL,

        // CABL: read-only customer view (no cart edits, no rebates).
        BusinessUnit.CABL to setOf(
            Capability.CART_VIEW,
            Capability.CATALOG_VIEW,
            Capability.INVOICE_VIEW,
            Capability.ORDER_VIEW,
        ),

        // SESM: sales/service — cart + rebates + catalog/orders, but not invoices.
        BusinessUnit.SESM to setOf(
            Capability.CART_VIEW,
            Capability.CART_EDIT,
            Capability.CATALOG_VIEW,
            Capability.ORDER_VIEW,
            Capability.REBATE_VIEW,
        ),
    )
}
