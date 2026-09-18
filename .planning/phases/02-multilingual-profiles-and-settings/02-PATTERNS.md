# Phase 2: Multilingual Profiles and Settings - Pattern Map

**Mapped:** 2026-09-18
**Files analyzed:** 24 new/modified targets
**Analogs found:** 24 / 24 (all analog paths passed git ls-files)

The ime/language filenames below are the recommended names from
02-RESEARCH.md; exact names remain discretionary. MainActivity, Routes,
AvailableLayouts, and ZipUtils are integration touchpoints explicitly called
out by the phase artifacts: preserve their public contracts where possible,
but replace the old ownership/security boundaries required by the new manager
and staged restore.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt | config/persistence | transform + CRUD | same file, lines 26-79, 283-409 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt | provider | event-driven initialization | same file, lines 29-49, 71-93 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt | service | event-driven observer → UI state | same file, lines 67-115 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/app/MainActivity.kt | provider/integration | event-driven Compose setup | same file, lines 42-83 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/app/Routes.kt | route | request-response navigation | same file, lines 20-56 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/app/settings/LayoutScreen.kt | component/screen | request-response + event-driven state | same file, lines 20-89 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/app/settings/BackupRestoreScreen.kt | component/screen | file-I/O + request-response | same file, lines 31-84, 117-149 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapter.kt | adapter/service | file-I/O request-response | same file, lines 18-48 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt | service/registry | CRUD + transform/fallback | same file, lines 42-72, 150-170, 219-269 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/lib/backup/BackupManager.kt | service/utility | file-I/O batch | same file, lines 36-112 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/lib/ZipUtils.kt | utility | file-I/O transform | same file, lines 37-61 | exact |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageProfile.kt | model | transform | ime/layout/Layout.kt, lines 53-58, 194-255 | role-match |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageConfigSerDe.kt | utility/codec | transform | Layout.kt, lines 158-191; BackupManager.kt, lines 36-37 | role-match |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageConfigNormalizer.kt | domain policy | transform | AvailableLayouts.kt, lines 56-137, 219-269 | role-match |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageMigration.kt | utility/domain policy | batch transform | AppPrefs.kt, lines 283-398; PreferenceModel.kt, lines 240-283 | role-match |
| 8vim/src/main/kotlin/inc/flide/vim8/ime/language/LanguageManager.kt | service | CRUD + event-driven publish | AvailableLayouts.kt, lines 150-217; VIM8Application.kt, lines 29-49 | role-match |
| 8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageConfigSerDeSpec.kt | test | transform | PreferenceSerDeSpec.kt, lines 18-162 | role-match |
| 8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageMigrationSpec.kt | test | batch transform | PreferenceModelSpec.kt, lines 27-121, 312-334 | role-match |
| 8vim/src/test/kotlin/inc/flide/vim8/ime/language/LanguageManagerSpec.kt | test | CRUD + fallback | AvailableLayoutsSpec.kt, lines 44-109, 157-294 | role-match |
| 8vim/src/test/kotlin/inc/flide/vim8/lib/backup/BackupManagerSpec.kt (extend) | test | file-I/O batch | same file, lines 50-225 | exact |
| 8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt (extend) | test | file-I/O request-response | same file, lines 19-86 | exact |
| 8vim/src/test/kotlin/inc/flide/vim8/lib/ZipUtilsSpec.kt (extend) | test | file-I/O/security | same file, lines 12-57 | exact |
| 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt (extend if registry retained) | test | CRUD + fallback | same file, lines 188-366 | exact |
| 8vim/src/androidTest/kotlin/inc/flide/vim8/app/LanguageProfilesScreenTest.kt | test | request-response UI | SettingsScreenTest.kt, lines 12-21 | role-match |

## Pattern Assignments

### AppPrefs.kt (config/persistence, transform + CRUD)

**Analog:** 8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt,
lines 26-79, 283-409 (tracked).

**Preference registration** (lines 26-34, 61-79):

~~~kotlin
class AppPrefs : PreferenceModel(9) {
    val layout = Layout()
    val theme = Theme()

    val current = custom(
        key = "prefs_layout_current",
        default = EmbeddedLayout("en"),
        serde = LayoutSerDe
    )
}
~~~

