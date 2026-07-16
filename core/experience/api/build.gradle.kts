import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:experience — the access SNAPSHOT model the backend resolves at login:
// Experience, BusinessUnit, UserRole, Feature, ExperienceSnapshot, ExperienceProvider.
// Pure types + coroutines (StateFlow in ExperienceProvider), no Compose.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.experience"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            // StateFlow is part of the ExperienceProvider surface, so consumers need it too.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
