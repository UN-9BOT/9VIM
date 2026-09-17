---
phase: 01-stable-fork-baseline
plan: 06
subsystem: compatibility
tags: [android, gradle, ci, provenance, docker, git]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: pre-proof baseline ledger, pinned build gate, picker evidence, URI lifecycle, and JVM Enter contract
provides:
  - exact-SHA local proof of the manifest-compatible baseline command and focused JVM regressions
  - human-approved proof branch consumed by Plan 07
  - local and origin absence evidence for the intended immutable tag
affects: [phase-01-final-tag, ci, release-baseline]

# Actuals (#2632)
actuals:
  tokens: 3532
  tasks: 2
  commits: 3
plan_head_before: 7fce642ed9bf518522c2532dabe39b37bd1fe260

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Exact-SHA approval binds toolchain, full gate, focused regressions, and tag-absence evidence before release metadata is created.
    - Docker is used as a verification environment only; the host Java 26/SDK limitation is not treated as project status.

key-files:
  created:
    - .planning/phases/01-stable-fork-baseline/01-06-SUMMARY.md
  modified:
    - docs/compatibility/baseline.md
    - .planning/WINDOWS.md

key-decisions:
  - "Use the local proof branch with Java 17, Android API 36, and Build Tools 36.0.0 in the pinned Docker environment."
  - "The exact approved SHA is 5ffcd291ed865be4299a960a81370d642a281265; no other commit may authorize Plan 07."
  - "#622 remains omit-unavailable because the user intentionally chose skip; this is not a device-unavailable claim."
  - "The intended tag remains absent and is reserved for Plan 07; this plan creates no tag."

patterns-established:
  - "Record machine-readable proof fields alongside human-readable command output and exact SHA evidence."
  - "Keep pre-proof ledger wording honest while placing final approval evidence in the plan summary and tag workflow."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "Manifest-compatible local proof covers Java 17, Android API 36/Build Tools 36.0.0, the full baseline gate, and focused layout/Enter JVM regressions for one exact SHA."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "Docker ./scripts/baseline-check.sh at 5ffcd291ed865be4299a960a81370d642a281265"
        status: pass
      - kind: unit
        ref: "Docker filtered :8vim:testDebugUnitTest with AvailableLayoutsSpec, LayoutSpec, KeyboardManagerSpec, EditorInstanceSpec, and CustomLayoutImportAdapterSpec"
        status: pass
    human_judgment: true
    rationale: "The exact-SHA proof branch is a blocking human approval before Plan 07 may create the tag."
  - id: D2
    description: "Mandatory picker smoke remains passed and the optional #622 outcome remains explicitly settled as an intentional omission."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: manual_procedural
        ref: "01-04-SUMMARY.md device evidence: V2425A/API 36, picker=pass"
        status: pass
      - kind: other
        ref: "01-04-SUMMARY.md and 01-05-SUMMARY.md: #622=omit-unavailable"
        status: pass
    human_judgment: true
    rationale: "The picker and optional-scope decisions require the recorded device/user evidence; automation must not infer adoption."
  - id: D3
    description: "The intended tag fork-baseline-v0.18.0-rc.1 is absent from local refs and origin tag refs."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "git show-ref --tags --verify and git ls-remote --tags origin for fork-baseline-v0.18.0-rc.1"
        status: pass
    human_judgment: true
    rationale: "The user explicitly approved the exact proof branch; tag creation is deferred to dependent Plan 07."

# Metrics
duration: "3h 6m (includes blocking human checkpoint wait)"
completed: 2026-09-17
status: complete
commits: 3
---

# Phase 1 Plan 6: Stable Fork Baseline Summary

**Human-approved exact-SHA local Docker proof for the reproducible baseline, with tag creation deferred to Plan 07**

## Performance

- **Duration:** 3 h 6 min, including the blocking human checkpoint wait.
- **Started:** 2026-09-17T20:30:00+03:00
- **Completed:** 2026-09-17T23:36:00+03:00
- **Tasks:** 2
- **Files modified:** 2 tracked files, including planning metadata.

## Accomplishments

- Finalized the pre-proof provenance ledger with the canonical command,
  toolchain contract, picker/device evidence, upstream provenance, scope
  exclusions, and explicit host-tooling limitation.
- Proved the exact candidate SHA in Docker with Java 17, Android API 36, and
  Build Tools 36.0.0. The full `./scripts/baseline-check.sh` completed with
  `BUILD SUCCESSFUL`; the focused layout/Enter JVM command also completed with
  `BUILD SUCCESSFUL` under `C.UTF-8`.
- Preserved the passed picker result and intentional `#622` omission, and
  confirmed the intended tag is absent locally and in `origin`.

## Human Approval Record

The blocking checkpoint was approved exactly for the Task 1 HEAD:

```text
proof=local; approved_sha=5ffcd291ed865be4299a960a81370d642a281265; evidence=docker_java17_api36_baseline=BUILD_SUCCESSFUL_targeted_jvm=BUILD_SUCCESSFUL; picker=pass; #622=omit-unavailable; tag_absent_local=true; tag_absent_origin=true
```

