import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:access:real — the access policy (BusinessUnit→Capability, UserRole→Permission) + RealAccessControl.
// Applies Metro to contribute its AccessControl binding; bundles :core:access:api. Logic lives here, never in :api.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.access.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:access:api"))
            implementation(project(":core:di:api")) // AppScope (DI scope marker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
