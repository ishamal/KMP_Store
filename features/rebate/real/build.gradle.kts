import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// REAL module: internal implementations + Compose UI + DI contributions. Applies Metro.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.feature.rebate.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":features:rebate:api"))
            implementation(project(":core:di:api"))
            implementation(project(":core:access:api"))  // AccessControl, Capability (REBATE_VIEW)
            implementation(project(":core:session:api")) // SessionManager (logout clears the session)
            implementation(project(":core:navigation:api"))
            implementation(project(":core:ui:api"))
            implementation(libs.compose.runtime)
        }
        androidMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.navigation3.runtime)
            // Molecule MVI + Metro-injected ViewModel (pilot).
            implementation(libs.molecule.runtime)
            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
