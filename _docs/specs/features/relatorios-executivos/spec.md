# Relatórios Executivos — Specification

## Problem Statement

A tela **Relatórios** existe no frontend e promete PDFs de folha e benefícios, mas **não há backend** — todas as chamadas a `/relatorios/*` retornam 404 (`CONCERNS.md`, L-001). Gestores e RH continuam exportando planilhas paralelas (ex.: *Relatório Custo Benefício Folha*) porque o sistema não entrega documentos prontos para apresentação. O dashboard já calcula KPIs ricos (custo empresa, breakdown por CC/linha/cargo, top rubricas, evolução mensal), porém essa inteligência **não sai do navegador** em formato compartilhável.

O usuário quer relatórios que **encham os olhos** — visual premium, identidade Techne — **e** tragam informação acionável para fechamento mensal, gestão de custo e auditoria operacional.

## Goals

- [ ] Fechar o gap frontend/backend: gerar, listar e baixar relatórios PDF com status (`PENDENTE` → `PROCESSADO` / `ERRO`)
- [ ] Entregar **pelo menos dois relatórios executivos** no MVP com layout premium (capa, KPIs, gráficos, tabelas) usando dados reais de folha, benefícios e totalização
- [ ] Respeitar **ACL por centro de custo** em todo relatório (mesma regra de Folha/Dashboard)
- [ ] Permitir identidade visual Techne (logo, cores, tipografia) configurável sem redeploy de código

## Out of Scope

| Feature | Reason |
| --- | --- |
| Motor legal de folha (INSS, IRRF, GFIP, eSocial) | Fora do escopo do produto; ADP é fonte |
| Holerite self-service / portal do colaborador | Deferred em `PROJECT.md` |
| Multi-empresa / multi-tenant | Fora de escopo |
| Relatórios agendados por e-mail | P3 desta feature; fase futura se não couber no MVP |
| Editor drag-and-drop de layout | Complexidade desproporcional; templates fixos premium |
| Relatórios personalizados por usuário (filtros salvos) | P3; não bloqueia MVP |
| Interface contábil (GL) / export SAP | Integração futura |
| Substituição do dashboard interativo | Relatórios complementam, não substituem Recharts na tela |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Formato principal = **PDF**; CSV como complemento P2 | PDF no MVP; CSV na story P2 de export tabular | UI existente já espera blob PDF; CSV é mais rápido para auditoria | n |
| Geração **assíncrona** com persistência de metadados | Job grava registro `PENDENTE`, processa, atualiza `PROCESSADO`/`ERRO` | Contrato já modelado em `relatorioService.ts`; evita timeout HTTP em competências grandes | n |
| Biblioteca PDF no **backend** (novo domínio `relatorios`) | Domínio `relatorios.{api,application,infrastructure}`; comunicação via ports Folha/Benefícios/Dashboard | ACL server-side; arquivo único para download; alinha AD-008 | n |
| Logo Techne em `backend/src/main/resources/branding/logo.png` (PNG, fundo transparente, ≥ 300 px largura) | Placeholder textual "TECHNE" até o usuário fornecer asset | Usuário autorizou pedir logo; placeholder permite desenvolvimento paralelo | n |
| Paleta visual | Primária `#1976d2`, secundária `#dc004e`, neutros `#1e293b` / `#f8fafc` — alinhada ao MUI atual + tom executivo | Consistência com app; usuário pode ajustar via config | n |
| Competência padrão ao gerar | Mês/ano corrente; seletor na UI substitui default | Comportamento atual da tela Relatórios | n |
| Quem pode gerar | Qualquer usuário autenticado com acesso à competência (ACL); download só do próprio escopo | Mesma política de consulta folha | n |
| Retenção de PDFs gerados | 12 meses rolling; purge job P2 | Evita crescimento ilimitado de blob storage | n |
| Gráficos no PDF | Representação estática (barras/pizza simplificadas ou sparklines) derivada dos mesmos agregados do Dashboard | "Encher os olhos" sem exigir motor de chart interativo no PDF | n |
| Relatório folha usa **ficha processada** quando existir; fallback ADP bruto | Prioriza `ficha_mensal` pós-processamento; senão `folha_pagamento` ativa | Alinha totalização AD-012 (custo empresa) | n |
| Idioma e locale | pt-BR; moeda BRL `R$`; datas `dd/MM/yyyy` | Padrão operacional Techne | n |

**Assets solicitados ao usuário (bloqueiam polish final, não o MVP funcional):**

