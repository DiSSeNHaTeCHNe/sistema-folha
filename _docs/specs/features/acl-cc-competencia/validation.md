# Validation — acl-cc-competencia

## Status atual

| Campo | Valor |
| ----- | ----- |
| **Verdict** | PASS ✅ |
| **Spec slug** | `acl-cc-competencia` |
| **HEAD** | `7e0421da6f3803df46814d255ab00d3e5da59550` |
| **Commit range** | `12804f9..7e0421d` (round 3, T12) |
| **Open gaps** | none |
| **Gate** | 305 passed, 0 failed (`mvn test`) |
| **ModularArchitectureTest** | 18 passed, 0 failed |
| **Sensor** | 1 injected (drill-down `podeAcessarFicha`), 4 killed, 0 survived |

---

## acl-cc-competencia — 2026-07-29 — 520fad4..f72297f

**Overall:** FAIL ❌ — gate e sensor verdes; lacunas de rastreio spec-anchored em 3/17 ACs.

### Gate

| Command | Result |
| ------- | ------ |
| `cd backend && mvn test` | **293 passed**, 0 failed, 0 skipped |
| `cd backend && mvn test -Dtest=ModularArchitectureTest` | **18 passed**, 0 failed |

> Nota: primeira execução no sandbox falhou (MockMaker); reexecução com permissões completas passou.

### Discrimination sensor

Fault injetado: reverter `aplicarFiltroAcesso` / filtro in-memory para usar **somente** `funcionario.getCentroCusto()` (ignorar CC da linha).

| # | Mutation target | Test killed by | Result |
| - | --------------- | -------------- | ------ |
| 1 | `FolhaPagamentoService.aplicarFiltroAcesso` | `FolhaPagamentoServiceTest:279` `assertEquals(1, gestorA.size())` | **KILLED** |
| 2 | `FolhaConsultaAdapter.pertenceAosCentros` | `FolhaConsultaAdapterTest:115` `assertEquals(1, gestorA.size())` | **KILLED** |
| 3 | `BeneficioMensalService.aplicarFiltroAcesso(BeneficioMensal)` | `BeneficioMensalServiceTest:428` `assertTrue(...removerSeAutorizado...)` | **KILLED** |
| 4 | `BeneficioConsultaAdapter` stream filter | `BeneficioConsultaAdapterTest:121` `assertEquals(1L, gestorA)` | **KILLED** |

Mutations descartadas; working tree limpo após restore.

### Spec-anchored AC evidence

#### P1 — ACL folha

| AC | Spec outcome | Evidence (file:line → assertion) | Status |
| -- | ------------ | -------------------------------- | ------ |
| **FCC-01** | Scoped user vê só linhas com CC efetivo ∈ escopo | `FolhaPagamentoServiceTest:208-209` → `assertEquals(1, result.size())` filtra CC 10 vs 20; `FolhaConsultaAdapterTest:137-138` → `assertEquals(1, result.size())` + `assertEquals(100L, centroCustoId())`; `CentroCustoEfetivoTest:32` → `assertTrue(pertenceAoEscopo(10L, Set.of(10L,20L)))` | ✅ |
| **FCC-02** | CC-A jan visível, fev oculto para gestor CC-A | Mecanismo parcial via `FolhaPagamentoServiceTest:277-279` (gestor CC-A vê linha CC=100) — **sem fixture jan+fev** | ⚠️ GAP |
| **FCC-03** | CC-B vê fev, não jan (inverso) | `FolhaPagamentoServiceTest:285` → `assertTrue(gestorB.isEmpty())` cobre gestor CC-B **não** vê linha CC-A; **sem linha CC-B visível para gestor B** | ⚠️ GAP |
| **FCC-04** | `acessoTotal=true` retorna todas linhas ativas | `FolhaPagamentoServiceTest:88-89` → `assertEquals(1, result.size())`; `:142-143` consulta por funcionário | ✅ |
| **FCC-05** | Linha sem CC → fallback `funcionario.centro_custo_id` | `CentroCustoEfetivoTest:21-22` → `assertEquals(200L, idOf(null, 200L))`; `:42` → `assertFalse(pertenceAoEscopo(null, ...))` nega scoped sem CC | ✅ |
| **FCC-06** | Testes discriminatórios folha matam mutação CC-atual | `FolhaPagamentoServiceTest:279,285`; `FolhaConsultaAdapterTest:115,120`; sensor #1–2 killed | ✅ |

