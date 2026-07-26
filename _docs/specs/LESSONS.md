# LESSONS — auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `_docs/specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation — do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 — When grepping for forbidden legacy path prefixes, match the path token itself (e.g. .specs), not only the slash-suffixed form (.specs/), so join()-style literals cannot bypass the gate.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `harness/tlc-paths` · harmful: 0
- features: ajuste-harness
- evidence: validation.md:sensor-A / lessons.py:STORE_REL (harness/tlc-paths)
- last seen: 2026-07-26T16:06:16Z

## Quarantined (failed when applied — ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
