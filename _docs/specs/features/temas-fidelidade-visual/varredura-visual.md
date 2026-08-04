# Varredura visual pós-fidelidade — T10

**Feature**: `_docs/specs/features/temas-fidelidade-visual/`
**Task**: T10 — Fase 4, varredura visual das telas nos cinco temas
**Data**: 2026-08-04
**Branch**: `feat/temas-fidelidade-visual` @ `724d254` (base `0a0eac7`)
**Status**: **executada** — o bloqueio de ambiente registrado antes caiu

## Ambiente da medição

| Item | Valor |
| --- | --- |
| URL | `http://localhost:3000` (dev server servindo o código da branch) |
| Sessão | autenticada, dados reais (310 funcionários, competências 01/2026–06/2026) |
| Viewport | 1335 × 1322 CSS px |
| Instrumento | `getComputedStyle` + `getBoundingClientRect` via MCP `claude-in-chrome`, sonda injetada na página |
| Complemento | capturas de tela (`_docs/estudo-visual/capturas-pos-fidelidade/`) |
| Troca de tema | `localStorage['sistema-folha:tema']` + reload; tema ativo reconfirmado por token computado (`background.default`, `fontFamily`) a cada troca |

Contraste calculado por WCAG 2.x: luminância relativa sobre a cor de primeiro
plano composta com o fundo efetivo, obtido subindo a árvore até o primeiro
`background-color` opaco.

---

## 1. Escala tipográfica — tela × tema

O resultado é **uniforme**: nas 16 telas roteadas e nos 5 temas, **todo** `h4`
mede 24px/600, **todo** `h3` mede 27px/600 e **todo** `h6` mede 16px/600.
Nenhuma exceção, nenhum caso de prop vencendo o tema.

| Variante | Alvo (spec) | indigo | techne | soft | corporate | classico |
| --- | --- | --- | --- | --- | --- | --- |
| `h4` — título de página | 24px / 600 | 24/600 ×16 | 24/600 ×16 | 24/600 ×16 | 24/600 ×16 | 24/600 ×16 |
| `h3` — maior valor de KPI | 27px / 600 | 27/600 ×3 | 27/600 ×3 | 27/600 ×3 | 27/600 ×3 | 27/600 ×3 |
| `h6` — título de card | 16px / 600 | 16/600 ×60 | 16/600 ×60 | 16/600 ×60 | 16/600 ×60 | 16/600 ×60 |
| `h5` (sem override na spec) | — | 24/400 ×3 | 24/400 ×3 | 24/400 ×3 | 24/400 ×3 | 24/400 ×3 |

(Contagens = número de ocorrências somadas nas 16 telas. Todas as ocorrências de
cada variante têm o mesmo par tamanho/peso.)

**AC5 de P1-Escala — atendido**: título de página 24px, maior valor de KPI 27px,
medidos por `getComputedStyle` no navegador, nos cinco temas.

Medições pontuais no `/dashboard` (todas as telas, todos os temas):

- `h4` "Dashboard Gerencial" → `fontSize: 24px`, `fontWeight: 600`
- `h3` "310" (Total de Funcionários) → `fontSize: 27px`, `fontWeight: 600`
- `h4` "R$ 5.063.295,00" (Custo Empresa) → `fontSize: 24px`, `fontWeight: 600`
- `h6` "Evolução da Folha de Pagamento" → `fontSize: 16px`, `fontWeight: 600`

### Achado de hierarquia (não é violação de AC)

`h5` continua no default do MUI: **24px/400** — exatamente o mesmo tamanho do
`h4` de título de página, só que mais leve. Aparece em 3 telas
(`/folha-pagamento` "Resumos da Folha de Pagamento", `/beneficios-mensais`,
`/api-keys`). Em `/folha-pagamento` os dois convivem na mesma tela, um sob o
outro, com o mesmo tamanho. Em `/api-keys` o `h5` é o **único** título — a tela
não tem `h4`, então não tem título de página no padrão da escala.

