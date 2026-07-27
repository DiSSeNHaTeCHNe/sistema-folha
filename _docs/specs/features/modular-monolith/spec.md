# Monólito Modular — Specification

## Problem Statement

O sistema-folha é um monolito Spring Boot + React SPA com pacotes planos por camada (`controller/`, `service/`, `repository/`), acoplamento cross-domain via repositórios injetados em controllers e services, modelo dual de benefícios (legado `beneficios` + `beneficio_mensal`) e ACL de organograma com bug de conflação (`Optional.empty()` trata “sem funcionário” e “sem nó” como o mesmo sinal; `Set` vazio implica acesso total). Isso viola os princípios de modular-design-principles (fronteiras, contratos explícitos, isolamento de estado, lógica fora da borda) e impede evolução segura. Após o harness estável (`ajuste-harness`) e as features de produto de benefícios mensais já entregues, a prioridade é **refatorar** a estrutura — sem novas funcionalidades de produto — mantendo deploy in-process.

## Goals

- [ ] Eliminar completamente o domínio legado `Beneficio` (tabela, entity, repository, fallback, `beneficioService.ts`); `BeneficioMensal` como **única** fonte de custo de benefícios
- [ ] Introduzir contratos explícitos cross-domain (`BeneficioConsultaPort`, `OrganogramaAcessoPort`) com comunicação síncrona in-process; consumidores (Folha, Dashboard) não acessam repositórios de outros domínios
- [ ] Corrigir bug de ACL de organograma em P1: distinguir “sem funcionário vinculado” vs “funcionário sem nó”; **negar acesso** em ambos os casos salvo vínculo explícito; eliminar interpretação de conjunto vazio como acesso total indevido
- [ ] Migrar pacotes incrementalmente por domínio (Benefícios → Folha → demais) com regras ArchUnit crescentes; controllers finos (sem injeção de repository)
- [ ] Alinhar frontend ao mínimo modular (5A): services por domínio, remoção de órfãos, páginas sem import direto de `api.ts`; **sem** rewrite para skills FE target
- [ ] Verificação documentada de conformidade **backend e frontend** com checklist de modular-design-principles ao fechar a feature

## Out of Scope

Explicitamente excluído. Documentado para evitar scope creep.

| Feature | Reason |
| ------- | ------ |
| Novas telas, fluxos ou endpoints de produto | Refator only — sem features novas |
| Backend de Relatórios / PDF | Produto inexistente; fora do escopo (CONCERNS) |
| Pagamentos, microservices, extração para serviços separados | Monólito permanece in-process |
| Rewrite frontend para skills target (`src/features/`, TanStack Query, RHF+Zod em massa) | Escopo 5A = mínimo; AD-004 skills FE = target |
| Reimplementar harness / TLC / paths specs | Já coberto por `ajuste-harness` |
| Reimplementar produto benefícios mensais (CRUD tipos, importação XLSX, telas) | Já coberto por `beneficios-mensais`, `alteracao-beneficios-mensais`, `ajuste-beneficios-reorganizacao` |
| Migração de dados legado `beneficios` → `beneficio_mensal` | Remoção do legado; dados históricos não migrados automaticamente |
| Testcontainers / `@SpringBootTest` em massa | Fora do refactor estrutural salvo gates mínimos por AC |
| Alterar modelo de auth (JWT, refresh, roles) além de alinhar matchers | SecurityConfig = refactor de paths existentes |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here — nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Remover legado `Beneficio` por completo | Drop tabela `beneficios` via Flyway; remover entity/repo/fallback/FE órfão | Mensal é fonte única; elimina dual model (CONCERNS) | y |
| Organograma / Acesso = 2B | Submodule `organograma.acesso` dentro do bounded context Organograma; demais domínios consomem apenas `OrganogramaAcessoPort` | Contrato explícito sem expor repositories internos | y |
| Migração de pacotes = 3B incremental | Ordem: Benefícios → Folha → Cadastros/Organograma/Auth/Dashboard/Importação; ArchUnit cresce a cada domínio migrado | Reduz big-bang; permite gates por fase | y |
| Totalização cross-domain = 4A | `BeneficioConsultaPort` síncrono in-process; após remoção legado, port lê **somente** `beneficio_mensal` | Folha/Dashboard não injetam `BeneficioMensalRepository` | y |
| Frontend = 5A mínimo | Alinhar `services/*` a domínios; remover órfãos; páginas delegam a services (não `api.ts`); manter layout brownfield (`pages/` + `services/`) | AD-004: skills FE são target, não escopo desta feature | y |
| ACL bug = 6A em P1 | Sem funcionário → **negar**; funcionário sem nó no organograma → **negar**; sinais distintos no contrato (não `Optional.empty()` ambíguo); `centrosCustoAcessiveis` vazio **não** significa acesso total salvo flag explícita `acessoTotal=true` com funcionário **e** nó vinculados | Corrige conflação documentada em CONCERNS e `OrganogramaAcessoService` | y |
| Pacote base pós-migração | `br.com.techne.sistemafolha.{dominio}.{camada}` — ex.: `beneficios.application`, `beneficios.domain`, `beneficios.infrastructure`; shared kernel mínimo em `shared` ou `common` só para tipos transversais | Alinha modular-decomposition sem multi-módulo Maven | y |
| Dados em `beneficios` existentes | Drop com migração; operação aceita perda de dados legado não replicados em mensal | Usuário confirmou remoção total, não coexistência | y |
| Domínios P1 backend | Benefícios (completo) + ports + ACL + remoção legado + controllers finos dos controllers já tocados | Vertical slice mínimo demonstrável | y |
| Domínios P2 backend | Folha + demais domínios restantes + ArchUnit enforcement + logging estruturado por domínio | Incremento após P1 estável | y |
| Ordem exata Cadastros vs Organograma em P2 | Cadastros antes de Organograma (menos cross-cutting) | Reduz dependências na segunda leva | n (default OK) |
| Retenção de `Map<String,Object>` em info de acesso | Substituir por DTO tipado (`AcessoUsuarioDTO` evoluído) no port | Contrato explícito (princípio 5) | y |

