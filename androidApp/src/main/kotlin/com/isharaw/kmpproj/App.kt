package com.isharaw.kmpproj

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.branding.FlavorDefaults
import com.isharaw.kmpproj.branding.brandColorsFor
import com.isharaw.kmpproj.branding.colorSchemeFor
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.isharaw.kmpproj.core.BusinessUnit
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceController
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.LocalBrandColorScheme
import com.isharaw.kmpproj.core.LocalExperienceController
import com.isharaw.kmpproj.core.LocalExperienceSnapshot
import com.isharaw.kmpproj.core.LocalFeatureActions
import com.isharaw.kmpproj.core.LocalNavigator
import com.isharaw.kmpproj.core.LocalSnapshotController
import com.isharaw.kmpproj.core.Navigator
import com.isharaw.kmpproj.core.SnapshotController
import com.isharaw.kmpproj.core.StubCapabilities
import com.isharaw.kmpproj.di.AppGraph
import com.isharaw.kmpproj.di.createAppGraph
import com.isharaw.kmpproj.feature.login.ui.LoginScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@Composable
fun App() {
    val graph = remember { createAppGraph() }
    // Observable session (RealSessionManager is Compose-backed): null → login, else → app.
    val session = graph.sessionManager.session

    // Activity-level ViewModelStoreOwner — used to clear stale VMs on login/logout/BU-switch.
    // Must be captured here (in Composition) before any clear() calls in click handlers.
    val viewModelStoreOwner = LocalViewModelStoreOwner.current

    // --- Single source of truth: the live snapshot ----------------------------------------
    // Reset to the session's snapshot whenever the session changes (login → new snapshot,
    // logout → null). Runtime BU/experience switches also update this var through the
    // SnapshotController, keeping currentExperience consistent without a second independent state.
    var currentSnapshot by remember(session) { mutableStateOf(session?.snapshot) }

    // currentExperience is DERIVED from the snapshot. This ensures a BU switch (which recomputes
    // the snapshot with a different experience) always keeps colors and gating in sync.
    val currentExperience = currentSnapshot?.experience ?: FlavorDefaults.defaultExperience

    // --- ExperienceScope graph extension --------------------------------------------------
    // Recreated whenever currentSnapshot changes (login, BU/experience switch, logout→null).
    // Non-null only while a snapshot exists (i.e. when the user is logged in).
    val experienceGraph = remember(currentSnapshot) {
        currentSnapshot?.let { graph.experienceGraphFactory.createExperienceGraph(it) }
    }

    // --- SnapshotController: one recompute path for both experience and BU switches ----------
    // Created once per session; captures graph (stable) and reads/writes currentSnapshot lazily
    // through its mutableState delegate.
    val snapshotController: SnapshotController? = if (session != null) {
        remember(session) {
            object : SnapshotController {
                override val currentBusinessUnit: BusinessUnit?
                    get() = currentSnapshot?.businessUnit

                override fun availableBusinessUnitsFor(experience: Experience): List<BusinessUnit> =
                    when (experience) {
                        Experience.KEELS -> listOf(BusinessUnit.CABL, BusinessUnit.USBL)
                        Experience.CARGILLS -> listOf(BusinessUnit.SENM)
                        Experience.GLOMARK -> emptyList()
                    }

                override fun switchBusinessUnit(businessUnit: BusinessUnit) {
                    recomputeAndLoad(
                        experience = currentSnapshot?.experience ?: return,
                        businessUnit = businessUnit,
                    )
                }

                override fun switchExperience(experience: Experience) {
                    // Prefer configured defaults; fall back to first available BU; keep current
                    // if none (e.g. GLOMARK has no selector, we retain whatever BU is live).
                    val newBu = graph.experienceBusinessUnitDefaults.defaultFor(experience)
                        ?: availableBusinessUnitsFor(experience).firstOrNull()
                        ?: currentSnapshot?.businessUnit
                        ?: BusinessUnit.USBL
                    recomputeAndLoad(experience = experience, businessUnit = newBu)
                }

                private fun recomputeAndLoad(experience: Experience, businessUnit: BusinessUnit) {
                    val role = currentSnapshot?.userRoles ?: return
                    val newSnapshot = graph.experienceProvider.getExperienceSnapshot(
                        experience = experience,
                        businessUnit = businessUnit,
                        userRoles = role,
                        capabilities = StubCapabilities.capabilitiesFor(businessUnit),
                        permission = StubCapabilities.permissionListFor(businessUnit),
                    )
                    // Clear stale VMs before the snapshot swaps so children always see fresh ones.
                    // This runs in an onClick handler (before the next recomposition), so it
                    // executes before any metroViewModel() call reads the store.
                    viewModelStoreOwner?.viewModelStore?.clear()
                    currentSnapshot = newSnapshot
                }
            }
        }
    } else null

    // ExperienceController: now delegates to SnapshotController so the visual brand and capability
    // gating stay in sync — both derive from the single currentSnapshot source of truth.
    val experienceController = remember(session) {
        object : ExperienceController {
            override val current: Experience get() = currentExperience
            override fun switch(experience: Experience) {
                snapshotController?.switchExperience(experience)
                // Before login the switcher is inaccessible; no-op is safe.
            }
        }
    }

    // Colors (Material scheme + custom) for the active experience — react to snapshot changes
    // because currentExperience is derived from currentSnapshot.
    val colorScheme = colorSchemeFor(currentExperience)
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            // Post-login: use the ExperienceScope factory (contains snapshot-scoped VMs).
            // Pre-login: fall back to the app-scoped factory.
            LocalMetroViewModelFactory provides (experienceGraph?.viewModelFactory
                ?: graph.metroViewModelFactory),
            LocalBrandColorScheme provides brandColorsFor(currentExperience),
            LocalExperienceController provides experienceController,
            LocalExperienceSnapshot provides currentSnapshot,
            LocalSnapshotController provides snapshotController,
            LocalFeatureActions provides graph.featureActions,
        ) {
            // Clear ViewModelStore on login (new session) and logout (null session). Runs in the
            // composition body (before children compose), so VMs are always recreated against the
            // current ExperienceScope factory rather than re-using stale instances.
            remember(session) {
                viewModelStoreOwner?.viewModelStore?.clear()
            }

            if (session == null) {
                LoginScreen(
                    validator = graph.loginValidator,
                    authenticator = graph.authenticator,
                    onLoginSuccess = { graph.sessionManager.session = it },
                )
            } else {
                MainScaffold(graph = graph, snapshot = currentSnapshot ?: session.snapshot)
            }
        }
    }
}

