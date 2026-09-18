# Phase 2: Multilingual Profiles and Settings — Research

**Researched:** 2026-09-18
**Domain:** Android/Kotlin, Compose settings, SharedPreferences migration, SAF, backup/restore, IME state
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within Phase 2 scope. Next/previous controls, gesture
switching, per-app language memory, and language-scoped smart text remain in
later roadmap phases.
</user_constraints>

Источник ограничений: текст выше скопирован дословно из `02-CONTEXT.md`.
[VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:23-111,238-243]

## Project Constraints (task-supplied AGENTS.md)

- План исполнения обязан идти через TDD: сначала тест, подтверждённое падение,
  затем минимальная production-реализация. [VERIFIED: task-supplied AGENTS.md]
- Нельзя добавлять скрытое состояние, monkey-patch атрибуты функций или ad-hoc
  DI только ради unit-тестов; platform seams должны быть нормальными production
  границами. [VERIFIED: task-supplied AGENTS.md]
- Kotlin/Compose изменения должны быть точечными, а итог — проверен тестами до
  объявления готовности. [VERIFIED: task-supplied AGENTS.md]
- Репозиторного `./AGENTS.md` нет; эти ограничения пришли непосредственно в
  задаче. [VERIFIED: filesystem check]

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REQ-language-profiles | Add stable ordered `LanguageProfile` state above `Layout<*>`, support embedded and custom layouts equally, migrate `prefs.layout.current`, persist active language, provide safe fallback, and centralize writes in one manager observed by the IME. | Aggregate ordered snapshot, idempotent legacy bootstrap, operation-specific fallback matrix, application-scoped manager, resolved IME session state. |
| REQ-language-settings | Enable, order, select primary, edit optional custom locale, delete custom layouts safely, and survive restart/backup/restore. | Unified Compose list, accessible explicit controls, metadata dialog, stable-ID archive manifest, ID→new-URI restore remap, warning-bearing restore result. |

Требования и acceptance-текст определены в `REQUIREMENTS.md`.
[VERIFIED: .planning/REQUIREMENTS.md:14-19]
</phase_requirements>

## Summary

Фазу следует строить вокруг одного сериализованного ordered snapshot и одного
application-scoped manager. Не хранить `profiles`, `activeProfileId` и
`primaryProfileId` независимыми SharedPreferences-ключами: текущий
`PreferenceData.set()` создаёт новый editor и вызывает `apply()` для каждого
значения, поэтому межключевой invariant может быть виден в промежуточном
состоянии. Один JSON snapshot в одном `PreferenceData<LanguageConfig>` даёт
одну атомарную замену и естественно сохраняет порядок `List`.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt:32-43]
[CITED: https://developer.android.com/reference/android/content/SharedPreferences.Editor]

Миграция должна быть двухфазной: поднять `AppPrefs` schema version и оставить
старые preferences как read-only migration inputs; при первом создании manager,
если aggregate key отсутствует, прочитать legacy current/history/previous,
проверить custom URI, построить нормализованный snapshot и записать его ровно
один раз. `PreferenceModel.migrate()` обрабатывает записи по одной, поэтому без
расширения framework hook он не может корректно синтезировать одну коллекцию из
нескольких legacy keys. Наличие нового aggregate key является idempotence guard.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt:240-283]

Backup/restore нельзя ограничить generic `exportedKeys`: новый archive manifest
должен связывать immutable profile ID с archive-relative document path, а restore
должен после staging/validation создать явное `profileId → new sourceUri`
соответствие. Live import дедуплицирует по `sourceUri`, но restored ID остаётся
неизменным. Текущий suffix-based remap и broad `copyRecursively()` этого контракта
не обеспечивают и одновременно наследуют Zip Slip/partial-restore риск.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:39-112]
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/ZipUtils.kt:37-61]

