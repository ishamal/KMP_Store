import java.io.File
import java.util.Properties

/**
 * Single source of truth for which feature modules each store ships — **config-driven**, in a common,
 * platform-neutral location so both `androidApp` and `shared` (iOS) read the same data.
 *
 * Each store's data lives in its own config file: `config/stores/<store>.properties`, e.g.
 * `config/stores/storeA.properties`:
 * ```
 * storeName=storeA
 * features=login,cart,invoices,settings,orders,rebate,passwordReset
 * ```
 * The store key is the file name (without extension). To add a store: create
 * `config/stores/<store>.properties` (and that store's Android branding) — no edits here.
 *
 * Every function takes the Gradle **rootDir** (pass `rootDir` from a build script). Config is resolved
 * relative to it — **NOT** via `System.getProperty("user.dir")`, which is unreliable: under Android
 * Studio's Gradle sync (and sometimes the daemon) `user.dir` is not the project root (it can be the
 * user's home dir), so `config/stores` would resolve to the wrong place.
 *
 * NOTE (configuration cache): these files are read with `java.io.File` during configuration. With the
 * Gradle configuration cache enabled, editing a config file may not invalidate the cache on its own —
 * run with `--no-configuration-cache` (or change buildSrc) after editing store config.
 */
object StoreManifest {

    const val SELECTED_STORE = "storeA"

    /** Base package; each store's applicationId is derived as BASE_APPLICATION_ID + "." + store name. */
    const val BASE_APPLICATION_ID = "com.isharaw.kmpproj"

    private data class StoreConfig(val storeName: String, val features: List<String>)

    // NOTE: read fresh on every access (no `by lazy`/cached val). `object StoreManifest` lives for the
    // whole Gradle daemon JVM, so a cached value would not pick up a newly added store.properties until
    // the daemon restarts. Re-reading a few small files during configuration is negligible.

    /** store -> feature list (the public shape consumers already use). */
    fun stores(rootDir: File): Map<String, List<String>> =
        loadConfigs(rootDir).mapValues { it.value.features }

    fun featuresFor(rootDir: File, store: String): List<String> =
        config(rootDir, store).features

    fun storeName(rootDir: File, store: String): String =
        config(rootDir, store).storeName

    /** applicationId is derived: base package + the store name appended (lowercased). */
    fun applicationId(rootDir: File, store: String): String =
        "$BASE_APPLICATION_ID.${storeName(rootDir, store).lowercase()}"

    private fun config(rootDir: File, store: String): StoreConfig =
        loadConfigs(rootDir)[store]
            ?: error("Unknown store '$store'. Known stores: ${loadConfigs(rootDir).keys}")

    private fun loadConfigs(rootDir: File): Map<String, StoreConfig> {
        val storesDir = File(rootDir, "config/stores")
        val files = storesDir.listFiles { f -> f.isFile && f.extension == "properties" }
            ?: error("Cannot list store config files under $storesDir")
        val found = files.map { file ->
            val props = Properties().apply { file.inputStream().use { load(it) } }
            val features = props.getProperty("features").orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            // The store name drives the applicationId suffix; defaults to the file name if omitted.
            val storeName = props.getProperty("storeName")?.trim()?.takeIf { it.isNotEmpty() }
                ?: file.nameWithoutExtension
            file.nameWithoutExtension to StoreConfig(storeName = storeName, features = features)
        }.sortedBy { it.first }
        check(found.isNotEmpty()) { "No <store>.properties files found under $storesDir" }
        return found.toMap()
    }
}
