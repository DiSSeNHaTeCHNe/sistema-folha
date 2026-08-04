# Fidelidade Visual dos Temas — Validation

> **Arquivo append-only.** Apenas o bloco `## Status atual` é reescrito a cada rodada.
> Cada execução do Verifier acrescenta uma seção nova ao final; nada de execuções
> anteriores é editado ou removido.

---

## Status atual

| Campo | Valor |
| --- | --- |
| **Veredito** | ❌ FAIL — 1 mutante sobrevivente + 1 critério de sucesso não verificável |
| **Spec vigente** | `_docs/specs/features/temas-fidelidade-visual/spec.md` (com QA-1 e QA-2 resolvidas em 2026-08-04) |
| **HEAD validado** | `b4209a1` (branch `feat/temas-fidelidade-visual`, base `0a0eac7`) |
| **Gate** | ✅ `lint` 0 erros / `test` 622 passed, 0 failed / `build` OK |
| **Sensor** | 7 mutações, 6 mortas, 1 sobrevivente |
| **Última execução** | 2026-08-04 |

**Gaps abertos:**

1. **G1 (Major)** — TEMAF-06 / P1-Escala AC6 não é coberto para o tema `classico`.
   Mutante sobrevivente: acrescentar `body1: { fontSize: '0.5rem' }` em `criarClassico`
   (`frontend/src/theme/themes.ts:202-217`) não derruba nenhum teste. A asserção de
   variantes preservadas existe só para os temas montados por `montarTema`
   (`frontend/src/theme/tokens.test.ts:117`).
2. **G2 (Major)** — TEMAF-07 / TEMAF-14: a varredura visual das 20 telas (T10) não
   aconteceu. Os critérios de sucesso "capturas comparadas com `capturas-implementado/`"
   e "nenhuma tela com texto ilegível, cortado ou sobreposto" seguem sem evidência.
   Estado do transbordo do card de Custo Empresa segue não registrado.
3. **G3 (Minor / spec-precision)** — o critério de sucesso literal
   `git grep -c 'fontWeight=' -- 'frontend/src/**/*.tsx'` retorna **1**, não 0
   (`frontend/src/theme/escalaRenderizada.test.tsx:38` — caso de controle deliberado).
   O AC1 da história de props escopa o grep a `src/pages/` e `src/components/` (onde
   retorna 0); o Success Criteria da spec não repete o escopo.
4. **G4 (Minor / spec-precision)** — P1-Props AC4 ("a cor do título SHALL ser
   `theme.palette.text.primary`") é verificado por proxy (herança + não-acento) em
   `frontend/src/pages/Dashboard/Dashboard.test.tsx:217-229`, não pelo valor absoluto.
   Discrimina (mutante M6 morto), mas não pinça o valor da spec. Depende de G2.
5. **G5 (Minor / residual)** — restam pesos de fonte fora do tema na forma `sx`, que a
   guarda e a regra de lint não pegam (a regra mira `JSXAttribute[name.name='fontWeight']`):
   `frontend/src/pages/Importacao/index.tsx:816` (`<Typography sx={{ fontWeight: 'bold' }}>`).
   Pré-existente e fora da letra do AC1, mas contraria a Goal "peso decidido pelo tema".

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
