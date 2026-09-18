package inc.flide.vim8.ime.language

import android.content.Context
import androidx.preference.PreferenceManager
import inc.flide.vim8.AppPrefs
import inc.flide.vim8.ime.layout.CustomLayout
import inc.flide.vim8.ime.layout.EmbeddedLayout
import inc.flide.vim8.ime.layout.Layout

data class LegacyLanguageState(
    val current: LanguageSource? = null,
    val customHistory: Set<String> = emptySet(),
    val previousValid: LanguageSource? = null
)

data class LanguageBootstrapState(
    val aggregatePresent: Boolean,
    val aggregate: LanguageConfig?,
    val legacy: LegacyLanguageState = LegacyLanguageState()
)

object LanguageBootstrapReader {
    fun read(context: Context, prefs: AppPrefs): LanguageBootstrapState {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val aggregatePresent = sharedPreferences.contains(prefs.language.config.key)
        val aggregate = sharedPreferences.getString(prefs.language.config.key, null)
            ?.let(LanguageConfigSerDe::decode)
        if (aggregatePresent) {
            return LanguageBootstrapState(aggregatePresent = true, aggregate = aggregate)
        }

        fun Layout<*>.toLanguageSource(): LanguageSource = when (this) {
            is EmbeddedLayout -> LanguageSource.Embedded(path)
            is CustomLayout -> LanguageSource.Custom(path.toString())
            else -> error("Unsupported legacy layout")
        }

        val current = prefs.layout.current
            .takeIf { sharedPreferences.contains(it.key) }
            ?.get()
            ?.toLanguageSource()
        val previous = prefs.layout.previousValid
            .takeIf { sharedPreferences.contains(it.key) }
            ?.get()
            ?.toLanguageSource()
        val history = prefs.layout.custom.history
            .takeIf { sharedPreferences.contains(it.key) }
            ?.get()
            .orEmpty()
        return LanguageBootstrapState(
            aggregatePresent = false,
            aggregate = null,
            legacy = LegacyLanguageState(current, history, previous)
        )
    }
}

object LanguageMigration {
    fun migrate(
        legacy: LegacyLanguageState,
        embeddedProfiles: List<LanguageProfile>,
        isValid: (LanguageSource) -> Boolean
    ): LanguageConfig {
        val embeddedBySource = embeddedProfiles.associateBy { it.source }
        val validCurrent = legacy.current?.takeIf { source ->
            source in embeddedBySource || source is LanguageSource.Custom && isValid(source)
        }
        val validPrevious = legacy.previousValid?.takeIf { source ->
            source in embeddedBySource || source is LanguageSource.Custom && isValid(source)
        }
        val activeSource = validCurrent ?: validPrevious ?: LanguageSource.Embedded("en")

        val validHistory = legacy.customHistory
            .asSequence()
            .map(LanguageSource::Custom)
            .filter(isValid)
            .sortedBy(LanguageSource.Custom::sourceUri)
            .toList()

        if (legacy.current == null && validHistory.isEmpty()) {
            return LanguageConfig.fresh(embeddedProfiles)
        }

        fun profile(source: LanguageSource): LanguageProfile = embeddedBySource[source]
            ?: when (source) {
                is LanguageSource.Custom -> LanguageProfile.custom(source.sourceUri)
                is LanguageSource.Embedded -> LanguageProfile.embedded(source.layoutId)
            }

        val enabledSources = buildList {
            add(activeSource)
            validHistory.filterTo(this) { it != activeSource }
        }.distinct()
        val enabledProfiles = enabledSources.map { profile(it).copy(enabled = true) }
        val enabledIds = enabledProfiles.mapTo(hashSetOf(), LanguageProfile::id)
        val disabledEmbedded = embeddedProfiles
            .filterNot { it.id in enabledIds }
            .distinctBy(LanguageProfile::id)
            .sortedWith(compareBy<LanguageProfile>({ it.id != "embedded:en" }, { it.id }))
            .map { it.copy(enabled = false) }
        val activeId = profile(activeSource).id
        return LanguageConfig(
            profiles = enabledProfiles + disabledEmbedded,
            primaryProfileId = activeId,
            activeProfileId = activeId,
            lastValidProfileId = activeId
        )
    }
}
