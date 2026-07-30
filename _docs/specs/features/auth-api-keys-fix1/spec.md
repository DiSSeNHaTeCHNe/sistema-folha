# auth-api-keys-fix1 — Evidência HTTP, ACL e observabilidade Specification

**Parent:** `_docs/specs/features/auth-api-keys/` (MVP executado `e8b57ad..e201a7c` em `feat/auth-api-keys`)  
**Related:** AD-008, AD-014; `validation.md` gaps pós fix-cycle-1; code-review classificação **(b)**  
**Complexity:** Medium  
**Spec status:** Execute complete 2026-07-30 on `feat/auth-api-keys`; pending Verifier fix1

> **Nota:** Esta revisão **não reimplementa** o MVP. Fecha lacunas de **spec-precision** e evidência automatizada apontadas pelo Verifier e code-review após a execução inicial. Branch de implementação continua `feat/auth-api-keys` (commits prefixados `fix1:`).

## Problem Statement

O MVP de API Keys entregou comportamento correto em unit tests e build, mas vários ACs P1 ficaram com evidência **indireta** (service/filter) ou **build-only** (UI): HTTP 403/400 não assertados no controller, ACL não verificada end-to-end, observabilidade do write-guard ausente, `ultimo_uso_em` persistido na schema mas nunca atualizado, e UI sem Vitest. Isso impede fechar a feature Large com confiança de regressão e deixa gaps documentados em `validation.md` abertos.

## Goals

- [x] Fechar evidência **HTTP** para create/list/403/400 de `/auth/api-keys` via `@WebMvcTest`
- [x] Provar que API Key **herda ACL de leitura** (smoke comparativo JWT vs Bearer key)
- [x] Entregar log auditável em bloqueio write-guard (sem secret)
- [x] Atualizar `ultimo_uso_em` em auth bem-sucedida e refletir na listagem
- [x] Cobrir gate de rota UI e chip `API_KEY` com Vitest (padrão `AdminRoute.test.tsx`)

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Reimplementar MVP (migration, service, filters, UI completa) | Já entregue; fix1 = evidência + lacunas |
| Playwright E2E fluxo completo create→copy→revoke | P3; Vitest cobre gates críticos |
| Matriz ACL completa em todos os domínios | Smoke 1 endpoint com `OrganogramaAcessoPort` basta |
| Rate limit / escopos FULL (APIKEY-18/19) | Deferred no parent |
| Otimizar BCrypt no hot path | Tradeoff de segurança; fora deste fix |
| Análise Sonar na branch | Operacional pré-merge; não é AC |
| Novos endpoints ou campos de request | Sem drift do contrato MVP |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| IDs de requisito | Prefixo `FIX1-NN` nesta spec; refinam ACs `APIKEY-*` do parent | Padrão `folha-custo-clt-fix1` | y |
| Teste HTTP | `@WebMvcTest(controllers = ApiKeyController.class)` + `@Import(SecurityConfig.class)` + `@MockBean ApiKeyService` | Padrão existente (`OrganogramaControllerWebMvcTest`) | y |
| ACL smoke endpoint | `GET` em recurso já coberto por ACL organograma (ex.: listagem scoped) — escolher **um** endpoint estável no codebase | Evita inventar rota nova | y |
| Comparação ACL | Mesmo usuário U: resposta JWT vs Bearer API Key SHALL ser igual (status) quando port nega/permite | Operationalização de APIKEY-05 | y |
| Log write-guard | `WARN` com `login` do `Authentication`; sem header `Authorization` | Parent implicit Observability + spring-security skill | y |
| `ultimo_uso_em` | Update síncrono no commit de `autenticarPorChave` bem-sucedida; sem batch/async | MVP volume baixo | y |
| UI tests | Vitest + Testing Library; mock `useAuth` / services — **sem** MSW global | AD-004 brownfield; espelhar `AdminRoute.test.tsx` | y |
| PUT/PATCH write-guard | Testes unitários adicionais no filter existente | Gap test-quality code-review; não muda produto | y |

