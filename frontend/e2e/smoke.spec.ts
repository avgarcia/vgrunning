import { expect, test } from "@playwright/test";

test("loads the synthetic SPA shell without browser errors", async ({ page }) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") {
      browserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  await page.goto("/");

  await expect(page).toHaveTitle("Running Coach");
  await expect(page.getByRole("heading", { level: 1, name: "Running Coach" })).toBeVisible();
  await expect(page.getByRole("main")).toHaveAttribute("data-api-base-path", "/api");
  expect(browserErrors).toEqual([]);
});
