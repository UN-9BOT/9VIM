package inc.flide.vim8.ime.language

data class LegacyLanguageState(
    val current: LanguageSource? = null,
    val customHistory: Set<String> = emptySet(),
    val previousValid: LanguageSource? = null
)

object LanguageMigration {
    fun migrate(
        legacy: LegacyLanguageState,
        embeddedProfiles: List<LanguageProfile>,
        isValid: (LanguageSource) -> Boolean
    ): LanguageConfig = LanguageConfig.fresh(embeddedProfiles)
}
