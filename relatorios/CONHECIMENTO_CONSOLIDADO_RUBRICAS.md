# 💰 Conhecimento Consolidado - Tela de Rubricas

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

A tela de **Rubricas** é uma implementação CRUD (Create, Read, Update, Delete) que gerencia as rubricas (itens) da folha de pagamento. É uma entidade fundamental do sistema, representando salários, descontos, benefícios e outros valores que compõem a folha de pagamento.

### Características Principais

- ✅ **CRUD Completo**: Cadastrar, listar, atualizar e remover rubricas
- ✅ **Relacionamento @ManyToOne**: Com TipoRubrica (PROVENTO, DESCONTO, INFORMATIVO)
- ✅ **Código Único**: Validação de unicidade e código não editável
- ✅ **Porcentagem Opcional**: Campo numérico com valor padrão de 100%
- ✅ **Soft Delete**: Exclusão lógica preservando integridade referencial
- ✅ **Validações**: Frontend e backend com validações consistentes
- ✅ **Controller do React Hook Form**: Para gerenciamento avançado do Select

### O que são Rubricas?

**Rubricas** são os itens que compõem a folha de pagamento. Exemplos:

- **Proventos**: Salário Base, Horas Extras, Comissões, Gratificações
- **Descontos**: INSS, IRRF, Vale Transporte, Plano de Saúde
- **Informativos**: Faltas, Férias, Afastamentos (não afetam o cálculo)

Cada rubrica possui:
- **Código**: Identificador único (ex: 001, 002, INSS)
- **Descrição**: Nome descritivo (ex: "Salário Base", "INSS")
- **Tipo**: PROVENTO, DESCONTO ou INFORMATIVO
- **Porcentagem**: Percentual aplicado (opcional, padrão 100%)

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
- **React Hook Form** com `Controller` para Select avançado
- **Axios** para comunicação HTTP
- **React Toastify** para notificações

### Estrutura de Arquivos

```
📁 Backend
├── src/main/java/br/com/techne/sistemafolha/
│   ├── controller/
│   │   └── RubricaController.java           (70 linhas)
│   ├── service/
│   │   └── RubricaService.java              (94 linhas)
│   ├── repository/
│   │   ├── RubricaRepository.java           (23 linhas)
│   │   └── TipoRubricaRepository.java       (15 linhas)
│   ├── model/
│   │   ├── Rubrica.java                     (34 linhas)
│   │   └── TipoRubrica.java                 (24 linhas)
│   └── dto/
│       └── RubricaDTO.java                  (37 linhas)

📁 Frontend
├── src/
│   ├── pages/
│   │   └── Rubricas/
│   │       └── index.tsx                     (243 linhas)
│   ├── services/
│   │   └── rubricaService.ts                (55 linhas)
│   └── types/
│       └── index.ts                          (interface Rubrica)
```

**Total de Código**: ~595 linhas (Backend: 297 linhas | Frontend: 298 linhas)

---

## 🔧 Camada Backend

### 1. Entidades JPA

#### `Rubrica.java`

```java
package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubricas")
public class Rubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "tipo_rubrica_id", nullable = false)
    private TipoRubrica tipoRubrica;

    @Column
    private Double porcentagem;

    @Column(nullable = false)
    private Boolean ativo = true;
}
```

**Características da Entidade Rubrica:**

- **@Column(unique = true)**: Garante unicidade do código a nível de banco
- **@ManyToOne**: Relacionamento obrigatório com TipoRubrica
- **nullable = false**: Código, descrição, tipo e ativo são obrigatórios
- **porcentagem**: Campo opcional (pode ser null)
- **Soft Delete**: Campo `ativo` para exclusão lógica
- **Lombok @Data**: Gera automaticamente getters, setters, equals, hashCode e toString
- **@NoArgsConstructor e @AllArgsConstructor**: Construtores gerados pelo Lombok

---

#### `TipoRubrica.java`

```java
package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tipo_rubrica")
public class TipoRubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;
}
```

**Características da Entidade TipoRubrica:**

- **Entidade Base**: Sem relacionamentos obrigatórios
- **Descrição Única**: PROVENTO, DESCONTO, INFORMATIVO
- **Soft Delete**: Campo `ativo` para exclusão lógica

---

### 2. DTO - `RubricaDTO.java`

```java
package br.com.techne.sistemafolha.dto;

public class RubricaDTO {
    private Long id;
    private String codigo;
    private String descricao;
    private String tipoRubricaDescricao;
    private String tipo;
    private Double porcentagem;
    private Boolean ativo;

    public RubricaDTO(Long id, String codigo, String descricao, String tipoRubricaDescricao, String tipo, Double porcentagem, Boolean ativo) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipoRubricaDescricao = tipoRubricaDescricao;
        this.tipo = tipo;
        this.porcentagem = porcentagem;
        this.ativo = ativo;
    }

    // getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipoRubricaDescricao() { return tipoRubricaDescricao; }
    public void setTipoRubricaDescricao(String tipoRubricaDescricao) { this.tipoRubricaDescricao = tipoRubricaDescricao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Double getPorcentagem() { return porcentagem; }
    public void setPorcentagem(Double porcentagem) { this.porcentagem = porcentagem; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
```

**Características do DTO:**

- **Classe Regular**: Não é um Record (permite flexibilidade)
- **Campos Duplicados**: `tipoRubricaDescricao` e `tipo` (compatibilidade frontend/backend)
- **Validações**: Implementadas no Service layer ao invés de anotações Jakarta
- **Mutável**: Permite modificações após criação

