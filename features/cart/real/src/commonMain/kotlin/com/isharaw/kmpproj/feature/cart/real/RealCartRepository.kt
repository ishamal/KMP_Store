package com.isharaw.kmpproj.feature.cart.real

import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.core.access.AccessControl
import com.isharaw.kmpproj.core.access.Capability
import com.isharaw.kmpproj.core.access.withCapability
import com.isharaw.kmpproj.feature.cart.CartItem
import com.isharaw.kmpproj.feature.cart.CartRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * In-memory cart. @SingleIn because it holds mutable cart state for the app's lifetime.
 *
 * **Function-level capability check:** every mutating operation is wrapped in
 * [AccessControl.withCapability] for CART_EDIT — if the current user's business unit doesn't grant it,
 * the edit is **silently skipped** (no exception), independent of any UI. (Role *permissions* are
 * gated separately in the UI; see CartScreen's PermissionGate.)
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealCartRepository(
    private val accessControl: AccessControl,
    private val sessionManager: SessionManager,
) : CartRepository {

    private var current: List<CartItem> = listOf(
        CartItem(1, "Wireless Mouse", 19.99, 1),
        CartItem(2, "Mechanical Keyboard", 79.50, 1),
        CartItem(3, "USB-C Cable", 8.25, 2),
    )

    override val items: List<CartItem> get() = current
    override val total: Double get() = current.sumOf { it.lineTotal }

    override fun increment(id: Int) = update(id) { it.copy(quantity = it.quantity + 1) }
    override fun decrement(id: Int) = update(id) {
        if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else it
    }
    override fun remove(id: Int) = guardedEdit {
        current = current.filterNot { it.id == id }
    }

    private fun update(id: Int, transform: (CartItem) -> CartItem) = guardedEdit {
        current = current.map { if (it.id == id) transform(it) else it }
    }

    /**
     * Runs [block] only if the logged-in user's business unit grants the CART_EDIT capability;
     * otherwise the edit is skipped (no exception, no change).
     */
    private fun guardedEdit(block: () -> Unit) {
        val businessUnit = sessionManager.session?.businessUnit ?: return
        accessControl.withCapability(businessUnit, Capability.CART_EDIT) { block() }
    }
}