---

## 2. Cobertura de telas

16 telas roteadas cobertas × 5 temas = **80 medições**.

| # | Tela | Rota | Coberta |
| --- | --- | --- | --- |
| 1 | Dashboard | `/dashboard` | ✅ |
| 2 | Funcionários | `/funcionarios` | ✅ |
| 3 | Folha de Pagamento | `/folha-pagamento` | ✅ |
| 4 | Benefícios Mensais | `/beneficios-mensais` | ✅ |
| 5 | Relatórios | `/relatorios` | ✅ |
| 6 | API Keys | `/api-keys` | ✅ |
| 7 | Usuários | `/usuarios` | ✅ |
| 8 | Linhas de Negócio | `/linhas-negocio` | ✅ |
| 9 | Centros de Custo | `/centros-custo` | ✅ |
| 10 | Cargos | `/cargos` | ✅ |
| 11 | Rubricas | `/rubricas` | ✅ |
| 12 | Rubricas Fixas | `/rubricas-fixas` | ✅ |
| 13 | Tipos de Benefício | `/tipos-beneficio` | ✅ |
| 14 | Organograma (lista) | `/organograma` | ✅ |
| 15 | Organograma (gráfico) | `/organograma` + toggle | ✅ (parcial, ver §7) |
| 16 | Importação | `/importacao` | ✅ |
| 17 | Folha → Funcionários da competência | sub-tela de `/folha-pagamento` | ✅ (só `indigo`) |

### Telas não cobertas

| Tela do inventário | Motivo |
| --- | --- |
| `01-login` | Exigiria encerrar a sessão autenticada do usuário. Não feito por regra de escopo. |
| `05-dashboard-v2` | **Rota não existe mais** em `frontend/src/routes/index.tsx`. O inventário do estudo é anterior à remoção. |
| `03-dashboard-graficos`, `04-dashboard-rubricas` | Não são telas distintas — são regiões de rolagem de `/dashboard`, já cobertas na mesma medição (a sonda mede o documento inteiro, não só o viewport). |
| Diálogo "Ver Rubricas" (`FolhaPagamento` linhas 380/434, `Typography variant="subtitle1"` "Total: R$ …") | **Não coberto por ausência de dado**: o diálogo abre com "Ficha não processada para este funcionário. Execute o processamento da competência." Nenhuma ficha processada na base para os funcionários testados. Ver §6. |

---

## 3. Corte, transbordo e sobreposição

| Tela | Achado | Temas | Veredito |
| --- | --- | --- | --- |
| `/dashboard` — card Custo Empresa | avatar do KPI empurrado para fora do card e cortado por `overflow-x: hidden` | **todos os 5** | defeito de layout, ver §4 |
| `/dashboard` — eixo Y do gráfico de evolução | rótulos `R$ 650.000`…`R$ 2.600.000` quebram em 2 linhas e vazam 7–19px à esquerda do `<svg>` (`overflow: hidden`) → primeiro dígito cortado | **todos os 5** | pré-existente, ver §4 |
| `/funcionarios` | 142–193 elementos com `text-overflow: ellipsis` acionado (nomes longos em cards) | todos | **truncamento por design**, não é defeito |
| `/organograma` (gráfico) | 8–9 rótulos de nó com reticências | todos | truncamento por design |
| `/dashboard` — KPIs `h3` no `techne` | `scrollHeight` 34px > `clientHeight` 32px | só `techne` | cosmético — `overflow: visible`, nada é cortado. Poppins tem métricas mais altas que o `lineHeight` 1.167 do `h3`. |

**Sobreposição real entre irmãos de texto: nenhuma** em nenhuma tela, em nenhum
tema. (Os 4 pares reportados no `/dashboard`/`techne` são `<tspan>` de eixo do
Recharts em linhas consecutivas — as caixas de linha se tocam por 7px, os glifos
não. Verificado por captura.)

