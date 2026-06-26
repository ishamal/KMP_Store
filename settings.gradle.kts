rootProject.name = "KmpProj"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":core:di:api")
include(":core:di:real")
include(":core:model:api")
include(":core:model:real")
include(":core:experience:api")
include(":core:experience:real")
include(":core:session:api")
include(":core:session:real")
include(":core:navigation:api")
include(":core:navigation:real")
include(":core:ui:api")
include(":core:ui:real")
include(":features:login:api")
include(":features:login:real")
include(":features:cart:api")
include(":features:cart:real")
include(":features:invoices:api")
include(":features:invoices:real")
include(":features:settings:api")
include(":features:settings:real")
include(":features:orders:api")
include(":features:orders:real")
include(":features:rebate:api")
include(":features:rebate:real")
include(":features:passwordReset:api")
include(":features:passwordReset:real")
include(":features:test")
