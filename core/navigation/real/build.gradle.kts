import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:navigation:real — no implementation today (navigation primitives are contracts/types in
// :core:navigation:api). Exists to mirror the feature api/real convention; bundles :core:navigation:api.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.navigation.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":core:navigation:api"))
        }
    }
}
