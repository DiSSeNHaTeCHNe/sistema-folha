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

### L-002 — When spec AC requires lint exit 0, record lint failure as ac_gap even if checklist treats lint as advisory — align spec, checklist, or fix debt before PASS.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: modular-monolith
- evidence: P1 FE AC5 / npm run lint exit 1 (frontend)
- last seen: 2026-07-26T23:05:34Z

### L-003 — ArchUnit rules on domain and selected boundaries do not enforce application-layer foreign infrastructure imports — add ..application.. cross-domain rule or refactor to ports.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/arch` · harmful: 0
- features: modular-monolith
- evidence: P2 AC1(a) / BeneficioMensalService.java:16 cadastros.infrastructure (backend/arch)
- last seen: 2026-07-26T23:05:34Z

### L-004 — Port-level ACL unit tests do not satisfy HTTP ACs that require JSON field proof — add MockMvc or service test for DTO mapping at the auth boundary.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `backend/auth` · harmful: 0
- features: modular-monolith
- evidence: P1 ACL AC7 GET /auth/acesso (backend/auth)
- last seen: 2026-07-26T23:05:34Z

### L-005 — ArchUnit on ..domain.. alone is insufficient — also forbid ..application.. depending on foreign ..infrastructure.. (same-domain OK; document allowlists).
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/arch` · harmful: 0
- features: modular-monolith
- evidence: ModularArchitectureTest.java:80 / MOD-15 (backend/arch)
- last seen: 2026-07-26T23:44:20Z

### L-006 — When verifying login/refresh/permitAll, assert each public auth matcher with an anonymous MockMvc call — inspecting only /auth/login misses /auth/refresh regressions.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/security` · harmful: 0
- features: modular-monolith
- evidence: SecurityConfig.java:33 / MOD-13 (backend/security)
- last seen: 2026-07-26T23:44:20Z

### L-007 — Never treat empty centrosCustoIds as unscoped query; distinguish explicit acessoTotal from restricted-empty and return empty results for the latter.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/beneficios/acl` · harmful: 0
- features: modular-monolith
- evidence: BeneficioMensalService.java:198 / MOD-09 (backend/beneficios/acl)
- last seen: 2026-07-26T23:44:20Z

### L-008 — When a preserve-existing-rejection AC is written, name the concrete invalid input and expected exception/status in the spec and assert that exact outcome in a unit test
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `importacao` · harmful: 0
- features: modular-boundary-hardening
- evidence: MODBH-26 (importacao)
- last seen: 2026-07-27T04:20:00Z

### L-009 — Reconcile Sonar new_violations against PREVIOUS_VERSION baseline before declaring QG pass; incremental smell fixes alone may not zero leak-period violations.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `sonar` · harmful: 0
- features: adequacao-analise-projeto-r2
- evidence: AAP2-02 (sonar)
- last seen: 2026-07-29T20:52:14Z

### L-010 — Verify Sonar aggregate coverage (not just new_coverage) meets the spec floor after FE lcov import; Vitest page smoke may lift new_coverage but miss aggregate threshold.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `coverage` · harmful: 0
- features: adequacao-analise-projeto-r2
- evidence: AAP2-10 (coverage)
- last seen: 2026-07-29T20:52:14Z

## Quarantined (failed when applied — ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