#### P1 — Resumo / dashboard

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-07** | Resumo scoped agrega só linhas CC efetivo ∈ escopo | `ResumoFolhaPagamentoServiceTest:134-137` → totais 7300/8000 scoped ≠ snapshot global; `:433-434` verify port com `Set.of(CENTRO_A)` | ✅ |
| **FCC-08** | Dashboard usa CC efetivo via adapter | `DashboardServiceTest:172-174` → evolução scoped `5200.00` ≠ global `99999.00`; `:177-178` verify `findLinhasAtivasPorCompetencia(..., eq(centros))` | ✅ |
| **FCC-09** | Snapshot global existe, zero linhas no escopo → zeros | `ResumoFolhaPagamentoServiceTest:264-267` → `assertEquals(0, dto.totalEmpregados())` + zeros em totais; `:299-300` consulta por competência | ✅ |

#### P2 — Endpoint CC

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-10** | `GET /folha-pagamento/centro-custo/{id}` filtra `folha.centro_custo_id` | `FolhaPagamentoServiceTest:325-330` → retorna linha CC=100 com func CC=200; verify `findByCentroCusto...` never `findByFuncionarioCentroCusto...` | ✅ |
| **FCC-11** | Sem permissão CC → lista vazia | `FolhaPagamentoServiceTest:296` → `assertTrue(result.isEmpty())`; verify repo never called | ✅ |

#### P2 — Exibição folha

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-12** | Snapshot expõe CC da linha (não só funcionário) | `FolhaConsultaAdapterTest:115-116` → `assertEquals(100L, gestorA.get(0).centroCustoId())` com linha=100 func=200 | ✅ |

#### P1 — ACL benefícios

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-13** | Create persiste `centro_custo_id` snapshot | `BeneficioMensalServiceTest:404-405` → `verify save argThat(bm.getCentroCusto().getId().equals(10L))` | ✅ |
| **FCC-14** | Listagem scoped usa COALESCE linha→funcionário | Repo JPQL `BeneficioMensalRepository:26` COALESCE; teste `BeneficioMensalServiceTest:156-159` verifica query scoped — **sem cenário snapshot≠atual na listagem** | ⚠️ GAP |
| **FCC-15** | ACL pós-transferência usa snapshot | `BeneficioMensalServiceTest:428,432` → gestor CC snapshot 100 autoriza, CC atual 200 nega; sensor #3 killed | ✅ |
| **FCC-16** | Testes discriminatórios benefícios | `BeneficioConsultaAdapterTest:121,125`; sensor #4 killed | ✅ |

#### P2 — Exibição benefícios

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-17** | DTO reflete CC da linha com fallback | `BeneficioMensalServiceTest:460-461` → `assertEquals(100L, centroCustoId())` + `"CC Alpha"` com snapshot 100 / func 200 | ✅ |

### Summary

| Check | Result |
| ----- | ------ |
| Spec-anchored | **14/17 matched**, 3 gaps (FCC-02, FCC-03, FCC-14) |
| Gate | **293 passed**, 0 failed |
| Sensor | **4 injected, 4 killed**, 0 survived |
| ModularArchitectureTest | **18 passed** |

### Ranked gaps (blocking FAIL)

1. **FCC-02 / FCC-03** — Falta teste com **duas competências** (jan CC-A, fev CC-B) provando visibilidade cruzada por mês; FCC-06 cobre mecanismo em competência única apenas.
2. **FCC-14** — `listarPorCompetenciaParaUsuario` scoped verifica chamada ao repositório COALESCE, mas não há assert discriminatório (snapshot CC-A + func CC-B → gestor A vê / gestor B não) equivalente a FCC-16 no service de listagem.

### Recommended follow-up (author, not verifier)

- Adicionar `FolhaPagamentoServiceTest` / `FolhaConsultaAdapterTest` com duas linhas jan/fev e asserts FCC-02/FCC-03.
- Adicionar `BeneficioMensalServiceTest` `listarPorCompetencia_linhaCcDiferenteDoFuncionarioAtual_fcc14` mockando repo ou teste de integração JPQL.

---

## acl-cc-competencia — 2026-07-29 — 520fad4..e65a395 (fix cycle 1 re-run)

**Overall:** PASS ✅ — gate e sensor verdes; 17/17 ACs com evidência spec-anchored; gaps FCC-02, FCC-03, FCC-14 fechados em `e65a395`.

### Gate

