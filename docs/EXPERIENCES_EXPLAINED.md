# Experiences, Explained for Beginners

A gentle, step-by-step walkthrough of how this app decides **what a logged-in user is allowed to
see and do**. No prior context needed. (For the dense reference version, see
[`EXPERIENCES.md`](EXPERIENCES.md).)

---

## 1. The one-sentence idea

> Two different users can open the **exact same app** and see **different things** — because each
> user has an **experience**, and the experience decides what they're allowed to see.

In this app there are two experiences: **USBL** (the full experience) and **CABL** (a restricted
one). USBL sees everything; CABL sees less.

### A real-world analogy 🎟️

Think of a **cinema**:
- The **building** is the app (everyone walks into the same building).
- Your **ticket** is your *session* (you got it when you "logged in" at the counter).
- Your ticket type — **VIP** or **Standard** — is your *experience*.
- What your ticket *lets you do* (enter the VIP lounge, get free popcorn, sit in row A) are
  *capabilities*.

The staff never look at you and think "oh, this is a VIP person, so…". They just check: *does your
ticket allow this specific thing?* That's the whole design.

---

## 2. Store vs Experience — don't mix them up

These are **two different questions**, decided at two different times:

| | Question | Decided when? | Example |
|---|---|---|---|
| **Store** | Is this feature even *in* the app? | when the app is **built** | storeB's app simply has no "Orders" code inside it |
| **Experience** | May *this user* see/use it? | when the user **logs in** | a CABL user can't see the Export button |

> Store = *what's in the box.* Experience = *what this person is allowed to touch.*

This document is only about the **Experience** half.

---

## 3. The four building blocks

All of these live in `core/` so every feature can use them.

### (a) `Experience` — which kind of user
`core/.../Experience.kt`
```kotlin
enum class Experience { USBL, CABL }
```
Just two names. That's it.

### (b) `Session` — who is logged in right now
`core/.../Experience.kt`
```kotlin
data class Session(val email: String, val experience: Experience)
```
When you log in, the app creates one `Session`: your email + which experience you got. This is your
"ticket".

### (c) `Capability` — individual things you might be allowed to do
`core/.../Capability.kt`
```kotlin
enum class Capability {
    VIEW_PAID_INVOICES,
    VIEW_PENDING_INVOICES,
    VIEW_OVERDUE_INVOICES,
    EXPORT_INVOICES,
    VIEW_ORDERS,
}
```
Each value is **one specific permission**. Notice we don't have a capability called "isCabl" — we
list *actual things* ("can export invoices", "can see paid invoices"). That's important (see §6).

### (d) `ExperienceCatalog` — the rulebook mapping experience → capabilities
`core/.../Capability.kt`
```kotlin
object ExperienceCatalog {
    private val capabilities = mapOf(
        Experience.USBL to Capability.entries.toSet(),               // USBL → ALL capabilities
        Experience.CABL to setOf(Capability.VIEW_PENDING_INVOICES),  // CABL → just this one
    )
    fun capabilitiesOf(experience: Experience) = capabilities[experience].orEmpty()
}
```
This is the **single place** that says "what each experience can do." Want to change what CABL can
do? Edit this one map. Nothing else changes.

And the helper everyone uses to ask a yes/no question:
```kotlin
fun Experience.has(capability: Capability): Boolean =
    capability in ExperienceCatalog.capabilitiesOf(this)
```
So in code you write `experience.has(Capability.EXPORT_INVOICES)` → `true` or `false`.

---

## 4. The whole flow, start to finish

Here's what actually happens, in order:

```
1. User types email + password on the Login screen
        │
2. Authenticator turns that into a Session (email + experience)
   (real backend would decide; right now it's a stub — see below)
        │
3. SessionManager stores the Session for the app's lifetime
        │
4. The app shell (App.kt) reads session.experience
        │
5. Every screen asks "experience.has(SOME_CAPABILITY)?" to decide what to show
```

### Step 2 in detail — where the experience comes from

Today there's no backend, so it's faked from the email (`features/login/.../RealLogin.kt`):
```kotlin
class RealAuthenticator : Authenticator {
    override fun authenticate(email: String, password: String): Session {
        val experience =
            if (email.startsWith("cabl", ignoreCase = true)) Experience.CABL else Experience.USBL
        return Session(email = email.trim(), experience = experience)
    }
}
```
So:
- log in as **`cabl@x.com`** → you get **CABL**
- log in as **anything else** (e.g. `me@x.com`) → you get **USBL**

