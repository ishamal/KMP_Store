/**
 * Single source of truth for which feature modules each store ships.
 *
 * To add a store: add an entry to [stores] and create a matching Android flavor source set
 * `androidApp/src/<store>/.../ui/StoreFeatures.kt`.
 * To add a feature: create the `:features:<name>` module, include it in settings.gradle.kts,
 * then add its name to the relevant stores below.
 */
object StoreManifest {

    const val DEFAULT_STORE = "storeA"

    val stores: Map<String, List<String>> = linkedMapOf(
        "storeA" to listOf("login", "cart", "invoices", "settings", "orders","rebate", "passwordReset"),
        "storeB" to listOf("login", "cart", "settings", "rebate"),
        "storeC" to listOf("login", "settings", "orders", "passwordReset"),
    )

    fun featuresFor(store: String): List<String> =
        stores[store] ?: error("Unknown store '$store'. Known stores: ${stores.keys}")

    fun applicationId(store: String): String =
        "com.isharaw.kmpproj.${store.lowercase()}"
}
