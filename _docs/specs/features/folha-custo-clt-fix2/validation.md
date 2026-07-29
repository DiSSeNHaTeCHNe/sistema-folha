# folha-custo-clt-fix2 Validation

## Status atual
- **Veredito**: PASS
- **Spec vigente**: folha-custo-clt-fix2/spec.md
- **HEAD**: a31bc15
- **Gaps abertos**: FIX2-10 (sensor comportamental, não mock `EncargosRateioService`); FIX2-19 (P2 FE sem teste automatizado, AD-004); FIX2-18 (reprocesso UI cadastro Rubricas não testada)

---

## Execução: folha-custo-clt-fix2 — 2026-07-29 — 70364a8..06849c9

### Veredito: PASS

P1 (FIX2-01…FIX2-16, FIX2-22…FIX2-24) com evidência em testes unitários + gate completo verde. P2 (FIX2-17…FIX2-19) com evidência de migração/código/build; FE sem teste Vitest (gap conhecido AD-004).

### Evidência por AC

| AC | Evidência (file:line) | Assertion | Outcome spec |
| --- | --- | --- | --- |
| FIX2-01 | `FolhaMotorCalculo.java:80-85`; `FolhaMotorCalculoTest.java:102-110,125-127` | `porcentagemNull_trataComo100NoCusto`: custo=688; `porcentagemEfetiva(null)`=100 | `valor × op_custo × (%/100)`, default 100 |
| FIX2-02 | `FolhaMotorCalculo.java:55-56`; `FolhaMotorCalculoTest.java:77-86` | bruto=liquido=7258.43 com %138.63 | Bruto/líquido sem `%` |
| FIX2-03 | `FolhaMotorCalculoTest.java:64-73,114-119` | custo/contribuição=10062.36 | 7258.43×138.63%=10062.36 HALF_UP |
| FIX2-04 | `V1.22__ficha_linha_porcentagem.sql:1-5`; `FolhaProcessamentoServiceTest.java:278-325`; `FolhaConsultaAdapterTest.java:214-245` | snapshot ADP/CUSTO_FIXO/CALCULADO persiste %; adapter expõe | Snapshot `%` na linha |
| FIX2-05 | `FolhaProcessamentoServiceTest.java:364-392` | `fichaFinal.getCustoFolha()`=10062.36 | `custo_folha` persistido com FIX2-01 |
| FIX2-06 | `FolhaTotalizacaoService.java:60-62`; `FolhaCustoEmpresaComposer.java:10-14` | `encargosRateados=ZERO`; `compor(folha, encargos, benefícios)` | Composição sem encargos rateados |
| FIX2-07 | `FolhaTotalizacaoServiceTest.java:58,114-115,135-136` | `encargosRateados()`=0.00 | DTO deprecated sempre 0 |
| FIX2-08 | `FolhaLinhaAgregacaoTest.java:92-103`; `ResumoFolhaPagamentoServiceTest.java:148-181` | encargos map ignorado; `totalEncargos()`=0 | Resumo sem rateio na composição |
| FIX2-09 | `DashboardService.java:114`; `DashboardServiceTest.java:116-126,158-169` | `custoMensalFolha` via `FolhaTotalizacaoPort` | Dashboard KPI sem rateio |
| FIX2-10 | `FolhaTotalizacaoServiceTest.java:118-138`; sensor M2 (ver abaixo) | encargosRateados=0 com snapshot 1000; mutação rateio → FAIL | Regressão detecta reintrodução de rateio |
| FIX2-11 | `FolhaFichaConsultaServiceTest.java:112-130,180-199` | contribuição=10062.36; CUSTO_FIXO/BENEFICIO listados | API detalhe com `%` no custo |
| FIX2-12 | — | — | **GAP**: nenhum teste soma Σ contribuições aba = `custoEmpresa` card |
| FIX2-13 | `FolhaFichaConsultaServiceTest.java:158-177` | `noneMatch` rubrica ENCARGO | Sem linha encargos rateados |
| FIX2-14 | `FolhaAclParidadeResumoCardsTest.java:94-145` | `totalCustoEmpresa` resumo = Σ cards scoped | Paridade custo scoped |
| FIX2-15 | `FolhaAclParidadeResumoCardsTest.java:188-221` | salBruto/salLiquido=7258.43; custo=10062.36 | Bruto/líquido cards sem `%` |
| FIX2-16 | `FolhaAclParidadeResumoCardsTest.java:147-185`; `ResumoFolhaPagamentoServiceTest.java:148-181` | global Σ cards = resumo; sem rateio | Resumo global sem rateio ADP |
| FIX2-17 | `V1.23__seed_rubrica_porcentagem_legado.sql:1-6` | UPDATE codigo 0010 → 138.63 com comentário | Seed documentado |
| FIX2-18 | `FolhaProcessamentoServiceTest.java:396-432` | reprocesso: custo 7258.43→10062.36 após `%` cadastro | Custo reflete `%` após reprocesso |
| FIX2-19 | `frontend/src/pages/FolhaPagamento/index.tsx:576-581` | subtexto Folha/Benefícios; `encargosRateados` não renderizado | P2 card decomposição (build OK) |
| FIX2-20 | `FolhaProcessamentoServiceTest.java:329-360`; `FolhaMotorCalculoTest.java:102-110` | bruto/custo=10688 com fixa 688 @100% | Fixa 688 no custo; bruto original |
| FIX2-21 | `FolhaFichaConsultaServiceTest.java:134-154` | valor=7258.43; contrib GROSS/NET=7258.43 | Detalhe bruto/líquido valor original |
| FIX2-22 | `FolhaAclParidadeResumoCardsTest.java:132-144,224-237` | `totalBruto` resumo = Σ `salBruto` cards | Paridade resumo ↔ cards bruto |
| FIX2-23 | `FolhaAclParidadeResumoCardsTest.java:135-144,228-236` | `totalLiquido` resumo = Σ `salLiquido` cards | Paridade resumo ↔ cards líquido |
| FIX2-24 | `ResumoFolhaPagamentoServiceTest.java:178-181`; `FolhaAclParidadeResumoCardsTest.java:184` | `totalBruto`≠`totalPagamentos` snapshot quando linhas existem | Não mapeia pagamentos ADP como bruto |