**Primary recommendation:** сначала зафиксировать pure domain snapshot,
normalizer и migration/fallback tests; затем подключить manager к AppPrefs,
SAF/backup, Compose и, последним integration slice, к существующему
`Vim8ImeService.keyboardData` без пересоздания input view. [ASSUMED]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|--------------|----------------|-----------|
| Profile model and invariants | Android application/domain | Persistence | Manager computes the whole valid next snapshot before any write. [ASSUMED] |
| Ordered persistence and migration | SharedPreferences storage | Application manager | Existing typed registry owns preference identity; manager performs cross-key legacy bootstrap. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:24-79; 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt:207-225] |
| Embedded/custom validation | Layout domain | Android SAF | Existing loaders validate both sources; SAF owns persistent URI grants. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt:35-58,134-152,194-255] |
| Settings affordances | Compose settings UI | Application manager | UI renders state and issues manager commands; it does not write prefs directly. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:47-57,103-108] |
| Active layout delivery | IME service | Application manager | Existing service swaps `keyboardData`; manager should emit only fully loaded sessions. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt:79-115] |
| Backup/restore remap | Backup boundary | Application manager/storage | Archive code owns documents/remap; manager normalizes and commits one replacement snapshot. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:82-99] |
| URI grant/cache cleanup | Android SAF/cache boundary | Application manager | External cleanup is idempotent side effect after a valid state transition. [CITED: https://developer.android.com/reference/android/content/ContentResolver] |

## Standard Stack

Новые зависимости для фазы не нужны. Использовать уже подключённые Kotlin,
Compose Material 3, Jackson, Arrow, Kotest и MockK. [VERIFIED: gradle/libs.versions.toml:1-38; 8vim/build.gradle.kts:230-280]

### Core

| Library/platform | Version | Purpose | Why Standard |
|------------------|---------|---------|--------------|
| Kotlin | 2.2.10 | Domain model, manager, SerDe | Уже основной язык модуля. [VERIFIED: gradle/libs.versions.toml:23] |
| Jetpack Compose UI | 1.10.4 | Settings rendering/state collection | Уже подключён к settings/IME UI. [VERIFIED: gradle/libs.versions.toml:7,48] |
| Material 3 | 1.4.0 | Switch, RadioButton, dialog, accessible controls | Уже подключён, новых UI packages не требуется. [VERIFIED: gradle/libs.versions.toml:9,46] |
| Jackson Kotlin/JSON | 2.18.6 | Versioned aggregate preference and archive manifest | Уже используется `JsonMapper` в backup. [VERIFIED: gradle/libs.versions.toml:20,61; 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:12-13,37] |
| Android SharedPreferences | platform API, minSdk 24 | Persist one serialized snapshot via existing `PreferenceModel` | Сохраняет существующую datastore identity и export/reset behavior. [VERIFIED: 8vim/build.gradle.kts:77-103; 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt:231-283] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Arrow Core | 1.2.4 | Explicit layout/import/restore failures | На fallible boundaries, сохраняя текущий `Either` pattern. [VERIFIED: gradle/libs.versions.toml:18,54] |
| Kotest | 5.9.1 | Domain, migration, fallback, SerDe/property tests | Все JVM contracts. [VERIFIED: gradle/libs.versions.toml:35,80-86] |
| MockK | 1.14.9 | Android Context/ContentResolver/SharedPreferences boundaries | Только platform boundaries. [VERIFIED: gradle/libs.versions.toml:38,87-89] |
| Compose UI test | 1.10.4 | Settings semantics/interactions | Instrumented radio/switch/reorder/dialog checks. [VERIFIED: gradle/libs.versions.toml:7,74] |

Проверяемые discrete versions процитированы дословно:

DATA_K7P4N2Q8_START
```toml
androidx-compose = "1.10.4"
androidx-compose-material3 = "1.4.0"
arrow = "1.2.4"
jackson = "2.18.6"
kotlin = "2.2.10"
kotest = "5.9.1"
mockk = "1.14.9"
```
DATA_K7P4N2Q8_END
[VERIFIED: gradle/libs.versions.toml:7-23,35-38]

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| One aggregate JSON preference | Several primitive/StringSet keys | Rejected: order and active/primary/enabled invariants can tear across writes. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt:35-43] |
| Existing SharedPreferences registry | AndroidX DataStore/Room | Rejected for this phase: violates locked brownfield persistence boundary and expands migration surface. [VERIFIED: .planning/PROJECT.md:49-55] |
| Explicit up/down controls | Drag-and-drop | Rejected by D-06. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:49-51] |
| Stable ID plus mutable `sourceUri` | URI as the only record key | Rejected for restore: URI changes while backup ID must remain stable. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:87-93] |

**Installation:** none. The existing dependency block already contains every
recommended library. [VERIFIED: 8vim/build.gradle.kts:230-280]

## Package Legitimacy Audit

Not applicable: the recommended implementation installs no external package.
The planner must not add a dependency for JSON, ordering, locale parsing,
flows, ZIP handling, or hashing because equivalent project/platform facilities
already exist. [VERIFIED: 8vim/build.gradle.kts:230-280]

## Architecture Patterns

### System Architecture Diagram

```text
Legacy prefs / fresh install / restored archive
                    │
                    ▼
       decode + validate + migrate/reconcile
                    │
                    ▼
       LanguageConfig normalizer (pure)
      ┌─────────────┼────────────────┐
      │ valid       │ missing custom │ malformed
      ▼             ▼                ▼
 ordered snapshot  prune + warning  safe embedded en
      │
      ▼
 LanguageManager computes complete next state
      │ validate/load target before commit
      ▼
 one aggregate SharedPreferences write
      │
      ├──────────────► StateFlow to Compose settings
      └──────────────► resolved active KeyboardData to existing IME view

Backup export: snapshot ─► archive manifest ─► custom documents
Restore: ZIP staging ─► containment/schema checks ─► ID→new URI remap
        ─► normalize ─► one replacement commit ─► warnings to UI
```

Этот flow следует locked manager boundary и существующим application/IME
ownership seams. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:103-111; 8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt:29-43]

### Recommended Project Structure

```text
8vim/src/main/kotlin/inc/flide/vim8/
├── AppPrefs.kt                         # aggregate preference + inert legacy inputs
├── VIM8Application.kt                  # application-scoped LanguageManager
├── Vim8ImeService.kt                   # observe resolved active session only
├── ime/language/
│   ├── LanguageProfile.kt              # profile/layout-ref/config value types
│   ├── LanguageConfigSerDe.kt          # versioned JSON preference codec
│   ├── LanguageConfigNormalizer.kt     # pure invariants/fallback decisions
│   ├── LanguageMigration.kt            # pure legacy → aggregate transform
│   └── LanguageManager.kt              # sole mutation/orchestration API
├── app/settings/
│   ├── LayoutScreen.kt                 # unified ordered profile settings
│   └── CustomLayoutImportAdapter.kt    # SAF acquire/release boundary
└── lib/backup/BackupManager.kt          # versioned manifest and ID→URI remap
```

Это рекомендуемые имена в зоне agent discretion, не существующие факты.
[ASSUMED]

### Pattern 1: One aggregate, versioned snapshot

