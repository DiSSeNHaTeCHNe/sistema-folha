# 💰 Conhecimento Consolidado: Tela de Folha de Pagamento

## 📌 Visão Geral

A tela de **Folha de Pagamento** é um dos módulos mais críticos do sistema, responsável pela **visualização e consulta** dos dados de folha de pagamento importados. A tela oferece uma visão hierárquica em dois níveis: **Resumos por Competência** (visão macro) e **Funcionários com suas Rubricas** (visão detalhada).

### Objetivo da Tela
- Visualizar resumos consolidados de folha de pagamento por competência (mês/ano)
- Consultar dados detalhados de funcionários e suas rubricas
- Filtrar folhas por múltiplos critérios (funcionário, centro de custo, linha de negócio, período)
- Navegar entre visões agregadas e detalhadas
- Apresentar informações de proventos, descontos e valores líquidos

### Características Principais
- **Visão Dupla**: Resumos gerais e detalhes por funcionário
- **Importação de Dados**: Folhas são populadas via importação de arquivos ADP
- **Consulta Apenas**: Não há criação manual de registros (dados são importados)
- **Soft Delete**: Registros são marcados como inativos, nunca deletados fisicamente
- **Relacionamentos Ricos**: Funcionários, Rubricas, Cargos, Centros de Custo, Linhas de Negócio

---

## 🏗️ Arquitetura da Aplicação

### Stack Tecnológico

#### Backend
- **Framework**: Spring Boot 3.2.3
- **Linguagem**: Java 17
- **ORM**: Spring Data JPA + Hibernate
- **Banco de Dados**: PostgreSQL
- **Tipos Numéricos**: BigDecimal para valores monetários
- **API Doc**: OpenAPI 3 (Swagger)
- **Padrão**: DTOs imutáveis (Records)

#### Frontend
- **Framework**: React 19.1
- **Linguagem**: TypeScript
- **UI Library**: Material-UI (MUI) v7
- **Formulários**: React Hook Form
- **HTTP Client**: Axios
- **Formatação**: Intl.NumberFormat para valores monetários

---

## 📂 Estrutura de Arquivos

### Backend

```
src/main/java/br/com/techne/sistemafolha/
├── controller/
│   ├── FolhaPagamentoController.java          # REST API para itens de folha
│   └── ResumoFolhaPagamentoController.java    # REST API para resumos
├── repository/
│   ├── FolhaPagamentoRepository.java          # Acesso a dados de itens
│   └── ResumoFolhaPagamentoRepository.java    # Acesso a dados de resumos
├── model/
│   ├── FolhaPagamento.java                    # Entidade JPA (itens)
│   └── ResumoFolhaPagamento.java              # Entidade JPA (resumos)
├── dto/
│   ├── FolhaPagamentoDTO.java                 # DTO imutável (itens)
│   └── ResumoFolhaPagamentoDTO.java           # DTO imutável (resumos)
└── service/
    └── (Não há service layer - controllers acessam repositories diretamente)
```

### Frontend

```
frontend/src/
├── pages/
│   └── FolhaPagamento/
│       └── index.tsx                          # Componente principal
├── services/
│   ├── api.ts                                 # Configuração Axios
│   ├── folhaPagamentoService.ts               # Serviço de API (itens)
│   └── resumoFolhaPagamentoService.ts         # Serviço de API (resumos)
└── types/
    └── index.ts                               # Interfaces TypeScript
```

---

## 🔄 Fluxo de Dados Completo

### 1. Listagem de Resumos da Folha (Tela Inicial)

```
Frontend (index.tsx)
    │
    ├─> useEffect: fetchFolha() chamado na montagem
    │
    ├─> resumoFolhaPagamentoService.listarTodos()
    │
    └─> api.get('/resumo-folha-pagamento')
            │
            ├─> Axios Interceptor adiciona JWT
            │
            └─> Backend: ResumoFolhaPagamentoController.listarTodos()
                    │
                    └─> ResumoFolhaPagamentoRepository.findByAtivoTrue()
                            │
                            └─> Retorna List<ResumoFolhaPagamento>
                                    │
                                    └─> Controller.toDTO()
                                            │
                                            └─> Retorna List<ResumoFolhaPagamentoDTO>
                                                    │
                                                    └─> Frontend recebe dados
                                                            │
                                                            ├─> setResumosFolha(resumosOrdenados)
                                                            ├─> Ordena por competenciaInicio DESC
                                                            │
                                                            └─> Renderiza TableContainer com resumos
                                                                    │
                                                                    ├─> Competência
                                                                    ├─> Total Empregados
                                                                    ├─> Total Encargos
                                                                    ├─> Total Pagamentos
                                                                    ├─> Total Descontos
                                                                    ├─> Total Líquido (destaque)
                                                                    ├─> Data Importação
                                                                    └─> Botão "Ver Funcionários"
```

### 2. Navegação para Funcionários de uma Competência

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em "Ver Funcionários" de um resumo
    │
    ├─> handleVerFuncionarios(resumo)
    │       ├─> setResumoSelecionado(resumo)
    │       └─> setMostrarFuncionarios(true)
    │
    ├─> Renderização condicional: mostrarFuncionarios = true
    │
    ├─> Filtra funcionariosResumo pela competência selecionada:
    │   └─> item.dataInicio === resumoSelecionado.competenciaInicio
    │       && item.dataFim === resumoSelecionado.competenciaFim
    │
    └─> Exibe Cards com:
            ├─> Nome do Funcionário
            ├─> Cargo
            ├─> Centro de Custo
            ├─> Linha de Negócio
            ├─> Valor Total (calculado: proventos - descontos)
            └─> Botão "Ver Rubricas"
```

### 3. Detalhamento de Rubricas de um Funcionário

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em "Ver Rubricas" em um card de funcionário
    │
    ├─> handleDetalharRubricas(funcionario)
    │       ├─> setFuncionarioSelecionado(funcionario)
    │       │
    │       ├─> Filtra folha[] (já carregada em memória):
    │       │   └─> funcionarioId === funcionario.funcionarioId
    │       │       && dataInicio === funcionario.dataInicio
    │       │       && dataFim === funcionario.dataFim
    │       │
    │       ├─> setRubricasFuncionario(rubricas)
    │       └─> setOpenDetalhesDialog(true)
    │
    └─> Renderiza Dialog com TableContainer:
            ├─> Rubrica (código + descrição)
            ├─> Tipo (PROVENTO, DESCONTO, INFORMATIVO)
            ├─> Valor (formatado R$)
            ├─> Quantidade
            └─> Base de Cálculo (formatado R$)
```

### 4. Filtros Dinâmicos de Folha

