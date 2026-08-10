---
name: validate-terminology
description: Validate consistent terminology across product and design documents. Use when a phase closes or a pull request adds, renames, or changes operational concepts.
---

# Validate Terminology

Review changed documents and their referenced phase documents for terms that describe the same concept differently or the same term with incompatible meanings.

1. Build a short list of changed operational terms.
2. Compare each term with existing definitions and usages.
3. Flag synonyms, undefined terms, and conflicting definitions.
4. Recommend one canonical term and the document that should define it.

Do not invent a glossary entry when the product owner has not chosen the concept. Produce this report:

```markdown
## Terminology review
- Status: ready for human review | requires decision | blocked
- Evidence: <term and source sections>
- Findings: <conflict or none>
- Required action: <canonical wording or decision>
- Human reviewer: Architecture reviewer
```
