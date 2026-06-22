# Dependency Injection with Metro — a beginner's guide

> Navigation update: features now self-register a Navigation 3 NavDestination (NavKey + TabMeta + an entry{} installer); the old AppTab and Slot abstractions were removed. See ADDING_STORES_AND_FEATURES.md for the current pattern.

How the app wires its objects together, explained from scratch using the project's real classes.
Metro ([dev.zacsweers.metro](https://github.com/ZacSweers/metro)) is a **compile-time** dependency
injection (DI) framework — a Kotlin compiler plugin, so there's no reflection and mistakes are
caught when you build.

See also: [`ARCHITECTURE.md`](ARCHITECTURE.md) (the big picture) and
[`STORES_AND_FEATURES.md`](STORES_AND_FEATURES.md) (how this powers self-registration).

---

## 1. The problem DI solves

**Without** DI, objects build their own dependencies:
```kotlin
@Composable fun CartScreen() {
    val repository = remember { CartRepository() }   // the screen builds its own repo
}
```
Every call site hardcodes *how* to build things; if `CartRepository` later needs a database you edit
them all; sharing one instance and testing are awkward.

**With** DI, something central builds objects and hands them in:
```kotlin
@Composable fun CartScreen(repository: CartRepository)   // repo is given to it
```
That "something central" is the **graph**. Metro is the tool that generates the graph for you.

> Mental model: Metro is an **automatic factory**. You *describe what exists* with annotations and
> *declare what you want* with an interface; Metro *writes the wiring code* ("build X, which needs Y,
> which needs Z…") at compile time.

---

## 2. The vocabulary (as used in this project)

### `@Inject` — "Metro may build this"
```kotlin
@Inject @SingleIn(AppScope::class)
class CartRepository
```
- `@Inject` → Metro is allowed to construct it (via its constructor; constructor args are supplied
  recursively).
- `@SingleIn(AppScope::class)` → make **one** instance for the app's lifetime (a singleton).

### `AppScope` — a lifetime label (`core/.../AppScope.kt`)
```kotlin
abstract class AppScope private constructor()
```
Just a **marker** (never instantiated). It names a lifetime: things scoped to `AppScope` live as
long as the app graph. The whole app uses this one scope.

### `@DependencyGraph` — the menu + the factory (`androidApp/.../di/AppGraph.kt`)
```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val loginValidator: LoginValidator
    val sessionManager: SessionManager
    @Multibinds(allowEmpty = true) val tabs: Set<AppTab>
}
fun createAppGraph(): AppGraph = createGraph<AppGraph>()
```
You write only the **interface** (a menu: "you can get these out of me"). Metro **generates** the
implementing class at compile time, wiring everything from the `@Inject` constructors.
`createGraph<AppGraph>()` returns an instance of that generated class. In `App.kt`:
```kotlin
val graph = remember { createAppGraph() }          // build the factory once
LoginScreen(validator = graph.loginValidator, …)   // pull a dependency out
```
You never wrote "how to build `LoginValidator`" — Metro saw `@Inject` on it and figured it out.

---

## 3. Multibindings — how features plug in

A normal accessor returns **one** object. A **multibinding** lets **many modules each add into one
collection**. That's how the tab list is assembled.

The graph asks for a *set*:
```kotlin
@Multibinds(allowEmpty = true) val tabs: Set<AppTab>   // many features fill this
```
`@Multibinds(allowEmpty = true)` declares the multibinding and says "it's OK if no one contributes"
(e.g. a store with no `Slot`).

Each feature contributes **one element**, from its own module
(`features/cart/.../CartContribution.kt`):
```kotlin
@ContributesTo(AppScope::class)   // "merge me into the AppScope graph"
@BindingContainer                 // "I hold @Provides recipes"
object CartContribution {
    @Provides @IntoSet            // "put what I return INTO the Set<AppTab>"
    fun cartTab(repository: CartRepository): AppTab =
        AppTab("Cart", "🛒", order = 10) { _, _ -> CartScreen(repository = repository) }
}
```
- `@ContributesTo(AppScope::class)` — Metro **scans all modules on the classpath** for these and
  merges them into the graph.
- `@BindingContainer` — marks the object as holding `@Provides`.
- `@Provides` — a **recipe** for building something Metro can't just `@Inject`-construct (here an
  `AppTab` that wraps a Compose lambda).
- `@IntoSet` — add the result to the `Set<AppTab>` instead of exposing it standalone.
- `repository: CartRepository` — Metro supplies it automatically (it's `@Inject`).

The same idea targets *inside* a screen via a generic `Slot`: `rebate` contributes a
`Slot(id = SlotId.SETTINGS) { … }` (not an `AppTab`) into `Set<Slot>`, and the Settings screen
renders matching ones with `SlotHost(slots, SlotId.SETTINGS)`. The **return type** (`AppTab` vs
`Slot`) routes a contribution to the right place; the `SlotId` picks the host screen.

---

## 4. Why this gives "no per-store code"

> Metro aggregates contributions **from whatever modules are on the classpath**.

A product flavor (storeA/B/C) links only its own feature modules (per `StoreManifest`). So when
Metro builds `AppGraph`:
- **storeB** links `cart` + `settings` → only their tabs land in `Set<AppTab>`.
- `invoices`/`orders` aren't linked → their contributions don't exist → those tabs simply aren't there.

Same `AppGraph` interface, same `App.kt` — the **set differs per store automatically**. That's why
there are no `androidApp/src/storeX` source sets.

---

## 5. End-to-end trace

```
1. App() → createAppGraph()
      └─ Metro's generated AppGraph implementation is instantiated.
2. graph.loginValidator
      └─ Metro returns the @Inject-built LoginValidator (singleton).
3. login → graph.sessionManager.session = Session(email, experience)
      └─ sessionManager is the one @SingleIn instance; everyone shares it.
4. graph.tabs
      └─ Metro builds Set<AppTab> by running every @ContributesTo @Provides @IntoSet
         found on THIS flavor's classpath, auto-supplying each repository:
           CartContribution.cartTab(cartRepository)                       → AppTab("Cart")
           OrdersContribution.ordersTab(orderRepository)                  → AppTab("Orders")   (if linked)
           InvoicesContribution.invoicesTab(invoiceRepo, sessionManager)  → AppTab("Invoices")
           SettingsContribution.settingsTab(settingsRepo, Set<Slot>) → AppTab("Settings")
5. App sorts by order, filters by experience capabilities, renders the tabs.
```

---

## 6. One-line summary

> You **annotate** what exists (`@Inject`, `@Provides @IntoSet`, `@ContributesTo`) and **declare**
> what you want (the `AppGraph` interface). Metro **generates the wiring** at compile time, and the
> build's linked modules decide which contributions exist — so the graph composes itself per store.

## 7. Annotation cheat-sheet

| Annotation | Means | Example here |
|---|---|---|
| `@Inject` | Metro may construct this class | `CartRepository`, `LoginValidator` |
| `@SingleIn(AppScope::class)` | one instance for the app's life | all repositories, `SessionManager` |
| `@DependencyGraph(AppScope::class)` | the graph interface Metro implements | `AppGraph` |
| `createGraph<T>()` | build the graph instance | `createAppGraph()` |
| `@ContributesTo(AppScope::class)` | merge this module's bindings into the graph | every `*Contribution` |
| `@BindingContainer` | object holds `@Provides` recipes | every `*Contribution` |
| `@Provides` | a recipe for building something | `cartTab(...)` |
| `@IntoSet` | add the result into a `Set<…>` multibinding | tabs, settings sections |
| `@Multibinds(allowEmpty = true)` | declare a (possibly empty) multibinding | `tabs`, `slots` |
