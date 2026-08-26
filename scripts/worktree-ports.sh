#!/usr/bin/env sh
set -eu

BASE_PORT=8700
SPAN=1000
PORT_BLOCK_SIZE=100
RESERVED_BLOCK_SIZE_DEFAULT=10

# Keep this list in sync with browser-restricted local ports. The API is also
# browser-facing because web apps often call it with fetch(), so both API_PORT
# and WEB_PORT are sanitized through chrome_safe_port().
WEB_RESTRICTED_PORTS=" 1 7 9 11 13 15 17 19 20 21 22 23 25 37 42 43 53 69 77 79 87 95 101 102 103 104 109 110 111 113 115 117 119 123 135 137 139 143 161 179 389 427 465 512 513 514 515 526 530 531 532 540 548 554 556 563 587 601 636 989 990 993 995 1719 1720 1723 2049 3659 4045 5060 5061 6000 6566 6665 6666 6667 6668 6669 6697 10080 "

usage() {
  echo "usage: worktree-ports.sh [env|export|exec [KEY=VALUE ...] -- <command>]" >&2
}

worktree_root() {
  if root="$(git rev-parse --show-toplevel 2>/dev/null)"; then
    printf '%s\n' "$root"
  else
    pwd -P
  fi
}

hash_hex() {
  root="$1"
  # macOS ships shasum, Linux commonly ships sha256sum, and openssl is a
  # practical fallback. Avoid Python/Node here so root scripts stay
  # language-neutral before any stack-specific dependencies are installed.
  if command -v shasum >/dev/null 2>&1; then
    printf '%s' "$root" | shasum -a 256 | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    printf '%s' "$root" | sha256sum | awk '{print $1}'
  elif command -v openssl >/dev/null 2>&1; then
    printf '%s' "$root" | openssl dgst -sha256 | awk '{print $NF}'
  else
    echo "worktree-ports.sh requires shasum, sha256sum, or openssl" >&2
    return 1
  fi
}

hex_to_decimal() {
  # POSIX shell arithmetic does not portably parse 32-bit hex values, so awk
  # does the conversion in a way that works on macOS and Linux.
  awk -v hex="$1" '
    BEGIN {
      decimal = 0
      digits = "0123456789abcdef"
      hex = tolower(hex)
      for (pos = 1; pos <= length(hex); pos++) {
        value = index(digits, substr(hex, pos, 1)) - 1
        decimal = (decimal * 16) + value
      }
      print decimal
    }
  '
}

port_block() {
  # Hash the absolute worktree path so sibling git worktrees receive stable but
  # different port blocks without a central registry.
  root="$(worktree_root)"
  digest="$(hash_hex "$root")"
  prefix="$(printf '%s' "$digest" | cut -c 1-8)"
  value="$(hex_to_decimal "$prefix")"
  blocks=$((SPAN / PORT_BLOCK_SIZE))
  echo $((BASE_PORT + ((value % blocks) * PORT_BLOCK_SIZE)))
}

is_positive_integer() {
  case "${1:-}" in
    ''|*[!0-9]*) return 1 ;;
    *) [ "$1" -gt 0 ] ;;
  esac
}

reserved_block_start() {
  if is_positive_integer "${WORKTREE_PORT_BLOCK_START:-}"; then
    printf '%s\n' "$WORKTREE_PORT_BLOCK_START"
    return 0
  fi
  return 1
}

reserved_block_size() {
  if is_positive_integer "${WORKTREE_PORT_BLOCK_SIZE:-}"; then
    printf '%s\n' "$WORKTREE_PORT_BLOCK_SIZE"
  else
    printf '%s\n' "$RESERVED_BLOCK_SIZE_DEFAULT"
  fi
}

