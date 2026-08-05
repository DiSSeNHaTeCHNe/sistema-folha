# Dashboard Customizável Specification

**Related:** `_docs/specs/features/workspace-usuario/estudo-dashboard-customizavel.md` (estudo de origem — decisões AD-DC-01…09); `_docs/specs/features/workspace-usuario/spec.md` (Nível 2 — estende o registry e o modelo de layout desta feature); `_docs/specs/features/workspace-usuario/estudo-dashboard-query-builder.md` (Fase 3, fora desta spec); AD-008 (layout de pacote por domínio); AD-011 (`ACESSO_TOTAL` ≠ `ADMIN`); AD-013 (API Key `sf_live_`, consumidores de `/dashboard/stats`)
**Complexity:** Complex
**Spec status:** Draft — tasks.md capturado (24 tasks, 4 batches), aguardando aprovação Execute

## Problem Statement

O dashboard gerencial é hoje um único componente de 603 linhas com estrutura totalmente fixa no JSX: onze blocos em ordem imutável, sem seletor de competência (mostra sempre a folha mais recente), com `slice(0, 5)` e `slice(0, 6)` cravados nos gráficos de pizza — não há como enxergar o sétimo centro de custo. Um diretor que só acompanha custo total vê a mesma tela que um analista de benefícios, e nenhum dos dois consegue ajustá-la. Pior: três dos quatro cards de KPI exibem chips de variação percentual **inventados** (`+2.5% este mês`, `+5.2% este mês`, `Estável` são strings literais no código), o que é um defeito de confiabilidade em produção e não apenas dívida estética.

Do lado do servidor, `GET /api/dashboard/stats` calcula os onze blocos em toda requisição, mesmo que o usuário olhe dois. O motor de agregação já é parametrizável — `DashboardConsultaPort.getStatsForCompetencia` e `getEvolucaoMeses` existem e estão testados — mas nada disso é exposto pelo controller.

Esta feature entrega um dashboard que cada usuário monta: escolhe quais widgets ver, em que ordem e com que largura, com o layout persistido por usuário no backend (Fase 1); e depois parametriza cada widget por competência, top N, filtro de centro de custo/linha e tipo de visualização (Fase 2). O dashboard clássico permanece intacto e acessível durante toda a transição.

## Goals

- [ ] Usuário monta o próprio layout (quais widgets, em que ordem, com que largura), salva, sai e reencontra o mesmo layout em outro navegador
- [ ] Layout padrão do primeiro acesso é **idêntico** ao dashboard atual — paridade visual completa, zero sensação de regressão
- [ ] Nenhum número exibido na tela é inventado — os três chips de variação falsos deixam de existir
- [ ] Usuário escolhe a competência exibida (global e, quando quiser, por widget) — hoje é sempre a folha mais recente, sem alternativa
- [ ] Custo de consulta passa a ser proporcional ao que está na tela — hoje os onze blocos são calculados sempre
- [ ] O escopo de centro de custo (regra canônica `CentroCustoEfetivo`) é reaplicado no servidor em **todo** endpoint de widget — o catálogo filtrado é defesa secundária, nunca única
- [ ] `/dashboard` clássico e `GET /dashboard/stats` continuam funcionando sem alteração durante toda a transição

## Out of Scope

Explicitamente excluídos. Documentado para evitar expansão de escopo.

| Feature | Reason |
| --- | --- |
| Query builder self-service (métrica × dimensão, SQL dinâmico) | Fase 3 do estudo, ~3 meses, condicionada aos gatilhos do §9 do estudo; tem estudo dedicado (`estudo-dashboard-query-builder.md`); reimplementar a garantia de ACL em cada caminho de SQL gerado é risco desproporcional agora |
| Templates de dashboard por papel (ADMIN/RH publica, usuário clona) | Usuário optou por adiar; `workspace-usuario` já especifica um catálogo de templates e é o lugar natural |
| Resize livre em pixels (`react-grid-layout`) | Presets de largura cobrem o caso comum; `colSpan`/`rowSpan` já são a abstração que absorve essa troca depois, sem mudar o modelo de dados |
| Variação percentual real nos KPIs (competência atual vs. anterior) | Os chips falsos são **removidos** nesta feature; calcular variação de verdade é trabalho de backend novo, adiado deliberadamente |
| Datasets criados pelo usuário, fórmulas, workspaces múltiplos | Nível 2 — `workspace-usuario/spec.md`. Esta feature é o Nível 1 e não cria dado novo, só reorganiza o que já é calculado |
| Substituir ou remover a rota `/dashboard` atual | Usuário decidiu convivência das duas telas; a remoção da antiga é decisão futura, tomada por evidência de adoção |
| Compartilhar layout entre usuários | Layout é estritamente pessoal nesta feature; compartilhamento de estrutura é a fronteira de `workspace-usuario` |
| Endpoint batch (`POST /dashboard/widgets/batch`) | Só se a medição da Fase 2 mostrar problema real de paralelismo — otimização condicionada a evidência |
| Sincronizar tema/preferências de UI no backend | Feature distinta já registrada em STATE.md Deferred Ideas |

