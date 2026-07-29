# Adequação P2 — Validation Notes (preliminary)

**Date:** 2026-07-29  
**Branch:** `feat/adequacao-analise-projeto`  
**Sonar analysis:** `./diversos/scripts/sonar-analyze.sh` @ post-T12

## JaCoCo thresholds (AAP-11…16)

| Domain | Threshold | Actual | Status |
| ------ | --------- | ------ | ------ |
| organograma | ≥50% | 66.0% | PASS |
| security | ≥40% | 74.7% | PASS |
| importacao | ≥75% | 76.8% | PASS |
| global backend | ≥65% | 71.6% | PASS |

Gate: `bash diversos/scripts/check-jacoco-thresholds.sh` → exit 0

## Sonar metrics (AAP-05, AAP-10, AAP-18)

| Metric | Target | Actual | Status |
| ------ | ------ | ------ | ------ |
| Bugs OPEN | 0 | 0 | PASS |
| Vulns CRITICAL+MAJOR OPEN | 0 | 4 | **FAIL** (see exceptions) |
| Quality Gate | OK or documented | ERROR | Documented exceptions |

### Vulnerability exceptions (pre-existing / accepted)

1. **java:S4502** — CSRF disabled in `SecurityConfig` (JWT stateless API). Mitigated + documented in `_docs/specs/INTEGRATIONS.md` (T5 / AAP-07).
2. **java:S5804** (×2) — User enumeration warnings on `AuthenticationService` catch paths. Anti-enumeration unified in T6 (AAP-08); Sonar rule still flags pattern — accepted pending Sonar suppression or rule config review.
3. **typescript:S2245** — `Math.random` in FE folha page (out of P2 scope; P3/hygiene follow-up).

### Quality Gate ERROR exceptions (AAP-18)

QG status **ERROR** driven by historical `PREVIOUS_VERSION` baseline (2026-07-27), not new bugs:

1. **new_violations** = 240 (threshold 0) — pre-2026-07-27 debt; P2 added tests/docs only.
2. **new_coverage** = 60.1% (threshold 80%) — frontend still 0% Vitest until AAP-23 (Batch 3).

**Approved for feature validation:** bugs=0; JaCoCo domain gates pass; QG ERROR documented. Vulns CRITICAL+MAJOR require Batch 3 hygiene or Sonar accept/FP review.
