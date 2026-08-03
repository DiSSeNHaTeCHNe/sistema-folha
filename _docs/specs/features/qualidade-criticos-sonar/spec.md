# Qualidade — 7 CRITICAL do Sonar + S8688 (tempo testável) Specification

## Problem Statement

A base está verde (Quality Gate OK, A/A/A, 0 bugs/vulns/blocker), mas 7 issues **CRITICAL** permanecem e a regra **`java:S8688`** (`.now()` sem `ZoneId`/`Clock`) é a #1 do projeto com **23 ocorrências** no backend. Além do smell, o `.now()` implícito torna o comportamento dependente de tempo **não determinístico e frágil em teste** — foi a causa-raiz dos 2 testes que apareceram vermelhos no fluxo ADP. Corrigir agora, com o gate de código novo já ativo, impede que essa dívida cresça e destrava testes determinísticos de tempo.

## Goals

- [ ] **0 issues CRITICAL** abertas no SonarQube para o projeto (hoje: 7).
- [ ] **0 ocorrências de `java:S8688`** no backend `src/main` (hoje: 23) via injeção de `Clock` nas camadas de aplicação.
- [ ] **Tempo testável:** pelo menos os fluxos de negócio dependentes de "agora" (expiração de token/API key, competência default, `isExpirada`) passam a ser testados com um `Clock` fixo.
- [ ] **Zero regressão:** suíte completa verde (backend 1044+ testes, frontend), sem enfraquecer/remover asserts; Quality Gate permanece **OK** e `new_violations = 0`.
- [ ] Cobertura permanece **≥ 95%** linha/branch (backend) e não cai abaixo do gate.

## Out of Scope

| Item | Reason |
| --- | --- |
| Refactor do God method `ImportacaoFolhaAdpService.importarFolhaAdp` (CC 71) | Não está entre os 7 CRITICAL atuais; refactor grande e de risco — merece feature própria |
| Automatizar Sonar/JaCoCo `check` no CI (mover gate do script externo para o build) | Domínio de infra/CI, não de código; recomendado como follow-up separado (ver Assumptions) |
| Smells cosméticos em massa (S6204, S6759, S1874, S2629, S1128…) | Baixo risco; PR de autofix próprio; o gate de código novo já os contém |
| Mudança de fuso horário efetivo da aplicação | Correção preserva comportamento; `Clock` default = zona atual do sistema |
| Injeção de `Clock` em entidades JPA via container | Entidades não são beans Spring (ver decisão em Assumptions) |
| Mover `@PrePersist` de entidades para serviços (Opção B completa) | Rejeitada na Specify original; QUAL-12 cobre **somente** comparação de expiração na validação auth |

---

## Assumptions & Open Questions

| Assumption / decisão | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| **Onde o `Clock` é injetado** | Um `@Bean Clock clock()` (`Clock.systemDefaultZone()`) em `config`; injetado nos **serviços/adapters** (beans). | Serviços são beans → injeção limpa e testável; preserva a zona atual. | **y** |
| **`.now()` em entidades JPA (`@PrePersist`/`@PreUpdate`)** — `Funcionario`, `RefreshToken`, `BeneficioMensal`, `NoOrganograma`, `CentroCustoOrganograma`, `FuncionarioOrganograma`, `TipoBeneficio`, `ApiKey` | **Opção A (confirmada):** manter timestamp no `@PrePersist` usando `LocalDateTime.now(Clock.systemDefaultZone())` explícito (satisfaz S8688, preserva comportamento). | Baixo risco; comportamento idêntico. Trade-off aceito: o tempo das entidades permanece não-testável (testabilidade fica nos serviços via QUAL-10). | **y** |
| **Escopo de "0 S8688"** | Apenas backend `src/main` (23 ocorrências). | Frontend não tem essa regra; foco onde o smell existe. | n |
| **Validação de expiração auth (QUAL-12)** | **Opção B parcial (confirmada 2026-08-03):** comparação `now(clock).isAfter(dataExpiracao)` na **camada de serviço** (`validarRefreshToken`, `autenticarPorChave`); entidades mantêm `isExpirado()`/`isExpirada()` para outros usos, mas auth **não delega** a elas. | Fecha split-brain Clock; QUAL-10 literal sem contorno de `setDataExpiracao`; comportamento idêntico com bean default. | **y** |

**Open questions:** nenhuma bloqueante.

**Emenda B2 (2026-08-03):** usuário aprovou QUAL-12 — validação de expiração auth na camada de serviço com `Clock` injetado (Opção B parcial). `@PrePersist` permanece Opção A.