**What:** persist the entire ordered profile state as one JSON string. The list
index is the order; do not also persist an `order` integer that can disagree
with the list. Keep `primaryProfileId`, `activeProfileId`, and persisted
`lastValidProfileId` in the same envelope. [ASSUMED]

**Why:** one `SharedPreferences.Editor.apply()` atomically performs all changes
in that editor, while current `PreferenceData.set()` uses one editor per value.
[CITED: https://developer.android.com/reference/android/content/SharedPreferences.Editor]
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt:35-43]

**Recommended sketch (not drop-in code):** [ASSUMED]

```kotlin
data class LanguageConfig(
    val schemaVersion: Int,
    val profiles: List<LanguageProfile>,
    val primaryProfileId: String,
    val activeProfileId: String,
    val lastValidProfileId: String?,
)

data class LanguageProfile(
    val id: String,
    val layoutRef: LayoutRef,
    val displayName: String,
    val localeTag: String?,
    val enabled: Boolean,
)
```

### Pattern 2: Identity and source location are separate

For embedded profiles, derive an immutable ID from the embedded path. For a new
live custom import, derive an immutable ID from the exact URI-string identity
used by Phase 1. Persist custom `sourceUri` separately. On restore, preserve ID
and replace only `sourceUri`. On later live import, deduplicate first by current
canonical `sourceUri`; if it matches a restored profile, reuse that profile's
persisted ID rather than deriving a second one. [ASSUMED]

Phase 1 identity compares custom URI strings verbatim and explicitly treats
content digest only as cache identity. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:42-54,144-163]

Recommended concrete ID format is `embedded:<path>` and
`custom:<canonical-uri-string>` for newly created live records; it is an
internal versioned format and must never be recomputed for restored records.
[ASSUMED]

### Pattern 3: Normalize before persist, never repair after persist

Every manager command follows one transaction shape: read current snapshot →
resolve/load required layouts → compute next snapshot → normalize all invariants
→ persist once → emit one non-null resolved session. A failed layout load
does not publish the requested invalid active ID. [ASSUMED]

Normalizer invariants:

1. IDs and custom source identities are unique. [ASSUMED]
2. Ordered list has at least one enabled, valid profile. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-78]
3. Primary and active point to enabled, valid members. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-78,94-99]
4. Reorder/metadata edits preserve IDs, active, and primary. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:34-43]
5. Malformed persisted data becomes a valid embedded-en snapshot, not a crash or partial object. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:109-111]

### Pattern 4: Operation-specific fallback policy

Do not implement a single “first valid profile” helper; locked precedence differs
by operation. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-99]

| Trigger | Required resolution before commit |
|---------|-----------------------------------|
| Disable/remove non-active | Keep active/primary unless removed primary needs deterministic promotion. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-74] |
| Disable/remove active, primary still valid | Select enabled valid primary. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-62] |
| Disable active that is also primary | Select next enabled valid profile in persisted order, wrapping once; assign it to both primary and active. Wrapping is recommended interpretation. [ASSUMED] |
| Disable final enabled | Reject; emit no state change. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:72-74] |
| Inactive custom URI breaks | Remove it; do not change active. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:75-78] |
| Active custom URI breaks | Persisted last-valid active, then embedded en; remove broken profile. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:75-78] |
| Restore loses custom documents | Restored valid primary, restored valid last-active, then embedded en; preserve remaining order and warn. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:94-99] |
| Malformed snapshot | Embedded en only, primary=active=en. [ASSUMED] |

### Pattern 5: Idempotent legacy bootstrap

Current preference schema is version 9 and its relevant exact values are:

DATA_B4T9R6M1_START
```kotlin
class AppPrefs : PreferenceModel(9) {
```
DATA_B4T9R6M1_END
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:26]

DATA_Q2H7V5C9_START
```kotlin
val current = custom(
    key = "prefs_layout_current",
    default = EmbeddedLayout("en"),
    serde = LayoutSerDe
)

val previousValid = custom(
    key = "prefs_layout_previous_valid",
    default = EmbeddedLayout("en"),
    serde = LayoutSerDe,
    canBeExported = false
)

inner class Custom {
    val history = stringSet(
        key = "prefs_layout_custom_history",
        default = emptySet()
    )
}
```
DATA_Q2H7V5C9_END
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:61-78]

Recommended bootstrap algorithm: [ASSUMED]

1. Bump the model version and register one exportable aggregate preference.
2. Retain the three quoted legacy preferences as non-exported, read-only inputs;
   remove all runtime writers to them.
3. If aggregate `getOrNull()` is non-null, decode/normalize it and never rerun
   legacy migration.
4. If aggregate is absent, load legacy current, previous-valid, and every valid
   custom history URI. Put current first; because persisted `StringSet` has no
   order contract, sort remaining custom URI strings for deterministic output.
5. Set active and primary to valid legacy current; if broken, apply Phase 1
   previous-valid→en fallback. Write the aggregate once.
6. Fresh install follows the same absent-key path and creates embedded en only.

The sort policy and primary=current choice are recommended discretion decisions
and must be frozen in tests because legacy `StringSet` cannot prove recency/order
after persistence. [ASSUMED]

### Pattern 6: Stage, validate, remap, replace

Recommended backup manifest contains a schema version, complete language
snapshot, and explicit records `{profileId, archivePath}` for custom documents.
Archive file names should be generated safe names independent of URI/display
name; the manifest provides identity. [ASSUMED]

Restore sequence: [ASSUMED]