```
Frontend (index.tsx)
    │
    ├─> Usuário preenche filtros (funcionário, centro de custo, linha de negócio, mês, ano)
    │
    ├─> handleSubmitFiltros(handleFiltrarFolha)
    │
    └─> fetchFolha(filtros)
            │
            ├─> getPeriodo(mes, ano) calcula dataInicio e dataFim:
            │   └─> dataInicio = YYYY-MM-01, dataFim = YYYY-MM-31
            │
            ├─> Lógica de Prioridade:
            │   ├─> Se funcionarioId:
            │   │   └─> folhaPagamentoService.buscarPorFuncionario(funcionarioId, dataInicio, dataFim)
            │   │       └─> GET /folha-pagamento/funcionario/{funcionarioId}?dataInicio=&dataFim=
            │   │
            │   ├─> Se centroCustoId:
            │   │   └─> folhaPagamentoService.buscarPorCentroCusto(centroCustoId, dataInicio, dataFim)
            │   │       └─> GET /folha-pagamento/centro-custo/{centroCustoId}?dataInicio=&dataFim=
            │   │
            │   ├─> Se linhaNegocioId:
            │   │   └─> folhaPagamentoService.buscarPorLinhaNegocio(linhaNegocioId, dataInicio, dataFim)
            │   │       └─> GET /folha-pagamento/linha-negocio/{linhaNegocioId}?dataInicio=&dataFim=
            │   │
            │   ├─> Se apenas mes && ano:
            │   │   └─> folhaPagamentoService.buscarPorPeriodo(dataInicio, dataFim)
            │   │       └─> GET /folha-pagamento?dataInicio=&dataFim=
            │   │
            │   └─> Senão:
            │       └─> folhaPagamentoService.listar()
            │           └─> GET /folha-pagamento (todos os registros)
            │
            ├─> Backend: FolhaPagamentoController
            │   ├─> consultarPorFuncionario()
            │   │   └─> FolhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue()
            │   │
            │   ├─> consultarPorCentroCusto()
            │   │   └─> FolhaPagamentoRepository.findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue()
            │   │
            │   ├─> consultarPorLinhaNegocio()
            │   │   └─> FolhaPagamentoRepository.findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue()
            │   │
            │   └─> consultarPorPeriodo()
            │       └─> FolhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue()
            │
            ├─> Retorna List<FolhaPagamentoDTO>
            │
            ├─> Frontend recebe dados
            │
            ├─> setFolha(data)
            │
            └─> Cria resumo por funcionário (reduce):
                    │
                    ├─> Agrupa por: funcionarioId + dataInicio + dataFim
                    │
                    ├─> Calcula:
                    │   ├─> totalRubricas (contagem)
                    │   └─> valorTotal (soma: PROVENTO (+), DESCONTO (-), INFORMATIVO ignorado)
                    │
                    └─> setFuncionariosResumo(resumo ordenado por nome)
```

### 5. Busca Local por Nome

```
Frontend (index.tsx)
    │
    ├─> Usuário digita no campo de busca
    │
    ├─> handleSearch(event)
    │   └─> setSearchTerm(event.target.value)
    │
    ├─> filteredFuncionarios (computed):
    │   ├─> Filtra funcionariosResumo[] por nome (case-insensitive)
    │   │
    │   └─> Se mostrarFuncionarios && resumoSelecionado:
    │       └─> Filtra também por competência
    │
    └─> Renderiza apenas filteredFuncionarios
```

---

## 🔍 Análise Detalhada do Código Backend

### 1. Entidade `FolhaPagamento.java`

```java
@Entity
@Table(name = "folha_pagamento")
public class FolhaPagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne
    @JoinColumn(name = "rubrica_id", nullable = false)
    private Rubrica rubrica;

    @ManyToOne
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @ManyToOne
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne
    @JoinColumn(name = "linha_negocio_id")
    private LinhaNegocio linhaNegocio;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "quantidade", nullable = false)
    private BigDecimal quantidade;

    @Column(name = "base_calculo")
    private BigDecimal baseCalculo;

    @Column(nullable = false)
    private Boolean ativo = true;
}
```

**Características:**
- **Relacionamentos**:
  - `@ManyToOne` com `Funcionario` (obrigatório)
  - `@ManyToOne` com `Rubrica` (obrigatório)
  - `@ManyToOne` com `Cargo` (opcional)
  - `@ManyToOne` com `CentroCusto` (opcional)
  - `@ManyToOne` com `LinhaNegocio` (opcional)

- **Campos de Período**:
  - `dataInicio`: início da competência (ex: 2024-01-01)
  - `dataFim`: fim da competência (ex: 2024-01-31)

- **Campos Monetários** (BigDecimal):
  - `valorTotal`: valor total do item
  - `valor`: valor unitário
  - `quantidade`: multiplicador
  - `baseCalculo`: base para cálculos (ex: salário base)

- **Soft Delete**:
  - Campo `ativo` (Boolean, default `true`)

- **Observações**:
  - Não há validações Jakarta Bean Validation
  - Não há auditoria automática (campos de criação/atualização)
  - Registros são criados via importação, não manualmente

### 2. Entidade `ResumoFolhaPagamento.java`

```java
@Entity
@Table(name = "resumo_folha_pagamento")
public class ResumoFolhaPagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "total_empregados", nullable = false)
    private Integer totalEmpregados;
    
    @Column(name = "total_encargos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEncargos;
    
    @Column(name = "total_pagamentos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPagamentos;
    
    @Column(name = "total_descontos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDescontos;
    
    @Column(name = "total_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalLiquido;
    
    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;
    
    @Column(name = "competencia_fim", nullable = false)
    private LocalDate competenciaFim;
    
    @Column(name = "data_importacao")
    private LocalDateTime dataImportacao;
    
    @Column(name = "ativo")
    private Boolean ativo = true;
}
```

**Características:**
- **Campos Agregados**:
  - `totalEmpregados`: contagem de funcionários
  - `totalEncargos`: soma de encargos
  - `totalPagamentos`: soma de proventos
  - `totalDescontos`: soma de descontos
  - `totalLiquido`: pagamentos - descontos

- **Período**:
  - `competenciaInicio`: início do período (ex: 2024-01-01)
  - `competenciaFim`: fim do período (ex: 2024-01-31)

- **Auditoria**:
  - `dataImportacao`: timestamp da importação (LocalDateTime)

- **Soft Delete**:
  - Campo `ativo` (Boolean, default `true`)

- **Precisão Monetária**:
  - `precision = 15, scale = 2` para valores monetários

### 3. Repository `FolhaPagamentoRepository.java`

