# Folha CLT — Bruto, Líquido e Custo Empresa Specification

**Parent / source:** `_docs/folha-salarial/especificacao-etapa-1-clt.md`  
**Related:** `acl-scoped-folha-resumo` (RSF-01…08, B1), `beneficios-mensais`, AD-007, AD-011  
**Complexity:** Large  
**Spec status:** Draft 2026-07-28 — ACL + composição 3 origens (INT-1/INT-2) — ver `context.md`

## Problem Statement

O sistema calcula e exibe folha com semântica incompleta: a tela mostra um “total” local (≈ líquido), o dashboard usa líquido como “custo mensal”, e não há bruto/líquido/custo empresa auditáveis por funcionário. O backend tem `FolhaTotalizacaoService` e `/totais-funcionarios`, mas sem operadores parametrizáveis, sem persistência em ficha, sem rateio de encargos e **sem estender o contrato ACL** já validado em `acl-scoped-folha-resumo` para os novos totalizadores.

Gestores com escopo parcial precisam ver bruto, líquido e custo empresa **do seu organograma** — com paridade entre resumo, cards e detalhe — sem vazar totais globais da importação ADP.

Além disso, custos Techne que **não** vêm do ADP nem de `beneficio_mensal` (ajuda de custo, housing, viagens, férias proporcionais) precisam de cadastro, injeção no processamento mensal e rastreio de origem nas abas de detalhe — hoje inexistentes no código.

## Goals

- [ ] Calcular e persistir bruto, líquido e custo_folha por colaborador CLT × competência (`ficha_mensal`); **custo empresa composto na consulta** (folha + encargos + benefícios)
- [ ] Cadastrar **custos fixos Techne** por funcionário e injetá-los como `ficha_linha` no processamento mensal (INT-2)
- [ ] Parametrizar impacto de cada rubrica via operadores (−1/0/+1) no cadastro mestre
- [ ] Expor APIs e UI com três totalizadores no resumo, cards e detalhe por aba
- [ ] **Manter contrato ACL dual-path:** `acessoTotal` → snapshot/ficha global; escopo parcial → recalcular de linhas filtradas; encargos = 0 no scoped
- [ ] Garantir paridade resumo scoped ↔ soma dos cards na mesma competência
- [ ] Corrigir dashboard: custo empresa real (não líquido)

## Out of Scope

| Feature | Reason |
| --- | --- |
| PJ, terceiros, retenções, visões paralelas | Etapa 2+ (`especificacao-etapa-1-clt` §3.2) |
| Rateio de encargos proporcional ao **escopo** do gestor | Rejeitado em `acl-scoped-folha-resumo` (B1) |
| Resumos persistidos por centro de custo | Agregação on-the-fly (padrão existente) |
| Portal holerite / flag visível ao colaborador | Fase futura |
| Meses especiais 13–18, 1ª parcela pagamento | Etapa 2 ou posterior |
| Regime explícito além do seed CLT + placeholders inativos | P2 schema; regras PJ/estágio na Etapa 2/3 |
| Rubricas **calculadas por regra complexa** (dissídio m−2, VR automático por jornada, housing condicional a CC) | P3 — após motor + custos fixos base |
| Reescrever ACL de benefícios | Já adequado (`beneficios-mensais`) |
| Custos fixos via `beneficio_mensal` | Origens distintas; benefícios = planilha operacional (INT-1 vs INT-2) |

---

## Composição de custo — origens de dados

Todo totalizador SHALL tratar estas origens de forma **distinta**:

| Origem | Exemplos | Persistência | Quando entra no cálculo | Impacto bruto/líquido |
| --- | --- | --- | --- | --- |
| **FOLHA_ADP** | Salário, descontos importados | `ficha_linha` pós-import ADP | Importação ADP | Via operadores da rubrica |
| **CUSTO_FIXO** | Ajuda de custo, viagens, housing (valor cadastrado) | `funcionario_rubrica_fixa` → injetado como `ficha_linha` | `POST /folha-pagamento/processar` | Via operadores da rubrica |
| **CALCULADO** | Férias proporcionais CLT (fator 2,5) | Gerado no processamento → `ficha_linha` | `POST /folha-pagamento/processar` | Via operadores da rubrica |
| **BENEFICIO** | VR, AM operadora (planilha RH) | `beneficio_mensal` (módulo separado) | **Consulta** (`BeneficioConsultaPort`) | **Somente** `custoEmpresa` |

```text
custoFolha      = Σ ficha_linha (FOLHA_ADP + CUSTO_FIXO + CALCULADO) × operador_custo
custoBeneficios = BeneficioConsultaPort (na consulta)
custoEmpresa    = custoFolha + encargosRateados + custoBeneficios
bruto / liquido = Σ ficha_linha × operador_bruto / operador_liquido (sem BENEFICIO)
```

Decisões: INT-1 (benefícios), INT-2 (custos fixos) em `context.md`.

## ACL Contract (cross-cutting — mandatory)

Todo endpoint e tela que exiba bruto, líquido ou custo empresa SHALL implementar **dois caminhos**:

| Caminho | Condição | Fonte | Encargos rateados |
| --- | --- | --- | --- |
| **Global** | `acessoTotal=true` | Snapshot `ResumoFolhaPagamento` + totais persistidos em `ficha_mensal` (pós-processamento) | Rateio proporcional ao bruto CLT (D4-CLT) |
| **Scoped** | usuário com `centrosCustoIds` válidos | Recalcular `Σ(valor × operador)` de linhas ativas cujo CC do funcionário ∈ escopo | **`0`** (B1 herdado) |

**Regras invariantes (herdam RSF-01…05):**

1. Scoped **nunca** lê totais persistidos globais da ficha/snapshot sem recalcular.
2. `totalEncargos` e parcela rateada no custo empresa scoped = **`0`**.
3. Competência no snapshot sem linhas no escopo → linha com **zeros** preservando metadados (A2).
4. Deny (sem nó/centros e sem `acessoTotal`) → lista vazia.
5. Benefícios no custo: **buscar na consulta** via `BeneficioConsultaPort` — não persistir como `ficha_linha` (INT-1).
6. Custos fixos: **cadastro + injeção no processamento** como `ficha_linha` com `origemLinha ∈ {FOLHA_ADP, CUSTO_FIXO, CALCULADO}` — **não** compor na consulta (INT-2); ACL igual às demais linhas (filtro por CC do funcionário).
7. **Paridade:** totais scoped do resumo = soma dos totais por funcionário visíveis (± arredondamento).

Ver decisões em `context.md` (ACL-1…5, INT-1, INT-2).

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Herança ACL | Estender `acl-scoped-folha-resumo`; não alterar B1/A2 | Feature ACL verificada; etapa 1 §10.6 | y |
| Fonte transição | `folha_pagamento` + projeção `ficha_mensal` (ARCH-1 / D5-CLT Opção B) | Menor risco na importação | y |
| Operadores iniciais | Derivados de `tipo_rubrica` (D3-CLT) | Paridade com comportamento atual | y |
| Rateio encargos (global) | Proporcional ao bruto CLT (D4-CLT Opção A) | Spec etapa 1 §6.2 | y |
| Naming API | `custoEmpresa` (spec); deprecar alias `salCustoTechne` no DTO | Alinhar semântica | y |
| FE agregação | Zero cálculo local de totais; API only (FE-1) | Evita divergência ACL | y |
| Dashboard scoped | Custo empresa recalculado do escopo, não `totalLiquido` snapshot | Mesmo padrão evolução scoped | y |
| Competência 13º | Flag `decimoTerceiro` separa normal vs 13º em todos os caminhos ACL | Já existente | y |
| Benefícios no custo | **Compor na consulta** via `BeneficioConsultaPort`; não gravar em `ficha_linha` (INT-1) | Módulo separado; ACL na leitura | y |
| Custos fixos Techne | **Cadastro** `funcionario_rubrica_fixa` + **injeção** no processamento como `ficha_linha` (INT-2) | Distinto de benefícios; impacta bruto/líquido/custo_folha via operadores | y |
| Alteração de custo fixo | Reflete após **reprocessar** competência (não na consulta, unlike BENEFICIO) | Linhas materializadas na ficha | y |
| `origemLinha` em `ficha_linha` | Enum `FOLHA_ADP`, `CUSTO_FIXO`, `CALCULADO` | UI e auditoria; abas de detalhe | y |