Register one exportable aggregate custom preference with a JSON serde. Keep
legacy current, previousValid, and custom.history as read-only migration
inputs until the aggregate exists. The aggregate contains ordered profiles,
enabled flags, immutable IDs, metadata, primaryProfileId, activeProfileId, and
the last-valid reference used by restore fallback. Do not split those
references into independent keys.

**Version/migration hook** (lines 283-298, 396-409):

~~~kotlin
override fun migrate(
    previousVersion: Int,
    entry: PreferenceMigrationEntry
): PreferenceMigrationEntry = when (previousVersion) {
    1 -> when (entry.key) {
        "select_keyboard_layout" -> entry.transform(key = "prefs_layout_current")
        else -> entry.keepAsIs()
    }
    else -> entry.keepAsIs()
}
~~~

Bump the model version, but perform the cross-key legacy bootstrap in
LanguageMigration/LanguageManager: PreferenceModel.migrate visits entries
independently and cannot synthesize one ordered value from current/history/
previous. Preserve the existing postInitialize cache cleanup convention.

### VIM8Application.kt (provider, event-driven)

**Analog:** same file, lines 29-49, 71-93 (tracked).

~~~kotlin
class VIM8Application : Application() {
    private val prefs by appPreferenceModel()
    val backupManager = lazy { BackupManager(this) }

    override fun onCreate() {
        super.onCreate()
        vim8ApplicationReference = WeakReference(this)
        prefs.initialize(this)
    }
}
~~~

Add an application-scoped lazy LanguageManager using the existing layout loader
and context. Initialize after preferences are ready; settings and IME must get
the same instance.

**Context accessor pattern** (lines 71-93):

~~~kotlin
fun Context.layoutLoader() = this.vim8Application().layoutLoader
fun Context.backupManager() = this.vim8Application().backupManager
~~~

Expose Context.languageManager() the same way. Do not create a manager in
MainActivity or a composable.

### Vim8ImeService.kt (service, event-driven)

**Analog:** same file, lines 67-115 (tracked).

**Current load-then-swap observer** (lines 95-100):

~~~kotlin
prefs.layout.current.observe {
    it.loadKeyboardData(layoutLoader, this)
        .onRight { keyboardData ->
            this.keyboardData = keyboardData
        }
}
~~~

Replace only the source observer with the manager's fully resolved active
session. Keep success-only publication: never publish null or unloaded data,
and never write preferences from the IME.

**Existing input-view boundary** (lines 109-115):

~~~kotlin
override fun onCreateInputView(): View {
    super.installViewTreeOwners()
    keyboardData = safeLoadKeyboardData(layoutLoader, this)
    val composeView = ComposeInputView()
    inputWindowView = composeView
    return composeView
}
~~~

Profile changes update keyboardData inside this existing Compose view. Do not
call onCreateInputView, recreate the service, or set keyboardData to null during
a switch.

### MainActivity.kt (provider/integration, event-driven)

**Analog:** same file, lines 42-83 (tracked).

**Current boundary to replace:**

~~~kotlin
var availableLayouts: WeakReference<AvailableLayouts?> = WeakReference(null)

if (availableLayouts.get() == null) {
    availableLayouts = WeakReference(AvailableLayouts(layoutLoader, context))
}
~~~

Remove the Activity-owned weak registry as language authority. Obtain the
application manager through context.languageManager() and retain only
navigation/preview locals. Preserve the existing prefs.onReady/setContent
lifecycle at lines 47-69.

### Routes.kt (route, request-response)

**Analog:** same file, lines 20-56 (tracked).

~~~kotlin
object Settings {
    const val LAYOUTS = "settings/layouts"
}

composable(Settings.LAYOUTS) { LayoutScreen() }
~~~

Keep this destination if the unified profile list remains in LayoutScreen. If
a separate screen is chosen, add it with the same NavHost/composable pattern;
do not make navigation a second profile state store.

### LayoutScreen.kt (screen, request-response + event-driven)

**Analog:** same file, lines 20-89 (tracked).

**Screen/dialog pattern** (lines 20-58):