---

### 3. Repositories

#### `RubricaRepository.java`

```java
package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Rubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RubricaRepository extends JpaRepository<Rubrica, Long> {
    List<Rubrica> findByAtivoTrue();
    Optional<Rubrica> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Query("UPDATE Rubrica r SET r.ativo = false WHERE r.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<Rubrica> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
```

**Características do Repository:**

- **Query Methods**: Nomenclatura derivada automática
- **Validação de Unicidade**: `existsByCodigo` e `findByCodigo`
- **Soft Delete**: Query JPQL customizada
- **Filtro de Ativos**: `findByAtivoTrue` e `findByIdAndAtivoTrue`

---

#### `TipoRubricaRepository.java`

```java
package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.TipoRubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoRubricaRepository extends JpaRepository<TipoRubrica, Long> {
    
    Optional<TipoRubrica> findByDescricao(String descricao);
    
    Optional<TipoRubrica> findByDescricaoAndAtivoTrue(String descricao);
}
```

**Características do Repository:**

- **Busca por Descrição**: Para converter string em entidade
- **Filtro de Ativos**: Para garantir que apenas tipos ativos sejam usados

---

### 4. Service - `RubricaService.java`

```java
package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.RubricaDTO;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.model.TipoRubrica;
import br.com.techne.sistemafolha.repository.RubricaRepository;
import br.com.techne.sistemafolha.repository.TipoRubricaRepository;
import br.com.techne.sistemafolha.exception.RubricaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricaService {
    private final RubricaRepository rubricaRepository;
    private final TipoRubricaRepository tipoRubricaRepository;

    public List<RubricaDTO> listarTodas() {
        return rubricaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public RubricaDTO buscarPorId(Long id) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(this::toDTO)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));
    }

    @Transactional
    public RubricaDTO cadastrar(RubricaDTO dto) {
        if (rubricaRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
        }

        Rubrica rubrica = toEntity(dto);
        rubrica = rubricaRepository.save(rubrica);
        return toDTO(rubrica);
    }

    @Transactional
    public RubricaDTO atualizar(Long id, RubricaDTO dto) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(id)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));

        if (!rubrica.getCodigo().equals(dto.getCodigo()) && rubricaRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
        }

        Rubrica rubricaAtualizada = toEntity(dto);
        rubricaAtualizada.setId(id);
        rubricaAtualizada = rubricaRepository.save(rubricaAtualizada);
        return toDTO(rubricaAtualizada);
    }

    @Transactional
    public void remover(Long id) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(id)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));

        rubricaRepository.softDelete(id);
    }

    private RubricaDTO toDTO(Rubrica rubrica) {
        String tipoDescricao = rubrica.getTipoRubrica() != null ? rubrica.getTipoRubrica().getDescricao() : null;
        return new RubricaDTO(
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getDescricao(),
            tipoDescricao,
            tipoDescricao,
            rubrica.getPorcentagem(),
            rubrica.getAtivo()
        );
    }

    private Rubrica toEntity(RubricaDTO dto) {
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(dto.getCodigo());
        rubrica.setDescricao(dto.getDescricao());
        if (dto.getTipoRubricaDescricao() != null) {
            TipoRubrica tipo = tipoRubricaRepository.findByDescricao(dto.getTipoRubricaDescricao())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de rubrica não encontrado: " + dto.getTipoRubricaDescricao()));
            rubrica.setTipoRubrica(tipo);
        }
        rubrica.setPorcentagem(dto.getPorcentagem());
        rubrica.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return rubrica;
    }
}
```

**Características do Service:**

- **Validação de Código Único**: Antes de cadastrar e atualizar
- **Busca de TipoRubrica**: Por descrição (PROVENTO, DESCONTO, INFORMATIVO)
- **Exceções Específicas**: `RubricaNotFoundException` e `IllegalArgumentException`
- **Soft Delete**: Chama método do repository
- **Conversão Entity ↔ DTO**: Métodos privados `toDTO()` e `toEntity()`
- **@Transactional**: Garante atomicidade nas operações de escrita

---

### 5. Controller - `RubricaController.java`

