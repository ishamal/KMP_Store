package com.isharaw.kmpproj.core

/**
 * Holds the current [Session] (set at login). Public API contract; the implementation
 * (`RealSessionManager`) lives in a `:real` module.
 */
interface SessionManager {
    var session: Session?
}
