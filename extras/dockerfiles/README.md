# Dockerfile Alternates

These Dockerfiles are examples for deployment parity. They are not root defaults because deployment platform, build strategy, and runtime shape vary by project.

Copy and adapt only after choosing the service boundary and deployment target.

## Files

- `Dockerfile.api.example`: Python HTTP API image.
- `Dockerfile.worker.example`: Python worker image.
- `Dockerfile.web-typescript.example`: framework-neutral TypeScript check/build image.

For framework-specific web deploys, replace the web example with the framework's production build and runtime guidance.

## Agent Notes

- Do not add Dockerfiles until the deployment target and service boundaries are
  known.
- Keep build contexts small and confirm `.dockerignore` excludes secrets,
  caches, local dependencies, and `.context/`.
- Replace example package names and commands with target-specific entrypoints.
