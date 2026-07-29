# Organograma — Linhas de Hierarquia Design

**Spec**: `_docs/specs/features/organograma-linhas-hierarquia/spec.md`  
**Status**: Draft — aguardando aprovação

---

## Architecture Overview

Entrega **100% frontend**, sem toque em backend, services ou contratos de API. Duas frentes independentes que compartilham apenas o tipo `NoOrganogramaWithChildren` já construído em `Organograma/index.tsx`:

1. **Modo Lista** — substituir recuo puro (`ml={4}`) por layout de árvore com conectores CSS (`::before` / `::after`) em wrapper dedicado, preservando `useDroppable` no `Card`.
2. **Modo Gráfico** — rotacionar o algoritmo de layout existente em `OrganogramaGrafico` (trocar papéis de X/Y), ajustar handles ReactFlow para Top/Bottom e tipo de aresta `step`.

Nenhum estado novo, nenhum hook novo, nenhuma prop de negócio alterada nos handlers existentes.

```mermaid
flowchart TB
  subgraph Page["Organograma/index.tsx"]
    VM[viewMode list | graph]
    ARV[construirArvore]
    VM -->|list| LIST[NoOrganogramaCard + TreeBranch wrapper]
    VM -->|graph| GRAF[OrganogramaGrafico]
    ARV --> LIST
    ARV --> GRAF
  end

  subgraph ListVisual["Modo Lista — só visual"]
    TB[OrganogramaTreeBranch]
    TB --> CSS[Pseudo-elementos divider 2px]
    TB --> CARD[Card + useDroppable inalterado]
  end

  subgraph GraphVisual["Modo Gráfico — só visual"]
    PN[processNode vertical]
    PN --> RF[ReactFlow nodes/edges]
    RF --> H[Handles Top/Bottom]
    RF --> E[Edges type step]
  end

  LIST --> TB
  GRAF --> PN
```

### Princípio de não-regressão

| Camada | O que permanece idêntico |
| ------ | ------------------------ |
| Handlers | `handleDragEnd`, `onSubmit`, `handleDelete`, `handleRemove*` |
| DnD | `useDroppable({ id: \`no-${no.id}\` })` no `Card`; `DndContext` intacto |
| Estado | `expandedNodeId`, `hoveredNodeId`, `viewMode`, dialogs |
| API | `organogramaService.*` — zero chamadas novas ou alteradas |
| Gráfico | Props de `OrganogramaGrafico`; callbacks `onEdit`, `onDelete`, etc. |

**Único efeito colateral aceito:** deslocamento visual de ~0–8px nos cards filhos (linhas ocupam parte do gutter); não altera hit area do drop além do que o layout já define.

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --------- | -------- | ---------- |
| `NoOrganogramaCard` | `frontend/src/pages/Organograma/index.tsx:80` | Envolver filhos com novo wrapper; adicionar props visuais opcionais (`treeContext`) |
| `construirArvore` | `Organograma/index.tsx:449` | Reutilizar sem alteração — árvore já ordenada por `posicao` |
| `OrganogramaGrafico` | `frontend/src/components/OrganogramaGrafico/index.tsx` | Alterar só `processNode`, handles e edge defaults |
| `NoOrganogramaNode` | `OrganogramaGrafico/index.tsx:64` | Trocar `Position.Left/Right` → `Top/Bottom` |
| ReactFlow stack | `OrganogramaGrafico` | `Background`, `Controls`, `MiniMap`, `fitView` — inalterados |
| MUI `Box` + `sx` | Toda a tela | Conectores via `sx` com `theme.palette.divider` |

### Integration Points

| System | Integration Method |
| ------ | ------------------ |
| Backend / API | **Nenhuma** |
| `organogramaService` | **Nenhuma alteração** |
| React Router | Rota `/organograma` inalterada |
| `@dnd-kit/core` | Drop target continua no `Card`; linhas ficam **fora** do `ref` do droppable |

---

## Components

### `OrganogramaTreeBranch` (novo — co-locado em `Organograma/index.tsx`)

