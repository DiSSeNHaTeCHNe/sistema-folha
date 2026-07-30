# Adequação da Análise de Projeto Specification

**Parent / related:** `_docs/specs/CONCERNS.md`, `_docs/specs/TESTING.md`, análise SonarQube @ 2026-07-29 (`sistema-folha`)  
**Complexity:** Large  
**Spec status:** Done — merged to `main` @ `047e64d` (2026-07-29); Verifier PASS

## Problem Statement

A análise de saúde do projeto (SonarQube + JaCoCo + brownfield `CONCERNS.md`) expôs gaps estruturais entre o estado atual de `main` e um baseline operacional confiável: **5 bugs** (2 blocker), **6 vulnerabilidades** Sonar, **386 code smells**, cobertura agregada **35,3%** (frontend sem testes puxa o índice), domínios críticos com cobertura baixa (**organograma 22,5%**, **security 12%**, **auth 28,9%**), Quality Gate **ERROR**, e dívida documentada (SecurityConfig inconsistente, importação ADP frágil, relatórios UI órfãos, dual model benefícios).

Sem uma feature de adequação rastreável, correções ficam ad hoc e a análise não vira entregáveis verificáveis. Esta feature converte o diagnóstico em remediação **faseada**, com critérios mensuráveis e rastreio spec → design → tasks → validation.

## Goals

- [ ] Zerar **bugs Sonar OPEN** no projeto (blocker + major) com correções mínimas e testes que provam o fix
- [ ] Endereçar **vulnerabilidades Sonar** classificadas CRITICAL/MAJOR com mitigação documentada ou código corrigido
- [ ] Elevar cobertura JaCoCo nos domínios **organograma**, **security** e **importacao** acima de limiares definidos nesta spec
- [ ] Estabelecer gate local reproduzível (`mvn test` + `./diversos/scripts/sonar-analyze.sh`) com Quality Gate **OK** ou exceções explícitas documentadas
- [ ] Atualizar `_docs/specs/CONCERNS.md` refletindo itens resolvidos vs remanescentes

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Zerar todos os 386 code smells | Escopo infinito; tratar só critical/blocker Sonar + smells tocados pelos fixes |
| Remoção completa modelo legado `Beneficio` | Feature separada; dual model permanece |
| Backend de Relatórios PDF | UI órfã documentada; implementação = feature futura |
| Adequação FE completa às skills TARGET (AD-004) | ROADMAP deferred; só setup Vitest mínimo nesta feature (P2) |
| Testcontainers / `@SpringBootTest` em massa | Manter padrão unitário + MockMvc pontual |
| Reescrever `ImportacaoFolhaAdpService` (complexidade 71) | Só null-safety + testes fixture; refactor estrutural fora |
| Mudança de semântica ACL / JWT / organograma | Fix-only; sem alterar contratos HTTP existentes |
| Configurar CI/CD remoto | Gate local documentado; pipeline = M3 DevOps |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| A1 — Escopo = remediação do diagnóstico 2026-07-29 | Sim — bugs/vulns/coverage gaps listados na análise | Usuário pediu "adequação de toda a análise" | y |
| A2 — Entrega faseada P1→P2→P3 | P1 bugs+security blockers; P2 coverage gaps; P3 hygiene Sonar | Large feature; evita big-bang | y |
| A3 — CSRF desabilitado | Manter `csrf.disable()` para API JWT stateless; documentar em `INTEGRATIONS.md` ou AD | Padrão atual; Sonar S4502 = review documentado, não habilitar CSRF cookie | y |
| A4 — Enumeração de usuário (S5804) | Mensagens genéricas unificadas login inválido; sem revelar se login existe | Mitigação pragmática sem redesign auth | y |
| A5 — Cobertura alvo backend (JaCoCo linhas) | **organograma ≥ 50%**, **security ≥ 40%**, **importacao ≥ 75%**, **global backend ≥ 65%** | Baseline atual: 22,5% / 12% / 65% / 62,4%; metas incrementais | y |
| A6 — Sonar QG sucesso | Quality Gate **OK** após `./diversos/scripts/sonar-analyze.sh` OU registro de exceções aprovadas em `validation.md` | QG atual ERROR por `new_coverage` + `new_violations`; foco em bugs=0 primeiro | y |
| A7 — Frontend Vitest | P2: setup + 1 smoke test; lcov opcional P2 | TESTING.md: FE tests = TARGET até ROADMAP | y |
| A8 — `ddl-auto: update` | Fora desta feature; registrar follow-up em CONCERNS | Risco ops; não blocker Sonar | y |
| A9 — JWT secret default | P1: fail-fast ou warn em profile `prod` se `JWT_SECRET` = default | CONCERNS.md item conhecido | y |
| A10 — Commits | Atômicos por task; usuário controla push | Preferência projeto | y |

