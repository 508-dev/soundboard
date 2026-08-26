# 508 Devkit Decisions

Last reviewed: 2026-06-03

This is the constitution for the devkit. Use it as the decision authority; use files in this repo as examples or frozen primitives according to the decision below.

## Apply The Gold/Filler Test

Decision: keep and test files that agents would not reliably reproduce correctly without this repo. Treat ordinary app code as disposable examples.

Why: agents can generate plausible FastAPI handlers, workers, and frontend apps. They are less reliable at reproducing devkit-specific topology, safety policy, worktree behavior, and operational memory conventions.

Deviate when: a real target repo needs product code. Generate it for that repo instead of copying placeholder app code from the devkit.

## Dependency Cooldowns

Decision: use dependency cooldowns in every package manager that supports them.

Why: new package versions are a supply-chain risk window. The exact cooldowns are non-obvious and should converge across repos.

Deviate when: a security fix or production incident requires an immediate update. Document the exception in the PR.

## Locked Installs

Decision: commit lockfiles and use frozen or locked installs in CI.

Why: agents and humans need reproducible dependency resolution.

Deviate when: a repo is intentionally a library template without a runnable dependency graph. Document why no lockfile is committed.

## Bun First, pnpm First-Class

Decision: show Bun first for JavaScript workspaces while keeping pnpm first-class for teams, repos, or large workspaces that prefer pnpm-specific monorepo behavior.

Why: Bun is fast and simple for greenfield repos, and the author prefers it. That preference should not imply pnpm is second-class or wrong for new projects.

Deviate when: the target repo already uses pnpm, npm, Yarn, or another package manager for a clear reason. Do not churn package managers during unrelated work.

## Frontend Framework Neutrality

Decision: do not choose Next.js, Vite, TanStack Start, Astro, Expo, or any other frontend framework in root defaults.

Why: frontend framework choice depends on product shape, deployment target, routing, rendering model, and team familiarity.

Deviate when: the target repo has already chosen a framework or the user explicitly asks for one.

## Host Apps, Compose Infra

Decision: run app processes on the host and infrastructure through Docker Compose during local development.

Why: host app processes are easier for agents to inspect and faster for reload loops. Compose still provides concrete examples for local infrastructure such as databases and caches.

Deviate when: deployment parity, binary dependencies, or team policy require full-container development. Put that in docs and scripts explicitly.

## Deterministic Worktree Ports

Decision: derive local ports from the absolute worktree path.

Why: sibling worktrees should run concurrently without hand-editing `.env` files.

Deviate when: a platform assigns ports dynamically. Preserve the script for local development unless it is truly irrelevant.

## `.context/` Workspace Memory

Decision: keep `.context/` gitignored as workspace-local operational memory for humans and agents. Do not ship it as tracked template content.

Why: architecture notes, decisions, failures, runbooks, and summaries prevent repeated failed approaches and preserve local reasoning.

Deviate when: information is durable, user-facing, or contributor-facing. Put that in README, docs, or official project documentation instead.

## GitHub Hygiene

Decision: include small issue/PR templates, least-privilege workflows, pinned action SHAs, and Renovate cooldown policy. Keep Gitleaks and Dependency Review as opt-in extras: Gitleaks can create noisy baseline findings, and Dependency Review depends on GitHub's dependency graph and is primarily vulnerability/license/change reporting rather than active supply-chain attack detection.

Why: collaboration and security hygiene are broadly useful and easy to drift across repos.

Deviate when: a repo does not use GitHub or has a stronger existing platform policy.

## Currency

Decision: the devkit owns topology and policy, not permanent version freshness.

Why: frozen files age. Agents should preserve the repo's decisions while verifying current tool versions, action SHAs, and API docs when applying the devkit to a target repo.

Deviate when: working fully offline. In that case, copy the known-good pinned versions and document that currency was not verified.
