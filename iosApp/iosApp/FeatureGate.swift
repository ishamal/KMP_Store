import SwiftUI
import Shared

/// **iOS (SwiftUI) feature wrapper** — the counterpart of the Android Compose `FeatureGate`, and the
/// coarse partner of `CapabilityGate`. Renders `content` only if `snapshot` has the feature `featureId`.
///
/// Shares the same Kotlin check: `ExperienceSnapshot.hasFeature` (exported from the `Shared` framework).
///
/// ```swift
/// FeatureGate(snapshot: snapshot, featureId: .orders) {
///     OrdersSummaryCard(...)
/// }
/// ```
struct FeatureGate<Content: View>: View {
    let snapshot: ExperienceSnapshot
    let featureId: FeatureId
    @ViewBuilder let content: () -> Content

    var body: some View {
        if snapshot.hasFeature(featureId: featureId) {
            content()
        }
    }
}
