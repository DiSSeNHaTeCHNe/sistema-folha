# Organograma — Linhas de Hierarquia (Lista + Gráfico Vertical)

## Problem Statement

Na tela de Organograma, a hierarquia é difícil de seguir visualmente: no **modo Lista**, os nós filhos aparecem apenas com recuo (`margin-left`), sem conectores visuais entre pai e filhos; no **modo Gráfico**, o layout é **horizontal** (esquerda → direita), distante do organograma clássico top-down com linhas verticais. Usuários que gerenciam estruturas com vários níveis perdem tempo para identificar quem reporta a quem.

## Goals

- [ ] Exibir **linhas conectoras de árvore** no modo Lista (vertical + ramo horizontal por nó filho)
- [ ] Reorientar o modo Gráfico para layout **vertical top-down** (raiz no topo, filhos abaixo), com arestas visíveis conectando pai → filho
- [ ] Preservar **100% das funcionalidades existentes** (CRUD de nós, drag & drop no modo Lista, expand/hover, zoom/pan no Gráfico, etc.)
- [ ] Manter consistência visual com o tema Material-UI já usado na tela

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Alterações de backend ou API | Entrega exclusivamente visual no frontend |
| Novo modo de visualização ou toggle horizontal/vertical | Escopo = vertical fixo no Gráfico; sem opção extra |
| Drag & drop no modo Gráfico | Não existe hoje; fora desta entrega |
| Mudança de comportamento de expand/hover/accordion | Apenas adaptação visual às novas linhas |
| Exportação PNG/SVG/PDF do organograma | Melhoria futura documentada em relatórios |
| Biblioteca nova de organograma | Reutilizar ReactFlow + CSS existentes |
| Refatoração estrutural para `src/features/` | Fora do escopo; alterar só os arquivos atuais do organograma |
| Testes E2E Playwright | Harness FE ainda sem Vitest/E2E configurado; validação manual + lint/build |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Estilo das linhas — modo Lista | Cor `theme.palette.divider` (fallback `#bdbdbd`), espessura **2px**, cantos retos | Discreto, legível, alinhado ao MUI; não compete com borda azul dos cards | n |
| Padrão de conector — modo Lista | Árvore clássica: linha vertical contínua à esquerda dos filhos + ramo horizontal até o card; último irmão **não** prolonga a vertical abaixo de si | Padrão universal de tree view (explorador de arquivos) | n |
| Indentação — modo Lista | Manter equivalente visual ao recuo atual (~**32px** / `theme.spacing(4)`) entre níveis | Paridade com layout atual; linhas ocupam parte desse espaço | n |
| Linhas com card expandido ou compacto | Conectores permanecem alinhados ao **centro vertical do card** (modo compacto ou expandido) | Evita linhas “quebradas” quando usuário expande nó | n |
| Layout — modo Gráfico | **Top-down**: eixo Y = profundidade hierárquica; eixo X = posição entre irmãos | Requisito explícito do usuário (“tornar vertical”) | y |
| Handles ReactFlow | `target` em **Top**, `source` em **Bottom** (substituir Left/Right) | Conectores verticais naturais pai→filho | n |
| Tipo de aresta — modo Gráfico | `step` (ou `smoothstep` se `step` gerar artefatos) com cor `#1976d2` e `strokeWidth: 3` | Mantém identidade visual atual; `step` favorece organograma clássico | n |
| Espaçamento — modo Gráfico | Vertical entre níveis ~**200px**; horizontal entre irmãos ~**280–350px** (ajustável na implementação) | Valores próximos aos atuais (`levelWidth=350`, `nodeHeight=200`), só invertendo eixos | n |
| Múltiplas raízes | Cada raiz forma subárvore independente; linhas não cruzam entre raízes | Comportamento atual preservado | n |
| Organograma vazio / nó único | Sem linhas quando não há relação pai-filho | Linhas só existem onde há hierarquia | n |
| Regressão funcional | Nenhum handler, prop ou chamada de API alterada | Requisito explícito: “somente alteração visual” | y |

**Open questions:** none — all resolved or logged above (pending user confirmation on assumptions marked `n`).

