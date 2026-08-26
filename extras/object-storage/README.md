# Object Storage Extra

Use this extra only when a project needs local S3-compatible object storage.
Object storage is not a root default because many projects do not need it, and
the right provider or emulator depends on the product.

## Contains

- `compose.object-storage.yml.example`: opt-in MinIO services for Docker
  Compose, based on the 508-workflows local infrastructure pattern.

## MinIO Notes

The example uses Chainguard images:

- `cgr.dev/chainguard/minio` for the server.
- `cgr.dev/chainguard/minio-client` for one-time bucket creation.

Keep the server and client images explicit. Official and Chainguard MinIO image
contents and entrypoints can differ, and prior downstream work hit issues when
assuming one image could safely cover both server runtime and client setup.

The `minio-init` service waits for the server healthcheck and runs:

```bash
mc mb --ignore-existing local/${MINIO_INTERNAL_BUCKET:-internal-transfers}
```

This keeps bucket creation idempotent and makes app services depend on
`minio-init` completing successfully instead of racing the first upload.

## Agent Notes

- Do not add this extra unless the target repo actually needs object storage.
- Prefer an explicit bucket-init service over ad hoc app startup bucket
  creation.
- Use `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` as env-loaded fields.
  `MINIO_ACCESS_KEY` and `MINIO_SECRET_KEY` may be application aliases, but do
  not assume MinIO itself reads those names.
- Add worktree-safe host ports only when the service is exposed to the host.
  Internal Compose consumers should use `http://minio:9000`.
- If copying this into a repo with existing object storage config, adapt names
  and bucket policy to the repo instead of preserving `internal-transfers`.
