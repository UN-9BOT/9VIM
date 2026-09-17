---
phase: 01-stable-fork-baseline
plan: 04
subsystem: layout
tags: [android, saf, compose, custom-layouts, device-smoke, upstream-614]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: transactional custom-layout import, deterministic stale fallback, and baseline test/toolchain gates
provides:
  - production SAF adapter using the wildcard OpenDocument MIME contract
  - current-device evidence for valid and invalid custom-layout picker flows
  - explicit user decision to omit optional upstream #622 from Phase 1
affects: [custom-layout-picker, phase-01-plan-05, phase-01-final-baseline]

# Actuals (#2632)
actuals:
  tokens: 2562
  tasks: 2
  commits: 3
plan_head_before: 7052a6f2481da9d5aedc389dc66b4ffe4766dabe

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Android SAF provider concerns stay in a typed adapter; domain validation and mutations stay in AvailableLayouts.
    - Manual device evidence distinguishes required picker acceptance from optional gesture adoption.

key-files:
  created:
    - 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt
    - 8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt
  modified:
    - 8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt

key-decisions:
  - "The real picker uses */* and requests only persistable read permission before one typed AvailableLayouts import call."
  - "The baseline picker passed on V2425A/API 36: valid custom YAML became active, repeated URI import created no duplicate, and invalid/zero-layer imports alerted without changing the current layout."
  - "#622=omit-unavailable: the user intentionally skipped the optional candidate in Phase 1; no candidate/device adoption evidence or runtime change is claimed."

patterns-established:
  - "OpenDocument URI -> persistable read grant -> typed import result -> existing alert/current UI."
  - "Optional upstream behavior is omitted unless its isolated candidate receives explicit current-device evidence."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "SAF import adapter exposes */*, requests a persistable read grant, delegates once, and folds typed failures into the existing alert."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: unit
        ref: "8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt"
        status: unknown
    human_judgment: true
    rationale: "The targeted Gradle command was attempted but stopped during Android project configuration because the local Android SDK location is unavailable; the unrun verification is recorded in WINDOWS.md."
  - id: D2
    description: "The baseline APK accepts a valid provider YAML, activates it, reuses the same URI without a duplicate, and rejects invalid/zero-layer YAML without changing the current layout."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: manual_procedural
        ref: "Device V2425A/API 36; baseline APK SHA-256 e700bc2fa678635c2940434baac826b1d1a58b05541322ec17d29b61e6d2fff6"
        status: pass
    human_judgment: true
    rationale: "SAF provider selection, persisted URI access, active-layout behavior, and visible alerts require a real Android device."
  - id: D3
    description: "The optional #622 adoption gate has one explicit omission outcome and leaves baseline runtime code unchanged."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: manual_procedural
        ref: "User decision: #622=omit-unavailable"
        status: pass
    human_judgment: true
    rationale: "Adoption is a user-controlled scope decision; the optional candidate was deliberately not created or tested."

# Metrics
duration: "6h 11m (includes human checkpoint wait)"
completed: 2026-09-17
commits: 3
status: complete
---

# Phase 1 Plan 4: Stable Fork Baseline Summary

**SAF custom-layout import is device-proven, with deterministic duplicate/error behavior and an explicit omission of optional #622**

## Performance

- **Duration:** 6 h 11 min including the human checkpoint wait; device smoke occurred before this close-out.
- **Started:** 2026-09-17T10:38:49Z
- **Completed:** 2026-09-17T16:50:00Z
- **Tasks:** 2
- **Files modified:** 3 production/test files plus planning metadata

## Accomplishments

- Added `CustomLayoutImportAdapter` as the production Android SAF boundary. It
  uses `*/*`, requests only persistable read access, delegates one typed import
  transaction, and returns a typed UI-facing result.
- Simplified `LayoutScreen` to fold the adapter result into the existing alert;
  validation, URI identity, history, and current-layout mutation remain in the
  domain boundary.
- Verified the baseline APK on `V2425A` (`10CF4J15A9002FS`), Android API 36.
  Valid custom YAML imported and worked, repeat import of the same URI produced
  no duplicate, and invalid/zero-layer YAML showed validation alerts while the
  current layout stayed unchanged.

