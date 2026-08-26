#!/usr/bin/env python3
"""Emit stable local development ports for this git worktree."""

from __future__ import annotations

import hashlib
import os
import subprocess
import sys
from pathlib import Path


BASE_PORT = 8700
SPAN = 1000
PORT_BLOCK_SIZE = 100
RESERVED_BLOCK_SIZE_DEFAULT = 10
HASH_OFFSETS = {
    "WEB_PORT": 30,
    "API_PORT": 20,
    "WORKER_HEALTH_PORT": 35,
    "POSTGRES_HOST_PORT": 40,
    "REDIS_HOST_PORT": 50,
    "OTEL_HTTP_PORT": 80,
}
RESERVED_BLOCK_OFFSETS = {
    "WEB_PORT": 0,
    "API_PORT": 1,
    "WORKER_HEALTH_PORT": 2,
    "POSTGRES_HOST_PORT": 3,
    "REDIS_HOST_PORT": 4,
    "OTEL_HTTP_PORT": 5,
}
WEB_RESTRICTED_PORTS = frozenset(
    {
        1,
        7,
        9,
        11,
        13,
        15,
        17,
        19,
        20,
        21,
        22,
        23,
        25,
        37,
        42,
        43,
        53,
        69,
        77,
        79,
        87,
        95,
        101,
        102,
        103,
        104,
        109,
        110,
        111,
        113,
        115,
        117,
        119,
        123,
        135,
        137,
        139,
        143,
        161,
        179,
        389,
        427,
        465,
        512,
        513,
        514,
        515,
        526,
        530,
        531,
        532,
        540,
        548,
        554,
        556,
        563,
        587,
        601,
        636,
        989,
        990,
        993,
        995,
        1719,
        1720,
        1723,
        2049,
        3659,
        4045,
        5060,
        5061,
        6000,
        6566,
        6665,
        6666,
        6667,
        6668,
        6669,
        6697,
        10080,
    }
)


def worktree_root() -> Path:
    """Return the git worktree root, falling back to cwd outside git repos."""
    try:
        output = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"],
            stderr=subprocess.DEVNULL,
            text=True,
        ).strip()
        return Path(output)
    except Exception:
        return Path.cwd().resolve()


