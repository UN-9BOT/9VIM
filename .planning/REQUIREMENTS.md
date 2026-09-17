# Requirements: 9VIM

**Defined:** 2026-09-17
**Core Value:** Users can type reliably in several languages with fast 8VIM gestures, without losing text, privacy, or custom-layout support.
**Source:** `docs/prd/8vim_fork_implementation_brief_ru.md`

## v1 Requirements

### Stable Fork

- [x] **REQ-stable-fork-baseline**: Establish a tagged, reproducible fork baseline with CI unit tests, lint/style checks, debug APK build, selected upstream fixes, explicit custom-layout override semantics, and a captured Enter/newline regression.
  - **Acceptance:** A clean checkout builds through one CI command; all unit tests pass; the custom-layout picker works on current Android; a baseline tag/commit exists; historic Dependabot PRs are not bulk-merged.

### Multilingual Profiles and Settings

- [ ] **REQ-language-profiles**: Add stable ordered `LanguageProfile` state above `Layout<*>`, support embedded and custom layouts equally, migrate `prefs.layout.current`, persist the active language, provide safe fallback, and centralize changes in one language/session manager observed by the IME.
  - **Acceptance:** At least two embedded layouts and one custom layout can be enabled together; list, order, and active language survive restart; upgrade preserves the previous active and custom layouts; at least one valid layout always remains; programmatic switching has no visible keyboard flicker.
- [ ] **REQ-language-settings**: Provide multi-layout settings for enabling, ordering, choosing the primary language, assigning an optional locale to custom layouts, and deleting custom layouts without leaving a dangling active language.
  - **Acceptance:** The user can configure EN + RU + custom LV; EN → RU → LV order persists; custom layouts switch like embedded layouts; settings survive restart and backup/restore when exported.

### Switching and Context

- [ ] **REQ-quick-language-switch**: Add next/previous language commands through `CustomKeycode` and `KeyboardManager`, with a sidebar control, visible active-language code, haptics, and an accessibility label, without conflating language switching with changing Android IMEs.
  - **Acceptance:** One action cycles EN → RU → LV → EN; previous returns backward; active language is visually clear; input state and current text remain intact; no character or fake external keycode is emitted.
- [ ] **REQ-gesture-language-switch**: Add configurable, disableable next/previous language gestures through the existing action/custom-keycode pipeline, avoiding layout conflicts and hidden reserved sequences.
  - **Acceptance:** The gesture can be disabled or remapped; ordinary symbols remain reachable; custom layouts are not broken by a hidden sequence; gesture and sidebar use the same language manager.
- [ ] **REQ-per-app-language-memory**: Optionally remember `languageProfileId` per `EditorInfo.packageName`, restore it before keyboard view preparation, store no typed text, and tolerate stale mappings and backup/restore.
  - **Acceptance:** Telegram can restore RU while GitHub restores EN; mapping remains local and needs no network permission; removing a language does not prevent the IME from opening.

### Language-Scoped Suggestions

- [ ] **REQ-language-aware-suggestions**: Introduce typed, language-scoped suggestions with separate orchestration, sources, ranking, history, and seed data while preserving three gesture-selectable UI slots, keeping Text Replacement separate, and disabling learning/prediction in password fields.
  - **Acceptance:** RU history cannot affect EN ranking; switching language clears or recomputes suggestions; the existing three-slot selection UI still works; password fields neither learn nor show suggestions.
- [ ] **REQ-dictionary-autocomplete**: Provide deterministic offline completion per supported language, ranked by prefix, base/user frequency, active language, and initial-letter case, using only license-approved dictionary data.
  - **Acceptance:** Completion is isolated per enabled language; RU words do not appear in EN mode unless bilingual mode is enabled; frequently used words rise in rank; suggestion case matches sentence context.

### Correction and Prediction

- [ ] **REQ-typo-correction-suggestions**: Generate active-language correction candidates using length-aware Damerau-Levenshtein/edit-distance operations and type them as `CORRECTION` suggestions.
  - **Acceptance:** A known typo produces a correction candidate; correction respects the active language.
- [ ] **REQ-automatic-correction-undo**: Apply high-confidence automatic correction only at word boundaries, retain short-lived original/replacement/session state for immediate undo, expose an independent toggle, and suppress correction in password, URL/email/code-like, and no-dictionary contexts.
  - **Acceptance:** Auto-replacement can be disabled without hiding suggestions; immediate undo restores the original word; password fields are never corrected.
- [ ] **REQ-next-word-prediction**: Provide bounded offline per-language bigram prediction, weighted toward user history, with frequency fallback and aggregate n-gram storage cleanup.
  - **Acceptance:** After `thank`, `you` can outrank an unrelated globally frequent word; EN and RU n-grams remain separate; storage has a defined limit/cleanup strategy; prediction works fully offline.

### Advanced Multilingual UX and Release

- [ ] **REQ-one-word-temporary-language**: Add a visible one-word temporary-language state with base/temporary IDs, automatic language-aware return at word boundary, cancellation without input, and temporary-language smart text.
  - **Acceptance:** RU → temporary EN → automatic RU needs no second manual switch; cancel/back returns to the base language without input; prediction/autocorrect use the temporary language dictionary.
- [ ] **REQ-bilingual-suggestions**: Optionally rank candidates from a primary and secondary language using script as a signal and retaining candidate language IDs, without making suggestion language automatically change the layout.
  - **Acceptance:** Absent from the source document.
- [ ] **REQ-release-hardening**: Prepare a public fork release with a compatibility matrix, regression suite, reproducible build, appropriate application ID/signing, changelog, attribution/notices, dictionary/assets license audit, versioning, and release tags.
  - **Acceptance:** Absent from the source document.

## v2 Requirements

None defined.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Radial language selector | Deferred until reliable next/previous switching is stable |
| Switching to another Android IME | Separate product behavior from language switching inside 9VIM |
| Cloud suggestion or correction services | Violates offline-first privacy scope |
| Automatic language detection as source of truth | Explicit active language remains authoritative |
| Multi-sector geometry and layout tutorial | Do not block multilingual/smart-text v1 |
| Unlicensed GPL/source-available dictionaries or assets | Incompatible without an explicit licensing decision |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| REQ-stable-fork-baseline | Phase 1 | Complete |
| REQ-language-profiles | Phase 2 | Pending |
| REQ-language-settings | Phase 2 | Pending |
| REQ-quick-language-switch | Phase 3 | Pending |
| REQ-gesture-language-switch | Phase 3 | Pending |
| REQ-per-app-language-memory | Phase 3 | Pending |
| REQ-language-aware-suggestions | Phase 4 | Pending |
| REQ-dictionary-autocomplete | Phase 4 | Pending |
| REQ-typo-correction-suggestions | Phase 5 | Pending |
| REQ-automatic-correction-undo | Phase 5 | Pending |
| REQ-next-word-prediction | Phase 5 | Pending |
| REQ-one-word-temporary-language | Phase 6 | Pending |
| REQ-bilingual-suggestions | Phase 6 | Pending |
| REQ-release-hardening | Phase 6 | Pending |

**Coverage:**

- v1 requirements: 14 total
- Mapped to phases: 14
- Unmapped: 0 ✓

---
*Requirements defined: 2026-09-17*
*Last updated: 2026-09-17 after roadmap creation*