| Command | Result |
| ------- | ------ |
| `cd backend && mvn test` | **296 passed**, 0 failed, 0 skipped (+3 vs execução anterior) |
| `cd backend && mvn test -Dtest=ModularArchitectureTest` | **18 passed**, 0 failed (incluído no gate acima) |

### Discrimination sensor

Fault injetado: reverter filtro in-memory para usar **somente** `funcionario.centroCusto` (`CentroCustoEfetivo.idOf(null, funcCcId)`).

| # | Mutation target | Test killed by | Result |
| - | --------------- | -------------- | ------ |
| 1 | `FolhaPagamentoService.aplicarFiltroAcesso` | `FolhaPagamentoServiceTest:293` `assertEquals(1, gestorAJan.size())` | **KILLED** |
| 2 | `FolhaConsultaAdapter.pertenceAosCentros` | `FolhaConsultaAdapterTest:129` `assertEquals(1, gestorAJan.size())` | **KILLED** |
| 3 | `BeneficioMensalService.aplicarFiltroAcesso(BeneficioMensal)` | `BeneficioMensalServiceTest:472` `assertTrue(removerSeAutorizado...)` | **KILLED** |

Mutations descartadas; working tree limpo após restore.

### Gap closure (cycle 1)

| Gap | Fix commit | Evidence |
| --- | ---------- | -------- |
| FCC-02 | `e65a395` | `FolhaPagamentoServiceTest:293-298` gestor CC-A vê jan, não fev; `FolhaConsultaAdapterTest:129-134` paridade adapter |
| FCC-03 | `e65a395` | `FolhaPagamentoServiceTest:304-309` gestor CC-B vê fev, não jan; `FolhaConsultaAdapterTest:138-143` inverso |
| FCC-14 | `e65a395` | `BeneficioMensalServiceTest:343-356` snapshot CC-A (100L) + func CC-B (200L): gestor A vê 1 linha CC=100L, gestor B vazio |

### Spec-anchored AC evidence

#### P1 — ACL folha

| AC | Spec outcome | Evidence (file:line → assertion) | Status |
| -- | ------------ | -------------------------------- | ------ |
| **FCC-01** | Scoped user vê só linhas com CC efetivo ∈ escopo | `FolhaPagamentoServiceTest:208-209`; `FolhaConsultaAdapterTest:176-177`; `CentroCustoEfetivoTest:32` | ✅ |
| **FCC-02** | CC-A jan visível, fev oculto para gestor CC-A | `FolhaPagamentoServiceTest:293-298`; `FolhaConsultaAdapterTest:129-134` fixture jan CC-A / fev CC-B, func atual CC-B | ✅ |
| **FCC-03** | CC-B vê fev, não jan (inverso) | `FolhaPagamentoServiceTest:304-309`; `FolhaConsultaAdapterTest:138-143` | ✅ |
| **FCC-04** | `acessoTotal=true` retorna todas linhas ativas | `FolhaPagamentoServiceTest:88-89`; `:142-143` | ✅ |
| **FCC-05** | Linha sem CC → fallback `funcionario.centro_custo_id` | `CentroCustoEfetivoTest:21-22`; `:42` | ✅ |
| **FCC-06** | Testes discriminatórios folha matam mutação CC-atual | `FolhaPagamentoServiceTest:341,347`; `FolhaConsultaAdapterTest:176,181`; sensor #1–2 killed | ✅ |

#### P1 — Resumo / dashboard

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-07** | Resumo scoped agrega só linhas CC efetivo ∈ escopo | `ResumoFolhaPagamentoServiceTest:134-137`; `:433-434` | ✅ |
| **FCC-08** | Dashboard usa CC efetivo via adapter | `DashboardServiceTest:172-174`; `:177-178` | ✅ |
| **FCC-09** | Snapshot global existe, zero linhas no escopo → zeros | `ResumoFolhaPagamentoServiceTest:264-267`; `:299-300` | ✅ |

#### P2 — Endpoint CC

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-10** | `GET /folha-pagamento/centro-custo/{id}` filtra `folha.centro_custo_id` | `FolhaPagamentoServiceTest:387-392` | ✅ |
| **FCC-11** | Sem permissão CC → lista vazia | `FolhaPagamentoServiceTest:358` | ✅ |

#### P2 — Exibição folha

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-12** | Snapshot expõe CC da linha (não só funcionário) | `FolhaConsultaAdapterTest:176-177` linha=100 func=200 | ✅ |