**Open questions:** none — defaults acima cobrem Cadastros vs Organograma em P2; demais decisões bloqueadas pelo usuário.

---

## User Stories

### P1: Remover modelo dual e legado `Beneficio` ⭐ MVP

**User Story**: Como mantenedor do sistema, quero eliminar por completo o domínio legado de benefícios (`beneficios`), para que exista uma única fonte de verdade (`beneficio_mensal`) e não haja fallback silencioso na totalização ou dashboard.

**Why P1**: Dual model é a maior fonte de inconsistência de custo; bloqueia contratos limpos (`BeneficioConsultaPort`).

**Acceptance Criteria**:

1. WHEN o backend for compilado THEN o sistema SHALL NOT conter `Beneficio.java`, `BeneficioRepository.java`, imports de `model.Beneficio` ou queries ao legado em services
2. WHEN `FolhaTotalizacaoService` calcular totais para uma competência THEN o sistema SHALL obter custos de benefícios **somente** via `BeneficioConsultaPort` (dados mensais), sem branch de fallback legado
3. WHEN `DashboardService` agregar estatísticas de benefícios THEN o sistema SHALL usar `BeneficioConsultaPort` ou métricas derivadas de `beneficio_mensal`, sem `BeneficioRepository`
4. WHEN a migração Flyway for aplicada THEN o sistema SHALL executar DDL idempotente que remove tabela `beneficios` e índices associados (`idx_beneficios_*`)
5. WHEN o frontend for buildado THEN o sistema SHALL NOT conter `frontend/src/services/beneficioService.ts` nem tipos/export órfãos de `Beneficio` legado
6. WHEN testes unitários de totalização/dashboard forem executados THEN o sistema SHALL NOT mockar `BeneficioRepository`; mocks SHALL usar `BeneficioConsultaPort`

**Independent Test**: `mvn test` passa; grep no repo retorna zero ocorrências de `BeneficioRepository`/`/beneficios` API; competência com `beneficio_mensal` totaliza custo correto sem tabela legado.

---

### P1: `BeneficioConsultaPort` in-process (somente mensal) ⭐ MVP

**User Story**: Como domínio Folha/Dashboard, quero consultar custos agregados de benefícios por funcionário/competência via contrato estável, sem acessar persistência do domínio Benefícios diretamente.

**Why P1**: Implementa princípio 5 (comunicação explícita) e 8 (isolamento de estado) para o acoplamento mais crítico pós-remoção legado.

**Acceptance Criteria**:

1. WHEN o módulo Benefícios expõe `BeneficioConsultaPort` THEN o contrato SHALL incluir operações para: (a) somar valor de benefícios por `funcionarioId` + competência; (b) contar lançamentos; (c) verificar existência de dados mensais na competência — retornando `BigDecimal`/`int`/`boolean`, não entities JPA
2. WHEN `FolhaTotalizacaoService` ou `DashboardService` precisarem de dados de benefício THEN eles SHALL depender **apenas** de `BeneficioConsultaPort`, não de `BeneficioMensalRepository`
3. WHEN a implementação do port for invocada THEN ela SHALL ler exclusivamente repositórios/tabelas do domínio Benefícios (`beneficio_mensal`, `tipo_beneficio`)
4. WHEN não existirem lançamentos mensais para a competência THEN o port SHALL retornar zero/contagem zero (não fallback, não exceção)
5. WHEN o port for registrado no Spring THEN SHALL existir uma implementação `@Service` no pacote `beneficios` e wiring por interface no domínio consumidor

**Independent Test**: Teste unitário de `FolhaTotalizacaoService` com mock de `BeneficioConsultaPort` prova totais; teste do adapter Benefícios com repositório mensal mockado prova agregação.

---

### P1: Controllers finos — sem injeção de repository ⭐ MVP

**User Story**: Como mantenedor da API, quero que controllers deleguem exclusivamente a application services, sem lógica de persistência na borda HTTP, para respeitar fronteiras modulares.

**Why P1**: Violação atual documentada (`BeneficioMensalController`, `FolhaPagamentoController`, `ResumoFolhaPagamentoController`, `AuthController` injetam repositories).

**Acceptance Criteria**:

1. WHEN qualquer controller listado em CONCERNS for inspecionado após P1 THEN ele SHALL NOT declarar campos `*Repository` como dependências
2. WHEN `BeneficioMensalController` receber POST/PUT/GET THEN o controller SHALL delegar a `BeneficioMensalService` (ou facade equivalente no pacote beneficios) incluindo resolução de usuário logado e validações hoje inline
3. WHEN `FolhaPagamentoController` ou `ResumoFolhaPagamentoController` forem chamados THEN toda query/soft-delete/map DTO SHALL residir em service de Folha, não no controller
4. WHEN `AuthController` precisar de dados de usuário THEN SHALL usar `UsuarioService` ou serviço de Auth, não `UsuarioRepository` direto
5. WHEN `mvn test` for executado THEN testes existentes SHALL passar; novos testes de controller (MockMvc opcional) SHALL confirmar delegação sem regressão de status HTTP

**Independent Test**: Revisão estática — zero `Repository` imports em `controller/` dos domínios tocados; smoke manual/Swagger dos endpoints afetados retorna mesmos contratos JSON.

---

### P1: Pacotes por domínio — leva Benefícios ⭐ MVP

**User Story**: Como arquiteto do monólito, quero o domínio Benefícios reorganizado em pacote coeso (`beneficios.*`), como primeira leva da migração 3B, para estabelecer o padrão das demais levas.

**Why P1**: Primeiro domínio com fronteira clara pós-remoção legado; habilita ArchUnit inicial.

**Acceptance Criteria**:

1. WHEN o código de Benefícios for localizado THEN SHALL residir sob `br.com.techne.sistemafolha.beneficios` com subpacotes mínimos: `api` (controllers), `application` (services/use cases), `domain` (entities), `infrastructure` (repositories), `port` (interfaces públicas incl. `BeneficioConsultaPort`)
2. WHEN classes forem movidas THEN imports em outros domínios SHALL apontar para pacotes públicos (`port`, `api` DTOs compartilhados) — não para `infrastructure`
3. WHEN a aplicação iniciar THEN Spring SHALL scanear e registrar beans do pacote `beneficios` sem regressão de rotas existentes (`/beneficio-mensal`, `/tipo-beneficio`, `/importacao/beneficios-mensais`)
4. WHEN `grep -r "sistemafolha.service.BeneficioMensalService"` for executado THEN SHALL retornar zero (classe realocada)
5. WHEN testes do domínio Benefícios forem executados THEN `mvn test -Dtest=*Beneficio*,*TipoBeneficio*,*ImportacaoBeneficioMensal*` SHALL exit 0