- **Purpose**: Desenhar conectores de árvore (vertical + horizontal) ao redor de cada nó filho no modo Lista, sem interferir no DnD.
- **Location**: `frontend/src/pages/Organograma/index.tsx` (componente interno, mesmo arquivo — evita terceiro arquivo; spec limita escopo a 2 arquivos)
- **Interfaces**:

```typescript
interface OrganogramaTreeContext {
  /** true quando o nó tem pai (não é raiz dentro de um grupo de filhos) */
  hasParent: boolean;
  /** true quando é o último filho entre irmãos */
  isLastSibling: boolean;
}

interface OrganogramaTreeBranchProps {
  treeContext?: OrganogramaTreeContext;
  children: React.ReactNode;
}
```

- **Dependencies**: MUI `Box`, `useTheme` (opcional para `divider`)
- **Reuses**: Padrão de indentação atual (`spacing(4)` = 32px)

#### Layout estrutural (Lista)

```
┌─ OrganogramaTreeBranch (hasParent=false) ─────────────────┐
│  ┌─ Card (droppable) ─────────────────────────────────┐   │
│  │  Diretoria                                          │   │
│  └────────────────────────────────────────────────────┘   │
│  ┌─ children container (pl=4, position relative) ────────┐│
│  │  │ vertical line (::before on container)              ││
│  │  ├─ TreeBranch (hasParent, !isLast) ─ Card TI         ││
│  │  └─ TreeBranch (hasParent, isLast)  ─ Card RH         ││
│  └───────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────┘
```

#### CSS concreto (tokens)

| Token | Valor |
| ----- | ----- |
| `TREE_INDENT` | `theme.spacing(4)` → 32px |
| `TREE_LINE_WIDTH` | `2px` |
| `TREE_LINE_COLOR` | `theme.palette.divider` |
| `TREE_BRANCH_Y` | `28px` — alinhado ao centro do card **compacto** (`minHeight: 56` → metade) |

**Regra para expand/hover (ORG-LIN-04):** ramo horizontal fixo em `28px` do topo da linha do filho (centro do modo compacto). Quando o card expande, a linha **permanece visível** no topo do row; o card cresce para baixo. Isso evita `ref`/measurement e atende o spec (“conectores permanecem visíveis”) sem quebrar DnD. Pequeno desalinhamento visual aceito vs. centro dinâmico — documentado como trade-off consciente.

**Pseudo-elementos por filho** (`hasParent === true`):

```typescript
// Esboço — implementação final no Execute
{
  position: 'relative',
  pl: 2, // 16px — espaço após a vertical compartilhada
  '&::before': {
    content: '""',
    position: 'absolute',
    left: 0,
    top: 0,
    height: isLastSibling ? TREE_BRANCH_Y : '100%',
    borderLeft: `${TREE_LINE_WIDTH} solid`,
    borderColor: 'divider',
  },
  '&::after': {
    content: '""',
    position: 'absolute',
    left: 0,
    top: TREE_BRANCH_Y,
    width: 16,
    borderTop: `${TREE_LINE_WIDTH} solid`,
    borderColor: 'divider',
  },
}
```

**Container de filhos** (vertical compartilhada entre irmãos):

```typescript
{
  ml: 4,
  mt: 2,
  position: 'relative',
  pl: 0,
}
```

Raízes (`treeContext` omitido ou `hasParent: false`): sem pseudo-elementos; apenas renderizam `Card` + filhos.

### `NoOrganogramaCard` (alteração mínima)

- **Purpose**: Card interativo existente — **sem mudança de comportamento**.
- **Location**: `frontend/src/pages/Organograma/index.tsx:80`
- **Alterações**:
  - Novas props opcionais: `treeContext?: OrganogramaTreeContext`
  - Envolver retorno externo em `OrganogramaTreeBranch`
  - Ao mapear `no.children`, passar `isLastSibling: index === no.children.length - 1` e `hasParent: true`
  - Raízes top-level: `nos.map` sem `treeContext`
