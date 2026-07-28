# ajustes-sistema Validation

**Date**: 2026-07-28  
**Spec**: `_docs/specs/features/ajustes-sistema/spec.md`  
**Design**: `_docs/specs/features/ajustes-sistema/design.md`  
**Tasks**: `_docs/specs/features/ajustes-sistema/tasks.md`  
**Diff range**: uncommitted working tree — ajustes-sistema scope only (see Diff Range)  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Verdict

**PASS** — all 13 requirement IDs verified; backend gate **23/23** green (post fix cycle 1); frontend build green. Discrimination sensor: **3/6 killed**; **3 surviving** (FE-only per AD-004) — not AC failures.

---

## Fix Cycle 1 (2026-07-28)

| Fix | Spec anchor | Change | Gate |
| --- | ----------- | ------ | ---- |
| API error on resumo fetch | Implicit: "Erros de API exibidos na página (benefícios)" | `BeneficiosMensais/index.tsx` catch sets `setError('Erro ao buscar resumos de benefícios')` | FE build ✅ |
| T2 centros vazios test | T2 Done when: centros vazios → lista vazia | `listarCompetenciasParaUsuario_restrito_sem_centros_retorna_vazio_sem_agregacao_unscoped` | BE 23/23 ✅ |

No spec drift. No new features beyond AC gaps.

---

## Spec-Anchored Acceptance Criteria

| Req ID | Spec-defined outcome (WHEN → THEN) | Evidence | Result |
| ------ | ------------------------------------ | -------- | ------ |
| **MENU-01** | Usuário com `ADMIN` vê seção **Cadastros** com 8 sub-itens | `frontend/src/utils/permissions.ts:14-16` `isAdmin` checks `ADMIN` only; `Layout/index.tsx:90-98` 8 `cadastroItems`; `:118-141` render gated by `userIsAdmin` | ✅ PASS |
| **MENU-02** | Usuário sem `ADMIN` não vê seção Cadastros nem divider anterior | `Layout/index.tsx:118-141` `{userIsAdmin && (<> Divider + Collapse …)}`; `isAdmin` excludes `ACESSO_TOTAL` | ✅ PASS |
| **MENU-03** | Não-admin em rota cadastro → redirect `/dashboard` + notificação; admin acessa normalmente | `AdminRoute.tsx:21-23` `Navigate` with `state: { acessoNegado: true }`; `Dashboard/index.tsx:52-57` snackbar "Acesso negado. Apenas administradores."; `routes/index.tsx:56-65` 8 rotas wrapped; `:25` admin gets `<Outlet />` | ✅ PASS |
| **SENHA-01** | Menu AccountCircle exibe **Alterar senha** além de nome e Sair | `Layout/index.tsx:196-198` `MenuItem` "Alterar senha" between nome and Sair | ✅ PASS |
| **SENHA-02** | **Alterar senha** abre dialog com senha atual, nova, confirmar | `AlterarSenhaDialog/index.tsx:93-168` Dialog + 3 password fields; `Layout/index.tsx:73-76,249-255` opens dialog | ✅ PASS |
| **SENHA-03** | Senha atual incorreta → mensagem clara; dialog permanece aberto | `AlterarSenhaDialog/index.tsx:81-83,97-100` 400 → Alert "Senha atual incorreta", no `handleClose` | ✅ PASS |
| **SENHA-04** | Nova senha &lt; 6 ou confirmação diferente → erro no campo; sem API | `AlterarSenhaDialog/index.tsx:122-125` RHF `minLength: 6`; `:142-145` `validate` confirmação; `handleSubmit` blocks invalid submit; `usuarioService` not called | ✅ PASS |
| **BEN-01** | Tela inicial `/beneficios-mensais` = resumos por competência; filtros ano (select obrigatório) e mês (1–12); Filtrar/Limpar | `BeneficiosMensais/index.tsx:428-475` title "Resumos de Benefícios Mensais"; ano Select `:446-468` + `required`; mês numérico `:433-444`; Filtrar/Limpar `:470-474`; `!mostrarFuncionarios` default | ✅ PASS |
| **BEN-02** | Linhas: competência, total funcionários, total R$, qtd lançamentos, **Ver Funcionários** | `:482-515` table columns + button; data from `listarCompetencias` `:148` | ✅ PASS |
| **BEN-03** | **Ver Funcionários** → lista com Voltar, título competência, filtros linha/CC/busca, cards com nome/cargo/CC/linha/total R$/Ver Benefícios | `:291-424` drill-down view; `:309-380` filters; `:383-416` cards; `BeneficioMensalDTO` extended `cargoDescricao`, `linhaNegocioDescricao` `BeneficioMensalDTO.java:38-45`; `BeneficioMensalService.java:286-300` `toDTO` | ✅ PASS |
| **BEN-04** | **Ver Benefícios** → dialog: código, descrição tipo, valor, observação | `BeneficiosMensais/index.tsx:530-573` dialog table columns; `:555-563` maps `tipoBeneficioCodigo`, `tipoBeneficioDescricao`, `valor`, `observacao` | ✅ PASS |
| **BEN-05** | **← Voltar** retorna a resumos preservando filtros ano/mês | `:240-247` `handleVoltarParaResumos` uses `getValuesResumo()` then `fetchResumosCompetencia(filtrosAtuais)` | ✅ PASS |
| **BEN-06** | Backend aplica ACL organograma em consultas de benefícios; endpoint agrega por competência | `BeneficioMensalService.java:84-95` reuses `obterContextoAcesso` / `centrosParaFiltro`; `:271-278` dual query; `BeneficioMensalRepository.java:64-96` JPQL GROUP BY; tests `listarCompetenciasParaUsuario_acesso_negado_retorna_vazio`, `_acesso_total_usa_query_sem_filtro_centro`, `_restrito_com_centros_usa_query_com_centros` | ✅ PASS |
| **BEN-07** | `GET /beneficio-mensal/competencias?ano=&mes=` agrega por par competência, ACL-scoped, filtrável ano/mês | `BeneficioMensalController.java:51-61`; `BeneficioMensalService.java:182-196` `periodoDe`; test `listarCompetenciasParaUsuario_filtro_ano_mes_restringe_ao_mes`; WebMvc `listarCompetencias_delegaAoService` | ✅ PASS |

