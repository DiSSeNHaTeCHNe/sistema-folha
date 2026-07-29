# Organograma — Linhas de Hierarquia Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/organograma-linhas-hierarquia/design.md`  
**Spec**: `_docs/specs/features/organograma-linhas-hierarquia/spec.md`  
**Status**: Execute complete — T1–T6 done (commits 573dcd7…616cee6)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute.  
> Guidelines found: `frontend/AGENTS.md` (Vitest target, **não configurado** em `package.json`), `_docs/specs/TESTING.md`, spec Out of Scope (sem E2E nesta entrega).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| `OrganogramaTreeBranch` + `NoOrganogramaCard` (modo Lista) | none | Manual UAT: ACs ORG-LIN-01…06 + edge cases Lista em `validation.md` | `frontend/src/pages/Organograma/index.tsx` | build gate |
| `OrganogramaGrafico` / `processNode` (modo Gráfico) | none | Manual UAT: ACs ORG-LIN-07…12 + edge cases Gráfico em `validation.md` | `frontend/src/components/OrganogramaGrafico/index.tsx` | build gate |
| Backend / API | none | N/A — fora de escopo | — | — |
| Cross-mode (P2) | none | Manual UAT: ORG-LIN-13…15; lint/build automatizado | ambos arquivos | build gate |

**Nota:** Strong default (1:1 AC → teste automatizado) **não aplicável** nesta entrega — repo FE sem `npm run test`; spec explicita validação manual + lint/build. Verifier documenta evidência por AC em `validation.md`.

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1–T4 (alterações TSX) | `cd frontend && npm run lint` |
| Build | Após T5 e fechamento da feature | `cd frontend && npm run lint && npm run build` |
| Full | N/A (sem testes automatizados FE) | — |

---

## Execution Plan

Phases ordered sequentially. **6 tasks → 1 batch inline** (≤ ~8 tasks; sem sub-agents).

### Phase 1: Modo Lista — conectores CSS

```
T1 → T2
```

### Phase 2: Modo Gráfico — layout vertical

```
T3 → T4
```

### Phase 3: Gate + evidência

```
T5 → T6
```

---

## Task Breakdown

### T1: Criar `OrganogramaTreeBranch` e tipos `OrganogramaTreeContext`

**What**: Componente wrapper co-locado com pseudo-elementos CSS (`divider`, 2px, `TREE_BRANCH_Y=28px`) e `pointerEvents: 'none'` nas linhas.  
**Where**: `frontend/src/pages/Organograma/index.tsx` (acima de `NoOrganogramaCard`)  
**Depends on**: None  
**Reuses**: MUI `Box` + `sx`; tokens do `design.md`  
**Requirements**: ORG-LIN-01 (base visual)

**Tools**:

- MCP: NONE
- Skill: `component-architecture` (acessibilidade: linhas decorativas, sem role interativo)

**Done when**:

- [x] `OrganogramaTreeContext` e `OrganogramaTreeBranchProps` definidos conforme design
- [x] Com `hasParent: true`, renderiza `::before` (vertical) e `::after` (horizontal)
- [x] Com `isLastSibling: true`, vertical termina em `TREE_BRANCH_Y` (não prolonga abaixo)
- [x] Com `hasParent: false` / `treeContext` omitido, sem pseudo-elementos (raiz)
- [x] Pseudo-elementos com `pointerEvents: 'none'`
- [x] Gate check passes: `cd frontend && npm run lint`

**Tests**: none  
**Gate**: quick

**Commit**: `feat(organograma): add OrganogramaTreeBranch CSS tree connectors`

---

### T2: Integrar árvore em `NoOrganogramaCard` (recursão + DnD inalterado)

**What**: Envolver cada nó em `OrganogramaTreeBranch`; passar `treeContext` na recursão de filhos; substituir `ml={4}` pelo container do design; **manter `ref={setNodeRef}` só no `Card`**.  
**Where**: `frontend/src/pages/Organograma/index.tsx` (`NoOrganogramaCard` + `nos.map` raízes)  
**Depends on**: T1  
**Reuses**: handlers e `useDroppable` existentes — **zero alteração**  
**Requirements**: ORG-LIN-01, ORG-LIN-02, ORG-LIN-03, ORG-LIN-04, ORG-LIN-05, ORG-LIN-06

