from __future__ import annotations

from sqlalchemy import MetaData
from sqlalchemy.orm import DeclarativeBase

metadata = MetaData()


class Base(DeclarativeBase):
    # Export one declarative base so future models and Alembic share metadata.
    metadata = metadata