**Nenhuma tela apresenta texto ilegível por sobreposição.** Os cortes reais
estão nos dois casos de layout acima, ambos pré-existentes à feature.

---

## 4. Veredito do transbordo do card de Custo Empresa

### **ALIVIADO, mas PERSISTENTE — mudou de forma**

Medições no `/dashboard`, card "Custo Empresa", valor `R$ 5.063.295,00`,
`variant="h4"` (24px/600):

| Tema | Largura do valor | Gap valor→avatar | Avatar além da borda do card | Sobreposição valor×ícone |
| --- | --- | --- | --- | --- |
| `indigo` | 180px | **0px** | **+25px** (cortado) | 0px |
| `soft` | 180px | **0px** | **+25px** (cortado) | 0px |
| `corporate` | 180px | **0px** | **+25px** (cortado) | 0px |
| `classico` | 189px | **0px** | **+34px** (cortado) | 0px |
| `techne` | 191px | **0px** | **+36px** (cortado) | 0px |

Referência dos outros três KPIs no mesmo tema (`techne`): avatar **−17px**
(dentro do card, respeitando o padding); gap valor→avatar de 0px
(Total de Funcionários), 19px (Benefícios Ativos) e 51px (Relação P/D).

Card: 228px de largura, `overflow-x: hidden`, padding interno de 17px.

**Leitura:**

- **Aliviado**: o valor **não cobre mais o ícone**. A sobreposição valor×avatar,
  que o inventário do estado atual descrevia como "`R$ 5.063.295,00` ultrapassa
  a largura do card e encobre parcialmente o ícone", é hoje **0px** em todos os
  temas. A escala reduzida (`h4` de 34px → 24px) recuperou ~30% da largura.
- **Persistente**: a linha continua não cabendo. O `flex` empurra o avatar para
  fora e o `overflow: hidden` do card o corta. Visualmente sobra uma fatia do
  círculo colorido na borda direita — ver `capturas-pos-fidelidade/techne-kpi-row-zoom.png`
  e `indigo-dashboard.jpg`.
- A causa raiz é **estrutural, não tipográfica**: 228px de card para
  `17 + 191 + 0 + 56 + 17 = 281px` de conteúdo. Nenhuma redução de fonte razoável
  fecha 53px. Precisa de `flexWrap`, `minWidth: 0`, valor abreviado
  (`R$ 5,06 mi`) ou card mais largo para o KPI monetário.
- Nas capturas de referência (`capturas-implementado/techne-dashboard.jpg` e
  `indigo-dashboard.jpg`) **o mesmo corte já existia**. O defeito é anterior à
  feature e a feature o atenuou sem eliminá-lo.

### Defeito irmão descoberto na varredura

O eixo Y do gráfico "Evolução da Folha de Pagamento" tem o mesmo problema: os
rótulos `R$ 650.000`+ não cabem na largura do `YAxis`, quebram em duas linhas e
ainda assim começam **7 a 19px à esquerda** da borda do `<svg>`, que tem
`overflow: hidden` — o primeiro caractere é cortado (`R$` vira `H$`, `600.000`
vira `500.000` na renderização). Ocorre nos **cinco temas**, com números
praticamente idênticos (svgL = 305; ticks começam em 286–303). Está presente
também nas capturas de referência. **Fora do escopo da T10** — registrado.

---

## 5. R-1 — contraste ícone × fundo do avatar de KPI

**CONFIRMADO** para `corporate`, `soft` e `techne`. **PARCIALMENTE REFUTADO**
para `classico`.

`Dashboard/index.tsx:206,234,262,290,500,550` — `color: X.main` sobre
`bgcolor: X.light`. Medido no navegador, cor computada do `<svg>` contra o
`background-color` efetivo do `.MuiAvatar-root`.

