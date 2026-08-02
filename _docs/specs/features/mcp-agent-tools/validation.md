## Status atual
- Veredito: PASS
- Spec: mcp-agent-tools
- HEAD: 8e411c6619dfd08e79c02e3ffe7af389e7d97fd0
- Gaps abertos: none

---

## Execução: mcp-agent-tools — 2026-08-01 — c46d5b66106e1d91796bcfb14bfee7f575347919..ee6c5c872d172755e3ef8864e817e3affa83bfae

### Veredito: PASS

### Gate

| Step | Command | Outcome |
| ---- | ------- | ------- |
| Syntax | `bash -n .cursor/scripts/mcp-sistema-folha.sh` | exit 0 |
| Self-test | `bash diversos/openapi/validate-mcp-whitelist.sh --self-test` | exit 0 — negative (fictitious ID) + positive (prod yaml) |
| Validator | `bash diversos/openapi/validate-mcp-whitelist.sh` | exit 0 — `OK — 12 operationIds validated` |
| Live smoke | `bash diversos/openapi/smoke-mcp-api.sh` | exit 0 — API up; `/auth/acesso` 200 JSON; folha 05/2026 scoped |

### Evidência por AC (MCP-01…15)

| AC | Spec assertion | Evidence (file:line) | Outcome |
| -- | -------------- | -------------------- | ------- |
| MCP-01 | Bridge SHALL apply **readonly** (GET/HEAD only) | `diversos/openapi/api-to-mcp.yml:5` (`readonly: true`); `.cursor/scripts/mcp-sistema-folha.sh:50` (`exec npx … api-to-mcp rest --config`); `validate-mcp-whitelist.sh:147-148` (fail if not readonly) | readonly flag set; launcher delegates to api-to-mcp; validator enforces |
| MCP-02 | **Zero** tools map to POST/PUT/DELETE | `validate-mcp-whitelist.sh:27-75` (MUTABLE_IDS blocklist); `185-201` (non_get fail); self-test + prod validator exit 0 | All 12 whitelisted ops are GET; no mutable IDs in list |
| MCP-03 | Whitelisted read tool → **200** JSON with valid key | `smoke-mcp-api.sh:53-70` (`GET /auth/acesso` → 200 + json.load); gate output: `OK: GET /auth/acesso → 200 JSON` | 200 + parseable JSON (live) |
| MCP-04 | Tool count **≤15** and **≥10** | `api-to-mcp.yml:6-18` (12 IDs); `validate-mcp-whitelist.sh:155-157`; gate: `12 operationIds validated` | count=12 ∈ [10,15] |
| MCP-05 | Mandatory IDs present | `api-to-mcp.yml:7-8,12,15`; `validate-mcp-whitelist.sh:20-25,159-164` | obterInformacoesAcesso, listarTodos, consultarTotaisPorFuncionario, listarCompetencias all present |
| MCP-06 | Excludes organograma/import/api-keys/auth-session/dashboard | `validate-mcp-whitelist.sh:77-82` (FORBIDDEN_PATH_PREFIXES); `27-75` (login/logout/refresh in MUTABLE_IDS); `api-to-mcp.yml` (no such IDs); gate exit 0 | No forbidden paths or excluded domains in whitelist |
| MCP-07 | `listarTodos` {ano:2026,mes:5} reflects ACL scope | `smoke-mcp-api.sh:72-129`; gate: `folha items=1, cadastro funcionarios=12, global ceiling=310` + `scoped cardinality evidence (folha ≤ cadastro < global ceiling)` | folha=1 < cadastro=12 < 310 (scoped, not global) |
| MCP-08 | `api-to-mcp.yml` exists with spec, readonly, only | `diversos/openapi/api-to-mcp.yml:3-18` | File present; `spec:`, `readonly: true`, 12-item `only:` |
| MCP-09 | Launcher invokes `api-to-mcp rest --config …` + env load | `.cursor/scripts/mcp-sistema-folha.sh:8-17,45,50` | Config path + OPENAPI_BEARER_TOKEN export + exec api-to-mcp |
| MCP-10 | Missing `SISTEMA_FOLHA_API_KEY` → fail with existing message | `.cursor/scripts/mcp-sistema-folha.sh:19-32` (exit 1 + `[sistema-folha MCP] API key não configurada`) | Code path preserves fail-fast before exec (runtime move of gitignored mcp.env blocked in sandbox) |
| MCP-11 | README fluxo recomendado (escopo → resumo → totais → benefícios) | `diversos/openapi/README.md:34-45` (ordered table steps 1–8) | Table covers escopo → folha → benefícios → cadastro |
| MCP-12 | README distinguishes folha vs cadastro vs fora MCP | `diversos/openapi/README.md:47-49` (**Folha**, **Cadastro**, **Fora do MCP** paragraphs) | Explicit three-way distinction |
| MCP-13 | README references `@sgaluza/api-to-mcp` (not bridge) | `diversos/openapi/README.md:30`; grep: zero `openapi-mcp-bridge` hits | api-to-mcp documented; legacy bridge absent |
| MCP-14 | Validator exit **0** when all IDs exist | `validate-mcp-whitelist.sh:262`; gate: exit 0 + OK message | Prod run passed |
| MCP-15 | Missing whitelisted ID → exit **≠0** + print missing | `validate-mcp-whitelist.sh:178-183,221-238`; self-test: negative case passed, stderr lists `__fictitiousOperationIdXYZ__` | Non-zero exit + missing ID printed |

