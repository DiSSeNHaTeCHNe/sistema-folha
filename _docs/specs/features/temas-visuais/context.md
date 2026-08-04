# Temas Visuais — Context

Decisões do usuário capturadas na fase Specify. Cada uma fecha uma gray area que
tinha mais de um caminho válido.

---

## D1 — Persistência: `localStorage` apenas

**Pergunta**: onde persistir a preferência de tema?

**Escolha**: `localStorage`, chave `sistema-folha:tema`.

**Alternativas descartadas**:

- *Backend por usuário* — traria migration Flyway, coluna em `usuario`, endpoint de
  preferências, ACL e testes de API para dentro da Fase 1. Escopo desproporcional ao
  ganho, que é a preferência seguir o usuário entre máquinas.
- *Híbrido (local agora, backend depois)* — mesma entrega da opção escolhida, com
  compromisso futuro implícito.

**Consequência**: a escolha vale por navegador e por máquina. Um usuário que troca de
computador vê o tema padrão. Isso é aceito.

**Registrar como Deferred Idea no STATE.md**: "Preferência de tema por usuário no
backend" — retomar se houver demanda real.

---

## D2 — Tema padrão ao final: `techne`

**Pergunta**: qual tema fica como padrão depois das cinco fases?

**Escolha**: `techne`.

**Razão determinante**: o backend já emite os relatórios PDF com `#7836FC` e `#3661FC`
(`application.yml`, `RelatorioBrandingProperties`, AD-015). Manter a interface em azul
MUI genérico enquanto o relatório sai na marca da empresa é uma incoerência visível
para quem recebe os dois artefatos.

**Alternativas descartadas**:

- *Manter `classico`* — ninguém percebe mudança sem opt-in, mas o esforço das quatro
  fases fica invisível por padrão.
- *Decidir na Fase 5* — adiaria uma decisão que já tem critério objetivo.

**Consequência**: usuários sem preferência gravada passam a ver a interface mudar na
entrega da Fase 5. Quem já escolheu um tema não é afetado (TEMA-17).

**Registrar como decisão AD-NNN no STATE.md** ao concluir a Fase 5.

---

## D3 — Seletor no menu do avatar

**Pergunta**: onde o usuário troca de tema?

**Escolha**: item "Aparência" no menu do avatar do `AppBar`, abrindo um dialog com as
opções e amostras de cor.

**Alternativas descartadas**:

- *Página `/preferencias`* — mais escalável, mas cria rota, entrada de menu e navegação
  para hospedar uma única opção.
- *Ambos* — duplicaria a superfície sem necessidade neste momento.

**Consequência**: reusa o padrão já estabelecido por `AlterarSenhaDialog` — mesmo local
de gatilho, mesma mecânica de dialog, mesmo estilo de teste. Se surgirem outras
preferências, o dialog vira o embrião de uma página.

---

## D4 — Tokenizar tudo na Fase 1

**Pergunta**: o Dashboard (25 cores fixas) e o Organograma (6) ignoram o tema. Quando
tokenizar?

**Escolha**: integralmente na Fase 1.

**Alternativas descartadas**:

- *Só o necessário por fase* — entregaria a Fase 2 mais cedo, mas o Indigo dark
  (Fase 4) expõe todas as cores fixas de uma vez, concentrando o risco justamente na
  fase mais arriscada.
- *Fase 1 sem Organograma* — deixaria os nós do ReactFlow em azul `#1976d2` fixo em
  todos os temas, um defeito visível.

**Consequência**: a Fase 1 fica mais pesada (10 tasks) e as Fases 2-5 ficam baratas
(2-4 tasks cada), consistindo essencialmente em declarar tokens e verificar. É a
distribuição de risco desejada: o trabalho difícil acontece uma vez, com o tema atual
como referência de regressão.

---

## Contexto técnico relevante levantado no scan

- Stack real do frontend: React 19 + MUI v7 + Vite 6 + Vitest 4. O `frontend/AGENTS.md`
  descreve Tailwind e Vite 8, que são **TARGET**, não o estado atual (AD-004).
- `main.tsx` define o tema inline com `createTheme`; existe também um
  `src/theme.ts` exportando um tema mais completo que **não é usado** por ninguém.
  A Fase 1 precisa resolver essa duplicidade.
- `src/index.css` é o boilerplate do Vite, com `background-color: #242424`,
  `body { display: flex }` e estilo global em `button`.
- 33 arquivos de teste no frontend; `src/test/renderWithProviders.tsx` é o harness
  padrão e hoje envolve apenas `MemoryRouter` e o mock de auth — precisará envolver
  também o provider de tema.
- Cores fixas por arquivo: `pages/Dashboard/index.tsx` (25), `components/OrganogramaGrafico/index.tsx` (6),
  `theme.ts` (3), `main.tsx` (2), `pages/Funcionarios/index.tsx` (1). Mais `rgba(` em
  Dashboard (11), Funcionários (3) e `RelatorioCatalogCard.tsx` (1).
- Não há lições confirmadas no store (`lessons.py list --status confirmed` retorna vazio).