**Tools**:

- MCP: NONE
- Skill: `forms-validation` N/A; `component-architecture`

**Done when**:

- [x] Filhos recebem `{ hasParent: true, isLastSibling: index === length - 1 }`
- [x] Raízes top-level sem `treeContext` (ORG-LIN-03)
- [x] Múltiplos filhos compartilham vertical; último irmão corta linha (ORG-LIN-02)
- [x] Expand/hover: linhas permanecem visíveis com ramo em 28px (ORG-LIN-04)
- [x] `useDroppable` id `no-${no.id}` inalterado no `Card`; `isOver` highlight funciona (ORG-LIN-05)
- [x] Handlers `onEdit`, `onDelete`, `onAddChild`, `onRemove*` não modificados (ORG-LIN-06)
- [x] Gate check passes: `cd frontend && npm run lint`

**Tests**: none  
**Gate**: quick

**Commit**: `feat(organograma): wire tree connectors into list mode cards`

---

### T3: Layout vertical top-down em `processNode`

**What**: Inverter eixos do algoritmo: profundidade em Y (`LEVEL_HEIGHT=200`), irmãos em X (`SIBLING_GAP=300`), centralizar pai em `middleX`, empilhar raízes com `ROOT_GAP=80`.  
**Where**: `frontend/src/components/OrganogramaGrafico/index.tsx` (`useMemo` / `processNode`)  
**Depends on**: None *(paralelo conceitual com Phase 1; sequencial no Execute após T2)*  
**Reuses**: algoritmo recursivo existente; apenas troca de eixos  
**Requirements**: ORG-LIN-07, ORG-LIN-09, ORG-LIN-12

**Tools**:

- MCP: `plugin-context7-plugin-context7` (ReactFlow layout, se dúvida)
- Skill: NONE

**Done when**:

- [x] Raízes no topo; filhos com Y crescente (ORG-LIN-07)
- [x] Pai centralizado horizontalmente entre filhos (ORG-LIN-09)
- [x] Múltiplas raízes empilhadas verticalmente sem sobreposição (ORG-LIN-12)
- [x] Nó único sem filhos: sem arestas (edge case spec)
- [x] Gate check passes: `cd frontend && npm run lint`

**Tests**: none  
**Gate**: quick

**Commit**: `feat(organograma): vertical top-down layout in graph mode`

---

### T4: Handles Top/Bottom e arestas `step`

**What**: Trocar `Position.Left/Right` → `Top/Bottom` em `NoOrganogramaNode`; edges `type: 'step'`; `defaultEdgeOptions` alinhado; manter `markerEnd: ArrowClosed` e estilo `#1976d2`.  
**Where**: `frontend/src/components/OrganogramaGrafico/index.tsx`  
**Depends on**: T3  
**Reuses**: ReactFlow `Handle`, `MarkerType`, edges push existente  
**Requirements**: ORG-LIN-08, ORG-LIN-10, ORG-LIN-11

**Tools**:

- MCP: `plugin-context7-plugin-context7` (ReactFlow step edges)
- Skill: NONE

**Done when**:

- [x] Arestas entram pelo topo do filho e saem pela base do pai (ORG-LIN-08)
- [x] `Background`, `Controls`, `MiniMap`, `fitView` inalterados (ORG-LIN-10)
- [x] Botões editar/excluir/adicionar filho e expand/hover inalterados (ORG-LIN-11)
- [x] Se `step` gerar artefato visual, fallback documentado no commit message para `smoothstep`
- [x] Gate check passes: `cd frontend && npm run lint`

**Tests**: none  
**Gate**: quick

**Commit**: `feat(organograma): top-bottom handles and step edges in graph mode`

---

### T5: Build gate frontend

**What**: Garantir compilação TypeScript + Vite sem erros novos.  
**Where**: `frontend/` (gate only)  
**Depends on**: T2, T4  
**Reuses**: scripts existentes  
**Requirements**: ORG-LIN-14

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `cd frontend && npm run lint` — exit 0
- [x] `cd frontend && npm run build` — exit 0
- [x] Nenhum arquivo fora de escopo alterado (apenas os 2 arquivos da feature + docs spec)

**Tests**: none  
**Gate**: build

