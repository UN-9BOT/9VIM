# Phase 2: Multilingual Profiles and Settings - Context

**Gathered:** 2026-09-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver persistent multilingual profile state above the existing layout engine
and a Compose settings flow for configuring it. Users must be able to enable,
order, name, localize, and choose the primary profile among embedded and
custom layouts; the state must migrate from the single-layout preference,
survive restart and supported backup/restore, and always resolve to a valid
enabled profile. This phase does not add next/previous switching controls,
gestures, per-app memory, or language-scoped smart text; those are later
phases.

</domain>

<decisions>
## Implementation Decisions

### Profile defaults, identity, and migration

- **D-01:** A new installation enables only embedded `en` initially. Other
  embedded profiles are opt-in from settings.
- **D-02:** Migration from `prefs.layout.current` is idempotent. Preserve the
  old current layout as the active profile and include every valid custom
  history entry in the enabled ordered set; do not auto-enable unrelated
  embedded layouts. Prune stale custom URIs using the Phase 1 rules.
  — **Reversibility:** one-way — changing the migration contract later would
  require another preference migration and could alter an existing user's
  enabled set.
- **D-03:** `LanguageProfile.id` is immutable. For live custom layouts it is
  derived from the canonical URI identity established in Phase 1. Changing a
  custom display name or optional locale updates metadata only and must not
  change ordering, active state, per-app mappings, or deduplication.
  — **Reversibility:** one-way — IDs are persisted and referenced by active,
  backup, and future per-app state.
- **D-04:** Persist `primaryProfileId` separately from `activeProfileId`.
  Primary is the startup/default fallback; active is the current runtime
  selection. Reordering does not implicitly change either reference unless
  its profile is removed.

### Settings interaction

- **D-05:** Show embedded and custom profiles in one ordered list. A source/type
  badge may distinguish them without splitting the switch order into sections.
- **D-06:** Reordering uses explicit up/down buttons, not drag-and-drop. This
  keeps ordering deterministic and provides a straightforward accessibility
  path.
- **D-07:** Edit custom display name and optional locale in a profile detail
  dialog with explicit confirmation; keep the main list compact.
- **D-08:** Choose the primary/default profile through a separate radio group,
  distinct from enabled toggles and runtime active state.
- **D-09:** A successful custom import remains an enabled profile and becomes
  active immediately, reusing the Phase 1 URI-based deduplication behavior.

### Removal and fallback

- **D-10:** Removing or disabling the active profile resolves the replacement
  to the enabled, valid primary profile before committing the state change.
- **D-11:** The active profile may be disabled directly. The replacement and
  enabled-set invariant are resolved atomically. If the disabled profile is
  itself primary, promote a deterministic remaining enabled profile (the next
  available profile in persisted order) to both primary and active. Do not
  select an arbitrary valid profile.
- **D-12:** Removing a custom profile means forgetting it from profile state
  and history, revoking persisted URI permission when held, and clearing
  derived cache/metadata. Never delete or modify the user's external source
  document.
- **D-13:** The final enabled profile cannot be disabled. Its toggle/action is
  unavailable and the UI explains that at least one language profile is
  required; do not silently enable another language.
- **D-14:** Phase 1 broken-URI behavior remains authoritative: inactive broken
  URIs are removed without changing current; active broken URIs fall back to
  the last valid active profile, then embedded `en`; broken URIs are removed
  from history/enabled state; never pick the first arbitrary valid layout.

### Backup and restore

- **D-15:** Backup the complete language configuration: stable profile IDs,
  enabled state, persisted order, metadata including optional locale,
  `primaryProfileId`, `activeProfileId`, and the custom layout documents needed
  for restore. Do not include typed text, word-frequency data, prediction
  history, or other NLP personalization in this phase.
- **D-16:** Restore fully replaces the device's language configuration rather
  than merging it. A custom profile keeps its stable backup ID while its
  backing `sourceUri` is remapped to the newly restored local document. URI
  remains the source identity for live import/deduplication; restore must have
  an explicit ID-to-new-URI mapping.
  — **Reversibility:** costly — changing this contract affects archive schema,
  profile references, and restore compatibility.
- **D-17:** Restore is successful when it produces a valid usable state even if
  some custom documents are unavailable. Remove unavailable profiles while
  preserving remaining order, resolve invalid references as restored valid
  primary → restored last-valid profile → embedded `en`, keep the enabled set
  non-empty with primary and active inside it, and surface missing custom
  layouts as a non-fatal warning.

### the agent's Discretion

- Exact Kotlin class/file names for `LanguageProfile`, the profile collection,
  and the central language/session manager, provided all writes go through one
  manager and IDs/ordering/active invariants above are preserved.
- Exact visual treatment of the unified list, type badge, radio group, and
  detail dialog, provided the selected interactions and accessibility behavior
  remain intact.
- Exact serialized schema for profile metadata and archive remap records,
  provided migration is idempotent, backup IDs remain stable, and malformed
  records fail safe.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product brief and phase contract

- `docs/prd/8vim_fork_implementation_brief_ru.md` §Этап 1 — language profile
  model, stable IDs, migration, fallback, and manager boundary.