**Open questions:** none — ver `context.md`.

---

## User Stories

### P1: Operadores de rubrica no cadastro ⭐ MVP

**User Story**: Como gestor de RH, quero definir o impacto de cada rubrica nos totalizadores bruto, líquido e custo, para parametrizar o motor sem alterar código.

**Why P1**: Base do cálculo; substitui coeficientes fixos de `tipo_rubrica`.

**Acceptance Criteria**:

1. (FCLT-01) WHEN migração Flyway rodar THEN cada rubrica existente SHALL receber operadores derivados de `tipo_rubrica` (PROVENTO +1/+1/+1; DESCONTO 0/−1/0; INFORMATIVO 0/0/0)
2. (FCLT-02) WHEN operador cadastra/edita rubrica THEN API SHALL aceitar e persistir `operadorBruto`, `operadorLiquido`, `operadorCusto` ∈ {−1, 0, 1}
3. (FCLT-03) WHEN UI de Rubricas salvar rubrica THEN SHALL exibir e editar os três operadores com labels acessíveis

**Independent Test**: Criar rubrica DESCONTO com operadores custom → linha impacta só líquido.

---

### P1: Motor de cálculo e ficha mensal ⭐ MVP

**User Story**: Como sistema, quero persistir bruto, líquido e custo_folha por funcionário × competência após importação, e compor custo empresa na consulta com benefícios e encargos.

**Why P1**: Critério de aceite etapa 1 §10.1–2; separa folha ADP de benefícios mensais (INT-1).

**Acceptance Criteria**:

1. (FCLT-04) WHEN importação ADP concluir para competência THEN SHALL existir `ficha_mensal` por funcionário importado com `bruto`, `liquido`, `custoFolha` persistidos (derivados das `ficha_linha`)
2. (FCLT-05) WHEN motor recalcular ficha THEN `bruto = Σ(valor × operador_bruto)`, `liquido = Σ(valor × operador_liquido)`, `custoFolha = Σ(valor × operador_custo)` das `ficha_linha`
3. (FCLT-06) WHEN qualquer endpoint de consulta calcular `custoEmpresa` THEN SHALL compor na leitura: `custoFolha + encargosRateados + custoBeneficios`, onde `custoBeneficios` vem de `BeneficioConsultaPort` para a competência — **sem** depender de valor persistido de benefícios na ficha
4. (FCLT-07) WHEN linha inserida na ficha THEN operadores SHALL ser **copiados** da rubrica (snapshot na linha) e `origemLinha` SHALL ser persistido (`FOLHA_ADP`, `CUSTO_FIXO` ou `CALCULADO`)
5. (FCLT-08) WHEN testes unitários do motor rodarem THEN SHALL cobrir PROVENTO+DESCONTO, operador custom e arredondamento 2 casas
6. (FCLT-INT-01) WHEN benefícios forem lançados/importados **após** a folha da mesma competência THEN `custoEmpresa` na consulta SHALL refletir os novos valores **sem** reprocessar importação ADP

**Independent Test**: Importar ADP → importar benefícios → consultar totais → custo inclui benefícios; alterar benefício → consulta atualiza sem reimport folha.

---