## Device Evidence

| Field | Evidence |
|---|---|
| Baseline commit | `0358d9ae1f2dc7ec415b6014e1ee1b8b7a66fb00` |
| APK | `8vim/build/outputs/apk/debug/8vim-debug.apk` |
| APK SHA-256 | `e700bc2fa678635c2940434baac826b1d1a58b05541322ec17d29b61e6d2fff6` |
| Device | `V2425A`, serial `10CF4J15A9002FS` |
| Android API | `36` |
| Picker outcome | `picker=pass` |

Fixtures copied to the device:

- valid: `/sdcard/Download/9vim-custom-layout-test.yaml`, SHA
  `ae6ac586...c9051`
- invalid: `/sdcard/Download/9vim-invalid-layout-test.yaml`, SHA
  `52dd52...e54b`
- zero-layer: `/sdcard/Download/9vim-zero-layer-layout-test.yaml`, SHA
  `79cd9c...4477`

The valid layout became active and available; selecting it again via the same
URI did not create a duplicate. The invalid and zero-layer fixtures produced
validation alerts and left the current layout unchanged. Screenshots are kept
as local `.idea/` evidence and are not baseline source artifacts.

## Optional #622 Gate

`#622=omit-unavailable` is recorded as the required schema outcome, with an
explicitly honest reason: the user intentionally chose to skip this optional
candidate in Phase 1. The Android device was available for the mandatory picker
smoke; this omission does **not** claim that the device was unavailable. No
disposable #622 worktree, candidate APK, gesture test, adoption evidence, or
runtime change was created. Plan 05 must preserve the baseline without #622.

## Task Commits

1. **Task 1: Connect OpenDocument through a production SAF import adapter** -
   `c52e6dd9` (RED spec), `0358d9ae` (production implementation)
2. **Task 2: Record current-device picker evidence and the #622 adoption gate** -
   recorded in this plan metadata commit; no production files changed

**Plan metadata:** recorded in the final plan-completion commit.

## TDD Gate Compliance

- The adapter spec was committed before the production adapter implementation.
- The targeted command was attempted:
  `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.app.settings.CustomLayoutImportAdapterSpec' --tests 'inc.flide.vim8.ime.layout.AvailableLayoutsSpec' --tests 'inc.flide.vim8.ime.layout.LayoutSpec'`.
- Local Gradle configuration stopped before test discovery because the Android
  SDK location is unavailable. No local unit-test pass is claimed; the existing
  unrun verification is entry 3 in `.planning/WINDOWS.md`.

## Files Created/Modified

- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt` - typed SAF/provider boundary.
- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt` - wildcard picker and thin result wiring.
- `8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt` - MIME, permission, delegation, and error contract.

## Decisions Made

- Keep provider permission and exception handling in the adapter; keep layout
  validation and state changes in `AvailableLayouts`.
- Treat the mandatory picker as passed only from the real device evidence above.
- Omit #622 deliberately for this phase. It has no candidate or adoption claim.

## Deviations from Plan

None - the plan's required picker evidence was completed and the optional
gesture branch was explicitly omitted by the user as allowed by D-15.

## Issues Encountered

- The local targeted Gradle verification remains environment-blocked at
  configuration because Android SDK location is missing. This is recorded in
  `.planning/WINDOWS.md`; the manual device checkpoint is independent evidence
  for the SAF provider boundary.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The required SAF picker behavior is proven on a current API 36 device. Plan 05
should record the optional #622 omission and retain the current gesture runtime;
no candidate patch is available to port. CI or an SDK-capable host still needs
to run the targeted JVM suite and the full baseline gate.

---
*Phase: 01-stable-fork-baseline*
*Plan: 04*
*Completed: 2026-09-17*

## Self-Check: PASSED

- Summary file and adapter/spec files exist at the planned paths.
- Task 1 commits `c52e6dd9` and `0358d9ae` are present.
- The recorded APK SHA-256 matches the local baseline APK.
- `git diff --check` is clean for the plan changes.
- No production or test stubs were found in the files modified by this plan.
