# auth-api-keys-fix2 — ACL cadastro, erros HTTP e UX SPA Specification

**Parent:** `_docs/specs/features/auth-api-keys/` (MVP + fix1 Verified)  
**Related:** AD-011 (`ACESSO_TOTAL` ≠ `ADMIN`); AD-014 (API Key read-only); MCP/agent smoke 2026-07-30; parent edge **APIKEY-05** (“GET sem ACL → lista vazia / deny, não bypass”)  
**Complexity:** Medium  
**Spec status:** Done 2026-07-30 — Execute T1–T8 complete on `feat/auth-api-keys`; full gate PASS; pending Verifier fix2

> **Nota:** fix2 fecha lacunas **operacionais** expostas após MCP + testes manuais com API Key read-only. **Não** reimplementa MVP/fix1. Branch sugerida: `feat/auth-api-keys-fix2` (commits prefixados `fix2:`).

## Problem Statement

Com API Key read-only, endpoints de **folha** e **benefícios** respeitam ACL organograma (ex.: 10 empregados no escopo Plugin vs 310 globais). Porém **`GET /funcionarios`** e **`GET /usuarios`** retornam cadastro **global** (410/411 registros), violando APIKEY-05 e induzindo agentes MCP a inferir escopo errado. Adicionalmente, **`GET /beneficio-mensal` sem query params** responde **500** em vez de **400**; a SPA trata **403 como 401** no interceptor axios, deslogando o usuário ao falhar create de API Key (ADMIN na rota, sem permissão `API_KEY`, recebe 403 legítimo).

## Goals

- [x] Aplicar **ACL de leitura** em listagens e consultas por ID de **funcionários** e **usuários** (JWT e Bearer API Key)
- [x] Garantir **400** para parâmetros obrigatórios ausentes em `GET /beneficio-mensal`
- [x] Corrigir interceptor FE para **não** refresh/logout em **403** de permissão de negócio
- [x] Alinhar UX da página `/api-keys`: ADMIN sem `API_KEY` não tenta create silencioso que desloga
- [x] Evidência automatizada (unit/WebMvc/Vitest) comparando JWT vs API Key nos novos filtros ACL

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| ACL em todos os cadastros (cargos, rubricas, centros-custo, linhas-negocio, organograma CRUD) | fix2 = gaps críticos para agente; matriz completa → follow-up |
| Resumo folha global unscoped (Deferred AD-011) | Comportamento existente; fora deste fix |
| Servidor MCP / OpenAPI bridge / `mcp.json` | Infra local já configurada; não é produto backend |
| Rate limit, escopos por domínio (APIKEY-18/19) | Deferred parent |
| Permitir ADMIN criar API Key **sem** possuir `API_KEY` | Parent exige `API_KEY` para create; fix2 alinha UX, não relaxa gate |
| Sanitização CWE-117 write-guard log | Deferred fix1 code-review |
| Mutations cadastro via API Key | Já bloqueadas write-guard (403) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| IDs de requisito | Prefixo `FIX2-NN` nesta spec; refinam **APIKEY-05** e gaps operacionais | Padrão fix1 | y |
| Escopo ACL cadastro | Mesmo `OrganogramaAcessoPort` / `AccessContextDTO` usado em Folha/Benefícios | AD-008 via port; paridade domínios | y |
| `acessoTotal=true` | Listagens cadastro retornam **todos** os registros (comportamento atual) | AD-011 | y |
| Escopo parcial — funcionários | Filtrar por `funcionario.centroCusto.id ∈ centrosCustoIds`; funcionário **sem** CC → **excluído** da lista scoped | Paridade `CentroCustoEfetivo` folha | y |
| Escopo parcial — usuários | Listar apenas usuários cujo `funcionario` vinculado tem CC no escopo; usuários **sem** funcionário → **excluídos** | Evita vazar cadastro admin/RH fora do nó | y |
| GET por ID fora do escopo | **`404`** (Not Found), não 403 | Não vazar existência; padrão cadastro existente | y |
| Parâmetro `centroCustoId` em `GET /funcionarios` | Se informado e **fora** do escopo → lista **vazia** (200 + `[]`), não 403 | Consistente com folha centro-custo deny | y |
| Controller cadastro | Passar `Authentication.getName()` ao service (padrão `BeneficioMensalController`) | Brownfield | y |
| Cross-domain | `cadastros.application` / `auth.application` dependem de `OrganogramaAcessoPort` + `UsuarioLookupPort` apenas | AD-008/AD-010 | y |
| FE 403 | Interceptor refresh/logout **somente** em **401**; 403 propaga erro ao caller | 403 = permissão negada, sessão válida | y |
| UI create key | Botão create **desabilitado** + Alert explicativa se usuário tem `ADMIN` mas **não** `API_KEY` | Gate backend inalterado (APIKEY-02); FIX2-CTX-02 | y |
| Teste paridade ACL | `@WebMvcTest` ou teste de service: mesmo login JWT vs Bearer `sf_live_*` → mesmo status e cardinalidade | Extensão fix1 FIX1-06 | y |