---

## Assumptions & Open Questions

Toda ambiguidade está resolvida ou registrada aqui — nada fica silenciosamente indefinido.

| Assumption / decisão | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Escopo da spec | Fase 1 + Fase 2 do estudo; Fase 3 fora | Usuário confirmou após ver o detalhamento das fases | y |
| Layout padrão | Os 11 widgets do dashboard atual, mesma ordem e larguras equivalentes | Paridade visual é o critério de aceite emocional da Fase 1 (`context.md`) | y |
| Chips de variação falsos | Removidos, não recalculados | Custo zero, para de exibir número inventado imediatamente; variação real fica em Deferred | y |
| Convivência de telas | `/dashboard` permanece inalterada; tela nova em rota paralela; dois itens de menu simultâneos | Migração por adoção, não por imposição; o projeto não tem feature flag | y |
| Gate de acesso à tela nova | Sem escopo de dados (mesmo critério de `DashboardService.deveNegarAcesso`) ⇒ item de menu oculto e rota/endpoints bloqueados; usuário fica com `/dashboard` clássico | Montar layout de dado que a pessoa não pode ver não faz sentido; sem permissão nova dedicada | y |
| Salvamento | Explícito (Salvar / Cancelar / Restaurar padrão); sem autosave | Previsível; em falha o usuário não perde o trabalho | y |
| Tamanho do widget | Fase 1 expõe só largura por presets (P/M/G/Full → `colSpan` 3/4/6/12); altura automática por tipo | Menos superfície para o usuário quebrar o layout; `rowSpan` nasce no schema para a Fase 2 liberar sem migration | y |
| Competência (Fase 2) | Seletor global no topo + override opcional por widget | Permite comparar julho x junho lado a lado | y |
| Persistência da competência global | Seleção global vale para a sessão corrente e **não** é gravada no layout; só o override por widget é persistido | Competência global é gesto exploratório; gravá-la faria o usuário reabrir o dashboard preso a um mês antigo | n — validar no Design |
| Rota e rótulo de menu | Rota `/meu-dashboard`, item de menu "Meu Dashboard" ao lado de "Dashboard" | Nome descreve a natureza pessoal do layout; `/dashboard-v2` do estudo é jargão de implementação vazando para a URL | n — nome sujeito a veto |
| Estratégia de persistência do layout | JSONB por usuário (AD-DC-03 do estudo), via `@JdbcTypeCode(SqlTypes.JSON)` nativo do Hibernate 6 — **sem dependência nova** | Payload é sempre lido e escrito inteiro, cardinalidade baixa, absorve a evolução de `config` na Fase 2 sem migration | n — **Design deve validar: não existe nenhum JSONB no backend hoje**; alternativa é tabela normalizada `dashboard_layout_widget` |
| Status HTTP de validação | **400** (convenção real do `GlobalExceptionHandler`), não 422 como o estudo propunha; corpo no formato `ErrorResponse(status, message)` | O projeto não usa RFC 7807/`ProblemDetail`; divergir criaria um segundo padrão de erro | y |
| Colunas de auditoria | `data_criacao` / `data_atualizacao` com `@PrePersist`/`@PreUpdate`, como em `Funcionario` | Convenção real do projeto; `data_alteracao` do estudo não existe em lugar nenhum | y |
| Notificação de erro na tela | Componente `Notification` + hook `useNotification`, já usado pela página Dashboard | `react-toastify` está instalado mas **não há `ToastContainer` montado** — os toasts atuais provavelmente não renderizam | y |
| Limite de widgets por layout | 30 por layout, validado no servidor (`@Size`) e refletido na interface | Guard-rail contra payload inflado e contra 30 requisições simultâneas na Fase 2 | n — valor exato confirmável no Design |
| Concorrência entre abas/dispositivos | Última escrita vence; sem lock otimista no MVP | Layout é pessoal e de baixa contenção; o custo de conflito é reordenar de novo, não perder dado de negócio | n |
| Widget salvo que sumiu do catálogo | Frontend ignora silenciosamente o `widgetId` desconhecido e renderiza o resto; servidor rejeita `widgetId` desconhecido apenas na **escrita** | `widgetId` é chave lógica, nunca FK (AD-DC-05); layouts antigos não podem quebrar por evolução do catálogo | y |
| Ciclo de vida do layout | Sem expiração; `ON DELETE CASCADE` no usuário; `DELETE /dashboard/layout` significa "voltar ao padrão", não "ficar sem dashboard" | Layout é preferência de UI, não dado de negócio com retenção | y |
| Cache de partida | Último layout conhecido em `localStorage`, seguindo o padrão de `theme/storage.ts` | Evita flash do layout padrão antes da resposta do servidor; nunca é fonte de verdade | y |
| Widget "Funcionários por Cargo" | Entra no catálogo (não no layout padrão). `porCargo` já é calculado e devolvido pelo `DashboardStatsDTO` e **nunca foi renderizado** | Custo de backend zero e dá sentido concreto ao gesto de "adicionar widget" já na Fase 1 | n — sujeito a veto |
| Múltiplas instâncias do mesmo widget | Só na Fase 2; na Fase 1 o catálogo marca como já adicionado o que está no layout | `instanceId` já existe no modelo desde a Fase 1, então liberar depois não exige migration | y |
| Motor de grid | `@dnd-kit/sortable` sobre CSS Grid de 12 colunas (AD-DC-01) | Sem dependência nova; `KeyboardSensor` do dnd-kit dá acessibilidade por teclado de graça. **Ressalva factual:** hoje só `@dnd-kit/core` é usado (Organograma), e para associação, não reordenação — `sortable` está instalado mas nunca foi importado | y (decisão), com ressalva de familiaridade menor que o estudo supunha |
| Idioma e locale | pt-BR; moeda BRL `R$ 1.234,56`; competência `MM/yyyy` | Padrão operacional de todo o sistema | y |

