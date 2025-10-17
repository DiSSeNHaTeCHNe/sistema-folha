# 📋 Conhecimento Consolidado: Tela de Cargos

## 📌 Visão Geral

A tela de **Cargos** é uma das telas mais **simples e diretas** do sistema, implementando um **CRUD básico** para gerenciamento de cargos de funcionários. É um exemplo perfeito de uma funcionalidade CRUD bem estruturada, com código limpo e mínimo necessário.

### Objetivo da Tela
- Cadastrar cargos da empresa (ex: Desenvolvedor, Analista, Gerente)
- Listar todos os cargos ativos
- Atualizar informações de cargos existentes
- Remover cargos (soft delete)
- Servir como entidade de referência para Funcionários

### Características Principais
- **CRUD Completo**: Create, Read, Update, Delete
- **Soft Delete**: Registros marcados como inativos, nunca deletados fisicamente
- **Entidade Simples**: Apenas 3 campos (id, descrição, ativo)
- **Sem Relacionamentos na Tela**: Embora seja referenciado por Funcionários, não há navegação
- **Validações Básicas**: Descrição obrigatória e tamanho mínimo/máximo
- **Interface Tabular**: Lista completa em uma única tabela

---

## 🏗️ Arquitetura da Aplicação

### Stack Tecnológico

#### Backend
- **Framework**: Spring Boot 3.2.3
- **Linguagem**: Java 17
- **ORM**: Spring Data JPA + Hibernate
- **Banco de Dados**: PostgreSQL
- **Validação**: Jakarta Bean Validation
- **Logging**: SLF4J
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
│   └── CargoController.java                    # REST API (57 linhas)
├── service/
│   └── CargoService.java                       # Lógica de negócio (81 linhas)
├── repository/
│   └── CargoRepository.java                    # Acesso a dados (9 linhas)
├── model/
│   └── Cargo.java                              # Entidade JPA (20 linhas)
├── dto/
│   └── CargoDTO.java                           # DTO imutável (20 linhas)
└── exception/
    └── CargoNotFoundException.java             # Exceção customizada
```

**Total**: ~187 linhas de código backend

### Frontend

```
frontend/src/
├── pages/
│   └── Cargos/
│       └── index.tsx                           # Componente principal (172 linhas)
├── services/
│   └── cargoService.ts                         # Serviço de API (34 linhas)
└── types/
    └── index.ts                                # Interface Cargo
```

**Total**: ~206 linhas de código frontend

---

## 🔄 Fluxo de Dados Completo

### 1. Listagem de Cargos (Carga Inicial)

```
Frontend (index.tsx)
    │
    ├─> useEffect: carregarCargos() chamado na montagem
    │
    └─> cargoService.listarTodos()
            │
            └─> api.get('/cargos')
                    │
                    ├─> Axios Interceptor adiciona JWT
                    │
                    └─> Backend: CargoController.listarTodos()
                            │
                            └─> CargoService.listarTodos()
                                    │
                                    ├─> cargoRepository.findAll()
                                    ├─> Filtra apenas ativos (.filter(c -> c.isAtivo()))
                                    ├─> Converte para DTO (.map(this::toDTO))
                                    │
                                    └─> Retorna List<CargoDTO>
                                            │
                                            └─> Frontend recebe dados
                                                    │
                                                    ├─> setCargos(data)
                                                    │
                                                    └─> Renderiza TableContainer
                                                            │
                                                            ├─> ID
                                                            ├─> Descrição
                                                            ├─> Status (Ativo/Inativo)
                                                            └─> Ações (Editar/Excluir)
```

### 2. Cadastro de Novo Cargo

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em "Novo Cargo"
    │   └─> handleOpen() sem parâmetro
    │       ├─> setSelectedCargo(null)
    │       ├─> reset() (limpa formulário)
    │       └─> setOpen(true)
    │
    ├─> Dialog abre com formulário vazio
    │
    ├─> Usuário preenche "Descrição"
    │
    ├─> Usuário clica em "Cadastrar"
    │   └─> handleSubmit(onSubmit)
    │       │
    │       └─> cargoService.cadastrar(data)
    │               │
    │               └─> api.post('/cargos', data)
    │                       │
    │                       └─> Backend: CargoController.cadastrar()
    │                               │
    │                               └─> CargoService.cadastrar(dto)
    │                                       │
    │                                       ├─> toEntity(dto)
    │                                       │   └─> Cria Cargo com ativo=true
    │                                       │
    │                                       ├─> cargoRepository.save(cargo)
    │                                       │
    │                                       └─> toDTO(cargo)
    │                                               │
    │                                               └─> Frontend recebe CargoDTO
    │                                                       │
    │                                                       ├─> toast.success()
    │                                                       ├─> handleClose()
    │                                                       └─> carregarCargos() (recarrega lista)
```

