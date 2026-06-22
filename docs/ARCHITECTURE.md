# Architecture — a beginner's guide

> Navigation update: features now self-register a Navigation 3 NavDestination (NavKey + TabMeta + an entry{} installer); the old AppTab and Slot abstractions were removed. See ADDING_STORES_AND_FEATURES.md for the current pattern.

This document explains **how the whole project is put together and how it builds**, assuming you're
new to Kotlin Multiplatform (KMP). Read this first; the other docs go deeper on specific topics:

- [`STORES_AND_FEATURES.md`](STORES_AND_FEATURES.md) — the store/feature design in detail
- [`ADDING_STORES_AND_FEATURES.md`](ADDING_STORES_AND_FEATURES.md) — how-to with copy-paste steps
- [`METRO_DI.md`](METRO_DI.md) — how dependency injection (Metro) works, for beginners
- [`EXPERIENCES.md`](EXPERIENCES.md) — per-user (USBL/CABL) runtime behaviour
- [`TUTORIAL_ADD_ORDERS_FEATURE.md`](TUTORIAL_ADD_ORDERS_FEATURE.md) — a full worked example

---

## 1. What this app is, in one paragraph

It's a **Kotlin Multiplatform** app that runs on **Android and iOS**. The business **logic is
shared** (written once in Kotlin); the **UI is native** on each platform (Jetpack Compose on
Android, SwiftUI on iOS). It ships as **multiple "stores"** (storeA/B/C) that each bundle a
**different set of features** (decided at build time), and within a store each user gets an
**"experience"** (USBL/CABL) that changes what they can see/do (decided at runtime, after login).

## 2. The four ideas that explain everything

Keep these four "axes" separate in your head — most of the design falls out of them:

| Axis | Question | When decided | Mechanism |
|---|---|---|---|
| **Platform** | Android or iOS? | build/run | KMP source sets + native UI |
| **Store** | Which features are in this build? | **build time** | Gradle product flavors + `StoreManifest` |
| **Feature** | What is a unit of functionality? | — | a Gradle module that **self-registers** |
| **Experience** | What may *this user* see/do? | **runtime** (after login) | capabilities in `ExperienceCatalog` |

> Build-time vs runtime is the single most important distinction. A **store** physically removes a
> feature from the binary. An **experience** hides/limits things in a binary that already contains
> them.

> **Android navigation uses Navigation 3.** `App.kt` holds a back stack (`mutableStateListOf<NavKey>`)
> of `TabKey`s and renders the top entry with `NavDisplay`; the bottom `NavigationBar` mutates that
> back stack (tap a tab → reset to its key), and the system back button pops via `onBack`. Features
> stay nav-agnostic — the shell maps each contributed `AppTab` to a `NavEntry`. (Nav3 is Android-only,
> in `androidApp`; drill-down screens later = push another `NavKey`.)

## 3. The building blocks (Gradle modules)

A **module** is a folder with its own `build.gradle.kts` — an independently compiled piece. They're
listed in `settings.gradle.kts`.

```
KmpProj/
├── buildSrc/            "build code". Holds StoreManifest.kt (the store→features table) so
│                        build scripts can read it.
├── core/                tiny shared foundation: AppScope, Experience, Capability,
│                        ExperienceCatalog, Session, SessionManager, AppTab, Slot,
│                        formatPrice. Everyone depends on this.
├── features/            one module per feature — each is self-contained:
│   ├── login/    logic: LoginValidator, Authenticator        ui: LoginScreen
│   ├── cart/     logic: CartItem, CartRepository             ui: CartScreen + CartContribution
│   ├── invoices/ logic: Invoice, InvoiceRepository           ui: InvoicesScreen + InvoicesContribution
│   ├── orders/   logic: Order, OrderRepository               ui: OrdersScreen + OrdersContribution
│   ├── settings/ logic: SettingsRepository                   ui: SettingsScreen + SettingsContribution
│   └── rebate/   logic: RebateRepository                     ui: RebateSection + RebateContribution
├── shared/              builds the iOS "Shared" framework + tiny common code (Greeting/Platform).
├── androidApp/          the Android app: App.kt (navigation shell) + one AppGraph (DI).
└── iosApp/              the iOS app: SwiftUI (ContentView.swift) + Xcode project/config.
```

Dependency direction (who depends on whom):
```
features/*  ─depends on→  core
androidApp  ─depends on→  shared, core, and the feature modules of the active flavor
shared      ─depends on→  the feature modules of the active store (for the iOS framework)
```
`core` depends on nobody (no cycles). That's why the DI scope marker (`AppScope`) lives there.

