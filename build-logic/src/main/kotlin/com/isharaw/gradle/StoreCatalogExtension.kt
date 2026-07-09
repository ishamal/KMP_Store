package com.isharaw.gradle

/**
 * Read-only view of the store catalog, registered as the `storeCatalog` project extension by
 * [StoreCatalogPlugin]. Backed by the [STORES] table in Stores.kt. Consumers (androidApp, shared,
 * the iOS scaffolding script) call these functions; they don't touch [STORES] directly.
 */
open class StoreCatalogExtension(private val storeDefs: List<StoreDef>) {

    /** Default store when `-Pstore` is not passed. */
    val selectedStore: String get() = SELECTED_STORE

    val storeNames: Set<String> get() = storeDefs.map { it.name }.toSet()

    /** store -> its feature list. */
    fun stores(): Map<String, List<String>> = storeDefs.associate { it.name to it.features }

    fun featuresFor(store: String): List<String> = def(store).features

    fun businessUnitDefaults(store: String): String = def(store).businessUnitDefaults

    /** Base package + lowercased store name, e.g. `com.isharaw.kmpproj.storea`. */
    fun applicationId(store: String): String =
        "$BASE_APPLICATION_ID.${def(store).name.lowercase()}"

    private fun def(store: String): StoreDef =
        storeDefs.firstOrNull { it.name == store }
            ?: error("Unknown store '$store'. Known stores: $storeNames")

    companion object {
        const val SELECTED_STORE = "storeA"
        const val BASE_APPLICATION_ID = "com.isharaw.kmpproj"
    }
}
