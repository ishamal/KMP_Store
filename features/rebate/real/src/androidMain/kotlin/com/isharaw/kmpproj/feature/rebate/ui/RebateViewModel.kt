package com.isharaw.kmpproj.feature.rebate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.isharaw.kmpproj.core.CustomerScope
import com.isharaw.kmpproj.core.SessionManager
import com.isharaw.kmpproj.feature.rebate.RebateClient
import com.isharaw.kmpproj.feature.rebate.RebateSummary
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(CustomerScope::class, binding = binding<ViewModel>())
@ViewModelKey(RebateViewModel::class)
internal class RebateViewModel(
    private val rebateClient: RebateClient,
    private val sessionManager: SessionManager,
) : MoleculeViewModel<RebateEvent, RebateState>() {
    @Composable
    override fun present(): RebateState = rebatePresenter(rebateClient, sessionManager)
}

@Composable
internal fun rebatePresenter(
    rebateClient: RebateClient,
    sessionManager: SessionManager,
): RebateState {
    val scope = rememberCoroutineScope()

    // Pass the user's identity (business unit + role) into the state; the screen's AccessGate does the
    // capability ∩ permission check. No access guard is read in the UI.
    val session = sessionManager.session

    // Dashboard data (capability-gated inside summary()). `loading` until it returns; null = denied.
    var loading by remember { mutableStateOf(true) }
    var summary by remember { mutableStateOf<RebateSummary?>(null) }
    LaunchedEffect(Unit) {
        summary = rebateClient.summary()
        loading = false
    }

    return RebateState(
        businessUnit = session?.businessUnit,
        userRole = session?.userRole,
        loading = loading,
        summary = summary,
        onEvent = { event ->
            when (event) {
                RebateEvent.LogoutClick -> scope.launch { rebateClient.logout() }
                RebateEvent.RebateFunctionOneClick -> rebateClient.rebateFunctionOne()
                RebateEvent.RebateFunctionTwoClick -> rebateClient.rebateFunctionTwo()
                RebateEvent.RebateFunctionThreeClick -> rebateClient.rebateFunctionThree()
            }
        },
    )
}