Evidence details:

- Docker image: `thyrlian/android-sdk:latest`; SDK volume contains
  `platforms;android-36` and `build-tools;36.0.0`.
- Toolchain preflight output: `java_major=17`,
  `platform_android_36=present`, `build_tools_36.0.0=present`, and
  `baseline-check: preflight passed`.
- Full gate: `./scripts/baseline-check.sh` — `BUILD SUCCESSFUL in 14s`,
  `67 actionable tasks: 3 executed, 64 up-to-date`.
- Focused gate:
  `:8vim:testDebugUnitTest --rerun-tasks --console=plain` filtered to
  `AvailableLayoutsSpec`, `LayoutSpec`, `KeyboardManagerSpec`,
  `EditorInstanceSpec`, and `CustomLayoutImportAdapterSpec` —
  `BUILD SUCCESSFUL in 1m 22s`, `29 actionable tasks: 29 executed`.
- Prior device evidence: `picker=pass` on `V2425A`/API 36; valid custom YAML
  became active, same-URI import did not duplicate, and invalid/zero-layer
  imports left the current layout unchanged.
- The approved proof explicitly records `#622=omit-unavailable` as the
  user's intentional skip, not as a device-unavailable claim.
- `fork-baseline-v0.18.0-rc.1` was absent from local refs and `origin` tag
  refs. No tag was created or approved by this plan.

## Task Commits

Each task was committed atomically; the second Task 1 commit fixes only the
literal wording required by its automated acceptance search:

1. **Task 1: Finalize the pre-proof evidence ledger** — `97d20831` (`docs`)
2. **Task 1 verification wording correction** — `5ffcd291` (`fix`)
3. **Task 2: Record exact-SHA local proof approval and plan metadata** —
   included in the final metadata commit

**Plan metadata:** the final completion commit contains this summary,
`STATE.md`, `ROADMAP.md`, and related GSD bookkeeping.

## Files Created/Modified

- `docs/compatibility/baseline.md` — pre-proof provenance ledger; final proof
  fields remain intentionally reserved for the tag annotation/Plan 07.
- `.planning/phases/01-stable-fork-baseline/01-06-SUMMARY.md` — exact-SHA
  proof record and blocking-approval evidence.
- `.planning/WINDOWS.md` — closes the earlier SDK-blocked targeted verification
  entries after the Docker reruns passed.

## Decisions Made

- Selected `proof=local` because the manifest-compatible Docker environment
  provided Java 17, Android API 36, and Build Tools 36.0.0.
- Bound approval to
  `5ffcd291ed865be4299a960a81370d642a281265`; Plan 07 must reject any other
  SHA.
- Kept the mandatory picker evidence as `picker=pass` and the optional
  candidate as `#622=omit-unavailable` per the user's explicit skip.
- Deferred creation of `fork-baseline-v0.18.0-rc.1` to Plan 07.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Corrected baseline acceptance wording**

- **Found during:** Task 1 automated verification.
- **Issue:** The ledger said `Android SDK Platform 36` and formatted the host
  value as `Java \`26\``, so the plan's literal `API 36`/`Java 26` acceptance
  searches did not match.
- **Fix:** Reworded the same pre-proof limitation to include the required
  explicit forms without adding a final-green or tag-created claim.
- **Files modified:** `docs/compatibility/baseline.md`
- **Verification:** All Task 1 acceptance searches and `git diff --check`
  passed.
- **Committed in:** `5ffcd291`.

**Total deviations:** 1 auto-fixed (Rule 3: 1)
**Impact on plan:** Documentation-only correction; no runtime scope changed.

## Issues Encountered

- The original host still reports Java 26 and lacks the manifest SDK entries;
  the Docker proof keeps that limitation honest and does not mutate host
  tooling.
- A direct focused Gradle run with the container's default `US-ASCII` locale
  failed while writing a Unicode HTML report (`???`). Re-running the same
  command with `LANG=C.UTF-8 LC_ALL=C.UTF-8` passed. The canonical baseline
  script already applies this locale safeguard.
- Existing Gradle/AGP deprecation warnings were non-blocking; no unrelated
  source cleanup was introduced.

## User Setup Required

None — no external service configuration is required for this proof record.

## Next Phase Readiness

Plan 07 has an exact human-approved local proof, the approved SHA, picker and
`#622` outcomes, and verified tag absence. It may create exactly one immutable
annotated `fork-baseline-v0.18.0-rc.1` tag only on the approved SHA. This plan
did not create the tag.

## Self-Check: PASSED

- Summary file exists at the planned path.
- Task commits `97d20831` and `5ffcd291` are present after the persisted plan
  base `7fce642e`.
- The summary records the exact approved SHA and all required machine-readable
  proof fields.
- `git diff --check` is clean for the plan changes.
- No runtime, auth, network-endpoint, or schema surface was added by this
  plan.

---
*Phase: 01-stable-fork-baseline*
*Plan: 06*
*Completed: 2026-09-17*