```java
package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.RubricaDTO;
import br.com.techne.sistemafolha.service.RubricaService;
import br.com.techne.sistemafolha.exception.RubricaNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rubricas")
@RequiredArgsConstructor
@Tag(name = "Rubricas", description = "API para gerenciamento de rubricas")
public class RubricaController {
    private final RubricaService rubricaService;

    @GetMapping
    @Operation(summary = "Lista todas as rubricas ativas")
    public ResponseEntity<List<RubricaDTO>> listar() {
        return ResponseEntity.ok(rubricaService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma rubrica ativa pelo ID")
    public ResponseEntity<RubricaDTO> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(rubricaService.buscarPorId(id));
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova rubrica")
    public ResponseEntity<RubricaDTO> cadastrar(@Valid @RequestBody RubricaDTO dto) {
        try {
            return ResponseEntity.ok(rubricaService.cadastrar(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma rubrica existente")
    public ResponseEntity<RubricaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody RubricaDTO dto) {
        try {
            return ResponseEntity.ok(rubricaService.atualizar(id, dto));
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma rubrica (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            rubricaService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Características do Controller:**

- **RESTful**: Segue convenções REST (GET, POST, PUT, DELETE)
- **Try-Catch**: Tratamento explícito de exceções
- **Status HTTP Apropriados**: 200, 201, 204, 400, 404
- **Swagger/OpenAPI**: Documentação automática da API
- **@Valid**: Validação de DTOs (se houver anotações Jakarta)

---

## 🎨 Camada Frontend

### 1. Componente Principal - `pages/Rubricas/index.tsx`

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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import { rubricaService } from '../../services/rubricaService';
import type { Rubrica } from '../../types';

interface RubricaFormData {
  codigo: string;
  descricao: string;
  tipo: string;
  porcentagem?: number;
}

const tiposRubrica = [
  { value: 'PROVENTO', label: 'Provento' },
  { value: 'DESCONTO', label: 'Desconto' },
  { value: 'INFORMATIVO', label: 'Informativo' },
];

export default function Rubricas() {
  const [rubricas, setRubricas] = useState<Rubrica[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedRubrica, setSelectedRubrica] = useState<Rubrica | null>(null);
  const { register, handleSubmit, reset, setValue, control } = useForm<RubricaFormData>();

  useEffect(() => {
    carregarRubricas();
  }, []);

  const carregarRubricas = async () => {
    try {
      const data = await rubricaService.listarTodos();
      setRubricas(data);
    } catch (error) {
      toast.error('Erro ao carregar rubricas');
    }
  };

  const handleOpen = (rubrica?: Rubrica) => {
    if (rubrica) {
      setSelectedRubrica(rubrica);
      setValue('codigo', rubrica.codigo);
      setValue('descricao', rubrica.descricao);
      // Mapeia a descrição para o valor correto do dropdown
      const tipoValue = rubrica.tipoRubricaDescricao || rubrica.tipo;
      let mappedTipo = 'INFORMATIVO'; // valor padrão
      if (tipoValue === 'PROVENTO') mappedTipo = 'PROVENTO';
      else if (tipoValue === 'DESCONTO') mappedTipo = 'DESCONTO';
      else if (tipoValue === 'INFORMATIVO') mappedTipo = 'INFORMATIVO';
      setValue('tipo', mappedTipo);
      setValue('porcentagem', rubrica.porcentagem);
    } else {
      setSelectedRubrica(null);
      reset();
      // Define valor padrão de 100 para nova rubrica
      setValue('porcentagem', 100);
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: RubricaFormData) => {
    try {
      if (selectedRubrica) {
        await rubricaService.atualizar(selectedRubrica.id, data);
        toast.success('Rubrica atualizada com sucesso');
      } else {
        await rubricaService.cadastrar(data);
        toast.success('Rubrica cadastrada com sucesso');
      }
      handleClose();
      carregarRubricas();
    } catch (error) {
      toast.error('Erro ao salvar rubrica');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir esta rubrica?')) {
      try {
        await rubricaService.remover(id);
        toast.success('Rubrica excluída com sucesso');
        carregarRubricas();
      } catch (error) {
        toast.error('Erro ao excluir rubrica');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Rubricas</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Nova Rubrica
        </Button>
      </Box>

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Porcentagem</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rubricas.map((rubrica) => (
                  <TableRow key={rubrica.id}>
                    <TableCell>{rubrica.codigo}</TableCell>
                    <TableCell>{rubrica.descricao}</TableCell>
                    <TableCell>
                      {rubrica.tipoRubricaDescricao || rubrica.tipo || '-'}
                    </TableCell>
                    <TableCell>
                      {rubrica.porcentagem ? `${rubrica.porcentagem}%` : '-'}
                    </TableCell>
                    <TableCell>{rubrica.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        onClick={() => handleOpen(rubrica)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(rubrica.id)}
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
          {selectedRubrica ? 'Editar Rubrica' : 'Nova Rubrica'}
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <TextField
              {...register('codigo', { required: 'Código é obrigatório' })}
              label="Código"
              fullWidth
              margin="normal"
              required
              disabled={!!selectedRubrica}
            />
            <TextField
              {...register('descricao', { required: 'Descrição é obrigatória' })}
              label="Descrição"
              fullWidth
              margin="normal"
              required
            />
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Tipo</InputLabel>
              <Controller
                name="tipo"
                control={control}
                defaultValue=""
                render={({ field }) => (
                  <Select
                    label="Tipo"
                    {...field}
                    value={field.value || ''}
                  >
                    {tiposRubrica.map((tipo) => (
                      <MenuItem key={tipo.value} value={tipo.value}>
                        {tipo.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <TextField
              {...register('porcentagem', {
                min: { value: 0, message: 'Porcentagem deve ser maior ou igual a 0' }
              })}
              label="Porcentagem (%)"
              type="number"
              fullWidth
              margin="normal"
              inputProps={{ min: 0, step: 0.01 }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              {selectedRubrica ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
```

**Características do Componente:**

#### Constante de Tipos

```tsx
const tiposRubrica = [
  { value: 'PROVENTO', label: 'Provento' },
  { value: 'DESCONTO', label: 'Desconto' },
  { value: 'INFORMATIVO', label: 'Informativo' },
];
```

- **Hardcoded**: Tipos fixos no frontend (poderia vir do backend)
- **Value/Label**: Separação entre valor técnico e exibição

---

#### Gerenciamento de Formulário

