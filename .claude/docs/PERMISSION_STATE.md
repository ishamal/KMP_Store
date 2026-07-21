# Tech Design: PermissionState Handling in Android

## Overview

This document describes how feature-level permission state is resolved, scoped, and consumed
across all Android feature ViewModels. It covers the canonical data flow, the key types involved,
the rules every ViewModel must follow, and how to model a `PermissionState` value object for
any feature screen.

---

## Architecture: End-to-End Data Flow

```
Login
  └─ Backend response
       └─ capabilities: Set<String>   ← what the user is granted
       └─ permissions:  Set<String>   ← what is permitted for this BU / role
            │
            ▼
       ExperienceProvider.getExperienceSnapshot()
         effective = capabilities ∩ permissions
         resolvedFeatures = effective grouped by FeatureId prefix
            │
            ▼
       ExperienceSnapshot  (immutable value object, one per session)
            │
            ▼
       ExperienceGraph created  (@GraphExtension(ExperienceScope::class))
         binds snapshot: ExperienceSnapshot  (non-null)
            │
            ▼
       FeatureViewModel constructed by Metro
         receives ExperienceSnapshot at injection time
         computes FeaturePermissionState once (immutable for VM lifetime)
            │
            ▼
       presenter() → FeatureState
         .permissionState  ← drives screen-level visibility / gating
            │
            ▼
       FeatureScreen
         shows / hides UI elements based on permission flags
```

---

## Key Types

### ExperienceSnapshot (`core/experience/api`)

The immutable access model resolved at login. All permission decisions derive from it.

```kotlin
data class ExperienceSnapshot(
    val experience: Experience,
    val businessUnit: BusinessUnit,
    val userRoles: UserRole,
    val resolvedFeatures: Set<Feature>,
) {
    fun hasFeature(featureId: FeatureId): Boolean
    fun hasCapability(capability: String): Boolean
    fun hasCapability(featureId: FeatureId, capability: String): Boolean
}
```

### Capability (`core/experience/api`)

Type-safe catalog of all dotted capability keys. The single source of truth — no raw strings at
call sites.

```kotlin
enum class Capability(val value: String) {
    REBATE_VIEW("rebate.view"),
    ORDER_VIEW("order.view"),
    DELIVERY_WIDGET_VIEW("delivery.widget.view"),
    // …
}
```

### PermissionCheck (`core/experience/api`)

The shared allow/deny rule, common to Android and iOS. Two checks must both hold:
1. The capability's prefix maps to a `FeatureId` **and** the snapshot resolves that feature.
2. The snapshot grants the exact capability string.

```kotlin
fun ExperienceSnapshot?.permits(capability: Capability?): Boolean
```

- `capability = null` → always `true` (ungated)
- `snapshot = null` (no active session) → always `false`
- Prefix maps to no known `FeatureId` → feature check is skipped, only the capability grant is checked

### PermissionGate (`core/ui/api`)

Compose-level wrapper over `permits`. Use this to conditionally render a composable block. Use
`permits()` directly when you need a Boolean to drive state (e.g. inside a `PermissionState`
value object).

```kotlin
@Composable
fun PermissionGate(
    capability: Capability? = null,
    snapshot: ExperienceSnapshot? = LocalExperienceSnapshot.current,
    fallback: @Composable () -> Unit = {},
    content: @Composable (FeatureAction?) -> Unit,
)
```

### ExperienceScope (`core/di/api`)

DI scope marker for one resolved snapshot. Recreated on every BU/experience switch, torn down at
logout. ViewModels bound here receive a non-null `ExperienceSnapshot` at construction time.

---

## PermissionState Lifetime

| Session event | Effect on PermissionState |
|---|---|
| Login | `ExperienceGraph` created → ViewModel constructed with current `ExperienceSnapshot` → `permissionState` computed |
| BU / experience switch | `ExperienceGraph` recreated → ViewModel destroyed and rebuilt → `permissionState` reflects new snapshot |
| Logout | `ExperienceScope` torn down → ViewModel destroyed |

No manual refresh, no observable, no nullable holder. Scope recreation is the update mechanism.

---

## Rules for Feature ViewModels

**1. Bind into `ExperienceScope`, not `AppScope`.**

`AppScope` singletons outlive sessions. Binding a snapshot-consuming VM there carries stale data
across logout/re-login or BU switches. `ExperienceScope` ties the VM lifetime to exactly one
resolved snapshot.

**2. Inject `ExperienceSnapshot` directly. Never inject `ExperienceProvider`.**

`ExperienceProvider` is a pure stateless builder (`getExperienceSnapshot()`). It holds no live
snapshot. The live snapshot is bound inside `ExperienceGraph` — inject it from there.

