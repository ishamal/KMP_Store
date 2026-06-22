import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:session:real — RealSessionManager (the Compose-state-backed SessionManager) + its Metro
// binding. Linked by the app so every store gets the session implementation.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.session.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:session:api"))
            implementation(project(":core:di:api")) // AppScope
            implementation(libs.compose.runtime)    // mutableStateOf
        }
    }
}
