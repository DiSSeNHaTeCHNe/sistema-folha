# Linear Issue Management Rules

## 1. Objetivo

Padronizar o uso do Linear para trabalho multiagente, mantendo rastreabilidade entre planejamento, especificação, implementação, QA e documentação.

Esta regra define como issues devem ser identificadas, nomeadas, organizadas e distribuídas entre agentes.

---

## 2. Key da issue

Usar uma key única por produto/projeto.

Exemplo correto:

```text
ARC-101 - [Backend] Implementar endpoint de criação de mundo
ARC-102 - [Frontend] Criar tela de criação de mundo
ARC-103 - [QA] Validar fluxo de criação de mundo
ARC-104 - [Docs] Atualizar roteiro operacional
```

Evitar keys por frente, salvo se existirem times realmente separados no Linear:

```text
BACK-101
FRONT-102
QA-103
DOCS-104
```

A separação por frente deve ser feita por:

- prefixo no título;
- labels;
- assignee/agente;
- projeto;
- ciclo/milestone;
- épico/PBI pai.

---

## 3. Padrão de título

Toda issue deve seguir o padrão:

```text
[Frente] Verbo + objeto + contexto
```

Exemplos:

```text
[Planning] Quebrar PRD em épicos e tarefas
[PRD] Definir regras da importação mensal
[TDD] Definir arquitetura da importação
[SDD] Especificar contratos da API
[Backend] Implementar persistência mensal da folha
[API] Expor endpoint de importação
[Frontend] Criar tela de upload da folha
[QA] Validar rollback total em caso de erro
[Docs] Atualizar roteiro operacional
[Infra] Configurar Flyway no projeto
[Security] Proteger login contra força bruta
```

O título deve deixar claro:

- a frente responsável;
- a ação esperada;
- o objeto da tarefa;
- o contexto funcional ou técnico.

---

## 4. Frentes aceitas

As frentes oficiais são:

```text
[Planning]
[PRD]
[TDD]
[SDD]
[Backend]
[API]
[Frontend]
[QA]
[Docs]
[Infra]
[Security]
[Integration]
[Refactor]
[Research]
```

Cada issue deve ter uma frente principal.

Se uma issue envolver várias frentes, ela deve ser quebrada em issues menores.

Exemplo ruim:

```text
ARC-200 - [Backend/Frontend/QA] Implementar login completo
```

Exemplo correto:

```text
ARC-201 - [Backend] Implementar autenticação no domínio
ARC-202 - [API] Expor endpoint de login
ARC-203 - [Frontend] Criar tela de login
ARC-204 - [Security] Aplicar proteção contra força bruta
ARC-205 - [QA] Validar fluxo de login
ARC-206 - [Docs] Documentar autenticação
```

---

## 5. Labels recomendadas

### Por frente

```text
planning
prd
tdd
sdd
backend
api
frontend
qa
docs
infra
security
integration
refactor
research
```

### Por tipo de trabalho

```text
feature
bug
test
spec
documentation
tech-debt
refactor
chore
risk
blocked
```

### Por agente

```text
agent:pm
agent:architect
agent:backend
agent:api
agent:frontend
agent:qa
agent:docs
agent:infra
agent:security
agent:integration
```

Exemplo:

```text
Issue:
ARC-120 - [Backend] Implementar serviço de criação de mundo

Labels:
backend
feature
agent:backend
```

---

## 6. Épico/PBI pai

Toda entrega grande deve ter uma issue pai do tipo Epic ou PBI.

Exemplo:

```text
ARC-100 - [Epic] Importação mensal de folha
```

Issues filhas:

```text
ARC-101 - [PRD] Definir regras da importação mensal
ARC-102 - [TDD] Definir arquitetura da importação
ARC-103 - [SDD] Especificar validação atômica
ARC-104 - [Backend] Implementar entidades
ARC-105 - [API] Criar endpoint de upload
ARC-106 - [Frontend] Criar tela de upload
ARC-107 - [QA] Validar importação ponta a ponta
ARC-108 - [Docs] Documentar roteiro operacional
```

A issue pai deve conter:

- objetivo da entrega;
- escopo funcional;
- links para PRD/TDD/SDD;
- lista de issues filhas;
- critérios gerais de aceite;
- riscos conhecidos;
- dependências entre frentes.

---

## 7. Critérios mínimos de uma issue

Toda issue deve conter:

```md
## Objetivo

Descrever o que deve ser feito.

## Escopo

- Item 1
- Item 2
- Item 3

## Fora de escopo

- Item que não deve ser feito nesta issue

## Dependências

- ARC-XXX
- Documento PRD/TDD/SDD relacionado

## Critérios de aceite

- [ ] Critério 1
- [ ] Critério 2
- [ ] Critério 3

## Frente responsável

[Backend]

## Labels

backend, feature, agent:backend
```

---

## 8. Regras para multiagentes

- Uma issue deve ter uma única frente principal.
- Cada agente deve trabalhar apenas em issues da sua frente ou explicitamente atribuídas.
- Agentes não devem expandir escopo sem nova issue.
- Dependências devem estar explícitas na descrição da issue.
- Mudanças em contrato compartilhado devem ser feitas antes das tarefas dependentes.
- QA deve validar a integração final entre as entregas paralelas.
- Docs deve atualizar o roteiro operacional ao final da entrega.
- Issues de implementação devem estar ligadas a PRD, TDD ou SDD quando aplicável.

---

## 9. Regras de conclusão

Ao finalizar uma issue, o agente deve registrar:

````md
## Resultado

Descrever o que foi entregue.

## Arquivos alterados

- path/do/arquivo.ext

## Testes executados

```bash
comando usado para testar
```

## Evidências

- resultado dos testes;
- logs relevantes;
- screenshots, se aplicável;
- payloads de exemplo, se aplicável.

## Riscos ou pendências

- Nenhum risco conhecido
````

---

## 10. Ciclo de vida da issue por agente

Todo agente deve atualizar o status da issue no Linear de acordo com o progresso real do trabalho.

### Ao iniciar a issue

O agente deve, obrigatoriamente:

1. Atribuir a issue a si mesmo (`assignee`).
2. Mover o status para **In Progress**.

Isso sinaliza aos demais agentes que a issue está sendo executada e evita trabalho duplicado.

### Ao finalizar a issue

O agente deve, obrigatoriamente:

1. Registrar o resultado na issue (conforme seção 9 — Regras de conclusão).
2. Mover o status para **Done**.

### Regras complementares

- Nenhum agente deve marcar uma issue como **Done** sem ter registrado resultado, arquivos alterados e evidências.
- Se o agente não conseguir concluir a issue, deve mover para **Blocked** e registrar o motivo no comentário.
- Nenhum agente deve iniciar uma issue com status **Done** ou **Canceled**.
- Issues **In Progress** atribuídas a outro agente não devem ser assumidas sem alinhamento explícito.

---

## 11. Regra final

A key da issue identifica o item de trabalho dentro do produto.

A frente de execução deve ser identificada por:

- prefixo no título;
- labels;
- assignee/agente;
- épico/PBI pai;
- documentação vinculada.

Modelo recomendado:

```text
ARC-123 - [Backend] Implementar persistência mensal da folha
ARC-124 - [Frontend] Criar tela de auditoria para Admin
ARC-125 - [QA] Validar importação atômica
ARC-126 - [Docs] Atualizar roteiro operacional
```
