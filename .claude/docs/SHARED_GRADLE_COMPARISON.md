# `shared/build.gradle.kts` — Sysco vs. KmpProj

A side-by-side reference comparing the **Sysco** project's `shared` module build file
(pasted for reference) with this repo's **KmpProj** `shared/build.gradle.kts`.
Use it to understand what each project does differently and what we could adopt.

---

## TL;DR of differences

| Area | Sysco | KmpProj (this repo) |
|------|-------|---------------------|
| Plugins | Custom convention plugins (`sysco.*`) + `skie` | Raw catalog plugins (`kotlinMultiplatform`, `androidMultiplatformLibrary`, `metro`) |
| iOS targets | `targets.withType<KotlinNativeTarget>().configureEach` | Explicit `iosArm64()` + `iosSimulatorArm64()` |
| Android config | `android { namespace = ... }` (handled by convention plugin) | Full `androidLibrary { namespace, compileSdk, minSdk, compilerOptions, withHostTest }` |
| Project refs | Type-safe accessors (`projects.featureLoginApi`) | String paths (`project(":features:login:api")`) |
| Swift bridge | **SKIE** plugin + `binaryOption("bundleId", ...)` | none |
| Module layout | Flat names (`core-user-api`, `feature-login-real`) | Nested (`:features:login:api`, `:core`) |
| Store features | `export` list is **hardcoded**; loop commented out | `export` driven by `storeFeatures` loop (line 24) |
| Dependency split | Rich `:api`/`:real` separation across many core modules | Single `:core` module + per-feature `:api`/`:real` |

---

## 1. Plugins

**Sysco**
```kotlin
plugins {
  alias(libs.plugins.sysco.kotlinMultiplatform)   // convention plugin
  alias(libs.plugins.sysco.androidMultiplatform)  // convention plugin
  alias(libs.plugins.sysco.metro)                 // convention plugin
  alias(libs.plugins.skie)                         // Swift interop sugar
}
```
- `sysco.*` are **convention plugins** (defined in their `build-logic`/`buildSrc`). They wrap
  the boilerplate (Android namespace defaults, compileSdk, JVM target, common test deps, etc.)
  so the module file stays short.
- **SKIE** (`co.touchlab.skie`) post-processes the generated Obj-C/Swift header to expose
  Kotlin enums with associated values, sealed classes, default args, flows, etc. as idiomatic Swift.

**KmpProj**
```kotlin
plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.metro)
}
```
- Uses the raw plugins directly — all Android/iOS config is inline in this file (no convention layer).
- **No SKIE** → Swift consumers see the raw generated Obj-C interop (less idiomatic).

---

## 2. iOS target declaration

**Sysco** (generic, configures every native target):
```kotlin
targets.withType<KotlinNativeTarget>().configureEach {
  binaries.framework {
    baseName = "Shared"
    isStatic = true
    binaryOption("bundleId", "com.sysco.shared")
    ...
  }
}
```

**KmpProj** (explicit target list):
```kotlin
listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
  iosTarget.binaries.framework {
    baseName = "Shared"
    isStatic = true
    ...
  }
}
```
- Sysco's `withType<KotlinNativeTarget>().configureEach` applies to **whatever** native targets
  the convention plugin registered — more flexible, but the targets themselves are declared elsewhere.
