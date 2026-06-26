package com.isharaw.kmpproj.di

import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.CustomerScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * [MetroViewModelFactory] for the **customer** child graph — the [CustomerScope] counterpart of
 * [AppViewModelFactory]. Built from the customer-scoped ViewModel multibinding maps (filled by
 * `@ContributesIntoMap(CustomerScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`), so
 * [CustomerGraph.metroViewModelFactory] resolves and `metroViewModel()` can create customer VMs.
 */
@Inject
@SingleIn(CustomerScope::class)
@ContributesBinding(CustomerScope::class)
class CustomerViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
