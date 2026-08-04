# Temas Visuais — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Spec**: `_docs/specs/features/temas-visuais/spec.md`
**Context**: `_docs/specs/features/temas-visuais/context.md`
**Design**: `_docs/specs/features/temas-visuais/design.md`
**Estudo visual de origem**: `_docs/estudo-visual/`
**Status**: Execute complete — 23 tasks + 2 fix cycles (Verifier PASS @ `ab2cf01`)
**User preference (project):** sem commits automáticos salvo pedido explícito.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `frontend/AGENTS.md` (stack TARGET, ver AD-004), `AGENTS.md` raiz, `frontend/vite.config.ts` (Vitest + v8 coverage, sem threshold configurado), amostragem de 33 arquivos `*.test.tsx?` existentes. Sem lições confirmadas no store.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Fábrica e registro de temas (`theme/tokens.ts`, `theme/themes.ts`) | unit (Vitest) | Todos os ramos; 1:1 com TEMA-04; cada tema registrado produz `Theme` válido com `palette.charts` e `palette.chrome` populados | `src/theme/*.test.ts` | `npm run test -- src/theme` |
| Persistência (`theme/storage.ts`) | unit (Vitest) | Todos os ramos + todos os edge cases da spec: chave ausente, string vazia, id desconhecido, valor não-string, `getItem` lança, `setItem` lança | `src/theme/storage.test.ts` | `npm run test -- src/theme/storage` |
| Contraste (`theme/contraste.ts`) | unit (Vitest) | Fórmula WCAG validada contra pares de referência conhecidos; varredura parametrizada `TEMAS × pares` (TEMA-18) | `src/theme/contraste.test.ts` | `npm run test -- src/theme/contraste` |
| Context provider (`contexts/ThemeContext.tsx`) | unit (Vitest + Testing Library) | 1:1 com TEMA-06/07; troca aplica sem remontar rota; hook fora do provider lança | `src/contexts/ThemeContext.test.tsx` | `npm run test -- src/contexts/ThemeContext` |
| Dialog de aparência (`components/AparenciaDialog/`) | unit (Vitest + Testing Library) | 1:1 com TEMA-09; lista todos os temas; marca o ativo via `aria-checked`; seleção por teclado; fechar sem selecionar preserva o tema | `src/components/AparenciaDialog/*.test.tsx` | `npm run test -- src/components/AparenciaDialog` |
| Layout / menu do avatar (`components/Layout/`) | unit (Vitest + Testing Library) | TEMA-08; item "Aparência" presente e acima de "Alterar senha"; abre o dialog. Suíte existente (5 testes) não regride | `src/components/Layout/Layout.test.tsx` | `npm run test -- src/components/Layout` |
| Páginas tokenizadas (Dashboard, Organograma, Funcionários) | unit (Vitest + Testing Library) | Suítes existentes passam sem alteração de asserção; smoke de render por tema registrado | `src/pages/**/*.test.tsx`, `src/components/OrganogramaGrafico/*.test.tsx` | `npm run test -- src/pages src/components/OrganogramaGrafico` |
| Harness de teste (`test/renderWithProviders.tsx`) | none | Gate: as 33 suítes existentes continuam verdes | `src/test/` | `npm run test` |
| Config (`.eslintrc.json`, `index.css`, `main.tsx`, `augment.d.ts`) | none | Gate de build e lint | — | `npm run lint && npm run build` |

**Baseline de cobertura**: registrar `npm run test:coverage` antes da T1 e comparar ao final de cada fase. Cobertura não pode regredir (Success Criteria da spec).

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após task com testes unitários em um caminho específico | `cd frontend && npm run test -- <caminho>` |
| Build | Após task só de config, tipos ou CSS | `cd frontend && npm run lint && npm run build` |
| Full | Após a última task de cada fase e antes do Verifier | `cd frontend && npm run lint && npm run test && npm run build` |

---

## Execution Plan

