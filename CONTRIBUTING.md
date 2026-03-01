# Contributing to CIPHER

## Code Standards
- We stringently deploy **Test-Driven Development (TDD)** mapping closely onto the `RED-GREEN-REFACTOR` methodology.
- Software structures identically deploy code utilizing standards articulated inside the Code Design Blueprints.

## Security & Secrets Policy
**CRITICAL**: YOU MUST NEVER commit any sensitive infrastructure identifiers mapping to database schemas, REST APIs, or cryptographic `.jks`/`.keystore` targets.
- Force verify `.gitignore` scopes encompass temporary configurations completely.
- Found yourself accidentally executing a compromised commit locally? **Do not push objects upstream.** Run `git reset HEAD~1` natively or request pipeline history truncations prior to distribution.

## Development Workflow
1. Spawn explicit branches tracing off `main` utilizing identifiers like `feat/`, `fix/`, `chore/` or `doc/`.
2. Always apply `verification-before-completion`, implying test suite matrices evaluate with absolutely 0 structural failures executing clean local builds natively.
3. Align semantic metadata to commit footprints concisely identifying implementation behaviors.

## Pull Requests
- CI validations strictly check security linters blocking secret injections actively.
- Code configurations enforce standard `pytest` coverage.
