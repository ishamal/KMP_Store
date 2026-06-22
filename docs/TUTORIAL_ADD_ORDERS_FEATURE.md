# Tutorial: Add a Feature from Scratch (worked example: `orders`)

> Navigation update: features now self-register a Navigation 3 NavDestination (NavKey + TabMeta + an entry{} installer); the old AppTab and Slot abstractions were removed. See ADDING_STORES_AND_FEATURES.md for the current pattern.

A beginner-friendly, end-to-end walkthrough for **people new to Kotlin Multiplatform (KMP)**.
We build a real `orders` feature and explain *every* step and *why*. By the end you'll understand
modules, source sets, dependency injection (DI), the store manifest, and how a screen reaches the
app. Everything here is exactly what was done to add `orders` to this project.

> Related: architecture overview in [`STORES_AND_FEATURES.md`](STORES_AND_FEATURES.md);
> the condensed how-to in [`ADDING_STORES_AND_FEATURES.md`](ADDING_STORES_AND_FEATURES.md).

---

## 0. Concepts you need first (2-minute primer)

- **Module** = an independent Gradle project (a folder with its own `build.gradle.kts`). This app
  is split into many: `:core`, `:shared`, `:androidApp`, and one per feature under `:features:*`.
  Small modules build faster and keep features isolated.
- **KMP source sets** = folders that target different platforms inside one module:
  - `src/commonMain` — Kotlin shared by **all** platforms (Android + iOS). Put **logic** here.
  - `src/androidMain` — **Android-only** Kotlin. Put **Jetpack Compose UI** here.
  - (`src/iosMain` exists in `:shared` for iOS-only Kotlin.)
  Rule of thumb in this project: *logic is shared, UI is native* (Compose on Android, SwiftUI on iOS).
- **Dependency Injection (DI)** = instead of a screen doing `OrderRepository()` itself, something
  central (the "graph") creates it and hands it in. We use **Metro**, a compile-time DI library.
  - `@Inject` on a class = "Metro, you may construct this."
  - `@SingleIn(AppScope::class)` = "keep one instance for the app's lifetime" (a singleton).
  - `@DependencyGraph` interface = the catalogue of things you can pull out; `createGraph<…>()`
    builds it.
- **The manifest** = [`buildSrc/StoreManifest.kt`](../buildSrc/src/main/kotlin/StoreManifest.kt),
  the single list of *which features each store ships*. Adding a feature name there wires the
  Gradle dependencies automatically.
- **Stores / flavors** = this app ships as `storeA`, `storeB`, `storeC` — different builds with
  different feature sets. On Android these are **product flavors** (Build Variants dropdown).

We'll add `orders` to **storeA**.

---

## Step 1 — Create the feature module folder + `build.gradle.kts`

A module is just a folder with a `build.gradle.kts`. Create:

```
features/orders/
  build.gradle.kts
  src/commonMain/kotlin/com/isharaw/kmpproj/feature/orders/
  src/androidMain/kotlin/com/isharaw/kmpproj/feature/orders/
```

### 1a. Creating the `commonMain` / `androidMain` folder structure

⚠️ **These folders are NOT created automatically.** When you make a new directory under
`features/`, it's empty — there's no `src/commonMain` etc. KMP recognizes source sets purely **by
folder name convention**, so *you* create them. Two things must line up:

- **Source-set name** — exactly `commonMain` (all platforms) or `androidMain` (Android only),
  placed under `src/`.
- **Package path** — the folders after `kotlin/` must match your `package` line. For
  `package com.isharaw.kmpproj.feature.orders` the path is `com/isharaw/kmpproj/feature/orders`.

**Way 1 — Terminal (fastest).** `mkdir -p` makes all nested folders at once:
```bash
mkdir -p features/orders/src/commonMain/kotlin/com/isharaw/kmpproj/feature/orders
mkdir -p features/orders/src/androidMain/kotlin/com/isharaw/kmpproj/feature/orders
```

**Way 2 — Android Studio.**
1. Switch the Project panel from the **Android** view to the **Project** view (dropdown at the top
   of the Project tool window). The *Android* view hides/regroups raw folders, which is why a new
   module "shows no structure" — the *Project* view shows the real files on disk.
2. Right-click the module → **New → Directory**, then type the **whole nested path with slashes**
   and press Enter — Android Studio creates every level:
   ```
   src/commonMain/kotlin/com/isharaw/kmpproj/feature/orders
   ```
   Repeat for `src/androidMain/kotlin/com/isharaw/kmpproj/feature/orders`.
