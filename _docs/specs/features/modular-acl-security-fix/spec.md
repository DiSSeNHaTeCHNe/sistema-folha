# Modular ACL / Security Fix — Specification

**Parent feature:** `modular-monolith` (re-Verifier **PASS** 2026-07-26)  
**Sibling (do not duplicate / do not regress):** `modular-monolith-fix` (Execute + Verifier **PASS** — ArchUnit application-layer, `cadastros.port`/`auth.port`, `/auth/acesso` DTO unit test, FE lint AD-004)  
**Source:** Code-review 2026-07-26 — CRITICAL regressions not closed by parent Verifier or by `modular-monolith-fix`  
**Complexity:** Medium (Spec brief; Design written on request; Tasks skip — implicit in Execute)  
**Spec status:** Re-reviewed 2026-07-26 against current tree (post sibling PASS) — ACs still valid; evidence anchors refreshed

## Problem Statement

A migração modular entregou `OrganogramaAcessoPort` com semântica correta (“empty ≠ total access”), e o path Folha filtra via `aplicarFiltroAcesso`. Porém `BeneficioMensalService` ainda trata `Set` vazio em `buscarPorCompetencia` / `buscarResumoPorCompetencia` como query **sem escopo** — vazamento de dados para usuário restrito (`acessoTotal=false`, `temFuncionario`+`temNo`, `centrosCustoIds` vazio). Em paralelo, `POST /auth/refresh` **não** está em `permitAll` no `SecurityConfig`, enquanto o FE renova o token com `fetch` **sem** header `Authorization` — quebrando o intent MOD-13 (login/refresh/permitAll). O `DELETE` de folha migrado também não aplica ACL, ao contrário de benefícios (`removerSeAutorizado`).

O re-Verifier do pai marcou MOD-13 ✅ inspecionando só `POST /auth/login` — **falso positivo** para refresh; este fix fecha a prova e o matcher.

## Goals

- [ ] Eliminar bypass ACL empty-set em listagens/resumo de benefício mensal: empty + restrito ⇒ lista vazia (nunca query unscoped)
- [ ] Restaurar `POST /auth/refresh` como `permitAll` (paths relativos ao `context-path: /api`) + teste de segurança que prove anon → 2xx/4xx de negócio, não 401 do filtro
- [ ] Alinhar soft-delete de folha ao padrão ACL de benefícios (`remover` autorizado por login + centro)
- [ ] Cobertura de teste ancorada nos ACs abaixo (unitário ACL + MockMvc/security refresh)
- [ ] Preservar wiring de ports do sibling (`UsuarioLookupPort`, `FuncionarioConsultaPort`, `OrganogramaAcessoPort`) — zero regressão ArchUnit AD-009

## Out of Scope

Explicitamente excluído. Documentado para evitar scope creep.

| Feature | Reason |
| ------- | ------ |
| ArchUnit `..application..` ↔ foreign infra / novos ports Cadastros | Já PASS em `modular-monolith-fix`; não reabrir |
| MockMvc HTTP de `GET /auth/acesso` / AuthController | Parent ⚠️ spec-precision (service-level DTO já PASS); optional Auth MockMvc = follow-up separado |
| Zerar ESLint brownfield | AD-004 / advisory |
| Privilege escalation em `/usuarios` (ADMIN) | Deferred concern (CONCERNS / hygiene) |
| Performance N+1 em loops de importação | Feature própria |
| Password logging / higiene de log de credenciais | Deferred security hygiene |
| Remover allowlist AD-009 (dashboard/importacao) | Follow-up ports Folha/Cadastros stats |
| Reabrir migração de pacotes / remoção legado | Já entregue |
| Alterar modelo JWT / refresh token store | Só matcher + comportamento permitAll; sem redesign de auth |
| Microserviços / multi-módulo Maven | Monólito in-process permanece |
| Mudança obrigatória no FE `api.ts` | Contrato = FE sem Bearer continua válido (MODACL-09) |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here — nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Empty set + `acessoTotal=false` | Retornar lista/resumo **vazios**; **nunca** chamar query unscoped | Semântica MOD-09 / AD-007; Folha já nega via filtro in-memory | y |
| Empty set + `acessoTotal=true` | Query unscoped permanece válida (acesso total explícito) | Flag dedicada; não derivar de `isEmpty()` ambíguo | y |
| Sinal `centrosParaFiltro` | Preferir API que não conflate “total” e “vazio restrito” (ex.: short-circuit antes da query, ou branch em `acessoTotal`) — implementação livre desde que AC passe | Bug atual: `emptySet()` para total e empty IDs para restrito colidem no `if (centros.isEmpty())` | y |
| Refresh matcher | `.requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()` ao lado de `/auth/login`, paths **sem** prefixo `/api` duplicado | Alinha `SecurityConfig` + FE `fetch` sem Bearer; Spring Security 6 | y |
| JWT filter sem Bearer | Continua a cadeia (`filterChain.doFilter`) — **não** é a causa do 401; o bloqueio é `anyRequest().authenticated()` | Verificado em `JwtAuthenticationFilter` L40–43 | y |
| Logout | Permanecer autenticado (fora deste fix) | Escopo = refresh | y |
| Folha `remover` ACL = P1 | Exigir login + `aplicarFiltroAcesso` (espelho `removerSeAutorizado`); deny ⇒ 404/`false` (mesmo contrato HTTP de benefícios) | Controller `remover` só passa `id`; service sem ACL | y |
| Sibling sequencing | Execute **após** sibling PASS (já ocorrido); não depende de novos ports; **não** reintroduzir `*.infrastructure` estrangeira | AD-008/AD-009 | y |
| `listarPorFuncionarioParaUsuario` | Fora do bypass empty-set (filtra in-memory após query por funcionário); não expandir ACs salvo regressão descoberta | Path distinto de `buscarPorCompetencia` | y |

