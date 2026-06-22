package com.isharaw.kmpproj.core

/** Presentation-layer currency formatting for the Android UI, shared across feature screens. */
fun formatPrice(value: Double): String {
    val cents = Math.round(value * 100)
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$$whole.$fraction"
}
