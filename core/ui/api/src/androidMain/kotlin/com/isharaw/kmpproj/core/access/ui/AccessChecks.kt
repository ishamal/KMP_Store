package com.isharaw.kmpproj.core.access.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.isharaw.kmpproj.core.access.Capability
import com.isharaw.kmpproj.core.access.Feature
import com.isharaw.kmpproj.core.access.Permission

/**
 * Compose-level access checks — the `@Composable` counterpart of the function-layer `AccessChecks`
 * (`requireCapability` / `withCapability` …). These read the ambient [LocalAccessGuard], so you can
 * branch on access **inside a composable** without fetching the guard yourself:
 *
 * ```
 * if (hasPermission(Permission.REBATE_VIEW)) {
 *     RebateLink()
 * }
 * val label = if (hasPermission(Permission.CART_EDIT)) "Edit" else "View"
 * ```
 *
 * Prefer [PermissionGate] / [AccessGate] for simple show/hide; reach for these when you need the
 * **boolean** itself (changing a label, enabling a control, a larger `when`, …).
 *
 * They must be called inside the logged-in subtree (below where the app shell provides
 * [LocalAccessGuard]); calling them before login throws "LocalAccessGuard not provided".
 */

/** True if the current user's **role** grants [permission]. The usual UI-level check. */
@Composable
@ReadOnlyComposable
fun hasPermission(permission: Permission): Boolean =
    LocalAccessGuard.current.hasPermission(permission)

/** True if both the user's business unit **and** role allow [feature] (capability ∩ permission). */
@Composable
@ReadOnlyComposable
fun isAllowed(feature: Feature): Boolean =
    LocalAccessGuard.current.isAllowed(feature)

/**
 * True if the current user's **business unit** grants [capability]. Capabilities are normally enforced
 * in the function layer (`requireCapability`/`withCapability`); use this only when the UI must reflect
 * the capability directly.
 */
@Composable
@ReadOnlyComposable
fun hasCapability(capability: Capability): Boolean =
    LocalAccessGuard.current.hasCapability(capability)
