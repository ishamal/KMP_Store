import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

// The active store (defaults to StoreManifest.SELECTED_STORE) decides which feature modules
// the iOS framework links and exports. See buildSrc/StoreManifest.kt.
val store = providers.gradleProperty("store").getOrElse(StoreManifest.SELECTED_STORE)
val storeFeatures = StoreManifest.featuresFor(store)

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            // Export only the public :api contracts to Swift (never the Real impls).
            storeFeatures.forEach { export(project(":features:$it:api")) }
        }
    }

    androidLibrary {
       namespace = "com.isharaw.kmpproj.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       withHostTest {}
    }

    sourceSets {
        // Features are linked iOS-only here (api, so they are exportable from the framework).
        // The Android app pulls feature modules in directly per product flavor, so the Android
        // side of `shared` never carries store-specific features.
        iosMain.dependencies {
            implementation(project(":core:di:api"))
            implementation(project(":core:model:api"))
            implementation(project(":core:session:api"))
            storeFeatures.forEach {
                api(project(":features:$it:api"))            // contracts (exported)
                implementation(project(":features:$it:real")) // impls (linked, internal, not exported)
            }
        }
        // The iOS DI graph declares an invoices accessor only when this store ships invoices.
        // (Same store-aware idea as the Android flavor source sets, driven by -Pstore.)
//        val iosGraphDir = if ("invoices" in storeFeatures) "src/iosStoreWithInvoices/kotlin"
//                          else "src/iosStoreBase/kotlin"
//        iosMain { kotlin.srcDir(iosGraphDir) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
