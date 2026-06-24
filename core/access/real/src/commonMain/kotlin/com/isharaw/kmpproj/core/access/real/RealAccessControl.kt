package com.isharaw.kmpproj.core.access.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.Capability
import com.isharaw.kmpproj.core.access.Feature
import com.isharaw.kmpproj.core.access.Permission
import com.isharaw.kmpproj.core.access.UserRole
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The "logic" behind the [AccessControl] black box. The actual grant tables live in their own files —
 * [BusinessUnitCapabilities] (BusinessUnit → Capability) and [UserRolePermissions] (UserRole →
 * Permission) — so there's an obvious place to edit when a feature is added. This class just reads
 * them and computes the intersection.
 *
 * Wired by Metro: `@ContributesBinding` registers it as the [AccessControl] for the app graph, so
 * features just inject `AccessControl` and never depend on this class.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealAccessControl : AccessControl {

    override fun capabilitiesOf(unit: BusinessUnit): Set<Capability> =
        BusinessUnitCapabilities.map[unit].orEmpty()

    override fun permissionsOf(role: UserRole): Set<Permission> =
        UserRolePermissions.map[role].orEmpty()

    override fun allowedFeatures(unit: BusinessUnit, role: UserRole): Set<Feature> {
        // Intersect on the raw backend ids (the format both sides use), then resolve to the known
        // [Feature]s. A feature shows only when the business unit AND the user role both grant its id;
        // unknown ids (e.g. a new backend permission the app doesn't model yet) are ignored.
        val capabilityIds: Set<String> = capabilitiesOf(unit).mapTo(mutableSetOf()) { it.id }
        val permissionIds: Set<String> = permissionsOf(role).mapTo(mutableSetOf()) { it.id }
        val sharedIds: Set<String> = capabilityIds intersect permissionIds
        return Feature.entries.filterTo(mutableSetOf()) { it.key in sharedIds }
    }

    override fun hasCapability(unit: BusinessUnit, capability: Capability): Boolean =
        capability in capabilitiesOf(unit)

    override fun hasPermission(role: UserRole, permission: Permission): Boolean =
        permission in permissionsOf(role)

    override fun isAllowed(unit: BusinessUnit, role: UserRole, feature: Feature): Boolean {
        // Both sides must grant the feature's key — match on the raw id so a backend-supplied
        // capability/permission string lines up with the feature.
        val grantedByUnit = capabilitiesOf(unit).any { it.id == feature.key }
        val grantedByRole = permissionsOf(role).any { it.id == feature.key }
        return grantedByUnit && grantedByRole
    }
}
