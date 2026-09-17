# 9VIM

## What This Is

9VIM is a brownfield Android input method based on 8VIM, evolving into a daily-use multilingual keyboard. It preserves the gesture-first XPad input model while adding first-class embedded and custom language profiles, fast language switching, offline suggestions, typo correction, and release-grade reliability.

## Core Value

Users can type reliably in several languages with fast 8VIM gestures, without losing text, privacy, or custom-layout support.

## Success Metric

14/14 v1 requirements verified; unit tests, lint, and debug build pass in CI.

## Requirements

### Validated

- ✓ Android IME lifecycle and gesture/XPad input engine exist in the upstream codebase.
- ✓ Embedded and custom YAML layouts can be parsed and loaded.
- ✓ Compose settings, `CustomKeycode`, three suggestion slots, local word-frequency storage, Text Replacement, tests, and CI provide the brownfield foundation.
- ✓ Reproducible tagged fork baseline, custom-layout import/fallback behavior, and Enter/newline regression coverage — Phase 1.

### Active

- [ ] Introduce persistent language profiles and safe migration from the single-layout state.
- [ ] Let users configure embedded and custom languages, order, locale, and default.
- [ ] Support ordinary and gesture-driven next/previous language switching.
- [ ] Remember the selected language per application without storing typed text.
- [ ] Scope suggestion state, history, completion, correction, and prediction by language.
- [ ] Provide deterministic offline dictionary completion and typo correction with undo.
- [ ] Support one-word temporary language mode and optional bilingual suggestions.
- [ ] Harden and package a trustworthy public fork release.

### Out of Scope

- Radial language selector — separate UX work after next/previous switching is stable.
- Switching to another Android IME — distinct from changing language inside 9VIM.
- Cloud text processing or ML services — smart text is offline-first and typed text stays local.
- Automatic language detection as state authority — explicit active language remains the source of truth.
- GPL/source-available dictionaries, code, or assets — excluded without an explicit licensing decision.
- Multi-sector input geometry, layout tutorial, bulk historic Dependabot merges, and extra layout contributions — do not block multilingual v1.

## Context

- Existing application module: `8vim/`, package `inc.flide.vi8`, Kotlin/Compose Android IME.
- Current preferences expose one `prefs.layout.current`; v1 needs enabled profiles, stable IDs, explicit ordering, active language, migration, and safe fallback.
- `Vim8ImeService` observes layout state, while `KeyboardManager`, `CustomKeycode`, and the action map already provide the input command pipeline.
- `SuggestionsManager` and `WordFrequencyRepository` currently use global rather than language-scoped data.
- Custom layouts must remain first-class and may have an optional user-selected locale.
- Known fragile areas include lifecycle-global state, runtime null assertions, `runBlocking` on input paths, selective lint disables, and non-transactional preference/backup behavior.

## Constraints

- **Runtime**: Android IME; minSdk 24, targetSdk 35, compileSdk 36, JVM 17.
- **Brownfield**: Extend the existing IME, Compose settings, layout parser, command pipeline, suggestion bar, and storage patterns; do not rewrite the application.
- **Architecture**: Language is state above `Layout<*>`, never a `LayerLevel`; UI, gestures, and per-app restoration use one `LanguageManager`.
- **Persistence**: Use the existing `Datastore`/`PreferenceModel` identity and explicit migrations; retain at least one valid enabled layout and recover from missing custom layouts.
- **Privacy**: Smart text is local; password fields disable learning, prediction, and correction; per-app memory stores only package name and language profile ID.
- **Compatibility**: Preserve custom layouts, current input text, suggestion gestures, Text Replacement separation, process restart, and backup/restore behavior.
- **Licensing**: Audit every dictionary and asset before inclusion; preserve Apache-2.0 attribution and notices.
- **Quality**: Follow Kotest/MockK conventions and repository lint/ktlint/checkstyle rules; validate domain logic, migrations, fallbacks, privacy, IME lifecycle, and manual device scenarios.

## Key Decisions

No ADR-classified locked decisions were provided. The following implementation-brief guidance is recorded as pending and may be refined during phase planning.

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Model language separately from layout levels | Dictionaries, prediction, custom layouts, and switching need stable language identity | — Pending |
| Route every language change through one manager | Prevents divergent preference writes and UI/gesture behavior | — Pending |
| Keep explicit active language authoritative | Detection is unreliable as the primary state mechanism | — Pending |
| Keep Text Replacement separate from autocorrect | Deterministic user rules and probabilistic correction require different contracts | — Pending |
| Build smart text offline-first | Keyboard text is sensitive and core features must not depend on a network | — Pending |

## Evolution

- After each phase, move verified requirements to Validated and record material decisions.
- Revisit this document when scope, compatibility constraints, or the active milestone changes.

---
*Last updated: 2026-09-18 after Phase 1*
