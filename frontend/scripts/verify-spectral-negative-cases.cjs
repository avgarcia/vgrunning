const { spawnSync } = require("node:child_process");
const { resolve } = require("node:path");

const frontend = resolve(__dirname, "..");
const spectral = resolve(frontend, "node_modules", "@stoplight", "spectral-cli", "dist", "index.js");
const ruleset = resolve(frontend, "..", "api", "openapi", ".spectral.yaml");
const fixtures = new Map([
  ["role-prefix.yaml", "no-role-prefixes"],
  ["secret-query.yaml", "no-secrets-in-url"],
  ["secret-path.yaml", "no-secrets-in-url"],
  ["get-request-body.yaml", "no-get-request-body"],
  ["duplicate-operation-id.yaml", "operation-id-unique"],
  ["version-prefix.yaml", "no-version-prefix"],
  ["missing-operation-id.yaml", "operation-id-required"],
  ["invalid-post-response.yaml", "http-semantics"],
  ["missing-problem-details.yaml", "http-semantics"],
  ["wrong-problem-schema.yaml", "http-semantics"],
  ["missing-csrf.yaml", "http-semantics"],
  ["csrf-on-get.yaml", "http-semantics"],
  ["optional-path-parameter.yaml", "http-semantics"],
  ["unpaginated-collection.yaml", "http-semantics"],
  ["open-write-schema.yaml", "closed-object-schemas"],
  ["unsafe-example-secret.yaml", "safe-examples"],
]);
const fixturesDirectory = resolve(frontend, "..", "api", "openapi", "test-fixtures", "spectral");

for (const [fixture, expectedRule] of fixtures) {
  const result = spawnSync(process.execPath, [spectral, "lint", "--ruleset", ruleset, resolve(fixturesDirectory, fixture)], {
    encoding: "utf8",
  });
  if (result.error) {
    throw result.error;
  }
  if (result.status === 0) {
    throw new Error(`Spectral aceptó el fixture inválido ${fixture}.`);
  }
  const output = `${result.stdout}\n${result.stderr}`;
  if (!output.includes(expectedRule)) {
    throw new Error(`El fixture ${fixture} falló, pero no activó la regla esperada ${expectedRule}.\n${output}`);
  }
}

const validFixture = "valid-resource.yaml";
const validResult = spawnSync(process.execPath, [spectral, "lint", "--ruleset", ruleset, resolve(fixturesDirectory, validFixture)], {
  encoding: "utf8",
});
if (validResult.error) {
  throw validResult.error;
}
if (validResult.status !== 0) {
  throw new Error(`Spectral rechazó el fixture válido ${validFixture}.\n${validResult.stdout}\n${validResult.stderr}`);
}
