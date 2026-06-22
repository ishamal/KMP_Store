import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:di:real — no implementation today (the di concern is a contract-only marker). Exists to
// mirror the feature api/real convention; bundles :core:di:api.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.di.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:di:api"))
        }
    }
}
