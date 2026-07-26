---
name: break-prd-tdd-sdd-into-linear-issues
description: Mapeia e analisa documentos de requisitos técnicos (PRD, TDD, SDD, SPEC) para quebrá-los sistematicamente em issues atômicas, rastreáveis e prontas para execução multiagente no Linear, organizadas por frentes técnicas (Backend, API, Frontend, QA, etc.).
---

# Skill: Break PRD/TDD/SDD into Linear Issues

Este procedimento operacional descreve como analisar documentos técnicos de alto nível (PRD, TDD, SDD, SPEC) e convertê-los de forma sistemática em um backlog de issues do Linear atômicas, rastreáveis e prontas para execução multiagente.

---

## 1. Objetivo da Skill

Garantir que nenhuma funcionalidade, requisito de segurança ou validação técnica descrita no PRD/TDD/SDD seja esquecida, quebrando-os em tarefas pequenas que possam ser distribuídas entre frentes (`[Backend]`, `[Frontend]`, `[QA]`, etc.) seguindo os padrões definidos em `rules/linear_issue_management.md`.

---

## 2. Passo a Passo de Execução

### Passo 1: Leitura e Mapeamento de Pilares (PRD)
- Leia todo o documento de requisitos do produto (PRD).
- Identifique as grandes áreas de escopo do Milestone/Sprint atual (ex: Core Agent Loop, TUI, persistência, etc.).
- Cada pilar de escopo será mapeado como uma **Issue Pai** do tipo **`[Epic]`** ou **`[PBI]`**.

### Passo 2: Faturamento em Frentes e Dependências (TDD/SDD)
Para cada Epic identificado, quebre-o em tarefas especializadas por frente responsável, mapeando as dependências lógicas (backend deve preceder frontend, etc.):

1.  **[Planning]**: Planejamento do ciclo e desmembramento técnico inicial.
2.  **[Backend]**: Modelagem de dados, regras de negócio no domínio e algoritmos fundamentais.
3.  **[API]**: Exposição de endpoints, contratos, schemas DTO e controllers.
4.  **[Frontend]**: Interface gráfica de usuário, layout, responsividade e integração com a API.
5.  **[Infra]**: Provisionamento de serviços, bancos de dados, CI/CD ou pacotes.
6.  **[Security]**: Mecanismos de sanitização, isolamento, sandbox e proteção de segredos.
7.  **[QA]**: Testes de integração ponta a ponta e validação do fluxo funcional.
8.  **[Docs]**: Roteiros operacionais, README, wikis e guias de uso.

### Passo 3: Aplicação de Nomenclatura Estrita
Toda issue gerada deve, obrigatoriamente, seguir o padrão de título:
```text
[Frente] Verbo + Objeto + Contexto
```
*Exemplo Correto:* `[Backend] Implementar circuit breaker no cliente de API externa`  
*Exemplo Incorreto:* `Implementar API externa` (Sem frente, sem verbo claro, sem contexto técnico).

### Passo 4: Geração do Corpo da Issue
Para cada issue listada no backlog, escreva a descrição detalhada seguindo o padrão mínimo estruturado:

```markdown
## Objetivo

Descrever brevemente o que esta tarefa atinge e qual requisito do PRD ela satisfaz.

## Escopo

- [ ] Item específico de implementação
- [ ] Validação básica ou tratamento de erros local

## Fora de escopo

- O que NÃO deve ser feito para evitar vazamento de escopo (scope creep).

## Dependências

- Key ou Nome da tarefa que precisa ser concluída antes desta (ex: a tarefa do Backend).
- Link para a especificação `_docs/specs/features/...` correspondente.

## Critérios de aceite

- [ ] Critério 1 (técnico ou comportamental)
- [ ] Critério 2 (cobertura de testes unitários > X%)

## Frente responsável

[Nome da Frente]

## Labels

[Labels recomendadas]
```

### Passo 5: Verificação de Rastreabilidade (Traceability Check)
Faça uma verificação cruzada (cross-check):
- Cada linha de requisito funcional (P0/P1) do PRD possui pelo menos uma tarefa de Backend/API correspondente?
- Cada funcionalidade visível possui uma tarefa de `[QA]` dedicada para validação ponta a ponta?
- As alterações de comando ou comportamento possuem uma tarefa de `[Docs]` correspondente?

---

## 3. Exemplo Prático de Conversão

### Entrada (PRD original):
> *"O serviço deve registrar todas as iterações e chamadas de ferramentas em um arquivo de log local YYYY-MM-DD.jsonl na pasta de logs configurável do projeto, limitando o sumário do resultado a 2KB para não estourar espaço em disco."*

### Saída (Linear Issues geradas):

*   **`[Epic] Cost & Audit Logging`**
    *   `[Backend] Criar serviço de persistência de Logs em arquivo JSONL`
        *   Frente: `[Backend]`
        *   Escopo: Lógica de rota do log, criação de pastas locais e rota de data YYYY-MM-DD.
    *   `[Refactor] Implementar utilitário de truncamento de texto para 2KB`
        *   Frente: `[Refactor]`
        *   Escopo: Função puramente testável que corta strings com segurança de bytes.
    *   `[QA] Validar integridade do JSONL e truncamento sob estresse`
        *   Frente: `[QA]`
        *   Escopo: Testes integrados que simulam payloads gigantes e validam limites de bytes.
