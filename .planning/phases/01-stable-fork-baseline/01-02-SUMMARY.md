---
phase: 01-stable-fork-baseline
plan: 02
subsystem: layout
tags: [android, kotlin, custom-layouts, uri, md5, cache, kotest, mockk]

# Dependency graph
requires:
  - phase: 01-stable-fork-baseline
    provides: stable fork baseline and existing layout preference/loader boundaries
provides:
  - typed transactional custom-layout import with URI-keyed upsert and activation
  - persisted previous-valid identity for deterministic stale-layout fallback
  - one-snapshot raw content cache with URI-specific metadata derivation
  - JVM regression coverage for import, cache, stream, and fallback behavior
affects: [custom-layout-picker, layout-reload, ime-startup, phase-02-language-profiles]

# Actuals (#2632)
actuals:
  tokens: 9512
  tasks: 3
  commits: 9
plan_head_before: 4488baf92cbf32a5d917c9c727bc431f2300838d

# Tech tracking
tech-stack:
  added: []
  patterns:
    - URI string is the custom-layout identity; MD5 is cache/change detection only.
    - Raw KeyboardData is cached before URI-derived display metadata is applied.
    - Stale cleanup branches on active identity and restores previous-valid before embedded en.

key-files:
  created: []
  modified:
    - 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt
    - 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt
    - 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt
    - 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/LayoutSpec.kt

key-decisions:
  - "Custom URI strings remain the durable identity, so equal content does not merge available entries."
  - "A single previous-valid serialized layout is updated only on a successful identity change, not on same-URI refresh."
  - "previousValid is local-only (canBeExported=false) so backup/restore cannot export an inaccessible provider URI."
  - "New invalid imports are non-mutating; failures for already-known URIs prune history and apply active-aware fallback."

patterns-established:
  - "Validate before mutating current/history; return Either<LayoutError, ...> from the domain import boundary."
  - "Read each provider stream once into a closed byte snapshot, then hash and parse that snapshot."

requirements-completed: [REQ-stable-fork-baseline]

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "Validated custom URI import is upserted once, added to history, activated, and refreshed without duplicates."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: unit
        ref: "8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt#Import a custom layout"
        status: unknown
    human_judgment: true
    rationale: "The targeted Gradle task was blocked during Android project configuration because the local Android SDK is missing."
  - id: D2
    description: "URI identities stay distinct across equal bytes while changed content refreshes the MD5-keyed raw cache and provider streams close."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: unit
        ref: "8vim/src/test/kotlin/inc/flide/vim8/ime/layout/LayoutSpec.kt#reads one closed snapshot for digest and parser"
        status: unknown
    human_judgment: true
    rationale: "The targeted Gradle task could not reach test discovery without an Android SDK location."
  - id: D3
    description: "Known stale URIs are removed, inactive failures preserve current, and active failures restore previous-valid or embedded en deterministically."
    requirement: REQ-stable-fork-baseline
    verification:
      - kind: unit
        ref: "8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt#Prune stale custom layouts"
        status: unknown
    human_judgment: true
    rationale: "The targeted Gradle task stopped at configuration due the unavailable Android SDK, so runtime assertions remain to be confirmed in CI/device-capable tooling."

# Metrics
duration: 21min
completed: 2026-09-17
commits: 9
status: complete
---

# Phase 1 Plan 2: Stable Fork Baseline Summary

**Transactional URI-based custom-layout import with deterministic stale fallback and raw MD5 cache snapshots**

## Performance

- **Duration:** 21 min
- **Started:** 2026-09-17T10:06:48Z
- **Completed:** 2026-09-17T10:28:12Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- Added a typed `importLayout` boundary that validates a custom layout before changing state, upserts by URI, de-duplicates history, saves the prior valid identity, and activates the imported entry immediately.
- Reworked layout loading around one closed byte snapshot; MD5 now only keys raw parsed content, while URI-specific names are derived after cache lookup and changed bytes reparse.
- Centralized stale URI pruning: inactive broken entries leave the current layout untouched; active entries restore the persisted previous-valid layout or embedded `en`, never an arbitrary map entry.

## Task Commits

Each task was implemented with RED-first TDD commits and production commits:

