from __future__ import annotations

import uvicorn
from fastapi import FastAPI

from example_shared.observability import configure_logging
from example_shared.schemas import HealthResponse
from example_shared.settings import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)
    app = FastAPI(title="508 Devkit API")

    @app.get("/health", response_model=HealthResponse)
    def health() -> HealthResponse:
        # Keep this endpoint dependency-free. Add separate readiness checks when
        # a target service has real database or queue dependencies.
        return HealthResponse(service=settings.otel_service_name, status="ok")

    return app


def main() -> None:
    settings = get_settings()
    uvicorn.run(
        "example_api.main:create_app",
        factory=True,
        host=settings.api_host,
        port=settings.api_port,
    )


if __name__ == "__main__":
    main()
