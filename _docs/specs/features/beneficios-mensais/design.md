# Benefícios Mensais — Design

## Architecture Decision

### Abordagem: Novo módulo espelhando `folha_pagamento`

Nova tabela `beneficio_mensal` com FK para `tipo_beneficio`, mesma representação de competência (data_inicio/data_fim = primeiro e último dia do mês), mesmos padrões de acesso por organograma.

**Justificativa**: Manter consistência com o padrão existente de `folha_pagamento` facilita reuso de componentes (frontend e backend), diminui curva de aprendizado e garante que benefícios mensais possam ser cruzados com a folha na mesma competência.

---

## Data Model

### Nova entidade: `TipoBeneficio`

```
tipo_beneficio
├── id: BIGSERIAL PK
├── codigo: VARCHAR(50) NOT NULL UNIQUE
├── descricao: VARCHAR(200) NOT NULL
├── ativo: BOOLEAN NOT NULL DEFAULT TRUE
├── data_criacao: TIMESTAMP DEFAULT NOW()
└── data_atualizacao: TIMESTAMP DEFAULT NOW()
```

### Nova entidade: `BeneficioMensal`

```
beneficio_mensal
├── id: BIGSERIAL PK
├── funcionario_id: BIGINT FK → funcionarios(id) NOT NULL
├── tipo_beneficio_id: BIGINT FK → tipo_beneficio(id) NOT NULL
├── valor: DECIMAL(10,2) NOT NULL
├── competencia_inicio: DATE NOT NULL
├── competencia_fim: DATE NOT NULL
├── observacao: VARCHAR(500)
├── ativo: BOOLEAN NOT NULL DEFAULT TRUE
├── data_criacao: TIMESTAMP DEFAULT NOW()
├── data_atualizacao: TIMESTAMP DEFAULT NOW()
├── criado_por: VARCHAR(100)
└── atualizado_por: VARCHAR(100)
```

**Índices:**
- `idx_beneficio_mensal_competencia` ON (competencia_inicio, competencia_fim)
- `idx_beneficio_mensal_func_comp` ON (funcionario_id, competencia_inicio) — busca por funcionário/mês
- UNIQUE (funcionario_id, tipo_beneficio_id, competencia_inicio) — impede duplicata por tipo/mês

### Resumo (view ou query agregada)

Não criar tabela separada de resumo (diferente de `resumo_folha_pagamento`). O resumo será calculado em tempo de consulta via GROUP BY `tipo_beneficio_id`, pois o volume esperado (centenas de registros/mês) não justifica materialização.

---

## Backend Components

### Layer Map

```
controller/
├── TipoBeneficioController.java       → CRUD tipos (admin only)
└── BeneficioMensalController.java     → CRUD + consulta + importação

dto/
├── TipoBeneficioDTO.java
├── BeneficioMensalDTO.java
├── BeneficioMensalResumoDTO.java      → {codigo, descricao, total, qtdLancamentos}
└── ImportacaoResultadoDTO.java        → {processadas, erros, totalValor}

model/
├── TipoBeneficio.java
└── BeneficioMensal.java

repository/
├── TipoBeneficioRepository.java
└── BeneficioMensalRepository.java

service/
├── TipoBeneficioService.java
├── BeneficioMensalService.java        → CRUD + resumo
└── ImportacaoBeneficioMensalService.java → Parser .xlsx + persistência
```

### Endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | /tipo-beneficio | authenticated | Listar tipos ativos |
| POST | /tipo-beneficio | ADMIN | Criar tipo |
| PUT | /tipo-beneficio/{id} | ADMIN | Atualizar tipo |
| DELETE | /tipo-beneficio/{id} | ADMIN | Soft delete tipo |
| GET | /beneficio-mensal?competenciaInicio&competenciaFim | authenticated + organograma | Listar lançamentos (filtro acesso) |
| GET | /beneficio-mensal/resumo?competenciaInicio&competenciaFim | authenticated + organograma | Resumo agrupado por tipo |
| GET | /beneficio-mensal/funcionario/{id}?competenciaInicio&competenciaFim | authenticated + organograma | Lançamentos de um funcionário |
| POST | /beneficio-mensal | authenticated | Criar lançamento manual |
| DELETE | /beneficio-mensal/{id} | authenticated | Soft delete lançamento |
| POST | /importacao/beneficios-mensais?competenciaInicio&competenciaFim&confirmar=false | authenticated | Importar .xlsx |

