# Benefícios Mensais — Tasks

## Task Overview

| # | Task | Depends | Parallel | Estimate |
|---|------|---------|----------|----------|
| 1 | Flyway: criar tabela `tipo_beneficio` + seed | — | [P] | S |
| 2 | Flyway: criar tabela `beneficio_mensal` + índices | 1 | [P] | S |
| 3 | Entity + Repository: `TipoBeneficio` | 1 | [P] | S |
| 4 | Entity + Repository: `BeneficioMensal` | 2,3 | — | S |
| 5 | Service + Controller: `TipoBeneficio` CRUD | 3 | [P] | M |
| 6 | Service: `BeneficioMensalService` (CRUD + resumo) | 4 | — | M |
| 7 | Controller: `BeneficioMensalController` (endpoints + organograma) | 6 | — | M |
| 8 | Service: `ImportacaoBeneficioMensalService` (parser xlsx) | 4 | [P] | L |
| 9 | Controller: endpoint importação + duplicidade 409 | 7,8 | — | M |
| 10 | Frontend: services (`tipoBeneficioService`, `beneficioMensalService`) | 7 | [P] | S |
| 11 | Frontend: página `BeneficiosMensais` (resumo + drill-down) | 10 | — | L |
| 12 | Frontend: dialog de importação xlsx | 9,11 | — | M |
| 13 | Ajustar `FolhaTotalizacaoService` para priorizar `beneficio_mensal` | 6 | [P] | M |
| 14 | Rota + menu lateral | 11 | — | S |

**Legenda:** S = small (< 1h), M = medium (1-3h), L = large (3-5h), [P] = paralelizável

---

## Task Definitions

### Task 1: Flyway V1.12 — `tipo_beneficio` + seed

**What:** Criar migration Flyway `V1.12__create_tipo_beneficio.sql` com DDL + INSERT dos 8 tipos iniciais.

**Where:** `backend/src/main/resources/db/migration/V1.12__create_tipo_beneficio.sql`

**Depends on:** Nenhuma

**Reuses:** Padrão de migrations existentes (V1.0 ~ V1.11)

**Done when:**
- Tabela `tipo_beneficio` criada com colunas: id, codigo (UNIQUE), descricao, ativo, data_criacao, data_atualizacao
- 8 registros seed inseridos (códigos da planilha)
- Migration executa sem erro em banco limpo

**Tests:** `flyway migrate` passa sem erro

**Gate:** `./mvnw flyway:migrate -pl backend` exit 0

---

### Task 2: Flyway V1.13 — `beneficio_mensal` + índices

**What:** Criar migration `V1.13__create_beneficio_mensal.sql` com DDL + índices + constraint unique.

**Where:** `backend/src/main/resources/db/migration/V1.13__create_beneficio_mensal.sql`

**Depends on:** Task 1 (FK para tipo_beneficio)

**Reuses:** Padrão `folha_pagamento` (competencia como date range)

**Done when:**
- Tabela `beneficio_mensal` criada com FKs para `funcionarios` e `tipo_beneficio`
- Índices: `idx_beneficio_mensal_competencia`, `idx_beneficio_mensal_func_comp`
- Unique constraint: (funcionario_id, tipo_beneficio_id, competencia_inicio)

**Tests:** `flyway migrate` passa

**Gate:** `./mvnw flyway:migrate -pl backend` exit 0

---

### Task 3: Entity + Repository `TipoBeneficio`

**What:** Criar JPA entity `TipoBeneficio.java` + `TipoBeneficioRepository.java`.

**Where:**
- `backend/src/main/java/br/com/techne/sistemafolha/model/TipoBeneficio.java`
- `backend/src/main/java/br/com/techne/sistemafolha/repository/TipoBeneficioRepository.java`

**Depends on:** Task 1

**Reuses:** Padrão de entities existentes (Lombok @Data, @Entity)

**Done when:**
- Entity mapeia corretamente para `tipo_beneficio`
- Repository extends JpaRepository com queries:
  - `findByCodigoAndAtivoTrue(String codigo)`
  - `findAllByAtivoTrue()`
- Application starta sem erro

**Tests:** Build compila

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 4: Entity + Repository `BeneficioMensal`

**What:** Criar JPA entity `BeneficioMensal.java` + `BeneficioMensalRepository.java`.

**Where:**
- `backend/src/main/java/br/com/techne/sistemafolha/model/BeneficioMensal.java`
- `backend/src/main/java/br/com/techne/sistemafolha/repository/BeneficioMensalRepository.java`

**Depends on:** Tasks 2, 3

**Reuses:** Padrão `FolhaPagamento` entity (ManyToOne funcionario, date range)

**Done when:**
- Entity com @ManyToOne para Funcionario e TipoBeneficio
- Repository queries:
  - `findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(LocalDate, LocalDate)`
  - `findByFuncionarioIdAndCompetenciaInicioAndAtivoTrue(Long, LocalDate)`
  - Resumo agrupado (JPQL com SUM/COUNT GROUP BY tipo)
  - `softDeleteByCompetencia(LocalDate, LocalDate)` — @Modifying