**AC coverage**: 13/13 ✅

**Edge cases (spec):**

| Edge | Evidence | Result |
| ---- | -------- | ------ |
| Resumos vazios → mensagem orientativa | `BeneficiosMensais/index.tsx:522-525` "Nenhum benefício mensal encontrado." | ✅ PASS |
| Funcionários vazios pós-filtro | `:420-423` "Nenhum funcionário encontrado para este período." | ✅ PASS |
| Senha: API 400/401/403 sem stack trace | `AlterarSenhaDialog/index.tsx:79-86` catch → Alert or generic notification | ✅ PASS |

---

## Discrimination Sensor

Behavior-level faults injected mentally against existing tests (no tree mutation committed).

| # | Mutation | Expected fault | Killed by existing tests? | Severity if survives |
| - | -------- | -------------- | ------------------------- | -------------------- |
| M1 | `permissions.ts` — `isAdmin` returns true for `ACESSO_TOTAL` | Não-admin com bypass vê Cadastros | ❌ **Survives** — sem teste FE | **Medium** |
| M2 | `routes/index.tsx` — remove `AdminRoute` wrapper | URL direta `/usuarios` acessível | ❌ **Survives** — sem teste FE | **Medium** |
| M3 | `usuarioService.ts` — `alterarSenha` usa PUT em vez de POST | Troca de senha quebrada em runtime | ❌ **Survives** — build passa; sem teste FE | **Medium** |
| M4 | `BeneficioMensalService` — `listarCompetenciasParaUsuario` sempre chama `competenciasResumo` (ignora centros) | Vazamento ACL em agregação | ✅ **Killed** — `listarCompetenciasParaUsuario_restrito_com_centros_usa_query_com_centros` | — |
| M5 | `periodoDe` ignora `mes` | Filtro mês ineficaz | ✅ **Killed** — `listarCompetenciasParaUsuario_filtro_ano_mes_restringe_ao_mes` | — |
| M6 | Remover early-return ACL negado em `listarCompetenciasParaUsuario` | Dados expostos sem organograma | ✅ **Killed** — `listarCompetenciasParaUsuario_acesso_negado_retorna_vazio` | — |

**Sensor score**: 3/6 killed. Remaining M1–M3 are FE-only per tasks matrix (AD-004).

