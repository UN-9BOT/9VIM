---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# External Integrations

**Analysis Date:** 2026-09-17

## APIs & External Services

**Community and distribution links:**

- GitHub, Matrix, Twitter, Google Play - opened through Android `ACTION_VIEW` from `8vim/src/main/kotlin/inc/flide/vim8/lib/android/LaunchUtils.kt`; URLs are constants in `8vim/src/main/kotlin/inc/flide/vim8/app/Urls.kt`.
- F-Droid and Google Play - distribution references in `README.md`; no in-app SDK integration detected.

**Schema resources:**

- 8vim.github.io JSON Schema identifiers - metadata identifiers in `8vim/src/main/resources/schemas/`; schemas are bundled and used locally, not fetched at runtime.

## Data Storage

**Databases:**

- Private on-device SQLite - word-frequency data through `SQLiteOpenHelper` in `8vim/src/main/kotlin/inc/flide/vim8/ime/nlp/WordFrequencyDatabase.kt` and repository access in `WordFrequencyRepository.kt`.
- Android SharedPreferences - settings and serialized preferences via `8vim/src/main/kotlin/inc/flide/vim8/datastore/model/PreferenceModel.kt`.

**File Storage:**

- Android app files/assets - bundled dictionaries/layouts in `8vim/src/main/assets/` and `8vim/src/main/res/raw/`.
- User-selected YAML files and ZIP backups - Android document/content intents handled by `8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt` and `8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt`.

**Caching:**

- Local CBOR cache for parsed layouts via `8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Cache.kt` and `CacheParser.kt`.

## Authentication & Identity

**Auth Provider:**

- None. The app has no account system or remote identity provider.

## Monitoring & Observability

**Error Tracking:**

- None detected (no crash/analytics SDK).

**Logs:**

- SLF4J with Logback Android configured in `8vim/src/main/assets/logback.xml`; logging remains on-device.

## CI/CD & Deployment

**Hosting:**

- Android artifacts are distributed through Google Play and F-Droid; project source/releases are linked to GitHub in `README.md`.

**CI Pipeline:**

- GitHub Actions references appear in `metadata/en-US/changelogs/`; workflow definitions are not detected in the scanned repository.

## Environment Configuration

**Required env vars:**

- None for normal builds/runtime.
- Release signing variables are optional and consumed by `8vim/build.gradle.kts`.

**Secrets location:**

- External build environment only; secret files and values are not inspected.

## Webhooks & Callbacks

**Incoming:**

- None. Android intent filters in `8vim/src/main/AndroidManifest.xml` accept launcher, IME, custom `vim8://app-ui`, and local YAML/file-open intents.

**Outgoing:**

- User-initiated browser/share intents only, implemented in `8vim/src/main/kotlin/inc/flide/vim8/lib/android/LaunchUtils.kt`; no server callbacks.

---

*Integration audit: 2026-09-17*
