import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ApiClientProvider } from "./api/ApiClientProvider";
import { App } from "./App";

describe("App", () => {
  it("renders the accessible technical shell with the generated client configuration", () => {
    render(
      <ApiClientProvider>
        <App />
      </ApiClientProvider>,
    );

    expect(screen.getByRole("heading", { level: 1, name: "Running Coach" })).toBeVisible();
    expect(screen.getByText("La base de la aplicación está preparada.")).toBeVisible();
    expect(screen.getByRole("main")).toHaveAttribute("data-api-base-path", "/api");
  });
});
