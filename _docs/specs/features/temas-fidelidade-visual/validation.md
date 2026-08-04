# Fidelidade Visual dos Temas — Validation

> **Arquivo append-only.** Apenas o bloco `## Status atual` é reescrito a cada rodada.
> Cada execução do Verifier acrescenta uma seção nova ao final; nada de execuções
> anteriores é editado ou removido.

---

## Status atual

| Campo | Valor |
| --- | --- |
| **Veredito** | ✅ PASS — 14/14 ACs com evidência; verificação visual executada; nenhum gap Major aberto |
| **Spec vigente** | `_docs/specs/features/temas-fidelidade-visual/spec.md` (QA-1 e QA-2 resolvidas; Success Criteria reescopado em `ebc2535`) |
| **HEAD validado** | `d51cf4c` (branch `feat/temas-fidelidade-visual`, base `0a0eac7`) |
| **Gate** | ✅ `lint` 0 erros / `test` 632 passed, 0 failed (44 arquivos) / `build` OK |
| **Sensor** | Rodada 2: 6 mutações, 6 mortas. Acumulado: 13 mutações, 13 mortas. **Não refeito na rodada 3** — `git diff ebc2535..d51cf4c -- frontend/src/` é vazio |
| **Última execução** | 2026-08-04 (rodada 3) |

**Gaps abertos:**

1. **G5 (Minor / residual, pré-existente)** — pesos de fonte fora do tema na forma `sx`,
   que nem a guarda nem a regra de lint pegam (a regra mira `JSXAttribute[name.name='fontWeight']`):
   `frontend/src/pages/Importacao/index.tsx:816` (`<Typography variant="body2" sx={{ mt: 2, fontWeight: 'bold' }}>`)
   e `frontend/src/pages/Funcionarios/index.tsx:499` (`<Typography variant="h6" sx={{ fontWeight: 600, fontSize: '1rem' }}>`,
   este último redundante com o tema). Fora da letra do AC1 de P1-Props, mas contraria a
   Goal "peso decidido pelo tema".
2. **G6 (Minor — residual de cobertura da T10)** — três pontos declaradamente não medidos
   pela varredura, nenhum deles tocando um AC: o diálogo "Ver Rubricas" (onde vivem os
   `subtitle1` de `FolhaPagamento/index.tsx:380,434`) não abre por ausência de ficha
   processada na base; os rótulos de nó do Organograma em modo gráfico ficam abaixo de 1px
   mensurável com `fitView`; a sub-tela "Funcionários da competência" foi medida só no `indigo`.
3. **G7 (Minor — defeito de redação da spec)** — o Success Criteria "Capturas das 20 telas
   comparadas com `capturas-implementado/`" é insatisfazível na letra:
   `_docs/estudo-visual/capturas-implementado/` contém **4 arquivos**, não 20. A T10 comparou
   os 4 disponíveis e substituiu o restante por medição instrumentada (evidência mais forte
   que a comparação visual pretendida). Cumprido em substância; o texto do critério precisa
   ser corrigido ou o baseline completado.

**Gaps fechados na rodada 3:** **G2** (T10 executada, `d51cf4c` — ver julgamento na seção
da rodada 3). **Fechados na rodada 2:** G1 (FIX-1, `b0280e8`), G3 (FIX-4, `ebc2535`),
G4 (FIX-2, `41771a3`). O marcador `SPEC_DEVIATION` obsoleto de `techne.info` foi
rebaixado a nota (FIX-3, `b8d8019`).

**Dívidas registradas fora do escopo desta feature** (D-1 a D-7 de
`varredura-visual.md` §10) — nenhuma é gap desta feature; nenhuma é coberta por AC.
Destaque: **D-3** (R-1 — 19 pares ícone×avatar abaixo de 3:1 em `corporate`/`soft`/`techne`
e 1 em `classico`) e **D-5** (`primary.main` como cor de **texto** reprova AA em 4 dos
5 temas) — **D-5 confirmado como pré-existente**, ver rodada 3.

---

## Execução — `temas-fidelidade-visual` · 2026-08-04 · `0a0eac7..b4209a1`

**Verifier**: sub-agente independente (autor ≠ verificador). Cobertura re-derivada do
zero a partir da spec, regra evidência-ou-zero.
**Veredito**: ❌ **FAIL**

### Task Completion

| Task | Status | Notas |
| --- | --- | --- |
| T1 | ✅ Done | `8b7963c` — `TokensTema` + repasse em `montarTema` |
| T2 | ✅ Done | `3143838` — 4 temas + `classico`; `SPEC_DEVIATION` techne.info (QA-2, resolvida) |
| T3 | ✅ Done | `8f4b308` + `6a8fe17` |
| T4 | ✅ Done | `5af7e26` |
| T5 | ✅ Done | `9007dff` |
| T6 | ✅ Done | `341c3b5` |
| T7 | ✅ Done | `a54967d` |
| T8 | ✅ Done | `c684967` |
| T9 | ✅ Done | `99653cf` |
| T10 | ❌ Bloqueada | Varredura visual não realizada; substituída por medição em jsdom (`fe3975f`). Ver G2. |

### Spec-Anchored Acceptance Criteria

#### P1: Cores semânticas derivadas do tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — `success.main` vem dos tokens do tema, não do default MUI | valor declarado ≠ default MUI | `frontend/src/theme/tokens.test.ts:69-73` — `expect(theme.palette.success.main).toBe('#0F6E56')` + `.not.toBe('#2e7d32')`; `frontend/src/theme/themes.test.ts:137-146` — `expect(palette[papel].main).toBe(esperado[papel])` para 4 temas | ✅ PASS |
| AC2 — idem `warning`/`error`/`info` | idem | `frontend/src/theme/tokens.test.ts:75,81,87`; `frontend/src/theme/themes.test.ts:148-156` — `expect(palette[papel].main).not.toBe(DEFAULTS_MUI[papel])` | ✅ PASS |
| AC3 — `indigo`: as 4 semânticas ≥ 4.5:1 vs `background.paper` | razão ≥ `RAZAO_MINIMA_AA` (4.5) | `frontend/src/theme/contraste.test.ts:76-90` — `expect(razaoContraste(palette[papel].main, palette.background.paper)).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA)`, parametrizado em `TEMAS` (inclui `indigo`) | ✅ PASS |
| AC4 — todos os temas **exceto `classico`** ≥ 4.5:1 | idem, `classico` isento (QA-1) | mesma varredura, com `EXCECOES_SEMANTICAS` em `frontend/src/theme/contraste.test.ts:50` (`classico/warning`) | ✅ PASS (desvio declarado e autorizado pelo usuário) |
| AC5 — fundo e ícone do avatar de KPI derivam dos tokens do tema ativo | `bgcolor` = `palette[papel].light`, `color` = `palette[papel].main`/`.dark` | `frontend/src/pages/Dashboard/Dashboard.test.tsx:189-207` — `expect(canaisDaCor(estilo.backgroundColor)).toEqual(canaisDaCor(palette[papel].light))` + `.not.toEqual(canaisDaCor(paletteDeFabrica[papel].light))`, `it.each(TEMA_IDS)` | ✅ PASS |

> Nota de precisão: AC1 diz "e não o default `#4caf50` do MUI". O default real do MUI v5
> em light mode é `#2e7d32`, e é exatamente o valor que o `classico` declara por DD-4.
> A asserção `not.toBe(default)` roda só nos 4 temas não-`classico`; para o `classico`
> o valor é pinçado literalmente em `frontend/src/theme/themes.test.ts:49-63`. Não é gap
> de cobertura, mas a literalidade do AC1 e DD-4 colidem — registrado.

