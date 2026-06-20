# 📊 Conhecimento Consolidado - Tela de Centros de Custo

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

A tela de **Centros de Custo** é uma implementação CRUD (Create, Read, Update, Delete) que gerencia os centros de custo da empresa. Diferentemente da tela de Cargos (que é completamente independente), esta tela possui um **relacionamento obrigatório com Linha de Negócio**, demonstrando como implementar CRUDs com dependências de outras entidades.

### Características Principais

- ✅ **CRUD Completo**: Cadastrar, listar, atualizar e remover centros de custo
- ✅ **Relacionamento Obrigatório**: Cada centro de custo deve estar associado a uma linha de negócio
- ✅ **Soft Delete**: Exclusão lógica preservando integridade referencial
- ✅ **Validações**: Frontend e backend com validações consistentes
- ✅ **UI Simples e Funcional**: Interface Material-UI intuitiva
- ✅ **Filtragem por Linha de Negócio**: Endpoint adicional para busca segmentada

### Diferenças em Relação à Tela de Cargos

| Aspecto | Cargos | Centros de Custo |
|---------|--------|------------------|
| Campos | ID, Descrição, Ativo | ID, Descrição, Ativo, Linha de Negócio |
| Relacionamentos | Nenhum | @ManyToOne com LinhaNegocio |
| Formulário | 1 campo (descrição) | 2 campos (descrição + select de linha) |
| Endpoints | CRUD básico | CRUD + filtro por linha de negócio |
| Validação | Descrição obrigatória | Descrição + Linha de Negócio obrigatórias |
| Complexidade | Baixa | Média (gerenciamento de relacionamento) |

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
│   │   └── CentroCustoController.java       (64 linhas)
│   ├── service/
│   │   └── CentroCustoService.java          (92 linhas)
│   ├── repository/
│   │   └── CentroCustoRepository.java       (25 linhas)
│   ├── model/
│   │   └── CentroCusto.java                 (24 linhas)
│   ├── dto/
│   │   └── CentroCustoDTO.java              (24 linhas)
│   └── exception/
│       └── CentroCustoNotFoundException.java

