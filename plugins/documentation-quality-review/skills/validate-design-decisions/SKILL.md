---
name: validate-design-decisions
description: Validate documented product, data, permission, or architecture decisions. Use when a pull request introduces or changes a decision with downstream implementation impact.
---

# Validate Design Decisions

For every changed decision, verify that the documentation contains a reason, an alternative considered or explicitly omitted, the impact, and the phase where it must be implemented.

1. Identify the decision and its owner if stated.
2. Check that the decision does not contradict existing requirements or scope.
3. Flag decisions presented as facts without rationale, impact, or implementation target.
4. Separate an unresolved decision from a deliberate deferral.

Do not choose an alternative on behalf of the decision owner. Produce this report:

```markdown
## Design decision review
- Status: ready for human review | requires decision | blocked
- Evidence: <decision and section>
- Findings: <missing rationale, impact, or conflict>
- Required action: <decision owner action or none>
- Human reviewer: Architecture reviewer
```