#### P1 — ACL benefícios

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-13** | Create persiste `centro_custo_id` snapshot | `BeneficioMensalServiceTest:448-449` | ✅ |
| **FCC-14** | Listagem scoped usa COALESCE linha→funcionário | `BeneficioMensalRepository:26` COALESCE JPQL; `BeneficioMensalServiceTest:343-356` cenário snapshot≠atual | ✅ |
| **FCC-15** | ACL pós-transferência usa snapshot | `BeneficioMensalServiceTest:472,476`; sensor #3 killed | ✅ |
| **FCC-16** | Testes discriminatórios benefícios | `BeneficioConsultaAdapterTest:121,125` | ✅ |

#### P2 — Exibição benefícios

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-17** | DTO reflete CC da linha com fallback | `BeneficioMensalServiceTest:504-505` | ✅ |

### Summary

| Check | Result |
| ----- | ------ |
| Spec-anchored | **17/17 matched**, 0 gaps |
| Gate | **296 passed**, 0 failed |
| Sensor | **3 injected, 3 killed**, 0 survived |
| ModularArchitectureTest | **18 passed** |
| Cycle-1 gaps | **FCC-02, FCC-03, FCC-14 closed** |

### Ranked gaps

_none — feature ready for merge from verification perspective._

---

## acl-cc-competencia — 2026-07-29 — e65a395..12804f9 (round 2, T8–T11)

**Overall:** PASS ✅ — gate e sensor verdes; 25/25 ACs com evidência spec-anchored; round 2 (ficha path + hygiene/perf) fechado.

### Gate

| Command | Result |
| ------- | ------ |
| `cd backend && mvn test` | **301 passed**, 0 failed, 0 skipped (+5 vs cycle 1) |
| `cd backend && mvn test -Dtest=ModularArchitectureTest` | **18 passed**, 0 failed (incluído no gate acima) |

> Nota: execução com permissões completas (sandbox MockMaker falha).

### Discrimination sensor (ficha path)

Fault injetado: reverter `FolhaConsultaAdapter.linhasDeFicha` para fetch unscoped + filtro in-memory **somente** `funcionario.centroCusto` (ignorar CC snapshot da ficha).

| # | Mutation target | Test killed by | Result |
| - | --------------- | -------------- | ------ |
| 1 | `FolhaConsultaAdapter.linhasDeFicha` | `FolhaConsultaAdapterTest:355` `assertEquals(1, gestorA.size())` | **KILLED** |

Mutations descartadas; working tree limpo após restore.

### Round 2 delivery (T8–T11)

| Task | Commit | Scope |
| ---- | ------ | ----- |
| T8 | `0573799` | V1.26 migration + `FichaMensal.centroCusto` + snapshot em `montarFicha` |
| T9 | `3cef3ec` | `FichaLinhaRepository` COALESCE scoped query + adapter ficha path + FCC-22 |
| T10 | `96811e9` | `countByCompetenciaECentros` SQL + adapter refactor FCC-23 |
| T11 | `12804f9` | Rename `CentroCustoEfetivoIdIn` + remove orphan folha repo method FCC-24/25 |

### Spec-anchored AC evidence

#### P1 — ACL folha (cycle 1, re-verified)

| AC | Spec outcome | Evidence (file:line → assertion) | Status |
| -- | ------------ | -------------------------------- | ------ |
| **FCC-01** | Scoped user vê só linhas com CC efetivo ∈ escopo | `FolhaPagamentoServiceTest:208-209`; `FolhaConsultaAdapterTest:176-177`; `CentroCustoEfetivoTest:32` | ✅ |
| **FCC-02** | CC-A jan visível, fev oculto para gestor CC-A | `FolhaPagamentoServiceTest:293-298`; `FolhaConsultaAdapterTest:129-134` | ✅ |
| **FCC-03** | CC-B vê fev, não jan (inverso) | `FolhaPagamentoServiceTest:304-309`; `FolhaConsultaAdapterTest:138-143` | ✅ |
| **FCC-04** | `acessoTotal=true` retorna todas linhas ativas | `FolhaPagamentoServiceTest:88-89`; `:142-143` | ✅ |
| **FCC-05** | Linha sem CC → fallback `funcionario.centro_custo_id` | `CentroCustoEfetivoTest:21-22`; `:42` | ✅ |
| **FCC-06** | Testes discriminatórios folha matam mutação CC-atual | `FolhaPagamentoServiceTest:341,347`; `FolhaConsultaAdapterTest:176,181` | ✅ |

