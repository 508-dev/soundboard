export function apiBaseUrl(env: Record<string, string | undefined> = {}): string {
  // Use a neutral env name in the stack. Map it to NEXT_PUBLIC_*, VITE_*, or
  // another framework-specific public variable only after choosing a framework.
  return env.WEB_API_BASE_URL ?? "http://127.0.0.1:8720";
}
