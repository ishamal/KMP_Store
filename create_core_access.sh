#!/usr/bin/env bash
set -euo pipefail
#
# Recreates the :core:access module (api + real) — access-control vocabulary + black-box
# contract (api) and the policy maps + RealAccessControl (real).
#
# Usage: copy this file into the ROOT of your other project (the dir containing core/) and run:
#   bash create_core_access.sh
#
echo "Creating core/access ..."

mkdir -p "core/access/api"
cat > "core/access/api/build.gradle.kts" <<'COREACCESS_EOF'
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:access — access-control vocabulary + the AccessControl black-box contract:
// BusinessUnit, UserRole, Capability, Permission, Feature, AccessKeys, AccessControl. No deps, no Compose.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.access"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/AccessControl.kt" <<'COREACCESS_EOF'
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
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/AccessKeys.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

/**
 * The complete catalog of access-control keys in the project — the dotted-string ids the backend
 * uses (e.g. `"cart.view"`). This is the **single source** of the key strings: [Feature], [Capability]
 * and [Permission] are all built from these constants, so a key is written exactly once.
 *
 * Capabilities (granted by a [BusinessUnit]) and permissions (granted by a [UserRole]) draw from the
 * **same vocabulary**, so [Permissions] and [Capabilities] list the same keys. One entry per gated
 * action of every feature/function in the app. (Login & password-reset are pre-auth flows, so they
 * are not access-gated and have no keys here.)
 *
 * To add a function: add its `const val` here, add a matching [Feature] entry, then grant it to the
 * relevant business units / roles in `RealAccessControl`.
 */
object AccessKeys {
    // Cart
    const val CART_VIEW = "cart.view"
    const val CART_EDIT = "cart.edit"

    // Catalog (product browsing)
    const val CATALOG_VIEW = "catalog.view"

    // Invoices
    const val INVOICE_VIEW = "invoice.view"
    const val INVOICE_EXPORT = "invoice.export"

    // Orders
    const val ORDER_VIEW = "order.view"

    // Rebate
    const val REBATE_VIEW = "rebate.view"

    // Settings
    const val SETTINGS_VIEW = "settings.view"
    const val SETTINGS_EDIT = "settings.edit"

    /** Every access key in the project, as a flat list (the dotted ids the backend uses). */
    val ALL: List<String> = listOf(
        CART_VIEW, CART_EDIT,
        CATALOG_VIEW,
        INVOICE_VIEW, INVOICE_EXPORT,
        ORDER_VIEW,
        REBATE_VIEW,
        SETTINGS_VIEW, SETTINGS_EDIT,
    )

    /** Permissions vocabulary (the keys a [UserRole] can grant) — same keys as [Capabilities]. */
    val Permissions: List<String> = ALL

    /** Capabilities vocabulary (the keys a [BusinessUnit] can grant) — same keys as [Permissions]. */
    val Capabilities: List<String> = ALL
}
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/BusinessUnit.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

/**
 * A **business unit**. A user belongs to one; it grants a set of [Capability]
 * (see [AccessControl.capabilitiesOf]).
 */
enum class BusinessUnit { USBL, CABL, SESM }
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/Capability.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

import kotlin.jvm.JvmInline

/**
 * What a [BusinessUnit] grants, carried as the **raw backend [id]** (e.g. `"cart.view"`). Kept as a
 * String so the in-memory policy matches exactly what the backend will send — when the backend is
 * wired up, `capabilitiesOf` just returns those strings and no type changes. **Distinct** from
 * [Permission] (granted by a [UserRole]).
 *
 * The named constants below mirror the known [Feature]s (the single source of the key strings) for
 * readable code; an unknown backend id is still a valid `Capability("…")`.
 */
