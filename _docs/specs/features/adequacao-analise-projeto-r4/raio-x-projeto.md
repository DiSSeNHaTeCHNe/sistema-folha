# Raio-X do Projeto — sistema-folha

**Data:** 2026-07-29  
**Escopo:** Estado atual do HEAD `main` @ `088a438` (AS-IS — não prescrição de arquitetura futura)  
**Método:** Orquestração code-review (adaptado a X-ray de projeto) + modular-decomposition (Patterns 1–5) + modular-design-principles + SonarQube MCP + JaCoCo local  
**Parent:** `_docs/specs/features/adequacao-analise-projeto-r4/spec.md`  
**Relacionado:** `ARCHITECTURE.md`, `CONCERNS.md`, `STRUCTURE.md`, `TESTING.md`, `STATE.md`, `ROADMAP.md`

---

## Metadados

| Campo | Valor |
| ----- | ----- |
| Branch analisada | `main` (alinhada com `origin/main`) |
| Subagentes | Modular decomposition · Modular principles · Security/quality |
| Sonar projectKey | `sistema-folha` |
| JaCoCo local | `backend/target/site/jacoco/jacoco.xml` (2026-07-29) |
| Quality Gate | **OK** |
| Achados materiais | ~15 sinais P1/operacionais + 121 code smells Sonar (0 bugs/vulns) |

---

## Resumo executivo

Monólito modular **backend real e testado** (7 domínios + ports + ArchUnit), frontend ainda **brownfield por páginas**, Quality Gate **verde**, cobertura de application layer boa e de borda (controllers/FE) baixa, com dívida operacional viva em Relatórios, secrets/compose e documentação desatualizada.

**Veredito AS-IS:** o projeto **não** é mais o monólito flat descrito em `STRUCTURE.md` / `TESTING.md`. O backend opera como monólito modular com isolamento de infraestrutura e ACL verificáveis. A maturidade full-stack é **assimétrica**: qualidade Sonar de código novo está sob controle (QG OK), mas a superfície de produto ainda carrega gaps reais (Relatórios), acoplamento físico de dados (schema único) e documentação que descreve um estado anterior.

**Plano sugerido:** ver **§8 Sugestões e plano de ação** (prioridades P1–P5, horizontes e anti-padrões).

---

## 1. Qualidade — SonarQube + JaCoCo

### SonarQube (baseline @ main)

| Métrica | Valor |
| ------- | ----- |
| Quality Gate | **OK** |
| Condições QG (new code) | `new_coverage` 80.0% · `new_duplicated_lines_density` 1.37% · `new_violations` 0 |
| Bugs / Vulnerabilities / Hotspots | **0 / 0 / 0** |
| Code smells (OPEN) | **121** |
| Ratings (Reliability / Security / Maintainability) | **A / A / A** (1.0) |
| Duplicação | **1.9%** (~419 linhas, 21 blocos) |
| ncloc | **18 979** |
| Coverage agregado (BE+FE) | **59.8%** (1608/5780 linhas descobertas) |

### JaCoCo backend (local)

| Métrica | Valor |
| ------- | ----- |
| INSTRUCTION | 63.8% (14727/23100) |
| LINE | 81.9% (2922/3568) |
| BRANCH | 34.1% (827/2424) |
| METHOD | 72.2% (898/1244) |
| CLASS | 91.4% (148/162) |

**Pacotes com menor cobertura (instrução):**

| Pacote | % |
| ------ | - |
| `organograma.domain` | 16.3% |
| `beneficios.domain` | 17.3% |
| `auth.domain` | 19.1% |
| `cadastros.domain` | 28.0% |
| `auth.api` | 35.9% |
| `folha.domain` | 42.0% |

**Pacotes com maior cobertura (instrução):**

| Pacote | % |
| ------ | - |
| `folha.application` | 92.7% |
| `auth.application` | 95.0% |
| `organograma.acesso.application` | 95.2% |
| `dashboard.application` | 96.1% |
| `*.port` (vários) | ~100% |

### Issues Sonar CRITICAL (7 abertas)