**Open questions:** none — OQ-1/OQ-2 resolvidos em `context.md` (defaults confirmados 2026-07-30).

**Remaining dimensions N/A for this scope:** rate limits, external deps, idempotency, data expiry, concurrency beyond read filters.

---

## User Stories

### P1: ACL em `GET /funcionarios` ⭐ MVP

**User Story**: Como operador com escopo organograma (ou agente MCP com API Key), quero que listagens e consultas de funcionários respeitem meus centros de custo acessíveis, para não ver cadastro global.

**Why P1**: Evidência MCP — 410 funcionários retornados vs 10 no escopo folha; violação direta APIKEY-05.

**Acceptance Criteria**:

1. (FIX2-01) WHEN usuário **U** com `acessoTotal=false` e `centrosCustoIds` não vazio chamar `GET /funcionarios` THEN resposta SHALL ser **`200`** e lista SHALL conter **somente** funcionários ativos cujo `centroCusto.id ∈ centrosCustoIds` (funcionários sem CC excluídos)
2. (FIX2-02) WHEN **U** chamar `GET /funcionarios/{id}` para funcionário cujo CC **não** está no escopo THEN resposta SHALL ser **`404`**
3. (FIX2-03) WHEN **U** com `acessoTotal=true` chamar `GET /funcionarios` THEN resposta SHALL manter comportamento atual (lista completa conforme filtros query)
4. (FIX2-04) WHEN **U** autenticado via **Bearer API Key read-only** chamar os mesmos GETs de FIX2-01/02/03 THEN respostas SHALL ter **mesmo status HTTP** e **mesma cardinalidade** que JWT para **U** (paridade fix1)

**Independent Test**: `FuncionarioServiceTest` + `FuncionarioAclWebMvcTest` — usuário scoped CC {793,825} → N registros; global user → 410; API Key parity case.

---

### P1: ACL em `GET /usuarios` ⭐ MVP

**User Story**: Como operador scoped, quero listagens de usuários filtradas pelo organograma, para agentes não enumerarem contas globais.

**Why P1**: MCP retornou 411 usuários sem filtro — mesmo risco de inferência errada.

**Acceptance Criteria**:

1. (FIX2-05) WHEN **U** com `acessoTotal=false` chamar `GET /usuarios` THEN resposta SHALL ser **`200`** e lista SHALL conter **somente** usuários ativos cujo `funcionario` vinculado tem `centroCusto.id ∈ centrosCustoIds` (usuários sem funcionário **excluídos** — ver OQ-1)
2. (FIX2-06) WHEN **U** chamar `GET /usuarios/{id}` para usuário cujo funcionário está fora do escopo (ou sem funcionário) THEN resposta SHALL ser **`404`**
3. (FIX2-07) WHEN **U** com `acessoTotal=true` chamar `GET /usuarios` THEN lista SHALL permanecer global (comportamento atual)
4. (FIX2-08) WHEN Bearer API Key de **U** chamar FIX2-05/06/07 THEN paridade JWT vs API Key (status + cardinalidade) SHALL hold