**Implicit-requirement dimensions (Medium sweep):**

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | N/A — sem inputs novos |
| Failure / partial-failure states | N/A — renderização puramente visual |
| Auth / API / concurrency / persistence | N/A — frontend only |
| Observability | N/A |
| State-transition integrity | N/A — estados de expand/hover/drag inalterados |
| **Remaining dimensions** | N/A for this scope |

---

## User Stories

### P1: Modo Lista — linhas conectoras de hierarquia ⭐ MVP

**User Story**: As a **gestor de organograma**, I want **ver linhas verticais e ramos horizontais ligando pais e filhos no modo Lista** so that **identifique rapidamente a estrutura hierárquica sem contar recuos**.

**Why P1**: Modo Lista é o default da tela e onde ocorre drag & drop; é onde a ausência de conectores mais prejudica a leitura.

**Acceptance Criteria**:

1. WHEN usuário abre `/organograma` com `viewMode = list` e existem nós com `parentId` THEN cada nó filho SHALL exibir conector visual (linha vertical + ramo horizontal) ligando-o ao bloco de filhos do pai
2. WHEN um nó possui múltiplos filhos THEN sistema SHALL desenhar linha vertical compartilhada à esquerda do grupo de filhos, com ramo horizontal para **cada** filho
3. WHEN um nó é o **último irmão** entre filhos do mesmo pai THEN a linha vertical SHALL terminar no ramo desse nó (sem continuar abaixo)
4. WHEN existem **múltiplas raízes** THEN cada subárvore SHALL ter conectores independentes, sem linhas cruzando entre raízes
5. WHEN usuário expande ou passa o mouse sobre um nó (modo compacto ↔ expandido) THEN conectores SHALL permanecer visíveis e alinhados ao card
6. WHEN usuário arrasta funcionário ou centro de custo para um nó no modo Lista THEN drag & drop SHALL continuar funcionando como antes (drop target, feedback `isOver`, toast de sucesso)
7. WHEN usuário executa ações de editar, excluir ou adicionar filho no modo Lista THEN comportamento SHALL ser idêntico ao atual (mesmos handlers e dialogs)

**Independent Test**: Abrir organograma com 3+ níveis → confirmar linhas entre Diretoria → TI → Desenvolvimento → arrastar funcionário para um nó → expandir nó → editar nome → verificar que linhas permanecem coerentes.

---

### P1: Modo Gráfico — layout vertical top-down ⭐ MVP

**User Story**: As a **gestor de organograma**, I want **ver o organograma no modo Gráfico disposto de cima para baixo com linhas conectando pais e filhos** so that **tenha visão clássica de organograma corporativo**.

**Why P1**: Segundo requisito explícito; complementa o modo Lista para estruturas grandes.

**Acceptance Criteria**:

1. WHEN usuário alterna para `viewMode = graph` THEN nós SHALL ser posicionados com **raízes no topo** e descendentes **abaixo** (eixo Y cresce para baixo)
2. WHEN existe relação pai-filho THEN ReactFlow SHALL renderizar aresta visível do pai para cada filho, entrando pelo **topo** do card filho e saindo pela **base** do card pai
3. WHEN layout é calculado THEN nó pai SHALL permanecer **centralizado horizontalmente** em relação aos filhos (mesma regra de centralização do algoritmo atual, aplicada no eixo X)
4. WHEN usuário usa zoom, pan, minimap ou fit view THEN controles SHALL funcionar como hoje
5. WHEN usuário clica expandir, editar, excluir ou adicionar filho no modo Gráfico THEN ações SHALL funcionar como antes
6. WHEN existem múltiplas raízes THEN cada subárvore SHALL ser empilhada verticalmente sem sobreposição de nós
7. WHEN há apenas um nó raiz sem filhos THEN gráfico SHALL exibir o nó sem arestas (estado válido)

**Independent Test**: Alternar para Gráfico → confirmar raiz no topo → filhos abaixo conectados por linhas → zoom/pan → editar nó → adicionar filho → recarregar e verificar layout.

---

### P2: Consistência visual e não-regressão ⭐ Should have

**User Story**: As a **usuário da tela**, I want **linhas discretas e estáveis ao alternar modos** so that **a experiência permaneça profissional e previsível**.

