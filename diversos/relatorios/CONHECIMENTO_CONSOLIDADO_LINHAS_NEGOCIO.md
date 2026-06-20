# 🏢 Conhecimento Consolidado - Tela de Linhas de Negócio

## 📋 Índice

1. [Visão Geral](#-visão-geral)
2. [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
3. [Camada Backend](#-camada-backend)
4. [Camada Frontend](#-camada-frontend)
5. [Fluxo de Dados](#-fluxo-de-dados)
6. [Exemplos de Código](#-exemplos-de-código)
7. [Padrões e Boas Práticas](#-padrões-e-boas-práticas)
8. [Melhorias Futuras](#-melhorias-futuras)

---

## 🎯 Visão Geral

A tela de **Linhas de Negócio** é uma implementação CRUD (Create, Read, Update, Delete) que gerencia as linhas de negócio da empresa. É uma **entidade base do sistema**, sem relacionamentos obrigatórios, servindo como referência para outras entidades (especialmente Centros de Custo).

### Características Principais

- ✅ **CRUD Completo**: Cadastrar, listar, atualizar e remover linhas de negócio
- ✅ **Entidade Base**: Não possui relacionamentos obrigatórios
- ✅ **Soft Delete**: Exclusão lógica preservando integridade referencial
- ✅ **Validações**: Frontend e backend com validações consistentes
- ✅ **UI Simples e Funcional**: Interface Material-UI intuitiva
- ✅ **Código Minimalista**: ~177 linhas backend, ~172 linhas frontend

### Comparação: Cargos vs Linhas de Negócio

Ambas são entidades base extremamente similares:

| Aspecto | Cargos | Linhas de Negócio |
|---------|--------|-------------------|
| Campos | ID, Descrição, Ativo | ID, Descrição, Ativo |
| Relacionamentos | Nenhum | Nenhum |
| Formulário | 1 campo (descrição) | 1 campo (descrição) |
| Endpoints | 5 (CRUD básico) | 5 (CRUD básico) |
| Backend | ~187 linhas | ~177 linhas |
| Frontend | ~206 linhas | ~172 linhas |
| Complexidade | Baixa | Baixa |
| Uso | Referenciado por Funcionários | Referenciado por Centros de Custo |

**Diferença Principal**: Centros de Custo depende de Linhas de Negócio, enquanto Funcionários dependem de Cargos.

---

## 🏗️ Arquitetura e Tecnologias

### Stack Tecnológica

#### Backend
- **Java 17** com Spring Boot 3.2.3
- **Spring Data JPA** para persistência
- **PostgreSQL** como banco de dados
- **Lombok** para redução de boilerplate
- **Jakarta Bean Validation** para validações
- **OpenAPI/Swagger** para documentação da API

#### Frontend
- **React 19.1** com TypeScript
- **Material-UI (MUI) v7** para componentes
- **React Hook Form** para gerenciamento de formulários
- **Axios** para comunicação HTTP
- **React Toastify** para notificações

### Estrutura de Arquivos

```
📁 Backend
├── src/main/java/br/com/techne/sistemafolha/
│   ├── controller/
│   │   └── LinhaNegocioController.java       (76 linhas)
│   ├── service/
│   │   └── LinhaNegocioService.java          (71 linhas)
│   ├── repository/
│   │   └── LinhaNegocioRepository.java       (23 linhas)
│   ├── model/
│   │   └── LinhaNegocio.java                 (20 linhas)
│   ├── dto/
│   │   └── LinhaNegocioDTO.java              (19 linhas)
│   └── exception/
│       └── LinhaNegocioNotFoundException.java

📁 Frontend
├── src/
│   ├── pages/
│   │   └── LinhasNegocio/
│   │       └── index.tsx                     (172 linhas)
│   ├── services/
│   │   └── linhaNegocioService.ts            (34 linhas)
│   └── types/
│       └── index.ts                          (interface LinhaNegocio)
```

**Total de Código**: ~415 linhas (Backend: 209 linhas | Frontend: 206 linhas)

---

## 🔧 Camada Backend

### 1. Entidade JPA - `LinhaNegocio.java`

```java
package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "linhas_negocio")
public class LinhaNegocio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String descricao;
    
    @Column(nullable = false)
    private Boolean ativo = true;
}
```

**Características da Entidade:**

- **Minimalista**: Apenas 3 campos (id, descricao, ativo)
- **@Table**: Mapeia para `linhas_negocio` no banco
- **@GeneratedValue**: ID auto-incrementado
- **nullable = false**: Descrição e ativo são obrigatórios
- **Soft Delete**: Campo `ativo` para exclusão lógica
- **Lombok @Data**: Gera automaticamente getters, setters, equals, hashCode e toString
- **Sem Relacionamentos**: Entidade base do sistema

### 2. DTO - `LinhaNegocioDTO.java`

```java
package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Linha de Negócio")
public record LinhaNegocioDTO(
    @Schema(description = "Identificador único da linha de negócio", example = "1")
    Long id,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição da linha de negócio", example = "Tecnologia da Informação", required = true)
    String descricao,

    @Schema(description = "Indica se a linha de negócio está ativa", example = "true")
    Boolean ativo
) {}
```

**Características do DTO:**

- **Record do Java 14+**: Imutável por padrão, conciso e seguro
- **Validações Jakarta**:
  - `@NotBlank`: Garante que a descrição não seja vazia
  - `@Size(min = 3, max = 100)`: Limite de caracteres
- **Documentação Swagger**: Cada campo possui descrição e exemplo
- **Separação de Concerns**: DTO não possui lógica de negócio

### 3. Repository - `LinhaNegocioRepository.java`

```java
package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.LinhaNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinhaNegocioRepository extends JpaRepository<LinhaNegocio, Long> {
    List<LinhaNegocio> findByAtivoTrue();
    Optional<LinhaNegocio> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Transactional
    @Query("UPDATE LinhaNegocio l SET l.ativo = false WHERE l.id = :id")
    void softDelete(@Param("id") Long id);
}
```

**Características do Repository:**

- **Spring Data JPA**: Métodos gerados automaticamente
- **Query Methods**: Nomenclatura derivada (`findByAtivoTrue`)
- **Filtro Composto**: `findByIdAndAtivoTrue` combina dois critérios
- **Soft Delete**: Query customizada JPQL para exclusão lógica
- **@Modifying**: Indica que a query modifica dados
- **@Transactional**: Garante transação para a operação de soft delete

### 4. Service - `LinhaNegocioService.java`

```java
package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.LinhaNegocioDTO;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.repository.LinhaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinhaNegocioService {
    private final LinhaNegocioRepository linhaNegocioRepository;

    public List<LinhaNegocioDTO> listarTodas() {
        return linhaNegocioRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LinhaNegocioDTO buscarPorId(Long id) {
        return linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));
    }

    @Transactional
    public LinhaNegocioDTO cadastrar(LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = toEntity(dto);
        return toDTO(linhaNegocioRepository.save(linhaNegocio));
    }

    @Transactional
    public LinhaNegocioDTO atualizar(Long id, LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));

        linhaNegocio.setDescricao(dto.descricao());
        return toDTO(linhaNegocioRepository.save(linhaNegocio));
    }

    @Transactional
    public void remover(Long id) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));
        linhaNegocio.setAtivo(false);
        linhaNegocioRepository.save(linhaNegocio);
    }

    private LinhaNegocioDTO toDTO(LinhaNegocio linhaNegocio) {
        return new LinhaNegocioDTO(
            linhaNegocio.getId(),
            linhaNegocio.getDescricao(),
            linhaNegocio.getAtivo()
        );
    }

    private LinhaNegocio toEntity(LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setDescricao(dto.descricao());
        linhaNegocio.setAtivo(true);
        return linhaNegocio;
    }
}
```

**Características do Service:**

- **Injeção de Dependências**: Usa `@RequiredArgsConstructor` do Lombok
- **Filtro de Ativos**: `.filter(ln -> ln.getAtivo())` garante operações apenas em registros ativos
- **Exceções Específicas**: Lança `LinhaNegocioNotFoundException` quando não encontrado
- **@Transactional**: Garante atomicidade nas operações de escrita
- **Streams Java**: Uso funcional para conversão Entity ↔ DTO
- **Soft Delete**: Atualiza apenas o campo `ativo` ao invés de deletar fisicamente
- **Métodos Privados**: `toDTO()` e `toEntity()` para conversões

### 5. Controller - `LinhaNegocioController.java`

```java
package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.LinhaNegocioDTO;
import br.com.techne.sistemafolha.service.LinhaNegocioService;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/linhas-negocio")
@RequiredArgsConstructor
@Tag(name = "Linhas de Negócio", description = "API para gerenciamento de linhas de negócio")
public class LinhaNegocioController {
    private final LinhaNegocioService linhaNegocioService;

    @GetMapping
    @Operation(summary = "Lista todas as linhas de negócio ativas")
    public ResponseEntity<List<LinhaNegocioDTO>> listarTodos() {
        return ResponseEntity.ok(linhaNegocioService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma linha de negócio pelo ID")
    public ResponseEntity<LinhaNegocioDTO> buscarPorId(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(linhaNegocioService.buscarPorId(id));
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova linha de negócio")
    public ResponseEntity<LinhaNegocioDTO> cadastrar(
            @Parameter(description = "Dados da linha de negócio") @Valid @RequestBody LinhaNegocioDTO linhaNegocio) {
        try {
            return ResponseEntity.ok(linhaNegocioService.cadastrar(linhaNegocio));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma linha de negócio existente")
    public ResponseEntity<LinhaNegocioDTO> atualizar(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id,
            @Parameter(description = "Dados atualizados da linha de negócio") @Valid @RequestBody LinhaNegocioDTO linhaNegocio) {
        try {
            return ResponseEntity.ok(linhaNegocioService.atualizar(id, linhaNegocio));
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma linha de negócio")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id) {
        try {
            linhaNegocioService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Características do Controller:**

- **RESTful**: Segue convenções REST (GET, POST, PUT, DELETE)
- **@Valid**: Ativa validação automática do DTO
- **ResponseEntity**: Controle fino sobre o código HTTP de resposta
- **Swagger/OpenAPI**: Documentação automática da API
- **Try-Catch**: Tratamento explícito de exceções para retornar status HTTP corretos
- **Status HTTP Apropriados**:
  - `200 OK` para sucesso (GET, POST, PUT)
  - `204 No Content` para deleção bem-sucedida
  - `404 Not Found` para recurso não encontrado
  - `400 Bad Request` para validações falhas

### 6. Tratamento de Exceções

**LinhaNegocioNotFoundException.java:**
```java
public class LinhaNegocioNotFoundException extends RuntimeException {
    public LinhaNegocioNotFoundException(Long id) {
        super("Linha de Negócio não encontrada com ID: " + id);
    }
}
```

**GlobalExceptionHandler.java (trecho relevante):**
```java
@ExceptionHandler(LinhaNegocioNotFoundException.class)
public ResponseEntity<ErrorResponse> handleLinhaNegocioNotFoundException(
        LinhaNegocioNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

---

## 🎨 Camada Frontend

### 1. Componente Principal - `pages/LinhasNegocio/index.tsx`

```tsx
import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { linhaNegocioService } from '../../services/linhaNegocioService';
import type { LinhaNegocio } from '../../types';

interface LinhaNegocioFormData {
  descricao: string;
}

export default function LinhasNegocio() {
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedLinhaNegocio, setSelectedLinhaNegocio] = useState<LinhaNegocio | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<LinhaNegocioFormData>();

  useEffect(() => {
    carregarLinhasNegocio();
  }, []);

  const carregarLinhasNegocio = async () => {
    try {
      const data = await linhaNegocioService.listarTodos();
      setLinhasNegocio(data);
    } catch (error) {
      toast.error('Erro ao carregar linhas de negócio');
    }
  };

  const handleOpen = (linhaNegocio?: LinhaNegocio) => {
    if (linhaNegocio) {
      setSelectedLinhaNegocio(linhaNegocio);
      setValue('descricao', linhaNegocio.descricao);
    } else {
      setSelectedLinhaNegocio(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: LinhaNegocioFormData) => {
    try {
      if (selectedLinhaNegocio) {
        await linhaNegocioService.atualizar(selectedLinhaNegocio.id, data);
        toast.success('Linha de Negócio atualizada com sucesso');
      } else {
        await linhaNegocioService.cadastrar(data);
        toast.success('Linha de Negócio cadastrada com sucesso');
      }
      handleClose();
      carregarLinhasNegocio();
    } catch (error) {
      toast.error('Erro ao salvar linha de negócio');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir esta linha de negócio?')) {
      try {
        await linhaNegocioService.remover(id);
        toast.success('Linha de Negócio excluída com sucesso');
        carregarLinhasNegocio();
      } catch (error) {
        toast.error('Erro ao excluir linha de negócio');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Linhas de Negócio</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Nova Linha de Negócio
        </Button>
      </Box>

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
                {linhasNegocio.map((linhaNegocio) => (
                  <TableRow key={linhaNegocio.id}>
                    <TableCell>{linhaNegocio.id}</TableCell>
                    <TableCell>{linhaNegocio.descricao}</TableCell>
                    <TableCell>{linhaNegocio.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        onClick={() => handleOpen(linhaNegocio)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(linhaNegocio.id)}
                      >
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

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedLinhaNegocio ? 'Editar Linha de Negócio' : 'Nova Linha de Negócio'}
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
              {selectedLinhaNegocio ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
```

**Características do Componente:**

#### Estados (`useState`)

```tsx
const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
const [open, setOpen] = useState(false);
const [selectedLinhaNegocio, setSelectedLinhaNegocio] = useState<LinhaNegocio | null>(null);
```

- **linhasNegocio**: Lista de linhas de negócio exibida na tabela
- **open**: Controla visibilidade do modal de criação/edição
- **selectedLinhaNegocio**: Armazena a linha de negócio sendo editada (null = nova)

#### Carregamento de Dados (`useEffect`)

```tsx
useEffect(() => {
  carregarLinhasNegocio();
}, []);
```

- **Executa uma vez**: Na montagem do componente
- **Carrega dados**: Busca todas as linhas de negócio ativas

#### Gerenciamento do Formulário

```tsx
const handleOpen = (linhaNegocio?: LinhaNegocio) => {
  if (linhaNegocio) {
    setSelectedLinhaNegocio(linhaNegocio);
    setValue('descricao', linhaNegocio.descricao);
  } else {
    setSelectedLinhaNegocio(null);
    reset();
  }
  setOpen(true);
};
```

- **Modo Edição**: Preenche formulário com dados existentes via `setValue`
- **Modo Criação**: Limpa formulário via `reset()`
- **React Hook Form**: Controla validação e submissão

#### Submissão de Dados

```tsx
const onSubmit = async (data: LinhaNegocioFormData) => {
  try {
    if (selectedLinhaNegocio) {
      await linhaNegocioService.atualizar(selectedLinhaNegocio.id, data);
      toast.success('Linha de Negócio atualizada com sucesso');
    } else {
      await linhaNegocioService.cadastrar(data);
      toast.success('Linha de Negócio cadastrada com sucesso');
    }
    handleClose();
    carregarLinhasNegocio();
  } catch (error) {
    toast.error('Erro ao salvar linha de negócio');
  }
};
```

- **Lógica Condicional**: Detecta se é criação ou atualização
- **Recarregamento**: Chama `carregarLinhasNegocio()` para atualizar a lista
- **Feedback Visual**: Toasts de sucesso/erro

### 2. Service - `linhaNegocioService.ts`

```typescript
import api from './api';
import type { LinhaNegocio } from '../types';

interface LinhaNegocioFormData {
  descricao: string;
}

const linhaNegocioService = {
  listarTodos: async (): Promise<LinhaNegocio[]> => {
    const response = await api.get('/linhas-negocio');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<LinhaNegocio> => {
    const response = await api.get(`/linhas-negocio/${id}`);
    return response.data;
  },

  cadastrar: async (data: LinhaNegocioFormData): Promise<LinhaNegocio> => {
    const response = await api.post('/linhas-negocio', data);
    return response.data;
  },

  atualizar: async (id: number, data: LinhaNegocioFormData): Promise<LinhaNegocio> => {
    const response = await api.put(`/linhas-negocio/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/linhas-negocio/${id}`);
  }
};

export { linhaNegocioService };
```

**Características do Service:**

- **Axios Instance**: Usa a instância configurada em `api.ts` (com interceptors JWT)
- **Tipagem TypeScript**: Tipos explícitos em parâmetros e retornos
- **Interface de Formulário**: `LinhaNegocioFormData` define estrutura de entrada
- **Promise-based**: Todas as funções são assíncronas
- **CRUD Completo**: Cinco operações básicas

### 3. Types - `types/index.ts` (interface LinhaNegocio)

```typescript
export interface LinhaNegocio {
  id: number;
  descricao: string;
  ativo: boolean;
}
```

---

## 🔄 Fluxo de Dados

### 1. Fluxo de Listagem

```
┌─────────────────────────────────────────────────────────────────────┐
│                   LISTAGEM DE LINHAS DE NEGÓCIO                      │
└─────────────────────────────────────────────────────────────────────┘

   [1] useEffect()
        │
        ├─→ carregarLinhasNegocio()
        │
        ├─→ linhaNegocioService.listarTodos()
        │
        ├─→ GET /linhas-negocio  ─────────────────────┐
        │                                              │
        │                                              ▼
        │                        LinhaNegocioController.listarTodos()
        │                                              │
        │                                              ▼
        │                        LinhaNegocioService.listarTodas()
        │                                              │
        │                                              ▼
        │                        Repository.findByAtivoTrue()
        │                                              │
        │                                              ▼
        │                        SELECT * FROM linhas_negocio 
        │                        WHERE ativo = true
        │                                              │
        │   ┌──────────────────────────────────────────┘
        │   │
        │   ├─→ List<LinhaNegocio> entities
        │   │
        │   ├─→ .stream().map(this::toDTO)
        │   │
        │   └─→ List<LinhaNegocioDTO>
        │
        ├─→ setLinhasNegocio(data)
        │
        └─→ Renderização da Tabela
                │
                └─→ Para cada linhaNegocio:
                     Renderiza TableRow com ID, Descrição, Status e Ações
```

### 2. Fluxo de Criação

```
┌─────────────────────────────────────────────────────────────────────┐
│                   CRIAÇÃO DE LINHA DE NEGÓCIO                        │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica "Nova Linha de Negócio"
        │
        ├─→ handleOpen()
        │    │
        │    ├─→ setSelectedLinhaNegocio(null)
        │    ├─→ reset()  (limpa formulário)
        │    └─→ setOpen(true)
        │
        ├─→ Dialog é exibido
        │    │
        │    └─→ TextField (descricao)
        │
   [2] Usuário preenche formulário e clica "Cadastrar"
        │
        ├─→ handleSubmit(onSubmit)
        │
        ├─→ onSubmit(data)
        │    │
        │    └─→ linhaNegocioService.cadastrar(data)
        │
        ├─→ POST /linhas-negocio
        │    Body: { descricao: "Tecnologia da Informação" }
        │
        ├─→ Backend: LinhaNegocioController.cadastrar()
        │    │
        │    ├─→ @Valid valida DTO
        │    │    ├─→ @NotBlank descricao
        │    │    └─→ @Size(min=3, max=100)
        │    │
        │    └─→ LinhaNegocioService.cadastrar(dto)
        │         │
        │         ├─→ toEntity(dto)
        │         │
        │         ├─→ linhaNegocioRepository.save(linhaNegocio)
        │         │    │
        │         │    └─→ INSERT INTO linhas_negocio 
        │         │        (descricao, ativo) 
        │         │        VALUES (?, true)
        │         │
        │         └─→ toDTO(saved)
        │
        ├─→ Frontend recebe LinhaNegocioDTO
        │
        ├─→ toast.success("Linha de Negócio cadastrada com sucesso")
        │
        ├─→ handleClose()  (fecha modal)
        │
        └─→ carregarLinhasNegocio()  (atualiza lista)
```

### 3. Fluxo de Edição

```
┌─────────────────────────────────────────────────────────────────────┐
│                   EDIÇÃO DE LINHA DE NEGÓCIO                         │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica no ícone de edição
        │
        ├─→ handleOpen(linhaNegocio)
        │    │
        │    ├─→ setSelectedLinhaNegocio(linhaNegocio)
        │    ├─→ setValue('descricao', linhaNegocio.descricao)
        │    └─→ setOpen(true)
        │
        ├─→ Dialog é exibido com dados preenchidos
        │
   [2] Usuário edita e clica "Atualizar"
        │
        ├─→ handleSubmit(onSubmit)
        │
        ├─→ onSubmit(data)
        │    │
        │    └─→ linhaNegocioService.atualizar(id, data)
        │
        ├─→ PUT /linhas-negocio/{id}
        │    Body: { descricao: "TI e Inovação" }
        │
        ├─→ Backend: LinhaNegocioController.atualizar()
        │    │
        │    └─→ LinhaNegocioService.atualizar(id, dto)
        │         │
        │         ├─→ Busca LinhaNegocio existente
        │         │    └─→ Se não encontrar: throw LinhaNegocioNotFoundException
        │         │
        │         ├─→ linhaNegocio.setDescricao(dto.descricao())
        │         │
        │         ├─→ linhaNegocioRepository.save(linhaNegocio)
        │         │    │
        │         │    └─→ UPDATE linhas_negocio 
        │         │        SET descricao = ?
        │         │        WHERE id = ?
        │         │
        │         └─→ toDTO(updated)
        │
        ├─→ toast.success("Linha de Negócio atualizada com sucesso")
        │
        ├─→ handleClose()
        │
        └─→ carregarLinhasNegocio()
```

### 4. Fluxo de Deleção

```
┌─────────────────────────────────────────────────────────────────────┐
│                   DELEÇÃO DE LINHA DE NEGÓCIO                        │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica no ícone de deletar
        │
        ├─→ handleDelete(id)
        │
        ├─→ window.confirm("Tem certeza que deseja excluir...?")
        │    │
        │    ├─→ Se cancelar: Fluxo termina
        │    │
        │    └─→ Se confirmar:
        │
        ├─→ linhaNegocioService.remover(id)
        │
        ├─→ DELETE /linhas-negocio/{id}
        │
        ├─→ Backend: LinhaNegocioController.remover()
        │    │
        │    └─→ LinhaNegocioService.remover(id)
        │         │
        │         ├─→ Busca LinhaNegocio
        │         │    └─→ Se não encontrar: throw LinhaNegocioNotFoundException
        │         │
        │         ├─→ linhaNegocio.setAtivo(false)  ← Soft Delete
        │         │
        │         └─→ linhaNegocioRepository.save(linhaNegocio)
        │              │
        │              └─→ UPDATE linhas_negocio 
        │                  SET ativo = false
        │                  WHERE id = ?
        │
        ├─→ Status: 204 No Content
        │
        ├─→ toast.success("Linha de Negócio excluída com sucesso")
        │
        └─→ carregarLinhasNegocio()  (atualiza lista)
```

---

## 💻 Exemplos de Código

### Exemplo 1: Cadastrar Linha de Negócio

**Request:**
```http
POST /linhas-negocio
Content-Type: application/json

{
  "descricao": "Tecnologia da Informação"
}
```

**Response (200 OK):**
```json
{
  "id": 5,
  "descricao": "Tecnologia da Informação",
  "ativo": true
}
```

**SQL Executado:**
```sql
INSERT INTO linhas_negocio (descricao, ativo) 
VALUES ('Tecnologia da Informação', true);
```

---

### Exemplo 2: Listar Todas as Linhas de Negócio

**Request:**
```http
GET /linhas-negocio
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "descricao": "Tecnologia da Informação",
    "ativo": true
  },
  {
    "id": 2,
    "descricao": "Recursos Humanos",
    "ativo": true
  },
  {
    "id": 3,
    "descricao": "Financeiro",
    "ativo": true
  }
]
```

**SQL Executado:**
```sql
SELECT * FROM linhas_negocio WHERE ativo = true;
```

---

### Exemplo 3: Atualizar Linha de Negócio

**Request:**
```http
PUT /linhas-negocio/5
Content-Type: application/json

{
  "descricao": "TI e Inovação"
}
```

**Response (200 OK):**
```json
{
  "id": 5,
  "descricao": "TI e Inovação",
  "ativo": true
}
```

**SQL Executado:**
```sql
UPDATE linhas_negocio 
SET descricao = 'TI e Inovação' 
WHERE id = 5;
```

---

### Exemplo 4: Soft Delete

**Request:**
```http
DELETE /linhas-negocio/5
```

**Response (204 No Content):**
```
(corpo vazio)
```

**SQL Executado:**
```sql
UPDATE linhas_negocio 
SET ativo = false 
WHERE id = 5;
```

**Importante**: O registro não é deletado fisicamente, apenas marcado como inativo.

---

### Exemplo 5: Tentativa de Buscar Linha de Negócio Inexistente

**Request:**
```http
GET /linhas-negocio/999
```

**Response (404 NOT FOUND):**
```json
{
  "status": 404,
  "message": "Linha de Negócio não encontrada com ID: 999",
  "timestamp": "2025-10-16T10:30:00"
}
```

---

## 🎯 Padrões e Boas Práticas

### 1. Backend

#### Soft Delete
```java
// ❌ Evitar: Delete físico
linhaNegocioRepository.deleteById(id);

// ✅ Preferir: Soft delete
linhaNegocio.setAtivo(false);
linhaNegocioRepository.save(linhaNegocio);
```

**Benefícios:**
- Preserva integridade referencial (Centros de Custo dependem de Linhas de Negócio)
- Permite auditoria e recuperação de dados
- Evita cascata de deleções indesejadas

---

#### DTOs Imutáveis com Records
```java
// ✅ Records são imutáveis, concisos e seguros
public record LinhaNegocioDTO(
    Long id,
    String descricao,
    Boolean ativo
) {}
```

**Benefícios:**
- Imutabilidade garante thread-safety
- Menos código boilerplate
- Semântica clara de "objeto de dados"

---

#### Uso de Streams
```java
// ✅ Streams para transformações funcionais
return linhaNegocioRepository.findByAtivoTrue().stream()
    .map(this::toDTO)
    .collect(Collectors.toList());
```

**Benefícios:**
- Código declarativo e legível
- Facilita testes unitários
- Evita loops manuais e mutações

---

#### Tratamento de Exceções no Controller
```java
// ✅ Try-catch explícito para controle de status HTTP
@GetMapping("/{id}")
public ResponseEntity<LinhaNegocioDTO> buscarPorId(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(linhaNegocioService.buscarPorId(id));
    } catch (LinhaNegocioNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
```

**Benefícios:**
- Controle preciso sobre status HTTP
- Mensagens de erro claras
- Segue padrões REST

---

### 2. Frontend

#### React Hook Form para Validação
```tsx
// ✅ Validação declarativa com React Hook Form
<TextField
  {...register('descricao', { 
    required: 'Descrição é obrigatória',
    minLength: { value: 3, message: 'Mínimo 3 caracteres' }
  })}
/>
```

**Benefícios:**
- Validação antes de enviar ao backend
- Reduz requisições desnecessárias
- Feedback imediato ao usuário

---

#### Toasts para Feedback Visual
```tsx
// ✅ Feedback claro para o usuário
try {
  await linhaNegocioService.cadastrar(data);
  toast.success('Linha de Negócio cadastrada com sucesso');
} catch (error) {
  toast.error('Erro ao salvar linha de negócio');
}
```

**Benefícios:**
- UX responsivo
- Confirmação visual de ações
- Tratamento consistente de erros

---

#### Confirmação de Deleção
```tsx
// ✅ Confirmar antes de deletar
const handleDelete = async (id: number) => {
  if (window.confirm('Tem certeza que deseja excluir esta linha de negócio?')) {
    await linhaNegocioService.remover(id);
  }
};
```

**Benefícios:**
- Evita deleções acidentais
- Melhora UX
- Padrão amplamente reconhecido

---

### 3. Arquitetura

#### Separação de Camadas

```
┌──────────────────────────────────────────────────────────────┐
│                      ARQUITETURA CLEAN                        │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  Controller (REST)  →  Service (Lógica)  →  Repository (DB)  │
│         ↓                    ↓                      ↓         │
│        DTOs            Entities (JPA)          Database       │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

**Responsabilidades:**

| Camada | Responsabilidade | Não Deve Fazer |
|--------|------------------|----------------|
| **Controller** | Receber requests, validar DTOs, retornar responses | Lógica de negócio, acesso direto ao DB |
| **Service** | Regras de negócio, coordenar repositories, transformar entidades | Acesso direto ao DB, construção de responses |
| **Repository** | Queries ao banco, CRUD básico | Lógica de negócio, transformação de DTOs |
| **DTO** | Transferência de dados, validações básicas | Lógica de negócio, acesso ao DB |
| **Entity** | Representação de tabelas, relacionamentos JPA | Lógica de apresentação |

---

## 🚀 Melhorias Futuras

### 1. Paginação

**Backend:**
```java
@GetMapping
public ResponseEntity<Page<LinhaNegocioDTO>> listarTodos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(linhaNegocioService.listarTodas(pageable));
}
```

**Service:**
```java
public Page<LinhaNegocioDTO> listarTodas(Pageable pageable) {
    return linhaNegocioRepository.findByAtivoTrue(pageable)
        .map(this::toDTO);
}
```

**Repository:**
```java
Page<LinhaNegocio> findByAtivoTrue(Pageable pageable);
```

**Frontend:**
```tsx
import { TablePagination } from '@mui/material';

<TablePagination
  component="div"
  count={totalElements}
  page={page}
  onPageChange={handleChangePage}
  rowsPerPage={rowsPerPage}
  onRowsPerPageChange={handleChangeRowsPerPage}
/>
```

---

### 2. Busca por Nome

**Backend:**
```java
@GetMapping("/buscar")
public ResponseEntity<List<LinhaNegocioDTO>> buscarPorDescricao(
    @RequestParam String descricao
) {
    return ResponseEntity.ok(linhaNegocioService.buscarPorDescricao(descricao));
}
```

**Repository:**
```java
List<LinhaNegocio> findByAtivoTrueAndDescricaoContainingIgnoreCase(String descricao);
```

**Frontend:**
```tsx
const [searchTerm, setSearchTerm] = useState('');

const filteredLinhas = linhasNegocio.filter(ln =>
  ln.descricao.toLowerCase().includes(searchTerm.toLowerCase())
);

<TextField
  label="Buscar"
  value={searchTerm}
  onChange={(e) => setSearchTerm(e.target.value)}
/>
```

---

### 3. Validação de Duplicidade

**Backend:**
```java
@Transactional
public LinhaNegocioDTO cadastrar(LinhaNegocioDTO dto) {
    if (linhaNegocioRepository.existsByDescricaoAndAtivoTrue(dto.descricao())) {
        throw new LinhaNegocioDuplicadaException(dto.descricao());
    }
    
    LinhaNegocio linhaNegocio = toEntity(dto);
    return toDTO(linhaNegocioRepository.save(linhaNegocio));
}
```

**Repository:**
```java
boolean existsByDescricaoAndAtivoTrue(String descricao);
```

---

### 4. Ordenação Customizável

**Backend:**
```java
@GetMapping
public ResponseEntity<List<LinhaNegocioDTO>> listarTodos(
    @RequestParam(defaultValue = "descricao") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    return ResponseEntity.ok(linhaNegocioService.listarTodas(sort));
}
```

**Frontend:**
```tsx
import { TableSortLabel } from '@mui/material';

<TableCell>
  <TableSortLabel
    active={orderBy === 'descricao'}
    direction={orderBy === 'descricao' ? order : 'asc'}
    onClick={() => handleRequestSort('descricao')}
  >
    Descrição
  </TableSortLabel>
</TableCell>
```

---

### 5. Auditoria Completa

**Entidade:**
```java
@Data
@Entity
@Table(name = "linhas_negocio")
@EntityListeners(AuditingEntityListener.class)
public class LinhaNegocio {
    // ... campos existentes
    
    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    
    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
```

**Migration:**
```sql
ALTER TABLE linhas_negocio 
ADD COLUMN criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN atualizado_em TIMESTAMP;
```

---

### 6. Exportação para Excel/CSV

**Backend:**
```java
@GetMapping("/exportar")
public ResponseEntity<byte[]> exportarLinhasNegocio() {
    List<LinhaNegocioDTO> linhas = linhaNegocioService.listarTodas();
    byte[] excelBytes = excelService.gerarExcel(linhas);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=linhas_negocio.xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelBytes);
}
```

**Frontend:**
```tsx
const handleExportar = async () => {
  const response = await api.get('/linhas-negocio/exportar', { 
    responseType: 'blob' 
  });
  
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'linhas_negocio.xlsx');
  document.body.appendChild(link);
  link.click();
  link.remove();
};
```

---

### 7. Skeleton Loading

**Frontend:**
```tsx
import { Skeleton } from '@mui/material';

{loading ? (
  <TableBody>
    {[...Array(5)].map((_, index) => (
      <TableRow key={index}>
        <TableCell><Skeleton /></TableCell>
        <TableCell><Skeleton /></TableCell>
        <TableCell><Skeleton /></TableCell>
        <TableCell><Skeleton /></TableCell>
      </TableRow>
    ))}
  </TableBody>
) : (
  <TableBody>
    {linhasNegocio.map((linhaNegocio) => (
      // ... renderização normal
    ))}
  </TableBody>
)}
```

---

### 8. Badge com Contagem de Centros de Custo

**Backend:**
```java
// DTO estendido
public record LinhaNegocioComContagemDTO(
    Long id,
    String descricao,
    Boolean ativo,
    Long quantidadeCentrosCusto
) {}

// Service
public List<LinhaNegocioComContagemDTO> listarTodasComContagem() {
    return linhaNegocioRepository.findByAtivoTrue().stream()
        .map(ln -> new LinhaNegocioComContagemDTO(
            ln.getId(),
            ln.getDescricao(),
            ln.getAtivo(),
            centroCustoRepository.countByLinhaNegocioIdAndAtivoTrue(ln.getId())
        ))
        .collect(Collectors.toList());
}
```

**Frontend:**
```tsx
import { Badge } from '@mui/material';

<TableCell>
  {linhaNegocio.descricao}
  <Badge 
    badgeContent={linhaNegocio.quantidadeCentrosCusto} 
    color="primary"
    style={{ marginLeft: 8 }}
  />
</TableCell>
```

---

## 📝 Conclusão

A tela de **Linhas de Negócio** é um exemplo perfeito de **CRUD simples e bem estruturado**. Ela demonstra:

✅ **Entidade base** sem relacionamentos obrigatórios  
✅ **Soft Delete** preservando integridade referencial  
✅ **Validações** consistentes frontend e backend  
✅ **Código minimalista** (~415 linhas totais)  
✅ **Arquitetura limpa** com separação de camadas  
✅ **Padrões RESTful** bem implementados  

Esta implementação serve como **template** para outras entidades base do sistema, como:

- Cargos (praticamente idêntico)
- Tipos de Rubrica
- Status de aprovação
- Qualquer entidade simples sem relacionamentos

**Diferença entre Linhas de Negócio e Centros de Custo:**
- **Linhas de Negócio**: Entidade base (sem dependências)
- **Centros de Custo**: Depende de Linhas de Negócio (@ManyToOne)

---

## 📊 Estatísticas Finais

| Métrica | Valor |
|---------|-------|
| **Total de Código** | ~415 linhas |
| **Backend** | 209 linhas |
| **Frontend** | 206 linhas |
| **Arquivos Backend** | 5 |
| **Arquivos Frontend** | 2 |
| **Endpoints REST** | 5 |
| **Operações CRUD** | 5 (List, Get, Create, Update, Delete) |
| **Relacionamentos JPA** | 0 (entidade base) |
| **Complexidade** | **Baixa** (CRUD simples) |
| **Uso no Sistema** | Referenciado por Centros de Custo |

---

**Documento gerado em:** 16 de Outubro de 2025  
**Versão do Sistema:** 1.0  
**Autor:** Sistema de Documentação Automática

