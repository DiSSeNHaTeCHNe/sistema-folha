# Auth — API Keys Context

**Gathered:** 2026-07-29  
**Spec:** `_docs/specs/features/auth-api-keys/spec.md`  
**Status:** Locked — Design Approach A; **C supersedida → READ-ONLY** (2026-07-29)

---

## Feature Boundary

Entregar API Keys por usuário (PAT) com permissão `API_KEY`, autenticação Bearer convivendo com JWT, ACL de **leitura** herdada do dono, **escopo READ-ONLY obrigatório** (mutações HTTP → 403), expiração ≤ 365 dias, UI `/api-keys`, revogação cross-user por `ADMIN` via JWT. **Não** entregar servidor MCP nesta feature.

---

## Implementation Decisions

### A — Gate de permissão

- Nome canônico: **`API_KEY`** (`ROLE_API_KEY`)
- Quem tem `API_KEY` cria/lista/revoga as próprias keys **via JWT**
- Autenticação via API Key exige que o dono ainda possua `API_KEY`
- Concessão via `usuario_permissoes` + chip Usuários

### B — Quem gerencia

- Usuário com `API_KEY`: próprias keys (JWT)
- **`ADMIN` pode revogar** keys de qualquer usuário (JWT)
- Gestão de keys via API Key (POST/DELETE) → **403** (read-only)

### C — Escopos (**supersedida**)

- **Antes:** key herda authorities completas do usuário (escrita inclusa)  
- **Agora (2026-07-29):** **todas as API Keys são READ-ONLY**
  - Autenticação via key marca o `SecurityContext` como sessão API Key
  - Métodos `POST`/`PUT`/`PATCH`/`DELETE` → `403`
  - Métodos `GET`/`HEAD`/`OPTIONS` → ACL/authorities de leitura do dono
  - JWT de sessão **não** é afetado (SPA com mutações intactas)
- Sem opção FULL / escopo configurável no MVP (Deferred APIKEY-19)

### D — Surface UI (P1)

- Página `/api-keys`: create/list/copy-once/revoke + indicação **somente leitura**
- Chip `API_KEY` em Usuários
- Admin revoga keys alheias na UI

### E — Header / formato

- `Authorization: Bearer <api-key>` com prefixo `sf_live_`
- Sem `X-API-Key` no MVP

### F — Expiração

- `data_expiracao` obrigatória; TTL 1..365; default 365

### Agent's Discretion

- Marcador de auth API Key: authority extra `ROLE_API_KEY_READONLY` **ou** `Authentication` details / credential type — desde que o write-guard detecte de forma confiável
- Filtro `ApiKeyWriteGuardFilter` após o Bearer dual-path
- Coluna `escopo` no DB opcional (`READ` fixo) vs constante em código — preferir coluna `escopo VARCHAR(16) NOT NULL DEFAULT 'READ'` para forward-compat
- Pacote `auth.*` + `security/` (AD-008)
- Hash BCrypt; `404` key alheia; Flyway próximo número

### Declined / Undiscussed Gray Areas → Assumptions

- Unicidade de `nome`: não exigir
- Un-revoke: não
- Rate limit: fora
- Escopo FULL: deferred (não no MVP)

---

## Specific References

- Produto: MCP futuro + mitigação de prompt injection via key só-leitura
- Analogia Linear: token no config do agente
- Liberar por usuário = `API_KEY`

---

## Deferred Ideas

- Servidor MCP (tools, host Cursor) — com tools alinhadas a GET
- OAuth / `mcp_auth`
- Escopo FULL / por domínio
- Rate limiting / quotas
- Rotação automática
- Service account sem `Usuario`
