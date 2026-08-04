# Fidelidade Visual dos Temas — Specification

## Problem Statement

A feature `temas-visuais` entregou os cinco temas com Verifier PASS em `ab2cf01`.
As cores de acento estão corretas, mas ao comparar com os mockups de
`_docs/estudo-visual/` a interface ficou parecida, não igual.

O estudo `_docs/estudo-visual/aproximacao-mockups.md` mediu a diferença no
navegador e isolou três causas. Esta feature endereça as duas primeiras, que se
resolvem sem redesenhar tela alguma. A terceira (estrutura do cartão de KPI)
fica registrada como decisão adiada.

## Goals

- [ ] Os quatro papéis semânticos (`success`, `warning`, `error`, `info`) derivam do tema em todos os cinco temas — hoje resolvem no default de fábrica do MUI
- [ ] Título de página, valor de KPI e título de card na escala dos mockups (24px / 27px / 16px equivalentes)
- [ ] Peso e cor de texto passam a ser decididos pelo tema, não por prop inline — zero `fontWeight=` em Typography no código de aplicação
- [ ] Nenhum arquivo de página alterado por motivo de estilo além da remoção de props

## Out of Scope

| Item | Razão |
| --- | --- |
| Camada C — componente `CartaoIndicador` sem avatar | Redesenho de estrutura. Decisão adiada até avaliar o resultado desta feature. |
| Transbordo do card de Custo Empresa | Deve ser aliviado pela redução do valor de 48px para 27px, mas a correção definitiva depende da Camada C. Se persistir, vira quick task própria. |
| Corte da coluna Ações na Folha de Pagamento | Defeito de layout pré-existente, registrado no inventário do estado atual. Não é de tema. |
| Densidade de tabela, largura da sidebar e tamanho do menu | Medição mostrou que já estão dentro do alvo (ver Assumptions). Mexer seria regressão. |
| Novos temas ou alteração de paleta de acento | As paletas dos cinco temas permanecem como estão (AD-016). |
| `pages/DashboardCustomizavel/` | PoC em `.gitignore`, não roteado. Fora do grep rastreado, conforme validation de `temas-visuais`. |

---

## Assumptions & Open Questions

| Assumption / decisão | Escolha | Rationale | Confirmado? |
| --- | --- | --- | --- |
| Cores semânticas por tema ou compartilhadas | Por tema, derivadas da paleta de cada um | O Indigo dark precisa de verde/âmbar claros; o Techne, de tons que conversem com o violeta | y |
| Títulos coloridos ou escuros | Escuros — remover `color="primary"` de títulos h1-h6 | Igual ao mockup; a cor de acento passa a valer para ações, não para texto | y |
| **Escala dos mockups é relativa, não absoluta** | Aplicar fator 1,415 aos valores do mockup | O mockup foi desenhado num quadro de 1070px; o sistema roda a 1515px. Comparar px absolutos inflava a diferença — o estudo original reportava fatores de 2× a 2,5× que na prática são 1,4× a 1,8× | n — correção do agente, ver Nota de escala |
| Densidade de tabela, sidebar e menu | Manter como estão | Após aplicar o fator, os valores atuais já caem dentro do alvo (13,4 vs 14px; 260 vs 240px; 15 vs 16px) | n — decorre da medição |
| `color="textSecondary"` (API depreciada) | Normalizar para `color="text.secondary"` | 18 ocorrências usando a forma antiga do MUI v4; a nova é equivalente e é a suportada | n — decisão técnica do agente |
| Props `color` semânticas (`success.main`, `error`, `warning.main`, `info.main`) | Manter | São tokens legítimos e passam a resolver corretamente após TEMAF-01 | n — decisão técnica do agente |
| Valor de `fontWeight` no tema | 600 para h3/h4/h6, conforme mockup | O mockup usa 600; o código usa `bold` (700) | n — decisão técnica do agente |

**Open questions:**

