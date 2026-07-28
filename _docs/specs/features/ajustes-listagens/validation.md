# Ajustes — Listagens, Filtros e UX — Validation

**Date**: 2026-07-28  
**Spec**: `_docs/specs/features/ajustes-listagens/spec.md`  
**Diff range**: working tree (uncommitted feature branch)  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | Sort pós-ACL em `consultarPorFuncionario` + 2 testes de ordenação |
| T2   | ✅ Done | `RubricaStatusFiltro`, repo/service/controller + `RubricaServiceTest` (7 testes) |
| T3   | ✅ Done | `UsuarioRepository.findByFiltros`, service/controller + 6 testes de listagem |
| T4   | ✅ Done | Card Filtros em `Rubricas/index.tsx` + `rubricaService.listar(filtros)` |
| T5   | ✅ Done | Toggles independentes em `AlterarSenhaDialog` |
| T6   | ✅ Done | Smoke Usuários — FE já integrado; zero diff necessário |

---

## Spec-Anchored Acceptance Criteria

### P1: Detalhe Folha ordenado (FOLHA-ORD-01…03)

| Requirement | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ----------- | ------------------------- | -------------------- | ----------------------- | ------ |
| FOLHA-ORD-01 | WHEN abre Ver Rubricas THEN linhas ordenadas por `rubricaCodigo` ↑ lex pt-BR | `100` antes de `200` | `FolhaPagamentoService.java:57-58` — `.sorted(Comparator.comparing(FolhaPagamentoDTO::rubricaCodigo, CASE_INSENSITIVE_ORDER)...)`; `FolhaPagamentoServiceTest.java:217-218` — `assertEquals("100", result.get(0).rubricaCodigo())` | ✅ PASS |
| FOLHA-ORD-02 | WHEN mesmo `rubricaCodigo` THEN desempate por `id` ↑ | id `10` antes de `20` | `FolhaPagamentoService.java:58` — `.thenComparing(FolhaPagamentoDTO::id, ...)`; `FolhaPagamentoServiceTest.java:236-237` — `assertEquals(10L, result.get(0).id())` | ✅ PASS |
| FOLHA-ORD-03 | WHEN sem rubricas THEN tabela vazia sem erro | lista vazia, sem exceção | `FolhaPagamentoService.java:54-59` — stream sobre `linhas` vazio → `collect` → `List.of()`; sem teste dedicado | ⚠️ Spec-precision gap (impl OK, sem assert explícito) |

### P1: Rubricas filtros/ordem (RUB-01…11)

| Requirement | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ----------- | ------------------------- | -------------------- | ----------------------- | ------ |
| RUB-01 | WHEN abre `/rubricas` THEN card **Filtros** padrão Funcionários/Usuários | Card + título + campos + Filtrar/Limpar | `Rubricas/index.tsx:168-222` — `<Card>`, `<Typography variant="h6">Filtros</Typography>`, `flexWrap`, botões Filtrar/Limpar | ✅ PASS |
| RUB-02 | WHEN carga sem filtros THEN ordenadas por `codigo` ↑ | ORDER BY codigo ASC | `RubricaRepository.java:29` — `ORDER BY r.codigo ASC`; `RubricaServiceTest.java:41` — `verify(...findByFiltros(isNull(), isNull(), eq(true)))` | ⚠️ Spec-precision gap (ORDER BY no repo; sem assert de ordem no teste) |
| RUB-03 | WHEN filtro Id/Código + Filtrar THEN `codigo` contém (CI) | pattern `%valor%` ILIKE | `RubricaService.java:31-32`; `RubricaServiceTest.java:69-71` — `verify(...findByFiltros(eq("%ABC%"), ...))` | ✅ PASS |
| RUB-04 | WHEN filtro Descrição + Filtrar THEN `descricao` contém (CI) | pattern `%valor%` ILIKE | `RubricaService.java:36-37`; `RubricaServiceTest.java:79-81` — `verify(...findByFiltros(isNull(), eq("%foo%"), ...))` | ✅ PASS |
| RUB-05 | WHEN status Ativo THEN só `ativo=true` | `ativo=true` na query | `RubricaService.java:48-49`; `RubricaServiceTest.java:41` — `verify(...eq(true))` | ✅ PASS |
| RUB-06 | WHEN status Inativo THEN só `ativo=false` | `ativo=false` na query | `RubricaService.java:51-52`; `RubricaServiceTest.java:51` — `verify(...eq(false))` | ✅ PASS |
| RUB-07 | WHEN status Todos THEN ativas e inativas | `ativo=null` na query | `RubricaService.java:54`; `RubricaServiceTest.java:61` — `verify(...isNull())` | ✅ PASS |
| RUB-08 | WHEN status omitido na carga THEN padrão Ativo | default ATIVO | `RubricaController.java:29` — `defaultValue = "ATIVO"`; `Rubricas/index.tsx:41-45,70` — `DEFAULT_FILTROS.status: 'ATIVO'`; `rubricaService.ts:42` — `params.append('status', filtros?.status ?? 'ATIVO')` | ✅ PASS |
| RUB-09 | WHEN Limpar THEN reset Ativo + vazios + recarga | defaults restaurados | `Rubricas/index.tsx:96-98` — `resetFilter(DEFAULT_FILTROS); carregarRubricas(DEFAULT_FILTROS)` | ✅ PASS |
| RUB-10 | WHEN filtros sem resultados THEN **"Nenhuma rubrica encontrada"** | mensagem exata | `Rubricas/index.tsx:239-243` — `{rubricas.length === 0 ? ... "Nenhuma rubrica encontrada"}` | ✅ PASS |
| RUB-11 | WHEN filtros aplicados THEN `GET /rubricas?codigo&descricao&status` | params opcionais ATIVO\|INATIVO\|TODOS | `RubricaController.java:25-30`; `rubricaService.ts:37-44` — `URLSearchParams` com codigo/descricao/status | ✅ PASS |

