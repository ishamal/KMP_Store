package com.isharaw.kmpproj.core.experience.real

import com.isharaw.kmpproj.core.ExperienceScope
import com.isharaw.kmpproj.core.ExperienceSnapshot
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings

/**
 * Metro graph extension scoped to one resolved [ExperienceSnapshot]. Created by [Factory] whenever
 * the snapshot changes (login or BU/experience switch), dropped at logout.
 *
 * Extends [MetroViewModelMultibindings] so Metro wires the three ViewModel multibinding maps for
 * this scope; snapshot-consuming ViewModels contribute into [ExperienceScope] and receive a
 * non-null [ExperienceSnapshot] at construction time.
 */
@GraphExtension(ExperienceScope::class)
interface ExperienceGraph : MetroViewModelMultibindings {
    /** The resolved snapshot bound for this scope (non-null, passed via the [Factory]). */
    val snapshot: ExperienceSnapshot

    /** Concrete factory this scope exposes; provide via [LocalMetroViewModelFactory] in the shell. */
    val viewModelFactory: ExperienceViewModelFactory

    @GraphExtension.Factory
    interface Factory {
        fun createExperienceGraph(@Provides snapshot: ExperienceSnapshot): ExperienceGraph
    }
}
