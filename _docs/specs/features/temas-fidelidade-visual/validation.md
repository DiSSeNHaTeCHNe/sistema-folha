# Fidelidade Visual dos Temas — Validation

> **Arquivo append-only.** Apenas o bloco `## Status atual` é reescrito a cada rodada.
> Cada execução do Verifier acrescenta uma seção nova ao final; nada de execuções
> anteriores é editado ou removido.

---

## Status atual

| Campo | Valor |
| --- | --- |
| **Veredito** | ✅ PASS — com 1 item aberto bloqueado por ambiente (G2), fora do alcance automatizado |
| **Spec vigente** | `_docs/specs/features/temas-fidelidade-visual/spec.md` (QA-1 e QA-2 resolvidas; Success Criteria reescopado em `ebc2535`) |
| **HEAD validado** | `ebc2535` (branch `feat/temas-fidelidade-visual`, base `0a0eac7`) |
| **Gate** | ✅ `lint` 0 erros / `test` 632 passed, 0 failed / `build` OK |
| **Sensor** | 6 mutações, 6 mortas, 0 sobreviventes (inclui a reinjeção do sobrevivente da rodada 1) |
| **Última execução** | 2026-08-04 (rodada 2) |

**Gaps abertos:**

1. **G2 (Major — bloqueado por ambiente, não é defeito de código)** — TEMAF-07 / TEMAF-14:
   a varredura visual das 20 telas × 5 temas (T10) segue sem acontecer. `claude-in-chrome`
   não alcança o sandbox e não há dev server no host. Continuam sem evidência: legibilidade,
   corte e sobreposição; o estado do transbordo do card de Custo Empresa; a comparação com
   `capturas-implementado/`. Requer decisão do usuário (UAT manual ou reagendar T10) —
   não é fechável por teste automatizado.
2. **G5 (Minor / residual, pré-existente)** — pesos de fonte fora do tema na forma `sx`,
   que nem a guarda nem a regra de lint pegam (a regra mira `JSXAttribute[name.name='fontWeight']`):
   `frontend/src/pages/Importacao/index.tsx:816` (`<Typography variant="body2" sx={{ mt: 2, fontWeight: 'bold' }}>`)
   e `frontend/src/pages/Funcionarios/index.tsx:499` (`<Typography variant="h6" sx={{ fontWeight: 600, fontSize: '1rem' }}>`,
   este último redundante com o tema). Fora da letra do AC1 de P1-Props, mas contraria a
   Goal "peso decidido pelo tema".

**Gaps fechados na rodada 2:** G1 (FIX-1, `b0280e8`), G3 (FIX-4, `ebc2535`),
G4 (FIX-2, `41771a3`). O marcador `SPEC_DEVIATION` obsoleto de `techne.info` foi
rebaixado a nota (FIX-3, `b8d8019`).

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
