---
phase: 01-stable-fork-baseline
verified: 2026-09-17T21:45:10Z
status: passed
score: 21/21 must-haves verified
covered_files:
  - .github/workflows/pr-test.yml
  - .planning/phases/01-stable-fork-baseline/01-01-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-01-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-02-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-02-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-03-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-03-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-04-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-04-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-05-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-05-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-06-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-06-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-07-PLAN.md
  - .planning/phases/01-stable-fork-baseline/01-07-SUMMARY.md
  - .planning/phases/01-stable-fork-baseline/01-CONTEXT.md
  - .planning/phases/01-stable-fork-baseline/01-DISCUSSION-LOG.md
  - .planning/phases/01-stable-fork-baseline/01-PATTERNS.md
  - .planning/phases/01-stable-fork-baseline/01-RESEARCH.md
  - .planning/phases/01-stable-fork-baseline/01-VALIDATION.md
  - .planning/phases/01-stable-fork-baseline/COVERAGE.md
  - 8vim/build.gradle.kts
  - 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt
  - 8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt
  - 8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt
  - 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt
  - 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt
  - 8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt
  - 8vim/src/test/kotlin/inc/flide/vim8/ime/editor/EditorInstanceSpec.kt
  - 8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt
  - 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt
  - 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/LayoutSpec.kt
  - config/baseline-toolchain.properties
  - docs/compatibility/baseline.md
  - docs/compatibility/enter-newline.md
  - scripts/baseline-check.sh
covered_digest: "v1:sha256:45c377e8fb5973c410abe73c0941af75eae9b7f0d8529de633ff77a1929a41fb"
behavior_unverified: 0
overrides_applied: 0
advisory:
  - finding: "The execute:post code-review hook did not produce REVIEW.md after three read-only reviewer attempts."
    category: other
    reason: "The hook is configured onError=skip; direct source audit and all executable gates were completed."
    evidence_status: "reviewer agents stalled; no source change was made"
human_verification: []
---

# Phase 1: Stable Fork Baseline Verification Report

**Phase Goal:** Developers and users have a reproducible, regression-visible
baseline on which multilingual work can safely build.

**Verified:** 2026-09-17T21:45:10Z

**Status:** passed

