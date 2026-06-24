package com.isharaw.kmpproj.core.access

import kotlin.jvm.JvmInline

/**
 * What a [BusinessUnit] grants, carried as the **raw backend [id]** (e.g. `"cart.view"`). Kept as a
 * String so the in-memory policy matches exactly what the backend will send — when the backend is
 * wired up, `capabilitiesOf` just returns those strings and no type changes. **Distinct** from
 * [Permission] (granted by a [UserRole]).
 *
 * The named constants below mirror the known [Feature]s (the single source of the key strings) for
 * readable code; an unknown backend id is still a valid `Capability("…")`.
 */
@JvmInline
value class Capability(val id: String) {
    companion object {
        val CART_VIEW = Capability(AccessKeys.CART_VIEW)
        val CART_EDIT = Capability(AccessKeys.CART_EDIT)
        val CATALOG_VIEW = Capability(AccessKeys.CATALOG_VIEW)
        val INVOICE_VIEW = Capability(AccessKeys.INVOICE_VIEW)
        val INVOICE_EXPORT = Capability(AccessKeys.INVOICE_EXPORT)
        val ORDER_VIEW = Capability(AccessKeys.ORDER_VIEW)
        val REBATE_VIEW = Capability(AccessKeys.REBATE_VIEW)
        val SETTINGS_VIEW = Capability(AccessKeys.SETTINGS_VIEW)
        val SETTINGS_EDIT = Capability(AccessKeys.SETTINGS_EDIT)

        /** Every capability the app knows (the whole [AccessKeys.Capabilities] vocabulary). */
        val ALL: Set<Capability> = AccessKeys.Capabilities.mapTo(LinkedHashSet()) { Capability(it) }
    }
}
