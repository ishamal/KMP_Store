package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.Capability
import com.isharaw.kmpproj.core.ExperienceScope
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.FeatureId
import com.isharaw.kmpproj.feature.rebate.RebateRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.Flow

@Inject
@ContributesIntoMap(ExperienceScope::class, binding = binding<ViewModel>())
@ViewModelKey(RebateViewModel::class)
class RebateViewModel(
    private val repository: RebateRepository,
    private val snapshot: ExperienceSnapshot,
) : MoleculeViewModel<RebateEvent, RebateState>() {

    @Composable
    override fun present(
        events: Flow<RebateEvent>,
    ): RebateState {
        val rebates = remember { repository.available() }
        val total = remember { repository.total() }

        // snapshot is immutable within this scope; freshness comes from VM recreation on switch.
        // Plain val (no remember{}) — staleness bug from the old ExperienceProvider.hasCapability
        // caching is fixed because the VM is recreated with a fresh snapshot on every BU switch.
        val canViewTotal = snapshot.hasCapability(FeatureId.REBATE, Capability.REBATE_VIEW_TOTAL.value)
        val canViewDaily = snapshot.hasCapability(FeatureId.REBATE, Capability.REBATE_VIEW_DAILY.value)

        return RebateState(
            rebates = rebates,
            total = total,
            canViewTotal = canViewTotal,
            canViewDaily = canViewDaily,
            onEvent = { /* no events yet */ },
        )
    }
}