```java
@Repository
public interface FolhaPagamentoRepository extends JpaRepository<FolhaPagamento, Long> {
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.funcionario.id = :funcionarioId AND f.dataInicio = :dataInicio AND f.dataFim = :dataFim")
    List<FolhaPagamento> findByFuncionarioAndPeriodo(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.funcionario.centroCusto = :centroCusto AND f.dataInicio = :dataInicio AND f.dataFim = :dataFim")
    List<FolhaPagamento> findByCentroCustoAndPeriodo(CentroCusto centroCusto, LocalDate dataInicio, LocalDate dataFim);
    
    boolean existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
        Long funcionarioId, Long rubricaId, LocalDate dataInicio, LocalDate dataFim);

    List<FolhaPagamento> findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim);
    
    List<FolhaPagamento> findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(CentroCusto centroCusto, LocalDate dataInicio, LocalDate dataFim);
    
    List<FolhaPagamento> findByDataInicioBetweenAndAtivoTrue(LocalDate dataInicio, LocalDate dataFim);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.linhaNegocio = :linhaNegocio AND f.dataInicio BETWEEN :dataInicio AND :dataFim AND f.ativo = true")
    List<FolhaPagamento> findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue(
        @Param("linhaNegocio") LinhaNegocio linhaNegocio, 
        @Param("dataInicio") LocalDate dataInicio, 
        @Param("dataFim") LocalDate dataFim
    );
    
    @Modifying
    @Query("UPDATE FolhaPagamento f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);
    
    List<FolhaPagamento> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.ativo = true AND f.dataInicio = :competenciaInicio AND f.dataFim = :competenciaFim")
    List<FolhaPagamento> findByCompetenciaAndAtivoTrue(@Param("competenciaInicio") LocalDate competenciaInicio, @Param("competenciaFim") LocalDate competenciaFim);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.funcionario.id = :funcionarioId AND f.ativo = true AND f.dataInicio = :competenciaInicio AND f.dataFim = :competenciaFim")
    List<FolhaPagamento> findByFuncionarioIdAndCompetenciaAndAtivoTrue(@Param("funcionarioId") Long funcionarioId, @Param("competenciaInicio") LocalDate competenciaInicio, @Param("competenciaFim") LocalDate competenciaFim);
}
```

**Características:**
- **Métodos Derivados** (Spring Data JPA):
  - `findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue()`: filtra por funcionário e período
  - `findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue()`: navega relacionamento funcionario.centroCusto
  - `findByDataInicioBetweenAndAtivoTrue()`: filtra apenas por período
  - `findByFuncionarioIdAndAtivoTrue()`: todos os registros ativos de um funcionário

- **Queries JPQL Customizadas**:
  - `findByFuncionarioAndPeriodo()`: filtra por funcionário e período exato
  - `findByCentroCustoAndPeriodo()`: filtra por centro de custo e período exato
  - `findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue()`: filtra por linha de negócio e intervalo de datas
  - `findByCompetenciaAndAtivoTrue()`: filtra por competência exata
  - `findByFuncionarioIdAndCompetenciaAndAtivoTrue()`: filtra por funcionário e competência exata

- **Validação de Duplicados**:
  - `existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim()`: verifica se registro já existe

- **Soft Delete**:
  - `softDelete()`: atualiza `ativo = false`

### 4. Repository `ResumoFolhaPagamentoRepository.java`

```java
@Repository
public interface ResumoFolhaPagamentoRepository extends JpaRepository<ResumoFolhaPagamento, Long> {
    
    List<ResumoFolhaPagamento> findByAtivoTrue();
    
    List<ResumoFolhaPagamento> findByCompetenciaInicioBetweenAndAtivoTrue(LocalDate dataInicio, LocalDate dataFim);
    
    Optional<ResumoFolhaPagamento> findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(LocalDate competenciaInicio, LocalDate competenciaFim);
    
    @Query("SELECT r FROM ResumoFolhaPagamento r WHERE r.ativo = true ORDER BY r.dataImportacao DESC")
    List<ResumoFolhaPagamento> findLatestResumos();
    
    @Query("SELECT r FROM ResumoFolhaPagamento r WHERE r.ativo = true ORDER BY r.competenciaInicio DESC")
    List<ResumoFolhaPagamento> findByCompetenciaMaisRecente();
    
    @Query("SELECT r FROM ResumoFolhaPagamento r WHERE r.ativo = true AND r.competenciaInicio >= :dataInicio ORDER BY r.competenciaInicio ASC")
    List<ResumoFolhaPagamento> findUltimos12Meses(@Param("dataInicio") LocalDate dataInicio);
}
```

**Características:**
- **Métodos Derivados**:
  - `findByAtivoTrue()`: lista todos os resumos ativos
  - `findByCompetenciaInicioBetweenAndAtivoTrue()`: filtra por intervalo de competências
  - `findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue()`: busca competência exata

- **Queries JPQL com Ordenação**:
  - `findLatestResumos()`: ordena por `dataImportacao DESC`
  - `findByCompetenciaMaisRecente()`: ordena por `competenciaInicio DESC`
  - `findUltimos12Meses()`: filtra últimos 12 meses e ordena por `competenciaInicio ASC`

### 5. Controller `FolhaPagamentoController.java`

```java
@RestController
@RequestMapping("/folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "API para consulta de folha de pagamento")
public class FolhaPagamentoController {
    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por funcionário")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(funcionarioId, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping("/centro-custo/{centroCustoId}")
    @Operation(summary = "Consulta folha de pagamento ativa por centro de custo")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorCentroCusto(
            @PathVariable Long centroCustoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        CentroCusto centroCusto = centroCustoRepository.findById(centroCustoId)
            .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));
            
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(centroCusto, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping("/linha-negocio/{linhaNegocioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por linha de negócio")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorLinhaNegocio(
            @PathVariable Long linhaNegocioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(linhaNegocioId)
            .orElseThrow(() -> new LinhaNegocioNotFoundException(linhaNegocioId));
            
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue(linhaNegocio, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping
    @Operation(summary = "Consulta folha de pagamento ativa por período (mês/ano)")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        if (dataInicio != null && dataFim != null) {
            List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
                .findByDataInicioBetweenAndAtivoTrue(dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(folha);
        } else {
            // Se não houver filtro, retorna tudo
            List<FolhaPagamentoDTO> folha = folhaPagamentoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(folha);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um registro de folha de pagamento (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return folhaPagamentoRepository.findById(id)
            .map(folha -> {
                folhaPagamentoRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private FolhaPagamentoDTO toDTO(FolhaPagamento folha) {
        return new FolhaPagamentoDTO(
            folha.getId(),
            folha.getFuncionario().getId(),
            folha.getFuncionario().getNome(),
            folha.getRubrica().getId(),
            folha.getRubrica().getCodigo(),
            folha.getRubrica().getDescricao(),
            folha.getRubrica().getTipoRubrica().getDescricao(),
            folha.getCargo() != null ? folha.getCargo().getId() : null,
            folha.getCargo() != null ? folha.getCargo().getDescricao() : null,
            folha.getCentroCusto() != null ? folha.getCentroCusto().getId() : null,
            folha.getCentroCusto() != null ? folha.getCentroCusto().getDescricao() : null,
            folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getId() : null,
            folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getDescricao() : null,
            folha.getDataInicio(),
            folha.getDataFim(),
            folha.getValor(),
            folha.getQuantidade(),
            folha.getBaseCalculo()
        );
    }
}
```

