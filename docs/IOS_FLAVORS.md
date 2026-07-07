# iOS Flavors (Stores) — A Beginner's Guide

How this project ships **one app per store** (storeA / storeB / storeC) on iOS, and how to **add a
new store**. Written for someone who has never touched the iOS build before.

Companion docs: [`STORES_AND_FEATURES.md`](STORES_AND_FEATURES.md) (the store/feature architecture)
and [`PRODUCT_AND_APPID.md`](PRODUCT_AND_APPID.md) (application IDs).

---

## 1. What is a "store" (and why it's called a "flavor")

A **store** is one branded build of the same codebase — think "the KEELS app", "the CARGILLS app",
"the GLOMARK app". Each store ships a **different set of features** and its own app icon / bundle id,
but they all come from this one repository.

On **Android** this is a *product flavor* (you pick it in Android Studio's **Build Variants** panel).
iOS has no "flavors" feature, so we recreate the same idea with standard Xcode building blocks. That's
what this doc explains.

**The golden rule — one source of truth:** every store is defined by a single file:

```
config/stores/storeA.properties
config/stores/storeB.properties
config/stores/storeC.properties
```

Example (`config/stores/storeC.properties`):

```properties
storeName=storeC
features=login,cart,settings,orders
businessUnitDefaults=KEELS:USBL
```

| Key | Meaning |
|---|---|
| `storeName` | the store id; also the app id suffix → `com.isharaw.kmpproj.storec` |
| `features` | which feature **modules** get compiled into this store's app |
| `businessUnitDefaults` | runtime default business unit per experience (used after login) |

`buildSrc/StoreManifest.kt` reads these `.properties` files. **Both** the Android build and the iOS
framework build ask `StoreManifest` "what features does store X have?" — so the two platforms never
disagree.

---

## 2. How a store is selected on iOS (the chain)

There is no BuildConfig on iOS. Instead, selecting a store flows through four standard Xcode/Gradle
pieces:

```
 You pick an Xcode SCHEME            e.g. "iosApp-storeC"
        │
        ▼
 Scheme points at a BUILD CONFIGURATION   e.g. "StoreC-Debug"
        │
        ▼
 Build config includes an XCCONFIG file   iosApp/Configuration/Config-storeC.xcconfig
        │  which sets:  GRADLE_STORE = storeC
        │               PRODUCT_BUNDLE_IDENTIFIER = com.isharaw.kmpproj.storec
        │               SWIFT_ACTIVE_COMPILATION_CONDITIONS = (STORE_HAS_* flags)
        ▼
 A "Compile Kotlin Framework" build phase runs:
        ./gradlew :shared:embedAndSignAppleFrameworkForXcode -Pstore="${GRADLE_STORE:-storeA}"
        │
        ▼
 shared/build.gradle.kts:  StoreManifest.featuresFor(rootDir, "storeC")  →  [login, cart, settings, orders]
        │  → only those feature :api/:real modules are linked into Shared.framework
        ▼
 Swift code:  #if STORE_HAS_INVOICES … #endif   (compiled in only when the flag is set)
```

**In plain words:** picking the scheme sets `GRADLE_STORE`, which is passed to Gradle as `-Pstore`,
which tells the shared framework which feature modules to include. The `STORE_HAS_*` flags let Swift
UI code turn store-specific screens on/off.

### What exists today

| Store | Xcode scheme to pick | Build configs | xcconfig | Ships |
|---|---|---|---|---|
| storeA | **iosApp** (the default) | `Debug` / `Release` | `Config.xcconfig` | login, cart, invoices, settings, orders, rebate, passwordReset |
| storeB | **iosApp-storeB** | `StoreB-Debug` / `StoreB-Release` | `Config-storeB.xcconfig` | login, cart, settings, rebate |
| storeC | **iosApp-storeC** | `StoreC-Debug` / `StoreC-Release` | `Config-storeC.xcconfig` | login, cart, settings, orders |

---

## 3. Building / running a store on iOS

### In Xcode (the normal way)

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. In the toolbar (top-left, next to the Run ▶ button) click the **scheme selector**.
3. Choose **`iosApp-storeB`** (or `iosApp-storeC`, or `iosApp` for storeA).
4. Pick a simulator, press **Run ▶**. Xcode runs Gradle to build the framework for that store, then
   builds and launches the app.

That scheme selector is the iOS equivalent of Android Studio's *Build Variants* panel.

### From the terminal (just the shared framework)

You don't need Xcode to check that a store's Kotlin side compiles:

```bash
# Build the iOS framework for a specific store:
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeC

# List every scheme + build config Xcode can see:
xcodebuild -list -project iosApp/iosApp.xcodeproj
```

---

## 4. Add a NEW store — end to end

Worked example: add **`storeD`** shipping `login, cart, settings` for the KEELS brand.

> ⚠️ **iOS requirement first:** the shared iOS graph
> (`shared/src/iosMain/.../di/CommonGraph.kt`) currently assumes **every store ships `login`, `cart`,
> and `settings`** (it exposes `loginValidator`, `cartRepository`, `settingsRepository`). If your
> store's `features` omit any of those, the iOS framework will fail to compile
> (`Unresolved reference: cart`). So either include those three, or first make them optional (see
> [§6](#6-the-login--cart--settings-rule-on-ios)).

### Step 1 — Define the store (drives BOTH platforms)

Create `config/stores/storeD.properties`:

```properties
# Store D config — read at build time by buildSrc/StoreManifest.kt
storeName=storeD
features=login,cart,settings
businessUnitDefaults=KEELS:USBL
```

That's the only change the Android **flavor** needs — `androidApp/build.gradle.kts` auto-creates a
`storeD` flavor from `StoreManifest.stores()`, with app id `com.isharaw.kmpproj.stored`, linking
exactly those feature modules.

### Step 2 — Android per-store source set (required)

Each store pins its default brand in its own source set (this is **not** auto-generated):

- `androidApp/src/storeD/kotlin/com/isharaw/kmpproj/branding/FlavorDefaults.kt`:
  ```kotlin
  package com.isharaw.kmpproj.branding

  import com.isharaw.kmpproj.core.Experience

  /** storeD flavor defaults — the brand shown before login. */
  object FlavorDefaults {
      val defaultExperience = Experience.KEELS
  }
  ```
- `androidApp/src/storeD/res/values/strings.xml` — the launcher label:
  ```xml
  <resources><string name="app_name">Store D</string></resources>
  ```

> Without `FlavorDefaults.kt`, the `storeD` flavor won't compile (`App.kt` reads
> `FlavorDefaults.defaultExperience`, and it lives only in the flavor source sets, not `src/main`).

At this point storeD works on **Android**. The rest is iOS.

> 🅰️ **Shortcut for Steps 3 & 5:** run
> ```bash
> ./gradlew generateIosStore -Pstore=storeD
> ```
> and it writes the xcconfig (Step 3) **and** the scheme (Step 5) for you from the `.properties`
> file — leaving only Step 4 (the pbxproj build configs) to do by hand in Xcode. The manual steps
> below explain what that task produces.

### Step 3 — iOS xcconfig

Either run the shortcut above, or copy `iosApp/Configuration/Config-storeC.xcconfig` →
`Config-storeD.xcconfig` and edit:

```
PRODUCT_BUNDLE_IDENTIFIER=com.isharaw.kmpproj.stored
GRADLE_STORE=storeD
// Set STORE_HAS_* flags ONLY for features this store ships that have #if guards in Swift.
// storeD has no invoices, so leave it empty:
SWIFT_ACTIVE_COMPILATION_CONDITIONS=
```

(If storeD shipped invoices, this would be `SWIFT_ACTIVE_COMPILATION_CONDITIONS=STORE_HAS_INVOICES`,
like storeA.)

### Step 4 — iOS build configurations (the fiddly part)

You need two build configs, `StoreD-Debug` and `StoreD-Release`. **Easiest (recommended for
beginners): use the Xcode UI** — it edits the project file safely for you:

1. Open `iosApp.xcodeproj`. In the Project navigator click the blue **iosApp** project at the top.
2. Select the **PROJECT** "iosApp" (not the target) → **Info** tab → **Configurations**.
3. Click **+** → **Duplicate "StoreC-Debug" Configuration** → rename it **`StoreD-Debug`**.
4. Repeat: duplicate `StoreC-Release` → **`StoreD-Release`**.
5. For each new config, set its config file to **`Config-storeD`** (the dropdown by the config name).

<details>
<summary>What that does under the hood (for the curious / if you edit the file by hand)</summary>

In `iosApp/iosApp.xcodeproj/project.pbxproj` it adds **four** `XCBuildConfiguration` objects — a
Debug + Release pair for the *project* level and another pair for the *iosApp target* level — each
with `baseConfigurationReferenceRelativePath = "Config-storeD.xcconfig"`, and lists them in the two
`XCConfigurationList` `buildConfigurations` arrays. The existing storeC entries use readable ids
(`…C001`–`…C004`); mirror them as `…D001`–`…D004`.
</details>

### Step 5 — iOS scheme (so it's selectable)

Copy the storeB/storeC scheme and repoint it:

- Copy `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp-storeC.xcscheme` →
  `iosApp-storeD.xcscheme`, then replace every `StoreC-Debug`/`StoreC-Release` with
  `StoreD-Debug`/`StoreD-Release`. (Leave the `BlueprintIdentifier` — it's the shared iosApp target.)

Or in Xcode: **Product → Scheme → Manage Schemes → duplicate `iosApp-storeC` → rename
`iosApp-storeD` → Edit Scheme →** set every action's build config to the `StoreD-*` ones, and tick
**Shared**.

### Step 6 — Verify

```bash
plutil -lint iosApp/iosApp.xcodeproj/project.pbxproj          # project file still valid
xcodebuild -list -project iosApp/iosApp.xcodeproj             # shows iosApp-storeD + StoreD-Debug/Release
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeD   # framework compiles
./gradlew :androidApp:compileStoreDDebugKotlin               # Android side compiles
```

Then open Xcode, pick the **iosApp-storeD** scheme, and Run.

---

## 5. Quick checklist

Adding a store touches these files:

| # | File | Platform | Auto or manual |
|---|---|---|---|
| 1 | `config/stores/<store>.properties` | both | **manual** (the source of truth) |
| 2 | `androidApp/src/<store>/kotlin/.../branding/FlavorDefaults.kt` | Android | **manual** |
| 3 | `androidApp/src/<store>/res/values/strings.xml` | Android | **manual** |
| — | the Android product flavor | Android | *auto* (from StoreManifest) |
| — | which feature modules link into the framework | iOS | *auto* (from `-Pstore`) |
| 4 | `iosApp/Configuration/Config-<store>.xcconfig` | iOS | *auto* (`./gradlew generateIosStore`) |
| 5 | `StoreX-Debug` / `StoreX-Release` build configs (pbxproj) | iOS | **manual** (Xcode UI) |
| 6 | `iosApp-<store>.xcscheme` | iOS | *auto* (`./gradlew generateIosStore`) |

---

## 6. The "login + cart + settings" rule on iOS

`shared/src/iosMain/.../di/CommonGraph.kt` hardcodes accessors for the features it assumes *every*
store ships:

```kotlin
interface CommonGraph {
    val loginValidator: LoginValidator      // feature: login
    val cartRepository: CartRepository       // feature: cart
    val settingsRepository: SettingsRepository // feature: settings
}
```

If a new store's `features` omit any of these, `Shared.framework` won't compile for that store. Two
options:

1. **Include them** in the store's `features` (simplest — what storeC does).
2. **Make one optional** (proper fix for a store that genuinely lacks it): move that accessor out of
   `CommonGraph` into a store-aware source set (there's a sketch for exactly this, for invoices, in
   the commented-out block of `shared/build.gradle.kts`), and guard the Swift usage with a
   `#if STORE_HAS_<FEATURE>` flag in `ContentView.swift` + the xcconfigs that ship it.

---

## 7. Known limitation — scalar config isn't on iOS yet

On Android the scalar `.properties` values reach the app via `BuildConfig`
(`BUSINESS_UNIT_DEFAULTS`) and the per-flavor `FlavorDefaults` (default experience). **iOS has no
equivalent yet** — selecting a store changes *which feature modules* compile in, but
`businessUnitDefaults` and the default experience are not exposed to Swift.

The clean fix (not yet implemented) is to generate a small Kotlin `StoreConfig` object in `shared`
from `StoreManifest` at build time, so Swift can read `StoreConfig.BUSINESS_UNIT_DEFAULTS` etc. — the
same single source of truth as Android. Ask before relying on those values on iOS.

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Unresolved reference: cart` (or login/settings) building the framework | store's `features` omit a feature `CommonGraph` assumes | add it to `features`, or make it optional (§6) |
| iOS store builds the wrong feature set | scheme → build config → xcconfig `GRADLE_STORE` mismatch, or missing xcconfig | check `Config-<store>.xcconfig` has `GRADLE_STORE=<store>` |
| New store's scheme not in the dropdown | scheme file missing or not marked *Shared* | add `iosApp-<store>.xcscheme` under `xcshareddata/xcschemes/` |
| A `#if STORE_HAS_X` screen is missing/extra | `SWIFT_ACTIVE_COMPILATION_CONDITIONS` in the xcconfig | add/remove the `STORE_HAS_X` flag |
| `xcodebuild -list` doesn't show the new build configs | pbxproj edit incomplete | duplicate configs via Xcode UI (§Step 4), or check all 4 objects + both config lists |
| Android `storeX` flavor won't compile | missing per-store source set | add `androidApp/src/<store>/.../FlavorDefaults.kt` |
