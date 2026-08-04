# Fidelidade Visual dos Temas — Code Review

**Diff revisado**: `0a0eac7..d1627ca` (branch `feat/temas-fidelidade-visual`)
**Data**: 2026-08-04
**Executado por**: sub-agente `/code-review` (independente do autor e do Verifier)
**Veredito**: ✅ **Aprovado com ressalvas** — 0 blockers

O Verifier verifica cobertura contra a spec. Este review olha o que a spec não
pergunta: regressão, acoplamento, acessibilidade e manutenibilidade. Nenhum item
abaixo bloqueia o merge; todos precisam de decisão de escopo antes de virar task.

---

## Ressalvas

### R-1 — Contraste ícone × fundo do avatar de KPI abaixo de 3:1 nos temas novos

`frontend/src/theme/themes.ts:30,59,131` (`corporate`, `soft`, `techne`)

As semânticas desses três temas declaram apenas `main`. O MUI então deriva
`light = lighten(main, 0.2)`, que é um meio-tom, não um tint. O avatar de KPI
pinta `color: X.main` sobre `bgcolor: X.light`
(`frontend/src/pages/Dashboard/index.tsx:206,234,262,290,500,550`), e esse par
fica em **1,40–1,60:1** — abaixo dos 3:1 do WCAG 1.4.11 para componente gráfico.

`indigo` (com `light` explícito por DD-3) e `classico` (tints `#e8f5e8`…) ficam em
5,1–6,1:1. Ou seja: exatamente os três temas que esta feature introduziu são os
piores no elemento que o AC5 de P1-Semânticas nomeia.

**Não é violação de AC** — AC3/AC4 exigem 4.5:1 de `X.main` contra
`background.paper`, e isso passa. É o par renderizado que ninguém mediu (ver R-2).

**Fix provável**: declarar `light` explícito nas semânticas de `corporate`, `soft`
e `techne`, como já é feito no `indigo`.

### R-2 — A varredura de contraste mede um par que não é o renderizado

`frontend/src/theme/contraste.test.ts:76-90`

A varredura AA mede `X.main` × `background.paper`. O par efetivamente renderizado
no caso do AC5 é `X.main` × `X.light`. A garantia de contraste da feature não
cobre o pixel que a feature mexeu.

Relacionado: `frontend/src/theme/themes.test.ts:196` mede o `light` do `indigo`
contra `'#FFFFFF'` — superfície que não existe num tema escuro. Funciona como
proxy de "é escuro o bastante", mas a asserção afirma outra coisa.

### R-3 — No `indigo`, `light` é mais escuro que `main` e vaza para fora do avatar

`frontend/src/theme/themes.ts:89-94`

A inversão do contrato do MUI resolve o avatar, mas `.light` não é privado dele.
`Alert` standard usa `color: lighten(light, 0.6)` / `bg: darken(light, 0.9)`; o
outlined usa `border: 1px solid palette[color].light`. No `indigo` isso vira texto
acinzentado e borda quase preta em todo Alert/Notification. Nenhum teste cobre
`Alert` por tema.

**Alternativa**: token próprio (ex.: em `chrome`) para o fundo de avatar, em vez
de sobrecarregar `light`.

### R-4 — Perda de ênfase sem substituto no tema

As remoções de `fontWeight` em variantes **fora** da escala (`h3`/`h4`/`h6`) não
foram compensadas — `design.md` mantém `body*` e `subtitle*` no default 400.
Pontos afetados:

| Arquivo:linha | Elemento |
| --- | --- |
| `components/AparenciaDialog/index.tsx:66` | nome do tema no seletor |
| `components/OrganogramaGrafico/index.tsx:123,155` | rótulos do gráfico |
| `pages/Organograma/index.tsx:632` | nome do nó |
| `pages/FolhaPagamento/index.tsx:380,434` | linha "Total: R$ …" |
| `pages/FolhaPagamento/index.tsx:775` | custo empresa (era `medium`) |
| `pages/Dashboard/index.tsx:523,529,573,579` | top-5 |

Totais financeiros perdendo destaque é o caso mais sensível — e a varredura visual
que pegaria isso (T10) está bloqueada.

### R-5 — `Relatorios/index.tsx` reescrito inteiro por normalização de EOL

O arquivo mudou 470 linhas para uma alteração de 1 linha; o resto é `\r\r\n` →
`\r\n`. Conteúdo verificado como idêntico fora da prop — sem regressão funcional —
mas destrói o `git blame` e, sem `.gitattributes` no repo, tende a voltar no
próximo save em editor Windows.