| Arquivo | Regra | Tema |
| ------- | ----- | ---- |
| `ImportacaoBeneficioMensalService.java` | S3776 | Complexidade cognitiva 27 |
| `RubricaService.java` | S1192 | Literal duplicado |
| `ImportacaoFolhaAdpService.java` | S1192 | Literal filial ADP duplicado |
| `Organograma/index.tsx` | S3776, S2004 | CC 23; nesting >4 níveis |
| `Usuarios/index.tsx` | S2004 | Nesting >4 níveis |

### Cobertura zero (amostra Sonar)

Quase todos os `*Controller` backend e vários `*Service.ts` / form components frontend — testes concentrados em application services, não na borda HTTP/UI.

---

## 2. Arquitetura modular — AS-IS

### Backend

```text
185 classes main · 7 domínios · 19 controllers · 27 migrações Flyway · 56 *Test.java

cadastros 52 | folha 47 | beneficios 25 | auth 19 | organograma 18 | dashboard 8 | importacao 4
+ shared: config, security, exception, shared
```

- Layout `{dominio}.{api|application|domain|infrastructure|port}` — pacotes flat legados (`controller/service/model/...`) = **0 arquivos**.
- **~20 artefatos de port** (interfaces + snapshots/commands).
- **ArchUnit** (`ModularArchitectureTest`): bloqueia `*.infrastructure` estrangeira e bypass de ACL; **não** bloqueia imports cross-domain de entities JPA.
- Deploy: **1 JAR + 1 JVM + 1 PostgreSQL** (AD-007).

### Classes mais densas (LOC)

| Classe | LOC |
| ------ | --- |
| `OrganogramaService.java` | 547 |
| `ImportacaoFolhaAdpService.java` | 456 |
| `BeneficioMensalService.java` | 373 |
| `DashboardService.java` | 367 |
| `FolhaPagamento/index.tsx` | 852 |
| `Organograma/index.tsx` | 1113 |

### Frontend

- `pages/` (15) + `services/` (~16) + `components/` — **sem** `src/features/` (AD-004: target, não gate).
- **27** testes Vitest; **sem** Playwright/E2E configurado.

### Ports publicadas (inventário)

| Port | Consumidores principais |
| ---- | ----------------------- |
| `OrganogramaAcessoPort` | auth, folha (3 svcs), beneficios, dashboard |
| `BeneficioConsultaPort` | folha totalização/ficha/resumo, dashboard |
| `FolhaConsultaPort` / `FolhaImportacaoPort` / `FolhaProcessamentoPort` / `FolhaTotalizacaoPort` | importacao, dashboard, folha interna |
| `CadastrosLookupPort` / `CadastrosImportLookupPort` / `FuncionarioConsultaPort` | importacao, folha, organograma |
| `UsuarioLookupPort` | organograma.acesso, folha, beneficios, dashboard |

### Acoplamento observado

| Tipo | Estado |
| ---- | ------ |
| Ports (application layer) | **Forte** — ACL, benefício, folha via contratos |
| Model JPA cross-domain (`cadastros.domain.*`) | **Dominante** — FKs em folha/benefícios/auth/organograma |
| Foreign infrastructure | **Quase zero** (exceto `CustomUserDetailsService` → `UsuarioRepository`) |

### Modular decomposition — Patterns 1–5 (síntese)

| Pattern | Achado AS-IS |
| ------- | ------------ |
| **1 — Inventory** | 7 domínios; cadastros (28%) e folha (25%) são os maiores; legacy flat removido |
| **2 — Duplication** | Benefício legado removido; import ADP vs import benefício mensal são fluxos distintos; ORM compartilha `cadastros.domain` |
| **3 — Flattening** | Hierarquia `dominio → camada` consistente; `organograma.acesso` é submódulo intencional |
| **4 — Coupling** | Application layer usa ports; domain layer acoplado via JPA entities |
| **5 — Domain map** | cadastros (hub), folha (core), beneficios/organograma/importacao/dashboard (supporting), auth (generic) |

---

## 3. Scorecard — 10 princípios modulares (AS-IS)

