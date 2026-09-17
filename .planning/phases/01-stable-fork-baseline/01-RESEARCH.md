# Phase 1: Stable Fork Baseline - Research

**Researched:** 2026-09-17
**Domain:** Android IME baseline, CI/toolchain reproducibility, custom-layout lifecycle, Enter routing
**Confidence:** HIGH for codebase behavior; MEDIUM for external platform guidance

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### CI contract

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

#### Custom-layout override

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

#### Enter/newline regression

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

#### Upstream fixes and baseline marker

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

### Deferred Ideas (OUT OF SCOPE)

- Full Android instrumentation/device compatibility matrix for Enter/newline.
- Dependency modernization using current mutually compatible versions.
- Radial language selector and other multilingual features remain in later
  roadmap phases.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REQ-stable-fork-baseline | Tagged reproducible fork baseline with CI unit/lint/style/APK gates, selected upstream fixes, custom-layout override semantics, and Enter regression | CI topology, toolchain manifest, upstream diff audit, URI lifecycle design, JVM test map, tag gate |
</phase_requirements>

## Summary

The phase should be planned as four independently verifiable slices: (1) one
repository-owned baseline command plus an all-PR workflow and toolchain manifest,
(2) transactional URI-based custom-layout import/fallback, (3) the small JVM
Enter precedence contract, and (4) upstream provenance plus final tag. No new
runtime dependency is needed. The existing Gradle/Kotest/MockK/Arrow stack is
sufficient. [VERIFIED: `8vim/build.gradle.kts:8-17,150-224,266-276`]

The current code is close but does not satisfy the product semantics. Importing
a new URI validates then activates it, while re-importing an existing URI takes
a separate path that can fail silently. Removal of any broken custom URI resets
the current layout, even when that URI was inactive. The cache stores a
URI-derived display name inside MD5-keyed data, so two different URIs with the
same bytes can leak metadata across identities. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt:84-117`;
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:36-79`;
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt:79-97`]

The current environment cannot prove a green baseline: the wrapper runs with
Java 26, Java 17 is not installed, and Gradle fails before task execution because
no Android SDK location/platform is configured. This is an environment gap, not
evidence that the repository fails under the required manifest. [VERIFIED:
local probes on 2026-09-17; `./gradlew :8vim:check :8vim:assembleDebug --dry-run`
failed with `SDK location not found`]

**Primary recommendation:** lead with the production-quality tracer
`scripts/baseline-check.sh` invoked unchanged by CI; then harden layout import at
the existing `AvailableLayouts` boundary, add focused JVM regressions, apply the
two mandatory one-purpose upstream fixes, and create the tag only after the same
baseline command passes from a clean checkout.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Canonical baseline gate | Repository build tooling | GitHub Actions | The script is the source of truth; CI only provisions the manifest and invokes it |
| Custom layout picker | Compose settings / Android SAF | Layout domain + preferences | UI obtains permission and reports errors; domain owns validation, identity, history, activation, fallback |
| Custom layout cache | Layout domain | Android ContentResolver | MD5 invalidates parsed bytes; URI remains identity and metadata source |
| Enter/newline behavior | IME input domain | Android InputConnection | `KeyboardManager` chooses action vs Enter; `EditorInstance` performs the platform call |
| Baseline marker | Git/release metadata | Documentation | Tag is created only after mandatory verification and records the accepted commit |

## Standard Stack

### Core

| Component | Pinned value | Purpose | Evidence |
|-----------|--------------|---------|----------|
| Gradle wrapper | `9.4.0` | Canonical build runner | Quote: `distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip` [VERIFIED: `gradle/wrapper/gradle-wrapper.properties:1-5`] |
| Android Gradle Plugin | `9.1.0` | Android build/lint/APK tasks | Quote: `android-gradle-plugin = "9.1.0"` [VERIFIED: `gradle/libs.versions.toml:4-4`] |
| Java | `17` | Gradle/Android compile target and CI runtime | Quote: `sourceCompatibility = JavaVersion.VERSION_17`, `targetCompatibility = JavaVersion.VERSION_17` [VERIFIED: `8vim/build.gradle.kts:56-59`] |
| Android SDK | `compileSdk = 36`, `targetSdk = 35`, `minSdk = 24` | Compile and runtime compatibility envelope | Verbatim values [VERIFIED: `8vim/build.gradle.kts:53-76`] |
| SDK Build Tools | `36.0.0` | AGP 9.1 default build toolchain | [CITED: https://developer.android.com/build/releases/agp-9-1-0-release-notes] |
| Kotest / MockK | `5.9.1` / `1.14.9` | Existing JVM regression style | Verbatim values [VERIFIED: `gradle/libs.versions.toml:35-38`] |

### Supporting

| Component | Purpose | Use |
|-----------|---------|-----|
| Arrow `Either`/`Option` | Typed layout failures and transactional branching | Preserve existing error boundary instead of exceptions in UI |
| Android `OpenDocument` | Persistable URI selection | Launch with wildcard MIME, validate actual content after selection |
| GitHub Actions setup-java/setup-gradle | Provision Java and Gradle cache | Keep workflow orchestration thin and call the repository script |

No external package installation is required. Dependency modernization and
historical Dependabot merges are explicitly out of scope.

## Recommended Architecture Patterns

### 1. Repository-owned baseline gate

Recommended files:

```text
scripts/baseline-check.sh
config/baseline-toolchain.properties
docs/compatibility/enter-newline.md
.github/workflows/pr-test.yml
```

The manifest should declare the wrapper/Java/compile SDK/build-tools values in
one machine-readable place. The script should fail early when Java is not major
17 or `platforms;android-36` is unavailable, then run explicit tasks rather than
hide the contract behind `check`:

```text
./gradlew --no-daemon \
  :8vim:testDebugUnitTest \
  :8vim:lint \
  :8vim:ktlintCheck \
  :8vim:checkstyle \
  :8vim:assembleDebug
