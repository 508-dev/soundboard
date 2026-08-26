#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { resolve } from "node:path";

const BASE_PORT = 8700;
const SPAN = 1000;
const PORT_BLOCK_SIZE = 100;
const RESERVED_BLOCK_SIZE_DEFAULT = 10;
const OFFSETS = {
  WEB_PORT: 30,
  API_PORT: 20,
  WORKER_HEALTH_PORT: 35,
  POSTGRES_HOST_PORT: 40,
  REDIS_HOST_PORT: 50,
  OTEL_HTTP_PORT: 80,
};
const RESERVED_BLOCK_OFFSETS = {
  WEB_PORT: 0,
  API_PORT: 1,
  WORKER_HEALTH_PORT: 2,
  POSTGRES_HOST_PORT: 3,
  REDIS_HOST_PORT: 4,
  OTEL_HTTP_PORT: 5,
};
const WEB_RESTRICTED_PORTS = new Set([
  1, 7, 9, 11, 13, 15, 17, 19, 20, 21, 22, 23, 25, 37, 42, 43, 53, 69, 77, 79, 87, 95, 101, 102,
  103, 104, 109, 110, 111, 113, 115, 117, 119, 123, 135, 137, 139, 143, 161, 179, 389, 427, 465,
  512, 513, 514, 515, 526, 530, 531, 532, 540, 548, 554, 556, 563, 587, 601, 636, 989, 990, 993,
  995, 1719, 1720, 1723, 2049, 3659, 4045, 5060, 5061, 6000, 6566, 6665, 6666, 6667, 6668, 6669,
  6697, 10080,
]);

function worktreeRoot() {
  try {
    return execFileSync("git", ["rev-parse", "--show-toplevel"], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
  } catch {
    return process.cwd();
  }
}

function portBlock(root) {
  const digest = createHash("sha256").update(resolve(root)).digest("hex");
  return (
    BASE_PORT +
    (Number.parseInt(digest.slice(0, 8), 16) % (SPAN / PORT_BLOCK_SIZE)) * PORT_BLOCK_SIZE
  );
}

function chromeSafePort(port) {
  let nextPort = port;
  while (WEB_RESTRICTED_PORTS.has(nextPort)) {
    nextPort += 1;
  }
  return nextPort;
}

function positiveInteger(value) {
  if (!/^[0-9]+$/.test(value ?? "")) {
    return undefined;
  }
  const parsed = Number.parseInt(value, 10);
  return parsed > 0 ? parsed : undefined;
}

function unusedPortInBlock(start, end, used, { browserSafe = false } = {}) {
  for (let port = start; port <= end; port += 1) {
    const blocked = browserSafe && WEB_RESTRICTED_PORTS.has(port);
    if (!blocked && !used.has(port)) {
      return port;
    }
  }
  const kind = browserSafe ? "browser-safe free" : "free";
  throw new Error(`reserved port block does not have enough ${kind} ports`);
}

function portsForBase(base) {
  const values = Object.fromEntries(
    Object.entries(OFFSETS).map(([key, offset]) => [key, base + offset]),
  );
  values.WEB_PORT = chromeSafePort(values.WEB_PORT);
  values.API_PORT = chromeSafePort(values.API_PORT);
  return values;
}

function portsForReservedBlock(start, size) {
  if (size < Object.keys(RESERVED_BLOCK_OFFSETS).length) {
    throw new Error("WORKTREE_PORT_BLOCK_SIZE must be at least 6");
  }

  const end = start + size - 1;
  const used = new Set();
  const values = {};
  for (const [key, offset] of Object.entries(RESERVED_BLOCK_OFFSETS)) {
    values[key] = unusedPortInBlock(start + offset, end, used, {
      browserSafe: key === "WEB_PORT" || key === "API_PORT",
    });
    used.add(values[key]);
  }
  return values;
}

function validateUniquePorts(values) {
  const seen = new Set();
  for (const port of Object.values(values)) {
    if (seen.has(port)) {
      throw new Error(
        `worktree port reservation produced duplicate port ${port}; adjust WORKTREE_PRIMARY_PORT, WORKTREE_PRIMARY_PORT_TARGET, or WORKTREE_PORT_BLOCK_*`,
      );
    }
    seen.add(port);
  }
}

function portsForEnvironment(env = process.env) {
  const blockStart = positiveInteger(env.WORKTREE_PORT_BLOCK_START);
  const values =
    blockStart === undefined
      ? portsForBase(portBlock(worktreeRoot()))
      : portsForReservedBlock(
          blockStart,
          positiveInteger(env.WORKTREE_PORT_BLOCK_SIZE) ?? RESERVED_BLOCK_SIZE_DEFAULT,
        );

  const primary = positiveInteger(env.WORKTREE_PRIMARY_PORT);
  if (primary !== undefined) {
    const target = env.WORKTREE_PRIMARY_PORT_TARGET ?? "WEB_PORT";
    if (target !== "API_PORT" && target !== "WEB_PORT") {
      throw new Error("WORKTREE_PRIMARY_PORT_TARGET must be API_PORT or WEB_PORT");
    }
    values[target] = chromeSafePort(primary);
  }

  validateUniquePorts(values);
  return values;
}

function envValues(env = process.env) {
  const values = portsForEnvironment(env);
  const postgresUrl = `postgresql://app:app@127.0.0.1:${values.POSTGRES_HOST_PORT}/app`;
  return {
    WEB_URL: `http://127.0.0.1:${values.WEB_PORT}`,
    WEB_PORT: String(values.WEB_PORT),
    API_PORT: String(values.API_PORT),
    WORKER_HEALTH_PORT: String(values.WORKER_HEALTH_PORT),
    POSTGRES_HOST_PORT: String(values.POSTGRES_HOST_PORT),
    REDIS_HOST_PORT: String(values.REDIS_HOST_PORT),
    OTEL_HTTP_PORT: String(values.OTEL_HTTP_PORT),
    POSTGRES_URL: postgresUrl,
    DATABASE_URL: postgresUrl,
    REDIS_URL: `redis://127.0.0.1:${values.REDIS_HOST_PORT}/0`,
    WEB_API_BASE_URL: `http://127.0.0.1:${values.API_PORT}`,
    OTEL_EXPORTER_OTLP_ENDPOINT: `http://127.0.0.1:${values.OTEL_HTTP_PORT}`,
  };
}

const command = process.argv[2] ?? "env";

try {
  if (command === "env") {
    const values = envValues();
    for (const [key, value] of Object.entries(values)) {
      // biome-ignore lint/suspicious/noConsole: CLI output is consumed by humans and shell scripts.
      console.log(`${key}=${value}`);
    }
  } else if (command === "export") {
    const values = envValues();
    for (const [key, value] of Object.entries(values)) {
      // biome-ignore lint/suspicious/noConsole: CLI output is consumed by humans and shell scripts.
      console.log(`export ${key}=${value}`);
    }
  } else {
    // biome-ignore lint/suspicious/noConsole: CLI usage errors belong on stderr.
    console.error("usage: worktree-ports.mjs [env|export]");
    process.exit(2);
  }
} catch (error) {
  // biome-ignore lint/suspicious/noConsole: CLI usage errors belong on stderr.
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}
