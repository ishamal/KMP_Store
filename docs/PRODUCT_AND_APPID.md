# Product vs AppId — the super-app split (a beginner's guide)

You're building a **super-app**: one codebase that also ships as several standalone brand apps. This
doc explains the two concepts that make that work cleanly — **Product** and **AppId (Experience)** —
how they differ, and how they fit your current code (`Experience`, business units, `ExperienceSnapshot`).

---

## 1. The problem in one paragraph

The same code is shipped as **different installable apps**:
- "Keels" standalone (only the Keels brand)
- "Cargills" standalone (only Cargills)
- the **Super App** (Keels **and** Cargills **and** Glomark, switchable inside the app)

These are different **binaries**, but they're mostly the same app. And at runtime the user is *in* one
**brand experience** at a time. So we need to separate **"which app did they install"** from **"which
experience are they using right now."** Those are the two ideas.

---

## 2. The two ideas

| Concept | Question it answers | When fixed | Example |
|---|---|---|---|
| **Product** (a.k.a. ProductId / shell) | *Which binary did the user install?* | **build time** | `KeelsStandalone`, `CargillsStandalone`, **`SuperApp`** |
| **AppId** (a.k.a. Experience) | *Which brand experience is active right now?* | **runtime** | `KEELS`, `CARGILLS`, `GLOMARK` |

> **Product = the installed shell. AppId = the experience selected inside that shell.**

The crucial difference:
- In a **standalone** product, the Product allows **exactly one** experience → there's nothing to
  pick, the experience is fixed.
- In the **Super App** product, the Product allows **several** experiences → the user **chooses/switches**
  between them at runtime.

So "is there an experience picker?" is simply: *does this Product support more than one experience?*

---

## 3. How this maps to YOUR current code

You already have both halves — they're just not formally split yet:

| The doc's term | What you have today | Notes |
|---|---|---|
| **Product / ProductId** | the **store / build flavor** (`storeA`, `storeB`, `storeC` via `StoreManifest`) | the build-time binary |
| **AppId / Experience** | the **`Experience` enum** (`KEELS`, `CARGILLS`, `GLOMARK`) | the runtime brand |
| supported experiences for a product | *(missing)* — today `RealAuthenticator` hardcodes `Experience.KEELS` | a **Product → experiences** map |

So the super-app step is: introduce a **Product** that maps to **a set of experiences**, and let the
user select among them (instead of the experience being a hardcoded constant).

---

## 4. The Product catalog — Product → supported experiences

A small lookup says which experiences each Product ships:

```kotlin
enum class Product { KEELS_STANDALONE, CARGILLS_STANDALONE, GLOMARK_STANDALONE, SUPER_APP }

object ProductCatalog {
    fun experiencesOf(product: Product): Set<Experience> = when (product) {
        Product.KEELS_STANDALONE    -> setOf(Experience.KEELS)
        Product.CARGILLS_STANDALONE -> setOf(Experience.CARGILLS)
        Product.GLOMARK_STANDALONE  -> setOf(Experience.GLOMARK)
        Product.SUPER_APP           -> setOf(Experience.KEELS, Experience.CARGILLS, Experience.GLOMARK)
    }
}
```

The **Product is known at app start** (it's the binary) — e.g. baked into the build like your store
flavor (`BuildConfig`/`StoreManifest`). A standalone product yields a 1-element set; the super-app
yields many.

---

## 5. Which experiences can the user actually pick? (the intersection)

A super-app supports several experiences, **but the logged-in user may not be entitled to all of them.**
A business unit (CABL/USBL/SENM) only allows certain experiences. So the experiences offered are an
**intersection**:

```
selectable experiences =
    ProductCatalog.experiencesOf(product)        // what this binary ships
      ∩ businessUnit.allowedExperiences           // what the user's business unit permits
```

- **1 result** → auto-select it, no picker (this is exactly how a standalone behaves).
- **many results** → show an experience switcher.
- **0 results** → the user can't use this product (error / sign out).

> This is the same "intersection of ceilings" idea from access control, but at the **experience**
> level instead of the capability level.

---

## 6. How it plugs into `ExperienceSnapshot`

Today: `experience` is a hardcoded input to `getExperienceSnapshot(experience, businessUnit, role, capabilities)`.

With the split, **experience becomes a runtime selection**, and everything downstream stays the same:

```
build           → Product is known (e.g. SUPER_APP)
login           → businessUnit, role, (capabilities) come back
select experience → from  ProductCatalog.experiencesOf(product) ∩ businessUnit.allowedExperiences
        │            (auto if one, picker if many)
        ▼
getExperienceSnapshot(selectedExperience, businessUnit, role, capabilities)   ← unchanged
        ▼
ExperienceSnapshot  →  tabs + capability gating as today
```

Each experience can resolve to **different features/capabilities** (the snapshot is per-experience).
Switching experience in the super-app = re-resolve the snapshot for the newly selected experience (and,
if you've added the customer scope, **rebuild the customer graph** for it — like a re-login).

---

## 7. What to add to your codebase (checklist)

1. **`Product` enum + `ProductCatalog`** (`core:experience:api`) — Product → `Set<Experience>`.
2. **Know the Product at startup** — bake it into the build like the store flavor
   (`StoreManifest`/`BuildConfig`), so `KeelsStandalone` and `SuperApp` are different builds.
3. **An "active experience" holder** — for standalone it's the single value; for the super-app it's a
   selectable, observable value (a `CurrentExperience` like a session-scoped piece of state).
4. **Experience selection step** — after login, compute `experiencesOf(product) ∩ businessUnit.allowed`;
   auto-pick if one, otherwise show a switcher.
5. **Resolve the snapshot per selected experience** — call `getExperienceSnapshot(activeExperience, …)`,
   and re-resolve when the user switches.
6. **(Theme/branding follows the experience)** — your per-store branding becomes **per-experience**
   branding inside the super-app, applied when the active experience changes.

---

## 8. Worked example

**Build:** the Super App (`Product.SUPER_APP` → `{KEELS, CARGILLS, GLOMARK}`).

**Login:** business unit = `USBL`, which allows `{KEELS, CARGILLS}`.

**Selectable experiences:** `{KEELS, CARGILLS, GLOMARK} ∩ {KEELS, CARGILLS} = {KEELS, CARGILLS}` →
**two** → show a switcher.

- User picks **Keels** → `getExperienceSnapshot(KEELS, USBL, role, caps)` → Keels tabs/branding.
- User switches to **Cargills** → re-resolve → Cargills tabs/branding, fresh customer scope.

**Contrast — the Keels standalone build:** `Product.KEELS_STANDALONE → {KEELS}`, intersect with the
business unit → at most `{KEELS}` → **no picker**, experience fixed. Same code, no super-app UI.

---

## 9. Recap

- **Product** = the installed binary (build time); **AppId/Experience** = the brand active at runtime.
- A standalone Product allows **one** experience (fixed); the **Super App** allows **many** (switchable).
- The experiences a user may pick = `ProductCatalog.experiencesOf(product) ∩ businessUnit.allowedExperiences`
  — one ⇒ auto-select, many ⇒ switcher.
- The selected experience feeds your existing `getExperienceSnapshot(...)` unchanged; switching just
  re-resolves the snapshot (and customer scope) for the new experience.
- In your code: **store/flavor ≈ Product**, **`Experience` enum ≈ AppId** — you mainly need a
  `ProductCatalog` and a runtime experience selection instead of a hardcoded `Experience.KEELS`.
