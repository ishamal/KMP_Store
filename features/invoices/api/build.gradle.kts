import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// API module: public contracts + models, exported to iOS. NO Metro plugin.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.feature.invoices.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model:api"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
