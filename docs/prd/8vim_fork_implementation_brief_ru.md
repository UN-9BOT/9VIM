# 8VIM Fork — план развития multilingual keyboard и smart text

> Рабочий implementation brief для агента, который будет развивать форк 8VIM.
>
> Исходная точка: текущий `master` upstream 8VIM уже изучен и форк создан. Документ задаёт **порядок работ, бизнес-смысл каждого этапа, технические ориентиры, критерии готовности и ссылки на upstream**.

## 1. Цель проекта

Сделать из 8VIM полноценную многоязычную клавиатуру, которой можно пользоваться как основной клавиатурой каждый день: быстро переключать языки, работать с несколькими встроенными и пользовательскими раскладками, получать подсказки слов и исправления опечаток, не теряя основную идею 8VIM — быстрый жестовый ввод.

Главная продуктовая последовательность:

1. Сначала стабилизировать форк и закрыть несколько очевидных upstream-проблем.
2. Затем сделать правильную модель нескольких языков и раскладок.
3. После этого добавить быстрый переключатель языка с надёжным обычным UX.
4. Затем добавить «8VIM-like» жестовое переключение.
5. Только после появления понятия активного языка переделывать autocomplete/autocorrect.
6. После базового smart text добавлять per-app memory и более экспериментальные режимы.

Ключевой принцип: **язык — это отдельное состояние над layout engine, а не ещё один LayerLevel внутри YAML-раскладки**. В проекте действительно есть до шести уровней раскладки, но эти уровни предназначены для символов/действий внутри одной раскладки. Использование их как языкового state усложнит dictionaries, prediction, custom layouts и настройки.

---

## 2. Что уже есть в upstream и что следует сохранить

Не переписывать приложение с нуля. В текущем коде уже есть хорошие базовые блоки:

- Android IME service и жизненный цикл клавиатуры;
- gesture recognition и XPad/8VIM input engine;
- YAML/custom layout parser;
- `AvailableLayouts`, который уже умеет перечислять embedded + custom layouts и переключать `prefs.layout.current`;
- Compose UI и settings infrastructure;
- `CustomKeycode` для внутренних действий клавиатуры;
- suggestion bar и три gesture-selectable suggestion slots;
- локальная word-frequency DB;
- Text Replacement (`omw` → `On my way!`);
- unit tests и CI.

Главная архитектурная проблема для multilingual: сейчас `AppPrefs.Layout` хранит **один** `layout.current`. Модель «набор включённых языков + активный язык + порядок переключения» отсутствует.

Главная архитектурная проблема smart text: текущий `SuggestionsManager` работает с одной глобальной `WordFrequencyRepository`; suggestions в основном представляют собой prefix completion, а «next word» — глобальные наиболее частые слова. Это нельзя масштабировать на несколько языков без переделки слоя данных.

### Важные файлы upstream

