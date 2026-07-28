# Funcionários, Folha e Dashboard — UX Specification

## Problem Statement

Três telas centrais do RH têm lacunas de usabilidade e precisão de dados: (1) funcionários inativados somem da listagem, impossibilitando consulta histórica e filtro por status; (2) o gráfico de evolução da folha no dashboard inclui competências marcadas como 13º salário, distorcendo a série mensal regular; (3) a tela de Folha de Pagamento carrega todos os anos sem filtro obrigatório, degradando performance e dificultando o uso cotidiano.

## Goals

- [ ] Permitir consultar funcionários ativos, inativos ou todos, com destaque visual para inativos
- [ ] Manter ação explícita de inativação em funcionários ativos (soft-delete existente)
- [ ] Excluir folhas de 13º salário (`decimoTerceiro = true`) da série **Evolução da Folha de Pagamento** no dashboard
- [ ] Tornar o filtro de ano obrigatório na Folha de Pagamento, com ano corrente como padrão ao abrir a tela

## Out of Scope

| Feature | Reason |
| --- | --- |
| Reativar funcionário inativo | Não solicitado; exige regra de CPF único entre ativos |
| Editar cadastro de funcionário inativo | Não solicitado; inativos são consulta histórica |
| Excluir 13º de outros KPIs do dashboard (custo mensal, breakdowns) | Usuário restringiu à evolução |
| Alterar flag `decimoTerceiro` na importação | Comportamento de importação permanece |
| Filtro de mês obrigatório na Folha de Pagamento | Usuário especificou apenas ano |
| Migração de dados ou schema | Campo `ativo` e `decimo_terceiro` já existem |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Inativação = soft-delete via `DELETE /funcionarios/{id}` | Reutilizar endpoint e fluxo existentes (confirmação + toast) | Campo `ativo` e `remover()` já implementados no backend | n |
| Filtro de status default = **Ativo** | `ativo=true` na API quando filtro = Ativo | Alinhado ao pedido e ao comportamento atual da listagem | n |
| Cards inativos: somente leitura | Ocultar ações Editar e Inativar quando `ativo=false` | Inativo é registro histórico; reativação fora de escopo | n |
| Ícone inativo | MUI `PersonOff` ou `Block` com `aria-label="Inativo"` | Usuário autorizou ícone opcional; melhora acessibilidade | n |
| Estilo cinza | `bgcolor: grey.100`, texto `text.disabled`, opacidade ~0.85 | Evidencia inativo sem quebrar layout do card | n |
| Dashboard: escopo = apenas `evolucaoMensal` | Filtrar resumos com `decimoTerceiro = true` na query de evolução | Pedido literal do usuário | n |
| Folha: filtro no backend | Novo param `ano` (ou uso de `/periodo` com bounds derivados) | Evita carregar todos os resumos; alinha com ACL server-side | n |
| Folha: mês permanece opcional | Filtrar por ano; mês restringe subconjunto quando informado | Usuário não pediu mês obrigatório | n |
| Permissões | Mesmas de hoje (`/funcionarios/**` autenticado; folha com ACL) | Sem mudança de autorização solicitada | n |

**Open questions:** none — all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Inativar funcionário ativo ⭐ MVP

**User Story**: Como operador de RH, quero inativar um funcionário ativo a partir da tela de cadastro, para preservar o histórico de folha sem excluir dados.

**Why P1**: Base do ciclo de vida do cadastro; hoje a ação existe mas o funcionário desaparece da lista — este story garante o contrato de inativação explícito.

**Acceptance Criteria**:

1. WHEN o usuário visualiza um card de funcionário **ativo** THEN o sistema SHALL exibir ação **Inativar** (ícone ou botão com tooltip/label acessível)
2. WHEN o usuário confirma a inativação THEN o sistema SHALL chamar `DELETE /api/funcionarios/{id}` e definir `ativo=false` no registro (soft-delete)
3. WHEN a inativação conclui com sucesso THEN o sistema SHALL exibir feedback de sucesso e atualizar a listagem conforme o filtro de status vigente
4. WHEN o funcionário já está inativo THEN o sistema SHALL **não** exibir a ação Inativar no card

**Independent Test**: Inativar um funcionário ativo via UI → verificar HTTP 204/200 e registro com `ativo=false` via API ou filtro Inativos.

---

### P1: Filtro de status na listagem de funcionários ⭐ MVP

**User Story**: Como operador de RH, quero filtrar funcionários por status (Ativo, Inativo, Todos), para consultar cadastros históricos sem perder o foco nos ativos no dia a dia.

**Why P1**: Sem filtro server-side, inativos permanecem invisíveis — bloqueia os stories de card cinza e consulta histórica.

**Acceptance Criteria**:

1. WHEN o usuário abre a tela Funcionários THEN o filtro de status SHALL estar em **Ativo** por padrão
2. WHEN o filtro está em **Ativo** THEN `GET /api/funcionarios` SHALL retornar apenas registros com `ativo=true` (comportamento atual preservado)
3. WHEN o filtro está em **Inativo** THEN `GET /api/funcionarios?ativo=false` SHALL retornar apenas registros com `ativo=false`
4. WHEN o filtro está em **Todos** THEN `GET /api/funcionarios?ativo=` (omitido ou valor especial acordado no design) SHALL retornar ativos e inativos
5. WHEN o usuário combina filtro de status com filtros existentes (nome, cargo, centro de custo, linha de negócio) THEN o sistema SHALL aplicar **todos** os critérios conjuntamente
6. WHEN o usuário clica **Limpar Filtros** THEN o filtro de status SHALL voltar para **Ativo** (não para Todos)

**Independent Test**: Cadastrar 2 ativos e inativar 1 → filtro Ativo mostra 1; Inativo mostra 1; Todos mostra 2.

---

### P1: Destaque visual de funcionário inativo ⭐ MVP

**User Story**: Como operador de RH, quero identificar visualmente funcionários inativos na grade de cards, para não confundi-los com ativos.

**Why P1**: Complemento direto do filtro; requisito explícito de UX.

**Acceptance Criteria**:

1. WHEN um card representa funcionário com `ativo=false` THEN o sistema SHALL renderizar textos do card em tom cinza (`text.disabled` ou equivalente)
2. WHEN um card representa funcionário com `ativo=false` THEN o sistema SHALL aplicar fundo ou borda atenuada (ex.: `grey.100`) distinguível de cards ativos
3. WHEN um card representa funcionário com `ativo=false` THEN o sistema SHALL exibir indicador visual de inativo (ícone ou chip **Inativo**) com nome acessível para leitores de tela
4. WHEN um card representa funcionário com `ativo=true` THEN o sistema SHALL manter estilo visual atual (sem indicador de inativo)

**Independent Test**: Filtro Inativo → todos os cards exibidos atendem critérios 1–3; filtro Ativo → nenhum card com estilo inativo.

---

### P1: Excluir 13º salário da evolução da folha ⭐ MVP

**User Story**: Como gestor, quero que o gráfico **Evolução da Folha de Pagamento** reflita apenas folhas regulares, para analisar tendência mensal sem pico distorcido do 13º.

**Why P1**: Requisito explícito de precisão analítica no dashboard.

**Acceptance Criteria**:

1. WHEN o dashboard calcula `evolucaoMensal` THEN o sistema SHALL **excluir** resumos (`ResumoFolhaPagamento`) com `decimoTerceiro = true`
2. WHEN existem resumo regular e resumo 13º na mesma competência THEN `evolucaoMensal` SHALL conter **apenas** o ponto da folha regular (`decimoTerceiro = false`)
3. WHEN o usuário tem acesso total (`acessoTotal`) THEN a exclusão SHALL aplicar-se em `calcularEvolucaoMensal()`
4. WHEN o usuário tem ACL scoped por centro de custo THEN a exclusão SHALL aplicar-se em `calcularEvolucaoMensalScoped()` — competências derivadas somente de resumos não-13º
5. WHEN não há folhas regulares no período THEN `evolucaoMensal` SHALL ser lista vazia (sem fallback para dados mock no ambiente de produção/teste de integração)

**Independent Test**: Importar competência dez/2025 regular + 13º → gráfico exibe um ponto de dez/2025 com valores da folha regular apenas.

---

### P1: Ano obrigatório na Folha de Pagamento ⭐ MVP

**User Story**: Como operador de RH, quero que a tela de Folha de Pagamento exija seleção de ano (padrão ano corrente), para listar apenas competências relevantes.

**Why P1**: Performance e usabilidade; hoje a tela carrega todos os resumos sem restrição.

**Acceptance Criteria**:

1. WHEN o usuário abre a tela Folha de Pagamento THEN o campo **Ano** SHALL estar preenchido com o **ano corrente** (`LocalDate.now().getYear()` / `new Date().getFullYear()`)
2. WHEN a tela carrega THEN o sistema SHALL buscar resumos **filtrados pelo ano selecionado** via API (não filtragem client-side após `listarTodos()`)
3. WHEN o usuário tenta filtrar com ano vazio THEN o sistema SHALL impedir submit e exibir validação (ano obrigatório)
4. WHEN o usuário seleciona ano válido e clica Filtrar THEN o sistema SHALL exibir apenas resumos cuja `competenciaInicio` pertence ao ano informado
5. WHEN o usuário clica Limpar THEN o ano SHALL resetar para o **ano corrente** e recarregar dados desse ano
6. WHEN o filtro de mês estiver preenchido THEN o sistema SHALL restringir adicionalmente ao mês dentro do ano selecionado (mês continua opcional)

**Independent Test**: Com resumos em 2024 e 2026 → abrir tela mostra só 2026; alterar para 2024 → lista muda; limpar → volta a 2026.

---

### P2: API documentada para filtro de ano na folha

**User Story**: Como integrador/consumidor da API, quero contrato explícito do parâmetro `ano` no endpoint de resumos, para clientes gerados e testes automatizados.

**Why P2**: Boas práticas OpenAPI; não bloqueia MVP se FE usar `/periodo` com bounds derivados internamente.

**Acceptance Criteria**:

1. WHEN `GET /api/resumo-folha-pagamento?ano={yyyy}` é chamado THEN o OpenAPI SHALL documentar `ano` como parâmetro inteiro obrigatório para listagem filtrada (ou default documentado = ano corrente)
2. WHEN `ano` está fora do intervalo razoável (ex.: &lt; 2000 ou &gt; 2100) THEN o sistema SHALL retornar HTTP 400 com mensagem em português

**Independent Test**: Swagger UI mostra param `ano`; request com `ano=1999` retorna 400.

---

## Edge Cases

- WHEN inativar funcionário e filtro está em **Ativo** THEN o card SHALL desaparecer da listagem após refresh (comportamento esperado)
- WHEN filtro **Inativo** e nenhum inativo existe THEN o sistema SHALL exibir estado vazio orientativo
- WHEN CPF duplicado entre ativos na reativação futura THEN N/A (reativação fora de escopo)
- WHEN competência tem só folha 13º (sem regular) THEN mês SHALL **omitir** ponto na evolução (não usar valores do 13º)
- WHEN ano selecionado na folha não possui resumos THEN o sistema SHALL exibir estado vazio (não erro)
- WHEN usuário scoped não tem resumos no ano THEN lista vazia respeitando ACL (não vazar dados de outros centros)

---

## Implicit-Requirement Dimensions (Large feature sweep)

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Ano: inteiro 2000–2100; status: enum `true`/`false`/omitido (Todos) |
| Failure / partial-failure states | Inativação idempotente: segundo DELETE em inativo → 404; filtro inválido → 400 |
| Idempotency / retry / duplicate handling | Inativar já inativo → erro; reimport folha não afetada nesta feature |
| Auth boundaries & rate limits | Sem alteração; endpoints autenticados existentes |
| Concurrency / ordering | Last-write-wins em `ativo`; listagem eventualmente consistente pós-inativação |
| Data lifecycle / expiry | Inativos permanecem indefinidamente; sem purge |
| Observability | Sem requisito novo de métricas |
| External-dependency failure | N/A |
| State-transition integrity | Transição permitida: `ativo true → false` via DELETE; reversa fora de escopo |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FUNC-01 | P1: Inativar funcionário | Tasks | T3 |
| FUNC-02 | P1: Inativar funcionário | Tasks | T2, T3 |
| FUNC-03 | P1: Filtro de status | Tasks | T1, T2, T3 |
| FUNC-04 | P1: Filtro de status | Tasks | T1, T2, T3 |
| FUNC-05 | P1: Filtro de status | Tasks | T1, T2, T3 |
| FUNC-06 | P1: Destaque visual inativo | Tasks | T3 |
| FUNC-07 | P1: Destaque visual inativo | Tasks | T3 |
| DASH-01 | P1: Excluir 13º evolução | Tasks | T6 |
| DASH-02 | P1: Excluir 13º evolução (scoped) | Tasks | T6 |
| FOLH-01 | P1: Ano obrigatório | Tasks | T4, T5 |
| FOLH-02 | P1: Ano obrigatório | Tasks | T4, T5 |
| FOLH-03 | P1: Ano obrigatório | Tasks | T4, T5 |
| FOLH-04 | P2: API ano documentada | Tasks | T4 |

**Coverage:** 13 total, 13 mapped to tasks, 0 unmapped

---

## Success Criteria

- [x] Operador filtra funcionários Inativos e identifica cards cinza com indicador visual em &lt; 5 segundos
- [x] Inativação de funcionário ativo funciona com confirmação e respeita filtro vigente
- [x] Gráfico de evolução não exibe pontos originados de resumos `decimoTerceiro=true` (teste automatizado)
- [x] Folha de Pagamento abre com ano corrente e não dispara listagem sem filtro de ano
- [x] Testes unitários cobrem filtro `ativo` em `FuncionarioService`, exclusão 13º em `DashboardService`, e filtro por ano em `ResumoFolhaPagamentoService`

---

## Brownfield Notes (Knowledge Verification — Step 1)

| Área | Estado atual | Gap |
| --- | --- | --- |
| Funcionário `ativo` | Campo e soft-delete existem (`Funcionario.java`, `FuncionarioService.remover`) | Listagem sempre `ativo=true`; sem query param |
| UI Inativar | `Funcionarios/index.tsx` — botão com tooltip "Inativar" | `ativo` ignorado no render; sem filtro status |
| 13º salário | `ResumoFolhaPagamento.decimoTerceiro` | `findUltimos12Meses` não filtra |
| Folha ano | Filtro client-side opcional, default vazio | Sem param `ano` na API; `listarTodos()` na carga |

**Referências de código:**

- `backend/.../cadastros/application/FuncionarioService.java`
- `backend/.../folha/infrastructure/ResumoFolhaPagamentoRepository.java` (`findUltimos12Meses`)
- `backend/.../dashboard/application/DashboardService.java` (`calcularEvolucaoMensal*`)
- `frontend/src/pages/Funcionarios/index.tsx`
- `frontend/src/pages/FolhaPagamento/index.tsx`
- `frontend/src/pages/BeneficiosMensais/index.tsx` (padrão de ano default)
