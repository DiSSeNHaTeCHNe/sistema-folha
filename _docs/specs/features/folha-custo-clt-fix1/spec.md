# folha-custo-clt-fix1 — Processamento automático pós-import ADP Specification

**Parent:** `_docs/specs/features/folha-custo-clt/` (P2 hook pós-import; alinha FCLT-04)  
**Complexity:** Medium  
**Spec status:** Done 2026-07-28 — Execute T1–T8 complete on `feat/folha-custo-clt`

## Problem Statement

Após importar folha ADP, o operador vê totais nos cards mas o **detalhe por rubrica** falha com *“Ficha não processada…”* porque `POST /folha-pagamento/processar` é passo **manual** separado (decisão ARCH-1 Opção B, design P2 não implementado). Isso quebra a expectativa de “importei a folha de maio — está pronta” e gerou incidente real (competência 5/2026: 3.928 linhas ADP, 0 fichas).

## Goals

- [x] Encadear **importação ADP → processamento ficha** numa única operação bem-sucedida
- [x] Retornar **sucesso** à UI/API **somente** quando importação **e** processamento terminarem OK
- [x] Oferecer **reprocessamento manual** em Cadastros → Importação para competências já importadas

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Processamento automático na importação de **benefícios mensais** | Módulo separado (INT-1); benefícios compõem na consulta |
| Botão de processar em Folha de Pagamento | Deferred em `context.md` FIX1-CTX-01 |
| Processamento assíncrono / job em background | Volume atual; sync suficiente |
| Alterar ARCH-1 Opção B (ficha continua projeção materializada) | Escopo = gatilho, não schema |
| Mudar regra ADMIN em `/processar` | Mantida; importação já é fluxo admin |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Transação única import+process | Rollback import se process falhar | “Feito” = ambos; ver FIX1-CTX-02 | y |
| Cross-domain via port | `FolhaProcessamentoPort` | AD-010 | y |
| `recalcularFerias` no auto | `false` | FIX1-CTX-04 | y |
| Botão manual | Página `/importacao` | FIX1-CTX-01 | y |
| Reprocesso manual | `POST /folha-pagamento/processar` existente | Reuso API T6 | y |
| Auth importação | Fluxo admin existente | `/importacao` já em `AdminRoute` | y |

**Open questions:** none — ver `context.md`.

**Remaining dimensions N/A for this scope:** rate limits, external deps (ADP file unchanged), data expiry.

---

## User Stories

### P1: Importação ADP encadeia processamento ⭐ MVP

**User Story**: Como operador RH admin, quero que ao importar folha ADP o sistema já materialize a ficha mensal, para usar detalhe Bruto/Líquido/Custo sem passo extra.

**Why P1**: Fecha gap operacional e FCLT-04; elimina estado “importado sem ficha”.

**Acceptance Criteria**:

1. (FIX1-01) WHEN importação ADP concluir com sucesso (incluindo substituição confirmada) THEN sistema SHALL invocar processamento da **mesma** `competenciaInicio`, `competenciaFim` e `decimoTerceiro` da importação, com `opcoes.recalcularFerias=false`
2. (FIX1-02) WHEN importação **e** processamento concluírem THEN API SHALL retornar `success=true` e mensagem indicando importação **e** processamento (ex.: totais de registros importados + `totalFichas`/`totalLinhas` do processamento)
3. (FIX1-03) WHEN processamento falhar após persistência da importação na mesma transação THEN API SHALL retornar erro (`success=false`), **não** persistir importação (rollback), e mensagem SHALL indicar falha no processamento da ficha
4. (FIX1-04) WHEN importação falhar antes do processamento THEN processamento **não** SHALL ser invocado e resposta SHALL refletir falha de importação apenas
5. (FIX1-05) WHEN operação encadeada concluir THEN SHALL existir ao menos uma `ficha_mensal` por funcionário CLT processado na competência (refina FCLT-04)

**Independent Test**: Importar ADP de competência de teste → resposta 200 com stats de processamento → detalhe por funcionário abre abas sem erro “Ficha não processada”.

---

### P1: UI importação reflete pipeline completo ⭐ MVP

**User Story**: Como operador, quero feedback claro na tela de Importação de que importação **e** ficha foram geradas juntas.

**Acceptance Criteria**:

1. (FIX1-06) WHEN importação encadeada retornar sucesso THEN UI Importação Folha ADP SHALL exibir toast/mensagem de sucesso mencionando **importação e processamento** (não só “arquivo importado”)
2. (FIX1-07) WHEN API retornar erro FIX1-03 THEN UI SHALL exibir erro e **não** estado de sucesso parcial
3. (FIX1-08) WHEN importação encadeada em andamento THEN UI SHALL indicar progresso (ex.: “Importando e processando ficha…”) até resposta final

**Independent Test**: Upload ADP → toast único de sucesso com texto composto; simular erro de processamento → toast de erro, sem banner verde.

---

### P2: Reprocessamento manual em Cadastros → Importação

**User Story**: Como operador admin, quero reprocessar uma competência já importada (ex.: após alterar rubrica fixa), sem reenviar o arquivo ADP.

**Acceptance Criteria**:

1. (FIX1-09) WHEN admin acessar `/importacao` THEN SHALL existir seção **“Processar ficha da competência”** com seleção mês/ano, checkbox 13º salário e botão **Processar**
2. (FIX1-10) WHEN admin clicar Processar THEN UI SHALL chamar `POST /folha-pagamento/processar` com competência selecionada e SHALL exibir resultado (`totalFichas`, `totalLinhas`) ou erro da API
3. (FIX1-11) WHEN usuário não-admin tentar processar THEN API SHALL retornar **403** (regra existente) e UI SHALL exibir mensagem de permissão
4. (FIX1-12) WHEN admin marcar opção **Recalcular férias proporcionais** no reprocesso manual THEN request SHALL enviar `opcoes.recalcularFerias=true`
5. (FIX1-13) WHEN reprocesso manual concluir com sucesso THEN toast SHALL confirmar processamento da competência informada

**Independent Test**: Sem reimportar ADP, selecionar 5/2026 → Processar → detalhe folha passa a abrir; alterar rubrica fixa → reprocessar → totais atualizados.

---

## Edge Cases

- WHEN competência importada sem linhas CLT processáveis THEN processamento SHALL completar com `totalFichas=0` e resposta ainda `success=true` se importação OK (sem linhas ADP válidas)
- WHEN timeout de importação grande THEN cliente mantém timeout estendido existente (5 min); operação continua síncrona
- WHEN reprocesso manual em competência **sem** importação ADP THEN API processamento SHALL persistir ficha vazia/zero conforme motor atual e UI SHALL informar totais retornados
- WHEN substituição de competência (409 → confirmar) THEN encadeamento FIX1-01 SHALL rodar **após** substituição confirmada, na mesma transação

---

## API / Contract Notes (informative)

| Endpoint | Change |
| -------- | ------ |
| `POST /importacao/folha-adp` | Orquestra import + process; estende response DTO |
| `POST /folha-pagamento/processar` | Inalterado; usado pelo botão manual FIX1-09 |
| Novo `FolhaProcessamentoPort` | `processar(competenciaInicio, competenciaFim, decimoTerceiro, opcoes)` |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| FIX1-01 | P1 encadeamento | T2, T3 | Done |
| FIX1-02 | P1 resposta sucesso | T4 | Done |
| FIX1-03 | P1 rollback | T3, T4 | Done |
| FIX1-04 | P1 import falha | T3 | Done |
| FIX1-05 | P1 ficha existe (FCLT-04) | T3 | Done |
| FIX1-06 | P1 UI sucesso | T5 | Done |
| FIX1-07 | P1 UI erro | T5 | Done |
| FIX1-08 | P1 UI loading | T5 | Done |
| FIX1-09 | P2 botão/seção | T7 | Done |
| FIX1-10 | P2 chama API | T6, T7 | Done |
| FIX1-11 | P2 ACL ADMIN | T7, T8 | Done |
| FIX1-12 | P2 recalcular férias | T6, T7 | Done |
| FIX1-13 | P2 toast | T7 | Done |

**Coverage:** 13 total, 8 tasks mapped (T1–T8)

**Cross-ref:** Atualizar `folha-custo-clt/spec.md` FCLT-04 traceability na Execute de `folha-custo-clt-fix1`.

---

## Success Criteria

- [x] Importar competência nova (ex. 5/2026) num único fluxo deixa `ficha_mensal` > 0 e detalhe abre sem mensagem de ficha ausente
- [x] Falha simulada no processamento não deixa importação commitada isolada
- [x] Admin reprocessa competência pela tela Importação sem reupload ADP
- [ ] Verifier folha-custo-clt-fix1 PASS com evidência FIX1-01…13