**Partial gap**: `listarCompetenciasParaUsuario_restrito_sem_centros` não tem teste espelhando `listarPorCompetenciaParaUsuario_restrito_sem_centros_retorna_vazio_sem_query_unscoped` — código espelha o padrão (`:90-92`); risco **Low** se alguém remover só esse guard no método de competências.

---

## Gate Check

| Gate | Command | Result |
| ---- | ------- | ------ |
| Quick (Verifier) | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest,BeneficioMensalControllerWebMvcTest` | **23 passed**, 0 failed |
| FE build | `cd frontend && npm run build` | ✅ Success (`tsc -b && vite build`) |
| Full backend | Not required by tasks gate | — |

**Test breakdown**: BeneficioMensalServiceTest 21 | BeneficioMensalControllerWebMvcTest 2

---

## Gaps (ranked by severity)

No AC failures. Recommended hardening (optional, not blocking PASS):

1. **Medium — FE AdminRoute / isAdmin** — No automated guard for MENU-02/03 (M1, M2 survive; AD-004).
2. **Medium — FE alterarSenha HTTP contract** — No test that service uses POST + query params (M3 survives).
3. **Low — BE competencias restrito sem centros** — Missing symmetric unit test for empty-centros early return on `listarCompetenciasParaUsuario`.
4. **Low — BE toDTO cargo/linha** — No unit test asserting enriched fields in `BeneficioMensalDTO` (runtime-only verification).
5. **Low — FE drill-down / senha UX** — Build gate only; manual UAT advised for 3-level navigation and password change E2E.

---

## Diff Range (ajustes-sistema scope only)

**New:**

- `frontend/src/utils/permissions.ts`
- `frontend/src/routes/AdminRoute.tsx`
- `frontend/src/components/AlterarSenhaDialog/index.tsx`
- `backend/.../beneficios/api/BeneficioMensalCompetenciaResumoDTO.java`
- `backend/.../beneficios/infrastructure/BeneficioMensalCompetenciaProjection.java`
- `_docs/specs/features/ajustes-sistema/{spec,design,tasks,validation}.md`

**Modified (feature):**

- `frontend/src/components/Layout/index.tsx`
- `frontend/src/pages/Dashboard/index.tsx`
- `frontend/src/pages/BeneficiosMensais/index.tsx`
- `frontend/src/routes/index.tsx`
- `frontend/src/services/usuarioService.ts`
- `frontend/src/services/beneficioMensalService.ts`
- `frontend/src/types/index.ts`
- `backend/.../beneficios/api/BeneficioMensalController.java`
- `backend/.../beneficios/api/BeneficioMensalDTO.java`
- `backend/.../beneficios/application/BeneficioMensalService.java`
- `backend/.../beneficios/infrastructure/BeneficioMensalRepository.java`
- `backend/.../beneficios/application/BeneficioMensalServiceTest.java`
- `backend/.../beneficios/api/BeneficioMensalControllerWebMvcTest.java`

**Out of scope** (present in working tree but not mapped to this feature): `Funcionario*`, `DashboardService*`, `FolhaConsultaAdapter*`, `ResumoFolhaPagamento*`, `V1.0__initial_schema.sql`, `acl-scoped-folha-resumo`, `funcionarios-folha-dashboard-ux`.

---

## Requirement Traceability Update

| Requirement | Status |
| ----------- | ------ |
| MENU-01 … MENU-03 | ✅ Verified |
| SENHA-01 … SENHA-04 | ✅ Verified |
| BEN-01 … BEN-07 | ✅ Verified |

---

## Fix Tasks (only if FAIL — none required)

N/A — verdict PASS. Optional follow-ups from gaps above:

- **FT-1**: Vitest — `isAdmin` returns false for `ACESSO_TOTAL`-only user; true only for `ADMIN`.
- **FT-2**: Vitest — `AdminRoute` redirects non-admin to `/dashboard` with `acessoNegado` state.
- **FT-3**: Vitest — `usuarioService.alterarSenha` asserts POST to `/usuarios/{id}/alterar-senha` with query params.
- **FT-4**: JUnit — `listarCompetenciasParaUsuario_restrito_sem_centros_retorna_vazio_sem_agregacao_unscoped`.
