# Experiences (USBL / CABL) — per-user behaviour, centralized

> Navigation update: features self-register via two independent DI multibindings — an
> `EntryProviderInstaller` (registers the Nav3 `entry { }`) and an optional `Tab(key, TabMeta)`
> (the bottom-bar item). Screens reached from a button (not a tab) contribute an installer and **no**
> `Tab`. The older `NavDestination`/`AppTab`/`Slot` abstractions were removed. Non-common features can
> also surface as **Settings actions** via a `FeatureAction` multibinding (see the section below). See
> ADDING_STORES_AND_FEATURES.md and PASSWORD_RESET_FEATURE.md for the current patterns.

Each store serves two **experiences** — **USBL** and **CABL**. An experience changes what a
logged-in user sees/does at **runtime** (e.g. USBL sees all invoices, CABL sees only pending; some
tabs/actions are hidden). It's a different axis from stores, and it is driven by **one central
table of capabilities** — there are **no `experience == …` checks scattered around the code**.

## Two axes (don't confuse them)

| | Decided… | Single source of truth | Question it answers |
|---|---|---|---|
| **Store** (storeA/B/C) | **build time** | `buildSrc/StoreManifest.kt` | *Does this build contain the feature at all?* |
| **Experience** (USBL/CABL) | **runtime**, after login | `core/.../Capability.kt` (`ExperienceCatalog`) | *What may THIS logged-in user see/do?* |

`ExperienceCatalog` is the runtime analogue of `StoreManifest`: edit one table to change behaviour.

## How it works

1. **Experience + session** (`core/.../Experience.kt`):
   ```kotlin
   enum class Experience { USBL, CABL }
   data class Session(val email: String, val experience: Experience)
   ```
2. **Central capability table** (`core/.../Capability.kt`) — the only place experiences are mapped.
   Note data slices are modelled as **one capability each** (see "Data scoping" below):
   ```kotlin
   enum class Capability {
       VIEW_PAID_INVOICES, VIEW_PENDING_INVOICES, VIEW_OVERDUE_INVOICES,  // invoice slices
       EXPORT_INVOICES, VIEW_ORDERS,                                       // actions / tabs
   }

   object ExperienceCatalog {                       // ← edit here to change an experience
       private val capabilities = mapOf(
           Experience.USBL to Capability.entries.toSet(),               // full experience
           Experience.CABL to setOf(Capability.VIEW_PENDING_INVOICES),  // restricted
       )
       fun capabilitiesOf(e: Experience) = capabilities[e].orEmpty()
   }
   fun Experience.has(capability: Capability) = capability in ExperienceCatalog.capabilitiesOf(this)
   ```
3. **Login picks the experience** (`features/login/.../Authenticator.kt`, a **stub** — replace with
   the backend): returns `Session(email, experience)`.
4. **App shell** (`androidApp/src/main/App.kt`) keeps the `Session`, stores it in `SessionManager`,
   and passes `session.experience` down.

## Two ways behaviour changes — both read the central catalog (no `experience ==`)

### Data scoping — declarative, NO `if`/`when` on the experience
Don't branch on the experience (that grows into nested `if`/`when` as experiences multiply).
Instead model **each data slice as a capability** and filter **generically** — the feature's only
experience knowledge is a tiny static table, and adding an experience never touches it:
```kotlin
// features/invoices/commonMain — the feature's one small table (status → capability)
private val statusCapability = mapOf(
    InvoiceStatus.PAID to Capability.VIEW_PAID_INVOICES,
    InvoiceStatus.PENDING to Capability.VIEW_PENDING_INVOICES,
    InvoiceStatus.OVERDUE to Capability.VIEW_OVERDUE_INVOICES,
)
// generic filter — no experience comparison, no branching
fun InvoiceRepository.invoicesFor(experience: Experience): List<Invoice> =
    all().filter { experience.has(statusCapability.getValue(it.status)) }
```
"See everything" = the experience is granted all the slice capabilities. Restricting an experience
= grant fewer slices in `ExperienceCatalog`. A **new experience** is one row in the catalog; a
**new data slice** is one row in the feature's table — never an added branch.

