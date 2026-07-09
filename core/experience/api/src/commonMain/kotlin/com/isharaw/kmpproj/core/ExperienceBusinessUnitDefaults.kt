package com.isharaw.kmpproj.core

/**
 * The configured default [BusinessUnit] per [Experience] for this store build.
 *
 * Declared per store in the build-logic `STORES` table (Stores.kt) as a comma-separated list of
 * `EXPERIENCE:BUSINESS_UNIT` pairs (e.g. `businessUnitDefaults = "KEELS:USBL,CARGILLS:SENM"`), surfaced at runtime via
 * `BuildConfig.BUSINESS_UNIT_DEFAULTS`. The app graph parses and holds one instance.
 *
 * Use [defaultFor] to retrieve the pre-configured BU when the user first switches to a given
 * experience — this avoids the UI needing to hard-code "KEELS should start on USBL".
 */
class ExperienceBusinessUnitDefaults(
    private val defaults: Map<Experience, BusinessUnit>,
) {
    /** Returns the configured default BU for [experience], or `null` if not configured. */
    fun defaultFor(experience: Experience): BusinessUnit? = defaults[experience]

    companion object {
        val Empty = ExperienceBusinessUnitDefaults(emptyMap())

        /**
         * Parses `"KEELS:USBL,CARGILLS:SENM"` from `BuildConfig.BUSINESS_UNIT_DEFAULTS`.
         * Unknown/malformed pairs are silently skipped, so old builds survive schema extensions.
         */
        fun parse(raw: String): ExperienceBusinessUnitDefaults {
            if (raw.isBlank()) return Empty
            val map = raw.split(",").mapNotNull { pair ->
                val parts = pair.trim().split(":")
                if (parts.size != 2) return@mapNotNull null
                val exp = runCatching { Experience.valueOf(parts[0].trim()) }.getOrNull()
                    ?: return@mapNotNull null
                val bu = runCatching { BusinessUnit.valueOf(parts[1].trim()) }.getOrNull()
                    ?: return@mapNotNull null
                exp to bu
            }.toMap()
            return ExperienceBusinessUnitDefaults(map)
        }
    }
}
