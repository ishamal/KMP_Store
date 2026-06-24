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
