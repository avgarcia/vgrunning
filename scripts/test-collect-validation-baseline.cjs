#!/usr/bin/env node

const assert = require("node:assert/strict");
const { collect, selectSamples } = require("./collect-validation-baseline.cjs");

const workflows = ["quality-gate", "codeql-java", "codeql-javascript-typescript"];
let identifier = 1;
function run(name, event, group) {
  return {
    id: identifier++, name, event, status: "completed", conclusion: "success", head_sha: group,
    head_branch: event === "push" ? "main" : "feature/test", created_at: "2026-08-28T00:00:00Z",
    run_started_at: "2026-08-28T00:00:00Z", updated_at: "2026-08-28T00:01:00Z",
    pull_requests: event === "pull_request" ? [{ number: Number(group.replace("pr", "")) }] : [],
  };
}
const runs = [
  ...workflows.map((name) => run(name, "pull_request", "pr1")),
  ...workflows.map((name) => run(name, "push", "main1")),
];
const api = (endpoint) => {
  if (endpoint.includes("/actions/runs?per_page=100&page=")) return { workflow_runs: runs };
  if (endpoint.includes("/jobs?")) return { jobs: [{ name: "quality-gate", runner_name: "GitHub Actions", runner_group_name: "GitHub Actions", conclusion: "success", started_at: "2026-08-28T00:00:00Z", completed_at: "2026-08-28T00:01:00Z", steps: [{ name: "Run ./gradlew fastGate", conclusion: "success", started_at: "2026-08-28T00:00:00Z", completed_at: "2026-08-28T00:01:00Z" }] }] };
  if (endpoint.includes("/artifacts?")) return { artifacts: [{ name: "quality-reports", size_in_bytes: 12, expired: false }] };
  throw new Error(`Endpoint inesperado: ${endpoint}`);
};

const report = collect({ repo: "owner/repo", prLimit: 1, mainLimit: 1 }, api);
assert.equal(report.samples.pullRequests.length, 1);
assert.equal(report.samples.main.length, 1);
assert.equal(report.samples.pullRequests[0].workflows[0].jobs[0].durationSeconds, 60);
assert.equal(report.samples.pullRequests[0].workflows[0].jobs[0].cache, "not-observable");
assert.equal(report.samples.pullRequests[0].workflows[0].artifacts[0].name, "quality-reports");
assert.throws(() => selectSamples(runs.slice(0, 2), "pull_request", 1), /muestras completas/);
console.log("Autoprueba de la línea base superada con fixtures mínimos.");