~~~kotlin
@Composable
fun LayoutScreen() = Screen {
    title = stringRes(R.string.settings__layouts__title)
    previewFieldVisible = true

    content {
        PreferenceGroup {
            Dialog {
                index = { availableLayouts.get()?.index ?: 0 }
                items = { availableLayouts.get()?.displayNames ?: emptyList() }
                onConfirm { availableLayouts.get()?.selectLayout(it) }
            }
        }
    }
}
~~~

Evolve this into one ordered Column of embedded/custom profiles with source
badge, toggle, explicit up/down buttons, separate primary radio group, and
delete/edit actions. Render manager state and send manager commands; do not
mutate AppPrefs or prefs.layout.current directly. Screen already owns vertical
scrolling, so do not nest LazyColumn.

**SAF launcher/error boundary** (lines 61-89):

~~~kotlin
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    when (val result = CustomLayoutImportAdapter(
        context.contentResolver, layouts
    ).importLayout(uri)) {
        is CustomLayoutImportResult.Success -> Unit
        is CustomLayoutImportResult.Failure -> showAlert(
            context, R.string.dialog__error__title, result.error.message
        )
    }
}
~~~

Keep SAF launching here, but make successful import go through the manager so
the profile stays enabled and becomes active immediately (D-09).

### BackupRestoreScreen.kt (screen, file-I/O + request-response)

**Analog:** same file, lines 31-84, 117-149 (tracked).

**Result-bearing launcher pattern** (lines 43-84):

~~~kotlin
backupManager.export()
    .flatMap { Either.catch { context.contentResolver.writeFromFile(uri, it) } }
    .onRight { context.showToast(successId) }
    .onLeft { context.showToast(failureId, "error_message" to it.message) }

backupManager.import(uri)
    .onLeft { context.showToast(failureId, "error_message" to it.message) }
    .onRight { option -> /* warning or success */ }
~~~

Continue using Either for fatal restore failure and Option<Int> (or a typed
warning result) for non-fatal missing custom documents. Show the warning
without popping the screen until the replacement configuration is usable.
Preserve the existing reset confirmation dialog at lines 124-149.

### CustomLayoutImportAdapter.kt (adapter, file-I/O request-response)

**Analog:** same file, lines 18-48 (tracked).

~~~kotlin
sealed interface CustomLayoutImportResult {
    data class Success(val layout: Layout<*>) : CustomLayoutImportResult
    data class Failure(val error: LayoutError) : CustomLayoutImportResult
}

fun importLayout(uri: Uri): CustomLayoutImportResult = runCatching {
    contentResolver.takePersistableUriPermission(
        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
    )
    availableLayouts.importLayout(CustomLayout(uri))
}.fold(/* Either → Success/Failure */)
~~~

Retain wildcard MIME, read-only persistable permission, and provider exception
handling at this boundary. Track whether this call acquired a new grant and
release it if validation fails. Removing a profile releases held permissions
but never deletes the external document (D-12).

### AvailableLayouts.kt (registry, CRUD + transform/fallback)

**Analog:** same file, lines 42-72, 150-170, 197-217, 219-269 (tracked).

**URI identity and cleanup** (lines 42-54, 56-72):

~~~kotlin
private fun sameIdentity(left: Layout<*>, right: Layout<*>): Boolean = when {
    left is EmbeddedLayout && right is EmbeddedLayout -> left.path == right.path
    left is CustomLayout && right is CustomLayout ->
        left.path.toString() == right.path.toString()
    else -> false
}
~~~

Keep canonical URI string as live custom source identity; content digest is only
a cache key. Move preference/history mutations behind LanguageManager, or make
this registry a manager-owned loader/validator. Preserve history/cache cleanup.

**Validate before mutate and activate on success** (lines 150-170):

~~~kotlin
return layout.loadKeyboardData(layoutLoader, context).flatMap { keyboardData ->
    if (keyboardData.totalLayers == 0) emptyLayoutError().left()
    else {
        upsert(layout, keyboardData)
        updateHistory(path)
        rememberPreviousValid(current, layout)
        prefs.layout.current.set(layout)
        layout.right()
    }
}.onLeft { if (knownLayout) removeStaleLayout(path) }
~~~

The manager must preserve this order but commit one normalized aggregate, not
several primitive keys. selectLayout (lines 197-217) is the load-then-activate
analog; listCustomLayoutHistory (lines 219-269) is the stale URI/prior-valid
analog. Replace arbitrary-first fallback with the D-14 precedence.

