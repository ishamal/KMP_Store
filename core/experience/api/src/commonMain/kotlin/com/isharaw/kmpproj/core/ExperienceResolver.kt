package com.isharaw.kmpproj.core

/**
 * Builds the [ExperienceSnapshot] for a login. **App-scoped / stateless** because it runs *at login* —
 * before the customer (logged-in) scope exists — and its result is what creates that scope.
 * Implemented by `RealExperienceResolver`.
 */
interface ExperienceResolver {
    /**
     * Resolves the snapshot from a successful login. The **effective** capabilities are the
     * intersection of [capabilities] (what the user is granted) and [permitionList] (what is permitted)
     * — only keys present in **both** are kept. Features are then derived from that effective set
     * (a capability's prefix names its feature, e.g. `"cart.view"` ⇒ the CART feature).
     */
    fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRole: UserRole,
        capabilities: Set<String>,
        permitionList: Set<String>,
    ): ExperienceSnapshot
}
