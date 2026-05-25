# OpenSpec Changes

`openspec/changes/` stores in-flight change contracts.

Use this directory when:

- the change is medium or large
- the requirement is uncertain
- user-facing behavior may change
- multiple implementation paths exist

Recommended structure:

```text
openspec/changes/<change-id>/
  proposal.md
  spec.md
  tasks.md
  design.md   # optional
```

Rules:

- only confirmed decisions belong here
- exploratory notes stay in `research/`
- once implementation starts, the active change here is the source of truth
