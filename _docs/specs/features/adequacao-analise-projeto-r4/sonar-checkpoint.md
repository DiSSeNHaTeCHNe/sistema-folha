# Sonar checkpoint — R4 (T9)

**Date:** 2026-07-29  
**Branch:** `feat/adequacao-analise-projeto`  
**Command:** `./diversos/scripts/sonar-analyze.sh` → exit **0** (ANALYSIS SUCCESSFUL)

## Metrics (period PREVIOUS_VERSION @ 2026-07-27)

| Metric | Value | R4 internal target |
| ------ | ----- | ------------------ |
| Quality Gate | **OK** | OK |
| `new_coverage` | **80.0%** | ≥ 85% |
| `new_branch_coverage` | **62.6%** | ≥ 70% (informativo) |
| `new_violations` | **0** | 0 |
| `coverage` (aggregate) | **59.8%** | — |

## Decision for T11

- `new_coverage` **< 85%** → **T11 code required** (≤2 cases, leak-period scope AAP4-19).
- `new_branch_coverage` **< 70%** → branch tests encouraged within same budget.
- Budget remaining before T11: **8/10** new `it(`/`test(`.

_T13 will fold this block into `validation.md`._