### BackupManager.kt (service, file-I/O batch)

**Analog:** same file, lines 36-112 (tracked).

**JSON mapper and export staging** (lines 36-76):

~~~kotlin
private val prefs by appPreferenceModel()
private val objectMapper = JsonMapper.builder().build().registerKotlinModule()

val dir = File(context.cacheDir, UUID.randomUUID().toString())
val customDir = File(dir, "custom")
val settings = File(dir, "settings.json")
// copy custom streams, write settings, then ZipUtils.zip(dir, zipFile)
~~~

Keep Jackson/Kotlin and archive-relative safe names, but replace generic
exported-key/suffix remapping with a versioned manifest containing the complete
language snapshot and {profileId, archivePath} records. Whitelist only language
settings and custom layout documents; exclude typed text and NLP data.

**Current import order to replace** (lines 80-110):

~~~kotlin
ZipUtils.unzip(zipFile, dstDir)
val keys = objectMapper.readValue<Map<String, Any?>>(File(dstDir, "settings.json"))
dstDir.copyRecursively(context.filesDir, overwrite = true)
// suffix matching then prefs.exportedKeys = keys
~~~

Stage and validate the complete archive first, copy accepted documents to
app-owned restore storage, build explicit profileId → new sourceUri mappings,
normalize, and replace one aggregate snapshot. Missing documents warn while
preserving remaining order; never derive restored IDs from new URIs.

### ZipUtils.kt (utility, file-I/O transform)

**Analog:** same file, lines 37-61 (tracked).

~~~kotlin
fun unzip(srcFile: File, dstDir: File) {
    require(srcFile.exists() && srcFile.isFile)
    dstDir.mkdirs()
    ZipFile(srcFile).use { zip ->
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryFile = File(dstDir, entry.name)
            if (entry.isDirectory) entryFile.mkdir()
            else zip.copy(entry, entryFile)
        }
    }
}
~~~

Retain the small object API, but resolve each entry under a canonical staging
root and reject absolute/.. escapes before opening output. Add bounded entry
count/bytes and cleanup on failure; tests cover traversal and no outside-root
writes.

### LanguageProfile.kt (model, transform)

**Analog:** tracked ime/layout/Layout.kt, lines 53-58, 194-255.

~~~kotlin
interface Layout<T> {
    val path: T
    fun inputStream(context: Context): Either<LayoutError, InputStream>
    fun md5(context: Context): Option<String>
    fun defaultName(context: Context): String
}

data class EmbeddedLayout(override val path: String) : Layout<String>
data class CustomLayout(override val path: Uri) : Layout<Uri>
~~~

Model LanguageProfile above Layout<*> with immutable id, discriminated
embedded/custom ref, display name, nullable locale, and enabled state.
LanguageConfig preserves List order and separate primaryProfileId and
activeProfileId; keep last-valid active if restore needs it. Separate immutable
ID from mutable custom sourceUri so restore changes only the URI. Metadata edits
must not affect ID, order, references, or deduplication.

### LanguageConfigSerDe.kt (codec, transform)

**Analogs:** tracked Layout.kt lines 158-191 and BackupManager.kt lines 36-37.

**Preference serde contract** (tracked PreferenceSerDe.kt, lines 6-10):

~~~kotlin
interface PreferenceSerDe<V : Any> {
    fun serialize(editor: SharedPreferences.Editor, key: String, value: V)
    fun deserialize(sharedPreferences: SharedPreferences, key: String, default: V): V
    fun deserialize(value: Any?): V?
}
~~~

**Discriminated layout serde** (tracked Layout.kt, lines 158-191):

~~~kotlin
val encodedValue = when (value) {
    is EmbeddedLayout -> "e" + value.path
    is CustomLayout -> "c" + value.path
    else -> null
}
editor.putString(key, encodedValue)
~~~

Use existing Jackson JsonMapper for a versioned aggregate JSON string. Preserve
ordered arrays and nullable metadata. Distinguish missing aggregate key from a
present malformed value: missing invokes legacy bootstrap; malformed repairs to
embedded en and does not resurrect legacy state.

### LanguageConfigNormalizer.kt (domain policy, transform)

**Analog:** tracked AvailableLayouts.kt, lines 56-137 and 219-269.