### R-6 — Query estrutural no teste de avatares

`frontend/src/pages/Dashboard/Dashboard.test.tsx:129-136`

`avatarDoCardKpi` navega por `parentElement.parentElement.lastElementChild`. Um
`Box` a mais no cabeçalho do card quebra 5 testes. Contraria a regra de query do
`frontend/AGENTS.md`.

### R-7 — Guarda de props só enxerga arquivos rastreados

`frontend/src/theme/noStyleProps.test.ts:19-25`

A varredura usa `git ls-files`, logo um `.tsx` novo ainda não adicionado ao índice
passa livre justamente enquanto está sendo escrito. Além disso a suíte passa a
depender do binário `git` e de um checkout `.git` presente.

---

## Nits

- `frontend/eslint.config.js:45` — o seletor `JSXAttribute[name.name='fontWeight']`
  vale para qualquer JSX (`Box`, `Chip`…), não só `Typography`, embora a mensagem
  fale de `Typography`. E não fecha o caminho de fuga real (`sx={{ fontWeight }}`),
  que segue vivo em **4** pontos, não 2: `Dashboard/index.tsx:515,565` além de
  `Importacao/index.tsx:816` e `Funcionarios/index.tsx:499`. O inventário do G5 da
  `validation.md` lista só os dois últimos.
- Lint e guarda cobrem apenas `src/pages/**` e `src/components/**`. Um futuro
  `src/features/` (estrutura TARGET do `frontend/AGENTS.md`) nasceria fora das duas
  redes.
- `frontend/src/theme/noStyleProps.test.ts:36` — `/<Typography\b[^>]*>/` encerra a
  tag no primeiro `>`; um arrow inline (`onClick={() => …}`) dentro da tag causaria
  falso negativo na checagem de `color="primary"`.
- `Dashboard.test.tsx` — o JSDoc de `canaisDaCor` ficou órfão acima de `paraPx`;
  e `paraPx` está duplicado entre `Dashboard.test.tsx` e `escalaRenderizada.test.tsx`.
- `Dashboard/index.tsx:223` — "Custo Empresa" continua em `h4` (24px) enquanto os
  outros três KPIs usam `h3` (27px). Com a escala reduzida, a inconsistência na
  mesma linha fica mais visível.

---

## Pontos positivos registrados

- A escala centralizada em `ESCALA_TIPOGRAFICA`, com merge em `montarTema` **e** no
  `classico`, garante o AC7 (escala idêntica entre temas) por construção, não por
  disciplina.
- `escalaRenderizada.test.tsx` com o caso de controle "prop vence variante" é a
  prova certa para justificar a Fase 3 — e falha se alguém reintroduzir o padrão.
- Os marcadores `SPEC_DEVIATION` estão no ponto exato do código, não só na spec.

---

## Follow-up — R-1 e R-2 fechados (2026-08-04)

Acrescentado após o review; nada acima foi reescrito.

- **R-1 — resolvido pela quick task 012** (`d55132d`, `649041a`). `light` explícito
  nas quatro semânticas de `corporate`, `soft` e `techne` (tint = `main` misturado a
  88% de branco), e `warning.main` do `classico` corrigido de `#f57c00` para
  `#b05900`. DD-4 foi revisado e a isenção QA-1 encerrada por decisão do usuário:
  nenhum tema fica isento por acessibilidade.
- **R-2 — resolvido em duas etapas.** A 012 acrescentou o par `X.main × X.light`
  (piso 3:1, WCAG 1.4.11) à varredura de `contraste.test.ts` — o par de fato
  renderizado no avatar de KPI. A **quick task 013** (`641fb2e`, `5a92326`) fechou a
  outra metade, o **D-5** da `varredura-visual.md`: `primary.main` como cor de
  **texto** sobre `background.paper` (piso 4,5:1). `primary.main` foi escurecido na
  origem em `classico`, `corporate` e `soft`, e clareado no `indigo` (tema escuro);
  `primary.contrastText` de `corporate` e `soft` passou a `#FFFFFF`; `primary.light`
  passou a ser declarado nos cinco temas. Números em
  `_docs/specs/quick/013-contraste-primary-texto/TASK.md`.

Como as duas varreduras percorrem `TEMAS`, nenhum tema futuro escapa dos pares —
que era a preocupação de fundo da R-2.