| Avatar (KPI) | Papel | `indigo` | `techne` | `soft` | `corporate` | `classico` |
| --- | --- | --- | --- | --- | --- | --- |
| Total de Funcionários | `primary` | **6,16** | 1,40 | 1,60 | 1,55 | **4,03** |
| Custo Empresa | `success` | **5,13** | 1,53 | 1,53 | 1,53 | **4,56** |
| Benefícios Ativos | `warning` | **5,28** | 1,54 | 1,57 | 1,57 | **2,47** |
| Relação P/D | `info` | **3,15** | 2,38 | 2,51 | 2,43 | **6,93** |
| Top 5 Proventos (lista) | `success` | **5,13** | 1,53 | 1,53 | 1,53 | **4,56** |
| Top 5 Descontos (lista) | `error` | **5,59** | 1,50 | 1,54 | 1,50 | **4,92** |
| **Reprovados < 3:1 (WCAG 1.4.11)** | | **0 / 6** | **6 / 6** | **6 / 6** | **6 / 6** | **1 / 6** |

Cores computadas (ícone / fundo):

- `techne`: `#0a7ab0`/`#3b94bf`, `#0f6e56`/`#3f8b77`, `#8a5200`/`#a17433`, `#07557b`/`#3b94bf`, `#a32d2d`/`#b55757`
- `soft`: `#5f5e5a`/`#7f7e7b`, `#0f6e56`/`#3f8b77`, `#854f0b`/`#9d723b`, `#42413e`/`#7f7e7b`, `#993c1d`/`#ad634a`
- `corporate`: `#185fa5`/`#467fb7`, `#0f6e56`/`#3f8b77`, `#854f0b`/`#9d723b`, `#104273`/`#467fb7`, `#a32d2d`/`#b55757`
- `indigo`: `#afa9ec`/`#2e2c4a`, `#5dcaa5`/`#23473c`, `#ef9f27`/`#4a3616`, `#7a76a5`/`#2e2c4a`, `#f09595`/`#4a2c2c`
- `classico`: `#1976d2`/`#e3f2fd`, `#2e7d32`/`#e8f5e8`, `#f57c00`/`#fff3e0`, `#115293`/`#e3f2fd`, `#c62828`/`#ffebee`

**Conclusões:**

1. A faixa **1,40–1,60:1** prevista pelo review está **confirmada com números
   quase idênticos** em `corporate`, `soft` e `techne`. Todos os 18 avatares
   desses três temas reprovam em 1.4.11.
2. `indigo` está **acima de 3:1 em todos os seis** (3,15–6,16). O `light`
   explícito por DD-3 funciona — é a prova empírica da decisão.
3. `classico` **não** está em 5,1–6,1 como o review estimou: está em
   **2,47–6,93**, e o avatar de `warning` (Benefícios Ativos) reprova em **2,47**.
   É o mesmo `#f57c00` já coberto pelo `SPEC_DEVIATION` de
   `contraste.test.ts:41-50`, agora com um segundo par afetado.
4. O `info` do `indigo` (3,15) passa por margem estreita — qualquer ajuste
   futuro de `info.light` precisa remedir.

**Decisão sugerida** (fora do escopo da T10): vira task. O fix do review —
declarar `light` explícito nas semânticas de `corporate`, `soft` e `techne` —
resolve 18 dos 19 pares reprovados. O 19º (`classico`/`warning`) é o desvio já
documentado.

---

## 6. R-4 — perda de ênfase sem substituto no tema

**Veredito misto**: 1 ponto confirmado como regressão real, 2 refutados, 1 não
coberto.

