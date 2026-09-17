# Stable fork baseline provenance

This ledger records what was selected, tested, deferred, or explicitly
excluded for Phase 1. The final CI proof and immutable marker remain reserved
for Plan 06.

## Canonical build contract

The repository-owned entrypoint is:

```bash
./scripts/baseline-check.sh
```

It validates `config/baseline-toolchain.properties`, then runs the required
Gradle gates in one invocation:

```text
:8vim:testDebugUnitTest :8vim:lint :8vim:ktlintCheck :8vim:checkstyle :8vim:assembleDebug
```

| Contract field | Value | Evidence class |
| --- | --- | --- |
| Gradle wrapper | `9.4.0` | repository manifest/preflight |
| Android Gradle Plugin | `9.1.0` | version catalog/preflight |
| Java | `17` | manifest and CI setup |
| Android compile SDK | `36` | manifest and module declaration |
| Android target SDK | `35` | manifest and module declaration |
| Android minimum SDK | `24` | manifest and module declaration |
| Android build tools | `36.0.0` | manifest and CI provisioning |

The PR workflow invokes this command for every pull request targeting
`master`; connected-device tests are separate smoke evidence. The final exact
SHA proof is a blocking Plan 06 checkpoint and is not implied by this ledger.

## Pre-proof environment boundary

The original host preflight observed Java `26` and no Android SDK Platform 36
or Build Tools `36.0.0`. That is a host-tooling limitation, not a project
result: the canonical command must only be evaluated with the manifest-matched
Java 17/Android SDK environment. A pinned Docker environment may supply that
toolchain, but its output still has to be matched to the exact committed SHA
at the Plan 06 proof checkpoint.

## Mandatory upstream provenance

| Upstream | Selected scope | Local evidence/status |
| --- | --- | --- |
| [#614](https://github.com/8VIM/8VIM/pull/614) | Widen the SAF `OpenDocument` MIME filter from `application/octet-stream` to `*/*`; retain URI permission and parser validation. | Implemented in the picker adapter and exercised by the device picker smoke. |
| [#604](https://github.com/8VIM/8VIM/pull/604) | Configure AboutLibraries with `excludeFields = arrayOf("generated")` so generated metadata is reproducible. | Implemented in `8vim/build.gradle.kts`; no dependency versions were changed. |

## Custom-layout evidence and rules

Manual smoke evidence for the picker:

| Field | Result |
| --- | --- |
| APK source commit | `0358d9ae1f2dc7ec415b6014e1ee1b8b7a66fb00` |
| APK | `8vim/build/outputs/apk/debug/8vim-debug.apk` |
| APK SHA-256 | `e700bc2fa678635c2940434baac826b1d1a58b05541322ec17d29b61e6d2fff6` |
| Device | `V2425A`, serial `10CF4J15A9002FS` |
| Android API | `36` |
| Picker outcome | `picker=pass` |

The valid YAML became active and available. Re-importing the same URI produced
no duplicate. Invalid and zero-layer YAML showed validation alerts and left
the current layout unchanged. The device fixtures were copied with these
SHA-256 values:

| Fixture | SHA-256 |
| --- | --- |
| valid `en.yaml` | `ae6ac5861f671c7fd9fda48a50926196a8b4f0f2014fd87d756430b50d9c9051` |
| invalid `invalid_file.yaml` | `52dd52c487a2d877fe0e7349644b5eb4fb98cfff1b4190c768f995fbe817e54b` |
| zero-layer `no_layers.yaml` | `79cd9cce8a5ca8a1099d6a46c6ed7c60126bfc810de62d037e2a9d1bd9104477` |

The product contract implemented by the layout domain is:

- A validated import is immediately active and available.
- A repeated URI reuses one entry, refreshes its content/metadata, and stays
  the active identity; a different URI remains distinct even with equal MD5.
- MD5 is cache invalidation/change detection only, never layout identity.
- A broken URI is removed from history and enabled/available state. If it was
  inactive, the current layout is unchanged.
- If the broken URI was active, restore the persisted last valid identity; if
  that cannot load, use embedded `en`. No arbitrary first valid entry is used.
- A failed import for a new URI alerts and mutates neither current layout nor
  history.

## Enter/newline evidence boundary

The regression contract is JVM-only. Reproduce it with:

```bash
./gradlew :8vim:testDebugUnitTest \
  --tests 'inc.flide.vim8.ime.keyboard.text.KeyboardManagerSpec' \
  --tests 'inc.flide.vim8.ime.editor.EditorInstanceSpec'
```

`IME_FLAG_NO_ENTER_ACTION` has precedence over explicit actions. Multiline
`NONE` routes to a real Enter/newline; `SEND`/`DONE` and the other explicit
actions route to `performEditorAction` only when the flag is absent. The
Android app/device compatibility matrix remains deferred; see
`docs/compatibility/enter-newline.md` for the complete case table.

## Optional and historical scope decisions

### #622 — omitted

The recorded outcome is `#622=omit-unavailable`. This is the required plan
schema value for the omission branch, and means the user intentionally chose
to skip the optional gesture candidate for Phase 1. It does **not** claim that
the Android device was unavailable. No candidate worktree, gesture runtime
change, optional preference, resource string, or adoption evidence exists.

### Explicit exclusions

- [#553](https://github.com/8VIM/8VIM/pull/553) is excluded: its broad custom-
  layout behavior is replaced by the URI transaction/fallback rules above;
  its unrelated version rollback is not taken.
- Historical Dependabot PRs are excluded. Current dependency versions remain
  in place while the declared build is compatible.
- Dependency modernization, a full instrumentation matrix, and unrelated
  upstream changes are outside this baseline.

## Final proof fields — Plan 06

These fields are intentionally left for the final proof step; no final green
result or tag creation is asserted here:

- Final CI run URL: `TBD — Plan 06`
- Final CI commit SHA: `TBD — Plan 06`
- Immutable tag name: `fork-baseline-v0.18.0-rc.1`
- Immutable tag target SHA: `TBD — Plan 06`
- Tag verification evidence: `TBD — Plan 06`