**Open questions:** none — defaults acima cobrem gray areas; amendment via Discuss só se usuário rejeitar metas de cobertura.

### Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | AAP-02, AAP-03: null-safety upload ADP e DTO organograma |
| Failure / partial-failure | AAP-01: transação benefício não silenciosa; testes provam rollback/commit |
| Idempotency / retry | N/A — fixes pontuais |
| Auth boundaries | AAP-06…AAP-09: SecurityConfig + debug endpoints + JWT secret |
| Concurrency / ordering | N/A |
| Data lifecycle | N/A |
| Observability | AAP-18: remover/gate debug logs produção (OrganogramaController S4507) |
| External-dependency failure | N/A |
| State-transition integrity | N/A |

---

## User Stories

### P1: Zerar bugs Sonar (confiabilidade) ⭐ MVP

**User Story**: Como mantenedor, quero que todos os bugs Sonar abertos sejam corrigidos, para que o rating de confiabilidade não dependa de defects blocker/major conhecidos.

**Why P1**: 2 blockers em `BeneficioMensalService` podem quebrar transações reais; NPEs em importação/organograma são risco operacional.

**Acceptance Criteria**:

1. **(AAP-01)** WHEN `BeneficioMensalService.criarParaUsuario` ou `removerSeAutorizado` forem executados THEN `@Transactional` SHALL aplicar-se corretamente (sem propagação inválida via `this`; Sonar `java:S2229` resolvido)
2. **(AAP-02)** WHEN upload ADP tiver filename null THEN `ImportacaoFolhaAdpController` SHALL retornar 400 com mensagem clara (não NPE; Sonar `java:S2259` resolvido)
3. **(AAP-03)** WHEN `OrganogramaService` receber `dto` nullable no path afetado THEN SHALL tratar com validação ou early return (Sonar `java:S2259` resolvido)
4. **(AAP-04)** WHEN `OrganogramaGrafico` renderizar condicional redundante THEN SHALL simplificar lógica (Sonar `typescript:S3923` resolvido)
5. **(AAP-05)** WHEN `./diversos/scripts/sonar-analyze.sh` completar THEN contagem Sonar `bugs` OPEN SHALL ser **0**

**Independent Test**: `curl` Sonar API `types=BUG&statuses=OPEN` → total 0; testes unitários/MockMvc cobrem AAP-01…03.

---

### P1: Endurecer superfície de segurança ⭐ MVP

**User Story**: Como responsável por segurança, quero mitigar vulnerabilidades Sonar CRITICAL/MAJOR e inconsistências de `SecurityConfig`, para reduzir risco de bypass ACL e config insegura em deploy.

**Why P1**: Rating segurança **D**; CSRF review + enumeração + matchers desalinhados (`CONCERNS.md`).

**Acceptance Criteria**:

1. **(AAP-06)** WHEN `SecurityConfig` for auditado contra rotas reais (`context-path: /api`) THEN matchers obsoletos (ex.: `/api/beneficios/**`) SHALL ser removidos ou corrigidos; rotas `tipo-beneficio` / `beneficio-mensal` SHALL permanecer protegidas conforme regras atuais
2. **(AAP-07)** WHEN API permanecer JWT stateless THEN documentação SHALL explicar CSRF disabled (Sonar S4502 fechado como safe com evidência)
3. **(AAP-08)** WHEN login falhar por credencial inválida THEN resposta SHALL ser genérica (mesma mensagem login inexistente vs senha errada; Sonar S5804 mitigado)
4. **(AAP-09)** WHEN profile produção estiver ativo sem `JWT_SECRET` externo THEN aplicação SHALL falhar startup ou logar ERROR bloqueante (não usar default silenciosamente)
5. **(AAP-10)** WHEN `./diversos/scripts/sonar-analyze.sh` completar THEN contagem Sonar `vulnerabilities` OPEN CRITICAL+MAJOR SHALL ser **0** (MINOR documentados se permanecerem)

