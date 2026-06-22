package com.isharaw.kmpproj.core

/**
 * Things an [Experience] may be allowed to do or see. Features and the navigation check
 * **capabilities** (never `experience == …`), so behavior is data-driven and centralized.
 */
enum class Capability {
    // Invoice data slices — one capability per slice. An experience is granted the slices it may
    // see; features filter generically (no if/when on the experience). "See everything" = granted
    // all three.
    VIEW_PAID_INVOICES,
    VIEW_PENDING_INVOICES,
    VIEW_OVERDUE_INVOICES,
    // Actions / whole-tab visibility.
    EXPORT_INVOICES,
    VIEW_ORDERS,
}

/**
 * The single source of truth for "what each experience can do" — the runtime analogue of
 * `buildSrc/StoreManifest.kt`. To change an experience, edit this one table.
 */
object ExperienceCatalog {
    private val capabilities: Map<Experience, Set<Capability>> = mapOf(
        // USBL: the full experience (all slices + all actions).
        Experience.USBL to Capability.entries.toSet(),
        // CABL: restricted — pending invoices only, no export, no orders.
        Experience.CABL to setOf(Capability.VIEW_PENDING_INVOICES),
    )

    fun capabilitiesOf(experience: Experience): Set<Capability> =
        capabilities[experience].orEmpty()
}

/** Reads the central catalog. Use this everywhere instead of comparing experiences. */
fun Experience.has(capability: Capability): Boolean =
    capability in ExperienceCatalog.capabilitiesOf(this)

/**
 * Marks an item whose visibility is gated by a [Capability]. Model UI actions/sections as a list of
 * these and render with [allowedFor], instead of scattering `if (experience.has(...))` checks — adding
 * an action becomes one list entry, never a new `if`.
 */
interface CapabilityGated {
    val capability: Capability
}

/** Keeps only the items the [experience] is allowed to see. */
fun <T : CapabilityGated> Iterable<T>.allowedFor(experience: Experience): List<T> =
    filter { experience.has(it.capability) }
