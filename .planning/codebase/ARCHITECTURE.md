---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
<!-- refreshed: 2026-09-17 -->

# Architecture

**Analysis Date:** 2026-09-17

## System Overview

```text
Android framework
   ├─ VIM8Application (`8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`)
   ├─ MainActivity → Compose settings/setup (`.../app/MainActivity.kt`)
   └─ Vim8ImeService → Compose IME (`.../Vim8ImeService.kt`)
          │
          ├─ managers: keyboard, editor, clipboard, NLP, theme
          ├─ YAML layout parser/loader + CBOR cache
          └─ Preference datastore / Android InputConnection
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Application container | Initializes preferences, theme and lazy shared managers | `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt` |
| Settings UI | Navigation, setup, preferences and preview keyboard | `8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt`, `.../app/Routes.kt` |
| IME service | Android input-method lifecycle and keyboard composition | `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt` |
| Input domain | Editor, dispatch, keyboard state and layouts | `8vim/src/main/kotlin/inc/flide/vim8/ime/` |
| Persistence | Cached preference models and serialization | `8vim/src/main/kotlin/inc/flide/vim8/datastore/` |

## Pattern Overview

**Overall:** Android service/application shell with feature-oriented Kotlin modules and Jetpack Compose UI.

**Key Characteristics:**

- `VIM8Application` owns lazy process-wide services; Context extension accessors expose them.
- UI observes preference/state flows and renders Compose screens.
- Layouts are parsed from versioned YAML resources, merged with common layers, and cached as CBOR.
- Domain classes are mostly framework-light and are unit tested independently.

## Layers

**Platform entry points:** `8vim/src/main/AndroidManifest.xml` declares `VIM8Application`, `MainActivity`, and `Vim8ImeService`.

**Application services:** `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt` constructs managers and starts dictionary seeding.

**IME domain:** `8vim/src/main/kotlin/inc/flide/vim8/ime/` contains input lifecycle, editor, keyboard variants, gestures, layout, NLP, clipboard, theme and UI. It depends on Android APIs, preferences and shared libraries.

**Settings UI:** `8vim/src/main/kotlin/inc/flide/vim8/app/settings/` and `.../app/setup/` contain Compose screens; navigation is centralized in `.../app/Routes.kt`.

**Shared infrastructure:** `8vim/src/main/kotlin/inc/flide/vim8/lib/` provides Compose, Android, geometry, backup and Kotlin helpers.

## Data Flow

### Primary IME Request Path

1. Android invokes `Vim8ImeService.onCreateInputView` (`8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt`).
2. Service loads selected YAML through `LayoutLoader`/`YamlLayoutLoader` (`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/LayoutLoader.kt`).
3. `KeyboardManager` and `EditorInstance` process gestures/key actions against `InputConnection`.
4. `ImeUi` selects text, numeric, selection, symbols or clipboard Compose layouts.

### Settings Flow

1. `MainActivity` waits for `AppPrefs` readiness.
2. `Routes.AppNavHost` selects setup or settings destination.
3. Screens mutate observed `PreferenceModel` values; shared managers consume them.

**State Management:** Preferences are cached in `Datastore`; UI uses `observeAsState`; keyboard state is exposed by `KeyboardManager.activeState`.

## Key Abstractions

- `LayoutLoader`/`LayoutParser` isolate layout sources and parsing (`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/`).
- `PreferenceModel`, `PreferenceData`, and `PreferenceSerDe` define typed persistent settings (`8vim/src/main/kotlin/inc/flide/vim8/datastore/model/`).
- `LifecycleInputMethodService` supplies lifecycle-aware IME base behavior (`8vim/src/main/kotlin/inc/flide/vim8/ime/lifecycle/`).
- `ScreenScope` and Compose helpers standardize settings screen composition (`8vim/src/main/kotlin/inc/flide/vim8/lib/compose/`).

## Entry Points

- `MainActivity.onCreate`: launches setup/settings Compose UI.
- `Vim8ImeService.onCreateInputView`: creates the keyboard view; input callbacks handle editor state.
- `VIM8Application.onCreate`: initializes process services and background word-frequency seeding.

## Architectural Constraints

- **Threading:** Android main thread for UI/IME; `VIM8Application.appScope` uses IO + `SupervisorJob` for seeding.
- **Global state:** lazy service singletons in `VIM8Application`; weak references in `VIM8Application.kt`, `MainActivity.kt`, and `Vim8ImeService.kt`.
- **Persistence:** preference models must be obtained through `Datastore`/`appPreferenceModel` to preserve cache identity.
- **Resources:** user layouts and built-in layouts use raw YAML; schema files live in `8vim/src/main/resources/schemas/`.

## Anti-Patterns

### Bypassing application services

**What happens:** A feature constructs its own manager instead of using Context accessors.
**Why it's wrong:** State and preference observers diverge from the process singleton.
**Do this instead:** Use accessors such as `context.keyboardManager()` from `VIM8Application.kt`.

### Mixing navigation into screens

**What happens:** Screens add destinations independently.
**Why it's wrong:** Route ownership and setup start-destination logic become inconsistent.
**Do this instead:** Register destinations in `8vim/src/main/kotlin/inc/flide/vim8/app/Routes.kt`.

## Error Handling

**Strategy:** Fallible layout operations return Arrow `Either<LayoutError, ...>`; resource exceptions are wrapped as `ExceptionWrapperError`.

**Patterns:** Validate/merge layouts in `YamlLayoutLoader`; use safe loaders for IME startup; preferences expose nullable/default reads.

## Cross-Cutting Concerns

**Logging:** Logback configuration in `8vim/src/main/resources/logback.xml` and assets.
**Validation:** YAML schemas and parser/model validation under `ime/layout`.
**Authentication:** Not applicable; this is a local keyboard application.

---

*Architecture analysis: 2026-09-17*