```

These task names exist in the current project. Quote: `testDebugUnitTest - Run
unit tests for the debug build`, `lint - Runs lint on the default variant`,
`ktlintCheck`, `checkstyle`, and `assembleDebug`. [VERIFIED: local
`:8vim:tasks --all` probe on 2026-09-17] `check` already depends explicitly on
`checkstyle`, and all `Test` tasks except `testDebugUnitTest` are disabled.
[VERIFIED: `8vim/build.gradle.kts:22-37,216-224`]

The PR workflow currently performs path detection and can skip both tests and
build for changes outside selected filters. [VERIFIED:
`.github/workflows/pr-test.yml:18-117`] Replace the mandatory path-conditioned
topology with a job that runs on every PR targeting `master`, provisions the
manifest toolchain, and executes only `./scripts/baseline-check.sh`. Extra YAML
layout validation may remain a separate conditional job, but it must not replace
or gate whether the baseline job starts. Omitting `paths`/`paths-ignore` is the
official all-path behavior. [CITED:
https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax]

### 2. Transactional custom-layout import

Move import state transitions behind a production-facing method on
`AvailableLayouts` (or a small layout-domain coordinator), leaving
`LayoutScreen` responsible only for SAF launch, persistable permission, and
showing the returned error. This is a production abstraction: it creates one
place for validate → update registry/history → remember fallback → activate.
Do not add hidden function attributes or test-only DI.

Recommended transaction:

1. Construct identity from `Uri.toString()`; never use MD5 as the map/history key.
2. Parse and validate before changing current/history. `totalLayers == 0` remains invalid.
3. On success, replace/update the map entry for that URI, de-duplicate history,
   move it into the intended deterministic order, and activate it.
4. When activation changes identity, persist the old known-valid current layout
   as the single previous-valid fallback. Do not overwrite it when refreshing
   the already-active URI.
5. Return a typed success/error result; UI shows the existing alert on error.

`CustomLayout` is already a data class whose equality includes `path: Uri`, and
the serialized identities are verbatim `e<path>` / `c<path>`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt:103-136,158-166`]
The preference keys/default are verbatim `prefs_layout_current`, embedded `en`,
and `prefs_layout_custom_history`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:52-72`]

The existing `LinkedHashSet` prevents exact duplicate strings, but the existing
map cleanup compares a URI-string set against `CustomLayout.toString()` rather
than `layout.path.toString()`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:83-107`]
Correct that comparison and cover reload stability.