| # | Princípio | Status | Nota |
| - | --------- | ------ | ---- |
| 1 | Well-defined boundaries | **Partial** | BE Strong, FE Weak |
| 2 | Composability | **Partial** | Ports in-process; Relatórios FE sem BE |
| 3 | Independence | **Partial** | Spring context + DB único |
| 4 | Individual scale | **Weak** | Pool/JVM únicos |
| 5 | Explicit communication | **Partial** | Ports Strong; entities vazam em ports |
| 6 | Replaceability | **Partial** | Adapters in-process |
| 7 | Deployment independence | **Weak** | Por decisão AD-007 |
| 8 | State isolation | **Weak** | Schema único + FKs cross-domain |
| 9 | Observability | **Partial** | `DomainLogging` em ~13 services |
| 10 | Fail independence | **Weak** | Sync in-process; ADP 1 `@Transactional` |

### Sinais P0 / P1 / P2

**P0:** nenhum confirmado (ArchUnit AD-010; ACL via port; JWT fail-fast fora dev/test).

**P1:**

| Sinal | Evidência |
| ----- | --------- |
| Acoplamento JPA cross-domain sem port | FKs `folha`/`beneficios`/`auth` → `cadastros.domain.*` |
| Domain layer leak (ArchUnit não cobre) | `OrganogramaService` importa `cadastros.domain.*` |
| Relatórios FE sem backend | `relatorioService.ts`; zero `@RestController` Relatorio |
| ADP transação ampla | `ImportacaoFolhaAdpService` `@Transactional` monolítico |
| API Key spec ahead of code | AD-014 em `STATE.md`; sem `ApiKey*` no backend |

**P2:**

| Sinal | Evidência |
| ----- | --------- |
| Docs stale | STRUCTURE, CONCERNS, TESTING, ROADMAP |
| CORS/LAN/credentials no repo | `WebConfig`, `application.yml`, `docker-compose.yml` |
| `@tanstack/react-query` morto | `package.json` sem uso |
| Docker Java 21 vs pom 17 | `Dockerfile` vs `pom.xml` |

---

## 4. Segurança e qualidade operacional

### Achados por severidade

#### 🚨 Critical

| Achado | Evidência |
| ------ | --------- |
| Relatórios: rota FE viva, API inexistente | `frontend/src/services/relatorioService.ts`, `routes/index.tsx`; testes mockam serviço (`Relatorios.test.tsx`) |

#### 🔒 Security

| Achado | Evidência | Mitigação existente |
| ------ | --------- | ------------------- |
| Compose `SPRING_PROFILES_ACTIVE=dev` + JWT default | `docker-compose.yml` | `JwtSecretStartupValidator` fail-fast fora dev/test |
| CORS LAN hardcoded | `WebConfig.java` | — |
| Credenciais / JDBC LAN no YAML | `application.yml` | Override por env |
| Tokens em `localStorage` | `tokenService.ts` | Padrão SPA brownfield |
| CSRF disabled | `SecurityConfig.java` | Documentado (API JWT stateless) |
| Swagger/OpenAPI `permitAll` | `SecurityConfig.java` | Restringir em prod |

#### ⚡ Performance

| Achado | Evidência |
| ------ | --------- |
| N+1 count benefícios na totalização | `FolhaTotalizacaoService` loop + `contarLancamentosPorFuncionarioECompetencia` |
| `findAll()` quando período nulo | `FolhaPagamentoService.consultarPorPeriodo` |
| ADP `@Transactional` amplo | `ImportacaoFolhaAdpService.importarFolhaAdp` |
| Count via fetch + `.size()` | `BeneficioConsultaAdapter` |

#### ⚠️ Warning

| Achado | Evidência |
| ------ | --------- |
| Controllers: ~19 vs ~3 WebMvc tests | Cobertura borda HTTP fina |
| Sem E2E Playwright | `frontend/package.json` |
| `application-dev.yml` `ddl-auto: update` | Drift dev-only |
| Benefício legado em CONCERNS | **Resolvido no código** (V1.14; sem `Beneficio.java`) |

### Destaques positivos

