---
schema_version: 1
open_count: 0
waived_count: 0
fixed_count: 3
total_count: 3
last_updated: 2026-09-17T20:39:50.089Z
---

# Broken Windows Ledger

> Cross-phase defect register. With `workflow.windows_enforce` enabled, `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 01 | unrun-verify | 8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt | 179 | Targeted AvailableLayoutsSpec/LayoutSpec Gradle verification blocked before test discovery: Android SDK location is missing. | fixed |  | 2026-09-17T10:28:12.493Z | 2026-09-17T20:39:49.703Z |
| 2 | 01 | unrun-verify | 8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt |  | Targeted KeyboardManagerSpec/EditorInstanceSpec Gradle verification blocked before test discovery: Android SDK location is missing. | fixed |  | 2026-09-17T10:36:43.642Z | 2026-09-17T20:39:49.900Z |
| 3 | 01 | unrun-verify | 8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt | 29 | Targeted CustomLayoutImportAdapterSpec/AvailableLayoutsSpec/LayoutSpec Gradle verification blocked before test discovery: Android SDK location is missing. | fixed |  | 2026-09-17T10:47:59.699Z | 2026-09-17T20:39:50.089Z |

````json
[
  {
    "id": 1,
    "kind": "unrun-verify",
    "phase": "01",
    "file": "8vim/src/test/kotlin/inc/flide/vim8/ime/layout/AvailableLayoutsSpec.kt",
    "line": 179,
    "description": "Targeted AvailableLayoutsSpec/LayoutSpec Gradle verification blocked before test discovery: Android SDK location is missing.",
    "status": "fixed",
    "reason": "",
    "recorded_at": "2026-09-17T10:28:12.493Z",
    "resolved_at": "2026-09-17T20:39:49.703Z",
    "milestone": null
  },
  {
    "id": 2,
    "kind": "unrun-verify",
    "phase": "01",
    "file": "8vim/src/test/kotlin/inc/flide/vim8/ime/keyboard/text/KeyboardManagerSpec.kt",
    "line": null,
    "description": "Targeted KeyboardManagerSpec/EditorInstanceSpec Gradle verification blocked before test discovery: Android SDK location is missing.",
    "status": "fixed",
    "reason": "",
    "recorded_at": "2026-09-17T10:36:43.642Z",
    "resolved_at": "2026-09-17T20:39:49.900Z",
    "milestone": null
  },
  {
    "id": 3,
    "kind": "unrun-verify",
    "phase": "01",
    "file": "8vim/src/test/kotlin/inc/flide/vim8/app/settings/CustomLayoutImportAdapterSpec.kt",
    "line": 29,
    "description": "Targeted CustomLayoutImportAdapterSpec/AvailableLayoutsSpec/LayoutSpec Gradle verification blocked before test discovery: Android SDK location is missing.",
    "status": "fixed",
    "reason": "",
    "recorded_at": "2026-09-17T10:47:59.699Z",
    "resolved_at": "2026-09-17T20:39:50.089Z",
    "milestone": null
  }
]
````
