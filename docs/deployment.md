# Deployment

508 Devkit does not choose a deployment platform by default. Pick the smallest deployment shape that matches the product, team, and operational constraints.

## Decision Record

When applying this devkit to a project, record the deployment decision here:

- Target platform:
- Services deployed:
- Database and storage:
- Secrets management:
- Preview environment strategy:
- Rollback strategy:
- Production health checks:

## Common Options

| Option | Good Fit | Tradeoffs |
| --- | --- | --- |
| Fly.io | Small teams that want simple app hosting close to users. | Requires platform-specific config and operational familiarity. |
| Render | Straightforward web services, workers, and managed databases. | Less control than lower-level infrastructure. |
| Vercel or Cloudflare Pages | Frontend-first apps and edge-friendly web surfaces. | Backend, worker, and database workflows may need separate hosting. |
| Kamal | Teams that want Docker deploys to owned servers. | Requires server operations, registry setup, and rollback discipline. |
| Coolify | Self-hosted platform-style deploys. | Adds a platform to operate and upgrade. |
| Kubernetes | Larger teams with existing cluster operations. | Too much machinery for most new projects. |

## Workflow Guidance

Keep deployment workflows platform-specific and explicit. A project should add deploy CI only after the platform is chosen and secrets are configured.

Before enabling automatic production deploys:

1. Add a health endpoint or smoke test.
2. Document required environment variables.
3. Confirm rollback behavior.
4. Keep preview deploys separate from production deploys.
5. Use least-privilege deployment credentials.

## Agent Notes

- Do not infer a deployment platform from this devkit. Inspect the target repo,
  hosting account, and team preference first.
- Keep deployment workflows out of new repos until secrets and rollback are
  known.
- If deployment is undecided, leave the decision record blank rather than
  copying placeholder platform files.
