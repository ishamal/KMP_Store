package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.AppScope
import com.isharaw.kmpproj.core.ExperienceProvider
import com.isharaw.kmpproj.core.FeatureId
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
    private val experience: ExperienceProvider,
) : MoleculeViewModel<RebateEvent, RebateState>() {

    @Composable
    override fun present(events: Flow<RebateEvent>): RebateState {
        val rebates = remember { repository.available() }
        val total = remember { repository.total() }

        // Function-level restriction: each rebate function is shown only if its capability is present.
        val canViewTotal = remember { experience.hasCapability(FeatureId.REBATE, "rebate.view.total") }
        val canViewDaily = remember { experience.hasCapability(FeatureId.REBATE, "rebate.view.daily") }

        return RebateState(
            rebates = rebates,
            total = total,
            canViewTotal = canViewTotal,
            canViewDaily = canViewDaily,
            onEvent = { /* no events yet */ },
        )
    }
}