**Open questions:** none — gaps derivados de `validation.md` e code-review já classificados **(b)**.

**Remaining dimensions N/A for this scope:** rate limits, external deps, concurrency beyond sync update, data archival.

---

## User Stories

### P1: Contrato HTTP `/auth/api-keys` (WebMvc) ⭐ MVP

**User Story**: Como mantenedor, quero testes HTTP que assertem status e corpo nos caminhos felizes e de erro do controller, para que APIKEY-01/02/03/09 não dependam só de unit tests do service.

**Why P1**: Verifier marcou APIKEY-02/03 como spec-precision; fix-cycle-1 corrigiu handler 403 mas sem assert HTTP no POST.

**Acceptance Criteria**:

1. (FIX1-01) WHEN `@WebMvcTest` enviar `POST /auth/api-keys` com JWT mockado de usuário **sem** permissão `API_KEY` e body JSON válido THEN resposta SHALL ser **`403`** com corpo de erro padronizado (`GlobalExceptionHandler`) — refina **APIKEY-02**
2. (FIX1-02) WHEN `POST /auth/api-keys` com JWT + `API_KEY` e body `{"nome":""}` ou omitindo `nome` THEN resposta SHALL ser **`400`** (Bean Validation `@NotBlank`) — refina **APIKEY-03**
3. (FIX1-03) WHEN `POST /auth/api-keys` com `diasValidade` = `0` ou `366` THEN resposta SHALL ser **`400`** (Bean Validation `@Min`/`@Max`) — refina **APIKEY-03**
4. (FIX1-04) WHEN `POST /auth/api-keys` com JWT + `API_KEY` e body válido THEN resposta SHALL ser **`201`** e JSON SHALL conter `id`, `nome`, `prefixo`, `chave`, `dataExpiracao`, `escopo` com valor **`READ`** — refina **APIKEY-01**
5. (FIX1-05) WHEN `GET /auth/api-keys` com JWT + `API_KEY` THEN resposta SHALL ser **`200`** e cada item SHALL conter metadados **sem** campo `chave` nem hash — refina **APIKEY-09**

**Independent Test**: `mvn test -Dtest=ApiKeyControllerWebMvcTest` — 5 casos verdes assertando status HTTP exatos.

---

### P1: ACL de leitura com Bearer API Key (smoke) ⭐ MVP

**User Story**: Como operador de segurança, quero prova automatizada de que API Key não bypassa ACL organograma em leitura.

**Why P1**: APIKEY-05 só tinha assert de `SecurityContext` no filter test.

**Acceptance Criteria**:

1. (FIX1-06) WHEN usuário **U** autenticado via **JWT** chamar `GET` no endpoint escolhido para ACL smoke com condição de **negado** pelo `OrganogramaAcessoPort` THEN resposta SHALL ser **`403`** ou **`200`** com lista vazia — conforme regra **já existente** desse endpoint para JWT
2. (FIX1-06b) WHEN o **mesmo U** autenticado via **Bearer API Key válida** (marker read-only) chamar o **mesmo GET** na mesma condição ACL THEN resposta SHALL ter o **mesmo status HTTP** que FIX1-06 para JWT
3. (FIX1-06c) WHEN testes FIX1-06/06b forem executados THEN SHALL falhar se autenticação API Key retornasse **`200` com dados** onde JWT retorna deny/vazio

**Independent Test**: Teste dedicado (ex.: `ApiKeyAclWebMvcTest` ou caso em suite existente) com `@MockBean OrganogramaAcessoPort` configurado para deny — compara JWT `@WithMockUser` vs filter API Key simulado.

---

### P1: Observabilidade write-guard ⭐ MVP

**User Story**: Como operador de segurança, quero log de tentativas de mutação bloqueadas por API Key, sem expor o secret.

**Why P1**: Dimensão Observabilidade do parent exigia log em falha write-guard; implementação MVP não logava.

**Acceptance Criteria**:

