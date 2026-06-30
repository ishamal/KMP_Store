# Project

At session start: silently read `.claude/docs/STACK.md` and `.claude/docs/ARCHITECTURE.md`. Parse and internalize — never ask the user to explain the stack. Check `_lean_dev_sessions/` and load the most recent file silently as prior session context.

stack:.claude/docs/STACK.md
arch:.claude/docs/ARCHITECTURE.md
sessions:_lean_dev_sessions/

## Conventions

- Module split: `core/<name>/{api,real}` and `features/<name>/{api,real}`. `api` = interfaces + pure types (no Compose plugin); `real` = implementations + Compose UI + Metro bindings.
- DI: Metro. Bind impls with `@ContributesBinding(AppScope::class)` + `@Inject`; app-wide singletons use `@SingleIn(AppScope::class)`.
- Compose UI lives in each module's `androidMain`. Reference modules via type-safe accessors (`projects.core.ui.api`).
- Stores are product flavors (storeA/B/C) defined ONLY by `config/stores/<store>.properties` — never hard-code store lists in build files. Adding a store = add its `.properties` + flavor source set.
- Experience-based theming is runtime (keyed by `Experience`), not per-flavor. Brand colors: add raw tokens + a `BrandColorScheme` in `androidApp/.../branding/<Brand>Colors.kt`; read via `AppTheme.colors.*` (custom roles) or `MaterialTheme.colorScheme` (Material slots).
- Don't reintroduce a strings/wordings branding system — it was deliberately removed; only `BrandColorScheme` theming remains. `app_name` in flavor `strings.xml` is just the launcher label.

## Key Files

- `androidApp/src/main/kotlin/.../App.kt` — Compose root, session routing, theme + experience wiring.
- `androidApp/src/main/kotlin/.../branding/` — `BrandPalette.kt` (`brandColorsFor`/`colorSchemeFor`) + per-brand `*Colors.kt`.
- `core/ui/.../BrandColorScheme.kt`, `AppTheme.kt`, `ExperienceController.kt` — theming types/accessors.
- `core/experience/.../ExperienceSnapshot.kt` — access model (features/capabilities); `RealExperienceResolver`/`RealExperienceReader`.
- `buildSrc/StoreManifest.kt` + `config/stores/*.properties` — flavor & feature source of truth.
- `features/login/real/.../StubLoginData.kt` — stubbed backend/auth.