```tsx
const handleOpen = (rubrica?: Rubrica) => {
  if (rubrica) {
    setSelectedRubrica(rubrica);
    setValue('codigo', rubrica.codigo);
    setValue('descricao', rubrica.descricao);
    // Mapeia a descrição para o valor correto do dropdown
    const tipoValue = rubrica.tipoRubricaDescricao || rubrica.tipo;
    let mappedTipo = 'INFORMATIVO'; // valor padrão
    if (tipoValue === 'PROVENTO') mappedTipo = 'PROVENTO';
    else if (tipoValue === 'DESCONTO') mappedTipo = 'DESCONTO';
    else if (tipoValue === 'INFORMATIVO') mappedTipo = 'INFORMATIVO';
    setValue('tipo', mappedTipo);
    setValue('porcentagem', rubrica.porcentagem);
  } else {
    setSelectedRubrica(null);
    reset();
    // Define valor padrão de 100 para nova rubrica
    setValue('porcentagem', 100);
  }
  setOpen(true);
};
```

- **Mapeamento de Tipo**: Garante compatibilidade entre backend e frontend
- **Valor Padrão**: Porcentagem 100% para nova rubrica
- **Código**: Preenchido apenas em edição (disabled depois)

---

#### Controller do React Hook Form

```tsx
<Controller
  name="tipo"
  control={control}
  defaultValue=""
  render={({ field }) => (
    <Select
      label="Tipo"
      {...field}
      value={field.value || ''}
    >
      {tiposRubrica.map((tipo) => (
        <MenuItem key={tipo.value} value={tipo.value}>
          {tipo.label}
        </MenuItem>
      ))}
    </Select>
  )}
/>
```

- **Controller**: Necessário para integrar Select do MUI com React Hook Form
- **Spread Operator**: `{...field}` passa value, onChange, onBlur, etc.
- **Default Value**: String vazia para evitar erro de "uncontrolled to controlled"

---

#### Campo Código Não Editável

```tsx
<TextField
  {...register('codigo', { required: 'Código é obrigatório' })}
  label="Código"
  fullWidth
  margin="normal"
  required
  disabled={!!selectedRubrica}  // ← Disabled quando está editando
/>
```

- **Disabled**: Código não pode ser alterado após criação
- **Validação de Unicidade**: Protegida no backend

---

### 2. Service - `rubricaService.ts`

```typescript
import api from './api';
import type { Rubrica } from '../types';

interface RubricaFormData {
  codigo: string;
  descricao: string;
  tipo: string;
  porcentagem?: number;
}

const rubricaService = {
  listarTodos: async (): Promise<Rubrica[]> => {
    const response = await api.get('/rubricas');
    // Mapeia os dados para garantir compatibilidade
    return response.data.map((item: any) => ({
      ...item,
      tipo: item.tipoRubricaDescricao || item.tipo,
      tipoRubricaDescricao: item.tipoRubricaDescricao
    }));
  },

  buscarPorId: async (id: number): Promise<Rubrica> => {
    const response = await api.get(`/rubricas/${id}`);
    const item = response.data;
    return {
      ...item,
      tipo: item.tipoRubricaDescricao || item.tipo,
      tipoRubricaDescricao: item.tipoRubricaDescricao
    };
  },

  cadastrar: async (data: RubricaFormData): Promise<Rubrica> => {
    const payload = {
      ...data,
      tipoRubricaDescricao: data.tipo
    };
    const response = await api.post('/rubricas', payload);
    return response.data;
  },

  atualizar: async (id: number, data: RubricaFormData): Promise<Rubrica> => {
    const payload = {
      ...data,
      tipoRubricaDescricao: data.tipo
    };
    const response = await api.put(`/rubricas/${id}`, payload);
    return response.data;
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/rubricas/${id}`);
  }
};

export { rubricaService };
```

**Características do Service:**

- **Mapeamento de Dados**: Garante compatibilidade entre `tipo` e `tipoRubricaDescricao`
- **Transformação de Payload**: Adiciona `tipoRubricaDescricao` antes de enviar ao backend
- **Axios Instance**: Usa configuração global com JWT interceptors
- **Tipagem TypeScript**: Interfaces para entrada e saída

---

### 3. Types - `types/index.ts` (interface Rubrica)

```typescript
export interface Rubrica {
  id: number;
  codigo: string;
  descricao: string;
  tipo?: string;
  tipoRubricaDescricao?: string;
  porcentagem?: number;
  ativo: boolean;
}
```

---

## 🔄 Fluxo de Dados

### 1. Fluxo de Listagem

```
┌─────────────────────────────────────────────────────────────────────┐
│                        LISTAGEM DE RUBRICAS                          │
└─────────────────────────────────────────────────────────────────────┘

   [1] useEffect()
        │
        ├─→ carregarRubricas()
        │
        ├─→ rubricaService.listarTodos()
        │
        ├─→ GET /rubricas  ───────────────────────────┐
        │                                              │
        │                                              ▼
        │                        RubricaController.listar()
        │                                              │
        │                                              ▼
        │                        RubricaService.listarTodas()
        │                                              │
        │                                              ▼
        │                        Repository.findByAtivoTrue()
        │                                              │
        │                                              ▼
        │                        SELECT r.*, t.descricao as tipo_descricao
        │                        FROM rubricas r
        │                        JOIN tipo_rubrica t ON r.tipo_rubrica_id = t.id
        │                        WHERE r.ativo = true
        │                                              │
        │   ┌──────────────────────────────────────────┘
        │   │
        │   ├─→ List<Rubrica> entities
        │   │
        │   ├─→ .stream().map(this::toDTO)
        │   │    │
        │   │    └─→ Extrai tipoRubrica.getDescricao()
        │   │
        │   └─→ List<RubricaDTO>
        │
        ├─→ Frontend mapeia dados:
        │    └─→ tipo = tipoRubricaDescricao || tipo
        │
        ├─→ setRubricas(data)
        │
        └─→ Renderização da Tabela
                │
                └─→ Para cada rubrica:
                     ├─→ Código
                     ├─→ Descrição
                     ├─→ Tipo (PROVENTO/DESCONTO/INFORMATIVO)
                     ├─→ Porcentagem (%)
                     ├─→ Status (Ativo/Inativo)
                     └─→ Ações (Editar/Excluir)
