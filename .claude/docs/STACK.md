runtime: Kotlin Multiplatform (Android + iOS). Kotlin 2.4.0, JVM target 11. Android compileSdk/targetSdk 36, minSdk 26. AGP 9.0.1.
framework: Compose Multiplatform 1.11.1 (UI). Material3 1.11.0-alpha07. Compose UI lives in androidMain of each module; the Kotlin Compose compiler plugin is applied widely, but only :real feature modules and androidApp apply the org.jetbrains.compose plugin (composeMultiplatform).
deps:
  - DI: Metro 1.2.1 (dev.zacsweers.metro) — @Inject, @ContributesBinding(AppScope::class), @SingleIn(AppScope::class); app graph via createAppGraph(). Metro-x ViewModel integration (metroViewModel(), LocalMetroViewModelFactory).
  - Navigation: AndroidX Navigation3 1.1.3 (NavKey, NavDisplay, entryProvider). Custom Navigator (core/navigation) wraps a back stack; LocalNavigator exposes goTo/back.
  - Presentation: Molecule 2.2.0 (molecule-runtime) available for state.
  - androidx lifecycle 2.11.0-beta01 (viewmodel-compose, runtime-compose), activity-compose 1.13.0.
dev: Gradle + version catalog (gradle/libs.versions.toml). Type-safe project accessors enabled (e.g. projects.core.ui.api). Store→feature wiring lives in the build-logic included build: the STORES table in build-logic/.../Stores.kt is the source of truth; the store-catalog/store-features convention plugins drive the Android product flavors and the iOS -Pstore framework build. Configuration cache is ON — editing Stores.kt recompiles build-logic and invalidates the cache automatically (no --no-configuration-cache needed).
test: kotlin-test (commonTest), junit 4.13.2, androidx espresso/testExt for instrumented.
db: none yet (no persistence layer). Login/backend are stubbed — see features/login/real StubLoginData.kt.
