package com.isharaw.kmpproj.core.access

/** Thrown by [requireCapability] / [requirePermission] when an access check fails. */
class AccessDeniedException(message: String) : RuntimeException(message)

/**
 * **Function-level** capability guard. Capabilities (granted by the [BusinessUnit]) are enforced in the
 * logic / repository layer — call this at the top of a business operation so the action can't run
 * without the capability, regardless of any UI:
 *
 * ```
 * fun editCart(...) {
 *     accessControl.requireCapability(session.businessUnit, Capability.CART_EDIT)
 *     …
 * }
 * ```
 */
fun AccessControl.requireCapability(unit: BusinessUnit, capability: Capability) {
    if (!hasCapability(unit, capability)) {
        throw AccessDeniedException("Business unit $unit is missing capability '${capability.id}'.")
    }
}

/**
 * **UI-level** permission guard, non-Compose form (e.g. for view-models / click handlers). Permissions
 * (granted by the [UserRole]) gate what the user may do in the UI. The Compose wrapper `PermissionGate`
 * is the usual way to gate visibility; use this when you need the check in plain Kotlin.
 */
fun AccessControl.requirePermission(role: UserRole, permission: Permission) {
    if (!hasPermission(role, permission)) {
        throw AccessDeniedException("Role $role is missing permission '${permission.id}'.")
    }
}

/**
 * Runs [block] **only if** [unit] grants [capability]; otherwise does nothing and returns `null`.
 * Use this (instead of [requireCapability]) when an action should be silently skipped — not throw —
 * when the capability isn't valid:
 *
 * ```
 * accessControl.withCapability(unit, Capability.CART_EDIT) {
 *     // runs only when allowed
 * }
 * ```
 */
inline fun <T> AccessControl.withCapability(
    unit: BusinessUnit,
    capability: Capability,
    block: () -> T,
): T? = if (hasCapability(unit, capability)) block() else null

/**
 * Runs [block] **only if** [role] grants [permission]; otherwise does nothing and returns `null`.
 * The no-throw counterpart of [requirePermission].
 */
inline fun <T> AccessControl.withPermission(
    role: UserRole,
    permission: Permission,
    block: () -> T,
): T? = if (hasPermission(role, permission)) block() else null
