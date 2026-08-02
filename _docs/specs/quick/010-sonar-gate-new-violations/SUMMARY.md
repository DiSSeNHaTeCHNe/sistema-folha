# Quick Task 010 — SUMMARY

**Date:** 2026-08-02
**Scope:** Small (Quick-Fix) · **Status:** Implementado, gate build verde (confirmação Sonar em curso)
**Commit:** _não commitado_ — preferência do projeto: usuário controla commits (STATE.md)

## O que foi feito

Corrigidas as 10 violações de código novo que reprovavam o Quality Gate (`new_violations > 0`), todas na feature API Keys. Mudanças mecânicas, sem alteração de comportamento, seguindo convenções já presentes no projeto.

| # | Regra | Sev | Arquivo | Correção |
| --- | --- | --- | --- | --- |
| 1 | `java:S2387` | BLOCKER | `security/ApiKeyWriteGuardFilter.java` | campo `logger` → `log` (deixa de sombrear `GenericFilterBean.logger`) |
| 2 | `java:S6204` | MAJOR | `cadastros/application/FuncionarioService.java:94` | `.collect(Collectors.toList())` → `.toList()` |
| 3-4 | `java:S8688` | INFO | `auth/application/ApiKeyService.java:67,130` | `LocalDateTime.now()` → `LocalDateTime.now(Clock.systemDefaultZone())` (+ import) |
| 5-6 | `java:S8688` | INFO | `auth/domain/ApiKey.java:53,60` | idem (+ import) |
| 7 | `java:S1128` | MINOR | `folha/application/FolhaConsultaAdapter.java:25` | removido import não usado `java.util.stream.Collectors` |
| 8-9 | `typescript:S1874` | MINOR | `frontend/src/pages/ApiKeys/index.tsx:290,299` | `inputProps={...}` → `slotProps={{ htmlInput: {...} }}` |
| 10 | `typescript:S1874` | MINOR | `frontend/src/pages/ApiKeys/index.tsx:324` | `InputProps={...}` → `slotProps={{ input: {...} }}` |

**Arquivos tocados:** 5 backend + 1 frontend (6 no total) + este quick-task. `git diff --stat`: 13 inserções, 12 remoções.

## Verificação (Gate: build)

| Check | Resultado |
| --- | --- |
| `frontend npm run build` (tsc) | ✅ built in 3.55s |
| `frontend vitest ApiKeys` | ✅ 22/22 |
| `backend mvn test` | ✅ 1044 testes · 0 falhas · 0 erros · 1 skip |
| Sem `inputProps/InputProps` restante | ✅ |
| `Collectors` em FuncionarioService | mantido (uso restante na linha 47, fora do escopo) |

## Notas / decisões

- **SGV-02 (sem mudança de comportamento):** `Clock.systemDefaultZone()` = mesmo fuso que `.now()` sem argumento; `.toList()` = lista imutável equivalente ao `Collectors.toList()` para o uso atual (só leitura/stream); `slotProps` é o substituto 1:1 do MUI v7 para os props depreciados.
- **SGV-03 (convenção):** todas as escolhas espelham código já existente (`Clock.systemDefaultZone()` em DashboardService/BeneficioMensalService; `slotProps.htmlInput` em BeneficiosMensais; campo `log` no restante dos filtros/serviços).
- **Fora de escopo (deferido):** injeção de `Clock` como dependência para testabilidade real do tempo; smells legados (God method importação, N+1 organograma).

## Próximo passo

Confirmar `new_violations = 0` e Quality Gate `OK` na reanálise Sonar; então commitar (sugestão: `fix(sonar): resolve 10 new-code violations to restore quality gate`).