@JvmInline
value class Capability(val id: String) {
    companion object {
        val CART_VIEW = Capability(AccessKeys.CART_VIEW)
        val CART_EDIT = Capability(AccessKeys.CART_EDIT)
        val CATALOG_VIEW = Capability(AccessKeys.CATALOG_VIEW)
        val INVOICE_VIEW = Capability(AccessKeys.INVOICE_VIEW)
        val INVOICE_EXPORT = Capability(AccessKeys.INVOICE_EXPORT)
        val ORDER_VIEW = Capability(AccessKeys.ORDER_VIEW)
        val REBATE_VIEW = Capability(AccessKeys.REBATE_VIEW)
        val SETTINGS_VIEW = Capability(AccessKeys.SETTINGS_VIEW)
        val SETTINGS_EDIT = Capability(AccessKeys.SETTINGS_EDIT)

        /** Every capability the app knows (the whole [AccessKeys.Capabilities] vocabulary). */
        val ALL: Set<Capability> = AccessKeys.Capabilities.mapTo(LinkedHashSet()) { Capability(it) }
    }
}
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/Feature.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

/**
 * A **feature/action** that can be shown in the app, identified by a stable dotted [key] from
 * [AccessKeys] (the single source of the key strings). One entry per gated action of every function
 * in the project. A feature is shown only when **both** the business unit (via a [Capability]) **and**
 * the user role (via a [Permission]) grant it — see [AccessControl.allowedFeatures].
 */
enum class Feature(val key: String) {
    CART_VIEW(AccessKeys.CART_VIEW),
    CART_EDIT(AccessKeys.CART_EDIT),
    CATALOG_VIEW(AccessKeys.CATALOG_VIEW),
    INVOICE_VIEW(AccessKeys.INVOICE_VIEW),
    INVOICE_EXPORT(AccessKeys.INVOICE_EXPORT),
    ORDER_VIEW(AccessKeys.ORDER_VIEW),
    REBATE_VIEW(AccessKeys.REBATE_VIEW),
    SETTINGS_VIEW(AccessKeys.SETTINGS_VIEW),
    SETTINGS_EDIT(AccessKeys.SETTINGS_EDIT),
    ;

    companion object {
        /** Look up a feature by its dotted [key]; throws if unknown. */
        fun fromKey(key: String): Feature =
            entries.firstOrNull { it.key == key.trim() }
                ?: error("Unknown feature '$key'. Known: ${entries.map { it.key }}")
    }
}
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/Permission.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

import kotlin.jvm.JvmInline

/**
 * What a [UserRole] grants, carried as the **raw backend [id]** (e.g. `"cart.view"`) — same format the
 * backend will send, so it stays a drop-in until then. **Distinct** from [Capability] (granted by a
 * [BusinessUnit]).
 *
 * The named constants below mirror the known [Feature]s (the single source of the key strings); an
 * unknown backend id is still a valid `Permission("…")`.
 */
@JvmInline
value class Permission(val id: String) {
    companion object {
        val CART_VIEW = Permission(AccessKeys.CART_VIEW)
        val CART_EDIT = Permission(AccessKeys.CART_EDIT)
        val CATALOG_VIEW = Permission(AccessKeys.CATALOG_VIEW)
        val INVOICE_VIEW = Permission(AccessKeys.INVOICE_VIEW)
        val INVOICE_EXPORT = Permission(AccessKeys.INVOICE_EXPORT)
        val ORDER_VIEW = Permission(AccessKeys.ORDER_VIEW)
        val REBATE_VIEW = Permission(AccessKeys.REBATE_VIEW)
        val SETTINGS_VIEW = Permission(AccessKeys.SETTINGS_VIEW)
        val SETTINGS_EDIT = Permission(AccessKeys.SETTINGS_EDIT)

        /** Every permission the app knows (the whole [AccessKeys.Permissions] vocabulary). */
        val ALL: Set<Permission> = AccessKeys.Permissions.mapTo(LinkedHashSet()) { Permission(it) }
    }
}
COREACCESS_EOF

mkdir -p "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/api/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/UserRole.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

/**
 * A **user role** within a shop (every shop has these roles). It grants a set of [Permission]
 * (see [AccessControl.permissionsOf]).
 */
enum class UserRole { ADMIN, CUSTOMER_ADMIN, MANAGER, USER }
COREACCESS_EOF

