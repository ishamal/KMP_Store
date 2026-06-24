package com.isharaw.kmpproj.core.access

import kotlin.jvm.JvmInline

/**
 * What a [UserRole] grants, carried as the **raw backend [id]** (e.g. `"cart.view"`) — same format the
 * backend will send, so it stays a drop-in until then. **Distinct** from [Capability] (granted by a
 * [BusinessUnit]).
 *
 * The named constants below mirror the known [Feature]s (the single source of the key strings); an
 * unknown backend id is still a valid `Permission("…")`.
 */
@JvmInline
value class Permission(val id: String) {
    companion object {
        val CART_VIEW = Permission(AccessKeys.CART_VIEW)
        val CART_EDIT = Permission(AccessKeys.CART_EDIT)
        val CATALOG_VIEW = Permission(AccessKeys.CATALOG_VIEW)
        val INVOICE_VIEW = Permission(AccessKeys.INVOICE_VIEW)
        val INVOICE_EXPORT = Permission(AccessKeys.INVOICE_EXPORT)
        val ORDER_VIEW = Permission(AccessKeys.ORDER_VIEW)
        val REBATE_VIEW = Permission(AccessKeys.REBATE_VIEW)
        val SETTINGS_VIEW = Permission(AccessKeys.SETTINGS_VIEW)
        val SETTINGS_EDIT = Permission(AccessKeys.SETTINGS_EDIT)

        /** Every permission the app knows (the whole [AccessKeys.Permissions] vocabulary). */
        val ALL: Set<Permission> = AccessKeys.Permissions.mapTo(LinkedHashSet()) { Permission(it) }
    }
}
