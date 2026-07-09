plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    // Store catalog (the STORES table in build-logic) — via the included build, not buildSrc.
    id("com.isharaw.store-catalog")
}

// Expose the store catalog to the generateIosStore script below as plain, config-cache-friendly
// data (Maps), so the applied script needs no build-logic types on its classpath.
val storeCatalog = extensions.getByType<com.isharaw.gradle.StoreCatalogExtension>()
extra["storeFeaturesByStore"] = storeCatalog.stores()
extra["storeAppIds"] = storeCatalog.storeNames.associateWith { storeCatalog.applicationId(it) }

// iOS store scaffolding task (`generateIosStore`) — kept in its own script for readability.
apply(from = "gradle/generate-ios-store.gradle.kts")