**Independent Test**: Boot local + CRUD tipo benefício + listagem mensal via API; estrutura de pastas conforme AC1.

---

### P1: `OrganogramaAcessoPort` + correção do bug de ACL ⭐ MVP

**User Story**: Como operador com login válido, quero que o controle de acesso hierárquico negue corretamente usuários sem funcionário ou sem nó no organograma, e que outros domínios consultem ACL apenas via contrato explícito.

**Why P1**: Bug de segurança/dados — `Optional.empty()` e `Set` vazio tratados como acesso total indevidamente (CONCERNS, regra 4 conflitando com regra 3).

**Acceptance Criteria**:

1. WHEN `OrganogramaAcessoPort` for definido THEN o contrato SHALL expor: `obterCentrosCustoAcessiveis(usuarioId)`, `usuarioPodeAcessarCentroCusto(usuarioId, centroCustoId)`, `obterContextoAcesso(usuarioId)` retornando DTO tipado com campos explícitos: `temFuncionarioVinculado`, `temNoOrganograma`, `acessoTotal` (boolean), `centrosCustoIds`, `motivoNegacao` (enum ou código quando negado)
2. WHEN usuário **não** tiver funcionário vinculado THEN o port SHALL retornar `temFuncionarioVinculado=false`, `acessoTotal=false`, conjunto de centros **vazio** e `usuarioPodeAcessarCentroCusto` SHALL retornar **false** para qualquer centro
3. WHEN usuário tiver funcionário **sem** vínculo a nó ativo no organograma THEN o port SHALL retornar `temFuncionarioVinculado=true`, `temNoOrganograma=false`, `acessoTotal=false`, centros vazios e `usuarioPodeAcessarCentroCusto` SHALL retornar **false** (negar — não acesso total)
4. WHEN usuário tiver funcionário **com** nó no organograma THEN o port SHALL retornar `temNoOrganograma=true`, `acessoTotal=false` (salvo decisão futura explícita de admin global), e `centrosCustoIds` SHALL conter IDs do nó + descendentes
5. WHEN `acessoTotal=true` for exposto THEN SHALL ocorrer **somente** se produto documentar perfil admin/exceção explícita com flag dedicada — **não** derivado de `Optional.empty()` ou `Set.isEmpty()` ambíguo
6. WHEN Folha, Benefícios Mensais ou Dashboard aplicarem filtro de centro THEN SHALL usar `OrganogramaAcessoPort`, não `OrganogramaAcessoService` concreto nem repositories de organograma
7. WHEN `GET /auth/acesso` for chamado THEN a resposta JSON SHALL refletir os sinais distintos (breaking change controlada no DTO documentada no design); frontend SHALL interpretar `acessoTotal` apenas quando `temFuncionarioVinculado && temNoOrganograma && centrosCustoIds.isEmpty()` **ou** flag admin explícita — nunca quando `!temFuncionarioVinculado`
8. WHEN testes unitários de ACL forem executados THEN SHALL cobrir os três cenários: sem funcionário, com funcionário sem nó, com funcionário e nó — provando negação nos dois primeiros

**Independent Test**: Três usuários fixture — (A) sem funcionário vê lista vazia/bloqueio; (B) funcionário sem nó idem; (C) funcionário com nó vê subset de centros.

---

### P1: Alinhamento frontend mínimo (5A) ⭐ MVP

**User Story**: Como mantenedor do frontend, quero services alinhados a domínios, código órfão removido e páginas sem acoplamento direto ao cliente HTTP, sem reescrever para o layout target das skills FE.

**Why P1**: FE participa da conformidade modular; órfãos (`beneficioService.ts`, `pages/Example`, imports diretos de `api.ts`) contradizem fronteiras.

**Acceptance Criteria**:

1. WHEN `frontend/src/pages` for analisado THEN nenhum arquivo SHALL importar `../../services/api` ou `./api` diretamente — delegação via `*Service.ts` do domínio (mínimo: `FolhaPagamento/index.tsx`, `Funcionarios/index.tsx` migrados)
2. WHEN build (`npm run build`) executar THEN SHALL NOT existir `beneficioService.ts`, `pages/Example/` (se presente), `App.tsx`/`App.css` fora do grafo de `main.tsx` (remover ou integrar)
3. WHEN services existirem THEN SHALL seguir nomenclatura por domínio já usada: `beneficioMensalService`, `tipoBeneficioService`, `folhaPagamentoService` (criar se ausente), `organogramaService`, etc. — um service por agregado REST principal
4. WHEN `AuthContext` ou consumidores de acesso interpretarem `AcessoUsuarioDTO` THEN SHALL usar campos distintos pós-correção ACL (não tratar array vazio como acesso total universal)
5. WHEN build frontend for executado THEN `npm run build` SHALL exit 0; `npm run lint` SHALL ser tratado como **advisory** (dívida brownfield / AD-004) — conformidade modular FE é o checklist (`check-modular-compliance.sh`: greps mandatory + build mandatory + lint advisory), não ESLint verde global; não introduzir rewrite para `src/features/` nem TanStack Query obrigatório
6. WHEN tipos de `Beneficio` legado existirem em `types/index.ts` THEN SHALL ser removidos se não referenciados

**Independent Test**: `npm run build` exit 0; `./diversos/scripts/check-modular-compliance.sh` mandatory PASS; grep `from.*services/api` em `pages/` retorna zero; menu e telas existentes funcionam. (`npm run lint` advisory — ver AD-004 / feature `modular-monolith-fix`.)

---

### P1: `SecurityConfig` — alinhamento de paths (refactor) ⭐ MVP

**User Story**: Como mantenedor de segurança, quero matchers Spring Security alinhados ao `context-path: /api` e rotas reais, removendo regras obsoletas, sem alterar o modelo JWT existente.

**Why P1**: CONCERNS — `/api/beneficios/**` obsoleto; mismatch `/api/` vs controllers sem prefixo pode falhar regras `ADMIN`.

**Acceptance Criteria**:

1. WHEN `SecurityConfig` for revisado THEN matchers SHALL usar paths consistentes com `server.servlet.context-path` (paths relativos ao context **sem** duplicar `/api` onde o framework já aplica context-path, **ou** documentar padrão único escolhido e aplicá-lo a todos os matchers)
2. WHEN matcher `/api/beneficios/**` existir THEN SHALL ser removido (rota inexistente)
3. WHEN rotas `/tipo-beneficio` mutáveis existirem THEN regras `hasRole("ADMIN")` SHALL corresponder aos paths efetivos usados pelos controllers
4. WHEN teste de segurança mínimo for adicionado THEN SHALL provar que POST `/tipo-beneficio` sem role ADMIN retorna 403 e com ADMIN retorna 2xx (MockMvc ou `@WebMvcTest`)
5. WHEN nenhuma feature de auth nova for introduzida THEN login/refresh/permitAll permanecem inalterados em comportamento

**Independent Test**: Matriz manual ou teste MockMvc cobrindo tipo-beneficio ADMIN vs operador; Swagger acessível conforme antes.

---

### P2: ArchUnit — enforcement crescente por domínio

**User Story**: Como mantenedor, quero regras ArchUnit automatizadas que proíbam dependências proibidas entre domínios, crescendo a cada leva migrada (Benefícios → Folha → restante).

**Why P2**: P1 estabelece padrão; P2 torna violações build-breaking.

**Acceptance Criteria**:

1. WHEN `mvn test` executar THEN SHALL incluir testes ArchUnit em `src/test/java` que falham se: (a) domínio X importar `infrastructure` de domínio Y; (b) controller fora de `*.api` injetar repository; (c) Folha/Dashboard importarem repositories de Benefícios ou Organograma
2. WHEN domínio Folha for migrado para pacote `folha.*` THEN ArchUnit SHALL adicionar regra de encapsulamento Folha ↔ Benefícios via ports only
3. WHEN nova leva Cadastros/Organograma migrar THEN regras SHALL expandir sem relaxar regras anteriores
4. WHEN dependência `archunit-junit5` for adicionada THEN versão SHALL ser compatível com Java 17 / Spring Boot 3.2

**Independent Test**: Introduzir import proibido de propósito → build falha; remover → passa.

---

### P2: Migrar domínios Folha e demais (leva 2+)

**User Story**: Como mantenedor, quero continuar a migração 3B movendo Folha e demais domínios para pacotes coesos, mantendo contratos via ports.

**Why P2**: P1 só cobre Benefícios + ports; folha/importação/dashboard permanecem planos até P2.

**Acceptance Criteria**:

1. WHEN Folha for migrada THEN classes (`FolhaPagamento*`, `FolhaTotalizacaoService`, `ImportacaoFolhaAdpService`, `ResumoFolhaPagamento*`) SHALL residir em `br.com.techne.sistemafolha.folha.*` com mesma estrutura de camadas do P1
2. WHEN Organograma for migrado THEN `OrganogramaService`, entidades de nó/vínculo e submodule `acesso` SHALL residir em `organograma.*`; `OrganogramaAcessoPort` permanece superfície pública
3. WHEN Cadastros (Funcionário, Cargo, Centro, Rubrica, Linha) forem migrados THEN cada um MAY share pacote `cadastros.*` ou subpacotes por agregado — decisão no design, mas sem cross-import de `infrastructure` entre agregados consumidores
4. WHEN aplicação iniciar após P2 THEN todas as rotas documentadas em Swagger SHALL permanecer disponíveis
5. WHEN ArchUnit P2 estiver ativo THEN nenhuma violação new/old SHALL existir no main

**Independent Test**: Smoke API por domínio; tree de pacotes reflete bounded contexts.

---

### P2: Logging estruturado por domínio

**User Story**: Como operador em homologação, quero logs atribuíveis ao domínio de origem (Benefícios, Folha, Organograma), para diagnosticar falhas sem grep amplo.

**Why P2**: Princípio 9 (observabilidade por módulo); baixo risco após fronteiras definidas.

**Acceptance Criteria**:

1. WHEN services application layer logarem eventos THEN SHALL incluir campo estruturado `domain=<nome>` (MDC ou prefixo JSON/key-value consistente) — mínimo nos domínios migrados
2. WHEN `OrganogramaAcessoPort` negar acesso THEN SHALL logar em WARN com `domain=organograma`, `usuarioId`, `motivoNegacao` sem expor PII além do ID
3. WHEN configuração existente de logback/log4j for alterada THEN SHALL ser retrocompatível (níveis default inalterados)

**Independent Test**: Executar fluxo ACL negado + totalização; inspecionar logs por `domain=`.

---

### P3: Checklist automatizado de conformidade modular + `ARCHITECTURE.md`

**User Story**: Como QA/agente Verifier, quero checklist BE+FE rastreável e documentação de arquitetura atualizada, para provar conformidade com modular-design-principles ao fechar a feature.

**Why P3**: Success criteria exige evidência; brownfield doc hoje descreve dual model e pacotes planos.

**Acceptance Criteria**:

1. WHEN feature for encerrada THEN SHALL existir `_docs/specs/features/modular-monolith/validation.md` (Verifier TLC) com evidências por AC
2. WHEN checklist BE for executado THEN SHALL verificar: ports cross-domain, zero legado Beneficio, ArchUnit verde, controllers sem repository, pacotes por domínio migrados conforme fase
3. WHEN checklist FE for executado THEN SHALL verificar: zero import `api.ts` em pages, órfãos removidos, interpretação ACL correta
4. WHEN `ARCHITECTURE.md` for atualizado THEN SHALL remover seção "Dual benefit domain"; documentar monólito modular in-process, ports (`BeneficioConsultaPort`, `OrganogramaAcessoPort`), ordem de migração 3B e diagrama de dependências permitidas
5. WHEN script ou target Maven/npm de checklist for criado (opcional) THEN SHALL ser documentado em design/tasks com comando único reproduzível

**Independent Test**: Revisão de `validation.md` + diff de `ARCHITECTURE.md` + execução checklist sem itens falhos.

---

## Edge Cases

- WHEN competência tiver `beneficio_mensal` parcial (alguns funcionários) THEN totalização SHALL somar apenas lançamentos mensais existentes por funcionário (zero para funcionários sem lançamento), sem erro
- WHEN drop da tabela `beneficios` rodar em ambiente que já não a tem THEN migração Flyway SHALL ser idempotente (`DROP TABLE IF EXISTS`)
- WHEN usuário admin de produto precisar acesso global futuro THEN SHALL usar flag/role explícita no port — não reintroduzir `Set` vazio ambíguo
- WHEN nó de organograma não tiver centros de custo mas tiver descendentes com centros THEN ACL SHALL incluir centros dos descendentes (regra 5 existente preservada)
- WHEN `BeneficioConsultaPort` for chamado com competência nula ou lista de folha vazia THEN SHALL retornar valores neutros sem NPE
- WHEN frontend receber 403 por ACL THEN SHALL exibir estado vazio ou mensagem de sem permissão — não lista completa
- WHEN controllers forem refatorados THEN contratos OpenAPI/DTO SHALL permanecer compatíveis (sem campos removidos silenciosamente exceto ACL DTO documentado)
- WHEN migração de pacote quebrar scan Spring THEN aplicação SHALL falhar no startup (fail-fast), não em runtime parcial
- WHEN dados legado existirem apenas em `beneficios` THEN após drop operação SHALL reportar zero benefícios até importação mensal — comportamento esperado, não bug

