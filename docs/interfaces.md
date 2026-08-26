# Repo Template vs Skill

Both interfaces make sense, but they should have different jobs.

## Repository Template

Use this repository as the source of truth.

It is best for:

- Copying concrete files into a new project.
- Creating a bootstrap repository through GitHub's `Use this template` button.
- Reviewing conventions in GitHub.
- Evolving scripts, CI, docs, and examples through normal PRs.
- Letting any agent or human inspect the same artifact.

GitHub's template button copies the default-branch file tree. That is a useful
starting point, but generated repos should immediately run a template selection
pass: keep applicable root hygiene, choose stacks and extras intentionally, and
delete everything else before product work starts.

## Downloadable Skill

A skill is useful as the agent-facing interface over this repo.

It is best for:

- Telling Codex how to apply the template.
- Asking an agent to inspect an existing repo and map which template pieces fit.
- Keeping the "how to use the devkit" workflow available from any coding session.

## Recommendation

Keep both:

- `508-devkit` repo: durable source of truth.
- `508-devkit` skill: lightweight instructions that point at this repo and describe how to choose files.

The skill should not duplicate every template file. It should reference this repository and teach the agent how to apply it.

## Agent Notes

- Update the repository files first; the skill should summarize how to apply
  them, not fork their content.
- When a convention changes, update both the tracked docs and
  `skills/508-devkit/SKILL.md` so agents do not apply stale behavior.
- Keep examples short enough that humans can review the policy in a PR.