1. Extract to a unique staging directory with canonical containment checks.
2. Parse manifest and settings; reject unsupported future schema before writes.
3. Validate/copy each custom document into a unique app-owned restore directory.
4. Build explicit `profileId → new file/content URI`; keep profile IDs unchanged.
5. Drop missing/invalid documents, normalize with restore precedence, collect warnings.
6. Atomically replace only the language configuration snapshot.
7. Publish state, then clean unreferenced old app-owned restore files.

Legacy version-9 backups have no profile IDs. Support them by first applying
their current path remap, then passing the resulting old current/history through
the same legacy bootstrap; ID preservation begins with the new archive schema.
[ASSUMED]

### Pattern 7: Load-then-swap IME session

`Vim8ImeService` currently creates one Compose input view and keeps
`keyboardData` as Compose mutable state. It also directly observes
`prefs.layout.current` and swaps the data on successful load. Preserve the
state-swap boundary, but replace the raw preference observer with manager
observation of a fully resolved session; never set `keyboardData` to null and
never call IME/view recreation for profile changes. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt:74-115,214-225]

Android defines `onCreateInputView()` as the creation of the input view hierarchy
and may call it again for configuration changes; ordinary profile selection
should therefore remain a state update inside the existing view. This final
sentence is an inference from the platform lifecycle contract.
[CITED: https://developer.android.com/reference/android/inputmethodservice/InputMethodService]

### Pattern 8: Accessible settings controls

- Render all embedded/custom entries in the same `Column`; the enclosing
  `Screen` already owns vertical scrolling, so do not nest a `LazyColumn`.
  [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/compose/Screen.kt:103-129]
- Use a row-level `toggleable(Role.Switch)` plus trailing `Switch`, matching the
  existing settings primitive; final-enabled row is disabled and has explanatory
  supporting text. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/ui/SwitchPreference.kt:27-50]
- Give up/down/delete/edit icons non-null localized content descriptions and
  disable the impossible boundary move. [CITED: https://developer.android.com/develop/ui/compose/accessibility/semantics]
- Put enabled profiles in a separate `selectableGroup`; each whole row uses
  `selectable(role = Role.RadioButton)` and child `RadioButton(onClick = null)`.
  [CITED: https://developer.android.com/develop/ui/compose/components/radio-button]
- Keep edit dialog values local until explicit confirm; blank locale becomes
  null, and a nonblank tag should be normalized/validated before manager call.
  [ASSUMED]

### Anti-Patterns to Avoid

- **Independent preference writes:** can expose active/primary IDs that no
  longer belong to the enabled list. Use one aggregate snapshot. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt:35-43]
- **StringSet as order:** Android preference Set is not the profile order. Use a
  serialized List. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceSerDe.kt:88-106]
- **ID derived on every read:** restore changes source URI; immutable ID and
  mutable source must be separate. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:87-93]
- **UI writes to `prefs.layout.current`:** diverges from manager invariants and
  Phase 3 consumers. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:103-105]
- **Publish-then-load:** causes invalid active state and possible flicker. Load
  fully, then commit/emit once. [ASSUMED]
- **First valid fallback:** contradicts operation-specific locked precedence.
  [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-99]
- **Broad restore copy:** current `copyRecursively(filesDir)` can partially
  overwrite before semantic validation. Use staged promotion of known files.
  [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:81-108]
- **Unbounded/uncontained unzip:** reject traversal and resource exhaustion
  before writing outside staging. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/ZipUtils.kt:37-61]
- **Weak Activity global manager:** current `availableLayouts` is a weak global
  initialized from Compose. Move language authority to `VIM8Application`.
  [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt:38-83]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON configuration/archive codec | Delimiter strings or manual escaping | Existing Jackson Kotlin `JsonMapper` | Handles structured Lists/nullability and already powers backup. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:12-13,37,75,87-90] |
| Layout validation | Extension/MIME/name heuristics | Existing `Layout.loadKeyboardData` and zero-layer check | Current parser/loader is authoritative and Phase 1 picker accepts `*/*`. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:144-169; 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt:28-48] |
| URI permission lifecycle | Copying external document or custom grant registry | `takePersistableUriPermission` / `releasePersistableUriPermission` | Android owns durable grant state across reboot. [CITED: https://developer.android.com/reference/android/content/ContentResolver] |
| UI radio semantics | Independent clickable radio icon | `selectableGroup`, selectable row, `Role.RadioButton`, passive child RadioButton | Official Compose accessibility pattern. [CITED: https://developer.android.com/develop/ui/compose/components/radio-button] |
| State observation | Activity weak reference or polling prefs | Application-scoped manager plus `StateFlow`/existing observable pattern | Existing process services and Compose already observe state. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt:29-43; 8vim/src/main/kotlin/inc/flide/vim8/ime/nlp/SuggestionsManager.kt:34-35] |
| Fallback | Generic first/firstOrNull selection | Named, operation-specific pure policy functions | Locked precedences differ. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-99] |

