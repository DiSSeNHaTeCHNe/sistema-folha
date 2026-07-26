# Ajuste Benefícios — Tasks

## Task Overview

| # | Task | Depends | Parallel | Estimate |
|---|------|---------|----------|----------|
| 1 | Backend: remover BeneficioController + BeneficioService + BeneficioDTO | — | [P] | S |
| 2 | Backend: remover ImportacaoBeneficioService (CSV) + endpoint | — | [P] | S |
| 3 | Frontend: criar página TiposBeneficio (CRUD) | — | [P] | M |
| 4 | Frontend: adicionar "Tipos de Benefício" ao menu Cadastros + rota | 3 | — | S |
| 5 | Frontend: substituir card CSV por card Benefícios Mensais (.xlsx) em /importacao | — | [P] | M |
| 6 | Frontend: remover ImportacaoDialog de BeneficiosMensais | 5 | — | S |
| 7 | Frontend: remover página Beneficios + rota + menu item + adicionar redirect | 1 | — | S |
| 8 | Validação: build backend + frontend + teste manual | 1-7 | — | S |

**Legenda:** S = small (< 1h), M = medium (1-3h), [P] = paralelizável

---

## Task Definitions

### Task 1: Backend — Remover BeneficioController + BeneficioService + BeneficioDTO

**What:** Deletar controller, service e DTO do módulo de benefícios legado. Verificar que nenhum outro service depende deles.

**Where:**
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/controller/BeneficioController.java`
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/service/BeneficioService.java`
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/dto/BeneficioDTO.java`
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/exception/BeneficioNotFoundException.java` (se existir)

**Depends on:** Nenhuma

**Pre-check:** Grep por `BeneficioService` e `BeneficioController` em todo o backend para confirmar que não há outros consumidores.

**Done when:**
- Arquivos deletados
- `./mvnw compile -pl backend` passa sem erro
- Endpoint `GET /beneficios/funcionario/1?data=2024-01-01` retorna 404

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 2: Backend — Remover ImportacaoBeneficioService (CSV)

**What:** Deletar o service de importação CSV de benefícios legado e o endpoint correspondente.

