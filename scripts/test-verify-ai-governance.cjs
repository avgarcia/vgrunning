#!/usr/bin/env node

const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const sourceRoot = path.resolve(__dirname, "..");
const fixtureRoot = fs.mkdtempSync(path.join(os.tmpdir(), "running-coach-ai-governance-"));
const checker = path.resolve(__dirname, "verify-ai-governance.cjs");
const files = [
  "AGENTS.md",
  "docs/ai-governance.md",
  ".agents/skills/implementar-slice/SKILL.md",
  ".agents/skills/implementar-slice/agents/openai.yaml",
  "config/linear-agent/team-guidance.md",
  "config/linear-agent/preparar-slice-running-coach.md",
];

function run() {
  return spawnSync(process.execPath, [checker, fixtureRoot], { encoding: "utf8" });
}

try {
  for (const relativePath of files) {
    const destination = path.join(fixtureRoot, relativePath);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.copyFileSync(path.join(sourceRoot, relativePath), destination);
  }

  const valid = run();
  if (valid.status !== 0) {
    throw new Error(`El fixture válido fue rechazado: ${valid.stderr ?? valid.error?.message ?? "sin salida"}`);
  }

  const metadataPath = path.join(fixtureRoot, ".agents/skills/implementar-slice/agents/openai.yaml");
  const metadata = fs.readFileSync(metadataPath, "utf8");
  fs.writeFileSync(metadataPath, metadata.replace("allow_implicit_invocation: false", "allow_implicit_invocation: true"));
  const implicit = run();
  if (implicit.status !== 1 || !implicit.stderr.includes("allow_implicit_invocation: false")) {
    throw new Error("El verificador no rechazó la invocación implícita.");
  }

  fs.writeFileSync(metadataPath, metadata, "utf8");
  const skillPath = path.join(fixtureRoot, ".agents/skills/implementar-slice/SKILL.md");
  const skill = fs.readFileSync(skillPath, "utf8");
  fs.writeFileSync(skillPath, skill.replace("No apruebes ni fusiones", "Aprueba y fusiona"), "utf8");
  const merge = run();
  if (merge.status !== 1 || !merge.stderr.includes("No apruebes ni fusiones")) {
    throw new Error("El verificador no rechazó una Skill que autoriza el merge.");
  }

  console.log("Autoprueba del gobierno de IA superada con fixtures mínimos.");
} finally {
  fs.rmSync(fixtureRoot, { recursive: true, force: true });
}
