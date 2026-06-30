import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:ui — small presentation helpers shared across Android feature screens (formatPrice,
// CapabilityGate). Uses ONLY the Kotlin Compose compiler plugin (not Compose Multiplatform) so it can
// expose reusable @Composable primitives without pulling in the org.jetbrains.compose plugin.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":core:model:api"))     // Capability, Experience, has()
            implementation(libs.compose.runtime)
            api(libs.compose.ui)                 // Color — exposed in the public BrandColors type
        }
    }
}
