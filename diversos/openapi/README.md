# OpenAPI — Sistema Folha

Spec gerada a partir do backend em execução (`springdoc`):

```bash
curl -s http://localhost:8083/api/api-docs -o diversos/openapi/sistema-folha-openapi.json
python3 -m json.tool diversos/openapi/sistema-folha-openapi.json > /dev/null
bash diversos/openapi/validate-mcp-whitelist.sh
```

Após regenerar a spec, rode `validate-mcp-whitelist.sh` para garantir que os `operationId` da whitelist MCP ainda existem.

## MCP (Cursor / Claude)

1. Crie a API key na UI (`/api-keys`) com permissão `API_KEY`.
2. Salve o secret em `.cursor/mcp.env` (gitignored):

   ```bash
   cp .cursor/mcp.env.example .cursor/mcp.env
   # edite .cursor/mcp.env e cole sf_live_...
   chmod 600 .cursor/mcp.env
   ```

   Alternativa: `~/.config/sistema-folha/mcp.env` com a mesma variável.

3. **Cursor:** Settings → **MCP** → habilite `sistema-folha` → **Refresh**.

   Config do projeto: `.cursor/mcp.json` (caminho relativo ao repo).

4. **Claude Desktop:** edite `~/Library/Application Support/Claude/claude_desktop_config.json` e use o mesmo launcher com **caminho absoluto** (Claude não aceita paths relativos). Modelo: `diversos/openapi/claude-desktop-mcp.example.json`.

   A API key fica em `.cursor/mcp.env` (ou `~/.config/sistema-folha/mcp.env`) — **não** coloque o token no JSON do Claude.

   Reinicie o Claude Desktop após salvar o config.

   Após alterar `api-to-mcp.yml` ou regenerar a OpenAPI, faça **Refresh** (Cursor) ou reinicie (Claude) para recarregar a lista de tools.

Bridge OpenAPI → MCP via `@sgaluza/api-to-mcp` + whitelist versionada.

Whitelist versionada: `diversos/openapi/api-to-mcp.yml` (`readonly: true` + 12 `operationId`).

### Fluxo recomendado para agentes

| Ordem | Pergunta do usuário | Tool (`operationId`) | Domínio |
| ----- | ------------------- | -------------------- | ------- |
| 1 | Qual meu escopo (CC, organograma)? | `obterInformacoesAcesso` | Escopo |
| 2 | Totais da folha MM/AAAA? | `listarTodos` ou `consultarPorCompetencia` | **Folha** |
| 3 | Resumos em intervalo / últimas competências? | `consultarPorPeriodo`, `listarMaisRecentes` | **Folha** |
| 4 | Breakdown por funcionário? | `consultarTotaisPorFuncionario` | **Folha** |
| 5 | Lançamentos detalhados / ficha individual? | `consultarPorPeriodo_1`, `buscarFichaPorFuncionario` | **Folha** |
| 6 | Benefícios por competência / resumo por tipo? | `listarCompetencias`, `resumoPorCompetencia` | **Benefícios** |
| 7 | Quantos no cadastro (scoped)? | `listar_2` | **Cadastro** |
| 8 | Usuários visíveis ao operador? | `listar` | **Cadastro** |

**Folha** = resumo e lançamentos de folha de pagamento (paths `/resumo-folha-pagamento`, `/folha-pagamento`).  
**Cadastro** = `listar_2` (`GET /funcionarios`) e `listar` (`GET /usuarios`) — ambos **respeitam ACL** desde `auth-api-keys-fix2`; use para contagem de cadastro, não para totais de folha.  
**Fora do MCP** = organograma CRUD, importação, API Keys, login/logout/refresh, dashboard e demais mutações — não aparecem na whitelist (`api-to-mcp.yml`).

### Tools expostas (whitelist)

| Tool (`operationId`) | HTTP | Path | Uso |
| --- | --- | --- | --- |
| `obterInformacoesAcesso` | GET | `/auth/acesso` | Escopo do usuário (CC, organograma) |
| `listarTodos` | GET | `/resumo-folha-pagamento` | Totais consolidados por ano/mês |
| `consultarPorCompetencia` | GET | `/resumo-folha-pagamento/competencia` | Resumo por competência |
| `consultarPorPeriodo` | GET | `/resumo-folha-pagamento/periodo` | Resumos em intervalo |
| `listarMaisRecentes` | GET | `/resumo-folha-pagamento/latest` | Últimas competências |
| `consultarTotaisPorFuncionario` | GET | `/folha-pagamento/totais-funcionarios` | Breakdown por funcionário |
| `consultarPorPeriodo_1` | GET | `/folha-pagamento` | Lançamentos detalhados |
| `buscarFichaPorFuncionario` | GET | `/folha-pagamento/fichas/por-funcionario` | Ficha individual |
| `listarCompetencias` | GET | `/beneficio-mensal/competencias` | Benefícios por competência |
| `resumoPorCompetencia` | GET | `/beneficio-mensal/resumo` | Resumo benefícios por tipo |
| `listar_2` | GET | `/funcionarios` | Cadastro scoped (contagem ACL) |
| `listar` | GET | `/usuarios` | Usuários scoped |
