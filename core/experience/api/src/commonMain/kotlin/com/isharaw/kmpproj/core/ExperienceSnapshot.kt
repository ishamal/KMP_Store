package com.isharaw.kmpproj.core

/**
 * The access **snapshot** the backend resolves for a logged-in user (sent down at login). It bundles
 * who the user is and what they may use, so the app doesn't compute access itself — it just reads
 * [resolvedFeatures].
 *
 * @param experience the store brand (Keels / Cargills / Glomark).
 * @param businessUnit the user's business unit (CABL / USBL / SENM).
 * @param userRoles the roles the user holds.
 * @param resolvedFeatures the features the backend resolved as available to this user; the app shows a
 *   feature's tab only if its [Feature] is in here.
 */
data class ExperienceSnapshot(
    val experience: Experience,
    val businessUnit: BusinessUnit,
    val userRoles: UserRole,
    val resolvedFeatures: Set<Feature>,
)
