package com.isharaw.kmpproj.core

/**
 * The type-safe catalog of access capabilities. Each entry owns its dotted wire [value] (e.g.
 * `"rebate.view.total"`) — this enum is the **single source of truth** for those strings, so gates
 * and stub data reference `Capability.REBATE_VIEW_TOTAL` instead of a typo-prone literal.
 *
 * The dotted [value]'s prefix maps to a [FeatureId] via [FeatureId.fromCapability] (`REBATE_VIEW` →
 * [FeatureId.REBATE]); the gate ([permits]) derives the feature from it, so entries don't store the
 * feature themselves. [value] remains the boundary type the backend snapshot speaks in
 * ([ExperienceSnapshot.grantedCapabilities] is `Set<String>`); use [from] to map an inbound string
 * back to an entry.
 */
enum class Capability(val value: String) {
    CART_VIEW("cart.view"),
    CART_ADD("cart.add"),
    CART_REMOVE("cart.remove"),
    CART_CHECKOUT("cart.checkout"),

    CATALOG_VIEW("catalog.view"),

    INVOICE_VIEW("invoice.view"),
    INVOICE_EXPORT("invoice.export"),

    ORDER_VIEW("order.view"),
    ORDER_CREATE("order.create"),
    ORDER_UPDATE("order.update"),
    ORDER_CANCEL("order.cancel"),

    REBATE_VIEW("rebate.view"),
    REBATE_VIEW_TOTAL("rebate.view.total"),
    REBATE_VIEW_DAILY("rebate.view.daily"),

    SETTINGS_VIEW("settings.view"),
    SETTINGS_EDIT("settings.edit"),
    ;

    companion object {
        /** The entry for a dotted wire [value], or `null` if it isn't in the catalog. */
        fun from(value: String): Capability? = entries.firstOrNull { it.value == value }
    }
}
