# Fidelidade Visual dos Temas — Débito Técnico

**Data**: 2026-08-04
**Origem**: feature `temas-fidelidade-visual` (T1–T10) + quick tasks 012, 013 e 014
**Estado da branch no fechamento**: Verifier PASS (rodada 3), code review aprovado com
ressalvas, verificação no navegador PASS, 658 testes, lint 0 erros, build ok.

Nada abaixo quebra funcionalidade. Não há impacto em cálculo, dado, API ou regra de
negócio. Todos os itens são de percepção visual ou de acessibilidade.

Decisão do usuário em 2026-08-04: **registrar como débito e mergear**. Nenhum item
abaixo bloqueia o merge.

---

## Prioridade alta — impacto funcional real

### DT-1 — Foco sem indicador visível (pré-existente)

`Mui-focusVisible` é aplicado, mas nenhum componente pinta `outline` ou `box-shadow`
próprio. Quem navega por teclado não vê onde está. Vale para os itens do Drawer **e**
para o botão `contained` — não é um caso isolado.

- **Critério violado**: WCAG 2.4.7 (Focus Visible), nível AA
- **Origem**: pré-existente; nenhuma task desta rodada causou ou piorou
- **Impacto**: quebra navegação por teclado e uso com leitor de tela
- **Fix provável**: `:focus-visible` com outline em `chrome.fgAtivo` ou `primary.main`,
  nos `components` do tema — vale para todos os temas de uma vez
- **Evidência**: `_docs/specs/quick/014-overlay-elevacao-contraste-efetivo/VERIFICACAO.md`

### DT-2 — Rota ativa não é indicada no Drawer (pré-existente)

Nenhum item do Drawer recebe `.Mui-selected`. O usuário se localiza apenas pelo título
da página. Os tokens `chrome.selecionado` e `chrome.fgAtivo` **existem no tema e não são
consumidos por ninguém** — código morto esperando um consumidor.

- **Origem**: pré-existente
- **Impacto**: custo de orientação; agravado em telas profundas
- **Fix provável**: aplicar `selected` no `ListItemButton` da rota ativa, consumindo os
  tokens que já existem
- **Correção de registro**: a rodada 2 da verificação afirmou que a seleção usava
  `chrome.*` e estava ok. Estava errado — a rodada 3 provou que não há `.Mui-selected`.

---

## Prioridade média — regressões desta rodada

### DT-3 — `MuiPickersPopper` sem borda no modo escuro

Introduzido pela **quick task 014**. Ao desligar o overlay de elevação
(`MuiPaper.styleOverrides.root.backgroundImage: 'none'`), Menu, Select e Popover
passariam a pintar exatamente a cor do Card sob eles, com a sombra invisível no escuro.
A 014 corrigiu adicionando borda de 1px em `divider` a `MuiPopover` e `MuiMenu`.

`MuiPickersPopper` (calendário do DatePicker em `/relatorios` e em `components/DateField`)
**é `Paper` mas não é `Popover` nem `Menu`** — recebeu o `backgroundImage: 'none'` sem
receber a borda. `AutocompleteField` tem o mesmo defeito, mas está sem tela.

- **Origem**: quick task 014 (`afea6f6`)
- **Impacto**: no `indigo`, difícil ver onde o popup do calendário termina. As datas
  seguem legíveis e os cliques funcionam.
- **Escopo**: um tema, um componente
- **Fix provável**: estender a mesma regra de borda a `MuiPickersPopper` — uma linha
- **Status da evidência**: gap identificado por código; **pixel não medido** — o popper
  não montou no alvo em 4 tentativas

### DT-4 — Alerts perdem cor semântica sob os tints novos

Introduzido pela **quick task 012**, ao trocar os `light` derivados (meio-tons) por
tints claros. O `Alert` standard do MUI computa `bg: lighten(light, 0.9)` e
`color: darken(light, 0.6)`. Com tint claro na entrada, o fundo vira quase branco
(`#fcfdfd`) e o texto vira cinza; no `indigo` o fundo vira `#040407`.

