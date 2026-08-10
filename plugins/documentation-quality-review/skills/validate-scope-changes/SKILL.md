---
name: validate-scope-changes
description: Validate that a documentation pull request declares its changes to scope, assumptions, risks, and decisions. Use when reviewing any pull request that modifies phase, product, or design documents.
---

# Validate Scope Changes

Compare the pull request description and diff with the previous document state.

1. Identify additions, removals, and reprioritizations that affect MVP scope.
2. Check that the PR description declares changed assumptions, risks, decisions, and exclusions.
3. Flag scope changes hidden as wording or undocumented changes in responsibility.
4. Verify that referenced phases and decisions still exist.

Do not approve the PR; require the author to describe an omitted scope change. Produce this report:

```markdown
## Scope change review
- Status: ready for human review | requires decision | blocked
- Evidence: <PR section and diff location>
- Findings: <undeclared scope change or none>
- Required action: <PR or document update>
- Human reviewer: Pull request reviewer
```
