# Alteração Benefícios Mensais — Design

**Spec**: `_docs/specs/features/alteracao-beneficios-mensais/spec.md`
**Status**: Draft

---

## Architecture Overview

Nenhuma mudança arquitetural. O fluxo permanece idêntico:

```
Upload .xlsx → Controller → ImportacaoBeneficioMensalService → Repository.save()
```

As alterações são internas ao service (mapeamento de colunas) e ao seed SQL (códigos de tipo).

---

## Code Reuse Analysis

### Componentes existentes (sem mudança)

| Componente | Location | Impacto |
|-----------|----------|---------|
| `BeneficioMensal` (entity) | `model/BeneficioMensal.java` | Nenhum — schema inalterado |
| `BeneficioMensalDTO` (record) | `dto/BeneficioMensalDTO.java` | Nenhum |
| `BeneficioMensalRepository` | `repository/BeneficioMensalRepository.java` | Nenhum |
| `BeneficioMensalService` | `service/BeneficioMensalService.java` | Nenhum |
| `BeneficioMensalController` | `controller/BeneficioMensalController.java` | Nenhum |
| `ImportacaoBeneficioMensalController` | `controller/ImportacaoBeneficioMensalController.java` | Nenhum |
| `FolhaTotalizacaoService` | `service/FolhaTotalizacaoService.java` | Nenhum — consome `valor`, `funcionario`, `tipoBeneficio`, `competencia` |
| `V1.13__create_beneficio_mensal.sql` | `db/migration/` | Nenhum — DDL inalterado |

### Componentes alterados

| Componente | Location | Mudança |
|-----------|----------|---------|
| `V1.12__create_tipo_beneficio.sql` | `db/migration/` | Seed data: códigos texto → numéricos |
| `ImportacaoBeneficioMensalService` | `service/` | Constantes de mapeamento + nome da aba |
| `ImportacaoBeneficioMensalServiceTest` | `test/.../service/` | Helper de workbook + dados de teste |

---

## Mudanças Detalhadas

### 1. V1.12 — Seed data `tipo_beneficio`

DDL da tabela (`CREATE TABLE`) permanece idêntico. Apenas o `INSERT` muda:

| Antes (código texto) | Depois (código numérico) | Descrição (da planilha real) |
|---------------------|-------------------------|------------------------------|
| `SEGUROS` | `5322` | Seguros - Custo Empresa |
| `VALE_REFEICAO` | `5612` | Vale Refeição - Custo Empresa |
| `VALE_TRANSPORTE` | `5903` | Vale Transporte - Custo Empresa |
| `VALE_ALIMENTACAO` | `5615` | Vale Alimentação - Custo Empresa |
| `AM_UNIMED_CE` | `4000` | Assi Med - UNIMED CE- Custo Empresa |
| `AM_GNDI_INTERMEDICA` | `4003` | Assi Medic-GNDI Inter Custo Empresa |
| `AM_OMINT` | `4002` | Assi Med - OMINT MEDI CUSTO EMPRESA |
| `AM_UNIMED_SALVADOR` | `4001` | Assi Med-UNIMED SALV- Custo Empresa |

A coluna `tipo_beneficio.codigo` é `VARCHAR(50)` — armazena `"5322"` como string, sem necessidade de mudar DDL.

### 2. ImportacaoBeneficioMensalService — Constantes

**Antes (layout hipotético):**

```java
private static final String ABA_LANCAMENTOS = "Lancamentos";
private static final int COL_CPF = 0;
private static final int COL_NOME = 1;
private static final int COL_DESCRICAO = 2;
private static final int COL_CODIGO = 3;
private static final int COL_VALOR = 4;
```

**Depois (layout planilha real):**

```java
private static final String ABA_PLANILHA = "Planilha1";
private static final int COL_NOME = 1;       // B
private static final int COL_CPF = 2;         // C
private static final int COL_CODIGO = 8;      // I
private static final int COL_DESCRICAO = 9;   // J
private static final int COL_VALOR = 13;      // N
```

