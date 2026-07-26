# Benefícios Mensais — Specification

## Problem Statement

O sistema trata benefícios como registros livres (texto em `descricao`) sem um catálogo formal de tipos, sem gravação por competência mensal e sem controle de acesso hierárquico (organograma). Isso impede totalização confiável por tipo, comparação entre meses e gera inconsistência com a planilha operacional usada pelo RH. A página `BeneficiosMensais` existe como placeholder vazio.

## Goals

- [x] Criar cadastro de **Tipo de Benefício** normalizado, compatível com os códigos da planilha operacional (SEGUROS, VALE_REFEICAO, etc.)
- [x] Implementar módulo **Benefícios Mensais** com gravação por competência (data_inicio/data_fim = primeiro/último dia do mês), espelhando a estrutura de `folha_pagamento`
- [x] Aplicar os mesmos direitos de acesso da Folha de Pagamento (organograma → centros de custo acessíveis)
- [x] Entregar tela frontend com resumo por competência + drill-down por funcionário, seguindo o layout da pasta de trabalho "Resumo" da planilha

## Out of Scope

| Feature | Reason |
| --- | --- |
| Motor de cálculo automático de benefícios | Valores vêm de importação ou input manual |
| Integração direta com operadoras de plano | Fase futura; input é planilha/CSV |
| Portal do colaborador (autoatendimento) | Fora do escopo do produto atual |
| Migração retroativa de `beneficios` antigos para o modelo mensal | Pode ser fase 2; agora coexistem |

---

## User Stories

### P1: Cadastro de Tipos de Benefício ⭐ MVP

**User Story**: Como gestor de RH, quero manter um cadastro de tipos de benefício com código e descrição padronizados, para que os lançamentos mensais estejam normalizados e totalizáveis.

**Why P1**: Sem o cadastro, não há como vincular lançamentos a tipos estruturados — base de toda a feature.

**Acceptance Criteria**:

1. WHEN o usuário acessa o cadastro de tipos THEN o sistema SHALL exibir lista paginada com colunas: Código, Descrição, Ativo
2. WHEN o usuário cria um tipo com código `VALE_REFEICAO` e descrição `Vale Refeição - Custo Empresa` THEN o sistema SHALL persistir o registro e retorná-lo com id gerado
3. WHEN o usuário tenta criar tipo com código já existente THEN o sistema SHALL retornar HTTP 409 com mensagem de conflito
4. WHEN o usuário edita a descrição de um tipo THEN o sistema SHALL atualizar sem alterar o código
5. WHEN o usuário desativa um tipo THEN o sistema SHALL fazer soft-delete (ativo=false) e o tipo não aparece mais no dropdown de lançamentos

**Independent Test**: CRUD via Swagger — criar, listar, editar, desativar um tipo e confirmar via GET.

**Tipos iniciais (seed da planilha)**:

| Código | Descrição |
| --- | --- |
| SEGUROS | Seguros - Custo Empresa |
| VALE_REFEICAO | Vale Refeição - Custo Empresa |
| VALE_TRANSPORTE | Vale Transporte - Custo Empresa |
| VALE_ALIMENTACAO | Vale Alimentação - Custo Empresa |
| AM_UNIMED_CE | Assistência Médica - Unimed CE - Custo Empresa |
| AM_GNDI_INTERMEDICA | Assistência Médica - GNDI Intermédica - Custo Empresa |
| AM_OMINT | Assistência Médica - Omint - Custo Empresa |
| AM_UNIMED_SALVADOR | Assistência Médica - Unimed Salvador - Custo Empresa |

---

### P1: Lançamento Mensal de Benefícios (por competência) ⭐ MVP

**User Story**: Como operador de RH, quero registrar benefícios mensais por funcionário vinculados a uma competência (mês/ano), para que os custos sejam rastreáveis mês a mês, da mesma forma que a folha de pagamento.

**Why P1**: Core da feature — sem isso, não há dado mensal para totalizar nem exibir.

**Acceptance Criteria**:

1. WHEN o operador importa a planilha de benefícios (aba Lançamentos: CPF, Nome, Descrição, Código, Valor) para competência 10/2024 THEN o sistema SHALL criar registros em `beneficio_mensal` com `competencia_inicio=2024-10-01`, `competencia_fim=2024-10-31` e `tipo_beneficio_id` resolvido pelo código
2. WHEN já existem lançamentos para a mesma competência e o operador reimporta THEN o sistema SHALL alertar duplicidade (HTTP 409) e pedir confirmação antes de substituir
3. WHEN o operador cria lançamento manual (API POST) informando `funcionarioId`, `tipoBeneficioId`, `valor`, `competenciaInicio`, `competenciaFim` THEN o sistema SHALL persistir o registro
4. WHEN CPF informado não corresponde a funcionário ativo THEN o sistema SHALL rejeitar a linha com mensagem indicando CPF inválido
5. WHEN o operador consulta lançamentos de uma competência THEN o sistema SHALL retornar apenas registros do período filtrado, aplicando filtro de organograma (centros de custo acessíveis ao usuário logado)

**Independent Test**: Importar planilha com 3 funcionários e 2 tipos → verificar registros criados → consultar por competência → reimportar e confirmar substituição.

---

### P1: Resumo de Benefícios Mensais (tela frontend) ⭐ MVP

**User Story**: Como gestor de RH, quero visualizar um resumo dos benefícios mensais agrupado por tipo (com total de valor e quantidade de lançamentos), filtrável por competência, semelhante à aba "Resumo" da planilha e à tela de Folha de Pagamento.

**Why P1**: Interface principal do módulo; sem ela o dado é inacessível ao usuário final.

