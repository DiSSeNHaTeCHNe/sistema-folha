# Monólito Modular Fix — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/modular-monolith-fix/design.md`  
**Status**: Draft  
**Approach**: A (ports Cadastros+Auth + ArchUnit application-layer + allowlist dashboard/importacao)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004), `modular-monolith-fix/spec.md` (MODFIX ACs), AD-009.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Port adapters (`*ConsultaAdapter`, `*LookupAdapter`) | unit (Mockito) | Happy path + empty Optional; null-arg if design requires; 1:1 methods of port | `backend/src/test/java/**/application/*AdapterTest.java` | `cd backend && mvn test` |
| Application consumers (refactors) | unit (Mockito) | Update mocks to ports; preserve existing AC coverage; no foreign-repo mocks | `backend/src/test/java/**/application/*Test.java` | `cd backend && mvn test` |
| Auth DTO mapping (`AuthenticationService`) | unit (Mockito) | SEM_FUNCIONARIO + grant parcial — all ACL fields asserted (MODFIX-12–13) | `backend/src/test/java/**/auth/application/*Acesso*Test.java` | `cd backend && mvn test` |
| ArchUnit rules | unit (ArchUnit) | Application→foreign infra fails outside allowlist; same-domain OK; suite green | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Port interfaces / docs / checklist scripts | none | — (build / script exit) | — | build / `./diversos/scripts/check-modular-compliance.sh` |
| Controllers MockMvc (P2) | unit (MockMvc) | Delegation to service; no repository in controller | `backend/src/test/java/**/api/*WebMvc*Test.java` | `cd backend && mvn test` |
| Frontend | none | Build mandatory; lint advisory (AD-004); fix only introduced debt | — | `cd frontend && npm run build` (+ lint advisory) |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após adapters/consumers/ArchUnit/auth unit | `cd backend && mvn test` |
| Full | Após FE lint/checklist ou MockMvc P2 | `cd backend && mvn test && cd ../frontend && npm run build` |
| Build | Fechamento de fase / interface-only | `cd backend && mvn clean package -DskipTests` ou `mvn clean package` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Ports Cadastros

```
T1 → T2 → T3 → T4
```

### Phase 2: Port Auth

```
T5 → T6
```

### Phase 3: Wire Benefícios

```
T7 → T8
```

### Phase 4: Wire Folha + Organograma

```
T9 → T10 → T11
```

### Phase 5: Wire Auth UsuarioService

```
T12
```

### Phase 6: ArchUnit application-layer

```
T13
```

### Phase 7: Prova ACL AuthenticationService

```
T14
```

### Phase 8: Contrato FE lint + checklist

```
T15 → T16
```

### Phase 9: P2 MockMvc (opcional — não bloqueia re-validação pai)

```
T17
```

### Phase 10: Gate conformidade final

```
T18
```

---

## Task Breakdown

### T1: Criar interface `FuncionarioConsultaPort`

**What**: Contrato público Cadastros para consulta de funcionário (sem tipos de infrastructure).  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/cadastros/port/FuncionarioConsultaPort.java`  
**Depends on**: None  
**Reuses**: Design §1; padrão `BeneficioConsultaPort`  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Métodos: `findById`, `findByIdAndAtivoTrue`, `findByCpfAndAtivoTrue` → `Optional<Funcionario>`
- [x] Pacote `cadastros.port`; sem imports `*.infrastructure`
- [x] Compila

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(cadastros): add FuncionarioConsultaPort`

---

### T2: Implementar `FuncionarioConsultaAdapter` + testes

**What**: Adapter `@Service` delegando a `FuncionarioRepository`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioConsultaAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioConsultaAdapterTest.java`

**Depends on**: T1  
**Reuses**: Métodos do `FuncionarioRepository`; padrão `BeneficioConsultaAdapterTest`  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Adapter implementa os 3 métodos da port
- [x] Testes: presente / empty Optional para pelo menos `findById` e `findByCpfAndAtivoTrue`
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): implement FuncionarioConsultaAdapter`

---

### T3: Criar interface `CadastrosLookupPort`

**What**: Contrato de lookup CentroCusto + LinhaNegocio.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/cadastros/port/CadastrosLookupPort.java`  
**Depends on**: None (sequencial após T2 no plano)  
**Reuses**: Design §2  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] `findCentroCustoById`, `findLinhaNegocioById` → `Optional<...>`
- [x] Sem imports infrastructure
- [x] Compila

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(cadastros): add CadastrosLookupPort`

---