Cinco fases, na ordem pedida: uma de preparação e uma por tema. A Fase 1 excede o
orçamento de um worker (13 tasks), então é subdividida em três sub-fases coesas —
as sub-fases são pontos de corte de batch, não fases semânticas adicionais.

### Fase 1 — Preparação: adequar o frontend a temas e preservar o tema atual (13 tasks)

**1A — Fundação do tema (5 tasks)**

```
T1 → T2 → T3 → T4 → T5
```

**1B — Wiring e harness de teste (3 tasks)**

```
T6 → T7 → T8
```

**1C — Tokenização das telas e guarda anti-regressão (5 tasks)**

```
T9 → T10 → T11 → T12 → T13
```

### Fase 2 — Tema Corporate slate (2 tasks)

```
T14 → T15
```

### Fase 3 — Tema Soft neutral (2 tasks)

```
T16 → T17
```

### Fase 4 — Tema Indigo dark (3 tasks)

```
T18 → T19 → T20
```

### Fase 5 — Tema Techne brand e adoção como padrão (3 tasks)

```
T21 → T22 → T23
```

---

## Task Breakdown

### T1: Declarar a extensão da paleta MUI

**What**: Adicionar os slots `palette.charts` e `palette.chrome` ao tipo `Palette` do MUI por augmentation de módulo.
**Where**: `frontend/src/theme/augment.d.ts` (novo)
**Depends on**: None
**Reuses**: Padrão oficial `declare module '@mui/material/styles'`
**Requirement**: TEMA-02

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `Palette` e `PaletteOptions` declaram `charts: string[]` e `chrome: { bg; fg; fgAtivo; selecionado }`
- [ ] `npm run build` compila sem erro de tipo
- [ ] Nenhum `any` e nenhum `as` introduzido (regra do `frontend/AGENTS.md`)

**Tests**: none · **Gate**: build
**Commit**: `feat(tema): declara slots charts e chrome na paleta MUI`

---

### T2: Criar a fábrica de temas

**What**: Definir o tipo `TokensTema` e a função `montarTema(tokens)` que produz um `Theme` com os overrides de `MuiDrawer`, `MuiAppBar`, `MuiCard`, `MuiTableCell` e `MuiListItemButton`.
**Where**: `frontend/src/theme/tokens.ts` (novo)
**Depends on**: T1
**Reuses**: `src/theme.ts` (overrides existentes, antes de removê-lo em T7)
**Requirement**: TEMA-01

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `montarTema` popula `palette.charts` e `palette.chrome` a partir dos tokens
- [ ] Overrides de componente escritos uma única vez, parametrizados por token
- [ ] Teste unitário: um token de exemplo produz `Theme` com `palette.charts.length > 0` e `palette.chrome.bg` igual ao token informado
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: 3 testes passam

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): fabrica montarTema com overrides parametrizados`

---

### T3: Registrar os temas e o tema `classico`

**What**: Criar o registro `TEMAS` com `TEMA_IDS`, `TEMA_PADRAO`, `criarTema(id)` e o type guard `isTemaId`, contendo apenas o tema `classico` que reproduz exatamente o `createTheme` inline atual de `main.tsx`.
**Where**: `frontend/src/theme/themes.ts` (novo)
**Depends on**: T2
**Reuses**: `main.tsx` (paleta inline atual — fonte de verdade para `classico`)
**Requirement**: TEMA-01, TEMA-04

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `classico` construído fora da fábrica (DD-3), com `primary #1976d2`, `secondary #dc004e`, `background.default #f5f5f5`
- [ ] `classico` também expõe `palette.charts` e `palette.chrome` com os valores em uso hoje
- [ ] `isTemaId` retorna `false` para `null`, `undefined`, `''`, número e id desconhecido
- [ ] `TEMA_PADRAO === 'classico'` nesta fase
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: 6 testes passam

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): registro de temas com classico preservado`

---

### T4: Implementar a persistência da preferência

**What**: Criar `lerTemaSalvo()` e `gravarTema(id)` sobre `localStorage`, chave `sistema-folha:tema`, resilientes a exceção e a valor inválido.
**Where**: `frontend/src/theme/storage.ts` (novo)
**Depends on**: T3
**Reuses**: `isTemaId` de T3
**Requirement**: TEMA-04, TEMA-05, TEMA-06

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `lerTemaSalvo` retorna `TEMA_PADRAO` quando: chave ausente, string vazia, id desconhecido, valor não-string, `getItem` lança
- [ ] `gravarTema` não propaga exceção quando `setItem` lança (cota, modo privado)
- [ ] Nenhum caminho de falha escreve em console (decisão: falha silenciosa)
- [ ] Gate: `npm run test -- src/theme/storage`
- [ ] Test count: 7 testes passam (um por edge case da spec)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): persistencia da preferencia em localStorage`

