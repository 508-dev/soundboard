# Observability

508 Devkit includes environment names for logs, Sentry, and OTEL, but does not force a telemetry vendor or SDK into every project.

Root defaults:

- `LOG_LEVEL`
- `SENTRY_DSN`
- `OTEL_EXPORTER_OTLP_ENDPOINT`
- `OTEL_SERVICE_NAME`

Use `example_shared.observability.configure_logging` as the minimal runtime hook. Add Sentry or OpenTelemetry SDK initialization only after the target service chooses those dependencies.

## When To Add SDKs

Add Sentry when the project has a real error reporting destination and release/environment naming.

Add OpenTelemetry when the project has a collector, trace sampling policy, and service naming convention.

Keep SDK initialization in one shared helper per runtime so API, worker, and job processes behave consistently.

## Agent Notes

- Do not add telemetry SDK dependencies just because the env vars exist.
- Add Sentry, OTEL, metrics, or log shipping only after the target repo has a
  destination and naming convention.
- Keep one runtime-local initialization helper so services do not configure
  observability differently by accident.
