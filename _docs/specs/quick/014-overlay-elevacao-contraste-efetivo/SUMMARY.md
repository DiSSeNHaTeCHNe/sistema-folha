# Quick Task 014 — Summary

**Date:** 2026-08-04
**Branch:** `feat/temas-fidelidade-visual` (base `0a0eac7`, HEAD anterior `59de312`)
**Status:** ✅ Done

## O que foi feito

Fechados os defeitos que a rodada 2 da verificação no navegador mediu
(`_docs/specs/quick/013-contraste-primary-texto/VERIFICACAO.md` §R2.7) — três corrigidos,
um registrado como dívida por decisão do usuário — e, principalmente, **fechada a porta**
para a classe de defeito que já havia se repetido três vezes: o teste media um par
plausível e o pixel renderizado era outro.

1. **Overlay de elevação desligado** (`MuiPaper.styleOverrides.root.backgroundImage: 'none'`
   em `montarTema`). O token volta a ser a verdade renderizada.
2. **Última cor literal sobre fundo `.light` eliminada** (`Funcionarios/index.tsx:556`),
   depois de varrer o repositório inteiro atrás de outras.
3. **`AparenciaDialog`**: o fundo do tema selecionado deixou de ser um branco translúcido.
4. **Varredura do fundo efetivo** em `contraste.test.ts`, com contraprova.
5. **`noColorLiterals.test.ts`** passa a barrar cor nomeada — a fresta pela qual
   `color: 'white'` sobreviveu a duas quick tasks.

Razões calculadas com `frontend/src/theme/contraste.ts` (WCAG 2.1); os α do overlay foram
lidos do `--Paper-overlay` que o MUI v7 injeta inline. Nada estimado.

## Antes → depois

| Defeito | Onde | Antes | Depois |
| --- | --- | --- | --- |
| **1** — overlay de elevação | `indigo`, `primary.main` sobre o Card | `#272733`, **3,95** ❌ | `#1C1C28`, **4,53** ✅ |
| **1** — idem, Dialog | `indigo`, 30 elementos em 6 telas | `#41414B`, **2,68** ❌ | `#1C1C28`, **4,53** ✅ |
| **2** — ícone "Inativar" no hover | `Funcionarios/index.tsx:556` | **1,14 / 1,21 / 1,20 / 1,21** ❌ | **4,92 / 5,83 / 5,79 / 5,83** ✅ (`indigo` 12,44 → 5,59) |
| **3** — `CheckIcon` do tema ativo | `AparenciaDialog/index.tsx` | **1,68** ❌ (2,76 só com o overlay off) | **3,58** ✅ no `indigo`, 4,02–4,70 nos claros |
| **4** — `Alert` descolorido (R-3) | 5 temas, `/api-keys` | contraste 6,05–9,47 ✅, cor semântica perdida | **não corrigido** — dívida registrada |

## Fundo efetivo × token, por nível de elevação

Todos os níveis em uso no código de aplicação, medidos no `Paper` montado:

| Elevação | Componente | α do overlay | `indigo` antes | `indigo` depois |
| --- | --- | --- | --- | --- |
| 1 | Card / Paper | 0,051 | `#282833` | `#1C1C28` = token ✅ |
| 3 | Paper do Login | 0,082 | `#2E2E38` | `#1C1C28` ✅ |
| 4 | AppBar | 0,092 | `#222228` (sobre `chrome.bg`) | `#0C0C12` = `chrome.bg` ✅ |
| 8 | Menu / Select | 0,119 | `#373742` | `#1C1C28` ✅ |
| 16 | Drawer temporário | 0,147 | `#3A3A44` | `#1C1C28` ✅ |
| 24 | Dialog | 0,165 | `#41414B` | `#1C1C28` ✅ |

Nos quatro temas claros o overlay não existe: fundo efetivo `#FFFFFF` nos seis níveis,
antes e depois. `classico`, que não passa por `montarTema`, também é provado pela varredura.

## Efeitos colaterais tratados

- **Menu / Select / Popover**: com o overlay desligado passariam a pintar a mesma cor do
  Card sob eles (1,00:1), e a sombra de elevação é invisível no tema escuro. Recebeu
  **borda de 1px em `divider`**, apenas no modo escuro (`#2A2A38`, 1,19 contra o paper).
- **Dialog**: separação caiu de 1,97 para 1,18 contra o scrim do `Backdrop` — mantida sem
  ação, porque o scrim escurece 50% de tudo atrás e a delimitação continua perceptível.
