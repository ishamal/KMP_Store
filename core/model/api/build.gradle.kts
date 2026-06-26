import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:model — pure domain types (TabMeta, …) shared by every feature and by iOS. The access
// snapshot model (Experience/BusinessUnit/UserRole/Feature/ExperienceSnapshot) lives in
// :core:experience:api, re-exported here so existing consumers keep getting it. No Compose.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.model"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:experience:api")) // TabMeta.feature: Feature; re-export the snapshot model
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