### P1: ACL — Resumo da folha com três totalizadores ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero ver bruto, líquido e custo empresa no resumo **apenas do meu organograma**, para que os números batam com os cards de funcionários.

**Why P1**: Extensão obrigatória de RSF-01; pedido explícito do usuário.

**Acceptance Criteria**:

1. (FCLT-ACL-01) WHEN usuário **sem** `acessoTotal` consultar `GET /resumo-folha-pagamento*` THEN DTO SHALL incluir `totalBruto`, `totalLiquido`, `totalCustoEmpresa` calculados **somente** de linhas ativas cujo CC ∈ `centrosCustoIds`, usando operadores de rubrica
2. (FCLT-ACL-02) WHEN caminho scoped THEN `totalEncargos = 0` e `totalCustoEmpresa` SHALL **excluir** encargos rateados (custo_folha scoped + benefícios scoped + 0)
3. (FCLT-ACL-03) WHEN usuário com `acessoTotal=true` THEN resumo SHALL incluir `totalBruto`, `totalLiquido`, `totalCustoEmpresa` agregados da competência global (snapshot estendido e/ou soma de fichas + encargos do snapshot)
4. (FCLT-ACL-04) WHEN competência existir no snapshot mas sem linhas no escopo THEN SHALL retornar competência com `totalBruto=0`, `totalLiquido=0`, `totalCustoEmpresa=0`, demais campos monetários 0, metadados preservados (A2)
5. (FCLT-ACL-05) WHEN deny (sem nó/centros) THEN lista vazia — sem regressão RSF-03
6. (FCLT-ACL-06) WHEN testes unitários scoped rodarem THEN SHALL falhar se caminho scoped ler totais persistidos globais da ficha sem recalcular (sensor discrimination)

**Independent Test**: 2 CCs; usuário só CC-A; resumo scoped bruto/custo ≠ global; competência só CC-B → zeros; soma cards = resumo.

---

### P1: ACL — Totais por funcionário (cards) ⭐ MVP

**User Story**: Como gestor, quero ver bruto, líquido e custo empresa no card de cada funcionário, respeitando meu escopo de acesso.

**Why P1**: UI principal; substitui “Total” único calculado no FE.

**Acceptance Criteria**:

1. (FCLT-ACL-07) WHEN `GET /folha-pagamento/totais-funcionarios` for chamado THEN resposta SHALL incluir `bruto`, `liquido`, `custoFolha`, `custoBeneficios`, `custoEmpresa` por funcionário
2. (FCLT-ACL-08) WHEN usuário scoped THEN endpoint SHALL considerar **apenas** linhas de funcionários cujo CC ∈ `centrosCustoIds` **antes** de totalizar; encargos rateados = 0 por funcionário
3. (FCLT-ACL-09) WHEN usuário `acessoTotal` THEN encargos rateados SHALL aplicar rateio D4-CLT por funcionário
4. (FCLT-ACL-10) WHEN FE listar cards de funcionários THEN SHALL consumir API de totais (não agregar localmente de `/folha-pagamento`)
5. (FCLT-ACL-11) WHEN mesma competência THEN soma de `bruto`/`liquido`/`custoEmpresa` dos cards SHALL igualar totais scoped do resumo (FCLT-ACL-06)

**Independent Test**: Scoped user → cards só do escopo; soma = resumo; acessoTotal → encargos > 0 no custo.

---

### P1: ACL — Detalhe por totalizador (abas) ⭐ MVP

**User Story**: Como gestor, quero abrir o detalhe do funcionário em abas Bruto / Líquido / Custo, vendo só rubricas que compõem cada total, dentro do meu escopo.

**Why P1**: Pedido do usuário; API `totalizer=` da etapa 1 §7.

**Acceptance Criteria**:

1. (FCLT-ACL-12) WHEN `GET /folha-pagamento/fichas/{id}/linhas?totalizer=GROSS|NET|COMPANY_COST` THEN SHALL retornar linhas de `ficha_linha` (`FOLHA_ADP`, `CUSTO_FIXO`, `CALCULADO`) com `contribuicao = valor × operador` para o totalizador pedido, **somente** se operador ≠ 0
2. (FCLT-ACL-13) WHEN usuário scoped tentar ficha de funcionário fora do escopo THEN SHALL retornar 404 ou lista vazia (mesmo critério `aplicarFiltroAcesso`)
3. (FCLT-ACL-14) WHEN FE abrir detalhe THEN SHALL exibir abas acessíveis por teclado; cada aba lista rubricas filtradas pela API (sem misturar totalizadores)
4. (FCLT-ACL-15) WHEN FE abrir aba Custo THEN SHALL listar: (a) linhas de `ficha_linha` com operador_custo ≠ 0, agrupadas ou rotuladas por `origemLinha` (`FOLHA_ADP`, `CUSTO_FIXO`, `CALCULADO`); (b) lançamentos de `beneficio_mensal` obtidos **na consulta** (`origem=BENEFICIO`); encargos rateados **não** aparecem no scoped

**Independent Test**: Funcionário com PROVENTO+DESCONTO → aba Bruto só proventos; aba Líquido ambos; scoped não vê funcionário de outro CC.

---

### P1: ACL — Dashboard custo empresa ⭐ MVP

**User Story**: Como gestor, quero que o dashboard mostre custo empresa real do meu escopo, não o líquido da folha global.

**Why P1**: Critério etapa 1 §10.4; estende RSF-06 para custo.

**Acceptance Criteria**:

1. (FCLT-ACL-16) WHEN `GET /dashboard/stats` e usuário scoped THEN `custoMensalFolha` (renomear label FE para “Custo Empresa”) SHALL refletir custo empresa do escopo (custo_folha + benefícios + 0 encargos), **não** `resumo.totalLiquido`
2. (FCLT-ACL-17) WHEN `acessoTotal` THEN KPI SHALL usar custo empresa global da competência mais recente (com encargos rateados)
3. (FCLT-ACL-18) WHEN evolução mensal scoped THEN pontos SHALL usar custo empresa scoped por competência (paridade com RSF-06)

**Independent Test**: Scoped dashboard ≠ liquido global; acessoTotal inclui encargos.

---

### P1: Frontend — Resumo e cards ⭐ MVP

**User Story**: Como operador de RH, quero colunas bruto/líquido/custo no resumo e nos cards, consumindo a API correta.

**Why P1**: Entrega visível; remove cálculo local incorreto.

**Acceptance Criteria**:

1. (FCLT-09) WHEN tela Folha — Resumo THEN SHALL exibir colunas Bruto, Líquido, Custo Empresa além das existentes
2. (FCLT-10) WHEN tela Folha — Cards THEN SHALL exibir os três valores por funcionário
3. (FCLT-11) WHEN detalhe funcionário THEN SHALL usar abas por totalizador (FCLT-ACL-12…15)
4. (FCLT-12) WHEN valores monetários THEN FE SHALL tratar como string decimal (sem `Number()` para cálculo)

**Independent Test**: Walkthrough UI scoped vs total — números batem com API Postman.

---

### P2: Rateio de encargos (acesso total) 

**User Story**: Como analista com acesso total, quero encargos patronais rateados por funcionário no custo empresa, para drill-down completo.

**Why P2**: D4-CLT; depende de ficha + bruto persistido.

**Acceptance Criteria**:

1. (FCLT-13) WHEN pós-import e `acessoTotal` THEN encargos rateados por funcionário = `(bruto_func / bruto_total_competencia) × totalEncargos_snapshot`, arredondado HALF_UP 2 casas; soma das parcelas = totalEncargos ± R$ 0,01
2. (FCLT-14) WHEN scoped THEN rateio **não** executado (encargos = 0)

**Independent Test**: 2 funcionários, bruto 8k/2k, encargos 1k → 800/200.

