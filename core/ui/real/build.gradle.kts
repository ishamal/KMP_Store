import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:ui:real — no implementation today (formatPrice is a public helper in :core:ui:api).
// Exists to mirror the feature api/real convention; bundles :core:ui:api.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.ui.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":core:ui:api"))
        }
    }
}
