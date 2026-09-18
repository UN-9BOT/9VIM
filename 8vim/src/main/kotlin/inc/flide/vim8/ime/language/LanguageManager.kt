package inc.flide.vim8.ime.language

import android.content.Context
import android.net.Uri
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import inc.flide.vim8.datastore.model.PreferenceData
import inc.flide.vim8.ime.layout.CustomLayout
import inc.flide.vim8.ime.layout.EmbeddedLayout
import inc.flide.vim8.ime.layout.Layout
import inc.flide.vim8.ime.layout.LayoutLoader
import inc.flide.vim8.ime.layout.embeddedLayouts
import inc.flide.vim8.ime.layout.loadKeyboardData
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.error.LayoutError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LanguageLayoutCatalog {
    fun embeddedProfiles(): List<LanguageProfile>
    fun load(profile: LanguageProfile): Either<LayoutError, KeyboardData>
}

class AndroidLanguageLayoutCatalog(
    private val layoutLoader: LayoutLoader,
    private val context: Context
) : LanguageLayoutCatalog {
    override fun embeddedProfiles(): List<LanguageProfile> =
        embeddedLayouts(layoutLoader, context).map { (layout, name) ->
            LanguageProfile.embedded(layout.path).copy(displayName = name)
        }

    override fun load(profile: LanguageProfile): Either<LayoutError, KeyboardData> =
        profile.source.toLayout().loadKeyboardData(layoutLoader, context)

    private fun LanguageSource.toLayout(): Layout<*> = when (this) {
        is LanguageSource.Embedded -> EmbeddedLayout(layoutId)
        is LanguageSource.Custom -> CustomLayout(Uri.parse(sourceUri))
    }
}

class LanguageManager(
    private val preference: PreferenceData<LanguageConfig>,
    private val catalog: LanguageLayoutCatalog,
    private val bootstrapState: LanguageBootstrapState
) {
    private val embeddedProfiles = catalog.embeddedProfiles()
    private val bootstrapCandidate = if (bootstrapState.aggregatePresent) {
        bootstrapState.aggregate ?: LanguageConfig.fresh(embeddedProfiles)
    } else {
        LanguageMigration.migrate(
            legacy = bootstrapState.legacy,
            embeddedProfiles = embeddedProfiles,
            isValid = ::isSourceValid
        )
    }
    private val initialConfig = reconcile(
        bootstrapCandidate
    )
    private val initialSession = resolveInitialSession(initialConfig)
    private val resolvedInitialConfig = initialSession?.let { session ->
        initialConfig.copy(
            activeProfileId = session.profile.id,
            lastValidProfileId = session.profile.id
        )
    } ?: initialConfig
    private val mutableConfig = MutableStateFlow(resolvedInitialConfig)
    private val mutableSession = MutableStateFlow<ResolvedLanguageSession?>(null)

    val config: StateFlow<LanguageConfig> = mutableConfig
    val session: StateFlow<ResolvedLanguageSession?> = mutableSession

    init {
        mutableSession.value = initialSession
        if (!bootstrapState.aggregatePresent || resolvedInitialConfig != bootstrapState.aggregate) {
            preference.set(resolvedInitialConfig)
        }
    }

    constructor(
        preference: PreferenceData<LanguageConfig>,
        catalog: LanguageLayoutCatalog,
        persistedConfig: LanguageConfig?
    ) : this(
        preference = preference,
        catalog = catalog,
        bootstrapState = LanguageBootstrapState(
            aggregatePresent = persistedConfig != null,
            aggregate = persistedConfig
        )
    )

    fun selectActive(profileId: String): Either<LayoutError, ResolvedLanguageSession> {
        val profile = mutableConfig.value.profiles.firstOrNull {
            it.id == profileId && it.enabled
        } ?: return languageError("Unknown or disabled language profile: $profileId").left()

        return loadValid(profile).map { keyboardData ->
            val session = ResolvedLanguageSession(profile, keyboardData)
            val nextConfig = mutableConfig.value.copy(
                activeProfileId = profile.id,
                lastValidProfileId = profile.id
            )
            preference.set(nextConfig)
            mutableConfig.value = nextConfig
            mutableSession.value = session
            session
        }
    }

    private fun reconcile(candidate: LanguageConfig): LanguageConfig {
        val distinct = candidate.profiles.distinctBy(LanguageProfile::id)
        val knownIds = distinct.mapTo(hashSetOf(), LanguageProfile::id)
        val additions = embeddedProfiles
            .filterNot { it.id in knownIds }
            .sortedBy(LanguageProfile::id)
            .map { it.copy(enabled = false) }
        return candidate.copy(profiles = distinct + additions)
    }

    private fun resolveInitialSession(candidate: LanguageConfig): ResolvedLanguageSession? {
        val enabled = candidate.profiles.filter(LanguageProfile::enabled)
        val preferredIds = listOfNotNull(
            candidate.activeProfileId,
            candidate.lastValidProfileId,
            "embedded:en"
        ).distinct()
        val ordered = preferredIds.mapNotNull { id -> enabled.firstOrNull { it.id == id } } +
            enabled.filterNot { it.id in preferredIds }
        return ordered.firstNotNullOfOrNull { profile ->
            loadValid(profile).getOrNull()?.let { ResolvedLanguageSession(profile, it) }
        }
    }

    private fun loadValid(profile: LanguageProfile): Either<LayoutError, KeyboardData> =
        catalog.load(profile).flatMap { keyboardData ->
            if (keyboardData.totalLayers == 0) {
                languageError("Language profile has no keyboard layers: ${profile.id}").left()
            } else {
                keyboardData.right()
            }
        }

    private fun isSourceValid(source: LanguageSource): Boolean {
        val profile = when (source) {
            is LanguageSource.Embedded -> embeddedProfiles.firstOrNull {
                it.source == source
            } ?: return false

            is LanguageSource.Custom -> LanguageProfile.custom(source.sourceUri)
        }
        return loadValid(profile).isRight()
    }

    private fun languageError(message: String): LayoutError =
        ExceptionWrapperError(IllegalArgumentException(message))
}