### Sensor de discriminação

| Mutação | Alvo | Testes executados | Resultado |
| --- | --- | --- | --- |
| M1 — ignorar `%` em `contribuicaoCusto` | `FolhaMotorCalculo.java:80-85` | `FolhaMotorCalculoTest#calcularPorLinhas_porcentagem13863_custoAncora1006236`, `FolhaTotalizacaoServiceTest#calcularTotaisPorFuncionario_porcentagemSnapshot_aplicaSomenteNoCusto`, `FolhaFichaConsultaServiceTest#listarLinhasPorTotalizador_companyCost_aplicaPorcentagemNoCusto`, `FolhaAclParidadeResumoCardsTest#scopedCards_brutoLiquidoIgnoramPorcentagem` | **4/4 killed** |
| M2 — reintroduzir rateio proporcional em `FolhaTotalizacaoService` | `FolhaTotalizacaoService.java:60-62` | `FolhaTotalizacaoServiceTest#calcularTotaisPorFuncionario_global_totalEncargosSnapshot_naoCompoeCustoEmpresa`, `FolhaLinhaAgregacaoTest#agregarComBeneficiosEEncargos_encargosIgnoradosNaComposicao`, `FolhaAclParidadeResumoCardsTest` (3 tests) | **2/5 killed** (rateio + paridade global); agregador isolado sobrevive (esperado — rateio removido também em `FolhaLinhaAgregacao`) |
| M3 — bruto/líquido com `%` (não executada; M1 já cobre custo; FIX2-02 coberto por testes dedicados) | — | — | N/A |

Mutações aplicadas em scratch e revertidas via `git checkout` após execução.

### Gaps encontrados

1. **FIX2-12 (Major, P1)**: Falta teste que some contribuições `COMPANY_COST` + benefícios e compare com `custoEmpresa` do card (±0,01). Motor e endpoints usam mesma fórmula, mas AC pede paridade explícita aba↔card.
2. **FIX2-10 (Minor)**: Tasks T5 previam `verify` mock em `EncargosRateioService.ratearPorFuncionario`; serviço foi removido do wiring de composição. Sensor comportamental (`totalEncargosSnapshot`≠0 → `encargosRateados=0`, custo inalterado) + M2 matam reintrodução — equivalente funcional, não literal ao mock draft.
3. **FIX2-19 (Minor, P2)**: Subtexto FE verificado por inspeção + build; sem Vitest/Playwright (AD-004 gap documentado em tasks).
4. **FIX2-18 (Minor, P2)**: Reprocesso backend coberto; persistência UI cadastro Rubricas não testada nesta feature.