```

---

### 2. Fluxo de Criação

```
┌─────────────────────────────────────────────────────────────────────┐
│                       CRIAÇÃO DE RUBRICA                             │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica "Nova Rubrica"
        │
        ├─→ handleOpen()
        │    │
        │    ├─→ setSelectedRubrica(null)
        │    ├─→ reset()
        │    ├─→ setValue('porcentagem', 100)  ← Valor padrão
        │    └─→ setOpen(true)
        │
        ├─→ Dialog é exibido
        │    │
        │    ├─→ TextField (codigo) - HABILITADO
        │    ├─→ TextField (descricao)
        │    ├─→ Select (tipo) via Controller
        │    └─→ TextField (porcentagem) - default 100
        │
   [2] Usuário preenche formulário e clica "Cadastrar"
        │
        ├─→ handleSubmit(onSubmit)
        │
        ├─→ onSubmit(data)
        │    │
        │    └─→ rubricaService.cadastrar(data)
        │         │
        │         └─→ Adiciona tipoRubricaDescricao ao payload
        │
        ├─→ POST /rubricas
        │    Body: {
        │      codigo: "001",
        │      descricao: "Salário Base",
        │      tipoRubricaDescricao: "PROVENTO",
        │      tipo: "PROVENTO",
        │      porcentagem: 100
        │    }
        │
        ├─→ Backend: RubricaController.cadastrar()
        │    │
        │    ├─→ @Valid valida DTO (se houver anotações)
        │    │
        │    └─→ RubricaService.cadastrar(dto)
        │         │
        │         ├─→ Valida código único
        │         │    └─→ if (existsByCodigo): throw IllegalArgumentException
        │         │
        │         ├─→ toEntity(dto)
        │         │    │
        │         │    ├─→ Busca TipoRubrica por descrição
        │         │    │    └─→ tipoRubricaRepository.findByDescricao("PROVENTO")
        │         │    │
        │         │    └─→ rubrica.setTipoRubrica(tipo)
        │         │
        │         ├─→ rubricaRepository.save(rubrica)
        │         │    │
        │         │    └─→ INSERT INTO rubricas 
        │         │        (codigo, descricao, tipo_rubrica_id, porcentagem, ativo) 
        │         │        VALUES ('001', 'Salário Base', 1, 100, true)
        │         │
        │         └─→ toDTO(saved)
        │
        ├─→ Frontend recebe RubricaDTO
        │
        ├─→ toast.success("Rubrica cadastrada com sucesso")
        │
        ├─→ handleClose()
        │
        └─→ carregarRubricas()  (atualiza lista)
```

---

### 3. Fluxo de Edição

```
┌─────────────────────────────────────────────────────────────────────┐
│                       EDIÇÃO DE RUBRICA                              │
└─────────────────────────────────────────────────────────────────────┘

   [1] Usuário clica no ícone de edição
        │
        ├─→ handleOpen(rubrica)
        │    │
        │    ├─→ setSelectedRubrica(rubrica)
        │    ├─→ setValue('codigo', rubrica.codigo)
        │    ├─→ setValue('descricao', rubrica.descricao)
        │    ├─→ setValue('tipo', mappedTipo)  ← Mapeia tipo
        │    ├─→ setValue('porcentagem', rubrica.porcentagem)
        │    └─→ setOpen(true)
        │
        ├─→ Dialog é exibido com dados preenchidos
        │    │
        │    ├─→ TextField (codigo) - DESABILITADO ✋
        │    ├─→ TextField (descricao) - preenchido
        │    ├─→ Select (tipo) - selecionado
        │    └─→ TextField (porcentagem) - preenchido
        │
   [2] Usuário edita e clica "Atualizar"
        │
        ├─→ handleSubmit(onSubmit)
        │
        ├─→ onSubmit(data)
        │    │
        │    └─→ rubricaService.atualizar(id, data)
        │
        ├─→ PUT /rubricas/{id}
        │    Body: {
        │      codigo: "001",  ← Mesmo valor (disabled)
        │      descricao: "Salário Base Mensal",
        │      tipoRubricaDescricao: "PROVENTO",
        │      tipo: "PROVENTO",
        │      porcentagem: 100
        │    }
        │
        ├─→ Backend: RubricaController.atualizar()
        │    │
        │    └─→ RubricaService.atualizar(id, dto)
        │         │
        │         ├─→ Busca Rubrica existente
        │         │    └─→ Se não encontrar: throw RubricaNotFoundException
        │         │
        │         ├─→ Valida código único (se mudou)
        │         │    └─→ if (codigo diferente && exists): throw IllegalArgumentException
        │         │
        │         ├─→ toEntity(dto)
        │         │    └─→ Busca novo TipoRubrica
        │         │
        │         ├─→ rubricaAtualizada.setId(id)
        │         │
        │         ├─→ rubricaRepository.save(rubricaAtualizada)
        │         │    │
        │         │    └─→ UPDATE rubricas 
        │         │        SET descricao = ?, tipo_rubrica_id = ?, porcentagem = ?
        │         │        WHERE id = ?
        │         │
        │         └─→ toDTO(updated)
        │
        ├─→ toast.success("Rubrica atualizada com sucesso")
        │
        ├─→ handleClose()
        │
        └─→ carregarRubricas()