### Access Control

Reusar `OrganogramaAcessoService.obterCentrosCustoAcessiveis(usuarioId)`:
- Empty set → acesso irrestrito (admin/sem nó no organograma)
- Non-empty → filtrar `beneficio_mensal` por `funcionario.centroCusto.id IN centrosAcessiveis`

Tipos de benefício: CRUD restrito a role `ADMIN` (via `@PreAuthorize` ou check manual de role no SecurityConfig).

### Import Logic (xlsx)

1. Receber `MultipartFile` + `competenciaInicio` + `competenciaFim` + `confirmar` (boolean)
2. Abrir workbook via Apache POI (já no classpath Spring Boot Starter)
3. Ler aba "Lancamentos" a partir da row 2 (skip header)
4. Para cada row com CPF preenchido:
   - Buscar `Funcionario` ativo por CPF
   - Buscar `TipoBeneficio` por código (coluna D)
   - Validar valor ≥ 0
   - Acumular registro ou erro
5. Verificar duplicidade: se existem registros para mesma competência
   - `confirmar=false` → HTTP 409 com resumo do que será substituído
   - `confirmar=true` → soft delete dos anteriores + inserir novos
6. Retornar `ImportacaoResultadoDTO`

### Dependência: Apache POI

Já utilizado? Verificar `pom.xml`. Se não, adicionar:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## Frontend Components

### Page: `BeneficiosMensais/index.tsx`

**Layout** (seguir padrão Funcionarios/FolhaPagamento):

```
┌─────────────────────────────────────────┐
│ Benefícios Mensais         [Importar]   │
├─────────────────────────────────────────┤
│ Competência: [Mês ▼] [Ano ▼]           │
├─────────────────────────────────────────┤
│ Resumo por Tipo                         │
│ ┌───────┬────────────────┬──────┬─────┐ │
│ │Código │ Descrição      │Total │ Qtd │ │
│ ├───────┼────────────────┼──────┼─────┤ │
│ │SEGUROS│ Seguros - Custo│R$...│  5  │ │
│ │ (expandir → lista funcionários)     │ │
│ └───────┴────────────────┴──────┴─────┘ │
│                                         │
│ Total Geral: R$ X.XXX,XX               │
└─────────────────────────────────────────┘
```

**Componentes reutilizados:**
- `formatarDataCompetencia` (já existe em utils)
- Selectors de competência (padrão FolhaPagamento)
- Layout com Card + filtros (padrão Funcionarios)

**Services:**
- `tipoBeneficioService.ts` — CRUD tipos
- `beneficioMensalService.ts` — listar, resumo, importar

### Dialog de Importação

- File input `.xlsx`
- Seletor de competência (mês/ano)
- Botão "Importar"
- Se 409 → exibir alerta "Já existem dados para esta competência. Deseja substituir?" → [Cancelar] [Substituir]
- Resultado: snackbar com "X registros importados, Y erros"

---

## Migration Strategy

### V1.12 — `tipo_beneficio` + seed
### V1.13 — `beneficio_mensal` + índices

Duas migrations para rollback granular.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Apache POI não está no classpath | Build fail | Adicionar ao pom.xml como primeira task |
| Schema drift entre Flyway e Hibernate | Startup fail se `ddl-auto=validate` | Manter `ddl-auto=update` e garantir Flyway ≥ DDL |
| Volume de dados cresce | Query lenta no resumo | Índice composto + paginação; materializar resumo só se > 10k rows/mês |
| Tabela `beneficios` legada conflita com `beneficio_mensal` | Dupla contagem no custo | `FolhaTotalizacaoService` prioriza `beneficio_mensal` quando presente |
