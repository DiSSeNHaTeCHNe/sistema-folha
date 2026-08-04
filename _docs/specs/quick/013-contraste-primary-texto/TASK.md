# Quick Task 013: `primary.main` como cor de texto — contraste AA (D-5)

**Date:** 2026-08-04
**Status:** Done

## Description

Fechar o D-5: `primary.main` é usado como **cor de texto** sobre `background.paper`
(valores monetários, `Button variant="text"`, chips "Normal"/"Filtrar"/"Ver
Funcionários") em `/folha-pagamento`, `/relatorios`, `/usuarios`, `/rubricas`,
`/rubricas-fixas` e `/importacao`, e reprovava AA em quatro dos cinco temas.
Continuação da quick task 012, que fechou o R-1 e parou no guardrail de arquivos.

Esse par nunca esteve em `contraste.test.ts`: a varredura media
`primary.contrastText × primary.main` (texto **dentro** do botão preenchido), par
diferente, que passa folgado. É a ressalva **R-2** do code review.

## Desvio autorizado do guardrail

O quick mode limita a **3 arquivos** (`quick-mode.md`, Guardrails). Esta task toca
**6 arquivos de código** + docs. O usuário autorizou explicitamente a extensão do
teto (2026-08-04), pelas razões que a 012 já havia mapeado:

| Arquivo | Por que é inevitável |
| --- | --- |
| `frontend/src/theme/tokens.ts` | `TokensTema.primary` não aceitava `light`; sem isso o MUI deriva `lighten(main, 0.2)` e o par `main × light` fica em ~1,4:1 |
| `frontend/src/theme/themes.ts` | origem das cores |
| `frontend/src/theme/contraste.test.ts` | os dois pares novos da varredura |
| `frontend/src/theme/themes.test.ts` | fixava os `primary` antigos |
| `frontend/src/contexts/ThemeContext.test.tsx` | fixava `primary:#3B82F6` e `primary:#1976d2` (linhas 69 e 103) |
| `frontend/src/pages/Funcionarios/index.tsx` | regressão colateral: o hover do ícone Editar pintava `color: 'white'` sobre `primary.light`, que deixou de ser meio-tom e virou tint claro — o ícone sumiria |

O escopo continua descritível em uma frase e sem decisão de design em aberto, então
o resto do quick mode se aplica normalmente.

## Decisões do usuário aplicadas (vinculantes)

- **Resolver escurecendo `primary.main` na origem**, não criando token de acento
  novo. Desvio perceptível da cor de marca em `soft` e `corporate` aceito em troca
  de cobertura total e de não manter duas cores de acento.
- **Os cinco temas são ajustados, inclusive o `classico`** — coerente com DD-4
  revisado e QA-1 encerrada na quick 012: nenhum tema fica isento.
- **Teto de arquivos estendido** (acima).

## Cores: antes → depois

Todas as razões foram **recalculadas** com `razaoContraste` de
`frontend/src/theme/contraste.ts` (WCAG 2.1), não estimadas. Matiz e saturação
preservadas; só a luminosidade se move, até o menor valor que fecha o piso.

### `primary.main` × `background.paper` (piso AA 4,5:1 — o par do D-5)

| Tema | antes | depois | razão antes → depois | × `background.default` |
| --- | --- | --- | --- | --- |
| `classico` | `#1976d2` | `#1873cd` | 4,60 → **4,80** | 4,37 → **4,56** |
| `corporate` | `#3B82F6` | `#1167F4` | 3,68 → **4,92** | 3,40 → **4,54** |
| `soft` | `#1D9E75` | `#188361` | 3,39 → **4,71** | 3,25 → **4,51** |
| `indigo` | `#7F77DD` | `#8078DD` | 4,48 → **4,53** | 4,96 → **5,01** |
| `techne` | `#7836FC` | `#7836FC` (inalterado) | 5,63 | 5,02 |

`indigo` é tema escuro: o ajuste é **clarear**, não escurecer. `#8078DD` é o menor
clareamento que fecha o piso — `L + 0,2%` em HSL, matiz `244,71°` e saturação `60%`
intactas. `techne` não muda e segue casado com
`relatorios.branding.primary-color` no `application.yml`.

### `primary.contrastText` × `primary.main` (piso AA 4,5:1 — segue passando)

| Tema | antes | depois | razão |
| --- | --- | --- | --- |
| `classico` | derivado (`#fff`) | derivado (`#fff`) | **4,80** |
| `corporate` | `#0F172A` | **`#FFFFFF`** | 4,92 (`#0F172A` cairia para 3,63) |
| `soft` | `#0F172A` | **`#FFFFFF`** | 4,71 (`#0F172A` cairia para 3,79) |
| `indigo` | `#12121A` | `#12121A` | **5,01** |
| `techne` | `#FFFFFF` | `#FFFFFF` | **5,63** |

### `primary.light` explícito (piso gráfico 3:1, WCAG 1.4.11)

Mesmo critério e mesmo estilo de tint da quick 012 nas semânticas (`main`
misturado a 88% de branco). No `indigo`, por DD-3, o `light` é **mais escuro** que
o `main` (`main × 0,36`), para o fundo não ficar quase branco no tema escuro.

| Tema | `primary.light` | `main × light` |
| --- | --- | --- |
| `classico` | `#e3eef9` | **4,09** |
| `corporate` | `#E2EDFE` | **4,16** |
| `soft` | `#E3F0EC` | **4,03** |
| `indigo` | `#2E2B50` | **3,58** |
| `techne` | `#EFE7FF` | **4,70** |

Sem valor explícito o MUI derivava `lighten(main, 0.2)` — meio-tom, no máximo
~1,4:1 contra o próprio `main`, ou seja, nenhum `primary` alcançaria 3:1 por
derivação.

As `amostras` dos cinco temas (swatches do `AparenciaDialog`) foram atualizadas
para os novos `primary.main`.

## Files Changed

- `frontend/src/theme/tokens.ts` — `TokensTema.primary` passa a ser
  `TokensPapelSemantico` (aceita `light`); `montarTema` já repassava o objeto
  inteiro à `palette`
- `frontend/src/theme/themes.ts` — `primary.main`, `primary.light`,
  `primary.contrastText` e `amostras` dos cinco temas
- `frontend/src/theme/contraste.test.ts` — pares `primary.main × background.paper`
  (4,5:1) e `primary.main × primary.light` (3:1); `primary` incorporado à varredura
  de avatar da 012 via `PAPEIS_COM_TINT`, sem duplicar o bloco
- `frontend/src/theme/themes.test.ts` — asserções de `primary` revistas e
  reforçadas
- `frontend/src/contexts/ThemeContext.test.tsx` — `primary:#1167F4` e
  `primary:#1873cd`
- `frontend/src/pages/Funcionarios/index.tsx` — hover do ícone Editar passa a usar
  `color: 'primary.main'` (era `'white'`)

## Asserções revistas (nenhuma enfraquecida, nenhuma deletada)

| Local | O quê | Por quê |
| --- | --- | --- |
| `themes.test.ts:73` `preserves classico palette` | `primary.main` sai deste bloco de "cores preservadas" | passou a ser cor ajustada por acessibilidade, como o `warning` na 012 |
| `themes.test.ts` (novo) `corrige primary.main do classico` | fixa `#1873cd`, `not.toBe('#1976d2')`, `light` e **AA contra paper e default** | mesmo padrão do teste de `warning` da 012 — mais forte que a asserção que substitui |
| `themes.test.ts` `registers corporate/soft/indigo…` | novo valor + `not.toBe(<antigo>)` + `contrastText` | impede regressão silenciosa de volta à cor antiga |
| `themes.test.ts` (novo) `declara primary.light explícito…` | 5 temas × (`main`, `light`, AA vs paper, AA do `contrastText`) | fixa a tabela acima e ancora as razões |
| `ThemeContext.test.tsx:69,103` | `#3B82F6` → `#1167F4`, `#1976d2` → `#1873cd` | a verdade mudou; o teste segue verificando o mesmo comportamento |

## Verification

- [x] `primary.main` ≥ 4,5:1 contra `background.paper` nos 5 temas (D-5 fechado)
- [x] `primary.main × primary.light` ≥ 3:1 nos 5 temas (WCAG 1.4.11)
- [x] `primary.contrastText × primary.main` segue ≥ 4,5:1 nos 5 temas
- [x] varredura percorre `TEMAS` — nenhum tema futuro escapa
- [x] `npm run lint` — 0 erros (15 warnings pré-existentes)
- [x] `npm run test` — **647 testes, 0 falhas** (baseline 641 + 6 novos)
- [x] `npm run build` — exit 0
- [ ] conferência no navegador — pendente, junto com a da quick 012

## Commit

- `641fb2e` — fix(tema): escurece primary.main para AA como cor de texto e declara primary.light
- `5a92326` — test(tema): varre primary.main como texto e o par primary.main x primary.light
