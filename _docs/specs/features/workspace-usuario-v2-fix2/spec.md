# workspace-usuario-v2-fix2 — Data router e correção blank screen Specification

**Parent:** `_docs/specs/features/workspace-usuario-v2/` (WKS2-09 exige `useBlocker`; validation PASS @ `5d65deb` após fix1)  
**Baseline congelado:** `_docs/specs/features/workspace-usuario/spec.md` (WKS-01…32 — regressão = bug v1)  
**Related:** WKS2-09; bug runtime confirmado em produção/dev (`useBlocker must be used within a data router`)  
**Complexity:** Medium  
**Spec status:** Draft — aguardando aprovação → Design → Tasks → Execute

> **Nota:** fix2 **não reimplementa** fluxos v2. Corrige crash ao abrir workspace (`/workspace/:id`) causado por `useUnsavedChangesGuard` → `useBlocker` rodando sob `BrowserRouter` declarativo. Solução escolhida: **Opção A** — migrar roteamento global para data router (`createBrowserRouter` + `RouterProvider`). Branch de execução: `feat/workspace-usuario-v2` (commits prefixados `fix2:`).

## Problem Statement

Após fix1, o hub `/workspace` renderiza normalmente, mas ao clicar **Abrir** em um workspace a tela fica **branca**. O console exibe:

```text
Uncaught Error: useBlocker must be used within a data router.
```

`WorkspaceDetailPage` monta `useUnsavedChangesGuard`, que chama `useBlocker` (React Router v7). Esse hook **exige** data router (`RouterProvider` + `createBrowserRouter` / `createHashRouter`). A app usa `BrowserRouter` declarativo em `frontend/src/routes/index.tsx`, então o React quebra antes de renderizar a página de detalhe.

Os testes de `WorkspaceDetailPage` **mockam** o guard e não reproduzem o crash; o teste unitário de `useUnsavedChangesGuard` usa `RouterProvider` corretamente — só a app real está inconsistente.

## Goals

- [ ] Gestor abre workspace via **Abrir** e vê a tela de detalhe (toolbar, widgets ou empty state) — **NOT** tela branca
- [ ] Console **sem** erro `useBlocker must be used within a data router` ao navegar para `/workspace/:id`
- [ ] WKS2-09 preservado: guard de alterações não salvas (`beforeunload` + bloqueio in-app) funcional em modo edição
- [ ] Todas as rotas existentes continuam resolvendo (paridade de URLs, guards `PrivateRoute` / `AdminRoute` / `WorkspaceRoute` / `ApiKeyRoute`)
- [ ] Suite FE workspace + regressão global permanece verde

## Out of Scope

| Feature | Reason |
| --- | --- |
| Lazy loading / code splitting por rota | Skill `routing-perf` é TARGET (AD-004); não bloqueia fix2 |
| Reestruturar pastas para `src/features/` | Brownfield; fora do escopo cirúrgico |
| Adicionar `useUnsavedChangesGuard` em `DatasetEditorPage` | Design v2 previu, mas não está montado hoje; follow-up separado |
| Playwright E2E jornada workspace | Parent Out of Scope v2; fix2 valida via Vitest + smoke manual |
| Migrar `renderWithProviders` para data router em **todos** os testes do repo | Apenas o mínimo para cobrir ACs fix2; bulk migration = follow-up |
| Alterações backend / OpenAPI | Bug 100% frontend routing |
| Remover `WorkspacePage.tsx` deprecated | Fora do fix2 |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| IDs de requisito | Prefixo `WKS2F2-NN`; refinam WKS2-09 + bug blank screen | Padrão fix1 (`WKS2F1-NN`) | n |
| **Abordagem de correção** | Opção A: migrar app inteira para data router | Usuário escolheu; habilita `useBlocker` nativamente; alinha com design v2 T8 | y |
| API de roteamento | `createBrowserRouter` + `RouterProvider`; rotas como `RouteObject[]` | Padrão React Router v7 para data APIs; substitui `<BrowserRouter><Routes>` | n |
| Escopo da migração | **Global** — todas as rotas em `frontend/src/routes/index.tsx` | `useBlocker` exige contexto de data router na árvore; migrar só workspace não basta se o provider não envolver a app | n |
| `AuthProvider` | Permanece envolvendo `RouterProvider` (ou como root route wrapper) — **sem** mover lógica de auth | Preservar `useAuth` em guards; ordem: `AuthProvider` → `RouterProvider` | n |
| Basename / future flags | Manter basename implícito (`/`); sem `future` flags experimentais | Paridade com setup atual | n |
| Test harness | `renderWithProviders` continua com `MemoryRouter` para testes legados; **novo** helper ou caso de teste com `createMemoryRouter` + `RouterProvider` para AC de integração do detail page **sem mock** do guard | Evita big-bang em 30+ arquivos de teste; AC fix2 exige prova real do guard | n |
| Branch Execute | `feat/workspace-usuario-v2`; commits `fix2:` | Mesma branch da v2/fix1 | n |

