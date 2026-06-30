// KEELS string resources — a resource-only Android library. Its own namespace gives it its own R, so
// it can use the SAME string keys as the other brand modules without clashing. androidApp picks the
// active brand's strings at runtime (see androidApp BrandStrings.kt). To localize: add res/values-<locale>/.
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.isharaw.kmpproj.branding.keells"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
