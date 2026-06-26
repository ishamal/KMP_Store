# Metro scopes — custom scopes & a session scope (beginner guide)

A follow-up to [`METRO_DI_FROM_ZERO.md`](METRO_DI_FROM_ZERO.md). It answers: *can I define my own
scope (like a per-login "session" scope), and how?* Short answer: **yes** — with a custom **scope
marker** plus a **graph extension** (a child graph). This doc explains both, from scratch.

> Today this project uses **one** scope, `AppScope`, for everything. Nothing below is wired in yet —
> it's the recipe for adding a second, shorter-lived scope when you need one.

---

## 1. First: what *is* a scope, really?

A **scope** is just a **lifetime label**. In this repo:

```kotlin
abstract class AppScope private constructor()   // a marker — never instantiated
```

`@SingleIn(AppScope::class)` means *"one instance that lives as long as the app graph."* `AppScope`
isn't special — it's an ordinary class used as a name tag. **So you can define your own** the same way.

### Why you'd want a second scope
Some objects shouldn't live for the whole app — they belong to a **login session** and should be
**thrown away on logout** (and rebuilt on the next login). Examples here:
- the current `Session` (business unit + role),
- the `AccessGuard`,
- maybe a cart that should reset when the user logs out.

Putting those in `AppScope` makes them live forever (stale after logout). A **session scope** gives
them the right lifetime.

---

## 2. Step 1 — define a custom scope marker

Exactly like `AppScope`:

```kotlin
// core/.../SessionScope.kt
abstract class SessionScope private constructor()
```

That's it — a scope is "defined" just by declaring a marker class. Now `@SingleIn(SessionScope::class)`
means "one instance **per session**."

---

## 3. Step 2 — a child graph for that scope (`@GraphExtension`)

A scope needs a **graph** that owns its lifetime. For a shorter-lived scope you use a **graph
extension** — a child graph that can see everything in its parent (`AppGraph`) **plus** its own
session-scoped bindings.

```kotlin
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(SessionScope::class)
interface SessionGraph {
    // session-scoped things you can pull out:
    val accessGuard: AccessGuard

    // How to build a SessionGraph. The Session is INPUT (provided when we create the child).
    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides session: Session): SessionGraph
    }
}
```

- `@GraphExtension(SessionScope::class)` → "child graph for the `SessionScope` lifetime."
- It automatically **extends the parent**: anything in `AppGraph` (e.g. `AccessControl`) is available
  to bindings here, but session-scoped objects are **not** visible to the app scope (correct: the
  parent shouldn't see child things).
- `@GraphExtension.Factory` with `create(@Provides session: Session)` → the factory that **builds a new
  child**, taking the live `Session` as input. `@Provides` makes that `session` injectable inside the
  child (so anything in `SessionScope` can depend on `Session`).

Now a session-scoped binding can depend on the session:
```kotlin
@Inject
@SingleIn(SessionScope::class)
class AccessGuard(
    private val accessControl: AccessControl,   // from the PARENT (AppScope)
    private val session: Session,               // provided into the child
)
```

---

## 4. Step 3 — let the parent create children

The parent graph exposes the child factory so the app shell can make/destroy sessions:

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val accessControl: AccessControl
    val sessionGraphFactory: SessionGraph.Factory   // ← parent hands out the child factory
}
```

Then in the app shell, at login/logout:

```kotlin
val appGraph = createAppGraph()                    // lives for the whole app

// on login — build a session child from the Session we just got:
val sessionGraph = appGraph.sessionGraphFactory.create(session)
val guard = sessionGraph.accessGuard               // session-scoped

