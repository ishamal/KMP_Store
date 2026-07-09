plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // The Android Gradle Plugin, so StoreFeaturesPlugin can configure the Application DSL
    // (product flavors + per-flavor dependencies). Version comes from the shared catalog.
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
}

gradlePlugin {
    plugins {
        // Exposes the `storeCatalog` extension from the STORES table (Stores.kt).
        // Applied by `shared` and the root project.
        register("storeCatalog") {
            id = "com.isharaw.store-catalog"
            implementationClass = "com.isharaw.gradle.StoreCatalogPlugin"
        }
        // Android convention plugin: creates a product flavor per store and links that store's
        // feature :real modules. Applied by `androidApp`.
        register("storeFeatures") {
            id = "com.isharaw.store-features"
            implementationClass = "com.isharaw.gradle.StoreFeaturesPlugin"
        }
    }
}
