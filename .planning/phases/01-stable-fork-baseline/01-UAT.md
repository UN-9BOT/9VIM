---
status: complete
phase: 01-stable-fork-baseline
source: 01-01-SUMMARY.md, 01-02-SUMMARY.md, 01-03-SUMMARY.md, 01-04-SUMMARY.md, 01-05-SUMMARY.md, 01-06-SUMMARY.md, 01-07-SUMMARY.md
started: 2026-09-17T20:30:00Z
updated: 2026-09-17T21:50:23Z
---

## Current Test

[testing complete]

## Tests

### 1. Import a valid custom layout
expected: Select a valid YAML through the Android file picker; the layout is added to available layouts and becomes active and usable.
result: pass
reported: "custom импортировался. работает"

### 2. Re-import the same URI
expected: Importing the same provider URI again reuses the existing record and does not create a duplicate.
result: pass
reported: "1. дубля нет."

### 3. Reject a zero-layer YAML
expected: Selecting a zero-layer YAML shows a validation alert and leaves the current layout unchanged.
result: pass
reported: "это был zero."

### 4. Reject an invalid YAML
expected: Selecting malformed YAML shows a validation alert and leaves the current layout unchanged.
result: pass
reported: "ща invalid."

## Summary

total: 4
passed: 4
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

<!-- No gaps were reported. -->