// on logout — just drop the reference:
// sessionGraph = null  → everything @SingleIn(SessionScope) is garbage-collected.
```

**That's the whole point:** logging out drops the child graph, so every session-scoped object dies and
the next login builds fresh ones. App-scoped singletons (in `AppGraph`) are untouched.

---

## 5. Variant — let a feature *contribute* the child graph (modular)

This section is an **optional convenience**. It does the same thing as §4, just without editing
`AppGraph`. Here's the difference, slowly.

### The "problem" it solves
In §4, **`AppGraph` had to name the child itself**:
```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val sessionGraphFactory: SessionGraph.Factory   // ← you hand-wrote this line
}
```
That's fine when `SessionGraph` lives right next to `AppGraph`. But if `SessionGraph` lives in some
**feature module**, you'd have to edit the core `AppGraph` every time — the very thing this project
avoids elsewhere (features add tabs without editing `App.kt`). We'd like the child graph to **attach
itself**.

### The fix: `@ContributesTo` on the factory
Put `@ContributesTo(AppScope::class)` on the **`Factory`**:
```kotlin
@GraphExtension(SessionScope::class)
interface SessionGraph {
    val accessGuard: AccessGuard

    @ContributesTo(AppScope::class)        // "Metro: attach me to the AppScope graph automatically"
    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides session: Session): SessionGraph
    }
}
```

**What Metro does with that:** at compile time it makes the **generated `AppGraph` implementation also
implement `SessionGraph.Factory`** — even though your `AppGraph` *interface* never mentions it. The
factory is now "inside" the app graph; it's just not on the menu you wrote by hand.

### Getting it out: `asContribution<>()`
Because your hand-written `AppGraph` interface has **no** `sessionGraphFactory` property, you can't
write `appGraph.sessionGraphFactory`. So you ask the graph for the contributed type instead:
```kotlin
val factory = appGraph.asContribution<SessionGraph.Factory>()   // "give me the factory that got contributed"
val sessionGraph = factory.create(session)                      // then use it exactly like §4
```
`asContribution<T>()` just means *"this graph also implements `T` (because something contributed it) —
give me that view of it."* After that line, everything is identical to §4.

### §4 vs §5 side by side
| | §4 (explicit) | §5 (contributed) |
|---|---|---|
| Who puts the child in `AppGraph`? | **you** add `val …Factory` | **`@ContributesTo`** does it |
| Edit core `AppGraph` when adding a child? | yes | **no** |
| Get the factory | `appGraph.sessionGraphFactory` | `appGraph.asContribution<SessionGraph.Factory>()` |
| Best when… | child defined beside `AppGraph` | child defined in a separate **feature module** |

### The one sentence to remember
You already saw `@ContributesBinding` add **one object** to the graph without editing it. `@ContributesTo`
on a graph-extension factory does the same for a **whole child graph**: it plugs itself in, and you fetch
it with `asContribution<>()` instead of a hand-written accessor. **If this feels like overkill, just use
§4** — they produce the same result.

---

## 6. Passing values into a graph (`@Provides` vs `@Includes`)

Children (and root graphs) often need **input values** known only at creation time (a `Session`, a
user id…). Two ways, via a factory:

```kotlin
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Includes

@DependencyGraph(AppScope::class)
interface AppGraph {
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides userId: String,        // becomes injectable inside the graph
            @Includes config: ConfigGraph,   // pulls bindings FROM another graph (input-only)
        ): AppGraph
    }
}