**Key insight:** complexity is not the data class; it is atomic preservation of
identity, order, references, resource validity, grants, cache and backup remaps
under every mutation. Centralize that state machine and keep UI/IME as consumers.
[ASSUMED]

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | Default SharedPreferences contain legacy exact keys `"prefs_layout_current"`, `"prefs_layout_previous_valid"`, `"prefs_layout_custom_history"`, `"prefs_layout_cache"`; user backup ZIPs contain `settings.json` and custom documents. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:55-78; 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:39-76] | Code migration plus legacy archive importer. Keep legacy keys read-only until aggregate exists; new archives get explicit schema/remap records. |
| Live service config | None: no remote service or UI/database-held configuration participates; layouts and backups are local. [VERIFIED: .planning/codebase/INTEGRATIONS.md:5-45] | None. |
| OS-registered state | SAF persisted read grants for imported `content://` URIs; IME service registration is unchanged. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt:32-38; 8vim/src/main/AndroidManifest.xml:22-33] | Release held grant when custom profile is forgotten; never remove external document. No IME re-registration. |
| Secrets/env vars | No language-profile secret/env key; release-signing env vars are build-only and unrelated. [VERIFIED: 8vim/build.gradle.kts:113-121] | None. |
| Build artifacts | CBOR layout cache files under `context.cacheDir` and cache-key set can outlive a removed custom profile; restored app-owned documents can outlive replaced configs. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Cache.kt:11-33; 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:81-108] | Add targeted cache eviction and reference-based cleanup of app-owned restored documents; do not delete original external URIs. |

## Common Pitfalls

### Pitfall 1: “Atomic manager” with non-atomic persistence

**What goes wrong:** manager computes a valid result but writes list, primary and
active separately; observers see a dangling ID between writes. **Why:** each
current preference set has its own editor/apply. **Avoid:** one aggregate
snapshot. **Warning sign:** tests need to suppress observers during mutation.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceData.kt:35-43]

### Pitfall 2: Re-running migration after malformed data

**What goes wrong:** corrupt aggregate deserialization returns “absent”, so stale
legacy preferences resurrect old state. **Avoid:** distinguish missing key from
malformed present key; malformed present data resolves to safe en and is repaired,
while only truly absent aggregate invokes legacy bootstrap. [ASSUMED]

### Pitfall 3: Losing restored identity

**What goes wrong:** restored file URI is used to regenerate ID, breaking active,
primary and future per-app references. **Avoid:** archive record preserves ID;
only source URI is remapped. **Warning sign:** restore code calls normal live-ID
factory for every document. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:87-99]

### Pitfall 4: URI dedupe after restore creates duplicates

**What goes wrong:** restored profile has backup ID and a new local URI; importing
that same URI later derives a new ID. **Avoid:** live import first finds an
existing profile by canonical source URI and reuses its ID. [ASSUMED]

### Pitfall 5: Permission and cache cleanup is mistaken for atomic storage

**What goes wrong:** an exception releasing a grant prevents the state invariant
from being committed, or state is valid but a grant leaks silently. **Avoid:**
commit a valid state once; perform idempotent external cleanup with explicit
warning/retry result. Check held persisted grants before release. [ASSUMED]
[CITED: https://developer.android.com/reference/android/content/ContentResolver]

### Pitfall 6: Existing import leaks a newly taken grant on validation failure

**What goes wrong:** current adapter takes the grant before validation and does
not release it if layout import returns Left. **Avoid:** track whether this call
acquired a new grant and release on failed new import; preserve an existing
profile's already-needed grant. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt:32-48]

### Pitfall 7: Fallback precedence collapses into list order

**What goes wrong:** a broken active custom layout selects the first list item,
violating last-valid→en; user disable has a different primary-first policy.
**Avoid:** tests name trigger and expected precedence explicitly.
[VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:61-99]

### Pitfall 8: IME flashes old/null layout

**What goes wrong:** active ID emits before its KeyboardData is ready, or service
recreates the Compose view. **Avoid:** manager loads first, then emits one resolved
session; service atomically swaps non-null `keyboardData`. **Warning sign:** code
sets `keyboardData = null` during selection. [ASSUMED]

### Pitfall 9: Nested scroll and icon-only accessibility regressions

**What goes wrong:** LazyColumn is placed under the already scrollable Screen,
and reorder buttons have no readable label/state. **Avoid:** plain Column, disabled
boundary controls, localized content descriptions, selectable radio group.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/compose/Screen.kt:103-129]
[CITED: https://developer.android.com/develop/ui/compose/accessibility/semantics]

### Pitfall 10: Restore validates after copying

**What goes wrong:** current implementation recursively copies staged content to
`filesDir` before version/semantic replacement completes. **Avoid:** validate the
entire archive and known paths in staging, then promote only accepted documents.
[VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:81-108]

## Code Examples

Все examples ниже — planning sketches, а не готовый production API. [ASSUMED]

### Pure transition before persistence

```kotlin
fun setEnabled(profileId: String, enabled: Boolean): Either<LanguageError, LanguageConfig> {
    val current = state.value.config
    val candidate = transitions.setEnabled(current, profileId, enabled)
    val resolved = normalizer.resolve(candidate).bind()
    store.set(resolved.config) // one aggregate preference write
    publish(resolved)          // one fully loaded, non-null session
    return resolved.config.right()
}
```

### Restore mapping keeps ID stable

```kotlin
val remappedProfiles = manifest.profiles.mapNotNull { profile ->
    when (val ref = profile.layoutRef) {
        is EmbeddedRef -> profile
        is CustomRef -> restoredUriByProfileId[profile.id]
            ?.let { newUri -> profile.copy(layoutRef = ref.copy(sourceUri = newUri)) }
    }
}
```

### Accessible primary radio group

```kotlin
Column(Modifier.selectableGroup()) {
    enabledProfiles.forEach { profile ->
        Row(
            Modifier.selectable(
                selected = profile.id == primaryProfileId,
                onClick = { manager.setPrimary(profile.id) },
                role = Role.RadioButton,
            ),
        ) {
            RadioButton(
                selected = profile.id == primaryProfileId,
                onClick = null,
            )
            Text(profile.displayName)
        }
    }
}
```

Pattern source: official Compose radio button guidance.
[CITED: https://developer.android.com/develop/ui/compose/components/radio-button]

## State of the Art

| Old/current approach | Phase 2 approach | Impact |
|----------------------|------------------|--------|
| One `Layout<*>` in `prefs_layout_current` | Versioned ordered `LanguageConfig` above layouts | Enables stable multi-profile identity/order and atomic references. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt:61-78; .planning/REQUIREMENTS.md:16-19] |
| Custom history in `StringSet` | Profiles in serialized List | Order survives restart/backup. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceSerDe.kt:88-106] |
| Activity-owned weak `AvailableLayouts` | Application-scoped language manager | Settings and IME share one authority. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt:42-83; 8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt:29-43] |
| Backup rewrites URI suffixes | Manifest maps stable ID to restored URI | Restore changes source location without changing identity. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt:67-73,99-108; .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:87-99] |
| IME observes raw current preference | IME observes resolved manager session | Load-then-swap avoids null/invalid intermediate state. [ASSUMED] |

