package com.isharaw.kmpproj.core.access

/**
 * A **user role** within a shop (every shop has these roles). It grants a set of [Permission]
 * (see [AccessControl.permissionsOf]).
 */
enum class UserRole { ADMIN, CUSTOMER_ADMIN, MANAGER, USER }
