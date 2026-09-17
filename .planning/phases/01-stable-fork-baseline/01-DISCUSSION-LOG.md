# Phase 1: Stable Fork Baseline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `01-CONTEXT.md` — this log preserves the alternatives considered.

**Date:** 2026-09-17
**Phase:** 1-Stable Fork Baseline
**Areas discussed:** CI contract, Custom override, Enter/newline matrix, Upstream fixes

---

## CI contract

| Option | Description | Selected |
|--------|-------------|----------|
| One Gradle command | Use `./gradlew check :8vim:assembleDebug` directly. | |
| CI orchestrator | Keep reusable workflows as the source of truth. | |
| Baseline script | Add one repository entrypoint for the complete baseline run. | ✓ |

**User's choice:** Baseline script.
**Notes:** The script must run four mandatory gates: unit tests, lint, ktlint/checkstyle, and debug APK. Connected tests remain a separate device smoke activity. Reproducibility uses an explicit toolchain manifest. The full gate is required on every PR.

### CI gates

| Option | Description | Selected |
|--------|-------------|----------|
| Four mandatory gates | Unit tests, lint, ktlint/checkstyle, and `assembleDebug`. | ✓ |
| Plus connected tests | Add `connectedDebugAndroidTest` to CI. | |
| Aggregate check only | Rely on `check` and `assembleDebug` dependencies. | |

**User's choice:** Four mandatory gates.

### Reproducibility

| Option | Description | Selected |
|--------|-------------|----------|
| Wrapper + JDK 17 | Check current wrapper/JDK and document SDK. | |
| Toolchain manifest | Make toolchain versions explicit and checkable. | ✓ |
| Documentation only | Describe versions without runtime checks. | |

**User's choice:** Toolchain manifest.

### CI trigger

| Option | Description | Selected |
|--------|-------------|----------|
| Every PR | Run the full baseline gate regardless of path filters. | ✓ |
| Path-filtered | Preserve current conditional jobs. | |
| PR plus nightly | Filtered PR checks plus scheduled full run. | |

**User's choice:** Every PR.

---

## Custom override

| Option | Description | Selected |
|--------|-------------|----------|
| Immediately current | Successful import replaces current layout. | ✓ |
| Add only | Import changes history; picker changes current. | |
| Ask confirmation | Confirm before replacing current. | |

**User's choice:** Immediately current.
**Notes:** A successful custom import becomes active and is added to available layouts. Re-importing the same URI must not create a duplicate; refresh/reuse the existing entry and activate it.

### Invalid URI

| Option | Description | Selected |
|--------|-------------|----------|
| Preserve previous current | Remove stale URI and keep the last current layout. | ✓ (refined) |
| Fallback to embedded | Always switch to embedded default. | |
| Pick first valid | Choose the first available valid layout. | ✗ |

**User's choice:** Remove every broken URI from history/enabled layouts. If it is inactive, leave current unchanged. If active, restore the last valid active layout; if none exists, use embedded `en`/default. Never choose the first arbitrary valid layout.

### Duplicate identity

| Option | Description | Selected |
|--------|-------------|----------|
| URI identity | Same URI reuses one entry; different URIs remain separate. | ✓ |
| Content-hash deduplication | Merge entries with identical MD5. | |
| Name replacement | Replace entries with matching display names. | |

**User's choice:** URI-based identity. MD5 is only for cache invalidation/change detection. If content changes at the same URI, refresh parsed data/cache/metadata while retaining identity.

### Import errors

| Option | Description | Selected |
|--------|-------------|----------|
| Alert and preserve current | Show parser/validation error; do not save URI or change current. | ✓ |
| Snackbar and preserve current | Show a brief non-modal error. | |
| Add disabled | Save URI for a later retry. | |

**User's choice:** Alert and preserve current.

---

## Enter/newline matrix

| Option | Description | Selected |
|--------|-------------|----------|
| Field types plus smoke | Generic EditorInfo matrix plus messaging device smoke. | |
| Named applications | Require Slack/Discord and other named apps. | |
| JVM matrix only | Keep this phase's required regression in JVM tests. | ✓ |

**User's choice:** JVM matrix only.

### Enter rule

| Option | Description | Selected |
|--------|-------------|----------|
| Multiline newline | Multiline/no action sends Enter; action fields call editor action. | ✓ (refined) |
| ImeOptions as-is | Test only the current branching behavior. | |
| Always editor action | Always call `performEditorAction` for action fields. | |

**User's choice:** Treat behavior as a product contract. `IME_FLAG_NO_ENTER_ACTION` overrides action handling. Multiline without an explicit action emits a real Enter/newline. `SEND/DONE/GO/SEARCH/NEXT/PREVIOUS` may call `performEditorAction` only when the flag is absent.

### Matrix cases

| Option | Description | Selected |
|--------|-------------|----------|
| Four representative cases | Small explicit behavioral matrix. | ✓ |
| All action x flags | Full action/flag combination matrix. | |
| InputType matrix | Add all text/number/phone/datetime variations. | |

**User's choice:** Four representative cases; parameterize explicit actions if trivial, but do not build a full Cartesian product.

### Regression record

| Option | Description | Selected |
|--------|-------------|----------|
| JVM test plus note | Regression spec and short compatibility note. | ✓ |
| JVM spec only | No separate note. | |
| Fixture plus JVM spec | Add a fixture abstraction for the matrix. | |

**User's choice:** JVM test plus note. No separate fixture abstraction for four cases.

---

## Upstream fixes

| Option | Description | Selected |
|--------|-------------|----------|
| 614 + 604 now | Include file-picker MIME and reproducible metadata fixes. | |
| All after smoke | Include #614/#604 and adopt #622 only after smoke. | ✓ |
| Baseline only | Do not transfer upstream patches. | |

**User's choice:** Include #614 and #604; include #622 only if the manual smoke gate passes.

### Gesture smoke

| Option | Description | Selected |
|--------|-------------|----------|
| Manual device gate | Current Android device smoke is required for #622. | ✓ |
| Instrumentation gate | Require an automated emulator suite. | |
| Always defer | Keep #622 out even after smoke. | |

**User's choice:** Manual device gate; no full instrumentation suite is required for baseline adoption.

### Baseline tag

| Option | Description | Selected |
|--------|-------------|----------|
| One immutable tag | Tag the final baseline after all mandatory gates. | ✓ |
| Two tags | Tag upstream snapshot and final baseline separately. | |
| Commit only | Record only the final SHA. | |

**User's choice:** One immutable tag.

### Dependency policy

| Option | Description | Selected |
|--------|-------------|----------|
| Only necessary targeted updates | No historical Dependabot; isolated updates only when required. | ✓ |
| No updates | Freeze every dependency in this phase. | |
| Selective package batch | Apply a small grouped update set. | |

**User's choice:** Do not merge/cherry-pick historical Dependabot PRs. Keep current versions if green; only isolated updates strictly required for build/CI/security compatibility are allowed. Modernization is a separate phase.

---

## the agent's Discretion

- Exact baseline script filename and implementation.
- Exact toolchain manifest format.
- Exact compatibility-note filename and test parameterization.
- Exact immutable tag name and manual #622 smoke checklist.

## Deferred Ideas

- Full Android instrumentation/device compatibility matrix for Enter/newline.
- Dependency modernization using current mutually compatible versions.