**Deprecated after this phase:** direct runtime writes to
`prefs.layout.current`, custom history mutation outside manager, and
`availableLayouts` as settings/activity authority. Keep only the legacy values
needed for one-time migration. [ASSUMED]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Persist all visible embedded/custom profiles in one ordered snapshot; append newly discovered embedded layouts disabled. | Architecture Patterns | Reconciliation/order behavior on app upgrade would need a different schema/task. |
| A2 | Legacy migration orders current first, then remaining valid custom URIs lexicographically; primary becomes old current. | Legacy bootstrap | Existing StringSet has no durable order, so another policy changes migrated order. |
| A3 | “Next available in persisted order” scans forward and wraps once. | Fallback matrix | Last-position disable behavior changes. |
| A4 | Recommended new IDs use `embedded:<path>` and `custom:<canonical-uri-string>`. | Identity | Format becomes persistent ABI; must be frozen before implementation. |
| A5 | Persist `lastValidProfileId` inside the aggregate and include it in new backup manifests. | Snapshot/backup | D-17 needs restored last-valid precedence; omitting it requires another recoverable-history mechanism. |
| A6 | Existing version-9 backup archives remain supported through legacy migration. | Backup | Dropping compatibility would violate brownfield expectations but is not stated as an explicit D-number. |
| A7 | Blank locale maps to null; nonblank locale tags are validated/normalized. | Settings | Accepting arbitrary strings may impair later dictionary selection. |

Планировщик должен freeze A1–A7 as explicit implementation decisions before
their first persisted-format task; A2/A4/A5 are one-way or costly once shipped.

## Open Questions

1. **Where will executable Android proof run?**
   - What we know: host has Java 26, not Java 17; local Android API 36 and Build
     Tools 36.0.0 are missing, while Docker client/server 29.5.1 are available.
     [VERIFIED: environment probes 2026-09-18]
   - What's unclear: the prior compatible Docker SDK volume invocation is not
     versioned as a repository command. [VERIFIED: repository search]
   - Recommendation: use exact-SHA CI or re-use/document the Phase 1 pinned
     Docker environment before claiming green product checks. [ASSUMED]

2. **No unresolved product question remains.**
   - What we know: CONTEXT locks defaults, identity, interactions, fallbacks,
     backup and restore. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:23-111]
   - What's unclear: only A1–A7 implementation-discretion details above.
   - Recommendation: planner records them in plan task actions; do not ask again
     about D-01–D-17. [ASSUMED]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Repository Gradle wrapper | JVM/build gates | ✓ | 9.4.0 | — [VERIFIED: `./gradlew --version`] |
| Java | Android build | ✗ compatible | Host 26.0.1; required 17 | Exact-SHA CI or pinned Docker Java 17. [VERIFIED: environment probe; scripts/baseline-check.sh:90-145] |
| Android SDK Platform | compile/lint/APK/instrumentation | ✗ host | API 36 missing | Exact-SHA CI or pinned Docker SDK. [VERIFIED: `./scripts/baseline-check.sh --preflight-only`] |
| Android Build Tools | APK gate | ✗ host | 36.0.0 missing | Exact-SHA CI or pinned Docker SDK. [VERIFIED: `./scripts/baseline-check.sh --preflight-only`] |
| Docker daemon | Compatible build environment | ✓ | 29.5.1 | — [VERIFIED: `docker version`] |
| adb | Manual/device UI and flicker smoke | ✓ | 35.0.2 | CI/instrumented emulator for automation; physical-device smoke still needed. [VERIFIED: `adb version`] |

**Missing dependencies with no fallback:** none, provided exact-SHA CI or the
known compatible Docker environment is available. [ASSUMED]

**Missing dependencies with fallback:** host Java 17, API 36, Build Tools
36.0.0. The canonical host preflight currently fails before Gradle gates.
[VERIFIED: environment probe 2026-09-18]

## Validation Architecture

