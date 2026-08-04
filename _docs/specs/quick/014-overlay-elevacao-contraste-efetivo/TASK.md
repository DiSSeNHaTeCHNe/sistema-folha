# Quick Task 014: overlay de elevação e varredura do fundo efetivo

**Date:** 2026-08-04
**Status:** Done

## Description

Fechar os defeitos que a verificação no navegador das quick tasks 012 e 013 mediu
(`_docs/specs/quick/013-contraste-primary-texto/VERIFICACAO.md` §R2.7) e, principalmente,
fechar a porta: desligar o overlay de elevação do MUI no modo escuro para que o **token
volte a ser a verdade renderizada**, trocar a última cor literal sobre fundo `.light`, e
fazer a varredura de contraste medir o **fundo efetivo** dos níveis de elevação em uso —
não apenas `background.paper` cru.

## Contexto: o mesmo defeito, três vezes

R-1, R-2 e o D-5 no `indigo` são a mesma classe: **o teste mediu um par plausível e o
pixel renderizado era outro**. Esta task ataca a causa, não só os sintomas.

## Desvio autorizado do guardrail

O quick mode limita a **3 arquivos** (`quick-mode.md`, Guardrails). Esta task toca
**5 arquivos de código** + docs. O usuário autorizou explicitamente a extensão do teto
(2026-08-04), como já havia feito na quick 013.

| Arquivo | Por que é inevitável |
| --- | --- |
| `frontend/src/theme/tokens.ts` | origem do overlay: `montarTema` é onde os `components` do tema são montados |
| `frontend/src/pages/Funcionarios/index.tsx` | a regressão da 012 (`color: 'white'` sobre `error.light`) mora aqui |
| `frontend/src/components/AparenciaDialog/index.tsx` | o defeito 3 da verificação não é resolvido pelo item 1 sozinho (2,76:1 < 3:1) |
| `frontend/src/theme/contraste.test.ts` | a varredura do fundo efetivo — a rede que faltava |
| `frontend/src/theme/noColorLiterals.test.ts` | a fresta por onde `color: 'white'` passou duas vezes |

## Decisões do usuário aplicadas (vinculantes)

- **Desligar o overlay de elevação**, em vez de clarear `primary.main` do `indigo`. Motivo
  registrado pelo usuário: faz o token voltar a ser a verdade, de modo que o que o teste
  mede passa a ser o que a tela mostra.
- **Varrer o fundo efetivo**, não só o token, nos níveis de elevação realmente usados.
- **Não corrigir** a descoloração dos `Alert` (item 5) — é decisão de design não tomada.
- **Teto de arquivos estendido** (acima).

## Files Changed

- `frontend/src/theme/tokens.ts` — `MuiPaper.styleOverrides.root.backgroundImage: 'none'`
  em `montarTema`; borda de 1px em `MuiPopover`/`MuiMenu` só no modo escuro, para as
  superfícies flutuantes não ficarem indistinguíveis do Card sob elas
- `frontend/src/pages/Funcionarios/index.tsx` — hover do ícone "Inativar": `color: 'white'`
  → `'error.main'`
- `frontend/src/components/AparenciaDialog/index.tsx` — fundo do tema selecionado:
  `action.selected` (branco translúcido) → `primary.light` (opaco)
- `frontend/src/theme/contraste.test.ts` — varredura do fundo **renderizado** de um `Paper`
  em cada nível de elevação em uso (+10 medições de identidade com o token, +10 de AA)
- `frontend/src/theme/noColorLiterals.test.ts` — passa a barrar também cor **nomeada**
  (`'white'`, `'black'`) em `pages/` e `components/`

## 1. Overlay de elevação desligado

Em `mode: 'dark'` o MUI pinta `background-image: linear-gradient(rgba(255,255,255,α),
rgba(255,255,255,α))` sobre `background.paper`, com α crescente por elevação. No `indigo`
o Card renderizava `#272733`, não o token `#1C1C28`.

α por elevação, lidos do `--Paper-overlay` que o MUI v7 injeta inline (não estimados):

