# Organograma — Linhas de Hierarquia Validation

## Status atual

**Verdict**: PASS ✅  
**Spec slug**: organograma-linhas-hierarquia  
**HEAD commit**: `260aeeb7d9f75b8b9f4c63a091fe1476f2e53de1`  
**Open gaps** (ranked):

1. **P1 — Browser UAT not executed**: Visual/interaction ACs (ORG-LIN-01…06, 07…12, 13, 15) verified by code inspection + gate only; manual checklist in Author section remains unchecked.
2. **P2 — ORG-LIN-04 design trade-off**: Branch Y fixed at 28px (`TREE_BRANCH_Y`); expanded cards (~200px) may not center connector — accepted per spec assumption, not browser-validated.
3. **P3 — No automated FE tests**: Per tasks.md Test Coverage Matrix; discrimination sensor used as lightweight substitute.

**Closed in fix cycle 1**: `aria-hidden` on `OrganogramaTreeBranch` wrapper (would hide interactive Card descendants from AT) — removed at `260aeeb`.

---

## Execução: organograma-linhas-hierarquia — fix cycle 1 — 2026-07-29 — 616cee6..260aeeb

**Verifier**: Fresh-eyes sub-agent (author ≠ verifier)  
**Range**: `616cee69e984027ef53990c57f6cbc9d92c98aef..260aeeb7d9f75b8b9f4c63a091fe1476f2e53de1`  
**Verdict**: **PASS ✅** — aria-hidden a11y fix verified; gate re-run at HEAD; 15/15 ACs unchanged (static).

### Fix verification

| Item | Before (`616cee6`) | After (`260aeeb`) | Verdict |
| ---- | ------------------ | ----------------- | ------- |
| `OrganogramaTreeBranch` wrapper | `aria-hidden={hasParent ? true : undefined}` on `<Box>` hid all descendants (incl. interactive Card) from AT | Prop removed; wrapper is structural only | **PASS** |
| Decorative lines | `pointerEvents: 'none'` on `::before`/`::after` (`Organograma/index.tsx:116,126`) | Unchanged | **PASS** |
| `aria-hidden` in Organograma page | Present on branch wrapper | Grep: 0 matches under `frontend/src/pages/Organograma/` | **PASS** |

**Evidence**: `git show 260aeeb` — 1-line deletion at `Organograma/index.tsx:103`; current `OrganogramaTreeBranch` (`Organograma/index.tsx:95-133`) renders `<Box sx={...}>` without `aria-hidden`; comment at `:94` documents decorative-only role.

### Gate results (re-run at HEAD `260aeeb`)

| Gate | Command | Result | Notes |
| ---- | ------- | ------ | ----- |
| Lint | `cd frontend && npm run lint` | PASS | 0 errors; 8 warnings (pre-existing) |
| Build | `cd frontend && npm run build` | PASS | `tsc -b && vite build` completed in ~4.3s |

### Per-AC delta

No AC regression. All 15 ACs retain prior static PASS from `0ff24d0..616cee6` run; fix is a11y-only (wrapper no longer suppresses Card buttons/links for screen readers).

### Gaps ranked (unchanged except closed item)

1. Browser UAT not run — visual/interaction ACs rely on static evidence only.
2. ORG-LIN-04 branch alignment at fixed 28px when card expands — design trade-off, unverified in browser.
3. No Vitest/E2E coverage — sensor supplements but does not replace runtime visual validation.

---

## Execução: organograma-linhas-hierarquia — 2026-07-29 — 0ff24d0..616cee6

