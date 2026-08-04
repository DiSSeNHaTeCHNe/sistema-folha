# Verificação no navegador — quick tasks 012, 013 e 014

Continuação de `_docs/specs/quick/013-contraste-primary-texto/VERIFICACAO.md`
(rodadas 1 e 2). Este arquivo registra a **rodada 3**.

---

# §R3 — Rodada 3: medição no pixel do build com a quick 014

**Data**: 2026-08-04
**Branch**: `feat/temas-fidelidade-visual` @ `02a7e12`
**Executado por**: sub-agente Verificador independente (não é o autor das quick tasks)
**Alvo**: `http://localhost:3000` — nginx do `docker-compose`, bundle estático,
sessão autenticada, dados reais. Viewport 1335×1322.
**Regra**: evidência-ou-zero. Nenhuma correção de código; só medição, julgamento e registro.

## R3.0 — Confirmação do alvo (primeiro passo obrigatório)

`fetch('/', {cache:'no-store'})` → um único `<script src="/assets/index-BhNIQgTr.js">`,
**1 521 120 bytes**, relido com `cache: 'no-store'` antes de qualquer medição.
Busca literal no corpo do bundle servido:

| Marcador | Papel | Ocorrências |
| --- | --- | --- |
| `#b05900` | `classico.warning.main` (quick 012) | 1 ✅ |
| `#1167F4` / `#188361` / `#8078DD` / `#1873cd` | `primary.main` novos (quick 013) | 2 cada ✅ |
| `backgroundImage:"none"` | **quick 014** — `MuiPaper.styleOverrides.root` | 2 ✅ |
| `MuiPopover` | **quick 014** — borda da superfície flutuante | 6 ✅ |
| `MuiMenu:` | idem | 1 ✅ |

**O alvo serve o código da 014.** Contraprova de equivalência com o repositório: o
`dist/` reconstruído neste gate a partir de `02a7e12`
(`index-DG9kbxdG.js`, 1 521 848 B) tem **exatamente as mesmas contagens** dos oito
marcadores acima. Os 728 bytes de diferença são de ambiente de build (imagem
Docker × este shell), como na rodada 2.

### Nota de método — o overlay continua declarado, mas não é pintado

`--Paper-overlay` **continua sendo injetado inline** pelo MUI v7 em todo `Paper`
do `indigo` (`0.051` no Card, `0.092` no AppBar, `0.147` no Drawer temporário,
`0.165` no Dialog). O que a 014 fez foi neutralizá-lo: `background-image` computa
`none` em todos eles. Medi `--Paper-overlay` **e** `background-image` em cada
superfície justamente para não confundir "a variável sumiu" com "o gradiente não
pinta". É o segundo que importa, e é o que está confirmado.

---

## R3.1 — Confirmação de que a rodada 2 não regrediu

### R-1 — ícone × fundo do avatar de KPI (`/dashboard`, 6 avatares × 5 temas)

Sonda: `getComputedStyle` no `<svg>` do avatar × fundo efetivo do `.MuiAvatar-root`
(composição alpha subindo a árvore). Os 6 avatares têm `background-image: none` e
`background-color` opaco — nenhum é badge de gradiente; os badges `#1`…`#5` do
top-5 continuam excluídos por filtro (só entram avatares que contêm `<svg>`).

| Avatar (KPI) | Papel | `classico` | `corporate` | `soft` | `indigo` | `techne` |
| --- | --- | --- | --- | --- | --- | --- |
| Total de Funcionários | `info` | 4,03 | 5,46 | 5,49 | 6,16 | 4,05 |
| Custo Empresa | `success` | 4,56 | 5,22 | 5,22 | 5,13 | 5,22 |
| Benefícios Ativos | `warning` | 4,48 | 5,63 | 5,63 | 5,28 | 5,35 |
| Relação P/D | `info` (ícone `info.dark`) | 6,93 | 8,58 | 8,63 | 3,15 | 6,90 |
| Top 5 Proventos | `success` | 4,56 | 5,22 | 5,22 | 5,13 | 5,22 |
| Top 5 Descontos | `error` | 4,92 | 5,83 | 5,79 | 5,59 | 5,83 |
| **≥ 3:1** | | **6/6** | **6/6** | **6/6** | **6/6** | **6/6** |