**Características:**
- **Endpoints RESTful**:
  - `GET /folha-pagamento/funcionario/{funcionarioId}`: consulta por funcionário
  - `GET /folha-pagamento/centro-custo/{centroCustoId}`: consulta por centro de custo
  - `GET /folha-pagamento/linha-negocio/{linhaNegocioId}`: consulta por linha de negócio
  - `GET /folha-pagamento`: consulta por período ou todos os registros
  - `DELETE /folha-pagamento/{id}`: soft delete

- **Parâmetros de Query**:
  - `dataInicio` e `dataFim`: obrigatórios nos endpoints específicos, opcionais no endpoint geral

- **Acesso Direto ao Repository**:
  - Não há camada de Service
  - Controller acessa repositories diretamente
  - Lógica de negócio é simples (apenas consultas)

- **Conversão para DTO**:
  - Método `toDTO()` navega relacionamentos
  - Campos opcionais (cargo, centroCusto, linhaNegocio) tratados com ternários

- **Tratamento de Exceções**:
  - `CentroCustoNotFoundException` e `LinhaNegocioNotFoundException` para IDs inválidos

### 6. Controller `ResumoFolhaPagamentoController.java`

```java
@RestController
@RequestMapping("/resumo-folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Resumo Folha de Pagamento", description = "API para consulta de resumos de folha de pagamento")
public class ResumoFolhaPagamentoController {
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @GetMapping
    @Operation(summary = "Lista todos os resumos de folha de pagamento ativos")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarTodos() {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository.findByAtivoTrue()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    @GetMapping("/periodo")
    @Operation(summary = "Consulta resumos por período")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository
            .findByCompetenciaInicioBetweenAndAtivoTrue(dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    @GetMapping("/competencia")
    @Operation(summary = "Consulta resumo por competência específica")
    public ResponseEntity<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim) {
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    @Operation(summary = "Lista os resumos mais recentes")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarMaisRecentes() {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository.findLatestResumos()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    private ResumoFolhaPagamentoDTO toDTO(ResumoFolhaPagamento resumo) {
        return new ResumoFolhaPagamentoDTO(
            resumo.getId(),
            resumo.getTotalEmpregados(),
            resumo.getTotalEncargos(),
            resumo.getTotalPagamentos(),
            resumo.getTotalDescontos(),
            resumo.getTotalLiquido(),
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getDataImportacao(),
            resumo.getAtivo()
        );
    }
}
```

**Características:**
- **Endpoints RESTful**:
  - `GET /resumo-folha-pagamento`: lista todos os resumos ativos
  - `GET /resumo-folha-pagamento/periodo`: consulta por intervalo de competências
  - `GET /resumo-folha-pagamento/competencia`: consulta competência específica
  - `GET /resumo-folha-pagamento/latest`: lista resumos mais recentes

- **Ordenação**:
  - Endpoint `/latest` ordena por `dataImportacao DESC`

- **Tratamento de Ausência**:
  - Endpoint `/competencia` retorna `404 Not Found` se não encontrar

### 7. DTOs

#### `FolhaPagamentoDTO.java`

```java
public record FolhaPagamentoDTO(
    Long id,
    Long funcionarioId,
    String funcionarioNome,
    Long rubricaId,
    String rubricaCodigo,
    String rubricaDescricao,
    String rubricaTipo,
    Long cargoId,
    String cargoDescricao,
    Long centroCustoId,
    String centroCustoDescricao,
    Long linhaNegocioId,
    String linhaNegocioDescricao,
    LocalDate dataInicio,
    LocalDate dataFim,
    BigDecimal valor,
    BigDecimal quantidade,
    BigDecimal baseCalculo
) {}
```

**Características:**
- **Record** (Java 14+): DTO imutável, conciso, thread-safe
- **Campos Relacionados**: IDs e descrições para exibição
- **Campos Monetários**: `valor`, `quantidade`, `baseCalculo` (BigDecimal)
- **Período**: `dataInicio`, `dataFim` (LocalDate)

#### `ResumoFolhaPagamentoDTO.java`

```java
public record ResumoFolhaPagamentoDTO(
    Long id,
    Integer totalEmpregados,
    BigDecimal totalEncargos,
    BigDecimal totalPagamentos,
    BigDecimal totalDescontos,
    BigDecimal totalLiquido,
    LocalDate competenciaInicio,
    LocalDate competenciaFim,
    LocalDateTime dataImportacao,
    Boolean ativo
) {}
```

**Características:**
- **Record** (Java 14+): DTO imutável
- **Campos Agregados**: totais (Integer e BigDecimal)
- **Período**: `competenciaInicio`, `competenciaFim` (LocalDate)
- **Auditoria**: `dataImportacao` (LocalDateTime)

---

## 🎨 Análise Detalhada do Código Frontend

### 1. Componente Principal `index.tsx`

#### Imports e Tipos Locais

```typescript
import { useState, useEffect } from 'react';
import {
  Box, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, Typography, TextField, InputAdornment, Snackbar, Dialog,
  DialogTitle, DialogContent, DialogActions, Card, CardContent,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { Search as SearchIcon, Visibility as VisibilityIcon } from '@mui/icons-material';
import { Controller, useForm } from 'react-hook-form';
import type { FolhaPagamento } from '../../types';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';
import { resumoFolhaPagamentoService, type ResumoFolhaPagamento } from '../../services/resumoFolhaPagamentoService';
import api from '../../services/api';

interface Funcionario {
  id: number;
  nome: string;
}

interface CentroCusto {
  id: number;
  descricao: string;
}

interface LinhaNegocio {
  id: number;
  descricao: string;
}

interface FiltrosFolha {
  funcionarioId: string;
  centroCustoId: string;
  linhaNegocioId: string;
  mes: string;
  ano: string;
}

interface FuncionarioResumo {
  funcionarioId: number;
  funcionarioNome: string;
  dataInicio: string;
  dataFim: string;
  totalRubricas: number;
  valorTotal: number;
  cargoDescricao?: string;
  centroCustoDescricao?: string;
  linhaNegocioDescricao?: string;
}
```

**Observações:**
- Tipos locais para estruturas não provenientes do backend
- `FuncionarioResumo` é uma estrutura agregada calculada no frontend

#### Estados