**Open questions:** nenhuma pendente sem registro. Os itens marcados `n` acima são decisões de Design (estratégia JSONB, valor do limite, nome da rota, persistência da competência global, widget de Cargo) e cada um tem default explícito para o caso de não serem revisitados.

---

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | `widgetId` validado contra o catálogo do servidor na escrita; `colSpan` ∈ [1,12]; `rowSpan` ∈ [1,3]; `ordem` ≥ 0; máximo de 30 widgets por layout; `instanceId` obrigatório e único dentro do layout; `nome` do layout ≤ 100 caracteres. Na Fase 2: `topN` ∈ [1,50], `competencia` no formato `yyyy-MM` e existente, dimensão de filtro restrita a whitelist |
| Failure / partial-failure states | O `PUT` do layout é atômico: ou o layout inteiro é gravado, ou nada muda. Em falha o usuário permanece no modo de edição com as alterações intactas e recebe erro explícito — nunca há gravação parcial nem perda silenciosa do trabalho. Na Fase 2, falha de um widget isola-se nele: o widget mostra estado de erro com opção de recarregar, os demais continuam renderizando |
| Idempotency / retry / duplicate handling | `PUT /dashboard/layout` é idempotente por natureza (substitui o layout inteiro) — reenviar o mesmo payload produz o mesmo estado. `DELETE` sobre layout já ausente devolve o mesmo 204. Adicionar o mesmo `widgetId` duas vezes é impedido na Fase 1 (catálogo marca como já adicionado) e permitido na Fase 2 com `instanceId` distinto |
| Auth boundaries & rate limits | `usuario_id` derivado **sempre** de `Authentication`, nunca do payload — o DTO não carrega identificador de usuário. Todos os endpoints exigem autenticação e escopo de dados válido; sem escopo ⇒ 403. Catálogo filtrado por `AccessContextDTO`, e cada endpoint de widget da Fase 2 **reaplica** o escopo independentemente. Sem rate limit dedicado: o limite de 30 widgets é o guard-rail de volume |
| Concurrency / ordering | Duas abas editando o mesmo layout: última escrita vence, sem lock otimista (baixa contenção, dado de preferência). A ordem dos widgets é explícita no campo `ordem` e normalizada pelo servidor na gravação, então nenhuma renderização depende da ordem do array no JSON |
| Data lifecycle / expiry | Layout não expira. `ON DELETE CASCADE` no usuário. `DELETE /dashboard/layout` reseta para o padrão (o usuário volta ao layout de paridade, nunca fica sem dashboard). `versao_schema` permite normalização lazy do formato em memória, com regravação no próximo salvamento, sem downtime |
| Observability | Log estruturado com prefixo de domínio, no padrão de `DashboardService`: criação do layout padrão no primeiro acesso, gravação, reset, e rejeição de `widgetId` inválido (indício de payload adulterado). Na Fase 2, log de consulta por widget com o escopo aplicado |
| External-dependency failure | N/A — sem dependência externa além do PostgreSQL já em uso. Nenhuma biblioteca nova é necessária: `@dnd-kit/sortable` e `@tanstack/react-query` já estão no `package.json`, e o suporte a JSON no Hibernate 6 é nativo |
| State-transition integrity | Layout: inexistente → padrão (criado no primeiro `GET`) → customizado → padrão de novo (via reset). Modo de edição: visualização → edição → (salvo \| cancelado) → visualização; cancelar nunca persiste, salvar sempre persiste o layout inteiro. Um layout nunca fica num estado intermediário entre dois salvamentos |

---

## User Stories

### P1: Ver o dashboard customizável com paridade total ⭐ MVP

**User Story**: Como usuário do sistema, quero abrir o dashboard customizável e encontrar exatamente a tela que já conheço, para adotar a novidade sem perder nada e sem precisar reconfigurar coisa alguma.

**Why P1**: Paridade é a condição de entrada. Um dashboard novo que começa vazio ou diferente do atual é percebido como regressão e não é adotado.

