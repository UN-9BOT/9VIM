package inc.flide.vim8.ime.language

import inc.flide.vim8.ime.layout.models.KeyboardData

sealed interface LanguageSource {
    data class Embedded(val layoutId: String) : LanguageSource

    data class Custom(val sourceUri: String) : LanguageSource
}

data class LanguageProfile(
    val id: String,
    val source: LanguageSource,
    val displayName: String? = null,
    val localeTag: String? = null,
    val enabled: Boolean = false
) {
    companion object {
        fun embedded(layoutId: String): LanguageProfile = LanguageProfile(
            id = "embedded:$layoutId",
            source = LanguageSource.Embedded(layoutId)
        )

        fun custom(sourceUri: String): LanguageProfile = LanguageProfile(
            id = "custom:$sourceUri",
            source = LanguageSource.Custom(sourceUri)
        )
    }
}

data class LanguageConfig(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val profiles: List<LanguageProfile>,
    val primaryProfileId: String,
    val activeProfileId: String,
    val lastValidProfileId: String?
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fresh(embeddedProfiles: List<LanguageProfile>): LanguageConfig {
            val ordered = embeddedProfiles
                .distinctBy(LanguageProfile::id)
                .sortedWith(compareBy<LanguageProfile>({ it.id != "embedded:en" }, { it.id }))
                .map { it.copy(enabled = it.id == "embedded:en") }
            return LanguageConfig(
                profiles = ordered,
                primaryProfileId = "embedded:en",
                activeProfileId = "embedded:en",
                lastValidProfileId = "embedded:en"
            )
        }
    }
}

data class ResolvedLanguageSession(
    val profile: LanguageProfile,
    val keyboardData: KeyboardData
)