#### P1: Escala tipográfica dos mockups no tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — `h4.fontSize` = `1.5rem` | `1.5rem` | `frontend/src/theme/tokens.test.ts:101-109` — `expect(theme.typography.h4.fontSize).toBe('1.5rem')`; `frontend/src/theme/themes.test.ts:127-135` (5 temas) | ✅ PASS |
| AC2 — `h3.fontSize` = `1.6875rem` | `1.6875rem` | `frontend/src/theme/tokens.test.ts:103`; `frontend/src/theme/themes.test.ts:129` | ✅ PASS |
| AC3 — `h6.fontSize` = `1rem` | `1rem` | `frontend/src/theme/tokens.test.ts:107`; `frontend/src/theme/themes.test.ts:133` | ✅ PASS |
| AC4 — `{h3,h4,h6}.fontWeight` = 600 | 600 | `frontend/src/theme/tokens.test.ts:104,106,108`; `frontend/src/theme/themes.test.ts:130,132,134` — `expect(typography.hN.fontWeight).toBe(600)` | ✅ PASS |
| AC5 — no Dashboard, título = 24px e maior valor de KPI = 27px por `getComputedStyle` | 24px / 27px | `frontend/src/pages/Dashboard/Dashboard.test.tsx:237-244` — `expect(paraPx(getComputedStyle(titulo).fontSize)).toBeCloseTo(24, 1)` e `...toBeCloseTo(27, 1)`, `it.each(TEMA_IDS)`; reforçado por `frontend/src/theme/escalaRenderizada.test.tsx:29-33` | ⚠️ PASS parcial — medido em **jsdom**, não no navegador. Cobre a resolução da variante pelo tema; **não** cobre cascade real, layout, transbordo ou legibilidade (G2) |
| AC6 — `body1`/`body2`/`subtitle1`/`subtitle2` permanecem nos valores atuais | iguais ao `createTheme()` de fábrica | `frontend/src/theme/tokens.test.ts:117-124` — `expect(theme.typography[variante].fontSize).toBe(padrao[variante].fontSize)` | ❌ **GAP** — cobre só temas montados por `montarTema`. `classico` (`frontend/src/theme/themes.ts:202-217`) não tem evidência. Mutante M7 sobreviveu |
| AC7 — escala idêntica entre os 5 temas | mesmos valores em `TEMA_IDS` | `frontend/src/theme/themes.test.ts:127-135` — `it.each(TEMA_IDS)`; `frontend/src/theme/escalaRenderizada.test.tsx:28-34` — `describe.each(TEMA_IDS)` | ✅ PASS |

#### P1: Peso e cor de texto decididos pelo tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — zero prop `fontWeight=` em `src/pages/`+`src/components/` rastreados | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:39-43` — `expect(arquivos).toEqual([])` sobre `git ls-files` + `/\bfontWeight\s*=/` | ✅ PASS (0 no escopo do AC) — ver G3 e G5 |
| AC2 — zero `color="primary"` em `Typography` de `h1`–`h6` | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:45-53` — casa `variant="hN"` e `color="primary"` na mesma tag de abertura | ✅ PASS |
| AC3 — zero `color="textSecondary"` | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:55-59` — `expect(arquivos).toEqual([])` | ✅ PASS |
| AC4 — cor do título de página = `theme.palette.text.primary` | valor de `palette.text.primary` | `frontend/src/pages/Dashboard/Dashboard.test.tsx:217-229` — `expect(...color).not.toEqual(canaisDaCor(palette.primary.main))` + `expect(color).toBe(getComputedStyle(conteiner).color)` | ⚠️ **Spec-precision gap** — proxy (herança + não-acento), não o valor absoluto. Discrimina (M6 morto), mas o valor literal fica para T10 (G4) |
| AC5 — props `color` semânticas preservadas | `success.main` etc. continuam resolvendo pelo tema | `frontend/src/pages/Dashboard/Dashboard.test.tsx:247-253` — `expect(canaisDaCor(getComputedStyle(valor).color)).toEqual(canaisDaCor(palette.success.main))` | ✅ PASS |
| AC6 — `npm run lint` sai ≠ 0 com `fontWeight=` reintroduzido | exit code ≠ 0 | `frontend/eslint.config.js:45-49` — `no-restricted-syntax` / `JSXAttribute[name.name='fontWeight']`; comprovado empiricamente pelo mutante M3 (1 error reportado em `src/pages/Dashboard/index.tsx:176`) | ✅ PASS |
| AC7 — suítes existentes passam sem alteração de asserção | nenhum `expect` alterado/removido | `git diff 0a0eac7..HEAD -- 'src/**/*.test.*' \| grep -cE "^-.*expect\("` → **0**; único teste tocado é `Dashboard.test.tsx` (+110, apenas adições) | ✅ PASS |

**Status**: ❌ 1 gap de cobertura (P1-Escala AC6 no `classico`), 1 spec-precision gap
(P1-Props AC4), 1 PASS parcial (P1-Escala AC5 / TEMAF-07).

### Edge Cases

- [x] `classico` recebe a escala nova preservando as cores — `frontend/src/theme/themes.ts:215-216`; asserção em `frontend/src/theme/themes.test.ts:127` (escala) e `:49-63` (cores).
- [ ] Transbordo do card de Custo Empresa após a redução para 24px — **não verificado** (depende de T10, G2).
- [ ] Organograma sob `indigo` com nós distinguíveis — nenhum teste cobre o `color: '#fff'` remanescente citado na spec. Sem evidência.
- [x] Nenhum teste que assertava cor/peso foi deletado — 0 linhas `-expect(` no diff de testes.

### Discrimination Sensor

Estado descartável: cópia temporária do arquivo (`cp` → mutação → suíte → restauração
no mesmo comando), com `git status --porcelain` limpo confirmado após cada mutação.

| # | File:line | Mutação | Suíte executada | Killed? |
| --- | --- | --- | --- | --- |
| M1 | `frontend/src/theme/tokens.ts:40` | `h4.fontSize` `1.5rem` → `2.125rem` (valor antigo) | `src/theme/{tokens,escalaRenderizada,themes}` | ✅ Morto — 12 falhas |
| M2 | `frontend/src/theme/themes.ts:138` | `techne.info` `#0A7AB0` → default MUI `#0288d1` | `src/theme/{themes,contraste}` | ✅ Morto — 3 falhas (valor, ≠default, contraste) |
| M3 | `frontend/src/pages/Dashboard/index.tsx:176` | reintroduz `fontWeight="bold"` no título | `src/theme/noStyleProps` + `eslint` | ✅ Morto — 1 falha de teste **e** 1 error de lint |
| M4 | `frontend/src/theme/themes.ts:91` | remove `light: '#23473C'` do `indigo.success` (DD-3) | `src/theme/themes` + `src/pages/Dashboard` | ✅ Morto — 1 falha (`themes.test.ts:158`). **Observação**: o teste de avatares do Dashboard não o pega — ele compara o computado contra `palette[papel].light` do próprio tema mutado (tautológico quanto ao valor); quem pinça o valor é `themes.test.ts` |
| M5 | `frontend/src/theme/tokens.ts:51` | remove o repasse de `warning` em `montarTema` | `src/theme` (8 arquivos) | ✅ Morto — 12 falhas |
| M6 | `frontend/src/pages/Dashboard/index.tsx:176` | título ganha `color="text.secondary"` | `src/pages/Dashboard` | ✅ Morto — 5 falhas (1 por tema) |
| M7 | `frontend/src/theme/themes.ts:215` | acrescenta `body1: { fontSize: '0.5rem' }` em `criarClassico` | `src/theme` (8 arquivos, 101 testes) | ❌ **Sobreviveu** — 101 passed → G1 |

**Profundidade**: lightweight+ (7 mutações, acima do mínimo).
**Resultado**: 6/7 mortos — ❌ FAIL.

### Gate Check

- **Comando**: `cd frontend && npm run lint && npm run test && npm run build`
  (suíte executada em blocos por caminho devido ao timeout de 45s do shell; soma abaixo)
- **lint**: exit 0 — 15 problems (0 errors, 15 warnings pré-existentes)
- **test**: **622 passed, 0 failed, 0 skipped** em 44 arquivos
  - `src/theme` 101 · `src/pages` (5 blocos) 370 · `src/components` 40 · restante 111
- **build**: exit 0 — `✓ built in 7.83s`
- **Baseline**: 559 (validation de `temas-visuais`) → **+63 testes**, nenhum removido
- **Integridade**: 0 asserções deletadas ou enfraquecidas no diff (`grep -cE "^-.*expect\("` = 0)

