from __future__ import annotations

import logging

from example_shared.settings import Settings


def configure_logging(level: str) -> None:
    logging.basicConfig(
        level=getattr(logging, level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )


def observability_summary(settings: Settings) -> dict[str, str | bool | None]:
    return {
        "environment": settings.environment,
        "log_level": settings.log_level,
        "service_name": settings.otel_service_name,
        "sentry_enabled": settings.sentry_dsn is not None,
        "otel_endpoint": settings.otel_exporter_otlp_endpoint,
    }