---

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | MOD-14 (controllers finos preservam `@Valid`); port methods validam IDs não-nulos — rejeitar com exceção de domínio ou empty result conforme contrato |
| Failure / partial-failure states | MOD-02 (port retorna zero, não exceção, se sem dados mensais); MOD-09 (ACL nega explicitamente); migração Flyway idempotente MOD-01 |
| Idempotency / retry | N/A porque refactor estrutural não introduz novos endpoints mutáveis; importações existentes mantêm semântica 409 já entregue |
| Auth boundaries & rate limits | MOD-13 (SecurityConfig paths); MOD-09 (ACL); JWT unchanged — N/A rate limits |
| Concurrency / ordering | N/A porque ports síncronos in-process sem filas; transações permanecem `@Transactional` nos services donos do agregado |
| Data lifecycle / expiry | MOD-01 (drop legado); dados mensais lifecycle já definido em features anteriores — N/A nova política |
| Observability | MOD-22 (logging estruturado P2); MOD-09 logs de negação ACL |
| External-dependency failure | N/A porque monólito in-process sem integrações externas novas nesta feature |
| State-transition integrity | MOD-01 (remoção legado atômica via migration + deploy); soft-delete existente inalterado |

---

## Requirement Traceability

Each requirement gets a unique ID for tracking across design, tasks, and validation.

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MOD-01 | P1: Remover legado Beneficio | Tasks | In Tasks |
| MOD-02 | P1: BeneficioConsultaPort | Tasks | In Tasks |
| MOD-03 | P1: BeneficioConsultaPort | Tasks | In Tasks |
| MOD-04 | P1: Controllers finos | Tasks | In Tasks |
| MOD-05 | P1: Controllers finos | Tasks | In Tasks |
| MOD-06 | P1: Pacotes Benefícios | Tasks | In Tasks |
| MOD-07 | P1: Pacotes Benefícios | Tasks | In Tasks |
| MOD-08 | P1: OrganogramaAcessoPort | Tasks | In Tasks |
| MOD-09 | P1: ACL bug fix | Tasks | In Tasks |
| MOD-10 | P1: OrganogramaAcessoPort | Tasks | In Tasks |
| MOD-11 | P1: FE mínimo 5A | Tasks | In Tasks |
| MOD-12 | P1: FE mínimo 5A | Tasks | In Tasks |
| MOD-13 | P1: SecurityConfig paths | Tasks | In Tasks |
| MOD-14 | P1: Controllers finos | Tasks | In Tasks |
| MOD-15 | P2: ArchUnit enforcement | Tasks | In Tasks |
| MOD-16 | P2: ArchUnit enforcement | Tasks | In Tasks |
| MOD-17 | P2: Migrar Folha | Tasks | In Tasks |
| MOD-18 | P2: Migrar Organograma/Cadastros | Tasks | In Tasks |
| MOD-19 | P2: Migrar demais domínios | Tasks | In Tasks |
| MOD-20 | P2: ArchUnit leva Folha | Tasks | In Tasks |
| MOD-21 | P2: Logging estruturado | Tasks | In Tasks |
| MOD-22 | P2: Logging estruturado | Tasks | In Tasks |
| MOD-23 | P3: validation.md Verifier | Execute | Pending (Verifier) |
| MOD-24 | P3: Checklist BE compliance | Tasks | In Tasks |
| MOD-25 | P3: Checklist FE compliance | Tasks | In Tasks |
| MOD-26 | P3: ARCHITECTURE.md update | Tasks | In Tasks |
| MOD-27 | P1: Remover FE legado | Tasks | In Tasks |
| MOD-28 | P1: Dashboard via port | Tasks | In Tasks |
| MOD-29 | P1: ACL DTO /auth/acesso | Tasks | In Tasks |
| MOD-30 | P3: Script checklist (opcional) | Tasks | In Tasks |

