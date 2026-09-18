---
phase: "02"
slug: "multilingual-profiles-and-settings"
status: draft
nyquist_compliant: false
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
| 02-01-01 | 01 | 1 | REQ-language-profiles | T-02-01 / — | Malformed snapshots normalize to valid embedded EN without partial state. | unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageConfigSerDeSpec'` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | REQ-language-profiles | T-02-02 / — | Legacy migration is idempotent and prunes stale custom URIs without random fallback. | unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageMigrationSpec'` | ❌ W0 | ⬜ pending |
| 02-01-03 | 01 | 1 | REQ-language-profiles | T-02-03 / — | Manager mutations preserve enabled/primary/active invariants and emit a fully resolved session. | unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.LanguageManagerSpec'` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | REQ-language-settings | T-02-04 / — | Backup whitelists profile state/documents and rejects traversal/partial restore. | unit | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.lib.backup.BackupManagerSpec' --tests 'inc.flide.vim8.lib.ZipUtilsSpec'` | ⚠️ existing files, new cases | ⬜ pending |
| 02-02-02 | 02 | 2 | REQ-language-settings | T-02-05 / — | Unified settings controls preserve order/primary and expose accessible state labels. | instrumentation | `./gradlew :8vim:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest` | ❌ W0 | ⬜ pending |
| 02-03-01 | 03 | 3 | REQ-language-profiles | T-02-06 / — | Active selection updates keyboard data without recreating the input view or publishing null. | device smoke | `adb shell am instrument -w -e class inc.flide.vim8.app.LanguageProfilesScreenTest <test-apk-runner>` | ❌ manual/device | ⬜ pending |

## Wave 0 Requirements

- [ ] `8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageConfigSerDeSpec.kt` — ordered snapshot round-trip and malformed/missing schema cases.
- [ ] `8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageMigrationSpec.kt` — fresh install, legacy current/history, stale URI, dedupe, and repeated initialization matrix.
- [ ] `8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageManagerSpec.kt` — reorder, metadata, primary/active, disable/remove, fallback, and one resolved emission.
- [ ] Extend `8vim/src/test/kotlin/inc/flide/vim8/lib/backup/BackupManagerSpec.kt` and `8vim/src/test/kotlin/inc/flide/vim8/lib/ZipUtilsSpec.kt` — stable-ID remap, full replacement, warnings, permission cleanup, and traversal rejection.
- [ ] `8vim/src/androidTest/kotlin/inc/flide/vim8/app/LanguageProfilesScreenTest.kt` — unified list, up/down controls, primary radio, custom detail dialog, toggle/delete semantics, and persistence.

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
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
