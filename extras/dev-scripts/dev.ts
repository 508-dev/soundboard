import { spawn } from "node:child_process";

const env = { ...process.env };

function run(name: string, command: string, args: string[]) {
  const child = spawn(command, args, { env, stdio: "inherit" });
  child.on("exit", (code, signal) => {
    if (signal) {
      process.kill(process.pid, signal);
      return;
    }
    if (code && code !== 0) {
      process.exit(code);
    }
  });
  child.on("error", (error) => {
    // biome-ignore lint/suspicious/noConsole: CLI scripts report process failures on stderr.
    console.error(`${name} failed: ${error.message}`);
    process.exit(1);
  });
  return child;
}

const api = run("api", "uv", [
  "run",
  "--package",
  "example-api",
  "uvicorn",
  "example_api.main:create_app",
  "--factory",
  "--host",
  env.API_HOST ?? "127.0.0.1",
  "--port",
  env.API_PORT ?? "8720",
  "--reload",
]);

const web = run("web", "bun", ["run", "--cwd", "stacks/typescript", "dev"]);

process.on("SIGINT", () => {
  api.kill("SIGTERM");
  web.kill("SIGTERM");
});
