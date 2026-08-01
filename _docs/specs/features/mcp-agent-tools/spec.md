# mcp-agent-tools — MCP scoped para agentes Specification

**Related:** AD-013 (API Key PAT read-only); `auth-api-keys` / `auth-api-keys-fix2` (ACL + smoke MCP); `.cursor/mcp.json`; `diversos/openapi/sistema-folha-openapi.json`  
**Complexity:** Medium  
**Spec status:** Approved — defaults confirmados no Design (2026-08-01)

## Problem Statement

O servidor MCP do projeto (`@sgaluza/openapi-mcp-bridge` sobre a OpenAPI completa) expõe **90 tools** — CRUD de organograma, cadastros, importação, mutações de folha e consultas. Agentes no Cursor precisam escolher entre dezenas de tools com nomes genéricos (`listar_2`, `buscarPorId_4`), aumentando latência, confusão de roteamento e risco de chamar endpoint errado (cadastro vs folha). Embora API Keys sejam read-only no backend, o MCP ainda **lista** tools de escrita, poluindo o contexto do agente. Operadores precisam de um MCP **enxuto e previsível** para consultas de folha/benefícios com ACL, sem reimplementar a API.

## Goals

- [ ] Reduzir tools MCP expostas no Cursor para um **conjunto curado** orientado a agentes (consulta folha/benefícios/escopo)
- [ ] Garantir que **nenhuma tool mutável** (POST/PUT/DELETE) apareça no MCP, mesmo que exista na spec OpenAPI
- [ ] Manter autenticação atual via `SISTEMA_FOLHA_API_KEY` / `.cursor/mcp.env` sem mudança de UX para o operador
- [ ] Documentar roteamento recomendado (qual tool usar para qual pergunta)

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Grupo springdoc `/api-docs/mcp` no backend | Filtro na origem é follow-up; MVP = filtro no bridge |
| Segundo servidor MCP “full API” para devs | Um servidor `sistema-folha` no projeto; devs usam HTTP/curl ou spec completa localmente |
| Renomear `operationId` no Spring (eliminar `_N`) | Melhoria de DX separada; não bloqueia filtro |
| Rate limit / escopos granulares de API Key | Deferred `auth-api-keys` APIKEY-18/19 |
| OAuth / `mcp_auth` Cursor | MVP permanece PAT estático |
| Alterações de ACL ou endpoints REST | Coberto por `auth-api-keys-fix2`; esta feature só configura MCP |
| Testes E2E Playwright do Cursor | Verificação = smoke MCP + contagem de tools |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Bridge npm | Migrar de `@sgaluza/openapi-mcp-bridge` para `@sgaluza/api-to-mcp` | Sucessor com `--readonly`, `--only`, config YAML; aliases legados `OPENAPI_BEARER_TOKEN` | y |
| Estratégia de filtro | **`--readonly` + whitelist `--only`** (não só readonly) | Readonly ainda expõe ~43 GETs; whitelist reduz para ~12–15 tools relevantes | y |
| Spec de entrada | Manter `diversos/openapi/sistema-folha-openapi.json` (completa); filtro só no bridge | Evita duplicar spec; regen continua `curl api-docs` | y |
| Config versionada | `diversos/openapi/api-to-mcp.yml` com `options.readonly: true` e lista `only:` | Legível, revisável em PR; script aponta `--config` | y |
| Tools na whitelist MVP | Ver tabela **Curated tool set** abaixo (12 tools) | Derivado do smoke Humberto 2026-07-30 + README existente | y |
| Incluir cadastro scoped | `listar_2` (funcionários) e `listar` (usuários) **incluídos** na whitelist | fix2 aplicou ACL; útil para contagem de cadastro vs folha | y |
| Incluir auth login/logout/refresh | **Excluídos** — MCP usa API Key Bearer, não JWT session | Reduz ruído; `obterInformacoesAcesso` basta para escopo | y |
| Incluir dashboard / importação / organograma CRUD | **Excluídos** da whitelist | Fora do fluxo “dados da folha”; readonly já bloqueia mutações | y |
| Falha se operationId sumir da spec | Script de smoke **falha** se qualquer ID da whitelist não existir na spec após regen OpenAPI | Evita MCP silenciosamente incompleto | y |
| Overrides de descrição | P3 — não no MVP | `api-to-mcp` suporta `overrides:`; adiar | y |

### Curated tool set (default whitelist)