**Open questions:** none blocking — defaults acima registrados para confirmação na aprovação da spec.

**Implicit-requirement dimensions (Medium — relevant only):**

| Dimension | Resolution |
| --- | --- |
| Failure / partial-failure | Se rota desconhecida, manter redirect `*` → `/dashboard`; se auth loading, spinner fullscreen (paridade `PrivateRoute`) |
| Auth boundaries | Guards existentes inalterados em comportamento; apenas adaptados à forma `RouteObject` |
| State-transition integrity | `useBlocker` só ativo quando `dirty=true` (WKS2-09); confirm dialog antes de descartar |
| Concurrency / ordering | N/A |
| Remaining dimensions | N/A for this fix scope |

---

## User Stories

### P1: Abrir workspace sem crash ⭐ MVP

**User Story**: Como gestor, quero clicar **Abrir** no hub e ver o detalhe do workspace, para continuar editando widgets ou instalar templates.

**Why P1**: Bug bloqueante — funcionalidade core v2 inacessível em runtime.

**Acceptance Criteria**:

1. (WKS2F2-01) WHEN usuário clica **Abrir** no hub THEN navegação SHALL ir para `/workspace/{id}` e renderizar `WorkspaceDetailPage` com heading do workspace — **NOT** tela branca
2. (WKS2F2-02) WHEN `WorkspaceDetailPage` monta THEN console SHALL **NOT** conter erro `useBlocker must be used within a data router`
3. (WKS2F2-03) WHEN workspace existe e API responde THEN página SHALL exibir toolbar (ex.: botões **Adicionar widget**, **Editar layout**) ou empty state **Workspace vazio** — conforme fixtures com/sem widgets

**Independent Test**: Mock services → render app (ou detail) sob `RouterProvider` → clicar Abrir ou navegar direto → assert heading visível; Vitest **sem** mock de `useUnsavedChangesGuard`.

---

### P1: Data router global ⭐ MVP

**User Story**: Como desenvolvedor, quero que o roteamento use data APIs do React Router v7, para que hooks como `useBlocker` funcionem em produção.

**Why P1**: Pré-requisito técnico da correção; sem isso WKS2-09 permanece quebrado.

**Acceptance Criteria**:

1. (WKS2F2-04) WHEN app inicia THEN root SHALL usar `RouterProvider` com router criado por `createBrowserRouter` — **NOT** `<BrowserRouter>`
2. (WKS2F2-05) WHEN usuário acessa rotas existentes (`/dashboard`, `/workspace`, `/workspace/datasets`, `/login`, rota admin `/usuarios`) THEN cada rota SHALL renderizar o componente correto (smoke: elemento característico ou redirect esperado)
3. (WKS2F2-06) WHEN usuário não autenticado acessa rota privada THEN SHALL redirecionar para `/login` (paridade `PrivateRoute`)
4. (WKS2F2-07) WHEN usuário sem acesso workspace acessa `/workspace` THEN SHALL ver alerta de acesso negado (paridade `WorkspaceRoute`)

**Independent Test**: Vitest smoke em `routes/index` ou teste de integração leve com `createMemoryRouter` espelhando árvore de rotas; `npm run build` passa.

---

### P1: WKS2-09 guard funcional ⭐ MVP

**User Story**: Como gestor em modo edição de layout, quero ser avisado antes de sair com alterações não salvas, para não perder trabalho.

**Why P1**: Requisito parent WKS2-09; motivo original do `useBlocker`.

**Acceptance Criteria**:

1. (WKS2F2-08) WHEN modo edição ativo e layout **dirty** THEN `useUnsavedChangesGuard` SHALL registrar listener `beforeunload` (paridade teste existente)
2. (WKS2F2-09) WHEN modo edição dirty e usuário tenta navegação in-app THEN sistema SHALL exibir `window.confirm` com mensagem padrão **Existem alterações não salvas. Deseja sair sem salvar?** antes de prosseguir
3. (WKS2F2-10) WHEN usuário cancela o confirm THEN navegação SHALL permanecer na página de detalhe
4. (WKS2F2-11) WHEN `dirty=false` THEN navegação in-app SHALL proceder **sem** confirm

