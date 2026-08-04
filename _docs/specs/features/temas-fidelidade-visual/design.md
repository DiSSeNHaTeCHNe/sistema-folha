# Fidelidade Visual dos Temas — Design

**Spec**: `_docs/specs/features/temas-fidelidade-visual/spec.md`
**Context**: `_docs/specs/features/temas-fidelidade-visual/context.md`
**Estudo de origem**: `_docs/estudo-visual/aproximacao-mockups.md`
**Escopo**: Large — cinco temas, 11 arquivos de página, nova regra de lint.

---

## Princípio

A feature anterior estabeleceu que o componente não conhece cor. Esta estabelece
que o componente também não conhece **peso nem escala**. Ele declara intenção
semântica (`variant="h4"`, `color="success.main"`) e o tema resolve tudo.

O que torna isso necessário é uma característica do MUI, confirmada no navegador:

```
.css-1qoxlvu { font-size: 2.125rem; font-weight: 700 }
```

O MUI funde `theme.typography.h4` com as system props do componente numa única
classe emotion. O `font-size` veio do tema; o `font-weight` veio da prop
`fontWeight="bold"`. **Prop vence tema, sempre.** Não há override de tema capaz
de ganhar dessa disputa sem `!important`.

Daí a ordem das camadas: B ajusta o tema, B' remove o que impede o tema de valer.
Fazer B sem B' entrega meia mudança — fontes menores, ainda em negrito e coloridas.

---

## Camada A — cores semânticas

### Extensão de `TokensTema`

```ts
export interface TokensTema {
  // ...campos atuais
  success: { main: string; light?: string; contrastText?: string };
  warning: { main: string; light?: string; contrastText?: string };
  error:   { main: string; light?: string; contrastText?: string };
  info:    { main: string; light?: string; contrastText?: string };
}
```

`light` é opcional e importa: o Dashboard usa `backgroundColor: 'info.light'`
com `color: 'info.main'` nos avatares. Se `light` não for declarado, o MUI o
deriva de `main` clareando — o que funciona nos temas claros e produz um fundo
quase branco no Indigo dark. Por isso o `indigo` declara `light` explicitamente.

### Valores por tema

Vêm do dicionário `THEMES` de `_docs/estudo-visual/gerador/gen_mockups.py` — são
os tons que aparecem nos mockups aprovados, não escolhas novas.

| Tema | success | warning | error | info |
| --- | --- | --- | --- | --- |
| `classico` | default MUI | default MUI | default MUI | default MUI |
| `corporate` | `#0F6E56` | `#854F0B` | `#A32D2D` | `#185FA5` |
| `soft` | `#0F6E56` | `#854F0B` | `#993C1D` | `#5F5E5A` |
| `indigo` | `#5DCAA5` | `#EF9F27` | `#F09595` | `#AFA9EC` |
| `techne` | `#0F6E56` | `#8A5200` | `#A32D2D` | `#0C8DCE` |

O `classico` reproduz o default do MUI de propósito (DD-3 da spec anterior: o
tema "sem mudança" não pode mudar).

### Contraste

O teste parametrizado de `contraste.ts` ganha quatro pares novos por tema:
`{success,warning,error,info}.main` contra `background.paper`. Como a varredura
já percorre `TEMAS`, basta adicionar os pares à lista — nenhum tema futuro fica
de fora.

---

## Camada B — escala tipográfica

### Onde entra

Em `montarTema`, no bloco `typography`, ao lado do `fontFamily` que já existe.
A escala é **compartilhada** entre os temas: um tema muda cor e fonte, não
hierarquia.

```ts
typography: {
  ...tokens.typography,
  h3: { fontSize: '1.6875rem', fontWeight: 600 },  // 27px  (era 48px)
  h4: { fontSize: '1.5rem',    fontWeight: 600 },  // 24px  (era 34px)
  h6: { fontSize: '1rem',      fontWeight: 600 },  // 16px  (era 20px)
}
```

Os valores saem da Nota de escala da spec: mockup × 1,415.

### O que não muda

`body1`, `body2`, `subtitle1`, `subtitle2`, `caption`, `h1`, `h2` e `h5`
permanecem no default. A medição mostrou que corpo de texto, tabela, menu e
sidebar já estão dentro do alvo — mexer seria regressão, não aproximação.

`h5` fica de fora porque só tem 6 usos e nenhum deles é título de página ou valor
de KPI. Se a varredura visual da Fase 2 revelar desalinhamento, entra como fix.

### Efeito colateral esperado

O card de Custo Empresa usa `variant="h4"` para `R$ 5.063.295,00`. Reduzir de
34px para 24px encolhe o texto em ~29% e deve aliviar o transbordo. **Não há
garantia de eliminá-lo** — o card tem largura fixa e o valor pode crescer. A
correção definitiva é a Camada C.

---

## Camada B' — remoção das props

Três operações mecânicas, nesta ordem:

| # | Operação | Ocorrências | Efeito |
| --- | --- | --- | --- |
| 1 | Remover `fontWeight="bold"` e `fontWeight="medium"` | 24 | O peso passa a vir de `theme.typography.*` |
| 2 | Remover `color="primary"` de `Typography` com `variant` h1-h6 | 10 | Título assume `text.primary` |
| 3 | Trocar `color="textSecondary"` por `color="text.secondary"` | 18 | Sai da API v4 depreciada |

### O que **não** se remove

`color="success.main"`, `color="warning.main"`, `color="error"`,
`color="info.main"` — 14 ocorrências. São tokens semânticos legítimos, e é
justamente a Camada A que os faz resolver corretamente. Removê-los desfaria o
ganho de A.

Também ficam as 4 ocorrências de `color="primary"` que **não** são título — cada
uma avaliada individualmente na task.

### Distribuição por arquivo

| Arquivo | `fontWeight` | `color="primary"` em título |
| --- | --- | --- |
| `pages/Dashboard/index.tsx` | 16 | 9 |
| `pages/FolhaPagamento/index.tsx` | 3 | — |
| `components/OrganogramaGrafico/index.tsx` | 2 | — |
| `pages/Usuarios/index.tsx` | 1 | 1 |
| `pages/Relatorios/index.tsx` | 1 | — |
| `pages/Relatorios/RelatorioCatalogCard.tsx` | 1 | — |
| `pages/Organograma/index.tsx` | 1 | — |
| `components/AparenciaDialog/index.tsx` | 1 | — |

O Dashboard concentra 2/3 do trabalho — coerente com ele ser a tela que mais
destoa nas capturas.

### Lint anti-regressão

A regra existente (`no-restricted-syntax` contra literais de cor) ganha uma
irmã contra a prop:

```jsonc
{
  "selector": "JSXAttribute[name.name='fontWeight']",
  "message": "fontWeight em prop vence o tema. Defina o peso em theme.typography.* (ver _docs/specs/features/temas-fidelidade-visual/design.md)."
}
```

Escopada a `src/pages/**` e `src/components/**`, com `src/theme/**` isento.
Sem isso, a próxima tela escrita reintroduz o problema — foi exatamente o que
aconteceu com as cores antes da regra anterior existir.

---

## Verificação: medição, não inspeção

O critério "título mede 24px" precisa ser executável, senão vira olhômetro. Duas
camadas de verificação:

1. **Unitária** (`theme/typography.test.ts`) — asserta os valores em
   `criarTema(id).typography` para os cinco temas. Rápido, roda no gate.
2. **Renderizada** (`theme/escalaRenderizada.test.tsx`) — monta um `Typography
   variant="h4"` sob cada tema com `renderWithProviders` e confirma o
   `fontSize` computado. É o que pega o caso de a prop vencer o tema — se alguém
   reintroduzir `fontWeight`, este teste falha antes do lint.

A varredura visual das 20 telas continua sendo feita por captura, comparando com
`_docs/estudo-visual/capturas-implementado/`.

---

## Decisões de design registradas

| # | Decisão | Alternativa descartada | Razão |
| --- | --- | --- | --- |
| DD-1 | Escala compartilhada entre temas | Escala por tema | Um tema muda cor e fonte, não hierarquia; escala por tema multiplicaria a matriz de teste por 5 sem ganho |
| DD-2 | B' remove props em vez de vencê-las | `!important` nos overrides do tema | `!important` funcionaria e deixaria a dívida no lugar; a próxima tela repetiria o padrão |
| DD-3 | `light` explícito nas semânticas do `indigo` | Deixar o MUI derivar | A derivação clareia `main`, produzindo fundo quase branco de avatar no tema escuro |
| DD-4 | `classico` mantém semânticas default do MUI | Dar a ele a paleta do estudo | O `classico` existe para não mudar; dar-lhe cores novas contraria o propósito |
| DD-5 | Só `h3`, `h4`, `h6` mudam | Revisar a escala inteira | Medição normalizada mostrou que as demais já estão no alvo |
| DD-6 | Lint contra `fontWeight` em prop | Só documentar a convenção | Convenção não documentada em lint não sobrevive à terceira tela nova |
| DD-7 | Teste de escala renderizada além do unitário | Só o unitário sobre o objeto tema | O unitário não detecta prop vencendo o tema — que é a causa raiz desta feature |

---

## Riscos

| Risco | Mitigação |
| --- | --- |
| Remover props quebra asserção de teste existente | Fase 3 roda a suíte inteira como gate; asserções de estilo são revistas, nunca deletadas (regra da skill) |
| Títulos escuros desagradam quem já usa o sistema | Decisão explícita do usuário (D2); reversível trocando 10 linhas |
| Redução de escala deixa alguma tela apertada | Varredura visual das 20 telas na Fase 4, comparando com as capturas de referência |
| Semânticas novas falham contraste em algum tema | O teste parametrizado roda no gate da própria task que declara as cores |
| Transbordo do Custo Empresa persiste | Registrado na spec como sem garantia; vira quick task se sobreviver |
| `classico` regride visualmente pela escala | AC explícito: as **cores** do `classico` são preservadas, a escala é aplicada. Se for para preservar a escala também, é decisão a registrar antes do Execute |
