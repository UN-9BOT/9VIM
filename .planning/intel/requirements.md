# Requirements Intel

## REQ-stable-fork-baseline
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Зафиксировать стабильную baseline-версию форка: baseline/tag upstream master, CI с unit-тестами, lint, ktlint/checkstyle (если остаются) и debug APK build; воспроизвести или перенести PR #614 и PR #604, проверить PR #622 после device smoke-test, реализовать override semantics из PR #553 явно и воспроизвести Enter/newline bug из issue #568.
- acceptance: Clean checkout форка собирается одной командой в CI; все unit tests зелёные; custom layout picker работает на актуальном Android; есть baseline tag/commit для сравнения будущих regressions; нет массового merge старых Dependabot PR.
- scope: Этап 0, стабильная база форка, CI, upstream fixes, Enter/newline regression

## REQ-language-profiles
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Ввести отдельную domain-модель LanguageProfile над существующим Layout<*> с равноправными embedded и custom layouts, стабильным profile ID, optional localeTag, явным порядком, active language ID и безопасным fallback; добавить enabledLanguageProfiles и activeLanguageId, миграцию prefs.layout.current без потери настроек, единый LanguageManager/LayoutSessionManager и наблюдение active language/layout state в Vim8ImeService.
- acceptance: Можно включить минимум 2 embedded layouts и минимум 1 custom layout одновременно; после restart сохраняются список, порядок и активный язык; старый пользователь после upgrade сохраняет прежний layout активным и не теряет custom layouts; нельзя оставить систему без единственного валидного enabled layout; программное переключение layout не создаёт visible keyboard flicker.
- scope: Этап 1, модель нескольких языков, language profiles, preferences migration

## REQ-language-settings
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Дать пользователю экран multi-layout management со списками embedded/custom layouts, включением языка, изменением порядка, выбором default/primary language, optional locale для custom layout и безопасным удалением custom layout без dangling activeLanguageId.
- acceptance: Пользователь может настроить EN + RU + custom LV; порядок EN → RU → LV сохраняется; custom layout участвует в переключении как embedded; настройки переживают restart и backup/restore, если эти preferences экспортируются.
- scope: Этап 2, настройки языков и раскладок

## REQ-quick-language-switch
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Добавить надёжный обычный переключатель языка как fallback через CustomKeycode и KeyboardManager.handleKeyCode(...) → LanguageManager, с командами SWITCH_LANGUAGE_NEXT и SWITCH_LANGUAGE_PREVIOUS, sidebar control, индикатором активного кода, haptic feedback и accessibility label; не смешивать его с переключением на другую Android IME из issue #437.
- acceptance: Одно действие циклически переключает EN → RU → LV → EN; previous-language возвращает назад; активный язык визуально понятен; input engine и текущий текст не сбрасываются; переключение не добавляет символ и не посылает внешнему приложению fake keycode.
- scope: Этап 3, обычное быстрое переключение языка внутри 8VIM

## REQ-gesture-language-switch
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Добавить gesture language switching через существующую action/custom-keycode pipeline: configurable gesture для next/previous, default gesture только без конфликтов с layouts, а radial language selector оставить отдельной UX-задачей после стабилизации next/previous.
- acceptance: Gesture можно отключить или переназначить; gesture не делает обычные символы недоступными; custom layouts не ломаются из-за зарезервированной скрытой sequence; next/previous gesture и sidebar control используют один LanguageManager.
- scope: Этап 4, жестовое переключение языка

## REQ-per-app-language-memory
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Запоминать languageProfileId отдельно для каждого приложения по EditorInfo.packageName, с feature toggle Remember language per app, хранить только package name и language ID, восстанавливать язык в onStartInputView до/при подготовке keyboard view и обрабатывать stale mappings и backup без введённого текста.
- acceptance: После выбора RU в Telegram и EN в GitHub повторный вход в соответствующее text field восстанавливает правильный язык; mapping локальный и не требует network permission; удаление языка не ломает открытие IME.
- scope: Этап 5, per-app language memory

