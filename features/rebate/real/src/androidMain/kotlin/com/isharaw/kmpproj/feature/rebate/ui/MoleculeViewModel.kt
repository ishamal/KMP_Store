package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Base ViewModel that produces a [StateFlow] of [Model] from a Compose [present] presenter
 * (Cash App Molecule pattern). Pilot location — promote to a shared Android module to reuse.
 */
abstract class MoleculeViewModel<Event, Model> : ViewModel() {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 20)

    val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
        scope.launchMolecule(mode = RecompositionMode.ContextClock) { present(events) }
    }

    fun take(event: Event) {
        check(events.tryEmit(event)) { "Event buffer overflow." }
    }

    @Composable
    protected abstract fun present(events: Flow<Event>): Model
}