### T4: Implementar `CadastrosLookupAdapter` + testes

**What**: Adapter `@Service` sobre repos Centro/Linha.  
**Where**:
- `.../cadastros/application/CadastrosLookupAdapter.java`
- `.../cadastros/application/CadastrosLookupAdapterTest.java`

**Depends on**: T3  
**Reuses**: `CentroCustoRepository`, `LinhaNegocioRepository`  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Ambos métodos implementados
- [x] Testes: presente + empty para cada método (ou ≥2 casos cobrindo ambos)
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes novos

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): implement CadastrosLookupAdapter`

---

### T5: Criar interface `UsuarioLookupPort`

**What**: Contrato Auth para lookup de usuário por id/login.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/auth/port/UsuarioLookupPort.java`  
**Depends on**: None (após Phase 1 no plano)  
**Reuses**: Design §3  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] `findById`, `findByLoginAndAtivoTrue` → `Optional<Usuario>`
- [x] Pacote `auth.port`
- [x] Compila

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(auth): add UsuarioLookupPort`

---

### T6: Implementar `UsuarioLookupAdapter` + testes

**What**: Adapter `@Service` sobre `UsuarioRepository` (same-domain).  
**Where**:
- `.../auth/application/UsuarioLookupAdapter.java`
- `.../auth/application/UsuarioLookupAdapterTest.java`

**Depends on**: T5  
**Reuses**: `UsuarioRepository`  
**Requirement**: MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Adapter implementa a port
- [x] Testes: findById / findByLogin presentes e empty
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes novos

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(auth): implement UsuarioLookupAdapter`

---

### T7: Refatorar `BeneficioMensalService` para ports

**What**: Remover `FuncionarioRepository` e `UsuarioRepository`; injetar `FuncionarioConsultaPort` + `UsuarioLookupPort`; atualizar testes.  
**Where**:
- `.../beneficios/application/BeneficioMensalService.java`
- `.../beneficios/application/BeneficioMensalServiceTest.java`

**Depends on**: T2, T6  
**Reuses**: Comportamento ACL/`orElseThrow` existente  
**Requirement**: MODFIX-06, MODFIX-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero imports `cadastros.infrastructure` / `auth.infrastructure` neste service
- [x] Testes mockam ports (não repos estrangeiros)
- [x] Gate: `cd backend && mvn test`
- [x] Suite `BeneficioMensalServiceTest` passa (sem silent deletions)

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(beneficios): BeneficioMensalService uses cadastros/auth ports`

---

### T8: Refatorar `ImportacaoBeneficioMensalService` para `FuncionarioConsultaPort`

**What**: Substituir `FuncionarioRepository` por port; atualizar testes.  
**Where**:
- `.../beneficios/application/ImportacaoBeneficioMensalService.java`
- `.../beneficios/application/ImportacaoBeneficioMensalServiceTest.java`

**Depends on**: T2  
**Reuses**: Lookup por CPF existente  
**Requirement**: MODFIX-06, MODFIX-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero `cadastros.infrastructure` neste service
- [x] Testes usam `@Mock FuncionarioConsultaPort`
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(beneficios): importacao uses FuncionarioConsultaPort`

---

### T9: Refatorar `FolhaPagamentoService` para ports

**What**: Remover repos Cadastros/Auth estrangeiros; injetar `CadastrosLookupPort` + `UsuarioLookupPort`; atualizar testes.  
**Where**:
- `.../folha/application/FolhaPagamentoService.java`
- `.../folha/application/FolhaPagamentoServiceTest.java`

**Depends on**: T4, T6  
**Reuses**: Filtro ACL / map DTO existentes  
**Requirement**: MODFIX-07, MODFIX-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero `cadastros.infrastructure` / `auth.infrastructure` neste service
- [x] Testes mockam ports
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): FolhaPagamentoService uses lookup ports`

---

### T10: Refatorar `OrganogramaService` para ports Cadastros

**What**: Remover `FuncionarioRepository` / `CentroCustoRepository`; injetar ports; cobrir wiring com teste unitário mínimo se inexistente.  
**Where**:
- `.../organograma/application/OrganogramaService.java`
- `.../organograma/application/OrganogramaServicePortWiringTest.java` (criar se não houver teste)

**Depends on**: T2, T4  
**Reuses**: Métodos de vínculo funcionário/centro  
**Requirement**: MODFIX-08, MODFIX-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero `cadastros.infrastructure` neste service
- [x] ≥1 teste unitário prova resolução via port (funcionário ou centro)
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(organograma): OrganogramaService uses cadastros ports`

