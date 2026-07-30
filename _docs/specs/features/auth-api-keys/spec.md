# Auth — API Keys Specification

**Related:** conversa MCP (agente consulta dados); AD-008 (pacote `auth.*`); AD-011 (`ACESSO_TOTAL` ≠ `ADMIN`); AD-014 (API Key read-only)  
**Complexity:** Large  
**Spec status:** MVP **Executed** 2026-07-30 (`feat/auth-api-keys`, `e8b57ad..e201a7c`); Verifier PASS com gaps spec-precision → **`auth-api-keys-fix1`** (Draft 2026-07-30)  
**Confirmed:** 2026-07-29 (A–F); **C supersedida → escopo READ-ONLY** (ver `context.md`)

## Problem Statement

Integrações e agentes (ex.: servidor MCP no Cursor) precisam autenticar na API sem usar o JWT de sessão do browser (access curto + refresh). Hoje só existe login/refresh; não há credencial de longa duração por usuário. Sem API Key, o cliente teria que colar JWT no `mcp.json` (expira, difícil de revogar de forma isolada) ou abrir um buraco com token compartilhado (bypass de ACL e sem auditoria por pessoa). Keys com os mesmos poderes de escrita do usuário ampliariam o impacto de prompt injection em agentes — por isso toda API Key é **somente leitura**.

## Goals

- [ ] Permitir que usuário com permissão `API_KEY` crie, liste e revogue API Keys pessoais (UI + API) via sessão JWT
- [ ] Autenticar requests HTTP com API Key no mesmo usuário dono (authorities de leitura + ACL de organograma)
- [ ] Garantir que autenticação via API Key **não** permita mutações HTTP (POST/PUT/PATCH/DELETE → 403)
- [ ] Garantir que o secret da chave seja exibido apenas na criação e nunca persistido em claro
- [ ] Liberar/bloquear uso de API Key via permissão `API_KEY` em `usuario_permissoes`
- [ ] Exigir expiração com TTL máximo de 365 dias
- [ ] Permitir que `ADMIN` revogue keys de qualquer usuário (via JWT)
- [ ] Preparar o contrato HTTP para consumo por MCP/scripts sem implementar o servidor MCP nesta feature

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Servidor MCP (tools, host Cursor, `mcp.json` de produto) | Feature seguinte; esta entrega = credencial + auth read-only + UI |
| OAuth / fluxo `mcp_auth` do Cursor | Alternativa futura; MVP = PAT estático |
| Rate limiting / quotas por chave | Observabilidade mínima basta no MVP |
| Rotação automática de chaves | Revogação + nova criação cobre o MVP |
| Escopo FULL / escrita via API Key | Explicitamente rejeitado; mitigação prompt-injection / agente |
| Escopos granulares além de READ (ex. por domínio) | Deferred; MVP = read-only global na key |
| API Key de service account sem `Usuario` | Todo token deve mapear a um usuário para ACL |
| Reutilizar `refresh_tokens` como API Key | Modelo e ciclo de vida diferentes |
| Alterar regras de ACL organograma / `ACESSO_TOTAL` | Já cobertas por features ACL; API Key só herda leitura |
| Portal self-service externo / multi-tenant | Fora do escopo do PROJECT.md |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Domínio / pacote | `auth.*` + filtro em `security/` | AD-008 | y |
| Formato da chave | Prefixo `sf_live_` + secret (≥32 bytes, Base64URL); lookup por prefixo | Gray E | y |
| Header HTTP | `Authorization: Bearer <api-key>` | Gray E | y |
| Persistência do secret | Hash BCrypt + `prefixo`; secret só na resposta de create | spring-security | y |
| Quem cria/lista/revoga | Dono com `API_KEY` nas próprias; `ADMIN` revoga qualquer — **sempre via JWT** (SPA) | Gray A+B; mutações de key bloqueadas se Bearer for API Key | y |
| Gate de uso | Permissão `API_KEY` para create/manage (JWT) e para autenticar via key | Gray A | y |
| Escopo da key | **Todas as API Keys são READ-ONLY** (sem opção FULL no MVP) | Gray C supersedida — mitigar escrita via agente/prompt injection | y (2026-07-29) |
| Expiração | Obrigatória; TTL 1..365 dias; default 365 se omitido | Gray F | y |
| Surface UI | Tela `/api-keys` no P1 + chip `API_KEY` em Usuários; UI indica “somente leitura” | Gray D | y |
| Convivência JWT | Bearer `sf_live_…` → API Key read-only; JWT → acesso completo da sessão | SPA inalterado | y |
| Logs | Nunca secret/`Authorization` completo; pode `prefixo` + `usuarioId` | spring-security | y |
| Key alheia (não-ADMIN) | `404` | Não vazar existência | y |
| Nomes duplicados | Permitidos por usuário | Discuss | y |
| Métodos HTTP permitidos com API Key | `GET`, `HEAD`, `OPTIONS` apenas | Definição operacional de read-only | y (ajuste 2026-07-29) |