| # | Questão | Origem | Status |
| --- | --- | --- | --- |
| QA-1 | `classico` + `warning.main` (`#f57c00`) rende **2,70:1** contra `background.paper` — abaixo do AA de 4.5:1 exigido por AC3/AC4. O próprio default de fábrica do MUI (`#ed6c02`) renderia 3,11:1. AC4 e DD-4 ("`classico` não muda") são inconciliáveis como escritos. Batch 1 excluiu **esse único par (1 de 20)** da varredura, com `SPEC_DEVIATION` em `frontend/src/theme/contraste.test.ts`. Opções: (a) aceitar o `classico` fora do AA nesse papel; (b) autorizar alterar `warning.main` do `classico` (muda asserção existente e contraria DD-4). | Batch 1 / T2 | **Aberta — aguarda usuário** |
| QA-2 | `techne.info` foi escurecido de `#0C8DCE` (tabela do design) para `#0A7AB0`. O valor original rende 3,67:1 contra `#FFFFFF`, abaixo do AA de AC4; `#0A7AB0` é a variante mais próxima em matiz que atinge 4,75:1. `SPEC_DEVIATION` em `frontend/src/theme/themes.ts`. Coerente com `context.md` D1, mas diverge do valor literal da tabela do design. | Batch 1 / T2 | **Aberta — aguarda usuário** |
| QA-3 | Campos semânticos declarados **opcionais** em `TokensTema` (o design mostra obrigatórios), para não quebrar o build entre os commits de T1 e T2. Nenhum AC afetado; T2 comprova que os cinco temas declaram os quatro papéis. | Batch 1 / T1 | Resolvida — decisão técnica do agente |

---

## Nota de escala (correção do estudo de origem)

O estudo `aproximacao-mockups.md` comparou pixels do mockup com pixels do sistema
sem normalizar a escala. O mockup foi renderizado num quadro de **1070px** de
largura (A4 paisagem menos margens); a captura do sistema tem **1515px**. O fator
é **1,415**.

Os alvos desta spec já estão corrigidos:

| Elemento | Mockup | Equivalente real | Hoje | Ação |
| --- | --- | --- | --- | --- |
| Título de página | 17px | **24,1px** | 34px | reduzir |
| Valor de KPI | 19px | **26,9px** | 48px | reduzir |
| Título de card | 11px | **15,6px** | 20px | reduzir |
| Label de KPI | 9px | 12,7px | 14px | manter |
| Item de menu | 10,5px | 14,9px | 16px | manter |
| Corpo de tabela | 9,5px | 13,4px | 14px | manter |
| Largura da sidebar | 184px | 260,4px | 240px | manter |

Consequência prática: a Camada B é bem menor do que o estudo sugeria — três
variantes de tipografia, não a escala inteira.

---

## Implicit-Requirement Dimensions Sweep

Escopo Large (toca os cinco temas e 11 arquivos de página).

| Dimensão | Resolução |
| --- | --- |
| Validação de entrada e limites | N/A — não há entrada de usuário; a feature é de estilo |
| Falha / falha parcial | N/A — sem I/O, sem estado remoto |
| Idempotência / retry / duplicata | N/A — renderização é pura em relação ao tema |
| Fronteiras de autorização e rate limit | N/A — nenhuma tela muda de permissão |
| Concorrência / ordenação | N/A — sem estado compartilhado novo |
| Ciclo de vida / expiração do dado | N/A — nenhum dado novo persistido |
| Observabilidade | N/A — sem backend envolvido |
| Falha de dependência externa | N/A — nenhuma dependência nova |
| Integridade de transição de estado | TEMAF-08 — a troca de tema continua atômica; a escala é do tema, não do componente |

---

## User Stories

### P1: Cores semânticas derivadas do tema ⭐ MVP

**User Story**: Como usuário, quero que os indicadores de sucesso, alerta e erro
usem as cores do tema que escolhi, para que a interface não misture paletas.

