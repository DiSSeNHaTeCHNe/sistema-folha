# Organograma — Linhas de Hierarquia Validation

**Feature**: `organograma-linhas-hierarquia`  
**Author**: Batch Worker 1 (Execute)  
**Verifier**: Pending (fresh-eyes review)  
**Validation method**: Code inspection + static analysis (lint/build gates). No browser UAT in CI.

---

## Gate Results

| Gate | Command | Result | Commit |
| ---- | ------- | ------ | ------ |
| Quick (T1–T4) | `cd frontend && npm run lint` | PASS (0 errors) | T1–T4 |
| Build (T5–T6) | `cd frontend && npm run lint && npm run build` | PASS | T5, T6 |

---

## Per-AC Evidence (Code Inspection)

| AC ID | Requirement | Status | Evidence (file:line) | Notes |
| ----- | ----------- | ------ | -------------------- | ----- |
| ORG-LIN-01 | Lista — conector pai-filho (vertical + horizontal) | PASS (static) | `Organograma/index.tsx:109-128` — `::before` vertical + `::after` horizontal when `hasParent`; `347-350` passes `hasParent: true` to children | Browser UAT pending Verifier |
| ORG-LIN-02 | Lista — múltiplos filhos; último irmão corta vertical | PASS (static) | `Organograma/index.tsx:114` — `height: isLastSibling ? TREE_BRANCH_Y : '100%'`; `349` — `isLastSibling: index === length - 1` | Shared vertical via sibling stack; last sibling stops at 28px |
| ORG-LIN-03 | Lista — múltiplas raízes independentes | PASS (static) | `Organograma/index.tsx:912-925` — top-level `nos.map` without `treeContext`; each root is separate `NoOrganogramaCard` subtree | No cross-root connectors |
| ORG-LIN-04 | Lista — expand/hover; linhas visíveis em 28px | PASS (static) | `Organograma/index.tsx:79,123` — `TREE_BRANCH_Y=28` fixed branch; `172-174,185` — expand/hover handlers unchanged on Card | Trade-off: branch fixed at compact center per design |
| ORG-LIN-05 | Lista — DnD inalterado | PASS (static) | `Organograma/index.tsx:164-169` — `useDroppable({ id: \`no-${no.id}\` })`; `182` — `ref={setNodeRef}` on Card only; `117,127` — `pointerEvents: 'none'` on pseudo-elements | Drop target not on decorative lines |
| ORG-LIN-06 | Lista — CRUD handlers inalterados | PASS (static) | `Organograma/index.tsx:228-247,338-341` — `onEdit`, `onDelete`, `onAddChild`, `onRemove*` passed through unchanged | No handler modifications |
| ORG-LIN-07 | Gráfico — layout top-down (Y cresce) | PASS (static) | `OrganogramaGrafico/index.tsx:314-316,355` — `LEVEL_HEIGHT=200`, children at `y + LEVEL_HEIGHT`; `397-400` — roots stacked by `currentRootY` | Roots at top, children below |
| ORG-LIN-08 | Gráfico — arestas Top/Bottom pai→filho | PASS (static) | `OrganogramaGrafico/index.tsx:75-78` — target `Position.Top`; `278-281` — source `Position.Bottom`; `359-375` — edges with `markerEnd: ArrowClosed`, `#1976d2` | Edge type `step` |
| ORG-LIN-09 | Gráfico — pai centralizado entre filhos | PASS (static) | `OrganogramaGrafico/index.tsx:383-386` — `middleX = (childrenMinX + childrenMaxX) / 2` | Same centering rule, X axis |
| ORG-LIN-10 | Gráfico — zoom/pan/minimap/fitView | PASS (static) | `OrganogramaGrafico/index.tsx:429-447` — `fitView`, `minZoom`, `maxZoom`, `Background`, `Controls`, `MiniMap` unchanged | No regression in ReactFlow chrome |
| ORG-LIN-11 | Gráfico — ações de nó inalteradas | PASS (static) | `OrganogramaGrafico/index.tsx:113-136,168-176` — Add/Edit/Delete IconButtons; `88-90` — expand/hover on Card | Handlers passed via node `data` |
| ORG-LIN-12 | Gráfico — múltiplas raízes sem sobreposição | PASS (static) | `OrganogramaGrafico/index.tsx:397-400` — `currentRootY = result.maxY + LEVEL_HEIGHT + ROOT_GAP` (ROOT_GAP=80) | Subtrees stacked vertically |
| ORG-LIN-13 | P2 — `expandedNodeId` preservado ao trocar modo | PASS (static) | `Organograma/index.tsx:422,823-828,880-883,921-924` — single `expandedNodeId` state; passed to both Lista and Gráfico | `viewMode` toggle does not reset expand state |
| ORG-LIN-14 | P2 — lint/build sem regressão | PASS | T5 gate: `npm run lint` exit 0, `npm run build` exit 0 | Commit `fd622ad` |
| ORG-LIN-15 | P2 — hierarquia profunda legível | PASS (static) | Lista: `Organograma/index.tsx:333` — `ml={4}` indent + recursive render; Gráfico: `minZoom={0.1}` at `OrganogramaGrafico/index.tsx:432` | Scroll (Lista) / zoom out (Gráfico) |

---

## Manual UAT Checklists (for Verifier / browser)

### Modo Lista

- [ ] 3+ níveis hierárquicos exibem conectores visíveis
- [ ] Múltiplos filhos do mesmo pai compartilham vertical; último irmão sem prolongamento
- [ ] Múltiplas raízes: conectores isolados por subárvore
- [ ] Expandir nó: linhas permanecem visíveis (ramo em ~28px)
- [ ] Hover nó: linhas permanecem visíveis
- [ ] Drag funcionário/centro de custo → drop em nó: toast sucesso, highlight `isOver`
- [ ] Editar / excluir / adicionar filho: dialogs e comportamento idênticos

### Modo Gráfico

- [ ] Raízes no topo; filhos abaixo conectados por linhas step
- [ ] Zoom (scroll), pan (arrastar), minimap, fit view funcionam
- [ ] Editar / excluir / adicionar filho nos nós do gráfico
- [ ] Múltiplas raízes empilhadas sem sobreposição
- [ ] Nó raiz isolado (sem filhos): sem arestas

### P2 (cross-mode)

- [ ] Alternar Lista ↔ Gráfico com nó expandido: estado preservado
- [ ] Viewport ≥768px: linhas legíveis, texto dos cards não oculto
- [ ] Organograma 5+ níveis: scroll (Lista) ou zoom out (Gráfico) suficiente

---

## Edge Cases (static)

| Case | Expected | Evidence |
| ---- | -------- | -------- |
| Árvore vazia | Empty state, sem linhas | `Organograma/index.tsx:898-910` |
| Nó sem filhos | Sem container filhos / sem arestas | `Organograma/index.tsx:332`; `OrganogramaGrafico/index.tsx:342-344` |
| Card expandido | Ramo fixo 28px | `TREE_BRANCH_Y` design trade-off |

---

## Deviations / Blockers

None identified in Execute. Verifier may upgrade static PASS to browser-verified PASS/FAIL.
