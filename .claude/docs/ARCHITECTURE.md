entry: androidApp — MainActivity.kt → App.kt (Compose root). App.kt creates the Metro graph, observes graph.sessionManager.session (null → LoginScreen, else → MainScaffold with Navigation3 tabs). iOS entry: iosApp/ + shared module. Per-store builds via product flavors (storeA/storeB/storeC); pick the variant in Android Studio's Build Variants panel.

dirs:
  - core/<name>/{api,real}: api = interfaces + pure types; real = implementations + Compose UI + Metro bindings. Modules: di, model, experience, session, navigation, ui.
    - core/model: domain types; re-exports core/experience (Experience, BusinessUnit, UserRole, Feature, ExperienceSnapshot).
    - core/experience: ExperienceSnapshot (access model) + ExperienceResolver (builds snapshot at login) + ExperienceReader (app-scoped set-later holder; load() on home, clear() on logout).
    - core/ui: shared Android Compose helpers (FeatureGate, CapabilityGate, formatPrice) + branding theme (BrandColorScheme, LocalBrandColorScheme, AppTheme, ExperienceController). Avoids the compose-MPP plugin; depends on compose.ui for Color.
  - features/<name>/{api,real}: login, cart, invoices, settings, orders, rebate, passwordReset (+ features/test). Each :real registers tabs/nav entries + Metro bindings; flavors link only the :real modules their store lists.
  - androidApp/src/main: App shell + branding values (src/main/kotlin/.../branding/: per-brand color files KeelsColors/CargillsColors/GlomarkColors + BrandPalette mappers). src/<store>/ = flavor source sets (FlavorDefaults.kt pins the default Experience; res/values/strings.xml holds the launcher app_name only).
  - config/stores/<store>.properties: store name + feature list (single source of truth, read by buildSrc/StoreManifest.kt).
  - shared/: KMP shared (iOS bridge). buildSrc/: StoreManifest. _lean_dev_sessions/: prior session notes.

pattern:
  - Access/gating: backend (stubbed) resolves an ExperienceSnapshot at login (experience + businessUnit + role + resolvedFeatures). UI gates via snapshot.hasFeature/hasCapability and FeatureGate/CapabilityGate. Tabs filtered by resolved features AND which feature modules the flavor links.
  - Experience = store brand (KEELS/CARGILLS/GLOMARK). Theming is runtime, keyed by the active Experience: App holds currentExperience state (seeded from FlavorDefaults, synced to session.snapshot.experience, switchable in-app via ExperienceController / Settings switcher). brandColorsFor(experience) → BrandColorScheme provided via LocalBrandColorScheme; colorSchemeFor(experience) derives the Material ColorScheme. Read colors anywhere via AppTheme.colors.* (custom roles) or MaterialTheme.colorScheme (Material slots).
  - api/real split keeps feature impls swappable and flavor-trimmable; Metro @ContributesBinding wires real → api.

infra: no backend/CI wired yet. Stubbed auth (StubLoginData). Single Gradle build; per-store APKs via flavors (applicationId = com.isharaw.kmpproj.<store>).
