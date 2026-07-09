import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
    // Creates the per-store product flavors and links each store's feature :real modules.
    id("com.isharaw.store-features")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.isharaw.kmpproj"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.isharaw.kmpproj"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    // Per-store product flavors (storeADebug / storeBDebug / …) and their feature :real modules are
    // created by the com.isharaw.store-features convention plugin from the build-logic STORES table.

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.di.api)
    implementation(projects.core.model.api)
    implementation(projects.core.navigation.api)
    implementation(projects.core.ui.api)
    implementation(projects.core.session.api)
    // Links the SessionManager implementation into the app graph for every store.
    implementation(projects.core.session.real)
    // ExperienceProvider binding (reads the resolved snapshot off the session).
    implementation(projects.core.experience.real)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Metro ViewModel integration (graph builds the VM factory; screens use metroViewModel()).
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    // Per-store feature :real modules are added by the com.isharaw.store-features convention plugin
    // (via `${'$'}{store}Implementation`), so unshipped features are removed from the build.
}
