import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
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

    // Per-store builds, generated from buildSrc/StoreManifest.kt. Pick the variant
    // (storeADebug / storeBDebug / …) from the Build Variants panel in Android Studio.
    // Each store ships only the feature modules listed for it in the manifest.
    flavorDimensions += "store"
    productFlavors {
        StoreManifest.stores.keys.forEach { store ->
            create(store) {
                dimension = "store"
                applicationId = StoreManifest.applicationId(store)
            }
        }
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

    // Each flavor pulls in exactly the feature :real modules its store declares (:api is transitive).
    StoreManifest.stores.forEach { (store, features) ->
        features.forEach { feature ->
            add("${store}Implementation", project(":features:$feature:real"))
        }
    }
}
