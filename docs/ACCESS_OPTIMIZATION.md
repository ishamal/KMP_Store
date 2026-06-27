# Optimizing the access checks — what changed and why

This explains the optimization applied to the `ExperienceSnapshot` access system: **what was slow,
what we changed, and why it's faster**. Beginner-friendly; assumes you've seen `ExperienceSnapshot`,
`Feature`/`FeatureId`, and the `CapabilityGate` wrappers.

---

## 1. The model (one-line recap)

After login the backend resolves an **`ExperienceSnapshot`**:

```kotlin
data class ExperienceSnapshot(
    val experience: Experience,
    val businessUnit: BusinessUnit,
    val userRoles: UserRole,
    val resolvedFeatures: Set<Feature>,   // each Feature has a featureId + a Set<String> of capabilities
)
```

The whole app asks it two kinds of questions:
- **feature** questions — "is the ORDERS feature available?" (gates tabs)
- **capability** questions — "is `order.create` granted?" (gates in-screen functions)

---

## 2. The problem — every check re-did work

The checks recomputed their answer **on every call**. Two hot spots:

### a) `hasCapability(...)` re-flattened everything
```kotlin
// BEFORE  (extension in Capabilities.kt)
val ExperienceSnapshot.grantedCapabilities: Set<String>
    get() = resolvedFeatures.flatMapTo(mutableSetOf()) { it.capabilities }   // ← runs EVERY time

fun ExperienceSnapshot.hasCapability(capability: String) = capability in grantedCapabilities
```
`grantedCapabilities` is a `get()` (computed property) — so **each** `hasCapability` call walked every
feature and every capability and built a brand-new `Set`. One screen with 5 capability gates × each
recomposition = that flatten runs over and over.

### b) feature/capability lookups were linear scans
```kotlin
// BEFORE  (RealExperienceProvider)
override fun capabilitiesOf(featureId) =
    features.firstOrNull { it.featureId == featureId }?.capabilities.orEmpty()   // ← scans the list
override fun hasFeature(featureId) =
    features.any { it.featureId == featureId }                                   // ← scans the list
```
`features` also rebuilt a `List` on each access (`resolvedFeatures.toList()`), and tab filtering in
`App.kt` did `resolvedFeatures.any { it.featureId == id }` **per tab**.

> Why it matters: access checks are called in tight UI paths — every tab, every gated button, on every
> recomposition. Doing O(features × capabilities) work each time adds up and allocates garbage.

---

## 3. The fix — compute the indexes once, then O(1) lookups

The snapshot is **immutable** and built **once per login**. So we build the lookup structures once
(lazily, cached on the instance) and every query becomes a hash lookup.

```kotlin
// AFTER  (members on ExperienceSnapshot)
data class ExperienceSnapshot( … ) {

    // Built once per snapshot, cached. NOT part of equals/hashCode/copy (derived from the ctor data).
    private val featuresById: Map<FeatureId, Feature> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedFeatures.associateBy { it.featureId }
    }
    val grantedCapabilities: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedFeatures.flatMapTo(mutableSetOf()) { it.capabilities }
    }

    fun hasFeature(featureId: FeatureId)        = featureId in featuresById                  // O(1)
    fun capabilitiesOf(featureId: FeatureId)    = featuresById[featureId]?.capabilities.orEmpty() // O(1)
    fun hasCapability(capability: String)       = capability in grantedCapabilities          // O(1)
    fun hasCapability(featureId, capability)    = capability in capabilitiesOf(featureId)     // O(1)
}
```

### The three ideas
1. **`by lazy`** — the map and the flattened set are computed on **first access** and then **reused**.
   The expensive work happens once per login, not once per check.
2. **Hash lookups** — `featureId in featuresById` and `capability in grantedCapabilities` are O(1),
   replacing the linear scans / re-flattening.
3. **Lives on the snapshot** — the data and the queries are in one place, so every caller (Android,
   iOS, the provider) shares the same cached indexes.

> **Why `LazyThreadSafetyMode.PUBLICATION`?** The snapshot may be read from the UI thread and a
> coroutine/ViewModel. `PUBLICATION` is thread-safe **without locking** (in a rare race it just computes
> twice and keeps one result) — cheaper than the default `SYNCHRONIZED` for a value that's only read.

> **Why it's safe on a `data class`:** properties declared in the class *body* (not the primary
> constructor) are excluded from `equals`/`hashCode`/`copy`. So two snapshots with the same constructor
> data are still equal, and `copy()` works — the caches are pure derived state.

---

## 4. Everything else now delegates to the snapshot

- **`RealExperienceProvider`** stopped re-scanning and forwards to the cached methods:
  ```kotlin
  override fun hasFeature(id)        = snapshot?.hasFeature(id) ?: false
  override fun capabilitiesOf(id)    = snapshot?.capabilitiesOf(id).orEmpty()
  override fun hasCapability(id, c)  = snapshot?.hasCapability(id, c) ?: false
  ```
- **`App.kt`** tab filter: `id == null || snapshot.hasFeature(id)` (was a per-tab `any { … }` scan).
- **`CapabilityGate`** (Android Compose) and **`CapabilityGate`** (iOS SwiftUI) both call
  `snapshot.hasCapability(capability)` — the same O(1) member.
- Deleted **`Capabilities.kt`** — the extension functions became members, so there's a single home for
  the logic (no duplicated check code split between an extension file and the provider).

---

## 5. Before → after at a glance

| Check | Before (per call) | After |
|---|---|---|
| `hasCapability("order.create")` | flatten **all** features × capabilities into a new `Set` | `Set` membership — **O(1)** |
| `hasFeature(id)` | linear scan of the feature list | `Map` lookup — **O(1)** |
| `capabilitiesOf(id)` | `firstOrNull { … }` scan | `Map` lookup — **O(1)** |
| tab filtering (`App.kt`) | `resolvedFeatures.any { … }` per tab | `snapshot.hasFeature(id)` |
| where the logic lives | split: extension file + provider scans | one place: the snapshot |

**Cost model:** the indexes are built **once per snapshot** (i.e. once per login). After that, every
gate / tab / recomposition is a constant-time lookup with no new allocations.

---

## 6. One-paragraph recap

The `ExperienceSnapshot` is immutable and built once at login, so we moved the access queries onto it
and back them with **lazily-cached** structures — a `Map<FeatureId, Feature>` and a flattened
`Set<String>` of capabilities. Feature and capability checks became **O(1)** hash lookups instead of
per-call list scans and set re-flattening, the provider and both UI wrappers delegate to those cached
members, and the duplicated check code was consolidated into the snapshot. Same behaviour, the work is
paid once, reads are constant-time.