**Acceptance Criteria**:

1. (DASHC-01) WHEN um usuário com escopo de dados acessa a tela customizável pela primeira vez THEN o sistema SHALL criar e exibir um layout padrão contendo os 11 widgets do dashboard atual, na mesma ordem: os 4 KPIs (Total de Funcionários, Custo Empresa, Benefícios Ativos, Relação P/D), o gráfico de área de Evolução da Folha, as 4 pizzas (Funcionários por CC, Funcionários por Linha, Custo por CC, Custo por Linha) e as 2 listas (Top Proventos, Top Descontos)
2. (DASHC-02) WHEN o layout padrão é exibido THEN cada widget SHALL apresentar o mesmo valor que o bloco equivalente do `/dashboard` clássico para o mesmo usuário e a mesma competência
3. (DASHC-03) WHEN um usuário **sem escopo de dados** (sem funcionário vinculado ou sem nó no organograma, o mesmo critério aplicado hoje em `DashboardService`) tenta acessar a tela customizável THEN o sistema SHALL negar com HTTP 403 nos endpoints de layout e de catálogo, e o item de menu correspondente NÃO SHALL ser exibido para ele
4. (DASHC-04) WHEN um usuário sem escopo de dados acessa `/dashboard` THEN a tela clássica SHALL continuar respondendo exatamente como hoje, sem alteração de comportamento
5. (DASHC-05) WHEN qualquer usuário navega no menu THEN SHALL ver dois itens distintos e simultâneos — o dashboard clássico e o customizável — sem que um substitua ou redirecione para o outro
6. (DASHC-06) WHEN a tela customizável é carregada THEN nenhum KPI SHALL exibir chip de variação percentual, e as strings `+2.5% este mês`, `+5.2% este mês` e `Estável` NÃO SHALL existir em nenhum ponto do código do dashboard

**Independent Test**: Logar com usuário que tem escopo, abrir a tela nova sem nunca a ter aberto antes, e comparar bloco a bloco com `/dashboard` — mesmos 11 blocos, mesma ordem, mesmos números, nenhum chip de variação. Logar com usuário sem vínculo no organograma e confirmar 403 na rota nova e menu sem o item, com `/dashboard` ainda acessível.

---

### P1: Reordenar, redimensionar e remover widgets ⭐ MVP

**User Story**: Como gestor, quero arrastar os widgets para a ordem que faz sentido para mim, escolher a largura de cada um e tirar da tela o que não uso, para que o dashboard reflita o que eu realmente acompanho.

**Why P1**: É o núcleo da feature. Sem manipulação direta do layout, nada mais nesta spec tem propósito.

**Acceptance Criteria**:

1. (DASHC-07) WHEN o usuário ativa o modo de edição e arrasta um widget para outra posição THEN o sistema SHALL reposicioná-lo na grade e atualizar a ordem de todos os widgets afetados
2. (DASHC-08) WHEN o usuário navega até um widget por teclado no modo de edição e usa as teclas de movimentação THEN o sistema SHALL permitir reordenar sem uso de mouse
3. (DASHC-09) WHEN o usuário escolhe um preset de largura (P, M, G ou Full) no menu de um widget THEN o sistema SHALL aplicar respectivamente `colSpan` 3, 4, 6 ou 12 e refletir a mudança imediatamente na grade
4. (DASHC-10) WHEN o usuário remove um widget THEN ele SHALL desaparecer da grade e voltar a ficar disponível no catálogo
5. (DASHC-11) WHEN a tela está **fora** do modo de edição THEN a grade SHALL ser estática, sem alças de arraste, sem menu de tamanho e sem botão de remover
6. (DASHC-12) WHEN a largura da viewport está abaixo do breakpoint de tela pequena THEN todo widget SHALL ocupar as 12 colunas, independentemente do `colSpan` salvo, e o layout salvo NÃO SHALL ser alterado por isso
7. (DASHC-13) WHEN o usuário remove todos os widgets THEN a tela SHALL exibir um estado vazio explicativo com ação para adicionar widget ou restaurar o padrão — e NÃO SHALL travar nem exibir área em branco sem saída

**Independent Test**: Entrar em modo de edição, arrastar o quarto widget para a primeira posição, mudar sua largura para Full, remover outro widget, e confirmar visualmente que a grade reflete as três operações. Repetir a reordenação usando apenas o teclado. Estreitar a janela e confirmar que tudo empilha em coluna única.

---

### P1: Adicionar widget a partir do catálogo ⭐ MVP

**User Story**: Como gestor, quero abrir uma lista dos widgets disponíveis e escolher o que quero acrescentar ao meu dashboard, para montar a tela sem depender de um desenvolvedor.

**Why P1**: Remover sem poder adicionar de volta é um caminho sem retorno — as duas operações são a mesma capacidade.

**Acceptance Criteria**:

1. (DASHC-14) WHEN o usuário abre o catálogo de widgets THEN o sistema SHALL listá-los a partir do **servidor** (nunca de uma lista fixa no frontend), com título, descrição e categoria (KPI, GRÁFICO ou LISTA)
2. (DASHC-15) WHEN o catálogo é montado para um usuário THEN o servidor SHALL filtrá-lo pelo contexto de acesso desse usuário, de modo que um widget cujo dado ele não pode consumir não apareça sequer como opção
3. (DASHC-16) WHEN o usuário adiciona um widget do catálogo THEN o sistema SHALL inseri-lo ao final da grade com a largura padrão definida no catálogo para aquele widget
4. (DASHC-17) WHEN um widget já está presente no layout THEN o catálogo SHALL indicá-lo como já adicionado e impedir a inclusão duplicada (na Fase 1 não há múltiplas instâncias)
5. (DASHC-18) WHEN o layout já contém o número máximo de widgets permitido THEN o sistema SHALL impedir novas inclusões com mensagem explicando o limite, tanto na interface quanto na validação do servidor

**Independent Test**: Remover a lista de Top Descontos, abrir o catálogo, confirmar que ela reaparece como disponível e que os widgets ainda presentes estão marcados como já adicionados; readicioná-la e confirmar que entra ao final com a largura padrão.

---

### P1: Salvar, cancelar e restaurar o layout ⭐ MVP

**User Story**: Como gestor, quero salvar explicitamente o layout que montei e reencontrá-lo em qualquer navegador, e poder desistir das alterações ou voltar ao padrão quando quiser.

**Why P1**: Sem persistência por usuário no servidor, a customização morre ao trocar de máquina — o que é exatamente o problema que o estudo descartou ao rejeitar `localStorage` como solução final.

**Acceptance Criteria**:

1. (DASHC-19) WHEN o usuário confirma **Salvar** no modo de edição THEN o sistema SHALL persistir o layout completo vinculado ao usuário autenticado e sair do modo de edição
2. (DASHC-20) WHEN o usuário salva, encerra a sessão e acessa a tela de outro navegador ou dispositivo THEN SHALL ver o mesmo layout que salvou
3. (DASHC-21) WHEN o usuário aciona **Cancelar** no modo de edição THEN o sistema SHALL descartar todas as alterações da sessão de edição e restaurar o último layout salvo, sem gravar nada
4. (DASHC-22) WHEN o usuário aciona **Restaurar padrão** e confirma THEN o sistema SHALL remover a customização e voltar ao layout padrão de paridade descrito em DASHC-01
5. (DASHC-23) WHEN a gravação do layout falha (erro de rede ou do servidor) THEN o sistema SHALL manter o usuário no modo de edição com todas as alterações preservadas na tela e exibir mensagem de erro clara — NÃO SHALL descartar o trabalho nem afirmar sucesso
6. (DASHC-24) WHEN dois usuários distintos customizam seus dashboards THEN o layout de um NÃO SHALL ser visível nem afetado pelo outro, em nenhum cenário
7. (DASHC-25) WHEN uma requisição de gravação chega THEN o servidor SHALL derivar o usuário exclusivamente da autenticação e ignorar qualquer identificador de usuário presente no corpo da requisição
8. (DASHC-26) WHEN o corpo da gravação contém um `widgetId` que não existe no catálogo, ou valores fora dos limites (`colSpan` fora de 1–12, `rowSpan` fora de 1–3, `ordem` negativa, mais widgets que o máximo permitido) THEN o servidor SHALL rejeitar a requisição inteira com HTTP 400 e mensagem indicando o campo inválido, sem gravar nada
9. (DASHC-27) WHEN um layout salvo contém um `widgetId` que deixou de existir no catálogo THEN a leitura SHALL continuar funcionando: o widget desconhecido é ignorado e os demais são renderizados normalmente

**Independent Test**: Montar um layout distinto do padrão, salvar, deslogar, entrar em janela anônima e confirmar o mesmo layout. Editar, cancelar, e confirmar que voltou ao salvo. Restaurar padrão e confirmar os 11 widgets originais. Enviar via cliente HTTP um `PUT` com `widgetId` inexistente e com `colSpan: 99` e confirmar 400 sem efeito colateral.

---

### P2: Escolher a competência exibida

**User Story**: Como gestor, quero escolher de qual competência o dashboard está falando, e fixar uma competência diferente em um widget específico, para comparar dois meses lado a lado.

**Why P2**: Depende de a Fase 1 estar em pé. Hoje o dashboard mostra sempre a folha mais recente sem alternativa, e o motor de consulta por competência já existe no `DashboardConsultaPort` — é exposição, não construção.

**Acceptance Criteria**:

1. (DASHC-28) WHEN o usuário seleciona uma competência no seletor global THEN todos os widgets sem competência própria SHALL passar a exibir os dados daquela competência
2. (DASHC-29) WHEN o usuário fixa uma competência específica na configuração de um widget THEN aquele widget SHALL ignorar o seletor global e exibir sempre a competência fixada, inclusive após recarregar a página
3. (DASHC-30) WHEN não existe folha processada para a competência selecionada THEN os widgets afetados SHALL exibir um estado vazio explícito informando a ausência de dados naquela competência — NÃO SHALL exibir zeros como se fossem valores reais
4. (DASHC-31) WHEN nenhuma competência é selecionada explicitamente THEN o sistema SHALL usar a folha mais recente disponível, preservando o comportamento atual como padrão