**Commit**: `chore(organograma): verify lint and build for hierarchy lines feature`

---

### T6: Manual UAT + preparação Verifier

**What**: Executar checklist manual contra todos os ACs; registrar evidências iniciais para o Verifier em `validation.md` (rascunho PASS/FAIL por AC).  
**Where**: `_docs/specs/features/organograma-linhas-hierarquia/validation.md` (criar)  
**Depends on**: T5  
**Reuses**: seed `_docs/organograma/seed-organograma-2026.sql` se ambiente disponível  
**Requirements**: ORG-LIN-01…15 (evidência manual)

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven` → `validate.md` (Verifier fresh-eyes após este commit)

**Done when**:

- [x] Checklist Lista: 3+ níveis, múltiplos filhos, último irmão, múltiplas raízes, expand, DnD, CRUD
- [x] Checklist Gráfico: top-down, zoom/pan/minimap, CRUD, múltiplas raízes, nó isolado
- [x] Checklist P2: alternar modos preserva `expandedNodeId`; viewport ≥768px legível; 5+ níveis
- [x] `validation.md` criado com tabela per-AC (evidência ou FAIL explícito)
- [x] Gate check passes: `cd frontend && npm run lint && npm run build`

**Tests**: none (manual UAT documentado)  
**Gate**: build

**Commit**: `docs(organograma): add manual UAT evidence for hierarchy lines`

> **Pós-T6:** Verifier sub-agent roda automaticamente (author ≠ verifier) → atualiza `validation.md` final.

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5 ──→ T6 ──→ [Verifier automático]

Batch único (6 tasks) — Worker 1 complete (573dcd7…616cee6).
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: OrganogramaTreeBranch + tipos | 1 componente co-locado | ✅ Granular |
| T2: Integração NoOrganogramaCard | 1 componente (modify wiring) | ✅ Granular |
| T3: processNode vertical | 1 algoritmo / 1 arquivo | ✅ Granular |
| T4: Handles + edges | 1 arquivo, 1 concern visual | ✅ Granular |
| T5: Build gate | gate only | ✅ Granular |
| T6: Manual UAT + validation.md | 1 doc artefato | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | entrada Phase 1 | ✅ Match |
| T2 | T1 | após T1 | ✅ Match |
| T3 | None* | entrada Phase 2 após T2 | ✅ Match |
| T4 | T3 | após T3 | ✅ Match |
| T5 | T2, T4 | após T2 e T4 | ✅ Match |
| T6 | T5 | após T5 | ✅ Match |

\*T3 não depende de T2 no código (arquivos distintos), mas Execute segue ordem de fases: T2 antes de T3.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | UI page wrapper | none | none | ✅ OK |
| T2 | UI page card | none | none | ✅ OK |
| T3 | UI graph layout | none | none | ✅ OK |
| T4 | UI graph handles/edges | none | none | ✅ OK |
| T5 | gate only | none | none | ✅ OK |
| T6 | validation doc | none | none (manual UAT) | ✅ OK |

---

## Requirement → Task Map

| Requirement ID | Task(s) |
| -------------- | ------- |
| ORG-LIN-01 | T1, T2 |
| ORG-LIN-02 | T1, T2 |
| ORG-LIN-03 | T2 |
| ORG-LIN-04 | T2 |
| ORG-LIN-05 | T2 |
| ORG-LIN-06 | T2 |
| ORG-LIN-07 | T3 |
| ORG-LIN-08 | T4 |
| ORG-LIN-09 | T3 |
| ORG-LIN-10 | T4 |
| ORG-LIN-11 | T4 |
| ORG-LIN-12 | T3 |
| ORG-LIN-13 | T6 |
| ORG-LIN-14 | T5 |
| ORG-LIN-15 | T6 |

**Coverage:** 15/15 requirements mapped ✓

---

## Tools Recommendation (confirm before Execute)

| Task | MCP | Skill |
| ---- | --- | ----- |
| T1–T2 | NONE | `component-architecture` |
| T3–T4 | Context7 (ReactFlow, se necessário) | NONE |
| T5–T6 | NONE | `tlc-spec-driven` (Verifier) |

**Pergunta para o usuário antes do Execute:**

> Para cada task, confirma as ferramentas acima ou prefere ajustar MCPs/skills?