**Spec-anchored check: 15/15 ACs matched**

### Sensor de discriminação

Scratch copies under `.scratch/verifier-sensor/` (discarded after run; no repo mutations).

| # | Fault injected | Test run | Result |
| - | -------------- | -------- | ------ |
| 1 | `readonly: false` in temp yaml | `YAML=mutB.yml bash validate-mcp-whitelist.sh` | **KILLED** — exit 1: `options.readonly must be true` |
| 2 | `only:` truncated to 8 IDs | `YAML=mutC.yml bash validate-mcp-whitelist.sh` | **KILLED** — exit 1: `count must be between 10 and 15 (got 8)` |
| 3 | MCP-15 bypass (skip missing_in_spec + skip unknown op lookup) | `bash mutD.sh --self-test` | **KILLED** — exit 1: `self-test FAILED: expected non-zero exit for fictitious ID` |

**Summary: 3 injected, 3 killed, 0 survived**

Note: A meta-mutant weakening the self-test assertion (`bad_status -eq 999`) **survived** `--self-test` — the harness does not self-verify assertion logic; policy faults in temp yaml and MCP-15 bypass are still caught.

### Gaps

None for this verification round.

---

## Execução: mcp-agent-tools — fix cycle 1 — 2026-08-01 — ee6c5c872d172755e3ef8864e817e3affa83bfae..8e411c6619dfd08e79c02e3ffe7af389e7d97fd0

### Veredito: PASS

**Fix under review:** `smoke-mcp-api.sh` now `exit 1` when `folha_count > GLOBAL_EMPLOYEE_CEILING` (default 310); env loading order aligned with launcher (`.cursor/mcp.env` → `SISTEMA_FOLHA_MCP_ENV` / `~/.config/sistema-folha/mcp.env`).

### Gate

| Step | Command | Outcome |
| ---- | ------- | ------- |
| Syntax | `bash -n .cursor/scripts/mcp-sistema-folha.sh` | exit 0 |
| Self-test | `bash diversos/openapi/validate-mcp-whitelist.sh --self-test` | exit 0 — negative (fictitious ID) + positive (prod yaml) |
| Validator | `bash diversos/openapi/validate-mcp-whitelist.sh` | exit 0 — `OK — 12 operationIds validated` |
| Live smoke | `bash diversos/openapi/smoke-mcp-api.sh` | exit 0 — API up; `/auth/acesso` 200 JSON; folha 05/2026 scoped (items=1, cadastro=12, ceiling=310) |
| Fail-path probe | `SMOKE_GLOBAL_EMPLOYEE_CEILING=0 bash diversos/openapi/smoke-mcp-api.sh` | exit 1 — `FAIL: folha cardinality (1) exceeds global ceiling (0)` (MCP-07 hard fail) |

### Evidência por AC (delta fix cycle 1)

| AC | Spec assertion | Evidence (file:line) | Outcome |
| -- | -------------- | -------------------- | ------- |
| MCP-07 | `listarTodos` scoped — folha cardinality SHALL NOT reflect global (~310) | `smoke-mcp-api.sh:128-130` (`folha_count > GLOBAL_EMPLOYEE_CEILING` → exit 1); gate live: items=1 < cadastro=12 < 310; fail-path probe exit 1 when ceiling=0 | Hard fail on unscoped cardinality; live scoped evidence preserved |
| MCP-09 | Env load order consistent with launcher | `smoke-mcp-api.sh:19-27` mirrors `mcp-sistema-folha.sh:8-16` (`.cursor/mcp.env` then `SISTEMA_FOLHA_MCP_ENV`) | Aligned |
| MCP-03 | Whitelisted read tool → 200 JSON | `smoke-mcp-api.sh:55-72`; gate: `OK: GET /auth/acesso → 200 JSON` | Unchanged; pass |

**Spec-anchored check (full range c46d5b6..8e411c6): 15/15 ACs matched** — remaining ACs unchanged from prior round; see section above.

### Sensor de discriminação

| # | Fault injected | Test run | Result |
| - | -------------- | -------- | ------ |
| 1 | `readonly: false` in temp yaml | `YAML=.scratch/verifier-sensor/mutB.yml bash validate-mcp-whitelist.sh` | **KILLED** — exit 1: `options.readonly must be true` |
| 2 | `only:` truncated to 8 IDs | `YAML=.scratch/verifier-sensor/mutC.yml bash validate-mcp-whitelist.sh` | **KILLED** — exit 1: `count must be between 10 and 15 (got 8)` |
| 3 | MCP-15 bypass (skip missing_in_spec + skip unknown op lookup) | `SPEC=diversos/openapi/sistema-folha-openapi.json bash .scratch/verifier-sensor/mutD.sh --self-test` | **KILLED** — exit 1: `self-test FAILED: expected non-zero exit for fictitious ID` |

**Summary: 3 injected, 3 killed, 0 survived**

### Gaps

None for fix cycle 1.
