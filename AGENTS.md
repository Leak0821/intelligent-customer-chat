# Intelligent Customer Chat Agent Contract

## Scope

This repository is in the setup phase for an intelligent customer service project.

- Code is not the priority yet.
- Documentation, workflow, and change discipline come first.
- The technical stack is intentionally undecided at this stage.

## Instruction Order

When guidance conflicts, use this order:

1. Explicit user instruction
2. This file
3. Approved OpenSpec change under `openspec/changes/`
4. Stable project rules in `docs/`
5. Local assumptions

## Working Defaults

- Reply in Chinese unless the user asks otherwise.
- Clarify scope, success criteria, and non-goals before coding.
- Prefer the smallest change that proves the idea.
- Do not do incidental refactors.
- Do not lock in frameworks, vendors, or architecture without an explicit decision.

## Documentation Workflow

Use the following default flow for medium, large, or uncertain work:

1. Discovery in `research/`
2. Confirmed change contract in `openspec/changes/<change-id>/`
3. Implementation task by task
4. Verification with explicit evidence
5. Archive stable behavior into `openspec/specs/`

Rules:

- `research/` is exploratory and is not implementation authority.
- `openspec/changes/` is the implementation contract once created.
- Do not implement behavior that is not captured in the active OpenSpec change.
- For small localized edits, OpenSpec can be skipped if the user-facing behavior does not change materially.

## Required Evidence

Before claiming a task is done, report:

- changed files
- commands run
- verification results
- known risks or skipped checks

## Repository Boundaries

Commit:

- durable Markdown documentation
- reusable scripts
- sanitized examples and templates
- approved OpenSpec and research artifacts

Keep local only:

- secrets, tokens, private keys, cookies
- `.local-ssh/`
- `.codex-home/`
- `.codex-local/`
- machine-specific configs with absolute paths
- generated caches, cloned tool sources, and temporary artifacts

## Customer Service Domain Guardrails

For any customer-facing behavior change, the spec should state:

- user or operator role
- channel or entry point
- trigger and inputs
- knowledge source or grounding source
- response boundaries and refusal rules
- escalation or handoff conditions
- logging, replay, or evaluation evidence
