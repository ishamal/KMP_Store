import java.io.File
import java.util.Properties

/**
 * Single source of truth for which feature modules each store ships — **config-driven**, in a common,
 * platform-neutral location so both `androidApp` and `shared` (iOS) read the same data.
 *
 * Each store's data lives in its own config file: `config/stores/<store>.properties`, e.g.
 * `config/stores/storeA.properties`:
 * ```
 * applicationId=com.isharaw.kmpproj.storea
 * features=login,cart,invoices,settings,orders,rebate,passwordReset
 * ```
 * The store name is the file name (without extension). To add a store: create
 * `config/stores/<store>.properties` (and that store's Android branding) — no edits here.
 *
 * NOTE (configuration cache): these files are read with `java.io.File` during configuration. With the
 * Gradle configuration cache enabled, editing a config file may not invalidate the cache on its own —
 * run with `--no-configuration-cache` (or change buildSrc) after editing store config.
 */
object StoreManifest {

    const val SELECTED_STORE = "storeA"

    /** Common config dir holding one <store>.properties per store (resolved from the root project). */
    private val storesDir = File(System.getProperty("user.dir"), "config/stores")

    private data class StoreConfig(val features: List<String>, val applicationId: String)

    // NOTE: read fresh on every access (no `by lazy`/cached val). `object StoreManifest` lives for the
    // whole Gradle daemon JVM, so a cached value would not pick up a newly added store.properties until
    // the daemon restarts. Re-reading a few small files during configuration is negligible.

    /** store -> feature list (the public shape consumers already use). */
    val stores: Map<String, List<String>> get() = loadConfigs().mapValues { it.value.features }

    fun featuresFor(store: String): List<String> =
        loadConfigs()[store]?.features ?: error("Unknown store '$store'. Known stores: ${loadConfigs().keys}")

    fun applicationId(store: String): String =
        loadConfigs()[store]?.applicationId ?: error("Unknown store '$store'. Known stores: ${loadConfigs().keys}")

    private fun loadConfigs(): Map<String, StoreConfig> {
        val files = storesDir.listFiles { f -> f.isFile && f.extension == "properties" }
            ?: error("Cannot list store config files under $storesDir")
        val found = files.map { file ->
            val props = Properties().apply { file.inputStream().use { load(it) } }
            val features = props.getProperty("features").orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val appId = props.getProperty("applicationId")
                ?: error("Missing 'applicationId' in ${file.path}")
            file.nameWithoutExtension to StoreConfig(features, appId)
        }.sortedBy { it.first }
        check(found.isNotEmpty()) { "No <store>.properties files found under $storesDir" }
        return found.toMap()
    }
}
