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
3. Active OpenSpec change under `openspec/changes/`
4. Stable project rules in `docs/`
5. Local assumptions

## Project Defaults

- Reply in Chinese unless the user asks otherwise.
- Clarify scope, success criteria, and non-goals before coding.
- Prefer the smallest change that proves the idea.
- Do not do incidental refactors.
- Do not lock in frameworks, vendors, or architecture without an explicit decision.

## Workflow Stack

This repository standardizes on:

- Codex CLI / Codex app as the execution environment
- `oh-my-codex` as the workflow orchestration layer
- `superpowers brainstorming` for requirement exploration
- OpenSpec as the only implementation contract after scope is confirmed
- `gstack` as second-opinion review for spec and diff
- `superpowers implementation` for disciplined execution, including multi-agent execution when appropriate

Role rules:

- `oh-my-codex` organizes the flow, records status, and helps stage the work.
- `superpowers brainstorming` explores and compares options, but does not define final scope.
- OpenSpec defines the final behavior, acceptance criteria, and task boundaries.
- `gstack` challenges assumptions and reviews artifacts, but does not silently widen scope.
- `superpowers implementation` executes tasks from OpenSpec and does not redefine requirements.

## Task Levels

Use the following default sizing unless the user explicitly requests otherwise.

### Small Task

Use for:

- small localized fixes
- copy updates
- minor config changes
- simple script or test adjustments

Rules:

- OpenSpec is optional.
- Use `inspect -> edit -> targeted verification -> report evidence`.
- Keep the change local and reversible.

### Medium Task

Use for:

- new behavior
- workflow change
- multi-file feature work
- any change that needs acceptance criteria

Rules:

- create or update OpenSpec before implementation
- `gstack` review is recommended when risk is non-trivial

### Large Task

Use for:

- architecture changes
- data model changes
- public API changes
- auth, permission, deployment, migration, or compatibility-sensitive work

Rules:

- require `proposal.md`, `spec.md`, `tasks.md`, and usually `design.md`
- require `gstack` review before implementation

## Uncertain Requirement Handling

Treat the task as uncertain when:

- the goal is vague
- acceptance criteria are missing
- multiple valid paths exist
- product, UX, architecture, API, security, or data decisions are still open

Rules:

- do not jump directly into implementation
- use `superpowers brainstorming` first for medium and large uncertain work
- convert only confirmed outcomes into OpenSpec
- exploratory notes stay in `research/`

If the user explicitly says not to brainstorm yet, skip that stage for now and focus on governance or documentation work only.

## Stage Control

When the user limits the current round to a specific stage, do not cross into later stages.

Allowed stages:

1. `governance`
2. `discovery`
3. `openspec`
4. `review-spec`
5. `implement-task`
6. `test-fix`
7. `review-diff`
8. `archive`

Stage rules:

- `governance`: repository rules, workflow docs, templates, and boundaries only
- `discovery`: research and exploration only, no business code changes
- `openspec`: create or update OpenSpec only
- `review-spec`: review spec artifacts only
- `implement-task`: implement only the selected task from the active OpenSpec change
- `test-fix`: verify and fix issues related to the current change only
- `review-diff`: review the diff only
- `archive`: promote stable behavior into `openspec/specs/`

## Multi-Agent Rules

`superpowers implementation` may use multiple agents only when it improves throughput without creating ambiguous ownership.

Allowed:

- independent analysis tracks
- parallel code reading
- parallel validation or review passes
- implementation split by clearly separated tasks

Not allowed:

- multiple agents editing the same unresolved requirement
- implementation before the active OpenSpec change is clear
- letting parallel agents define conflicting scope

Merge rules:

- OpenSpec remains the single source of truth
- one task owner must reconcile agent outputs before completion
- verification evidence must be reported against the merged result

## Documentation Workflow

Default flow for medium, large, or uncertain work:

1. `oh-my-codex` frames the task and stage
2. `superpowers brainstorming` explores uncertain work when needed
3. confirmed decisions move into `research/` or directly into OpenSpec
4. OpenSpec change is created in `openspec/changes/<change-id>/`
5. `gstack` reviews the spec when risk warrants it
6. `superpowers implementation` executes tasks one by one
7. verification evidence is collected
8. stable behavior is archived into `openspec/specs/`

Rules:

- `research/` is exploratory and is not implementation authority
- `openspec/changes/` is the implementation contract once created
- do not implement behavior that is not captured in the active OpenSpec change
- for small localized edits, OpenSpec can be skipped if the user-facing behavior does not materially change

## Engineering Defaults

Apply these rules whenever code work starts:

- think before coding
- keep the solution minimal
- make surgical changes only
- define success criteria and verification evidence before implementation
- do not hardcode secrets
- do not hide unverified assumptions behind code changes

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
