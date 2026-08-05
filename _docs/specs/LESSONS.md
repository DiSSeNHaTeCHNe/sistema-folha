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

### L-011 — PDF renderer tests must assert spec-defined KPI labels and values extracted from bytes, not only magic bytes and partial numeric substrings.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `relatorios/pdf` · harmful: 0
- features: relatorios-executivos
- evidence: REL-07 FolhaExecutivoPdfRenderer.java:105-108 (relatorios/pdf)
- last seen: 2026-08-03T21:42:54Z

### L-012 — List endpoints need an ordering assertion on returned DTO sequence, not only HTTP 200.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `relatorios/api` · harmful: 0
- features: relatorios-executivos
- evidence: sensor-probe: list OrderByAnoDescMesDesc (relatorios/api)
- last seen: 2026-08-03T21:42:54Z

### L-013 — Cover every RelatorioStatus branch in download tests, including ERRO returning 409, not only PENDENTE.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `relatorios/api` · harmful: 0
- features: relatorios-executivos
- evidence: REL-05 ERRO download (relatorios/api)
- last seen: 2026-08-03T21:42:54Z

### L-014 — Assert structured log lines with Logback ListAppender for async workers — implementation-only logs fail evidence-or-zero.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/relatorios/worker` · harmful: 0
- features: relatorios-executivos-fix1
- evidence: FIX1-04 (backend/relatorios/worker)
- last seen: 2026-08-04T01:42:03Z

### L-015 — Entity mapping ACs need a reflection or DataJpaTest asserting column type/mapping, not compile-only.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend/jpa/entity` · harmful: 0
- features: relatorios-executivos-fix1
- evidence: FIX1-22 (backend/jpa/entity)
- last seen: 2026-08-04T01:42:03Z

### L-016 — When spec requires dataProcessamento on ERRO, assert it explicitly in failure-path worker tests.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `backend/relatorios/worker` · harmful: 0
- features: relatorios-executivos-fix1
- evidence: FIX1-02 (backend/relatorios/worker)
- last seen: 2026-08-04T01:42:03Z

### L-017 — Quando uma variante de configuracao e construida fora da fabrica compartilhada, replique para ela o mesmo teste parametrizado que cobre a fabrica
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `theme` · harmful: 0
- features: temas-fidelidade-visual
- evidence: M7 — frontend/src/theme/themes.ts:215 (validation.md, Discrimination Sensor) (theme)
- last seen: 2026-08-04T14:25:01Z

### L-018 — Quando o ambiente de teste nao resolve o valor final, asserte o valor esperado e registre a limitacao, nao apenas um proxy de herança
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `frontend-tests` · harmful: 0
- features: temas-fidelidade-visual
- evidence: P1-Props AC4 — frontend/src/pages/Dashboard/Dashboard.test.tsx:217 (frontend-tests)
- last seen: 2026-08-04T14:25:10Z

### L-019 — Criterio de sucesso expresso como comando de grep deve repetir o mesmo escopo de diretorios do AC correspondente
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: temas-fidelidade-visual
- evidence: Success Criteria da spec vs P1-Props AC1 — frontend/src/theme/escalaRenderizada.test.tsx:38 (spec)
- last seen: 2026-08-04T14:25:10Z

### L-020 — Quando o usuario ratifica um desvio e o documento de origem e atualizado, rebaixe o marcador SPEC_DEVIATION a nota explicativa no mesmo commit
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `docs` · harmful: 0
- features: temas-fidelidade-visual
- evidence: SPEC_DEVIATION em frontend/src/theme/themes.ts:134 (docs)
- last seen: 2026-08-04T14:25:10Z

### L-021 — Include npm run build in release gate; Vitest alone does not catch broken TypeScript imports under MeuDashboard/widgets.
- signal: `gate_fail` · recurrence: 1 feature(s) · scope: `frontend/build` · harmful: 0
- features: dashboard-customizavel
- evidence: validation.md Gate Check: widgetDataUtils.ts:5-6, DistribuicaoWidget.tsx:57 (frontend/build)
- last seen: 2026-08-05T01:47:28Z

### L-022 — Dual-menu convivência ACs require Layout nav test asserting both Dashboard and Meu Dashboard links when access helper returns true.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend/navigation` · harmful: 0
- features: dashboard-customizavel
- evidence: DASHC-05 (frontend/navigation)
- last seen: 2026-08-05T01:47:28Z

### L-023 — Responsive grid ACs need breakpoint test asserting rendered span 12 on xs without mutating persisted colSpan.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend/grid` · harmful: 0
- features: dashboard-customizavel
- evidence: DASHC-12 (frontend/grid)
- last seen: 2026-08-05T01:47:28Z

## Quarantined (failed when applied — ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
