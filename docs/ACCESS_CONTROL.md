# Access control — how it flows through the app (a beginner's guide)

This explains, step by step, how the app decides **what a logged-in user is allowed to do** — from
the moment they log in to the moment a button is shown or an action is blocked. No prior knowledge
assumed.

---

## 1. The five words you need

| Word | What it is | Example |
|---|---|---|
| **BusinessUnit** | *Who the user's organisation is.* One per user. | `USBL`, `CABL`, `SESM` |
| **UserRole** | *What job the user has* (every shop has these roles). | `ADMIN`, `MANAGER`, `USER`, `CUSTOMER_ADMIN` |
| **Capability** | A thing a **BusinessUnit** is allowed to do. | `cart.edit` |
| **Permission** | A thing a **UserRole** is allowed to do. | `cart.edit` |
| **Feature** | A thing in the app that can be shown/hidden. | `CART_EDIT` (key `"cart.view"`, `"cart.edit"`, …) |

> Capabilities and permissions use the **same words** (`cart.edit`, `invoice.view`, …) but come from
> **two different authorities**: the business unit grants capabilities, the role grants permissions.

### The golden rule (the intersection)
A user can do something **only if BOTH agree**:

```
allowed = (business unit grants the capability)  AND  (role grants the permission)
```

If either side says no, it's not allowed. Example: the `USBL` business unit allows `cart.edit`, but a
`USER` role does not → that user **cannot** edit the cart.

---

## 2. Where the rules are written (just two tables)

All the "who can do what" lives in two files — the **policy**. Editing access = editing these:

- **`BusinessUnitCapabilities.kt`** — `BusinessUnit → set of Capability`
  ```kotlin
  USBL → [cart.view, cart.edit, invoice.view, …]   // full
  CABL → [cart.view, catalog.view, invoice.view, …] // read-only customer
  ```
- **`UserRolePermissions.kt`** — `UserRole → set of Permission`
  ```kotlin
  ADMIN → everything
  USER  → [cart.view]            // can look, not edit
  ```

The list of all possible keys lives in one catalog file, **`AccessKeys.kt`**
(`"cart.view"`, `"cart.edit"`, `"invoice.view"`, …).

---

## 3. The "black box" you ask (`AccessControl`)

Code never reads those tables directly. It asks one object, **`AccessControl`**, simple yes/no
questions:

```kotlin
accessControl.hasCapability(unit, Capability.CART_EDIT)   // does this business unit allow it?
accessControl.hasPermission(role, Permission.CART_EDIT)   // does this role allow it?
accessControl.isAllowed(unit, role, Feature.CART_EDIT)    // do BOTH allow it? (the golden rule)
accessControl.allowedFeatures(unit, role)                 // everything this user may see
```

- The **questions** (the `AccessControl` interface) live in `:core:access:api` — open to everyone.
- The **answers** (the real logic + the two tables) live in `:core:access:real` — hidden.

This split is why features can *ask* without *seeing* how it's decided. The real implementation is
handed to them automatically by the DI system (Metro).

---

## 4. The flow, from login to a button

Here's the whole journey of one user:

```
1. LOG IN
   RealAuthenticator turns the email into a Session:
   Session(email, businessUnit = USBL, userRole = ADMIN)
        │
2. SESSION REMEMBERED
   The Session is stored; it now carries the user's identity (unit + role).
        │
3. APP SHELL PUBLISHES THE "GUARD"   (App.kt, after login)
   AccessGuard(accessControl, session.businessUnit, session.userRole)
   is put into LocalAccessGuard so any screen can read it.
        │
        ├──────────────► 4a. UI LAYER checks PERMISSION (role)
        │                    PermissionGate(Permission.CART_EDIT) { editButtons() }
        │                    → shows the buttons only if the ROLE allows it.
        │
        └──────────────► 4b. FUNCTION LAYER checks CAPABILITY (business unit)
                             RealCartRepository.requireCartEdit()
                             → accessControl.requireCapability(unit, CART_EDIT)
                             → throws if the BUSINESS UNIT doesn't allow it.
```

### The layering rule (important)
- **Permissions are checked in the UI.** Use `PermissionGate(Permission.X) { … }` to show/hide
  controls based on the user's **role**.