**3. Compute `PermissionState` once, at construction time.**

`ExperienceSnapshot` is immutable within the scope. A constructor-time `val` makes this explicit
and avoids per-recomposition overhead.

**4. Use `snapshot.permits(Capability.X)` at call sites.**

Prefer the type-safe `Capability` overload over raw strings. If a capability doesn't exist in the
enum yet, add it there first — it is the single source of truth.

---

## Pattern: Modeling a PermissionState

Define a dedicated value object per screen that flattens the capabilities the screen cares about
into named Boolean flags. The screen should never call `snapshot.permits()` directly — that logic
belongs in the ViewModel/presenter layer.

```kotlin
data class FeaturePermissionState(
    val isXEnabled: Boolean = false,
    val isYEnabled: Boolean = false,
)

private fun resolvePermissionState(snapshot: ExperienceSnapshot): FeaturePermissionState =
    FeaturePermissionState(
        isXEnabled = snapshot.permits(Capability.FEATURE_X),
        isYEnabled = snapshot.permits(Capability.FEATURE_Y),
    )
```

The ViewModel holds it as a `val`:

```kotlin
@Inject
@ViewModelKey(FeatureViewModel::class)
@ContributesIntoMap(ExperienceScope::class, binding = binding<ViewModel>())
internal class FeatureViewModel(
    private val snapshot: ExperienceSnapshot,
    // … other dependencies
) : MoleculeViewModel<FeatureEvent, FeatureState>() {

    private val permissionState = resolvePermissionState(snapshot)

    @Composable
    override fun present(): FeatureState =
        FeatureState(
            permissionState = permissionState,
            // …
        )
}
```

---

## Gating Layers

A screen's `PermissionState` is a **presentation concern** — it controls what the screen renders.
It is not the authoritative access check. Each feature is also independently gated at the nav /
tab level via `PermissionGate` (contributed by that feature's `*Contribution.kt`).

```
Screen-level row / widget visibility  ←  FeaturePermissionState  (presentation layer)
Tab / nav entry visibility            ←  PermissionGate(Capability.FEATURE_VIEW)  (nav layer)
Feature sub-widget visibility         ←  PermissionGate(Capability.FEATURE_SUB_X)  (feature layer)
```

All three resolve from the same `ExperienceSnapshot` and are consistent by construction.

---

## Examples

### Single-feature VM (RebateViewModel)

Reads capabilities scoped to one feature using the `(featureId, capability)` overload, which
restricts the check to capabilities belonging to that feature.

```kotlin
@Inject
@ContributesIntoMap(ExperienceScope::class, binding = binding<ViewModel>())
@ViewModelKey(RebateViewModel::class)
class RebateViewModel(
    private val repository: RebateRepository,
    private val snapshot: ExperienceSnapshot,
) : MoleculeViewModel<RebateEvent, RebateState>() {

    @Composable
    override fun present(events: Flow<RebateEvent>): RebateState {
        val canViewTotal = snapshot.hasCapability(FeatureId.REBATE, Capability.REBATE_VIEW_TOTAL.value)
        val canViewDaily = snapshot.hasCapability(FeatureId.REBATE, Capability.REBATE_VIEW_DAILY.value)
        // …
    }
}
```

### Multi-feature aggregation VM (MoreViewModel)

Aggregates capabilities across multiple features into one `PermissionState` value object. The
pattern is identical — only the number of capability checks differs.

```kotlin
@Inject
@ViewModelKey(MoreViewModel::class)
@ContributesIntoMap(ExperienceScope::class, binding = binding<ViewModel>())
internal class MoreViewModel(
    private val moreClient: MoreClient,
    private val monitoringService: MonitoringService,
    private val snapshot: ExperienceSnapshot,
) : MoleculeViewModel<MoreEvent, MoreState>() {

    private val permissionState = resolveMorePermissionState(snapshot)

    @Composable
    override fun present(): MoreState {
        val scope = rememberCoroutineScope()
        return MoreState(
            morePermissionState = permissionState,
            onEvent = { event ->
                when (event) {
                    MoreEvent.LogoutClick -> scope.launch { moreClient.logout() }
                    MoreEvent.StartView   -> monitoringService.startView(VIEW_KEY, "More")
                    MoreEvent.StopView    -> monitoringService.stopView(VIEW_KEY)
                }
            },
        )
    }
}

private fun resolveMorePermissionState(snapshot: ExperienceSnapshot): MorePermissionState =
    MorePermissionState(
        isRebateEnabled   = snapshot.permits(Capability.REBATE_VIEW),
        isOrderEnabled    = snapshot.permits(Capability.ORDER_VIEW),
        isDeliveryEnabled = snapshot.permits(Capability.DELIVERY_WIDGET_VIEW),
    )
```
