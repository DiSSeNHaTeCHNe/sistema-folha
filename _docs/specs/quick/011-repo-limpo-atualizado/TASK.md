# Quick Task 011 — Repositório limpo e documentação alinhada

**Date:** 2026-08-03  
**Scope:** Medium (multi-batch quick-fix — governança + specs + higiene)  
**Related:** análise de status 2026-08-03; itens 1, 2 e 4 do plano de ações

## Problem

Documentação de governança (`STATE`, `HANDOFF`, `CONCERNS`, `ROADMAP`, `README`) está defasada vs `main` (relatórios, API keys, benefícios mensais, ACL já mergeados). Artefatos de spec úteis permanecem untracked. Branches locais mergeadas e lixo de working tree poluem o repositório.

## Requirements

| ID | WHEN | THEN |
| --- | --- | --- |
| **RLA-01** | Leitura de `STATE.md` e `HANDOFF.md` | SHALL refletir `main` @ HEAD; L-001 marcado resolvido; próximo foco = `temas-visuais` (spec pronta) |
| **RLA-02** | Leitura de `CONCERNS.md` | Relatórios/PDF backend SHALL estar **Resolved**; sync date 2026-08-03 |
| **RLA-03** | Leitura de `ROADMAP.md` M2 | Benefícios mensais, ACL base, fix3 CLT, relatórios executivos SHALL estar **COMPLETE** ou status real |
| **RLA-04** | Leitura de `README.md` | SHALL descrever stack/arquitetura atual, funcionalidades entregues, gates de teste; sem "próximos passos" obsoletos |
| **RLA-05** | Specs úteis untracked | `temas-visuais/`, docs pendentes de features, openapi export SHALL ser versionados |
| **RLA-06** | Working tree | Lixo local (`.bak`, `test-results/`, PDFs apresentação, PoC não roteado) SHALL ser ignorado ou removido |
| **RLA-07** | Branches locais mergeadas em `main` | SHALL ser deletadas (`feat/relatorios-executivos`, `feat/auth-api-keys`, etc.) |

## Out of Scope

- Execute de `temas-visuais` (código)
- CI/CD remoto (M3)
- Deletar branches **não** mergeadas (`feat/cobertura-testes-95`, `feat/adequacao-analise-projeto`, `feat/qualidade-criticos-sonar`)
- Commit de `.scratch/`, `.cursor/settings.json`, apresentações PDF

## Verification

- [ ] `grep` CONCERNS não reporta 404 relatórios como Open
- [ ] ROADMAP não diz "stub" benefícios mensais
- [ ] `git status` limpo de artefatos versionáveis pendentes
- [ ] Branches mergeadas removidas localmente
- [ ] README aponta `_docs/specs/` como docs canônicas

## Batches (commits atômicos)

1. `docs(specs): sync STATE HANDOFF CONCERNS ROADMAP`
2. `docs(readme): align with main architecture and features`
3. `docs(specs): version pending feature specs and openapi`
4. `chore(repo): gitignore local junk and drop merged branches`
