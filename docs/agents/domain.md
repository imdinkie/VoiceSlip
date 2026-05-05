# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Preferred layout

This is a single-context repo. Prefer repo-local domain documentation:

- **`CONTEXT.md`** at the repo root for the domain glossary, project language, and current system context.
- **`docs/adr/`** for architectural decision records.

If these files do not exist yet, proceed silently. Do not flag their absence or suggest creating them upfront unless the task is specifically about domain documentation or architectural decisions. The producer skill (`/grill-with-docs`) can create them lazily when terms or decisions actually get resolved.

## File structure

Expected repo-local structure:

```text
/
|-- CONTEXT.md
|-- docs/
|   |-- adr/
|   |   |-- 0001-example-decision.md
|   |-- agents/
|       |-- domain.md
|       |-- issue-tracker.md
|       |-- triage-labels.md
|-- app/
```

## Use the glossary's vocabulary

When your output names a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term as defined in `CONTEXT.md`. Do not drift to synonyms the glossary explicitly avoids.

If the concept you need is not in the glossary yet, that is a signal: either you are inventing language the project does not use, or there is a real gap to resolve with `/grill-with-docs`.

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