**Why P1**: É um defeito objetivo e visível — avatares azul-claro, verde e laranja
de fábrica aparecem inclusive no Indigo dark, onde destoam do fundo. Corrige-se
só no tema, sem tocar componente.

**Acceptance Criteria**:

1. WHEN um tema qualquer está ativo THEN `theme.palette.success.main` SHALL ser um valor declarado nos tokens desse tema, e não o default `#4caf50` do MUI
2. WHEN um tema qualquer está ativo THEN o mesmo SHALL valer para `warning.main`, `error.main` e `info.main`
3. WHEN o tema `indigo` está ativo THEN as quatro cores semânticas SHALL ter contraste mínimo 4.5:1 contra `background.paper`
4. WHEN qualquer tema está ativo THEN as quatro cores semânticas SHALL ter contraste mínimo 4.5:1 contra `background.paper`
5. WHEN um avatar de KPI do Dashboard renderiza THEN a cor de fundo e a cor do ícone SHALL derivar dos tokens do tema ativo

**Independent Test**: alternar entre os cinco temas no Dashboard e confirmar que os quatro avatares mudam de cor junto; rodar o teste de contraste parametrizado.

---

### P1: Escala tipográfica dos mockups no tema ⭐ MVP

**User Story**: Como usuário, quero que as telas mostrem mais informação por vez,
para não precisar rolar tanto em telas que são de consulta.

**Why P1**: É a diferença mais visível em relação aos mockups e resolve-se em
`montarTema`, sem editar tela alguma.

**Acceptance Criteria**:

1. WHEN qualquer tema está ativo THEN `theme.typography.h4.fontSize` SHALL corresponder a 24px (`1.5rem`)
2. WHEN qualquer tema está ativo THEN `theme.typography.h3.fontSize` SHALL corresponder a 27px (`1.6875rem`)
3. WHEN qualquer tema está ativo THEN `theme.typography.h6.fontSize` SHALL corresponder a 16px (`1rem`)
4. WHEN qualquer tema está ativo THEN `theme.typography.{h3,h4,h6}.fontWeight` SHALL ser 600
5. WHEN o Dashboard renderiza THEN o título da página SHALL medir 24px e o maior valor de KPI SHALL medir 27px, verificável por `getComputedStyle`
6. WHEN qualquer tema está ativo THEN `theme.typography.body1`, `body2`, `subtitle1` e `subtitle2` SHALL permanecer nos valores atuais (fora do escopo desta mudança)
7. WHEN o tema muda THEN a escala SHALL permanecer idêntica entre os cinco temas — a escala não é por tema

**Independent Test**: medir os três elementos no navegador nos cinco temas e conferir contra a tabela da Nota de escala.

---

### P1: Peso e cor de texto decididos pelo tema

**User Story**: Como desenvolvedor, quero que peso e cor de texto venham do tema,
para que ajustar a identidade visual não exija varrer as telas de novo.

**Why P1**: Sem isso a história anterior fica pela metade — o `fontSize` do tema
é aplicado, mas `fontWeight="bold"` na prop continua vencendo, e os títulos
seguem coloridos.

**Acceptance Criteria**:

1. WHEN o build roda THEN não SHALL existir nenhuma prop `fontWeight=` em `Typography` nos arquivos rastreados de `src/pages/` e `src/components/` — hoje são 24
2. WHEN o build roda THEN não SHALL existir prop `color="primary"` em `Typography` de variante `h1` a `h6` — hoje são 10
3. WHEN o build roda THEN não SHALL existir a forma depreciada `color="textSecondary"` — hoje são 18; substituída por `color="text.secondary"`
4. WHEN um título de página renderiza THEN a cor SHALL ser `theme.palette.text.primary`
5. WHEN uma prop `color` semântica (`success.main`, `warning.main`, `error`, `info.main`) existe THEN ela SHALL ser preservada — são tokens legítimos
6. WHEN `npm run lint` roda com uma prop `fontWeight=` reintroduzida em `Typography` THEN o ESLint SHALL falhar com exit code diferente de 0
7. WHEN as suítes existentes rodam THEN todas SHALL passar sem alteração de asserção — a remoção de props não muda texto nem estrutura

