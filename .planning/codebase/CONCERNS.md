---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# Codebase Concerns

**Analysis Date:** 2026-09-17

## Tech Debt

**Legacy preference persistence:**

- Issue: Application settings still use the default `SharedPreferences` store and custom serialization rather than a typed, transactional store.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt`, `8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceSerDe.kt`
- Impact: Schema changes and partial writes are difficult to reason about; malformed values can surface as runtime failures.
- Fix approach: Introduce a versioned typed persistence boundary and make migrations/updates atomic.

**Build checks are selectively disabled:**

- Issue: Lint disables dependency, target API, resource, and several compatibility checks; only the debug unit-test task is enabled in the custom `Test` configuration.
- Files: `8vim/build.gradle.kts`
- Impact: Dependency drift and Android compatibility regressions can reach release builds without an enforced signal.
- Fix approach: Re-enable checks one category at a time and keep an explicit CI task matrix for unit, instrumentation, lint, and release verification.

## Known Bugs

**Backup restore can partially overwrite application files:**

- Symptoms: Import copies the extracted directory recursively into `context.filesDir` before all semantic validation succeeds.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt`
- Trigger: Import an archive with invalid, unexpected, or malformed entries that passes the initial JSON read.
- Workaround: Only import backups produced by the application and retain a manual backup of app data.

## Security Considerations

**Zip path traversal (Zip Slip):**

- Risk: `ZipUtils.unzip` constructs `File(dstDir, flexEntry.name)` without canonical-path containment validation. A crafted `../` entry can write outside the destination, and restore then copies into `filesDir`.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/lib/ZipUtils.kt`, `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt`
- Current mitigation: The archive is first extracted under a UUID directory in the app files area; no entry-name validation is present.
- Recommendations: Resolve canonical paths and reject entries outside the destination, reject absolute paths and symlinks, enforce entry/count/size limits, and validate the complete archive before copying.

**Sensitive keyboard data in backups:**

- Risk: Export serializes all `prefs.exportedKeys` and custom layouts into a ZIP in cache; backups may contain user dictionaries, replacements, or other typing preferences.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt`, `8vim/src/main/kotlin/inc/flide/vim8/ime/text/TextReplacementManager.kt`
- Current mitigation: Files are held in app-private cache/files directories.
- Recommendations: Document contents clearly, minimize exported data, and offer encryption/integrity protection for user-shared backups.

## Performance Bottlenecks

**Synchronous coroutine bridging in input handling:**

- Problem: Key dispatch and long-press interruption use `runBlocking` on input/UI paths.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/ime/input/InputEventDispatcher.kt`, `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/xpad/KeyboardController.kt`
- Cause: Suspending work is forced to complete synchronously for each gesture/key event.
- Improvement path: Use a service-owned coroutine scope, non-blocking launches, and explicit cancellation/backpressure for repeat and long-press events.

## Fragile Areas

**Global application and loader state:**

- Files: `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`, `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/LayoutLoader.kt`, `8vim/src/main/kotlin/inc/flide/vim8/lib/compose/Resources.kt`
- Why fragile: Module-level references, `lateinit`/`!!`, and a mutable cached `KeyboardData` assume lifecycle initialization order and single-process access.
- Safe modification: Keep context/application dependencies explicit, make cache invalidation synchronized, and test service/activity recreation and process death.
- Test coverage: Existing tests exercise layout behavior but do not cover lifecycle races or cache invalidation under concurrent loads.

**Null assertions at runtime boundaries:**

- Files: `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt`, `8vim/src/main/kotlin/inc/flide/vim8/ime/lifecycle/LifecycleInputMethodService.kt`, `8vim/src/main/kotlin/inc/flide/vim8/lib/compose/SystemUi.kt`
- Why fragile: Missing resources, window tokens, or lifecycle state cause crashes instead of recoverable UI/service errors.
- Safe modification: Convert boundary lookups to explicit nullable/error results and add recreation/configuration tests.
- Test coverage: No instrumentation coverage is present for these failure paths.

## Scaling Limits

**Unbounded backup materialization:**

- Current capacity: Export/import copies every custom layout and archive entry using `copyTo` with no configured size or count limit.
- Limit: Large or adversarial archives consume cache/filesystem space and memory/time during JSON and ZIP processing.
- Scaling path: Stream with quotas, reject oversized entries/archives, clean temporary directories in `finally`, and avoid fixed `backup.zip` collisions.

## Dependencies at Risk

**Android/Kotlin toolchain compatibility workarounds:**

- Risk: The build pins current AGP/Kotlin compatibility via deprecated DSL and disables lint checks due to known K2/AGP failures.
- Files: `8vim/build.gradle.kts`, `gradle/libs.versions.toml`
- Impact: Toolchain upgrades can fail abruptly or conceal new API/dependency issues.
- Migration plan: Track the upstream compatibility fixes, remove deprecated DSL usage, and restore disabled lint checks after each upgrade.

## Missing Critical Features

**Backup integrity and transactional restore:**

- Problem: There is no manifest/signature, schema validation of all fields, or rollback if restore fails midway.
- Blocks: Safe recovery from corrupted or tampered user backups.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt`

## Test Coverage Gaps

**Archive security and failure handling:**

- What's not tested: Zip Slip, absolute paths, symlinks, oversized entries, malformed settings, partial-copy rollback, and cleanup.
- Files: `8vim/src/test/kotlin/inc/flide/vim8/lib/ZipUtilsSpec.kt`, `8vim/src/test/kotlin/inc/flide/vim8/lib/backup/BackupManagerSpec.kt`
- Risk: Crafted or corrupted backups can escape intended storage or leave inconsistent preferences/files.
- Priority: High

**Android lifecycle/input integration:**

- What's not tested: `Vim8ImeService` recreation, window/token absence, asynchronous gesture cancellation, and configuration changes.
- Files: `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt`, `8vim/src/main/kotlin/inc/flide/vim8/ime/input/InputEventDispatcher.kt`, `8vim/src/androidTest/kotlin/inc/flide/vim8/app/SettingsScreenTest.kt`
- Risk: Device-specific crashes and input freezes remain undetected.
- Priority: Medium

---

*Concerns audit: 2026-09-17*
