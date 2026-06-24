package com.isharaw.kmpproj.core.access

/**
 * A **feature/action** that can be shown in the app, identified by a stable dotted [key] from
 * [AccessKeys] (the single source of the key strings). One entry per gated action of every function
 * in the project. A feature is shown only when **both** the business unit (via a [Capability]) **and**
 * the user role (via a [Permission]) grant it — see [AccessControl.allowedFeatures].
 */
enum class Feature(val key: String) {
    CART_VIEW(AccessKeys.CART_VIEW),
    CART_EDIT(AccessKeys.CART_EDIT),
    CATALOG_VIEW(AccessKeys.CATALOG_VIEW),
    INVOICE_VIEW(AccessKeys.INVOICE_VIEW),
    INVOICE_EXPORT(AccessKeys.INVOICE_EXPORT),
    ORDER_VIEW(AccessKeys.ORDER_VIEW),
    REBATE_VIEW(AccessKeys.REBATE_VIEW),
    SETTINGS_VIEW(AccessKeys.SETTINGS_VIEW),
    SETTINGS_EDIT(AccessKeys.SETTINGS_EDIT),
    ;

    companion object {
        /** Look up a feature by its dotted [key]; throws if unknown. */
        fun fromKey(key: String): Feature =
            entries.firstOrNull { it.key == key.trim() }
                ?: error("Unknown feature '$key'. Known: ${entries.map { it.key }}")
    }
}
