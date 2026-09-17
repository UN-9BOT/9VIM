# Context Intel

## Project goal
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- note: Сделать из 8VIM полноценную многоязычную клавиатуру для ежедневного использования: быстро переключать языки, работать со встроенными и пользовательскими раскладками, получать подсказки и исправления опечаток, сохраняя быстрый жестовый ввод.
- note: Продуктовая последовательность в источнике: стабильная база → модель языков и раскладок → обычное переключение → жестовое переключение → language-aware autocomplete/autocorrect → per-app memory и экспериментальные режимы.

## Existing upstream capabilities
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- note: Уже есть Android IME service/lifecycle, gesture recognition и XPad/8VIM input engine, YAML/custom layout parser, AvailableLayouts для embedded и custom layouts, Compose UI/settings infrastructure, CustomKeycode, suggestion bar с тремя gesture-selectable slots, локальная word-frequency DB, Text Replacement и unit tests/CI.
- note: Текущие узкие места: AppPrefs.Layout хранит один layout.current; SuggestionsManager работает с одной глобальной WordFrequencyRepository, prefix completion и глобальным next-word ranking.
- note: Upstream reference files перечислены в PRD: AppPrefs.kt, Layout.kt, AvailableLayouts.kt, Vim8ImeService.kt, EditorInstance.kt, KeyboardManager.kt, CustomKeycode.kt, SuggestionsManager.kt, WordFrequencyRepository.kt, LayerLevel.kt.

## Architecture invariants
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- note: Язык — отдельное состояние над layout engine, а не LayerLevel внутри YAML-раскладки; LayerLevel предназначен для символов/действий одной раскладки.
- note: UI, gesture и per-app restore должны вызывать один LanguageManager; прямые записи старого language/layout state из нескольких мест не допускаются.
- note: Explicit active language является источником истины для autocomplete/autocorrect; automatic detection может быть только дополнительным ranking signal.
- note: Custom layouts — first-class citizen; custom profile может иметь optional user-selected locale для dictionary features.

## Privacy, licensing, and delivery principles
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- note: Core smart text должен быть offline-first; typed text не отправляется во внешние сервисы.
- note: Text Replacement — deterministic user configuration, autocorrect — probabilistic engine; системы не следует смешивать.
- note: Для сторонних dictionary/assets/code нужно отдельно проверять лицензии; GPL/source-available материалы нельзя копировать в Apache-2.0 fork без явного решения.
- note: Каждый feature PR должен включать сборку и существующие tests, unit tests для domain logic, migration/fallback tests при изменении storage, отсутствие hidden direct writes, privacy-проверки для text features, отсутствие network dependency, custom-layout compatibility, manual IME scenarios, корректное backup/reset preferences и license attribution.

## Product milestones and sequencing
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- note: Milestone A Stable Fork = Этап 0; B Multilingual MVP = Этапы 1–4; C Context-aware Multilingual = Этапы 5–7; D Gboard-class basic smart text = Этапы 8–9; E 8VIM-specific multilingual UX = Этапы 10–11.
- note: Рекомендованный порядок PR: baseline, LanguageProfile/migration/manager, Settings UI, next/previous keycode/sidebar, gesture, per-app memory, language-aware suggestions/storage, completion, typo suggestions, automatic correction/undo, bigram prediction, temporary mode, bilingual suggestions, release hardening.
- note: При сокращении scope не сокращать Этапы 1–4; если остаётся одна smart-text задача, сначала делать language-aware autocomplete/completion; autocorrect не начинать до стабилизации active language и per-language storage.