**Open questions:** none — C atualizada para READ-ONLY.

---

## User Stories

### P1: Criar API Key (secret one-shot + expiry) ⭐ MVP

**User Story**: Como usuário com permissão `API_KEY`, quero criar uma API Key nomeada com validade até 365 dias, para autenticar integrações/agentes em modo somente leitura.

**Why P1**: Sem create + secret one-shot, não há credencial utilizável.

**Acceptance Criteria**:

1. (APIKEY-01) WHEN um usuário autenticado (**JWT**) com permissão `API_KEY` enviar `POST /auth/api-keys` com `nome` válido e `diasValidade` ∈ [1, 365] (ou omitido → 365) THEN o sistema SHALL criar um registro ligado a esse `usuario_id`, SHALL definir `data_expiracao` = agora + `diasValidade`, SHALL retornar `201` com `id`, `nome`, `prefixo`, `chave` (secret completo), `dataExpiracao`, `escopo=READ` (ou equivalente), e metadados, e SHALL persistir apenas o **hash** da chave (nunca o secret em claro)
2. (APIKEY-02) WHEN o mesmo usuário sem permissão `API_KEY` tentar `POST /auth/api-keys` THEN o sistema SHALL responder `403`
3. (APIKEY-03) WHEN `nome` estiver ausente/em branco **ou** `diasValidade` estiver fora de [1, 365] THEN o sistema SHALL responder `400` (Bean Validation)
4. (APIKEY-04) WHEN a chave for criada THEN o valor completo SHALL começar com `sf_live_` e SHALL ter ≥ 32 bytes de entropia no secret

**Independent Test**: Login JWT com `API_KEY` → POST → body contém `chave` uma vez e indica read-only; DB tem hash ≠ `chave`.

---

### P1: Autenticar request com API Key (leitura + ACL) ⭐ MVP

**User Story**: Como cliente de integração, quero enviar a API Key no Bearer e ler dados no mesmo escopo ACL do usuário dono, sem poder alterar o sistema.

**Why P1**: Valor da feature + mitigação de escrita indevida.

**Acceptance Criteria**:

1. (APIKEY-05) WHEN uma request **GET** (ou HEAD/OPTIONS) com Bearer de API Key válida (não revogada, não expirada, usuário ativo, com `API_KEY`) chamar um endpoint protegido de leitura THEN o sistema SHALL autenticar como o `Usuario` dono e SHALL aplicar a mesma ACL/`OrganogramaAcessoPort` (e authorities de papel para autorização de leitura) que um login JWT desse usuário
2. (APIKEY-06) WHEN a key estiver revogada, expirada, hash não bater, usuário inativo, ou usuário sem `API_KEY` THEN o sistema SHALL **não** autenticar via API Key (→ `401` nos endpoints autenticados)
3. (APIKEY-07) WHEN o Bearer for um JWT válido (fluxo atual) THEN o sistema SHALL continuar autenticando via JWT **com mutações permitidas** (sem regressão SPA)
4. (APIKEY-08) WHEN testes do filtro/auth forem executados THEN SHALL existir cobertura que falharia se API Key válida não populasse o `SecurityContext` com o usuário dono; e cobertura de negação para key revogada e expirada

**Independent Test**: GET com key → 200 no escopo ACL; revogar/expirar → 401; JWT POST continua 2xx/4xx de negócio (não 403 do guard de API Key).

---

### P1: API Key read-only bloqueia mutações ⭐ MVP

**User Story**: Como operador de segurança, quero que qualquer autenticação via API Key rejeite escritas, mesmo se o dono for `ADMIN`, para limitar dano de agentes/prompt injection.

**Why P1**: Gray C supersedida; requisito explícito do produto.

**Acceptance Criteria**:

1. (APIKEY-17) WHEN a request estiver autenticada **via API Key** e o método HTTP for `POST`, `PUT`, `PATCH` ou `DELETE` THEN o sistema SHALL responder `403` **antes** de executar a ação de negócio (incluindo endpoints `hasRole("ADMIN")` e `/auth/api-keys` mutáveis)
2. (APIKEY-17b) WHEN a mesma request mutável usar **JWT** de sessão do mesmo usuário THEN o sistema SHALL **não** aplicar o bloqueio de API Key (comportamento atual de mutação preservado)
3. (APIKEY-17c) WHEN testes do write-guard forem executados THEN SHALL existir cobertura que falharia se um usuário `ADMIN` autenticado só com API Key conseguisse um `POST` mutável com status ≠ 403