---

### T11: Refatorar `OrganogramaAcessoService` para `UsuarioLookupPort`

**What**: Remover `UsuarioRepository`; injetar `UsuarioLookupPort`; atualizar testes ACL.  
**Where**:
- `.../organograma/acesso/application/OrganogramaAcessoService.java`
- `.../organograma/acesso/application/OrganogramaAcessoServiceTest.java`

**Depends on**: T6  
**Reuses**: Cenários ACL existentes (3 denial/grant)  
**Requirement**: MODFIX-10 (enabler ArchUnit)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero `auth.infrastructure` neste service
- [x] Testes ACL existentes passam com mock da port
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥3 testes ACL permanecem

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(organograma): ACL service uses UsuarioLookupPort`

---

### T12: Refatorar `UsuarioService` para `FuncionarioConsultaPort`

**What**: Remover `FuncionarioRepository`; injetar port; adicionar/atualizar teste de vínculo funcionário.  
**Where**:
- `.../auth/application/UsuarioService.java`
- `.../auth/application/UsuarioServiceTest.java` (criar se necessário)

**Depends on**: T2  
**Reuses**: Fluxos cadastrar/atualizar com `funcionarioId`  
**Requirement**: MODFIX-09, MODFIX-10

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Zero `cadastros.infrastructure` neste service
- [x] ≥1 teste com mock da port (vínculo funcionário presente ou not found)
- [x] Gate: `cd backend && mvn test`
- [x] Same-domain `UsuarioRepository` permanece permitido

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(auth): UsuarioService uses FuncionarioConsultaPort`

---

### T13: ArchUnit — application-layer foreign infrastructure + allowlist

**What**: Adicionar regras ArchUnit (AD-009): `..application..` não depende de `..infrastructure..` estrangeira; same-domain OK; omitir `dashboard.application` e `importacao.application` com `because` documentando deferral.  
**Where**: `backend/src/test/java/br/com/techne/sistemafolha/arch/ModularArchitectureTest.java`  
**Depends on**: T7, T8, T9, T10, T11, T12  
**Reuses**: Regras Folha→Benefícios existentes (não relaxar)  
**Requirement**: MODFIX-05, MODFIX-11

**Tools**:
- MCP: `user-context7` (opcional — ArchUnit)
- Skill: `modular-design-principles`

