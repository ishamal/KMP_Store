# iOS store flavors — compiling only the features each store needs (a beginner's guide)

> **Read first:** [`ARCHITECTURE.md`](ARCHITECTURE.md) explains stores, features and DI on Android.
> This doc is the **iOS** counterpart: how one `iosApp` builds as several stores (storeA / storeB /
> storeC), each containing **only its own features** — and how we made the iOS side *feature-driven*
> so any store composition just works.

---

## 1. The goal, in one paragraph

On Android, a "store" is a **product flavor**: picking `storeBDebug` automatically links only storeB's
feature modules, and the UI self-assembles from whatever features are present. We want the **same on
iOS**: a `storeC` build of the iPhone app should contain *only* storeC's features (login, settings,
orders) — no cart code, no invoices code — and show only those screens. This is good for binary size,
clean separation, and not shipping code a store shouldn't have.

The tricky part: **Xcode has no "product flavor" concept.** So we assemble the same result from a few
smaller pieces. This doc walks through each one.

---

## 2. The mental model: two halves

A store flavor on iOS is **two cooperating halves**:

| Half | Decides | Lives in |
|---|---|---|
| **Kotlin half** | which feature *logic* is compiled into the `Shared` framework | `shared/build.gradle.kts` (Gradle) |
| **Swift half** | which feature *screens* are compiled into the app | `ContentView.swift` + Xcode config |

The bridge between them is a single string: the **store name** (`storeA` / `storeB` / `storeC`). Xcode
holds it in a variable called `GRADLE_STORE`; everything else is derived from it.

```
Xcode build config (e.g. "StoreC-Debug")
   └─ xcconfig sets GRADLE_STORE = storeC
        ├─► Kotlin half:  ./gradlew … -Pstore=storeC   → framework with only storeC's feature logic
        └─► Swift half:   STORE_HAS_ORDERS flag         → compiles only the Orders screen
```

---

## 3. The Kotlin half — only compile needed feature *logic*

### a) `-Pstore` already filters features

This part existed before and is the heart of it. `shared/build.gradle.kts` reads a Gradle property
`-Pstore` and links only that store's feature modules:

```kotlin
val store = providers.gradleProperty("store").getOrElse(StoreManifest.SELECTED_STORE)
val storeFeatures = StoreManifest.featuresFor(store)   // e.g. storeC → [login, settings, orders]
…
storeFeatures.forEach {
    api(project(":features:$it:api"))             // the contract (exported to Swift)
    implementation(project(":features:$it:real")) // the implementation (linked, hidden)
}
```

So storeC's `Shared` framework literally has no `cart` or `invoices` code. **That alone is "compile
only needed features."** The Xcode build phase passes the store in:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode -Pstore="${GRADLE_STORE:-storeA}"
```

### b) The problem we fixed: the DI graph assumed a fixed feature set

Swift can't call Metro's `createGraph()` directly (it's a compile-time trick), so the framework exposes
a small **`IosAppGraph`** with accessors like `cartRepository`, `invoiceRepository`. Originally this graph
was **hand-written** and assumed *every* store ships login + cart + settings. storeC has **no cart**, so
its graph couldn't compile:

```
CommonGraph.kt: Unresolved reference 'CartRepository'
```

Hand-maintaining a graph per store doesn't scale (with N optional features you'd need 2^N variants).

### c) The fix: **generate** the graph from the store's features

We now generate `IosAppGraph.kt` at build time, including an accessor **only** for each feature the
store ships. In `shared/build.gradle.kts`:

```kotlin
// feature -> (repository type, accessor name) — only shipped features get an accessor
val iosGraphAccessors = linkedMapOf(
    "login"    to ("…feature.login.LoginValidator"      to "loginValidator"),
    "cart"     to ("…feature.cart.CartRepository"       to "cartRepository"),
    "settings" to ("…feature.settings.SettingsRepository" to "settingsRepository"),
    "invoices" to ("…feature.invoices.InvoiceRepository"  to "invoiceRepository"),
    "orders"   to ("…feature.orders.OrderRepository"      to "orderRepository"),
)

