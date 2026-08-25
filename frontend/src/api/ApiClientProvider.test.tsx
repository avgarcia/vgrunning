import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useApiClient } from "./ApiClientContext";
import { ApiClientProvider } from "./ApiClientProvider";

describe("ApiClientProvider", () => {
  it("configures relative same-origin requests with credentials", () => {
    const { result } = renderHook(() => useApiClient(), { wrapper: ApiClientProvider });

    expect(result.current.basePath).toBe("/api");
    expect(result.current.baseOptions).toMatchObject({ withCredentials: true });
  });
});