**Cleanup/fallback shape** (lines 56-72, 115-137):

~~~kotlin
private fun removeCustomLayout(path: String) {
    // remove registry entry and history path
    if (isCustomPath(prefs.layout.previousValid.get(), path)) {
        prefs.layout.previousValid.set(prefs.layout.previousValid.default)
    }
}

private fun restorePreviousValidOrDefault() {
    // load previous; if invalid remove custom previous and use current.default
}
~~~

Implement a pure normalizer/transition policy: dedupe IDs and source URIs,
retain persisted list order, keep at least one valid enabled profile, and ensure
primary/active are enabled valid members. Use operation-specific fallbacks:
disable/remove active → valid primary; active-primary → next remaining
persisted profile deterministically; broken active URI → last-valid active then
embedded en; final enabled toggle → reject without a write.

### LanguageMigration.kt (domain utility, batch transform)

**Analogs:** tracked AppPrefs.kt lines 61-79, 283-398 and PreferenceModel.kt
lines 240-283.

**Legacy inputs** (AppPrefs.kt, lines 61-79):

~~~kotlin
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
val history = stringSet(
    key = "prefs_layout_custom_history", default = emptySet()
)
~~~

**Per-entry migration limitation** (PreferenceModel.kt, lines 240-283):

~~~kotlin
val prefsEntries = sharedPreferences.all.entries.toSet()
prefsEntries.forEach { entry ->
    (oldVersion until version).forEach { fromVersion ->
        val migrationResult = migrate(fromVersion, PreferenceMigrationEntry(...))
        // transform/reset this entry, then serialize it
    }
}
~~~

Build the aggregate only when its key is truly absent: current first, valid
custom history next in deterministic order (StringSet has no order contract),
and active/primary from valid legacy current. Prune stale custom URIs under
Phase 1 rules. Aggregate presence is the idempotence guard; a malformed
present aggregate is repaired, not re-migrated from old keys.

### LanguageManager.kt (service, CRUD + event-driven publish)

**Analogs:** tracked AvailableLayouts.kt lines 150-217 and VIM8Application.kt
lines 29-49.

**Validate/load before mutation** (AvailableLayouts.kt, lines 150-169):

~~~kotlin
return layout.loadKeyboardData(layoutLoader, context).flatMap { keyboardData ->
    if (keyboardData.totalLayers == 0) emptyLayoutError().left()
    else {
        // upsert, remember previous valid, update history, activate
        layout.right()
    }
}
~~~

Expose the sole mutation API: import, enable/disable, reorder, setPrimary,
selectActive, metadata edit, remove, and restore. Each command reads the
aggregate, loads/validates required layouts, computes/normalizes the complete
next snapshot, persists once, and emits one non-null resolved session. Live
import deduplicates by canonical URI and reuses a restored profile ID.
External grant/cache cleanup is idempotent after the valid transition. Register
the manager using the VIM8Application lazy-service pattern.

## Test Pattern Assignments

### LanguageConfigSerDeSpec.kt (new)

**Analog:** tracked PreferenceSerDeSpec.kt, lines 18-77, 80-162.

~~~kotlin
class PreferenceSerDeSpec : FunSpec({
    context("Serializing") {
        withData(nameFn = { it.first::class.simpleName.orEmpty() }, data) { ... }
    }
    context("Deserializing value") {
        serde.deserialize(null).shouldBeNull()
        serde.deserialize(Exception("wrong type")).shouldBeNull()
    }
})
~~~

Use FunSpec/context/withData for ordered round-trip, nullable locale,
embedded/custom refs, schema version, malformed-present versus missing,
duplicate IDs, and unsupported future schema. Assert List order exactly.

### LanguageMigrationSpec.kt (new)

**Analog:** tracked PreferenceModelSpec.kt, lines 27-121, 312-334.

~~~kotlin
val sharedData = hashMapOf<String, Any?>()
beforeTest { pref = TestModel(); sharedData.clear() }
"Initializing" When {
    "migrating" should {
        // seed legacy entries, initialize, assert transformed sharedData
    }
}
~~~

Reuse MockK SharedPreferences/Editor state-backed seams and Kotest nested
contexts. Cover fresh en, embedded/custom current, valid history, stale
active/inactive URI, deterministic history order, and repeated initialization
without resurrecting legacy state.