1. **Logo principal** — PNG ou SVG, fundo transparente, versão colorida para capa e monocromática opcional para rodapé
2. **Logo secundário / ícone** (opcional) — favicon ou marca reduzida para cabeçalho de páginas internas
3. **Confirmação de cores institucionais** — se diferentes de `#1976d2` / `#dc004e`

**Open questions:** none blocking spec — assets de marca registrados como dependência externa acima.

---

## Implicit-Requirement Dimensions (Large feature)

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | `mes` 1–12, `ano` 2000–2100; rejeitar competência futura; competência sem dados → relatório com estado vazio explícito (não erro) |
| Failure / partial-failure states | Job `ERRO` persiste mensagem truncada (≤500 chars); download bloqueado se status ≠ `PROCESSADO` |
| Idempotency / retry / duplicate handling | Uma geração ativa por `(tipo, mes, ano, usuarioId)`; re-gerar substitui registro anterior do mesmo usuário/tipo/competência |
| Auth boundaries & rate limits | JWT/API Key autenticados; dados filtrados por `OrganogramaAcessoPort`; max 3 gerações simultâneas por usuário |
| Concurrency / ordering | Geração serializada por `(tipo, mes, ano)` globalmente (lock) para evitar duplicata de processamento pesado |
| Data lifecycle / expiry | Metadados + blob PDF; purge 12 meses (P2); soft-delete de registros |
| Observability | Log estruturado `relatorios` domain: início/fim/erro de geração com `relatorioId`, `login`, competência |
| External-dependency failure | N/A — sem deps externas além de PostgreSQL |
| State-transition integrity | `PENDENTE` → `PROCESSADO` \| `ERRO`; sem regressão de status |

---

## User Stories

### P1: Backend de relatórios (API mínima funcional) ⭐ MVP

**User Story**: Como operador de RH, quero gerar, listar e baixar relatórios PDF pela tela existente, para que a funcionalidade prometida no menu deixe de retornar erro.

**Why P1**: Fecha L-001; desbloqueia toda a feature; vertical slice mínimo.

**Acceptance Criteria**:

1. WHEN o usuário autenticado envia `POST /api/relatorios/folha` com `{ mes, ano }` válidos THEN o sistema SHALL criar registro com `status=PENDENTE`, processar assincronamente e retornar DTO com `id`, totais agregados e `status` final (`PROCESSADO` ou `ERRO`) em até 60 s para competência com ≤ 500 funcionários no escopo do usuário
2. WHEN o usuário envia `POST /api/relatorios/beneficio` com `{ mes, ano }` válidos THEN o sistema SHALL seguir o mesmo fluxo de geração assíncrona para relatório de benefícios
3. WHEN o usuário chama `GET /api/relatorios/folha` ou `GET /api/relatorios/beneficio` THEN o sistema SHALL retornar lista ordenada por `ano DESC, mes DESC` contendo apenas relatórios gerados por usuários do mesmo tenant (single-tenant) — visíveis a todos autenticados com ACL na competência
4. WHEN o usuário chama `GET /api/relatorios/folha/{id}/download` e `status=PROCESSADO` THEN o sistema SHALL retornar `Content-Type: application/pdf` com blob persistido
5. WHEN `status` é `PENDENTE` ou `ERRO` THEN download SHALL retornar HTTP 409 com mensagem indicando indisponibilidade
6. WHEN `mes` ou `ano` estão fora dos limites THEN o sistema SHALL retornar HTTP 400 (Bean Validation)

**Independent Test**: Postman — gerar folha mês corrente → listar → download PDF → verificar `%PDF` no magic bytes.

---

### P1: Relatório Executivo de Folha (PDF premium) ⭐ MVP

**User Story**: Como gestor ou diretor, quero um PDF executivo da competência com KPIs, gráficos e breakdowns, para apresentar em reuniões sem montar planilha manual.

**Why P1**: Entrega o "encherga os olhos" + utilidade gerencial; reutiliza agregados do Dashboard.

**Acceptance Criteria**:

1. WHEN o PDF de folha é gerado para competência com dados THEN a **página 1 (capa)** SHALL conter: logo (ou placeholder), título "Relatório Executivo de Folha", competência `MM/yyyy`, data/hora de geração, nome do usuário gerador, e **4 KPI cards** — Total Funcionários, Custo Empresa (R$), Total Proventos (R$), Total Descontos (R$)
2. WHEN a capa é renderizada THEN os valores dos KPIs SHALL ser idênticos (± R$ 0,01) aos retornados por `DashboardService.getStats()` para o mesmo login e competência
3. WHEN o PDF inclui breakdown THEN SHALL haver seção **Por Centro de Custo** com tabela (CC, Qtd. Funcionários, Valor Total R$) ordenada por valor DESC, limitada a top 15 + linha "Outros" se houver mais
4. WHEN o PDF inclui breakdown THEN SHALL haver seção **Por Linha de Negócio** com mesma estrutura da seção CC
5. WHEN o PDF inclui rubricas THEN SHALL haver seção **Top 5 Proventos** e **Top 5 Descontos** (código, descrição, valor total, qtd. lançamentos)
6. WHEN o PDF inclui evolução THEN SHALL haver gráfico estático de **Evolução dos últimos 6 meses** (custo empresa ou líquido — mesmo campo usado no dashboard scoped) com rótulos `MMM/yyyy`
7. WHEN o usuário tem escopo restrito (sem `ACESSO_TOTAL`) THEN todos os totais e tabelas SHALL refletir **apenas** centros de custo acessíveis — paridade com dashboard scoped
8. WHEN a competência não possui folha processada/importada THEN o PDF SHALL ser gerado com capa + mensagem "Sem dados para a competência selecionada" (status `PROCESSADO`, não `ERRO`)
9. WHEN qualquer página além da capa existir THEN SHALL haver rodapé com numeração `Página X de Y` e texto "Gerado pelo Sistema de Folha — Techne"

**Independent Test**: Gerar PDF com usuário admin → abrir → conferir KPIs contra GET `/dashboard/stats`; repetir com usuário scoped e confirmar totais menores.

---

### P1: Relatório Custo Benefício + Folha (PDF premium) ⭐ MVP

**User Story**: Como analista de RH/financeiro, quero um PDF consolidando custo de benefícios por tipo e custo de folha, para substituir a planilha *Relatório Custo Benefício Folha*.

**Why P1**: Evidência operacional real (migration V1.12); alto valor imediato.

**Acceptance Criteria**:

1. WHEN o PDF de benefícios é gerado THEN a capa SHALL conter: logo, título "Relatório de Custo — Benefícios e Folha", competência, KPIs — Total Benefícios (R$), Qtd. Lançamentos, Total Custo Folha (R$), Custo Empresa Consolidado (R$ = custo folha + benefícios no escopo ACL)
2. WHEN o PDF lista benefícios THEN SHALL haver tabela **Resumo por Tipo** (código, descrição, total R$, qtd. lançamentos) — paridade com `GET /beneficio-mensal/resumo` para a competência
3. WHEN o usuário clica visualmente em um tipo na tabela (representado no PDF como sub-seção) THEN o PDF SHALL incluir **detalhamento Top 10 funcionários** por valor naquele tipo (nome, CC, valor R$); demais agrupados em "Outros (N funcionários, R$ X)"
4. WHEN o PDF inclui visão cruzada THEN SHALL haver matriz simplificada **Top 5 Centros de Custo × Top 5 Tipos de Benefício** (valores R$) quando houver dados
5. WHEN não existem benefícios na competência mas existe folha THEN o PDF SHALL gerar capa + seção folha + nota "Nenhum benefício lançado"
6. WHEN valores monetários são exibidos THEN SHALL usar formato pt-BR `R$ 1.234,56` em todo o documento

**Independent Test**: Importar benefícios de uma competência conhecida → gerar PDF → conferir totais por tipo contra API resumo.

---

### P1: Hub de Relatórios redesenhado ⭐ MVP

**User Story**: Como usuário, quero uma tela de relatórios visualmente atraente com seleção de competência e preview das opções, para descobrir e gerar documentos sem fricção.

**Why P1**: "Encher os olhos" começa na UI; tela atual é tabela crua sem seletor de competência.

**Acceptance Criteria**:

1. WHEN o usuário abre `/relatorios` THEN o sistema SHALL exibir **cards de catálogo** (não apenas tabs) para cada tipo: "Executivo de Folha" e "Custo Benefício + Folha", com ícone, descrição curta e badge do último status gerado
2. WHEN o usuário seleciona competência (MonthPicker mês/ano) THEN botão "Gerar" SHALL usar essa competência (não apenas mês corrente hardcoded)
3. WHEN a geração está em andamento (`PENDENTE`) THEN o card SHALL exibir indicador de progresso e desabilitar re-geração até concluir
4. WHEN o relatório está `PROCESSADO` THEN SHALL exibir botão **Baixar PDF** com ícone e preview thumbnail estático (primeira página renderizada como imagem ≤ 200 px, ou ícone PDF premium se thumbnail indisponível)
5. WHEN o relatório está `ERRO` THEN SHALL exibir mensagem de erro amigável e botão **Tentar novamente**
6. WHEN a tela renderiza THEN SHALL aplicar layout responsivo Material-UI v7 consistente com Dashboard (espaçamento generoso, tipografia hierárquica, cores primárias)
7. WHEN o usuário usa leitor de tela THEN cards e ações SHALL ser navegáveis por `role`/label (sem depender só de ícones)

**Independent Test**: Vitest — selecionar competência passada → mock service → verificar payload `{ mes, ano }` correto; Playwright smoke opcional P2.

---

### P2: Export CSV tabular

**User Story**: Como analista, quero baixar CSV além do PDF, para pivotar dados em Excel.

**Why P2**: Auditoria operacional; complementa PDF sem duplicar layout.

**Acceptance Criteria**:

1. WHEN o usuário clica "Exportar CSV" no card de relatório processado THEN o sistema SHALL baixar CSV UTF-8 com separador `;` (padrão BR)
2. WHEN o CSV é de folha THEN SHALL conter uma linha por funcionário: nome, CC, linha negócio, cargo, bruto, líquido, custo folha, custo benefícios, custo empresa — paridade com `GET /folha-pagamento/totais-funcionarios`
3. WHEN o CSV é de benefícios THEN SHALL conter uma linha por lançamento: funcionário, CC, tipo código, tipo descrição, valor

**Independent Test**: Gerar → download CSV → parse header + row count vs API.

---

### P2: Relatório Custo por Funcionário (PDF)

**User Story**: Como gestor de CC, quero PDF tabular ranking de custo por funcionário, para identificar outliers no fechamento.

**Acceptance Criteria**:

1. WHEN gerado THEN PDF SHALL listar funcionários ordenados por `custoEmpresa` DESC com colunas: #, Nome, Cargo, CC, Bruto, Líquido, Custo Empresa
2. WHEN escopo ACL restringe CC THEN lista SHALL conter apenas funcionários visíveis
3. WHEN competência tem > 100 funcionários THEN PDF SHALL paginar (50 linhas/página) mantendo cabeçalho repetido

**Independent Test**: Comparar soma coluna Custo Empresa com total dashboard scoped.

---

### P2: Configuração de marca (logo e cores)

**User Story**: Como administrador, quero que o sistema use a identidade visual Techne nos PDFs sem alterar código.

**Acceptance Criteria**:

1. WHEN existe arquivo `branding/logo.png` no classpath THEN PDFs SHALL renderizá-lo na capa
2. WHEN admin substitui o arquivo (deploy ou volume montado) THEN próximas gerações SHALL usar o novo logo sem restart se config `spring.devtools` ou reload explícito documentado; senão após restart — documentar no README
3. WHEN cores são configuradas via `application.yml` (`relatorios.branding.primary-color`, `secondary-color`) THEN capa e cabeçalhos SHALL refletir essas cores (hex válido `#RRGGBB`)

**Independent Test**: Trocar logo placeholder → gerar PDF → inspeção visual.

---

### P3: Variação mês a mês (MoM)

**User Story**: Como diretor financeiro, quero ver no PDF a variação percentual vs mês anterior nos KPIs principais.

**Acceptance Criteria**:

1. WHEN competência N tem competência N-1 com dados THEN KPIs na capa SHALL exibir delta % com seta ↑ verde (redução de custo) ou ↓ vermelho (aumento) conforme métrica
2. WHEN N-1 não existe THEN delta SHALL exibir "—" sem erro

---

### P3: Purge automático de relatórios antigos

**User Story**: Como operador de infra, quero que PDFs com mais de 12 meses sejam removidos automaticamente.

**Acceptance Criteria**:

1. WHEN job diário executa THEN registros com `dataProcessamento` > 365 dias SHALL ser soft-deleted e blob removido
2. WHEN purge executa THEN SHALL logar quantidade removida no domain `relatorios`

---

## Edge Cases

