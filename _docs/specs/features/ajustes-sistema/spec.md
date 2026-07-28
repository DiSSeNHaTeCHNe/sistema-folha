# Ajustes Sistema Specification

## Problem Statement

Três lacunas de UX e governança afetam o uso diário do sistema: (1) usuários não-administradores veem o menu **Cadastros** e podem acessar rotas de configuração que deveriam ser exclusivas de administradores; (2) não existe fluxo de **troca de senha** para o usuário logado — apenas admins alteram senha de terceiros na tela de Usuários; (3) **Benefícios Mensais** usa padrão visual diferente da Folha (resumo por tipo com linhas expansíveis), dificultando a navegação consistente entre os dois módulos de consulta.

## Goals

- [x] Restringir visibilidade e acesso ao menu **Cadastros** somente a usuários com permissão `ADMIN`
- [x] Permitir que qualquer usuário autenticado altere a própria senha pelo menu do ícone de conta (AccountCircle)
- [x] Reestruturar **Benefícios Mensais** com o mesmo fluxo drill-down da Folha: **Resumo (competências) → Lista Funcionários → Dados Benefícios (dialog)**

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| RBAC completo para todos os itens do menu principal | Escopo limitado ao menu Cadastros; demais permissões (`FOLHA_PAGAMENTO`, `CADASTROS`, etc.) permanecem sem filtro no sidebar |
| Admin resetar senha de outro usuário via novo fluxo | Já existe na tela Usuários; não duplicar |
| CRUD manual ou importação de benefícios | Fluxos existentes permanecem; apenas consulta/navegação muda |
| Nova entidade/tabela `resumo_beneficio_mensal` | Resumo por competência será agregado a partir de `beneficio_mensal` (como hoje), sem persistir snapshot |
| Troca de senha com e-mail / token de recuperação | Fora do escopo desta entrega |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| "ADM" = permissão `ADMIN` | Usar `user.permissoes.includes('ADMIN')` | Código existente usa `ADMIN`, não `ADM` nem `CADASTROS` | n |
| `ACESSO_TOTAL` não substitui `ADMIN` para Cadastros | Apenas `ADMIN` vê Cadastros | Usuário pediu "somente ADM"; `ACESSO_TOTAL` é bypass de organograma, não administração | n |
| Bloqueio de rota direta para não-ADMIN | Redirect para `/dashboard` + mensagem | Esconder menu sem guard de rota é insuficiente para segurança de UX | n |
| Troca de senha exige senha atual | Sim, obrigatória | Endpoint backend `alterarSenha` já valida senha atual; evita hijack de sessão | n |
| Regras de senha nova | Mínimo 6 caracteres + confirmação igual | Alinhado à validação existente em `Usuarios/index.tsx` | n |
| Alinhar contrato API senha | Corrigir frontend para `POST /usuarios/{id}/alterar-senha` com query params | Backend expõe POST + `@RequestParam`; frontend chama PUT inexistente | n |
| Colunas do resumo de Benefícios | Adaptadas ao domínio: Competência, Total Funcionários, Total Benefícios (R$), Qtd. Lançamentos, Ações | Folha tem colunas de provento/desconto/líquido inexistentes em benefícios; mesma **navegação**, colunas específicas | n |
| Filtros na lista de funcionários (Benefícios) | Mesmos de Folha: linha de negócio, centro de custo, busca textual | Paridade de funcionamento com Folha | n |
| Dialog de detalhe Benefícios | Tabela: código, descrição do tipo, valor, observação | Equivalente ao dialog "Ver Rubricas" da Folha | n |
| Endpoint novo para listar competências | `GET /beneficio-mensal/competencias?ano=&mes=` retornando lista agregada ACL-scoped | Folha usa `resumo_folha_pagamento`; benefícios não tem tabela de resumo — agregar por competência no repositório | n |

**Open questions:** none — all resolved or logged above (pending user confirmation on assumptions marked `n`).

---

## User Stories

### P1: Menu Cadastros visível apenas para ADMIN ⭐ MVP

**User Story**: As a **usuário sem permissão ADMIN**, I want **não ver o menu Cadastros** so that **não acesse acidentalmente telas de configuração do sistema**.

**Why P1**: Exposição indevida de cadastros sensíveis (usuários, organograma, importação) a perfis operacionais.

**Acceptance Criteria**:

