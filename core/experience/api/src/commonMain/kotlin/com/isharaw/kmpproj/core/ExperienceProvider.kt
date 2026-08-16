package com.isharaw.kmpproj.core

/**
 * Stateless snapshot builder. Computes an [ExperienceSnapshot] from the inputs resolved at login
 * or on a runtime BU/experience switch.
 *
 * The live snapshot is no longer held here — it is bound non-null inside the
 * [com.isharaw.kmpproj.core.experience.real.ExperienceGraph] extension, recreated per switch and
 * dropped at logout. Non-Compose observers should subscribe to
 * [com.isharaw.kmpproj.core.experience.real.ExperienceGraph.snapshot] instead.
 */
interface ExperienceProvider {

    /**
     * Builds the snapshot for a login or a runtime BU/experience switch. The **effective**
     * capabilities are the intersection of [capabilities] (what the user is granted) and
     * [permission] (what is permitted) — only keys present in **both** are kept. Features are then
     * derived from that effective set (a capability's prefix names its feature, e.g. `"cart.view"`
     * ⇒ the CART feature). Pure — does **not** load or store the result.
     */
    fun getExperienceSnapshot(
        experience: Experience,
        businessUnit: BusinessUnit,
        userRoles: UserRole,
        capabilities: Set<String>,
        permission: Set<String>,
    ): ExperienceSnapshot
}
