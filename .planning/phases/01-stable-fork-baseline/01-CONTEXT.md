# Phase 1: Stable Fork Baseline - Context

**Gathered:** 2026-09-17
**Status:** Ready for planning

<domain>
## Phase Boundary

Establish a reproducible, regression-visible fork baseline before multilingual
feature work: a single documented CI entrypoint, green unit/lint/style/build
checks, explicit custom-layout override behavior, a small Enter/newline
regression contract, selected upstream fixes, and one immutable comparison tag.

This phase does not introduce language profiles, language switching, smart-text
features, dependency modernization, or a full instrumentation suite.

</domain>

<decisions>
## Implementation Decisions

### CI contract

- **D-01:** The canonical baseline entrypoint is a repository baseline script,
  not a hidden CI-only sequence.
- **D-02:** The script must fail unless unit tests, lint, ktlint/checkstyle, and
  the debug APK build all pass. The required product checks are
  `testDebugUnitTest`, configured lint/style checks, and `assembleDebug`.
- **D-03:** Connected Android tests are not a required CI gate for this phase;
  device checks remain a separate smoke-test activity.
- **D-04:** Reproducibility is documented and checked through an explicit
  toolchain manifest covering the repository Gradle wrapper, Java 17, and the
  Android SDK/toolchain used by the build.
- **D-05:** The full baseline gate runs on every pull request, independent of
  path filters.

### Custom-layout override

- **D-06:** After successful validation, an imported custom layout immediately
  becomes the active/current layout and is added to the available-layout list.
- **D-07:** Custom layout identity is URI-based. Re-selecting the same URI
  reuses one history entry, refreshes parsed data/cache/metadata when content
  changed, and makes that entry active. A different URI remains a separate
  entry even when its MD5 matches; MD5 is only for cache invalidation/change
  detection, not identity.
- **D-08:** A broken or deleted URI is always removed from history/enabled
  layouts. If it is inactive, current layout is unchanged. If it is active,
  restore the last valid active layout; if none exists, use the embedded
  default layout (for example `en`). Never choose the first arbitrary valid
  layout.
- **D-09:** Import/parser/validation errors show an alert, leave current
  layout unchanged, and do not add the URI to history.

### Enter/newline regression

- **D-10:** The regression contract is JVM-only for this phase. It is not a
  substitute for a future device/app compatibility suite.
- **D-11:** `IME_FLAG_NO_ENTER_ACTION` has priority over editor actions.
  Multiline editors without an explicit action produce a real Enter/newline
  event. Explicit `SEND`, `DONE`, `GO`, `SEARCH`, `NEXT`, and `PREVIOUS`
  actions may call `performEditorAction` only when the flag is absent.
- **D-12:** Required coverage is four explicit representative behavioral
  cases: multiline `NONE`, multiline with `NO_ENTER_ACTION`, single-line
  `SEND`/`DONE`, and an explicit action with `NO_ENTER_ACTION`. Parameterizing
  the explicit-action case is fine; a full `InputType × action × flag`
  Cartesian product is out of scope.
- **D-13:** Record the contract in the existing JVM regression spec plus a
  short compatibility note. Do not create a separate fixture abstraction for
  these four cases.

### Upstream fixes and baseline marker

- **D-14:** Include the minimal file-picker MIME fix from upstream PR #614 and
  the reproducible-metadata fix from PR #604 in this baseline.
- **D-15:** Consider the gesture-semantics change from PR #622 only after a
  manual smoke-test on a current Android device. No full instrumentation suite
  is required for adoption; if smoke fails or is unavailable, leave #622 out.
- **D-16:** Do not take PR #553 as-is. Its custom-layout behavior is replaced
  by the explicit URI-based override and fallback rules above, without an
  unrelated version rollback.
- **D-17:** Create one immutable git tag on the final baseline after all
  mandatory checks and accepted fixes pass.
- **D-18:** Never merge or cherry-pick historical Dependabot PRs. Keep current
  dependency versions when CI/build is green; only isolated updates strictly
  required for build, CI, or security compatibility are allowed in this phase.
  Dependency modernization is a separate follow-up phase.

### the agent's Discretion

- Exact script filename, shell/Kotlin implementation, and manifest format,
  provided the canonical command and required gates remain observable.
- Exact compatibility-note filename and test parameterization, provided the
  four behavioral cases and precedence rule are explicit.
- Exact tag name, provided it is a single immutable tag on the final baseline
  and its commit is recorded.
- Exact manual smoke-test checklist for #622, provided it tests the changed
  gesture semantics on a current Android device and records pass/fail evidence.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product brief and phase contract