### LanguageManagerSpec.kt (new)

**Analog:** tracked AvailableLayoutsSpec.kt, lines 44-109, 157-294.

~~~kotlin
beforeSpec {
    mockkStatic(::appPreferenceModel)
    // provide mutable PreferenceData fakes
}

"Import a custom layout" should {
    "validate before adding and activate it" {
        availableLayouts.importLayout(customLayout).shouldBeRight(customLayout)
        historyValue shouldBe linkedSetOf(uri)
        currentValue shouldBe customLayout
    }
}
~~~

Prefer pure transition tests over platform mocks. Add invariant/property cases
for reorder, metadata-only edits, separate primary/active, final-enabled
rejection, active-primary deterministic promotion, same-URI ID reuse, restore
ID/URI remap, broken URI precedence, cleanup calls, and exactly one resolved
session emission per successful command.

### BackupManagerSpec.kt (extend)

**Analog:** same tracked file, lines 50-225.

~~~kotlin
beforeSpec {
    mockkStatic(ContentResolver::readToFile)
    mockkObject(ZipUtils)
    mockkStatic(::appPreferenceModel)
}
context("Export") { /* inspect settings.json and staged custom files */ }
context("Import") { /* seed settings.json, call manager.import */ }
~~~

Keep temp cacheDir/filesDir, Jackson fixture parsing, and MockK static
boundaries. Extend assertions for complete profile manifest/privacy whitelist,
stable backup ID with new URI mapping, full replacement not merge, missing
document warning/order/fallback, legacy archive, malformed future schema, and
no broad copyRecursively before validation.

### CustomLayoutImportAdapterSpec.kt (extend)

**Analog:** same tracked file, lines 19-86.

~~~kotlin
test("takes only a persistable read grant before delegating one import") {
    justRun { contentResolver.takePersistableUriPermission(uri, readFlag) }
    every { availableLayouts.importLayout(CustomLayout(uri)) } returns imported.right()
    result shouldBe CustomLayoutImportResult.Success(imported)
}
~~~

Retain success/domain/provider failure tests. Add new-grant release assertion
when validation rejects a URI; verify no second manager mutation and no external
file deletion.

### ZipUtilsSpec.kt (extend)

**Analog:** same tracked file, lines 12-57.

~~~kotlin
class ZipUtilsSpec : FunSpec({
    beforeTest { inputDir = Files.createTempDirectory("8vim_zip").toFile() }
    test("Zip and unzip") {
        ZipUtils.zip(inputDir!!, zipFile!!)
        ZipUtils.unzip(zipFile!!, outputDir!!)
    }
})
~~~

Keep property-based round-trip coverage and add absolute/../ traversal,
entry-count/size bound, cleanup, and no-outside-root assertions.

### AvailableLayoutsSpec.kt (extend if registry/loader remains)

**Analog:** same tracked file, lines 188-294 and 296-366.

~~~kotlin
"reject a new invalid URI without mutating current or history" {
    availableLayouts.importLayout(customLayout).shouldBeLeft(error)
    historyValue shouldBe emptySet()
    currentValue shouldBe embeddedLayouts.first().first
}
~~~

Preserve/adjust these Phase 1 invariants when AvailableLayouts becomes a
manager-owned validator. Add same-URI reuse, distinct URI/same bytes, stale
inactive versus active fallback, and cache/history cleanup assertions.

### LanguageProfilesScreenTest.kt (new instrumentation)

**Analog:** tracked SettingsScreenTest.kt, lines 12-21.

~~~kotlin
@RunWith(KotestRunnerAndroid::class)
class SettingsScreenTest : FunSpec({
    test("test") {
        val compose = createAndroidComposeRule<MainActivity>()
        compose.onNodeWithText(/* localized label */).performClick()
    }
})
~~~

Use the same Android Kotest runner and Compose rule. Assert semantics rather
than implementation details: unified list, switch role/state, disabled final
toggle with explanation, enabled-profile radio group, accessible reorder/
delete/edit labels, explicit dialog confirm/cancel, persistence after restart,
and warning rendering.

## Shared Patterns

### S1. One aggregate preference write

**Sources:** PreferenceData.kt:24-43, PreferenceModel.kt:207-224,
AppPrefs.kt:61-79.

