package com.isharaw.kmpproj.core

import androidx.navigation3.runtime.NavKey

/**
 * A host surface where feature-contributed [FeatureAction]s render. Add a slot when a new host needs
 * to show them.
 */
enum class FeatureSlot {
    /** The Settings screen. */
    SETTINGS,
    // e.g. HOME, OVERFLOW_MENU, …
}

/**
 * Identifies one of the app's non-common (store-specific) features. A host surface uses this to
 * decide how/where to present the action — e.g. Settings renders [REBATE] as a card at the top and
 * [PASSWORD_RESET] as a button before "Log out". Add a value when a new optional feature exposes an
 * action.
 */
enum class FeatureKind {
    REBATE,
    PASSWORD_RESET,
}

/**
 * An optional, store-gated entry point a feature exposes into a shared host surface, contributed via
 * a DI multibinding (`@Provides @IntoSet`). Because contributions come only from linked modules, an
 * action appears only in stores whose manifest ships the contributing feature — the host surface
 * never depends on the feature.
 *
 * The feature declares *which host* ([slots]) and *what it is* ([kind]); the host maps the kind to a
 * concrete placement + representation. A single action may target several slots.
 *
 * @param label   text shown to the user
 * @param target  destination opened on click (the feature's [NavKey])
 * @param slots   the host surfaces that render it (an action may appear in several)
 * @param kind    which non-common feature this is — the host renders it accordingly
 * @param order   sort order within a host (lower = first)
 */
class FeatureAction(
    val label: String,
    val target: NavKey,
    val slots: Set<FeatureSlot>,
    val kind: FeatureKind,
    val order: Int = 0,
) {
    /** Convenience for the common single-slot case. */
    constructor(label: String, target: NavKey, slot: FeatureSlot, kind: FeatureKind, order: Int = 0)
        : this(label, target, setOf(slot), kind, order)
}