// build it with the factory instead of createGraph<>():
val graph = createGraphFactory<AppGraph.Factory>().create("user-123", configGraph)
```

- `@Provides x` → `x` is now a binding **inside** the graph (anything can inject it).
- `@Includes g` → makes another graph's bindings available here, but `g`'s params are **input-only**
  (not injectable back out).
- `createGraphFactory<…>()` is the factory equivalent of `createGraph<…>()` (which this project uses
  for the no-input `AppGraph`).

---

## 7. When to add a scope (rule of thumb)

| Situation | Use |
|---|---|
| Lives for the whole app (repositories, `AccessControl`, `SessionManager`) | `AppScope` (what we have) |
| Lives only while logged in; dies on logout (`Session`, `AccessGuard`, per-login caches) | a **`SessionScope`** child graph |
| Lives for one screen/flow | another child graph (e.g. `ScreenScope`) |

Don't add scopes you don't need — each is a child graph to manage. A second scope earns its keep when
some objects have a **genuinely shorter lifetime** than the app.

---

## 8. Worked example in this project: `CustomerScope` (resets every login)

This project actually ships a custom scope — **`CustomerScope`**, for the logged-in customer session.
It's the concrete version of everything above; the rebate ViewModel lives in it.

### How the scope is defined
`core/di/api/.../CustomerScope.kt` — just a marker class, exactly like `AppScope`:
```kotlin
abstract class CustomerScope private constructor()
```

### The child graph (`androidApp/.../di/CustomerGraph.kt`)
```kotlin
@GraphExtension(CustomerScope::class)
interface CustomerGraph : ViewModelGraph {   // ViewModelGraph → a customer-scoped ViewModel map + factory
    @GraphExtension.Factory
    interface Factory { fun create(): CustomerGraph }
}
```
The parent (`AppGraph`) hands out the factory:
```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    val customerGraphFactory: CustomerGraph.Factory
}
```

### What lives in the scope
The rebate ViewModel is contributed to `CustomerScope` (not `AppScope`), so it lands in the **customer**
graph's ViewModel map (served by a `CustomerViewModelFactory` bound with `@ContributesBinding(CustomerScope::class)`):
```kotlin
@Inject
@ContributesIntoMap(CustomerScope::class, binding = binding<ViewModel>())
@ViewModelKey(RebateViewModel::class)
internal class RebateViewModel(private val rebateClient: RebateClient) : MoleculeViewModel<…>()
```

### ⭐ How it "resets every login" — the key line
In `App.kt` the child graph is built with **`remember(session)`**:
```kotlin
val session = graph.sessionManager.session
if (session == null) {
    LoginScreen(...)                                 // logged out — no customer graph exists
} else {
    val customerGraph = remember(session) {          // ← keyed on the session
        graph.customerGraphFactory.create()
    }
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides customerGraph.metroViewModelFactory,
    ) { MainScaffold(...) }
}
```
`remember(session)` **is** the reset. Its key is the `session`:
- **Log out** → `session` becomes `null` → the `else` branch leaves composition → `customerGraph` is
  forgotten and garbage-collected → **every `@SingleIn(CustomerScope)` object dies** (the rebate VM and
  its loaded `RebateSummary`, etc.).
- **Log in again** → a new `session` → `remember(session)` sees a **new key** → calls `create()` again →
  a **brand-new customer graph** with **fresh** session-scoped objects.

```
login(userA) → customerGraph A   (rebate VM A, summary A)
logout        → graph A dropped → A's session-scoped objects GC'd
login(userB) → customerGraph B   (rebate VM B, summary B)   ← fresh, nothing from A
```

App-scoped singletons in `AppGraph` (repositories, `AccessControl`, `SessionManager`) are untouched
across logins — only the customer graph resets.

> **Why a fresh login always resets:** `Session` is a `data class`, so each successful login builds a
> *new* `Session` instance → a different `remember` key → a new customer graph. (If you ever reused the
> exact same `Session` object, add something unique like a login timestamp to force the reset.)

---

## 9. Recap

1. A scope is just a **marker class** (`abstract class SessionScope private constructor()`).
2. Give it a **child graph** with `@GraphExtension(SessionScope::class)` — it sees the parent's
   bindings plus its own `@SingleIn(SessionScope::class)` ones.
3. Build children from the parent via a `@GraphExtension.Factory`, passing live inputs with
   `@Provides`. **Dropping the child graph = ending that scope.**
4. Use `@ContributesTo` on the factory for modular, hand-wiring-free extensions, and `@Provides` /
   `@Includes` to feed values in.

> Annotations are all in `dev.zacsweers.metro`. See [`METRO_DI.md`](METRO_DI.md) for multibindings and
> the [official Metro docs](https://zacsweers.github.io/metro/latest/dependency-graphs/) for the full
> reference.