- JWT hardening: `JwtSecretStartupValidator`, login timing-safe, filtros sem log de token.
- Regressão security: `SecurityConfig*Test`, `JwtAuthenticationFilterTest`.
- ArchUnit + ports cross-domain.
- EntityGraph/JOIN FETCH em leituras folha/organograma.
- Testcontainers ADP (Docker-gated).

---

## 5. Paisagem de testes (AS-IS)

| Área | Quantidade | Qualidade |
| ---- | ---------- | --------- |
| Backend `*Test.java` | **56** | Forte em services/security; fraca em controllers/repos |
| Frontend Vitest | **27** arquivos (~184 casos pós-R3) | MSW + interceptors; páginas CRUD parciais |
| E2E | **0** | Gap auth refresh, ACL UI, import |
| ArchUnit | 1 suite | Boundaries enforced at build |

| Serviço crítico | Testes? | Gap |
| --------------- | ------- | --- |
| `FolhaTotalizacaoService` | Sim | Sem perf test N+1 |
| `ImportacaoFolhaAdpService` | Sim + integration | Transação não stress-tested |
| `OrganogramaAcessoService` | Sim | Sem E2E FE |
| `AuthenticationService` | Sim | Sem WebMvc chain completa |

---

## 6. Drift documentação ↔ código

| Documento | Diz | Código @ main |
| --------- | --- | ------------- |
| `STRUCTURE.md` | Flat `controller/service/model` | Modular completo |
| `CONCERNS.md` | Dual Benefício **Open** | Removido (V1.14) |
| `CONCERNS.md` | `MethodArgumentNotValid` ausente | Presente em `GlobalExceptionHandler` |
| `TESTING.md` | ~5 testes, sem Vitest/JaCoCo | 56 BE + 27 FE; JaCoCo+Testcontainers |
| `ROADMAP.md` M1 | Relatórios PDF **COMPLETE** | UI existe; backend **não** |
| `ARCHITECTURE.md` | Monólito modular + ports | **Alinhado** |
| `STATE.md` AD-014 | API Key READ-ONLY | Spec untracked; **sem implementação** |

---

## 7. Trabalho em curso (working tree @ análise)

| Item | Estado |
| ---- | ------ |
| `_docs/specs/features/auth-api-keys/` | Spec draft (untracked) |
| `_docs/specs/features/adequacao-analise-projeto-r4/` | Spec/design draft |
| `STATE.md` | Modificado localmente |
| `acl-cc-competencia` | Complete no STATE; squash merge pendente |

---

## 8. Sugestões e plano de ação

> Seção prescritiva — ações sugeridas com base no AS-IS acima. **Não** propõe refatorar tudo: o backend modular já está sólido e o Quality Gate está verde. O maior retorno está em **fechar gaps operacionais**, **alinhar docs com a realidade** e **endurecer bordas** (HTTP, FE, deploy), sem reabrir a migração modular.

### Princípio orientador

| Horizonte | Foco |
| --------- | ---- |
| **Esta semana** | Relatórios (esconder ou API mínima) + compose/secrets |
| **Próxima sprint** | Doc sync (`CONCERNS`, `STRUCTURE`, `TESTING`, `ROADMAP`) via R4 |
| **Contínuo** | ArchUnit + ports; batch count benefícios; WebMvc smoke tests |
| **Quando houver demanda** | `auth-api-keys`, `folha-custo-clt-fix3`, E2E Playwright |

---

### Prioridade 1 — Corrigir o que quebra ou engana hoje

#### 1.1 Relatórios: decidir e executar em 1 sprint

**Problema:** maior risco de produto — rota ativa (`/relatorios`), testes que mockam sucesso, backend inexistente (404 em listar/gerar/download).

| Opção | Ação | Quando escolher |
| ----- | ---- | --------------- |
| **A — API mínima** | Implementar `/relatorios/folha`, `/relatorios/beneficio` (+ download) reutilizando dados de resumo/folha já existentes | Relatórios é uso imediato; alinha ROADMAP M1 que marca “COMPLETE” |
| **B — Esconder** | Remover ou desabilitar menu/rota até existir backend | Relatórios não é prioridade agora |

