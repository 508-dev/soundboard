# TODO To Issue Workflow

This extra converts TODO-style comments into GitHub issues. It is intentionally not enabled by default.

Reasons to opt in carefully:

- It grants workflow write permissions.
- It uses a third-party action.
- It can create issue noise if the repository does not have a clear TODO policy.
- Some variants write issue links back to the default branch.

Before enabling it:

1. Review the action source and pin the action version.
2. Decide which identifiers are allowed, such as `TODO`, `FIXME`, or `BUG`.
3. Decide whether generated issues should be linked back into source files.
4. Use the default `GITHUB_TOKEN` when possible; add project secrets only when required.
5. Prefer manual `workflow_dispatch` at first, then move to scheduled or merge-triggered runs after the workflow proves useful.

Copy `todo-to-issue.yml.example` to `.github/workflows/todo-to-issue.yml` when ready.

## Agent Notes

- Do not enable this workflow without an explicit TODO policy.
- Prefer `workflow_dispatch` while testing so the repo does not create issue
  noise automatically.
- Re-check workflow permissions before copying it into a target repo.