**Independent Test**: Bearer API Key + `POST /folha-pagamento/processar` (ou outro POST autenticado) → 403; mesmo user com JWT → não é 403 do guard.

---

### P1: Listar e revogar próprias keys ⭐ MVP

**User Story**: Como dono da key, quero listar metadados das minhas keys e revogar uma key comprometida **pela UI/API com JWT**, sem invalidar a senha/login.

**Why P1**: Operação segura mínima.

**Acceptance Criteria**:

1. (APIKEY-09) WHEN o usuário com `API_KEY` chamar `GET /auth/api-keys` (**JWT**) THEN o sistema SHALL listar apenas as keys dele com metadados (`id`, `nome`, `prefixo`, datas, `revogado`, indicação read-only) e SHALL **não** incluir o secret nem o hash
2. (APIKEY-10) WHEN o usuário chamar revoke/delete para uma key própria (**JWT**) THEN o sistema SHALL marcar `revogado=true` e auth subsequente com essa key SHALL falhar (APIKEY-06); revoke já revogada SHALL ser idempotente
3. (APIKEY-11) WHEN o usuário **sem** `ADMIN` tentar revogar/listar key de outro usuário THEN o sistema SHALL responder `404`
4. (APIKEY-10b) WHEN revoke/create for tentado com Bearer de **API Key** THEN o sistema SHALL responder `403` (APIKEY-17)

**Independent Test**: Criar/revogar com JWT; tentar DELETE `/auth/api-keys/{id}` com a própria API Key → 403.

---

### P1: ADMIN revoga keys de outros ⭐ MVP

**User Story**: Como `ADMIN`, quero revogar a API Key de qualquer usuário **via JWT**, para cortar acesso comprometido sem resetar senha.

**Why P1**: Confirmado na gray B.

**Acceptance Criteria**:

1. (APIKEY-15) WHEN `ADMIN` listar keys filtrando por `usuarioId` (JWT) THEN o sistema SHALL retornar metadados das keys desse usuário (sem secrets)
2. (APIKEY-15b) WHEN `ADMIN` revogar uma key de outro usuário (JWT) THEN o sistema SHALL marcar `revogado=true` e auth com essa key SHALL falhar

**Independent Test**: Admin JWT revoga key B → Bearer da key B → 401.

---

### P1: Permissão `API_KEY` concedível (API + UI Usuários) ⭐ MVP

**User Story**: Como administrador, quero conceder/remover `API_KEY` na tela de Usuários, para liberar ou cortar o direito de gerenciar/usar API Keys.

**Why P1**: Direito de acesso por usuário.

**Acceptance Criteria**:

1. (APIKEY-12) WHEN a string `API_KEY` for incluída em `permissoes` de um usuário THEN o sistema SHALL persistir e o usuário passará a poder criar (JWT) / autenticar (leitura) com API Key
2. (APIKEY-13) WHEN `API_KEY` for removida THEN autenticações com keys existentes desse usuário SHALL falhar e create SHALL retornar `403`
3. (APIKEY-14) WHEN o formulário de Usuários listar permissões disponíveis THEN SHALL incluir `API_KEY`

**Independent Test**: Toggle na UI/API; create/auth refletem o gate.

---

### P1: UI de API Keys ⭐ MVP

**User Story**: Como usuário com `API_KEY`, quero uma tela para criar, ver, copiar (só no create) e revogar minhas keys **somente leitura**; como `ADMIN`, quero revogar keys de outros.

**Why P1**: Gray D opção 3.

**Acceptance Criteria**:

1. (APIKEY-16) WHEN usuário com `API_KEY` abrir a UI de API Keys THEN SHALL poder criar key (nome + validade 1..365), ver o secret **somente** após o create (copiar), ver indicação de **somente leitura**, listar metadados sem secret, e revogar as próprias
2. (APIKEY-16b) WHEN `ADMIN` na UI selecionar outro usuário THEN SHALL poder listar metadados e revogar keys desse usuário
3. (APIKEY-16c) WHEN usuário sem `API_KEY` e sem `ADMIN` tentar acessar a UI THEN SHALL ser bloqueado sem expor secrets

**Independent Test**: Fluxo UI create → badge read-only → copiar → listar → revoke.

---

### P3: Rate limit / escopos além de READ — Deferred

**User Story**: Como operador de segurança, quero throttle e escopos por domínio.

**Why P3**: Fora do MVP.

**Acceptance Criteria**:

1. (APIKEY-18) Rate limit — Deferred / N/A MVP
2. (APIKEY-19) Escopo FULL ou por domínio — Deferred (READ-ONLY fixo no MVP)

---

## Edge Cases

