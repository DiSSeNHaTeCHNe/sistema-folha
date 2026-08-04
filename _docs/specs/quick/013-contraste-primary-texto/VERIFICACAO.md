# Verificação no navegador — quick tasks 012 e 013

Este documento tem **duas rodadas**:

| Rodada | Data | Alvo | Veredito |
| --- | --- | --- | --- |
| **1** (§1 a §8 + Anexo) | 2026-08-04 | bundle `index-82Qa2YrV.js` — **pré-012/013** | ❌ FAIL (não verificável) |
| **2** (§R2 em diante) | 2026-08-04, após republicação | bundle `index-DesManuh.js` — **com as duas quick tasks** | ❌ **FAIL** (2 defeitos medidos) |

O registro da rodada 1 é preservado na íntegra: ele explica por que a medição
atrasou e contém o inventário estático de consumidores de `.light`, que a
rodada 2 reaproveita e amplia.

---

## Rodada 1 — bloqueada (build antigo)

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

---
---

# §R2 — Rodada 2: medição no pixel do build novo

**Data**: 2026-08-04 (após o usuário republicar o alvo)
**Branch**: `feat/temas-fidelidade-visual` @ `65e9abc`
**Executado por**: sub-agente Verificador independente (não é o autor das quick tasks)
**Alvo**: `http://localhost:3000` — nginx do `docker-compose`, bundle estático,
sessão autenticada, dados reais. Viewport 1335×1266.

## R2.0 — Confirmação do alvo (primeiro passo obrigatório)

`fetch('/')` → um único `<script src="/assets/index-DesManuh.js">`
(**1 520 914 bytes**). Busca literal no corpo do bundle servido:

| Cor | Papel | Presente |
| --- | --- | --- |
| `#b05900` | `classico.warning.main` novo (quick 012) | ✅ 1× |
| `#1167F4` | `corporate.primary.main` novo (013) | ✅ 2× |
| `#188361` | `soft.primary.main` novo (013) | ✅ 2× |
| `#8078DD` | `indigo.primary.main` novo (013) | ✅ 2× |
| `#1873cd` | `classico.primary.main` novo (013) | ✅ 2× |
| `#E2EEEB`, `#E2EDFE`, `#E3F0EC`, `#EFE7FF`, `#2E2B50` | tints novos (012/013) | ✅ |

**O alvo é o build novo.** A rodada 1 media `index-82Qa2YrV.js`, que não continha
nenhum desses valores. Página recarregada com `cache: 'no-store'` antes de medir.

### Por que `#3B82F6`, `#1D9E75` e `#f57c00` ainda aparecem no bundle

Investigado — **os três são uso legítimo, nenhum é consumidor esquecido**:

| Hex | Ocorrências no bundle | Origem | Veredito |
| --- | --- | --- | --- |
| `#3B82F6` | 1 | `frontend/src/theme/themes.ts:17` — `CORPORATE_CHARTS[0]`, **paleta de gráficos** (`pieColors` do Dashboard), não é `primary.main` | legítimo |
| `#1D9E75` | 1 | `frontend/src/theme/themes.ts:53` — `SOFT_CHARTS[0]`, idem | legítimo |
| `#f57c00` | 1 | **não vem do código da aplicação**: `rg` em `frontend/src` só encontra o valor em comentários e testes (que não entram no bundle). Vem de `node_modules/@mui/material/colors/orange.js` — é `orange[700]` da paleta de cores do próprio MUI, arrastado pelo import de `@mui/material/colors` | legítimo |
| `#0F172A` | 2 | `corporate.chrome.bg` e a `amostra` do `AparenciaDialog` — deixou de ser `contrastText`, mas segue sendo o grafite da sidebar | legítimo |

Confirmação estática de que `primary.main` não ficou preso ao valor antigo em
lugar nenhum: `themes.test.ts:142` e `:153` asseguram
`palette.primary.main !== '#3B82F6'` / `!== '#1D9E75'`, e a medição no pixel
(§R2.2) só encontrou os valores novos como cor de texto.

---

## R2.1 — R-1: ícone × fundo do avatar de KPI (`/dashboard`, 6 avatares × 5 temas)

