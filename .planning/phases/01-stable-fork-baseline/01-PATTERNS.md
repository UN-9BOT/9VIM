# Phase 1: Stable Fork Baseline - Pattern Map

**Mapped:** 2026-09-17
**Files classified:** 19 implementation/test/documentation targets
**Analogs found:** 19 / 19
**Tracked-source gate:** every analog below was verified with `git ls-files`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Tracked Analog | Match Quality |
|---|---|---|---|---|
| `scripts/baseline-check.sh` | build utility | batch | `.github/workflows/codecheck.yaml`, `.github/workflows/build.yaml` | flow-match |
| `config/baseline-toolchain.properties` | config | file-I/O | `gradle/wrapper/gradle-wrapper.properties`, `8vim/version.properties` | exact-format |
| `.github/workflows/pr-test.yml` | CI config | event-driven | `.github/workflows/codecheck.yaml`, `.github/workflows/build.yaml` | exact |
| `docs/compatibility/enter-newline.md` | compatibility note | file-I/O | `docs/prd/8vim_fork_implementation_brief_ru.md` | role-match |
| `docs/compatibility/baseline.md` | provenance/smoke note | file-I/O | `docs/prd/8vim_fork_implementation_brief_ru.md`, `.github/workflows/bump-version.yaml` | role-match |
| `8vim/build.gradle.kts` | build config | transform | same file's plugin/task configuration | exact |
| `8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt` | Compose component/platform adapter | request-response | existing `fileSelector()` | exact |
| `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt` | domain service/store | transactional CRUD | existing history/reload/select methods | exact |
| `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt` | model/loader utility | file-I/O + transform | existing `loadKeyboardData`/`LayoutSerDe` | exact |
| `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt` | persistent config model | CRUD | existing `Layout.current`/`Custom.history` | exact |
| `8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt` | unit test | transactional CRUD | existing layout history/selection tests | exact |
| `8vim/src/test/kotlin/inc/flide/vim8/ime/layout/LayoutSpec.kt` | unit test | file-I/O + transform | existing resolver/cache tests | exact |
| `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt` | input controller | event-driven | existing `handleEnter()` | exact; likely verify-only |
| `8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt` | unit test | event-driven | existing public `onInputKeyUp` matrix | exact |
| `8vim/src/test/kotlin/inc/flide/vim8/ime/editor/EditorInstanceSpec.kt` | unit test | request-response | existing Enter delegation tests | exact; verify-only |
| `8vim/src/main/kotlin/inc/flide/vim8/app/settings/GestureScreen.kt` | Compose component | event-driven | existing `SwitchPreference` entries | exact; optional #622 |
| `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/xpad/KeyboardController.kt` | gesture controller | event-driven | existing movement state machine | exact; optional #622 |
| `8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/xpad/KeyboardControllerSpec.kt` | unit test | event-driven | existing touch-sequence tests | exact; optional #622 |
| `8vim/src/main/res/values/strings.xml` | UI resources | lookup | existing settings strings | exact; optional #622 |

## Pattern Assignments

### Baseline script, manifest, and all-PR workflow

**Targets:** `scripts/baseline-check.sh`, `config/baseline-toolchain.properties`,
`.github/workflows/pr-test.yml`

**Analogs:** `.github/workflows/codecheck.yaml`, `.github/workflows/build.yaml`,
`gradle/wrapper/gradle-wrapper.properties`, `8vim/build.gradle.kts`

**Thin CI provisioning pattern** (`.github/workflows/codecheck.yaml:16-23`):

```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
  with:
    java-version: "17"
    distribution: temurin
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v4
- run: ./gradlew check
```

Keep this checkout/setup shape, but make the unconditional PR job invoke only
`./scripts/baseline-check.sh`. Do not retain the current `changes` dependency or
job-level path conditions from `.github/workflows/pr-test.yml:18-105` for the
mandatory baseline job. YAML-layout validation may remain an additional job.

