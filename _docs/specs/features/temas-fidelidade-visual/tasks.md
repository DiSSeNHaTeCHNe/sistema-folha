# Fidelidade Visual dos Temas — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Spec**: `_docs/specs/features/temas-fidelidade-visual/spec.md`
**Context**: `_docs/specs/features/temas-fidelidade-visual/context.md`
**Design**: `_docs/specs/features/temas-fidelidade-visual/design.md`
**Estudo de origem**: `_docs/estudo-visual/aproximacao-mockups.md` (+ `.pdf` com as capturas lado a lado)
**Referência visual**: `_docs/estudo-visual/propostas-visual-sistema-folha.pdf`, `capturas-implementado/`
**Feature anterior**: `_docs/specs/features/temas-visuais/` (PASS @ `ab2cf01`)
**Status**: Em execução — Fases 1–3 concluídas (batches 1 e 2); Fase 4 (T10) bloqueada por indisponibilidade de captura. Branch `feat/temas-fidelidade-visual`, base `0a0eac7`.
**User preference (project):** commits atômicos por task autorizados nesta execução.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `frontend/AGENTS.md` (stack TARGET, AD-004), `AGENTS.md` raiz, `frontend/vite.config.ts` (Vitest + v8, sem threshold), 41 arquivos de teste existentes, validation de `temas-visuais` (baseline 559 testes). Sem lições confirmadas no store.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Fábrica de temas (`theme/tokens.ts`) | unit (Vitest) | 1:1 com TEMAF-01/04/05/06; tokens semânticos chegam à paleta; escala aplicada; variantes fora de escopo intocadas | `src/theme/tokens.test.ts` | `npm run test -- src/theme/tokens` |
| Registro de temas (`theme/themes.ts`) | unit (Vitest) | 1:1 com TEMAF-02/08; os 5 temas declaram as 4 semânticas; nenhuma igual ao default do MUI, exceto `classico`; escala idêntica entre temas | `src/theme/themes.test.ts` | `npm run test -- src/theme/themes` |
| Contraste (`theme/contraste.ts`) | unit (Vitest) | TEMAF-03; varredura `TEMAS × pares` incluindo os 4 pares semânticos novos contra `background.paper` | `src/theme/contraste.test.ts` | `npm run test -- src/theme/contraste` |
| Escala renderizada (novo) | unit (Vitest + Testing Library) | TEMAF-07; `fontSize` computado de `h3`/`h4`/`h6` sob cada tema; detecta prop vencendo o tema | `src/theme/escalaRenderizada.test.tsx` | `npm run test -- src/theme/escalaRenderizada` |
| Guarda de props (novo) | unit (Vitest) | TEMAF-09/10/11/12; varre os `.tsx` rastreados e falha se houver `fontWeight=`, `color="textSecondary"` ou `color="primary"` em título | `src/theme/noStyleProps.test.ts` | `npm run test -- src/theme/noStyleProps` |
| Páginas alteradas (Dashboard, Folha, Usuários, Organograma, Relatórios, AparenciaDialog) | unit (Vitest + Testing Library) | TEMAF-14; suítes existentes passam sem alteração de asserção; asserções de estilo revistas, nunca deletadas | `src/pages/**/*.test.tsx`, `src/components/**/*.test.tsx` | `npm run test -- src/pages src/components` |
| Config (`eslint.config.js`) | none | Gate de lint e build | — | `npm run lint && npm run build` |

**Baseline**: 559 testes (validation de `temas-visuais`). A contagem final não pode ser menor.

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após task com testes unitários num caminho específico | `cd frontend && npm run test -- <caminho>` |
| Build | Após task só de config ou lint | `cd frontend && npm run lint && npm run build` |
| Full | Após a última task de cada fase e antes do Verifier | `cd frontend && npm run lint && npm run test && npm run build` |

---

## Execution Plan

Quatro fases. As três primeiras são as camadas A, B e B' do estudo, na ordem em
que se habilitam: A é independente, B precisa existir antes de B' fazer sentido,
e B' é o que faz B valer de fato. A quarta é a verificação visual, que só pode
rodar com tudo aplicado.

### Fase 1 — Camada A: cores semânticas (3 tasks)

```
T1 → T2 → T3
```

### Fase 2 — Camada B: escala tipográfica no tema (2 tasks)

```
T4 → T5
```

### Fase 3 — Camada B': devolver peso e cor ao tema (4 tasks)

```
T6 → T7 → T8 → T9
```

### Fase 4 — Verificação visual das 20 telas (1 task)

```
T10
```

---

## Task Breakdown

### T1: [x] Estender `TokensTema` com os papéis semânticos