### Code Quality

| Princípio | Status |
| --- | --- |
| Nenhuma feature além do pedido | ✅ |
| Sem abstração para uso único | ✅ — `ESCALA_TIPOGRAFICA` é constante compartilhada por `montarTema` e `criarClassico`, uso duplo real |
| Sem "flexibilidade" desnecessária | ✅ |
| Só tocou os arquivos necessários | ⚠️ — `frontend/src/pages/Relatorios/index.tsx` aparece com 235±/235∓ no diff, mas `git diff -w` reduz a **1 linha** substantiva: o arquivo tinha terminadores `\r\r\n` e foi normalizado inteiro. Ruído de diff, sem mudança de comportamento; não pedido pela spec |
| Não "melhorou" código não relacionado | ✅ (à parte do item acima) |
| Segue padrões existentes | ✅ — reutiliza `renderWithProviders`, `razaoContraste`, padrão de guarda de `noColorLiterals.test.ts` |
| Aprovaria em review sênior | ✅ com ressalvas (G1, G2) |
| Testes mapeiam a ACs e não são rasos | ✅ — 6/7 mutantes mortos; nenhum teste órfão de requisito |
| Spec-anchored outcome check | ⚠️ — 1 gap + 1 spec-precision gap (ver tabelas) |
| Cobertura por camada | ⚠️ — fábrica/registro/contraste 1:1 com ACs; camada visual (T10) sem cobertura |
| Guidelines do projeto seguidos | ✅ — `frontend/AGENTS.md`, `AGENTS.md` raiz; sem `any`/`as` novos |

### Desvios declarados — confirmação do Verifier

| Desvio | Local | Veredito |
| --- | --- | --- |
| `classico × warning.main` fora da varredura de contraste | `frontend/src/theme/contraste.test.ts:41-50` | ✅ **Confirmado e legítimo** — AC4 da spec vigente já diz "exceto `classico`"; a exceção é 1 par de 20 e está documentada. Deixou de ser desvio de fato |
| `techne.info` = `#0A7AB0` | `frontend/src/theme/themes.ts:134-138` | ✅ **Confirmado, mas o comentário está obsoleto** — `design.md:62` já traz `#0A7AB0` na tabela. O bloco `SPEC_DEVIATION` afirma divergir do design e não diverge mais. Recomendo rebaixar o comentário a nota explicativa |
| `src/pages/DashboardCustomizavel/**` isento no ESLint | `frontend/eslint.config.js:28-35` | ✅ **Confirmado e legítimo** — `.gitignore:132`, `git ls-files` retorna 0 arquivos; a spec lista o diretório em Out of Scope |
| T10 bloqueada | `frontend/src/pages/Dashboard/Dashboard.test.tsx:231-236` | ⚠️ **Confirmado como desvio real e não fechado** — a medição em jsdom cobre a resolução da variante pelo tema, mas não a varredura visual. Ver G2 |

### Requirement Traceability Update

| Requisito | Status anterior | Status verificado |
| --- | --- | --- |
| TEMAF-01 | Done | ✅ Verified |
| TEMAF-02 | Done | ✅ Verified |
| TEMAF-03 | Done | ✅ Verified (com isenção QA-1 autorizada) |
| TEMAF-04 | Done | ✅ Verified |
| TEMAF-05 | Done | ✅ Verified |
| TEMAF-06 | Done | ❌ Needs Fix — `classico` sem cobertura (G1) |
| TEMAF-07 | Parcial | ⚠️ Parcial confirmado — jsdom sim, navegador não (G2) |
| TEMAF-08 | Done | ✅ Verified |
| TEMAF-09 | Done | ✅ Verified (escopo do AC1) |
| TEMAF-10 | Done | ✅ Verified — valor absoluto de `text.primary` pendente (G4) |
| TEMAF-11 | Done | ✅ Verified |
| TEMAF-12 | Done | ✅ Verified |
| TEMAF-13 | Done | ✅ Verified (comprovado por M3) |
| TEMAF-14 | Done | ⚠️ Parcial — asserções intactas ✅; varredura visual ❌ (G2) |

### Fix Plans

**Fix 1 — G1: cobrir as variantes preservadas no `classico`** · Prioridade: Major
- **Causa raiz**: `criarClassico` monta o tema fora de `montarTema`; o teste de variantes
  preservadas (`tokens.test.ts:117`) só exercita a fábrica.
- **Task**: estender `frontend/src/theme/themes.test.ts` com um `it.each(TEMA_IDS)` que
  compare `body1`, `body2`, `subtitle1`, `subtitle2`, `caption`, `h1`, `h2` e `h5` de cada
  tema contra `createTheme().typography` — mesma lista `VARIANTES_PRESERVADAS`.
- **Verify**: mutação M7 (`body1: { fontSize: '0.5rem' }` em `criarClassico`) passa a falhar.
- **Done when**: `npm run test -- src/theme` verde e M7 morto.

**Fix 2 — G2: varredura visual das 20 telas** · Prioridade: Major (bloqueada por ambiente)
- **Causa raiz**: `claude-in-chrome` não alcança o servidor do sandbox e não há dev server no host.
- **Task**: rodar T10 com dev server acessível ao MCP de navegador; registrar as medições,
  o estado do transbordo de Custo Empresa e a comparação com `capturas-implementado/`.
- **Done when**: os quatro "Done when" de T10 preenchidos neste arquivo.

**Fix 3 — G3/G4/G5** · Prioridade: Minor
- G3: alinhar o Success Criteria da spec ao escopo do AC1 (`src/pages/` + `src/components/`)
  ou isentar explicitamente o caso de controle de `escalaRenderizada.test.tsx`.
- G4: fecha junto com Fix 2.
- G5: decidir se `sx={{ fontWeight }}` entra na guarda; se sim, estender o detector de
  `noStyleProps.test.ts` e tratar `frontend/src/pages/Importacao/index.tsx:816`.

### Summary

**Overall**: ⚠️ Quase pronto — funcionalmente entregue, com 1 buraco de cobertura provado
por mutação e a verificação visual em aberto.

**O que funciona**: as quatro semânticas derivam do tema nos cinco temas e passam no AA
(com a isenção autorizada do `classico`); a escala 27/24/16 a 600 é idêntica nos cinco temas
e sobrevive à renderização; as props de peso e cor saíram das telas rastreadas e o lint
barra a reintrodução (comprovado empiricamente); nenhuma asserção existente foi enfraquecida;
gate integralmente verde com 622 testes (+63 sobre o baseline de 559).

**O que falta**: G1 (teste), G2 (ambiente), G3–G5 (precisão/limpeza).

**Próximo passo**: aplicar Fix 1 (barato, fecha o único mutante sobrevivente) e reagendar T10.

---

## Execução — `temas-fidelidade-visual` · 2026-08-04 · rodada 2 · `eef2532..ebc2535`

**Range acumulado da feature**: `0a0eac7..ebc2535`.
**Verifier**: sub-agente independente, novo (autor ≠ verificador; verificador ≠ o da rodada 1
quanto ao estado — cobertura re-derivada do zero, regra evidência-ou-zero).
**Veredito**: ✅ **PASS** — com G2 registrado como aberto e bloqueado por ambiente.

> Nota de leitura: as referências `file:line` da rodada 1 são registro histórico daquele
> HEAD (`b4209a1`) e não foram atualizadas. As citações desta seção referem `ebc2535`;
> `themes.test.ts` e `Dashboard.test.tsx` deslocaram linhas por causa dos fixes.

### Escopo desta rodada

Quatro commits de fix, nenhum deles alterando comportamento de produção:

| Fix | Commit | O que mudou |
| --- | --- | --- |
| FIX-1 | `b0280e8` | `themes.test.ts` — `it.each(TEMA_IDS)` sobre `VARIANTES_PRESERVADAS` (fecha G1) |
| FIX-2 | `41771a3` | `Dashboard.test.tsx` — asserção do valor absoluto de `palette.text.primary` (fecha G4) |
| FIX-3 | `b8d8019` | `themes.ts` — marcador `SPEC_DEVIATION` obsoleto de `techne.info` vira nota comum |
| FIX-4 | `ebc2535` | `spec.md` — Success Criteria reescopado a `src/pages/**` + `src/components/**` (fecha G3) |

