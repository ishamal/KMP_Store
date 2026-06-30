// CARGILLS string resources (see :core:branding:keells for the per-brand resource-module pattern).
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.isharaw.kmpproj.branding.cargills"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