**Open questions:** none — defaults acima fecham ambiguidade.

**Evidence anchors (codebase, re-verified 2026-07-26 pós-sibling):**

| Issue | Location (current) | Still present? |
| ----- | ------------------ | -------------- |
| Empty → unscoped | `BeneficioMensalService.centrosParaFiltro` L157–161 + `buscarPorCompetencia` L196–198 / `buscarResumoPorCompetencia` L207–208 | ✅ Yes — leak |
| Test gap MODACL-05 | `BeneficioMensalServiceTest` cobre SEM_FUNCIONARIO deny + `listarPorCompetencia(..., emptySet)` como **total**; **não** cobre restrito+empty via `*ParaUsuario` | ✅ Gap remains |
| FE refresh sem Authorization | `frontend/src/services/api.ts` L85–90 — `fetch` headers só `Content-Type` | ✅ Yes |
| Refresh não permitAll | `SecurityConfig.java` L33 — só `POST /auth/login` permitAll; refresh cai em `anyRequest().authenticated()` L45 | ✅ Yes |
| Folha delete sem ACL | `FolhaPagamentoService.remover(Long)` L121–127; `FolhaPagamentoController.remover` L80–81 sem `Authentication` | ✅ Yes |
| Contraste OK | `BeneficioMensalService.removerSeAutorizado`; Folha listagens usam `aplicarFiltroAcesso` | ✅ Still valid |
| Ports (sibling) | `BeneficioMensalService` / `FolhaPagamentoService` usam `UsuarioLookupPort` + `OrganogramaAcessoPort` (+ `FuncionarioConsultaPort` / `CadastrosLookupPort`) | ✅ Preserve |

**Implicit-requirement dimensions (Medium):** Auth boundaries (refresh permitAll + ACL delete/list) cobertos nos ACs; failure/partial = lista vazia / 404 sem vazar; remaining dimensions N/A for this scope (no concurrency redesign, no external deps, no rate limits).

**Validation gap note:** Parent `modular-monolith/validation.md` (re-verify PASS) ainda marca MOD-13 com evidência só de `POST /auth/login` — falso positivo para refresh; este fix fecha matcher + prova automatizada.

---

## User Stories

### P1: Fechar bypass ACL empty-set em Benefício Mensal ⭐ MVP

**User Story**: Como operador com acesso restrito ao organograma, quero que listagens e resumos de benefício mensal nunca retornem dados globais quando meu conjunto de centros estiver vazio, para que “empty ≠ total access” valha também no domínio Benefícios.

**Why P1**: CRITICAL data leak — contradiz MOD-09 / AD-007; Folha já nega corretamente.

**Acceptance Criteria**:

1. (MODACL-01) WHEN usuário com `acessoTotal=false`, `temFuncionarioVinculado=true`, `temNoOrganograma=true` e `centrosCustoIds` **vazio** consultar listagem por competência (`listarPorCompetenciaParaUsuario` ou path equivalente) THEN o sistema SHALL retornar lista vazia e SHALL NOT invocar query unscoped de competência (sem filtro de centro)
2. (MODACL-02) WHEN o mesmo perfil consultar resumo por competência (`resumoPorCompetenciaParaUsuario` ou path equivalente) THEN o sistema SHALL retornar resumo vazio e SHALL NOT invocar agregação unscoped
3. (MODACL-03) WHEN usuário com `acessoTotal=true` consultar listagem/resumo por competência THEN o sistema SHALL continuar podendo ler todos os lançamentos ativos da competência (comportamento de acesso total explícito preservado)
4. (MODACL-04) WHEN usuário com `acessoTotal=false` e `centrosCustoIds` **não vazio** consultar por competência THEN o sistema SHALL retornar somente lançamentos cujo funcionário pertença a um dos centros do conjunto
5. (MODACL-05) WHEN testes unitários do service forem executados THEN SHALL existir cobertura que falharia se `centros.isEmpty()` voltasse a mapear para find-all sob usuário **restrito** (assert lista/resumo vazios + `verify` repository: unscoped **nunca** chamado) — distinto do caso SEM_FUNCIONARIO já existente

**Independent Test**: Mock `OrganogramaAcessoPort` com restricted+empty centers → list/resumo vazios; verify unscoped repo method never called; total-access fixture ainda chama unscoped ou equivalente documentado.

---

### P1: `POST /auth/refresh` permitAll + teste de segurança ⭐ MVP

**User Story**: Como cliente SPA com access token expirado, quero renovar a sessão enviando só o refresh token no body (sem Bearer), para que o interceptor de `api.ts` continue funcionando após a migração modular.

**Why P1**: Regressão de auth — FE já usa `fetch` sem `Authorization`; MOD-13 exige login/refresh permitAll inalterado.

**Acceptance Criteria**:

1. (MODACL-06) WHEN `SecurityConfig` for inspecionado THEN SHALL existir matcher `HttpMethod.POST` em `/auth/refresh` com `permitAll()`, usando path relativo ao `context-path` (sem duplicar `/api`), coeso com `/auth/login`
2. (MODACL-07) WHEN `POST /auth/refresh` for chamado **sem** header `Authorization` e com body JSON contendo refresh token THEN Spring Security SHALL NOT responder 401 por ausência de autenticação (a cadeia autoriza o endpoint; validade do token permanece responsabilidade do application service — 2xx ou 4xx de negócio)
3. (MODACL-08) WHEN teste de segurança (MockMvc / `@WebMvcTest` / padrão de `SecurityConfigTipoBeneficioTest`) for executado THEN SHALL provar anon `POST /auth/refresh` não bloqueado por `authenticated()`; e SHALL continuar provando que rotas autenticadas (ex. mutação ADMIN já coberta) não regrediram para `permitAll` indevido
4. (MODACL-09) WHEN o FE `frontend/src/services/api.ts` mantiver refresh via `fetch` sem Bearer THEN o contrato HTTP SHALL permanecer compatível (sem exigir mudança FE obrigatória neste fix)

**Independent Test**: Security test verde para refresh anônimo; login continua permitAll; suite `mvn test` sem regressão nos testes de `tipo-beneficio` ADMIN.

---

### P1: ACL no soft-delete de Folha (espelho benefícios) ⭐ MVP

**User Story**: Como operador restrito, quero que `DELETE /folha-pagamento/{id}` só soft-delete registros cujo funcionário/centro eu possa acessar, para não haver escalação de exclusão cross-centro no controller migrado.

**Why P1**: Superfície migrada (`folha.api` / `folha.application`) sem ACL no delete; benefícios já tem `removerSeAutorizado`.

**Acceptance Criteria**:

1. (MODACL-10) WHEN `FolhaPagamentoController.remover` for invocado THEN o controller SHALL passar o login autenticado (`Authentication`) ao service (não só o `id`)
2. (MODACL-11) WHEN usuário restrito tentar remover folha de funcionário fora dos seus `centrosCustoIds` THEN o sistema SHALL NOT soft-delete e SHALL responder como não encontrado / não autorizado no mesmo padrão HTTP de benefícios (`404` ou equivalent already used — sem vazar existência cross-centro além do contrato atual de benefícios)
3. (MODACL-12) WHEN usuário com acesso ao centro do registro remover THEN o sistema SHALL soft-delete e retornar sucesso (`204`/`noContent` conforme contrato atual)
4. (MODACL-13) WHEN testes unitários de `FolhaPagamentoService` (ou equivalente) forem executados THEN SHALL cobrir deny vs allow no `remover` com mock de `OrganogramaAcessoPort`

