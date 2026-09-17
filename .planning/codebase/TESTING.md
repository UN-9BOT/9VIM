---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# Testing Patterns

**Analysis Date:** 2026-09-17

## Test Framework

**Runner:**

- Kotest 5.9.1 with `FunSpec`, data tests, property tests, and JUnit 5 integration; dependencies are declared in `8vim/build.gradle.kts` and `gradle/libs.versions.toml`.
- Android tests use `br.com.colman:kotest-runner-android` and JUnit4 Compose integration.
- Config: Gradle test configuration in `8vim/build.gradle.kts`.

**Assertion Library:**

- `io.kotest.matchers` (`shouldBe`, and related matchers) plus Kotest assertions.

**Run Commands:**

```bash
./gradlew :8vim:testDebugUnitTest       # JVM unit tests
./gradlew :8vim:connectedDebugAndroidTest # instrumentation/Compose tests
./gradlew :8vim:testDebugUnitTest      # also finalizes Jacoco reporting
```

## Test File Organization

**Location:**

- JVM tests are co-located by package mirror under `8vim/src/test/kotlin/inc/flide/vim8/`.
- Instrumentation tests live under `8vim/src/androidTest/kotlin/inc/flide/vim8/`.
- YAML/XML fixtures live under `8vim/src/test/resources/schemas/`.

**Naming:**

- Unit test classes use the production subject plus `Spec`, e.g. `KeyboardSpec.kt`; the Android exception is `SettingsScreenTest.kt`.

**Structure:**

```text
8vim/src/test/kotlin/inc/flide/vim8/<feature>/<Subject>Spec.kt
8vim/src/test/resources/schemas/<version-or-common>/<fixture>.yaml
```

## Test Structure

**Suite Organization:**

```kotlin
class InputShiftStateSpec : FunSpec({
    context("fromInt") {
        withData(nameFn = { "${it.first} -> ${it.second}" }, ... ) { (input, expected) ->
            InputShiftState.fromInt(input) shouldBe expected
        }
    }
})
```

**Patterns:**

- Group behavior with `context(...)`; use descriptive `test(...)` blocks for individual scenarios.
- Use `withData` for input/output matrices and `checkAll` for generated properties (`lib/ZipUtilsSpec.kt`).
- Use `beforeTest`/`afterTest` for temporary files and mock lifecycle; clean resources in teardown.

## Mocking

**Framework:** MockK (`mockk`, `mockk-android`, `mockk-agent`).

**Patterns:**

```kotlin
context = mockk(relaxed = true) {
    every { systemService(ClipboardManager::class) } answers { androidClipboardManager }
}
mockkStatic(::appPreferenceModel)
every { appPreferenceModel() } returns cachedModel
verify { observer.onChanged(true) }
```

**What to Mock:**

- Mock Android `Context`, services, lifecycle objects, SharedPreferences, and module-level Android bridge functions, as shown in `ime/clipboard/ClipboardManagerSpec.kt` and `datastore/nodel/PreferenceModelSpec.kt`.
- Clear constructor/static mocks in teardown when used.

**What NOT to Mock:**

- Keep pure parsers, state machines, geometry, and transformations real; use property/data tests instead (`ime/input/` and `ime/layout/`).
- Do not instantiate unavailable Android framework classes in JVM tests; isolate pure logic as done in `ime/nlp/SuggestionsManagerSpec.kt`.

## Fixtures and Factories

**Test Data:**

- Reusable generated data is centralized in `arbitraries/Arbitraries.kt` using `Arb`, `arbitrary`, `map`, `list`, and `orNull`.
- Structured parser fixtures are committed YAML/XML files under `8vim/src/test/resources/schemas/` and loaded by layout/parser specs.

**Location:**

- Shared generators: `8vim/src/test/kotlin/inc/flide/vim8/arbitraries/Arbitraries.kt`.
- Feature fixtures: `8vim/src/test/resources/schemas/`.

## Coverage

**Requirements:** No numeric threshold detected. Debug unit-test coverage is enabled and Jacoco reports are configured in `8vim/build.gradle.kts`.

**View Coverage:**

```bash
./gradlew :8vim:testDebugUnitTest :8vim:jacocoTestReport
```

## Test Types

**Unit Tests:**

- Broad Kotest coverage targets input state, keyboard models/controllers, layout parsing, NLP, datastore, backup, and utility behavior under `8vim/src/test/kotlin/`.

**Integration Tests:**

- Android instrumentation currently covers Compose/settings interaction in `app/SettingsScreenTest.kt`.

**E2E Tests:**

- No separate E2E framework detected.

## Common Patterns

**Async Testing:**

```kotlin
beforeTest { /* configure observer or mock */ }
afterTest { /* clear mocks/resources */ }
```

- Prefer deterministic callback capture with MockK slots/answers in lifecycle and preference tests.

**Error Testing:**

- Assert Arrow `Either`/`Option` outcomes and invalid fixture behavior; use Kotest exception matchers where a thrown failure is the contract.

---

*Testing analysis: 2026-09-17*