**Independent Test**: Testes security existentes + novos para matchers; doc CSRF; Sonar vuln API → 0 CRITICAL/MAJOR.

---

### P2: Cobertura de testes — domínios frágeis

**User Story**: Como Tech Lead, quero testes nos domínios com pior cobertura JaCoCo (organograma, security, importação), para que regressões ACL/import não dependam só de análise manual.

**Why P2**: organograma 22,5%, security 12%, importação ADP sem teste dedicado (`CONCERNS.md`).

**Acceptance Criteria**:

1. **(AAP-11)** WHEN `mvn test` gerar JaCoCo THEN cobertura de linhas do pacote `organograma` SHALL ser **≥ 50%**
2. **(AAP-12)** WHEN `mvn test` gerar JaCoCo THEN cobertura de linhas do pacote `security` SHALL ser **≥ 40%**
3. **(AAP-13)** WHEN `mvn test` gerar JaCoCo THEN cobertura de linhas do pacote `importacao` SHALL ser **≥ 75%**
4. **(AAP-14)** WHEN `ImportacaoFolhaAdpService` processar fixture mínima THEN SHALL existir teste unitário com arquivo/layout representativo (happy path + 1 falha de validação)
5. **(AAP-15)** WHEN `GlobalExceptionHandler` receber exceções mapeadas THEN SHALL existir teste unitário cobrindo ao menos 3 handlers (`NotFound`, `IllegalArgument`, validação)
6. **(AAP-16)** WHEN `mvn test` gerar JaCoCo THEN cobertura global backend (linhas, excl. DTOs conforme `sonar.coverage.exclusions`) SHALL ser **≥ 65%**

**Independent Test**: Parse `backend/target/site/jacoco/jacoco.xml` por domínio; gates numéricos; testes novos passam.

---

### P2: Gate Sonar reproduzível

**User Story**: Como desenvolvedor, quero um gate local claro pós-análise, para saber se o projeto atende qualidade mínima antes de merge.

**Acceptance Criteria**:

1. **(AAP-17)** WHEN `./diversos/scripts/sonar-analyze.sh` rodar após `mvn test` THEN JaCoCo XML SHALL ser importado (sensor JaCoCo > 0ms)
2. **(AAP-18)** WHEN Quality Gate for avaliado THEN projeto SHALL atingir status **OK** OU `validation.md` SHALL listar exceções aprovadas com rationale (máx. 3 itens)
3. **(AAP-19)** WHEN análise completar THEN `_docs/specs/CONCERNS.md` SHALL ser atualizado: itens resolvidos marcados; pendentes com data

**Independent Test**: Script sonar-analyze exit 0; API QG status; CONCERNS diff reflete closes.

---

### P3: Hygiene Sonar critical/blocker smells (touch-only)

**User Story**: Como mantenedor, quero reduzir code smells critical/blocker **nos arquivos alterados por P1/P2**, sem caçar dívida histórica inteira.

**Acceptance Criteria**:

1. **(AAP-20)** WHEN arquivos forem modificados em P1/P2 THEN zero novos issues Sonar severity BLOCKER/CRITICAL SHALL permanecer nesses arquivos
2. **(AAP-21)** WHEN `JwtAuthenticationFilter` field `logger` conflitar com `GenericFilterBean` THEN SHALL renomear (Sonar `java:S1149` blocker)
3. **(AAP-22)** WHEN `@Transactional` via `this` existir em services tocados (ex.: `FolhaTotalizacaoService`, `OrganogramaAcessoService`) THEN SHALL refatorar no mesmo PR **somente se** alterados por esta feature; caso contrário registrar follow-up em CONCERNS

**Independent Test**: Sonar issues search nos paths tocados → 0 blocker/critical.

---

### P3: Fundação de testes frontend (mínimo)

**User Story**: Como time FE, quero Vitest configurado com smoke test, para Sonar deixar de reportar 0% cobertura FE como única realidade.

**Why P3**: AD-004; ROADMAP M3; não exige cobertura FE alta nesta feature.

**Acceptance Criteria**:

1. **(AAP-23)** WHEN `cd frontend && npm test` for executado THEN SHALL existir runner Vitest configurado com ≥ 1 teste passando
2. **(AAP-24)** WHEN `./diversos/scripts/sonar-analyze.sh` rodar com lcov configurado THEN Sonar SHALL importar cobertura FE > 0% (opcional se lcov não configurável sem deps extras — registrar N/A em validation)

**Independent Test**: `npm test` exit 0; Sonar frontend coverage > 0 se AAP-24 aplicável.

---

## Edge Cases

- WHEN fix transacional exigir extrair inner service THEN SHALL usar padrão já existente no repo (self-injection ou bean separado), não inventar framework
- WHEN SecurityConfig change quebrar test `@SpringBootTest` existentes THEN SHALL atualizar testes no mesmo task
- WHEN meta de cobertura organograma não atingível sem Testcontainers THEN SHALL documentar em validation e propor AD follow-up (não baixar meta silenciosamente)
- WHEN Sonar QG falhar só em `new_violations` históricas THEN AAP-18 permite exceção documentada vs zerar 386 smells

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AAP-01 | P1: Bugs | Design | Mapped |
| AAP-02 | P1: Bugs | Design | Mapped |
| AAP-03 | P1: Bugs | Design | Mapped |
| AAP-04 | P1: Bugs | Design | Mapped |
| AAP-05 | P1: Bugs | Design | Mapped |
| AAP-06 | P1: Security | Design | Mapped |
| AAP-07 | P1: Security | Design | Mapped |
| AAP-08 | P1: Security | Design | Mapped |
| AAP-09 | P1: Security | Design | Mapped |
| AAP-10 | P1: Security | Design | Mapped |
| AAP-11 | P2: Coverage | Design | Mapped |
| AAP-12 | P2: Coverage | Design | Mapped |
| AAP-13 | P2: Coverage | Design | Mapped |
| AAP-14 | P2: Coverage | Design | Mapped |
| AAP-15 | P2: Coverage | Design | Mapped |
| AAP-16 | P2: Coverage | Design | Mapped |
| AAP-17 | P2: Sonar gate | Design | Mapped |
| AAP-18 | P2: Sonar gate | Design | Mapped |
| AAP-19 | P2: Sonar gate | Design | Mapped |
| AAP-20 | P3: Hygiene | Design | Mapped |
| AAP-21 | P3: Hygiene | Design | Mapped |
| AAP-22 | P3: Hygiene | Design | Mapped |
| AAP-23 | P3: FE Vitest | Design | Mapped |
| AAP-24 | P3: FE Vitest | Design | Mapped |

**Coverage:** 24 total, 24 mapped to stories, 0 unmapped

---

## Success Criteria

- [ ] Sonar `bugs` = 0; `vulnerabilities` CRITICAL+MAJOR = 0
- [ ] JaCoCo: organograma ≥ 50%, security ≥ 40%, importacao ≥ 75%, backend global ≥ 65%
- [ ] `mvn test` verde (305+ tests, zero regressão)
- [ ] `./diversos/scripts/sonar-analyze.sh` executável com JaCoCo importado
- [ ] Quality Gate OK ou exceções explícitas em `validation.md` (≤ 3)
- [ ] `_docs/specs/CONCERNS.md` atualizado pós-entrega
- [ ] Nenhuma breaking change de contrato HTTP/DTO

---

## Baseline de referência (2026-07-29)

| Métrica | Valor atual |
| ------- | ----------- |
| Sonar coverage (agregado) | 35,3% |
| JaCoCo backend (linhas) | 62,4% |
| Bugs Sonar | 5 |
| Vulnerabilities Sonar | 6 |
| Code smells | 386 |
| Quality Gate | ERROR |
| Testes backend | 305 |

**Evidência:** análise chat + Sonar dashboard `sistema-folha` @ `2026-07-29T17:45:31Z`.

---

## Próximos passos (TLC)

1. **Confirmar spec** — especialmente metas AAP-11…16 (cobertura) e escopo P3 FE
2. **Design** — Large: arquitetura de fixes transacionais, estratégia SecurityConfig, matriz testes por domínio
3. **Tasks** — ~15–20 tasks em 3 fases (P1 bugs/security → P2 coverage/gate → P3 hygiene/FE)
4. **Execute** — após confirmação
