# validation — temas-visuais

## Status atual

| Campo | Valor |
| --- | --- |
| **Verdict** | **PASS** |
| **Spec vigente** | `_docs/specs/features/temas-visuais/spec.md` |
| **HEAD commit** | `ab2cf01` — fix(cycle-2): preserve classico dashboard colors and strengthen theme tests |
| **Open gaps** | 1. **P2 — TEMA-14:** Organograma indigo — smoke only; sem asserção de contraste/distinguibilidade de nós, arestas e minimap. 2. **P2 — TEMA-10 (test gap):** `index.css` conforme spec, mas sem teste automatizado que rejeite declarações de cor. 3. **Info:** PoC `DashboardCustomizavel/` gitignored — fora do escopo de grep rastreado. |

---

## temas-visuais — 2026-08-03 — a16f25f..8f5f9a1

**Verdict:** FAIL  
**Commit range:** `a16f25f..8f5f9a1` (23 commits)  
**Gate:** lint exit 0 (12 warnings), test 551/551 pass (baseline 487, delta +64), build exit 0  
**Sensor:** killed=3, survived=0

### Gate evidence

| Step | Command | Result |
| --- | --- | --- |
| Lint | `cd frontend && npm run lint` | exit 0 — 12 warnings, 0 errors |
| Test | `cd frontend && npm run test` | exit 0 — 41 files, **551 passed** (baseline 487) |
| Build | `cd frontend && npm run build` | exit 0 — Poppins woff2 bundled in `dist/assets/` |

### Grep color literals (tracked `git ls-files`)

```
frontend/src/pages/Organograma/index.tsx:1218          color: '#fff',
frontend/src/pages/Relatorios/RelatorioCatalogCard.tsx:61 boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
```

Nota: `frontend/src/pages/DashboardCustomizavel/` está em `.gitignore` (PoC não roteado) mas contém 17+ literais no working tree — fora do commit, porém violaria o grep da spec se incluído.

### AC evidence (TEMA-01..TEMA-18)

