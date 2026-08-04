# auth-api-keys-fix2 Context

**Gathered:** 2026-07-30  
**Spec:** `_docs/specs/features/auth-api-keys-fix2/spec.md`  
**Status:** Locked — defaults OQ-1/OQ-2 confirmados no Design

---

## Feature Boundary

Fechar gaps de **ACL cadastro**, **400 em benefício** e **UX SPA** expostos por smoke MCP com API Key read-only. Não alterar gate `API_KEY` para create no backend.

---

## Implementation Decisions

### FIX2-CTX-01 — Usuários sem funcionário (OQ-1)

- **Decisão:** Na listagem scoped (`acessoTotal=false`), usuários **sem** `funcionario` vinculado são **excluídos**.
- **Rationale:** Evita vazar contas admin/RH; paridade com “sem CC → excluído” em funcionários.
- **GET por ID:** usuário sem funcionário → **404** para caller scoped.

### FIX2-CTX-02 — UX create API Key ADMIN sem `API_KEY` (OQ-2)

- **Decisão:** Botão **“Nova API Key” desabilitado** + `Alert`/`Tooltip` com texto: *“Conceda a permissão API_KEY ao seu usuário para criar chaves.”*
- **Rationale:** Gate backend inalterado (APIKEY-02); evita POST 403; ADMIN ainda pode **listar/revogar** keys alheias.
- **Revogar:** permanece habilitado para ADMIN (JWT).

### FIX2-CTX-03 — Abordagem ACL cadastro

- **Decisão:** **Approach A** — métodos `*ParaUsuario(String login, …)` nos services existentes; controllers GET recebem `Authentication`.
- **Cross-domain:** `OrganogramaAcessoPort` + `UsuarioLookupPort` apenas (AD-008).
- **Filtro CC:** `CentroCustoEfetivo.pertenceAoEscopo` / helpers espelhando `BeneficioMensalService`.

### FIX2-CTX-04 — Benefício 400

- **Decisão:** `@ExceptionHandler(MissingServletRequestParameterException)` em `GlobalExceptionHandler` → **400** padronizado; belt-and-suspenders além do Spring default.

### FIX2-CTX-05 — Interceptor axios

- **Decisão:** Refresh/logout **somente** em **401**; **403** rejeita promise sem side effects.

---

## Out of Scope (reconfirmado)

- ACL em cargos, rubricas, centros-custo, organograma CRUD completo
- Relaxar `exigirPermissaoApiKey` para ADMIN
- Servidor MCP