| Elevação | Componente | α | fundo antes | `#8078DD` antes | fundo depois | `#8078DD` depois |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Card / Paper padrão | 0,051 | `#282833` (`#272733` no navegador) | **3,92** (3,95 medido) ❌ | `#1C1C28` | **4,53** ✅ |
| 3 | Paper do Login | 0,082 | `#2E2E38` | — | `#1C1C28` | **4,53** ✅ |
| 4 | AppBar | 0,092 | `#222228` sobre `chrome.bg` | — | `#0C0C12` | — |
| 8 | Menu / Select | 0,119 | `#373742` | — | `#1C1C28` | **4,53** ✅ |
| 16 | Drawer (temporário) | 0,147 | `#3A3A44` | — | `#1C1C28` | — |
| 24 | Dialog | 0,165 | `#41414B` | **2,71** (2,68 medido) ❌ | `#1C1C28` | **4,53** ✅ |

Nos quatro temas claros é **no-op**, confirmado por medição: o `--Paper-overlay` vem vazio
e o fundo renderizado é `#FFFFFF` em todos os seis níveis.

`classico` não passa por `montarTema` (tem `createTheme` próprio) e **não recebeu o
override** — não precisa: é `mode: 'light'`, e a varredura nova prova, tema a tema, que o
fundo efetivo é o token nos seis níveis. Se algum dia virar escuro, o teste falha.

### Efeitos colaterais nas demais superfícies `Paper`

| Superfície | Sobre o quê aparece | Separação antes | Separação depois | Ação |
| --- | --- | --- | --- | --- |
| **Menu / Select / Popover** | Card (`#1C1C28`) | `#373742` × `#282833` = **1,24** | `#1C1C28` × `#1C1C28` = **1,00** — indistinguível | **borda 1px `divider`** (`#2A2A38`, 1,19 contra o paper) só no modo escuro |
| **Dialog** | scrim do Backdrop (`#09090D`) | 1,97 | **1,18** + sombra | nenhuma: o scrim escurece 50% de tudo atrás, a delimitação continua perceptível |
| **Drawer permanente** | — | `elevation 0`, sem overlay | idem | nenhuma (o MUI já usava elevação 0 fora do `variant="temporary"`) |
| **AppBar** | — | `chrome.bg` clareado a `#222228`; `chrome.fg` rendia **4,62** | `chrome.bg` = `#0C0C12`; `chrome.fg` rende **5,69** | nenhuma — **melhora**: passa a bater com o par `chrome.fg / chrome.bg` que o teste já assertava |
| **Accordion** | — | não usado em nenhuma tela | — | nenhuma |

## 2. Cor literal sobre fundo `.light` — `Funcionarios/index.tsx:556`

Mesma correção que a 013 aplicou na linha 534 (botão Editar) e não olhou a linha de baixo.

| Tema | `error.light` | ícone antes | razão antes | ícone depois | razão depois |
| --- | --- | --- | --- | --- | --- |
| `classico` | `#ffebee` | `white` | **1,14** ❌ | `error.main` `#c62828` | **4,92** ✅ |
| `corporate` | `#F4E6E6` | `white` | **1,21** ❌ | `#A32D2D` | **5,83** ✅ |
| `soft` | `#F3E8E4` | `white` | **1,20** ❌ | `#993C1D` | **5,79** ✅ |
| `techne` | `#F4E6E6` | `white` | **1,21** ❌ | `#A32D2D` | **5,83** ✅ |
| `indigo` | `#4A2C2C` | `white` | 12,44 ✅ | `#F09595` | **5,59** ✅ |

Piso: 3:1 (WCAG 1.4.11, ícone). O par `error.main × error.light` já é varrido pela 012.

### Varredura do repositório atrás de outras ocorrências

`rg` em `frontend/src/**` por `'white'`, `"white"`, `#fff`, `#ffffff`, `'black'`,
`common.white`, `common.black`, e por todo consumidor de `bgcolor|backgroundColor|bg:` com
`.light`/`.main`. Resultado: **`Funcionarios:556` era a última ocorrência do defeito.**

