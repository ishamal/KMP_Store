# Adding a New Store or Feature — Step by Step

A practical companion to [`STORES_AND_FEATURES.md`](STORES_AND_FEATURES.md) (architecture) and
[`EXPERIENCES.md`](EXPERIENCES.md) (runtime user experiences). This is the **how-to**.

All packages are `com.isharaw.kmpproj.*`.

---

## Mental model (what decides what a build shows)

1. **The manifest** `buildSrc/StoreManifest.kt` — which feature **modules** each store links
   (build-time; this is how a feature is *removed* from a store's binary).
2. **Feature self-registration** — each feature **contributes its own tab** (or Settings section)
   into the app's DI graph via Metro multibindings. There is **one** store-agnostic graph
   (`androidApp/src/main/.../di/AppGraph.kt`) exposing `Set<AppTab>`; the app renders whatever the
   linked features contributed. **There are no per-store source sets** (`androidApp/src/storeX` does
   not exist) — the flavor's linked modules determine the contributed set automatically.
3. **Experiences/capabilities** (`core/.../Capability.kt`) — runtime gating of tabs/actions/data per
   logged-in user. See [`EXPERIENCES.md`](EXPERIENCES.md).

So: *manifest* = does the build contain it; *contribution* = it shows up automatically;
*capability* = whether this user sees it.

---

## Add a new FEATURE

Worked example: a `reports` feature shown as a tab in storeA.

### 1. Create the module `features/reports/`
- `features/reports/build.gradle.kts` — copy `features/orders/build.gradle.kts` (Metro + Compose
  plugins, `androidMain` Compose deps, `commonMain` → `:core` + `compose.runtime`); change the
  namespace to `com.isharaw.kmpproj.feature.reports`.
- `features/reports/src/commonMain/.../Reports.kt` — logic:
  ```kotlin
  @Inject @SingleIn(AppScope::class)
  class ReportsRepository { fun all(): List<String> = listOf("Q1", "Q2") }
  ```
- `features/reports/src/androidMain/.../ReportsScreen.kt` — the Compose screen
  (`@Composable fun ReportsScreen(repository: ReportsRepository)`).

### 2. Register the module — `settings.gradle.kts`
```kotlin
include(":features:reports")
```

### 3. Add it to the manifest for the stores that ship it — `buildSrc/StoreManifest.kt`
```kotlin
"storeA" to listOf("login", "cart", "invoices", "settings", "orders", "reports"),
```

### 4. Self-register the destination — `features/reports/src/androidMain/.../ReportsContribution.kt`
Each feature contributes a Navigation 3 **`NavDestination`** (its `NavKey` + `TabMeta` for the
bottom bar + an `entry { }` installer). This is the only wiring; **no app-side or per-store edits**:
```kotlin
data object ReportsKey : NavKey

@ContributesTo(AppScope::class)
@BindingContainer
object ReportsContribution {
    @Provides @IntoSet
    fun reportsDestination(repository: ReportsRepository): NavDestination =
        NavDestination(ReportsKey, TabMeta("Reports", "📊", order = 40 /*, requiredCapability = … */)) {
            entry<ReportsKey> { ReportsScreen(repository = repository) }
        }
}
```
- `TabMeta.order` positions the tab (the multibinding set is unordered).
- Add `requiredCapability = Capability.X` to hide it for some experiences (see EXPERIENCES.md).
- Need the current experience for data scoping? Inject `SessionManager` and read
  `sessionManager.session!!.experience` (see `InvoicesContribution`).
- The `:real` module needs `implementation(libs.androidx.navigation3.runtime)`.

### 5. (A feature reached *from* another screen)
There's no `Slot` anymore — a feature is just its own `NavDestination`. To open it from elsewhere
(e.g. a button in Settings), expose its `NavKey` and `backStack.add(ReportsKey)`. (Rebate is a plain
top-level destination today.)

### 6. iOS (only if it ships there)
The framework export is automatic (the manifest drives `shared`). Add the SwiftUI view/tab in
`iosApp/iosApp/ContentView.swift` (guard with `#if STORE_HAS_REPORTS` if store-specific).

### 7. Verify — see [below](#verification).

---

## Add a new STORE

Worked example: **storeC** = `login, settings, orders`.

### 1. One manifest line — `buildSrc/StoreManifest.kt`
```kotlin
val stores = linkedMapOf(
    "storeA" to listOf("login", "cart", "invoices", "settings", "orders"),
    "storeB" to listOf("login", "cart", "settings", "rebate"),
    "storeC" to listOf("login", "settings", "orders"),
)
```
That's it for Android: you get the flavor `storeCDebug/Release` (applicationId
`com.isharaw.kmpproj.storec`) linking exactly those modules; their contributions form storeC's tabs.
**No `androidApp/src/storeC` source set is needed** — that's the whole point of self-registration.

> A store just needs to include `login` (the gate) and `settings` (the only always-present tab).
> Everything else is whatever modules you list.

### 2. iOS (only if the store ships there) — manual
- `iosApp/Configuration/Config-storeC.xcconfig` (copy `Config-storeB.xcconfig`): set
  `PRODUCT_BUNDLE_IDENTIFIER`, `GRADLE_STORE=storeC`, and the `STORE_HAS_*` flags it ships.
- In `iosApp.xcodeproj`, add `StoreC-Debug/Release` build configs (mirror `StoreB-*`) + an
  `iosApp-storeC` scheme. *(This is the only part not driven by the manifest.)*

### 3. Verify.

---

## Verification

```
./gradlew :androidApp:assembleStoreADebug :androidApp:assembleStoreBDebug :androidApp:assembleStoreCDebug
# feature membership (per store):
./gradlew :androidApp:dependencies --configuration storeCDebugRuntimeClasspath | grep -c features:orders   # 1
./gradlew :androidApp:dependencies --configuration storeCDebugRuntimeClasspath | grep -c features:invoices # 0
# iOS framework still links:
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeA
```
Then run each variant from Android Studio's **Build Variants** panel — each store shows exactly the
tabs its modules contribute, gated by the logged-in experience.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| New feature's tab doesn't appear | No contribution, or the module isn't in that store's manifest | Add the `@ContributesTo @BindingContainer` with `@Provides @IntoSet AppTab`; add the module to the store in `StoreManifest` |
| `Unresolved reference: ReportsRepository` in androidApp | n/a — the app no longer references feature repos directly | Make sure the contribution lives in the **feature's** `androidMain`, not androidApp |
| `Smart cast … impossible` on `requiredCapability` | nullable property from another module | copy to a local `val` before the null check |
| Tabs in the wrong order | `Set` is unordered | set distinct `order` values on each `AppTab` |
| iOS storeX builds wrong feature set | missing xcconfig / build config / scheme | add `Config-storeX.xcconfig` + configs + scheme |
