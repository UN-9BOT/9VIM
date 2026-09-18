package inc.flide.vim8.ime.language

import arrow.core.left
import arrow.core.right
import inc.flide.vim8.datastore.model.PreferenceData
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class LanguageManagerSpec : DescribeSpec({
    val en = LanguageProfile.embedded("en")
    val ru = LanguageProfile.embedded("ru")
    val lv = LanguageProfile.embedded("lv")

    fun keyboard() = KeyboardData(characterSets = listOf(listOf(null)))

    describe("language session tracer") {
        it("boots with only embedded en enabled and publishes a resolved session") {
            val preference = mockk<PreferenceData<LanguageConfig>>(relaxed = true)
            val catalog = object : LanguageLayoutCatalog {
                override fun embeddedProfiles() = listOf(ru, en, lv)
                override fun load(profile: LanguageProfile) = keyboard().right()
            }

            val manager = LanguageManager(preference, catalog, persistedConfig = null)

            manager.config.value.profiles.map { it.id } shouldContainExactly
                listOf(en.id, lv.id, ru.id)
            manager.config.value.profiles.filter { it.enabled }.map { it.id } shouldBe
                listOf(en.id)
            manager.config.value.primaryProfileId shouldBe en.id
            manager.config.value.activeProfileId shouldBe en.id
            manager.config.value.lastValidProfileId shouldBe en.id
            manager.session.value?.profile?.id shouldBe en.id
            verify(exactly = 1) { preference.set(manager.config.value) }
        }

        it("loads before one snapshot write and leaves state unchanged after a failed load") {
            val preference = mockk<PreferenceData<LanguageConfig>>(relaxed = true)
            val failed = mutableSetOf<String>()
            val catalog = object : LanguageLayoutCatalog {
                override fun embeddedProfiles() = listOf(en, ru, lv)
                override fun load(profile: LanguageProfile) = if (profile.id in failed) {
                    ExceptionWrapperError(IllegalStateException("unavailable")).left()
                } else {
                    keyboard().right()
                }
            }
            val initial = LanguageConfig.fresh(listOf(en, ru, lv)).copy(
                profiles = listOf(en.copy(enabled = true), ru.copy(enabled = true), lv)
            )
            val manager = LanguageManager(preference, catalog, persistedConfig = initial)
            val bootConfig = manager.config.value
            val bootSession = manager.session.value

            manager.selectActive(ru.id).isRight() shouldBe true
            manager.config.value.activeProfileId shouldBe ru.id
            manager.config.value.lastValidProfileId shouldBe ru.id
            manager.session.value?.profile?.id shouldBe ru.id
            verify(exactly = 1) { preference.set(manager.config.value) }

            val selectedConfig = manager.config.value
            val selectedSession = manager.session.value
            failed += en.id
            manager.selectActive(en.id).isLeft() shouldBe true
            manager.config.value shouldBe selectedConfig
            manager.session.value shouldBe selectedSession
            verify(exactly = 1) { preference.set(any()) }
            bootConfig shouldBe initial
            bootSession?.profile?.id shouldBe en.id
        }
    }
})
