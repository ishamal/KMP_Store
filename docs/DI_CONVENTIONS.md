# Dependency Injection conventions (Metro, api/real)

> Navigation update: features now self-register a Navigation 3 NavDestination (NavKey + TabMeta + an entry{} installer); the old AppTab and Slot abstractions were removed. See ADDING_STORES_AND_FEATURES.md for the current pattern.

How DI and modularization work in this repo. See also [`METRO_DI.md`](METRO_DI.md) (Metro basics)
and [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Module split: `api` vs `real`

Every feature is two Gradle modules:

| Module | Contains | Metro? | iOS |
|---|---|---|---|
| `features/<f>/api` | public **interfaces + models** (e.g. `CartRepository`, `CartItem`) | no plugin (may use `libs.metro.runtime` for annotations) | **exported** to the `Shared` framework |
| `features/<f>/real` | **implementations** (`RealCartRepository : CartRepository`), Compose UI (`CartScreen`), DI contributions (`CartContribution`) | applies Metro | linked into the framework, **not exported** |

`:core` is an API-style foundation (no Metro plugin): `AppScope`, `Experience`/`Capability`,
`Session`, `SessionManager` (interface), `AppTab`, `Slot`/`SlotHost`, `formatPrice`.

## DI rules

- **Code we own** → annotate the impl directly:
  `@Inject @ContributesBinding(AppScope::class) class RealX(...) : X`. No module/`@BindingContainer` needed.
- **Code we don't own / values that need a recipe** (e.g. an `AppTab` wrapping a Compose lambda) →
  `@ContributesTo(AppScope::class) @BindingContainer object XContribution { @Provides @IntoSet fun … }`.
- **`@SingleIn(AppScope::class)`** only for **stateful** singletons:
  `RealSessionManager`, `RealCartRepository`, `RealSettingsRepository`. Stateless impls
  (`RealLoginValidator`, `RealAuthenticator`, `RealInvoiceRepository`, `RealOrderRepository`,
  `RealRebateRepository`) get none.
- The one app graph (`androidApp/.../di/AppGraph.kt`, and the iOS graph in `shared/iosMain`)
  aggregates all `@ContributesBinding`/`@ContributesTo` from the linked `:real` modules.

## Build wiring (driven by `buildSrc/StoreManifest.kt`)
- `androidApp` links `:features:<f>:real` per product flavor (`:api` is transitive).
- `shared` (iOS) `export`s `:features:<f>:api` and `implementation`-links `:features:<f>:real`.

## IDE
`.idea/misc.xml` has an `EntryPointsManager` block so Metro-annotated declarations aren't flagged
"unused".

## Two deviations from the shared conventions doc (and why)

1. **Impls are `public`, not `internal`.** Metro **cannot aggregate `internal` `@ContributesBinding`
   across module boundaries** — the generated glue references the class by name and can't see an
   `internal` type from another module (you get `MissingBinding` for the interface). Multi-module
   Metro therefore requires public contributed impls. (Consequence: a few `Real*` symbols appear in
   the exported iOS framework. They're unused from Swift; eliminating them would require collapsing
   the per-feature graph into each module, which conflicts with having one app graph.)
2. **No `kmpproj.metro` Gradle convention plugin (yet).** A precompiled `buildSrc` plugin that
   applies Metro forces the Kotlin/Metro Gradle plugins onto every module's classpath, which clashes
   with the versioned `alias(libs.plugins.…)` applications. Doing it "right" needs a separate
   `build-logic` composite where modules apply *only* convention-plugin ids (the NowInAndroid
   pattern). For now each `:real` module applies `alias(libs.plugins.metro)` directly. The
   `build-logic` convention plugin is a clean follow-up.

## Adding a feature (api/real)
1. `features/<f>/api`: interface(s) + models. (no Metro)
2. `features/<f>/real`: `RealX` (`@Inject @ContributesBinding(AppScope::class)`), screen, and a
   `@ContributesTo @BindingContainer` contribution (`@Provides @IntoSet AppTab`/`Slot`). Applies
   `alias(libs.plugins.metro)` + Compose; `api(project(":features:<f>:api"))` + `implementation(:core)`.
3. `settings.gradle.kts`: include `:features:<f>:api` and `:features:<f>:real`.
4. `buildSrc/StoreManifest.kt`: add `<f>` to the stores that ship it.
