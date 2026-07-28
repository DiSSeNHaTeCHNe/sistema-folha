# Ajustes — Listagens, Filtros e UX

## Problem Statement

Quatro lacunas de UX afetam o uso diário do sistema: (1) o **detalhamento de rubricas por funcionário** na Folha exibe linhas em ordem arbitrária; (2) a tela **Rubricas** não possui filtros e a listagem não está ordenada por identificador; (3) a tela **Usuários** já exibe filtros visuais, mas o backend ignora os parâmetros enviados e a listagem não segue ordem alfabética; (4) o dialog **Alterar senha** (menu AccountCircle) não permite visualizar **Nova senha** e **Confirmar nova senha** enquanto digita, dificultando conferência da credencial.

## Goals

- [x] Ordenar rubricas no dialog de detalhe do funcionário (Folha) por identificador da rubrica
- [x] Ordenar e filtrar a listagem de Rubricas (id/código, descrição, status Ativo/Inativo/Todos)
- [x] Ordenar Usuários alfabeticamente e garantir que os filtros existentes funcionem corretamente
- [x] Reutilizar o padrão visual de filtros já adotado em Funcionários e Usuários
- [x] Adicionar toggle de visibilidade (ícone olho) em **Nova senha** e **Confirmar nova senha** no dialog Alterar senha do menu do usuário

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Ordenação clicável por coluna (sort header) | Escopo limitado a ordem fixa definida nesta entrega |
| Paginação server-side nas listagens | Listagens atuais são full-load; sem mudança de contrato |
| Novos campos de filtro em Usuários além de nome, login e funcionário | Escopo = verificar/corrigir filtros já existentes na UI |
| Filtros na Folha além do dialog de rubricas do funcionário | Apenas detalhamento por funcionário |
| Inclusão de rubricas inativas no detalhe da Folha | Detalhe continua mostrando linhas de folha importadas (rubricas referenciadas), independente do status cadastral |
| Toggle de visibilidade na senha atual do dialog Alterar senha | Requisito explícito: olhinho **somente** em nova senha e confirmação |
| Alterar fluxo de troca de senha na tela Usuários (admin) | Escopo limitado ao dialog do ícone AccountCircle (`AlterarSenhaDialog`) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| "Id da rubrica" = campo **`codigo`** | Ordenação e filtro usam `codigo` / `rubricaCodigo` | Coluna exibida na tela Rubricas é "Código"; é o identificador de negócio na folha/ADP; PK numérica não aparece na UI | n |
| Ordenação de `codigo` | Crescente, **lexicográfica** (`localeCompare` pt-BR) | Simples e previsível; códigos numéricos com zero-padding (ex.: `001`) já ordenam corretamente | n |
| Filtro por id/código | **Contém** (substring), case-insensitive | Paridade com filtro de nome em Funcionários (`%valor%`) | n |
| Filtro por descrição | **Contém** (substring), case-insensitive | Mesmo padrão de busca textual do projeto | n |
| Status Rubricas — padrão | **Ativo** (equivalente ao comportamento atual) | `RubricaService.listarTodas()` hoje retorna só ativos | n |
| Ordenação Usuários | **`nome` ascendente** (pt-BR); desempate por **`login`** ascendente | Requisito explícito de ordem alfabética | n |
| Filtros Usuários — correspondência parcial | `nome` e `login`: contains case-insensitive; `funcionarioId`: match exato | Alinhar implementação backend ao que a UI já envia e ao padrão de Funcionários | n |
| Onde aplicar ordenação Folha | **Backend** em `FolhaPagamentoService.consultarPorFuncionario` + garantia no FE ao renderizar | Ordem estável independente de origem dos dados; FE pode reordenar defensivamente | n |
| Padrão visual de filtros | Card MUI com título **"Filtros"**, campos em `flexWrap`, botões **Filtrar** (contained) e **Limpar** (outlined) | Referência: `Funcionarios/index.tsx` e `Usuarios/index.tsx` | n |
| Toggle senha — padrão visual | `IconButton` + `Visibility` / `VisibilityOff` em `InputAdornment` no `TextField` | Mesmo padrão já usado em `Usuarios/index.tsx` (dialog de cadastro) | y |
| Toggle senha — estado por campo | **Nova senha** e **Confirmar nova senha** com toggles **independentes** | Usuário pode revelar um campo sem afetar o outro | y |
| Toggle senha — senha atual | Campo permanece sempre `type="password"` **sem** botão olho | Decisão explícita do usuário | y |

