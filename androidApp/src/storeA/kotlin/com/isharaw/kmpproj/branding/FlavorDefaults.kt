package com.isharaw.kmpproj.branding

import com.isharaw.kmpproj.core.Experience

/**
 * KEELS (storeA) flavor defaults. One copy of this object lives in each flavor source set
 * (androidApp/src/<store>/kotlin/...); the active flavor's version is compiled in.
 *
 * It pins only the **default experience** for this build — the brand shown before login (and the
 * starting point for in-app switching). The actual colors/wordings for every experience live in the
 * runtime registry (`core/ui` BrandTheme), so any store can be themed at runtime. Add a store → add
 * its flavor source set with a FlavorDefaults pinning its experience.
 */
object FlavorDefaults {
    val defaultExperience = Experience.KEELS
}