### 3. Cache raw parsed data, derive URI metadata after lookup

Keep MD5 as the content cache key, but do not cache URI-derived fallback names.
Load cached/raw `KeyboardData`, cache the raw parser result, and only then apply
`defaultName(context)` when its declared name is empty. That gives same-URI
content refresh via a new MD5 while preserving different URI identities and
display metadata even for identical bytes. The current implementation modifies
the name before `cache.add(md5, it)`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt:79-97`]

Close every `ContentResolver` stream with `use` while calculating MD5 or parsing.
The current MD5 path opens a stream without an explicit close. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt:158-166`]

### 4. Active-aware stale URI pruning

Centralize stale removal and branch on identity:

- broken URI != current: remove it from map/history; do not change current;
- broken URI == current: attempt the persisted previous-valid layout;
- previous-valid fails or is absent: select the declared embedded default `en`;
- never use map order, `firstOrNone`, or index zero as fallback selection logic.

Current `removeFromHistory()` always calls `prefs.layout.current.reset()` and
`selectLayout()` resets it again on load failure. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:36-49,66-80`]
`findIndex()` also maps unknown current state to `defaultIndex`, so tests must
assert identity, not just index `0`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:110-115`]

Boundary rule for D-08/D-09: a failed never-before-seen picker candidate is a
rejected import and leaves state unchanged. A previously accepted URI that now
fails during same-URI refresh, reload, or selection is a known stale layout: show
the alert, remove it from history/available layouts, and apply the active-aware
fallback above. This gives the user's explicit “broken URI must not be retried”
rule precedence for persisted identities while preserving transactional behavior
for new candidates.

### 5. Enter precedence at the existing routing seam