```typescript
const [searchTerm, setSearchTerm] = useState('');
const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
const [folha, setFolha] = useState<FolhaPagamento[]>([]);
const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
const [resumosFolha, setResumosFolha] = useState<ResumoFolhaPagamento[]>([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState('');
const [openSnackbar, setOpenSnackbar] = useState(false);
const [snackbarMsg, setSnackbarMsg] = useState('');
const [openDialog, setOpenDialog] = useState(false);
const [openDetalhesDialog, setOpenDetalhesDialog] = useState(false);
const [funcionarioSelecionado, setFuncionarioSelecionado] = useState<FuncionarioResumo | null>(null);
const [rubricasFuncionario, setRubricasFuncionario] = useState<FolhaPagamento[]>([]);
const [resumoSelecionado, setResumoSelecionado] = useState<ResumoFolhaPagamento | null>(null);
const [mostrarFuncionarios, setMostrarFuncionarios] = useState(false);
```

**Observações:**
- **Estados Principais**:
  - `folha[]`: todos os itens de folha carregados
  - `funcionariosResumo[]`: agregação por funcionário (calculada no frontend)
  - `resumosFolha[]`: resumos por competência (vindos do backend)
- **Estados de Navegação**:
  - `mostrarFuncionarios`: controla visão atual (resumos vs funcionários)
  - `resumoSelecionado`: competência selecionada para drill-down
- **Estados de Modais**:
  - `openDetalhesDialog`: dialog de rubricas do funcionário
  - `funcionarioSelecionado` e `rubricasFuncionario`: dados do modal

#### Formulário de Filtros

```typescript
const { control, handleSubmit: handleSubmitFiltros, reset: resetFiltros, watch } = useForm<FiltrosFolha>({
  defaultValues: {
    funcionarioId: '',
    centroCustoId: '',
    linhaNegocioId: '',
    mes: '',
    ano: ''
  }
});

const watchedValues = watch();
```

**Observações:**
- React Hook Form para gestão de filtros
- `watch()` permite acesso em tempo real aos valores

#### Cálculo de Período

```typescript
const getPeriodo = (mes?: string, ano?: string) => {
  const mesValue = mes || watchedValues.mes;
  const anoValue = ano || watchedValues.ano;
  
  if (mesValue && anoValue) {
    const m = mesValue.padStart(2, '0');
    const dataInicio = `${anoValue}-${m}-01`;
    const dataFim = `${anoValue}-${m}-31`;
    return { dataInicio, dataFim };
  }
  return { dataInicio: '', dataFim: '' };
};
```

**Observações:**
- Converte `mes` e `ano` para `dataInicio` e `dataFim` no formato ISO
- `padStart(2, '0')` garante formato `MM`

#### Carregamento de Dados Principal

```typescript
const fetchFolha = async (filtros?: FiltrosFolha) => {
  setLoading(true);
  try {
    let data: FolhaPagamento[] = [];
    const { dataInicio, dataFim } = getPeriodo(filtros?.mes, filtros?.ano);
    
    // Buscar dados da folha baseado nos filtros (lógica de prioridade)
    if (filtros?.funcionarioId) {
      data = await folhaPagamentoService.buscarPorFuncionario(Number(filtros.funcionarioId), dataInicio, dataFim);
    } else if (filtros?.centroCustoId) {
      data = await folhaPagamentoService.buscarPorCentroCusto(filtros.centroCustoId, dataInicio, dataFim);
    } else if (filtros?.linhaNegocioId) {
      data = await folhaPagamentoService.buscarPorLinhaNegocio(filtros.linhaNegocioId, dataInicio, dataFim);
    } else if (filtros?.mes && filtros?.ano) {
      data = await folhaPagamentoService.buscarPorPeriodo(dataInicio, dataFim);
    } else {
      data = await folhaPagamentoService.listar();
    }
    
    setFolha(data);
    
    // Buscar todos os resumos da folha
    try {
      const resumos = await resumoFolhaPagamentoService.listarTodos();
      const resumosOrdenados = resumos.sort((a, b) => {
        const dataA = new Date(a.competenciaInicio).getTime();
        const dataB = new Date(b.competenciaInicio).getTime();
        return dataB - dataA;
      });
      setResumosFolha(resumosOrdenados);
    } catch (err) {
      console.log('Nenhum resumo encontrado');
    }
    
    // Criar resumo por funcionário (reduce)
    const resumo = data.reduce((acc, item) => {
      const key = `${item.funcionarioId}-${item.dataInicio}-${item.dataFim}`;
      if (!acc[key]) {
        acc[key] = {
          funcionarioId: item.funcionarioId,
          funcionarioNome: item.funcionarioNome,
          dataInicio: item.dataInicio,
          dataFim: item.dataFim,
          totalRubricas: 0,
          valorTotal: 0,
          cargoDescricao: item.cargoDescricao,
          centroCustoDescricao: item.centroCustoDescricao,
          linhaNegocioDescricao: item.linhaNegocioDescricao,
        };
      }
      acc[key].totalRubricas += 1;
      
      // Calcula o total baseado no tipo da rubrica
      switch (item.rubricaTipo) {
        case 'PROVENTO':
          acc[key].valorTotal += item.valor;
          break;
        case 'DESCONTO':
          acc[key].valorTotal -= item.valor;
          break;
        case 'INFORMATIVO':
          // Ignora rubricas informativas no cálculo
          break;
        default:
          // Fallback: soma o valor
          acc[key].valorTotal += item.valor;
      }
      
      return acc;
    }, {} as Record<string, FuncionarioResumo>);
    
    setFuncionariosResumo(Object.values(resumo).sort((a, b) => 
      a.funcionarioNome.localeCompare(b.funcionarioNome)
    ));
  } catch (err) {
    setError('Erro ao buscar registros');
  } finally {
    setLoading(false);
  }
};
```

**Observações:**
- **Lógica de Prioridade de Filtros**:
  1. Funcionário (mais específico)
  2. Centro de Custo
  3. Linha de Negócio
  4. Período (mes + ano)
  5. Todos os registros (sem filtro)

- **Busca Paralela de Resumos**:
  - `resumoFolhaPagamentoService.listarTodos()` é chamado independente dos filtros
  - Ordenação por `competenciaInicio DESC`

- **Agregação Frontend**:
  - `reduce()` agrupa itens por `funcionarioId-dataInicio-dataFim`
  - Calcula `totalRubricas` (contagem)
  - Calcula `valorTotal`:
    - `PROVENTO`: soma (+)
    - `DESCONTO`: subtrai (-)
    - `INFORMATIVO`: ignora
  - Ordena por nome do funcionário

#### Manipuladores de Eventos