**ID format:** `MOD-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 30 total, 29 mapped to tasks (T1–T32), 1 Verifier-owned (MOD-23) ✅

---

## Success Criteria

How we know the feature is successful:

- [ ] Zero referências a `Beneficio` legado (entity, repo, tabela, fallback, FE service) — MOD-01, MOD-27
- [ ] `FolhaTotalizacaoService` e `DashboardService` consomem apenas `BeneficioConsultaPort` — MOD-02, MOD-03, MOD-28
- [ ] Domínio Benefícios em pacote `beneficios.*` com port público — MOD-06, MOD-07
- [ ] Controllers tocados sem injeção de repository — MOD-04, MOD-05, MOD-14
- [ ] `OrganogramaAcessoPort` implementado; consumidores desacoplados — MOD-08, MOD-10
- [ ] ACL: sem funcionário → negar; sem nó → negar; sinais distintos no DTO — MOD-09, MOD-29
- [ ] Frontend: pages sem `api.ts` direto; órfãos removidos; ACL interpretada corretamente; build verde; lint advisory (AD-004 / checklist) — MOD-11, MOD-12
- [ ] `SecurityConfig` sem matchers obsoletos; paths consistentes — MOD-13
- [ ] ArchUnit verde com regras crescentes pós-P2 — MOD-15, MOD-16, MOD-20
- [ ] Domínios Folha + demais migrados conforme P2 — MOD-17, MOD-18, MOD-19
- [ ] Logs com `domain=` nos módulos migrados — MOD-21, MOD-22
- [ ] `validation.md` Verifier PASS + `ARCHITECTURE.md` atualizado — MOD-23, MOD-26

### Checklist de conformidade — Backend (modular-design-principles)

| Princípio | Evidência exigida | Req |
| --------- | ----------------- | --- |
| 1 Well-defined boundaries | Pacotes por domínio; ports como superfície pública | MOD-06, MOD-08, MOD-17 |
| 2 Composability | Folha/Dashboard funcionam via ports injetados | MOD-02, MOD-03 |
| 3 Independence | Testes unitários com mocks de port, não repos alheios | MOD-02, MOD-09 |
| 4 Individual scale | N/A in-process monolith — sem tuning por módulo nesta fase | N/A |
| 5 Explicit communication | `BeneficioConsultaPort`, `OrganogramaAcessoPort` documentados | MOD-02, MOD-08 |
| 6 Replaceability | Consumidores dependem de interface, não implementação infra | MOD-02, MOD-08 |
| 7 Deployment independence | Monólito único deploy — explícito em ARCHITECTURE.md | MOD-26 |
| 8 State isolation | Sem cross-domain repository injection pós-refactor | MOD-04, MOD-15 |
| 9 Observability | Logging `domain=` | MOD-21 |
| 10 Fail independence | N/A — sem circuit breakers in-process; falhas propagam como hoje | N/A |

### Checklist de conformidade — Frontend (escopo 5A)

| Critério | Evidência exigida | Req |
| -------- | ----------------- | --- |
| Borda HTTP isolada em services | Zero import `api.ts` em `pages/` | MOD-11 |
| Órfãos removidos | Sem `beneficioService.ts`, Example/App mortos | MOD-12, MOD-27 |
| Contrato ACL | UI trata negação vs acesso restrito vs total explícito | MOD-12, MOD-29 |
| Sem rewrite target | Estrutura brownfield `pages/` + `services/` preservada | MOD-11 |
| Build verde; lint advisory | `npm run build` + checklist (lint AD-004 advisory) | MOD-11 |

---

## References

- Brownfield: `_docs/specs/ARCHITECTURE.md`, `_docs/specs/CONCERNS.md`, `_docs/specs/STRUCTURE.md`
- Princípios: `.agents/skills/modular-design-principles/SKILL.md`
- Decomposição: `.agents/skills/modular-decomposition/SKILL.md`
- Features produto já entregues (não reimplementar): `beneficios-mensais`, `alteracao-beneficios-mensais`, `ajuste-beneficios-reorganizacao`
- Harness (escopo separado): `ajuste-harness`
- Decisões projeto: `_docs/specs/STATE.md` (AD-004 skills FE target)