| ID | Outcome | Evidence (file:line + assertion) | Status |
| --- | --- | --- | --- |
| TEMA-01 | Zero literais de cor em `src/pages/` e `src/components/` | **Violations:** `Organograma/index.tsx:1218` `#fff`; `RelatorioCatalogCard.tsx:61` `rgba(...)`. Nenhum teste grep/lint automatizado. | **FAIL** |
| TEMA-02 | Dashboard obtém cores de gráfico de `theme.palette.charts` | Implementação: `Dashboard/index.tsx:47` `chartColors = theme.palette.charts`, `:112` `color: chartColors[...]`. Asserção parcial tema: `themes.test.ts:35` `expect(theme.palette.charts).toEqual([...CLASSICO_CHARTS])`. **Sem asserção no Dashboard.** | **PARTIAL** |
| TEMA-03 | `npm run lint` falha ao introduzir cor fixa em pages/components | Regra existe em `.eslintrc.json:38-44` mas **não** em `eslint.config.js` (config ativa). `npm run lint` passa com literais presentes. Sem teste de integração lint. | **FAIL** |
| TEMA-04 | Id persistido validado contra lista conhecida | `storage.ts:8` `isTemaId(valor) ? valor : TEMA_PADRAO`; `storage.test.ts:22-24` `expect(lerTemaSalvo()).toBe(TEMA_PADRAO)` após id desconhecido | PASS |
| TEMA-05 | `localStorage` indisponível não quebra boot | `storage.ts:9-10` catch → `TEMA_PADRAO`; `storage.test.ts:32-36` getItem throws → default; `:39-43` setItem throws → `not.toThrow()` | PASS |
| TEMA-06 | Preferência persiste entre recarregamentos | `storage.test.ts:46-49` `gravarTema('classico')` + `lerTemaSalvo()` → `'classico'`; `ThemeContext.test.tsx:101` `gravarSpy.toHaveBeenCalledWith('classico')` | PASS |
| TEMA-07 | Troca atômica sem recarregar / sem remount | `ThemeContext.test.tsx:94-104` `mount-count:1` após `setTemaId`; `primary:#1976d2` aplicado imediatamente | PASS |
| TEMA-08 | Item "Aparência" acima de "Alterar senha" | `Layout.test.tsx:78-82` `expect(aparenciaIndex).toBeGreaterThan(-1)` e `expect(alterarSenhaIndex).toBeGreaterThan(aparenciaIndex)` | PASS |
| TEMA-09 | Dialog lista temas, amostras, `aria-checked`, teclado | `AparenciaDialog.test.tsx:29` radio Clássico; `:39` `toHaveAttribute('aria-checked','true')`; `:56-68` Enter/Space; `:84-85` amostras por label | PASS |
| TEMA-10 | `index.css` só reset estrutural, sem cor | `index.css:1-12` — apenas `:root` tipografia e `body` margin/min-size. **Sem teste automatizado.** | **PARTIAL** |
| TEMA-11 | Corporate `#3B82F6` primary, `#0F172A` chrome, charts, 5 telas | `themes.test.ts:58-59` primary/chrome; `:61` charts length; `telasPorTema.test.tsx:285-289` smoke 5 telas; `contraste.test.ts:47-57` corporate in `it.each(TEMAS)` | PASS |
| TEMA-12 | Soft `#1D9E75` primary, `#F4F2EC` sidebar, 5 telas | `themes.test.ts:66-67`; `telasPorTema.test.tsx:285-289` tema soft; `contraste.test.ts:47-57` | PASS |
| TEMA-13 | Indigo `palette.mode:'dark'`, `#7F77DD` primary, 5 telas | `themes.test.ts:74-75`; `telasPorTema.test.tsx:285-289` tema indigo; `contraste.test.ts:47-57` | PASS |
| TEMA-14 | Organograma distinguível no escuro | `OrganogramaGrafico.test.tsx:239-256` renderiza region + "Diretoria" com `temaId:'indigo'`. Implementação: `OrganogramaGrafico/index.tsx:326,386,391` `primaryMain`. **Sem asserção de contraste/distinguibilidade de nós/arestas/minimap.** | **PARTIAL** |
| TEMA-15 | Techne `#7836FC` = `application.yml`, chrome/background | `themes.test.ts:83-85` tokens; `:90-92` `expect(...).toBe(lerPrimaryColorRelatorios())`; `application.yml:58` `primary-color: "#7836FC"` | PASS |
| TEMA-16 | Poppins local via `@fontsource/poppins` | `main.tsx:1-3` imports latin 400/500/600; `themes.test.ts:86` `fontFamily.toMatch(/^Poppins/)`; build emite `poppins-latin-*.woff2` | PASS |
| TEMA-17 | Padrão `techne`; respeita preferência gravada | `themes.test.ts:53` `TEMA_PADRAO === 'techne'`; `ThemeContext.test.tsx:48-56` default techne; `:59-69` mock corporate prevalece | PASS |
| TEMA-18 | WCAG AA 4.5:1 parametrizado por tema | `contraste.test.ts:47-57` `it.each(TEMAS)` + `expect(razaoContraste(fg,bg)).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA)` | PASS |

**Classico preservation (P1 base):** `themes.test.ts:26-30` `primary.main '#1976d2'`, `background.default '#f5f5f5'`.

**Organograma primary tokenization:** `OrganogramaGrafico/index.tsx:326` `primaryMain = theme.palette.primary.main` — implementado; cobertura via smoke indigo apenas (TEMA-14).

### Discrimination sensor

Mutations aplicadas em scratch (backup `.scratch/sensor-temas-visuais/`, restore imediato):

| # | Mutation | Target test | Result |
| --- | --- | --- | --- |
| M1 | `TEMA_PADRAO` → `'classico'` | `themes.test.ts:53` | **KILLED** — `Expected "techne" Received "classico"` |
| M2 | `aria-checked={false}` fixo | `AparenciaDialog.test.tsx:50` | **KILLED** — `Expected aria-checked="true" Received "false"` |
| M3 | Techne primary `#7836FC` → `#000001` | `themes.test.ts:92` | **KILLED** — mismatch vs `application.yml` |

Sensor gap: literais de cor em pages **não** seriam detectados por nenhum teste existente (survived-by-absence).

### Ranked gaps (for orchestrator)

