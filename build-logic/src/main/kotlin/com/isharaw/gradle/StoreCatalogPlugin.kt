package com.isharaw.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `storeCatalog` extension from the [STORES] table (Stores.kt). Validates that every
 * feature a store lists has a real `:features:<name>:real` module — failing fast with a clear message
 * on a typo, instead of a confusing "project not found" deep in the build.
 *
 * There is no file I/O and no ValueSource: the store data is compiled into this build, so editing
 * Stores.kt recompiles `build-logic` and Gradle invalidates the configuration cache automatically.
 */
class StoreCatalogPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target.extensions.findByName(EXTENSION_NAME) != null) return

        STORES.forEach { store ->
            store.features.forEach { feature ->
                require(target.rootProject.findProject(":features:$feature:real") != null) {
                    "Store '${store.name}' lists feature '$feature', but there is no module " +
                        ":features:$feature:real. Fix the STORES table in build-logic (Stores.kt)."
                }
            }
        }

        target.extensions.create(EXTENSION_NAME, StoreCatalogExtension::class.java, STORES)
    }

    private companion object {
        const val EXTENSION_NAME = "storeCatalog"
    }
}