**Verification mode:** Initial goal-backward audit. The dedicated verifier
agent stalled; this report records the same checks directly against the
codebase, artifacts, executable gates, and local/remote refs.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|---|---|---|
| 1 | A clean checkout produces the debug APK through the documented CI command, with unit tests and configured lint/style checks passing. | ✓ VERIFIED | Docker run of `./scripts/baseline-check.sh` passed on Java 17/API 36/Build Tools 36.0.0; script invokes unit, lint, ktlint, checkstyle, and `assembleDebug`. |
| 2 | A current Android device can select a custom layout through the file picker and use explicit override behavior. | ✓ VERIFIED | User/device smoke on `V2425A`, API 36: valid YAML became active and available; repeated URI produced no duplicate; invalid/zero-layer YAML alerted without changing current layout. |
| 3 | Enter/newline behavior is reproducible and fixed for representative messaging fields. | ✓ VERIFIED | `KeyboardManagerSpec` and `EditorInstanceSpec` targeted JVM command passed; `NO_ENTER_ACTION` precedence and multiline newline cases are covered. |
| 4 | A baseline tag/commit exists for regression comparison without historic dependency bulk-merges. | ✓ VERIFIED | Annotated local/origin tag `fork-baseline-v0.18.0-rc.1` peels to approved SHA `5ffcd291…`; baseline ledger excludes #553, rollback, and historical Dependabot work. |
| 5 | Every mandatory baseline gate is observable through one repository command and fails on a failed sub-gate. | ✓ VERIFIED | `scripts/baseline-check.sh` uses strict failure semantics and one wrapper invocation for all required tasks. |
| 6 | Manifest/toolchain expectations are explicit and every pull request runs the same command without path filters. | ✓ VERIFIED | `config/baseline-toolchain.properties` declares seven values; `.github/workflows/pr-test.yml` has an unconditional `pull_request` baseline job. |
| 7 | Connected Android tests remain separate smoke evidence. | ✓ VERIFIED | CI baseline script contains no connected-test task; device evidence is recorded separately in Plan 04/06 summaries. |
| 8 | #604 reproducible metadata is ported without dependency modernization or historic Dependabot changes. | ✓ VERIFIED | `excludeFields = arrayOf("generated")` is configured; version catalog/dependency files were not modernized. |
| 9 | Validated custom URI imports are upserted once and activated, with URI rather than MD5/name as identity. | ✓ VERIFIED | `AvailableLayouts.importLayout` validates before mutation, updates history/current, and `sameIdentity` compares custom URI strings; matching JVM specs pass. |
| 10 | Stale inactive URIs are pruned without changing current; stale active URIs restore previous-valid identity or embedded `en`, never map order. | ✓ VERIFIED | `reloadCustomLayouts`/`removeStaleLayout` remove history and branch on active identity; `restorePreviousValidOrDefault` is covered by passing fallback specs. |
| 11 | A new invalid import is typed and leaves current/history unchanged. | ✓ VERIFIED | `Either<LayoutError, Layout<*>>` import path validates before mutation; invalid and zero-layer specs pass. |
| 12 | Enter behavior has an honest JVM-only compatibility boundary. | ✓ VERIFIED | `docs/compatibility/enter-newline.md` and `baseline.md` explicitly defer device/app matrix; targeted JVM tests pass. |
| 13 | `IME_FLAG_NO_ENTER_ACTION` always wins over explicit editor action. | ✓ VERIFIED | `KeyboardManager.handleEnter()` checks `flagNoEnterAction` before `performEnterAction`; parameterized precedence tests pass. |
| 14 | Exactly the representative Enter behavior families are covered without a Cartesian fixture layer. | ✓ VERIFIED | Existing JVM specs contain multiline NONE, multiline NO_ENTER_ACTION, SEND/DONE, and explicit-action-with-flag cases. |
| 15 | SAF picker accepts provider files through `*/*`, delegates the transaction, activates valid layouts, and alerts without mutation on failure. | ✓ VERIFIED | `CustomLayoutImportAdapter` uses wildcard MIME contract and typed result; adapter/layout specs and device smoke pass. |
| 16 | Picker behavior is manually proven on a current device, independently of JVM tests. | ✓ VERIFIED | Device `V2425A`/API 36 smoke and screenshots are recorded in Plan 04/06 evidence. |
| 17 | #622 has an explicit current-device adopt/omit outcome before runtime changes. | ✓ VERIFIED | Plan 04/05 record `#622=omit-unavailable` as intentional user skip; optional five files have clean diffs. |
| 18 | Omitted #622 leaves all optional runtime/resource files untouched. | ✓ VERIFIED | `git diff --exit-code` passes for `AppPrefs.kt`, `GestureScreen.kt`, `KeyboardController.kt`, `KeyboardControllerSpec.kt`, and `strings.xml`. |
| 19 | #553 rollback and historical Dependabot work are explicitly excluded. | ✓ VERIFIED | Exclusions are written in `docs/compatibility/baseline.md` and Plan 05 summary; no such changes appear after the baseline tag. |
| 20 | Exact-SHA proof is human-approved and deferred scope is not silently promoted. | ✓ VERIFIED | User approved `proof=local` for `5ffcd291…`; connected tests, Enter device matrix, and #622 remain explicitly scoped/deferred. |
| 21 | One annotated immutable tag is created on the approved SHA, with only planning metadata after proof and matching remote peeled target. | ✓ VERIFIED | Local/remote tag object `bec2b000…` is annotated; peeled target equals approved SHA; `5ffcd291..HEAD` contains only `.planning/` paths. |

**Score:** 21/21 truths verified.

## Required Artifacts

| Artifact | Status | Details |
|---|---|---|
| `scripts/baseline-check.sh` | ✓ EXISTS + SUBSTANTIVE + WIRED | Strict preflight and one canonical Gradle gate; called by PR workflow. |
| `config/baseline-toolchain.properties` | ✓ EXISTS + SUBSTANTIVE | Gradle/AGP/Java/SDK/build-tools contract is explicit. |
| `docs/compatibility/baseline.md` | ✓ EXISTS + SUBSTANTIVE | Records selected, manual, deferred, and excluded evidence. |
| `docs/compatibility/enter-newline.md` | ✓ EXISTS + SUBSTANTIVE | JVM-only Enter contract and deferred matrix. |
| `AvailableLayouts.kt` / `Layout.kt` | ✓ EXISTS + SUBSTANTIVE + WIRED | URI identity, cache semantics, validation, stale cleanup, and deterministic fallback are used by settings/IME paths. |
| `CustomLayoutImportAdapter.kt` / `LayoutScreen.kt` | ✓ EXISTS + SUBSTANTIVE + WIRED | SAF boundary connects provider URI to domain import and typed UI alert handling. |
| `KeyboardManager.kt` plus JVM specs | ✓ EXISTS + SUBSTANTIVE + WIRED | Enter routing is exercised by named passing tests. |
| `01-01..01-07-SUMMARY.md` | ✓ EXISTS + SUBSTANTIVE | Each plan has completion evidence and self-checks. |
| `fork-baseline-v0.18.0-rc.1` | ✓ EXISTS + ANNOTATED | Local and origin refs share tag object and peeled target. |

