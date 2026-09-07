#!/usr/bin/env node

const fs = require("node:fs");
const path = require("node:path");

const repositoryRoot = process.argv[2] ? path.resolve(process.argv[2]) : path.resolve(__dirname, "..");
const documentationRoots = ["README.md", "AGENTS.md", "docs"];
const markdownFiles = [];

function collectMarkdownFiles(target) {
  const absoluteTarget = path.join(repositoryRoot, target);
  const stat = fs.statSync(absoluteTarget);
  if (stat.isFile() && absoluteTarget.endsWith(".md")) {
    markdownFiles.push(absoluteTarget);
    return;
  }
  for (const entry of fs.readdirSync(absoluteTarget, { withFileTypes: true })) {
    const child = path.join(absoluteTarget, entry.name);
    if (entry.isDirectory()) {
      collectMarkdownFiles(path.relative(repositoryRoot, child));
    } else if (entry.isFile() && child.endsWith(".md")) {
      markdownFiles.push(child);
    }
  }
}

function githubAnchor(heading) {
  return heading
    .trim()
    .toLowerCase()
    .replace(/[\\`*_{}[\\]<>]/g, "")
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .replace(/\s+/g, "-");
}

function anchorsIn(file) {
  const counts = new Map();
  const anchors = new Set();
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const match = /^(#{1,6})\s+(.+?)\s*#*\s*$/.exec(line);
    if (!match) continue;
    const base = githubAnchor(match[2]);
    const count = counts.get(base) ?? 0;
    counts.set(base, count + 1);
    anchors.add(count === 0 ? base : `${base}-${count}`);
  }
  return anchors;
}

function isExternal(target) {
  return /^(?:[a-z][a-z0-9+.-]*:|\/)/i.test(target);
}

for (const root of documentationRoots) collectMarkdownFiles(root);

const failures = [];
const linkPattern = /!?\[[^\]]*\]\(([^)\s]+)(?:\s+[^)]*)?\)/g;
for (const source of markdownFiles) {
  const contents = fs.readFileSync(source, "utf8");
  for (const match of contents.matchAll(linkPattern)) {
    const target = match[1];
    if (isExternal(target)) continue;
    const [targetPath, fragment] = target.split("#", 2);
    const destination = targetPath
      ? path.resolve(path.dirname(source), decodeURIComponent(targetPath))
      : source;
    if (!fs.existsSync(destination)) {
      failures.push(`${path.relative(repositoryRoot, source)} -> ${target}: destino inexistente`);
      continue;
    }
    if (fragment) {
      if (path.extname(destination).toLowerCase() !== ".md") {
        failures.push(`${path.relative(repositoryRoot, source)} -> ${target}: ancla fuera de Markdown`);
      } else if (!anchorsIn(destination).has(fragment)) {
        failures.push(`${path.relative(repositoryRoot, source)} -> ${target}: ancla inexistente`);
      }
    }
  }
}

if (failures.length > 0) {
  console.error("Enlaces documentales inválidos:\n" + failures.join("\n"));
  process.exit(1);
}

console.log(`Enlaces documentales locales verificados: ${markdownFiles.length} archivos Markdown.`);
