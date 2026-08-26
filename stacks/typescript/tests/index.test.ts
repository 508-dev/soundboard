import { describe, expect, it } from "vitest";

import { apiBaseUrl } from "../src";

describe("apiBaseUrl", () => {
  it("uses the configured public API URL", () => {
    expect(apiBaseUrl({ WEB_API_BASE_URL: "http://localhost:9999" })).toBe("http://localhost:9999");
  });
});