**Artifacts:** 9/9 required artifact groups verified.

## Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `.github/workflows/pr-test.yml` | `scripts/baseline-check.sh` | unconditional PR job | ✓ WIRED | Workflow run block contains exactly the repository command. |
| `scripts/baseline-check.sh` | Gradle module gates | tracked wrapper invocation | ✓ WIRED | Unit, lint, ktlint, checkstyle, and APK tasks are listed once. |
| `LayoutScreen` | `AvailableLayouts.importLayout` | `CustomLayoutImportAdapter` | ✓ WIRED | SAF URI permission and typed result delegate to the transaction. |
| `AvailableLayouts` | `Layout.loadKeyboardData` | validation/cache path | ✓ WIRED | Import/reload/select all load and validate snapshots before state mutation. |
| stale current URI | previous/default layout | `restorePreviousValidOrDefault` | ✓ WIRED | Active identity is checked; no arbitrary first-valid selection exists. |
| `KeyboardManager.handleEnter` | `EditorInstance` | `performEnter`/`performEnterAction` | ✓ WIRED | Flag precedence is explicit and named tests pass. |
| Plan 06 approval | local/origin tag | approved SHA parsing and peeled refs | ✓ WIRED | Tag object `bec2b000…` resolves to approved commit, not planning HEAD. |

**Wiring:** 7/7 connections verified.

## Requirements Coverage

| Requirement | Status | Evidence |
|---|---|---|
| `REQ-stable-fork-baseline` | ✓ SATISFIED | CI/toolchain gate, upstream scope, URI lifecycle, Enter JVM regression, device picker smoke, and immutable tag are all evidenced. |

**Coverage:** 1/1 phase requirements satisfied.

## Behavioral Spot-Checks

| Behavior | Command/evidence | Result |
|---|---|---|
| Full baseline | Docker `./scripts/baseline-check.sh` with manifest SDK volume and UTF-8 locale | ✓ PASS — `BUILD SUCCESSFUL` |
| Focused layout/Enter/SAF regressions | Docker filtered `testDebugUnitTest` | ✓ PASS — `BUILD SUCCESSFUL` |
| Optional-file omission | `git diff --exit-code -- <five files>` | ✓ PASS |
| Tag immutability | `git cat-file`, `git rev-list`, `git ls-remote --tags` | ✓ PASS — matching object/peeled SHA |

## Anti-Patterns Found

| Severity | Finding | Status |
|---|---|---|
| ℹ️ Info | Existing AGP/Gradle deprecation warnings appear during build. | Non-blocking; unrelated to this phase's scope. |
| 📋 Advisory | Code-review hook did not produce `01-REVIEW.md` after three read-only attempts. | Skipped by configured `onError=skip`; direct audit found no blocker. |

**Blockers:** 0. **Warnings:** 0. **Advisories:** 1.

## Human Verification Required

None pending. The required device picker smoke was completed by the user on
`V2425A`, and the blocking exact-SHA proof checkpoint was explicitly approved.
The Enter device/app matrix and optional #622 behavior remain intentionally
deferred, not falsely promoted to this phase's evidence.

## Gaps Summary

**No gaps found.** Phase goal achieved; the immutable comparison tag is ready
for future regression checks.

## Verification Metadata

**Verification approach:** Goal-backward, with direct source inspection and
executable evidence.

**Must-haves source:** ROADMAP success criteria merged with unique PLAN
frontmatter truths.

**Automated checks:** 4 behavioral groups passed; Docker baseline gate and
focused JVM gate were green.

**Human checks:** 0 pending (device smoke and proof approval complete).

**Total verification time:** Inline audit completed 2026-09-17.

---
*Verified: 2026-09-17T21:45:10Z*
*Verifier: root agent (dedicated verifier stalled; direct audit used)*
