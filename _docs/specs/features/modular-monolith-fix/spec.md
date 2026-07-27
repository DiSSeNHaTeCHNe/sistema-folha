# Monólito Modular — Fix Follow-up Specification

**Parent feature:** `modular-monolith`  
**Parent validation:** `_docs/specs/features/modular-monolith/validation.md` (Independent Verifier FAIL, 2026-07-26)  
**Complexity:** Medium → Large (ArchUnit + port refactors across multiple domains)

## Problem Statement

A feature `modular-monolith` concluiu Execute (T1–T32) com gates mandatórios verdes (backend 62/62 testes, FE build, checklist modular), porém o Verifier independente reprovou por **4 lacunas** ancoradas na spec pai: (1) AC P1 exige `npm run lint` exit 0 mas o brownfield tem ~42 erros ESLint pré-existentes e o checklist trata lint como advisory; (2) ArchUnit não cobre imports de `..infrastructure..` estrangeira na camada `..application..`, deixando acoplamento cross-domain em cinco services; (3) `GET /auth/acesso` não tem prova automatizada de mapeamento JSON dos sinais ACL distintos; (4) MockMvc de delegação fina em controllers permanece opcional sem cobertura. Este follow-up **não reabre** escopo de produto — fecha gaps de verificação e isolamento modular pendentes para permitir re-validação PASS da feature pai.

## Goals

- [ ] Alinhar contrato FE lint vs checklist modular com honestidade explícita (AD-004): compliance script como fonte de verdade para gates modulares; corrigir apenas dívida lint **introduzida** por `modular-monolith` ou registrar exceção documentada
- [ ] Enforçar isolamento da camada application via ArchUnit + refatoração (ports onde necessário) para eliminar imports de `*.infrastructure` de domínios estrangeiros nos cinco offenders conhecidos
- [ ] Provar contrato HTTP de `GET /auth/acesso` com teste automatizado (MockMvc ou teste de service na borda auth) cobrindo campos ACL distintos
- [ ] (P2) Opcionalmente adicionar MockMvc mínimo confirmando delegação controller → service nos domínios já tocados pela migração
- [ ] Re-verificação independente de `modular-monolith` passa sem lacunas hard (lint contract + ArchUnit application-layer + `/auth/acesso`)

## Out of Scope

Explicitamente excluído. Documentado para evitar scope creep.

| Feature | Reason |
| ------- | ------ |
| Reimplementar migração modular T1–T32 | Já entregue; este follow-up só fecha gaps |
| Zerar toda dívida ESLint brownfield (~42 erros pré-existentes) | AD-004: skills FE são target; fora do escopo salvo erros **introduzidos** por modular-monolith |
| Rewrite frontend (TanStack Query, `src/features/`, Vitest em massa) | Deferred / ROADMAP |
| Novos endpoints, telas ou regras de negócio | Fix-only |
| Microserviços, multi-módulo Maven, eventos assíncronos | Monólito in-process permanece |
| Testcontainers / `@SpringBootTest` em massa | Manter padrão unitário + MockMvc pontual |
| Reabrir remoção legado `Beneficio` ou ports já verificados | PASS na validação pai |
| Alterar semântica ACL além de provar mapeamento JSON existente | Comportamento já corrigido em modular-monolith |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here — nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| FE lint P1 = alinhar contrato, não zerar brownfield | Compliance script (`check-modular-compliance.sh`) permanece fonte de verdade para gates modulares FE; corrigir só lint introduzido por modular-monolith; documentar exceção AD-004 no Success Criteria desta feature e referenciar na re-validação pai | L-002 (candidate): AC literal vs checklist advisory; AD-004 skills FE = target | y |
| ArchUnit application-layer = P1 | Regra ArchUnit cobrindo `..application..` → proibir dependência de `..*.infrastructure..` de **outro** domínio; refatorar offenders conhecidos via ports | L-003 (candidate): domain rules não bastam | y |
| Offenders iniciais (validation pai) | `BeneficioMensalService`, `ImportacaoBeneficioMensalService`, `FolhaPagamentoService`, `OrganogramaService`, `UsuarioService` importando `cadastros.infrastructure.*` | Evidência estática em validation.md | y |
| Port strategy | Introduzir ports read-only em `cadastros.port` (ex.: `FuncionarioConsultaPort`) para pares de maior acoplamento; adapter em cadastros; consumidores dependem só da interface | Princípio 5/8 modular-design; menor blast radius que mover lógica | y |
| `/auth/acesso` test = P1 | MockMvc autenticado **ou** teste unitário de `AuthenticationService.obterAcessoUsuarioPorLogin` com assert explícito em cada campo JSON de `AcessoUsuarioDTO` para fixture SEM_FUNCIONARIO (e ao menos um grant parcial) | L-004 (candidate): port tests ≠ HTTP proof | y |
| Controller MockMvc = P2 | Um teste por controller crítico (BeneficioMensal, FolhaPagamento, Auth) provando zero repository no controller e delegação ao service | Gap spec-precision opcional na pai | y |
| Parent spec amendment | Se lint permanecer advisory, atualizar Success Criteria / AC de `modular-monolith/spec.md` via nota de re-validação ou amendment mínimo documentado neste fix (não reabrir Discuss) | Fechar lacuna MOD-11 sem mentir no Verifier | n (Design decide mecanismo) |
| Working tree uncommitted | Fix parte do mesmo working tree da migração; usuário controla commits | Preferência do projeto | y |