- WHEN Bearer vazio ou malformado THEN sistema SHALL seguir filtro atual (não autentica; não 500)
- WHEN prefixo bate mas hash não THEN SHALL não autenticar
- WHEN usuário dono for soft-desativado (`ativo=false`) THEN keys dele SHALL deixar de autenticar
- WHEN create for chamado duas vezes com mesmo `nome` THEN sistema SHALL permitir (nomes não únicos)
- WHEN key expirada THEN SHALL equivaler a inválida (401)
- WHEN `diasValidade` = 0, negativo ou > 365 THEN SHALL `400`
- WHEN revoke de key já revogada (JWT) THEN SHALL sucesso idempotente
- WHEN API Key + `POST`/`PUT`/`PATCH`/`DELETE` THEN SHALL `403` (mesmo user `ADMIN` / `ACESSO_TOTAL`)
- WHEN API Key + `GET` em recurso sem ACL THEN SHALL seguir regras ACL existentes (lista vazia / deny), não bypass

---

## Implicit-Requirement Dimensions Sweep (Large)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | `nome` `@NotBlank` + `@Size(max=100)`; `diasValidade` 1..365; secret server-side |
| Failure / partial-failure | Create: sem secret órfão; revoke idempotente |
| Idempotency / retry / duplicate | Create não idempotente; revoke idempotente |
| Auth boundaries & rate limits | Gate `API_KEY` + ACL leitura + **write-guard** API Key; rate limit N/A |
| Concurrency / ordering | Revoke vs request: eventual após commit |
| Data lifecycle / expiry | Expiry ≤365d; soft revoke; sem un-revoke |
| Observability | Log `usuarioId` + `prefixo` + falha write-guard; nunca secret |
| External-dependency failure | N/A |
| State-transition integrity | `ativa → revogada` apenas |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| APIKEY-01 | P1: Criar | Execute | Verified (service); **HTTP 201 → fix1 FIX1-04** |
| APIKEY-02 | P1: Criar (403) | Execute | Verified (fix1) |
| APIKEY-03 | P1: Criar (validação) | Execute | Verified (fix1) |
| APIKEY-04 | P1: Criar (formato) | Execute | Verified |
| APIKEY-05 | P1: Auth leitura | Execute | Verified (fix1) |
| APIKEY-06 | P1: Auth negação | Execute | Verified |
| APIKEY-07 | P1: JWT regressão | Execute | Verified (fix-cycle-1 WebMvc mocks) |
| APIKEY-08 | P1: Auth testes | Execute | Verified |
| APIKEY-09 | P1: Listar | Execute | Verified (fix1) |
| APIKEY-10 | P1: Revogar própria | Execute | Verified |
| APIKEY-10b | P1: Revogar via API Key → 403 | Execute | Verified |
| APIKEY-11 | P1: Isolamento dono | Execute | Verified |
| APIKEY-12 | P1: Permissão conceder | Execute | Verified |
| APIKEY-13 | P1: Permissão remover | Execute | Verified |
| APIKEY-14 | P1: Chip Usuários | Execute | Verified (fix1) |
| APIKEY-15 | P1: ADMIN list | Execute | Verified |
| APIKEY-15b | P1: ADMIN revoke | Execute | Verified |
| APIKEY-16 | P1: UI keys | Execute | Verified (build) |
| APIKEY-16b | P1: UI admin | Execute | Verified (build) |
| APIKEY-16c | P1: UI gate | Execute | Verified (fix1) |
| APIKEY-17 | P1: Write-guard | Execute | Verified; **PUT/PATCH tests → fix1 FIX1-12** |
| APIKEY-17b | P1: JWT não bloqueado | Execute | Verified |
| APIKEY-17c | P1: Write-guard testes | Execute | Verified |
| APIKEY-18 | P3: Rate limit | - | Deferred |
| APIKEY-19 | P3: Escopo FULL | - | Deferred |

**Fix1 cross-ref:** `_docs/specs/features/auth-api-keys-fix1/spec.md` — FIX1-07/08 (observabilidade + `ultimo_uso_em`) fecham lacunas implícitas/schema.

**Coverage:** 25 total; 23 P1 Verified no MVP; 8 refinados por fix1; 2 Deferred.

---

## Success Criteria

- [x] Usuário com `API_KEY` cria key na UI (≤365d), copia secret uma vez e faz **GET** autenticado só com Bearer da key
- [x] Mesma key em **POST/PUT/PATCH/DELETE** recebe **403** (mesmo se dono for ADMIN)
- [x] JWT da sessão continua permitindo mutações
- [x] Revogar (JWT) impede novo acesso sem afetar senha
- [x] Remover permissão `API_KEY` impede create e autenticação via key
- [x] Secret nunca reaparece em listagem nem em logs
- [x] Servidor MCP **não** é entregue nesta feature
- [ ] Evidência automatizada HTTP/ACL/UI/observabilidade — **fix1** (`auth-api-keys-fix1`)
