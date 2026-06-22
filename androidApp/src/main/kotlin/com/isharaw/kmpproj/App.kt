package com.isharaw.kmpproj

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.isharaw.kmpproj.branding.BrandColors
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.isharaw.kmpproj.core.Branding
import com.isharaw.kmpproj.core.Experience
import com.isharaw.kmpproj.core.LocalBranding
import com.isharaw.kmpproj.core.LocalExperience
import com.isharaw.kmpproj.core.LocalNavigator
import com.isharaw.kmpproj.core.Navigator
import com.isharaw.kmpproj.core.has
import com.isharaw.kmpproj.di.AppGraph
import com.isharaw.kmpproj.di.createAppGraph
import com.isharaw.kmpproj.feature.login.ui.LoginScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@Composable
fun App() {
    // Per-store brand colors come from androidApp/src/<store>/kotlin/.../BrandColors.kt (flavor
    // source set). Applied once, so every screen (incl. feature screens) picks up the active theme.
    val brandColors = lightColorScheme(
        primary = BrandColors.primary,
        secondary = BrandColors.secondary,
    )
    // Per-store wordings from the active flavor's strings.xml, published for feature screens to read.
    val branding = Branding(
        appName = stringResource(R.string.app_name),
        welcome = stringResource(R.string.welcome_message),
    )
    MaterialTheme(colorScheme = brandColors) {
        val graph = remember { createAppGraph() }
        // Make Metro's VM factory + per-store branding available to any screen below.
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            LocalBranding provides branding,
        ) {
            // Observable session (RealSessionManager is Compose-backed): null → login, else → app.
            val session = graph.sessionManager.session

            if (session == null) {
                LoginScreen(
                    validator = graph.loginValidator,
                    authenticator = graph.authenticator,
                    onLoginSuccess = { graph.sessionManager.session = it },
                )
            } else {
                // Publish the logged-in experience once; capability-aware UI reads it ambiently.
                CompositionLocalProvider(LocalExperience provides session.experience) {
                    MainScaffold(graph = graph, experience = session.experience)
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(graph: AppGraph, experience: Experience) {
    // Tabs come from whatever features the flavor linked; ordered + capability-filtered.
    val tabs = remember(experience) {
        graph.tabs
            .sortedBy { it.meta.order }
            .filter { tab ->
                val cap = tab.meta.requiredCapability
                cap == null || experience.has(cap)
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