**Questões abertas (code-review pós-Execute — classificação (c), fora dos ACs; não implementar nesta feature):**
- Organograma: god-file (~1.3k linhas) após extração in-file — split em módulos irmãos; `console.log` de debug residual; helpers duplicados de parse/filter.
- JwtService: cobertura Sonar **70%** em paths de erro JWT (parse/signature/key guard) — pré-existente; fora do escopo.
- RefreshToken entidade: linhas `@PrePersist`/`isExpirado` com cobertura baixa (Opção A limita Clock.fixed na entidade).
- ImportacaoFolhaAdpService: smells S8786 (regex), S1068 (`rubricasIgnore`), S112/S1141 — explicitamente fora de escopo.
- ImportacaoBeneficioMensalService: N+1 (2 SELECTs/linha) pré-existente — bulk-load seria melhoria de perf, não AC.
- AuthenticationServiceAcessoTest: sem `@Spy Clock` (risco NPE se testes futuros tocarem authenticate/refresh).
- Usuarios FE: `typescript:S1874` InputProps deprecated (pré-existente, fora dos hunks).

**Implicit-requirement dimensions sweep (Large):** Input validation — N/A (sem novas entradas). Failure/partial states — N/A (refactor preserva fluxos existentes). Idempotency/retry — N/A. Auth boundaries — N/A (guards inalterados). Concurrency/ordering — N/A. Data lifecycle/expiry — **coberto** (QUAL-08/QUAL-09: expiração de token/API key passa a usar `Clock`). Observability — **coberto** (QUAL-11: logging preservado nos métodos refatorados). External-dependency failure — N/A. State-transition integrity — N/A.

---

## User Stories

### P1: Eliminar os 7 CRITICAL ⭐ MVP

**User Story**: Como mantenedor, quero que as 7 issues CRITICAL do Sonar sejam corrigidas sem mudar comportamento, para que a base fique sem alertas de alta severidade e mais legível.

**Why P1**: São a maior severidade aberta; escopo explícito do pedido.

**Acceptance Criteria**:

1. WHEN a análise Sonar rodar após o fix THEN o projeto SHALL reportar **0 issues CRITICAL**.
2. WHEN `ImportacaoBeneficioMensalService.importar` (S3776, l.61) for refatorado THEN sua complexidade cognitiva SHALL ficar **≤ 15** e o comportamento observável (resultado da importação) SHALL permanecer idêntico (testes existentes verdes).
3. WHEN a função em `frontend/src/pages/Organograma/index.tsx:408` (S3776) for refatorada THEN a complexidade cognitiva SHALL ficar **≤ 15** sem alterar o render/comportamento.
4. WHEN as funções aninhadas em `Organograma/index.tsx:465,470` e `Usuarios/index.tsx:621` (S2004) forem extraídas THEN o aninhamento SHALL ficar **≤ 4 níveis**.
5. WHEN o literal duplicado em `RubricaService.java:64` (S1192, mensagem "Rubrica não encontrada com ID: ") for extraído para constante THEN não SHALL haver duplicação ≥3× do literal.
6. WHEN o literal duplicado em `ImportacaoFolhaAdpService.java` (S1192, "Filial 0065 TECHNE - EDUCACAO" em `inicializarMapaEmpresas`) for extraído para constante THEN não SHALL haver duplicação ≥3× do literal.

**Independent Test**: Rodar `./diversos/scripts/sonar-analyze.sh` → CRITICAL = 0; suíte backend+frontend verde.

---

### P1: Tornar o tempo explícito e testável (S8688) ⭐ MVP

**User Story**: Como mantenedor, quero um `Clock` injetável usado nas camadas de aplicação, para que o smell S8688 desapareça e o comportamento dependente de tempo seja determinístico em teste.

**Why P1**: S8688 é a regra #1 (23 ocorrências) e a raiz da fragilidade de testes de tempo.

**Acceptance Criteria**:

1. WHEN um `Clock` bean for definido em `config` e injetado nos serviços/adapters THEN nenhuma chamada `*.now()` sem argumento SHALL permanecer nos **serviços/adapters** do backend.
2. WHEN a análise Sonar rodar THEN o backend SHALL reportar **0 ocorrências de `java:S8688`**.
3. WHEN o `Clock` for injetado THEN o comportamento default (zona/instante atual) SHALL permanecer idêntico ao anterior (`Clock.systemDefaultZone()`).
4. WHEN entidades JPA marcarem timestamps no `@PrePersist`/`@PreUpdate` THEN SHALL usar forma explícita de zona conforme a decisão registrada em Assumptions (sem mudar o instante gravado).

**Independent Test**: Sonar → S8688 = 0 no backend; grep por `now()` sem arg em serviços = 0.

---

### P2: Testes determinísticos de tempo com Clock fixo

**User Story**: Como mantenedor, quero testes que fixam o `Clock`, para provar que a injeção entregou testabilidade real (não só silenciou a regra) e prevenir os flakes de tempo observados no ADP.

**Why P2**: Sem isto, injetar `Clock` é cerimônia. Fecha a causa-raiz dos testes que oscilaram vermelho.

**Acceptance Criteria**:

1. WHEN existir teste com `Clock` fixo para expiração de refresh token/API key THEN o teste SHALL assertar o resultado esperado em um instante controlado (ex.: token expirado vs válido em datas fixas).
2. WHEN o `Clock` fixo apontar antes/depois da expiração THEN o serviço SHALL retornar, respectivamente, válido/expirado — **usando o mesmo `Clock` injetado no serviço**, sem depender do relógio real nem de `setDataExpiracao` manual para simular expirado.
3. WHEN a validação ocorrer no instante exato de `dataExpiracao` THEN SHALL manter semântica `isAfter` (token/key **válido** no instante exato; expirado somente após).

