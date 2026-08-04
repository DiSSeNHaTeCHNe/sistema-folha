# Quick Task 012 — Summary

**Date:** 2026-08-04
**Branch:** `feat/temas-fidelidade-visual` (base `263045b`)
**Status:** Parcial — R-1/R-2 resolvidos e QA-1 encerrada; D-5 não aplicado (guardrail)

## O que foi feito

1. **`light` explícito nas semânticas dos temas claros.** `corporate`, `soft` e
   `techne` declaravam só `main`; o MUI derivava `light = lighten(main, 0.2)`, um
   meio-tom. O avatar de KPI pinta `color: X.main` sobre `bgcolor: X.light`
   (`Dashboard/index.tsx:206,234,262,290,500,550`) e o par ficava em 1,40–2,51:1.
   Regra usada para o tint: `main` misturado a **88% de branco** — mesma construção
   para os três temas, e todo par resultante fica acima de 4:1, folgado sobre o
   piso de 3:1 do WCAG 1.4.11.
2. **`warning.main` do `classico` corrigido** — único par do tema abaixo do piso.
3. **Varredura ampliada** com o par `X.main × X.light` (piso 3:1) nos cinco temas.
4. **QA-1 encerrada**: `EXCECOES_SEMANTICAS` e o `SPEC_DEVIATION` de
   `classico/warning` removidos de `contraste.test.ts`. A varredura semântica AA
   agora cobre 20 pares de 20.

## Cores alteradas — antes → depois, com razão calculada

Razões calculadas com `frontend/src/theme/contraste.ts` (WCAG 2.1), não estimadas.

### Tints de avatar (`X.light`) — par medido: `X.main / X.light`, piso 3:1

| Tema | Papel | `main` | `light` antes (derivado) | razão antes | `light` depois | razão depois |
| --- | --- | --- | --- | --- | --- | --- |
| corporate | success | `#0F6E56` | `#3F8B78` | 1,53 | `#E2EEEB` | **5,22** |
| corporate | warning | `#854F0B` | `#9D723C` | 1,57 | `#F0EAE2` | **5,63** |
| corporate | error | `#A32D2D` | `#B55757` | 1,50 | `#F4E6E6` | **5,83** |
| corporate | info | `#185FA5` | `#467FB7` | 1,55 | `#E3ECF4` | **5,46** |
| soft | success | `#0F6E56` | `#3F8B78` | 1,53 | `#E2EEEB` | **5,22** |
| soft | warning | `#854F0B` | `#9D723C` | 1,57 | `#F0EAE2` | **5,63** |
| soft | error | `#993C1D` | `#AD634A` | 1,54 | `#F3E8E4` | **5,79** |
| soft | info | `#5F5E5A` | `#7F7E7B` | 1,60 | `#ECECEB` | **5,49** |
| techne | success | `#0F6E56` | `#3F8B78` | 1,53 | `#E2EEEB` | **5,22** |
| techne | warning | `#8A5200` | `#A17533` | 1,55 | `#F1EAE0` | **5,35** |
| techne | error | `#A32D2D` | `#B55757` | 1,50 | `#F4E6E6` | **5,83** |
| techne | info | `#0A7AB0` | `#3B95C0` | 1,41 | `#E2EFF6` | **4,05** |

`indigo` já tinha `light` explícito por DD-3 (5,13–6,16) e não foi tocado.

### `classico` — `warning.main`

| Cor | Antes | Depois |
| --- | --- | --- |
| valor | `#f57c00` | `#b05900` |
| × `background.paper` (`#ffffff`) | 2,70 | **4,91** |
| × `background.default` (`#f8f9fa`) | 2,57 | **4,66** |
| × `warning.light` (`#fff3e0`) | 2,47 | **4,48** |

`#fff3e0` foi preservado como tint. Demais pares do `classico` já passavam
(`success` 4,56 · `error` 4,92 · `info` 4,03 contra o respectivo `light`).

## Pares novos na varredura

- 1 par por papel semântico × 5 temas: `X.main × X.light`, piso **3:1**
  (WCAG 1.4.11 — componente gráfico). Total: **20 novas medições**, em 5 testes.
- O par `primary.main × background.paper` (piso 4,5:1) **não** foi adicionado —
  ver bloqueio abaixo.

## DD-4 e QA-1

- **DD-4 revisado** e **QA-1 encerrada** por decisão do usuário em 2026-08-04.
  Registrado em `spec.md` (linha da QA-1), `design.md` (isenção do `classico` +
  nota abaixo da tabela de decisões) e neste diretório. `validation.md` não foi
  tocado — é append-only do Verifier.

## Asserções revistas em `themes.test.ts`

| Asserção | O que mudou | Por quê |
| --- | --- | --- |
| `preserves classico palette…` | saiu `warning.main === '#f57c00'`; comentário registra que essa é a única cor do `classico` alterada e por quê | o valor deixou de ser verdade; as outras asserções do `classico` continuam fixadas |
| novo: `corrige warning.main do classico por acessibilidade` | fixa `#b05900`, afirma `not.toBe('#f57c00')` e mede AA contra `paper` **e** `default` | a nova verdade documentada, mais forte que a asserção que substituiu |
| novo: `tema %s declara light explícito nas quatro semânticas` (corporate/soft/techne) | fixa os 12 tints | os tints passam a ser valor de design, não derivação |

Nenhuma asserção foi enfraquecida, nenhum teste removido ou pulado.

## Bloqueio — D-5 não aplicado

O guardrail do quick mode é de 3 arquivos de código, e os 3 já foram usados.
Escurecer `primary.main` quebraria `frontend/src/contexts/ThemeContext.test.tsx:69,103`
(que fixam `primary:#3B82F6` e `primary:#1976d2`), e incluir `primary` no par
`main × light` exigiria `primary.light` explícito — que hoje não cabe no tipo
`TokensTema.primary` (`frontend/src/theme/tokens.ts:19`). Cores já calculadas e
prontas na tabela de `TASK.md`.

## Gate

`cd frontend && npm run lint && npm run test && npm run build` — lint 0 erros
(15 warnings pré-existentes), **641 testes / 0 falhas** em 44 arquivos (632 antes
+ 9 novos), build exit 0.
