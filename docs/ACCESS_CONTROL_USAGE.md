# Access control — usage reference (copy-paste recipes)

A practical "how do I use it" companion to [`ACCESS_CONTROL.md`](ACCESS_CONTROL.md) (which explains the
concepts and the flow). This page is just the recipes.

> **The one rule:** check **permissions in the UI** (`PermissionGate`, role-based) and **capabilities
> in functions** (`requireCapability`, business-unit-based).

---

## API at a glance

| You have… | You want to… | Use |
|---|---|---|
| a Compose screen | show/hide a control by the user's **role** | `PermissionGate(Permission.X) { … }` |
| a Compose screen | show/hide a whole feature (tab) — both sides | `AccessGate(Feature.X) { … }` |
| a Compose screen | a boolean for the role | `LocalAccessGuard.current.hasPermission(Permission.X)` |
| a repository / use-case | enforce a **business-unit** capability | `accessControl.requireCapability(unit, Capability.X)` |
| any Kotlin code | a boolean for the unit | `accessControl.hasCapability(unit, Capability.X)` |
| any Kotlin code | the full allowed set | `accessControl.allowedFeatures(unit, role)` |

`Permission`, `Capability`, `Feature` all come from `:core:access:api`.

---

## Recipe 1 — gate a button in the UI (permission / role)

```kotlin
import com.isharaw.kmpproj.core.access.Permission
import com.isharaw.kmpproj.core.access.ui.PermissionGate

PermissionGate(Permission.CART_EDIT) {
    Button(onClick = { … }) { Text("Edit") }
}

// with a fallback when not allowed:
PermissionGate(Permission.INVOICE_EXPORT, fallback = { Text("Upgrade to export") }) {
    Button(onClick = { … }) { Text("Export") }
}
```

Nothing renders if the role lacks the permission. No `if` at the call site.

---

## Recipe 2 — enforce a capability in a function (business unit)

```kotlin
import com.isharaw.kmpproj.core.access.Capability
import com.isharaw.kmpproj.core.access.requireCapability

class RealCartRepository(
    private val accessControl: AccessControl,
    private val sessionManager: SessionManager,
) : CartRepository {

    override fun remove(id: Int) {
        val unit = sessionManager.session!!.businessUnit
        accessControl.requireCapability(unit, Capability.CART_EDIT)  // throws AccessDeniedException if denied
        … // do the work
    }
}
```

Put the check at the **top** of the operation so it can't run without the capability — even if the UI
is wrong or bypassed.

---

## Recipe 3 — show a whole feature (tab) only if truly allowed

`AccessGate` checks **both** sides (capability ∩ permission) — right for whole-feature visibility:

```kotlin
import com.isharaw.kmpproj.core.access.Feature
import com.isharaw.kmpproj.core.access.ui.AccessGate

AccessGate(Feature.REBATE_VIEW) {
    RebatesTab()
}
```

Or filter a list of tabs:
```kotlin
val guard = LocalAccessGuard.current
val visibleTabs = tabs.filter { it.feature in guard.allowedFeatures }
```

---

## Recipe 4 — handle a denied capability gracefully (no crash)

`requireCapability` **throws**. If the call happens from a click handler, catch it:

```kotlin
var error by remember { mutableStateOf<String?>(null) }

Button(onClick = {
    try {
        repository.remove(item.id)
    } catch (e: AccessDeniedException) {
        error = e.message
    }
}) { Text("Remove") }

error?.let { Snackbar { Text(it) } }
```

Or check first with the boolean form and avoid the throw:
```kotlin
if (accessControl.hasCapability(unit, Capability.CART_EDIT)) repository.remove(id)
```

---

## Recipe 5 — wire the guard once in the app shell

Already done in `App.kt`, shown here for reference. After login, publish the guard so every screen can
read it:

```kotlin
CompositionLocalProvider(
    LocalAccessGuard provides AccessGuard(
        accessControl = graph.accessControl,
        businessUnit  = session.businessUnit,
        userRole      = session.userRole,
    ),
) {
    MainScaffold(...)
}
```

For this to work the app graph must expose `AccessControl`:
- `androidApp/build.gradle.kts` → `implementation(projects.core.access.real)`
- `AppGraph` → `val accessControl: AccessControl`

---

## Recipe 6 — add access for a new action

1. `AccessKeys.kt` → `const val INVOICE_EXPORT = "invoice.export"`
2. `Feature.kt` → `INVOICE_EXPORT(AccessKeys.INVOICE_EXPORT)`
3. `Capability.kt` & `Permission.kt` → add the `INVOICE_EXPORT` constant
4. `BusinessUnitCapabilities.kt` → grant the capability to the right units
5. `UserRolePermissions.kt` → grant the permission to the right roles
6. UI: `PermissionGate(Permission.INVOICE_EXPORT) { … }`
7. Function: `accessControl.requireCapability(unit, Capability.INVOICE_EXPORT)`

---

## Gotchas

- **`LocalAccessGuard not provided`** → you used a gate outside the logged-in tree. Gates only work
  below the `CompositionLocalProvider(LocalAccessGuard provides …)` in `App.kt` (i.e. after login).
- **Binding not found for `AccessControl`** → the module/app graph doesn't depend on
  `:core:access:real`. Add it.
- **Capability vs Permission mixed up** → `PermissionGate` takes a `Permission` (role);
  `requireCapability` takes a `Capability` (business unit). They're distinct types on purpose.