mkdir -p "core/access/real"
cat > "core/access/real/build.gradle.kts" <<'COREACCESS_EOF'
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:access:real — the access policy (BusinessUnit→Capability, UserRole→Permission) + RealAccessControl.
// Applies Metro to contribute its AccessControl binding; bundles :core:access:api. Logic lives here, never in :api.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.isharaw.kmpproj.core.access.real"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:access:api"))
            implementation(project(":core:di:api")) // AppScope (DI scope marker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
COREACCESS_EOF

mkdir -p "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real"
cat > "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real/BusinessUnitCapabilities.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access.real

import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.Capability

/**
 * **BusinessUnit → Capability policy.** This is the one place that says which capabilities each
 * business unit makes available.
 *
 * ➜ When you add a new feature: after adding its key to `AccessKeys` / `Feature` / `Capability`, grant
 *   the new `Capability.X` to every business unit that should have it **here**. (`Capability.ALL`
 *   automatically includes it for full-access units like USBL.)
 */
internal object BusinessUnitCapabilities {
    val map: Map<BusinessUnit, Set<Capability>> = mapOf(
        // USBL: full access — every capability in the catalog.
        BusinessUnit.USBL to Capability.ALL,

        // CABL: read-only customer view (no cart edits, no rebates).
        BusinessUnit.CABL to setOf(
            Capability.CART_VIEW,
            Capability.CATALOG_VIEW,
            Capability.INVOICE_VIEW,
            Capability.ORDER_VIEW,
        ),

        // SESM: sales/service — cart + rebates + catalog/orders, but not invoices.
        BusinessUnit.SESM to setOf(
            Capability.CART_VIEW,
            Capability.CART_EDIT,
            Capability.CATALOG_VIEW,
            Capability.ORDER_VIEW,
            Capability.REBATE_VIEW,
        ),
    )
}
COREACCESS_EOF

mkdir -p "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real"
cat > "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real/RealAccessControl.kt" <<'COREACCESS_EOF'
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
COREACCESS_EOF

mkdir -p "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real"
cat > "core/access/real/src/commonMain/kotlin/com/isharaw/kmpproj/core/access/real/UserRolePermissions.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access.real

import com.isharaw.kmpproj.core.access.Permission
import com.isharaw.kmpproj.core.access.UserRole

/**
 * **UserRole → Permission policy.** This is the one place that says which permissions each user role
 * grants (every shop has these roles).
 *
 * ➜ When you add a new feature: after adding its key to `AccessKeys` / `Feature` / `Permission`, grant
 *   the new `Permission.X` to every role that should have it **here**. (`Permission.ALL` automatically
 *   includes it for full-access roles like ADMIN.)
 */
internal object UserRolePermissions {
    val map: Map<UserRole, Set<Permission>> = mapOf(
        // Admin: everything.
        UserRole.ADMIN to Permission.ALL,

        // Customer admin: view carts (NOT edit them) + see invoices. Note: even though the USBL
        // business unit grants CART_EDIT, this role doesn't — so a USBL + CUSTOMER_ADMIN user cannot
        // edit the cart (the intersection in allowedFeatures drops CART_EDIT).
        UserRole.CUSTOMER_ADMIN to setOf(
            Permission.CART_VIEW,
            Permission.INVOICE_VIEW,
        ),

        // Manager: view carts/invoices + rebates, no cart edits.
        UserRole.MANAGER to setOf(
            Permission.CART_VIEW,
            Permission.INVOICE_VIEW,
            Permission.REBATE_VIEW,
        ),

        // Plain user: view the cart only.
        UserRole.USER to setOf(
            Permission.CART_VIEW,
        ),
    )
}
COREACCESS_EOF

mkdir -p "core/access/real/src/commonTest/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/real/src/commonTest/kotlin/com/isharaw/kmpproj/core/access/AccessControlTest.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

