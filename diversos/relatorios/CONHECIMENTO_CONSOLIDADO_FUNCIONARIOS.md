# 📋 Conhecimento Consolidado: Tela de Funcionários

## 📌 Visão Geral

A tela de **Funcionários** é um dos principais módulos do Sistema de Folha de Pagamento, responsável pelo gerenciamento completo do cadastro de colaboradores da empresa. Esta tela oferece funcionalidades de CRUD (Create, Read, Update, Delete) com soft delete, filtros avançados, e validações robustas tanto no frontend quanto no backend.

### Objetivo da Tela
- Cadastrar, visualizar, editar e remover funcionários
- Permitir filtragem por múltiplos critérios (nome, cargo, centro de custo, linha de negócio)
- Garantir integridade referencial com Cargo, Centro de Custo e Linha de Negócio
- Manter validações de CPF único por funcionário ativo
- Implementar soft delete para preservar histórico de dados

---

## 🏗️ Arquitetura da Aplicação

### Stack Tecnológico

#### Backend
- **Framework**: Spring Boot 3.2.3
- **Linguagem**: Java 17
- **ORM**: Spring Data JPA + Hibernate
- **Banco de Dados**: PostgreSQL
- **Validação**: Jakarta Validation (Bean Validation)
- **API Doc**: OpenAPI 3 (Swagger)
- **Padrão**: DTOs imutáveis (Records)

#### Frontend
- **Framework**: React 19.1
- **Linguagem**: TypeScript
- **UI Library**: Material-UI (MUI) v7
- **Formulários**: React Hook Form
- **HTTP Client**: Axios
- **Notificações**: React Toastify

---

## 📂 Estrutura de Arquivos

### Backend

```
src/main/java/br/com/techne/sistemafolha/
├── controller/
│   └── FuncionarioController.java         # REST API endpoints
├── service/
│   └── FuncionarioService.java            # Lógica de negócio
├── repository/
│   └── FuncionarioRepository.java         # Acesso a dados (JPA)
├── model/
│   └── Funcionario.java                   # Entidade JPA
├── dto/
│   └── FuncionarioDTO.java                # DTO imutável (Record)
└── exception/
    ├── FuncionarioNotFoundException.java  # Exceção customizada
    └── FuncionarioJaExisteException.java  # Exceção customizada
```

### Frontend

```
frontend/src/
├── pages/
│   └── Funcionarios/
│       └── index.tsx                      # Componente principal
├── services/
│   ├── api.ts                             # Configuração Axios
│   └── funcionarioService.ts              # Serviço de API
└── types/
    └── index.ts                           # Interfaces TypeScript
```

---

## 🔄 Fluxo de Dados Completo

### 1. Listagem de Funcionários

```
Frontend (index.tsx)
    │
    ├─> carregarDados() chamado no useEffect
    │
    └─> api.get('/funcionarios')
            │
            ├─> Axios Interceptor adiciona JWT
            │
            └─> Backend: FuncionarioController.listar()
                    │
                    └─> FuncionarioService.listarTodos()
                            │
                            └─> FuncionarioRepository.findByAtivoTrue()
                                    │
                                    └─> Retorna List<Funcionario>
                                            │
                                            └─> FuncionarioService.toDTO()
                                                    │
                                                    └─> Retorna List<FuncionarioDTO>
                                                            │
                                                            └─> Frontend recebe dados
                                                                    │
                                                                    └─> setFuncionarios(data)
                                                                            │
                                                                            └─> Renderiza Cards
```

### 2. Filtros de Funcionários

```
Frontend (index.tsx)
    │
    ├─> Usuário preenche filtros (nome, cargo, centro de custo, linha de negócio)
    │
    ├─> handleSubmit(handleFiltros)
    │
    └─> api.get('/funcionarios?nome=X&cargoId=Y&centroCustoId=Z&linhaNegocioId=W')
            │
            └─> Backend: FuncionarioController.listar()
                    │
                    ├─> Detecta query params
                    │
                    └─> FuncionarioService.listarComFiltros(nome, cargoId, centroCustoId, linhaNegocioId)
                            │
                            └─> FuncionarioRepository.findByFiltros()
                                    │
                                    ├─> Query JPQL com LEFT JOINs
                                    ├─> ILIKE para busca case-insensitive
                                    ├─> Filtros opcionais (NULL aceito)
                                    │
                                    └─> Retorna List<Funcionario> filtrada
                                            │
                                            └─> FuncionarioService.toDTO()
                                                    │
                                                    └─> Frontend recebe dados filtrados
                                                            │
                                                            └─> setFuncionarios(data)
                                                                    │
                                                                    └─> toast.success(${response.data.length} funcionário(s) encontrado(s))
```

### 3. Cadastro de Funcionário

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em "Novo Funcionário"
    │
    ├─> handleOpen() abre Dialog
    │
    ├─> Usuário preenche formulário:
    │   ├─> Nome (TextField)
    │   ├─> CPF (TextField)
    │   ├─> Data de Admissão (Date)
    │   ├─> Cargo (Select)
    │   ├─> Centro de Custo (Select)
    │   └─> Linha de Negócio (Select disabled - auto-preenchido)
    │
    ├─> handleSubmitEdit(onSubmit)
    │
    ├─> onSubmit() prepara dados:
    │   ├─> Converte IDs de string para number
    │   └─> Valida campos obrigatórios (react-hook-form)
    │
    └─> api.post('/funcionarios', dadosParaEnvio)
            │
            └─> Backend: FuncionarioController.cadastrar()
                    │
                    ├─> Validação Jakarta Bean Validation (@Valid)
                    │
                    └─> FuncionarioService.cadastrar(dto)
                            │
                            ├─> Valida CPF único: existsByCpfAndAtivoTrue()
                            │
                            ├─> Busca Cargo ativo: cargoRepository.findById()
                            │
                            ├─> Busca Centro de Custo ativo: centroCustoRepository.findById()
                            │
                            ├─> toEntity(dto) converte DTO para entidade
                            │
                            ├─> Associa cargo e centro de custo
                            │
                            ├─> funcionarioRepository.save()
                            │   ├─> @PrePersist: dataCriacao e dataAtualizacao
                            │   └─> ativo = true (default)
                            │
                            └─> toDTO() retorna FuncionarioDTO
                                    │
                                    └─> Frontend:
                                            ├─> toast.success('Funcionário cadastrado')
                                            ├─> handleClose() fecha Dialog
                                            └─> carregarDados() atualiza lista
