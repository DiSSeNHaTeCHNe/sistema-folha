# Alteração Benefícios Mensais — Specification

## Problem Statement

As migrações e o serviço de importação de benefícios mensais foram construídos com base num layout de planilha hipotético (aba "Lancamentos", 5 colunas, códigos texto como `SEGUROS`, `VALE_REFEICAO`). A planilha real de custo/benefício ("Relatorio Custo Beneficio Folha MM_AAAA.xlsx") tem estrutura diferente: 15 colunas, aba "Planilha1" e códigos numéricos (`4000`, `5322`, etc.). A importação falha porque não encontra a aba nem os campos esperados.

## Goals

- [x] Adaptar seed data de `tipo_beneficio` para códigos numéricos reais da planilha
- [x] Alinhar o serviço de importação ao layout real: aba "Planilha1", mapeamento correto de colunas (CPF na col C, código na col I, valor na col N)
- [x] Manter schema de `beneficio_mensal` inalterado (sem colunas novas)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Adicionar colunas novas em `beneficio_mensal` | Escopo explícito: sem campos extras |
| Extração automática de competência dos cabeçalhos | Competência continua informada como parâmetro da API |
| Import da Sheet "Planilha2" (resumo/pivot) | Apenas totalização; sem dados granulares |
| Auto-criação de `TipoBeneficio` durante importação | Códigos devem existir previamente via CRUD ou seed |
| Alterações no frontend | Escopo restrito a backend |

---

## Análise da Planilha Real

### Layout — Sheet "Planilha1" (900 linhas, 15 colunas)

| Col | Header | Tipo | Usado na importação? |
|-----|--------|------|---------------------|
| A | Matrícula | int | Não |
| B | Nome | string | Sim — identificação em erros |
| C | CPF | number (sem formatação) | **Sim — FK → funcionarios** |
| D | Data Admissão | date | Não |
| E | Data Rescisão | date/null | Não |
| F | Estabelecimento | string | Não |
| G | C.R. | string | Não |
| H | Cód. Centro Custo Contabilidade | int | Não |
| I | Código (benefício) | int | **Sim — FK → tipo_beneficio.codigo** |
| J | Descrição (benefício) | string | Sim — observação |
| K | Clas. | string | Não |
| L | Processo | string | Não |
| M | MAI/26 - Hora | number | Não |
| N | MAI/26 - Valor | decimal | **Sim — valor** |
| O | MAI/26 - Dt Pgto | date | Não |

### Códigos de Tipo de Benefício reais (coluna I)

| Código | Descrição na planilha |
|--------|----------------------|
| 4000 | Assi Med - UNIMED CE- Custo Empresa |
| 4001 | Assi Med-UNIMED SALV- Custo Empresa |
| 4002 | Assi Med - OMINT MEDI CUSTO EMPRESA |
| 4003 | Assi Medic-GNDI Inter Custo Empresa |
| 5322 | Seguros - Custo Empresa |
| 5612 | Vale Refeição - Custo Empresa |
| 5615 | Vale Alimentação - Custo Empresa |
| 5903 | Vale Transporte - Custo Empresa |

---

## User Stories

### P1: Seed data alinhada à planilha real ⭐ MVP

**User Story**: Como operador de folha, quero que os tipos de benefício no sistema correspondam aos códigos da planilha real, para que a importação consiga localizar cada tipo.

**Why P1**: Sem os códigos corretos, o import falha em 100% das linhas.

**Acceptance Criteria**:

1. WHEN migration V1.12 executa THEN sistema SHALL inserir `tipo_beneficio` com códigos `4000`, `4001`, `4002`, `4003`, `5322`, `5612`, `5615`, `5903` e descrições da planilha real
2. WHEN tipo_beneficio já existia com códigos antigos (texto) THEN migration SHALL substituir pelo seed atualizado (arquivos não commitados, sem impacto em produção)

**Independent Test**: `SELECT codigo, descricao FROM tipo_beneficio ORDER BY codigo` retorna 8 registros com códigos numéricos.

---

### P1: Importação com mapeamento correto ⭐ MVP

**User Story**: Como operador de folha, quero fazer upload da planilha "Relatorio Custo Beneficio Folha" e ter os lançamentos importados corretamente.

**Why P1**: É o fluxo principal de operação mensal.

**Acceptance Criteria**:

1. WHEN operador faz upload de .xlsx THEN serviço SHALL ler a aba "Planilha1" (não "Lancamentos")
2. WHEN serviço processa cada linha THEN SHALL extrair: CPF da **coluna C** (idx 2), nome da **coluna B** (idx 1), descrição da **coluna J** (idx 9), código do tipo de benefício da **coluna I** (idx 8), valor da **coluna N** (idx 13)
3. WHEN CPF (coluna C) é numérico sem formatação THEN serviço SHALL normalizar para 11 dígitos com zeros à esquerda
4. WHEN código do tipo (coluna I) é numérico THEN serviço SHALL converter para String e buscar em `tipo_beneficio.codigo`
5. WHEN tipo de benefício não é encontrado pelo código THEN serviço SHALL registrar erro com linha, CPF, nome e código inválido
6. WHEN existem lançamentos prévios para mesma competência e `confirmar=false` THEN serviço SHALL lançar exceção de duplicidade (comportamento preservado)

**Independent Test**: Upload da planilha real via `/api/importacao/beneficios-mensais` com sucesso.

---

## Edge Cases

- WHEN CPF (coluna C) tem menos de 11 dígitos THEN normalizar com zeros à esquerda (já implementado)
- WHEN valor (coluna N) é zero THEN aceitar normalmente (zero é válido)
- WHEN valor (coluna N) está vazio THEN registrar erro (valor obrigatório)
- WHEN aba "Planilha1" não existe no arquivo THEN lançar exceção com mensagem clara
- WHEN linha vazia (CPF null) THEN pular silenciosamente
- WHEN código numérico não existe na tabela tipo_beneficio THEN registrar erro sem abortar demais linhas

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---------------|-------|-------|--------|
| BENM-01 | P1: Seed data — tipo_beneficio códigos numéricos | Done | T1 |
| BENM-02 | P1: Import — aba "Planilha1" | Done | T2 |
| BENM-03 | P1: Import — mapeamento colunas corretas (C, B, J, I, N) | Done | T2 |
| BENM-04 | P1: Import — código tipo benefício numérico → String | Done | T2 |

**Coverage:** 4 total, 4 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] Planilha real importa com sucesso via `/api/importacao/beneficios-mensais` (validação manual pendente)
- [x] Tipos de benefício com códigos numéricos corretos no banco
- [x] `FolhaTotalizacaoService` continua funcionando sem alterações
- [x] `mvn clean compile` sem erros