3. Right-click that leaf folder → **New → Kotlin Class/File** to add code.

**Why the folders may still look "plain" (not highlighted source roots):** a folder only becomes a
recognized source root after (a) the module's `build.gradle.kts` declares the targets, and (b) the
module is `include`d in `settings.gradle.kts` (Step 2) **and you run Gradle Sync**. Before sync,
they're just ordinary folders and imports won't resolve.

**Path = colons map to folders:** `include(":features:orders")` → `features/orders/`;
`include(":orders")` → `orders/` at the project root. Put a feature under `features/` and use the
`:features:<name>` form.

> *Wizard alternative:* **File → New → New Module… → Kotlin Multiplatform Shared Module** scaffolds
> the folders and adds the `include(...)` for you, but you'll still rewrite `build.gradle.kts` to
> match the other features — copying an existing feature is usually faster.

### 1b. The `build.gradle.kts`

`features/orders/build.gradle.kts` — **copy `features/settings/build.gradle.kts`** and change the
namespace. Here's what each part means:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)        // makes this a KMP module
    alias(libs.plugins.androidMultiplatformLibrary) // adds the Android (library) target
    alias(libs.plugins.metro)                       // DI compiler plugin
    alias(libs.plugins.composeMultiplatform)        // Compose tooling
    alias(libs.plugins.composeCompiler)             // compiles @Composable functions
}