val generateIosAppGraph by tasks.registering {
    val members = iosGraphAccessors.filterKeys { it in storeFeatures }.values.toList()
    inputs.property("members", members.map { "${it.first}:${it.second}" })  // re-run when store changes
    outputs.dir(generatedIosGraphDir)
    doLast { /* write interface IosAppGraph { … } with just those accessors */ }
}

// Make compilation depend on the generator by adding its output as a source folder:
iosMain { kotlin.srcDir(generateIosAppGraph) }
```

For storeC this generates exactly:

```kotlin
@DependencyGraph(AppScope::class)
interface IosAppGraph {
    val loginValidator: LoginValidator
    val settingsRepository: SettingsRepository
    val orderRepository: OrderRepository      // ← no cartRepository, because storeC has no cart
}
fun createIosAppGraph(): IosAppGraph = createGraph<IosAppGraph>()
```

> **Why this is the "feature-driven" win:** the graph now always matches the build. Adding a feature to
> the iOS app = one line in `iosGraphAccessors`; every store that ships it gets the accessor, every
> store that doesn't simply won't have it. This mirrors how Android self-assembles its tabs.

The old hand-written files (`iosStoreBase/`, `iosStoreWithInvoices/`, `CommonGraph.kt`) were deleted.

---

## 4. The Swift half — only compile needed *screens*

The Kotlin graph for storeC has no `cartRepository`, so Swift must not reference one. We gate each
feature's UI with a **compilation flag** `STORE_HAS_<FEATURE>` using Swift's `#if`:

```swift
TabView {
    #if STORE_HAS_CART
    CartView(repository: appGraph.cartRepository)
        .tabItem { Label("Cart", systemImage: "cart") }
    #endif
    #if STORE_HAS_ORDERS
    OrdersView(repository: appGraph.orderRepository)
        .tabItem { Label("Orders", systemImage: "shippingbox") }
    #endif
    #if STORE_HAS_INVOICES
    InvoicesView(repository: appGraph.invoiceRepository)
        .tabItem { Label("Invoices", systemImage: "doc.text") }
    #endif
    SettingsView(repository: appGraph.settingsRepository, …)   // login + settings are in every store
        .tabItem { Label("Settings", systemImage: "gear") }
}
```

> **Beginner gotcha we hit:** it's not enough to gate where a view is *used*. The **view struct itself**
> (`struct CartView { let repository: CartRepository … }`) names `CartRepository`, which doesn't exist
> in storeC's framework. So the *whole struct* must be inside `#if STORE_HAS_CART … #endif`. If you see
> "cannot find type X in scope," a definition is leaking outside its `#if`.

`#if` flags compile the code out entirely (zero bytes shipped), exactly like a missing Android module.

---

## 5. The Xcode plumbing — where the store name is set

Xcode is told which store to build via three artifacts **per store**. (This is the manual part that
Android generates automatically.)

### a) An `.xcconfig` — the per-store settings file
`iosApp/Configuration/Config-storeC.xcconfig`:
```
PRODUCT_BUNDLE_IDENTIFIER = com.isharaw.kmpproj.storec   // its own app id
GRADLE_STORE = storeC                                    // → passed to Gradle as -Pstore
SWIFT_ACTIVE_COMPILATION_CONDITIONS = STORE_HAS_ORDERS   // → the Swift #if flags for this store
```
- `GRADLE_STORE` drives the **Kotlin half** (which features compile into the framework).
- `SWIFT_ACTIVE_COMPILATION_CONDITIONS` drives the **Swift half** (which screens compile). storeA gets
  `STORE_HAS_CART STORE_HAS_INVOICES STORE_HAS_ORDERS`; storeB gets `STORE_HAS_CART`; storeC gets
  `STORE_HAS_ORDERS`. (login + settings are universal, so they need no flag.)

### b) Build configurations — in `project.pbxproj`
Xcode's project file lists **build configurations**. The default ones are `Debug` / `Release` (storeA).
We add `StoreC-Debug` / `StoreC-Release`, each pointing at `Config-storeC.xcconfig`. They're registered
in two places (the project's list and the app target's list). This is fiddly to hand-edit; normally you
do it in Xcode's UI (**Project ▸ Info ▸ Configurations ▸ +**), but it can be done carefully by following
the existing storeB blocks as a template, then validating with `plutil -lint project.pbxproj`.