import com.isharaw.kmpproj.core.access.real.RealAccessControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessControlTest {

    // Use the black-box contract type on purpose: callers only ever see AccessControl.
    private val access: AccessControl = RealAccessControl()

    @Test
    fun businessUnit_returnsItsCapabilities() {
        assertEquals(Capability.ALL, access.capabilitiesOf(BusinessUnit.USBL)) // full access
        assertEquals(
            setOf(Capability.CART_VIEW, Capability.CATALOG_VIEW, Capability.INVOICE_VIEW, Capability.ORDER_VIEW),
            access.capabilitiesOf(BusinessUnit.CABL),
        )
    }

    @Test
    fun catalog_coversEveryFeatureKey() {
        // The separate catalog (AccessKeys) and the typed Feature enum stay in sync.
        assertEquals(Feature.entries.map { it.key }.toSet(), AccessKeys.ALL.toSet())
        // Capabilities and permissions share the same vocabulary.
        assertEquals(AccessKeys.ALL.toSet(), AccessKeys.Permissions.toSet())
        assertEquals(AccessKeys.ALL.toSet(), AccessKeys.Capabilities.toSet())
    }

    @Test
    fun userRole_returnsItsPermissions() {
        assertEquals(Permission.ALL, access.permissionsOf(UserRole.ADMIN))
        assertEquals(
            setOf(Permission.CART_VIEW, Permission.INVOICE_VIEW),
            access.permissionsOf(UserRole.CUSTOMER_ADMIN),
        )
        assertEquals(setOf(Permission.CART_VIEW), access.permissionsOf(UserRole.USER))
    }

    @Test
    fun cartEdit_hiddenWhenRoleLacksIt_evenIfBusinessUnitGrantsIt() {
        // The concept: USBL (business unit) HAS CART_EDIT, but CUSTOMER_ADMIN (role) does NOT.
        assertTrue(Capability.CART_EDIT in access.capabilitiesOf(BusinessUnit.USBL))
        assertTrue(Permission.CART_EDIT !in access.permissionsOf(UserRole.CUSTOMER_ADMIN))

        // So a USBL + CUSTOMER_ADMIN user does NOT get cart.edit (role wins the veto).
        val shown = access.allowedFeatures(BusinessUnit.USBL, UserRole.CUSTOMER_ADMIN)
        assertTrue(Feature.CART_EDIT !in shown)
        assertEquals(setOf(Feature.CART_VIEW, Feature.INVOICE_VIEW), shown)
    }

    @Test
    fun allowedFeatures_isIntersectionOfUnitAndRole() {
        // CABL unit grants: cart.view, invoice.view
        // MANAGER role grants: cart.view, invoice.view, rebate.view
        // Shown = intersection: cart.view, invoice.view  (rebate.view dropped — unit doesn't grant it)
        assertEquals(
            setOf(Feature.CART_VIEW, Feature.INVOICE_VIEW),
            access.allowedFeatures(BusinessUnit.CABL, UserRole.MANAGER),
        )
        assertTrue(Feature.REBATE_VIEW !in access.allowedFeatures(BusinessUnit.CABL, UserRole.MANAGER))
    }

    @Test
    fun allowedFeatures_userRoleNarrowsAFullUnit() {
        // USBL grants everything, but a plain USER only adds cart.view → only cart.view shows.
        assertEquals(
            setOf(Feature.CART_VIEW),
            access.allowedFeatures(BusinessUnit.USBL, UserRole.USER),
        )
    }

    @Test
    fun allowedFeatures_unitNarrowsAFullRole() {
        // ADMIN grants everything, but SESM has no invoices → invoice.view/export hidden.
        assertEquals(
            setOf(Feature.CART_VIEW, Feature.CART_EDIT, Feature.CATALOG_VIEW, Feature.ORDER_VIEW, Feature.REBATE_VIEW),
            access.allowedFeatures(BusinessUnit.SESM, UserRole.ADMIN),
        )
    }

    @Test
    fun hasCapability_checksBusinessUnitOnly() {
        assertTrue(access.hasCapability(BusinessUnit.USBL, Capability.CART_EDIT))   // USBL is full
        assertFalse(access.hasCapability(BusinessUnit.CABL, Capability.CART_EDIT))  // CABL is read-only
    }

    @Test
    fun hasPermission_checksUserRoleOnly() {
        assertTrue(access.hasPermission(UserRole.ADMIN, Permission.CART_EDIT))
        assertFalse(access.hasPermission(UserRole.CUSTOMER_ADMIN, Permission.CART_EDIT))
    }

    @Test
    fun isAllowed_requiresBothSides() {
        // USBL grants cart.edit, but CUSTOMER_ADMIN doesn't → not allowed (role vetoes).
        assertFalse(access.isAllowed(BusinessUnit.USBL, UserRole.CUSTOMER_ADMIN, Feature.CART_EDIT))
        // ADMIN grants cart.edit, but CABL doesn't → not allowed (unit vetoes).
        assertFalse(access.isAllowed(BusinessUnit.CABL, UserRole.ADMIN, Feature.CART_EDIT))
        // Both grant cart.view → allowed.
        assertTrue(access.isAllowed(BusinessUnit.CABL, UserRole.USER, Feature.CART_VIEW))
    }

    @Test
    fun feature_hasStableDottedKey() {
        assertEquals("cart.view", Feature.CART_VIEW.key)
        assertEquals(Feature.REBATE_VIEW, Feature.fromKey("rebate.view"))
    }

    @Test
    fun grants_carryRawBackendIds() {
        // The backend sends strings like "cart.view"; the value classes ARE those strings, so a raw
        // id from the backend equals the named constant (drop-in when the backend is wired up).
        assertEquals("cart.view", Capability.CART_VIEW.id)
        assertEquals(Capability.CART_VIEW, Capability("cart.view"))
        assertEquals(Permission.INVOICE_VIEW, Permission("invoice.view"))
    }
}
COREACCESS_EOF

