# Quick Task 010 — Zerar as 10 novas violações do Quality Gate (SonarQube)

**Date:** 2026-08-02
**Scope:** Small (Quick-Fix)
**Related:** feature `auth-api-keys-fix2` (código novo que introduziu as violações) · raio-x `_docs/specs/features/raio-x-projeto/`

## Problem

A reanálise SonarQube de 2026-08-02 reprovou o Quality Gate: condição `new_violations > 0` com **10 violações em código novo** (todas na feature API Keys). Coverage de código novo (97,2%) e duplicação passam; o gate só falha pelas 10 issues. Todas são mecânicas e não alteram comportamento.

## Requirements

| ID | WHEN | THEN |
| --- | --- | --- |
| **SGV-01** | Reanálise Sonar após o fix | SHALL reportar `new_violations = 0` e Quality Gate `OK` |
| **SGV-02** | Correção de cada violação | SHALL preservar o comportamento observável (sem mudança funcional) |
| **SGV-03** | Escolha de cada correção | SHALL seguir convenção já existente no projeto |

## Violações a corrigir

| Regra | Sev | Arquivo:linha | Correção (convenção do projeto) |
| --- | --- | --- | --- |
| `java:S2387` | BLOCKER | `security/ApiKeyWriteGuardFilter.java:24` | renomear campo `logger` → `log` (não sombrear `GenericFilterBean.logger`) |
| `java:S6204` | MAJOR | `cadastros/application/FuncionarioService.java:94` | `.collect(Collectors.toList())` → `.toList()` |
| `java:S8688` | INFO | `auth/application/ApiKeyService.java:67,130` | `LocalDateTime.now()` → `LocalDateTime.now(Clock.systemDefaultZone())` |
| `java:S8688` | INFO | `auth/domain/ApiKey.java:53,60` | `LocalDateTime.now()` → `LocalDateTime.now(Clock.systemDefaultZone())` |
| `java:S1128` | MINOR | `folha/application/FolhaConsultaAdapter.java:25` | remover import não usado `java.util.stream.Collectors` |
| `typescript:S1874` | MINOR | `frontend/src/pages/ApiKeys/index.tsx:290,299` | `inputProps={...}` → `slotProps={{ htmlInput: {...} }}` |
| `typescript:S1874` | MINOR | `frontend/src/pages/ApiKeys/index.tsx:324` | `InputProps={...}` → `slotProps={{ input: {...} }}` |

## Out of Scope

- Injetar `Clock` como dependência (refactor de testabilidade — fica para trabalho próprio)
- Refatorar o God method de importação, N+1 do OrganogramaService ou demais smells legados
- Qualquer mudança de fuso horário efetivo (usar `systemDefaultZone()` mantém o comportamento atual)

## Verification (Gate: build)

```bash
cd backend && mvn -q test        # backend não quebra
cd frontend && npm run build     # tsc + build passam (pega uso incorreto de slotProps)
```

Confirmação final: reanálise Sonar (`./diversos/scripts/sonar-analyze.sh`) → `new_violations = 0`, Quality Gate `OK`.