is_web_restricted_port() {
  case "$WEB_RESTRICTED_PORTS" in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

chrome_safe_port() {
  port="$1"
  while is_web_restricted_port "$port"; do
    port=$((port + 1))
  done
  echo "$port"
}

is_used_port() {
  case "$2" in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

unused_port_in_block() {
  port="$1"
  end="$2"
  used="$3"
  while [ "$port" -le "$end" ]; do
    if ! is_used_port "$port" "$used"; then
      echo "$port"
      return 0
    fi
    port=$((port + 1))
  done
  echo "reserved port block does not have enough free ports" >&2
  return 1
}

browser_safe_unused_port_in_block() {
  port="$1"
  end="$2"
  used="$3"
  while [ "$port" -le "$end" ]; do
    if ! is_web_restricted_port "$port" && ! is_used_port "$port" "$used"; then
      echo "$port"
      return 0
    fi
    port=$((port + 1))
  done
  echo "reserved port block does not have enough browser-safe free ports" >&2
  return 1
}

apply_primary_port_reservation() {
  if ! is_positive_integer "${WORKTREE_PRIMARY_PORT:-}"; then
    return 0
  fi

  primary="$(chrome_safe_port "$WORKTREE_PRIMARY_PORT")"
  target="${WORKTREE_PRIMARY_PORT_TARGET:-WEB_PORT}"
  case "$target" in
    API_PORT)
      API_PORT="$primary"
      ;;
    WEB_PORT)
      WEB_PORT="$primary"
      ;;
    *)
      echo "WORKTREE_PRIMARY_PORT_TARGET must be API_PORT or WEB_PORT" >&2
      return 1
      ;;
  esac
}

validate_unique_ports() {
  used=" "
  for port in "$API_PORT" "$WEB_PORT" "$WORKER_HEALTH_PORT" "$POSTGRES_HOST_PORT" "$REDIS_HOST_PORT" "$OTEL_HTTP_PORT"; do
    if is_used_port "$port" "$used"; then
      echo "worktree port reservation produced duplicate port ${port}; adjust WORKTREE_PRIMARY_PORT, WORKTREE_PRIMARY_PORT_TARGET, or WORKTREE_PORT_BLOCK_*" >&2
      return 1
    fi
    used="${used}${port} "
  done
}

calculate_ports() {
  if base="$(reserved_block_start)"; then
    size="$(reserved_block_size)"
    if [ "$size" -lt 6 ]; then
      echo "WORKTREE_PORT_BLOCK_SIZE must be at least 6" >&2
      return 1
    fi
    # A reserved block is usually small, so use compact offsets inside it.
    end=$((base + size - 1))
    used=" "
    WEB_PORT="$(browser_safe_unused_port_in_block "$base" "$end" "$used")"
    used="${used}${WEB_PORT} "
    API_PORT="$(browser_safe_unused_port_in_block "$((base + 1))" "$end" "$used")"
    used="${used}${API_PORT} "
    WORKER_HEALTH_PORT="$(unused_port_in_block "$((base + 2))" "$end" "$used")"
    used="${used}${WORKER_HEALTH_PORT} "
    POSTGRES_HOST_PORT="$(unused_port_in_block "$((base + 3))" "$end" "$used")"
    used="${used}${POSTGRES_HOST_PORT} "
    REDIS_HOST_PORT="$(unused_port_in_block "$((base + 4))" "$end" "$used")"
    used="${used}${REDIS_HOST_PORT} "
    OTEL_HTTP_PORT="$(unused_port_in_block "$((base + 5))" "$end" "$used")"
  else
    base="$(port_block)"
    # Offsets are intentionally sparse. Future services can claim unused slots
    # without changing existing ports for API, web, database, or cache examples.
    API_PORT="$(chrome_safe_port "$((base + 20))")"
    WEB_PORT="$(chrome_safe_port "$((base + 30))")"
    WORKER_HEALTH_PORT="$((base + 35))"
    POSTGRES_HOST_PORT="$((base + 40))"
    REDIS_HOST_PORT="$((base + 50))"
    OTEL_HTTP_PORT="$((base + 80))"
  fi

  apply_primary_port_reservation
  validate_unique_ports

  POSTGRES_URL="postgresql://app:app@127.0.0.1:${POSTGRES_HOST_PORT}/app"
  DATABASE_URL="$POSTGRES_URL"
  REDIS_URL="redis://127.0.0.1:${REDIS_HOST_PORT}/0"
  WEB_URL="http://127.0.0.1:${WEB_PORT}"
  WEB_API_BASE_URL="http://127.0.0.1:${API_PORT}"
  OTEL_EXPORTER_OTLP_ENDPOINT="http://127.0.0.1:${OTEL_HTTP_PORT}"
}

