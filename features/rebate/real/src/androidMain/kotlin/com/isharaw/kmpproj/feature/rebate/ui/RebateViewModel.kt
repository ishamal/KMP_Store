package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.feature.rebate.RebateRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.Flow

@Inject
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(RebateViewModel::class)
class RebateViewModel(
    private val repository: RebateRepository,
) : MoleculeViewModel<RebateEvent, RebateState>() {

    @Composable
    override fun present(events: Flow<RebateEvent>): RebateState {
        val rebates = remember { repository.available() }
        val total = remember { repository.total() }
        return RebateState(
            rebates = rebates,
            total = total,
            onEvent = { /* no events yet */ },
        )
    }
}
