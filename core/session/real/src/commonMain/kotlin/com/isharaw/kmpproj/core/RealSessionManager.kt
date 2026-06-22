package com.isharaw.kmpproj.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Holds the current session for the app's lifetime. Backed by Compose state so the app shell
 * reacts to login/logout automatically (no callbacks threaded through navigation).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealSessionManager : SessionManager {
    override var session: Session? by mutableStateOf(null)
}
