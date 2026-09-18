---
phase: "02"
slug: "multilingual-profiles-and-settings"
status: draft
nyquist_compliant: true
wave_0_complete: false
created: "2026-09-18"
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Kotest 5.9.1 + JUnit 5; MockK 1.14.9 at Android boundaries; Compose instrumentation for settings semantics |
| **Config file** | `8vim/build.gradle.kts` |
| **Quick run command** | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.*' --tests 'inc.flide.vim8.lib.backup.BackupManagerSpec' --tests 'inc.flide.vim8.lib.ZipUtilsSpec' --tests 'inc.flide.vim8.app.settings.CustomLayoutImportAdapterSpec'` |
| **Full suite command** | `./scripts/baseline-check.sh` |
| **Estimated runtime** | ~5 minutes in the pinned Java 17/API 36 Docker environment |

The host Java 26/API 36 gap is an environment limitation; use the Phase 1
pinned Docker command or exact-SHA CI for build/lint/APK gates.

## Sampling Rate

- **After every task commit:** Run the focused profile/backup JVM command above.
- **After every plan wave:** Run `./scripts/baseline-check.sh` in the pinned
  Java 17/API 36 environment.
- **Before `$gsd-verify-work`:** Focused tests, full baseline, and available
  settings instrumentation must be green.
- **Max feedback latency:** 300 seconds in Docker; 60 seconds for focused JVM
  tests when Gradle caches are warm.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | REQ-language-profiles | T-02-01, T-02-03, T-02-01-SAF/ZIP/RESTORE/PRIVACY | Fresh catalog has EN enabled and RU/LV disabled; initial/live IME loading uses one resolved manager session with no runtime legacy preference authority. | JVM unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec'` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | REQ-language-profiles | T-02-02, T-02-01-SAF/ZIP/RESTORE/PRIVACY | LanguageManager alone detects aggregate absence, calls pure LanguageMigration, normalizes, and performs the sole bootstrap write; repeated initialization is idempotent. | JVM unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec'` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | REQ-language-profiles | T-02-05, T-02-02-ZIP/RESTORE/PRIVACY | Co-located serde plus normalizer preserve ordered aggregate invariants and disabled embedded opt-ins while rejecting malformed input safely. | JVM unit/property | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec'` | ❌ W0 | ⬜ pending |
| 02-02-02 | 02 | 2 | REQ-language-profiles, REQ-language-settings | T-02-05, T-02-06, T-02-02-RESTORE | Manager transitions enforce primary-first remove-active, reject final-profile disable/delete without side effects, and expose one validated replaceAll boundary; AvailableLayouts has no runtime legacy preference access. | JVM unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec' --tests 'inc.flide.vim8.ime.layout.AvailableLayoutsSpec'` | ⚠️ one new, one existing | ⬜ pending |
| 02-02-03 | 02 | 2 | REQ-language-profiles, REQ-language-settings | T-02-04, T-02-06, T-02-02-ZIP/RESTORE/PRIVACY | SAF import balances read grants, validates before mutation, dedupes canonical URI identity, and never changes external documents. | JVM unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.app.settings.CustomLayoutImportAdapterSpec' --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec'` | ⚠️ adapter existing, manager W0 | ⬜ pending |
| 02-03-01 | 03 | 3 | REQ-language-profiles, REQ-language-settings | T-02-07, T-02-09, T-02-03-SAF/ZIP/RESTORE/PRIVACY | Unified EN/RU/custom list exposes accessible ordering, toggle, primary, and final-enabled protections over manager state. | instrumentation | `./gradlew :8vim:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest` | ❌ W0 | ⬜ pending |
| 02-03-02 | 03 | 3 | REQ-language-settings | T-02-07, T-02-08, T-02-03-SAF/ZIP/RESTORE/PRIVACY | Confirmed metadata/import/removal preserves identity; final-enabled delete is disabled/explained and remove-active displays the atomically selected valid primary. | instrumentation | `./gradlew :8vim:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest` | ❌ W0 | ⬜ pending |
| 02-04-01 | 04 | 4 | REQ-language-settings | T-02-10, T-02-11, T-02-15 | ZIP extraction rejects absolute/traversal/duplicate/over-quota entries and removes failed staging output. | JVM unit/property | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.lib.ZipUtilsSpec'` | ⚠️ existing, new cases | ⬜ pending |
| 02-04-02 | 04 | 4 | REQ-language-profiles, REQ-language-settings | T-02-12, T-02-13, T-02-14, T-02-15 | Current/version-9 archives validate in staging, exclude typed/NLP data, use one replaceAll path, and roll back every newly copied durable file on failure/interruption. | JVM unit/integration | `test -f 8vim/src/test/resources/backup/version-9-settings.json && ./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.lib.backup.BackupManagerSpec' --tests 'inc.flide.vim8.lib.ZipUtilsSpec'` | ⚠️ specs existing, fixture W0 | ⬜ pending |
| 02-04-03 | 04 | 4 | REQ-language-profiles, REQ-language-settings | T-02-12, T-02-13, T-02-15 | Fatal, complete, and partial-warning restore outcomes expose only committed usable manager state and persist across recreation. | instrumentation | `./gradlew :8vim:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest` | ❌ W0 | ⬜ pending |

## Wave 0 Requirements

- [ ] `8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageManagerSpec.kt` — aggregate serde, pure migration/manager bootstrap ownership, reorder, metadata, primary/active, final disable/delete rejection, replaceAll, fallback, and one resolved emission.
- [ ] Extend `8vim/src/test/kotlin/inc/flide/vim8/lib/backup/BackupManagerSpec.kt` and `8vim/src/test/kotlin/inc/flide/vim8/lib/ZipUtilsSpec.kt` — stable-ID remap, full replacement, rollback-ledger cleanup on failure/interruption, warnings, permission cleanup, and traversal rejection.
- [ ] `8vim/src/test/resources/backup/version-9-settings.json` — legacy archive fixture proving LanguageMigration and shared replaceAll compatibility.
- [ ] `8vim/src/androidTest/kotlin/inc/flide/vim8/app/LanguageProfilesScreenTest.kt` — unified list, up/down controls, primary radio, custom detail dialog, final-enabled toggle/delete explanation, remove-active primary fallback, and persistence.

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|-----------|-------------------|
| Active profile changes visible keyboard without flicker or IME recreation | REQ-language-profiles | Android IME lifecycle/render timing is not fully observable in JVM tests. | On the connected API 36 device, enable EN/RU/custom LV, select each row repeatedly, and confirm the existing keyboard view remains while layout data changes with no blank/old-layout flash. |
| Missing custom document restore warning | REQ-language-settings | SAF provider availability and user-visible warning depend on device/file picker behavior. | Restore an archive after removing one staged custom document; confirm remaining profiles/order stay usable and a non-fatal warning is shown. |

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 300s for full gates
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
