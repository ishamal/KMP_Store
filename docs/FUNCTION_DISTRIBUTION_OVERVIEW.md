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

### 8.1 Single source of truth — `buildSrc/StoreManifest.kt`
- `buildSrc` is a special Gradle module: it's compiled first and put on **every build script's
  classpath automatically**, in type-safe Kotlin.
- `StoreManifest` is one map — **store → list of functions** — that *both* the build configuration and
  the packaging read. Change membership in **one place**, everywhere updates. No duplication.

### 8.2 Build-time distribution — Gradle flavors & per-flavor dependencies
- Gradle generates a **product flavor per store** straight from `StoreManifest`.
- For each flavor, **only that store's feature modules** are added as dependencies
  (`add("${store}Implementation", project(":features:<f>:real"))`).
- The shared (iOS) module **exports only the selected store's** `:api` modules.
- **Net effect:** an app's binary physically contains *only its functions* — Store A's app doesn't even
  include Store B's code.

### 8.3 Self-registration & assembly — Metro `@IntoSet` + `@Multibinds`
This is what lets the app **discover** its functions without a central wiring list:
- Each feature **contributes itself** with `@Provides @IntoSet` (a screen installer, a bottom-bar tab, a
  settings/feature action) inside a `@ContributesTo(scope)` container.
- The app graph **declares the collectors**: `@Multibinds(allowEmpty = true) val tabs: Set<Tab>`,
  `… entryInstallers: Set<EntryProviderInstaller>`, `… featureActions: Set<FeatureAction>`.
- At compile time, **Metro gathers every `@IntoSet` contribution that's on the classpath** — i.e. the
  modules the active flavor linked — into those sets.
- The app simply **renders whatever is in the sets**. There is no master list to edit when a function is
  added or removed.
- **`@Multibinds(allowEmpty = true)` is the key enabler:** a store that ships *none* of a given function
  type still gets a valid (empty) set — so omitting a function is a config change, not a code change.

> 🔑 **Why `@Multibinds` matters here:** it is the "collection point." Build-time decides *which*
> `@IntoSet` contributions exist on the classpath; `@Multibinds` is what assembles them into the set the
> app consumes. Together they turn "which functions are in this app" into pure configuration.

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
- **One place to change membership** (`StoreManifest`); the **compiler enforces** it and **Metro
  assembles** it — no manual per-app wiring, no drift.
- **Adding a function or a store** touches *configuration*, not every app's code.
- **`@Multibinds` + `@IntoSet`** make features true plug-ins: drop the module in (via the manifest) and
  it appears; remove it and it's gone — safely, because empty sets are allowed.

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