---

### T5: Implementar o verificador de contraste WCAG

**What**: Criar `razaoContraste(fg, bg)` com a fórmula de luminância relativa da WCAG 2.1 e o teste parametrizado que varre `TEMAS × pares` exigindo ≥ 4.5:1.
**Where**: `frontend/src/theme/contraste.ts` (novo), `frontend/src/theme/contraste.test.ts` (novo)
**Depends on**: T3
**Reuses**: `TEMAS` de T3
**Requirement**: TEMA-18

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `razaoContraste('#000000', '#ffffff')` retorna 21 e `razaoContraste('#ffffff', '#ffffff')` retorna 1
- [ ] Teste parametrizado percorre todos os temas de `TEMAS` — temas adicionados nas fases 2-5 entram automaticamente
- [ ] Pares verificados: `text.primary`/`background.default`, `text.primary`/`background.paper`, `text.secondary`/`background.paper`, `chrome.fg`/`chrome.bg`, `chrome.fgAtivo`/`chrome.selecionado`, `primary.contrastText`/`primary.main`
- [ ] `classico` passa em todos os pares
- [ ] Gate: `npm run test -- src/theme/contraste`
- [ ] Test count: 3 testes de fórmula + 6 pares × 1 tema = 9 asserções

**Tests**: unit · **Gate**: quick
**Commit**: `test(tema): verificador de contraste WCAG parametrizado por tema`

---

### T6: Criar o provider de tema

