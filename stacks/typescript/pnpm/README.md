# pnpm Stack Variant

The root TypeScript example shows Bun first. Use these files when a project
prefers pnpm, usually for a larger JavaScript workspace or a team that already
standardizes on pnpm.

Copy these files over the root equivalents:

- `package.json`
- `pnpm-workspace.yaml`

Then generate and commit the pnpm lockfile before enabling frozen installs:

```bash
pnpm install
git add pnpm-lock.yaml
```

Then update CI install commands from:

```bash
bun install --frozen-lockfile
```

to:

```bash
pnpm install --frozen-lockfile
```

Keep `bunfig.toml` out of pnpm projects unless Bun is still used for local scripts.

## Agent Notes

- Use this variant only when pnpm is already preferred or the TypeScript
  workspace needs pnpm's monorepo behavior.
- Update CI install commands and package scripts together.
- Do not enable `pnpm install --frozen-lockfile` until `pnpm-lock.yaml` is
  generated and committed.
- Keep `minimumReleaseAge: 10080` in `pnpm-workspace.yaml`.
