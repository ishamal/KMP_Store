# Per-app shared-wiring — explained from zero

This explains the architecture decision behind a review comment: **why each app gets its own
"shared-wiring" module instead of one module wiring every app.** Beginner-friendly — it builds up
every idea from scratch. No prior DI knowledge assumed.

---

## 0. The one sentence version

> Instead of **one** module that plugs everything together for **all** the apps (and hopes they don't
> interfere), **each app gets its own** plug-it-together module. They all agree on the *shapes* of the
> parts (the `api`), but each app supplies its *own actual parts* (the `real`).

The rest of this doc unpacks that sentence.

---

## 1. Two kinds of module: `api` and `real`

Every feature/core area in this project is split into two modules:

| Module | Contains | Analogy |
|---|---|---|
| `:something:api` | **interfaces** + plain data types. *What* something does, not how. | The **shape of a wall socket** — everyone agrees on it. |
| `:something:real` | the **actual implementation**. *How* it does it. | The **appliance** you plug into that socket. |

Example: `Analytics` is an interface in `:api` (`fun track(event)`). `RealAnalytics` is the class in
`:real` that actually sends the event somewhere.

> **Why split them?** Code all over the app can depend on the *socket* (`api`) without caring which
> *appliance* (`real`) is plugged in. You can swap the appliance without rewiring the whole house.

---

## 2. "Common" vs "platform" code

This is a **Kotlin Multiplatform** project — the same code can run on Android **and** iOS. So each
module has source folders:

- `commonMain/` — code that works on **both** Android and iOS.
- `androidMain/` — code that only works on **Android** (can use Android/Compose types).
- `iosMain/` — code that only works on **iOS**.

> **Rule of thumb:** if something is in `androidMain`, iOS **cannot see it**. To share a thing with
> both platforms, it has to live in `commonMain` — and that means it can't mention any Android-only
> type.

---

## 3. What "wiring" means (dependency injection)

Your app is made of lots of small parts that need each other: the login screen needs an
`AuthService`, which needs a `Network`, which needs `Config`, and so on. **Somebody has to connect
all those parts together.** That "connecting" is called **dependency injection (DI)**. This project
uses a DI tool called **Metro**.

Think of it like assembling flat-pack furniture: DI is the step where all the pieces get bolted
together into a finished product. In Metro, the finished product is called a **graph** (declared with
`@DependencyGraph`) — a big object that knows how to build every part and hand them out.

The **"wiring module"** is simply the module that declares that graph and says *which real parts go
into it*.

---

## 4. The problem: one wiring module for every app

Right now there's basically **one** wiring module (`:shared`) that tries to build the graph for
whatever app/store is being built, using **if-statements**:

```kotlin
// pseudo-code of today's approach
if (store == "storeA") useTheseParts()
else                   useThoseParts()
```

Here's the danger. In Metro, a real part can say "add me to the graph automatically":

```kotlin
@ContributesBinding(AppScope::class)   // "put me into ANY app graph"
class RealAnalytics(...) : Analytics
```

If **one** module builds the graph for **every** app, then a part meant only for App A can quietly
end up inside App B's graph too — because they're assembled from the same pile of parts. The
if-statements are *supposed* to keep them separate, but it's easy to get wrong, and when it goes
wrong you get a confusing bug where **one app's settings "leak" into another**.

> **Analogy:** one electrical panel wired to every apartment in the building. Flip a switch meant for
> apartment A and — oops — a light turns on in apartment B. The wires are too tangled together.

---

## 5. The fix: each app gets its own wiring module

Give **every app its own wiring module**, each building **its own** graph from **its own** chosen
parts:

```
App A's wiring module          App B's wiring module
  @DependencyGraph               @DependencyGraph
  uses A's real parts            uses B's real parts
  exports the shared api         exports the shared api
```

Now App B's graph can *only* see the parts App B's wiring module chose. There is **no shared pile**
for a stray part to leak out of. No if-statements branching on the store. Each app is isolated by
**structure**, not by careful convention.

> **Analogy:** give each apartment its own electrical panel. A switch in apartment A physically cannot
> affect apartment B.

The key line from the comment —

> *"each app shared-wiring module will export the same common api, but will implement their real
> implementations that are specific to them"*

— just means: **everybody agrees on the socket shapes (`api`), but each app plugs in its own
appliances (`real`).**

---

## 6. Why the apis must first become "common"

For every app to "export the **same** common api," those `api` modules must actually live in
`commonMain` (Section 2) — so Android *and* iOS can both use them.

The catch: an `api` is often *stuck* in `androidMain` because it secretly depends on an Android-only
type. You can't just drag the file into `commonMain` — it won't compile for iOS.

**Real example from this repo:** `FeatureAction` couldn't be common because it carried a
`NavKey` (an Android navigation type). Making it shareable meant first **removing** that Android type
and replacing it with a plain, platform-neutral one.

So *"migrate the remaining shopApi modules to being common, coreAnalyticsShopApi is the last one"*
means: there are a few `api` modules still holding an Android-only dependency, and finishing those
migrations is what unlocks the whole per-app plan. (That specific module name comes from a *reference
project* in `.claude/docs/`, not from this repo — it's naming the pattern, not a file you'll find here.)

Each migration is the same 3 steps:
1. Find the Android-only type leaking into the `api`.
2. Replace it with a common/plain type (push the Android bit down into `real`).
3. Move the now-neutral `api` into `commonMain`.

---

## 7. What's a "convention plugin"?

The comment ends with *"I'm working on a convention plugin for shared-wiring."*

A **convention plugin** is a reusable bundle of Gradle build settings. Instead of copy-pasting the
same 40 lines of build config into every app's wiring module (and getting them slightly different
each time), you write the setup **once** and each module just says "apply the shared-wiring
convention." Every app's wiring module then gets configured **identically and correctly**, for free.

> **Analogy:** a checklist template. Rather than each electrician wiring each panel from memory, they
> all follow the same printed checklist — so every panel comes out the same.

---

## 8. Before → after at a glance

| | Before (rejected) | After (target) |
|---|---|---|
| How many wiring modules | **one** for all apps | **one per app** |
| How apps stay separate | if-statements on store (fragile) | structure — separate graphs |
| Risk of config leaking between apps | real | eliminated |
| The `api` modules | some stuck in `androidMain` | all in `commonMain`, shared |
| The `real` modules | picked by conditionals | each app picks its own |
| Build config | hand-written per module | standardized by a convention plugin |

---

## 9. One-paragraph recap

Apps are built from small parts. Each part has a **shape** (`api`, an interface) and an **implementation**
(`real`). Today one module (`:shared`) wires the parts for every app using store if-statements, which
risks one app's parts leaking into another. The plan: give **each app its own wiring module** so each
builds an isolated graph from its own chosen `real` parts, while all apps **export the same common
`api`**. For that to work, the remaining `api` modules must move from `androidMain` into `commonMain`
(by removing their Android-only types first) — `coreAnalyticsShopApi` is thought to be the last one.
A **convention plugin** will make every app's wiring module configure itself the same way. Same app
behaviour, but apps are isolated by structure instead of by fragile conventions.
