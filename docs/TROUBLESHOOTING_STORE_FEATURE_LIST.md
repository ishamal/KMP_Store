# Troubleshooting: "feature list is not coming" in `shared`

Why a store's feature "doesn't come" — there are **two independent failure modes**:
- **Part 1 (build-time):** the store-driven loop in `shared/build.gradle.kts` resolves an empty/wrong
  feature list, so the module is never exported/linked.
- **Part 2 (runtime DI):** the module *is* linked, but its `FeatureActions` never appears because the
  Metro `@ContributesTo` scope doesn't match the graph that consumes `Set<FeatureActions>`.

Use the [quick decision guide](#quick-decision-guide) at the very bottom to tell which one you have.

---

# Part 1 — build-time: the loop resolves an empty/wrong feature list

Based on the `environment`-property setup:

```kotlin
// shared/build.gradle.kts
val store = providers.gradleProperty("environment").getOrElse(StoreManifest.DEFAULT_STORE)
val storeFeatures = StoreManifest.getFeaturesForStore(store)

// …
storeFeatures.forEach { export(project(":feature-$it-api")) }
storeFeatures.forEach {
    implementation(project(":feature-$it-real"))
    api(project(":feature-$it-api"))
}
```

```kotlin
// StoreManifest
object StoreManifest {
    const val DEFAULT_STORE = "sysco"
    val stores = linkedMapOf(
        "sysco"   to listOf("order"),
        "newport" to listOf("rebate"),
    )
    fun getFeaturesForStore(store: String): List<String> = stores[store] ?: emptyList()
}
```

---

## TL;DR

`storeFeatures` is **not** read from the Android build variant you select. It's read **once, at Gradle
configuration time, from the `environment` Gradle property** — defaulting to `DEFAULT_STORE` ("sysco").
If that property is absent you always get `sysco` → `["order"]`; if it's set to anything that isn't a
key in `stores`, `getFeaturesForStore` **silently returns `emptyList()`** and the loop contributes
nothing. Either way a store's feature "doesn't come."

---

## Why it fails — three independent causes

### 1. `shared` resolves ONE store, from a property — not from the flavor
The Android **app** loops over *all* stores and adds per-flavor deps:
```kotlin
StoreManifest.stores.forEach { (store, features) ->
    features.forEach { f -> add("${store}Implementation", project(":feature-$f-real")) }
}
```
So `syscoDebug` gets `order`, `newportDebug` gets `rebate` — correct, automatically.

But `shared` does **not** do that. It picks a *single* store from a Gradle property:
```kotlin
val store = providers.gradleProperty("environment").getOrElse(StoreManifest.DEFAULT_STORE)
```
Selecting the `newport` build variant in the IDE does **not** set `environment=newport`. The Android
*flavor* and the *Gradle property* are unrelated. So unless something explicitly passes
`-Penvironment=newport`, `shared` always resolves to `DEFAULT_STORE = "sysco"` and only ever sees
`["order"]`. The newport feature (`rebate`) never reaches the iOS framework → "not coming."

### 2. `getOrElse` only fills in when the property is *absent*
```kotlin
providers.gradleProperty("environment").getOrElse(DEFAULT_STORE)
```
If the property is **present but empty** (`-Penvironment=` or `environment=` in `gradle.properties`),
`getOrElse` does **not** apply the default — you get `""`. Then `getFeaturesForStore("")` → `emptyList()`.

### 3. `getFeaturesForStore` hides the mistake
```kotlin
fun getFeaturesForStore(store: String) = stores[store] ?: emptyList()
```
Any unknown/empty/misspelled/wrong-case store returns `[]` with **no error**. So a wrong `environment`
value produces a silent empty list instead of a loud failure — which is exactly why this is hard to
spot. (Note `applicationIdForStore` already `throw`s on unknown — the two functions disagree.)

> Common trigger: the value is passed under a **different key**. If your CI / iOS build passes
> `-Pstore=newport` (an older name) but `shared` reads `environment`, the value is ignored and you
> silently fall back to `sysco`.

---

## Diagnose in 30 seconds

Add one log line after the resolution:
```kotlin
val store = providers.gradleProperty("environment").getOrElse(StoreManifest.DEFAULT_STORE)
val storeFeatures = StoreManifest.getFeaturesForStore(store)
logger.lifecycle("[shared] store='$store' features=$storeFeatures")
```
Run:
```bash
./gradlew :shared:help                        # expect: store='sysco'  features=[order]
./gradlew :shared:help -Penvironment=newport  # expect: store='newport' features=[rebate]
```
Read the result:
| Output of first command | Meaning |
|---|---|
| `store='sysco' features=[order]` | Resolution works; you were just expecting another store → pass `-Penvironment=…` (cause 1) |
| `store='' features=[]` | Something sets `environment` to empty (cause 2) |
| `store='<wrong>' features=[]` | Value isn't a key in `stores` — wrong key/spelling/case (cause 3) |

---

## The fix

### Fix 1 — fail loudly (do this first)
Make an unknown store an error so the real cause surfaces immediately instead of an empty list:
```kotlin
fun getFeaturesForStore(store: String): List<String> =
    stores[store] ?: error("Unknown store '$store'. Known stores: ${stores.keys}")
```

### Fix 2 — pass the store under the exact key `environment`
Whatever selects the store for `shared` (CI flag, `gradle.properties`, the iOS/Xcode Gradle
invocation) must use **`environment`** — matching what the code reads:
```properties
# gradle.properties (or -Penvironment=newport on the command line / in the iOS build script)
environment=newport
```
If anything currently passes `-Pstore=…`, rename it to `-Penvironment=…` (or read `store` in the code —
just make the two sides agree).

### Fix 3 — guard against an empty value (optional, robustness)
Treat a blank property like an absent one:
```kotlin
val store = providers.gradleProperty("environment").orNull
    ?.takeIf { it.isNotBlank() }
    ?: StoreManifest.DEFAULT_STORE
```

### Fix 4 — confirm the modules exist
The loop builds dash paths `:feature-<name>-api` / `:feature-<name>-real`. For `order`/`rebate`:
```kotlin
// settings.gradle.kts — exact dash names required
include(":feature-order-api");  include(":feature-order-real")
include(":feature-rebate-api"); include(":feature-rebate-real")
```
(A missing module gives "Project with path ':feature-…' could not be found" — a *different* error from
the silent empty list.)

---

## Mental model to remember

| | Android app | `shared` (iOS framework) |
|---|---|---|
| How the store is chosen | **all** stores, per build flavor | **one** store, from the `environment` Gradle property |
| Selecting `newport` variant in IDE | links `rebate` automatically | **no effect** — still uses the property/default |
| To get `newport`'s feature | build the `newport` flavor | pass `-Penvironment=newport` |

The feature list "doesn't come" because `shared` is waiting for a property the build never gave it (or
gave it wrong), and `getFeaturesForStore` swallowed the mistake.

---

# Part 2 — the feature is built/linked but STILL doesn't appear (DI scope mismatch)

Everything above is **build-time** (is the module in the binary?). There's a second, separate failure
mode at **runtime**: the module *is* linked, but its `FeatureActions` never shows up in the menu/MORE
section because of a **Metro scope mismatch** between where it's *contributed* and where it's *consumed*.

## The symptom
- Gradle is fine (`./gradlew projects` shows the module, the store loop is non-empty).
- The screen exists, but the feature's entry (e.g. Rebate in the MORE section) is missing.
- `Set<FeatureActions>` injected into the screen is empty or missing your feature.

## The cause — contribution scope ≠ consumer scope
A `@Provides @IntoSet` only lands in the **multibinding of the graph whose scope it's contributed to**.

```kotlin
@BindingContainer
@ContributesTo(CustomerScope::class)        // ← contributes into the CustomerScope graph
object RebateModule {
    @Provides @IntoSet
    fun rebateFeature(): FeatureActions = FeatureActions(
        target = RebateRoute, order = 0,
        availableSections = setOf(AvailableSections.MORE),
        featureKey = FeatureKEY.REBATE,
    )
}
```
```kotlin
@DependencyGraph(scope = AppScope::class, additionalScopes = [AnalyticsScope::class])
interface AndroidAppGraph : AppGraph, ViewModelGraph, MetroAppComponentProviders {
    @Multibinds(allowEmpty = true) val availableFeatures: Set<FeatureActions>   // ← AppScope
}
```

`RebateModule` contributes to **CustomerScope**, but `availableFeatures` is declared on the
**AppScope** graph. `AppScope` ≠ `CustomerScope`, so the rebate `FeatureActions` is **not** in the
`AppScope` set. If the MORE screen reads `availableFeatures` from `AndroidAppGraph`, rebate is missing.
`@Multibinds(allowEmpty = true)` makes that empty set *legal*, so it fails **silently** — no error.

| MORE screen injects `Set<FeatureActions>` from… | Rebate (`@ContributesTo(CustomerScope)`) appears? |
|---|---|
| the **CustomerScope** graph (`CustomerGraph`) | ✅ yes |
| the **AppScope** graph (`AndroidAppGraph`) | ❌ no — scope mismatch |

## What you need to do

**Step 1 — find the consumer.** Where is the set actually injected/read?
```bash
grep -rn "availableFeatures\|Set<FeatureActions>" --include=*.kt
```
Note which graph/scope that injection point belongs to.

**Step 2 — make contribution scope == consumer scope.** Pick ONE:

- **Option A (features are customer-scoped — usually correct here):** declare and consume the
  multibinding on the **CustomerGraph**, not the app graph:
  ```kotlin
  @DependencyGraph(scope = CustomerScope::class)
  interface CustomerGraph {
      @Multibinds(allowEmpty = true) val availableFeatures: Set<FeatureActions>
      // …
  }
  ```
  and inject `availableFeatures` from the customer graph (the one built by `customerGraphFactory`).
  Leave `RebateModule` as `@ContributesTo(CustomerScope::class)`.

- **Option B (features are app-scoped):** move the contribution up to the app scope:
  ```kotlin
  @ContributesTo(AppScope::class)   // was CustomerScope
  object RebateModule { /* … */ }
  ```
  so it lands in the same graph that declares `availableFeatures`.

> Do **not** declare the same `@Multibinds val availableFeatures` in *both* graphs and expect them to
> merge — they're separate sets. Choose the single scope where features live and keep both the
> contribution and the consumer there.

**Step 3 — verify both contributions use the same scope.** `RebateModule` also contributes an
`EntryProviderInstaller` (the screen). It must reach whatever runs the Nav3 `entryProvider`. Confirm
that consumer is in the **same** scope too, or the button will navigate to a route with no registered
screen.

**Step 4 — re-check the build-time prerequisite (Part 1).** A scope fix won't help if the module isn't
even linked for the active store. Confirm `:feature-rebate-real` is in the build (`./gradlew projects`)
and the store loop produced `rebate` (the `logger.lifecycle` line).

## Quick decision guide

```
Feature entry missing?
├─ Is the feature in the store you're building?  (StoreManifest[store] contains it)
│    └─ NO  → it can't appear — add it to that store, or build the store that has it  ← START HERE
├─ Is the module on the GRAPH module's classpath?  (dependencies | grep)
│    └─ NO  → Part 1 (Gradle property / includes / store membership)
└─ YES, module is linked into the graph
     └─ Is Set<FeatureActions> consumed from the SAME scope RebateModule contributes to?
          ├─ NO  → Part 2: align scopes (Option A or B)
          └─ YES → check availableSections filter (e.g. the screen filters MORE and your
                    FeatureActions has availableSections = setOf(MORE)) and featureKey handling
```

---

# Diagnostic checklist — 8 commands (run from the repo root)

Run these **in order** and stop at the first one that's wrong. Each command is self-contained.
Values below are filled in for the reported case: **variant `syscoDebug`**, store `sysco`, feature `rebate`.
Adjust `VARIANT` / `STORE` / `FEATURE` / `APP` / `WIRING` for other cases.

```bash
# Set once per shell, then run the numbered commands below
VARIANT=syscoDebug          # the build variant you're running
STORE=sysco                 # the store/flavor part of that variant
FEATURE=rebate              # the feature you expect to see
APP=:app                    # application module path
WIRING=:shared-wiring       # the module that DECLARES AndroidAppGraph (@DependencyGraph)
```

## Copy-paste ALL 8 at once

Paste this whole block into a terminal at the repo root. It sets the variables and runs every check
with labelled headers, so you can copy the entire output back in one go.

```bash
VARIANT=syscoDebug; STORE=sysco; FEATURE=rebate; APP=:app; WIRING=:shared-wiring

echo "##### 1. is '$FEATURE' in store '$STORE'? #####"
grep -Rn "\"$STORE\"" --include=*.kt . | grep "to listOf"

echo "##### 2. is feature-$FEATURE-real on the GRAPH ($WIRING) classpath? #####"
./gradlew $WIRING:dependencies --configuration ${VARIANT}RuntimeClasspath 2>/dev/null | grep -i "feature-$FEATURE" || echo "(none)"

echo "##### 3. is feature-$FEATURE-real linked into the APP ($APP) for $VARIANT? #####"
./gradlew $APP:dependencies --configuration ${VARIANT}RuntimeClasspath 2>/dev/null | grep -i "feature-$FEATURE" || echo "(none)"

echo "##### 4. are the feature-$FEATURE modules registered? #####"
./gradlew projects 2>/dev/null | grep -i "feature-$FEATURE" || echo "(none)"

echo "##### 5. what store/features does shared resolve? (needs the logger line in shared/build.gradle.kts) #####"
./gradlew $WIRING:help 2>&1 | grep "\[shared\]" || echo "(no logger output)"
./gradlew $WIRING:help -Penvironment=$STORE 2>&1 | grep "\[shared\]" || echo "(no logger output)"

echo "##### 6. where is availableFeatures consumed? #####"
grep -rn "availableFeatures" --include=*.kt . || echo "(none)"

echo "##### 7. graph wiring / CustomerGraph relationship #####"
grep -rn "createGraph\|AndroidAppGraph\|CustomerGraph\|customerGraphFactory" --include=*.kt . || echo "(none)"

echo "##### 8. what scope does the $FEATURE module contribute to? #####"
grep -rn "ContributesTo" --include=*.kt . | grep -i "$FEATURE" || echo "(none)"

echo "##### DONE #####"
```

---

## The same checks, one at a time (with explanations)

### 1. Is the feature even in the store you're building? (root-cause check)
```bash
grep -Rn "\"$STORE\"" --include=*.kt . | grep "to listOf"
```
The `listOf(...)` for `$STORE` **must contain `$FEATURE`**. If it doesn't, it can *never* appear on this
variant — that's the whole problem. → fix: add `$FEATURE` to that store in `StoreManifest`, or build the
store that has it. **(For `syscoDebug` this is empty for `rebate` → stop here.)**

### 2. Is `feature-<FEATURE>-real` on the GRAPH module's classpath? (the key check)
```bash
./gradlew $WIRING:dependencies --configuration ${VARIANT}RuntimeClasspath | grep -i "feature-$FEATURE"
```
Prints nothing → the module isn't visible where `AndroidAppGraph` is compiled, so Metro **cannot merge**
its `@IntoSet` → the set is empty. (Empty here is expected whenever #1 is empty.)

### 3. Is `feature-<FEATURE>-real` linked into the APP for this variant?
```bash
./gradlew $APP:dependencies --configuration ${VARIANT}RuntimeClasspath | grep -i "feature-$FEATURE"
```
Prints but **#2 doesn't** → the "linked for flavor ≠ merged into the graph" mismatch (app links it, but
the graph in `$WIRING` doesn't). Feed the feature to `$WIRING` (via `shared`'s `environment` loop).

### 4. Are the feature modules registered at all?
```bash
./gradlew projects | grep -i "feature-$FEATURE"
```
Prints nothing → not `include(...)`d in `settings.gradle.kts` (or wrong dash names).

### 5. What store/features does `shared` resolve?
First add this line to `shared/build.gradle.kts` after `storeFeatures` is computed:
```kotlin
logger.lifecycle("[shared] store='$store' features=$storeFeatures")
```
then:
```bash
./gradlew $WIRING:help | grep "\[shared\]"                        # default (no -P)
./gradlew $WIRING:help -Penvironment=$STORE | grep "\[shared\]"   # explicit store
```
Shows the exact store + feature list `shared` linked. `store='sysco' features=[order]` confirms rebate
is excluded.

### 6. Where is `availableFeatures` consumed (which scope/graph)?
```bash
grep -rn "availableFeatures" --include=*.kt .
```
The injection point's graph/scope must match the scope `RebateModule` contributes to.

### 7. Graph wiring + `CustomerGraph` relationship
```bash
grep -rn "createGraph\|AndroidAppGraph\|CustomerGraph\|customerGraphFactory" --include=*.kt .
```
Confirms whether `CustomerGraph` is a child of `AndroidAppGraph` (child can see parent AppScope bindings).

### 8. What scope is `RebateModule` contributing to right now?
```bash
grep -rn "ContributesTo" --include=*.kt . | grep -i "$FEATURE"
```
Confirms the `@ContributesTo(...)` scope actually matches where `availableFeatures` is declared.

---

### Reading the results

| Command | If empty / wrong → |
|---|---|
| **1** `listOf` for `sysco` has no `rebate` | Root cause: feature not in this store. Add it to `StoreManifest["sysco"]` **or** build `newportDebug`. |
| **2** no `feature-rebate` on `$WIRING` | Not on the graph's classpath → can't be merged. Fix linkage/store. |
| **3** present but **2** empty | App links it, graph doesn't → feed it to `shared-wiring` via `shared`'s loop. |
| **4** nothing | Module not `include`d in `settings.gradle.kts`. |
| **5** `features=[order]` | `shared` resolved sysco → rebate excluded; switch store / pass `-Penvironment`. |
| **6/7** consumer scope ≠ contribution scope | Part 2 scope mismatch — align scopes. |
| **8** scope ≠ graph's scope | Move `@ContributesTo` to the graph's scope (e.g. `AppScope`). |

**Bottom line for `syscoDebug`:** commands **1 and 2 are empty for `rebate`** because **rebate isn't in the
`sysco` store** (it's newport-only). Fix = add `rebate` to `"sysco"` in `StoreManifest`, or build
`newportDebug`. The scope/Metro details (6–8) only matter once 1–4 are green.
