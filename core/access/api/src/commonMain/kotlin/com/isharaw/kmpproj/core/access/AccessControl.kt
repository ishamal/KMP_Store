package com.isharaw.kmpproj.core.access

/**
 * The access-control **black box**. Callers pass a [BusinessUnit] and/or a [UserRole] and get back
 * grants; they never see how it's decided. All mapping logic lives in `RealAccessControl`
 * (`:core:model:real`).
 *
 * This contract and its vocabulary types ([BusinessUnit], [UserRole], [Capability], [Permission],
 * [Feature]) live in `:core:model:api`, so **features can inject and use `AccessControl`** by
 * depending only on `:core:model:api`. The implementation is supplied at runtime via Metro.
 */
interface AccessControl {
    /** The capabilities the business [unit] grants (empty set if none). */
    fun capabilitiesOf(unit: BusinessUnit): Set<Capability>

    /** The permissions the [role] grants (empty set if none). */
    fun permissionsOf(role: UserRole): Set<Permission>

    /**
     * The features actually shown to a user who is in [unit] with [role]: the **intersection** of
     * what the business unit's capabilities allow and what the role's permissions allow (matched on
     * the [Feature] each unlocks). A feature appears only if **both** sides grant it.
     */
    fun allowedFeatures(unit: BusinessUnit, role: UserRole): Set<Feature>

    // ---- yes/no checks: pass a capability, a permission, or both (a feature) and get true/false ----

    /** True if the business [unit] grants [capability] (checks the BusinessUnit → Capability map). */
    fun hasCapability(unit: BusinessUnit, capability: Capability): Boolean

    /** True if the [role] grants [permission] (checks the UserRole → Permission map). */
    fun hasPermission(role: UserRole, permission: Permission): Boolean

    /**
     * True only if **both** sides grant [feature]: the business [unit] has the matching capability
     * AND the [role] has the matching permission. Use this to gate a feature for a logged-in user,
     * e.g. `isAllowed(session.businessUnit, session.userRole, Feature.CART_EDIT)`.
     */
    fun isAllowed(unit: BusinessUnit, role: UserRole, feature: Feature): Boolean
}
