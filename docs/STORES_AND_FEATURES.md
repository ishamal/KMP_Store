# Stores & Features Architecture

> Navigation update: features now self-register a Navigation 3 NavDestination (NavKey + TabMeta + an entry{} installer); the old AppTab and Slot abstractions were removed. See ADDING_STORES_AND_FEATURES.md for the current pattern.

This app is a Kotlin Multiplatform (Android + iOS) project that ships to **multiple stores**,
where each store ships a **different set of features**. Feature membership is removed at
**build time** (not just hidden), and the whole thing is driven by a single manifest.

> Adding a store or feature? See the step-by-step guide with copy-paste examples and a
> troubleshooting table: [`ADDING_STORES_AND_FEATURES.md`](ADDING_STORES_AND_FEATURES.md).
>
> Per-user behaviour (USBL/CABL **experiences** — a *runtime* concern, distinct from build-time
> stores; driven by a central capability catalog): [`EXPERIENCES.md`](EXPERIENCES.md).

---

## 1. Big picture

```
buildSrc/StoreManifest.kt        <-- single source of truth: store -> [features]
        │
        ├── shared/build.gradle.kts      loops the manifest -> iOS framework exports
        └── androidApp/build.gradle.kts  loops the manifest -> Android flavors + deps

features/                         self-contained feature modules (logic + Android UI + DI contribution)
  ├── login/      commonMain: LoginValidator              androidMain: LoginScreen
  ├── cart/       commonMain: CartItem, CartRepository     androidMain: CartScreen + CartContribution
  ├── invoices/   commonMain: Invoice, InvoiceRepository   androidMain: InvoicesScreen + InvoicesContribution
  └── settings/   commonMain: SettingsRepository           androidMain: SettingsScreen + SettingsContribution

androidApp/      navigation shell only (App + one store-agnostic AppGraph) — renders graph.tabs
iosApp/          SwiftUI UI          (iOS-native, consumes the Shared framework)
shared/          KMP "glue": builds the iOS `Shared` framework + common non-feature code
```

Key principles:

- **UI is not shared across platforms.** Android uses Jetpack Compose; iOS uses SwiftUI. Only
  **logic** is shared (the `features/*` `commonMain` and `shared/`).
