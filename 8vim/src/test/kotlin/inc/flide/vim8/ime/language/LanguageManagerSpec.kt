package inc.flide.vim8.ime.language

import arrow.core.left
import arrow.core.right
import inc.flide.vim8.datastore.model.PreferenceData
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
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

    describe("legacy migration") {
        it("keeps a valid custom current first and sorts valid history by canonical URI") {
            val currentUri = "content://layouts/z-current"
            val firstUri = "content://layouts/a-first"
            val staleUri = "content://layouts/m-stale"
            val migrated = LanguageMigration.migrate(
                legacy = LegacyLanguageState(
                    current = LanguageSource.Custom(currentUri),
                    customHistory = setOf(currentUri, staleUri, firstUri),
                    previousValid = LanguageSource.Embedded("ru")
                ),
                embeddedProfiles = listOf(ru, en, lv),
                isValid = { source ->
                    source !is LanguageSource.Custom || source.sourceUri != staleUri
                }
            )

            migrated.profiles.filter(LanguageProfile::enabled).map { it.id } shouldContainExactly
                listOf("custom:$currentUri", "custom:$firstUri")
            migrated.activeProfileId shouldBe "custom:$currentUri"
            migrated.primaryProfileId shouldBe "custom:$currentUri"
            migrated.lastValidProfileId shouldBe "custom:$currentUri"
            migrated.profiles.none { it.id == "custom:$staleUri" } shouldBe true
        }

        it("uses valid previous for stale active and embedded en when both are stale") {
            val stale = LanguageSource.Custom("content://layouts/stale")
            val previous = LanguageSource.Custom("content://layouts/previous")

            val restored = LanguageMigration.migrate(
                legacy = LegacyLanguageState(
                    current = stale,
                    customHistory = setOf(stale.sourceUri, previous.sourceUri),
                    previousValid = previous
                ),
                embeddedProfiles = listOf(en, ru, lv),
                isValid = { it == previous || it is LanguageSource.Embedded }
            )
            restored.activeProfileId shouldBe "custom:${previous.sourceUri}"
            restored.profiles.filter(LanguageProfile::enabled).map { it.id } shouldContainExactly
                listOf("custom:${previous.sourceUri}")

            val fallback = LanguageMigration.migrate(
                legacy = LegacyLanguageState(
                    current = stale,
                    customHistory = setOf(stale.sourceUri, previous.sourceUri),
                    previousValid = previous
                ),
                embeddedProfiles = listOf(ru, lv, en),
                isValid = { it is LanguageSource.Embedded }
            )
            fallback.activeProfileId shouldBe en.id
            fallback.primaryProfileId shouldBe en.id
            fallback.lastValidProfileId shouldBe en.id
            fallback.profiles.filter(LanguageProfile::enabled).map { it.id } shouldBe listOf(en.id)
        }

        it("rejects malformed and duplicate aggregate snapshots") {
            LanguageConfigSerDe.decode("{not-json").shouldBeNull()
            val duplicate = LanguageConfig(
                profiles = listOf(en.copy(enabled = true), en.copy(enabled = true)),
                primaryProfileId = en.id,
                activeProfileId = en.id,
                lastValidProfileId = en.id
            )
            LanguageConfigSerDe.decode(LanguageConfigSerDe.encode(duplicate)).shouldBeNull()
        }

        it("uses legacy only when the aggregate key is truly absent") {
            val preference = mockk<PreferenceData<LanguageConfig>>(relaxed = true)
            val custom = LanguageSource.Custom("content://layouts/current")
            val catalog = object : LanguageLayoutCatalog {
                override fun embeddedProfiles() = listOf(en, ru, lv)
                override fun load(profile: LanguageProfile) = keyboard().right()
            }
            val absent = LanguageManager(
                preference,
                catalog,
                LanguageBootstrapState(
                    aggregatePresent = false,
                    aggregate = null,
                    legacy = LegacyLanguageState(
                        current = custom,
                        customHistory = setOf(custom.sourceUri)
                    )
                )
            )
            absent.config.value.activeProfileId shouldBe "custom:${custom.sourceUri}"
            verify(exactly = 1) { preference.set(absent.config.value) }

            clearMocks(preference, answers = false, recordedCalls = true)
            val malformedPresent = LanguageManager(
                preference,
                catalog,
                LanguageBootstrapState(
                    aggregatePresent = true,
                    aggregate = null,
                    legacy = LegacyLanguageState(
                        current = custom,
                        customHistory = setOf(custom.sourceUri)
                    )
                )
            )
            malformedPresent.config.value.activeProfileId shouldBe en.id
            val resurrected = malformedPresent.config.value.profiles.any {
                it.id == "custom:${custom.sourceUri}"
            }
            resurrected shouldBe false
            verify(exactly = 1) { preference.set(malformedPresent.config.value) }
        }

        it("does not rewrite or resurrect legacy state after a valid aggregate restart") {
            val preference = mockk<PreferenceData<LanguageConfig>>(relaxed = true)
            val catalog = object : LanguageLayoutCatalog {
                override fun embeddedProfiles() = listOf(en, ru, lv)
                override fun load(profile: LanguageProfile) = keyboard().right()
            }
            val aggregate = LanguageConfig.fresh(listOf(en, ru, lv))

            val manager = LanguageManager(
                preference,
                catalog,
                LanguageBootstrapState(
                    aggregatePresent = true,
                    aggregate = aggregate,
                    legacy = LegacyLanguageState(
                        current = LanguageSource.Custom("content://layouts/ignored")
                    )
                )
            )

            manager.config.value shouldBe aggregate
            verify(exactly = 0) { preference.set(any()) }
        }
    }
})
