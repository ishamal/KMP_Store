// Standalone included build that provides this project's convention plugins.
// Wired into the main build via `pluginManagement { includeBuild("build-logic") }` in the root
// settings.gradle.kts. Unlike buildSrc, editing this does NOT reconfigure the whole project.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Re-use the main project's version catalog so the AGP version can't drift.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
