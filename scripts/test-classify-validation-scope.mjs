#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { classify } from "./classify-validation-scope.mjs";

const root = path.resolve(import.meta.dirname, "..");
const matrix = JSON.parse(fs.readFileSync(path.join(root, "config", "validation-matrix.json"), "utf8"));
const cases = [
  [["docs/adr/0024-hybrid-validation-ai-authority.md"], ["docsCheck", "gitleaks"], true],
  [["src/main/java/com/vgrunning/planning/api/Plan.java"], ["backendCheck", "codeql-java", "gitleaks"], false],
  [["src/main/resources/db/migration/V003__technical.sql"], ["backendCheck", "gitleaks", "postgresql-tests"], false],
  [["api/openapi/running-coach.yaml"], ["apiCheck", "backendCheck", "codeql-java", "codeql-javascript-typescript", "frontendCheck", "gitleaks"], true],
  [["frontend/src/App.tsx"], ["codeql-javascript-typescript", "frontendCheck", "gitleaks"], false],
  [["Dockerfile"], ["qualityGate"], false],
  [["scripts/test-classify-validation-scope.mjs"], ["qualityGate"], false],
  [["unclassified/file.txt"], ["qualityGate"], false],
];
for (const [files, gates, semanticReview] of cases) {
  const plan = classify(files, matrix);
  if (JSON.stringify(plan.gates) !== JSON.stringify(gates) || plan.semanticReview !== semanticReview) {
    throw new Error(`Clasificación incorrecta para ${files.join(", ")}: ${JSON.stringify(plan)}`);
  }
}
console.log("Fixtures de clasificación de validación superados.");
