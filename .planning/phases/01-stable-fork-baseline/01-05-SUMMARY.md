---
phase: 01-stable-fork-baseline
plan: 05
subsystem: compatibility
tags: [android, compatibility, provenance, custom-layouts, upstream]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: device-gated SAF picker evidence, URI-based layout lifecycle, and JVM Enter contract
provides:
  - explicit omission of optional upstream #622 without runtime/resource changes
  - consolidated baseline provenance ledger for selected, deferred, and excluded work
affects: [phase-01-final-proof, ci, custom-layouts, enter-routing]

# Actuals (#2632)
actuals:
  tokens: 1330
  tasks: 2
  commits: 2
plan_head_before: 7bc8dc30e54eab71566a3708a8834011c8fd66a5

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Evidence ledger separates automated checks, manual device smoke, conditional decisions, deferred scope, and exclusions.
    - Optional upstream changes require an explicit Plan 04 adoption outcome before source mutation.

key-files:
  created:
    - docs/compatibility/baseline.md
  modified: []

key-decisions:
  - "#622 remains omitted: the user intentionally chose skip; this does not claim that the available Android device was unavailable."
  - "The final CI URL/SHA and immutable tag target stay reserved for Plan 06 and are not asserted in the pre-proof ledger."
  - "#553, historical Dependabot work, version rollback, and dependency modernization remain outside this baseline."

patterns-established:
  - "Baseline provenance records exact device/APK/fixture hashes and links each claim to its evidence class."
  - "URI identity, MD5 cache-only semantics, active-aware stale fallback, and JVM-only Enter scope are stated as product contracts."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "Optional #622 omission is gated by the recorded Plan 04 outcome and leaves all five optional runtime/resource files unchanged."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "git diff --exit-code -- five optional #622 files"
        status: pass
    human_judgment: true
    rationale: "The omission is an explicit user scope decision; source cleanliness is automated, but adoption is not inferred by automation."
  - id: D2
    description: "Baseline provenance ledger records #604/#614, picker device/API/APK SHA evidence, URI/fallback rules, Enter JVM scope, exclusions, and deferred final fields."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "rg acceptance contract for docs/compatibility/baseline.md"
        status: pass
    human_judgment: true
    rationale: "The note consolidates prior manual and automated evidence; final CI and tag proof intentionally remain for Plan 06."

# Metrics
duration: 7min
completed: 2026-09-17
commits: 2
status: complete
---

# Phase 1 Plan 5: Stable Fork Baseline Summary

**Optional #622 is deliberately absent, with mandatory upstream, picker, URI, Enter, and exclusion provenance consolidated for final proof**

## Performance

- **Duration:** 7 min
- **Started:** 2026-09-17T17:21:12Z
- **Completed:** 2026-09-17T17:28:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Verified the omission branch from Plan 04 and kept `AppPrefs.kt`,
  `GestureScreen.kt`, `KeyboardController.kt`, `KeyboardControllerSpec.kt`,
  and `strings.xml` byte-for-byte unchanged.
- Added `docs/compatibility/baseline.md` with the canonical command and pinned
  toolchain contract, #604/#614 provenance, and current-device picker evidence.
- Recorded URI identity/stale-fallback behavior, the JVM-only Enter command and
  boundary, the exact #622 decision, and #553/Dependabot exclusions.
- Reserved final CI URL/SHA and immutable tag fields for Plan 06 without making
  an early final-proof claim.

## Task Commits

1. **Task 1: Apply or omit #622 exactly as the device gate directs** - no source
   commit; omission verified with a clean diff for all five optional files.
2. **Task 2: Consolidate mandatory and optional baseline provenance** -
   `aa9be271` (`docs`)

**Plan metadata:** recorded in the final plan-completion commit.

## Files Created/Modified

- `docs/compatibility/baseline.md` - evidence ledger for the Phase 1 baseline.

## Decisions Made

- Kept #622 out of the fork because the user explicitly chose `skip`; the
  recorded `#622=omit-unavailable` schema outcome is not a device-unavailable
  claim.
- Kept the final CI run, approved SHA, and immutable tag target as Plan 06
  fields; this note does not preempt the exact proof gate.
- Documented #553/version rollback, historical Dependabot changes, and
  dependency modernization as excluded scope.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- No new issue blocked this plan. The omission verification and documentation
  acceptance checks passed. Existing historical unrun-verification entries in
  `.planning/WINDOWS.md` remain for the phase-level gate to resolve.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Plan 06 can fill the reserved final CI/SHA/tag fields after selecting and
approving one exact proof branch. The optional gesture files remain untouched,
and the provenance note is ready for final review.

---
*Phase: 01-stable-fork-baseline*
*Plan: 05*
*Completed: 2026-09-17*

## Self-Check: PASSED

- `docs/compatibility/baseline.md` exists and passes every plan acceptance
  search, including the no-early-final-proof assertion.
- `01-04-SUMMARY.md` records `#622=omit-unavailable`.
- All five optional #622 files have a clean diff and unchanged SHA-256 values.
- Commit `aa9be271` exists and is the measured task commit after the plan base.
- `git diff --check` is clean for the plan changes.
