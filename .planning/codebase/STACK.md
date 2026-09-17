---
last_mapped_commit: 03214b7c33a6e06c22ad0be43286d5e6c438fdd6
last_mapped_at: 2026-09-17
---
# Technology Stack

**Analysis Date:** 2026-09-17

## Languages

**Primary:**

- Kotlin 2.2.10 - Android application and input-method service in `8vim/src/main/kotlin/`
- Java 17 - Android/JVM compatibility and any Java sources under `8vim/src/`

**Secondary:**

- XML - Android manifest, resources, themes, and layouts in `8vim/src/main/res/`
- YAML/JSON - keyboard layouts and schemas in `8vim/src/main/res/raw/` and `8vim/src/main/resources/`

## Runtime

**Environment:**

- Android SDK, minSdk 24, targetSdk 35, compileSdk 36 - configured in `8vim/build.gradle.kts`
- JVM 17 - configured by `compileOptions` and Kotlin `JvmTarget.JVM_17` in `8vim/build.gradle.kts`

**Package Manager:**

- Gradle wrapper/version catalog - `gradlew`, `gradle/libs.versions.toml`
- Lockfile: Not detected

## Frameworks

**Core:**

- Android Gradle Plugin 9.1.0 - application packaging in `8vim/build.gradle.kts`
- AndroidX AppCompat/Core/Lifecycle/Preference - Android platform integration
- Jetpack Compose 1.10.4 and Material 3 1.4.0 - settings UI and theme
- Kotlin Coroutines (Android transitively) - IO work in `8vim/src/main/kotlin/inc/flide/vim8/VIM8Application.kt`

**Testing:**

- JUnit 5 via `de.mannodermaus.android-junit5` 1.13.1.0 and Kotest 5.9.1 - unit tests
- MockK 1.14.9 - test doubles
- JaCoCo - unit-test coverage configured in `8vim/build.gradle.kts`

**Build/Dev:**

- Kotlin Android, Compose compiler, KSP - `build.gradle.kts`
- ktlint 12.1.1 and Checkstyle - style checks in `8vim/build.gradle.kts` and `config/checkstyle/checkstyle.xml`

## Key Dependencies

**Critical:**

- Jackson 2.18.6 (Kotlin, YAML, CBOR) - layout parsing and backup/cache serialization
- Arrow 1.2.4 and Arrow Jackson integration 0.14.0 - functional data types and optics
- NetworkNT JSON Schema Validator 1.0.73 - validates imported YAML layouts
- Android Material 1.13.0, Compose UI/Material3 - UI components

**Infrastructure:**

- `com.github.tony19:logback-android` 3.0.0 and SLF4J 2.0.16 - Android logging
- Apache Commons Text 1.15.0 and Commons Codec 1.17.1 - text processing and hashing
- Mikepenz AboutLibraries 11.2.3 - third-party license screen
- Skydoves Colorpicker Compose 1.1.2 - color preference UI

## Configuration

**Environment:**

- Optional release signing variables are read by `8vim/build.gradle.kts` (`VIM8_BUILD_KEYSTORE_FILE` and related names); values are not committed here.
- Version metadata is read from `8vim/version.properties`.

**Build:**

- `settings.gradle.kts`, `build.gradle.kts`, `8vim/build.gradle.kts`
- `gradle/libs.versions.toml`, `gradle.properties`, `8vim/proguard-rules.pro`
- `config/checkstyle/checkstyle.xml`

## Platform Requirements

**Development:**

- Android SDK platforms/build tools supporting API 36 and JDK 17; use the repository Gradle wrapper.

**Production:**

- Android device API 24+; packaged as an Android application and IME service (`inc.flide.vi8`).

---

*Stack analysis: 2026-09-17*
