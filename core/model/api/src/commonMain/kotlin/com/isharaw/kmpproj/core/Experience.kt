package com.isharaw.kmpproj.core

/**
 * The in-app **experience** a logged-in user gets. This is a RUNTIME concept (known only after
 * login), unlike a store which is fixed at build time. What each experience can do is declared
 * centrally in [ExperienceCatalog] — code asks for [Capability]s, it never compares experiences.
 */
enum class Experience { USBL, CABL }