**Acceptance Criteria**:

1. WHEN o usuário acessa a página "Benefícios Mensais" THEN o sistema SHALL exibir seletor de competência (mês/ano) com a última competência disponível pré-selecionada
2. WHEN uma competência é selecionada THEN o sistema SHALL exibir tabela-resumo com colunas: Código, Descrição, Total (R$), Qtd. Lançamentos — uma linha por tipo de benefício com lançamentos naquele mês
3. WHEN o usuário clica em uma linha do resumo THEN o sistema SHALL expandir/drill-down mostrando os funcionários com seus valores individuais para aquele tipo/competência
4. WHEN o usuário não tem acesso a todos os centros de custo THEN o sistema SHALL exibir apenas dados dos centros que ele pode visualizar (mesma regra da folha)
5. WHEN não existem lançamentos para a competência selecionada THEN o sistema SHALL exibir estado vazio com mensagem orientativa

**Independent Test**: Importar dados de 2 competências → navegar entre elas → verificar totais → logar com usuário restrito e confirmar filtro de acesso.

---

### P2: Importação de Planilha Excel (.xlsx)

**User Story**: Como operador de RH, quero importar a planilha de benefícios no formato Excel (mesma estrutura de `planilha_beneficios_custo_empresa.xlsx`) diretamente pela interface, para não precisar converter para CSV.

**Why P2**: Conveniência operacional; o formato Excel é o padrão de trabalho do RH.

**Acceptance Criteria**:

1. WHEN o operador faz upload de arquivo `.xlsx` com aba "Lancamentos" contendo colunas CPF, Nome, Descrição, Código, Valor THEN o sistema SHALL parsear e importar seguindo a mesma lógica do lançamento por competência
2. WHEN o arquivo não contém a aba "Lancamentos" ou as colunas esperadas THEN o sistema SHALL retornar erro descritivo
3. WHEN o upload é concluído com sucesso THEN o sistema SHALL exibir resumo da importação: total de linhas processadas, linhas com erro, valores totalizados

**Independent Test**: Upload via tela → importar planilha com 5 lançamentos → verificar resumo e dados persistidos.

---

### P2: Totalização "Custo Techne" incluindo benefícios mensais

**User Story**: Como gestor financeiro, quero que o endpoint de totais por funcionário (`/folha-pagamento/totais-funcionarios`) some os benefícios mensais da mesma competência ao campo "Custo Benefícios", para ter visão consolidada real.

**Why P2**: Atualmente `FolhaTotalizacaoService` busca da tabela `beneficios` (vigência); deve passar a buscar de `beneficio_mensal` quando existirem dados mensais para a competência.

**Acceptance Criteria**:

1. WHEN existem lançamentos em `beneficio_mensal` para a competência consultada THEN o sistema SHALL usar esses valores para compor "Custo Benefícios" no endpoint de totais
2. WHEN não existem lançamentos mensais mas existem benefícios legados (tabela `beneficios`) ativos no período THEN o sistema SHALL manter o fallback para a soma dos benefícios ativos (comportamento atual)
3. WHEN ambos existem THEN o sistema SHALL priorizar `beneficio_mensal` (evitar dupla contagem)

**Independent Test**: Inserir dados em ambas tabelas → chamar endpoint → confirmar que só `beneficio_mensal` é somado quando presente.

---

### P3: Menu de navegação e rota

**User Story**: Como usuário do sistema, quero acessar "Benefícios Mensais" pelo menu lateral, com ícone e posicionamento consistente com os demais módulos.

**Why P3**: UX; a funcionalidade existe via URL direta mas precisa ser descobrível.

**Acceptance Criteria**:

1. WHEN o usuário está logado THEN o sistema SHALL exibir item "Benefícios Mensais" no menu lateral, agrupado próximo de "Folha de Pagamento"
2. WHEN o usuário clica no item THEN o sistema SHALL navegar para `/beneficios-mensais`

**Independent Test**: Login → verificar menu → clicar → confirmar navegação.

---

## Edge Cases

- WHEN planilha contém código de tipo inexistente no cadastro THEN sistema SHALL rejeitar a linha indicando o código inválido (não criar tipo automaticamente)
- WHEN planilha contém linhas em branco (CPF vazio) THEN sistema SHALL ignorá-las silenciosamente
- WHEN valor do benefício é zero ou negativo THEN sistema SHALL aceitar zero (pode ser isenção) mas rejeitar negativo
- WHEN a competência informada tem formato inválido THEN sistema SHALL retornar HTTP 400 com mensagem clara
- WHEN usuário sem vínculo no organograma consulta benefícios THEN sistema SHALL retornar todos (acesso irrestrito, mesma regra da folha)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| BENM-01 | P1: Cadastro Tipos | Execute | Verified |
| BENM-02 | P1: Lançamento Mensal | Execute | Verified |
| BENM-03 | P1: Resumo Frontend | Execute | Verified |
| BENM-04 | P2: Importação Excel | Execute | Verified |
| BENM-05 | P2: Totalização Custo Techne | Execute | Verified |
| BENM-06 | P3: Menu/Rota | Execute | Verified |

**ID format:** `BENM-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 6 total, 6 mapped to tasks, 0 unmapped ✓

---

## Success Criteria

- [x] Operador de RH consegue importar planilha de benefícios e visualizar resumo por competência em < 2 minutos
- [x] Totais por tipo batem com a aba "Resumo" da planilha original
- [x] Usuários com acesso restrito só visualizam dados de seus centros de custo
- [x] Endpoint de totais por funcionário reflete benefícios mensais quando disponíveis
- [x] Tipos de benefício seed correspondem 1:1 aos códigos da planilha operacional
