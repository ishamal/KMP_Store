package com.isharaw.kmpproj

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.branding.FlavorDefaults
import com.isharaw.kmpproj.branding.brandColorsFor
import com.isharaw.kmpproj.branding.colorSchemeFor
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.ExperienceController
import com.isharaw.kmpproj.core.ExperienceSnapshot
import com.isharaw.kmpproj.core.LocalBrandColorScheme
import com.isharaw.kmpproj.core.LocalExperienceController
import com.isharaw.kmpproj.core.LocalNavigator
import com.isharaw.kmpproj.core.Navigator
import com.isharaw.kmpproj.di.AppGraph
import com.isharaw.kmpproj.di.createAppGraph
import com.isharaw.kmpproj.feature.login.ui.LoginScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@Composable
fun App() {
    val graph = remember { createAppGraph() }
    // Observable session (RealSessionManager is Compose-backed): null → login, else → app.
    val session = graph.sessionManager.session

    // The active store brand. Seeded from the flavor's pinned default (the brand shown before login),
    // then follows the logged-in snapshot — and can be switched in-app via LocalExperienceController.
    var currentExperience by remember { mutableStateOf(FlavorDefaults.defaultExperience) }
    LaunchedEffect(session) {
        // Adopt the session's resolved experience on login; fall back to the flavor default on logout.
        currentExperience = session?.snapshot?.experience ?: FlavorDefaults.defaultExperience
    }
    val experienceController = remember {
        object : ExperienceController {
            override val current: Experience get() = currentExperience
            override fun switch(experience: Experience) { currentExperience = experience }
        }
    }

    // Colors (Material scheme + custom) and wordings for the active experience are resolved per
    // Experience, so switching the experience re-themes the whole app — including feature screens.
    val colorScheme = colorSchemeFor(currentExperience)
    MaterialTheme(colorScheme = colorScheme) {
        // Make Metro's VM factory + the active brand (wordings, custom colors) + store switcher
        // available to any screen below.
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            LocalBrandColorScheme provides brandColorsFor(currentExperience),
            LocalExperienceController provides experienceController,
        ) {
            // Keep the app-scoped reader in sync with the session: load the snapshot when home loads,
            // clear it on logout (so one user's snapshot can't leak to the next). Runs synchronously on
            // session change, before any child reads the reader.
            remember(session) {
                val current = session
                if (current != null) graph.experienceReader.load(current.snapshot)
                else graph.experienceReader.clear()
            }

            if (session == null) {
                LoginScreen(
                    validator = graph.loginValidator,
                    authenticator = graph.authenticator,
                    onLoginSuccess = { graph.sessionManager.session = it },
                )
            } else {
                MainScaffold(graph = graph, snapshot = session.snapshot)
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
    val backStack = remember(tabs) { mutableStateListOf<NavKey>(tabs.first().key) }

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