(When a real login API exists, only this one function changes — the backend tells you the
experience. Everything downstream stays the same.)

---

## 5. How a screen uses it — two patterns

There are exactly two ways the experience changes the UI. Both just ask `has(...)`.

### Pattern A — "hide/show a button or whole tab"

**A button inside a screen** (`features/invoices/.../InvoicesScreen.kt`). We model actions as a list,
each carrying the capability it needs, then keep only the allowed ones:
```kotlin
private data class InvoiceAction(
    val label: String,
    override val capability: Capability,
    val onClick: () -> Unit,
) : CapabilityGated

val actions = listOf(
    InvoiceAction("Export", Capability.EXPORT_INVOICES) { /* export */ },
).allowedFor(experience)        // ← keeps only the ones this user may use

actions.forEach { TextButton(onClick = it.onClick) { Text(it.label) } }
```
A CABL user doesn't have `EXPORT_INVOICES`, so `allowedFor` drops it → no Export button. No `if`
needed, and adding a new action is just one more row in the list.

**A whole tab** is the same idea, declared on the tab and filtered once in the app shell
(`features/orders/.../OrdersContribution.kt` + `App.kt`):
```kotlin
// the Orders feature declares what its tab needs:
Tab(OrdersKey, TabMeta("Orders", "📦", order = 20, requiredCapability = Capability.VIEW_ORDERS))

// App.kt hides tabs the user can't use — in ONE place for all tabs:
graph.tabs.filter { it.meta.requiredCapability?.let(experience::has) ?: true }
```
CABL lacks `VIEW_ORDERS` → the Orders tab never appears for them.

### Pattern B — "show *less data*, not a different screen"

Sometimes the screen is the same but the **data** differs. Invoices is the example: USBL sees all
invoices; CABL sees only *pending* ones. We don't write `if (cabl) …`. Instead each invoice *status*
maps to a capability, and we filter (`features/invoices/api/.../Invoices.kt`):
```kotlin
private val statusCapability = mapOf(
    InvoiceStatus.PAID    to Capability.VIEW_PAID_INVOICES,
    InvoiceStatus.PENDING to Capability.VIEW_PENDING_INVOICES,
    InvoiceStatus.OVERDUE to Capability.VIEW_OVERDUE_INVOICES,
)

fun InvoiceRepository.invoicesFor(experience: Experience): List<Invoice> =
    all().filter { experience.has(statusCapability.getValue(it.status)) }
```
For a CABL user, `has(VIEW_PAID_INVOICES)` is `false`, so paid invoices are filtered out; only
pending ones (which CABL *does* have) remain.

---

## 5½. Zooming in on `CapabilityGated` and `allowedFor`

Pattern A used these two helpers from `core/.../Capability.kt`. They look fancy but the idea is
simple — let's take them apart.

```kotlin
interface CapabilityGated {
    val capability: Capability
}

fun <T : CapabilityGated> Iterable<T>.allowedFor(experience: Experience): List<T> =
    filter { experience.has(it.capability) }
```

### The interface = a contract ("you must wear a badge")

An **interface** is a *promise*. `CapabilityGated` says:

> "Any class that implements me **promises it can tell you which capability it needs** — via a
> `capability` property."

It doesn't care *what* the thing is (a button, a card, a menu row, a tab). It only requires that the
thing can answer one question: *"what permission do you require?"* So when `InvoiceAction` declares
`: CapabilityGated`, it's signing that contract:
```kotlin
private data class InvoiceAction(
    ...,
    override val capability: Capability,   // ← fulfilling the promise
) : CapabilityGated
```

### The generic function = one reusable "bouncer"

`fun <T : CapabilityGated> Iterable<T>.allowedFor(...)` has three parts:

1. **`<T : CapabilityGated>`** — a **generic**. `T` means *"any type — I don't care which —
   **as long as** it implements `CapabilityGated`."* This is why the same function works for
   `InvoiceAction` today and any future `MenuItem`, `Shortcut`, etc. Written **once**, used by all.
2. **`Iterable<T>.allowedFor(...)`** — an **extension function**: it bolts a new ability,
   `.allowedFor(...)`, onto *any* collection of such items, so you can call `myList.allowedFor(experience)`.