1. WHEN usuário logado possui `ADMIN` em `permissoes` THEN sidebar SHALL exibir a seção colapsável **Cadastros** com todos os 8 sub-itens atuais (Usuários, Linhas de Negócio, Centros de Custo, Cargos, Rubricas, Tipos de Benefício, Organograma, Importação)
2. WHEN usuário logado **não** possui `ADMIN` THEN sidebar SHALL **não** renderizar a seção **Cadastros** (nem o divider imediatamente anterior)
3. WHEN usuário sem `ADMIN` navega diretamente para qualquer rota de cadastro (`/usuarios`, `/linhas-negocio`, `/centros-custo`, `/cargos`, `/rubricas`, `/tipos-beneficio`, `/organograma`, `/importacao`) THEN sistema SHALL redirecionar para `/dashboard` e SHALL exibir notificação informando acesso negado
4. WHEN usuário com `ADMIN` acessa rotas de cadastro THEN sistema SHALL permitir navegação normalmente

**Independent Test**: Logar como usuário `OPERADOR` → menu Cadastros ausente; tentar `/usuarios` → redirect dashboard. Logar como `ADMIN` → menu visível e rotas acessíveis.

---

### P1: Troca de senha no menu do usuário ⭐ MVP

**User Story**: As a **usuário autenticado**, I want **alterar minha senha pelo menu do ícone de conta** so that **possa manter minha credencial segura sem depender de um administrador**.

**Why P1**: Funcionalidade básica de autogestão ausente; API backend já existe mas sem UI conectada.

**Acceptance Criteria**:

1. WHEN usuário clica no ícone AccountCircle THEN menu SHALL exibir, além do nome e **Sair**, a opção **Alterar senha**
2. WHEN usuário seleciona **Alterar senha** THEN sistema SHALL abrir dialog/modal com campos: **Senha atual**, **Nova senha**, **Confirmar nova senha**
3. WHEN usuário submete com senha atual incorreta THEN sistema SHALL exibir mensagem de erro clara (ex.: "Senha atual incorreta") e SHALL manter o dialog aberto
4. WHEN nova senha tem menos de 6 caracteres OR confirmação difere da nova senha THEN sistema SHALL exibir erro de validação no campo correspondente e SHALL **não** chamar a API
5. WHEN submissão é válida THEN sistema SHALL chamar `POST /usuarios/{id}/alterar-senha?senhaAtual=&novaSenha=` para o **próprio** usuário logado (`user.id`) e, em sucesso, SHALL fechar o dialog e exibir confirmação de sucesso
6. WHEN API retorna erro (400/401/403) THEN sistema SHALL exibir mensagem amigável sem expor stack trace

**Independent Test**: Logar → AccountCircle → Alterar senha → preencher campos válidos → senha alterada; tentar login com nova senha → sucesso.

---

### P1: Benefícios Mensais com fluxo Resumo → Funcionários → Detalhe ⭐ MVP

**User Story**: As a **gestor ou operador de benefícios**, I want **navegar benefícios mensais no mesmo padrão da Folha de Pagamento** so that **consulte competências, funcionários e lançamentos com experiência consistente**.

**Why P1**: Requisito explícito de paridade UX; reduz curva de aprendizado entre módulos.

**Acceptance Criteria**:

1. WHEN usuário abre `/beneficios-mensais` THEN tela inicial SHALL ser **Resumos de Benefícios Mensais** — tabela de competências com filtros **Ano** (obrigatório, select) e **Mês** (opcional, numérico 1–12), botões Filtrar e Limpar — espelhando o padrão da tela de resumo da Folha
2. WHEN resumos são carregados THEN cada linha SHALL exibir: período de competência (`competenciaInicio` a `competenciaFim`), total de funcionários distintos, total em R$ dos benefícios, quantidade de lançamentos, e botão **Ver Funcionários**
3. WHEN usuário clica **Ver Funcionários** em uma competência THEN tela SHALL alternar para **Lista de Funcionários** com: botão **← Voltar**, título com competência selecionada, filtros (linha de negócio, centro de custo, busca por nome), grid de cards por funcionário mostrando nome, cargo, centro de custo, linha de negócio, total de benefícios (R$), e botão **Ver Benefícios**
4. WHEN usuário clica **Ver Benefícios** em um card THEN sistema SHALL abrir dialog listando os lançamentos daquele funcionário na competência: código do tipo, descrição, valor (R$), observação
5. WHEN usuário clica **← Voltar** na lista de funcionários THEN sistema SHALL retornar à tabela de resumos preservando filtros de ano/mês
6. WHEN não há dados para os filtros THEN sistema SHALL exibir estado vazio com mensagem orientativa (equivalente à Folha)
7. WHEN dados são consultados THEN backend SHALL aplicar **mesmo filtro ACL por organograma** já usado em `BeneficioMensalService.listarPorCompetenciaParaUsuario` — usuário só vê competências/funcionários/lançamentos dentro do seu escopo de acesso
8. WHEN competências são listadas THEN endpoint `GET /beneficio-mensal/competencias` SHALL agregar por par `(competenciaInicio, competenciaFim)` respeitando ACL, filtráveis por `ano` e opcionalmente `mes`