- **Invariante**: `ref={setNodeRef}` permanece **somente** no `Card`.

### `OrganogramaGrafico` / `processNode` (layout vertical)

- **Purpose**: Posicionar nós top-down e arestas verticais via ReactFlow.
- **Location**: `frontend/src/components/OrganogramaGrafico/index.tsx`
- **Alterações**:

| Aspecto | Atual (horizontal) | Novo (vertical) |
| ------- | ------------------ | --------------- |
| Profundidade | `x += levelWidth (350)` | `y += levelHeight (200)` |
| Irmãos | `currentY += maxY + nodeHeight` | `currentX += maxX + nodeGap (300)` |
| Centralização pai | `middleY` no eixo Y | `middleX` no eixo X |
| Raízes múltiplas | empilhadas em Y | empilhadas em Y (subárvore completa abaixo da anterior) |
| Handles | Left / Right | **Top / Bottom** |
| Edge type | `smoothstep` | **`step`** (fallback `smoothstep` se artefato visual) |
| Bounds retorno | `{ minY, maxY }` | `{ minX, maxX, minY, maxY }` — Y inclui altura da subárvore para empilhar raízes |

#### Pseudocódigo `processNode` vertical

```typescript
const LEVEL_HEIGHT = 200;  // distância vertical entre pai e filhos
const SIBLING_GAP = 300;   // distância horizontal entre irmãos
const ROOT_GAP = 80;       // espaço entre subárvores de raízes distintas

function processNode(no, x, y, level): Bounds {
  nodes.push({ id, position: { x, y }, ... });

  if (!no.children?.length) return { minX: x, maxX: x, minY: y, maxY: y };

  let currentX = x;
  let childrenMinX = Infinity, childrenMaxX = -Infinity;
  let childrenMaxY = y;

  for (const child of no.children) {
    const b = processNode(child, currentX, y + LEVEL_HEIGHT, level + 1);
    edges.push({ source: parent, target: child, type: 'step', ... });
    childrenMinX = min(childrenMinX, b.minX);
    childrenMaxX = max(childrenMaxX, b.maxX);
    childrenMaxY = max(childrenMaxY, b.maxY);
    currentX = b.maxX + SIBLING_GAP;
  }

  node.position.x = (childrenMinX + childrenMaxX) / 2; // centralizar pai
  return {
    minX: min(x, childrenMinX),
    maxX: max(x, childrenMaxX),
    minY: y,
    maxY: childrenMaxY,
  };
}

// Raízes
let currentRootY = 0;
for (const root of nos) {
  const b = processNode(root, 0, currentRootY, 0);
  currentRootY = b.maxY + LEVEL_HEIGHT + ROOT_GAP;
}
```

#### Handles e arestas

```typescript
<Handle type="target" position={Position.Top} id="target" />
<Handle type="source" position={Position.Bottom} id="source" />
```

```typescript
defaultEdgeOptions={{
  type: 'step',
  animated: false,
  style: { strokeWidth: 3, stroke: '#1976d2' },
}}
// markerEnd opcional — manter ArrowClosed para direção pai→filho (ORG-LIN-08)
```

**Nota ReactFlow:** `step` edges com handles Top/Bottom produzem traçado em “L” (vertical + horizontal), adequado a organograma corporativo.

---

## Data Models

Nenhum model novo. Props visuais locais apenas:

```typescript
interface OrganogramaTreeContext {
  hasParent: boolean;
  isLastSibling: boolean;
}
```

**Relationships**: Derivado em runtime do índice no array `no.children`; não persiste.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| -------------- | -------- | ----------- |
| Árvore vazia | Empty state existente | Nenhuma linha renderizada |
| Nó sem filhos | Sem container de filhos / sem arestas | Normal |
| Card expande | Linha fixa em 28px | Linha visível; leve offset vs centro expandido |
| ReactFlow `step` artefato | Fallback para `smoothstep` no Execute | Aresta ainda visível |
| Build/lint failure | Gate bloqueia commit da task | Corrigir antes de merge |

---

## Risks & Concerns

