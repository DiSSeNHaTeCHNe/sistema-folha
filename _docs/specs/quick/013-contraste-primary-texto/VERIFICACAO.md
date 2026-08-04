# Verificação no navegador — quick tasks 012 e 013

**Data**: 2026-08-04
**Branch**: `feat/temas-fidelidade-visual` @ `6b0ddfe`
**Executado por**: sub-agente Verificador independente (não é o autor das quick tasks)
**Regra**: evidência-ou-zero. Nada de correção de código — só medição, julgamento e registro.
**Veredito**: ❌ **FAIL (não verificável)** — o alvo em `http://localhost:3000` **não
serve o código das quick tasks 012 e 013**. Ver §1.

---

## 1. Bloqueio de ambiente — o alvo serve build antigo

### O que a task assumia

> "O dev server serve o código da branch (Vite com HMR a partir de
> `/Volumes/SSD_Externo/repo/sistema-folha/frontend`)."

**Isso não é verdade no ambiente medido.** `http://localhost:3000` é o serviço
`frontend` do `docker-compose.yml` — target `frontend-prod`, **nginx** na porta 80
mapeada para 3000, servindo um bundle **estático assado na imagem em tempo de
`docker build`**. Não há volume montado, não há Vite, não há HMR.

```yaml
# docker-compose.yml
frontend:
  build: { context: ., dockerfile: Dockerfile, target: frontend-prod }
  ports: ["3000:80"]
```

### Evidência 1 — não é dev server

| Sonda | Resultado |
| --- | --- |
| `document.querySelectorAll('script')` | um único `/assets/index-82Qa2YrV.js` (bundle com hash de build) |
| `window.__vite_plugin_react_preamble_installed__` | `false` |
| `fetch('/src/theme/themes.ts')` | 464 bytes — é o `index.html` de fallback, não o módulo transformado |
| `fetch('/index.html').headers.server` | `nginx/1.31.3` |
| `fetch` em `:5173`, `:5174`, `:4173`, `:3001`, `:8080` | todas `TypeError` (nada escutando) |

### Evidência 2 — o bundle servido é anterior à 012 e à 013

`fetch('/assets/index-82Qa2YrV.js')` (1 520 635 bytes) e busca literal pelos hexes:

| Cor | Só existe **depois** do fix | Presente no bundle servido |
| --- | --- | --- |
| `#1167F4` (`corporate.primary.main` novo) | sim | ❌ não |
| `#188361` (`soft.primary.main` novo) | sim | ❌ não |
| `#8078DD` (`indigo.primary.main` novo) | sim | ❌ não |
| `#1873cd` (`classico.primary.main` novo) | sim | ❌ não |
| `#b05900` (`classico.warning.main` novo, quick 012) | sim | ❌ não |
| `#E2EEEB`, `#E2EDFE`, `#E3F0EC`, `#EFE7FF`, `#2E2B50` (tints novos) | sim | ❌ não |
| `#f57c00` (`classico.warning.main` **antigo**) | não | ✅ sim |

Prova final, extraída do próprio bundle servido (as `amostras` do
`AparenciaDialog`, que a 013 atualizou):

```
{id:"classico",…,amostras:["#1976d2","#dc004e","#f8f9fa"]}
{id:"soft",   …,amostras:["#2C2C2A","#1D9E75","#D85A30"]}
{id:"indigo", …,amostras:["#12121A","#7F77DD","#5DCAA5"]}
```

No repo em `6b0ddfe` esses valores são `#1873cd`, `#188361` e `#8078DD`.
**O bundle servido é, sem ambiguidade, o estado pré-012/pré-013.**

Nota: o `dist/` local do repo (`frontend/dist/assets/index-Be1EM4_W.js`) **contém**
todos os hexes novos — ou seja, o código está correto; o que está velho é a
**imagem Docker em execução**, cujo hash de asset (`82Qa2YrV`) não bate com o
`dist/` local (`Be1EM4_W`).