**Independent Test**: Rodar os testes com `Clock.fixed(...)`; avançar o clock para além do TTL muda o retorno de `validarRefreshToken`/`autenticarPorChave` sem mutar a entidade.

---

### P2: Validação auth com Clock único (QUAL-12) ⭐ Emenda B2

**User Story**: Como mantenedor, quero que a **validação** de expiração de refresh token e API key use o mesmo `Clock` injetado da criação, para eliminar split-brain e tornar QUAL-10 testável de ponta a ponta.

**Why P2**: Code-review identificou que serviços usam `clock` injetado mas delegam expiração a entidades com `Clock.systemDefaultZone()` — fragilidade latente e testes contornados.

**Acceptance Criteria**:

1. WHEN `RefreshTokenService.validarRefreshToken` for chamado THEN a comparação de expiração SHALL usar `LocalDateTime.now(clock).isAfter(dataExpiracao)` (ou equivalente com o `Clock` injetado), **não** delegar a `RefreshToken.isExpirado()`.
2. WHEN `ApiKeyService.autenticarPorChave` verificar expiração THEN SHALL usar o `Clock` injetado do serviço, **não** delegar a `ApiKey.isExpirada()`.
3. WHEN o bean `Clock` for `Clock.systemDefaultZone()` (produção) THEN o comportamento observável SHALL permanecer idêntico ao anterior (mesma semântica `isAfter`).
4. WHEN testes usarem `Clock.fixed` com instância de serviço cujo clock aponta após `dataExpiracao` THEN `validarRefreshToken`/`autenticarPorChave` SHALL retornar inválido/vazio **sem** mutar `dataExpiracao` na entidade.

**Independent Test**: `RefreshTokenServiceTest` e `ApiKeyServiceTest` — duas instâncias de serviço (clock antes e depois do TTL), mesma entidade, asserts de retorno.

---

### P3: Guardrail de não-regressão de qualidade

**User Story**: Como mantenedor, quero garantir que estas correções não baixem cobertura nem introduzam violações novas, para manter o gate verde.

**Why P3**: Proteção transversal; validada pelo gate existente.

**Acceptance Criteria**:

1. WHEN o gate de código novo rodar após o fix THEN `new_violations` SHALL ser **0** e Quality Gate SHALL ser **OK**.
2. WHEN a suíte completa rodar THEN backend e frontend SHALL passar sem asserts enfraquecidos/removidos e cobertura SHALL permanecer ≥ meta (95% linha/branch backend).

---

## Edge Cases

- WHEN um serviço refatorado tiver caminho de erro (ex.: importação com layout inválido) THEN o mesmo erro/exception SHALL continuar ocorrendo (comportamento preservado).
- WHEN o `Clock` fixo cair exatamente no instante de expiração THEN a regra de borda (`isAfter`/`isBefore`) SHALL manter a semântica atual (inclusiva/exclusiva idêntica ao código original).
- WHEN um literal extraído para constante for usado em mensagem de exceção THEN o texto exibido ao cliente SHALL permanecer byte-a-byte igual.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| QUAL-01 | P1: 7 CRITICAL — S3776 ImportacaoBeneficioMensalService | Execute | Verified |
| QUAL-02 | P1: 7 CRITICAL — S3776 Organograma FE | Execute | Verified |
| QUAL-03 | P1: 7 CRITICAL — S2004 Organograma FE (2×) | Execute | Verified |
| QUAL-04 | P1: 7 CRITICAL — S2004 Usuarios FE | Execute | Verified |
| QUAL-05 | P1: 7 CRITICAL — S1192 RubricaService | Execute | Verified |
| QUAL-06 | P1: 7 CRITICAL — S1192 ImportacaoFolhaAdpService | Execute | Verified |
| QUAL-07 | P1: S8688 — Clock bean + injeção em serviços | Execute | Verified |
| QUAL-08 | P1: S8688 — 0 `.now()` sem arg no backend | Execute | Verified |
| QUAL-09 | P1: S8688 — comportamento de tempo preservado (entidades) | Execute | Verified |
| QUAL-10 | P2: testes determinísticos com Clock fixo | Execute | Verified |
| QUAL-11 | P3: guardrail gate/cobertura (0 new_violations, ≥95%) | Execute | Verified |
| QUAL-12 | P2: validação auth com Clock injetado (Opção B parcial) | Execute | Verified |

**Coverage:** 12 total, 12 Verified, 0 Needs Fix, 0 Pending.

---

## Success Criteria

- [ ] SonarQube: **CRITICAL = 0** e **S8688 = 0** no backend, Quality Gate **OK**, `new_violations = 0`.
- [ ] Suíte completa verde; nenhum assert enfraquecido/removido; cobertura ≥ 95% linha/branch (backend).
- [ ] Existe ≥1 teste com `Clock` fixo cobrindo expiração dependente de tempo.
- [ ] Nenhuma mudança de comportamento observável (mensagens, instantes gravados, fluxos de erro idênticos).