@Composable
private fun MainScaffold(graph: AppGraph, snapshot: ExperienceSnapshot) {
    // Tabs come from whatever features the flavor linked; ordered + filtered by the backend-resolved
    // features (a tab with no feature is always shown).
    val tabs = remember(snapshot) {
        graph.tabs
            .sortedBy { it.meta.order }
            .filter { tab ->
                val id = tab.meta.featureId
                id == null || snapshot.hasFeature(id)
            }
    }
    // No key on this remember: tabs may shrink/grow when the BU changes, but we never want
    // a BU switch to reset the back stack — that would eject the user from Settings (the very
    // screen hosting the BU switcher). The back stack persists for the lifetime of the session;
    // the tab bar filters reactively off the separately-keyed `tabs` derived state.
    val backStack = remember { mutableStateListOf<NavKey>(tabs.first().key) }

    // Back-stack-backed Navigator so screens (e.g. Settings) can open non-tab destinations.
    val navigator = remember(backStack) {
        object : Navigator {
            override fun goTo(key: NavKey) { backStack.add(key) }
            override fun back() { backStack.removeLastOrNull() }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = backStack.lastOrNull() == tab.key,
                        onClick = { backStack.clear(); backStack.add(tab.key) },
                        icon = { Text(tab.meta.icon) },
                        label = { Text(tab.meta.label) },
                    )
                }
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalNavigator provides navigator) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(padding),
                onBack = { backStack.removeLastOrNull() },
                // Every linked feature registers its own entries.
                entryProvider = entryProvider { graph.entryInstallers.forEach { it() } },
            )
        }
    }
}