**Open questions:** none — all resolved or logged above (pending user confirmation on assumptions marked `n`).

---

## User Stories

### P1: Detalhe de rubricas na Folha ordenado por id ⭐ MVP

**User Story**: As a **operador de folha**, I want **ver as rubricas de um funcionário ordenadas pelo id/código da rubrica** so that **localize proventos e descontos na sequência esperada do holerite**.

**Why P1**: Consulta diária de holerite; ordem arbitrária dificulta conferência linha a linha.

**Acceptance Criteria**:

1. WHEN usuário abre o dialog **Ver Rubricas** de um funcionário THEN sistema SHALL exibir as linhas ordenadas por `rubricaCodigo` **crescente** (lexicográfica pt-BR)
2. WHEN duas linhas possuem o mesmo `rubricaCodigo` THEN sistema SHALL manter ordem estável desempatando por `id` da linha de folha crescente
3. WHEN não há rubricas no período THEN dialog SHALL exibir tabela vazia (sem erro)

**Independent Test**: Abrir Folha → competência → funcionário → Ver Rubricas → confirmar que códigos aparecem em ordem crescente (ex.: `100`, `200`, `301` ou `001`, `002`).

---

### P1: Rubricas — ordenação e filtros ⭐ MVP

**User Story**: As a **administrador de cadastros**, I want **listar rubricas ordenadas por id/código e filtrar por código, descrição e status** so that **encontre e mantenha rubricas ativas/inativas com eficiência**.

**Why P1**: Tela de cadastro sem filtros escala mal; hoje só lista ativos sem critério de ordem.

**Acceptance Criteria**:

1. WHEN usuário abre `/rubricas` THEN tela SHALL exibir card **Filtros** no padrão visual de Funcionários/Usuários (Card + título + campos + Filtrar/Limpar)
2. WHEN listagem é carregada sem filtros aplicados THEN rubricas SHALL aparecer ordenadas por `codigo` crescente (lexicográfica pt-BR)
3. WHEN usuário informa filtro **Id/Código** e clica **Filtrar** THEN sistema SHALL retornar rubricas cujo `codigo` **contém** o valor informado (case-insensitive)
4. WHEN usuário informa filtro **Descrição** e clica **Filtrar** THEN sistema SHALL retornar rubricas cuja `descricao` **contém** o valor informado (case-insensitive)
5. WHEN usuário seleciona status **Ativo** THEN sistema SHALL listar apenas rubricas com `ativo = true`
6. WHEN usuário seleciona status **Inativo** THEN sistema SHALL listar apenas rubricas com `ativo = false`
7. WHEN usuário seleciona status **Todos** THEN sistema SHALL listar rubricas ativas e inativas
8. WHEN status não é informado na carga inicial THEN padrão SHALL ser **Ativo** (paridade com comportamento atual)
9. WHEN usuário clica **Limpar** THEN campos de filtro SHALL resetar para padrão (status **Ativo**, demais vazios) e SHALL recarregar listagem
10. WHEN filtros combinados não retornam resultados THEN tabela SHALL exibir mensagem **"Nenhuma rubrica encontrada"**
11. WHEN filtros são aplicados THEN backend endpoint `GET /rubricas` SHALL aceitar query params `codigo`, `descricao` e `status` (`ATIVO` \| `INATIVO` \| `TODOS`)

**Independent Test**: Cadastrar rubricas ativas/inativas com códigos distintos → filtrar por código parcial → filtrar por descrição → alternar status → limpar → verificar ordem por código.

---

### P1: Usuários — ordem alfabética e filtros funcionais ⭐ MVP

**User Story**: As a **administrador**, I want **ver usuários em ordem alfabética e filtrar por nome, login e funcionário vinculado** so that **gerencie contas de acesso de forma previsível**.

**Why P1**: UI já promete filtros que o backend não aplica (`GET /usuarios` ignora query params); ordem atual é indefinida.

**Acceptance Criteria**:

1. WHEN usuário abre `/usuarios` ou aplica filtros THEN listagem SHALL estar ordenada por **`nome` ascendente** (pt-BR); desempate por **`login` ascendente**
2. WHEN usuário preenche filtro **Nome** e clica **Filtrar** THEN backend SHALL retornar usuários ativos cujo `nome` **contém** o valor (case-insensitive)
3. WHEN usuário preenche filtro **Login** e clica **Filtrar** THEN backend SHALL retornar usuários ativos cujo `login` **contém** o valor (case-insensitive)
4. WHEN usuário seleciona **Funcionário** e clica **Filtrar** THEN backend SHALL retornar apenas usuários vinculados ao `funcionarioId` selecionado
5. WHEN filtros são combinados THEN backend SHALL aplicar **AND** entre critérios informados (não vazios)
6. WHEN usuário clica **Limpar** THEN filtros SHALL resetar e listagem SHALL recarregar todos os usuários ativos em ordem alfabética
7. WHEN filtros não retornam resultados THEN tabela SHALL exibir **"Nenhum usuário encontrado"**
8. WHEN backend recebe `GET /usuarios` THEN SHALL aceitar query params opcionais `nome`, `login`, `funcionarioId` conforme já enviado pelo `usuarioService.listar`

**Independent Test**: Criar usuários com nomes distintos → confirmar ordem A–Z → filtrar por nome parcial → filtrar por login → filtrar por funcionário → limpar.

---

### P1: Dialog Alterar senha — visibilidade de nova senha ⭐ MVP

**User Story**: As a **usuário autenticado**, I want **revelar/ocultar nova senha e confirmação no dialog Alterar senha** so that **possa conferir o que digitei sem expor a senha atual**.

**Why P1**: Dialog já existe (`AlterarSenhaDialog`); ausência do olhinho nas novas senhas dificulta digitação correta; senha atual deve permanecer oculta por segurança de UX.

**Acceptance Criteria**:

1. WHEN usuário abre **Alterar senha** pelo menu AccountCircle THEN campos **Nova senha** e **Confirmar nova senha** SHALL exibir ícone de visibilidade (`Visibility` / `VisibilityOff`) no `InputAdornment` do campo, no mesmo padrão de `Usuarios/index.tsx`
2. WHEN usuário clica no ícone de **Nova senha** THEN campo SHALL alternar entre `type="password"` (oculto) e `type="text"` (visível); estado inicial SHALL ser oculto
3. WHEN usuário clica no ícone de **Confirmar nova senha** THEN campo SHALL alternar visibilidade **independentemente** do campo Nova senha; estado inicial SHALL ser oculto
4. WHEN dialog é exibido THEN campo **Senha atual** SHALL permanecer sempre oculto (`type="password"`) e SHALL **não** exibir ícone de visibilidade
5. WHEN dialog é fechado ou reaberto THEN toggles de visibilidade das novas senhas SHALL resetar para oculto
6. WHEN usuário submete o formulário THEN comportamento de validação e chamada API SHALL permanecer inalterado (mín. 6 caracteres, confirmação igual, senha atual obrigatória)

**Independent Test**: Logar → AccountCircle → Alterar senha → clicar olho em Nova senha → texto visível → clicar olho em Confirmar (independente) → confirmar que Senha atual não tem olho → fechar e reabrir → campos novamente ocultos.

---

## Edge Cases

- WHEN filtro de código/descrição/nome/login é apenas espaços THEN sistema SHALL tratar como critério vazio (ignorar)
- WHEN funcionário no filtro de Usuários é **Todos** (valor vazio) THEN sistema SHALL não filtrar por funcionário
- WHEN rubrica inativa existe mas linhas de folha a referenciam THEN detalhe da Folha SHALL continuar exibindo essas linhas (sem filtro por status cadastral)
- WHEN códigos de rubrica misturam formatos (`1`, `01`, `ABC`) THEN ordenação lexicográfica SHALL ser aplicada uniformemente (sem conversão numérica implícita)
- WHEN API de filtros falha THEN frontend SHALL exibir notificação de erro e manter última listagem válida ou estado vazio conforme padrão da tela
- WHEN usuário alterna visibilidade e submete o formulário THEN valores enviados SHALL ser os mesmos independentemente de estarem visíveis ou ocultos na tela
- WHEN dialog Alterar senha reabre após erro de senha atual incorreta THEN toggles de nova senha SHALL voltar a oculto

