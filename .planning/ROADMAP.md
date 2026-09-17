# Roadmap: 9VIM

## Overview

The roadmap stabilizes the existing Android IME before introducing persistent multilingual state, then delivers reliable switching and context memory, rebuilds smart text around language-scoped offline data, adds correction/prediction, and finishes with 8VIM-specific multilingual UX plus public-release hardening. Phases are vertical delivery boundaries and retain the implementation brief's dependency order.

## Phases

**Phase Numbering:**

- Integer phases are planned milestone work.
- Decimal phases are urgent insertions between planned phases.

- [x] **Phase 1: Stable Fork Baseline** - Establish a reproducible, tested reference build before feature work. (completed 2026-09-18)
- [ ] **Phase 2: Multilingual Profiles and Settings** - Let users persistently configure embedded and custom language profiles.
- [ ] **Phase 3: Reliable Language Switching and Context** - Switch languages from controls or gestures and restore them per app.
- [ ] **Phase 4: Language-Scoped Suggestions** - Preserve the suggestion workflow while isolating dictionaries, history, and completion by language.
- [ ] **Phase 5: Correction and Context Prediction** - Add safe typo correction, undo, and bounded offline next-word prediction.
- [ ] **Phase 6: Advanced Multilingual UX and Public Release** - Deliver temporary/bilingual workflows and a release users can trust daily.

## Phase Details

### Phase 1: Stable Fork Baseline

**Goal**: Developers and users have a reproducible, regression-visible baseline on which multilingual work can safely build.
**Depends on**: Nothing (first phase)
**Requirements**: REQ-stable-fork-baseline
**Success Criteria** (what must be TRUE):

  1. A clean checkout produces the debug APK through the documented CI command, with unit tests and configured lint/style checks passing.
  2. A current Android device can select a custom layout through the file picker and use explicit override behavior.
  3. The Enter/newline behavior is reproducible and its expected result is fixed for representative messaging fields.
  4. A baseline tag/commit exists for regression comparison, without bulk-merging historic dependency PRs.

**Plans**: TBD
**Wave 1**

- [x] 01-01-PLAN.md
- [x] 01-02-PLAN.md
- [x] 01-03-PLAN.md

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-04-PLAN.md

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 01-05-PLAN.md

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 01-06-PLAN.md

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 01-07-PLAN.md

### Phase 2: Multilingual Profiles and Settings

**Goal**: Users can configure, persist, and safely migrate an ordered set of embedded and custom language profiles.
**Depends on**: Phase 1
**Requirements**: REQ-language-profiles, REQ-language-settings
**Success Criteria** (what must be TRUE):

  1. A user can enable at least two embedded layouts and one custom layout, order them as EN → RU → LV, and choose the primary language.
  2. Enabled profiles, order, optional custom locale, and active language survive restart and supported backup/restore.
  3. Upgrading from the single-layout preference preserves the previous active layout and existing custom layouts.
  4. Removing or breaking a profile always selects a valid fallback and never leaves the IME with no enabled layout.
  5. Selecting a profile updates the visible keyboard without full-IME recreation or visible flicker.

**Plans**: TBD
**UI hint**: yes

### Phase 3: Reliable Language Switching and Context

**Goal**: Users can change language instantly through accessible controls or configurable gestures, with optional per-app restoration.
**Depends on**: Phase 2
**Requirements**: REQ-quick-language-switch, REQ-gesture-language-switch, REQ-per-app-language-memory
**Success Criteria** (what must be TRUE):

  1. Next and previous actions cycle the configured EN → RU → LV order without inserting text, emitting fake keycodes, or resetting the current input session.
  2. The keyboard shows the active language, provides haptic/accessibility feedback, and exposes a configurable or disableable gesture using the same switching behavior as the sidebar control.
  3. Existing symbols and custom-layout actions remain reachable when language gestures are enabled.
  4. With per-app memory enabled, returning to Telegram and GitHub restores their independently chosen languages before the old layout can flash.
  5. Per-app state remains local, stores no typed text, and safely ignores mappings to removed profiles.

**Plans**: TBD
**UI hint**: yes

### Phase 4: Language-Scoped Suggestions

**Goal**: Users receive private, deterministic completions for the active language through the existing three-slot suggestion workflow.
**Depends on**: Phase 3
**Requirements**: REQ-language-aware-suggestions, REQ-dictionary-autocomplete
**Success Criteria** (what must be TRUE):

  1. Switching language clears or recomputes suggestions, and RU history or dictionary words do not affect EN results.
  2. The existing three visible suggestion slots and gesture selection continue to work with typed, language-identified candidates.
  3. Prefix completion ranks licensed offline dictionary entries by base/user frequency and preserves appropriate initial-letter case.
  4. Repeatedly used words rise in ranking only within their language profile.
  5. Password fields show no suggestions, record no learning data, and no typed text is sent to an external service.

**Plans**: TBD
**UI hint**: yes

### Phase 5: Correction and Context Prediction

**Goal**: Users get useful language-aware typo fixes and next-word suggestions without unsafe or irreversible edits.
**Depends on**: Phase 4
**Requirements**: REQ-typo-correction-suggestions, REQ-automatic-correction-undo, REQ-next-word-prediction
**Success Criteria** (what must be TRUE):

  1. Known active-language typos produce typed correction candidates while words from another language do not leak into correction ranking.
  2. Users can disable automatic replacement independently and immediately undo a high-confidence correction to recover the exact original word.
  3. Password, URL/email, code-like, and no-dictionary contexts avoid unsafe automatic correction.
  4. Context such as `thank` can raise `you` above unrelated frequent words, while EN and RU n-grams remain isolated.
  5. Prediction works offline using bounded aggregate storage with an observable cleanup/limit policy.

**Plans**: TBD

### Phase 6: Advanced Multilingual UX and Public Release

**Goal**: Users can handle mixed-language writing efficiently and install a hardened public build suitable for daily keyboard use.
**Depends on**: Phase 5
**Requirements**: REQ-one-word-temporary-language, REQ-bilingual-suggestions, REQ-release-hardening
**Success Criteria** (what must be TRUE):

  1. A user can switch from RU to temporary EN for one word and return automatically at a valid boundary, or cancel back to RU without inserting text.
  2. Temporary mode is visually distinct and uses the temporary language for completion, correction, and prediction.
  3. When bilingual suggestions are enabled, primary and secondary candidates retain their language identity and selecting a secondary candidate does not force a layout change.
  4. Compatibility scenarios cover supported Android versions, editor field types, representative messaging/browser/terminal apps, restart, migration, backup/restore, privacy, switching, and correction undo.
  5. A reproducible, signed, versioned release with changelog, attribution, license-audited dictionaries/assets, and release tag can be installed as intended.

**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:** Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Stable Fork Baseline | 7/7 | Complete    | 2026-09-18 |
| 2. Multilingual Profiles and Settings | 0/TBD | Not started | - |
| 3. Reliable Language Switching and Context | 0/TBD | Not started | - |
| 4. Language-Scoped Suggestions | 0/TBD | Not started | - |
| 5. Correction and Context Prediction | 0/TBD | Not started | - |
| 6. Advanced Multilingual UX and Public Release | 0/TBD | Not started | - |