- KmpProj declares the two iOS targets right here. Functionally equivalent for iOS-only setups.
- Sysco adds `binaryOption("bundleId", ...)` (sets the framework's CFBundleIdentifier) — KmpProj does not.

---

## 3. Project references: type-safe accessors vs. string paths

**Sysco**: `export(projects.featureLoginApi)`, `api(projects.coreUserReal)` …
**KmpProj**: `project(":features:login:api")` …

Both projects have `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` available, but:
- Sysco consistently uses the `projects.*` accessors (compile-checked, IDE auto-complete, refactor-safe).
- KmpProj uses string paths in the `storeFeatures.forEach` loop because the path is **built
  dynamically** (`":features:$it:api"`) — type-safe accessors can't be used with a runtime string.
  This is the key reason the loop form stays on `project("...")` rather than `projects.*`.

> Note the bad import in the Sysco snippet: `import org.jetbrains.compose.reload.gradle.project`
> shadows Gradle's built-in `project(path)` accessor. It should be removed.

---

## 4. Module granularity

**Sysco** splits cross-cutting concerns into many `:api` / `:real` core modules:
`coreAnalyticsShop`, `coreAnalyticsCommon`, `coreFeatureFlagCommon`, `coreFeatureFlagShop`,
`coreUser`, `coreConfig`, `coreLogger`, `coreMonitoring`, `coreDatabase`, `coreNetwork`,
`coreAuth`, plus `interactorLogout`, and Android-only `featureHome`, `featureRoot`, `coreFeature`, `coreUi`.

**KmpProj** currently has a single `:core` module plus per-feature `:api`/`:real` pairs
(`login`, `cart`, `invoices`, `settings`, `orders`, `rebate`) and `:features:test`.

**Takeaway:** Sysco demonstrates the mature end-state of the same architecture KmpProj is using —
strict `:api` (contracts, exported) vs `:real` (impls, never exported) separation, applied to
*core* concerns too, not just features.

---

## 5. The store-features pattern (most relevant to KmpProj)

Both projects share the **same idea**: a `StoreManifest` decides which features a store ships,
and the iOS framework exports only the matching `:api` contracts.

**Sysco** — currently **hardcoded** the exports and left the loop commented:
```kotlin
export(projects.featureLoginApi)
export(projects.featureMoreApi)
export(projects.featureRebateApi)
//  storeFeatures.forEach {
//    export( project("feature:$it:api") )   // wrong path: missing leading ':' and plural 'features'
//  }
```

**KmpProj** — already uses the dynamic loop (working):
```kotlin
storeFeatures.forEach { export(project(":features:$it:api")) }
```

⚠️ Two correctness notes carried over from earlier analysis:
1. `export(...)` requires the same module to also be an `api(...)` dependency of `iosMain`,
   otherwise the build fails with *"dependencies exported in the framework are not specified
   as API-dependencies"*. In KmpProj that `iosMain` block is still commented (lines 45–48).
2. Sysco's commented path `"feature:$it:api"` is wrong (should be `":features:$it:api"`).

---

## 6. Source-set dependency rules

**Sysco** documents intent inline:
- `commonMain` → only `:real` impls (`api` for re-exported, `implementation` for internal-only
  like `coreDatabaseReal`, `coreNetworkReal`, `coreAuthReal`, `coreLoggerReal`, `interactorLogoutReal`).
- `androidMain` → Android-only features (`featureHomeReal`, `featureRootReal`, `coreUi`, `metro.android`).

**KmpProj**:
- `iosMain` → `:core` only (feature wiring still commented).
- `commonTest` → `kotlin.test`.
- Android features are pulled in by the **app per product flavor**, so `shared`'s Android side
  stays store-agnostic (documented in the comments at lines 40–42).

This is a genuine architectural divergence: **Sysco links Android features inside `shared`;
KmpProj links them in the app per flavor.**

---

## What KmpProj could adopt from Sysco

1. **Convention plugins** to shrink `shared/build.gradle.kts` and share Android/iOS config.
2. **SKIE** for idiomatic Swift interop if iOS consumption grows.
3. **`binaryOption("bundleId", ...)`** on the framework.
4. **Type-safe `projects.*` accessors** for the static (non-loop) dependencies.
5. Finishing the **`iosMain` api/implementation loop** so the export loop actually compiles.

---

## 7. Fixing the Sysco file to use the loop (reference)

Converting Sysco's hardcoded feature exports to the store-driven loop requires **three** edits.
The export loop alone will not compile.

### Edit 1 — remove the shadowing import
```kotlin
// ❌ delete this — it shadows Gradle's built-in project(path) accessor:
// import org.jetbrains.compose.reload.gradle.project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
```

### Edit 2 — loop the feature exports (keep core exports hardcoded)
```kotlin
binaries.framework {
  baseName = "Shared"
  isStatic = true
  binaryOption("bundleId", "com.sysco.shared")

  // Core contracts are always exported (not store-specific) — keep hardcoded.
  export(projects.coreAnalyticsShopApi)
  export(projects.coreAnalyticsCommonApi)
  export(projects.coreFeatureFlagCommonApi)
  export(projects.coreUserApi)
  export(projects.coreConfigApi)
  export(projects.coreLoggerApi)
  export(projects.coreMonitoringApi)

  // Feature contracts are store-driven — replaces the hardcoded login/more/rebate lines.
  storeFeatures.forEach { export(project(":feature:$it:api")) }
}
```

### Edit 3 — add the matching api() loop in commonMain
```kotlin
commonMain.dependencies {
  api(projects.coreAnalyticsCommonReal)
  // ... other core :real modules ...
  api(projects.coreMonitoringReal)

  // Each exported :api must also be an api() dep of the iOS compilation, or the build fails
  // with "dependencies exported in the framework are not specified as API-dependencies".
  // The :real impl re-exposes its :api via `api`, so adding :real satisfies the export.
  storeFeatures.forEach { api(project(":feature:$it:real")) }

  implementation(projects.coreDatabaseReal)
  implementation(projects.coreNetworkReal)
  implementation(projects.coreAuthReal)
  implementation(projects.coreLoggerReal)
  implementation(projects.interactorLogoutReal)
}
```

### Two things to verify before relying on this
1. **Path segment** — Sysco's accessor `projects.featureLoginApi` maps to `:feature:login:api`
   (**singular** `feature`). Confirm against `settings.gradle.kts`; if `include(...)` uses
   `:features:`, switch the loop to `":features:$it:api"` / `":features:$it:real"`.
2. **Feature names** — `StoreManifest.getFeaturesForStore(store)` must return the same names
   (`login`, `more`, `rebate`, …) used in the module paths. If a feature like `more` ships in
   **every** store, keep its `export` hardcoded and only loop the truly store-variant features.

---

## 8. How to add a new feature (so Gradle doesn't ignore / fail it)

> Context from the build errors we hit: the Sysco repo flattens Gradle paths with **dashes**
> (the wiring module is `:shared-wiring`, not `:shared:wiring`), so feature modules are
> `:feature-<name>-api` / `:feature-<name>-real`, **not** `:feature:<name>:api`.
> The dynamic loop only works when every link below uses the *exact same* path format.

### The 3 places a feature must be registered

A feature is "ignored" / fails to resolve when one of these is missing or uses the wrong format.
Adding feature **`rebate`** is the worked example:

**1. Create the module folders + build files**
```
feature/rebate/api/build.gradle.kts     // contracts (interfaces, models) — exported to Swift
feature/rebate/real/build.gradle.kts    // implementations — linked, never exported
```

**2. Register both modules in `settings.gradle.kts`** — match the repo's dash format exactly:
```kotlin
include(":feature-rebate-api")
include(":feature-rebate-real")
// If settings.gradle.kts maps folders → dash paths via a helper, just add the folder and
// let the helper register it. Verify the resulting path with: ./gradlew projects
```

**3. Add the bare feature name to `StoreManifest`** for every store that ships it:
```kotlin
val stores = mapOf(
  "storeB" to listOf("login", "more", "rebate"),  // ← add "rebate" (bare name, no "projects." prefix)
)
```

### What you do NOT need to touch

Because the framework + `commonMain` use the loop, `shared/wiring/build.gradle.kts` needs **no edit**:
```kotlin
storeFeatures.forEach { export(project(":feature-$it-api")) }   // auto-exports rebate
storeFeatures.forEach { api(project(":feature-$it-real")) }     // auto-links rebate impl
```
Adding the bare name in `StoreManifest` is enough — the loop picks it up.

### The format rule (the cause of every error we hit)

`$it` is a **bare folder name** (`rebate`). The loop wraps it into a full path. All three of these
must agree, or Gradle reports *"Project with path ':feature-…' could not be found"*:

| Place | Must produce |
|-------|--------------|
| `settings.gradle.kts` include | `:feature-rebate-api`, `:feature-rebate-real` |
| Loop string in build file | `":feature-$it-api"`, `":feature-$it-real"` |
| `StoreManifest` list entry | `"rebate"` (bare — **not** `"projects.featureRebateApi"`) |

### Common mistakes → error mapping (from this thread)

| Symptom | Cause | Fix |
|---------|-------|-----|
| `':feature:projects.featureRebateApi:api' could not be found` | `StoreManifest` stored the accessor string | Store the **bare name** `"rebate"` |
| `':feature:rebate:api' could not be found` | Wrong path separator (colon vs dash) | Use `":feature-$it-api"` to match `:shared-wiring` style |
| `dependencies exported in the framework are not specified as API-dependencies` | Exported `:api` but didn't `api()` it in a source set | Add the `commonMain` `api(project(":feature-$it-real"))` loop |
| Module exported twice / duplicate export error | Hardcoded `export(...)` **and** the loop both cover it | Remove the hardcoded line, keep only the loop |

### Verify after adding
```bash
./gradlew projects                         # confirm :feature-rebate-api shows up
./gradlew :shared-wiring:help              # confirm config phase resolves the loop
./gradlew :shared-wiring:assemble -Pstore=storeB   # build the store that ships rebate
```