**Independent Test**: Importar ou usar dados seed → abrir Benefícios → filtrar ano → ver competências → Ver Funcionários → Ver Benefícios no dialog → Voltar.

---

## Edge Cases

- WHEN usuário perde permissão `ADMIN` em sessão ativa (edge improvável) THEN guard de rota SHALL bloquear na próxima navegação; menu SHALL refletir permissões do `user` em localStorage na montagem do Layout
- WHEN lista de resumos de benefícios está vazia para o ano THEN tabela SHALL mostrar mensagem "Nenhum benefício mensal encontrado" (não erro genérico)
- WHEN funcionário tem zero lançamentos após filtro client-side THEN grid SHALL mostrar mensagem "Nenhum funcionário encontrado para este período"
- WHEN senha nova igual à senha atual THEN backend pode aceitar ou rejeitar; frontend SHALL tratar resposta 400 com mensagem genérica se backend não distinguir
- WHEN usuário tenta alterar senha de outro usuário via API THEN backend SHALL continuar exigindo autenticação; **follow-up de segurança**: endpoint atual permite alterar qualquer `id` — fora do escopo desta spec, mas troca via UI usa sempre `user.id` do contexto

---

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | Senha: min 6 chars, confirmação; mês 1–12, ano via select |
| Failure / partial-failure states | Erros de API exibidos no dialog (senha) e na página (benefícios); redirect em rota negada |
| Idempotency / retry | N/A — operações de consulta; troca de senha não retenta automaticamente |
| Auth boundaries | Menu Cadastros: `ADMIN`; troca senha: próprio usuário; benefícios: ACL organograma existente |
| Concurrency / ordering | N/A |
| Data lifecycle / expiry | N/A |
| Observability | N/A para esta entrega |
| External-dependency failure | N/A |
| State-transition integrity | Drill-down benefícios: resumo ↔ funcionários via state local; voltar limpa seleção de competência parcialmente (preserva filtros ano/mês) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MENU-01 | P1: Menu Cadastros ADMIN | Execute (T5) | ✅ Verified |
| MENU-02 | P1: Menu Cadastros ADMIN | Execute (T5) | ✅ Verified |
| MENU-03 | P1: Menu Cadastros ADMIN | Execute (T5) | ✅ Verified |
| SENHA-01 | P1: Troca de senha | Execute (T4) | ✅ Verified |
| SENHA-02 | P1: Troca de senha | Execute (T4) | ✅ Verified |
| SENHA-03 | P1: Troca de senha | Execute (T4) | ✅ Verified |
| SENHA-04 | P1: Troca de senha | Execute (T4) | ✅ Verified |
| BEN-01 | P1: Benefícios drill-down | Execute (T6) | ✅ Verified |
| BEN-02 | P1: Benefícios drill-down | Execute (T6) | ✅ Verified |
| BEN-03 | P1: Benefícios drill-down | Execute (T3,T6) | ✅ Verified |
| BEN-04 | P1: Benefícios drill-down | Execute (T6) | ✅ Verified |
| BEN-05 | P1: Benefícios drill-down | Execute (T6) | ✅ Verified |
| BEN-06 | P1: Benefícios drill-down | Execute (T1-T3,T6) | ✅ Verified |
| BEN-07 | P1: Benefícios drill-down | Execute (T2) | ✅ Verified |

**Coverage:** 13 total, 13 mapped to tasks ✅

---

## Success Criteria

- [x] Usuário não-ADMIN não vê menu Cadastros e não acessa rotas de cadastro via URL
- [x] Usuário autenticado altera a própria senha pelo menu AccountCircle sem intervenção de admin
- [x] Tela Benefícios Mensais segue fluxo de 3 níveis idêntico em navegação à Folha de Pagamento
- [x] Testes automatizados cobrem guard de rota ADMIN (FE build gate), validação do form de senha (FE build gate), e agregação/listagem de competências de benefícios (backend — 21 unit + 2 WebMvc)