Medido com `getComputedStyle` no `<svg>` do avatar e no fundo efetivo do
`.MuiAvatar-root` (composição alpha subindo a árvore). Os 6 avatares têm
`background-image: none` e `background-color` opaco — **nenhum é badge de
gradiente**; os badges `#1`…`#5` do top-5 (10 elementos com
`linear-gradient(135deg, …)`, que geraram o falso positivo da varredura
anterior) foram excluídos por filtro (só entram avatares que contêm `<svg>`) e
foram reconfirmados como gradiente nesta rodada.

Coluna "antes" = `varredura-pos-fidelidade.md` §5, reproduzida com exatidão pela
mesma sonda na rodada 1.

| Avatar (KPI) | Papel | `classico` | `corporate` | `soft` | `indigo` | `techne` |
| --- | --- | --- | --- | --- | --- | --- |
| Total de Funcionários | `info` | 4,03 → **4,03** ✅ | 1,55 → **5,46** ✅ | 1,60 → **5,49** ✅ | 6,16 → **6,16** ✅ | 1,40 → **4,05** ✅ |
| Custo Empresa | `success` | 4,56 → **4,56** ✅ | 1,53 → **5,22** ✅ | 1,53 → **5,22** ✅ | 5,13 → **5,13** ✅ | 1,53 → **5,22** ✅ |
| Benefícios Ativos | `warning` | 2,47 → **4,48** ✅ | 1,57 → **5,63** ✅ | 1,57 → **5,63** ✅ | 5,28 → **5,28** ✅ | 1,54 → **5,35** ✅ |
| Relação P/D | `info` (ícone `info.dark`) | 6,93 → **6,93** ✅ | 2,43 → **8,58** ✅ | 2,51 → **8,63** ✅ | 3,15 → **3,15** ✅ | 2,38 → **6,90** ✅ |
| Top 5 Proventos | `success` | 4,56 → **4,56** ✅ | 1,53 → **5,22** ✅ | 1,53 → **5,22** ✅ | 5,13 → **5,13** ✅ | 1,53 → **5,22** ✅ |
| Top 5 Descontos | `error` | 4,92 → **4,92** ✅ | 1,50 → **5,83** ✅ | 1,54 → **5,79** ✅ | 5,59 → **5,59** ✅ | 1,50 → **5,83** ✅ |
| **≥ 3:1** | | 5/6 → **6/6** | 0/6 → **6/6** | 0/6 → **6/6** | 6/6 → **6/6** | 0/6 → **6/6** |

### **Antes: 11/30. Depois: 30/30 ≥ 3:1.** ✅ **R-1 fechado no pixel.**

Pares renderizados medidos (ícone / fundo), para rastreabilidade:

| Tema | Total Func. | Custo | Benefícios | Relação P/D | Top Prov. | Top Desc. |
| --- | --- | --- | --- | --- | --- | --- |
| `classico` | `#1976d2`/`#e3f2fd` | `#2e7d32`/`#e8f5e8` | **`#b05900`**/`#fff3e0` | `#115293`/`#e3f2fd` | `#2e7d32`/`#e8f5e8` | `#c62828`/`#ffebee` |
| `corporate` | `#185fa5`/**`#e3ecf4`** | `#0f6e56`/**`#e2eeeb`** | `#854f0b`/**`#f0eae2`** | `#104273`/**`#e3ecf4`** | `#0f6e56`/**`#e2eeeb`** | `#a32d2d`/**`#f4e6e6`** |
| `soft` | `#5f5e5a`/**`#ececeb`** | `#0f6e56`/**`#e2eeeb`** | `#854f0b`/**`#f0eae2`** | `#42413e`/**`#ececeb`** | `#0f6e56`/**`#e2eeeb`** | `#993c1d`/**`#f3e8e4`** |
| `indigo` | `#afa9ec`/`#2e2c4a` | `#5dcaa5`/`#23473c` | `#ef9f27`/`#4a3616` | `#7a76a5`/`#2e2c4a` | `#5dcaa5`/`#23473c` | `#f09595`/`#4a2c2c` |
| `techne` | `#0a7ab0`/**`#e2eff6`** | `#0f6e56`/**`#e2eeeb`** | `#8a5200`/**`#f1eae0`** | `#07557b`/**`#e2eff6`** | `#0f6e56`/**`#e2eeeb`** | `#a32d2d`/**`#f4e6e6`** |

Confirma o achado da rodada 1: **nenhum dos 6 avatares usa `primary`** — quem os
corrigiu foi a **012** (tints das semânticas). O `indigo` não mudou porque já
tinha `light` explícito e escuro (DD-3).

---

## R2.2 — D-5: `primary.main` como cor de texto, em elemento real

Sonda: varre `body *`, filtra elementos com nó de texto direto e visíveis, e
guarda os cuja **cor de primeiro plano computada** é exatamente o `primary.main`
do tema; mede contra o **fundo efetivo renderizado**.

Telas varridas nos 5 temas: `/folha-pagamento` (pior caso), `/relatorios`,
`/usuarios`, `/rubricas`, `/rubricas-fixas`, `/importacao`.
Contagem em `/folha-pagamento`: **22 elementos** nos 5 temas — mesma população da
`varredura-pos-fidelidade.md` §7.1 (7 valores `R$ …` 16px/400, 7 botões
"Ver Funcionários" 13px/500, 6 chips "Normal" 14px/400, "Filtrar" e "Limpar"
14px/500).

### Sobre `background.paper` (cards)

| Tema | `primary.main` | fundo do card | antes | depois | Veredito |
| --- | --- | --- | --- | --- | --- |
| `classico` | `#1873cd` | `#ffffff` | 4,37 ❌ | **4,80** | ✅ |
| `corporate` | `#1167f4` | `#ffffff` | 3,68 ❌ | **4,92** | ✅ |
| `soft` | `#188361` | `#ffffff` | 3,39 ❌ | **4,71** | ✅ |
| `techne` | `#7836fc` | `#ffffff` | 5,63 ✅ | **5,63** | ✅ |
| `indigo` | `#8078dd` | `#1c1c28` **em token**, `#272733` **renderizado** | 4,48 ❌ | **3,95** ❌ | ❌ **reprova** |

### Sobre `background.default` (barra de filtros, fora do card)

| Tema | fundo | antes | depois |
| --- | --- | --- | --- |
| `classico` | `#f8f9fa` | 4,20 | **4,56** ✅ |
| `corporate` | `#f4f6f8` | 3,39 | **4,54** ✅ |
| `soft` | `#fbfaf7` | 3,26 | **4,51** ✅ |
| `indigo` | `#12121a` | — | **5,01** ✅ |
| `techne` | `#eff2f7` | — | **5,01** ✅ |

### 🔴 Defeito 1 — o `indigo` não fecha o D-5 no pixel: o overlay de elevação

O cálculo da 013 (`4,53:1`) usa o **token** `background.paper` = `#1C1C28`. O que
o navegador pinta num `Card`/`Paper` do MUI em `mode: 'dark'` **não é `#1C1C28`**:
o MUI aplica um *overlay* de elevação como `background-image`:

```
background-image: linear-gradient(rgba(255,255,255,0.05), rgba(255,255,255,0.05));
background-color: rgb(28,28,40);
```

Composto, o fundo real do card é **`#272733`**, e `#8078DD` sobre ele rende
**3,95:1** — abaixo do piso AA de 4,5:1. Medido em `/folha-pagamento` (22
elementos), `/relatorios` (2), `/usuarios` (1), `/rubricas` (1),
`/rubricas-fixas` (1) e `/importacao` (4) — **todos a 3,95**.

Escala do overlay por elevação (medida no alvo, `primary.main` novo × antigo):

| Elevação | overlay | fundo real | `#8078DD` (novo) | `#7F77DD` (antigo) |
| --- | --- | --- | --- | --- |
| token puro (o que o teste assume) | — | `#1c1c28` | 4,53 ✅ | 4,48 |
| Card / Paper (0,05) | 5 % | `#272733` | **3,95** ❌ | 3,91 |
| AppBar (0,09) | 9 % | `#30303b` | 3,48 ❌ | 3,44 |
| Dialog (0,165) | 16,5 % | `#41414b` | 2,68 ❌ | 2,65 |

**Isto não é uma regressão** — o valor antigo era 3,91 e o novo é 3,95, ou seja, a
013 melhorou 0,04. **É o D-5 continuar aberto no `indigo`**, e é exatamente o
mesmo tipo de erro que originou R-1/R-2/D-5: o par medido pelo teste unitário
(`primary.main` × token `background.paper`) **não é o par renderizado** no tema
escuro. Para fechar seria preciso ou clarear `primary.main` até ~`#8F88E4`
(4,5:1 contra `#272733`), ou desligar o overlay
(`MuiPaper: { defaultProps: { elevation: 0 } }` / `styleOverrides.root: { backgroundImage: 'none' }`).

### 🟠 Defeito 2 (mesma causa, superfície elevada) — `primary` como ícone em `Dialog`

`AparenciaDialog` (`components/AparenciaDialog/index.tsx:90`) usa
`<CheckIcon color="primary">` para marcar o tema ativo. No `indigo`, o
`.MuiDialog-paper` tem overlay 0,165 → fundo `#41414b`, e o card do tema
selecionado fica em `#606068`. Medido: **`#8078dd` sobre `#606068` = 1,68:1**,
muito abaixo dos 3:1 de 1.4.11. Atenuante: a seleção também é sinalizada por
borda (`borderColor: 'primary.main'`) e pelo `outline`, então não há perda de
informação — mas o ícone é praticamente invisível. Nos 4 temas claros não há
overlay e o mesmo ícone fica em ≥ 4,7:1.

### **D-5: 4/5 temas ≥ 4,5:1 no pixel** (antes 1/5). `indigo` reprova em 3,95.

---

## R2.3 — Regressões causadas pela mudança de `.light`

Inventário estático refeito nesta rodada (`rg '\.light' frontend/src`, excluindo
`theme/` e testes) — bate com o da rodada 1, **9 consumidores**, nenhum novo.

### 🔴 Defeito 3 — ícone "Inativar" some no hover (`Funcionarios/index.tsx:556`)

A rodada 1 classificou esta linha como "⚪ não medido; par ícone×tint é o mesmo
varrido a 3:1". **Isso está errado** e o pixel prova: o ícone não é `error.main`,
é literalmente `color: 'white'`:

```tsx
// frontend/src/pages/Funcionarios/index.tsx:552-559
sx={{
  color: 'error.main',
  '&:hover': {
    backgroundColor: 'error.light',
    color: 'white',
  },
}}
```

Quando a **quick 012** transformou `error.light` de meio-tom em **tint claro**, o
ícone branco passou a ser branco-sobre-quase-branco. Medido com o mouse
realmente parado sobre o botão (com espera de 2 s para a transição de
`background-color` do `IconButton` concluir):

| Tema | fundo no hover (`error.light`) | ícone | razão | Veredito |
| --- | --- | --- | --- | --- |
| `classico` | `#ffebee` | `#ffffff` | **1,14** | ❌ invisível |
| `corporate` | `#f4e6e6` | `#ffffff` | **1,21** | ❌ invisível |
| `soft` | `#f3e8e4` | `#ffffff` | **1,20** | ❌ invisível |
| `techne` | `#f4e6e6` | `#ffffff` | **1,21** | ❌ invisível |
| `indigo` | `#4a2c2c` | `#ffffff` | 12,44 | ✅ (tint escuro) |

Evidência visual salva: `screenshot-1785869767193-10b74816.jpg` (tema
`corporate`, card "Adolfo Rodrigues Machado Junior" com o mouse sobre a lixeira —
o ícone aparece como um vulto branco sobre o rosa claro).

É **regressão introduzida pela quick 012 e não capturada pela 013**, que corrigiu
o botão vizinho (`:534`) pelo mesmo motivo e parou ali.

### ✅ O que a 013 corrigiu está certo no pixel (`Funcionarios/index.tsx:534`)

Ícone "Editar" no hover, `color: 'primary.main'` sobre `bgcolor: 'primary.light'`:

| Tema | fundo (`primary.light`) | ícone (`primary.main`) | razão | Veredito |
| --- | --- | --- | --- | --- |
| `classico` | `#e3eef9` | `#1873cd` | **4,09** | ✅ ≥ 3:1 |
| `corporate` | `#e2edfe` | `#1167f4` | **4,16** | ✅ |
| `soft` | `#e3f0ec` | `#188361` | **4,02** | ✅ |
| `indigo` | `#2e2b50` | `#8078dd` | **3,58** | ✅ |
| `techne` | `#efe7ff` | `#7836fc` | **4,70** | ✅ |

Bate com o par varrido por `contraste.test.ts` (`primary.main × primary.light`).

### 🟡 Defeito 4 (R-3 confirmado, visual e não de contraste) — `Alert` descolorido

`Alert` standard do MUI deriva de `.light`: `bg = lighten(light, 0.9)` e
`color = darken(light, 0.6)` (modo claro). Com `light` virando um **tint a 88 % de
branco**, `lighten(tint, 0.9)` fica **quase branco** e `darken(tint, 0.6)` fica
**cinza neutro** — o alerta perde a cor semântica. Medido em `/api-keys`
(2 `Alert` standard renderizados sem interação):

| Tema | Alert `info` — texto / ícone | fundo do Alert | Alert `warning` — texto / ícone | fundo |
| --- | --- | --- | --- | --- |
| `classico` | `#5a6065` 6,26 ✅ / `#1976d2` 4,52 ✅ | `#fcfdfe` | `#666159` 6,05 ✅ / `#b05900` 4,84 ✅ | `#fffdfb` |
| `corporate` | `#5a5e61` 6,42 ✅ / `#185fa5` 6,40 ✅ | `#fcfdfd` | `#605d5a` 6,39 ✅ / `#854f0b` 6,57 ✅ | `#fdfcfc` |
| `soft` | `#5e5e5e` 6,37 ✅ / `#5f5e5a` 6,38 ✅ | `#fdfdfd` | `#605d5a` 6,39 ✅ / `#854f0b` 6,57 ✅ | `#fdfcfc` |
| `indigo` | `#abaab6` 8,93 ✅ / `#afa9ec` 9,47 ✅ | `#040407` | `#b6aea1` 9,27 ✅ / `#ef9f27` 9,36 ✅ | `#070502` |
| `techne` | `#5a5f62` 6,35 ✅ / `#0a7ab0` 4,66 ✅ | `#fcfdfe` | `#605d59` 6,39 ✅ / `#8a5200` 6,23 ✅ | `#fdfcfb` |

**Nenhum reprova em contraste** — o texto ganhou contraste, inclusive. O problema
é de identidade visual: em `corporate`/`soft`/`techne` o fundo do alerta é
indistinguível do card (`#fcfdfd` sobre `#ffffff`) e o texto é cinza; a severidade
só se lê pelo ícone, que mantém `.main`. No `indigo` o fundo vira **`#040407`**,
mais escuro que o `background.default` do tema. Registrado como **defeito visual
de baixa severidade**, não como falha de acessibilidade.
`Alert` outlined (que usa `border: 1px solid palette[X].light`) **não é usado em
nenhuma tela** — a busca não encontrou nenhum `MuiAlert-outlined*` renderizado.

### Consumidores de `.light` não cobertos (com o motivo)

| Local | Motivo |
| --- | --- |
| `Organograma/index.tsx:617` (`primary.light` em `isOver`) | ⚪ **não coberto** — só aparece durante drag-and-drop de nó, que **alteraria dados**. A tela foi aberta e varrida em repouso nos 5 temas: nenhum elemento com `primary.main` como texto, nenhum corte. |
| `DashboardCustomizavel/widgets/{KpiWidget,TopRubricasWidget}.tsx` | ⚪ **não coberto** — a rota `dashboard-v2` não existe em `routes/index.tsx`; componentes sem tela. |

### Estados forçados

| Estado | Como foi forçado | Resultado |
| --- | --- | --- |
| `hover` — ícone Editar (`primary.light`) | mouse real do MCP sobre o botão, 2 s de espera | ✅ 3,58–4,70 nos 5 temas (tabela acima) |
| `hover` — ícone Inativar (`error.light`) | idem | ❌ 1,14–1,21 em 4 temas (**Defeito 3**) |
| `hover` — linha de tabela (`/rubricas`, `indigo`) | mouse real sobre `tbody tr` | fundo permanece `rgba(0,0,0,0)` (a linha não tem estilo de hover); texto 14,69, ícone `primary` 3,95 ✅, ícone `error` 6,60 ✅ |
| `selected` — item da sidebar | leitura de `.Mui-selected` e dos itens do `Drawer` | sidebar usa tokens `chrome.*`, **intocados pelas quick tasks**: texto `#8a88a3`/`#0c0c12` = 5,69 ✅, ícones 19,5 ✅ |
| `focus` (teclado) | 3× `Tab` a partir do topo + `element.focus()` em botão `contained` | `Mui-focusVisible` aplicado, mas **sem `outline` e sem `box-shadow` próprio** nos itens do `Drawer` — indicador de foco fraco. É comportamento pré-existente do tema (nenhuma quick task toca `focusVisible`); **não é regressão**, fica registrado. Botão `contained` em foco mantém 4,92 (corporate). |
| `disabled` | botão "Nova API Key" em `/api-keys` | `#9c9ea1` sobre `#d2d5d9` = **1,83** — é o `action.disabled` padrão do MUI, **não usa `primary`**; idêntico antes e depois. Pré-existente, fora do escopo. |

---

## R2.4 — `primary.contrastText` no botão preenchido real

Medido no `<button class="MuiButton-containedPrimary">` de `/funcionarios`
("Novo Funcionário" e "Filtrar"), cor de texto computada × fundo do próprio botão:

| Tema | `contrastText` renderizado | fundo (`primary.main`) | razão | Esperado pela 013 | Veredito |
| --- | --- | --- | --- | --- | --- |
| `classico` | `#ffffff` (derivado) | `#1873cd` | **4,80** | 4,80 | ✅ |
| `corporate` | **`#ffffff`** (era `#0F172A`) | `#1167f4` | **4,92** | 4,92 | ✅ |
| `soft` | **`#ffffff`** (era `#0F172A`) | `#188361` | **4,71** | 4,71 | ✅ |
| `indigo` | `#12121a` | `#8078dd` | **5,01** | 5,01 | ✅ |
| `techne` | `#ffffff` | `#7836fc` | **5,63** | 5,63 | ✅ |

**5/5 confirmados no pixel**, exatamente nos valores calculados pela quick task.
A troca de `contrastText` de `corporate` e `soft` para `#FFFFFF` está de fato no ar.

---

## R2.5 — Escala tipográfica: sem regressão

`getComputedStyle` em `/dashboard` e `/funcionarios`, nos 5 temas:

| Tema | `h4` (título de página) | `h3` (maior KPI) | `MuiTypography-h6` (título de card) | `fontFamily` |
| --- | --- | --- | --- | --- |
| `classico` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `-apple-system` |
| `corporate` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `soft` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `indigo` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `techne` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Poppins` |

Bate com a `varredura-pos-fidelidade.md` §1. Nota de método: um seletor `h6` cru
pega também `subtitle1`/`subtitle2` renderizados como `<h6>` (16px/400 e
14px/500) — o título de card é o `.MuiTypography-h6`, medido acima.
Nenhum corte, sobreposição ou ilegibilidade nova observada nas telas percorridas
(`/dashboard`, `/funcionarios`, `/folha-pagamento`, `/relatorios`, `/usuarios`,
`/rubricas`, `/rubricas-fixas`, `/importacao`, `/api-keys`, `/organograma`) —
verificação por captura de tela e por varredura de `getBoundingClientRect`
(nenhum elemento medido com largura ou altura zero entre os varridos).

---

## R2.6 — Gate

`cd frontend && npm run lint && npm run test && npm run build` em `65e9abc`:

| Etapa | Resultado |
| --- | --- |
| `npm run lint` | ✅ **0 erros**, 15 warnings pré-existentes (`react-hooks/exhaustive-deps` ×6, `react-refresh/only-export-components` ×9) |
| `npm run test` | ✅ **647 passed, 0 failed** em **44 arquivos** |
| `npm run build` | ✅ `tsc -b` exit 0; `vite build` exit 0 — `dist/assets/index-Be1EM4_W.js` (1 521,64 kB) |

Suíte executada em blocos por caminho (o shell do ambiente de verificação tem
timeout de 45 s):

| Bloco | Arquivos | Testes |
| --- | --- | --- |
| `src/theme` | 8 | 121 ✅ |
| `src/components` + `src/contexts` | 6 | 60 ✅ |
| `src/utils` `src/hooks` `src/lib` `src/routes` `src/services` `src/test` `src/smoke.test.tsx` | 16 | 91 ✅ |
| `src/pages/{ApiKeys,BeneficiosMensais,Cargos,CentrosCusto,Dashboard,DashboardCustomizavel}` | 4 | 98 ✅ |
| `src/pages/{FolhaPagamento,Funcionarios,Importacao,LinhasNegocio,Login}` | 4 | 130 ✅ |
| `src/pages/{Organograma,TiposBeneficio,Usuarios}` | 2 | 61 ✅ |
| `src/pages/{Relatorios,Rubricas,RubricasFixas}` | 4 | 86 ✅ |
| **Total** | **44** | **647 passed, 0 failed** |

**O gate passa.** Note que isso é justamente o ponto dos defeitos 1 e 2: a suíte
verde **não vê** nem o overlay de elevação do modo escuro (mede o token) nem o
`color: 'white'` do hover em `Funcionarios:556`.

### Nota sobre o hash do bundle

O bundle servido (`index-DesManuh.js`, 1 520 914 B) **não é byte a byte** o
produzido localmente por este gate (`index-Be1EM4_W.js`, 1 521 642 B) — 728 bytes
de diferença, esperada por ser outro ambiente de build (imagem Docker). O **código
de tema é o mesmo**: a contagem de cada hex relevante é idêntica nos dois arquivos
(`#b05900` 1×, `#1167F4` 2×, `#188361` 2×, `#8078DD` 2×, `#E2EEEB` 3×, `#1873cd`
2×, `#3B82F6` 1×, `#1D9E75` 1×, `#f57c00` 1×) e todos os valores medidos no
navegador batem com os calculados pelas quick tasks até a segunda casa.

---

## R2.7 — Veredito final da rodada 2

### ❌ **FAIL**

**O que passou** (e está provado no pixel):

- ✅ **R-1 fechado**: 30/30 pares ícone × avatar ≥ 3:1 (antes 11/30). A quick 012
  entregou o que prometeu nos 5 temas.
- ✅ **`primary.contrastText`**: 5/5 no botão `contained` real, nos valores exatos.
- ✅ **`Funcionarios:534`** (o consumidor de `primary.light` que a 013 corrigiu):
  3,58–4,70 nos 5 temas, ≥ 3:1.
- ✅ **D-5 fechado em 4 dos 5 temas** (`classico`, `corporate`, `soft`, `techne`),
  em elemento real, nas 6 telas, sobre `paper` **e** sobre `default`.
- ✅ **Escala tipográfica intacta**; nenhum corte ou sobreposição novo.
- ✅ **Alerts** não reprovam em contraste em nenhum tema.

**O que reprova**:

| # | Defeito | Severidade | Evidência |
| --- | --- | --- | --- |
| **1** | `indigo`: `primary.main` como texto rende **3,95:1** sobre o card, não 4,53 — o MUI pinta um overlay de elevação `rgba(255,255,255,0.05)` sobre `background.paper`, e o teste unitário mede o token, não o pixel | 🔴 **D-5 continua aberto no `indigo`** | 30 elementos em 6 telas, todos a 3,95 |
| **2** | `Funcionarios/index.tsx:556`: ícone "Inativar" no `hover` é `color: 'white'` sobre `error.light`, que a 012 tornou tint claro → **1,14–1,21:1** em `classico`/`corporate`/`soft`/`techne` | 🔴 **regressão introduzida pela 012** | medição com mouse real + captura |
| **3** | `AparenciaDialog`: `<CheckIcon color="primary">` no `indigo` rende **1,68:1** sobre o `Dialog` elevado | 🟠 mesma causa do #1; atenuado por borda redundante | medido no diálogo aberto |
| **4** | `Alert` standard perde a cor semântica sob os tints novos (fundo quase branco / quase preto, texto cinza) | 🟡 visual, não de acessibilidade | `/api-keys`, 5 temas |

### Continua aberto

| # | Item | Estado |
| --- | --- | --- |
| 1 | D-5 no `indigo` — decidir entre clarear `primary.main` (~`#8F88E4`) ou desligar o overlay de elevação (`MuiPaper.styleOverrides.root.backgroundImage: 'none'`) | 🔴 |
| 2 | `Funcionarios/index.tsx:556` — trocar `color: 'white'` por `error.main` no hover, espelhando o que a 013 fez em `:534` | 🔴 |
| 3 | Ajustar `contraste.test.ts` para medir o **fundo efetivo do tema escuro** (paper + overlay), e não só o token — senão o mesmo erro se repete | 🟠 |
| 4 | `AparenciaDialog:90` — ícone de tema ativo no `indigo` | 🟠 |
| 5 | Descoloração do `Alert` sob tints (R-3) | 🟡 |
| 6 | `Organograma:617` (`isOver`) | ⚪ não coberto — exige drag-and-drop, que alteraria dados |
| 7 | `KpiWidget` / `TopRubricasWidget` | ⚪ não coberto — rota `dashboard-v2` inexistente |
| 8 | `Alert` em `/relatorios` | ⚪ **não coberto** — nenhum `Alert` renderizou no estado de dados desta sessão |
| 9 | Indicador de `focus` fraco nos itens do `Drawer` (sem `outline`/`box-shadow`) | 🟡 pré-existente, fora do escopo das quick tasks |
| 10 | Botão `disabled` a 1,83:1 (`action.disabled` padrão do MUI) | 🟡 pré-existente, fora do escopo |
| 11 | Rótulo de papel dos avatares na `varredura-pos-fidelidade.md` §5 ("Total de Funcionários" e "Relação P/D" são `info`, não `primary`) | 🟡 correção de documentação, reconfirmada nesta rodada |

---

## Anexo R2 — método

| Item | Valor |
| --- | --- |
| Alvo | `http://localhost:3000` (nginx, `docker-compose`), sessão autenticada, dados reais |
| Bundle medido | `/assets/index-DesManuh.js`, 1 520 914 bytes — **contém as cores das duas quick tasks** |
| Instrumento | `getComputedStyle` via MCP `claude-in-chrome` (`javascript_tool`), sonda injetada |
| Fundo efetivo | subida na árvore compondo, por nó, `background-color` **e** `background-image` quando o gradiente é de cor única (é assim que o MUI pinta o overlay de elevação do modo escuro); `background-image` de múltiplas cores é marcado `NONFLAT` e o elemento é excluído em vez de gerar número — foi essa guarda que expôs o Defeito 1 e que evitou o falso positivo dos badges do top-5 |
| Contraste | WCAG 2.1, luminância relativa; piso 4,5:1 para texto e 3:1 para ícone/gráfico |
| Estados | `hover` com o mouse real do MCP + 2 s de espera (o `IconButton` do MUI **transiciona** `background-color`: ler logo após o `hover` devolve o fundo pré-transição — erro cometido e corrigido nesta rodada); `focus` por `Tab` e `element.focus()` |
| Troca de tema | `localStorage['sistema-folha:tema']` + reload; tema ativo **reconfirmado a cada medição** por token computado (`body` background + `fontFamily`), nunca assumido |
| Capturas | `screenshot-1785869767193-10b74816.jpg` (hover do ícone Inativar em `corporate`) e captura do `AparenciaDialog` no `indigo`. `Page.captureScreenshot` funcionou nesta rodada. |
| Restauração | tema devolvido a **`indigo`**; sondas removidas do `sessionStorage` (`__probe013`, `__probe013v2`, `__probe013v3`) e de `window` |
| Dados do sistema | **nenhum** registro criado, editado, excluído; nenhum formulário submetido. Único diálogo aberto foi o `AparenciaDialog`, fechado com `Esc` |
