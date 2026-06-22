package com.isharaw.kmpproj.di

import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * Concrete [MetroViewModelFactory] for the app graph. metrox ships the multibinding maps
 * (populated by `@ContributesIntoMap(binding = binding<ViewModel>())` + `@ViewModelKey` on each VM);
 * we subclass + contribute so [com.isharaw.kmpproj.di.AppGraph.metroViewModelFactory] resolves and
 * `metroViewModel()` can create VMs.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AppViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
