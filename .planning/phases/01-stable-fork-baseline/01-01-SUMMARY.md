---
phase: 01-stable-fork-baseline
plan: 01
subsystem: infra
tags: [ci, gradle, android-sdk, shell, github-actions]

# Dependency graph
requires: []
provides:
  - "Manifest-validated baseline preflight and one canonical Gradle gate command"
  - "Unconditional pull-request baseline job with Java 17 and Android API 36"
  - "Deterministic AboutLibraries metadata without dependency churn"
affects: [phase-02, ci, build-baseline]

# Actuals (#2632)
actuals:
  tokens: 3248
  tasks: 3
  commits: 3
plan_head_before: abe492a7019d68e26301b9f5e313ee7c62c1bbe2

# Tech tracking
tech-stack:
  added: ["android-actions/setup-android@v3"]
  patterns: ["flat toolchain manifest", "preflight-before-build", "single unconditional CI entrypoint"]

key-files:
  created:
    - scripts/baseline-check.sh
    - config/baseline-toolchain.properties
  modified:
    - .github/workflows/pr-test.yml
    - 8vim/build.gradle.kts

key-decisions:
  - "The seven baseline toolchain values are versioned in a flat properties manifest."
  - "Preflight reports observed versus expected Java and Android SDK values and never mutates the host."
  - "Every pull request runs the repository script; path filters and conditional mandatory jobs are removed."
  - "AboutLibraries excludes generated metadata only; dependency versions remain unchanged."

patterns-established:
  - "The repository-owned script is the sole source of the mandatory Gradle task list."
  - "CI provisions the manifest toolchain before invoking the baseline script."

requirements-completed: [REQ-stable-fork-baseline]

coverage:
  - id: D1
    description: "Canonical baseline script validates all seven manifest values and runs the mandatory Gradle gates once."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "Task 1 structural verification command"
        status: pass
      - kind: other
        ref: "./scripts/baseline-check.sh"
        status: fail
    human_judgment: true
    rationale: "The local host has Java 26 and no Android Platform 36/build-tools 36.0.0; CI provisioning must provide the final green proof."
  - id: D2
    description: "Mandatory baseline job runs on every pull request with Java 17 and Android API 36/build-tools 36.0.0."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "Task 2 PyYAML structural verification command"
        status: pass
    human_judgment: false
  - id: D3
    description: "AboutLibraries output excludes generated metadata without changing dependency or wrapper versions."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "Task 3 rg and git diff verification command"
        status: pass
    human_judgment: false

# Metrics
duration: 10min
completed: 2026-09-17
commits: 3
status: complete
---

# Phase 1 Plan 1: Stable Fork Baseline Summary

**A reproducible, manifest-validated baseline command with unconditional PR execution and deterministic library metadata**

## Performance

- **Duration:** 10 min
- **Started:** 2026-09-17T12:47:03+03:00
- **Completed:** 2026-09-17T12:57:23+03:00
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Added `scripts/baseline-check.sh` with strict preflight checks for Gradle, AGP, Java, SDK declarations, Android Platform 36, and build-tools 36.0.0.
- Added the pinned `config/baseline-toolchain.properties` contract and a single wrapper invocation covering unit tests, lint, ktlint, checkstyle, and debug APK assembly.
- Replaced path-filtered PR topology with an unconditional baseline job that provisions Java 17 and Android API 36.
- Ported only the reproducible AboutLibraries `generated` metadata exclusion.

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire one manifest-validated baseline command through the real Gradle gates** - `78a4a73b` (`chore`)
2. **Task 2: Make the baseline command unconditional on every pull request** - `e6500f4e` (`chore`)
3. **Task 3: Port the reproducible AboutLibraries metadata fix only** - `3e6c1aa6` (`chore`)

## Files Created/Modified

- `scripts/baseline-check.sh` - canonical preflight and Gradle baseline entrypoint.
- `config/baseline-toolchain.properties` - explicit Gradle/AGP/JDK/SDK contract.
- `.github/workflows/pr-test.yml` - unconditional Java/Android baseline job.
- `8vim/build.gradle.kts` - deterministic AboutLibraries metadata configuration.

## Decisions Made

- Keep connected Android tests outside the mandatory command; this plan provisions only the unit/lint/build baseline.
- Fail before Gradle when the host does not match the declared Java or Android SDK contract, so local limitations are visible.
- Keep the existing dependency graph and wrapper unchanged while applying PR #604 behavior.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The local host reports Java 26 instead of the required Java 17 and has no configured Android SDK Platform 36 or build-tools 36.0.0. The new preflight exits non-zero with observed/expected values as designed.
- The targeted JVM command also stopped before tests because `ANDROID_HOME`/`local.properties` did not identify an SDK. No source test result was claimed as green.

## User Setup Required

None - CI provisions the declared toolchain; local developers need Java 17 and Android SDK Platform 36/build-tools 36.0.0 for a green preflight.

## Next Phase Readiness

The repository now has the baseline entrypoint and PR wiring needed by subsequent plans. The final phase baseline proof remains dependent on a CI runner or local host with Java 17 and Android SDK API 36.

## Self-Check: PASSED

- All four key files exist.
- Three task commits are present after the persisted plan base.
- Task acceptance and structural verification commands passed.
- Full local baseline failure is recorded as an expected environment limitation, not a false green result.

---
*Phase: 01-stable-fork-baseline*
*Completed: 2026-09-17*
