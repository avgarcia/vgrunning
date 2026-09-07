#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { pathToFileURL } from "node:url";

const repositoryRoot = path.resolve(import.meta.dirname, "..");

function parseArguments(argv) {
  const options = { base: "origin/main", output: path.join(repositoryRoot, "build", "reports", "validation", "plan.json"), files: null, filesFile: null, all: false };
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === "--base") options.base = argv[++index];
    else if (argument === "--output") options.output = path.resolve(argv[++index]);
    else if (argument === "--files") options.files = argv[++index].split(",").filter(Boolean);
    else if (argument === "--files-file") options.filesFile = path.resolve(argv[++index]);
    else if (argument === "--all") options.all = true;
    else throw new Error(`Argumento no admitido: ${argument}`);
  }
  return options;
}

function globToRegExp(pattern) {
  const escaped = pattern
    .replace(/[.+^${}()|[\]\\]/g, "\\$&")
    .replace(/\*\*/g, "§§")
    .replace(/\*/g, "[^/]*")
    .replace(/§§/g, ".*");
  return new RegExp(`^${escaped}$`);
}

function matches(file, pattern) {
  return globToRegExp(pattern).test(file.replaceAll("\\", "/"));
}

function readChangedFiles(options) {
  if (options.all) return ["__all__"];
  if (options.files) return options.files;
  if (options.filesFile) return fs.readFileSync(options.filesFile, "utf8").split(/\r?\n/).filter(Boolean);
  const committed = execFileSync("git", ["diff", "--name-only", `${options.base}...HEAD`], { cwd: repositoryRoot, encoding: "utf8" });
  const workingTree = execFileSync("git", ["diff", "--name-only"], { cwd: repositoryRoot, encoding: "utf8" });
  const untracked = execFileSync("git", ["ls-files", "--others", "--exclude-standard"], { cwd: repositoryRoot, encoding: "utf8" });
  return [...new Set(`${committed}\n${workingTree}\n${untracked}`.split(/\r?\n/).filter(Boolean))].sort();
}

export function classify(files, matrix) {
  if (files.includes("__all__")) {
    return { surfaces: ["all"], gates: ["qualityGate"], semanticReview: false, reasons: ["Ejecución fuera de una PR: inventario integral obligatorio."] };
  }
  const selected = new Map();
  const unknown = [];
  for (const file of files) {
    let matchingSurfaces = matrix.surfaces.filter((surface) => surface.patterns.some((pattern) => matches(file, pattern)));
    if (matchingSurfaces.some((surface) => surface.name === "persistence")) {
      matchingSurfaces = matchingSurfaces.filter((surface) => surface.name !== "backend");
    }
    if (matchingSurfaces.length === 0) unknown.push(file);
    for (const surface of matchingSurfaces) {
      const value = selected.get(surface.name) ?? { gates: new Set(), semanticReview: false, files: [] };
      surface.gates.forEach((gate) => value.gates.add(gate));
      value.semanticReview ||= surface.semanticReview;
      value.files.push(file);
      selected.set(surface.name, value);
    }
  }
  if (unknown.length > 0) {
    return { surfaces: [matrix.default.surface], gates: matrix.default.gates, semanticReview: matrix.default.semanticReview, reasons: [matrix.default.reason], unknownFiles: unknown };
  }
  const gates = new Set();
  let semanticReview = false;
  const reasons = [];
  for (const [name, value] of selected) {
    value.gates.forEach((gate) => gates.add(gate));
    semanticReview ||= value.semanticReview;
    reasons.push(`${name}: ${value.files.join(", ")}`);
  }
  return { surfaces: [...selected.keys()].sort(), gates: [...gates].sort(), semanticReview, reasons };
}

function main() {
  const options = parseArguments(process.argv.slice(2));
  const matrix = JSON.parse(fs.readFileSync(path.join(repositoryRoot, "config", "validation-matrix.json"), "utf8"));
  const files = readChangedFiles(options);
  const classification = classify(files, matrix);
  const plan = { version: matrix.version, mode: "shadow", base: options.all ? null : options.base, files, ...classification };
  fs.mkdirSync(path.dirname(options.output), { recursive: true });
  fs.writeFileSync(options.output, JSON.stringify(plan, null, 2) + "\n", "utf8");
  console.log(`Plan de validación en sombra: ${plan.gates.join(", ")}.`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
