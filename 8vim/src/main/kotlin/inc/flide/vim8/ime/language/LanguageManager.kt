package inc.flide.vim8.ime.language

import arrow.core.Either
import arrow.core.left
import inc.flide.vim8.datastore.model.PreferenceData
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.error.LayoutError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LanguageLayoutCatalog {
    fun embeddedProfiles(): List<LanguageProfile>
    fun load(profile: LanguageProfile): Either<LayoutError, KeyboardData>
}

class LanguageManager(
    private val preference: PreferenceData<LanguageConfig>,
    private val catalog: LanguageLayoutCatalog,
    persistedConfig: LanguageConfig?
) {
    private val mutableConfig = MutableStateFlow(
        persistedConfig ?: LanguageConfig.fresh(listOf(LanguageProfile.embedded("en")))
    )
    private val mutableSession = MutableStateFlow<ResolvedLanguageSession?>(null)

    val config: StateFlow<LanguageConfig> = mutableConfig
    val session: StateFlow<ResolvedLanguageSession?> = mutableSession

    fun selectActive(profileId: String): Either<LayoutError, ResolvedLanguageSession> =
        ExceptionWrapperError(
            UnsupportedOperationException("Language selection is not implemented: $profileId")
        ).left()
}