**Explicit task contract** (`8vim/build.gradle.kts:22-37,150-171,216-224`):

```kotlin
tasks.register<Checkstyle>("checkstyle") { /* tracked config */ }
tasks.check {
    dependsOn("checkstyle")
}
lint {
    abortOnError = true
    warningsAsErrors = true
}
tasks.withType<Test> {
    enabled = name == "testDebugUnitTest"
}
```

The shell script should use strict failure semantics, validate Java/SDK against
the manifest, then run the explicit module tasks in one wrapper invocation:

```text
:8vim:testDebugUnitTest :8vim:lint :8vim:ktlintCheck :8vim:checkstyle :8vim:assembleDebug
```

**Properties format** (`gradle/wrapper/gradle-wrapper.properties:1-5`):

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
```

Use flat deterministic `key=value` entries for wrapper `9.4.0`, Java `17`,
compile SDK `36`, and build tools `36.0.0`. The script must derive/check values,
not silently install or mutate the host toolchain.

### Mandatory upstream fixes (#614 and #604)

**Targets:** `LayoutScreen.kt`, `8vim/build.gradle.kts`

**Picker boundary pattern** (`LayoutScreen.kt:78-117`):

```kotlin
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )
    // Domain import result drives the existing alert.
}
return { launcher.launch(arrayOf("*/*")) }
```

Preserve SAF permission and parser validation; #614 is only the MIME widening
from `application/octet-stream` to `*/*`.

**Plugin-local reproducibility configuration** (`8vim/build.gradle.kts:8-18`):

```kotlin
plugins {
    alias(libs.plugins.mikepenz.aboutlibraries)
}
```

Add the minimal #604 block next to the plugin/build configuration:

```kotlin
aboutLibraries {
    excludeFields = arrayOf("generated")
}
```

Do not cherry-pick historical branches or alter dependency versions.

### Transactional custom-layout import and fallback

**Targets:** `LayoutScreen.kt`, `AvailableLayouts.kt`, `Layout.kt`, `AppPrefs.kt`

**Existing typed loader boundary** (`Layout.kt:79-97`):

```kotlin
fun <T> Layout<T>.loadKeyboardData(
    layoutLoader: LayoutLoader,
    context: Context,
): Either<LayoutError, KeyboardData> = md5(context)
    .toEither { ExceptionWrapperError(Exception("MD5")) }
    .flatMap { md5 ->
        val cache by context.cache()
        cache.load(md5).fold({
            inputStream(context).flatMap(layoutLoader::loadKeyboardData)
        }, { it.right() })
    }
```

Keep `Either<LayoutError, ...>` as the public failure contract. Cache raw parsed
data by content digest and derive URI-specific fallback display metadata after
cache lookup. Close resolver streams with `use`. URI string remains identity;
MD5 is change detection only.

**Serialized identity and preference pattern** (`Layout.kt:103-136`,
`AppPrefs.kt:52-72`):

```kotlin
is EmbeddedLayout -> "e${value.path}"
is CustomLayout -> "c${value.path}"

val current = custom(
    key = "prefs_layout_current",
    default = EmbeddedLayout("en"),
    serde = LayoutSerDe,
)
val history = stringSet(
    key = "prefs_layout_custom_history",
    default = emptySet(),
)
```

Add one narrowly scoped serialized previous-valid identity beside `current`.
Update it only after a successful identity change, never while refreshing the
already-active URI. This is the deterministic fallback; never use map order.

**Registry mutation seam** (`AvailableLayouts.kt:56-80`):

```kotlin
fun updateKeyboardData(layout: Layout<*>): Boolean =
    layoutsWithKeyboardData
        .getOrNone(layout)
        .flatMap { layout.loadKeyboardData(layoutLoader, context).getOrNone() }
        .onSome { prefs.layout.current.set(layout) }
        .isSome()
```

Replace the Boolean/split UI flow with one domain import operation returning a
typed result. Required order: validate → upsert URI entry → de-duplicate/move
history deterministically → remember previous valid identity → activate.
Failure for a new URI changes nothing; failure for a persisted URI prunes it.

**Stale-layout branch to replace** (`AvailableLayouts.kt:36-49`):

```kotlin
history.remove(path)
historyPref.set(history)
prefs.layout.current.reset() // currently unconditional; must become identity-aware
layoutsWithKeyboardData.remove(layout)
findIndex()
```

Branch on URI identity: inactive stale URI only disappears; active stale URI
restores previous valid identity, otherwise embedded `en`. Fix custom-map cleanup
to compare `layout.path.toString()`, not `layout.toString()`.

### Custom-layout JVM regressions

**Targets:** `AvailableLayoutsSpec.kt`, `LayoutSpec.kt`

**Mocked preference/domain boundary** (`AvailableLayoutsSpec.kt:44-79`):

```kotlin
beforeSpec {
    mockkStatic(::appPreferenceModel)
    mockkStatic(::embeddedLayouts)
    mockkStatic(String::toCustomLayout)
}
beforeTest {
    currentLayout = mockk(relaxed = true) {
        every { default } returns embeddedLayouts.first().first
    }
    historyData = mockk(relaxed = true) {
        every { get() } returns emptySet()
    }
}
```

Extend this public boundary; do not add hidden runtime test seams. Add named
tests for same-URI upsert/no duplicate/activation, different URI with same bytes,
failed new import/no mutation, inactive stale removal, active stale → previous,
active stale → `en`, and reload stability. Assert concrete identities, not index
zero.

**ContentResolver/cache pattern** (`LayoutSpec.kt:94-153`):

```kotlin
every { androidContentResolver.openInputStream(any()) } returns inputStream
val layout = spyk(CustomLayout(uri))
every { layoutLoader.loadKeyboardData(any()) } returns keyboardData.right()
layout.loadKeyboardData(layoutLoader, context) shouldBeRight expected
```

Use two distinct mocked URIs and equal MD5 bytes to prove identity/metadata do
not leak through the cache; verify streams close and changed same-URI content is
reparsed under a new digest.

### Enter/newline regression contract

**Targets:** `KeyboardManagerSpec.kt`, verify-only `KeyboardManager.kt` and
`EditorInstanceSpec.kt`, plus `docs/compatibility/enter-newline.md`

**Runtime seam** (`KeyboardManager.kt:203-220`):

```kotlin
when (val action = editorInstance.imeOptions.action) {
    DONE, GO, NEXT, PREVIOUS, SEARCH, SEND -> {
        if (editorInstance.imeOptions.flagNoEnterAction) {
            editorInstance.performEnter()
        } else {
            editorInstance.performEnterAction(action)
        }
    }
    else -> editorInstance.performEnter()
}
```

The current runtime already expresses the locked precedence. Drive it through
the public `onInputKeyUp` path used by `KeyboardManagerSpec.kt:267-287`.

**Platform delegation evidence** (`EditorInstanceSpec.kt:125-160`):

```kotlin
editorInstance.performEnterAction(action)
verify { inputConnection.performEditorAction(eq(action.toInt())) }

editorInstance.performEnter()
verify { editorInstance.sendDownAndUpKeyEvent(eq(KeyEvent.KEYCODE_ENTER), eq(0)) }
```

Add four representative manager cases: multiline `NONE`, multiline with
`NO_ENTER_ACTION`, single-line `SEND`/`DONE`, and explicit action with
`NO_ENTER_ACTION`. Parameterize explicit actions with Kotest `withData`; no new
fixture abstraction. The compatibility note must state JVM contract coverage
and explicitly defer the device/app matrix.

### Optional #622 gesture-semantics slice

**Targets (only after device checkpoint):** `AppPrefs.kt`, `GestureScreen.kt`,
`KeyboardController.kt`, `KeyboardControllerSpec.kt`, `strings.xml`

**Preference/UI pattern** (`AppPrefs.kt:234-242`, `GestureScreen.kt:14-30`):

```kotlin
val fnEnabled = boolean("prefs_keyboard_behavior_fn_enabled", default = true)

SwitchPreference(
    prefs.keyboard.behavior.fnEnabled,
    title = stringRes(R.string.settings__gesture__fn_enabled__title),
    summary = stringRes(R.string.settings__gesture__fn_enabled__summary),
)
```

If adopted, place `allowComplexGestures` in `Keyboard.Behavior`, default `true`
to preserve behavior, and expose it with the same `SwitchPreference` pattern.

**Gesture state-machine seam** (`KeyboardController.kt:211-251,294-317`):

```kotlin
if (currentFingerPosition == FingerPosition.INSIDE_CIRCLE) {
    processLayerMovements()
} else {
    detectKeySelection()
}
```

Keep the change isolated at unresolved center crossings. Reuse the existing
movement reset fields/methods; do not change normal key dispatch, long press, or
full rotation.

**Test sequence pattern** (`KeyboardControllerSpec.kt:216-275`):

```kotlin
every { event.actionMasked } answers { MotionEvent.ACTION_DOWN } andThenAnswer {
    MotionEvent.ACTION_MOVE
}
every { event.y } returnsMany sequence.map { it.ordinal.toFloat() }
sequence.forEach { controller.onTouchEventInternal(event) }
verifyOrder { eventDispatcher.sendDownUp(action, false) }
```

Add enabled-preserves, disabled-resets, and valid-center-hit tests. If no current
device is available or smoke fails, omit all five changes and record that result.

### Baseline provenance, smoke evidence, and immutable tag

**Targets:** `docs/compatibility/baseline.md` and an annotated git tag created
only after implementation (the tag is not a source file).

**Existing tag convention** (`.github/workflows/bump-version.yaml:77-95`):

```yaml
- name: Commit files
  run: |
    git commit -a -m "chore(bump): release v${{steps.vars.outputs.version_name}}"
    git tag v${{steps.vars.outputs.version_name}}
```

Follow the repository's version-derived naming style but use the locked single,
fork-specific immutable tag (recommended `fork-baseline-v0.18.0-rc.1`). Before
creation, verify it does not exist. Record tag name, target SHA, mandatory
baseline command result, #614/#604 provenance, custom-layout device/API smoke,
Enter JVM scope, and #622 accepted/omitted evidence. Tagging is the final gate.

## Shared Patterns

### Error handling

Use Arrow `Either<LayoutError, ...>` at layout boundaries and fold the typed
result into the existing UI alert (`LayoutScreen.kt:92-107`). State mutation
must happen only on the success branch.

### Persistent identity

Use `LayoutSerDe`'s `e`/`c` prefixes and URI string identity. Preference access
stays behind `appPreferenceModel()`; callers must not invent parallel storage.

### Testing

Use Kotest `FunSpec`/`WordSpec`, `withData`, and MockK at Android/platform
boundaries. Test production-facing methods; do not add test-only DI or hidden
function attributes. Execution must follow repository TDD rules.

### Scope fences

- No connected-test CI gate or full instrumentation matrix.
- No dependency modernization or historical Dependabot cherry-picks.
- No PR #553 version rollback.
- No #622 runtime changes before the manual checkpoint.
- No tag until all mandatory checks/evidence are complete.

## No Analog Found

None. New script and documentation targets have strong in-repository workflow,
properties, PRD, and release analogs; no ignored `.codex` runtime path is used.

## Metadata

**Analog search scope:** `.github/workflows/`, `8vim/`, `config/`, `docs/`,
`gradle/`

**Strong analog groups:** CI/build tooling; layout lifecycle; editor routing;
gesture controller; release/tagging

**Pattern extraction date:** 2026-09-17