### ✅ **30/30 ≥ 3:1 — valor por valor idêntico à rodada 2.** Sem regressão.

### `primary.contrastText` no botão preenchido real

`<button class="MuiButton-containedPrimary">` "Novo Funcionário" em `/funcionarios`,
cor de texto computada × fundo do próprio botão:

| Tema | `contrastText` renderizado | fundo (`primary.main`) | razão | rodada 2 |
| --- | --- | --- | --- | --- |
| `classico` | `#ffffff` | `#1873cd` | **4,80** | 4,80 ✅ |
| `corporate` | `#ffffff` | `#1167f4` | **4,92** | 4,92 ✅ |
| `soft` | `#ffffff` | `#188361` | **4,71** | 4,71 ✅ |
| `indigo` | `#12121a` | `#8078dd` | **5,01** | 5,01 ✅ |
| `techne` | `#ffffff` | `#7836fc` | **5,63** | 5,63 ✅ |

**5/5 confirmados.** Sem regressão.

### Escala tipográfica

`getComputedStyle` em `/dashboard`, nos 5 temas:

| Tema | `.MuiTypography-h4` | `.MuiTypography-h3` | `.MuiTypography-h6` | `fontFamily` |
| --- | --- | --- | --- | --- |
| `classico` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `-apple-system` |
| `corporate` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `soft` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `indigo` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Roboto` |
| `techne` | 24px/600 ✅ | 27px/600 ✅ | 16px/600 ✅ | `Poppins` |

**Sem regressão.**

---

## R3.2 — O que a 014 alega ter consertado

### Defeito 1 — D-5 no `indigo`: `primary.main` como texto sobre o Card

Sonda: varre `body *`, filtra elementos visíveis com nó de texto direto cuja **cor
de primeiro plano computada** é exatamente `primary.main`, e mede contra o **fundo
efetivo renderizado**.

**Fundo computado do Card, `indigo`, nas 6 telas:**
`background-color: rgb(28,28,40)`, `background-image: none`, `--Paper-overlay:
linear-gradient(rgba(255,255,255,0.051), …)` → **fundo efetivo `#1c1c28`**.

### ✅ **O fundo computado é o token.** Era `#272733` na rodada 2.

| Tela | elementos com `primary.main` sobre o Card | antes (R2) | **agora** |
| --- | --- | --- | --- |
| `/folha-pagamento` | 20 (`R$ …` 16px/400 ×7, "Ver Funcionários" 13px/500 ×7, chip "Normal" 14px/400 ×6) | 3,95 ❌ | **4,53** ✅ |
| `/relatorios` | 2 | 3,95 ❌ | **4,53** ✅ |
| `/usuarios` | 1 | 3,95 ❌ | **4,53** ✅ |
| `/rubricas` | 1 | 3,95 ❌ | **4,53** ✅ |
| `/rubricas-fixas` | 1 | 3,95 ❌ | **4,53** ✅ |
| `/importacao` | 4 | 3,95 ❌ | **4,53** ✅ |
| **Total** | **29 elementos** | **todos 3,95** | **todos 4,53** |

Mais 2 elementos em `/folha-pagamento` ("Filtrar", "Limpar" 14px/500) sobre
`background.default` `#12121a` a **5,01** ✅.
Contagem total em `/folha-pagamento`: **22 elementos**, a mesma população da
rodada 2 e da `varredura-pos-fidelidade.md` §7.1.

**Sobre o Dialog** (`.MuiDialog-paper`, elevação 24): `background-color:
rgb(28,28,40)`, `background-image: none`, `--Paper-overlay` α 0,165 presente e
neutralizado → fundo efetivo **`#1c1c28`** (era `#41414b`).
`#8078dd` sobre ele = **4,53** (era **2,68**). Medido em **elemento real**: o
`FormDialog` "Nova Rubrica" de `/rubricas`, aberto e fechado com `Esc` sem
submeter — 1 elemento com `primary.main` como texto, a **4,53**.

**D-5 nos quatro temas claros** (`/folha-pagamento`, 22 elementos cada) — mantido:

| Tema | sobre `background.paper` | sobre `background.default` |
| --- | --- | --- |
| `classico` | `#1873cd` / `#ffffff` = **4,80** ✅ | `#f8f9fa` = **4,56** ✅ |
| `corporate` | `#1167f4` / `#ffffff` = **4,92** ✅ | `#f4f6f8` = **4,54** ✅ |
| `soft` | `#188361` / `#ffffff` = **4,71** ✅ | `#fbfaf7` = **4,51** ✅ |
| `techne` | `#7836fc` / `#ffffff` = **5,63** ✅ | `#eff2f7` = **5,01** ✅ |

### **D-5: 5/5 temas ≥ 4,5:1 no pixel** (rodada 2: 4/5; pré-012/013: 1/5). ✅ **Fechado.**

### Defeito 2 — `Funcionarios/index.tsx:557`, ícone "Inativar" no hover

Medido por **dois instrumentos independentes**, que coincidem onde ambos se
aplicam (ver Anexo R3, "Estados"):

1. mouse real do MCP parado sobre o botão + 2–3 s de espera (`indigo`, `corporate`);
2. leitura da **regra `:hover` gerada pelo Emotion** em `document.styleSheets`
   para a classe `css-…` do próprio botão renderizado (5 temas).

| Tema | fundo no hover (`error.light`) | ícone antes (R2) | razão antes | **ícone agora** | **razão agora** |
| --- | --- | --- | --- | --- | --- |
| `classico` | `#ffebee` | `white` | **1,14** ❌ | `#c62828` (`error.main`) | **4,92** ✅ |
| `corporate` | `#f4e6e6` | `white` | **1,21** ❌ | `#a32d2d` | **5,83** ✅ |
| `soft` | `#f3e8e4` | `white` | **1,20** ❌ | `#993c1d` | **5,79** ✅ |
| `indigo` | `#4a2c2c` | `white` | 12,44 | `#f09595` | **5,59** ✅ |
| `techne` | `#f4e6e6` | `white` | **1,21** ❌ | `#a32d2d` | **5,83** ✅ |

Bate com os valores calculados no `TASK.md` §2 até a segunda casa, nos cinco temas.
A sonda também verifica explicitamente que **nenhuma regra `:hover` do botão declara
`color: white` / `rgb(255,255,255)`** — a fresta está fechada no CSS renderizado, não
só no fonte.

**Vizinho "Editar" (`:534`, corrigido pela 013) — não regrediu:**

| Tema | fundo (`primary.light`) | ícone (`primary.main`) | razão | rodada 2 |
| --- | --- | --- | --- | --- |
| `classico` | `#e3eef9` | `#1873cd` | **4,09** ✅ | 4,09 |
| `corporate` | `#e2edfe` | `#1167f4` | **4,16** ✅ | 4,16 |
| `soft` | `#e3f0ec` | `#188361` | **4,02** ✅ | 4,02 |
| `indigo` | `#2e2b50` | `#8078dd` | **3,58** ✅ | 3,58 |
| `techne` | `#efe7ff` | `#7836fc` | **4,70** ✅ | 4,70 |

### Defeito 3 — `AparenciaDialog`, `CheckIcon` do tema selecionado

Diálogo aberto pelo menu de conta nos 5 temas e fechado com `Esc`. Medido no
`<svg>` do `CheckIcon` × fundo efetivo do card do tema ativo:

| Tema | fundo antes (`action.selected`) | razão antes | **fundo agora (`primary.light`)** | **razão agora** |
| --- | --- | --- | --- | --- |
| `classico` | `#EBEBEB` | 4,03 | `#e3eef9` | **4,09** ✅ |
| `corporate` | `#EBEBEB` | 4,13 | `#e2edfe` | **4,16** ✅ |
| `soft` | `#EBEBEB` | 3,95 | `#e3f0ec` | **4,02** ✅ |
| `indigo` | `#606068` | **1,68** ❌ | `#2e2b50` | **3,58** ✅ |
| `techne` | `#EBEBEB` | 4,72 | `#efe7ff` | **4,70** ✅ |