**Done when**:
- [x] Regras novas verdes no tree atual
- [x] Introduzir import proibido temporário em scratch mental / confirmação: regra falharia em consumer P1 com infra estrangeira
- [x] Allowlist só dashboard + importacao; comentário/AD-009 no `because`
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` e `cd backend && mvn test`
- [x] Grep: `rg 'cadastros\.infrastructure' backend/src/main/java --glob '**/application/**'` zero fora de `cadastros` (e zero auth.infra fora de auth, exceto allowlist packages)

**Tests**: unit (ArchUnit)  
**Gate**: quick  
**Commit**: `test(arch): enforce application-layer foreign-infra rules`

---

### T14: Teste `AuthenticationServiceAcessoTest` (ACL JSON/DTO)

**What**: Unit test cobrindo SEM_FUNCIONARIO + grant parcial com asserts em todos os campos distintivos de `AcessoUsuarioDTO`.  
**Where**: `backend/src/test/java/br/com/techne/sistemafolha/auth/application/AuthenticationServiceAcessoTest.java`  
**Depends on**: T11 (port ACL estável; mapping Auth já existe)  
**Reuses**: `AccessContextDTO` / `MotivoNegacaoAcesso`; padrão Mockito  
**Requirement**: MODFIX-12, MODFIX-13, MODFIX-14

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Caso SEM_FUNCIONARIO: `temFuncionarioVinculado=false`, `temNoOrganograma=false`, `acessoTotal=false`, centros vazios, `motivoNegacao=SEM_FUNCIONARIO`
- [x] Caso grant parcial: flags true/true, `acessoTotal=false`, `centrosCustoIds` com IDs esperados
- [x] Sem `@SpringBootTest`
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes novos

**Tests**: unit  
**Gate**: quick  
**Commit**: `test(auth): assert AcessoUsuarioDTO ACL signals`

---

### T15: Checklist — mensagem AD-004 no lint advisory

**What**: Reforçar em `check-modular-compliance.sh` que lint é advisory e conformidade modular FE ≠ ESLint global (citar AD-004). Parent MOD-11 já amendado no Design — verificar texto alinhado.  
**Where**: `diversos/scripts/check-modular-compliance.sh` (+ leitura conferência `modular-monolith/spec.md` MOD-11)  
**Depends on**: None (pode rodar após T14 no plano)  
**Reuses**: Design §6  
**Requirement**: MODFIX-01, MODFIX-02

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Seção lint imprime AD-004 + “≠ ESLint verde global”
- [x] Spec pai MOD-11 permanece com lint advisory / build mandatory
- [x] Script ainda exit 0 quando só lint falha e mandatory passa
- [x] Gate: `./diversos/scripts/check-modular-compliance.sh` (mandatory PASS)

**Tests**: none  
**Gate**: full (`./diversos/scripts/check-modular-compliance.sh` + `cd frontend && npm run build`)  
**Commit**: `chore: clarify AD-004 lint advisory in compliance script`

---

### T16: Lint FE — só dívida introduzida pela migração

**What**: Identificar violações ESLint **introduzidas** em arquivos tocados por `modular-monolith`; corrigir ou scoped-disable justificado; documentar contagem pré/pós no Done when / STATE se zero novos.  
**Where**: Arquivos FE alterados pela migração (`AuthContext`, pages Folha/Funcionarios, `types`, services)  
**Depends on**: T15  
**Reuses**: AD-004; não mass-fix brownfield  
**Requirement**: MODFIX-03, MODFIX-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Contagem documentada: erros introduzidos = 0 **ou** corrigidos
- [x] `cd frontend && npm run build` exit 0
- [x] Não expandir para ~42 erros pré-existentes em arquivos não tocados
- [x] Gate: Full FE build

**Tests**: none  
**Gate**: full  
**Commit**: `fix(frontend): clear lint debt introduced by modular migration` (ou `docs:` se zero fix)

---

### T17: P2 — MockMvc delegação `BeneficioMensalController` (opcional)

**What**: `@WebMvcTest` com service mockado; endpoint representativo; status compatível; sem repository no controller.  
**Where**: `backend/src/test/java/.../beneficios/api/BeneficioMensalControllerWebMvcTest.java`  
**Depends on**: T7  
**Reuses**: `SecurityConfigTipoBeneficioTest` pattern  
**Requirement**: MODFIX-15, MODFIX-16 (parcial — Auth MockMvc pode ficar deferred na validation)

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint` (padrões MockMvc)

**Done when**:
- [x] ≥1 teste MockMvc passa com `@MockBean BeneficioMensalService`
- [x] Controller sob teste não injeta repository
- [x] Gate: `cd backend && mvn test`
- [x] Se deferred pelo usuário: marcar na validation — não bloqueia PASS P1

**Tests**: unit (MockMvc)  
**Gate**: quick  
**Commit**: `test(beneficios): MockMvc thin controller delegation`

---

### T18: Gate conformidade final + handoff

**What**: Rodar checklist + suite BE + FE build; atualizar Handoff STATE; confirmar greps MODFIX-06–09.  
**Where**: `diversos/scripts/check-modular-compliance.sh`; `_docs/specs/STATE.md` (Handoff only)  
**Depends on**: T13, T14, T16 (T17 opcional)  
**Reuses**: Design Success Criteria  
**Requirement**: MODFIX-01–14 (fecho P1)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `./diversos/scripts/check-modular-compliance.sh` exit 0
- [x] `cd backend && mvn clean package` — testes passam incl. ArchUnit
- [x] `cd frontend && npm run build` exit 0
- [x] Grep application ≠ cadastros: zero `cadastros.infrastructure` fora de `cadastros` (allowlist dashboard/importacao documentada)
- [x] Handoff aponta: pronto para Verifier `modular-monolith-fix` + re-Verifier pai

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package && cd ../frontend && npm run build`) + checklist  
**Commit**: `chore: record modular-monolith-fix compliance gate`

---

> **Nota:** `_docs/specs/features/modular-monolith-fix/validation.md` é produzido pelo **Verifier** TLC após o último task de Execute — não é task do autor. Em seguida, re-Verifier da feature pai `modular-monolith`.

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4
Phase 2:  T5 ──→ T6
Phase 3:  T7 ──→ T8
Phase 4:  T9 ──→ T10 ──→ T11
Phase 5:  T12
Phase 6:  T13
Phase 7:  T14
Phase 8:  T15 ──→ T16
Phase 9:  T17
Phase 10: T18
```

