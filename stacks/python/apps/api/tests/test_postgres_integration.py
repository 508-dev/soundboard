from __future__ import annotations

import os

import pytest
from sqlalchemy import create_engine, text

from example_shared.settings import get_settings


@pytest.mark.integration
def test_postgres_connection() -> None:
    if os.environ.get("RUN_INTEGRATION_TESTS") != "1":
        pytest.skip("set RUN_INTEGRATION_TESTS=1 and start compose postgres to run")

    engine = create_engine(get_settings().database_url)
    with engine.connect() as connection:
        assert connection.execute(text("select 1")).scalar_one() == 1