- **Capabilities are checked in functions.** Call `accessControl.requireCapability(unit, Capability.X)`
  at the top of a business operation (in a repository/use-case) so it's enforced based on the user's
  **business unit**, no matter what the UI does.

Why both? **Defense in depth.** The UI hides what the role can't do (nice UX); the function refuses
what the business unit can't do (real enforcement, even if the UI is wrong or bypassed).

---

## 5. The worked example shipped in this app (cart editing)

**Policy:** `cart.edit` permission is granted only to `ADMIN`; `cart.edit` capability is granted to
`USBL`/`SESM` (not `CABL`).

**UI** (`CartScreen.kt`):
```kotlin
PermissionGate(Permission.CART_EDIT) {       // role check
    Row { "-"  qty  "+"   Remove }           // edit controls
}
```

**Function** (`RealCartRepository.kt`):
```kotlin
private fun requireCartEdit() {
    val unit = sessionManager.session!!.businessUnit
    accessControl.requireCapability(unit, Capability.CART_EDIT)   // capability check (throws if denied)
}
```

**Try it** — log in with different emails (the stub maps email → identity):

| Email | Becomes | Edit buttons? (UI/role) | Edit works? (function/unit) |
|---|---|---|---|
| `user@x.com` | USBL · USER | hidden (USER lacks permission) | — |
| `admin@x.com` | USBL · ADMIN | shown | yes |
| `cabl.admin@x.com` | CABL · ADMIN | shown | **blocked** — unit lacks the capability |

The last row is the point of having both layers: the UI lets the admin try, but the function still
refuses because the *business unit* isn't allowed.

---

## 6. Where everything lives (module map)

```
core/access/api/   ← the words + the questions (open to features)
  ├── BusinessUnit.kt, UserRole.kt          (who the user is)
  ├── Capability.kt, Permission.kt, Feature.kt  (the vocabulary)
  ├── AccessKeys.kt                          (the catalog of "cart.view" strings)
  ├── AccessControl.kt                       (the black-box interface)
  └── AccessChecks.kt                        (requireCapability / requirePermission helpers)

core/access/real/  ← the answers (hidden)
  ├── BusinessUnitCapabilities.kt            (BusinessUnit → Capability table)
  ├── UserRolePermissions.kt                 (UserRole → Permission table)
  └── RealAccessControl.kt                   (reads the tables, does the intersection)

core/ui/api/ (androidMain)
  └── access/ui/AccessGate.kt                (AccessGuard, LocalAccessGuard, PermissionGate)

core/session/api/Session.kt                  (carries businessUnit + userRole)
androidApp/di/AppGraph.kt                    (exposes accessControl)
androidApp/App.kt                            (publishes LocalAccessGuard after login)
```

---

## 7. How to add access for a NEW feature

Say you add an "export invoices" action:

1. **`AccessKeys.kt`** — add `const val INVOICE_EXPORT = "invoice.export"`.
2. **`Feature.kt`** — add `INVOICE_EXPORT(AccessKeys.INVOICE_EXPORT)`.
3. **`Capability.kt` / `Permission.kt`** — add `INVOICE_EXPORT` constants.
4. **`BusinessUnitCapabilities.kt`** — grant `Capability.INVOICE_EXPORT` to the right units.
5. **`UserRolePermissions.kt`** — grant `Permission.INVOICE_EXPORT` to the right roles.
6. **UI** — wrap the export button in `PermissionGate(Permission.INVOICE_EXPORT) { … }`.
7. **Function** — at the top of the export function, call
   `accessControl.requireCapability(unit, Capability.INVOICE_EXPORT)`.

A test (`catalog_coversEveryFeatureKey`) checks that steps 1 and 2 stay in sync.

---

## 8. One-paragraph recap

A user logs in and gets a **BusinessUnit** + **UserRole** on their `Session`. The app builds an
**AccessGuard** from that and shares it. In the **UI**, `PermissionGate` shows controls based on the
**role's permission**; in **functions**, `requireCapability` enforces the **business unit's
capability**. Something is truly allowed only when **both** agree. The rules live in two small tables
in `:core:access:real`; everyone else just asks the `AccessControl` black box.