**Why P2**: Garante qualidade percebida; não bloqueia MVP funcional.

**Acceptance Criteria**:

1. WHEN usuário alterna entre modos Lista ↔ Gráfico THEN estado de nós expandidos (`expandedNodeId`) SHALL ser preservado
2. WHEN tela é renderizada em viewport ≥ **768px** THEN linhas SHALL permanecer legíveis (sem sobreposição que oculte texto dos cards)
3. WHEN `npm run lint` e `npm run build` são executados no frontend THEN SHALL completar sem erros novos introduzidos por esta feature
4. WHEN organograma possui **5+ níveis** de profundidade THEN linhas/conectores SHALL continuar renderizando (scroll no Lista; zoom out no Gráfico)

**Independent Test**: Alternar modos com nó expandido → lint/build → organograma profundo (seed) → inspeção visual.

---

## Edge Cases

- WHEN organograma está vazio (`nos.length === 0`) THEN tela SHALL exibir empty state atual, sem linhas
- WHEN nó pai tem **1 filho** THEN conector Lista SHALL exibir linha simples pai→filho; Gráfico SHALL exibir uma aresta
- WHEN card expande de ~56px para ~200px de altura THEN conectores Lista SHALL realinhar sem desaparecer
- WHEN último filho de um pai é excluído THEN linhas do pai SHALL desaparecer (sem filhos, sem conectores)
- WHEN drag está ativo sobre nó no modo Lista THEN highlight de drop (`isOver`) SHALL permanecer visível sobre/além das linhas

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| ORG-LIN-01 | P1: Lista — conector pai-filho | Tasks | Done |
| ORG-LIN-02 | P1: Lista — múltiplos filhos e último irmão | Tasks | Done |
| ORG-LIN-03 | P1: Lista — múltiplas raízes | Tasks | Done |
| ORG-LIN-04 | P1: Lista — expand/hover preservado | Tasks | Done |
| ORG-LIN-05 | P1: Lista — drag & drop inalterado | Tasks | Done |
| ORG-LIN-06 | P1: Lista — CRUD inalterado | Tasks | Done |
| ORG-LIN-07 | P1: Gráfico — layout top-down | Tasks | Done |
| ORG-LIN-08 | P1: Gráfico — arestas Top/Bottom | Tasks | Done |
| ORG-LIN-09 | P1: Gráfico — centralização pai/filhos | Tasks | Done |
| ORG-LIN-10 | P1: Gráfico — zoom/pan/minimap | Tasks | Done |
| ORG-LIN-11 | P1: Gráfico — ações de nó inalteradas | Tasks | Done |
| ORG-LIN-12 | P1: Gráfico — múltiplas raízes | Tasks | Done |
| ORG-LIN-13 | P2: Preservar estado ao trocar modo | Tasks | Done |
| ORG-LIN-14 | P2: lint/build sem regressão | Tasks | Done |
| ORG-LIN-15 | P2: hierarquia profunda legível | Tasks | Done |

**Coverage:** 15 total, 15 mapped in tasks.md (T1–T6), 0 unmapped ✓

---

## Success Criteria

- [ ] Usuário identifica relação pai-filho no modo Lista **sem contar níveis de recuo**
- [ ] Modo Gráfico exibe organograma **top-down** reconhecível como hierarquia corporativa
- [ ] Zero regressões em drag & drop (Lista), CRUD de nós e controles ReactFlow (Gráfico)
- [ ] `cd frontend && npm run lint && npm run build` passam após implementação
- [ ] Validação manual documentada em `validation.md` após Execute + Verifier

---

## Arquivos impactados (referência para Design/Execute)

| Arquivo | Alteração esperada |
| ------- | ------------------ |
| `frontend/src/pages/Organograma/index.tsx` | Conectores CSS no `NoOrganogramaCard`; props `depth`/`isLast`/`isFirst` se necessário |
| `frontend/src/components/OrganogramaGrafico/index.tsx` | Layout vertical; handles Top/Bottom; ajuste do algoritmo `processNode` |

**Complexity tier:** Medium — Specify ✓ → Design ✓ → Tasks ✓ → Execute