**Independent Test**: Selecionar uma competência anterior no seletor global e confirmar que todos os KPIs mudam de forma coerente com o `/dashboard/stats` daquela competência; fixar a competência anterior em um único widget, voltar o seletor global para a atual, e confirmar os dois meses convivendo na mesma tela.

---

### P2: Configurar os parâmetros de cada widget

**User Story**: Como gestor, quero ajustar o recorte de cada widget — quantos itens ele mostra, qual centro de custo ou linha de negócio ele considera, e como ele se apresenta — para ver o sétimo centro de custo que hoje simplesmente não aparece.

**Why P2**: É onde o `slice(0, 5)` cravado no código vira decisão do usuário. Depende da estrutura de configuração por widget que a Fase 1 já deixa pronta no modelo de dados.

**Acceptance Criteria**:

1. (DASHC-32) WHEN o usuário altera o número de itens exibidos de um widget de distribuição ou de lista THEN o widget SHALL passar a exibir essa quantidade, respeitando o limite máximo definido
2. (DASHC-33) WHEN o usuário filtra um widget por centro de custo ou linha de negócio THEN o widget SHALL considerar apenas o recorte selecionado, e as opções oferecidas SHALL estar contidas no escopo de acesso do usuário
3. (DASHC-34) WHEN o usuário altera o tipo de visualização de um widget de distribuição (por exemplo, de pizza para barras) THEN o widget SHALL passar a renderizar naquele formato com os mesmos dados
4. (DASHC-35) WHEN a configuração de um widget é gravada com parâmetro fora da whitelist (dimensão inexistente, quantidade fora do intervalo permitido, tipo de visualização não suportado) THEN o servidor SHALL rejeitar com HTTP 400 sem gravar
5. (DASHC-36) WHEN qualquer configuração de widget é processada THEN nenhum valor vindo do usuário SHALL ser interpolado diretamente em SQL — dimensões e ordenações SHALL ser resolvidas por whitelist tipada no servidor

**Independent Test**: Configurar o widget de Custo por Centro de Custo para exibir 10 itens e confirmar que o sétimo centro de custo aparece; enviar via cliente HTTP uma configuração com dimensão inventada e confirmar 400.

---

### P2: Múltiplas instâncias do mesmo widget

**User Story**: Como gestor, quero colocar o mesmo widget duas vezes na tela com recortes diferentes, para comparar sem alternar entre configurações.

**Why P2**: É a consequência natural de widgets configuráveis — sem isso, comparar exige trocar a configuração de ida e volta.

**Acceptance Criteria**:

1. (DASHC-37) WHEN o usuário adiciona um widget que já está no layout THEN o sistema SHALL criar uma nova instância independente com identificador próprio, e a restrição de duplicidade da Fase 1 (DASHC-17) SHALL deixar de valer
2. (DASHC-38) WHEN duas instâncias do mesmo widget têm configurações distintas THEN cada uma SHALL exibir o resultado da própria configuração, sem interferência da outra
3. (DASHC-39) WHEN o usuário remove uma das instâncias THEN as demais SHALL permanecer intactas com suas configurações

**Independent Test**: Adicionar dois widgets de Custo por Centro de Custo, fixar competências diferentes em cada um, e confirmar que exibem meses diferentes simultaneamente; remover um e confirmar que o outro não é afetado.

---

### P2: Carregamento por widget com ACL reaplicada

**User Story**: Como responsável pelo sistema, quero que cada widget busque o próprio dado num endpoint que revalida o escopo de acesso, para que o custo de consulta acompanhe o que está na tela e nenhum recorte novo abra caminho para vazamento de dado.

**Why P2**: É a contrapartida técnica obrigatória da configuração por widget. Sem ela, cada parâmetro novo seria um caminho novo de consulta sem barreira própria.

**Acceptance Criteria**:

1. (DASHC-40) WHEN um dashboard é carregado THEN o sistema SHALL consultar apenas os dados dos widgets presentes no layout — um widget ausente do layout NÃO SHALL gerar consulta
2. (DASHC-41) WHEN qualquer endpoint de widget é chamado THEN ele SHALL resolver e aplicar o escopo de centro de custo do usuário no servidor, independentemente de o widget ter vindo de um catálogo já filtrado
3. (DASHC-42) WHEN um usuário chama diretamente o endpoint de um widget que não consta do seu catálogo filtrado THEN o sistema SHALL negar a requisição — o filtro do catálogo NÃO SHALL ser a única barreira
4. (DASHC-43) WHEN o escopo de um usuário restringe centros de custo THEN nenhum endpoint de widget SHALL retornar valor agregado que inclua centro de custo fora desse escopo, aplicando a mesma regra canônica de centro de custo efetivo já usada em Folha e Dashboard
5. (DASHC-44) WHEN consumidores externos chamam `GET /dashboard/stats` THEN o endpoint SHALL continuar respondendo com o mesmo contrato, marcado como descontinuado mas funcional, para não quebrar integrações por API Key