- **Drawer permanente**: já era `elevation 0`, sem overlay. Inalterado.
- **AppBar**: **melhorou** — `chrome.fg` sobre o fundo real passou de 4,62 para 5,69, que é
  exatamente o valor que o par `chrome.fg / chrome.bg` já assertava no teste.
- **Accordion**: não usado em nenhuma tela.

## Varredura nova

| Asserção | Piso | Medições |
| --- | --- | --- |
| fundo efetivo do `Paper` **é** `background.paper` em toda elevação em uso | igualdade | 5 temas × 6 níveis = **30** |
| `primary.main` sobre o fundo efetivo do Card (1) e do Dialog (24) | 4,5:1 (AA) | 5 temas × 2 níveis = **10** |
| `pages/`/`components/` sem cor **nomeada** (`'white'`, `'black'`) | — | 1 |

Contraprova executada: removido o `backgroundImage: 'none'`, as duas primeiras falham no
`indigo` (`expected '#33333d' to be '#1c1c28'`; `expected 3.3588… to be >= 4.5`). A rede
pega o defeito que a suíte verde não via. Como a varredura percorre `TEMAS`, nenhum tema
futuro escapa.

## Desvios

- **Guardrail de 3 arquivos estendido** para 5 arquivos de código, com autorização
  explícita do usuário (2026-08-04), como já ocorrera na quick 013. Justificativa arquivo a
  arquivo no `TASK.md`.
- **Nenhum `SPEC_DEVIATION`.** A única decisão de design em aberto (a descoloração dos
  `Alert`, ressalva R-3) **não foi tomada**: ficou registrada como dívida, com os números.
- Nenhuma asserção enfraquecida, nenhum teste removido, pulado ou desabilitado.

## Dívida registrada (não corrigida)

`Alert` standard perde a cor semântica sob os tints da quick 012: `lighten(tint, 0.9)` dá
fundo quase branco (`#fcfdfd`) e `darken(tint, 0.6)` dá texto cinza; no `indigo` o fundo
vira `#040407`. **Contraste passa em 20/20 pares (6,05–9,47)** — é defeito visual, não de
acessibilidade. É a ressalva **R-3** do code review se materializando. Corrigir exige
decidir entre sobrecarregar `.light` ou criar um token próprio de tint — decisão de design
que o usuário não tomou. Números completos no `TASK.md` §5.

## Gate

- `npm run lint` — 0 erros (15 warnings pré-existentes)
- `npm run test` — **658 passed, 0 failed** em 44 arquivos (baseline 647 + 11)
- `npm run build` — `tsc -b` exit 0, `vite build` exit 0

Suíte executada em blocos por caminho (o shell do ambiente tem timeout de 45 s):

| Bloco | Arquivos | Testes |
| --- | --- | --- |
| `src/theme` + `src/contexts` | 10 | 152 ✅ (141 antes + 11) |
| `src/components` | 4 | 40 ✅ |
| `src/utils` `src/hooks` `src/lib` `src/routes` `src/services` `src/test` `src/smoke.test.tsx` | 16 | 91 ✅ |
| `src/pages/{ApiKeys,BeneficiosMensais,Cargos,CentrosCusto,Dashboard,DashboardCustomizavel}` | 4 | 98 ✅ |
| `src/pages/{FolhaPagamento,Funcionarios,Importacao,LinhasNegocio,Login}` | 4 | 130 ✅ |
| `src/pages/{Organograma,TiposBeneficio,Usuarios}` | 2 | 61 ✅ |
| `src/pages/{Relatorios,Rubricas,RubricasFixas}` | 4 | 86 ✅ |
| **Total** | **44** | **658 passed, 0 failed** |

## Commits

- `afea6f6` — `fix(tema): desliga o overlay de elevacao do modo escuro e separa superficies flutuantes`
- `95df7fe` — `fix(funcionarios): usa error.main no hover do icone Inativar`
- `2640f2b` — `fix(aparencia): usa primary.light no fundo do tema selecionado`
- `2378986` — `test(tema): varre o fundo efetivo das elevacoes em uso e barra cor nomeada`

## Pendente

Nova conferência no navegador (é do Verifier, não desta task): confirmar no pixel os
4,53:1 do `indigo` sobre Card e Dialog, os hovers de `Funcionarios`, o `AparenciaDialog` e
a separação do `Menu`/`Select` com a borda nova, nos cinco temas.