### P1: Usuários ordem/filtros (USR-01…08)

| Requirement | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ----------- | ------------------------- | -------------------- | ----------------------- | ------ |
| USR-01 | WHEN abre/aplica filtros THEN ordem `nome` ↑, desempate `login` ↑ | ORDER BY nome, login ASC | `UsuarioRepository.java:26` — `ORDER BY u.nome ASC, u.login ASC`; `UsuarioServiceTest.java:54` — `verify(usuarioRepository).findByFiltros(...)` | ⚠️ Spec-precision gap (ORDER BY no repo; sem assert de ordem) |
| USR-02 | WHEN filtro Nome + Filtrar THEN `nome` contém (CI), só ativos | `%valor%` ILIKE + `u.ativo=true` | `UsuarioService.java:38-40`; `UsuarioRepository.java:22`; `UsuarioServiceTest.java:62-64` — `verify(...eq("%Maria%"), ...)` | ✅ PASS |
| USR-03 | WHEN filtro Login + Filtrar THEN `login` contém (CI) | `%valor%` ILIKE | `UsuarioService.java:43-45`; `UsuarioServiceTest.java:72-74` — `verify(...eq("%adm%"), ...)` | ✅ PASS |
| USR-04 | WHEN filtro Funcionário + Filtrar THEN match exato `funcionarioId` | `f.id = :funcionarioId` | `UsuarioRepository.java:25`; `UsuarioServiceTest.java:82-84` — `verify(...eq(FUNCIONARIO_ID))` | ✅ PASS |
| USR-05 | WHEN filtros combinados THEN AND entre critérios | todos patterns aplicados | `UsuarioRepository.java:23-25`; `UsuarioServiceTest.java:102-104` — `verify(...eq("%Maria%"), eq("%adm%"), eq(FUNCIONARIO_ID))` | ✅ PASS |
| USR-06 | WHEN Limpar THEN reset + recarga todos ativos | filtros vazios + `carregarDados()` | `Usuarios/index.tsx:212-214` — `resetFilter(); carregarDados()` | ✅ PASS |
| USR-07 | WHEN filtros sem resultados THEN **"Nenhum usuário encontrado"** | mensagem exata | `Usuarios/index.tsx:362-366` — `usuarios.length === 0 ? ... "Nenhum usuário encontrado"` | ✅ PASS |
| USR-08 | WHEN `GET /usuarios` THEN aceita `nome`, `login`, `funcionarioId` | params opcionais | `UsuarioController.java:24-28`; `usuarioService.ts:20-26` — `URLSearchParams` append nome/login/funcionarioId | ✅ PASS |

### P1: Dialog Alterar senha (SENHA-VIS-01…06)

| Requirement | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ----------- | ------------------------- | -------------------- | ----------------------- | ------ |
| SENHA-VIS-01 | WHEN abre Alterar senha THEN olho em Nova + Confirmar (padrão Usuarios) | `Visibility`/`VisibilityOff` + `InputAdornment` | `AlterarSenhaDialog/index.tsx:145-156,177-188`; ref. `Usuarios/index.tsx:534-547` | ✅ PASS |
| SENHA-VIS-02 | WHEN clica olho Nova senha THEN alterna password/text; inicial oculto | `showNovaSenha` default false | `AlterarSenhaDialog/index.tsx:35,139,149` — `type={showNovaSenha ? 'text' : 'password'}` | ✅ PASS |
| SENHA-VIS-03 | WHEN clica olho Confirmar THEN independente; inicial oculto | state separado | `AlterarSenhaDialog/index.tsx:36,171,181` — `showConfirmarSenha` independente de `showNovaSenha` | ✅ PASS |
| SENHA-VIS-04 | WHEN dialog exibido THEN Senha atual sempre oculta, sem ícone | `type="password"`, sem adornment | `AlterarSenhaDialog/index.tsx:116-125` — `type="password"`, sem `InputAdornment` | ✅ PASS |
| SENHA-VIS-05 | WHEN fecha/reabre THEN toggles resetam oculto | ambos false | `AlterarSenhaDialog/index.tsx:54-60` — `useEffect(open)` set false; `63-68` — `handleClose` set false | ✅ PASS |
| SENHA-VIS-06 | WHEN submete THEN validação/API inalteradas | min 6 chars, confirmação, senha atual | `AlterarSenhaDialog/index.tsx:71-85` — length check + `usuarioService.alterarSenha`; rules `114-165` | ✅ PASS |

