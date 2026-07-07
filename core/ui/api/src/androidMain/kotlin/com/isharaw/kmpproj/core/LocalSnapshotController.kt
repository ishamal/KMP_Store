package com.isharaw.kmpproj.core

import androidx.compose.runtime.compositionLocalOf

/**
 * The active [SnapshotController] for runtime BU / Experience switching. Null before login (the
 * controller is only meaningful when a session exists). Settings reads this local to drive the BU
 * selector without threading parameters through DI.
 *
 * Uses `compositionLocalOf` (not `static`) so Settings only recomposes when the controller
 * reference changes — e.g. on logout — not on every snapshot update.
 */
val LocalSnapshotController = compositionLocalOf<SnapshotController?> { null }