**Verifier**: Fresh-eyes sub-agent (author ≠ verifier)  
**Range**: `0ff24d0079dd14e19e05b268a06034631e4e5640..616cee69e984027ef53990c57f6cbc9d92c98aef`  
**Verdict**: **PASS ✅** — 15/15 ACs pass static spec-anchored check + build gate; browser UAT deferred (gap #1).

### Per-AC evidence

| AC ID | Verdict | Evidence (file:line + assertion) |
| ----- | ------- | -------------------------------- |
| ORG-LIN-01 | PASS (static) | `Organograma/index.tsx:107-128` — when `hasParent`, `::before` draws vertical `borderLeft` 2px + `::after` draws horizontal `borderTop` at `TREE_BRANCH_Y`; `347-350` passes `hasParent: true` to each child |
| ORG-LIN-02 | PASS (static) | `Organograma/index.tsx:114` — `height: isLastSibling ? \`${TREE_BRANCH_Y}px\` : '100%'` shares vertical across siblings; `349` — `isLastSibling: index === no.children.length - 1` stops vertical at last branch |
| ORG-LIN-03 | PASS (static) | `Organograma/index.tsx:912-925` — top-level `nos.map` renders roots without `treeContext`; each root is independent subtree, no cross-root pseudo-elements |
| ORG-LIN-04 | PASS (static) | `Organograma/index.tsx:79,123,190` — connectors always rendered when `hasParent`; expand/hover toggles `showDetails` on Card only; lines use fixed 28px branch (spec assumption) |
| ORG-LIN-05 | PASS (static) | `Organograma/index.tsx:164-169,182` — `useDroppable` id `no-${no.id}` + `ref={setNodeRef}` on Card; `117,127` — `pointerEvents: 'none'` on decorative pseudo-elements |
| ORG-LIN-06 | PASS (static) | `Organograma/index.tsx:227-247,338-341` — `onEdit`, `onDelete`, `onAddChild`, `onRemove*` handlers passed through unchanged from parent |
| ORG-LIN-07 | PASS (static) | `OrganogramaGrafico/index.tsx:314-316,355,397-400` — `LEVEL_HEIGHT=200`; children placed at `y + LEVEL_HEIGHT`; roots stacked via `currentRootY` (Y grows downward) |
| ORG-LIN-08 | PASS (static) | `OrganogramaGrafico/index.tsx:75-78` — target `Position.Top`; `278-281` — source `Position.Bottom`; `359-375` — edges `type: 'step'`, `#1976d2`, `strokeWidth: 3` |
| ORG-LIN-09 | PASS (static) | `OrganogramaGrafico/index.tsx:383-386` — `middleX = (childrenMinX + childrenMaxX) / 2`; parent node repositioned to center over children |
| ORG-LIN-10 | PASS (static) | `OrganogramaGrafico/index.tsx:429-447` — `fitView`, `minZoom={0.1}`, `maxZoom={2}`, `Background`, `Controls`, `MiniMap` present and unchanged |
| ORG-LIN-11 | PASS (static) | `OrganogramaGrafico/index.tsx:113-137,168-177,88-90` — Add/Edit/Delete IconButtons + expand/hover on Card; handlers via node `data` |
| ORG-LIN-12 | PASS (static) | `OrganogramaGrafico/index.tsx:397-400` — `currentRootY = result.maxY + LEVEL_HEIGHT + ROOT_GAP` (ROOT_GAP=80) stacks multiple root subtrees vertically |
| ORG-LIN-13 | PASS (static) | `Organograma/index.tsx:422,773-775,826-828,880-883,921-924` — single `expandedNodeId` state; `setViewMode` does not reset expand state; shared by Lista and Gráfico |
| ORG-LIN-14 | PASS | Verifier gate: `cd frontend && npm run lint && npm run build` — exit 0 (0 errors, 8 pre-existing warnings) at HEAD `616cee6` |
| ORG-LIN-15 | PASS (static) | Lista: `Organograma/index.tsx:333` — `ml={4}` recursive indent enables scroll; Gráfico: `OrganogramaGrafico/index.tsx:432` — `minZoom={0.1}` allows zoom-out for deep trees |

### Gate results

| Gate | Command | Result | Notes |
| ---- | ------- | ------ | ----- |
| Lint | `cd frontend && npm run lint` | PASS | 0 errors; 8 warnings (pre-existing, incl. `OrganogramaGrafico/index.tsx:409` exhaustive-deps) |
| Build | `cd frontend && npm run build` | PASS | `tsc -b && vite build` completed in ~4.3s |

### Discrimination sensor (3 mutations, scratch + revert)

| # | Mutation | Target AC | Result | Rationale |
| - | -------- | --------- | ------ | --------- |
| 1 | `TREE_BRANCH_Y = 28` → `999` | ORG-LIN-02, ORG-LIN-04 | **KILLED** | Horizontal branch at `top: 999px` misaligns connector; last-sibling vertical extends to 999px instead of card center |
| 2 | `isLastSibling: index === length - 1` → `!==` | ORG-LIN-02 | **KILLED** | Inverts tree-view rule: non-last siblings truncate vertical at 28px; last sibling prolongs vertical below branch |
| 3 | `Position.Top/Bottom` → `Left/Right` | ORG-LIN-08 | **KILLED** | Edges connect horizontally (Left/Right handles) instead of top-down Top→Bottom |

**Summary**: 3 mutations, **3 killed**, **0 survived**.

### Gaps ranked

1. Browser UAT not run — visual/interaction ACs rely on static evidence only (see Author checklist below).
2. ORG-LIN-04 branch alignment at fixed 28px when card expands — design trade-off, unverified in browser.
3. No Vitest/E2E coverage — sensor supplements but does not replace runtime visual validation.

---

## Execução: Author (T6 — Batch Worker 1)

**Feature**: `organograma-linhas-hierarquia`  
**Author**: Batch Worker 1 (Execute)  
**Verifier**: Superseded by fresh-eyes run above  
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