**Tests:** Build compila

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 5: Service + Controller `TipoBeneficio` CRUD

**What:** Criar `TipoBeneficioService` + `TipoBeneficioController` com CRUD restrito a ADMIN para escrita.

**Where:**
- `backend/src/main/java/br/com/techne/sistemafolha/service/TipoBeneficioService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/controller/TipoBeneficioController.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dto/TipoBeneficioDTO.java`

**Depends on:** Task 3

**Reuses:** Padrão FuncionarioService/Controller (soft delete, DTO pattern)

**Done when:**
- GET /tipo-beneficio → lista tipos ativos (qualquer autenticado)
- POST /tipo-beneficio → cria tipo (admin only, valida código unique → 409 se duplicado)
- PUT /tipo-beneficio/{id} → atualiza descrição (admin only)
- DELETE /tipo-beneficio/{id} → soft delete (admin only)
- SecurityConfig atualizado para /tipo-beneficio/** POST/PUT/DELETE = ADMIN

**Tests:** Teste unitário do service (criar, duplicar, desativar)

**Gate:** `./mvnw test -pl backend -Dtest=TipoBeneficioServiceTest` exit 0

---

### Task 6: Service `BeneficioMensalService`

**What:** Criar service com lógica de CRUD, consulta por competência com filtro organograma, e query de resumo agrupado.

**Where:** `backend/src/main/java/br/com/techne/sistemafolha/service/BeneficioMensalService.java`

**Depends on:** Task 4

**Reuses:**
- `OrganogramaAcessoService.obterCentrosCustoAcessiveis()` (mesmo padrão FolhaPagamentoController)
- Pattern de filtro por centros acessíveis

**Done when:**
- `listarPorCompetencia(dataInicio, dataFim, Set<Long> centros)` → List<BeneficioMensalDTO>
- `resumoPorCompetencia(dataInicio, dataFim, Set<Long> centros)` → List<BeneficioMensalResumoDTO> com {codigo, descricao, total, qtdLancamentos}
- `listarPorFuncionario(funcId, dataInicio, dataFim)` → List<BeneficioMensalDTO>
- `criar(dto)` → persiste
- `remover(id)` → soft delete

**Tests:** Teste unitário (mock repository, verificar filtro de centros)

**Gate:** `./mvnw test -pl backend -Dtest=BeneficioMensalServiceTest` exit 0

---

### Task 7: Controller `BeneficioMensalController`

**What:** REST controller com endpoints e integração com organograma.

**Where:**
- `backend/src/main/java/br/com/techne/sistemafolha/controller/BeneficioMensalController.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dto/BeneficioMensalDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dto/BeneficioMensalResumoDTO.java`

**Depends on:** Task 6

**Reuses:** Pattern de `FolhaPagamentoController` (obterCentrosAcessiveis, aplicarFiltroAcesso)

**Done when:**
- GET /beneficio-mensal?competenciaInicio&competenciaFim → lista com filtro organograma
- GET /beneficio-mensal/resumo?competenciaInicio&competenciaFim → resumo agrupado
- GET /beneficio-mensal/funcionario/{id}?competenciaInicio&competenciaFim → detalhe
- POST /beneficio-mensal → criar lançamento
- DELETE /beneficio-mensal/{id} → soft delete
- Todos endpoints aplicam filtro de acesso por organograma

**Tests:** Build compila + endpoint acessível via Swagger

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 8: Service `ImportacaoBeneficioMensalService`

**What:** Parser de arquivo .xlsx usando Apache POI. Lê aba "Lancamentos", valida, persiste.

**Where:**
- `backend/src/main/java/br/com/techne/sistemafolha/service/ImportacaoBeneficioMensalService.java`
- `backend/pom.xml` (adicionar poi-ooxml se necessário)

**Depends on:** Task 4

**Reuses:** Padrão `ImportacaoFolhaAdpService` (verificação duplicidade, CPF lookup, confirm/reject flow)

**Done when:**
- Recebe MultipartFile + competência + confirmar flag
- Lê aba "Lancamentos" (colunas A=CPF, B=Nome, C=Descrição, D=Código, E=Valor)
- Para cada row com CPF preenchido: busca funcionário, busca tipo por código, valida valor ≥ 0
- Se duplicidade (mesma competência já existe) e `confirmar=false` → lança exception (409)
- Se `confirmar=true` → soft delete anteriores + insere novos
- Retorna ImportacaoResultadoDTO {processadas, erros, totalValor, detalhesErros}

**Tests:** Teste unitário com mock workbook

**Gate:** `./mvnw test -pl backend -Dtest=ImportacaoBeneficioMensalServiceTest` exit 0

---

### Task 9: Endpoint de importação

**What:** Adicionar endpoint POST /importacao/beneficios-mensais no controller.

**Where:** `backend/src/main/java/br/com/techne/sistemafolha/controller/BeneficioMensalController.java` (ou ImportacaoController existente)

**Depends on:** Tasks 7, 8

**Done when:**
- POST /importacao/beneficios-mensais aceita multipart/form-data com file + competenciaInicio + competenciaFim + confirmar
- Retorna 200 com ImportacaoResultadoDTO em caso de sucesso
- Retorna 409 com mensagem se duplicidade e confirmar=false
- Retorna 400 se arquivo inválido

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 10: Frontend services

**What:** Criar services axios para tipos e benefícios mensais.

**Where:**
- `frontend/src/services/tipoBeneficioService.ts`
- `frontend/src/services/beneficioMensalService.ts`

**Depends on:** Task 7 (endpoints existem)

**Reuses:** Padrão `folhaPagamentoService.ts` / `funcionarioService.ts`

**Done when:**
- `tipoBeneficioService`: listar(), criar(), atualizar(), remover()
- `beneficioMensalService`: listar(params), resumo(params), porFuncionario(id, params), criar(dto), remover(id), importar(file, competencia, confirmar)

**Gate:** `npm run build` (frontend) sem erro de tipos

---

### Task 11: Página `BeneficiosMensais`

**What:** Implementar page completa com seletor de competência, tabela resumo, drill-down por tipo.

**Where:** `frontend/src/pages/BeneficiosMensais/index.tsx`

**Depends on:** Task 10

**Reuses:**
- Layout padrão Funcionarios (Card header + filtros + tabela)
- Seletor de competência padrão FolhaPagamento
- `formatarDataCompetencia`, `getApiErrorMessage`

**Done when:**
- Seletor mês/ano funcional (default = última competência com dados)
- Tabela resumo: Código | Descrição | Total (R$) | Qtd. Lançamentos
- Click em linha expande mostrando funcionários com valores individuais
- Total geral no rodapé
- Estado vazio quando sem dados
- Aplica filtro de organograma (backend já filtra, frontend só exibe)
- Responsivo (MUI breakpoints)

**Gate:** `npm run build` (frontend) sem erro + visual review

---

### Task 12: Dialog de importação

**What:** Modal para upload de .xlsx com seletor de competência e tratamento de 409.

**Where:** Componente dentro de `frontend/src/pages/BeneficiosMensais/`

**Depends on:** Tasks 9, 11

**Done when:**
- Botão "Importar" no header da página abre dialog
- Dialog: file input (.xlsx), seletores mês/ano, botão "Importar"
- Se 409 → exibir alerta com opção "Substituir" (reenvia com confirmar=true)
- Resultado: toast com "X registros importados, Y erros"
- Refresh automático da tabela após importação

**Gate:** `npm run build` sem erro + teste manual upload

---

### Task 13: Ajustar `FolhaTotalizacaoService`

**What:** Quando existem dados em `beneficio_mensal` para a competência, usar esses valores em vez da tabela `beneficios` legada.

**Where:** `backend/src/main/java/br/com/techne/sistemafolha/service/FolhaTotalizacaoService.java`

**Depends on:** Task 6

**Reuses:** Queries existentes de benefícios

**Done when:**
- Se `beneficioMensalRepository.existsByCompetencia(dataInicio, dataFim)` = true → somar de `beneficio_mensal`
- Caso contrário → manter fallback para `beneficioRepository` (comportamento atual)
- Nunca somar ambos para evitar dupla contagem
- Teste unitário cobre ambos cenários

**Gate:** `./mvnw test -pl backend -Dtest=FolhaTotalizacaoServiceTest` exit 0

---

### Task 14: Rota + menu lateral

**What:** Adicionar rota `/beneficios-mensais` e item no menu sidebar.

**Where:**
- `frontend/src/routes/index.tsx`
- `frontend/src/components/Layout/` (sidebar/menu component)

**Depends on:** Task 11

**Done when:**
- Rota `/beneficios-mensais` renderiza page BeneficiosMensais
- Menu lateral exibe "Benefícios Mensais" próximo a "Folha de Pagamento"
- Navegação funcional

**Gate:** `npm run build` sem erro + navegação manual OK

---

## Execution Order (Critical Path)

```
Phase 1 (parallel):
  Task 1 → Task 3 → Task 5 (tipos: migration → entity → CRUD)
  
Phase 2 (depends on Phase 1):
  Task 2 → Task 4 → Task 6 → Task 7 (mensal: migration → entity → service → controller)
  
Phase 3 (parallel, depends on Task 4):
  Task 8 (import service)
  Task 13 (totalização)
  
Phase 4 (depends on Phase 2+3):
  Task 9 (endpoint importação)
  Task 10 → Task 11 → Task 12 → Task 14 (frontend pipeline)
```

## Status

| Task | Status | Assignee |
|------|--------|----------|
| 1 | Verified | agent |
| 2 | Verified | agent |
| 3 | Verified | agent |
| 4 | Verified | agent |
| 5 | Verified | agent |
| 6 | Verified | agent |
| 7 | Verified | agent |
| 8 | Verified | agent |
| 9 | Verified | agent |
| 10 | Verified | agent |
| 11 | Verified | agent |
| 12 | Verified | agent |
| 13 | Verified | agent |
| 14 | Verified | agent |