| Ocorrência | Veredito |
| --- | --- |
| `pages/Funcionarios/index.tsx:557` | ❌ **corrigida** — era a terceira do padrão |
| `pages/Dashboard/index.tsx:514,564` | ✅ legítima — `common.white` (token, não literal) sobre `linear-gradient` de cor de gráfico; são os badges `#1`…`#5`, não um par `.light`/`.main` |
| `pages/Organograma/index.tsx:1218` | ✅ legítima — `common.white` no `Backdrop`, cuja base é preta translúcida |
| `components/OrganogramaGrafico/index.tsx:116` | ✅ legítima — `theme.palette.getContrastText(primary.main)`, derivado do próprio token |
| `pages/Organograma/index.tsx:617` (`primary.light` em `isOver`) | ✅ texto é `text.primary`, não literal — segue não coberto por pixel (exigiria drag-and-drop) |
| `pages/Relatorios/RelatorioCatalogCard.tsx:75` | ✅ `primary.contrastText` sobre `primary.main` — par já varrido |
| `widgets/{KpiWidget,TopRubricasWidget}.tsx` | ✅ só tokens `X.main` sobre `X.light` — par já varrido (rota `dashboard-v2` inexistente) |

Para não depender de varredura manual outra vez, `noColorLiterals.test.ts` passou a barrar
cor **nomeada** além de hex/`rgba()`/`hsl()`. Era exatamente a fresta: `'white'` não é
hexadecimal, e por isso sobreviveu a duas quick tasks. `'common.white'` continua válido —
o ponto antes do nome impede o casamento.

## 3. `AparenciaDialog/index.tsx` — ícone do tema ativo

Desligar o overlay **não bastou**. O fundo do card selecionado era `action.selected`, um
branco translúcido a 16% no modo escuro, que clareia qualquer superfície sob ele:

| Etapa | fundo efetivo (`indigo`) | `#8078DD` sobre ele |
| --- | --- | --- |
| antes (overlay 0,165 + `action.selected`) | `#5F5F68` (`#606068` no navegador) | **1,70** (1,68 medido) ❌ |
| só com o overlay desligado | `#40404A` | **2,76** ❌ (piso 3:1) |
| com `primary.light` no lugar de `action.selected` | `#2E2B50` | **3,58** ✅ |

`primary.light` é opaco — não há nada a compor — e forma com `primary.main` o par já
varrido a 3:1 em `contraste.test.ts`. É a mesma remédio do item 2: a superfície passa a ser
um token, não uma camada translúcida. Nos cinco temas:

| Tema | fundo antes (`action.selected`) | razão antes | fundo depois (`primary.light`) | razão depois |
| --- | --- | --- | --- | --- |
| `classico` | `#EBEBEB` | 4,03 | `#e3eef9` | **4,09** ✅ |
| `corporate` | `#EBEBEB` | 4,13 | `#E2EDFE` | **4,16** ✅ |
| `soft` | `#EBEBEB` | 3,95 | `#E3F0EC` | **4,02** ✅ |
| `indigo` | `#5F5F68` | **1,70** ❌ | `#2E2B50` | **3,58** ✅ |
| `techne` | `#EBEBEB` | 4,72 | `#EFE7FF` | **4,70** ✅ |

A seleção continua sinalizada de três formas redundantes: borda `primary.main`, fundo
`primary.light` e o `CheckIcon`.

## 4. Varredura do fundo efetivo (a rede que faltava)

`contraste.test.ts` passou a **montar um `Paper`** por tema e por nível de elevação, ler o
estilo computado (`background-color` + `background-image`, resolvendo o `--Paper-overlay`
que o MUI injeta inline) e compor as camadas. Não presume: mede.

Níveis descobertos no código de aplicação — não uma matriz especulativa de 24:

| Elevação | Origem no código |
| --- | --- |
| 1 | padrão de `Card` (17 arquivos) e `Paper` (6 arquivos) |
| 3 | `pages/Login/index.tsx:64`, único `elevation` explícito do repositório |
| 4 | padrão do `AppBar` (`components/Layout`) |
| 8 | padrão do `Menu` (2 arquivos) e do `Select` (7 arquivos) |
| 16 | padrão do `Drawer` (`components/Layout`) |
| 24 | padrão do `Dialog` (17 arquivos) |

