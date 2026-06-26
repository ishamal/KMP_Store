package com.isharaw.kmpproj.core

/** Stable identity of a top-level app feature — used for tab gating and equality. */
enum class FeatureId { CART, CATALOG, INVOICES, ORDERS, REBATE, SETTINGS }

/**
 * A feature the backend **resolved** for a user: its [featureId], a display [featureName], and the
 * [capabilities] the user has within it (dotted action keys, e.g. for ORDERS: `"order.view"`,
 * `"order.create"`, `"order.update"`, `"order.cancel"`).
 *
 * The [ExperienceSnapshot] carries the set of these; the app shows a feature's tab when a Feature with
 * the matching [featureId] is present (and the feature module is linked in the build).
 */
data class Feature(
    val featureId: FeatureId,
    val featureName: String,
    val capabilities: Set<String>,
)