**Independent Test**: Suíte de ACL por endpoint de widget, comparando usuário com acesso total, usuário com escopo restrito e usuário sem vínculo — o resultado do escopo restrito nunca pode conter centro de custo fora do seu conjunto, e a soma dos widgets de um usuário com acesso total tem de bater com `/dashboard/stats`.

---

**P3:** nenhuma história de prioridade 3 nesta spec. As ideias de menor prioridade que surgiram (variação percentual real, templates por papel, resize livre em pixels, endpoint batch) estão registradas em Out of Scope e em `context.md` — Deferred Ideas, e não são entregas desta feature.

---

## Edge Cases

- WHEN o usuário sem escopo de dados tenta acessar diretamente a URL da tela customizável, sem passar pelo menu, THEN o sistema SHALL negar com 403 no servidor — a ocultação do item de menu é conveniência de interface, nunca o controle de acesso
- WHEN dois dispositivos do mesmo usuário salvam layouts diferentes quase ao mesmo tempo THEN o último a gravar prevalece integralmente, e o outro dispositivo passa a ver esse layout no próximo carregamento — sem mesclagem parcial dos dois
- WHEN a resposta do servidor demora THEN a tela SHALL renderizar o último layout conhecido do cache local como estado de partida, sem nunca tratá-lo como fonte de verdade nem gravá-lo de volta
- WHEN o cache local contém um layout em formato mais antigo que o suportado THEN o sistema SHALL descartá-lo e usar a resposta do servidor, sem quebrar a renderização
- WHEN um layout gravado numa versão anterior do formato é lido THEN o sistema SHALL normalizá-lo em memória para o formato corrente e regravá-lo apenas no próximo salvamento do usuário — sem migração destrutiva nem downtime
- WHEN o layout do usuário contém widgets em ordem duplicada ou com lacunas (por gravação concorrente ou payload manipulado) THEN o servidor SHALL normalizar a ordem na gravação, garantindo sequência determinística na leitura
- WHEN um widget individual falha ao carregar seu dado na Fase 2 THEN o widget SHALL exibir estado de erro com ação de tentar novamente, e os demais widgets SHALL permanecer funcionais
- WHEN o usuário arrasta um widget e solta fora da área da grade THEN o sistema SHALL cancelar a operação mantendo a posição original, sem alterar o layout
- WHEN o usuário sai do modo de edição pela navegação do navegador com alterações não salvas THEN o sistema SHALL alertá-lo antes de descartar o trabalho
- WHEN o dashboard clássico e o customizável são abertos lado a lado pelo mesmo usuário na mesma competência THEN os valores equivalentes SHALL coincidir — divergência entre as duas telas é defeito, não diferença de recorte

---

## Requirement Traceability

