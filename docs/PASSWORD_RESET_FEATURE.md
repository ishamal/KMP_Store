# Adding the Password-Reset Feature — Step by Step

A worked, real example of adding a feature that is:
- **store-gated** — ships in **storeA + storeC only** (not storeB), and
- **reached from a button**, not a bottom tab — the **"Reset password"** button on the Settings screen,
- a **sample screen** showing a single line: *"this is password reset"*.

Companion to [`ADDING_STORES_AND_FEATURES.md`](ADDING_STORES_AND_FEATURES.md). All packages are
`com.isharaw.kmpproj.*`. Verified with `./gradlew :androidApp:assembleStore{A,B,C}Debug` → BUILD SUCCESSFUL.

---

## The two hard problems (and how we solved them)

This feature is trickier than a normal tab feature for two reasons:

1. **Settings ships in every store, but password-reset does not.**
   So `:features:settings:real` **cannot depend** on `:features:passwordReset:*` — that module is
   absent in storeB and the build would break. → Solved with a **DI multibinding** (`FeatureAction`):
   password-reset *contributes* a button into Settings; Settings just renders whatever buttons it's
   given. In storeB that set is empty, so no button appears.

2. **A feature must register a screen without necessarily being a bottom tab, and screens can't navigate.**
   Features self-register via two independent multibindings: an **`EntryProviderInstaller`** (registers
   the Nav3 screen) and a **`Tab`** (the bottom-bar item). Password-reset contributes an installer but
   **no `Tab`**, so it has a screen with no tab. Plus a `Navigator` CompositionLocal lets the Settings
   button push the password-reset destination onto the back stack.

   > Earlier this used a single `NavDestination` + a `TabMeta.showInBottomBar` flag. We later adopted
   > the `EntryProviderInstaller` + `Tab` split (mirroring the other project's pattern), which makes
   > "register a screen" and "show a tab" two separate contributions — so a hidden screen is simply
   > "an installer with no Tab," and the flag is gone.

```
Settings screen ──renders──> FeatureAction("Reset password", target = PasswordResetKey, slot = SETTINGS)
        │ (multibinding, contributed ONLY by the passwordReset module)
        └─onClick─> LocalNavigator.goTo(PasswordResetKey)
                          │
                          └─> NavDisplay shows PasswordResetScreen ("this is password reset")
                              (registered via EntryProviderInstaller; contributes no Tab)
```

---

## Step 1 — Core plumbing (`:core`)

Three small, reusable additions. These are generic — any future "button → sub-screen" feature reuses them.

### 1a. `EntryProviderInstaller` + `Tab` — separate "register a screen" from "show a tab"
`core/src/androidMain/.../Navigation.kt`
```kotlin
// A function a feature contributes to register its Nav3 screen(s).
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

// A bottom-bar tab a feature contributes. Screens reached from a button contribute NO Tab.
class Tab(val key: NavKey, val meta: TabMeta)
```
`TabMeta` is unchanged (label/icon/order/`requiredCapability`) — there is no `showInBottomBar` flag;
a screen simply has a tab if (and only if) its feature also contributes a `Tab`.

### 1b. `Navigator` — let any screen push/pop the back stack
`core/src/androidMain/.../Navigator.kt` (new)
```kotlin
interface Navigator {
    fun goTo(key: NavKey)
    fun back()
}
val LocalNavigator = staticCompositionLocalOf<Navigator> { error("LocalNavigator not provided …") }
```
Requires `implementation(libs.compose.runtime)` in `core`'s **androidMain** (for `staticCompositionLocalOf`).
Added to `core/build.gradle.kts`. (Android-only — not exported to iOS.)

### 1c. `FeatureAction` — a generic, store-gated entry point features expose into a host surface
`core/src/androidMain/.../FeatureAction.kt` (new)
```kotlin
enum class FeatureSlot { SETTINGS /*, HOME, MENU, … */ }   // which host renders the action

class FeatureAction(
    val label: String,
    val target: NavKey,            // destination opened on click
    val slots: Set<FeatureSlot>,   // the surfaces it appears in (can be several)
    val order: Int = 0,
) {
    // convenience for the common single-surface case
    constructor(label: String, target: NavKey, slot: FeatureSlot, order: Int = 0)
        : this(label, target, setOf(slot), order)
}
```
Generic on purpose: this single multibinding can feed the Settings screen today and other surfaces
later. Each host renders only the actions whose `slots` contain it, e.g. Settings filters with
`FeatureSlot.SETTINGS in it.slots`. A single action can declare multiple slots to appear in more
than one surface. (This replaced the original Settings-specific `SettingsAction`.)

---

## Step 2 — Create the feature module `features/passwordReset/`

Follows the standard `:api` / `:real` split (copied from `features/settings/`).

```
features/passwordReset/
├── api/
│   ├── build.gradle.kts                     # namespace …feature.passwordreset.api, NO Metro
│   └── src/commonMain/.../PasswordReset.kt  # placeholder contract (PasswordResetGateway)
└── real/
    ├── build.gradle.kts                     # namespace …passwordreset.real, Metro + Compose
    └── src/androidMain/.../
        ├── ui/PasswordResetScreen.kt        # the sample screen
        └── di/PasswordResetContribution.kt  # self-registration (the wiring)
```

### The screen — `PasswordResetScreen.kt`
```kotlin
@Composable
fun PasswordResetScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("this is password reset")
    }
}
```

### The contribution — `PasswordResetContribution.kt` (the only wiring)
Contributes **two** things into the app graph via Metro `@Provides @IntoSet`. Note it contributes an
`EntryProviderInstaller` but **no `Tab`** — that's what keeps it off the bottom bar:
```kotlin
data object PasswordResetKey : NavKey

@ContributesTo(AppScope::class)
@BindingContainer
object PasswordResetContribution {
    // 1. Register the screen (NO Tab → no bottom-bar item; reached only from the button).
    @Provides @IntoSet
    fun passwordResetEntry(): EntryProviderInstaller = {
        entry<PasswordResetKey> { PasswordResetScreen() }
    }

    // 2. The Settings button that opens it.
    @Provides @IntoSet
    fun passwordResetAction(): FeatureAction =
        FeatureAction(label = "Reset password", target = PasswordResetKey, slot = FeatureSlot.SETTINGS, order = 0)
}
```
Because both live in **this** module, they only enter the graph in stores that link it.

> A normal tab feature (e.g. cart) instead contributes **both** an `EntryProviderInstaller` *and* a
> `Tab(CartKey, TabMeta("Cart", "🛒", order = 10))`.

`real/build.gradle.kts` needs `implementation(libs.androidx.navigation3.runtime)` (for `NavKey`),
the Compose deps, Metro, and `api(project(":features:passwordReset:api"))`.

---

## Step 3 — Register the module — `settings.gradle.kts`
```kotlin
include(":features:passwordReset:api")
include(":features:passwordReset:real")
```

## Step 4 — Gate it to storeA + storeC — `buildSrc/StoreManifest.kt`
```kotlin
"storeA" to listOf("login", "cart", "invoices", "settings", "orders", "passwordReset"),  // ← added
"storeB" to listOf("login", "cart", "settings", "rebate"),                                //   (not here)
"storeC" to listOf("login", "settings", "orders", "passwordReset"),                       // ← added
```
This is the **only** place membership is decided. `androidApp/build.gradle.kts` already loops the
manifest (`add("${store}Implementation", project(":features:$feature:real"))`), so storeA/storeC
link `passwordReset:real` and storeB does not — automatically.

---

## Step 5 — Make Settings render contributed buttons (`:features:settings:real`)

Settings stays store-agnostic — it depends on **`FeatureAction` (core)**, never on password-reset.

### `SettingsContribution.kt` — inject the multibound set (entry + its tab)
```kotlin
@Provides @IntoSet
fun settingsEntry(
    repository: SettingsRepository,
    sessionManager: SessionManager,
    featureActions: Set<FeatureAction>,      // ← injected multibinding (empty in storeB)
): EntryProviderInstaller = {
    entry<SettingsKey> {
        SettingsScreen(
            repository = repository,
            userEmail = sessionManager.session?.email.orEmpty(),
            actions = featureActions
                .filter { it.slot == FeatureSlot.SETTINGS }   // only SETTINGS-slot actions
                .sortedBy { it.order },
            onLogout = { sessionManager.session = null },
        )
    }
}

@Provides @IntoSet
fun settingsTab(): Tab = Tab(SettingsKey, TabMeta("Settings", "⚙️", order = 100))
```

### `SettingsScreen.kt` — render a button per action
```kotlin
val navigator = LocalNavigator.current
// …
actions.forEach { action ->
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { navigator.goTo(action.target) }, modifier = Modifier.fillMaxWidth()) {
        Text(action.label)
    }
}
```

---

## Step 6 — App-shell wiring (`androidApp`)

### `AppGraph.kt` — declare the multibindings
```kotlin
@Multibinds(allowEmpty = true) val entryInstallers: Set<EntryProviderInstaller>  // every screen
@Multibinds(allowEmpty = true) val tabs: Set<Tab>                                // only tab screens
@Multibinds(allowEmpty = true) val featureActions: Set<FeatureAction>            // store-gated actions
```
`allowEmpty = true` is **essential** for `featureActions` — storeB contributes none, so the set
must be allowed to be empty.

### `App.kt` — bar from `tabs`, entries from `entryInstallers`, provide the Navigator
```kotlin
val tabs = remember(experience) {
    graph.tabs.sortedBy { it.meta.order }
        .filter { it.meta.requiredCapability?.let(experience::has) ?: true }   // capability-gated
}
val backStack = remember(tabs) { mutableStateListOf<NavKey>(tabs.first().key) }

val navigator = remember(backStack) {
    object : Navigator {
        override fun goTo(key: NavKey) { backStack.add(key) }
        override fun back() { backStack.removeLastOrNull() }
    }
}
// bottom bar iterates `tabs`; entryProvider installs ALL screens via graph.entryInstallers.forEach { it() };
// NavDisplay is wrapped in CompositionLocalProvider(LocalNavigator provides navigator).
```

---

## Why each piece is where it is

| Concern | Lives in | Why |
|---|---|---|
| Is the feature in the build? | `StoreManifest` (storeA/C) | single source of truth for store membership |
| Does the button appear? | `FeatureAction` (slot = SETTINGS) multibinding from `passwordReset:real` | only linked modules contribute → auto store-gated |
| Settings ↔ password-reset coupling | none (both depend on `:core` only) | Settings can't depend on a module absent in storeB |
| Screen reachable but not a tab | contributes `EntryProviderInstaller` but **no** `Tab` | entry installed, no bottom-bar item |
| Button can navigate | `Navigator` / `LocalNavigator` | screens push the back stack without owning it |
| Empty button set in storeB | `@Multibinds(allowEmpty = true)` | storeB contributes zero `FeatureAction`s |

---

## Verification

```bash
# All three variants compile (storeB must build with an EMPTY featureActions set):
./gradlew :androidApp:assembleStoreADebug :androidApp:assembleStoreBDebug :androidApp:assembleStoreCDebug

# Membership (api + real = 2 when present, 0 when absent):
./gradlew :androidApp:dependencies --configuration storeADebugRuntimeClasspath | grep -c features:passwordReset  # 2
./gradlew :androidApp:dependencies --configuration storeBDebugRuntimeClasspath | grep -c features:passwordReset  # 0
./gradlew :androidApp:dependencies --configuration storeCDebugRuntimeClasspath | grep -c features:passwordReset  # 2
```
Actual results: storeA = 2, storeB = 0, storeC = 2 — and BUILD SUCCESSFUL for all three.

Then in Android Studio's **Build Variants** panel:
- **storeADebug / storeCDebug** → Settings shows a **"Reset password"** button → tapping it opens the
  *"this is password reset"* screen (system back returns to Settings).
- **storeBDebug** → Settings has **no** such button; the password-reset module isn't in the binary.

---

## Files changed (summary)

**New**
- `features/passwordReset/api/build.gradle.kts`
- `features/passwordReset/api/src/commonMain/.../PasswordReset.kt`
- `features/passwordReset/real/build.gradle.kts`
- `features/passwordReset/real/src/androidMain/.../ui/PasswordResetScreen.kt`
- `features/passwordReset/real/src/androidMain/.../di/PasswordResetContribution.kt`
- `core/src/androidMain/.../Navigator.kt`
- `core/src/androidMain/.../FeatureAction.kt` — generic `FeatureAction` + `FeatureSlot` (was `SettingsAction`)
- `core/src/androidMain/.../Navigation.kt` — `EntryProviderInstaller` typealias + `Tab`

**Edited (incl. the `EntryProviderInstaller` + `Tab` refactor)**
- `core/src/androidMain/.../NavDestination.kt` — **deleted** (replaced by `Navigation.kt`)
- `core/src/commonMain/.../TabMeta.kt` — (no `showInBottomBar`; tab-ness is now "contributes a `Tab`")
- `core/build.gradle.kts` — `compose.runtime` in androidMain
- `settings.gradle.kts` — include the two new modules
- `buildSrc/.../StoreManifest.kt` — add `passwordReset` to storeA + storeC
- `androidApp/.../di/AppGraph.kt` — `@Multibinds entryInstallers` + `tabs` + `featureActions`
- `androidApp/.../App.kt` — bar from `tabs`, entries from `entryInstallers`, provide `Navigator`
- every `*Contribution.kt` (cart, invoices, orders, rebate, settings, passwordReset) — now contribute
  an `EntryProviderInstaller` (+ a `Tab`, except password-reset)
- `features/settings/real/.../ui/SettingsScreen.kt` — render action buttons
