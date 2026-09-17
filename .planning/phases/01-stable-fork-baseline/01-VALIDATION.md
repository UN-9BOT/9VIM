---
phase: "1"
slug: "stable-fork-baseline"
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-09-17"
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Kotest 5.9.1 with JUnit 5 bridge and MockK 1.14.9 |
| **Config file** | `8vim/build.gradle.kts`, `gradle/libs.versions.toml` |
| **Quick run command** | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.layout.AvailableLayoutsSpec' --tests 'inc.flide.vim8.ime.layout.LayoutSpec' --tests 'inc.flide.vim8.ime.keyboard.text.KeyboardManagerSpec'` |
| **Full suite command** | `./scripts/baseline-check.sh` |
| **Estimated runtime** | ~180 seconds on a warm CI runner |

---

## Sampling Rate

- **After every task commit:** Run the targeted JVM command above.
- **After every plan wave:** Run `./scripts/baseline-check.sh`.
- **Before `$gsd-verify-work`:** Full baseline script must be green.
- **Max feedback latency:** 180 seconds.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01 | 01 | 1 | REQ-stable-fork-baseline | T-01 / — | Toolchain mismatch fails before build | shell/CI | `./scripts/baseline-check.sh` | ❌ W0 | ⬜ pending |
| 01-02 | 01 | 1 | REQ-stable-fork-baseline | T-02 / T-03 | Invalid URI cannot mutate current/history; no arbitrary fallback | unit | targeted JVM command | ❌ W0 | ⬜ pending |
| 01-03 | 01 | 1 | REQ-stable-fork-baseline | — | `IME_FLAG_NO_ENTER_ACTION` takes precedence over editor action | unit | targeted JVM command | ❌ W0 | ⬜ pending |
| 01-04 | 02 | 2 | REQ-stable-fork-baseline | T-04 / — | Only reviewed upstream behavior is adopted and tag points to verified commit | shell/manual | `git show-ref --verify refs/tags/<baseline-tag>` | ❌ W0 | ⬜ pending |

---

## Wave 0 Requirements

- [ ] `scripts/baseline-check.sh` — canonical unit/lint/ktlint/checkstyle/debug APK gate.
- [ ] `config/baseline-toolchain.properties` — Java 17, compile SDK 36, target SDK 35, min SDK 24, Gradle/AGP/build-tools versions.
- [ ] `AvailableLayoutsSpec` — same-URI refresh, duplicate prevention, inactive/active stale URI fallback cases.
- [ ] `LayoutSpec` — URI identity remains distinct when MD5/content is identical.
- [ ] `KeyboardManagerSpec` — four explicit Enter/newline precedence cases.
- [ ] Compatibility note — JVM-only Enter contract and deferred device/app matrix.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Custom layout picker accepts a valid YAML from a current Android provider and activates it | REQ-stable-fork-baseline | SAF provider and persisted URI permission require Android runtime | On a current Android device, open Layout settings, choose a valid YAML through the file picker, confirm it appears in available layouts and becomes active; choose an invalid/zero-layer file and confirm alert plus unchanged current layout. |
| Optional PR #622 gesture semantics | REQ-stable-fork-baseline | User explicitly requires a current-device smoke gate and no full instrumentation suite | If a current device is available, verify default behavior, reset-mode semantics, normal letters/layer movement/long-press, and restart persistence; record pass/fail. If unavailable or failing, record “not adopted in Phase 1”. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies.
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify.
- [ ] Wave 0 covers all missing references above.
- [ ] No watch-mode flags.
- [ ] Feedback latency < 180s.
- [ ] `nyquist_compliant: true` set in frontmatter.

**Approval:** pending
