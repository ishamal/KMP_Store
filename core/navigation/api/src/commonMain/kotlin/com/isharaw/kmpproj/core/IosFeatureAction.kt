package com.isharaw.kmpproj.core

/**
 * A host surface where feature-contributed actions render. Defined in commonMain so both Android
 * ([FeatureAction]) and iOS ([IosFeatureAction]) share the same enum and never drift.
 *
 * Android: [FeatureAction.slots] carries `Set<FeatureSlot>`.
 * iOS: [IosFeatureAction.slot] carries a single [FeatureSlot] (all existing actions target exactly
 * one slot; simplifies Kotlin → Swift bridging).
 */
enum class FeatureSlot {
    /** The Settings screen. */
    SETTINGS,
    // e.g. HOME, OVERFLOW_MENU, …
}

/**
 * Identifies one of the app's non-common (store-specific) features. Shared by [FeatureAction]
 * (Android) and [IosFeatureAction] (iOS) so the host surfaces on both platforms drive their UI from
 * the same vocabulary.
 */
enum class FeatureKind {
    REBATE,
    PASSWORD_RESET,
}

/**
 * NavKey-free counterpart of Android's [FeatureAction] — carries the same store-gating semantics
 * (contributed via `@Provides @IntoSet` from each feature's `:real` module, so it only appears in
 * stores that link the module) without pulling in Navigation 3 or Compose.
 *
 * Swift reads `IosAppGraph.featureActions`, filters to [FeatureSlot.SETTINGS], sorts by [order],
 * and routes on [kind] to build the Settings UI dynamically.
 *
 * @param label human-readable label shown in the UI
 * @param slot  the single host surface that renders this action
 * @param kind  which optional feature this is — lets the host decide placement/appearance
 * @param order sort order within the host (lower = first)
 */
data class IosFeatureAction(
    val label: String,
    val slot: FeatureSlot,
    val kind: FeatureKind,
    val order: Int = 0,
)
