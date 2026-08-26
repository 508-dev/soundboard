# Frontend

508 Devkit does not choose a frontend framework by default.

Next.js, Vite, TanStack Start, Astro, Expo, and other frameworks are all reasonable choices for different products. Pick the framework after inspecting the product shape, deployment target, routing needs, data-loading model, and team familiarity.

## Root Convention

The `stacks/typescript` package is a framework-neutral TypeScript workspace. It exists to capture shared JavaScript conventions:

- Bun package scripts.
- TypeScript typechecking.
- Biome formatting and linting.
- Vitest unit tests.
- Drizzle placeholder config as one TypeScript-side database access example
  when needed.

It is not meant to be copied as a finished web application.

## Public Environment Variables

The devkit uses neutral environment names such as `WEB_API_BASE_URL`. Map them to the framework-specific public env shape only after choosing a framework:

| Framework | Public Env Example |
| --- | --- |
| Next.js | `NEXT_PUBLIC_API_BASE_URL` |
| Vite | `VITE_API_BASE_URL` |
| TanStack Start | Use the framework's documented public env pattern. |
| Astro | `PUBLIC_API_BASE_URL` |

Avoid baking one framework's public env prefix into shared scripts or root docs.

## Agent Guidance

When bootstrapping a target repo, agents should ask or infer:

- Is the web surface an app, marketing site, dashboard, mobile shell, or no frontend at all?
- Does it need SSR, static output, edge runtime, API routes, file-based routing, or client-only rendering?
- Where will it deploy?
- Which frontend conventions already exist in the target repo?

If the answer is unclear, leave the frontend framework unselected and document the decision needed.

When adding a frontend later, update the public environment variable mapping,
CI job names, and development docs together so agents do not mix framework
conventions.