**Open questions:** none — parent spec amendment mechanism (nota vs edit inline) fica em Agent's Discretion no Design.

**Lessons applied (candidate — not yet confirmed):** L-002 (lint AC vs checklist), L-003 (ArchUnit application layer), L-004 (HTTP JSON proof for ACL).

**Implicit-requirement dimensions (Medium):** Auth boundaries covered by `/auth/acesso` test; observability N/A (no new logging); concurrency/idempotency/external-deps N/A for fix-only refactors. Remaining dimensions N/A for this scope.

---

## User Stories

### P1: Alinhar contrato FE lint e checklist modular ⭐ MVP

**User Story**: Como mantenedor do monólito, quero que gates frontend modulares e ACs de lint tenham um contrato único e honesto, para que re-validação não falhe por ambiguidade entre spec literal e checklist advisory.

**Why P1**: Independent Verifier FAIL hard gap — MOD-11 / P1 FE AC5 (`npm run lint` exit 0 vs ~42 erros brownfield).

**Acceptance Criteria**:

1. (MODFIX-01) WHEN `./diversos/scripts/check-modular-compliance.sh` for executado na raiz THEN o script SHALL continuar tratando greps modulares FE e `npm run build` como **mandatory** e `npm run lint` como **advisory** com mensagem explícita citando AD-004
2. (MODFIX-02) WHEN a spec desta feature e a re-validação de `modular-monolith` forem confrontadas THEN o Success Criteria SHALL declarar explicitamente que **conformidade modular FE ≠ ESLint totalmente verde** salvo dívida introduzida por modular-monolith
3. (MODFIX-03) WHEN `git diff` da migração modular-monolith for analisado contra baseline lint THEN o sistema SHALL identificar e corrigir (ou scoped-disable justificado) **somente** violações ESLint **introduzidas** por arquivos tocados na migração; violações pré-existentes em arquivos não tocados SHALL NOT ser escopo obrigatório desta feature
4. (MODFIX-04) WHEN `npm run build` for executado em `frontend/` THEN SHALL exit 0 (regressão zero vs estado pós-modular-monolith)

**Independent Test**: Rodar compliance script → mandatory PASS; documentar contagem lint pré vs pós-fix em arquivos tocados; build verde.

---

### P1: Isolar camada application — ArchUnit + ports ⭐ MVP

**User Story**: Como arquiteto do monólito, quero que services de application não importem infraestrutura de outros domínios, para completar isolamento de estado além das regras ArchUnit já verdes na camada domain.

**Why P1**: Independent Verifier FAIL — application-layer cross-infra (5 offenders); literal intent P2 AC1(a) / Success Criteria “State isolation” incompleto.

