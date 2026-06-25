This is a Kotlin Multiplatform project targeting Android, iOS.

## 📚 Documentation

New here? Start with the architecture overview, then the topic guides:

- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) — **start here**: how it's built, for beginners
  (incl. how to set up this store/feature system in a new project)
- [docs/STORES_AND_FEATURES.md](./docs/STORES_AND_FEATURES.md) — the store/feature design
- [docs/ADDING_STORES_AND_FEATURES.md](./docs/ADDING_STORES_AND_FEATURES.md) — how-to with steps
- [docs/METRO_DI.md](./docs/METRO_DI.md) — how dependency injection (Metro) works, for beginners
- [docs/DI_CONVENTIONS.md](./docs/DI_CONVENTIONS.md) — api/real module split + Metro conventions
- [docs/EXPERIENCES.md](./docs/EXPERIENCES.md) — per-user (USBL/CABL) runtime behaviour
- [docs/ACCESS_CONTROL.md](./docs/ACCESS_CONTROL.md) — business-unit/role access control (capabilities vs permissions), beginner-friendly
- [docs/ACCESS_CONTROL_USAGE.md](./docs/ACCESS_CONTROL_USAGE.md) — access control usage reference (copy-paste recipes)
- [docs/TUTORIAL_ADD_ORDERS_FEATURE.md](./docs/TUTORIAL_ADD_ORDERS_FEATURE.md) — full worked example


* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…