- **Each feature owns its Android UI.** A feature module's `commonMain` holds the logic
  (models + repositories / validators) and its `androidMain` holds the Compose screen. iOS
  screens stay in `iosApp` (SwiftUI can't live in a Kotlin module). Compose is Android-only:
  feature `commonMain` carries just `compose.runtime` (needed wherever the Compose compiler runs);
  the UI artifacts (`foundation`/`material3`/`ui`) are `androidMain`-only, so the iOS framework
  stays Compose-free.
- **One manifest decides everything.** [`buildSrc/StoreManifest.kt`](../buildSrc/src/main/kotlin/StoreManifest.kt)
  lists which features each store ships. Build scripts loop over it — there are no
  per-feature `if` branches.

---

## 2. The manifest — `buildSrc/StoreManifest.kt`

```kotlin
object StoreManifest {
    const val DEFAULT_STORE = "storeA"

    val stores: Map<String, List<String>> = linkedMapOf(
        "storeA" to listOf("login", "cart", "invoices", "settings"),
        "storeB" to listOf("login", "cart", "settings"),
    )

    fun featuresFor(store: String): List<String> = stores[store] ?: error("Unknown store '$store'…")
    fun applicationId(store: String): String = "com.isharaw.kmpproj.${store.lowercase()}"
}
```

It lives in `buildSrc` so it is a typed object importable from any `*.build.gradle.kts`.
An unknown `-Pstore=` value fails the build fast.

---

## 3. How each layer consumes the manifest

### Android — product flavors (`androidApp/build.gradle.kts`)

Flavors and per-flavor feature dependencies are **generated** from the manifest:

```kotlin
flavorDimensions += "store"
productFlavors {
    StoreManifest.stores.keys.forEach { store ->
        create(store) { dimension = "store"; applicationId = StoreManifest.applicationId(store) }
    }
}

dependencies {
    // …compose deps…
    StoreManifest.stores.forEach { (store, features) ->
        features.forEach { feature -> add("${store}Implementation", project(":features:$feature")) }
    }
}
```

Result: build variants `storeADebug`, `storeBDebug`, `storeARelease`, `storeBRelease`, each
with its own `applicationId` (`com.isharaw.kmpproj.storea` / `.storeb`) and only its
declared feature modules on the classpath.

### Android — feature self-registration (no per-store source sets)

The app shell (`androidApp/src/main/.../App.kt`) is **store-agnostic** — it just renders the tabs
the DI graph collected:
```kotlin
val tabs = graph.tabs.sortedBy { it.order }.filter { /* capability gate */ }
```
Each feature **contributes its own tab** from its `androidMain` via a Metro multibinding:
```kotlin
@ContributesTo(AppScope::class) @BindingContainer
object InvoicesContribution {
    @Provides @IntoSet
    fun invoicesTab(repository: InvoiceRepository, sessionManager: SessionManager): AppTab =
        AppTab("Invoices", "🧾", order = 30) { _, _ -> InvoicesScreen(repository, sessionManager.session!!.experience) }
}
```
The single `AppGraph` (`androidApp/src/main/.../di/AppGraph.kt`) exposes `Set<AppTab>`; Metro
aggregates contributions from whatever feature modules the flavor links. So storeB simply doesn't
link `:features:invoices` → no invoices contribution → no invoices tab, and **storeB literally
cannot reference invoices**. There are **no `androidApp/src/storeX` source sets**. (A feature shown
inside another screen — like `rebate` inside Settings — contributes a generic `Slot` tagged with a
`SlotId` instead of an `AppTab`; the host renders it with `SlotHost(slots, SlotId.X)`.)

### iOS — the `Shared` framework (`shared/build.gradle.kts`)

iOS builds one store at a time. The framework exports exactly the active store's features:

```kotlin
val store = providers.gradleProperty("store").getOrElse(StoreManifest.DEFAULT_STORE)
val storeFeatures = StoreManifest.featuresFor(store)

// framework block:
storeFeatures.forEach { export(project(":features:$it")) }
// iosMain dependencies:
storeFeatures.forEach { api(project(":features:$it")) }
```

`shared`'s own code (`Greeting`, `Platform`) uses no feature module. Android does **not** get
features through `shared` — it links them directly per flavor (above).

### iOS — store selection & feature flags (Xcode)

iOS can't use Gradle flavors, so the store is selected by the **Xcode scheme/configuration**:

- `iosApp/Configuration/Config.xcconfig` (storeA) and `Config-storeB.xcconfig` set:
  - `PRODUCT_BUNDLE_IDENTIFIER` (`…storea` / `…storeb`)
  - `GRADLE_STORE` (`storeA` / `storeB`) — passed to Gradle by the build phase
  - `SWIFT_ACTIVE_COMPILATION_CONDITIONS` — e.g. `STORE_HAS_INVOICES` for storeA, empty for storeB
- The **"Compile Kotlin Framework"** build phase runs:
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode -Pstore="${GRADLE_STORE:-storeA}"`
- The project has build configurations `Debug`/`Release` (storeA) and `StoreB-Debug`/`StoreB-Release`,
  with schemes **iosApp** (storeA) and **iosApp-storeB** (storeB).
- Swift code guards optional features with `#if STORE_HAS_<FEATURE>` (see
  `iosApp/iosApp/ContentView.swift`).

---

## 4. Building & running

### Android

| Variant | Command | Includes |
|---|---|---|
| storeA (full) | `./gradlew :androidApp:assembleStoreADebug` | cart, invoices, settings |
| storeB | `./gradlew :androidApp:assembleStoreBDebug` | cart, settings |

