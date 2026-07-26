# AGENT.md

## 1. Papel do agente

Este projeto utiliza agentes para apoiar planejamento, especificação, implementação, QA, documentação e organização operacional.

Cada agente deve atuar dentro da sua frente de responsabilidade, respeitando o escopo da issue, os documentos canônicos do projeto e as regras de governança definidas no repositório.

---

## 2. Fontes de orientação

Antes de executar qualquer tarefa, o agente deve verificar se existe orientação aplicável em:

```text
.agents/rules/
.agents/skills/
.agents/references/
_docs/prd/
_docs/tdd/
_docs/sdd/
_docs/specs/
.agents/references/specs-layout.md
```

Quando houver conflito entre documentos, seguir esta prioridade:

```text
1. Instrução direta do usuário
2. Documentos canônicos mais recentes do projeto
3. PRD/TDD/SDD/SPEC relacionados à tarefa
4. Regras em .agents/rules/
5. Skills em .agents/skills/[pasta-da-skill]/SKILL.md
6. Referências detalhadas em .agents/references/
7. Convenções locais do código existente
```

O agente não deve improvisar comportamento que contradiga PRD, TDD, SDD, SPEC ou regras explícitas.

### Spec-driven outputs (TLC)

Artefatos gerados pela skill `tlc-spec-driven` e referências em `.agents/references/` devem usar **somente** o layout flat em `_docs/specs/`.

Regra completa:

```text
.agents/references/specs-layout.md
```

Resumo obrigatório:

- Raiz: `_docs/specs/` (nunca `.specs/`, `project/` ou `codebase/`)
- Projeto: `_docs/specs/PROJECT.md`, `ROADMAP.md`, `STATE.md`, `HANDOFF.md`
- Brownfield: `_docs/specs/{STACK,ARCHITECTURE,CONVENTIONS,STRUCTURE,TESTING,INTEGRATIONS,CONCERNS}.md`
- Features: `_docs/specs/features/[feature]/{spec,context,design,tasks}.md`
- Quick tasks: `_docs/specs/quick/NNN-slug/{TASK,SUMMARY}.md`

### Spec-driven verification checklist (P3)

Feature Large/Complex fechada ⇒ `_docs/specs/features/[feature]/validation.md` (nunca `.specs/`).

---

## 3. Linear Workflow

O projeto usa Linear para organizar trabalho multiagente.

Regras obrigatórias:

- Usar uma key única por produto/projeto, exemplo: `ARC-123`.
- Não criar keys separadas por frente, como `BACK-123`, `FRONT-123`, `QA-123`, salvo se existirem times Linear realmente separados.
- Identificar a frente pelo título, labels, assignee/agente e épico/PBI pai.
- Usar o padrão de título: `[Frente] Verbo + objeto + contexto`.
- Quebrar tarefas grandes em issues menores por frente.
- Cada agente deve trabalhar somente em issues da sua frente ou explicitamente atribuídas.
- Issues de implementação devem estar ligadas a PRD, TDD ou SDD quando aplicável.

Regra completa:

```text
.agents/rules/linear_issue_management.md
```

Skill para quebrar documentos em issues:

```text
.agents/skills/break-prd-tdd-sdd-into-linear-issues/SKILL.md
```

---

## 4. Uso de rules

Arquivos em `.agents/rules/` definem regras obrigatórias de governança.

O agente deve usar uma rule quando a tarefa envolver padrões, limites, convenções ou restrições do projeto.

Exemplo:

```text
.agents/rules/linear_issue_management.md
```

Usar esta rule sempre que a tarefa envolver:

- Linear;
- backlog;
- issues;
- épicos;
- PBIs;
- organização multiagente;
- separação por backend, frontend, QA, docs, infra ou planejamento.

---

## 5. Uso de skills

Arquivos em `.agents/skills/[pasta-da-skill]/SKILL.md` definem procedimentos operacionais estruturados.