Diff da rodada: 4 arquivos, +86/−8. Único arquivo de produção tocado é `frontend/src/theme/themes.ts`,
e apenas em linhas de comentário (`git diff eef2532..ebc2535 -- frontend/src/theme/themes.ts` mostra
somente o bloco de comentário de `TOKENS_TECHNE`; o valor `#0A7AB0` é idêntico).

### Confirmação dos fixes alegados

| Fix | Alegação | Evidência própria do Verifier | Veredito |
| --- | --- | --- | --- |
| FIX-1 | `classico` passa a ser coberto por AC6 | `frontend/src/theme/themes.test.ts:153-165` — `it.each(TEMA_IDS)` × `VARIANTES_PRESERVADAS` (`body1`, `body2`, `subtitle1`, `subtitle2`, `caption`, `h1`, `h2`, `h5`), comparando `fontSize`, `fontWeight` e `lineHeight` contra `createTheme().typography`. Reinjeção do mutante da rodada 1 → **morto** (M8) | ✅ **Confirmado** |
| FIX-2 | AC4 passa a asseverar o valor de `text.primary`, não um proxy | `frontend/src/pages/Dashboard/Dashboard.test.tsx:264-272` — `expect(getComputedStyle(titulo).color).toBe(corNormalizada(palette.text.primary))`, com `CssBaseline` montado (`:96-103`) e normalização pela CSSOM preservando alfa (`:111-115`). **Não é tautológica**: M9 (`sx={{ color: 'common.black' }}` no título) mata os 5 casos novos e **não** mata o teste-proxy anterior (`:244-256`) — ou seja, o novo teste captura exatamente a discriminação que faltava (alfa `rgba(0,0,0,0.87)` vs `rgb(0,0,0)`) | ✅ **Confirmado** |
| FIX-3 | Marcador obsoleto removido | `git grep -n SPEC_DEVIATION -- frontend/` retorna 2 ocorrências, nenhuma em `themes.ts`: `frontend/src/theme/contraste.test.ts:42` (legítimo, QA-1) e `frontend/eslint.config.js:30` (isenção de `DashboardCustomizavel`, já confirmada legítima na rodada 1). O comentário em `frontend/src/theme/themes.ts:134-137` explica o valor e diz explicitamente "Não é desvio" | ✅ **Confirmado** |
| FIX-4 | Grep reescopado retorna 0 | `git grep -c 'fontWeight=' -- 'frontend/src/pages/**/*.tsx' 'frontend/src/components/**/*.tsx'` → **sem saída, exit 1** (0 ocorrências). O grep amplo antigo retorna **1** (`frontend/src/theme/escalaRenderizada.test.tsx:40`), agora explicitamente isentado no texto do critério | ✅ **Confirmado** |

### Spec-Anchored Acceptance Criteria — re-derivado em `ebc2535`

#### P1: Cores semânticas derivadas do tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — `success.main` vem dos tokens, não do default MUI | valor declarado ≠ default MUI | `frontend/src/theme/tokens.test.ts:71-72` — `expect(theme.palette.success.main).toBe('#0F6E56')` + `.not.toBe(DEFAULTS_MUI.success)`; `frontend/src/theme/themes.test.ts:175` — `expect(palette[papel].main).toBe(esperado[papel])` (4 temas) | ✅ PASS |
| AC2 — idem `warning`/`error`/`info` | idem | `frontend/src/theme/tokens.test.ts:77-78,83-84,89-90`; `frontend/src/theme/themes.test.ts:185` — `expect(palette[papel].main).not.toBe(DEFAULTS_MUI[papel])` | ✅ PASS |
| AC3 — `indigo`: as 4 semânticas ≥ 4.5:1 vs `background.paper` | razão ≥ `RAZAO_MINIMA_AA` | `frontend/src/theme/contraste.test.ts:85-87` — `expect(razaoContraste(palette[papel].main, palette.background.paper)).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA)`, parametrizado em `TEMAS` | ✅ PASS |
| AC4 — todos os temas **exceto `classico`** ≥ 4.5:1 | idem, `classico` isento (QA-1) | mesma varredura + `EXCECOES_SEMANTICAS` em `frontend/src/theme/contraste.test.ts:50` (`classico/warning`), guarda em `:81` | ✅ PASS (isenção autorizada pelo usuário, já refletida no texto do AC) |
| AC5 — fundo e ícone do avatar de KPI derivam dos tokens do tema ativo | `bgcolor` = `palette[papel].light`, `color` = `palette[papel].main`/`.dark` | `frontend/src/pages/Dashboard/Dashboard.test.tsx:216-234` — `it.each(TEMA_IDS)`, compara canais do computado contra `palette[papel].light` e `.not.toEqual` a paleta de fábrica. Mutante M12 (`success.light`→`primary.light` em `Dashboard/index.tsx:234`) mata os 5 casos | ✅ PASS |

#### P1: Escala tipográfica dos mockups no tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — `h4.fontSize` = `1.5rem` | `1.5rem` | `frontend/src/theme/tokens.test.ts:105` — `expect(theme.typography.h4.fontSize).toBe('1.5rem')`; `frontend/src/theme/themes.test.ts:140-148` (`it.each(TEMA_IDS)`) | ✅ PASS |
| AC2 — `h3.fontSize` = `1.6875rem` | `1.6875rem` | `frontend/src/theme/tokens.test.ts:103`; `frontend/src/theme/themes.test.ts:140-148` | ✅ PASS |
| AC3 — `h6.fontSize` = `1rem` | `1rem` | `frontend/src/theme/tokens.test.ts:107`; `frontend/src/theme/themes.test.ts:140-148` | ✅ PASS |
| AC4 — `{h3,h4,h6}.fontWeight` = 600 | 600 | `frontend/src/theme/tokens.test.ts:104,106,108` — `expect(theme.typography.hN.fontWeight).toBe(600)` | ✅ PASS |
| AC5 — no Dashboard, título = 24px e maior KPI = 27px por `getComputedStyle` | 24px / 27px | `frontend/src/pages/Dashboard/Dashboard.test.tsx:285-286` — `toBeCloseTo(24, 1)` / `toBeCloseTo(27, 1)`, `it.each(TEMA_IDS)`; reforçado por `frontend/src/theme/escalaRenderizada.test.tsx:28-33` | ⚠️ PASS parcial — medido em **jsdom**; a varredura no navegador segue bloqueada (G2) |
| AC6 — `body1`/`body2`/`subtitle1`/`subtitle2` permanecem nos valores atuais | iguais ao `createTheme()` de fábrica | **Fechado nesta rodada**: `frontend/src/theme/themes.test.ts:153-165` — `it.each(TEMA_IDS)` cobrindo os 5 temas, inclusive `classico`; mais `frontend/src/theme/tokens.test.ts:121-122` para a fábrica | ✅ PASS (era ❌ GAP na rodada 1) |
| AC7 — escala idêntica entre os 5 temas | mesmos valores em `TEMA_IDS` | `frontend/src/theme/themes.test.ts:140-148`; `frontend/src/theme/escalaRenderizada.test.tsx:28-34` (`describe.each(TEMA_IDS)`). Mutante M11 (h3 divergente só no `classico`) mata 2 testes | ✅ PASS |

#### P1: Peso e cor de texto decididos pelo tema

