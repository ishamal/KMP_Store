import SwiftUI
import Shared

/// **iOS (SwiftUI) capability wrapper** — the counterpart of the Android Compose `CapabilityGate`.
/// Renders `content` only if `snapshot` grants `capability` (a dotted key, e.g. "order.create").
///
/// Both platforms share the same Kotlin check: `ExperienceSnapshot.hasCapability` (now an O(1) member
/// on the snapshot, exported from the `Shared` framework).
///
/// ```swift
/// CapabilityGate(snapshot: snapshot, capability: "order.create") {
///     Button("New order") { … }
/// }
/// ```
struct CapabilityGate<Content: View>: View {
    let snapshot: ExperienceSnapshot
    let capability: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        if snapshot.hasCapability(capability: capability) {
            content()
        }
    }
}