#### P1 — Resumo / dashboard (cycle 1, re-verified)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-07** | Resumo scoped agrega só linhas CC efetivo ∈ escopo | `ResumoFolhaPagamentoServiceTest:134-137`; `:433-434` | ✅ |
| **FCC-08** | Dashboard usa CC efetivo via adapter | `DashboardServiceTest:172-174`; `:177-178` | ✅ |
| **FCC-09** | Snapshot global existe, zero linhas no escopo → zeros | `ResumoFolhaPagamentoServiceTest:264-267`; `:299-300` | ✅ |

#### P2 — Endpoint CC (cycle 1, re-verified)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-10** | `GET /folha-pagamento/centro-custo/{id}` filtra `folha.centro_custo_id` | `FolhaPagamentoServiceTest:387-392` | ✅ |
| **FCC-11** | Sem permissão CC → lista vazia | `FolhaPagamentoServiceTest:358` | ✅ |

#### P2 — Exibição folha (cycle 1, re-verified)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-12** | Snapshot expõe CC da linha (não só funcionário) | `FolhaConsultaAdapterTest:176-177` | ✅ |

#### P1 — ACL benefícios (cycle 1, re-verified)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-13** | Create persiste `centro_custo_id` snapshot | `BeneficioMensalServiceTest:448-449` | ✅ |
| **FCC-14** | Listagem scoped usa COALESCE linha→funcionário | `BeneficioMensalRepository:38` COALESCE JPQL; `BeneficioMensalServiceTest:343-356` | ✅ |
| **FCC-15** | ACL pós-transferência usa snapshot | `BeneficioMensalServiceTest:472,476` | ✅ |
| **FCC-16** | Testes discriminatórios benefícios | `BeneficioConsultaAdapterTest:115,119` | ✅ |

#### P2 — Exibição benefícios (cycle 1, re-verified)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-17** | DTO reflete CC da linha com fallback | `BeneficioMensalServiceTest:504-505` | ✅ |

#### P1 — ACL ficha processada (round 2, NEW)

| AC | Spec outcome | Evidence (file:line → assertion) | Status |
| -- | ------------ | -------------------------------- | ------ |
| **FCC-18** | Processamento persiste `ficha_mensal.centro_custo_id` = CC efetivo do grupo | `FolhaProcessamentoServiceTest:558-559` linha CC=100 func=200 → ficha CC=100L; `:635-636` fallback linha null → func CC=200L | ✅ |
| **FCC-19** | Path ficha scoped usa COALESCE(ficha, funcionário) | `FichaLinhaRepository:50` JPQL COALESCE; `FolhaConsultaAdapter:60-61` delega scoped query; `FolhaConsultaAdapterTest:346-351` verify repo call | ✅ |
| **FCC-20** | `toLinhaSnapshotFromFicha` expõe CC da ficha; fallback funcionário se null | `FolhaConsultaAdapter:173-175`; `FolhaConsultaAdapterTest:356` snapshot CC=100; `:305` fallback ficha null → func CC=100L | ✅ |
| **FCC-21** | Reprocessamento atualiza CC snapshot da ficha | `FolhaProcessamentoServiceTest:594,602` primeiro CC=100L, reprocesso CC=200L | ✅ |
| **FCC-22** | Teste discriminatório path ficha mata mutação CC-atual | `FolhaConsultaAdapterTest:355-360` gestor A vê CC snapshot, gestor B vazio; sensor #1 killed | ✅ |

#### P2 — Hygiene e performance (round 2, NEW)

| AC | Spec outcome | Evidence | Status |
| -- | ------------ | -------- | ------ |
| **FCC-23** | COUNT scoped benefício usa SQL COALESCE (não full fetch) | `BeneficioMensalRepository:26` COALESCE COUNT; `BeneficioConsultaAdapterTest:139-142` verify `countByCompetenciaECentros`, never unscoped fetch | ✅ |
| **FCC-24** | Método repo benefício reflete CC efetivo | `BeneficioMensalRepository:40` `findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue`; `BeneficioMensalServiceTest:337` verify | ✅ |
| **FCC-25** | Orphan `findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue` removido | `FolhaPagamentoRepository` — método ausente; grep codebase produção zero callers | ✅ |

### Summary

