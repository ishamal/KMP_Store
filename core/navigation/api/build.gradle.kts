import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:navigation — Android-only navigation/contribution primitives:
// Tab, EntryProviderInstaller, Navigator/LocalNavigator, FeatureAction/FeatureSlot/FeatureKind.
// Compose + Nav3 live here (androidMain), so they never leak into the iOS framework.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.navigation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            // `api` so consumers of :core:navigation:api also see TabMeta/Capability (Tab uses TabMeta).
            api(project(":core:model:api"))
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.compose.runtime)
        }
    }
}
