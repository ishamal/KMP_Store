package com.isharaw.gradle

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Android convention plugin. Creates one product flavor per store and links exactly that store's
 * feature `:real` modules, so features a store does not ship are removed from the build. Replaces
 * the flavor + per-flavor dependency loops that used to sit inline in `androidApp/build.gradle.kts`.
 */
class StoreFeaturesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(StoreCatalogPlugin::class.java)
        val catalog = target.extensions.getByType(StoreCatalogExtension::class.java)

        target.pluginManager.withPlugin("com.android.application") {
            val android = target.extensions.getByType(ApplicationExtension::class.java)

            android.flavorDimensions += DIMENSION
            catalog.stores().keys.forEach { store ->
                android.productFlavors.create(store) {
                    dimension = DIMENSION
                    applicationId = catalog.applicationId(store)
                    buildConfigField(
                        "String",
                        "BUSINESS_UNIT_DEFAULTS",
                        "\"${catalog.businessUnitDefaults(store)}\"",
                    )
                }
            }

            // AGP creates the flavor-specific configurations (storeAImplementation, …) as it
            // processes the flavors; add each store's feature :real modules after evaluation so
            // every configuration exists.
            target.afterEvaluate {
                catalog.stores().forEach { (store, features) ->
                    features.forEach { feature ->
                        target.dependencies.add(
                            "${store}Implementation",
                            target.project(":features:$feature:real"),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val DIMENSION = "store"
    }
}