mkdir -p "core/access/real/src/commonTest/kotlin/com/isharaw/kmpproj/core/access"
cat > "core/access/real/src/commonTest/kotlin/com/isharaw/kmpproj/core/access/AccessInjectionTest.kt" <<'COREACCESS_EOF'
package com.isharaw.kmpproj.core.access

import com.isharaw.kmpproj.core.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the feature-facing path. A feature depends only on `:core:model:api` and references the
 * [AccessControl] **contract** — never `RealAccessControl` (which lives in `:real` and is invisible
 * to features). Metro supplies the implementation from the contributed binding. This graph stands in
 * for the real app graph; in production you'd add `val access: AccessControl` to the existing AppGraph.
 */
@DependencyGraph(AppScope::class)
interface AccessConsumerGraph {
    val access: AccessControl
}

class AccessInjectionTest {

    @Test
    fun featureGetsDataThroughInjectedContract() {
        val graph = createGraph<AccessConsumerGraph>()
        val access: AccessControl = graph.access // only the api type is named here

        assertEquals(
            setOf(Capability.CART_VIEW, Capability.CATALOG_VIEW, Capability.INVOICE_VIEW, Capability.ORDER_VIEW),
            access.capabilitiesOf(BusinessUnit.CABL),
        )
        assertEquals(setOf(Permission.CART_VIEW), access.permissionsOf(UserRole.USER))
        assertEquals(
            setOf(Feature.CART_VIEW, Feature.INVOICE_VIEW),
            access.allowedFeatures(BusinessUnit.CABL, UserRole.MANAGER),
        )
    }
}
COREACCESS_EOF

echo ""
echo "core/access created. Next steps (manual):"
echo "  1) settings.gradle.kts: add"
echo "       include(\":core:access:api\")"
echo "       include(\":core:access:real\")"
echo "  2) Version catalog (gradle/libs.versions.toml) must define plugins:"
echo "       kotlinMultiplatform, androidMultiplatformLibrary, metro   and lib: kotlin.test"
echo "  3) :core:di:api must exist and expose AppScope (used by RealAccessControl)."
echo "  4) If a module needs access control, depend on :core:access:api and inject AccessControl."