📁 Frontend
├── src/
│   ├── pages/
│   │   └── CentrosCusto/
│   │       └── index.tsx                     (202 linhas)
│   ├── services/
│   │   └── centroCustoService.ts            (40 linhas)
│   └── types/
│       └── index.ts                          (interface CentroCusto)
```

**Total de Código**: ~471 linhas (Backend: 229 linhas | Frontend: 242 linhas)

---

## 🔧 Camada Backend

### 1. Entidade JPA - `CentroCusto.java`

```java
package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "centros_custo")
public class CentroCusto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String descricao;
    
    @Column(nullable = false)
    private Boolean ativo = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_negocio_id", nullable = false)
    private LinhaNegocio linhaNegocio;
}
```

**Características da Entidade:**

- **@ManyToOne com LinhaNegocio**: Cada centro de custo pertence a uma linha de negócio
- **FetchType.LAZY**: Carregamento preguiçoso para otimizar performance
- **nullable = false**: Relacionamento obrigatório a nível de banco de dados
- **Soft Delete**: Campo `ativo` para exclusão lógica
- **Lombok @Data**: Gera automaticamente getters, setters, equals, hashCode e toString

### 2. DTO - `CentroCustoDTO.java`

```java
package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Centro de Custo")
public record CentroCustoDTO(
    @Schema(description = "Identificador único do centro de custo", example = "1")
    Long id,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição do centro de custo", example = "Desenvolvimento", required = true)
    String descricao,

    @Schema(description = "Indica se o centro de custo está ativo", example = "true")
    Boolean ativo,

    @NotNull(message = "A linha de negócio é obrigatória")
    @Schema(description = "ID da linha de negócio associada ao centro de custo", example = "1", required = true)
    Long linhaNegocioId
) {}
```

**Características do DTO:**

- **Record do Java 14+**: Imutável por padrão, conciso e seguro
- **Validações Jakarta**:
  - `@NotBlank`: Garante que a descrição não seja vazia
  - `@Size(min = 3, max = 100)`: Limite de caracteres
  - `@NotNull`: Linha de negócio obrigatória
- **Documentação Swagger**: Cada campo possui descrição e exemplo
- **Separação de Concerns**: DTO não possui lógica de negócio

### 3. Repository - `CentroCustoRepository.java`

```java
package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {
    boolean existsByDescricao(String descricao);

    @Modifying
    @Transactional
    @Query("UPDATE CentroCusto c SET c.ativo = false WHERE c.id = :id")
    void softDelete(@Param("id") Long id);

    List<CentroCusto> findByAtivoTrue();
    
    List<CentroCusto> findByLinhaNegocioIdAndAtivoTrue(Long linhaNegocioId);
}
```

**Características do Repository:**

- **Spring Data JPA**: Métodos gerados automaticamente
- **Query Methods**: Nomenclatura derivada (`findByAtivoTrue`)
- **Filtro Composto**: `findByLinhaNegocioIdAndAtivoTrue` combina dois critérios
- **Soft Delete**: Query customizada para exclusão lógica
- **Validação de Duplicidade**: `existsByDescricao` para evitar nomes repetidos

### 4. Service - `CentroCustoService.java`

```java
package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.CentroCustoDTO;
import br.com.techne.sistemafolha.exception.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.repository.LinhaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroCustoService {
    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;

    public List<CentroCustoDTO> listarTodas() {
        return centroCustoRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CentroCustoDTO> listarPorLinhaNegocio(Long linhaNegocioId) {
        return centroCustoRepository.findByLinhaNegocioIdAndAtivoTrue(linhaNegocioId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CentroCustoDTO buscarPorId(Long id) {
        return centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new CentroCustoNotFoundException(id));
    }

    @Transactional
    public CentroCustoDTO cadastrar(CentroCustoDTO dto) {
        // Valida se a linha de negócio existe e está ativa
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        CentroCusto centroCusto = toEntity(dto);
        centroCusto.setLinhaNegocio(linhaNegocio);
        return toDTO(centroCustoRepository.save(centroCusto));
    }

    @Transactional
    public CentroCustoDTO atualizar(Long id, CentroCustoDTO dto) {
        CentroCusto centroCusto = centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(id));

        // Valida se a nova linha de negócio existe e está ativa
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        centroCusto.setDescricao(dto.descricao());
        centroCusto.setLinhaNegocio(linhaNegocio);
        return toDTO(centroCustoRepository.save(centroCusto));
    }

    @Transactional
    public void remover(Long id) {
        CentroCusto centroCusto = centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(id));
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);
    }

    private CentroCustoDTO toDTO(CentroCusto centroCusto) {
        return new CentroCustoDTO(
            centroCusto.getId(),
            centroCusto.getDescricao(),
            centroCusto.getAtivo(),
            centroCusto.getLinhaNegocio().getId()
        );
    }

    private CentroCusto toEntity(CentroCustoDTO dto) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setDescricao(dto.descricao());
        centroCusto.setAtivo(true);
        return centroCusto;
    }
}
```

**Características do Service:**

- **Injeção de Dependências**: Usa `@RequiredArgsConstructor` do Lombok
- **Validação de Relacionamento**: Verifica se a linha de negócio existe antes de salvar
- **Filtro de Ativos**: `.filter(ln -> ln.getAtivo())` garante referência a entidades ativas
- **Exceções Específicas**: Lança `CentroCustoNotFoundException` e `LinhaNegocioNotFoundException`
- **@Transactional**: Garante atomicidade nas operações de escrita
- **Streams Java**: Uso funcional para conversão Entity ↔ DTO
- **Soft Delete**: Atualiza apenas o campo `ativo` ao invés de deletar fisicamente

### 5. Controller - `CentroCustoController.java`

```java
package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.CentroCustoDTO;
import br.com.techne.sistemafolha.service.CentroCustoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/centros-custo")
@RequiredArgsConstructor
@Tag(name = "Centros de Custo", description = "API para gerenciamento de centros de custo")
public class CentroCustoController {
    private final CentroCustoService centroCustoService;

    @GetMapping
    @Operation(summary = "Lista todos os centros de custo ativos")
    public ResponseEntity<List<CentroCustoDTO>> listarTodos() {
        return ResponseEntity.ok(centroCustoService.listarTodas());
    }