```

---

### 4. Fluxo de Validação de Código Único

```
┌─────────────────────────────────────────────────────────────────────┐
│              VALIDAÇÃO DE CÓDIGO ÚNICO                               │
└─────────────────────────────────────────────────────────────────────┘

   [Cenário 1: Criação]
   
   rubricaService.cadastrar({ codigo: "001", ... })
        │
        ├─→ RubricaService.cadastrar(dto)
        │    │
        │    ├─→ rubricaRepository.existsByCodigo("001")
        │    │    │
        │    │    └─→ SELECT COUNT(*) FROM rubricas WHERE codigo = '001'
        │    │
        │    ├─→ if (true): throw IllegalArgumentException
        │    │    └─→ "Já existe uma rubrica com o código: 001"
        │    │
        │    └─→ if (false): Prossegue com save()
        
   
   [Cenário 2: Edição]
   
   rubricaService.atualizar(5, { codigo: "002", ... })
        │
        ├─→ RubricaService.atualizar(5, dto)
        │    │
        │    ├─→ Busca rubrica id=5 (codigo atual = "001")
        │    │
        │    ├─→ Verifica se código mudou:
        │    │    │
        │    │    ├─→ if ("001" != "002"):  ← Código mudou!
        │    │    │    │
        │    │    │    └─→ rubricaRepository.existsByCodigo("002")
        │    │    │         │
        │    │    │         └─→ if (true): throw IllegalArgumentException
        │    │    │
        │    │    └─→ if ("001" == "001"):  ← Código não mudou
        │    │         └─→ Não valida (mesmo registro)
```

---

## 💻 Exemplos de Código

### Exemplo 1: Cadastrar Rubrica

**Request:**
```http
POST /rubricas
Content-Type: application/json

{
  "codigo": "001",
  "descricao": "Salário Base",
  "tipoRubricaDescricao": "PROVENTO",
  "tipo": "PROVENTO",
  "porcentagem": 100
}
```

**Response (200 OK):**
```json
{
  "id": 15,
  "codigo": "001",
  "descricao": "Salário Base",
  "tipoRubricaDescricao": "PROVENTO",
  "tipo": "PROVENTO",
  "porcentagem": 100,
  "ativo": true
}
```

**SQL Executado:**
```sql
-- 1. Busca TipoRubrica
SELECT * FROM tipo_rubrica WHERE descricao = 'PROVENTO';

-- 2. Valida código único
SELECT COUNT(*) FROM rubricas WHERE codigo = '001';

-- 3. Insere rubrica
INSERT INTO rubricas (codigo, descricao, tipo_rubrica_id, porcentagem, ativo) 
VALUES ('001', 'Salário Base', 1, 100, true);
```

---

### Exemplo 2: Tentativa de Cadastro com Código Duplicado

**Request:**
```http
POST /rubricas
Content-Type: application/json

{
  "codigo": "001",
  "descricao": "Outra Rubrica",
  "tipoRubricaDescricao": "PROVENTO",
  "porcentagem": 100
}
```

**Response (400 BAD REQUEST):**
```json
{
  "status": 400,
  "message": "Já existe uma rubrica com o código: 001",
  "timestamp": "2025-10-16T11:00:00"
}
```

---

### Exemplo 3: Listar Todas as Rubricas

**Request:**
```http
GET /rubricas
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "codigo": "001",
    "descricao": "Salário Base",
    "tipoRubricaDescricao": "PROVENTO",
    "tipo": "PROVENTO",
    "porcentagem": 100,
    "ativo": true
  },
  {
    "id": 2,
    "codigo": "INSS",
    "descricao": "INSS - Desconto",
    "tipoRubricaDescricao": "DESCONTO",
    "tipo": "DESCONTO",
    "porcentagem": 11,
    "ativo": true
  },
  {
    "id": 3,
    "codigo": "HE50",
    "descricao": "Hora Extra 50%",
    "tipoRubricaDescricao": "PROVENTO",
    "tipo": "PROVENTO",
    "porcentagem": 150,
    "ativo": true
  }
]
```

---

### Exemplo 4: Atualizar Rubrica

**Request:**
```http
PUT /rubricas/1
Content-Type: application/json

{
  "codigo": "001",
  "descricao": "Salário Base Mensal",
  "tipoRubricaDescricao": "PROVENTO",
  "tipo": "PROVENTO",
  "porcentagem": 100
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "codigo": "001",
  "descricao": "Salário Base Mensal",
  "tipoRubricaDescricao": "PROVENTO",
  "tipo": "PROVENTO",
  "porcentagem": 100,
  "ativo": true
}
```

**SQL Executado:**
```sql
-- 1. Busca rubrica existente
SELECT * FROM rubricas WHERE id = 1 AND ativo = true;

-- 2. Busca TipoRubrica
SELECT * FROM tipo_rubrica WHERE descricao = 'PROVENTO';

-- 3. Atualiza
UPDATE rubricas 
SET descricao = 'Salário Base Mensal', 
    tipo_rubrica_id = 1, 
    porcentagem = 100 