| Check | Result |
| ----- | ------ |
| Spec-anchored | **25/25 matched**, 0 gaps |
| Gate | **301 passed**, 0 failed |
| Sensor (ficha) | **1 injected, 1 killed**, 0 survived |
| ModularArchitectureTest | **18 passed** |
| Round 2 scope | **FCC-18…25 closed** |

### Ranked gaps

_none — feature ready for merge from verification perspective._

---

## acl-cc-competencia — 2026-07-29 — 12804f9..7e0421d (round 3, T12)

**Overall:** PASS ✅ — gate e sensor verdes; 28/28 ACs com evidência spec-anchored; round 3 (ficha drill-down ACL) fechado.

### Gate

| Command | Result |
| ------- | ------ |
| `cd backend && mvn test` | **305 passed**, 0 failed, 0 skipped (+4 vs round 2) |
| `cd backend && mvn test -Dtest=ModularArchitectureTest` | **18 passed**, 0 failed (incluído no gate acima) |

> Nota: execução com permissões completas (sandbox MockMaker falha).

### Discrimination sensor (drill-down ficha)

Fault injetado: reverter `FolhaFichaConsultaService.podeAcessarFicha` para usar **somente** `funcionario.centroCusto` (ignorar CC snapshot da ficha).

| # | Mutation target | Test killed by | Result |
| - | --------------- | -------------- | ------ |
| 1 | `FolhaFichaConsultaService.podeAcessarFicha` | `FolhaFichaConsultaServiceTest:306` `listarLinhasPorTotalizador_snapshotCentroA_funcionarioCentroB_gestorA_acessa` | **KILLED** |
| 2 | `FolhaFichaConsultaService.podeAcessarFicha` | `FolhaFichaConsultaServiceTest:319` `listarLinhasPorTotalizador_snapshotCentroA_funcionarioCentroB_gestorB_retorna404` | **KILLED** |
| 3 | `FolhaFichaConsultaService.podeAcessarFicha` | `FolhaFichaConsultaServiceTest:334` `buscarFichaIdPorFuncionario_snapshotCentroA_funcionarioCentroB_gestorA_retornaId` | **KILLED** |
| 4 | `FolhaFichaConsultaService.podeAcessarFicha` | `FolhaFichaConsultaServiceTest:349` `buscarFichaIdPorFuncionario_snapshotCentroA_funcionarioCentroB_gestorB_retorna404` | **KILLED** |

Mutations descartadas; working tree limpo após restore.

### Round 3 delivery (T12)

| Task | Commit | Scope |
| ---- | ------ | ----- |
| T12 | `7e0421d` | `podeAcessarFicha` CC efetivo + repo fetch snapshot + FCC-26…28 |

### Spec-anchored AC evidence

#### P1 — ACL ficha drill-down (round 3, NEW)

| AC | Spec outcome | Evidence (file:line → assertion) | Status |
| -- | ------------ | -------------------------------- | ------ |
| **FCC-26** | Drill-down ACL usa CC efetivo `COALESCE(ficha, funcionário)` ∈ escopo | `FolhaFichaConsultaService:124-128` → `CentroCustoEfetivo.idOf(linhaCcId, funcCcId)` + `pertenceAoEscopo`; `FichaMensalRepository:21,30` LEFT JOIN FETCH `f.centroCusto` | ✅ |
| **FCC-27** | Snapshot CC-A + func CC-B → gestor A acessa, gestor B 404 | `FolhaFichaConsultaServiceTest:309` gestor A `assertEquals(1, result.size())`; `:319` gestor B `assertThrows(FichaMensalNotFoundException)`; `:337` buscar gestor A `assertEquals(FICHA_ID)`; `:349` buscar gestor B `assertThrows` | ✅ |
| **FCC-28** | Testes discriminatórios FCC-27 matam mutação CC-atual | `FolhaFichaConsultaServiceTest:297-351` 4 cenários snapshot≠func; sensor #1–4 killed | ✅ |

#### Cycle 1 + round 2 ACs (re-verified, unchanged)

FCC-01…FCC-25 — evidência mantida em rounds anteriores; nenhuma regressão no gate (+4 testes novos, 0 falhas).

### Summary

| Check | Result |
| ----- | ------ |
| Spec-anchored | **28/28 matched**, 0 gaps |
| Gate | **305 passed**, 0 failed |
| Sensor (drill-down) | **1 injected, 4 killed**, 0 survived |
| ModularArchitectureTest | **18 passed** |
| Round 3 scope | **FCC-26…28 closed** |

### Ranked gaps

_none — feature ready for merge from verification perspective._