- Install on a device/emulator: `./gradlew :androidApp:installStoreADebug`
- In Android Studio: **Build → Select Build Variant…**, pick `storeADebug` / `storeBDebug`, then ▶ Run.
- APK output: `androidApp/build/outputs/apk/<store>/debug/`.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode, pick the scheme **iosApp** (storeA) or
**iosApp-storeB** (storeB), then build/run. The framework is rebuilt for the right store
automatically via `GRADLE_STORE`.

---

## 5. How to add a NEW FEATURE

Example: adding a `reports` feature.

1. **Create the module** `features/reports/`:
   - `features/reports/build.gradle.kts` — copy an existing feature's build file (e.g.
     `features/settings/build.gradle.kts`, which already has the Compose plugins + deps) and
     change the `namespace` to `com.isharaw.kmpproj.feature.reports`.
   - `features/reports/src/commonMain/.../Reports.kt` — models + repository (plain Kotlin;
     `@Inject @SingleIn(AppScope::class)` for DI).
   - `features/reports/src/androidMain/.../ReportsScreen.kt` — the Compose screen
     (`@Composable fun ReportsScreen(repository: ReportsRepository)`).

2. **Register the module** in `settings.gradle.kts`:
   ```kotlin
   include(":features:reports")
   ```
   *(This stays manual — `buildSrc` isn't available during settings evaluation.)*

3. **Add it to the manifest** (`buildSrc/StoreManifest.kt`) for each store that ships it:
   ```kotlin
   "storeA" to listOf("login", "cart", "invoices", "settings", "reports"),
   ```

4. **Android tab** — the screen already lives in the feature module (step 1). Register its tab
   in the `StoreFeatures.kt` of the stores that ship it (resolving the repo from the graph):
   ```kotlin
   fun storeFeatureTabs(graph: CommonGraph): List<AppTab> = listOf(
       AppTab("Invoices", "🧾") { _, _ -> InvoicesScreen(repository = (graph as AppGraph).invoiceRepository) },
       AppTab("Reports", "📊") { _, _ -> ReportsScreen(repository = (graph as AppGraph).reportsRepository) },
   )
   ```
   Add a `reportsRepository` accessor to the storeA `AppGraph` (or `CommonGraph` if every store
   ships it). *(If the feature is common to every store, add the tab directly in `App.kt`.)*

5. **iOS UI** (only if iOS ships it):
   - Add a SwiftUI view + tab in `iosApp/iosApp/ContentView.swift`, guarded by
     `#if STORE_HAS_REPORTS … #endif`.
   - Add `STORE_HAS_REPORTS` to `SWIFT_ACTIVE_COMPILATION_CONDITIONS` in the xcconfig of each
     store that ships it.

6. **Verify** (see §7).

---

## 6. How to add a NEW STORE

Example: adding `storeC` (cart + settings only).

1. **Add it to the manifest** (`buildSrc/StoreManifest.kt`):
   ```kotlin
   val stores = linkedMapOf(
       "storeA" to listOf("login", "cart", "invoices", "settings"),
       "storeB" to listOf("login", "cart", "settings"),
       "storeC" to listOf("login", "cart", "settings"),
   )
   ```
   That alone gives you the Android flavor `storeCDebug` / `storeCRelease` with applicationId
   `com.isharaw.kmpproj.storec`, linking only the listed features.

2. **Android UI** — create `androidApp/src/storeC/kotlin/com/isharaw/kmpproj/ui/StoreFeatures.kt`:
   ```kotlin
   package com.isharaw.kmpproj.ui
   fun storeFeatureTabs(): List<AppTab> = emptyList()  // or this store's optional tabs
   ```
   *(If storeC ships an optional feature whose UI lives in a source set, also put that screen
   under `src/storeC`.)*

3. **iOS** (only if storeC ships on iOS) — this part is manual:
   - Create `iosApp/Configuration/Config-storeC.xcconfig` (bundle id, `GRADLE_STORE=storeC`,
     the right `STORE_HAS_*` flags).
   - In Xcode, add build configurations `StoreC-Debug` / `StoreC-Release` based on that xcconfig,
     and a scheme **iosApp-storeC** that uses them.

4. **Verify** (see §7).

---

## 7. Verifying a change

```bash
# Android: flavor builds and feature membership
./gradlew :androidApp:assembleStoreADebug
./gradlew :androidApp:assembleStoreBDebug
./gradlew :androidApp:dependencies --configuration storeADebugRuntimeClasspath | grep -c features:invoices  # 1
./gradlew :androidApp:dependencies --configuration storeBDebugRuntimeClasspath | grep -c features:invoices  # 0

# iOS framework: exported feature set follows the manifest per -Pstore
H=shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Headers/Shared.h
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeA
grep -c 'swift_name("InvoiceRepository")' $H   # 1
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeB
grep -c 'swift_name("InvoiceRepository")' $H   # 0
```

For iOS UI, open the project in Xcode and run each scheme — confirm the right tabs appear and
the bundle id matches the store.

---

## 8. Dependency injection (Metro)

Dependencies are wired with [Metro](https://github.com/ZacSweers/metro), a compile-time KMP DI
framework (plugin `dev.zacsweers.metro`, pinned in `gradle/libs.versions.toml`). No reflection,
no KSP — it's a Kotlin compiler plugin applied to `:core`, every `:features:*`, `:shared`, and
`:androidApp`.

- **Scope marker** `AppScope` lives in `:core` (every feature depends on it). Repositories are
  `@Inject` + `@SingleIn(AppScope::class)` (see `features/*/src/commonMain`), so each is a
  singleton for the app's lifetime.
- **One graph per platform** (Metro requires the final `@DependencyGraph` in platform code,
  because features are linked differently per platform):
  - **Android:** `CommonGraph` (common accessors) in `androidApp/src/main/.../di/`; each flavor's
    `AppGraph : CommonGraph` + `createAppGraph()` in `androidApp/src/<store>/.../di/`. storeA's
    graph adds `invoiceRepository`; storeB's does not.
  - **iOS:** `CommonGraph` in `shared/src/iosMain/.../di/`; the concrete `IosAppGraph` +
    `createIosAppGraph()` live in a `-Pstore`-selected source dir
    (`shared/src/iosStoreWithInvoices` vs `src/iosStoreBase`), wired in `shared/build.gradle.kts`.
- **Usage:**
  - Android `App.kt`: `val graph = remember { createAppGraph() }`, then screens get their repo
    (`CartScreen(repository = graph.cartRepository)`); optional features resolved in the flavor's
    `storeFeatureTabs(graph)`.
  - iOS `ContentView.swift`: `let appGraph = IosAppGraphKt.createIosAppGraph()`, then
    `CartView(repository: appGraph.cartRepository)`; the invoices view stays under
    `#if STORE_HAS_INVOICES` and reads `appGraph.invoiceRepository`.
- **Store-aware:** because storeB links no invoices module, its graph has no invoices binding —
  the same build-time exclusion as the rest of the architecture. (Verify: the storeB framework
  header shows `invoiceRepository` 0 times.)

When you **add a feature**, annotate its repository `@Inject @SingleIn(AppScope::class)`, then add
an accessor to the graphs of the stores that ship it (the common graph if every store has it, or
the per-store `AppGraph`/`IosAppGraph` otherwise).

---

## 9. What stays manual (by design)

| Task | Why |
|---|---|
| `include(":features:<name>")` in `settings.gradle.kts` | `buildSrc` isn't loaded during settings evaluation. |
| iOS xcconfig + Xcode build config + scheme per store | Xcode project structure isn't generated from the manifest. |
| Compose / SwiftUI screens for a new feature | UI is platform-native and can't be generated from a feature list. |

A future enhancement could add a Gradle task to generate the iOS `Config-<store>.xcconfig`
files from the manifest.
