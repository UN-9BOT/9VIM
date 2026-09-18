---
phase: 02-multilingual-profiles-and-settings
plan: "01"
subsystem: language-state
tags: [kotlin, sharedpreferences, stateflow, migration, android-ime]
requires:
  - phase: 01-stable-fork-baseline
    provides: pinned Java 17/API 36 build route and validated layout loading
provides:
  - versioned ordered LanguageConfig aggregate with stable profile identities
  - application-scoped LanguageManager with load-before-write active selection
  - deterministic one-time migration from legacy layout preferences
  - resolved language sessions observed by the existing IME view
affects: [02-02, 02-03, 02-04, language-settings, backup-restore, switching]
actuals:
  tokens: 9586
  tasks: 2
  commits: 4
plan_head_before: fa8c7d039d02f6eadbcfba1ed917d8eb3180d23b
tech-stack:
  added: []
  patterns:
    - one versioned aggregate preference for ordered language state
    - application-scoped sole writer publishing resolved non-null sessions
    - pure legacy transformation guarded by aggregate-key presence
key-files:
  created:
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageProfile.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageMigration.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageManager.kt
    - 8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageManagerSpec.kt
  modified:
    - 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt
key-decisions:
  - "Profile IDs are immutable references; a custom profile keeps its ID when sourceUri changes."
  - "Only true aggregate absence consults legacy state; malformed-present data repairs to safe embedded en."
patterns-established:
  - "Load-then-swap: resolve non-zero-layer KeyboardData before the sole aggregate write and session publication."
  - "Catalog reconciliation preserves persisted order and appends newly discovered embedded profiles disabled."
requirements-completed: [REQ-language-profiles]
coverage:
  - id: D1
    description: "Fresh state enables embedded en and catalogs other embedded profiles disabled."
    requirement: REQ-language-profiles
    verification:
      - kind: unit
        ref: "LanguageManagerSpec#boots with only embedded en enabled and publishes a resolved session"
        status: pass
    human_judgment: false
  - id: D2
    description: "Legacy current/history migrate once in deterministic order with stale-profile fallback."
    requirement: REQ-language-profiles
    verification:
      - kind: unit
        ref: "LanguageManagerSpec#legacy migration"
        status: pass
    human_judgment: false
  - id: D3
    description: "Stable profile identity is independent from mutable custom source URI metadata."
    requirement: REQ-language-profiles
    verification:
      - kind: unit
        ref: "LanguageConfigSerDe round-trip and migration identity cases"
        status: pass
    human_judgment: false
  - id: D4
    description: "Active selection loads before one write and updates the existing IME session without recreation."
    requirement: REQ-language-profiles
    verification:
      - kind: integration
        ref: "LanguageManagerSpec plus Docker ./scripts/baseline-check.sh"
        status: pass
    human_judgment: false
duration: 18min
completed: 2026-09-18
status: complete
---

# Phase 02 Plan 01: Language State Tracer Summary

**Atomic ordered language profiles with idempotent legacy migration and load-before-swap IME sessions**

## Performance

- **Duration:** 18 min
- **Started:** 2026-09-18T15:24:48Z
- **Completed:** 2026-09-18T15:42:27Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added a strict versioned `LanguageConfig` aggregate and stable embedded/custom profile model.
- Centralized bootstrap, reconciliation, selection, persistence, and resolved sessions in one manager.
- Preserved valid legacy current/history deterministically while pruning stale custom sources.
- Rewired `Vim8ImeService` to consume resolved sessions inside its existing Compose view.

## TDD Gate Compliance

- RED tracer: `5a11a30b`; intentional aggregate assertion failure verified as `RED_EVIDENCE_OK`.
- GREEN tracer: `5b0f77f6`; focused spec passed in pinned Java 17/API 36 Docker.
- RED migration: `b85e5e40`; intentional legacy-order assertion verified as `RED_EVIDENCE_OK`.
- GREEN migration: `722a6a5b`; seven focused cases and ktlint passed.

## Task Commits

1. **Task 1 RED: language session tracer** — `5a11a30b` (`test`)
2. **Task 1 GREEN: aggregate-to-IME tracer** — `5b0f77f6` (`feat`)
3. **Task 2 RED: legacy migration matrix** — `b85e5e40` (`test`)
4. **Task 2 GREEN: idempotent legacy bootstrap** — `722a6a5b` (`feat`)

## Files Created/Modified

- `LanguageProfile.kt` — domain contracts and strict JSON serde.
- `LanguageMigration.kt` — aggregate presence reader and pure legacy transformer.
- `LanguageManager.kt` — catalog resolver, sole writer, and resolved session publisher.
- `LanguageManagerSpec.kt` — tracer, failure atomicity, migration, repair, and restart coverage.
- `AppPrefs.kt` — schema 10 aggregate preference registration.
- `VIM8Application.kt` — one lazy process-wide manager.
- `Vim8ImeService.kt` — resolved-session observation without input-view recreation.
- `Layout.kt` — removed preference-mutating initial layout load.

## Decisions Made

- Kept custom profile ID separate from `sourceUri`, preserving future restore remapping.
- Treated malformed-present aggregate bytes as repair input, never as permission to resurrect legacy state.
- Kept all layout I/O behind a production `LanguageLayoutCatalog` boundary.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The host has Java 26 and no Android SDK. All verification used the established pinned Docker route.
- The first RED attempt was a compile-only failure and was rejected; compile scaffolding was added and RED was rerun to an intentional assertion failure before GREEN.
- A stale Gradle HTML report path blocked one run; `cleanTestDebugUnitTest` removed generated output and the valid RED run completed.

## Verification

- Focused `LanguageManagerSpec`: 7/7 passed.
- `:8vim:ktlintCheck`: passed.
- `./scripts/baseline-check.sh`: passed in 1m43s; tests, lint, ktlint, checkstyle, and debug APK succeeded.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Aggregate, manager, and migration boundaries are ready for invariant-safe mutations and SAF lifecycle work in Plan 02.
- Legacy keys remain registered for compatibility; later settings/backup plans must finish replacing their legacy UI/archive call sites.

## Self-Check: PASSED

- All key files exist and all four task commits resolve.
- TDD RED/GREEN commit patterns are present and the measured ledger count is four.
- Focused and full pinned verification passed with no uncommitted production changes.

---
*Phase: 02-multilingual-profiles-and-settings*
*Completed: 2026-09-18*