- [`AppPrefs.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/AppPrefs.kt) — preferences, сейчас один `layout.current`.
- [`Layout.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/layout/Layout.kt) — embedded/custom layout abstractions и загрузка.
- [`AvailableLayouts.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/layout/AvailableLayouts.kt) — список layouts и существующий `selectLayout(which)`.
- [`Vim8ImeService.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/Vim8ImeService.kt) — IME lifecycle; наблюдает `prefs.layout.current`; `onStartInputView(EditorInfo, ...)` — естественная точка для per-app language memory.
- [`EditorInstance.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/editor/EditorInstance.kt) — `EditorInfo`, `InputConnection`, editor actions.
- [`KeyboardManager.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManager.kt) — маршрутизация text/key/custom actions.
- [`CustomKeycode.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/CustomKeycode.kt) — внутренние команды (`SWITCH_TO_*`, suggestion selection и т.д.).
- [`SuggestionsManager.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/nlp/SuggestionsManager.kt) — текущий prediction orchestration.
- [`WordFrequencyRepository.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/nlp/WordFrequencyRepository.kt) — глобальная SQLite frequency DB.
- [`LayerLevel.kt`](https://github.com/8VIM/8VIM/blob/master/8vim/src/main/kotlin/inc/flide/vim8/ime/layout/models/LayerLevel.kt) — уровни внутри layout; не использовать как language state.

---

# Этап 0. Зафиксировать стабильную базу форка

## Бизнес-задача

Перед развитием новых возможностей сделать форк предсказуемым: он должен собираться, тестироваться и не содержать нескольких уже известных мелких поломок upstream.

## Зачем это нужно

Если одновременно менять фундамент языков, prediction и чинить старые баги, потом будет невозможно понять, какой change сломал клавиатуру. Нужна чистая baseline-версия, относительно которой все следующие изменения будут маленькими и проверяемыми.

## Что сделать технически

1. Зафиксировать текущий upstream `master` как baseline/tag в форке.
2. Убедиться, что CI запускает как минимум:
   - `testDebugUnitTest`;
   - lint;
   - ktlint/checkstyle, если они остаются частью текущего проекта;
   - debug APK build.
3. Перенести или самостоятельно воспроизвести небольшие полезные upstream fixes:
   - [PR #614 — Fix Dang File Picker](https://github.com/8VIM/8VIM/pull/614). Patch минимальный (`application/octet-stream` → `*/*`). После выбора файла полагаться на существующую parser validation, а не на MIME.
   - [PR #604 — reproducible build](https://github.com/8VIM/8VIM/pull/604). Убирает generated timestamp из AboutLibraries metadata.
4. Проверить [PR #622 — optional complex gesture reset mode](https://github.com/8VIM/8VIM/pull/622). Он небольшой, mergeable и содержит тесты. Переносить только после device smoke-test, потому что он меняет gesture semantics.
5. Не принимать как есть [PR #553](https://github.com/8VIM/8VIM/pull/553). Бизнес-проблема правильная — custom layout должен иметь понятную override semantics — но patch просто снимает конфликтную проверку и содержит лишний version rollback. Реализовать override явно и покрыть тестами.
6. Воспроизвести Enter/newline bug из [issue #568](https://github.com/8VIM/8VIM/issues/568) в Slack/Discord/других приложениях и зафиксировать ожидаемое поведение до feature work.

## Критерии готовности

- Clean checkout форка собирается одной командой в CI.
- Все unit tests зелёные.
- Custom layout picker работает на актуальном Android.
- Есть baseline tag/commit, от которого можно сравнивать будущие regressions.
- Нет массового merge старых Dependabot PR: зависимости обновляются отдельной контролируемой задачей, если это реально нужно.

---

# Этап 1. Ввести нормальную модель нескольких языков

## Бизнес-задача

Позволить пользователю заранее выбрать несколько языков/раскладок, между которыми клавиатура сможет мгновенно переключаться.

## Зачем это нужно

Сейчас 8VIM фактически предполагает один активный layout. Для человека, который пишет на русском и английском или использует три языка, это делает клавиатуру неудобной как основную. Кроме того, без понятия активного языка невозможно правильно реализовать словари, autocomplete и autocorrect.

Основной upstream context: [issue #308 — Proper language selection](https://github.com/8VIM/8VIM/issues/308).

## Что сделать технически

Ввести отдельную domain-модель над существующим `Layout<*>`.

Рекомендуемая концепция (точные имена классов можно адаптировать к стилю проекта):

```kotlin
data class LanguageProfile(
    val id: String,
    val layout: LayoutRef,
    val localeTag: String?,
    val displayName: String,
    val enabled: Boolean,
    val order: Int,
)
```

Требования к модели:

- embedded layout и custom layout должны быть равноправными;
- `id` должен быть стабильным между restart/backup/restore;
- у custom layout locale может быть неизвестен, поэтому `localeTag` должен быть optional и редактироваться пользователем;
- порядок languages должен сохраняться явно;
- нельзя хранить порядок через обычный `Set`;
- активный язык должен ссылаться на stable profile ID или эквивалентный устойчивый идентификатор;
- если layout исчез/сломался, должен быть безопасный fallback на первый enabled layout или English/default.

В preferences добавить состояние уровня:

```text
enabledLanguageProfiles
activeLanguageId
```

Текущий `prefs.layout.current` должен мигрировать в новую модель без потери настроек существующего пользователя. Если PreferenceModel требует schema version bump — сделать миграцию явно и покрыть тестами.

Создать сервис уровня `LanguageManager` / `LayoutSessionManager`, который является единственным местом для операций:

```text
getEnabledLanguages()
getActiveLanguage()
selectLanguage(id)
selectNextLanguage()
selectPreviousLanguage()
```

Не размазывать прямые записи `prefs.layout.current.set(...)` по UI и gesture controller.

`Vim8ImeService` должен наблюдать именно активный language/layout state и обновлять `keyboardData` без пересоздания всей IME.

## Критерии готовности

- Можно включить минимум 2 embedded layouts и минимум 1 custom layout одновременно.
- После restart список, порядок и активный язык сохраняются.
- Старый пользователь после upgrade получает свой прежний layout как активный и не теряет custom layouts.
- Нельзя оставить систему без единственного валидного enabled layout.
- Переключение layout программным вызовом не создаёт visible keyboard flicker.

---

# Этап 2. Сделать понятные настройки языков

## Бизнес-задача

Дать пользователю простой экран, где он выбирает языки, их порядок и основную раскладку.

## Зачем это нужно

Быстрое переключение имеет смысл только тогда, когда пользователь сам контролирует, между чем именно происходит переключение. Настройки должны одинаково поддерживать встроенные и пользовательские layouts.

## Что сделать технически

На базе существующего Layout settings UI сделать multi-layout management:

- список доступных embedded layouts;
- список/import custom layouts;
- checkbox/switch «использовать этот язык»;
- изменение порядка языков;
- выбор default/primary language;
- для custom layout — optional locale/language association;
- удаление custom layout не должно оставлять dangling `activeLanguageId`;
- минимум один язык всегда enabled.

Не ограничиваться простой заменой текущего radio button на checkbox без state model из Этапа 1.

## Критерии готовности

- Пользователь может настроить EN + RU + custom LV.
- Порядок EN → RU → LV сохраняется.
- Custom layout участвует в переключении так же, как embedded.
- Настройки переживают restart и backup/restore, если эти preferences экспортируемые.

---

# Этап 3. Добавить надёжное быстрое переключение языка

## Бизнес-задача

Сделать базовый способ менять язык одним действием прямо на клавиатуре.

## Зачем это нужно

Это минимальная функция, после которой multilingual 8VIM становится практически пригодным. Даже если пользователь не захочет изучать новый жест, он должен иметь очевидный и доступный способ переключиться.

## Продуктовое решение

Сначала сделать **обычный переключатель (globe/language control) как fallback**, а уже следующим этапом — жест. Это не противоречит философии 8VIM: надёжный fallback снижает риск, а жест потом даёт быстрый expert UX.

## Что сделать технически

Расширить существующий механизм `CustomKeycode`, а не вводить fake Android key events.

Например:

```text
SWITCH_LANGUAGE_NEXT
SWITCH_LANGUAGE_PREVIOUS
```

Обработка идёт через `KeyboardManager.handleKeyCode(CustomKeycode, ...)` → `LanguageManager`.

Это удобно, потому что одна и та же команда сможет использоваться:

- sidebar button;
- YAML movement/action;
- future radial selector;
- accessibility action.

UI:

- добавить globe/language control в sidebar или другое устойчивое место;
- коротко показывать активный код языка (`EN`, `RU`, `LV`) в центральной области или рядом с control;
- при переключении дать существующий haptic feedback;
- обеспечить accessibility label.

Отдельно рассмотреть [issue #437 — quick switch to another Android keyboard](https://github.com/8VIM/8VIM/issues/437). Это **другая** функция: EN↔RU внутри 8VIM и 8VIM↔Gboard/другая IME не должны смешиваться в одном state.

## Критерии готовности

- Одно действие циклически переключает EN → RU → LV → EN.
- Previous-language action возвращает пользователя назад.
- Активный язык визуально понятен.
- Input engine и текущий текст не сбрасываются.
- Переключение не добавляет символ в editor и не посылает внешнему приложению fake keycode.

---

# Этап 4. Добавить жестовое переключение в стиле 8VIM

## Бизнес-задача

Сделать смену языка частью моторного жестового потока 8VIM, чтобы опытному пользователю не приходилось целиться в кнопку.

## Зачем это нужно

Именно gesture-first UX отличает 8VIM от обычной клавиатуры. Для человека, который несколько раз меняет язык внутри одного предложения, экономия одного-двух действий на каждом switch заметно влияет на скорость.

## Что сделать технически

Не хардкодить новый movement поверх существующего `actionMap` без проверки конфликтов.

Приоритет вариантов:

1. **Configurable gesture, вызывающий `SWITCH_LANGUAGE_NEXT/PREVIOUS`** через существующую action/custom-keycode pipeline.
2. Предустановленный default gesture только если он гарантированно не ломает основные layouts.
3. Radial language selector по long-press центра — отдельная UX-задача после стабильного next/previous gesture.

Идеи для UX, которые можно прототипировать:

```text
             EN
              ↑
              │
        LV ←  ●  → RU
              │
              ↓
           Symbols
```

или два motor gestures:

```text
clockwise       -> next language
counterclockwise -> previous language
```

После gesture switch кратко показывать `RU` / `EN` / `LV` в центральном круге.

Важно: существующие `LayerLevel.SECOND...SIXTH` не являются language modes. Не переносить языки в layers.

## Критерии готовности

- Gesture можно отключить или переназначить.
- Gesture не делает обычные символы недоступными.
- Custom layouts не ломаются из-за зарезервированной скрытой sequence.
- Next/previous gesture и sidebar control используют один `LanguageManager`, а не две реализации.

---

# Этап 5. Запоминать язык отдельно для каждого приложения

## Бизнес-задача

Автоматически возвращать пользователю привычный язык в конкретном приложении.

Пример:

```text
Telegram -> RU
GitHub   -> EN
WhatsApp -> LV
Termux   -> EN
```

## Зачем это нужно

Пользователь часто устойчиво использует разные приложения на разных языках. Если клавиатура сама восстанавливает последний выбранный язык, ручных переключений становится существенно меньше.

## Что сделать технически

Android уже передаёт `EditorInfo` при `onStartInputView`. Текущий `Vim8ImeService` вызывает `editorInstance.handleStartInputView(info)`, поэтому hook уже есть.

Использовать `EditorInfo.packageName` как ключ локальной таблицы:

```text
packageName -> languageProfileId
```

Требования:

- настройка feature toggle `Remember language per app`;
- хранить только package name + language ID, никакого текста;
- если сохранённый язык больше не enabled — fallback, а stale mapping удалить/игнорировать;
- при manual language switch обновлять mapping текущего app;
- при `onStartInputView` восстановить language до/в момент подготовки keyboard view, чтобы не было мигания старой раскладки;
- решить, входит ли mapping в backup. Рекомендуется: да, если пользовательские preferences уже экспортируются, но никаких введённых текстов там быть не должно.

## Критерии готовности

- После выбора RU в Telegram и EN в GitHub повторный вход в соответствующее text field восстанавливает правильный язык.
- Mapping локальный и не требует network permission.
- Удаление языка не ломает открытие IME.

---

# Этап 6. Переделать prediction layer под несколько языков

## Бизнес-задача

Подготовить систему подсказок так, чтобы она понимала текущий язык и могла дальше поддерживать completion, исправления опечаток и prediction.

## Зачем это нужно

Существующая frequency DB глобальная. Если оставить её как есть, русские, английские и латышские слова будут участвовать в одном ranking. Это даст плохие suggestions и усложнит autocorrect.

Этот этап — фундамент smart text. Не начинать полноценный autocorrect раньше него.

## Что сделать технически

Разделить orchestration, sources и ranking.

Рекомендуемая модель:

```kotlin
data class Suggestion(
    val text: String,
    val type: SuggestionType,
    val languageId: String,
    val score: Double,
    val source: SuggestionSource,
)
```

Минимальные `SuggestionType`:

```text
COMPLETION
CORRECTION
NEXT_WORD
USER_WORD
```

Архитектурно:

```text
TextContextReader
        ↓
Active LanguageProfile
        ↓
SuggestionEngine
   ├── DictionaryCompletionProvider
   ├── UserHistoryProvider
   ├── AutocorrectProvider       (следующий этап)
   └── NextWordProvider          (позже)
        ↓
SuggestionRanker
        ↓
существующие 3 UI slots
```

Что изменить:

- `WordFrequencyRepository` должен хранить/запрашивать данные **с language key** либо использовать отдельное partition/storage per language;
- seed words должны быть per-language;
- `SuggestionsManager` не должен считать любой `String` одинаковым по происхождению;
- сохранить существующий UI из трёх suggestions и gesture selection — это сильная сторона проекта;
- `TextReplacementManager` оставить отдельной системой: shortcut replacement не является autocorrect;
- PASSWORD input variations должны отключать learning/prediction;
- не отправлять typed text во внешние сервисы в рамках первого smart-text implementation.

## Критерии готовности

- RU frequency/history не влияет на EN ranking.
- При switch language suggestion state очищается/пересчитывается для нового языка.
- Старый UI выбора 3 suggestions продолжает работать.
- Password fields не обучают словарь и не показывают suggestions.

---

# Этап 7. Сделать нормальный словарный autocomplete

## Бизнес-задача

Во время набора предлагать завершение текущего слова, как в обычной современной клавиатуре.

Пример:

```text
prog -> program / programming / progressive
```

## Зачем это нужно

Для 8VIM длинные слова особенно выгодно завершать раньше: пользователь экономит несколько gesture sequences. Это даёт ощутимую пользу даже до появления сложного autocorrect.

## Что сделать технически

1. Для каждого поддерживаемого языка иметь base dictionary.
2. Перед включением сторонних dictionary data обязательно проверить лицензию отдельно от лицензии исходного кода.
3. Не копировать GPL/source-available код или dictionary assets в Apache-2.0 fork без явного решения по лицензированию.
4. Ranking должен учитывать как минимум:
   - prefix match;
   - базовую частоту слова;
   - user frequency;
   - active language;
   - регистр первого символа.
5. Пользовательские слова должны постепенно подниматься в ranking.
6. Желательно предусмотреть import/export user dictionary позже, но не блокировать первый release.

Начать с deterministic offline implementation. ML для первой версии не нужен.

## Критерии готовности

- Completion работает отдельно для каждого enabled языка.
- Слово из RU dictionary не появляется в EN mode без специального bilingual режима.
- Часто используемые пользователем слова поднимаются в ranking.
- Регистр предложения корректно отражается в suggestion.

---

# Этап 8. Добавить исправление опечаток

## Бизнес-задача

Предлагать правильное слово, если пользователь допустил типичную опечатку, и позже — автоматически исправлять очевидные ошибки.

Пример:

```text
helo -> hello
превт -> привет
```

## Зачем это нужно

Это одна из главных функций, из-за которых пользователи остаются на Gboard и других зрелых клавиатурах. Без typo correction suggestion bar помогает только тогда, когда prefix уже правильный.

## Что сделать технически

Разделить rollout на две стадии.

### 8A. Сначала только correction suggestions

Candidate generation:

- Damerau-Levenshtein / edit distance;
- transposition;
- insertion/deletion/substitution;
- distance threshold зависит от длины слова;
- кандидаты только из active-language dictionary;
- user frequency участвует в ranking.

В suggestion bar correction должна быть типизирована как `CORRECTION`, а не просто строка.

### 8B. Затем automatic correction

Автоматически заменять только при достаточно высокой confidence и только на word boundary.

Обязательно реализовать undo contract:

- typed `helo `;
- keyboard исправляет на `hello `;
- immediate Backspace/undo возвращает оригинальное `helo`, а не просто удаляет последний символ.

Хранить кратковременное состояние последней autocorrection:

```text
originalText
replacementText
boundary
cursor position/session
```

Не делать aggressive autocorrect в:

- password/visible-password fields;
- URL/email/code-like contexts, пока нет уверенных правил;
- custom/no-dictionary language profiles.

Не путать autocorrect с `TextReplacementManager`: пользовательский shortcut всегда является явным правилом, autocorrect — вероятностным решением.

## Критерии готовности

- Известная опечатка получает correction candidate.
- Correction учитывает active language.
- Auto-replacement можно отключить отдельно от suggestion display.
- Immediate undo возвращает исходное слово.
- Коррекция не срабатывает в password fields.

---

# Этап 9. Добавить следующий-word prediction

## Бизнес-задача

После завершения слова предлагать наиболее вероятные следующие слова.

## Зачем это нужно

Это следующая ступень после completion/autocorrect: пользователь может вводить целое слово одним выбором вместо серии жестов.

## Что сделать технически

Не начинать с большой ML-модели. Для первой версии достаточно локального per-language statistical model:

- bigram frequency;
- при необходимости trigram позже;
- user history имеет больший вес, чем generic seed;
- fallback на частотные слова только если context model ничего не знает.

Текущий upstream `getTopWords()` не является настоящим next-word prediction, потому что не учитывает предыдущее слово. Его следует считать fallback, а не финальным алгоритмом.

Storage должен быть language-scoped и ограниченным по размеру. Не хранить полный пользовательский текст — только агрегированные token/n-gram counters.

## Критерии готовности

- `thank` может повышать вероятность `you` относительно глобально частого случайного слова.
- EN и RU n-grams не смешиваются.
- Storage имеет понятный лимит/cleanup strategy.
- Feature полностью работает offline.

---

# Этап 10. Сделать «один английский термин внутри русского текста»

## Бизнес-задача

Дать пользователю быстрый временный переход на другой язык только для одного слова, после чего автоматически вернуться назад.

Пример:

```text
RU -> temporary EN -> "GitHub" -> automatic RU
```

## Зачем это нужно

В технической переписке и многоязычном общении пользователи постоянно вставляют один иностранный термин внутрь предложения. Обычный RU → EN → RU требует двух переключений на одно слово.

## Важное UX-уточнение

Не реализовывать это буквально как «держать палец на центре, другой рукой печатать, отпустить». Для single-touch/one-handed 8VIM это неудобный interaction model.

Лучше сделать **one-word temporary mode**:

1. специальный gesture активирует следующий/предыдущий язык временно;
2. пользователь вводит одно слово обычным способом;
3. на word boundary клавиатура автоматически возвращается на исходный язык.

## Что сделать технически

State machine примерно уровня:

```text
baseLanguageId
temporaryLanguageId
temporaryMode = true/false
```

При activation сохранить base language, выбрать temporary.

Return trigger:

- space;
- sentence punctuation;
- enter;
- configurable word-boundary logic.

Не возвращаться посреди слова из-за apostrophe/hyphen без language-aware rules.

Визуально temporary state должен отличаться, например `EN•` или короткой подсветкой языка.

## Критерии готовности

- RU → one-word EN → автоматический RU работает без второго manual switch.
- Cancel gesture/back action возвращает base language без ввода.
- Prediction/autocorrect во временном режиме используют temporary language dictionary.

---

# Этап 11. Bilingual / mixed-language suggestions — только после явных языков

## Бизнес-задача

Опционально предлагать слова сразу из двух часто используемых языков, если пользователь этого хочет.

## Зачем это нужно

Это приблизит поведение к mature keyboards, где английские термины могут появляться внутри русского текста без постоянного manual switch.

## Что сделать технически

Этот этап не должен заменять explicit language selection. Сначала должна стабильно работать модель активного языка.

Возможная реализация:

- primary active language;
- optional secondary suggestion language;
- script detection (Latin/Cyrillic/etc.) как сильный ranking signal;
- candidates получают language ID;
- layout switch не обязан происходить автоматически только потому, что suggestion пришёл из secondary dictionary.

Не делать automatic language detection основой всего state management.

---

# Этап 12. Release hardening и публичная версия форка

## Бизнес-задача

Подготовить версию, которую можно рекомендовать людям как ежедневную клавиатуру, а не только development build.

## Зачем это нужно

Для IME доверие критично: любые зависания, потеря текста, неправильный Enter или внезапная смена языка воспринимаются значительно хуже, чем аналогичный баг в обычном приложении.

## Что сделать технически

### Compatibility matrix

Проверить минимум:

- Android 14/15/16 или доступный эквивалент текущих target devices;
- обычные text fields;
- password fields;
- multiline fields;
- search fields;
- email/URL fields;
- Telegram/Signal/WhatsApp или эквивалентные messaging apps;
- Slack/Discord для Enter/newline regression;
- браузер;
- Termux/code editor.

### Regression tests

Обязательные сценарии:

- migration со старого single-layout state;
- enable/disable/reorder languages;
- embedded + custom switch;
- broken/missing custom layout fallback;
- per-app restore;
- language switch during active text field;
- suggestion reset on language switch;
- password privacy;
- autocorrect undo;
- process restart;
- backup/restore.

### Release engineering

- reproducible build;
- отдельный applicationId для публичного fork release, если fork должен устанавливаться параллельно upstream;
- не обязательно массово переименовывать Kotlin namespace только ради applicationId;
- собственный signing key;
- changelog;
- attribution и Apache-2.0 notices;
- audit лицензий dictionaries/assets;
- versioning и release tags.

---

# Upstream backlog: что учитывать при разработке

## Высокий приоритет / использовать как исходный материал

- [Issue #308 — Proper language selection](https://github.com/8VIM/8VIM/issues/308) — основной multilingual requirement.
- [PR #614 — Fix Dang File Picker](https://github.com/8VIM/8VIM/pull/614) — маленький practical Android fix.
- [PR #604 — Remove some non-determinism](https://github.com/8VIM/8VIM/pull/604) — reproducible build.
- [PR #622 — Add optional complex gesture reset mode](https://github.com/8VIM/8VIM/pull/622) — потенциально полезный gesture fix, есть тесты.
- [Issue #568 — Enter key submits instead of breaking the line](https://github.com/8VIM/8VIM/issues/568) — важный daily-use compatibility bug.
- [Issue #437 — quick switch to another keyboard](https://github.com/8VIM/8VIM/issues/437) — отдельная IME-switch feature.

## Использовать идею, но не patch как есть

- [PR #553 — custom layout movements override](https://github.com/8VIM/8VIM/pull/553). Требование полезное, реализацию перепроверить/переделать и покрыть тестами.

## Языковые contribution PR, которые можно рассмотреть отдельно

- [PR #594 — Amazigh layout](https://github.com/8VIM/8VIM/pull/594)
- [PR #626 — Dutch layout extensions](https://github.com/8VIM/8VIM/pull/626)

Их лучше принимать отдельно от core multilingual architecture, чтобы изменения layout data не смешивались с state-management PR.

## Не включать в основной roadmap сейчас

- [PR #363 — Multi sectors](https://github.com/8VIM/8VIM/pull/363): сам автор обозначает branch как не готовую к merge; это отдельный эксперимент input geometry.
- [PR #530 — Layout tutorial](https://github.com/8VIM/8VIM/pull/530): полезный onboarding, но не блокирует multilingual/smart text.
- старые Dependabot PR: текущий master уже обогнал многие предложенные там версии; делать свежий dependency audit вместо cherry-pick исторических bumps.

---

# Рекомендованный порядок PR в форке

Агенту лучше делать изменения отдельными небольшими PR/commits в такой последовательности:

```text
PR 01  Baseline fixes / CI / reproducible build
PR 02  LanguageProfile + prefs migration + LanguageManager
PR 03  Multi-language Settings UI
PR 04  Next/Previous language CustomKeycode + sidebar + indicator
PR 05  Gesture language switching
PR 06  Per-app language memory
PR 07  Language-aware Suggestion model + repository split
PR 08  Per-language dictionary completion
PR 09  Typo correction suggestions
PR 10  Automatic correction + undo
PR 11  Bigram next-word prediction
PR 12  One-word temporary language mode
PR 13  Optional bilingual suggestions
PR 14  Release hardening/documentation
```

Не объединять PR 02–10 в одну большую ветку. Особенно не смешивать migration preferences, gesture controller и autocorrect в одном change set.

---

# Технические решения, которые следует считать предпочтительными

## 1. Language state отдельно от layout layers

`LayerLevel` остаётся механизмом конкретной раскладки. `LanguageProfile` управляет выбором layout и language-specific services.

## 2. Один `LanguageManager`

UI, gesture и per-app restore должны вызывать один и тот же manager. Никаких прямых `prefs.layout.current.set(...)` из пяти мест.

## 3. `CustomKeycode` как мост от gesture/layout к language command

В существующем проекте этот механизм уже используется для внутренних действий. Добавление `SWITCH_LANGUAGE_NEXT/PREVIOUS` естественнее, чем synthetic `KeyEvent`.

## 4. Explicit active language — источник истины

Autocomplete/autocorrect не должен угадывать язык, чтобы понять, какой dictionary использовать. Auto-detection может быть дополнительным ranking signal позже.

## 5. Smart text offline-first

Для первой версии prediction/autocorrect полностью локальные. Это проще, быстрее, приватнее и соответствует ожиданиям от клавиатуры.

## 6. Text Replacement не смешивать с autocorrect

Shortcut rules — deterministic user configuration. Autocorrect — probabilistic engine. Они могут пользоваться общим `InputConnection`, но должны иметь разные domain APIs.

## 7. Custom layouts — first-class citizen

Нельзя реализовать multilingual только для embedded resources. Issue #308 обсуждал именно необходимость custom layout support. Custom profile должен иметь optional user-selected locale для dictionary features.

---

# Definition of Done для каждого feature PR

Каждый PR считается готовым только если:

1. Сборка и существующие tests проходят.
2. Для новой domain logic есть unit tests.
3. Есть migration/fallback test, если меняются preferences/storage.
4. Нет hidden direct writes в старое language/layout state в обход manager.
5. Password/security-sensitive input variations проверены, если feature касается текста/suggestions.
6. Нет network dependency для core typing path.
7. Изменение не ломает custom layouts.
8. В PR описаны manual test scenarios на реальном IME.
9. Если добавлен новый preference — он корректно ведёт себя в backup/reset.
10. Если использованы сторонние dictionary/assets/code — указана и проверена лицензия.

---

# Product milestones

## Milestone A — Stable Fork

Результат: форк нормально собирается, есть несколько upstream fixes, можно уверенно начинать feature development.

Включает Этап 0.

## Milestone B — Multilingual MVP

Результат: пользователь выбирает EN/RU/LV/custom layouts и мгновенно переключается между ними кнопкой или жестом.

Включает Этапы 1–4.

Это первая версия, которую уже имеет смысл использовать ежедневно для проверки multilingual UX.

## Milestone C — Context-aware Multilingual

Результат: язык запоминается per app; suggestions понимают активный язык; работает нормальный per-language completion.

Включает Этапы 5–7.

## Milestone D — Gboard-class basic smart text

Результат: typo suggestions, безопасный autocorrect с undo и простой context-aware next-word prediction.

Включает Этапы 8–9.

## Milestone E — 8VIM-specific multilingual UX

Результат: one-word temporary language mode и, при необходимости, bilingual suggestions.

Включает Этапы 10–11.

---

# Главный приоритет для агента

Если приходится сокращать scope, **не сокращать Этапы 1–4**. Именно они решают основную продуктовую проблему: 8VIM должен стать реально многоязычным.

Если после них остаётся ресурс только на одну smart-text задачу, сначала делать **language-aware autocomplete/completion**, а не ML prediction.

Не начинать autocorrect, пока active language и per-language storage не являются стабильным фундаментом.