1. **Task 1: Drive one valid picker candidate through validate, upsert, remember, and activate** - `6c269564` (RED), `d60026e3` (GREEN)
2. **Task 2: Separate URI identity and metadata from the MD5 content cache** - `a89ce9b4` (RED), `69f833f7` (GREEN), `30a3cb54` (coverage)
3. **Task 3: Prune stale URIs with active-aware previous/default fallback** - `0dc0ff49` (RED), `b56a365c` (GREEN), `db9990a7` (validation coverage), `e458326b` (backup-safe fallback identity)

The plan metadata commit is pending after state and roadmap updates.

## TDD Gate Compliance

- RED tests were committed before each corresponding implementation slice.
- The required commands were attempted after implementation:
  `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.layout.AvailableLayoutsSpec' --tests 'inc.flide.vim8.ime.layout.LayoutSpec'`.
- Verification stopped during Gradle configuration with `SDK location not found`; no test assertion pass is claimed. The unrun verification is recorded in `.planning/WINDOWS.md` for CI follow-up.

## Files Created/Modified

- `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt` - Adds the serialized local previous-valid layout identity, defaulting to embedded `en`.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt` - Implements typed import/upsert, URI history de-duplication, active-aware stale cleanup, and deterministic fallback.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt` - Implements safe startup fallback and one-snapshot MD5/cache/parser loading with closed streams.
- `8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt` - Covers import transaction, duplicate identity, invalid input, stale pruning, and repeated reloads.
- `8vim/src/test/kotlin/inc/flide/vim8/ime/layout/LayoutSpec.kt` - Covers URI metadata isolation, byte-change refresh, provider/parser failures, and stream closure.

## Decisions Made

- URI string is the product identity; MD5 remains an internal raw-content cache key and is never used for identity or integrity.
- Same-URI refresh reuses one registry identity and does not overwrite `previousValid` when the URI is already active.
- Persisted previous-valid identity is marked non-exportable to avoid restoring an inaccessible external URI through backup data; a restore without it safely uses embedded `en`.
- A known stale URI is removed from both available state and history; only the active stale URI triggers fallback.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Applied deterministic previous-valid fallback during IME startup**

- **Found during:** Task 3 (stale URI pruning)
- **Issue:** `safeLoadKeyboardData` still reset the current layout directly when a persisted custom URI failed, bypassing the required previous-valid then embedded-default policy.
- **Fix:** Removed the unconditional reset, removed the broken current URI from history, validated the persisted previous layout, and fell back to the declared default when it was unavailable or empty.
- **Files modified:** `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt`
- **Verification:** Static diff review and targeted tests added; Gradle execution remained blocked by missing Android SDK.
- **Committed in:** `b56a365c`

**2. [Rule 2 - Missing Critical] Kept previous-valid fallback local to the app**

- **Found during:** Task 3 (fallback preference review)
- **Issue:** Exporting an external provider URI as fallback state could make backup/restore point at a URI without a valid permission grant.
- **Fix:** Set `canBeExported=false` for `prefs_layout_previous_valid`; fallback remains migration-safe and defaults to embedded `en`.
- **Files modified:** `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt`
- **Verification:** Preference declaration review; no backup export path is introduced.
- **Committed in:** `e458326b`

**Total deviations:** 2 auto-fixed (Rule 2: 2)

**Impact on plan:** Both changes are correctness and data-safety safeguards within the requested layout lifecycle; no unrelated scope was added.

## Issues Encountered

- The local runtime has Java 26 and no Android SDK/platform configured. The required Gradle test command fails before compilation/test discovery with `SDK location not found`.
- No package, SDK, or local properties were installed or mutated. CI with the pinned Android toolchain must execute the RED/GREEN test suite before runtime verification is considered complete.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The domain boundary is ready for the later SAF picker adapter to call `importLayout` and surface its typed `LayoutError`. CI/device-capable validation remains required for the JVM regressions and the picker flow.

## Self-Check: PASSED

- Summary file exists at the planned path.
- All nine task commits are present between `plan_head_before` and `HEAD`.
- `git diff --check` is clean for the plan diff.
- No production or test stubs were found in the files modified by this plan.

---
*Phase: 01-stable-fork-baseline*
*Plan: 02*
*Completed: 2026-09-17*
