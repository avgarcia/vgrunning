import { fileURLToPath, URL } from "node:url";

import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const repositoryRoot = fileURLToPath(new URL("..", import.meta.url));
const generatedApiClient = fileURLToPath(
  new URL("../build/generated/openapi/client/typescript", import.meta.url),
);
const generatedFrontend = fileURLToPath(new URL("../build/generated/frontend", import.meta.url));
const axiosEntry = fileURLToPath(new URL("./node_modules/axios/index.js", import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@running-coach/api-client": generatedApiClient,
      axios: axiosEntry,
    },
  },
  server: {
    fs: {
      allow: [repositoryRoot],
    },
  },
  build: {
    emptyOutDir: true,
    outDir: generatedFrontend,
  },
  preview: {
    host: "127.0.0.1",
    port: 4173,
    strictPort: true,
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}"],
    setupFiles: "./src/test/setup.ts",
  },
});
