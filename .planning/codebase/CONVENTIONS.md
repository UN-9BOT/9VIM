---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# Coding Conventions

**Analysis Date:** 2026-09-17

## Naming Patterns

**Files:**

- Kotlin files use PascalCase for types/screens and descriptive `Spec` suffixes for tests, e.g. `BackupManager.kt` and `BackupManagerSpec.kt`.
- Packages are lowercase and mirror feature boundaries under `inc.flide.vim8`.

**Functions:**

- Functions and properties use lowerCamelCase; composables generally retain descriptive screen/component names, e.g. `ThemeScreen` in `app/settings/ThemeScreen.kt`.
- Boolean properties use readable predicates such as `enabled` and `is...`; conversion helpers are concise (`toInt`, `fromInt`) as in `ime/input/InputShiftState.kt`.

**Variables:**

- Local variables use lowerCamelCase. Immutable values use `val`; mutable state is explicit with `var` or mutable collections.
- Constants use upper snake case, e.g. `WORD_BOUNDARY_CHARS_TEST` in `ime/nlp/SuggestionsManagerSpec.kt`.

**Types:**

- Classes, enums, objects, and type parameters use PascalCase. Enum members use uppercase snake case (`UNSHIFTED`, `CAPS_LOCK`).

## Code Style

**Formatting:**

- Kotlin formatting is enforced by the `org.jlleitschuh.gradle.ktlint` plugin in `8vim/build.gradle.kts`, with Android mode and console, HTML, and Checkstyle reporters.
- Use four-space indentation, trailing commas in multiline calls/collections, and expression bodies where they improve clarity; match surrounding Kotlin style.
- Java/XML files are checked by `config/checkstyle/checkstyle.xml`; Java line length is 120 and tabs, wildcard imports, missing braces, and naming are checked.

**Linting:**

- `android { lint { ... } }` in `8vim/build.gradle.kts` aborts on errors and treats warnings as errors, with a documented set of Android/AGP compatibility suppressions.
- Keep suppressions narrow and explain compatibility/workaround reasons, as in `Vim8ImeService.kt` and the Gradle configuration.

## Import Organization

**Order:**

1. Android/Kotlin and third-party imports.
2. Project imports grouped after external imports.
3. Java standard-library imports are commonly kept with external imports according to ktlint output.

**Path Aliases:**

- No custom path aliases detected; use fully qualified package imports rooted at `inc.flide.vim8`.

## Error Handling

**Patterns:**

- Model recoverable failures explicitly with Arrow types, especially `Either<Throwable, T>` and `Option`, as in `lib/backup/BackupManager.kt`.
- Wrap boundary operations with `Either.catch`; use `getOrElse`, `getOrNone`, and `onSome` rather than unchecked nullable chains when working in Arrow-based code.
- Throw an actionable `error(...)` for malformed input that cannot continue, and preserve the message explaining the invalid archive in `lib/backup/BackupManager.kt`.
- Use nullable values only where absence is part of the Android/API contract; prefer sealed/enumerated domain states for finite states.

## Logging

**Framework:** `slf4j-api` with `logback-android` (configured as dependencies in `8vim/build.gradle.kts`).

**Patterns:**

- Keep logging at Android/service and integration boundaries; avoid noisy logging in pure geometry/input transformations.
- Tests use `logback-classic` and exclude `logback-android` for JVM compatibility.

## Comments

**When to Comment:**

- Comment non-obvious platform constraints, compatibility workarounds, and test limitations. `ime/nlp/SuggestionsManagerSpec.kt` documents why Android spell-check classes are not instantiated in JVM tests.
- Prefer names and small functions over comments that restate implementation.

**JSDoc/TSDoc:**

- Not applicable; Kotlin KDoc is used selectively for public or test-subject rationale.

## Function Design

**Size:** Keep functions focused on one transformation or side effect; UI screens compose smaller functions and reusable elements from `lib/compose/`.

**Parameters:** Prefer typed domain models and lambdas for callbacks; use named arguments and multiline calls for many parameters.

**Return Values:** Return immutable values where possible. Use Arrow `Either`/`Option` for fallible/optional library operations and direct values for pure calculations.

## Module Design

**Exports:** Keep implementation private when it is test-only or internal (`SuggestionsManagerTestSubject`); expose feature APIs from their owning package.

**Barrel Files:** No barrel/index module pattern detected. Import concrete files/packages directly.

---

*Convention analysis: 2026-09-17*
