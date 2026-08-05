# Dashboard Customizável — Context

**Gathered:** 2026-08-04
**Spec:** `_docs/specs/features/dashboard-customizavel/spec.md`
**Estudo de origem:** `_docs/specs/features/workspace-usuario/estudo-dashboard-customizavel.md`
**Status:** Ready for Execute (Batch 1: T1–T6)

---

## Feature Boundary

Transformar o dashboard gerencial hoje fixo (`frontend/src/pages/Dashboard/index.tsx`, 603 linhas hardcoded) numa tela onde cada usuário monta o próprio layout: escolhe quais widgets ver, em que ordem e com que largura (Fase 1), e depois parametriza cada widget — competência, top N, filtro de centro de custo/linha, tipo de visualização (Fase 2). O layout é persistido no backend por usuário. O dashboard clássico permanece acessível e inalterado durante toda a transição.

**Fora da fronteira:** query builder self-service (Fase 3 do estudo, com estudo dedicado próprio) e templates de dashboard por papel.

---

## Implementation Decisions

### Escopo e faseamento

- A spec cobre **Fase 1 + Fase 2** do estudo numa única especificação (usuário confirmou após ver o detalhamento das fases).
- **Fase 3 (query builder) fica fora** — permanece condicionada aos gatilhos do estudo §9.
- **Templates de dashboard por papel ficam fora** desta spec — o usuário optou por adiar; a feature `workspace-usuario` já especifica um catálogo de templates e é o lugar natural para isso.

### Layout padrão e paridade visual

- O layout padrão do primeiro acesso é **exatamente o dashboard atual**: os mesmos 11 widgets, na mesma ordem, com as mesmas larguras aparentes. Ninguém abre a tela nova e sente que perdeu algo.
- Os 11 widgets de paridade: 4 KPIs (Total de Funcionários, Custo Empresa, Benefícios Ativos, Relação P/D), 1 gráfico de área (Evolução da Folha), 4 pizzas (Funcionários por CC, Funcionários por Linha, Custo por CC, Custo por Linha), 2 listas (Top Proventos, Top Descontos).

### Chips de variação falsos

- Os três chips hardcoded no JSX (`+2.5% este mês` na linha 199, `+5.2% este mês` na 227, `Estável` na 255) são **removidos**, não recalculados. Custo zero e o sistema para de exibir número inventado imediatamente.
- Fica registrado que "variação real calculada no backend" foi considerada e adiada — não é rejeição da ideia, é separação de escopo.

### Convivência com o dashboard atual

- A rota `/dashboard` atual **permanece funcionando e inalterada**. Não é substituída, não é redirecionada, não fica atrás de flag (o projeto não tem infraestrutura de feature flag).
- A tela customizável vive numa **rota nova, em paralelo**.
- **Dois itens de menu distintos e simultâneos**, ambos visíveis para quem tem acesso. O usuário escolhe qual usar; a migração é por adoção, não por imposição.

### Regra de acesso à tela nova

- Usuário **sem escopo de dados** (sem vínculo de funcionário ou sem nó no organograma, o mesmo critério de `deveNegarAcesso` no `DashboardService`) **não tem acesso ao dashboard customizável**: o item de menu não aparece e a rota é bloqueada.
- Esse usuário continua com o `/dashboard` clássico, que mantém o comportamento atual.
- Racional do usuário: montar um layout de dados que a pessoa não pode enxergar não faz sentido — melhor não oferecer a tela do que oferecer uma tela vazia.
- **Não** haverá permissão nova dedicada (`DASHBOARD_CUSTOMIZAR` foi descartada) — o gate é o escopo de dados que já existe.

### Modo de edição e salvamento

- **Salvamento explícito**: botão Salvar, botão Cancelar (descarta as mudanças da sessão de edição) e Restaurar padrão (volta ao layout de paridade).
- Nada de autosave. Se o salvamento falhar, o usuário vê o erro e **não perde o trabalho** — as edições continuam na tela para nova tentativa.
- Fora do modo de edição a grade é estática, sem handles de arraste.

### Tamanho dos widgets

- **Fase 1 controla apenas a largura**, por presets (P / M / G / Full).
- **Altura é automática por tipo de widget** — o usuário não escolhe. Menos controle é menos chance de o usuário produzir um layout quebrado.
- O campo de altura (`rowSpan`) **nasce no modelo de dados desde a Fase 1**, apenas não exposto na interface, para que a Fase 2 possa liberá-lo sem migration.

### Competência (Fase 2)

- **Seletor global no topo da página** define a competência de todos os widgets.
- **Um widget individual pode fixar a própria competência**, sobrescrevendo o global — é o que permite colocar julho ao lado de junho e comparar.

### Agent's Discretion

Itens que o usuário deixou para o agente decidir ou que não foram levantados na discussão, resolvidos por default e registrados como Assumption na spec:

- Caminho exato da rota nova e rótulo do item de menu.
- Estratégia de persistência do layout (JSONB vs. tabela normalizada) — decisão de Design, com o alerta de que **não existe nenhum JSONB no backend hoje**.
- Status HTTP de erro de validação — resolvido pela convenção real do projeto (400), não pelo 422 que o estudo propunha.
- Mecanismo de notificação de erro na tela — resolvido pelo padrão real da página (`useNotification`), não por `react-toastify`.
- Valores de limite (máximo de widgets por layout).
- Se a competência global selecionada persiste entre sessões ou é apenas da sessão corrente.

### Declined / Undiscussed Gray Areas → Assumptions

Todas as áreas cinzentas levantadas foram discutidas e decididas. As demais decisões (concorrência entre abas, ciclo de vida do layout, observabilidade, comportamento com widget removido do catálogo) não foram levantadas pelo usuário e estão registradas com default e racional na seção **Assumptions & Open Questions** da spec — nenhuma foi silenciosamente descartada.

---

## Specific References

- **"Quero widgets iguais da dash atual e layout atual como default"** — âncora de paridade. É o critério de sucesso emocional da Fase 1: a tela nova não pode parecer uma regressão.
- **"Manter a rota atual e criar uma nova rota com essa spec"** — convivência, não substituição.
- **"É possível remover o acesso ao dash customizável? Aí fica só o atual"** — origem da regra de gate por escopo de dados.

---

## Deferred Ideas

- **Variação percentual real nos KPIs** (competência atual vs. anterior, calculada no backend) — substituiria com honestidade os chips que estão sendo removidos.
- **Templates de dashboard por papel** — ADMIN/RH publica, usuário clona. Sai desta spec; candidato natural ao catálogo de templates de `workspace-usuario`.
- **Query builder self-service** — Fase 3, condicionada aos gatilhos do estudo §9; estudo dedicado em `estudo-dashboard-query-builder.md`.
- **Resize livre em pixels** via `react-grid-layout` — só se os presets de largura virarem reclamação recorrente; o modelo de dados (`colSpan`/`rowSpan`) já é a abstração certa para absorver essa troca.
- **Endpoint batch** (`POST /dashboard/widgets/batch`) — só se a medição da Fase 2 mostrar problema de paralelismo de requisições.
- **Widget "Funcionários por Cargo"** — o campo `porCargo` já é calculado e devolvido pelo `DashboardStatsDTO`, mas nunca foi renderizado. Custo de backend zero. Proposto como assumption na spec, sujeito a veto.