1. **P0 — TEMA-01/03:** Remover `#fff` (`Organograma/index.tsx:1218`) e `rgba` (`RelatorioCatalogCard.tsx:61`); migrar regra `no-restricted-syntax` de `.eslintrc.json` para `eslint.config.js` para que `npm run lint` falhe em literais.
2. **P1 — TEMA-01/03 test gap:** Adicionar teste Vitest ou script CI que falhe se `rg '#…\|rgba\(' src/pages src/components` retornar ocorrências em arquivos rastreados.
3. **P1 — TEMA-02:** Asserção em `Dashboard.test.tsx` (ou teste dedicado) provando que cores de pie/area vêm de `criarTema(id).palette.charts`, não array literal.
4. **P2 — TEMA-10:** Teste lendo `index.css` e rejeitando declarações de cor.
5. **P2 — TEMA-14:** Asserções de contraste ou presença de `primaryMain` em edges/minimap sob tema `indigo`.
6. **Info:** PoC `DashboardCustomizavel/` gitignored — tokenizar ou excluir explicitamente do escopo de grep se intencional.

---

## temas-visuais — fix cycles 1-2 — 2026-08-03 — 8f5f9a1..ab2cf01

**Verdict:** PASS  
**Commit range:** `8f5f9a1..ab2cf01` (2 fix commits: `3cb506b`, `ab2cf01`)  
**Gate:** lint exit 0 (15 warnings), test 559/559 pass (baseline 487, delta +72), build exit 0  
**Sensor:** killed=2, survived=0

### Gate evidence

| Step | Command | Result |
| --- | --- | --- |
| Lint | `cd frontend && npm run lint` | exit 0 — 15 warnings, 0 errors |
| Test | `cd frontend && npm run test` | exit 0 — 42 files, **559 passed** (baseline 487) |
| Build | `cd frontend && npm run build` | exit 0 — Poppins woff2 bundled in `dist/assets/` |

### Grep color literals (tracked `git ls-files`)

```
git ls-files 'src/pages/**/*.tsx' 'src/components/**/*.tsx' | xargs rg '#…|rgba\(|hsla\('
→ ZERO_MATCHES
```

Vitest guard: `noColorLiterals.test.ts:27-38` — `expect(violations).toEqual([])`.

ESLint guard (scratch): `src/pages/_lintSensor.tsx` com `#ff0000` → exit 1, regra `no-restricted-syntax` em `eslint.config.js:31-39`.

### Fix commits

| Commit | Summary |
| --- | --- |
| `3cb506b` | Remove literais (`Organograma/index.tsx`, `RelatorioCatalogCard.tsx`); migra regra para `eslint.config.js`; adiciona `noColorLiterals.test.ts`; asserção charts em `Dashboard.test.tsx` |
| `ab2cf01` | Preserva paleta classico expandida (`themes.ts` + `themes.test.ts:26-39`); reforça `AparenciaDialog.test.tsx` (troca imediata de tema) |

### AC evidence (TEMA-01..TEMA-18)

