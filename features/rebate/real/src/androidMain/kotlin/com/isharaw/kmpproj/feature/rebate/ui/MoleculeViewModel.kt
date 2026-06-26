package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Base ViewModel that produces a [StateFlow] of [Model] from a Compose [present] presenter
 * (Cash App Molecule pattern). The state carries its own `onEvent` callback (events are handled
 * inside the presenter), so `present()` takes no parameters. Pilot location — promote to a shared
 * Android module to reuse.
 */
abstract class MoleculeViewModel<Event, Model> : ViewModel() {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
        scope.launchMolecule(mode = RecompositionMode.ContextClock) { present() }
    }

    @Composable
    protected abstract fun present(): Model
}