1. (FIX1-07) WHEN `ApiKeyWriteGuardFilter` bloquear mutação (`POST`/`PUT`/`PATCH`/`DELETE`) por marker read-only THEN SHALL emitir log **`WARN`** contendo o **`login`** (ou identificador equivalente) do principal autenticado
2. (FIX1-07b) WHEN log FIX1-07 for emitido THEN mensagem SHALL **NOT** conter substring `sf_live_`, header `Authorization` completo, nem secret da key
3. (FIX1-07c) WHEN teste unitário do filter simular bloqueio THEN SHALL assertar presença do log (appender/capturing) conforme padrão `ApiKeyServiceTest` secret-not-logged

**Independent Test**: `ApiKeyWriteGuardFilterTest` + appender — mutação bloqueada gera WARN com login, zero secret.

---

### P1: `ultimo_uso_em` na autenticação ⭐ MVP

**User Story**: Como dono da key, quero ver na listagem quando a key foi usada pela última vez, para auditoria operacional.

**Why P1**: Schema/DTO/UI já expõem coluna; auth nunca atualizava — wiring incompleto (code-review **c** elevado a fix por coluna já exposta ao usuário).

**Acceptance Criteria**:

1. (FIX1-08) WHEN `autenticarPorChave` autenticar com sucesso THEN SHALL persistir `ultimo_uso_em = OffsetDateTime.now()` (ou equivalente) na linha `api_keys` correspondente
2. (FIX1-08b) WHEN autenticação falhar (revogada, expirada, hash inválido, sem `API_KEY`) THEN SHALL **NOT** atualizar `ultimo_uso_em`
3. (FIX1-08c) WHEN `GET /auth/api-keys` após uso bem-sucedido THEN item SHALL incluir `ultimoUsoEm` **não nulo** refletindo o último uso

**Independent Test**: `ApiKeyServiceTest` — auth OK → captor `save` com timestamp; list DTO populated.

---

### P1: Vitest — gate de rota e permissão UI ⭐ MVP

**User Story**: Como mantenedor FE, quero testes automatizados nos gates críticos da UI de API Keys, alinhados ao padrão brownfield Vitest.

**Why P1**: APIKEY-14/16c tinham só build gate; risco de regressão silenciosa na rota.

**Acceptance Criteria**:

1. (FIX1-09) WHEN `ApiKeyRoute` renderizar com usuário **sem** `API_KEY` e **sem** `ADMIN` THEN SHALL redirecionar para `/dashboard` (assert `getByText` destino) — refina **APIKEY-16c**
2. (FIX1-10) WHEN usuário com permissão `API_KEY` (não necessariamente ADMIN) THEN `ApiKeyRoute` SHALL renderizar rota filha — refina **APIKEY-16c**
3. (FIX1-11) WHEN teste de `Usuarios` (ou equivalente) inspecionar `permissoesDisponiveis` THEN array SHALL incluir string **`API_KEY`** — refina **APIKEY-14**

**Independent Test**: `npm test -- ApiKeyRoute.test.tsx` + assert em `Usuarios.test.tsx` — Vitest verde.

---

### P2: Write-guard PUT/PATCH (test quality)

**User Story**: Como mantenedor, quero cobertura simétrica dos métodos mutáveis no filter test.

**Acceptance Criteria**:

1. (FIX1-12) WHEN testes `ApiKeyWriteGuardFilterTest` executarem com marker read-only THEN casos **`PUT`** e **`PATCH`** SHALL retornar **`403`** e SHALL NOT invocar `filterChain.doFilter` — refina cobertura **APIKEY-17**

**Independent Test**: `@ParameterizedTest` ou dois métodos — PUT/PATCH → 403.

---

## Edge Cases