- **Origem**: quick task 012 — é a ressalva **R-3** do code review se materializando
- **Contraste**: passa em todos os 20 pares medidos (6,05–9,47). **Não é defeito de
  acessibilidade**, é defeito visual.
- **Impacto**: perde-se o pareamento cor↔significado que permite distinguir erro de
  sucesso no canto do olho. O texto continua dizendo o que é.
- **Fix provável**: token próprio para fundo de avatar (a alternativa que o R-3 já
  sugeria), liberando `.light` do papel duplo. **É decisão de design** — não cabe em
  quick mode.
- **Nota**: `Alert` outlined não é usado em tela nenhuma; o Alert de `/relatorios` não
  renderizou no estado de dados atual e ficou sem medição.

---

## Prioridade baixa — dívida herdada e inconsistências

### DT-5 — `sx={{ fontWeight }}` fora do alcance da guarda

A regra ESLint mira `JSXAttribute[name.name='fontWeight']` e o teste de guarda usa
`/\bfontWeight\s*=/`. Nenhum dos dois pega a forma `sx`. Ocorrências vivas:
`Importacao/index.tsx:816`, `Funcionarios/index.tsx:499`, `Dashboard/index.tsx:515,565`
(estes dois últimos em `Avatar`, fora até da Goal da spec).

Pré-existente e fora da letra do AC1, mas contraria a Goal "peso decidido pelo tema".
É o gap **G5** da `validation.md`.

### DT-6 — Transbordo estrutural do card de Custo Empresa

Medido na T10: sobreposição valor×ícone zerada nos cinco temas (era real antes), mas o
avatar fica 25–36px fora do card. São **281px de conteúdo num card de 228px** — déficit
que ajuste de fonte não fecha. A spec previu isso ("SHALL diminuir o transbordo, mas não
há garantia de eliminá-lo"). A correção definitiva é a **Camada C** (componente
`CartaoIndicador` sem avatar), registrada como Out of Scope desde o início.

Relacionado: `Dashboard/index.tsx:223` — "Custo Empresa" usa `h4` (24px) enquanto os
outros três KPIs usam `h3` (27px). Com a escala reduzida, a inconsistência na mesma
linha ficou mais visível.

### DT-7 — Perda de ênfase em `FolhaPagamento` (R-4 parcial)

`FolhaPagamento/index.tsx` — no card de funcionário, "Custo Empresa: R$ …" ficou
`16px/400`, idêntico às cinco linhas vizinhas do mesmo card. O total mais importante não
tem marca que o separe. Medido e confirmado na T10.

O restante do R-4 foi **refutado** por medição: o top-5 do Dashboard preserva três níveis
distintos, e as colunas Total Líquido / Custo Empresa da tabela distinguem por 16px vs
14px mais cor semântica.

Não coberto: `subtitle1` "Total: R$ …" (`:380,434`) — exigiria ficha processada na base.

### DT-8 — Higiene de EOL e `.gitattributes` (R-5)

`Relatorios/index.tsx` foi reescrito inteiro (470 linhas) para uma mudança de 1 linha; o
resto é normalização de `\r\r\n` → `\r\n`. Conteúdo verificado como idêntico fora da
prop — sem regressão funcional — mas destrói o `git blame`. Sem `.gitattributes` no repo,
tende a voltar no próximo save em editor Windows.

### DT-9 — Fragilidade dos instrumentos de teste (R-6, R-7)

- `Dashboard.test.tsx` — `avatarDoCardKpi` navega por
  `parentElement.parentElement.lastElementChild`. Um `Box` a mais no cabeçalho do card
  quebra 5 testes. Contraria a regra de query do `frontend/AGENTS.md`.
- `noStyleProps.test.ts` — a varredura usa `git ls-files`, logo um `.tsx` novo ainda não
  adicionado ao índice passa livre justamente enquanto está sendo escrito. E a suíte
  passa a depender do binário `git` e de um checkout `.git` presente.
- `noStyleProps.test.ts:36` — `/<Typography\b[^>]*>/` encerra a tag no primeiro `>`; um
  arrow inline (`onClick={() => …}`) dentro da tag causaria falso negativo.

