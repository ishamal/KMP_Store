package com.isharaw.kmpproj.core

/**
 * Bottom-bar presentation for a top-level Navigation 3 destination. Stored in `NavEntry.metadata`
 * under [KEY]; the app shell reads it to build the navigation bar and apply capability gating.
 */
data class TabMeta(
    val label: String,
    val icon: String,
    val order: Int,
    val requiredCapability: Capability? = null,
) {
    companion object {
        const val KEY = "tab"
    }
}
