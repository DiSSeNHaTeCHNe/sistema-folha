# 🩻 Raio-X do Projeto `sistema-folha` — Estado Atual

> **Escopo:** fotografia do estado **real** do projeto (não do ideal), gerada por orquestração multiagente.
> **Data:** 2026-08-02
> **Branch:** `feat/cobertura-testes-95` (com alterações **não commitadas** na árvore de trabalho)
> **HEAD validado:** `7dea09d`
> **Método:** agente principal como orquestrador + 3 subagentes read-only em paralelo, aplicando as skills do projeto:
> - `.agents/skills/modular-design-principles` (10 princípios + severidade P0/P1/P2)
> - `.agents/skills/modular-decomposition` (Patterns 1–5)
> - `.agents/skills/code-review` (dimensões Segurança, Arquitetura, Regressão, Performance, Testes)
> **Ferramentas:** SonarQube v26.7 (UP) + JaCoCo 0.8.12 + ArchUnit.

---

## Painel executivo

| Indicador | Valor |
|---|---|
| Quality Gate (Sonar) | ✅ **OK** — Reliability **A** · Security **A** · Maintainability **A** |
| Bugs / Vulnerabilidades / Security Hotspots | **0 / 0 / 0** |
| Cobertura real (JaCoCo/lcov, 02/08) | Backend **96,75% instr · 95,39% branch · 96,57% linha** · Frontend **97,4% linha · 95,1% branch · 94,7% functions** |
| Meta de cobertura (≥95% linha e branch, ambas stacks) | **Atingida** (no HEAD limpo) |
| Débito técnico | ~18h (1081 min) · duplicação **1,9%** · NCLOC 18.985 |
| Issues abertas (Sonar facets) | **493** — MINOR 255 · MAJOR 169 · INFO 38 · CRITICAL 28 · BLOCKER 3 |
| Tamanho | ~11.9k LOC backend · 11 módulos · 1044 testes backend + 32 arquivos de teste FE |
| ArchUnit | **18/18** regras de fronteira passando |

### Veredito

Projeto **saudável e maduro** na fotografia estática: gate verde, três A's, zero bugs/vulnerabilidades, cobertura real acima de 95% nas duas stacks, monólito modular com fronteiras forçadas por ArchUnit e higiene de código notável (sem secrets, sem `as any`, sem `TODO/FIXME` reais em produção, sem `printStackTrace`).

**Ressalva do "estado atual":** três fatos vermelhos vivos que a fotografia bonita esconde — a suíte de testes está quebrada na árvore atual, o artefato JaCoCo em disco engana quando regenerado sem recompilar, e a análise Sonar está 3 dias defasada. A saúde "A/A/A" é verdadeira no HEAD limpo `7dea09d`; **não** na foto do working tree agora.

---

## 1. 🔴 O que está QUEBRADO agora (real, não ideal)

1. **Suíte de testes VERMELHA na árvore atual.** Reports de 02/08 07:42 mostram `ImportacaoFolhaAdpServiceTest` com **1 falha + 1 erro** de 61 testes:
   - `importar_fixtureLayoutInvalido_funcionarioInexistente_lancaRuntimeException` → `assertTrue` esperava `true`, veio `false`.
   - `importar_fixtureMinimal_happyPathPersisteLinha` → **Mockito strict-stubbing mismatch** em `folhaConsultaPort.existsResumoAtivo(...)`.
   - **Causa provável:** os fixtures `folha-adp-invalid.txt` e `folha-adp-minimal.txt` estão **modificados e não commitados**. O gate de 95% foi validado no HEAD limpo `7dea09d`, não na árvore atual.

2. **Artefato JaCoCo em disco é enganoso.** Lido sem recompilar com as anotações Lombok `@Generated`, o relatório mostra **~65% instr / 36% branch**. A cobertura real (96,75%) só aparece regenerando `jacoco:report` após o compile. O gate vive num script externo (`diversos/scripts/check-coverage-95.sh`); **não há goal `check` do JaCoCo no `pom.xml`**.

3. **Análise SonarQube defasada (30/07, 3 dias).** O coverage no Sonar (**59,8%**) é pré-push de testes e não reflete a realidade (96,75%). A métrica `code_smells=121` do dashboard também está velha — os *facets* de hoje retornam **493 issues abertas**. Necessário re-rodar `./diversos/scripts/sonar-analyze.sh`.

---

## 2. 🔒 Segurança