**Recomendação:** se não há uso imediato → **B agora**; se há → **A com escopo mínimo** (sem PDF complexo no primeiro passo).

**Arquivos envolvidos:** `frontend/src/services/relatorioService.ts`, `frontend/src/pages/Relatorios/`, `frontend/src/routes/index.tsx`; backend — controller inexistente hoje.

#### 1.2 Endurecer deploy (mesmo em homologação)

Gap entre “código seguro” e “deploy seguro”:

| Item | Ação |
| ---- | ---- |
| Profile `dev` no Compose | Não subir `api` com `SPRING_PROFILES_ACTIVE=dev` em ambientes expostos — validator permite JWT default só em dev/test |
| Secrets | Exigir `JWT_SECRET` via env; rotacionar em prod |
| JDBC / Postgres | Externalizar host, user, password (hoje `192.168.68.110`, `postgres/postgres` no YAML/Compose) |
| CORS | Mover origins de `WebConfig.java` para config por ambiente |
| Swagger | `/swagger-ui/**` e `/api-docs/**` `permitAll` — restringir por rede ou profile em prod |

---

### Prioridade 2 — Sincronizar documentação (baixo esforço, alto retorno)

Vários docs descrevem um projeto que **já não existe** — risco de decisões erradas (ex.: reimplementar Benefício legado).

| Documento | O que corrigir | Feature natural |
| --------- | -------------- | --------------- |
| `CONCERNS.md` | Benefício legado removido; handler `MethodArgumentNotValid` presente; status Relatórios | R4 |
| `STRUCTURE.md` | Remover árvore `controller/service/model`; refletir `{dominio}.{camada}` | R4 |
| `TESTING.md` | 56 BE + 27 FE; JaCoCo; Testcontainers; Vitest/MSW | R4 |
| `ROADMAP.md` | M1 Relatórios “COMPLETE” vs UI sem backend | R4 |
| `backend/AGENTS.md` | Contagens e layout modular | R4 |
| `frontend/AGENTS.md` | Nota brownfield AD-004 (target vs obrigação) | R4 |

**Sugestão:** tratar doc sync como **primeira entrega da R4** (docs-first), antes de features novas.

---

### Prioridade 3 — Qualidade onde o raio-x mostrou buraco

#### Backend

| Ação | Impacto | Esforço |
| ---- | ------- | ------- |
| **WebMvc smoke tests** — Auth, Dashboard, cadastros críticos | Hoje ~19 controllers, ~3 com teste dedicado; fecha gap borda HTTP | Médio |
| **Batch count em `BeneficioConsultaPort`** | Elimina N+1 em `FolhaTotalizacaoService` (loop + `contarLancamentosPorFuncionarioECompetencia`) | Baixo |
| **Importação ADP** — medir timeout em arquivo grande antes de chunking | Transação `@Transactional` ampla; não reescrever de imediato | Baixo → médio |

#### Frontend

| Ação | Prioridade |
| ---- | ---------- |
| Relatórios — teste real ou remover rota (alinhar com P1) | Alta |
| `AuthContext` — `useMemo` no value (Sonar S6481) | Média |
| Páginas sem teste: `TiposBeneficio`, `Cargos`, `CentrosCusto`, `LinhasNegocio` | Média |
| **E2E mínimo Playwright:** login → folha resumo → organograma ACL | Média (carryover AAP3-21 / R4) |

#### Cobertura

- QG OK em **código novo** (80%); agregado **59.8%** — não é crise, mas priorizar:
  - pacotes `*.domain` (16–42% instr.)
  - controllers backend
  - `*Service.ts` frontend

Meta interna R4: **`new_coverage` ≥ 85%** (margem sobre floor 80%).

---

### Prioridade 4 — Modularidade: manter, não expandir

O backend já cumpre AD-007/AD-010. **Não** sugerir novo split de pacotes ou microserviços.

**Manter como gate de PR:**

- Cross-domain só via `*.port` na application layer.
- ArchUnit verde.
- Código novo no domínio correto (`{dominio}.{camada}`).

**Aceitar como monólito consciente (não tratar como bug):**

