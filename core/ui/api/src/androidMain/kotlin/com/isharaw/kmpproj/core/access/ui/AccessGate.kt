package com.isharaw.kmpproj.core.access.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.Capability
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

    /**
     * The user's business unit grants [capability]. Prefer checking capabilities in the function layer
     * (`requireCapability`/`withCapability`); use this only when the UI genuinely must reflect it.
     */
    fun hasCapability(capability: Capability): Boolean = accessControl.hasCapability(businessUnit, capability)

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

/**
 * Combined gate: renders [content] only if the user's business unit grants [capability] **and** their
 * role grants [permission]. Use when the capability and permission are **different** keys; if they're
 * the same action, prefer [AccessGate] with a single [Feature].
 *
 * Note: per the layering convention, capability checks normally belong in the function layer — reach
 * for this overload only when the UI must genuinely reflect both sides.
 *
 * ```
 * AccessGate(Capability.CART_EDIT, Permission.CART_VIEW) {
 *     EditButton(...)   // unit may edit AND role may at least view
 * }
 * ```
 */
@Composable
fun AccessGate(
    capability: Capability,
    permission: Permission,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val guard = LocalAccessGuard.current
    if (guard.hasCapability(capability) && guard.hasPermission(permission)) content() else fallback()
}

// ---------------------------------------------------------------------------------------------------
// Explicit-identity gates: pass the user's [BusinessUnit] + [UserRole] in (e.g. from a ViewModel's
// state) instead of reading them from an ambient guard. Only the stateless policy ([AccessControl]) is
// ambient, via [LocalAccessControl].
// ---------------------------------------------------------------------------------------------------

/**
 * The access-control **policy** (stateless, app-scoped), published once by the app shell. Unlike
 * [LocalAccessGuard] this carries **no** session identity — pass the user's [BusinessUnit] + [UserRole]
 * explicitly to the gates below.
 */
val LocalAccessControl = staticCompositionLocalOf<AccessControl> {
    error("LocalAccessControl not provided — wrap content in CompositionLocalProvider(LocalAccessControl provides graph.accessControl)")
}

/**
 * Gated wrapper. Renders [content] only if the **intersection** of [businessUnit]'s capabilities and
 * [userRole]'s permissions grants [feature] (i.e. both the unit AND the role allow it); otherwise
 * renders [fallback] (empty by default). The identity is passed in (from the ViewModel); the policy
 * comes from [LocalAccessControl].
 *
 * ```
 * AccessGate(state.businessUnit, state.userRole, Feature.REBATE_VIEW, fallback = { NoAccess() }) {
 *     RebateDashboard(...)
 * }
 * ```
 */
@Composable
fun AccessGate(
    businessUnit: BusinessUnit,
    userRole: UserRole,
    feature: Feature,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (LocalAccessControl.current.isAllowed(businessUnit, userRole, feature)) content() else fallback()
}

/**
 * Explicit-identity gate for **different** keys: renders [content] only if [businessUnit] grants
 * [capability] AND [userRole] grants [permission]. (Same as the `feature` overload when both keys are
 * the same action.)
 */
@Composable
fun AccessGate(
    businessUnit: BusinessUnit,
    userRole: UserRole,
    capability: Capability,
    permission: Permission,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val ac = LocalAccessControl.current
    if (ac.hasCapability(businessUnit, capability) && ac.hasPermission(userRole, permission)) {
        content()
    } else {
        fallback()
    }
}
