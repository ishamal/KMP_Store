# Function Distribution Across Multiple Apps — Platform Overview

> **Audience:** Leadership / Product / Engineering management
> **Purpose:** Explain, in business terms, how one platform delivers many branded apps — each with the
> right set of functions for that brand and for each type of user.
> **Companion diagrams** (open in [draw.io](https://app.diagrams.net) / diagrams.net):
> - `function-distribution-usecase.drawio` — **use case diagram** (actors + functions)
> - `function-distribution-flow.drawio` — **flow diagram** (build-time → run-time)

---

## ℹ️ Executive summary

We build **one shared platform** and assemble **multiple apps** from it. Every business function
(orders, rebates, invoices, settings, etc.) is a self-contained building block. For each app we simply
choose **which blocks to include**, and at runtime each app automatically shows **only the functions a
given user is allowed to use**.

**Result:** new brands and new functions ship faster, at lower cost, with consistent quality — without
forking the codebase per customer.

---

## 1. The business problem

- We serve **multiple brands/customers** ("stores") that need **different mixes of functions**.
- Within a single brand, **different user types** (e.g. internal vs. customer) should see **different
  functions**.
- Building and maintaining a **separate app per brand** is slow, expensive, and error-prone (every fix
  must be repeated everywhere).

## 2. The solution at a glance

| Principle | What it means | Business value |
|---|---|---|
| **One platform** | Single shared codebase; write a function once | No duplicated effort |
| **Functions as building blocks** | Each function is an independent module | Mix-and-match per app |
| **Per-app selection** | A manifest lists which functions each app ships | New brand = a configuration, not a rewrite |
| **Per-user adaptation** | Each app tailors itself to the logged-in user | One app serves many user types |

---

## 3. Two levels of distribution (the key concept)

Functions are distributed in **two independent layers**:

| Layer | Decides… | When | Controlled by | Analogy |
|---|---|---|---|---|
| **Build-time (per app)** | *Is the function in this app at all?* | When the app is built/released | **Store Manifest** (one config) | What's in the box you ship |
| **Run-time (per user)** | *May THIS user see/use it?* | After the user logs in | **Experience + Capabilities** | What this person is allowed to touch |

> 🟦 **Build-time** keeps each app lean — Brand A's app doesn't even contain Brand B's functions.
> 🟪 **Run-time** lets a single app serve different user types without separate builds.

---

## 4. Use case walkthrough

**Scenario:** Two brands on the same platform.

| | **Store A** | **Store B** |
|---|---|---|
| Functions shipped (build-time) | Orders, Cart, Invoices, Settings | Cart, Settings, **Rebates** |
| Example user — *full access* | sees Orders, all invoices, export | sees Rebates |
| Example user — *restricted* | sees pending invoices only, no export | sees a limited rebate view |

**Step by step (what a user experiences):**
1. The user installs **Store A's** app → it contains only Store A's functions (Orders, Cart, …).
2. The user **logs in** → the platform identifies their **experience** (e.g. *full* vs *restricted*).
3. The app checks the user's **capabilities** and **shows only the permitted functions** — e.g. a
   restricted user sees pending invoices but no "Export" button, and no Orders tab.
4. A different user on the **same installed app** logs in and sees a **different** set of functions —
   no new app, no redeploy.

> The same two mechanisms also let us place a function in the right spot — e.g. a function can appear
> as a main tab in one context and inside the "More" menu in another, purely by configuration.

---

## 5. How a new function reaches an app (lifecycle)

```
Build the function (once)
        │
Add it to the Store Manifest for the apps that should ship it
        │
It self-registers in those apps' navigation/menus automatically  (no app-by-app rewiring)
        │
At runtime it appears only for users whose experience grants the capability
```

**Why this matters to delivery:** adding a function to a brand is typically a **one-line manifest
change** plus the function module — not a bespoke integration in each app.

---

## 6. Business benefits

| Benefit | How the platform delivers it |
|---|---|
| ⏱️ **Faster time-to-market** | New brand = select functions in a manifest; new function = add one module |
| 💰 **Lower cost** | Build/maintain each function once instead of per brand |
| ✅ **Consistent quality** | A fix to a shared function benefits every app at once |
| 🎯 **Per-customer customization** | Each brand ships exactly its functions; each user sees exactly their allowed set |
| 🔒 **Reduced risk** | Apps don't contain unrelated brands' code; user permissions are centrally controlled |
| ♻️ **Reuse across platforms** | The same shared core is designed to serve Android and iOS |

---

## 7. Diagrams

### 7.1 Use case diagram
Open **`function-distribution-usecase.drawio`**. It shows the **actors** and the **functions (use
cases)** they can reach:

- **Actors:** *Full-access User (USBL)*, *Restricted User (CABL)*, *Release/Build Engineer*.
- **Build-time use case:** the engineer *selects which functions each app ships* (Store Manifest).
- **Run-time use cases (inside the app):** Log in, View Orders, View Invoices, Export Invoices,
  View Rebates, Manage Settings, Reset Password.
- **Relationships:** functions `<<include>>` *Log in* (always required); *Export Invoices*
  `<<extend>>` *View Invoices* (shown only when the user's capability allows).
- The **Restricted User** has no line to *View Orders* / *Export Invoices* — visualizing run-time gating.

```
   (Release Engineer) ── Select functions per app (Store Manifest)        [build time]
   ─────────────────────────────────────────────────────────────────────
   ┌─ Store App ─────────────────────────────────────────────┐           [run time]
   (USBL) ───── Log in · View Orders · View Invoices ·        │
        │       Export Invoices · Manage Settings · Reset PW  │
   (CABL) ───── Log in · View Invoices · Manage Settings ·    │
                Reset Password        (no Orders / no Export)  │
   └─────────────────────────────────────────────────────────┘
```

### 7.2 Flow diagram

Open **`function-distribution-flow.drawio`** in diagrams.net for the editable version. Text outline:

```
                 ┌───────────────────────────────────────────┐
                 │      ONE SHARED PLATFORM (codebase)        │
                 │  Functions: Login, Cart, Invoices,         │
                 │  Orders, Rebates, Settings, …              │
                 └───────────────────────┬───────────────────┘
                                         │  BUILD TIME
                                         ▼
                 ┌───────────────────────────────────────────┐
                 │   STORE MANIFEST — which functions per app │
                 └───────┬───────────────┬───────────────────┘
                         ▼               ▼               ▼
                 ┌────────────┐   ┌────────────┐   ┌────────────┐
                 │  Store A   │   │  Store B   │   │  Store C   │
                 │ Orders,    │   │ Cart,      │   │ Settings,  │
                 │ Cart,      │   │ Settings,  │   │ Orders     │
                 │ Invoices…  │   │ Rebates    │   │            │
                 └─────┬──────┘   └─────┬──────┘   └─────┬──────┘
                       └───────── user opens an app ─────┘
                                         │  RUN TIME
                                         ▼
                          ┌──────────────────────────┐
                          │  User logs in            │
                          │  → Experience (full /     │
                          │     restricted)           │
                          └─────────────┬────────────┘
                                         ▼
                          ┌──────────────────────────┐
                          │  Capabilities catalog     │
                          │  (what this user may do)  │
                          └─────────────┬────────────┘
                                         ▼
                          ┌──────────────────────────┐
                          │  Functions shown / hidden │
                          │  for THIS user            │
                          └──────────────────────────┘
```

---

## 8. How we achieve it — the technical mechanism (for engineering)

The two-layer distribution above is delivered by **three technologies working together**:
a single config in **`buildSrc`**, **Gradle** build-time wiring, and **Metro DI multibindings**
(`@IntoSet` + `@Multibinds`).

### 8.1 Single source of truth — one config file per store
Each store is described by a tiny, human-readable config file in a common location
`config/stores/<store>.properties`:

```properties
# config/stores/storeA.properties
applicationId=com.isharaw.kmpproj.storea
features=login,cart,invoices,settings,orders,rebate,passwordReset
```
```properties
# config/stores/storeB.properties
applicationId=com.isharaw.kmpproj.storeb
features=login,cart,settings,rebate
```

`StoreManifest` (in `buildSrc`, on every build script's classpath) simply reads these files and exposes
the data to the build:

```kotlin
object StoreManifest {
    const val SELECTED_STORE = "storeA"

    // store -> the functions it ships (read from config/stores/<store>.properties)
    val stores: Map<String, List<String>> get() = loadConfigs().mapValues { it.value.features }

    fun featuresFor(store: String): List<String> = loadConfigs()[store]!!.features
    fun applicationId(store: String): String = loadConfigs()[store]!!.applicationId
}
```

**A store is a single config file.** Define a store's identity and the functions it ships in one place;
everything else (the Android app, the iOS framework) reads from it. No code change to add or adjust a store.

### 8.2 Build-time distribution — generated from the config
Gradle generates a **product flavor per store** and links **only that store's functions**, straight
from the config — no per-store build code:

```kotlin
// androidApp/build.gradle.kts  — flavors + dependencies are derived from the config
productFlavors {
    StoreManifest.stores.keys.forEach { store ->
        create(store) { applicationId = StoreManifest.applicationId(store) }
    }
}

dependencies {
    StoreManifest.stores.forEach { (store, features) ->
        features.forEach { feature ->
            add("${store}Implementation", project(":features:$feature:real"))
        }
    }
}
```

**Net effect:** each app's binary physically contains *only its functions*. You can prove it:
```bash
./gradlew :androidApp:dependencies --configuration storeBDebugRuntimeClasspath | grep features
#  → only Store B's features (cart, settings, rebate). Store A's code is simply not there.
```
The iOS framework composes the same way from the same config, so Android and iOS stay in lock-step.

### 8.3 Self-registration & assembly — Metro `@IntoSet` + `@Multibinds`
Each function **registers itself** — there's no central list of "what's in the app." A feature
contributes itself into shared collections:

```kotlin
// features/orders/.../OrdersContribution.kt — the Orders function plugs itself in
@ContributesTo(AppScope::class)
@BindingContainer
object OrdersContribution {
    @Provides @IntoSet
    fun ordersEntry(repo: OrderRepository): EntryProviderInstaller = { entry<OrdersKey> { OrdersScreen(repo) } }

    @Provides @IntoSet
    fun ordersTab(): Tab = Tab(OrdersKey, TabMeta("Orders", "📦", order = 20))
}
```

The app graph just **declares the collectors** and the app renders whatever arrived:

```kotlin
// androidApp/.../AppGraph.kt
@Multibinds(allowEmpty = true) val tabs: Set<Tab>
@Multibinds(allowEmpty = true) val entryInstallers: Set<EntryProviderInstaller>
@Multibinds(allowEmpty = true) val featureActions: Set<FeatureAction>
```

At compile time **Metro gathers every `@IntoSet` contribution on the classpath** — i.e. exactly the
functions the active store linked — into those sets. The app renders the sets; there is **no master
list to edit** when a function is added or removed. `@Multibinds(allowEmpty = true)` means a store that
ships none of a given function type still gets a valid (empty) set — so omitting a function is a config
change, never a code change.

> 🔑 **Why `@Multibinds` matters here:** it is the "collection point." Build-time decides *which*
> `@IntoSet` contributions exist on the classpath; `@Multibinds` is what assembles them into the set the
> app consumes. Together they turn "which functions are in this app" into pure configuration.

#### Anatomy of a `FeatureAction`
A `FeatureAction` is how a function exposes an entry point into a shared host surface (e.g. the Settings
screen). It is **pure data** — the function describes *what* it is and *where* it can appear; the host
decides how to draw it.

```kotlin
class FeatureAction(
    val label: String,            // text shown to the user
    val target: NavKey,           // destination opened on click
    val slots: Set<FeatureSlot>,  // which host surface(s) may render it
    val kind: FeatureKind,        // which feature this is — host maps it to a look/placement
    val order: Int = 0,           // sort order within a host (lower = first)
)
```

| Field | Meaning | At runtime |
|---|---|---|
| `label` | text shown to the user | rendered as the button/card label |
| `target` | the destination it opens | on click → navigate to `target` |
| `slots` | host surface(s) it belongs to (a `Set`) | a host keeps only actions whose `slots` contain it; the `Set` lets one action appear in several surfaces |
| `kind` | which feature it is | the host maps the kind to a concrete look/placement |
| `order` | position among siblings | actions are sorted ascending |

Supporting enums: **`FeatureSlot`** = *where* it can appear (e.g. `SETTINGS`); **`FeatureKind`** =
*what* it is (e.g. `REBATE`, `PASSWORD_RESET`).

The two real contributions:

```kotlin
// Rebate → shown in Settings, drawn as a card, sorted after order 0
FeatureAction(label = "Rebates",        target = RebateKey,        slot = FeatureSlot.SETTINGS,
              kind = FeatureKind.REBATE,         order = 10)

// Password reset → shown in Settings, drawn as a button, sorted first
FeatureAction(label = "Reset password", target = PasswordResetKey, slot = FeatureSlot.SETTINGS,
              kind = FeatureKind.PASSWORD_RESET, order = 0)
```

How the host (Settings) uses these values: **filter** by `slots` → **sort** by `order` → **render** by
`kind` (Rebate as a card, Password reset as a button), each showing its `label`; on tap →
`navigator.goTo(target)`. A function controls its own presence and presentation entirely through this
small data object — no edits in the host.

### 8.4 End-to-end flow

```
  buildSrc/StoreManifest.kt          (ONE map:  store -> [functions])
            │   read by Gradle (type-safe, on every build script's classpath)
            ▼
  Gradle: product flavor per store + per-flavor module dependencies      ── BUILD TIME ──
     storeA  ->  links feature modules  Orders, Cart, Invoices, Settings
     storeB  ->  links feature modules  Cart, Settings, Rebates
            │   only LINKED modules are compiled into the app
            ▼
  Metro compiles the app's DI graph and COLLECTS every @IntoSet
  contribution from the linked modules into the @Multibinds sets:
     Set<Tab> · Set<EntryProviderInstaller> · Set<FeatureAction>
            │   app renders exactly the assembled sets (self-registration)
            ▼
  On login:  Experience -> Capabilities  filter those sets              ── RUN TIME ──
            ▼
  User sees exactly the functions their app shipped AND they're allowed
```

### 8.5 Why this design pays off
- **One place to change membership** (the store config); the **compiler enforces** it and **Metro
  assembles** it — no manual per-app wiring, no drift.
- **Adding a function or a store** touches *configuration*, not every app's code.
- **`@Multibinds` + `@IntoSet`** make features true plug-ins: list the module in a store's config and
  it appears; remove it and it's gone — safely, because empty sets are allowed.

### 8.6 Common *and* exclusive functions — one uniform mechanism
Every function lives in exactly **one place** — an independent module under `features/` — and whether it
is shared by many stores or exclusive to one is decided **purely by configuration**. There is nothing
special to set up for an exclusive function; it's the same module, just listed in fewer stores.

```properties
# config/stores/storeA.properties
features=login,cart,invoices,settings,orders,rebate,passwordReset

# config/stores/storeB.properties
features=login,cart,settings,rebate
```

Read straight off the config:
- **`login`, `settings`** appear in every store → *common* functions.
- **`invoices`, `orders`, `passwordReset`** appear in some stores → *partly shared*.
- **`rebate`** could appear in just one store → *exclusive to that store* — yet it's the very same
  `features/rebate` module, with the same structure as every other function.

**"Common vs exclusive" is data, not structure.** The same module powers both cases; the only difference
is how many store configs name it. This keeps every function uniform, reusable, and discoverable in one
predictable location — and means an exclusive function needs **no special placement** to be exclusive.

### 8.7 Adding or sharing a function — configuration, not code

**Make an exclusive function shared** = add its name to another store's config. One line:
```diff
  # config/stores/storeA.properties
- features=login,cart,invoices,settings,orders,passwordReset
+ features=login,cart,invoices,settings,orders,rebate,passwordReset
```
That single edit ships `rebate` to Store A too — the flavor links it, Metro registers it, both Android
and iOS pick it up. No module moves, no rewiring.

**Add a whole new store** = one command (scaffolds the config + branding), then build:
```bash
./scripts/new-store.sh storeD login,settings,orders
./gradlew :androidApp:assembleStoreDDebug
```

**Add a brand-new function** = create one module under `features/<name>/{api,real}`, let it self-register
(`@IntoSet`), and list it in the stores that should ship it. Every function — common or exclusive —
follows this same path.

---

## 9. Glossary

| Term | Plain meaning |
|---|---|
| **Platform / shared codebase** | The single code base all apps are built from |
| **Function (feature)** | A self-contained capability (Orders, Rebates, …) |
| **App / Store** | A branded application assembled from selected functions |
| **Store Manifest** | The configuration listing which functions each app ships (build-time) |
| **Experience** | The "type" of logged-in user (e.g. full vs restricted) |
| **Capability** | A specific permission an experience grants (run-time) |
