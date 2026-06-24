package com.isharaw.kmpproj.core

import com.isharaw.kmpproj.core.access.BusinessUnit
import com.isharaw.kmpproj.core.access.UserRole

/**
 * The logged-in user for this app session. Carries the access-control identity granted at login —
 * the [businessUnit] and [userRole] — which `AccessControl` turns into the user's allowed features
 * via `allowedFeatures(businessUnit, userRole)`. [experience] is the legacy gating axis, kept until
 * it is migrated onto the business-unit / role model.
 */
data class Session(
    val email: String,
    val experience: Experience,
    val businessUnit: BusinessUnit,
    val userRole: UserRole,
)