3. **`= filter { experience.has(it.capability) }`** — the actual work: keep only the items whose
   required `capability` this experience has.

### Analogy 🛂

`allowedFor` is a **bouncer at a door**. Every guest (item) wears a badge stating *which pass they
need* (`capability`). The bouncer doesn't know or care whether you're a button, a tab, or a menu row
— it just reads your badge and checks your ticket (`experience.has(...)`). Pass holders get through;
the rest are turned away.

The `CapabilityGated` interface is simply the house rule **"everyone must wear a badge"** — that's
exactly what lets one generic bouncer handle every kind of guest.

### So, end to end
```kotlin
listOf(
    InvoiceAction("Export", Capability.EXPORT_INVOICES) { … },   // a guest wearing the EXPORT badge
).allowedFor(experience)                                          // bouncer checks each guest's badge
// USBL → [Export]   (has the pass)
// CABL → []         (no pass → turned away)
```
No `if` per action; add a new gated action by adding one more "guest" to the list.

---

## 6. The golden rule 🥇

> **Never write `if (experience == Experience.CABL)`.**
> Always ask about a **capability**: `experience.has(Capability.SOMETHING)`.

Why this matters (this is the heart of the whole design):
- If you scatter `experience == CABL` checks everywhere and later add a third experience (say
  `Experience.PARTNER`), you'd have to hunt down and fix *every* check. 😱
- With capabilities, you add `PARTNER` to **one** map (`ExperienceCatalog`) and *every* screen
  instantly does the right thing — because they were asking "can you do X?", not "who are you?". 🎉

---

## 7. Walked-through example

Let's log in two ways on **storeA** and see what differs.

**Log in as `me@x.com` → experience = USBL** (has every capability):
- Invoices screen: sees **paid + pending + overdue** invoices, **Export** button **shown**.
- Bottom bar: **Orders** tab **shown** (`VIEW_ORDERS` granted).

**Log in as `cabl@x.com` → experience = CABL** (only `VIEW_PENDING_INVOICES`):
- Invoices screen: sees **pending only**; no Export button (`EXPORT_INVOICES` not granted).
- Bottom bar: **no Orders** tab (`VIEW_ORDERS` not granted).

Same app, same code, two different results — all decided by the one map in `ExperienceCatalog`.

---

## 8. Cookbook — making changes

**"Let CABL also see paid invoices."**
→ In `ExperienceCatalog`, add `VIEW_PAID_INVOICES` to CABL's set. Done. (No screen changes.)

**"Add a new permission, e.g. delete an invoice."**
1. Add `DELETE_INVOICE` to the `Capability` enum.
2. Grant it to the right experiences in `ExperienceCatalog`.
3. Use it: add an `InvoiceAction("Delete", Capability.DELETE_INVOICE) { … }` to the list (Pattern A),
   or `requiredCapability = Capability.DELETE_INVOICE` on a tab.

**"Add a third experience, e.g. PARTNER."**
1. Add `PARTNER` to the `Experience` enum.
2. Add its capability set to `ExperienceCatalog`.
3. Map credentials → `PARTNER` in `Authenticator`.
→ No screen code changes — every `has(...)` check just works.

---

## 9. Try it yourself

1. Run the **storeADebug** variant (Build Variants panel in Android Studio).
2. Log in as `me@x.com` → notice Orders tab + Export button + all invoices.
3. Log out, log in as `cabl@x.com` → Orders tab gone, no Export, pending invoices only.
4. Open `core/.../Capability.kt`, add `VIEW_ORDERS` to the CABL set, re-run → CABL now has the
   Orders tab. That one line is the whole change.

---

## 10. Quick glossary

| Term | Plain meaning |
|---|---|
| **Experience** | the "type" of logged-in user (USBL / CABL) |
| **Session** | the current logged-in user (email + experience) — your "ticket" |
| **Capability** | one specific thing you may be allowed to do |
| **ExperienceCatalog** | the one rulebook: experience → its capabilities |
| **`experience.has(cap)`** | the yes/no question every screen asks |
| **`CapabilityGated` / `allowedFor`** | helper to filter a list of actions to the allowed ones |
| **`requiredCapability`** | the capability a tab needs to be shown |
