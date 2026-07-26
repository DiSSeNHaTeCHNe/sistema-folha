# Alteração Benefícios Mensais — Tasks

**Design**: `_docs/specs/features/alteracao-beneficios-mensais/design.md`
**Status**: Complete

---

## Execution Plan

```
Phase 1 (Parallel):
  ├── T1 [P]  Migration: seed data códigos numéricos
  └── T2 [P]  Service + Test: mapeamento colunas planilha real

Phase 2 (Sequential):
  T3  Gate: compilação + testes
```

---

## Task Breakdown

### T1: Migration — seed data tipo_beneficio com códigos numéricos [P]

**What**: Atualizar o `INSERT` de V1.12 trocando códigos texto (`SEGUROS`, `VALE_REFEICAO`, etc.) por códigos numéricos (`5322`, `5612`, etc.) com descrições da planilha real.
**Where**: `backend/src/main/resources/db/migration/V1.12__create_tipo_beneficio.sql`
**Depends on**: Nenhuma
**Reuses**: DDL existente (só muda o bloco `INSERT ... VALUES`)
**Requirement**: BENM-01

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] INSERT contém exatamente 8 registros com códigos: `4000`, `4001`, `4002`, `4003`, `5322`, `5612`, `5615`, `5903`
- [x] Descrições correspondem à planilha real
- [x] DDL (`CREATE TABLE`, `CREATE INDEX`) inalterado
- [x] Arquivo é SQL válido

**Tests**: none (Flyway migrations — gate manual / future CI, per TESTING.md)
**Gate**: build (`mvn clean compile`)

---

### T2: Service + Test — mapeamento colunas planilha real [P]

**What**: Atualizar constantes de coluna e nome da aba em `ImportacaoBeneficioMensalService` para o layout real da planilha (Planilha1, 15 colunas). Adicionar método `lerCodigoTipoBeneficio()` para tratar código numérico. Atualizar testes unitários para gerar workbook mock com novo layout.
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/service/ImportacaoBeneficioMensalService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/service/ImportacaoBeneficioMensalServiceTest.java`
**Depends on**: Nenhuma
**Reuses**: Métodos existentes `lerTexto()`, `lerValor()`, `normalizarCpf()` (sem mudança)
**Requirement**: BENM-02, BENM-03, BENM-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Changes no Service**:
1. Constante `ABA_LANCAMENTOS = "Lancamentos"` → `ABA_PLANILHA = "Planilha1"`
2. Constantes de coluna:
   - `COL_CPF`: 0 → **2** (coluna C)
   - `COL_NOME`: 1 → **1** (coluna B — sem mudança)
   - `COL_DESCRICAO`: 2 → **9** (coluna J)
   - `COL_CODIGO`: 3 → **8** (coluna I)
   - `COL_VALOR`: 4 → **13** (coluna N)
3. Novo método `lerCodigoTipoBeneficio(Cell)`: se numérico, cast `(long)` → `String.valueOf()` para evitar `"5322.0"`
4. No loop de processamento: trocar `lerTexto(row.getCell(COL_CODIGO))` por `lerCodigoTipoBeneficio(row.getCell(COL_CODIGO))`

**Changes no Test**:
1. Helper `workbookComLinhas()`: aba `"Planilha1"`, header com 15 colunas, dados posicionados nos índices corretos
2. Método `linha()`: recebe dados na nova estrutura e posiciona CPF como numérico (idx 2), código como numérico (idx 8), valor como numérico (idx 13)
3. Atualizar `setUp()`: `tipoBeneficio.setCodigo("5612")` em vez de `"VALE_REFEICAO"`
4. Atualizar dados de mock em cada `@Test`: códigos numéricos (`5612`, `5322`, etc.)
5. Atualizar teste de aba inválida: verificar mensagem `"Planilha1"` em vez de `"Lancamentos"`

**Done when**:
- [x] Service lê aba "Planilha1"
- [x] Constantes de coluna refletem layout real (CPF=2, Nome=1, Desc=9, Codigo=8, Valor=13)
- [x] Código numérico do tipo é lido sem casas decimais
- [x] Todos os 6 testes existentes passam com dados atualizados
- [x] Gate check passa: `mvn test`
- [x] Test count: 6 testes passam (nenhum teste removido)

**Tests**: unit (Service business rules, per TESTING.md)
**Gate**: quick (`mvn test`)

---

### T3: Gate — compilação completa + testes

**What**: Verificar que T1 + T2 integram corretamente: compilação sem erros e todos os testes passam.
**Where**: Raiz do backend
**Depends on**: T1, T2
**Reuses**: N/A
**Requirement**: Todos (BENM-01 a BENM-04)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `mvn clean compile` → exit 0
- [x] `mvn test` → exit 0, 6+ testes de `ImportacaoBeneficioMensalServiceTest` passam
- [x] Nenhum warning de Flyway (SQL sintaticamente válido)

**Tests**: none (validação cruzada)
**Gate**: build (`mvn clean compile` + `mvn test`)

---

## Validation Tables

### Task Granularity Check

| Task | Scope | Status |
|------|-------|--------|
| T1: Migration seed data | 1 arquivo SQL, 1 bloco INSERT | ✅ Granular |
| T2: Service + Test | 2 arquivos coesos (service + seu teste unitário) | ✅ Granular (co-located per TESTING.md) |
| T3: Gate compilação | 0 arquivos, 2 comandos | ✅ Granular |

### Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
|------|-------------------|---------------|--------|
| T1 | Nenhuma | Parallel, sem seta de entrada | ✅ Match |
| T2 | Nenhuma | Parallel, sem seta de entrada | ✅ Match |
| T3 | T1, T2 | Após Phase 1 (T1 e T2) | ✅ Match |

### Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
|------|-----------|-----------------|-----------|--------|
| T1: Migration | Flyway migration | none (Manual / future CI) | none | ✅ OK |
| T2: Service + Test | Service (business rules) | Unit (mock repos) | unit | ✅ OK |
| T3: Gate | N/A (nenhum código) | N/A | none | ✅ OK |

---

## Status

| Task | Status | Assignee |
|------|--------|----------|
| T1 | Complete | agent |
| T2 | Complete | agent |
| T3 | Complete | agent |