- WHEN usuário sem vínculo organograma tenta gerar THEN sistema SHALL retornar HTTP 403 (mesma regra folha scoped)
- WHEN geração falha por exceção interna THEN status `ERRO`, mensagem genérica ao usuário, detalhe no log server-side
- WHEN download é solicitado para `id` inexistente THEN HTTP 404
- WHEN competência futura (mês/ano > corrente) THEN HTTP 400 "Competência futura não permitida"
- WHEN re-geração é solicitada para mesma tupla `(tipo, mes, ano, login)` THEN sistema SHALL invalidar PDF anterior e substituir metadados
- WHEN PDF excede 50 MB THEN geração SHALL falhar com `ERRO` e mensagem "Relatório excede tamanho máximo"

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| REL-01 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-02 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-03 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-04 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-05 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-06 | P1: Backend API | Execute | Verified (pending Verifier) |
| REL-07 | P1: PDF Folha capa/KPIs | Execute | Verified (pending Verifier) |
| REL-08 | P1: PDF Folha paridade dashboard | Execute | Verified (pending Verifier) |
| REL-09 | P1: PDF Folha breakdown CC | Execute | Verified (pending Verifier) |
| REL-10 | P1: PDF Folha breakdown LN | Execute | Verified (pending Verifier) |
| REL-11 | P1: PDF Folha top rubricas | Execute | Verified (pending Verifier) |
| REL-12 | P1: PDF Folha evolução 6m | Execute | Verified (pending Verifier) |
| REL-13 | P1: PDF Folha ACL scoped | Execute | Verified (pending Verifier) |
| REL-14 | P1: PDF Folha competência vazia | Execute | Verified (pending Verifier) |
| REL-15 | P1: PDF Folha rodapé | Execute | Verified (pending Verifier) |
| REL-16 | P1: PDF Benefícios capa/KPIs | Execute | Verified (pending Verifier) |
| REL-17 | P1: PDF Benefícios resumo tipo | Execute | Verified (pending Verifier) |
| REL-18 | P1: PDF Benefícios drill-down | Execute | Verified (pending Verifier) |
| REL-19 | P1: PDF Benefícios matriz CC×tipo | Execute | Verified (pending Verifier) |
| REL-20 | P1: PDF Benefícios folha sem benefício | Execute | Verified (pending Verifier) |
| REL-21 | P1: PDF Benefícios formato moeda | Execute | Verified (pending Verifier) |
| REL-22 | P1: Hub catálogo cards | Execute | Verified (pending Verifier) |
| REL-23 | P1: Hub seletor competência | Execute | Verified (pending Verifier) |
| REL-24 | P1: Hub estado PENDENTE | Execute | Verified (pending Verifier) |
| REL-25 | P1: Hub download/thumbnail | Execute | Verified (pending Verifier) |
| REL-26 | P1: Hub estado ERRO | Execute | Verified (pending Verifier) |
| REL-27 | P1: Hub layout/a11y | Execute | Verified (pending Verifier) |
| REL-28 | P2: CSV folha | - | Pending |
| REL-29 | P2: CSV benefícios | - | Pending |
| REL-30 | P2: PDF custo/funcionário | - | Pending |
| REL-31 | P2: Branding config | - | Pending |
| REL-32 | P3: MoM delta | - | Pending |
| REL-33 | P3: Purge 12m | - | Pending |

**Coverage:** 33 total, 27 mapped to tasks (REL-01…27 Execute complete, pending Verifier), 6 deferred P2/P3

---

## Success Criteria

- [ ] Tela `/relatorios` funciona end-to-end (gerar → listar → baixar) sem 404
- [ ] PDF Executivo de Folha passa review visual: capa + KPIs + ≥3 seções analíticas em ≤ 5 páginas para competência típica (~200 funcionários)
- [ ] Totais do PDF batem com dashboard (± R$ 0,01) para mesmo usuário/competência
- [ ] Usuário scoped vê apenas dados do seu organograma no PDF
- [ ] Gestor consegue gerar e baixar relatório em < 2 minutos (incluindo processamento) para competência média
- [ ] Logo Techne aparece na capa após fornecimento do asset (placeholder aceitável até lá)

---

## Auto-Size Assessment

| Attribute | Value |
| --- | --- |
| **Scope** | **Large** — novo domínio backend, geração PDF, redesign frontend, ACL, jobs assíncronos |
| **Design** | **Required** — escolha biblioteca PDF, schema persistência, templates visuais |
| **Tasks** | **Required** — 15 tarefas MVP em 4 fases ([`tasks.md`](tasks.md)) |
| **Discuss** | Parcialmente coberto via Assumptions; confirmar assets de marca com usuário |

**Próximo passo sugerido:** Tasks (`tasks.md`) após aprovação do [`design.md`](design.md).