**Where:**
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/service/ImportacaoBeneficioService.java`
- EDIT controller de importação (remover endpoint `POST /importacao/beneficios`)

**Depends on:** Nenhuma

**Pre-check:** Grep por `ImportacaoBeneficioService` e `importarBeneficios` para encontrar referências.

**Done when:**
- Service deletado
- Endpoint CSV removido
- `./mvnw compile -pl backend` passa sem erro

**Gate:** `./mvnw compile -pl backend` exit 0

---

### Task 3: Frontend — Criar página TiposBeneficio (CRUD)

**What:** Criar tela CRUD para Tipos de Benefício seguindo o **mesmo padrão de `Cargos/index.tsx`**.

**Where:** `frontend/src/pages/TiposBeneficio/index.tsx`

**Depends on:** Nenhuma (o backend `TipoBeneficioController` já existe)

**Reuses:**
- Padrão de layout `Cargos/index.tsx` (lista + dialog de criação/edição)
- `tipoBeneficioService.ts` (já existe)
- MUI components (Card, Dialog, TextField, Button)
- `react-toastify` para feedback
- Helpers: `getApiErrorMessage`

**Done when:**
- Lista exibe tipos com colunas: Código, Descrição, Ativo (badge)
- Botão "Novo Tipo" abre dialog com campos Código + Descrição
- Botão "Editar" em cada row abre dialog pré-preenchido (código read-only)
- Botão "Desativar" faz soft delete com confirmação
- Validação: código obrigatório, descrição obrigatória
- Erro 409 (código duplicado) exibe toast amigável
- `npm run build` sem erro

**Gate:** `npm run build` (frontend) exit 0

---

### Task 4: Frontend — Adicionar ao menu Cadastros + rota

**What:** Registrar "Tipos de Benefício" na seção Cadastros do sidebar e criar rota no router.

**Where:**
- `frontend/src/components/Layout/index.tsx` → adicionar item ao `cadastroItems`
- `frontend/src/routes/index.tsx` → import + Route

**Depends on:** Task 3

**Done when:**
- Item "Tipos de Benefício" visível na seção Cadastros do menu com ícone `CardGiftcard`
- Clicar navega para `/tipos-beneficio`
- Rota renderiza `TiposBeneficio` page

**Gate:** `npm run build` exit 0

---

### Task 5: Frontend — Substituir card CSV por card Benefícios Mensais em /importacao

**What:** Na página `/importacao`, substituir o card "Importação de Benefícios" (aceita .csv, sem competência) por um novo card "Importação de Benefícios Mensais" (aceita .xlsx, com seletor de competência).

**Where:** `frontend/src/pages/Importacao/index.tsx`

**Depends on:** Nenhuma (endpoint `/importacao/beneficios-mensais` já existe no backend)

**Reuses:**
- Pattern do card "Folha ADP" existente (file input, validation, upload, conflict dialog)
- `beneficioMensalService.importar()` ou `importacaoService.importarBeneficiosMensais()`

**Changes:**
1. Substituir icon `CardGiftcardIcon` + texto "CSV" por "Benefícios Mensais" + ".xlsx"
2. Adicionar seletores de Mês (1-12) e Ano (current ± 2) antes do file input
3. Alterar validação de file extension de `.csv` para `.xlsx`
4. Alterar chamada de service: `importacaoService.importarBeneficios(file)` → `importacaoService.importarBeneficiosMensais(file, competenciaInicio, competenciaFim, confirmar)`
5. Tratar 409 com dialog de substituição (mesmo padrão que já existe para folha ADP)
6. Atualizar help dialog com instruções do novo formato

**Done when:**
- Card exibe "Importação de Benefícios Mensais" com seletores de competência
- Aceita apenas `.xlsx`
- Upload chama endpoint correto com competência
- Dialog de substituição funciona em caso de 409
- Card de status exibe resultado
- `npm run build` sem erro

**Gate:** `npm run build` exit 0

---

### Task 6: Frontend — Remover ImportacaoDialog de BeneficiosMensais

**What:** Deletar `ImportacaoDialog.tsx` e remover referência/botão de importação da página BeneficiosMensais.

**Where:**
- DELETE `frontend/src/pages/BeneficiosMensais/ImportacaoDialog.tsx`
- EDIT `frontend/src/pages/BeneficiosMensais/index.tsx` → remover import, state, botão "Importar" e render do dialog

**Depends on:** Task 5 (importação já funciona em `/importacao`)

**Done when:**
- Arquivo `ImportacaoDialog.tsx` não existe
- Página BeneficiosMensais não tem botão/dialog de importação
- `npm run build` sem erro

**Gate:** `npm run build` exit 0

---

### Task 7: Frontend — Remover página Beneficios + redirect

**What:** Deletar a página legada `/beneficios`, remover do router e menu, e adicionar redirect.

**Where:**
- DELETE `frontend/src/pages/Beneficios/` (toda a pasta)
- EDIT `frontend/src/routes/index.tsx`:
  - Remover `import { Beneficios }` 
  - Remover `<Route path="/beneficios" element={<Beneficios />} />`
  - Adicionar `<Route path="/beneficios" element={<Navigate to="/beneficios-mensais" replace />} />`
- EDIT `frontend/src/components/Layout/index.tsx`:
  - Remover objeto `{ text: 'Benefícios', icon: <CardGiftcard />, path: '/beneficios' }` do `menuItems`

**Depends on:** Task 1 (backend endpoint já não existe)

**Done when:**
- Pasta `Beneficios/` não existe
- Menu lateral não tem item "Benefícios" (apenas "Benefícios Mensais")
- URL `/beneficios` redireciona para `/beneficios-mensais`
- `npm run build` sem erro

**Gate:** `npm run build` exit 0

---

### Task 8: Validação — Build completo + smoke test

**What:** Verificar que tudo compila e funciona end-to-end.

**Where:** Raiz do projeto

**Depends on:** Tasks 1-7

**Done when:**
- `./mvnw compile -pl backend` → exit 0
- `npm run build` (frontend) → exit 0
- Menu lateral: "Benefícios Mensais" + "Tipos de Benefício" em Cadastros (sem "Benefícios")
- `/importacao` mostra cards "Folha ADP" + "Benefícios Mensais"
- `/beneficios` redireciona para `/beneficios-mensais`
- `/tipos-beneficio` exibe CRUD funcional

**Gate:** Ambos builds passam + visual review

---

## Execution Order

```
Phase 1 (todas paralelas):
  Task 1 — backend: remover controller/service legado
  Task 2 — backend: remover importação CSV
  Task 3 — frontend: criar TiposBeneficio
  Task 5 — frontend: card .xlsx em /importacao

Phase 2 (depende de Phase 1):
  Task 4 — menu + rota (depende de Task 3)
  Task 6 — remover ImportacaoDialog (depende de Task 5)
  Task 7 — remover página Beneficios (depende de Task 1)

Phase 3:
  Task 8 — validação final
```

## Status

| Task | Status | Assignee |
|------|--------|----------|
| 1 | Complete | agent |
| 2 | Complete | agent |
| 3 | Complete | agent |
| 4 | Complete | agent |
| 5 | Complete | agent |
| 6 | Complete | agent |
| 7 | Complete | agent |
| 8 | Complete | agent |
