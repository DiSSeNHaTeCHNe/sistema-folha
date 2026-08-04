# Quick Task 012: Contraste dos temas — avatar de KPI e texto de acento

**Date:** 2026-08-04
**Status:** Parcial — R-1 concluído; D-5 bloqueado pelo guardrail de 3 arquivos de código

## Description

Corrigir os dois pares de contraste que a varredura de `contraste.test.ts` não
media: ícone × fundo do avatar de KPI (code-review R-1) e `primary.main` como cor
de texto sobre `background.paper` (D-5). Origem: `code-review.md` R-1/R-2 e
`_docs/estudo-visual/varredura-pos-fidelidade.md` seção 7 (medições no navegador).

## Files Changed

- `frontend/src/theme/themes.ts` — `light` explícito nas quatro semânticas de
  `corporate`, `soft` e `techne`; `warning.main` do `classico` corrigido
- `frontend/src/theme/contraste.test.ts` — par `X.main × X.light` (piso 3:1)
  acrescentado à varredura; `SPEC_DEVIATION`/`EXCECOES_SEMANTICAS` de
  `classico × warning` removido
- `frontend/src/theme/themes.test.ts` — asserção do `classico` revista (o
  `warning.main` deixou de ser `#f57c00`) e tints dos temas claros fixados

## Decisões do usuário aplicadas (2026-08-04)

- **DD-4 revisado**: o `classico` é ajustado por acessibilidade. Enquadramento
  aceito: ou muda em todos os papéis que reprovam, ou em nenhum.
- **QA-1 encerrada**: a isenção do par `classico × warning.main` deixa de existir;
  nenhum par fica fora da varredura.
- **D-5 resolve escurecendo `primary.main`** na origem, sem token novo.

## Verification

- [x] `X.main / X.light` ≥ 3:1 (WCAG 1.4.11) nos 5 temas × 4 papéis — varredura nova
- [x] `X.main / background.paper` ≥ 4.5:1 nos 5 temas, agora **sem exceções**
- [x] `npm run lint` — 0 erros
- [x] `npm run test` — 641 testes, 0 falhas (baseline 632 + 9 novos)
- [x] `npm run build` — exit 0
- [ ] `primary.main` ≥ 4.5:1 contra `background.paper` (D-5) — **não executado**

## Bloqueio (D-5)

Aplicar D-5 exige um **quarto arquivo de código**, o que o quick mode não comporta:

| Sub-item | Arquivo extra necessário | Por quê |
| --- | --- | --- |
| Escurecer `primary.main` de `classico` e `corporate` | `frontend/src/contexts/ThemeContext.test.tsx:69,103` | as asserções fixam `primary:#3B82F6` e `primary:#1976d2`; ficariam vermelhas |
| Par `primary.main × primary.light` na varredura | `frontend/src/theme/tokens.ts:19` | `TokensTema.primary` não aceita `light`; sem `light` explícito o MUI deriva `lighten(main, 0.2)`, que rende no máximo ~1,4:1 contra o próprio `main` — nenhum valor de `primary` alcança 3:1 por derivação |

Por isso a varredura nova cobre os quatro papéis semânticos (o par do avatar), e o
par `primary.main × background.paper` não foi adicionado: sem escurecer o
`primary` ele falharia em `soft` (3,39), `corporate` (3,68) e `indigo` (4,48).

Valores já calculados para D-5, prontos para aplicar quando o quarto arquivo for
autorizado (matiz e saturação preservadas, só a luminosidade cai/sobe até o piso):

| Tema | `primary.main` antes → depois | × `background.paper` | × `background.default` | `contrastText` |
| --- | --- | --- | --- | --- |
| `classico` | `#1976d2` → `#1873cd` | 4,60 → 4,80 | 4,37 → 4,56 | derivado (`#fff`), 4,80 |
| `corporate` | `#3B82F6` → `#1167f4` | 3,68 → 4,92 | 3,39 → 4,54 | precisa virar `#FFFFFF` (4,92); `#0F172A` cairia para 3,63 |
| `soft` | `#1D9E75` → `#188361` | 3,39 → 4,71 | 3,25 → 4,51 | precisa virar `#FFFFFF` (4,71); `#0F172A` cairia para 3,79 |
| `indigo` | `#7F77DD` → clarear (tema escuro) | 4,48 → ≥4,5 | 4,96 | `#12121A` segue ≥4,5 |

## Commit

- `d55132d` — fix(tema): tints explicitos no avatar de KPI e warning acessivel no classico
- `649041a` — test(tema): varre o par icone x fundo do avatar e encerra a isencao QA-1
