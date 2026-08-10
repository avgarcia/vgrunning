---
name: validate-verifiable-requirements
description: Validate that functional Must requirements are observable and testable. Use when drafting or reviewing requirements, acceptance criteria, or scope changes in product documentation.
---

# Validate Verifiable Requirements

Review every changed Must requirement. A requirement is verifiable only when it states the actor, observable behavior, expected result, and applicable constraints or error conditions.

1. Quote the requirement and identify the missing element, if any.
2. Reject aspirational wording such as "basic", "easy", or "fast" unless it has an observable definition.
3. Check that exclusions and notifications are stated as behavior, not assumptions.

Do not rewrite business intent without identifying it as a proposed change. Produce this report:

```markdown
## Requirement verification review
- Status: ready for human review | requires decision | blocked
- Evidence: <requirement and section>
- Findings: <non-verifiable requirement or none>
- Required action: <precise clarification>
- Human reviewer: Product reviewer
```