### c) A scheme — what the Run button uses
`iosApp-storeC.xcscheme` ties the Run/Test/Archive actions to the `StoreC-Debug`/`StoreC-Release`
configurations. Schemes appear in the dropdown next to Xcode's Run button.

---

## 6. The one non-obvious bug: `KOTLIN_FRAMEWORK_BUILD_TYPE`

The Kotlin Multiplatform plugin figures out whether to build a **debug** or **release** framework by
reading Xcode's `CONFIGURATION` name — but it expects it to be **exactly** `Debug` or `Release`. A custom
name like `StoreC-Debug` isn't recognised, and you get:

```
Unable to detect Kotlin framework build type for CONFIGURATION=StoreC-Debug …
```

Fix: tell it explicitly in each custom build configuration's settings:
```
KOTLIN_FRAMEWORK_BUILD_TYPE = debug     // in StoreC-Debug
KOTLIN_FRAMEWORK_BUILD_TYPE = release   // in StoreC-Release
```
(The stock `Debug`/`Release` configs don't need this; any custom-named one does — storeB needed it too.)

---

## 7. How to add a NEW iOS store flavor (checklist)

Say you add `storeD` (already a row in `config/stores/storeD.properties`):

1. **Kotlin half** — nothing to do if storeD's features are already in `iosGraphAccessors`. If storeD
   ships a *new* feature whose screen you want on iOS, add one line to `iosGraphAccessors` in
   `shared/build.gradle.kts` and (if it needs a Swift screen) a `#if STORE_HAS_<FEATURE>` block.
2. **xcconfig** — copy `Config-storeC.xcconfig` → `Config-storeD.xcconfig`; set `GRADLE_STORE=storeD`,
   the bundle id, and the `SWIFT_ACTIVE_COMPILATION_CONDITIONS` flags for storeD's features.
3. **Build configurations** — in Xcode: **Project ▸ Info ▸ Configurations ▸ +** twice, making
   `StoreD-Debug` (base config = `Config-storeD.xcconfig`) and `StoreD-Release`. Add
   `KOTLIN_FRAMEWORK_BUILD_TYPE = debug` / `release` to each.
4. **Scheme** — duplicate the `iosApp-storeC` scheme as `iosApp-storeD`, pointing its actions at the
   `StoreD-*` configurations. Mark it **Shared** so it's committed.
5. **Run** — pick the `iosApp-storeD` scheme and ▶. Only storeD's features compile, on both halves.

---

## 8. Build & verify from the command line

```bash
# Kotlin half only (fast sanity — compiles the generated graph for a store):
./gradlew :shared:compileKotlinIosSimulatorArm64 -Pstore=storeC

# Whole app (both halves) for a store, simulator, no signing:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp-storeC \
  -destination 'generic/platform=iOS Simulator' -configuration StoreC-Debug \
  CODE_SIGNING_ALLOWED=NO build

# Sanity-check the project file after hand-edits:
plutil -lint iosApp/iosApp.xcodeproj/project.pbxproj
```

All three stores (storeA: cart+invoices+orders, storeB: cart, storeC: orders) build green, each
containing only its own features.

---

## 9. Recap

- **Compiling only needed features on iOS has two halves** that share one value (the store name):
  the Kotlin framework (`-Pstore` → `storeFeatures`) and the Swift app (`STORE_HAS_*` `#if` flags).
- We made the iOS side **feature-driven** by *generating* `IosAppGraph` from the store's feature list,
  so it always matches the build — no hand-written per-store graphs, no "every store has cart"
  assumption. Add a feature in one map; stores that ship it get it, stores that don't won't reference it.
- The **Xcode plumbing** (xcconfig + build configurations + scheme) is the manual equivalent of an
  Android product flavor; `KOTLIN_FRAMEWORK_BUILD_TYPE` must be set for any custom-named configuration.