| Ponto | Medição | Veredito |
| --- | --- | --- |
| `FolhaPagamento/index.tsx:775` — "Custo Empresa: R$ …" no card de funcionário | `16px/400 rgba(255,255,255,0.7)` — **idêntico** a "Cargo:", "Centro de Custo:", "Linha de Negócio:", "Bruto:" e "Líquido:" no mesmo card | 🔴 **CONFIRMADO** — indistinguível. Era `fontWeight="medium"`; hoje é a mesma pintura das outras cinco linhas. Ver `capturas-pos-fidelidade/indigo-folha-detalhe-dialog.jpg`. |
| `FolhaPagamento` — colunas Total Líquido / Custo Empresa da tabela de resumos | valor: `16px/400` em `primary.main` (`#7F77DD`) e `success.main` (`#5DCAA5`); células vizinhas: `14px/400` em `text.primary` | 🟢 **REFUTADO** — 2px maior **e** colorido. Distinção preservada por tamanho + cor semântica. |
| `Dashboard/index.tsx:523,529,573,579` — top-5 | rubrica `14px/500` em `text.primary`; valor `14px/400` em `success.main`/`error.main`; ocorrências `12px/400` em `text.secondary` | 🟢 **REFUTADO** — três níveis distintos preservados (peso 500 do `subtitle2` + cor semântica no valor + tamanho menor no secundário). |
| `FolhaPagamento/index.tsx:380,434` — `subtitle1` "Total: R$ …" | — | ⚪ **NÃO COBERTO** — o diálogo "Ver Rubricas" retorna "Ficha não processada para este funcionário" em toda a amostra testada. Sem dado, sem medição. |

Os demais pontos de R-4 (`AparenciaDialog:66`, `OrganogramaGrafico:123,155`,
`Organograma:632`) foram varridos: nenhum resultou em corte, sobreposição ou
falha de contraste. A perda de peso neles é uma decisão de densidade visual, não
um defeito mensurável.

---

## 7. Contraste real de texto — achados por tema

Nenhuma tela apresentou **texto ilegível**. Os achados abaixo são infrações de
AA (4,5:1 corpo / 3:1 texto grande) medidas contra o fundo efetivo real.

### 7.1 O padrão dominante — `primary.main` usado como cor de texto

`primary.main` renderizado como **texto** sobre `background.paper` não está na
varredura de `contraste.test.ts` (o par testado é
`primary.contrastText / primary.main`). Medido no navegador:

| Tema | `primary.main` sobre paper | Onde aparece | AA 4,5:1 |
| --- | --- | --- | --- |
| `soft` | **3,39** (`#1D9E75` / `#ffffff`) | valores monetários da tabela de resumos (16px/400 ×7), "Ver Funcionários" (13px/500 ×8), "Normal" (14px/400 ×6), "Filtrar" (14px/500 ×4) | ❌ |
| `corporate` | **3,68** (`#3B82F6` / `#ffffff`) | mesmos pontos (×22 em `/folha-pagamento`) | ❌ |
| `indigo` | **4,48** (`#7F77DD` / `#1c1c28`) | mesmos pontos + "Limpar", "Gerar novamente", "Selecionar Arquivo" | ❌ (margem de 0,02) |
| `classico` | **4,37** (`#1976D2` / `#f8f9fa`) | "Filtrar" (×2) | ❌ (margem de 0,13) |
| `techne` | ✅ passa | — | ✅ |

Contagem por tela e tema (elementos abaixo do mínimo AA):

| Tela | indigo | techne | soft | corporate | classico |
| --- | --- | --- | --- | --- | --- |
| `/folha-pagamento` | 20 | 0 | 23 | 22 | 2 |
| `/importacao` | 6 | 2 | 6 | 6 | 2 |
| `/beneficios-mensais` | 1 | 0 | 3 | 3 | 2 |
| `/relatorios` | 2 | 0 | 2 | 2 | 0 |
| `/usuarios` | 1 | 0 | 1 | 1 | 0 |
| `/rubricas`, `/rubricas-fixas` | 1 cada | 0 | 1 cada | 1 cada | 0 |
| `/api-keys` | 1 | 1 | 1 | 1 | 1 |
| `/organograma` | 0 | 1 | 38 | 0 | 0 |
| `/dashboard` | 0 | 0 | 0 | 0 | 2 |
| `/funcionarios` | 0 | 0 | 1 | 0 | 0 |
| demais 5 telas | 0 | 0 | 0 | 0 | 0 |

