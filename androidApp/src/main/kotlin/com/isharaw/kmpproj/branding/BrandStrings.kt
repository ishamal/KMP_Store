package com.isharaw.kmpproj.branding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.isharaw.kmpproj.core.Branding
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.branding.keells.R as KeelsR
import com.isharaw.kmpproj.branding.cargills.R as CargillsR
import com.isharaw.kmpproj.branding.glomark.R as GlomarkR

/**
 * Maps an [Experience] to its wordings. Each brand's strings live in its OWN resource module
 * (:core:branding:<brand>) with IDENTICAL keys (app_name, welcome, …) — distinct only by the module's
 * R namespace, which is what lets the keys be the same. The active brand's module is selected here at
 * runtime, so switching the experience swaps every string. Localizable per brand via the module's
 * res/values-<locale>/. It's @Composable because `stringResource` resolves against the configuration.
 */
@Composable
fun brandingFor(experience: Experience): Branding = when (experience) {
    Experience.KEELS -> Branding(
        appName = stringResource(KeelsR.string.app_name),
        welcome = stringResource(KeelsR.string.welcome),
        tagline = stringResource(KeelsR.string.tagline),
        loginCta = stringResource(KeelsR.string.login_cta),
    )
    Experience.CARGILLS -> Branding(
        appName = stringResource(CargillsR.string.app_name),
        welcome = stringResource(CargillsR.string.welcome),
        tagline = stringResource(CargillsR.string.tagline),
        loginCta = stringResource(CargillsR.string.login_cta),
    )
    Experience.GLOMARK -> Branding(
        appName = stringResource(GlomarkR.string.app_name),
        welcome = stringResource(GlomarkR.string.welcome),
        tagline = stringResource(GlomarkR.string.tagline),
        loginCta = stringResource(GlomarkR.string.login_cta),
    )
}