WHERE id = 1;
```

---

## 🎯 Padrões e Boas Práticas

### 1. Backend

#### Validação de Código Único

```java
// ✅ Validar código único antes de cadastrar
if (rubricaRepository.existsByCodigo(dto.getCodigo())) {
    throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
}

// ✅ Validar código único ao atualizar (se mudou)
if (!rubrica.getCodigo().equals(dto.getCodigo()) && rubricaRepository.existsByCodigo(dto.getCodigo())) {
    throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
}
```

**Benefícios:**
- Evita duplicação de códigos
- Garante integridade de dados
- Mensagens de erro claras

---

#### Busca de Entidade Relacionada por Descrição

```java
// ✅ Buscar TipoRubrica por descrição (flexível para o frontend)
TipoRubrica tipo = tipoRubricaRepository.findByDescricao(dto.getTipoRubricaDescricao())
    .orElseThrow(() -> new IllegalArgumentException("Tipo de rubrica não encontrado: " + dto.getTipoRubricaDescricao()));
```

**Benefícios:**
- Frontend não precisa saber IDs internos
- Mais legível e manutenível
- Validação automática de tipos válidos

---

#### Soft Delete com Query Customizada

```java
// ✅ Soft delete via query JPQL
@Modifying
@Query("UPDATE Rubrica r SET r.ativo = false WHERE r.id = :id")
void softDelete(@Param("id") Long id);
```

**Benefícios:**
- Preserva integridade referencial
- Histórico de folha de pagamento mantido
- Recuperação possível

---

### 2. Frontend

#### Controller para Select do Material-UI

```tsx
// ✅ Usar Controller para integrar Select com React Hook Form
<Controller
  name="tipo"
  control={control}
  defaultValue=""
  render={({ field }) => (
    <Select label="Tipo" {...field} value={field.value || ''}>
      {tiposRubrica.map((tipo) => (
        <MenuItem key={tipo.value} value={tipo.value}>
          {tipo.label}
        </MenuItem>
      ))}
    </Select>
  )}
/>
```

**Benefícios:**
- Integração correta com React Hook Form
- Validação automática
- Estado gerenciado centralizadamente

---

#### Campo Não Editável Após Criação

```tsx
// ✅ Desabilitar código em modo de edição
<TextField
  {...register('codigo', { required: 'Código é obrigatório' })}
  label="Código"
  disabled={!!selectedRubrica}  // ← Disabled se está editando
/>
```

**Benefícios:**
- Evita mudanças acidentais de código
- Preserva consistência de dados
- UX clara (usuário sabe que não pode alterar)

---

#### Valor Padrão de Porcentagem

```tsx
// ✅ Definir valor padrão de 100% para nova rubrica
const handleOpen = (rubrica?: Rubrica) => {
  if (rubrica) {
    // ... modo edição
  } else {
    setSelectedRubrica(null);
    reset();
    setValue('porcentagem', 100);  // ← Valor padrão
  }
  setOpen(true);
};
```

**Benefícios:**
- Melhora UX (maioria das rubricas usa 100%)
- Reduz erros de entrada
- Segue convenção do negócio

---

#### Mapeamento de Dados no Service

```tsx
// ✅ Mapear dados para compatibilidade frontend/backend
listarTodos: async (): Promise<Rubrica[]> => {
  const response = await api.get('/rubricas');
  return response.data.map((item: any) => ({
    ...item,
    tipo: item.tipoRubricaDescricao || item.tipo,
    tipoRubricaDescricao: item.tipoRubricaDescricao
  }));
},
```

**Benefícios:**
- Garante compatibilidade entre versões de API
- Flexibilidade para mudanças futuras
- Código mais robusto

---

## 🚀 Melhorias Futuras

### 1. Tipos de Rubrica Dinâmicos

**Problema Atual**: Tipos hardcoded no frontend

**Solução:**

**Backend:**
```java
@GetMapping("/tipos")
@Operation(summary = "Lista tipos de rubrica disponíveis")
public ResponseEntity<List<TipoRubricaDTO>> listarTipos() {
    return ResponseEntity.ok(tipoRubricaService.listarTodos());
}
```

**Frontend:**
```tsx
const [tiposRubrica, setTiposRubrica] = useState<TipoRubrica[]>([]);

useEffect(() => {
  const carregarTipos = async () => {
    const tipos = await api.get('/rubricas/tipos');
    setTiposRubrica(tipos.data);
  };
  carregarTipos();
  carregarRubricas();
}, []);
```

---

### 2. Busca e Filtros

**Frontend:**
```tsx
const [searchTerm, setSearchTerm] = useState('');
const [tipoFilter, setTipoFilter] = useState<string | null>(null);

const rubricasFiltradas = rubricas.filter(r => {
  const matchSearch = r.codigo.toLowerCase().includes(searchTerm.toLowerCase()) ||
                      r.descricao.toLowerCase().includes(searchTerm.toLowerCase());
  const matchTipo = !tipoFilter || r.tipoRubricaDescricao === tipoFilter;
  return matchSearch && matchTipo;
});

<TextField
  label="Buscar"
  value={searchTerm}
  onChange={(e) => setSearchTerm(e.target.value)}
/>

<Select
  label="Filtrar por Tipo"
  value={tipoFilter || ''}
  onChange={(e) => setTipoFilter(e.target.value || null)}
>
  <MenuItem value="">Todos</MenuItem>
  <MenuItem value="PROVENTO">Proventos</MenuItem>
  <MenuItem value="DESCONTO">Descontos</MenuItem>
  <MenuItem value="INFORMATIVO">Informativos</MenuItem>