- 🔒 **Único achado real — vazamento de PII (CPF):** `ImportacaoFolhaAdpService.java:330-334` monta mensagem com **CPF completo**, faz `logger.warn(msg)` e essas mensagens são concatenadas e lançadas em `RuntimeException` (l.218-223) → CPF propaga para **logs e para a resposta HTTP de erro**. **Recomendação:** mascarar o CPF e não devolver PII bruta ao cliente.
- ✅ Guards de auth centralizados (`SecurityConfig`): RBAC `ROLE_ADMIN` em `/folha-pagamento/processar`, writes de `/tipo-beneficio`, `anyRequest().authenticated()` como default; filtro JWT **fail-closed**.
- ✅ Zero secrets hardcoded; senhas/tokens não são logados. CSRF desabilitado é **legítimo e documentado** (API JWT stateless, com `NOSONAR`).
- 💡 Os 3 BLOCKER do Sonar são **baixo risco real**: `S2387` (shadow do campo `log` em `JwtAuthenticationFilter:53`) e 2× `S2229` (auto-invocação `@Transactional` em `BeneficioMensalService`).

---

## 3. 🏗️ Arquitetura modular (Patterns 1–5 + 10 princípios)

**Maturidade ALTA para monólito modular — nenhum P0.** Fronteiras críticas (isolamento de `infrastructure` e ACL de organograma via port) explícitas e **forçadas por ArchUnit**; ownership de tabela 1:1 módulo→tabela (nenhuma tabela compartilhada); shared kernel mínimo e saudável (`CentroCustoEfetivo`, `DomainLogging`); zero classes órfãs.

### Inventário de módulos (Pattern 1)

| Módulo | Camadas | Papel / bounded context | Perfil |
|---|---|---|---|
| **cadastros** | api/app/dom/port/infra | Dados-mestre (Funcionário, CentroCusto, Rubrica, Cargo, LinhaNegócio) | Supporting — baixa volatilidade, **alto fan-in** |
| **folha** | completo | Cálculo/processamento de folha | Core |
| **beneficios** | completo | Benefícios mensais | Core/supporting |
| **organograma** | completo + sub-contexto `acesso` | Estrutura org + ACL | Supporting |
| **auth** | completo | Identidade/credenciais | Generic |
| **dashboard** | api + application | Read-model/consultas agregadas | Query module (hexágono fino) |
| **importacao** | api + application | Orquestração de ingestão ADP | Orchestration (hexágono fino) |
| config, security, shared, exception | flat | Cross-cutting técnico | — |

### Violações de fronteira

| Sev | Achado | Evidência |
|---|---|---|
| **P1** | Reach-through de repositório | `security/CustomUserDetailsService.java:12-14` injeta `auth.infrastructure.UsuarioRepository` — **não coberto por ArchUnit**. Resolver fundindo `security`+`auth` ou usando `UsuarioLookupPort` (já existe). |
| **P1** | Contrato vaza modelo de domínio | `cadastros/port/FuncionarioConsultaPort.java` e `CadastrosLookupPort.java` retornam **entidades** em vez de Snapshots — enquanto `folha`/`beneficios`/`organograma.acesso` já usam Snapshot. |
| **P2** | Model coupling via JPA `@ManyToOne` cross-module para `cadastros` | `folha`, `beneficios`, `organograma`, `auth` declaram FK para entidades de `cadastros` — **liberado por design** no monólito; só bloqueia extração futura. |
| **P2** | Lógica de "competência" (YearMonth) duplicada | espalhada por `folha` + `beneficios` + `importacao` — candidato a value object compartilhado. |

**Eixo dominante de acoplamento:** `cadastros` é o mega-upstream de quase todos. Pela fórmula força×distância×volatilidade é **balanceado hoje** (mesmo processo/DB; dados-mestre estáveis); só se torna bloqueante ao buscar extração para serviços. Ação barata e de alto valor: converter os 2 ports de `cadastros` para Snapshots.

**Lacunas do ArchUnit:** não cobre (a) `security→auth.infrastructure`, nem (b) pureza de contrato (ports retornando entidades). Duas regras novas fechariam o gap com baixo esforço.

---

## 4. ⚙️ Qualidade de código

- ⚠️ **God method** `ImportacaoFolhaAdpService.importarFolhaAdp` — **complexidade cognitiva 71** (a pior do projeto), ~180 linhas com IO + parsing posicional por `substring` + regras + dedup + persistência + orquestração. Arquivo #1 em issues (40). Também tem lista de 12 parâmetros (`processarRubrica`) e dados de negócio hardcoded (mapa de empresas, rubricas a ignorar).
- ⚠️ **N+1 confirmado** em `OrganogramaService.toDTOCompleto` (2 queries/nó, aplicado sobre todos os nós em `listarTodos`/`obterArvoreCompleta`) + `save()` dentro de recursão. Impacto cresce com o tamanho da árvore.
- ⚠️ **Dead code:** `OrganogramaService.toEntity` (l.506-519) nunca referenciado.
- 💡 2× `S2447` (retorna `null` onde se espera `Boolean` → risco de NPE) em `FuncionarioService`/`RubricaService`.
- 💡 A maioria das 493 issues é **smell cosmético em massa** (S8694 88× · S6204 42× · S2629 log-args 45× · S1128 imports 30×) — autofix, baixo risco.