**5/5 ≥ 3:1** (WCAG 1.4.11). O fundo do card selecionado é **opaco** e igual ao token
`primary.light` nos cinco temas — não há mais camada translúcida a compor.
A seleção continua tripla: borda `primary.main` (`rgb(128,120,221)` no `indigo`),
fundo `primary.light` e o `CheckIcon`.
Captura: `outputs/screenshot-1785876266105-0d113bd5.jpg`.

---

## R3.3 — O efeito colateral que a 014 introduziu

### Menu / Select / Popover

| Tema | fundo do `.MuiMenu-paper` | `background-image` | borda | separação vs Card | borda vs paper | sombra |
| --- | --- | --- | --- | --- | --- | --- |
| `classico` | `#ffffff` | none | `0px none` | **1,00** | — | visível |
| `corporate` | `#ffffff` | none | `0px none` | **1,00** | — | visível |
| `soft` | `#ffffff` | none | `0px none` | **1,00** | — | visível |
| `techne` | `#ffffff` | none | `0px none` | **1,00** | — | visível |
| `indigo` | `#1c1c28` | none | **`1px solid rgb(42,42,56)`** (`divider`) | **1,00** | **1,19** | invisível no escuro |

- **A borda existe e só no modo escuro**, exatamente como a 014 declara.
  Confirmada em duas superfícies distintas: o **menu de conta** do `AppBar`
  (`MuiPopover-paper MuiMenu-paper MuiPaper-elevation8`) e o **`Select` "Status"**
  do filtro de `/funcionarios`, que abre parcialmente sobre o Card de filtros e
  parcialmente sobre `background.default`.
- **Veredito visual: passa, com margem fina.** Nas capturas o contorno é
  discernível — mas 1,19:1 é o único separador no escuro, já que a sombra do MUI é
  preta. Onde o menu extravasa o Card, o próprio `background.default` (`#12121a`,
  1,11 contra o paper) reforça a leitura. **Funciona; não é folgado.**
- Nos quatro temas claros a razão também é 1,00 — mas isso é **anterior à 014**
  (`#ffffff` sobre `#ffffff` já era o caso) e ali a sombra preta é visível.
- Capturas: `outputs/screenshot-1785876485386-77b606fc.jpg` (`Select` aberto no
  `indigo`, sobre o Card e sobre o fundo da página).

### Dialog contra o scrim

Backdrop real medido: `rgba(0, 0, 0, 0.5)`, visível, cobrindo 1335px.

| | fundo do Dialog | scrim efetivo | separação |
| --- | --- | --- | --- |
| antes (R2) | `#41414b` | `#09090d` | **1,97** |
| **agora** | `#1c1c28` | `#09090d` | **1,18** |

Confirma o número declarado pela 014. **Veredito visual: continua distinguível** —
o scrim escurece 50 % de *tudo* que está atrás, então a fronteira do diálogo é lida
pelo contraste entre "área escurecida" e "área não escurecida", não pelo par de
cores. Verificado na captura do `AparenciaDialog` no `indigo`. ✅ sem ação.

### Drawer permanente e AppBar

| Superfície | elevação | `--Paper-overlay` | `background-image` | fundo efetivo | veredito |
| --- | --- | --- | --- | --- | --- |
| Drawer permanente (`indigo`) | 0 | α 0 | none | `#0c0c12` = `chrome.bg` | ✅ inalterado |
| Drawer **temporário** (`indigo`) | 16 | α 0,147 | none | `#0c0c12` = `chrome.bg` | ✅ neutralizado |
| AppBar (`indigo`) | 4 | α 0,092 | none | `#0c0c12` = `chrome.bg` | ✅ era `#222228` |

**AppBar, `chrome.fg` sobre o fundo real:** `#8a88a3` / `#0c0c12` = **5,69**
(era 4,62). ✅ **Melhora confirmada no valor exato declarado** — passa a bater com o
par `chrome.fg / chrome.bg` que o teste já assertava.

AppBar nos demais temas (medido, para registro): `classico` `#ffffff`/`#1873cd` =
4,80 · `corporate` `#94a3b8`/`#0f172a` = 6,96 · `soft` `#5f5e5a`/`#f4f2ec` = 5,80 ·
`techne` `#b8c0d4`/`#20284e` = 7,82.