</Select>
```

---

### 3. Paginação

**Backend:**
```java
@GetMapping
public ResponseEntity<Page<RubricaDTO>> listar(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String tipo
) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(rubricaService.listarTodas(pageable, tipo));
}
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

### 4. Ordenação por Coluna

**Frontend:**
```tsx
import { TableSortLabel } from '@mui/material';

const [orderBy, setOrderBy] = useState<'codigo' | 'descricao'>('codigo');
const [order, setOrder] = useState<'asc' | 'desc'>('asc');

const handleRequestSort = (property: 'codigo' | 'descricao') => {
  const isAsc = orderBy === property && order === 'asc';
  setOrder(isAsc ? 'desc' : 'asc');
  setOrderBy(property);
};

<TableCell>
  <TableSortLabel
    active={orderBy === 'codigo'}
    direction={orderBy === 'codigo' ? order : 'asc'}
    onClick={() => handleRequestSort('codigo')}
  >
    Código
  </TableSortLabel>
</TableCell>
```

---

### 5. Badge de Uso nas Folhas

**Backend:**
```java
// DTO estendido
public record RubricaComContagemDTO(
    Long id,
    String codigo,
    String descricao,
    String tipoRubricaDescricao,
    Double porcentagem,
    Boolean ativo,
    Long quantidadeUsos  // ← Novo campo
) {}

// Service
public List<RubricaComContagemDTO> listarTodasComContagem() {
    return rubricaRepository.findByAtivoTrue().stream()
        .map(r -> new RubricaComContagemDTO(
            r.getId(),
            r.getCodigo(),
            r.getDescricao(),
            r.getTipoRubrica().getDescricao(),
            r.getPorcentagem(),
            r.getAtivo(),
            folhaPagamentoRepository.countByRubricaId(r.getId())
        ))
        .collect(Collectors.toList());
}
```

**Frontend:**
```tsx
import { Badge, Chip } from '@mui/material';

<TableCell>
  {rubrica.descricao}
  {rubrica.quantidadeUsos > 0 && (
    <Chip 
      label={rubrica.quantidadeUsos} 
      size="small" 
      color="primary"
      style={{ marginLeft: 8 }}
    />
  )}
</TableCell>
```

---

### 6. Validação de Porcentagem por Tipo

**Backend:**
```java
@Transactional
public RubricaDTO cadastrar(RubricaDTO dto) {
    // Validações existentes...
    
    // ✅ Nova validação: desconto não pode ter porcentagem > 100%
    if ("DESCONTO".equals(dto.getTipoRubricaDescricao())) {
        if (dto.getPorcentagem() != null && dto.getPorcentagem() > 100) {
            throw new IllegalArgumentException("Desconto não pode ter porcentagem maior que 100%");
        }
    }
    
    // ... resto do código
}
```

---

### 7. Histórico de Alterações

**Migration:**
```sql
CREATE TABLE rubrica_audit (
    id SERIAL PRIMARY KEY,
    rubrica_id BIGINT NOT NULL,
    acao VARCHAR(10) NOT NULL, -- CREATE, UPDATE, DELETE
    usuario VARCHAR(100),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dados_antigos JSON,
    dados_novos JSON
);
```

**Backend:**
```java
@Service
public class RubricaAuditService {
    
    public void registrarAlteracao(Long rubricaId, String acao, String usuario, 
                                   RubricaDTO dadosAntigos, RubricaDTO dadosNovos) {
        // Salva no histórico
    }
}
```

---

## 📝 Conclusão

A tela de **Rubricas** demonstra uma implementação CRUD com características intermediárias:

✅ **Relacionamento @ManyToOne** com TipoRubrica  
✅ **Validação de código único** (campo não editável)  
✅ **Campo opcional** (porcentagem)  
✅ **Valor padrão** (100%)  
✅ **Controller do React Hook Form** para Select  
✅ **Mapeamento de dados** entre frontend e backend  
✅ **Soft Delete** preservando histórico  

Esta implementação serve como **referência** para entidades que possuem:

- Campo único não editável (CPF, CNPJ, matrícula)
- Relacionamento simples com entidade de classificação
- Campos opcionais com valores padrão
- Selects com valores predefinidos

**Exemplos de uso similar:**
- Funcionários (CPF único, relacionamento com Cargo)
- Produtos (código único, relacionamento com Categoria)
- Clientes (CNPJ único, relacionamento com Segmento)

---

## 📊 Estatísticas Finais

| Métrica | Valor |
|---------|-------|
| **Total de Código** | ~595 linhas |
| **Backend** | 297 linhas |
| **Frontend** | 298 linhas |
| **Arquivos Backend** | 6 |
| **Arquivos Frontend** | 2 |
| **Endpoints REST** | 5 |
| **Operações CRUD** | 5 (List, Get, Create, Update, Delete) |
| **Relacionamentos JPA** | 1 (@ManyToOne TipoRubrica) |
| **Campos da Entidade** | 6 (id, codigo, descricao, tipoRubrica, porcentagem, ativo) |
| **Validações Especiais** | Código único, Tipo de rubrica válido |
| **Complexidade** | **Média** (CRUD + Relacionamento + Validação Única) |

---

**Documento gerado em:** 16 de Outubro de 2025  
**Versão do Sistema:** 1.0  
**Autor:** Sistema de Documentação Automática