def port_block(root: Path) -> int:
    """Map a worktree path into a deterministic block of local ports."""
    digest = hashlib.sha256(str(root.resolve()).encode("utf-8")).hexdigest()
    return BASE_PORT + ((int(digest[:8], 16) % (SPAN // PORT_BLOCK_SIZE)) * PORT_BLOCK_SIZE)


def chrome_safe_port(port: int) -> int:
    while port in WEB_RESTRICTED_PORTS:
        port += 1
    return port


def unused_port_in_block(start: int, end: int, used: set[int], *, browser_safe: bool = False) -> int:
    port = start
    while port <= end:
        blocked = browser_safe and port in WEB_RESTRICTED_PORTS
        if not blocked and port not in used:
            return port
        port += 1
    kind = "browser-safe free" if browser_safe else "free"
    msg = f"reserved port block does not have enough {kind} ports"
    raise ValueError(msg)


def ports_for_base(base: int, offsets: dict[str, int] | None = None) -> dict[str, int]:
    """Apply offsets and sanitize browser-facing service ports."""
    selected_offsets = offsets or HASH_OFFSETS
    values = {name: base + offset for name, offset in selected_offsets.items()}
    values["WEB_PORT"] = chrome_safe_port(values["WEB_PORT"])
    values["API_PORT"] = chrome_safe_port(values["API_PORT"])
    return values


def ports_for_reserved_block(start: int, size: int) -> dict[str, int]:
    """Allocate compact, unique ports inside a reserved block."""
    if size < len(RESERVED_BLOCK_OFFSETS):
        msg = "WORKTREE_PORT_BLOCK_SIZE must be at least 6"
        raise ValueError(msg)

    end = start + size - 1
    used: set[int] = set()
    values: dict[str, int] = {}
    for name, offset in RESERVED_BLOCK_OFFSETS.items():
        values[name] = unused_port_in_block(
            start + offset,
            end,
            used,
            browser_safe=name in {"API_PORT", "WEB_PORT"},
        )
        used.add(values[name])
    return values


def validate_unique_ports(values: dict[str, int]) -> None:
    """Catch primary-port overrides that collide with generated service ports."""
    seen: set[int] = set()
    for port in values.values():
        if port in seen:
            msg = (
                f"worktree port reservation produced duplicate port {port}; "
                "adjust WORKTREE_PRIMARY_PORT, WORKTREE_PRIMARY_PORT_TARGET, or WORKTREE_PORT_BLOCK_*"
            )
            raise ValueError(msg)
        seen.add(port)


def positive_int(value: str | None) -> int | None:
    if value is None or not value.isdecimal():
        return None
    parsed = int(value)
    return parsed if parsed > 0 else None


def ports_for_environment(env: dict[str, str], root: Path | None = None) -> dict[str, int]:
    """Use generic orchestrator reservations when present, otherwise hash the worktree."""
    block_start = positive_int(env.get("WORKTREE_PORT_BLOCK_START"))
    if block_start is not None:
        block_size = positive_int(env.get("WORKTREE_PORT_BLOCK_SIZE")) or RESERVED_BLOCK_SIZE_DEFAULT
        values = ports_for_reserved_block(block_start, block_size)
    else:
        values = ports_for_base(port_block(root or worktree_root()))

    primary = positive_int(env.get("WORKTREE_PRIMARY_PORT"))
    if primary is not None:
        target = env.get("WORKTREE_PRIMARY_PORT_TARGET", "WEB_PORT")
        if target not in {"API_PORT", "WEB_PORT"}:
            msg = "WORKTREE_PRIMARY_PORT_TARGET must be API_PORT or WEB_PORT"
            raise ValueError(msg)
        values[target] = chrome_safe_port(primary)

    validate_unique_ports(values)
    return values


def env_values(env: dict[str, str] | None = None) -> dict[str, str]:
    """Return shell-friendly strings consumed by Compose and dev scripts."""
    values = ports_for_environment(env or os.environ)
    result = {name: str(port) for name, port in values.items()}
    result.update(derived_env_values(result))
    return ordered_env_values(result)


def derived_env_values(env: dict[str, str]) -> dict[str, str]:
    """Return URL values derived from the final port environment."""
    web = env["WEB_PORT"]
    api = env["API_PORT"]
    postgres = env["POSTGRES_HOST_PORT"]
    redis = env["REDIS_HOST_PORT"]
    otel = env["OTEL_HTTP_PORT"]
    postgres_url = f"postgresql://app:app@127.0.0.1:{postgres}/app"
    return {
        "WEB_URL": f"http://127.0.0.1:{web}",
        "POSTGRES_URL": postgres_url,
        "DATABASE_URL": postgres_url,
        "REDIS_URL": f"redis://127.0.0.1:{redis}/0",
        "WEB_API_BASE_URL": f"http://127.0.0.1:{api}",
        "OTEL_EXPORTER_OTLP_ENDPOINT": f"http://127.0.0.1:{otel}",
    }


def ordered_env_values(env: dict[str, str]) -> dict[str, str]:
    """Keep WEB_URL first for URL scanners while preserving shell-friendly keys."""
    result = {
        "WEB_URL": env["WEB_URL"],
        "WEB_PORT": env["WEB_PORT"],
        "API_PORT": env["API_PORT"],
        "WORKER_HEALTH_PORT": env["WORKER_HEALTH_PORT"],
        "POSTGRES_HOST_PORT": env["POSTGRES_HOST_PORT"],
        "REDIS_HOST_PORT": env["REDIS_HOST_PORT"],
        "OTEL_HTTP_PORT": env["OTEL_HTTP_PORT"],
        "POSTGRES_URL": env["POSTGRES_URL"],
        "DATABASE_URL": env["DATABASE_URL"],
        "REDIS_URL": env["REDIS_URL"],
        "WEB_API_BASE_URL": env["WEB_API_BASE_URL"],
        "OTEL_EXPORTER_OTLP_ENDPOINT": env["OTEL_EXPORTER_OTLP_ENDPOINT"],
    }
    return result


def print_env(export: bool = False) -> None:
    for key, value in env_values().items():
        prefix = "export " if export else ""
        print(f"{prefix}{key}={value}")


def run_with_env(args: list[str]) -> int:
    if not args:
        print("usage: worktree-ports.py exec [KEY=VALUE ...] -- <command> [args...]", file=sys.stderr)
        return 2

    index = 0
    overrides: dict[str, str] = {}
    while index < len(args):
        token = args[index]
        if "=" not in token or token.startswith("-"):
            break
        key, value = token.split("=", 1)
        overrides[key] = value
        index += 1

    if index >= len(args) or args[index] != "--":
        print("usage: worktree-ports.py exec [KEY=VALUE ...] -- <command> [args...]", file=sys.stderr)
        return 2

    command = args[index + 1 :]
    if not command:
        print("usage: worktree-ports.py exec [KEY=VALUE ...] -- <command> [args...]", file=sys.stderr)
        return 2

    # Apply overrides before generation so generic reservation inputs affect the
    # computed ports, then apply them again so direct API_PORT=... style
    # overrides still win over generated values.
    env = os.environ.copy()
    env.update(overrides)
    env.update(env_values(env))
    env.update(overrides)
    for key, value in derived_env_values(env).items():
        if key not in overrides:
            env[key] = value

    return subprocess.run(command, env=env, check=False).returncode


def main() -> int:
    command = sys.argv[1] if len(sys.argv) > 1 else "env"
    if command == "env":
        print_env()
        return 0
    if command == "export":
        print_env(export=True)
        return 0
    if command == "exec":
        return run_with_env(sys.argv[2:])
    print("usage: worktree-ports.py [env|export|exec [KEY=VALUE ...] -- <command>]", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
