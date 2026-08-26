# Add Service

Use when adding a new `apps/*` service to a repo that follows 508 Devkit conventions.

## Workflow

1. Inspect existing apps and package naming.
2. Choose the smallest service shape that fits: API, worker, CLI, scheduled job, or web workspace.
3. Add package metadata, source layout, tests, and local scripts.
4. Wire settings through Pydantic or the repo's existing boundary schema.
5. Update Compose only when the service needs local infrastructure.
6. Update CI path filters and docs.
7. Run narrow checks for the new service.
