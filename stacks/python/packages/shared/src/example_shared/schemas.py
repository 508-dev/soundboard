from __future__ import annotations

from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    service: str
    status: str = Field(pattern="^(ok|degraded|down)$")
