#!/usr/bin/env node

const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repositoryRoot = fs.mkdtempSync(path.join(os.tmpdir(), "running-coach-documentation-"));
const checker = path.resolve(__dirname, "verify-documentation-links.cjs");

try {
  fs.mkdirSync(path.join(repositoryRoot, "docs"));
  fs.writeFileSync(path.join(repositoryRoot, "AGENTS.md"), "# Agent guidance\n", "utf8");
  fs.writeFileSync(
    path.join(repositoryRoot, "docs", "local.md"),
    "# Local environment\n\n[Self link](#local-environment)\n",
    "utf8",
  );
  fs.writeFileSync(path.join(repositoryRoot, "README.md"), "[Missing](docs/missing.md)\n", "utf8");

  const broken = spawnSync(process.execPath, [checker, repositoryRoot], { encoding: "utf8" });
  if (broken.status !== 1 || !broken.stderr.includes("destino inexistente")) {
    throw new Error("El verificador documental no rechazó un enlace local inexistente.");
  }

  fs.writeFileSync(path.join(repositoryRoot, "README.md"), "[Local](docs/local.md#local-environment)\n", "utf8");
  const valid = spawnSync(process.execPath, [checker, repositoryRoot], { encoding: "utf8" });
  if (valid.status !== 0) {
    throw new Error("El verificador documental rechazó un enlace válido: " + valid.stderr);
  }

  console.log("Autoprueba del verificador documental superada.");
} finally {
  fs.rmSync(repositoryRoot, { recursive: true, force: true });
}