| Critério | Outcome definido na spec | `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| AC1 — zero prop `fontWeight=` em `src/pages/` + `src/components/` rastreados | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:39-43` — `expect(arquivos).toEqual([])` sobre `git ls-files` + `/\bfontWeight\s*=/`. Grep independente do Verifier confirma 0 | ✅ PASS (ver G5 para a forma `sx`) |
| AC2 — zero `color="primary"` em `Typography` `h1`–`h6` | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:45-52` | ✅ PASS |
| AC3 — zero `color="textSecondary"` | 0 arquivos | `frontend/src/theme/noStyleProps.test.ts:55-58`. Mutante M13 (reintroduz a forma depreciada em `Usuarios/index.tsx:286`) mata o teste | ✅ PASS |
| AC4 — cor do título de página = `theme.palette.text.primary` | valor de `palette.text.primary` do tema ativo | **Fechado nesta rodada**: `frontend/src/pages/Dashboard/Dashboard.test.tsx:264-272` — `expect(getComputedStyle(titulo).color).toBe(corNormalizada(palette.text.primary))`, `it.each(TEMA_IDS)`, alfa preservado. Discriminação provada por M9 | ✅ PASS (era ⚠️ spec-precision gap na rodada 1) |
| AC5 — props `color` semânticas preservadas | `success.main` etc. resolvem pelo tema | `frontend/src/pages/Dashboard/Dashboard.test.tsx:295` — `expect(canaisDaCor(getComputedStyle(valor).color)).toEqual(canaisDaCor(palette.success.main))` | ✅ PASS |
| AC6 — `npm run lint` sai ≠ 0 com `fontWeight=` reintroduzido | exit code ≠ 0 | `frontend/eslint.config.js:45-49` — `no-restricted-syntax` / `JSXAttribute[name.name='fontWeight']`; comprovado empiricamente na rodada 1 (M3). Regra inalterada nesta rodada | ✅ PASS |
| AC7 — suítes existentes passam sem alteração de asserção | nenhum `expect` alterado/removido | `git diff eef2532..ebc2535 -- 'frontend/src/**/*.test.*' \| grep -cE "^-.*expect\("` → **0** (+4 asserções novas); acumulado `0a0eac7..ebc2535` também **0** | ✅ PASS |

**Status**: ✅ **14/14 ACs cobertos com evidência**, 0 gaps de cobertura, 0 spec-precision gaps.
1 PASS parcial em P1-Escala AC5 (camada jsdom sim, navegador não — G2).

### Edge Cases

- [x] `classico` recebe a escala nova preservando as cores — `frontend/src/theme/themes.ts:216` (`...ESCALA_TIPOGRAFICA`); asserções em `frontend/src/theme/themes.test.ts:140-148` (escala) e `:56-70` (cores). Agora também `:153-165` para as variantes não tocadas.
- [ ] Transbordo do card de Custo Empresa após a redução para 24px — **segue não verificado** (G2).
- [ ] Organograma sob `indigo` com nós distinguíveis — segue sem teste cobrindo o `color: '#fff'` remanescente. Sem evidência.
- [x] Nenhum teste que assertava cor/peso foi deletado — 0 linhas `-expect(` no diff acumulado de testes.

### Discrimination Sensor — rodada 2

Estado descartável: `cp` do arquivo para `/tmp` → mutação por `sed` → suíte → restauração pelo
backup, tudo no mesmo comando. `git status --porcelain` conferido após **cada** mutação: retorna
apenas o ruído pré-existente da sessão (` M _docs/specs/STATE.md` e 3 entradas `??` alheias à
feature), idêntico antes e depois; `git diff --stat -- frontend/` **vazio** ao final.

| # | File:line | Mutação | Suíte executada | Killed? |
| --- | --- | --- | --- | --- |
| **M8** | `frontend/src/theme/themes.ts:217` | **Reinjeção do sobrevivente da rodada 1** — `body1: { fontSize: '0.5rem' }` em `criarClassico` | `themes.test.ts` + `tokens.test.ts` | ✅ **Morto** — 1 falha (`tema classico preserva as demais variantes no default do MUI (TEMAF-06)`). Era o mutante M7 sobrevivente; **G1 fechado** |
| M9 | `frontend/src/pages/Dashboard/index.tsx:176` | título ganha `sx={{ color: 'common.black' }}` — difere de `text.primary` só no canal alfa nos temas claros | `src/pages/Dashboard/Dashboard.test.tsx` | ✅ Morto — 5 falhas (1 por tema), todas no teste novo de FIX-2. **O teste-proxy da rodada 1 não pega essa mutação** — prova de que o teste novo agrega discriminação real e não é tautológico |
| M10 | `frontend/src/theme/tokens.ts:42` | acrescenta `subtitle2: { fontWeight: 900 }` a `ESCALA_TIPOGRAFICA` | `themes.test.ts` + `tokens.test.ts` | ✅ Morto — 6 falhas (5 do novo `it.each` + a da fábrica). Exercita o braço `fontWeight` da asserção nova |
| M11 | `frontend/src/theme/themes.ts:217` | `h3: { fontSize: '2.5rem', fontWeight: 600 }` só em `criarClassico` (quebra AC7 num tema) | `src/theme` (8 arquivos) | ✅ Morto — 2 falhas (`themes.test.ts` TEMAF-08 + `escalaRenderizada.test.tsx` 27px no `classico`) |
| M12 | `frontend/src/pages/Dashboard/index.tsx:234` | avatar de KPI `success.light`/`success.main` → `primary.light`/`primary.main` (AC5) | `src/pages/Dashboard/Dashboard.test.tsx` | ✅ Morto — 5 falhas (1 por tema) |
| M13 | `frontend/src/pages/Usuarios/index.tsx:286` | reintroduz a forma depreciada `color="textSecondary"` (AC3) | `src/theme/noStyleProps.test.ts` | ✅ Morto — 1 falha (TEMAF-11) |

**Profundidade**: lightweight+ (6 mutações; 5 inéditas em relação à rodada 1, mais a reinjeção obrigatória).
**Resultado**: **6/6 mortos** — ✅ PASS. Acumulado da feature: 13 mutações, 13 mortas
(o único sobrevivente histórico, M7, agora morre como M8).

### Gate Check

- **Comando**: `cd frontend && npm run lint && npm run test && npm run build`
  (suíte rodada em blocos por caminho devido ao timeout de 45s do shell; soma abaixo)
- **lint**: exit 0 — 15 problems (0 errors, 15 warnings `react-refresh`, todos pré-existentes)
- **test**: **632 passed, 0 failed, 0 skipped** em 44 arquivos
  - `src/theme` 106 · `src/components` 40 · `src/{contexts,hooks,lib,routes,services,test,utils}` 109
  - `src/smoke + pages/{ApiKeys,BeneficiosMensais,Dashboard*}` 100 · `pages/{FolhaPagamento,Funcionarios,Importacao,Login}` 130
  - `pages/{Organograma,Relatorios*}` 69 · `pages/{Rubricas*,Usuarios}` 78
- **build**: exit 0 — `✓ built in 9.70s`
- **Rodada 1**: 622 → **rodada 2: 632** (+10: 5 do FIX-1, 5 do FIX-2). Nenhum teste removido.
- **Baseline `temas-visuais`**: 559 → **+73**
- **Nota de contagem**: dos 44 arquivos, 43 são rastreados; 1 é `src/pages/DashboardCustomizavel/`
  (PoC em `.gitignore`, 23 testes), que entra no filtro de caminho mas está fora do escopo da spec.
  Somente rastreados: **609 testes em 43 arquivos**. A contagem de 632 usa o mesmo método da rodada 1,
  para comparabilidade.
- **Integridade**: 0 asserções deletadas ou enfraquecidas (`grep -cE "^-.*expect\("` = 0 na rodada e no acumulado)

### Code Quality — rodada 2

| Princípio | Status |
| --- | --- |
| Nenhuma feature além do pedido | ✅ — os 4 commits fecham exatamente os gaps G1/G3/G4 e a nota obsoleta |
| Sem abstração para uso único | ✅ — `VARIANTES_PRESERVADAS` e `corNormalizada` são locais aos respectivos testes |
| Sem "flexibilidade" desnecessária | ✅ |
| Só tocou os arquivos necessários | ✅ — 4 arquivos, nenhum de produção alterado em comportamento |
| Não "melhorou" código não relacionado | ✅ |
| Segue padrões existentes | ✅ — `it.each(TEMA_IDS)`, `renderWithProviders`, comentários ancorados em TEMAF-xx |
| Aprovaria em review sênior | ✅ — `renderDashboardComBaseline` isolado num helper próprio evita contaminar as asserções vizinhas, o que é a escolha certa |
| Testes mapeiam a ACs e não são rasos | ✅ — 6/6 mutantes mortos; nenhum teste órfão de requisito |
| Spec-anchored outcome check | ✅ — 14/14, sem spec-precision gap remanescente |
| Cobertura por camada | ⚠️ — fábrica/registro/contraste/render-jsdom 1:1 com ACs; camada visual real (T10) sem cobertura (G2) |
| Guidelines do projeto seguidos | ✅ — `frontend/AGENTS.md`, `AGENTS.md` raiz; sem `any`/`as` novos |

### Desvios declarados — reconfirmação

| Desvio | Local | Veredito rodada 2 |
| --- | --- | --- |
| `classico × warning.main` fora da varredura de contraste | `frontend/src/theme/contraste.test.ts:42-50` | ✅ Legítimo e inalterado — AC4 vigente diz "exceto `classico`"; 1 par de 20 |
| `techne.info` = `#0A7AB0` | `frontend/src/theme/themes.ts:134-138` | ✅ **Encerrado** — marcador `SPEC_DEVIATION` removido em `b8d8019`; virou nota explicativa, coerente com `design.md` |
| `src/pages/DashboardCustomizavel/**` isento no ESLint | `frontend/eslint.config.js:28-35` | ✅ Legítimo e inalterado — diretório em `.gitignore` e em Out of Scope |
| Caso de controle com `fontWeight=` em `escalaRenderizada.test.tsx` | `frontend/src/theme/escalaRenderizada.test.tsx:36-47` | ✅ **Encerrado** — o Success Criteria agora isenta o caso explicitamente (`ebc2535`); o teste prova que a prop inline vence o tema, que é a razão da Fase 3 |
| T10 bloqueada | — | ⚠️ **Segue em aberto** (G2) — bloqueio de ambiente, não de código |

### Requirement Traceability Update — rodada 2

| Requisito | Status rodada 1 | Status rodada 2 |
| --- | --- | --- |
| TEMAF-01 | ✅ Verified | ✅ Verified |
| TEMAF-02 | ✅ Verified | ✅ Verified |
| TEMAF-03 | ✅ Verified (isenção QA-1) | ✅ Verified (isenção QA-1) |
| TEMAF-04 | ✅ Verified | ✅ Verified |
| TEMAF-05 | ✅ Verified | ✅ Verified |
| TEMAF-06 | ❌ Needs Fix | ✅ **Verified** — FIX-1, mutante M8 morto |
| TEMAF-07 | ⚠️ Parcial | ⚠️ **Parcial** — jsdom sim, navegador não (G2) |
| TEMAF-08 | ✅ Verified | ✅ Verified (reforçado por M11) |
| TEMAF-09 | ✅ Verified | ✅ Verified |
| TEMAF-10 | ✅ Verified (valor absoluto pendente) | ✅ **Verified integralmente** — FIX-2, valor de `text.primary` fixado |
| TEMAF-11 | ✅ Verified | ✅ Verified (reforçado por M13) |
| TEMAF-12 | ✅ Verified | ✅ Verified |
| TEMAF-13 | ✅ Verified | ✅ Verified |
| TEMAF-14 | ⚠️ Parcial | ⚠️ **Parcial** — asserções intactas ✅; varredura visual ❌ (G2) |

### Success Criteria da spec — conferência item a item

| Critério | Resultado |
| --- | --- |
| `git grep -c 'fontWeight='` em `pages/**` + `components/**` = 0 | ✅ 0 (exit 1, sem saída) |
| `git grep -cE '<Typography[^>]*color="textSecondary"'` = 0 | ✅ 0 |
| Nos 5 temas, semânticas ≠ default do MUI | ✅ `themes.test.ts:185` (4 temas) + `:56-70` (valores literais do `classico`, que por DD-4 reproduz o default de propósito) |
| Título 24px e maior KPI 27px por `getComputedStyle` | ⚠️ ✅ em jsdom (`Dashboard.test.tsx:285-286`), ❌ no navegador (G2) |
| `lint`, `test` e `build` em verde | ✅ os três |
| Contagem de testes ≥ 559 | ✅ 632 |
| Capturas das 20 telas comparadas com `capturas-implementado/` | ❌ **não realizado** (G2) |

### Fix Plans remanescentes

**G2 — varredura visual das 20 telas** · Prioridade: Major · **bloqueado por ambiente**
- **Causa raiz**: `claude-in-chrome` não alcança o servidor do sandbox; não há dev server no host.
- **Não é fechável por teste automatizado.** Requer decisão do usuário: (a) UAT manual guiado,
  (b) reagendar T10 com dev server acessível ao MCP de navegador, ou (c) aceitar o risco
  residual e registrar como dívida.
- **Done when**: os quatro "Done when" de T10 preenchidos, incluindo o estado do transbordo
  do card de Custo Empresa e a comparação com `capturas-implementado/`.

**G5 — pesos de fonte na forma `sx`** · Prioridade: Minor · pré-existente
- Ocorrências: `frontend/src/pages/Importacao/index.tsx:816` e `frontend/src/pages/Funcionarios/index.tsx:499`.
  (`frontend/src/pages/Dashboard/index.tsx:515,565` também têm `fontWeight` em `sx`, mas em
  `Avatar`, não em `Typography` — fora até da Goal, que fala de `Typography`.)
- Decisão pendente: estender o detector de `noStyleProps.test.ts` e a regra de lint à forma
  `sx={{ fontWeight }}`, ou registrar como fora de escopo. Não é regressão desta feature.

### Summary — rodada 2

**Overall**: ✅ **Pronto no que é automatizável**, com a verificação visual em aberto por ambiente.

**Spec-anchored check**: 14/14 ACs com evidência `file:line`, 0 gaps, 0 spec-precision gaps.
**Sensor**: 6/6 mutações mortas, incluindo a reinjeção do único sobrevivente da rodada 1.
**Gate**: lint 0 erros · 632 testes passando · build OK.

**O que mudou desde a rodada 1**: G1 fechado com cobertura real (não com uma asserção
acomodada — o mutante morre); G4 fechado com o valor absoluto da spec e discriminação
provada por um mutante que o teste-proxy anterior deixava passar; G3 fechado por
reescopo da spec, alinhando o Success Criteria ao AC1 que ele sempre pretendeu espelhar;
marcador de desvio obsoleto rebaixado a nota.

**O que falta**: G2 (ambiente — precisa de decisão do usuário) e G5 (residual, pré-existente).

**Próximo passo**: escalar G2 ao usuário (UAT manual ou reagendamento de T10). Nenhum
fix de código pendente.

---

## Execução — `temas-fidelidade-visual` · 2026-08-04 · rodada 3 · `ebc2535..d51cf4c`

**Range acumulado da feature**: `0a0eac7..d51cf4c`.
**Verifier**: sub-agente independente (autor ≠ verificador). Regra evidência-ou-zero.
**Veredito**: ✅ **PASS** — G2 fechado; nenhum gap Major aberto.

### Escopo desta rodada — deliberadamente estreito

A rodada 2 deu PASS com um único gap Major: **G2**, a varredura visual bloqueada por
ambiente. O bloqueio caiu (o usuário subiu o frontend em `http://localhost:3000` com
backend e sessão autenticada) e a T10 foi executada. Esta rodada julga **apenas** se a
evidência da T10 fecha o G2, e reavalia TEMAF-07/TEMAF-14 e os Edge Cases à luz dela.

**Pré-condição verificada pelo Verifier antes de qualquer outra coisa:**

```
git diff --stat ebc2535..d51cf4c -- frontend/src/   →  (vazio)
git diff --stat ebc2535..d51cf4c                    →  4 arquivos, todos em _docs/
                                                       (code-review.md, tasks.md,
                                                        validation.md, varredura-visual.md)
```

**Nenhuma linha de código de produção ou de teste mudou desde a rodada 2.** Por isso a
varredura de cobertura dos 14 ACs e o sensor de mutação **não foram refeitos** — os
resultados da rodada 2 continuam válidos por construção. O gate foi reexecutado para
confirmar o estado.

> Nota: há uma modificação **não commitada** em
> `backend/src/main/java/br/com/techne/sistemafolha/beneficios/infrastructure/BeneficioMensalRepository.java`
> (JPQL `cc.codigo` → `CAST(cc.id AS string)`) que **não é desta feature**. Confirmado que
> não entra no range validado e não foi tocada.

---

### Julgamento do G2 — a evidência da T10 se sustenta?

Artefatos julgados: `_docs/specs/features/temas-fidelidade-visual/varredura-visual.md`
(versionado, `d51cf4c`), `_docs/estudo-visual/varredura-pos-fidelidade.md` (dados brutos,
533 linhas) e `_docs/estudo-visual/capturas-pos-fidelidade/` (6 arquivos). As duas últimas
estão em `.gitignore`.

**Veredito: G2 FECHADO.** Os quatro "Done when" de T10 estão preenchidos com medição real
de navegador, não com proxy. As verificações céticas abaixo foram feitas pelo Verifier,
não aceitas do relatório.

| Alegação da T10 | Verificação independente do Verifier | Veredito |
| --- | --- | --- |
| "16 rotas ativas cobertas × 5 temas" (a spec fala em **20 telas**) | Contei em `frontend/src/routes/index.tsx`: o `<Routes>` declara **exatamente 16 rotas que renderizam tela autenticada** (`/dashboard`, `/funcionarios`, `/folha-pagamento`, `/beneficios-mensais`, `/relatorios`, `/api-keys`, `/usuarios`, `/linhas-negocio`, `/centros-custo`, `/cargos`, `/rubricas`, `/rubricas-fixas`, `/tipos-beneficio`, `/organograma`, `/importacao` — 15 — mais `/organograma` em modo gráfico como 16ª superfície). As demais entradas são `/login` e **três** `<Navigate>` (`/beneficios`, `/`, `*`). **`/dashboard-v2` não existe** no arquivo. As "20 telas" vêm de `inventario-visual-estado-atual.pdf`, anterior à remoção e que conta regiões de rolagem de `/dashboard` como telas distintas | ✅ **Confirmado — não é subcontagem.** A reconciliação 20→16 é real e justificada |
| Título 24px/600 e maior KPI 27px/600 em 5/5 temas × 16 telas; `h6` 16px/600 ×60 por tema | Consistência interna conferida: o relatório diz que `/api-keys` **não tem `h4` algum**, e ainda assim conta 16 `h4` em 16 telas. Fecha porque `/dashboard` tem **dois** `h4` (o título e o valor de Custo Empresa, que é `h4` e não `h3`). Os dados brutos (§1 de `varredura-pos-fidelidade.md`) listam os dois nominalmente. Contagens de `h3` (×3) batem com os 3 KPIs `h3` do Dashboard | ✅ **Confirmado — internamente consistente** |
| Zero sobreposições e zero ilegibilidades | Os 4 "pares" sinalizados no `/dashboard`/`techne` foram investigados no relatório e descartados como `<tspan>` de eixo do Recharts em linhas consecutivas (caixas de linha se tocam 7px, glifos não), com confirmação por captura. O falso positivo dos badges "brancos sobre branco" foi rastreado até `background-image: linear-gradient` — limitação declarada da sonda, não achado. **Descartar os próprios positivos com causa nomeada é sinal de rigor, não de complacência** | ✅ **Confirmado** |
| Dois cortes pré-existentes nos 5 temas | Ambos com geometria em px nos dados brutos e ambos verificados nas capturas de referência. Ver Edge Cases abaixo | ✅ **Confirmado** |

**Fraquezas reais encontradas na evidência** (registradas, nenhuma invalida o fechamento):

1. **Erro de citação `file:line` em §6.** O relatório atribui a "Custo Empresa: R$ …"
   *do card de funcionário* a `FolhaPagamento/index.tsx:775`. Conferi: a linha 775 em
   `d51cf4c` é a **célula da tabela** (`<Typography color="success.main" variant="body1">`);
   a linha do card está em ~638. O rótulo veio da tabela de R-4 do `code-review.md:73`, que
   já trazia a mesma linha. **Os dois elementos foram de fato medidos** (um confirmado, o
   outro refutado), então não há buraco de cobertura — só a citação está errada.
2. **Durabilidade da evidência.** Os dados brutos e as capturas estão em `.gitignore`.
   O artefato versionado é o resumo. Um verificador futuro não consegue re-derivar a
   medição a partir do repositório. Não é gap desta feature; é caveat de rastreabilidade.
3. **Três pontos declaradamente não medidos** → G6 (ver Status atual). Nenhum toca um AC.

---

### Status revisado de TEMAF-07 e TEMAF-14

| Requisito | Status rodada 2 | Status rodada 3 | Base |
| --- | --- | --- | --- |
| **TEMAF-07** — "Escala: verificação medida no navegador" | ⚠️ Parcial (jsdom sim, navegador não) | ✅ **Done / Verified** | O AC5 de P1-Escala pede título de página a 24px e maior valor de KPI a 27px "verificável por `getComputedStyle`". Medido **no navegador**, `getComputedStyle` real, 5 temas × 16 telas: `h4` 24/600, `h3` 27/600, `h6` 16/600, sem uma única exceção e sem nenhuma prop inline vencendo o tema. O tema ativo foi reconfirmado a cada troca por token computado (`background.default` + `fontFamily` — `techne` em Poppins, `classico` em `-apple-system`), o que descarta a hipótese de as 5 medições serem o mesmo tema repetido. Isso é **mais** do que o AC pede |
| **TEMAF-14** — "suítes existentes sem alteração de asserção" | ⚠️ Parcial (asserções ✅; varredura ❌) | ✅ **Done / Verified** | O AC7 de P1-Props é uma propriedade do **diff**, não da tela: `git diff 0a0eac7..d51cf4c -- 'frontend/src/**/*.test.*' \| grep -cE "^-.*expect\("` → **0**. Já estava satisfeito na rodada 1. O "Parcial" da rodada 2 era um efeito colateral de o G2 ter sido pendurado nos dois requisitos. **A varredura não contradiz nada e acrescenta uma confirmação lateral**: como não houve regressão visual, não existia motivo oculto para enfraquecer asserção alguma |

---

### Edge Cases da spec — cobertura à luz da T10

| Edge Case | Evidência | Veredito |
| --- | --- | --- |
| `classico` recebe a escala nova preservando as cores | Testes (`themes.test.ts:140-148` escala, `:56-70` cores, `:153-165` variantes preservadas) **e** navegador: `classico` mede `h4` 24/600 como os demais, com `background.default` `#f8f9fa` e cores próprias intactas | ✅ **Coberto** |
| "`h4` com valor monetário longo: a redução para 24px SHALL **diminuir** o transbordo, **mas não há garantia de eliminá-lo**" | Medido no card Custo Empresa: sobreposição valor×ícone **25px+ → 0px nos 5 temas** (o inventário do estado atual descrevia o valor "encobrindo parcialmente o ícone"); avatar segue **25–36px fora do card**, cortado por `overflow-x: hidden`. Déficit estrutural: `17+191+0+56+17 = 281px` de conteúdo em card de 228px. Presente também nas capturas de referência | ✅ **Coberto — e o resultado bate com a letra da spec.** Diminuiu (o que era SHALL) e não eliminou (o que a spec explicitamente não garantia). O Out of Scope já previa: "Se persistir, vira quick task própria" → é a **D-1** |
| Organograma sob `indigo` com nós distinguíveis | Medido: `/organograma` (lista) sob `indigo` = **limpo** (0 cortes, 0 sobreposições, 0 falhas de contraste); modo gráfico sob `indigo` = `e8 c0 o0 b1`, e o único `b` é a marca d'água "React Flow" da biblioteca. **A premissa do edge case, porém, é falsa**: verifiquei que não existe `#fff` algum em `frontend/src/pages/Organograma/index.tsx` — nem em `d51cf4c` nem em `0a0eac7`. A linha 1218 é um `<Backdrop sx={{ color: 'common.white' }}>` (spinner de operação), sem relação com distinguibilidade de nó | ✅ **Coberto na substância** (nós distinguíveis sob `indigo`, medido), com a ressalva de que os rótulos de nó em modo gráfico não são mensuráveis sob `fitView` (G6) e de que a premissa citada na spec está desatualizada |
| Teste que assertava cor/peso SHALL ser revisto, não deletado | `grep -cE "^-.*expect\("` = **0** no diff acumulado | ✅ **Coberto** |

---

### D-5 é regressão desta feature ou dívida pré-existente?

**Veredito: PRÉ-EXISTENTE.** Determinado com `git diff 0a0eac7..d51cf4c`, não pelo relatório.

D-5 é `primary.main` usado como cor de **texto** sobre `background.paper`, reprovando AA
em `soft` (3,39), `corporate` (3,68), `indigo` (4,48) e `classico` (4,37).

| Teste | Resultado |
| --- | --- |
| As ocorrências mudaram na feature? | `git diff 0a0eac7..d51cf4c -- frontend/src/pages/FolhaPagamento/index.tsx \| grep -E '^[+-].*primary'` → **saída vazia**. As duas `Typography` com `color="primary"` do arquivo (`:752` `variant="body2"` "Normal"; `:770` `variant="body1"` valor de Total Líquido) são **idênticas à base**. Os demais pontos ("Filtrar", "Ver Funcionários", "Limpar") são `Button`/`Chip` do MUI, não tocados |
| A feature aumentou o uso de `primary.main` como texto? | **Não — reduziu.** `git diff 0a0eac7..d51cf4c -- frontend/src/pages frontend/src/components \| grep -cE '^-.*color="primary"'` → **10** linhas removidas (é exatamente o TEMAF-10), e **0** adicionadas em código de produção |
| Vetor teórico de regressão: perder a isenção WCAG de "texto grande" ao cair de `bold` ≥18,66px para 400? | **Não se aplica.** Todos os elementos de D-5 medem 13–16px — sempre abaixo do limiar de texto grande, tanto antes quanto depois. O limiar de 4,5:1 já valia |
| Algum AC cobre esse par? | **Não.** AC3/AC4 de P1-Semânticas cobrem as **quatro semânticas** contra `background.paper`, e essas passam. `primary` não é uma das quatro. `contraste.test.ts` testa `primary.contrastText / primary.main` — par diferente |

**Conclusão**: D-5 **não é gap desta feature**. É a R-2 do `code-review.md`, agora confirmada
com números, e vira dívida separada. A observação do worker de que o par está **ausente**
de `contraste.test.ts` é procedente e é o ponto de partida natural do fix — mas fechá-lo
exigiria um AC que a spec vigente não tem.

Pela mesma lógica, **D-3** (R-1, contraste ícone×avatar) também não é gap desta feature:
nenhum AC cobre o par ícone×fundo do avatar, e o `indigo` — o único tema onde a feature
declarou `light` explícito, por DD-3 — é o único que **passa nos seis pares** (3,15–6,16).
Isto é a prova empírica de que DD-3 estava certa, e simultaneamente o argumento para
estendê-la a `corporate`/`soft`/`techne`, o que resolveria 18 dos 19 pares reprovados.

---

### Gate Check — rodada 3

- **Comando**: `cd frontend && npm run lint && npm run test && npm run build`
  (suíte em blocos por caminho por causa do timeout de 45s do shell)
- **lint**: exit 0 — 15 problems (**0 errors**, 15 warnings `react-refresh` pré-existentes)
- **test**: **632 passed, 0 failed, 0 skipped** em **44 arquivos** — idêntico à rodada 2,
  como esperado de um diff sem código
  - `src/theme` 106 (8 arq.) · `src/components` 40 (4) ·
    `src/{contexts,hooks,lib,routes,services,test,utils}` 109 (17) ·
    `src/smoke + pages/{ApiKeys,BeneficiosMensais,Dashboard*}` 100 (5) ·
    `pages/{FolhaPagamento,Funcionarios,Importacao,Login}` 130 (4) ·
    `pages/{Organograma,Relatorios*}` 69 (3) · `pages/{Rubricas*,Usuarios}` 78 (3)
- **build**: exit 0 — `✓ built in 9.28s`
- **Baseline `temas-visuais`**: 559 → **632** (+73)
- **Integridade**: 0 asserções deletadas ou enfraquecidas no acumulado

### Success Criteria da spec — conferência na rodada 3

| Critério | Rodada 2 | Rodada 3 |
| --- | --- | --- |
| `git grep -c 'fontWeight='` em `pages/**` + `components/**` = 0 | ✅ | ✅ (código inalterado) |
| `git grep -cE '<Typography[^>]*color="textSecondary"'` = 0 | ✅ | ✅ |
| Nos 5 temas, semânticas ≠ default do MUI | ✅ | ✅ |
| Título 24px e maior KPI 27px por `getComputedStyle` | ⚠️ só jsdom | ✅ **Cumprido integralmente** — medido no navegador, 5 temas × 16 telas |
| `lint`, `test` e `build` em verde | ✅ | ✅ |
| Contagem de testes ≥ 559 | ✅ 632 | ✅ 632 |
| Capturas das 20 telas comparadas com `capturas-implementado/` | ❌ | 🟡 **Cumprido em substância, insatisfazível na letra** — `capturas-implementado/` contém **4 arquivos** (`corporate-dashboard.jpg`, `indigo-dashboard.jpg`, `techne-dashboard.jpg`, `techne-folha.jpg`), nunca 20. Os 4 foram comparados 1:1 com `capturas-pos-fidelidade/` (§8 de `varredura-visual.md`), mais 2 capturas novas de detalhe; o restante das 16 telas foi coberto por medição instrumentada, que é evidência **mais forte** que a comparação visual pretendida. **Nenhuma regressão de legibilidade encontrada** — que é a cláusula operativa do critério. Ver G7 |

**Placar**: **6/7 cumpridos integralmente + 1 cumprido em substância** (o 7º com defeito
de redação na própria spec).

### Requirement Traceability Update — rodada 3

| Requisito | Rodada 2 | Rodada 3 |
| --- | --- | --- |
| TEMAF-01 a TEMAF-06 | ✅ Verified | ✅ Verified (inalterados — sem mudança de código) |
| **TEMAF-07** | ⚠️ Parcial | ✅ **Verified** — medido no navegador (T10) |
| TEMAF-08 a TEMAF-13 | ✅ Verified | ✅ Verified (inalterados) |
| **TEMAF-14** | ⚠️ Parcial | ✅ **Verified** — 0 asserções alteradas; a varredura não contradiz |

**14/14 requisitos verificados. Nenhum `Parcial` remanescente.**

### Task Completion — rodada 3

| Task | Status | Notas |
| --- | --- | --- |
| T1–T9 | ✅ Done | Inalteradas desde a rodada 2 |
| **T10** | ✅ **Done** | `d51cf4c` — varredura executada; 4/4 "Done when" preenchidos com evidência de navegador. Era ❌ Bloqueada nas rodadas 1 e 2 |

### Summary — rodada 3

**Overall**: ✅ **Pronto.** O único item Major que restava era ambiental, e caiu.

**O que esta rodada estabeleceu**: a T10 aconteceu de verdade, com dev server real, dados
reais e instrumentação por `getComputedStyle`/`getBoundingClientRect`; a contagem de telas
foi conferida contra `routes/index.tsx` e não é subcontagem; TEMAF-07 e TEMAF-14 passam a
Verified; os quatro Edge Cases da spec têm evidência, e o do Custo Empresa saiu **exatamente**
como a spec previa ("diminuir, sem garantia de eliminar"); D-5 e D-3 são dívidas
pré-existentes, provadas por diff, e não gaps desta feature.

**O que sobra**: G5, G6 e G7 — todos Minor, nenhum bloqueante, nenhum exigindo mudança de
código de produção. Mais as dívidas D-1 a D-7, que a própria varredura levantou e que
merecem virar tasks próprias (prioridade sugerida: D-3 primeiro, porque o fix é declarar
`light` explícito em três temas e resolve 18 pares de uma vez).

**Próximo passo**: nenhum fix pendente nesta feature. Abrir tasks para D-1/D-2/D-3/D-4/D-5
e corrigir a redação do 7º Success Criteria (G7).