| ID | Outcome | Evidence (file:line + assertion) | Status |
| --- | --- | --- | --- |
| TEMA-01 | Zero literais de cor em `src/pages/` e `src/components/` | Grep tracked: zero; `noColorLiterals.test.ts:38` `expect(violations).toEqual([])` | **PASS** |
| TEMA-02 | Dashboard obtém cores de gráfico de `theme.palette.charts` | `Dashboard/index.tsx:47` `chartColors = theme.palette.charts`; `Dashboard.test.tsx:112-119` `expect(chartPalette).toContain(fill)`; `themes.test.ts:44` classico charts | **PASS** |
| TEMA-03 | `npm run lint` falha ao introduzir cor fixa em pages/components | `eslint.config.js:31-39` regra ativa; scratch `#ff0000` → exit 1 + mensagem PT; `noColorLiterals.test.ts` cobre grep | **PASS** |
| TEMA-04 | Id persistido validado contra lista conhecida | `storage.ts:8` `isTemaId(valor) ? valor : TEMA_PADRAO`; `storage.test.ts:22-24` id desconhecido → default | PASS |
| TEMA-05 | `localStorage` indisponível não quebra boot | `storage.ts:9-10` catch → `TEMA_PADRAO`; `storage.test.ts:32-36` getItem throws; `:39-43` setItem throws → `not.toThrow()` | PASS |
| TEMA-06 | Preferência persiste entre recarregamentos | `storage.test.ts:46-49` `gravarTema('classico')` + `lerTemaSalvo()`; `AparenciaDialog.test.tsx:59-62` `gravarSpy.toHaveBeenCalledWith('techne')` | PASS |
| TEMA-07 | Troca atômica sem recarregar / sem remount | `ThemeContext.test.tsx:94-104` `mount-count:1` após `setTemaId`; `primary:#1976d2` imediato | PASS |
| TEMA-08 | Item "Aparência" acima de "Alterar senha" | `Layout.test.tsx:78-82` índices menu avatar | PASS |
| TEMA-09 | Dialog lista temas, amostras, `aria-checked`, teclado | `AparenciaDialog.test.tsx:29` radio Clássico; `:48` `aria-checked='true'`; `:59-63` troca imediata; `:66-78` Enter/Space; `:91-95` amostras | PASS |
| TEMA-10 | `index.css` só reset estrutural, sem cor | `index.css:1-12` — tipografia + margin/min-size apenas. **Sem teste automatizado.** | **PARTIAL** |
| TEMA-11 | Corporate `#3B82F6` primary, `#0F172A` chrome, charts, 5 telas | `themes.test.ts:65-70`; `telasPorTema.test.tsx:285-289`; `contraste.test.ts:47-57` | PASS |
| TEMA-12 | Soft `#1D9E75` primary, `#F4F2EC` sidebar, 5 telas | `themes.test.ts:73-78`; `telasPorTema.test.tsx:285-289`; `contraste.test.ts:47-57` | PASS |
| TEMA-13 | Indigo `palette.mode:'dark'`, `#7F77DD` primary, 5 telas | `themes.test.ts:81-87`; `telasPorTema.test.tsx:285-289`; `contraste.test.ts:47-57` | PASS |
| TEMA-14 | Organograma distinguível no escuro | `OrganogramaGrafico.test.tsx:239-256` smoke indigo; impl `OrganogramaGrafico/index.tsx:326,386,391,458,464` `primaryMain`. **Sem asserção de contraste/distinguibilidade.** | **PARTIAL** |
| TEMA-15 | Techne `#7836FC` = `application.yml`, chrome/background | `themes.test.ts:90-101`; `application.yml:58` `primary-color: "#7836FC"` | PASS |
| TEMA-16 | Poppins local via `@fontsource/poppins` | `main.tsx:1-3`; `themes.test.ts:95`; build emite `poppins-latin-*.woff2` | PASS |
| TEMA-17 | Padrão `techne`; respeita preferência gravada | `themes.test.ts:61-62`; `ThemeContext.test.tsx:48-56` default techne; `:59-69` corporate prevalece | PASS |
| TEMA-18 | WCAG AA 4.5:1 parametrizado por tema | `contraste.test.ts:47-57` `it.each(TEMAS)` + `toBeGreaterThanOrEqual(RAZAO_MINIMA_AA)` | PASS |

**Classico preservation (cycle-2):** `themes.test.ts:26-39` paleta expandida (`primary`, `secondary`, `info`, `success`, `warning`, `error`, `divider`, `background`).

### Discrimination sensor

Mutations aplicadas em scratch (backup `.scratch/sensor-temas-visuais/`, restore imediato):

| # | Mutation | Target test | Result |
| --- | --- | --- | --- |
| M1 | `TEMA_PADRAO: TemaId = 'classico'` | `themes.test.ts:62` | **KILLED** — `Expected "techne" Received "classico"` |
| M2 | `export const BAD = "#ff0000"` em `src/components/_sensorMut.tsx` | `noColorLiterals.test.ts:38` | **KILLED** — violations non-empty |

### Ranked gaps (for orchestrator)

1. **P2 — TEMA-14:** Asserções de contraste ou presença de `primaryMain` em edges/minimap sob tema `indigo` (além do smoke atual).
2. **P2 — TEMA-10:** Teste Vitest lendo `index.css` e rejeitando declarações de cor (`color`, `background-color`, etc.).
3. **Info:** PoC `DashboardCustomizavel/` gitignored — tokenizar ou excluir explicitamente do escopo de grep se intencional.
