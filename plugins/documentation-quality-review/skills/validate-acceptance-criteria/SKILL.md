---
name: validate-acceptance-criteria
description: Validate acceptance criteria for functional Must requirements. Use when requirements are prepared for implementation, user-flow design, test planning, or pull request review.
---

# Validate Acceptance Criteria

For every changed Must requirement, verify that its acceptance criteria describe at least one success scenario and relevant error or boundary behavior.

1. Link the requirement to its criteria; flag missing links.
2. Check that criteria use observable inputs, actions, and outcomes.
3. Identify criteria that depend on undocumented data, permissions, or decisions.
4. Flag examples disguised as exhaustive criteria.

Do not claim that criteria are tested; this skill validates documentation only. Produce this report:

```markdown
## Acceptance criteria review
- Status: ready for human review | requires decision | blocked
- Evidence: <requirement and criteria>
- Findings: <missing scenario or dependency>
- Required action: <criterion or decision needed>
- Human reviewer: Product reviewer and Architecture reviewer
```