### Gate

| Comando | Resultado |
| --- | --- |
| `cd backend && mvn test` | **PASS** — 263 tests, 0 failures, 0 errors, 0 skipped |
| `cd frontend && npm run lint` | **PASS** — 0 errors, 8 warnings pré-existentes (react-hooks/exhaustive-deps) |
| `cd frontend && npm run build` | **PASS** — tsc + vite build OK |

---

## Execução: folha-custo-clt-fix2 fix cycle 1 — 2026-07-29 — 70364a8..a31bc15

### Veredito: PASS

Gaps P1 **FIX2-12** e edge case **totalEncargos informativo** (global com linhas) fechados em `aaf7343` + `a31bc15`. Gate completo verde (264 tests). Sensores M1/M2 matam regressões dos fixes.

### Delta vs execução anterior (06849c9)

| Item | Antes | Depois |
| --- | --- | --- |
| FIX2-12 | GAP — sem teste aba↔card | `FolhaFichaConsultaServiceTest.java:205-249` — Σ contribuições aba Custo+benefícios = `custoEmpresa` card (±0,01), fixture 138,63% + fixa 688 + VR 600 → 11350,36 |
| totalEncargos (global c/ linhas) | `toDtoFromLinhas(..., ZERO)` — encargos zerados indevidamente | `ResumoFolhaPagamentoService.java:108` passa `resumo.getTotalEncargos()` informativo; custoEmpresa inalterado (sem rateio) |
| Testes backend | 263 | **264** (+1 FIX2-12) |

### Evidência delta (ACs afetados)

| AC | Evidência (file:line) | Assertion | Outcome |
| --- | --- | --- | --- |
| FIX2-12 | `FolhaFichaConsultaServiceTest.java:205-249` | `listarLinhasPorTotalizador_companyCost_somaContribuicoes_igualCustoEmpresaCard`: 3 linhas; card=11350,36; `\|card−Σcontrib\|≤0,01` | Paridade aba Custo+benefícios ↔ card |
| FIX2-08/16 (encargos informativo) | `ResumoFolhaPagamentoService.java:107-108`; `ResumoFolhaPagamentoServiceTest.java:174-178` | global c/ linhas: `totalEncargos()`=10000 snapshot; `totalCustoEmpresa()`=8000 (sem rateio); `encargos+custo≠custo` | Encargos ADP exibidos, não compostos no custo |

### Sensor de discriminação (fix cycle 1)

| Mutação | Alvo | Testes executados | Resultado |
| --- | --- | --- | --- |
| M1 — zerar `totalEncargosInformativo` em global c/ linhas | `ResumoFolhaPagamentoService.java:108` → `BigDecimal.ZERO` | `ResumoFolhaPagamentoServiceTest#listarTodos_acessoTotal_comLinhas_agregaSemRateio_totalBrutoOperadorBased` | **1/1 killed** (linha 174: expected encargos 10000, got 0) |
| M2 — ignorar `%` em `contribuicaoCusto` | `FolhaMotorCalculo.java:80-85` | `FolhaFichaConsultaServiceTest#listarLinhasPorTotalizador_companyCost_somaContribuicoes_igualCustoEmpresaCard`, `#companyCost_aplicaPorcentagemNoCusto`; `FolhaMotorCalculoTest#calcularPorLinhas_porcentagem13863_custoAncora1006236` | **3/3 killed** |

Mutações aplicadas em scratch e revertidas via `git checkout` após execução.

### Gaps remanescentes

1. **FIX2-10 (Minor)**: Sensor comportamental + M2 (execução anterior) matam reintrodução de rateio; não há mock literal em `EncargosRateioService.ratearPorFuncionario`.
2. **FIX2-19 (Minor, P2)**: Subtexto FE verificado por inspeção + build; sem Vitest/Playwright (AD-004).
3. **FIX2-18 (Minor, P2)**: Reprocesso backend coberto; persistência UI cadastro Rubricas não testada nesta feature.

### Gate

| Comando | Resultado |
| --- | --- |
| `cd backend && mvn test` | **PASS** — 264 tests, 0 failures, 0 errors, 0 skipped |
| `cd frontend && npm run lint` | **PASS** — 0 errors, 8 warnings pré-existentes (react-hooks/exhaustive-deps) |
| `cd frontend && npm run build` | **PASS** — tsc + vite build OK |
