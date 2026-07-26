# Ajuste Benefícios — Reorganização (Remover legado + Cadastro + Importação)

## Problem Statement

Atualmente existem **3 superfícies de benefícios** no sistema, causando confusão e redundância:

1. **`/beneficios`** — Página legada com mock data (stub não funcional), `BeneficioController` read-only com model de vigência
2. **`/beneficios-mensais`** — Módulo novo com lançamentos por competência, já com `ImportacaoDialog.tsx` próprio
3. **`/importacao`** — Página centralizada de importação que já importa benefícios CSV (legado) e folha ADP

O cadastro de "Tipos de Benefício" (CRUD) não está acessível como tela independente — está misturado no fluxo de `BeneficiosMensais`. Isso não segue o padrão dos outros cadastros (Cargos, Centros de Custo, Rubricas) que ficam na seção "Cadastros" do menu.

## Goals

- [x] Remover completamente o módulo "Benefícios" legado (frontend page, backend controller/service/entity/repository)
- [x] Criar tela CRUD de "Tipos de Benefício" na seção **Cadastros** do menu lateral (mesmo padrão de Cargos, Rubricas, etc.)
- [x] Mover a importação de benefícios mensais (.xlsx) para a **tela de Importação** existente (`/importacao`), substituindo a importação CSV legada
- [x] Remover o `ImportacaoDialog.tsx` da página `BeneficiosMensais` (importação fica centralizada em `/importacao`)

## Out of Scope

| Feature | Reason |
| --- | --- |
| Migrar dados da tabela `beneficios` para `beneficio_mensal` | Decisão anterior: coexistência; dados legados ficam inativos |
| Alterar lógica de acesso/organograma de BeneficiosMensais | Já especificado e implementado na feature anterior |
| Novo layout da página Importação | Só adicionar card de benefícios mensais; manter layout existente |

---

## User Stories

### P1: Remover módulo "Benefícios" legado ⭐ MVP

**User Story**: Como desenvolvedor/gestor do produto, quero eliminar a página e backend de "Benefícios" legado que nunca foi funcional (mock data), para evitar confusão com o módulo real "Benefícios Mensais".

**Why P1**: A presença simultânea de `/beneficios` e `/beneficios-mensais` no menu confunde o usuário. O legado não tem funcionalidade real.

**Acceptance Criteria**:

1. WHEN o sistema é acessado THEN NÃO SHALL existir item "Benefícios" no menu lateral (apenas "Benefícios Mensais" permanece)
2. WHEN a rota `/beneficios` é acessada diretamente THEN o sistema SHALL redirecionar para `/beneficios-mensais` (ou 404)
3. WHEN o backend é iniciado THEN NÃO SHALL existir endpoints em `/beneficios` (controller removido)
4. WHEN o código é compilado THEN NÃO SHALL existir referências a `BeneficioController`, `BeneficioService`, `BeneficioDTO` (exceto entidade `Beneficio` que persiste para `FolhaTotalizacaoService` fallback)

**Independent Test**: Verificar menu lateral — apenas "Benefícios Mensais" visível. Chamar `/api/beneficios/funcionario/1?data=2024-01-01` → 404.

**Nota**: A **entidade `Beneficio.java`** e o **`BeneficioRepository`** permanecem no código pois `FolhaTotalizacaoService` ainda os usa como fallback quando não há dados em `beneficio_mensal`. Apenas a camada de exposição (controller, service, DTO, frontend page) é removida.

---

### P1: Tela de Cadastro "Tipos de Benefício" na seção Cadastros ⭐ MVP

**User Story**: Como gestor de RH (admin), quero acessar o cadastro de Tipos de Benefício pela seção "Cadastros" no menu lateral, no mesmo padrão de Cargos, Centros de Custo e Rubricas.

**Why P1**: Consistência UX — todos os cadastros mestres estão nessa seção; tipos de benefício é um cadastro mestre.

**Acceptance Criteria**:

1. WHEN o usuário expande a seção "Cadastros" no menu THEN o sistema SHALL exibir "Tipos de Benefício" como item do grupo (ao lado de Cargos, Centros de Custo, etc.)
2. WHEN o usuário clica em "Tipos de Benefício" THEN o sistema SHALL navegar para `/tipos-beneficio`
3. WHEN o admin acessa a tela THEN o sistema SHALL exibir lista com colunas: Código, Descrição, Ativo — e botões Novo/Editar/Desativar
4. WHEN um operador não-admin acessa a tela THEN o sistema SHALL exibir a lista em modo leitura (sem botões de escrita) OU bloquear acesso conforme decisão de role
5. WHEN o admin cria/edita/desativa um tipo THEN o sistema SHALL refletir imediatamente na lista e nos dropdowns da importação