```

### 4. Edição de Funcionário

```
Frontend (index.tsx)
    │
    ├─> Usuário clica no ícone de Editar
    │
    ├─> handleOpen(funcionario) abre Dialog
    │
    ├─> setSelectedFuncionario(funcionario)
    │
    ├─> useEffect detecta open && selectedFuncionario
    │
    ├─> resetEdit(dadosFuncionario) preenche formulário
    │
    ├─> Usuário altera dados
    │
    ├─> handleSubmitEdit(onSubmit)
    │
    └─> api.put('/funcionarios/${selectedFuncionario.id}', dadosParaEnvio)
            │
            └─> Backend: FuncionarioController.atualizar()
                    │
                    └─> FuncionarioService.atualizar(id, dto)
                            │
                            ├─> Busca funcionário ativo: findById(id).filter(ativo)
                            │
                            ├─> Valida CPF único (se alterado):
                            │   └─> Se CPF != CPF original && existsByCpfAndAtivoTrue()
                            │       └─> throw IllegalArgumentException
                            │
                            ├─> Busca Cargo ativo: cargoRepository.findById()
                            │
                            ├─> Busca Centro de Custo ativo: centroCustoRepository.findById()
                            │
                            ├─> Atualiza campos:
                            │   ├─> setNome()
                            │   ├─> setCpf()
                            │   ├─> setDataAdmissao()
                            │   ├─> setIdExterno()
                            │   ├─> setCargo()
                            │   └─> setCentroCusto()
                            │
                            ├─> @PreUpdate: atualiza dataAtualizacao
                            │
                            ├─> funcionarioRepository.save()
                            │
                            └─> toDTO() retorna FuncionarioDTO atualizado
                                    │
                                    └─> Frontend:
                                            ├─> toast.success('Funcionário atualizado')
                                            ├─> handleClose() fecha Dialog
                                            └─> carregarDados() atualiza lista
```

### 5. Exclusão de Funcionário (Soft Delete)

```
Frontend (index.tsx)
    │
    ├─> Usuário clica no ícone de Deletar
    │
    ├─> window.confirm('Tem certeza que deseja excluir este funcionário?')
    │
    └─> api.delete('/funcionarios/${id}')
            │
            └─> Backend: FuncionarioController.remover()
                    │
                    └─> FuncionarioService.remover(id)
                            │
                            ├─> Busca funcionário ativo: findById(id).filter(ativo)
                            │
                            ├─> setAtivo(false) - SOFT DELETE
                            │
                            ├─> @PreUpdate: atualiza dataAtualizacao
                            │
                            ├─> funcionarioRepository.save()
                            │
                            └─> Frontend:
                                    ├─> toast.success('Funcionário excluído')
                                    └─> carregarDados() atualiza lista
