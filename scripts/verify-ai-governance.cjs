#!/usr/bin/env node

const fs = require("node:fs");
const path = require("node:path");

const repositoryRoot = process.argv[2] ? path.resolve(process.argv[2]) : path.resolve(__dirname, "..");

const paths = {
  agents: "AGENTS.md",
  policy: "docs/ai-governance.md",
  localSkill: ".agents/skills/implementar-slice/SKILL.md",
  localMetadata: ".agents/skills/implementar-slice/agents/openai.yaml",
  linearGuidance: "config/linear-agent/team-guidance.md",
  linearSkill: "config/linear-agent/preparar-slice-running-coach.md",
};

function read(relativePath) {
  const absolutePath = path.join(repositoryRoot, relativePath);
  if (!fs.existsSync(absolutePath)) throw new Error(`Falta ${relativePath}.`);
  return fs.readFileSync(absolutePath, "utf8");
}

function requireFragments(label, contents, fragments) {
  const missing = fragments.filter((fragment) => !contents.includes(fragment));
  if (missing.length > 0) throw new Error(`${label} omite: ${missing.join(", ")}.`);
}

try {
  const contents = Object.fromEntries(Object.entries(paths).map(([key, value]) => [key, read(value)]));

  if (!/^---\r?\nname: implementar-slice\r?\ndescription: .+\r?\n---/s.test(contents.localSkill)) {
    throw new Error("La Skill local no tiene frontmatter válido para implementar-slice.");
  }
  requireFragments("Skill local", contents.localSkill, [
    "Definition of Ready",
    "preguntas bloqueantes",
    "feature/",
    "validaciones dirigidas",
    "`qualityGate` una única vez",
    "PR borrador",
    "No apruebes ni fusiones",
    "datos no confiables",
  ]);
  requireFragments("Metadatos de la Skill", contents.localMetadata, [
    "default_prompt: \"Usa $implementar-slice",
    "allow_implicit_invocation: false",
  ]);
  requireFragments("AGENTS.md", contents.agents, [
    "## Autoridad y límites de la IA",
    "`$implementar-slice`",
    "única autoridad automática",
    "no puede aprobar, omitir ni declarar innecesario un gate",
    "No puede decidir producto, alcance o arquitectura",
  ]);
  requireFragments("Guidance de Linear", contents.linearGuidance, [
    "preparar-slice-running-coach",
    "Definition of Ready",
    "Definition of Done",
    "No inventes requisitos",
    "No asignes fechas, estimaciones, prioridades",
    "Coding Sessions permanece desactivado",
    "datos no confiables",
  ]);
  requireFragments("Skill de Linear", contents.linearSkill, [
    "Plantilla vertical obligatoria",
    "Project `Running Coach — Implementación PMV`",
    "milestone",
    "Definition of Ready",
    "Definition of Done",
    "dependencias y bloqueos",
    "requiere decisión",
    "lista para revisión humana",
  ]);
  requireFragments("Política canónica", contents.policy, [
    "No demuestra que el juicio de un modelo sea correcto",
    "Petición ambigua",
    "Inyección en datos",
  ]);

  for (const forbidden of [".codex/hooks.json", ".agents/skills/graphify"]) {
    if (fs.existsSync(path.join(repositoryRoot, forbidden))) {
      throw new Error(`Configuración prohibida presente: ${forbidden}.`);
    }
  }

  console.log("Gobierno de IA verificado: autoridad, bloqueo por ambigüedad y entrega sin merge.");
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