~~~kotlin
final override fun set(value: V, sync: Boolean) {
    cachedValue.set(value)
    if (sync) {
        val editor = model.sharedPreferences.edit()
        serde.serialize(editor, key, value)
        editor.apply()
    }
}
~~~

Use one JSON PreferenceData<LanguageConfig> so list/order/primary/active
cannot be observed torn across keys. Manager normalizes before this one write;
UI and IME only observe.

### S2. Arrow Either at fallible boundaries

**Sources:** Layout.kt:134-152, AvailableLayouts.kt:150-170.

~~~kotlin
fun <T> Layout<T>.loadKeyboardData(...): Either<LayoutError, KeyboardData> =
    inputStream(context)
        .flatMap { it.readSnapshot() }
        .flatMap { snapshot -> /* cache/load/parse */ }
~~~

Use typed Either/Option for layout validation, SAF failures, archive warnings,
and missing documents. Do not publish a profile until data is loaded and
validated.

### S3. Identity, ordering, and stale cleanup

**Sources:** AvailableLayouts.kt:42-54, 89-109, 219-269; Layout.kt:158-191.

Compare custom layouts by canonical URI string; digest is cache identity only.
Persist profiles as a JSON List, never StringSet. On removal/stale URI, clear
profile/history/cache metadata and permission as applicable, while leaving the
external source untouched.

### S4. Application ownership and observation

**Sources:** VIM8Application.kt:29-49, 71-93; MainActivity.kt:42-83;
Vim8ImeService.kt:95-115.

Construct shared services from VIM8Application and expose Context accessors.
Replace Activity weak globals and raw current observers with the one manager's
state/session flow. The IME swaps non-null keyboardData inside its existing view.

### S5. Compose settings/accessibility

**Sources:** Screen.kt:110-128, SwitchPreference.kt:31-50,
Dialog.kt:61-99, TextReplacementScreen.kt:70-141, 176-214.

Use Screen/PreferenceGroup and a plain Column; row-level
toggleable(Role.Switch) with passive trailing Switch; whole-row
selectable(Role.RadioButton) with passive RadioButton(onClick = null); local
dialog state until explicit confirm; localized content descriptions for all
icon-only actions. Disable boundary reorder and final-enabled toggle with
supporting text.

### S6. Staged backup and safe extraction

**Sources:** BackupManager.kt:39-78, 80-112; ZipUtils.kt:37-61.

Export known language data and documents with Jackson and archive-relative names.
On restore: stage, enforce canonical containment/resource bounds, parse and
validate metadata, copy accepted documents, build explicit ID→new-URI remaps,
normalize, commit one replacement snapshot, then clean unreferenced app-owned
files. Never promote the whole staging tree before semantic validation.

### S7. Kotest/MockK test structure

**Sources:** PreferenceModelSpec.kt:27-86, AvailableLayoutsSpec.kt:44-109,
BackupManagerSpec.kt:50-128.

Use FunSpec/WordSpec, nested contexts, beforeSpec for static/platform seams,
beforeTest for mutable fixtures, property tests for serde/ZIP round trips, and
MockK only at Android/SAF/SharedPreferences boundaries. Assert state snapshots
and call counts rather than hidden test seams.

## No Analog Found

None. LanguageManager and its pure normalizer/migration have no exact existing
class, but the role-match analogs cover state transitions, fallback,
persistence, and ownership. Planner should use research sketches for new
domain APIs while copying the cited mechanics.

## Metadata

**Analog search scope:** 8vim/src/main/kotlin/inc/flide/vim8/{app,datastore,ime,lib};
8vim/src/test/kotlin/inc/flide/vim8; 8vim/src/androidTest/kotlin/inc/flide/vim8.
**Files scanned:** tracked AppPrefs, datastore model/serde, layout loader and
registry, application/activity/IME, settings/Compose primitives, backup/ZIP,
and corresponding Kotest/Compose specs.
**Project instructions:** no repository AGENTS.md exists; task-supplied TDD and
no-hidden-test-seam constraints were preserved.
**Tracked-source gate:** every analog path named here was verified with
git ls-files -- path and is tracked source, not a gitignored mirror.
**Pattern extraction date:** 2026-09-18