### 7.2 Achados específicos

| Tela | Tema | Elemento | Medida | Nota |
| --- | --- | --- | --- | --- |
| `/organograma` | `soft` | 38 chips de contagem, texto branco sobre `secondary.main` `#D85A30` | **3,87** | 13px/400 → exige 4,5. Reprova. |
| `/dashboard` | `classico` | KPI "0" de Benefícios Ativos, `#f57c00` sobre `#ffffff` | **2,70** | 27px/600 → texto grande, exige 3,0. **Reprova mesmo como texto grande.** É o `SPEC_DEVIATION` já documentado, aqui confirmado no pixel. |
| `/dashboard` | `classico` | chip "Estável", `#f57c00` 13px/400 | **2,70** | Mesmo token, agora como texto de corpo. |
| `/api-keys` | todos | botão "Nova API Key" **desabilitado** | 1,83–2,60 | Controle desabilitado — isento por WCAG 1.4.3. Registrado, não é defeito. |
| `/importacao` | todos | botões "Importar …" **desabilitados** | 1,85–2,50 | Idem. |
| `/organograma` (gráfico) | todos | marca d'água "React Flow" 10px | 1,23–2,79 | Atribuição da biblioteca, não é texto do produto. |

### Falso positivo descartado

A sonda acusou 10 elementos "`#1`…`#5` brancos sobre branco, razão 1,00" no
`/dashboard` dos 4 temas claros. **Investigado e descartado**: são os avatares de
posição do top-5, pintados com `background-image: linear-gradient(135deg, …)` —
a sonda só lia `background-color`. Verificado na captura: os badges são legíveis
em todos os temas.

---

## 8. Comparação com `capturas-implementado/`

Comparado `_docs/estudo-visual/capturas-implementado/` (antes desta feature) com
`_docs/estudo-visual/capturas-pos-fidelidade/` (agora).

| Aspecto | Antes (`capturas-implementado/`) | Agora | Alinhamento com os mockups |
| --- | --- | --- | --- |
| Título de página | violeta/roxo (`primary.main`), ~34px, bold | `text.primary`, 24px/600 | ✅ os mockups usam título neutro e discreto |
| Maior valor de KPI | violeta, ~34px, bold | neutro `text.primary`, 27px/600 | ✅ |
| Título de card ("Evolução da Folha…") | violeta, bold | `text.primary`, 16px/600 | ✅ |
| Peso geral | bold espalhado por props inline | 600 vindo do tema, uniforme | ✅ camada B' cumprida |
| Avatares de KPI no `indigo` | tons claros quase brancos (`lighten(main, .2)`) | tons escuros dedicados (`#2e2c4a`, `#23473c`…) | ✅ DD-3 visível na captura |
| Custo Empresa | avatar cortado na borda do card | avatar ainda cortado, mas o valor não cobre mais o ícone | 🟡 aliviado, não resolvido |
| Eixo Y do gráfico | primeiro caractere cortado | idêntico | 🔴 inalterado |
| Fonte no `techne` | Poppins | Poppins (confirmado: `fontFamily` computado) | ✅ |

Arquivos:

- `_docs/estudo-visual/capturas-pos-fidelidade/techne-dashboard.jpg` ↔ `capturas-implementado/techne-dashboard.jpg`
- `_docs/estudo-visual/capturas-pos-fidelidade/indigo-dashboard.jpg` ↔ `capturas-implementado/indigo-dashboard.jpg`
- `_docs/estudo-visual/capturas-pos-fidelidade/corporate-dashboard.jpg` ↔ `capturas-implementado/corporate-dashboard.jpg`
- `_docs/estudo-visual/capturas-pos-fidelidade/techne-folha.jpg` ↔ `capturas-implementado/techne-folha.jpg`
- `_docs/estudo-visual/capturas-pos-fidelidade/techne-kpi-row-zoom.png` — recorte da linha de KPIs
- `_docs/estudo-visual/capturas-pos-fidelidade/indigo-folha-detalhe-dialog.jpg` — evidência de R-4