## 4. KMP source sets (the folders inside a module)

Inside a KMP module, code is split by target platform — **by folder-name convention**:

- `src/commonMain/kotlin/…` — Kotlin shared by **all** platforms → **logic** goes here.
- `src/androidMain/kotlin/…` — **Android-only** Kotlin → **Jetpack Compose** UI goes here.
- `src/iosMain/kotlin/…` — iOS-only Kotlin (used in `shared`).

Rule of thumb in this project: **logic in `commonMain`, Compose UI in `androidMain`, SwiftUI in
`iosApp`.** (iOS UI can't be Kotlin, so it lives in the iOS app, not in a module.)

## 5. The key technologies (and why)

- **Kotlin Multiplatform** — write logic once, run on Android + iOS.
- **Jetpack Compose / SwiftUI** — native UI per platform (we deliberately don't share UI).
- **Gradle + version catalog** (`gradle/libs.versions.toml`) — one place for dependency/plugin
  versions; build files reference `libs.…`.
- **Product flavors** (Android) — build the same app several ways (storeA/B/C), each with its own
  `applicationId` and its own set of feature modules.
- **Metro** (`dev.zacsweers.metro`) — compile-time **dependency injection**. Instead of a screen
  doing `CartRepository()` itself, a central **graph** creates objects and hands them out. "Compile
  time" = it's a Kotlin compiler plugin (no reflection, errors caught at build).

## 6. How a build is assembled (the pipeline)

### The single source of truth
`buildSrc/StoreManifest.kt` maps each store to the feature modules it ships:
```kotlin
"storeA" to listOf("login", "cart", "invoices", "settings", "orders"),
"storeB" to listOf("login", "cart", "settings", "rebate"),
"storeC" to listOf("login", "settings", "orders"),
```

### Android build (one flavor = one store)
1. `androidApp/build.gradle.kts` reads the manifest and **generates a product flavor per store**,
   and for each flavor adds **only that store's feature modules** as dependencies.
2. You pick a **Build Variant** (e.g. `storeADebug`) in Android Studio (or run
   `./gradlew :androidApp:assembleStoreADebug`).
3. Gradle compiles `core`, the linked feature modules, `shared`, and `androidApp` for that flavor.
4. The **Metro** compiler plugin builds the DI graph, collecting each linked feature's
   contributions (see §7).
5. Output: an APK that contains **only** that store's features. (storeB's APK literally has no
   invoices code.)

### iOS build (one store per build, chosen by Xcode scheme)
1. Xcode runs a build phase: `./gradlew :shared:embedAndSignAppleFrameworkForXcode -Pstore=<store>`
   (the store comes from the scheme's `.xcconfig`).
2. `shared/build.gradle.kts` reads the manifest and **exports that store's feature logic** into a
   `Shared` framework (a binary Swift can import).
3. Xcode compiles the SwiftUI app (`iosApp`) against `Shared`. `#if STORE_HAS_*` flags hide
   per-store Swift code.
4. Output: an `.app` whose framework contains only that store's feature logic.

## 7. How features plug in (self-registration via DI)

Adding a feature must **not** require editing the app. So each feature **contributes itself** to the
DI graph, and the app just renders whatever's there.

- A feature's `androidMain` has a small contribution object, e.g.
  `features/orders/.../OrdersContribution.kt`:
  ```kotlin
  @ContributesTo(AppScope::class) @BindingContainer
  object OrdersContribution {
      @Provides @IntoSet
      fun ordersTab(repository: OrderRepository): AppTab =
          AppTab("Orders", "📦", order = 20, requiredCapability = Capability.VIEW_ORDERS) { _, _ ->
              OrdersScreen(repository = repository)
          }
  }
  ```
  - `@ContributesTo(AppScope::class)` → "add me to the app graph."
  - `@Provides @IntoSet … : AppTab` → "put this tab into the graph's **set of tabs**" (a
    *multibinding* — many modules add into one `Set<AppTab>`).
- The app has **one** graph, `androidApp/.../di/AppGraph.kt`:
  ```kotlin
  @DependencyGraph(AppScope::class)
  interface AppGraph {
      @Multibinds(allowEmpty = true) val tabs: Set<AppTab>
      @Multibinds(allowEmpty = true) val slots: Set<Slot>
      val loginValidator; val authenticator; val sessionManager
  }
  ```
- `App.kt` renders `graph.tabs` (sorted by `order`, filtered by the user's experience).

Because a flavor only links its own feature modules, only those contribute → the tab set differs per
store **automatically**, with **no per-store code** (no `androidApp/src/storeX` folders).

A feature that belongs *inside* another screen (like `rebate` inside Settings) contributes a generic
`Slot` (tagged with a `SlotId`) instead of an `AppTab`; the host screen renders matching slots with
`SlotHost(slots, SlotId.X)`. This works for any host, not just Settings.

## 8. Experiences & capabilities (runtime gating)

After login the user has an **Experience** (USBL/CABL). What each experience can do is one central
table, `core/.../Capability.kt`:
```kotlin
enum class Capability { VIEW_PAID_INVOICES, VIEW_PENDING_INVOICES, VIEW_OVERDUE_INVOICES, EXPORT_INVOICES, VIEW_ORDERS }
object ExperienceCatalog {
    private val capabilities = mapOf(
        Experience.USBL to Capability.entries.toSet(),               // everything
        Experience.CABL to setOf(Capability.VIEW_PENDING_INVOICES),  // restricted
    )
}
fun Experience.has(capability: Capability) = capability in ExperienceCatalog.capabilitiesOf(this)
```
The code never compares experiences (`experience == …`); it asks `experience.has(Capability.X)`:
- **Hide a tab/action**: `AppTab(..., requiredCapability = VIEW_ORDERS)` / `if (experience.has(EXPORT_INVOICES))`.
- **Scope data without branching**: filter rows by a per-row capability (e.g. invoices show a row
  only if the experience holds the capability for that row's status). Adding an experience never
  adds an `if` — you just edit the catalog. (Details in `EXPERIENCES.md`.)

## 9. End-to-end runtime flow

```
App() ──creates──► AppGraph (Metro)            // one graph for this store/flavor
  │
  └─ no session yet ─► LoginScreen(validator, authenticator)
        user logs in ─► Authenticator → Session(email, experience)   // experience from "backend" (stubbed)
        store it in graph.sessionManager, set local state
  │
  └─ has session ─► MainScaffold
        tabs = graph.tabs                         // all linked features' tabs (multibinding)
                 .sortedBy { order }              // stable order
                 .filter { it.requiredCapability == null || experience.has(it) }   // runtime gate
        render bottom nav + the selected screen
```

**Worked example — storeA, CABL user:**
- Build (storeA) contains cart, invoices, settings, orders → their tabs are contributed.
- Login as `cabl@…` → experience = CABL.
- `Orders` tab has `requiredCapability = VIEW_ORDERS`; CABL lacks it → **Orders tab hidden**.
- `Invoices` tab shows, but `invoicesFor(CABL)` returns **pending only**, and the **Export** action
  (needs `EXPORT_INVOICES`) is hidden.
- Result: CABL sees Cart, Invoices (pending, no export), Settings.

## 10. Glossary (quick beginner reference)

- **Module** — a folder with `build.gradle.kts`, compiled independently; listed in `settings.gradle.kts`.
- **Source set** — `commonMain` / `androidMain` / `iosMain` folders that target platforms.
- **Product flavor** — a build variant of the Android app (storeA/B/C) selectable in *Build Variants*.
- **Version catalog** — `gradle/libs.versions.toml`, central dependency/plugin versions.
- **DI / graph** — dependency injection; the graph creates and supplies objects (via Metro).
- **`@Inject` / `@SingleIn`** — "Metro may construct this" / "keep one instance for the app's life".
- **Multibinding (`@IntoSet`)** — many modules contribute elements into one `Set<…>` the app reads.
- **`AppScope`** — the DI scope marker (in `core`); everything is scoped to the app's lifetime.
- **Store** — a build-time bundle of features (flavor + `StoreManifest`), own `applicationId`.
- **Experience** — a runtime user profile (USBL/CABL) → a set of **capabilities**.
- **Capability** — a named permission/visibility flag, mapped per experience in `ExperienceCatalog`.

## 11. Build & run cheat-sheet

```
# Android (pick the store via the variant name)
./gradlew :androidApp:assembleStoreADebug        # build APK
./gradlew :androidApp:installStoreBDebug         # build + install on device/emulator
# in Android Studio: Build > Select Build Variant… → storeA/B/CDebug → ▶ Run
# log in as cabl@x.com (CABL) vs user@x.com (USBL) to see experiences differ

# iOS (pick the store via the Xcode scheme)
open iosApp/iosApp.xcodeproj   # choose scheme "iosApp" (storeA) or "iosApp-storeB" → Run

# iOS framework only (CI / sanity)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeA
```

---

## 12. Setting up this store/feature management in a NEW project

If you're starting fresh, here's the order to build the same system. Each step is small; the result
is "add a store = 1 line, add a feature = 1 module that self-registers."

### Step 0 — Start from a KMP template
Create a Kotlin Multiplatform app (Android Studio's **New Project → Kotlin Multiplatform**, or the
[KMP wizard](https://kmp.jetbrains.com/)). You'll get `shared` + `androidApp` + `iosApp`. Confirm it
runs on both platforms before adding anything.

### Step 1 — Version catalog: add the plugins
In `gradle/libs.versions.toml` add Metro and (if not present) Compose Multiplatform + the Compose
compiler plugin:
```toml
[versions]
metro = "1.2.1"          # must support your Kotlin version
[plugins]
metro = { id = "dev.zacsweers.metro", version.ref = "metro" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### Step 2 — `buildSrc` + the manifest (the store→features table)
Create `buildSrc/build.gradle.kts` (`plugins { \`kotlin-dsl\` }`, repos mavenCentral+google) and
`buildSrc/src/main/kotlin/StoreManifest.kt`:
```kotlin
object StoreManifest {
    const val DEFAULT_STORE = "storeA"
    val stores: Map<String, List<String>> = linkedMapOf(
        "storeA" to listOf("login", "settings"),   // start tiny; grow later
    )
    fun featuresFor(store: String) = stores[store] ?: error("Unknown store '$store'")
    fun applicationId(store: String) = "com.example.app.${store.lowercase()}"
}
```

### Step 3 — `:core` foundation
A small KMP module (kotlinMultiplatform + androidMultiplatformLibrary + metro + compose plugins).
Put in it: `AppScope` (DI scope marker), `Experience`/`Session`, `Capability` + `ExperienceCatalog`
+ `Experience.has(...)`, `SessionManager`, and the UI contribution types `AppTab`/`Slot`
(in `androidMain`). Add `compose.runtime` to `commonMain`, `compose.ui` to `androidMain`. Everything
else depends on `:core`; `:core` depends on nothing (avoids cycles).

### Step 4 — `shared` builds the iOS framework from the manifest
In `shared/build.gradle.kts` read the manifest and export the active store's features:
```kotlin
val store = providers.gradleProperty("store").getOrElse(StoreManifest.DEFAULT_STORE)
val storeFeatures = StoreManifest.featuresFor(store)
// in the framework block:  storeFeatures.forEach { export(project(":features:$it")) }
// in iosMain deps:         storeFeatures.forEach { api(project(":features:$it")) }
```

### Step 5 — `androidApp`: flavors + the one graph + the shell
- `androidApp/build.gradle.kts`: generate a flavor per store and link each store's modules:
  ```kotlin
  flavorDimensions += "store"
  productFlavors {
      StoreManifest.stores.keys.forEach { s -> create(s) { dimension = "store"; applicationId = StoreManifest.applicationId(s) } }
  }
  dependencies {
      implementation(projects.core)
      StoreManifest.stores.forEach { (s, feats) -> feats.forEach { add("${s}Implementation", project(":features:$it")) } }
  }
  ```
- `androidApp/src/main/.../di/AppGraph.kt`: the single store-agnostic graph with
  `@Multibinds(allowEmpty = true) val tabs: Set<AppTab>` (+ `slots`, session accessors)
  and `fun createAppGraph() = createGraph<AppGraph>()`.
- `androidApp/src/main/.../App.kt`: login gate → then render `graph.tabs` sorted by `order` and
  filtered by `experience.has(requiredCapability)`. **This file never changes when you add features.**

### Step 6 — Add feature modules that self-register
For each feature: a module (`features/<name>`) with logic in `commonMain` (`@Inject @SingleIn`
repository), Compose screen in `androidMain`, and a contribution object
(`@ContributesTo(AppScope::class) @BindingContainer object … { @Provides @IntoSet fun tab(...): AppTab = … }`).
Then `include(":features:<name>")` in `settings.gradle.kts` and add the name to the right stores in
`StoreManifest`. Nothing in `androidApp` changes.

### Step 7 — iOS per-store config
Per store: a `Config-<store>.xcconfig` (bundle id, `GRADLE_STORE`, `STORE_HAS_*` flags), Xcode build
configurations, and a scheme; the framework build phase passes `-Pstore=$GRADLE_STORE`.

### From then on — day-to-day management
- **New store:** one row in `StoreManifest` (Android is done) + iOS config/scheme.
- **New feature:** new module + `include` + manifest entry + one contribution file.
- **Restrict per user:** add a `Capability`, grant it in `ExperienceCatalog`, gate with
  `requiredCapability` / `experience.has(...)`.

That's the entire "store + feature management" system: a manifest for build-time composition, DI
multibindings for self-registration, and a capability catalog for runtime experiences.