**Independent Test**: `useUnsavedChangesGuard.test.tsx` (já usa `RouterProvider`) permanece verde; **novo** teste de integração em `WorkspaceDetailPage` **sem mock** do guard validando WKS2F2-09/10/11.

---

### P2: Regressão e observabilidade

**User Story**: Como QA, quero garantia automatizada de que a migração não quebrou outras áreas.

**Why P2**: Migração global de router tem blast radius; gate de regressão reduz risco de merge.

**Acceptance Criteria**:

1. (WKS2F2-12) WHEN `npm test` executar THEN contagem de testes SHALL ser ≥ baseline pré-fix2 (sem deleções silenciosas)
2. (WKS2F2-13) WHEN `npm run build` executar THEN SHALL completar sem erro TypeScript
3. (WKS2F2-14) WHEN `npm test -- src/pages/Workspace` executar THEN suite workspace SHALL passar 100%

**Independent Test**: Release gate FE documentado em tasks.md.

---

## Edge Cases

- WHEN usuário acessa `/workspace/:id` com id inválido (`NaN`) THEN SHALL exibir **Workspace não encontrado** — não crash
- WHEN `getWorkspace` falha (404/rede) THEN SHALL exibir estado de erro com botão **Voltar ao hub** — não tela branca
- WHEN usuário confirma descarte no guard THEN navegação SHALL completar para destino
- WHEN hot reload em dev após migração THEN app SHALL remontar sem erro de router duplicado

---

## Requirement Traceability

| Requirement ID | Story | Refina | Status |
| --- | --- | --- | --- |
| WKS2F2-01 | P1: Abrir sem crash | blank screen bug | Done |
| WKS2F2-02 | P1: Abrir sem crash | blank screen bug | Done |
| WKS2F2-03 | P1: Abrir sem crash | WKS2-08 parcial | Done |
| WKS2F2-04 | P1: Data router | infra routing | Done |
| WKS2F2-05 | P1: Data router | infra routing | Done |
| WKS2F2-06 | P1: Data router | auth parity | Done |
| WKS2F2-07 | P1: Data router | workspace ACL | Done |
| WKS2F2-08 | P1: WKS2-09 guard | WKS2-09 | Done |
| WKS2F2-09 | P1: WKS2-09 guard | WKS2-09 | Done |
| WKS2F2-10 | P1: WKS2-09 guard | WKS2-09 | Done |
| WKS2F2-11 | P1: WKS2-09 guard | WKS2-09 | Done |
| WKS2F2-12 | P2: Regressão | — | Done |
| WKS2F2-13 | P2: Regressão | — | Done |
| WKS2F2-14 | P2: Regressão | workspace suite | Done |

**Coverage:** 14 total, 14 mapped (Design/Tasks pending)

### Mapa fix2 → parent / bug

| Lacuna / bug | Requisitos fix2 |
| --- | --- |
| Tela branca ao Abrir workspace | WKS2F2-01…03 |
| `BrowserRouter` incompatível com `useBlocker` | WKS2F2-04…07 |
| WKS2-09 guard quebrado em produção | WKS2F2-08…11 |
| Regressão pós-migração | WKS2F2-12…14 |

---

## Success Criteria

- [ ] Gestor abre workspace pelo hub e vê conteúdo — zero tela branca
- [ ] Console limpo de erro `useBlocker` em `/workspace/:id`
- [ ] Guard dirty funciona em modo edição (confirm ao sair)
- [ ] `npm test` + `npm run build` verdes; suite workspace ≥ baseline
- [ ] Verifier fix2: 14/14 WKS2F2 + WKS2-09 re-evidenciado PASS

---

## Auto-Size Assessment

| Attribute | Value |
| --- | --- |
| **Scope** | **Medium** — migração `routes/index.tsx` para data router; ajuste test harness mínimo; 1 teste integração detail sem mock guard |
| **Design** | **Recommended (light)** — árvore `RouteObject`, ordem providers, estratégia test harness |
| **Tasks** | **Required** — ~3–5 tasks (router migration, integration test, guard unmock, regression gate) |
| **Discuss** | Não acionado — Opção A confirmada pelo usuário |

**Próximo passo sugerido:** aprovar spec → **Design** (light) → **Tasks** → **Execute** em `feat/workspace-usuario-v2` (prefixo commits `fix2:`).