(`_docs/estudo-visual/` está em `.gitignore`; as capturas ficam locais.)

---

## 9. Resumo dos vereditos da T10

| "Done when" | Resultado |
| --- | --- |
| Título 24px e maior KPI 27px por `getComputedStyle`, registrados | ✅ confirmado nos 5 temas, nas 16 telas |
| Nenhuma tela com texto ilegível, cortado ou sobreposto | 🟡 **zero sobreposições e zero ilegibilidades**; dois cortes de layout **pré-existentes** persistem nos 5 temas (avatar do Custo Empresa, eixo Y do gráfico) |
| Estado do transbordo do Custo Empresa registrado | ✅ **aliviado, persistente** — sobreposição valor×ícone 0px (era real); avatar 25–36px fora do card e cortado |
| Comparação com as capturas de referência | ✅ §8 |
| Gate `lint && test && build` | ✅ ver commit |

## 10. Defeitos registrados (fora do escopo da T10)

1. **D-1** — Card de KPI de Custo Empresa: conteúdo de 281px em card de 228px;
   avatar cortado por `overflow: hidden` nos 5 temas.
2. **D-2** — Eixo Y do gráfico do Dashboard: rótulos vazam à esquerda do `<svg>`
   e têm o primeiro caractere cortado nos 5 temas.
3. **D-3** — R-1 confirmado: 18 pares ícone×avatar abaixo de 3:1 em `corporate`,
   `soft` e `techne`; +1 em `classico` (`warning`).
4. **D-4** — R-4 confirmado no ponto `FolhaPagamento:775`: "Custo Empresa: R$ …"
   ficou tipograficamente idêntico às linhas vizinhas.
5. **D-5** — `primary.main` como cor de **texto** sobre `background.paper`
   reprova AA em `soft` (3,39), `corporate` (3,68), `indigo` (4,48) e `classico`
   (4,37). Par não coberto por `contraste.test.ts` (é a R-2 do review,
   confirmada com números).
6. **D-6** — `secondary.main` do `soft` com texto branco: 3,87 em 38 chips do
   Organograma.
7. **D-7** — `h5` sem override na escala fica em 24px/400, mesmo tamanho do
   título de página; `/api-keys` não tem `h4` algum.

---

## Follow-up — D-3 e D-5 fechados (2026-08-04)

Acrescentado após a varredura; os achados acima ficam como registrados.

- **D-3** — fechado pela quick task 012 (`d55132d`, `649041a`): tints explícitos nas
  semânticas de `corporate`, `soft` e `techne`, e `warning.main` do `classico`.
- **D-5** — fechado pela quick task 013 (`641fb2e`, `5a92326`). `primary.main` foi
  ajustado na origem, sem token de acento novo, por decisão do usuário: `classico`
  `#1976d2` → `#1873cd` (4,37 → 4,56 vs default; 4,60 → 4,80 vs paper), `corporate`
  `#3B82F6` → `#1167F4` (3,68 → 4,92), `soft` `#1D9E75` → `#188361` (3,39 → 4,71),
  `indigo` `#7F77DD` → `#8078DD` (4,48 → 4,53; tema escuro, então clareia).
  `techne` já passava (5,63) e não mudou. `contrastText` de `corporate` e `soft`
  passou a `#FFFFFF`. O par `primary.main × background.paper` entrou na varredura de
  `contraste.test.ts` com piso 4,5:1, junto com `primary.main × primary.light` a
  3:1 — fecha também a R-2 do `code-review.md`.

D-1, D-2, D-4, D-6 e D-7 seguem abertos.
