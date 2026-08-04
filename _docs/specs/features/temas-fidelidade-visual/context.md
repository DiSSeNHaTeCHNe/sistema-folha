# Fidelidade Visual dos Temas — Context

Decisões do usuário e rastreabilidade com o estudo de origem.

---

## Origem: onde cada requisito nasceu

Esta feature não parte de uma ideia — parte de medição. A cadeia é:

```
_docs/estudo-visual/propostas-visual-sistema-folha.pdf   (mockups, 4 direções)
        ↓
_docs/specs/features/temas-visuais/                       (implementado, PASS @ ab2cf01)
        ↓
_docs/estudo-visual/inventario-visual-estado-atual.pdf    (20 telas antes)
_docs/estudo-visual/capturas-implementado/                (telas depois)
        ↓
_docs/estudo-visual/aproximacao-mockups.md + .pdf         (3 causas medidas)
        ↓
esta feature                                              (camadas A, B, B')
```

Ler `aproximacao-mockups.md` antes de projetar qualquer task. As nuâncias que
não cabem na spec estão lá — em especial as capturas lado a lado, que mostram
por que "só trocar a cor" não bastou.

---

## D1 — Cores semânticas por tema, derivadas da paleta

**Pergunta**: `success`, `warning`, `error` e `info` devem ser definidas por tema
ou compartilhadas entre os cinco?

**Escolha**: por tema, derivadas da paleta de cada um.

**Alternativas descartadas**:

- *Conjunto compartilhado* — um verde/âmbar/vermelho/azul únicos, escolhidos para
  passar contraste nos cinco. Menos trabalho, mas o Indigo dark ficaria com cores
  que não são dele, que é justamente o defeito visível hoje.
- *Dois conjuntos (claro/escuro)* — meio-termo; resolveria o Indigo dark mas
  deixaria Techne e Corporate com o mesmo verde, sem relação com o violeta ou o
  azul de cada um.

**Consequência**: cada tema ganha quatro cores a escolher, e cada escolha precisa
passar no teste de contraste. Mais trabalho de curadoria; resultado coerente.

**Referência de partida** — a paleta do estudo já sugere valores por família:

| Tema | success | warning | error | info |
| --- | --- | --- | --- | --- |
| classico | preservar o atual | preservar | preservar | preservar |
| corporate | `#0F6E56` | `#854F0B` | `#A32D2D` | `#185FA5` |
| soft | `#0F6E56` | `#854F0B` | `#993C1D` | `#5F5E5A` |
| indigo | `#5DCAA5` | `#EF9F27` | `#F09595` | `#AFA9EC` |
| techne | `#0F6E56` | `#8A5200` | `#A32D2D` | `#0C8DCE` |

Os valores de `corporate`, `soft`, `indigo` e `techne` vêm do gerador dos mockups
(`_docs/estudo-visual/gerador/gen_mockups.py`, dicionário `THEMES`), então já são
os tons que aparecem nas propostas aprovadas. O `info` do techne (`#0C8DCE`) é o
azul secundário do site institucional.

**Cuidado com o `classico`**: ele foi construído fora da fábrica (DD-3 da spec
anterior) para não regredir visualmente. Suas semânticas devem reproduzir o
default do MUI, não os valores acima — caso contrário o tema "sem mudança"
muda.

---

## D2 — Títulos escuros, não coloridos

**Pergunta**: nos mockups os títulos são escuros; hoje são azuis/violeta.
Remover `color="primary"` dos títulos?

**Escolha**: sim, títulos escuros.

**Alternativas descartadas**:

- *Manter coloridos* — preservaria a identidade atual, mas deixaria uma diferença
  visível em relação aos mockups justamente no elemento mais proeminente da tela.
- *Só o título principal* — inconsistência dentro da própria tela: título escuro
  com subtítulos de card coloridos.

**Consequência**: mudança perceptível para quem já usa o sistema. A cor de acento
deixa de aparecer em texto e passa a valer para ações — botões, links, estados
selecionados. É o padrão dos mockups e reduz a competição visual na tela.

**Escopo exato**: 10 ocorrências de `color="primary"` em `Typography` com
`variant` de `h1` a `h6` (9 em `pages/Dashboard/index.tsx`, 1 em
`pages/Usuarios/index.tsx`). As outras 4 ocorrências de `color="primary"` não
são títulos e devem ser avaliadas caso a caso.

---

## D3 — Correção de escala (decisão do agente, não do usuário)

O estudo de origem comparou pixels do mockup com pixels do sistema sem normalizar
a escala. O mockup foi renderizado num quadro de **1070px**; o sistema roda a
**1515px**. Fator: **1,415**.

Isso inflou os números do estudo. Onde ele dizia "2×" e "2,5×", a diferença real
é 1,4× e 1,8×. E três itens que o estudo listava como divergentes — densidade de
tabela, largura de sidebar e tamanho do menu — na verdade **já estão dentro do
alvo**.

Efeito no escopo: a Camada B caiu de "revisar a escala inteira" para "ajustar
três variantes de tipografia". O `aproximacao-mockups.md` permanece como está,
com esta correção registrada aqui e na Nota de escala da spec.

---

## Contexto técnico levantado

- **Precedência confirmada empiricamente.** O título do Dashboard renderiza com
  a classe emotion `.css-1qoxlvu { font-size: 2.125rem; font-weight: 700 }`. O
  MUI funde a variante do tema com as system props numa única classe; a prop
  vence. Por isso a Camada B sozinha não resolveria — daí a B'.
- **Inventário de props em arquivos rastreados**: 23 `fontWeight="bold"` +
  1 `fontWeight="medium"` = 24; `color="text.secondary"` 26;
  `color="textSecondary"` 18 (API v4 depreciada); `color="primary"` 14 (10 em
  títulos); semânticas (`success.main`, `warning.main`, `error`, `info.main`,
  `error.main`) 14.
- **Variantes em uso**: `body2` 45, `h6` 33, `h4` 17, `caption` 12,
  `subtitle2` 11, `subtitle1` 9, `h5` 6, `body1` 4, `h3` 3.
- **Gaps abertos herdados** de `temas-visuais` (validation P2): TEMA-14
  (Organograma indigo sem asserção de distinguibilidade) e TEMA-10 (`index.css`
  sem teste que rejeite cor). Não são desta feature, mas a Fase 1 encosta neles.
- **Literais remanescentes** no código rastreado:
  `pages/Organograma/index.tsx:1218` (`color: '#fff'`) e
  `pages/Relatorios/RelatorioCatalogCard.tsx:61` (`boxShadow` com `rgba`).
- **`pages/DashboardCustomizavel/`** está em `.gitignore` com 17+ literais no
  working tree. Fora do escopo, conforme a validation anterior.
- **Baseline de testes**: 559 (registrado na validation de `temas-visuais`).
- Sem lições confirmadas no store (`lessons.py list --status confirmed` vazio).