### 3. Edição de Cargo Existente

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em ícone "Editar" de um cargo
    │   └─> handleOpen(cargo)
    │       ├─> setSelectedCargo(cargo)
    │       ├─> setValue('descricao', cargo.descricao)
    │       └─> setOpen(true)
    │
    ├─> Dialog abre com formulário preenchido
    │
    ├─> Usuário altera "Descrição"
    │
    ├─> Usuário clica em "Atualizar"
    │   └─> handleSubmit(onSubmit)
    │       │
    │       └─> cargoService.atualizar(selectedCargo.id, data)
    │               │
    │               └─> api.put(`/cargos/${id}`, data)
    │                       │
    │                       └─> Backend: CargoController.atualizar()
    │                               │
    │                               └─> CargoService.atualizar(id, dto)
    │                                       │
    │                                       ├─> cargoRepository.findById(id)
    │                                       ├─> Filtra ativo (.filter(c -> c.isAtivo()))
    │                                       ├─> Lança CargoNotFoundException se não encontrado
    │                                       ├─> cargo.setDescricao(dto.descricao())
    │                                       ├─> cargoRepository.save(cargo)
    │                                       │
    │                                       └─> toDTO(cargo)
    │                                               │
    │                                               └─> Frontend recebe CargoDTO
    │                                                       │
    │                                                       ├─> toast.success()
    │                                                       ├─> handleClose()
    │                                                       └─> carregarCargos()
```

### 4. Exclusão de Cargo (Soft Delete)

```
Frontend (index.tsx)
    │
    ├─> Usuário clica em ícone "Excluir" de um cargo
    │
    ├─> window.confirm('Tem certeza que deseja excluir este cargo?')
    │   └─> Se confirmar:
    │       └─> handleDelete(id)
    │               │
    │               └─> cargoService.remover(id)
    │                       │
    │                       └─> api.delete(`/cargos/${id}`)
    │                               │
    │                               └─> Backend: CargoController.remover()
    │                                       │
    │                                       └─> CargoService.remover(id)
    │                                               │
    │                                               ├─> cargoRepository.findById(id)
    │                                               ├─> Filtra ativo
    │                                               ├─> Lança exceção se não encontrado
    │                                               ├─> cargo.setAtivo(false)
    │                                               ├─> cargoRepository.save(cargo)
    │                                               │
    │                                               └─> Frontend recebe 204 No Content
    │                                                       │
    │                                                       ├─> toast.success()
    │                                                       └─> carregarCargos()
```

---

## 🔍 Análise Detalhada do Código Backend

### 1. Entidade `Cargo.java` (20 linhas)

```java
@Data
@Entity
@Table(name = "cargos")
public class Cargo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String descricao;
    
    @Column(nullable = false)
    private boolean ativo = true;
}
```

**Características:**
- **Simplicidade Máxima**: Apenas 3 campos
- **@Data** (Lombok): Gera getters, setters, equals, hashCode, toString
- **@Entity**: Marca como entidade JPA
- **@Table**: Nome da tabela no banco
- **@Id + @GeneratedValue**: Chave primária auto-incrementada
- **@Column(nullable = false)**: Campos obrigatórios
- **Default Value**: `ativo = true` (novos registros são ativos)

**Observações:**
- Sem auditoria (dataCriacao, dataAtualizacao)
- Sem validações Jakarta Bean Validation (validações no DTO)
- Sem relacionamentos mapeados (embora seja referenciado por Funcionario)

### 2. Repository `CargoRepository.java` (9 linhas)

```java
@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
}
```

**Características:**
- **Interface vazia**: Herda todos os métodos de `JpaRepository`
- **Métodos herdados**:
  - `findAll()`: lista todos os cargos
  - `findById(Long id)`: busca por ID
  - `save(Cargo cargo)`: salva ou atualiza
  - `delete(Cargo cargo)`: deleta (não usado devido ao soft delete)
  - Muitos outros métodos prontos

**Observações:**
- Não há queries customizadas
- Não há métodos derivados (findByDescricao, etc.)
- Filtro de `ativo` é feito no Service, não no Repository

### 3. DTO `CargoDTO.java` (20 linhas)

```java
@Schema(description = "DTO para Cargo")
public record CargoDTO(
    @Schema(description = "Identificador único do cargo", example = "1")
    Long id,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição do cargo", example = "Desenvolvedor", required = true)
    String descricao,

    @Schema(description = "Indica se o cargo está ativo", example = "true")
    Boolean ativo
) {}
```

**Características:**
- **Record** (Java 14+): DTO imutável, conciso, thread-safe
- **Jakarta Bean Validation**:
  - `@NotBlank`: descrição não pode ser nula ou vazia
  - `@Size(min=3, max=100)`: tamanho entre 3 e 100 caracteres
- **OpenAPI (@Schema)**: Documentação Swagger
- **Imutabilidade**: Records são imutáveis por padrão

**Observações:**
- Validações no DTO, não na Entidade
- Mesma estrutura da Entidade (todos os campos)

### 4. Service `CargoService.java` (81 linhas)

```java
@Service
@RequiredArgsConstructor
public class CargoService {
    private static final Logger logger = LoggerFactory.getLogger(CargoService.class);