---

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | Query params opcionais; trim em strings; `funcionarioId` numérico; enum status Rubricas |
| Failure / partial-failure states | Toast/notification em erro de API; sem retry automático |
| Idempotency / retry | N/A — operações de consulta |
| Auth boundaries | Rotas `/rubricas` e `/usuarios` permanecem protegidas por guard **ADMIN** existente; Folha mantém ACL organograma; Alterar senha continua restrito ao próprio usuário logado |
| Concurrency / ordering | Ordenação determinística definida na spec (codigo/nome) |
| Data lifecycle / expiry | N/A |
| Observability | N/A para esta entrega |
| External-dependency failure | N/A |
| State-transition integrity | Limpar filtros restaura defaults; drill-down Folha inalterado |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| FOLHA-ORD-01 | P1: Detalhe Folha ordenado | Tasks (T1) | ✅ Verified |
| FOLHA-ORD-02 | P1: Detalhe Folha ordenado | Tasks (T1) | ✅ Verified |
| FOLHA-ORD-03 | P1: Detalhe Folha ordenado | Tasks (T1) | ⚠️ Verified (impl; no unit assert) |
| RUB-01 | P1: Rubricas filtros/ordem | Tasks (T4) | ✅ Verified |
| RUB-02 | P1: Rubricas filtros/ordem | Tasks (T2) | ⚠️ Verified (ORDER BY repo; no order assert) |
| RUB-03 | P1: Rubricas filtros/ordem | Tasks (T2,T4) | ✅ Verified |
| RUB-04 | P1: Rubricas filtros/ordem | Tasks (T2,T4) | ✅ Verified |
| RUB-05 | P1: Rubricas filtros/ordem | Tasks (T2) | ✅ Verified |
| RUB-06 | P1: Rubricas filtros/ordem | Tasks (T2) | ✅ Verified |
| RUB-07 | P1: Rubricas filtros/ordem | Tasks (T2) | ✅ Verified |
| RUB-08 | P1: Rubricas filtros/ordem | Tasks (T2,T4) | ✅ Verified |
| RUB-09 | P1: Rubricas filtros/ordem | Tasks (T4) | ✅ Verified |
| RUB-10 | P1: Rubricas filtros/ordem | Tasks (T4) | ✅ Verified |
| RUB-11 | P1: Rubricas filtros/ordem | Tasks (T2) | ✅ Verified |
| USR-01 | P1: Usuários ordem/filtros | Tasks (T3) | ⚠️ Verified (ORDER BY repo; no order assert) |
| USR-02 | P1: Usuários ordem/filtros | Tasks (T3) | ✅ Verified |
| USR-03 | P1: Usuários ordem/filtros | Tasks (T3) | ✅ Verified |
| USR-04 | P1: Usuários ordem/filtros | Tasks (T3) | ✅ Verified |
| USR-05 | P1: Usuários ordem/filtros | Tasks (T3) | ✅ Verified |
| USR-06 | P1: Usuários ordem/filtros | Tasks (T6) | ✅ Verified |
| USR-07 | P1: Usuários ordem/filtros | Tasks (T6) | ✅ Verified |
| USR-08 | P1: Usuários ordem/filtros | Tasks (T3) | ✅ Verified |
| SENHA-VIS-01 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |
| SENHA-VIS-02 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |
| SENHA-VIS-03 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |
| SENHA-VIS-04 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |
| SENHA-VIS-05 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |
| SENHA-VIS-06 | P1: Dialog senha — olhinho | Tasks (T5) | ✅ Verified |

**Coverage:** 28 total, 28 mapped to tasks ✅

---

## Success Criteria

- [x] Dialog de rubricas na Folha exibe linhas sempre em ordem crescente de código da rubrica
- [x] Tela Rubricas possui filtros visuais alinhados a Funcionários/Usuários e filtra/ordena conforme critérios
- [x] Tela Usuários lista em ordem alfabética por nome e filtros nome/login/funcionário funcionam via API
- [x] Testes automatizados cobrem ordenação Folha, filtros/ordem Rubricas e filtros/ordem Usuários
- [x] Dialog Alterar senha exibe olhinho em Nova senha e Confirmar nova senha; Senha atual permanece sem toggle
