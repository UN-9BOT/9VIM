---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# Codebase Structure

**Analysis Date:** 2026-09-17

## Directory Layout

```text
9VIM/
├── 8vim/                         # Android application module
│   └── src/{main,test,androidTest}/
├── config/checkstyle/             # Java style configuration
├── gradle/                        # Version catalog and Gradle support
├── metadata/                      # F-Droid/store metadata
├── random_images/                 # Store and design assets
└── build.gradle.kts               # Root build configuration
```

## Directory Purposes

**`8vim/src/main/kotlin/inc/flide/vim8/`:** Kotlin production code grouped by app, datastore, IME, theme and reusable `lib` helpers.

**`8vim/src/main/res/`:** Android resources: Compose-supporting values, drawables, fonts, raw YAML layouts and input-method XML.

**`8vim/src/main/assets/`:** bundled fonts, dictionary seed CSV and logging assets.

**`8vim/src/test/kotlin/`:** co-located package-mirror unit tests; fixtures are in `8vim/src/test/resources/`.

**`8vim/src/main/resources/schemas/`:** JSON schemas for common and versioned layout formats.

## Key File Locations

**Entry Points:** `8vim/src/main/AndroidManifest.xml`, `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`, `8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt`, `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt`.

**Configuration:** `build.gradle.kts`, `8vim/build.gradle.kts`, `gradle.properties`, `8vim/version.properties`, `config/checkstyle/checkstyle.xml`.

**Core Logic:** `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/`, `.../ime/input/`, `.../ime/editor/`, `.../ime/layout/`.

**Testing:** `8vim/src/test/kotlin/inc/flide/vim8/` mirrors production packages; Android tests are under `8vim/src/androidTest/`.

## Naming Conventions

**Files:** Kotlin classes use PascalCase (`KeyboardManager.kt`); tests append `Spec` (`KeyboardManagerSpec.kt`); Android resources use lowercase snake_case.

**Directories:** Lowercase package hierarchy, with feature subpackages (`ime/keyboard/text`, `app/settings/about`).

## Where to Add New Code

**New Feature:** Put domain code under the relevant `8vim/src/main/kotlin/inc/flide/vim8/ime/<feature>/` or `app/<area>/`; expose shared lifecycle services through `VIM8Application.kt`.

**New Component/Module:** Compose UI belongs under `app/settings`, `app/setup`, or `lib/compose`; keyboard rendering belongs under `ime/keyboard/view`.

**Utilities:** Add narrowly scoped helpers to `8vim/src/main/kotlin/inc/flide/vim8/lib/<category>/`.

**Tests:** Mirror the production package under `8vim/src/test/kotlin/inc/flide/vim8/` and place YAML fixtures under `8vim/src/test/resources/`.

## Special Directories

**`8vim/src/main/res/raw/`:** Built-in keyboard layouts and actions; committed source data, not generated.

**`8vim/build/` and root `build/`:** Gradle-generated outputs; do not add source code.

**`metadata/`:** Committed distribution metadata and screenshots/changelogs.

---

*Structure analysis: 2026-09-17*
