1. Overview
   The app has two orthogonal systems that work together:

System	What it controls	Where it lives
FeatureActions	Navigation — which screens exist, where they appear (More tab, Home tab, etc.)	core/feature/api
FeaturePermissions	Visibility — whether a screen/section is shown to the logged-in user based on their role, business unit, and capabilities	core/featurePermission/api
Both systems are fully decoupled from the UI. Screens do not import feature lists or check permissions directly — they are injected or consumed through CompositionLocal.

2. Key Concepts Glossary
   FeatureActions
   kotlin
   core/feature/api/src/androidMain/…/navigation/FeaturesActions.kt
   class FeatureActions(
   val availableSections: Set<AvailableSections>, // which tabs this feature appears in
   val featureKey: FeatureKEY,                    // identifies the feature (REBATE, ORDER, …)
   val target: NavKey,                            // the nav destination to navigate to
   val order: Int = 0,                            // display order within the section
   )

enum class AvailableSections { MORE, HOME, CART, ACCOUNT, HOME_TAB, NONE }
enum class FeatureKEY { REBATE, ORDER }
A FeatureActions object is a build-time declaration that says:

"The Rebate feature exists, it should appear in the MORE and HOME_TAB sections, and tapping it navigates to RebateRoute."
EntryProviderInstaller
kotlin
core/feature/api/…/EntryProviderInstaller.kt
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit
A lambda that registers a nav entry (screen) inside a NavDisplay. Each feature provides one via DI so that the home scaffold remains unaware of which features are installed.

FeatureId & Feature
kotlin
core/featurePermission/api/…/Feature.kt
enum class FeatureId(val key: String, val displayName: String) {
REBATE("rebate", "Rebate"),
ORDERS("order", "Orders"),
CART("cart", "Cart"),
// …
}

data class Feature(
val featureId: FeatureId,
val featureName: String,
val resolvedCapabilities: Set<String>, // e.g. {"rebate.view", "rebate.total"}
)
FeatureId links a permission capability string (e.g. "rebate.view") to a named feature. It is the bridge between the backend permission system and the UI.

ExperienceSnapshot
kotlin
core/featurePermission/api/…/ExperienceSnapshot.kt
data class ExperienceSnapshot(
val experience: Experience,           // SYSCO_SHOP | NEW_PORT | SYSCO_COMMERCE
val businessUnit: BusinessUnit,       // USBL | CABL | SENM
val userRoles: UserRole,              // ADMIN | MANAGER | USER | CUSTOMER_ADMIN
val resolvedFeatures: Set<Feature>,   // features the user is allowed to see
)
An immutable snapshot computed at login time (and when the user switches experience). It answers: "Given this user's role and capabilities, which features should be visible?"

ExperienceProvider
kotlin
core/featurePermission/api/…/ExperienceProvider.kt
interface ExperienceProvider {
val snapshot: ExperienceSnapshot?
val snapshotFlow: StateFlow<ExperienceSnapshot?>

fun getExperienceSnapshot(experience, businessUnit, userRoles, capabilities, permission): ExperienceSnapshot
fun load(experienceSnapshot: ExperienceSnapshot)
fun clear()
}
A singleton (AppScope) that holds the live snapshot. Components observe snapshotFlow to react to experience switches.

3. FeatureActions — Navigation Registration
   How it works
   Each feature module self-registers its nav entry and its FeatureActions descriptor into DI using Metro's @IntoSet multi-binding. No central registry needs updating.

┌──────────────────────────────────────────────────────────────┐
│  Feature Module (e.g. feature/order/real)                    │
│                                                              │
│  OrderModule (@ContributesTo CustomerScope)                  │
│    @IntoSet  provideOrderEntry() → EntryProviderInstaller    │
│    @IntoSet  orderFeature()      → FeatureActions            │
└──────────────────────────────────────────────────────────────┘
│ Metro collects all @IntoSet contributions
▼
┌──────────────────────────────────────────────────────────────┐
│  CustomerGraph (CustomerScope)                               │
│    Set<EntryProviderInstaller>  ← all nav entries            │
│    Set<FeatureActions>          ← all feature descriptors    │
└──────────────────────────────────────────────────────────────┘
│ injected into
▼
┌──────────────────────────────────────────────────────────────┐
│  MoreModule (CustomerScope)                                  │
│    receives Set<FeatureActions>, passes to moreEntry()       │
│    moreEntry filters by AvailableSections.MORE               │
└──────────────────────────────────────────────────────────────┘
Order module example
kotlin
feature/order/real/…/di/OrderModule.kt
@BindingContainer
@ContributesTo(CustomerScope::class)
object OrderModule {

// 1. Register the nav screen
@Provides @IntoSet
fun provideOrderEntry(): EntryProviderInstaller = {
orderEntry()       // extension on EntryProviderScope<NavKey>
}

// 2. Register the feature descriptor (appears in MORE tab, order=0)
@Provides @IntoSet
fun orderFeature(): FeatureActions = FeatureActions(
target            = OrderRoute,
order             = 0,
availableSections = setOf(AvailableSections.MORE),
featureKey        = FeatureKEY.ORDER,
)
}
Filtering by section
moreEntry receives the full Set<FeatureActions> and filters down to only those declared for the MORE section:

kotlin
feature/more/real/…/navigation/moreEntry.kt
fun EntryProviderScope<NavKey>.moreEntry(
features: Set<FeatureActions>,
) = entry<MoreRoute> {
MoreScreen(
features = features
.filter { AvailableSections.MORE in it.availableSections }
.sortedBy { it.order },
)
}
4. FeaturePermissions — Runtime Access Control
   How permissions are resolved
   At login the app knows:

The user's BusinessUnit (e.g. USBL) → determines which capabilities the account has
The user's UserRole (e.g. MANAGER) → determines which capabilities the role permits
The Experience (e.g. SYSCO_SHOP) → determines which brand is active
RealExperienceProvider.getExperienceSnapshot() computes the intersection of account capabilities and role permissions:

kotlin
core/featurePermission/real/…/RealExperienceProvider.kt
val interceptedCapabilities = capabilities intersect permission

val resolvedFeatures = interceptedCapabilities
.groupBy { FeatureId.fromCapability(it) }      // "rebate.view" → FeatureId.REBATE
.mapNotNullTo(mutableSetOf()) { (featureId, caps) ->
featureId?.let { Feature(it, it.displayName, caps.toSet()) }
}
Capability → FeatureId mapping
Capabilities follow a <featureKey>.<action> naming convention:

Capability string	Maps to FeatureId
rebate.view, rebate.total, rebate.daily	FeatureId.REBATE
order.view, order.create, order.cancel	FeatureId.ORDERS
cart.view, cart.add, cart.checkout	FeatureId.CART
invoice.view, invoice.export	FeatureId.INVOICES
StabLoginData — per-BusinessUnit capabilities
During development, real API permissions are not available. StabLoginData provides hard-coded capability sets per BusinessUnit:

kotlin
core/featurePermission/api/…/StabLoginData.kt
fun capabilitiesFor(businessUnit: BusinessUnit): Set<String> = when (businessUnit) {
BusinessUnit.USBL -> setOf(
"cart.view", "cart.add", "order.view", "order.create", "order.cancel",
"rebate.view", "rebate.total", "rebate.daily",
"delivery.widget.view", "invoice.view", "settings.view", …
)
BusinessUnit.CABL -> setOf("cart.view", "invoice.view", "settings.view", "delivery.details.view")
BusinessUnit.SENM -> setOf(
"cart.view", "cart.add", "catalog.view",
"delivery.widget.view", "order.view", "order.create", "rebate.view", "settings.view",
)
}
BusinessUnit	Capabilities (abbreviated)
USBL	Full: cart, order, rebate, delivery widget, invoice, settings
CABL	Limited: cart.view, invoice.view, settings.view, delivery.details
SENM	Partial: cart, order, rebate.view, delivery.widget, catalog
BU-Aware Capability Resolution During Switching
⚠️ Critical Rule
When building a snapshot after a business unit or experience switch, always derive capabilities from StabLoginData.capabilitiesFor(newBusinessUnit) — not from userWithCustomerContext.capabilities, which is fixed at login time for the user's original BU. Passing stale capabilities causes resolvedFeatures to be identical to the previous snapshot, and FeatureGate/CapabilityGate will not update.
kotlin
feature/more/real/…/state/morePresenter.kt
is MoreEvent.SwitchBusinessUnit -> {
selectedBusinessUnit.value = event.businessUnit
val newSnapshot = experienceProvider.getExperienceSnapshot(
experience   = currentExperience,
businessUnit = event.businessUnit,
userRoles    = userWithCustomerContext.userRole,
// ✅ Fresh capabilities for the newly selected BU
capabilities = StabLoginData.capabilitiesFor(event.businessUnit),
permission   = userWithCustomerContext.permissions,
)
experienceProvider.load(newSnapshot)
}
5. DI Deep-Dive — Scopes and Wiring
   The two scopes
   AppScope  ────────────────────────────────────────────────────
   │  Lives for the entire app process lifetime                  │
   │  • RealExperienceProvider  (@SingleIn AppScope)             │
   │  • AvailableExperiences    (from BuildConfig, per flavour)  │
   │  • defaultExperience       (Experience, from BuildConfig)   │
   │  • ApolloClient, AppDatabase, etc.                          │
   ────────────────────────────────────────────────────────────
   │  extended by (when user logs in)
   ▼
   CustomerScope ────────────────────────────────────────────────
   │  Lives for the duration of one customer session             │
   │  Created via CustomerGraph.Factory                          │
   │  • UserWithCustomerContext    (current customer keys)       │
   │  • ExperienceSnapshot         (resolved feature set)        │
   │  • Set<FeatureActions>        (collected from all modules)  │
   │  • Set<EntryProviderInstaller>(nav entries from modules)    │
   │  • All ViewModels for customer-scoped screens               │
   ────────────────────────────────────────────────────────────
   AndroidAppGraph (AppScope)
   kotlin
   shared/wiring/src/androidMain/…/AndroidAppGraph.kt
   @DependencyGraph(scope = AppScope::class)
   interface AndroidAppGraph : AppGraph, ViewModelGraph {

val experienceProvider: ExperienceProvider

@Multibinds(allowEmpty = true)
val entryProviders: Set<EntryProviderInstaller>

@Multibinds(allowEmpty = true)
val availableFeatures: Set<FeatureActions>

@DependencyGraph.Factory
fun interface Factory {
fun create(
@Provides variant: Variant,
@Provides application: Application,
@Provides defaultExperience: Experience,
@Provides availableExperiences: AvailableExperiences,
): AndroidAppGraph
}
}
CustomerGraph (CustomerScope)
kotlin
feature/home/real/…/di/CustomerGraph.kt
@GraphExtension(CustomerScope::class)
interface CustomerGraph : ViewModelGraph {
val userWithCustomerContext: UserWithCustomerContext
val experienceSnapshot: ExperienceSnapshot

@GraphExtension.Factory
fun interface Factory {
fun create(
@Provides userWithCustomerContext: UserWithCustomerContext,
@Provides experienceSnapshot: ExperienceSnapshot,
): CustomerGraph
}
}
CustomerGraph is a graph extension — it inherits everything from AppScope and adds customer-specific bindings. The factory is called by homeEntry once per customer session.

