# Quick Task 007 — Botão Inativar Funcionário

**Date:** 2026-07-28  
**Scope:** Small (Quick-Fix)  
**Parent spec:** `_docs/specs/features/funcionarios-folha-dashboard-ux/spec.md` (FUNC-01, FUNC-02)

## Problem

Operador de RH precisa inativar funcionário ativo pela tela de cadastro, preservando histórico de folha (soft-delete).

## Requirements

| ID | WHEN | THEN |
| --- | --- | --- |
| **FUNC-01** | Card de funcionário **ativo** | Exibir ação **Inativar** (ícone/botão com tooltip/label acessível) |
| **FUNC-02** | Usuário confirma inativação | `DELETE /api/funcionarios/{id}` → `ativo=false`; feedback sucesso; atualizar listagem conforme filtro vigente; card inativo **sem** Inativar |

## Out of Scope

- Reativar funcionário
- Editar inativo
- Demais stories da feature pai (filtro status, dashboard, folha)

## Verification

```bash
cd backend && mvn test -Dtest=FuncionarioServiceTest#remover_sets_ativo_false,FuncionarioServiceTest#segundo_remover_throws_funcionario_not_found_exception
```

Manual: Funcionários → card ativo → Inativar → confirmar → toast + sumir da lista Ativo; filtro Inativo → card cinza sem botão.
