package com.isharaw.kmpproj.core.experience.real

import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.ExperienceScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * Concrete [MetroViewModelFactory] for the [ExperienceScope] graph extension.
 *
 * Mirrors [com.isharaw.kmpproj.di.AppViewModelFactory] but scoped to [ExperienceScope].
 * **No `@ContributesBinding`** — the child graph inherits the parent's [MetroViewModelFactory]
 * binding (AppViewModelFactory); re-binding the supertype would be a duplicate-binding error.
 * The factory is exposed as a concrete-typed accessor on [ExperienceGraph] instead.
 */
@Inject
@SingleIn(ExperienceScope::class)
class ExperienceViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