kotlin {
    iosArm64(); iosSimulatorArm64()                 // iOS targets (logic compiles for iOS too)

    androidLibrary {
        namespace = "com.isharaw.kmpproj.feature.orders"   // unique per module
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))        // gives us AppScope + formatPrice
            implementation(libs.compose.runtime)    // see note ⬇
        }
        androidMain.dependencies {                  // the actual UI libs — Android only
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
```

> **Why `compose.runtime` in `commonMain`?** The Compose *compiler* plugin runs on every target
> (including iOS) and refuses to compile unless the Compose runtime is on the classpath there. The
> runtime is multiplatform and tiny, so it goes in `commonMain`; the heavy UI artifacts stay
> Android-only. This keeps the iOS framework Compose-free.

---

## Step 2 — Register the module in `settings.gradle.kts`

Gradle only knows about modules you `include`. Add one line:

```kotlin
include(":features:orders")
```

*(This stays manual — `buildSrc`/the manifest isn't available this early in the build.)* After
this, do a **Gradle Sync** in Android Studio (elephant icon / the "Sync Now" banner).

---

## Step 3 — Write the logic in `commonMain`

`features/orders/src/commonMain/.../feature/orders/Orders.kt`:

```kotlin
package com.isharaw.kmpproj.feature.orders

import com.isharaw.kmpproj.core.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

enum class OrderStatus { PROCESSING, SHIPPED, DELIVERED }

data class Order(val number: String, val date: String, val itemCount: Int,
                 val total: Double, val status: OrderStatus)

@Inject                          // Metro may construct this
@SingleIn(AppScope::class)       // one instance for the app's lifetime
class OrderRepository {
    fun all(): List<Order> = listOf(/* sample data */)
}
```

This is **pure Kotlin, no UI** — it lives in `commonMain` so iOS can reuse it later. The two
annotations are the entire DI contract for this class.

---

## Step 4 — Write the Compose screen in `androidMain`

`features/orders/src/androidMain/.../feature/orders/OrdersScreen.kt`:

```kotlin
package com.isharaw.kmpproj.feature.orders
// …compose imports…
import com.isharaw.kmpproj.core.formatPrice   // shared currency helper from :core

@Composable
fun OrdersScreen(repository: OrderRepository) {   // repo is passed IN (DI), not created here
    val orders = remember { repository.all() }
    // …LazyColumn of order rows…
}
```

Key idea: the screen **receives** its `OrderRepository` as a parameter. It never calls
`OrderRepository()` itself — the DI graph provides it (Step 6). This is what makes it testable and
swappable.

---

## Step 5 — Add `orders` to the store manifest

[`buildSrc/StoreManifest.kt`](../buildSrc/src/main/kotlin/StoreManifest.kt) — add `"orders"` to
each store that should ship it. We chose **storeA**:

```kotlin
"storeA" to listOf("login", "cart", "invoices", "settings", "orders"),
```

This one edit makes the build scripts add `:features:orders` to storeA's classpath **and** export
it to the iOS framework for storeA — no other Gradle edits needed (the scripts loop over the
manifest).

---

## Step 6 — Self-register the tab (DI + navigation in one file)

The feature registers **itself** — there's no per-store graph or app-side edit. In the feature's
`androidMain`, add `features/orders/src/androidMain/.../OrdersContribution.kt`:

```kotlin
@ContributesTo(AppScope::class)
@BindingContainer
object OrdersContribution {
    @Provides @IntoSet
    fun ordersTab(repository: OrderRepository): AppTab =
        AppTab("Orders", "📦", order = 20) { _, _ -> OrdersScreen(repository = repository) }
}
```

What's happening:
- `@ContributesTo(AppScope::class)` adds this to the app's DI graph automatically (because the
  module is on storeA's classpath, per Step 5's manifest entry).
- `@Provides @IntoSet ... : AppTab` puts the tab into the graph's `Set<AppTab>` multibinding.
- You never write *how* to build `OrderRepository` — Metro sees its `@Inject` (Step 3) and supplies
  it as the `repository` parameter.
- `order` positions the tab. Add `requiredCapability = Capability.X` to hide it for some
  experiences; inject `SessionManager` if the screen needs the current experience.

That's the whole wiring. The single `AppGraph` and `App.kt` (in `androidApp/src/main`) are
**store-agnostic** — `App.kt` just renders `graph.tabs`, so you never touch them to add a feature,
and there are **no `androidApp/src/storeX` source sets**.

---

## Step 8 — Build & run in Android Studio

1. **Sync Gradle** (so the new module/manifest are picked up).
2. Open the **Build Variants** panel (left edge, or **View → Tool Windows → Build Variants**) and
   set the `:androidApp` variant to **`storeADebug`**.
3. Press **▶ Run**. You should see the bottom bar: **Cart · Orders · Invoices · Settings**.

Command-line equivalent:
```
./gradlew :androidApp:assembleStoreADebug      # build
./gradlew :androidApp:installStoreADebug       # install on a running emulator/device
```

Sanity checks (optional):
```
./gradlew :androidApp:dependencies --configuration storeADebugRuntimeClasspath | grep -c features:orders  # 1
./gradlew :androidApp:dependencies --configuration storeBDebugRuntimeClasspath | grep -c features:orders  # 0
```

---

## Step 9 — (Optional) Make it appear on iOS

Adding `orders` to storeA's manifest already makes `:shared` export `OrderRepository` to the iOS
framework. To actually show it in the iOS app you'd:
1. Add `val orderRepository: OrderRepository` to the iOS graph
   (`shared/src/iosStoreWithInvoices/.../di/IosAppGraph.kt`).
2. Add `STORE_HAS_ORDERS` to `SWIFT_ACTIVE_COMPILATION_CONDITIONS` in `iosApp/Configuration/Config.xcconfig`.
3. In `iosApp/iosApp/ContentView.swift`, add an `OrdersView(repository: appGraph.orderRepository)`
   tab guarded by `#if STORE_HAS_ORDERS`, and write that SwiftUI view.

*(Ask and I can do the iOS side — it mirrors the invoices setup.)*

---

## Recap — the 6 edits to add a feature (all inside the feature module + 2 registry files)

| # | File | What |
|---|---|---|
| 1 | `features/orders/build.gradle.kts` | new module (copy a feature, change namespace) |
| 2 | `features/orders/src/commonMain/.../Orders.kt` | logic + `@Inject @SingleIn` repository |
| 3 | `features/orders/src/androidMain/.../OrdersScreen.kt` | Compose screen (takes repo as param) |
| 4 | `settings.gradle.kts` | `include(":features:orders")` |
| 5 | `buildSrc/StoreManifest.kt` | add `"orders"` to the stores that ship it |
| 6 | `features/orders/src/androidMain/.../OrdersContribution.kt` | self-register the tab (`@ContributesTo` + `@Provides @IntoSet AppTab`) |

Note: **no `androidApp` edits** — the app shell renders `graph.tabs`, and the feature contributes
its tab itself. No per-store source sets.

## If something doesn't compile
- **The Orders tab doesn't appear** → missing the contribution (Step 6) or the manifest entry
  (Step 5), so nothing was contributed for that store. Re-sync Gradle.
- **`Unresolved reference` to `AppTab`/`Capability`** → the contribution must be in the feature's
  `androidMain` and the module must depend on `:core` (the feature build file already does).
- **"Compose Runtime not on the class path"** → put `implementation(libs.compose.runtime)` in
  `commonMain` (the Compose compiler runs on every target); UI artifacts stay in `androidMain`.
- After any Gradle file change, always **Sync Gradle** before running.