    @GetMapping("/linha-negocio/{linhaNegocioId}")
    @Operation(summary = "Lista centros de custo por linha de negócio")
    public ResponseEntity<List<CentroCustoDTO>> listarPorLinhaNegocio(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long linhaNegocioId) {
        return ResponseEntity.ok(centroCustoService.listarPorLinhaNegocio(linhaNegocioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um centro de custo pelo ID")
    public ResponseEntity<CentroCustoDTO> buscarPorId(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id) {
        return ResponseEntity.ok(centroCustoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo centro de custo")
    public ResponseEntity<CentroCustoDTO> cadastrar(
            @Parameter(description = "Dados do centro de custo") @Valid @RequestBody CentroCustoDTO centroCusto) {
        return ResponseEntity.ok(centroCustoService.cadastrar(centroCusto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um centro de custo existente")
    public ResponseEntity<CentroCustoDTO> atualizar(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do centro de custo") @Valid @RequestBody CentroCustoDTO centroCusto) {
        return ResponseEntity.ok(centroCustoService.atualizar(id, centroCusto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um centro de custo")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id) {
        centroCustoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Características do Controller:**

- **RESTful**: Segue convenções REST (GET, POST, PUT, DELETE)
- **@Valid**: Ativa validação automática do DTO
- **ResponseEntity**: Controle fino sobre o código HTTP de resposta
- **Swagger/OpenAPI**: Documentação automática da API
- **Endpoint Adicional**: `/linha-negocio/{id}` para filtrar centros de custo
- **Status HTTP Apropriados**:
  - `200 OK` para sucesso (GET, POST, PUT)
  - `204 No Content` para deleção bem-sucedida

### 6. Tratamento de Exceções

**CentroCustoNotFoundException.java:**
```java
public class CentroCustoNotFoundException extends RuntimeException {
    public CentroCustoNotFoundException(Long id) {
        super("Centro de Custo não encontrado com ID: " + id);
    }
}
```

**GlobalExceptionHandler.java (trecho relevante):**
```java
@ExceptionHandler(CentroCustoNotFoundException.class)
public ResponseEntity<ErrorResponse> handleCentroCustoNotFoundException(
        CentroCustoNotFoundException ex) {
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

### 1. Componente Principal - `pages/CentrosCusto/index.tsx`

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
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
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
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';
import type { CentroCusto, LinhaNegocio } from '../../types';

interface CentroCustoFormData {
  descricao: string;
  linhaNegocioId: number;
}

export default function CentrosCusto() {
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedCentroCusto, setSelectedCentroCusto] = useState<CentroCusto | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<CentroCustoFormData>();

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      const [centrosCustoData, linhasNegocioData] = await Promise.all([
        centroCustoService.listarTodos(),
        linhaNegocioService.listarTodos(),
      ]);
      setCentrosCusto(centrosCustoData);
      setLinhasNegocio(linhasNegocioData);
    } catch (error) {
      toast.error('Erro ao carregar dados');
    }
  };

  const handleOpen = (centroCusto?: CentroCusto) => {
    if (centroCusto) {
      setSelectedCentroCusto(centroCusto);
      setValue('descricao', centroCusto.descricao);
      setValue('linhaNegocioId', centroCusto.linhaNegocioId);
    } else {
      setSelectedCentroCusto(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: CentroCustoFormData) => {
    try {
      if (selectedCentroCusto) {
        await centroCustoService.atualizar(selectedCentroCusto.id, data);
        toast.success('Centro de Custo atualizado com sucesso');
      } else {
        await centroCustoService.cadastrar(data);
        toast.success('Centro de Custo cadastrado com sucesso');
      }
      handleClose();
      carregarDados();
    } catch (error) {
      toast.error('Erro ao salvar centro de custo');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este centro de custo?')) {
      try {
        await centroCustoService.remover(id);
        toast.success('Centro de Custo excluído com sucesso');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao excluir centro de custo');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Centros de Custo</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Novo Centro de Custo
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
                  <TableCell>Linha de Negócio</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {centrosCusto.map((centroCusto) => {
                  const linhaNegocio = linhasNegocio.find(ln => ln.id === centroCusto.linhaNegocioId);
                  return (
                    <TableRow key={centroCusto.id}>
                      <TableCell>{centroCusto.id}</TableCell>
                      <TableCell>{centroCusto.descricao}</TableCell>
                      <TableCell>{linhaNegocio?.descricao || 'N/A'}</TableCell>
                      <TableCell>{centroCusto.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                      <TableCell align="center">
                        <IconButton
                          color="primary"
                          onClick={() => handleOpen(centroCusto)}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          color="error"
                          onClick={() => handleDelete(centroCusto.id)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedCentroCusto ? 'Editar Centro de Custo' : 'Novo Centro de Custo'}
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
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Linha de Negócio</InputLabel>
              <Select
                {...register('linhaNegocioId', { required: 'Linha de Negócio é obrigatória' })}
                label="Linha de Negócio"
              >
                {linhasNegocio.map((linhaNegocio) => (
                  <MenuItem key={linhaNegocio.id} value={linhaNegocio.id}>
                    {linhaNegocio.descricao}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              {selectedCentroCusto ? 'Atualizar' : 'Cadastrar'}
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
const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
const [open, setOpen] = useState(false);
const [selectedCentroCusto, setSelectedCentroCusto] = useState<CentroCusto | null>(null);
```

- **centrosCusto**: Lista de centros de custo exibida na tabela
- **linhasNegocio**: Lista de linhas de negócio para o select do formulário
- **open**: Controla visibilidade do modal de criação/edição
- **selectedCentroCusto**: Armazena o centro de custo sendo editado (null = novo)

#### Carregamento de Dados (`useEffect` + `Promise.all`)

```tsx
const carregarDados = async () => {
  try {
    const [centrosCustoData, linhasNegocioData] = await Promise.all([
      centroCustoService.listarTodos(),
      linhaNegocioService.listarTodos(),
    ]);
    setCentrosCusto(centrosCustoData);
    setLinhasNegocio(linhasNegocioData);
  } catch (error) {
    toast.error('Erro ao carregar dados');
  }
};
```

- **Promise.all**: Carrega centros de custo e linhas de negócio em paralelo
- **Otimização de Performance**: Reduz tempo total de carregamento
- **Tratamento de Erro**: Toast genérico em caso de falha

#### Gerenciamento do Formulário

```tsx
const handleOpen = (centroCusto?: CentroCusto) => {
  if (centroCusto) {
    setSelectedCentroCusto(centroCusto);
    setValue('descricao', centroCusto.descricao);
    setValue('linhaNegocioId', centroCusto.linhaNegocioId);
  } else {
    setSelectedCentroCusto(null);
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
const onSubmit = async (data: CentroCustoFormData) => {
  try {
    if (selectedCentroCusto) {
      await centroCustoService.atualizar(selectedCentroCusto.id, data);
      toast.success('Centro de Custo atualizado com sucesso');
    } else {
      await centroCustoService.cadastrar(data);
      toast.success('Centro de Custo cadastrado com sucesso');
    }
    handleClose();
    carregarDados();
  } catch (error) {
    toast.error('Erro ao salvar centro de custo');
  }
};
```

- **Lógica Condicional**: Detecta se é criação ou atualização
- **Recarregamento**: Chama `carregarDados()` para atualizar a lista
- **Feedback Visual**: Toasts de sucesso/erro

#### Renderização da Linha de Negócio na Tabela

```tsx
{centrosCusto.map((centroCusto) => {
  const linhaNegocio = linhasNegocio.find(ln => ln.id === centroCusto.linhaNegocioId);
  return (
    <TableRow key={centroCusto.id}>
      <TableCell>{centroCusto.id}</TableCell>
      <TableCell>{centroCusto.descricao}</TableCell>
      <TableCell>{linhaNegocio?.descricao || 'N/A'}</TableCell>
      <TableCell>{centroCusto.ativo ? 'Ativo' : 'Inativo'}</TableCell>
      {/* ... */}
    </TableRow>
  );
})}
```

- **Join Manual**: Busca a descrição da linha de negócio via `find()`
- **Fallback**: Exibe "N/A" se a linha de negócio não for encontrada
- **Optional Chaining**: `linhaNegocio?.descricao` evita erros de referência nula

### 2. Service - `centroCustoService.ts`

```typescript
import api from './api';
import type { CentroCusto } from '../types';

interface CentroCustoFormData {
  descricao: string;
  linhaNegocioId: number;
}

const centroCustoService = {
  listarTodos: async (): Promise<CentroCusto[]> => {
    const response = await api.get('/centros-custo');
    return response.data;
  },

  buscarPorId: async (id: number): Promise<CentroCusto> => {
    const response = await api.get(`/centros-custo/${id}`);
    return response.data;
  },

  listarPorLinhaNegocio: async (linhaNegocioId: number): Promise<CentroCusto[]> => {
    const response = await api.get(`/centros-custo/linha-negocio/${linhaNegocioId}`);
    return response.data;
  },

  cadastrar: async (data: CentroCustoFormData): Promise<CentroCusto> => {
    const response = await api.post('/centros-custo', data);
    return response.data;
  },

  atualizar: async (id: number, data: CentroCustoFormData): Promise<CentroCusto> => {
    const response = await api.put(`/centros-custo/${id}`, data);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/centros-custo/${id}`);
  }
};

export { centroCustoService };
```

**Características do Service:**

- **Axios Instance**: Usa a instância configurada em `api.ts` (com interceptors JWT)
- **Tipagem TypeScript**: Tipos explícitos em parâmetros e retornos
- **Método Adicional**: `listarPorLinhaNegocio` para filtrar por linha
- **Interface de Formulário**: `CentroCustoFormData` define estrutura de entrada
- **Promise-based**: Todas as funções são assíncronas

### 3. Types - `types/index.ts` (interface CentroCusto)

```typescript
export interface CentroCusto {
  id: number;
  descricao: string;
  ativo: boolean;
  linhaNegocioId: number;
}

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
│                        LISTAGEM DE CENTROS DE CUSTO                  │
└─────────────────────────────────────────────────────────────────────┘

   [1] useEffect()
        │
        ├─→ carregarDados()
        │
        ├─→ Promise.all([
        │     centroCustoService.listarTodos(),
        │     linhaNegocioService.listarTodos()
        │   ])
        │
        ├─→ GET /centros-custo  ──────────────────┐
        │                                          │
        │                                          ▼
        │                        CentroCustoController.listarTodos()
        │                                          │
        │                                          ▼
        │                        CentroCustoService.listarTodas()
        │                                          │
        │                                          ▼
        │                        Repository.findByAtivoTrue()
        │                                          │
        │                                          ▼
        │                        SELECT * FROM centros_custo 
        │                        WHERE ativo = true
        │                                          │
        │   ┌──────────────────────────────────────┘
        │   │
        │   ├─→ List<CentroCusto> entities
        │   │
        │   ├─→ .stream().map(this::toDTO)
        │   │
        │   └─→ List<CentroCustoDTO>
        │
        ├─→ setCentrosCusto(data)
        │
        ├─→ setLinhasNegocio(data)
        │
        └─→ Renderização da Tabela
                │
                ├─→ Para cada centroCusto:
                │     │
                │     ├─→ Busca linhaNegocio via find()
                │     │
                │     └─→ Renderiza TableRow com ID, Descrição, 
                │         Linha de Negócio, Status e Ações
```

### 2. Fluxo de Criação

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CRIAÇÃO DE CENTRO DE CUSTO                       │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica "Novo Centro de Custo"
        │
        ├─→ handleOpen()
        │    │
        │    ├─→ setSelectedCentroCusto(null)
        │    ├─→ reset()  (limpa formulário)
        │    └─→ setOpen(true)
        │
        ├─→ Dialog é exibido
        │    │
        │    ├─→ TextField (descricao)
        │    └─→ Select (linhaNegocioId)
        │         └─→ Preenchido com linhasNegocio
        │
   [2] Usuário preenche formulário e clica "Cadastrar"
        │
        ├─→ handleSubmit(onSubmit)
        │
        ├─→ onSubmit(data)
        │    │
        │    └─→ centroCustoService.cadastrar(data)
        │
        ├─→ POST /centros-custo
        │    Body: { descricao, linhaNegocioId }
        │
        ├─→ Backend: CentroCustoController.cadastrar()
        │    │
        │    ├─→ @Valid valida DTO
        │    │    ├─→ @NotBlank descricao
        │    │    ├─→ @Size(min=3, max=100)
        │    │    └─→ @NotNull linhaNegocioId
        │    │
        │    └─→ CentroCustoService.cadastrar(dto)
        │         │
        │         ├─→ Busca LinhaNegocio no banco
        │         │    └─→ Se não encontrar: throw LinhaNegocioNotFoundException
        │         │
        │         ├─→ toEntity(dto)
        │         │
        │         ├─→ centroCusto.setLinhaNegocio(linhaNegocio)
        │         │
        │         ├─→ centroCustoRepository.save(centroCusto)
        │         │    │
        │         │    └─→ INSERT INTO centros_custo 
        │         │        (descricao, ativo, linha_negocio_id) 
        │         │        VALUES (?, true, ?)
        │         │
        │         └─→ toDTO(saved)
        │
        ├─→ Frontend recebe CentroCustoDTO
        │
        ├─→ toast.success("Centro de Custo cadastrado com sucesso")
        │
        ├─→ handleClose()  (fecha modal)
        │
        └─→ carregarDados()  (atualiza lista)
```

### 3. Fluxo de Edição

```
┌─────────────────────────────────────────────────────────────────────┐
│                     EDIÇÃO DE CENTRO DE CUSTO                        │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica no ícone de edição
        │
        ├─→ handleOpen(centroCusto)
        │    │
        │    ├─→ setSelectedCentroCusto(centroCusto)
        │    ├─→ setValue('descricao', centroCusto.descricao)
        │    ├─→ setValue('linhaNegocioId', centroCusto.linhaNegocioId)
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
        │    └─→ centroCustoService.atualizar(selectedCentroCusto.id, data)
        │
        ├─→ PUT /centros-custo/{id}
        │    Body: { descricao, linhaNegocioId }
        │
        ├─→ Backend: CentroCustoController.atualizar()
        │    │
        │    └─→ CentroCustoService.atualizar(id, dto)
        │         │
        │         ├─→ Busca CentroCusto existente
        │         │    └─→ Se não encontrar: throw CentroCustoNotFoundException
        │         │
        │         ├─→ Busca nova LinhaNegocio
        │         │    └─→ Se não encontrar: throw LinhaNegocioNotFoundException
        │         │
        │         ├─→ centroCusto.setDescricao(dto.descricao())
        │         ├─→ centroCusto.setLinhaNegocio(linhaNegocio)
        │         │
        │         ├─→ centroCustoRepository.save(centroCusto)
        │         │    │
        │         │    └─→ UPDATE centros_custo 
        │         │        SET descricao = ?, linha_negocio_id = ?
        │         │        WHERE id = ?
        │         │
        │         └─→ toDTO(updated)
        │
        ├─→ toast.success("Centro de Custo atualizado com sucesso")
        │
        ├─→ handleClose()
        │
        └─→ carregarDados()
```

### 4. Fluxo de Deleção

```
┌─────────────────────────────────────────────────────────────────────┐
│                     DELEÇÃO DE CENTRO DE CUSTO                       │
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
        ├─→ centroCustoService.remover(id)
        │
        ├─→ DELETE /centros-custo/{id}
        │
        ├─→ Backend: CentroCustoController.remover()
        │    │
        │    └─→ CentroCustoService.remover(id)
        │         │
        │         ├─→ Busca CentroCusto
        │         │    └─→ Se não encontrar: throw CentroCustoNotFoundException
        │         │
        │         ├─→ centroCusto.setAtivo(false)  ← Soft Delete
        │         │
        │         └─→ centroCustoRepository.save(centroCusto)
        │              │
        │              └─→ UPDATE centros_custo 
        │                  SET ativo = false
        │                  WHERE id = ?
        │
        ├─→ Status: 204 No Content
        │
        ├─→ toast.success("Centro de Custo excluído com sucesso")
        │
        └─→ carregarDados()  (atualiza lista)
```

---

## 💻 Exemplos de Código

### Exemplo 1: Cadastrar Centro de Custo

**Request:**
```http
POST /centros-custo
Content-Type: application/json

{
  "descricao": "Desenvolvimento Backend",
  "linhaNegocioId": 2
}
```

**Response (200 OK):**
```json
{
  "id": 15,
  "descricao": "Desenvolvimento Backend",
  "ativo": true,
  "linhaNegocioId": 2
}
```

**Código Backend Executado:**
```java
// 1. Valida DTO
@Valid CentroCustoDTO dto

// 2. Busca Linha de Negócio
LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(2L)
    .filter(ln -> ln.getAtivo())
    .orElseThrow(() -> new LinhaNegocioNotFoundException(2L));

// 3. Cria entidade
CentroCusto centroCusto = new CentroCusto();
centroCusto.setDescricao("Desenvolvimento Backend");
centroCusto.setAtivo(true);
centroCusto.setLinhaNegocio(linhaNegocio);

// 4. Salva no banco
CentroCusto saved = centroCustoRepository.save(centroCusto);

// 5. Retorna DTO
return new CentroCustoDTO(
    saved.getId(),
    saved.getDescricao(),
    saved.getAtivo(),
    saved.getLinhaNegocio().getId()
);
```

**Código Frontend Executado:**
```tsx
const onSubmit = async (data: CentroCustoFormData) => {
  try {
    // data = { descricao: "Desenvolvimento Backend", linhaNegocioId: 2 }
    const response = await centroCustoService.cadastrar(data);
    // response = { id: 15, descricao: "Desenvolvimento Backend", ativo: true, linhaNegocioId: 2 }
    
    toast.success('Centro de Custo cadastrado com sucesso');
    handleClose();
    carregarDados(); // Recarrega lista
  } catch (error) {
    toast.error('Erro ao salvar centro de custo');
  }
};
```

---

### Exemplo 2: Listar Centros de Custo por Linha de Negócio

**Request:**
```http
GET /centros-custo/linha-negocio/2
```

**Response (200 OK):**
```json
[
  {
    "id": 15,
    "descricao": "Desenvolvimento Backend",
    "ativo": true,
    "linhaNegocioId": 2
  },
  {
    "id": 18,
    "descricao": "Desenvolvimento Frontend",
    "ativo": true,
    "linhaNegocioId": 2
  }
]
```

**Código Backend Executado:**
```java
public List<CentroCustoDTO> listarPorLinhaNegocio(Long linhaNegocioId) {
    return centroCustoRepository.findByLinhaNegocioIdAndAtivoTrue(linhaNegocioId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}

// SQL Gerado:
// SELECT * FROM centros_custo 
// WHERE linha_negocio_id = 2 AND ativo = true
```

**Código Frontend:**
```tsx
const centrosCusto = await centroCustoService.listarPorLinhaNegocio(2);
console.log(centrosCusto); 
// [{ id: 15, descricao: "Desenvolvimento Backend", ... }, ...]
```

---

### Exemplo 3: Validação de Linha de Negócio Inexistente

**Request:**
```http
POST /centros-custo
Content-Type: application/json

{
  "descricao": "Centro Teste",
  "linhaNegocioId": 999
}
```

**Response (404 NOT FOUND):**
```json
{
  "status": 404,
  "message": "Linha de Negócio não encontrada com ID: 999",
  "timestamp": "2025-10-16T10:30:00"
}
```

**Código Backend Executado:**
```java
@Transactional
public CentroCustoDTO cadastrar(CentroCustoDTO dto) {
    LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
        .filter(ln -> ln.getAtivo())
        .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));
    // Lança exceção aqui ↑
}

// GlobalExceptionHandler captura:
@ExceptionHandler(LinhaNegocioNotFoundException.class)
public ResponseEntity<ErrorResponse> handleLinhaNegocioNotFoundException(
        LinhaNegocioNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
}
```

---

## 🎯 Padrões e Boas Práticas

### 1. Backend

#### Soft Delete
```java
// ❌ Evitar: Delete físico
centroCustoRepository.deleteById(id);

// ✅ Preferir: Soft delete
centroCusto.setAtivo(false);
centroCustoRepository.save(centroCusto);
```

**Benefícios:**
- Preserva integridade referencial
- Permite auditoria e recuperação de dados
- Evita cascata de deleções indesejadas

---

#### Validação de Relacionamentos
```java
// ✅ Sempre validar entidades relacionadas antes de salvar
LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
    .filter(ln -> ln.getAtivo())  // ← Valida que está ativa
    .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));
```

**Benefícios:**
- Evita referências órfãs
- Garante consistência de dados
- Fornece mensagens de erro claras

---

#### DTOs Imutáveis com Records
```java
// ✅ Records são imutáveis, concisos e seguros
public record CentroCustoDTO(
    Long id,
    String descricao,
    Boolean ativo,
    Long linhaNegocioId
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
return centroCustoRepository.findByAtivoTrue().stream()
    .map(this::toDTO)
    .collect(Collectors.toList());
```

**Benefícios:**
- Código declarativo e legível
- Facilita testes unitários
- Evita loops manuais e mutações

---

### 2. Frontend

#### Carregamento Paralelo com `Promise.all`
```tsx
// ✅ Carrega múltiplas entidades em paralelo
const [centrosCustoData, linhasNegocioData] = await Promise.all([
  centroCustoService.listarTodos(),
  linhaNegocioService.listarTodos(),
]);
```

**Benefícios:**
- Reduz tempo total de carregamento
- Evita waterfalls de requisições
- Melhora experiência do usuário

---

#### Join Manual no Frontend
```tsx
// ✅ Join manual para exibir nome da linha de negócio
{centrosCusto.map((centroCusto) => {
  const linhaNegocio = linhasNegocio.find(ln => ln.id === centroCusto.linhaNegocioId);
  return (
    <TableRow key={centroCusto.id}>
      <TableCell>{linhaNegocio?.descricao || 'N/A'}</TableCell>
    </TableRow>
  );
})}
```

**Benefícios:**
- Backend envia apenas IDs (payload menor)
- Frontend controla renderização
- Flexibilidade para exibir dados relacionados

---

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
  await centroCustoService.cadastrar(data);
  toast.success('Centro de Custo cadastrado com sucesso');
} catch (error) {
  toast.error('Erro ao salvar centro de custo');
}
```

**Benefícios:**
- UX responsivo
- Confirmação visual de ações
- Tratamento consistente de erros

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

#### Inversão de Dependência

```java
// ✅ Service depende de abstrações (interfaces)
@RequiredArgsConstructor
public class CentroCustoService {
    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;
}

// ✅ Injeção via construtor com Lombok
```

**Benefícios:**
- Facilita testes unitários (mock de repositories)
- Baixo acoplamento
- Segue princípios SOLID

---

## 🚀 Melhorias Futuras

### 1. Paginação e Busca

**Backend:**
```java
@GetMapping
public ResponseEntity<Page<CentroCustoDTO>> listarTodos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String descricao
) {
    Pageable pageable = PageRequest.of(page, size);
    
    if (descricao != null) {
        return ResponseEntity.ok(
            centroCustoService.buscarPorDescricao(descricao, pageable)
        );
    }
    
    return ResponseEntity.ok(centroCustoService.listarTodas(pageable));
}
```

**Repository:**
```java
Page<CentroCusto> findByAtivoTrueAndDescricaoContainingIgnoreCase(
    String descricao, Pageable pageable
);
```

**Frontend:**
```tsx
import { TablePagination } from '@mui/material';

const [page, setPage] = useState(0);
const [rowsPerPage, setRowsPerPage] = useState(20);

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

### 2. Validação de Duplicidade

**Backend:**
```java
@Transactional
public CentroCustoDTO cadastrar(CentroCustoDTO dto) {
    // Verifica se já existe centro de custo com mesma descrição
    if (centroCustoRepository.existsByDescricaoAndAtivoTrue(dto.descricao())) {
        throw new CentroCustoDuplicadoException(dto.descricao());
    }
    
    // ... resto do código
}
```

**Repository:**
```java
boolean existsByDescricaoAndAtivoTrue(String descricao);
```

---

### 3. Ordenação Customizável

**Backend:**
```java
@GetMapping
public ResponseEntity<List<CentroCustoDTO>> listarTodos(
    @RequestParam(defaultValue = "descricao") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    return ResponseEntity.ok(centroCustoService.listarTodas(sort));
}
```

**Service:**
```java
public List<CentroCustoDTO> listarTodas(Sort sort) {
    return centroCustoRepository.findByAtivoTrue(sort).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
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

### 4. Filtro em Cascata

**Frontend:**
```tsx
const [selectedLinhaNegocio, setSelectedLinhaNegocio] = useState<number | null>(null);

useEffect(() => {
  if (selectedLinhaNegocio) {
    centroCustoService.listarPorLinhaNegocio(selectedLinhaNegocio)
      .then(setCentrosCusto);
  } else {
    carregarDados();
  }
}, [selectedLinhaNegocio]);

// Componente de filtro
<FormControl>
  <InputLabel>Filtrar por Linha de Negócio</InputLabel>
  <Select
    value={selectedLinhaNegocio || ''}
    onChange={(e) => setSelectedLinhaNegocio(Number(e.target.value))}
  >
    <MenuItem value="">Todos</MenuItem>
    {linhasNegocio.map((ln) => (
      <MenuItem key={ln.id} value={ln.id}>
        {ln.descricao}
      </MenuItem>
    ))}
  </Select>
</FormControl>
```

---

### 5. Auditoria Completa

**Entidade:**
```java
@Data
@Entity
@Table(name = "centros_custo")
@EntityListeners(AuditingEntityListener.class)
public class CentroCusto {
    // ... campos existentes
    
    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    
    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
    
    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private String criadoPor;
    
    @LastModifiedBy
    @Column(name = "atualizado_por")
    private String atualizadoPor;
}
```

**Migration:**
```sql
ALTER TABLE centros_custo 
ADD COLUMN criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN atualizado_em TIMESTAMP,
ADD COLUMN criado_por VARCHAR(100),
ADD COLUMN atualizado_por VARCHAR(100);
```

---

### 6. Exportação para Excel/CSV

**Backend:**
```java
@GetMapping("/exportar")
public ResponseEntity<byte[]> exportarCentrosCusto() {
    List<CentroCustoDTO> centrosCusto = centroCustoService.listarTodas();
    
    byte[] excelBytes = excelService.gerarExcel(centrosCusto);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=centros_custo.xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelBytes);
}
```

**Frontend:**
```tsx
import { Download as DownloadIcon } from '@mui/icons-material';

const handleExportar = async () => {
  const response = await api.get('/centros-custo/exportar', { 
    responseType: 'blob' 
  });
  
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'centros_custo.xlsx');
  document.body.appendChild(link);
  link.click();
  link.remove();
};

<Button startIcon={<DownloadIcon />} onClick={handleExportar}>
  Exportar
</Button>
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
        <TableCell><Skeleton /></TableCell>
      </TableRow>
    ))}
  </TableBody>
) : (
  <TableBody>
    {centrosCusto.map((centroCusto) => (
      // ... renderização normal
    ))}
  </TableBody>
)}
```

---

## 📝 Conclusão

A tela de **Centros de Custo** é um exemplo perfeito de **CRUD com relacionamento simples**. Ela demonstra:

✅ **Relacionamento @ManyToOne** bem implementado  
✅ **Validação de entidades relacionadas** no backend  
✅ **Carregamento paralelo** de dados no frontend  
✅ **Join manual** para exibir dados relacionados  
✅ **Soft Delete** preservando integridade referencial  
✅ **Filtro adicional** por linha de negócio  

Esta implementação serve como **template** para outras entidades que possuam relacionamentos simples com outras tabelas, como:

- Funcionários → Cargo
- Funcionários → Centro de Custo
- Rubricas → Tipo de Rubrica
- Benefícios → Funcionário

---

## 📊 Estatísticas Finais

| Métrica | Valor |
|---------|-------|
| **Total de Código** | ~471 linhas |
| **Backend** | 229 linhas |
| **Frontend** | 242 linhas |
| **Arquivos Backend** | 5 |
| **Arquivos Frontend** | 2 |
| **Endpoints REST** | 6 |
| **Operações CRUD** | 5 (List, Get, Create, Update, Delete) |
| **Endpoints Especiais** | 1 (Filtro por Linha de Negócio) |
| **Relacionamentos JPA** | 1 (@ManyToOne LinhaNegocio) |
| **Complexidade** | **Média** (CRUD + Relacionamento) |

---

**Documento gerado em:** 16 de Outubro de 2025  
**Versão do Sistema:** 1.0  
**Autor:** Sistema de Documentação Automática