### Evidência 3 — o instrumento foi validado contra os números "antes"

Antes de parar, a sonda foi rodada no build antigo e **reproduziu exatamente** os
números da `varredura-pos-fidelidade.md` §5 e §7.1 — o que confirma que a sonda
está correta e que o alvo é o estado pré-fix.

`indigo` (tema ativo do usuário), ícone × fundo do avatar de KPI:

| Avatar | medido agora | `varredura-pos-fidelidade.md` §5 |
| --- | --- | --- |
| Total de Funcionários | `#afa9ec` / `#2e2c4a` → **6,16** | 6,16 |
| Custo Empresa | `#5dcaa5` / `#23473c` → **5,13** | 5,13 |
| Benefícios Ativos | `#ef9f27` / `#4a3616` → **5,28** | 5,28 |
| Relação P/D | `#7a76a5` / `#2e2c4a` → **3,15** | 3,15 |
| Top 5 Proventos | `#5dcaa5` / `#23473c` → **5,13** | 5,13 |
| Top 5 Descontos | `#f09595` / `#4a2c2c` → **5,59** | 5,59 |

`corporate`, mesmos seis avatares — os tints ainda são os **derivados** do MUI
(`lighten(main, .2)`), isto é, a quick 012 não está no ar:

| Avatar | ícone / fundo | razão | §5 |
| --- | --- | --- | --- |
| Total de Funcionários | `#185fa5` / `#467fb7` | **1,55** | 1,55 |
| Custo Empresa | `#0f6e56` / `#3f8b77` | **1,53** | 1,53 |
| Benefícios Ativos | `#854f0b` / `#9d723b` | **1,57** | 1,57 |
| Relação P/D | `#104273` / `#467fb7` | **2,43** | 2,43 |
| Top 5 Proventos | `#0f6e56` / `#3f8b77` | **1,53** | 1,53 |
| Top 5 Descontos | `#a32d2d` / `#b55757` | **1,50** | 1,50 |

`corporate` em `/folha-pagamento`, `primary.main` como cor de texto (D-5) —
**22 elementos**, exatamente a contagem da §7.1:

| Elemento | tam/peso | fundo efetivo | razão |
| --- | --- | --- | --- |
| `R$ …` (7 valores monetários) | 16px/400 | `#ffffff` | **3,68** |
| "Ver Funcionários" (×7) | 13px/500 | `#ffffff` | **3,68** |
| chip "Normal" (×6) | 14px/400 | `#ffffff` | **3,68** |
| "Filtrar", "Limpar" (×2) | 14px/500 | `#f4f6f8` | **3,39** |

Cor de primeiro plano medida: `#3b82f6` — o `primary.main` **antigo**.

### Consequência

