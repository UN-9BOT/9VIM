# Enter/newline compatibility contract

## Scope

Phase 1 fixes the Enter routing contract at the JVM boundary. The regression
spec drives `KeyboardManager.onInputKeyUp` with the same `KEYCODE_ENTER` action
that the keyboard uses in production. `KeyboardManager` decides between a
real Enter and an editor action; `EditorInstance` delegates the selected path
to Android's `InputConnection`.

This is JVM regression evidence only. It does not claim Slack, Discord, or
current-device compatibility. The broader Android application and
`InputType × action × flag` matrix is deferred to a later compatibility pass.

## Precedence table

| Representative family | Editor action | `IME_FLAG_NO_ENTER_ACTION` | Expected route |
| --- | --- | --- | --- |
| Multiline with no explicit action | `NONE` | absent | `performEnter()` / newline |
| Multiline with the no-action flag | `NONE` | present | `performEnter()` / newline |
| Single-line submit action | `SEND` or `DONE` | absent | `performEnterAction(action)` |
| Explicit action with the no-action flag | `SEND`, `DONE`, `GO`, `SEARCH`, `NEXT`, or `PREVIOUS` | present | `performEnter()` / newline |

`IME_FLAG_NO_ENTER_ACTION` always wins. Consequently, an explicit editor
action is used only when that flag is absent. The `NONE` cases represent the
multiline/no-explicit-action families at the `ImeOptions` routing seam; Android
field metadata remains outside this JVM-only contract.

## Reproduce the JVM evidence

Run the existing manager and editor specs:

```bash
./gradlew :8vim:testDebugUnitTest \
  --tests 'inc.flide.vim8.ime.keyboard.text.KeyboardManagerSpec' \
  --tests 'inc.flide.vim8.ime.editor.EditorInstanceSpec'
```

`KeyboardManagerSpec` covers the four representative families directly
through `onInputKeyUp`. Each case checks the selected `EditorInstance` method
and verifies that the alternative method was not called. The explicit-action
family is parameterized only across `SEND`, `DONE`, `GO`, `SEARCH`, `NEXT`, and
`PREVIOUS`; no separate fixture abstraction is introduced.

`EditorInstanceSpec` records the platform boundary: `performEnterAction` calls
`InputConnection.performEditorAction`, while `performEnter` emits an Enter
key down/up pair. Those Android delegations are tested separately from the
manager's precedence decision.

## Compatibility boundary

The command above must be run in a Java 17 and Android SDK-capable environment
before treating the JVM assertions as green. This repository currently does
not have local Android SDK configuration, so an unavailable local run is not
converted into a device or app compatibility claim. Device/app smoke coverage
for messaging, browser, terminal, restart, and field-type combinations is
deferred.