**Status**: ✅ 25/28 ACs with full evidence — 3 spec-precision gaps (ordering/empty-list not asserted in unit tests; implementation present)

---

## Edge Cases

| Edge case | Evidence | Result |
| --------- | -------- | ------ |
| Filtro string só espaços → ignorar | `RubricaService.java:31,36`; `UsuarioService.java:39,44`; tests trim | ✅ |
| Funcionário "Todos" (vazio) → não filtrar | `usuarioService.ts:24` — append só se truthy; `UsuarioRepository.java:25` — `funcionarioId IS NULL OR` | ✅ |
| Rubrica inativa ainda no detalhe Folha | Sort pós-ACL sem filtro status cadastral (`FolhaPagamentoService.java:54-58`) | ✅ |
| Códigos mistos → ordem lexicográfica | `CASE_INSENSITIVE_ORDER` + test `100`/`200` | ✅ |
| API filtros falha → toast, mantém lista | `Rubricas/index.tsx:91-93`; `Usuarios/index.tsx:207-209` — catch sem `set*([])` | ✅ |
| Submit com senha visível → mesmos valores | toggle só altera `type`, não `field.value` | ✅ |
| Reabrir dialog após erro senha → toggles ocultos | `AlterarSenhaDialog/index.tsx:54-60` — reset em `open` | ✅ |

---

## Discrimination Sensor

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `FolhaPagamentoService.java:57-58` | Removed `.sorted(...)` comparator | ✅ Killed — `FolhaPagamentoServiceTest.java:217` — `expected: <100> but was: <200>` |
| 2 | `RubricaService.java:51-52` | `INATIVO` resolver returns `true` instead of `false` | ✅ Killed — `RubricaServiceTest#listar_inativo_passes_ativo_false_to_repository` BUILD FAILURE (verify mismatch) |
| 3 | `UsuarioService.java:39-45` | Removed `.trim()` / whitespace guard on nome/login | ✅ Killed — `UsuarioServiceTest#listar_trim_ignora_espacos_em_branco` BUILD FAILURE (verify expected `isNull()` patterns) |

**Sensor depth**: lightweight (3 mutations)  
**Result**: 3/3 killed — ✅ PASS  
**Note**: Mutations applied in scratch copy; working tree restored after each run.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep | ✅ |
| Matches existing patterns (Funcionario filtros, Usuarios olhinho) | ✅ |
| Spec-anchored outcome check | ⚠️ 3 precision gaps (see above) |
| Per-layer coverage (BE unit for domain; FE build gate) | ✅ per tasks matrix |
| Guidelines: `backend/AGENTS.md`, `frontend/AGENTS.md` | ✅ |

---

## Gate Check

| Gate | Command | Result |
| ---- | ------- | ------ |
| Backend full | `cd backend && mvn test` | **180** run, **0** failed, **0** skipped — BUILD SUCCESS |
| Frontend build | `cd frontend && npm run build` | BUILD SUCCESS (tsc + vite) |

**Feature-scoped test classes**:

| Class | Tests | New ordering/filter tests |
| ----- | ----- | ------------------------- |
| `FolhaPagamentoServiceTest` | 15 | +2 (`ordenar_por_rubricaCodigo`, `mesmo_rubricaCodigo_ordenar_por_id`) |
| `RubricaServiceTest` | 7 | +7 (new file) |
| `UsuarioServiceTest` | 8 | +6 listagem |

**Skipped tests**: none  
**Sandbox note**: first `mvn test` in sandbox failed (Mockito MockMaker); re-run with full permissions passed.

---

## Fix Plans

None required for spec compliance. Optional hardening (out of current task scope):

### Optional: strengthen ordering/empty assertions

- **What**: Add unit tests asserting empty `consultarPorFuncionario` list; integration-style test with ordered mock entities for Rubricas/Usuários.
- **Where**: respective `*ServiceTest.java`
- **Priority**: Minor (discrimination sensor already kills sort/trim faults)

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| FOLHA-ORD-01 | In Tasks | ✅ Verified |
| FOLHA-ORD-02 | In Tasks | ✅ Verified |
| FOLHA-ORD-03 | In Tasks | ⚠️ Verified (impl; no unit assert) |
| RUB-01…11 | In Tasks | ✅ Verified |
| USR-01…08 | In Tasks | ✅ Verified |
| SENHA-VIS-01…06 | In Tasks | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 25/28 ACs with full file:line + assertion — 3 spec-precision gaps (FOLHA-ORD-03 empty-list test; RUB-02 / USR-01 order not asserted in unit tests; JPQL ORDER BY present)  
**Sensor**: 3/3 mutations killed  
**Gate**: 180 BE tests passed; FE build passed

**What works**: Folha detalhe ordenado; Rubricas filtros BE+FE; Usuários filtros/ordem BE com FE existente; Alterar senha com toggles independentes.

**Next steps**: Optional test hardening for ordering/empty-list assertions; interactive UAT for dialog senha se desejado.