```typescript
const handleFiltrarFolha = async (filtros: FiltrosFolha) => {
  await fetchFolha(filtros);
};

const handleLimparFiltros = () => {
  resetFiltros();
  fetchFolha();
};

const handleDetalharRubricas = async (funcionario: FuncionarioResumo) => {
  setFuncionarioSelecionado(funcionario);
  const rubricas = folha.filter(item => 
    item.funcionarioId === funcionario.funcionarioId &&
    item.dataInicio === funcionario.dataInicio &&
    item.dataFim === funcionario.dataFim
  );
  setRubricasFuncionario(rubricas);
  setOpenDetalhesDialog(true);
};

const handleVerFuncionarios = (resumo: ResumoFolhaPagamento) => {
  setResumoSelecionado(resumo);
  setMostrarFuncionarios(true);
};

const handleVoltarParaResumos = () => {
  setMostrarFuncionarios(false);
  setResumoSelecionado(null);
};
```

**Observações:**
- **handleDetalharRubricas**: filtra `folha[]` em memória (sem nova requisição)
- **handleVerFuncionarios**: alterna para visão de funcionários
- **handleVoltarParaResumos**: volta para visão de resumos

#### Filtragem Local

```typescript
const filteredFuncionarios = funcionariosResumo.filter((item) => {
  const nomeMatch = item.funcionarioNome.toLowerCase().includes(searchTerm.toLowerCase());
  if (mostrarFuncionarios && resumoSelecionado) {
    // Filtra também por competência
    return (
      nomeMatch &&
      item.dataInicio === resumoSelecionado.competenciaInicio &&
      item.dataFim === resumoSelecionado.competenciaFim
    );
  }
  return nomeMatch;
});
```

**Observações:**
- Busca case-insensitive por nome
- Se `mostrarFuncionarios`, filtra também pela competência selecionada

#### Renderização JSX

A renderização é dividida em **duas visões principais**:

##### **Visão 1: Resumos da Folha (Tela Inicial)**

```typescript
{!mostrarFuncionarios && (
  <>
    <Typography variant="h5" gutterBottom sx={{ mb: 2 }}>
      Resumos da Folha de Pagamento
    </Typography>
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Competência</TableCell>
            <TableCell align="right">Total Empregados</TableCell>
            <TableCell align="right">Total Encargos</TableCell>
            <TableCell align="right">Total Pagamentos</TableCell>
            <TableCell align="right">Total Descontos</TableCell>
            <TableCell align="right">Total Líquido</TableCell>
            <TableCell>Data Importação</TableCell>
            <TableCell align="center">Ações</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {resumosFolha.map((resumo) => (
            <TableRow key={resumo.id || Math.random()}>
              <TableCell>
                {formatarDataCompetencia(resumo.competenciaInicio)} a {formatarDataCompetencia(resumo.competenciaFim)}
              </TableCell>
              <TableCell align="right">{resumo.totalEmpregados}</TableCell>
              <TableCell align="right">
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(resumo.totalEncargos)}
              </TableCell>
              <TableCell align="right">
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(resumo.totalPagamentos)}
              </TableCell>
              <TableCell align="right">
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(resumo.totalDescontos)}
              </TableCell>
              <TableCell align="right">
                <Typography color="primary" variant="h6">
                  {new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(resumo.totalLiquido)}
                </Typography>
              </TableCell>
              <TableCell>
                {new Date(resumo.dataImportacao).toLocaleString()}
              </TableCell>
              <TableCell align="center">
                <Button
                  variant="outlined"
                  startIcon={<VisibilityIcon />}
                  onClick={() => handleVerFuncionarios(resumo)}
                  size="small"
                >
                  Ver Funcionários
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  </>
)}
```

**Observações:**
- **TableContainer** com Material-UI
- Formatação monetária com `Intl.NumberFormat`
- `Total Líquido` destacado em `<Typography color="primary" variant="h6">`
- Botão "Ver Funcionários" navega para visão detalhada

##### **Visão 2: Funcionários de uma Competência**

```typescript
{mostrarFuncionarios && (
  <>
    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
      <Button
        variant="outlined"
        onClick={handleVoltarParaResumos}
        sx={{ mr: 2 }}
      >
        ← Voltar para Resumos
      </Button>
      <Typography variant="h5">
        Funcionários - Competência: {resumoSelecionado && (
          `${formatarDataCompetencia(resumoSelecionado.competenciaInicio)} a ${formatarDataCompetencia(resumoSelecionado.competenciaFim)}`
        )}
      </Typography>
    </Box>
    
    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
      {filteredFuncionarios.map((funcionario) => (
        <Card key={`${funcionario.funcionarioId}-${funcionario.dataInicio}`}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              {funcionario.funcionarioNome}
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              Cargo: {funcionario.cargoDescricao || '-'}
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              Centro de Custo: {funcionario.centroCustoDescricao || '-'}
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              Linha de Negócio: {funcionario.linhaNegocioDescricao || '-'}
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              Total: {new Intl.NumberFormat('pt-BR', {
                style: 'currency',
                currency: 'BRL',
              }).format(funcionario.valorTotal)}
            </Typography>
            <Box sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<VisibilityIcon />}
                onClick={() => handleDetalharRubricas(funcionario)}
                fullWidth
              >
                Ver Rubricas
              </Button>
            </Box>
          </CardContent>
        </Card>
      ))}
    </Box>
  </>
)}
```

**Observações:**
- **Grid Responsivo**: `display: grid`, `gridTemplateColumns: repeat(auto-fill, minmax(300px, 1fr))`
- Cards com informações do funcionário
- Botão "Ver Rubricas" abre dialog detalhado

##### **Dialog de Detalhes de Rubricas**

```typescript
<Dialog open={openDetalhesDialog} onClose={() => setOpenDetalhesDialog(false)} maxWidth="lg" fullWidth>
  <DialogTitle>
    Rubricas de {funcionarioSelecionado?.funcionarioNome} - 
    Período: {formatarDataCompetencia(funcionarioSelecionado?.dataInicio || '')} a {formatarDataCompetencia(funcionarioSelecionado?.dataFim || '')}
  </DialogTitle>
  <DialogContent>
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Rubrica</TableCell>
            <TableCell>Tipo</TableCell>
            <TableCell>Valor</TableCell>
            <TableCell>Quantidade</TableCell>
            <TableCell>Base de Cálculo</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rubricasFuncionario.map((item) => (
            <TableRow key={item.id}>
              <TableCell>
                {item.rubricaCodigo} - {item.rubricaDescricao}
              </TableCell>
              <TableCell>
                {item.rubricaTipo}
              </TableCell>
              <TableCell>
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(item.valor)}
              </TableCell>
              <TableCell>{item.quantidade}</TableCell>
              <TableCell>
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(item.baseCalculo)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setOpenDetalhesDialog(false)}>Fechar</Button>
  </DialogActions>
</Dialog>
```