Keep runtime code at `KeyboardManager.handleEnter()` and platform calls in
`EditorInstance`. The current runtime logic already gives
`flagNoEnterAction` priority for explicit actions, and otherwise sends Enter.
[VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt:203-220`]
The exact action enum values are quote: `UNSPECIFIED`, `DONE`, `GO`, `NEXT`,
`NONE`, `PREVIOUS`, `SEARCH`, `SEND`. [VERIFIED:
`8vim/src/main/kotlin/inc/flide/vim8/ime/editor/ImeOptions.kt:16-24`]
Android documents that multiline `TextView` normally sets
`IME_FLAG_NO_ENTER_ACTION`, and the flag means the action must not replace the
Enter key. [CITED:
https://developer.android.com/reference/android/view/inputmethod/EditorInfo]

Add the four locked cases directly to `KeyboardManagerSpec` through public
`onInputKeyUp`; parameterize the explicit action values but do not introduce a
new fixture abstraction. `EditorInstanceSpec` already proves that `performEnter`
sends `KEYCODE_ENTER` down/up and `performEnterAction` delegates to the input
connection. [VERIFIED:
`8vim/src/test/kotlin/inc/flide/vim8/ime/editor/EditorInstanceSpec.kt:125-160`]

## Upstream Provenance and Integration Boundaries

| Upstream | Exact scope | Recommendation | Verification |
|----------|-------------|----------------|--------------|
| PR #614 | One-line `application/octet-stream` → `*/*` change in `LayoutScreen` | Reproduce the one-line behavior; retain parser validation and persisted permission | Picker shows YAML/YML regardless of provider MIME; valid import activates; invalid import alerts and leaves state unchanged |
| PR #604 | `aboutLibraries { excludeFields = arrayOf("generated") }` | Port the behavior into current `8vim/build.gradle.kts`; do not cherry-pick old context | Generate AboutLibraries metadata twice and assert no `metadata.generated` difference |
| PR #622 | New preference/UI/controller reset mode plus three controller tests | Optional isolated slice after a current-device checkpoint; default must preserve existing complex gestures; omit if device unavailable/fails | Unit tests plus manual gesture checklist and recorded pass/fail |
| PR #553 | Removes conflict validation and rolls version from `0.18.0` to `0.17.6` | Do not cherry-pick; implement only current locked URI override/fallback semantics | Layout import tests and unchanged `8vim/version.properties` |

Primary diffs: [PR #614](https://github.com/8VIM/8VIM/pull/614),
[PR #604](https://github.com/8VIM/8VIM/pull/604),
[PR #622](https://github.com/8VIM/8VIM/pull/622), and
[PR #553](https://github.com/8VIM/8VIM/pull/553). The local tree does not contain
the referenced PR commits as ancestors, so provenance should be recorded in
commit messages or the baseline note rather than implied by history.

For #622, use a checkpoint before adoption. If a current device is available,
the smoke checklist should cover: default-on behavior unchanged; option-off
resets an unresolved center crossing; a valid gesture after reset commits once;
valid center-hit gestures still work; setting survives activity/IME restart;
basic letters, layer movement, and long-press remain usable. If no device is
available, record “not adopted in Phase 1” and continue—the locked phase contract
allows omission.

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---------|-------------|-------------|-----|
| Build orchestration | A second Gradle wrapper or CI-only task graph | Existing `gradlew` plus explicit module tasks | Keeps local and CI contracts identical |
| URI access | File-path conversion or extension-based filtering | Android SAF `OpenDocument`, persisted permission, parser validation | `content://` providers need resolver access; MIME is not authoritative |
| Layout validation | UI-side YAML checks | Existing `LayoutLoader` returning `Either<LayoutError, KeyboardData>` | Schema/parser behavior already exists and is testable |
| Enter behavior | App-name allowlists for Slack/Discord | `EditorInfo` action/flag precedence | Contract is editor metadata, not application identity |
| Fallback | First map entry/index zero | Persisted previous-valid identity then declared default | Map order is not product intent |
| Upstream integration | Bulk cherry-pick branch/Dependabot history | Reproduce minimal reviewed changes with provenance | Avoids unrelated versions/dependency drift |

## Common Pitfalls

### Silent same-URI failure

`LayoutScreen` calls `updateKeyboardData()` for a URI already in history but
ignores its Boolean result, so parser failure has no alert. [VERIFIED:
`LayoutScreen.kt:87-91`; `AvailableLayouts.kt:56-64`] Make the result typed and
route all import failures through the same alert path.

### Random or over-eager fallback

The current reset semantics can change current layout for an inactive broken
URI. Avoid fixing this by selecting the first remaining map entry; that violates
D-08. Assert the concrete previous/default identity in every regression.

### MD5 mistaken for identity

MD5 is currently the cache key. It must not become history/map identity, and
metadata derived from URI must not live inside the shared content-cache payload.

### CI still path-conditioned indirectly

Removing `on.pull_request.paths` is insufficient because the current workflow
uses job-level `if` conditions based on `dorny/paths-filter`. [VERIFIED:
`.github/workflows/pr-test.yml:18-105`] The baseline job itself must be unconditional
for PRs targeting the branch.

### Claiming device compatibility from JVM tests

The source brief asked to reproduce issue #568 in messaging apps, while locked
D-10 deliberately limits Phase 1 to JVM coverage. The compatibility note must
say “contract fixed in JVM regression; device/app matrix deferred,” not claim
Slack/Discord were verified. Custom picker and optional #622 still retain their
separate device smoke gates.

### Tagging before optional decisions settle

The immutable tag is last: mandatory baseline command green, mandatory #614/#604
accepted, custom-layout smoke evidence recorded, Enter JVM contract green, and
#622 either accepted with evidence or explicitly omitted.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Kotest `5.9.1`, JUnit 5 bridge, MockK `1.14.9` |
| Config | `8vim/build.gradle.kts` and `gradle/libs.versions.toml` |
| Targeted JVM run | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.layout.AvailableLayoutsSpec' --tests 'inc.flide.vim8.ime.layout.LayoutSpec' --tests 'inc.flide.vim8.ime.keyboard.text.KeyboardManagerSpec'` |
| Full phase gate | `./scripts/baseline-check.sh` |

### Requirement → Evidence Map

| Behavior | Test/evidence | Command or artifact | Exists? |
|----------|---------------|---------------------|---------|
| Same URI refreshes without duplicate and activates | Unit tests in `AvailableLayoutsSpec` | targeted JVM run | ❌ Wave 0 cases |
| Different URI + same MD5 stays separate with correct metadata | Unit tests in `LayoutSpec` and `AvailableLayoutsSpec` | targeted JVM run | ❌ Wave 0 cases |
| Failed new import alerts/no mutation | Domain result unit test + thin UI/manual smoke | targeted JVM + device checklist | ❌ Wave 0 cases |
| Inactive stale URI removed/current unchanged | `AvailableLayoutsSpec` | targeted JVM run | ❌ Wave 0 case |
| Active stale URI → previous-valid → embedded default | `AvailableLayoutsSpec` | targeted JVM run | ❌ Wave 0 cases |
| Enter precedence four representative cases | `KeyboardManagerSpec` | targeted JVM run | ❌ Wave 0 cases |
| Enter platform delegation | Existing `EditorInstanceSpec` | targeted JVM run | ✅ |
| Required CI tasks and debug APK | Baseline script | `./scripts/baseline-check.sh` | ❌ Wave 0 script |
| PR #604 metadata determinism | Build-output assertion | `generateLibraryDefinitionsDebug` twice + compare | ❌ Wave 0 check |
| Custom picker on current Android | Manual smoke evidence | checklist with device/API/result | manual required |
| Optional #622 | Unit + manual gate | controller spec + device checklist | conditional |
| Immutable baseline marker | Git verification | `git show-ref --verify refs/tags/<tag>` and recorded commit | final gate |

### Sampling Rate

- **Per implementation task:** targeted relevant spec(s), under the module JVM task.
- **Per wave:** `./scripts/baseline-check.sh`.
- **Phase gate:** clean checkout, manifest toolchain, baseline script green,
  required device evidence, then tag points to verified commit.

### Wave 0 Gaps

- [ ] Repository baseline script and explicit toolchain manifest.
- [ ] Layout import API/result that can be tested without Compose internals.
- [ ] Previous-valid layout preference or equivalent persisted identity.
- [ ] New URI/MD5/refresh/stale/fallback cases in `AvailableLayoutsSpec`/`LayoutSpec`.
- [ ] Four Enter precedence cases in existing `KeyboardManagerSpec`.

## Environment Availability

| Dependency | Required by | Available | Observed version/state | Fallback |
|------------|-------------|-----------|------------------------|----------|
| Gradle wrapper | All automated checks | ✓ | `9.4.0` downloaded and runnable | none needed |
| Java 17 runtime | Manifest/CI parity | ✗ | Host default is OpenJDK `26.0.1`; installed JVM dirs show 21 and 26 | CI `setup-java` 17; local install/configure 17 before proof |
| Android SDK Platform 36 | compile/lint/APK | ✗ | `/opt/android-sdk` has build-tools 37 and platform-tools, no platforms directory; no SDK env/local.properties | provision `platforms;android-36` and `build-tools;36.0.0` |
| ADB | Device smoke | ✓ | ADB `1.0.41`, platform-tools `35.0.2` | device still required |
| Current Android device | picker/#622 smoke | not detected | no device evidence obtained | #622 omitted if unavailable; picker smoke remains a phase acceptance checkpoint |

**Blocking for local automated proof:** Java 17 and Android SDK Platform 36 are
missing/unconfigured. The research did not install or mutate the host toolchain.

## Security Domain

### Applicable ASVS Categories

| Category | Applies | Control |
|----------|---------|---------|
| V2 Authentication | no | No authentication boundary in local IME phase |
| V3 Session Management | no | No server session |
| V4 Access Control | yes, Android URI grants | Persist only read permission for user-selected URI; do not broaden storage access |
| V5 Input Validation | yes | Treat selected URI/YAML as untrusted; schema/parser validation before state mutation |
| V6 Cryptography | no new crypto | MD5 remains cache invalidation only, never integrity/security identity |

Threats to test: provider returns null/throws, URI permission later revoked,
malformed or zero-layer YAML, repeated same URI, two URIs with identical bytes,
and content changing between MD5 and parse reads. The last race is best reduced
by reading one byte payload once and deriving both digest and parse input from
that snapshot; if that refactor is too broad, at minimum close streams and make
failure transactional. No network permission or external service is introduced.

## Project Constraints

No `./AGENTS.md` or `./.codex/AGENTS.md` file exists in the repository. Session
instructions still require production changes to follow test-first development:
write the failing test, confirm failure, then implement the minimum runtime
change; mock module/public boundaries rather than add hidden test seams. Research
creates no production code. User-facing reports remain Russian; technical
identifiers are preserved.

## Package Legitimacy Audit

Not applicable. This phase should add no external package. Existing Gradle
plugins, actions, AndroidX, Kotest, MockK, Arrow, Jackson, and schema validator
remain pinned; historical dependency PRs are explicitly excluded.

## Assumptions Log

| # | Claim | Risk if wrong | Disposition |
|---|-------|---------------|-------------|
| A1 | A persisted single previous-valid layout identity is the smallest durable model needed for D-08 | A later Phase 2 migration must absorb/remove this preference | Planner should keep it narrow and migration-friendly |
| A2 | Failed new URI import is non-mutating; failed refresh of an already-persisted URI prunes it and applies active-aware fallback | D-08/D-09 overlap could be interpreted differently | Encode this precedence in named tests and compatibility note |
| A3 | `build-tools;36.0.0` is the intended AGP 9.1 default | Runner image/tooling could select a newer compatible 36.x | Pin manifest and CI provisioning explicitly |

## Planning Resolutions

1. **RESOLVED — Preference shape for previous-valid layout**
   - Decision: use one narrowly scoped serialized `Layout<*>` identity beside
     `current`, default embedded `en`, updated only after successful identity
     changes and not during same-URI refresh.
   - Phase 2 must absorb/remove it when `LanguageProfile` becomes authoritative;
     Phase 1 does not create a second public layout authority.

2. **RESOLVED — Optional #622 scheduling**
   - Decision: build the isolated #622 candidate only in a disposable
     worktree/branch, run the current-device smoke before baseline adoption,
     then either port the isolated change after `adopt` evidence or discard the
     candidate and record `omit-unavailable`/`omit-failed`.

3. **RESOLVED — Baseline tag spelling**
   - Decision: create the single annotated tag
     `fork-baseline-v0.18.0-rc.1` only after exact-SHA local or GitHub-CI proof,
     after verifying the name is absent both locally and on `origin`.

## Sources

### Primary codebase/upstream

- Current repository files cited inline with line ranges.
- [8VIM PR #614](https://github.com/8VIM/8VIM/pull/614) — one-line file-picker MIME diff.
- [8VIM PR #604](https://github.com/8VIM/8VIM/pull/604) — AboutLibraries generated timestamp exclusion.
- [8VIM PR #622](https://github.com/8VIM/8VIM/pull/622) — optional gesture semantics and tests.
- [8VIM PR #553](https://github.com/8VIM/8VIM/pull/553) — rejected broad patch/version rollback.
- [8VIM issue #568](https://github.com/8VIM/8VIM/issues/568) — Enter/newline motivation.

### Official platform documentation

- [Android EditorInfo](https://developer.android.com/reference/android/view/inputmethod/EditorInfo) — action and `IME_FLAG_NO_ENTER_ACTION` semantics.
- [Android OpenDocument](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument) — document URI contract.
- [AGP 9.1 release notes](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — JDK/build-tools compatibility.
- [Android 16 SDK setup](https://developer.android.com/about/versions/16/setup-sdk) — API 36 platform/build-tools setup.
- [GitHub Actions workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax) — PR/path filter behavior.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — pinned values read from source-of-truth files.
- Architecture: HIGH — relevant runtime/test paths opened and traced.
- Upstream integration: HIGH — exact PR patch files inspected; adoption status
  still depends on local implementation/device evidence.
- Android/GitHub semantics: MEDIUM — official docs fetched through web search;
  Context7 was unavailable.
- Environment: HIGH — direct local probes; negative build result preserved.

**Research date:** 2026-09-17
**Valid until:** 2026-10-17 for repository findings; re-check hosted runner images
and upstream PR status before execution.
