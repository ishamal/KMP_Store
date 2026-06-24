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