Duas asserções novas, ambas percorrendo `TEMAS` — nenhum tema futuro escapa:

| Asserção | Piso | Medições |
| --- | --- | --- |
| fundo efetivo do `Paper` **é** `background.paper` em toda elevação em uso | igualdade exata | 5 temas × 6 níveis = **30** |
| `primary.main` sobre o fundo efetivo do Card (1) e do Dialog (24) | 4,5:1 (AA) | 5 temas × 2 níveis = **10** |

Contraprova executada: com o `backgroundImage: 'none'` removido, as duas asserções falham
no `indigo` (`expected '#33333d' to be '#1c1c28'` e `expected 3.3588… to be >= 4.5`). A
rede pega o defeito que a suíte verde não via.

## 5. Dívida registrada (NÃO corrigida) — `Alert` descolorido (R-3)

O `Alert` standard do MUI deriva de `.light`: `bg = lighten(light, 0.9)`,
`color = darken(light, 0.6)`. Com `light` virando tint a 88% de branco (quick 012), o fundo
fica quase branco e o texto cinza; no `indigo`, com `light` mais escuro que `main` (DD-3), o
fundo vira `#040407`. Números medidos no navegador em `/api-keys`:

| Tema | `info` texto / ícone | fundo | `warning` texto / ícone | fundo |
| --- | --- | --- | --- | --- |
| `classico` | 6,26 / 4,52 | `#fcfdfe` | 6,05 / 4,84 | `#fffdfb` |
| `corporate` | 6,42 / 6,40 | `#fcfdfd` | 6,39 / 6,57 | `#fdfcfc` |
| `soft` | 6,37 / 6,38 | `#fdfdfd` | 6,39 / 6,57 | `#fdfcfc` |
| `indigo` | 8,93 / 9,47 | `#040407` | 9,27 / 9,36 | `#070502` |
| `techne` | 6,35 / 4,66 | `#fcfdfe` | 6,39 / 6,23 | `#fdfcfb` |

**Contraste passa em 20/20** (6,05–9,47). É defeito **visual**: o alerta perde a cor
semântica — em `corporate`/`soft`/`techne` o fundo é indistinguível do card e a severidade
só se lê pelo ícone. É a ressalva **R-3** do code review se materializando.

**Não corrigido por decisão do usuário**: a saída (token próprio para o fundo de tint, em
vez de sobrecarregar `.light`) é decisão de design que ele não tomou. `Alert` outlined não
é usado em nenhuma tela.

## Verification

- [x] `indigo`: `primary.main` sobre o Card renderizado — 3,95 → **4,53** ✅ (piso AA)
- [x] `indigo`: `primary.main` sobre o Dialog renderizado — 2,68 → **4,53** ✅
- [x] fundo efetivo ≡ `background.paper` nos 5 temas × 6 níveis de elevação (30 medições)
- [x] hover do ícone "Inativar" ≥ 3:1 nos 5 temas — 1,14/1,21/1,20/1,21 → 4,92/5,83/5,79/5,83
- [x] `AparenciaDialog` `CheckIcon` ≥ 3:1 nos 5 temas — 1,70 no `indigo` → **3,58**
- [x] superfícies flutuantes não ficam indistinguíveis do fundo (tabela do item 1)
- [x] nenhuma cor literal (hex, `rgba()`, `hsl()` **ou nomeada**) em `pages/`/`components/`
- [x] `npm run lint` — 0 erros (15 warnings pré-existentes)
- [x] `npm run test` — **658 testes, 0 falhas** em 44 arquivos (baseline 647 + 11)
- [x] `npm run build` — `tsc -b` exit 0, `vite build` exit 0
- [ ] conferência no navegador — pendente (é do Verifier)

## Commits

- `afea6f6` — fix(tema): desliga o overlay de elevacao do modo escuro e separa superficies flutuantes
- `95df7fe` — fix(funcionarios): usa error.main no hover do icone Inativar
- `2640f2b` — fix(aparencia): usa primary.light no fundo do tema selecionado
- `2378986` — test(tema): varre o fundo efetivo das elevacoes em uso e barra cor nomeada
- `docs` — docs(quick): registra a quick task 014