| operationId | HTTP | Path | Agente usa para |
| ----------- | ---- | ---- | --------------- |
| `obterInformacoesAcesso` | GET | `/auth/acesso` | Escopo, CC, organograma |
| `listarTodos` | GET | `/resumo-folha-pagamento` | Totais consolidados por ano/mês |
| `consultarPorCompetencia` | GET | `/resumo-folha-pagamento/competencia` | Resumo por competência |
| `consultarPorPeriodo` | GET | `/resumo-folha-pagamento/periodo` | Resumos em intervalo |
| `listarMaisRecentes` | GET | `/resumo-folha-pagamento/latest` | Últimas competências |
| `consultarTotaisPorFuncionario` | GET | `/folha-pagamento/totais-funcionarios` | Breakdown por funcionário |
| `consultarPorPeriodo_1` | GET | `/folha-pagamento` | Lançamentos detalhados |
| `buscarFichaPorFuncionario` | GET | `/folha-pagamento/fichas/por-funcionario` | Ficha individual |
| `listarCompetencias` | GET | `/beneficio-mensal/competencias` | Benefícios por competência |
| `resumoPorCompetencia` | GET | `/beneficio-mensal/resumo` | Resumo benefícios por tipo |
| `listar_2` | GET | `/funcionarios` | Cadastro scoped (contagem) |
| `listar` | GET | `/usuarios` | Usuários scoped |

**Open questions:** none — OQ-1…OQ-5 resolvidos com defaults confirmados no Design (`design.md`).

**Remaining dimensions N/A for this scope:** persistence, payments, concurrency, data expiry, rate limits (backend unchanged).

---

## User Stories

### P1: MCP read-only sem tools mutáveis ⭐ MVP

**User Story**: Como operador que configura agentes no Cursor, quero que o servidor MCP exponha **somente** operações de leitura, para reduzir risco de prompt injection tentando POST/DELETE mesmo com API Key read-only.

**Why P1**: Defesa em profundidade; 47 tools mutáveis hoje poluem o catálogo.

**Acceptance Criteria**:

1. (MCP-01) WHEN operador iniciar o servidor MCP via `.cursor/scripts/mcp-sistema-folha.sh` THEN bridge SHALL aplicar modo **readonly** (somente GET/HEAD)
2. (MCP-02) WHEN Cursor listar tools do servidor `sistema-folha` THEN **zero** tools SHALL mapear para POST, PUT ou DELETE da OpenAPI
3. (MCP-03) WHEN operador invocar tool read-only whitelisted (ex.: `obterInformacoesAcesso`) com API Key válida THEN resposta SHALL ser **200** com payload JSON (paridade smoke HTTP 2026-07-30)

**Independent Test**: Refresh MCP no Cursor → inspecionar lista de tools → confirmar ausência de `processar`, `cadastrar`, `remover`, `importar*`.

---

### P1: Whitelist curada para consulta de folha ⭐ MVP

**User Story**: Como agente no Cursor respondendo “dados da folha MM/AAAA”, quero um conjunto **pequeno e nomeado** de tools, para acertar roteamento sem vasculhar 90 opções.

**Why P1**: Problema central da conversa; readonly sozinho ainda deixa ~43 GETs.

**Acceptance Criteria**:

1. (MCP-04) WHEN Cursor listar tools THEN contagem SHALL ser **≤ 15** e **≥ 10**
2. (MCP-05) WHEN contagem for verificada THEN tools SHALL incluir **obrigatoriamente**: `obterInformacoesAcesso`, `listarTodos`, `consultarTotaisPorFuncionario`, `listarCompetencias`
3. (MCP-06) WHEN contagem for verificada THEN tools SHALL **NOT** incluir: qualquer tool de Organograma CRUD, Importação, API Keys, Auth login/logout/refresh, Dashboard
4. (MCP-07) WHEN agente chamar `listarTodos` com `{ano: 2026, mes: 5}` via MCP THEN resposta SHALL refletir escopo ACL do usuário da key (ex.: 10 empregados Plugin, não 310 globais — paridade fix2)

**Independent Test**: Smoke MCP nativo (`CallMcpTool`) — escopo + folha 05/2026 + totais; comparar cardinalidade com teste Humberto.

---

### P2: Config versionada e script atualizado

**User Story**: Como mantenedor do repo, quero whitelist e flags MCP em arquivo versionado, para revisar mudanças em PR sem editar script bash.

**Why P2**: Operabilidade; evita lista longa em `exec npx ... --only "a,b,c,..."`.

**Acceptance Criteria**:

1. (MCP-08) WHEN repositório for clonado THEN arquivo `diversos/openapi/api-to-mcp.yml` SHALL existir documentando `spec`, `options.readonly: true` e lista `only:`
2. (MCP-09) WHEN `.cursor/scripts/mcp-sistema-folha.sh` executar THEN SHALL invocar `@sgaluza/api-to-mcp rest --config diversos/openapi/api-to-mcp.yml` (ou equivalente) mantendo carga de `.cursor/mcp.env`
3. (MCP-10) WHEN `SISTEMA_FOLHA_API_KEY` estiver ausente THEN script SHALL falhar com mensagem existente (comportamento preservado)

**Independent Test**: `./.cursor/scripts/mcp-sistema-folha.sh` com env válido → processo sobe; diff do yaml mostra whitelist.

---

### P2: Documentação de roteamento para agentes

**User Story**: Como operador ou agente, quero README claro de qual tool usar para cada tipo de pergunta, para não confundir cadastro com folha.

**Why P2**: Nomes `listar` / `listar_2` / `listarTodos` continuam ambíguos sem guia.

**Acceptance Criteria**:

1. (MCP-11) WHEN operador ler `diversos/openapi/README.md` seção MCP THEN SHALL encontrar tabela **fluxo recomendado** (escopo → resumo → totais → benefícios)
2. (MCP-12) WHEN README listar tools THEN SHALL distinguir explicitamente **folha** vs **cadastro** vs **fora do MCP**
3. (MCP-13) WHEN README descrever setup THEN SHALL referenciar `@sgaluza/api-to-mcp` (não `@sgaluza/openapi-mcp-bridge`)

**Independent Test**: Revisão documental — links e operationIds batem com `api-to-mcp.yml`.

---

### P3: Smoke automatizado da whitelist

**User Story**: Como CI/maintainer, quero script que valide operationIds da whitelist contra a spec OpenAPI, para detectar quebra após regen da API.

**Why P3**: Previne MCP parcial após rename de controller.

**Acceptance Criteria**:

1. (MCP-14) WHEN script `diversos/openapi/validate-mcp-whitelist.sh` (ou equivalente) rodar contra spec atual THEN exit code SHALL ser **0** se todos os IDs existirem
2. (MCP-15) WHEN spec não contiver um operationId whitelisted THEN script SHALL exit **≠ 0** e imprimir IDs faltantes

**Independent Test**: Remover ID fictício do yaml → script falha; restaurar → passa.

---

## Edge Cases

- WHEN OpenAPI for regenerada e renomear `operationId` THEN smoke MCP-14 SHALL falhar até whitelist ser atualizada
- WHEN API Key expirada ou revogada THEN tools MCP SHALL retornar **401/403** (comportamento API; MCP não mascara)
- WHEN operador não fizer Refresh no Cursor após mudança de whitelist THEN Cursor pode cachear lista antiga — README SHALL instruir Refresh
- WHEN `npx` não estiver no PATH do app GUI THEN script SHALL manter `PATH` com asdf shims (comportamento atual)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MCP-01 | P1: Read-only | Execute | Done |
| MCP-02 | P1: Read-only | Execute | Done |
| MCP-03 | P1: Read-only | Execute | Done |
| MCP-04 | P1: Whitelist | Execute | Done |
| MCP-05 | P1: Whitelist | Execute | Done |
| MCP-06 | P1: Whitelist | Execute | Done |
| MCP-07 | P1: Whitelist | Execute | Done |
| MCP-08 | P2: Config | Execute | Done |
| MCP-09 | P2: Config | Execute | Done |
| MCP-10 | P2: Config | Execute | Done |
| MCP-11 | P2: Docs | Execute | Done |
| MCP-12 | P2: Docs | Execute | Done |
| MCP-13 | P2: Docs | Execute | Done |
| MCP-14 | P3: Smoke script | Execute | Done |
| MCP-15 | P3: Smoke script | Execute | Done |

**Coverage:** 15 total, 15 mapped to tasks ✅

---

## Success Criteria

- [ ] Cursor MCP `sistema-folha` expõe **≤ 15 tools** (vs 90 atuais)
- [ ] Zero tools mutáveis na lista MCP
- [ ] Smoke “folha 05/2026” via MCP retorna mesmos totais scoped do teste Humberto (10 emp, líquido R$ 66.040,32)
- [ ] Whitelist e setup documentados em `diversos/openapi/README.md`
- [ ] Operador consegue reconfigurar com `cp mcp.env.example → mcp.env` + MCP Refresh (fluxo inalterado)