---

### P2: Regime de trabalho (seed CLT)

**User Story**: Como sistema, quero vínculo explícito CLT para preparar etapas 2/3.

**Acceptance Criteria**:

1. (FCLT-15) WHEN migração rodar THEN `regime_trabalho` seed CLT ativo; demais placeholders inativos; todos funcionários existentes → CLT

**Independent Test**: GET funcionário retorna regime CLT.

---

### P2: Custos fixos — cadastro por funcionário ⭐ MVP

**User Story**: Como operador de RH, quero cadastrar rubricas fixas de custo Techne por funcionário (ajuda de custo, viagens, housing), para que compõem bruto/líquido/custo_folha sem vir do ADP ou da planilha de benefícios.

**Why P2**: Terceira origem de custo (INT-2); distinta de `beneficio_mensal` (INT-1).

**Acceptance Criteria**:

1. (FCLT-18) WHEN migração Flyway rodar THEN SHALL existir tabela `funcionario_rubrica_fixa` com: `funcionario_id`, `rubrica_id`, `valor` (nullable se rubrica for calculada), `vigencia_inicio`, `vigencia_fim` (nullable = aberta), `comentario`, `ativo`
2. (FCLT-19) WHEN operador criar lançamento fixo informando funcionário, rubrica do catálogo, valor e vigência THEN API SHALL persistir e retornar id; rubrica SHALL existir no cadastro mestre com operadores definidos
3. (FCLT-20) WHEN operador tentar duplicata ativa (mesmo funcionário + rubrica + vigência sobreposta) THEN sistema SHALL retornar HTTP 409
4. (FCLT-21) WHEN UI **Rubricas Fixas** (ou seção equivalente em Cadastros) THEN SHALL permitir CRUD filtrado por funcionário/nome/rubrica, com campos valor e vigência acessíveis

**Independent Test**: Cadastrar ajuda de custo R$ 500 para funcionário → registro persistido → listagem filtra por funcionário.

---

### P2: Custos fixos — injeção no processamento mensal ⭐ MVP

**User Story**: Como sistema, quero injetar custos fixos vigentes como linhas da ficha no processamento da competência, para que entrem em bruto/líquido/custo_folha via operadores.

**Why P2**: Materializa INT-2; alinha pipeline etapa 1 §5.

**Acceptance Criteria**:

1. (FCLT-22) WHEN `POST /folha-pagamento/processar` para competência THEN SHALL, por funcionário CLT com ficha na competência, ler `funcionario_rubrica_fixa` vigentes e inserir `ficha_linha` com `origemLinha=CUSTO_FIXO`, valor do cadastro e operadores copiados da rubrica
2. (FCLT-23) WHEN linhas ADP e fixas coexistirem THEN motor SHALL recalcular `bruto`/`liquido`/`custoFolha` incluindo **ambas** as origens; duplicata mesma rubrica ADP+fixa SHALL ser tratada conforme regra de deduplicação do processamento (preferir ADP se conflito — log WARN)
3. (FCLT-24) WHEN custo fixo alterado no cadastro **após** processamento THEN totais SHALL atualizar **somente** após novo `POST /processar` na competência — consulta **não** lê cadastro diretamente (diferente de BENEFICIO)
4. (FCLT-INT-02) WHEN usuário scoped consultar totais THEN linhas `CUSTO_FIXO` SHALL respeitar ACL por CC do funcionário (mesmo critério `FOLHA_ADP`)

**Independent Test**: Processar out/2024 → ficha inclui linha CUSTO_FIXO; alterar cadastro → totais unchanged até reprocessar.

---

### P2: Férias proporcionais CLT (calculado)

**User Story**: Como sistema, quero injetar rubrica de férias proporcionais CLT (fator 2,5) no processamento mensal.

**Why P2**: Rubrica calculada §6.3 etapa 1; origem `CALCULADO`.

