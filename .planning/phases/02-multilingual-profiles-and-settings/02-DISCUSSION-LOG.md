# Phase 2: Multilingual Profiles and Settings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-18
**Phase:** 2-Multilingual Profiles and Settings
**Areas discussed:** Profile stability and migration, Settings UI, Removal and fallback, Backup and restore

---

## Profile stability and migration

| Option | Description | Selected |
|--------|-------------|----------|
| Only EN | Minimal predictable start; other embedded layouts are opt-in. | ✓ |
| All embedded | Enable every bundled layout immediately. | |
| EN + RU | Presume a Russian primary scenario. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Current + custom | Preserve the old active layout and valid custom history; do not auto-enable unrelated embedded layouts. | ✓ |
| All embedded + custom | Enable every embedded layout during migration. | |
| Only current | Preserve only the old current layout. | |

| Option | Description | Selected |
|--------|-------------|----------|
| ID unchanged | Keep immutable URI-based ID; name/locale are metadata. | ✓ |
| New ID | Generate a new identity when metadata changes. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Separate active ID | Persist independent primary/default and runtime active IDs. | ✓ |
| First in list | Make primary always equal to the first ordered profile. | |
| No separate field | Use only runtime active ID. | |

**User's choice:** New install only EN; migration current + valid custom; immutable URI-based IDs; separate primary and active IDs.
**Notes:** Migration must be idempotent and deduplicate profiles. Stale custom URIs are pruned under Phase 1 rules.

---

## Settings UI

| Option | Description | Selected |
|--------|-------------|----------|
| Drag-and-drop | Natural reorder gesture, with extra accessibility work. | |
| Up/down buttons | Explicit, testable, and accessible ordering controls. | ✓ |
| Both | Provide drag-and-drop plus buttons. | |

| Option | Description | Selected |
|--------|-------------|----------|
| One ordered list | Unified switch order with a type badge. | ✓ |
| Two sections | Separate embedded and custom sections. | |
| Sections plus preview | Separate editors plus an aggregate preview. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Detail dialog | Edit name/locale with explicit confirmation. | ✓ |
| Inline selector | Edit locale directly in the row. | |
| Only during import | Do not allow later correction. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Separate radio group | Distinguish primary from enabled and active. | ✓ |
| Star on row | Compact but less explicit. | |
| Detail dialog button | Keep the list smaller but hide primary selection. | |

**User's choice:** One ordered list, up/down buttons, detail dialog for custom metadata, separate primary radio group.
**Notes:** Successful import remains enabled and active per Phase 1.

---

## Removal and fallback

| Option | Description | Selected |
|--------|-------------|----------|
| Primary profile | Use the enabled, valid primary as replacement. | ✓ |
| Previous valid | Prefer the last working selection. | |
| First in order | Select the first profile regardless of primary. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Immediate fallback | Allow active toggle; resolve replacement atomically. | ✓ |
| Choose another first | Require a separate selection before disabling active. | |
| Disallow active toggle | Never allow the current row to be disabled directly. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Forget URI | Remove state/history/permission/cache; never touch external source. | ✓ |
| Only disable | Keep history and permission for later re-enable. | |
| Delete local copy | Remove an app-owned copy if present. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Block action | Keep final profile enabled and explain the invariant. | ✓ |
| Auto-enable EN | Silently introduce another profile. | |
| Confirmation dialog | Ask for a replacement before allowing disable. | |

**User's choice:** Active removal falls back to primary; custom removal forgets the URI without deleting the user's file; final profile cannot be disabled.
**Notes:** If primary itself is removed, promote the next remaining profile in persisted order to primary and active. Broken-URI behavior from Phase 1 remains unchanged.

---

## Backup and restore

| Option | Description | Selected |
|--------|-------------|----------|
| Preserve backup ID | Keep stable profile ID and remap only the restored source URI. | ✓ |
| New URI-based ID | Create a new ID for each restored custom URI. | |
| Skip custom | Avoid remapping by dropping custom profiles. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Full replacement | Restore the complete language configuration. | ✓ |
| Merge profiles | Combine backup and local profile state. | |
| Custom files only | Restore documents without language state. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Primary → last valid → EN | Resolve references deterministically and keep restore usable. | ✓ |
| Keep local active | Preserve current device session. | |
| Abort restore | Reject backup on any missing custom/active reference. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Full language state | Export IDs/order/enabled/metadata/locale/primary/active plus custom documents. | ✓ |
| Custom files only | Export documents but not profile configuration. | |
| Profiles without active | Export configuration but leave active local. | |

**User's choice:** Preserve backup IDs with source-URI remap; full replacement; non-fatal missing custom warning; full profile state but no typed text or NLP personalization.
**Notes:** Restored state must keep enabled non-empty and primary/active inside enabled.

---

## the agent's Discretion

- Exact domain class/file names and serialized schema, subject to the locked
  identity, ordering, migration, and restore invariants.
- Exact visual styling of list badges, radio group, buttons, and detail dialog.

## Deferred Ideas

None.