### 🟠 Superfícies elevadas que a 014 **não** tratou

| # | Superfície | Situação | Severidade |
| --- | --- | --- | --- |
| **A** | **`MuiPickersPopper`** — popup do `DatePicker` (`pages/Relatorios/CompetenciaPicker.tsx`, `components/DateField`) | É `styled(Paper)`: **recebe** `backgroundImage: 'none'`, portanto no `indigo` passa a pintar `#1c1c28`. Mas **não é `MuiPopover` nem `MuiMenu`**, logo **não recebe a borda de 1px** que a 014 adicionou. No escuro fica sem contorno, com a sombra preta invisível — exatamente o cenário que a borda existe para evitar. | 🟠 **gap real** |
| **B** | `MuiAutocomplete-paper` (`components/AutocompleteField`) | Mesma classe de problema (Popper, não Popover/Menu). **Sem risco atual**: nenhum arquivo importa o componente — está sem tela, como `KpiWidget`. | ⚪ |
| **C** | `Tooltip` (`ApiKeys:263`, `Dashboard:320`) | `.MuiTooltip-tooltip` **não é `Paper`** (fundo próprio `rgba(97,97,97,0.92)`); a 014 não o toca e ele não depende de contraste com a superfície sob si. | ⚪ sem risco |
| **D** | `Snackbar` (`components/Notification`) | Só renderiza após ação que altera dados. | ⚪ fora do escopo |
| **E** | `Accordion` | Confirmado: **não usado em nenhuma tela** (`grep '<Accordion'` = 0 ocorrências). | ✅ n/a |
| **F** | **Card dentro de Dialog** | Existe: os 5 cards de tema do `AparenciaDialog`. No `indigo` ficam `#1c1c28` sobre Dialog `#1c1c28` (**1,00**) — mas o componente já lhes dá `border: 1px` em `divider` (`rgb(42,42,56)`), então ficam delimitados. | ✅ coberto por acaso |
| **G** | `Paper elevation 3` do Login | ⚪ **não coberto** — exigiria abandonar a sessão autenticada. O nível 3 é coberto pela varredura nova de `contraste.test.ts`. | ⚪ |

**Sobre o item A — o que foi e o que não foi medido.** O gap está identificado por
leitura de código (`tokens.ts` só declara `MuiPopover` e `MuiMenu`). O **pixel não
foi medido**: o popper do `DatePicker` de `/relatorios` **não montou** no alvo em
nenhuma das quatro tentativas (clique real do MCP na coordenada verificada, clique
real duplo, `element.click()`, e `focus()` + `Enter`) — `.MuiPickersPopper-root`
nunca aparece no DOM e nenhum portal novo é criado em `document.body`. Registrado
como **não coberto por pixel, com causa conhecida**, e não como aprovado.

---

## R3.4 — Estados forçados

| Estado | Como foi forçado | Resultado |
| --- | --- | --- |
| `hover` — ícone Editar (`primary.light`) | mouse real do MCP + espera; e regra `:hover` do Emotion | ✅ **3,58–4,70** nos 5 temas |
| `hover` — ícone Inativar (`error.light`) | idem | ✅ **4,92–5,83** nos 5 temas (era 1,14–1,21 em quatro) |
| `focus` (teclado) | 3× `Tab` real a partir do topo de `/funcionarios` | `Mui-focusVisible` **é** aplicado (botão `contained` "Novo Funcionário"), mas `outline: none 0px` e `box-shadow` = **apenas a sombra de elevação padrão**. **Sem indicador de foco próprio.** Pré-existente (nenhuma quick task toca `focusVisible`); **não piorou**, e agora se sabe que vale também para o botão `contained`, não só para os itens do `Drawer` como a rodada 2 registrou. Contraste do botão em foco: 5,01 (`indigo`). |
| `selected` — item do menu lateral | leitura dos 14 `ListItemButton` do `Drawer` na rota ativa | 🔴 **nenhum item recebe `.Mui-selected`.** Todos os 14 têm `background-color: rgba(0,0,0,0)` e `color: #8a88a3` (`chrome.fg`), **inclusive o da rota corrente**. `components/Layout/index.tsx:124,132,142,152` nunca passam a prop `selected`. Ver nota abaixo. |
| `disabled` | ⚪ **não remedido nesta rodada** | A rodada 2 mediu `action.disabled` padrão do MUI a 1,83 — pré-existente, não usa `primary`, e nenhuma das três quick tasks o toca. |

