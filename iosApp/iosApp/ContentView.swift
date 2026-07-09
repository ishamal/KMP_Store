import SwiftUI
import Shared

// Single composition root: the Metro graph for this store. Created once.
// `createGraph` is a compile-time intrinsic, so we go through the exported factory function.
let appGraph: IosAppGraph = IosAppGraphKt.createIosAppGraph()

// MARK: - Root

struct ContentView: View {
    @State private var user: String? = nil

    var body: some View {
        if let user = user {
            MainTabView(userEmail: user, onLogout: { self.user = nil })
        } else {
            LoginView(validator: appGraph.loginValidator, onLoginSuccess: { self.user = $0 })
        }
    }
}

// MARK: - Login

struct LoginView: View {
    let validator: LoginValidator
    let onLoginSuccess: (String) -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var error: String? = nil

    var body: some View {
        VStack(spacing: 16) {
            Text("Welcome back").font(.title).bold()

            TextField("Email", text: $email)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            if let error = error {
                Text(error).foregroundColor(.red).font(.footnote)
            }

            Button("Log in") {
                if let validationError = validator.validate(email: email, password: password) {
                    error = validationError
                } else {
                    onLoginSuccess(email.trimmingCharacters(in: .whitespaces))
                }
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
        }
        .padding(24)
    }
}

// MARK: - Main tabs

struct MainTabView: View {
    let userEmail: String
    let onLogout: () -> Void

    var body: some View {
        TabView {
            CartView(repository: appGraph.cartRepository)
                .tabItem { Label("Cart", systemImage: "cart") }
            #if STORE_HAS_INVOICES
            InvoicesView(repository: appGraph.invoiceRepository)
                .tabItem { Label("Invoices", systemImage: "doc.text") }
            #endif
            SettingsView(
                repository: appGraph.settingsRepository,
                featureActions: appGraph.featureActions,
                userEmail: userEmail,
                onLogout: onLogout
            )
            .tabItem { Label("Settings", systemImage: "gear") }
        }
    }
}

// MARK: - Cart

struct CartView: View {
    let repository: CartRepository
    @State private var items: [CartItem] = []

    var body: some View {
        NavigationView {
            VStack {
                List {
                    ForEach(items, id: \.id) { item in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(item.name).fontWeight(.medium)
                                Spacer()
                                Text(formatPrice(item.lineTotal))
                            }
                            HStack {
                                Button("-") { repository.decrement(id: item.id); refresh() }
                                    .buttonStyle(.bordered)
                                Text("\(item.quantity)").padding(.horizontal, 8)
                                Button("+") { repository.increment(id: item.id); refresh() }
                                    .buttonStyle(.bordered)
                                Spacer()
                                Button("Remove") { repository.remove(id: item.id); refresh() }
                                    .foregroundColor(.red)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                HStack {
                    Text("Total").fontWeight(.bold)
                    Spacer()
                    Text(formatPrice(repository.total)).fontWeight(.bold)
                }
                .padding()
            }
            .navigationTitle("Cart")
            .onAppear { refresh() }
        }
    }

    private func refresh() { items = repository.items }
}

// MARK: - Invoices (storeA only)

#if STORE_HAS_INVOICES
struct InvoicesView: View {
    let repository: InvoiceRepository

    var body: some View {
        NavigationView {
            List(repository.all(), id: \.number) { invoice in
                HStack {
                    VStack(alignment: .leading) {
                        Text(invoice.number).fontWeight(.medium)
                        Text(invoice.date).font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text(formatPrice(invoice.amount)).fontWeight(.bold)
                        Text(statusLabel(invoice.status))
                            .font(.caption)
                            .foregroundColor(statusColor(invoice.status))
                    }
                }
            }
            .navigationTitle("Invoices")
        }
    }
}

private func statusLabel(_ status: InvoiceStatus) -> String {
    if status == InvoiceStatus.paid { return "Paid" }
    if status == InvoiceStatus.pending { return "Pending" }
    return "Overdue"
}

private func statusColor(_ status: InvoiceStatus) -> Color {
    if status == InvoiceStatus.paid { return .green }
    if status == InvoiceStatus.pending { return .orange }
    return .red
}
#endif

// MARK: - Settings

struct SettingsView: View {
    let repository: SettingsRepository
    /// The DI-contributed feature actions for this store (bridged from Kotlin `NSSet<IosFeatureAction>`).
    /// Passed from the graph so `SettingsView` never hard-codes which optional features exist —
    /// the set is empty in stores that ship no contributing feature.
    let featureActions: Set<IosFeatureAction>
    let userEmail: String
    let onLogout: () -> Void

    @State private var darkMode = false
    @State private var notifications = true

    /// Actions targeting the Settings slot, sorted by their declared order.
    /// `order` is `Int32` (bridged from Kotlin `int32_t`), directly `Comparable` in Swift.
    private var settingsActions: [IosFeatureAction] {
        featureActions
            .filter { $0.slot == FeatureSlot.settings }
            .sorted { $0.order < $1.order }
    }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    Text("Signed in as").font(.caption)
                    Text(userEmail).fontWeight(.medium)
                }
                Section {
                    Toggle("Dark mode", isOn: $darkMode)
                    Toggle("Notifications", isOn: $notifications)
                }
                // Store-gated feature actions — contributed via DI multibinding from `:real`
                // modules; section is absent in stores that ship no contributing feature.
                if !settingsActions.isEmpty {
                    Section("Features") {
                        ForEach(settingsActions, id: \.label) { action in
                            NavigationLink(action.label) {
                                featureDestination(for: action)
                            }
                        }
                    }
                }
                Section {
                    Button("Log out", action: onLogout).foregroundColor(.red)
                }
            }
            .navigationTitle("Settings")
            .onAppear {
                darkMode = repository.darkMode
                notifications = repository.notifications
            }
            .onChange(of: darkMode) { value in repository.darkMode = value }
            .onChange(of: notifications) { value in repository.notifications = value }
        }
    }

    /// Route each action kind to its destination view. Kotlin enums bridge as ObjC reference types,
    /// so compare with `==` (pointer/isEqual:) rather than Swift pattern matching.
    @ViewBuilder
    private func featureDestination(for action: IosFeatureAction) -> some View {
        if action.kind == FeatureKind.rebate {
            RebateView()
        } else if action.kind == FeatureKind.passwordReset {
            PasswordResetView()
        } else {
            Text(action.label)
        }
    }
}

// MARK: - Rebate (store-gated: storeA + storeB)
// Pushed via NavigationLink from SettingsView, so no nested NavigationView here.

struct RebateView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "percent")
                .font(.system(size: 48))
                .foregroundColor(.accentColor)
            Text("Your Rebates")
                .font(.title2)
                .fontWeight(.semibold)
            Text("Rebate details will appear here once the backend is wired.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .navigationTitle("Rebates")
    }
}

// MARK: - Password Reset (store-gated: storeA)
// Pushed via NavigationLink from SettingsView, so no nested NavigationView here.

struct PasswordResetView: View {
    @State private var email = ""
    @State private var submitted = false

    var body: some View {
        Form {
            Section(header: Text("Account email")) {
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
            }
            Section {
                Button("Send reset link") { submitted = true }
                    .disabled(email.isEmpty)
            }
            if submitted {
                Section {
                    Text("If an account exists for \(email), a reset link has been sent.")
                        .foregroundColor(.secondary)
                        .font(.footnote)
                }
            }
        }
        .navigationTitle("Reset Password")
    }
}

// MARK: - Helpers

private func formatPrice(_ value: Double) -> String {
    String(format: "$%.2f", value)
}
