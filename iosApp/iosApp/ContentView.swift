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
            // Each tab is compiled in only when this store ships the feature (STORE_HAS_* flags come
            // from the flavor's xcconfig). The matching graph accessor is generated to match, so a
            // store without a feature has neither the UI nor the property — mirrors the Android tabs.
            #if STORE_HAS_CART
            CartView(repository: appGraph.cartRepository)
                .tabItem { Label("Cart", systemImage: "cart") }
            #endif
            #if STORE_HAS_ORDERS
            OrdersView(repository: appGraph.orderRepository)
                .tabItem { Label("Orders", systemImage: "shippingbox") }
            #endif
            #if STORE_HAS_INVOICES
            InvoicesView(repository: appGraph.invoiceRepository)
                .tabItem { Label("Invoices", systemImage: "doc.text") }
            #endif
            SettingsView(repository: appGraph.settingsRepository, userEmail: userEmail, onLogout: onLogout)
                .tabItem { Label("Settings", systemImage: "gear") }
        }
    }
}

// MARK: - Cart (storeA + storeB)

#if STORE_HAS_CART
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
#endif

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

// MARK: - Orders (storeA + storeC)

#if STORE_HAS_ORDERS
struct OrdersView: View {
    let repository: OrderRepository

    var body: some View {
        NavigationView {
            List(repository.all(), id: \.number) { order in
                HStack {
                    VStack(alignment: .leading) {
                        Text(order.number).fontWeight(.medium)
                        Text("\(order.itemCount) items · \(order.date)")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text(formatPrice(order.total)).fontWeight(.bold)
                        Text(orderStatusLabel(order.status))
                            .font(.caption)
                            .foregroundColor(orderStatusColor(order.status))
                    }
                }
            }
            .navigationTitle("Orders")
        }
    }
}

private func orderStatusLabel(_ status: OrderStatus) -> String {
    if status == OrderStatus.processing { return "Processing" }
    if status == OrderStatus.shipped { return "Shipped" }
    return "Delivered"
}

private func orderStatusColor(_ status: OrderStatus) -> Color {
    if status == OrderStatus.processing { return .orange }
    if status == OrderStatus.shipped { return .blue }
    return .green
}
#endif

// MARK: - Settings

struct SettingsView: View {
    let repository: SettingsRepository
    let userEmail: String
    let onLogout: () -> Void

    @State private var darkMode = false
    @State private var notifications = true

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
}

// MARK: - Helpers

private func formatPrice(_ value: Double) -> String {
    String(format: "$%.2f", value)
}