- WHEN WebMvc test mockar `ApiKeyService` THEN controller SHALL ser testado no contrato HTTP; service continua coberto por `ApiKeyServiceTest` existente
- WHEN update `ultimo_uso_em` concorrer com revoke THEN último estado persistido SHALL respeitar transação (revoke prevalece; auth subsequente falha)
- WHEN log write-guard e principal anônimo (não deve ocorrer com marker) THEN filter SHALL NOT log secret mesmo em edge case
- WHEN FIX1-06 endpoint escolhido mudar comportamento ACL no futuro THEN teste SHALL documentar endpoint fixo no nome do teste

---

## Implicit-Requirement Dimensions Sweep (Medium)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | FIX1-02/03 — HTTP 400 via `@Valid` no controller |
| Failure / partial-failure | FIX1-01 — 403 HTTP explícito |
| Auth boundaries | FIX1-06 — ACL smoke JWT vs API Key |
| Observability | FIX1-07 — WARN write-guard |
| Data lifecycle | FIX1-08 — `ultimo_uso_em` |
| Remaining dimensions | N/A — rate limit, external deps, concurrency avançada |

---

## Requirement Traceability

| Requirement ID | Story | Refina (parent) | Phase | Status |
| -------------- | ----- | --------------- | ----- | ------ |
| FIX1-01 | P1 HTTP 403 | APIKEY-02 | Tasks | Verified (fix1) |
| FIX1-02 | P1 HTTP 400 nome | APIKEY-03 | Tasks | Verified (fix1) |
| FIX1-03 | P1 HTTP 400 dias | APIKEY-03 | Tasks | Verified (fix1) |
| FIX1-04 | P1 HTTP 201 create | APIKEY-01 | Tasks | Verified (fix1) |
| FIX1-05 | P1 HTTP 200 list | APIKEY-09 | Tasks | Verified (fix1) |
| FIX1-06 | P1 ACL deny JWT | APIKEY-05 | Tasks | Verified (fix1) |
| FIX1-06b | P1 ACL deny API Key | APIKEY-05 | Tasks | Verified (fix1) |
| FIX1-06c | P1 ACL discrimination | APIKEY-05 | Tasks | Verified (fix1) |
| FIX1-07 | P1 log WARN | Observability | Tasks | Verified (fix1) |
| FIX1-07b | P1 log sem secret | Observability | Tasks | Verified (fix1) |
| FIX1-07c | P1 log test | Observability | Tasks | Verified (fix1) |
| FIX1-08 | P1 ultimo uso write | — (schema/UI) | Tasks | Verified (fix1) |
| FIX1-08b | P1 ultimo uso skip fail | — | Tasks | Verified (fix1) |
| FIX1-08c | P1 ultimo uso list | — | Tasks | Verified (fix1) |
| FIX1-09 | P1 UI redirect | APIKEY-16c | Tasks | Verified (fix1) |
| FIX1-10 | P1 UI allow | APIKEY-16c | Tasks | Verified (fix1) |
| FIX1-11 | P1 chip API_KEY | APIKEY-14 | Tasks | Verified (fix1) |
| FIX1-12 | P2 PUT/PATCH 403 | APIKEY-17 | Tasks | Verified (fix1) |

**Coverage:** 16 total (13 P1 + 1 P2), todos mapeáveis a tasks fix1.

**Cross-ref parent:** Após Execute fix1, atualizar `auth-api-keys/spec.md` traceability — marcar APIKEY-02/03/05/09/14/16c como **Verified (fix1)** onde FIX1-* fechar evidência.

---

## Success Criteria

- [x] `ApiKeyControllerWebMvcTest` (ou nome equivalente) passa com asserts HTTP 403/400/201/200
- [x] Smoke ACL FIX1-06 compara JWT vs API Key no mesmo status
- [x] Write-guard emite WARN auditável sem secret; teste com appender passa
- [x] `ultimo_uso_em` atualizado após auth OK e visível na listagem
- [x] Vitest `ApiKeyRoute` + assert `API_KEY` em Usuários passam
- [x] Gate quick parent + novos testes: `mvn test -Dtest=ApiKey*Test` + `npm test` verde
- [ ] Verifier fix1 PASS; `validation.md` append-only com seção fix1