- `docs/prd/8vim_fork_implementation_brief_ru.md` §Этап 0 — source rationale,
  upstream PR boundaries, and Enter/newline baseline requirements.
- `.planning/PROJECT.md` — project scope, privacy/compatibility constraints,
  and explicit out-of-scope items.
- `.planning/REQUIREMENTS.md` — `REQ-stable-fork-baseline` acceptance contract.
- `.planning/ROADMAP.md` — Phase 1 goal and success criteria.
- `.planning/STATE.md` — current phase position and known baseline concerns.

### Existing architecture and quality evidence

- `.planning/codebase/STACK.md` — Android/Gradle/JVM toolchain and test
  dependencies.
- `.planning/codebase/ARCHITECTURE.md` — IME/settings/layout integration
  boundaries.
- `.planning/codebase/TESTING.md` — Kotest/MockK conventions and commands.
- `.planning/codebase/CONCERNS.md` — disabled checks, lifecycle risks, and
  existing backup/input concerns relevant to regression hardening.

### CI and toolchain

- `.github/workflows/pr-test.yml` — current conditional PR orchestration to
  replace or wrap with the all-PR baseline gate.
- `.github/workflows/codecheck.yaml` — current `check`/coverage workflow.
- `.github/workflows/build.yaml` — current debug/release build workflow.
- `8vim/build.gradle.kts` — configured test, lint, ktlint, checkstyle, Jacoco,
  SDK, Java, and build-type behavior.
- `build.gradle.kts` — root build/plugin configuration.
- `gradle/libs.versions.toml` — pinned dependency/tool versions.
- `gradle/wrapper/gradle-wrapper.properties` — Gradle wrapper version.
- `config/checkstyle/checkstyle.xml` — style gate configuration.

### Custom layout and Enter behavior

- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt` — file
  picker validation, current-layout assignment, and history update.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt` —
  available-layout history, stale-layout removal, and fallback behavior.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt` — embedded/custom
  identity, URI loading, MD5 cache, and safe loading.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/editor/EditorInstance.kt` —
  `InputConnection` editor-action and Enter event boundaries.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt` —
  `handleEnter()` routing and input command pipeline.
- `8vim/src/test/kotlin/inc/flide/vim8/ime/editor/EditorInstanceSpec.kt` —
  existing editor-action/Enter test patterns.
- `8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt`
  — existing key-routing test patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `LayoutScreen.fileSelector()` already uses `OpenDocument`, MIME filtering,
  parser validation, persisted URI permission, and custom-layout history.
- `AvailableLayouts` already indexes embedded and custom layouts, reloads URI
  history, validates loaded data, and removes stale entries.
- `Layout.loadKeyboardData()` and `LayoutSerDe` already provide MD5 caching and
  stable `e`/`c` serialization prefixes.
- `EditorInstance` and `KeyboardManager.handleEnter()` are the narrow existing
  boundaries for the regression contract.
- Existing Kotest/MockK specs provide the test harness and mocking patterns.

### Established Patterns

- Gradle wrapper and version catalog are the source of build/tool versions;
  CI uses Java 17 and reusable workflows.
- Domain logic is tested with Kotest `FunSpec`, `context`, `withData`, and
  MockK only at Android/platform boundaries.
- Layout failures use `Either<LayoutError, ...>` and safe fallback helpers;
  preference identity must continue through `AppPrefs`/`PreferenceModel`.

### Integration Points

- The baseline script connects local execution to `.github/workflows/pr-test.yml`
  and reusable `codecheck.yaml`/`build.yaml` jobs.
- Custom override changes connect `LayoutScreen`, `AvailableLayouts`,
  `Layout.kt`, `AppPrefs`, and the IME's observed current layout.
- Enter regression changes connect `KeyboardManager`, `EditorInstance`,
  `ImeOptions`, and their JVM specs.

</code_context>

<specifics>
## Specific Ideas

- Product wording for stale custom layouts: remove an inactive broken URI
  without touching current; for an active broken URI, restore the last valid
  active layout or embedded `en`; never select a random first valid layout.
- Product wording for identity: `Custom layout identity is URI-based`; MD5 is
  cache/change detection only.
- The Enter contract is precedence-based (`NO_ENTER_ACTION` first), not merely
  a test of the current implementation.
- Manual smoke evidence is sufficient for optional #622 adoption in this
  baseline; full instrumentation can follow later.

</specifics>

<deferred>
## Deferred Ideas

- Full Android instrumentation/device compatibility matrix for Enter/newline.
- Dependency modernization using current mutually compatible versions.
- Radial language selector and other multilingual features remain in later
  roadmap phases.

</deferred>

---

*Phase: 1-Stable Fork Baseline*
*Context gathered: 2026-09-17*