O agente deve usar uma skill quando a tarefa exigir uma execução passo a passo ou a transformação de especificações técnicas em artefatos concretos.

Usar esta skill sempre que a tarefa pedir para:

- quebrar PRD em tarefas;
- transformar TDD/SDD/SPEC em backlog;
- criar issues para Linear;
- organizar execução paralela por múltiplos agentes;
- gerar épicos, PBIs, tasks e subtasks.

Consulte a especificação detalhada em:

```text
.agents/skills/break-prd-tdd-sdd-into-linear-issues/SKILL.md
```

---

## 6. Regras gerais de execução

O agente deve:

1. Ler a demanda antes de executar.
2. Identificar a frente responsável.
3. Verificar documentos relacionados.
4. Verificar regras e skills aplicáveis.
5. Não expandir escopo sem necessidade explícita.
6. Declarar dependências, riscos e bloqueios quando existirem.
7. Preservar rastreabilidade entre documento, issue, implementação, QA e docs.
8. Entregar saída objetiva, utilizável e compatível com o fluxo do projeto.

---

## 7. Regras para tarefas de implementação

Quando a tarefa envolver código, o agente deve:

- entender o comportamento esperado antes de alterar arquivos;
- preservar contratos existentes, salvo instrução contrária;
- evitar mudanças amplas e não relacionadas;
- atualizar ou criar testes quando aplicável;
- informar arquivos alterados;
- informar comandos de teste executados;
- registrar riscos ou pendências.

---

## 8. Regras para QA

Quando a tarefa envolver QA, o agente deve validar a entrega contra:

```text
PRD
TDD
SDD
SPEC
Critérios de aceite da issue
Regras em .agents/rules/
Comportamento real do sistema
```

O QA deve apontar:

- aprovado;
- aprovado com ressalvas;
- reprovado;
- evidências;
- riscos;
- ações recomendadas.

---

## 9. Regras para documentação

Quando a tarefa envolver documentação, o agente deve:

- manter consistência com o comportamento real do sistema;
- não documentar funcionalidades inexistentes;
- atualizar roteiros operacionais quando uma entrega mudar comandos, APIs, telas ou fluxos;
- escrever instruções executáveis por alguém que não conhece previamente os comandos.

---

## 10. Regra final

O agente deve favorecer entregas pequenas, rastreáveis e verificáveis.

Sempre que uma tarefa estiver grande demais, quebrar em partes menores seguindo:

```text
PRD -> TDD -> SDD -> Backend/API/Frontend/Infra/Security -> QA -> Docs
```

Para organização no Linear, seguir:

```text
.agents/rules/linear_issue_management.md
```

Para transformar documentos em backlog, seguir:

```text
.agents/skills/break-prd-tdd-sdd-into-linear-issues/SKILL.md
```

---

## 11. Unificação das Ferramentas e Agentes

Para que todos os agentes do sistema de desenvolvimento trabalhem de forma sincronizada e com a mesma base de conhecimento, os ponteiros de ferramentas apontam para o arquivo mestre `AGENTS.md` na **raiz** e para a pasta central `.agents/`:

*   **Cursor IDE:** `.cursor/rules/` (arquivo `.cursor/rules/.cursorrules`).
*   **Claude Code:** `.claude/CLAUDE.md` → `AGENTS.md` na raiz.
*   **Antigravity:** quando a config existir, apontar para `AGENTS.md` na raiz (não sob `_docs/`).
*   **Windsurf:** não suportado (stub removido).
*   **Outros Agentes e Integrações:** Seguem diretamente `AGENTS.md` (raiz) e `.agents/`.

**Regra:** não manter `AGENTS.md` permanente sob `_docs/`. Em `_docs/`, só `_docs/specs/` (processo) e, se necessário, material transitório em `_docs/temp/` (não versionado). Canônico de governança = `AGENTS.md` na raiz.

