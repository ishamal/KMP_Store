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