**Batch packing (Execute):** ~18 tasks → ~3 workers sugeridos  
`{P1+P2=6}`, `{P3+P4+P5=6}`, `{P6+P7+P8+P9+P10=6}` — offer sub-agents se >1 batch.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 | 1 interface | ✅ Granular |
| T2 | 1 adapter + tests | ✅ Granular |
| T3 | 1 interface | ✅ Granular |
| T4 | 1 adapter + tests | ✅ Granular |
| T5 | 1 interface | ✅ Granular |
| T6 | 1 adapter + tests | ✅ Granular |
| T7 | 1 service + test update | ✅ Granular |
| T8 | 1 service + test update | ✅ Granular |
| T9 | 1 service + test update | ✅ Granular |
| T10 | 1 service + wiring test | ✅ Granular |
| T11 | 1 service + test update | ✅ Granular |
| T12 | 1 service + test | ✅ Granular |
| T13 | ArchUnit rules file | ✅ Granular |
| T14 | 1 test class | ✅ Granular |
| T15 | 1 script (+ verify parent) | ✅ Granular |
| T16 | FE lint scoped | ✅ Granular |
| T17 | 1 MockMvc class (P2) | ✅ Granular |
| T18 | Gate + handoff | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | Phase1 start | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | None | T2→T3 (seq) | ✅ |
| T4 | T3 | T3→T4 | ✅ |
| T5 | None | Phase2 start | ✅ |
| T6 | T5 | T5→T6 | ✅ |
| T7 | T2, T6 | after P2 | ✅ |
| T8 | T2 | T7→T8 | ✅ |
| T9 | T4, T6 | Phase4 start | ✅ |
| T10 | T2, T4 | T9→T10 | ✅ |
| T11 | T6 | T10→T11 | ✅ |
| T12 | T2 | Phase5 | ✅ |
| T13 | T7–T12 | after P5 | ✅ |
| T14 | T11 | after T13 in plan; dep T11 | ✅ |
| T15 | None | Phase8 | ✅ |
| T16 | T15 | T15→T16 | ✅ |
| T17 | T7 | Phase9 | ✅ |
| T18 | T13, T14, T16 | Phase10 | ✅ |

Nota: T14 depende de T11 (não de T13); no plano sequencial T13 roda antes — OK (dep satisfeita). T17 depende só de T7; pode ser adiado sem bloquear T18 se P2 deferred.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | -------------- | --------- | ------ |
| T1 | Port interface | none | none | ✅ |
| T2 | Port adapter | unit | unit | ✅ |
| T3 | Port interface | none | none | ✅ |
| T4 | Port adapter | unit | unit | ✅ |
| T5 | Port interface | none | none | ✅ |
| T6 | Port adapter | unit | unit | ✅ |
| T7 | Application consumer | unit | unit | ✅ |
| T8 | Application consumer | unit | unit | ✅ |
| T9 | Application consumer | unit | unit | ✅ |
| T10 | Application consumer | unit | unit | ✅ |
| T11 | Application consumer | unit | unit | ✅ |
| T12 | Application consumer | unit | unit | ✅ |
| T13 | ArchUnit | unit (ArchUnit) | unit | ✅ |
| T14 | Auth DTO mapping | unit | unit | ✅ |
| T15 | Checklist script | none | none | ✅ |
| T16 | Frontend | none | none | ✅ |
| T17 | Controller MockMvc P2 | unit (MockMvc) | unit | ✅ |
| T18 | Docs/gate | none | none | ✅ |

---

## Requirement Traceability (tasks)

| Requirement ID | Tasks |
| -------------- | ----- |
| MODFIX-01 | T15, T18 |
| MODFIX-02 | T15 |
| MODFIX-03 | T16 |
| MODFIX-04 | T16, T18 |
| MODFIX-05 | T13 |
| MODFIX-06 | T7, T8 |
| MODFIX-07 | T9 |
| MODFIX-08 | T10 |
| MODFIX-09 | T12 |
| MODFIX-10 | T1–T12 |
| MODFIX-11 | T13, T18 |
| MODFIX-12 | T14 |
| MODFIX-13 | T14 |
| MODFIX-14 | T14 |
| MODFIX-15 | T17 |
| MODFIX-16 | T17 (parcial) |

**Coverage:** 16 IDs mapped; 0 unmapped.

---

## Tools question (before Execute)

Para cada task, quais ferramentas usar?

**MCPs disponíveis:** `user-context7`, `plugin-linear-linear`  
**Skills relevantes:** `modular-design-principles`, `jpa-performance`, `spring-security`, `spring-boot-new-endpoint`, `tlc-spec-driven`

Defaults já preenchidos por task (NONE / skill nomeada). Confirme se quer alterar algum antes do Execute.