    private final CargoRepository cargoRepository;

    public List<CargoDTO> listarTodos() {
        logger.info("Listando todos os cargos");
        return cargoRepository.findAll().stream()
                .filter(c -> c.isAtivo())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CargoDTO buscarPorId(Long id) {
        logger.info("Buscando cargo por ID: {}", id);
        return cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new CargoNotFoundException(id));
    }

    @Transactional
    public CargoDTO cadastrar(CargoDTO dto) {
        logger.info("Cadastrando novo cargo: {}", dto.descricao());
        Cargo cargo = toEntity(dto);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public CargoDTO atualizar(Long id, CargoDTO dto) {
        logger.info("Atualizando cargo ID: {}", id);
        Cargo cargo = cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(id));

        cargo.setDescricao(dto.descricao());
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public void remover(Long id) {
        logger.info("Removendo cargo ID: {}", id);
        Cargo cargo = cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(id));
        cargo.setAtivo(false);
        cargoRepository.save(cargo);
    }

    private CargoDTO toDTO(Cargo cargo) {
        return new CargoDTO(
            cargo.getId(),
            cargo.getDescricao(),
            cargo.isAtivo()
        );
    }

    private Cargo toEntity(CargoDTO dto) {
        Cargo cargo = new Cargo();
        cargo.setDescricao(dto.descricao());
        cargo.setAtivo(true);
        return cargo;
    }
}
```

**Características:**
- **@Service**: Marca como componente de serviço
- **@RequiredArgsConstructor** (Lombok): Injeta dependências via construtor
- **Logging**: SLF4J em todos os métodos principais
- **Streams**: Uso de programação funcional (.filter, .map, .collect)
- **@Transactional**: Métodos de escrita (cadastrar, atualizar, remover)
- **Filtro de Ativos**: `.filter(c -> c.isAtivo())` em todos os métodos de leitura
- **Exceção Customizada**: `CargoNotFoundException` se não encontrado

**Métodos:**
1. **listarTodos()**: 
   - `findAll()` → filtra ativos → mapeia para DTO
   
2. **buscarPorId()**:
   - `findById()` → filtra ativo → mapeia ou lança exceção
   
3. **cadastrar()**:
   - Converte DTO → Entidade
   - Salva no banco
   - Retorna DTO
   
4. **atualizar()**:
   - Busca entidade existente (apenas ativos)
   - Atualiza descrição
   - Salva
   - Retorna DTO
   
5. **remover()**:
   - Busca entidade (apenas ativos)
   - Marca `ativo = false`
   - Salva

**Conversões:**
- `toDTO()`: Entidade → DTO
- `toEntity()`: DTO → Entidade (sempre com `ativo = true`)

### 5. Controller `CargoController.java` (57 linhas)

```java
@RestController
@RequestMapping("/cargos")
@RequiredArgsConstructor
@Tag(name = "Cargos", description = "API para gerenciamento de cargos")
public class CargoController {
    private final CargoService cargoService;

