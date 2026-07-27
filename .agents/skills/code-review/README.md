# code-review

Multi-dimension code review via 6 parallel subagents, plus an **optional**
SonarQube + JaCoCo quality pass when MCP/coverage data are available.
Agnóstico de projeto — funciona com ou sem pull request aberto.

## Quando usar

Peça explicitamente:

- "review code"
- "review branch"
- "review das mudanças"
- "review PR #42"
- "code review"
- "review uncommitted"

A skill **não** é ativada automaticamente durante implementação ou perguntas gerais.

## Garantia read-only

A skill **nunca altera código**. Nenhum arquivo é criado, editado ou removido:

- Todos os subagentes são lançados com `readonly: true` (bloqueio no nível do sistema).
- No modo `report` (padrão): a saída é **texto direto no chat** — nenhum arquivo `.md` ou relatório é gravado em disco.
- No modo `github`: a saída são comentários via API do GitHub (`gh pr review --comment`). Nenhum arquivo local é tocado.
- Sonar: apenas leitura via MCP (sem mudar status de issues/hotspots). Não roda `mvn test` nem scanner.

## Como funciona

```text
Step 0  Resolve contexto (diff source, output mode, base branch)
Step 1  Inicializa: diff, intent, requisitos, docs + detecta Sonar/JaCoCo
Step 2  Lança 6 subagentes em paralelo (+ 7º Sonar/JaCoCo se disponível)
Step 3  Consolida achados em resumo único
```

### Modos de diff (INPUT)

| Modo | Quando | Comando interno |
|------|--------|-----------------|
| `branch` | **Padrão** | `git diff $(git merge-base HEAD {BASE})...HEAD` |
| `pull_request` | "review PR #N" | `gh pr diff {N}` |
| `uncommitted` | "review uncommitted" | `git diff` + `git diff --cached` |
| `files` | Lista explícita de arquivos | Leitura direta |

### Modos de saída (OUTPUT)

| Modo | Quando | Ação |
|------|--------|------|
| `report` | **Padrão** | Resumo no chat |
| `github` | "post on PR" + PR existe | Comentários inline + `gh pr review --comment` |

## Dimensões de Review

| # | Subagente | Foco |
|---|-----------|------|
| 1 | **Security** | Secrets, auth, injection, PII, CORS, validação |
| 2 | **Requirements** | Diff vs requisitos (Linear/Jira/GitHub + spec files) |
| 3 | **Tests** | Cobertura de testes, qualidade, anti-patterns |
| 4 | **Architecture** | Padrões do projeto + regras universais (SRP, naming, camadas) |
| 5 | **Regression** | Imports fantasmas, deleções não relacionadas, TODOs, asserts enfraquecidos |
| 6 | **Performance** | N+1, queries sem limite, I/O sequencial, alocações em hot path |
| 7 | **SonarQube + JaCoCo** *(opcional)* | Quality Gate, issues/hotspots no diff, cobertura de linhas novas |

### SonarQube + JaCoCo (quando disponível)

Pré-condições (fail-soft):

1. MCP SonarQube `ready` no Cursor
2. `sonar.projectKey` em `sonar-project.properties` (neste repo: `sistema-folha`)
3. Cobertura: `backend/target/site/jacoco/jacoco.xml` **ou** métricas de coverage já no Sonar

Para gerar/atualizar dados locais:

```bash
cd backend && mvn test          # JaCoCo → target/site/jacoco/jacoco.xml
./diversos/scripts/sonar-analyze.sh   # testes+JaCoCo + envio ao Sonar
```

Se Sonar/JaCoCo não estiver disponível, o review segue só com os 6 agentes e registra o skip no resumo.

## Descoberta dinâmica de docs

A skill **não** exige docs específicos. Ela busca automaticamente:

```text
_docs/specs/{ARCHITECTURE,CONVENTIONS,TESTING,STRUCTURE,INTEGRATIONS,CONCERNS}.md
docs/{architecture,conventions,testing,security,coding-patterns,integration-patterns}.md
.agents/rules/*.md
CONTRIBUTING.md, ARCHITECTURE.md
```

Se nenhum doc for encontrado, aplica apenas regras universais (OWASP, SOLID, boas práticas de teste).

## Descoberta de requisitos

Duas trilhas executadas em paralelo:

1. **Issue tracker** — extrai ID do branch (`[A-Z]+-[0-9]+`), tenta Linear MCP, depois GitHub Issues, depois Jira REST.
2. **Spec files** — busca em `_docs/specs/features/{feature}/`, `docs/specs/`, `.specs/`, links no corpo da PR.

Se nenhum for encontrado: "requirements verification skipped".

## Severidades

| Label | Significado |
|-------|-------------|
| 🚨 Critical | Bugs ou erros de lógica que causarão falhas |
| 🔒 Security | Vulnerabilidades ou exposição de dados |
| ⚡ Performance | Problemas de performance significativos |
| ⚠️ Warning | Code smells ou problemas de manutenibilidade |
| 💡 Suggestion | Melhorias opcionais |

## Exemplos de uso

```text
# Review da branch atual vs main (padrão)
> review code

# Review de PR aberto
> review PR #42

# Review só do que ainda não foi commitado
> review uncommitted changes

# Review e postar comentários no GitHub
> review PR #42 and post on GitHub

# Review de arquivos específicos
> review these files: src/service/Foo.java, src/controller/Bar.java
```

## Formato de saída

A saída vai para o **chat** (modo `report`) ou para **comentários no GitHub** (modo `github`).
Nenhum arquivo é criado em disco em nenhum dos dois modos.

O resumo consolidado inclui:

- Tabela de metadados (modo, docs carregados, Sonar QG/coverage, total de achados)
- Achados agrupados por severidade
- Lista de arquivos sem nenhum achado (gap detection)
- Um highlight positivo por subagente

## Relação com outras skills

| Skill | Diferença |
|-------|-----------|
| `review-bugbot` | Subagente nativo Cursor — cobre bugs/regressão. `code-review` orquestra 6 dimensões + Sonar opcional. |
| `review-security` | Subagente nativo Cursor — foco em segurança. `code-review` inclui segurança + demais dimensões. |
| `pr-review` (Fakeflix) | Hardcoded para Fakeflix + GitHub. `code-review` é agnóstico e funciona localmente. |

## Estrutura

```text
.agents/skills/code-review/
├── SKILL.md    # Protocolo de orquestração (instruções para o agente)
└── README.md   # Este arquivo (documentação humana)
```
