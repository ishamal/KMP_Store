package com.isharaw.kmpproj.core.access.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.Feature
import com.isharaw.kmpproj.core.access.Permission
import com.isharaw.kmpproj.core.access.UserRole

/**
 * The logged-in user's access context for the UI. Build it once in the app shell from the `Session`
 * and the injected [AccessControl], publish via [LocalAccessGuard], then gate UI with [PermissionGate]
 * (or [AccessGate] for whole-feature visibility).
 *
 * **Layering convention:** **permissions** (granted by the [UserRole]) are checked **at the UI level**
 * — that's what this guard exposes. **Capabilities** (granted by the [BusinessUnit]) are checked **at
 * the function level** in the logic layer via `AccessControl.requireCapability(...)`, NOT in the UI.
 */
class AccessGuard(
    private val accessControl: AccessControl,
    val businessUnit: BusinessUnit,
    val userRole: UserRole,
) {
    /** UI-level check: the user's role grants [permission]. */
    fun hasPermission(permission: Permission): Boolean = accessControl.hasPermission(userRole, permission)

    /** Whole-feature visibility (e.g. whether to show a tab): both the unit and the role grant it. */
    fun isAllowed(feature: Feature): Boolean = accessControl.isAllowed(businessUnit, userRole, feature)

    /** Every feature this user may see (capability ∩ permission) — handy for filtering tabs. */
    val allowedFeatures: Set<Feature> get() = accessControl.allowedFeatures(businessUnit, userRole)
}

/**
 * The current [AccessGuard], published once by the app shell:
 * `CompositionLocalProvider(LocalAccessGuard provides AccessGuard(accessControl, session.businessUnit, session.userRole))`.
 */
val LocalAccessGuard = staticCompositionLocalOf<AccessGuard> {
    error("LocalAccessGuard not provided — wrap content in CompositionLocalProvider(LocalAccessGuard provides …)")
}

/**
 * **The UI gate.** Renders [content] only if the current user's role grants [permission]; otherwise
 * renders [fallback] (nothing by default). This is where permissions are checked — at the UI level.
 *
 * ```
 * PermissionGate(Permission.INVOICE_VIEW) {
 *     InvoicesButton(...)
 * }
 * ```
 */
@Composable
fun PermissionGate(
    permission: Permission,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (LocalAccessGuard.current.hasPermission(permission)) content() else fallback()
}

/**
 * Whole-feature visibility gate (both the business unit's capability AND the role's permission grant
 * [feature]). Use for showing/hiding a whole feature such as a tab; for in-screen controls prefer
 * [PermissionGate].
 */
@Composable
fun AccessGate(
    feature: Feature,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (LocalAccessGuard.current.isAllowed(feature)) content() else fallback()
}