**Independent Test**: Dois fixtures de contexto ACL — deny não chama `softDelete`; allow chama; controller compila com `Authentication`.

---

## Edge Cases

- WHEN `acessoNegado(contexto)` (sem funcionário / sem nó) em benefício mensal THEN lista/resumo vazios (já existente) — SHALL permanecer; este fix não relaxa
- WHEN `centrosCustoIds` null no DTO de contexto THEN tratar como vazio restrito (deny), nunca unscoped — assumir normalização no port; se null chegar ao consumer, deny-safe
- WHEN refresh token ausente/inválido/expirado no body THEN application service / controller retorna erro de negócio; Security não exige JWT de access
- WHEN `DELETE` folha com id inexistente THEN 404/false sem side-effect (comportamento atual preservado)
- WHEN `acessoTotal=true` no delete de folha THEN SHALL permitir soft-delete de qualquer registro ativo encontrado
- WHEN consumer já usa ports do sibling THEN refactor ACL SHALL NOT reintroduzir `cadastros.infrastructure` / `auth.infrastructure` estrangeiros

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MODACL-01 | P1: Bypass empty-set listagem | Execute (implicit) | Done |
| MODACL-02 | P1: Bypass empty-set resumo | Execute (implicit) | Done |
| MODACL-03 | P1: Preservar acesso total | Execute (implicit) | Done |
| MODACL-04 | P1: Filtro por centros | Execute (implicit) | Done |
| MODACL-05 | P1: Testes deny empty≠unscoped | Execute (implicit) | Done |
| MODACL-06 | P1: Matcher refresh permitAll | Execute (implicit) | Done |
| MODACL-07 | P1: Refresh sem Authorization | Execute (implicit) | Done |
| MODACL-08 | P1: Security test refresh | Execute (implicit) | Done |
| MODACL-09 | P1: Compat FE fetch refresh | Execute (implicit) | Done |
| MODACL-10 | P1: Controller folha passa login | Execute (implicit) | Done |
| MODACL-11 | P1: Delete folha deny ACL | Execute (implicit) | Done |
| MODACL-12 | P1: Delete folha allow ACL | Execute (implicit) | Done |
| MODACL-13 | P1: Testes delete folha ACL | Execute (implicit) | Done |

**ID format:** `MODACL-NN`  
**Coverage:** 13 total; mapped to Tasks T1–T4; 0 unmapped  

**Parent AC linkage:**

| This fix | Parent (`modular-monolith`) |
| -------- | --------------------------- |
| MODACL-01–05 | MOD-09 / MOD-10 (consumers ACL; empty ≠ total) |
| MODACL-06–09 | MOD-13 AC5 (login/refresh/permitAll) — fecha falso positivo do Verifier |
| MODACL-10–13 | Controllers finos + ACL em superfície Folha migrada |

---

## Success Criteria

- [ ] Nenhum path de listagem/resumo de benefício mensal usa query unscoped quando `acessoTotal=false` e centros vazios
- [ ] `POST /auth/refresh` permitAll; teste de segurança prova anon sem Bearer; FE refresh continua sem mudança obrigatória
- [ ] Soft-delete de folha exige ACL alinhada a benefícios
- [ ] `mvn test` verde nos testes novos/alterados desta feature; sem regressão `SecurityConfigTipoBeneficioTest`
- [ ] Diff intencional **não** reabre ArchUnit/ports/lint do sibling; `ModularArchitectureTest` permanece verde
- [ ] Ready for Independent Verifier contra MODACL-01…13 com evidence-or-zero

---

## Sizing & Next Phase

| Item | Value |
| ---- | ----- |
| Scope | Medium |
| Specify | This document (re-reviewed 2026-07-26) |
| Design | `_docs/specs/features/modular-acl-security-fix/design.md` Approach A — **awaiting approve** |
| Tasks | `_docs/specs/features/modular-acl-security-fix/tasks.md` (T1–T4) — **awaiting approve** |
| Execute | Awaiting Design+Tasks approve → Execute (4 tasks, 1 batch inline) |

**Implicit Execute steps (preview, not tasks.md):**

1. Corrigir branch empty-set em `BeneficioMensalService` + testes MODACL-01–05 (fixture restrito+empty)  
2. Adicionar matcher + teste security refresh MODACL-06–08  
3. ACL em `FolhaPagamentoService.remover` / controller + testes MODACL-10–13  
4. Gate `mvn test` nos alvos tocados + `ModularArchitectureTest` (no regressão sibling)  
5. Verifier independente → `validation.md`