**Independent Test**: `git grep -c 'fontWeight=' -- 'frontend/src/**/*.tsx'` retorna 0; abrir o Dashboard e confirmar título escuro e semibold.

---

## Edge Cases

- WHEN o tema `classico` está ativo THEN a escala nova SHALL ser aplicada também a ele — o `classico` preserva as **cores** de antes, não a escala (ver Nota abaixo)
- WHEN um `Typography` usa `variant="h4"` para exibir um valor monetário longo THEN a redução para 24px SHALL diminuir o transbordo, mas não há garantia de eliminá-lo
- WHEN o Organograma renderiza sob o tema `indigo` THEN as cores semânticas novas SHALL manter os nós distinguíveis — a tela tem `color: '#fff'` remanescente na linha 1218
- WHEN um teste existente asserta cor ou peso de texto THEN ele SHALL ser revisto, não deletado — a remoção de props pode invalidar asserções de estilo

**Nota sobre o `classico`**: a spec anterior construiu o `classico` fora da fábrica
(DD-3) justamente para não regredir visualmente. A escala nova **é** aplicada a
ele; as cores permanecem. Se a decisão for preservar também a escala do
`classico`, isso vira uma assunção a registrar antes do Execute.

---

## Requirement Traceability

| ID | Story | Camada | Fase | Status |
| --- | --- | --- | --- | --- |
| TEMAF-01 | P1: Semânticas — tokens no tipo e na fábrica | A | 1 | Done |
| TEMAF-02 | P1: Semânticas — declaradas nos cinco temas | A | 1 | Done |
| TEMAF-03 | P1: Semânticas — contraste 4.5:1 contra `background.paper` | A | 1 | Done ⚠️ (1/20 pares excluído — ver Questões abertas) |
| TEMAF-04 | P1: Escala — `h3`, `h4`, `h6` em `montarTema` | B | 2 | Done |
| TEMAF-05 | P1: Escala — `fontWeight` 600 nas três variantes | B | 2 | Done |
| TEMAF-06 | P1: Escala — demais variantes preservadas | B | 2 | Done |
| TEMAF-07 | P1: Escala — verificação medida no navegador | B | 2 | Pending |
| TEMAF-08 | P1: Escala — idêntica entre os cinco temas | B | 2 | Done |
| TEMAF-09 | P1: Props — remover 24 `fontWeight=` | B' | 3 | Pending |
| TEMAF-10 | P1: Props — remover 10 `color="primary"` em títulos | B' | 3 | Pending |
| TEMAF-11 | P1: Props — normalizar 18 `color="textSecondary"` | B' | 3 | Pending |
| TEMAF-12 | P1: Props — preservar props semânticas | B' | 3 | Pending |
| TEMAF-13 | P1: Props — lint barra reintrodução de `fontWeight=` | B' | 3 | Pending |
| TEMAF-14 | P1: Props — suítes existentes sem alteração de asserção | B' | 3 | Pending |

**Coverage:** 14 requisitos, 14 mapeados para tasks, 0 sem mapeamento.

---

## Success Criteria

- [ ] `git grep -c 'fontWeight=' -- 'frontend/src/**/*.tsx'` retorna 0
- [ ] `git grep -cE '<Typography[^>]*color="textSecondary"' -- 'frontend/src/**/*.tsx'` retorna 0
- [ ] Nos cinco temas, `theme.palette.{success,warning,error,info}.main` difere do default do MUI
- [ ] Título de página mede 24px e maior valor de KPI mede 27px, medidos por `getComputedStyle`
- [ ] `npm run lint`, `npm run test` e `npm run build` em verde ao final de cada fase
- [ ] Contagem de testes ≥ 559 (baseline registrado na validation de `temas-visuais`)
- [ ] Capturas das 20 telas comparadas com `capturas-implementado/` — nenhuma regressão de legibilidade