**Independent Test**: Login admin → Cadastros → Tipos de Benefício → criar tipo "ODONTO" → verificar que aparece na lista → desativar → verificar que sumiu da lista de ativos.

---

### P1: Mover importação de benefícios para tela Importação ⭐ MVP

**User Story**: Como operador de RH, quero importar a planilha de benefícios mensais (.xlsx) pela mesma tela onde importo folha ADP, para ter um ponto único de entrada de dados no sistema.

**Why P1**: Centralizar importação. A tela `/importacao` já é o lugar natural e o usuário já conhece o fluxo.

**Acceptance Criteria**:

1. WHEN o operador acessa `/importacao` THEN o sistema SHALL exibir 3 cards: "Importação de Folha ADP" (existente), "Importação de Benefícios Mensais" (novo, substituindo o card CSV legado)
2. WHEN o card "Importação de Benefícios Mensais" é usado THEN o sistema SHALL solicitar: arquivo .xlsx + competência (mês/ano seletores) + botão "Importar"
3. WHEN o upload é realizado com sucesso THEN o sistema SHALL exibir resultado no card de "Status da Importação" (mesmo padrão da folha ADP)
4. WHEN a competência já tem dados importados THEN o sistema SHALL exibir dialog de confirmação de substituição (mesmo padrão 409 da folha ADP)
5. WHEN a página `BeneficiosMensais` é acessada THEN NÃO SHALL existir botão/dialog de importação (importação é exclusiva de `/importacao`)

**Independent Test**: Ir em `/importacao` → selecionar .xlsx + competência 10/2024 → importar → verificar dados em `/beneficios-mensais` com competência 10/2024.

---

### P2: Remover importação CSV legada de benefícios

**User Story**: Como gestor do produto, quero que a importação CSV de benefícios (`importacaoService.importarBeneficios`) seja removida, já que foi substituída pela importação .xlsx por competência.

**Why P2**: Evitar dois caminhos de entrada para o mesmo dado. O CSV não grava por competência.

**Acceptance Criteria**:

1. WHEN o operador acessa `/importacao` THEN NÃO SHALL existir card de "Importação de Benefícios" (CSV)
2. WHEN o backend recebe POST em `/importacao/beneficios` (endpoint CSV legado) THEN o sistema SHALL retornar 404 ou 410 Gone
3. WHEN o código é compilado THEN NÃO SHALL existir `ImportacaoBeneficioService` (o CSV parser antigo)

**Independent Test**: Tentar acessar endpoint legado → 404. Verificar que `/importacao` mostra apenas "Folha ADP" + "Benefícios Mensais".

---

## Edge Cases

- WHEN o sistema tem dados em `beneficios` (legado) e `beneficio_mensal` THEN `FolhaTotalizacaoService` SHALL priorizar `beneficio_mensal` (comportamento já especificado, não muda)
- WHEN usuário digita `/beneficios` na URL THEN sistema SHALL redirecionar para `/beneficios-mensais` (graceful migration)
- WHEN existem referências em outros serviços para `BeneficioService` THEN a remoção SHALL ser safe (verificar dependências antes de deletar)
- WHEN a tela Importação é renderizada THEN o seletor de competência (mês/ano) do card de Benefícios Mensais SHALL ter como default o mês corrente

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| AJBEN-01 | P1: Remover legado | Execute | Complete |
| AJBEN-02 | P1: Tela Cadastro Tipos | Execute | Complete |
| AJBEN-03 | P1: Mover importação para /importacao | Execute | Complete |
| AJBEN-04 | P2: Remover importação CSV | Execute | Complete |

**ID format:** `AJBEN-[NUMBER]`

**Coverage:** 4 total, 4 mapped to tasks, 0 unmapped

---

## Success Criteria

- [x] Menu lateral tem apenas "Benefícios Mensais" (sem "Benefícios" legado)
- [x] "Tipos de Benefício" aparece na seção "Cadastros" com CRUD funcional
- [x] Importação de benefícios mensais (.xlsx) funciona pela tela `/importacao`
- [x] Página `BeneficiosMensais` não tem mais botão/dialog de importação
- [x] Nenhum endpoint de `/beneficios` (legado) responde no backend
- [x] Compilação frontend e backend sem erros após remoções