```

---

## 🔍 Análise Detalhada do Código Backend

### 1. Entidade `Funcionario.java`

```java
@Entity
@Table(name = "funcionarios")
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O CPF é obrigatório")
    @Size(min = 11, max = 11, message = "O CPF deve ter 11 dígitos")
    @Column(nullable = false, length = 11, unique = true)
    private String cpf;

    @NotNull(message = "A data de admissão é obrigatória")
    @Past(message = "A data de admissão deve ser uma data passada")
    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @NotNull(message = "O cargo é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @NotNull(message = "O centro de custo é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id", nullable = false)
    private CentroCusto centroCusto;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;

    @Column(name = "id_externo", length = 50, unique = true)
    private String idExterno;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
```

**Características:**
- **Relacionamentos**:
  - `@ManyToOne` com `Cargo` (LAZY loading)
  - `@ManyToOne` com `CentroCusto` (LAZY loading)
  - Linha de Negócio é obtida através do Centro de Custo

- **Validações**:
  - Nome: obrigatório, 3-100 caracteres
  - CPF: obrigatório, 11 dígitos, único
  - Data de admissão: obrigatória, deve ser passada
  - Cargo e Centro de Custo: obrigatórios

- **Soft Delete**:
  - Campo `ativo` (Boolean, default `true`)
  - Registros nunca são deletados fisicamente

- **Auditoria**:
  - `dataCriacao`: timestamp de criação (imutável)
  - `dataAtualizacao`: timestamp de última atualização
  - `criadoPor` e `atualizadoPor`: usuários responsáveis
  - Lifecycle callbacks: `@PrePersist` e `@PreUpdate`

- **Integração Externa**:
  - `idExterno`: identificador em sistemas externos (único)

### 2. Repository `FuncionarioRepository.java`

```java
@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findByCentroCustoAndAtivoTrue(CentroCusto centroCusto);
    Optional<Funcionario> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Transactional
    @Query("UPDATE Funcionario f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<Funcionario> findByIdExterno(String idExterno);
    boolean existsByIdExterno(String idExterno);

    List<Funcionario> findByAtivoTrue();
    Optional<Funcionario> findByCpfAndAtivoTrue(String cpf);
    boolean existsByCpfAndAtivoTrue(String cpf);
    Long countByAtivoTrue();
    
    @Query("SELECT f FROM Funcionario f " +
           "LEFT JOIN f.cargo c " +
           "LEFT JOIN f.centroCusto cc " +
           "LEFT JOIN cc.linhaNegocio ln " +
           "WHERE f.ativo = true " +
           "AND (:nomePattern IS NULL OR f.nome ILIKE :nomePattern) " +
           "AND (:cargoId IS NULL OR c.id = :cargoId) " +
           "AND (:centroCustoId IS NULL OR cc.id = :centroCustoId) " +
           "AND (:linhaNegocioId IS NULL OR ln.id = :linhaNegocioId) " +
           "ORDER BY f.nome")
    List<Funcionario> findByFiltros(
        @Param("nomePattern") String nomePattern,
        @Param("cargoId") Long cargoId,
        @Param("centroCustoId") Long centroCustoId,
        @Param("linhaNegocioId") Long linhaNegocioId
    );
}
```

**Características:**
- **Métodos Derivados** (Spring Data JPA):
  - `findByAtivoTrue()`: lista apenas funcionários ativos
  - `findByIdAndAtivoTrue()`: busca por ID se ativo
  - `findByCpfAndAtivoTrue()`: busca por CPF se ativo
  - `existsByCpfAndAtivoTrue()`: valida unicidade de CPF
  - `countByAtivoTrue()`: conta funcionários ativos

- **Query Customizada** (`findByFiltros`):
  - **LEFT JOINs**: `cargo`, `centroCusto`, `linhaNegocio`
  - **Filtro Case-Insensitive**: `ILIKE` para nome
  - **Filtros Opcionais**: parâmetros NULL são ignorados
  - **Ordenação**: por nome (alfabética)
  - **Pattern Matching**: nome é transformado em `%nome%` no Service

- **Soft Delete**:
  - Método `softDelete()` com `@Modifying`
  - Atualiza `ativo = false` sem deletar fisicamente

- **Integração Externa**:
  - `findByIdExterno()` e `existsByIdExterno()` para sincronização

### 3. Service `FuncionarioService.java`

```java
@Service
@RequiredArgsConstructor
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;
    private final CargoRepository cargoRepository;
    private final CentroCustoRepository centroCustoRepository;

    public List<FuncionarioDTO> listarTodos() {
        return funcionarioRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<FuncionarioDTO> listarComFiltros(String nome, Long cargoId, Long centroCustoId, Long linhaNegocioId) {
        // Preparar pattern para busca de nome (case insensitive)
        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome + "%";
        }
        
        return funcionarioRepository.findByFiltros(nomePattern, cargoId, centroCustoId, linhaNegocioId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FuncionarioDTO buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
    }

    @Transactional
    public FuncionarioDTO cadastrar(FuncionarioDTO dto) {
        if (funcionarioRepository.existsByCpfAndAtivoTrue(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um funcionário ativo com este CPF");
        }

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        Funcionario funcionario = toEntity(dto);
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioDTO atualizar(Long id, FuncionarioDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));

        if (!funcionario.getCpf().equals(dto.cpf()) && 
            funcionarioRepository.existsByCpfAndAtivoTrue(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um funcionário ativo com este CPF");
        }

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(dto.idExterno());
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public void remover(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    private FuncionarioDTO toDTO(Funcionario funcionario) {
        return new FuncionarioDTO(
            funcionario.getId(),
            funcionario.getNome(),
            funcionario.getCpf(),
            funcionario.getDataAdmissao(),
            funcionario.getCargo().getId(),
            funcionario.getCargo().getDescricao(),
            funcionario.getCentroCusto().getId(),
            funcionario.getCentroCusto().getDescricao(),
            funcionario.getCentroCusto().getLinhaNegocio().getId(),
            funcionario.getCentroCusto().getLinhaNegocio().getDescricao(),
            funcionario.getIdExterno(),
            funcionario.getAtivo()
        );
    }

    private Funcionario toEntity(FuncionarioDTO dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(dto.idExterno());
        funcionario.setAtivo(true);
        return funcionario;
    }
}
```

**Características:**
- **Validações de Negócio**:
  - CPF único: verifica `existsByCpfAndAtivoTrue()`
  - Cargo ativo: busca e valida `cargo.isAtivo()`
  - Centro de Custo ativo: busca e valida `centroCusto.getAtivo()`
  - Funcionário ativo: filtra `filter(Funcionario::getAtivo)`

- **Transações**:
  - `@Transactional` em operações de escrita (cadastrar, atualizar, remover)
  - Rollback automático em caso de exceção

- **Conversões**:
  - `toDTO()`: converte entidade JPA para DTO imutável
    - Navega relacionamentos: `cargo`, `centroCusto`, `linhaNegocio`
    - Retorna descrições além de IDs para exibição
  - `toEntity()`: converte DTO para entidade JPA
    - Associações são feitas separadamente no método `cadastrar()`

- **Filtros**:
  - `listarComFiltros()`: prepara `nomePattern` com `%nome%` para ILIKE
  - Parâmetros opcionais: apenas filtros não-nulos são aplicados

- **Soft Delete**:
  - `remover()`: não deleta fisicamente, apenas `setAtivo(false)`

### 4. Controller `FuncionarioController.java`

```java
@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionários", description = "API para gerenciamento de funcionários")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    @GetMapping
    @Operation(summary = "Lista todos os funcionários ativos com filtros opcionais")
    public ResponseEntity<List<FuncionarioDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long linhaNegocioId) {
        
        // Se algum filtro foi fornecido, usar o método de filtros
        if (nome != null || cargoId != null || centroCustoId != null || linhaNegocioId != null) {
            return ResponseEntity.ok(funcionarioService.listarComFiltros(nome, cargoId, centroCustoId, linhaNegocioId));
        }
        
        // Caso contrário, usar o método básico
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário ativo pelo ID")
    public ResponseEntity<FuncionarioDTO> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(funcionarioService.buscarPorId(id));
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo funcionário")
    public ResponseEntity<FuncionarioDTO> cadastrar(@Valid @RequestBody FuncionarioDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioService.cadastrar(dto));
        } catch (FuncionarioJaExisteException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um funcionário existente")
    public ResponseEntity<FuncionarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioService.atualizar(id, dto));
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um funcionário (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            funcionarioService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Características:**
- **Endpoints RESTful**:
  - `GET /funcionarios`: lista todos ou com filtros
  - `GET /funcionarios/{id}`: busca por ID
  - `POST /funcionarios`: cria novo funcionário
  - `PUT /funcionarios/{id}`: atualiza funcionário
  - `DELETE /funcionarios/{id}`: soft delete

- **Validação**:
  - `@Valid`: dispara validação Bean Validation no DTO

- **Tratamento de Exceções**:
  - `FuncionarioNotFoundException`: retorna `404 Not Found`
  - `FuncionarioJaExisteException`: retorna `400 Bad Request`
  - `IllegalArgumentException`: retorna erro (tratado pelo handler global)

- **Query Parameters**:
  - `@RequestParam(required = false)`: filtros opcionais
  - Lógica no controller decide qual método do service chamar

- **Documentação**:
  - `@Tag`: agrupa endpoints no Swagger
  - `@Operation`: descreve cada endpoint

### 5. DTO `FuncionarioDTO.java`

```java
@Schema(description = "DTO para Funcionário")
public record FuncionarioDTO(
    @Schema(description = "Identificador único do funcionário", example = "1")
    Long id,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome completo do funcionário", example = "João da Silva", required = true)
    String nome,

    @NotBlank(message = "O CPF é obrigatório")
    @Size(min = 11, max = 11, message = "O CPF deve ter 11 dígitos")
    @Schema(description = "CPF do funcionário (apenas números)", example = "12345678900", required = true)
    String cpf,

    @NotNull(message = "A data de admissão é obrigatória")
    @Past(message = "A data de admissão deve ser uma data passada")
    @Schema(description = "Data de admissão do funcionário", example = "2024-01-01", required = true)
    LocalDate dataAdmissao,

    @NotNull(message = "O cargo é obrigatório")
    @Schema(description = "ID do cargo do funcionário", example = "1", required = true)
    Long cargoId,

    @Schema(description = "Descrição do cargo do funcionário", example = "Analista de Sistemas")
    String cargoDescricao,

    @NotNull(message = "O centro de custo é obrigatório")
    @Schema(description = "ID do centro de custo do funcionário", example = "1", required = true)
    Long centroCustoId,

    @Schema(description = "Descrição do centro de custo do funcionário", example = "Tecnologia da Informação")
    String centroCustoDescricao,

    @Schema(description = "ID da linha de negócio", example = "1")
    Long linhaNegocioId,

    @Schema(description = "Descrição da linha de negócio", example = "Desenvolvimento de Software")
    String linhaNegocioDescricao,

    @Schema(description = "ID externo do funcionário", example = "12345")
    String idExterno,

    @Schema(description = "Indica se o funcionário está ativo", example = "true")
    Boolean ativo
) {}
```

**Características:**
- **Record** (Java 14+): DTO imutável, conciso, thread-safe
- **Validações Jakarta Bean Validation**:
  - `@NotBlank`: nome e CPF não podem ser vazios
  - `@Size`: limites de caracteres
  - `@NotNull`: campos obrigatórios
  - `@Past`: data de admissão deve ser passada
- **Documentação OpenAPI**: `@Schema` para cada campo
- **Campos de Relacionamento**:
  - IDs: `cargoId`, `centroCustoId`, `linhaNegocioId`
  - Descrições: para exibição no frontend

---

## 🎨 Análise Detalhada do Código Frontend

### 1. Componente Principal `index.tsx`

#### Imports e Tipos

```typescript
import { useEffect, useState } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions,
  DialogContent, DialogTitle, FormControl, IconButton,
  InputLabel, MenuItem, Select, TextField, Typography,
  FormHelperText,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import api from "../../services/api";
import { toast } from 'react-toastify';

interface FuncionarioLocal {
  id: number;
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId: number;
  cargoDescricao: string;
  centroCustoId: number;
  centroCustoDescricao: string;
  linhaNegocioId: number;
  linhaNegocioDescricao: string;
  idExterno?: string;
  ativo: boolean;
}

interface Cargo {
  id: number;
  descricao: string;
  ativo: boolean;
}

interface CentroCusto {
  id: number;
  descricao: string;
  ativo: boolean;
  linhaNegocioId: number;
}

interface LinhaNegocio {
  id: number;
  descricao: string;
}

interface Filtros {
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId: string;
  centroCustoId: string;
  linhaNegocioId: string;
}
```

**Observações:**
- Interface `FuncionarioLocal` espelha o DTO backend
- Tipos locais para `Cargo`, `CentroCusto`, `LinhaNegocio`
- Interface `Filtros` para o formulário de filtros
- Material-UI para UI consistente

#### Estados e Formulários

```typescript
const [funcionarios, setFuncionarios] = useState<FuncionarioLocal[]>([]);
const [cargos, setCargos] = useState<Cargo[]>([]);
const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
const [open, setOpen] = useState(false);
const [selectedFuncionario, setSelectedFuncionario] = useState<FuncionarioLocal | null>(null);

// Formulário de filtros
const { register, handleSubmit, reset, control } = useForm<Filtros>({
  defaultValues: {
    nome: '', cpf: '', dataAdmissao: '',
    cargoId: '', centroCustoId: '', linhaNegocioId: ''
  }
});

// Formulário de criação/edição
const { 
  register: registerEdit, 
  handleSubmit: handleSubmitEdit, 
  reset: resetEdit, 
  control: controlEdit, 
  setValue: setValueEdit 
} = useForm({
  defaultValues: {
    nome: '', cpf: '', dataAdmissao: '',
    cargoId: '', centroCustoId: '', linhaNegocioId: ''
  }
});
```

**Observações:**
- **Dois formulários independentes**:
  - Um para filtros (barra de pesquisa)
  - Outro para criar/editar (Dialog)
- **React Hook Form**:
  - Gerencia estado do formulário
  - Validações embutidas
  - `setValue` para atualização programática

#### Carregamento de Dados

```typescript
useEffect(() => {
  carregarDados();
  carregarTodosCargos();
  carregarTodosCentrosCusto();
}, []);

const carregarDados = async () => {
  try {
    const [funcionariosRes, linhasNegocioRes] = await Promise.all([
      api.get('/funcionarios'),
      api.get('/linhas-negocio'),
    ]);
    setFuncionarios(funcionariosRes.data);
    setLinhasNegocio(linhasNegocioRes.data);
  } catch (error) {
    toast.error('Erro ao carregar dados');
  }
};

const carregarTodosCargos = async () => {
  try {
    const response = await api.get('/cargos');
    setCargos(response.data);
  } catch (error) {
    toast.error('Erro ao carregar cargos');
  }
};

const carregarTodosCentrosCusto = async () => {
  try {
    const response = await api.get('/centros-custo');
    setCentrosCusto(response.data);
  } catch (error) {
    toast.error('Erro ao carregar centros de custo');
  }
};
```

**Observações:**
- **Promise.all**: carrega funcionários e linhas de negócio em paralelo
- Carrega cargos e centros de custo separadamente
- Toast para feedback de erros

#### Sincronização do Formulário de Edição

```typescript
useEffect(() => {
  if (open && selectedFuncionario) {
    // Modo edição: preenche com dados do funcionário
    const dadosFuncionario = {
      nome: selectedFuncionario.nome || '',
      cpf: selectedFuncionario.cpf || '',
      dataAdmissao: selectedFuncionario.dataAdmissao || '',
      linhaNegocioId: selectedFuncionario.linhaNegocioId?.toString() || '',
      cargoId: selectedFuncionario.cargoId?.toString() || '',
      centroCustoId: selectedFuncionario.centroCustoId?.toString() || ''
    };
    resetEdit(dadosFuncionario);
  } else if (open && !selectedFuncionario) {
    // Modo criação: formulário vazio
    const dadosVazios = {
      nome: '', cpf: '', dataAdmissao: '',
      linhaNegocioId: '', cargoId: '', centroCustoId: ''
    };
    resetEdit(dadosVazios);
  }
}, [open, selectedFuncionario]);
```

**Observações:**
- Sincroniza formulário com `selectedFuncionario`
- `resetEdit()` preenche ou limpa o formulário
- Conversão de IDs para strings (compatibilidade com `<Select>`)

#### Manipuladores de Eventos

```typescript
const handleOpen = (funcionario?: FuncionarioLocal) => {
  setSelectedFuncionario(funcionario || null);
  setOpen(true);
};

const handleClose = () => {
  setOpen(false);
  setSelectedFuncionario(null);
  resetEdit({
    nome: '', cpf: '', dataAdmissao: '',
    linhaNegocioId: '', cargoId: '', centroCustoId: ''
  });
};

const onSubmit = async (data: any) => {
  try {
    // Converter strings para números onde necessário
    const dadosParaEnvio = {
      ...data,
      cargoId: data.cargoId ? Number(data.cargoId) : undefined,
      centroCustoId: data.centroCustoId ? Number(data.centroCustoId) : undefined,
      linhaNegocioId: data.linhaNegocioId ? Number(data.linhaNegocioId) : undefined
    };

    if (selectedFuncionario) {
      await api.put(`/funcionarios/${selectedFuncionario.id}`, dadosParaEnvio);
      toast.success('Funcionário atualizado com sucesso');
    } else {
      await api.post('/funcionarios', dadosParaEnvio);
      toast.success('Funcionário cadastrado com sucesso');
    }
    handleClose();
    carregarDados();
  } catch (error) {
    toast.error('Erro ao salvar funcionário');
  }
};

const handleDelete = async (id: number) => {
  if (window.confirm('Tem certeza que deseja excluir este funcionário?')) {
    try {
      await api.delete(`/funcionarios/${id}`);
      toast.success('Funcionário excluído com sucesso');
      carregarDados();
    } catch (error) {
      toast.error('Erro ao excluir funcionário');
    }
  }
};

const handleFiltros = async (filtros: Filtros) => {
  try {
    const params = new URLSearchParams();
    if (filtros.nome) params.append('nome', filtros.nome);
    if (filtros.cargoId && filtros.cargoId !== '') params.append('cargoId', filtros.cargoId);
    if (filtros.centroCustoId && filtros.centroCustoId !== '') params.append('centroCustoId', filtros.centroCustoId);
    if (filtros.linhaNegocioId && filtros.linhaNegocioId !== '') params.append('linhaNegocioId', filtros.linhaNegocioId);

    const response = await api.get(`/funcionarios?${params.toString()}`);
    setFuncionarios(response.data);
    toast.success(`${response.data.length} funcionário(s) encontrado(s)`);
  } catch (error) {
    toast.error('Erro ao filtrar funcionários');
  }
};
```

**Observações:**
- **onSubmit**: 
  - Converte strings para números (IDs)
  - Detecta modo (edição vs criação) baseado em `selectedFuncionario`
  - Chama API apropriada (PUT vs POST)
- **handleDelete**: 
  - Confirmação com `window.confirm`
  - Soft delete no backend
- **handleFiltros**: 
  - Constrói query string com `URLSearchParams`
  - Apenas filtros preenchidos são incluídos

#### Renderização JSX

```typescript
return (
  <Box p={3}>
    {/* Cabeçalho */}
    <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
      <Typography variant="h4">Funcionários</Typography>
      <Button
        variant="contained"
        color="primary"
        startIcon={<AddIcon />}
        onClick={() => handleOpen()}
      >
        Novo Funcionário
      </Button>
    </Box>

    {/* Formulário de Filtros */}
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <form onSubmit={handleSubmit(handleFiltros)}>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <Box flex="1" minWidth="250px">
              <TextField fullWidth label="Nome" {...register('nome')} />
            </Box>
            <Box flex="1" minWidth="250px">
              <Controller
                name="linhaNegocioId"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Linha de Negócio</InputLabel>
                    <Select {...field} label="Linha de Negócio">
                      <MenuItem value="">Todas</MenuItem>
                      {linhasNegocio.map((linha) => (
                        <MenuItem key={linha.id} value={linha.id.toString()}>
                          {linha.descricao}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              />
            </Box>
            {/* ... outros filtros ... */}
            <Box width="100%">
              <Button type="submit" variant="contained" color="primary" sx={{ mr: 2 }}>
                Filtrar
              </Button>
              <Button 
                variant="outlined" 
                color="secondary"
                onClick={() => {
                  reset();
                  carregarDados();
                }}
              >
                Limpar Filtros
              </Button>
            </Box>
          </Box>
        </form>
      </CardContent>
    </Card>

    {/* Cards de Funcionários */}
    <Box display="flex" flexWrap="wrap" gap={2}>
      {funcionarios.map((funcionario) => (
        <Box key={funcionario.id} flex="1" minWidth="300px" maxWidth="400px">
          <Card>
            <CardContent>
              <Typography variant="h6">{funcionario.nome}</Typography>
              <Typography color="textSecondary">CPF: {funcionario.cpf}</Typography>
              <Typography color="textSecondary">
                Data de Admissão: {formatarDataCompetencia(funcionario.dataAdmissao)}
              </Typography>
              <Typography color="textSecondary">
                Cargo: {funcionario.cargoDescricao || 'N/A'}
              </Typography>
              <Typography color="textSecondary">
                Centro de Custo: {funcionario.centroCustoDescricao || 'N/A'}
              </Typography>
              <Typography color="textSecondary">
                Linha de Negócio: {funcionario.linhaNegocioDescricao || 'N/A'}
              </Typography>
              <Box display="flex" justifyContent="flex-end" mt={2}>
                <IconButton onClick={() => handleOpen(funcionario)}>
                  <EditIcon />
                </IconButton>
                <IconButton onClick={() => handleDelete(funcionario.id)}>
                  <DeleteIcon />
                </IconButton>
              </Box>
            </CardContent>
          </Card>
        </Box>
      ))}
    </Box>

    {/* Dialog de Criação/Edição */}
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {selectedFuncionario ? 'Editar Funcionário' : 'Novo Funcionário'}
      </DialogTitle>
      <form onSubmit={handleSubmitEdit(onSubmit)}>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2}>
            <TextField
              fullWidth
              label="Nome"
              {...registerEdit('nome', { required: true })}
            />
            <TextField
              fullWidth
              label="CPF"
              {...registerEdit('cpf', { required: true })}
            />
            <TextField
              fullWidth
              label="Data de Admissão"
              type="date"
              InputLabelProps={{ shrink: true }}
              {...registerEdit('dataAdmissao', { required: true })}
            />
            <Controller
              name="cargoId"
              control={controlEdit}
              rules={{ required: 'Cargo é obrigatório' }}
              render={({ field }) => (
                <FormControl fullWidth>
                  <InputLabel>Cargo</InputLabel>
                  <Select
                    {...field}
                    label="Cargo"
                    onChange={(e) => {
                      field.onChange(e);
                      // Limpar centro de custo e linha de negócio ao mudar cargo
                      if (!selectedFuncionario) {
                        setValueEdit('centroCustoId', '');
                        setValueEdit('linhaNegocioId', '');
                      }
                    }}
                  >
                    {cargos.map((cargo) => (
                      <MenuItem key={cargo.id} value={cargo.id.toString()}>
                        {cargo.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            />
            <Controller
              name="centroCustoId"
              control={controlEdit}
              rules={{ required: 'Centro de Custo é obrigatório' }}
              render={({ field }) => (
                <FormControl fullWidth>
                  <InputLabel>Centro de Custo</InputLabel>
                  <Select
                    {...field}
                    label="Centro de Custo"
                    onChange={(e) => {
                      field.onChange(e);
                      // Atualizar linha de negócio automaticamente
                      const centroSelecionado = centrosCusto.find(c => c.id === Number(e.target.value));
                      if (centroSelecionado && centroSelecionado.linhaNegocioId) {
                        setValueEdit('linhaNegocioId', centroSelecionado.linhaNegocioId.toString());
                      }
                    }}
                  >
                    {centrosCusto.map((centro) => (
                      <MenuItem key={centro.id} value={centro.id.toString()}>
                        {centro.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                  <FormHelperText>
                    A seleção do centro de custo determina automaticamente a linha de negócio
                  </FormHelperText>
                </FormControl>
              )}
            />
            <Controller
              name="linhaNegocioId"
              control={controlEdit}
              render={({ field }) => (
                <FormControl fullWidth>
                  <InputLabel>Linha de Negócio (Informativo)</InputLabel>
                  <Select
                    {...field}
                    label="Linha de Negócio (Informativo)"
                    disabled={true}
                  >
                    {linhasNegocio.map((linha) => (
                      <MenuItem key={linha.id} value={linha.id.toString()}>
                        {linha.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancelar</Button>
          <Button type="submit" variant="contained" color="primary">
            Salvar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  </Box>
);
```

**Observações:**
- **Layout Responsivo**:
  - `display="flex"` e `flexWrap="wrap"` para adaptação
  - `minWidth` e `maxWidth` para cards
- **Material-UI Components**:
  - `TextField` para inputs simples
  - `Select` com `Controller` para dropdowns
  - `Dialog` para modal de criação/edição
- **Lógica de Dependência**:
  - Ao selecionar Centro de Custo, Linha de Negócio é auto-preenchida
  - Linha de Negócio é `disabled` (apenas informativo)
- **Validações**:
  - `required: true` no `registerEdit`
  - `rules={{ required: 'Mensagem' }}` no `Controller`
- **UX**:
  - Botão "Limpar Filtros" recarrega todos os funcionários
  - Confirmação para deletar
  - Toasts para feedback

### 2. Serviço Frontend `funcionarioService.ts`

```typescript
import api from './api';
import type { Funcionario } from '../types';

export const funcionarioService = {
  listar: async () => {
    const response = await api.get<Funcionario[]>('/funcionarios');
    return response.data;
  },

  buscarPorId: async (id: number) => {
    const response = await api.get<Funcionario>(`/funcionarios/${id}`);
    return response.data;
  },

  criar: async (funcionario: Omit<Funcionario, 'id'>) => {
    const response = await api.post<Funcionario>('/funcionarios', funcionario);
    return response.data;
  },

  atualizar: async (id: number, funcionario: Partial<Funcionario>) => {
    const response = await api.put<Funcionario>(`/funcionarios/${id}`, funcionario);
    return response.data;
  },

  remover: async (id: number) => {
    await api.delete(`/funcionarios/${id}`);
  },
};
```

**Observações:**
- Camada de abstração sobre `api.ts`
- Tipagem completa com TypeScript
- `Omit<Funcionario, 'id'>` para criação (id é gerado pelo backend)
- `Partial<Funcionario>` para atualização (permite campos opcionais)

---

## 🔒 Validações e Regras de Negócio

### Backend

1. **Validações Jakarta Bean Validation**:
   - `@NotBlank`: nome e CPF obrigatórios
   - `@Size`: limites de caracteres
   - `@NotNull`: campos obrigatórios
   - `@Past`: data de admissão deve ser passada

2. **Validações Customizadas** (Service):
   - CPF único: `existsByCpfAndAtivoTrue()`
   - Cargo ativo: `cargoRepository.findById().filter(isAtivo())`
   - Centro de Custo ativo: `centroCustoRepository.findById().filter(getAtivo())`

3. **Integridade Referencial**:
   - `@ManyToOne` com `@JoinColumn(nullable = false)`
   - Foreign keys no banco de dados

4. **Soft Delete**:
   - Campo `ativo` nunca permite registros deletados fisicamente
   - Consultas sempre filtram `WHERE ativo = true`

### Frontend

1. **Validações React Hook Form**:
   - `required: true`: campos obrigatórios
   - `rules={{ required: 'Mensagem' }}`: validações customizadas

2. **Validações de UX**:
   - Confirmação para deletar
   - Linha de Negócio auto-preenchida (não editável)
   - Conversão de strings para números antes de enviar

3. **Feedback Visual**:
   - Toasts para sucesso/erro
   - Mensagens de validação no formulário

---

## 🎨 UX/UI da Tela

### Layout

1. **Cabeçalho**:
   - Título "Funcionários"
   - Botão "Novo Funcionário" (destaque)

2. **Filtros**:
   - Card destacado no topo
   - 4 filtros: Nome, Linha de Negócio, Cargo, Centro de Custo
   - Botões "Filtrar" e "Limpar Filtros"
   - Layout responsivo (flexWrap)

3. **Lista de Funcionários**:
   - Cards em grid responsivo
   - Informações exibidas:
     - Nome (destaque)
     - CPF
     - Data de Admissão (formatada)
     - Cargo
     - Centro de Custo
     - Linha de Negócio
   - Ações: Editar e Deletar (ícones)

4. **Dialog de Criação/Edição**:
   - Modal centralizado
   - Campos:
     - Nome (TextField)
     - CPF (TextField)
     - Data de Admissão (Date)
     - Cargo (Select)
     - Centro de Custo (Select)
     - Linha de Negócio (Select disabled)
   - Botões: Cancelar e Salvar

### Interações

1. **Filtros**:
   - Preenchimento independente
   - Botão "Filtrar" dispara busca
   - Botão "Limpar Filtros" limpa formulário e recarrega todos

2. **Criação**:
   - Botão "Novo Funcionário" abre Dialog
   - Campos obrigatórios validados
   - Centro de Custo determina Linha de Negócio
   - Botão "Salvar" envia POST

3. **Edição**:
   - Ícone de Editar abre Dialog pré-preenchido
   - Validações mantidas
   - Botão "Salvar" envia PUT

4. **Exclusão**:
   - Ícone de Deletar dispara confirmação
   - Confirmação com `window.confirm`
   - Soft delete no backend

### Responsividade

- **Desktop**: Cards em grid (3-4 colunas)
- **Tablet**: Cards em grid (2 colunas)
- **Mobile**: Cards empilhados (1 coluna)
- Filtros também se adaptam (flexWrap)

---

## 📊 Relacionamentos e Dependências

### Diagrama de Relacionamentos

```
LinhaNegocio (1) ───< (N) CentroCusto (1) ───< (N) Funcionario
                                                       │
                                      Cargo (1) ───< (N)
```

**Explicação:**
- **Funcionario** pertence a **1 Cargo** (`@ManyToOne`)
- **Funcionario** pertence a **1 Centro de Custo** (`@ManyToOne`)
- **Centro de Custo** pertence a **1 Linha de Negócio** (`@ManyToOne`)
- **Linha de Negócio** é derivada automaticamente do Centro de Custo

### Cascata de Dados

1. **Criação**:
   - Frontend envia: `cargoId`, `centroCustoId`
   - Backend deriva: `linhaNegocioId` (via `centroCusto.linhaNegocio`)
   - Validações: cargo ativo, centro de custo ativo

2. **Edição**:
   - Frontend envia: `cargoId`, `centroCustoId`
   - Backend atualiza relacionamentos
   - Validações: CPF único (se alterado), cargo ativo, centro de custo ativo

3. **Exclusão**:
   - Soft delete: `ativo = false`
   - Relacionamentos preservados (histórico)

---

## 🐛 Possíveis Problemas e Soluções

### 1. CPF Duplicado

**Problema**: Tentar cadastrar funcionário com CPF já existente.

**Solução**:
- Backend valida: `existsByCpfAndAtivoTrue()`
- Lança: `IllegalArgumentException`
- Frontend recebe: `400 Bad Request`
- Exibe: toast "Erro ao salvar funcionário"

**Melhoria**: Mensagem específica no frontend (ex: "CPF já cadastrado")

### 2. Cargo ou Centro de Custo Inativo

**Problema**: Tentar associar funcionário a cargo/centro de custo inativo.

**Solução**:
- Backend valida: `filter(isAtivo())` / `filter(getAtivo())`
- Lança: `CargoNotFoundException` / `CentroCustoNotFoundException`
- Frontend recebe: `404 Not Found`
- Exibe: toast "Erro ao salvar funcionário"

**Melhoria**: Filtrar apenas ativos nos dropdowns do frontend

### 3. Linha de Negócio Não Sincronizada

**Problema**: Linha de Negócio do formulário não atualiza ao mudar Centro de Custo.

**Solução**:
- No `onChange` de `centroCustoId`:
  ```typescript
  const centroSelecionado = centrosCusto.find(c => c.id === Number(e.target.value));
  if (centroSelecionado && centroSelecionado.linhaNegocioId) {
    setValueEdit('linhaNegocioId', centroSelecionado.linhaNegocioId.toString());
  }
  ```

### 4. Data de Admissão Futura

**Problema**: Tentar cadastrar funcionário com data futura.

**Solução**:
- Backend valida: `@Past`
- Lança: `MethodArgumentNotValidException`
- Frontend recebe: `400 Bad Request`
- Exibe: toast "Erro ao salvar funcionário"

**Melhoria**: Validação frontend com `max={new Date().toISOString().split('T')[0]}`

### 5. Lazy Loading com Sessão Fechada

**Problema**: Acesso a `cargo` ou `centroCusto` fora de transação.

**Solução**:
- Método `toDTO()` acessa relacionamentos dentro de transação (Service)
- JPA carrega relacionamentos dentro do contexto transacional

---

## 🚀 Melhorias Futuras Possíveis

### Backend

1. **Validação de CPF**:
   - Implementar algoritmo de validação de CPF
   - Usar biblioteca como `hibernate-validator-cep-and-cpf`

2. **Paginação**:
   - Adicionar `Pageable` no endpoint de listagem
   - Retornar `Page<FuncionarioDTO>` para grandes volumes

3. **Ordenação Customizada**:
   - Permitir ordenação por qualquer campo
   - Adicionar `@RequestParam Sort sort`

4. **Auditoria Completa**:
   - Implementar `criadoPor` e `atualizadoPor`
   - Usar `@CreatedBy` e `@LastModifiedBy` do Spring Data JPA Auditing

5. **Histórico de Alterações**:
   - Tabela de auditoria com Envers (Hibernate)
   - Rastrear todas as mudanças

6. **Busca Full-Text**:
   - Implementar busca por múltiplos campos
   - Usar Elasticsearch ou PostgreSQL Full-Text Search

7. **Exportação**:
   - Endpoint para exportar CSV/Excel
   - Usar Apache POI ou OpenCSV

8. **Importação em Lote**:
   - Endpoint para upload de CSV
   - Validação e processamento em lote

### Frontend

1. **Validação de CPF**:
   - Máscara de input (ex: `###.###.###-##`)
   - Validação de formato no frontend

2. **Paginação**:
   - Componente `Pagination` do MUI
   - Controle de `page` e `size`

3. **Ordenação**:
   - Tabela em vez de cards (opção de visualização)
   - Colunas ordenáveis

4. **Busca em Tempo Real**:
   - Debounce no campo de nome
   - Atualização automática da lista

5. **Filtros Avançados**:
   - Data de admissão (intervalo)
   - Status (ativo/inativo)
   - Múltiplos cargos/centros de custo

6. **Exportação**:
   - Botão "Exportar para Excel"
   - Aplicar filtros atuais

7. **Visualização Detalhada**:
   - Modal de visualização (sem edição)
   - Histórico de alterações
   - Vínculos com folha de pagamento

8. **Upload de Foto**:
   - Campo de foto no formulário
   - Exibição de avatar nos cards

9. **Validações em Tempo Real**:
   - Verificar CPF duplicado ao digitar
   - Feedback visual imediato

10. **Responsividade Avançada**:
    - Visualização mobile otimizada
    - Swipe para editar/deletar

---

## 📋 Checklist de Implementação

### Backend
- ✅ Entidade `Funcionario` com validações
- ✅ Repository com métodos customizados
- ✅ Service com lógica de negócio
- ✅ Controller RESTful
- ✅ DTO imutável (Record)
- ✅ Soft delete
- ✅ Filtros dinâmicos
- ✅ Validações de integridade
- ✅ Tratamento de exceções
- ✅ Documentação OpenAPI

### Frontend
- ✅ Componente com listagem
- ✅ Formulário de filtros
- ✅ Dialog de criação/edição
- ✅ Validações com React Hook Form
- ✅ Integração com API
- ✅ Toasts de feedback
- ✅ Layout responsivo
- ✅ Sincronização de relacionamentos
- ✅ Confirmação de exclusão
- ✅ Formatação de data

---

## 🎯 Conclusão

A tela de **Funcionários** é um exemplo robusto de CRUD completo no sistema, implementando:

1. **Backend Sólido**:
   - Arquitetura em camadas (Controller → Service → Repository)
   - Validações em múltiplas camadas
   - Soft delete para preservar histórico
   - Relacionamentos bem definidos
   - DTOs imutáveis para segurança

2. **Frontend Moderno**:
   - React com TypeScript para type safety
   - Material-UI para UI consistente
   - React Hook Form para gestão de formulários
   - Filtros dinâmicos
   - UX intuitiva

3. **Integração Completa**:
   - API RESTful bem documentada
   - Comunicação com Axios e interceptors
   - Sincronização automática de relacionamentos
   - Feedback visual em todas as ações

4. **Boas Práticas**:
   - Separação de responsabilidades
   - Validações em ambos os lados
   - Tratamento de erros robusto
   - Código limpo e manutenível

Esta tela serve como referência para implementação de outras funcionalidades CRUD no sistema.

---

**Documento criado em:** 16 de outubro de 2025  
**Última atualização:** 16 de outubro de 2025  
**Versão:** 1.0

