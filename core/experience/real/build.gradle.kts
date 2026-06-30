import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:experience:real — RealExperienceResolver (builds the snapshot at login) and
// RealExperienceReader (app-scoped set-later holder for the snapshot + queries). Applies Metro.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.experience.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:experience:api"))
            implementation(project(":core:di:api")) // AppScope, CustomerScope
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
