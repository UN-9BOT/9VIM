---
phase: 01-stable-fork-baseline
plan: 03
subsystem: testing
tags: [android, ime, enter, newline, kotest, mockk, compatibility]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: stable KeyboardManager/EditorInstance seams and JVM test conventions
provides:
  - four representative Enter/newline routing regression families at onInputKeyUp
  - explicit JVM-only compatibility note with deferred device/app boundary
affects: [enter-routing, ime-compatibility, phase-02-language-profiles]

# Actuals (#2632)
actuals:
  tokens: 1790
  tasks: 2
  commits: 3
plan_head_before: 0207bbefe916e867ab343d6f6e6340b12272f95c

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Public KeyboardManager.onInputKeyUp tests assert the selected and rejected EditorInstance paths.
    - Explicit editor actions are parameterized only for the NO_ENTER_ACTION precedence family.
    - JVM evidence and Android device/app compatibility claims remain separate.

key-files:
  created:
    - docs/compatibility/enter-newline.md
  modified:
    - 8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt

key-decisions:
  - "Keep KeyboardManager.kt unchanged: its existing handleEnter implementation already gives IME_FLAG_NO_ENTER_ACTION precedence."
  - "Cover SEND and DONE as named single-line cases, and parameterize only the six explicit actions with NO_ENTER_ACTION."
  - "Document JVM routing evidence without claiming messaging-app or current-device compatibility."

patterns-established:
  - "Enter tests use the production KEYCODE_ENTER path and verify exactly one EditorInstance method."
  - "InputConnection delegation remains covered by EditorInstanceSpec, separate from routing precedence."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "Four representative Enter/newline routing families cover flag precedence, real Enter, and explicit editor actions."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: unit
        ref: "8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt#Enter routing through onInputKeyUp"
        status: unknown
    human_judgment: true
    rationale: "The required Gradle command stopped during project configuration because the local Android SDK location is unavailable; test assertions need CI or an SDK-capable host."
  - id: D2
    description: "Compatibility note records the precedence table, JVM reproduction command, platform delegation boundary, and deferred device/app matrix."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "rg acceptance checks for docs/compatibility/enter-newline.md"
        status: pass
    human_judgment: false

# Metrics
duration: 8min
completed: 2026-09-17
status: complete
---

# Phase 1 Plan 3: Stable Fork Baseline Summary

**Enter/newline precedence regression coverage at the public keyboard key-up seam with an explicit JVM-only compatibility boundary**

## Performance

- **Duration:** 8 min
- **Started:** 2026-09-17T10:29:40Z
- **Completed:** 2026-09-17T10:37:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added named JVM regression cases for multiline `NONE`, multiline
  `NO_ENTER_ACTION`, single-line `SEND`/`DONE`, and all six explicit actions
  with `NO_ENTER_ACTION`.
- Every case exercises `KeyboardManager.onInputKeyUp` and verifies both the
  selected `EditorInstance` call and the absence of the alternative call.
- Recorded the `KeyboardManager` versus `EditorInstance` responsibility split
  and deferred Android device/app compatibility claims in a durable note.

## Task Commits

Each task was committed atomically:

1. **Task 1: Prove Enter routing through onInputKeyUp with four locked cases** -
   `08e5fa6a` (test)
2. **Task 2: Record the JVM contract and deferred device/app boundary** -
   `45b26a1c` (docs)

**Plan metadata:** recorded in the final plan-completion commit.

## TDD Gate Compliance

- **RED:** The regression tests were added and committed before any runtime
  edit. The required Gradle command was attempted, but project configuration
  stopped with `SDK location not found` before test discovery; no failing
  assertion or `RED_EVIDENCE_OK` result is claimed.
- **GREEN:** Source inspection confirms the existing `handleEnter()` already
  implements the locked precedence, so no production correction was needed.
  The test suite could not be executed locally to claim a green assertion run.
- **REFACTOR:** None; the existing runtime implementation is unchanged.

## Files Created/Modified

- `8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt` -
  public key-up routing cases with negative-call assertions.
- `docs/compatibility/enter-newline.md` - precedence table, reproduction
  command, platform boundary, and deferred compatibility scope.

## Decisions Made

- Kept `KeyboardManager.kt` unchanged because its existing implementation
  already gives `IME_FLAG_NO_ENTER_ACTION` priority.
- Used no new fixture abstraction and limited Kotest parameterization to the
  explicit-action family.
- Treated JVM tests as the contract while explicitly deferring device/app
  compatibility evidence.

## Deviations from Plan

None - plan executed as written. The required JVM verification was attempted
and is documented as environment-blocked below.

## Issues Encountered

- `./gradlew :8vim:testDebugUnitTest --tests
  'inc.flide.vim8.ime.keyboard.text.KeyboardManagerSpec' --tests
  'inc.flide.vim8.ime.editor.EditorInstanceSpec'` fails during Gradle
  configuration because `local.properties`/Android SDK location is missing.
- The host reports Java 26; the phase manifest requires Java 17. No SDK or
  Java installation was added or mutated.
- The unrun verification is recorded in `.planning/WINDOWS.md` for CI or a
  properly provisioned local environment.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The routing contract is regression-visible and documented for downstream
multilingual work. Run the targeted JVM command in CI or on a host with Java 17
and Android SDK configuration before treating the assertions as green.

---
*Phase: 01-stable-fork-baseline*
*Completed: 2026-09-17*

## Self-Check: PASSED

- Summary file exists at the planned path.
- Task commits `08e5fa6a` and `45b26a1c` are present.
- `git diff --check` is clean for the plan changes.
- No production or test stubs were found in the files modified by this plan.