---

## 5. 🧪 Testes & cobertura

**Suíte robusta, não cosmética** — sinais raros de qualidade: **mutation testing manual documentado** (4/5 mutantes mortos, sobrevivente reportado honestamente), `ArgumentCaptor` + fixtures reais nos hotspots, ratio `assertEquals:assertNotNull` ≈ **19:1**, **zero services públicos sem teste**, `validation.md` com 15/15 critérios rastreados a `file:line`, 596 queries semânticas `getByRole` no frontend.

**Fraquezas localizadas e honestas:**
- ~8 controllers WebMvc testados **só por status HTTP, sem validar o body** (ex.: `ResumoFolhaPagamentoControllerWebMvcTest`). O padrão certo existe (`CargoControllerWebMvcTest:66` usa `jsonPath`), só não é uniforme.
- `ResumoFolhaPagamentoControllerWebMvcTest:78` cristaliza **bug de contrato como esperado**: input inválido devolvendo **500** (deveria ser 400).
- O "95% global" mascara services individualmente <95% branch: `JwtService` (72% instr), `OrganogramaController` (79%), `FolhaPagamentoService` (90% branch).
- Frontend `functions` 94,69% puxado por **helper de teste contado na cobertura** (`renderWithProviders.tsx`, 43% fn) — `vite.config.ts` sem `coverage.exclude`. Smell de config, não de teste.
- **Sem testes de a11y** (`jest-axe`/`vitest-axe` ausentes), embora o uso de `getByRole` deixe os testes implicitamente acessíveis.

---

## 6. 🎯 Ações priorizadas (esforço × impacto)

| # | Ação | Esforço | Impacto | Onde |
|---|---|---|---|---|
| 1 | **Consertar os 2 testes vermelhos** do ADP (ou commitar/reverter os fixtures) | Baixo | 🔴 Alto | `ImportacaoFolhaAdpServiceTest` + fixtures modificados |
| 2 | **Mascarar CPF em logs e não retornar PII em exceções** | Baixo | 🔒 Alto | `ImportacaoFolhaAdpService.java:330-334, 218-223` |
| 3 | **Re-rodar análise Sonar** + adicionar goal JaCoCo `check` no `pom` | Baixo | Alto | `sonar-analyze.sh` + `backend/pom.xml` |
| 4 | Refatorar o God method de importação (CC 71): extrair leitor/parser/validador/orquestrador | Alto | Alto | `ImportacaoFolhaAdpService` |
| 5 | Fechar 2 violações P1: `security→auth.infra` e ports de `cadastros` retornando entidades | Médio | Médio | `CustomUserDetailsService`, `FuncionarioConsultaPort`/`CadastrosLookupPort` |
| 6 | N+1 + dead code em `OrganogramaService`; `S2447` null→Boolean | Médio | Médio | `OrganogramaService`, `Funcionario/RubricaService` |
| 7 | Limpeza cosmética em massa (autofix) + `coverage.exclude` no `vite.config` | Baixo | Baixo | vários |

---

## 7. 🕳️ Lacunas de tooling/governança

- ArchUnit não cobre `security→auth.infrastructure` nem pureza de contrato dos ports.
- Gate de cobertura mora em script externo, não no build Maven → frágil e reproduzível de forma enganosa (armadilha dos 65%).
- Dashboard Sonar defasado dá falsa leitura de coverage (59,8%) a quem só olha a UI.

---

## Anexo — Fontes e evidências

- **SonarQube:** `http://localhost:9000/dashboard?id=sistema-folha` (última análise 2026-07-30).
- **JaCoCo:** `backend/target/site/jacoco/jacoco.csv` (agregado 96,75% instr).
- **Frontend coverage:** `frontend/coverage/coverage-summary.json`.
- **Surefire:** `backend/target/surefire-reports/` (1044 testes; 2 vermelhos em `ImportacaoFolhaAdpServiceTest` em 02/08 07:42).
- **ArchUnit:** `backend/src/test/java/br/com/techne/sistemafolha/arch/ModularArchitectureTest.java` (18 regras).
- **Meta de cobertura:** `_docs/specs/features/cobertura-testes-95/{spec,validation}.md`.

_Relatório consolidado pelo agente orquestrador a partir de 3 subagentes read-only. Nenhum arquivo de código foi modificado nesta análise._