| Requirement ID | Story | Fase | Phase | Status |
| --- | --- | --- | --- | --- |
| DASHC-01 | P1: Paridade total | 1 | T5, T8, T9, T15 | In Tasks |
| DASHC-02 | P1: Paridade total | 1 | T8, T9, T15 | In Tasks |
| DASHC-03 | P1: Paridade total | 1 | T3, T6, T14, T15 | In Tasks |
| DASHC-04 | P1: Paridade total | 1 | T14 | In Tasks |
| DASHC-05 | P1: Paridade total | 1 | T14 | In Tasks |
| DASHC-06 | P1: Paridade total | 1 | T8, T10, T15 | In Tasks |
| DASHC-07 | P1: Reordenar/redimensionar/remover | 1 | T12, T15 | In Tasks |
| DASHC-08 | P1: Reordenar/redimensionar/remover | 1 | T12, T15 | In Tasks |
| DASHC-09 | P1: Reordenar/redimensionar/remover | 1 | T12, T15 | In Tasks |
| DASHC-10 | P1: Reordenar/redimensionar/remover | 1 | T13, T15 | In Tasks |
| DASHC-11 | P1: Reordenar/redimensionar/remover | 1 | T9, T12 | In Tasks |
| DASHC-12 | P1: Reordenar/redimensionar/remover | 1 | T12, T15 | In Tasks |
| DASHC-13 | P1: Reordenar/redimensionar/remover | 1 | T13, T15 | In Tasks |
| DASHC-14 | P1: Catálogo | 1 | T4, T6, T7, T13 | In Tasks |
| DASHC-15 | P1: Catálogo | 1 | T4, T6 | In Tasks |
| DASHC-16 | P1: Catálogo | 1 | T13, T15 | In Tasks |
| DASHC-17 | P1: Catálogo | 1 | T13 | In Tasks |
| DASHC-18 | P1: Catálogo | 1 | T13, T5 | In Tasks |
| DASHC-19 | P1: Salvar/cancelar/restaurar | 1 | T5, T6, T11, T14 | In Tasks |
| DASHC-20 | P1: Salvar/cancelar/restaurar | 1 | T11, T16 | In Tasks |
| DASHC-21 | P1: Salvar/cancelar/restaurar | 1 | T11, T14, T15 | In Tasks |
| DASHC-22 | P1: Salvar/cancelar/restaurar | 1 | T5, T14 | In Tasks |
| DASHC-23 | P1: Salvar/cancelar/restaurar | 1 | T11, T14, T15 | In Tasks |
| DASHC-24 | P1: Salvar/cancelar/restaurar | 1 | T6, T15 | In Tasks |
| DASHC-25 | P1: Salvar/cancelar/restaurar | 1 | T3, T5 | In Tasks |
| DASHC-26 | P1: Salvar/cancelar/restaurar | 1 | T5, T6 | In Tasks |
| DASHC-27 | P1: Salvar/cancelar/restaurar | 1 | T5, T11, T15 | In Tasks |
| DASHC-28 | P2: Competência | 2 | T17, T21, T24 | In Tasks |
| DASHC-29 | P2: Competência | 2 | T22 | In Tasks |
| DASHC-30 | P2: Competência | 2 | T17, T24 | In Tasks |
| DASHC-31 | P2: Competência | 2 | T21 | In Tasks |
| DASHC-32 | P2: Parâmetros por widget | 2 | T17, T19, T22, T24 | In Tasks |
| DASHC-33 | P2: Parâmetros por widget | 2 | T17, T22 | In Tasks |
| DASHC-34 | P2: Parâmetros por widget | 2 | T19, T22, T24 | In Tasks |
| DASHC-35 | P2: Parâmetros por widget | 2 | T17, T6 | In Tasks |
| DASHC-36 | P2: Parâmetros por widget | 2 | T17 | In Tasks |
| DASHC-37 | P2: Múltiplas instâncias | 2 | T23 | In Tasks |
| DASHC-38 | P2: Múltiplas instâncias | 2 | T23 | In Tasks |
| DASHC-39 | P2: Múltiplas instâncias | 2 | T23 | In Tasks |
| DASHC-40 | P2: Carregamento por widget | 2 | T17, T20, T24 | In Tasks |
| DASHC-41 | P2: Carregamento por widget | 2 | T3, T17, T18 | In Tasks |
| DASHC-42 | P2: Carregamento por widget | 2 | T4, T18 | In Tasks |
| DASHC-43 | P2: Carregamento por widget | 2 | T17, T18, T19 | In Tasks |
| DASHC-44 | P2: Carregamento por widget | 2 | T18, T24 | In Tasks |

**ID format:** `DASHC-[NÚMERO]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 44 total, 44 mapeados a tasks (T1–T24), 0 pendentes ✅

---

## Success Criteria

- [ ] Um usuário monta seu layout, sai, entra de outro navegador e reencontra exatamente o que salvou
- [ ] No primeiro acesso, a tela nova é indistinguível do dashboard atual em conteúdo e ordem — nenhum usuário precisa configurar nada para começar a usá-la
- [ ] Nenhum valor exibido no dashboard é inventado: as três strings de variação hardcoded não existem mais no código
- [ ] Um usuário com escopo restrito de centros de custo não consegue, por nenhum caminho — catálogo, layout salvo, ou chamada direta a endpoint de widget — obter valor agregado que inclua centro de custo fora do seu escopo
- [ ] O dashboard clássico e `GET /dashboard/stats` continuam funcionando sem alteração de contrato ao final da entrega
- [ ] Falha ao salvar o layout nunca faz o usuário perder as alterações que estava fazendo
- [ ] O usuário consegue ver o sétimo centro de custo, que hoje é inacessível pelo `slice` fixo
- [ ] Um layout salvo continua carregando depois de um widget ser removido do catálogo

---

## Auto-Size Assessment

| Attribute | Value |
| --- | --- |
| **Scope** | **Complex** — persistência nova por usuário (primeiro JSONB do backend, se AD-DC-03 for mantido no Design), refatoração de um componente de 603 linhas em widgets independentes, motor de grade com arraste e acessibilidade por teclado, catálogo filtrado por ACL, e na Fase 2 a quebra de um DTO monolítico em endpoints parametrizados com revalidação de escopo em cada um |
| **Design** | **Concluído** — `design.md` (Approach A JSONB + stats monolítico→por widget + dnd-kit sortable); AD-017 active |
| **Tasks** | **Concluído** — 24 tasks em 6 fases, 4 batches Execute |
| **Discuss** | **Executado** — decisões capturadas em `context.md` (escopo, paridade, chips, convivência, gate de acesso, salvamento, tamanho, competência). Design deve revisitar apenas os itens marcados como não confirmados na tabela de Assumptions |

**Próximo passo sugerido:** Aprovar `tasks.md` e iniciar Execute — Batch 1 (T1–T6, backend layout).