`workflow.nyquist_validation` is absent because `.planning/config.json` does
not exist; per GSD contract validation is enabled. [VERIFIED: filesystem check]

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Kotest 5.9.1 + JUnit 5; MockK 1.14.9 at Android boundaries. [VERIFIED: gradle/libs.versions.toml:35-38,80-89] |
| Config file | `8vim/build.gradle.kts` (`useJUnit`, debug unit task only). [VERIFIED: 8vim/build.gradle.kts:177-195,220-228] |
| Quick run command | `./gradlew :8vim:testDebugUnitTest --tests 'inc.flide.vim8.ime.language.*' --tests 'inc.flide.vim8.lib.backup.BackupManagerSpec' --tests 'inc.flide.vim8.app.settings.CustomLayoutImportAdapterSpec'` [ASSUMED: proposed test paths] |
| Full suite command | `./scripts/baseline-check.sh` [VERIFIED: scripts/baseline-check.sh:198-212] |
| Instrumented settings command | `./gradlew :8vim:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest` [ASSUMED: proposed test class] |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-language-profiles | Snapshot round-trip, malformed safe default, uniqueness/order invariants | JVM unit/property | quick command above | ❌ Wave 0 |
| REQ-language-profiles | Fresh install + idempotent legacy current/history migration, stale URI pruning | JVM unit | quick command above | ❌ Wave 0 |
| REQ-language-profiles | Disable/remove/broken/restore fallback matrix; final-enabled rejection | JVM unit/data | quick command above | ❌ Wave 0 |
| REQ-language-profiles | Import same URI reuses ID; restore remap preserves ID while URI changes | JVM unit/integration | quick command above | ❌ Wave 0 extensions |
| REQ-language-profiles | Manager emits one fully loaded non-null session; reorder/metadata do not alter refs | JVM unit | quick command above | ❌ Wave 0 |
| REQ-language-profiles | Existing IME view changes visible KeyboardData without recreation/flicker | device smoke + narrow instrumentation if feasible | connected test + manual observation | ❌ Wave 0/manual |
| REQ-language-settings | Unified list, toggle, up/down order, separate primary radio semantics | Compose instrumentation | instrumented settings command | ❌ Wave 0 |
| REQ-language-settings | Custom edit confirm/cancel; delete active safely; final toggle disabled/explained | Compose instrumentation + manager JVM | instrumented + quick commands | ❌ Wave 0 |
| REQ-language-settings | Backup includes full config/docs, excludes NLP data, restore replaces and warns on missing docs | JVM backup tests | quick command above | ⚠️ Existing file, new cases needed |
| REQ-language-settings | Legacy backup restores through migration | JVM backup test | quick command above | ⚠️ Existing file, new fixture/case needed |

### Required Test Matrix

1. **Migration:** fresh, embedded current, custom current in history, duplicate
   current/history, stale inactive, stale active with valid previous, both broken,
   repeated initialization, unordered history input. [VERIFIED: D-01,D-02,D-14]
2. **Mutation:** reorder preserves refs; metadata preserves ID; primary only
   enabled; disable active with primary; disable active-primary next/wrap; final
   disable rejected; remove custom invokes cleanup. [VERIFIED: D-03,D-04,D-10–D-13]
3. **Identity/remap:** same URI no duplicate; same bytes/different URI distinct;
   restore ID stable/new URI; import restored URI reuses restored ID.
   [VERIFIED: D-03,D-09,D-16]
4. **Restore:** full replacement, missing one/all custom docs, invalid primary,
   invalid active, preserved remaining order, warning, legacy archive, malformed
   future schema, traversal entry. [VERIFIED: D-15–D-17]
5. **IME/UI:** no null intermediate session, no `onCreateInputView` request on
   selection, accessible reorder/radio/toggle semantics, restart persistence.
   [VERIFIED: REQ-language-profiles acceptance]

### Sampling Rate

- **Per task commit:** relevant filtered `testDebugUnitTest --tests ...`; for UI
  task, compile plus instrumented test when a device is attached. [ASSUMED]
- **Per wave merge:** `./scripts/baseline-check.sh` in Java 17/API 36 environment.
  [VERIFIED: scripts/baseline-check.sh:198-212]
- **Phase gate:** full baseline green, focused profile/backup JVM green,
  instrumentation semantics green, and physical/emulator smoke recording no
  visible old/null layout flash. [ASSUMED]

### Wave 0 Gaps

- [ ] `ime/language/LanguageConfigSerDeSpec.kt` — ordered round-trip,
  malformed/present vs missing, schema compatibility. [ASSUMED]
- [ ] `ime/language/LanguageMigrationSpec.kt` — D-01/D-02/D-14 matrix and
  idempotence. [ASSUMED]
- [ ] `ime/language/LanguageManagerSpec.kt` — all mutation/fallback/identity
  invariants and single resolved emission. [ASSUMED]
- [ ] Extend `BackupManagerSpec.kt`, `CustomLayoutImportAdapterSpec.kt`, and
  `ZipUtilsSpec.kt` for stable remap, replacement, warnings, permission release,
  and traversal rejection. [VERIFIED: existing files]
- [ ] `androidTest/.../LanguageProfilesScreenTest.kt` — unified list, control
  semantics, dialog, persisted order/primary. [ASSUMED]

### Exact Verification Commands

```bash
# Focused domain + integration JVM tests (after Wave 0 files exist)
./gradlew :8vim:testDebugUnitTest \
  --tests 'inc.flide.vim8.ime.language.*' \
  --tests 'inc.flide.vim8.lib.backup.BackupManagerSpec' \
  --tests 'inc.flide.vim8.lib.ZipUtilsSpec' \
  --tests 'inc.flide.vim8.app.settings.CustomLayoutImportAdapterSpec'

# All JVM tests
./gradlew :8vim:testDebugUnitTest

# Settings semantics/interactions on attached device/emulator
./gradlew :8vim:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=inc.flide.vim8.app.LanguageProfilesScreenTest

# Canonical final unit/lint/ktlint/checkstyle/APK gate
./scripts/baseline-check.sh
```