MoreModule (CustomerScope)
kotlin
feature/more/real/…/di/MoreModule.kt
@BindingContainer
@ContributesTo(CustomerScope::class)
object MoreModule {

@Provides @IntoSet
fun provideMoreEntry(
features: Set<FeatureActions>,
): EntryProviderInstaller = {
moreEntry(features)
}
}
MoreViewModel (CustomerScope)
kotlin
@ViewModelKey
@ContributesIntoMap(CustomerScope::class, binding = binding<ViewModel>())
internal class MoreViewModel(
private val moreClient: MoreClient,
private val navigator: Navigator,
private val experienceProvider: ExperienceProvider,                      // AppScope
private val userWithCustomerContext: UserWithCustomerContext,             // CustomerScope
private val availableExperiences: AvailableExperiences,                  // AppScope
private val experienceBusinessUnitDefaults: ExperienceBusinessUnitDefaults, // AppScope ← new
) : MoleculeViewModel<MoreEvent, MoreState>()
⚠️ Scope injection rule
A lower-scope object (CustomerScope) can freely inject higher-scope dependencies (AppScope). The reverse is not allowed.
ExperienceBusinessUnitDefaults (AppScope)
kotlin
core/featurePermission/api/…/ExperienceBusinessUnitDefaults.kt
class ExperienceBusinessUnitDefaults(
private val defaults: Map<Experience, BusinessUnit>
) {
/** Returns the configured default BU for this experience, or null if not configured. */
fun defaultFor(experience: Experience): BusinessUnit? = defaults[experience]

companion object {
val Empty = ExperienceBusinessUnitDefaults(emptyMap())

    /** Parses "SYSCO_SHOP:USBL,NEW_PORT:SENM" from BuildConfig.BUSINESS_UNIT_DEFAULTS. */
    fun parse(raw: String): ExperienceBusinessUnitDefaults
}
}
Wired in ShopApplication by parsing BuildConfig.BUSINESS_UNIT_DEFAULTS (generated from config/stores/*.properties).

6. The Full Data Flow
   App Startup
   │
   ▼
   ShopApplication.appGraph  ←  AndroidAppGraph.Factory.create(
   defaultExperience = Experience.valueOf(BuildConfig.EXPERIENCE),
   availableExperiences = parse(BuildConfig.AVAILABLE_EXPERIENCES)
   )
   │
   ▼ user logs in
   RealUserClient.observeUserWithCustomerContext()
   │  reads from Room DB → maps to UserWithCustomerContext(experience = defaultExperience)
   ▼
   RootViewModel detects user  →  pushes HomeRoute(userWithCustomerContext)
   │
   ▼
   homeEntry { key ->
   experienceProvider.clear()
   val snapshot = experienceProvider.getExperienceSnapshot(
   experience   = key.userWithCustomerContext.experience,
   businessUnit = key.userWithCustomerContext.businessUnit,
   userRoles    = key.userWithCustomerContext.userRole,
   capabilities = key.userWithCustomerContext.capabilities,
   permission   = key.userWithCustomerContext.permissions,
   )
   experienceProvider.load(snapshot)
   customerGraphFactory.create(userWithCustomerContext, experienceSnapshot = snapshot)
   }
   │
   ▼ CustomerScope alive — MoreViewModel can be created
   MoreViewModel.present()
   val liveSnapshot by experienceProvider.snapshotFlow.collectAsState()
   currentExperience = liveSnapshot?.experience
   │
   ▼
   MoreScreen
   ExperienceSwitcher (visible only when availableExperiences.size > 1)
   PermissionGate(capability = "rebate.view") { RebateButton() }   ← featureId=REBATE, compiled-in + snapshot
   PermissionGate(capability = "order.view")  { OrderButton()  }   ← featureId=ORDERS, compiled-in + snapshot
   LogoutButton
7. Gating UI with PermissionGate
   PermissionGate is the single composable for all permission-based UI gating. It replaces the old FeatureGate and CapabilityGate with one unified API.

Signature
kotlin
core/ui/src/androidMain/…/featuregate/PermissionGate.kt
@Composable
fun PermissionGate(
capability: String? = null,       // drives both feature-id lookup and capability check
fallback: @Composable () -> Unit = {},
content: @Composable (FeatureActions?) -> Unit,
)
capability is the single entry-point. It is optional — omitting it means no gating and content is always shown. The gate performs two checks from one string: a compiled-in feature check and a runtime capability check, both derived automatically from the capability string.

How it works
kotlin
// 1. Derive FeatureId from the capability prefix  ("rebate.view" → FeatureId.REBATE)
val featureId      = capability?.let { FeatureId.fromCapability(it) }

// 2. Look up the compiled-in FeatureActions for this build flavour
val featureActions = featureId?.let { id -> features.firstOrNull { it.featureKey == id } }

// 3. Both conditions must be true
val granted =
(featureId == null || featureActions != null && snapshot?.hasFeature(featureId) == true) &&
(capability == null || snapshot?.hasCapability(capability) == true)

if (granted) content(featureActions) else fallback()
featureId is derived internally — callers never pass it directly. featureActions is resolved from LocalFeatureActions (the compiled feature set for this flavour); a null result means the feature module is absent from the current build's classpath.

ℹ️ One parameter, two gates
Passing capability = "rebate.view" automatically gates on both:
the compiled-in FeatureActions for FeatureId.REBATE (build-time check), and
snapshot.hasCapability("rebate.view") (runtime permission check).
Use a more-specific capability (e.g. "rebate.total") to gate a sub-widget while still inheriting the parent feature check.
Composition locals
Local	Provided in	Contents
LocalExperienceSnapshot	MainActivity (from snapshotFlow)	Runtime snapshot — resolved features + capabilities
LocalFeatureActions	homeEntry (from CustomerGraph.availableFeatures)	Compiled FeatureActions set for this flavour
Usage — primary feature section gate
Pass the feature's primary capability (the .view variant). The gate internally derives the FeatureId and performs both the compiled-in feature check and the runtime capability check.

kotlin
PermissionGate(capability = "rebate.view") { rebateActions ->
rebateActions?.let {
RebateButton(feature = it, onClick = { state.onEvent(MoreEvent.RebateClick(it)) })
}
}
Usage — sub-feature capability gate
Pass a more-specific capability to gate a sub-widget. The parent feature check (FeatureId.REBATE) is still enforced because the prefix is identical.

kotlin
PermissionGate(capability = "rebate.total") { _ ->
RebateTotalWidget()
}

// Non-feature-mapped capability (no matching FeatureId prefix):
// featureId resolves to null → feature check short-circuits to true
PermissionGate(capability = "delivery.widget.view") { _ ->
DeliveryWidget(deliveryInfo = deliveryInfo, onEvent = onEvent)
}
What gates what
Call	Derived FeatureId	Checks performed	Controls
PermissionGate(capability = "rebate.view")	REBATE	compiled-in FeatureActions + hasFeature(REBATE) + hasCapability("rebate.view")	Entire Rebate section
PermissionGate(capability = "order.view")	ORDERS	compiled-in FeatureActions + hasFeature(ORDERS) + hasCapability("order.view")	Entire Order section
PermissionGate(capability = "rebate.total")	REBATE	compiled-in FeatureActions + hasFeature(REBATE) + hasCapability("rebate.total")	Rebate Total sub-widget
PermissionGate(capability = "delivery.widget.view")	null (no prefix match)	hasCapability("delivery.widget.view") only	Delivery widget
PermissionGate()	null	none — always granted	Unconditional render
Why compositionLocalOf (not staticCompositionLocalOf)
kotlin
// ✅ compositionLocalOf — each PermissionGate that reads this local is a direct
// Compose subscriber. When the snapshot changes, only gate composables recompose,
// not the entire NavDisplay subtree.
val LocalExperienceSnapshot: ProvidableCompositionLocal<ExperienceSnapshot?> =
compositionLocalOf { null }
⚠️ Anti-pattern
staticCompositionLocalOf does not set up per-consumer read subscriptions. Compose's "skip unchanged" optimisation can skip re-evaluating a gate nested inside a stable NavDisplay entry even when the snapshot changed. Always use compositionLocalOf for values that update at runtime.
Recomposition chain after a BU/experience switch
MoreEvent.SwitchBusinessUnit(CABL)
│
▼
morePresenter: StabLoginData.capabilitiesFor(CABL) → new capabilities set
│
▼
experienceProvider.load(newSnapshot)           ← MutableStateFlow.value = newSnapshot
│
▼ StateFlow emits (data class != previous)
MainActivity: currentExperienceSnapshot changes (collectAsStateWithLifecycle)
│
▼ compositionLocalOf — only PermissionGate readers recompose
PermissionGate(capability = "rebate.view")           → false for CABL → 🚫 hidden
PermissionGate(capability = "delivery.widget.view")  → false for CABL → 🚫 hidden
8. Experience & Business Unit Switching (Commerce Flavour)
   The syscoCommerce flavour supports switching between SYSCO_SHOP and NEW_PORT experiences at runtime. Each experience exposes its own set of selectable Business Units. The More screen drives both switches.

8.1 Experience Switching
Config
ini
config/stores/syscoCommerce.properties
experience=SYSCO_SHOP
availableExperiences=SYSCO_SHOP,NEW_PORT
features=order,rebate
# Default business unit per experience: <EXPERIENCE>:<BUSINESS_UNIT> pairs separated by commas
businessUnitDefaults=SYSCO_SHOP:USBL,NEW_PORT:SENM
Switch flow
User taps "Newport" button in MoreScreen
│
▼
MoreEvent.SwitchExperience(Experience.NEW_PORT)
│
▼
morePresenter:
newBusinessUnit = experienceBusinessUnitDefaults.defaultFor(NEW_PORT)  // SENM
capabilities    = StabLoginData.capabilitiesFor(SENM)                  // fresh caps
│
▼
experienceProvider.getExperienceSnapshot(
experience   = NEW_PORT,
businessUnit = SENM,
capabilities = { "cart.view", "order.view", "rebate.view", … },
…
)
experienceProvider.load(newSnapshot)
│
├── snapshotFlow emits NEW_PORT snapshot
│       ├── MainActivity: currentExperience = NEW_PORT → brand colors update
│       ├── LocalExperienceSnapshot updates → PermissionGates re-evaluate
│       └── MorePresenter: currentExperience = NEW_PORT → chip highlights update
│
└── homeEntry is NOT re-triggered (remember-keyed to userWithCustomerContext)
ℹ️ Note
Single-experience flavours (syscoshop, newport) never show the experience switcher because availableExperiences.size == 1.
8.2 Business Unit Selection
Each experience exposes a specific set of selectable Business Units. Below the experience chips in ExperienceSwitcher, a BusinessUnitSelector row is rendered whenever the selected experience has more than zero available BUs.

Experience → BusinessUnit mapping
kotlin
feature/more/real/…/state/morePresenter.kt
private fun businessUnitsFor(experience: Experience): List<BusinessUnit> = when (experience) {
Experience.SYSCO_SHOP     -> listOf(BusinessUnit.CABL, BusinessUnit.USBL)
Experience.NEW_PORT       -> listOf(BusinessUnit.SENM)
Experience.SYSCO_COMMERCE -> emptyList()
}
Experience	Available Business Units	Default (from config)
SYSCO_SHOP	CABL, USBL	USBL
NEW_PORT	SENM	SENM
SYSCO_COMMERCE	none	—
Config-level BU defaults
The default BusinessUnit per experience is declared in the store .properties file:

ini
# config/stores/syscoshop.properties
businessUnitDefaults=SYSCO_SHOP:USBL

# config/stores/newport.properties
businessUnitDefaults=NEW_PORT:SENM

# config/stores/syscoCommerce.properties
businessUnitDefaults=SYSCO_SHOP:USBL,NEW_PORT:SENM
ExperienceBusinessUnitDefaults.parse(raw) reads this at app startup and provides defaultFor(experience):

kotlin
ShopApplication.kt
val appGraph = AndroidAppGraph.Factory.create(
…
experienceBusinessUnitDefaults = ExperienceBusinessUnitDefaults.parse(
BuildConfig.BUSINESS_UNIT_DEFAULTS   // e.g. "SYSCO_SHOP:USBL,NEW_PORT:SENM"
),
)
BU switch flow
User taps "CABL" chip in BusinessUnitSelector (was on USBL)
│
▼
MoreEvent.SwitchBusinessUnit(BusinessUnit.CABL)
│
▼
morePresenter:
selectedBusinessUnit.value = CABL       // chip highlight updates immediately
capabilities = StabLoginData.capabilitiesFor(CABL)
// = { "cart.view", "invoice.view", "settings.view", … }
│
▼
experienceProvider.getExperienceSnapshot(
experience   = SYSCO_SHOP,
businessUnit = CABL,
capabilities = { "cart.view", "invoice.view", "settings.view", … },
permission   = userWithCustomerContext.permissions,
)
// resolvedFeatures = capabilities ∩ permissions
// → REBATE absent for CABL → FeatureGate(REBATE) hides button
experienceProvider.load(newSnapshot)
│
▼ StateFlow emits (businessUnit field differs → new data class instance)
compositionLocalOf → only gate composables recompose
PermissionGate("rebate.view")              → false → 🚫 Rebate button hidden
PermissionGate("order.view")               → true  → ✅ Order button visible
PermissionGate("delivery.widget.view")     → false → 🚫 delivery widget hidden
MoreState fields added for BU switching
kotlin
data class MoreState(
val availableExperiences: List<Experience>,
val currentExperience: Experience,
val currentBusinessUnit: BusinessUnit?,                      // selected BU chip
val availableBusinessUnitsForExperience: List<BusinessUnit>, // chips to render
val deliveryInfo: DeliveryInfo?,
val onEvent: (MoreEvent) -> Unit,
)

sealed interface MoreEvent {
// …existing…
data class SwitchExperience(val experience: Experience)     : MoreEvent
data class SwitchBusinessUnit(val businessUnit: BusinessUnit) : MoreEvent  // ← new
}
9. Build-Time Feature Bundling
   The Commerce app uses a store-driven build system to control which feature modules are compiled into each APK at build time. Unused features are never shipped to a flavour — they are excluded at the Gradle dependency level, not hidden at runtime.

9.1 The Store Properties File
Each store (flavour) has a .properties file in config/stores/ — the single source of truth for everything that differs between flavours:

config/stores/
├── syscoshop.properties        ← Sysco Shop flavour
├── newport.properties          ← Newport flavour
└── syscoCommerce.properties    ← Commerce flavour (multi-experience)
ini
config/stores/syscoCommerce.properties
storeName=syscoCommerce
experience=SYSCO_SHOP
availableExperiences=SYSCO_SHOP,NEW_PORT
features=order,rebate
# Default business unit per experience: <EXPERIENCE>:<BUSINESS_UNIT> pairs separated by commas
businessUnitDefaults=SYSCO_SHOP:USBL,NEW_PORT:SENM
Store file	features	experience	availableExperiences	businessUnitDefaults
syscoshop.properties	order	SYSCO_SHOP	SYSCO_SHOP	SYSCO_SHOP:USBL
newport.properties	rebate	NEW_PORT	NEW_PORT	NEW_PORT:SENM
syscoCommerce.properties	order,rebate	SYSCO_SHOP	SYSCO_SHOP,NEW_PORT	SYSCO_SHOP:USBL,NEW_PORT:SENM
9.2 StoreManifest — the Gradle Utility
kotlin
buildSrc/src/main/kotlin/StoreManifest.kt
object StoreManifest {
const val SELECTED_STORE = "syscoCommerce"   // default when -Pstore is not passed

fun featuresFor(rootDir: File, store: String): List<String>
fun experienceFor(rootDir: File, store: String): String
fun availableExperiencesFor(rootDir: File, store: String): List<String>
fun applicationId(rootDir: File, store: String): String
fun stores(rootDir: File): Map<String, List<String>>
}
9.3 Selecting a Store
kotlin
val store = providers.gradleProperty("store").getOrElse(StoreManifest.SELECTED_STORE)
How to select	Example
Default (from gradle.properties)	store=syscoshop
CLI override	./gradlew assembleDebug -Pstore=newport
CI environment variable	ORG_GRADLE_PROJECT_store=syscoCommerce
9.4 shared/wiring — KMP Framework Dependencies
kotlin
shared/wiring/build.gradle.kts
val store         = providers.gradleProperty("store").getOrElse(StoreManifest.SELECTED_STORE)
val storeFeatures = StoreManifest.featuresFor(rootDir, store)

kotlin {
targets.withType<KotlinNativeTarget>().configureEach {
binaries.framework {
storeFeatures.forEach {
export(project(":feature-$it-api"))
}
}
}

sourceSets {
commonMain.dependencies {
storeFeatures.forEach {
implementation(project(":feature-$it-real"))
api(project(":feature-$it-api"))
}
}
}
}
9.5 shopApp — Android Product Flavors + BuildConfig
① Creates a product flavor for each store:

kotlin
androidCommerce/shopApp/build.gradle.kts
flavorDimensions += "store"
productFlavors {
StoreManifest.stores(storeRoot).keys.forEach { store ->
create(store) {
dimension    = "store"
applicationId = StoreManifest.applicationId(storeRoot, store)
buildConfigField("String", "EXPERIENCE",
"\"${StoreManifest.experienceFor(storeRoot, store)}\"")
buildConfigField("String", "AVAILABLE_EXPERIENCES",
"\"${StoreManifest.availableExperiencesFor(storeRoot, store).joinToString(",")}\"")
}
}
}
② Adds feature modules only to the flavour that needs them:

kotlin
dependencies {
implementation(projects.sharedWiring)

StoreManifest.stores(storeRoot).forEach { (store, features) ->
features.forEach { feature ->
add("${store}Implementation", project(":feature-$feature-api"))
add("${store}Implementation", project(":feature-$feature-real"))
}
}
}
syscoshopImplementation       ← :feature-order-api,  :feature-order-real
newportImplementation         ← :feature-rebate-api, :feature-rebate-real
syscoCommerceImplementation   ← :feature-order-api,  :feature-order-real,
:feature-rebate-api, :feature-rebate-real
9.6 How BuildConfig Reaches Runtime
kotlin
androidCommerce/shopApp/…/ShopApplication.kt
val appGraph: AndroidAppGraph by lazy {
createGraphFactory<AndroidAppGraph.Factory>().create(
variant              = Variant.Dev,
application          = this,
defaultExperience    = Experience.valueOf(AppBuildConfig.EXPERIENCE),
availableExperiences = AvailableExperiences(
AppBuildConfig.AVAILABLE_EXPERIENCES
.split(",")
.map { Experience.valueOf(it.trim()) }
),
)
}
9.7 Why DI Wires Itself Automatically
Because Metro collects @ContributesTo / @IntoSet contributions only from modules present on the classpath, the DI graph is automatically correct for each flavour:

syscoshop build classpath:
:feature-order-real   → OrderModule  (@ContributesTo CustomerScope) ✅ present
:feature-rebate-real  → RebateModule (@ContributesTo CustomerScope) ❌ NOT present

∴ CustomerScope.Set<FeatureActions>         = { orderFeature() }
∴ CustomerScope.Set<EntryProviderInstaller> = { orderEntry() }
∴ MoreScreen only shows the Order button
syscoCommerce build classpath:
:feature-order-real   → OrderModule  ✅ present
:feature-rebate-real  → RebateModule ✅ present

∴ CustomerScope.Set<FeatureActions>         = { orderFeature(), rebateFeature() }
∴ CustomerScope.Set<EntryProviderInstaller> = { orderEntry(), rebateEntry() }
∴ MoreScreen shows Order AND Rebate buttons
💡 Key insight
No if (flavor == "commerce") checks anywhere. The feature simply doesn't exist in the binary.
9.8 Complete Flavour Matrix
syscoshop	newport	syscoCommerce
applicationId	…mss.syscoshop	…mss.newport	…mss.syscocommerce
BuildConfig.EXPERIENCE	SYSCO_SHOP	NEW_PORT	SYSCO_SHOP
BuildConfig.AVAILABLE_EXPERIENCES	SYSCO_SHOP	NEW_PORT	SYSCO_SHOP,NEW_PORT
BuildConfig.BUSINESS_UNIT_DEFAULTS	SYSCO_SHOP:USBL	NEW_PORT:SENM	SYSCO_SHOP:USBL,
NEW_PORT:SENM
Default BU for SYSCO_SHOP	USBL	—	USBL
Default BU for NEW_PORT	—	SENM	SENM
:feature-order-*	✅ compiled	❌ absent	✅ compiled
:feature-rebate-*	❌ absent	✅ compiled	✅ compiled
Order button in More	✅ shown	❌ hidden	✅ shown
Rebate button in More	❌ hidden	✅ shown	✅ shown
Experience switcher	❌ hidden	❌ hidden	✅ shown
BU selector (SYSCO_SHOP)	USBL pre-selected	—	USBL pre-selected
BU selector (NEW_PORT)	—	SENM pre-selected	SENM pre-selected
9.9 How to Add a New Store
Create config/stores/<storeName>.properties:
ini
storeName=myNewStore
experience=SYSCO_SHOP
availableExperiences=SYSCO_SHOP
features=order
That's it. Gradle automatically:
Creates a new Android product flavor myNewStore
Adds :feature-order-api and :feature-order-real as myNewStoreImplementation dependencies
Emits correct BuildConfig.EXPERIENCE and BuildConfig.AVAILABLE_EXPERIENCES constants
Wires Metro DI so only Order's @ContributesTo modules are included
10. How to Add a New Feature — Step-by-Step
    Step 0 — Declare the feature in the store .properties file
    ini
# config/stores/syscoCommerce.properties
features=order,rebate,my_new_feature   # ← append here

# config/stores/syscoshop.properties
features=order,my_new_feature          # ← only if syscoshop should include it

# config/stores/newport.properties
features=rebate                        # ← leave as-is if newport should NOT have it
⚠️ Important
The feature name must match the Gradle module name: feature-<name>-api and feature-<name>-real. This is the only place you declare which flavours own which features.
Step 1 — Add a FeatureKEY
kotlin
core/feature/api/…/FeaturesActions.kt
enum class FeatureKEY {
REBATE,
ORDER,
MY_NEW_FEATURE,   // ← add here
}
Step 2 — Add a FeatureId (if needs permission-gating)
kotlin
core/featurePermission/api/…/Feature.kt
enum class FeatureId(val key: String, val displayName: String) {
// …
MY_NEW_FEATURE("my_new_feature", "My New Feature"),  // ← key must match capability prefix
}
Step 3 — Add capabilities to StabLoginData (for dev/test)
kotlin
core/featurePermission/api/…/StabLoginData.kt
fun capabilitiesFor(businessUnit: BusinessUnit) = when (businessUnit) {
BusinessUnit.USBL -> setOf(
// …existing…
"my_new_feature.view", "my_new_feature.edit",
)
}
Step 4 — Create the DI module for your feature
kotlin
feature/myNewFeature/real/…/di/MyNewFeatureModule.kt
@BindingContainer
@ContributesTo(CustomerScope::class)
object MyNewFeatureModule {

@Provides @IntoSet
fun provideEntry(): EntryProviderInstaller = {
myNewFeatureEntry()
}

@Provides @IntoSet
fun featureActions(): FeatureActions = FeatureActions(
target            = MyNewFeatureRoute,
order             = 2,
availableSections = setOf(AvailableSections.MORE),
featureKey        = FeatureKEY.MY_NEW_FEATURE,
)
}
Step 5 — Gate the UI
kotlin
// Use the primary view capability — FeatureId is derived internally
PermissionGate(capability = "my_new_feature.view") { myFeatureActions ->
myFeatureActions?.let { MyNewFeatureButton(…) }
}

// Gate a sub-section with a more-specific capability
PermissionGate(capability = "my_new_feature.edit") { _ ->
MyNewFeatureEditWidget(…)
}
Step 6 — Wire the feature into the wiring module
kotlin
shared/wiring/build.gradle.kts
storeFeatures.forEach {
implementation(project(":feature-$it-real"))
api(project(":feature-$it-api"))
}
// OR for always-present features:
api(projects.featureMyNewFeatureReal)
11. Quick-Reference Diagrams
    Build-time feature bundling pipeline
    config/stores/<store>.properties
    features=order,rebate
    │
    │  parsed at Gradle evaluation time
    ▼
    buildSrc/StoreManifest.kt
    featuresFor(...)          → ["order", "rebate"]
    experienceFor(...)        → "SYSCO_SHOP"
    availableExperiencesFor(…)→ ["SYSCO_SHOP", "NEW_PORT"]
    │
    ├────────────────────────────────────────────────┐
    ▼                                                ▼
    shared/wiring/build.gradle.kts           shopApp/build.gradle.kts
    storeFeatures.forEach {                  productFlavors { create(store) {
    api(":feature-$it-api")                  buildConfigField("EXPERIENCE", …)
    implementation(":feature-$it-real")      buildConfigField("AVAILABLE_EXPERIENCES", …)
    export(":feature-$it-api") // iOS      }
    }                                        }
    add("${store}Implementation", ":feature-order-*")
    │                                                │
    └────────────────────────────────────────────────┘
    │
    only declared modules on compile classpath
    │
    ▼
    Metro scans classpath for @ContributesTo
    → only present modules contribute to DI
    │
    ┌────────────┴────────────┐
    ▼                         ▼
    Set<FeatureActions>     Set<EntryProviderInstaller>
    (auto-populated)        (auto-populated)
    │                         │
    ▼                         ▼
    MoreScreen                NavDisplay
    (filtered buttons)         (registered screens)
    Module dependency overview
    core/feature/api          core/featurePermission/api         core/ui
    FeatureActions            FeatureId, Feature             PermissionGate
    EntryProviderInstaller    ExperienceSnapshot               (reads LocalFeatureActions
    AvailableSections         ExperienceProvider                and LocalExperienceSnapshot)
    FeatureKEY
    │                         │                              │
    └──────────┬──────────────┘──────────────────────────────┘
    ▼
    feature/<name>/real
    <Name>Module
    @IntoSet FeatureActions
    @IntoSet EntryProviderInstaller
    <Name>ViewModel (@ContributesIntoMap CustomerScope)
    │
    ▼
    feature/home/real
    CustomerGraph  (CustomerScope)
    homeEntry      (creates CustomerScope, loads ExperienceSnapshot)
    │
    ▼
    shared/wiring
    AndroidAppGraph (AppScope)
    Scope lifetime
    Process starts ──────────────────────────────────────────────► Process dies
    │                                                                           │
    │  AppScope ─────────────────────────────────────────────────────────────  │
    │  │ RealExperienceProvider, AvailableExperiences, defaultExperience  …    │
    │  │                                                                        │
    │  │  User logs in ──────────────────────────► Logout / customer switch    │
    │  │  │                                                  │                  │
    │  │  │  CustomerScope ─────────────────────────────────│                  │
    │  │  │  │ UserWithCustomerContext                       │                  │
    │  │  │  │ ExperienceSnapshot                            │                  │
    │  │  │  │ Set<FeatureActions>                           │                  │
    │  │  │  │ All ViewModels (MoreVM, OrderVM, …)           │                  │
    │  │  │  └──────────────────────────────────────────────┘                  │