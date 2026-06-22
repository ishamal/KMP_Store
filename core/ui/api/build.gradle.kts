import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:ui — small presentation helpers shared across Android feature screens (e.g. formatPrice).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
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
}