### 🟡 Correção ao registro da rodada 2 — `selected` no `Drawer`

A rodada 2 registrou: *"`selected` — item da sidebar … sidebar usa tokens
`chrome.*` … texto `#8a88a3`/`#0c0c12` = 5,69 ✅"*. **Os 5,69 são do item
normal, não do selecionado** — item selecionado **não existe** na UI.
Consequências, ambas **pré-existentes e fora do escopo das quick tasks**, mas que
valem registro porque a rodada 2 afirmou o contrário:

1. A rota ativa **não é indicada** no menu lateral.
2. O override `MuiListItemButton['&.Mui-selected']` de `tokens.ts:132` — e com ele
   os tokens `chrome.selecionado` e `chrome.fgAtivo` dos cinco temas — **nunca é
   renderizado**. É código morto na UI (segue exercitado por teste unitário).

---

## R3.5 — Corte, sobreposição e ilegibilidade

Varredura de `getBoundingClientRect` sobre todo elemento visível com texto direto,
no `indigo`, em 6 telas:

| Tela | elementos com texto | tamanho zero | fora do viewport | `scrollWidth > innerWidth` |
| --- | --- | --- | --- | --- |
| `/dashboard` | 110 | 0 | 0 | não |
| `/funcionarios` | 436 | 3 | 0 | não |
| `/folha-pagamento` | 97 | 0 | 16 | não |
| `/rubricas` | 706 | 0 | 0 | não |
| `/usuarios` | 2084 | 2 | 0 | não |
| `/importacao` | 39 | 0 | 0 | não |

- Os **16 de `/folha-pagamento`** estão todos dentro de um `TableContainer` com
  `overflow-x: auto` — **rolagem horizontal legítima** de tabela larga
  (colunas "Custo Empresa", "Ações", `R$ …`, "Ver Funcionários"), não corte.
- Os **5 de tamanho zero** são o zero-width-space (`​`) do `renderValue` de
  `Select` sem valor. Pré-existente.
- **Nada novo.** Telas percorridas com captura no `indigo`: `/dashboard`,
  `/funcionarios`, `/folha-pagamento`, `/relatorios`, `/usuarios`, `/rubricas`,
  `/rubricas-fixas`, `/importacao`, `/api-keys`.
- Observação sem número (não medida): em `/relatorios`, sob tema escuro, a
  pré-visualização do PDF dentro de cada card é uma área branca grande. É o
  conteúdo do próprio PDF, **pré-existente** e alheio às quick tasks.

### `Alert` — dívida R-3 declarada, confirmada inalterada

`/api-keys` no `indigo`, os 2 `Alert` standard que renderizam sem interação:
`info` fundo `#040407`, texto **8,93** ✅; `warning` fundo `#070502`, texto
**9,27** ✅. Números idênticos aos da rodada 2 e do `TASK.md` §5 — a 014 declarou
que **não** corrigiria isso, e de fato não mexeu. Contraste passa; a perda de cor
semântica segue aberta como decisão de design.

---

## R3.6 — Gate

`cd frontend && npm run lint && npm run test && npm run build` em `02a7e12`:

| Etapa | Resultado |
| --- | --- |
| `npm run lint` | ✅ **0 erros**, 15 warnings pré-existentes (`react-hooks/exhaustive-deps` ×6, `react-refresh/only-export-components` ×9) |
| `npm run test` | ✅ **658 passed, 0 failed** em **44 arquivos** — bate com o declarado no `SUMMARY.md` |
| `npm run build` | ✅ `tsc -b` exit 0; `vite build` exit 0 — `dist/assets/index-DG9kbxdG.js` (1 521,85 kB) |

Suíte em blocos por caminho (o shell do ambiente tem timeout de 45 s):

