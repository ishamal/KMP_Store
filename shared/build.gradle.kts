import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

// The active store (defaults to StoreManifest.SELECTED_STORE) decides which feature modules
// the iOS framework links and exports. See buildSrc/StoreManifest.kt.
val store = providers.gradleProperty("store").getOrElse(StoreManifest.SELECTED_STORE)
val storeFeatures = StoreManifest.featuresFor(store)

// Feature -> (repository type, graph accessor) exposed to Swift through the iOS framework. The
// IosAppGraph is GENERATED below with only the accessors for features this store actually ships, so
// the graph always matches the build (no hand-written per-store graph variants). Mirrors how the
// Android app self-assembles its tabs: add a feature here and any store that ships it gets it on iOS.
val iosGraphAccessors = linkedMapOf(
    "login" to ("com.isharaw.kmpproj.feature.login.LoginValidator" to "loginValidator"),
    "cart" to ("com.isharaw.kmpproj.feature.cart.CartRepository" to "cartRepository"),
    "settings" to ("com.isharaw.kmpproj.feature.settings.SettingsRepository" to "settingsRepository"),
    "invoices" to ("com.isharaw.kmpproj.feature.invoices.InvoiceRepository" to "invoiceRepository"),
    "orders" to ("com.isharaw.kmpproj.feature.orders.OrderRepository" to "orderRepository"),
)

val generatedIosGraphDir = layout.buildDirectory.dir("generated/iosGraph/kotlin")

// Writes shared/build/generated/iosGraph/.../di/IosAppGraph.kt from this store's features. The
// `inputs.property` makes the task re-run when the store (-Pstore) changes its accessor set.
val generateIosAppGraph by tasks.registering {
    val outDir = generatedIosGraphDir
    val members = iosGraphAccessors.filterKeys { it in storeFeatures }.values.toList()
    inputs.property("members", members.map { "${it.first}:${it.second}" })
    outputs.dir(outDir)
    doLast {
        val imports = members.joinToString("") { (type, _) -> "import $type\n" }
        val accessors = members.joinToString("") { (type, name) ->
            "    val $name: ${type.substringAfterLast('.')}\n"
        }
        val code = buildString {
            appendLine("package com.isharaw.kmpproj.di")
            appendLine()
            appendLine("import com.isharaw.kmpproj.core.AppScope")
            appendLine("import dev.zacsweers.metro.DependencyGraph")
            appendLine("import dev.zacsweers.metro.createGraph")
            append(imports)
            appendLine()
            appendLine("/** GENERATED from the active store's features (-Pstore). Exposes only shipped features to Swift. */")
            appendLine("@DependencyGraph(AppScope::class)")
            appendLine("interface IosAppGraph {")
            append(accessors)
            appendLine("}")
            appendLine()
            appendLine("/** Swift entry point — `createGraph` is a compile-time intrinsic, so Swift goes through this. */")
            appendLine("fun createIosAppGraph(): IosAppGraph = createGraph<IosAppGraph>()")
        }
        val pkgDir = outDir.get().asFile.resolve("com/isharaw/kmpproj/di").apply { mkdirs() }
        pkgDir.resolve("IosAppGraph.kt").writeText(code)
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            // Export only the public :api contracts to Swift (never the Real impls).
            storeFeatures.forEach { export(project(":features:$it:api")) }
        }
    }

    androidLibrary {
       namespace = "com.isharaw.kmpproj.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       withHostTest {}
    }

    sourceSets {
        // Features are linked iOS-only here (api, so they are exportable from the framework).
        // The Android app pulls feature modules in directly per product flavor, so the Android
        // side of `shared` never carries store-specific features.
        iosMain.dependencies {
            implementation(project(":core:di:api"))
            implementation(project(":core:model:api"))
            implementation(project(":core:session:api"))
            storeFeatures.forEach {
                api(project(":features:$it:api"))            // contracts (exported)
                implementation(project(":features:$it:real")) // impls (linked, internal, not exported)
            }
        }
        // The iOS DI graph (IosAppGraph) is generated per store from its feature list — see
        // `generateIosAppGraph` above. Wiring the task as a source dir makes compilation depend on it.
        iosMain { kotlin.srcDir(generateIosAppGraph) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
