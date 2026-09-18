package inc.flide.vim8.ime.language

import android.content.SharedPreferences
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import inc.flide.vim8.datastore.model.PreferenceSerDe
import inc.flide.vim8.ime.layout.models.KeyboardData
import java.util.Locale

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

        fun custom(
            sourceUri: String,
            id: String = "custom:$sourceUri"
        ): LanguageProfile = LanguageProfile(
            id = id,
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

object LanguageConfigSerDe : PreferenceSerDe<LanguageConfig> {
    private data class ProfileDto(
        val id: String,
        val sourceType: String,
        val sourceValue: String,
        val displayName: String? = null,
        val localeTag: String? = null,
        val enabled: Boolean = false
    )

    private data class ConfigDto(
        val schemaVersion: Int,
        val profiles: List<ProfileDto>,
        val primaryProfileId: String,
        val activeProfileId: String,
        val lastValidProfileId: String? = null
    )

    private val mapper = JsonMapper.builder().build().registerKotlinModule()
    private val localePattern = Regex("^[A-Za-z]{2,8}([_-][A-Za-z0-9]{1,8})*$")

    override fun serialize(
        editor: SharedPreferences.Editor,
        key: String,
        value: LanguageConfig
    ) {
        editor.putString(key, encode(value))
    }

    override fun deserialize(
        sharedPreferences: SharedPreferences,
        key: String,
        default: LanguageConfig
    ): LanguageConfig = sharedPreferences.getString(key, null)
        ?.let(::decode)
        ?: default

    override fun deserialize(value: Any?): LanguageConfig? = (value as? String)?.let(::decode)

    fun encode(config: LanguageConfig): String = mapper.writeValueAsString(
        ConfigDto(
            schemaVersion = config.schemaVersion,
            profiles = config.profiles.map { profile ->
                val (type, source) = when (val profileSource = profile.source) {
                    is LanguageSource.Embedded -> "embedded" to profileSource.layoutId
                    is LanguageSource.Custom -> "custom" to profileSource.sourceUri
                }
                ProfileDto(
                    id = profile.id,
                    sourceType = type,
                    sourceValue = source,
                    displayName = profile.displayName,
                    localeTag = profile.localeTag,
                    enabled = profile.enabled
                )
            },
            primaryProfileId = config.primaryProfileId,
            activeProfileId = config.activeProfileId,
            lastValidProfileId = config.lastValidProfileId
        )
    )

    fun decode(value: String): LanguageConfig? = runCatching {
        val dto = mapper.readValue<ConfigDto>(value)
        require(dto.schemaVersion == LanguageConfig.CURRENT_SCHEMA_VERSION)
        require(dto.profiles.isNotEmpty())
        require(dto.profiles.map(ProfileDto::id).distinct().size == dto.profiles.size)
        val profiles = dto.profiles.map { profile ->
            require(profile.id.isNotBlank() && profile.sourceValue.isNotBlank())
            val localeTag = profile.localeTag?.trim()?.ifEmpty { null }
            require(localeTag == null || localePattern.matches(localeTag))
            val source = when (profile.sourceType) {
                "embedded" -> LanguageSource.Embedded(profile.sourceValue)
                "custom" -> LanguageSource.Custom(profile.sourceValue)
                else -> error("Unknown language source")
            }
            LanguageProfile(
                id = profile.id,
                source = source,
                displayName = profile.displayName,
                localeTag = localeTag?.let { Locale.forLanguageTag(it).toLanguageTag() },
                enabled = profile.enabled
            )
        }
        val ids = profiles.mapTo(hashSetOf(), LanguageProfile::id)
        require(dto.primaryProfileId in ids && dto.activeProfileId in ids)
        require(dto.lastValidProfileId == null || dto.lastValidProfileId in ids)
        LanguageConfig(
            schemaVersion = dto.schemaVersion,
            profiles = profiles,
            primaryProfileId = dto.primaryProfileId,
            activeProfileId = dto.activeProfileId,
            lastValidProfileId = dto.lastValidProfileId
        )
    }.getOrNull()
}
