# Metro DI from absolute zero — a very beginner's guide

This teaches **dependency injection (DI)** and the **Metro** library from scratch, one tiny step at a
time, using this project's real classes. When you finish, read [`METRO_DI.md`](METRO_DI.md) for the
faster reference and the multibinding/self-registration details.

No prior DI knowledge assumed.

---

## 1. What problem are we even solving?

A program is lots of small objects that need each other. For example, our cart needs two helpers:

```kotlin
class RealCartRepository(
    private val accessControl: AccessControl,   // needs this
    private val sessionManager: SessionManager, // and this
) : CartRepository { … }
```

So to **create** a cart, you first need an `AccessControl` and a `SessionManager`. And those might
need *their* own helpers, and so on. Someone has to build this whole chain in the right order.

### The painful way (build it yourself everywhere)
```kotlin
val sessionManager = RealSessionManager()
val accessControl  = RealAccessControl()
val cart           = RealCartRepository(accessControl, sessionManager)
```
Problems:
- Every place that needs a cart must know **how** to build one (and its helpers).
- If `RealCartRepository` later needs a *third* helper, you edit **every** such place.
- Keeping a **single shared** cart (so the whole app sees the same items) is fiddly.

### The DI way (someone else builds it, hands it to you)
You just say *"I want a `CartRepository`"* and receive a ready-made one. You never write the
`RealCartRepository(...)` line yourself.

That "someone else" is the **graph**. **Metro** is the tool that writes the graph for you.

> **Mental model:** Metro is an **automatic factory**. You *label* the parts it's allowed to build,
> and you *declare* what you want. Metro figures out the assembly order and generates all the wiring
> code — at **compile time** (it's a Kotlin compiler plugin, so errors show up when you build, not at
> runtime).

---

## 2. Annotation #1: `@Inject` — "Metro, you may build this"

Put `@Inject` on a class and Metro is allowed to construct it. Its **constructor parameters are the
ingredients** — Metro will supply each one automatically.

```kotlin
@Inject
class RealCartRepository(
    private val accessControl: AccessControl,
    private val sessionManager: SessionManager,
) : CartRepository
```

You're telling Metro: *"To build a cart, you need an `AccessControl` and a `SessionManager` — go get
them yourself."* You never call this constructor; Metro does.

> Ingredients are found **recursively**. To build the cart, Metro builds an `AccessControl` first; to
> build that, it builds *its* ingredients; and so on, until it bottoms out.

---

## 3. Annotation #2: `@ContributesBinding` — "use me when someone asks for the interface"

Notice the cart needs an **`AccessControl`** — but `AccessControl` is just an *interface* (a contract,
no code). Metro can't build an interface. It needs to know **which concrete class** to use.

That's what `@ContributesBinding` says. On the real implementation:

```kotlin
@Inject
@ContributesBinding(AppScope::class)   // "when someone needs AccessControl, use ME"
class RealAccessControl : AccessControl { … }
```

Now whenever anything (like our cart) asks for `AccessControl`, Metro supplies a `RealAccessControl`.

> **Why bother with interfaces?** So callers depend on the *contract*, not the implementation. The
> cart doesn't know or care that it's `RealAccessControl` — it could be a fake one in a test. This is
> the project's `api` / `real` split: the interface lives in `:api`, the `@ContributesBinding` class
> in `:real`.

---

## 4. Annotation #3: `@SingleIn` — "keep just one of these"

By default Metro could build a new object every time. Often you want **one shared instance** — e.g.
ONE cart, so every screen sees the same items. Add `@SingleIn`:

```kotlin
@Inject
@SingleIn(AppScope::class)              // one instance, shared
@ContributesBinding(AppScope::class)
class RealCartRepository(…) : CartRepository
```

Now there is a single cart for the whole app. (Without this, two screens might accidentally get two
different carts.)

---

## 5. What is `AppScope`?

`@SingleIn(AppScope::class)` and `@ContributesBinding(AppScope::class)` keep mentioning **`AppScope`**.
It's just a **label for a lifetime**:

```kotlin
abstract class AppScope private constructor()   // never created — just a name
```

"`AppScope`" means *"lives as long as the app."* This project uses one scope for everything. Think of
it as the name tag on the box that holds all the singletons.

---

## 6. Annotation #4: `@DependencyGraph` — the menu you order from

So far we've **labelled** parts. Now we need to **declare what we want out**. That's the graph: an
**interface** listing what you can pull out. You write only the interface; Metro generates the class
that actually builds everything.

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val sessionManager: SessionManager   // "I can hand you a SessionManager"
    val accessControl: AccessControl     // "…and an AccessControl"
    val loginValidator: LoginValidator
    // …
}

