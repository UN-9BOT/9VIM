---
phase: 01-stable-fork-baseline
plan: 07
subsystem: compatibility
tags: [git, provenance, release-baseline, immutable-tag]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: human-approved exact-SHA local proof, picker evidence, and settled optional scope
provides:
  - annotated local and origin baseline tag at the approved proof commit
  - remote tag-object and peeled-target evidence for immutable baseline comparison
affects: [phase-01-verification, release-baseline, regression-comparison]

# Actuals (#2632)
actuals:
  tokens: 2784
  tasks: 2
  commits: 1
plan_head_before: 55840f6ae047df44f07ae3f7bf23b8d98a255de2

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Exact approved commit is used as the tag target; planning-only commits remain outside the baseline marker.
    - Annotated tag publication is verified through both remote tag-object and peeled commit references.

key-files:
  created:
    - refs/tags/fork-baseline-v0.18.0-rc.1
    - .planning/phases/01-stable-fork-baseline/01-07-SUMMARY.md
  modified:
    - .planning/STATE.md
    - .planning/ROADMAP.md
    - .planning/state.json

key-decisions:
  - "Bind fork-baseline-v0.18.0-rc.1 to the human-approved commit 5ffcd291ed865be4299a960a81370d642a281265, not the later planning-only HEAD."
  - "Create and publish exactly one annotated tag through an explicit non-force tag-ref push; never move, overwrite, delete, or reuse it."
  - "Preserve proof=local, picker=pass, and #622=omit-unavailable in the tag annotation and publication record."

patterns-established:
  - "Record local tag object, remote tag object, peeled target, planning-only HEAD, and UTC verification time for release provenance."
  - "Keep all post-proof commits confined to .planning metadata so the immutable marker remains a product-tree snapshot."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "One local annotated fork-baseline-v0.18.0-rc.1 tag targets the exact Plan 06 approved commit."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "git cat-file -t tag; git rev-list -n 1 fork-baseline-v0.18.0-rc.1"
        status: pass
    human_judgment: false
  - id: D2
    description: "Origin exposes the same annotated tag object and a peeled target equal to the approved SHA."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "git ls-remote --tags origin refs/tags/fork-baseline-v0.18.0-rc.1 and ^{}"
        status: pass
    human_judgment: false
  - id: D3
    description: "The approved SHA to planning-only HEAD path contains only .planning metadata changes."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: other
        ref: "git diff --name-only 5ffcd291ed865be4299a960a81370d642a281265..HEAD"
        status: pass
    human_judgment: false

# Metrics
duration: 8min
completed: 2026-09-17
status: complete
commits: 1
---

# Phase 1 Plan 7: Stable Fork Baseline Summary

**Published the immutable annotated fork baseline at the human-approved proof SHA with matching origin tag evidence**

## Performance

- **Duration:** 8 min.
- **Started:** 2026-09-17T20:47:49Z.
- **Completed:** 2026-09-17T20:54:10Z.
- **Tasks:** 2.
- **Files modified:** 4 planning files; one annotated Git ref created.

## Accomplishments

- Parsed the sole machine-readable approval from Plan 06 and validated
  `5ffcd291ed865be4299a960a81370d642a281265` as an ancestor commit.
- Confirmed every path after the approved SHA was under `.planning/`; no
  source, build, runtime, or documentation file changed after proof.
- Rechecked local and `origin` tag absence, then created exactly one annotated
  `fork-baseline-v0.18.0-rc.1` tag explicitly on the approved SHA.
- Pushed only `refs/tags/fork-baseline-v0.18.0-rc.1` without force and verified
  the remote tag object and peeled commit target.

## Publication Evidence

```text
proof=local
approved_sha=5ffcd291ed865be4299a960a81370d642a281265
planning_only_head=55840f6ae047df44f07ae3f7bf23b8d98a255de2
tag=fork-baseline-v0.18.0-rc.1
local_tag_object=bec2b0006f504c8506d014e2bdecacbeffb64fb1
remote_tag_object=bec2b0006f504c8506d014e2bdecacbeffb64fb1
remote_peeled_sha=5ffcd291ed865be4299a960a81370d642a281265
picker=pass
#622=omit-unavailable
tag_created_at_utc=2026-09-17T20:47:49Z
remote_verified_at_utc=2026-09-17T20:51:33Z
push_result=success; ref=refs/tags/fork-baseline-v0.18.0-rc.1; force=false
```

The annotation itself contains the proof branch, approved SHA, Docker
baseline/targeted-JVM evidence, picker result, and settled `#622` outcome.
The pre-create checks found no local or origin tag. The final origin query
returned the same tag object `bec2b0006f504c8506d014e2bdecacbeffb64fb1` and
peeled target `5ffcd291ed865be4299a960a81370d642a281265`.

## Task Commits

1. **Task 1: Recheck absence and create the annotated local tag** — Git ref
   `refs/tags/fork-baseline-v0.18.0-rc.1`, tag object
   `bec2b0006f504c8506d014e2bdecacbeffb64fb1` (not a commit).
2. **Task 2: Publish the exact tag ref and verify remote immutability evidence**
   — recorded in the plan metadata commit.

**Plan metadata:** recorded in the final plan-completion commit.

## Files Created/Modified

- `refs/tags/fork-baseline-v0.18.0-rc.1` — annotated marker for the approved
  product-tree commit.
- `.planning/phases/01-stable-fork-baseline/01-07-SUMMARY.md` — local and
  remote publication evidence.
- `.planning/STATE.md`, `.planning/ROADMAP.md`, `.planning/state.json` — GSD
  completion metadata.

## Decisions Made

- Used only the human-approved Plan 06 SHA as the tag target; the later
  planning-only `HEAD` was explicitly excluded.
- Published only the exact tag ref with a non-force push and compared both
  local and remote tag objects plus the peeled commit target.
- Kept the user-selected `#622=omit-unavailable` outcome unchanged and did
  not touch optional runtime files or the pre-proof documentation ledger.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

- An extra post-push remote query briefly hit a transient DNS resolution error;
  the required remote tag and peeled-target verification was retried and passed
  at `2026-09-17T20:51:33Z`. No tag overwrite or additional ref push occurred.

## User Setup Required

None — no additional external service configuration is required.

## Next Phase Readiness

Phase 1 has its reproducible local proof and shared immutable comparison marker.
The verifier can use `fork-baseline-v0.18.0-rc.1` as the regression baseline;
the product tree remains exactly the approved SHA despite later planning
metadata commits.

---
*Phase: 01-stable-fork-baseline*
*Plan: 07*
*Completed: 2026-09-17*

## Self-Check: PASSED

- Summary file exists at the planned path.
- Approved commit `5ffcd291ed865be4299a960a81370d642a281265` and annotated tag
  object `bec2b0006f504c8506d014e2bdecacbeffb64fb1` resolve correctly.
- Local and origin tag checks show the same peeled target and no force update.
- Post-proof path validation contains only `.planning/` changes.
- `git diff --check` is clean for the summary changes.