    @GetMapping
    @Operation(summary = "Lista todos os cargos ativos")
    public ResponseEntity<List<CargoDTO>> listarTodos() {
        return ResponseEntity.ok(cargoService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cargo pelo ID")
    public ResponseEntity<CargoDTO> buscarPorId(
            @Parameter(description = "ID do cargo") @PathVariable Long id) {
        return ResponseEntity.ok(cargoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo cargo")
    public ResponseEntity<CargoDTO> cadastrar(
            @Parameter(description = "Dados do cargo") @Valid @RequestBody CargoDTO cargo) {
        return ResponseEntity.ok(cargoService.cadastrar(cargo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cargo existente")
    public ResponseEntity<CargoDTO> atualizar(
            @Parameter(description = "ID do cargo") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do cargo") @Valid @RequestBody CargoDTO cargo) {
        return ResponseEntity.ok(cargoService.atualizar(id, cargo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um cargo")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do cargo") @PathVariable Long id) {
        cargoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Características:**
- **@RestController**: Marca como controller REST
- **@RequestMapping("/cargos")**: Base path
- **@RequiredArgsConstructor**: Injeta CargoService
- **OpenAPI**:
  - `@Tag`: Agrupa endpoints no Swagger
  - `@Operation`: Descreve cada endpoint
  - `@Parameter`: Documenta parâmetros

**Endpoints:**
1. **GET /cargos**: Lista todos (apenas ativos)
2. **GET /cargos/{id}**: Busca por ID
3. **POST /cargos**: Cadastra novo cargo
4. **PUT /cargos/{id}**: Atualiza cargo existente
5. **DELETE /cargos/{id}**: Remove cargo (soft delete)

**Validações:**
- `@Valid`: Ativa validações do DTO (Jakarta Bean Validation)
- `@PathVariable`: Extrai ID da URL
- `@RequestBody`: Recebe JSON no corpo da requisição

**Responses:**
- `200 OK`: Sucesso com corpo (GET, POST, PUT)
- `204 No Content`: Sucesso sem corpo (DELETE)
- `404 Not Found`: Cargo não encontrado (tratado por exception handler global)
- `400 Bad Request`: Validação falhou

---

## 🎨 Análise Detalhada do Código Frontend

### 1. Componente Principal `index.tsx` (172 linhas)

#### Imports e Tipos

```typescript
import { useEffect, useState } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions,
  DialogContent, DialogTitle, IconButton, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { cargoService } from '../../services/cargoService';
import type { Cargo } from '../../types';

interface CargoFormData {
  descricao: string;
}
```

**Observações:**
- Material-UI completo (tabelas, dialogs, ícones)
- React Hook Form para gestão de formulário
- Toast para notificações
- Tipo `CargoFormData` local (apenas descrição)

#### Estados

```typescript
const [cargos, setCargos] = useState<Cargo[]>([]);
const [open, setOpen] = useState(false);
const [selectedCargo, setSelectedCargo] = useState<Cargo | null>(null);
const { register, handleSubmit, reset, setValue } = useForm<CargoFormData>();
```

**Observações:**
- **cargos[]**: Lista de cargos carregada do backend
- **open**: Controla abertura do Dialog
- **selectedCargo**: Cargo sendo editado (null = novo)
- **React Hook Form**: `register`, `handleSubmit`, `reset`, `setValue`

#### Efeitos e Carregamento

```typescript
useEffect(() => {
  carregarCargos();
}, []);

const carregarCargos = async () => {
  try {
    const data = await cargoService.listarTodos();
    setCargos(data);
  } catch (error) {
    toast.error('Erro ao carregar cargos');
  }
};
```

**Observações:**
- `useEffect` vazio: carrega na montagem do componente
- Try-catch: tratamento de erro com toast
- Assíncrono: usa async/await

#### Handlers

**1. Abrir Dialog**

```typescript
const handleOpen = (cargo?: Cargo) => {
  if (cargo) {
    setSelectedCargo(cargo);
    setValue('descricao', cargo.descricao);
  } else {
    setSelectedCargo(null);
    reset();
  }
  setOpen(true);
};
```

**Observações:**
- Parâmetro opcional: `cargo?`
- **Edição**: preenche formulário com `setValue`
- **Novo**: limpa formulário com `reset()`

**2. Fechar Dialog**

```typescript
const handleClose = () => {
  setOpen(false);
  reset();
};
```

**3. Submissão do Formulário**

```typescript
const onSubmit = async (data: CargoFormData) => {
  try {
    if (selectedCargo) {
      await cargoService.atualizar(selectedCargo.id, data);
      toast.success('Cargo atualizado com sucesso');
    } else {
      await cargoService.cadastrar(data);
      toast.success('Cargo cadastrado com sucesso');
    }
    handleClose();
    carregarCargos();
  } catch (error) {
    toast.error('Erro ao salvar cargo');
  }
};
```

**Observações:**
- **Condicional**: atualiza se tem `selectedCargo`, senão cadastra
- **Feedback**: toast de sucesso/erro
- **Atualização**: fecha dialog e recarrega lista

**4. Exclusão**

```typescript
const handleDelete = async (id: number) => {
  if (window.confirm('Tem certeza que deseja excluir este cargo?')) {
    try {
      await cargoService.remover(id);
      toast.success('Cargo excluído com sucesso');
      carregarCargos();
    } catch (error) {
      toast.error('Erro ao excluir cargo');
    }
  }
};
```

**Observações:**
- **Confirmação**: `window.confirm()` nativo
- **Soft delete**: backend marca como inativo

#### Renderização JSX

**Estrutura Principal**

```typescript
<Box p={3}>
  {/* Cabeçalho */}
  <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
    <Typography variant="h4">Cargos</Typography>
    <Button
      variant="contained"
      color="primary"
      startIcon={<AddIcon />}
      onClick={() => handleOpen()}
    >
      Novo Cargo
    </Button>
  </Box>

  {/* Tabela */}
  <Card>
    <CardContent>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Descrição</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="center">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {cargos.map((cargo) => (
              <TableRow key={cargo.id}>
                <TableCell>{cargo.id}</TableCell>
                <TableCell>{cargo.descricao}</TableCell>
                <TableCell>{cargo.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                <TableCell align="center">
                  <IconButton color="primary" onClick={() => handleOpen(cargo)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton color="error" onClick={() => handleDelete(cargo.id)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </CardContent>
  </Card>

  {/* Dialog */}
  <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
    <DialogTitle>
      {selectedCargo ? 'Editar Cargo' : 'Novo Cargo'}
    </DialogTitle>
    <form onSubmit={handleSubmit(onSubmit)}>
      <DialogContent>
        <TextField
          {...register('descricao', { required: 'Descrição é obrigatória' })}
          label="Descrição"
          fullWidth
          margin="normal"
          required
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancelar</Button>
        <Button type="submit" variant="contained" color="primary">
          {selectedCargo ? 'Atualizar' : 'Cadastrar'}
        </Button>
      </DialogActions>
    </form>
  </Dialog>
</Box>
```

**Observações:**
- **Layout**: Box com padding de 3
- **Cabeçalho**: Título + Botão "Novo Cargo"
- **Tabela**: Card → TableContainer → Table
- **Colunas**: ID, Descrição, Status, Ações
- **Ações**: Ícones de Editar (azul) e Excluir (vermelho)
- **Dialog**: Modal para criar/editar
- **Formulário**: Um único campo (descrição)
- **React Hook Form**: `{...register('descricao')}`
- **Validação**: `required: 'Descrição é obrigatória'`

### 2. Serviço Frontend `cargoService.ts` (34 linhas)

```typescript
import api from './api';
import type { Cargo } from '../types';

interface CargoFormData {
  descricao: string;
}

const cargoService = {
  listarTodos: async (): Promise<Cargo[]> => {
    const response = await api.get('/cargos');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Cargo> => {
    const response = await api.get(`/cargos/${id}`);
    return response.data;
  },

  cadastrar: async (data: CargoFormData): Promise<Cargo> => {
    const response = await api.post('/cargos', data);
    return response.data;
  },

  atualizar: async (id: number, data: CargoFormData): Promise<Cargo> => {
    const response = await api.put(`/cargos/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/cargos/${id}`);
  }
};

export { cargoService };
```

**Características:**
- **Camada de Abstração**: Isola lógica de API
- **Tipagem Completa**: TypeScript com tipos explícitos
- **CRUD Completo**: 5 métodos
- **Async/Await**: Todas as operações assíncronas
- **Retornos Tipados**: `Promise<Cargo>`, `Promise<Cargo[]>`, `Promise<void>`

**Métodos:**
1. **listarTodos()**: GET /cargos
2. **buscarPorId(id)**: GET /cargos/:id
3. **cadastrar(data)**: POST /cargos
4. **atualizar(id, data)**: PUT /cargos/:id
5. **remover(id)**: DELETE /cargos/:id

---

## 🔒 Validações e Regras de Negócio

### Backend

1. **Validações de DTO** (Jakarta Bean Validation):
   - `@NotBlank`: descrição não nula/vazia
   - `@Size(min=3, max=100)`: tamanho entre 3 e 100 caracteres

2. **Soft Delete**:
   - Campo `ativo` sempre utilizado
   - Filtro `.filter(c -> c.isAtivo())` em todas as consultas
   - Remoção marca `ativo = false`, nunca deleta fisicamente

3. **Exceção Customizada**:
   - `CargoNotFoundException` se ID não encontrado ou inativo
   - Tratada por exception handler global

4. **Transações**:
   - `@Transactional` em métodos de escrita
   - Rollback automático em caso de erro

5. **Logging**:
   - SLF4J em todos os métodos principais
   - Nível INFO para operações normais

### Frontend

1. **Validações de Formulário** (React Hook Form):
   - `required: 'Descrição é obrigatória'`
   - Validação nativa do HTML5 (`required` no TextField)

2. **Confirmação de Exclusão**:
   - `window.confirm()` antes de deletar

3. **Feedback Visual**:
   - Toast de sucesso/erro em todas as operações
   - Recarregamento da lista após mudanças

4. **Tratamento de Erros**:
   - Try-catch em todas as operações assíncronas
   - Mensagens de erro genéricas (sem detalhes técnicos)

---

## 🎨 UX/UI da Tela

### Layout

1. **Cabeçalho**:
   - Título "Cargos" (h4)
   - Botão "Novo Cargo" (direita, azul, ícone +)

2. **Tabela Principal**:
   - Card com TableContainer
   - Colunas: ID, Descrição, Status, Ações
   - Ações: ícones de Editar (azul) e Excluir (vermelho)

3. **Dialog de Criação/Edição**:
   - Modal centrado (`maxWidth="sm"`)
   - Título dinâmico: "Novo Cargo" ou "Editar Cargo"
   - Campo único: Descrição (TextField)
   - Botões: "Cancelar" e "Cadastrar"/"Atualizar"

### Interações

1. **Listagem**:
   - Carregamento automático ao abrir a tela
   - Exibe ID, descrição e status de cada cargo

2. **Criação**:
   - Clique em "Novo Cargo" → Dialog abre
   - Preenche descrição → Clique em "Cadastrar"
   - Toast de sucesso → Dialog fecha → Lista atualiza

3. **Edição**:
   - Clique em ícone de Editar → Dialog abre com dados preenchidos
   - Altera descrição → Clique em "Atualizar"
   - Toast de sucesso → Dialog fecha → Lista atualiza

4. **Exclusão**:
   - Clique em ícone de Excluir → Confirmação
   - Se confirmar → Toast de sucesso → Lista atualiza
   - Se cancelar → Nada acontece

### Responsividade

- **Desktop**: Tabela completa, dialog centralizado
- **Tablet**: Tabela com scroll horizontal se necessário
- **Mobile**: Tabela compacta, dialog fullscreen

---

## 🐛 Possíveis Problemas e Soluções

### 1. Cargo Referenciado por Funcionários

**Problema**: Tentar excluir cargo que tem funcionários associados pode causar erros de integridade referencial.

**Solução Atual**: Soft delete (não deleta fisicamente).

**Melhoria**:
- Validação backend: verificar se há funcionários antes de marcar como inativo
- Frontend: exibir mensagem informativa se cargo em uso

### 2. Duplicação de Descrição

**Problema**: Nada impede cadastrar dois cargos com mesma descrição.

**Solução Atual**: Não há validação de duplicados.

**Melhoria**:
- Constraint UNIQUE no banco para descrição
- Validação backend antes de salvar
- Mensagem de erro clara no frontend

### 3. Lista Longa de Cargos

**Problema**: Se houver centenas de cargos, a tabela fica difícil de navegar.

**Solução Atual**: Sem paginação ou busca.

**Melhoria**:
- Paginação (TablePagination do MUI)
- Campo de busca por descrição
- Ordenação por colunas

### 4. Sem Auditoria

**Problema**: Não há registro de quem criou/alterou e quando.

**Solução Atual**: Não há campos de auditoria.

**Melhoria**:
- Adicionar campos: `dataCriacao`, `dataAtualizacao`, `criadoPor`, `atualizadoPor`
- `@PrePersist` e `@PreUpdate` na entidade
- Capturar usuário logado do contexto de segurança

---

## 🚀 Melhorias Futuras Possíveis

### Backend

1. **Auditoria Automática**:
   - Campos de data/usuário de criação e atualização
   - Usar `@EntityListeners` com `AuditingEntityListener`

2. **Validação de Duplicados**:
   - Constraint UNIQUE na descrição
   - Query customizada: `existsByDescricaoAndAtivoTrue()`

3. **Ordenação**:
   - Retornar cargos ordenados por descrição
   - Query: `findAllByAtivoTrueOrderByDescricaoAsc()`

4. **Cache**:
   - `@Cacheable` para listar todos
   - `@CacheEvict` em operações de escrita

5. **Query Customizadas**:
   - Busca por descrição parcial
   - Filtro por status (ativo/inativo)

### Frontend

1. **Paginação**:
   - `TablePagination` do MUI
   - Controle de `page` e `rowsPerPage`

2. **Busca**:
   - Campo de busca por descrição
   - Filtro local (client-side) ou remoto (server-side)

3. **Ordenação**:
   - Colunas clicáveis para ordenar
   - `TableSortLabel` do MUI

4. **Filtro de Status**:
   - Toggle para exibir inativos
   - Chip de status colorido

5. **Bulk Actions**:
   - Checkbox para seleção múltipla
   - Ações em lote (ativar/inativar múltiplos)

6. **Validações Avançadas**:
   - Descrição única (validação remota)
   - Debounce na busca

7. **Skeleton Loading**:
   - Indicador visual durante carregamento
   - `Skeleton` do MUI

8. **Confirmação Melhorada**:
   - Dialog customizado em vez de `window.confirm()`
   - Mais informações sobre o impacto da exclusão

---

## 📋 Checklist de Implementação

### Backend
- ✅ Entidade Cargo (20 linhas)
- ✅ Repository (9 linhas, interface vazia)
- ✅ DTO com validações (20 linhas)
- ✅ Service completo (81 linhas)
- ✅ Controller RESTful (57 linhas)
- ✅ Soft delete
- ✅ Logging
- ✅ Transações
- ✅ Exceção customizada
- ✅ Documentação OpenAPI

### Frontend
- ✅ Componente principal (172 linhas)
- ✅ Service de API (34 linhas)
- ✅ Listagem em tabela
- ✅ Dialog de criação/edição
- ✅ Validações de formulário (React Hook Form)
- ✅ Confirmação de exclusão
- ✅ Toast notifications
- ✅ Tratamento de erros
- ✅ Recarregamento após mudanças

### Funcionalidades
- ✅ Listar cargos (apenas ativos)
- ✅ Buscar por ID
- ✅ Cadastrar novo cargo
- ✅ Atualizar cargo existente
- ✅ Remover cargo (soft delete)

---

## 🎯 Conclusão

A tela de **Cargos** é um **exemplo perfeito de CRUD simples e bem estruturado**, implementando:

1. **Backend Minimalista** (~187 linhas):
   - Entidade com 3 campos apenas
   - Repository sem queries customizadas
   - Service com lógica de negócio clara
   - Controller com 5 endpoints RESTful
   - Soft delete em todos os métodos

2. **Frontend Funcional** (~206 linhas):
   - Componente único com tabela e dialog
   - React Hook Form para gestão de formulário
   - Material-UI para UI consistente
   - Service de API isolado

3. **Características Principais**:
   - **Simplicidade**: Código mínimo necessário
   - **Padrões**: Segue padrões do sistema
   - **Validações**: Backend e frontend
   - **Soft Delete**: Preserva histórico
   - **Logging**: Rastreabilidade

4. **Exemplo de Referência**:
   - Serve como template para outros CRUDs simples
   - Código limpo e fácil de entender
   - Boa separação de responsabilidades

Esta tela demonstra que **nem sempre é necessário complexidade** - às vezes, um CRUD simples e bem feito é exatamente o que se precisa.

---

**Documento criado em:** 16 de outubro de 2025  
**Última atualização:** 16 de outubro de 2025  
**Versão:** 1.0