fun createAppGraph(): AppGraph = createGraph<AppGraph>()   // build the factory
```

- `@DependencyGraph(AppScope::class)` → "this is the graph for the `AppScope` lifetime."
- Each `val x: T` is a **menu item**: "you can ask me for a `T`."
- `createGraph<AppGraph>()` → Metro's generated implementation. **You never write the body** — the
  compiler does, wiring everything from the `@Inject` / `@ContributesBinding` labels.

In `App.kt`:
```kotlin
val graph = remember { createAppGraph() }   // build the factory once, at startup
val validator = graph.loginValidator        // pull a ready-made object out
```
You never wrote *how* to build `loginValidator` — Metro saw the labels and figured it out.

---

## 7. Putting it together: how one `RealCartRepository` gets born

When the app needs the cart (a feature contributes it — see §8), Metro does this **for you**:

```
You ask for: CartRepository
   │
   ├─ @ContributesBinding says: that means RealCartRepository
   │
   ├─ RealCartRepository's constructor needs: AccessControl, SessionManager
   │       │
   │       ├─ AccessControl  → @ContributesBinding → RealAccessControl  (built, @SingleIn cached)
   │       └─ SessionManager → @ContributesBinding → RealSessionManager (built, @SingleIn cached)
   │
   └─ Metro calls RealCartRepository(accessControl, sessionManager) and caches it (@SingleIn)
```

You wrote **none** of those construction lines. You only put labels on classes and listed what you
wanted. That's the whole point: **describe, don't assemble.**

---

## 8. How features add themselves (multibindings, the short version)

One menu item gives you **one** object. But the app's bottom tabs come from **many** features, each in
its own module. Metro supports a "many contributors, one collection" pattern called a **multibinding**.

The graph declares a *set*:
```kotlin
@Multibinds(allowEmpty = true) val tabs: Set<Tab>   // many features fill this
```

Each feature adds one element from its own module:
```kotlin
@ContributesTo(AppScope::class)    // "merge my stuff into the graph"
@BindingContainer                  // "I hold @Provides recipes"
object CartContribution {
    @Provides @IntoSet             // "put what I return INTO the Set<Tab>"
    fun cartTab(): Tab = Tab(CartKey, TabMeta("Cart", "🛒", order = 10))
}
```
Because a build only links the feature modules it ships, only those contribute — so the tab set
composes itself per build with no central edits. (Full details in [`METRO_DI.md`](METRO_DI.md) §3.)

---

## 9. The four "verbs" you'll use 90% of the time

| You want to… | Write | Example in this repo |
|---|---|---|
| let Metro build a class | `@Inject` on it | `RealCartRepository` |
| keep one shared instance | add `@SingleIn(AppScope::class)` | every repository |
| say "use this class for that interface" | `@ContributesBinding(AppScope::class)` | `RealAccessControl : AccessControl` |
| get something out | add `val x: T` to `AppGraph` | `val accessControl` |

And for feature self-registration: `@ContributesTo` + `@BindingContainer` + `@Provides @IntoSet`.

---

## 10. Try it yourself: add a new injected service

1. **Contract** (`:api`):
   ```kotlin
   interface Clock { fun now(): Long }
   ```
2. **Implementation** (`:real`):
   ```kotlin
   @Inject
   @SingleIn(AppScope::class)
   @ContributesBinding(AppScope::class)
   class RealClock : Clock {
       override fun now() = /* … */ 0L
   }
   ```
3. **Use it** — just declare it as a constructor parameter anywhere Metro builds the class:
   ```kotlin
   @Inject
   class RealCartRepository(
       private val clock: Clock,           // ← Metro now supplies this automatically
       private val accessControl: AccessControl,
       private val sessionManager: SessionManager,
   ) : CartRepository
   ```
   You added one parameter; you did **not** edit any `RealCartRepository(...)` call site, because there
   are none — Metro builds it. That's the payoff.

---

## 11. One-paragraph recap

You **label** classes Metro may build (`@Inject`), say which class implements each interface
(`@ContributesBinding`), mark the ones that should be shared (`@SingleIn`), and **list what you want**
in a graph interface (`@DependencyGraph`). At compile time Metro writes all the construction code and
caches the singletons. You describe the parts; Metro assembles the machine.

> Next: [`METRO_DI.md`](METRO_DI.md) for multibindings, `@Provides` recipes, and how this powers
> per-store self-registration.
