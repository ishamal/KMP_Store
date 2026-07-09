package com.isharaw.gradle

/**
 * One store's build-time definition — the single source of truth for which features a store ships.
 * Used by BOTH the Android product flavors and the iOS framework build.
 *
 * Add a store: add a [StoreDef] entry to [STORES]. Remove a store: delete its entry. The compiler
 * checks it, so there are no stringly-typed config files to keep in sync.
 */
data class StoreDef(
    /** Store id; also the product-flavor name and the applicationId suffix (lowercased). */
    val name: String,
    /** Bare feature names. Each must have a `:features:<name>:real` module (validated at configure time). */
    val features: List<String>,
    /** `<EXPERIENCE>:<BUSINESS_UNIT>` pairs, surfaced to the app as BuildConfig.BUSINESS_UNIT_DEFAULTS. */
    val businessUnitDefaults: String = "",
)

/**
 * Every store. Adding a store = adding one line here; Android (a product flavor) and iOS (the
 * `-Pstore` framework build) both pick it up automatically.
 */
internal val STORES: List<StoreDef> = listOf(
    StoreDef(
        name = "storeA",
        features = listOf("login", "cart", "invoices", "settings", "orders", "rebate", "passwordReset"),
        businessUnitDefaults = "KEELS:USBL,CARGILLS:SENM",
    ),
    StoreDef(
        name = "storeB",
        features = listOf("login", "cart", "settings", "rebate"),
        businessUnitDefaults = "KEELS:USBL",
    ),
    StoreDef(
        name = "storeC",
        features = listOf("login", "cart", "settings", "orders"),
        businessUnitDefaults = "KEELS:USBL",
    ),
    StoreDef(
        name = "storeD",
        features = listOf("login", "cart", "settings", "orders"),
        businessUnitDefaults = "KEELS:USBL",
    ),
)