| Bloco | Arquivos | Testes |
| --- | --- | --- |
| `src/theme` | 8 | 132 ✅ |
| `src/contexts` + `src/components` | 6 | 60 ✅ |
| `src/utils` `src/hooks` `src/lib` `src/routes` `src/services` `src/test` `src/smoke.test.tsx` | 16 | 91 ✅ |
| `src/pages/{ApiKeys,BeneficiosMensais,Cargos,CentrosCusto,Dashboard,DashboardCustomizavel}` | 4 | 98 ✅ |
| `src/pages/{FolhaPagamento,Funcionarios,Importacao,LinhasNegocio,Login}` | 4 | 130 ✅ |
| `src/pages/{Organograma,TiposBeneficio,Usuarios}` | 2 | 61 ✅ |
| `src/pages/{Relatorios,Rubricas,RubricasFixas}` | 4 | 86 ✅ |
| **Total** | **44** | **658 passed, 0 failed** |

Baseline 647 + 11 da varredura nova, como declarado.

---

## R3.7 — Veredito final da rodada 3

### ✅ **PASS**

Os três defeitos que a rodada 2 mediu estão **fechados no pixel**, nos cinco temas,
nos valores exatos que a quick 014 calculou — e nada do que a rodada 2 havia
aprovado regrediu.

| # | Item | Antes (R2) | Agora | Veredito |
| --- | --- | --- | --- | --- |
| 1 | `indigo`, `primary.main` sobre o **Card** (29 elementos, 6 telas) | 3,95 ❌ | **4,53** ✅ | fechado |
| 1 | `indigo`, idem sobre o **Dialog** | 2,68 ❌ | **4,53** ✅ | fechado |
| — | fundo computado do Card e do Dialog no `indigo` | `#272733` / `#41414b` | **`#1c1c28` = token** | fechado |
| 2 | `Funcionarios:557`, "Inativar" no hover, 4 temas claros | 1,14–1,21 ❌ | **4,92–5,83** ✅ | fechado |
| 3 | `AparenciaDialog`, `CheckIcon` no `indigo` | 1,68 ❌ | **3,58** ✅ (4,02–4,70 nos claros) | fechado |
| — | R-1, 30 pares ícone × avatar | 30/30 | **30/30** ✅ | sem regressão |
| — | `primary.contrastText`, 5 botões `contained` | 5/5 | **5/5** ✅ | sem regressão |
| — | `Funcionarios:534`, "Editar" no hover | 3,58–4,70 | **3,58–4,70** ✅ | sem regressão |
| — | escala tipográfica | 24/27/16 px, peso 600 | **idem** ✅ | sem regressão |
| — | AppBar, `chrome.fg` sobre o fundo real | 4,62 | **5,69** ✅ | melhorou |
| — | Menu/Select/Popover no escuro | — | borda 1px `divider`, **presente e legível** ✅ | efeito colateral tratado |
| — | Dialog contra o scrim | 1,97 | 1,18 — **distinguível na prática** ✅ | efeito colateral aceito |
| — | Drawer permanente e temporário | — | `chrome.bg`, inalterado ✅ | ok |

### Continua aberto

| # | Item | Estado |
| --- | --- | --- |
| 1 | **`MuiPickersPopper` sem a borda de 1px no modo escuro** — o `DatePicker` de `/relatorios` e o `components/DateField` são `Paper` mas não `Popover`/`Menu`, então recebem o `backgroundImage: 'none'` **sem** o separador que a 014 criou para as demais superfícies flutuantes | 🟠 gap identificado por código; **pixel não medido** (o popper não monta no alvo) |
| 2 | `Alert` standard perde a cor semântica sob os tints da 012 (R-3) | 🟡 dívida **declarada** pela 014, por decisão do usuário |
| 3 | Indicador de `focus` sem `outline` nem `box-shadow` próprio — vale para itens do `Drawer` **e** para o botão `contained` | 🟡 pré-existente, fora do escopo |
| 4 | **Nenhum item do `Drawer` recebe `.Mui-selected`**: a rota ativa não é indicada, e `chrome.selecionado`/`chrome.fgAtivo` são código morto na UI. Corrige a leitura da rodada 2 | 🟡 pré-existente, fora do escopo |
| 5 | Botão `disabled` a 1,83 (`action.disabled` padrão do MUI) | 🟡 pré-existente; não remedido nesta rodada |
| 6 | `Organograma:617` (`primary.light` em `isOver`) | ⚪ não coberto — exige drag-and-drop, que alteraria dados |
| 7 | `KpiWidget` / `TopRubricasWidget` / `AutocompleteField` | ⚪ não coberto — componentes sem tela |
| 8 | `Tooltip`, `Snackbar`, `Paper` elevação 3 do Login | ⚪ não coberto — nenhum renderizou sem ação que altere dados ou sem sair da sessão |
| 9 | Rótulo de papel dos avatares na `varredura-pos-fidelidade.md` §5 ("Total de Funcionários" e "Relação P/D" são `info`, não `primary`) | 🟡 correção de documentação, pendente desde a rodada 1 |