| Concern | Location | Impact | Mitigation |
| ------- | -------- | ------ | ---------- |
| **Arquivo monolítico** — `Organograma/index.tsx` ~1050 linhas | `pages/Organograma/index.tsx` | Difícil manutenção | Wrapper `OrganogramaTreeBranch` isolado no topo do arquivo; sem extrair para `features/` (fora de escopo) |
| **Linha desalinhada em card expandido** | Tree branch CSS | Conector não no centro vertical do card expandido | Aceitar offset fixo em 28px (spec trade-off); linha permanece visível |
| **useMemo deps incompletas** no Gráfico | `OrganogramaGrafico/index.tsx:405` | Nós não re-renderizam expand state até remount | **Fora de escopo** — bug pré-existente; incluir `expandedNodeId`/`hoveredNodeId` nas deps só se necessário para validar ORG-LIN-04 no Gráfico |
| **Zero testes FE** | `TESTING.md` | Regressão só via lint/build + UAT manual | Execute: lint + build gate; Verifier: checklist manual por AC |
| **DnD hit area** | `Card` ref | Linhas não devem capturar pointer events | `pointerEvents: 'none'` nos pseudo-elementos; droppable só no Card |
| **console.log em produção** | Ambos arquivos | Ruído no console | **Opcional P3** — remover logs existentes se tocados no diff; não obrigatório pelo spec |

---

## Tech Decisions

| Decision | Choice | Rationale |
| -------- | ------ | --------- |
| Onde colocar CSS da árvore | Componente co-locado `OrganogramaTreeBranch` no mesmo arquivo | Spec limita a 2 arquivos; evita pasta nova |
| Alinhamento do ramo horizontal | Fixo `28px` (centro card compacto) | Sem `ref`/ResizeObserver; DnD estável |
| Cor/espessura linhas Lista | `divider` / 2px | Assunção spec; discreto vs borda primary do card |
| Layout Gráfico | Inverter eixos no `processNode` existente | Reutiliza algoritmo provado; sem dagre/elk |
| Tipo de aresta Gráfico | `step` primário | Organograma clássico; fallback `smoothstep` |
| Handles Gráfico | Top (target) + Bottom (source) | Fluxo pai→filho vertical |
| Setas nas arestas | Manter `markerEnd: ArrowClosed` | Reforça direção hierárquica |
| Extração para `src/features/` | **Não fazer** | Explicitamente out of scope no spec |
| Backend | **Não tocar** | Spec + AD-002 (frontend/backend separados) |

> Decisões feature-local — **nenhum AD-NNN novo** necessário em `STATE.md`.

---

## Requirement Mapping (Design → Implementação)

| Req ID | Componente | Implementação |
| ------ | ---------- | ------------- |
| ORG-LIN-01…06 | `OrganogramaTreeBranch` + `NoOrganogramaCard` | CSS tree + DnD inalterado |
| ORG-LIN-07…12 | `OrganogramaGrafico.processNode` + handles | Layout vertical + edges step |
| ORG-LIN-13…15 | Page + gate | Estado preservado; lint/build; seed manual |

---

## Execute Preview (Tasks implícitos — ≤6 passos)

1. **T1** — Criar `OrganogramaTreeBranch` + tokens CSS; integrar em `NoOrganogramaCard` com `treeContext` → gate lint
2. **T2** — Passar `isLastSibling`/`hasParent` na recursão de filhos e raízes → gate lint
3. **T3** — Rotacionar `processNode` para layout vertical; ajustar empilhamento de raízes → gate lint
4. **T4** — Handles Top/Bottom + edges `step` + `defaultEdgeOptions` → gate lint
5. **T5** — `npm run build` frontend + validação manual Lista (3+ níveis, DnD, expand)
6. **T6** — Validação manual Gráfico (top-down, zoom/pan, CRUD) → Verifier

**Batch único** (~6 tasks) — Execute inline, sem sub-agents.

---

## Aprovação

Após aprovação deste design → **Execute** (Tasks implícitos, sem `tasks.md` formal).
