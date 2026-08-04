# Quick Task 013 — Summary

**Date:** 2026-08-04
**Branch:** `feat/temas-fidelidade-visual` (base `0a0eac7`, HEAD anterior `274625a`)
**Status:** ✅ Done

## O que foi feito

Fechado o **D-5** e endereçada a ressalva **R-2** do code review de
`temas-fidelidade-visual`: `primary.main` é cor de **texto** sobre
`background.paper` em ~22 elementos por tema, e esse par nunca foi medido — a
varredura olhava `primary.contrastText × primary.main`, que é o texto *dentro* do
botão preenchido.

1. `TokensTema.primary` passou a aceitar `light` (era `{ main, contrastText }`).
   Sem isso o MUI deriva `lighten(main, 0.2)`, um meio-tom que rende ~1,4:1 contra
   o próprio `main` — nenhum `primary` alcançaria os 3:1 do WCAG 1.4.11 por
   derivação.
2. `primary.main` escurecido (clareado, no `indigo`) nos temas que reprovavam;
   `contrastText` ajustado onde a inversão de luminosidade exigiu.
3. `primary.light` explícito nos cinco temas, tint a 88% de branco (no `indigo`,
   `main × 0,36`, mais escuro que o `main`, por DD-3).
4. Dois pares novos na varredura de `contraste.test.ts`, que percorre `TEMAS`.

## Cores (razões calculadas com `theme/contraste.ts`, não estimadas)

| Tema | `primary.main` antes → depois | × paper | `contrastText` | `primary.light` (novo) | `main × light` |
| --- | --- | --- | --- | --- | --- |
| `classico` | `#1976d2` → `#1873cd` | 4,60 → **4,80** | derivado `#fff` (4,80) | `#e3eef9` | 4,09 |
| `corporate` | `#3B82F6` → `#1167F4` | 3,68 → **4,92** | `#0F172A` → **`#FFFFFF`** (4,92) | `#E2EDFE` | 4,16 |
| `soft` | `#1D9E75` → `#188361` | 3,39 → **4,71** | `#0F172A` → **`#FFFFFF`** (4,71) | `#E3F0EC` | 4,03 |
| `indigo` | `#7F77DD` → `#8078DD` | 4,48 → **4,53** | `#12121A` (5,01) | `#2E2B50` | 3,58 |
| `techne` | `#7836FC` (inalterado) | **5,63** | `#FFFFFF` (5,63) | `#EFE7FF` | 4,70 |

`techne` não muda e segue casado com `relatorios.branding.primary-color`.
`indigo` é escuro: o ajuste é clarear, e `#8078DD` é o menor clareamento que fecha
o piso (`L + 0,2%` em HSL, matiz e saturação intactas).

## Varredura

| Par | Piso | Medições |
| --- | --- | --- |
| `primary.main / background.paper` | 4,5:1 (AA) | 5 (1 por tema) |
| `primary.main / primary.light` | 3:1 (WCAG 1.4.11) | 5 (`primary` incorporado ao bloco de avatar da 012 via `PAPEIS_COM_TINT`, sem duplicação) |

## Desvios

- **Guardrail de 3 arquivos estendido**, com autorização explícita do usuário —
  6 arquivos de código. Justificativa arquivo a arquivo no `TASK.md`.
- **Regressão colateral corrigida**: `Funcionarios/index.tsx:534` pintava o ícone
  Editar de branco sobre `primary.light` no hover. Com `primary.light` virando tint
  claro, o ícone sumiria; passou a `color: 'primary.main'`, que é justamente o par
  agora varrido a 3:1.

## Gate

- `npm run lint` — 0 erros (15 warnings pré-existentes)
- `npm run test` — **647 passed, 0 failed** (44 arquivos; baseline 641 + 6)
- `npm run build` — exit 0

## Commits

- `641fb2e` — `fix(tema): escurece primary.main para AA como cor de texto e declara primary.light`
- `5a92326` — `test(tema): varre primary.main como texto e o par primary.main x primary.light`

## Pendente

Conferência no navegador dos cinco temas (junto com a da quick 012). Nada
bloqueado.