**Acceptance Criteria**:

1. (FCLT-16) WHEN `POST /folha-pagamento/processar` com `opcoes.recalcularFerias=true` THEN SHALL inserir/atualizar `ficha_linha` da rubrica férias CLT (cod. semântico `5000` ou mapeamento configurado) com valor derivado do fator **2,5** e `origemLinha=CALCULADO`
2. (FCLT-25) WHEN férias recalculadas THEN linha SHALL impactar totais conforme operadores da rubrica no catálogo

**Independent Test**: Processar competência → linha férias presente com origem CALCULADO; bruto/custo refletem operadores.

---

### P3: Rubricas calculadas avançadas

**User Story**: Como RH, quero regras automáticas para dissídio, VR por jornada e housing condicional a CC.

**Why P3**: Catálogo §6.4 completo; depende de configs e motor estável.

**Acceptance Criteria**:

1. (FCLT-17) WHEN processamento mensal e configs aplicáveis THEN SHALL injetar linhas `CALCULADO` para rubricas parametrizadas (dissídio, VR, housing) conforme etapa 1 §6.4–6.5

**Independent Test**: Funcionário elegível VR jornada ≥ 8h → linha VR gerada no processamento.

## Edge Cases

- WHEN custo fixo cadastrado mas competência **não** reprocessada THEN consulta exibe valores da última ficha (stale até processar)
- WHEN rubrica fixa sem valor e rubrica não é calculada THEN rejeitar no cadastro (400)
- WHEN vigência fixa não cobre competência THEN linha **não** injetada no processamento
- WHEN `decimoTerceiro=true` THEN todos os caminhos ACL SHALL filtrar/agregar separadamente da folha regular (DT13 existente)
- WHEN rubrica com operador 0 em totalizador THEN linha **não** aparece na aba correspondente
- WHEN funcionário sem linhas no escopo THEN **não** aparece nos cards scoped (não confundir com competência zerada no resumo)
- WHEN arredondamento rateio encargos THEN última parcela ajusta centavos para fechar total
- WHEN reimportação substituir competência THEN fichas e totais scoped recalculados refletem novos dados
- WHEN API indisponível / 403 THEN FE SHALL mensagem clara; não fallback para cálculo local

---

## Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --- | --- |
| Auth boundaries | Todos endpoints folha/resumo/dashboard passam `login` → `OrganogramaAcessoPort`; dual-path obrigatório |
| Failure / partial-failure | Import parcial existente; ficha recalculada atomicamente por competência em `@Transactional` |
| Concurrency / ordering | Reimportação serializada por competência (lock ou delete+insert idempotente) |
| Observability | Log domínio `folha` ao recalcular scoped vs global |
| Input validation | Operadores ∈ {−1,0,1}; totalizer enum validado |
| External deps | ADP import inalterado na transição (ARCH-1) |
| Data lifecycle | `ficha_mensal` replace por competência no processamento; `funcionario_rubrica_fixa` vigência independente |
| Idempotency | Reprocessar mesma competência substitui fichas e re-injeta custos fixos/calculados |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FCLT-01 | P1: Operadores migração | T1 | Done |
| FCLT-02 | P1: Operadores API | T2 | Done |
| FCLT-03 | P1: Operadores UI | T21 | Tasks |
| FCLT-04 | P1: Ficha pós-import | T3, T4, T6 | Done |
| FCLT-05 | P1: Motor fórmulas | T5, T6, T7 | Done |
| FCLT-06 | P1: Custo empresa global | T5, T12, T13 | Tasks |
| FCLT-07 | P1: Snapshot operadores linha | T3, T4, T6 | Done |
| FCLT-08 | P1: Testes motor | T5, T7 | Done |
| FCLT-INT-01 | P1: Benefícios pós-import folha | T8, T13 | Tasks |
| FCLT-09 | P1: FE resumo colunas | T22 | Tasks |
| FCLT-10 | P1: FE cards | T23 | Tasks |
| FCLT-11 | P1: FE abas | T24 | Tasks |
| FCLT-12 | P1: FE dinheiro string | T22, T23 | Tasks |
| FCLT-13 | P2: Rateio encargos | T9 | Tasks |
| FCLT-14 | P2: Rateio só total | T9 | Tasks |
| FCLT-15 | P2: Regime CLT | T17 | Tasks |
| FCLT-16 | P2: Férias 2,5 | T20 | Tasks |
| FCLT-17 | P3: Rubricas calc. avançadas | — | Deferred |
| FCLT-18 | P2: Schema custo fixo | T17 | Tasks |
| FCLT-19 | P2: API custo fixo CRUD | T18 | Tasks |
| FCLT-20 | P2: Conflito vigência | T18 | Tasks |
| FCLT-21 | P2: UI Rubricas Fixas | T25 | Tasks |
| FCLT-22 | P2: Injeção processamento | T19 | Tasks |
| FCLT-23 | P2: Dedup ADP vs fixo | T19 | Tasks |
| FCLT-24 | P2: Stale até reprocess | T19 | Tasks |
| FCLT-25 | P2: Férias operadores | T20 | Tasks |
| FCLT-INT-02 | P2: ACL custo fixo | T19 | Tasks |
| FCLT-ACL-01 | P1: Resumo scoped 3 totais | T11, T12 | Tasks |
| FCLT-ACL-02 | P1: Resumo scoped encargos 0 | T11, T12 | Tasks |
| FCLT-ACL-03 | P1: Resumo global | T12 | Tasks |
| FCLT-ACL-04 | P1: Resumo zeros A2 | T12 | Tasks |
| FCLT-ACL-05 | P1: Deny | T12 | Tasks |
| FCLT-ACL-06 | P1: Teste discrimination | T12 | Tasks |
| FCLT-ACL-07 | P1: API totais campos | T13 | Tasks |
| FCLT-ACL-08 | P1: Totais scoped | T10, T13 | Tasks |
| FCLT-ACL-09 | P1: Totais + rateio total | T9, T13 | Tasks |
| FCLT-ACL-10 | P1: FE sem agregação local | T23 | Tasks |
| FCLT-ACL-11 | P1: Paridade resumo↔cards | T23 | Tasks |
| FCLT-ACL-12 | P1: API linhas totalizer | T14 | Tasks |
| FCLT-ACL-13 | P1: Detalhe deny out-of-scope | T14 | Tasks |
| FCLT-ACL-14 | P1: FE abas a11y | T24 | Tasks |
| FCLT-ACL-15 | P1: Aba custo scoped | T14, T24 | Tasks |
| FCLT-ACL-16 | P1: Dashboard scoped custo | T15, T26 | Tasks |
| FCLT-ACL-17 | P1: Dashboard global custo | T15 | Tasks |
| FCLT-ACL-18 | P1: Evolução custo scoped | T15, T16 | Tasks |

**Coverage:** 41 requirements mapped (FCLT-17 P3 deferred) → 27 tasks T1–T27

---

## Success Criteria

- [ ] Gestor scoped: bruto/líquido/custo no resumo = soma dos cards na mesma competência; encargos = R$ 0,00
- [ ] `acessoTotal`: custo empresa inclui encargos rateados; dashboard ≠ líquido
- [ ] Detalhe por aba distingue origens `FOLHA_ADP`, `CUSTO_FIXO`, `CALCULADO` e `BENEFICIO` sem misturar totalizadores
- [ ] Custos fixos cadastrados aparecem na ficha após processamento; alteração exige reprocess (não consulta ad hoc)
- [ ] Benefícios mensais entram só em `custoEmpresa` na consulta, sem `ficha_linha`
- [ ] Testes FCLT-ACL-06 matam regressão “scoped lê snapshot global”
- [ ] Nenhum cálculo de totalizador no frontend
