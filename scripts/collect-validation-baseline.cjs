#!/usr/bin/env node

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const REQUIRED_WORKFLOWS = ["quality-gate", "codeql-java", "codeql-javascript-typescript"];

function parseArguments(argumentsList) {
  const options = {
    repo: "avgarcia/vgrunning",
    prLimit: 6,
    mainLimit: 4,
    outputDirectory: path.resolve("build/reports/validation"),
  };
  for (let index = 0; index < argumentsList.length; index += 1) {
    const argument = argumentsList[index];
    const value = argumentsList[index + 1];
    if (argument === "--repo") options.repo = value;
    else if (argument === "--pr-limit") options.prLimit = Number.parseInt(value, 10);
    else if (argument === "--main-limit") options.mainLimit = Number.parseInt(value, 10);
    else if (argument === "--output") options.outputDirectory = path.resolve(value);
    else throw new Error(`Argumento no reconocido: ${argument}.`);
    index += 1;
  }
  if (!Number.isInteger(options.prLimit) || options.prLimit < 1) throw new Error("--pr-limit debe ser un entero positivo.");
  if (!Number.isInteger(options.mainLimit) || options.mainLimit < 1) throw new Error("--main-limit debe ser un entero positivo.");
  return options;
}

function runGh(argumentsList) {
  const result = spawnSync("gh", argumentsList, { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr.trim() || "gh api no pudo obtener la línea base.");
  return JSON.parse(result.stdout);
}

function durationSeconds(startedAt, completedAt) {
  if (!startedAt || !completedAt) return null;
  return Math.max(0, Math.round((Date.parse(completedAt) - Date.parse(startedAt)) / 1000));
}

function groupKey(run, event) {
  if (event === "pull_request") {
    const number = run.pull_requests?.[0]?.number;
    return number ? `pr-${number}-${run.head_sha}` : `pr-commit-${run.head_sha}`;
  }
  return run.head_branch === "main" ? `main-${run.head_sha}` : null;
}

function selectSamples(runs, event, limit) {
  const grouped = new Map();
  for (const run of runs) {
    if (run.event !== event || !REQUIRED_WORKFLOWS.includes(run.name) || run.status !== "completed") continue;
    const key = groupKey(run, event);
    if (!key) continue;
    const workflows = grouped.get(key) ?? new Map();
    if (!workflows.has(run.name)) workflows.set(run.name, run);
    grouped.set(key, workflows);
  }
  const complete = [...grouped.values()]
    .filter((workflows) => REQUIRED_WORKFLOWS.every((workflow) => workflows.has(workflow)))
    .map((workflows) => REQUIRED_WORKFLOWS.map((workflow) => workflows.get(workflow)))
    .sort((left, right) => Date.parse(right[0].created_at) - Date.parse(left[0].created_at));
  if (complete.length < limit) {
    throw new Error(`Solo hay ${complete.length} muestras completas de ${event}; se requieren ${limit}.`);
  }
  return complete.slice(0, limit);
}

function jobSummary(job) {
  const steps = (job.steps ?? []).map((step) => ({
    name: step.name,
    conclusion: step.conclusion,
    durationSeconds: durationSeconds(step.started_at, step.completed_at),
  }));
  return {
    name: job.name,
    runner: job.runner_name ?? "not-observable",
    runnerGroup: job.runner_group_name ?? "not-observable",
    conclusion: job.conclusion,
    durationSeconds: durationSeconds(job.started_at, job.completed_at),
    cache: "not-observable",
    toolchains: steps.filter((step) => /setup-(java|node)|setup-gradle/i.test(step.name)).map((step) => step.name),
    steps,
  };
}

function summariseRun(run, jobs, artifacts) {
  const summarizedJobs = jobs.map(jobSummary);
  return {
    id: run.id,
    workflow: run.name,
    conclusion: run.conclusion,
    durationSeconds: durationSeconds(run.run_started_at, run.updated_at),
    runnerMinutesObserved: summarizedJobs.reduce((total, job) => total + (job.durationSeconds ?? 0), 0) / 60,
    jobs: summarizedJobs,
    artifacts: artifacts.map((artifact) => ({
      name: artifact.name,
      sizeBytes: artifact.size_in_bytes,
      expired: artifact.expired,
    })),
  };
}

function buildReport(options, prSamples, mainSamples, details) {
  const samples = { pullRequests: [], main: [] };
  for (const [name, source] of [["pullRequests", prSamples], ["main", mainSamples]]) {
    for (const runs of source) {
      const first = runs[0];
      samples[name].push({
        key: groupKey(first, name === "pullRequests" ? "pull_request" : "push"),
        commit: first.head_sha,
        createdAt: first.created_at,
        workflows: runs.map((run) => summariseRun(run, details.jobs[run.id], details.artifacts[run.id])),
      });
    }
  }
  return {
    schemaVersion: 1,
    repository: options.repo,
    measurement: {
      runnerMinutesObserved: "Suma de duraciones de jobs; no representa facturación de GitHub.",
      cache: "GitHub Actions no expone un estado de cache verificable mediante esta API; se registra not-observable.",
      workflows: REQUIRED_WORKFLOWS,
    },
    samples,
  };
}

function markdownReport(report) {
  const lines = [
    "# Línea base de validación",
    "",
    `Repositorio: \`${report.repository}\``,
    "",
    "`runner-minutes observados` suma la duración de jobs y no representa facturación de GitHub. El estado de caché es `not-observable` cuando la API no ofrece una señal verificable.",
    "",
  ];
  for (const [title, samples] of [["PR", report.samples.pullRequests], ["main", report.samples.main]]) {
    lines.push(`## Muestras ${title}`, "");
    for (const sample of samples) {
      const minutes = sample.workflows.reduce((total, workflow) => total + workflow.runnerMinutesObserved, 0).toFixed(2);
      lines.push(`### ${sample.key}`, "", `- Commit: \`${sample.commit}\``, `- Runner-minutes observados: ${minutes}`, "");
      for (const workflow of sample.workflows) {
        lines.push(`#### ${workflow.workflow}`, "", `- Conclusión: ${workflow.conclusion}`, `- Duración: ${workflow.durationSeconds ?? "not-observable"} s`, `- Artefactos: ${workflow.artifacts.map((artifact) => artifact.name).join(", ") || "ninguno"}`, "");
        for (const job of workflow.jobs) {
          lines.push(`- Job \`${job.name}\`: ${job.durationSeconds ?? "not-observable"} s; runner ${job.runner}; caché ${job.cache}; pasos ${job.steps.map((step) => step.name).join(" | ")}`);
        }
        lines.push("");
      }
    }
  }
  return lines.join("\n");
}

function collect(options, api) {
  const runs = [];
  for (let page = 1; page <= 10; page += 1) {
    const currentPage = api(`/repos/${options.repo}/actions/runs?per_page=100&page=${page}`).workflow_runs ?? [];
    runs.push(...currentPage);
    try {
      selectSamples(runs, "pull_request", options.prLimit);
      selectSamples(runs, "push", options.mainLimit);
      break;
    } catch (error) {
      if (currentPage.length < 100 || page === 10) break;
    }
  }
  const prSamples = selectSamples(runs, "pull_request", options.prLimit);
  const mainSamples = selectSamples(runs, "push", options.mainLimit);
  const details = { jobs: {}, artifacts: {} };
  for (const run of [...prSamples, ...mainSamples].flat()) {
    details.jobs[run.id] = api(`/repos/${options.repo}/actions/runs/${run.id}/jobs?per_page=100`).jobs ?? [];
    details.artifacts[run.id] = api(`/repos/${options.repo}/actions/runs/${run.id}/artifacts?per_page=100`).artifacts ?? [];
  }
  return buildReport(options, prSamples, mainSamples, details);
}

function main() {
  const options = parseArguments(process.argv.slice(2));
  const report = collect(options, (endpoint) => runGh(["api", endpoint]));
  fs.mkdirSync(options.outputDirectory, { recursive: true });
  fs.writeFileSync(path.join(options.outputDirectory, "validation-baseline.json"), JSON.stringify(report, null, 2) + "\n", "utf8");
  fs.writeFileSync(path.join(options.outputDirectory, "validation-baseline.md"), markdownReport(report), "utf8");
  console.log(`Línea base generada en ${options.outputDirectory}.`);
}

if (require.main === module) {
  try {
    main();
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}

module.exports = { buildReport, collect, markdownReport, parseArguments, selectSamples, summariseRun };