### Capability gating (hide tabs / actions)
- **A whole tab is declarative** — the feature's `Tab` states the capability it needs (in its
  `TabMeta`), and the shell filters centrally (no `if` in any feature or store):
  ```kotlin
  // features/orders/.../OrdersContribution.kt — just declare it on the Tab:
  @Provides @IntoSet
  fun ordersTab(): Tab =
      Tab(OrdersKey, TabMeta("Orders", "📦", order = 20, requiredCapability = Capability.VIEW_ORDERS))

  // androidApp/.../App.kt — filtered in ONE place for all tabs:
  graph.tabs.sortedBy { it.meta.order }.filter { tab ->
      val cap = tab.meta.requiredCapability
      cap == null || experience.has(cap)
  }
  ```
- **An action inside a screen** checks the capability (still central policy, just a local read):
  ```kotlin
  // features/invoices/.../InvoicesScreen.kt
  if (experience.has(Capability.EXPORT_INVOICES)) { TextButton(...) { Text("Export") } }
  ```

### Settings actions (`FeatureAction`) — store-gated today, experience-gating is opt-in
Non-common features (Rebate, Password reset) surface inside Settings as `FeatureAction`s contributed
`@IntoSet`, keyed by `FeatureKind`; the Settings screen renders each kind (Rebate = card, Password
reset = button). These are **store-gated only** — they appear wherever the contributing module is
linked (`StoreManifest`), and currently do **not** consult the experience.

To also gate one by experience, do it the same way everything else does — via a capability, never an
`experience ==` check. Two options:
```kotlin
// (a) add a capability to FeatureAction and filter it where Settings collects them:
.filter { it.requiredCapability?.let(experience::has) ?: true }

// (b) or read the capability locally where the action is rendered in SettingsScreen:
if (experience.has(Capability.RESET_PASSWORD)) { /* render the action */ }
```
Either keeps policy in `ExperienceCatalog`. (Not wired yet — Settings actions are store-gated only.)

## What's implemented

| Capability | USBL | CABL |
|---|---|---|
| `VIEW_PAID_INVOICES` | ✅ | ❌ |
| `VIEW_PENDING_INVOICES` | ✅ | ✅ |
| `VIEW_OVERDUE_INVOICES` | ✅ | ❌ |
| `EXPORT_INVOICES` | ✅ Export shown | ❌ hidden |
| `VIEW_ORDERS` | ✅ Orders tab shown | ❌ hidden |

Net effect: USBL sees all invoices; CABL sees pending only. Grant/revoke a capability in
`ExperienceCatalog` to change this — nothing else changes.

## Adding behaviour
- **New capability:** add a `Capability` entry, grant it to the right experiences in
  `ExperienceCatalog`, then either check `experience.has(it)` (data/action) or set
  `requiredCapability = …` on a tab.
- **New experience:** add an `Experience` entry and its capability set in `ExperienceCatalog`, and
  map credentials → experience in `Authenticator`. No call sites change.

## Rules
- **Never compare experiences** (`experience == USBL`). Always go through `Capability` /
  `experience.has(...)` / `requiredCapability`, so policy stays in `ExperienceCatalog`.
- Keep capability checks for data in `commonMain` (pure, reusable on iOS); pass `experience` in.
- The app shell does tab gating in **one** place (`App.kt`, filtering `graph.tabs` by
  `meta.requiredCapability`); features never gate their own tab.

## iOS (not wired yet)
`Experience`, `Capability`, `ExperienceCatalog`, `Session` live in `commonMain`, so they're in the
`Shared` framework. Wire the SwiftUI `LoginView` to build a `Session` via `Authenticator`, pass
`experience` to views, and gate with `experience.has(...)`.

## Verify
```
./gradlew :androidApp:assembleStoreADebug :androidApp:assembleStoreBDebug :androidApp:assembleStoreCDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Pstore=storeA
```
Manual (storeADebug): log in as `cabl@x.com` → pending invoices only, no Export, no Orders tab;
any other email (USBL) → all invoices + Export + Orders tab.