Commands using proposed classes are [ASSUMED]; the full gate is
[VERIFIED: scripts/baseline-check.sh:198-212].

## Security Domain

`security_enforcement` is not explicitly false, so this section is required.
[VERIFIED: `.planning/config.json` absent]

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | No account/authentication domain. [VERIFIED: .planning/codebase/INTEGRATIONS.md:25-29] |
| V3 Session Management | no | Language session is local UI/domain state, not an authentication session. [VERIFIED: .planning/codebase/ARCHITECTURE.md:7-31] |
| V4 Access Control | yes | Android SAF persisted read grants; release held grants when profile is forgotten. [CITED: https://developer.android.com/reference/android/content/ContentResolver] |
| V5 Input Validation | yes | Existing YAML parser/zero-layer validation; strict JSON schema/version checks; canonical ZIP containment and bounded known entries. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt:144-169] |
| V6 Cryptography | no | No encryption/signature is introduced by the locked phase; do not hand-roll crypto. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:6-16,80-99] |

### Known Threat Patterns for Android local backup/SAF

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| ZIP path traversal (`../`, absolute path) | Tampering / Elevation | Resolve canonical destination under staging root and reject escaping entries before output. Current code lacks this check. [VERIFIED: 8vim/src/main/kotlin/inc/flide/vim8/lib/ZipUtils.kt:37-61] |
| Oversized/count-heavy archive | Denial of Service | Bound total bytes, entry count and per-entry size; stream to staging and cleanup in `finally`. [ASSUMED] |
| Partial restore overwrites files before validation | Tampering / DoS | Parse and validate all metadata/documents first; promote known files only; commit config last. [VERIFIED: current unsafe order at BackupManager.kt:81-108] |
| Stale persisted URI grant after failure/removal | Information disclosure / resource leak | Release only held grants; release newly acquired grant on rejected import; surface cleanup failure. [CITED: https://developer.android.com/reference/android/content/ContentResolver] |
| Backup unintentionally includes learned/typed data | Information disclosure | Whitelist language snapshot and layout documents; assert no NLP DB/history/text payload in archive. [VERIFIED: .planning/phases/02-multilingual-profiles-and-settings/02-CONTEXT.md:82-86] |
| Malformed profile references | Tampering / DoS | Decode to DTO, reject duplicates/type mismatches, normalize primary/active/enabled before one commit. [ASSUMED] |

The manifest declares only `VIBRATE`; this phase must not add network/storage
permissions. Exact current permission value is quoted below. [VERIFIED: 8vim/src/main/AndroidManifest.xml:5]

DATA_M8D3S6J2_START
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```
DATA_M8D3S6J2_END
[VERIFIED: 8vim/src/main/AndroidManifest.xml:5]

## Sources

### Primary (HIGH confidence)

- `02-CONTEXT.md` — D-01–D-17, discretion, scope/deferred work.
- `REQUIREMENTS.md:14-19` and `ROADMAP.md:58-72` — required behavior and success criteria.
- `AppPrefs.kt:24-79,283-410` — schema version, exact legacy keys/defaults, migration hook.
- `PreferenceModel.kt:13-18,62-100,207-283` and `PreferenceData.kt:8-85` — registry, export, migration and write/observer behavior.
- `Layout.kt:35-58,60-105,134-255` and `AvailableLayouts.kt:16-271` — layout identity, validation, cache, fallback and import behavior.
- `LayoutScreen.kt`, `CustomLayoutImportAdapter.kt`, `Screen.kt`, `SwitchPreference.kt` — current UI/SAF patterns.
- `BackupManager.kt:28-112` and `ZipUtils.kt:9-61` — archive layout, remap and security boundaries.
- `VIM8Application.kt:29-93`, `MainActivity.kt:38-105`, `Vim8ImeService.kt:67-115` — service ownership and IME update seams.
- Existing JVM/instrumentation specs — Kotest/MockK patterns and gaps.

### Secondary (MEDIUM confidence)

- https://developer.android.com/reference/android/content/SharedPreferences.Editor — atomic editor apply contract.
- https://developer.android.com/reference/android/content/ContentResolver — persisted URI grant acquire/release contract.
- https://developer.android.com/develop/ui/compose/components/radio-button — accessible radio-group pattern.
- https://developer.android.com/develop/ui/compose/accessibility/semantics — content/state semantics.
- https://developer.android.com/reference/android/inputmethodservice/InputMethodService — input view lifecycle.

### Tertiary (LOW confidence)

- A1–A7 in the Assumptions Log are recommended implementation-discretion
  decisions, not verified platform facts.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — read directly from version catalog/build file; no new
  packages. [VERIFIED: gradle/libs.versions.toml; 8vim/build.gradle.kts]
- Architecture: HIGH — locked context plus direct code inspection of every
  integration boundary. [VERIFIED: cited files]
- Persistence/migration: HIGH — exact preference implementation and keys read;
  recommended schema details are isolated in assumptions. [VERIFIED: cited files]
- Backup/security: HIGH for current risks, MEDIUM for proposed quota policy.
- Compose/IME: HIGH for current code, MEDIUM for lifecycle/accessibility guidance
  from official Android docs.
- Pitfalls: HIGH where derived from code/locked decisions; LOW only for explicitly
  tagged assumptions.

**Research date:** 2026-09-18
**Valid until:** 2026-10-18 (stable brownfield stack; refresh if dependencies or
Phase 1 layout/backup code changes)
