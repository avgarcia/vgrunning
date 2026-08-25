const { spawnSync } = require("node:child_process");
const { resolve } = require("node:path");

const frontend = resolve(__dirname, "..");
const spectral = process.platform === "win32"
  ? resolve(frontend, "node_modules", ".bin", "spectral.cmd")
  : resolve(frontend, "node_modules", ".bin", "spectral");
const ruleset = resolve(frontend, "..", "api", "openapi", ".spectral.yaml");
const fixtures = [
  "role-prefix.yaml",
  "secret-query.yaml",
  "get-request-body.yaml",
  "duplicate-operation-id.yaml",
];

for (const fixture of fixtures) {
  const result = spawnSync(spectral, ["lint", "--ruleset", ruleset, resolve(frontend, "..", "api", "openapi", "test-fixtures", "spectral", fixture)], {
    encoding: "utf8",
  });
  if (result.status === 0) {
    throw new Error(`Spectral aceptó el fixture inválido ${fixture}.`);
  }
}
