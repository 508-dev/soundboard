# GitHub Template Usage

508 Devkit is marked as a GitHub template repository, but it is still a
pick-and-choose reference repo. GitHub templates copy the default-branch file
tree into a new repository. That generated repository should be treated as a
bootstrap workspace, not as the final project shape.

## Intended Flow

1. Create a new repository from the GitHub `Use this template` button.
2. Make the first project PR a template selection pass.
3. Use `MANIFEST.md` to produce a selection report before editing. Cover every
   top-level path in the devkit and generated repo with an adopt, adapt, skip,
   delete, or defer decision and a one-line reason.
4. Keep the root hygiene files that apply to most software repos.
5. Select only the language stacks and extras that match the project.
6. Delete unselected examples, placeholder workflows, and docs that no longer
   describe the project.
7. Rename package names, env defaults, service names, and documentation from
   devkit examples to the real product.
8. Run the narrowest relevant checks before building product features.

The selection pass is part of using the template. Do not start feature work
while the repository still contains irrelevant stacks or optional extras.

## Keep By Default

These files are broadly useful for most generated repos, with project-specific
edits:

- `AGENTS.md`, `CLAUDE.md`, and `.cursor/rules/`.
- `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, and the docs that still match
  the project.
- `scripts/` entrypoints for lint, typecheck, test, dev, Compose, and worktree
  ports.
- `.env.example`, `.gitignore`, `.dockerignore`, `.editorconfig`, and
  `.worktreeinclude`.
- `.github/` PR and issue templates when the project uses GitHub.

## Select Explicitly

Keep these only when they fit the project:

- `stacks/typescript/` for JavaScript or TypeScript projects.
- `stacks/typescript/pnpm/` when pnpm is the chosen JavaScript package manager.
- `stacks/python/` for Python APIs, workers, packages, or tools.
- `stacks/ruby/` for Ruby, Rails, or Rack projects.
- `compose.yml` and `docker-compose.yml` only for the local infrastructure the
  project actually needs.
- `extras/` directories only after confirming the workflow, owners, secrets,
  permissions, and deployment needs.
- `skills/` only when the target repo wants to ship project-local agent skills.

## Remove Or Rewrite

Before calling the generated repo ready, remove or rewrite:

- Unused language stacks.
- Example app/package names such as `508-devkit`, `example_api`, and
  `example_shared`.
- Optional GitHub extras with placeholder owners or extra permissions.
- Docs that describe template design history instead of the generated project.
- Databases, caches, object storage, observability, or LLM env vars that the
  project does not use.

## Agent Prompt

Use this prompt immediately after creating a repository from the template:

```text
This repository was generated from 508 Devkit. Do a template selection pass:
use MANIFEST.md to produce a selection report before editing, covering every
top-level path in the devkit and this repo with adopt/adapt/skip/delete/defer
and a one-line reason. Then keep only the root hygiene, stacks, extras, docs,
and workflows that fit this project, delete the rest, rename all devkit/example
identifiers, and run the narrowest relevant checks.
```
