---
name: validate-blocking-questions
description: Validate that open questions capable of changing scope, data models, or user flows are resolved or explicitly treated as blockers. Use before closing a project phase or approving related documentation.
---

# Validate Blocking Questions

Review assumptions, open questions, risks, and decisions in the changed phase documents.

1. Identify unanswered questions that could alter scope, data, permissions, flows, or compliance.
2. Classify each as resolved, deliberately deferred with an owner and deadline, or blocking.
3. Flag assumptions that are presented as decisions without validation.
4. Block phase closure when an unowned question can change the next phase.

Do not silently resolve a product question. Produce this report:

```markdown
## Blocking questions review
- Status: ready for human review | requires decision | blocked
- Evidence: <question, assumption, or risk>
- Findings: <classification and rationale>
- Required action: <owner decision or none>
- Human reviewer: Product reviewer
```