- FKs JPA cross-domain para `cadastros.domain.*`
- 1 schema PostgreSQL compartilhado
- FE brownfield `pages/` — target `src/features/` só quando ROADMAP/AD-004 liberar

**Único refactor modular que vale agora:**

- Extrair métodos / sub-responsabilidades **dentro do domínio** em classes densas:
  - `OrganogramaService` (547 LOC)
  - `ImportacaoFolhaAdpService` (456 LOC)
- Sem novos bounded contexts.

---

### Prioridade 5 — Backlog alinhado ao STATE

Ordem sugerida de execução:

| # | Item | Motivo |
| - | ---- | ------ |
| 1 | **`acl-cc-competencia`** — squash merge | Complete no STATE; entrega pendente de merge |
| 2 | **Doc sync (R4)** | Evita drift; base para próximos agentes/PRs |
| 3 | **Relatórios** — decisão A ou B (P1) | Gap operacional #1 |
| 4 | **`auth-api-keys`** | Spec pronta (AD-014 READ-ONLY); código zero — só se integração/MCP for prioridade |
| 5 | **`folha-custo-clt-fix3`** | Continuidade natural M2 (motor folha) |

---

### O que não fazer agora

| Evitar | Motivo |
| ------ | ------ |
| Reintroduzir ou “migrar” Benefício legado | Já removido (V1.14); `BeneficioConsultaPort` é caminho único |
| Multi-módulo Maven ou extração de serviço | Custo >> benefício no estado atual |
| Perseguir 80% coverage **global** antes de Relatórios e deploy | Retorno decrescente vs gaps P1 |
| Refatorar FE inteiro para `src/features/` | AD-004: target, ainda não gate de PR |
| Reset Sonar `PREVIOUS_VERSION` sem hardening | Esconde dívida; R4 prioriza buffer + testes cirúrgicos |
| Nova maratona de page tests só para Sonar | R3 já provou volume; R4 = sustentabilidade |

---

### Resumo das sugestões

```text
┌─────────────────────────────────────────────────────────────┐
│  PLANO SUGERIDO (pós raio-x)                                │
├─────────────────────────────────────────────────────────────┤
│  P1  Relatórios (A ou B) + hardening compose/secrets        │
│  P2  Doc sync → adequacao-analise-projeto-r4                │
│  P3  WebMvc smoke · batch benefício · E2E mínimo            │
│  P4  Manter ArchUnit/ports · refator interno classes densas │
│  P5  Merge acl-cc → R4 → Relatórios → auth-keys → fix3    │
│  ✗   Microserviços · benefício legado · FE features/      │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Diagrama de estado atual

```text
┌─────────────────────────────────────────────────────────────┐
│  ESTADO ATUAL @ main (088a438)                              │
├─────────────────────────────────────────────────────────────┤
│  BE:  monólito modular maduro (ports + ArchUnit)            │
│  FE:  SPA brownfield por página                             │
│  QG:  OK · 0 bugs/vulns · 121 smells · dup 1.9%            │
│  Testes: application/ACL fortes · borda HTTP/FE fraca       │
│  Dados: 1 schema · cadastros como hub JPA                   │
│  Risco vivo: Relatórios 404 · secrets/compose · docs stale  │
│  Spec ahead: auth-api-keys (sem código)                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. Referências de execução

| Artefato | Path |
| -------- | ---- |
| ArchUnit | `backend/src/test/java/.../arch/ModularArchitectureTest.java` |
| Sonar config | `sonar-project.properties` |
| JaCoCo thresholds | `diversos/scripts/check-jacoco-thresholds.sh` |
| Sonar analyze | `diversos/scripts/sonar-analyze.sh` |
| R3 validation (baseline anterior) | `_docs/specs/features/adequacao-analise-projeto-r3/validation.md` |
| R4 spec (follow-up) | `_docs/specs/features/adequacao-analise-projeto-r4/spec.md` |

---

_Relatório gerado por orquestração multiagente (code-review + modular-decomposition + modular-design-principles + Sonar/JaCoCo). Snapshot AS-IS — revisar após merges significativos ou novo ciclo de adequação._
