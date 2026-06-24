package com.isharaw.kmpproj.core.access

/**
 * The complete catalog of access-control keys in the project — the dotted-string ids the backend
 * uses (e.g. `"cart.view"`). This is the **single source** of the key strings: [Feature], [Capability]
 * and [Permission] are all built from these constants, so a key is written exactly once.
 *
 * Capabilities (granted by a [BusinessUnit]) and permissions (granted by a [UserRole]) draw from the
 * **same vocabulary**, so [Permissions] and [Capabilities] list the same keys. One entry per gated
 * action of every feature/function in the app. (Login & password-reset are pre-auth flows, so they
 * are not access-gated and have no keys here.)
 *
 * To add a function: add its `const val` here, add a matching [Feature] entry, then grant it to the
 * relevant business units / roles in `RealAccessControl`.
 */
object AccessKeys {
    // Cart
    const val CART_VIEW = "cart.view"
    const val CART_EDIT = "cart.edit"

    // Catalog (product browsing)
    const val CATALOG_VIEW = "catalog.view"

    // Invoices
    const val INVOICE_VIEW = "invoice.view"
    const val INVOICE_EXPORT = "invoice.export"

    // Orders
    const val ORDER_VIEW = "order.view"

    // Rebate
    const val REBATE_VIEW = "rebate.view"

    // Settings
    const val SETTINGS_VIEW = "settings.view"
    const val SETTINGS_EDIT = "settings.edit"

    /** Every access key in the project, as a flat list (the dotted ids the backend uses). */
    val ALL: List<String> = listOf(
        CART_VIEW, CART_EDIT,
        CATALOG_VIEW,
        INVOICE_VIEW, INVOICE_EXPORT,
        ORDER_VIEW,
        REBATE_VIEW,
        SETTINGS_VIEW, SETTINGS_EDIT,
    )

    /** Permissions vocabulary (the keys a [UserRole] can grant) — same keys as [Capabilities]. */
    val Permissions: List<String> = ALL

    /** Capabilities vocabulary (the keys a [BusinessUnit] can grant) — same keys as [Permissions]. */
    val Capabilities: List<String> = ALL
}
