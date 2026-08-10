---
name: validate-phase-traceability
description: Validate traceability between product and design phases. Use when closing a phase or reviewing phase documentation that changes risks, decisions, requirements, or planned follow-up work.
---

# Validate Phase Traceability

Review the relevant phase documents and the pull request diff. Use `docs/documentation-quality-gates.md` when it exists in the workspace.

1. List each changed or active risk, decision, and Must requirement.
2. Identify its treatment in the next phase, or verify that it is explicitly recorded as pending or out of scope.
3. Flag missing, ambiguous, or contradictory links between phases.

Do not approve the pull request or declare a product decision correct. Produce this report:

```markdown
## Traceability review
- Status: ready for human review | requires decision | blocked
- Evidence: <source document and section>
- Findings: <specific missing links or contradictions>
- Required action: <action or none>
- Human reviewer: Architecture reviewer
```