### 3. ImportacaoBeneficioMensalService — Leitura do código

O código do tipo de benefício (coluna I) vem como **número inteiro** na planilha (ex.: `5322`), não como texto. O método `lerTexto()` via `DataFormatter` já converte numéricos para string, mas pode incluir formatação decimal (ex.: `"5322.0"`).

**Mudança necessária**: ao ler a célula do código, se for numérica, obter como `long` e converter para `String` sem casas decimais:

```java
String codigo = lerCodigoTipoBeneficio(row.getCell(COL_CODIGO));
```

Método novo:

```java
private String lerCodigoTipoBeneficio(Cell cell) {
    if (cell == null) {
        return "";
    }
    if (cell.getCellType() == CellType.NUMERIC) {
        return String.valueOf((long) cell.getNumericCellValue());
    }
    return dataFormatter.formatCellValue(cell).trim();
}
```

### 4. ImportacaoBeneficioMensalServiceTest — Helper refatorado

O helper `workbookComLinhas` precisa gerar planilha com aba `"Planilha1"` e 15 colunas (posicionando dados nos índices corretos).

**Mudanças no helper:**

```java
// Antes
var sheet = workbook.createSheet("Lancamentos");
header: [CPF(0), Nome(1), Descrição(2), Código(3), Valor(4)]
dados:  [cpf(0), nome(1), descricao(2), codigo(3), valor(4)]

// Depois
var sheet = workbook.createSheet("Planilha1");
header: [Matrícula(0), Nome(1), CPF(2), ..., Código(8), Descrição(9), ..., Valor(13), ...]
dados:  [matricula(0), nome(1), cpf(2), ..., codigo(8), descricao(9), ..., valor(13), ...]
```

O método `linha()` passa a receber os campos na nova ordem e posicionar nas colunas corretas. Código do tipo de benefício deve ser numérico (ex.: `5612` em vez de `"VALE_REFEICAO"`). CPF deve ser numérico (ex.: `12345678901` como double, não `"12345678901"` como texto).

---

## Error Handling

Nenhuma mudança no tratamento de erros. Mesma estrutura atual:

| Cenário | Handling | Mensagem |
|---------|----------|----------|
| Aba "Planilha1" não existe | `IllegalArgumentException` | "Aba 'Planilha1' não encontrada no arquivo" |
| CPF não encontrado | Adiciona ao `detalhesErros` | "Funcionário ativo não encontrado para o CPF informado" |
| Código tipo inexistente | Adiciona ao `detalhesErros` | "Tipo de benefício não encontrado para o código: 9999" |
| Valor vazio | Adiciona ao `detalhesErros` | "Valor é obrigatório" |
| Qualquer erro → nenhum registro salvo | `ImportacaoBeneficioMensalInvalidaException` | Lista detalhada de erros |

---

## Tech Decisions

| Decisão | Escolha | Razão |
|---------|---------|-------|
| Código tipo como String mesmo sendo numérico | Manter `VARCHAR(50)` na coluna `codigo` | Flexibilidade para futuros tipos com código não-numérico; evita mudança de DDL |
| Converter código numérico via cast `(long)` | Método dedicado `lerCodigoTipoBeneficio()` | `DataFormatter` pode retornar `"5322.0"` para numéricos; cast para `long` garante `"5322"` |
| CPF numérico na planilha | Reutilizar `normalizarCpf()` existente | Já trata padding com zeros à esquerda; `DataFormatter` converte numérico para string |

---

## Requirement Coverage

| Requirement | Covered by |
|-------------|-----------|
| BENM-01: Seed data códigos numéricos | V1.12 SQL — seed data |
| BENM-02: Aba "Planilha1" | Service — constante `ABA_PLANILHA` |
| BENM-03: Mapeamento colunas | Service — constantes `COL_*` |
| BENM-04: Código numérico → String | Service — `lerCodigoTipoBeneficio()` |