---

## Anexo R3 — método

| Item | Valor |
| --- | --- |
| Alvo | `http://localhost:3000` (nginx, `docker-compose`), sessão autenticada, dados reais |
| Bundle medido | `/assets/index-BhNIQgTr.js`, **1 521 120 bytes** — contém `backgroundImage:"none"` (×2), `MuiPopover` (×6), `MuiMenu:` (×1) e os hexes das quick 012/013; contagens idênticas ao `dist/` reconstruído de `02a7e12` |
| Instrumento | `getComputedStyle` via MCP `claude-in-chrome` (`javascript_tool`), sonda injetada em `sessionStorage` e reavaliada após cada navegação |
| Fundo efetivo | subida na árvore compondo, por nó, `background-image` **sobre** `background-color`; gradiente de cor única é composto como camada (é assim que o MUI pintava o overlay), gradiente multicolorido marca `NONFLAT` e o elemento é excluído em vez de gerar número — a mesma guarda da rodada 2, que evita o falso positivo dos badges do top-5 |
| Contraste | WCAG 2.1, luminância relativa; piso 4,5:1 para texto e 3:1 para ícone/gráfico |
| Estados | `hover` com o mouse real do MCP + 2–3 s de espera **e**, em paralelo, leitura da regra `:hover` gerada pelo Emotion em `document.styleSheets` para a classe do elemento renderizado. Os dois métodos foram cruzados em `indigo` e `corporate` e **coincidem no centésimo** (3,58 / 5,59 e 4,16 / 5,83), o que valida o segundo para os temas em que o mouse sintético não fixou o `:hover`. `focus` por `Tab` real |
| Calibração de coordenadas | as coordenadas do `computer` estão no espaço da **captura** (1103×1092), não em px CSS (1335×1322) — fator 0,8262. Erro cometido e corrigido no início desta rodada; além disso o layout dos cards muda por tema (fontes diferentes), então as posições foram relidas do DOM imediatamente antes de cada hover |
| Troca de tema | `localStorage['sistema-folha:tema']` + navegação; tema ativo **reconfirmado a cada medição** por token computado (`body` background + `fontFamily`), nunca assumido |
| Capturas | `outputs/screenshot-1785876266105-0d113bd5.jpg` (`AparenciaDialog` no `indigo`, card selecionado com `primary.light` + `CheckIcon`), `outputs/screenshot-1785876485386-77b606fc.jpg` (`Select` aberto no `indigo` sobre o Card, com a borda nova), mais zooms de região do hover dos ícones e do menu de conta. `Page.captureScreenshot` funcionou nesta rodada |
| Restauração | tema devolvido a **`indigo`**; sondas removidas do `sessionStorage` (`__probe014`, `__scan014`, `__full014`, `__hov014`, `__apar014`, `__selm014`, `__cssh014`, `__hv014`, `__chk014`) e de `window` — verificado por leitura após o reload |
| Dados do sistema | **nenhum** registro criado, editado ou excluído; **nenhum formulário submetido**. Diálogos abertos: `AparenciaDialog` (5×) e o `FormDialog` "Nova Rubrica" (1×), todos fechados com `Esc`/navegação sem salvar |