**Observações:**
- `maxWidth="lg" fullWidth` para dialog amplo
- Tabela com todas as rubricas do funcionário
- Formatação monetária consistente

### 2. Serviços Frontend

#### `folhaPagamentoService.ts`

```typescript
import api from './api';
import type { FolhaPagamento } from '../types';

export const folhaPagamentoService = {
  listar: async () => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento');
    return response.data;
  },

  buscarPorFuncionario: async (funcionarioId: number, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/funcionario/${funcionarioId}`, {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  buscarPorCentroCusto: async (centroCusto: string, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/centro-custo/${centroCusto}`, {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  buscarPorLinhaNegocio: async (linhaNegocio: string, dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>(`/folha-pagamento/linha-negocio/${linhaNegocio}`, {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  buscarPorPeriodo: async (dataInicio: string, dataFim: string) => {
    const response = await api.get<FolhaPagamento[]>('/folha-pagamento', {
      params: { dataInicio, dataFim },
    });
    return response.data;
  },

  // Métodos CRUD (não usados na tela de visualização)
  buscarPorId: async (id: number) => { ... },
  criar: async (folhaPagamento: Omit<FolhaPagamento, 'id'>) => { ... },
  atualizar: async (id: number, folhaPagamento: Partial<FolhaPagamento>) => { ... },
  remover: async (id: number) => { ... },
  importar: async (arquivo: File) => { ... },
};
```

**Observações:**
- Camada de abstração sobre `api.ts`
- Tipagem completa com TypeScript
- Parâmetros de query (`dataInicio`, `dataFim`) passados via `params`

#### `resumoFolhaPagamentoService.ts`

```typescript
import api from './api';

export interface ResumoFolhaPagamento {
  id: number;
  totalEmpregados: number;
  totalEncargos: number;
  totalPagamentos: number;
  totalDescontos: number;
  totalLiquido: number;
  competenciaInicio: string;
  competenciaFim: string;
  dataImportacao: string;
  ativo: boolean;
}

const resumoFolhaPagamentoService = {
  listarTodos: async (): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento');
    return response.data;
  },

  buscarPorPeriodo: async (dataInicio: string, dataFim: string): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento/periodo', {
      params: { dataInicio, dataFim }
    });
    return response.data;
  },

  buscarPorCompetencia: async (competenciaInicio: string, competenciaFim: string): Promise<ResumoFolhaPagamento> => {
    const response = await api.get('/resumo-folha-pagamento/competencia', {
      params: { competenciaInicio, competenciaFim }
    });
    return response.data;
  },

  listarMaisRecentes: async (): Promise<ResumoFolhaPagamento[]> => {
    const response = await api.get('/resumo-folha-pagamento/latest');
    return response.data;
  }
};

export { resumoFolhaPagamentoService };
```

**Observações:**
- Interface `ResumoFolhaPagamento` definida localmente
- Métodos para consultas por período e competência específica

---

## 📊 Relacionamentos e Dependências

### Diagrama de Relacionamentos (Entidade `FolhaPagamento`)

```
Funcionario (1) ───< (N) FolhaPagamento
Rubrica (1) ───< (N) FolhaPagamento
Cargo (1) ───< (N) FolhaPagamento [opcional]
CentroCusto (1) ───< (N) FolhaPagamento [opcional]
LinhaNegocio (1) ───< (N) FolhaPagamento [opcional]
```

**Explicação:**
- **FolhaPagamento** pertence a **1 Funcionário** (obrigatório)
- **FolhaPagamento** pertence a **1 Rubrica** (obrigatório)
- **FolhaPagamento** pertence a **1 Cargo** (opcional)
- **FolhaPagamento** pertence a **1 Centro de Custo** (opcional)
- **FolhaPagamento** pertence a **1 Linha de Negócio** (opcional)

### Fluxo de Importação

```
Arquivo ADP (txt/csv)
    │
    └─> API: POST /importacao/importar-adp
            │
            ├─> Parse do arquivo
            ├─> Validações de dados
            ├─> Busca/Criação de entidades relacionadas:
            │   ├─> Funcionario (busca por CPF ou idExterno)
            │   ├─> Rubrica (busca por código)
            │   ├─> Cargo (busca por descrição)
            │   ├─> CentroCusto (busca por descrição)
            │   └─> LinhaNegocio (busca por descrição)
            │
            ├─> Criação de FolhaPagamento[]
            │
            ├─> Cálculo de ResumoFolhaPagamento:
            │   ├─> totalEmpregados (count distinct funcionarioId)
            │   ├─> totalEncargos (sum where tipo = ENCARGO)
            │   ├─> totalPagamentos (sum where tipo = PROVENTO)
            │   ├─> totalDescontos (sum where tipo = DESCONTO)
            │   └─> totalLiquido (pagamentos - descontos)
            │
            └─> Salva no banco de dados
```

---

## 🔒 Validações e Regras de Negócio

### Backend

1. **Validação de Duplicados**:
   - `existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim()` garante unicidade

2. **Soft Delete**:
   - Campo `ativo` nunca permite registros deletados fisicamente
   - Consultas sempre filtram `WHERE ativo = true`

3. **Validação de Entidades Relacionadas**:
   - `CentroCustoNotFoundException` e `LinhaNegocioNotFoundException` para IDs inválidos

4. **Precisão Monetária**:
   - `BigDecimal` para todos os valores monetários
   - `precision = 15, scale = 2` em campos de banco

### Frontend

1. **Cálculo de Totais**:
   - Lógica de soma/subtração baseada em `rubricaTipo`:
     - `PROVENTO`: soma (+)
     - `DESCONTO`: subtrai (-)
     - `INFORMATIVO`: ignora

2. **Ordenação**:
   - Resumos ordenados por `competenciaInicio DESC`
   - Funcionários ordenados por `funcionarioNome ASC`

3. **Filtragem Local**:
   - Busca case-insensitive por nome

---

## 🎨 UX/UI da Tela

### Layout

1. **Cabeçalho**:
   - Título "Folha de Pagamento"

2. **Barra de Filtros**:
   - 5 filtros: Funcionário, Centro de Custo, Linha de Negócio, Mês, Ano
   - Botões "Filtrar" e "Limpar"
   - Layout horizontal (flexbox)

3. **Campo de Busca**:
   - Busca por nome de funcionário (local)
   - Ícone de lupa

4. **Visão 1: Resumos da Folha**:
   - Tabela com:
     - Competência (dataInicio a dataFim)
     - Total Empregados
     - Total Encargos (R$)
     - Total Pagamentos (R$)
     - Total Descontos (R$)
     - **Total Líquido** (R$, destaque)
     - Data Importação
     - Ações: Botão "Ver Funcionários"

5. **Visão 2: Funcionários de uma Competência**:
   - Botão "← Voltar para Resumos"
   - Título com competência selecionada
   - Grid de Cards:
     - Nome do Funcionário
     - Cargo
     - Centro de Custo
     - Linha de Negócio
     - Total (R$)
     - Botão "Ver Rubricas"

6. **Dialog de Rubricas**:
   - Título com nome do funcionário e período
   - Tabela com:
     - Rubrica (código + descrição)
     - Tipo (PROVENTO/DESCONTO/INFORMATIVO)
     - Valor (R$)
     - Quantidade
     - Base de Cálculo (R$)

### Interações

1. **Filtros**:
   - Preenchimento independente
   - Botão "Filtrar" dispara busca
   - Botão "Limpar" limpa formulário e recarrega todos

2. **Navegação entre Visões**:
   - "Ver Funcionários" navega para visão de funcionários
   - "← Voltar para Resumos" retorna para visão inicial

3. **Detalhamento**:
   - "Ver Rubricas" abre dialog modal
   - Dialog é fechado com botão "Fechar"

4. **Busca**:
   - Digitação em tempo real filtra funcionários localmente

### Responsividade

- **Desktop**: Grid de cards com 3-4 colunas
- **Tablet**: Grid de cards com 2 colunas
- **Mobile**: Grid de cards com 1 coluna (empilhados)
- Tabelas com scroll horizontal em telas pequenas

---

## 🐛 Possíveis Problemas e Soluções

### 1. Performance com Grande Volume de Dados

**Problema**: Carregar todos os itens de folha de uma vez pode ser lento.

**Solução Atual**: Filtros reduzem o volume de dados carregados.

**Melhoria**: 
- Implementar paginação no backend
- Lazy loading de rubricas (carregar apenas quando abrir dialog)

### 2. Agregação Frontend vs Backend

**Problema**: Agregação de `funcionariosResumo` é feita no frontend, pode ser lenta com muitos dados.

**Solução Atual**: Funciona bem para volumes médios.

**Melhoria**:
- Criar endpoint backend para agregação por funcionário
- Retornar dados já agregados

### 3. Ausência de Paginação

**Problema**: Tabela de resumos pode ter muitas competências.

**Solução Atual**: Não há paginação.

**Melhoria**:
- Implementar paginação com Material-UI `TablePagination`

### 4. Falta de Validações de Data

**Problema**: Usuário pode digitar mês inválido (ex: 13).

**Solução Atual**: `inputProps={{ min: 1, max: 12 }}` limita entrada.

**Melhoria**:
- Validação com React Hook Form
- Mensagens de erro mais claras

---

## 🚀 Melhorias Futuras Possíveis

### Backend

1. **Paginação**:
   - Adicionar `Pageable` nos endpoints
   - Retornar `Page<FolhaPagamentoDTO>`

2. **Agregação Backend**:
   - Endpoint para resumo por funcionário: `GET /folha-pagamento/resumo-funcionarios`
   - Evitar agregação no frontend

3. **Exportação**:
   - Endpoint para exportar CSV/Excel
   - Usar Apache POI ou OpenCSV

4. **Ordenação Customizada**:
   - Permitir ordenação por qualquer campo
   - Adicionar `@RequestParam Sort sort`

5. **Cache**:
   - Implementar cache para resumos (dados raramente mudam)
   - Usar Spring Cache ou Redis

6. **Service Layer**:
   - Criar `FolhaPagamentoService` para isolar lógica de negócio
   - Desacoplar controller de repository

### Frontend

1. **Paginação**:
   - Componente `TablePagination` do MUI
   - Controle de `page` e `size`

2. **Lazy Loading**:
   - Carregar rubricas apenas ao abrir dialog
   - Reduzir memória ocupada

3. **Filtros Avançados**:
   - Intervalo de competências (data início e data fim)
   - Tipo de rubrica (PROVENTO, DESCONTO, INFORMATIVO)

4. **Gráficos**:
   - Gráfico de evolução mensal de custos (Recharts)
   - Gráfico de distribuição de custos por centro de custo

5. **Exportação**:
   - Botão "Exportar para Excel"
   - Aplicar filtros atuais

6. **Busca Avançada**:
   - Busca por CPF, cargo, centro de custo
   - Autocomplete para seleção de funcionário

7. **Comparação de Competências**:
   - Selecionar duas competências e comparar lado a lado

8. **Detalhes Expandidos**:
   - Expandir linha da tabela para mostrar mais detalhes
   - Usar `TableRow` expandível do MUI

9. **Notificações**:
   - Toast para feedback de ações (filtros aplicados, erros)

10. **Responsividade Avançada**:
    - Visualização mobile otimizada
    - Swipe para navegar entre competências

---

## 📋 Checklist de Implementação

### Backend
- ✅ Entidade `FolhaPagamento` com relacionamentos
- ✅ Entidade `ResumoFolhaPagamento` com agregações
- ✅ Repository com métodos customizados
- ✅ Controller RESTful para folha
- ✅ Controller RESTful para resumos
- ✅ DTOs imutáveis (Records)
- ✅ Soft delete
- ✅ Filtros dinâmicos (funcionário, centro, linha, período)
- ✅ Queries JPQL otimizadas
- ✅ Documentação OpenAPI

### Frontend
- ✅ Componente com visão dupla (resumos e funcionários)
- ✅ Formulário de filtros (React Hook Form)
- ✅ Agregação por funcionário (reduce)
- ✅ Dialog de detalhes de rubricas
- ✅ Integração com API
- ✅ Formatação monetária (Intl.NumberFormat)
- ✅ Layout responsivo
- ✅ Navegação entre visões
- ✅ Busca local por nome
- ✅ Ordenação de dados

---

## 🎯 Conclusão

A tela de **Folha de Pagamento** é um exemplo de **visualização complexa** com navegação hierárquica, implementando:

1. **Backend Simples e Eficiente**:
   - Controllers acessando repositories diretamente (sem service layer)
   - Queries JPQL otimizadas para filtros
   - Soft delete para preservar histórico
   - DTOs imutáveis para segurança

2. **Frontend Rico**:
   - Visão dupla (resumos e funcionários)
   - Agregação de dados no frontend (reduce)
   - Navegação drill-down (resumo → funcionários → rubricas)
   - Material-UI para UI consistente

3. **Integração Completa**:
   - API RESTful bem documentada
   - Comunicação com Axios e interceptors
   - Formatação monetária consistente
   - Feedback visual em todas as ações

4. **Boas Práticas**:
   - Separação de responsabilidades
   - Queries otimizadas
   - Código limpo e manutenível
   - Tipagem completa com TypeScript

Esta tela serve como referência para implementação de **visualizações complexas com navegação hierárquica** no sistema.

---

**Documento criado em:** 16 de outubro de 2025  
**Última atualização:** 16 de outubro de 2025  
**Versão:** 1.0