### DT-10 — Lint e guarda cobrem só `src/pages/**` e `src/components/**`

Um futuro `src/features/` (estrutura TARGET do `frontend/AGENTS.md`) nasceria fora das
duas redes. O seletor `JSXAttribute[name.name='fontWeight']` também vale para qualquer
JSX (`Box`, `Chip`…), embora a mensagem de erro fale só de `Typography`.

### DT-11 — Achados de contraste fora do escopo das ACs

- **D-6** — `/organograma` no `soft`: 38 chips de contagem, branco sobre
  `secondary.main` `#D85A30`, razão **3,87**. Texto de 13px/400 exige 4,5. Reprova.
- **D-7** — `h5` fica em 24px/400, mesmo tamanho do título de página, criando ambiguidade
  de hierarquia. `/api-keys` não tem nenhum `h4`. O `design.md` deixou `h5` de fora por
  ter só 6 usos, nenhum deles título de página ou valor de KPI (DD-5).
- **`disabled` a 1,83** — **não é defeito**: é o `action.disabled` do MUI, e o WCAG isenta
  controle desabilitado justamente porque a baixa visibilidade comunica o estado.

---

## Cobertura não alcançada — registrar, não corrigir

Superfícies e estados que ficaram sem medição no navegador, com o motivo:

| Item | Motivo |
| --- | --- |
| `MuiPickersPopper` no pixel | popper não montou no alvo em 4 tentativas |
| `Tooltip`, `Snackbar` | não são `Paper` / não renderizaram no estado atual |
| `AutocompleteField` | componente sem tela |
| `Login` com `Paper elevation 3` | exigiria encerrar a sessão do usuário |
| `Organograma` estado `isOver` (`:617`) | exigiria drag, alteraria dados |
| `KpiWidget`, `TopRubricasWidget` | sem rota |
| Diálogo "Ver Rubricas" | sem ficha processada na base |
| Rótulos de nó do Organograma em modo gráfico | `fitView` reduz as caixas abaixo de 1px medível |
| Alert de `/relatorios` | não renderizou no estado de dados atual |

---

## O que **não** é débito — fechado e verificado no pixel

Registrado para que ninguém reabra por engano:

- **R-1** — contraste ícone × fundo do avatar de KPI: **30/30 pares ≥ 3:1**, contra 11/30
  antes. Fechado pela 012, confirmado em duas rodadas de verificação.
- **R-2 / D-5** — `primary.main` como cor de texto: 5/5 temas ≥ 4,5:1. Fechado pelas 013 e
  014. No `indigo`, Card 3,95 → **4,53** e Dialog 2,68 → **4,53**, com o fundo computado
  voltando a ser o token.
- **Regressão do ícone "Inativar"** (`Funcionarios/index.tsx:557`): 1,14–1,21 → 4,92–5,83.
- **`CheckIcon` do `AparenciaDialog`**: 1,68 → 3,58–4,70 nos cinco temas.
- **A porta ficou fechada.** A varredura passou a medir o **fundo efetivo** por nível de
  elevação (30 medições + 10 de AA), com contraprova executada: removendo o
  `backgroundImage: 'none'`, as asserções falham no `indigo`. E `noColorLiterals.test.ts`
  passou a barrar cor **nomeada** (`'white'`, `'black'`) — era a fresta exata por onde o
  mesmo defeito entrou três vezes.

---

## Referências

- `_docs/specs/features/temas-fidelidade-visual/validation.md` — Verifier, 3 rodadas, append-only
- `_docs/specs/features/temas-fidelidade-visual/code-review.md` — R-1 a R-7 e nits
- `_docs/specs/features/temas-fidelidade-visual/varredura-visual.md` — T10, 16 rotas × 5 temas
- `_docs/specs/quick/012-contraste-temas-avatar-texto/`
- `_docs/specs/quick/013-contraste-primary-texto/` — inclui `VERIFICACAO.md` rodadas 1 e 2
- `_docs/specs/quick/014-overlay-elevacao-contraste-efetivo/` — inclui `VERIFICACAO.md` rodada 3