## REQ-language-aware-suggestions
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Переделать prediction layer на language-scoped Suggestion model и разделённые orchestration, sources и ranking: язык должен входить в frequency/history storage и seed words; сохранить 3 UI slots и gesture selection; оставить TextReplacementManager отдельным; отключать learning/prediction для PASSWORD input и не отправлять typed text во внешние сервисы.
- acceptance: RU frequency/history не влияет на EN ranking; при switch language suggestion state очищается или пересчитывается; старый UI выбора 3 suggestions продолжает работать; password fields не обучают словарь и не показывают suggestions.
- scope: Этап 6, language-aware prediction layer, frequency/history storage, privacy

## REQ-dictionary-autocomplete
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Добавить deterministic offline dictionary completion отдельно для каждого поддерживаемого языка с ranking по prefix match, базовой и пользовательской частоте, active language и регистру первого символа; до подключения сторонних dictionary data проверить лицензии и не переносить GPL/source-available code или assets без решения по лицензированию.
- acceptance: Completion работает отдельно для каждого enabled языка; слово из RU dictionary не появляется в EN mode без специального bilingual режима; часто используемые пользователем слова поднимаются в ranking; регистр предложения корректно отражается в suggestion.
- scope: Этап 7, словарный autocomplete/completion

## REQ-typo-correction-suggestions
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: На стадии 8A добавить только correction suggestions: генерировать active-language candidates по Damerau-Levenshtein/edit distance с transposition/insertion/deletion/substitution и threshold, зависящим от длины слова; типизировать correction в suggestion bar как CORRECTION, а не просто строку.
- acceptance: Известная опечатка получает correction candidate; correction учитывает active language.
- scope: Этап 8A, correction suggestions

## REQ-automatic-correction-undo
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: На стадии 8B добавить automatic correction только при достаточно высокой confidence и на word boundary, с кратковременным состоянием последней коррекции (originalText, replacementText, boundary, cursor position/session); не применять aggressive autocorrect в password/visible-password, URL/email/code-like и custom/no-dictionary contexts; не смешивать autocorrect с TextReplacementManager.
- acceptance: Auto-replacement можно отключить отдельно от suggestion display; immediate undo возвращает исходное слово; correction не срабатывает в password fields.
- scope: Этап 8B, automatic correction и undo contract

## REQ-next-word-prediction
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Добавить offline per-language statistical next-word prediction на bigram frequency (trigram позже при необходимости), с большим весом user history, fallback на частотные слова при отсутствии context model, language-scoped storage и ограничением размера/cleanup strategy; getTopWords() считать fallback, а не настоящим context prediction.
- acceptance: thank может повышать вероятность you относительно глобально частого случайного слова; EN и RU n-grams не смешиваются; storage имеет понятный лимит/cleanup strategy; feature полностью работает offline.
- scope: Этап 9, next-word prediction

## REQ-one-word-temporary-language
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Реализовать one-word temporary mode с baseLanguageId, temporaryLanguageId и temporaryMode: специальный gesture временно выбирает соседний язык, на word boundary (space, punctuation, enter или configurable language-aware logic) возвращает base language; cancel/back возвращает base без ввода; temporary state визуально отличается.
- acceptance: RU → one-word EN → автоматический RU работает без второго manual switch; cancel gesture/back action возвращает base language без ввода; prediction/autocorrect во временном режиме используют temporary language dictionary.
- scope: Этап 10, временный язык на одно слово

## REQ-bilingual-suggestions
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Опционально поддержать mixed-language suggestions после стабилизации explicit language selection: primary active language, optional secondary suggestion language, script detection как ranking signal и language ID у кандидатов; suggestion из secondary dictionary не обязан автоматически менять layout.
- acceptance:
- scope: Этап 11, bilingual/mixed-language suggestions

## REQ-release-hardening
- source: /home/unbot/code/opensource/9VIM/docs/prd/8vim_fork_implementation_brief_ru.md
- description: Подготовить публичный fork release через compatibility matrix, regression tests и release engineering: reproducible build, отдельный applicationId при необходимости параллельной установки, собственный signing key, changelog, attribution/Apache-2.0 notices, dictionary/assets license audit, versioning и release tags.
- acceptance:
- scope: Этап 12, release hardening и публичная версия
