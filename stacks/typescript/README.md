# TypeScript Stack

Use this stack when the target repository needs TypeScript-side conventions,
whether or not it has a browser-facing app.

This stack is framework-neutral. It captures package scripts, TypeScript,
Biome, Vitest, Drizzle examples, and environment naming. It is not a Vite,
Next.js, Astro, TanStack Start, or Expo scaffold.

## Agent Notes

- Inspect the target repo before copying this stack.
- Keep the stack if the repo needs TypeScript tooling, shared TypeScript
  contracts, Drizzle schema examples, or frontend conventions.
- Do not infer a frontend framework from this stack. Choose a framework only
  after the product shape and deployment target are clear.
- Replace placeholder tables, package names, and env values with target-specific
  contracts.
- Run `bun run lint`, `bun run typecheck`, `bun run test`, and `bun run build`
  after adapting the stack.

## Files

- `package.json`: stack-local scripts and dependencies.
- `src/index.ts`: neutral API base URL helper.
- `src/db/schema.ts`: placeholder Drizzle schema example.
- `tests/`: Vitest example coverage.
- `pnpm/`: package-manager stack variant for larger TypeScript workspaces.