**What**: Adicionar `success`, `warning`, `error` e `info` ao tipo `TokensTema` e repassá-los à `palette` em `montarTema`.
**Where**: `frontend/src/theme/tokens.ts`
**Depends on**: None
**Reuses**: estrutura de `primary`/`secondary` já presente no mesmo arquivo
**Requirement**: TEMAF-01

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Os quatro campos aceitam `{ main; light?; contrastText? }`
- [ ] `montarTema` repassa os quatro para `palette`, sem valor default embutido na fábrica
- [ ] Teste: um token de exemplo produz `Theme` com `palette.success.main` igual ao informado, e o mesmo para os outros três
- [ ] Teste: quando `light` é informado, ele chega à paleta; quando não é, o MUI deriva (DD-3)
- [ ] Nenhum `any` e nenhum `as` introduzido
- [ ] Gate: `npm run test -- src/theme/tokens`
- [ ] Test count: +6 testes

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): tokens semanticos na fabrica de temas` → **feito** `8b7963c`

---

### T2: [x] Declarar as semânticas nos cinco temas e cobrir contraste

**What**: Preencher `success`, `warning`, `error` e `info` em `corporate`, `soft`, `indigo` e `techne` com os valores da tabela do design, manter o `classico` no default do MUI, e adicionar os quatro pares novos à varredura de contraste.
**Where**: `frontend/src/theme/themes.ts`, `frontend/src/theme/contraste.test.ts`
**Depends on**: T1
**Reuses**: tabela de valores em `design.md` (origem: `_docs/estudo-visual/gerador/gen_mockups.py`, dicionário `THEMES`)
**Requirement**: TEMAF-02, TEMAF-03

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `corporate`, `soft`, `indigo` e `techne` declaram as quatro semânticas conforme a tabela do design
- [ ] `indigo` declara `light` explícito nas quatro (DD-3), evitando fundo de avatar quase branco
- [ ] `classico` reproduz o default do MUI nas quatro (DD-4)
- [ ] Teste: para cada tema exceto `classico`, cada uma das quatro difere do default do MUI
- [ ] Teste: `classico` mantém os defaults
- [ ] Varredura de contraste cobre `{success,warning,error,info}.main` × `background.paper` nos 5 temas e passa em todos
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: +10 asserções de tema +20 de contraste (4 pares × 5 temas)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): cores semanticas por tema nos cinco temas` → **feito** `3143838`

---

### T3: [x] Verificar os avatares do Dashboard sob os cinco temas

**What**: Confirmar que os quatro avatares de KPI e os dois de lista do Dashboard derivam do tema ativo, corrigindo qualquer token que ainda escape.
**Where**: `frontend/src/pages/Dashboard/index.tsx`, `frontend/src/pages/Dashboard/Dashboard.test.tsx`
**Depends on**: T2
**Reuses**: `renderWithProviders({ temaId })`
**Requirement**: TEMAF-02

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Teste parametrizado renderiza o Dashboard sob os cinco temas e asserta que a cor de fundo de cada avatar corresponde ao token do tema ativo
- [ ] Nenhum avatar renderiza com cor de fábrica do MUI em nenhum tema
- [ ] Suíte existente do Dashboard passa sem alteração de asserção
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count: +5 (um por tema); total acumulado ≥ 559 + 41

**Tests**: unit · **Gate**: full
**Commit**: `test(dashboard): avatares derivam do tema em todos os temas` → **feito** `8f4b308 (+ 6a8fe17)`

---

### T4: [x] Aplicar a escala tipográfica em `montarTema`

