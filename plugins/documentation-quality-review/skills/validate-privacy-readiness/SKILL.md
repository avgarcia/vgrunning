---
name: validate-privacy-readiness
description: Validate that documentation records the privacy readiness evidence required before production. Use before a production release or when product changes introduce personal data, feedback, retention, access, or deletion behavior.
---

# Validate Privacy Readiness

Review product and release documentation for the documented evidence required before production: responsible party, legal basis, retention, access, deletion, and treatment of declared feedback.

1. Identify personal data and feedback processing described by the change.
2. Check whether each required evidence item is documented or explicitly pending.
3. Flag production readiness as blocked when evidence is absent.
4. Distinguish documentation completeness from legal advice or legal approval.

Do not provide legal advice or approve compliance. Produce this report:

```markdown
## Privacy readiness review
- Status: ready for human review | requires decision | blocked
- Evidence: <documented controls or missing evidence>
- Findings: <missing privacy evidence or none>
- Required action: <privacy owner action>
- Human reviewer: Privacy reviewer or DPO
```