- `docs/prd/8vim_fork_implementation_brief_ru.md` §Этап 2 — multi-layout
  settings, custom locale, deletion, and acceptance examples.
- `docs/prd/8vim_fork_implementation_brief_ru.md` §Технические решения,
  пункты 1, 2, 4, 7 — language-above-layout, one manager, explicit active
  language, and custom layouts as first-class citizens.
- `.planning/PROJECT.md` — project scope, privacy, compatibility, backup, and
  explicit out-of-scope boundaries.
- `.planning/REQUIREMENTS.md` — `REQ-language-profiles` and
  `REQ-language-settings` acceptance contracts.
- `.planning/ROADMAP.md` — Phase 2 goal and success criteria.
- `.planning/STATE.md` — current phase position and known lifecycle concerns.
- `.planning/phases/01-stable-fork-baseline/01-CONTEXT.md` — locked URI
  identity, import activation/deduplication, and broken-layout fallback rules.

### Existing architecture and persistence

- `.planning/codebase/STACK.md` — Android/Gradle/JVM and Compose/Kotlin
  dependencies.
- `.planning/codebase/ARCHITECTURE.md` — application/service ownership,
  settings flow, layout loading, and preference boundaries.
- `.planning/codebase/CONVENTIONS.md` — Kotlin, Compose, Arrow, and test
  conventions.
- `.planning/codebase/STRUCTURE.md` — locations for settings, datastore,
  layout, tests, and backup code.
- `8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt` — current preference
  schema and migration/version hook replacing single-layout state.
- `8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt` —
  typed preference registry, versioned migrations, export/reset semantics.
- `8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt` —
  preference serialization and observation patterns.

### Layout, settings, IME, and backup integration

- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt` — embedded/custom
  layout references, URI loading, cache identity, and safe loading.
- `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt` —
  current embedded/custom registry, URI history, import/upsert, stale removal,
  and previous-valid fallback.
- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt` — current
  Compose layout picker and custom import entry point to evolve into profile
  management.
- `8vim/src/main/kotlin/inc/flide/vim8/app/settings/BackupRestoreScreen.kt` —
  existing backup/restore UI and user-visible result handling.
- `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt` — current
  settings/custom-file archive format and URI remapping behavior.
- `8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt` — current layout
  observer and keyboard-data update boundary; must observe the central language
  state without recreating the IME.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `PreferenceModel`/`AppPrefs`: typed SharedPreferences registry, versioned
  `migrate`, exported-key filtering, reset semantics, and observers; extend
  this identity rather than introducing a second persistence mechanism.
- `AvailableLayouts`: embedded/custom discovery, URI history, validation,
  upsert, stale removal, and previous-valid/default recovery; wrap or refactor
  it behind the new language/session manager.
- `LayoutScreen` and `datastore/ui` Compose preference primitives: existing
  screen shell, import launcher, dialogs, rows, and state observation to reuse
  for the unified ordered list.
- `BackupManager`: existing ZIP/settings archive and custom document packaging;
  extend its schema for stable profile IDs, metadata, and explicit source-URI
  remaps.

### Established Patterns

- Shared services are created through `VIM8Application`/Context accessors and
  preference state is observed rather than copied into independent singletons.
- Fallible layout operations use Arrow `Either` and safe loaders; malformed
  profile/archive records should follow the same explicit failure/fallback
  style.
- Preference values currently use primitive/StringSet serializers. Ordered
  profiles must use an explicit ordered serialization format; a plain Set must
  not be treated as the source of order.
- Tests mirror production packages and use Kotest `FunSpec`/contexts plus
  MockK only at Android/platform boundaries.

### Integration Points

- `AppPrefs`/`Datastore` migration and export registry: new profile state,
  schema version, and idempotent old-layout migration.
- `AvailableLayouts`/`Layout`/new language manager: one source of truth for
  profile selection, custom URI validation, active/primary invariants, and
  keyboard-data updates.
- `LayoutScreen`, `Routes`, and backup/restore screen: unified list, ordering
  buttons, primary radio group, custom detail dialog, and restore warnings.
- `Vim8ImeService`: observe active profile/layout state and update
  `keyboardData` without full IME recreation or visible flicker.
- JVM specs for `AvailableLayouts`, `PreferenceModel`, layout serialization,
  and backup behavior: migration, ordering, dedupe, fallback, privacy, and
  restore remap coverage.

</code_context>

<specifics>
## Specific Ideas

- The target user flow is EN → RU → LV, with custom LV behaving like an
  embedded profile in the same ordered list.
- A successful custom import immediately adds/enables the profile and makes it
  active; importing the same URI reuses the existing profile.
- A broken URI is removed from history/enabled state. If it was inactive,
  current language is untouched; if active, fallback is last-valid active then
  embedded `en`, never an arbitrary first valid layout.
- Restore must not carry typed text or language-learning data; a missing custom
  document is a warning, not a reason to discard the rest of the backup.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 2 scope. Next/previous controls, gesture
switching, per-app language memory, and language-scoped smart text remain in
later roadmap phases.

</deferred>

---

*Phase: 2-Multilingual Profiles and Settings*
*Context gathered: 2026-09-18*