print_env() {
  prefix="$1"
  calculate_ports
  printf '%sWEB_URL=%s\n' "$prefix" "$WEB_URL"
  printf '%sWEB_PORT=%s\n' "$prefix" "$WEB_PORT"
  printf '%sAPI_PORT=%s\n' "$prefix" "$API_PORT"
  printf '%sWORKER_HEALTH_PORT=%s\n' "$prefix" "$WORKER_HEALTH_PORT"
  printf '%sPOSTGRES_HOST_PORT=%s\n' "$prefix" "$POSTGRES_HOST_PORT"
  printf '%sREDIS_HOST_PORT=%s\n' "$prefix" "$REDIS_HOST_PORT"
  printf '%sOTEL_HTTP_PORT=%s\n' "$prefix" "$OTEL_HTTP_PORT"
  printf '%sPOSTGRES_URL=%s\n' "$prefix" "$POSTGRES_URL"
  printf '%sDATABASE_URL=%s\n' "$prefix" "$DATABASE_URL"
  printf '%sREDIS_URL=%s\n' "$prefix" "$REDIS_URL"
  printf '%sWEB_API_BASE_URL=%s\n' "$prefix" "$WEB_API_BASE_URL"
  printf '%sOTEL_EXPORTER_OTLP_ENDPOINT=%s\n' "$prefix" "$OTEL_EXPORTER_OTLP_ENDPOINT"
}

export_env() {
  calculate_ports
  export API_PORT WEB_PORT WORKER_HEALTH_PORT
  export POSTGRES_HOST_PORT REDIS_HOST_PORT
  export OTEL_HTTP_PORT POSTGRES_URL DATABASE_URL REDIS_URL
  export WEB_URL WEB_API_BASE_URL OTEL_EXPORTER_OTLP_ENDPOINT
}

has_override() {
  key="$1"
  while IFS= read -r assignment; do
    case "$assignment" in
      "$key="*) return 0 ;;
    esac
  done <<EOF
$overrides
EOF
  return 1
}

refresh_derived_env_after_overrides() {
  if ! has_override POSTGRES_URL; then
    POSTGRES_URL="postgresql://app:app@127.0.0.1:${POSTGRES_HOST_PORT}/app"
  fi
  if ! has_override DATABASE_URL; then
    DATABASE_URL="$POSTGRES_URL"
  fi
  if ! has_override REDIS_URL; then
    REDIS_URL="redis://127.0.0.1:${REDIS_HOST_PORT}/0"
  fi
  if ! has_override WEB_URL; then
    WEB_URL="http://127.0.0.1:${WEB_PORT}"
  fi
  if ! has_override WEB_API_BASE_URL; then
    WEB_API_BASE_URL="http://127.0.0.1:${API_PORT}"
  fi
  if ! has_override OTEL_EXPORTER_OTLP_ENDPOINT; then
    OTEL_EXPORTER_OTLP_ENDPOINT="http://127.0.0.1:${OTEL_HTTP_PORT}"
  fi
  export POSTGRES_URL DATABASE_URL REDIS_URL
  export WEB_URL WEB_API_BASE_URL OTEL_EXPORTER_OTLP_ENDPOINT
}

run_with_env() {
  if [ "$#" -eq 0 ]; then
    usage
    return 2
  fi

  # Allow one-off overrides before "--", matching the Python helper:
  #   ./scripts/worktree-ports.sh exec API_PORT=9000 -- ./scripts/dev.sh
  overrides=""
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --)
        shift
        break
        ;;
      *=*)
        export "$1"
        overrides="${overrides}
$1"
        shift
        ;;
      *)
        usage
        return 2
        ;;
    esac
  done

  if [ "$#" -eq 0 ]; then
    usage
    return 2
  fi

  export_env
  while IFS= read -r assignment; do
    if [ -n "$assignment" ]; then
      export "$assignment"
    fi
  done <<EOF
$overrides
EOF

  refresh_derived_env_after_overrides

  exec "$@"
}

command="${1:-env}"
if [ "$#" -gt 0 ]; then
  shift
fi

case "$command" in
  env)
    print_env ""
    ;;
  export)
    print_env "export "
    ;;
  exec)
    run_with_env "$@"
    ;;
  *)
    usage
    exit 2
    ;;
esac