**Independent Test**: `UsuarioServiceTest` + WebMvc — scoped vs total; paridade Bearer.

---

### P1: Validação HTTP `GET /beneficio-mensal` ⭐ MVP

**User Story**: Como integrador/agente, quero **400** claro quando omitir parâmetros obrigatórios, para não interpretar falha de servidor.

**Why P1**: MCP smoke — `GET /beneficio-mensal` sem params → **500**; agente não consegue autocorrigir.

**Acceptance Criteria**:

1. (FIX2-09) WHEN cliente autenticado chamar `GET /beneficio-mensal` **sem** `competenciaInicio` e/ou `competenciaFim` THEN resposta SHALL ser **`400`** com corpo de erro padronizado (`GlobalExceptionHandler`), **NOT** `500`
2. (FIX2-10) WHEN mesma requisição inválida for repetida com Bearer API Key THEN status SHALL ser **`400`** (mesmo que JWT)

**Independent Test**: WebMvc ou `MockMvc` — GET `/beneficio-mensal` → `400`; teste de regressão no handler se necessário.

---

### P1: Interceptor axios — 403 ≠ logout ⭐ MVP

**User Story**: Como usuário SPA com sessão JWT válida, quero permanecer logado quando uma ação retorna 403 de permissão, para ver erro de negócio em vez de logout.

**Why P1**: Bug reportado — create API Key por ADMIN sem `API_KEY` → 403 → refresh → logout.

**Acceptance Criteria**:

1. (FIX2-11) WHEN resposta HTTP tiver status **`401`** THEN interceptor axios SHALL tentar refresh token e, se falhar, SHALL disparar logout (comportamento atual preservado)
2. (FIX2-12) WHEN resposta HTTP tiver status **`403`** THEN interceptor SHALL **NOT** tentar refresh nem `logoutOnAuthFailure`; SHALL rejeitar a promise com erro para o caller tratar
3. (FIX2-13) WHEN teste Vitest simular resposta **403** de `POST /auth/api-keys` THEN tokens SHALL permanecer armazenados e evento `auth:logout` SHALL **NOT** ser disparado

**Independent Test**: `frontend/src/services/api.test.ts` (ou equivalente) — mock 403 vs 401.

---

### P2: UX create API Key — ADMIN sem `API_KEY`

**User Story**: Como ADMIN sem permissão `API_KEY`, quero feedback claro na UI em vez de botão que falha e confunde.

**Acceptance Criteria**:

1. (FIX2-14) WHEN usuário tiver `ADMIN` mas **não** `API_KEY` THEN página `/api-keys` SHALL exibir controles de **create desabilitados** (ou ocultos) e mensagem indicando necessidade da permissão `API_KEY`
2. (FIX2-15) WHEN usuário tiver `API_KEY` THEN create SHALL permanecer habilitado (sem regressão)

**Independent Test**: Vitest em `ApiKeys/index.tsx` ou teste dedicado — ADMIN-only vs API_KEY user.

---

## Edge Cases

- WHEN scoped user não tem CC acessíveis (`centrosCustoIds` vazio, `acessoTotal=false`) THEN `GET /funcionarios` e `GET /usuarios` SHALL retornar **`200`** com lista **vazia** (paridade folha)
- WHEN query `centroCustoId` em `/funcionarios` aponta CC fora do escopo THEN lista vazia (200), não 403
- WHEN JWT e API Key do mesmo **U** consultam recurso in-scope THEN payloads SHALL ser equivalentes (mesmos IDs); out-of-scope → 404 em ambos
- WHEN write-guard bloqueia `POST /funcionarios` via API Key THEN continua **403** write-guard (fora escopo fix2 prod, apenas regressão)
- WHEN `GET /usuarios/login/{login}` for usado THEN mesma regra ACL de FIX2-06 (404 se fora escopo)