**Acceptance Criteria**:

1. (MODFIX-05) WHEN `ModularArchitectureTest` for executado THEN SHALL existir regra que falha se qualquer classe em `..application..` do domínio D depender de classe em `..infrastructure..` de domínio D' onde D' ≠ D (exceção: mesmo domínio raiz, ex. `beneficios.application` → `beneficios.infrastructure`)
2. (MODFIX-06) WHEN o backend for compilado após refator THEN zero imports de `cadastros.infrastructure` (ou equivalente estrangeiro) SHALL permanecer em `beneficios.application.BeneficioMensalService` e `beneficios.application.ImportacaoBeneficioMensalService`
3. (MODFIX-07) WHEN o backend for compilado após refator THEN zero imports de `cadastros.infrastructure` SHALL permanecer em `folha.application.FolhaPagamentoService`
4. (MODFIX-08) WHEN o backend for compilado após refator THEN zero imports de `cadastros.infrastructure` SHALL permanecer em `organograma.application.OrganogramaService`
5. (MODFIX-09) WHEN o backend for compilado após refator THEN zero imports de `cadastros.infrastructure` SHALL permanecer em `auth.application.UsuarioService`
6. (MODFIX-10) WHEN consumidores precisarem de dados de Funcionário/Cadastros THEN SHALL depender de contrato em `cadastros.port` (ou port nomeada no Design), não de repositories JPA estrangeiros; adapter `@Service` reside em `cadastros`
7. (MODFIX-11) WHEN `mvn test` incluir `ModularArchitectureTest` THEN todas as regras ArchUnit (existentes + nova application-layer) SHALL passar com 0 violations

**Independent Test**: `mvn test -Dtest=ModularArchitectureTest` verde; `rg cadastros\.infrastructure backend/src/main/java/**/application` retorna zero em domínios não-cadastros.

---

### P1: Provar contrato JSON `GET /auth/acesso` ⭐ MVP

**User Story**: Como mantenedor de segurança/ACL, quero teste automatizado na borda auth provando que o JSON de acesso expõe sinais ACL distintos, para que regressões de mapeamento sejam detectadas sem depender só de inspeção estática.

**Why P1**: Spec-precision gap hardening — P1 ACL AC7; L-004.

**Acceptance Criteria**:

1. (MODFIX-12) WHEN existir usuário autenticado sem funcionário vinculado e `GET /auth/acesso` for invocado (MockMvc) **ou** `AuthenticationService.obterAcessoUsuarioPorLogin` for chamado em teste THEN a resposta SHALL conter `temFuncionarioVinculado=false`, `temNoOrganograma=false`, `acessoTotal=false`, `centrosCustoIds` vazio ou ausente de centros, e `motivoNegacao` coerente com `SEM_FUNCIONARIO`
2. (MODFIX-13) WHEN existir fixture de funcionário com nó no organograma (grant parcial) THEN o JSON SHALL conter `temFuncionarioVinculado=true`, `temNoOrganograma=true`, `acessoTotal=false`, e `centrosCustoIds` não vazio com IDs esperados
3. (MODFIX-14) WHEN `mvn test` for executado THEN o novo teste SHALL passar sem `@SpringBootTest` full context salvo MockMvc slice já usado no projeto (preferir unit + MockMvc leve conforme `TESTING.md`)

**Independent Test**: Teste nomeado (ex. `AuthAcessoContractTest` ou `AuthenticationServiceAcessoTest`) falha se campos JSON forem omitidos ou invertidos.

---

### P2: MockMvc opcional — delegação controller → service

**User Story**: Como mantenedor da API, quero smoke tests MockMvc confirmando que controllers delegam a services, para reforçar AC de controllers finos além de grep estático.

**Why P2**: Gap opcional na pai (P1 controllers AC5); não bloqueia re-validação se P1 stories passarem.

**Acceptance Criteria**:

1. (MODFIX-15) WHEN MockMvc chamar endpoint representativo de `BeneficioMensalController` com mocks de service THEN o controller SHALL NOT acionar repository; status HTTP SHALL permanecer compatível com contrato existente
2. (MODFIX-16) WHEN MockMvc ou teste equivalente cobrir `AuthController` login/acesso THEN delegação a `AuthenticationService` SHALL ser verificável (mock verify ou ausência de repo no controller — já PASS estático, reforço comportamental)

**Independent Test**: Subconjunto de testes MockMvc passa; grep continua zero `Repository` em controllers.

---

## Edge Cases

- WHEN port Cadastros retornar funcionário inexistente THEN consumidores SHALL propagar exceção de domínio existente (`FuncionarioNotFoundException` ou equivalente), não NPE
- WHEN ArchUnit rule for adicionada THEN classes em `..application..` do domínio cadastros importando `cadastros.infrastructure` SHALL remain allowed (same-domain exception)
- WHEN lint pré-existente permanecer em arquivos não tocados THEN re-validação SHALL NOT exigir exit 0 global desde que MODFIX-01–04 satisfeitos
- WHEN refactor de port alterar wiring Spring THEN `mvn clean package` SHALL continuar passando (startup regression)
- WHEN teste `/auth/acesso` usar MockMvc THEN security filter/JWT SHALL ser configurado conforme padrão existente em testes de security (ex. `SecurityConfigTipoBeneficioTest`)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MODFIX-01 | P1: FE lint contract | Tasks | In Tasks (T15, T18) |
| MODFIX-02 | P1: FE lint contract | Tasks | In Tasks (T15) |
| MODFIX-03 | P1: FE lint contract | Tasks | In Tasks (T16) |
| MODFIX-04 | P1: FE lint contract | Tasks | In Tasks (T16, T18) |
| MODFIX-05 | P1: Application isolation | Tasks | In Tasks (T13) |
| MODFIX-06 | P1: Application isolation | Tasks | In Tasks (T7, T8) |
| MODFIX-07 | P1: Application isolation | Tasks | In Tasks (T9) |
| MODFIX-08 | P1: Application isolation | Tasks | In Tasks (T10) |
| MODFIX-09 | P1: Application isolation | Tasks | In Tasks (T12) |
| MODFIX-10 | P1: Application isolation | Tasks | In Tasks (T1–T12) |
| MODFIX-11 | P1: Application isolation | Tasks | In Tasks (T13, T18) |
| MODFIX-12 | P1: `/auth/acesso` JSON | Tasks | In Tasks (T14) |
| MODFIX-13 | P1: `/auth/acesso` JSON | Tasks | In Tasks (T14) |
| MODFIX-14 | P1: `/auth/acesso` JSON | Tasks | In Tasks (T14) |
| MODFIX-15 | P2: Controller MockMvc | Tasks | In Tasks (T17) |
| MODFIX-16 | P2: Controller MockMvc | Tasks | In Tasks (T17) |

**Coverage:** 16 total, 16 mapped to tasks ✅

**Parent traceability:** Fecha lacunas MOD-11 (lint sub-criterion), MOD-15/MOD-16 (ArchUnit application), MOD-12 ACL HTTP proof; MOD-10 delegação MockMvc (P2).

---

## Success Criteria

How we know the feature is successful:

- [ ] Independent re-validation of `modular-monolith` reports **PASS** on former hard gaps: FE lint contract (MODFIX-01–04), ArchUnit application-layer (MODFIX-05–11), `/auth/acesso` JSON test (MODFIX-12–14)
- [ ] `./diversos/scripts/check-modular-compliance.sh` exit 0 (mandatory checks unchanged or clarified)
- [ ] `cd backend && mvn clean package` — all tests pass including expanded ArchUnit
- [ ] `cd frontend && npm run build` exit 0
- [ ] Zero `cadastros.infrastructure` imports in non-cadastros `..application..` packages (static grep + ArchUnit)
- [ ] Documented AD-004 alignment: modular FE compliance does not require full-repo ESLint green unless introduced debt fixed (MODFIX-02)
- [ ] (P2) MODFIX-15–16 implemented or explicitly deferred in validation with user ack — does not block PASS if P1 complete