**What**: Definir `h3`, `h4` e `h6` com os tamanhos e peso da Nota de escala, preservando as demais variantes.
**Where**: `frontend/src/theme/tokens.ts`
**Depends on**: T3
**Reuses**: bloco `typography` que já recebe `fontFamily`
**Requirement**: TEMAF-04, TEMAF-05, TEMAF-06, TEMAF-08

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `h3` = `1.6875rem` / 600; `h4` = `1.5rem` / 600; `h6` = `1rem` / 600
- [ ] `fontFamily` do tema continua sendo respeitado (Poppins no `techne`)
- [ ] Teste: `body1`, `body2`, `subtitle1`, `subtitle2`, `caption`, `h1`, `h2` e `h5` permanecem sem override
- [ ] Teste: a escala é idêntica nos cinco temas (TEMAF-08)
- [ ] Gate: `npm run test -- src/theme/tokens src/theme/themes`
- [ ] Test count: +8

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): escala tipografica dos mockups em montarTema` → **feito** `5af7e26`

---

### T5: [x] Testar a escala renderizada sob cada tema

**What**: Criar o teste que monta `Typography` de cada variante sob cada tema e asserta o `fontSize` computado — a rede que detecta prop vencendo o tema.
**Where**: `frontend/src/theme/escalaRenderizada.test.tsx` (novo)
**Depends on**: T4
**Reuses**: `renderWithProviders({ temaId })`
**Requirement**: TEMAF-07

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Para cada tema, `Typography variant="h4"` computa `24px`; `h3` computa `27px`; `h6` computa `16px`
- [ ] Um caso de controle com `fontWeight="bold"` inline demonstra que a prop vence — documentando a razão da Fase 3 e falhando se alguém reintroduzir
- [ ] Gate: `npm run test -- src/theme/escalaRenderizada`
- [ ] Test count: +16 (3 variantes × 5 temas + 1 controle)

**Tests**: unit · **Gate**: quick
**Commit**: `test(tema): escala renderizada verificada por tema` → **feito** `9007dff`

---

### T6: [x] Remover as props de estilo do Dashboard

**What**: Remover as 16 props `fontWeight` e as 9 props `color="primary"` de títulos do Dashboard, deixando peso e cor virem do tema.
**Where**: `frontend/src/pages/Dashboard/index.tsx`, `Dashboard.test.tsx`
**Depends on**: T5
**Reuses**: tabela de distribuição por arquivo em `design.md`
**Requirement**: TEMAF-09, TEMAF-10, TEMAF-12

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Zero `fontWeight=` no arquivo
- [ ] Zero `color="primary"` em `Typography` com `variant` h1-h6
- [ ] Props semânticas (`success.main`, `warning.main`, `info.main`, `error`) preservadas — TEMAF-12
- [ ] Suíte do Dashboard passa; asserções de estilo revistas, nenhuma deletada
- [ ] Gate: `npm run test -- src/pages/Dashboard`

**Tests**: unit · **Gate**: quick
**Commit**: `refactor(dashboard): peso e cor de texto vindos do tema` → **feito** `341c3b5`

---

### T7: [x] Remover as props de estilo das demais telas

**What**: Remover as 8 props `fontWeight` restantes e 1 `color="primary"` de título nos sete arquivos que sobram.
**Where**: `pages/FolhaPagamento/index.tsx` (3), `components/OrganogramaGrafico/index.tsx` (2), `pages/Usuarios/index.tsx` (1+1), `pages/Relatorios/index.tsx` (1), `pages/Relatorios/RelatorioCatalogCard.tsx` (1), `pages/Organograma/index.tsx` (1), `components/AparenciaDialog/index.tsx` (1)
**Depends on**: T6
**Reuses**: mesma transformação mecânica de T6
**Requirement**: TEMAF-09, TEMAF-10, TEMAF-12

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `git grep -c 'fontWeight=' -- 'frontend/src/**/*.tsx'` retorna 0
- [ ] `color="primary"` em título removido de `Usuarios`
- [ ] As 4 ocorrências de `color="primary"` que não são título foram avaliadas e a decisão registrada no commit
- [ ] Suítes das telas afetadas passam sem alteração de asserção
- [ ] Gate: `npm run test -- src/pages src/components`

**Tests**: unit · **Gate**: quick
**Commit**: `refactor(telas): remove props de peso e cor de texto` → **feito** `a54967d`

---

### T8: [x] Normalizar `color="textSecondary"`

**What**: Substituir as 18 ocorrências da forma depreciada do MUI v4 pela forma atual `color="text.secondary"`.
**Where**: arquivos `.tsx` rastreados em `src/pages/` e `src/components/`
**Depends on**: T7
**Reuses**: —
**Requirement**: TEMAF-11

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `git grep -c 'color="textSecondary"' -- 'frontend/src/**/*.tsx'` retorna 0
- [ ] Nenhuma mudança visual — as duas formas resolvem para o mesmo token
- [ ] Suítes passam sem alteração de asserção
- [ ] Gate: `npm run test -- src/pages src/components`

**Tests**: unit · **Gate**: quick
**Commit**: `refactor(telas): normaliza textSecondary para text.secondary` → **feito** `c684967`

---

### T9: [x] Ativar o lint e a guarda contra props de estilo

**What**: Adicionar a regra `no-restricted-syntax` contra `fontWeight` em prop e criar o teste que varre os `.tsx` rastreados.
**Where**: `frontend/eslint.config.js`, `frontend/src/theme/noStyleProps.test.ts` (novo)
**Depends on**: T8
**Reuses**: regra anti-literal-de-cor já existente na mesma config (feature anterior, T11)
**Requirement**: TEMAF-13

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `npm run lint` sai com código 0 no estado atual
- [ ] Inserir `fontWeight="bold"` temporariamente em `src/pages/` faz o lint sair diferente de 0, apontando linha e mensagem; a inserção é revertida
- [ ] `src/theme/**` continua isento
- [ ] Teste varre os `.tsx` rastreados e falha se houver `fontWeight=`, `color="textSecondary"` ou `color="primary"` em título
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count: +3

**Tests**: unit · **Gate**: full
**Commit**: `chore(lint): proibe prop de peso de fonte fora do tema` → **feito** `99653cf`

---

### T10: [!] Varredura visual das 20 telas nos cinco temas — BLOQUEADA

**What**: Capturar as 20 telas sob cada tema, comparar com `_docs/estudo-visual/capturas-implementado/` e com os mockups, e registrar o resultado.
**Where**: `_docs/specs/features/temas-fidelidade-visual/validation.md`, capturas em `_docs/estudo-visual/`
**Depends on**: T9
**Reuses**: rotas listadas em `_docs/estudo-visual/README.md`; gerador `gerador/gen_estado_atual.py`
**Requirement**: TEMAF-07, TEMAF-14

**Tools**: MCP: `claude-in-chrome` (captura) · Skill: NONE

**Done when**:

- [ ] Título de página mede 24px e maior valor de KPI mede 27px, medidos por `getComputedStyle` e registrados
- [ ] Nenhuma tela apresenta texto ilegível, cortado ou sobreposto em nenhum dos cinco temas
- [ ] Estado do transbordo do card de Custo Empresa registrado: resolvido, aliviado ou persistente
- [ ] Comparação com as capturas de referência anexada ao `validation.md`
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count final ≥ 559 + soma das tasks anteriores

**Tests**: unit (suíte completa como gate) · **Gate**: full
**Commit**: `docs(tema): varredura visual pos-fidelidade` → **nao aplicado** — a varredura nao aconteceu. Entregue no lugar `test(dashboard): mede escala renderizada do titulo e do maior KPI` `fe3975f`, com o bloqueio registrado em `_docs/estudo-visual/varredura-pos-fidelidade.md`.

---

## Phase Execution Map

```
Fase 1 → Fase 2 → Fase 3 → Fase 4

Fase 1:  T1 ──→ T2 ──→ T3
Fase 2:  T4 ──→ T5
Fase 3:  T6 ──→ T7 ──→ T8 ──→ T9
Fase 4:  T10
```

10 tasks empacotam em 2 batches: `[Fase 1 + Fase 2 = 5]`, `[Fase 3 + Fase 4 = 5]`.
Execução estritamente sequencial.

---

## Task Granularity Check

| Task | Escopo | Status |
| --- | --- | --- |
| T1 | 1 tipo + 1 função, mesmo arquivo | ✅ Granular (coeso) |
| T2 | 1 registro + pares no teste de contraste | ✅ Granular (coeso) |
| T3 | 1 página (verificação) | ✅ Granular |
| T4 | 1 bloco de `typography` | ✅ Granular |
| T5 | 1 arquivo de teste | ✅ Granular |
| T6 | 1 página | ✅ Granular |
| T7 | 7 arquivos, mesma transformação mecânica | ⚠️ OK — coeso; separar por arquivo geraria 7 commits triviais |
| T8 | N arquivos, substituição única | ⚠️ OK — coeso |
| T9 | 1 config + 1 teste | ✅ Granular (coeso) |
| T10 | 1 varredura de verificação | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (corpo) | Diagrama mostra | Status |
| --- | --- | --- | --- |
| T1 | None | — (início Fase 1) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 (fronteira de fase) | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 (fronteira de fase) | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 (fronteira de fase) | ✅ Match |

Nenhuma task depende de fase posterior.

---

## Test Co-location Validation

| Task | Camada criada/modificada | Matriz exige | Task declara | Status |
| --- | --- | --- | --- | --- |
| T1 | Fábrica de temas | unit | unit | ✅ OK |
| T2 | Registro de temas + contraste | unit | unit | ✅ OK |
| T3 | Página (verificação) | unit | unit | ✅ OK |
| T4 | Fábrica de temas | unit | unit | ✅ OK |
| T5 | Escala renderizada | unit | unit | ✅ OK |
| T6 | Página alterada | unit | unit | ✅ OK |
| T7 | Páginas alteradas | unit | unit | ✅ OK |
| T8 | Páginas alteradas | unit | unit | ✅ OK |
| T9 | Config + guarda de props | none (config) + unit (guarda) | unit | ✅ OK — usa o tipo mais alto exigido |
| T10 | Suíte completa como gate | unit | unit | ✅ OK |

Nenhuma violação. Nenhuma task produz código não verificado.

---

## Perguntas antes do Execute

1. **`classico` e a escala** — a spec aplica a escala nova também ao `classico`, preservando só as cores dele. Se a intenção for que o `classico` fique idêntico ao que é hoje (cores **e** escala), isso muda T4 e precisa virar assunção antes do Execute.
2. **Skills do projeto** — todas as tasks estão `Skill: NONE`. Faz sentido acionar `testing-a11y` em T3, T5 e T10, e `component-architecture` em T6 e T7?
3. **Sub-agentes** — 10 tasks empacotam em 2 batches. Confirma workers, ou prefere inline?
4. **Commits** — a preferência do projeto é sem commits automáticos. Mantém, ou autoriza um commit atômico por task?