---

## Implicit-Requirement Dimensions Sweep (Medium)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | FIX2-09/10 — params obrigatórios benefício |
| Failure / partial-failure | FIX2-11/12 — 403 não derruba sessão |
| Auth boundaries | FIX2-01…08 — ACL cadastro read path |
| Observability | N/A — sem novos logs obrigatórios |
| Remaining dimensions | N/A — rate limit, external deps, concurrency, expiry |

---

## Requirement Traceability

| Requirement ID | Story | Refina (parent) | Phase | Status |
| -------------- | ----- | --------------- | ----- | ------ |
| FIX2-01 | P1 Funcionários list scoped | APIKEY-05 | Tasks | Done |
| FIX2-02 | P1 Funcionários get 404 | APIKEY-05 | Tasks | Done |
| FIX2-03 | P1 Funcionários acessoTotal | APIKEY-05 | Tasks | Done |
| FIX2-04 | P1 Funcionários API Key parity | APIKEY-05 | Tasks | Done |
| FIX2-05 | P1 Usuários list scoped | APIKEY-05 | Tasks | Done |
| FIX2-06 | P1 Usuários get 404 | APIKEY-05 | Tasks | Done |
| FIX2-07 | P1 Usuários acessoTotal | APIKEY-05 | Tasks | Done |
| FIX2-08 | P1 Usuários API Key parity | APIKEY-05 | Tasks | Done |
| FIX2-09 | P1 Benefício 400 | — | Tasks | Done |
| FIX2-10 | P1 Benefício 400 API Key | — | Tasks | Done |
| FIX2-11 | P1 FE 401 refresh | — | Tasks | Done |
| FIX2-12 | P1 FE 403 no logout | — | Tasks | Done |
| FIX2-13 | P1 FE 403 test | — | Tasks | Done |
| FIX2-14 | P2 UI ADMIN create disabled | APIKEY-02 (UX) | Tasks | Done |
| FIX2-15 | P2 UI API_KEY create OK | APIKEY-01 | Tasks | Done |

**Coverage:** 15 total (13 P1 + 2 P2), todos mapeáveis a tasks fix2.

**Cross-ref parent:** Após Execute fix2, atualizar `auth-api-keys/spec.md` — APIKEY-05 passa de “Verified (fix1 smoke folha)” para **Verified (fix2 cadastro)**.

---

## Success Criteria

- [x] Usuário scoped (ex. Humberto / Plugin) — `GET /funcionarios` retorna ~10, não 410; API Key idêntica ao JWT
- [x] `GET /beneficio-mensal` sem params → **400**
- [x] ADMIN sem `API_KEY` cria key na UI → permanece logado; vê mensagem ou botão desabilitado
- [x] Gates: `mvn test` (novos testes ACL + benefício) + `npm test` (api interceptor + ApiKeys UI) verdes
- [ ] Verifier fix2 PASS; `validation.md` append-only

---

## Evidence Base (MCP smoke 2026-07-30)

| Endpoint | Scoped (Plugin) | Global | Avaliação |
| -------- | --------------- | ------ | --------- |
| `/resumo-folha-pagamento?ano=2026&mes=5` | 10 emp / R$ 66.040 líq. | 310 / R$ 2,45 mi | ✅ ACL OK |
| `/folha-pagamento/totais-funcionarios` | 10 func. | — | ✅ ACL OK |
| `/funcionarios` | **410** | 410 | ❌ gap fix2 |
| `/usuarios` | **411** | 411 | ❌ gap fix2 |
| `POST /folha-pagamento/processar` | 403 | — | ✅ read-only OK |
| `/beneficio-mensal` (sem params) | **500** | — | ❌ gap fix2 |
