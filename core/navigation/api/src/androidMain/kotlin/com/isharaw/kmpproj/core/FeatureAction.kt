package com.isharaw.kmpproj.core

import androidx.navigation3.runtime.NavKey

// FeatureSlot and FeatureKind are defined in commonMain (IosFeatureAction.kt) so iOS can share
// the same enums without pulling in Nav3. Android sees them through commonMain → androidMain
// visibility; this file only needs to define the NavKey-carrying FeatureAction class.

/**
 * An optional, store-gated entry point a feature exposes into a shared host surface, contributed via
 * a DI multibinding (`@Provides @IntoSet`). Because contributions come only from linked modules, an
 * action appears only in stores whose manifest ships the contributing feature — the host surface
 * never depends on the feature.
 *
 * The feature declares *which host* ([slots]) and *what it is* ([kind]); the host maps the kind to a
 * concrete placement + representation. A single action may target several slots.
 *
 * @param label     text shown to the user
 * @param target    destination opened on click (the feature's [NavKey])
 * @param slots     the host surfaces that render it (an action may appear in several)
 * @param kind      which non-common feature this is — the host renders it accordingly
 * @param featureId the permission feature this action belongs to, if any — lets `PermissionGate`
 *                  resolve the compiled-in action for a capability and hand it to its content. Null
 *                  for actions with no permission feature (e.g. password reset).
 * @param order     sort order within a host (lower = first)
 */
class FeatureAction(
    val label: String,
    val target: NavKey,
    val slots: Set<FeatureSlot>,
    val kind: FeatureKind,
    val featureId: FeatureId? = null,
    val order: Int = 0,
) {
    /** Convenience for the common single-slot case. */
    constructor(
        label: String,
        target: NavKey,
        slot: FeatureSlot,
        kind: FeatureKind,
        featureId: FeatureId? = null,
        order: Int = 0,
    ) : this(label, target, setOf(slot), kind, featureId, order)
}