**What**: Criar `AppThemeProvider` (estado do tema + `ThemeProvider` + `CssBaseline`) e o hook `useAppTheme`.
**Where**: `frontend/src/contexts/ThemeContext.tsx` (novo)
**Depends on**: T4
**Reuses**: `src/contexts/AuthContext.tsx` (padrão de contexto + hook com erro fora do provider)
**Requirement**: TEMA-06, TEMA-07

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `useAppTheme` expõe `temaId`, `setTemaId`, `temas`
- [ ] `setTemaId` grava via `gravarTema` e aplica o tema sem remontar os filhos
- [ ] `useAppTheme` fora do provider lança erro com mensagem explícita
- [ ] Estado inicial vem de `lerTemaSalvo()`
- [ ] Gate: `npm run test -- src/contexts/ThemeContext`
- [ ] Test count: 5 testes passam

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): AppThemeProvider com persistencia e troca em runtime`

---

### T7: Ligar o provider e limpar o CSS global

**What**: Trocar o `createTheme` inline de `main.tsx` pelo `AppThemeProvider`, reduzir `index.css` a reset estrutural sem cor, e remover o `src/theme.ts` órfão.
**Where**: `frontend/src/main.tsx`, `frontend/src/index.css`, `frontend/src/theme.ts` (remover)
**Depends on**: T6
**Reuses**: —
**Requirement**: TEMA-10

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `main.tsx` não declara tema nem importa `createTheme`
- [ ] `index.css` não contém nenhuma declaração de cor, nem `body { display: flex }`, nem estilo global em `button`
- [ ] `src/theme.ts` removido e nenhum import remanescente aponta para ele
- [ ] `npm run build` compila e `npm run lint` sai com código 0
- [ ] Aplicação sobe com aparência idêntica à anterior (tema `classico`)

**Tests**: none · **Gate**: build
**Commit**: `refactor(tema): substitui tema inline pelo provider e limpa index.css`

---

### T8: Adaptar o harness de teste ao provider

**What**: Envolver `renderWithProviders` com o `ThemeProvider` e aceitar `temaId?: TemaId` para renderizar sob um tema específico.
**Where**: `frontend/src/test/renderWithProviders.tsx`
**Depends on**: T7
**Reuses**: assinatura e padrão existentes do harness
**Requirement**: TEMA-01

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `renderWithProviders` aceita `temaId` opcional, default `TEMA_PADRAO`
- [ ] As 33 suítes existentes passam sem alteração de asserção
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count: contagem total igual ou maior que o baseline registrado antes da T1 (nenhuma exclusão silenciosa)

**Tests**: none (gate sobre a suíte inteira) · **Gate**: full
**Commit**: `test(tema): harness renderWithProviders com provider de tema`

---

### T9: Tokenizar o Dashboard

**What**: Substituir as 25 cores fixas e 11 `rgba(` do Dashboard por tokens do tema, incluindo a paleta de gráficos via `theme.palette.charts`.
**Where**: `frontend/src/pages/Dashboard/index.tsx`
**Depends on**: T8
**Reuses**: tabela de mapeamento cor→token em `design.md`
**Requirement**: TEMA-01, TEMA-02

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Zero literal de cor no arquivo (`grep -E "#[0-9a-fA-F]{3,8}\b|rgba?\(" retorna vazio`)
- [ ] `pieColors` removido; Recharts recebe `theme.palette.charts[i]` via `useTheme()`
- [ ] `AreaChart` usa `theme.palette.primary.main` no lugar de `#4F46E5`
- [ ] Sombras vêm de `theme.shadows[n]`, não de `rgba(0,0,0,0.1)`
- [ ] Aparência sob `classico` idêntica à anterior (comparação com captura pré-T9)
- [ ] Gate: `npm run test -- src/pages/Dashboard`
- [ ] Test count: suíte `Dashboard.test.tsx` passa sem alteração de asserção

**Tests**: unit · **Gate**: quick
**Commit**: `refactor(dashboard): substitui cores fixas por tokens do tema`

---

### T10: Tokenizar o Organograma e a tela de Funcionários

**What**: Substituir os 6 `#1976d2` do `OrganogramaGrafico` (nós, arestas, minimap) e a borda `#e0e0e0` mais os 3 `rgba(` de Funcionários por tokens.
**Where**: `frontend/src/components/OrganogramaGrafico/index.tsx`, `frontend/src/pages/Funcionarios/index.tsx`
**Depends on**: T9
**Reuses**: mesma tabela de mapeamento
**Requirement**: TEMA-01

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Zero literal de cor nos dois arquivos
- [ ] Nós, arestas e `nodeColor` do minimap usam `theme.palette.primary.main`
- [ ] Bordas usam `theme.palette.divider`
- [ ] Gate: `npm run test -- src/pages/Funcionarios src/components/OrganogramaGrafico`
- [ ] Test count: as duas suítes passam sem alteração de asserção

**Tests**: unit · **Gate**: quick
**Commit**: `refactor(organograma,funcionarios): substitui cores fixas por tokens`

---

### T11: Ativar o lint anti-cor-fixa

**What**: Adicionar a regra `no-restricted-syntax` barrando literais de cor em `src/pages/**` e `src/components/**`, com `src/theme/**` isento.
**Where**: `frontend/.eslintrc.json`
**Depends on**: T10
**Reuses**: seletor documentado em `design.md`
**Requirement**: TEMA-03

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `npm run lint` sai com código 0 no estado atual do repositório
- [ ] Inserir `#ff0000` temporariamente em um arquivo de `src/pages/` faz o lint sair com código diferente de 0, apontando linha e mensagem; a inserção é revertida
- [ ] Arquivos em `src/theme/` com cor literal não são sinalizados
- [ ] Gate: `npm run lint && npm run build`

**Tests**: none · **Gate**: build
**Commit**: `chore(lint): proibe cor fixa fora de src/theme`

---

### T12: Criar o dialog de aparência

**What**: Criar o `AparenciaDialog` listando os temas registrados com nome, descrição e amostras, marcando o ativo e aplicando a seleção.
**Where**: `frontend/src/components/AparenciaDialog/index.tsx` (novo), `AparenciaDialog.test.tsx` (novo)
**Depends on**: T11
**Reuses**: `src/components/AlterarSenhaDialog/` (estrutura de dialog e padrão de teste)
**Requirement**: TEMA-09

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Lista todos os itens de `TEMAS`, cada um com nome, descrição e amostras
- [ ] Tema ativo marcado com `aria-checked="true"`; os demais `false`
- [ ] Seleção aplica o tema imediatamente, sem recarregar
- [ ] Fechar sem selecionar preserva o tema anterior
- [ ] Navegável por teclado: Tab alcança cada opção, Enter e Espaço selecionam
- [ ] Sem cor literal no componente (a regra de T11 vale aqui)
- [ ] Gate: `npm run test -- src/components/AparenciaDialog`
- [ ] Test count: 6 testes passam

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): dialog de selecao de aparencia`

---

### T13: Expor "Aparência" no menu do avatar

**What**: Adicionar o item "Aparência" no menu do avatar do `Layout`, acima de "Alterar senha", abrindo o `AparenciaDialog`.
**Where**: `frontend/src/components/Layout/index.tsx`, `Layout.test.tsx`
**Depends on**: T12
**Reuses**: mecânica do item "Alterar senha" já presente no mesmo menu
**Requirement**: TEMA-08

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Item "Aparência" presente no menu e posicionado antes de "Alterar senha"
- [ ] Clicar abre o `AparenciaDialog`
- [ ] Os 5 testes existentes de `Layout.test.tsx` continuam passando
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count: 5 existentes + 2 novos = 7 testes passam

**Tests**: unit · **Gate**: full
**Commit**: `feat(tema): item Aparencia no menu do avatar`

---

### T14: Registrar o tema Corporate slate

**What**: Adicionar o tema `corporate` ao registro `TEMAS` usando `montarTema`.
**Where**: `frontend/src/theme/themes.ts`
**Depends on**: T13
**Reuses**: `montarTema` (T2); paleta em `_docs/estudo-visual/README.md` §01
**Requirement**: TEMA-11, TEMA-18

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `primary.main` = `#3B82F6`; `chrome.bg` = `#0F172A`; `background.default` = `#F4F6F8`
- [ ] `palette.charts` definida para o tema
- [ ] Teste de contraste de T5 passa automaticamente para `corporate` nos 6 pares
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: +6 asserções de contraste (total 15)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): tema Corporate slate`

---

### T15: Verificar Corporate slate nas cinco telas

**What**: Renderizar Login, Dashboard, Funcionários, Folha de Pagamento e Organograma sob `corporate` e corrigir tokens faltantes que a varredura revelar.
**Where**: `frontend/src/pages/**`, `frontend/src/components/**`, `frontend/src/theme/themes.ts`
**Depends on**: T14
**Reuses**: `renderWithProviders({ temaId })` de T8
**Requirement**: TEMA-11

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Smoke test parametrizado renderiza as 5 telas sob `corporate` sem erro
- [ ] Nenhum elemento ilegível ou invisível nas 5 telas (verificação visual registrada)
- [ ] Qualquer token faltante descoberto é adicionado à fábrica, não ao componente
- [ ] Gate: `npm run lint && npm run test && npm run build`
- [ ] Test count: baseline + 5 smokes por tema

**Tests**: unit · **Gate**: full
**Commit**: `test(tema): varredura das telas sob Corporate slate`

---

### T16: Registrar o tema Soft neutral

**What**: Adicionar o tema `soft` ao registro `TEMAS`.
**Where**: `frontend/src/theme/themes.ts`
**Depends on**: T15
**Reuses**: `montarTema`; paleta em `_docs/estudo-visual/README.md` §02
**Requirement**: TEMA-12, TEMA-18

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `primary.main` = `#1D9E75`; `chrome.bg` = `#F4F2EC`; `background.default` = `#FBFAF7`
- [ ] Teste de contraste passa para `soft` nos 6 pares
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: +6 asserções de contraste (total 21)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): tema Soft neutral`

---

### T17: Verificar Soft neutral nas cinco telas

**What**: Mesma varredura de T15 aplicada a `soft`.
**Where**: `frontend/src/pages/**`, `frontend/src/theme/themes.ts`
**Depends on**: T16
**Reuses**: smoke parametrizado de T15
**Requirement**: TEMA-12

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Smoke renderiza as 5 telas sob `soft` sem erro
- [ ] Nenhum elemento ilegível ou invisível
- [ ] Gate: `npm run lint && npm run test && npm run build`

**Tests**: unit · **Gate**: full
**Commit**: `test(tema): varredura das telas sob Soft neutral`

---

### T18: Registrar o tema Indigo dark

**What**: Adicionar o tema `indigo` com `palette.mode: 'dark'`.
**Where**: `frontend/src/theme/themes.ts`
**Depends on**: T17
**Reuses**: `montarTema`; paleta em `_docs/estudo-visual/README.md` §03
**Requirement**: TEMA-13, TEMA-18

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `palette.mode` = `'dark'`; `primary.main` = `#7F77DD`; `background.default` = `#12121A`; `background.paper` = `#1C1C28`
- [ ] `palette.charts` ajustada para legibilidade sobre fundo escuro
- [ ] Teste de contraste passa para `indigo` nos 6 pares
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: +6 asserções de contraste (total 27)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): tema Indigo dark`

---

### T19: Garantir o Organograma legível no tema escuro

**What**: Ajustar `OrganogramaGrafico` para que nós, arestas, minimap e o fundo do canvas ReactFlow permaneçam distinguíveis sob `palette.mode: 'dark'`.
**Where**: `frontend/src/components/OrganogramaGrafico/index.tsx`
**Depends on**: T18
**Reuses**: tokens já introduzidos em T10
**Requirement**: TEMA-14

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Fundo do canvas e do minimap seguem `palette.background`, não branco fixo do ReactFlow
- [ ] Texto dos nós usa `palette.getContrastText` sobre a cor do nó
- [ ] Nenhuma cor literal reintroduzida (lint de T11 continua verde)
- [ ] Gate: `npm run test -- src/components/OrganogramaGrafico`
- [ ] Test count: suíte existente + 1 teste de render sob `indigo`

**Tests**: unit · **Gate**: quick
**Commit**: `fix(organograma): legibilidade sob tema escuro`

---

### T20: Verificar Indigo dark nas cinco telas

**What**: Varredura das 5 telas sob `indigo`, com atenção a superfícies claras remanescentes.
**Where**: `frontend/src/pages/**`, `frontend/src/theme/themes.ts`
**Depends on**: T19
**Reuses**: smoke parametrizado de T15
**Requirement**: TEMA-13

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] Smoke renderiza as 5 telas sob `indigo` sem erro
- [ ] Nenhum card, avatar ou gráfico com fundo claro e texto claro
- [ ] Gate: `npm run lint && npm run test && npm run build`

**Tests**: unit · **Gate**: full
**Commit**: `test(tema): varredura das telas sob Indigo dark`

---

### T21: Empacotar a fonte Poppins

**What**: Adicionar `@fontsource/poppins` (subset latin, pesos 400/500/600) e importá-la no bootstrap da aplicação.
**Where**: `frontend/package.json`, `frontend/src/main.tsx`
**Depends on**: T20
**Reuses**: —
**Requirement**: TEMA-16

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `@fontsource/poppins` em `dependencies` com versão fixada
- [ ] Import dos pesos 400/500/600, subset latin
- [ ] `npm run build` não emite requisição a domínio externo para fonte
- [ ] Gate: `npm run lint && npm run build`

**Tests**: none · **Gate**: build
**Commit**: `chore(tema): empacota fonte Poppins localmente`

---

### T22: Registrar o tema Techne brand

**What**: Adicionar o tema `techne` com a paleta institucional e tipografia Poppins.
**Where**: `frontend/src/theme/themes.ts`
**Depends on**: T21
**Reuses**: `montarTema`; `backend/src/main/resources/application.yml` (`relatorios.branding.primary-color`) como fonte de verdade da cor primária
**Requirement**: TEMA-15, TEMA-18

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `primary.main` = `#7836FC`, idêntico a `relatorios.branding.primary-color` no `application.yml`
- [ ] `chrome.bg` = `#20284E`; `background.default` = `#EFF2F7`
- [ ] `typography.fontFamily` começa com `Poppins`
- [ ] Teste de contraste passa para `techne` nos 6 pares
- [ ] Teste que falha se `primary.main` divergir do valor de branding do backend
- [ ] Gate: `npm run test -- src/theme`
- [ ] Test count: +6 asserções de contraste +1 de paridade com o backend (total 34)

**Tests**: unit · **Gate**: quick
**Commit**: `feat(tema): tema Techne brand alinhado ao branding dos relatorios`

---

### T23: Adotar Techne como padrão e verificar as cinco telas

**What**: Mudar `TEMA_PADRAO` para `techne`, varrer as 5 telas sob o tema e registrar a decisão no STATE.md.
**Where**: `frontend/src/theme/themes.ts`, `_docs/specs/STATE.md`
**Depends on**: T22
**Reuses**: smoke parametrizado de T15
**Requirement**: TEMA-15, TEMA-17

**Tools**: MCP: NONE · Skill: NONE

**Done when**:

- [ ] `TEMA_PADRAO === 'techne'`
- [ ] Teste: sem preferência gravada, o provider inicia em `techne`
- [ ] Teste: com preferência gravada em outro tema, o provider respeita a preferência (não o padrão)
- [ ] Folha de Pagamento verificada especificamente para quebra de layout por métrica da Poppins
- [ ] Smoke renderiza as 5 telas sob `techne` sem erro
- [ ] Decisão `AD-016: Tema Techne como padrão do frontend` registrada no STATE.md
- [ ] Deferred Idea "preferência de tema por usuário no backend" registrada no STATE.md
- [ ] Gate: `npm run lint && npm run test && npm run build`

**Tests**: unit · **Gate**: full
**Commit**: `feat(tema): adota Techne brand como tema padrao`

---

## Phase Execution Map

```
Fase 1 → Fase 2 → Fase 3 → Fase 4 → Fase 5

Fase 1A:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5
Fase 1B:  T6 ──→ T7 ──→ T8
Fase 1C:  T9 ──→ T10 ──→ T11 ──→ T12 ──→ T13
Fase 2:   T14 ──→ T15
Fase 3:   T16 ──→ T17
Fase 4:   T18 ──→ T19 ──→ T20
Fase 5:   T21 ──→ T22 ──→ T23
```

Execução estritamente sequencial. 23 tasks empacotam em 4 batches de ~7:
`[1A+1B = 8]`, `[1C = 5]`, `[Fase 2+3+4 = 7]`, `[Fase 5 = 3]`.

---

## Task Granularity Check

| Task | Escopo | Status |
| --- | --- | --- |
| T1 | 1 arquivo de tipos | ✅ Granular |
| T2 | 1 função fábrica | ✅ Granular |
| T3 | 1 registro + 1 type guard, mesmo arquivo | ✅ Granular (coeso) |
| T4 | 2 funções, mesmo arquivo | ✅ Granular (coeso) |
| T5 | 1 função + seu teste | ✅ Granular |
| T6 | 1 provider + 1 hook, mesmo arquivo | ✅ Granular (coeso) |
| T7 | 3 arquivos, uma única mudança lógica (trocar a origem do tema) | ⚠️ OK — indivisível: separar deixaria o build quebrado |
| T8 | 1 arquivo de harness | ✅ Granular |
| T9 | 1 página | ✅ Granular |
| T10 | 2 arquivos, mesma transformação mecânica | ⚠️ OK — coeso |
| T11 | 1 arquivo de config | ✅ Granular |
| T12 | 1 componente | ✅ Granular |
| T13 | 1 componente (modificar) | ✅ Granular |
| T14, T16, T18, T22 | 1 entrada de tema cada | ✅ Granular |
| T15, T17, T20, T23 | 1 varredura de verificação cada | ✅ Granular |
| T19 | 1 componente | ✅ Granular |
| T21 | 1 dependência + 1 import | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (corpo) | Diagrama mostra | Status |
| --- | --- | --- | --- |
| T1 | None | — (início 1A) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T3 | T4 → T5 | ⚠️ Ver nota |
| T6 | T4 | T5 → T6 | ⚠️ Ver nota |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |
| T13 | T12 | T12 → T13 | ✅ Match |
| T14 | T13 | T13 → T14 (fronteira de fase) | ✅ Match |
| T15 | T14 | T14 → T15 | ✅ Match |
| T16 | T15 | T15 → T16 | ✅ Match |
| T17 | T16 | T16 → T17 | ✅ Match |
| T18 | T17 | T17 → T18 | ✅ Match |
| T19 | T18 | T18 → T19 | ✅ Match |
| T20 | T19 | T19 → T20 | ✅ Match |
| T21 | T20 | T20 → T21 | ✅ Match |
| T22 | T21 | T21 → T22 | ✅ Match |
| T23 | T22 | T22 → T23 | ✅ Match |

**Nota sobre T5 e T6**: a dependência real de T5 é T3 (precisa de `TEMAS`) e a de T6 é T4
(precisa de `storage`). O diagrama desenha a cadeia linear porque a execução é
sequencial e um único worker roda a sub-fase inteira em ordem — T5 depois de T4 e T6
depois de T5 satisfaz ambas as dependências reais. Nenhuma task depende de fase
posterior.

---

## Test Co-location Validation

| Task | Camada criada/modificada | Matriz exige | Task declara | Status |
| --- | --- | --- | --- | --- |
| T1 | Config/tipos | none | none | ✅ OK |
| T2 | Fábrica de temas | unit | unit | ✅ OK |
| T3 | Registro de temas | unit | unit | ✅ OK |
| T4 | Persistência | unit | unit | ✅ OK |
| T5 | Contraste | unit | unit | ✅ OK |
| T6 | Context provider | unit | unit | ✅ OK |
| T7 | Config (`main.tsx`, `index.css`) | none | none | ✅ OK |
| T8 | Harness de teste | none (gate sobre a suíte) | none | ✅ OK |
| T9 | Página tokenizada | unit | unit | ✅ OK |
| T10 | Página + componente tokenizados | unit | unit | ✅ OK |
| T11 | Config (eslint) | none | none | ✅ OK |
| T12 | Dialog | unit | unit | ✅ OK |
| T13 | Layout | unit | unit | ✅ OK |
| T14, T16, T18, T22 | Registro de temas | unit | unit | ✅ OK |
| T15, T17, T20, T23 | Páginas sob novo tema | unit | unit | ✅ OK |
| T19 | Componente | unit | unit | ✅ OK |
| T21 | Config/dependência | none | none | ✅ OK |

Nenhuma violação. Nenhuma task produz código não verificado.

---

## Perguntas antes do Execute

1. **MCPs e Skills por task** — todas as tasks estão marcadas `MCP: NONE · Skill: NONE`. O projeto tem as skills `component-architecture`, `forms-validation`, `routing-perf` e `testing-a11y` em `.agents/skills/`. Faz sentido acionar `testing-a11y` nas tasks de dialog (T12) e de varredura (T15, T17, T20, T23)?
2. **Sub-agentes** — 23 tasks empacotam em 4 batches. Confirma o uso de workers, ou prefere execução inline?
3. **Commits** — a preferência registrada no projeto é sem commits automáticos. Mantém, ou autoriza um commit atômico por task conforme o contrato da skill?
