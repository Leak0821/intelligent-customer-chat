# Repository Guidelines

## Project Structure & Module Organization
This repository is currently empty, so new work should follow a simple, predictable layout:
- `src/` for application code
- `tests/` for automated tests
- `assets/` for static files such as images or fixtures
- `docs/` for reference material and implementation notes

Keep modules small and colocated with related tests when that improves readability.

## Build, Test, and Development Commands
No build or test tooling is configured yet. When you add a stack, document the commands here and keep them consistent:
- `npm test` or `pytest` for the main test suite
- `npm run build` or `make build` for production builds
- `npm run dev` for local development

Prefer adding scripts to a project-level entry point such as `package.json`, `Makefile`, or `pyproject.toml`.

## Coding Style & Naming Conventions
Use the formatter and linter native to the chosen stack. Until tooling is added:
- Indent with 2 spaces for JavaScript/TypeScript, 4 spaces for Python
- Use `snake_case` for Python files and `camelCase` for JavaScript variables/functions
- Use `PascalCase` for classes and React components
- Name tests after the behavior they verify, such as `chat_service.test.ts`

## Testing Guidelines
Add tests for any non-trivial behavior. Prefer clear, behavior-focused names like `should_retry_failed_requests`. Keep tests deterministic and avoid external network calls unless explicitly mocked.

## Commit & Pull Request Guidelines
There is no existing git history to infer a commit style from. Use short, imperative commit messages, for example: `Add chat message parser`.

Pull requests should include:
- A concise summary of the change
- Any setup or migration steps
- Screenshots or logs for UI or runtime changes
- Links to related issues when applicable

## Agent-Specific Instructions
Before making structural changes, check for existing conventions and update this file if the repository gains new tooling or directories. Keep this guide aligned with the actual project layout.
