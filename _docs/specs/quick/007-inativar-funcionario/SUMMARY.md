# Quick Task 007 — Summary

**Date:** 2026-07-28  
**Status:** ✅ Complete (pre-existing in `funcionarios-folha-dashboard-ux` working tree)  
**Commits:** none (per user request)

## Outcome

Botão **Inativar** em cards de funcionários ativos, com confirmação, soft-delete via API existente e refresh da listagem respeitando filtro de status.

## Evidence

### FUNC-01 — Botão Inativar em card ativo

- `frontend/src/pages/Funcionarios/index.tsx:509-544` — gate `funcionario.ativo !== false`; `IconButton` `title="Inativar"` + `DeleteIcon`

### FUNC-02 — Fluxo completo

| Layer | Evidence |
| --- | --- |
| FE confirm + DELETE | `index.tsx:254-267` — `window.confirm` → `funcionarioService.remover(id)` → toast → `filtrar(getValues())` |
| FE service | `funcionarioService.ts:38-40` — `api.delete('/funcionarios/${id}')` |
| BE controller | `FuncionarioController.java:65-73` — `@DeleteMapping("/{id}")` |
| BE soft-delete | `FuncionarioService.java:107-112` — `setAtivo(false)` + `save` |
| BE tests | `FuncionarioServiceTest.java:131-149` — `remover_sets_ativo_false`, `segundo_remover_throws_funcionario_not_found_exception` |

## Gate

| Command | Result |
| --- | --- |
| `mvn test -Dtest=FuncionarioServiceTest` | **9/9 passed** (2026-07-28) |
| `mvn test -Dtest=FuncionarioServiceTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest,FolhaConsultaAdapterTest` | **37/37 passed** (2026-07-28) |

## Gaps (non-blocking)

- ~~`aria-label` opcional nos IconButtons (só `title` hoje)~~ **Fix cycle 1 (2026-07-28):** `aria-label="Editar"` e `aria-label="Inativar"` em `Funcionarios/index.tsx`
- Sem teste FE automatizado (AD-004)

## Fix cycles

| Cycle | Change | Gate |
| --- | --- | --- |
| 1 | `aria-label` nos IconButtons Editar/Inativar (FUNC-01 a11y) | `FuncionarioServiceTest#remover_*` **2/2 passed** |
