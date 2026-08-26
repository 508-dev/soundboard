# Dev Script Extras

The root template keeps shell wrappers as the canonical entrypoints:

- `scripts/dev.sh`
- `scripts/docker-compose.sh`
- `scripts/check-all.sh`

That is intentional. Shell wrappers are easy for humans, CI, and agents to discover, and they can delegate to Python or Bun where those tools are better.

Recommended split:

- Use `.sh` for stable top-level commands and process orchestration.
- Use shell for dependency-free deterministic logic that must work before language dependencies are installed, such as worktree ports.
- Use `.mjs` or `.ts` for JS-only projects where the script directly interacts with Vite, Next.js, Drizzle, or TypeScript config.

This directory provides examples for JS-first repos that want JavaScript or TypeScript helper scripts.

## Agent Notes

- Keep `.sh` wrappers as the top-level interface unless the target repo has a
  strong reason to standardize on another script runtime.
- Use these examples as implementation references, not as files to copy into
  every project.
- If a helper depends on Bun, Node, or TypeScript, keep that dependency inside
  the selected stack instead of making root setup language-specific.
