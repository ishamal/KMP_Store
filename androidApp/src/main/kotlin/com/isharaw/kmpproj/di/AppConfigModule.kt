package com.isharaw.kmpproj.di

import com.isharaw.kmpproj.BuildConfig
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.ExperienceBusinessUnitDefaults
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Bridges flavor-level [BuildConfig] constants into the Metro graph.
 * `BuildConfig.BUSINESS_UNIT_DEFAULTS` is a per-flavor string generated from
 * `config/stores/<store>.properties` (e.g. `"KEELS:USBL,CARGILLS:SENM"`).
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AppConfigModule {

    @Provides
    fun provideExperienceBusinessUnitDefaults(): ExperienceBusinessUnitDefaults =
        ExperienceBusinessUnitDefaults.parse(BuildConfig.BUSINESS_UNIT_DEFAULTS)
}