Conforme a regra da task ("se ainda assim estiver velho, **PARE e reporte** —
medir código velho é pior que não medir"), **a verificação de pixel das quick
tasks 012 e 013 não foi executada**. As seções 2 a 5 abaixo registram o que ficou
**não coberto** e por quê, e o que foi possível medir sem depender do fix.

**Para desbloquear**: republicar o alvo a partir de `6b0ddfe` — `docker compose up
-d --build frontend`, ou subir um Vite dev server (`cd frontend && npm run dev`) e
repetir esta verificação contra ele.

---

## 2. R-1 — ícone × fundo do avatar de KPI, 6 avatares × 5 temas

**NÃO COBERTO** — motivo: §1 (alvo serve build pré-012).

A coluna "antes" abaixo é a da `varredura-pos-fidelidade.md` §5, **reconfirmada
por medição própria** neste ambiente em `indigo` e `corporate` (§1, Evidência 3).
A coluna "depois" **não foi medida** e não é preenchida por dedução.

| Avatar (KPI) | Papel | `indigo` antes | `techne` antes | `soft` antes | `corporate` antes | `classico` antes | depois |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Total de Funcionários | `info` | 6,16 ✅ | 1,40 ❌ | 1,60 ❌ | 1,55 ❌ | 4,03 ✅ | ⚪ não medido |
| Custo Empresa | `success` | 5,13 ✅ | 1,53 ❌ | 1,53 ❌ | 1,53 ❌ | 4,56 ✅ | ⚪ não medido |
| Benefícios Ativos | `warning` | 5,28 ✅ | 1,54 ❌ | 1,57 ❌ | 1,57 ❌ | 2,47 ❌ | ⚪ não medido |
| Relação P/D | `info` (ícone `info.dark`) | 3,15 ✅ | 2,38 ❌ | 2,51 ❌ | 2,43 ❌ | 6,93 ✅ | ⚪ não medido |
| Top 5 Proventos | `success` | 5,13 ✅ | 1,53 ❌ | 1,53 ❌ | 1,53 ❌ | 4,56 ✅ | ⚪ não medido |
| Top 5 Descontos | `error` | 5,59 ✅ | 1,50 ❌ | 1,54 ❌ | 1,50 ❌ | 4,92 ✅ | ⚪ não medido |
| **≥ 3:1** | | **6/6** | **0/6** | **0/6** | **0/6** | **5/6** | **— de 30** |

**Antes: 11/30 pares ≥ 3:1. Depois: não medido.**

### Achados colaterais da medição (válidos independentemente do fix)

1. **Correção de rótulo na §5 da varredura anterior.** O avatar "Total de
   Funcionários" está documentado como papel `primary`; no código
   (`frontend/src/pages/Dashboard/index.tsx:206`) ele é
   `backgroundColor: 'info.light', color: 'info.main'` — papel **`info`**, não
   `primary`. Idem "Relação P/D" (`Dashboard/index.tsx:290`), que é
   `info.light` × **`info.dark`**. Consequência prática: **nenhum dos 6 avatares
   de KPI usa `primary`**, logo a mudança de `primary.light` da quick 013 **não
   altera este conjunto** — quem os corrige é a quick 012 (tints das semânticas).
2. **O falso positivo do `linear-gradient` foi evitado por construção.** A sonda
   percorre a árvore acumulando camadas e registra `background-image` quando
   existe. Os 6 avatares medidos têm `background-image: none` e
   `background-color` opaco — nenhum deles é o badge do top-5. Os badges `#1`…`#5`
   (`Dashboard/index.tsx:513,563`) foram **excluídos do conjunto por filtro**
   (só entram avatares que contêm `<svg>`), e não por interpretação de resultado.

---

## 3. D-5 — `primary.main` como cor de texto sobre `background.paper`

**NÃO COBERTO** no elemento renderizado — motivo: §1.

| Tema | antes (medido, `/folha-pagamento`) | elemento | depois |
| --- | --- | --- | --- |
| `soft` | 3,39 ❌ | valores monetários 16px/400, "Ver Funcionários" 13px/500, chip "Normal" 14px/400 | ⚪ não medido |
| `corporate` | **3,68 ❌ — reconfirmado por medição própria, 22 elementos** | idem (7 + 7 + 6), + "Filtrar"/"Limpar" a 3,39 sobre `background.default` | ⚪ não medido |
| `indigo` | 4,48 ❌ | idem + "Limpar", "Gerar novamente", "Selecionar Arquivo" | ⚪ não medido |
| `classico` | 4,37 ❌ | "Filtrar" (×2) | ⚪ não medido |
| `techne` | 5,63 ✅ | — | ⚪ não medido |

**Antes: 1/5 temas ≥ 4,5:1. Depois: não medido.**

### O que foi possível medir e vale para o depois: as superfícies reais

O erro que originou R-1, R-2 e D-5 foi medir um par que não é o renderizado. Um
componente desse par — a **superfície** — não mudou nas quick tasks e **pôde ser
medido**. Confirmado por `getComputedStyle` nos 5 temas, no `/dashboard`:

| Tema | `background.default` medido (`body`) | `background.paper` medido (`.MuiCard-root`) | Igual ao token de `themes.ts`? |
| --- | --- | --- | --- |
| `classico` | `rgb(248,249,250)` = `#f8f9fa` | `rgb(255,255,255)` = `#ffffff` | ✅ |
| `corporate` | `rgb(244,246,248)` = `#f4f6f8` | `rgb(255,255,255)` = `#ffffff` | ✅ |
| `soft` | `rgb(251,250,247)` = `#fbfaf7` | `rgb(255,255,255)` = `#ffffff` | ✅ |
| `indigo` | `rgb(18,18,26)` = `#12121a` | `rgb(28,28,40)` = `#1c1c28` | ✅ |
| `techne` | `rgb(239,242,247)` = `#eff2f7` | `rgb(255,255,255)` = `#ffffff` | ✅ |

Isto **valida a premissa de superfície** de `contraste.test.ts`: os cards
realmente pintam `background.paper`, e o fundo de página realmente é
`background.default`. Nenhuma camada intermediária (gradiente, `alpha`,
`Paper` aninhado) altera o fundo efetivo dos elementos varridos. É a metade do
par que o teste unitário podia estar errando — **e não está**.

O que continua **não verificado**: que `primary.main` seja de fato a cor de
primeiro plano renderizada nesses ~22 elementos **com o valor novo**. No build
antigo ela é `#3b82f6` (corporate), o valor antigo — coerente.

---

## 4. Regressões que as mudanças de cor podem ter causado

**NÃO COBERTO por medição** — motivo: §1. Nenhum estado `hover`, `selected` ou
`disabled` foi forçado, porque forçá-los no build antigo não diria nada sobre o
fix. O que segue é o **inventário de risco** levantado por leitura de código, para
a verificação futura atacar direto.

### 4.1 Consumidores de `.light` — mapa completo

| Arquivo:linha | Uso | Papel | Risco com o tint novo |
| --- | --- | --- | --- |
| `frontend/src/pages/Dashboard/index.tsx:206,290` | `bgcolor: info.light` + ícone `info.main`/`info.dark` | `info` | coberto pela 012; **não usa `primary`** |
| `frontend/src/pages/Dashboard/index.tsx:234,500` | `bgcolor: success.light` + `success.main` | `success` | coberto pela 012 |
| `frontend/src/pages/Dashboard/index.tsx:262` | `bgcolor: warning.light` + `warning.main` | `warning` | coberto pela 012 |
| `frontend/src/pages/Dashboard/index.tsx:550` | `bgcolor: error.light` + `error.main` | `error` | coberto pela 012 |
| `frontend/src/pages/Funcionarios/index.tsx:534` | `hover` do ícone Editar: `bgcolor: primary.light` | `primary` | **corrigido pela 013** (`color: 'white'` → `'primary.main'`) — ⚪ não confirmado no pixel |
| `frontend/src/pages/Funcionarios/index.tsx:556` | `hover` do ícone Excluir: `bgcolor: error.light` | `error` | ⚪ não medido; par ícone×tint é o mesmo varrido a 3:1 |
| `frontend/src/pages/Organograma/index.tsx:617` | `bgcolor: primary.light` quando `isOver` (drag-over) | `primary` | ⚪ **não coberto**: só aparece durante drag-and-drop de nó, que **alteraria dados** — proibido por escopo. Texto interno é `text.primary`; nos 4 temas claros o tint novo é quase branco (favorece) e no `indigo` é `#2E2B50` (escuro, favorece). Risco baixo, **não verificado**. |
| `frontend/src/pages/DashboardCustomizavel/widgets/KpiWidget.tsx:20,29,36,43,50` | `bg: info.light / success.light / warning.light` | semânticas | ⚪ **não coberto**: a rota `dashboard-v2` foi removida de `routes/index.tsx` (registrado na `varredura-visual.md` §2) — componente sem tela |
| `frontend/src/pages/DashboardCustomizavel/widgets/TopRubricasWidget.tsx:21` | `bg: success.light / error.light` | semânticas | ⚪ idem |

**Nenhum consumidor de `primary.light` além de `Funcionarios:534` e
`Organograma:617` foi encontrado no código de aplicação** — os dois que a 013 já
havia mapeado. Isso é uma verificação **positiva e completa** por varredura
estática (`rg '\.light'` em `frontend/src/**/*.tsx`), independente do build.

### 4.2 R-3 — `.light` vaza para `Alert` / `Snackbar`

`Alert` standard do MUI usa `color: lighten(light, 0.6)` e
`bg: darken(light, 0.9)`; `Alert` outlined usa `border: 1px solid palette[X].light`.
Com os tints novos (e no `indigo`, com `light` **mais escuro** que `main`), o
risco é real. Alerts visíveis no produto:

| Arquivo:linha | Severidade | Visível quando |
| --- | --- | --- |
| `pages/ApiKeys/index.tsx:175,181,317` | `info`, `warning` | `/api-keys` — **visível sem interação** |
| `pages/Relatorios/index.tsx:222` | `info` | `/relatorios` — **visível sem interação** |
| `pages/Usuarios/index.tsx:545` | `info` | dentro de diálogo |
| `pages/Importacao/index.tsx:712,772,812` | `success`, `error`, `warning` | `/importacao`, após ação |
| `pages/Dashboard/index.tsx:99,100` | `error`, `info` | só em erro / sem dados |
| `pages/DashboardCustomizavel/index.tsx:88,89,147` | `error`, `info` | rota removida |
| `pages/Login/index.tsx:78`, `components/AlterarSenhaDialog/index.tsx:107` | `error` | após falha de credencial |
| `components/Notification/index.tsx:24` | dinâmica | Snackbar global |

⚪ **Nenhum medido.** Os dois candidatos "de graça" para a verificação futura são
`/api-keys` e `/relatorios`, que renderizam `Alert` sem nenhuma interação —
**e nenhum deles usa `primary`**, portanto o vetor de risco da 013 sobre `Alert`
é indireto (só via `primary` se algum `Alert color="primary"` for adicionado no
futuro; hoje não existe). O risco concreto de R-3 continua sendo o das
**semânticas alteradas pela 012**, nos 5 temas.

---

## 5. `primary.contrastText` no botão preenchido real

⚪ **NÃO COBERTO** — motivo: §1. No build servido, `corporate` e `soft` ainda têm
`contrastText: #0F172A` sobre o `primary.main` antigo; medir isso não diz nada
sobre a mudança para `#FFFFFF`.

| Tema | `contrastText` esperado após a 013 | razão esperada × `primary.main` | medido no botão `variant="contained"` |
| --- | --- | --- | --- |
| `classico` | derivado (`#fff`) | 4,80 | ⚪ não medido |
| `corporate` | `#FFFFFF` (era `#0F172A`) | 4,92 | ⚪ não medido |
| `soft` | `#FFFFFF` (era `#0F172A`) | 4,71 | ⚪ não medido |
| `indigo` | `#12121A` | 5,01 | ⚪ não medido |
| `techne` | `#FFFFFF` | 5,63 | ⚪ não medido |

---

## 6. Escala tipográfica — não regrediu

✅ **VERIFICADO**, e é a única seção de pixel com veredito positivo, por dois
motivos independentes:

1. **Nenhuma das duas quick tasks toca tipografia.** Os arquivos alterados
   (`theme/tokens.ts`, `theme/themes.ts`, os três de teste,
   `pages/Funcionarios/index.tsx`) não mexem em `ESCALA_TIPOGRAFICA` nem em
   variantes — a escala é a mesma no build servido e em `6b0ddfe`.
2. **Medido por `getComputedStyle` no `/dashboard` nos 5 temas:**

| Tema | `h4` (título de página) | `h3` (maior KPI) | `h6` (título de card) | `fontFamily` |
| --- | --- | --- | --- | --- |
| `classico` | **24px / 600** ✅ | **27px / 600** ✅ | **16px / 600** ✅ | `-apple-system` |
| `corporate` | **24px / 600** ✅ | **27px / 600** ✅ | **16px / 600** ✅ | `Roboto` |
| `soft` | **24px / 600** ✅ | **27px / 600** ✅ | **16px / 600** ✅ | `Roboto` |
| `indigo` | **24px / 600** ✅ | **27px / 600** ✅ | **16px / 600** ✅ | `Roboto` |
| `techne` | **24px / 600** ✅ | **27px / 600** ✅ | **16px / 600** ✅ | `Poppins` |

Bate com a `varredura-pos-fidelidade.md` §1. **Sem regressão.**

⚪ Não coberto: corte, sobreposição e ilegibilidade **novos** nas demais telas —
dependeriam do build novo para significar alguma coisa.

---

## 7. Gate

`cd frontend && npm run lint && npm run test && npm run build` em `6b0ddfe`:

| Etapa | Resultado |
| --- | --- |
| `npm run lint` | ✅ **0 erros**, 15 warnings pré-existentes (`react-hooks/exhaustive-deps` ×6, `react-refresh/only-export-components` ×9) |
| `npm run test` | ✅ **647 passed, 0 failed** em 44 arquivos — bate com o declarado no `SUMMARY.md` da 013 |
| `npm run build` | ✅ `tsc -b` exit 0; `vite build` exit 0, `dist/assets/index-Be1EM4_W.js` (1 521,64 kB) |

### 7.1 Resultado da suíte, por bloco

O shell do ambiente de verificação tem timeout de 45 s, então a suíte foi
executada em blocos por caminho (`npx vitest run <caminho>`), somando o total:

| Bloco | Arquivos | Testes |
| --- | --- | --- |
| `src/theme` | 8 | 121 ✅ |
| `src/components` + `src/contexts` | 6 | 60 ✅ |
| `src/utils` + `src/hooks` + `src/lib` + `src/routes` + `src/services` + `src/test` + `src/smoke.test.tsx` | 16 | 91 ✅ |
| `src/pages/{ApiKeys,BeneficiosMensais,Cargos,CentrosCusto,Dashboard,DashboardCustomizavel}` | 4 | 98 ✅ |
| `src/pages/{FolhaPagamento,Funcionarios,Importacao,LinhasNegocio,Login}` | 4 | 130 ✅ |
| `src/pages/Organograma` | 1 | 39 ✅ |
| `src/pages/{Relatorios,Rubricas,RubricasFixas}` | 4 | 86 ✅ |
| `src/pages/{TiposBeneficio,Usuarios}` | 1 | 22 ✅ |
| **Total** | **44** | **647 passed, 0 failed** |

Nenhum teste pulado (`skipped`), nenhum falho. O gate **passa** — o que reforça a
leitura da §8: o problema é de **publicação do alvo**, não de código.

---

## 8. Veredito final

### ❌ **FAIL — não verificável no ambiente fornecido**

Não é um FAIL sobre o **código**: o `dist/` local construído de `6b0ddfe` contém
todas as cores novas, e o gate passa. É um FAIL sobre a **verificação**: o
objetivo desta task era confirmar o **pixel**, e o pixel disponível é o de um
build anterior às duas quick tasks. Aprovar sem medir seria repetir exatamente o
erro que gerou R-1, R-2 e D-5 — dar por boa uma cor que ninguém viu renderizada.

### Continua aberto

| # | Item | Estado |
| --- | --- | --- |
| 1 | **Republicar o alvo** a partir de `6b0ddfe` (`docker compose up -d --build frontend` ou `npm run dev`) e repetir esta verificação | 🔴 bloqueante |
| 2 | R-1 — 30 pares ícone × avatar (6 × 5 temas) ≥ 3:1 | ⚪ não medido |
| 3 | D-5 — `primary.main` como texto ≥ 4,5:1 nos 5 temas, em `/folha-pagamento`, `/relatorios`, `/usuarios`, `/rubricas`, `/rubricas-fixas`, `/importacao` | ⚪ não medido |
| 4 | `primary.contrastText` no botão `variant="contained"`, 5 temas | ⚪ não medido |
| 5 | Estados `hover` / `selected` / `disabled` — em especial `Funcionarios/index.tsx:534` (ícone Editar) | ⚪ não medido |
| 6 | R-3 — `Alert` standard e outlined nos 5 temas (`/api-keys` e `/relatorios` renderizam sem interação) | ⚪ não medido |
| 7 | `Organograma/index.tsx:617` (`primary.light` em drag-over) | ⚪ não coberto — exigiria drag-and-drop, que alteraria dados |
| 8 | `KpiWidget` / `TopRubricasWidget` (`X.light`) | ⚪ não coberto — rota `dashboard-v2` removida |
| 9 | Rótulo de papel dos avatares na `varredura-pos-fidelidade.md` §5 ("Total de Funcionários" e "Relação P/D" são `info`, não `primary`) | 🟡 correção de documentação pendente |
| 10 | D-1, D-2, D-4, D-6, D-7 | seguem abertos, fora do escopo desta verificação |

### O que ficou provado, apesar do bloqueio

- ✅ As **superfícies** (`background.paper` / `background.default`) que
  `contraste.test.ts` assume são as realmente renderizadas nos 5 temas (§3) —
  metade do par do D-5 está confirmada no pixel.
- ✅ A **escala tipográfica** não regrediu (§6).
- ✅ **Não existe consumidor de `primary.light`** no código de aplicação além dos
  dois que a 013 já mapeou (§4.1).
- ✅ Os 6 avatares de KPI **não usam `primary`** — a 013 não os afeta; quem os
  corrige é a 012 (§2).
- ✅ O instrumento de medição foi **calibrado** contra os números "antes"
  publicados e os reproduziu com exatidão (§1, Evidência 3).

---

## Anexo — método

| Item | Valor |
| --- | --- |
| Alvo | `http://localhost:3000` (nginx, serviço `frontend` do `docker-compose.yml`) |
| Bundle servido | `/assets/index-82Qa2YrV.js`, 1 520 635 bytes |
| Bundle do repo em `6b0ddfe` | `frontend/dist/assets/index-Be1EM4_W.js`, 1 521 642 bytes |
| Sessão | autenticada, dados reais |
| Instrumento | `getComputedStyle` via MCP `claude-in-chrome` (`javascript_tool`), sonda injetada |
| Fundo efetivo | subida na árvore acumulando camadas, composição alpha-over, parada no primeiro `background-color` opaco; `background-image` registrado quando presente (guarda contra o falso positivo de `linear-gradient` da varredura anterior) |
| Contraste | WCAG 2.1, luminância relativa |
| Troca de tema | `localStorage['sistema-folha:tema']` + reload; tema ativo **reconfirmado por token computado** (`body` background + `fontFamily`) a cada troca |
| Capturas | ⚪ nenhuma salva — `Page.captureScreenshot` retornou timeout de CDP (30 s) nas duas tentativas. Toda a evidência acima é numérica, extraída por `getComputedStyle` e por leitura do bundle servido. |
| Restauração | tema devolvido a `indigo`; nenhuma chave de sonda criada no `localStorage` (a sonda viveu em `window`, e foi removida) |
| Dados do sistema | nenhum registro criado, editado, excluído ou formulário submetido |
